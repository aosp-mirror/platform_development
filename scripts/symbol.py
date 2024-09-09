#!/usr/bin/env python3
#
# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Module for looking up symbolic debugging information.

The information can include symbol names, offsets, and source locations.
"""

import atexit
import json
import glob
import os
import platform
import re
import shutil
import signal
import subprocess
import unittest

ANDROID_BUILD_TOP = os.environ.get("ANDROID_BUILD_TOP", ".")


def FindClangDir():
  get_clang_version = ANDROID_BUILD_TOP + "/build/soong/scripts/get_clang_version.py"
  if os.path.exists(get_clang_version):
    # We want the script to fail if get_clang_version.py exists but is unable
    # to find the clang version.
    version_output = subprocess.check_output(get_clang_version, text=True)
    return ANDROID_BUILD_TOP + "/prebuilts/clang/host/linux-x86/" + version_output.strip()
  else:
    return None


def FindSymbolsDir():
  saveddir = os.getcwd()
  os.chdir(ANDROID_BUILD_TOP)
  stream = None
  try:
    cmd = "build/soong/soong_ui.bash --dumpvar-mode --abs TARGET_OUT_UNSTRIPPED"
    stream = subprocess.Popen(cmd, stdout=subprocess.PIPE, universal_newlines=True, shell=True).stdout
    return str(stream.read().strip())
  finally:
    if stream is not None:
        stream.close()
    os.chdir(saveddir)

SYMBOLS_DIR = FindSymbolsDir()

ARCH_IS_32BIT = None

VERBOSE = False

# These are private. Do not access them from other modules.
_CACHED_TOOLCHAIN = None
_CACHED_CXX_FILT = None

# Caches for symbolized information.
_SYMBOL_INFORMATION_ADDR2LINE_CACHE = {}
_SYMBOL_INFORMATION_OBJDUMP_CACHE = {}
_SYMBOL_DEMANGLING_CACHE = {}

# Caches for pipes to subprocesses.

class ProcessCache:
  _cmd2pipe = {}
  _lru = []

  # Max number of open pipes.
  _PIPE_MAX_OPEN = 10

  def GetProcess(self, cmd):
    cmd_tuple = tuple(cmd)  # Need to use a tuple as lists can't be dict keys.
    # Pipe already available?
    if cmd_tuple in self._cmd2pipe:
      pipe = self._cmd2pipe[cmd_tuple]
      # Update LRU.
      self._lru = [(cmd_tuple, pipe)] + [i for i in self._lru if i[0] != cmd_tuple]
      return pipe

    # Not cached, yet. Open a new one.

    # Check if too many are open, close the old ones.
    while len(self._lru) >= self._PIPE_MAX_OPEN:
      open_cmd, open_pipe = self._lru.pop()
      del self._cmd2pipe[open_cmd]
      self.TerminateProcess(open_pipe)

    # Create and put into cache.
    pipe = self.SpawnProcess(cmd)
    self._cmd2pipe[cmd_tuple] = pipe
    self._lru = [(cmd_tuple, pipe)] + self._lru
    return pipe

  def SpawnProcess(self, cmd):
     return subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, universal_newlines=True)

  def TerminateProcess(self, pipe):
    pipe.stdin.close()
    pipe.stdout.close()
    pipe.terminate()
    pipe.wait()

  def KillAllProcesses(self):
    for _, open_pipe in self._lru:
      self.TerminateProcess(open_pipe)
    _cmd2pipe = {}
    _lru = []


_PIPE_ADDR2LINE_CACHE = ProcessCache()
_PIPE_CPPFILT_CACHE = ProcessCache()


# Process cache cleanup on shutdown.

def CloseAllPipes():
  _PIPE_ADDR2LINE_CACHE.KillAllProcesses()
  _PIPE_CPPFILT_CACHE.KillAllProcesses()


atexit.register(CloseAllPipes)


def PipeTermHandler(signum, frame):
  CloseAllPipes()
  os._exit(0)


for sig in (signal.SIGABRT, signal.SIGINT, signal.SIGTERM):
  signal.signal(sig, PipeTermHandler)




def ToolPath(tool, toolchain=None):
  """Return a fully-qualified path to the specified tool, or just the tool if it's on PATH """
  if shutil.which(tool):
    return tool
  if not toolchain:
    toolchain = FindToolchain()
  return os.path.join(toolchain, tool)


