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

"""Contains utility functions for interacting with the Android build system."""

# Python imports
import os

# local imports
import errors
import logger


def GetTop():
  """Returns the full pathname of the "top" of the Android development tree.

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    the absolute file path of the Android build root.

  Raises:
    AbortError: if Android build root could not be found.
  """
  # TODO: does this need to be reimplemented to be like gettop() in envsetup.sh
  root_path = os.getenv('ANDROID_BUILD_TOP')
  if root_path is None:
    logger.Log('Error: ANDROID_BUILD_TOP not defined. Please run envsetup.sh')
    raise errors.AbortError
  return root_path
