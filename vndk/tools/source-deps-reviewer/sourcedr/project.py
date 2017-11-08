#!/usr/bin/env python3

"""SourceDR project configurations and databases.

`Project` class holds configuration files, review databases, pattern databases,
and `codesearch` index files.
"""

import collections
import json
import os
import shutil

from sourcedr.codesearch import CodeSearch, PathFilter
from sourcedr.pattern_db import PatternDB
from sourcedr.review_db import ReviewDB
from sourcedr.utils import LockedFile


class Config(object):
    """SourceDR project configuration file."""

    DEFAULT_NAME = 'sourcedr.json'

    _PATH_TRAVERSAL_ATTRS = (
            'file_ext_blacklist', 'file_name_blacklist',
            'path_component_blacklist')


    @classmethod
    def get_default_path(cls, project_dir):
        """Get the default path of the configuration file under a project
        directory."""
        return os.path.join(project_dir, cls.DEFAULT_NAME)


    def __init__(self, path):
        self.path = path

        self.source_dir = None
        self.file_ext_blacklist = set()
        self.file_name_blacklist = set()
        self.path_component_blacklist = set()


    def load(self):
        """Load the project configuration from the JSON file."""
        with open(self.path, 'r') as config_fp:
            config_json = json.load(config_fp)
            for key, value in config_json.items():
                if key == 'source_dir':
                    self.source_dir = value
                elif key in self._PATH_TRAVERSAL_ATTRS:
                    setattr(self, key, set(value))
                else:
                    raise ValueError('unknown config name: ' + key)


    def save(self):
        """Save the project configuration to the JSON file."""
        with LockedFile(self.path, 'x') as config_fp:
            config = collections.OrderedDict()
            config['source_dir'] = self.source_dir
            for key in self._PATH_TRAVERSAL_ATTRS:
                config[key] = sorted(getattr(self, key))
            json.dump(config, config_fp, indent=2)


class Project(object):
    """SourceDR project configuration files and databases."""

    def __init__(self, project_dir):
        """Load a project from a given project directory."""

        project_dir = os.path.abspath(project_dir)
        self.project_dir = project_dir

        if not os.path.isdir(project_dir):
            raise ValueError('project directory not found: ' + project_dir)

        # Load configuration files
        config_path = Config.get_default_path(project_dir)
        self.config = Config(config_path)
        self.config.load()

        # Recalculate source directory
        self.source_dir = os.path.abspath(
                os.path.join(project_dir, self.config.source_dir))

        # csearchindex file
        path_filter = PathFilter(self.config.file_ext_blacklist,
                                 self.config.file_name_blacklist,
                                 self.config.path_component_blacklist)
        csearch_index_path = CodeSearch.get_default_path(project_dir)
        self.codesearch = CodeSearch(self.source_dir, csearch_index_path,
                                     path_filter)
        self.codesearch.add_default_filters()

        # Review database file
        review_db_path = ReviewDB.get_default_path(project_dir)
        self.review_db = ReviewDB(review_db_path, self.codesearch)

        # Pattern database file
        pattern_db_path = PatternDB.get_default_path(project_dir)
        self.pattern_db = PatternDB(pattern_db_path)

        # Sanity checks
        self._check_source_dir()
        self._check_lock_files()


    def update_csearch_index(self, remove_existing_index):
        """Create or update codesearch index."""
        self.codesearch.build_index(remove_existing_index)


    def update_review_db(self):
        """Update the entries in the review database."""
        patterns, is_regexs = self.pattern_db.load()
        self.review_db.find(patterns, is_regexs)


    def _check_source_dir(self):
        """Check the availability of the source directory."""
        if not os.path.isdir(self.source_dir):
            raise ValueError('source directory not found: ' + self.source_dir)


    def _check_lock_files(self):
        """Check whether there are some lock files."""
        for path in (self.config.path, self.review_db.path,
                     self.pattern_db.path):
            if LockedFile.is_locked(path):
                raise ValueError('file locked: ' + path)


    @classmethod
    def create_project_dir(cls, project_dir, source_dir):
        """Create a directory for a new project and setup default
        configurations."""

        if not os.path.isdir(source_dir):
            raise ValueError('source directory not found: ' + source_dir)

        os.makedirs(project_dir, exist_ok=True)

        # Compute the relative path between project_dir and source_dir
        project_dir = os.path.abspath(project_dir)
        source_dir = os.path.relpath(os.path.abspath(source_dir), project_dir)

        # Copy default files
        defaults_dir = os.path.join(os.path.dirname(__file__), 'defaults')
        for name in (Config.DEFAULT_NAME, PatternDB.DEFAULT_NAME):
            shutil.copyfile(os.path.join(defaults_dir, name),
                            os.path.join(project_dir, name))

        # Update the source directory in the configuration file
        config_path = Config.get_default_path(project_dir)
        config = Config(config_path)
        config.load()
        config.source_dir = source_dir
        config.save()

        return Project(project_dir)


    @classmethod
    def get_or_create_project_dir(cls, project_dir, source_dir):
        config_file_path = Config.get_default_path(project_dir)
        if os.path.exists(config_file_path):
            return Project(project_dir)
        else:
            return cls.create_project_dir(project_dir, source_dir)
