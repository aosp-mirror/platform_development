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

from optparse import OptionParser
import os
import stat
import sys
import zipfile
from zipfile import ZipFile
import divide_and_compress_constants


def Main(argv):
  parser = CreateOptionsParser()
  (options, args) = parser.parse_args()
  VerifyArguments(options, parser)
  zipper = DirectoryZipper(options.destination, 
                           options.sourcefiles, 
                           ParseSize(options.filesize),
                           options.compress)
  zipper.StartCompress()
  

def CreateOptionsParser():
  rtn = OptionParser()
  rtn.add_option('-s', '--sourcefiles', dest='sourcefiles', default=None,
                 help='The directory containing the files to compress')
  rtn.add_option('-d', '--destination', dest='destination', default=None,
                 help=('Where to put the archive files, this should not be'
                       ' a child of where the source files exist.'))
  rtn.add_option('-f', '--filesize', dest='filesize', default='1M',
                 help=('Maximum size of archive files. A number followed by' 
                       'a magnitude indicator, eg. 1000000B == one million '
                       'BYTES, 500K == five hundred KILOBYTES, 1.2M == one '
                       'point two MEGABYTES. 1M == 1048576 BYTES'))
  rtn.add_option('-n', '--nocompress', action='store_false', dest='compress',
                 default=True, 
                 help=('Whether the archive files should be compressed, or '
                       'just a concatenation of the source files'))
  return rtn


def VerifyArguments(options, parser):
  try:
    if options.sourcefiles is None or options.destination is None:
      parser.print_help()
      sys.exit(-1)
  except (AttributeError), err:
    parser.print_help()
    sys.exit(-1)


def ParseSize(size_str):
  if len(size_str) < 2:
    raise ValueError(('filesize argument not understood, please include'
                      ' a numeric value and magnitude indicator'))
  magnitude = size_str[len(size_str)-1:]
  if not magnitude in ('K', 'B', 'M'):
    raise ValueError(('filesize magnitude indicator not valid, must be \'K\','
                      '\'B\', or \'M\''))
  numeral = float(size_str[0:len(size_str)-1])
  if magnitude == 'K':
    numeral *= 1024
  elif magnitude == 'M':
    numeral *= 1048576
  return int(numeral)


