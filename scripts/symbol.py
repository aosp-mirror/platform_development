#!/usr/bin/python
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
import glob
import os
import platform
import re
import signal
import subprocess
import unittest

ANDROID_BUILD_TOP = os.environ["ANDROID_BUILD_TOP"]
if not ANDROID_BUILD_TOP:
  ANDROID_BUILD_TOP = "."

def FindSymbolsDir():
  saveddir = os.getcwd()
  os.chdir(ANDROID_BUILD_TOP)
  try:
    cmd = ("CALLED_FROM_SETUP=true BUILD_SYSTEM=build/core "
           "SRC_TARGET_DIR=build/target make -f build/core/config.mk "
           "dumpvar-abs-TARGET_OUT_UNSTRIPPED")
    stream = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True).stdout
    return os.path.join(ANDROID_BUILD_TOP, stream.read().strip())
  finally:
    os.chdir(saveddir)

SYMBOLS_DIR = FindSymbolsDir()

ARCH = None


# These are private. Do not access them from other modules.
_CACHED_TOOLCHAIN = None
_CACHED_TOOLCHAIN_ARCH = None

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
     return subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)

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
  """Return a fully-qualified path to the specified tool"""
  if not toolchain:
    toolchain = FindToolchain()
  return glob.glob(os.path.join(toolchain, "*-" + tool))[0]


def FindToolchain():
  """Returns the toolchain matching ARCH."""
  global _CACHED_TOOLCHAIN, _CACHED_TOOLCHAIN_ARCH
  if _CACHED_TOOLCHAIN is not None and _CACHED_TOOLCHAIN_ARCH == ARCH:
    return _CACHED_TOOLCHAIN

  # We use slightly different names from GCC, and there's only one toolchain
  # for x86/x86_64. Note that these are the names of the top-level directory
  # rather than the _different_ names used lower down the directory hierarchy!
  gcc_dir = ARCH
  if gcc_dir == "arm64":
    gcc_dir = "aarch64"
  elif gcc_dir == "mips64":
    gcc_dir = "mips"
  elif gcc_dir == "x86_64":
    gcc_dir = "x86"

  os_name = platform.system().lower();

  available_toolchains = glob.glob("%s/prebuilts/gcc/%s-x86/%s/*-linux-*/bin/" % (ANDROID_BUILD_TOP, os_name, gcc_dir))
  if len(available_toolchains) == 0:
    raise Exception("Could not find tool chain for %s" % (ARCH))

  toolchain = sorted(available_toolchains)[-1]

  if not os.path.exists(ToolPath("addr2line", toolchain)):
    raise Exception("No addr2line for %s" % (toolchain))

  _CACHED_TOOLCHAIN = toolchain
  _CACHED_TOOLCHAIN_ARCH = ARCH
  print "Using %s toolchain from: %s" % (_CACHED_TOOLCHAIN_ARCH, _CACHED_TOOLCHAIN)
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

  addr_to_line = CallAddr2LineForSet(lib, unique_addrs)
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


def CallAddr2LineForSet(lib, unique_addrs):
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

  symbols = SYMBOLS_DIR + lib
  if not os.path.exists(symbols):
    symbols = lib
    if not os.path.exists(symbols):
      return None

  # Make sure the symbols path is not a directory.
  if os.path.isdir(symbols):
    return None

  cmd = [ToolPath("addr2line"), "--functions", "--inlines",
      "--demangle", "--exe=" + symbols]
  child = _PIPE_ADDR2LINE_CACHE.GetProcess(cmd)

  for addr in addrs:
    child.stdin.write("0x%s\n" % addr)
    child.stdin.flush()
    records = []
    first = True
    while True:
      symbol = child.stdout.readline().strip()
      if symbol == "??":
        symbol = None
      location = child.stdout.readline().strip()
      if location == "??:0" or location == "??:?":
        location = None
      if symbol is None and location is None:
        break
      records.append((symbol, location))
      if first:
        # Write a blank line as a sentinel so we know when to stop
        # reading inlines from the output.
        # The blank line will cause addr2line to emit "??\n??:0\n".
        child.stdin.write("\n")
        first = False
    result[addr] = records
    addr_cache[addr] = records
  return result


