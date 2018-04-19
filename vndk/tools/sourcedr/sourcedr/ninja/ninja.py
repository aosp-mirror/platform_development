#!/usr/bin/env python3

"""Ninja File Parser.
"""

from __future__ import print_function

import argparse
import collections
import os
import re
import struct
import sys

try:
    import cPickle as pickle  # Python 2
except ImportError:
    import pickle  # Python 3

try:
    from cStringIO import StringIO  # Python 2
except ImportError:
    from io import StringIO  # Python 3

try:
    from sys import intern
except ImportError:
    pass  # In Python 2, intern() is a built-in function.

if sys.version_info < (3,):
    # Wrap built-in open() function to ignore encoding in Python 2.
    _builtin_open = open
    def open(path, mode, encoding=None):
        return _builtin_open(path, mode)

    # Replace built-in zip() function with itertools.izip
    from itertools import izip as zip


class EvalEnv(dict):
    __slots__ = ('parent')


    def __init__(self, *args, **kwargs):
        super(EvalEnv, self).__init__(*args, **kwargs)
        self.parent = None


    def get_recursive(self, key, default=None):
        try:
            return self[key]
        except KeyError:
            if self.parent:
                return self.parent.get_recursive(key, default)
            return default


class BuildEvalEnv(EvalEnv):
    __slots__ = ('_build_env', '_rule_env')


    def __init__(self, build_env, rule_env):
        self._build_env = build_env
        self._rule_env = rule_env


    def get_recursive(self, key, default=None):
        try:
            return self._build_env[key]
        except KeyError:
            pass

        if self._rule_env:
            try:
                return self._rule_env[key]
            except KeyError:
                pass

        if self._build_env.parent:
            return self._build_env.parent.get_recursive(key, default)
        return default


class EvalError(ValueError):
    """Exceptions for ``EvalString`` evalution errors."""
    pass


class EvalCircularError(EvalError):
    """Exception for circular substitution in ``EvalString``."""


    def __init__(self, expanded_vars):
        super(EvalCircularError, self).__init__(
                'circular evaluation: ' + ' -> '.join(expanded_vars))


class EvalString(tuple):
    """Strings with variables to be substituted."""


    def __bool__(self):
        """Check whether this is an empty string."""
        return len(self) > 1


    def __nonzero__(self):
        """Check whether this is an empty string (Python2)."""
        return self.__bool__()


    def create_iters(self):
        """Create descriptors and segments iterators."""
        curr_iter = iter(self)
        descs = next(curr_iter)
        return zip(descs, curr_iter)


def _eval_string(s, env, expanded_vars, result_buf):
    """Evaluate each segments in ``EvalString`` and write result to the
    given StringIO buffer.

    Args:
        env: A ``dict`` that maps a name to ``EvalString`` object.
        expanded_vars: A ``list`` that keeps the variable under evaluation.
        result_buf: Output buffer.
    """
    if type(s) is str:
        result_buf.write(s)
        return

    for desc, seg in s.create_iters():
        if desc == 't':
            # Append raw text
            result_buf.write(seg)
        else:
            # Substitute variables
            varname = seg
            if varname in expanded_vars:
                raise EvalCircularError(expanded_vars + [varname])
            expanded_vars.append(varname)
            try:
                next_es = env.get_recursive(varname)
                if next_es:
                    _eval_string(next_es, env, expanded_vars, result_buf)
            finally:
                expanded_vars.pop()


def eval_string(s, env):
    """Evaluate a ``str`` or ``EvalString`` in an environment.

    Args:
        env: A ``dict`` that maps a name to an ``EvalString`` object.

    Returns:
        str: The result of evaluation.

    Raises:
        EvalNameError: Unknown variable name occurs.
        EvalCircularError: Circular variable substitution occurs.
    """
    expanded_vars = []
    result_buf = StringIO()
    _eval_string(s, env, expanded_vars, result_buf)
    return result_buf.getvalue()


def eval_path_strings(strs, env):
    """Evalute a list of ``EvalString`` in an environment and normalize paths.

    Args:
        strs: A list of ``EvalString`` which should be treated as paths.
        env: A ``dict`` that maps a name to an ``EvalString`` object.

    Returns:
        The list of evaluated strings.
    """
    return [intern(os.path.normpath(eval_string(s, env))) for s in strs]


