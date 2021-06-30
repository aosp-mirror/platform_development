#!/usr/bin/env python3
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
import json
import logging
import os
import posixpath
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap

# Shared functions across gdbclient.py and ndk-gdb.py.
import gdbrunner

g_temp_dirs = []


def read_toolchain_config(root):
    """Finds out current toolchain version."""
    version_output = subprocess.check_output(
        f'{root}/build/soong/scripts/get_clang_version.py',
        text=True)
    return version_output.strip()


def get_lldb_path(toolchain_path):
    for lldb_name in ['lldb.sh', 'lldb.cmd', 'lldb', 'lldb.exe']:
        debugger_path = os.path.join(toolchain_path, "bin", lldb_name)
        if os.path.isfile(debugger_path):
            return debugger_path
    return None


def get_lldb_server_path(root, clang_base, clang_version, arch):
    arch = {
        'arm': 'arm',
        'arm64': 'aarch64',
        'x86': 'i386',
        'x86_64': 'x86_64',
    }[arch]
    return os.path.join(root, clang_base, "linux-x86",
                        clang_version, "runtimes_ndk_cxx", arch, "lldb-server")


def get_tracer_pid(device, pid):
    if pid is None:
        return 0

    line, _ = device.shell(["grep", "-e", "^TracerPid:", "/proc/{}/status".format(pid)])
    tracer_pid = re.sub('TracerPid:\t(.*)\n', r'\1', line)
    return int(tracer_pid)


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
        help="override the port used on the host [default: 5039]")
    parser.add_argument(
        "--user", nargs="?", default="root",
        help="user to run commands as on the device [default: root]")
    parser.add_argument(
        "--setup-forwarding", default=None,
        choices=["lldb", "vscode-lldb"],
        help=("Set up lldb-server and port forwarding. Prints commands or " +
              ".vscode/launch.json configuration needed to connect the debugging " +
              "client to the server. 'vscode' with llbd and 'vscode-lldb' both " +
              "require the 'vadimcn.vscode-lldb' extension."))

    parser.add_argument(
        "--env", nargs=1, action="append", metavar="VAR=VALUE",
        help="set environment variable when running a binary")

    return parser.parse_args()


def verify_device(root, device):
    names = set([device.get_prop("ro.build.product"), device.get_prop("ro.product.name")])
    target_device = os.environ["TARGET_PRODUCT"]
    if target_device not in names:
        msg = "TARGET_PRODUCT ({}) does not match attached device ({})"
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


def make_temp_dir(prefix):
    global g_temp_dirs
    result = tempfile.mkdtemp(prefix='lldbclient-linker-')
    g_temp_dirs.append(result)
    return result


def ensure_linker(device, sysroot, interp):
    """Ensure that the device's linker exists on the host.

    PT_INTERP is usually /system/bin/linker[64], but on the device, that file is
    a symlink to /apex/com.android.runtime/bin/linker[64]. The symbolized linker
    binary on the host is located in ${sysroot}/apex, not in ${sysroot}/system,
    so add the ${sysroot}/apex path to the solib search path.

    PT_INTERP will be /system/bin/bootstrap/linker[64] for executables using the
    non-APEX/bootstrap linker. No search path modification is needed.

    For a tapas build, only an unbundled app is built, and there is no linker in
    ${sysroot} at all, so copy the linker from the device.

    Returns:
        A directory to add to the soinfo search path or None if no directory
        needs to be added.
    """

    # Static executables have no interpreter.
    if interp is None:
        return None

    # lldb will search for the linker using the PT_INTERP path. First try to find
    # it in the sysroot.
    local_path = os.path.join(sysroot, interp.lstrip("/"))
    if os.path.exists(local_path):
        return None

    # If the linker on the device is a symlink, search for the symlink's target
    # in the sysroot directory.
    interp_real, _ = device.shell(["realpath", interp])
    interp_real = interp_real.strip()
    local_path = os.path.join(sysroot, interp_real.lstrip("/"))
    if os.path.exists(local_path):
        if posixpath.basename(interp) == posixpath.basename(interp_real):
            # Add the interpreter's directory to the search path.
            return os.path.dirname(local_path)
        else:
            # If PT_INTERP is linker_asan[64], but the sysroot file is
            # linker[64], then copy the local file to the name lldb expects.
            result = make_temp_dir('lldbclient-linker-')
            shutil.copy(local_path, os.path.join(result, posixpath.basename(interp)))
            return result

    # Pull the system linker.
    result = make_temp_dir('lldbclient-linker-')
    device.pull(interp, os.path.join(result, posixpath.basename(interp)))
    return result


