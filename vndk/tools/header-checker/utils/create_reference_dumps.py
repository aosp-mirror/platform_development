#!/usr/bin/env python3

import argparse
import os
import time

from utils import (
    AOSP_DIR, SOURCE_ABI_DUMP_EXT_END, SO_EXT, Target,
    copy_reference_dump, find_lib_lsdumps, get_build_vars_for_product,
    make_libraries, make_tree, read_lsdump_paths)


PRODUCTS_DEFAULT = ['aosp_arm', 'aosp_arm64', 'aosp_x86', 'aosp_x86_64']

PREBUILTS_ABI_DUMPS_DIR = os.path.join(AOSP_DIR, 'prebuilts', 'abi-dumps')
PREBUILTS_ABI_DUMPS_SUBDIRS = ('ndk', 'platform', 'vndk')
NON_AOSP_TAGS = {'VENDOR', 'PRODUCT', 'VNDK-ext', 'VNDK-SP-ext'}

SOONG_DIR = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')


class GetRefDumpDirStem:
    def __init__(self, ref_dump_dir):
        self.ref_dump_dir = ref_dump_dir

    def __call__(self, subdir, arch):
        return os.path.join(self.ref_dump_dir, arch)


class GetVersionedRefDumpDirStem:
    def __init__(self, chosen_vndk_version, chosen_platform_version,
                 binder_bitness):
        self.chosen_vndk_version = chosen_vndk_version
        self.chosen_platform_version = chosen_platform_version
        self.binder_bitness = binder_bitness

    def __call__(self, subdir, arch):
        if subdir not in PREBUILTS_ABI_DUMPS_SUBDIRS:
            raise ValueError(f'"{subdir}" is not a valid dump directory under '
                             f'{PREBUILTS_ABI_DUMPS_DIR}.')
        version_stem = (self.chosen_vndk_version
                        if subdir == 'vndk'
                        else self.chosen_platform_version)
        return os.path.join(PREBUILTS_ABI_DUMPS_DIR, subdir, version_stem,
                            self.binder_bitness, arch)


def make_libs_for_product(libs, product, variant, vndk_version, targets,
                          exclude_tags):
    print('making libs for', product + '-' + variant)
    if libs:
        make_libraries(product, variant, vndk_version, targets, libs,
                       exclude_tags)
    else:
        make_tree(product, variant)


def tag_to_dir_name(tag):
    if tag in NON_AOSP_TAGS:
        return ''
    if tag == 'NDK':
        return 'ndk'
    if tag in ('PLATFORM', 'LLNDK'):
        return 'platform'
    if tag.startswith('VNDK'):
        return 'vndk'
    raise ValueError(tag + ' is not a known tag.')


def find_and_copy_lib_lsdumps(get_ref_dump_dir_stem, target, libs,
                              lsdump_paths):
    arch_lsdump_paths = find_lib_lsdumps(lsdump_paths, libs, target)
    num_created = 0
    for tag, path in arch_lsdump_paths:
        ref_dump_dir_stem = get_ref_dump_dir_stem(tag_to_dir_name(tag),
                                                  target.get_arch_str())
        copy_reference_dump(
            path, os.path.join(ref_dump_dir_stem, 'source-based'))
        num_created += 1
    return num_created


def create_source_abi_reference_dumps(args, get_ref_dump_dir_stem,
                                      lsdump_paths, targets):
    num_libs_copied = 0
    for target in targets:
        assert target.primary_arch != ''
        print(f'Creating dumps for arch: {target.arch}, '
              f'primary arch: {target.primary_arch}')

        num_libs_copied += find_and_copy_lib_lsdumps(
            get_ref_dump_dir_stem, target, args.libs, lsdump_paths)
    return num_libs_copied


