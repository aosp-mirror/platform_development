# Copyright 2017 - The Android Open Source Project
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

"""Runs Treble compatibility check between /system and /vendor.

One of the major goal of project Treble is to do system-only OTA across
major Android releases (e.g., O -> P). VINTF check is to ensure a given
system.img can work well on a vendor.img, including HALs versions match,
kernel version match, SEPolicy version match, etc. See the following link
for more details:

  https://source.android.com/devices/architecture/vintf/
"""

from gsi_util.checkers import check_result
from gsi_util.utils import vintf_utils


class VintfChecker(object):   # pylint: disable=too-few-public-methods
  """The checker to perform VINTF check between /system and /vendor."""

  # A dict to specify required VINTF checks.
  # Each item is a tuple containing a (manifest, matrix) pair for the match
  # check.
  _REQUIRED_CHECKS = {
      'Framework manifest match': ('/system/manifest.xml',
                                   '/vendor/compatibility_matrix.xml'),
      'Device manifest match': ('/vendor/manifest.xml',
                                '/system/compatibility_matrix.xml'),
  }

  def __init__(self, file_accessor):
    """Inits a VINTF checker with a given file_accessor.

    Args:
      file_accessor: Provides file access to get files that are installed
      on /system and /vendor partition of a device.
    """
    self._file_accessor = file_accessor

  def check(self):
    """Performs the Treble VINTF compatibility check.

    Returns:
      A list of check_result.CheckResultItem() tuples.

    Raises:
      RuntimeError: An error occurred when accessing required files.
    """
    check_result_items = []

    for title in self._REQUIRED_CHECKS:
      manifest_filename, matrix_filename = self._REQUIRED_CHECKS[title]

      with self._file_accessor.prepare_multi_files(
          [manifest_filename, matrix_filename]) as [manifest, matrix]:
        if not manifest:
          raise RuntimeError('Failed to open: {}'.format(manifest_filename))
        if not matrix:
          raise RuntimeError('Failed to open: {}'.format(matrix_filename))

        # Runs the check item and appends the result.
        result_ok, stderr = vintf_utils.checkvintf(manifest, matrix)
        check_result_items.append(
            check_result.CheckResultItem(title, result_ok, stderr))

    return check_result_items
