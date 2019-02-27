#!/usr/bin/env python3

import gzip
import os
import subprocess
import sys
import tempfile
import collections


SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))

try:
    AOSP_DIR = os.environ['ANDROID_BUILD_TOP']
except KeyError:
    print('error: ANDROID_BUILD_TOP environment variable is not set.',
          file=sys.stderr)
    sys.exit(1)

BUILTIN_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'bionic', 'libc', 'include'),
    os.path.join(AOSP_DIR, 'external', 'libcxx', 'include'),
    os.path.join(AOSP_DIR, 'prebuilts', 'clang-tools', 'linux-x86',
                 'clang-headers'),
)

EXPORTED_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'development', 'vndk', 'tools', 'header-checker',
                 'tests'),
)

SO_EXT = '.so'
SOURCE_ABI_DUMP_EXT_END = '.lsdump'
SOURCE_ABI_DUMP_EXT = SO_EXT + SOURCE_ABI_DUMP_EXT_END
COMPRESSED_SOURCE_ABI_DUMP_EXT = SOURCE_ABI_DUMP_EXT + '.gz'
VENDOR_SUFFIX = '.vendor'

DEFAULT_CPPFLAGS = ['-x', 'c++', '-std=c++11']
DEFAULT_CFLAGS = ['-std=gnu99']
DEFAULT_HEADER_FLAGS = ["-dump-function-declarations"]
DEFAULT_FORMAT = 'ProtobufTextFormat'


def get_reference_dump_dir(reference_dump_dir_stem,
                           reference_dump_dir_insertion, lib_arch):
    reference_dump_dir = os.path.join(reference_dump_dir_stem, lib_arch)
    reference_dump_dir = os.path.join(reference_dump_dir,
                                      reference_dump_dir_insertion)
    return reference_dump_dir


def copy_reference_dumps(lib_paths, reference_dir_stem,
                         reference_dump_dir_insertion, lib_arch, compress):
    reference_dump_dir = get_reference_dump_dir(reference_dir_stem,
                                                reference_dump_dir_insertion,
                                                lib_arch)
    num_created = 0
    for lib_path in lib_paths:
        copy_reference_dump(lib_path, reference_dump_dir, compress)
        num_created += 1
    return num_created


def copy_reference_dump(lib_path, reference_dump_dir, compress):
    reference_dump_path = os.path.join(
        reference_dump_dir, os.path.basename(lib_path))
    if compress:
        reference_dump_path += '.gz'
    os.makedirs(os.path.dirname(reference_dump_path), exist_ok=True)
    output_content = read_output_content(lib_path, AOSP_DIR)
    if compress:
        with gzip.open(reference_dump_path, 'wb') as f:
            f.write(bytes(output_content, 'utf-8'))
    else:
        with open(reference_dump_path, 'wb') as f:
            f.write(bytes(output_content, 'utf-8'))
    print('Created abi dump at', reference_dump_path)
    return reference_dump_path


def read_output_content(output_path, replace_str):
    with open(output_path, 'r') as f:
        return f.read().replace(replace_str, '')


def run_header_abi_dumper(input_path, cflags=tuple(),
                          export_include_dirs=EXPORTED_HEADERS_DIR,
                          flags=tuple()):
    """Run header-abi-dumper to dump ABI from `input_path` and return the
    output."""
    with tempfile.TemporaryDirectory() as tmp:
        output_path = os.path.join(tmp, os.path.basename(input_path)) + '.dump'
        run_header_abi_dumper_on_file(input_path, output_path,
                                      export_include_dirs, cflags, flags)
        return read_output_content(output_path, AOSP_DIR)


