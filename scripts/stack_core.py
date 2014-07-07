#!/usr/bin/env python
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

import re
import symbol
import unittest

def ConvertTrace(lines):
  tracer = TraceConverter()
  print "Reading symbols from", symbol.SYMBOLS_DIR
  tracer.ConvertTrace(lines)

class TraceConverter:
  process_info_line = re.compile("(pid: [0-9]+, tid: [0-9]+.*)")
  abi_line = re.compile("(ABI: \'(.*)\')")
  revision_line = re.compile("(Revision: \'(.*)\')")
  signal_line = re.compile("(signal [0-9]+ \(.*\).*)")
  abort_message_line = re.compile("(Abort message: '.*')")
  thread_line = re.compile("(.*)(\-\-\- ){15}\-\-\-")
  dalvik_jni_thread_line = re.compile("(\".*\" prio=[0-9]+ tid=[0-9]+ NATIVE.*)")
  dalvik_native_thread_line = re.compile("(\".*\" sysTid=[0-9]+ nice=[0-9]+.*)")
  register_line = re.compile("$a")
  trace_line = re.compile("$a")
  value_line = re.compile("$a")
  code_line = re.compile("$a")
  trace_lines = []
  value_lines = []
  last_frame = -1
  width = "{8}"

  def __init__(self): pass

  register_names = {
    "arm": "r0|r1|r2|r3|r4|r5|r6|r7|r8|r9|sl|fp|ip|sp|lr|pc|cpsr",
    "arm64": "x0|x1|x2|x3|x4|x5|x6|x7|x8|x9|x10|x11|x12|x13|x14|x15|x16|x17|x18|x19|x20|x21|x22|x23|x24|x25|x26|x27|x28|x29|x30|sp|pc",
    "mips": "zr|at|v0|v1|a0|a1|a2|a3|t0|t1|t2|t3|t4|t5|t6|t7|s0|s1|s2|s3|s4|s5|s6|s7|t8|t9|k0|k1|gp|sp|s8|ra|hi|lo|bva|epc",
    "x86": "eax|ebx|ecx|edx|esi|edi|x?cs|x?ds|x?es|x?fs|x?ss|eip|ebp|esp|flags",
    "x86_64": "rax|rbx|rcx|rdx|rsi|rdi|r8|r9|r10|r11|r12|r13|r14|r15|cs|ss|rip|rbp|rsp|eflags",
  }

  def UpdateAbiRegexes(self):
    if symbol.ARCH == "arm64" or symbol.ARCH == "mips64" or symbol.ARCH == "x86_64":
      self.width = "{16}"

    self.register_line = re.compile("(([ ]*\\b(" + self.register_names[symbol.ARCH] + ")\\b +[0-9a-f]" + self.width + "){2,4})")

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
    self.trace_line = re.compile("(.*)\#([0-9]+)[ \t]+(..)[ \t]+([0-9a-f]" + self.width + ")[ \t]+([^\r\n \t]*)( \((.*)\))?")  # pylint: disable-msg=C6310
    # Examples of matched value lines include:
    #   bea4170c  8018e4e9  /data/data/com.my.project/lib/libmyproject.so
    #   bea4170c  8018e4e9  /data/data/com.my.project/lib/libmyproject.so (symbol)
    #   03-25 00:51:05.530 I/DEBUG ( 65): bea4170c 8018e4e9 /data/data/com.my.project/lib/libmyproject.so
    # Again, note the spacing differences.
    self.value_line = re.compile("(.*)([0-9a-f]" + self.width + ")[ \t]+([0-9a-f]" + self.width + ")[ \t]+([^\r\n \t]*)( \((.*)\))?")
    # Lines from 'code around' sections of the output will be matched before
    # value lines because otheriwse the 'code around' sections will be confused as
    # value lines.
    #
    # Examples include:
    #   801cf40c ffffc4cc 00b2f2c5 00b2f1c7 00c1e1a8
    #   03-25 00:51:05.530 I/DEBUG ( 65): 801cf40c ffffc4cc 00b2f2c5 00b2f1c7 00c1e1a8
    self.code_line = re.compile("(.*)[ \t]*[a-f0-9]" + self.width +
                                "[ \t]*[a-f0-9]" + self.width +
                                "[ \t]*[a-f0-9]" + self.width +
                                "[ \t]*[a-f0-9]" + self.width +
                                "[ \t]*[a-f0-9]" + self.width +
                                "[ \t]*[ \r\n]")  # pylint: disable-msg=C6310

  def CleanLine(self, ln):
    # AndroidFeedback adds zero width spaces into its crash reports. These
    # should be removed or the regular expresssions will fail to match.
    return unicode(ln, errors='ignore')

  def PrintTraceLines(self, trace_lines):
    """Print back trace."""
    maxlen = max(map(lambda tl: len(tl[1]), trace_lines))
    spacing = ""
    if symbol.ARCH == "arm64" or symbol.ARCH == "mips64" or symbol.ARCH == "x86_64":
      spacing = "        "
    print
    print "Stack Trace:"
    print "  RELADDR   " + spacing + "FUNCTION".ljust(maxlen) + "  FILE:LINE"
    for tl in self.trace_lines:
      (addr, symbol_with_offset, location) = tl
      print "  %8s  %s  %s" % (addr, symbol_with_offset.ljust(maxlen), location)
    return

  def PrintValueLines(self, value_lines):
    """Print stack data values."""
    maxlen = max(map(lambda tl: len(tl[2]), self.value_lines))
    print
    print "Stack Data:"
    print "  ADDR      VALUE     " + "FUNCTION".ljust(maxlen) + "  FILE:LINE"
    for vl in self.value_lines:
      (addr, value, symbol_with_offset, location) = vl
      print "  %8s  %8s  %s  %s" % (addr, value, symbol_with_offset.ljust(maxlen), location)
    return

  def PrintOutput(self, trace_lines, value_lines):
    if self.trace_lines:
      self.PrintTraceLines(self.trace_lines)
    if self.value_lines:
      self.PrintValueLines(self.value_lines)

  def PrintDivider(self):
    print
    print "-----------------------------------------------------\n"

  def ConvertTrace(self, lines):
    lines = map(self.CleanLine, lines)
    for line in lines:
      self.ProcessLine(line)
    self.PrintOutput(self.trace_lines, self.value_lines)

  def ProcessLine(self, line):
    process_header = self.process_info_line.search(line)
    signal_header = self.signal_line.search(line)
    abort_message_header = self.abort_message_line.search(line)
    thread_header = self.thread_line.search(line)
    register_header = self.register_line.search(line)
    abi_header = self.abi_line.search(line)
    revision_header = self.revision_line.search(line)
    dalvik_jni_thread_header = self.dalvik_jni_thread_line.search(line)
    dalvik_native_thread_header = self.dalvik_native_thread_line.search(line)
    if process_header or signal_header or abort_message_header or thread_header or abi_header or \
        register_header or dalvik_jni_thread_header or dalvik_native_thread_header or revision_header:
      if self.trace_lines or self.value_lines:
        self.PrintOutput(self.trace_lines, self.value_lines)
        self.PrintDivider()
        self.trace_lines = []
        self.value_lines = []
        self.last_frame = -1
      if process_header:
        print process_header.group(1)
      if signal_header:
        print signal_header.group(1)
      if abort_message_header:
        print abort_message_header.group(1)
      if register_header:
        print register_header.group(1)
      if thread_header:
        print thread_header.group(1)
      if dalvik_jni_thread_header:
        print dalvik_jni_thread_header.group(1)
      if dalvik_native_thread_header:
        print dalvik_native_thread_header.group(1)
      if revision_header:
        print revision_header.group(1)
      if abi_header:
        print abi_header.group(1)
        symbol.ARCH = abi_header.group(2)
        self.UpdateAbiRegexes()
      return
    if self.trace_line.match(line):
      match = self.trace_line.match(line)
      (unused_0, frame, unused_1,
       code_addr, area, symbol_present, symbol_name) = match.groups()

      if frame <= self.last_frame and (self.trace_lines or self.value_lines):
        self.PrintOutput(self.trace_lines, self.value_lines)
        self.PrintDivider()
        self.trace_lines = []
        self.value_lines = []
      self.last_frame = frame

      if area == "<unknown>" or area == "[heap]" or area == "[stack]":
        self.trace_lines.append((code_addr, "", area))
      else:
        # If a calls b which further calls c and c is inlined to b, we want to
        # display "a -> b -> c" in the stack trace instead of just "a -> c"
        info = symbol.SymbolInformation(area, code_addr)
        nest_count = len(info) - 1
        for (source_symbol, source_location, object_symbol_with_offset) in info:
          if not source_symbol:
            if symbol_present:
              source_symbol = symbol.CallCppFilt(symbol_name)
            else:
              source_symbol = "<unknown>"
          if not source_location:
            source_location = area
          if nest_count > 0:
            nest_count = nest_count - 1
            arrow = "v------>"
            if symbol.ARCH == "arm64" or symbol.ARCH == "mips64" or symbol.ARCH == "x86_64":
              arrow = "v-------------->"
            self.trace_lines.append((arrow, source_symbol, source_location))
          else:
            if not object_symbol_with_offset:
              object_symbol_with_offset = source_symbol
            self.trace_lines.append((code_addr,
                                object_symbol_with_offset,
                                source_location))
    if self.code_line.match(line):
      # Code lines should be ignored. If this were exluded the 'code around'
      # sections would trigger value_line matches.
      return
    if self.value_line.match(line):
      match = self.value_line.match(line)
      (unused_, addr, value, area, symbol_present, symbol_name) = match.groups()
      if area == "<unknown>" or area == "[heap]" or area == "[stack]" or not area:
        self.value_lines.append((addr, value, "", area))
      else:
        info = symbol.SymbolInformation(area, value)
        (source_symbol, source_location, object_symbol_with_offset) = info.pop()
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

    #self.PrintOutput(self.trace_lines, self.value_lines)


