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

"""This module contains the unit tests to check the AST classes."""

import unittest

from blueprint import Bool, Concat, Dict, Expr, Integer, List, String, VarRef


#------------------------------------------------------------------------------
# Expr
#------------------------------------------------------------------------------

class ExprTest(unittest.TestCase):
    """Unit tests for the Expr class."""

    def test_eval(self):
        """Test whether Expr.eval() raises NotImplementedError."""

        with self.assertRaises(NotImplementedError):
            Expr().eval({})


#------------------------------------------------------------------------------
# Bool
#------------------------------------------------------------------------------

class BoolTest(unittest.TestCase):
    """Unit tests for the Bool class."""

    def test_bool(self):
        """Test Bool.__init__(), Bool.__bool__(), and Bool.eval() methods."""

        false_expr = Bool(False)
        self.assertFalse(bool(false_expr))
        self.assertFalse(false_expr.eval({}))

        true_expr = Bool(True)
        self.assertTrue(bool(true_expr))
        self.assertTrue(true_expr.eval({}))

        self.assertEqual(Bool(False), false_expr)
        self.assertEqual(Bool(True), true_expr)


    def test_equal(self):
        """Test Bool.__eq__() method."""

        false_expr1 = Bool(False)
        false_expr2 = Bool(False)
        true_expr1 = Bool(True)
        true_expr2 = Bool(True)

        self.assertIsNot(false_expr1, false_expr2)
        self.assertEqual(false_expr1, false_expr2)

        self.assertIsNot(true_expr1, true_expr2)
        self.assertEqual(true_expr1, true_expr2)


    def test_hash(self):
        """Test Bool.__hash__() method."""

        false_expr = Bool(False)
        true_expr = Bool(True)

        self.assertEqual(hash(Bool(False)), hash(false_expr))
        self.assertEqual(hash(Bool(True)), hash(true_expr))


    def test_repr(self):
        """Test Bool.__repr__() method."""

        self.assertEqual('False', repr(Bool(False)))
        self.assertEqual('True', repr(Bool(True)))


#------------------------------------------------------------------------------
# Integer
#------------------------------------------------------------------------------

class IntegerTest(unittest.TestCase):
    """Unit tests for the Integer class."""

    def test_int(self):
        """Test Integer.__init__(), Integer.__bool__(), Integer.__int__(), and
        Integer.eval() methods."""

        expr = Integer(0)
        self.assertFalse(bool(expr))
        self.assertEqual(0, int(expr))
        self.assertEqual(0, int(expr.eval({})))

        expr = Integer(1)
        self.assertTrue(bool(expr))
        self.assertEqual(1, int(expr))
        self.assertEqual(1, int(expr.eval({})))

        expr = Integer(2)
        self.assertTrue(bool(expr))
        self.assertEqual(2, int(expr))
        self.assertEqual(2, int(expr.eval({})))


    def test_equal(self):
        """Test Integer.__eq__() method."""

        expr_zero1 = Integer(0)
        expr_zero2 = Integer(0)
        expr_one1 = Integer(1)
        expr_one2 = Integer(1)

        self.assertIsNot(expr_zero1, expr_zero2)
        self.assertEqual(expr_zero1, expr_zero2)

        self.assertIsNot(expr_one1, expr_one2)
        self.assertEqual(expr_one1, expr_one2)


    def test_hash(self):
        """Test Integer.__hash__() method."""

        expr_zero = Integer(0)
        expr_one = Integer(1)

        self.assertEqual(hash(Integer(False)), hash(expr_zero))
        self.assertEqual(hash(Integer(True)), hash(expr_one))


    def test_repr(self):
        """Test Integer.__repr__() method."""

        self.assertEqual('0', repr(Integer(0)))
        self.assertEqual('1', repr(Integer(1)))


#------------------------------------------------------------------------------
# String
#------------------------------------------------------------------------------

class StringTest(unittest.TestCase):
    """Unit tests for the String class."""

    def test_string(self):
        """Test String.__init__() and String.eval() methods."""

        expr = String('test')
        self.assertEqual('test', expr.eval({}))


#------------------------------------------------------------------------------
# VarRef
#------------------------------------------------------------------------------

