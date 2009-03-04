#!/usr/bin/env python
#
# Copyright 2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""A class to serve pages from zip files and use memcache for performance.

This contains a class and a function to create an anonymous instance of the
class to serve HTTP GET requests. Memcache is used to increase response speed
and lower processing cycles used in serving. Credit to Guido van Rossum and
his implementation of zipserve which served as a reference as I wrote this.

  MemcachedZipHandler: Class that serves request
  create_handler: method to create instance of MemcachedZipHandler
"""

__author__ = 'jmatt@google.com (Justin Mattson)'

import email.Utils
import logging
import mimetypes
import time
import zipfile

from google.appengine.api import memcache
from google.appengine.ext import webapp
from google.appengine.ext.webapp import util


def create_handler(zip_files, max_age=None, public=None):
  """Factory method to create a MemcachedZipHandler instance.

  Args:
    zip_files: A list of file names, or a list of lists of file name, first
        member of file mappings. See MemcachedZipHandler documentation for
        more information about using the list of lists format
    max_age: The maximum client-side cache lifetime
    public: Whether this should be declared public in the client-side cache
  Returns:
    A MemcachedZipHandler wrapped in a pretty, anonymous bow for use with App
    Engine

  Raises:
    ValueError: if the zip_files argument is not a list
  """
  # verify argument integrity. If the argument is passed in list format,
  # convert it to list of lists format
  
  if zip_files and type(zip_files).__name__ == 'list':
    num_items = len(zip_files)
    while num_items > 0:
      if type(zip_files[num_items - 1]).__name__ != 'list':
        zip_files[num_items - 1] = [zip_files[num_items-1]]
      num_items -= 1
  else:
    raise ValueError('File name arguments must be a list')

  class HandlerWrapper(MemcachedZipHandler):
    """Simple wrapper for an instance of MemcachedZipHandler.

    I'm still not sure why this is needed
    """
    
    def get(self, name):
      self.zipfilenames = zip_files
      self.TrueGet(name)
      if max_age is not None:
        MAX_AGE = max_age
      if public is not None:
        PUBLIC = public

  return HandlerWrapper


class MemcachedZipHandler(webapp.RequestHandler):
  """Handles get requests for a given URL.

  Serves a GET request from a series of zip files. As files are served they are
  put into memcache, which is much faster than retreiving them from the zip
  source file again. It also uses considerably fewer CPU cycles.
  """
  zipfile_cache = {}                # class cache of source zip files
  MAX_AGE = 600                     # max client-side cache lifetime
  PUBLIC = True                     # public cache setting
  CACHE_PREFIX = 'cache://'         # memcache key prefix for actual URLs
  NEG_CACHE_PREFIX = 'noncache://'  # memcache key prefix for non-existant URL

  def TrueGet(self, name):
    """The top-level entry point to serving requests.

    Called 'True' get because it does the work when called from the wrapper
    class' get method

    Args:
      name: URL requested

    Returns:
      None
    """
    name = self.PreprocessUrl(name)

    # see if we have the page in the memcache
    resp_data = self.GetFromCache(name)
    if resp_data is None:
      logging.info('Cache miss for %s', name)
      resp_data = self.GetFromNegativeCache(name)
      if resp_data is None:
        resp_data = self.GetFromStore(name)

        # IF we have the file, put it in the memcache
        # ELSE put it in the negative cache
        if resp_data is not None:
          self.StoreOrUpdateInCache(name, resp_data)
        else:
          logging.info('Adding %s to negative cache, serving 404', name)
          self.StoreInNegativeCache(name)
          self.Write404Error()
          return
      else:
        self.Write404Error()
        return

    content_type, encoding = mimetypes.guess_type(name)
    if content_type:
      self.response.headers['Content-Type'] = content_type
    self.SetCachingHeaders()
    self.response.out.write(resp_data)

  def PreprocessUrl(self, name):
    """Any preprocessing work on the URL when it comes it.

    Put any work related to interpretting the incoming URL here. For example,
    this is used to redirect requests for a directory to the index.html file
    in that directory. Subclasses should override this method to do different
    preprocessing.

    Args:
      name: The incoming URL

    Returns:
      The processed URL
    """
    # handle special case of requesting the domain itself
    if not name:
      name = 'index.html'

    # determine if this is a request for a directory
    final_path_segment = name
    final_slash_offset = name.rfind('/')
    if final_slash_offset != len(name) - 1:
      final_path_segment = name[final_slash_offset + 1:]
      if final_path_segment.find('.') == -1:
        name = ''.join([name, '/'])

    # if this is a directory, redirect to index.html
    if name[len(name) - 1:] == '/':
      return '%s%s' % (name, 'index.html')
    else:
      return name

  def GetFromStore(self, file_path):
    """Retrieve file from zip files.

    Get the file from the source, it must not have been in the memcache. If
    possible, we'll use the zip file index to quickly locate where the file
    should be found. (See MapToFileArchive documentation for assumptions about
    file ordering.) If we don't have an index or don't find the file where the
    index says we should, look through all the zip files to find it.

    Args:
      file_path: the file that we're looking for

    Returns:
      The contents of the requested file
    """
    resp_data = None
    file_itr = iter(self.zipfilenames)

    # check the index, if we have one, to see what archive the file is in
    archive_name = self.MapFileToArchive(file_path)
    if not archive_name:
      archive_name = file_itr.next()[0]
    
    while resp_data is None and archive_name:
      zip_archive = self.LoadZipFile(archive_name)
      if zip_archive:

        # we expect some lookups will fail, and that's okay, 404s will deal
        # with that
        try:
          resp_data = zip_archive.read(file_path)
        except (KeyError, RuntimeError), err:
          # no op
          x = False
        if resp_data is not None:
          logging.info('%s read from %s', file_path, archive_name)
          
      try:
        archive_name = file_itr.next()[0]
      except (StopIteration), err:
        archive_name = False

    return resp_data

  def LoadZipFile(self, zipfilename):
    """Convenience method to load zip file.

    Just a convenience method to load the zip file from the data store. This is
    useful if we ever want to change data stores and also as a means of
    dependency injection for testing. This method will look at our file cache
    first, and then load and cache the file if there's a cache miss

    Args:
      zipfilename: the name of the zip file to load

    Returns:
      The zip file requested, or None if there is an I/O error
    """
    zip_archive = None
    zip_archive = self.zipfile_cache.get(zipfilename)
    if zip_archive is None:
      try:
        zip_archive = zipfile.ZipFile(zipfilename)
        self.zipfile_cache[zipfilename] = zip_archive
      except (IOError, RuntimeError), err:
        logging.error('Can\'t open zipfile %s, cause: %s' % (zipfilename,
                                                             err))
    return zip_archive

  def MapFileToArchive(self, file_path):
    """Given a file name, determine what archive it should be in.

    This method makes two critical assumptions.
    (1) The zip files passed as an argument to the handler, if concatenated
        in that same order, would result in a total ordering
        of all the files. See (2) for ordering type.
    (2) Upper case letters before lower case letters. The traversal of a
        directory tree is depth first. A parent directory's files are added
        before the files of any child directories

    Args:
      file_path: the file to be mapped to an archive

    Returns:
      The name of the archive where we expect the file to be
    """
    num_archives = len(self.zipfilenames)
    while num_archives > 0:
      target = self.zipfilenames[num_archives - 1]
      if len(target) > 1:
        if self.CompareFilenames(target[1], file_path) >= 0:
          return target[0]
      num_archives -= 1

    return None

  def CompareFilenames(self, file1, file2):
    """Determines whether file1 is lexigraphically 'before' file2.

    WARNING: This method assumes that paths are output in a depth-first,
    with parent directories' files stored before childs'

    We say that file1 is lexigraphically before file2 if the last non-matching
    path segment of file1 is alphabetically before file2.
    
    Args:
      file1: the first file path
      file2: the second file path

    Returns:
      A positive number if file1 is before file2
      A negative number if file2 is before file1
      0 if filenames are the same
    """
    f1_segments = file1.split('/')
    f2_segments = file2.split('/')

    segment_ptr = 0
    while (segment_ptr < len(f1_segments) and
           segment_ptr < len(f2_segments) and
           f1_segments[segment_ptr] == f2_segments[segment_ptr]):
      segment_ptr += 1

    if len(f1_segments) == len(f2_segments):

      # we fell off the end, the paths much be the same
      if segment_ptr == len(f1_segments):
        return 0

      # we didn't fall of the end, compare the segments where they differ
      if f1_segments[segment_ptr] < f2_segments[segment_ptr]:
        return 1
      elif f1_segments[segment_ptr] > f2_segments[segment_ptr]:
        return -1
      else:
        return 0

      # the number of segments differs, we either mismatched comparing
      # directories, or comparing a file to a directory
    else:

      # IF we were looking at the last segment of one of the paths,
      # the one with fewer segments is first because files come before
      # directories
      # ELSE we just need to compare directory names
      if (segment_ptr + 1 == len(f1_segments) or
          segment_ptr + 1 == len(f2_segments)):
        return len(f2_segments) - len(f1_segments)
      else:
        if f1_segments[segment_ptr] < f2_segments[segment_ptr]:
          return 1
        elif f1_segments[segment_ptr] > f2_segments[segment_ptr]:
          return -1
        else:
          return 0

  def SetCachingHeaders(self):
    """Set caching headers for the request."""
    max_age = self.MAX_AGE
    self.response.headers['Expires'] = email.Utils.formatdate(
        time.time() + max_age, usegmt=True)
    cache_control = []
    if self.PUBLIC:
      cache_control.append('public')
    cache_control.append('max-age=%d' % max_age)
    self.response.headers['Cache-Control'] = ', '.join(cache_control)

  def GetFromCache(self, filename):
    """Get file from memcache, if available.

    Args:
      filename: The URL of the file to return

    Returns:
      The content of the file
    """
    return memcache.get('%s%s' % (self.CACHE_PREFIX, filename))

  def StoreOrUpdateInCache(self, filename, data):
    """Store data in the cache.

    Store a piece of data in the memcache. Memcache has a maximum item size of
    1*10^6 bytes. If the data is too large, fail, but log the failure. Future
    work will consider compressing the data before storing or chunking it

    Args:
      filename: the name of the file to store
      data: the data of the file

    Returns:
      None
    """
    try:
      if not memcache.add('%s%s' % (self.CACHE_PREFIX, filename), data):
        memcache.replace('%s%s' % (self.CACHE_PREFIX, filename), data)
    except (ValueError), err:
      logging.warning('Data size too large to cache\n%s' % err)

  def Write404Error(self):
    """Ouptut a simple 404 response."""
    self.error(404)
    self.response.out.write(
        ''.join(['<html><head><title>404: Not Found</title></head>',
                 '<body><b><h2>Error 404</h2><br/>',
                 'File not found</b></body></html>']))

  def StoreInNegativeCache(self, filename):
    """If a non-existant URL is accessed, cache this result as well.

    Future work should consider setting a maximum negative cache size to
    prevent it from from negatively impacting the real cache.

    Args:
      filename: URL to add ot negative cache

    Returns:
      None
    """
    memcache.add('%s%s' % (self.NEG_CACHE_PREFIX, filename), -1)

  def GetFromNegativeCache(self, filename):
    """Retrieve from negative cache.

    Args:
      filename: URL to retreive

    Returns:
      The file contents if present in the negative cache.
    """
    return memcache.get('%s%s' % (self.NEG_CACHE_PREFIX, filename))


def main():
  application = webapp.WSGIApplication([('/([^/]+)/(.*)',
                                         MemcachedZipHandler)])
  util.run_wsgi_app(application)


if __name__ == '__main__':
  main()
