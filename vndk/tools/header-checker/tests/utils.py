#!/usr/bin/env python3

import tempfile
import os
import subprocess

SCRIPT_DIR = os.path.abspath(os.path.dirname(__file__))
AOSP_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, *['..'] * 5))

BUILTIN_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'bionic', 'libc', 'include'),
    os.path.join(AOSP_DIR, 'external', 'libcxx', 'include'),
    os.path.join(AOSP_DIR, 'prebuilts', 'sdk', 'renderscript', 'clang-include'),
)

EXPORTED_HEADERS_DIR = (
    os.path.join(AOSP_DIR, 'development', 'vndk', 'tools', 'header-checker',
                 'tests'),
)

def run_header_checker(input_path, cflags=[]):
    with tempfile.TemporaryDirectory() as tmp:
        output_name = os.path.join(tmp, os.path.basename(input_path)) + '.dump'
        cmd = ['header-abi-dumper', '-o', output_name, input_path,]
        for d in EXPORTED_HEADERS_DIR:
            cmd += ['-I', d]
        cmd+= ['--']
        for d in BUILTIN_HEADERS_DIR:
            cmd += ['-isystem', d]
        cmd += cflags
        subprocess.check_call(cmd)
        with open(output_name, 'r') as f:
            return f.read().replace(SCRIPT_DIR, '.')
