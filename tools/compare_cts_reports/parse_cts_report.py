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
"""Convert single cts report into information files.

Given a cts report, which could be a zip file or test_result.xml, this script
turns them into three files: info.json, result.csv, and summary.csv.
"""

import argparse
import csv
import json
import os
import shutil
import tempfile
import xml.etree.ElementTree as ET
import zipfile
import constant


# TODO(b/293809772): Logging.


class CtsReport:
  """Class to record the test result of a cts report."""

  STATUS_ORDER = [
      'pass',
      'IGNORED',
      'ASSUMPTION_FAILURE',
      'fail',
      'TEST_ERROR',
      'TEST_STATUS_UNSPECIFIED',
  ]

  FAIL_INDEX = STATUS_ORDER.index('fail')

  def __init__(self, info):
    self.info = info
    self.result_tree = {}
    self.module_summaries = {}

  @staticmethod
  def is_fail(status):
    if status == constant.NO_DATA:
      return False
    else:
      return CtsReport.STATUS_ORDER.index(status) >= CtsReport.FAIL_INDEX

  def gen_keys_list(self):
    """Generate a 2D-list of keys."""

    keys_list = []

    modules = self.result_tree

    for module_name, abis in modules.items():
      for abi, test_classes in abis.items():
        for class_name, tests in test_classes.items():
          for test_name in tests.keys():
            keys_list.append([module_name, abi, class_name, test_name])

    return keys_list

  def is_compatible(self, info):
    return self.info['build_fingerprint'] == info['build_fingerprint']

  def get_test_status(self, module_name, abi, class_name, test_name):
    """Get test status from the CtsReport object."""

    if module_name not in self.result_tree:
      return constant.NO_DATA
    abis = self.result_tree[module_name]

    if abi not in abis:
      return constant.NO_DATA
    test_classes = abis[abi]

    if class_name not in test_classes:
      return constant.NO_DATA

    tests = test_classes[class_name]

    if test_name not in tests:
      return constant.NO_DATA

    return tests[test_name]

  def set_test_status(
      self, module_name, abi, class_name, test_name, test_status
  ):
    """Set test status to the CtsReport object."""

    previous = self.get_test_status(module_name, abi, class_name, test_name)

    abis = self.result_tree.setdefault(module_name, {})
    test_classes = abis.setdefault(abi, {})
    tests = test_classes.setdefault(class_name, {})

    if previous == constant.NO_DATA:
      tests[test_name] = test_status

      module_summary = self.module_summaries.setdefault(module_name, {})
      summary = module_summary.setdefault(abi, self.ModuleSummary())
      summary.counter[test_status] += 1

    elif (CtsReport.STATUS_ORDER.index(test_status)
          < CtsReport.STATUS_ORDER.index(previous)):
      summary = self.module_summaries[module_name][abi]

      tests[test_name] = test_status

      summary.counter[previous] -= 1
      summary.counter[test_status] += 1

  def read_test_result_xml(self, test_result_path):
    """Read the result from test_result.xml into a CtsReport object."""

    tree = ET.parse(test_result_path)
    root = tree.getroot()

    for module in root.iter('Module'):
      module_name = module.attrib['name']
      abi = module.attrib['abi']

      for testcase in module.iter('TestCase'):
        class_name = testcase.attrib['name']

        for test in testcase.iter('Test'):
          test_name = test.attrib['name']
          result = test.attrib['result']
          self.set_test_status(module_name, abi, class_name, test_name, result)

  def load_from_csv(self, result_csvfile):
    """Read the information of the report from the csv files.

    Args:
      result_csvfile: path to result.csv
    """

    result_reader = csv.reader(result_csvfile)

    try:
      next(result_reader)  # skip the header of csv file
    except StopIteration:
      print(f'Empty file: {result_csvfile.name}')
      return

    for row in result_reader:
      module_name, abi, class_name, test_name, result = row
      self.set_test_status(module_name, abi, class_name, test_name, result)

  def write_to_csv(self, result_csvfile, summary_csvfile):
    """Write the information of the report to the csv files.

    Args:
      result_csvfile: path to result.csv
      summary_csvfile: path to summary.csv
    """

    summary_writer = csv.writer(summary_csvfile)
    summary_writer.writerow(['module_name', 'abi'] + CtsReport.STATUS_ORDER)

    result_writer = csv.writer(result_csvfile)
    result_writer.writerow(
        ['module_name', 'abi', 'class_name', 'test_name', 'result']
    )

    modules = self.result_tree

    for module_name, abis in modules.items():
      for abi, test_classes in abis.items():
        module_summary = self.module_summaries[module_name][abi]

        summary = module_summary.summary_list()

        row = [module_name, abi] + summary
        summary_writer.writerow(row)

        for class_name, tests in test_classes.items():
          for test_name, result in tests.items():
            result_writer.writerow(
                [module_name, abi, class_name, test_name, result]
            )

  def output_files(self, output_dir):
    """Produce output files into the directory."""

    parsed_info_path = os.path.join(output_dir, 'info.json')
    parsed_result_path = os.path.join(output_dir, 'result.csv')
    parsed_summary_path = os.path.join(output_dir, 'summary.csv')

    files = [parsed_info_path, parsed_result_path, parsed_summary_path]

    for f in files:
      if os.path.exists(f):
        raise FileExistsError(f'Output file {f} already exists.')

    with open(parsed_info_path, 'w') as info_file:
      info_file.write(json.dumps(self.info, indent=2))

    with (
        open(parsed_result_path, 'w') as result_csvfile,
        open(parsed_summary_path, 'w') as summary_csvfile,
    ):
      self.write_to_csv(result_csvfile, summary_csvfile)

    for f in files:
      print(f'Parsed output {f}')

    return files

  class ModuleSummary:
    """Record the result summary of each (module, abi) pair."""

    def __init__(self):
      self.counter = dict.fromkeys(CtsReport.STATUS_ORDER, 0)

    @property
    def tested_items(self):
      """All tested items."""
      items = 0
      for status in CtsReport.STATUS_ORDER:
        items += self.counter[status]
      return items

    @property
    def pass_rate(self):
      """Pass rate of the module."""
      if self.tested_items == 0:
        return 0.0
      else:
        pass_category = 0
        for status in CtsReport.STATUS_ORDER:
          if not CtsReport.is_fail(status):
            pass_category += self.counter[status]
        return pass_category / self.tested_items

    def print_summary(self):
      for key in CtsReport.STATUS_ORDER:
        print(f'{key}: {self.counter[key]}')
        print()

    def summary_list(self):
      return [self.counter[key] for key in CtsReport.STATUS_ORDER]


