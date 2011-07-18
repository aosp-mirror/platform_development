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

"""In memory representation of AndroidManifest.xml file.

Specification of AndroidManifest.xml can be found at
http://developer.android.com/guide/topics/manifest/manifest-intro.html
"""

# python imports
import os
import xml.dom.minidom
import xml.parsers


class AndroidManifest(object):
  """In memory representation of AndroidManifest.xml file."""

  FILENAME = 'AndroidManifest.xml'

  def __init__(self, app_path=None):
    if app_path:
      self._ParseManifest(app_path)

  def GetAppPath(self):
    """Retrieve file system path to this manifest file's directory."""
    return self._app_path

  def GetPackageName(self):
    """Retrieve package name defined at <manifest package="...">.

    Returns:
      Package name if defined, otherwise None
    """
    manifest = self._GetManifestElement()
    if not manifest or not manifest.hasAttribute('package'):
      return None
    return manifest.getAttribute('package')

  def _ParseManifest(self, app_path):
    """Parse AndroidManifest.xml at the specified path.

    Args:
      app_path: path to folder containing AndroidManifest.xml
    Raises:
      IOError: AndroidManifest.xml cannot be found at given path, or cannot be
          opened for reading
    """
    self._app_path = app_path
    self._manifest_path = os.path.join(app_path, self.FILENAME)
    self._dom = xml.dom.minidom.parse(self._manifest_path)

  def AddUsesSdk(self, min_sdk_version):
    """Adds a uses-sdk element to manifest.

    Args:
      min_sdk_version: value to provide for minSdkVersion attribute.
    """
    manifest = self._GetManifestElement()
    uses_sdk_elements = manifest.getElementsByTagName('uses-sdk')
    if uses_sdk_elements:
      uses_sdk_element = uses_sdk_elements[0]
    else:
      uses_sdk_element = self._dom.createElement('uses-sdk')
      manifest.appendChild(uses_sdk_element)

    uses_sdk_element.setAttribute('android:minSdkVersion', min_sdk_version)
    self._SaveXml()

  def GetInstrumentationNames(self):
    """Get the instrumentation names from manifest.

    Returns:
      list of names, might be empty
    """
    instr_elements = self._dom.getElementsByTagName('instrumentation')
    instrs = []
    for element in instr_elements:
      instrs.append(element.getAttribute('android:name'))
    return instrs

  def _GetManifestElement(self):
    """Retrieve the root manifest element.

    Returns:
      the DOM element for manifest or None.
    """
    manifests = self._dom.getElementsByTagName('manifest')
    if not manifests:
      return None
    return manifests[0]

  def _SaveXml(self):
    """Saves the manifest to disk."""
    self._dom.writexml(open(self._manifest_path, mode='w'), encoding='utf-8')


def CreateAndroidManifest(path):
  """Factory method for creating a AndroidManifest.

  Args:
    path: the directory for the manifest file

  Return:
    the AndroidManifest or None if there was no file present
  """
  manifest_path = os.path.join(path, AndroidManifest.FILENAME)
  if os.path.isfile(manifest_path):
    manifest = AndroidManifest()
    manifest._ParseManifest(path)
    return manifest
  else:
    return None
