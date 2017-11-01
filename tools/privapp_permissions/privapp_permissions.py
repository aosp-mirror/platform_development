#!/usr/bin/env python

#
# Copyright 2016, The Android Open Source Project
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
#

"""
  privapp_permission.py: Generates privapp-permissions.xml file for
  apps in system/priv-app directory

  Usage:
  . build/envsetup.sh
  lunch product_name
  m -j32
  development/tools/privapp_permissions/privapp_permissions.py [package_name]

"""

import os
import re
import subprocess
import sys
from xml.dom import minidom

try:
    ANDROID_PRODUCT_OUT = os.environ['ANDROID_PRODUCT_OUT']
    ANDROID_HOST_OUT = os.environ['ANDROID_HOST_OUT']
except KeyError as e:
    exit("Build environment not set up - " + str(e))
BASE_XML_FNAME = "privapp-permissions-platform.xml"

def main():
    # Parse base XML files in /etc dir, permissions listed there don't have to be re-added
    base_permissions = {}
    for xml_file in list_config_xml_files():
        parse_config_xml(xml_file, base_permissions)

    # Extract signature|privileged permissions available in the platform
    framework_apk = os.path.join(ANDROID_PRODUCT_OUT, 'system/framework/framework-res.apk')
    platform_priv_permissions = extract_priv_permissions(framework_apk)

    priv_apps = [sys.argv[1]] if len(sys.argv) > 1 else list_privapps()
    apps_redefine_base = []
    results = {}
    for priv_app in priv_apps:
        pkg_info = extract_pkg_and_requested_permissions(priv_app)
        pkg_name = pkg_info['package_name']
        priv_perms = get_priv_permissions(pkg_info['permissions'], platform_priv_permissions)
        # Compute diff against permissions defined in base file
        if base_permissions and (pkg_name in base_permissions):
            base_permissions_pkg = base_permissions[pkg_name]
            priv_perms = remove_base_permissions(priv_perms, base_permissions_pkg)
            if priv_perms:
                apps_redefine_base.append(pkg_name)
        if priv_perms:
            results[pkg_name] = sorted(priv_perms)

    print_xml(results, apps_redefine_base)

def print_xml(results, apps_redefine_base):
    """
    Print results to xml file
    """
    print """\
<?xml version="1.0" encoding="utf-8"?>
<permissions>"""
    for package_name in sorted(results):
        if package_name in apps_redefine_base:
            print '    <!-- Additional permissions on top of %s -->' % BASE_XML_FNAME
        print '    <privapp-permissions package="%s">' % package_name
        for p in results[package_name]:
            print '        <permission name="%s"/>' % p
        print '    </privapp-permissions>'
        print

    print "</permissions>"

def remove_base_permissions(priv_perms, base_perms):
    """
    Removes set of base_perms from set of priv_perms
    """
    if (not priv_perms) or (not base_perms): return priv_perms
    return set(priv_perms) - set(base_perms)

def get_priv_permissions(requested_perms, priv_perms):
    """
    Return only permissions that are in priv_perms set
    """
    return set(requested_perms).intersection(set(priv_perms))

def list_privapps():
    """
    Extract package name and requested permissions.
    """
    priv_app_dir = os.path.join(ANDROID_PRODUCT_OUT, 'system/priv-app')
    apks = []
    for dirName, subdirList, fileList in os.walk(priv_app_dir):
        for fname in fileList:
            if fname.endswith(".apk"):
                file_path = os.path.join(dirName, fname)
                apks.append(file_path)

    return apks

def list_config_xml_files():
    """
    Extract package name and requested permissions.
    """
    perm_dir = os.path.join(ANDROID_PRODUCT_OUT, 'system/etc/permissions')
    conf_dir = os.path.join(ANDROID_PRODUCT_OUT, 'system/etc/sysconfig')

    xml_files = []
    for root_dir in [perm_dir, conf_dir]:
        for dirName, subdirList, fileList in os.walk(root_dir):
            for fname in fileList:
                if fname.endswith(".xml"):
                    file_path = os.path.join(dirName, fname);
                    xml_files.append(file_path)
    return xml_files


def extract_pkg_and_requested_permissions(apk_path):
    """
    Extract package name and list of requested permissions from the
    dump of manifest file
    """
    aapt_args = ["d", "permissions", apk_path]
    txt = aapt(aapt_args)

    permissions = []
    package_name = None
    rawLines = txt.split('\n')
    for line in rawLines:
        regex = r"uses-permission: name='([\S]+)'"
        matches = re.search(regex, line)
        if matches:
            name = matches.group(1)
            permissions.append(name)
        regex = r"package: ([\S]+)"
        matches = re.search(regex, line)
        if matches:
            package_name = matches.group(1)

    return {'package_name': package_name, 'permissions' : permissions}

def extract_priv_permissions(apk_path):
    """
    Extract list signature|privileged permissions from the dump of
    manifest file
    """
    aapt_args = ["d", "xmltree", apk_path, "AndroidManifest.xml"]
    txt = aapt(aapt_args)
    rawLines = txt.split('\n')
    n = len(rawLines)
    i = 0
    permissions_list = []
    while i<n:
        line = rawLines[i]
        if line.find("E: permission (") != -1:
            i+=1
            name = None
            level = None
            while i<n:
                line = rawLines[i];
                if line.find("E: ") != -1:
                    break
                regex = r'A: android:name\([\S]+\)=\"([\S]+)\"';
                matches = re.search(regex, line);
                if matches:
                    name = matches.group(1)
                    i+=1
                    continue
                regex = r'A: android:protectionLevel\([^\)]+\)=\(type [\S]+\)0x([\S]+)';
                matches = re.search(regex, line);
                if matches:
                    level = int(matches.group(1), 16)
                    i+=1
                    continue
                i+=1
            if name and level and level & 0x12 == 0x12:
                    permissions_list.append(name)
        else:
            i+=1

    return permissions_list

def parse_config_xml(base_xml, results):
    """
    Parse an XML file that will be used as base.
    """
    dom = minidom.parse(base_xml)
    nodes =  dom.getElementsByTagName("privapp-permissions")
    for node in nodes:
        permissions = node.getElementsByTagName("permission")
        package_name = node.getAttribute('package');
        plist = []
        if package_name in results:
            plist = results[package_name]
        for p in permissions:
            perm_name = p.getAttribute('name')
            if perm_name:
                plist.append(perm_name)
        results[package_name] = plist
    return results

def aapt(args):
    """
    Run aapt command
    """
    return subprocess.check_output([ANDROID_HOST_OUT + '/bin/aapt'] + args,
        stderr=subprocess.STDOUT)

if __name__ == '__main__':
    main()
