#!/usr/bin/python
#
# Copyright (C) 2011 The Android Open Source Project
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

import re
import os
import sys
import getopt
import shutil
import subprocess
import zipfile

VERBOSE = False
TOP_FOLDER = "src"
_RE_PKG = re.compile("^\s*package\s+([^\s;]+)\s*;.*")

# Holds cmd-line arguments and context information
class Params(object):
    def __init__(self):
        self.EXEC_ZIP = False
        self.DRY = False
        self.PROPS = None
        self.SRC = None
        self.DST = None
        self.CNT_USED = 0
        self.CNT_NOPKG = 0
        # DIR is the list of directories to scan in TOPDIR.
        self.DIR = "frameworks libcore"
        self.IGNORE_DIR = [ "hosttests" ]
        # IGNORE is a list of namespaces to ignore. Must be java
        # package definitions (e.g. "com.blah.foo.")
        self.IGNORE = [ "sun.", "libcore.", "dalvik.",
                        "com.test.", "com.google.",
                        "coretestutils.", "test.", "test2.", "tests." ]
        self.zipfile = None


def verbose(msg, *args):
    """Prints a verbose message to stderr if --verbose is set."""
    global VERBOSE
    if VERBOSE:
        if args:
            msg = msg % args
        print >>sys.stderr, msg


# Prints a usage summary
def usage(error=None):
    print """
 Description:
   This script collects all framework Java sources from the current android
   source code and places them in a source.zip file that can be distributed
   by the SDK Manager.

 Usage:
   %s [-n|-v|-z] <source.properties> <sources.zip> <topdir>

 The source.properties file must exist and will be injected in the Zip file.
 The source directory must already exist.
 Use -v for verbose output (lists each file being picked up or ignored).
 Use -n for a dry-run (doesn't write the zip file).
 Use -z to use the system 'zip' command instead of the Python Zip module.

""" % sys.argv[0]

    if error:
        print >>sys.stderr, "Error:", error


# Parse command line args, returns a Params instance or sys.exit(2) on error
# after printing the error and the usage.
def parseArgs(argv):
    global VERBOSE
    p = Params()
    error = None

    try:
        opts, args = getopt.getopt(argv[1:],
                         "zvns:",
                         [ "exec-zip", "verbose", "dry", "sourcedir=" ])
    except getopt.GetoptError, e:
        error = str(e)

    if error is None:
        for o, a in opts:
            if o in [ "-n", "--dry" ]:
                # Dry mode: don't copy/zip, print what would be done.
                p.DRY = True
            if o in [ "-v", "--verbose" ]:
                # Verbose mode. Display everything that's going on.
                VERBOSE = True
            elif o in [ "-s", "--sourcedir" ]:
                # The source directories to process (space separated list)
                p.DIR = a
            elif o in [ "-z", "--exec-zip" ]:
                # Don't use Python zip, instead call the 'zip' system exec.
                p.EXEC_ZIP = True

        if len(args) != 3:
            error = "Missing arguments: <source> <dest>"
        else:
            p.PROPS = args[0]
            p.DST = args[1]
            p.SRC = args[2]

            if not os.path.isfile(p.PROPS):
                error = "%s is not a file" % p.PROPS
            if not os.path.isdir(p.SRC):
                error = "%s is not a directory" % p.SRC

    if error:
        usage(error)
        sys.exit(2)

    return p


