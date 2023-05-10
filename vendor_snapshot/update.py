#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
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
"""Unzips and installs the vendor snapshot."""

import argparse
import glob
import logging
import os
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
import json

INDENT = ' ' * 4


def get_target_arch(json_rel_path):
    return json_rel_path.split('/')[0]


def get_arch(json_rel_path):
    return json_rel_path.split('/')[1].split('-')[1]


def get_variation(json_rel_path):
    return json_rel_path.split('/')[2]

# convert .bp prop dictionary to .bp prop string
def gen_bp_prop(prop, ind):
    bp = ''
    for key in sorted(prop):
        val = prop[key]

        # Skip empty list or dict, rather than printing empty prop like
        # "key: []," or "key: {},"
        if type(val) == list and len(val) == 0:
            continue
        if type(val) == dict and gen_bp_prop(val, '') == '':
            continue

        bp += ind + key + ': '
        if type(val) == bool:
            bp += 'true,\n' if val else 'false,\n'
        elif type(val) == str:
            bp += '"%s",\n' % val
        elif type(val) == list:
            bp += '[\n'
            for elem in val:
                bp += ind + INDENT + '"%s",\n' % elem
            bp += ind + '],\n'
        elif type(val) == dict:
            bp += '{\n'
            bp += gen_bp_prop(val, ind + INDENT)
            bp += ind + '},\n'
        else:
            raise TypeError('unsupported type %s for gen_bp_prop' % type(val))
    return bp


# Remove non-existent dirs from given list. Emits warning for such dirs.
def remove_invalid_dirs(paths, bp_dir, module_name):
    ret = []
    for path in paths:
        if os.path.isdir(os.path.join(bp_dir, path)):
            ret.append(path)
        else:
            logging.warning('Dir "%s" of module "%s" does not exist', path,
                            module_name)
    return ret


JSON_TO_BP = {
    'ModuleName': 'name',
    'ModuleStemName': 'stem',
    'RelativeInstallPath': 'relative_install_path',
    'ExportedDirs': 'export_include_dirs',
    'ExportedSystemDirs': 'export_system_include_dirs',
    'ExportedFlags': 'export_flags',
    'Sanitize': 'sanitize',
    'SanitizeMinimalDep': 'sanitize_minimal_dep',
    'SanitizeUbsanDep': 'sanitize_ubsan_dep',
    'Symlinks': 'symlinks',
    'StaticExecutable': 'static_executable',
    'InstallInRoot': 'install_in_root',
    'InitRc': 'init_rc',
    'VintfFragments': 'vintf_fragments',
    'SharedLibs': 'shared_libs',
    'StaticLibs': 'static_libs',
    'RuntimeLibs': 'runtime_libs',
    'Required': 'required',
    'Filename': 'filename',
    'CrateName': 'crate_name',
    'Prebuilt': 'prebuilt',
    'Overrides': 'overrides',
    'MinSdkVersion': 'min_sdk_version',
}

LICENSE_KEYS = {
    'LicenseKinds': 'license_kinds',
    'LicenseTexts': 'license_text',
}

NOTICE_DIR = 'NOTICE_FILES'

SANITIZER_VARIANT_PROPS = {
    'export_include_dirs',
    'export_system_include_dirs',
    'export_flags',
    'sanitize_minimal_dep',
    'sanitize_ubsan_dep',
    'src',
}

EXPORTED_FLAGS_PROPS = {
    'export_include_dirs',
    'export_system_include_dirs',
    'export_flags',
}

# Convert json file to Android.bp prop dictionary
def convert_json_to_bp_prop(json_path, bp_dir):
    prop = json.load(json_path)
    return convert_json_data_to_bp_prop(prop, bp_dir)