def FindToolchain():
  """Returns the toolchain."""

  global _CACHED_TOOLCHAIN
  if _CACHED_TOOLCHAIN:
    return _CACHED_TOOLCHAIN

  llvm_binutils_dir = ANDROID_BUILD_TOP + "/prebuilts/clang/host/linux-x86/llvm-binutils-stable/";
  if not os.path.exists(llvm_binutils_dir):
    raise Exception("Could not find llvm tool chain directory %s" % (llvm_binutils_dir))

  _CACHED_TOOLCHAIN = llvm_binutils_dir
  print("Using toolchain from:", _CACHED_TOOLCHAIN)
  return _CACHED_TOOLCHAIN


def SymbolInformation(lib, addr):
  """Look up symbol information about an address.

  Args:
    lib: library (or executable) pathname containing symbols
    addr: string hexidecimal address

  Returns:
    A list of the form [(source_symbol, source_location,
    object_symbol_with_offset)].

    If the function has been inlined then the list may contain
    more than one element with the symbols for the most deeply
    nested inlined location appearing first.  The list is
    always non-empty, even if no information is available.

    Usually you want to display the source_location and
    object_symbol_with_offset from the last element in the list.
  """
  info = SymbolInformationForSet(lib, set([addr]))
  return (info and info.get(addr)) or [(None, None, None)]


def SymbolInformationForSet(lib, unique_addrs):
  """Look up symbol information for a set of addresses from the given library.

  Args:
    lib: library (or executable) pathname containing symbols
    unique_addrs: set of hexidecimal addresses

  Returns:
    A dictionary of the form {addr: [(source_symbol, source_location,
    object_symbol_with_offset)]} where each address has a list of
    associated symbols and locations.  The list is always non-empty.

    If the function has been inlined then the list may contain
    more than one element with the symbols for the most deeply
    nested inlined location appearing first.  The list is
    always non-empty, even if no information is available.

    Usually you want to display the source_location and
    object_symbol_with_offset from the last element in the list.
  """
  if not lib:
    return None

  addr_to_line = CallLlvmSymbolizerForSet(lib, unique_addrs)
  if not addr_to_line:
    return None

  addr_to_objdump = CallObjdumpForSet(lib, unique_addrs)
  if not addr_to_objdump:
    return None

  result = {}
  for addr in unique_addrs:
    source_info = addr_to_line.get(addr)
    if not source_info:
      source_info = [(None, None)]
    if addr in addr_to_objdump:
      (object_symbol, object_offset) = addr_to_objdump.get(addr)
      object_symbol_with_offset = FormatSymbolWithOffset(object_symbol,
                                                         object_offset)
    else:
      object_symbol_with_offset = None
    result[addr] = [(source_symbol, source_location, object_symbol_with_offset)
        for (source_symbol, source_location) in source_info]

  return result


def _OptionalStackRecordField(json_result, field):
  """Fix up bizarre formatting of llvm-symbolizer output

  Some parts of the FRAME output are output as a string containing a hex
  integer, or the empty string when it's missing.

  Args:
    json_result: dictionary containing the Frame response
    field: name of the field we want to read

  Returns:
    integer of field value, or None if missing
  """
  value = json_result.get(field, "")
  if isinstance(value, int):
    # Leaving this here in case someone decides to fix the types of the
    # symbolizer output, so it's easier to roll out.
    return value
  if value != "":
    return int(value, 16)
  return None


def _GetJSONSymbolizerForLib(lib, args=None):
  """ Find symbol file for lib, and return a llvm-symbolizer instance for it.

  Args:
    lib: library (or executable) pathname containing symbols
    args: (optional) list of arguments to pass to llvm-symbolizer

  Returns:
    child process, or None if lib not found
  """
  if args is None:
    args = []
  symbols = SYMBOLS_DIR + lib
  if not os.path.exists(symbols):
    symbols = lib
    if not os.path.exists(symbols):
      return None

  # Make sure the symbols path is not a directory.
  if os.path.isdir(symbols):
    return None

  cmd = [ToolPath("llvm-symbolizer"), "--output-style=JSON"] + args + ["--obj=" + symbols]
  return _PIPE_ADDR2LINE_CACHE.GetProcess(cmd)


