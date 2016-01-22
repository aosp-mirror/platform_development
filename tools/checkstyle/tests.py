#!/usr/bin/python

#
# Copyright 2015, The Android Open Source Project
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
#

"""Tests the Checkstyle script used to run style checks on Java files."""

from StringIO import StringIO
import unittest
import checkstyle


TEST_RULE = u'com.puppycrawl.tools.checkstyle.checks.BANANAS'
TEST_SHA = u'0000deadbeef000000deadbeef00deadbeef0000'
TEST_ROOT = u'/usr/local/android/master/framework/support'
TEST_FILE1 = TEST_ROOT + u'/Blarg.java'
TEST_FILE2 = TEST_ROOT + u'/Blarg2.java'
TEST_FILE_NON_JAVA = TEST_ROOT + u'/blarg.cc'
FILE_ADDED = u'A '
FILE_MODIFIED = u'M '
FILE_UNTRACKED = u'??'


def mock_repository_root():
  return TEST_ROOT


def mock_last_commit():
  return TEST_SHA


def mock_modified_files_good(root, tracked_only=False, commit=None):
  if commit:
    return {TEST_FILE1: FILE_MODIFIED, TEST_FILE2: FILE_ADDED}
  return {}


def mock_modified_files_uncommitted(root, tracked_only=False, commit=None):
  if tracked_only and not commit:
    return {TEST_FILE1: FILE_MODIFIED}
  if commit:
    return {TEST_FILE1: FILE_MODIFIED, TEST_FILE2: FILE_ADDED}
  return {}


def mock_modified_files_untracked(root, tracked_only=False, commit=None):
  if not tracked_only:
    return {TEST_FILE1: FILE_UNTRACKED}
  if commit:
    return {TEST_FILE2: FILE_ADDED}
  return {}


def mock_modified_files_non_java(root, tracked_only=False, commit=None):
  if commit:
    return {TEST_FILE1: FILE_MODIFIED, TEST_FILE_NON_JAVA: FILE_ADDED}
  return {}


class TestCheckstyle(unittest.TestCase):

  def setUp(self):
    checkstyle.git.repository_root = mock_repository_root
    checkstyle.git.last_commit = mock_last_commit

  def test_ShouldSkip(self):
    self.assertFalse(checkstyle._ShouldSkip(None, 1, TEST_RULE))
    self.assertTrue(checkstyle._ShouldSkip([], 1, TEST_RULE))
    self.assertFalse(checkstyle._ShouldSkip([1], 1, TEST_RULE))
    self.assertFalse(checkstyle._ShouldSkip([1, 2, 3], 1, TEST_RULE))
    self.assertTrue(checkstyle._ShouldSkip([1, 2, 3], 4, TEST_RULE))
    for rule in checkstyle.FORCED_RULES:
      self.assertFalse(checkstyle._ShouldSkip([1, 2, 3], 1, rule))
      self.assertFalse(checkstyle._ShouldSkip([1, 2, 3], 4, rule))

  def test_GetModifiedFiles(self):
    checkstyle.git.modified_files = mock_modified_files_good
    out = StringIO()
    sha, files = checkstyle._GetModifiedFiles(out)
    output = out.getvalue()
    self.assertEqual(output, '')
    self.assertEqual(sha, TEST_SHA)
    self.assertEqual(files, {TEST_FILE1: FILE_MODIFIED, TEST_FILE2: FILE_ADDED})

  def test_GetModifiedFilesUntracked(self):
    checkstyle.git.modified_files = mock_modified_files_untracked
    out = StringIO()
    sha, files = checkstyle._GetModifiedFiles(out)
    output = out.getvalue()
    self.assertEqual(output, checkstyle.ERROR_UNTRACKED + TEST_FILE1 + '\n\n')
    self.assertEqual(sha, TEST_SHA)
    self.assertEqual(files, {TEST_FILE2: FILE_ADDED})

  def test_GetModifiedFilesUncommitted(self):
    checkstyle.git.modified_files = mock_modified_files_uncommitted
    with self.assertRaises(SystemExit):
      out = StringIO()
      checkstyle._GetModifiedFiles(out)
    self.assertEqual(out.getvalue(), checkstyle.ERROR_UNCOMMITTED)

  def test_GetModifiedFilesNonJava(self):
    checkstyle.git.modified_files = mock_modified_files_non_java
    out = StringIO()
    sha, files = checkstyle._GetModifiedFiles(out)
    output = out.getvalue()
    self.assertEqual(output, '')
    self.assertEqual(sha, TEST_SHA)
    self.assertEqual(files, {TEST_FILE1: FILE_MODIFIED})


if __name__ == '__main__':
  unittest.main()