# Converts parsed json dictionary (which is intermediate) to Android.bp prop
# dictionary. This validates paths such as include directories and init_rc
# files while converting.
def convert_json_data_to_bp_prop(prop, bp_dir):
    ret = {}
    lic_ret = {}

    module_name = prop['ModuleName']
    ret['name'] = module_name

    # Soong will complain about non-existing paths on Android.bp. There might
    # be missing files among generated header files, so check all exported
    # directories and filter out invalid ones. Emits warning for such dirs.
    # TODO: fix soong to track all generated headers correctly
    for key in {'ExportedDirs', 'ExportedSystemDirs'}:
        if key in prop:
            prop[key] = remove_invalid_dirs(prop[key], bp_dir, module_name)

    for key in prop:
        if key in JSON_TO_BP:
            ret[JSON_TO_BP[key]] = prop[key]
        elif key in LICENSE_KEYS:
            if key == 'LicenseTexts':
                # Add path prefix
                lic_ret[LICENSE_KEYS[key]] = [os.path.join(NOTICE_DIR, lic_path)
                                              for lic_path in prop[key]]
            else:
                lic_ret[LICENSE_KEYS[key]] = prop[key]
        else:
            logging.warning('Unknown prop "%s" of module "%s"', key,
                            module_name)

    return ret, lic_ret

def is_64bit_arch(arch):
    return '64' in arch # arm64, x86_64

def remove_keys_from_dict(keys, d):
    # May contain subdictionaries (e.g. cfi), so recursively erase
    for k in list(d.keys()):
        if k in keys:
            del d[k]
        elif type(d[k]) == dict:
            remove_keys_from_dict(keys, d[k])

def reexport_vndk_header(name, arch_props):
    remove_keys_from_dict(EXPORTED_FLAGS_PROPS, arch_props)
    for arch in arch_props:
        arch_props[arch]['shared_libs'] = [name]
        arch_props[arch]['export_shared_lib_headers'] = [name]

def gen_bp_module(image, variation, name, version, target_arch, vndk_list, arch_props, bp_dir):
    # Generate Android.bp module for given snapshot.
    # If a vndk library with the same name exists, reuses exported flags of the vndk library,
    # instead of the snapshot's own flags.

    if variation == 'license':
        bp = 'license {\n'
        bp += gen_bp_prop(arch_props, INDENT)
        bp += '}\n\n'
        return bp

    prop = {
        # These are common for all snapshot modules.
        image: True,
        'arch': {},
    }

    if variation != 'etc':
        prop['version'] = str(version)
        prop['target_arch'] = target_arch
    else:
        prop['prefer'] = True

    reexport_vndk_name = name
    if reexport_vndk_name == "libc++_static":
        reexport_vndk_name = "libc++"

    if reexport_vndk_name in vndk_list:
        if variation == 'shared':
            logging.error("Module %s is both vendor snapshot shared and vndk" % name)
        reexport_vndk_header(reexport_vndk_name, arch_props)

    # Factor out common prop among architectures to minimize Android.bp.
    common_prop = None
    for arch in arch_props:
        if common_prop is None:
            common_prop = dict()
            for k in arch_props[arch]:
                common_prop[k] = arch_props[arch][k]
            continue
        for k in list(common_prop.keys()):
            if k not in arch_props[arch] or common_prop[k] != arch_props[arch][k]:
                del common_prop[k]

    # Some keys has to be arch_props to prevent 32-bit only modules from being
    # used as 64-bit modules, and vice versa.
    for arch_prop_key in ['src', 'cfi']:
        if arch_prop_key in common_prop:
            del common_prop[arch_prop_key]
    prop.update(common_prop)

    has32 = has64 = False
    stem32 = stem64 = ''

    for arch in arch_props:
        for k in common_prop:
            if k in arch_props[arch]:
                del arch_props[arch][k]
        prop['arch'][arch] = arch_props[arch]

        has64 |= is_64bit_arch(arch)
        has32 |= not is_64bit_arch(arch)

        # Record stem for snapshots.
        # We don't check existence of 'src'; src must exist for executables
        if variation == 'binary':
            if is_64bit_arch(arch):
                stem64 = os.path.basename(arch_props[arch]['src'])
            else:
                stem32 = os.path.basename(arch_props[arch]['src'])

    # header snapshots doesn't need compile_multilib. The other snapshots,
    # shared/static/object/binary snapshots, do need them
    if variation != 'header' and variation != 'etc':
        if has32 and has64:
            prop['compile_multilib'] = 'both'
        elif has32:
            prop['compile_multilib'] = '32'
        elif has64:
            prop['compile_multilib'] = '64'
        else:
            raise RuntimeError("Module %s doesn't have prebuilts." % name)

    # For binary snapshots, prefer 64bit if their stem collide and installing
    # both is impossible
    if variation == 'binary' and stem32 == stem64:
        prop['compile_multilib'] = 'first'

    module_type = ''

    if variation == 'etc':
        module_type = 'snapshot_etc'
    else:
        module_type = '%s_snapshot_%s' % (image, variation)

    bp = module_type + ' {\n'
    bp += gen_bp_prop(prop, INDENT)
    bp += '}\n\n'
    return bp

