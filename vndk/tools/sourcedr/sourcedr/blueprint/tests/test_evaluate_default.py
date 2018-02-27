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

"""This module contains the unit tests to check evaluate_default(s)."""

import unittest

from blueprint import Dict, evaluate_default, evaluate_defaults


#------------------------------------------------------------------------------
# Evaluate Default
#------------------------------------------------------------------------------

class EvaluateDefaultTest(unittest.TestCase):
    """Test cases for evaluate_default()."""

    def test_evaluate_default(self):
        """Test evaluate_default()."""

        attrs = {'a': 'specified_a', 'b': 'specified_b'}
        default_attrs = {'a': 'default_a', 'c': 'default_c'}

        result = evaluate_default(attrs, default_attrs)

        self.assertEqual(len(result), 3)
        self.assertEqual(result['a'], 'specified_a')
        self.assertEqual(result['b'], 'specified_b')
        self.assertEqual(result['c'], 'default_c')


    def test_evaluate_default_nested(self):
        """Test evaluate_default() with nested properties."""

        attrs = {'c': Dict({'a': 'specified_a'})}
        default_attrs = {'c': Dict({'a': 'default_a', 'b': 'default_b'})}

        result = evaluate_default(attrs, default_attrs)

        self.assertEqual(len(result), 1)
        self.assertEqual(len(result['c']), 2)
        self.assertEqual(result['c']['a'], 'specified_a')
        self.assertEqual(result['c']['b'], 'default_b')


#------------------------------------------------------------------------------
# Evaluate Defaults
#------------------------------------------------------------------------------

class EvaluateDefaultsTest(unittest.TestCase):
    """Test cases for evaluate_defaults()."""

    def test_evaluate_defaults(self):
        """Test evaluate_defaults()."""

        modules = [
            ('cc_defaults', {
                'name': 'libfoo-defaults',
                'a': 'default_a',
                'b': 'default_b',
            }),
            ('cc_library', {
                'name': 'libfoo',
                'defaults': ['libfoo-defaults'],
                'a': 'specified_a',
            }),
        ]

        modules = evaluate_defaults(modules)

        module = modules[-1][1]
        self.assertEqual(module['name'], 'libfoo')
        self.assertEqual(module['a'], 'specified_a')
        self.assertEqual(module['b'], 'default_b')


    def test_evaluate_two_defaults(self):
        """Test evaluate_defaults() with two defaults."""

        modules = [
            ('cc_defaults', {
                'name': 'libfoo-defaults',
                'a': 'libfoo_default_a',
                'b': 'libfoo_default_b',
            }),
            ('cc_defaults', {
                'name': 'libbar-defaults',
                'a': 'libbar_default_a',
                'b': 'libbar_default_b',
                'c': 'libbar_default_c',
            }),
            ('cc_library', {
                'name': 'libfoo',
                'defaults': ['libfoo-defaults', 'libbar-defaults'],
                'a': 'specified_a',
            }),
        ]

        modules = evaluate_defaults(modules)

        module = modules[-1][1]
        self.assertEqual(module['name'], 'libfoo')
        self.assertEqual(module['a'], 'specified_a')
        self.assertEqual(module['b'], 'libfoo_default_b')
        self.assertEqual(module['c'], 'libbar_default_c')


    def test_skip_modules_without_name(self):
        """Test whether evaluate_defaults() skips modules without names."""

        modules = [('special_rules', {})]

        try:
            modules = evaluate_defaults(modules)
        except KeyError:
            self.fail('modules without names must not cause KeyErrors')


    def test_evaluate_recursive(self):
        """Test whether evaluate_defaults() can evaluate defaults
        recursively."""

        modules = [
            ('cc_defaults', {
                'name': 'libfoo-defaults',
                'defaults': ['libtest-defaults'],
                'a': 'libfoo_default_a',
                'b': 'libfoo_default_b',
            }),
            ('cc_defaults', {
                'name': 'libbar-defaults',
                'a': 'libbar_default_a',
                'b': 'libbar_default_b',
                'c': 'libbar_default_c',
                'd': 'libbar_default_d',
            }),
            ('cc_defaults', {
                'name': 'libtest-defaults',
                'a': 'libtest_default_a',
                'b': 'libtest_default_b',
                'c': 'libtest_default_c',
                'e': 'libtest_default_e',
            }),
            ('cc_library', {
                'name': 'libfoo',
                'defaults': ['libfoo-defaults', 'libbar-defaults'],
                'a': 'specified_a',
            }),
        ]

        modules = evaluate_defaults(modules)

        module = modules[-1][1]
        self.assertEqual(module['name'], 'libfoo')
        self.assertEqual(module['a'], 'specified_a')
        self.assertEqual(module['b'], 'libfoo_default_b')
        self.assertEqual(module['c'], 'libtest_default_c')
        self.assertEqual(module['d'], 'libbar_default_d')
        self.assertEqual(module['e'], 'libtest_default_e')


    def test_evaluate_recursive_diamond(self):
        """Test whether evaluate_defaults() can evaluate diamond defaults
        recursively."""

        modules = [
            ('cc_defaults', {
                'name': 'libfoo-defaults',
                'defaults': ['libtest-defaults'],
                'a': 'libfoo_default_a',
                'b': 'libfoo_default_b',
            }),
            ('cc_defaults', {
                'name': 'libbar-defaults',
                'defaults': ['libtest-defaults'],
                'a': 'libbar_default_a',
                'b': 'libbar_default_b',
                'c': 'libbar_default_c',
                'd': 'libbar_default_d',
            }),
            ('cc_defaults', {
                'name': 'libtest-defaults',
                'a': 'libtest_default_a',
                'b': 'libtest_default_b',
                'c': 'libtest_default_c',
                'e': 'libtest_default_e',
            }),
            ('cc_library', {
                'name': 'libfoo',
                'defaults': ['libfoo-defaults', 'libbar-defaults'],
                'a': 'specified_a',
            }),
        ]

        modules = evaluate_defaults(modules)

        module = modules[-1][1]
        self.assertEqual(module['name'], 'libfoo')
        self.assertEqual(module['a'], 'specified_a')
        self.assertEqual(module['b'], 'libfoo_default_b')
        self.assertEqual(module['c'], 'libtest_default_c')
        self.assertEqual(module['d'], 'libbar_default_d')
        self.assertEqual(module['e'], 'libtest_default_e')


if __name__ == '__main__':
    unittest.main()
