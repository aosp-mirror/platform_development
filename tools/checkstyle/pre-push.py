#!/usr/bin/python

#
# Copyright 2016, The Android Open Source Project
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
#

"""Pre-push hook to run Checkstyle on changes that are about to be uploaded.

  Usage: add a symbolic link from /path/to/git/repo/.git/hooks/pre-push to
  this file.
"""

import sys
import checkstyle


def main():
  print '\nStarting Checkstyle!\n'
  sys.stdout.flush()

  ins = raw_input()
  (errors, warnings) = checkstyle.RunCheckstyleOnACommit(ins.split(' ')[1])

  if errors or warnings:
    print 'Upload anyway (y/N)?:'
    sys.stdin = open('/dev/tty')
    sys.stdout.flush()
    answer = raw_input()
    if 'y' == answer.lower():
      sys.exit(0)
    else:
      sys.exit(1)


if __name__ == '__main__':
  main()
