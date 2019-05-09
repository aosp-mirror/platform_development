#!/usr/bin/env python3

# This tool updates extracts the information from Android.bp and updates the
# datasets for eligible VNDK libraries.

import argparse
import collections
import csv
import json
import os.path
import posixpath
import re
import sys

def load_make_vars(path):
    result = collections.OrderedDict([
        ('SOONG_LLNDK_LIBRARIES', set()),
        ('SOONG_VNDK_SAMEPROCESS_LIBRARIES', set()),
        ('SOONG_VNDK_CORE_LIBRARIES', set()),
        ('SOONG_VNDK_PRIVATE_LIBRARIES', set()),
    ])

    assign_len = len(' := ')

    with open(path, 'r') as fp:
        for line in fp:
            for key, value in result.items():
                if line.startswith(key):
                    mod_names = line[len(key) + assign_len:].strip().split(' ')
                    value.update(mod_names)

    return result.values()

def load_install_paths(module_info_path):
    with open(module_info_path, 'r') as fp:
        data = json.load(fp)

    result = set()
    name_path_dict = {}
    patt = re.compile(
            '.*[\\\\/]target[\\\\/]product[\\\\/][^\\\\/]+([\\\\/].*)$')
    for name, module in data.items():
        for path in module['installed']:
            match = patt.match(path)
            if not match:
                continue
            path = match.group(1)
            path = path.replace(os.path.sep, '/')
            path = path.replace('/lib/', '/${LIB}/')
            path = path.replace('/lib64/', '/${LIB}/')
            path = re.sub('/vndk-sp(?:-[^/$]*)/', '/vndk-sp${VNDK_VER}/', path)
            path = re.sub('/vndk(?:-[^/$]*)/', '/vndk${VNDK_VER}/', path)
            result.add(path)

            if name.endswith('_32'):
                name = name[0:-3]

            name_path_dict[name] = path

    return (result, name_path_dict)

def _is_stale_module(path, installed_paths):
    if path in installed_paths:
        return False
    # libclang_rt.asan-${arch}-android and
    # libclang_rt.ubsan_standalone-${arch}-android may vary between different
    # architectures.
    if posixpath.basename(path).startswith('libclang_rt'):
        return False
    return True

def remove_stale_modules(data, installed_paths):
    result = {}
    for path, row in data.items():
        if not _is_stale_module(path, installed_paths):
            result[path] = row
    return result

def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('tag_file')
    parser.add_argument('-o', '--output', required=True)
    parser.add_argument('--make-vars', required=True,
                        help='out/soong/make_vars-$(TARGET).mk')
    parser.add_argument('--module-info', required=True,
                        help='out/target/product/$(TARGET)/module-info.json')
    return parser.parse_args()

