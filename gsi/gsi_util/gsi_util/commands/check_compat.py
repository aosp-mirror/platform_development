# Copyright 2017 - The Android Open Source Project
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
"""Provides command 'check_compat'."""

from __future__ import print_function

import argparse
import logging

from gsi_util.checkers import checker
from gsi_util.commands.common import image_sources


class CheckReporter(object):
  """Outputs the checker result with formatting."""

  # The output will look like:
  #
  # check result 1                : pass
  # check result 2                : pass
  #
  # ------------------------------------
  # Pass/Total                    : 2/2
  _OUTPUT_FORMAT = '{:30}: {}'
  _ERR_MSE_FORMAT = '    {}'
  _OUTPUT_MAX_LEN = 36
  _SUMMARY_NAME = 'Pass/Total'

  def __init__(self):
    """Whether to only output a summary result of all checks."""
    self._only_summary = False

  def set_only_summary(self):
    """Only outputs summary result.

    When _only_summary is set, only shows the number of pass items over
    the number of total check items.
    """
    self._only_summary = True

  @staticmethod
  def _get_result_str(result_ok):
    """Gets the result string 'pass' or 'fail' based on the check result."""
    return 'pass' if result_ok else 'fail'

  def _output_result_item(self, result_item):
    """Outputs the result of a CheckResultItem().

    Args:
      result_item: a namedtuple of check_result.CheckResultItem().

    Returns:
      True if the test result passed. False otherwise.
    """
    title, result_ok, stderr = result_item

    if not self._only_summary:
      result_str = self._get_result_str(result_ok)
      print(self._OUTPUT_FORMAT.format(title, result_str))
      if stderr:
        print(self._ERR_MSE_FORMAT.format(stderr))

    return result_ok

  def _output_summary(self, num_pass_items, num_all_items):
    """Outputs a summary of all checker tests.

    Args:
      num_pass_items: The number of passing tests.
      num_all_items: Total number of finished tests.
    """
    print('-' * self._OUTPUT_MAX_LEN)
    summary_result_str = '{}/{}'.format(num_pass_items, num_all_items)
    print(self._OUTPUT_FORMAT.format(self._SUMMARY_NAME, summary_result_str))

  def output(self, check_result_items):
    """The main public method to output a sequence of CheckResultItem()s."""
    num_pass_items = 0
    num_all_items = 0
    for result_item in check_result_items:
      result_ok = self._output_result_item(result_item)
      if result_ok:
        num_pass_items += 1
      num_all_items += 1
    self._output_summary(num_pass_items, num_all_items)


def _format_check_list(check_list):
  """Returns a string of check list item names."""
  # The string is like: "'check_item1', 'check_item2', 'check_item3'".
  return ', '.join('{!r}'.format(x.check_item) for x in check_list)


def do_list_checks(_):
  """Prints the all supported check items."""
  print(_format_check_list(checker.Checker.get_all_check_list()))


def do_check_compat(args):
  """The actual function to do 'gsi_util check_compat' command."""
  logging.info('==== CHECK_COMPAT ====')
  logging.info('  system=%s vendor=%s', args.system, args.vendor)

  check_list = (checker.Checker.make_check_list(args.CHECK_ITEM)
                if args.CHECK_ITEM else checker.Checker.get_all_check_list())
  logging.debug('Starting check list: %s', _format_check_list(check_list))
  mounter = image_sources.create_composite_mounter_by_args(args)
  with mounter as file_accessor:
    the_checker = checker.Checker(file_accessor)
    check_result = the_checker.check(check_list)

  reporter = CheckReporter()
  if args.only_summary:
    reporter.set_only_summary()
  reporter.output(check_result)

  logging.info('==== DONE ====')


_CHECK_COMPAT_DESC = """
'check_compat' command checks compatibility between images.

You must assign both image sources by SYSTEM and VENDOR.

You could use command 'list_checks' to query all check items:

    $ ./gsi_util.py list_checks

Here is an examples to check a system.img and a device are compatible:

    $ ./gsi_util.py check_compat --system system.img --vendor adb"""


def setup_command_args(parser):
  """Sets up command 'list_checks' and 'check_compat'."""

  # Command 'list_checks'.
  list_check_parser = parser.add_parser(
      'list_checks', help='lists all possible check items. Run')
  list_check_parser.set_defaults(func=do_list_checks)

  # command 'check_compat'
  check_compat_parser = parser.add_parser(
      'check_compat',
      help='checks compatibility between a system and a vendor',
      description=_CHECK_COMPAT_DESC,
      formatter_class=argparse.RawTextHelpFormatter)
  check_compat_parser.add_argument(
      '-s',
      '--only-summary',
      action='store_true',
      help='only output the summary result')
  image_sources.add_argument_group(
      check_compat_parser,
      required_images=['system', 'vendor'])
  check_compat_parser.add_argument(
      'CHECK_ITEM',
      type=str,
      nargs='*',
      help=('the check item to be performed\n'
            'select one from: {}\n'.format(_format_check_list(
                checker.Checker.get_all_check_list())) +
            'if not given, it will check all'))
  check_compat_parser.set_defaults(func=do_check_compat)
