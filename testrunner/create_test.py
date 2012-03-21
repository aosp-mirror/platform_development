#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Utility to create Android project files for tests."""

# python imports
import datetime
import optparse
import os
import string
import sys

# local imports
import android_mk
import android_manifest


class TestsConsts(object):
  """Constants for test Android.mk and AndroidManifest.xml creation."""

  MK_BUILD_INCLUDE = "call all-makefiles-under,$(LOCAL_PATH)"
  MK_BUILD_STRING = "\ninclude $(%s)\n" % MK_BUILD_INCLUDE
  TEST_MANIFEST_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<!-- Copyright (C) $YEAR The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$PACKAGE_NAME.tests">

    <application>
        <uses-library android:name="android.test.runner" />
    </application>

    <instrumentation android:name="android.test.InstrumentationTestRunner"
        android:targetPackage="$PACKAGE_NAME"
        android:label="Tests for $MODULE_NAME">
    </instrumentation>
</manifest>
"""
  TEST_MK_TEMPLATE = """LOCAL_PATH := $$(call my-dir)
include $$(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_SRC_FILES := $$(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := ${MODULE_NAME}Tests${CERTIFICATE}

LOCAL_INSTRUMENTATION_FOR := ${MODULE_NAME}

LOCAL_SDK_VERSION := current

include $$(BUILD_PACKAGE)
"""
  TESTS_FOLDER = "tests"


def _GenerateTestManifest(manifest, module_name, mapping=None):
  """Create and populate tests/AndroidManifest.xml with variable values from
  Android.mk and AndroidManifest.xml.

  Does nothing if tests/AndroidManifest.xml already exists.

  Args:
    manifest: AndroidManifest object for application manifest
    module_name: module name used for labelling
    mapping: optional user defined mapping of variable values, replaces values
        extracted from AndroidManifest.xml
  Raises:
    IOError: tests/AndroidManifest.xml cannot be opened for writing
  """
  # skip if file already exists
  tests_path = "%s/%s" % (manifest.GetAppPath(), TestsConsts.TESTS_FOLDER)
  tests_manifest_path = "%s/%s" % (tests_path, manifest.FILENAME)
  if os.path.exists(tests_manifest_path):
    _PrintMessage("%s already exists, not overwritten" % tests_manifest_path)
    return

  if not mapping:
    package_name = manifest.GetPackageName()
    mapping = {"PACKAGE_NAME":package_name, "MODULE_NAME":module_name,
               "YEAR":datetime.date.today().year}
  output = string.Template(TestsConsts.TEST_MANIFEST_TEMPLATE).substitute(mapping)

  # create tests folder if not existent
  if not os.path.exists(tests_path):
    os.mkdir(tests_path)

  # write tests/AndroidManifest.xml
  tests_manifest = open(tests_manifest_path, mode="w")
  tests_manifest.write(output)
  tests_manifest.close()
  _PrintMessage("Created %s" % tests_manifest_path)


def _GenerateTestMK(mk, app_path, mapping=None):
  """Create and populate tests/Android.mk with variable values from Android.mk.

  Does nothing if tests/Android.mk already exists.

  Args:
    mk: AndroidMK object for application makefile
    app_path: path to the application being tested
    mapping: optional user defined mapping of variable values, replaces
        values stored in mk
  Raises:
    IOError: tests/Android.mk cannot be opened for writing
  """
  # skip if file already exists
  tests_path = "%s/%s" % (app_path, TestsConsts.TESTS_FOLDER)
  tests_mk_path = "%s/%s" % (tests_path, mk.FILENAME)
  if os.path.exists(tests_mk_path):
    _PrintMessage("%s already exists, not overwritten" % tests_mk_path)
    return

  # append test build if not existent in makefile
  if not mk.HasInclude(TestsConsts.MK_BUILD_INCLUDE):
    mk_path = "%s/%s" % (app_path, mk.FILENAME)
    mk_file = open(mk_path, mode="a")
    mk_file.write(TestsConsts.MK_BUILD_STRING)
    mk_file.close()

  # construct tests/Android.mk
  # include certificate definition if existent in makefile
  certificate = mk.GetVariable(mk.CERTIFICATE)
  if certificate:
    cert_definition = ("\n%s := %s" % (mk.CERTIFICATE, certificate))
  else:
    cert_definition = ""
  if not mapping:
    module_name = mk.GetVariable(mk.PACKAGE_NAME)
    mapping = {"MODULE_NAME":module_name, "CERTIFICATE":cert_definition}
  output = string.Template(TestsConsts.TEST_MK_TEMPLATE).substitute(mapping)

  # create tests folder if not existent
  if not os.path.exists(tests_path):
    os.mkdir(tests_path)

  # write tests/Android.mk to disk
  tests_mk = open(tests_mk_path, mode="w")
  tests_mk.write(output)
  tests_mk.close()
  _PrintMessage("Created %s" % tests_mk_path)


def _ParseArgs(argv):
  """Parse the command line arguments.

  Args:
    argv: the list of command line arguments
  Returns:
    a tuple of options and individual command line arguments.
  """
  parser = optparse.OptionParser(usage="%s <app_path>" % sys.argv[0])
  options, args = parser.parse_args(argv)
  if len(args) < 1:
    _PrintError("Error: Incorrect syntax")
    parser.print_usage()
    sys.exit()
  return (options, args)


def _PrintMessage(msg):
  print >> sys.stdout, msg


def _PrintError(msg):
  print >> sys.stderr, msg


def _ValidateInputFiles(mk, manifest):
  """Verify that required variables are defined in input files.

  Args:
    mk: AndroidMK object for application makefile
    manifest: AndroidManifest object for application manifest
  Raises:
    RuntimeError: mk does not define LOCAL_PACKAGE_NAME or
                  manifest does not define package variable
  """
  module_name = mk.GetVariable(mk.PACKAGE_NAME)
  if not module_name:
    raise RuntimeError("Variable %s missing from %s" %
        (mk.PACKAGE_NAME, mk.FILENAME))

  package_name = manifest.GetPackageName()
  if not package_name:
    raise RuntimeError("Variable package missing from %s" % manifest.FILENAME)


def main(argv):
  options, args = _ParseArgs(argv)
  app_path = args[0];

  if not os.path.exists(app_path):
    _PrintError("Error: Application path %s not found" % app_path)
    sys.exit()

  try:
    mk = android_mk.CreateAndroidMK(path=app_path)
    manifest = android_manifest.AndroidManifest(app_path=app_path)
    _ValidateInputFiles(mk, manifest)

    module_name = mk.GetVariable(mk.PACKAGE_NAME)
    _GenerateTestMK(mk, app_path)
    _GenerateTestManifest(manifest, module_name)
  except Exception, e:
    _PrintError("Error: %s" % e)
    _PrintError("Error encountered, script aborted")
    sys.exit()

  src_path = app_path + "/tests/src"
  if not os.path.exists(src_path):
    os.mkdir(src_path)


if __name__ == "__main__":
  main(sys.argv[1:])
