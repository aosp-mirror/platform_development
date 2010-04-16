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

"""Utility to parse suite info from xml."""

# Python imports
import xml.dom.minidom
import xml.parsers

# local imports
import errors
import logger
import host_test
import instrumentation_test
import native_test


class XmlSuiteParser(object):
  """Parses XML attributes common to all TestSuite's."""

  # common attributes
  _NAME_ATTR = 'name'
  _BUILD_ATTR = 'build_path'
  _CONTINUOUS_ATTR = 'continuous'
  _SUITE_ATTR = 'suite'
  _DESCRIPTION_ATTR = 'description'
  _EXTRA_BUILD_ARGS_ATTR = 'extra_build_args'
  _FULL_MAKE_ATTR = 'full_make'

  def Parse(self, element):
    """Populates common suite attributes from given suite xml element.

    Args:
      element: xml node to parse
    Raises:
      ParseError if a required attribute is missing.
    Returns:
      parsed test suite or None
    """
    parser = None
    if element.nodeName == InstrumentationParser.TAG_NAME:
      parser = InstrumentationParser()
    elif element.nodeName == NativeParser.TAG_NAME:
      parser = NativeParser()
    elif element.nodeName == HostParser.TAG_NAME:
      parser = HostParser()
    else:
      logger.Log('Unrecognized tag %s found' % element.nodeName)
      return None
    test_suite = parser.Parse(element)
    return test_suite

  def _ParseCommonAttributes(self, suite_element, test_suite):
    test_suite.SetName(self._ParseAttribute(suite_element, self._NAME_ATTR,
                                            True))
    test_suite.SetBuildPath(self._ParseAttribute(suite_element,
                                                 self._BUILD_ATTR, True))
    test_suite.SetContinuous(self._ParseAttribute(suite_element,
                                                  self._CONTINUOUS_ATTR,
                                                  False, default_value=False))
    test_suite.SetSuite(self._ParseAttribute(suite_element, self._SUITE_ATTR, False,
                                           default_value=None))
    test_suite.SetDescription(self._ParseAttribute(suite_element,
                                                   self._DESCRIPTION_ATTR,
                                                   False,
                                                   default_value=''))
    test_suite.SetExtraBuildArgs(self._ParseAttribute(
        suite_element, self._EXTRA_BUILD_ARGS_ATTR, False, default_value=''))
    test_suite.SetIsFullMake(self._ParseAttribute(
        suite_element, self._FULL_MAKE_ATTR, False, default_value=False))


  def _ParseAttribute(self, suite_element, attribute_name, mandatory,
                      default_value=None):
    if suite_element.hasAttribute(attribute_name):
      value = suite_element.getAttribute(attribute_name)
    elif mandatory:
      error_msg = ('Could not find attribute %s in %s' %
                   (attribute_name, self.TAG_NAME))
      raise errors.ParseError(msg=error_msg)
    else:
      value = default_value
    return value


class InstrumentationParser(XmlSuiteParser):
  """Parses instrumentation suite attributes from xml."""

  # for legacy reasons, the xml tag name for java (device) tests is 'test'
  TAG_NAME = 'test'

  _PKG_ATTR = 'package'
  _RUNNER_ATTR = 'runner'
  _CLASS_ATTR = 'class'
  _TARGET_ATTR = 'coverage_target'

  def Parse(self, suite_element):
    """Creates suite and populate with data from xml element."""
    suite = instrumentation_test.InstrumentationTestSuite()
    XmlSuiteParser._ParseCommonAttributes(self, suite_element, suite)
    suite.SetPackageName(self._ParseAttribute(suite_element, self._PKG_ATTR,
                                              True))
    suite.SetRunnerName(self._ParseAttribute(
        suite_element, self._RUNNER_ATTR, False,
        instrumentation_test.InstrumentationTestSuite.DEFAULT_RUNNER))
    suite.SetClassName(self._ParseAttribute(suite_element, self._CLASS_ATTR,
                                            False))
    suite.SetTargetName(self._ParseAttribute(suite_element, self._TARGET_ATTR,
                                             False))
    return suite


class NativeParser(XmlSuiteParser):
  """Parses native suite attributes from xml."""

  TAG_NAME = 'test-native'

  def Parse(self, suite_element):
    """Creates suite and populate with data from xml element."""
    suite = native_test.NativeTestSuite()
    XmlSuiteParser._ParseCommonAttributes(self, suite_element, suite)
    return suite


class HostParser(XmlSuiteParser):
  """Parses host suite attributes from xml."""

  TAG_NAME = 'test-host'

  _CLASS_ATTR = 'class'
  # TODO: consider obsoleting in favor of parsing the Android.mk to find the
  # jar name
  _JAR_ATTR = 'jar_name'

  def Parse(self, suite_element):
    """Creates suite and populate with data from xml element."""
    suite = host_test.HostTestSuite()
    XmlSuiteParser._ParseCommonAttributes(self, suite_element, suite)
    suite.SetClassName(self._ParseAttribute(suite_element, self._CLASS_ATTR,
                                            True))
    suite.SetJarName(self._ParseAttribute(suite_element, self._JAR_ATTR, True))
    return suite
