#!/usr/bin/env python3
#
# Copyright (C) 2019 The Android Open Source Project
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

"""Fix prebuilt ELF check errors.

This script fixes prebuilt ELF check errors by updating LOCAL_SHARED_LIBRARIES,
adding LOCAL_MULTILIB, or adding LOCAL_CHECK_ELF_FILES.
"""

import argparse

from elfcheck.rewriter import Rewriter


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('android_mk', help='path to Android.mk')
    parser.add_argument('--var', action='append', default=[],
                        metavar='KEY=VALUE', help='extra makefile variables')
    return parser.parse_args()


def _parse_arg_var(args_var):
    variables = {}
    for var in args_var:
        if '=' in var:
            key, value = var.split('=', 1)
            key = key.strip()
            value = value.strip()
            variables[key] = value
    return variables


def main():
    """Main function"""
    args = _parse_args()
    rewriter = Rewriter(args.android_mk, _parse_arg_var(args.var))
    rewriter.rewrite()


if __name__ == '__main__':
    main()
