#!/usr/bin/python2.4
#
# Copyright (C) 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Module to compress directories in to series of zip files.

This module will take a directory and compress all its contents, including
child directories into a series of zip files named N.zip where 'N' ranges from
0 to infinity. The zip files will all be below a certain specified maximum
threshold.

The directory is compressed with a depth first traversal, each directory's
file contents being compressed as it is visisted, before the compression of any
child directory's contents. In this way the files within an archive are ordered
and the archives themselves are ordered.

The class also constructs a 'main.py' file intended for use with Google App
Engine with a custom App Engine program not currently distributed with this
code base. The custom App Engine runtime can leverage the index files written
out by this class to more quickly locate which zip file to serve a given URL
from.
"""

__author__ = 'jmatt@google.com (Justin Mattson)'

import optparse
import os
import stat
import sys
import zipfile
import divide_and_compress_constants


def CreateOptionsParser():
  """Creates the parser for command line arguments.

  Returns:
    A configured optparse.OptionParser object.
  """
  rtn = optparse.OptionParser()
  rtn.add_option('-s', '--sourcefiles', dest='sourcefiles', default=None,
                 help='The directory containing the files to compress')
  rtn.add_option('-d', '--destination', dest='destination', default=None,
                 help=('Where to put the archive files, this should not be'
                       ' a child of where the source files exist.'))
  rtn.add_option('-f', '--filesize', dest='filesize', default='1M',
                 help=('Maximum size of archive files. A number followed by '
                       'a magnitude indicator either "B", "K", "M", or "G". '
                       'Examples:\n  1000000B == one million BYTES\n'
                       '  1.2M == one point two MEGABYTES\n'
                       '  1M == 1048576 BYTES'))
  rtn.add_option('-n', '--nocompress', action='store_false', dest='compress',
                 default=True,
                 help=('Whether the archive files should be compressed, or '
                       'just a concatenation of the source files'))
  return rtn


def VerifyArguments(options, parser):
  """Runs simple checks on correctness of commandline arguments.

  Args:
    options: The command line options passed.
    parser: The parser object used to parse the command string.
  """
  try:
    if options.sourcefiles is None or options.destination is None:
      parser.print_help()
      sys.exit(-1)
  except AttributeError:
    parser.print_help()
    sys.exit(-1)


def ParseSize(size_str):
  """Parse the file size argument from a string to a number of bytes.

  Args:
    size_str: The string representation of the file size.

  Returns:
    The file size in bytes.

  Raises:
    ValueError: Raises an error if the numeric or qualifier portions of the
      file size argument is invalid.
  """
  if len(size_str) < 2:
    raise ValueError(('filesize argument not understood, please include'
                      ' a numeric value and magnitude indicator'))
  magnitude = size_str[-1]
  if not magnitude in ('B', 'K', 'M', 'G'):
    raise ValueError(('filesize magnitude indicator not valid, must be "B",'
                      '"K","M", or "G"'))
  numeral = float(size_str[:-1])
  if magnitude == 'K':
    numeral *= 1024
  elif magnitude == 'M':
    numeral *= 1048576
  elif magnitude == 'G':
    numeral *= 1073741824
  return int(numeral)


class DirectoryZipper(object):
  """Class to compress a directory and all its sub-directories."""

  def __init__(self, output_path, base_dir, archive_size, enable_compression):
    """DirectoryZipper constructor.

    Args:
      output_path: A string, the path to write the archives and index file to.
      base_dir: A string, the directory to compress.
      archive_size: An number, the maximum size, in bytes, of a single
        archive file.
      enable_compression: A boolean, whether or not compression should be
        enabled, if disabled, the files will be written into an uncompresed
        zip.
    """
    self.output_dir = output_path
    self.current_archive = '0.zip'
    self.base_path = base_dir
    self.max_size = archive_size
    self.compress = enable_compression

    # Set index_fp to None, because we don't know what it will be yet.
    self.index_fp = None

  def StartCompress(self):
    """Start compress of the directory.

    This will start the compression process and write the archives to the
    specified output directory. It will also produce an 'index.txt' file in the
    output directory that maps from file to archive.
    """
    self.index_fp = open(os.path.join(self.output_dir, 'main.py'), 'w')
    self.index_fp.write(divide_and_compress_constants.file_preamble)
    os.path.walk(self.base_path, self.CompressDirectory, 1)
    self.index_fp.write(divide_and_compress_constants.file_endpiece)
    self.index_fp.close()

  def RemoveLastFile(self, archive_path=None):
    """Removes the last item in the archive.

    This removes the last item in the archive by reading the items out of the
    archive, adding them to a new archive, deleting the old archive, and
    moving the new archive to the location of the old archive.

    Args:
      archive_path: Path to the archive to modify. This archive should not be
        open elsewhere, since it will need to be deleted.

    Returns:
      A new ZipFile object that points to the modified archive file.
    """
    if archive_path is None:
      archive_path = os.path.join(self.output_dir, self.current_archive)

    # Move the old file and create a new one at its old location.
    root, ext = os.path.splitext(archive_path)
    old_archive = ''.join([root, '-old', ext])
    os.rename(archive_path, old_archive)
    old_fp = self.OpenZipFileAtPath(old_archive, mode='r')

    # By default, store uncompressed.
    compress_bit = zipfile.ZIP_STORED
    if self.compress:
      compress_bit = zipfile.ZIP_DEFLATED
    new_fp = self.OpenZipFileAtPath(archive_path,
                                    mode='w',
                                    compress=compress_bit)

    # Read the old archive in a new archive, except the last one.
    for zip_member in old_fp.infolist()[:-1]:
      new_fp.writestr(zip_member, old_fp.read(zip_member.filename))

    # Close files and delete the old one.
    old_fp.close()
    new_fp.close()
    os.unlink(old_archive)

  def OpenZipFileAtPath(self, path, mode=None, compress=zipfile.ZIP_DEFLATED):
    """This method is mainly for testing purposes, eg dependency injection."""
    if mode is None:
      if os.path.exists(path):
        mode = 'a'
      else:
        mode = 'w'

    if mode == 'r':
      return zipfile.ZipFile(path, mode)
    else:
      return zipfile.ZipFile(path, mode, compress)

  def CompressDirectory(self, unused_id, dir_path, dir_contents):
    """Method to compress the given directory.

    This method compresses the directory 'dir_path'. It will add to an existing
    zip file that still has space and create new ones as necessary to keep zip
    file sizes under the maximum specified size. This also writes out the
    mapping of files to archives to the self.index_fp file descriptor

    Args:
      unused_id: A numeric identifier passed by the os.path.walk method, this
        is not used by this method.
      dir_path: A string, the path to the directory to compress.
      dir_contents: A list of directory contents to be compressed.
    """
    # Construct the queue of files to be added that this method will use
    # it seems that dir_contents is given in reverse alphabetical order,
    # so put them in alphabetical order by inserting to front of the list.
    dir_contents.sort()
    zip_queue = []
    for filename in dir_contents:
      zip_queue.append(os.path.join(dir_path, filename))
    compress_bit = zipfile.ZIP_DEFLATED
    if not self.compress:
      compress_bit = zipfile.ZIP_STORED

    # Zip all files in this directory, adding to existing archives and creating
    # as necessary.
    while zip_queue:
      target_file = zip_queue[0]
      if os.path.isfile(target_file):
        self.AddFileToArchive(target_file, compress_bit)

        # See if adding the new file made our archive too large.
        if not self.ArchiveIsValid():

          # IF fixing fails, the last added file was to large, skip it
          # ELSE the current archive filled normally, make a new one and try
          #  adding the file again.
          if not self.FixArchive('SIZE'):
            zip_queue.pop(0)
          else:
            self.current_archive = '%i.zip' % (
                int(self.current_archive[
                    0:self.current_archive.rfind('.zip')]) + 1)
        else:

          # Write an index record if necessary.
          self.WriteIndexRecord()
          zip_queue.pop(0)
      else:
        zip_queue.pop(0)

  def WriteIndexRecord(self):
    """Write an index record to the index file.

    Only write an index record if this is the first file to go into archive

    Returns:
      True if an archive record is written, False if it isn't.
    """
    archive = self.OpenZipFileAtPath(
        os.path.join(self.output_dir, self.current_archive), 'r')
    archive_index = archive.infolist()
    if len(archive_index) == 1:
      self.index_fp.write(
          '[\'%s\', \'%s\'],\n' % (self.current_archive,
                                   archive_index[0].filename))
      archive.close()
      return True
    else:
      archive.close()
      return False

  def FixArchive(self, problem):
    """Make the archive compliant.

    Args:
      problem: An enum, the reason the archive is invalid.

    Returns:
      Whether the file(s) removed to fix the archive could conceivably be
      in an archive, but for some reason can't be added to this one.
    """
    archive_path = os.path.join(self.output_dir, self.current_archive)
    return_value = None

    if problem == 'SIZE':
      archive_obj = self.OpenZipFileAtPath(archive_path, mode='r')
      num_archive_files = len(archive_obj.infolist())

      # IF there is a single file, that means its too large to compress,
      # delete the created archive
      # ELSE do normal finalization.
      if num_archive_files == 1:
        print ('WARNING: %s%s is too large to store.' % (
            self.base_path, archive_obj.infolist()[0].filename))
        archive_obj.close()
        os.unlink(archive_path)
        return_value = False
      else:
        archive_obj.close()
        self.RemoveLastFile(
          os.path.join(self.output_dir, self.current_archive))
        print 'Final archive size for %s is %i' % (
            self.current_archive, os.path.getsize(archive_path))
        return_value = True
    return return_value

  def AddFileToArchive(self, filepath, compress_bit):
    """Add the file at filepath to the current archive.

    Args:
      filepath: A string, the path of the file to add.
      compress_bit: A boolean, whether or not this file should be compressed
        when added.

    Returns:
      True if the file could be added (typically because this is a file) or
      False if it couldn't be added (typically because its a directory).
    """
    curr_archive_path = os.path.join(self.output_dir, self.current_archive)
    if os.path.isfile(filepath) and not os.path.islink(filepath):
      if os.path.getsize(filepath) > 1048576:
        print 'Warning: %s is potentially too large to serve on GAE' % filepath
      archive = self.OpenZipFileAtPath(curr_archive_path,
                                       compress=compress_bit)
      # Add the file to the archive.
      archive.write(filepath, filepath[len(self.base_path):])
      archive.close()
      return True
    else:
      return False

  def ArchiveIsValid(self):
    """Check whether the archive is valid.

    Currently this only checks whether the archive is under the required size.
    The thought is that eventually this will do additional validation

    Returns:
      True if the archive is valid, False if its not.
    """
    archive_path = os.path.join(self.output_dir, self.current_archive)
    return os.path.getsize(archive_path) <= self.max_size


def main(argv):
  parser = CreateOptionsParser()
  (options, unused_args) = parser.parse_args(args=argv[1:])
  VerifyArguments(options, parser)
  zipper = DirectoryZipper(options.destination,
                           options.sourcefiles,
                           ParseSize(options.filesize),
                           options.compress)
  zipper.StartCompress()


if __name__ == '__main__':
  main(sys.argv)