def get_vndk_list(vndk_dir, target_arch):
    """Generates vndk_libs list, e.g. ['libbase', 'libc++', ...]
    This list is retrieved from vndk_dir/target_arch/configs/module_names.txt.
    If it doesn't exist, print an error message and return an empty list.
    """

    module_names_path = os.path.join(vndk_dir, target_arch, 'configs/module_names.txt')

    try:
        with open(module_names_path, 'r') as f:
            """The format of module_names.txt is a list of "{so_name} {module_name}", e.g.

                lib1.so lib1
                lib2.so lib2
                ...

            We extract the module name part.
            """
            return [l.split()[1] for l in f.read().strip('\n').split('\n')]
    except IOError as e:
        logging.error('Failed to read %s: %s' % (module_names_path, e.strerror))
    except IndexError as e:
        logging.error('Failed to parse %s: invalid format' % module_names_path)

    return []

def gen_bp_list_module(image, snapshot_version, vndk_list, target_arch, arch_props):
    """Generates a {image}_snapshot module which contains lists of snapshots.
    For vendor snapshot, vndk list is also included, extracted from vndk_dir.
    """

    bp = '%s_snapshot {\n' % image

    bp_props = dict()
    bp_props['name'] = '%s_snapshot' % image
    bp_props['version'] = str(snapshot_version)
    if image == 'vendor':
        bp_props['vndk_libs'] = vndk_list

    variant_to_property = {
        'shared': 'shared_libs',
        'static': 'static_libs',
        'rlib': 'rlibs',
        'header': 'header_libs',
        'binary': 'binaries',
        'object': 'objects',
    }

    # arch_bp_prop[arch][variant_prop] = list
    # e.g. arch_bp_prop['x86']['shared_libs'] == ['libfoo', 'libbar', ...]
    arch_bp_prop = dict()

    # Gather module lists per arch.
    # arch_props structure: arch_props[variant][module_name][arch]
    # e.g. arch_props['shared']['libc++']['x86']
    for variant in arch_props:
        if variant in variant_to_property:
            variant_name = variant_to_property[variant]
            for name in arch_props[variant]:
                for arch in arch_props[variant][name]:
                    if arch not in arch_bp_prop:
                        arch_bp_prop[arch] = dict()
                    if variant_name not in arch_bp_prop[arch]:
                        arch_bp_prop[arch][variant_name] = []
                    arch_bp_prop[arch][variant_name].append(name)

    bp_props['arch'] = arch_bp_prop
    bp += gen_bp_prop(bp_props, INDENT)

    bp += '}\n\n'
    return bp