def GetStackRecordsForSet(lib, unique_addrs):
  """Look up stack record information for a set of addresses

  Args:
    lib: library (or executable) pathname containing symbols
    unique_addrs: set of integer addresses look up.

  Returns:
    A list of tuples
    (addr, function_name, local_name, file_line, frame_offset, size, tag_offset)
    describing the local variables of the stack frame.
    frame_offset, size, tag_offset may be None.
  """
  child = _GetJSONSymbolizerForLib(lib)
  if child is None:
    return None
  records = []
  for addr in unique_addrs:
    child.stdin.write("FRAME 0x%x\n" % addr)
    child.stdin.flush()
    json_result = json.loads(child.stdout.readline().strip())
    for frame in json_result["Frame"]:
      records.append(
        (addr,
        frame["FunctionName"],
        frame["Name"],
        frame["DeclFile"] + ":" + str(frame["DeclLine"]),
        frame.get("FrameOffset"),
        _OptionalStackRecordField(frame, "Size"),
        _OptionalStackRecordField(frame, "TagOffset")))
  return records


def CallLlvmSymbolizerForSet(lib, unique_addrs):
  """Look up line and symbol information for a set of addresses.

  Args:
    lib: library (or executable) pathname containing symbols
    unique_addrs: set of string hexidecimal addresses look up.

  Returns:
    A dictionary of the form {addr: [(symbol, file:line)]} where
    each address has a list of associated symbols and locations
    or an empty list if no symbol information was found.

    If the function has been inlined then the list may contain
    more than one element with the symbols for the most deeply
    nested inlined location appearing first.
  """
  if not lib:
    return None

  result = {}
  addrs = sorted(unique_addrs)

  if lib in _SYMBOL_INFORMATION_ADDR2LINE_CACHE:
    addr_cache = _SYMBOL_INFORMATION_ADDR2LINE_CACHE[lib]

    # Go through and handle all known addresses.
    for x in range(len(addrs)):
      next_addr = addrs.pop(0)
      if next_addr in addr_cache:
        result[next_addr] = addr_cache[next_addr]
      else:
        # Re-add, needs to be symbolized.
        addrs.append(next_addr)

    if not addrs:
      # Everything was cached, we're done.
      return result
  else:
    addr_cache = {}
    _SYMBOL_INFORMATION_ADDR2LINE_CACHE[lib] = addr_cache

  child = _GetJSONSymbolizerForLib(
    lib, ["--functions", "--inlines", "--demangle"])
  if child is None:
    return None
  for addr in addrs:
    try:
      child.stdin.write("0x%s\n" % addr)
      child.stdin.flush()
      records = []
      json_result = json.loads(child.stdout.readline().strip())
      for symbol in json_result["Symbol"]:
        function_name = symbol["FunctionName"]
        # GNU style location: file_name:line_num
        location = ("%s:%s" % (symbol["FileName"], symbol["Line"]))
        records.append((function_name, location))
    except IOError as e:
      # Remove the / in front of the library name to match other output.
      records = [(None, lib[1:] + "  ***Error: " + str(e))]
    result[addr] = records
    addr_cache[addr] = records
  return result


def CallObjdumpForSet(lib, unique_addrs):
  """Use objdump to find out the names of the containing functions.

  Args:
    lib: library (or executable) pathname containing symbols
    unique_addrs: set of string hexidecimal addresses to find the functions for.

  Returns:
    A dictionary of the form {addr: (string symbol, offset)}.
  """
  if not lib:
    return None

  result = {}
  addrs = sorted(unique_addrs)

  addr_cache = None
  if lib in _SYMBOL_INFORMATION_OBJDUMP_CACHE:
    addr_cache = _SYMBOL_INFORMATION_OBJDUMP_CACHE[lib]

    # Go through and handle all known addresses.
    for x in range(len(addrs)):
      next_addr = addrs.pop(0)
      if next_addr in addr_cache:
        result[next_addr] = addr_cache[next_addr]
      else:
        # Re-add, needs to be symbolized.
        addrs.append(next_addr)

    if not addrs:
      # Everything was cached, we're done.
      return result
  else:
    addr_cache = {}
    _SYMBOL_INFORMATION_OBJDUMP_CACHE[lib] = addr_cache

  symbols = SYMBOLS_DIR + lib
  if not os.path.exists(symbols):
    symbols = lib
    if not os.path.exists(symbols):
      return None

  start_addr_dec = str(int(addrs[0], 16))
  stop_addr_dec = str(int(addrs[-1], 16) + 8)
  cmd = [ToolPath("llvm-objdump"),
         "--section=.text",
         "--demangle",
         "--disassemble",
         "--start-address=" + start_addr_dec,
         "--stop-address=" + stop_addr_dec,
         symbols]

  # Function lines look like:
  #   000177b0 <android::IBinder::~IBinder()+0x2c>:
  # We pull out the address and function first. Then we check for an optional
  # offset. This is tricky due to functions that look like "operator+(..)+0x2c"
  func_regexp = re.compile("(^[a-f0-9]*) \<(.*)\>:$")
  offset_regexp = re.compile("(.*)\+0x([a-f0-9]*)")

  # A disassembly line looks like:
  #   177b2:	b510      	push	{r4, lr}
  asm_regexp = re.compile("(^[ a-f0-9]*):[ a-f0-0]*.*$")

  current_symbol = None    # The current function symbol in the disassembly.
  current_symbol_addr = 0  # The address of the current function.
  addr_index = 0  # The address that we are currently looking for.

  stream = subprocess.Popen(cmd, stdout=subprocess.PIPE, universal_newlines=True).stdout
  for line in stream:
    # Is it a function line like:
    #   000177b0 <android::IBinder::~IBinder()>:
    components = func_regexp.match(line)
    if components:
      # This is a new function, so record the current function and its address.
      current_symbol_addr = int(components.group(1), 16)
      current_symbol = components.group(2)

      # Does it have an optional offset like: "foo(..)+0x2c"?
      components = offset_regexp.match(current_symbol)
      if components:
        current_symbol = components.group(1)
        offset = components.group(2)
        if offset:
          current_symbol_addr -= int(offset, 16)

    # Is it an disassembly line like:
    #   177b2:	b510      	push	{r4, lr}
    components = asm_regexp.match(line)
    if components:
      addr = components.group(1)
      target_addr = addrs[addr_index]
      i_addr = int(addr, 16)
      i_target = int(target_addr, 16)
      if i_addr == i_target:
        result[target_addr] = (current_symbol, i_target - current_symbol_addr)
        addr_cache[target_addr] = result[target_addr]
        addr_index += 1
        if addr_index >= len(addrs):
          break
  stream.close()

  return result


