#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
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

"""Utility to find instrumentation test definitions from file system."""

# python imports
import os
import re

# local imports
import android_build
import android_manifest
import android_mk
import instrumentation_test
import logger


class TestWalker(object):
  """Finds instrumentation tests from filesystem."""

  def FindTests(self, path):
    """Gets list of Android instrumentation tests found at given path.

    Tests are created from the <instrumentation> tags found in
    AndroidManifest.xml files relative to the given path.

    FindTests will first scan sub-folders of path for tests. If none are found,
    it will scan the file system upwards until a AndroidManifest.xml is found
    or the Android build root is reached.

    Some sample values for path:
    - a parent directory containing many tests:
    ie development/samples will return tests for instrumentation's in ApiDemos,
    ApiDemos/tests, Notepad/tests etc
    - a java test class file
    ie ApiDemos/tests/src/../ApiDemosTest.java will return a test for
    the instrumentation in ApiDemos/tests, with the class name filter set to
    ApiDemosTest
    - a java package directory
    ie ApiDemos/tests/src/com/example/android/apis will return a test for
    the instrumentation in ApiDemos/tests, with the java package filter set
    to com.example.android.apis.

    Args:
      path: file system path to search

    Returns:
      list of test suites that support operations defined by
      test_suite.AbstractTestSuite
    """
    if not os.path.exists(path):
      logger.Log('%s does not exist' % path)
      return []
    realpath = os.path.realpath(path)
    # ensure path is in ANDROID_BUILD_ROOT
    self._build_top = os.path.realpath(android_build.GetTop())
    if not self._IsPathInBuildTree(realpath):
      logger.Log('%s is not a sub-directory of build root %s' %
                 (path, self._build_top))
      return []

    # first, assume path is a parent directory, which specifies to run all
    # tests within this directory
    tests = self._FindSubTests(realpath, [])
    if not tests:
      logger.SilentLog('No tests found within %s, searching upwards' % path)
      tests = self._FindUpstreamTests(realpath)
    return tests

  def _IsPathInBuildTree(self, path):
    """Return true if given path is within current Android build tree.

    Args:
      path: absolute file system path

    Returns:
      True if path is within Android build tree
    """
    return os.path.commonprefix([self._build_top, path]) == self._build_top

  def _MakePathRelativeToBuild(self, path):
    """Convert given path to one relative to build tree root.

    Args:
      path: absolute file system path to convert.

    Returns:
      The converted path relative to build tree root.

    Raises:
      ValueError: if path is not within build tree
    """
    if not self._IsPathInBuildTree(path):
      raise ValueError
    build_path_len = len(self._build_top) + 1
    # return string with common build_path removed
    return path[build_path_len:]

  def _FindSubTests(self, path, tests, build_path=None):
    """Recursively finds all tests within given path.

    Args:
      path: absolute file system path to check
      tests: current list of found tests
      build_path: the parent directory where Android.mk that builds sub-folders
        was found

    Returns:
      updated list of tests
    """
    if not os.path.isdir(path):
      return tests
    filenames = os.listdir(path)
    if filenames.count(android_manifest.AndroidManifest.FILENAME):
      # found a manifest! now parse it to find the test definition(s)
      manifest = android_manifest.AndroidManifest(app_path=path)
      if not build_path:
        # haven't found a parent makefile which builds this dir. Use current
        # dir as build path
        tests.extend(self._CreateSuitesFromManifest(
            manifest, self._MakePathRelativeToBuild(path)))
      else:
        tests.extend(self._CreateSuitesFromManifest(manifest, build_path))
    # Try to build as much of original path as possible, so
    # keep track of upper-most parent directory where Android.mk was found that
    # has rule to build sub-directory makefiles
    # this is also necessary in case of overlapping tests
    # ie if a test exists at 'foo' directory  and 'foo/sub', attempting to
    # build both 'foo' and 'foo/sub' will fail.
    if filenames.count(android_mk.AndroidMK.FILENAME):
      android_mk_parser = android_mk.AndroidMK(app_path=path)
      if android_mk_parser.HasInclude('call all-makefiles-under,$(LOCAL_PATH)'):
        # found rule to build sub-directories. The parent path can be used, 
        # or if not set, use current path
        if not build_path:
          build_path = self._MakePathRelativeToBuild(path)
      else:
        build_path = None
    for filename in filenames:
      self._FindSubTests(os.path.join(path, filename), tests, build_path)
    return tests

  def _FindUpstreamTests(self, path):
    """Find tests defined upward from given path.

    Args:
      path: the location to start searching. If it points to a java class file
        or java package dir, the appropriate test suite filters will be set

    Returns:
      list of test_suite.AbstractTestSuite found, may be empty
    """
    class_name_arg = None
    package_name = None
    # if path is java file, populate class name
    if self._IsJavaFile(path):
      class_name_arg = self._GetClassNameFromFile(path)
      logger.SilentLog('Using java test class %s' % class_name_arg)
    elif self._IsJavaPackage(path):
      package_name = self._GetPackageNameFromDir(path)
      logger.SilentLog('Using java package %s' % package_name)
    manifest = self._FindUpstreamManifest(path)
    if manifest:
      logger.SilentLog('Found AndroidManifest at %s' % manifest.GetAppPath())
      build_path = self._MakePathRelativeToBuild(manifest.GetAppPath())
      return self._CreateSuitesFromManifest(manifest,
                                            build_path,
                                            class_name=class_name_arg,
                                            java_package_name=package_name)

  def _IsJavaFile(self, path):
    """Returns true if given file system path is a java file."""
    return os.path.isfile(path) and self._IsJavaFileName(path)

  def _IsJavaFileName(self, filename):
    """Returns true if given file name is a java file name."""
    return os.path.splitext(filename)[1] == '.java'

  def _IsJavaPackage(self, path):
    """Returns true if given file path is a java package.

    Currently assumes if any java file exists in this directory, than it
    represents a java package.

    Args:
      path: file system path of directory to check

    Returns:
      True if path is a java package
    """
    if not os.path.isdir(path):
      return False
    for file_name in os.listdir(path):
      if self._IsJavaFileName(file_name):
        return True
    return False

  def _GetClassNameFromFile(self, java_file_path):
    """Gets the fully qualified java class name from path.

    Args:
      java_file_path: file system path of java file

    Returns:
      fully qualified java class name or None.
    """
    package_name = self._GetPackageNameFromFile(java_file_path)
    if package_name:
      filename = os.path.basename(java_file_path)
      class_name = os.path.splitext(filename)[0]
      return '%s.%s' % (package_name, class_name)
    return None

  def _GetPackageNameFromDir(self, path):
    """Gets the java package name associated with given directory path.

    Caveat: currently just parses defined java package name from first java
    file found in directory.

    Args:
      path: file system path of directory

    Returns:
      the java package name or None
    """
    for filename in os.listdir(path):
      if self._IsJavaFileName(filename):
        return self._GetPackageNameFromFile(os.path.join(path, filename))

  def _GetPackageNameFromFile(self, java_file_path):
    """Gets the java package name associated with given java file path.

    Args:
      java_file_path: file system path of java file

    Returns:
      the java package name or None
    """
    logger.SilentLog('Looking for java package name in %s' % java_file_path)
    re_package = re.compile(r'package\s+(.*);')
    file_handle = open(java_file_path, 'r')
    for line in file_handle:
      match = re_package.match(line)
      if match:
        return match.group(1)
    return None

  def _FindUpstreamManifest(self, path):
    """Recursively searches filesystem upwards for a AndroidManifest file.

    Args:
      path: file system path to search

    Returns:
      the AndroidManifest found or None
    """
    if (os.path.isdir(path) and
        os.listdir(path).count(android_manifest.AndroidManifest.FILENAME)):
      return android_manifest.AndroidManifest(app_path=path)
    dirpath = os.path.dirname(path)
    if self._IsPathInBuildTree(path):
      return self._FindUpstreamManifest(dirpath)
    logger.Log('AndroidManifest.xml not found')
    return None

  def _CreateSuitesFromManifest(self, manifest, build_path, class_name=None,
                                java_package_name=None):
    """Creates TestSuites from a AndroidManifest.

    Args:
      manifest: the AndroidManifest
      build_path: the build path to use for test
      class_name: optionally, the class filter for the suite
      java_package_name: optionally, the java package filter for the suite

    Returns:
      the list of tests created
    """
    tests = []
    for instr_name in manifest.GetInstrumentationNames():
      pkg_name = manifest.GetPackageName()
      if instr_name.find(".") < 0:
        instr_name = "." + instr_name
      logger.SilentLog('Found instrumentation %s/%s' % (pkg_name, instr_name))
      suite = instrumentation_test.InstrumentationTestSuite()
      suite.SetPackageName(pkg_name)
      suite.SetBuildPath(build_path)
      suite.SetRunnerName(instr_name)
      suite.SetName(pkg_name)
      suite.SetClassName(class_name)
      suite.SetJavaPackageFilter(java_package_name)
      # this is a bit of a hack, assume if 'com.android.cts' is in
      # package name, this is a cts test
      # this logic can be removed altogether when cts tests no longer require
      # custom build steps
      if suite.GetPackageName().startswith('com.android.cts'):
        suite.SetSuite('cts')
      tests.append(suite)
    return tests
