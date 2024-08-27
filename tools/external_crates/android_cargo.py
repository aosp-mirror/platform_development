#!/usr/bin/env python3

# Copyright (C) 2024 The Android Open Source Project
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

import os
from pathlib import Path
import sys


def main() -> None:
  prebuilt_bin = (
      (
          Path(__file__).resolve().parents[3]
          / "prebuilts/rust/linux-x86/stable/rust-analyzer"
      )
      .resolve()
      .parents[0]
  )
  os.environ["PATH"] = os.pathsep.join([str(prebuilt_bin), os.environ["PATH"]])
  os.execv(prebuilt_bin / "cargo", sys.argv)


if __name__ == "__main__":
  main()
