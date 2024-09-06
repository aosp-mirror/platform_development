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

"""stack symbolizes native crash dumps."""

import collections
import functools
import os
import pathlib
import re
import subprocess
import symbol
import tempfile
import unittest

import example_crashes

def ConvertTrace(lines):
  tracer = TraceConverter()
  print("Reading symbols from", symbol.SYMBOLS_DIR)
  tracer.ConvertTrace(lines)

class TraceConverter:
  process_info_line = re.compile(r"(pid: [0-9]+, tid: [0-9]+.*)")
  revision_line = re.compile(r"(Revision: '(.*)')")
  signal_line = re.compile(r"(signal [0-9]+ \(.*\).*)")
  abort_message_line = re.compile(r"(Abort message: '.*')")
  thread_line = re.compile(r"(.*)(--- ){15}---")
  dalvik_jni_thread_line = re.compile("(\".*\" prio=[0-9]+ tid=[0-9]+ NATIVE.*)")
  dalvik_native_thread_line = re.compile("(\".*\" sysTid=[0-9]+ nice=[0-9]+.*)")
  register_line = re.compile("$a")
  trace_line = re.compile("$a")
  sanitizer_trace_line = re.compile("$a")
  value_line = re.compile("$a")
  code_line = re.compile("$a")
  zipinfo_central_directory_line = re.compile(r"Central\s+directory\s+entry")
  zipinfo_central_info_match = re.compile(
      r"^\s*(\S+)$\s*offset of local header from start of archive:\s*(\d+)"
      r".*^\s*compressed size:\s+(\d+)", re.M | re.S)
  unreachable_line = re.compile(r"((\d+ bytes in \d+ unreachable allocations)|"
                                r"(\d+ bytes unreachable at [0-9a-f]+)|"
                                r"(referencing \d+ unreachable bytes in \d+ allocation(s)?)|"
                                r"(and \d+ similar unreachable bytes in \d+ allocation(s)?))")
  trace_lines = []
  value_lines = []
  last_frame = -1
  width = "{8}"
  spacing = ""
  apk_info = dict()
  lib_to_path = dict()
  mte_fault_address = None
  mte_stack_records = []

  # We use the "file" command line tool to extract BuildId from ELF files.
  ElfInfo = collections.namedtuple("ElfInfo", ["bitness", "build_id"])
  readelf_output = re.compile(r"Class:\s*ELF(?P<bitness>32|64).*"
                              r"Build ID:\s*(?P<build_id>[0-9a-f]+)",
                              flags=re.DOTALL)

  def UpdateBitnessRegexes(self):
    if symbol.ARCH_IS_32BIT:
      self.width = "{8}"
      self.spacing = ""
    else:
      self.width = "{16}"
      self.spacing = "        "
    self.register_line = re.compile("    (([ ]*\\b(\S*)\\b +[0-9a-f]" + self.width + "){1,5}$)")

    # Note that both trace and value line matching allow for variable amounts of
    # whitespace (e.g. \t). This is because the we want to allow for the stack
    # tool to operate on AndroidFeedback provided system logs. AndroidFeedback
    # strips out double spaces that are found in tombsone files and logcat output.
    #
    # Examples of matched trace lines include lines from tombstone files like:
    #   #00  pc 001cf42e  /data/data/com.my.project/lib/libmyproject.so
    #
    # Or lines from AndroidFeedback crash report system logs like:
    #   03-25 00:51:05.520 I/DEBUG ( 65): #00 pc 001cf42e /data/data/com.my.project/lib/libmyproject.so
    # Please note the spacing differences.
    self.trace_line = re.compile(
        r".*"                                                 # Random start stuff.
        r"\#(?P<frame>[0-9]+)"                                # Frame number.
        r"[ \t]+..[ \t]+"                                     # (space)pc(space).
        r"(?P<offset>[0-9a-f]" + self.width + ")[ \t]+"       # Offset (hex number given without
                                                              #         0x prefix).
        r"(?P<dso>\[[^\]]+\]|[^\r\n \t]*)"                    # Library name.
        r"( \(offset (?P<so_offset>0x[0-9a-fA-F]+)\))?"       # Offset into the file to find the start of the shared so.
        r"(?P<symbolpresent> \((?P<symbol>.*?)\))?"           # Is the symbol there? (non-greedy)
        r"( \(BuildId: (?P<build_id>.*)\))?"                  # Optional build-id of the ELF file.
        r"[ \t]*$")                                           # End of line (to expand non-greedy match).
                                                              # pylint: disable-msg=C6310
    # Sanitizer output. This is different from debuggerd output, and it is easier to handle this as
    # its own regex. Example:
    # 08-19 05:29:26.283   397   403 I         :     #0 0xb6a15237  (/system/lib/libclang_rt.asan-arm-android.so+0x4f237)
    self.sanitizer_trace_line = re.compile(
        r".*"                                                 # Random start stuff.
        r"\#(?P<frame>[0-9]+)"                                # Frame number.
        r"[ \t]+0x[0-9a-f]+[ \t]+"                            # PC, not interesting to us.
        r"\("                                                 # Opening paren.
        r"(?P<dso>[^+]+)"                                     # Library name.
        r"\+"                                                 # '+'
        r"0x(?P<offset>[0-9a-f]+)"                            # Offset (hex number given with
                                                              #         0x prefix).
        r"\)")                                                # Closing paren.
                                                              # pylint: disable-msg=C6310
    # Examples of matched value lines include:
    #   bea4170c  8018e4e9  /data/data/com.my.project/lib/libmyproject.so
    #   bea4170c  8018e4e9  /data/data/com.my.project/lib/libmyproject.so (symbol)
    #   03-25 00:51:05.530 I/DEBUG ( 65): bea4170c 8018e4e9 /data/data/com.my.project/lib/libmyproject.so
    # Again, note the spacing differences.
    self.value_line = re.compile(r"(.*)([0-9a-f]" + self.width + r")[ \t]+([0-9a-f]" + self.width + r")[ \t]+([^\r\n \t]*)( \((.*)\))?")
    # Lines from 'code around' sections of the output will be matched before
    # value lines because otheriwse the 'code around' sections will be confused as
    # value lines.
    #
    # Examples include:
    #   801cf40c ffffc4cc 00b2f2c5 00b2f1c7 00c1e1a8
    #   03-25 00:51:05.530 I/DEBUG ( 65): 801cf40c ffffc4cc 00b2f2c5 00b2f1c7 00c1e1a8
    self.code_line = re.compile(r"(.*)[ \t]*[a-f0-9]" + self.width +
                                r"[ \t]*[a-f0-9]" + self.width +
                                r"[ \t]*[a-f0-9]" + self.width +
                                r"[ \t]*[a-f0-9]" + self.width +
                                r"[ \t]*[a-f0-9]" + self.width +
                                r"[ \t]*[ \r\n]")  # pylint: disable-msg=C6310
    self.mte_sync_line = re.compile(r".*signal 11 \(SIGSEGV\), code 9 \(SEGV_MTESERR\), fault addr 0x(?P<address>[0-9a-f]+)")
    self.mte_stack_record_line = re.compile(r".*stack_record fp:0x(?P<fp>[0-9a-f]+) "
                                            r"tag:0x(?P<tag>[0-9a-f]+) "
                                            r"pc:(?P<object>[^+]+)\+0x(?P<offset>[0-9a-f]+)"
                                            r"(?: \(BuildId: (?P<buildid>[A-Za-z0-9]+)\))?")

  def CleanLine(self, ln):
    # AndroidFeedback adds zero width spaces into its crash reports. These
    # should be removed or the regular expresssions will fail to match.
    return ln.encode().decode(encoding='utf8', errors='ignore')

  def PrintTraceLines(self, trace_lines):
    """Print back trace."""
    maxlen = max(len(tl[1]) for tl in trace_lines)
    print("\nStack Trace:")
    print("  RELADDR   " + self.spacing + "FUNCTION".ljust(maxlen) + "  FILE:LINE")
    for tl in self.trace_lines:
      (addr, symbol_with_offset, location) = tl
      print("  %8s  %s  %s" % (addr, symbol_with_offset.ljust(maxlen), location))

  def PrintValueLines(self, value_lines):
    """Print stack data values."""
    maxlen = max(len(tl[2]) for tl in self.value_lines)
    print("\nStack Data:")
    print("  ADDR      " + self.spacing + "VALUE     " + "FUNCTION".ljust(maxlen) + "  FILE:LINE")
    for vl in self.value_lines:
      (addr, value, symbol_with_offset, location) = vl
      print("  %8s  %8s  %s  %s" % (addr, value, symbol_with_offset.ljust(maxlen), location))

  def MatchStackRecords(self):
    if self.mte_fault_address is None:
      return
    fault_tag = (self.mte_fault_address >> 56) & 0xF
    untagged_fault_address = self.mte_fault_address & ~(0xF << 56)
    build_id_to_lib = {}
    record_for_lib = collections.defaultdict(lambda: collections.defaultdict(set))
    for lib, buildid, offset, fp, tag in self.mte_stack_records:
      if buildid is not None:
        if buildid not in build_id_to_lib:
          basename = os.path.basename(lib).split("!")[-1]
          newlib = self.GetLibraryByBuildId(symbol.SYMBOLS_DIR, basename, buildid)
          if newlib is not None:
            build_id_to_lib[buildid] = newlib
            lib = newlib
        else:
          lib = build_id_to_lib[buildid]
      record_for_lib[lib][offset].add((fp, tag))

    for lib, values in record_for_lib.items():
      records = symbol.GetStackRecordsForSet(lib, values.keys()) or []
      for addr, function_name, local_name, file_line, frame_offset, size, tag_offset in records:
        if frame_offset is None or size is None or tag_offset is None:
          continue
        for fp, tag in values[addr]:
          obj_offset = untagged_fault_address - fp - frame_offset
          if tag + tag_offset == fault_tag and obj_offset < size:
            print('')
            print('Potentially referenced stack object:')
            print('  %d bytes inside a variable "%s" in stack frame of function "%s"'% (obj_offset, local_name, function_name))
            print('  at %s' % file_line)

  def PrintOutput(self, trace_lines, value_lines):
    if self.trace_lines:
      self.PrintTraceLines(self.trace_lines)
    if self.value_lines:
      self.PrintValueLines(self.value_lines)
    if self.mte_stack_records:
      self.MatchStackRecords()

  def PrintDivider(self):
    print("\n-----------------------------------------------------\n")

  def DeleteApkTmpFiles(self):
    for _, _, tmp_files in self.apk_info.values():
      for tmp_file in tmp_files.values():
        os.unlink(tmp_file)

  def ConvertTrace(self, lines):
    lines = [self.CleanLine(line) for line in lines]
    try:
      if symbol.ARCH_IS_32BIT is None:
        symbol.SetBitness(lines)
      self.UpdateBitnessRegexes()
      for line in lines:
        self.ProcessLine(line)
      self.PrintOutput(self.trace_lines, self.value_lines)
    finally:
      # Delete any temporary files created while processing the lines.
      self.DeleteApkTmpFiles()

  def MatchTraceLine(self, line):
    match = self.trace_line.match(line)
    if match:
      return {"frame": match.group("frame"),
              "offset": match.group("offset"),
              "so_offset": match.group("so_offset"),
              "dso": match.group("dso"),
              "symbol_present": bool(match.group("symbolpresent")),
              "symbol_name": match.group("symbol"),
              "build_id": match.group("build_id")}
    match = self.sanitizer_trace_line.match(line)
    if match:
      return {"frame": match.group("frame"),
              "offset": match.group("offset"),
              "so_offset": None,
              "dso": match.group("dso"),
              "symbol_present": False,
              "symbol_name": None,
              "build_id": None}
    return None

  def ExtractLibFromApk(self, apk, shared_lib_name):
    # Create a temporary file containing the shared library from the apk.
    tmp_file = None
    try:
      tmp_fd, tmp_file = tempfile.mkstemp()
      if subprocess.call(["unzip", "-p", apk, shared_lib_name], stdout=tmp_fd) == 0:
        os.close(tmp_fd)
        shared_file = tmp_file
        tmp_file = None
        return shared_file
    finally:
      if tmp_file:
        os.close(tmp_fd)
        os.unlink(tmp_file)
    return None

  def ProcessCentralInfo(self, offset_list, central_info):
    match = self.zipinfo_central_info_match.search(central_info)
    if not match:
      raise Exception("Cannot find all info from zipinfo\n" + central_info)
    name = match.group(1)
    start = int(match.group(2))
    end = start + int(match.group(3))

    offset_list.append([name, start, end])
    return name, start, end

  def GetLibFromApk(self, apk, offset):
    # Convert the string to hex.
    offset = int(offset, 16)

    # Check if we already have information about this offset.
    if apk in self.apk_info:
      apk_full_path, offset_list, tmp_files = self.apk_info[apk]
      for file_name, start, end in offset_list:
        if offset >= start and offset < end:
          if file_name in tmp_files:
            return file_name, tmp_files[file_name]
          tmp_file = self.ExtractLibFromApk(apk_full_path, file_name)
          if tmp_file:
            tmp_files[file_name] = tmp_file
            return file_name, tmp_file
          break
      return None, None

    if not "ANDROID_PRODUCT_OUT" in os.environ:
      print("ANDROID_PRODUCT_OUT environment variable not set.")
      return None, None
    out_dir = os.environ["ANDROID_PRODUCT_OUT"]
    if not os.path.exists(out_dir):
      print("ANDROID_PRODUCT_OUT", out_dir, "does not exist.")
      return None, None
    if apk.startswith("/"):
      apk_full_path = out_dir + apk
    else:
      apk_full_path = os.path.join(out_dir, apk)
    if not os.path.exists(apk_full_path):
      print("Cannot find apk", apk)
      return None, None

    cmd = subprocess.Popen(["zipinfo", "-v", apk_full_path], stdout=subprocess.PIPE,
                           encoding='utf8')
    # Find the first central info marker.
    for line in cmd.stdout:
      if self.zipinfo_central_directory_line.search(line):
        break

    central_info = ""
    file_name = None
    offset_list = []
    for line in cmd.stdout:
      match = self.zipinfo_central_directory_line.search(line)
      if match:
        cur_name, start, end = self.ProcessCentralInfo(offset_list, central_info)
        if not file_name and offset >= start and offset < end:
          file_name = cur_name
        central_info = ""
      else:
        central_info += line
    if central_info:
      cur_name, start, end = self.ProcessCentralInfo(offset_list, central_info)
      if not file_name and offset >= start and offset < end:
        file_name = cur_name

    # Make sure the offset_list is sorted, the zip file does not guarantee
    # that the entries are in order.
    offset_list = sorted(offset_list, key=lambda entry: entry[1])

    # Save the information from the zip.
    tmp_files = dict()
    self.apk_info[apk] = [apk_full_path, offset_list, tmp_files]
    if not file_name:
      return None, None
    tmp_shared_lib = self.ExtractLibFromApk(apk_full_path, file_name)
    if tmp_shared_lib:
      tmp_files[file_name] = tmp_shared_lib
      return file_name, tmp_shared_lib
    return None, None

  # Find all files in the symbols directory and group them by basename (without directory).
  @functools.lru_cache(maxsize=None)
  def GlobSymbolsDir(self, symbols_dir):
    files_by_basename = {}
    for path in sorted(pathlib.Path(symbols_dir).glob("**/*")):
      if os.path.isfile(path):
        files_by_basename.setdefault(path.name, []).append(path)
    return files_by_basename

  # Use the "file" command line tool to find the bitness and build_id of given ELF file.
  @functools.lru_cache(maxsize=None)
  def GetLibraryInfo(self, lib):
    stdout = subprocess.check_output([symbol.ToolPath("llvm-readelf"), "-h", "-n", lib], text=True)
    match = self.readelf_output.search(stdout)
    if match:
      return self.ElfInfo(bitness=match.group("bitness"), build_id=match.group("build_id"))
    return None

  # Search for a library with the given basename and build_id anywhere in the symbols directory.
  @functools.lru_cache(maxsize=None)
  def GetLibraryByBuildId(self, symbols_dir, basename, build_id):
    for candidate in self.GlobSymbolsDir(symbols_dir).get(basename, []):
      info = self.GetLibraryInfo(candidate)
      if info and info.build_id == build_id:
        return "/" + str(candidate.relative_to(symbols_dir))
    return None

  def GetLibPath(self, lib):
    if lib in self.lib_to_path:
      return self.lib_to_path[lib]

    lib_path = self.FindLibPath(lib)
    self.lib_to_path[lib] = lib_path
    return lib_path

  def FindLibPath(self, lib):
    symbol_dir = symbol.SYMBOLS_DIR
    if os.path.isfile(symbol_dir + lib):
      return lib

    # Try and rewrite any apex files if not found in symbols.
    # For some reason, the directory in symbols does not match
    # the path on system.
    # The path is com.android.<directory> on device, but
    # com.google.android.<directory> in symbols.
    new_lib = lib.replace("/com.android.", "/com.google.android.")
    if os.path.isfile(symbol_dir + new_lib):
      return new_lib

    # When using atest, test paths are different between the out/ directory
    # and device. Apply fixups.
    if not lib.startswith("/data/local/tests/") and not lib.startswith("/data/local/tmp/"):
      print("WARNING: Cannot find %s in symbol directory" % lib)
      return lib

    test_name = lib.rsplit("/", 1)[-1]
    test_dir = "/data/nativetest"
    test_dir_bitness = ""
    if symbol.ARCH_IS_32BIT:
      bitness = "32"
    else:
      bitness = "64"
      test_dir_bitness = "64"

    # Unfortunately, the location of the real symbol file is not
    # standardized, so we need to go hunting for it.

    # This is in vendor, look for the value in:
    #   /data/nativetest{64}/vendor/test_name/test_name
    if lib.startswith("/data/local/tests/vendor/"):
      lib_path = os.path.join(test_dir + test_dir_bitness, "vendor", test_name, test_name)
      if os.path.isfile(symbol_dir + lib_path):
        return lib_path

    # Look for the path in:
    #   /data/nativetest{64}/test_name/test_name
    lib_path = os.path.join(test_dir + test_dir_bitness, test_name, test_name)
    if os.path.isfile(symbol_dir + lib_path):
      return lib_path

    # CtsXXX tests are in really non-standard locations try:
    #  /data/nativetest/{test_name}
    lib_path = os.path.join(test_dir, test_name)
    if os.path.isfile(symbol_dir + lib_path):
      return lib_path
    # Try:
    #   /data/nativetest/{test_name}{32|64}
    lib_path += bitness
    if os.path.isfile(symbol_dir + lib_path):
      return lib_path

    # Cannot find location, give up and return the original path
    print("WARNING: Cannot find %s in symbol directory" % lib)
    return lib


  def ProcessLine(self, line):
    ret = False
    process_header = self.process_info_line.search(line)
    signal_header = self.signal_line.search(line)
    abort_message_header = self.abort_message_line.search(line)
    thread_header = self.thread_line.search(line)
    register_header = self.register_line.search(line)
    revision_header = self.revision_line.search(line)
    dalvik_jni_thread_header = self.dalvik_jni_thread_line.search(line)
    dalvik_native_thread_header = self.dalvik_native_thread_line.search(line)
    unreachable_header = self.unreachable_line.search(line)
    if process_header or signal_header or abort_message_header or thread_header or \
        register_header or dalvik_jni_thread_header or dalvik_native_thread_header or \
        revision_header or unreachable_header:
      ret = True
      if self.trace_lines or self.value_lines or self.mte_stack_records:
        self.PrintOutput(self.trace_lines, self.value_lines)
        self.PrintDivider()
        self.trace_lines = []
        self.value_lines = []
        self.mte_fault_address = None
        self.mte_stack_records = []
        self.last_frame = -1
      if self.mte_sync_line.match(line):
        match = self.mte_sync_line.match(line)
        self.mte_fault_address = int(match.group("address"), 16)
      if process_header:
        print(process_header.group(1))
      if signal_header:
        print(signal_header.group(1))
      if abort_message_header:
        print(abort_message_header.group(1))
      if register_header:
        print(register_header.group(1))
      if thread_header:
        print(thread_header.group(1))
      if dalvik_jni_thread_header:
        print(dalvik_jni_thread_header.group(1))
      if dalvik_native_thread_header:
        print(dalvik_native_thread_header.group(1))
      if revision_header:
        print(revision_header.group(1))
      if unreachable_header:
        print(unreachable_header.group(1))
      return True
    trace_line_dict = self.MatchTraceLine(line)
    if trace_line_dict is not None:
      ret = True
      frame = int(trace_line_dict["frame"])
      code_addr = trace_line_dict["offset"]
      area = trace_line_dict["dso"]
      so_offset = trace_line_dict["so_offset"]
      symbol_present = trace_line_dict["symbol_present"]
      symbol_name = trace_line_dict["symbol_name"]
      build_id = trace_line_dict["build_id"]

      if frame <= self.last_frame and (self.trace_lines or self.value_lines):
        self.PrintOutput(self.trace_lines, self.value_lines)
        self.PrintDivider()
        self.trace_lines = []
        self.value_lines = []
      self.last_frame = frame

      if area == "<unknown>" or area == "[heap]" or area == "[stack]":
        self.trace_lines.append((code_addr, "", area))
      else:
        # If this is an apk, it usually means that there is actually
        # a shared so that was loaded directly out of it. In that case,
        # extract the shared library and the name of the shared library.
        lib = None
        # The format of the map name:
        #   Some.apk!libshared.so
        # or
        #   Some.apk
        if so_offset:
          # If it ends in apk, we are done.
          apk = None
          if area.endswith(".apk"):
            apk = area
          else:
            index = area.rfind(".so!")
            if index != -1:
              # Sometimes we'll see something like:
              #   #01 pc abcd  libart.so!libart.so (offset 0x134000)
              # Remove everything after the ! and zero the offset value.
              area = area[0:index + 3]
              so_offset = 0
            else:
              index = area.rfind(".apk!")
              if index != -1:
                apk = area[0:index + 4]
          if apk:
            lib_name, lib = self.GetLibFromApk(apk, so_offset)
        else:
          # Sometimes we'll see something like:
          #   #01 pc abcd  libart.so!libart.so
          # Remove everything after the !.
          index = area.rfind(".so!")
          if index != -1:
            area = area[0:index + 3]
        if not lib:
          lib = area
          lib_name = None

        if build_id:
          # If we have the build_id, do a brute-force search of the symbols directory.
          basename = os.path.basename(lib).split("!")[-1]
          lib = self.GetLibraryByBuildId(symbol.SYMBOLS_DIR, basename, build_id)
          if not lib:
            print("WARNING: Cannot find {} with build id {} in symbols directory."
                  .format(basename, build_id))
        else:
          # When using atest, test paths are different between the out/ directory
          # and device. Apply fixups.
          lib = self.GetLibPath(lib)

        # If a calls b which further calls c and c is inlined to b, we want to
        # display "a -> b -> c" in the stack trace instead of just "a -> c"
        info = symbol.SymbolInformation(lib, code_addr)
        nest_count = len(info) - 1
        for (source_symbol, source_location, symbol_with_offset) in info:
          if not source_symbol:
            if symbol_present:
              source_symbol = symbol.CallCppFilt(symbol_name)
            else:
              source_symbol = "<unknown>"
          if not symbol.VERBOSE:
            source_symbol = symbol.FormatSymbolWithoutParameters(source_symbol)
            symbol_with_offset = symbol.FormatSymbolWithoutParameters(symbol_with_offset)
          if not source_location:
            source_location = area
            if lib_name:
              source_location += "(" + lib_name + ")"
          if nest_count > 0:
            nest_count = nest_count - 1
            arrow = "v------>"
            if not symbol.ARCH_IS_32BIT:
              arrow = "v-------------->"
            self.trace_lines.append((arrow, source_symbol, source_location))
          else:
            if not symbol_with_offset:
              symbol_with_offset = source_symbol
            self.trace_lines.append((code_addr, symbol_with_offset, source_location))
    if self.code_line.match(line):
      # Code lines should be ignored. If this were exluded the 'code around'
      # sections would trigger value_line matches.
      return ret
    if self.value_line.match(line):
      ret = True
      match = self.value_line.match(line)
      (unused_, addr, value, area, symbol_present, symbol_name) = match.groups()
      if area == "<unknown>" or area == "[heap]" or area == "[stack]" or not area:
        self.value_lines.append((addr, value, "", area))
      else:
        info = symbol.SymbolInformation(area, value)
        (source_symbol, source_location, object_symbol_with_offset) = info.pop()
        # If there is no information, skip this.
        if source_symbol or source_location or object_symbol_with_offset:
          if not source_symbol:
            if symbol_present:
              source_symbol = symbol.CallCppFilt(symbol_name)
            else:
              source_symbol = "<unknown>"
          if not source_location:
            source_location = area
          if not object_symbol_with_offset:
            object_symbol_with_offset = source_symbol
          self.value_lines.append((addr,
                                   value,
                                   object_symbol_with_offset,
                                   source_location))
    if self.mte_stack_record_line.match(line):
      ret = True
      match = self.mte_stack_record_line.match(line)
      if self.mte_fault_address is not None:
        self.mte_stack_records.append(
          (match.group("object"),
           match.group("buildid"),
           int(match.group("offset"), 16),
           int(match.group("fp"), 16),
           int(match.group("tag"), 16)))

    return ret


