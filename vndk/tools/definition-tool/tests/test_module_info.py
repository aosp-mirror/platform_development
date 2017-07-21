#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from vndk_definition_tool import ModuleInfo

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


class ModuleInfoTest(unittest.TestCase):
    def test_default(self):
        m = ModuleInfo.load_from_path_or_default(None)
        self.assertEqual([], m.get_module_path('/system/lib64/libA.so'))

    def test_get_module_path(self):
        json_path = os.path.join(SCRIPT_DIR, 'testdata', 'test_module_info',
                                 'module-info.json')
        m = ModuleInfo.load_from_path_or_default(json_path)

        self.assertEqual(['system/core/libA'],
                         m.get_module_path('/system/lib64/libA.so'))
        self.assertEqual(['frameworks/base/libB'],
                         m.get_module_path('/system/lib64/libB.so'))
        self.assertEqual(['frameworks/base/libC'],
                         m.get_module_path('/system/lib64/libC.so'))
        self.assertEqual(['frameworks/base/libC'],
                         m.get_module_path('/system/lib64/hw/libC.so'))

        self.assertEqual(
                [], m.get_module_path('/system/lib64/libdoes_not_exist.so'))


if __name__ == '__main__':
    unittest.main()
