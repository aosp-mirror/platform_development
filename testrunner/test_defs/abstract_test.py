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

"""Abstract Android test suite."""

# Python imports
import xml.dom.minidom
import xml.parsers

# local imports
import errors


class AbstractTestSuite(object):
  """Represents a generic test suite definition parsed from xml.

  This class will parse the XML attributes common to all TestSuite's.
  """

  # name of xml tag a test suite handles. subclasses must define this.
  TAG_NAME = "unspecified"

  _NAME_ATTR = "name"
  _BUILD_ATTR = "build_path"
  _CONTINUOUS_ATTR = "continuous"
  _CTS_ATTR = "cts"
  _DESCRIPTION_ATTR = "description"
  _EXTRA_BUILD_ARGS_ATTR = "extra_build_args"

  def __init__(self):
    self._attr_map = {}

  def Parse(self, suite_element):
    """Populates this instance's data from given suite xml element.
    Raises:
      ParseError if a required attribute is missing.
    """
    # parse name first so it can be used for error reporting
    self._ParseAttribute(suite_element, self._NAME_ATTR, True)
    self._ParseAttribute(suite_element, self._BUILD_ATTR, True)
    self._ParseAttribute(suite_element, self._CONTINUOUS_ATTR, False,
                         default_value=False)
    self._ParseAttribute(suite_element, self._CTS_ATTR, False,
                         default_value=False)
    self._ParseAttribute(suite_element, self._DESCRIPTION_ATTR, False,
                         default_value="")
    self._ParseAttribute(suite_element, self._EXTRA_BUILD_ARGS_ATTR, False,
                         default_value="")

  def _ParseAttribute(self, suite_element, attribute_name, mandatory,
                      default_value=None):
    if suite_element.hasAttribute(attribute_name):
      self._attr_map[attribute_name] = \
          suite_element.getAttribute(attribute_name)
    elif mandatory:
      error_msg = ("Could not find attribute %s in %s %s" %
          (attribute_name, self.TAG_NAME, self.GetName()))
      raise errors.ParseError(msg=error_msg)
    else:
      self._attr_map[attribute_name] = default_value

  def GetName(self):
    return self._GetAttribute(self._NAME_ATTR)

  def GetBuildPath(self):
    """Returns the build path of this test, relative to source tree root."""
    return self._GetAttribute(self._BUILD_ATTR)

  def GetBuildDependencies(self, options):
    """Returns a list of dependent build paths."""
    return []

  def IsContinuous(self):
    """Returns true if test is flagged as being part of the continuous tests"""
    return self._GetAttribute(self._CONTINUOUS_ATTR)

  def IsCts(self):
    """Returns true if test is part of the compatibility test suite"""
    return self._GetAttribute(self._CTS_ATTR)

  def GetDescription(self):
    """Returns a description if available, an empty string otherwise."""
    return self._GetAttribute(self._DESCRIPTION_ATTR)

  def GetExtraBuildArgs(self):
    """Returns the extra build args if available, an empty string otherwise."""
    return self._GetAttribute(self._EXTRA_BUILD_ARGS_ATTR)

  def _GetAttribute(self, attribute_name):
    return self._attr_map.get(attribute_name)

  def Run(self, options, adb):
    """Runs the test.

    Subclasses must implement this.
    Args:
      options: global command line options
    """
    raise NotImplementedError
