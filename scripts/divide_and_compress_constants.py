#!/usr/bin/python2.4
#
# Copyright (C) 2008 Google Inc.
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

"""Constants for the divide_and_compress script and DirectoryZipper class."""

__author__ = 'jmatt@google.com (Justin Mattson)'

file_preamble = """#!/usr/bin/env python
#
# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import wsgiref.handlers
from google.appengine.ext import zipserve
from google.appengine.ext import webapp
import memcache_zipserve

class MainHandler(webapp.RequestHandler):

  def get(self):
    self.response.out.write('Hello world!')

def main():
  handler = memcache_zipserve.create_handler(["""

file_endpiece = """
    ])
  application = webapp.WSGIApplication([('/(.*)', handler)], debug=False)
  wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
    main()
"""