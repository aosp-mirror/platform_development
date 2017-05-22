#!/usr/bin/env python3

from __future__ import print_function

import argparse
import os
import re
import subprocess
import sys
import traceback

# Python 2 and 3 compatibility layers.
if sys.version_info >= (3, 0):
    from os import makedirs
    from shutil import which

    def get_byte(buf, idx):
        return buf[idx]

    def check_silent_call(cmd):
        subprocess.check_call(cmd, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL)
else:
    def makedirs(path, exist_ok):
        if exist_ok and os.path.isdir(path):
            return
        return os.makedirs(path)

    def which(cmd, mode=os.F_OK | os.X_OK, path=None):
        def is_executable(path):
            return (os.path.exists(file_path) and \
                    os.access(file_path, mode) and \
                    not os.path.isdir(file_path))
        if path is None:
            path = os.environ.get('PATH', os.defpath)
        for path_dir in path.split(os.pathsep):
            for file_name in os.listdir(path_dir):
                if file_name != cmd:
                    continue
                file_path = os.path.join(path_dir, file_name)
                if is_executable(file_path):
                    return file_path
        return None

    def get_byte(buf, idx):
        return ord(buf[idx])

    def check_silent_call(cmd):
        with open(os.devnull, 'wb') as devnull:
            subprocess.check_call(cmd, stdout=devnull, stderr=devnull)

    FileNotFoundError = OSError


# Path constants.
SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
AOSP_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, *['..'] * 4))
ABI_DUMPER = os.path.join(AOSP_DIR, 'external', 'abi-dumper', 'abi-dumper.pl')
VTABLE_DUMPER = 'vndk-vtable-dumper'
BINARY_ABI_DUMP_EXT = '.bdump'
STRIP_DEUBG_INFO = os.path.join(SCRIPT_DIR, 'strip_debug_info.pl')


# Compilation targets.
class Target(object):
    def __init__(self, arch, gcc_arch, gcc_prefix, gcc_version, lib_dir_name):
        self.arch = arch
        self.gcc_dir = self._get_prebuilts_gcc(gcc_arch, gcc_prefix,
                                               gcc_version)
        self.gcc_prefix = gcc_prefix
        self.lib_dir_name = lib_dir_name

    def _get_prebuilts_host(self):
        """Get the host dir for prebuilts"""
        if sys.platform.startswith('linux'):
            return 'linux-x86'
        if sys.platform.startswith('darwin'):
            return 'darwin-x86'
        raise NotImplementedError('unknown platform')

    def _get_prebuilts_gcc(self, gcc_arch, gcc_prefix, gcc_version):
        """Get the path to gcc for the current platform"""
        return os.path.join(AOSP_DIR, 'prebuilts', 'gcc',
                            self._get_prebuilts_host(), gcc_arch,
                            gcc_prefix + gcc_version)

    def get_exe(self, name):
        """Get the path to prebuilt executable"""
        return os.path.join(self.gcc_dir, 'bin', self.gcc_prefix + name)

class TargetRegistry(object):
    def __init__(self):
        self.targets = dict()

    def add(self, arch, gcc_arch, gcc_prefix, gcc_version, lib_dir_name):
         self.targets[arch] = Target(arch, gcc_arch, gcc_prefix, gcc_version,
                                     lib_dir_name)

    def get(self, arch_name, var_name):
        try:
            return self.targets[arch_name]
        except KeyError:
            print('{}: error: unknown {}: {}'
                    .format(sys.argv[0], var_name, arch_name), file=sys.stderr)
            sys.exit(1)

    @staticmethod
    def create():
        res = TargetRegistry()
        res.add('arm', 'arm', 'arm-linux-androideabi-', '4.9', 'lib')
        res.add('arm64', 'aarch64', 'aarch64-linux-android-', '4.9', 'lib64')
        res.add('mips', 'mips', 'mips64el-linux-android-', '4.9', 'lib')
        res.add('mips64', 'mips', 'mips64el-linux-android-', '4.9', 'lib64')
        res.add('x86', 'x86', 'x86_64-linux-android-', '4.9', 'lib')
        res.add('x86_64', 'x86', 'x86_64-linux-android-', '4.9', 'lib64')
        return res


