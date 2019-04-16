#!/usr/bin/env python3

import argparse
import collections
import os
import shutil
import time

from utils import (
    AOSP_DIR, COMPRESSED_SOURCE_ABI_DUMP_EXT, SOURCE_ABI_DUMP_EXT,
    SOURCE_ABI_DUMP_EXT_END, SO_EXT, copy_reference_dumps, find_lib_lsdumps,
    get_build_vars_for_product, get_module_variant_dir_name, make_libraries,
    make_tree, read_lsdump_paths)


PRODUCTS_DEFAULT = ['aosp_arm_ab', 'aosp_arm', 'aosp_arm64', 'aosp_x86_ab',
                    'aosp_x86', 'aosp_x86_64']

PREBUILTS_ABI_DUMPS_DEFAULT = os.path.join(AOSP_DIR, 'prebuilts', 'abi-dumps')

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


def get_lib_arch_str(target):
    assert target.primary_arch != ''
    target_arch_variant_str = ''
    # If TARGET_ARCH == TARGET_ARCH_VARIANT, soong makes targetArchVariant
    # empty. This is the case for aosp_x86_64 and aosp_x86_ab.
    if target.arch_variant != target.arch:
        target_arch_variant_str = '_' + target.arch_variant
    return target.arch + target_arch_variant_str


def find_and_copy_lib_lsdumps(target, ref_dump_dir_stem, ref_dump_dir_insertion,
                              core_or_vendor_shared_str, libs, lsdump_paths,
                              compress):
    module_variant_dir_name = get_module_variant_dir_name(
        target.arch, target.arch_variant, target.cpu_variant,
        core_or_vendor_shared_str)

    arch_lsdump_paths = find_lib_lsdumps(
        module_variant_dir_name, lsdump_paths, libs)

    # Copy the contents of the lsdump into their corresponding reference ABI
    # dumps directories.
    return copy_reference_dumps(arch_lsdump_paths, ref_dump_dir_stem,
                                ref_dump_dir_insertion,
                                get_lib_arch_str(target), compress)


def choose_vndk_version(version, platform_vndk_version, board_vndk_version):
    if version is None:
        # This logic must be in sync with the logic for reference ABI dumps
        # directory in `build/soong/cc/library.go`.
        version = platform_vndk_version
        if board_vndk_version not in ('current', ''):
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


def make_libs_for_product(libs, llndk_mode, product, variant, targets):
    print('making libs for', product + '-' + variant)
    if libs:
        make_libraries(product, variant, targets, libs, llndk_mode)
    else:
        make_tree(product, variant)


def find_and_remove_path(root_path, file_name=None):
    if file_name is not None:
        root_path = os.path.join(root_path, 'source-based', file_name)

    if os.path.exists(root_path):
        print('removing', root_path)
        if os.path.isfile(root_path):
            os.remove(root_path)
        else:
            shutil.rmtree(root_path)


def remove_references_for_all_arches_and_variants(args, product, targets,
                                                  chosen_vndk_version):
    libs = args.libs
    for target in targets:
        if target.arch == '' or target.arch_variant == '':
            continue

        dir_to_remove_vndk = os.path.join(
            get_ref_dump_dir_stem(args, 'vndk', product, chosen_vndk_version),
            get_lib_arch_str(target))

        dir_to_remove_ndk = os.path.join(
            get_ref_dump_dir_stem(args, 'ndk', product, chosen_vndk_version),
            get_lib_arch_str(target))

        if libs:
            for lib in libs:
                find_and_remove_path(dir_to_remove_vndk,
                                     lib + SOURCE_ABI_DUMP_EXT)
                find_and_remove_path(dir_to_remove_vndk,
                                     lib + COMPRESSED_SOURCE_ABI_DUMP_EXT)
                find_and_remove_path(dir_to_remove_ndk,
                                     lib + SOURCE_ABI_DUMP_EXT)
                find_and_remove_path(dir_to_remove_ndk,
                                     lib + COMPRESSED_SOURCE_ABI_DUMP_EXT)
        else:
            find_and_remove_path(dir_to_remove_vndk)
            find_and_remove_path(dir_to_remove_ndk)


