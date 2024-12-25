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

import argparse
import json
import logging
import os
import pathlib
import posixpath
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
from typing import Any, BinaryIO

import adb
# Shared functions across gdbclient.py and ndk-gdb.py.
import gdbrunner

g_temp_dirs = []

g_vscode_config_marker_begin = '// #lldbclient-generated-begin'
g_vscode_config_marker_end = '// #lldbclient-generated-end'


def read_toolchain_config(root: str) -> str:
    """Finds out current toolchain version."""
    version_output = subprocess.check_output(
        f'{root}/build/soong/scripts/get_clang_version.py',
        text=True)
    return version_output.strip()


def get_lldb_path(toolchain_path: str) -> str | None:
    for lldb_name in ['lldb.sh', 'lldb.cmd', 'lldb', 'lldb.exe']:
        debugger_path = os.path.join(toolchain_path, "bin", lldb_name)
        if os.path.isfile(debugger_path):
            return debugger_path
    return None


def get_lldb_server_path(root: str, clang_base: str, clang_version: str, arch: str) -> str:
    arch = {
        'arm': 'arm',
        'arm64': 'aarch64',
        'riscv64': 'riscv64',
        'x86': 'i386',
        'x86_64': 'x86_64',
    }[arch]
    return os.path.join(root, clang_base, "linux-x86",
                        clang_version, "runtimes_ndk_cxx", arch, "lldb-server")


def get_tracer_pid(device: adb.AndroidDevice, pid: int | str | None) -> int:
    if pid is None:
        return 0

    line, _ = device.shell(["grep", "-e", "^TracerPid:", "/proc/{}/status".format(pid)])
    tracer_pid = re.sub('TracerPid:\t(.*)\n', r'\1', line)
    return int(tracer_pid)


def parse_args() -> argparse.Namespace:
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
        help="Unused **host** port to forward the debug_socket to.[default: 5039]")
    parser.add_argument(
        "--user", nargs="?", default="root",
        help="user to run commands as on the device [default: root]")
    parser.add_argument(
        "--setup-forwarding", default=None,
        choices=["lldb", "vscode-lldb"],
        help=("Set up lldb-server and port forwarding. Prints commands or " +
              ".vscode/launch.json configuration needed to connect the debugging " +
              "client to the server. 'vscode' with lldb and 'vscode-lldb' both " +
              "require the 'vadimcn.vscode-lldb' extension."))
    parser.add_argument(
        "--vscode-launch-props", default=None,
        dest="vscode_launch_props",
        help=("JSON with extra properties to add to launch parameters when using " +
              "vscode-lldb forwarding."))
    parser.add_argument(
        "--vscode-launch-file", default=None,
        dest="vscode_launch_file",
        help=textwrap.dedent(f"""Path to .vscode/launch.json file for the generated launch
                     config when using vscode-lldb forwarding. The file needs to
                     contain two marker lines: '{g_vscode_config_marker_begin}'
                     and '{g_vscode_config_marker_end}'. The config will be written inline
                     between these lines, replacing any text that is already there."""))

    parser.add_argument(
        "--extra-cmds-file", default=None,
        help=f"""Path to file with extra commands to run at lldb startup/attachment.
                 One command per line. Commands prefixed with '#' will be ignored"""
    )

    parser.add_argument(
        "--env", nargs=1, action="append", metavar="VAR=VALUE",
        help="set environment variable when running a binary")
    parser.add_argument(
        "--chroot", nargs='?', default="", metavar="PATH",
        help="run command in a chroot in the given directory. Cannot be used with --cwd.")
    parser.add_argument(
        "--cwd", nargs='?', default="", metavar="PATH",
        help="working directory for the command. Cannot be used with --chroot.")

    return parser.parse_args()


def verify_device(device: adb.AndroidDevice) -> None:
    names = set([device.get_prop("ro.build.product"), device.get_prop("ro.product.name")])
    target_device = os.environ["TARGET_PRODUCT"]
    if target_device not in names:
        msg = "You used the wrong lunch: TARGET_PRODUCT ({}) does not match attached device ({})"
        sys.exit(msg.format(target_device, ", ".join(n if n else "None" for n in names)))


def get_device_dir_exists(device: adb.AndroidDevice, dir: str) -> bool:
    exit_code, _, _ = device.shell_nocheck(['[', '-d', dir, ']'])
    return exit_code == 0


def get_remote_pid(device: adb.AndroidDevice, process_name: str) -> int:
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


def make_temp_dir(prefix: str) -> str:
    global g_temp_dirs
    result = tempfile.mkdtemp(prefix='lldbclient-linker-')
    g_temp_dirs.append(result)
    return result


def ensure_linker(device: adb.AndroidDevice, sysroot: str, interp: str | None) -> str | None:
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


def handle_switches(args, sysroot: str) -> tuple[BinaryIO, int | None, str | None]:
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

