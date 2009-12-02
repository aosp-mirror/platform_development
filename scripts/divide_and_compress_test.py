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

"""Tests for divide_and_compress.py.

TODO(jmatt): Add tests for module methods.
"""

__author__ = 'jmatt@google.com (Justin Mattson)'

import os
import stat
import unittest
import zipfile

import divide_and_compress
import mox


class BagOfParts(object):
  """Just a generic class that I can use to assign random attributes to."""

  def NoOp(self):
    x = 1

    
class ValidAndRemoveTests(unittest.TestCase):
  """Test the ArchiveIsValid and RemoveLastFile methods."""
  
  def setUp(self):
    """Prepare the test.

    Construct some mock objects for use with the tests.
    """
    self.my_mox = mox.Mox()
    file1 = BagOfParts()
    file1.filename = 'file1.txt'
    file1.contents = 'This is a test file'
    file2 = BagOfParts()
    file2.filename = 'file2.txt'
    file2.contents = ('akdjfk;djsf;kljdslkfjslkdfjlsfjkdvn;kn;2389rtu4i'
                      'tn;ghf8:89H*hp748FJw80fu9WJFpwf39pujens;fihkhjfk'
                      'sdjfljkgsc n;iself')
    self.files = {'file1': file1, 'file2': file2}

  def tearDown(self):
    """Remove any stubs we've created."""
    self.my_mox.UnsetStubs()

  def testArchiveIsValid(self):
    """Test the DirectoryZipper.ArchiveIsValid method.

    Run two tests, one that we expect to pass and one that we expect to fail
    """
    test_file_size = 1056730
    self.my_mox.StubOutWithMock(os, 'stat')
    os.stat('/foo/0.zip').AndReturn([test_file_size])
    self.my_mox.StubOutWithMock(stat, 'ST_SIZE')
    stat.ST_SIZE = 0
    os.stat('/baz/0.zip').AndReturn([test_file_size])
    mox.Replay(os.stat)
    test_target = divide_and_compress.DirectoryZipper('/foo/', 'bar', 
                                                      test_file_size - 1, True)

    self.assertEqual(False, test_target.ArchiveIsValid(),
                     msg=('ERROR: Test failed, ArchiveIsValid should have '
                          'returned false, but returned true'))

    test_target = divide_and_compress.DirectoryZipper('/baz/', 'bar',
                                                      test_file_size + 1, True)
    self.assertEqual(True, test_target.ArchiveIsValid(),
                     msg=('ERROR: Test failed, ArchiveIsValid should have'
                          ' returned true, but returned false'))

  def testRemoveLastFile(self):
    """Test DirectoryZipper.RemoveLastFile method.

    Construct a ZipInfo mock object with two records, verify that write is
    only called once on the new ZipFile object.
    """
    source = self.CreateZipSource()
    dest = self.CreateZipDestination()
    source_path = ''.join([os.getcwd(), '/0-old.zip'])
    dest_path = ''.join([os.getcwd(), '/0.zip'])
    test_target = divide_and_compress.DirectoryZipper(
        ''.join([os.getcwd(), '/']), 'dummy', 1024*1024, True)
    self.my_mox.StubOutWithMock(test_target, 'OpenZipFileAtPath')
    test_target.OpenZipFileAtPath(source_path, mode='r').AndReturn(source)
    test_target.OpenZipFileAtPath(dest_path,
                                  compress=zipfile.ZIP_DEFLATED,
                                  mode='w').AndReturn(dest)
    self.my_mox.StubOutWithMock(os, 'rename')
    os.rename(dest_path, source_path)
    self.my_mox.StubOutWithMock(os, 'unlink')
    os.unlink(source_path)
    
    self.my_mox.ReplayAll()
    test_target.RemoveLastFile()
    self.my_mox.VerifyAll()    

  def CreateZipSource(self):
    """Create a mock zip sourec object.

    Read should only be called once, because the second file is the one
    being removed.

    Returns:
      A configured mocked
    """
    
    source_zip = self.my_mox.CreateMock(zipfile.ZipFile)
    source_zip.infolist().AndReturn([self.files['file1'], self.files['file1']])
    source_zip.infolist().AndReturn([self.files['file1'], self.files['file1']])
    source_zip.read(self.files['file1'].filename).AndReturn(
        self.files['file1'].contents)
    source_zip.close()
    return source_zip

  def CreateZipDestination(self):
    """Create mock destination zip.

    Write should only be called once, because there are two files in the
    source zip and we expect the second to be removed.

    Returns:
      A configured mocked
    """
    
    dest_zip = mox.MockObject(zipfile.ZipFile)
    dest_zip.writestr(self.files['file1'].filename,
                      self.files['file1'].contents)
    dest_zip.close()
    return dest_zip


