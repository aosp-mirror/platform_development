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

"""This module implements a Android.bp parser."""

import collections
import glob
import itertools
import os
import re
import sys


#------------------------------------------------------------------------------
# Python 2 compatibility
#------------------------------------------------------------------------------

if sys.version_info >= (3,):
    py3_chr = chr  # pylint: disable=invalid-name
else:
    def py3_chr(codepoint):
        """Convert an integer character codepoint into a utf-8 string."""
        return unichr(codepoint).encode('utf-8')

try:
    from enum import Enum
except ImportError:
    class _Enum(object):  # pylint: disable=too-few-public-methods
        """A name-value pair for each enumeration."""

        __slot__ = ('name', 'value')


        def __init__(self, name, value):
            """Create a name-value pair."""
            self.name = name
            self.value = value


        def __repr__(self):
            """Return the name of the enumeration."""
            return self.name


    class _EnumMeta(type):  # pylint: disable=too-few-public-methods
        """Metaclass for Enum base class."""

        def __new__(mcs, name, bases, attrs):
            """Collects enumerations from attributes of the derived classes."""
            enums = []
            new_attrs = {'_enums': enums}

            for key, value in attrs.iteritems():
                if key.startswith('_'):
                    new_attrs[key] = value
                else:
                    item = _Enum(key, value)
                    enums.append(item)
                    new_attrs[key] = item

            return type.__new__(mcs, name, bases, new_attrs)


        def __iter__(cls):
            """Iterate the list of enumerations."""
            return iter(cls._enums)


    class Enum(object):  # pylint: disable=too-few-public-methods
        """Enum base class."""
        __metaclass__ = _EnumMeta


#------------------------------------------------------------------------------
# Lexer
#------------------------------------------------------------------------------

class Token(Enum):  # pylint: disable=too-few-public-methods
    """Token enumerations."""

    EOF = 0

    IDENT = 1
    LPAREN = 2
    RPAREN = 3
    LBRACKET = 4
    RBRACKET = 5
    LBRACE = 6
    RBRACE = 7
    COLON = 8
    ASSIGN = 9
    ASSIGNPLUS = 10
    PLUS = 11
    COMMA = 12
    STRING = 13
    INTEGER = 14

    COMMENT = 15
    SPACE = 16


class LexerError(ValueError):
    """Lexer error exception class."""

    def __init__(self, buf, pos, message):
        """Create a lexer error exception object."""
        super(LexerError, self).__init__(message)
        self.message = message
        self.line, self.column = Lexer.compute_line_column(buf, pos)


    def __str__(self):
        """Convert lexer error to string representation."""
        return 'LexerError: {}:{}: {}'.format(
            self.line, self.column, self.message)


