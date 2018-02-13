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

"""This module contains the unit tests to check the Parser class."""

import unittest

from blueprint import Lexer, Parser, String, VarRef


#------------------------------------------------------------------------------
# Variable Definition
#------------------------------------------------------------------------------

class DefineVarTest(unittest.TestCase):
    def test_define_var(self):
        parser = Parser(None)

        str1 = String(1)
        parser.define_var('a', str1)
        self.assertEqual(len(parser.var_defs), 1)
        self.assertEqual(len(parser.vars), 1)
        self.assertIn('a', parser.vars)
        self.assertIs(parser.vars['a'], str1)

        str2 = String(2)
        parser.define_var('a', str2)
        self.assertEqual(len(parser.var_defs), 2)
        self.assertEqual(len(parser.vars), 1)
        self.assertIn('a', parser.vars)
        self.assertIs(parser.vars['a'], str2)


    def test_create_var_ref(self):
        parser = Parser(None)

        str1 = String(1)
        parser.define_var('a', str1)

        var1 = parser.create_var_ref('a')
        self.assertIsInstance(var1, VarRef)
        self.assertEqual(var1.name, 'a')
        self.assertIs(var1.value, str1)

        var2 = parser.create_var_ref('b')
        self.assertIsInstance(var2, VarRef)
        self.assertEqual(var2.name, 'b')
        self.assertIs(var2.value, None)


#------------------------------------------------------------------------------
# Parser
#------------------------------------------------------------------------------

class ParserTest(unittest.TestCase):
    def test_assign_string(self):
        lexer = Lexer('a = "example"')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr('example'))


    def test_list_empty(self):
        lexer = Lexer('a = []')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr([]))


    def test_list_one_element(self):
        lexer = Lexer('a = ["x"]')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr(['x']))


    def test_list_one_element_comma(self):
        lexer = Lexer('a = ["x",]')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr(['x']))


    def test_list_two_elements(self):
        lexer = Lexer('a = ["x", "y"]')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr(['x', 'y']))


    def test_list_two_elements_comma(self):
        lexer = Lexer('a = ["x", "y",]')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr(['x', 'y']))


    def test_dict_empty(self):
        lexer = Lexer('a = {}')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), repr({}))


    def test_dict_one_element(self):
        lexer = Lexer('a = {x: "1"}')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), '{x: \'1\'}')


    def test_dict_one_element_comma(self):
        lexer = Lexer('a = {x: "1",}')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), '{x: \'1\'}')


    def test_dict_two_elements(self):
        lexer = Lexer('a = {x: "1", y: "2"}')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), '{x: \'1\', y: \'2\'}')


    def test_dict_two_elements_comma(self):
        lexer = Lexer('a = {x: "1", y: "2",}')

        parser = Parser(lexer)
        parser.parse()
        self.assertEqual(parser.var_defs[0][0], 'a')
        self.assertEqual(repr(parser.var_defs[0][1]), '{x: \'1\', y: \'2\'}')


if __name__ == '__main__':
    unittest.main()
