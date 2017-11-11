#!/usr/bin/env python3

import os
import sys

try:
    from tempfile import TemporaryDirectory
except ImportError:
    import shutil
    import tempfile

    class TemporaryDirectory(object):
        def __init__(self, suffix='', prefix='tmp', dir=None):
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
    def makedirs(path, exist_ok):
        if exist_ok and os.path.exists(path):
            return
        return os.makedirs(path)

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
