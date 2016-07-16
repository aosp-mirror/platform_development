#!/usr/bin/env python
#
# Copyright (C) 2016 The Android Open Source Project
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
"""Clobbers all the per-API level headers with the current unified headers.

This is just for testing purposes. Having unified headers isn't worth as much
if we're still shipping N copies of them :)
"""
from __future__ import absolute_import
from __future__ import print_function

import logging
import os
import shutil


THIS_DIR = os.path.realpath(os.path.dirname(__file__))
ANDROID_ROOT = os.path.dirname(os.path.dirname(THIS_DIR))


ALL_ARCHITECTURES = (
    'arm',
    'arm64',
    'mips',
    'mips64',
    'x86',
    'x86_64',
)


def logger():
    """Return the main logger for this module."""
    return logging.getLogger(__name__)


def android_path(*args):
    """Returns a full path from the base of the Android source tree."""
    return os.path.join(ANDROID_ROOT, *args)


def copy_directory_contents(src, dst):
    """Copies the contents of a directory, merging with the destination.

    shutil.copytree requires that the destination does not exist. This function
    behaves like `cp -r`. That is, it merges the source and destination
    directories if appropriate.
    """
    for root, dirs, files in os.walk(src):
        subdir = os.path.relpath(root, src)
        dst_dir = os.path.join(dst, subdir)
        if not os.path.exists(dst_dir):
            os.makedirs(dst_dir)

        # This makes sure we copy even empty directories. We don't actually
        # need it, but for now it lets us diff between our result and the
        # legacy tool.
        for d in dirs:  # pylint: disable=invalid-name
            d_path = os.path.join(root, d)
            if os.path.islink(d_path):
                linkto = os.readlink(d_path)
                dst_file = os.path.join(dst_dir, d)
                logger().debug('Symlinking %s to %s', dst_file, linkto)
                os.symlink(linkto, dst_file)
            else:
                new_dir = os.path.join(dst_dir, d)
                if not os.path.exists(new_dir):
                    logger().debug('Making directory %s', new_dir)
                    os.makedirs(new_dir)

        for f in files:  # pylint: disable=invalid-name
            src_file = os.path.join(root, f)
            if os.path.islink(src_file):
                linkto = os.readlink(src_file)
                dst_file = os.path.join(dst_dir, f)
                logger().debug('Symlinking %s to %s', dst_file, linkto)
                os.symlink(linkto, dst_file)
            else:
                logger().debug('Copying %s', src_file)
                shutil.copy2(src_file, dst_dir)


def get_preserve_path(platform_dir):
    """Returns the path used for preserving headers."""
    return os.path.join(platform_dir, 'preserve')


def preserve_headers(keep_list, platform_dir):
    """Preserves a list of headers to be restored after the copy."""
    install_path = os.path.join(platform_dir, 'include')
    preserve_root = get_preserve_path(platform_dir)
    if os.path.exists(preserve_root):
        shutil.rmtree(preserve_root)
    for preserve in keep_list:
        path = os.path.join(install_path, preserve)
        if os.path.isdir(path):
            shutil.copytree(path, os.path.join(preserve_root, preserve))
        elif os.path.isfile(path):
            shutil.copy2(path, preserve_root)


def restore_headers(keep_list, platform_dir):
    """Restores headers preserved by preserve_headers."""
    install_path = os.path.join(platform_dir, 'include')
    preserve_root = get_preserve_path(platform_dir)
    for preserve in keep_list:
        path = os.path.join(preserve_root, preserve)
        if os.path.isdir(path):
            # Bionic does have include/android, so we need to merge directories
            # here.
            copy_directory_contents(
                path, os.path.join(install_path, preserve))
        elif os.path.isfile(path):
            shutil.copy2(path, install_path)


def platform_path_to_api_level(platform_path):
    """Returns the API level for a platform path."""
    basename = os.path.split(platform_path)[-1]
    # android-\d+
    return int(basename.split('-')[1])


def fixup_api_level_h(platform_dir):
    """Rewrites api-level.h for the correct platform."""
    api_level = platform_path_to_api_level(platform_dir)
    header = os.path.join(platform_dir, 'include/android/api-level.h')
    with open(header, 'w') as header_file:
        header_file.write(
            '#ifndef ANDROID_API_LEVEL_H\n'
            '#define ANDROID_API_LEVEL_H\n'
            '#ifndef __ANDROID_API__\n'
            '#define __ANDROID_API__ {}\n'
            '#endif /* __ANDROID_API__ */\n'
            '#endif /* ANDROID_API_LEVEL_H */\n'.format(api_level))


def copy_current_headers(platform_dir):
    """Copies the unified headers into a per-API directory."""
    libc_path = android_path('bionic/libc')
    install_path = os.path.join(platform_dir, 'include')

    # None of the platform APIs have unified headers yet. Copy those out of the
    # way of the purge and copy them back in afterward.
    keep_list = (
        'EGL',
        'GLES',
        'GLES2',
        'GLES3',
        'KHR',
        'OMXAL',
        'SLES',
        'android',
        'camera',
        'jni.h',
        'media',
        'vulkan',
        'zlib.h',
        'zconf.h',
    )
    preserve_headers(keep_list, platform_dir)

    if os.path.exists(install_path):
        shutil.rmtree(install_path)

    shutil.copytree(os.path.join(libc_path, 'include'), install_path)
    shutil.copytree(os.path.join(libc_path, 'kernel/uapi/linux'),
                    os.path.join(install_path, 'linux'))
    shutil.copytree(os.path.join(libc_path, 'kernel/uapi/asm-generic'),
                    os.path.join(install_path, 'asm-generic'))
    shutil.copy2(os.path.join(libc_path, 'NOTICE'), install_path)

    restore_headers(keep_list, platform_dir)

    if os.path.exists(get_preserve_path(platform_dir)):
        shutil.rmtree(get_preserve_path(platform_dir))

    fixup_api_level_h(platform_dir)

    api_level = platform_path_to_api_level(platform_dir)
    for arch in ALL_ARCHITECTURES:
        if arch in ('arm64', 'mips64', 'x86_64') and api_level < 21:
            continue

        # For multilib architectures we need to copy the 32-bit headers into
        # the 64-bit directory. For MIPS this is true of asm and machine, for
        # Intel it's only for asm.
        asm_arch = arch
        machine_arch = arch
        if arch == 'mips64':
            asm_arch = 'mips'
            machine_arch = 'mips'
        elif arch == 'x86_64':
            asm_arch = 'x86'

        arch_install_path = os.path.join(
            platform_dir, 'arch-' + arch, 'include')
        if os.path.exists(arch_install_path):
            shutil.rmtree(arch_install_path)

        machine_path = os.path.join(
            libc_path, 'arch-' + machine_arch, 'include/machine')
        asm_path = os.path.join(
            libc_path, 'kernel/uapi/asm-' + asm_arch, 'asm')

        if os.path.exists(arch_install_path):
            shutil.rmtree(arch_install_path)
        os.makedirs(arch_install_path)

        shutil.copytree(
            machine_path, os.path.join(arch_install_path, 'machine'))
        shutil.copytree(asm_path, os.path.join(arch_install_path, 'asm'))


def main():
    """Program entry point."""
    platforms_dir = os.path.join(THIS_DIR, 'platforms')
    for platform_dir in os.listdir(platforms_dir):
        path = os.path.join(platforms_dir, platform_dir)
        if not os.path.isdir(path):
            continue
        if platform_dir == 'common':
            # Just contains crtbrand.
            continue
        copy_current_headers(path)


if __name__ == '__main__':
    main()