example_arm_crash = """
Build fingerprint: 'google/volantis/flounder:L/AAV73D/1227711:userdebug/dev-keys'
Revision: '0'
ABI: 'arm'
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
    r0 00000000  r1 00002dd9  r2 00000006  r3 00000000
    r4 f710edd8  r5 00000006  r6 00000000  r7 0000010c
    r8 f71b9df4  r9 ab0b5028  sl f7175695  fp f710edd0
    ip 00002dd9  sp f710ed18  lr f7175ef1  pc f719a4e0  cpsr 60070010
    d0  ffffffffffffffff  d1  0000000000000031
    d2  0000000000000037  d3  0000000000000033
    d4  0000000000000000  d5  0000000000000000
    d6  0000000000000000  d7  0000000000000000
    d8  0000000000000000  d9  0000000000000000
    d10 0000000000000000  d11 0000000000000000
    d12 0000000000000000  d13 0000000000000000
    d14 0000000000000000  d15 0000000000000000
    d16 0000000000000000  d17 0000000000000fff
    d18 0000000000000000  d19 0000000000000000
    d20 0000000000000000  d21 0000000000000000
    d22 0000000000000000  d23 0000000000000000
    d24 0000000000000000  d25 0000000000000000
    d26 0000000000000000  d27 0000000000000000
    d28 0000000000000000  d29 0000000000000000
    d30 0000000000000000  d31 0000000000000000
    scr 00000000

backtrace:
    #00 pc 000374e0  /system/lib/libc.so (tgkill+12)
    #01 pc 00012eed  /system/lib/libc.so (pthread_kill+52)
    #02 pc 00013997  /system/lib/libc.so (raise+10)
    #03 pc 0001047d  /system/lib/libc.so (__libc_android_abort+36)
    #04 pc 0000eb1c  /system/lib/libc.so (abort+4)
    #05 pc 00000c6f  /system/xbin/crasher
    #06 pc 000126b3  /system/lib/libc.so (__pthread_start(void*)+30)
    #07 pc 000107fb  /system/lib/libc.so (__start_thread+6)
"""

