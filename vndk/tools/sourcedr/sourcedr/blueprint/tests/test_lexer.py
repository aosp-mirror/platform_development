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

"""This module contains the unit tests to check the Lexer class."""

import sys
import unittest

from blueprint import Lexer, LexerError, Token


#------------------------------------------------------------------------------
# Python 2 compatibility
#------------------------------------------------------------------------------

if sys.version_info >= (3,):
    py3_str = str  # pylint: disable=invalid-name
else:
    def py3_str(string):
        """Convert a string into a utf-8 encoded string."""
        return unicode(string).encode('utf-8')


#------------------------------------------------------------------------------
# LexerError
#------------------------------------------------------------------------------

class LexerErrorTest(unittest.TestCase):
    """Unit tests for LexerError class."""

    def test_lexer_error(self):
        """Test LexerError __init__(), __str__(), line, column, and message."""

        exc = LexerError('a %', 2, 'unexpected character')
        self.assertEqual(exc.line, 1)
        self.assertEqual(exc.column, 3)
        self.assertEqual(exc.message, 'unexpected character')
        self.assertEqual(str(exc), 'LexerError: 1:3: unexpected character')

        exc = LexerError('a\nb\ncde %', 8, 'unexpected character')
        self.assertEqual(exc.line, 3)
        self.assertEqual(exc.column, 5)
        self.assertEqual(exc.message, 'unexpected character')
        self.assertEqual(str(exc), 'LexerError: 3:5: unexpected character')


    def test_hierarchy(self):
        """Test the hierarchy of LexerError."""
        with self.assertRaises(ValueError):
            raise LexerError('a', 0, 'error')


class LexComputeLineColumn(unittest.TestCase):
    """Unit tests for Lexer.compute_line_column() method."""

    def test_compute_line_column(self):
        """Test the line and column computation."""

        # Line 1
        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 0)
        self.assertEqual(line, 1)
        self.assertEqual(column, 1)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 1)
        self.assertEqual(line, 1)
        self.assertEqual(column, 2)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 2)
        self.assertEqual(line, 1)
        self.assertEqual(column, 3)

        # Line 2
        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 3)
        self.assertEqual(line, 2)
        self.assertEqual(column, 1)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 4)
        self.assertEqual(line, 2)
        self.assertEqual(column, 2)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 5)
        self.assertEqual(line, 2)
        self.assertEqual(column, 3)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 6)
        self.assertEqual(line, 2)
        self.assertEqual(column, 4)

        # Line 3
        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 7)
        self.assertEqual(line, 3)
        self.assertEqual(column, 1)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 8)
        self.assertEqual(line, 3)
        self.assertEqual(column, 2)

        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 9)
        self.assertEqual(line, 3)
        self.assertEqual(column, 3)

        # Line 4 (empty line)
        line, column = Lexer.compute_line_column('ab\ncde\nfg\n', 10)
        self.assertEqual(line, 4)
        self.assertEqual(column, 1)


#------------------------------------------------------------------------------
# Lex.lex_string()
#------------------------------------------------------------------------------

