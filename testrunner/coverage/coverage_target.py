#
# Copyright 2012, The Android Open Source Project
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

class CoverageTarget:
  """ Represents a code coverage target definition"""

  def __init__(self):
    self._name = None
    self._type = None
    self._build_path = None
    self._paths = []

  def GetName(self):
    return self._name

  def SetName(self, name):
    self._name = name

  def GetPaths(self):
    return self._paths

  def AddPath(self, path):
    self._paths.append(path)

  def GetType(self):
    return self._type

  def SetType(self, buildtype):
    self._type = buildtype

  def GetBuildPath(self):
    return self._build_path

  def SetBuildPath(self, build_path):
    self._build_path = build_path

