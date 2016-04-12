#!/usr/bin/python
#
# Copyright (C) 2016 The Android Open Source Project
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

"""Reset a USB device (presumbly android phone) by serial number.

Given a serial number, inspects connected USB devices and issues USB
reset to the one that matches. Python version written by Than
McIntosh, based on a perl version from Chris Ferris. Intended for use
on linux.

"""

import fcntl
import getopt
import locale
import os
import re
import shlex
import subprocess
import sys

# Serial number of device that we want to reset
flag_serial = None

# Debugging verbosity level (0 -> no output)
flag_debug = 0

USBDEVFS_RESET = ord("U") << (4*2) | 20


def verbose(level, msg):
  """Print debug trace output of verbosity level is >= value in 'level'."""
  if level <= flag_debug:
    sys.stderr.write(msg + "\n")


def increment_verbosity():
  """Increment debug trace level by 1."""
  global flag_debug
  flag_debug += 1


def issue_ioctl_to_device(device):
  """Issue USB reset ioctl to device."""

  try:
    fd = open(device, "wb")
  except IOError as e:
    error("unable to open device %s: "
          "%s" % (device, e.strerror))
  verbose(1, "issuing USBDEVFS_RESET ioctl() to %s" % device)
  fcntl.ioctl(fd, USBDEVFS_RESET, 0)
  fd.close()


# perform default locale setup if needed
def set_default_lang_locale():
  if "LANG" not in os.environ:
    warning("no env setting for LANG -- using default values")
    os.environ["LANG"] = "en_US.UTF-8"
    os.environ["LANGUAGE"] = "en_US:"


def warning(msg):
  """Issue a warning to stderr."""
  sys.stderr.write("warning: " + msg + "\n")


def error(msg):
  """Issue an error to stderr, then exit."""
  sys.stderr.write("error: " + msg + "\n")
  exit(1)


# invoke command, returning array of lines read from it
def docmdlines(cmd, nf=None):
  """Run a command via subprocess, returning output as an array of lines."""
  verbose(2, "+ docmdlines executing: %s" % cmd)
  args = shlex.split(cmd)
  mypipe = subprocess.Popen(args, stdout=subprocess.PIPE)
  encoding = locale.getdefaultlocale()[1]
  pout, perr = mypipe.communicate()
  if mypipe.returncode != 0:
    if perr:
      decoded_err = perr.decode(encoding)
      warning(decoded_err)
    if nf:
      return None
    error("command failed (rc=%d): cmd was %s" % (mypipe.returncode, args))
  decoded = pout.decode(encoding)
  lines = decoded.strip().split("\n")
  return lines


def perform():
  """Main driver routine."""
  lines = docmdlines("usb-devices")
  dmatch = re.compile(r"^\s*T:\s*Bus\s*=\s*(\d+)\s+.*\s+Dev#=\s*(\d+).*$")
  smatch = re.compile(r"^\s*S:\s*SerialNumber=(.*)$")
  device = None
  found = False
  for line in lines:
    m = dmatch.match(line)
    if m:
      p1 = int(m.group(1))
      p2 = int(m.group(2))
      device = "/dev/bus/usb/%03d/%03d" % (p1, p2)
      verbose(1, "setting device: %s" % device)
      continue
    m = smatch.match(line)
    if m:
      ser = m.group(1)
      if ser == flag_serial:
        verbose(0, "matched serial %s to device "
                "%s, invoking reset" % (ser, device))
        issue_ioctl_to_device(device)
        found = True
        break
  if not found:
    error("unable to locate device with serial number %s" % flag_serial)


def usage(msgarg):
  """Print usage and exit."""
  if msgarg:
    sys.stderr.write("error: %s\n" % msgarg)
  print """\
    usage:  %s [options] XXYYZZ

    where XXYYZZ is the serial number of a connected Android device.

    options:
    -d    increase debug msg verbosity level

    """ % os.path.basename(sys.argv[0])
  sys.exit(1)


def parse_args():
  """Command line argument parsing."""
  global flag_serial

  try:
    optlist, args = getopt.getopt(sys.argv[1:], "d")
  except getopt.GetoptError as err:
    # unrecognized option
    usage(str(err))
  if not args or len(args) != 1:
    usage("supply a single device serial number as argument")
  flag_serial = args[0]

  for opt, _ in optlist:
    if opt == "-d":
      increment_verbosity()


set_default_lang_locale()
parse_args()
perform()
