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

# python imports
import os

# local imports
from abstract_test import AbstractTestSuite
import coverage
import errors
import logger


class InstrumentationTestSuite(AbstractTestSuite):
  """Represents a java instrumentation test suite definition run on Android device."""

  # for legacy reasons, the xml tag name for java (device) tests is "test:
  TAG_NAME = "test"

  _PKG_ATTR = "package"
  _RUNNER_ATTR = "runner"
  _CLASS_ATTR = "class"
  _TARGET_ATTR = "coverage_target"

  _DEFAULT_RUNNER = "android.test.InstrumentationTestRunner"

    # build path to Emma target Makefile
  _EMMA_BUILD_PATH = os.path.join("external", "emma")

  def _GetTagName(self):
    return self._TAG_NAME

  def GetPackageName(self):
    return self._GetAttribute(self._PKG_ATTR)

  def GetRunnerName(self):
    return self._GetAttribute(self._RUNNER_ATTR)

  def GetClassName(self):
    return self._GetAttribute(self._CLASS_ATTR)

  def GetTargetName(self):
    """Retrieve module that this test is targeting.

    Used for generating code coverage metrics.
    """
    return self._GetAttribute(self._TARGET_ATTR)

  def GetBuildDependencies(self, options):
    if options.coverage:
      return [self._EMMA_BUILD_PATH]
    return []

  def Parse(self, suite_element):
    super(InstrumentationTestSuite, self).Parse(suite_element)
    self._ParseAttribute(suite_element, self._PKG_ATTR, True)
    self._ParseAttribute(suite_element, self._RUNNER_ATTR, False, self._DEFAULT_RUNNER)
    self._ParseAttribute(suite_element, self._CLASS_ATTR, False)
    self._ParseAttribute(suite_element, self._TARGET_ATTR, False)

  def Run(self, options, adb):
    """Run the provided test suite.

    Builds up an adb instrument command using provided input arguments.

    Args:
      options: command line options to provide to test run
      adb: adb_interface to device under test
    """

    test_class = self.GetClassName()
    if options.test_class is not None:
      test_class = options.test_class.lstrip()
      if test_class.startswith("."):
        test_class = self.GetPackageName() + test_class
    if options.test_method is not None:
      test_class = "%s#%s" % (test_class, options.test_method)

    instrumentation_args = {}
    if test_class is not None:
      instrumentation_args["class"] = test_class
    if options.test_package:
      instrumentation_args["package"] = options.test_package
    if options.test_size:
      instrumentation_args["size"] = options.test_size
    if options.wait_for_debugger:
      instrumentation_args["debug"] = "true"
    if options.suite_assign_mode:
      instrumentation_args["suiteAssignment"] = "true"
    if options.coverage:
      instrumentation_args["coverage"] = "true"
    if options.preview:
      adb_cmd = adb.PreviewInstrumentationCommand(
          package_name=self.GetPackageName(),
          runner_name=self.GetRunnerName(),
          raw_mode=options.raw_mode,
          instrumentation_args=instrumentation_args)
      logger.Log(adb_cmd)
    elif options.coverage:
      coverage_gen = coverage.CoverageGenerator(adb)
      if not coverage_gen.TestDeviceCoverageSupport():
        raise errors.AbortError
      adb.WaitForInstrumentation(self.GetPackageName(),
                                 self.GetRunnerName())
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

      coverage_file = coverage_gen.ExtractReport(self, device_coverage_path)
      if coverage_file is not None:
        logger.Log("Coverage report generated at %s" % coverage_file)
    else:
      adb.WaitForInstrumentation(self.GetPackageName(),
                                 self.GetRunnerName())
      adb.StartInstrumentationNoResults(
          package_name=self.GetPackageName(),
          runner_name=self.GetRunnerName(),
          raw_mode=options.raw_mode,
          instrumentation_args=instrumentation_args)

  def _PrintTestResults(self, test_results):
    """Prints a summary of test result data to stdout.

    Args:
      test_results: a list of am_instrument_parser.TestResult
    """
    total_count = 0
    error_count = 0
    fail_count = 0
    for test_result in test_results:
      if test_result.GetStatusCode() == -1: # error
        logger.Log("Error in %s: %s" % (test_result.GetTestName(),
                                        test_result.GetFailureReason()))
        error_count+=1
      elif test_result.GetStatusCode() == -2: # failure
        logger.Log("Failure in %s: %s" % (test_result.GetTestName(),
                                          test_result.GetFailureReason()))
        fail_count+=1
      total_count+=1
    logger.Log("Tests run: %d, Failures: %d, Errors: %d" %
               (total_count, fail_count, error_count))
