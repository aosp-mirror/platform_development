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

import gdbclient
import unittest
import copy
import json

from typing import Any

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


if __name__ == '__main__':
    unittest.main(verbosity=2)