def run_header_abi_dumper_on_file(input_path, output_path,
                                  export_include_dirs=tuple(), cflags=tuple(),
                                  flags=tuple()):
    """Run header-abi-dumper to dump ABI from `input_path` and the output is
    written to `output_path`."""
    input_ext = os.path.splitext(input_path)[1]
    cmd = ['header-abi-dumper', '-o', output_path, input_path]
    for dir in export_include_dirs:
        cmd += ['-I', dir]
    cmd += flags
    if '-output-format' not in flags:
        cmd += ['-output-format', DEFAULT_FORMAT]
    if input_ext == ".h":
        cmd += DEFAULT_HEADER_FLAGS
    cmd += ['--']
    cmd += cflags
    if input_ext in ('.cpp', '.cc', '.h'):
        cmd += DEFAULT_CPPFLAGS
    else:
        cmd += DEFAULT_CFLAGS

    for dir in BUILTIN_HEADERS_DIR:
        cmd += ['-isystem', dir]
    # The export include dirs imply local include dirs.
    for dir in export_include_dirs:
        cmd += ['-I', dir]
    subprocess.check_call(cmd)


def run_header_abi_linker(output_path, inputs, version_script, api, arch,
                          flags=tuple()):
    """Link inputs, taking version_script into account"""
    cmd = ['header-abi-linker', '-o', output_path, '-v', version_script,
           '-api', api, '-arch', arch]
    cmd += flags
    if '-input-format' not in flags:
        cmd += ['-input-format', DEFAULT_FORMAT]
    if '-output-format' not in flags:
        cmd += ['-output-format', DEFAULT_FORMAT]
    cmd += inputs
    subprocess.check_call(cmd)
    return read_output_content(output_path, AOSP_DIR)


def make_targets(product, variant, targets):
    make_cmd = ['build/soong/soong_ui.bash', '--make-mode', '-j',
                'TARGET_PRODUCT=' + product, 'TARGET_BUILD_VARIANT=' + variant]
    make_cmd += targets
    subprocess.check_call(make_cmd, cwd=AOSP_DIR)


def make_tree(product, variant):
    """Build all lsdump files."""
    return make_targets(product, variant, ['findlsdumps'])


def make_libraries(product, variant, targets, libs, llndk_mode):
    """Build lsdump files for specific libs."""
    lsdump_paths = read_lsdump_paths(product, variant, targets, build=True)
    targets = []
    for name in libs:
        targets.extend(lsdump_paths[name].values())
    make_targets(product, variant, targets)


def get_lsdump_paths_file_path(product, variant):
    """Get the path to lsdump_paths.txt."""
    product_out = get_build_vars_for_product(
        ['PRODUCT_OUT'], product, variant)[0]
    return os.path.join(product_out, 'lsdump_paths.txt')


def _is_sanitizer_variation(variation):
    """Check whether the variation is introduced by a sanitizer."""
    return variation in {'asan', 'hwasan', 'tsan', 'intOverflow', 'cfi', 'scs'}


def _are_sanitizer_variations(variations):
    """Check whether these variations are introduced by sanitizers."""
    if isinstance(variations, str):
        variations = [v for v in variations.split('_') if v]
    return all(_is_sanitizer_variation(v) for v in variations)


def _read_lsdump_paths(lsdump_paths_file_path, targets):
    """Read lsdump path from lsdump_paths.txt for each libname and variant."""
    lsdump_paths = collections.defaultdict(dict)
    suffixes = collections.defaultdict(dict)

    prefixes = []
    prefixes.extend(get_module_variant_dir_name(
        target.arch, target.arch_variant, target.cpu_variant, '_core_shared')
        for target in targets)
    prefixes.extend(get_module_variant_dir_name(
        target.arch, target.arch_variant, target.cpu_variant, '_vendor_shared')
        for target in targets)

    with open(lsdump_paths_file_path, 'r') as lsdump_paths_file:
        for line in lsdump_paths_file:
            path = line.strip()
            if not path:
                continue
            dirname, filename = os.path.split(path)
            if not filename.endswith(SOURCE_ABI_DUMP_EXT):
                continue
            libname = filename[:-len(SOURCE_ABI_DUMP_EXT)]
            if not libname:
                continue
            variant = os.path.basename(dirname)
            if not variant:
                continue
            for prefix in prefixes:
                if not variant.startswith(prefix):
                    continue
                new_suffix = variant[len(prefix):]
                if not _are_sanitizer_variations(new_suffix):
                    continue
                old_suffix = suffixes[libname].get(prefix)
                if not old_suffix or new_suffix > old_suffix:
                    lsdump_paths[libname][prefix] = path
                    suffixes[libname][prefix] = new_suffix
    return lsdump_paths