example_arm64_crash = """
Build fingerprint: 'google/volantis/flounder:L/AAV73D/1227711:userdebug/dev-keys'
Revision: '0'
ABI: 'arm64'
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
    x0   0000000000000000  x1   0000000000002df1  x2   0000000000000006  x3   000000559dc73040
    x4   ffffffffffffffff  x5   0000000000000005  x6   0000000000000001  x7   0000000000000020
    x8   0000000000000083  x9   0000005563d21000  x10  0101010101010101  x11  0000000000000001
    x12  0000000000000001  x13  0000005563d21000  x14  0000005563d21000  x15  0000000000000000
    x16  0000005563d32f20  x17  0000000000000001  x18  0000000000000000  x19  000000559dc73040
    x20  0000007f844dcbb0  x21  0000007f84639000  x22  0000000000000000  x23  0000000000000006
    x24  0000007f845b2000  x25  0000007ff8f33bc0  x26  0000007f843df000  x27  000000559dc730c0
    x28  0000007f84639788  x29  0000007f844dc9c0  x30  0000007f845b38c4
    sp   0000007f844dc9c0  pc   0000007f845f28e0
    v0   2f2f2f2f2f2f2f2f  v1   5f6474656e62696c  v2   000000000000006f  v3   0000000000000000
    v4   8020080280200800  v5   0000000000000000  v6   0000000000000000  v7   8020080280200802
    v8   0000000000000000  v9   0000000000000000  v10  0000000000000000  v11  0000000000000000
    v12  0000000000000000  v13  0000000000000000  v14  0000000000000000  v15  0000000000000000
    v16  4010040140100401  v17  0000aaa800000000  v18  8020080280200800  v19  0000000000000000
    v20  0000000000000000  v21  0000000000000000  v22  0000000000000000  v23  0000000000000000
    v24  0000000000000000  v25  0000000000000000  v26  0000000000000000  v27  0000000000000000
    v28  0000000000000000  v29  0000000000000000  v30  0000000000000000  v31  0000000000000000

backtrace:
    #00 pc 00000000000588e0  /system/lib64/libc.so (tgkill+8)
    #01 pc 00000000000198c0  /system/lib64/libc.so (pthread_kill+160)
    #02 pc 000000000001ab34  /system/lib64/libc.so (raise+28)
    #03 pc 00000000000148bc  /system/lib64/libc.so (abort+60)
    #04 pc 00000000000016e0  /system/xbin/crasher64
    #05 pc 00000000000017f0  /system/xbin/crasher64
    #06 pc 0000000000018958  /system/lib64/libc.so (__pthread_start(void*)+52)
    #07 pc 0000000000014e90  /system/lib64/libc.so (__start_thread+16)
"""

