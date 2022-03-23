# Copyright 2018 - The Android Open Source Project
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

"""Merges SEPolicy files from platform and non-platform.

SEPolic files have been split into two parts: platform V.S. non-platform
(a.k.a. system V.S. vendor/odm, or framework V.S. non-framework).
The former files are stored on /system partition of a device, while the
latter files are stored on /vendor and/or /odm partitions of a device.

When the device boots a GSI, /init will merge SEPolicy files from those
partitions. If the merge fails, device will reboot to fastboot mode.
We can do the same SEPolicy merge on the host side to catch this error,
prior to use a GSI.

The SEPolicy merge logic can be found under function LoadSplitPolicy() in
the following source:
  https://android.googlesource.com/platform/system/core/+/master/init/selinux.cpp
"""

import logging

from gsi_util.checkers import check_result
from gsi_util.dumpers import xml_dumper
from gsi_util.utils import sepolicy_utils


# Constants for SEPolicy CIL (common intermediate language) files.
# SEPolicy files on /system.
_PLAT_SEPOLICY_CIL = '/system/etc/selinux/plat_sepolicy.cil'

# SEPolicy files on /vendor.
_VENDOR_VERSION_FILE = '/vendor/etc/selinux/plat_sepolicy_vers.txt'
_NONPLAT_SEPOLICY_CIL = '/vendor/etc/selinux/nonplat_sepolicy.cil'
# _NONPLAT_SEPOLICY_CIL has been renamed/split into the following two files.
_VENDOR_SEPOLICY_CIL = '/vendor/etc/selinux/vendor_sepolicy.cil'
_VENDOR_PLAT_PUB_SEPOLICY_CIL = '/vendor/etc/selinux/plat_pub_versioned.cil'

# SEPolicy file on /odm.
_ODM_SEPOLICY_CIL = '/odm/etc/selinux/odm_sepolicy.cil'

# System compatiblity file, required to get expected kernel sepolicy version.
_SYSTEM_COMPATIBILITY_MATRIX = '/system/compatibility_matrix.xml'

class SepolicyChecker(object):   # pylint: disable=too-few-public-methods
  """Checks SEPolicy can be merged between GSI and device images."""

  def __init__(self, file_accessor):
    """Inits a SEPolicy checker with a given file_accessor.

    Args:
      file_accessor: Provides file access to get required sepolicy files
      that are installed on /system and /vendor (and/or /odm) partitions of
      a device.
    """
    self._file_accessor = file_accessor

  def _get_vendor_mapping_version(self):
    """Gets the platform sepolicy version that vendor used.

    Note that the version is introduced in project Treble and isn't related
    to kernel SELinux policy version. In general, it will be aligned with
    Android SDK version: 26.0, 27.0, etc. For more details, please refer to:
      https://android.googlesource.com/platform/system/sepolicy/+/master/Android.mk

    Returns:
      A string indicating the version, e.g., '26.0'.

    Raises:
      RuntimeError: An error occurred when accessing required files.
    """

    with self._file_accessor.prepare_file(_VENDOR_VERSION_FILE) as version:
      if not version:
        raise RuntimeError('Failed to open: {}'.format(_VENDOR_VERSION_FILE))
      return open(version).readline().strip()

  def _get_kernel_policy_version(self):
    """Gets the kernel policy version that framework expects.

    The version is the SELinux policy version used by kernel. It can be
    obtained via '/sys/fs/selinux/policyvers' on a running device. The
    version is also included in system compatibility matrix to denote
    the policy version used in system sepolicy files.

    Returns:
      A string indicating the kernel SELinux policy version, e.g., '30'.
    """
    # XmlDumper expects the 2nd argument as a sequence.
    # And it only takes the first element as the file name to open.
    with xml_dumper.XmlDumper(
        self._file_accessor,
        (_SYSTEM_COMPATIBILITY_MATRIX,)) as dumper_instance:
      return dumper_instance.dump('./sepolicy/kernel-sepolicy-version')

  def check(self):
    """Merges system and vendor/odm SEPolicy files.

    Returns:
      A list of a single check_result.CheckResultItem() tuple.

    Raises:
      RuntimeError: An error occurred when accessing required files.
    """
    policy_version = self._get_kernel_policy_version()
    secilc_options = {'multiple-decl': None, 'mls': 'true',
                      'expand-generated': None, 'disable-neverallow': None,
                      'policyvers': policy_version, 'output': '/dev/null',
                      'filecontext': '/dev/null'}

    cil_files = []
    # Selects the mapping file based on vendor-used platform version.
    vendor_plat_vers = self._get_vendor_mapping_version()
    mapping_sepolicy_cil = '/system/etc/selinux/mapping/{}.cil'.format(
        vendor_plat_vers)

    with self._file_accessor.prepare_multi_files([
        _PLAT_SEPOLICY_CIL,
        mapping_sepolicy_cil,
        _NONPLAT_SEPOLICY_CIL,
        _VENDOR_SEPOLICY_CIL,
        _VENDOR_PLAT_PUB_SEPOLICY_CIL,
        _ODM_SEPOLICY_CIL]) as [plat_sepolicy, mapping_sepolicy,
                                nonplat_sepolicy, vendor_sepolicy,
                                vendor_plat_pub_sepolicy, odm_sepolicy]:
      if not plat_sepolicy:
        raise RuntimeError('Failed to open: {}'.format(_PLAT_SEPOLICY_CIL))
      if not mapping_sepolicy:
        raise RuntimeError('Failed to open: {}'.format(mapping_sepolicy_cil))
      cil_files += [plat_sepolicy, mapping_sepolicy]

      # nonplat_sepolicy has been split to vendor_sepolicy +
      # vendor_plat_pub_sepolicy. Here we support both configs.
      if nonplat_sepolicy:   # For legacy devices in Oreo/Oreo-MR1.
        cil_files += [nonplat_sepolicy]
        logging.debug('Using nonplat sepolicy: %r', _NONPLAT_SEPOLICY_CIL)
      elif vendor_sepolicy and vendor_plat_pub_sepolicy:
        cil_files += [vendor_sepolicy, vendor_plat_pub_sepolicy]
        logging.debug('Using vendor sepolicy: %r and %r',
                      _VENDOR_SEPOLICY_CIL, _VENDOR_PLAT_PUB_SEPOLICY_CIL)
      else:
        raise RuntimeError(
            'Failed to open vendor sepolicy files.\n'
            'Either {!r} or {!r}/{!r} should present'.format(
                _NONPLAT_SEPOLICY_CIL, _VENDOR_SEPOLICY_CIL,
                _VENDOR_PLAT_PUB_SEPOLICY_CIL))

      if odm_sepolicy:   # odm_sepolicy is optional.
        cil_files += [odm_sepolicy]

      # Executes the merge command.
      result_ok, stderr = sepolicy_utils.secilc(secilc_options, cil_files)

      # The caller (checker) expects to receive a list of CheckResultItems.
      return [check_result.CheckResultItem('SEPolicy merge test',
                                           result_ok,
                                           stderr)]
