#!/usr/bin/env python3

import argparse
import os
import time

from utils import (
    AOSP_DIR, SOURCE_ABI_DUMP_EXT_END, SO_EXT, BuildTarget, Arch,
    copy_reference_dump, find_lib_lsdumps, get_build_vars,
    make_libraries, make_targets, read_lsdump_paths)


PRODUCTS_DEFAULT = ['aosp_arm', 'aosp_arm64', 'aosp_x86', 'aosp_x86_64']

PREBUILTS_ABI_DUMPS_DIR = os.path.join(AOSP_DIR, 'prebuilts', 'abi-dumps')
PREBUILTS_ABI_DUMPS_SUBDIRS = ('ndk', 'platform', 'vndk')
KNOWN_TAGS = {'APEX', 'LLNDK', 'NDK', 'PLATFORM', 'VENDOR', 'PRODUCT'}
NON_AOSP_TAGS = {'VENDOR', 'PRODUCT'}

SOONG_DIR = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')


class GetRefDumpDirStem:
    def __init__(self, ref_dump_dir):
        self.ref_dump_dir = ref_dump_dir

    def __call__(self, subdir, arch_str):
        return os.path.join(self.ref_dump_dir, arch_str)


class GetVersionedRefDumpDirStem:
    def __init__(self, board_api_level, chosen_platform_version,
                 binder_bitness):
        self.board_api_level = board_api_level
        self.chosen_platform_version = chosen_platform_version
        self.binder_bitness = binder_bitness

    def __call__(self, subdir, arch_str):
        if subdir not in PREBUILTS_ABI_DUMPS_SUBDIRS:
            raise ValueError(f'"{subdir}" is not a valid dump directory under '
                             f'{PREBUILTS_ABI_DUMPS_DIR}.')
        version_stem = (self.board_api_level if subdir == 'vndk' else
                        self.chosen_platform_version)
        return os.path.join(PREBUILTS_ABI_DUMPS_DIR, subdir, version_stem,
                            self.binder_bitness, arch_str)


class LsdumpFilter:
    def __init__(self, include_names, include_tags, exclude_tags):
        self.include_names = include_names
        self.include_tags = include_tags
        self.exclude_tags = exclude_tags

    def __call__(self, tag, lib_name):
        """Determine whether to dump the library.

        lib_name does not contain '.so'.
        """
        if self.include_names and lib_name not in self.include_names:
            return False
        if self.include_tags and tag not in self.include_tags:
            return False
        if tag in self.exclude_tags:
            return False
        return True


def tag_to_dir_name(tag):
    if tag in NON_AOSP_TAGS:
        return ''
    if tag == 'NDK':
        return 'ndk'
    if tag in ('APEX', 'PLATFORM'):
        return 'platform'
    if tag == 'LLNDK':
        return 'vndk'
    raise ValueError(tag + ' is not a known tag.')


def find_and_copy_lib_lsdumps(get_ref_dump_dir_stem, arch, libs,
                              lsdump_paths):
    arch_lsdump_paths = find_lib_lsdumps(lsdump_paths, libs, arch)
    num_created = 0
    for tag, path in arch_lsdump_paths:
        ref_dump_dir_stem = get_ref_dump_dir_stem(tag_to_dir_name(tag),
                                                  arch.get_arch_str())
        copy_reference_dump(
            path, os.path.join(ref_dump_dir_stem, 'source-based'))
        num_created += 1
    return num_created


def create_source_abi_reference_dumps(args, get_ref_dump_dir_stem,
                                      lsdump_paths, arches):
    num_libs_copied = 0
    for arch in arches:
        assert arch.primary_arch != ''
        print(f'Creating dumps for arch: {arch.arch}, '
              f'primary arch: {arch.primary_arch}')

        num_libs_copied += find_and_copy_lib_lsdumps(
            get_ref_dump_dir_stem, arch, args.libs, lsdump_paths)
    return num_libs_copied


