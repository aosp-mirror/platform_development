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

"""List all pre-installed Android Apps with `sharedUserId` in their
`AndroidManifest.xml`."""

import argparse
import collections
import csv
import json
import os
import re
import subprocess
import sys


_SHARED_UID_PATTERN = re.compile('sharedUserId="([^"\\r\\n]*)"')


def load_module_paths(module_json):
    """Load module source paths."""
    result = {}
    with open(module_json, 'r') as json_file:
        modules = json.load(json_file)
    for name, module in modules.items():
        try:
            result[name] = module['path'][0]
        except IndexError:
            continue
    return result


def find_shared_uid(manifest_path):
    """Extract shared UID from AndroidManifest.xml."""
    try:
        with open(manifest_path, 'r') as manifest_file:
            content = manifest_file.read()
    except UnicodeDecodeError:
        return []
    return sorted(_SHARED_UID_PATTERN.findall(content))


def find_file(product_out, app_name):
    """Find the APK file for the app."""
    product_out = os.path.abspath(product_out)
    prefix_len = len(product_out) + 1
    partitions = (
        'data', 'odm', 'oem', 'product', 'system', 'system_ext', 'system_other',
        'vendor',)
    for partition in partitions:
        partition_dir = os.path.join(product_out, partition)
        for base, _, filenames in os.walk(partition_dir):
            for filename in filenames:
                name, ext = os.path.splitext(filename)
                if name == app_name and ext in {'.apk', '.jar'}:
                    return os.path.join(base, filename)[prefix_len:]
    return ''


AppInfo = collections.namedtuple(
    'AppInfo', 'name shared_uid installed_path source_path')


def collect_apps_with_shared_uid(product_out, module_paths):
    """Collect apps with shared UID."""
    apps_dir = os.path.join(product_out, 'obj', 'APPS')
    result = []
    for app_dir_name in os.listdir(apps_dir):
        app_name = re.sub('_intermediates$', '', app_dir_name)
        app_dir = os.path.join(apps_dir, app_dir_name)

        apk_file = os.path.join(app_dir, 'package.apk')
        if not os.path.exists(apk_file):
            print('error: Failed to find:', apk_file, file=sys.stderr)
            continue

        apk_unpacked = os.path.join(app_dir, 'package')
        if not os.path.exists(apk_unpacked):
            ret = subprocess.call(['apktool', 'd', 'package.apk'], cwd=app_dir)
            if ret != 0:
                print('error: Failed to unpack:', apk_file, file=sys.stderr)
                continue

        manifest_file = os.path.join(apk_unpacked, 'AndroidManifest.xml')
        if not os.path.exists(manifest_file):
            print('error: Failed to find:', manifest_file, file=sys.stderr)
            continue

        shared_uid = find_shared_uid(manifest_file)
        if not shared_uid:
            continue

        result.append(AppInfo(
            app_name, shared_uid, find_file(product_out, app_name),
            module_paths.get(app_name, '')))
    return result


def _parse_args():
    """Parse command line options."""
    parser = argparse.ArgumentParser()
    parser.add_argument('product_out')
    parser.add_argument('-o', '--output', required=True)
    return parser.parse_args()


def main():
    """Main function."""
    args = _parse_args()

    module_paths = load_module_paths(
        os.path.join(args.product_out, 'module-info.json'))

    result = collect_apps_with_shared_uid(args.product_out, module_paths)

    def _generate_sort_key(app):
        has_android_uid = any(
            uid.startswith('android.uid') for uid in app.shared_uid)
        return (not has_android_uid, app.installed_path.startswith('system'),
                app.installed_path)

    result.sort(key=_generate_sort_key)

    with open(args.output, 'w') as output_file:
        writer = csv.writer(output_file)
        writer.writerow(('App Name', 'UID', 'Installation Path', 'Source Path'))
        for app in result:
            writer.writerow((app.name, ' '.join(app.shared_uid),
                             app.installed_path, app.source_path))


if __name__ == '__main__':
    main()
