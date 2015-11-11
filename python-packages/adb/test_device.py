#!/usr/bin/env python
# -*- coding: utf-8 -*-
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
from __future__ import print_function

import contextlib
import hashlib
import os
import posixpath
import random
import re
import shlex
import shutil
import signal
import socket
import string
import subprocess
import sys
import tempfile
import unittest

import mock

import adb


def requires_root(func):
    def wrapper(self, *args):
        if self.device.get_prop('ro.debuggable') != '1':
            raise unittest.SkipTest('requires rootable build')

        was_root = self.device.shell(['id', '-un'])[0].strip() == 'root'
        if not was_root:
            self.device.root()
            self.device.wait()

        try:
            func(self, *args)
        finally:
            if not was_root:
                self.device.unroot()
                self.device.wait()

    return wrapper


def requires_non_root(func):
    def wrapper(self, *args):
        was_root = self.device.shell(['id', '-un'])[0].strip() == 'root'
        if was_root:
            self.device.unroot()
            self.device.wait()

        try:
            func(self, *args)
        finally:
            if was_root:
                self.device.root()
                self.device.wait()

    return wrapper


class GetDeviceTest(unittest.TestCase):
    def setUp(self):
        self.android_serial = os.getenv('ANDROID_SERIAL')
        if 'ANDROID_SERIAL' in os.environ:
            del os.environ['ANDROID_SERIAL']

    def tearDown(self):
        if self.android_serial is not None:
            os.environ['ANDROID_SERIAL'] = self.android_serial
        else:
            if 'ANDROID_SERIAL' in os.environ:
                del os.environ['ANDROID_SERIAL']

    @mock.patch('adb.device.get_devices')
    def test_explicit(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        device = adb.get_device('foo')
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_from_env(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        os.environ['ANDROID_SERIAL'] = 'foo'
        device = adb.get_device()
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_arg_beats_env(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        os.environ['ANDROID_SERIAL'] = 'bar'
        device = adb.get_device('foo')
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_no_such_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        self.assertRaises(adb.DeviceNotFoundError, adb.get_device, ['baz'])

        os.environ['ANDROID_SERIAL'] = 'baz'
        self.assertRaises(adb.DeviceNotFoundError, adb.get_device)

    @mock.patch('adb.device.get_devices')
    def test_unique_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo']
        device = adb.get_device()
        self.assertEqual(device.serial, 'foo')

    @mock.patch('adb.device.get_devices')
    def test_no_unique_device(self, mock_get_devices):
        mock_get_devices.return_value = ['foo', 'bar']
        self.assertRaises(adb.NoUniqueDeviceError, adb.get_device)


class DeviceTest(unittest.TestCase):
    def setUp(self):
        self.device = adb.get_device()


class ForwardReverseTest(DeviceTest):
    def _test_no_rebind(self, description, direction_list, direction,
                       direction_no_rebind, direction_remove_all):
        msg = direction_list()
        self.assertEqual('', msg.strip(),
                         description + ' list must be empty to run this test.')

        # Use --no-rebind with no existing binding
        direction_no_rebind('tcp:5566', 'tcp:6655')
        msg = direction_list()
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))

        # Use --no-rebind with existing binding
        with self.assertRaises(subprocess.CalledProcessError):
            direction_no_rebind('tcp:5566', 'tcp:6677')
        msg = direction_list()
        self.assertFalse(re.search(r'tcp:5566.+tcp:6677', msg))
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))

        # Use the absence of --no-rebind with existing binding
        direction('tcp:5566', 'tcp:6677')
        msg = direction_list()
        self.assertFalse(re.search(r'tcp:5566.+tcp:6655', msg))
        self.assertTrue(re.search(r'tcp:5566.+tcp:6677', msg))

        direction_remove_all()
        msg = direction_list()
        self.assertEqual('', msg.strip())

    def test_forward_no_rebind(self):
        self._test_no_rebind('forward', self.device.forward_list,
                            self.device.forward, self.device.forward_no_rebind,
                            self.device.forward_remove_all)

    def test_reverse_no_rebind(self):
        self._test_no_rebind('reverse', self.device.reverse_list,
                            self.device.reverse, self.device.reverse_no_rebind,
                            self.device.reverse_remove_all)

    def test_forward(self):
        msg = self.device.forward_list()
        self.assertEqual('', msg.strip(),
                         'Forwarding list must be empty to run this test.')
        self.device.forward('tcp:5566', 'tcp:6655')
        msg = self.device.forward_list()
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))
        self.device.forward('tcp:7788', 'tcp:8877')
        msg = self.device.forward_list()
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))
        self.assertTrue(re.search(r'tcp:7788.+tcp:8877', msg))
        self.device.forward_remove('tcp:5566')
        msg = self.device.forward_list()
        self.assertFalse(re.search(r'tcp:5566.+tcp:6655', msg))
        self.assertTrue(re.search(r'tcp:7788.+tcp:8877', msg))
        self.device.forward_remove_all()
        msg = self.device.forward_list()
        self.assertEqual('', msg.strip())

    def test_reverse(self):
        msg = self.device.reverse_list()
        self.assertEqual('', msg.strip(),
                         'Reverse forwarding list must be empty to run this test.')
        self.device.reverse('tcp:5566', 'tcp:6655')
        msg = self.device.reverse_list()
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))
        self.device.reverse('tcp:7788', 'tcp:8877')
        msg = self.device.reverse_list()
        self.assertTrue(re.search(r'tcp:5566.+tcp:6655', msg))
        self.assertTrue(re.search(r'tcp:7788.+tcp:8877', msg))
        self.device.reverse_remove('tcp:5566')
        msg = self.device.reverse_list()
        self.assertFalse(re.search(r'tcp:5566.+tcp:6655', msg))
        self.assertTrue(re.search(r'tcp:7788.+tcp:8877', msg))
        self.device.reverse_remove_all()
        msg = self.device.reverse_list()
        self.assertEqual('', msg.strip())

    # Note: If you run this test when adb connect'd to a physical device over
    # TCP, it will fail in adb reverse due to https://code.google.com/p/android/issues/detail?id=189821
    def test_forward_reverse_echo(self):
        """Send data through adb forward and read it back via adb reverse"""
        forward_port = 12345
        reverse_port = forward_port + 1
        forward_spec = "tcp:" + str(forward_port)
        reverse_spec = "tcp:" + str(reverse_port)
        forward_setup = False
        reverse_setup = False

        try:
            # listen on localhost:forward_port, connect to remote:forward_port
            self.device.forward(forward_spec, forward_spec)
            forward_setup = True
            # listen on remote:forward_port, connect to localhost:reverse_port
            self.device.reverse(forward_spec, reverse_spec)
            reverse_setup = True

            listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            with contextlib.closing(listener):
                # Use SO_REUSEADDR so that subsequent runs of the test can grab
                # the port even if it is in TIME_WAIT.
                listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

                # Listen on localhost:reverse_port before connecting to
                # localhost:forward_port because that will cause adb to connect
                # back to localhost:reverse_port.
                listener.bind(('127.0.0.1', reverse_port))
                listener.listen(4)

                client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                with contextlib.closing(client):
                    # Connect to the listener.
                    client.connect(('127.0.0.1', forward_port))

                    # Accept the client connection.
                    accepted_connection, addr = listener.accept()
                    with contextlib.closing(accepted_connection) as server:
                        data = 'hello'

                        # Send data into the port setup by adb forward.
                        client.sendall(data)
                        # Explicitly close() so that server gets EOF.
                        client.close()

                        # Verify that the data came back via adb reverse.
                        self.assertEqual(data, server.makefile().read())
        finally:
            if reverse_setup:
                self.device.reverse_remove(forward_spec)
            if forward_setup:
                self.device.forward_remove(forward_spec)