def CallCppFilt(mangled_symbol):
  if mangled_symbol in _SYMBOL_DEMANGLING_CACHE:
    return _SYMBOL_DEMANGLING_CACHE[mangled_symbol]

  global _CACHED_CXX_FILT
  if not _CACHED_CXX_FILT:
    toolchains = None
    clang_dir = FindClangDir()
    if clang_dir:
      if os.path.exists(clang_dir + "/bin/llvm-cxxfilt"):
        toolchains = [clang_dir + "/bin/llvm-cxxfilt"]
      else:
        raise Exception("bin/llvm-cxxfilt missing from " + clang_dir)
    else:
      # When run in CI, we don't have a way to find the clang version.  But
      # llvm-cxxfilt should be available in the following relative path.
      toolchains = glob.glob("./clang-r*/bin/llvm-cxxfilt")
      if toolchains and len(toolchains) != 1:
        raise Exception("Expected one llvm-cxxfilt but found many: " + \
                        ", ".join(toolchains))
    if not toolchains:
      raise Exception("Could not find llvm-cxxfilt tool")
    _CACHED_CXX_FILT = sorted(toolchains)[-1]

  cmd = [_CACHED_CXX_FILT]
  process = _PIPE_CPPFILT_CACHE.GetProcess(cmd)
  process.stdin.write(mangled_symbol)
  process.stdin.write("\n")
  process.stdin.flush()

  demangled_symbol = process.stdout.readline().strip()

  _SYMBOL_DEMANGLING_CACHE[mangled_symbol] = demangled_symbol

  return demangled_symbol


def FormatSymbolWithOffset(symbol, offset):
  if offset == 0:
    return symbol
  return "%s+%d" % (symbol, offset)

def FormatSymbolWithoutParameters(symbol):
  """Remove parameters from function.

  Rather than trying to parse the demangled C++ signature,
  it just removes matching top level parenthesis.
  """
  if not symbol:
    return symbol

  result = symbol
  result = result.replace(") const", ")")                  # Strip const keyword.
  result = result.replace("operator<<", "operator\u00AB")  # Avoid unmatched '<'.
  result = result.replace("operator>>", "operator\u00BB")  # Avoid unmatched '>'.
  result = result.replace("operator->", "operator\u2192")  # Avoid unmatched '>'.

  nested = []  # Keeps tract of current nesting level of parenthesis.
  for i in reversed(range(len(result))):  # Iterate backward to make cutting easier.
    c = result[i]
    if c == ')' or c == '>':
      if len(nested) == 0:
        end = i + 1  # Mark the end of top-level pair.
      nested.append(c)
    if c == '(' or c == '<':
      if len(nested) == 0 or {')':'(', '>':'<'}[nested.pop()] != c:
        return symbol  # Malformed: character does not match its pair.
      if len(nested) == 0 and c == '(' and (end - i) > 2:
        result = result[:i] + result[end:]  # Remove substring (i, end).
  if len(nested) > 0:
    return symbol  # Malformed: missing pair.

  return result.strip()