example_mips_crash = """
Build fingerprint: 'Android/aosp_mips/generic_mips:4.4.3.43.43.43/AOSP/enh06302258:eng/test-keys'
Revision: '0'
ABI: 'mips'
pid: 958, tid: 960, name: crasher  >>> crasher <<<
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
 zr 00000000  at 802babc0  v0 00000000  v1 77b99dd0
 a0 000003be  a1 000003c0  a2 00000006  a3 00000000
 t0 00000000  t1 9e7f5440  t2 00000020  t3 ffffff18
 t4 77a9c000  t5 00000001  t6 00000000  t7 00000000
 s0 000003c0  s1 77b99dd8  s2 00000000  s3 00000006
 s4 77db2028  s5 000003be  s6 77c39fa8  s7 77b99dd0
 t8 00000000  t9 77c89e80  k0 00000000  k1 00000000
 gp 77cce350  sp 77b99c78  s8 77db2020  ra 77c3b48c
 hi 00000000  lo 00000008 bva 7fff7008 epc 77c89e94

backtrace:
    #00 pc 00067e94  /system/lib/libc.so (tgkill+20)
    #01 pc 0001948c  /system/lib/libc.so (pthread_kill+244)
    #02 pc 0001b0e8  /system/lib/libc.so (raise+60)
    #03 pc 00012908  /system/lib/libc.so (abort+104)
    #04 pc 000012a4  /system/xbin/crasher
    #05 pc 00018008  /system/lib/libc.so (__pthread_start(void*)+96)
    #06 pc 00013198  /system/lib/libc.so (__start_thread+36)
"""

