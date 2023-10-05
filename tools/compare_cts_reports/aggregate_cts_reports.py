#!/usr/bin/python3
#
# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
"""Aggregate several cts reports into information files.

Given several cts reports, where a cts report could be a zip file or
test_result.xml, this script convert them into one set of information files.
The reports must be based on the same build fingerprint.
"""

import argparse
import os
import tempfile
import zipfile

import parse_cts_report


def aggregate_cts_reports(report_files):
  """Aggregate all report files and produce information files to output_dir.

  If the results of the same test are different in two reports, choose the one
  with a higher priority, following the order: PASS > IGNORED
  > ASSUMPTION_FAILURE > FAIL > TEST_ERROR > TEST_STATUS_UNSPECIFIED.

  Args:
    report_files: A list of paths to cts reports.

  Raises:
    UserWarning: Report files not compatible.

  Returns:
    A dictionary that maps build_fingerprint to a CtsReport object.
  """

  first_report_file = report_files[0]

  report = parse_cts_report.parse_report_file(first_report_file)

  with tempfile.TemporaryDirectory() as temp_dir:

    for report_file in report_files[1:]:
      xml_path = (
          parse_cts_report.extract_xml_from_zip(report_file, temp_dir)
          if zipfile.is_zipfile(report_file)
          else report_file)

      test_info = parse_cts_report.get_test_info_xml(xml_path)

      if not report.is_compatible(test_info):
        msg = (f'{report_file} is incompatible to {first_report_file}.')
        raise UserWarning(msg)

      report.read_test_result_xml(xml_path)

  return report


def main():
  parser = argparse.ArgumentParser()

  parser.add_argument('-r', '--report', required=True, nargs='+',
                      help=('Path to cts report(s), where a cts report could '
                            'be a zip archive or a xml file.'))
  parser.add_argument('-d', '--output-dir', required=True,
                      help=('Path to the directory to store output files.'))

  args = parser.parse_args()

  report_files = args.report
  output_dir = args.output_dir

  if not os.path.exists(output_dir):
    raise FileNotFoundError(f'Output directory {output_dir} does not exist.')

  report = aggregate_cts_reports(report_files)
  report.output_files(output_dir)


if __name__ == '__main__':
  main()