class Lexer(object):
    """Lexer to tokenize the input string."""

    def __init__(self, buf, offset=0, path=None):
        """Tokenize the source code in buf starting from offset.

        Args:
            buf (string) The source code to be tokenized.
            offset (int) The position to start.
        """

        self.buf = buf

        self.start = None
        self.end = offset
        self.token = None
        self.literal = None
        self.path = path

        self._next()


    def consume(self, *tokens):
        """Consume one or more token."""

        for token in tokens:
            if token == self.token:
                self._next()
            else:
                raise LexerError(self.buf, self.start,
                                 'unexpected token ' + self.token.name)


    def _next(self):
        """Read next non-comment non-space token."""

        buf_len = len(self.buf)
        while self.end < buf_len:
            self.start = self.end
            self.token, self.end, self.literal = self.lex(self.buf, self.start)
            if self.token != Token.SPACE and self.token != Token.COMMENT:
                return

        self.start = self.end
        self.token = Token.EOF
        self.literal = None


    @staticmethod
    def compute_line_column(buf, pos):
        """Compute the line number and the column number of a given position in
        the buffer."""

        prior = buf[0:pos]
        newline_pos = prior.rfind('\n')
        if newline_pos == -1:
            return (1, pos + 1)
        return (prior.count('\n') + 1, pos - newline_pos)


    UNICODE_CHARS_PATTERN = re.compile('[^\\\\\\n"]+')


    ESCAPE_CHAR_TABLE = {
        'a': '\a', 'b': '\b', 'f': '\f', 'n': '\n', 'r': '\r', 't': '\t',
        'v': '\v', '\\': '\\', '\'': '\'', '\"': '\"',
    }


    OCT_TABLE = {str(i) for i in range(8)}


    @staticmethod
    def decode_oct(buf, offset, start, end):
        """Read characters from buf[start:end] and interpret them as an octal
        integer."""

        if end > len(buf):
            raise LexerError(buf, offset, 'bad octal escape sequence')
        try:
            codepoint = int(buf[start:end], 8)
        except ValueError:
            raise LexerError(buf, offset, 'bad octal escape sequence')
        if codepoint > 0xff:
            raise LexerError(buf, offset, 'bad octal escape sequence')
        return codepoint


    @staticmethod
    def decode_hex(buf, offset, start, end):
        """Read characters from buf[start:end] and interpret them as a
        hexadecimal integer."""

        if end > len(buf):
            raise LexerError(buf, offset, 'bad hex escape sequence')
        try:
            return int(buf[start:end], 16)
        except ValueError:
            raise LexerError(buf, offset, 'bad hex escape sequence')


    @classmethod
    def lex_interpreted_string(cls, buf, offset):
        """Tokenize a golang interpreted string.

        Args:
            buf (str)    The source code buffer.
            offset (int) The position to find a golang interpreted string
                         literal.

        Returns:
            A tuple with the end of matched buffer and the interpreted string
            literal.
        """

        buf_len = len(buf)
        pos = offset + 1
        literal = ''
        while pos < buf_len:
            # Match unicode characters
            match = cls.UNICODE_CHARS_PATTERN.match(buf, pos)
            if match:
                literal += match.group(0)
                pos = match.end()
            # Read the next character
            try:
                char = buf[pos]
            except IndexError:
                raise LexerError(buf, pos,
                                 'unclosed interpreted string literal')
            if char == '\\':
                # Escape sequences
                try:
                    char = buf[pos + 1]
                except IndexError:
                    raise LexerError(buf, pos, 'bad escape sequence')
                if char in cls.OCT_TABLE:
                    literal += chr(cls.decode_oct(buf, pos, pos + 1, pos + 4))
                    pos += 4
                elif char == 'x':
                    literal += chr(cls.decode_hex(buf, pos, pos + 2, pos + 4))
                    pos += 4
                elif char == 'u':
                    literal += py3_chr(
                        cls.decode_hex(buf, pos, pos + 2, pos + 6))
                    pos += 6
                elif char == 'U':
                    literal += py3_chr(
                        cls.decode_hex(buf, pos, pos + 2, pos + 10))
                    pos += 10
                else:
                    try:
                        literal += cls.ESCAPE_CHAR_TABLE[char]
                        pos += 2
                    except KeyError:
                        raise LexerError(buf, pos, 'bad escape sequence')
                continue
            if char == '"':
                # End of string literal
                return (pos + 1, literal)
            raise LexerError(buf, pos, 'unclosed interpreted string literal')


    @classmethod
    def lex_string(cls, buf, offset):
        """Tokenize a golang string literal.

        Args:
            buf (str)    The source code buffer.
            offset (int) The position to find a golang string literal.

        Returns:
            A tuple with the end of matched buffer and the interpreted string
            literal.
        """

        char = buf[offset]
        if char == '`':
            try:
                end = buf.index('`', offset + 1)
                return (end + 1, buf[offset + 1 : end])
            except ValueError:
                raise LexerError(buf, len(buf), 'unclosed raw string literal')
        if char == '"':
            return cls.lex_interpreted_string(buf, offset)
        raise LexerError(buf, offset, 'no string literal start character')


    LEXER_PATTERNS = (
        (Token.IDENT, '[A-Za-z_][0-9A-Za-z_]*'),
        (Token.LPAREN, '\\('),
        (Token.RPAREN, '\\)'),
        (Token.LBRACKET, '\\['),
        (Token.RBRACKET, '\\]'),
        (Token.LBRACE, '\\{'),
        (Token.RBRACE, '\\}'),
        (Token.COLON, ':'),
        (Token.ASSIGN, '='),
        (Token.ASSIGNPLUS, '\\+='),
        (Token.PLUS, '\\+'),
        (Token.COMMA, ','),
        (Token.STRING, '["`]'),
        (Token.INTEGER, '-{0,1}[0-9]+'),

        (Token.COMMENT,
         '/(?:(?:/[^\\n]*)|(?:\\*(?:(?:[^*]*)|(?:\\*+[^/*]))*\\*+/))'),
        (Token.SPACE, '\\s+'),
    )


    LEXER_MATCHER = re.compile('|'.join(
        '(' + pattern + ')' for _, pattern in LEXER_PATTERNS))


    @classmethod
    def lex(cls, buf, offset):
        """Tokenize a token from buf[offset].

        Args:
            buf (string) The source code buffer.
            offset (int) The position to find and tokenize a token.

        Return:
            A tuple with three elements.  The first element is the token id.
            The second element is the end of the token.  The third element is
            the value for strings or identifiers.
        """

        match = cls.LEXER_MATCHER.match(buf, offset)
        if not match:
            raise LexerError(buf, offset, 'unknown token')
        token = cls.LEXER_PATTERNS[match.lastindex - 1][0]

        if token == Token.STRING:
            end, literal = cls.lex_string(buf, offset)
        else:
            end = match.end()
            if token in {Token.IDENT, Token.INTEGER}:
                literal = buf[offset:end]
            else:
                literal = None

        return (token, end, literal)


