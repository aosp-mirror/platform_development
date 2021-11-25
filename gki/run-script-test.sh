#
# Copyright (C) 2021 The Android Open Source Project
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

# Default to Linux version 5.10
LINUX_VER=5.10

# The only other Linux version supported is 5.4.
uname -r | grep ^5.4
if [ $? -eq 0 ]; then
  LINUX_VER=5.4
fi

# Unload test kernel module
su root insmod /data/local/tmp/kmi_sym-a12-$LINUX_VER.ko
if [ $? -ne 0 ]; then
  echo "Failed to load the test kernel module!"
  su root dmesg | grep kmi_sym: | tail -21 >&2
  exit 1
fi

# Unload test kernel module
su root rmmod kmi_sym
if [ $? -ne 0 ]; then
  echo "Failed to unload the test kernel module!"
  su root dmesg | tail -21 >&2
  exit 1
fi

exit 0
