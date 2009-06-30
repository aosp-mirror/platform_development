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

import re
from sets import Set


class AndroidMK(object):
  """In memory representation of Android.mk file."""

  _RE_INCLUDE = re.compile(r'include\s+\$\((.+)\)')
  _VAR_DELIMITER = ":="
  FILENAME = "Android.mk"
  CERTIFICATE = "LOCAL_CERTIFICATE"
  PACKAGE_NAME = "LOCAL_PACKAGE_NAME"

  def __init__(self, app_path=None):
    self._includes = Set() # variables included in makefile
    self._variables = {} # variables defined in makefile

    if app_path:
      self.ParseMK(app_path)

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

  def HasInclude(self, identifier):
    """Check variable is included in makefile.

    Args:
      identifer: name of variable to check
    Returns:
      True if identifer is included in makefile, otherwise False
    """
    return identifier in self._includes

  def ParseMK(self, app_path):
    """Parse Android.mk at the specified path.

    Args:
      app_path: path to folder containing Android.mk
    Raises:
      IOError: Android.mk cannot be found at given path, or cannot be opened
          for reading
    """
    self.app_path = app_path.rstrip("/")
    self.mk_path = "%s/%s" % (self.app_path, self.FILENAME)
    mk = open(self.mk_path)
    for line in mk:
      self._ProcessMKLine(line)
    mk.close()