class EvalStringBuilder(object):
    def __init__(self):
        self._segs = ['']


    def append_raw(self, text):
        descs = self._segs[0]
        if descs and descs[-1] == 't':
            self._segs[-1] += text
        else:
            self._segs[0] += 't'
            self._segs.append(text)
        return self


    def append_var(self, varname):
        self._segs[0] += 'v'
        self._segs.append(varname)
        return self


    def getvalue(self):
        return EvalString(intern(seg) for seg in self._segs)


class Build(object):
    __slots__ = ('explicit_outs', 'implicit_outs', 'rule', 'explicit_ins',
                 'implicit_ins', 'prerequisites', 'bindings',
                 'depfile_implicit_ins')


class Rule(object):
    __slots__ = ('name', 'bindings')


class Pool(object):
    __slots__ = ('name', 'bindings')


class Default(object):
    __slots__ = ('outs')


Token = collections.namedtuple('Token', 'kind line column value')


class TK(object):
    """Token ID enumerations."""

    # Trivial tokens
    EOF = 0
    COMMENT = 1
    NEWLINE = 2
    SPACE = 3
    ESC_NEWLINE = 4
    IDENT = 5
    PIPE2 = 6
    PIPE = 7
    COLON = 8
    ASSIGN = 9

    # Non-trivial tokens
    PATH = 10
    STRING = 11


class TokenMatcher(object):
    def __init__(self, patterns):
        self._matcher = re.compile('|'.join('(' + p + ')' for k, p in patterns))
        self._kinds = [k for k, p in patterns]


    def match(self, buf, pos):
        match = self._matcher.match(buf, pos)
        if not match:
            return None
        return (self._kinds[match.lastindex - 1], match.start(), match.end())


class ParseError(ValueError):
    def __init__(self, path, line, column, reason=None):
        self.path = path
        self.line = line
        self.column = column
        self.reason = reason


    def __repr__(self):
        s = 'ParseError: {}:{}:{}'.format(self.path, self.line, self.column)
        if self.reason:
            s += ': ' + self.reason
        return s


