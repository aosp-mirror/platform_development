#!/usr/bin/env python
#
# Copyright (C) 2015 The Android Open Source Project
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

import adb
import argparse
import logging
import os
import subprocess
import sys

# Shared functions across gdbclient.py and ndk-gdb.py.
import gdbrunner

def get_gdbserver_path(root, arch):
    path = "{}/prebuilts/misc/android-{}/gdbserver{}/gdbserver{}"
    if arch.endswith("64"):
        return path.format(root, arch, "64", "64")
    else:
        return path.format(root, arch, "", "")


def parse_args():
    parser = gdbrunner.ArgumentParser()

    group = parser.add_argument_group(title="attach target")
    group = group.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "-p", dest="target_pid", metavar="PID", type=int,
        help="attach to a process with specified PID")
    group.add_argument(
        "-n", dest="target_name", metavar="NAME",
        help="attach to a process with specified name")
    group.add_argument(
        "-r", dest="run_cmd", metavar="CMD", nargs=argparse.REMAINDER,
        help="run a binary on the device, with args")

    parser.add_argument(
        "--port", nargs="?", default="5039",
        help="override the port used on the host")
    parser.add_argument(
        "--user", nargs="?", default="root",
        help="user to run commands as on the device [default: root]")

    return parser.parse_args()


def dump_var(root, variable):
    make_args = ["make", "CALLED_FROM_SETUP=true",
                 "BUILD_SYSTEM={}/build/core".format(root),
                 "--no-print-directory", "-f",
                 "{}/build/core/config.mk".format(root),
                 "dumpvar-{}".format(variable)]

    # subprocess cwd argument does not change the PWD shell variable, but
    # dumpvar.mk uses PWD to create an absolute path, so we need to set it.
    saved_pwd = os.environ['PWD']
    os.environ['PWD'] = root
    make_output = subprocess.check_output(make_args, cwd=root)
    os.environ['PWD'] = saved_pwd
    return make_output.splitlines()[0]


def verify_device(root, props):
    names = set([props["ro.build.product"], props["ro.product.device"]])
    target_device = dump_var(root, "TARGET_DEVICE")
    if target_device not in names:
        msg = "TARGET_DEVICE ({}) does not match attached device ({})"
        sys.exit(msg.format(target_device, ", ".join(names)))


def get_remote_pid(device, process_name):
    processes = gdbrunner.get_processes(device)
    if process_name not in processes:
        msg = "failed to find running process {}".format(process_name)
        sys.exit(msg)
    pids = processes[process_name]
    if len(pids) > 1:
        msg = "multiple processes match '{}': {}".format(process_name, pids)
        sys.exit(msg)

    # Fetch the binary using the PID later.
    return pids[0]


def ensure_linker(device, sysroot, is64bit):
    local_path = os.path.join(sysroot, "system", "bin", "linker")
    remote_path = "/system/bin/linker"
    if is64bit:
        local_path += "64"
        remote_path += "64"
    if not os.path.exists(local_path):
        device.pull(remote_path, local_path)


def handle_switches(args, sysroot):
    """Fetch the targeted binary and determine how to attach gdb.

    Args:
        args: Parsed arguments.
        sysroot: Local sysroot path.

    Returns:
        (binary_file, attach_pid, run_cmd).
        Precisely one of attach_pid or run_cmd will be None.
    """

    device = args.device
    binary_file = None
    pid = None
    run_cmd = None

    args.su_cmd = ["su", args.user] if args.user else []

    if args.target_pid:
        # Fetch the binary using the PID later.
        pid = args.target_pid
    elif args.target_name:
        # Fetch the binary using the PID later.
        pid = get_remote_pid(device, args.target_name)
    elif args.run_cmd:
        if not args.run_cmd[0]:
            sys.exit("empty command passed to -r")
        if not args.run_cmd[0].startswith("/"):
            sys.exit("commands passed to -r must use absolute paths")
        run_cmd = args.run_cmd
        binary_file, local = gdbrunner.find_file(device, run_cmd[0], sysroot,
                                                 run_as_cmd=args.su_cmd)
    if binary_file is None:
        assert pid is not None
        try:
            binary_file, local = gdbrunner.find_binary(device, pid, sysroot,
                                                       run_as_cmd=args.su_cmd)
        except adb.ShellError:
            sys.exit("failed to pull binary for PID {}".format(pid))

    if not local:
        logging.warning("Couldn't find local unstripped executable in {},"
                        " symbols may not be available.".format(sysroot))

    return (binary_file, pid, run_cmd)