class FixArchiveTests(unittest.TestCase):
  """Tests for the DirectoryZipper.FixArchive method."""
  
  def setUp(self):
    """Create a mock file object."""
    self.my_mox = mox.Mox()
    self.file1 = BagOfParts()
    self.file1.filename = 'file1.txt'
    self.file1.contents = 'This is a test file'

  def tearDown(self):
    """Unset any mocks that we've created."""
    self.my_mox.UnsetStubs()

  def _InitMultiFileData(self):
    """Create an array of mock file objects.

    Create three mock file objects that we can use for testing.
    """
    self.multi_file_dir = []
    
    file1 = BagOfParts()
    file1.filename = 'file1.txt'
    file1.contents = 'kjaskl;jkdjfkja;kjsnbvjnvnbuewklriujalvjsd'
    self.multi_file_dir.append(file1)

    file2 = BagOfParts()
    file2.filename = 'file2.txt'
    file2.contents = ('He entered the room and there in the center, it was.'
                      ' Looking upon the thing, suddenly he could not remember'
                      ' whether he had actually seen it before or whether'
                      ' his memory of it was merely the effect of something'
                      ' so often being imagined that it had long since become '
                      ' manifest in his mind.')
    self.multi_file_dir.append(file2)

    file3 = BagOfParts()
    file3.filename = 'file3.txt'
    file3.contents = 'Whoa, what is \'file2.txt\' all about?'
    self.multi_file_dir.append(file3)
    
  def testSingleFileArchive(self):
    """Test behavior of FixArchive when the archive has a single member.

    We expect that when this method is called with an archive that has a
    single member that it will return False and unlink the archive.
    """
    test_target = divide_and_compress.DirectoryZipper(
        ''.join([os.getcwd(), '/']), 'dummy', 1024*1024, True)
    self.my_mox.StubOutWithMock(test_target, 'OpenZipFileAtPath')
    test_target.OpenZipFileAtPath(
        ''.join([os.getcwd(), '/0.zip']), mode='r').AndReturn(
            self.CreateSingleFileMock())
    self.my_mox.StubOutWithMock(os, 'unlink')
    os.unlink(''.join([os.getcwd(), '/0.zip']))
    self.my_mox.ReplayAll()
    self.assertEqual(False, test_target.FixArchive('SIZE'))
    self.my_mox.VerifyAll()

  def CreateSingleFileMock(self):
    """Create a mock ZipFile object for testSingleFileArchive.

    We just need it to return a single member infolist twice

    Returns:
      A configured mock object
    """
    mock_zip = self.my_mox.CreateMock(zipfile.ZipFile)
    mock_zip.infolist().AndReturn([self.file1])
    mock_zip.infolist().AndReturn([self.file1])
    mock_zip.close()
    return mock_zip

  def testMultiFileArchive(self):
    """Test behavior of DirectoryZipper.FixArchive with a multi-file archive.

    We expect that FixArchive will rename the old archive, adding '-old' before
    '.zip', read all the members except the last one of '-old' into a new
    archive with the same name as the original, and then unlink the '-old' copy
    """
    test_target = divide_and_compress.DirectoryZipper(
        ''.join([os.getcwd(), '/']), 'dummy', 1024*1024, True)
    self.my_mox.StubOutWithMock(test_target, 'OpenZipFileAtPath')
    test_target.OpenZipFileAtPath(
        ''.join([os.getcwd(), '/0.zip']), mode='r').AndReturn(
            self.CreateMultiFileMock())
    self.my_mox.StubOutWithMock(test_target, 'RemoveLastFile')
    test_target.RemoveLastFile(''.join([os.getcwd(), '/0.zip']))
    self.my_mox.StubOutWithMock(os, 'stat')
    os.stat(''.join([os.getcwd(), '/0.zip'])).AndReturn([49302])
    self.my_mox.StubOutWithMock(stat, 'ST_SIZE')
    stat.ST_SIZE = 0
    self.my_mox.ReplayAll()
    self.assertEqual(True, test_target.FixArchive('SIZE'))
    self.my_mox.VerifyAll()

  def CreateMultiFileMock(self):
    """Create mock ZipFile object for use with testMultiFileArchive.

    The mock just needs to return the infolist mock that is prepared in
    InitMultiFileData()

    Returns:
      A configured mock object
    """
    self._InitMultiFileData()
    mock_zip = self.my_mox.CreateMock(zipfile.ZipFile)
    mock_zip.infolist().AndReturn(self.multi_file_dir)
    mock_zip.close()
    return mock_zip