class VarRefTest(unittest.TestCase):
    """Unit tests for the VarRef class."""

    def test_var_ref(self):
        """Test VarRef.__init__() and VarRef.eval() methods."""

        expr = VarRef('a', String('b'))
        self.assertEqual('a', expr.name)
        self.assertEqual(String('b'), expr.value)
        self.assertEqual('b', expr.eval({}))


    def test_eval_with_value(self):
        """Test evaluation of local variables."""
        expr = VarRef('a', String('1'))
        self.assertEqual('1', expr.eval({'a': String('2')}))


    def test_eval_without_value(self):
        """Test evaluation of external variables."""
        expr = VarRef('a', None)
        self.assertEqual('2', expr.eval({'a': String('2')}))


    def test_eval_recursive(self):
        """Test recursive evaluation."""
        expr = VarRef('a', List([VarRef('x', None), VarRef('y', None)]))
        expr_eval = expr.eval({'x': String('1'), 'y': String('2')})
        self.assertIsInstance(expr_eval, List)
        self.assertEqual('1', expr_eval[0])
        self.assertEqual('2', expr_eval[1])


#------------------------------------------------------------------------------
# List
#------------------------------------------------------------------------------

class ListTest(unittest.TestCase):
    """Unit tests for the List class."""

    def test_list(self):
        """Test List.__init__() and List.eval() methods."""

        expr = List([String('a'), String('b')])
        self.assertEqual(String('a'), expr[0])
        self.assertEqual(String('b'), expr[1])

        expr = List([VarRef('a', None), VarRef('b', None)])
        expr_eval = expr.eval({'a': String('1'), 'b': String('2')})
        self.assertEqual('1', expr_eval[0])
        self.assertEqual('2', expr_eval[1])


#------------------------------------------------------------------------------
# Concatenation
#------------------------------------------------------------------------------

class ConcatTest(unittest.TestCase):
    """Unit tests for the Concat class."""

    def test_concat_list(self):
        """Test Concat.__init__() and Concat.eval() methods for List."""

        lhs = List([String('a'), String('b')])
        rhs = List([String('c'), String('d')])
        expr = Concat(lhs, rhs)

        self.assertIs(expr.lhs, lhs)
        self.assertIs(expr.rhs, rhs)

        expr_eval = expr.eval({})
        self.assertIsInstance(expr_eval, List)
        self.assertEqual('a', expr_eval[0])
        self.assertEqual('b', expr_eval[1])
        self.assertEqual('c', expr_eval[2])
        self.assertEqual('d', expr_eval[3])


    def test_concat_string(self):
        """Test Concat.__init__() and Concat.eval() methods for String."""

        lhs = String('a')
        rhs = String('b')
        expr = Concat(lhs, rhs)

        self.assertIs(expr.lhs, lhs)
        self.assertIs(expr.rhs, rhs)

        expr_eval = expr.eval({})
        self.assertIsInstance(expr_eval, String)
        self.assertEqual('ab', expr_eval)


    def test_type_error(self):
        """Test the type check in eval()."""

        str_obj = String('a')
        list_obj = List()

        with self.assertRaises(TypeError):
            Concat(str_obj, list_obj).eval({})

        with self.assertRaises(TypeError):
            Concat(list_obj, str_obj).eval({})


#------------------------------------------------------------------------------
# Dictionary
#------------------------------------------------------------------------------

class DictTest(unittest.TestCase):
    """Unit tests for the Dict class."""

    def test_dict(self):
        """Test Dict.__init__() method."""

        expr = Dict([('a', String('1')), ('b', Bool(True))])

        self.assertIn('a', expr)
        self.assertEqual(String('1'), expr['a'])

        self.assertIn('b', expr)
        self.assertEqual(Bool(True), expr['b'])


    def test_eval(self):
        """Test Dict.eval() method."""

        expr = Dict([('a', VarRef('a', None)), ('b', VarRef('b', None))])
        expr_eval = expr.eval({'a': String('1'), 'b': String('2')})

        self.assertIn('a', expr_eval)
        self.assertEqual('1', expr_eval['a'])

        self.assertIn('b', expr_eval)
        self.assertEqual('2', expr_eval['b'])


if __name__ == '__main__':
    unittest.main()