class Lexer(object):
    def __init__(self, lines_iterable, path='<stdin>', encoding='utf-8'):
        self.encoding = encoding
        self.path = path

        self._line_iter = iter(lines_iterable)
        self._line_buf = None
        self._line = 0
        self._line_pos = 0
        self._line_end = 0

        self._line_start = True

        self._next_token = None
        self._next_pos = None


    def raise_error(self, reason=None):
        raise ParseError(self.path, self._line, self._line_pos + 1, reason)


    def _read_next_line(self):
        try:
            self._line_buf = next(self._line_iter)
            self._line_pos = 0
            self._line_end = len(self._line_buf)
            self._line += 1
            return True
        except StopIteration:
            self._line_buf = None
            return False


    def _ensure_line(self):
        if self._line_buf and self._line_pos < self._line_end:
            return True
        return self._read_next_line()

    _COMMENT_MATCHER = re.compile(r'[ \t]*(?:#[^\n]*)?(?=\n)')


    def _ensure_non_comment_line(self):
        if not self._ensure_line():
            return False
        # Match comments or spaces
        match = self._COMMENT_MATCHER.match(self._line_buf)
        if not match:
            return True
        # Move the cursor to the newline character
        self._line_pos = match.end()
        return True

    _SPACE_MATCHER = re.compile(r'[ \t]+')


    def _skip_space(self):
        match = self._SPACE_MATCHER.match(self._line_buf, self._line_pos)
        if match:
            self._line_pos = match.end()

    _SIMPLE_TOKEN_MATCHER = TokenMatcher([
        (TK.COMMENT, r'#[^\n]*'),
        (TK.NEWLINE, r'[\r\n]'),
        (TK.SPACE, r'[ \t]+'),
        (TK.ESC_NEWLINE, r'\$[\r\n]'),
        (TK.IDENT, r'[\w_.-]+'),
        (TK.PIPE2, r'\|\|'),
        (TK.PIPE, r'\|'),
        (TK.COLON, r':'),
        (TK.ASSIGN, r'='),
    ])


    def peek(self):
        if self._next_token is not None:
            return self._next_token
        while True:
            if not self._ensure_non_comment_line():
                return Token(TK.EOF, self._line, self._line_pos + 1, '')

            match = self._SIMPLE_TOKEN_MATCHER.match(
                    self._line_buf, self._line_pos)
            if not match:
                return None
            kind, start, end = match

            # Skip comments and spaces
            if ((kind == TK.SPACE and not self._line_start) or
                (kind == TK.ESC_NEWLINE) or
                (kind == TK.COMMENT)):
                self._line_pos = end
                continue

            # Save the peaked token
            token = Token(kind, self._line, self._line_pos + 1,
                          self._line_buf[start:end])
            self._next_token = token
            self._next_pos = end
            return token


    def lex(self):
        token = self.peek()
        if not token:
            self.raise_error()
        self._line_start = token.kind == TK.NEWLINE
        self._line_pos = self._next_pos
        self._next_token = None
        self._next_pos = None
        return token


    def lex_match(self, match_set):
        token = self.lex()
        if token.kind not in match_set:
            self.raise_error()
        return token


    class STR_TK(object):
        END = 0
        CHARS = 1
        ESC_CHAR = 2
        ESC_NEWLINE = 3
        VAR = 4
        CURVE_VAR = 5


    _PATH_TOKEN_MATCHER = TokenMatcher([
        (STR_TK.END, r'[ \t\n|:]'),
        (STR_TK.CHARS, r'[^ \t\n|:$]+'),
        (STR_TK.ESC_CHAR, r'\$[^\n{\w_-]'),
        (STR_TK.ESC_NEWLINE, r'\$\n[ \t]*'),
        (STR_TK.VAR, r'\$[\w_-]+'),
        (STR_TK.CURVE_VAR, r'\$\{[\w_.-]+\}'),
    ])


    _STR_TOKEN_MATCHER = TokenMatcher([
        (STR_TK.END, r'\n+'),
        (STR_TK.CHARS, r'[^\n$]+'),
        (STR_TK.ESC_CHAR, r'\$[^\n{\w_-]'),
        (STR_TK.ESC_NEWLINE, r'\$\n[ \t]*'),
        (STR_TK.VAR, r'\$[\w_-]+'),
        (STR_TK.CURVE_VAR, r'\$\{[\w_.-]+\}'),
    ])


    def _lex_string_or_path(self, matcher, result_kind):
        self._ensure_line()
        self._skip_space()

        start_line = self._line
        start_column = self._line_pos + 1

        builder = EvalStringBuilder()

        while True:
            if not self._ensure_line():
                break

            match = matcher.match(self._line_buf, self._line_pos)
            if not match:
                self.raise_error('unknown character sequence')

            kind, start, end = match
            if kind == self.STR_TK.END:
                break

            self._line_pos = end

            if kind == self.STR_TK.CHARS:
                builder.append_raw(self._line_buf[start:end])
            elif kind == self.STR_TK.ESC_CHAR:
                ch = self._line_buf[start + 1]
                if ch in ' \t:$':
                    builder.append_raw(ch)
                else:
                    self.raise_error('bad escape sequence')
            elif kind == self.STR_TK.ESC_NEWLINE:
                if not self._read_next_line():
                    break
                self._skip_space()
            elif kind == self.STR_TK.VAR:
                builder.append_var(self._line_buf[start + 1 : end])
            else:
                assert kind == self.STR_TK.CURVE_VAR
                builder.append_var(self._line_buf[start + 2 : end - 1])

        self._next_token = None
        return Token(result_kind, start_line, start_column, builder.getvalue())


    def lex_path(self):
        return self._lex_string_or_path(self._PATH_TOKEN_MATCHER, TK.PATH)


    def lex_string(self):
        return self._lex_string_or_path(self._STR_TOKEN_MATCHER, TK.STRING)


Manifest = collections.namedtuple('Manifest', 'builds rules pools defaults')