class ShellTest(DeviceTest):
    def _interactive_shell(self, shell_args, input):
        """Runs an interactive adb shell.

        Args:
          shell_args: List of string arguments to `adb shell`.
          input: String input to send to the interactive shell.

        Returns:
          The remote exit code.

        Raises:
          unittest.SkipTest: The device doesn't support exit codes.
        """
        if self.device.SHELL_PROTOCOL_FEATURE not in self.device.features:
            raise unittest.SkipTest('exit codes are unavailable on this device')

        proc = subprocess.Popen(
                self.device.adb_cmd + ['shell'] + shell_args,
                stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                stderr=subprocess.PIPE)
        # Closing host-side stdin doesn't trigger a PTY shell to exit so we need
        # to explicitly add an exit command to close the session from the device
        # side, plus the necessary newline to complete the interactive command.
        proc.communicate(input + '; exit\n')
        return proc.returncode

    def test_cat(self):
        """Check that we can at least cat a file."""
        out = self.device.shell(['cat', '/proc/uptime'])[0].strip()
        elements = out.split()
        self.assertEqual(len(elements), 2)

        uptime, idle = elements
        self.assertGreater(float(uptime), 0.0)
        self.assertGreater(float(idle), 0.0)

    def test_throws_on_failure(self):
        self.assertRaises(adb.ShellError, self.device.shell, ['false'])

    def test_output_not_stripped(self):
        out = self.device.shell(['echo', 'foo'])[0]
        self.assertEqual(out, 'foo' + self.device.linesep)

    def test_shell_nocheck_failure(self):
        rc, out, _ = self.device.shell_nocheck(['false'])
        self.assertNotEqual(rc, 0)
        self.assertEqual(out, '')

    def test_shell_nocheck_output_not_stripped(self):
        rc, out, _ = self.device.shell_nocheck(['echo', 'foo'])
        self.assertEqual(rc, 0)
        self.assertEqual(out, 'foo' + self.device.linesep)

    def test_can_distinguish_tricky_results(self):
        # If result checking on ADB shell is naively implemented as
        # `adb shell <cmd>; echo $?`, we would be unable to distinguish the
        # output from the result for a cmd of `echo -n 1`.
        rc, out, _ = self.device.shell_nocheck(['echo', '-n', '1'])
        self.assertEqual(rc, 0)
        self.assertEqual(out, '1')

    def test_line_endings(self):
        """Ensure that line ending translation is not happening in the pty.

        Bug: http://b/19735063
        """
        output = self.device.shell(['uname'])[0]
        self.assertEqual(output, 'Linux' + self.device.linesep)

    def test_pty_logic(self):
        """Tests that a PTY is allocated when it should be.

        PTY allocation behavior should match ssh; some behavior requires
        a terminal stdin to test so this test will be skipped if stdin
        is not a terminal.
        """
        if self.device.SHELL_PROTOCOL_FEATURE not in self.device.features:
            raise unittest.SkipTest('PTY arguments unsupported on this device')
        if not os.isatty(sys.stdin.fileno()):
            raise unittest.SkipTest('PTY tests require stdin terminal')

        def check_pty(args):
            """Checks adb shell PTY allocation.

            Tests |args| for terminal and non-terminal stdin.

            Args:
                args: -Tt args in a list (e.g. ['-t', '-t']).

            Returns:
                A tuple (<terminal>, <non-terminal>). True indicates
                the corresponding shell allocated a remote PTY.
            """
            test_cmd = self.device.adb_cmd + ['shell'] + args + ['[ -t 0 ]']

            terminal = subprocess.Popen(
                    test_cmd, stdin=None,
                    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            terminal.communicate()

            non_terminal = subprocess.Popen(
                    test_cmd, stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            non_terminal.communicate()

            return (terminal.returncode == 0, non_terminal.returncode == 0)

        # -T: never allocate PTY.
        self.assertEqual((False, False), check_pty(['-T']))

        # No args: PTY only if stdin is a terminal and shell is interactive,
        # which is difficult to reliably test from a script.
        self.assertEqual((False, False), check_pty([]))

        # -t: PTY if stdin is a terminal.
        self.assertEqual((True, False), check_pty(['-t']))

        # -t -t: always allocate PTY.
        self.assertEqual((True, True), check_pty(['-t', '-t']))

    def test_shell_protocol(self):
        """Tests the shell protocol on the device.

        If the device supports shell protocol, this gives us the ability
        to separate stdout/stderr and return the exit code directly.

        Bug: http://b/19734861
        """
        if self.device.SHELL_PROTOCOL_FEATURE not in self.device.features:
            raise unittest.SkipTest('shell protocol unsupported on this device')

        # Shell protocol should be used by default.
        result = self.device.shell_nocheck(
                shlex.split('echo foo; echo bar >&2; exit 17'))
        self.assertEqual(17, result[0])
        self.assertEqual('foo' + self.device.linesep, result[1])
        self.assertEqual('bar' + self.device.linesep, result[2])

        self.assertEqual(17, self._interactive_shell([], 'exit 17'))

        # -x flag should disable shell protocol.
        result = self.device.shell_nocheck(
                shlex.split('-x echo foo; echo bar >&2; exit 17'))
        self.assertEqual(0, result[0])
        self.assertEqual('foo{0}bar{0}'.format(self.device.linesep), result[1])
        self.assertEqual('', result[2])

        self.assertEqual(0, self._interactive_shell(['-x'], 'exit 17'))

    def test_non_interactive_sigint(self):
        """Tests that SIGINT in a non-interactive shell kills the process.

        This requires the shell protocol in order to detect the broken
        pipe; raw data transfer mode will only see the break once the
        subprocess tries to read or write.

        Bug: http://b/23825725
        """
        if self.device.SHELL_PROTOCOL_FEATURE not in self.device.features:
            raise unittest.SkipTest('shell protocol unsupported on this device')

        # Start a long-running process.
        sleep_proc = subprocess.Popen(
                self.device.adb_cmd + shlex.split('shell echo $$; sleep 60'),
                stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT)
        remote_pid = sleep_proc.stdout.readline().strip()
        self.assertIsNone(sleep_proc.returncode, 'subprocess terminated early')
        proc_query = shlex.split('ps {0} | grep {0}'.format(remote_pid))

        # Verify that the process is running, send signal, verify it stopped.
        self.device.shell(proc_query)
        os.kill(sleep_proc.pid, signal.SIGINT)
        sleep_proc.communicate()
        self.assertEqual(1, self.device.shell_nocheck(proc_query)[0],
                         'subprocess failed to terminate')

    def test_non_interactive_stdin(self):
        """Tests that non-interactive shells send stdin."""
        if self.device.SHELL_PROTOCOL_FEATURE not in self.device.features:
            raise unittest.SkipTest('non-interactive stdin unsupported '
                                    'on this device')

        # Test both small and large inputs.
        small_input = 'foo'
        large_input = '\n'.join(c * 100 for c in (string.ascii_letters +
                                                  string.digits))

        for input in (small_input, large_input):
            proc = subprocess.Popen(self.device.adb_cmd + ['shell', 'cat'],
                                    stdin=subprocess.PIPE,
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE)
            stdout, stderr = proc.communicate(input)
            self.assertEqual(input.splitlines(), stdout.splitlines())
            self.assertEqual('', stderr)


class ArgumentEscapingTest(DeviceTest):
    def test_shell_escaping(self):
        """Make sure that argument escaping is somewhat sane."""

        # http://b/19734868
        # Note that this actually matches ssh(1)'s behavior --- it's
        # converted to `sh -c echo hello; echo world` which sh interprets
        # as `sh -c echo` (with an argument to that shell of "hello"),
        # and then `echo world` back in the first shell.
        result = self.device.shell(
            shlex.split("sh -c 'echo hello; echo world'"))[0]
        result = result.splitlines()
        self.assertEqual(['', 'world'], result)
        # If you really wanted "hello" and "world", here's what you'd do:
        result = self.device.shell(
            shlex.split(r'echo hello\;echo world'))[0].splitlines()
        self.assertEqual(['hello', 'world'], result)

        # http://b/15479704
        result = self.device.shell(shlex.split("'true && echo t'"))[0].strip()
        self.assertEqual('t', result)
        result = self.device.shell(
            shlex.split("sh -c 'true && echo t'"))[0].strip()
        self.assertEqual('t', result)

        # http://b/20564385
        result = self.device.shell(shlex.split('FOO=a BAR=b echo t'))[0].strip()
        self.assertEqual('t', result)
        result = self.device.shell(
            shlex.split(r'echo -n 123\;uname'))[0].strip()
        self.assertEqual('123Linux', result)

    def test_install_argument_escaping(self):
        """Make sure that install argument escaping works."""
        # http://b/20323053, http://b/3090932.
        for file_suffix in ('-text;ls;1.apk', "-Live Hold'em.apk"):
            tf = tempfile.NamedTemporaryFile('wb', suffix=file_suffix,
                                             delete=False)
            tf.close()

            # Installing bogus .apks fails if the device supports exit codes.
            try:
                output = self.device.install(tf.name)
            except subprocess.CalledProcessError as e:
                output = e.output

            self.assertIn(file_suffix, output)
            os.remove(tf.name)


class RootUnrootTest(DeviceTest):
    def _test_root(self):
        message = self.device.root()
        if 'adbd cannot run as root in production builds' in message:
            return
        self.device.wait()
        self.assertEqual('root', self.device.shell(['id', '-un'])[0].strip())

    def _test_unroot(self):
        self.device.unroot()
        self.device.wait()
        self.assertEqual('shell', self.device.shell(['id', '-un'])[0].strip())

    def test_root_unroot(self):
        """Make sure that adb root and adb unroot work, using id(1)."""
        if self.device.get_prop('ro.debuggable') != '1':
            raise unittest.SkipTest('requires rootable build')

        original_user = self.device.shell(['id', '-un'])[0].strip()
        try:
            if original_user == 'root':
                self._test_unroot()
                self._test_root()
            elif original_user == 'shell':
                self._test_root()
                self._test_unroot()
        finally:
            if original_user == 'root':
                self.device.root()
            else:
                self.device.unroot()
            self.device.wait()


class TcpIpTest(DeviceTest):
    def test_tcpip_failure_raises(self):
        """adb tcpip requires a port.

        Bug: http://b/22636927
        """
        self.assertRaises(
            subprocess.CalledProcessError, self.device.tcpip, '')
        self.assertRaises(
            subprocess.CalledProcessError, self.device.tcpip, 'foo')


class SystemPropertiesTest(DeviceTest):
    def test_get_prop(self):
        self.assertEqual(self.device.get_prop('init.svc.adbd'), 'running')

    @requires_root
    def test_set_prop(self):
        prop_name = 'foo.bar'
        self.device.shell(['setprop', prop_name, '""'])

        self.device.set_prop(prop_name, 'qux')
        self.assertEqual(
            self.device.shell(['getprop', prop_name])[0].strip(), 'qux')


def compute_md5(string):
    hsh = hashlib.md5()
    hsh.update(string)
    return hsh.hexdigest()


def get_md5_prog(device):
    """Older platforms (pre-L) had the name md5 rather than md5sum."""
    try:
        device.shell(['md5sum', '/proc/uptime'])
        return 'md5sum'
    except adb.ShellError:
        return 'md5'


class HostFile(object):
    def __init__(self, handle, checksum):
        self.handle = handle
        self.checksum = checksum
        self.full_path = handle.name
        self.base_name = os.path.basename(self.full_path)


class DeviceFile(object):
    def __init__(self, checksum, full_path):
        self.checksum = checksum
        self.full_path = full_path
        self.base_name = posixpath.basename(self.full_path)


def make_random_host_files(in_dir, num_files):
    min_size = 1 * (1 << 10)
    max_size = 16 * (1 << 10)

    files = []
    for _ in xrange(num_files):
        file_handle = tempfile.NamedTemporaryFile(dir=in_dir, delete=False)

        size = random.randrange(min_size, max_size, 1024)
        rand_str = os.urandom(size)
        file_handle.write(rand_str)
        file_handle.flush()
        file_handle.close()

        md5 = compute_md5(rand_str)
        files.append(HostFile(file_handle, md5))
    return files


def make_random_device_files(device, in_dir, num_files, prefix='device_tmpfile'):
    min_size = 1 * (1 << 10)
    max_size = 16 * (1 << 10)

    files = []
    for file_num in xrange(num_files):
        size = random.randrange(min_size, max_size, 1024)

        base_name = prefix + str(file_num)
        full_path = posixpath.join(in_dir, base_name)

        device.shell(['dd', 'if=/dev/urandom', 'of={}'.format(full_path),
                      'bs={}'.format(size), 'count=1'])
        dev_md5, _ = device.shell([get_md5_prog(device), full_path])[0].split()

        files.append(DeviceFile(dev_md5, full_path))
    return files


class FileOperationsTest(DeviceTest):
    SCRATCH_DIR = '/data/local/tmp'
    DEVICE_TEMP_FILE = SCRATCH_DIR + '/adb_test_file'
    DEVICE_TEMP_DIR = SCRATCH_DIR + '/adb_test_dir'

    def _verify_remote(self, checksum, remote_path):
        dev_md5, _ = self.device.shell([get_md5_prog(self.device),
                                        remote_path])[0].split()
        self.assertEqual(checksum, dev_md5)

    def _verify_local(self, checksum, local_path):
        with open(local_path, 'rb') as host_file:
            host_md5 = compute_md5(host_file.read())
            self.assertEqual(host_md5, checksum)

    def test_push(self):
        """Push a randomly generated file to specified device."""
        kbytes = 512
        tmp = tempfile.NamedTemporaryFile(mode='wb', delete=False)
        rand_str = os.urandom(1024 * kbytes)
        tmp.write(rand_str)
        tmp.close()

        self.device.shell(['rm', '-rf', self.DEVICE_TEMP_FILE])
        self.device.push(local=tmp.name, remote=self.DEVICE_TEMP_FILE)

        self._verify_remote(compute_md5(rand_str), self.DEVICE_TEMP_FILE)
        self.device.shell(['rm', '-f', self.DEVICE_TEMP_FILE])

        os.remove(tmp.name)

    def test_push_dir(self):
        """Push a randomly generated directory of files to the device."""
        self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        self.device.shell(['mkdir', self.DEVICE_TEMP_DIR])

        try:
            host_dir = tempfile.mkdtemp()

            # Make sure the temp directory isn't setuid, or else adb will complain.
            os.chmod(host_dir, 0o700)

            # Create 32 random files.
            temp_files = make_random_host_files(in_dir=host_dir, num_files=32)
            self.device.push(host_dir, self.DEVICE_TEMP_DIR)

            for temp_file in temp_files:
                remote_path = posixpath.join(self.DEVICE_TEMP_DIR,
                                             os.path.basename(host_dir),
                                             temp_file.base_name)
                self._verify_remote(temp_file.checksum, remote_path)
            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)

    @unittest.expectedFailure # b/25566053
    def test_push_empty(self):
        """Push a directory containing an empty directory to the device."""
        self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        self.device.shell(['mkdir', self.DEVICE_TEMP_DIR])

        try:
            host_dir = tempfile.mkdtemp()

            # Make sure the temp directory isn't setuid, or else adb will complain.
            os.chmod(host_dir, 0o700)

            # Create an empty directory.
            os.mkdir(os.path.join(host_dir, 'empty'))

            self.device.push(host_dir, self.DEVICE_TEMP_DIR)

            test_empty_cmd = ['[', '-d',
                              os.path.join(self.DEVICE_TEMP_DIR, 'empty')]
            rc, _, _ = self.device.shell_nocheck(test_empty_cmd)
            self.assertEqual(rc, 0)
            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)

    def test_multiple_push(self):
        """Push multiple files to the device in one adb push command.

        Bug: http://b/25324823
        """

        self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        self.device.shell(['mkdir', self.DEVICE_TEMP_DIR])

        try:
            host_dir = tempfile.mkdtemp()

            # Create some random files and a subdirectory containing more files.
            temp_files = make_random_host_files(in_dir=host_dir, num_files=4)

            subdir = os.path.join(host_dir, "subdir")
            os.mkdir(subdir)
            subdir_temp_files = make_random_host_files(in_dir=subdir,
                                                       num_files=4)

            paths = map(lambda temp_file: temp_file.full_path, temp_files)
            paths.append(subdir)
            self.device._simple_call(['push'] + paths + [self.DEVICE_TEMP_DIR])

            for temp_file in temp_files:
                remote_path = posixpath.join(self.DEVICE_TEMP_DIR,
                                             temp_file.base_name)
                self._verify_remote(temp_file.checksum, remote_path)

            for subdir_temp_file in subdir_temp_files:
                remote_path = posixpath.join(self.DEVICE_TEMP_DIR,
                                             # BROKEN: http://b/25394682
                                             # "subdir",
                                             temp_file.base_name)
                self._verify_remote(temp_file.checksum, remote_path)


            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)


    def _test_pull(self, remote_file, checksum):
        tmp_write = tempfile.NamedTemporaryFile(mode='wb', delete=False)
        tmp_write.close()
        self.device.pull(remote=remote_file, local=tmp_write.name)
        with open(tmp_write.name, 'rb') as tmp_read:
            host_contents = tmp_read.read()
            host_md5 = compute_md5(host_contents)
        self.assertEqual(checksum, host_md5)
        os.remove(tmp_write.name)

    @requires_non_root
    def test_pull_error_reporting(self):
        self.device.shell(['touch', self.DEVICE_TEMP_FILE])
        self.device.shell(['chmod', 'a-rwx', self.DEVICE_TEMP_FILE])

        try:
            output = self.device.pull(remote=self.DEVICE_TEMP_FILE, local='x')
        except subprocess.CalledProcessError as e:
            output = e.output

        self.assertIn('Permission denied', output)

        self.device.shell(['rm', '-f', self.DEVICE_TEMP_FILE])

    def test_pull(self):
        """Pull a randomly generated file from specified device."""
        kbytes = 512
        self.device.shell(['rm', '-rf', self.DEVICE_TEMP_FILE])
        cmd = ['dd', 'if=/dev/urandom',
               'of={}'.format(self.DEVICE_TEMP_FILE), 'bs=1024',
               'count={}'.format(kbytes)]
        self.device.shell(cmd)
        dev_md5, _ = self.device.shell(
            [get_md5_prog(self.device), self.DEVICE_TEMP_FILE])[0].split()
        self._test_pull(self.DEVICE_TEMP_FILE, dev_md5)
        self.device.shell_nocheck(['rm', self.DEVICE_TEMP_FILE])

    def test_pull_dir(self):
        """Pull a randomly generated directory of files from the device."""
        try:
            host_dir = tempfile.mkdtemp()

            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
            self.device.shell(['mkdir', '-p', self.DEVICE_TEMP_DIR])

            # Populate device directory with random files.
            temp_files = make_random_device_files(
                self.device, in_dir=self.DEVICE_TEMP_DIR, num_files=32)

            self.device.pull(remote=self.DEVICE_TEMP_DIR, local=host_dir)

            for temp_file in temp_files:
                host_path = os.path.join(host_dir, temp_file.base_name)

            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)

    def test_pull_empty(self):
        """Pull a directory containing an empty directory from the device."""
        try:
            host_dir = tempfile.mkdtemp()

            remote_empty_path = posixpath.join(self.DEVICE_TEMP_DIR, 'empty')
            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
            self.device.shell(['mkdir', '-p', remote_empty_path])

            self.device.pull(remote=remote_empty_path, local=host_dir)
            self.assertTrue(os.path.isdir(os.path.join(host_dir, 'empty')))
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)

    def test_multiple_pull(self):
        """Pull a randomly generated directory of files from the device."""

        try:
            host_dir = tempfile.mkdtemp()

            subdir = posixpath.join(self.DEVICE_TEMP_DIR, "subdir")
            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
            self.device.shell(['mkdir', '-p', subdir])

            # Create some random files and a subdirectory containing more files.
            temp_files = make_random_device_files(
                self.device, in_dir=self.DEVICE_TEMP_DIR, num_files=4)

            subdir_temp_files = make_random_device_files(
                self.device, in_dir=subdir, num_files=4, prefix='subdir_')

            paths = map(lambda temp_file: temp_file.full_path, temp_files)
            paths.append(subdir)
            self.device._simple_call(['pull'] + paths + [host_dir])

            for temp_file in temp_files:
                local_path = os.path.join(host_dir, temp_file.base_name)
                self._verify_local(temp_file.checksum, local_path)

            for subdir_temp_file in subdir_temp_files:
                local_path = os.path.join(host_dir,
                                          "subdir",
                                          subdir_temp_file.base_name)
                self._verify_local(subdir_temp_file.checksum, local_path)

            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if host_dir is not None:
                shutil.rmtree(host_dir)

    def test_sync(self):
        """Sync a randomly generated directory of files to specified device."""

        try:
            base_dir = tempfile.mkdtemp()

            # Create mirror device directory hierarchy within base_dir.
            full_dir_path = base_dir + self.DEVICE_TEMP_DIR
            os.makedirs(full_dir_path)

            # Create 32 random files within the host mirror.
            temp_files = make_random_host_files(in_dir=full_dir_path, num_files=32)

            # Clean up any trash on the device.
            device = adb.get_device(product=base_dir)
            device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])

            device.sync('data')

            # Confirm that every file on the device mirrors that on the host.
            for temp_file in temp_files:
                device_full_path = posixpath.join(self.DEVICE_TEMP_DIR,
                                                  temp_file.base_name)
                dev_md5, _ = device.shell(
                    [get_md5_prog(self.device), device_full_path])[0].split()
                self.assertEqual(temp_file.checksum, dev_md5)

            self.device.shell(['rm', '-rf', self.DEVICE_TEMP_DIR])
        finally:
            if base_dir is not None:
                shutil.rmtree(base_dir)

    def test_unicode_paths(self):
        """Ensure that we can support non-ASCII paths, even on Windows."""
        name = u'로보카 폴리'

        self.device.shell(['rm', '-f', '/data/local/tmp/adb-test-*'])
        remote_path = u'/data/local/tmp/adb-test-{}'.format(name)

        ## push.
        tf = tempfile.NamedTemporaryFile('wb', suffix=name, delete=False)
        tf.close()
        self.device.push(tf.name, remote_path)
        os.remove(tf.name)
        self.assertFalse(os.path.exists(tf.name))

        # Verify that the device ended up with the expected UTF-8 path
        output = self.device.shell(
                ['ls', '/data/local/tmp/adb-test-*'])[0].strip()
        self.assertEqual(remote_path.encode('utf-8'), output)

        # pull.
        self.device.pull(remote_path, tf.name)
        self.assertTrue(os.path.exists(tf.name))
        os.remove(tf.name)
        self.device.shell(['rm', '-f', '/data/local/tmp/adb-test-*'])


def main():
    random.seed(0)
    if len(adb.get_devices()) > 0:
        suite = unittest.TestLoader().loadTestsFromName(__name__)
        unittest.TextTestRunner(verbosity=3).run(suite)
    else:
        print('Test suite must be run with attached devices')


if __name__ == '__main__':
    main()