#------------------------------------------------------------------------------
# AST
#------------------------------------------------------------------------------

class Expr(object):  # pylint: disable=too-few-public-methods
    """Base class for all expressions."""

    def eval(self, env):
        """Evaluate the expression under an environment."""
        raise NotImplementedError()


class String(Expr, str):
    """String constant literal."""

    def eval(self, env):
        """Evaluate the string expression under an environment."""
        return self


class Bool(Expr):  # pylint: disable=too-few-public-methods
    """Boolean constant literal."""

    __slots__ = ('value',)


    def __init__(self, value):
        """Create a boolean constant literal."""
        self.value = value


    def __repr__(self):
        """Convert a boolean constant literal to string representation."""
        return repr(self.value)


    def __bool__(self):
        """Convert boolean constant literal to Python bool type."""
        return self.value

    __nonzero__ = __bool__


    def __eq__(self, rhs):
        """Compare whether two instances are equal."""
        return self.value == rhs.value


    def __hash__(self):
        """Compute the hashed value."""
        return hash(self.value)


    def eval(self, env):
        """Evaluate the boolean expression under an environment."""
        return self


class Integer(Expr):  # pylint: disable=too-few-public-methods
    """Integer constant literal."""

    __slots__ = ('value',)


    def __init__(self, value):
        """Create an integer constant literal."""
        self.value = value


    def __repr__(self):
        """Convert an integer constant literal to string representation."""
        return repr(self.value)


    def __bool__(self):
        """Convert an integer constant literal to Python bool type."""
        return bool(self.value)

    __nonzero__ = __bool__


    def __int__(self):
        """Convert an integer constant literal to Python int type."""
        return self.value


    def __eq__(self, rhs):
        """Compare whether two instances are equal."""
        return self.value == rhs.value


    def __hash__(self):
        """Compute the hashed value."""
        return hash(self.value)


    def eval(self, env):
        """Evaluate the integer expression under an environment."""
        return self


class VarRef(Expr):  # pylint: disable=too-few-public-methods
    """A reference to a variable."""

    def __init__(self, name, value):
        """Create a variable reference with a name and the value under static
        scoping."""
        self.name = name
        self.value = value


    def __repr__(self):
        """Convert a variable reference to string representation."""
        return self.name


    def eval(self, env):
        """Evaluate the identifier under an environment."""
        if self.value is None:
            return env[self.name].eval(env)
        return self.value.eval(env)


class List(Expr, list):
    """List expression."""

    def eval(self, env):
        """Evaluate list elements under an environment."""
        return List(item.eval(env) for item in self)


class Dict(Expr, collections.OrderedDict):
    """Dictionary expression."""

    def __repr__(self):
        attrs = ', '.join(key + ': ' + repr(value)
                          for key, value in self.items())
        return '{' + attrs + '}'


    def eval(self, env):
        """Evaluate dictionary values under an environment."""
        return Dict((key, value.eval(env)) for key, value in self.items())


class Concat(Expr):  # pylint: disable=too-few-public-methods
    """List/string/integer plus operator."""

    __slots__ = ('lhs', 'rhs')


    def __init__(self, lhs, rhs):
        """Create a list/string/integer plus expression."""
        self.lhs = lhs
        self.rhs = rhs


    def __repr__(self):
        return '(' + repr(self.lhs) + ' + ' + repr(self.rhs) + ')'


    def eval(self, env):
        """Evaluate list/string/integer plus operator under an environment."""
        lhs = self.lhs.eval(env)
        rhs = self.rhs.eval(env)
        if isinstance(lhs, List) and isinstance(rhs, List):
            return List(itertools.chain(lhs, rhs))
        if isinstance(lhs, String) and isinstance(rhs, String):
            return String(lhs + rhs)
        if isinstance(lhs, Integer) and isinstance(rhs, Integer):
            return Integer(int(lhs) + int(rhs))
        raise TypeError('bad plus operands')