def main():
    args = _parse_args()

    # Load libraries from `out/soong/make_vars-$(TARGET).mk`.
    llndk, vndk_sp, vndk, vndk_private = load_make_vars(args.make_vars)

    # Load eligible list csv file.
    with open(args.tag_file, 'r') as fp:
        reader = csv.reader(fp)
        header = next(reader)
        data = dict()
        regex_patterns = []
        for path, tag, comments in reader:
            if path.startswith('[regex]'):
                regex_patterns.append([path, tag, comments])
            else:
                data[path] = [path, tag, comments]

    # Delete non-existing libraries.
    installed_paths, name_path_dict = load_install_paths(args.module_info)
    data = remove_stale_modules(data, installed_paths)

    # Reset all /system/${LIB} libraries to FWK-ONLY.
    for path, row in data.items():
        if posixpath.dirname(path) == '/system/${LIB}':
            row[1] = 'FWK-ONLY'

    # Helper function to update the tag and the comments of an entry
    def update_tag(path, tag, comments=None):
        try:
            data[path][1] = tag
            if comments is not None:
                data[path][2] = comments
        except KeyError:
            data[path] = [path, tag, comments if comments is not None else '']

    # Helper function to find the subdir and the module name
    def get_subdir_and_name(name, name_path_dict, prefix_core, prefix_vendor):
        try:
            path = name_path_dict[name + '.vendor']
            assert path.startswith(prefix_vendor)
            name_vendor = path[len(prefix_vendor):]
        except KeyError:
            name_vendor = name + '.so'

        try:
            path = name_path_dict[name]
            assert path.startswith(prefix_core)
            name_core = path[len(prefix_core):]
        except KeyError:
            name_core = name + '.so'

        assert name_core == name_vendor
        return name_core

    # Update LL-NDK tags
    prefix_core = '/system/${LIB}/'
    for name in llndk:
        try:
            path = name_path_dict[name]
            assert path.startswith(prefix_core)
            name = path[len(prefix_core):]
        except KeyError:
            name = name + '.so'
        update_tag('/system/${LIB}/' + name, 'LL-NDK')

    # Update VNDK-SP and VNDK-SP-Private tags
    prefix_core = '/system/${LIB}/'
    prefix_vendor = '/system/${LIB}/vndk-sp${VNDK_VER}/'

    for name in (vndk_sp - vndk_private):
        name = get_subdir_and_name(name, name_path_dict, prefix_core,
                                   prefix_vendor)
        update_tag(prefix_core + name, 'VNDK-SP')
        update_tag(prefix_vendor + name, 'VNDK-SP')

    for name in (vndk_sp & vndk_private):
        name = get_subdir_and_name(name, name_path_dict, prefix_core,
                                   prefix_vendor)
        update_tag(prefix_core + name, 'VNDK-SP-Private')
        update_tag(prefix_vendor + name, 'VNDK-SP-Private')

    # Update VNDK and VNDK-Private tags
    prefix_core = '/system/${LIB}/'
    prefix_vendor = '/system/${LIB}/vndk${VNDK_VER}/'

    for name in (vndk - vndk_private):
        name = get_subdir_and_name(name, name_path_dict, prefix_core,
                                   prefix_vendor)
        update_tag(prefix_core + name, 'VNDK')
        update_tag(prefix_vendor + name, 'VNDK')

    for name in (vndk & vndk_private):
        name = get_subdir_and_name(name, name_path_dict, prefix_core,
                                   prefix_vendor)
        update_tag(prefix_core + name, 'VNDK-Private')
        update_tag(prefix_vendor + name, 'VNDK-Private')

    # Workaround for FWK-ONLY-RS
    libs = [
        'libft2',
        'libmediandk',
    ]
    for name in libs:
        update_tag('/system/${LIB}/' + name + '.so', 'FWK-ONLY-RS')

    # Workaround for LL-NDK APEX bionic
    libs = [
        'libc',
        'libdl',
        'libm',
    ]
    for name in libs:
        update_tag('/apex/com.android.runtime/${LIB}/bionic/' + name + '.so',
                   'LL-NDK')

    # Workaround for LL-NDK-Private
    libs = [
        'ld-android',
        'libc_malloc_debug',
        'libdl_android',
        'libnetd_client',
        'libtextclassifier_hash',
    ]
    for name in libs:
        update_tag('/system/${LIB}/' + name + '.so', 'LL-NDK-Private')

    # Workaround for libclang_rt.*.so
    lib_sets = {
        'LL-NDK': llndk,
        'VNDK': vndk,
    }
    prefixes = {
        'libclang_rt.asan': 'LL-NDK',
        'libclang_rt.hwasan': 'LL-NDK',
        'libclang_rt.scudo': 'VNDK',
        'libclang_rt.ubsan_standalone': 'VNDK',
    }
    for prefix, tag in prefixes.items():
        if any(name.startswith(prefix) for name in lib_sets[tag]):
            for path in list(data.keys()):
                if os.path.basename(path).startswith(prefix):
                    update_tag(path, tag)

    # Merge regular expression patterns into final dataset
    for regex in regex_patterns:
        data[regex[0]] = regex

    # Write updated eligible list file
    with open(args.output, 'w') as fp:
        writer = csv.writer(fp, lineterminator='\n')
        writer.writerow(header)
        writer.writerows(sorted(data.values()))

if __name__ == '__main__':
    sys.exit(main())
