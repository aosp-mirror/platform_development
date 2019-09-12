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

"""This module contains utilities to read ELF files."""

import re
import subprocess


_ELF_CLASS = re.compile(
    '\\s*Class:\\s*(.*)$')
_DT_NEEDED = re.compile(
    '\\s*0x[0-9a-fA-F]+\\s+\\(NEEDED\\)\\s+Shared library: \\[(.*)\\]$')
_DT_SONAME = re.compile(
    '\\s*0x[0-9a-fA-F]+\\s+\\(SONAME\\)\\s+Library soname: \\[(.*)\\]$')


def readobj(path):
    """Read ELF bitness, DT_SONAME, and DT_NEEDED."""

    # Read ELF class (32-bit / 64-bit)
    proc = subprocess.Popen(['readelf', '-h', path], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)
    stdout = proc.communicate()[0]
    is_32bit = False
    for line in stdout.decode('utf-8').splitlines():
        match = _ELF_CLASS.match(line)
        if match:
            if match.group(1) == 'ELF32':
                is_32bit = True

    # Read DT_SONAME and DT_NEEDED
    proc = subprocess.Popen(['readelf', '-d', path], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE)
    stdout = proc.communicate()[0]
    dt_soname = None
    dt_needed = set()
    for line in stdout.decode('utf-8').splitlines():
        match = _DT_NEEDED.match(line)
        if match:
            dt_needed.add(match.group(1))
            continue
        match = _DT_SONAME.match(line)
        if match:
            dt_soname = match.group(1)
            continue

    return is_32bit, dt_soname, dt_needed