def build_props(install_dir, image='', version=0 ):
    # props[target_arch]["static"|"shared"|"binary"|"header"|"license"][name][arch] : json
    props = dict()

    # {target_arch}/{arch}/{variation}/{module}.json
    for root, _, files in os.walk(install_dir, followlinks = True):
        for file_name in sorted(files):
            if not file_name.endswith('.json'):
                continue
            full_path = os.path.join(root, file_name)
            rel_path = os.path.relpath(full_path, install_dir)

            target_arch = get_target_arch(rel_path)
            arch = get_arch(rel_path)
            variation = get_variation(rel_path)
            bp_dir = os.path.join(install_dir, target_arch)

            if not target_arch in props:
                props[target_arch] = dict()
                props[target_arch]['license'] = dict()
            if not variation in props[target_arch]:
                props[target_arch][variation] = dict()

            with open(full_path, 'r') as f:
                prop, lic_prop = convert_json_to_bp_prop(f, bp_dir)
                # Remove .json after parsing?
                # os.unlink(full_path)

            if variation != 'header':
                prop['src'] = os.path.relpath(
                    rel_path[:-5],  # removing .json
                    target_arch)

            module_name = prop['name']

            # Is this sanitized variant?
            if 'sanitize' in prop:
                sanitizer_type = prop['sanitize']
                # module_name is {name}.{sanitizer_type}; trim sanitizer_type
                module_name = module_name[:-len(sanitizer_type) - 1]
                # Only leave props for the sanitize variant
                for k in list(prop.keys()):
                    if not k in SANITIZER_VARIANT_PROPS:
                        del prop[k]
                prop = {'name': module_name, sanitizer_type: prop}

            if not lic_prop:
                # This for the backward compatibility with the old snapshots
                notice_path = os.path.join(NOTICE_DIR, module_name + '.txt')
                if os.path.exists(os.path.join(bp_dir, notice_path)):
                    lic_prop['license_text'] = [notice_path]

            # Update license props
            if lic_prop and image and version:
                lic_name = '{image}-v{version}-{name}-license'.format(
                    image=image, version=version, name=module_name)
                if lic_name not in props[target_arch]['license']:
                    lic_prop['name'] = lic_name
                    props[target_arch]['license'][lic_name] = lic_prop
                else:
                    props[target_arch]['license'][lic_name].update(lic_prop)

                prop['licenses'] = [lic_name]

            variation_dict = props[target_arch][variation]
            if not module_name in variation_dict:
                variation_dict[module_name] = dict()
            if not arch in variation_dict[module_name]:
                variation_dict[module_name][arch] = prop
            else:
                variation_dict[module_name][arch].update(prop)

    return props

def convert_json_host_data_to_bp(mod, install_dir, version):
    """Create blueprint definition for a given host module.

    All host modules are created as a cc_prebuilt_binary
    blueprint module with the prefer attribute set to true.

    Modules that already have a prebuilt are not created.

    Args:
      mod: JSON definition of the module
      install_dir: installation directory of the host snapshot
      version: the version of the host snapshot
    """
    rust_proc_macro = mod.pop('RustProcMacro', False)
    prop, lic_prop = convert_json_data_to_bp_prop(mod, install_dir)
    if 'prebuilt' in prop:
        return

    if not rust_proc_macro:
        prop['host_supported'] = True
        prop['device_supported'] = False
        prop['stl'] = 'none'

    prop['prefer'] = True
    ## Move install file to host source file
    prop['target'] = dict()
    prop['target']['host'] = dict()
    prop['target']['host']['srcs'] = [prop['filename']]
    del prop['filename']

    bp = ''
    if lic_prop:
        lic_name = 'host-v{version}-{name}-license'.format(
                    version=version, name=prop['name'])
        lic_prop['name'] = lic_name
        prop['licenses'] = [lic_name]
        bp += 'license {\n'
        bp += gen_bp_prop(lic_prop, INDENT)
        bp += '}\n\n'

    mod_type = 'cc_prebuilt_binary'

    if rust_proc_macro:
        mod_type = 'rust_prebuilt_proc_macro'

    bp += mod_type + ' {\n' + gen_bp_prop(prop, INDENT) + '}\n\n'
    return bp

def gen_host_bp_file(install_dir, version):
    """Generate Android.bp for a host snapshot.

    This routine will find the JSON description file from a host
    snapshot and create a blueprint definition for each module
    and add to the created Android.bp file.

    Args:
      install_dir: directory where the host snapshot can be found
      version: the version of the host snapshot
    """
    bpfilename = 'Android.bp'
    with open(os.path.join(install_dir, bpfilename), 'w') as wfp:
        for file in os.listdir(install_dir):
            if file.endswith('.json'):
                with open(os.path.join(install_dir, file), 'r') as rfp:
                    props = json.load(rfp)
                    for mod in props:
                        prop = convert_json_host_data_to_bp(mod, install_dir, version)
                        if prop:
                            wfp.write(prop)