class LexStringTest(unittest.TestCase):
    """Unit tests for the Lexer.lex_string() method."""

    def test_raw_string_lit(self):
        """Test whether Lexer.lex_string() can tokenize raw string literal."""

        end, lit = Lexer.lex_string('`a`', 0)
        self.assertEqual(end, 3)
        self.assertEqual(lit, 'a')

        end, lit = Lexer.lex_string('`a\nb`', 0)
        self.assertEqual(end, 5)
        self.assertEqual(lit, 'a\nb')

        end, lit = Lexer.lex_string('"a""b"', 3)
        self.assertEqual(end, 6)
        self.assertEqual(lit, 'b')

        with self.assertRaises(LexerError) as ctx:
            Lexer.lex_string('`a', 0)
        self.assertEqual(ctx.exception.line, 1)
        self.assertEqual(ctx.exception.column, 3)

        with self.assertRaises(LexerError) as ctx:
            Lexer.lex_string('"a\nb"', 0)
        self.assertEqual(ctx.exception.line, 1)
        self.assertEqual(ctx.exception.column, 3)


    def test_interpreted_string_literal(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal."""

        end, lit = Lexer.lex_string('"a"', 0)
        self.assertEqual(end, 3)
        self.assertEqual(lit, 'a')

        end, lit = Lexer.lex_string('"n"', 0)
        self.assertEqual(end, 3)
        self.assertEqual(lit, 'n')

        with self.assertRaises(LexerError) as ctx:
            Lexer.lex_string('"\\', 0)
        self.assertEqual(ctx.exception.line, 1)
        self.assertEqual(ctx.exception.column, 2)


    def test_literal_escape_char(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal with a escaped character."""

        end, lit = Lexer.lex_string('"\\a"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\a')

        end, lit = Lexer.lex_string('"\\b"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\b')

        end, lit = Lexer.lex_string('"\\f"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\f')

        end, lit = Lexer.lex_string('"\\n"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\n')

        end, lit = Lexer.lex_string('"\\r"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\r')

        end, lit = Lexer.lex_string('"\\t"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\t')

        end, lit = Lexer.lex_string('"\\v"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\v')

        end, lit = Lexer.lex_string('"\\\\"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\\')

        end, lit = Lexer.lex_string('"\\\'"', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\'')

        end, lit = Lexer.lex_string('"\\\""', 0)
        self.assertEqual(end, 4)
        self.assertEqual(lit, '\"')

        with self.assertRaises(LexerError) as ctx:
            Lexer.lex_string('"\\?"', 0)
        self.assertEqual(ctx.exception.line, 1)
        self.assertEqual(ctx.exception.column, 2)


    def test_literal_escape_octal(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal with an octal escape sequence."""

        end, lit = Lexer.lex_string('"\\000"', 0)
        self.assertEqual(end, 6)
        self.assertEqual(lit, '\0')

        end, lit = Lexer.lex_string('"\\377"', 0)
        self.assertEqual(end, 6)
        self.assertEqual(lit, '\377')

        tests = [
            '"\\0',
            '"\\0"  ',
            '"\\09" ',
            '"\\009"',
        ]

        for test in tests:
            with self.assertRaises(LexerError) as ctx:
                Lexer.lex_string(test, 0)
            self.assertEqual(ctx.exception.line, 1)
            self.assertEqual(ctx.exception.column, 2)


    def test_literal_escape_hex(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal with a hexadecimal escape sequence."""

        end, lit = Lexer.lex_string('"\\x00"', 0)
        self.assertEqual(end, 6)
        self.assertEqual(lit, '\0')

        end, lit = Lexer.lex_string('"\\xff"', 0)
        self.assertEqual(end, 6)
        self.assertEqual(lit, '\xff')

        tests = [
            '"\\x',
            '"\\x"  ',
            '"\\x0" ',
            '"\\xg" ',
            '"\\x0g"',
        ]

        for test in tests:
            with self.assertRaises(LexerError) as ctx:
                Lexer.lex_string(test, 0)
            self.assertEqual(ctx.exception.line, 1)
            self.assertEqual(ctx.exception.column, 2)


    def test_literal_escape_little_u(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal with a little u escape sequence."""

        end, lit = Lexer.lex_string('"\\u0000"', 0)
        self.assertEqual(end, 8)
        self.assertEqual(lit, '\0')

        end, lit = Lexer.lex_string('"\\uffff"', 0)
        self.assertEqual(end, 8)
        self.assertEqual(lit, py3_str(u'\uffff'))

        tests = [
            '"\\u',
            '"\\u"    ',
            '"\\u0"   ',
            '"\\ug"   ',
            '"\\u0g"  ',
            '"\\u00g" ',
            '"\\u000g"',
        ]

        for test in tests:
            with self.assertRaises(LexerError) as ctx:
                Lexer.lex_string(test, 0)
            self.assertEqual(ctx.exception.line, 1)
            self.assertEqual(ctx.exception.column, 2)


    def test_literal_escape_big_u(self):
        """Test whether Lexer.lex_string() can tokenize interpreted string
        literal with a big u escape sequence."""

        end, lit = Lexer.lex_string('"\\U00000000"', 0)
        self.assertEqual(end, 12)
        self.assertEqual(lit, '\0')

        end, lit = Lexer.lex_string('"\\U0001ffff"', 0)
        self.assertEqual(end, 12)
        self.assertEqual(lit, py3_str(u'\U0001ffff'))

        tests = [
            '"\\U',
            '"\\U"        ',
            '"\\U0"       ',
            '"\\Ug"       ',
            '"\\U0g"      ',
            '"\\U00g"     ',
            '"\\U000g"    ',
            '"\\U000g"    ',
            '"\\U0000g"   ',
            '"\\U00000g"  ',
            '"\\U000000g" ',
            '"\\U0000000g"',
        ]

        for test in tests:
            with self.assertRaises(LexerError) as ctx:
                Lexer.lex_string(test, 0)
            self.assertEqual(ctx.exception.line, 1)
            self.assertEqual(ctx.exception.column, 2)


#------------------------------------------------------------------------------
# Lexer.lex()
#------------------------------------------------------------------------------

class LexTest(unittest.TestCase):
    """Unit tests for the Lexer.lex() method."""

    def test_lex_char(self):
        """Test whether Lexer.lex() can lex a character."""

        token, end, lit = Lexer.lex('(', 0)
        self.assertEqual(token, Token.LPAREN)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex(')', 0)
        self.assertEqual(token, Token.RPAREN)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('[', 0)
        self.assertEqual(token, Token.LBRACKET)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex(']', 0)
        self.assertEqual(token, Token.RBRACKET)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('{', 0)
        self.assertEqual(token, Token.LBRACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('}', 0)
        self.assertEqual(token, Token.RBRACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex(':', 0)
        self.assertEqual(token, Token.COLON)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('=', 0)
        self.assertEqual(token, Token.ASSIGN)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('+', 0)
        self.assertEqual(token, Token.PLUS)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex(',', 0)
        self.assertEqual(token, Token.COMMA)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)


    def test_lex_assign_plus(self):
        """Test whether Lexer.lex() can lex `+=` without problems."""

        token, end, lit = Lexer.lex('+=', 0)
        self.assertEqual(token, Token.ASSIGNPLUS)
        self.assertEqual(end, 2)
        self.assertEqual(lit, None)


    def test_lex_space(self):
        """Test whether Lexer.lex() can lex whitespaces."""

        token, end, lit = Lexer.lex(' ', 0)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('\t', 0)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('\r', 0)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('\n', 0)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 1)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('\n \r\t\n', 0)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 5)
        self.assertEqual(lit, None)


    def test_lex_comment(self):
        """Test whether Lexer.lex() can lex comments."""

        token, end, lit = Lexer.lex('// abcd', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 7)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('// abcd\nnext', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 7)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a\nb*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 7)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a\n *b*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 9)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a**b*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 8)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a***b*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 9)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/**/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 4)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/***/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 5)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/**a*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 6)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a**/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 6)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/***a*/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 7)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('/*a***/', 0)
        self.assertEqual(token, Token.COMMENT)
        self.assertEqual(end, 7)
        self.assertEqual(lit, None)


    def test_lex_string(self):
        """Test whether Lexer.lex() can lex a string."""

        token, end, lit = Lexer.lex('"a"', 0)
        self.assertEqual(token, Token.STRING)
        self.assertEqual(end, 3)
        self.assertEqual(lit, 'a')

        token, end, lit = Lexer.lex('`a\nb`', 0)
        self.assertEqual(token, Token.STRING)
        self.assertEqual(end, 5)
        self.assertEqual(lit, 'a\nb')


    def test_lex_ident(self):
        """Test whether Lexer.lex() can lex an identifier."""

        token, end, lit = Lexer.lex('ident', 0)
        self.assertEqual(token, Token.IDENT)
        self.assertEqual(end, 5)
        self.assertEqual(lit, 'ident')


    def test_lex_offset(self):
        """Test the offset argument of Lexer.lex()."""

        token, end, lit = Lexer.lex('a "b"', 0)
        self.assertEqual(token, Token.IDENT)
        self.assertEqual(end, 1)
        self.assertEqual(lit, 'a')

        token, end, lit = Lexer.lex('a "b"', end)
        self.assertEqual(token, Token.SPACE)
        self.assertEqual(end, 2)
        self.assertEqual(lit, None)

        token, end, lit = Lexer.lex('a "b"', end)
        self.assertEqual(token, Token.STRING)
        self.assertEqual(end, 5)
        self.assertEqual(lit, 'b')


#------------------------------------------------------------------------------
# Lexer class test
#------------------------------------------------------------------------------

class LexerTest(unittest.TestCase):
    """Unit tests for the Lexer class."""

    def test_lexer(self):
        """Test token, start, end, literal, and consume()."""

        lexer = Lexer('a b //a\n "c"', 0)

        self.assertEqual(lexer.start, 0)
        self.assertEqual(lexer.end, 1)
        self.assertEqual(lexer.token, Token.IDENT)
        self.assertEqual(lexer.literal, 'a')
        lexer.consume(Token.IDENT)

        self.assertEqual(lexer.start, 2)
        self.assertEqual(lexer.end, 3)
        self.assertEqual(lexer.token, Token.IDENT)
        self.assertEqual(lexer.literal, 'b')
        lexer.consume(Token.IDENT)

        self.assertEqual(lexer.start, 9)
        self.assertEqual(lexer.end, 12)
        self.assertEqual(lexer.token, Token.STRING)
        self.assertEqual(lexer.literal, 'c')
        lexer.consume(Token.STRING)

        self.assertEqual(lexer.start, 12)
        self.assertEqual(lexer.end, 12)
        self.assertEqual(lexer.token, Token.EOF)
        self.assertEqual(lexer.literal, None)


    def test_lexer_offset(self):
        """Test the offset argument of Lexer.__init__()."""

        lexer = Lexer('a b', 2)

        self.assertEqual(lexer.start, 2)
        self.assertEqual(lexer.end, 3)
        self.assertEqual(lexer.token, Token.IDENT)
        self.assertEqual(lexer.literal, 'b')
        lexer.consume(Token.IDENT)

        self.assertEqual(lexer.start, 3)
        self.assertEqual(lexer.end, 3)
        self.assertEqual(lexer.token, Token.EOF)
        self.assertEqual(lexer.literal, None)
        lexer.consume(Token.EOF)


    def test_lexer_path(self):
        """Test the path attribute of the Lexer object."""
        lexer = Lexer('content', path='test_path')
        self.assertEqual(lexer.path, 'test_path')


if __name__ == '__main__':
    unittest.main()
