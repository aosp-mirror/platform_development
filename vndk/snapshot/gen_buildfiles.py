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

import glob
import os
import sys

import utils


class GenBuildFile(object):
    """Generates Android.mk and Android.bp for VNDK snapshot.

    VNDK snapshot directory structure under prebuilts/vndk/v{version}:
        {SNAPSHOT_VARIANT}/
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
            configs/
                (various *.txt configuration files, e.g. ld.config.*.txt)
        ... (other {SNAPSHOT_VARIANT}/ directories)
        common/
            Android.mk
            NOTICE_FILES/
                (license files, e.g. libfoo.so.txt)
    """
    INDENT = '    '
    ETC_MODULES = [
        'ld.config.txt', 'llndk.libraries.txt', 'vndksp.libraries.txt'
    ]

    # TODO(b/70312118): Parse from soong build system
    RELATIVE_INSTALL_PATHS = {'android.hidl.memory@1.0-impl.so': 'hw'}

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
        self._snapshot_variants = utils.get_snapshot_variants(install_dir)
        self._mkfile = os.path.join(install_dir, utils.ANDROID_MK_PATH)
        self._vndk_core = self._parse_lib_list('vndkcore.libraries.txt')
        self._vndk_sp = self._parse_lib_list(
            os.path.basename(self._etc_paths['vndksp.libraries.txt']))
        self._vndk_private = self._parse_lib_list('vndkprivate.libraries.txt')

    def _get_etc_paths(self):
        """Returns a map of relative file paths for each ETC module."""

        etc_paths = dict()
        for etc_module in self.ETC_MODULES:
            etc_pattern = '{}*'.format(os.path.splitext(etc_module)[0])
            etc_path = glob.glob(
                os.path.join(self._install_dir, utils.CONFIG_DIR_PATH_PATTERN,
                             etc_pattern))[0]
            rel_etc_path = etc_path.replace(self._install_dir, '')[1:]
            etc_paths[etc_module] = rel_etc_path
        return etc_paths

    def _parse_lib_list(self, txt_filename):
        """Returns a map of VNDK library lists per VNDK snapshot variant.

        Args:
          txt_filename: string, name of snapshot config file

        Returns:
          dict, e.g. {'arm64': ['libfoo.so', 'libbar.so', ...], ...}
        """
        lib_map = dict()
        for txt_path in utils.find(self._install_dir, [txt_filename]):
            variant = utils.variant_from_path(txt_path)
            abs_path_of_txt = os.path.join(self._install_dir, txt_path)
            with open(abs_path_of_txt, 'r') as f:
                lib_map[variant] = f.read().strip().split('\n')
        return lib_map

    def generate_android_mk(self):
        """Autogenerates Android.mk."""

        etc_buildrules = []
        for prebuilt in self.ETC_MODULES:
            etc_buildrules.append(self._gen_etc_prebuilt(prebuilt))

        with open(self._mkfile, 'w') as mkfile:
            mkfile.write(self._gen_autogen_msg('#'))
            mkfile.write('\n')
            mkfile.write('LOCAL_PATH := $(call my-dir)\n')
            mkfile.write('\n')
            mkfile.write('\n\n'.join(etc_buildrules))
            mkfile.write('\n')

    def generate_android_bp(self):
        """Autogenerates Android.bp file for each VNDK snapshot variant."""

        for variant in self._snapshot_variants:
            bpfile = os.path.join(self._install_dir, variant, 'Android.bp')
            vndk_core_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_core[variant], variant, False)
            vndk_sp_buildrules = self._gen_vndk_shared_prebuilts(
                self._vndk_sp[variant], variant, True)

            with open(bpfile, 'w') as bpfile:
                bpfile.write(self._gen_autogen_msg('/'))
                bpfile.write('\n')
                bpfile.write(self._gen_bp_phony(variant))
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_core_buildrules))
                bpfile.write('\n')
                bpfile.write('\n'.join(vndk_sp_buildrules))

    def _gen_autogen_msg(self, comment_char):
        return ('{0}{0} THIS FILE IS AUTOGENERATED BY '
                'development/vndk/snapshot/gen_buildfiles.py\n'
                '{0}{0} DO NOT EDIT\n'.format(comment_char))

    def _get_versioned_name(self, prebuilt, variant, is_etc):
        """Returns the VNDK version-specific module name for a given prebuilt.

        The VNDK version-specific module name is defined as follows:
        For a VNDK shared lib: 'libfoo.so'
                            -> 'libfoo.vndk.{version}.{variant}.vendor'
        For an ETC module: 'foo.txt' -> 'foo.{version}.txt'

        Args:
          prebuilt: string, name of the prebuilt object
          variant: string, VNDK snapshot variant (e.g. 'arm64')
          is_etc: bool, True if the LOCAL_MODULE_CLASS of prebuilt is 'ETC'
        """
        name, ext = os.path.splitext(prebuilt)
        if is_etc:
            versioned_name = '{}.{}{}'.format(name, self._vndk_version, ext)
        else:
            versioned_name = '{}.vndk.{}.{}.vendor'.format(
                name, self._vndk_version, variant)

        return versioned_name

    def _gen_etc_prebuilt(self, prebuilt):
        """Generates build rule for an ETC prebuilt.

        Args:
          prebuilt: string, name of ETC prebuilt object
        """
        etc_path = self._etc_paths[prebuilt]
        etc_sub_path = etc_path[etc_path.index('/') + 1:]

        return ('#######################################\n'
                '# {prebuilt}\n'
                'include $(CLEAR_VARS)\n'
                'LOCAL_MODULE := {versioned_name}\n'
                'LOCAL_SRC_FILES := ../$(TARGET_ARCH)/{etc_sub_path}\n'
                'LOCAL_MODULE_CLASS := ETC\n'
                'LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)\n'
                'LOCAL_MODULE_STEM := $(LOCAL_MODULE)\n'
                'include $(BUILD_PREBUILT)\n'.format(
                    prebuilt=prebuilt,
                    versioned_name=self._get_versioned_name(
                        prebuilt, None, True),
                    etc_sub_path=etc_sub_path))

    def _gen_bp_phony(self, variant):
        """Generates build rule for phony package 'vndk_v{ver}_{variant}'.

        Args:
          variant: string, VNDK snapshot variant (e.g. 'arm64')
        """
        required = []
        for prebuilts in (self._vndk_core[variant], self._vndk_sp[variant]):
            for prebuilt in prebuilts:
                required.append(
                    self._get_versioned_name(prebuilt, variant, False))

        for prebuilt in self.ETC_MODULES:
            required.append(self._get_versioned_name(prebuilt, None, True))

        required_str = ['"{}",'.format(prebuilt) for prebuilt in required]
        required_formatted = '\n{ind}{ind}'.format(
            ind=self.INDENT).join(required_str)
        required_buildrule = ('{ind}required: [\n'
                              '{ind}{ind}{required_formatted}\n'
                              '{ind}],\n'.format(
                                  ind=self.INDENT,
                                  required_formatted=required_formatted))

        return ('phony {{\n'
                '{ind}name: "vndk_v{ver}_{variant}",\n'
                '{required_buildrule}'
                '}}\n'.format(
                    ind=self.INDENT,
                    ver=self._vndk_version,
                    variant=variant,
                    required_buildrule=required_buildrule))

    def _gen_vndk_shared_prebuilts(self, prebuilts, variant, is_vndk_sp):
        """Returns list of build rules for given prebuilts.

        Args:
          prebuilts: list of VNDK shared prebuilts
          variant: string, VNDK snapshot variant (e.g. 'arm64')
          is_vndk_sp: bool, True if prebuilts are VNDK_SP libs
        """
        build_rules = []
        for prebuilt in prebuilts:
            build_rules.append(
                self._gen_vndk_shared_prebuilt(prebuilt, variant, is_vndk_sp))
        return build_rules

    def _gen_vndk_shared_prebuilt(self, prebuilt, variant, is_vndk_sp):
        """Returns build rule for given prebuilt.

        Args:
          prebuilt: string, name of prebuilt object
          variant: string, VNDK snapshot variant (e.g. 'arm64')
          is_vndk_sp: bool, True if prebuilt is a VNDK_SP lib
        """

        def get_notice_file(prebuilt):
            """Returns build rule for notice file (attribute 'notice').

            Args:
              prebuilt: string, name of prebuilt object
            """
            notice = ''
            notice_file_name = '{}.txt'.format(prebuilt)
            notices_dir = os.path.join(self._install_dir,
                                       utils.NOTICE_FILES_DIR_PATH)
            notice_files = utils.find(notices_dir, [notice_file_name])
            if len(notice_files) > 0:
                notice = '{ind}notice: "{notice_file_path}",\n'.format(
                    ind=self.INDENT,
                    notice_file_path=os.path.join(
                        '..', utils.NOTICE_FILES_DIR_PATH, notice_files[0]))
            return notice

        def get_rel_install_path(prebuilt):
            """Returns build rule for 'relative_install_path'.

            Args:
              prebuilt: string, name of prebuilt object
            """
            rel_install_path = ''
            if prebuilt in self.RELATIVE_INSTALL_PATHS:
                path = self.RELATIVE_INSTALL_PATHS[prebuilt]
                rel_install_path += ('{ind}relative_install_path: "{path}",\n'
                                     .format(ind=self.INDENT, path=path))
            return rel_install_path

        def get_arch_srcs(prebuilt, variant):
            """Returns build rule for arch specific srcs.

            e.g.,
                arch: {
                    arm: {
                        srcs: ["..."]
                    },
                    arm64: {
                        srcs: ["..."]
                    },
                }

            Args:
              prebuilt: string, name of prebuilt object
              variant: string, VNDK snapshot variant (e.g. 'arm64')
            """
            arch_srcs = '{ind}arch: {{\n'.format(ind=self.INDENT)
            variant_path = os.path.join(self._install_dir, variant)
            src_paths = utils.find(variant_path, [prebuilt])
            for src in sorted(src_paths):
                arch_srcs += ('{ind}{ind}{arch}: {{\n'
                              '{ind}{ind}{ind}srcs: ["{src}"],\n'
                              '{ind}{ind}}},\n'.format(
                                  ind=self.INDENT,
                                  arch=utils.arch_from_path(
                                      os.path.join(variant, src)),
                                  src=src))
            arch_srcs += '{ind}}},\n'.format(ind=self.INDENT)
            return arch_srcs

        name = os.path.splitext(prebuilt)[0]
        vendor_available = str(
            prebuilt not in self._vndk_private[variant]).lower()
        if is_vndk_sp:
            vndk_sp = '{ind}{ind}support_system_process: true,\n'.format(
                ind=self.INDENT)
        else:
            vndk_sp = ''
        notice = get_notice_file(prebuilt)
        rel_install_path = get_rel_install_path(prebuilt)
        arch_srcs = get_arch_srcs(prebuilt, variant)

        return ('vndk_prebuilt_shared {{\n'
                '{ind}name: "{name}",\n'
                '{ind}version: "{ver}",\n'
                '{ind}target_arch: "{target_arch}",\n'
                '{ind}vendor_available: {vendor_available},\n'
                '{ind}vndk: {{\n'
                '{ind}{ind}enabled: true,\n'
                '{vndk_sp}'
                '{ind}}},\n'
                '{notice}'
                '{rel_install_path}'
                '{arch_srcs}'
                '}}\n'.format(
                    ind=self.INDENT,
                    name=name,
                    ver=self._vndk_version,
                    vendor_available=vendor_available,
                    target_arch=variant,
                    vndk_sp=vndk_sp,
                    notice=notice,
                    rel_install_path=rel_install_path,
                    arch_srcs=arch_srcs))


def main():
    """For local testing purposes.

    Note: VNDK snapshot must be already installed under
      prebuilts/vndk/v{version}.
    """
    ANDROID_BUILD_TOP = utils.get_android_build_top()
    PREBUILTS_VNDK_DIR = utils.join_realpath(ANDROID_BUILD_TOP,
                                             'prebuilts/vndk')

    vndk_version = 27  # set appropriately
    install_dir = os.path.join(PREBUILTS_VNDK_DIR, 'v{}'.format(vndk_version))

    buildfile_generator = GenBuildFile(install_dir, vndk_version)
    buildfile_generator.generate_android_mk()
    buildfile_generator.generate_android_bp()


if __name__ == '__main__':
    main()
