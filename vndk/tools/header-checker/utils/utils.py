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

SO_EXT = '.so'
SOURCE_ABI_DUMP_EXT_END = '.lsdump'
SOURCE_ABI_DUMP_EXT = SO_EXT + SOURCE_ABI_DUMP_EXT_END

DEFAULT_CPPFLAGS = ['-x', 'c++', '-std=c++11']
DEFAULT_CFLAGS = ['-std=gnu99']

TARGET_ARCHS = ['arm', 'arm64', 'x86', 'x86_64', 'mips', 'mips64']

def get_reference_dump_dir(reference_dump_dir_stem,
                           reference_dump_dir_insertion, lib_arch):
    reference_dump_dir = os.path.join(reference_dump_dir_stem, lib_arch)
    reference_dump_dir = os.path.join(reference_dump_dir,
                                      reference_dump_dir_insertion)
    return reference_dump_dir


def copy_reference_dumps(lib_paths, reference_dir_stem,
                         reference_dump_dir_insertion, lib_arch):
    reference_dump_dir = get_reference_dump_dir(reference_dir_stem,
                                                reference_dump_dir_insertion,
                                                lib_arch)
    num_created = 0
    for lib_path in lib_paths:
        copy_reference_dump(lib_path, reference_dump_dir)
        num_created += 1
    return num_created

def copy_reference_dump(lib_path, reference_dump_dir):
    reference_dump_path = os.path.join(reference_dump_dir,
                                       os.path.basename(lib_path))
    os.makedirs(os.path.dirname(reference_dump_path), exist_ok=True)
    output_content = read_output_content(lib_path, AOSP_DIR)
    with open(reference_dump_path, 'w') as f:
        f.write(output_content)
    print('Created abi dump at ', reference_dump_path)
    return reference_dump_path

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
    return reference_dump_path

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
    input_name, input_ext = os.path.splitext(input_path)
    cmd = ['header-abi-dumper', '-o', output_path, input_path,]
    for dir in export_include_dirs:
        cmd += ['-I', dir]
    cmd += ['--']
    cmd += cflags
    if input_ext == '.cpp' or input_ext == '.cc' or input_ext == '.h':
        cmd += DEFAULT_CPPFLAGS
    else:
        cmd += DEFAULT_CFLAGS

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

def find_lib_lsdumps(target_arch, target_arch_variant,
                     target_cpu_variant, soong_dir):
    """ Find the lsdump corresponding to lib_name for the given arch parameters
        if it exists"""
    assert 'ANDROID_PRODUCT_OUT' in os.environ
    cpu_variant = '_' + target_cpu_variant
    arch_variant = '_' + target_arch_variant
    lsdump_paths = []
    if target_cpu_variant == 'generic' or target_cpu_variant is None or\
        target_cpu_variant == '':
        cpu_variant = ''
    if target_arch_variant == target_arch or target_arch_variant is None or\
        target_arch_variant == '':
        arch_variant = ''

    target_dir = 'android_' + target_arch + arch_variant +\
    cpu_variant + '_vendor_shared'
    for base, dirnames, filenames in os.walk(soong_dir):
        for filename in filenames:
            name, ext = os.path.splitext(filename)
            sofile, soext = os.path.splitext(name)
            if ext == SOURCE_ABI_DUMP_EXT_END and soext == SO_EXT :
                path = os.path.join(base, filename)
                if target_dir in os.path.dirname(path):
                    lsdump_paths.append(path)
    return lsdump_paths

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

    cmd = ['build/soong/soong_ui.bash', '--dumpvar-mode', name]

    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, cwd=AOSP_DIR)
    out, err = proc.communicate()
    return out.decode('utf-8').strip()
