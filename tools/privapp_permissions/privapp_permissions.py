#!/usr/bin/env python
#
#   Copyright 2017 - The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

from __future__ import print_function
from xml.dom import minidom

import argparse
import itertools
import os
import re
import subprocess
import sys
import tempfile
import shutil

DEVICE_PREFIX = 'device:'
ANDROID_NAME_REGEX = r'A: android:name\([\S]+\)=\"([\S]+)\"'
ANDROID_PROTECTION_LEVEL_REGEX = \
    r'A: android:protectionLevel\([^\)]+\)=\(type [\S]+\)0x([\S]+)'
BASE_XML_FILENAME = 'privapp-permissions-platform.xml'

HELP_MESSAGE = """\
Generates privapp-permissions.xml file for priv-apps.

Usage:
    Specify which apk to generate priv-app permissions for. If no apk is \
specified, this will default to all APKs under "<ANDROID_PRODUCT_OUT>/ \
system/priv-app and (system/)product/priv-app".

Examples:

    For all APKs under $ANDROID_PRODUCT_OUT:
        # If the build environment has not been set up, do so:
        . build/envsetup.sh
        lunch product_name
        m -j32
        # then use:
        cd development/tools/privapp_permissions/
        ./privapp_permissions.py

    For a given apk:
        ./privapp_permissions.py path/to/the.apk

    For an APK already on the device:
        ./privapp_permissions.py device:/device/path/to/the.apk

    For all APKs on a device:
        ./privapp_permissions.py -d
        # or if more than one device is attached
        ./privapp_permissions.py -s <ANDROID_SERIAL>\
"""

# An array of all generated temp directories.
temp_dirs = []
# An array of all generated temp files.
temp_files = []


class MissingResourceError(Exception):
    """Raised when a dependency cannot be located."""


class Adb(object):
    """A small wrapper around ADB calls."""

    def __init__(self, path, serial=None):
        self.path = path
        self.serial = serial

    def pull(self, src, dst=None):
        """A wrapper for `adb -s <SERIAL> pull <src> <dst>`.
        Args:
            src: The source path on the device
            dst: The destination path on the host

        Throws:
            subprocess.CalledProcessError upon pull failure.
        """
        if not dst:
            if self.call('shell \'if [ -d "%s" ]; then echo True; fi\'' % src):
                dst = tempfile.mkdtemp()
                temp_dirs.append(dst)
            else:
                _, dst = tempfile.mkstemp()
                temp_files.append(dst)
        self.call('pull %s %s' % (src, dst))
        return dst

    def call(self, cmdline):
        """Calls an adb command.

        Throws:
            subprocess.CalledProcessError upon command failure.
        """
        command = '%s -s %s %s' % (self.path, self.serial, cmdline)
        return get_output(command)


class Aapt(object):
    def __init__(self, path):
        self.path = path

    def call(self, arguments):
        """Run an aapt command with the given args.

        Args:
            arguments: a list of string arguments
        Returns:
            The output of the aapt command as a string.
        """
        output = subprocess.check_output([self.path] + arguments,
                                         stderr=subprocess.STDOUT)
        return output.decode(encoding='UTF-8')


