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
specified, this will default to all APKs under "<ANDROID_PRODUCT_OUT>/\
<all the partitions>/priv-app/".

    To specify a target partition(s), use "-p <PARTITION>," where <PARTITION> \
can be "system", "product", "system/product", "system_ext", \
"system/system_ext", "system,system/product,vendor,system_ext", etc.

    When using adb, adb pull can take a long time. To see the adb pull \
progress, use "-v"

Examples:

    For all APKs under $ANDROID_PRODUCT_OUT/<all partitions>/priv-app/:
        # If the build environment has not been set up, do so:
        . build/envsetup.sh
        lunch product_name
        m -j32
        # then use:
        cd development/tools/privapp_permissions/
        ./privapp_permissions.py
        # or to search for apks in "product" partition
        ./privapp_permissions.py -p product
        # or to search for apks in system, product, and vendor partitions
        ./privapp_permissions.py -p system,product,vendor

    For an APK against $ANDROID_PRODUCT_OUT/<all partitions>/etc/permissions/:
        ./privapp_permissions.py path/to/the.apk
        # or against /product/etc/permissions/
        ./privapp_permissions.py path/to/the.apk -p product

    For an APK already on the device against /<all partitions>/etc/permissions/:
        ./privapp_permissions.py device:/device/path/to/the.apk
        # or against /product/etc/permissions/
        ./privapp_permissions.py path/to/the.apk -p product

    For all APKs on a device under /<all partitions>/priv-app/:
        ./privapp_permissions.py -d
        # or if more than one device is attached
        ./privapp_permissions.py -s <ANDROID_SERIAL>
        # or for all APKs on the "system" partitions
        ./privapp_permissions.py -d -p system