class AddFileToArchiveTest(unittest.TestCase):
  """Test behavior of method to add a file to an archive."""

  def setUp(self):
    """Setup the arguments for the DirectoryZipper object."""
    self.my_mox = mox.Mox()
    self.output_dir = '%s/' % os.getcwd()
    self.file_to_add = 'file.txt'
    self.input_dir = '/foo/bar/baz/'

  def tearDown(self):
    self.my_mox.UnsetStubs()

  def testAddFileToArchive(self):
    """Test the DirectoryZipper.AddFileToArchive method.

    We are testing a pretty trivial method, we just expect it to look at the
    file its adding, so that it possible can through out a warning.
    """
    test_target = divide_and_compress.DirectoryZipper(self.output_dir,
                                                      self.input_dir,
                                                      1024*1024, True)
    self.my_mox.StubOutWithMock(test_target, 'OpenZipFileAtPath')
    archive_mock = self.CreateArchiveMock()
    test_target.OpenZipFileAtPath(
        ''.join([self.output_dir, '0.zip']),
        compress=zipfile.ZIP_DEFLATED).AndReturn(archive_mock)
    self.StubOutOsModule()
    self.my_mox.ReplayAll()
    test_target.AddFileToArchive(''.join([self.input_dir, self.file_to_add]),
                                 zipfile.ZIP_DEFLATED)
    self.my_mox.VerifyAll()

  def StubOutOsModule(self):
    """Create a mock for the os.path and os.stat objects.

    Create a stub that will return the type (file or directory) and size of the
    object that is to be added.
    """
    self.my_mox.StubOutWithMock(os.path, 'isfile')
    os.path.isfile(''.join([self.input_dir, self.file_to_add])).AndReturn(True)
    self.my_mox.StubOutWithMock(os, 'stat')
    os.stat(''.join([self.input_dir, self.file_to_add])).AndReturn([39480])
    self.my_mox.StubOutWithMock(stat, 'ST_SIZE')
    stat.ST_SIZE = 0
    
  def CreateArchiveMock(self):
    """Create a mock ZipFile for use with testAddFileToArchive.

    Just verify that write is called with the file we expect and that the
    archive is closed after the file addition

    Returns:
      A configured mock object
    """
    archive_mock = self.my_mox.CreateMock(zipfile.ZipFile)
    archive_mock.write(''.join([self.input_dir, self.file_to_add]),
                       self.file_to_add)
    archive_mock.close()
    return archive_mock