class Resources(object):
    """A class that contains the resources needed to generate permissions.

    Attributes:
        adb: A wrapper class around ADB with a default serial. Only needed when
             using -d, -s, or "device:"
        _aapt_path: The path to aapt.
    """

    def __init__(self, adb_path=None, aapt_path=None, use_device=None,
                 serial=None, apks=None):
        self.adb = Resources._resolve_adb(adb_path)
        self.aapt = Resources._resolve_aapt(aapt_path)

        self._is_android_env = 'ANDROID_PRODUCT_OUT' in os.environ and \
                               'ANDROID_HOST_OUT' in os.environ
        use_device = use_device or serial or \
                     (apks and DEVICE_PREFIX in '&'.join(apks))

        self.adb.serial = self._resolve_serial(use_device, serial)

        if self.adb.serial:
            self.adb.call('root')
            self.adb.call('wait-for-device')

        if self.adb.serial is None and not self._is_android_env:
            raise MissingResourceError(
                'You must either set up your build environment, or specify a '
                'device to run against. See --help for more info.')

        self.system_privapp_apks, self.product_privapp_apks =(
                self._resolve_apks(apks))
        self.system_permissions_dir = (
                self._resolve_sys_path('system/etc/permissions'))
        self.system_sysconfig_dir = (
                self._resolve_sys_path('system/etc/sysconfig'))
        self.product_permissions_dir = (
                self._resolve_sys_path('product/etc/permissions',
                                       'system/product/etc/permissions'))
        self.product_sysconfig_dir = (
                self._resolve_sys_path('product/etc/sysconfig',
                                       'system/product/etc/sysconfig'))
        self.framework_res_apk = self._resolve_sys_path('system/framework/'
                                                        'framework-res.apk')

    @staticmethod
    def _resolve_adb(adb_path):
        """Resolves ADB from either the cmdline argument or the os environment.

        Args:
            adb_path: The argument passed in for adb. Can be None.
        Returns:
            An Adb object.
        Raises:
            MissingResourceError if adb cannot be resolved.
        """
        if adb_path:
            if os.path.isfile(adb_path):
                adb = adb_path
            else:
                raise MissingResourceError('Cannot resolve adb: No such file '
                                           '"%s" exists.' % adb_path)
        else:
            try:
                adb = get_output('which adb').strip()
            except subprocess.CalledProcessError as e:
                print('Cannot resolve adb: ADB does not exist within path. '
                      'Did you forget to setup the build environment or set '
                      '--adb?',
                      file=sys.stderr)
                raise MissingResourceError(e)
        # Start the adb server immediately so server daemon startup
        # does not get added to the output of subsequent adb calls.
        try:
            get_output('%s start-server' % adb)
            return Adb(adb)
        except:
            print('Unable to reach adb server daemon.', file=sys.stderr)
            raise

    @staticmethod
    def _resolve_aapt(aapt_path):
        """Resolves AAPT from either the cmdline argument or the os environment.

        Returns:
            An Aapt Object
        """
        if aapt_path:
            if os.path.isfile(aapt_path):
                return Aapt(aapt_path)
            else:
                raise MissingResourceError('Cannot resolve aapt: No such file '
                                           '%s exists.' % aapt_path)
        else:
            try:
                return Aapt(get_output('which aapt').strip())
            except subprocess.CalledProcessError:
                print('Cannot resolve aapt: AAPT does not exist within path. '
                      'Did you forget to setup the build environment or set '
                      '--aapt?',
                      file=sys.stderr)
                raise

    def _resolve_serial(self, device, serial):
        """Resolves the serial used for device files or generating permissions.

        Returns:
            If -s/--serial is specified, it will return that serial.
            If -d or device: is found, it will grab the only available device.
            If there are multiple devices, it will use $ANDROID_SERIAL.
        Raises:
            MissingResourceError if the resolved serial would not be usable.
            subprocess.CalledProcessError if a command error occurs.
        """
        if device:
            if serial:
                try:
                    output = get_output('%s -s %s get-state' %
                                        (self.adb.path, serial))
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'Received error when trying to get the state of '
                        'device with serial "%s". Is it connected and in '
                        'device mode?' % serial)
                if 'device' not in output:
                    raise MissingResourceError(
                        'Device "%s" is not in device mode. Reboot the phone '
                        'into device mode and try again.' % serial)
                return serial

            elif 'ANDROID_SERIAL' in os.environ:
                serial = os.environ['ANDROID_SERIAL']
                command = '%s -s %s get-state' % (self.adb, serial)
                try:
                    output = get_output(command)
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'Device with serial $ANDROID_SERIAL ("%s") not '
                        'found.' % serial)
                if 'device' in output:
                    return serial
                raise MissingResourceError(
                    'Device with serial $ANDROID_SERIAL ("%s") was '
                    'found, but was not in the "device" state.')

            # Parses `adb devices` so it only returns a string of serials.
            get_serials_cmd = ('%s devices | tail -n +2 | head -n -1 | '
                               'cut -f1' % self.adb.path)
            try:
                output = get_output(get_serials_cmd)
                # If multiple serials appear in the output, raise an error.
                if len(output.split()) > 1:
                    raise MissingResourceError(
                        'Multiple devices are connected. You must specify '
                        'which device to run against with flag --serial.')
                return output.strip()
            except subprocess.CalledProcessError:
                print('Unexpected error when querying for connected '
                      'devices.', file=sys.stderr)
                raise

    def _resolve_apks(self, apks):
        """Resolves all APKs to run against.

        Returns:
            If no apk is specified in the arguments, return all apks in
            system/priv-app. Otherwise, returns a list with the specified apk.
        Throws:
            MissingResourceError if the specified apk or system/priv-app cannot
            be found.
        """
        if not apks:
            return (self._resolve_all_system_privapps(),
                   self._resolve_all_product_privapps())

        ret_apks = []
        for apk in apks:
            if apk.startswith(DEVICE_PREFIX):
                device_apk = apk[len(DEVICE_PREFIX):]
                try:
                    apk = self.adb.pull(device_apk)
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'File "%s" could not be located on device "%s".' %
                        (device_apk, self.adb.serial))
                ret_apks.append(apk)
            elif not os.path.isfile(apk):
                raise MissingResourceError('File "%s" does not exist.' % apk)
            else:
                ret_apks.append(apk)
        return ret_apks, None

    def _resolve_all_system_privapps(self):
        """Extract package name and requested permissions."""
        if self._is_android_env:
            system_priv_app_dir = (
                    os.path.join(os.environ['ANDROID_PRODUCT_OUT'],
                                            'system/priv-app'))
        else:
            try:
                system_priv_app_dir = self.adb.pull('/system/priv-app/')
            except subprocess.CalledProcessError:
                raise MissingResourceError(
                    'Directory "/system/priv-app" could not be pulled from on '
                    'device "%s".' % self.adb.serial)

        return get_output('find %s -name "*.apk"' % system_priv_app_dir).split()

    def _resolve_all_product_privapps(self):
        """Extract package name and requested permissions."""
        if self._is_android_env:
            product_priv_app_dir = (
                    os.path.join(os.environ['ANDROID_PRODUCT_OUT'],
                                            'product/priv-app'))
            if not os.path.exists(product_priv_app_dir):
                product_priv_app_dir  = (
                        os.path.join(os.environ['ANDROID_PRODUCT_OUT'],
                                                'system/product/priv-app'))
        else:
            try:
                product_priv_app_dir = self.adb.pull('/product/priv-app/')
            except subprocess.CalledProcessError:
                print('Warning: Directory "/product/priv-app" could not be '
                        'pulled from on device "%s". Trying '
                        '"/system/product/priv-app"' % self.adb.serial,
                        file=sys.stderr)
                try:
                    product_priv_app_dir = (
                            self.adb.pull('/system/product/priv-app/'))
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'Directory "/system/product/priv-app" could not be '
                        'pulled from on device "%s".' % self.adb.serial)

        return get_output(
                'find %s -name "*.apk"' % product_priv_app_dir).split()

    def _resolve_sys_path(self, file_path, fallback_file_path=None):
        """Resolves a path that is a part of an Android System Image."""
        if self._is_android_env:
            sys_path = (
                    os.path.join(os.environ['ANDROID_PRODUCT_OUT'], file_path))
            if not os.path.exists(sys_path):
                sys_path = (
                        os.path.join(os.environ['ANDROID_PRODUCT_OUT'],
                        fallback_file_path))
        else:
            try:
                sys_path = self.adb.pull(file_path)
            except subprocess.CalledProcessError:
                print('Warning: Directory %s could not be pulled from on device'
                        '"%s". Trying "/system/product/priv-app"'
                        % (file_path, self.adb.serial), file=sys.stderr)
                try:
                    sys_path = self.adb.pull(fallback_file_path)
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'Directory %s could not be pulled from on '
                        'device "%s".' % (fallback_file_path, self.adb.serial))

        return sys_path