def add_to_path_dict(path, dictionary, libs=tuple()):
    name, lsdump_ext = os.path.splitext(path)
    sofile, so_ext = os.path.splitext(name)
    libname = os.path.basename(sofile)
    if lsdump_ext == SOURCE_ABI_DUMP_EXT_END and so_ext == SO_EXT:
        if libs and libname not in libs:
            return
        dictionary[libname].append(path)


def create_source_abi_reference_dumps(args, product,
                                      chosen_vndk_version, lsdump_paths,
                                      targets):
    ref_dump_dir_stem_vndk = \
        get_ref_dump_dir_stem(args, 'vndk', product, chosen_vndk_version)
    ref_dump_dir_stem_ndk = \
        get_ref_dump_dir_stem(args, 'ndk', product, chosen_vndk_version)
    ref_dump_dir_insertion = 'source-based'

    num_libs_copied = 0

    for target in targets:
        if target.arch == '' or target.arch_variant == '':
            continue

        print('Creating dumps for target_arch:', target.arch, 'and variant ',
              target.arch_variant)
        assert target.primary_arch != ''

        num_libs_copied += find_and_copy_lib_lsdumps(
            target, ref_dump_dir_stem_vndk, ref_dump_dir_insertion,
            '_vendor_shared', args.libs, lsdump_paths, args.compress)

        num_libs_copied += find_and_copy_lib_lsdumps(
            target, ref_dump_dir_stem_ndk, ref_dump_dir_insertion,
            '_core_shared', args.libs, lsdump_paths, args.compress)

    return num_libs_copied


def create_source_abi_reference_dumps_for_all_products(args):
    """Create reference ABI dumps for all specified products."""

    platform_vndk_version, board_vndk_version = get_build_vars_for_product(
        ['PLATFORM_VNDK_VERSION', 'BOARD_VNDK_VERSION'])
    chosen_vndk_version = choose_vndk_version(
        args.version, platform_vndk_version, board_vndk_version)

    num_processed = 0

    for product in args.products:
        targets = [Target(True, product), Target(False, product)]

        # Remove reference ABI dumps specified in `args.libs` (or remove all of
        # them if none of them are specified) so that we may build these
        # libraries successfully.
        remove_references_for_all_arches_and_variants(
            args, product, targets, chosen_vndk_version)

        if not args.no_make_lib:
            # Build all the specified libs (or build the 'vndk' target if none
            # of them are specified.)
            make_libs_for_product(args.libs, args.llndk, product,
                                  args.build_variant, targets)

        lsdump_paths = read_lsdump_paths(product, args.build_variant, targets,
                                         build=False)

        num_processed += create_source_abi_reference_dumps(
            args, product, chosen_vndk_version, lsdump_paths, targets)

    return num_processed


def _parse_args():
    """Parse the command line arguments."""

    parser = argparse.ArgumentParser()
    parser.add_argument('--version', help='VNDK version')
    parser.add_argument('--no-make-lib', action='store_true',
                        help='no m -j lib.vendor while creating reference')
    parser.add_argument('--llndk', action='store_true',
                        help='The libs specified by -l are llndk')
    parser.add_argument('-libs', action='append',
                        help='libs to create references for')
    parser.add_argument('-products', action='append',
                        help='products to create references for')
    parser.add_argument('--build-variant', default='userdebug',
                        help='build variant to create references for')
    parser.add_argument('--compress', action='store_true',
                        help='compress reference dump with gzip')
    parser.add_argument('-ref-dump-dir',
                        help='directory to copy reference abi dumps into',
                        default=PREBUILTS_ABI_DUMPS_DEFAULT)

    args = parser.parse_args()

    if args.products is None:
        # If `args.products` is unspecified, generate reference ABI dumps for
        # all products.
        args.products = PRODUCTS_DEFAULT

    return args


def main():
    args = _parse_args()

    start = time.time()
    num_processed = create_source_abi_reference_dumps_for_all_products(args)
    end = time.time()

    print()
    print('msg: Processed', num_processed, 'libraries in ', (end - start) / 60,
          ' minutes')


if __name__ == '__main__':
    main()
