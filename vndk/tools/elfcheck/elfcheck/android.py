#!/usr/bin/env python3
#
# Copyright (C) 2019 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""This module contains utilities to extract Android build system variables."""

import os.path


def find_android_build_top(path):
    """This function finds ANDROID_BUILD_TOP by searching parent directories of
    `path`."""
    path = os.path.dirname(os.path.abspath(path))
    prev = None
    while prev != path:
        if os.path.exists(os.path.join(path, '.repo', 'manifest.xml')):
            return path
        prev = path
        path = os.path.dirname(path)
    raise ValueError('failed to find ANDROID_BUILD_TOP')