def get_output(command):
    """Returns the output of the command as a string.

    Throws:
        subprocess.CalledProcessError if exit status is non-zero.
    """
    output = subprocess.check_output(command, shell=True)
    # For Python3.4, decode the byte string so it is usable.
    return output.decode(encoding='UTF-8')


def parse_args():
    """Parses the CLI."""
    parser = argparse.ArgumentParser(
        description=HELP_MESSAGE,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        '-d',
        '--device',
        action='store_true',
        default=False,
        required=False,
        help='Whether or not to generate the privapp_permissions file for the '
             'build already on a device. See -s/--serial below for more '
             'details.'
    )
    parser.add_argument(
        '--adb',
        type=str,
        required=False,
        metavar='<ADB_PATH',
        help='Path to adb. If none specified, uses the environment\'s adb.'
    )
    parser.add_argument(
        '--aapt',
        type=str,
        required=False,
        metavar='<AAPT_PATH>',
        help='Path to aapt. If none specified, uses the environment\'s aapt.'
    )
    parser.add_argument(
        '-s',
        '--serial',
        type=str,
        required=False,
        metavar='<SERIAL>',
        help='The serial of the device to generate permissions for. If no '
             'serial is given, it will pick the only device connected over '
             'adb. If multiple devices are found, it will default to '
             '$ANDROID_SERIAL. Otherwise, the program will exit with error '
             'code 1. If -s is given, -d is not needed.'
    )
    parser.add_argument(
        'apks',
        nargs='*',
        type=str,
        help='A list of paths to priv-app APKs to generate permissions for. '
             'To make a path device-side, prefix the path with "device:".'
    )
    parser.add_argument(
        '-w',
        '--writetodisk',
        action='store_true',
        default=False,
        required=False,
        help='Whether or not to store the generated permissions directly to '
             'a file. See --systemfile/--productfile for more information.'
    )
    parser.add_argument(
        '--systemfile',
        default='./system.xml',
        required=False,
        help='Path to system permissions file. Default value is ./system.xml'
    )
    parser.add_argument(
        '--productfile',
        default='./product.xml',
        required=False,
        help='Path to system permissions file. Default value is ./product.xml'
    )
    cmd_args = parser.parse_args()

    return cmd_args