def merge_launch_dict(base: dict[str, Any], to_add:  dict[str, Any] | None) -> None:
    """Merges two dicts describing VSCode launch.json properties: base and
    to_add. Base is modified in-place with items from to_add.
    Items from to_add that are not present in base are inserted. Items that are
    present are merged following these rules:
        - Lists are merged with to_add elements appended to the end of base
          list. Only a list can be merged with a list.
        - dicts are merged recursively. Only a dict can be merged with a dict.
        - Other present values in base get overwritten with values from to_add.

    The reason for these rules is that merging in new values should prefer to
    expand the existing set instead of overwriting where possible.
    """
    if to_add is None:
        return

    for key, val in to_add.items():
        if key not in base:
            base[key] = val
        else:
            if isinstance(base[key], list) and not isinstance(val, list):
                raise ValueError(f'Cannot merge non-list into list at key={key}. ' +
                'You probably need to wrap your value into a list.')
            if not isinstance(base[key], list) and isinstance(val, list):
                raise ValueError(f'Cannot merge list into non-list at key={key}.')
            if isinstance(base[key], dict) != isinstance(val, dict):
                raise ValueError(f'Cannot merge dict and non-dict at key={key}')

            # We don't allow the user to overwrite or interleave lists and don't allow
            # to delete dict entries.
            # It can be done but would make the implementation a bit more complicated
            # and provides less value than adding elements.
            # We expect that the config generated by gdbclient doesn't contain anything
            # the user would want to remove.
            if isinstance(base[key], list):
                base[key] += val
            elif isinstance(base[key], dict):
                merge_launch_dict(base[key], val)
            else:
                base[key] = val


def generate_vscode_lldb_script(root: str, sysroot: str, binary_name: str, port: str | int, solib_search_path: list[str], extra_props: dict[str, Any] | None) -> str:
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
        "processCreateCommands": ["gdb-remote {}".format(str(port))]
    }
    merge_launch_dict(res, extra_props)
    return json.dumps(res, indent=4)

def generate_lldb_script(root: str, sysroot: str, binary_name: str, port: str | int, solib_search_path: list[str], extra_cmds: list[str]) -> str:
    commands = []
    commands.append(
        'settings append target.exec-search-paths {}'.format(' '.join(solib_search_path)))

    commands.append('target create {}'.format(binary_name))
    # For RBE support.
    commands.append("settings append target.source-map '/b/f/w' '{}'".format(root))
    commands.append("settings append target.source-map '' '{}'".format(root))
    commands.append('target modules search-paths add / {}/'.format(sysroot))
    commands.append('# If the below `gdb-remote` fails, run the command manually, '
                    + 'as it may have raced with lldbserver startup.')
    commands.append('gdb-remote {}'.format(str(port)))
    for cmd in extra_cmds:
        if not cmd.startswith('#'):
            commands.append(cmd)
    return '\n'.join(commands)


def generate_setup_script(sysroot: str, linker_search_dir: str | None, binary_name: str, is64bit: bool, port: str | int, debugger: str, vscode_launch_props: dict[str, Any] | None, extra_cmds : list[str] | None) -> str:
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
            root, sysroot, binary_name, port, solib_search_path, vscode_launch_props)
    elif debugger == 'lldb':
        return generate_lldb_script(
            root, sysroot, binary_name, port, solib_search_path, extra_cmds)
    else:
        raise Exception("Unknown debugger type " + debugger)


def insert_commands_into_vscode_config(dst_launch_config: str, setup_commands: str) -> str:
    """Inserts setup commands into launch config between two marker lines.
    Marker lines are set in global variables g_vscode_config_marker_end and g_vscode_config_marker_end.
    The commands are inserted with the same indentation as the first marker line.

    Args:
        dst_launch_config: Config to insert commands into.
        setup_commands: Commands to insert.
    Returns:
        Config with inserted commands.
    Raises:
        ValueError if the begin marker is not found or not terminated with an end marker.
    """

    # We expect the files to be small (~10s KB), so we use simple string concatenation
    # for simplicity and readability even if it is slower.
    output = ""
    found_at_least_one_begin = False
    unterminated_begin_line = None

    # It might be tempting to rewrite this using find() or even regexes,
    # but keeping track of line numbers, preserving whitespace, and detecting indent
    # becomes tricky enough that this simple loop is more clear.
    for linenum, line in enumerate(dst_launch_config.splitlines(keepends=True), start=1):
       if unterminated_begin_line != None:
           if line.strip() == g_vscode_config_marker_end:
               unterminated_begin_line = None
           else:
               continue
       output += line
       if line.strip() == g_vscode_config_marker_begin:
           found_at_least_one_begin = True
           unterminated_begin_line = linenum
           marker_indent = line[:line.find(g_vscode_config_marker_begin)]
           output += textwrap.indent(setup_commands, marker_indent) + '\n'

    if not found_at_least_one_begin:
       raise ValueError(f"Did not find begin marker line '{g_vscode_config_marker_begin}' " +
                        "in the VSCode launch file")

    if unterminated_begin_line is not None:
       raise ValueError(f"Unterminated begin marker at line {unterminated_begin_line} " +
                        f"in the VSCode launch file. Add end marker line to file: '{g_vscode_config_marker_end}'")

    return output


