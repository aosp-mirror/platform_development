#!/usr/bin/env python3
#
# Copyright (C) 2017 The Android Open Source Project
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

import argparse
import collections
import difflib
import os
import subprocess
import sys
import tempfile

"""Test vndk vtable dumper"""

NDK_VERSION = 'r11'
API_LEVEL = 'android-24'

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
VNDK_VTABLE_DUMPER = 'vndk-vtable-dumper'

def get_dirnames(path, n):
    """Get directory, n directories before path"""
    for i in range(n):
        path = os.path.dirname(path)
    return path


def get_prebuilts_host():
    """Get the host dir for prebuilts"""
    if sys.platform.startswith('linux'):
        return 'linux-x86'
    if sys.platform.startswith('darwin'):
        return 'darwin-x86'
    raise NotImplementedError('unknown platform')


def get_prebuilts_gcc(android_build_top, arch, gcc_version):
    """Get the path to gcc for the current platform"""
    return os.path.join(android_build_top, 'prebuilts', 'gcc',
                        get_prebuilts_host(), arch, gcc_version)

def get_prebuilts_clang(android_build_top):
    """Get the path to prebuilt gcc for the current platform"""
    return os.path.join(android_build_top, 'prebuilts', 'clang', 'host',
                        get_prebuilts_host(), 'clang-stable')

def get_prebuilts_ndk(android_build_top, subdirs):
    """Get the path to prebuilt ndk  for the current platform and API level"""
    return os.path.join(android_build_top, 'prebuilts', 'ndk', NDK_VERSION,
                        'platforms', API_LEVEL, *subdirs)

def run_cmd(cmd, verbose=False):
    """Run the command given and print the command if verbose is True"""
    if verbose:
        print('RUN:', ' '.join(cmd), file=sys.stderr)
    subprocess.check_call(cmd)


def run_output(cmd, verbose=False):
    """Run the command given and print output of the command"""
    if verbose:
        print('RUN:', ' '.join(cmd), file=sys.stderr)
    return subprocess.check_output(cmd, universal_newlines=True)


def run_vtable_dump(path, verbose=False):
    """Run vndk vtable dumper"""
    return run_output([VNDK_VTABLE_DUMPER, path], verbose)


class Target(object):
    """Class representing a target: for eg: x86, arm64 etc"""
    def __init__(self, name, triple, cflags, ldflags, gcc_toolchain_dir,
                 clang_dir, ndk_include, ndk_lib):
        """Parameterized Constructor"""
        self.name = name
        self.target_triple = triple
        self.target_cflags = cflags
        self.target_ldflags = ldflags

        self.gcc_toolchain_dir = gcc_toolchain_dir
        self.clang_dir = clang_dir
        self.ndk_include = ndk_include
        self.ndk_lib = ndk_lib

    def compile(self, obj_file, src_file, cflags, verbose=False):
        """Compiles the given source files and produces a .o at obj_file"""
        clangpp = os.path.join(self.clang_dir, 'bin', 'clang++')

        cmd = [clangpp, '-o', obj_file, '-c', src_file]
        cmd.extend(['-fPIE', '-fPIC', '-fno-rtti', '-std=c++11'])
        cmd.extend(['-gcc-toolchain', self.gcc_toolchain_dir])
        cmd.extend(['-target', self.target_triple])
        cmd.extend(['-isystem', self.ndk_include])
        cmd.extend(cflags)
        cmd.extend(self.target_cflags)
        run_cmd(cmd, verbose)

    def link(self, out_file, obj_files, ldflags, verbose=False):
        """Link the given obj files to form a shared library"""
        crtbegin = os.path.join(self.ndk_lib, 'crtbegin_so.o')
        crtend = os.path.join(self.ndk_lib, 'crtend_so.o')
        clangpp = os.path.join(self.clang_dir, 'bin', 'clang++')

        cmd = [clangpp, '-o', out_file]
        cmd.extend(['-fPIE', '-fPIC', '-fno-rtti', '-Wl,--no-undefined', '-nostdlib'])
        cmd.append('-L' + self.ndk_lib)
        cmd.extend(['-gcc-toolchain', self.gcc_toolchain_dir])
        cmd.extend(['-target', self.target_triple])
        cmd.append(crtbegin)
        cmd.extend(obj_files)
        cmd.append(crtend)
        cmd.extend(ldflags)
        cmd.extend(self.target_ldflags)
        run_cmd(cmd, verbose)