"""

# An array of all generated temp directories.
temp_dirs = []
# An array of all generated temp files.
temp_files = []

def vprint(enable, message, *args):
    if enable:
        # Use stderr to avoid poluting print_xml result
        sys.stderr.write(message % args + '\n')

class MissingResourceError(Exception):
    """Raised when a dependency cannot be located."""


class Adb(object):
    """A small wrapper around ADB calls."""

    def __init__(self, path, serial=None, verbose=False):
        self.path = path
        self.serial = serial
        self.verbose = verbose

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
        self.call('pull %s %s' % (src, dst), False, self.verbose)
        return dst

    def call(self, cmdline, getoutput=True, verbose=False):
        """Calls an adb command.

        Throws:
            subprocess.CalledProcessError upon command failure.
        """
        command = '%s -s %s %s' % (self.path, self.serial, cmdline)
        if getoutput:
            return get_output(command)
        else:
            # Handle verbose mode only when the output is not needed
            # This is mainly for adb pull, which can take a long time
            extracmd = ' > /dev/null 2>&1'
            if verbose:
                # Use stderr to avoid poluting print_xml result
                extracmd = ' 1>&2'
            os.system(command + extracmd)

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
                 serial=None, partitions=None, verbose=False,
                 writetodisk=None, systemfile=None, productfile=None,
                 apks=None):
        self.adb = Resources._resolve_adb(adb_path)
        self.aapt = Resources._resolve_aapt(aapt_path)

        self.verbose = self.adb.verbose = verbose
        self.writetodisk = writetodisk
        self.systemfile = systemfile;
        self.productfile = productfile;

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

        if apks and (partitions == "all" or partitions.find(',') != -1):
            # override the partition to "system
            print('\n# Defaulting the target partition to "system". '
                  'Use -p option to specify the target partition '
                  '(must provide one target instead of a list).\n',
                  file=sys.stderr)
            partitions = "system"

        if partitions == "all":
            # This is the default scenario
            # Find all the partitions where priv-app exists
            self.partitions = self._get_partitions()
        else:
            # Initialize self.partitions with the specified partitions
            self.partitions = []
            for p in partitions.split(','):
                if p.endswith('/'):
                    p = p[:-1]
                self.partitions.append(p)
                # Check if the directory exists
                self._check_dir(p + '/priv-app')

        vprint(self.verbose,
                '# Examining the partitions: ' + str(self.partitions))

        # Create dictionary of array (partition as the key)
        self.privapp_apks = self._resolve_apks(apks, self.partitions)
        self.permissions_dirs = self._resolve_sys_paths('etc/permissions',
                                                       self.partitions)
        self.sysconfig_dirs = self._resolve_sys_paths('etc/sysconfig',
                                                     self.partitions)

        # Always use the one in /system partition,
        # as that is the only place we will find framework-res.apk
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

    def _get_partitions(self):
        """Find all the partitions to examine

        Returns:
            The array of partitions where priv-app exists
        Raises:
            MissingResourceError find command over adb shell fails.
        """
        if not self.adb.serial:
            privapp_dirs = get_output('cd  %s; find * -name "priv-app"'
                                      % os.environ['ANDROID_PRODUCT_OUT']
                                      + ' -type d | grep -v obj').split()
        else:
            try:
                privapp_dirs = self.adb.call('shell find \'/!(proc)\' \
                                           -name "priv-app" -type d').split()
            except subprocess.CalledProcessError:
                raise MissingResourceError(
                    '"adb shell find / -name priv-app -type d" did not succeed'
                    ' on device "%s".' % self.adb.serial)

        # Remove 'priv-app' from the privapp_dirs
        partitions = []
        for i in range(len(privapp_dirs)):
            partitions.append('/'.join(privapp_dirs[i].split('/')[:-1]))

        return partitions

    def _check_dir(self, directory):
        """Check if a given directory is valid

        Raises:
            MissingResourceError if a given directory does not exist.
        """
        if not self.adb.serial:
            if not os.path.isdir(os.environ['ANDROID_PRODUCT_OUT']
                                 + '/' + directory):
                raise MissingResourceError(
                    '%s does not exist' % directory)
        else:
            try:
                self.adb.call('shell ls %s' % directory)
            except subprocess.CalledProcessError:
                raise MissingResourceError(
                    '"adb shell ls %s" did not succeed on '
                    'device "%s".' % (directory, self.adb.serial))


    def _resolve_apks(self, apks, partitions):
        """Resolves all APKs to run against.

        Returns:
            If no apk is specified in the arguments, return all apks in
            priv-app in all the partitions.
            Otherwise, returns a list with the specified apk.
        Throws:
            MissingResourceError if the specified apk or
            <partition>/priv-app cannot be found.
        """
        results = {}
        if not apks:
            for p in partitions:
                results[p] = self._resolve_all_privapps(p)
            return results

        # The first element is what is passed via '-p' option
        # (default is overwritten to 'system' when apk is specified)
        p = partitions[0]
        results[p] = []
        for apk in apks:
            if apk.startswith(DEVICE_PREFIX):
                device_apk = apk[len(DEVICE_PREFIX):]
                try:
                    apk = self.adb.pull(device_apk)
                except subprocess.CalledProcessError:
                    raise MissingResourceError(
                        'File "%s" could not be located on device "%s".' %
                        (device_apk, self.adb.serial))
                results[p].append(apk)
            elif not os.path.isfile(apk):
                raise MissingResourceError('File "%s" does not exist.' % apk)
            else:
               results[p].append(apk)
        return results

    def _resolve_all_privapps(self, partition):
        """Resolves all APKs in <partition>/priv-app

        Returns:
            Return all apks in <partition>/priv-app
        Throws:
            MissingResourceError <partition>/priv-app cannot be found.
        """
        if not self.adb.serial:
            priv_app_dir = os.path.join(os.environ['ANDROID_PRODUCT_OUT'],
                                        partition + '/priv-app')
        else:
            try:
                priv_app_dir = self.adb.pull(partition + '/priv-app/')
            except subprocess.CalledProcessError:
                raise MissingResourceError(
                    'Directory "%s/priv-app" could not be pulled from on '
                    'device "%s".' % (partition, self.adb.serial))
        return get_output('find %s -name "*.apk"' % priv_app_dir).split()

    def _resolve_sys_path(self, file_path):
        """Resolves a path that is a part of an Android System Image."""
        if not self.adb.serial:
            return os.path.join(os.environ['ANDROID_PRODUCT_OUT'], file_path)
        else:
            return self.adb.pull(file_path)

    def _resolve_sys_paths(self, file_path, partitions):
        """Resolves a path that is a part of an Android System Image, for the
        specified partitions."""
        results = {}
        for p in partitions:
            results[p] = self._resolve_sys_path(p + '/' + file_path)
        return results


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
        '-v',
        '--verbose',
        action='store_true',
        default=False,
        required=False,
        help='Whether or not to enable more verbose logs such as '
             'adb pull progress to be shown'
    )
    parser.add_argument(
        '--adb',
        type=str,
        required=False,
        metavar='<ADB_PATH>',
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
        '-p',
        '--partitions',
        type=str,
        required=False,
        default='all',
        metavar='<PARTITION>',
        help='The target partition(s) to examine permissions for. '
             'It is set to "all" by default, which means all the partitions '
             'where priv-app diectory exists will be examined'
             'Use "," as a delimiter when specifying multiple partitions. '
             'E.g. "system,product"'
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


def create_permission_file(resources):
    """Prints out/creates permission file with missing permissions."""
    # First extract privileged permissions from framework-res.apk
    priv_permissions = extract_priv_permissions(resources.aapt,
                                                resources.framework_res_apk)

    results = {}
    for p in resources.partitions:
        results[p], apps_redefine_base = \
            generate_missing_permissions(resources, priv_permissions, p)
        enable_print = True
        vprint(enable_print, '#' * 80)
        vprint(enable_print, '#')
        if resources.writetodisk:
            # Check if it is likely a product partition
            if p.endswith('product'):
                out_file_name = resources.productfile;
            # Check if it is a system partition
            elif p.endswith('system'):
                out_file_name = resources.systemfile
            # Fallback to the partition name itself
            else:
                out_file_name = str(p).replace('/', '_') + '.xml'

            out_file = open(out_file_name, 'w')
            vprint(enable_print, '# %s XML written to %s:', p, out_file_name)
            vprint(enable_print, '#')
            vprint(enable_print, '#' * 80)
            print_xml(results[p], apps_redefine_base, p, out_file)
            out_file.close()
        else:
            vprint(enable_print, '# %s XML:', p)
            vprint(enable_print, '#')
            vprint(enable_print, '#' * 80)

        # Print it to stdout regardless of whether writing to a file or not
        print_xml(results[p], apps_redefine_base, p)


def generate_missing_permissions(resources, priv_permissions, partition):
    """Generates the missing permissions for the specified partition."""
    # Parse base XML files in /etc dir, permissions listed there don't have
    # to be re-added
    base_permissions = {}
    base_xml_files = itertools.chain(
        list_xml_files(resources.permissions_dirs[partition]),
        list_xml_files(resources.sysconfig_dirs[partition]))

    for xml_file in base_xml_files:
        parse_config_xml(xml_file, base_permissions)

    apps_redefine_base = []
    results = {}
    for priv_app in resources.privapp_apks[partition]:
        pkg_info = extract_pkg_and_requested_permissions(resources.aapt,
                                                         priv_app)
        pkg_name = pkg_info['package_name']
        # get intersection of what's requested by app and by framework
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

    return results, apps_redefine_base


def print_xml(results, apps_redefine_base, partition, fd=sys.stdout):
    """Print results to the given file."""
    fd.write('<?xml version="1.0" encoding="utf-8"?>\n')
    fd.write('<!-- for the partition: /%s -->\n' % partition)
    fd.write('<permissions>\n')
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
            partitions=args.partitions,
            verbose=args.verbose,
            writetodisk=args.writetodisk,
            systemfile=args.systemfile,
            productfile=args.productfile,
            apks=args.apks
        )
        create_permission_file(tool_resources)
    except MissingResourceError as e:
        print(str(e), file=sys.stderr)
        exit(1)
    finally:
        cleanup()