#------------------------------------------------------------------------------
# Parser
#------------------------------------------------------------------------------

class ParseError(ValueError):
    """Parser error exception class."""

    def __init__(self, lexer, message):
        """Create a parser error exception object."""
        super(ParseError, self).__init__(message)
        self.message = message
        self.line, self.column = \
            Lexer.compute_line_column(lexer.buf, lexer.start)


    def __str__(self):
        """Convert parser error to string representation."""
        return 'ParseError: {}:{}: {}'.format(
            self.line, self.column, self.message)


class Parser(object):
    """Parser to parse Android.bp files."""

    def __init__(self, lexer, inherited_env=None):
        """Initialize the parser with the lexer."""
        self.lexer = lexer

        self.var_defs = []
        self.vars = {} if inherited_env is None else dict(inherited_env)
        self.modules = []


    def parse(self):
        """Parse AST from tokens."""
        lexer = self.lexer
        while lexer.token != Token.EOF:
            if lexer.token == Token.IDENT:
                ident = self.parse_ident_lvalue()
                if lexer.token in {Token.ASSIGN, Token.ASSIGNPLUS}:
                    self.parse_assign(ident, lexer.token)
                elif lexer.token in {Token.LBRACE, Token.LPAREN}:
                    self.parse_module_definition(ident)
                else:
                    raise ParseError(lexer,
                                     'unexpected token ' + lexer.token.name)
            else:
                raise ParseError(lexer, 'unexpected token ' + lexer.token.name)
        lexer.consume(Token.EOF)


    def create_var_ref(self, name):
        """Create a variable reference."""
        return VarRef(name, self.vars.get(name))


    def define_var(self, name, value):
        """Define a variable."""
        self.var_defs.append((name, value))
        self.vars[name] = value


    def parse_assign(self, ident, assign_token):
        """Parse an assignment statement."""
        lexer = self.lexer
        lexer.consume(assign_token)
        value = self.parse_expression()
        if assign_token == Token.ASSIGNPLUS:
            value = Concat(self.create_var_ref(ident), value)
        self.define_var(ident, value)


    def parse_module_definition(self, module_ident):
        """Parse a module definition."""
        properties = self.parse_dict()
        properties['_path'] = String(self.lexer.path)
        self.modules.append((module_ident, properties))


    def parse_ident_lvalue(self):
        """Parse an identifier as an l-value."""
        ident = self.lexer.literal
        self.lexer.consume(Token.IDENT)
        return ident


    def parse_ident_rvalue(self):
        """Parse an identifier as a r-value.

        Returns:
            Returns VarRef if the literal is not 'true' nor 'false'.

            Returns Bool(true/false) if the literal is either 'true' or 'false'.
        """
        lexer = self.lexer
        if lexer.literal in {'true', 'false'}:
            result = Bool(lexer.literal == 'true')
        else:
            result = self.create_var_ref(lexer.literal)
        lexer.consume(Token.IDENT)
        return result


    def parse_string(self):
        """Parse a string."""
        lexer = self.lexer
        string = String(lexer.literal)
        lexer.consume(Token.STRING)
        return string


    def parse_integer(self):
        """Parse an integer."""
        lexer = self.lexer
        integer = Integer(int(lexer.literal))
        lexer.consume(Token.INTEGER)
        return integer


    def parse_operand(self):
        """Parse an operand."""
        lexer = self.lexer
        token = lexer.token
        if token == Token.STRING:
            return self.parse_string()
        if token == Token.IDENT:
            return self.parse_ident_rvalue()
        if token == Token.INTEGER:
            return self.parse_integer()
        if token == Token.LBRACKET:
            return self.parse_list()
        if token == Token.LBRACE:
            return self.parse_dict()
        if token == Token.LPAREN:
            lexer.consume(Token.LPAREN)
            operand = self.parse_expression()
            lexer.consume(Token.RPAREN)
            return operand
        raise ParseError(lexer, 'unexpected token ' + token.name)


    def parse_expression(self):
        """Parse an expression."""
        lexer = self.lexer
        expr = self.parse_operand()
        while lexer.token == Token.PLUS:
            lexer.consume(Token.PLUS)
            expr = Concat(expr, self.parse_operand())
        return expr


    def parse_list(self):
        """Parse a list."""
        result = List()
        lexer = self.lexer
        lexer.consume(Token.LBRACKET)
        while lexer.token != Token.RBRACKET:
            result.append(self.parse_expression())
            if lexer.token == Token.COMMA:
                lexer.consume(Token.COMMA)
        lexer.consume(Token.RBRACKET)
        return result


    def parse_dict(self):
        """Parse a dict."""
        result = Dict()
        lexer = self.lexer

        is_func_syntax = lexer.token == Token.LPAREN
        if is_func_syntax:
            lexer.consume(Token.LPAREN)
        else:
            lexer.consume(Token.LBRACE)

        while lexer.token != Token.RBRACE and lexer.token != Token.RPAREN:
            if lexer.token != Token.IDENT:
                raise ParseError(lexer, 'unexpected token ' + lexer.token.name)
            key = self.parse_ident_lvalue()

            if lexer.token == Token.ASSIGN:
                lexer.consume(Token.ASSIGN)
            else:
                lexer.consume(Token.COLON)

            value = self.parse_expression()
            result[key] = value

            if lexer.token == Token.COMMA:
                lexer.consume(Token.COMMA)

        if is_func_syntax:
            lexer.consume(Token.RPAREN)
        else:
            lexer.consume(Token.RBRACE)

        return result


