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

file_preamble = ('#!/usr/bin/env python\n'
                 '#\n'
                 '# Copyright 2008 Google Inc.\n'
                 '#\n'                                                       
                 '# Licensed under the Apache License, Version 2.0 (the' 
                 '\"License");\n'                               
                 '# you may not use this file except in compliance with the '
                 'License.\n'                                                 
                 '# You may obtain a copy of the License at\n'           
                 '#\n'
                 '#     http://www.apache.org/licenses/LICENSE-2.0\n'
                 '#\n'
                 '# Unless required by applicable law or agreed to in writing,'
                 ' software\n'                                              
                 '# distributed under the License is distributed on an \"AS' 
                 'IS\" BASIS,\n'
                 '# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either '
                 'express or implied.\n'
                 '# See the License for the specific language governing' 
                 ' permissions and\n'
                 '# limitations under the License.\n'
                 '#\n\n'
                 'import wsgiref.handlers\n'
                 'from google.appengine.ext import zipserve\n'
                 'from google.appengine.ext import webapp\n'
                 'import memcache_zipserve\n\n\n'
                 'class MainHandler(webapp.RequestHandler):\n\n'
                 '  def get(self):\n'
                 '    self.response.out.write(\'Hello world!\')\n\n'
                 'def main():\n'
                 '  application = webapp.WSGIApplication([(\'/(.*)\','
                 ' memcache_zipserve.create_handler([')

file_endpiece = ('])),\n'
                 '],\n'
                 'debug=False)\n'
                 '  wsgiref.handlers.CGIHandler().run(application)\n\n'
                 'if __name__ == \'__main__\':\n'
                 '  main()')
