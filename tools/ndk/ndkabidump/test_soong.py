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
"""Tests for Soong APIs."""
import contextlib
import os
from pathlib import Path
import tempfile
import unittest
import unittest.mock

from .soong import Soong


if "ANDROID_BUILD_TOP" not in os.environ:
    raise RuntimeError(
        "Cannot run Soong tests without ANDROID_BUILD_TOP defined. Run lunch."
    )


ANDROID_BUILD_TOP = Path(os.environ["ANDROID_BUILD_TOP"]).resolve()


class SoongTest(unittest.TestCase):
    """Tests for the Soong executor."""

    out_dir: Path

    def setUp(self) -> None:
        with contextlib.ExitStack() as stack:
            self.out_dir = Path(stack.enter_context(tempfile.TemporaryDirectory()))
            self.addCleanup(stack.pop_all().close)

    def test_finds_soong_ui(self) -> None:
        """Tests that soong_ui.bash is found correctly."""
        soong = Soong(ANDROID_BUILD_TOP, self.out_dir)
        self.assertTrue(soong.soong_ui_path.exists())
        self.assertEqual("soong_ui.bash", soong.soong_ui_path.name)

    def test_get_build_var(self) -> None:
        """Tests that we can read build variables from Soong."""
        soong = Soong(ANDROID_BUILD_TOP, self.out_dir)
        old_product = os.environ["TARGET_PRODUCT"]
        try:
            # Clear the lunched target out of the test environment for a
            # consistent result.
            del os.environ["TARGET_PRODUCT"]
            self.assertEqual("generic", soong.get_make_var("TARGET_DEVICE"))
        finally:
            os.environ["TARGET_PRODUCT"] = old_product

    def test_clean(self) -> None:
        """Tests that clean works."""
        soong = Soong(ANDROID_BUILD_TOP, self.out_dir)
        self.assertTrue(self.out_dir.exists())
        soong.clean()
        self.assertFalse(self.out_dir.exists())
        soong.clean()
        self.assertFalse(self.out_dir.exists())

    def test_build(self) -> None:
        """Tests that build invokes the correct command.

        Does not actually test a build, as there aren't any good options for short
        builds in the tree, so instead tests that soong_ui is called the way we expect
        it to be.
        """
        soong = Soong(ANDROID_BUILD_TOP, self.out_dir)
        with unittest.mock.patch.object(soong, "soong_ui") as soong_ui:
            soong.build(["foo"])
            soong_ui.assert_called_with(
                ["--make-mode", "--soong-only", "foo"], env=None
            )

    def test_build_creates_out_dir(self) -> None:
        """Tests that build creates the out directory if necessary."""
        soong = Soong(ANDROID_BUILD_TOP, self.out_dir)
        soong.clean()
        with unittest.mock.patch.object(soong, "soong_ui"):
            soong.build([])
        self.assertTrue(self.out_dir.exists())
