#
# Copyright (C) 2023 The Android Open Source Project
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

import copy
import json
import textwrap
import unittest
from typing import Any

import gdbclient


class LaunchConfigMergeTest(unittest.TestCase):
    def merge_compare(self, base: dict[str, Any], to_add: dict[str, Any] | None, expected: dict[str, Any]) -> None:
        actual = copy.deepcopy(base)
        gdbclient.merge_launch_dict(actual, to_add)
        self.assertEqual(actual, expected, f'base={base}, to_add={to_add}')

    def test_add_none(self) -> None:
        base = { 'foo' : 1 }
        to_add = None
        expected = { 'foo' : 1 }
        self.merge_compare(base, to_add, expected)

    def test_add_val(self) -> None:
        base = { 'foo' : 1 }
        to_add = { 'bar' : 2}
        expected = { 'foo' : 1, 'bar' : 2 }
        self.merge_compare(base, to_add, expected)

    def test_overwrite_val(self) -> None:
        base = { 'foo' : 1 }
        to_add = { 'foo' : 2}
        expected = { 'foo' : 2 }
        self.merge_compare(base, to_add, expected)

    def test_lists_get_appended(self) -> None:
        base = { 'foo' : [1, 2] }
        to_add = { 'foo' : [3, 4]}
        expected = { 'foo' : [1, 2, 3, 4] }
        self.merge_compare(base, to_add, expected)

    def test_add_elem_to_dict(self) -> None:
        base = { 'foo' : { 'bar' : 1 } }
        to_add = { 'foo' : { 'baz' : 2 } }
        expected = { 'foo' : { 'bar' :  1, 'baz' : 2 } }
        self.merge_compare(base, to_add, expected)

    def test_overwrite_elem_in_dict(self) -> None:
        base = { 'foo' : { 'bar' : 1 } }
        to_add = { 'foo' : { 'bar' : 2 } }
        expected = { 'foo' : { 'bar' : 2 } }
        self.merge_compare(base, to_add, expected)

    def test_merging_dict_and_value_raises(self) -> None:
        base = { 'foo' : { 'bar' : 1 } }
        to_add = { 'foo' : 2 }
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)

    def test_merging_value_and_dict_raises(self) -> None:
        base = { 'foo' : 2 }
        to_add = { 'foo' : { 'bar' : 1 } }
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)

    def test_merging_dict_and_list_raises(self) -> None:
        base = { 'foo' : { 'bar' : 1 } }
        to_add = { 'foo' : [1] }
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)

    def test_merging_list_and_dict_raises(self) -> None:
        base = { 'foo' : [1] }
        to_add = { 'foo' : { 'bar' : 1 } }
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)

    def test_adding_elem_to_list_raises(self) -> None:
        base = { 'foo' : [1] }
        to_add = { 'foo' : 2}
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)

    def test_adding_list_to_elem_raises(self) -> None:
        base = { 'foo' : 1 }
        to_add = { 'foo' : [2]}
        with self.assertRaises(ValueError):
            gdbclient.merge_launch_dict(base, to_add)


class VsCodeLaunchGeneratorTest(unittest.TestCase):
    def setUp(self) -> None:
        # These tests can generate long diffs, so we remove the limit
        self.maxDiff = None

    def test_generate_script(self) -> None:
        self.assertEqual(json.loads(gdbclient.generate_vscode_lldb_script(root='/root',
                                                            sysroot='/sysroot',
                                                            binary_name='test',
                                                            port=123,
                                                            solib_search_path=['/path1',
                                                                               '/path2'],
                                                            extra_props=None)),
        {
             'name': '(lldbclient.py) Attach test (port: 123)',
             'type': 'lldb',
             'request': 'custom',
             'relativePathBase': '/root',
             'sourceMap': { '/b/f/w' : '/root', '': '/root', '.': '/root' },
             'initCommands': ['settings append target.exec-search-paths /path1 /path2'],
             'targetCreateCommands': ['target create test',
                                      'target modules search-paths add / /sysroot/'],
             'processCreateCommands': ['gdb-remote 123']
         })

    def test_generate_script_with_extra_props(self) -> None:
        extra_props = {
            'initCommands' : ['settings append target.exec-search-paths /path3'],
            'processCreateCommands' : ['break main', 'continue'],
            'sourceMap' : { '/test/' : '/root/test'},
            'preLaunchTask' : 'Build'
        }
        self.assertEqual(json.loads(gdbclient.generate_vscode_lldb_script(root='/root',
                                                            sysroot='/sysroot',
                                                            binary_name='test',
                                                            port=123,
                                                            solib_search_path=['/path1',
                                                                               '/path2'],
                                                            extra_props=extra_props)),
        {
             'name': '(lldbclient.py) Attach test (port: 123)',
             'type': 'lldb',
             'request': 'custom',
             'relativePathBase': '/root',
             'sourceMap': { '/b/f/w' : '/root',
                           '': '/root',
                           '.': '/root',
                           '/test/' : '/root/test' },
             'initCommands': [
                 'settings append target.exec-search-paths /path1 /path2',
                 'settings append target.exec-search-paths /path3',
             ],
             'targetCreateCommands': ['target create test',
                                      'target modules search-paths add / /sysroot/'],
             'processCreateCommands': ['gdb-remote 123',
                                       'break main',
                                       'continue'],
             'preLaunchTask' : 'Build'
         })


