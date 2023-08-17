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
"""Compare failed tests in CTS/VTS test_result.xml.

Given two report files (A and B), this script compare them in two modes:
  One-way mode: For all the failed tests in A, list the tests and the results in
                both reports.
  Two-way mode: For all the tests in A and B, list the tests and the results in
                both reports. If a test only exists in one report, show NO_DATA
                in another one.

Usage example:
  ./compare_cts_reports.py -r test_result_1.xml test_result_2.xml
  -r test_result_3.xml -m 1 -d output_dir [-o]
  For this command line, the script aggregates test_result_1.xml and
  test_result_2.xml as one report, and then compare it with test_result_3.xml
  under one-way mode. The comparison result is written into output_dir/diff.csv.
"""

import argparse
import csv
import os
import tempfile

import aggregate_cts_reports
import parse_cts_report


def one_way_compare(reports, diff_csv):
  """Compare two reports in One-way Mode.

  Given two sets of reports, aggregate them into two reports (A and B).
  Then, list all failed tests in A, and show result of the same test in A and B.

  Args:
    reports: list of reports
    diff_csv: path to csv which stores comparison results
  """

  report_a = reports[0]
  report_b = reports[1]

  with open(diff_csv, 'w') as diff_csvfile:
    diff_writer = csv.writer(diff_csvfile)
    diff_writer.writerow(['module_name', 'abi', 'class_name', 'test_name',
                          'result in A', 'result in B'])

    for keys in report_a.gen_keys_list():
      module_name, abi, class_name, test_name = keys
      result_in_a = report_a.get_test_status(module_name, abi,
                                             class_name, test_name)

      if parse_cts_report.CtsReport.is_fail(result_in_a):
        result_in_b = report_b.get_test_status(module_name, abi,
                                               class_name, test_name)

        diff_writer.writerow([module_name, abi, class_name, test_name,
                              result_in_a, result_in_b])


def two_way_compare(reports, diff_csv):
  """Compare two reports in Two-way Mode.

  Given two sets of reports, aggregate them into two reports (A and B).
  Then, list all tests and show the results in A and B. If a test result exists
  in only one report, consider the result as NO_DATA in another report.

  Args:
    reports: list of reports
    diff_csv: path to csv which stores comparison results
  """

  diff = {}

  for i, report in enumerate(reports):
    for keys in report.gen_keys_list():
      module_name, abi, class_name, test_name = keys

      abis = diff.setdefault(module_name, {})
      test_classes = abis.setdefault(abi, {})
      tests = test_classes.setdefault(class_name, {})

      result = report.get_test_status(module_name, abi, class_name, test_name)

      if test_name not in tests:
        tests[test_name] = [parse_cts_report.NO_DATA, parse_cts_report.NO_DATA]

      tests[test_name][i] = result

  with open(diff_csv, 'w') as diff_csvfile:
    diff_writer = csv.writer(diff_csvfile)
    diff_writer.writerow(['module_name', 'abi', 'class_name', 'test_name',
                          'result in A', 'result in B'])

    for module_name, abis in diff.items():
      for abi, test_classes in abis.items():
        for class_name, tests in test_classes.items():
          for test_name, results in tests.items():
            if results[0] != results[1]:
              row = [module_name, abi, class_name, test_name] + results
              diff_writer.writerow(row)


def main():
  parser = argparse.ArgumentParser()

  parser.add_argument('--reports', '-r', required=True, nargs='+',
                      help=('Path to cts reports. Each flag -r is followed by'
                            'a group of files to be aggregated as one report.'),
                      action='append')
  parser.add_argument('--mode', '-m', required=True, choices=['1', '2'],
                      help='Comparison mode. 1: One-way mode. 2: Two-way mode.')
  parser.add_argument('--output-dir', '-d', required=True,
                      help='Directory to store output files.')
  parser.add_argument('--csv', default='diff.csv', help='Path to csv output.')
  parser.add_argument('--output-files', '-o', action='store_true',
                      help='Output parsed csv files.')

  args = parser.parse_args()

  report_files = args.reports
  mode = args.mode
  if (mode in ['1', '2']) and (len(report_files) != 2):
    msg = ('Two sets of reports are required for one-way and two-way mode.')
    raise UserWarning(msg)

  output_dir = args.output_dir
  if not os.path.exists(output_dir):
    raise FileNotFoundError(f'Output directory {output_dir} does not exist.')

  diff_csv = os.path.join(output_dir, args.csv)

  ctsreports = []
  for i, report_group in enumerate(report_files):
    report = aggregate_cts_reports.aggregate_cts_reports(report_group)

    if args.output_files:
      device_name = report.info['build_device']
      sub_dir_name = tempfile.mkdtemp(prefix=f'{i}_{device_name}_',
                                      dir=output_dir)
      report.output_files(sub_dir_name)

    ctsreports.append(report)

  if args.mode == '1':
    one_way_compare(ctsreports, diff_csv)
  elif args.mode == '2':
    two_way_compare(ctsreports, diff_csv)
  else:
    # TODO(b/292453652): Implement N-way comparison.
    print('Error: Arg --mode must be 1 or 2.')


if __name__ == '__main__':
  main()