def handle_switches(args, sysroot):
    """Fetch the targeted binary and determine how to attach lldb.

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
        run_cmd = args.run_cmd
        if not run_cmd[0].startswith("/"):
            try:
                run_cmd[0] = gdbrunner.find_executable_path(device, args.run_cmd[0],
                                                            run_as_cmd=args.su_cmd)
            except RuntimeError:
              sys.exit("Could not find executable '{}' passed to -r, "
                       "please provide an absolute path.".format(args.run_cmd[0]))

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

def generate_vscode_lldb_script(root, sysroot, binary_name, port, solib_search_path):
    # TODO It would be nice if we didn't need to copy this or run the
    #      lldbclient.py program manually. Doing this would probably require
    #      writing a vscode extension or modifying an existing one.
    # TODO: https://code.visualstudio.com/api/references/vscode-api#debug and
    #       https://code.visualstudio.com/api/extension-guides/debugger-extension and
    #       https://github.com/vadimcn/vscode-lldb/blob/6b775c439992b6615e92f4938ee4e211f1b060cf/extension/pickProcess.ts#L6
    res = {
        "name": "(lldbclient.py) Attach {} (port: {})".format(binary_name.split("/")[-1], port),
        "type": "lldb",
        "request": "custom",
        "relativePathBase": root,
        "sourceMap": { "/b/f/w" : root, '': root, '.': root },
        "initCommands": ['settings append target.exec-search-paths {}'.format(' '.join(solib_search_path))],
        "targetCreateCommands": ["target create {}".format(binary_name),
                                 "target modules search-paths add / {}/".format(sysroot)],
        "processCreateCommands": ["gdb-remote {}".format(port)]
    }
    return json.dumps(res, indent=4)

def generate_lldb_script(root, sysroot, binary_name, port, solib_search_path):
    commands = []
    commands.append(
        'settings append target.exec-search-paths {}'.format(' '.join(solib_search_path)))

    commands.append('target create {}'.format(binary_name))
    # For RBE support.
    commands.append("settings append target.source-map '/b/f/w' '{}'".format(root))
    commands.append("settings append target.source-map '' '{}'".format(root))
    commands.append('target modules search-paths add / {}/'.format(sysroot))
    commands.append('gdb-remote {}'.format(port))
    return '\n'.join(commands)


def generate_setup_script(debugger_path, sysroot, linker_search_dir, binary_file, is64bit, port, debugger, connect_timeout=5):
    # Generate a setup script.
    root = os.environ["ANDROID_BUILD_TOP"]
    symbols_dir = os.path.join(sysroot, "system", "lib64" if is64bit else "lib")
    vendor_dir = os.path.join(sysroot, "vendor", "lib64" if is64bit else "lib")

    solib_search_path = []
    symbols_paths = ["", "hw", "ssl/engines", "drm", "egl", "soundfx"]
    vendor_paths = ["", "hw", "egl"]
    solib_search_path += [os.path.join(symbols_dir, x) for x in symbols_paths]
    solib_search_path += [os.path.join(vendor_dir, x) for x in vendor_paths]
    if linker_search_dir is not None:
        solib_search_path += [linker_search_dir]

    if debugger == "vscode-lldb":
        return generate_vscode_lldb_script(
            root, sysroot, binary_file.name, port, solib_search_path)
    elif debugger == 'lldb':
        return generate_lldb_script(
            root, sysroot, binary_file.name, port, solib_search_path)
    else:
        raise Exception("Unknown debugger type " + debugger)


def do_main():
    required_env = ["ANDROID_BUILD_TOP",
                    "ANDROID_PRODUCT_OUT", "TARGET_PRODUCT"]
    for env in required_env:
        if env not in os.environ:
            sys.exit(
                "Environment variable '{}' not defined, have you run lunch?".format(env))

    args = parse_args()
    device = args.device

    if device is None:
        sys.exit("ERROR: Failed to find device.")

    root = os.environ["ANDROID_BUILD_TOP"]
    sysroot = os.path.join(os.environ["ANDROID_PRODUCT_OUT"], "symbols")

    # Make sure the environment matches the attached device.
    verify_device(root, device)

    debug_socket = "/data/local/tmp/debug_socket"
    pid = None
    run_cmd = None

    # Fetch binary for -p, -n.
    binary_file, pid, run_cmd = handle_switches(args, sysroot)

    with binary_file:
        if sys.platform.startswith("linux"):
            platform_name = "linux-x86"
        elif sys.platform.startswith("darwin"):
            platform_name = "darwin-x86"
        else:
            sys.exit("Unknown platform: {}".format(sys.platform))

        arch = gdbrunner.get_binary_arch(binary_file)
        is64bit = arch.endswith("64")

        # Make sure we have the linker
        clang_base = 'prebuilts/clang/host'
        clang_version = read_toolchain_config(root)
        toolchain_path = os.path.join(root, clang_base, platform_name,
                                      clang_version)
        llvm_readobj_path = os.path.join(toolchain_path, "bin", "llvm-readobj")
        interp = gdbrunner.get_binary_interp(binary_file.name, llvm_readobj_path)
        linker_search_dir = ensure_linker(device, sysroot, interp)

        tracer_pid = get_tracer_pid(device, pid)
        if tracer_pid == 0:
            cmd_prefix = args.su_cmd
            if args.env:
                cmd_prefix += ['env'] + [v[0] for v in args.env]

            # Start lldb-server.
            server_local_path = get_lldb_server_path(root, clang_base, clang_version, arch)
            server_remote_path = "/data/local/tmp/{}-lldb-server".format(arch)
            gdbrunner.start_gdbserver(
                device, server_local_path, server_remote_path,
                target_pid=pid, run_cmd=run_cmd, debug_socket=debug_socket,
                port=args.port, run_as_cmd=cmd_prefix, lldb=True)
        else:
            print(
                "Connecting to tracing pid {} using local port {}".format(
                    tracer_pid, args.port))
            gdbrunner.forward_gdbserver_port(device, local=args.port,
                                             remote="tcp:{}".format(args.port))

        debugger_path = get_lldb_path(toolchain_path)
        debugger = args.setup_forwarding or 'lldb'

        # Generate the lldb script.
        setup_commands = generate_setup_script(debugger_path=debugger_path,
                                               sysroot=sysroot,
                                               linker_search_dir=linker_search_dir,
                                               binary_file=binary_file,
                                               is64bit=is64bit,
                                               port=args.port,
                                               debugger=debugger)

        if not args.setup_forwarding:
            # Print a newline to separate our messages from the GDB session.
            print("")

            # Start lldb.
            gdbrunner.start_gdb(debugger_path, setup_commands, lldb=True)
        else:
            print("")
            print(setup_commands)
            print("")
            if args.setup_forwarding == "vscode-lldb":
                print(textwrap.dedent("""
                        Paste the above json into .vscode/launch.json and start the debugger as
                        normal. Press enter in this terminal once debugging is finished to shut
                        lldb-server down and close all the ports."""))
            else:
                print(textwrap.dedent("""
                        Paste the lldb commands above into the lldb frontend to set up the
                        lldb-server connection. Press enter in this terminal once debugging is
                        finished to shut lldb-server down and close all the ports."""))
            print("")
            raw_input("Press enter to shut down lldb-server")


def main():
    try:
        do_main()
    finally:
        global g_temp_dirs
        for temp_dir in g_temp_dirs:
            shutil.rmtree(temp_dir)


if __name__ == "__main__":
    main()
