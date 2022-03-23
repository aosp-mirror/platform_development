#!/usr/bin/env python
#
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

"""Script to run */tests/*_unittest.py files."""

from multiprocessing import Process
import os
import runpy


def get_unittest_files():
  matches = []
  for dirpath, _, filenames in os.walk('.'):
    if os.path.basename(dirpath) == 'tests':
      matches.extend(os.path.join(dirpath, f)
                     for f in filenames if f.endswith('_unittest.py'))
  return matches


def run_test(unittest_file):
  runpy.run_path(unittest_file, run_name='__main__')


if __name__ == '__main__':
  for path in get_unittest_files():
    # Forks a process to run the unittest.
    # Otherwise, it only runs one unittest.
    p = Process(target=run_test, args=(path,))
    p.start()
    p.join()
    if p.exitcode != 0:
      break  # stops on any failure unittest
