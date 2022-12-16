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
from __future__ import annotations

import atexit
import base64
import logging
import os
import re
import subprocess
from typing import Any, Callable


class FindDeviceError(RuntimeError):
    pass


class DeviceNotFoundError(FindDeviceError):
    def __init__(self, serial: str) -> None:
        self.serial = serial
        super(DeviceNotFoundError, self).__init__(
            'No device with serial {}'.format(serial))


class NoUniqueDeviceError(FindDeviceError):
    def __init__(self) -> None:
        super(NoUniqueDeviceError, self).__init__('No unique device')


class ShellError(RuntimeError):
    def __init__(
        self, cmd: list[str], stdout: str, stderr: str, exit_code: int
    ) -> None:
        super(ShellError, self).__init__(
            '`{0}` exited with code {1}'.format(cmd, exit_code))
        self.cmd = cmd
        self.stdout = stdout
        self.stderr = stderr
        self.exit_code = exit_code


def get_devices(adb_path: str = 'adb') -> list[str]:
    with open(os.devnull, 'wb') as devnull:
        subprocess.check_call([adb_path, 'start-server'], stdout=devnull,
                              stderr=devnull)
    out = split_lines(
        subprocess.check_output([adb_path, 'devices']).decode('utf-8'))

    # The first line of `adb devices` just says "List of attached devices", so
    # skip that.
    devices = []
    for line in out[1:]:
        if not line.strip():
            continue
        if 'offline' in line:
            continue

        serial, _ = re.split(r'\s+', line, maxsplit=1)
        devices.append(serial)
    return devices


def _get_unique_device(
    product: str | None = None, adb_path: str = 'adb'
) -> AndroidDevice:
    devices = get_devices(adb_path=adb_path)
    if len(devices) != 1:
        raise NoUniqueDeviceError()
    return AndroidDevice(devices[0], product, adb_path)


def _get_device_by_serial(
    serial: str, product: str | None = None, adb_path: str = 'adb'
) -> AndroidDevice:
    for device in get_devices(adb_path=adb_path):
        if device == serial:
            return AndroidDevice(serial, product, adb_path)
    raise DeviceNotFoundError(serial)


def get_device(
    serial: str | None = None, product: str | None = None, adb_path: str = 'adb'
) -> AndroidDevice:
    """Get a uniquely identified AndroidDevice if one is available.

    Raises:
        DeviceNotFoundError:
            The serial specified by `serial` or $ANDROID_SERIAL is not
            connected.

        NoUniqueDeviceError:
            Neither `serial` nor $ANDROID_SERIAL was set, and the number of
            devices connected to the system is not 1. Having 0 connected
            devices will also result in this error.

    Returns:
        An AndroidDevice associated with the first non-None identifier in the
        following order of preference:

        1) The `serial` argument.
        2) The environment variable $ANDROID_SERIAL.
        3) The single device connnected to the system.
    """
    if serial is not None:
        return _get_device_by_serial(serial, product, adb_path)

    android_serial = os.getenv('ANDROID_SERIAL')
    if android_serial is not None:
        return _get_device_by_serial(android_serial, product, adb_path)

    return _get_unique_device(product, adb_path=adb_path)


def _get_device_by_type(flag: str, adb_path: str) -> AndroidDevice:
    with open(os.devnull, 'wb') as devnull:
        subprocess.check_call([adb_path, 'start-server'], stdout=devnull,
                              stderr=devnull)
    try:
        serial = subprocess.check_output(
            [adb_path, flag, 'get-serialno']).decode('utf-8').strip()
    except subprocess.CalledProcessError:
        raise RuntimeError('adb unexpectedly returned nonzero')
    if serial == 'unknown':
        raise NoUniqueDeviceError()
    return _get_device_by_serial(serial, adb_path=adb_path)


def get_usb_device(adb_path: str = 'adb') -> AndroidDevice:
    """Get the unique USB-connected AndroidDevice if it is available.

    Raises:
        NoUniqueDeviceError:
            0 or multiple devices are connected via USB.

    Returns:
        An AndroidDevice associated with the unique USB-connected device.
    """
    return _get_device_by_type('-d', adb_path=adb_path)


def get_emulator_device(adb_path: str = 'adb') -> AndroidDevice:
    """Get the unique emulator AndroidDevice if it is available.

    Raises:
        NoUniqueDeviceError:
            0 or multiple emulators are running.

    Returns:
        An AndroidDevice associated with the unique running emulator.
    """
    return _get_device_by_type('-e', adb_path=adb_path)


def split_lines(s: str) -> list[str]:
    """Splits lines in a way that works even on Windows and old devices.

    Windows will see \r\n instead of \n, old devices do the same, old devices
    on Windows will see \r\r\n.
    """
    # rstrip is used here to workaround a difference between splitlines and
    # re.split:
    # >>> 'foo\n'.splitlines()
    # ['foo']
    # >>> re.split(r'\n', 'foo\n')
    # ['foo', '']
    return re.split(r'[\r\n]+', s.rstrip())


