#!/usr/bin/env python3
#
# Copyright (C) 2021 The Android Open Source Project
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

# acov-llvm.py is a tool for gathering coverage information from a device and
# generating an LLVM coverage report from that information. To use:
#
# This script would work only when the device image was built with the following
# build variables:
#     CLANG_COVERAGE=true NATIVE_COVERAGE_PATHS="<list-of-paths>"
#
# 1. [optional] Reset coverage information on the device
#   $ acov-llvm.py clean-device
#
# 2. Run tests
#
# 3. Flush coverage
# from select daemons and system processes on the device
#   $ acov-llvm.py flush [list of process names]
# or from all processes on the device:
#   $ acov-llvm.py flush
#
# 4. Pull coverage from device and generate coverage report
#   $ acov-llvm.py report -s <one-or-more-source-paths-in-$ANDROID_BUILD_TOP> \
#                         -b <one-or-more-binaries-in-$OUT> \
# E.g.:
# acov-llvm.py report \
#         -s bionic \
#         -b \
#         $OUT/symbols/apex/com.android.runtime/lib/bionic/libc.so \
#         $OUT/symbols/apex/com.android.runtime/lib/bionic/libm.so

import argparse
import logging
import os
import re
import subprocess
import time
import tempfile

from pathlib import Path

FLUSH_SLEEP = 60


def android_build_top():
    return Path(os.environ.get('ANDROID_BUILD_TOP', None))


def _get_clang_revision():
    version_output = subprocess.check_output(
        android_build_top() / 'build/soong/scripts/get_clang_version.py',
        text=True)
    return version_output.strip()


CLANG_TOP = android_build_top() / 'prebuilts/clang/host/linux-x86/' \
        / _get_clang_revision()
LLVM_PROFDATA_PATH = CLANG_TOP / 'bin' / 'llvm-profdata'
LLVM_COV_PATH = CLANG_TOP / 'bin' / 'llvm-cov'


def check_output(cmd, *args, **kwargs):
    """subprocess.check_output with logging."""
    cmd_str = cmd if isinstance(cmd, str) else ' '.join(cmd)
    logging.debug(cmd_str)
    return subprocess.run(
        cmd, *args, **kwargs, check=True, stdout=subprocess.PIPE).stdout


def adb(cmd, *args, **kwargs):
    """call 'adb <cmd>' with logging."""
    return check_output(['adb'] + cmd, *args, **kwargs)


def adb_root(*args, **kwargs):
    """call 'adb root' with logging."""
    return adb(['root'], *args, **kwargs)


def adb_shell(cmd, *args, **kwargs):
    """call 'adb shell <cmd>' with logging."""
    return adb(['shell'] + cmd, *args, **kwargs)


def send_flush_signal(pids=None):

    def _has_handler_sig37(pid):
        try:
            status = adb_shell(['cat', f'/proc/{pid}/status'],
                               text=True,
                               stderr=subprocess.DEVNULL)
        except subprocess.CalledProcessError:
            logging.warning(f'Process {pid} is no longer active')
            return False

        status = status.split('\n')
        sigcgt = [
            line.split(':\t')[1] for line in status if line.startswith('SigCgt')
        ]
        if not sigcgt:
            logging.warning(f'Cannot find \'SigCgt:\' in /proc/{pid}/status')
            return False
        return int(sigcgt[0], base=16) & (1 << 36)

    if not pids:
        output = adb_shell(['ps', '-eo', 'pid'], text=True)
        pids = [pid.strip() for pid in output.split()]
        pids = pids[1:]  # ignore the column header
    pids = [pid for pid in pids if _has_handler_sig37(pid)]

    if not pids:
        logging.warning(
            f'couldn\'t find any process with handler for signal 37')

    # Some processes may have exited after we run `ps` command above - ignore failures when
    # sending flush signal.
    # We rely on kill(1) sending the signal to all pids on the command line even if some don't
    # exist.  This is true of toybox and "probably implied" by POSIX, even if not explicitly called 
    # out [https://pubs.opengroup.org/onlinepubs/9699919799/utilities/kill.html].
    try:
        adb_shell(['kill', '-37'] + pids)
    except subprocess.CalledProcessError:
        logging.warning('Sending flush signal failed - some pids no longer active')


def do_clean_device(args):
    adb_root()

    logging.info('resetting coverage on device')
    send_flush_signal()

    logging.info(
        f'sleeping for {FLUSH_SLEEP} seconds for coverage to be written')
    time.sleep(FLUSH_SLEEP)

    logging.info('deleting coverage data from device')
    adb_shell(['rm', '-rf', '/data/misc/trace/*.profraw'])


def do_flush(args):
    adb_root()

    if args.procnames:
        pids = adb_shell(['pidof'] + args.procnames, text=True).split()
        logging.info(f'flushing coverage for pids: {pids}')
    else:
        pids = None
        logging.info('flushing coverage for all processes on device')

    send_flush_signal(pids)

    logging.info(
        f'sleeping for {FLUSH_SLEEP} seconds for coverage to be written')
    time.sleep(FLUSH_SLEEP)


def do_report(args):
    adb_root()

    temp_dir = tempfile.mkdtemp(
        prefix='covreport-', dir=os.environ.get('ANDROID_BUILD_TOP', None))
    logging.info(f'generating coverage report in {temp_dir}')

    # Pull coverage files from /data/misc/trace on the device
    compressed = adb_shell(['tar', '-czf', '-', '-C', '/data/misc', 'trace'])
    check_output(['tar', 'zxvf', '-', '-C', temp_dir], input=compressed)

    # Call llvm-profdata followed by llvm-cov
    profdata = f'{temp_dir}/merged.profdata'
    check_output(
        f'{LLVM_PROFDATA_PATH} merge --failure-mode=all --output={profdata} {temp_dir}/trace/*.profraw',
        shell=True)

    object_flags = [args.binary[0]] + ['--object=' + b for b in args.binary[1:]]
    source_dirs = ['/proc/self/cwd/' + s for s in args.source_dir]

    output_dir = f'{temp_dir}/html'

    check_output([
        str(LLVM_COV_PATH), 'show', f'--instr-profile={profdata}',
        '--format=html', f'--output-dir={output_dir}',
        '--show-region-summary=false'
    ] + object_flags + source_dirs)

    print(f'Coverage report data written in {output_dir}')


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '-v',
        '--verbose',
        action='store_true',
        default=False,
        help='enable debug logging')

    subparsers = parser.add_subparsers(dest='command', required=True)

    clean_device = subparsers.add_parser(
        'clean-device', help='reset coverage on device')
    clean_device.set_defaults(func=do_clean_device)

    flush = subparsers.add_parser(
        'flush', help='flush coverage for processes on device')
    flush.add_argument(
        'procnames',
        nargs='*',
        metavar='PROCNAME',
        help='flush coverage for one or more processes with name PROCNAME')
    flush.set_defaults(func=do_flush)

    report = subparsers.add_parser(
        'report', help='fetch coverage from device and generate report')
    report.add_argument(
        '-b',
        '--binary',
        nargs='+',
        metavar='BINARY',
        action='extend',
        required=True,
        help='generate coverage report for BINARY')
    report.add_argument(
        '-s',
        '--source-dir',
        nargs='+',
        action='extend',
        metavar='PATH',
        required=True,
        help='generate coverage report for source files in PATH')
    report.set_defaults(func=do_report)
    return parser.parse_args()


def main():
    args = parse_args()
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)

    args.func(args)


if __name__ == '__main__':
    main()
