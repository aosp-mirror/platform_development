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

"""A namedtuple to store a check result."""

from collections import namedtuple


CheckResultItem = namedtuple('CheckResultItem', 'title result_ok stderr')
"""The tuple to represent a check result.

  Props:
    title: A string to summarize the underlying check.
      e.g., 'SEPolicy check', 'Binder bitness check', etc.
    result_ok: True if the check passed, false otherwise.
    stderr: The stderr of the underlying executed command(s).
"""
