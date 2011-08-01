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

# local imports
import android_build
import android_mk
import gtest
import instrumentation_test
import logger


class TestWalker(object):
  """Finds Android tests from filesystem."""

  def FindTests(self, path):
    """Gets list of Android tests found at given path.

    Tests are created from info found in Android.mk and AndroidManifest.xml
    files relative to the given path.

    Currently supported tests are:
    - Android application tests run via instrumentation
    - native C/C++ tests using GTest framework. (note Android.mk must follow
      expected GTest template)

    FindTests will first scan sub-folders of path for tests. If none are found,
    it will scan the file system upwards until a valid test Android.mk is found
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

    TODO: add GTest examples

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

  def _FindSubTests(self, path, tests, upstream_build_path=None):
    """Recursively finds all tests within given path.

    Args:
      path: absolute file system path to check
      tests: current list of found tests
      upstream_build_path: the parent directory where Android.mk that builds
        sub-folders was found

    Returns:
      updated list of tests
    """
    if not os.path.isdir(path):
      return tests
    android_mk_parser = android_mk.CreateAndroidMK(path)
    if android_mk_parser:
      build_rel_path = self._MakePathRelativeToBuild(path)
      if not upstream_build_path:
        # haven't found a parent makefile which builds this dir. Use current
        # dir as build path
        tests.extend(self._CreateSuites(
            android_mk_parser, path, build_rel_path))
      else:
        tests.extend(self._CreateSuites(android_mk_parser, path,
                                        upstream_build_path))
      # Try to build as much of original path as possible, so
      # keep track of upper-most parent directory where Android.mk was found
      # that has rule to build sub-directory makefiles.
      # this is also necessary in case of overlapping tests
      # ie if a test exists at 'foo' directory  and 'foo/sub', attempting to
      # build both 'foo' and 'foo/sub' will fail.

      if android_mk_parser.HasInclude('call all-makefiles-under,$(LOCAL_PATH)'):
        # found rule to build sub-directories. The parent path can be used,
        # or if not set, use current path
        if not upstream_build_path:
          upstream_build_path = self._MakePathRelativeToBuild(path)
      else:
        upstream_build_path = None
    for filename in os.listdir(path):
      self._FindSubTests(os.path.join(path, filename), tests,
                         upstream_build_path)
    return tests

  def _FindUpstreamTests(self, path):
    """Find tests defined upward from given path.

    Args:
      path: the location to start searching.

    Returns:
      list of test_suite.AbstractTestSuite found, may be empty
    """
    factory = self._FindUpstreamTestFactory(path)
    if factory:
      return factory.CreateTests(sub_tests_path=path)
    else:
      return []

  def _GetTestFactory(self, android_mk_parser, path, build_path):
    """Get the test factory for given makefile.

    If given path is a valid tests build path, will return the TestFactory
    for creating tests.

    Args:
      android_mk_parser: the android mk to evaluate
      path: the filesystem path of the makefile
      build_path: filesystem path for the directory
      to build when running tests, relative to source root.

    Returns:
      the TestFactory or None if path is not a valid tests build path
    """
    if android_mk_parser.HasGTest():
      return gtest.GTestFactory(path, build_path)
    elif instrumentation_test.HasInstrumentationTest(path):
      return instrumentation_test.InstrumentationTestFactory(path,
          build_path)
    else:
      # somewhat unusual, but will continue searching
      logger.SilentLog('Found makefile at %s, but did not detect any tests.'
                       % path)

    return None

  def _GetTestFactoryForPath(self, path):
    """Get the test factory for given path.

    If given path is a valid tests build path, will return the TestFactory
    for creating tests.

    Args:
      path: the filesystem path to evaluate

    Returns:
      the TestFactory or None if path is not a valid tests build path
    """
    android_mk_parser = android_mk.CreateAndroidMK(path)
    if android_mk_parser:
      build_path = self._MakePathRelativeToBuild(path)
      return self._GetTestFactory(android_mk_parser, path, build_path)
    else:
      return None

  def _FindUpstreamTestFactory(self, path):
    """Recursively searches filesystem upwards for a test factory.

    Args:
      path: file system path to search

    Returns:
      the TestFactory found or None
    """
    factory = self._GetTestFactoryForPath(path)
    if factory:
      return factory
    dirpath = os.path.dirname(path)
    if self._IsPathInBuildTree(path):
      return self._FindUpstreamTestFactory(dirpath)
    logger.Log('A tests Android.mk was not found')
    return None

  def _CreateSuites(self, android_mk_parser, path, upstream_build_path):
    """Creates TestSuites from a AndroidMK.

    Args:
      android_mk_parser: the AndroidMK
      path: absolute file system path of the makefile to evaluate
      upstream_build_path: the build path to use for test. This can be
        different than the 'path', in cases where an upstream makefile
        is being used.

    Returns:
      the list of tests created
    """
    factory = self._GetTestFactory(android_mk_parser, path,
                                   build_path=upstream_build_path)
    if factory:
      return factory.CreateTests(path)
    else:
      return []