class CompressDirectoryTest(unittest.TestCase):
  """Test the master method of the class.

  Testing with the following directory structure.
  /dir1/
  /dir1/file1.txt
  /dir1/file2.txt
  /dir1/dir2/
  /dir1/dir2/dir3/
  /dir1/dir2/dir4/
  /dir1/dir2/dir4/file3.txt
  /dir1/dir5/
  /dir1/dir5/file4.txt
  /dir1/dir5/file5.txt
  /dir1/dir5/file6.txt
  /dir1/dir5/file7.txt
  /dir1/dir6/
  /dir1/dir6/file8.txt

  file1.txt., file2.txt, file3.txt should be in 0.zip
  file4.txt should be in 1.zip
  file5.txt, file6.txt should be in 2.zip
  file7.txt will not be stored since it will be too large compressed
  file8.txt should b in 3.zip
  """

  def setUp(self):
    """Setup all the mocks for this test."""
    self.my_mox = mox.Mox()

    self.base_dir = '/dir1'
    self.output_path = '/out_dir/'
    self.test_target = divide_and_compress.DirectoryZipper(
        self.output_path, self.base_dir, 1024*1024, True)
    
    self.InitArgLists()
    self.InitOsDotPath()
    self.InitArchiveIsValid()
    self.InitWriteIndexRecord()
    self.InitAddFileToArchive()

  def tearDown(self):
    self.my_mox.UnsetStubs()

  def testCompressDirectory(self):
    """Test the DirectoryZipper.CompressDirectory method."""
    self.my_mox.ReplayAll()
    for arguments in self.argument_lists:
      self.test_target.CompressDirectory(None, arguments[0], arguments[1])
    self.my_mox.VerifyAll()

  def InitAddFileToArchive(self):
    """Setup mock for DirectoryZipper.AddFileToArchive.

    Make sure that the files are added in the order we expect.
    """
    self.my_mox.StubOutWithMock(self.test_target, 'AddFileToArchive')
    self.test_target.AddFileToArchive('/dir1/file1.txt', zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/file2.txt', zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir2/dir4/file3.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file4.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file4.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file5.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file5.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file6.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file7.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir5/file7.txt',
                                      zipfile.ZIP_DEFLATED)
    self.test_target.AddFileToArchive('/dir1/dir6/file8.txt',
                                      zipfile.ZIP_DEFLATED)
  
  def InitWriteIndexRecord(self):
    """Setup mock for DirectoryZipper.WriteIndexRecord."""
    self.my_mox.StubOutWithMock(self.test_target, 'WriteIndexRecord')

    # we are trying to compress 8 files, but we should only attempt to
    # write an index record 7 times, because one file is too large to be stored
    self.test_target.WriteIndexRecord().AndReturn(True)
    self.test_target.WriteIndexRecord().AndReturn(False)
    self.test_target.WriteIndexRecord().AndReturn(False)
    self.test_target.WriteIndexRecord().AndReturn(True)
    self.test_target.WriteIndexRecord().AndReturn(True)
    self.test_target.WriteIndexRecord().AndReturn(False)
    self.test_target.WriteIndexRecord().AndReturn(True)

  def InitArchiveIsValid(self):
    """Mock out DirectoryZipper.ArchiveIsValid and DirectoryZipper.FixArchive.

    Mock these methods out such that file1, file2, and file3 go into one
    archive. file4 then goes into the next archive, file5 and file6 in the
    next, file 7 should appear too large to compress into an archive, and
    file8 goes into the final archive
    """
    self.my_mox.StubOutWithMock(self.test_target, 'ArchiveIsValid')
    self.my_mox.StubOutWithMock(self.test_target, 'FixArchive')
    self.test_target.ArchiveIsValid().AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(True)

    # should be file4.txt
    self.test_target.ArchiveIsValid().AndReturn(False)
    self.test_target.FixArchive('SIZE').AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(True)

    # should be file5.txt
    self.test_target.ArchiveIsValid().AndReturn(False)
    self.test_target.FixArchive('SIZE').AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(True)

    # should be file7.txt
    self.test_target.ArchiveIsValid().AndReturn(False)
    self.test_target.FixArchive('SIZE').AndReturn(True)
    self.test_target.ArchiveIsValid().AndReturn(False)
    self.test_target.FixArchive('SIZE').AndReturn(False)
    self.test_target.ArchiveIsValid().AndReturn(True)
    
  def InitOsDotPath(self):
    """Mock out os.path.isfile.

    Mock this out so the things we want to appear as files appear as files and
    the things we want to appear as directories appear as directories. Also
    make sure that the order of file visits is as we expect (which is why
    InAnyOrder isn't used here).
    """
    self.my_mox.StubOutWithMock(os.path, 'isfile')
    os.path.isfile('/dir1/dir2').AndReturn(False)
    os.path.isfile('/dir1/dir5').AndReturn(False)
    os.path.isfile('/dir1/dir6').AndReturn(False)
    os.path.isfile('/dir1/file1.txt').AndReturn(True)
    os.path.isfile('/dir1/file2.txt').AndReturn(True)
    os.path.isfile('/dir1/dir2/dir3').AndReturn(False)
    os.path.isfile('/dir1/dir2/dir4').AndReturn(False)
    os.path.isfile('/dir1/dir2/dir4/file3.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file4.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file4.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file5.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file5.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file6.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file7.txt').AndReturn(True)
    os.path.isfile('/dir1/dir5/file7.txt').AndReturn(True)
    os.path.isfile('/dir1/dir6/file8.txt').AndReturn(True)

  def InitArgLists(self):
    """Create the directory path => directory contents mappings."""
    self.argument_lists = []
    self.argument_lists.append(['/dir1',
                                ['file1.txt', 'file2.txt', 'dir2', 'dir5',
                                 'dir6']])
    self.argument_lists.append(['/dir1/dir2', ['dir3', 'dir4']])
    self.argument_lists.append(['/dir1/dir2/dir3', []])
    self.argument_lists.append(['/dir1/dir2/dir4', ['file3.txt']])
    self.argument_lists.append(['/dir1/dir5',
                                ['file4.txt', 'file5.txt', 'file6.txt',
                                 'file7.txt']])
    self.argument_lists.append(['/dir1/dir6', ['file8.txt']])
      
if __name__ == '__main__':
  unittest.main()
