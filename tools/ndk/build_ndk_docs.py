#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
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
"""Generates the NDK API docs for local viewing.

Note that the local docs will not exactly match the docs that are uploaded to
devsite. The theming is different and the per-file view is not available.
Ensure that your documentation is accessible from the module view or it will
not be discoverable on devsite.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys

THIS_DIR = Path(__file__).resolve().parent
ANDROID_TOP = THIS_DIR.parents[2]


def check_environment() -> None:
    """Validates that we have everything we need from the environment."""
    if shutil.which('doxygen') is None:
        sys.exit('Doxygen not found. Run `sudo apt install doxygen`.')

    if 'ANDROID_PRODUCT_OUT' not in os.environ:
        sys.exit('Could not find ANDROID_PRODUCT_OUT. Run lunch.')


def build_ndk() -> None:
    """Builds the NDK sysroot."""
    subprocess.run(["build/soong/soong_ui.bash", "--make-mode", "ndk"],
                   cwd=ANDROID_TOP,
                   check=True)


def generate_docs() -> None:
    """Generates the NDK API reference."""
    product_out = Path(os.environ['ANDROID_PRODUCT_OUT'])
    out_dir = product_out.parents[1] / 'common/ndk-docs'
    html_dir = out_dir / 'html'
    input_dir = product_out.parents[2] / 'soong/ndk/sysroot/usr/include'
    doxyfile_template = ANDROID_TOP / 'frameworks/native/docs/Doxyfile'
    out_dir.mkdir(parents=True, exist_ok=True)
    doxyfile = out_dir / 'Doxyfile'

    doxyfile_contents = doxyfile_template.read_text()
    doxyfile_contents += f'\nINPUT={input_dir}\nHTML_OUTPUT={html_dir}'
    doxyfile.write_text(doxyfile_contents)

    subprocess.run(['doxygen', str(doxyfile)], cwd=ANDROID_TOP, check=True)
    index = html_dir / 'index.html'
    print(f'Generated NDK API documentation to {index.as_uri()}')


def parse_args() -> argparse.Namespace:
    """Parses command line arguments."""
    parser = argparse.ArgumentParser(description=sys.modules[__name__].__doc__)
    return parser.parse_args()


def main() -> None:
    """Program entry point."""
    _ = parse_args()
    check_environment()
    build_ndk()
    generate_docs()


if __name__ == '__main__':
    main()
