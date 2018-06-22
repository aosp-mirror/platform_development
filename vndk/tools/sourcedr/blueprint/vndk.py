#!/usr/bin/env python3

#
# Copyright (C) 2018 The Android Open Source Project
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

"""This script scans all Android.bp in an android source tree and check the
correctness of dependencies."""

import copy

from blueprint import RecursiveParser, evaluate_defaults, fill_module_namespaces


class Module(object):
    """The class for Blueprint module definition."""

    def __init__(self, rule, attrs):
        """Initialize from a module definition."""
        self.rule = rule
        self._attrs = attrs


    def get_property(self, *names, **kwargs):
        """Get a property in the module definition."""
        try:
            result = self._attrs
            for name in names:
                result = result[name]
            return result
        except KeyError:
            return kwargs.get('default', None)


    def is_vndk(self):
        """Check whether this module is a VNDK shared library."""
        return bool(self.get_property('vndk', 'enabled'))


    def is_vndk_sp(self):
        """Check whether this module is a VNDK-SP shared library."""
        return bool(self.get_property('vndk', 'support_system_process'))


    def is_vendor(self):
        """Check whether this module is a vendor module."""
        return bool(self.get_property('vendor') or
                    self.get_property('proprietary'))


    def is_vendor_available(self):
        """Check whether this module is vendor available."""
        return bool(self.get_property('vendor_available'))


    def has_vendor_variant(self):
        """Check whether the module is VNDK or vendor available."""
        return self.is_vndk() or self.is_vendor_available()


    def get_name(self):
        """Get the module name."""
        return self.get_property('name')


    def get_dependencies(self):
        """Get module dependencies."""

        shared_libs = set(self.get_property('shared_libs', default=[]))
        static_libs = set(self.get_property('static_libs', default=[]))
        header_libs = set(self.get_property('header_libs', default=[]))

        target_vendor = self.get_property('target', 'vendor')
        if target_vendor:
            shared_libs -= set(target_vendor.get('exclude_shared_libs', []))
            static_libs -= set(target_vendor.get('exclude_static_libs', []))
            header_libs -= set(target_vendor.get('exclude_header_libs', []))

        return (sorted(shared_libs), sorted(static_libs), sorted(header_libs))


class ModuleClassifier(object):
    """Dictionaries (all_libs, vndk_libs, vndk_sp_libs,
    vendor_available_libs, and llndk_libs) for modules."""


    def __init__(self):
        self.all_libs = {}
        self.vndk_libs = {}
        self.vndk_sp_libs = {}
        self.vendor_available_libs = {}
        self.llndk_libs = {}


    def add_module(self, name, module):
        """Add a module to one or more dictionaries."""

        # If this is an llndk_library, add the module to llndk_libs and return.
        if module.rule == 'llndk_library':
            if name in self.llndk_libs:
                raise ValueError('lldnk name {!r} conflicts'.format(name))
            self.llndk_libs[name] = module
            return

        # Check the module name uniqueness.
        prev_module = self.all_libs.get(name)
        if prev_module:
            # If there are two modules with the same module name, pick the one
            # without _prebuilt_library_shared.
            if module.rule.endswith('_prebuilt_library_shared'):
                return
            if not prev_module.rule.endswith('_prebuilt_library_shared'):
                raise ValueError('module name {!r} conflicts'.format(name))

        # Add the module to dictionaries.
        self.all_libs[name] = module

        if module.is_vndk():
            self.vndk_libs[name] = module

        if module.is_vndk_sp():
            self.vndk_sp_libs[name] = module

        if module.is_vendor_available():
            self.vendor_available_libs[name] = module


    def _add_modules_from_parsed_pairs(self, parsed_items, namespaces):
        """Add modules from the parsed (rule, attrs) pairs."""

        for rule, attrs in parsed_items:
            name = attrs.get('name')
            if name is None:
                continue

            namespace = attrs.get('_namespace')
            if namespace not in namespaces:
                continue

            if rule == 'llndk_library':
                self.add_module(name, Module(rule, attrs))
            if rule in {'llndk_library', 'ndk_library'}:
                continue

            if rule.endswith('_library') or \
               rule.endswith('_library_shared') or \
               rule.endswith('_library_static') or \
               rule.endswith('_headers'):
                self.add_module(name, Module(rule, attrs))
                continue

            if rule == 'hidl_interface':
                attrs['vendor_available'] = True
                self.add_module(name, Module(rule, attrs))

                adapter_module_name = name + '-adapter-helper'
                adapter_module_dict = copy.deepcopy(attrs)
                adapter_module_dict['name'] = adapter_module_name
                self.add_module(adapter_module_name,
                                Module(rule, adapter_module_dict))
                continue


    def parse_root_bp(self, root_bp_path, namespaces=None):
        """Parse blueprint files and add module definitions."""

        namespaces = {''} if namespaces is None else set(namespaces)

        parser = RecursiveParser()
        parser.parse_file(root_bp_path)
        parsed_items = evaluate_defaults(parser.modules)
        parsed_items = fill_module_namespaces(root_bp_path, parsed_items)

        self._add_modules_from_parsed_pairs(parsed_items, namespaces)


    @classmethod
    def create_from_root_bp(cls, root_bp_path, namespaces=None):
        """Create a ModuleClassifier from a root blueprint file."""
        result = cls()
        result.parse_root_bp(root_bp_path, namespaces)
        return result
