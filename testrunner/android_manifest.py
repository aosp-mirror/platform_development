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
import xml.dom.minidom
import xml.parsers


class AndroidManifest(object):
  """In memory representation of AndroidManifest.xml file."""

  FILENAME = "AndroidManifest.xml"

  def __init__(self, app_path=None):
    if app_path:
      self.ParseManifest(app_path)

  def GetPackageName(self):
    """Retrieve package name defined at <manifest package="...">.

    Returns:
      Package name if defined, otherwise None
    """
    manifests = self._dom.getElementsByTagName("manifest")
    if not manifests or not manifests[0].getAttribute("package"):
      return None
    return manifests[0].getAttribute("package")

  def ParseManifest(self, app_path):
    """Parse AndroidManifest.xml at the specified path.

    Args:
      app_path: path to folder containing AndroidManifest.xml
    Raises:
      IOError: AndroidManifest.xml cannot be found at given path, or cannot be
          opened for reading
    """
    self.app_path = app_path.rstrip("/")
    self.manifest_path = "%s/%s" % (self.app_path, self.FILENAME)
    self._dom = xml.dom.minidom.parse(self.manifest_path)