def generate_gdb_script(sysroot, binary_file, is64bit, port, connect_timeout=5):
    # Generate a gdb script.
    # TODO: Detect the zygote and run 'art-on' automatically.
    root = os.environ["ANDROID_BUILD_TOP"]
    symbols_dir = os.path.join(sysroot, "system", "lib64" if is64bit else "lib")
    vendor_dir = os.path.join(sysroot, "vendor", "lib64" if is64bit else "lib")

    solib_search_path = []
    symbols_paths = ["", "hw", "ssl/engines", "drm", "egl", "soundfx"]
    vendor_paths = ["", "hw", "egl"]
    solib_search_path += [os.path.join(symbols_dir, x) for x in symbols_paths]
    solib_search_path += [os.path.join(vendor_dir, x) for x in vendor_paths]
    solib_search_path = ":".join(solib_search_path)

    gdb_commands = ""
    gdb_commands += "file '{}'\n".format(binary_file.name)
    gdb_commands += "directory '{}'\n".format(root)
    gdb_commands += "set solib-absolute-prefix {}\n".format(sysroot)
    gdb_commands += "set solib-search-path {}\n".format(solib_search_path)

    dalvik_gdb_script = os.path.join(root, "development", "scripts", "gdb",
                                     "dalvik.gdb")
    if not os.path.exists(dalvik_gdb_script):
        logging.warning(("couldn't find {} - ART debugging options will not " +
                         "be available").format(dalvik_gdb_script))
    else:
        gdb_commands += "source {}\n".format(dalvik_gdb_script)

    # Try to connect for a few seconds, sometimes the device gdbserver takes
    # a little bit to come up, especially on emulators.
    gdb_commands += """
python

def target_remote_with_retry(target, timeout_seconds):
  import time
  end_time = time.time() + timeout_seconds
  while True:
    try:
      gdb.execute("target remote " + target)
      return True
    except gdb.error as e:
      time_left = end_time - time.time()
      if time_left < 0 or time_left > timeout_seconds:
        print("Error: unable to connect to device.")
        print(e)
        return False
      time.sleep(min(0.25, time_left))

target_remote_with_retry(':{}', {})

end
""".format(port, connect_timeout)

    return gdb_commands


def main():
    args = parse_args()
    device = args.device

    if device is None:
        sys.exit("ERROR: Failed to find device.")

    props = device.get_props()

    root = os.environ["ANDROID_BUILD_TOP"]
    sysroot = dump_var(root, "abs-TARGET_OUT_UNSTRIPPED")

    # Make sure the environment matches the attached device.
    verify_device(root, props)

    debug_socket = "/data/local/tmp/debug_socket"
    pid = None
    run_cmd = None

    # Fetch binary for -p, -n.
    binary_file, pid, run_cmd = handle_switches(args, sysroot)

    with binary_file:
        arch = gdbrunner.get_binary_arch(binary_file)
        is64bit = arch.endswith("64")

        # Make sure we have the linker
        ensure_linker(device, sysroot, is64bit)

        # Start gdbserver.
        gdbserver_local_path = get_gdbserver_path(root, arch)
        gdbserver_remote_path = "/data/local/tmp/{}-gdbserver".format(arch)
        gdbrunner.start_gdbserver(
            device, gdbserver_local_path, gdbserver_remote_path,
            target_pid=pid, run_cmd=run_cmd, debug_socket=debug_socket,
            port=args.port, run_as_cmd=args.su_cmd)

        # Generate a gdb script.
        gdb_commands = generate_gdb_script(sysroot=sysroot,
                                           binary_file=binary_file,
                                           is64bit=is64bit,
                                           port=args.port)

        # Find where gdb is
        if sys.platform.startswith("linux"):
            platform_name = "linux-x86"
        elif sys.platform.startswith("darwin"):
            platform_name = "darwin-x86"
        else:
            sys.exit("Unknown platform: {}".format(sys.platform))
        gdb_path = os.path.join(root, "prebuilts", "gdb", platform_name, "bin",
                                "gdb")

        # Print a newline to separate our messages from the GDB session.
        print("")

        # Start gdb.
        gdbrunner.start_gdb(gdb_path, gdb_commands)

if __name__ == "__main__":
    main()
