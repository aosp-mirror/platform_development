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


class TestDefinitions(object):
  """Accessor for a test definitions xml file data.

  Expected format is:
   <test-definitions>
     <test
       name=""
       package=""
       [runner=""]
       [class=""]
       [coverage_target=""]
       [build_path=""]
       [continuous=false]
       [description=""]
      />
     <test-native
       name=""
       build_path=""
       [continuous=false]
       [description=""]
      />
     <test  ...
   </test-definitions>

  TODO: add format checking.
  """

  # tag/attribute constants
  _TEST_TAG_NAME = "test"
  _TEST_NATIVE_TAG_NAME = "test-native"

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
    except IOError:
      logger.Log("test file %s does not exist" % file_path)
      raise errors.ParseError
    except xml.parsers.expat.ExpatError:
      logger.Log("Error Parsing xml file: %s " %  file_path)
      raise errors.ParseError
    self._ParseDoc(doc)

  def ParseString(self, xml_string):
    """Alternate parse method that accepts a string of the xml data."""
    doc = xml.dom.minidom.parseString(xml_string)
    # TODO: catch exceptions and raise ParseError
    return self._ParseDoc(doc)

  def _ParseDoc(self, doc):
    suite_elements = doc.getElementsByTagName(self._TEST_TAG_NAME)

    for suite_element in suite_elements:
      test = self._ParseTestSuite(suite_element)
      self._AddTest(test)

    suite_elements = doc.getElementsByTagName(self._TEST_NATIVE_TAG_NAME)

    for suite_element in suite_elements:
      test = self._ParseNativeTestSuite(suite_element)
      self._AddTest(test)

  def _ParseTestSuite(self, suite_element):
    """Parse the suite element.
    
    Returns:
      a TestSuite object, populated with parsed data
    """
    test = TestSuite(suite_element)
    return test

  def _ParseNativeTestSuite(self, suite_element):
    """Parse the native test element.

    Returns:
      a TestSuite object, populated with parsed data
    Raises:
      ParseError if some required attribute is missing.
    """
    test = TestSuite(suite_element, native=True)
    return test

  def _AddTest(self, test):
    """Adds a test to this TestManifest.
    
    If a test already exists with the same name, it overrides it.
    
    Args:
      test: TestSuite to add
    """
    self._testname_map[test.GetName()] = test

  def GetTests(self):
    return self._testname_map.values()

  def GetContinuousTests(self):
    con_tests = []
    for test in self.GetTests():
      if test.IsContinuous():
        con_tests.append(test)
    return con_tests

  def GetTest(self, name):
    return self._testname_map.get(name, None)
  
class TestSuite(object):
  """Represents one test suite definition parsed from xml."""

  _NAME_ATTR = "name"
  _PKG_ATTR = "package"
  _RUNNER_ATTR = "runner"
  _CLASS_ATTR = "class"
  _TARGET_ATTR = "coverage_target"
  _BUILD_ATTR = "build_path"
  _CONTINUOUS_ATTR = "continuous"
  _DESCRIPTION_ATTR = "description"

  _DEFAULT_RUNNER = "android.test.InstrumentationTestRunner"

  def __init__(self, suite_element, native=False):
    """Populates this instance's data from given suite xml element.
    Raises:
      ParseError if some required attribute is missing.
    """
    self._native = native
    self._name = suite_element.getAttribute(self._NAME_ATTR)

    if self._native:
      # For native runs, _BUILD_ATTR is required
      if not suite_element.hasAttribute(self._BUILD_ATTR):
        logger.Log("Error: %s is missing required build_path attribute" %
                   self._name)
        raise errors.ParseError
    else:
      self._package = suite_element.getAttribute(self._PKG_ATTR)

    if suite_element.hasAttribute(self._RUNNER_ATTR):
      self._runner = suite_element.getAttribute(self._RUNNER_ATTR)
    else:
      self._runner = self._DEFAULT_RUNNER
    if suite_element.hasAttribute(self._CLASS_ATTR):
      self._class = suite_element.getAttribute(self._CLASS_ATTR)
    else:
      self._class = None
    if suite_element.hasAttribute(self._TARGET_ATTR):
      self._target_name = suite_element.getAttribute(self._TARGET_ATTR)
    else:
      self._target_name = None
    if suite_element.hasAttribute(self._BUILD_ATTR):
      self._build_path = suite_element.getAttribute(self._BUILD_ATTR)
    else:
      self._build_path = None
    if suite_element.hasAttribute(self._CONTINUOUS_ATTR):
      self._continuous = suite_element.getAttribute(self._CONTINUOUS_ATTR)
    else:
      self._continuous = False
    if suite_element.hasAttribute(self._DESCRIPTION_ATTR):
      self._description = suite_element.getAttribute(self._DESCRIPTION_ATTR)
    else:
      self._description = ""

  def GetName(self):
    return self._name

  def GetPackageName(self):
    return self._package

  def GetRunnerName(self):
    return self._runner

  def GetClassName(self):
    return self._class

  def GetTargetName(self):
    """Retrieve module that this test is targeting.
    
    Used for generating code coverage metrics.
    """ 
    return self._target_name

  def GetBuildPath(self):
    """Returns the build path of this test, relative to source tree root."""
    return self._build_path

  def IsContinuous(self):
    """Returns true if test is flagged as being part of the continuous tests"""  
    return self._continuous

  def IsNative(self):
    """Returns true if test is a native one."""
    return self._native

  def GetDescription(self):
    return self._description

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
