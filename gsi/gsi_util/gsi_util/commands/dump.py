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
"""Implementation of gsi_util command 'dump'."""

import argparse
import logging
import sys

from gsi_util.commands.common import image_sources
from gsi_util.dumpers import dumper


class DumpReporter(object):
  """Format and output dump info result to a output stream.

  When constructing DumpReporter, you need to give os and name_list.
  os is the stream to output the formatted, which should be inherited from
  io.IOBase. name_list is a string list describe the info names to be output.

  After collected all dump result, calls output() to output the
  dump_result_dict. dump_result_dict is a dictionary that maps info names to
  theirs result values.
  """

  _UNKNOWN_VALUE = '<unknown>'

  def __init__(self, os, name_list):
    """Inits DumpReporter with an output stream and an info name list.

    Args:
      os: the output stream of outputing the report
      name_list: the info name list will be output
    """
    self._os = os
    self._name_list = name_list
    self._show_unknown = False

  def set_show_unknown(self):
    """Enable force output dump info without dump result.

    By default, it doesn't output the dump info in the info name list which
    is not in dump result, i.e. the dump_result_dict of output().
    """
    self._show_unknown = True

  def _output_dump_info(self, info_name, value):
    print >> self._os, '{:30}: {}'.format(info_name, value)

  def output(self, dump_result_dict):
    """Output the given dump result.

    Args:
      dump_result_dict: the dump result dictionary to be output
    """
    for info_name in self._name_list:
      value = dump_result_dict.get(info_name)
      if not value:
        if not self._show_unknown:
          continue
        value = self._UNKNOWN_VALUE

      self._output_dump_info(info_name, value)


def do_list_dump(_):
  for info in dumper.Dumper.get_all_dump_list():
    print info.info_name


def do_dump(args):
  logging.info('==== DUMP ====')

  logging.debug('Info name list: %s', args.INFO_NAME)
  dump_list = dumper.Dumper.make_dump_list_by_name_list(args.INFO_NAME) if len(
      args.INFO_NAME) else dumper.Dumper.get_all_dump_list()

  mounter = image_sources.create_composite_mounter_by_args(args)
  with mounter as file_accessor:
    d = dumper.Dumper(file_accessor)
    dump_result_dict = d.dump(dump_list)

  # reserved for output to a file
  os = sys.stdout
  reporter = DumpReporter(os, (x.info_name for x in dump_list))
  if args.show_unknown:
    reporter.set_show_unknown()
  reporter.output(dump_result_dict)

  logging.info('==== DONE ====')


_DUMP_DESCRIPTION = ("""'dump' command dumps information from given image

You must assign at least one image source.

You could use command 'list_dump' to query all info names:

    $ ./gsi_util.py list_dump

For example you could use following command to query the security patch level
in an system image file:

    $ ./gsi_util.py dump --system system.img system_security_patch_level

You there is no given INFO_NAME, all information will be dumped.

Here are some other usage examples:

    $ ./gsi_util.py dump --system adb --vendor adb
    $ ./gsi_util.py dump --system system.img --show-unknown
    $ ./gsi_util.py dump --system my/out/folder/system""")


def setup_command_args(parser):
  # command 'list_dump'
  list_dump_parser = parser.add_parser(
      'list_dump', help='list all possible info names')
  list_dump_parser.set_defaults(func=do_list_dump)

  # command 'dump'
  dump_parser = parser.add_parser(
      'dump',
      help='dump information from given image',
      description=_DUMP_DESCRIPTION,
      formatter_class=argparse.RawTextHelpFormatter)
  dump_parser.add_argument(
      '-u',
      '--show-unknown',
      action='store_true',
      help='force display the dump info items in list which does not exist')
  image_sources.add_argument_group(dump_parser)
  dump_parser.add_argument(
      'INFO_NAME',
      type=str,
      nargs='*',
      help='the info name to be dumped. Dump all if not given')
  dump_parser.set_defaults(func=do_dump)
