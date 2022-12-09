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
"""APIs for interacting with Soong."""
import logging
import os
from pathlib import Path
import shlex
import shutil
import subprocess


def logger() -> logging.Logger:
    """Returns the module level logger."""
    return logging.getLogger(__name__)


class Soong:
    """Interface for interacting with Soong."""

    def __init__(self, build_top: Path, out_dir: Path) -> None:
        self.out_dir = out_dir
        self.soong_ui_path = build_top / "build/soong/soong_ui.bash"

    def soong_ui(
        self,
        args: list[str],
        env: dict[str, str] | None = None,
        capture_output: bool = False,
    ) -> str:
        """Executes soong_ui.bash and returns the output on success.

        Args:
            args: List of string arguments to pass to soong_ui.bash.
            env: Additional environment variables to set when running soong_ui.
            capture_output: True if the output of the command should be captured and
                returned. If not, the output will be printed to stdout/stderr and an
                empty string will be returned.

        Raises:
            subprocess.CalledProcessError: The subprocess failure if the soong command
            failed.

        Returns:
            The interleaved contents of stdout/stderr if capture_output is True, else an
            empty string.
        """
        if env is None:
            env = {}

        # Use a (mostly) clean environment to avoid the caller's lunch
        # environment affecting the build.
        exec_env = {
            # Newer versions of golang require the go cache, which defaults to somewhere
            # in HOME if not set.
            "HOME": os.environ["HOME"],
            "OUT_DIR": str(self.out_dir.resolve()),
            "PATH": os.environ["PATH"],
        }
        exec_env.update(env)
        env_prefix = " ".join(f"{k}={v}" for k, v in exec_env.items())
        cmd = [str(self.soong_ui_path)] + args
        logger().debug(f"running in {os.getcwd()}: {env_prefix} {shlex.join(cmd)}")
        result = subprocess.run(
            cmd,
            check=True,
            capture_output=capture_output,
            encoding="utf-8",
            env=exec_env,
        )
        return result.stdout

    def get_make_var(self, name: str) -> str:
        """Queries the build system for the value of a make variable.

        Args:
            name: The name of the build variable to query.

        Returns:
            The value of the build variable in string form.
        """
        return self.soong_ui(["--dumpvar-mode", name], capture_output=True).rstrip("\n")

    def clean(self) -> None:
        """Removes the output directory, if it exists."""
        if self.out_dir.exists():
            shutil.rmtree(self.out_dir)

    def build(self, targets: list[str], env: dict[str, str] | None = None) -> None:
        """Builds the given targets.

        The out directory will be created if it does not already exist. Existing
        contents will not be removed, but affected outputs will be modified.

        Args:
            targets: A list of target names to build.
            env: Additional environment variables to set when running the build.
        """
        self.out_dir.mkdir(parents=True, exist_ok=True)
        self.soong_ui(["--make-mode", "--soong-only"] + targets, env=env)
