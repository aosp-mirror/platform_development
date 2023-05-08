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
#

import argparse
from collections import defaultdict
import glob
import json
import logging
import os
import sys

import collect_licenses
import utils


class GenBuildFile(object):
    """Generates Android.bp for VNDK snapshot.

    VNDK snapshot directory structure under prebuilts/vndk/v{version}:
        Android.bp
        {SNAPSHOT_ARCH}/
            Android.bp
            arch-{TARGET_ARCH}-{TARGET_ARCH_VARIANT}/
                shared/
                    vndk-core/
                        (VNDK-core libraries, e.g. libbinder.so)
                    vndk-sp/
                        (VNDK-SP libraries, e.g. libc++.so)
            arch-{TARGET_2ND_ARCH}-{TARGET_2ND_ARCH_VARIANT}/
                shared/
                    vndk-core/
                        (VNDK-core libraries, e.g. libbinder.so)
                    vndk-sp/
                        (VNDK-SP libraries, e.g. libc++.so)
            binder32/
                (This directory is newly introduced in v28 (Android P) to hold
                prebuilts built for 32-bit binder interface.)
                Android.bp
                arch-{TARGET_ARCH}-{TARGE_ARCH_VARIANT}/
                    ...
            configs/
                (various *.txt configuration files, e.g. ld.config.*.txt)
        ... (other {SNAPSHOT_ARCH}/ directories)
        common/
            Android.bp
            NOTICE_FILES/
                (license files, e.g. libfoo.so.txt)
    """
    INDENT = '    '
    ETC_MODULES = [
        'llndk.libraries.txt',
        'vndksp.libraries.txt',
        'vndkcore.libraries.txt',
        'vndkprivate.libraries.txt',
        'vndkproduct.libraries.txt',
    ]

    def __init__(self, install_dir, vndk_version):
        """GenBuildFile constructor.

        Args:
          install_dir: string, absolute path to the prebuilts/vndk/v{version}
            directory where the build files will be generated.
          vndk_version: int, VNDK snapshot version (e.g. 30)
        """
        self._install_dir = install_dir
        self._vndk_version = vndk_version
        self._etc_paths = self._get_etc_paths()
        self._snapshot_archs = utils.get_snapshot_archs(install_dir)
        self._root_bpfile = os.path.join(install_dir, utils.ROOT_BP_PATH)
        self._common_bpfile = os.path.join(install_dir, utils.COMMON_BP_PATH)
        self._llndk = self._parse_lib_list(
            os.path.basename(self._etc_paths['llndk.libraries.txt']))
        self._vndk_core = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndkcore.libraries.txt']))
        self._vndk_sp = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndksp.libraries.txt']))
        self._vndk_private = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndkprivate.libraries.txt']))
        self._vndk_product = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndkproduct.libraries.txt']))
        self._modules_with_notice = self._get_modules_with_notice()
        self._license_in_json = not self._modules_with_notice
        self._license_kinds_map = defaultdict(set)
        self._license_texts_map = defaultdict(set)
        self.modules_with_restricted_lic = set()

    def _get_etc_paths(self):
        """Returns a map of relative file paths for each ETC module."""

        etc_paths = dict()
        for etc_module in self.ETC_MODULES:
            etc_pattern = '{}*'.format(os.path.splitext(etc_module)[0])
            globbed = glob.glob(
                os.path.join(self._install_dir, utils.CONFIG_DIR_PATH_PATTERN,
                             etc_pattern))
            if len(globbed) > 0:
                rel_etc_path = globbed[0].replace(self._install_dir, '')[1:]
                etc_paths[etc_module] = rel_etc_path
        return etc_paths

    def _parse_lib_list(self, txt_filename):
        """Returns a map of VNDK library lists per VNDK snapshot arch.

        Args:
          txt_filename: string, name of snapshot config file

        Returns:
          dict, e.g. {'arm64': ['libfoo.so', 'libbar.so', ...], ...}
        """
        lib_map = dict()
        for txt_path in utils.find(self._install_dir, [txt_filename]):
            arch = utils.snapshot_arch_from_path(txt_path)
            abs_path_of_txt = os.path.join(self._install_dir, txt_path)
            with open(abs_path_of_txt, 'r') as f:
                lib_map[arch] = f.read().strip().split('\n')
            if lib_map[arch] == ['']:
                lib_map[arch].clear()
        return lib_map

    def _get_modules_with_notice(self):
        """Returns a list of modules that have associated notice files. """
        notice_paths = glob.glob(
            os.path.join(self._install_dir, utils.NOTICE_FILES_DIR_PATH,
                         '*.txt'))
        return sorted(os.path.splitext(os.path.basename(p))[0] for p in notice_paths)

    def generate_root_android_bp(self):
        """Autogenerates Android.bp."""

        logging.info('Generating Android.bp for snapshot v{}'.format(
            self._vndk_version))
        prebuilt_buildrules = []
        for prebuilt in self.ETC_MODULES:
            prebuilt_buildrules.append(self._gen_etc_prebuilt(prebuilt))

        with open(self._root_bpfile, 'w') as bpfile:
            bpfile.write(self._gen_autogen_msg('/'))
            bpfile.write('\n')
            bpfile.write(self._gen_license_package())
            bpfile.write('\n')
            bpfile.write(self._gen_license())
            bpfile.write('\n')
            bpfile.write('\n'.join(prebuilt_buildrules))
            bpfile.write('\n')

        logging.info('Successfully generated {}'.format(self._root_bpfile))

    def generate_common_android_bp(self):
        """Autogenerates common/Android.bp."""

        logging.info('Generating common/Android.bp for snapshot v{}'.format(
            self._vndk_version))
        with open(self._common_bpfile, 'w') as bpfile:
            bpfile.write(self._gen_autogen_msg('/'))
            bpfile.write('\n')
            bpfile.write(self._gen_license_package())
            if self._license_in_json:
                for name in self._license_kinds_map:
                    bpfile.write('\n')
                    bpfile.write(self._gen_notice_license(name))
            else:
                for module in self._modules_with_notice:
                    bpfile.write('\n')
                    bpfile.write(self._gen_notice_license(module))

    def generate_android_bp(self):
        """Autogenerates Android.bp."""

        def gen_for_variant(arch, is_binder32=False):
            """Generates Android.bp file for specified VNDK snapshot variant.

            A VNDK snapshot variant is defined by the TARGET_ARCH and binder
            bitness. Example snapshot variants:
                vndk_v{ver}_arm:            {arch: arm, binder: 64-bit}
                vndk_v{ver}_arm_binder32:   {arch: arm, binder: 32-bit}

            Args:
              arch: string, VNDK snapshot arch (e.g. 'arm64')
              is_binder32: bool, True if binder interface is 32-bit
            """
            binder32_suffix = '_{}'.format(
                utils.BINDER32) if is_binder32 else ''
            logging.info('Generating Android.bp for vndk_v{}_{}{}'.format(
                self._vndk_version, arch, binder32_suffix))

            src_root = os.path.join(self._install_dir, arch)
            module_names_txt = os.path.join(
                src_root, "configs", "module_names.txt")
            module_names = dict()
            try:
                with open(module_names_txt, 'r') as f:
                    # Remove empty lines from module_names_txt
                    module_list = filter(None, f.read().split('\n'))
                for module in module_list:
                    lib, name = module.split(' ')
                    module_names[lib] = name
            except IOError:
                # If module_names.txt doesn't exist, ignore it and parse
                # module names out from .so filenames. (old snapshot)
                pass

            variant_subpath = arch
            if is_binder32:
                variant_subpath = os.path.join(arch, utils.BINDER32)
            variant_path = os.path.join(self._install_dir, variant_subpath)
            bpfile_path = os.path.join(variant_path, 'Android.bp')

            vndk_core_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_core[arch],
                arch,
                is_llndk=False,
                is_vndk_sp=False,
                is_binder32=is_binder32,
                module_names=module_names)
            vndk_sp_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_sp[arch],
                arch,
                is_llndk=False,
                is_vndk_sp=True,
                is_binder32=is_binder32,
                module_names=module_names)
            include_llndk = self._vndk_version > 30
            if include_llndk:
                llndk_buildrules = self._gen_vndk_shared_prebuilts(
                    self._llndk[arch],
                    arch,
                    is_llndk=True,
                    is_vndk_sp=False,
                    is_binder32=is_binder32,
                    module_names=module_names)

            with open(bpfile_path, 'w') as bpfile:
                bpfile.write(self._gen_autogen_msg('/'))
                bpfile.write('\n')
                bpfile.write(self._gen_license_package())
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_core_buildrules))
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_sp_buildrules))
                if include_llndk:
                    bpfile.write('\n')
                    bpfile.write('\n'.join(llndk_buildrules))

            variant_include_path = os.path.join(variant_path, 'include')
            include_path = os.path.join(self._install_dir, arch, 'include')
            if os.path.isdir(include_path) and variant_include_path != include_path:
                os.symlink(os.path.relpath(include_path, variant_path),
                    variant_include_path)

            logging.info('Successfully generated {}'.format(bpfile_path))

        for arch in self._snapshot_archs:
            if os.path.isdir(
                    os.path.join(self._install_dir, arch, utils.BINDER32)):
                gen_for_variant(arch, is_binder32=True)
            gen_for_variant(arch)

    def _gen_autogen_msg(self, comment_char):
        return ('{0}{0} THIS FILE IS AUTOGENERATED BY '
                'development/vndk/snapshot/gen_buildfiles.py\n'
                '{0}{0} DO NOT EDIT\n'.format(comment_char))

    def _gen_license_package(self):
        """ Generates license package for VNDK snapshot libs """
        return ('package {{\n'
                '{ind}default_applicable_licenses: ["prebuilts_vndk_v{version}_license"],\n'
                '}}\n'.format(
                    ind=self.INDENT,
                    version=self._vndk_version))

    def _get_license_kinds(self, module=''):
        """ Returns a set of license kinds

        Args:
          module: module name to find the license kind.
                  If empty, check all license files.
        """
        if self._license_in_json:
            license_kinds = set()
            if module == '':
                # collect all license kinds
                for kinds in self._license_kinds_map.values():
                    license_kinds.update(kinds)
                return license_kinds
            else:
                return self._license_kinds_map[module]

        license_collector = collect_licenses.LicenseCollector(self._install_dir)
        license_collector.run(module)
        return license_collector.license_kinds

    def _get_license_texts(self, module):
        if self._license_in_json:
            return {'{notice_dir}/{license_text}'.format(
                        notice_dir=utils.NOTICE_FILES_DIR_NAME,
                        license_text=license_text)
                        for license_text in self._license_texts_map[module]}
        else:
            return {'{notice_dir}/{module}.txt'.format(
                        notice_dir=utils.NOTICE_FILES_DIR_NAME,
                        module=module)}

    def _gen_license(self):
        """ Generates license module.

        It uses license files for all VNDK snapshot libraries in common/NOTICE_FILES directory.
        """
        license_kinds = self._get_license_kinds()
        license_kinds_string = ''
        for license_kind in sorted(license_kinds):
            license_kinds_string += '{ind}{ind}"{license_kind}",\n'.format(
                                    ind=self.INDENT, license_kind=license_kind)
        return ('license {{\n'
                '{ind}name: "prebuilts_vndk_v{version}_license",\n'
                '{ind}visibility: [":__subpackages__"],\n'
                '{ind}license_kinds: [\n'
                '{license_kinds}'
                '{ind}],\n'
                '{ind}license_text: ["{notice_files}"],\n'
                '}}\n'.format(
                    ind=self.INDENT,
                    version=self._vndk_version,
                    license_kinds=license_kinds_string,
                    notice_files=os.path.join(utils.NOTICE_FILES_DIR_PATH, '**', '*')))

    def _get_versioned_name(self,
                            prebuilt,
                            arch,
                            is_etc=False,
                            is_binder32=False,
                            module_names=None):
        """Returns the VNDK version-specific module name for a given prebuilt.

        The VNDK version-specific module name is defined as follows:
        For a VNDK shared lib: 'libfoo.so'
            if binder is 32-bit:
                'libfoo.vndk.{version}.{arch}.binder32.vendor'
            else:
                'libfoo.vndk.{version}.{arch}.vendor'
        For an ETC module: 'foo.txt' -> 'foo.{version}.txt'

        Args:
          prebuilt: string, name of the prebuilt object
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          is_etc: bool, True if the LOCAL_MODULE_CLASS of prebuilt is 'ETC'
          is_binder32: bool, True if binder interface is 32-bit
          module_names: dict, module names for given prebuilts
        """
        if is_etc:
            name, ext = os.path.splitext(prebuilt)
            versioned_name = '{}.{}{}'.format(name, self._vndk_version, ext)
        else:
            module_names = module_names or dict()
            if prebuilt in module_names:
                name = module_names[prebuilt]
            else:
                name = os.path.splitext(prebuilt)[0]
            binder_suffix = '.{}'.format(utils.BINDER32) if is_binder32 else ''
            versioned_name = '{}.vndk.{}.{}{}.vendor'.format(
                name, self._vndk_version, arch, binder_suffix)

        return versioned_name

    def _gen_etc_prebuilt(self, prebuilt):
        """Generates build rule for an ETC prebuilt.

        Args:
          prebuilt: string, name of ETC prebuilt object
        """
        etc_path = self._etc_paths[prebuilt]
        etc_sub_path = etc_path[etc_path.index('/') + 1:]

        prebuilt_etc = ('prebuilt_etc {{\n'
                        '{ind}name: "{versioned_name}",\n'
                        '{ind}target: {{\n'.format(
                            ind=self.INDENT,
                            versioned_name=self._get_versioned_name(
                                prebuilt, None, is_etc=True)))
        for arch in self._snapshot_archs:
            prebuilt_etc += ('{ind}{ind}android_{arch}: {{\n'
                             '{ind}{ind}{ind}src: "{arch}/{etc_sub_path}",\n'
                             '{ind}{ind}}},\n'.format(
                                 ind=self.INDENT,
                                 arch=arch,
                                 etc_sub_path=etc_sub_path))
        prebuilt_etc += ('{ind}}},\n'
                         '}}\n'.format(ind=self.INDENT))
        return prebuilt_etc

    def _gen_prebuilt_library_shared(self, prebuilt_lib_info):
        """Generates cc_prebuilt_library_shared modules for the old vendor
        compatibility.

        Some vendor modules still require old version of libraries that is not
        available from the current source tree. To provide the old copy of the
        libraries, use the vndk snapshot.

        Args:
            prebuilt_lib_info: pair of (string, list of strings), name of the
                        prebuilt library and the list of shared libs for it.
        """
        lib_name = prebuilt_lib_info[0]
        shared_libs = prebuilt_lib_info[1]

        shared_libs_prop = ''
        if shared_libs:
            shared_libs_prop = ('{ind}shared_libs: [\n'.format(ind=self.INDENT))
            for lib in shared_libs:
                shared_libs_prop += ('{ind}{ind}"{lib}",\n'.format(
                                        ind=self.INDENT, lib=lib))
            shared_libs_prop += ('{ind}],\n'.format(ind=self.INDENT))

        cc_prebuilt_libraries = ('cc_prebuilt_library_shared {{\n'
                                 '{ind}name: "{name}-vendorcompat",\n'
                                 '{ind}stem: "{name}",\n'
                                 '{ind}vendor: true,\n'
                                 '{ind}// These are already stripped, and '
                                 'restripping them just issues diagnostics.\n'
                                 '{ind}strip: {{\n'
                                 '{ind}{ind}none: true,\n'
                                 '{ind}}},\n'
                                 '{shared_libs}'
                                 '{ind}target: {{\n'.format(
                                    ind=self.INDENT,
                                    name=lib_name,
                                    shared_libs=shared_libs_prop))
        src_paths = utils.find(self._install_dir, [lib_name+'.so'])
        for src in src_paths:
            dirs = src.split(os.path.sep)
            if len(dirs) < 3 or not dirs[1].startswith('arch-{}-'.format(dirs[0])):
                continue
            cc_prebuilt_libraries += ('{ind}{ind}android_{arch}: {{\n'
                                      '{ind}{ind}{ind}srcs: ["{src}"],\n'
                                      '{ind}{ind}}},\n'.format(
                                        ind=self.INDENT, arch=dirs[0], src=src))
        cc_prebuilt_libraries += ('{ind}}},\n'
                                  '}}\n'.format(ind=self.INDENT))
        return cc_prebuilt_libraries

    def _gen_notice_license(self, module):
        """Generates a notice license build rule for a given module.
        When genererating each notice license, collect
        modules_with_restricted_lic, the list of modules that are under the GPL.

        Args:
          module: string, module name
        """
        def has_restricted_license(license_kinds):
            for lic in license_kinds:
                if 'GPL' in lic:
                    return True
            return False

        license_kinds = self._get_license_kinds(module)
        if has_restricted_license(license_kinds):
            self.modules_with_restricted_lic.add(module)
        license_kinds_string = ''
        for license_kind in sorted(license_kinds):
            license_kinds_string += '{ind}{ind}"{license_kind}",\n'.format(
                                    ind=self.INDENT, license_kind=license_kind)
        license_texts = self._get_license_texts(module)
        license_texts_string = ''
        for license_text in sorted(license_texts):
            license_texts_string += '{ind}{ind}"{license_text}",\n'.format(
                                    ind=self.INDENT, license_text=license_text)
        return ('license {{\n'
                '{ind}name: "{license_name}",\n'
                '{ind}license_kinds: [\n'
                '{license_kinds}'
                '{ind}],\n'
                '{ind}license_text: [\n'
                '{license_texts}'
                '{ind}],\n'
                '}}\n'.format(
                    ind=self.INDENT,
                    license_name=self._get_notice_license_name(module),
                    license_kinds=license_kinds_string,
                    license_texts=license_texts_string))

    def _get_notice_license_name(self, module):
        """ Gets a notice license module name for a given module.

        Args:
          notice: string, module name.
        """
        return 'vndk-v{ver}-{module}-license'.format(
            ver=self._vndk_version, module=module)

    def _gen_vndk_shared_prebuilts(self,
                                   prebuilts,
                                   arch,
                                   is_llndk,
                                   is_vndk_sp,
                                   is_binder32,
                                   module_names):
        """Returns list of build rules for given prebuilts.

        Args:
          prebuilts: list of VNDK shared prebuilts
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          is_llndk: bool, True if the prebuilts are LLNDK stubs
          is_vndk_sp: bool, True if prebuilts are VNDK_SP libs
          is_binder32: bool, True if binder interface is 32-bit
          module_names: dict, module names for given prebuilts
        """

        module_prebuilts = dict()
        for prebuilt in prebuilts:
            if prebuilt in module_names:
                name = module_names[prebuilt]
            else:
                name = os.path.splitext(prebuilt)[0]

            if name not in module_prebuilts:
                module_prebuilts[name] = list()
            module_prebuilts[name].append(prebuilt)

        build_rules = []
        for name in module_prebuilts:
            bp_module = self._gen_vndk_shared_prebuilt(
                name,
                arch,
                srcs=module_prebuilts[name],
                is_llndk=is_llndk,
                is_vndk_sp=is_vndk_sp,
                is_binder32=is_binder32)
            if bp_module:
                build_rules.append(bp_module)
        return build_rules

    def _gen_vndk_shared_prebuilt(self,
                                  name,
                                  arch,
                                  srcs,
                                  is_llndk,
                                  is_vndk_sp,
                                  is_binder32):
        """Returns build rule for given prebuilt module, or an empty
        string if the module is invalid (e.g. srcs doesn't exist).

        Args:
          name: string, name of prebuilt module
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          srcs: list, prebuilt source file names of this module
          is_llndk: bool, True if prebuilt is a LLNDK stub
          is_vndk_sp: bool, True if prebuilt is a VNDK_SP lib
          is_binder32: bool, True if binder interface is 32-bit
        """

        def is_prebuilts_in_list(prebuilts, vndk_list):
            for prebuilt in prebuilts:
                if prebuilt in vndk_list:
                    return True
            return False

        def get_license_prop(name):
            """Returns the license prop build rule.

            Args:
              name: string, name of the module
            """
            if name in self._license_kinds_map:
                return '{ind}licenses: ["{license}"],\n'.format(
                        ind=self.INDENT,
                        license=self._get_notice_license_name(name))
            return ''

        def get_notice_file(prebuilts):
            """Returns build rule for notice file (attribute 'licenses').

            Args:
              prebuilts: list, names of prebuilt objects
            """
            notice = ''
            for prebuilt in prebuilts:
                if prebuilt in self._modules_with_notice:
                    notice = '{ind}licenses: ["{notice_license}"],\n'.format(
                        ind=self.INDENT,
                        notice_license=self._get_notice_license_name(prebuilt))
                    break
            return notice

        def get_arch_props(name, arch, srcs_props):
            """Returns build rule for arch specific srcs.

            e.g.,
                arch: {
                    arm: {
                        export_include_dirs: ["..."],
                        export_system_include_dirs: ["..."],
                        export_flags: ["..."],
                        relative_install_path: "...",
                        srcs: ["..."]
                    },
                    arm64: {
                        export_include_dirs: ["..."],
                        export_system_include_dirs: ["..."],
                        export_flags: ["..."],
                        relative_install_path: "...",
                        srcs: ["..."]
                    },
                }

            Args:
              name: string, name of prebuilt module
              arch: string, VNDK snapshot arch (e.g. 'arm64')
              srcs_props: dict, prebuilt source paths and corresponding flags
            """
            arch_props = '{ind}arch: {{\n'.format(ind=self.INDENT)

            def list_to_prop_value(l, name):
                if len(l) == 0:
                    return ''
                dirs=',\n{ind}{ind}{ind}{ind}'.format(
                    ind=self.INDENT).join(['"%s"' % d for d in l])
                return ('{ind}{ind}{ind}{name}: [\n'
                        '{ind}{ind}{ind}{ind}{dirs},\n'
                        '{ind}{ind}{ind}],\n'.format(
                            ind=self.INDENT,
                            dirs=dirs,
                            name=name))

            def rename_generated_dirs(dirs):
                # Rename out/soong/.intermediates to generated-headers for better readability.
                return [d.replace(utils.SOONG_INTERMEDIATES_DIR, utils.GENERATED_HEADERS_DIR, 1) for d in dirs]

            for src in sorted(srcs_props.keys()):
                include_dirs = ''
                system_include_dirs = ''
                flags = ''
                relative_install_path = ''
                props = srcs_props[src]
                if 'ExportedDirs' in props:
                    dirs = rename_generated_dirs(props['ExportedDirs'])
                    l = ['include/%s' % d for d in dirs]
                    include_dirs = list_to_prop_value(l, 'export_include_dirs')
                if 'ExportedSystemDirs' in props:
                    dirs = rename_generated_dirs(props['ExportedSystemDirs'])
                    l = ['include/%s' % d for d in dirs]
                    system_include_dirs = list_to_prop_value(l, 'export_system_include_dirs')
                if 'ExportedFlags' in props:
                    flags = list_to_prop_value(props['ExportedFlags'], 'export_flags')
                if 'RelativeInstallPath' in props:
                    relative_install_path = ('{ind}{ind}{ind}'
                        'relative_install_path: "{path}",\n').format(
                            ind=self.INDENT,
                            path=props['RelativeInstallPath'])
                if 'LicenseKinds' in props:
                    self._license_kinds_map[name].update(props['LicenseKinds'])
                if 'LicenseTexts' in props:
                    self._license_texts_map[name].update(props['LicenseTexts'])

                arch_props += ('{ind}{ind}{arch}: {{\n'
                               '{include_dirs}'
                               '{system_include_dirs}'
                               '{flags}'
                               '{relative_install_path}'
                               '{ind}{ind}{ind}srcs: ["{src}"],\n'
                               '{ind}{ind}}},\n').format(
                                  ind=self.INDENT,
                                  arch=utils.prebuilt_arch_from_path(
                                      os.path.join(arch, src)),
                                  include_dirs=include_dirs,
                                  system_include_dirs=system_include_dirs,
                                  flags=flags,
                                  relative_install_path=relative_install_path,
                                  src=src)
            arch_props += '{ind}}},\n'.format(ind=self.INDENT)
            return arch_props

        src_root = os.path.join(self._install_dir, arch)
        if is_binder32:
            src_root = os.path.join(src_root, utils.BINDER32)

        src_paths = utils.find(src_root, srcs)
        # filter out paths under 'binder32' subdirectory
        src_paths = list(filter(lambda src: not src.startswith(utils.BINDER32),
            src_paths))
        # This module is invalid if no srcs are found.
        if not src_paths:
            logging.info('No srcs found for {}; skipping'.format(name))
            return ""

        product_available = ''
        # if vndkproduct.libraries.txt is empty, make the VNDKs available to product by default.
        if is_llndk or not self._vndk_product[arch] or is_prebuilts_in_list(srcs, self._vndk_product[arch]):
            product_available = '{ind}product_available: true,\n'.format(
                ind=self.INDENT)

        vndk_props = ''
        if not is_llndk:
            vndk_sp = ''
            if is_vndk_sp:
                vndk_sp = '{ind}{ind}support_system_process: true,\n'.format(
                    ind=self.INDENT)

            vndk_private = ''
            if is_prebuilts_in_list(srcs, self._vndk_private[arch]):
                vndk_private = '{ind}{ind}private: true,\n'.format(
                    ind=self.INDENT)

            vndk_props = ('{ind}vndk: {{\n'
                          '{ind}{ind}enabled: true,\n'
                          '{vndk_sp}'
                          '{vndk_private}'
                          '{ind}}},\n'.format(
                              ind=self.INDENT,
                              product_available=product_available,
                              vndk_sp=vndk_sp,
                              vndk_private=vndk_private))

        srcs_props = dict()
        for src in src_paths:
            props = dict()
            prop_path = os.path.join(src_root, src+'.json')
            try:
                with open(prop_path, 'r') as f:
                    props = json.loads(f.read())
                os.unlink(prop_path)
            except:
                # TODO(b/70312118): Parse from soong build system
                if name == 'android.hidl.memory@1.0-impl':
                    props['RelativeInstallPath'] = 'hw'
            srcs_props[src] = props
        arch_props = get_arch_props(name, arch, srcs_props)

        if self._license_in_json:
            license = get_license_prop(name)
        else:
            license = get_notice_file(srcs)

        binder32bit = ''
        if is_binder32:
            binder32bit = '{ind}binder32bit: true,\n'.format(ind=self.INDENT)

        min_sdk_version = ''
        for src, props in srcs_props.items():
            if 'MinSdkVersion' in props:
                min_sdk_version = '{ind}min_sdk_version: "{ver}",\n'.format(
                    ind=self.INDENT,
                    ver=props['MinSdkVersion'])
                break

        return ('vndk_prebuilt_shared {{\n'
                '{ind}name: "{name}",\n'
                '{ind}version: "{ver}",\n'
                '{ind}target_arch: "{target_arch}",\n'
                '{binder32bit}'
                '{ind}vendor_available: true,\n'
                '{product_available}'
                '{vndk_props}'
                '{min_sdk_version}'
                '{license}'
                '{arch_props}'
                '}}\n'.format(
                    ind=self.INDENT,
                    name=name,
                    ver=self._vndk_version,
                    target_arch=arch,
                    binder32bit=binder32bit,
                    product_available=product_available,
                    vndk_props=vndk_props,
                    min_sdk_version=min_sdk_version,
                    license=license,
                    arch_props=arch_props))


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version',
        type=utils.vndk_version_int,
        help='VNDK snapshot version to install, e.g. "{}".'.format(
            utils.MINIMUM_VNDK_VERSION))
    parser.add_argument(
        '-v',
        '--verbose',
        action='count',
        default=0,
        help='Increase output verbosity, e.g. "-v", "-vv".')
    return parser.parse_args()


def main():
    """For local testing purposes.

    Note: VNDK snapshot must be already installed under
      prebuilts/vndk/v{version}.
    """
    ANDROID_BUILD_TOP = utils.get_android_build_top()
    PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP,
                                             'prebuilts/vndk')

    args = get_args()
    vndk_version = args.vndk_version
    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))
    if not os.path.isdir(install_dir):
        raise ValueError(
            'Please provide valid VNDK version. {} does not exist.'
            .format(install_dir))
    utils.set_logging_config(args.verbose)

    buildfile_generator = GenBuildFile(install_dir, vndk_version)
    # To parse json information, read and generate arch android.bp using
    # generate_android_bp() first.
    buildfile_generator.generate_android_bp()
    buildfile_generator.generate_root_android_bp()
    buildfile_generator.generate_common_android_bp()

    logging.info('Done.')


if __name__ == '__main__':
    main()
