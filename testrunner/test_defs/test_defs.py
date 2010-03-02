#!/usr/bin/python2.4
#
#
# Copyright 2008, The Android Open Source Project
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

"""Parser for test definition xml files."""

# Python imports
import xml.dom.minidom
import xml.parsers

# local imports
import errors
import logger
import xml_suite_helper


class TestDefinitions(object):
  """Accessor for a test definitions xml file data.

  See test_defs.xsd for expected format.
  """

  def __init__(self):
    # dictionary of test name to tests
    self._testname_map = {}

  def __iter__(self):
    ordered_list = []
    for k in sorted(self._testname_map):
      ordered_list.append(self._testname_map[k])
    return iter(ordered_list)

  def Parse(self, file_path):
    """Parse the test suite data from from given file path.

    Args:
      file_path: absolute file path to parse
    Raises:
      ParseError if file_path cannot be parsed
    """
    try:
      doc = xml.dom.minidom.parse(file_path)
      self._ParseDoc(doc)
    except IOError:
      logger.Log("test file %s does not exist" % file_path)
      raise errors.ParseError
    except xml.parsers.expat.ExpatError:
      logger.Log("Error Parsing xml file: %s " %  file_path)
      raise errors.ParseError
    except errors.ParseError, e:
      logger.Log("Error Parsing xml file: %s Reason: %s" %  (file_path, e.msg))
      raise e

  def ParseString(self, xml_string):
    """Alternate parse method that accepts a string of the xml data."""
    doc = xml.dom.minidom.parseString(xml_string)
    # TODO: catch exceptions and raise ParseError
    return self._ParseDoc(doc)

  def _ParseDoc(self, doc):
    root_element = self._GetRootElement(doc)
    suite_parser = xml_suite_helper.XmlSuiteParser()
    for element in root_element.childNodes:
      if element.nodeType != xml.dom.Node.ELEMENT_NODE:
        continue
      test_suite = suite_parser.Parse(element)
      if test_suite:
        self._AddTest(test_suite)

  def _GetRootElement(self, doc):
    root_elements = doc.getElementsByTagName("test-definitions")
    if len(root_elements) != 1:
      error_msg = "expected 1 and only one test-definitions tag"
      raise errors.ParseError(msg=error_msg)
    return root_elements[0]

  def _AddTest(self, test):
    """Adds a test to this TestManifest.

    If a test already exists with the same name, it overrides it.

    Args:
      test: TestSuite to add
    """
    if self.GetTest(test.GetName()) is not None:
      logger.SilentLog("Overriding test definition %s" % test.GetName())
    self._testname_map[test.GetName()] = test

  def GetTests(self):
    return self._testname_map.values()

  def GetContinuousTests(self):
    con_tests = []
    for test in self.GetTests():
      if test.IsContinuous():
        con_tests.append(test)
    return con_tests

  def GetTestsInSuite(self, suite):
    """Return list of tests in given suite."""
    return [t for t in self.GetTests() if t.GetSuite() == suite]

  def GetTest(self, name):
    return self._testname_map.get(name, None)


def Parse(file_path):
  """Parses out a TestDefinitions from given path to xml file.

  Args:
    file_path: string absolute file path
  Returns:
    a TestDefinitions object containing data parsed from file_path
  Raises:
    ParseError if xml format is not recognized
  """
  tests_result = TestDefinitions()
  tests_result.Parse(file_path)
  return tests_result
