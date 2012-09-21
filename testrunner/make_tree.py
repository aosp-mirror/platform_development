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

"""Data structure for processing makefiles."""

import os

import android_build
import android_mk
import errors

class MakeNode(object):
  """Represents single node in make tree."""

  def __init__(self, name, parent):
    self._name = name
    self._children_map = {}
    self._is_leaf = False
    self._parent = parent
    self._includes_submake = None
    if parent:
      self._path = os.path.join(parent._GetPath(), name)
    else:
      self._path = ""

  def _AddPath(self, path_segs):
    """Adds given path to this node.

    Args:
      path_segs: list of path segments
    """
    if not path_segs:
      # done processing path
      return self
    current_seg = path_segs.pop(0)
    child = self._children_map.get(current_seg)
    if not child:
      child = MakeNode(current_seg, self)
      self._children_map[current_seg] = child
    return child._AddPath(path_segs)

  def _SetLeaf(self, is_leaf):
    self._is_leaf = is_leaf

  def _GetPath(self):
    return self._path

  def _DoesIncludesSubMake(self):
    if self._includes_submake is None:
      if self._is_leaf:
        path = os.path.join(android_build.GetTop(), self._path)
        mk_parser = android_mk.CreateAndroidMK(path)
        self._includes_submake = mk_parser.IncludesMakefilesUnder()
      else:
        self._includes_submake = False
    return self._includes_submake

  def _DoesParentIncludeMe(self):
    return self._parent and self._parent._DoesIncludesSubMake()

  def _BuildPrunedMakeList(self, make_list):
    if self._is_leaf and not self._DoesParentIncludeMe():
      make_list.append(os.path.join(self._path, "Android.mk"))
    for child in self._children_map.itervalues():
      child._BuildPrunedMakeList(make_list)


class MakeTree(MakeNode):
  """Data structure for building a non-redundant set of Android.mk paths.

  Used to collapse set of Android.mk files to use to prevent issuing make
  command that include same module multiple times due to include rules.
  """

  def __init__(self):
    super(MakeTree, self).__init__("", None)

  def AddPath(self, path):
    """Adds make directory path to tree.

    Will have no effect if path is already included in make set.

    Args:
      path: filesystem path to directory to build, relative to build root.
    """
    path = os.path.normpath(path)
    mk_path = os.path.join(android_build.GetTop(), path, "Android.mk")
    if not os.path.isfile(mk_path):
      raise errors.AbortError("%s does not exist" % mk_path)
    path_segs = path.split(os.sep)
    child = self._AddPath(path_segs)
    child._SetLeaf(True)

  def GetPrunedMakeList(self):
    """Return as list of the minimum set of Android.mk files necessary to
    build all leaf nodes in tree.
    """
    make_list = []
    self._BuildPrunedMakeList(make_list)
    return make_list

  def IsEmpty(self):
    return not self._children_map

