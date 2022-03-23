# Copyright 2017 - The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""File-related utilities."""

import contextlib
import os
import tempfile


@contextlib.contextmanager
def UnopenedTemporaryFile(**kwargs):
  """Creates and returns a unopened temprary file path.

  This function is similar to tempfile.TemporaryFile, except that an
  unopened file path is returend instead of a file-like object.
  The file will be deleted when the context manager is closed.

  Args:
    **kwargs: Any keyward arguments passed to tempfile.mkstemp (e.g., dir,
      prefix and suffix).

  Returns:
    An unopened temporary file path.
  """
  fd, path = tempfile.mkstemp(**kwargs)
  os.close(fd)

  try:
    yield path
  finally:
    if os.path.exists(path):
      os.unlink(path)