class Parser(object):
    """Ninja Manifest Parser

    This parser parses ninja-build manifest files, such as::

        cflags = -Wall

        pool cc_pool
          depth = 1

        rule cc
          command = gcc -c -o $out $in $cflags $extra_cflags
          pool = cc_pool

        build test.o : cc test.c
          extra_cflags = -Werror

        default test.o

    Example:
        >>> manifest = Parser().parse('build.ninja', 'utf-8')
        >>> print(manifest.builds)

    """


    def __init__(self, base_dir=None):
        if base_dir is None:
            self._base_dir = os.getcwd()
        else:
            self._base_dir = base_dir

        # File context
        self._context = []
        self._lexer = None
        self._env = None

        # Intermediate results
        self._builds = []
        self._rules = []
        self._pools = []
        self._defaults = []

        self._rules_dict = {}


    def _push_context(self, lexer, env):
        """Push a parsing file context.

        Args:
            lexer: Lexer for the associated file.
            env: Environment for global variable bindings.
        """

        self._context.append((self._lexer, self._env))
        self._lexer = lexer
        self._env = env


    def _pop_context(self):
        """Push a parsing file context."""

        current_context = (self._lexer, self._env)
        self._lexer, self._env = self._context.pop()
        return current_context


    def parse(self, path, encoding, depfile=None):
        """Parse a ninja-build manifest file.

        Args:
            path (str): Input file path to be parsed.
            encoding (str): Input file encoding.

        Returns:
            Manifest: Parsed manifest for the given ninja-build manifest file.
        """

        self._parse_internal(path, encoding, EvalEnv())
        if depfile:
            self.parse_dep_file(depfile, encoding)
        return Manifest(self._builds, self._rules, self._pools, self._defaults)


    def _parse_internal(self, path, encoding, env):
        path = os.path.join(self._base_dir, path)
        with open(path, 'r', encoding=encoding) as fp:
            self._push_context(Lexer(fp, path, encoding), env)
            try:
                self._parse_all_top_level_stmts()
            finally:
                self._pop_context()


    def _parse_all_top_level_stmts(self):
        """Parse all top-level statements in a file."""
        while self._parse_top_level_stmt():
            pass


    def _parse_top_level_stmt(self):
        """Parse a top level statement."""

        token = self._lexer.peek()
        if not token:
            # An unexpected non-trivial token occurs.  Raise an error.
            self._lexer.raise_error()

        if token.kind == TK.EOF:
            return False
        elif token.kind == TK.NEWLINE:
            self._lexer.lex()
        elif token.kind == TK.IDENT:
            ident = token.value
            if ident == 'rule':
                self._parse_rule_stmt()
            elif ident == 'build':
                self._parse_build_stmt()
            elif ident == 'default':
                self._parse_default_stmt()
            elif ident == 'pool':
                self._parse_pool_stmt()
            elif ident in {'subninja', 'include'}:
                self._parse_include_stmt()
            else:
                self._parse_global_binding_stmt()
        else:
            # An unexpected trivial token occurs.  Raise an error.
            self._lexer.raise_error()
        return True


    def _parse_path_list(self, end_set):
        """Parse a list of paths."""

        result = []
        while True:
            token = self._lexer.peek()
            if token:
                if token.kind in end_set:
                    break
                elif token.kind != TK.IDENT:
                    self._lexer.raise_error()

            token = self._lexer.lex_path()
            result.append(token.value)
        return result


    def _parse_binding_stmt(self):
        """Parse a variable binding statement.

        Example:
            IDENT = STRING
        """
        key = self._lexer.lex_match({TK.IDENT}).value
        self._lexer.lex_match({TK.ASSIGN})
        token = self._lexer.lex_string()
        value = token.value
        self._lexer.lex_match({TK.NEWLINE, TK.EOF})
        return (key, value)


    def _parse_global_binding_stmt(self):
        """Parse a global variable binding statement.

        Example:
            IDENT = STRING
        """

        key, value = self._parse_binding_stmt()
        value = eval_string(value, self._env)
        self._env[key] = value


    def _parse_local_binding_block(self):
        """Parse several local variable bindings.

        Example:
            SPACE IDENT1 = STRING1
            SPACE IDENT2 = STRING2
        """
        result = EvalEnv()
        while True:
            token = self._lexer.peek()
            if not token or token.kind != TK.SPACE:
                break
            self._lexer.lex()
            key, value = self._parse_binding_stmt()
            result[key] = value
        return result


    def _parse_build_stmt(self):
        """Parse `build` statement.

        Example:
            build PATH1 PATH2 | PATH3 PATH4 : IDENT PATH5 PATH6 | $
                  PATH7 PATH8 || PATH9 PATH10
            SPACE IDENT1 = STRING1
            SPACE IDENT2 = STRING2
        """

        token = self._lexer.lex_match({TK.IDENT})
        assert token.value == 'build'

        build = Build()

        # Parse explicit outs
        explicit_outs = self._parse_path_list({TK.PIPE, TK.COLON})

        # Parse implicit outs
        token = self._lexer.peek()
        if token.kind == TK.PIPE:
            self._lexer.lex()
            implicit_outs = self._parse_path_list({TK.COLON})
        else:
            implicit_outs = tuple()

        self._lexer.lex_match({TK.COLON})

        # Parse rule name for this build statement
        build.rule = self._lexer.lex_match({TK.IDENT}).value
        try:
            rule_env = self._rules_dict[build.rule].bindings
        except KeyError:
            if build.rule != 'phony':
                self._lexer.raise_error('undeclared rule name')
            rule_env = self._env

        # Parse explicit ins
        explicit_ins = self._parse_path_list(
                {TK.PIPE, TK.PIPE2, TK.NEWLINE, TK.EOF})

        # Parse implicit ins
        token = self._lexer.peek()
        if token.kind == TK.PIPE:
            self._lexer.lex()
            implicit_ins = self._parse_path_list({TK.PIPE2, TK.NEWLINE, TK.EOF})
        else:
            implicit_ins = tuple()

        # Parse order-only prerequisites
        token = self._lexer.peek()
        if token.kind == TK.PIPE2:
            self._lexer.lex()
            prerequisites = self._parse_path_list({TK.NEWLINE, TK.EOF})
        else:
            prerequisites = tuple()

        self._lexer.lex_match({TK.NEWLINE, TK.EOF})

        # Parse local bindings
        bindings = self._parse_local_binding_block()
        bindings.parent = self._env
        if bindings:
            build.bindings = bindings
        else:
            # Don't keep the empty ``dict`` object if there are no bindings
            build.bindings = None

        # Evaluate all paths
        env = BuildEvalEnv(bindings, rule_env)

        build.explicit_outs = eval_path_strings(explicit_outs, env)
        build.implicit_outs = eval_path_strings(implicit_outs, env)
        build.explicit_ins = eval_path_strings(explicit_ins, env)
        build.implicit_ins = eval_path_strings(implicit_ins, env)
        build.prerequisites = eval_path_strings(prerequisites, env)
        build.depfile_implicit_ins = tuple()

        self._builds.append(build)


    def _parse_rule_stmt(self):
        """Parse a `rule` statement.

        Example:
            rule IDENT
            SPACE IDENT1 = STRING1
            SPACE IDENT2 = STRING2
        """

        token = self._lexer.lex_match({TK.IDENT})
        assert token.value == 'rule'

        rule = Rule()
        rule.name = self._lexer.lex_match({TK.IDENT}).value
        self._lexer.lex_match({TK.NEWLINE, TK.EOF})
        rule.bindings = self._parse_local_binding_block()

        self._rules.append(rule)
        self._rules_dict[rule.name] = rule


    def _parse_default_stmt(self):
        """Parse a `default` statement.

        Example:
            default PATH1 PATH2 PATH3
        """

        token = self._lexer.lex_match({TK.IDENT})
        assert token.value == 'default'

        default = Default()
        outs = self._parse_path_list({TK.NEWLINE, TK.EOF})
        default.outs = eval_path_strings(outs, self._env)

        self._lexer.lex_match({TK.NEWLINE, TK.EOF})

        self._defaults.append(default)


    def _parse_pool_stmt(self):
        """Parse a `pool` statement.

        Example:
            pool IDENT
            SPACE IDENT1 = STRING1
            SPACE IDENT2 = STRING2
        """
        token = self._lexer.lex_match({TK.IDENT})
        assert token.value == 'pool'

        pool = Pool()

        token = self._lexer.lex()
        assert token.kind == TK.IDENT
        pool.name = token.value

        self._lexer.lex_match({TK.NEWLINE, TK.EOF})

        pool.bindings = self._parse_local_binding_block()

        self._pools.append(pool)


    def _parse_include_stmt(self):
        """Parse an `include` or `subninja` statement.

        Example:
            include PATH
            subninja PATH
        """

        token = self._lexer.lex_match({TK.IDENT})
        assert token.value in {'include', 'subninja'}
        wrap_env = token.value == 'subninja'

        token = self._lexer.lex_path()
        path = eval_string(token.value, self._env)  # XXX: Check lookup order
        self._lexer.lex_match({TK.NEWLINE, TK.EOF})

        if wrap_env:
            env = EvalEnv()
            env.parent = self._env
        else:
            env = self._env
        self._parse_internal(path, self._lexer.encoding, env)


    def parse_dep_file(self, path, encoding):
        depfile = DepFileParser().parse(path, encoding)
        for build in self._builds:
            depfile_implicit_ins = set()
            for explicit_out in build.explicit_outs:
                deps = depfile.get(explicit_out)
                if deps:
                    depfile_implicit_ins.update(deps.implicit_ins)
            build.depfile_implicit_ins = tuple(sorted(depfile_implicit_ins))


