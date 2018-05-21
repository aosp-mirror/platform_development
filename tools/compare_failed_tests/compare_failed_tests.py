#!/usr/bin/python
"""Compare failed tests in CTS/VTS test_result.xml.

Given two test_result.xml's (A and B), this script lists all failed tests in A,
and shows result of the same test in B.
"""

import argparse
import collections
import csv
import xml.etree.ElementTree as ET


PASS = 'pass'
FAIL = 'fail'
NO_DATA = 'no_data'

ATTRS_TO_SHOW = ['Result::Build.build_model',
                 'Result::Build.build_id',
                 'Result.suite_name',
                 'Result.suite_plan',
                 'Result.suite_build_number',
                 'Result.start_display']


def parse_attrib_path(attrib_path):
  first_dot = attrib_path.index('.')
  tags = attrib_path[:first_dot].split('::')
  attr_name = attrib_path[first_dot+1:]
  return tags, attr_name


def get_test_info(root):
  """Get test info from test_result.xml."""

  test_info = collections.OrderedDict()

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


def print_test_infos(test_result_a, test_result_b):
  """Print test infomation of both results in table format."""

  info_a = test_result_a['info']
  info_b = test_result_b['info']

  max_key_len = max([len(k) for k in info_a])
  max_value_a_len = max([len(info_a[k]) for k in info_a])
  max_value_b_len = max([len(info_b[k]) for k in info_b])
  table_len = (max_key_len + 2 + max_value_a_len + 2 + max_value_b_len)

  line_format = '{:{}}  {:{}}  {}'

  print '=' * table_len

  for key in info_a:
    print line_format.format(key, max_key_len,
                             info_a[key], max_value_a_len,
                             info_b[key])

  print '=' * table_len
  print


def get_result(test_result, module_name, testcase_name, test_name):
  """Get result of specifc module, testcase and test name."""

  modules = test_result['modules']
  if module_name not in modules:
    return NO_DATA

  testcases = modules[module_name]
  if testcase_name not in testcases:
    return NO_DATA

  tests = testcases[testcase_name]
  if test_name not in tests:
    return NO_DATA

  return tests[test_name]


def read_test_result_xml(test_result_path):
  """Given the path to a test_result.xml, read that into a ordered dict."""

  tree = ET.parse(test_result_path)
  root = tree.getroot()

  test_result = collections.OrderedDict()
  test_result['info'] = get_test_info(root)

  modules = collections.OrderedDict()
  test_result['modules'] = modules

  for module in root.iter('Module'):
    module_name = '|'.join([module.attrib['name'], module.attrib['abi']])

    if module_name in modules:
      print 'WARNING: Duplicate module: ' + module_name

    testcases = collections.OrderedDict()
    modules[module_name] = testcases

    for testcase in module.iter('TestCase'):
      testcase_name = testcase.attrib['name']

      if testcase_name in testcases:
        print 'WARNING: Duplicate testcase: ' + testcase_name

      tests = collections.OrderedDict()
      testcases[testcase_name] = tests

      for test in testcase.iter('Test'):
        test_name = test.attrib['name']

        if test_name in tests:
          print 'WARNING: Duplicate test: ' + test_name

        result = test.attrib['result']
        tests[test_name] = result

  return test_result


def compare_failed_tests(test_result_a, test_result_b,
                         csvfile, only_failed_both):
  """Do the comparison.

  Given two test result dicts (A and B), list all failed test in A and display
  result of the same test in B.

  Args:
    test_result_a: the dict returned from read_test_result(test_result_a.xml)
    test_result_b: the dict returned from read_test_result(test_result_b.xml)
    csvfile: a opened file
    only_failed_both: only display tests those failed in both test results

  Returns:
    string: diff report, summary
  """

  writer = csv.writer(csvfile)
  writer.writerow(['module', 'testcase', 'test', 'result in B'])

  summary = ''

  modules = test_result_a['modules']

  for module_name, testcases in modules.iteritems():
    module_sub_summary = ''

    for testcase_name, tests in testcases.iteritems():
      testcase_sub_summary = ''

      for test_name, result in tests.iteritems():
        if result == FAIL:
          result_b = get_result(
              test_result_b, module_name, testcase_name, test_name)

          if not only_failed_both or result_b == FAIL:
            testcase_sub_summary += '    ' + test_name + ': ' + result_b + '\n'
            writer.writerow([module_name, testcase_name, test_name, result_b])

      if testcase_sub_summary:
        module_sub_summary = '  ' + testcase_name + '\n' + testcase_sub_summary

    if module_sub_summary:
      summary += module_name + '\n' + module_sub_summary + '\n'

  return summary


def main():
  parser = argparse.ArgumentParser()

  parser.add_argument('test_result_a', help='path to first test_result.xml')
  parser.add_argument('test_result_b', help='path to second test_result.xml')
  parser.add_argument('--csv', default='diff.csv', help='path to csv output')
  parser.add_argument('--only-failed-both', action='store_true',
                      help='only list tests failed in both test_result.xml')

  args = parser.parse_args()

  test_result_a = read_test_result_xml(args.test_result_a)
  test_result_b = read_test_result_xml(args.test_result_b)

  print_test_infos(test_result_a, test_result_b)

  with open(args.csv, 'w') as csvfile:
    summary = compare_failed_tests(test_result_a, test_result_b, csvfile,
                                   args.only_failed_both)

    print summary


if __name__ == '__main__':
  main()
