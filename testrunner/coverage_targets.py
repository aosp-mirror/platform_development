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
import xml.dom.minidom
import xml.parsers
import os

import logger
import errors

class CoverageTargets:
  """Accessor for the code coverage target xml file 
  Expects the following format:
  <targets>
    <target 
      name=""
      type="JAVA_LIBRARIES|APPS"
      build_path=""
      
      [<src path=""/>] (0..*)  - These are relative to build_path. If missing, 
                                 assumes 'src'
    >/target>
    
    TODO: add more format checking  
  """
  
  _TARGET_TAG_NAME = 'coverage_target' 
  
  def __init__(self, ):
    self._target_map= {}
    
  def __iter__(self):
    return iter(self._target_map.values())
      
  def Parse(self, file_path):
    """Parse the coverage target data from from given file path, and add it to 
       the current object
       Args:
         file_path: absolute file path to parse
       Raises:
         errors.ParseError if file_path cannot be parsed  
    """
    try:
      doc = xml.dom.minidom.parse(file_path)
    except IOError:
      # Error: The results file does not exist 
      logger.Log('Results file %s does not exist' % file_path)
      raise errors.ParseError
    except xml.parsers.expat.ExpatError:
      logger.Log('Error Parsing xml file: %s ' %  file_path)
      raise errors.ParseError
    
    target_elements = doc.getElementsByTagName(self._TARGET_TAG_NAME)

    for target_element in target_elements:
      target = CoverageTarget(target_element)
      self._AddTarget(target)
    
  def _AddTarget(self, target): 
    self._target_map[target.GetName()] = target
     
  def GetBuildTargets(self):
    """ returns list of target names """
    build_targets = []
    for target in self:
      build_targets.append(target.GetName())
    return build_targets   
  
  def GetTargets(self):
    """ returns list of CoverageTarget"""
    return self._target_map.values()
  
  def GetTarget(self, name):
    """ returns CoverageTarget for given name. None if not found """
    try:
      return self._target_map[name]
    except KeyError:
      return None
  
class CoverageTarget:
  """ Represents one coverage target definition parsed from xml """
  
  _NAME_ATTR = 'name'
  _TYPE_ATTR = 'type'
  _BUILD_ATTR = 'build_path'
  _SRC_TAG = 'src'
  _PATH_ATTR = 'path'
  
  def __init__(self, target_element):
    self._name = target_element.getAttribute(self._NAME_ATTR)
    self._type = target_element.getAttribute(self._TYPE_ATTR)
    self._build_path = target_element.getAttribute(self._BUILD_ATTR)
    self._paths = []
    self._ParsePaths(target_element)
    
  def GetName(self):
    return self._name

  def GetPaths(self):
    return self._paths
  
  def GetType(self):
    return self._type

  def GetBuildPath(self):
    return self._build_path
  
  def _ParsePaths(self, target_element):
    src_elements = target_element.getElementsByTagName(self._SRC_TAG)
    if len(src_elements) <= 0:
      # no src tags specified. Assume build_path + src
      self._paths.append(os.path.join(self.GetBuildPath(), "src"))
    for src_element in src_elements:
      rel_path = src_element.getAttribute(self._PATH_ATTR)
      self._paths.append(os.path.join(self.GetBuildPath(), rel_path))
  
def Parse(xml_file_path):
  """parses out a file_path class from given path to xml"""
  targets = CoverageTargets()
  targets.Parse(xml_file_path)
  return targets