ATTRS_TO_SHOW = [
    'Result::Build.build_model',
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
    'Result.suite_build_number',
]


def parse_attrib_path(attrib_path):
  """Parse the path into xml tag and attribute name."""
  first_dot = attrib_path.index('.')
  tags = attrib_path[:first_dot].split('::')
  attr_name = attrib_path[first_dot + 1 :]
  return tags, attr_name


def get_test_info_xml(test_result_path):
  """Get test info from xml file."""

  tree = ET.parse(test_result_path)
  root = tree.getroot()

  test_info = {
      'tool_version': constant.VERSION,
      'source_path': test_result_path,
  }

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


def print_test_info(info):
  """Print test information of the result in table format."""

  max_key_len = max([len(k) for k in info])
  max_value_len = max([len(info[k]) for k in info])
  table_len = max_key_len + 2 + max_value_len

  print('=' * table_len)

  for key in info:
    print(f'{key:<{max_key_len}}  {info[key]}')

  print('=' * table_len)
  print()


def extract_xml_from_zip(zip_file_path, dest_dir):
  """Extract test_result.xml from the zip file."""

  sub_dir_name = os.path.splitext(os.path.basename(zip_file_path))[0]
  xml_path = os.path.join(sub_dir_name, 'test_result.xml')
  extracted_xml = os.path.join(dest_dir, 'test_result.xml')
  with zipfile.ZipFile(zip_file_path) as myzip:
    with myzip.open(xml_path) as source, open(extracted_xml, 'wb') as target:
      shutil.copyfileobj(source, target)
  return extracted_xml


def parse_report_file(report_file):
  """Turn one cts report into a CtsReport object."""

  with tempfile.TemporaryDirectory() as temp_dir:
    xml_path = (
        extract_xml_from_zip(report_file, temp_dir)
        if zipfile.is_zipfile(report_file)
        else report_file
    )

    test_info = get_test_info_xml(xml_path)
    print_test_info(test_info)

    report = CtsReport(test_info)
    report.read_test_result_xml(xml_path)

  return report


def main():
  parser = argparse.ArgumentParser()

  parser.add_argument(
      '-r',
      '--report',
      required=True,
      help=(
          'Path to a cts report, where a cts report could '
          'be a zip archive or a xml file.'
      ),
  )
  parser.add_argument(
      '-d',
      '--output-dir',
      required=True,
      help='Path to the directory to store output files.',
  )

  args = parser.parse_args()

  report_file = args.report
  output_dir = args.output_dir

  if not os.path.exists(output_dir):
    raise FileNotFoundError(f'Output directory {output_dir} does not exist.')

  report = parse_report_file(report_file)

  report.output_files(output_dir)


if __name__ == '__main__':
  main()
