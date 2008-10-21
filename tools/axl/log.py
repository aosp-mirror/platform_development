#!/usr/bin/env python

#
# Copyright 2007, The Android Open Source Project
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

import time, sys
import singletonmixin

class Log(singletonmixin.Singleton):

    def __init__(self, file):
        """_file: filename or open file"""

        if type(file) is str:
            self._file = open(file, "a")
        else:
            self._file = file

    def _getTime(self):
        tm = time.time()
        return "%s:%.2d" % (time.strftime('%m/%d/%Y %H:%M:%S',
                                          time.localtime(tm)),
                            int((tm - int(tm)) * 100))

    def _log(self, *logstrs):
        timeStr = self._getTime()
        for ln in " ".join(map(str, logstrs)).split("\n"):
            self._file.write("%s %s\n" % (timeStr, ln))
        self._file.flush()

    def debug(self, *logstrs):
        self._log("D", *logstrs)
    def info(self, *logstrs):
        self._log("I", *logstrs)
    def warn(self, *logstrs):
        self._log("W", *logstrs)
    def error(self, *logstrs):
        self._log("E", *logstrs)

    # default to info
    log = info
    __call__ = log
