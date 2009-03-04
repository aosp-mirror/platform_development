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

# Python imports
import xml.dom.minidom
import xml.parsers
from sets import Set

# local imports
import logger
import errors

class TestDefinitions(object):
  """Accessor for a test definitions xml file
     Expected format is:
     <test-definitions>
        <test 
           name="" 
           package=""
           [runner=""]
           [class=""]
           [coverage_target=""]
           [build_path=""]
           [continuous]
         />
        <test  ...  
     </test-definitions> 
     
     TODO: add format checking
  """

  # tag/attribute constants
  _TEST_TAG_NAME = 'test'

  def __init__(self, ):
    # dictionary of test name to tests
    self._testname_map = {}
    
  def __iter__(self):
    return iter(self._testname_map.values())
      
  def Parse(self, file_path):
    """Parse the test suite data from from given file path, and add it to the 
       current object
       Args:
         file_path: absolute file path to parse
       Raises:
         errors.ParseError if file_path cannot be parsed  
      """
    try:
      doc = xml.dom.minidom.parse(file_path)
    except IOError:
      logger.Log('test file %s does not exist' % file_path)
      raise errors.ParseError
    except xml.parsers.expat.ExpatError:
      logger.Log('Error Parsing xml file: %s ' %  file_path)
      raise errors.ParseError
    return self._ParseDoc(doc)
  
  def ParseString(self, xml_string):
    """Alternate parse method that accepts a string of the xml data instead of a
      file
    """
    doc = xml.dom.minidom.parseString(xml_string)
    # TODO: catch exceptions and raise ParseError
    return self._ParseDoc(doc)  

  def _ParseDoc(self, doc):    
    suite_elements = doc.getElementsByTagName(self._TEST_TAG_NAME)

    for suite_element in suite_elements:
      test = self._ParseTestSuite(suite_element)
      self._AddTest(test)
  
  def _ParseTestSuite(self, suite_element):
    """Parse the suite element
       Returns a TestSuite object, populated with parsed data 
    """   
    test = TestSuite(suite_element)
    return test    
    
  def _AddTest(self, test):
    """ Adds a test to this TestManifest. If a test already exists with the
      same name, it overrides it"""  
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
    try:
      return self._testname_map[name]
    except KeyError:
      return None
  
class TestSuite:
  """ Represents one test suite definition parsed from xml """
  
  _NAME_ATTR = 'name'
  _PKG_ATTR = 'package'
  _RUNNER_ATTR = 'runner'
  _CLASS_ATTR = 'class'
  _TARGET_ATTR = 'coverage_target'
  _BUILD_ATTR = 'build_path'
  _CONTINUOUS_ATTR = 'continuous'
  
  _DEFAULT_RUNNER = 'android.test.InstrumentationTestRunner'
  
  def __init__(self, suite_element):
    """ Populates this instance's data from given suite xml element"""
    self._name = suite_element.getAttribute(self._NAME_ATTR)
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
      
  def GetName(self):
    return self._name
  
  def GetPackageName(self):
    return self._package

  def GetRunnerName(self):
    return self._runner
  
  def GetClassName(self):
    return self._class
  
  def GetTargetName(self):
    """ Retrieve module that this test is targeting - used to show code coverage
    """ 
    return self._target_name
  
  def GetBuildPath(self):
    """ Return the path, relative to device root, of this test's Android.mk file
    """
    return self._build_path

  def IsContinuous(self):
    """Returns true if test is flagged as continuous worthy"""  
    return self._continuous
  
def Parse(file_path):
  """parses out a TestDefinitions from given path to xml file
  Args:
    file_path: string absolute file path
  Raises:
    ParseError if xml format is not recognized
  """
  tests_result = TestDefinitions()
  tests_result.Parse(file_path)
  return tests_result
