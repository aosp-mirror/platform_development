#!/usr/bin/env python3

import ninja

import os
import unittest


TEST_DIR = os.path.abspath(os.path.dirname(__file__))
TEST_DATA_DIR = os.path.join(TEST_DIR, 'testdata')

ENCODING = 'utf-8'


class MockedParser(ninja.Parser):
    def __init__(self, *args, **kwargs):
        super(MockedParser, self).__init__(*args, **kwargs)
        self.mocked_env = []

    def _push_context(self, lexer, env):
        super(MockedParser, self)._push_context(lexer, env)
        self.mocked_env.append(env)


class EvalStringTest(unittest.TestCase):
    def test_empty(self):
        s = ninja.EvalStringBuilder().getvalue()
        self.assertFalse(s)
        self.assertEqual('', ninja.eval_string(s, ninja.EvalEnv()))

    def test_append_raw(self):
        s = ninja.EvalStringBuilder().append_raw('a').getvalue()
        self.assertTrue(s)
        self.assertEqual('a', ninja.eval_string(s, ninja.EvalEnv()))

    def test_append_raw_concat(self):
        sb = ninja.EvalStringBuilder()
        sb.append_raw('a')
        sb.append_raw('b')
        s = sb.getvalue()

        self.assertTrue(s)
        self.assertEqual('ab', ninja.eval_string(s, ninja.EvalEnv()))

    def test_append_var(self):
        s = ninja.EvalStringBuilder().append_var('key').getvalue()
        self.assertTrue(s)

    def test_var_eval(self):
        env = ninja.EvalEnv()
        env['key'] = ninja.EvalStringBuilder().append_raw('value').getvalue()

        s = ninja.EvalStringBuilder().append_var('key').getvalue()
        self.assertEqual('value', ninja.eval_string(s, env))

    def test_var_concat_eval(self):
        env = ninja.EvalEnv()
        env['key1'] = ninja.EvalStringBuilder().append_raw('a').getvalue()
        env['key2'] = ninja.EvalStringBuilder().append_raw('b').getvalue()

        sb = ninja.EvalStringBuilder()
        sb.append_var('key1')
        sb.append_var('key2')
        s = sb.getvalue()

        self.assertEqual('ab', ninja.eval_string(s, env))

    def test_var_repeat_eval(self):
        env = ninja.EvalEnv()
        env['key1'] = ninja.EvalStringBuilder().append_raw('a').getvalue()
        env['key2'] = ninja.EvalStringBuilder().append_raw('b').getvalue()

        sb = ninja.EvalStringBuilder()
        sb.append_var('key1')
        sb.append_var('key1')
        sb.append_var('key2')
        sb.append_var('key1')
        sb.append_var('key2')
        s = sb.getvalue()

        self.assertEqual('aabab', ninja.eval_string(s, env))

    def test_var_recursive_eval(self):
        env = ninja.EvalEnv()
        env['a'] = ninja.EvalStringBuilder().append_var('b').getvalue()
        env['c'] = ninja.EvalStringBuilder().append_raw('d').getvalue()

        sb = ninja.EvalStringBuilder()
        sb.append_var('c')
        sb.append_var('c')
        env['b'] = sb.getvalue()

        sb = ninja.EvalStringBuilder()
        sb.append_var('a')
        sb.append_var('a')
        s = sb.getvalue()

        self.assertEqual('dddd', ninja.eval_string(s, env))

    def test_unknown_variable_eval_error(self):
        s = ninja.EvalStringBuilder().append_var('a').getvalue()
        self.assertEqual('', ninja.eval_string(s, ninja.EvalEnv()))

    def test_circular_eval_eval_error(self):
        env = ninja.EvalEnv()
        env['a'] = ninja.EvalStringBuilder().append_var('b').getvalue()
        env['b'] = ninja.EvalStringBuilder().append_var('b').getvalue()

        s = ninja.EvalStringBuilder().append_var('a').getvalue()
        with self.assertRaises(ninja.EvalCircularError):
            ninja.eval_string(s, env)

    def test_raw_and_var_eval(self):
        env = ninja.EvalEnv()
        env['b'] = ninja.EvalStringBuilder().append_raw('d').getvalue()

        sb = ninja.EvalStringBuilder()
        sb.append_raw('a')
        sb.append_var('b')
        sb.append_raw('c')
        s = sb.getvalue()

        self.assertEqual('adc', ninja.eval_string(s, env))