class RegisterPatternTests(unittest.TestCase):
  def assert_register_matches(self, abi, example_crash, stupid_pattern):
    tc = TraceConverter()
    lines = example_crash.split('\n')
    symbol.SetBitness(lines)
    tc.UpdateBitnessRegexes()
    for line in lines:
      tc.ProcessLine(line)
      is_register = (re.search(stupid_pattern, line) is not None)
      matched = (tc.register_line.search(line) is not None)
      self.assertEqual(matched, is_register, line)
    tc.PrintOutput(tc.trace_lines, tc.value_lines)

  def test_arm_registers(self):
    self.assert_register_matches("arm", example_crashes.arm, '\\b(r0|r4|r8|ip|scr)\\b')

  def test_arm64_registers(self):
    self.assert_register_matches("arm64", example_crashes.arm64, '\\b(x0|x4|x8|x12|x16|x20|x24|x28|sp|v[1-3]?[0-9])\\b')

  def test_x86_registers(self):
    self.assert_register_matches("x86", example_crashes.x86, '\\b(eax|esi|xcs|eip)\\b')

  def test_x86_64_registers(self):
    self.assert_register_matches("x86_64", example_crashes.x86_64, '\\b(rax|rsi|r8|r12|cs|rip)\\b')

  def test_riscv64_registers(self):
    self.assert_register_matches("riscv64", example_crashes.riscv64, '\\b(gp|t2|t6|s3|s7|s11|a3|a7|sp)\\b')