class DepFileError(ValueError):
    pass


class DepFileRecord(object):
    __slots__ = ('id', 'explicit_out', 'mtime', 'implicit_ins')


    def __init__(self, id, explicit_out, mtime, implicit_ins):
        self.id = id
        self.explicit_out = explicit_out
        self.mtime = mtime
        self.implicit_ins = implicit_ins


class DepFileParser(object):
    """Ninja deps log parser which parses ``.ninja_deps`` file.
    """


    def __init__(self):
        self._deps = []
        self._paths = []
        self._path_deps = {}


    def parse(self, path, encoding):
        with open(path, 'rb') as fp:
            return self._parse(fp, encoding)


    @staticmethod
    def _unpack_uint32(buf):
        return struct.unpack('<I', buf)[0]


    @staticmethod
    def _unpack_uint32_iter(buf):
        for p in struct.iter_unpack('<I', buf):
            yield p[0]


    if sys.version_info < (3,):
        @staticmethod
        def _extract_path(s, encoding):
            pos = len(s)
            count = 3
            while count > 0 and pos > 0 and s[pos - 1] == b'\0':
                pos -= 1
                count -= 1
            return intern(s[0:pos])
    else:
        @staticmethod
        def _extract_path(s, encoding):
            pos = len(s)
            count = 3
            while count > 0 and pos > 0 and s[pos - 1] == 0:
                pos -= 1
                count -= 1
            return intern(s[0:pos].decode(encoding))


    def _get_path(self, index):
        try:
            return self._paths[index]
        except IndexError:
            raise DepFileError('path index overflow')


    def _parse(self, fp, encoding):
        # Check the magic word
        if fp.readline() != b'# ninjadeps\n':
            raise DepFileError('bad magic word')

        # Check the file format version
        version = self._unpack_uint32(fp.read(4))
        if version != 3:
            raise DepFileError('unsupported deps log version: ' + str(version))

        # Read the records
        MAX_RECORD_SIZE = (1 << 19) - 1
        while True:
            buf = fp.read(4)
            if not buf:
                break

            record_size = self._unpack_uint32(buf)
            is_dep = bool(record_size >> 31)
            record_size &= (1 << 31) - 1

            if record_size > MAX_RECORD_SIZE:
                raise DepFileError('record size overflow')

            if is_dep:
                if record_size % 4 != 0 or record_size < 8:
                    raise DepFileError('corrupted deps record')

                buf = fp.read(record_size)

                dep_iter = self._unpack_uint32_iter(buf)

                idx = len(self._deps)
                explicit_out = self._get_path(next(dep_iter))
                mtime = next(dep_iter)
                implicit_ins = [self._get_path(p) for p in dep_iter]

                deps = DepFileRecord(idx, explicit_out, mtime, implicit_ins)

                old_deps = self._path_deps.get(explicit_out)
                if not old_deps:
                    self._deps.append(deps)
                    self._path_deps[explicit_out] = deps
                elif old_deps.mtime > deps.mtime:
                    self._deps.append(None)
                else:
                    self._deps[old_deps.id] = None
                    self._deps.append(deps)
                    self._path_deps[explicit_out] = deps
            else:
                if record_size < 4:
                    raise DepFileError('corrupted path record')
                buf = fp.read(record_size - 4)
                path = self._extract_path(buf, encoding)
                buf = fp.read(4)
                checksum = 0xffffffff ^ self._unpack_uint32(buf)
                if len(self._paths) != checksum:
                    raise DepFileError('bad path record checksum')
                self._paths.append(path)

        return self._path_deps


