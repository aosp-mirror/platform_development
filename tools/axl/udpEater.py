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

"""
  udpEater.py: receives UDP traffic

"""

import time, socket, string

def main():
    port = 9001

    svrsocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    svrsocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    svrsocket.bind(('', port))

    hostname = socket.gethostname()
    ip = socket.gethostbyname(hostname)
    print 'Server is at IP adress: ', ip
    print 'Listening for requests on port %s ...' % port

    count = 0
    while count < 400:
        data, address = svrsocket.recvfrom(8192)
        print 'Received packet', count, data[:34]
        count += 1

if __name__ == "__main__":
    main()
