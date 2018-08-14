#!/usr/bin/env python3
#
# Copyright (C) 2018 The Android Open Source Project
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

"""
battery_simulator.py is an interactive shell that modifies a device's battery
status.

$ battery_simulator.py
...> 60   # Sets battery level to 60%.
...> on   # Plug in charger.
...> 70   # Sets battery level to 70%.
...> off  # Plug out charger.
...> q    #quit

"""

import atexit
import os
import re
import sys

def echo_run(command):
    print("\x1b[36m[Running: %s]\x1b[0m" % command)
    os.system(command)

def battery_unplug():
    echo_run("adb shell dumpsys battery unplug")

@atexit.register
def battery_reset():
    echo_run("adb shell dumpsys battery reset")

def interactive_start():
    while True:
        try:
            val = input("Type NUMBER, 'on', 'off' or 'q' > ").lower()
        except EOFError:
            print()
            break
        if val == 'q':
            break
        if val == "on":
            echo_run("adb shell dumpsys battery set ac 1")
            continue
        if val == "off":
            echo_run("adb shell dumpsys battery set ac 0")
            continue
        if re.match("\d+", val):
            echo_run("adb shell dumpsys battery set level %s" % val)
            continue
        print("Unknown command.")


def main():
    battery_unplug()
    interactive_start()

if __name__ == '__main__':
    main()
