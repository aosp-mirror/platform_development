#!/usr/bin/env python

import os
import subprocess
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ANDROID_BUILD_TOP = os.path.abspath(os.path.join(SCRIPT_DIR, *(['..'] * 5)))

NDK_VERSION = 'r11'
API_LEVEL = 'android-24'

def get_prebuilts_host():
    if sys.platform.startswith('linux'):
        return 'linux-x86'
    if sys.platform.startswith('darwin'):
        return 'darwin-x86'
    raise NotImplementedError('unknown platform')

def get_prebuilts_gcc(arch, gcc_version):
    return os.path.join(ANDROID_BUILD_TOP, 'prebuilts', 'gcc',
                        get_prebuilts_host(), arch, gcc_version)

def get_prebuilts_clang():
    return os.path.join(ANDROID_BUILD_TOP, 'prebuilts', 'clang', 'host',
                        get_prebuilts_host(), 'clang-stable')

def get_prebuilts_ndk(subdirs):
    return os.path.join(ANDROID_BUILD_TOP, 'prebuilts', 'ndk', NDK_VERSION,
                        'platforms', API_LEVEL, *subdirs)


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

def create_targets():
    return {
        'arm': Target('arm', 'arm-linux-androideabi', [],[],
            get_prebuilts_gcc('arm', 'arm-linux-androideabi-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-arm', 'usr', 'include']),
            get_prebuilts_ndk(['arch-arm', 'usr', 'lib'])),

        'arm64': Target('arm64', 'aarch64-linux-android', [], [],
            get_prebuilts_gcc('aarch64', 'aarch64-linux-android-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-arm64', 'usr', 'include']),
            get_prebuilts_ndk(['arch-arm64', 'usr', 'lib'])),

        'x86': Target('x86', 'x86_64-linux-android', ['-m32'], ['-m32'],
            get_prebuilts_gcc('x86', 'x86_64-linux-android-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-x86', 'usr', 'include']),
            get_prebuilts_ndk(['arch-x86', 'usr', 'lib'])),

        'x86_64': Target('x86_64', 'x86_64-linux-android', ['-m64'], ['-m64'],
            get_prebuilts_gcc('x86', 'x86_64-linux-android-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-x86_64', 'usr', 'include']),
            get_prebuilts_ndk(['arch-x86_64', 'usr', 'lib64'])),

        'mips': Target('mips', 'mipsel-linux-android', [], [],
            get_prebuilts_gcc('mips', 'mips64el-linux-android-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-mips', 'usr', 'include']),
            get_prebuilts_ndk(['arch-mips', 'usr', 'lib'])),

        'mips64': Target('mips64', 'mips64el-linux-android',
            ['-march=mips64el', '-mcpu=mips64r6'],
            ['-march=mips64el', '-mcpu=mips64r6'],
            get_prebuilts_gcc('mips', 'mips64el-linux-android-4.9'),
            get_prebuilts_clang(),
            get_prebuilts_ndk(['arch-mips64', 'usr', 'include']),
            get_prebuilts_ndk(['arch-mips64', 'usr', 'lib64'])),
    }
