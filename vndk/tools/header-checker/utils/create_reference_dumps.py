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
                   SOURCE_ABI_DUMP_EXT, SOURCE_ABI_DUMP_EXT_END, SO_EXT,
                   make_targets)

PRODUCTS = ['aosp_arm_ab', 'aosp_arm64_ab', 'aosp_x86_ab', 'aosp_x86_64_ab']
FIND_LSDUMPS_TARGET = 'findlsdumps'
SOONG_DIR = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')

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

def get_lsdump_paths(product, libs):
    if libs is None:
        return get_lsdump_paths_from_out(product)
    return search_for_lsdump_paths(SOONG_DIR, libs)

def get_lsdump_paths_from_out(product):
    build_vars_to_fetch = ['OUT_DIR', 'TARGET_DEVICE']
    build_vars = get_build_vars_for_product(build_vars_to_fetch, product)
    lsdump_paths_file = os.path.join(AOSP_DIR, build_vars[0],'target',
                                     'product', build_vars[1],
                                     'lsdump_paths.txt')
    if os.path.exists(lsdump_paths_file) == False:
        make_targets([FIND_LSDUMPS_TARGET], product)
    lsdump_paths = dict()
    with open(lsdump_paths_file) as f:
        for path in f.read().split(' '):
            add_to_path_dict(path, lsdump_paths)
    return lsdump_paths

def find_and_copy_lib_lsdumps(target, ref_dump_dir_stem, ref_dump_dir_insertion,
                              core_or_vendor_shared_str, libs, lsdump_paths):
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

def choose_vndk_version(args_version, platform_vndk_version,
                        board_vndk_version):
    version = args_version
    if version is None:
        # This logic is to be kept in sync with the references directory logic
        # in build/soong/library.go .
        version = platform_vndk_version
        if board_vndk_version != 'current' and board_vndk_version != '':
            version = board_vndk_version
    return version

def get_ref_dump_dir_stem(args, vndk_or_ndk, product, chosen_vndk_version):
    binder_bitness = '64'
    if get_build_vars_for_product(['BINDER32BIT'], product)[0] == 'true':
        binder_bitness = '32'
    ref_dump_dir_stem = os.path.join(args.ref_dump_dir, vndk_or_ndk)
    ref_dump_dir_stem = os.path.join(ref_dump_dir_stem, chosen_vndk_version)
    ref_dump_dir_stem = os.path.join(ref_dump_dir_stem, binder_bitness)

    return ref_dump_dir_stem

def make_libs_for_all_arches_and_variants(libs, llndk_mode):
    for product in PRODUCTS:
        if libs:
            print('making libs for product:', product)
            make_libraries(libs, product, llndk_mode)
        else:
            print('making all libs for product: ', product)
            make_tree(product)

def find_and_remove_path(root_path, chosen_vndk_version, file_name=None):
    if file_name is not None:
        print('removing', file_name, 'from root', root_path)
        remove_cmd_str = 'find ' + root_path + ' -name ' + file_name +\
            ' -exec rm -rf {} \;'
        subprocess.check_call(remove_cmd_str, cwd=AOSP_DIR, shell=True)
    else:
        remove_cmd_str = 'rm -rf ' + chosen_vndk_version
        subprocess.check_call(remove_cmd_str, cwd=root_path, shell=True)

def remove_references_for_all_arches_and_variants(args, chosen_vndk_version):
    print('Removing reference dumps...')
    libs = args.libs
    for product in PRODUCTS:
        if libs:
            for lib in libs:
                find_and_remove_path(args.ref_dump_dir, chosen_vndk_version,
                                     lib + COMPRESSED_SOURCE_ABI_DUMP_EXT)
        else:
            find_and_remove_path(os.path.join(args.ref_dump_dir, 'ndk'),
                                 chosen_vndk_version)
            find_and_remove_path(os.path.join(args.ref_dump_dir, 'vndk'),
                                 chosen_vndk_version)

def add_to_path_dict(path, dictionary, libs=[]):
    name, lsdump_ext = os.path.splitext(path)
    sofile, so_ext = os.path.splitext(name)
    libname = os.path.basename(sofile)
    if lsdump_ext == SOURCE_ABI_DUMP_EXT_END and so_ext == SO_EXT:
        if libs and (libname not in libs):
            return
        if libname not in dictionary.keys():
            dictionary[libname] = [path]
        else:
            dictionary[libname].append(path)

def search_for_lsdump_paths(soong_dir, libs):
    lsdump_paths = dict()
    for root, dirs, files in os.walk(soong_dir):
        for file in files:
          add_to_path_dict(os.path.join(root, file), lsdump_paths, libs)
    return lsdump_paths

def create_source_abi_reference_dumps(args, product,
                                      chosen_vndk_version, lsdump_paths):
    ref_dump_dir_stem_vndk =\
        get_ref_dump_dir_stem(args, 'vndk', product, chosen_vndk_version)
    ref_dump_dir_stem_ndk =\
        get_ref_dump_dir_stem(args, 'ndk', product, chosen_vndk_version)
    ref_dump_dir_insertion = 'source-based'
    num_libs_copied = 0
    for target in [Target(True, product), Target(False, product)]:
        if target.arch ==  '' or target.arch_variant == '':
            continue
        print('Creating dumps for target_arch:', target.arch, 'and variant ',
              target.arch_variant)
        assert(target.primary_arch != '')
        num_libs_copied += find_and_copy_lib_lsdumps(
            target, ref_dump_dir_stem_vndk, ref_dump_dir_insertion,
            '_vendor_shared/', args.libs, lsdump_paths)

        num_libs_copied += find_and_copy_lib_lsdumps(
            target, ref_dump_dir_stem_ndk, ref_dump_dir_insertion,
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
    parser.add_argument('--llndk', help='The libs specified by -l are llndk',
                        default=False, action='store_true')
    parser.add_argument('-libs', help='libs to create references for',
                        action='append')
    parser.add_argument('-ref-dump-dir',
                        help='directory to copy reference abi dumps into',
                        default=os.path.join(AOSP_DIR,'prebuilts/abi-dumps'))
    args = parser.parse_args()
    num_processed = 0
    # Remove reference dumps specified by libs / all of them if none specified,
    # so that we may build those libraries succesfully.
    vndk_versions = get_build_vars_for_product(['PLATFORM_VNDK_VERSION',
                                                'BOARD_VNDK_VERSION'])
    platform_vndk_version = vndk_versions[0]
    board_vndk_version = vndk_versions[1]
    chosen_vndk_version = \
        choose_vndk_version(args.version, platform_vndk_version,
                           board_vndk_version)
    remove_references_for_all_arches_and_variants(args, chosen_vndk_version)
    # make all the libs specified / the 'vndk' target if none specified
    if (args.no_make_lib == False):
        make_libs_for_all_arches_and_variants(args.libs, args.llndk)
    for product in PRODUCTS:
        lsdump_paths = get_lsdump_paths(product, args.libs)
        num_processed += create_source_abi_reference_dumps(
            args, product, chosen_vndk_version, lsdump_paths)
    print()
    end = time.time()
    print('msg: Processed', num_processed, 'libraries in ', (end - start) / 60,
          ' minutes')
if __name__ == '__main__':
    main()