def create_source_abi_reference_dumps_for_all_products(args):
    """Create reference ABI dumps for all specified products."""
    num_processed = 0

    for product in args.products:
        build_target = BuildTarget(product, args.release, args.build_variant)
        (
            release_board_api_level, binder_32_bit,
            platform_version_codename, platform_sdk_version,
        ) = get_build_vars(
            ['RELEASE_BOARD_API_LEVEL', 'BINDER32BIT',
             'PLATFORM_VERSION_CODENAME', 'PLATFORM_SDK_VERSION'],
            build_target
        )
        if binder_32_bit == 'true':
            binder_bitness = '32'
        else:
            binder_bitness = '64'

        # chosen_platform_version is expected to be the finalized
        # PLATFORM_SDK_VERSION if the codename is REL.
        chosen_platform_version = (platform_sdk_version
                                   if platform_version_codename == 'REL'
                                   else 'current')

        arches = [arch for arch in
                  (Arch(True, build_target), Arch(False, build_target))
                  if arch.arch]

        if args.ref_dump_dir:
            get_ref_dump_dir_stem = GetRefDumpDirStem(args.ref_dump_dir)
            exclude_tags = set()
        else:
            get_ref_dump_dir_stem = GetVersionedRefDumpDirStem(
                release_board_api_level, chosen_platform_version,
                binder_bitness)
            exclude_tags = NON_AOSP_TAGS

        lsdump_filter = LsdumpFilter(args.libs, args.include_tags,
                                     exclude_tags)

        try:
            if not args.no_make_lib:
                print('making libs for', '-'.join(filter(None, build_target)))
                if args.libs:
                    make_libraries(build_target, arches, args.libs,
                                   lsdump_filter)
                elif args.include_tags:
                    make_targets(
                        build_target,
                        ['findlsdumps_' + tag for tag in args.include_tags])
                else:
                    make_targets(build_target, ['findlsdumps'])

            lsdump_paths = read_lsdump_paths(build_target, arches,
                                             lsdump_filter, build=False)

            num_processed += create_source_abi_reference_dumps(
                args, get_ref_dump_dir_stem, lsdump_paths, arches)
        except KeyError as e:
            if args.libs or not args.ref_dump_dir:
                raise RuntimeError('Please check the lib name, --lib-variant '
                                   'and -ref-dump-dir if you are updating '
                                   'reference dumps for product or vendor '
                                   'libraries.') from e
            raise

    return num_processed


def _parse_args():
    """Parse the command line arguments."""

    parser = argparse.ArgumentParser()
    parser.add_argument('--no-make-lib', action='store_true',
                        help='skip building dumps while creating references')
    parser.add_argument('--lib', '-libs', action='append',
                        dest='libs', metavar='LIB',
                        help='libs to create references for')
    parser.add_argument('--product', '-products', action='append',
                        dest='products', metavar='PRODUCT',
                        help='products to create references for')
    parser.add_argument('--release', '-release',
                        help='release configuration to create references for. '
                             'e.g., trunk_staging, next.')
    parser.add_argument('--build-variant', default='userdebug',
                        help='build variant to create references for')
    parser.add_argument('--lib-variant', action='append', dest='include_tags',
                        default=[], choices=KNOWN_TAGS,
                        help='library variant to create references for.')
    parser.add_argument('--ref-dump-dir', '-ref-dump-dir',
                        help='directory to copy reference abi dumps into')
    args = parser.parse_args()

    if args.libs:
        if any(lib_name.endswith(SOURCE_ABI_DUMP_EXT_END) or
               lib_name.endswith(SO_EXT) for lib_name in args.libs):
            parser.error('--lib should be followed by a base name without '
                         'file extension.')

    if NON_AOSP_TAGS.intersection(args.include_tags) and not args.libs:
        parser.error('--lib must be given if --lib-variant is any of ' +
                     str(NON_AOSP_TAGS))

    if args.ref_dump_dir and not args.libs:
        parser.error('--lib must be given if --ref-dump-dir is given.')

    if args.ref_dump_dir and len(args.include_tags) != 1:
        print('WARNING: Exactly one --lib-variant should be specified if '
              '--ref-dump-dir is given.')

    if args.products is None:
        # If `args.products` is unspecified, generate reference ABI dumps for
        # all products.
        args.products = PRODUCTS_DEFAULT

    return args


def main():
    args = _parse_args()

    # Clear SKIP_ABI_CHECKS as it forbids ABI dumps from being built.
    os.environ.pop('SKIP_ABI_CHECKS', None)

    if os.environ.get('KEEP_VNDK') == 'true':
        raise RuntimeError('KEEP_VNDK is not supported. Please undefine it.')

    start = time.time()
    num_processed = create_source_abi_reference_dumps_for_all_products(args)
    end = time.time()

    print()
    print('msg: Processed', num_processed, 'libraries in ', (end - start) / 60,
          ' minutes')


if __name__ == '__main__':
    main()
