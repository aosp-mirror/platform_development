#!/usr/bin/env python3

import os
import re
import sys
import subprocess
import argparse
import time

from utils import (make_libraries, make_tree, find_lib_lsdumps,
                   get_build_vars_for_product, AOSP_DIR, read_output_content,
                   copy_reference_dumps, COMPRESSED_SOURCE_ABI_DUMP_EXT,
                   make_targets)

PRODUCTS = ['aosp_arm_ab', 'aosp_arm64_ab', 'aosp_x86_ab', 'aosp_x86_64_ab']
FIND_LSDUMPS_TARGET = 'findlsdumps'

class Target(object):
    def __init__(self, has_2nd, product):
        extra = '_2ND' if has_2nd else ''
        build_vars_to_fetch = ['TARGET_ARCH',
                               'TARGET{}_ARCH'.format(extra),
                               'TARGET{}_ARCH_VARIANT'.format(extra),
                               'TARGET{}_CPU_VARIANT'.format(extra)]
        build_vars = get_build_vars_for_product(build_vars_to_fetch, product)
        self.primary_arch = build_vars[0]
        self.arch = build_vars[1]
        self.arch_variant = build_vars[2]
        self.cpu_variant = build_vars[3]

def get_lsdump_paths_file(product):
    build_vars_to_fetch = ['OUT_DIR', 'TARGET_DEVICE']
    build_vars = get_build_vars_for_product(build_vars_to_fetch, product)
    lsdump_paths_file = os.path.join(AOSP_DIR, build_vars[0],'target',
                                     'product', build_vars[1],
                                     'lsdump_paths.txt')
    if os.path.exists(lsdump_paths_file) == False:
        make_targets([FIND_LSDUMPS_TARGET], product)
    return lsdump_paths_file

def get_lsdump_paths_from_out(file_path):
    with open(file_path) as f:
        return f.read().split(' ')

def find_and_copy_lib_lsdumps(target, soong_dir, ref_dump_dir_stem,
                              ref_dump_dir_insertion,
                              core_or_vendor_shared_str,
                              libs, lsdump_paths):
    assert(target.primary_arch != '')
    target_arch_variant_str = ''
    # if TARGET_ARCH == TARGET_ARCH_VARIANT, soong makes targetArchVariant empty
    # this is the case for aosp_x86_64_ab and aosp_x86
    if target.arch_variant != target.arch:
        target_arch_variant_str = '_' + target.arch_variant

    arch_lsdump_paths = find_lib_lsdumps(target.arch, target.arch_variant,
                                         target.cpu_variant, lsdump_paths,
                                         core_or_vendor_shared_str,
                                         libs)
    # Copy the contents of the lsdump into it's corresponding
    # reference  directory.
    return copy_reference_dumps(arch_lsdump_paths, ref_dump_dir_stem,
                                ref_dump_dir_insertion,
                                target.arch + target_arch_variant_str)

def get_ref_dump_dir_stem(args, vndk_or_ndk, product, platform_vndk_version):
    version = args.version
    if version is None:
      version = platform_vndk_version
    if version != '' and version[0].isdigit() == False :
        version = 'current'
    primary_arch =\
        get_build_vars_for_product(['TARGET_ARCH'], product)[0]
    ref_dump_dir_stem = os.path.join(args.ref_dump_dir, vndk_or_ndk)
    ref_dump_dir_stem = os.path.join(ref_dump_dir_stem, version)
    ref_dump_dir_stem = os.path.join(ref_dump_dir_stem, primary_arch)

    return ref_dump_dir_stem

def make_libs_for_all_arches_and_variants(libs):
    for product in PRODUCTS:
        get_lsdump_paths_file(product)
        if libs:
            print('making libs for product:', product)
            make_libraries(libs, product)
        else:
            print('making all libs for product: ', product)
            make_tree(product)

def find_and_remove_path(root_path, file_name=None):
    if file_name is not None:
        print('removing', file_name, 'from root', root_path)
        remove_cmd_str = 'find ' + root_path + ' -name ' + file_name +\
            ' -exec rm -rf {} \;'
        subprocess.check_call(remove_cmd_str, cwd=AOSP_DIR, shell=True)
    else:
        remove_cmd_str = 'rm -rf ' + root_path
        subprocess.check_call(remove_cmd_str, cwd=AOSP_DIR, shell=True)

def remove_references_for_all_arches_and_variants(args):
    print('Removing reference dumps...')
    libs = args.libs
    for product in PRODUCTS:
        if libs:
            for lib in libs:
                find_and_remove_path(args.ref_dump_dir,
                                     lib + COMPRESSED_SOURCE_ABI_DUMP_EXT)
        else:
            find_and_remove_path(os.path.join(args.ref_dump_dir, 'ndk'))
            find_and_remove_path(os.path.join(args.ref_dump_dir, 'vndk'))


def create_source_abi_reference_dumps(soong_dir, args, product,
                                      platform_vndk_version):
    ref_dump_dir_stem_vndk =\
        get_ref_dump_dir_stem(args, 'vndk', product, platform_vndk_version)
    ref_dump_dir_stem_ndk =\
        get_ref_dump_dir_stem(args, 'ndk', product, platform_vndk_version)
    ref_dump_dir_insertion = 'source-based'
    num_libs_copied = 0
    lsdump_paths_file = get_lsdump_paths_file(product)
    lsdump_paths = get_lsdump_paths_from_out(lsdump_paths_file)
    for target in [Target(True, product), Target(False, product)]:
        if target.arch ==  '' or target.arch_variant == '':
            continue
        print('Creating dumps for target_arch:', target.arch, 'and variant ',
              target.arch_variant)
        assert(target.primary_arch != '')
        num_libs_copied += find_and_copy_lib_lsdumps(
            target, soong_dir, ref_dump_dir_stem_vndk, ref_dump_dir_insertion,
            '_vendor_shared/', args.libs, lsdump_paths)

        num_libs_copied += find_and_copy_lib_lsdumps(
            target, soong_dir, ref_dump_dir_stem_ndk, ref_dump_dir_insertion,
            '_core_shared/', args.libs, lsdump_paths)

    return num_libs_copied


def main():
    # Parse command line options.
    assert 'ANDROID_BUILD_TOP' in os.environ
    start = time.time()
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', help='VNDK version')
    parser.add_argument('--no-make-lib', help='no m -j lib.vendor while \
                        creating reference', default=False, action='store_true')
    parser.add_argument('-libs', help='libs to create references for',
                        action='append')
    parser.add_argument('-ref-dump-dir',
                        help='directory to copy reference abi dumps into',
                        default=os.path.join(AOSP_DIR,'prebuilts/abi-dumps'))
    args = parser.parse_args()
    num_processed = 0
    soong_dir = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')
    # Remove reference dumps specified by libs / all of them if none specified,
    # so that we may build those libraries succesfully.
    remove_references_for_all_arches_and_variants(args)
    # make all the libs specified / the entire vndk_package if none specified
    if (args.no_make_lib == False):
        make_libs_for_all_arches_and_variants(args.libs)

    platform_vndk_version =\
        get_build_vars_for_product(['PLATFORM_VNDK_VERSION'])[0]
    for product in PRODUCTS:
        num_processed += create_source_abi_reference_dumps(
            soong_dir, args, product, platform_vndk_version)
    print()
    end = time.time()
    print('msg: Processed', num_processed, 'libraries in ', (end - start) / 60)
if __name__ == '__main__':
    main()