def create_targets(top):
    """Create multiple targets objects, one for each architecture supported"""
    return [
        Target('arm', 'arm-linux-androideabi', [],[],
               get_prebuilts_gcc(top, 'arm', 'arm-linux-androideabi-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-arm', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-arm', 'usr', 'lib'])),

        Target('arm64', 'aarch64-linux-android', [], [],
               get_prebuilts_gcc(top, 'aarch64', 'aarch64-linux-android-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-arm64', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-arm64', 'usr', 'lib'])),

        Target('mips', 'mipsel-linux-android', [], [],
               get_prebuilts_gcc(top, 'mips', 'mips64el-linux-android-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-mips', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-mips', 'usr', 'lib'])),

        Target('mips64', 'mips64el-linux-android',
               ['-march=mips64el', '-mcpu=mips64r6'],
               ['-march=mips64el', '-mcpu=mips64r6'],
               get_prebuilts_gcc(top, 'mips', 'mips64el-linux-android-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-mips64', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-mips64', 'usr', 'lib64'])),

        Target('x86', 'x86_64-linux-android', ['-m32'], ['-m32'],
               get_prebuilts_gcc(top, 'x86', 'x86_64-linux-android-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-x86', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-x86', 'usr', 'lib'])),

        Target('x86_64', 'x86_64-linux-android', ['-m64'], ['-m64'],
               get_prebuilts_gcc(top, 'x86', 'x86_64-linux-android-4.9'),
               get_prebuilts_clang(top),
               get_prebuilts_ndk(top, ['arch-x86_64', 'usr', 'include']),
               get_prebuilts_ndk(top, ['arch-x86_64', 'usr', 'lib64'])),
    ]


class TestRunner(object):
    """Class to run the test"""
    def __init__(self, expected_dir, test_dir, verbose):
        """Parameterized constructor"""
        self.expected_dir = expected_dir
        self.test_dir = test_dir
        self.verbose = verbose
        self.num_errors = 0

    def check_output(self, expected_file_path, actual):
        """Compare the output of the test run and the expected output"""
        actual = actual.splitlines(True)
        with open(expected_file_path, 'r') as f:
            expected = f.readlines()
        if actual == expected:
            return
        for line in difflib.context_diff(expected, actual,
                                         fromfile=expected_file_path,
                                         tofile='actual'):
            sys.stderr.write(line)
        self.num_errors += 1

    def run_test_for_target(self, target):
        """Run the test for a specific target"""
        print('Testing target', target.name, '...', file=sys.stderr)

        expected_dir = os.path.join(self.expected_dir, target.name)

        # Create test directory for this target.
        test_dir = os.path.join(self.test_dir, target.name)
        os.makedirs(test_dir, exist_ok=True)

        # Compile and test "libtest.so".
        src_file = os.path.join(SCRIPT_DIR, 'test1.cpp')
        obj_file = os.path.join(test_dir, 'test.o')
        target.compile(obj_file, src_file, [], self.verbose)

        out_file = os.path.join(test_dir, 'libtest.so')
        target.link(out_file, [obj_file],
                    ['-shared', '-lc', '-lgcc', '-lstdc++'],
                    self.verbose)
        self.check_output(os.path.join(expected_dir, 'libtest.so.txt'),
                          run_vtable_dump(out_file, self.verbose))

    def run_test(self, targets):
        """Run test fo all targets"""
        for target in targets:
            self.run_test_for_target(target)


def main():
    """ Set up and run test"""
    # Parse command line arguments.
    parser = argparse.ArgumentParser()
    parser.add_argument('--verbose', '-v', action='store_true')
    parser.add_argument('--android-build-top', help='path to android build top')
    parser.add_argument('--test-dir',
                        help='directory for temporary files')
    parser.add_argument('--expected-dir', help='directory with expected output')
    args = parser.parse_args()

    # Find ${ANDROID_BUILD_TOP}.
    if args.android_build_top:
        android_build_top = args.android_build_top
    else:
        android_build_top = get_dirnames(SCRIPT_DIR, 5)

    # Find expected output directory.
    if args.expected_dir:
        expected_dir = args.expected_dir
    else:
        expected_dir = os.path.join(SCRIPT_DIR, 'expected')

    # Load compilation targets.
    targets = create_targets(android_build_top)

    # Run tests.
    if args.test_dir:
        os.makedirs(args.test_dir, exist_ok=True)
        runner = TestRunner(expected_dir, args.test_dir, args.verbose)
        runner.run_test(targets)
    else:
        with tempfile.TemporaryDirectory() as test_dir:
            runner = TestRunner(expected_dir, test_dir, args.verbose)
            runner.run_test(targets)

    if runner.num_errors:
        print('FAILED:', runner.num_errors, 'test(s) failed', file=sys.stderr)
    else:
        print('SUCCESS', file=sys.stderr)

    return 1 if runner.num_errors else 0

if __name__ == '__main__':
    sys.exit(main())
