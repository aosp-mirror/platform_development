#!/usr/bin/python3
#
# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
"""Constants for development/tools/compare_cts_reports/."""


VERSION = '1.0'

NO_DATA = 'null'
TESTED_ITEMS = 'tested_items'
PASS_RATE = 'pass_rate'

ABI_IGNORED = 'abi-ignored'
ABI_ARM_V7A = 'armeabi-v7a'
ABI_ARM_V8A = 'arm64-v8a'
ABI_X86 = 'x86'
ABI_X86_64 = 'x86_64'
ALL_TEST_ABIS = [ABI_ARM_V7A, ABI_ARM_V8A, ABI_X86, ABI_X86_64]