# Command tests.
def test_command(name, options, expected_output):
    def is_command_valid():
        try:
            if os.path.exists(name) and os.access(name, os.F_OK | os.X_OK):
                exec_path = name
            else:
                exec_path = which(name)
                if not exec_path:
                    return False
            output = subprocess.check_output([exec_path] + options)
            return (expected_output in output)
        except Exception:
            traceback.print_exc()
            return False

    if not is_command_valid():
        print('error: failed to run {} command'.format(name), file=sys.stderr)
        sys.exit(1)

def test_readelf_command(readelf):
    test_command(readelf, ['-v'], b'GNU readelf')

def test_objdump_command(objdump):
    test_command(objdump, ['-v'], b'GNU objdump')

def test_vtable_dumper_command():
    test_command(VTABLE_DUMPER, ['--version'], b'vndk-vtable-dumper')

def test_abi_dumper_command():
    test_command(ABI_DUMPER, ['-v'], b'ABI Dumper')

def test_all_commands(readelf, objdump):
    test_readelf_command(readelf)
    test_objdump_command(objdump)
    test_vtable_dumper_command()
    test_abi_dumper_command()


# ELF file format constants.
ELF_MAGIC = b'\x7fELF'

EI_CLASS = 4
EI_DATA = 5
EI_NIDENT = 8

ELFCLASS32 = 1
ELFCLASS64 = 2

ELFDATA2LSB = 1
ELFDATA2MSB = 2


# ELF file check utilities.
def is_elf_ident(buf):
    # Check the length of ELF ident.
    if len(buf) != EI_NIDENT:
        return False

    # Check ELF magic word.
    if buf[0:4] != ELF_MAGIC:
        return False

    # Check ELF machine word size.
    ei_class = get_byte(buf, EI_CLASS)
    if ei_class != ELFCLASS32 and ei_class != ELFCLASS64:
        return False

    # Check ELF endianness.
    ei_data = get_byte(buf, EI_DATA)
    if ei_data != ELFDATA2LSB and ei_data != ELFDATA2MSB:
        return False

    return True

def is_elf_file(path):
    try:
        with open(path, 'rb') as f:
            return is_elf_ident(f.read(EI_NIDENT))
    except FileNotFoundError:
        return False

def create_vndk_lib_name_filter(file_list_path):
    if not file_list_path:
        def accept_all_filenames(name):
            return True
        return accept_all_filenames

    with open(file_list_path, 'r') as f:
        lines = f.read().splitlines()

    patt = re.compile('^(?:' +
                      '|'.join('(?:' + re.escape(x) + ')' for x in lines) +
                      ')$')
    def accept_matched_filenames(name):
        return patt.match(name)
    return accept_matched_filenames

def run_cmd(cmd, show_commands):
    if show_commands:
        print(' '.join(cmd))
    check_silent_call(cmd)

def create_abi_reference_dump(out_dir, symbols_dir, api_level, show_commands,
                              target, is_vndk_lib_name, strip_debug_info):
    # Check command line tools.
    readelf = target.get_exe('readelf')
    objdump = target.get_exe('objdump')
    test_all_commands(readelf, objdump)

    # Check library directory.
    lib_dir = os.path.join(symbols_dir, 'system', target.lib_dir_name)
    if not os.path.exists(lib_dir):
        print('error: failed to find lib directory:', lib_dir, file=sys.stderr)
        sys.exit(1)

    # Append target architecture to output directory path.
    out_dir = os.path.join(out_dir, target.arch)

    # Process libraries.
    cmd_base = [ABI_DUMPER, '-lver', api_level, '-objdump', objdump,
                '-readelf', readelf, '-vt-dumper', which(VTABLE_DUMPER),
                '-use-tu-dump', '--quiet']

    num_processed = 0
    lib_dir = os.path.abspath(lib_dir)
    prefix_len = len(lib_dir) + 1
    for base, dirnames, filenames in os.walk(lib_dir):
        for filename in filenames:
            if not is_vndk_lib_name(filename):
                continue

            path = os.path.join(base, filename)
            if not is_elf_file(path):
                continue

            rel_path = path[prefix_len:]
            out_path = os.path.join(out_dir, rel_path) + BINARY_ABI_DUMP_EXT

            makedirs(os.path.dirname(out_path), exist_ok=True)
            cmd = cmd_base + [path, '-o', out_path]
            print('# FILE:', path)
            run_cmd(cmd, show_commands)
            if strip_debug_info:
                run_cmd([STRIP_DEUBG_INFO, out_path], show_commands)
            num_processed += 1

    return num_processed

