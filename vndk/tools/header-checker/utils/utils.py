#!/usr/bin/env python3

import tempfile
import os
import subprocess

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
AOSP_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, *['..'] * 5))

BUILTIN_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'bionic', 'libc', 'include'),
    os.path.join(AOSP_DIR, 'external', 'libcxx', 'include'),
    os.path.join(AOSP_DIR, 'prebuilts', 'sdk', 'renderscript', 'clang-include'),
)

EXPORTED_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'development', 'vndk', 'tools', 'header-checker',
                 'tests'),
)

SOURCE_ABI_DUMP_EXT = ".so.lsdump"

TARGET_ARCHS = ['arm', 'arm64', 'x86', 'x86_64', 'mips', 'mips64']

def get_reference_dump_dir(reference_dump_dir_stem,
                            reference_dump_dir_insertion, lib_arch):
    reference_dump_dir = os.path.join(reference_dump_dir_stem, lib_arch)
    reference_dump_dir = os.path.join(reference_dump_dir,
                                      reference_dump_dir_insertion)
    return reference_dump_dir

def copy_reference_dump(lib_path, reference_dump_dir_stem,
                        reference_dump_dir_insertion, lib_arch):
    if lib_path is None:
        return 0
    reference_dump_dir = get_reference_dump_dir(reference_dump_dir_stem,
                                                reference_dump_dir_insertion,
                                                lib_arch)
    reference_dump_path = os.path.join(reference_dump_dir,
                                       os.path.basename(lib_path))
    os.makedirs(os.path.dirname(reference_dump_path), exist_ok=True)
    output_content = read_output_content(lib_path, AOSP_DIR)
    with open(reference_dump_path, 'w') as f:
        f.write(output_content)
    print('Created abi dump at ', reference_dump_path)
    return 1

def copy_reference_dump_content(lib_name, output_content,
                                reference_dump_dir_stem,
                                reference_dump_dir_insertion, lib_arch):
    reference_dump_dir = get_reference_dump_dir(reference_dump_dir_stem,
                                                reference_dump_dir_insertion,
                                                lib_arch)
    reference_dump_path = os.path.join(reference_dump_dir,
                                       lib_name + SOURCE_ABI_DUMP_EXT)
    os.makedirs(os.path.dirname(reference_dump_path), exist_ok=True)
    with open(reference_dump_path, 'w') as f:
        f.write(output_content)
    print('Created abi dump at ', reference_dump_path)
    return 1

def read_output_content(output_path, replace_str):
    with open(output_path, 'r') as f:
        return f.read().replace(replace_str, '')

def run_header_abi_dumper(input_path, remove_absolute_paths, cflags=[],
                          export_include_dirs = EXPORTED_HEADERS_DIR):
    with tempfile.TemporaryDirectory() as tmp:
        output_path = os.path.join(tmp, os.path.basename(input_path)) + '.dump'
        run_header_abi_dumper_on_file(input_path, output_path,
                                      export_include_dirs, cflags)
        with open(output_path, 'r') as f:
            if remove_absolute_paths:
                return read_output_content(output_path, AOSP_DIR)
            else:
                return f.read()

def run_header_abi_dumper_on_file(input_path, output_path,
                                  export_include_dirs = [], cflags =[]):
    cmd = ['header-abi-dumper', '-o', output_path, input_path,]
    for dir in export_include_dirs:
        cmd += ['-I', dir]
    cmd += ['--']
    cmd += cflags
    for dir in BUILTIN_HEADERS_DIR:
        cmd += ['-isystem', dir]
    # export includes imply local includes
    for dir in export_include_dirs:
        cmd += ['-I', dir]
    subprocess.check_call(cmd)

def run_header_abi_linker(output_path, inputs, version_script, api, arch):
    """Link inputs, taking version_script into account"""
    with tempfile.TemporaryDirectory() as tmp:
        cmd = ['header-abi-linker', '-o', output_path, '-v', version_script,
               '-api', api, '-arch', arch]
        cmd += inputs
        subprocess.check_call(cmd)
        with open(output_path, 'r') as f:
            return read_output_content(output_path, AOSP_DIR)

def make_library(lib_name):
    # Create reference dumps for integration tests
    make_cmd = ['make', '-j', lib_name]
    subprocess.check_call(make_cmd, cwd=AOSP_DIR)

def find_lib_lsdump(lib_name, target_arch, target_arch_variant,
                    target_cpu_variant):
    """ Find the lsdump corresponding to lib_name for the given arch parameters
        if it exists"""
    assert 'ANDROID_PRODUCT_OUT' in os.environ
    cpu_variant = '_' + target_cpu_variant
    arch_variant = '_' + target_arch_variant

    if target_cpu_variant == 'generic' or target_cpu_variant is None or\
        target_cpu_variant == '':
        cpu_variant = ''
    if target_arch_variant == target_arch or target_arch_variant is None or\
        target_arch_variant == '':
        arch_variant = ''

    target_dir = 'android_' + target_arch + arch_variant +\
    cpu_variant + '_shared_core'
    soong_dir = os.path.join(AOSP_DIR, 'out', 'soong', '.intermediates')
    expected_lsdump_name = lib_name + SOURCE_ABI_DUMP_EXT
    for base, dirnames, filenames in os.walk(soong_dir):
        for filename in filenames:
            if filename == expected_lsdump_name:
                path = os.path.join(base, filename)
                if target_dir in os.path.dirname(path):
                    return path
    return None

def run_abi_diff(old_test_dump_path, new_test_dump_path, arch, lib_name,
                 flags=[]):
    abi_diff_cmd = ['header-abi-diff', '-new', new_test_dump_path, '-old',
                     old_test_dump_path, '-arch', arch, '-lib', lib_name]
    with tempfile.TemporaryDirectory() as tmp:
        output_name = os.path.join(tmp, lib_name) + '.abidiff'
        abi_diff_cmd += ['-o', output_name]
        abi_diff_cmd += flags
        try:
            subprocess.check_call(abi_diff_cmd)
        except subprocess.CalledProcessError as err:
            return err.returncode

    return 0


def get_build_var(name):
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