def version(adb_path: list[str] | None = None) -> int:
    """Get the version of adb (in terms of ADB_SERVER_VERSION)."""

    adb_path = adb_path if adb_path is not None else ['adb']
    version_output = subprocess.check_output(adb_path + ['version'], encoding='utf-8')
    pattern = r'^Android Debug Bridge version 1.0.(\d+)$'
    result = re.match(pattern, version_output.splitlines()[0])
    if not result:
        return 0
    return int(result.group(1))


class AndroidDevice(object):
    # Delimiter string to indicate the start of the exit code.
    _RETURN_CODE_DELIMITER = 'x'

    # Follow any shell command with this string to get the exit
    # status of a program since this isn't propagated by adb.
    #
    # The delimiter is needed because `printf 1; echo $?` would print
    # "10", and we wouldn't be able to distinguish the exit code.
    _RETURN_CODE_PROBE = [';', 'echo', '{0}$?'.format(_RETURN_CODE_DELIMITER)]

    # Maximum search distance from the output end to find the delimiter.
    # adb on Windows returns \r\n even if adbd returns \n. Some old devices
    # seem to actually return \r\r\n.
    _RETURN_CODE_SEARCH_LENGTH = len(
        '{0}255\r\r\n'.format(_RETURN_CODE_DELIMITER))

    def __init__(
        self, serial: str | None, product: str | None = None, adb_path: str = 'adb'
    ) -> None:
        self.serial = serial
        self.product = product
        self.adb_path = adb_path
        self.adb_cmd = [adb_path]

        if self.serial is not None:
            self.adb_cmd.extend(['-s', self.serial])
        if self.product is not None:
            self.adb_cmd.extend(['-p', self.product])
        self._linesep: str | None = None
        self._features: list[str] | None = None

    @property
    def linesep(self) -> str:
        if self._linesep is None:
            self._linesep = subprocess.check_output(
                self.adb_cmd + ['shell', 'echo'], encoding='utf-8')
        return self._linesep

    @property
    def features(self) -> list[str]:
        if self._features is None:
            try:
                self._features = split_lines(self._simple_call(['features']))
            except subprocess.CalledProcessError:
                self._features = []
        return self._features

    def has_shell_protocol(self) -> bool:
        return version(self.adb_cmd) >= 35 and 'shell_v2' in self.features

    def _make_shell_cmd(self, user_cmd: list[str]) -> list[str]:
        command = self.adb_cmd + ['shell'] + user_cmd
        if not self.has_shell_protocol():
            command += self._RETURN_CODE_PROBE
        return command

    def _parse_shell_output(self, out: str) -> tuple[int, str]:
        """Finds the exit code string from shell output.

        Args:
            out: Shell output string.

        Returns:
            An (exit_code, output_string) tuple. The output string is
            cleaned of any additional stuff we appended to find the
            exit code.

        Raises:
            RuntimeError: Could not find the exit code in |out|.
        """
        search_text = out
        if len(search_text) > self._RETURN_CODE_SEARCH_LENGTH:
            # We don't want to search over massive amounts of data when we know
            # the part we want is right at the end.
            search_text = search_text[-self._RETURN_CODE_SEARCH_LENGTH:]
        partition = search_text.rpartition(self._RETURN_CODE_DELIMITER)
        if partition[1] == '':
            raise RuntimeError('Could not find exit status in shell output.')
        result = int(partition[2])
        # partition[0] won't contain the full text if search_text was
        # truncated, pull from the original string instead.
        out = out[:-len(partition[1]) - len(partition[2])]
        return result, out

    def _simple_call(self, cmd: list[str]) -> str:
        logging.info(' '.join(self.adb_cmd + cmd))
        return subprocess.check_output(
            self.adb_cmd + cmd, stderr=subprocess.STDOUT).decode('utf-8')

    def shell(self, cmd: list[str]) -> tuple[str, str]:
        """Calls `adb shell`

        Args:
            cmd: command to execute as a list of strings.

        Returns:
            A (stdout, stderr) tuple. Stderr may be combined into stdout
            if the device doesn't support separate streams.

        Raises:
            ShellError: the exit code was non-zero.
        """
        exit_code, stdout, stderr = self.shell_nocheck(cmd)
        if exit_code != 0:
            raise ShellError(cmd, stdout, stderr, exit_code)
        return stdout, stderr

    def shell_nocheck(self, cmd: list[str]) -> tuple[int, str, str]:
        """Calls `adb shell`

        Args:
            cmd: command to execute as a list of strings.

        Returns:
            An (exit_code, stdout, stderr) tuple. Stderr may be combined
            into stdout if the device doesn't support separate streams.
        """
        cmd = self._make_shell_cmd(cmd)
        logging.info(' '.join(cmd))
        p = subprocess.Popen(
            cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, encoding='utf-8')
        stdout, stderr = p.communicate()
        if self.has_shell_protocol():
            exit_code = p.returncode
        else:
            exit_code, stdout = self._parse_shell_output(stdout)
        return exit_code, stdout, stderr

    def shell_popen(
        self,
        cmd: list[str],
        kill_atexit: bool = True,
        preexec_fn: Callable[[], None] | None = None,
        creationflags: int = 0,
        **kwargs: Any,
    ) -> subprocess.Popen[Any]:
        """Calls `adb shell` and returns a handle to the adb process.

        This function provides direct access to the subprocess used to run the
        command, without special return code handling. Users that need the
        return value must retrieve it themselves.

        Args:
            cmd: Array of command arguments to execute.
            kill_atexit: Whether to kill the process upon exiting.
            preexec_fn: Argument forwarded to subprocess.Popen.
            creationflags: Argument forwarded to subprocess.Popen.
            **kwargs: Arguments forwarded to subprocess.Popen.

        Returns:
            subprocess.Popen handle to the adb shell instance
        """

        command = self.adb_cmd + ['shell'] + cmd

        # Make sure a ctrl-c in the parent script doesn't kill gdbserver.
        if os.name == 'nt':
            creationflags |= subprocess.CREATE_NEW_PROCESS_GROUP
        else:
            if preexec_fn is None:
                preexec_fn = os.setpgrp
            elif preexec_fn is not os.setpgrp:
                fn = preexec_fn
                def _wrapper() -> None:
                    fn()
                    os.setpgrp()
                preexec_fn = _wrapper

        p = subprocess.Popen(command, creationflags=creationflags,
                             preexec_fn=preexec_fn, **kwargs)

        if kill_atexit:
            atexit.register(p.kill)

        return p

    def install(self, filename: str, replace: bool = False) -> str:
        cmd = ['install']
        if replace:
            cmd.append('-r')
        cmd.append(filename)
        return self._simple_call(cmd)

    def push(self, local: str | list[str], remote: str, sync: bool = False) -> str:
        """Transfer a local file or directory to the device.

        Args:
            local: The local file or directory to transfer.
            remote: The remote path to which local should be transferred.
            sync: If True, only transfers files that are newer on the host than
                  those on the device. If False, transfers all files.

        Returns:
            Output of the command.
        """
        cmd = ['push']
        if sync:
            cmd.append('--sync')

        if isinstance(local, str):
            cmd.extend([local, remote])
        else:
            cmd.extend(local)
            cmd.append(remote)

        return self._simple_call(cmd)

    def pull(self, remote: str, local: str) -> str:
        return self._simple_call(['pull', remote, local])

    def sync(self, directory: str | None = None) -> str:
        cmd = ['sync']
        if directory is not None:
            cmd.append(directory)
        return self._simple_call(cmd)

    def tcpip(self, port: str) -> str:
        return self._simple_call(['tcpip', port])

    def usb(self) -> str:
        return self._simple_call(['usb'])

    def reboot(self) -> str:
        return self._simple_call(['reboot'])

    def remount(self) -> str:
        return self._simple_call(['remount'])

    def root(self) -> str:
        return self._simple_call(['root'])

    def unroot(self) -> str:
        return self._simple_call(['unroot'])

    def connect(self, host: str) -> str:
        return self._simple_call(['connect', host])

    def disconnect(self, host: str) -> str:
        return self._simple_call(['disconnect', host])

    def forward(self, local: str, remote: str) -> str:
        return self._simple_call(['forward', local, remote])

    def forward_list(self) -> str:
        return self._simple_call(['forward', '--list'])

    def forward_no_rebind(self, local: str, remote: str) -> str:
        return self._simple_call(['forward', '--no-rebind', local, remote])

    def forward_remove(self, local: str) -> str:
        return self._simple_call(['forward', '--remove', local])

    def forward_remove_all(self) -> str:
        return self._simple_call(['forward', '--remove-all'])

    def reverse(self, remote: str, local: str) -> str:
        return self._simple_call(['reverse', remote, local])

    def reverse_list(self) -> str:
        return self._simple_call(['reverse', '--list'])

    def reverse_no_rebind(self, local: str, remote: str) -> str:
        return self._simple_call(['reverse', '--no-rebind', local, remote])

    def reverse_remove_all(self) -> str:
        return self._simple_call(['reverse', '--remove-all'])

    def reverse_remove(self, remote: str) -> str:
        return self._simple_call(['reverse', '--remove', remote])

    def wait(self) -> str:
        return self._simple_call(['wait-for-device'])

    def get_prop(self, prop_name: str) -> str | None:
        output = split_lines(self.shell(['getprop', prop_name])[0])
        if len(output) != 1:
            raise RuntimeError('Too many lines in getprop output:\n' +
                               '\n'.join(output))
        value = output[0]
        if not value.strip():
            return None
        return value

    def set_prop(self, prop_name: str, value: str) -> None:
        self.shell(['setprop', prop_name, value])

    def logcat(self) -> str:
        """Returns the contents of logcat."""
        return self._simple_call(['logcat', '-d'])

    def clear_logcat(self) -> None:
        """Clears the logcat buffer."""
        self._simple_call(['logcat', '-c'])
