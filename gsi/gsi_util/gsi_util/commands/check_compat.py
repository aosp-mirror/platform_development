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
"""Provide command 'check_compat'."""

import argparse
import logging

from gsi_util.checkers.checker import Checker
from gsi_util.mounters.composite_mounter import CompositeMounter


class CheckReporter(object):
  """Output the checker result with formating."""

  _OUTPUT_FORMAT = '{:30}: {}'
  _ERR_MSE_FORMAT = '    {}'
  _SUMMARY_NAME = 'summary'

  @staticmethod
  def _get_pass_str(is_pass):
    return 'pass' if is_pass else 'fail'

  def _output_result_item(self, result_item):
    name, result, message = result_item
    if not self._only_summary:
      result_str = self._get_pass_str(result)
      print self._OUTPUT_FORMAT.format(name, result_str)
      if message:
        print self._ERR_MSE_FORMAT.format(message)
    return result

  def _output_summary(self, summary_result):
    summary_result_str = self._get_pass_str(summary_result)
    print self._OUTPUT_FORMAT.format(self._SUMMARY_NAME, summary_result_str)

  def __init__(self):
    self._only_summary = False

  def set_only_summary(self):
    self._only_summary = True

  def output(self, check_results):
    all_pass = True
    for result_item in check_results:
      item_pass = self._output_result_item(result_item)
      all_pass = all_pass and item_pass
    self._output_summary(all_pass)


def do_list_check(_):
  for info in Checker.get_all_check_list():
    print info.id


def do_check_compat(args):
  logging.info('==== CHECK_COMPAT ====')
  logging.info('  system=%s vendor=%s', args.system, args.vendor)

  # args.system and args.vendor are required
  mounter = CompositeMounter()
  mounter.add_by_mount_target('system', args.system)
  mounter.add_by_mount_target('vendor', args.vendor)

  logging.debug('Checking ID list: %s', args.ID)
  check_list = Checker.make_check_list_with_ids(args.ID) if len(
      args.ID) else Checker.get_all_check_list()

  with mounter as file_accessor:
    checker = Checker(file_accessor)
    check_result = checker.check(check_list)

  reporter = CheckReporter()
  if args.only_summary:
    reporter.set_only_summary()
  reporter.output(check_result)

  logging.info('==== DONE ====')


DUMP_DESCRIPTION = """'check_compat' command checks compatibility images

You must assign at least one image source by SYSTEM and/or VENDOR.
Image source could be:

 adb[:SERIAL_NUM]: form the device which be connected with adb
  image file name: from the given image file, e.g. the file name of a GSI.
                   If a image file is assigned to be the source of system
                   image, gsu_util will detect system-as-root automatically.
      folder name: from the given folder, e.g. the system/vendor folder in an
                   Android build out folder.

You could use command 'list_check' to query all IDs:

    $ ./gsi_util.py list_check

Here is an examples to check a system.img and a device are compatible:

    $ ./gsi_util.py check_compat --system system.img --vendor adb"""


def setup_command_args(parser):
  """Setup command 'list_check' and 'check_compat'."""

  # command 'list_check'
  list_check_parser = parser.add_parser(
      'list_check', help='list all possible checking IDs')
  list_check_parser.set_defaults(func=do_list_check)

  # command 'check_compat'
  check_compat_parser = parser.add_parser(
      'check_compat',
      help='checks compatibility between a system and a vendor',
      description=DUMP_DESCRIPTION,
      formatter_class=argparse.RawTextHelpFormatter)
  check_compat_parser.add_argument(
      '--system',
      type=str,
      required=True,
      help='system image file name, folder name or "adb"')
  check_compat_parser.add_argument(
      '--vendor',
      type=str,
      required=True,
      help='vendor image file name, folder name or "adb"')
  check_compat_parser.add_argument(
      '--only-summary',
      action='store_true',
      help='only output the summary result')
  check_compat_parser.add_argument(
      'ID',
      type=str,
      nargs='*',
      help='the checking ID to be dumped. Check all if not given')
  check_compat_parser.set_defaults(func=do_check_compat)