def get_build_var_from_build_system(name):
    """Get build system variable for the launched target."""
    if 'ANDROID_PRODUCT_OUT' not in os.environ:
        return None

    cmd = ['make', '--no-print-directory', '-f', 'build/core/config.mk',
           'dumpvar-' + name]

    environ = dict(os.environ)
    environ['CALLED_FROM_SETUP'] = 'true'
    environ['BUILD_SYSTEM'] = 'build/core'

    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, env=environ,
                            cwd=AOSP_DIR)
    out, err = proc.communicate()
    return out.decode('utf-8').strip()

def get_build_var(name, args):
    """Get build system variable either from command line option or build
    system."""
    value = getattr(args, name.lower(), None)
    return value if value else get_build_var_from_build_system(name)

def report_missing_argument(parser, arg_name):
    parser.print_usage()
    print('{}: error: the following arguments are required: {}'
            .format(sys.argv[0], arg_name), file=sys.stderr)
    sys.exit(1)

def main():
    # Parse command line options.
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', '-o', metavar='path',
                        help='output directory for abi reference dump')
    parser.add_argument('--vndk-list', help='VNDK library list')
    parser.add_argument('--api-level', default='24', help='VNDK API level')
    parser.add_argument('--target-arch', help='target architecture')
    parser.add_argument('--target-2nd-arch', help='second target architecture')
    parser.add_argument('--product-out', help='android product out')
    parser.add_argument('--target-product', help='target product')
    parser.add_argument('--target-build-variant', help='target build variant')
    parser.add_argument('--symbols-dir', help='unstripped symbols directory')
    parser.add_argument('--show-commands', action='store_true',
                        help='Show commands')
    parser.add_argument('--strip-debug-info', action='store_true',
                        help='Remove debug information from ABI dump files')
    args = parser.parse_args()

    # Check the symbols directory.
    if args.symbols_dir:
        symbols_dir = args.symbols_dir
    else:
        # If the user did not specify the symbols directory, try to create
        # one from ANDROID_PRODUCT_OUT.
        product_out = get_build_var('PRODUCT_OUT', args)
        if not product_out:
            report_missing_argument(parser, '--symbols-dir')
        if not os.path.isabs(product_out):
            product_out = os.path.join(AOSP_DIR, product_out)
        symbols_dir = os.path.join(product_out, 'symbols')

    # Check the output directory.
    if args.output:
        out_dir = args.output
    else:
        # If the user did not specify the output directory, try to create one
        # default output directory from TARGET_PRODUCT and
        # TARGET_BUILD_VARIANT.

        target_product = get_build_var('TARGET_PRODUCT', args)
        target_build_variant = get_build_var('TARGET_BUILD_VARIANT', args)
        if not target_product or not target_build_variant:
            report_missing_argument(parser, '--output/-o')
        lunch_name = target_product + '-' + target_build_variant
        out_dir = os.path.join(AOSP_DIR, 'vndk', 'dumps', lunch_name)

    # Check the targets.
    target_registry = TargetRegistry.create()
    targets = []

    arch_name = get_build_var('TARGET_ARCH', args)
    if not arch_name:
        report_missing_argument(parser, '--target-arch')
    targets.append(target_registry.get(arch_name, 'TARGET_ARCH'))
    must_have_2nd_arch = (targets[0].lib_dir_name == 'lib64')

    arch_name = get_build_var('TARGET_2ND_ARCH', args)
    if arch_name:
        targets.append(target_registry.get(arch_name, 'TARGET_2ND_ARCH'))
    elif must_have_2nd_arch:
        report_missing_argument(parser, '--target-2nd-arch')

    # Dump all libraries for the specified architectures.
    num_processed = 0
    for target in targets:
        num_processed += create_abi_reference_dump(
                out_dir, symbols_dir, args.api_level, args.show_commands,
                target, create_vndk_lib_name_filter(args.vndk_list),
                args.strip_debug_info)

    # Print a summary at the end.
    _TERM_WIDTH = 79
    print()
    print('-' * _TERM_WIDTH)
    print('msg: Reference dump created at directory:', out_dir)
    print('msg: Processed', num_processed, 'libraries')

if __name__ == '__main__':
    main()