def gen_bp_files(image, vndk_dir, install_dir, snapshot_version):
    """Generates Android.bp for each archtecture.
    Android.bp will contain a {image}_snapshot module having lists of VNDK and
    vendor snapshot libraries, and {image}_snapshot_{variant} modules which are
    prebuilt libraries of the snapshot.

    Args:
      image: string, name of partition (e.g. 'vendor', 'recovery')
      vndk_dir: string, directory to which vndk snapshot is installed
      install_dir: string, directory to which the snapshot will be installed
      snapshot_version: int, version of the snapshot
    """
    props = build_props(install_dir, image, snapshot_version)

    for target_arch in sorted(props):
        androidbp = ''
        bp_dir = os.path.join(install_dir, target_arch)
        vndk_list = []
        if image == 'vendor':
            vndk_list = get_vndk_list(vndk_dir, target_arch)

        # Generate snapshot modules.
        for variation in sorted(props[target_arch]):
            for name in sorted(props[target_arch][variation]):
                androidbp += gen_bp_module(image, variation, name,
                                           snapshot_version, target_arch,
                                           vndk_list,
                                           props[target_arch][variation][name],
                                           bp_dir)

        # Generate {image}_snapshot module which contains the list of modules.
        androidbp += gen_bp_list_module(image, snapshot_version, vndk_list,
                                        target_arch, props[target_arch])
        with open(os.path.join(bp_dir, 'Android.bp'), 'w') as f:
            logging.info('Generating Android.bp to: {}'.format(f.name))
            f.write(androidbp)


def find_all_installed_files(install_dir):
    installed_files = dict()
    for root, _, files in os.walk(install_dir, followlinks = True):
        for file_name in sorted(files):
            if file_name.endswith('.json'):
                continue
            if file_name.endswith('Android.bp'):
                continue
            full_path = os.path.join(root, file_name)
            size = os.stat(full_path).st_size
            installed_files[full_path] = size

    logging.debug('')
    for f in sorted(installed_files.keys()):
        logging.debug(f)
    logging.debug('')
    logging.debug('found {} installed files'.format(len(installed_files)))
    logging.debug('')
    return installed_files


def find_files_in_props(target_arch, arch_install_dir, variation, name, props, file_to_info):
    logging.debug('{} {} {} {} {}'.format(
        target_arch, arch_install_dir, variation, name, props))

    def add_info(file, name, variation, arch, is_sanitized, is_header):
        info = (name, variation, arch, is_sanitized, is_header)
        info_list = file_to_info.get(file)
        if not info_list:
            info_list = []
            file_to_info[file] = info_list
        info_list.append(info)

    def find_file_in_list(dict, key, is_sanitized):
        list = dict.get(key)
        logging.debug('    {} {}'.format(key, list))
        if list:
            for item in list:
                item_path = os.path.join(arch_install_dir, item)
                add_info(item_path, name, variation, arch, is_sanitized, False)

    def find_file_in_dirs(dict, key, is_sanitized, is_header):
        dirs = dict.get(key)
        logging.debug('    {} {}'.format(key, dirs))
        if dirs:
            for dir in dirs:
                dir_path = os.path.join(arch_install_dir, dir)
                logging.debug('        scanning {}'.format(dir_path))
                for root, _, files in os.walk(dir_path, followlinks = True):
                    for file_name in sorted(files):
                        item_path = os.path.join(root, file_name)
                        add_info(item_path, name, variation, arch, is_sanitized, is_header)

    def find_file_in_dict(dict, is_sanitized):
        logging.debug('    arch {}'.format(arch))
        logging.debug('    name {}'.format( name))
        logging.debug('    is_sanitized {}'.format(is_sanitized))

        src = dict.get('src')
        logging.debug('    src {}'.format(src))
        if src:
            src_path = os.path.join(arch_install_dir, src)
            add_info(src_path, name, variation, arch, is_sanitized, False)

        notice = dict.get('notice')
        logging.debug('    notice {}'.format(notice))
        if notice:
            notice_path = os.path.join(arch_install_dir, notice)
            add_info(notice_path, name, variation, arch, is_sanitized, False)

        find_file_in_list(dict, 'init_rc', is_sanitized)
        find_file_in_list(dict, 'vintf_fragments', is_sanitized)

        find_file_in_dirs(dict, 'export_include_dirs', is_sanitized, True)
        find_file_in_dirs(dict, 'export_system_include_dirs', is_sanitized, True)

    for arch in sorted(props):
        name = props[arch]['name']
        find_file_in_dict(props[arch], False)
        cfi = props[arch].get('cfi')
        if cfi:
            find_file_in_dict(cfi, True)
        hwasan = props[arch].get('hwasan')
        if hwasan:
            find_file_in_dict(hwasan, True)


