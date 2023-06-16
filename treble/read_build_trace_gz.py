#!/usr/bin/env python3
#
# Copyright (C) 2023 The Android Open Source Project
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

import gzip
import json
import os
import sys

from collections import defaultdict

READ_DURATION = [
    'soong',
    'kati build',
    'ninja',
    'total',
]

class Trace:
    def __init__(self, trace_file):
        self.duration = dict()
        self._queue = defaultdict(list)
        self.target = os.path.splitext(os.path.basename(trace_file))[0]
        if not os.path.isfile(trace_file):
            return
        self._trace_file = gzip.open(trace_file, 'rt', encoding='utf-8')
        self._trace_data = json.load(self._trace_file)
        for t in self._trace_data:
            if 'ph' not in t:
                continue
            if t['ph'] == 'X':
                self.duration[t['name']] = t['dur']
                continue
            if t['ph'] == 'B':
                self._queue[(t['pid'], t['pid'])].append((t['name'], t['ts']))
                continue
            if t['ph'] == 'E':
                queue = self._queue[(t['pid'], t['pid'])]
                if not queue:
                    raise Exception('pid:{}, tid:{} not started'.format(t['pid'], t['pid']))
                name, ts = queue.pop()
                self.duration[name] = t['ts'] - ts
                continue

    def out_durations(self):
        out_str = self.target
        for name in READ_DURATION:
            if name not in self.duration:
                continue
            out_str = '{}, {}'.format(out_str, self.duration[name])
        out_str += '\n'
        sys.stdout.write(out_str)


def main(argv):
    trace = Trace(argv[1])
    trace.out_durations()

if __name__ == '__main__':
    main(sys.argv)