def read_lsdump_paths(product, variant, targets, build=True):
    """Build lsdump_paths.txt and read the paths."""
    lsdump_paths_file_path = get_lsdump_paths_file_path(product, variant)
    if build:
        make_targets(product, variant, [lsdump_paths_file_path])
    lsdump_paths_file_abspath = os.path.join(AOSP_DIR, lsdump_paths_file_path)
    return _read_lsdump_paths(lsdump_paths_file_abspath, targets)


def get_module_variant_dir_name(arch, arch_variant, cpu_variant,
                                variant_suffix):
    """Create module variant directory name from the target architecture, the
    target architecture variant, the target CPU variant, and a variant suffix
    (e.g. `_core_shared`, `_vendor_shared`, etc)."""

    if not arch_variant or arch_variant == arch:
        arch_variant = ''
    else:
        arch_variant = '_' + arch_variant

    if not cpu_variant or cpu_variant == 'generic':
        cpu_variant = ''
    else:
        cpu_variant = '_' + cpu_variant

    return 'android_' + arch + arch_variant + cpu_variant + variant_suffix


def find_lib_lsdumps(module_variant_dir_name, lsdump_paths, libs):
    """Find the lsdump corresponding to lib_name for the given module variant
    if it exists."""
    result = []
    for lib_name, variations in lsdump_paths.items():
        if libs and lib_name not in libs:
            continue
        for variation, path in variations.items():
            if variation.startswith(module_variant_dir_name):
                result.append(os.path.join(AOSP_DIR, path.strip()))
    return result


def run_abi_diff(old_test_dump_path, new_test_dump_path, arch, lib_name,
                 flags=tuple()):
    abi_diff_cmd = ['header-abi-diff', '-new', new_test_dump_path, '-old',
                    old_test_dump_path, '-arch', arch, '-lib', lib_name]
    with tempfile.TemporaryDirectory() as tmp:
        output_name = os.path.join(tmp, lib_name) + '.abidiff'
        abi_diff_cmd += ['-o', output_name]
        abi_diff_cmd += flags
        if '-input-format-old' not in flags:
            abi_diff_cmd += ['-input-format-old', DEFAULT_FORMAT]
        if '-input-format-new' not in flags:
            abi_diff_cmd += ['-input-format-new', DEFAULT_FORMAT]
        try:
            subprocess.check_call(abi_diff_cmd)
        except subprocess.CalledProcessError as err:
            return err.returncode

    return 0


def get_build_vars_for_product(names, product=None, variant=None):
    """ Get build system variable for the launched target."""

    if product is None and 'ANDROID_PRODUCT_OUT' not in os.environ:
        return None

    env = os.environ.copy()
    if product:
        env['TARGET_PRODUCT'] = product
    if variant:
        env['TARGET_BUILD_VARIANT'] = variant
    cmd = [
        os.path.join('build', 'soong', 'soong_ui.bash'),
        '--dumpvars-mode', '-vars', ' '.join(names),
    ]

    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, cwd=AOSP_DIR, env=env)
    out, err = proc.communicate()

    if proc.returncode != 0:
        print("error: %s" % err.decode('utf-8'), file=sys.stderr)
        return None

    build_vars = out.decode('utf-8').strip().splitlines()

    build_vars_list = []
    for build_var in build_vars:
        value = build_var.partition('=')[2]
        build_vars_list.append(value.replace('\'', ''))
    return build_vars_list