class DirectoryZipper(object):
  """Class to compress a directory and all its sub-directories."""  
  current_archive = None
  output_dir = None
  base_path = None
  max_size = None
  compress = None
  index_fp = None

  def __init__(self, output_path, base_dir, archive_size, enable_compression):
    """DirectoryZipper constructor.

    Args:
      output_path: the path to write the archives and index file to
      base_dir: the directory to compress
      archive_size: the maximum size, in bytes, of a single archive file
      enable_compression: whether or not compression should be enabled, if
        disabled, the files will be written into an uncompresed zip
    """
    self.output_dir = output_path
    self.current_archive = '0.zip'
    self.base_path = base_dir
    self.max_size = archive_size
    self.compress = enable_compression

  def StartCompress(self):
    """Start compress of the directory.

    This will start the compression process and write the archives to the
    specified output directory. It will also produce an 'index.txt' file in the
    output directory that maps from file to archive.
    """
    self.index_fp = open(''.join([self.output_dir, 'main.py']), 'w')
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
    Return:
      A new ZipFile object that points to the modified archive file
    """
    if archive_path is None:
      archive_path = ''.join([self.output_dir, self.current_archive])

    # Move the old file and create a new one at its old location
    ext_offset = archive_path.rfind('.')
    old_archive = ''.join([archive_path[0:ext_offset], '-old',
                           archive_path[ext_offset:]])
    os.rename(archive_path, old_archive)
    old_fp = self.OpenZipFileAtPath(old_archive, mode='r')

    if self.compress:
      new_fp = self.OpenZipFileAtPath(archive_path,
                                      mode='w',
                                      compress=zipfile.ZIP_DEFLATED)
    else:
      new_fp = self.OpenZipFileAtPath(archive_path,
                                      mode='w',
                                      compress=zipfile.ZIP_STORED)
    
    # Read the old archive in a new archive, except the last one
    zip_members = enumerate(old_fp.infolist())
    num_members = len(old_fp.infolist())
    while num_members > 1:
      this_member = zip_members.next()[1]
      new_fp.writestr(this_member.filename, old_fp.read(this_member.filename))
      num_members -= 1

    # Close files and delete the old one
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
      return ZipFile(path, mode)
    else:
      return ZipFile(path, mode, compress)

  def CompressDirectory(self, irrelevant, dir_path, dir_contents):
    """Method to compress the given directory.

    This method compresses the directory 'dir_path'. It will add to an existing
    zip file that still has space and create new ones as necessary to keep zip
    file sizes under the maximum specified size. This also writes out the
    mapping of files to archives to the self.index_fp file descriptor

    Args:
      irrelevant: a numeric identifier passed by the os.path.walk method, this
        is not used by this method
      dir_path: the path to the directory to compress
      dir_contents: a list of directory contents to be compressed
    """
    
    # construct the queue of files to be added that this method will use
    # it seems that dir_contents is given in reverse alphabetical order,
    # so put them in alphabetical order by inserting to front of the list
    dir_contents.sort()
    zip_queue = []
    if dir_path[len(dir_path) - 1:] == os.sep:
      for filename in dir_contents:
        zip_queue.append(''.join([dir_path, filename]))
    else:
      for filename in dir_contents:
        zip_queue.append(''.join([dir_path, os.sep, filename]))
    compress_bit = zipfile.ZIP_DEFLATED
    if not self.compress:
      compress_bit = zipfile.ZIP_STORED

    # zip all files in this directory, adding to existing archives and creating
    # as necessary
    while len(zip_queue) > 0:
      target_file = zip_queue[0]
      if os.path.isfile(target_file):
        self.AddFileToArchive(target_file, compress_bit)
        
        # see if adding the new file made our archive too large
        if not self.ArchiveIsValid():
          
          # IF fixing fails, the last added file was to large, skip it
          # ELSE the current archive filled normally, make a new one and try
          #  adding the file again
          if not self.FixArchive('SIZE'):
            zip_queue.pop(0)
          else:
            self.current_archive = '%i.zip' % (
                int(self.current_archive[
                    0:self.current_archive.rfind('.zip')]) + 1)
        else:

          # if this the first file in the archive, write an index record
          self.WriteIndexRecord()
          zip_queue.pop(0)
      else:
        zip_queue.pop(0)

  def WriteIndexRecord(self):
    """Write an index record to the index file.

    Only write an index record if this is the first file to go into archive

    Returns:
      True if an archive record is written, False if it isn't
    """
    archive = self.OpenZipFileAtPath(
        ''.join([self.output_dir, self.current_archive]), 'r')
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
      problem: the reason the archive is invalid

    Returns:
      Whether the file(s) removed to fix the archive could conceivably be
      in an archive, but for some reason can't be added to this one.
    """
    archive_path = ''.join([self.output_dir, self.current_archive])
    rtn_value = None
    
    if problem == 'SIZE':
      archive_obj = self.OpenZipFileAtPath(archive_path, mode='r')
      num_archive_files = len(archive_obj.infolist())
      
      # IF there is a single file, that means its too large to compress,
      # delete the created archive
      # ELSE do normal finalization
      if num_archive_files == 1:
        print ('WARNING: %s%s is too large to store.' % (
            self.base_path, archive_obj.infolist()[0].filename))
        archive_obj.close()
        os.unlink(archive_path)
        rtn_value = False
      else:
        self.RemoveLastFile(''.join([self.output_dir, self.current_archive]))
        archive_obj.close()
        print 'Final archive size for %s is %i' % (
            self.current_archive, os.stat(archive_path)[stat.ST_SIZE])
        rtn_value = True
    return rtn_value

  def AddFileToArchive(self, filepath, compress_bit):
    """Add the file at filepath to the current archive.

    Args:
      filepath: the path of the file to add
      compress_bit: whether or not this fiel should be compressed when added

    Returns:
      True if the file could be added (typically because this is a file) or
      False if it couldn't be added (typically because its a directory)
    """
    curr_archive_path = ''.join([self.output_dir, self.current_archive])
    if os.path.isfile(filepath):
      if os.stat(filepath)[stat.ST_SIZE] > 1048576:
        print 'Warning: %s is potentially too large to serve on GAE' % filepath
      archive = self.OpenZipFileAtPath(curr_archive_path,
                                       compress=compress_bit)
      # add the file to the archive
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
      True if the archive is valid, False if its not
    """
    archive_path = ''.join([self.output_dir, self.current_archive])
    if os.stat(archive_path)[stat.ST_SIZE] > self.max_size:
      return False
    else:
      return True

if __name__ == '__main__':
  Main(sys.argv)
