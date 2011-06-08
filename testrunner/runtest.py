#!/usr/bin/python2.4
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

"""Command line utility for running Android tests

runtest helps automate the instructions for building and running tests
- It builds the corresponding test package for the code you want to test
- It pushes the test package to your device or emulator
- It launches InstrumentationTestRunner (or similar) to run the tests you
specify.

runtest supports running tests whose attributes have been pre-defined in
_TEST_FILE_NAME files, (runtest <testname>), or by specifying the file
system path to the test to run (runtest --path <path>).

Do runtest --help to see full list of options.
"""

# Python imports
import glob
import optparse
import os
from sets import Set
import sys
import time

# local imports
import adb_interface
import android_build
import coverage
import errors
import logger
import run_command
from test_defs import test_defs
from test_defs import test_walker


class TestRunner(object):
  """Command line utility class for running pre-defined Android test(s)."""

  _TEST_FILE_NAME = "test_defs.xml"

  # file path to android core platform tests, relative to android build root
  # TODO move these test data files to another directory
  _CORE_TEST_PATH = os.path.join("development", "testrunner",
                                 _TEST_FILE_NAME)

  # vendor glob file path patterns to tests, relative to android
  # build root
  _VENDOR_TEST_PATH = os.path.join("vendor", "*", "tests", "testinfo",
                                   _TEST_FILE_NAME)

  _RUNTEST_USAGE = (
      "usage: runtest.py [options] short-test-name[s]\n\n"
      "The runtest script works in two ways.  You can query it "
      "for a list of tests, or you can launch one or more tests.")

  # default value for make -jX
  _DEFAULT_JOBS = 4

  _DALVIK_VERIFIER_OFF_PROP = "dalvik.vm.dexopt-flags = v=n"

  def __init__(self):
    # disable logging of timestamp
    self._root_path = android_build.GetTop()
    logger.SetTimestampLogging(False)
    self._adb = None
    self._known_tests = None
    self._options = None
    self._test_args = None
    self._tests_to_run = None

  def _ProcessOptions(self):
    """Processes command-line options."""
    # TODO error messages on once-only or mutually-exclusive options.
    user_test_default = os.path.join(os.environ.get("HOME"), ".android",
                                     self._TEST_FILE_NAME)

    parser = optparse.OptionParser(usage=self._RUNTEST_USAGE)

    parser.add_option("-l", "--list-tests", dest="only_list_tests",
                      default=False, action="store_true",
                      help="To view the list of tests")
    parser.add_option("-b", "--skip-build", dest="skip_build", default=False,
                      action="store_true", help="Skip build - just launch")
    parser.add_option("-j", "--jobs", dest="make_jobs",
                      metavar="X", default=self._DEFAULT_JOBS,
                      help="Number of make jobs to use when building")
    parser.add_option("-n", "--skip_execute", dest="preview", default=False,
                      action="store_true",
                      help="Do not execute, just preview commands")
    parser.add_option("-r", "--raw-mode", dest="raw_mode", default=False,
                      action="store_true",
                      help="Raw mode (for output to other tools)")
    parser.add_option("-a", "--suite-assign", dest="suite_assign_mode",
                      default=False, action="store_true",
                      help="Suite assignment (for details & usage see "
                      "InstrumentationTestRunner)")
    parser.add_option("-v", "--verbose", dest="verbose", default=False,
                      action="store_true",
                      help="Increase verbosity of %s" % sys.argv[0])
    parser.add_option("-w", "--wait-for-debugger", dest="wait_for_debugger",
                      default=False, action="store_true",
                      help="Wait for debugger before launching tests")
    parser.add_option("-c", "--test-class", dest="test_class",
                      help="Restrict test to a specific class")
    parser.add_option("-m", "--test-method", dest="test_method",
                      help="Restrict test to a specific method")
    parser.add_option("-p", "--test-package", dest="test_package",
                      help="Restrict test to a specific java package")
    parser.add_option("-z", "--size", dest="test_size",
                      help="Restrict test to a specific test size")
    parser.add_option("--annotation", dest="test_annotation",
                      help="Include only those tests tagged with a specific"
                      " annotation")
    parser.add_option("--not-annotation", dest="test_not_annotation",
                      help="Exclude any tests tagged with a specific"
                      " annotation")
    parser.add_option("-u", "--user-tests-file", dest="user_tests_file",
                      metavar="FILE", default=user_test_default,
                      help="Alternate source of user test definitions")
    parser.add_option("-o", "--coverage", dest="coverage",
                      default=False, action="store_true",
                      help="Generate code coverage metrics for test(s)")
    parser.add_option("-x", "--path", dest="test_path",
                      help="Run test(s) at given file system path")
    parser.add_option("-t", "--all-tests", dest="all_tests",
                      default=False, action="store_true",
                      help="Run all defined tests")
    parser.add_option("--continuous", dest="continuous_tests",
                      default=False, action="store_true",
                      help="Run all tests defined as part of the continuous "
                      "test set")
    parser.add_option("--timeout", dest="timeout",
                      default=300, help="Set a timeout limit (in sec) for "
                      "running native tests on a device (default: 300 secs)")
    parser.add_option("--suite", dest="suite",
                      help="Run all tests defined as part of the "
                      "the given test suite")
    group = optparse.OptionGroup(
        parser, "Targets", "Use these options to direct tests to a specific "
        "Android target")
    group.add_option("-e", "--emulator", dest="emulator", default=False,
                     action="store_true", help="use emulator")
    group.add_option("-d", "--device", dest="device", default=False,
                     action="store_true", help="use device")
    group.add_option("-s", "--serial", dest="serial",
                     help="use specific serial")
    parser.add_option_group(group)
    self._options, self._test_args = parser.parse_args()

    if (not self._options.only_list_tests
        and not self._options.all_tests
        and not self._options.continuous_tests
        and not self._options.suite
        and not self._options.test_path
        and len(self._test_args) < 1):
      parser.print_help()
      logger.SilentLog("at least one test name must be specified")
      raise errors.AbortError

    self._adb = adb_interface.AdbInterface()
    if self._options.emulator:
      self._adb.SetEmulatorTarget()
    elif self._options.device:
      self._adb.SetDeviceTarget()
    elif self._options.serial is not None:
      self._adb.SetTargetSerial(self._options.serial)

    if self._options.verbose:
      logger.SetVerbose(True)

    self._known_tests = self._ReadTests()

    self._options.host_lib_path = android_build.GetHostLibraryPath()
    self._options.test_data_path = android_build.GetTestAppPath()

  def _ReadTests(self):
    """Parses the set of test definition data.

    Returns:
      A TestDefinitions object that contains the set of parsed tests.
    Raises:
      AbortError: If a fatal error occurred when parsing the tests.
    """
    try:
      known_tests = test_defs.TestDefinitions()
      # only read tests when not in path mode
      if not self._options.test_path:
        core_test_path = os.path.join(self._root_path, self._CORE_TEST_PATH)
        if os.path.isfile(core_test_path):
          known_tests.Parse(core_test_path)
        # read all <android root>/vendor/*/tests/testinfo/test_defs.xml paths
        vendor_tests_pattern = os.path.join(self._root_path,
                                            self._VENDOR_TEST_PATH)
        test_file_paths = glob.glob(vendor_tests_pattern)
        for test_file_path in test_file_paths:
          known_tests.Parse(test_file_path)
        if os.path.isfile(self._options.user_tests_file):
          known_tests.Parse(self._options.user_tests_file)
      return known_tests
    except errors.ParseError:
      raise errors.AbortError

  def _DumpTests(self):
    """Prints out set of defined tests."""
    print "The following tests are currently defined:\n"
    print "%-25s %-40s %s" % ("name", "build path", "description")
    print "-" * 80
    for test in self._known_tests:
      print "%-25s %-40s %s" % (test.GetName(), test.GetBuildPath(),
                                test.GetDescription())
    print "\nSee %s for more information" % self._TEST_FILE_NAME

  def _DoBuild(self):
    logger.SilentLog("Building tests...")

    tests = self._GetTestsToRun()
    # turn off dalvik verifier if necessary
    self._TurnOffVerifier(tests)
    self._DoFullBuild(tests)

    target_set = Set()
    extra_args_set = Set()
    for test_suite in tests:
      self._AddBuildTarget(test_suite, target_set, extra_args_set)

    if not self._options.preview:
      self._adb.EnableAdbRoot()
    else:
      logger.Log("adb root")
    rebuild_libcore = False
    if target_set:
      if self._options.coverage:
        coverage.EnableCoverageBuild()
        # hack to remove core library intermediates
        # hack is needed because:
        # 1. EMMA_INSTRUMENT changes what source files to include in libcore
        #    but it does not trigger a rebuild
        # 2. there's no target (like "clear-intermediates") to remove the files
        #    decently
        rebuild_libcore = not coverage.TestDeviceCoverageSupport(self._adb)
        if rebuild_libcore:
          cmd = "rm -rf %s" % os.path.join(
              self._root_path,
              "out/target/common/obj/JAVA_LIBRARIES/core_intermediates/")
          logger.Log(cmd)
          run_command.RunCommand(cmd, return_output=False)

      target_build_string = " ".join(list(target_set))
      extra_args_string = " ".join(list(extra_args_set))

      # mmm cannot be used from python, so perform a similar operation using
      # ONE_SHOT_MAKEFILE
      cmd = 'ONE_SHOT_MAKEFILE="%s" make -j%s -C "%s" all_modules %s' % (
          target_build_string, self._options.make_jobs, self._root_path,
          extra_args_string)
      logger.Log(cmd)

      if self._options.preview:
        # in preview mode, just display to the user what command would have been
        # run
        logger.Log("adb sync")
      else:
        # set timeout for build to 10 minutes, since libcore may need to
        # be rebuilt
        run_command.RunCommand(cmd, return_output=False, timeout_time=600)
        logger.Log("Syncing to device...")
        self._adb.Sync(runtime_restart=rebuild_libcore)

  def _DoFullBuild(self, tests):
    """If necessary, run a full 'make' command for the tests that need it."""
    extra_args_set = Set()

    # hack to build cts dependencies
    # TODO: remove this when cts dependencies are removed
    if self._IsCtsTests(tests):
      # need to use make since these fail building with ONE_SHOT_MAKEFILE
      extra_args_set.add('CtsTestStubs')
      extra_args_set.add('android.core.tests.runner')
    for test in tests:
      if test.IsFullMake():
        if test.GetExtraBuildArgs():
          # extra args contains the args to pass to 'make'
          extra_args_set.add(test.GetExtraBuildArgs())
        else:
          logger.Log("Warning: test %s needs a full build but does not specify"
                     " extra_build_args" % test.GetName())

    # check if there is actually any tests that required a full build
    if extra_args_set:
      cmd = ('make -j%s %s' % (self._options.make_jobs,
                               ' '.join(list(extra_args_set))))
      logger.Log(cmd)
      if not self._options.preview:
        old_dir = os.getcwd()
        os.chdir(self._root_path)
        run_command.RunCommand(cmd, return_output=False)
        os.chdir(old_dir)

  def _AddBuildTarget(self, test_suite, target_set, extra_args_set):
    if not test_suite.IsFullMake():
      build_dir = test_suite.GetBuildPath()
      if self._AddBuildTargetPath(build_dir, target_set):
        extra_args_set.add(test_suite.GetExtraBuildArgs())
      for path in test_suite.GetBuildDependencies(self._options):
        self._AddBuildTargetPath(path, target_set)

  def _AddBuildTargetPath(self, build_dir, target_set):
    if build_dir is not None:
      build_file_path = os.path.join(build_dir, "Android.mk")
      if os.path.isfile(os.path.join(self._root_path, build_file_path)):
        target_set.add(build_file_path)
        return True
      else:
        logger.Log("%s has no Android.mk, skipping" % build_dir)
    return False

  def _GetTestsToRun(self):
    """Get a list of TestSuite objects to run, based on command line args."""
    if self._tests_to_run:
      return self._tests_to_run

    self._tests_to_run = []
    if self._options.all_tests:
      self._tests_to_run = self._known_tests.GetTests()
    elif self._options.continuous_tests:
      self._tests_to_run = self._known_tests.GetContinuousTests()
    elif self._options.suite:
      self._tests_to_run = \
          self._known_tests.GetTestsInSuite(self._options.suite)
    elif self._options.test_path:
      walker = test_walker.TestWalker()
      self._tests_to_run = walker.FindTests(self._options.test_path)

    for name in self._test_args:
      test = self._known_tests.GetTest(name)
      if test is None:
        logger.Log("Error: Could not find test %s" % name)
        self._DumpTests()
        raise errors.AbortError
      self._tests_to_run.append(test)
    return self._tests_to_run

  def _IsCtsTests(self, test_list):
    """Check if any cts tests are included in given list of tests to run."""
    for test in test_list:
      if test.GetSuite() == 'cts':
        return True
    return False

  def _TurnOffVerifier(self, test_list):
    """Turn off the dalvik verifier if needed by given tests.

    If one or more tests needs dalvik verifier off, and it is not already off,
    turns off verifier and reboots device to allow change to take effect.
    """
    # hack to check if these are framework/base tests. If so, turn off verifier
    # to allow framework tests to access package-private framework api
    framework_test = False
    for test in test_list:
      if os.path.commonprefix([test.GetBuildPath(), "frameworks/base"]):
        framework_test = True
    if framework_test:
      # check if verifier is off already - to avoid the reboot if not
      # necessary
      output = self._adb.SendShellCommand("cat /data/local.prop")
      if not self._DALVIK_VERIFIER_OFF_PROP in output:
        if self._options.preview:
          logger.Log("adb shell \"echo %s >> /data/local.prop\""
                     % self._DALVIK_VERIFIER_OFF_PROP)
          logger.Log("adb reboot")
          logger.Log("adb wait-for-device")
        else:
          logger.Log("Turning off dalvik verifier and rebooting")
          self._adb.SendShellCommand("\"echo %s >> /data/local.prop\""
                                     % self._DALVIK_VERIFIER_OFF_PROP)
          self._adb.SendCommand("reboot")
          # wait for device to go offline
          time.sleep(10)
          self._adb.SendCommand("wait-for-device", timeout_time=60,
                                retry_count=3)
          self._adb.EnableAdbRoot()

  def RunTests(self):
    """Main entry method - executes the tests according to command line args."""
    try:
      run_command.SetAbortOnError()
      self._ProcessOptions()
      if self._options.only_list_tests:
        self._DumpTests()
        return

      if not self._options.skip_build:
        self._DoBuild()

      for test_suite in self._GetTestsToRun():
        try:
          test_suite.Run(self._options, self._adb)
        except errors.WaitForResponseTimedOutError:
          logger.Log("Timed out waiting for response")

    except KeyboardInterrupt:
      logger.Log("Exiting...")
    except errors.AbortError, error:
      logger.Log(error.msg)
      logger.SilentLog("Exiting due to AbortError...")
    except errors.WaitForResponseTimedOutError:
      logger.Log("Timed out waiting for response")


def RunTests():
  runner = TestRunner()
  runner.RunTests()

if __name__ == "__main__":
  RunTests()