class RecursiveParser(object):
    """This is a recursive parser which will parse blueprint files
    recursively."""


    # Default Blueprint file name
    _DEFAULT_SUB_NAME = 'Android.bp'


    def __init__(self):
        """Initialize a recursive parser."""
        self.visited = set()
        self.modules = []


    @staticmethod
    def glob_sub_files(pattern, sub_file_name):
        """List the sub file paths that match with the pattern with
        wildcards."""

        for path in glob.glob(pattern):
            if os.path.isfile(path):
                if os.path.basename(path) == sub_file_name:
                    yield path
            else:
                sub_file_path = os.path.join(path, sub_file_name)
                if os.path.isfile(sub_file_path):
                    yield sub_file_path


    @classmethod
    def find_sub_files_from_env(cls, rootdir, env, use_subdirs,
                                default_sub_name=_DEFAULT_SUB_NAME):
        """Find the sub files from the names specified in build, subdirs, and
        optional_subdirs."""

        subs = []

        if 'build' in env:
            subs.extend(os.path.join(rootdir, filename)
                        for filename in env['build'].eval(env))
        if use_subdirs:
            sub_name = env['subname'] if 'subname' in env else default_sub_name

            if 'subdirs' in env:
                for path in env['subdirs'].eval(env):
                    subs.extend(cls.glob_sub_files(os.path.join(rootdir, path),
                                                   sub_name))
            if 'optional_subdirs' in env:
                for path in env['optional_subdirs'].eval(env):
                    subs.extend(cls.glob_sub_files(os.path.join(rootdir, path),
                                                   sub_name))
        return subs


    @staticmethod
    def _read_file(path, env):
        """Read a blueprint file and return modules and the environment."""
        with open(path, 'r') as bp_file:
            content = bp_file.read()
        parser = Parser(Lexer(content, path=path), env)
        parser.parse()
        return (parser.modules, parser.vars)


    def _parse_file(self, path, env, evaluate):
        """Parse a blueprint file and append to self.modules."""
        modules, sub_env = self._read_file(path, env)
        if evaluate:
            modules = [(ident, attrs.eval(env)) for ident, attrs in modules]
        self.modules += modules
        return sub_env


    def _parse_file_recursive(self, path, env, evaluate, use_subdirs):
        """Parse a blueprint file and recursively."""

        self.visited.add(path)

        sub_env = self._parse_file(path, env, evaluate)

        rootdir = os.path.dirname(path)

        sub_file_paths = self.find_sub_files_from_env(rootdir, sub_env,
                                                      use_subdirs)

        sub_env.pop('build', None)
        sub_env.pop('subdirs', None)
        sub_env.pop('optional_subdirs', None)

        for sub_file_path in sub_file_paths:
            if sub_file_path not in self.visited:
                self._parse_file_recursive(sub_file_path, sub_env, evaluate,
                                           use_subdirs)
        return sub_env


    def _scan_and_parse_all_file_recursive(self, filename, path, env, evaluate):
        """Scan all files with the specified name and parse them."""

        rootdir = os.path.dirname(path)
        assert rootdir, 'rootdir is empty but must be non-empty'

        envs = [(rootdir, env)]
        assert env is not None

        # Scan directories for all blueprint files
        for basedir, dirnames, filenames in os.walk(rootdir):
            # Drop irrelevant environments
            while not basedir.startswith(envs[-1][0]):
                envs.pop()

            # Filter sub directories
            new_dirnames = []
            for name in dirnames:
                if name in {'.git', '.repo'}:
                    continue
                if basedir == rootdir and name == 'out':
                    continue
                new_dirnames.append(name)
            dirnames[:] = new_dirnames

            # Parse blueprint files
            if filename in filenames:
                try:
                    path = os.path.join(basedir, filename)
                    sys.stdout.flush()
                    sub_env = self._parse_file_recursive(path, envs[-1][1],
                                                         evaluate, False)
                    assert sub_env is not None
                    envs.append((basedir, sub_env))
                except IOError:
                    pass


    def parse_file(self, path, env=None, evaluate=True,
                   default_sub_name=_DEFAULT_SUB_NAME):
        """Parse blueprint files recursively."""

        if env is None:
            env = {}

        path = os.path.abspath(path)

        sub_env = self._read_file(path, env)[1]

        if 'subdirs' in sub_env or 'optional_subdirs' in sub_env:
            self._parse_file_recursive(path, env, evaluate, True)
        else:
            self._scan_and_parse_all_file_recursive(
                default_sub_name, path, env, evaluate)


