#!/usr/bin/env python

from __future__ import print_function

import argparse
import collections
import os
import re
import subprocess
import sys

def detect_ndk_dir():
    ndk_dir = os.getenv('NDK')
    if not ndk_dir:
        error_msg = '''error: NDK toolchain is required for this test case.
error:
error: Steps:
error:   1. Download NDK from https://developer.android.com/ndk/downloads/
error:   2. Unzip the archive (android-ndk-r15c-linux-x86_64.zip)
error:   3. Set environment variable NDK to the extracted directory
error:      (export NDK=android-ndk-r15c)
error:'''
        print(error_msg, file=sys.stderr)
        raise ValueError('NDK toolchain not specified')

    if not os.path.exists(ndk_dir):
        raise ValueError('NDK toolchain not found')

    return ndk_dir

def detect_api_level(ndk_dir):
    try:
        apis = []
        pattern = re.compile('android-(\\d+)')
        for name in os.listdir(os.path.join(ndk_dir, 'platforms')):
            match = pattern.match(name)
            if match:
                apis.append(int(match.group(1)))
        if not apis:
            raise ValueError('failed to find latest api')
        return 'android-{}'.format(max(apis))
    except IOError:
        raise ValueError('failed to find latest api')

def detect_host():
    if sys.platform.startswith('linux'):
        return 'linux-x86_64'
    if sys.platform.startswith('darwin'):
        return 'darwin-x86_64'
    raise NotImplementedError('unknown host platform')

def get_gcc_dir(ndk_dir, arch, host):
    return os.path.join(ndk_dir, 'toolchains', arch, 'prebuilt', host)

def get_clang_dir(ndk_dir, host):
    return os.path.join(ndk_dir, 'toolchains', 'llvm', 'prebuilt', host)

def get_platform_dir(ndk_dir, api, subdirs):
    return os.path.join(ndk_dir, 'platforms', api, *subdirs)

class Target(object):
    def __init__(self, name, triple, cflags, ldflags, gcc_toolchain_dir,
                 clang_dir, ndk_include, ndk_lib):
        self.name = name
        self.target_triple = triple
        self.target_cflags = cflags
        self.target_ldflags = ldflags

        self.gcc_toolchain_dir = gcc_toolchain_dir
        self.clang_dir = clang_dir
        self.ndk_include = ndk_include
        self.ndk_lib = ndk_lib

    def check_paths(self):
        def check_path(path):
            if os.path.exists(path):
                return True
            print('error: File not found:', path, file=sys.stderr)
            return False

        ld_exeutable = os.path.join(
                self.gcc_toolchain_dir, 'bin', self.target_triple + '-ld')

        success = check_path(self.gcc_toolchain_dir)
        success &= check_path(ld_exeutable)
        success &= check_path(self.clang_dir)
        success &= check_path(self.ndk_include)
        success &= check_path(self.ndk_lib)
        return success

    def compile(self, obj_file, src_file, cflags):
        clang = os.path.join(self.clang_dir, 'bin', 'clang')

        cmd = [clang, '-o', obj_file, '-c', src_file]
        cmd.extend(['-fPIE', '-fPIC'])
        cmd.extend(['-gcc-toolchain', self.gcc_toolchain_dir])
        cmd.extend(['-target', self.target_triple])
        cmd.extend(['-isystem', self.ndk_include])
        cmd.extend(cflags)
        cmd.extend(self.target_cflags)
        subprocess.check_call(cmd)

    def link(self, out_file, obj_files, ldflags):
        if '-shared' in ldflags:
            crtbegin = os.path.join(self.ndk_lib, 'crtbegin_so.o')
            crtend = os.path.join(self.ndk_lib, 'crtend_so.o')
        else:
            crtbegin = os.path.join(self.ndk_lib, 'crtbegin_static.o')
            crtend = os.path.join(self.ndk_lib, 'crtend_android.o')

        clang = os.path.join(self.clang_dir, 'bin', 'clang')

        cmd = [clang, '-o', out_file]
        cmd.extend(['-fPIE', '-fPIC', '-Wl,--no-undefined', '-nostdlib'])
        cmd.append('-L' + self.ndk_lib)
        cmd.extend(['-gcc-toolchain', self.gcc_toolchain_dir])
        cmd.extend(['-target', self.target_triple])
        cmd.append(crtbegin)
        cmd.extend(obj_files)
        cmd.append(crtend)
        cmd.extend(ldflags)
        cmd.extend(self.target_ldflags)
        if '-shared' not in ldflags:
            cmd.append('-Wl,-pie')
        subprocess.check_call(cmd)

def create_targets(ndk_dir=None, api=None, host=None):
    if ndk_dir is None:
        ndk_dir = detect_ndk_dir()
    if api is None:
        api = detect_api_level(ndk_dir)
    if host is None:
        host = detect_host()

    targets = collections.OrderedDict()

    targets['arm'] = Target(
            'arm', 'arm-linux-androideabi', [],[],
            get_gcc_dir(ndk_dir, 'arm-linux-androideabi-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-arm', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-arm', 'usr', 'lib']))

    targets['arm64'] = Target(
            'arm64', 'aarch64-linux-android', [], [],
            get_gcc_dir(ndk_dir, 'aarch64-linux-android-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-arm64', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-arm64', 'usr', 'lib']))

    targets['x86'] = Target(
            'x86', 'i686-linux-android', ['-m32'], ['-m32'],
            get_gcc_dir(ndk_dir, 'x86-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-x86', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-x86', 'usr', 'lib']))

    targets['x86_64'] = Target(
            'x86_64', 'x86_64-linux-android', ['-m64'], ['-m64'],
            get_gcc_dir(ndk_dir, 'x86_64-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-x86_64', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-x86_64', 'usr', 'lib64']))

    targets['mips'] = Target(
            'mips', 'mipsel-linux-android', [], [],
            get_gcc_dir(ndk_dir, 'mipsel-linux-android-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-mips', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-mips', 'usr', 'lib']))

    targets['mips64'] = Target(
            'mips64', 'mips64el-linux-android',
            ['-march=mips64el', '-mcpu=mips64r6'],
            ['-march=mips64el', '-mcpu=mips64r6'],
            get_gcc_dir(ndk_dir, 'mips64el-linux-android-4.9', host),
            get_clang_dir(ndk_dir, host),
            get_platform_dir(ndk_dir, api, ['arch-mips64', 'usr', 'include']),
            get_platform_dir(ndk_dir, api, ['arch-mips64', 'usr', 'lib64']))

    return targets

def main():
    parser = argparse.ArgumentParser(
            description='Dry-run NDK toolchain detection')
    parser.add_argument('--ndk-dir')
    parser.add_argument('--api-level')
    parser.add_argument('--host')
    args = parser.parse_args()

    targets = create_targets(args.ndk_dir, args.api_level, args.host)

    success = True
    for name, target in targets.items():
        success &= target.check_paths()
    if not success:
        sys.exit(1)

    print('succeed')

if __name__ == '__main__':
    main()
