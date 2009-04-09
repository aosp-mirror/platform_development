#!/usr/bin/python
#
# Copyright (C) 2009 The Android Open Source Project
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
"""
Description:
   This script collects all framework Java sources from the current android
   source code and places them in a source folder suitable for the eclipse ADT
   plugin.

See usage() below.

Copyright (C) 2009 The Android Open Source Project
Licensed under the Apache License, Version 2.0 (the "License").
"""

import re
import os
import sys
import getopt
import shutil

_RE_PKG = re.compile("^\s*package\s+([^\s;]+)\s*;.*")

# Holds cmd-line arguments
class Params(object):
    def __init__(self):
        self.DRY = False
        self.DIR = "frameworks"
        self.SRC = None
        self.DST = None
        self.CNT_USED = 0
        self.CNT_NOPKG = 0


# Prints a usage summary
def usage(error=None):
    print """
 Description:
   This script collects all framework Java sources from the current android
   source code and places them in a source folder suitable for the eclipse ADT
   plugin.

 Usage:
   %s [-n] <android-git-repo root> <sdk/platforms/xyz/sources>
 
 The source and destination directories must already exist.
 Use -n for a dry-run.

""" % sys.argv[0]

    if error:
        print >>sys.stderr, "Error:", error


# Parse command line args, returns a Params instance or sys.exit(2) on error
# after printing the error and the usage.
def parseArgs(argv):
    p = Params()
    error = None
    
    try:
        opts, args = getopt.getopt(argv[1:],
                                   "ns:",
                                   [ "--dry", "--sourcedir=" ])
    except getopt.GetoptError, e:
        error = str(e)

    if error is None:
        for o, a in opts:
            if o in [ "-n", "--dry" ]:
                p.DRY = True
            elif o in [ "-s", "--sourcedir" ]:
                p.DIR = a

        if len(args) != 2:
            error = "Missing arguments: <source> <dest>"
        else:
            p.SRC = args[0]
            p.DST = args[1]

            if not os.path.isdir(p.SRC):
                error = "%s is not a directory" % p.SRC
            elif not os.path.isdir(p.DST):
                error = "%s is not a directory" % p.DST

    if error:
        usage(error)
        sys.exit(2)

    return p


# Recursively parses the given directory and process java files found
def parseSrcDir(p, srcdir):
    for f in os.listdir(srcdir):
        fp = os.path.join(srcdir, f)
        if f.endswith(".java") and os.path.isfile(fp):
            pkg = checkJavaFile(fp)
            if pkg:
                pkg = pkg.replace(".", os.path.sep)  # e.g. android.view => android/view
                copy(p, fp, f, pkg)
                p.CNT_USED += 1   # one more copied
            else:
                p.CNT_NOPKG += 1  # this java file lacked a package declaration
        elif os.path.isdir(fp):
            parseSrcDir(p, fp)


# Check a java file to find its package declaration, if any
def checkJavaFile(path):
    print "Process", path

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

# Create destination directory based on package name then copy the
# source file in there
def copy(p, fp, f, pkg):
    dstdir = os.path.join(p.DST, pkg)
    _mkdir(p, dstdir)
    _cp(p, fp, os.path.join(dstdir, f))

def _mkdir(p, dir):
    if not os.path.isdir(dir):
        if p.DRY:
            print "mkdir", dir
        else:
            os.makedirs(dir)

def _cp(p, src, dst):
    if p.DRY:
        print "cp", src, dst
    else:
        shutil.copyfile(src, dst)


def main():
    p = parseArgs(sys.argv)
    parseSrcDir(p, os.path.join(p.SRC, p.DIR))
    print "%d java files copied" % p.CNT_USED
    if p.CNT_NOPKG: print "%d java files ignored (no package)" % p.CNT_NOPKG
    if p.DRY: print "This was in *DRY* mode. No copies done."

if __name__ == "__main__":
    main()