def create_source_abi_reference_dumps_for_all_products(args):
    """Create reference ABI dumps for all specified products."""

    num_processed = 0

    for product in args.products:
        build_vars = get_build_vars_for_product(
            ['PLATFORM_VNDK_VERSION', 'BOARD_VNDK_VERSION', 'BINDER32BIT',
             'PLATFORM_VERSION_CODENAME', 'PLATFORM_SDK_VERSION'],
            product, args.build_variant)

        platform_vndk_version = build_vars[0]
        board_vndk_version = build_vars[1]
        platform_version_codename = build_vars[3]
        platform_sdk_version = build_vars[4]
        if build_vars[2] == 'true':
            binder_bitness = '32'
        else:
            binder_bitness = '64'

        # This logic must be in sync with the logic for reference ABI dumps
        # directory in `build/soong/cc/library.go`.
        # chosen_vndk_version is either the codename or the finalized
        # PLATFORM_SDK_VERSION.
        chosen_vndk_version = (platform_vndk_version
                               if board_vndk_version in ('current', '')
                               else board_vndk_version)
        # chosen_platform_version is expected to be the finalized
        # PLATFORM_SDK_VERSION if the codename is REL.
        chosen_platform_version = (platform_sdk_version
                                   if platform_version_codename == 'REL'
                                   else 'current')

        targets = [t for t in (Target(True, product), Target(False, product))
                   if t.arch]

        if args.ref_dump_dir:
            get_ref_dump_dir_stem = GetRefDumpDirStem(args.ref_dump_dir)
            exclude_tags = ()
        else:
            get_ref_dump_dir_stem = GetVersionedRefDumpDirStem(
                chosen_vndk_version,
                chosen_platform_version,
                binder_bitness)
            exclude_tags = NON_AOSP_TAGS

        try:
            if not args.no_make_lib:
                # Build .lsdump for all the specified libs, or build
                # `findlsdumps` if no libs are specified.
                make_libs_for_product(args.libs, product, args.build_variant,
                                      platform_vndk_version, targets,
                                      exclude_tags)

            lsdump_paths = read_lsdump_paths(product, args.build_variant,
                                             platform_vndk_version, targets,
                                             exclude_tags, build=False)

            num_processed += create_source_abi_reference_dumps(
                args, get_ref_dump_dir_stem, lsdump_paths, targets)
        except KeyError as e:
            if args.libs or not args.ref_dump_dir:
                raise RuntimeError('Please check the lib name or specify '
                                   '-ref-dump-dir if you are updating '
                                   'reference dumps for product or vendor '
                                   'libraries.') from e
            raise

    return num_processed


def _parse_args():
    """Parse the command line arguments."""

    parser = argparse.ArgumentParser()
    parser.add_argument('--version', help=argparse.SUPPRESS)
    parser.add_argument('--no-make-lib', action='store_true',
                        help='skip building dumps while creating references')
    parser.add_argument('-libs', action='append',
                        help='libs to create references for')
    parser.add_argument('-products', action='append',
                        help='products to create references for')
    parser.add_argument('--build-variant', default='userdebug',
                        help='build variant to create references for')
    parser.add_argument('--compress', action='store_true',
                        help=argparse.SUPPRESS)
    parser.add_argument('-ref-dump-dir',
                        help='directory to copy reference abi dumps into')

    args = parser.parse_args()

    if args.version is not None:
        parser.error('--version is deprecated. Please specify the version in '
                     'the reference dump directory path. e.g., '
                     '-ref-dump-dir prebuilts/abi-dumps/platform/current/64')

    if args.compress:
        parser.error("Compressed reference dumps are deprecated.")

    if args.libs:
        if any(lib_name.endswith(SOURCE_ABI_DUMP_EXT_END) or
               lib_name.endswith(SO_EXT) for lib_name in args.libs):
            parser.error('-libs should be followed by a base name without '
                         'file extension.')

    if args.ref_dump_dir and not args.libs:
        parser.error('-libs must be given if -ref-dump-dir is given.')

    if args.products is None:
        # If `args.products` is unspecified, generate reference ABI dumps for
        # all products.
        args.products = PRODUCTS_DEFAULT

    return args


def main():
    args = _parse_args()

    # Clear SKIP_ABI_CHECKS as it forbids ABI dumps from being built.
    os.environ.pop('SKIP_ABI_CHECKS', None)

    start = time.time()
    num_processed = create_source_abi_reference_dumps_for_all_products(args)
    end = time.time()

    print()
    print('msg: Processed', num_processed, 'libraries in ', (end - start) / 60,
          ' minutes')


if __name__ == '__main__':
    main()