def SetBitness(lines):
  global ARCH_IS_32BIT

  trace_line = re.compile("\#[0-9]+[ \t]+..[ \t]+([0-9a-f]{8}|[0-9a-f]{16})([ \t]+|$)")
  asan_trace_line = re.compile("\#[0-9]+[ \t]+0x([0-9a-f]+)[ \t]+")

  ARCH_IS_32BIT = False
  for line in lines:
    trace_match = trace_line.search(line)
    if trace_match:
      # Try to guess the arch, we know the bitness.
      if len(trace_match.group(1)) == 16:
        ARCH_IS_32BIT = False
      else:
        ARCH_IS_32BIT = True
      break
    asan_trace_match = asan_trace_line.search(line)
    if asan_trace_match:
      # We might be able to guess the bitness by the length of the address.
      if len(asan_trace_match.group(1)) > 8:
        ARCH_IS_32BIT = False
        # We know for a fact this is 64 bit, so we are done.
        break
      else:
        # This might be 32 bit, or just a small address. Keep going in this
        # case, but if we couldn't figure anything else out, go with 32 bit.
        ARCH_IS_32BIT = True

class FindClangDirTests(unittest.TestCase):
  @unittest.skipIf(ANDROID_BUILD_TOP == '.', 'Test only supported in an Android tree.')
  def test_clang_dir_found(self):
    self.assertIsNotNone(FindClangDir())

class SetBitnessTests(unittest.TestCase):
  def test_32bit_check(self):
    global ARCH_IS_32BIT

    SetBitness(["#00 pc 000374e0"])
    self.assertTrue(ARCH_IS_32BIT)

  def test_64bit_check(self):
    global ARCH_IS_32BIT

    SetBitness(["#00 pc 00000000000374e0"])
    self.assertFalse(ARCH_IS_32BIT)

  def test_32bit_asan_trace_line_toolchain(self):
    global ARCH_IS_32BIT

    SetBitness(["#10 0xb5eeba5d  (/system/vendor/lib/egl/libGLESv1_CM_adreno.so+0xfa5d)"])
    self.assertTrue(ARCH_IS_32BIT)

  def test_64bit_asan_trace_line_toolchain(self):
    global ARCH_IS_32BIT

    SetBitness(["#12 0x5d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)",
                "#12 0x11b35d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)"])
    self.assertFalse(ARCH_IS_32BIT)

class FormatSymbolWithoutParametersTests(unittest.TestCase):
  def test_c(self):
    self.assertEqual(FormatSymbolWithoutParameters("foo"), "foo")
    self.assertEqual(FormatSymbolWithoutParameters("foo+42"), "foo+42")

  def test_simple(self):
    self.assertEqual(FormatSymbolWithoutParameters("foo(int i)"), "foo")
    self.assertEqual(FormatSymbolWithoutParameters("foo(int i)+42"), "foo+42")
    self.assertEqual(FormatSymbolWithoutParameters("bar::foo(int i)+42"), "bar::foo+42")
    self.assertEqual(FormatSymbolWithoutParameters("operator()"), "operator()")

  def test_templates(self):
    self.assertEqual(FormatSymbolWithoutParameters("bar::foo<T>(vector<T>& v)"), "bar::foo<T>")
    self.assertEqual(FormatSymbolWithoutParameters("bar<T>::foo(vector<T>& v)"), "bar<T>::foo")
    self.assertEqual(FormatSymbolWithoutParameters("bar::foo<T>(vector<T<U>>& v)"), "bar::foo<T>")
    self.assertEqual(FormatSymbolWithoutParameters("bar::foo<(EnumType)0>(vector<(EnumType)0>& v)"),
                                                   "bar::foo<(EnumType)0>")

  def test_nested(self):
    self.assertEqual(FormatSymbolWithoutParameters("foo(int i)::bar(int j)"), "foo::bar")

  def test_unbalanced(self):
    self.assertEqual(FormatSymbolWithoutParameters("foo(bar(int i)"), "foo(bar(int i)")
    self.assertEqual(FormatSymbolWithoutParameters("foo)bar(int i)"), "foo)bar(int i)")
    self.assertEqual(FormatSymbolWithoutParameters("foo<bar(int i)"), "foo<bar(int i)")
    self.assertEqual(FormatSymbolWithoutParameters("foo>bar(int i)"), "foo>bar(int i)")

if __name__ == '__main__':
    unittest.main(verbosity=2)
