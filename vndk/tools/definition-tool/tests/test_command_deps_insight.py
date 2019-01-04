#!/usr/bin/env python3

from __future__ import print_function

import unittest

from vndk_definition_tool import (
    DepsInsightCommand, ModuleInfo, PT_SYSTEM, PT_VENDOR)

from .compat import StringIO, patch
from .utils import GraphBuilder


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
        # compute_degenerated_vndk() should not remove bad dependencies from
        # the output of deps-insight.  This test checks the existance of bad
        # dependencies.

        gb = GraphBuilder()

        libsystem = gb.add_lib32(PT_SYSTEM, 'libsystem')

        libsystem2 = gb.add_lib32(
            PT_SYSTEM, 'libsystem2', dt_needed=['libsystem.so'])

        libvndk = gb.add_lib32(
            PT_SYSTEM, 'libvndk', dt_needed=['libvendor_bad.so'],
            extra_dir='vndk')

        libvendor = gb.add_lib32(
            PT_VENDOR, 'libvendor', dt_needed=['libvndk.so'])

        libvendor_bad = gb.add_lib32(
            PT_VENDOR, 'libvendor_bad', extra_dir='vndk')

        gb.resolve()

        with patch('sys.stderr', StringIO()):
            vndk_sets = gb.graph.compute_degenerated_vndk(set(), None)

        self.assertNotIn(libvendor_bad, libvndk.deps_good)

        strs, mods = DepsInsightCommand.serialize_data(
            list(gb.graph.all_libs()), vndk_sets, ModuleInfo())

        # libsystem
        deps = self._get_module_deps(strs, mods, libsystem.path)
        self.assertFalse(deps)
        users = self._get_module_users(strs, mods, libsystem.path)
        self.assertIn(libsystem2.path, users)

        # libsystem2
        deps = self._get_module_deps(strs, mods, libsystem2.path)
        self.assertIn(libsystem.path, deps)
        users = self._get_module_users(strs, mods, libsystem2.path)
        self.assertFalse(users)

        # libvndk
        deps = self._get_module_deps(strs, mods, libvndk.path)
        self.assertIn(libvendor_bad.path, deps)
        users = self._get_module_users(strs, mods, libvndk.path)
        self.assertIn(libvendor.path, users)

        # libvendor
        deps = self._get_module_deps(strs, mods, libvendor.path)
        self.assertIn(libvndk.path, deps)
        users = self._get_module_users(strs, mods, libvendor.path)
        self.assertFalse(users)

        # libvendor_bad
        deps = self._get_module_deps(strs, mods, libvendor_bad.path)
        self.assertFalse(deps)
        users = self._get_module_users(strs, mods, libvendor_bad.path)
        self.assertIn(libvndk.path, users)
