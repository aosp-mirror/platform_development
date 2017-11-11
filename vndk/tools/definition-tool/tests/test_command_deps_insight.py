#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
import unittest

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from compat import StringIO, patch
from vndk_definition_tool import (
    DepsInsightCommand, ModuleInfo, PT_SYSTEM, PT_VENDOR)
from utils import GraphBuilder


class DepsInsightCommandTest(unittest.TestCase):
    _PATH_FIELD = 0
    _ELF_CLASS_FIELD = 1
    _TAGS_FIELD = 2
    _DEPS_FIELD = 3
    _USERS_FIELD = 4
    _SOURCE_DIRS_FIELD = 5


    @classmethod
    def _get_module(cls, strs, mods, path):
        for mod in mods:
            if strs[mod[cls._PATH_FIELD]] == path:
                return mod
        return None


    @classmethod
    def _get_module_deps(cls, strs, mods, path):
        mod = cls._get_module(strs, mods, path)
        result = set()
        for deps in mod[cls._DEPS_FIELD]:
            result.update(strs[mods[x][cls._PATH_FIELD]] for x in deps)
        return result


    @classmethod
    def _get_module_users(cls, strs, mods, path):
        mod = cls._get_module(strs, mods, path)
        users = mod[cls._USERS_FIELD]
        return set(strs[mods[x][cls._PATH_FIELD]] for x in users)


    def test_serialize_data_with_all_deps(self):
        """compute_degenerated_vndk() should not remove bad dependencies from
        the output of deps-insight.  This test checks the existance of bad
        dependencies."""

        gb = GraphBuilder()
        libfwk = gb.add_lib32(PT_SYSTEM, 'libfwk')
        libvndk = gb.add_lib32(PT_SYSTEM, 'libvndk',
                               dt_needed=['libvnd_bad.so'], extra_dir='vndk')
        libvndk_sp = gb.add_lib32(PT_SYSTEM, 'libutils',
                                  dt_needed=['libvnd_bad.so'],
                                  extra_dir='vndk-sp')
        libvnd = gb.add_lib32(PT_VENDOR, 'libvnd',
                              dt_needed=['libvndk.so', 'libutils.so'])
        libvnd_bad = gb.add_lib32(PT_VENDOR, 'libvnd_bad', extra_dir='vndk-sp')
        gb.resolve()

        with patch('sys.stderr', StringIO()):
            vndk_sets = gb.graph.compute_degenerated_vndk(set(), None)

        self.assertNotIn(libvnd_bad, libvndk.deps_good)
        self.assertNotIn(libvnd_bad, libvndk_sp.deps_good)

        strs, mods = DepsInsightCommand.serialize_data(
                list(gb.graph.all_libs()), vndk_sets, ModuleInfo())

        deps = self._get_module_deps(strs, mods, libvndk.path)
        self.assertIn(libvnd_bad.path, deps)

        deps = self._get_module_deps(strs, mods, libvndk_sp.path)
        self.assertIn(libvnd_bad.path, deps)

        users = self._get_module_users(strs, mods, libvnd_bad.path)
        self.assertIn(libvndk.path, users)
        self.assertIn(libvndk_sp.path, users)


if __name__ == '__main__':
    unittest.main()