def _parse_args():
    """Parse command line options."""

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest='command')

    def _register_input_file_args(parser):
        parser.add_argument('input_file', help='input ninja file')
        parser.add_argument('--ninja-deps', help='.ninja_deps file')
        parser.add_argument('--cwd', help='working directory for ninja')
        parser.add_argument('--encoding', default='utf-8',
                            help='ninja file encoding')

    # dump sub-command
    parser_dump = subparsers.add_parser('dump', help='dump dependency graph')
    _register_input_file_args(parser_dump)
    parser_dump.add_argument('-o', '--output', help='output file')

    # pickle sub-command
    parser_pickle = subparsers.add_parser(
            'pickle', help='serialize dependency graph with pickle')
    _register_input_file_args(parser_pickle)
    parser_pickle.add_argument('-o', '--output', required=True,
                               help='output file')

    # Parse arguments and check sub-command
    args = parser.parse_args()
    if args.command is None:
        parser.print_help()
        sys.exit(1)

    return args


def load_manifest_from_args(args):
    """Load the input manifest specified by command line options."""

    input_file = args.input_file

    # If the input file name ends with `.pickle`, load it with pickle.load().
    if input_file.endswith('.pickle'):
        with open(input_file, 'rb') as pickle_file:
            return pickle.load(pickle_file)

    # Parse the ninja file
    return Parser(args.cwd).parse(args.input_file, args.encoding,
                                  args.ninja_deps)