# Recursively parses the given directory and processes java files found
def parseSrcDir(p, srcdir):
    if not os.path.exists(srcdir):
        verbose("Error: Skipping unknown directory %s", srcdir)
        return

    for filename in os.listdir(srcdir):
        filepath = os.path.join(srcdir, filename)
        if filename.endswith(".java") and os.path.isfile(filepath):
            pkg = checkJavaFile(filepath)
            if not pkg:
                verbose("No package found in %s", filepath)
            if pkg:
                # Should we ignore this package?
                pkg2 = pkg
                if not "." in pkg2:
                    pkg2 += "."
                for ignore in p.IGNORE:
                    if pkg2.startswith(ignore):
                        verbose("Ignore package %s [%s]", pkg, filepath)
                        pkg = None
                        break

            if pkg:
                pkg = pkg.replace(".", os.path.sep)  # e.g. android.view => android/view
                copy(p, filepath, pkg)
                p.CNT_USED += 1
            else:
                p.CNT_NOPKG += 1
        elif os.path.isdir(filepath):
            if not filename in p.IGNORE_DIR:
                parseSrcDir(p, filepath)


# Check a java file to find its package declaration, if any
def checkJavaFile(path):
    try:
        f = None
        try:
            f = file(path)
            for l in f.readlines():
                m = _RE_PKG.match(l)
                if m:
                    return m.group(1)
        finally:
            if f: f.close()
    except Exception:
        pass

    return None


USED_ARC_PATH = {}

# Copy the given file (given its absolute filepath) to
# the relative desk_pkg directory in the zip file.
def copy(p, filepath, dest_pkg):
    arc_path = os.path.join(TOP_FOLDER, dest_pkg, os.path.basename(filepath))
    if arc_path in USED_ARC_PATH:
        verbose("Ignore duplicate archive path %s", arc_path)
    USED_ARC_PATH[arc_path] = 1
    if p.DRY:
        print >>sys.stderr, "zip %s [%s]" % (arc_path, filepath)
    elif p.zipfile is not None:
        if p.EXEC_ZIP:
            # zipfile is a path. Copy to it.
            dest_path = os.path.join(p.zipfile, arc_path)
            dest_dir = os.path.dirname(dest_path)
            if not os.path.isdir(dest_dir):
                os.makedirs(dest_dir)
            shutil.copyfile(filepath, dest_path)
        else:
            # zipfile is a ZipFile object. Compress with it.
            p.zipfile.write(filepath, arc_path)


def shellExec(*cmd):
  """
  Executes the given system command.

  The command must be split into a list (c.f. shext.split().)

  This raises an exception if the command fails.
  Stdin/out/err are not being redirected.
  """
  verbose("exec: %s", repr(cmd))
  subprocess.check_call(cmd)


def main():
    p = parseArgs(sys.argv)
    z = None
    try:
        if not p.DRY:
            if p.EXEC_ZIP:
                p.zipfile = p.DST + "_temp_dir"
                if os.path.exists(p.zipfile):
                    shutil.rmtree(p.zipfile)
                props_dest = os.path.join(p.zipfile, TOP_FOLDER + "/source.properties")
                os.makedirs(os.path.dirname(props_dest))
                shutil.copyfile(p.PROPS, props_dest)
            else:
                p.zipfile = z = zipfile.ZipFile(p.DST, "w", zipfile.ZIP_DEFLATED)
                z.write(p.PROPS, TOP_FOLDER + "/source.properties")
        for d in p.DIR.split():
            if d:
                parseSrcDir(p, os.path.join(p.SRC, d))
        if p.EXEC_ZIP and not p.DRY:
            curr_dir = os.getcwd()
            os.chdir(p.zipfile)
            if os.path.exists("_temp.zip"):
                os.unlink("_temp.zip");
            shellExec("zip", "-9r", "_temp.zip", TOP_FOLDER)
            os.chdir(curr_dir)
            shutil.move(os.path.join(p.zipfile, "_temp.zip"), p.DST)
            shutil.rmtree(p.zipfile)
    finally:
        if z is not None:
            z.close()
    print "%s: %d java files copied" % (p.DST, p.CNT_USED)
    if p.CNT_NOPKG:
        print "%s: %d java files ignored" % (p.DST, p.CNT_NOPKG)
    if p.DRY:
        print >>sys.stderr, "This was in *DRY* mode. No copies done."


if __name__ == "__main__":
    main()

# For emacs:
# -*- tab-width: 4; -*-
