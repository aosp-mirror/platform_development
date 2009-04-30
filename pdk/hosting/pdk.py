#!/usr/bin/python2.5
#
# Copyright (C) 2008 The Android Open Source Project
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

"""Serve static pages for the pdk on appengine
"""

import os

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app


class MainPage(webapp.RequestHandler):
  def get(self):
    self.redirect('online-pdk/guide/index.html')

application = webapp.WSGIApplication([('/', MainPage)], debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
  
# Testing
# You must install google appengine.  See: http://code.google.com/appengine/downloads.html
# 
# Here's the command to run the pdk-docs server locally:
#   python <path_to_appengine_installation>/dev_appserver.py --address 0.0.0.0 \
#     <path_to_cupcake_code>/android/out/target/common/docs
    
# To verify it is working you can access it with a browser loacally on port 8080:

# http://localhost:8080/index.html


# To upload this application:
# /home/build/static/projects/apphosting/devtools/appcfg.py update pdk/
# where the pdk directory contains: pdk.py, app.yaml, and the docs directory.
# where the docs are made from the Pdk.mk file.
