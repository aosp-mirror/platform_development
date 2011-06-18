#!/usr/bin/python
#
#
# Copyright 2011, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import unittest
sys.path.append('../..')

from testrunner import android_mk


class AndroidMKTest(unittest.TestCase):
  """Unit tests for AndroidMK."""

  def testHasGTest(self):
    """Test for AndroidMK.HasGTest."""
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_native.mk')
    self.assertTrue(mk_parser.HasGTest())

  def testHasGTest_lib(self):
    """Test for AndroidMK.HasGTest."""
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_gtestlib.mk')
    self.assertTrue(mk_parser.HasGTest())

  def testHasGTest_false(self):
    """Negative test for AndroidMK.HasGTest."""
    mk_parser = android_mk.CreateAndroidMK(path='.', filename='Android_java.mk')
    self.assertFalse(mk_parser.HasGTest())

  def testHasJavaLibrary(self):
    """Test for AndroidMK.HasJavaLibrary."""
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_java.mk')
    self.assertTrue(mk_parser.HasJavaLibrary('android.test.runner'))

  def testHasJavaLibrary_missing(self):
    """Negative test for AndroidMK.HasJavaLibrary.

    Test behavior when LOCAL_JAVA_LIBARIES rule is not present in makefile.
    """
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_native.mk')
    self.assertFalse(mk_parser.HasJavaLibrary('android.test.runner'))

  def testHasJavaLibrary_false(self):
    """Negative test for AndroidMK.HasJavaLibrary.

    Test behavior when LOCAL_JAVA_LIBARIES rule is present, but does not list
    given library.
    """
    mk_parser = android_mk.CreateAndroidMK(path='.', filename='Android_java.mk')
    self.assertFalse(mk_parser.HasJavaLibrary('doesntexist'))

  def testGetExpandedVariable(self):
    """Test for AndroidMK.GetExpandedVariable.
    """
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_native.mk')
    self.assertEquals('foo', mk_parser.GetExpandedVariable('LOCAL_MODULE'))

  def testGetExpandedVariable_loop(self):
    """Test for AndroidMK.GetExpandedVariable where variable expansion loops
    """
    mk_parser = android_mk.CreateAndroidMK(path='.',
                                           filename='Android_native.mk')
    try:
      mk_parser.GetExpandedVariable('recursive_var')
      self.assertTrue(False)
    except RuntimeError:
      # expected
      pass


if __name__ == '__main__':
  unittest.main()
