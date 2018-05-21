#!/usr/bin/env python
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
"""A utility to build and pack gsi_util."""

import argparse
from collections import namedtuple
import errno
import logging
import os
import shutil
import sys
import zipfile

from gsi_util.utils.cmd_utils import run_command

_MAKE_MODULE_NAME = 'gsi_util'

RequiredItem = namedtuple('RequiredItem', 'dest src')

# The list of dependency modules
# Format in (dest, src in host out)
REQUIRED_ITEMS = [
    # named as 'gsi_util.bin' to avoid conflict with the folder 'gsi_util'
    RequiredItem('gsi_util.bin', 'bin/gsi_util'),
    RequiredItem('bin/checkvintf', 'bin/checkvintf'),
    RequiredItem('lib64/libbase.so', 'lib64/libbase.so'),
    RequiredItem('lib64/liblog.so', 'lib64/liblog.so'),
    RequiredItem('bin/secilc', 'bin/secilc'),
    RequiredItem('lib64/libsepol.so', 'lib64/libsepol.so'),
    RequiredItem('bin/simg2img', 'bin/simg2img'),
    RequiredItem('lib64/libc++.so', 'lib64/libc++.so'),
]  # pyformat: disable

# Files to be included to zip file additionally
INCLUDE_FILES = [
    'README.md']  # pyformat: disable


def _check_android_env():
  if not os.environ.get('ANDROID_BUILD_TOP'):
    raise EnvironmentError('Need \'lunch\'.')


def _switch_to_prog_dir():
  prog = sys.argv[0]
  abspath = os.path.abspath(prog)
  dirname = os.path.dirname(abspath)
  os.chdir(dirname)


def _make_all():
  logging.info('Make %s...', _MAKE_MODULE_NAME)

  build_top = os.environ['ANDROID_BUILD_TOP']
  run_command(['make', '-j', _MAKE_MODULE_NAME], cwd=build_top)


def _create_dirs_and_copy_file(dest, src):
  dir_path = os.path.dirname(dest)
  try:
    if dir_path != '':
      os.makedirs(dir_path)
  except OSError as exc:
    if exc.errno != errno.EEXIST:
      raise
  logging.debug('copy(): %s %s', src, dest)
  shutil.copy(src, dest)


def _copy_deps():
  logging.info('Copy depend files...')
  host_out = os.environ['ANDROID_HOST_OUT']
  logging.debug('  ANDROID_HOST_OUT=%s', host_out)

  for item in REQUIRED_ITEMS:
    print 'Copy {}...'.format(item.dest)
    full_src = os.path.join(host_out, item.src)
    _create_dirs_and_copy_file(item.dest, full_src)


def _build_zipfile(filename):
  print 'Archive to {}...'.format(filename)
  with zipfile.ZipFile(filename, mode='w') as zf:
    for f in INCLUDE_FILES:
      print 'Add {}'.format(f)
      zf.write(f)
    for f in REQUIRED_ITEMS:
      print 'Add {}'.format(f[0])
      zf.write(f[0])


def do_setup_env(args):
  _check_android_env()
  _make_all()
  _switch_to_prog_dir()
  _copy_deps()


def do_list_deps(args):
  print 'Depend files (zip <== host out):'
  for item in REQUIRED_ITEMS:
    print '  {:20} <== {}'.format(item.dest, item.src)
  print 'Other include files:'
  for item in INCLUDE_FILES:
    print '  {}'.format(item)


def do_build(args):
  _check_android_env()
  _make_all()
  _switch_to_prog_dir()
  _copy_deps()
  _build_zipfile(args.output)


def main(argv):

  parser = argparse.ArgumentParser()
  subparsers = parser.add_subparsers(title='COMMAND')

  # Command 'setup_env'
  setup_env_parser = subparsers.add_parser(
      'setup_env',
      help='setup environment by building and copying dependency files')
  setup_env_parser.set_defaults(func=do_setup_env)

  # Command 'list_dep'
  list_dep_parser = subparsers.add_parser(
      'list_deps', help='list all dependency files')
  list_dep_parser.set_defaults(func=do_list_deps)

  # Command 'build'
  # TODO(szuweilin@): do not add this command if this is runned by package
  build_parser = subparsers.add_parser(
      'build', help='build a zip file including all required files')
  build_parser.add_argument(
      '-o',
      '--output',
      default='gsi_util.zip',
      help='the name of output zip file (default: gsi_util.zip)')
  build_parser.set_defaults(func=do_build)

  args = parser.parse_args(argv[1:])
  args.func(args)


if __name__ == '__main__':
  main(sys.argv)
