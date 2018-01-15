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
"""Implementation of gsi_util command 'pull'."""

import argparse
import logging
import shutil
import sys

from gsi_util.mounters.composite_mounter import CompositeMounter


def do_pull(args):
  logging.info('==== PULL ====')
  logging.info('  system=%s vendor=%s', args.system, args.vendor)

  if not args.system and not args.vendor:
    sys.exit('Without system nor vendor.')

  source, dest = args.SOURCE, args.DEST

  mounter = CompositeMounter()
  if args.system:
    mounter.add_by_mount_target('system', args.system)
  if args.vendor:
    mounter.add_by_mount_target('vendor', args.vendor)

  with mounter as file_accessor:
    with file_accessor.prepare_file(source) as filename:
      if not filename:
        print >> sys.stderr, 'Can not dump file: {}'.format(source)
      else:
        logging.debug('Copy %s -> %s', filename, dest)
        shutil.copy(filename, dest)

  logging.info('==== DONE ====')


DUMP_DESCRIPTION = """'pull' command pulls a file from the give image.

You must assign at least one image source by SYSTEM and/or VENDOR.
Image source could be:

 adb[:SERIAL_NUM]: pull the file form the device which be connected with adb
  image file name: pull the file from the given image file, e.g. the file name
                   of a GSI.
                   If a image file is assigned to be the source of system
                   image, gsu_util will detect system-as-root automatically.
      folder name: pull the file from the given folder, e.g. the system/vendor
                   folder in a Android build out folder.

SOURCE is the full path file name to pull, which must start with '/' and
includes the mount point. ex.

    /system/build.prop
    /vendor/compatibility_matrix.xml

Some usage examples:

    $ ./gsi_util.py pull --system adb:AB0123456789 /system/manifest.xml
    $ ./gsi_util.py pull --vendor adb /vendor/compatibility_matrix.xml
    $ ./gsi_util.py pull --system system.img /system/build.prop
    $ ./gsi_util.py pull --system my/out/folder/system /system/build.prop"""


def setup_command_args(parser):
  # command 'pull'
  dump_parser = parser.add_parser(
      'pull',
      help='pull a file from the given image',
      description=DUMP_DESCRIPTION,
      formatter_class=argparse.RawTextHelpFormatter)
  dump_parser.add_argument(
      '--system', type=str, help='system image file name, folder name or "adb"')
  dump_parser.add_argument(
      '--vendor', type=str, help='vendor image file name, folder name or "adb"')
  dump_parser.add_argument(
      'SOURCE',
      type=str,
      help='the full path file name in given image to be pull')
  dump_parser.add_argument(
      'DEST',
      nargs='?',
      default='.',
      type=str,
      help='the file name or directory to save the pulled file (default: .)')
  dump_parser.set_defaults(func=do_pull)
