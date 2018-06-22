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
#

"""This module contains the unit tests to whether module paths are kept
properly."""

import os
import unittest

from blueprint import Lexer, Parser, RecursiveParser


#------------------------------------------------------------------------------
# Module Path
#------------------------------------------------------------------------------

class ModulePathTest(unittest.TestCase):
    """Test cases for module path attribute."""

    def test_module_path_from_lexer(self):
        """Test whether the path are passed from Lexer to parsed modules."""
        content = '''
            cc_library {
                name: "libfoo",
            }
            '''

        parser = Parser(Lexer(content, path='test_path'))
        parser.parse()

        self.assertEqual('test_path', parser.modules[0][1]['_path'])


    def test_module_path_functional(self):
        SUBNAME = 'MockBuild.txt'

        test_dir = os.path.join(
                os.path.dirname(__file__), 'testdata', 'example')
        test_root_file = os.path.join(test_dir, SUBNAME)

        parser = RecursiveParser()
        parser.parse_file(test_root_file, default_sub_name=SUBNAME)

        named_mods = {module[1]['name']: module for module in parser.modules}

        self.assertEqual(os.path.join(test_dir, 'foo', SUBNAME),
                         named_mods['libfoo'][1]['_path'])
        self.assertEqual(os.path.join(test_dir, 'bar', SUBNAME),
                         named_mods['libbar'][1]['_path'])

if __name__ == '__main__':
    unittest.main()
