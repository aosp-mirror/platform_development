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
"""Add tests to TEST_MAPPING. Include tests for reverse dependencies."""
import json
import os
import platform
import subprocess
import sys

test_options = {
    "ring_device_test_tests_digest_tests": [{"test-timeout": "600000"}],
    "ring_device_test_src_lib": [{"test-timeout": "100000"}],
}
test_exclude = [
        "aidl_test_rust_client",
        "aidl_test_rust_service"
    ]
exclude_paths = [
        "//external/adhd",
        "//external/crosvm",
        "//external/libchromeos-rs",
        "//external/vm_tools"
    ]


class UpdaterException(Exception):
    pass


class Env(object):
    def __init__(self):
        try:
            self.ANDROID_BUILD_TOP = os.environ['ANDROID_BUILD_TOP']
        except KeyError:
            raise UpdaterException('$ANDROID_BUILD_TOP is not defined; you '
                                   'must first source build/envsetup.sh and '
                                   'select a target.')


class Bazel(object):
    # set up the Bazel queryview
    def __init__(self, env):
        if platform.system() != 'Linux':
            raise UpdaterException('This script has only been tested on Linux.')
        self.path = os.path.join(env.ANDROID_BUILD_TOP, "tools", "bazel")
        soong_ui = os.path.join(env.ANDROID_BUILD_TOP, "build", "soong", "soong_ui.bash")

        # soong_ui requires to be at the root of the repository.
        os.chdir(env.ANDROID_BUILD_TOP)
        print("Generating Bazel files...")
        cmd = [soong_ui, "--make-mode", "GENERATE_BAZEL_FILES=1", "nothing"]
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True)
        except subprocess.CalledProcessError as e:
            raise UpdaterException('Unable to generate bazel workspace: ' + e.output)

        print("Building Bazel Queryview. This can take a couple of minutes...")
        cmd = [soong_ui, "--build-mode", "--all-modules", "--dir=.", "queryview"]
        try:
            subprocess.check_output(cmd, stderr=subprocess.STDOUT, text=True)
        except subprocess.CalledProcessError as e:
            raise UpdaterException('Unable to update TEST_MAPPING: ' + e.output)

    # Return all modules for a given path.
    def query_modules(self, path):
        cmd = self.path + " query --config=queryview /" + path + ":all"
        out = subprocess.check_output(cmd, shell=True, stderr=subprocess.DEVNULL, text=True).strip().split("\n")
        modules = set()
        for line in out:
            # speed up by excluding unused modules.
            if "windows_x86" in line:
                continue
            modules.add(line)
        return modules

    # Return all reverse dependencies for a single module.
    def query_rdeps(self, module):
        cmd = (self.path + " query --config=queryview \'rdeps(//..., " +
                module + ")\' --output=label_kind")
        out = (subprocess.check_output(cmd, shell=True, stderr=subprocess.DEVNULL, text=True)
                .strip().split("\n"))
        if '' in out:
            out.remove('')
        return out

    def exclude_module(self, module):
        for path in exclude_paths:
            if module.startswith(path):
                return True
        return False

    # Return all reverse dependency tests for modules in this package.
    def query_rdep_tests(self, modules):
        rdep_tests = set()
        for module in modules:
            for rdep in self.query_rdeps(module):
                rule_type, _, mod = rdep.split(" ")
                if rule_type == "rust_test_" or rule_type == "rust_test":
                    if self.exclude_module(mod) == False:
                        rdep_tests.add(mod.split(":")[1].split("--")[0])
        return rdep_tests


class Package(object):
    def __init__(self, path, env, bazel):
        if path == None:
            self.dir = os.getcwd()
        else:
            self.dir = path
        try:
            self.dir_rel = self.dir.split(env.ANDROID_BUILD_TOP)[1]
        except IndexError:
            raise UpdaterException('The path ' + self.dir + ' is not under ' +
                            env.ANDROID_BUILD_TOP + '; You must be in the '
                            'directory of a crate or pass its absolute path '
                            'as first argument.')

        # Move to the package_directory.
        os.chdir(self.dir)
        modules = bazel.query_modules(self.dir_rel)
        self.rdep_tests = bazel.query_rdep_tests(modules)

    def get_rdep_tests(self):
        return self.rdep_tests


class TestMapping(object):
    def __init__(self, path):
        env = Env()
        bazel = Bazel(env)
        self.package = Package(path, env, bazel)

    def create(self):
        tests = self.package.get_rdep_tests()
        if not bool(tests):
            return
        test_mapping = self.tests_to_mapping(tests)
        self.write_test_mapping(test_mapping)

    def tests_to_mapping(self, tests):
        test_mapping = {"presubmit": []}
        for test in tests:
            if test in test_exclude:
                continue
            if test in test_options:
                test_mapping["presubmit"].append({"name": test, "options": test_options[test]})
            else:
                test_mapping["presubmit"].append({"name": test})
        test_mapping["presubmit"] = sorted(test_mapping["presubmit"], key=lambda t: t["name"])
        return test_mapping

    def write_test_mapping(self, test_mapping):
        with open("TEST_MAPPING", "w") as json_file:
            json_file.write("// Generated by update_crate_tests.py for tests that depend on this crate.\n")
            json.dump(test_mapping, json_file, indent=2, separators=(',', ': '), sort_keys=True)
            json_file.write("\n")
        print("TEST_MAPPING successfully updated!")


def main():
    if len(sys.argv) == 2:
        path = sys.argv[1]
    else:
        path = None
    try:
        test_mapping = TestMapping(path)
    except UpdaterException as err:
        sys.exit("Error: " + str(err))
    test_mapping.create()

if __name__ == '__main__':
  main()