def find_all_props_files(install_dir):

    # This function builds a database of filename to module. This means that we
    # need to dive into the json to find the files that the vendor snapshot
    # provides, and link these back to modules that provide them.

    file_to_info = dict()

    props = build_props(install_dir)
    for target_arch in sorted(props):
        arch_install_dir = os.path.join(install_dir, target_arch)
        for variation in sorted(props[target_arch]):
            for name in sorted(props[target_arch][variation]):
                find_files_in_props(
                    target_arch,
                    arch_install_dir,
                    variation,
                    name,
                    props[target_arch][variation][name],
                    file_to_info)

    logging.debug('')
    for f in sorted(file_to_info.keys()):
        logging.debug(f)
    logging.debug('')
    logging.debug('found {} props files'.format(len(file_to_info)))
    logging.debug('')
    return file_to_info


def get_ninja_inputs(ninja_binary, ninja_build_file, modules):
    """Returns the set of input file path strings for the given modules.

    Uses the `ninja -t inputs` tool.

    Args:
        ninja_binary: The path to a ninja binary.
        ninja_build_file: The path to a .ninja file from a build.
        modules: The list of modules to scan for inputs.
    """
    inputs = set()
    cmd = [
        ninja_binary,
        "-f",
        ninja_build_file,
        "-t",
        "inputs",
        "-d",
    ] + list(modules)
    logging.debug('invoke ninja {}'.format(cmd))
    inputs = inputs.union(set(
        subprocess.check_output(cmd).decode().strip("\n").split("\n")))

    return inputs

def check_host_usage(install_dir, ninja_binary, ninja_file, goals, output):
    """Find the host modules that are in the ninja build dependency for a given goal.

    To use this routine the user has installed a fake host snapshot with all
    possible host tools into the install directory.  Check the ninja build
    graph for any mapping to the modules in this installation directory.

    This routine will print out a vsdk_host_tools = [] statement with the
    dependent tools.  The user can then use the vsdk_host_tools as the
    deps of their host_snapshot module.
    """
    file_to_info = dict()
    for file in os.listdir(install_dir):
        if file.endswith('.json'):
            with open(os.path.join(install_dir,file),'r') as rfp:
                props = json.load(rfp)
                for mod in props:
                    file_to_info[os.path.join(install_dir,mod['Filename'])] = mod['ModuleName']

    used_modules = set()
    ninja_inputs = get_ninja_inputs(ninja_binary, ninja_file, goals)
    ## Check for host file in ninja inputs
    for file in file_to_info:
        if file in ninja_inputs:
            used_modules.add(file_to_info[file])

    with open(output, 'w') as f:
        f.write('vsdk_host_tools = [ \n')
        for m in sorted(used_modules):
            f.write('  "%s",\n' % m)
        f.write('] \n')

def check_module_usage(install_dir, ninja_binary, image, ninja_file, goals,
                       output):
    all_installed_files = find_all_installed_files(install_dir)
    all_props_files = find_all_props_files(install_dir)

    ninja_inputs = get_ninja_inputs(ninja_binary, ninja_file, goals)
    logging.debug('')
    logging.debug('ninja inputs')
    for ni in ninja_inputs:
        logging.debug(ni)

    logging.debug('found {} ninja_inputs for goals {}'.format(
        len(ninja_inputs), goals))

    # Intersect the file_to_info dict with the ninja_inputs to determine
    # which items from the vendor snapshot are actually used by the goals.

    total_size = 0
    used_size = 0
    used_file_to_info = dict()

    for file, size in all_installed_files.items():
        total_size += size
        if file in ninja_inputs:
            logging.debug('used: {}'.format(file))
            used_size += size
            info = all_props_files.get(file)

            if info:
                used_file_to_info[file] = info
            else:
                logging.warning('No info for file {}'.format(file))
                used_file_to_info[file] = 'no info'

    logging.debug('Total size {}'.format(total_size))
    logging.debug('Used size {}'.format(used_size))
    logging.debug('')
    logging.debug('used items')

    used_modules = set()

    for f, i in sorted(used_file_to_info.items()):
        logging.debug('{} {}'.format(f, i))
        for m in i:
            (name, variation, arch, is_sanitized, is_header) = m
            if not is_header:
                used_modules.add(name)

    with open(output, 'w') as f:
        f.write('%s_SNAPSHOT_MODULES := \\\n' % image.upper())
        for m in sorted(used_modules):
            f.write('  %s \\\n' % m)