def replace_file_contents(dst_path: os.PathLike, contents: str) -> None:
    """Replaces the contents of the file pointed to by dst_path.

    This function writes the new contents into a temporary file, then atomically swaps it with
    the target file. This way if a write fails, the original file is not overwritten.

    Args:
        dst_path: The path to the file to be replaced.
        contents: The new contents of the file.
    Raises:
        Forwards exceptions from underlying filesystem methods.
    """
    tempf = tempfile.NamedTemporaryFile('w', delete=False)
    try:
        tempf.write(contents)
        os.replace(tempf.name, dst_path)
    except:
        os.remove(tempf.name)
        raise


def write_vscode_config(vscode_launch_file: pathlib.Path, setup_commands: str) -> None:
    """Writes setup_commands into the file pointed by vscode_launch_file.

    See insert_commands_into_vscode_config for the description of how the setup commands are written.
    """
    contents = insert_commands_into_vscode_config(vscode_launch_file.read_text(), setup_commands)
    replace_file_contents(vscode_launch_file, contents)


def do_main() -> None:
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

    if args.cwd:
        if not get_device_dir_exists(device, args.cwd):
            raise ValueError('Working directory does not exist on device: {}'.format(args.cwd))
        if args.chroot:
            # See the comment in start_gdbserver about why this is not implemented.
            raise ValueError('--cwd and --chroot cannot be used together')

    # Make sure the environment matches the attached device.
    # Skip when running in a chroot because the chroot lunch target may not
    # match the device's lunch target.
    if not args.chroot:
        verify_device(device)

    debug_socket = "/data/local/tmp/debug_socket"
    pid = None
    run_cmd = None

    # Fetch binary for -p, -n.
    binary_file, pid, run_cmd = handle_switches(args, sysroot)

    vscode_launch_props = None
    if args.vscode_launch_props:
        if args.setup_forwarding != "vscode-lldb":
            raise ValueError(
                'vscode-launch-props requires --setup-forwarding=vscode-lldb')
        vscode_launch_props = json.loads(args.vscode_launch_props)

    vscode_launch_file = None
    if args.vscode_launch_file:
        if args.setup_forwarding != "vscode-lldb":
            raise ValueError(
                'vscode-launch-file requires --setup-forwarding=vscode-lldb')
        vscode_launch_file = args.vscode_launch_file

    extra_cmds = None
    if args.extra_cmds_file:
        if not os.path.exists(args.extra_cmds_file):
            raise ValueError(f'--extra-cmds-file {args.extra_cmds_file}: file does not exist')
        with open(args.extra_cmds_file, 'rt') as cmds_file:
            extra_cmds = [l.strip() for l in cmds_file.readlines()]

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
                port=args.port, run_as_cmd=cmd_prefix, lldb=True, chroot=args.chroot, cwd=args.cwd)
        else:
            print(
                "Connecting to tracing pid {} using local port {}".format(
                    tracer_pid, args.port))
            gdbrunner.forward_gdbserver_port(device, local=args.port,
                                             remote="tcp:{}".format(args.port))

        debugger_path = get_lldb_path(toolchain_path)
        debugger = args.setup_forwarding or 'lldb'

        # Generate the lldb script.
        setup_commands = generate_setup_script(sysroot=sysroot,
                                               linker_search_dir=linker_search_dir,
                                               binary_name=binary_file.name,
                                               is64bit=is64bit,
                                               port=args.port,
                                               debugger=debugger,
                                               vscode_launch_props=vscode_launch_props,
                                               extra_cmds=extra_cmds)

        if not args.setup_forwarding:
            # Print a newline to separate our messages from the GDB session.
            print("")

            # Start lldb.
            gdbrunner.start_gdb(debugger_path, setup_commands, lldb=True)
        else:
            if args.setup_forwarding == "vscode-lldb" and vscode_launch_file:
                write_vscode_config(pathlib.Path(vscode_launch_file) , setup_commands)
                print(f"Generated config written to '{vscode_launch_file}'")
            else:
                print("")
                print(setup_commands)
                print("")
                if args.setup_forwarding == "vscode-lldb":
                    print(textwrap.dedent("""
                            Paste the above json into .vscode/launch.json and start the debugger as
                            normal."""))
                else:
                    print(textwrap.dedent("""
                            Paste the lldb commands above into the lldb frontend to set up the
                            lldb-server connection."""))

            print(textwrap.dedent("""
                        Press enter in this terminal once debugging is finished to shut lldb-server
                        down and close all the ports."""))
            print("")
            input("Press enter to shut down lldb-server")


def main():
    try:
        do_main()
    finally:
        global g_temp_dirs
        for temp_dir in g_temp_dirs:
            shutil.rmtree(temp_dir)


if __name__ == "__main__":
    main()
