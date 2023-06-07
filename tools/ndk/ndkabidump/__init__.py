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
#
"""Tool for updating the prebuilt NDK ABI dumps."""
import argparse
import logging
from pathlib import Path
import shutil
import sys

from .soong import Soong


def logger() -> logging.Logger:
    """Returns the module level logger."""
    return logging.getLogger(__name__)


class Updater:
    """Tool for updating prebuilt NDK ABI dumps."""

    def __init__(self, src_dir: Path, build_dir: Path) -> None:
        self.src_dir = src_dir
        self.build_dir = build_dir

    def build_abi_dumps(self) -> None:
        """Builds the updated NDK ABI dumps."""
        soong = Soong(self.src_dir, self.build_dir)
        logger().info(f"Building ABI dumps to {self.build_dir}")
        soong.build(
            ["dump-ndk-abi"],
            env={
                "TARGET_PRODUCT": "ndk",
                # TODO: remove ALLOW_MISSING_DEPENDENCIES=true when all the
                # riscv64 dependencies exist (currently blocked by
                # http://b/273792258).
                "ALLOW_MISSING_DEPENDENCIES": "true",
                # TODO: remove BUILD_BROKEN_DISABLE_BAZEL=1 when bazel supports
                # riscv64 (http://b/262192655).
                "BUILD_BROKEN_DISABLE_BAZEL": "1",
            },
        )

    def copy_updated_abi_dumps(self) -> None:
        """Copies the NDK ABI dumps from the build directory to prebuilts."""
        prebuilts_project = self.src_dir / "prebuilts/abi-dumps"
        prebuilts_dir = prebuilts_project / "ndk"
        abi_out = self.build_dir / "soong/abi-dumps/ndk"
        for dump in abi_out.glob("**/abi.xml"):
            install_path = prebuilts_dir / dump.relative_to(abi_out)
            install_dir = install_path.parent
            if not install_dir.exists():
                install_dir.mkdir(parents=True)
            logger().info(f"Copying ABI dump {dump} to {install_path}")
            shutil.copy2(dump, install_path)

    def run(self) -> None:
        """Runs the updater.

        Cleans the out directory, builds the ABI dumps, and copies the results
        to the prebuilts directory.
        """
        self.build_abi_dumps()
        self.copy_updated_abi_dumps()


HELP = """\
Builds and updates the NDK ABI prebuilts.

Whenever a change is made that alters the NDK ABI (or an API level is
finalized, or a new preview codename is introduced to the build), the prebuilts
in prebuilts/abi-dumps/ndk need to be updated to match. For any finalized APIs,
the breaking change typically needs to be reverted.

Note that typically this tool should be executed via
development/tools/ndk/update_ndk_abi.sh. That script will ensure that this tool
is up-to-date and run with the correct arguments.
"""


class App:
    """Command line application from updating NDK ABI prebuilts."""

    @staticmethod
    def parse_args() -> argparse.Namespace:
        """Parses and returns command line arguments."""

        parser = argparse.ArgumentParser(
            formatter_class=argparse.RawDescriptionHelpFormatter, description=HELP
        )

        def resolved_path(path: str) -> Path:
            """Converts a string into a fully resolved Path."""
            return Path(path).resolve()

        parser.add_argument(
            "--src-dir",
            type=resolved_path,
            required=True,
            help="Path to the top of the Android source tree.",
        )

        parser.add_argument(
            "out_dir",
            type=resolved_path,
            metavar="OUT_DIR",
            help="Output directory to use for building ABI dumps.",
        )

        parser.add_argument(
            "-v",
            "--verbose",
            action="count",
            default=0,
            help="Increase logging verbosity.",
        )

        return parser.parse_args()

    def run(self) -> None:
        """Builds the new NDK ABI dumps and copies them to prebuilts."""
        args = self.parse_args()
        log_level = logging.DEBUG if args.verbose else logging.INFO
        logging.basicConfig(level=log_level)
        test_path = args.src_dir / "build/soong/soong_ui.bash"
        if not test_path.exists():
            sys.exit(
                f"Source directory {args.src_dir} does not appear to be an "
                f"Android source tree: {test_path} does not exist."
            )

        Updater(args.src_dir, args.out_dir).run()