class ParseErrorTest(unittest.TestCase):
    def test_repr(self):
        ex = ninja.ParseError('build.ninja', 5, 1)
        self.assertEqual('ParseError: build.ninja:5:1', repr(ex))

        ex = ninja.ParseError('build.ninja', 5, 1, 'invalid char')
        self.assertEqual('ParseError: build.ninja:5:1: invalid char', repr(ex))


class LexerTest(unittest.TestCase):
    def test_peek_skip_comment(self):
        lexer = ninja.Lexer(['#comment'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.EOF, tok.kind)

    def test_peek_skip_comment_line(self):
        lexer = ninja.Lexer(['#comment\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

        lexer = ninja.Lexer([' #comment\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

        lexer = ninja.Lexer(['\t#comment\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

        lexer = ninja.Lexer([' \t#comment\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

    def test_peek_skip_empty_line(self):
        lexer = ninja.Lexer([' \n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

        lexer = ninja.Lexer(['\t\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

        lexer = ninja.Lexer([' \t\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

    def test_peek_newline(self):
        lexer = ninja.Lexer(['\n'])
        tok = lexer.peek()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)

    def test_peek_space(self):
        lexer = ninja.Lexer([' a'])

        tok = lexer.peek()
        self.assertEqual(ninja.TK.SPACE, tok.kind)
        tok = lexer.peek()  # Again
        self.assertEqual(ninja.TK.SPACE, tok.kind)  # Not changed

        tok = lexer.lex()  # Consume
        self.assertEqual(ninja.TK.SPACE, tok.kind)  # Not changed
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

    def test_lex_space(self):
        lexer = ninja.Lexer([' '])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)

        lexer = ninja.Lexer(['\t'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)

        lexer = ninja.Lexer(['\t '])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)

        lexer = ninja.Lexer([' \t'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)

        lexer = ninja.Lexer([' a'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

    def test_lex_skip_space(self):
        lexer = ninja.Lexer(['a b'])

        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)

        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(3, tok.column)

    def test_lex_skip_space_newline_escape(self):
        lexer = ninja.Lexer(['build $\n', ' \texample'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(2, tok.line)
        self.assertEqual(3, tok.column)

        lexer = ninja.Lexer(['build $\n', 'example'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(2, tok.line)
        self.assertEqual(1, tok.column)

        lexer = ninja.Lexer(['build a:$\n', 'example'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(7, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.COLON, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(8, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(2, tok.line)
        self.assertEqual(1, tok.column)

        # Multiple newline escapes.
        lexer = ninja.Lexer(['build $\n', '$\n', '$\n', 'example'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(4, tok.line)
        self.assertEqual(1, tok.column)

    def test_peek_space_after_newline(self):
        lexer = ninja.Lexer(['a b\n', ' c'])

        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)

        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(3, tok.column)

        tok = lexer.lex()
        self.assertEqual(ninja.TK.NEWLINE, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(4, tok.column)

        # A space token must be emitted.
        tok = lexer.lex()
        self.assertEqual(ninja.TK.SPACE, tok.kind)
        self.assertEqual(2, tok.line)
        self.assertEqual(1, tok.column)

        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)
        self.assertEqual(2, tok.line)
        self.assertEqual(2, tok.column)

    def test_lex_ident(self):
        lexer = ninja.Lexer(['abcdefghijklmnopqrstuvwxyz'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

        lexer = ninja.Lexer(['ABCDEFGHIJKLMNOPQRSTUVWXYZ'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

        lexer = ninja.Lexer(['0123456789'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

        lexer = ninja.Lexer(['.'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

        lexer = ninja.Lexer(['-'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

        lexer = ninja.Lexer(['_'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.IDENT, tok.kind)

    def test_lex_assign(self):
        lexer = ninja.Lexer(['='])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.ASSIGN, tok.kind)

    def test_lex_colon(self):
        lexer = ninja.Lexer([':'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.COLON, tok.kind)

    def test_lex_pipe(self):
        lexer = ninja.Lexer(['|'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.PIPE, tok.kind)

    def test_lex_pipe2(self):
        lexer = ninja.Lexer(['||'])
        tok = lexer.lex()
        self.assertEqual(ninja.TK.PIPE2, tok.kind)

    def test_lex_non_trivial(self):
        lexer = ninja.Lexer(['$name'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex()
        lexer = ninja.Lexer(['${name}'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex()

    def test_lex_match(self):
        lexer = ninja.Lexer(['ident'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex_match({ninja.TK.PIPE})

    def test_lex_path_char(self):
        lexer = ninja.Lexer(['path1 path2'])

        tok = lexer.lex_path()
        self.assertEqual(ninja.TK.PATH, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        self.assertEqual(('t', 'path1'), tok.value)

        tok = lexer.lex_path()
        self.assertEqual(ninja.TK.PATH, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(7, tok.column)
        self.assertEqual(('t', 'path2'), tok.value)

    def test_lex_str_char(self):
        lexer = ninja.Lexer(['string with spaces'])
        tok = lexer.lex_string()
        self.assertEqual(ninja.TK.STRING, tok.kind)
        self.assertEqual(1, tok.line)
        self.assertEqual(1, tok.column)
        self.assertEqual(('t', 'string with spaces'), tok.value)

    def test_lex_path_escape_char(self):
        for char in ' \t$:':
            lexer = ninja.Lexer(['$' + char])
            tok = lexer.lex_path()
            self.assertEqual(ninja.TK.PATH, tok.kind)
            self.assertEqual(1, tok.line)
            self.assertEqual(1, tok.column)
            self.assertEqual(('t', char), tok.value)

    def test_lex_str_escape_char(self):
        for char in ' \t$:':
            lexer = ninja.Lexer(['$' + char])
            tok = lexer.lex_string()
            self.assertEqual(ninja.TK.STRING, tok.kind)
            self.assertEqual(1, tok.line)
            self.assertEqual(1, tok.column)
            self.assertEqual(('t', char), tok.value)

    def test_lex_path_escape_char_bad(self):
        lexer = ninja.Lexer(['$'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex_path()

        lexer = ninja.Lexer(['$%'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex_path()

    def test_lex_str_escape_char_bad(self):
        lexer = ninja.Lexer(['$'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex_string()

        lexer = ninja.Lexer(['$%'])
        with self.assertRaises(ninja.ParseError):
            lexer.lex_string()

    def test_lex_path_end_char(self):
        for char in ' \t\n:|':
            lexer = ninja.Lexer(['path' + char])
            tok = lexer.lex_path()
            self.assertEqual(ninja.TK.PATH, tok.kind)
            self.assertEqual(1, tok.line)
            self.assertEqual(1, tok.column)
            self.assertEqual(('t', 'path'), tok.value)

    def test_lex_path_var(self):
        lexer = ninja.Lexer(['$a'])
        tok = lexer.lex_path()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('v', 'a',), tok.value)

        lexer = ninja.Lexer(['${a}'])
        tok = lexer.lex_path()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('v', 'a',), tok.value)

        lexer = ninja.Lexer(['path/${a}'])
        tok = lexer.lex_path()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('tv' ,'path/', 'a'), tok.value)

    def test_lex_str_var(self):
        lexer = ninja.Lexer(['$a'])
        tok = lexer.lex_string()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('v', 'a'), tok.value)

        lexer = ninja.Lexer(['${a}'])
        tok = lexer.lex_string()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('v', 'a'), tok.value)

        lexer = ninja.Lexer(['path/${a}'])
        tok = lexer.lex_string()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('tv', 'path/', 'a'), tok.value)

        lexer = ninja.Lexer(['path/${a} with space'])
        tok = lexer.lex_string()
        self.assertIs(type(tok.value), ninja.EvalString)
        self.assertEqual(('tvt', 'path/', 'a', ' with space'), tok.value)


class ParserTest(unittest.TestCase):
    def test_init_base_dir(self):
        parser = ninja.Parser()
        self.assertEqual(os.getcwd(), parser._base_dir)
        parser = ninja.Parser('/path/to/a/dir')
        self.assertEqual('/path/to/a/dir', parser._base_dir)

    def test_global_binding_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'global_binding.ninja')

        parser = MockedParser()
        parser.parse(input_path, ENCODING)

        env = parser.mocked_env[0]
        self.assertEqual('1', env['a'])
        self.assertEqual('2', env['b'])
        self.assertEqual('3', env['c'])
        self.assertEqual('1 2 3', env['d'])
        self.assertEqual('mixed 1 and 2', env['e'])

    def test_rule_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'rule.ninja')

        parser = ninja.Parser()
        manifest = parser.parse(input_path, ENCODING)

        self.assertEqual(2, len(manifest.rules))

        rule_cc = manifest.rules[0]
        self.assertEqual('cc', rule_cc.name)
        self.assertEqual(1, len(rule_cc.bindings))

        sb = ninja.EvalStringBuilder()
        sb.append_raw('gcc -c -o ')
        sb.append_var('outs')
        sb.append_raw(' ')
        sb.append_var('ins')

        self.assertEqual(sb.getvalue(), rule_cc.bindings['command'])

        rule_ld = manifest.rules[1]
        self.assertEqual('ld', rule_ld.name)
        self.assertEqual(1, len(rule_ld.bindings))

        sb = ninja.EvalStringBuilder()
        sb.append_raw('gcc -o ')
        sb.append_var('outs')
        sb.append_raw(' ')
        sb.append_var('ins')

        self.assertEqual(sb.getvalue(), rule_ld.bindings['command'])

    def test_build_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'build.ninja')

        parser = ninja.Parser()
        manifest = parser.parse(input_path, ENCODING)

        self.assertEqual(1, len(manifest.builds))

        build = manifest.builds[0]
        self.assertEqual('explicit_out1', build.explicit_outs[0])
        self.assertEqual('explicit_out2', build.explicit_outs[1])
        self.assertEqual('implicit_out1', build.implicit_outs[0])
        self.assertEqual('implicit_out2', build.implicit_outs[1])
        self.assertEqual('phony', build.rule)
        self.assertEqual('explicit_in1', build.explicit_ins[0])
        self.assertEqual('explicit_in2', build.explicit_ins[1])
        self.assertEqual('implicit_in1', build.implicit_ins[0])
        self.assertEqual('implicit_in2', build.implicit_ins[1])
        self.assertEqual('order_only1', build.prerequisites[0])
        self.assertEqual('order_only2', build.prerequisites[1])

        self.assertEqual(('t', '1',), build.bindings['a'])
        self.assertEqual(('t', '2',), build.bindings['b'])

    def test_default_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'default.ninja')

        parser = ninja.Parser()
        manifest = parser.parse(input_path, ENCODING)

        self.assertEqual(1, len(manifest.defaults))

        default = manifest.defaults[0]
        self.assertEqual('foo.o', default.outs[0])
        self.assertEqual('bar.o', default.outs[1])

    def test_pool_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'pool.ninja')

        parser = ninja.Parser()
        manifest = parser.parse(input_path, ENCODING)

        self.assertEqual(1, len(manifest.pools))

        pool = manifest.pools[0]
        self.assertEqual('example', pool.name)
        self.assertEqual(('t', '5',), pool.bindings['depth'])

    def test_subninja_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'subninja.ninja')

        parser = MockedParser(TEST_DATA_DIR)
        manifest = parser.parse(input_path, ENCODING)

        env = parser.mocked_env[0]
        self.assertEqual('original', env['a'])
        self.assertEqual(2, len(manifest.builds))

        env = parser.mocked_env[1]
        self.assertEqual('changed', env['a'])

    def test_include_stmt(self):
        input_path = os.path.join(TEST_DATA_DIR, 'include.ninja')

        parser = MockedParser(TEST_DATA_DIR)
        manifest = parser.parse(input_path, ENCODING)

        env = parser.mocked_env[0]
        self.assertEqual('changed', env['a'])
        self.assertEqual(2, len(manifest.builds))


class ParserTestWithBadInput(unittest.TestCase):
    def test_unexpected_trivial_token(self):
        input_path = os.path.join(TEST_DATA_DIR, 'bad_trivial.ninja')
        with self.assertRaises(ninja.ParseError) as ctx:
            MockedParser().parse(input_path, ENCODING)

        self.assertEqual(input_path, ctx.exception.path)
        self.assertEqual(1, ctx.exception.line)
        self.assertEqual(1, ctx.exception.column)

    def test_unexpected_non_trivial_token(self):
        input_path = os.path.join(TEST_DATA_DIR, 'bad_non_trivial.ninja')
        with self.assertRaises(ninja.ParseError) as ctx:
            MockedParser().parse(input_path, ENCODING)

        self.assertEqual(input_path, ctx.exception.path)
        self.assertEqual(1, ctx.exception.line)
        self.assertEqual(1, ctx.exception.column)

    def test_bad_after_good(self):
        input_path = os.path.join(TEST_DATA_DIR, 'bad_after_good.ninja')
        with self.assertRaises(ninja.ParseError) as ctx:
            MockedParser().parse(input_path, ENCODING)

        self.assertEqual(input_path, ctx.exception.path)
        self.assertEqual(4, ctx.exception.line)
        self.assertEqual(1, ctx.exception.column)

    def test_bad_path(self):
        input_path = os.path.join(TEST_DATA_DIR, 'bad_path.ninja')
        with self.assertRaises(ninja.ParseError) as ctx:
            MockedParser().parse(input_path, ENCODING)

        self.assertEqual(input_path, ctx.exception.path)
        self.assertEqual(1, ctx.exception.line)
        self.assertEqual(9, ctx.exception.column)


if __name__ == '__main__':
    unittest.main()