def dump_manifest(manifest, file):
    """Dump a manifest to a text file."""

    for rule in manifest.rules:
        print('rule', rule.name, file=file)

    for build in manifest.builds:
        print('build', file=file)
        for path in build.explicit_outs:
            print('  explicit_out:', path, file=file)
        for path in build.implicit_outs:
            print('  implicit_out:', path, file=file)
        for path in build.explicit_ins:
            print('  explicit_in:', path, file=file)
        for path in build.implicit_ins:
            print('  implicit_in:', path, file=file)
        for path in build.prerequisites:
            print('  prerequisites:', path, file=file)
        for path in build.depfile_implicit_ins:
            print('  depfile_implicit_in:', path, file=file)

    for pool in manifest.pools:
        print('pool', pool.name, file=file)

    for default in manifest.defaults:
        print('default', file=file)
        for path in default.outs:
            print('  out:', path, file=file)


def command_dump_main(args):
    """Main function for the dump sub-command"""
    if args.output is None:
        dump_manifest(load_manifest_from_args(args), sys.stdout)
    else:
        with open(args.output, 'w') as output_file:
            dump_manifest(load_manifest_from_args(args), output_file)


def command_pickle_main(args):
    """Main function for the pickle sub-command"""
    with open(args.output, 'wb') as output_file:
        pickle.dump(load_manifest_from_args(args), output_file)


def main():
    """Main function for the executable"""
    args = _parse_args()
    if args.command == 'dump':
        command_dump_main(args)
    elif args.command == 'pickle':
        command_pickle_main(args)
    else:
        raise KeyError('unknown command ' + args.command)


if __name__ == '__main__':
    import ninja
    ninja.main()