def check_call(cmd):
    logging.debug('Running `{}`'.format(' '.join(cmd)))
    subprocess.check_call(cmd)


def fetch_artifact(branch, build, target, pattern, destination):
    """Fetches build artifacts from Android Build server.

    Args:
      branch: string, branch to pull build artifacts from
      build: string, build number to pull build artifacts from
      target: string, target name to pull build artifacts from
      pattern: string, pattern of build artifact file name
      destination: string, destination to pull build artifact to
    """
    fetch_artifact_path = '/google/data/ro/projects/android/fetch_artifact'
    cmd = [
        fetch_artifact_path, '--branch', branch, '--target', target, '--bid',
        build, pattern, destination
    ]
    check_call(cmd)

def install_artifacts(image, branch, build, target, local_dir, symlink,
                      install_dir):
    """Installs vendor snapshot build artifacts to {install_dir}/v{version}.

    1) Fetch build artifacts from Android Build server or from local_dir
    2) Unzip or create symlinks to build artifacts

    Args:
      image: string, img file for which the snapshot was created (vendor,
             recovery, etc.)
      branch: string or None, branch name of build artifacts
      build: string or None, build number of build artifacts
      target: string or None, target name of build artifacts
      local_dir: string or None, local dir to pull artifacts from
      symlink: boolean, whether to use symlinks instead of unzipping the
        vendor snapshot zip
      install_dir: string, directory to install vendor snapshot
      temp_artifact_dir: string, temp directory to hold build artifacts fetched
        from Android Build server. For 'local' option, is set to None.
    """
    artifact_pattern = image + '-*.zip'

    def unzip_artifacts(artifact_dir):
        artifacts = glob.glob(os.path.join(artifact_dir, artifact_pattern))
        for artifact in artifacts:
            logging.info('Unzipping Vendor snapshot: {}'.format(artifact))
            check_call(['unzip', '-qn', artifact, '-d', install_dir])

    if branch and build and target:
        with tempfile.TemporaryDirectory() as tmpdir:
            logging.info(
                'Fetching {pattern} from {branch} (bid: {build}, target: {target})'
                .format(
                    pattern=artifact_pattern,
                    branch=branch,
                    build=build,
                    target=target))
            fetch_artifact(branch, build, target, artifact_pattern, tmpdir)
            unzip_artifacts(tmpdir)
    elif local_dir:
        if symlink:
            # This assumes local_dir is the location of vendor-snapshot in the
            # build (e.g., out/soong/vendor-snapshot).
            #
            # Create the first level as proper directories and the next level
            # as symlinks.
            for item1 in os.listdir(local_dir):
                dest_dir = os.path.join(install_dir, item1)
                src_dir = os.path.join(local_dir, item1)
                if os.path.isdir(src_dir):
                    check_call(['mkdir', '-p', dest_dir])
                    # Create symlinks.
                    for item2 in os.listdir(src_dir):
                        src_item = os.path.join(src_dir, item2)
                        logging.info('Creating symlink from {} in {}'.format(
                            src_item, dest_dir))
                        os.symlink(src_item, os.path.join(dest_dir, item2))
        else:
            logging.info('Fetching local VNDK snapshot from {}'.format(
                local_dir))
            unzip_artifacts(local_dir)
    else:
        raise RuntimeError('Neither local nor remote fetch information given.')

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'snapshot_version',
        type=int,
        help='Vendor snapshot version to install, e.g. "30".')
    parser.add_argument(
        '--image',
        help=('Image whose snapshot is being updated (e.g., vendor, '
              'recovery , ramdisk, host, etc.)'),
        default='vendor')
    parser.add_argument('--branch', help='Branch to pull build from.')
    parser.add_argument('--build', help='Build number to pull.')
    parser.add_argument('--target', help='Target to pull.')
    parser.add_argument(
        '--local',
        help=('Fetch local vendor snapshot artifacts from specified local '
              'directory instead of Android Build server. '
              'Example: --local /path/to/local/dir'))
    parser.add_argument(
        '--symlink',
        action='store_true',
        help='Use symlinks instead of unzipping vendor snapshot zip')
    parser.add_argument(
        '--install-dir',
        required=True,
        help=(
            'Base directory to which vendor snapshot artifacts are installed. '
            'Example: --install-dir vendor/<company name>/vendor_snapshot/v30'))
    parser.add_argument(
        '--overwrite',
        action='store_true',
        help=(
            'If provided, does not ask before overwriting the install-dir.'))
    parser.add_argument(
        '--check-module-usage',
        action='store_true',
        help='Check which modules are used.')
    parser.add_argument(
        '--check-module-usage-goal',
        action='append',
        help='Goal(s) for which --check-module-usage is calculated.')
    parser.add_argument(
        '--check-module-usage-ninja-file',
        help='Ninja file for which --check-module-usage is calculated.')
    parser.add_argument(
        '--check-module-usage-output',
        help='File to which to write the check-module-usage results.')
    parser.add_argument(
        '--vndk-dir',
        help='Path to installed vndk snapshot directory. Needed to retrieve '
             'the list of VNDK. prebuilts/vndk/v{ver} will be used by default.')

    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()


