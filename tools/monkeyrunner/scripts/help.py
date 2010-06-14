#!/usr/bin/env monkeyrunner
# Copyright 2010, The Android Open Source Project
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
from com.android.monkeyrunner import MonkeyRunner as mr

import os
import sys

supported_formats = ['html', 'text']

if len(sys.argv) != 3:
  print 'help.py: format output'
  sys.exit(1)

(format, saveto_path) = sys.argv[1:]

if not format.lower() in supported_formats:
  print 'format %s is not a supported format' % format
  sys.exit(2)

output = mr.help(format=format)
if not output:
  print 'Error generating help format'
  sys.exit(3)

dirname = os.path.dirname(saveto_path)
try:
    os.makedirs(dirname)
except:
    print 'oops'
    pass # It already existed

fp = open(saveto_path, 'w')
fp.write(output)
fp.close()

sys.exit(0)
