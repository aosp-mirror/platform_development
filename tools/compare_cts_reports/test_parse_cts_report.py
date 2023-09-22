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
import filecmp
import os
import tempfile
import unittest
import parse_cts_report


class TestParse(unittest.TestCase):

  def test_set(self):
    info = {}
    report = parse_cts_report.CtsReport(info)
    test_item = ('module', 'abi', 'class', 'test')

    report.set_test_status(*test_item, 'IGNORED')
    report.set_test_status(*test_item, 'fail')

    self.assertEqual(report.get_test_status(*test_item), 'IGNORED')

    report.set_test_status(*test_item, 'pass')

    self.assertEqual(report.get_test_status(*test_item), 'pass')

  def test_parse_xml(self):
    report_file = 'testdata/test_result_1.xml'
    report = parse_cts_report.parse_report_file(report_file)

    self.check_ctsreport(report)

  def test_parse_zip(self):
    report_file = 'testdata/report.zip'
    report = parse_cts_report.parse_report_file(report_file)

    self.check_ctsreport(report)

  def check_ctsreport(self, report):
    self.assertEqual(
        report.get_test_status('module_1', 'arm64-v8a', 'testcase_1', 'test_1'),
        'pass',
    )
    self.assertEqual(
        report.get_test_status('module_1', 'arm64-v8a', 'testcase_1', 'test_2'),
        'fail',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_2', 'test_3'),
        'pass',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_3', 'test_4'),
        'ASSUMPTION_FAILURE',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_3', 'test_5'),
        'fail',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_4', 'test_6'),
        'IGNORED',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_4', 'test_7'),
        'fail',
    )
    self.assertEqual(
        report.get_test_status('module_2', 'arm64-v8a', 'testcase_4', 'test_8'),
        'TEST_STATUS_UNSPECIFIED',
    )
    self.assertEqual(
        report.get_test_status('module_3', 'arm64-v8a', 'testcase_5', 'test_9'),
        'pass',
    )
    self.assertEqual(
        report.get_test_status(
            'module_3', 'arm64-v8a', 'testcase_5', 'test_10'
        ),
        'TEST_ERROR',
    )

    self.assertEqual(report.info['build_model'], 'this_model')
    self.assertEqual(report.info['build_id'], '1412')
    self.assertEqual(report.info['build_fingerprint'], 'this_build_fingerprint')
    self.assertEqual(report.info['build_device'], 'this_device')
    self.assertEqual(report.info['build_version_sdk'], '34')
    self.assertEqual(report.info['build_version_security_patch'], '2023-06-05')
    self.assertEqual(report.info['build_board'], 'this_board')
    self.assertEqual(report.info['build_type'], 'userdebug')
    self.assertEqual(report.info['build_version_release'], '14')
    self.assertEqual(report.info['suite_name'], 'CTS')
    self.assertEqual(report.info['suite_version'], '14_r1')
    self.assertEqual(report.info['suite_plan'], 'cts')
    self.assertEqual(report.info['suite_build_number'], '1234567')

    module_summaries = report.module_summaries
    summary = module_summaries['module_1']['arm64-v8a']
    self.assertEqual(summary.counter['pass'], 1)
    self.assertEqual(summary.counter['fail'], 1)

    summary = module_summaries['module_2']['arm64-v8a']
    self.assertEqual(summary.counter['pass'], 1)
    self.assertEqual(summary.counter['IGNORED'], 1)
    self.assertEqual(
        summary.counter['ASSUMPTION_FAILURE'],
        1,
    )
    self.assertEqual(summary.counter['fail'], 2)
    self.assertEqual(
        summary.counter['TEST_STATUS_UNSPECIFIED'],
        1,
    )

    summary = module_summaries['module_3']['arm64-v8a']
    self.assertEqual(summary.counter['pass'], 1)
    self.assertEqual(summary.counter['TEST_ERROR'], 1)

  def test_output(self):
    report_file = 'testdata/test_result_1.xml'
    report = parse_cts_report.parse_report_file(report_file)

    with tempfile.TemporaryDirectory() as temp_dir:
      report.output_files(temp_dir)

      parsed_info_path = os.path.join(temp_dir, 'info.json')
      parsed_result_path = os.path.join(temp_dir, 'result.csv')
      parsed_summary_path = os.path.join(temp_dir, 'summary.csv')

      self.assertTrue(
          filecmp.cmp('testdata/output/info_1.json', parsed_info_path)
      )
      self.assertTrue(
          filecmp.cmp('testdata/output/result_1.csv', parsed_result_path)
      )
      self.assertTrue(
          filecmp.cmp('testdata/output/summary_1.csv', parsed_summary_path)
      )


if __name__ == '__main__':
  unittest.main()
