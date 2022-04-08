#!/usr/bin/env python
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
import glob
import json
import logging
import os
import sys

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
        'ld.config.txt',
        'llndk.libraries.txt',
        'vndksp.libraries.txt',
        'vndkcore.libraries.txt',
        'vndkprivate.libraries.txt'
    ]

    def __init__(self, install_dir, vndk_version):
        """GenBuildFile constructor.

        Args:
          install_dir: string, absolute path to the prebuilts/vndk/v{version}
            directory where the build files will be generated.
          vndk_version: int, VNDK snapshot version (e.g., 27, 28)
        """
        self._install_dir = install_dir
        self._vndk_version = vndk_version
        self._etc_paths = self._get_etc_paths()
        self._snapshot_archs = utils.get_snapshot_archs(install_dir)
        self._root_bpfile = os.path.join(install_dir, utils.ROOT_BP_PATH)
        self._common_bpfile = os.path.join(install_dir, utils.COMMON_BP_PATH)
        self._vndk_core = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndkcore.libraries.txt']))
        self._vndk_sp = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndksp.libraries.txt']))
        self._vndk_private = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndkprivate.libraries.txt']))
        self._modules_with_notice = self._get_modules_with_notice()

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
        return lib_map

    def _get_modules_with_notice(self):
        """Returns a list of modules that have associated notice files. """
        notice_paths = glob.glob(
            os.path.join(self._install_dir, utils.NOTICE_FILES_DIR_PATH,
                         '*.txt'))
        return [os.path.splitext(os.path.basename(p))[0] for p in notice_paths]

    def generate_root_android_bp(self):
        """Autogenerates Android.bp."""

        logging.info('Generating Android.bp for snapshot v{}'.format(
            self._vndk_version))
        etc_buildrules = []
        for prebuilt in self.ETC_MODULES:
            # ld.config.VER.txt is not installed as a prebuilt but is built and
            # installed from thesource tree at the time the VNDK snapshot is
            # installed to the system.img.
            if prebuilt == 'ld.config.txt':
                continue
            etc_buildrules.append(self._gen_etc_prebuilt(prebuilt))

        with open(self._root_bpfile, 'w') as bpfile:
            bpfile.write(self._gen_autogen_msg('/'))
            bpfile.write('\n')
            bpfile.write('\n'.join(etc_buildrules))
            bpfile.write('\n')

        logging.info('Successfully generated {}'.format(self._root_bpfile))

    def generate_common_android_bp(self):
        """Autogenerates common/Android.bp."""

        logging.info('Generating common/Android.bp for snapshot v{}'.format(
            self._vndk_version))
        with open(self._common_bpfile, 'w') as bpfile:
            bpfile.write(self._gen_autogen_msg('/'))
            for module in self._modules_with_notice:
                bpfile.write('\n')
                bpfile.write(self._gen_notice_filegroup(module))

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
            # For O-MR1 snapshot (v27), 32-bit binder prebuilts are not
            # isolated in separate 'binder32' subdirectory.
            if is_binder32 and self._vndk_version >= 28:
                variant_subpath = os.path.join(arch, utils.BINDER32)
            variant_path = os.path.join(self._install_dir, variant_subpath)
            bpfile_path = os.path.join(variant_path, 'Android.bp')

            vndk_core_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_core[arch],
                arch,
                is_vndk_sp=False,
                is_binder32=is_binder32,
                module_names=module_names)
            vndk_sp_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_sp[arch],
                arch,
                is_vndk_sp=True,
                is_binder32=is_binder32,
                module_names=module_names)

            with open(bpfile_path, 'w') as bpfile:
                bpfile.write(self._gen_autogen_msg('/'))
                bpfile.write('\n')
                bpfile.write(self._gen_bp_phony(arch, is_binder32, module_names))
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_core_buildrules))
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_sp_buildrules))

            variant_include_path = os.path.join(variant_path, 'include')
            include_path = os.path.join(self._install_dir, arch, 'include')
            if os.path.isdir(include_path) and variant_include_path != include_path:
                os.symlink(os.path.relpath(include_path, variant_path),
                    variant_include_path)

            logging.info('Successfully generated {}'.format(bpfile_path))

        if self._vndk_version == 27:
            # For O-MR1 snapshot (v27), 32-bit binder prebuilts are not
            # isolated in separate 'binder32' subdirectory.
            for arch in self._snapshot_archs:
                if arch in ('arm', 'x86'):
                    gen_for_variant(arch, is_binder32=True)
                else:
                    gen_for_variant(arch)
            return

        for arch in self._snapshot_archs:
            if os.path.isdir(
                    os.path.join(self._install_dir, arch, utils.BINDER32)):
                gen_for_variant(arch, is_binder32=True)
            gen_for_variant(arch)

    def _gen_autogen_msg(self, comment_char):
        return ('{0}{0} THIS FILE IS AUTOGENERATED BY '
                'development/vndk/snapshot/gen_buildfiles.py\n'
                '{0}{0} DO NOT EDIT\n'.format(comment_char))

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

    def _gen_notice_filegroup(self, module):
        """Generates a notice filegroup build rule for a given module.

        Args:
          notice: string, module name
        """
        return ('filegroup {{\n'
                '{ind}name: "{filegroup_name}",\n'
                '{ind}srcs: ["{notice_dir}/{module}.txt"],\n'
                '}}\n'.format(
                    ind=self.INDENT,
                    filegroup_name=self._get_notice_filegroup_name(module),
                    module=module,
                    notice_dir=utils.NOTICE_FILES_DIR_NAME))

    def _get_notice_filegroup_name(self, module):
        """ Gets a notice filegroup module name for a given module.

        Args:
          notice: string, module name.
        """
        return 'vndk-v{ver}-{module}-notice'.format(
            ver=self._vndk_version, module=module)

    def _gen_bp_phony(self, arch, is_binder32, module_names):
        """Generates build rule for phony package 'vndk_v{ver}_{arch}'.

        Args:
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          is_binder32: bool, True if binder interface is 32-bit
          module_names: dict, module names for given prebuilts
        """

        required = []
        for prebuilts in (self._vndk_core[arch], self._vndk_sp[arch]):
            for prebuilt in prebuilts:
                required.append(
                    self._get_versioned_name(
                        prebuilt,
                        arch,
                        is_binder32=is_binder32,
                        module_names=module_names))

        for prebuilt in self.ETC_MODULES:
            required.append(
                self._get_versioned_name(
                    prebuilt,
                    None,
                    is_etc=True,
                    is_binder32=is_binder32,
                    module_names=module_names))

        required_str = ['"{}",'.format(prebuilt) for prebuilt in required]
        required_formatted = '\n{ind}{ind}'.format(
            ind=self.INDENT).join(required_str)
        required_buildrule = ('{ind}required: [\n'
                              '{ind}{ind}{required_formatted}\n'
                              '{ind}],\n'.format(
                                  ind=self.INDENT,
                                  required_formatted=required_formatted))
        binder_suffix = '_{}'.format(utils.BINDER32) if is_binder32 else ''

        return ('phony {{\n'
                '{ind}name: "vndk_v{ver}_{arch}{binder_suffix}",\n'
                '{required_buildrule}'
                '}}\n'.format(
                    ind=self.INDENT,
                    ver=self._vndk_version,
                    arch=arch,
                    binder_suffix=binder_suffix,
                    required_buildrule=required_buildrule))

    def _gen_vndk_shared_prebuilts(self,
                                   prebuilts,
                                   arch,
                                   is_vndk_sp,
                                   is_binder32,
                                   module_names):
        """Returns list of build rules for given prebuilts.

        Args:
          prebuilts: list of VNDK shared prebuilts
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          is_vndk_sp: bool, True if prebuilts are VNDK_SP libs
          is_binder32: bool, True if binder interface is 32-bit
          module_names: dict, module names for given prebuilts
        """

        build_rules = []
        for prebuilt in prebuilts:
            build_rules.append(
                self._gen_vndk_shared_prebuilt(
                    prebuilt,
                    arch,
                    is_vndk_sp=is_vndk_sp,
                    is_binder32=is_binder32,
                    module_names=module_names))
        return build_rules

    def _gen_vndk_shared_prebuilt(self,
                                  prebuilt,
                                  arch,
                                  is_vndk_sp,
                                  is_binder32,
                                  module_names):
        """Returns build rule for given prebuilt.

        Args:
          prebuilt: string, name of prebuilt object
          arch: string, VNDK snapshot arch (e.g. 'arm64')
          is_vndk_sp: bool, True if prebuilt is a VNDK_SP lib
          is_binder32: bool, True if binder interface is 32-bit
          module_names: dict, module names for given prebuilts
        """

        def get_notice_file(prebuilt):
            """Returns build rule for notice file (attribute 'notice').

            Args:
              prebuilt: string, name of prebuilt object
            """
            notice = ''
            if prebuilt in self._modules_with_notice:
                notice = '{ind}notice: ":{notice_filegroup}",\n'.format(
                    ind=self.INDENT,
                    notice_filegroup=self._get_notice_filegroup_name(prebuilt))
            return notice

        def get_arch_props(prebuilt, arch):
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
              prebuilt: string, name of prebuilt object
              arch: string, VNDK snapshot arch (e.g. 'arm64')
            """
            arch_props = '{ind}arch: {{\n'.format(ind=self.INDENT)
            src_paths = utils.find(src_root, [prebuilt])
            # filter out paths under 'binder32' subdirectory
            src_paths = filter(lambda src: not src.startswith(utils.BINDER32),
                               src_paths)

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

            for src in sorted(src_paths):
                include_dirs = ''
                system_include_dirs = ''
                flags = ''
                relative_install_path = ''
                prop_path = os.path.join(src_root, src+'.json')
                props = dict()
                try:
                    with open(prop_path, 'r') as f:
                        props = json.loads(f.read())
                    os.unlink(prop_path)
                except:
                    # TODO(b/70312118): Parse from soong build system
                    if prebuilt == 'android.hidl.memory@1.0-impl.so':
                        props['RelativeInstallPath'] = 'hw'
                if 'ExportedDirs' in props:
                    l = ['include/%s' % d for d in props['ExportedDirs']]
                    include_dirs = list_to_prop_value(l, 'export_include_dirs')
                if 'ExportedSystemDirs' in props:
                    l = ['include/%s' % d for d in props['ExportedSystemDirs']]
                    system_include_dirs = list_to_prop_value(l, 'export_system_include_dirs')
                if 'ExportedFlags' in props:
                    flags = list_to_prop_value(props['ExportedFlags'], 'export_flags')
                if 'RelativeInstallPath' in props:
                    relative_install_path = ('{ind}{ind}{ind}'
                        'relative_install_path: "{path}",\n').format(
                            ind=self.INDENT,
                            path=props['RelativeInstallPath'])

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
        # For O-MR1 snapshot (v27), 32-bit binder prebuilts are not
        # isolated in separate 'binder32' subdirectory.
        if is_binder32 and self._vndk_version >= 28:
            src_root = os.path.join(src_root, utils.BINDER32)

        if prebuilt in module_names:
            name = module_names[prebuilt]
        else:
            name = os.path.splitext(prebuilt)[0]
        vendor_available = str(
            prebuilt not in self._vndk_private[arch]).lower()

        vndk_sp = ''
        if is_vndk_sp:
            vndk_sp = '{ind}{ind}support_system_process: true,\n'.format(
                ind=self.INDENT)

        notice = get_notice_file(prebuilt)
        arch_props = get_arch_props(prebuilt, arch)

        binder32bit = ''
        if is_binder32:
            binder32bit = '{ind}binder32bit: true,\n'.format(ind=self.INDENT)

        return ('vndk_prebuilt_shared {{\n'
                '{ind}name: "{name}",\n'
                '{ind}version: "{ver}",\n'
                '{ind}target_arch: "{target_arch}",\n'
                '{binder32bit}'
                '{ind}vendor_available: {vendor_available},\n'
                '{ind}vndk: {{\n'
                '{ind}{ind}enabled: true,\n'
                '{vndk_sp}'
                '{ind}}},\n'
                '{notice}'
                '{arch_props}'
                '}}\n'.format(
                    ind=self.INDENT,
                    name=name,
                    ver=self._vndk_version,
                    target_arch=arch,
                    binder32bit=binder32bit,
                    vendor_available=vendor_available,
                    vndk_sp=vndk_sp,
                    notice=notice,
                    arch_props=arch_props))


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'vndk_version',
        type=int,
        help='VNDK snapshot version to install, e.g. "27".')
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
    buildfile_generator.generate_root_android_bp()
    buildfile_generator.generate_common_android_bp()
    buildfile_generator.generate_android_bp()

    logging.info('Done.')


if __name__ == '__main__':
    main()
