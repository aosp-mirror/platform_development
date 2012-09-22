#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
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

"""In memory representation of Android.mk file.

Specifications for Android.mk can be found at
development/ndk/docs/ANDROID-MK.txt
"""

import os
import re
from sets import Set

import logger

class AndroidMK(object):
  """In memory representation of Android.mk file."""

  _RE_INCLUDE = re.compile(r'include\s+\$\((.+)\)')
  _RE_VARIABLE_REF = re.compile(r'\$\((.+)\)')
  _VAR_DELIMITER = ":="
  FILENAME = "Android.mk"
  CERTIFICATE = "LOCAL_CERTIFICATE"
  PACKAGE_NAME = "LOCAL_PACKAGE_NAME"

  def __init__(self):
    self._includes = Set() # variables included in makefile
    self._variables = {} # variables defined in makefile
    self._has_gtestlib = False

  def _ProcessMKLine(self, line):
    """Add a variable definition or include.

    Ignores unrecognized lines.

    Args:
      line: line of text from makefile
    """
    m = self._RE_INCLUDE.match(line)
    if m:
      self._includes.add(m.group(1))
    else:
      parts = line.split(self._VAR_DELIMITER)
      if len(parts) > 1:
        self._variables[parts[0].strip()] = parts[1].strip()
    # hack, look for explicit mention of libgtest_main
    if line.find('libgtest_main') != -1:
      self._has_gtestlib = True

  def GetVariable(self, identifier):
    """Retrieve makefile variable.

    Args:
      identifier: name of variable to retrieve
    Returns:
      value of specified identifier, None if identifier not found in makefile
    """
    # use dict.get(x) rather than dict[x] to avoid KeyError exception,
    # so None is returned if identifier not found
    return self._variables.get(identifier, None)

  def GetExpandedVariable(self, identifier):
    """Retrieve makefile variable.

    If variable value refers to another variable, recursively expand it to
    find its literal value

    Args:
      identifier: name of variable to retrieve
    Returns:
      value of specified identifier, None if identifier not found in makefile
    """
    # use dict.get(x) rather than dict[x] to avoid KeyError exception,
    # so None is returned if identifier not found
    return self.__RecursiveGetVariable(identifier, Set())

  def __RecursiveGetVariable(self, identifier, visited_variables):
    variable_value = self.GetVariable(identifier)
    if not variable_value:
      return None
    if variable_value in visited_variables:
      raise RuntimeError('recursive loop found for makefile variable %s'
                         % variable_value)
    m = self._RE_VARIABLE_REF.match(variable_value)
    if m:
      logger.SilentLog('Found variable ref %s for identifier %s'
                       % (variable_value, identifier))
      variable_ref = m.group(1)
      visited_variables.add(variable_ref)
      return self.__RecursiveGetVariable(variable_ref, visited_variables)
    else:
      return variable_value

  def HasInclude(self, identifier):
    """Check variable is included in makefile.

    Args:
      identifer: name of variable to check
    Returns:
      True if identifer is included in makefile, otherwise False
    """
    return identifier in self._includes

  def IncludesMakefilesUnder(self):
    """Check if makefile has a 'include makefiles under here' rule"""
    return self.HasInclude('call all-makefiles-under,$(LOCAL_PATH)')

  def HasJavaLibrary(self, library_name):
    """Check if library is specified as a local java library in makefile.

    Args:
      library_name: name of library to check
    Returns:
      True if library_name is included in makefile, otherwise False
    """
    java_lib_string = self.GetExpandedVariable('LOCAL_JAVA_LIBRARIES')
    if java_lib_string:
      java_libs = java_lib_string.split(' ')
      return library_name in java_libs
    return False

  def HasGTest(self):
    """Check if makefile includes rule to build a native gtest.

    Returns:
      True if rule to build native test is in makefile, otherwise False
    """
    return self._has_gtestlib or self.HasInclude('BUILD_NATIVE_TEST')

  def _ParseMK(self, mk_path):
    """Parse Android.mk at the specified path.

    Args:
      mk_path: path to Android.mk
    Raises:
      IOError: Android.mk cannot be found at given path, or cannot be opened
          for reading
    """
    mk = open(mk_path)
    for line in mk:
      self._ProcessMKLine(line)
    mk.close()


def CreateAndroidMK(path, filename=AndroidMK.FILENAME):
  """Factory method for creating a AndroidMK.

  Args:
    path: the directory of the make file
    filename: the filename of the makefile

  Return:
    the AndroidMK or None if there was no file present
  """
  mk_path = os.path.join(path, filename)
  if os.path.isfile(mk_path):
    mk = AndroidMK()
    mk._ParseMK(mk_path)
    return mk
  else:
    return None
