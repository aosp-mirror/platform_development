#!/usr/bin/python2.4
#
#
# Copyright 2008, The Android Open Source Project
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

"""TestSuite definition for Android instrumentation tests."""

import os
import re

# local imports
import android_manifest
from coverage import coverage
import errors
import logger
import test_suite


class InstrumentationTestSuite(test_suite.AbstractTestSuite):
  """Represents a java instrumentation test suite definition run on device."""

  DEFAULT_RUNNER = "android.test.InstrumentationTestRunner"

  def __init__(self):
    test_suite.AbstractTestSuite.__init__(self)
    self._package_name = None
    self._runner_name = self.DEFAULT_RUNNER
    self._class_name = None
    self._target_name = None
    self._java_package = None

  def GetPackageName(self):
    return self._package_name

  def SetPackageName(self, package_name):
    self._package_name = package_name
    return self

  def GetRunnerName(self):
    return self._runner_name

  def SetRunnerName(self, runner_name):
    self._runner_name = runner_name
    return self

  def GetClassName(self):
    return self._class_name

  def SetClassName(self, class_name):
    self._class_name = class_name
    return self

  def GetJavaPackageFilter(self):
    return self._java_package

  def SetJavaPackageFilter(self, java_package_name):
    """Configure the suite to only run tests in given java package."""
    self._java_package = java_package_name
    return self

  def GetTargetName(self):
    """Retrieve module that this test is targeting.

    Used for generating code coverage metrics.
    Returns:
      the module target name
    """
    return self._target_name

  def SetTargetName(self, target_name):
    self._target_name = target_name
    return self

  def GetBuildDependencies(self, options):
    if options.coverage_target_path:
      return [options.coverage_target_path]
    return []

  def Run(self, options, adb):
    """Run the provided test suite.

    Builds up an adb instrument command using provided input arguments.

    Args:
      options: command line options to provide to test run
      adb: adb_interface to device under test

    Raises:
      errors.AbortError: if fatal error occurs
    """

    test_class = self.GetClassName()
    if options.test_class is not None:
      test_class = options.test_class.lstrip()
      if test_class.startswith("."):
        test_class = self.GetPackageName() + test_class
    if options.test_method is not None:
      test_class = "%s#%s" % (test_class, options.test_method)

    test_package = self.GetJavaPackageFilter()
    if options.test_package:
      test_package = options.test_package

    if test_class and test_package:
      logger.Log('Error: both class and java package options are specified')

    instrumentation_args = {}
    if test_class is not None:
      instrumentation_args["class"] = test_class
    if test_package:
      instrumentation_args["package"] = test_package
    if options.test_size:
      instrumentation_args["size"] = options.test_size
    if options.wait_for_debugger:
      instrumentation_args["debug"] = "true"
    if options.suite_assign_mode:
      instrumentation_args["suiteAssignment"] = "true"
    if options.coverage:
      instrumentation_args["coverage"] = "true"
    if options.test_annotation:
      instrumentation_args["annotation"] = options.test_annotation
    if options.test_not_annotation:
      instrumentation_args["notAnnotation"] = options.test_not_annotation
    if options.preview:
      adb_cmd = adb.PreviewInstrumentationCommand(
          package_name=self.GetPackageName(),
          runner_name=self.GetRunnerName(),
          raw_mode=options.raw_mode,
          instrumentation_args=instrumentation_args)
      logger.Log(adb_cmd)
    elif options.coverage:
      coverage_gen = coverage.CoverageGenerator(adb)
      if options.coverage_target_path:
        coverage_target = coverage_gen.GetCoverageTargetForPath(options.coverage_target_path)
      elif self.GetTargetName():
        coverage_target = coverage_gen.GetCoverageTarget(self.GetTargetName())
      self._CheckInstrumentationInstalled(adb)
      # need to parse test output to determine path to coverage file
      logger.Log("Running in coverage mode, suppressing test output")
      try:
        (test_results, status_map) = adb.StartInstrumentationForPackage(
            package_name=self.GetPackageName(),
            runner_name=self.GetRunnerName(),
            timeout_time=60*60,
            instrumentation_args=instrumentation_args)
      except errors.InstrumentationError, errors.DeviceUnresponsiveError:
        return
      self._PrintTestResults(test_results)
      device_coverage_path = status_map.get("coverageFilePath", None)
      if device_coverage_path is None:
        logger.Log("Error: could not find coverage data on device")
        return

      coverage_file = coverage_gen.ExtractReport(
          self.GetName(), coverage_target, device_coverage_path,
          test_qualifier=options.test_size)
      if coverage_file is not None:
        logger.Log("Coverage report generated at %s" % coverage_file)

    else:
      self._CheckInstrumentationInstalled(adb)
      adb.StartInstrumentationNoResults(package_name=self.GetPackageName(),
                                        runner_name=self.GetRunnerName(),
                                        raw_mode=options.raw_mode,
                                        instrumentation_args=
                                        instrumentation_args)

  def _CheckInstrumentationInstalled(self, adb):
    if not adb.IsInstrumentationInstalled(self.GetPackageName(),
                                          self.GetRunnerName()):
      msg=("Could not find instrumentation %s/%s on device. Try forcing a "
           "rebuild by updating a source file, and re-executing runtest." %
           (self.GetPackageName(), self.GetRunnerName()))
      raise errors.AbortError(msg=msg)

  def _PrintTestResults(self, test_results):
    """Prints a summary of test result data to stdout.

    Args:
      test_results: a list of am_instrument_parser.TestResult
    """
    total_count = 0
    error_count = 0
    fail_count = 0
    for test_result in test_results:
      if test_result.GetStatusCode() == -1:  # error
        logger.Log("Error in %s: %s" % (test_result.GetTestName(),
                                        test_result.GetFailureReason()))
        error_count+=1
      elif test_result.GetStatusCode() == -2:  # failure
        logger.Log("Failure in %s: %s" % (test_result.GetTestName(),
                                          test_result.GetFailureReason()))
        fail_count+=1
      total_count+=1
    logger.Log("Tests run: %d, Failures: %d, Errors: %d" %
               (total_count, fail_count, error_count))

