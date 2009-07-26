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
import re
import subprocess

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
  root_path = os.getenv("ANDROID_BUILD_TOP")
  if root_path is None:
    logger.Log("Error: ANDROID_BUILD_TOP not defined. Please run envsetup.sh")
    raise errors.AbortError
  return root_path


def GetHostOsArch():
  """Identify the host os and arch.

  Returns:
    The triple (HOST_OS, HOST_ARCH, HOST_OS-HOST_ARCH).

  Raises:
    AbortError: If the os and/or arch could not be found.
  """
  command = ("CALLED_FROM_SETUP=true BUILD_SYSTEM=build/core "
             "make --no-print-directory -C \"%s\" -f build/core/config.mk "
             "dumpvar-report_config") % GetTop()

  # Use the shell b/c we set some env variables before the make command.
  config = subprocess.Popen(command, stdout=subprocess.PIPE,
                            shell=True).communicate()[0]
  host_os = re.search("HOST_OS=(\w+)", config).group(1)
  host_arch = re.search("HOST_ARCH=(\w+)", config).group(1)
  if not (host_os and host_arch):
    logger.Log("Error: Could not get host's OS and/or ARCH")
    raise errors.AbortError
  return (host_os, host_arch, "%s-%s" % (host_os, host_arch))


def GetHostBin():
  """Compute the full pathname to the host binary directory.

  Typically $ANDROID_BUILD_TOP/out/host/linux-x86/bin.

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    The absolute file path of the Android host binary directory.

  Raises:
    AbortError: if Android host binary directory could not be found.
  """
  (_, _, os_arch) = GetHostOsArch()
  path = os.path.join(GetTop(), "out", "host", os_arch, "bin")
  if not os.path.exists(path):
    logger.Log("Error: Host bin path could not be found %s" % path)
    raise errors.AbortError
  return path


def GetProductOut():
  """Returns the full pathname to the target/product directory.

  Typically the value of the env variable $ANDROID_PRODUCT_OUT.

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    The absolute file path of the Android product directory.

  Raises:
    AbortError: if Android product directory could not be found.
  """
  path = os.getenv("ANDROID_PRODUCT_OUT")
  if path is None:
    logger.Log("Error: ANDROID_PRODUCT_OUT not defined. Please run envsetup.sh")
    raise errors.AbortError
  return path


def GetTargetSystemBin():
  """Returns the full pathname to the target/product system/bin directory.

  Typically the value of the env variable $ANDROID_PRODUCT_OUT/system/bin

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    The absolute file path of the Android target system bin directory.

  Raises:
    AbortError: if Android target system bin directory could not be found.
  """
  path = os.path.join(GetProductOut(), "system", "bin")
  if not os.path.exists(path):
    logger.Log("Error: Target system bin path could not be found")
    raise errors.AbortError
  return path

def GetHostLibraryPath():
  """Returns the full pathname to the host java library output directory.

  Typically $ANDROID_BUILD_TOP/out/host/<host_os>/framework.

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    The absolute file path of the Android host java library directory.

  Raises:
    AbortError: if Android host java library directory could not be found.
  """
  (_, _, os_arch) = GetHostOsArch()
  path = os.path.join(GetTop(), "out", "host", os_arch, "framework")
  if not os.path.exists(path):
    logger.Log("Error: Host library path could not be found %s" % path)
    raise errors.AbortError
  return path

def GetTestAppPath():
  """Returns the full pathname to the test app build output directory.

  Typically $ANDROID_PRODUCT_OUT/data/app

  Assumes build environment has been properly configured by envsetup &
  lunch/choosecombo.

  Returns:
    The absolute file path of the Android test app build directory.
  """
  return os.path.join(GetProductOut(), "data", "app")