def create_permission_file(resources, privapp_apks, permissions_dir,
            sysconfig_dir, file=None):
    # Parse base XML files in /etc dir, permissions listed there don't have
    # to be re-added
    base_permissions = {}
    base_xml_files = itertools.chain(list_xml_files(permissions_dir),
                                     list_xml_files(sysconfig_dir))
    for xml_file in base_xml_files:
        parse_config_xml(xml_file, base_permissions)

    priv_permissions = extract_priv_permissions(resources.aapt,
                                                resources.framework_res_apk)

    apps_redefine_base = []
    results = {}
    for priv_app in privapp_apks:
        pkg_info = extract_pkg_and_requested_permissions(resources.aapt,
                                                         priv_app)
        pkg_name = pkg_info['package_name']
        priv_perms = get_priv_permissions(pkg_info['permissions'],
                                          priv_permissions)
        # Compute diff against permissions defined in base file
        if base_permissions and (pkg_name in base_permissions):
            base_permissions_pkg = base_permissions[pkg_name]
            priv_perms = remove_base_permissions(priv_perms,
                                                 base_permissions_pkg)
            if priv_perms:
                apps_redefine_base.append(pkg_name)
        if priv_perms:
            results[pkg_name] = sorted(priv_perms)

    print_xml(results, apps_redefine_base)
    if file is not None:
        print_xml(results, apps_redefine_base, file)

def print_xml(results, apps_redefine_base, fd=sys.stdout):
    """Print results to the given file."""
    fd.write('<?xml version="1.0" encoding="utf-8"?>\n<permissions>\n')
    for package_name in sorted(results):
        if package_name in apps_redefine_base:
            fd.write('    <!-- Additional permissions on top of %s -->\n' %
                     BASE_XML_FILENAME)
        fd.write('    <privapp-permissions package="%s">\n' % package_name)
        for p in results[package_name]:
            fd.write('        <permission name="%s"/>\n' % p)
        fd.write('    </privapp-permissions>\n')
        fd.write('\n')

    fd.write('</permissions>\n')


def remove_base_permissions(priv_perms, base_perms):
    """Removes set of base_perms from set of priv_perms."""
    if (not priv_perms) or (not base_perms):
        return priv_perms
    return set(priv_perms) - set(base_perms)


def get_priv_permissions(requested_perms, priv_perms):
    """Return only permissions that are in priv_perms set."""
    return set(requested_perms).intersection(set(priv_perms))


