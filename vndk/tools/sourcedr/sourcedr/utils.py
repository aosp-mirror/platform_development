#!/usr/bin/env python3

"""Utility functions or classes."""

import os


class LockedFile(object):  # pylint: disable=too-few-public-methods
    """Open a file with `.lock` file and rename it if everything goes well."""


    def __init__(self, path, mode):
        assert 'x' in mode
        self._path = path
        self._mode = mode
        self._fp = None


    def __enter__(self):
        """Open the file at the specified path and with specified mode."""
        self._fp = open(self._get_locked_path(self._path), self._mode)
        return self._fp


    def __exit__(self, exc_type, exc_val, exc_tb):
        """Close the file object and rename the file if there are no
        exceptions."""
        self._fp.close()
        self._fp = None
        if exc_val is None:
            os.rename(self._get_locked_path(self._path), self._path)


    @classmethod
    def _get_locked_path(cls, path):
        """Get the file path for the `.lock` file."""
        return path + '.lock'


    @classmethod
    def is_locked(cls, path):
        """Check whether a path is locked."""
        return os.path.exists(cls._get_locked_path(path))