def HasInstrumentationTest(path):
  """Determine if given path defines an instrumentation test.

  Args:
    path: file system path to instrumentation test.
  """
  manifest_parser = android_manifest.CreateAndroidManifest(path)
  if manifest_parser:
    return manifest_parser.GetInstrumentationNames()
  return False

class InstrumentationTestFactory(test_suite.AbstractTestFactory):
  """A factory for creating InstrumentationTestSuites"""

  def __init__(self, test_root_path, build_path):
    test_suite.AbstractTestFactory.__init__(self, test_root_path,
                                            build_path)

  def CreateTests(self, sub_tests_path=None):
    """Create tests found in test_path.

    Will create a single InstrumentationTestSuite based on info found in
    AndroidManifest.xml found at build_path. Will set additional filters if
    test_path refers to a java package or java class.
    """
    tests = []
    class_name_arg = None
    java_package_name = None
    if sub_tests_path:
      # if path is java file, populate class name
      if self._IsJavaFile(sub_tests_path):
        class_name_arg = self._GetClassNameFromFile(sub_tests_path)
        logger.SilentLog('Using java test class %s' % class_name_arg)
      elif self._IsJavaPackage(sub_tests_path):
        java_package_name = self._GetPackageNameFromDir(sub_tests_path)
        logger.SilentLog('Using java package %s' % java_package_name)
    try:
      manifest_parser = android_manifest.AndroidManifest(app_path=
                                                         self.GetTestsRootPath())
      instrs = manifest_parser.GetInstrumentationNames()
      if not instrs:
        logger.Log('Could not find instrumentation declarations in %s at %s' %
                   (android_manifest.AndroidManifest.FILENAME,
                    self.GetBuildPath()))
        return tests
      elif len(instrs) > 1:
        logger.Log("Found multiple instrumentation declarations in %s/%s. "
                   "Only using first declared." %
                   (self.GetBuildPath(),
                    android_manifest.AndroidManifest.FILENAME))
      instr_name = manifest_parser.GetInstrumentationNames()[0]
      # escape inner class names
      instr_name = instr_name.replace('$', '\$')
      pkg_name = manifest_parser.GetPackageName()
      if instr_name.find(".") < 0:
        instr_name = "." + instr_name
      logger.SilentLog('Found instrumentation %s/%s' % (pkg_name, instr_name))
      suite = InstrumentationTestSuite()
      suite.SetPackageName(pkg_name)
      suite.SetBuildPath(self.GetBuildPath())
      suite.SetRunnerName(instr_name)
      suite.SetName(pkg_name)
      suite.SetClassName(class_name_arg)
      suite.SetJavaPackageFilter(java_package_name)
      # this is a bit of a hack, assume if 'com.android.cts' is in
      # package name, this is a cts test
      # this logic can be removed altogether when cts tests no longer require
      # custom build steps
      if suite.GetPackageName().startswith('com.android.cts'):
        suite.SetSuite('cts')
      tests.append(suite)
      return tests

    except:
      logger.Log('Could not find or parse %s at %s' %
                 (android_manifest.AndroidManifest.FILENAME,
                  self.GetBuildPath()))
    return tests

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
