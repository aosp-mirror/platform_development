#!/usr/bin/env python3

import os
import tempfile
import unittest

from sourcedr.project import Config


TESTDATA_DIR = os.path.join(os.path.dirname(__file__), 'testdata')


class ConfigTest(unittest.TestCase):
    PROJECT_DIR = os.path.join(TESTDATA_DIR, 'project')
    CONFIG_PATH = os.path.join(PROJECT_DIR, Config.DEFAULT_NAME)


    def test_load(self):
        config = Config(self.CONFIG_PATH)
        config.load()
        self.assertEqual('path/to/android/src', config.source_dir)


    def test_save(self):
        with tempfile.TemporaryDirectory(prefix='test_sourcedr_') as tmp_dir:
            config_path = Config.get_default_path(tmp_dir)
            config = Config(config_path)
            config.source_dir = 'path/to/android/src'
            config.save()
            with open(config_path, 'r') as actual_fp:
                actual = actual_fp.read().strip()
        with open(self.CONFIG_PATH, 'r') as expected_fp:
            expected = expected_fp.read().strip()
        self.assertEqual(actual, expected)


if __name__ == '__main__':
    unittest.main()