class LaunchConfigInsertTest(unittest.TestCase):
    def setUp(self) -> None:
        # These tests can generate long diffs, so we remove the limit
        self.maxDiff = None

    def test_insert_config(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin
            // #lldbclient-generated-end""")
        to_insert = textwrap.dedent("""\
                                    foo
                                    bar""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            // #lldbclient-generated-begin
                            foo
                            bar
                            // #lldbclient-generated-end"""))

    def test_insert_into_start(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin
            // #lldbclient-generated-end
            more content""")
        to_insert = textwrap.dedent("""\
            foo
            bar""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            // #lldbclient-generated-begin
                            foo
                            bar
                            // #lldbclient-generated-end
                            more content"""))

    def test_insert_into_mid(self) -> None:
        dst = textwrap.dedent("""\
            start content
            // #lldbclient-generated-begin
            // #lldbclient-generated-end
            more content""")
        to_insert = textwrap.dedent("""\
            foo
            bar""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            start content
                            // #lldbclient-generated-begin
                            foo
                            bar
                            // #lldbclient-generated-end
                            more content"""))

    def test_insert_into_end(self) -> None:
        dst = textwrap.dedent("""\
            start content
            // #lldbclient-generated-begin
            // #lldbclient-generated-end""")
        to_insert = textwrap.dedent("""\
            foo
            bar""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            start content
                            // #lldbclient-generated-begin
                            foo
                            bar
                            // #lldbclient-generated-end"""))

    def test_insert_twice(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin
            // #lldbclient-generated-end
            // #lldbclient-generated-begin
            // #lldbclient-generated-end
            """)
        to_insert = 'foo'
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            // #lldbclient-generated-begin
                            foo
                            // #lldbclient-generated-end
                            // #lldbclient-generated-begin
                            foo
                            // #lldbclient-generated-end
                         """))

    def test_preserve_space_indent(self) -> None:
        dst = textwrap.dedent("""\
            {
              "version": "0.2.0",
              "configurations": [
                // #lldbclient-generated-begin
                // #lldbclient-generated-end
              ]
            }
        """)
        to_insert = textwrap.dedent("""\
            {
                "name": "(lldbclient.py) Attach test",
                "type": "lldb",
                "processCreateCommands": [
                    "gdb-remote 123",
                    "test"
                ]
            }""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                             {
                               "version": "0.2.0",
                               "configurations": [
                                 // #lldbclient-generated-begin
                                 {
                                     "name": "(lldbclient.py) Attach test",
                                     "type": "lldb",
                                     "processCreateCommands": [
                                         "gdb-remote 123",
                                         "test"
                                     ]
                                 }
                                 // #lldbclient-generated-end
                               ]
                             }
                         """))

    def test_preserve_tab_indent(self) -> None:
        dst = textwrap.dedent("""\
            {
            \t"version": "0.2.0",
            \t"configurations": [
            \t\t// #lldbclient-generated-begin
            \t\t// #lldbclient-generated-end
            \t]
            }
        """)
        to_insert = textwrap.dedent("""\
            {
            \t"name": "(lldbclient.py) Attach test",
            \t"type": "lldb",
            \t"processCreateCommands": [
            \t\t"gdb-remote 123",
            \t\t"test"
            \t]
            }""")
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            {
                            \t"version": "0.2.0",
                            \t"configurations": [
                            \t\t// #lldbclient-generated-begin
                            \t\t{
                            \t\t\t"name": "(lldbclient.py) Attach test",
                            \t\t\t"type": "lldb",
                            \t\t\t"processCreateCommands": [
                            \t\t\t\t"gdb-remote 123",
                            \t\t\t\t"test"
                            \t\t\t]
                            \t\t}
                            \t\t// #lldbclient-generated-end
                            \t]
                            }
                         """))

    def test_preserve_trailing_whitespace(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin \t
            // #lldbclient-generated-end\t """)
        to_insert = 'foo'
        self.assertEqual(gdbclient.insert_commands_into_vscode_config(dst,
                                                                      to_insert),
                         textwrap.dedent("""\
                            // #lldbclient-generated-begin \t
                            foo
                            // #lldbclient-generated-end\t """))

    def test_fail_if_no_begin(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-end""")
        with self.assertRaisesRegex(ValueError, 'Did not find begin marker line'):
            gdbclient.insert_commands_into_vscode_config(dst, 'foo')

    def test_fail_if_no_end(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin""")
        with self.assertRaisesRegex(ValueError, 'Unterminated begin marker at line 1'):
            gdbclient.insert_commands_into_vscode_config(dst, 'foo')

    def test_fail_if_begin_has_extra_text(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin text
            // #lldbclient-generated-end""")
        with self.assertRaisesRegex(ValueError, 'Did not find begin marker line'):
            gdbclient.insert_commands_into_vscode_config(dst, 'foo')

    def test_fail_if_end_has_extra_text(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-begin
            // #lldbclient-generated-end text""")
        with self.assertRaisesRegex(ValueError, 'Unterminated begin marker at line 1'):
            gdbclient.insert_commands_into_vscode_config(dst, 'foo')

    def test_fail_if_begin_end_swapped(self) -> None:
        dst = textwrap.dedent("""\
            // #lldbclient-generated-end
            // #lldbclient-generated-begin""")
        with self.assertRaisesRegex(ValueError, 'Unterminated begin marker at line 2'):
            gdbclient.insert_commands_into_vscode_config(dst, 'foo')


if __name__ == '__main__':
    unittest.main(verbosity=2)
