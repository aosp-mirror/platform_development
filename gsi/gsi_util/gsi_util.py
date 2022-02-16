#!/usr/bin/env python
#
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

"""gsi_util command-line utility."""

import argparse
import logging
import sys


class GsiUtil(object):
  """Object for gsi_util command line tool."""

  _GSI_UTIL_VERSION = '1.0'

  # Adds gsi_util COMMAND here.
  # TODO(bowgotsai): auto collect from gsi_util/commands/*.py
  _COMMANDS = ['flash_gsi', 'pull', 'dump', 'check_compat']

  _LOGGING_FORMAT = '%(message)s'
  _LOGGING_LEVEL = logging.WARNING

  @staticmethod
  def _get_module_name(command):
    return 'gsi_util.commands.' + command

  def run(self, argv):
    """Command-line processor."""

    # Sets up default logging.
    logging.basicConfig(format=self._LOGGING_FORMAT, level=self._LOGGING_LEVEL)

    # Adds top-level --version/--debug argument.
    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--version', action='version',
                        version='%(prog)s {}'.format(self._GSI_UTIL_VERSION))
    parser.add_argument(
        '-d', '--debug', help='debug mode.', action='store_true')

    # Adds subparsers for each COMMAND.
    subparsers = parser.add_subparsers(title='COMMAND')
    for command in self._COMMANDS:
      module_name = self._get_module_name(command)
      mod = __import__(module_name, globals(), locals(), ['setup_command_args'])
      mod.setup_command_args(subparsers)

    args = parser.parse_args(argv[1:])
    if args.debug:
      logging.getLogger().setLevel(logging.DEBUG)

    try:
      args.func(args)
    except Exception as e:
      logging.error('%s: %s', argv[0], e.message)
      if args.debug:
        logging.exception(e)
      sys.exit(1)


if __name__ == '__main__':
  tool = GsiUtil()
  tool.run(sys.argv)