#------------------------------------------------------------------------------
# Transformation
#------------------------------------------------------------------------------

def _build_named_modules_dict(modules):
    """Build a name-to-module dict."""
    named_modules = {}
    for i, (ident, attrs) in enumerate(modules):
        name = attrs.get('name')
        if name is not None:
            named_modules[name] = [ident, attrs, i]
    return named_modules


def _po_sorted_modules(modules, named_modules):
    """Sort modules in post order."""
    modules = [(ident, attrs, i) for i, (ident, attrs) in enumerate(modules)]

    # Build module dependency graph.
    edges = {}
    for ident, attrs, module_id in modules:
        defaults = attrs.get('defaults')
        if defaults:
            edges[module_id] = set(
                named_modules[default][2] for default in defaults)

    # Traverse module graph in post order.
    post_order = []
    visited = set()

    def _traverse(module_id):
        visited.add(module_id)
        for next_module_id in edges.get(module_id, []):
            if next_module_id not in visited:
                _traverse(next_module_id)
        post_order.append(modules[module_id])

    for module_id in range(len(modules)):
        if module_id not in visited:
            _traverse(module_id)

    return post_order


def evaluate_default(attrs, default_attrs):
    """Add default attributes if the keys do not exist."""
    for key, value in default_attrs.items():
        if key not in attrs:
            attrs[key] = value
        else:
            attrs_value = attrs[key]
            if isinstance(value, Dict) and isinstance(attrs_value, Dict):
                attrs[key] = evaluate_default(attrs_value, value)
    return attrs


def evaluate_defaults(modules):
    """Add default attributes to all modules if the keys do not exist."""
    named_modules = _build_named_modules_dict(modules)
    for ident, attrs, i in _po_sorted_modules(modules, named_modules):
        for default in attrs.get('defaults', []):
            attrs = evaluate_default(attrs, named_modules[default][1])
        modules[i] = (ident, attrs)
    return modules


def fill_module_namespaces(root_bp, modules):
    """Collect soong_namespace definition and set a `_namespace` property to
    each module definitions."""

    # Collect all namespaces
    rootdir = os.path.dirname(os.path.abspath(root_bp))
    namespaces = {rootdir}
    for ident, attrs in modules:
        if ident == 'soong_namespace':
            namespaces.add(os.path.dirname(attrs['_path']))

    # Build a path matcher for module namespaces
    namespaces = sorted(namespaces, reverse=True)
    path_matcher = re.compile(
        '|'.join('(' + re.escape(x) + '/.*)' for x in namespaces))

    # Trim the root directory prefix
    rootdir_prefix_len = len(rootdir) + 1
    namespaces = [path[rootdir_prefix_len:] for path in namespaces]

    # Fill in module namespaces
    for ident, attrs in modules:
        match = path_matcher.match(attrs['_path'])
        attrs['_namespace'] = namespaces[match.lastindex - 1]

    return modules
