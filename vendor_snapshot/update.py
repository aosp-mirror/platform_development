#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
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
"""Installs vendor snapshot under prebuilts/vendor/v{version}."""

import argparse
import glob
import logging
import os
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
import json

INDENT = ' ' * 4

def get_notice_path(module_name):
    return os.path.join('NOTICE_FILES', module_name+'.txt')

def get_target_arch(json_rel_path):
    return json_rel_path.split('/')[0]

def get_arch(json_rel_path):
    return json_rel_path.split('/')[1].split('-')[1]

def get_variation(json_rel_path):
    return json_rel_path.split('/')[2]

# convert .bp prop dictionary to .bp prop string
def gen_bp_prop(prop, ind):
    bp = ''
    for key in prop:
        val = prop[key]

        # Skip empty list or dict, rather than printing empty prop like
        # "key: []," or "key: {},"
        if type(val) == list or type(val) == dict:
            if len(val) == 0:
                continue

        bp += ind + key + ": "
        if type(val) == bool:
            bp += "true,\n" if val else "false,\n"
        elif type(val) == str:
            bp += '"%s",\n' % val
        elif type(val) == list:
            bp += '[\n'
            for elem in val:
                bp += ind + INDENT + '"%s",\n' % elem
            bp += ind + '],\n'
        elif type(val) == dict:
            bp += '{\n'
            bp += gen_bp_prop(val, ind + INDENT)
            bp += ind + '},\n'
        else:
            raise TypeError('unsupported type %s for gen_bp_prop' % type(val))
    return bp

# Remove non-existent dirs from given list. Emits warning for such dirs.
def remove_invalid_dirs(paths, bp_dir, module_name):
    ret = []
    for path in paths:
        if os.path.isdir(os.path.join(bp_dir, path)):
            ret.append(path)
        else:
            logging.warning(
                'Dir "%s" of module "%s" does not exist' % (path, module_name))
    return ret

JSON_TO_BP = {
    'ModuleName':          'name',
    'RelativeInstallPath': 'relative_install_path',
    'ExportedDirs':        'export_include_dirs',
    'ExportedSystemDirs':  'export_system_include_dirs',
    'ExportedFlags':       'export_flags',
    'SanitizeMinimalDep':  'sanitize_minimal_dep',
    'SanitizeUbsanDep':    'sanitize_ubsan_dep',
    'Symlinks':            'symlinks',
    'InitRc':              'init_rc',
    'VintfFragments':      'vintf_fragments',
    'SharedLibs':          'shared_libs',
    'RuntimeLibs':         'runtime_libs',
    'Required':            'required',
}

# Converts parsed json dictionary (which is intermediate) to Android.bp prop
# dictionary. This validates paths such as include directories and init_rc
# files while converting.
def convert_json_to_bp_prop(json_path, bp_dir):
    prop = json.load(json_path)
    ret = {}

    module_name = prop['ModuleName']
    ret['name'] = module_name

    # Soong will complain about non-existing paths on Android.bp. There might
    # be missing files among generated header files, so check all exported
    # directories and filter out invalid ones. Emits warning for such dirs.
    # TODO: fix soong to track all generated headers correctly
    for key in {'ExportedDirs', 'ExportedSystemDirs'}:
        if key in prop:
            prop[key] = remove_invalid_dirs(prop[key], bp_dir, module_name)

    for key in prop:
        if key in JSON_TO_BP:
            ret[JSON_TO_BP[key]] = prop[key]
        else:
            logging.warning(
                'Unknown prop "%s" of module "%s"' % (key, module_name))

    return ret

def gen_bp_module(variation, name, version, target_arch, arch_props, bp_dir):
    prop = {
        # These three are common for all snapshot modules.
        'version': str(version),
        'target_arch': target_arch,
        'vendor': True,
        'arch': {},
    }

    # Factor out common prop among architectures to minimize Android.bp.
    common_prop = None
    for arch in arch_props:
        if common_prop is None:
            common_prop = dict()
            for k in arch_props[arch]:
                common_prop[k] = arch_props[arch][k]
            continue
        for k in list(common_prop.keys()):
            if not k in arch_props[arch] or common_prop[k] != arch_props[arch][k]:
                del common_prop[k]

    # Forcing src to be arch_props prevents 32-bit only modules to be used as
    # 64-bit modules, and vice versa.
    if 'src' in common_prop:
        del common_prop['src']
    prop.update(common_prop)

    for arch in arch_props:
        for k in common_prop:
            if k in arch_props[arch]:
                del arch_props[arch][k]
        prop['arch'][arch] = arch_props[arch]

    bp = 'vendor_snapshot_%s {\n' % variation
    bp += gen_bp_prop(prop, INDENT)
    bp += '}\n\n'
    return bp

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'snapshot_version',
        type=int,
        help='Vendor snapshot version to install, e.g. "30".')
    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()

def main():
    """Program entry point."""
    args = get_args()
    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = min(args.verbose, 2)
    logging.basicConfig(
        format='%(levelname)-8s [%(filename)s:%(lineno)d] %(message)s',
        level=verbose_map[verbosity])
    install_dir = os.path.join('prebuilts', 'vendor', 'v'+str(args.snapshot_version))

    # props[target_arch]["static"|"shared"|"binary"|"header"][name][arch] : json
    props = dict()

    # {target_arch}/{arch}/{variation}/{module}.json
    for root, _, files in os.walk(install_dir):
        for file_name in sorted(files):
            if not file_name.endswith('.json'):
                continue
            full_path = os.path.join(root, file_name)
            rel_path = os.path.relpath(full_path, install_dir)

            target_arch = get_target_arch(rel_path)
            arch = get_arch(rel_path)
            variation = get_variation(rel_path)
            bp_dir = os.path.join(install_dir, target_arch)

            if not target_arch in props:
                props[target_arch] = dict()
            if not variation in props[target_arch]:
                props[target_arch][variation] = dict()

            with open(full_path, 'r') as f:
                prop = convert_json_to_bp_prop(f, bp_dir)
                # Remove .json after parsing?
                # os.unlink(full_path)

            if variation != 'header':
                prop['src'] = os.path.relpath(
                    rel_path[:-5], # removing .json
                    target_arch)

            module_name = prop['name']
            notice_path = 'NOTICE_FILES/' + module_name + ".txt"
            if os.path.exists(os.path.join(bp_dir, notice_path)):
                prop['notice'] = notice_path

            variation_dict = props[target_arch][variation]
            if not module_name in variation_dict:
                variation_dict[module_name] = dict()
            variation_dict[module_name][arch] = prop

    for target_arch in props:
        androidbp = ''
        bp_dir = os.path.join(install_dir, target_arch)
        for variation in props[target_arch]:
            for name in props[target_arch][variation]:
                androidbp += gen_bp_module(
                    variation,
                    name,
                    args.snapshot_version,
                    target_arch,
                    props[target_arch][variation][name],
                    bp_dir)
        with open(os.path.join(bp_dir, 'Android.bp'), 'w') as f:
            f.write(androidbp)

if __name__ == '__main__':
    main()
