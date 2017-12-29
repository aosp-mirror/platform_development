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

"""Implementation of gsi_util hello command."""

import logging


def do_hello(args):
  if args.foo:
    logging.info('hello foo')
  if args.bar:
    logging.info('hello bar')


def setup_command_args(subparsers):
  """Sets up command args for 'hello'."""
  hello_parser = subparsers.add_parser('hello', help='hello help')
  hello_parser.add_argument('--foo', action='store_true', help='foo help')
  hello_parser.add_argument('--bar', action='store_true', help='bar help')
  hello_parser.set_defaults(func=do_hello)
