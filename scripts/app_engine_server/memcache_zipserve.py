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
import re
import sys
import time
import yaml
import zipfile

from google.appengine.api import memcache
from google.appengine.ext import webapp
from google.appengine.ext.webapp import util
from time import localtime, strftime

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
  REDIRECT_PREFIX = 'redirect://'   # memcache key prefix for redirect data
  REDIRECT_FILE = 'redirects.yaml'  # Name of file that contains redirect table
  REDIRECT_SRC = 'src'              # Name of the 'source' attribute for a
                                    #   redirect table entry
  REDIRECT_DST = 'dst'              # Name of the 'destination' attribute for
                                    #   a redirect table entry
  REDIRECT_TYPE = 'type'            # Name of the 'type' attribute for a
                                    #   redirect table entry
  REDIRECT_TYPE_PERM = 'permanent'  # Redirect 'type' string indicating a 301
                                    #   redirect should be served
  REDIRECT_TYPE_TEMP = 'temporary'  # Redirect 'type'string indicate a 302
                                    #   Redirect should be served
  intlString = 'intl/'
  validLangs = ['en', 'de', 'es', 'fr','it','ja','zh-CN','zh-TW']

  def TrueGet(self, reqUri):
    """The top-level entry point to serving requests.

    Called 'True' get because it does the work when called from the wrapper
    class' get method. Some logic is applied to the request to serve files
    from an intl/<lang>/... directory or fall through to the default language.

    Args:
      name: URL requested

    Returns:
      None
    """
    langName = 'en'
    resetLangCookie = False
    urlLangName = None
    retry = False
    isValidIntl = False
    isStripped = False

    # Try to retrieve the user's lang pref from the cookie. If there is no
    # lang pref cookie in the request, add set-cookie to the response with the
    # default value of 'en'.
    try:
      langName = self.request.cookies['android_developer_pref_lang']
    except KeyError:
      resetLangCookie = True
      #logging.info('==========================EXCEPTION: NO LANG COOKIE FOUND, USING [%s]', langName)
    logging.info('==========================REQ INIT name [%s] langName [%s] resetLangCookie [%s]', reqUri, langName, resetLangCookie)

    # Do some prep for handling intl requests. Parse the url and validate
    # the intl/lang substring, extract the url lang code (urlLangName) and the
    # the uri that follows the intl/lang substring(contentUri)
    sections = reqUri.split("/", 2)
    isIntl = len(sections) > 2 and (sections[0] == "intl")
    if isIntl:
      isValidIntl = sections[1] in self.validLangs
      urlLangName = sections[1]
      contentUri = sections[2]
      logging.info('  Content URI is [%s]...', contentUri)
      if isValidIntl:
        if (langName != urlLangName) or (langName == 'en'):
          # if the lang code in the request is different from that in
          # the cookie, or if the target lang is en, strip the
          # intl/nn substring. It will later be redirected to
          # the user's preferred language url.
          # logging.info('  Handling a MISMATCHED intl request')
          reqUri = contentUri
          isStripped = True
          isValidIntl = False
          isIntl = False
          #logging.info('INTL PREP resetting langName to urlLangName [%s]', langName)
        #else:
        #  logging.info('INTL PREP no need to reset langName')
    else:
      contentUri = reqUri

    # Apply manual redirects from redirects.yaml. This occurs before any
    # other mutations are performed, to avoid odd redirect behavior
    # (For example, a user may want to redirect a directory without having
    # /index.html appended.)
    did_redirect = self.ProcessManualRedirects(contentUri, langName, isIntl)
    if did_redirect:
      return

    # Preprocess the req url. If it references a directory or the domain itself,
    # append '/index.html' to the url and 302 redirect. Otherwise, continue
    # processing the request below.
    did_redirect = self.PreprocessUrl(reqUri, langName)
    if did_redirect:
      return

    # Send for processing
    if self.isCleanUrl(reqUri, langName, isValidIntl, isStripped):
      # handle a 'clean' request.
      # Try to form a response using the actual request url.
      # logging.info('  Request being handled as clean: [%s]', name)
      if not self.CreateResponse(reqUri, langName, isValidIntl, resetLangCookie):
        # If CreateResponse returns False, there was no such document
        # in the intl/lang tree. Before going to 404, see if there is an
        # English-language version of the doc in the default
        # default tree and return it, else go to 404.
        self.CreateResponse(contentUri, langName, False, resetLangCookie)

    elif isIntl:
      # handle the case where we need to pass through an invalid intl req
      # for processing (so as to get 404 as appropriate). This is needed
      # because intl urls are passed through clean and retried in English,
      # if necessary.
      # logging.info('  Handling an invalid intl request...')
      self.CreateResponse(reqUri, langName, isValidIntl, resetLangCookie)

    else:
      # handle the case where we have a non-clean url (usually a non-intl
      # url) that we need to interpret in the context of any lang pref
      # that is set. Prepend an intl/lang string to the request url and
      # send it as a 302 redirect. After the redirect, the subsequent
      # request will be handled as a clean url.
      self.RedirToIntl(reqUri, self.intlString, langName)

  def ProcessManualRedirects(self, contentUri, langName, isIntl):
    """Compute any manual redirects for a request and execute them.

    This allows content authors to manually define a set of regex rules which,
    when matched, will cause an HTTP redirect to be performed.

    Redirect rules are typically stored in a file named redirects.yaml. See the
    comments in that file for more information about formatting.

    Redirect computations are stored in memcache for performance.

    Note that international URIs are handled automatically, and are assumed to
    mirror redirects for non-intl requests.

    Args:
      contentUri: The relative URI (without leading slash) that was requested.
        This should NOT contain an intl-prefix, if otherwise present.
      langName: The requested language.
      isIntl: True if contentUri originally contained an intl prefix.

    Results:
      boolean: True if a redirect has been set, False otherwise.
    """
    # Redirect data is stored in memcache for performance
    memcache_key = self.REDIRECT_PREFIX + contentUri
    redirect_data = memcache.get(memcache_key)
    if redirect_data is None:
      logging.info('Redirect cache miss. Computing new redirect data.\n'
                   'Memcache Key: ' + memcache_key)
      redirect_data = self.ComputeManualRedirectUrl(contentUri)
      memcache.set(memcache_key, redirect_data)
    contentUri = redirect_data[0]
    redirectType = redirect_data[1]

    # If this is an international URL, prepend intl path to minimize
    # number of redirects
    if isIntl:
      contentUri = '/%s%s%s' % (self.intlString, langName, contentUri)

    if redirectType is None:
      # No redirect necessary
      return False
    elif redirectType == self.REDIRECT_TYPE_PERM:
      logging.info('Sending permanent redirect: ' + contentUri);
      self.redirect(contentUri, permanent=True)
      return True
    elif redirectType == self.REDIRECT_TYPE_TEMP:
      logging.info('Sending temporary redirect: ' + contentUri);
      self.redirect(contentUri, permanent=False)
      return True
    else:
      # Invalid redirect type
      logging.error('Invalid redirect type: %s', redirectType)
      raise ('Invalid redirect type: %s', redirectType)

  def ComputeManualRedirectUrl(self, uri):
    """Read redirects file and evaluate redirect rules for a given URI.

    Args:
      uri: The relative URI (without leading slash) for which redirect data
        should be computed. No special handling of intl URIs is pefromed
        at this level.

    Returns:
      tuple: The computed redirect data. This tuple has two parts:
        redirect_uri: The new URI that should be used. (If no redirect rule is
          found, the original input to 'uri' will be returned.
        redirect_type: Either 'permanent' for an HTTP 301 redirect, 'temporary'
          for an HTTP 302 redirect, or None if no redirect should be performed.
    """
    # Redircts are defined in a file named redirects.yaml.
    try:
      f = open(self.REDIRECT_FILE)
      data = yaml.load(f)
      f.close()
    except IOError, e:
      logging.warning('Error opening redirect file (' + self.REDIRECT_FILE +
                      '): ' + e.strerror)
      return (uri, None)

    # The incoming path is missing a leading slash. However, many parts of the
    # redirect system require leading slashes to distinguish between relative
    # and absolute redirects. So, to compensate for this, we'll add a leading
    # slash here as well.
    uri = '/' + uri

    # Check to make sure we actually got an iterable list out of the YAML file
    if data is None:
      logging.warning('Redirect file (' + self.REDIRECT_FILE + ') not valid '
                      'YAML.')
    elif 'redirects' not in data:
      logging.warning('Redirect file (' + self.REDIRECT_FILE + ') not '
                      'properly formatted -- no \'redirects:\' header.')
    elif hasattr(data['redirects'], '__iter__'):
      # Iterate through redirect data, try to find a redirect that matches.
      for redirect in data['redirects']:
          # Note: re.search adds an implied '^' to the beginning of the regex
          # This means that the regex must match from the beginning of the
          # string.
          try:
            if re.match(redirect[self.REDIRECT_SRC], uri):
              # Match found. Apply redirect rule.
              redirect_uri = re.sub('^' + redirect[self.REDIRECT_SRC],
                  redirect[self.REDIRECT_DST], uri)
              logging.info('Redirect rule matched.\n'
                             'Rule: %s\n'
                             'Src: %s\n'
                             'Dst: %s',
                           redirect[self.REDIRECT_SRC], uri, redirect_uri)
              if self.REDIRECT_TYPE in redirect:
                redirect_type = redirect[self.REDIRECT_TYPE]
              else:
                # Default redirect type, if unspecified
                redirect_type = self.REDIRECT_TYPE_PERM
              return (redirect_uri, redirect_type)
          except:
            e = sys.exc_info()[1]
            raise ('Error while processing redirect rule.\n'
                     'Rule: %s\n'
                     'Error: %s' % (redirect[self.REDIRECT_SRC], e))
    # No redirect found, return URL unchanged
    return (uri, None)

  def isCleanUrl(self, name, langName, isValidIntl, isStripped):
    """Determine whether to pass an incoming url straight to processing.

       Args:
         name: The incoming URL

       Returns:
         boolean: Whether the URL should be sent straight to processing
    """
    # logging.info('  >>>> isCleanUrl name [%s] langName [%s] isValidIntl [%s]', name, langName, isValidIntl)
    if (langName == 'en' and not isStripped) or isValidIntl or not ('.html' in name) or (not isValidIntl and not langName):
      return True

  def PreprocessUrl(self, name, langName):
    """Any preprocessing work on the URL when it comes in.

    Put any work related to interpreting the incoming URL here. For example,
    this is used to redirect requests for a directory to the index.html file
    in that directory. Subclasses should override this method to do different
    preprocessing.

    Args:
      name: The incoming URL

    Returns:
      True if the request was redirected to '/index.html'.
      Otherewise False.
    """

    # determine if this is a request for a directory
    final_path_segment = name
    final_slash_offset = name.rfind('/')
    if final_slash_offset != len(name) - 1:
      final_path_segment = name[final_slash_offset + 1:]
      if final_path_segment.find('.') == -1:
        name = ''.join([name, '/'])

    # if this is a directory or the domain itself, redirect to /index.html
    if not name or (name[len(name) - 1:] == '/'):
      uri = ''.join(['/', name, 'index.html'])
      # logging.info('--->PREPROCESSING REDIRECT [%s] to [%s] with langName [%s]', name, uri, langName)
      self.redirect(uri, False)
      return True
    else:
      return False

  def RedirToIntl(self, name, intlString, langName):
    """Redirect an incoming request to the appropriate intl uri.

       For non-en langName, builds the intl/lang string from a
       base (en) string and redirects (302) the request to look for
       a version of the file in langName. For en langName, simply
       redirects a stripped uri string (intl/nn removed).

    Args:
      name: The incoming, preprocessed URL

    Returns:
      The lang-specific URL
    """
    if not (langName == 'en'):
      builtIntlLangUri = ''.join([intlString, langName, '/', name, '?', self.request.query_string])
    else:
      builtIntlLangUri = name
    uri = ''.join(['/', builtIntlLangUri])
    logging.info('-->REDIRECTING %s to  %s', name, uri)
    self.redirect(uri, False)
    return uri

  def CreateResponse(self, name, langName, isValidIntl, resetLangCookie):
    """Process the url and form a response, if appropriate.

       Attempts to retrieve the requested file (name) from cache,
       negative cache, or store (zip) and form the response.
       For intl requests that are not found (in the localized tree),
       returns False rather than forming a response, so that
       the request can be retried with the base url (this is the
       fallthrough to default language).

       For requests that are found, forms the headers and
       adds the content to the response entity. If the request was
       for an intl (localized) url, also resets the language cookie
       to the language specified in the url if needed, to ensure that
       the client language and response data remain harmonious.

    Args:
      name: The incoming, preprocessed URL
      langName: The language id. Used as necessary to reset the
                language cookie in the response.
      isValidIntl: If present, indicates whether the request is
                   for a language-specific url
      resetLangCookie: Whether the response should reset the
                       language cookie to 'langName'

    Returns:
      True: A response was successfully created for the request
      False: No response was created.
    """
    # see if we have the page in the memcache
    logging.info('PROCESSING %s langName [%s] isValidIntl [%s] resetLang [%s]',
      name, langName, isValidIntl, resetLangCookie)
    resp_data = self.GetFromCache(name)
    if resp_data is None:
      logging.info('  Cache miss for %s', name)
      resp_data = self.GetFromNegativeCache(name)
      if resp_data is None:
        resp_data = self.GetFromStore(name)

        # IF we have the file, put it in the memcache
        # ELSE put it in the negative cache
        if resp_data is not None:
          self.StoreOrUpdateInCache(name, resp_data)
        elif isValidIntl:
          # couldn't find the intl doc. Try to fall through to English.
          #logging.info('  Retrying with base uri...')
          return False
        else:
          logging.info('  Adding %s to negative cache, serving 404', name)
          self.StoreInNegativeCache(name)
          self.Write404Error()
          return True
      else:
        # found it in negative cache
        self.Write404Error()
        return True

    # found content from cache or store
    logging.info('FOUND CLEAN')
    if resetLangCookie:
      logging.info('  Resetting android_developer_pref_lang cookie to [%s]',
      langName)
      expireDate = time.mktime(localtime()) + 60 * 60 * 24 * 365 * 10
      self.response.headers.add_header('Set-Cookie',
      'android_developer_pref_lang=%s; path=/; expires=%s' %
      (langName, strftime("%a, %d %b %Y %H:%M:%S", localtime(expireDate))))
    mustRevalidate = False
    if ('.html' in name):
      # revalidate html files -- workaround for cache inconsistencies for
      # negotiated responses
      mustRevalidate = True
      #logging.info('  Adding [Vary: Cookie] to response...')
      self.response.headers.add_header('Vary', 'Cookie')
    content_type, encoding = mimetypes.guess_type(name)
    if content_type:
      self.response.headers['Content-Type'] = content_type
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif (name == 'favicon.ico'):
      self.response.headers['Content-Type'] = 'image/x-icon'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif name.endswith('.psd'):
      self.response.headers['Content-Type'] = 'application/octet-stream'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif name.endswith('.svg'):
      self.response.headers['Content-Type'] = 'image/svg+xml'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif name.endswith('.mp4'):
      self.response.headers['Content-Type'] = 'video/mp4'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif name.endswith('.webm'):
      self.response.headers['Content-Type'] = 'video/webm'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    elif name.endswith('.ogv'):
      self.response.headers['Content-Type'] = 'video/ogg'
      self.SetCachingHeaders(mustRevalidate)
      self.response.out.write(resp_data)
    return True

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

  def SetCachingHeaders(self, revalidate):
    """Set caching headers for the request."""
    max_age = self.MAX_AGE
    #self.response.headers['Expires'] = email.Utils.formatdate(
    #    time.time() + max_age, usegmt=True)
    cache_control = []
    if self.PUBLIC:
      cache_control.append('public')
    cache_control.append('max-age=%d' % max_age)
    if revalidate:
      cache_control.append('must-revalidate')
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
