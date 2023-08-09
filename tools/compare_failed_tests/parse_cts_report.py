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
"""Turn a cts report into three information files.

Given a zip file or a test_result.xml, this script read the xml file
and produce three output files:
  info.json
  result.csv
  summary.csv
"""

import argparse
import csv
import json
import os
import shutil
import xml.etree.ElementTree as ET
import zipfile


# TODO(b/293809772): Logging test result.
# TODO(b/293809772): Aggregate several CTS reports.


class ModuleSummary():
  """Class to record the test result summary of a cts module."""

  def __init__(self):
    self.counter = {
        'pass': 0,
        'fail': 0,
        'IGNORED': 0,
        'ASSUMPTION_FAILURE': 0,
        'TEST_ERROR': 0,
        'TEST_STATUS_UNSPECIFIED': 0,
    }

  def print_info(self):
    for key, value in self.counter.items():
      print(f'{key}: {value}')
    print()


ATTRS_TO_SHOW = ['Result::Build.build_model',
                 'Result::Build.build_id',
                 'Result::Build.build_fingerprint',
                 'Result::Build.build_device',
                 'Result::Build.build_version_sdk',
                 'Result::Build.build_version_security_patch',
                 'Result::Build.build_board',
                 'Result::Build.build_type',
                 'Result::Build.build_version_release',
                 'Result.suite_name',
                 'Result.suite_version',
                 'Result.suite_plan',
                 'Result.suite_build_number',]
RESULTS = ['pass',
           'fail',
           'IGNORED',
           'ASSUMPTION_FAILURE',
           'TEST_ERROR',
           'TEST_STATUS_UNSPECIFIED',]


def parse_attrib_path(attrib_path):
  """Parse the path into xml tag and attribute name."""
  first_dot = attrib_path.index('.')
  tags = attrib_path[:first_dot].split('::')
  attr_name = attrib_path[first_dot+1:]
  return tags, attr_name


def get_test_info(root):
  """Get test info from xml tree."""

  test_info = {}

  for attrib_path in ATTRS_TO_SHOW:
    tags, attr_name = parse_attrib_path(attrib_path)
    node = root

    while True:
      tags = tags[1:]
      if tags:
        node = node.find(tags[0])
      else:
        break

    test_info[attr_name] = node.attrib[attr_name]

  return test_info


def print_test_info(test_result):
  """Print test information of the result in table format."""

  info = test_result['info']

  max_key_len = max([len(k) for k in info])
  max_value_len = max([len(info[k]) for k in info])
  table_len = (max_key_len + 2 + max_value_len)

  print('=' * table_len)

  for key in info:
    print(f'{key:<{max_key_len}}  {info[key]}')

  print('=' * table_len)
  print()


def extract_xml_from_zip(zip_file_path, output_dir):
  """Extract test_result.xml from the zip file."""

  sub_dir_name = os.path.splitext(os.path.basename(zip_file_path))[0]
  xml_path = os.path.join(sub_dir_name, 'test_result.xml')
  extracted_xml = os.path.join(output_dir, 'test_result.xml')
  with zipfile.ZipFile(zip_file_path) as myzip:
    with myzip.open(xml_path) as source, open(extracted_xml, 'wb') as target:
      shutil.copyfileobj(source, target)
  return extracted_xml


def read_test_result_xml(test_result_path):
  """Given the path to a test_result.xml, read that into a dict."""

  tree = ET.parse(test_result_path)
  root = tree.getroot()

  test_result = {}
  test_result['info'] = get_test_info(root)

  modules = {}
  test_result['modules'] = modules

  for module in root.iter('Module'):
    module_name = module.attrib['name']
    abi_name = module.attrib['abi']

    abis = modules.setdefault(module_name, {})
    testcases = abis.setdefault(abi_name, {})

    for testcase in module.iter('TestCase'):
      testcase_name = testcase.attrib['name']

      tests = testcases.setdefault(testcase_name, {})

      for test in testcase.iter('Test'):
        test_name = test.attrib['name']

        if test_name in tests:
          print('[WARNING] duplicated test:', test_name)

        tests[test_name] = test.attrib['result']

  return test_result


def write_to_csv(test_result, result_csvfile, summary_csvfile):
  """Given a result dict, write to the csv files.

  Args:
    test_result: the dict returned from read_test_result(test_result.xml)
    result_csvfile: path to result.csv
    summary_csvfile: path to summary.csv
  """

  result_writer = csv.writer(result_csvfile)
  result_writer.writerow(['module_name', 'abi',
                          'class_name', 'test_name', 'result'])

  summary_writer = csv.writer(summary_csvfile)
  summary_writer.writerow(['module', 'abi', 'pass', 'fail', 'IGNORED',
                           'ASSUMPTION_FAILURE', 'TEST_ERROR',
                           'TEST_STATUS_UNSPECIFIED'])

  modules = test_result['modules']

  for module_name, abis in modules.items():
    module_result_summary = ModuleSummary()

    for abi_name, testcases in abis.items():
      for testcase_name, tests in testcases.items():
        for test_name, result in tests.items():
          result_writer.writerow([module_name, abi_name,
                                  testcase_name, test_name, result])
          module_result_summary.counter[result] += 1

      summary = [module_result_summary.counter[result] for result in RESULTS]
      summary_writer.writerow([module_name, abi_name] + summary)


def main():
  parser = argparse.ArgumentParser()

  parser.add_argument('--report-file', required=True,
                      help=('Path to a cts report, where a cts report could '
                            'be a zip archive or a xml file.'))
  parser.add_argument('-d', '--output-dir', required=True,
                      help=('Path to the directory to store output files.'))

  args = parser.parse_args()

  output_dir = args.output_dir
  if not os.path.exists(output_dir):
    raise FileNotFoundError(f'Output directory {output_dir} does not exist.')

  xml_path = (
      extract_xml_from_zip(args.report_file, output_dir)
      if zipfile.is_zipfile(args.report_file)
      else args.report_file)
  test_result = read_test_result_xml(xml_path)

  print_test_info(test_result)

  parsed_info_path = os.path.join(output_dir, 'info.json')
  parsed_result_path = os.path.join(output_dir, 'result.csv')
  parsed_summary_path = os.path.join(output_dir, 'summary.csv')

  with open(parsed_info_path, 'w') as info_file:
    info_file.write(json.dumps(test_result['info'], indent=2))

  with (
      open(parsed_result_path, 'w') as result_csvfile,
      open(parsed_summary_path, 'w') as summary_csvfile,
  ):
    write_to_csv(test_result, result_csvfile, summary_csvfile)

  for f in [parsed_info_path, parsed_result_path, parsed_summary_path]:
    print(f'Parsed output {f}')


if __name__ == '__main__':
  main()
