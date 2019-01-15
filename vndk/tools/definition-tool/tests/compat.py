#!/usr/bin/env python3

# pylint: disable=unused-import,import-error

import sys


try:
    from tempfile import TemporaryDirectory
except ImportError:
    import shutil
    import tempfile


    class TemporaryDirectory(object):
        def __init__(self, suffix='', prefix='tmp', dir=None):
            # pylint: disable=redefined-builtin
            self.name = tempfile.mkdtemp(suffix, prefix, dir)


        def __del__(self):
            self.cleanup()


        def __enter__(self):
            return self.name


        def __exit__(self, exc, value, tb):
            self.cleanup()


        def cleanup(self):
            if self.name:
                shutil.rmtree(self.name)
                self.name = None


if sys.version_info >= (3, 0):
    from os import makedirs
else:
    import os


    def makedirs(path, exist_ok):
        if exist_ok and os.path.exists(path):
            return
        os.makedirs(path)


if sys.version_info >= (3, 0):
    from io import StringIO
else:
    from StringIO import StringIO


try:
    from unittest.mock import patch
except ImportError:
    import contextlib


    @contextlib.contextmanager
    def patch(target, mock):
        obj, attr = target.rsplit('.')
        obj = __import__(obj)
        original_value = getattr(obj, attr)
        setattr(obj, attr, mock)
        try:
            yield
        finally:
            setattr(obj, attr, original_value)


if sys.version_info >= (3, 2):
    from unittest import TestCase
else:
    import unittest


    class TestCase(unittest.TestCase):
        def assertRegex(self, text, expected_regex, msg=None):
            # pylint: disable=deprecated-method
            self.assertRegexpMatches(text, expected_regex, msg)


        def assertNotRegex(self, text, unexpected_regex, msg=None):
            # pylint: disable=deprecated-method
            self.assertNotRegexpMatches(text, unexpected_regex, msg)