example_x86_crash = """
Build fingerprint: 'Android/aosp_x86_64/generic_x86_64:4.4.3.43.43.43/AOSP/enh06301456:eng/test-keys'
Revision: '0'
ABI: 'x86'
pid: 1566, tid: 1568, name: crasher  >>> crasher <<<
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
    eax 00000000  ebx 0000061e  ecx 00000620  edx 00000006
    esi f7679dd8  edi 00000000
    xcs 00000023  xds 0000002b  xes 0000002b  xfs 00000003  xss 0000002b
    eip f7758ea6  ebp 00000620  esp f7679c60  flags 00000282

backtrace:
    #00 pc 00076ea6  /system/lib/libc.so (tgkill+22)
    #01 pc 0001dc8b  /system/lib/libc.so (pthread_kill+155)
    #02 pc 0001f294  /system/lib/libc.so (raise+36)
    #03 pc 00017a04  /system/lib/libc.so (abort+84)
    #04 pc 00001099  /system/xbin/crasher
    #05 pc 0001cd58  /system/lib/libc.so (__pthread_start(void*)+56)
    #06 pc 00018169  /system/lib/libc.so (__start_thread+25)
    #07 pc 0000ed76  /system/lib/libc.so (__bionic_clone+70)
"""

example_x86_64_crash = """
Build fingerprint: 'Android/aosp_x86_64/generic_x86_64:4.4.3.43.43.43/AOSP/enh06301456:eng/test-keys'
Revision: '0'
ABI: 'x86_64'
pid: 1608, tid: 1610, name: crasher64  >>> crasher64 <<<
signal 6 (SIGABRT), code -6 (SI_TKILL), fault addr --------
    rax 0000000000000000  rbx 000000000000064a  rcx ffffffffffffffff  rdx 0000000000000006
    rsi 000000000000064a  rdi 0000000000000648
    r8  0000000000000001  r9  00007fe218110c98  r10 0000000000000008  r11 0000000000000206
    r12 0000000000000000  r13 0000000000000006  r14 00007fe218111ba0  r15 0000000000000648
    cs  0000000000000033  ss  000000000000002b
    rip 00007fe218201807  rbp 00007fe218111bb0  rsp 00007fe218111a18  eflags 0000000000000206

backtrace:
    #00 pc 0000000000077807  /system/lib64/libc.so (tgkill+7)
    #01 pc 000000000002243f  /system/lib64/libc.so (pthread_kill+143)
    #02 pc 0000000000023551  /system/lib64/libc.so (raise+17)
    #03 pc 000000000001ce6d  /system/lib64/libc.so (abort+61)
    #04 pc 0000000000001385  /system/xbin/crasher64
    #05 pc 00000000000014a8  /system/xbin/crasher64
    #06 pc 00000000000215ae  /system/lib64/libc.so (__pthread_start(void*)+46)
    #07 pc 000000000001d3eb  /system/lib64/libc.so (__start_thread+11)
    #08 pc 00000000000138f5  /system/lib64/libc.so (__bionic_clone+53)
"""


class RegisterPatternTests(unittest.TestCase):
  def assert_register_matches(self, abi, example_crash, stupid_pattern):
    tc = TraceConverter()
    symbol.ARCH = abi
    tc.UpdateAbiRegexes()
    for line in example_crash.split('\n'):
      is_register = (re.search(stupid_pattern, line) is not None)
      matched = (tc.register_line.search(line) is not None)
      self.assertEquals(matched, is_register, line)

  def test_arm_registers(self):
    self.assert_register_matches("arm", example_arm_crash, '\\b(r0|r4|r8|ip)\\b')

  def test_arm64_registers(self):
    self.assert_register_matches("arm64", example_arm64_crash, '\\b(x0|x4|x8|x12|x16|x20|x24|x28|sp)\\b')

  def test_mips_registers(self):
    self.assert_register_matches("mips", example_mips_crash, '\\b(zr|a0|t0|t4|s0|s4|t8|gp|hi)\\b')

  def test_x86_registers(self):
    self.assert_register_matches("x86", example_x86_crash, '\\b(eax|esi|xcs|eip)\\b')

  def test_x86_64_registers(self):
    self.assert_register_matches("x86_64", example_x86_64_crash, '\\b(rax|rsi|r8|r12|cs|rip)\\b')


if __name__ == '__main__':
    unittest.main()
