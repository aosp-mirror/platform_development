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

import sys

from read_build_trace_gz import Trace




def compare(target, target_name, ref, ref_name, ignore_text=None):
    """Compares which modules are built additionally from the target compared to
    the ref trace. It returns the additional modules sorted by the build time
    duration.

    Args:
      target: Trace class, The build.trace.gz information of the target build.
              It has duration dictionary of {module_name:duration}.
      target_name: str, target name of the target build.
      ref: Trace class, The build.trace.gz information of the reference build.
      ref_name: str, target name of the reference build.
    Returns:
      list of (module, duration) pairs, sum of all durations
    """
    additional_modules = dict()
    additional_time = 0

    # Trace.duration is a dictionary of module_name:duration.
    for mod in target.duration.keys():
        if ignore_text and ignore_text in mod:
            continue
        if mod.replace(target_name, ref_name) not in ref.duration:
            additional_modules[mod] = target.duration[mod]
            additional_time += target.duration[mod]

    return (sorted(additional_modules.items(), key=lambda x:x[1], reverse=True),
            additional_time)

def usec_to_min(usec):
    min = usec // 60000000
    sec = usec % 60000000 // 1000000
    msec = usec % 1000000 // 1000
    return (min, sec, msec)


def main(argv):
    # args: target_build.trace.gz target_name
    #       ref_build.trace.gz ref_name
    #       (ignore_text)
    ignore_text = None
    if len(argv) == 6:
        ignore_text = argv[5]
    elif len(argv) < 5 or len(argv) > 6:
        print("usage: compare_build_trace.py target_build.trace.gz target_name")
        print("                              ref_build.trace.gz ref_name")
        print("                              [ignore_text]")
        sys.exit(1)

    additional_modules, additional_time = compare(Trace(argv[1]), argv[2],
                                                  Trace(argv[3]), argv[4],
                                                  ignore_text)
    for module, duration in additional_modules:
        min, sec, msec = usec_to_min(duration)
        print('{min}m {sec}s {msec}ms: {name}'.format(
            min=min, sec=sec, msec=msec, name=module))
    min, sec, msec = usec_to_min(additional_time)
    print('Total: {min}m {sec}s {msec}ms'.format(
        min=min, sec=sec, msec=msec))

if __name__ == '__main__':
    main(sys.argv)