def list_xml_files(directory):
    """Returns a list of all .xml files within a given directory.

    Args:
        directory: the directory to look for xml files in.
    """
    xml_files = []
    for dirName, subdirList, file_list in os.walk(directory):
        for file in file_list:
            if file.endswith('.xml'):
                file_path = os.path.join(dirName, file)
                xml_files.append(file_path)
    return xml_files


def extract_pkg_and_requested_permissions(aapt, apk_path):
    """
    Extract package name and list of requested permissions from the
    dump of manifest file
    """
    aapt_args = ['d', 'permissions', apk_path]
    txt = aapt.call(aapt_args)

    permissions = []
    package_name = None
    raw_lines = txt.split('\n')
    for line in raw_lines:
        regex = r"uses-permission.*: name='([\S]+)'"
        matches = re.search(regex, line)
        if matches:
            name = matches.group(1)
            permissions.append(name)
        regex = r'package: ([\S]+)'
        matches = re.search(regex, line)
        if matches:
            package_name = matches.group(1)

    return {'package_name': package_name, 'permissions': permissions}


def extract_priv_permissions(aapt, apk_path):
    """Extract signature|privileged permissions from dump of manifest file."""
    aapt_args = ['d', 'xmltree', apk_path, 'AndroidManifest.xml']
    txt = aapt.call(aapt_args)
    raw_lines = txt.split('\n')
    n = len(raw_lines)
    i = 0
    permissions_list = []
    while i < n:
        line = raw_lines[i]
        if line.find('E: permission (') != -1:
            i += 1
            name = None
            level = None
            while i < n:
                line = raw_lines[i]
                if line.find('E: ') != -1:
                    break
                matches = re.search(ANDROID_NAME_REGEX, line)
                if matches:
                    name = matches.group(1)
                    i += 1
                    continue
                matches = re.search(ANDROID_PROTECTION_LEVEL_REGEX, line)
                if matches:
                    level = int(matches.group(1), 16)
                    i += 1
                    continue
                i += 1
            if name and level and level & 0x12 == 0x12:
                permissions_list.append(name)
        else:
            i += 1

    return permissions_list


def parse_config_xml(base_xml, results):
    """Parse an XML file that will be used as base."""
    dom = minidom.parse(base_xml)
    nodes = dom.getElementsByTagName('privapp-permissions')
    for node in nodes:
        permissions = (node.getElementsByTagName('permission') +
                       node.getElementsByTagName('deny-permission'))
        package_name = node.getAttribute('package')
        plist = []
        if package_name in results:
            plist = results[package_name]
        for p in permissions:
            perm_name = p.getAttribute('name')
            if perm_name:
                plist.append(perm_name)
        results[package_name] = plist
    return results


def cleanup():
    """Cleans up temp files."""
    for directory in temp_dirs:
        shutil.rmtree(directory, ignore_errors=True)
    for file in temp_files:
        os.remove(file)
    del temp_dirs[:]
    del temp_files[:]


if __name__ == '__main__':
    args = parse_args()
    try:
        tool_resources = Resources(
            aapt_path=args.aapt,
            adb_path=args.adb,
            use_device=args.device,
            serial=args.serial,
            apks=args.apks
        )
        system_permission_file=None
        product_permission_file=None
        print('#' * 80)
        print('#')
        if args.writetodisk:
            print('#System XML written to %s:' % args.systemfile)
            system_permission_file = open(args.systemfile, 'w')
        else:
            print('#System XML:')
        print('#')
        print('#' * 80)
        create_permission_file(
            tool_resources,
            tool_resources.system_privapp_apks,
            tool_resources.system_permissions_dir,
            tool_resources.system_sysconfig_dir,
            system_permission_file)
        if args.writetodisk:
            system_permission_file.close()
        if tool_resources.product_privapp_apks:
            print('#' * 80)
            print('#')
            if args.writetodisk:
                print('#Product XML written to %s:' % args.productfile)
                product_permission_file = open(args.productfile, 'w')
            else:
                print('#Product XML:')
            print('#')
            print('#' * 80)
            create_permission_file(
                tool_resources,
                tool_resources.product_privapp_apks,
                tool_resources.product_permissions_dir,
                tool_resources.product_sysconfig_dir,
                product_permission_file)
            if args.writetodisk:
                product_permission_file.close()
    except MissingResourceError as e:
        print(str(e), file=sys.stderr)
        exit(1)
    finally:
        cleanup()
