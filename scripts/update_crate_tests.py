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

test_options = {"ring_device_test_tests_digest_tests": [{"test-timeout": "600000"}]}
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

class Env(object):
    def __init__(self):
        try:
            self.ANDROID_BUILD_TOP = os.environ['ANDROID_BUILD_TOP']
        except:
            sys.exit('ERROR: this script must be run from an Android tree.')
        self.cwd = os.getcwd()
        self.cwd_relative = self.cwd.split(self.ANDROID_BUILD_TOP)[1]

class Bazel(object):
    # set up the Bazel queryview
    def __init__(self, env):
        os.chdir(env.ANDROID_BUILD_TOP)
        if not os.path.exists("out/soong/queryview"):
            print("Building Bazel Queryview. This can take a couple of minutes...")
            cmd = "./build/soong/soong_ui.bash --build-mode --all-modules --dir=. queryview"
            subprocess.check_output(cmd, shell=True)
        os.chdir(env.cwd)

    def path(self):
        # Only tested on Linux.
        if platform.system() != 'Linux':
            sys.exit('ERROR: this script has only been tested on Linux.')
        return "/usr/bin/bazel"

    # Return all modules for a given path.
    def query_modules(self, path):
        with open(os.devnull, 'wb') as DEVNULL:
            cmd = self.path() + " query --config=queryview /" + path + ":all"
            out = subprocess.check_output(cmd, shell=True, stderr=DEVNULL, text=True).strip().split("\n")
            modules = set()
            for line in out:
                # speed up by excluding unused modules.
                if "windows_x86" in line:
                    continue
                modules.add(line)
            return modules

    # Return all reverse dependencies for a single module.
    def query_rdeps(self, module):
        with open(os.devnull, 'wb') as DEVNULL:
            cmd = (self.path() + " query --config=queryview \'rdeps(//..., " +
                    module + ")\' --output=label_kind")
            out = (subprocess.check_output(cmd, shell=True, stderr=DEVNULL, text=True)
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
        print("Querying tests that depend on this crate for TEST_MAPPING. This can take a couple of minutes...")
        for module in modules:
            for rdep in self.query_rdeps(module):
                rule_type, tmp, mod = rdep.split(" ")
                if rule_type == "rust_test_" or rule_type == "rust_test":
                    if self.exclude_module(mod) == False:
                        rdep_tests.add(mod.split(":")[1].split("--")[0])
        return rdep_tests


class Crate(object):
    def __init__(self, path, bazel):
        self.modules = bazel.query_modules(path)
        self.rdep_tests = bazel.query_rdep_tests(self.modules)

    def get_rdep_tests(self):
        return self.rdep_tests


class TestMapping(object):
    def __init__(self):
        self.env = Env()
        self.bazel = Bazel(self.env)

    def create_test_mapping(self, path):
        tests = self.get_tests(path)
        if not bool(tests):
            return
        test_mapping = self.tests_to_mapping(tests)
        self.write_test_mapping(test_mapping)

    def get_tests(self, path):
        # for each path collect local Rust modules.
        if path is not None and path != "":
            return Crate(self.env.cwd_relative + "/" + path, self.bazel).get_rdep_tests()
        else:
            return Crate(self.env.cwd_relative, self.bazel).get_rdep_tests()

    def tests_to_mapping(self, tests):
        test_mapping = {"presubmit": []}
        for test in tests:
            if test in test_exclude:
                continue
            if test in test_options:
                test_mapping["presubmit"].append({"name": test, "options": test_options[test]})
            else:
                test_mapping["presubmit"].append({"name": test})
        return test_mapping

    def write_test_mapping(self, test_mapping):
        with open("TEST_MAPPING", "w") as json_file:
            json_file.write("// Generated by cargo2android.py for tests that depend on this crate.\n")
            json.dump(test_mapping, json_file, indent=2, separators=(',', ': '), sort_keys=True)
            json_file.write("\n")

def main():
    TestMapping().create_test_mapping(None)

if __name__ == '__main__':
  main()