class LibmemunreachablePatternTests(unittest.TestCase):
  def test_libmemunreachable(self):
    tc = TraceConverter()
    lines = example_crashes.libmemunreachable.split('\n')

    symbol.SetBitness(lines)
    self.assertTrue(symbol.ARCH_IS_32BIT)
    tc.UpdateBitnessRegexes()
    header_lines = 0
    trace_lines = 0
    for line in lines:
      tc.ProcessLine(line)
      if re.search(tc.unreachable_line, line) is not None:
        header_lines += 1
      if tc.MatchTraceLine(line) is not None:
        trace_lines += 1
    self.assertEqual(header_lines, 3)
    self.assertEqual(trace_lines, 2)
    tc.PrintOutput(tc.trace_lines, tc.value_lines)

class LongASANStackTests(unittest.TestCase):
  # Test that a long ASAN-style (non-padded frame numbers) stack trace is not split into two
  # when the frame number becomes two digits. This happened before as the frame number was
  # handled as a string and not converted to an integral.
  def test_long_asan_crash(self):
    tc = TraceConverter()
    lines = example_crashes.long_asan_crash.splitlines()
    symbol.SetBitness(lines)
    tc.UpdateBitnessRegexes()
    # Test by making sure trace_line_count is monotonically non-decreasing. If the stack trace
    # is split, a separator is printed and trace_lines is flushed.
    trace_line_count = 0
    for line in lines:
      tc.ProcessLine(line)
      self.assertLessEqual(trace_line_count, len(tc.trace_lines))
      trace_line_count = len(tc.trace_lines)
    # The split happened at transition of frame #9 -> #10. Make sure we have parsed (and stored)
    # more than ten frames.
    self.assertGreater(trace_line_count, 10)
    tc.PrintOutput(tc.trace_lines, tc.value_lines)

class ValueLinesTest(unittest.TestCase):
  def test_value_line_skipped(self):
    tc = TraceConverter()
    symbol.ARCH_IS_32BIT = True
    tc.UpdateBitnessRegexes()
    tc.ProcessLine("    12345678  00001000  .")
    self.assertEqual([], tc.value_lines)

if __name__ == '__main__':
    unittest.main(verbosity=2)