def StripPC(addr):
  """Strips the Thumb bit a program counter address when appropriate.

  Args:
    addr: the program counter address

  Returns:
    The stripped program counter address.
  """
  global ARCH
  if ARCH == "arm":
    return addr & ~1
  return addr


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

  start_addr_dec = str(StripPC(int(addrs[0], 16)))
  stop_addr_dec = str(StripPC(int(addrs[-1], 16)) + 8)
  cmd = [ToolPath("objdump"),
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

  stream = subprocess.Popen(cmd, stdout=subprocess.PIPE).stdout
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
      i_target = StripPC(int(target_addr, 16))
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

  cmd = [ToolPath("c++filt")]
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


def GetAbiFromToolchain(toolchain_var, bits):
  toolchain = os.environ.get(toolchain_var)
  if not toolchain:
    return None

  toolchain_match = re.search("\/(aarch64|arm|mips|x86)\/", toolchain)
  if toolchain_match:
    abi = toolchain_match.group(1)
    if abi == "aarch64":
      return "arm64"
    elif bits == 64:
      if abi == "x86":
        return "x86_64"
      elif abi == "mips":
        return "mips64"
    return abi
  return None

def Get32BitArch():
  # Check for ANDROID_TOOLCHAIN_2ND_ARCH first, if set, use that.
  # If not try ANDROID_TOOLCHAIN to find the arch.
  # If this is not set, then default to arm.
  arch = GetAbiFromToolchain("ANDROID_TOOLCHAIN_2ND_ARCH", 32)
  if not arch:
    arch = GetAbiFromToolchain("ANDROID_TOOLCHAIN", 32)
    if not arch:
      return "arm"
  return arch

def Get64BitArch():
  # Check for ANDROID_TOOLCHAIN, if it is set, we can figure out the
  # arch this way. If this is not set, then default to arm64.
  arch = GetAbiFromToolchain("ANDROID_TOOLCHAIN", 64)
  if not arch:
    return "arm64"
  return arch

def SetAbi(lines):
  global ARCH

  abi_line = re.compile("ABI: \'(.*)\'")
  trace_line = re.compile("\#[0-9]+[ \t]+..[ \t]+([0-9a-f]{8}|[0-9a-f]{16})([ \t]+|$)")
  asan_trace_line = re.compile("\#[0-9]+[ \t]+0x([0-9a-f]+)[ \t]+")

  ARCH = None
  for line in lines:
    abi_match = abi_line.search(line)
    if abi_match:
      ARCH = abi_match.group(1)
      break
    trace_match = trace_line.search(line)
    if trace_match:
      # Try to guess the arch, we know the bitness.
      if len(trace_match.group(1)) == 16:
        ARCH = Get64BitArch()
      else:
        ARCH = Get32BitArch()
      break
    asan_trace_match = asan_trace_line.search(line)
    if asan_trace_match:
      # We might be able to guess the bitness by the length of the address.
      if len(asan_trace_match.group(1)) > 8:
        ARCH = Get64BitArch()
        # We know for a fact this is 64 bit, so we are done.
        break
      else:
        ARCH = Get32BitArch()
        # This might be 32 bit, or just a small address. Keep going in this
        # case, but if we couldn't figure anything else out, go with 32 bit.
  if not ARCH:
    raise Exception("Could not determine arch from input, use --arch=XXX to specify it")


class FindToolchainTests(unittest.TestCase):
  def assert_toolchain_found(self, abi):
    global ARCH
    ARCH = abi
    FindToolchain() # Will throw on failure.

  def test_toolchains_found(self):
    self.assert_toolchain_found("arm")
    self.assert_toolchain_found("arm64")
    self.assert_toolchain_found("mips")
    self.assert_toolchain_found("x86")
    self.assert_toolchain_found("x86_64")

class SetArchTests(unittest.TestCase):
  def test_abi_check(self):
    global ARCH

    SetAbi(["ABI: 'arm'"])
    self.assertEqual(ARCH, "arm")
    SetAbi(["ABI: 'arm64'"])
    self.assertEqual(ARCH, "arm64")

    SetAbi(["ABI: 'mips'"])
    self.assertEqual(ARCH, "mips")
    SetAbi(["ABI: 'mips64'"])
    self.assertEqual(ARCH, "mips64")

    SetAbi(["ABI: 'x86'"])
    self.assertEqual(ARCH, "x86")
    SetAbi(["ABI: 'x86_64'"])
    self.assertEqual(ARCH, "x86_64")

  def test_32bit_trace_line_toolchain(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/arm/arm-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "arm")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/mips/arm-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "mips")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/x86/arm-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "x86")

  def test_32bit_trace_line_toolchain_2nd(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/arm/arm-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN_ARCH"] = "linux-x86/aarch64/aarch64-linux-android-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "arm")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/mips/mips-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/unknown/unknown-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "mips")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/x86/x86-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/unknown/unknown-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "x86")

  def test_64bit_trace_line_toolchain(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/aarch/aarch-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 00000000000374e0"])
    self.assertEqual(ARCH, "arm64")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/mips/arm-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 00000000000374e0"])
    self.assertEqual(ARCH, "mips64")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/x86/arm-linux-androideabi-4.9/bin"
    SetAbi(["#00 pc 00000000000374e0"])
    self.assertEqual(ARCH, "x86_64")

  def test_trace_default_abis(self):
    global ARCH

    os.environ.clear()
    SetAbi(["#00 pc 000374e0"])
    self.assertEqual(ARCH, "arm")
    SetAbi(["#00 pc 00000000000374e0"])
    self.assertEqual(ARCH, "arm64")

  def test_32bit_asan_trace_line_toolchain(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/arm/arm-linux-androideabi-4.9/bin"
    SetAbi(["#10 0xb5eeba5d  (/system/vendor/lib/egl/libGLESv1_CM_adreno.so+0xfa5d)"])
    self.assertEqual(ARCH, "arm")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/mips/arm-linux-androideabi-4.9/bin"
    SetAbi(["#10 0xb5eeba5d  (/system/vendor/lib/egl/libGLESv1_CM_adreno.so+0xfa5d)"])
    self.assertEqual(ARCH, "mips")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/x86/arm-linux-androideabi-4.9/bin"
    SetAbi(["#10 0xb5eeba5d  (/system/vendor/lib/egl/libGLESv1_CM_adreno.so+0xfa5d)"])
    self.assertEqual(ARCH, "x86")

  def test_32bit_asan_trace_line_toolchain_2nd(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/arm/arm-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN_ARCH"] = "linux-x86/aarch64/aarch64-linux-android-4.9/bin"
    SetAbi(["#3 0xae1725b5  (/system/vendor/lib/libllvm-glnext.so+0x6435b5)"])
    self.assertEqual(ARCH, "arm")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/mips/mips-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/unknown/unknown-linux-androideabi-4.9/bin"
    SetAbi(["#3 0xae1725b5  (/system/vendor/lib/libllvm-glnext.so+0x6435b5)"])
    self.assertEqual(ARCH, "mips")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN_2ND_ARCH"] = "linux-x86/x86/x86-linux-androideabi-4.9/bin"
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/unknown/unknown-linux-androideabi-4.9/bin"
    SetAbi(["#3 0xae1725b5  (/system/vendor/lib/libllvm-glnext.so+0x6435b5)"])
    self.assertEqual(ARCH, "x86")

  def test_64bit_asan_trace_line_toolchain(self):
    global ARCH

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/aarch/aarch-linux-androideabi-4.9/bin"
    SetAbi(["#0 0x11b35d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)"])
    self.assertEqual(ARCH, "arm64")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/mips/arm-linux-androideabi-4.9/bin"
    SetAbi(["#1 0x11b35d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)"])
    self.assertEqual(ARCH, "mips64")

    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/x86/arm-linux-androideabi-4.9/bin"
    SetAbi(["#12 0x11b35d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)"])
    self.assertEqual(ARCH, "x86_64")

    # Verify that if an address that might be 32 bit comes first, that
    # encountering a 64 bit address returns a 64 bit abi.
    ARCH = None
    os.environ.clear()
    os.environ["ANDROID_TOOLCHAIN"] = "linux-x86/x86/arm-linux-androideabi-4.9/bin"
    SetAbi(["#12 0x5d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)",
            "#12 0x11b35d33bf  (/system/lib/libclang_rt.asan-arm-android.so+0x823bf)"])
    self.assertEqual(ARCH, "x86_64")

  def test_asan_trace_default_abis(self):
    global ARCH

    os.environ.clear()
    SetAbi(["#4 0x1234349ab  (/system/vendor/lib/libllvm-glnext.so+0x64fc4f)"])
    self.assertEqual(ARCH, "arm64")
    SetAbi(["#1 0xae17ec4f  (/system/vendor/lib/libllvm-glnext.so+0x64fc4f)"])
    self.assertEqual(ARCH, "arm")

  def test_no_abi(self):
    global ARCH

    self.assertRaisesRegexp(Exception, "Could not determine arch from input, use --arch=XXX to specify it", SetAbi, [])

if __name__ == '__main__':
    unittest.main()
