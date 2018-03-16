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

"""Provide the information list for command 'dump'."""

from collections import namedtuple

from gsi_util.dumpers.prop_dumper import PropDumper
from gsi_util.dumpers.xml_dumper import XmlDumper

SYSTEM_MATRIX_DUMPER = (XmlDumper, '/system/compatibility_matrix.xml')
SYSTEM_BUILD_PROP_DUMPER = (PropDumper, '/system/build.prop')
SYSTEM_MANIFEST_DUMPER = (PropDumper, '/system/manifest.xml')

VENDOR_DEFAULT_PROP_DUMPER = (PropDumper, '/vendor/default.prop')
VENDOR_BUILD_PROP_DUMPER = (PropDumper, '/vendor/build.prop')

DumpInfoListItem = namedtuple('DumpInfoListItem',
                              'info_name dumper_create_args lookup_key')

# The total list of all possible dump info.
# It will be output by the order of the list.
DUMP_LIST = [
    # System
    DumpInfoListItem('system_fingerprint', SYSTEM_BUILD_PROP_DUMPER, 'ro.build.fingerprint'),
    DumpInfoListItem('system_sdk_ver', SYSTEM_BUILD_PROP_DUMPER, 'ro.build.version.sdk'),
    DumpInfoListItem('system_vndk_ver', SYSTEM_BUILD_PROP_DUMPER, 'ro.vndk.version'),
    DumpInfoListItem('system_security_patch_level', SYSTEM_BUILD_PROP_DUMPER, 'ro.build.version.security_patch'),
    DumpInfoListItem('system_kernel_sepolicy_ver', SYSTEM_MATRIX_DUMPER, './sepolicy/kernel-sepolicy-version'),
    DumpInfoListItem('system_support_sepolicy_ver', SYSTEM_MATRIX_DUMPER, './sepolicy/sepolicy-version'),
    DumpInfoListItem('system_avb_ver', SYSTEM_MATRIX_DUMPER, './avb/vbmeta-version'),
    # Vendor
    DumpInfoListItem('vendor_fingerprint', VENDOR_BUILD_PROP_DUMPER, 'ro.vendor.build.fingerprint'),
    DumpInfoListItem('vendor_vndk_ver', VENDOR_BUILD_PROP_DUMPER, 'ro.vndk.version'),
    DumpInfoListItem('vendor_security_patch_level', SYSTEM_BUILD_PROP_DUMPER, 'ro.vendor.build.version.security_patch'),
    DumpInfoListItem('vendor_low_ram', VENDOR_BUILD_PROP_DUMPER, 'ro.config.low_ram'),
    DumpInfoListItem('vendor_zygote', VENDOR_DEFAULT_PROP_DUMPER, 'ro.zygote'),
    DumpInfoListItem('vendor_oem_unlock_supported', VENDOR_DEFAULT_PROP_DUMPER, 'oem_unlock_supported'),
    DumpInfoListItem('vendor_adb_secure', VENDOR_DEFAULT_PROP_DUMPER, 'ro.adb.secure'),
]  # pyformat: disable
