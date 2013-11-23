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

"""Utilities for generating code coverage reports for Android tests."""

# Python imports
import glob
import optparse
import os

# local imports
import android_build
import android_mk
import coverage_target
import coverage_targets
import errors
import logger
import run_command


class CoverageGenerator(object):
  """Helper utility for obtaining code coverage results on Android.

  Intended to simplify the process of building,running, and generating code
  coverage results for a pre-defined set of tests and targets
  """

    # path to EMMA host jar, relative to Android build root
  _EMMA_JAR = os.path.join("external", "emma", "lib", "emma.jar")
  _TEST_COVERAGE_EXT = "ec"
  # root path of generated coverage report files, relative to Android build root
  _COVERAGE_REPORT_PATH = "emma"
  _TARGET_DEF_FILE = "coverage_targets.xml"
  _CORE_TARGET_PATH = os.path.join("development", "testrunner",
                                   _TARGET_DEF_FILE)
  # vendor glob file path patterns to tests, relative to android
  # build root
  _VENDOR_TARGET_PATH = os.path.join("vendor", "*", "tests", "testinfo",
                                     _TARGET_DEF_FILE)

  # path to root of target build intermediates
  _TARGET_INTERMEDIATES_BASE_PATH = os.path.join("target", "common",
                                                 "obj")

  def __init__(self, adb_interface):
    self._root_path = android_build.GetTop()
    self._out_path = android_build.GetOut()
    self._output_root_path = os.path.join(self._out_path,
                                          self._COVERAGE_REPORT_PATH)
    self._emma_jar_path = os.path.join(self._root_path, self._EMMA_JAR)
    self._adb = adb_interface
    self._targets_manifest = self._ReadTargets()

  def ExtractReport(self,
                    test_suite_name,
                    target,
                    device_coverage_path,
                    output_path=None,
                    test_qualifier=None):
    """Extract runtime coverage data and generate code coverage report.

    Assumes test has just been executed.
    Args:
      test_suite_name: name of TestSuite to generate coverage data for
      target: the CoverageTarget to use as basis for coverage calculation
      device_coverage_path: location of coverage file on device
      output_path: path to place output files in. If None will use
        <android_out_path>/<_COVERAGE_REPORT_PATH>/<target>/<test[-qualifier]>
      test_qualifier: designates mode test was run with. e.g size=small.
        If not None, this will be used to customize output_path as shown above.

    Returns:
      absolute file path string of generated html report file.
    """
    if output_path is None:
      report_name = test_suite_name
      if test_qualifier:
        report_name = report_name + "-" + test_qualifier
      output_path = os.path.join(self._out_path,
                                 self._COVERAGE_REPORT_PATH,
                                 target.GetName(),
                                 report_name)

    coverage_local_name = "%s.%s" % (report_name,
                                     self._TEST_COVERAGE_EXT)
    coverage_local_path = os.path.join(output_path,
                                       coverage_local_name)
    if self._adb.Pull(device_coverage_path, coverage_local_path):

      report_path = os.path.join(output_path,
                                 report_name)
      return self._GenerateReport(report_path, coverage_local_path, [target],
                                  do_src=True)
    return None

  def _GenerateReport(self, report_path, coverage_file_path, targets,
                      do_src=True):
    """Generate the code coverage report.

    Args:
      report_path: absolute file path of output file, without extension
      coverage_file_path: absolute file path of code coverage result file
      targets: list of CoverageTargets to use as base for code coverage
          measurement.
      do_src: True if generate coverage report with source linked in.
          Note this will increase size of generated report.

    Returns:
      absolute file path to generated report file.
    """
    input_metadatas = self._GatherMetadatas(targets)

    if do_src:
      src_arg = self._GatherSrcs(targets)
    else:
      src_arg = ""

    report_file = "%s.html" % report_path
    cmd1 = ("java -cp %s emma report -r html -in %s %s %s " %
            (self._emma_jar_path, coverage_file_path, input_metadatas, src_arg))
    cmd2 = "-Dreport.html.out.file=%s" % report_file
    self._RunCmd(cmd1 + cmd2)
    return report_file

  def _GatherMetadatas(self, targets):
    """Builds the emma input metadata argument from provided targets.

    Args:
      targets: list of CoverageTargets

    Returns:
      input metadata argument string
    """
    input_metadatas = ""
    for target in targets:
      input_metadata = os.path.join(self._GetBuildIntermediatePath(target),
                                    "coverage.em")
      input_metadatas += " -in %s" % input_metadata
    return input_metadatas

  def _GetBuildIntermediatePath(self, target):
    return os.path.join(
        self._out_path, self._TARGET_INTERMEDIATES_BASE_PATH, target.GetType(),
        "%s_intermediates" % target.GetName())

  def _GatherSrcs(self, targets):
    """Builds the emma input source path arguments from provided targets.

    Args:
      targets: list of CoverageTargets
    Returns:
      source path arguments string
    """
    src_list = []
    for target in targets:
      target_srcs = target.GetPaths()
      for path in target_srcs:
        src_list.append("-sp %s" %  os.path.join(self._root_path, path))
    return " ".join(src_list)

  def _MergeFiles(self, input_paths, dest_path):
    """Merges a set of emma coverage files into a consolidated file.

    Args:
      input_paths: list of string absolute coverage file paths to merge
      dest_path: absolute file path of destination file
    """
    input_list = []
    for input_path in input_paths:
      input_list.append("-in %s" % input_path)
    input_args = " ".join(input_list)
    self._RunCmd("java -cp %s emma merge %s -out %s" % (self._emma_jar_path,
                                                        input_args, dest_path))

  def _RunCmd(self, cmd):
    """Runs and logs the given os command."""
    run_command.RunCommand(cmd, return_output=False)

  def _CombineTargetCoverage(self):
    """Combines all target mode code coverage results.

    Will find all code coverage data files in direct sub-directories of
    self._output_root_path, and combine them into a single coverage report.
    Generated report is placed at self._output_root_path/android.html
    """
    coverage_files = self._FindCoverageFiles(self._output_root_path)
    combined_coverage = os.path.join(self._output_root_path,
                                     "android.%s" % self._TEST_COVERAGE_EXT)
    self._MergeFiles(coverage_files, combined_coverage)
    report_path = os.path.join(self._output_root_path, "android")
    # don't link to source, to limit file size
    self._GenerateReport(report_path, combined_coverage,
                         self._targets_manifest.GetTargets(), do_src=False)

  def _CombineTestCoverage(self):
    """Consolidates code coverage results for all target result directories."""
    target_dirs = os.listdir(self._output_root_path)
    for target_name in target_dirs:
      output_path = os.path.join(self._output_root_path, target_name)
      target = self._targets_manifest.GetTarget(target_name)
      if os.path.isdir(output_path) and target is not None:
        coverage_files = self._FindCoverageFiles(output_path)
        combined_coverage = os.path.join(output_path, "%s.%s" %
                                         (target_name, self._TEST_COVERAGE_EXT))
        self._MergeFiles(coverage_files, combined_coverage)
        report_path = os.path.join(output_path, target_name)
        self._GenerateReport(report_path, combined_coverage, [target])
      else:
        logger.Log("%s is not a valid target directory, skipping" % output_path)

  def _FindCoverageFiles(self, root_path):
    """Finds all files in <root_path>/*/*.<_TEST_COVERAGE_EXT>.

    Args:
      root_path: absolute file path string to search from
    Returns:
      list of absolute file path strings of coverage files
    """
    file_pattern = os.path.join(root_path, "*", "*.%s" %
                                self._TEST_COVERAGE_EXT)
    coverage_files = glob.glob(file_pattern)
    return coverage_files

  def _ReadTargets(self):
    """Parses the set of coverage target data.

    Returns:
       a CoverageTargets object that contains set of parsed targets.
    Raises:
       AbortError if a fatal error occurred when parsing the target files.
    """
    core_target_path = os.path.join(self._root_path, self._CORE_TARGET_PATH)
    try:
      targets = coverage_targets.CoverageTargets()
      targets.Parse(core_target_path)
      vendor_targets_pattern = os.path.join(self._root_path,
                                            self._VENDOR_TARGET_PATH)
      target_file_paths = glob.glob(vendor_targets_pattern)
      for target_file_path in target_file_paths:
        targets.Parse(target_file_path)
      return targets
    except errors.ParseError:
      raise errors.AbortError

  def TidyOutput(self):
    """Runs tidy on all generated html files.

    This is needed to the html files can be displayed cleanly on a web server.
    Assumes tidy is on current PATH.
    """
    logger.Log("Tidying output files")
    self._TidyDir(self._output_root_path)

  def _TidyDir(self, dir_path):
    """Recursively tidy all html files in given dir_path."""
    html_file_pattern = os.path.join(dir_path, "*.html")
    html_files_iter = glob.glob(html_file_pattern)
    for html_file_path in html_files_iter:
      os.system("tidy -m -errors -quiet %s" % html_file_path)
    sub_dirs = os.listdir(dir_path)
    for sub_dir_name in sub_dirs:
      sub_dir_path = os.path.join(dir_path, sub_dir_name)
      if os.path.isdir(sub_dir_path):
        self._TidyDir(sub_dir_path)

  def CombineCoverage(self):
    """Create combined coverage reports for all targets and tests."""
    self._CombineTestCoverage()
    self._CombineTargetCoverage()

  def GetCoverageTarget(self, name):
    """Find the CoverageTarget for given name"""
    target = self._targets_manifest.GetTarget(name)
    if target is None:
      msg = ["Error: test references undefined target %s." % name]
      msg.append(" Ensure target is defined in %s" % self._TARGET_DEF_FILE)
      raise errors.AbortError(msg)
    return target

  def GetCoverageTargetForPath(self, path):
    """Find the CoverageTarget for given file system path"""
    android_mk_path = os.path.join(path, "Android.mk")
    if os.path.exists(android_mk_path):
      android_mk_parser = android_mk.CreateAndroidMK(path)
      target = coverage_target.CoverageTarget()
      target.SetBuildPath(os.path.join(path, "src"))
      target.SetName(android_mk_parser.GetVariable(android_mk_parser.PACKAGE_NAME))
      target.SetType("APPS")
      return target
    else:
      msg = "No Android.mk found at %s" % path
      raise errors.AbortError(msg)


def EnableCoverageBuild():
  """Enable building an Android target with code coverage instrumentation."""
  os.environ["EMMA_INSTRUMENT"] = "true"

def Run():
  """Does coverage operations based on command line args."""
  # TODO: do we want to support combining coverage for a single target

  try:
    parser = optparse.OptionParser(usage="usage: %prog --combine-coverage")
    parser.add_option(
        "-c", "--combine-coverage", dest="combine_coverage", default=False,
        action="store_true", help="Combine coverage results stored given "
        "android root path")
    parser.add_option(
        "-t", "--tidy", dest="tidy", default=False, action="store_true",
        help="Run tidy on all generated html files")

    options, args = parser.parse_args()

    coverage = CoverageGenerator(None)
    if options.combine_coverage:
      coverage.CombineCoverage()
    if options.tidy:
      coverage.TidyOutput()
  except errors.AbortError:
    logger.SilentLog("Exiting due to AbortError")

if __name__ == "__main__":
  Run()
