# Copyright (C) 2009 The Android Open Source Project
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
#
# A nawk/gawk script used to extract the application's platform name from
# its default.properties file. It is called from build/core/add-application.mk
#

# we look for a line that looks like one of:
#    target=android-<api>
#    target=<vendor>:<name>:<api>
#
# <api> is a number, but can also be "Donut" for the first form,
# as a special case.
#
BEGIN {
    android_regex="android-[0-9A-Za-z_-]+"
    vendor_regex=":[0-9]+\\s*$"
    API=unknown
}

/^target\s*=\s*.*/ {
    if (match($0,android_regex)) {
        API=substr($0,RSTART,RLENGTH)
    }
    else if (match($0,vendor_regex)) {
        API="android-" substr($0,RSTART+1,RLENGTH)
    }
}

END {
    printf("%s", API)
}
