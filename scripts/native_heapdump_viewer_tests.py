#!/usr/bin/env python
#
# Copyright (C) 2018 The Android Open Source Project
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

"""Tests for the native_heapdump_viewer script."""

import native_heapdump_viewer
import os
import sys
import tempfile
import unittest

class NativeHeapdumpViewerTest(unittest.TestCase):
  _tmp_file_name = None

  def CreateTmpFile(self, contents):
    fd, self._tmp_file_name = tempfile.mkstemp()
    os.write(fd, contents.encode())
    os.close(fd)
    return self._tmp_file_name

  def tearDown(self):
    if self._tmp_file_name:
      try:
        os.unlink(self._tmp_file_name)
      except Exception:
        print("Failed to delete %s" % (heap))

class GetNumFieldValidTest(NativeHeapdumpViewerTest):
  _map_data = """
MAPS
1000-10000 r-xp 00000000 fd:00 495                            /data/does_not_exist.so
END
"""

  _heap_num_field_valid_version10 = """
Android Native Heap Dump v1.0

Total memory: 33800
Allocation records: 13
Backtrace size: 16

z 1  sz   1000  num    4  bt 1000 2000 3000
z 1  sz   2000  num    6  bt 1100 2100 3100
z 0  sz   1200  num    1  bt 1200 2200 3200
z 0  sz   8300  num    2  bt 1300 2300 3300
"""

  _heap_num_field_invalid_version10 = """
Android Native Heap Dump v1.0

Total memory: 12500
Allocation records: 4
Backtrace size: 16

z 1  sz   1000  num    16  bt 1000 2000 3000
z 1  sz   2000  num    16  bt 1100 2100 3100
z 0  sz   1200  num    16  bt 1200 2200 3200
z 0  sz   8300  num    16  bt 1300 2300 3300
"""

  _heap_data = """

Total memory: 200000
Allocation records: 64
Backtrace size: 16

z 1  sz   1000  num    16  bt 1000 2000 3000
z 1  sz   2000  num    16  bt 1100 2100 3100
z 0  sz   1200  num    16  bt 1200 2200 3200
z 0  sz   8300  num    16  bt 1300 2300 3300
"""

  def test_version10_valid(self):
    heap = self.CreateTmpFile(self._heap_num_field_valid_version10 + self._map_data)
    self.assertTrue(native_heapdump_viewer.GetNumFieldValid(heap))

  def test_version10_invalid(self):
    heap = self.CreateTmpFile(self._heap_num_field_invalid_version10 + self._map_data)
    self.assertFalse(native_heapdump_viewer.GetNumFieldValid(heap))

  def test_version11_valid(self):
    heap = self.CreateTmpFile("Android Native Heap Dump v1.1" + self._heap_data + self._map_data)
    self.assertTrue(native_heapdump_viewer.GetNumFieldValid(heap))

  def test_version12_valid(self):
    heap = self.CreateTmpFile("Android Native Heap Dump v1.2" + self._heap_data + self._map_data)
    self.assertTrue(native_heapdump_viewer.GetNumFieldValid(heap))

class ParseNativeHeapTest(NativeHeapdumpViewerTest):
  _backtrace_data = """
z 1  sz   1000  num    4  bt 1000 2000 3000
z 0  sz   8300  num    5  bt 1300 2300 3300
"""


  def test_backtrace_num_field_valid(self):
    heap = self.CreateTmpFile(self._backtrace_data)
    backtraces, mapppings = native_heapdump_viewer.ParseNativeHeap(heap, False, True, "")
    self.assertTrue(backtraces)
    self.assertEqual(2, len(backtraces))

    self.assertFalse(backtraces[0].is_zygote)
    self.assertEqual(1000, backtraces[0].size)
    self.assertEqual(4, backtraces[0].num_allocs)
    self.assertEqual([0x1000, 0x2000, 0x3000], backtraces[0].frames)

    self.assertTrue(backtraces[1].is_zygote)
    self.assertEqual(8300, backtraces[1].size)
    self.assertEqual(5, backtraces[1].num_allocs)
    self.assertEqual([0x1300, 0x2300, 0x3300], backtraces[1].frames)

  def test_backtrace_num_field_invalid(self):
    heap = self.CreateTmpFile(self._backtrace_data)
    backtraces, mapppings = native_heapdump_viewer.ParseNativeHeap(heap, False, False, "")
    self.assertTrue(backtraces)
    self.assertEqual(2, len(backtraces))

    self.assertFalse(backtraces[0].is_zygote)
    self.assertEqual(1000, backtraces[0].size)
    self.assertEqual(1, backtraces[0].num_allocs)
    self.assertEqual([0x1000, 0x2000, 0x3000], backtraces[0].frames)

    self.assertTrue(backtraces[1].is_zygote)
    self.assertEqual(8300, backtraces[1].size)
    self.assertEqual(1, backtraces[1].num_allocs)
    self.assertEqual([0x1300, 0x2300, 0x3300], backtraces[1].frames)

  def test_backtrace_reverse_field_valid(self):
    heap = self.CreateTmpFile(self._backtrace_data)
    backtraces, mapppings = native_heapdump_viewer.ParseNativeHeap(heap, True, True, "")
    self.assertTrue(backtraces)
    self.assertEqual(2, len(backtraces))

    self.assertFalse(backtraces[0].is_zygote)
    self.assertEqual(1000, backtraces[0].size)
    self.assertEqual(4, backtraces[0].num_allocs)
    self.assertEqual([0x3000, 0x2000, 0x1000], backtraces[0].frames)

    self.assertTrue(backtraces[1].is_zygote)
    self.assertEqual(8300, backtraces[1].size)
    self.assertEqual(5, backtraces[1].num_allocs)
    self.assertEqual([0x3300, 0x2300, 0x1300], backtraces[1].frames)

  def test_mappings(self):
    map_data = """
MAPS
1000-4000 r-xp 00000000 fd:00 495    /system/lib64/libc.so
6000-8000 r-xp 00000000 fd:00 495
a000-f000 r-xp 0000b000 fd:00 495    /system/lib64/libutils.so
END
"""

    heap = self.CreateTmpFile(map_data)
    backtraces, mappings = native_heapdump_viewer.ParseNativeHeap(heap, True, True, "")

    self.assertTrue(mappings)
    self.assertEqual(2, len(mappings))

    self.assertEqual(0x1000, mappings[0].start)
    self.assertEqual(0x4000, mappings[0].end)
    self.assertEqual(0, mappings[0].offset)
    self.assertEqual("/system/lib64/libc.so", mappings[0].name)

    self.assertEqual(0xa000, mappings[1].start)
    self.assertEqual(0xf000, mappings[1].end)
    self.assertEqual(0xb000, mappings[1].offset)
    self.assertEqual("/system/lib64/libutils.so", mappings[1].name)

if __name__ == '__main__':
  unittest.main(verbosity=2)