def main():
    """Program entry point."""
    args = get_args()

    host_image = args.image == 'host'
    verbose_map = (logging.WARNING, logging.INFO, logging.DEBUG)
    verbosity = min(args.verbose, 2)
    logging.basicConfig(
        format='%(levelname)-8s [%(filename)s:%(lineno)d] %(message)s',
        level=verbose_map[verbosity])

    if not args.install_dir:
        raise ValueError('Please provide --install-dir option.')
    install_dir = os.path.expanduser(args.install_dir)

    if args.check_module_usage:
        ninja_binary = './prebuilts/build-tools/linux-x86/bin/ninja'

        if not args.check_module_usage_goal:
            raise ValueError('Please provide --check-module-usage-goal option.')
        if not args.check_module_usage_ninja_file:
            raise ValueError(
                'Please provide --check-module-usage-ninja-file option.')
        if not args.check_module_usage_output:
            raise ValueError(
                'Please provide --check-module-usage-output option.')

        if host_image:
            check_host_usage(install_dir, ninja_binary,
                             args.check_module_usage_ninja_file,
                             args.check_module_usage_goal,
                             args.check_module_usage_output)
        else:
            check_module_usage(install_dir, ninja_binary, args.image,
                               args.check_module_usage_ninja_file,
                               args.check_module_usage_goal,
                               args.check_module_usage_output)
        return

    local = None
    if args.local:
        local = os.path.expanduser(args.local)

    if local:
        if args.build or args.branch or args.target:
            raise ValueError(
                'When --local option is set, --branch, --build or --target cannot be '
                'specified.')
        elif not os.path.isdir(local):
            raise RuntimeError(
                'The specified local directory, {}, does not exist.'.format(
                    local))
    else:
        if not (args.build and args.branch and args.target):
            raise ValueError(
                'Please provide --branch, --build and --target. Or set --local '
                'option.')

    snapshot_version = args.snapshot_version

    if os.path.exists(install_dir):
        def remove_dir():
            logging.info('Removing {}'.format(install_dir))
            check_call(['rm', '-rf', install_dir])
        if args.overwrite:
            remove_dir()
        else:
            resp = input('Directory {} already exists. IT WILL BE REMOVED.\n'
                         'Are you sure? (yes/no): '.format(install_dir))
            if resp == 'yes':
                remove_dir()
            elif resp == 'no':
                logging.info('Cancelled snapshot install.')
                return
            else:
                raise ValueError('Did not understand: ' + resp)
    check_call(['mkdir', '-p', install_dir])

    if args.vndk_dir:
        vndk_dir = os.path.expanduser(args.vndk_dir)
    else:
        vndk_dir = 'prebuilts/vndk/v%d' % snapshot_version
        logging.debug('Using %s for vndk directory' % vndk_dir)

    install_artifacts(
        image=args.image,
        branch=args.branch,
        build=args.build,
        target=args.target,
        local_dir=local,
        symlink=args.symlink,
        install_dir=install_dir)

    if host_image:
        gen_host_bp_file(install_dir, snapshot_version)

    else:
        gen_bp_files(args.image, vndk_dir, install_dir, snapshot_version)

if __name__ == '__main__':
    main()
