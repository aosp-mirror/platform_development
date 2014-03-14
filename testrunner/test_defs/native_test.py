#!/usr/bin/python2.4
#
#
# Copyright 2009, The Android Open Source Project
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

"""TestSuite for running native Android tests."""

# python imports
import os
import re

# local imports
import android_build
import logger
import run_command
import test_suite


class NativeTestSuite(test_suite.AbstractTestSuite):
  """A test suite for running native aka C/C++ tests on device."""

  def Run(self, options, adb):
    """Run the provided *native* test suite.

    The test_suite must contain a build path where the native test
    files are. Subdirectories are automatically scanned as well.

    Each test's name must have a .cc or .cpp extension and match one
    of the following patterns:
      - test_*
      - *_test.[cc|cpp]
      - *_unittest.[cc|cpp]
    A successful test must return 0. Any other value will be considered
    as an error.

    Args:
      options: command line options
      adb: adb interface
    """
    # find all test files, convert unicode names to ascii, take the basename
    # and drop the .cc/.cpp  extension.
    source_list = []
    build_path = os.path.join(android_build.GetTop(), self.GetBuildPath())
    os.path.walk(build_path, self._CollectTestSources, source_list)
    logger.SilentLog("Tests source %s" % source_list)

    # Host tests are under out/host/<os>-<arch>/bin.
    host_list = self._FilterOutMissing(android_build.GetHostBin(), source_list)
    logger.SilentLog("Host tests %s" % host_list)

    # Target tests are under $ANDROID_PRODUCT_OUT/data/nativetest.
    target_list = self._FilterOutMissing(android_build.GetTargetNativeTestPath(),
                                         source_list)
    logger.SilentLog("Target tests %s" % target_list)

    # Run on the host
    logger.Log("\nRunning on host")
    for f in host_list:
      if run_command.RunHostCommand(f) != 0:
        logger.Log("%s... failed" % f)
      else:
        if run_command.HasValgrind():
          if run_command.RunHostCommand(f, valgrind=True) == 0:
            logger.Log("%s... ok\t\t[valgrind: ok]" % f)
          else:
            logger.Log("%s... ok\t\t[valgrind: failed]" % f)
        else:
          logger.Log("%s... ok\t\t[valgrind: missing]" % f)

    # Run on the device
    logger.Log("\nRunning on target")
    for f in target_list:
      full_path = os.path.join(os.sep, "data", "nativetest", f)

      # Single quotes are needed to prevent the shell splitting it.
      output = adb.SendShellCommand("'%s 2>&1;echo -n exit code:$?'" %
                                    "(cd /sdcard;%s)" % full_path,
                                    int(options.timeout))
      success = output.endswith("exit code:0")
      logger.Log("%s... %s" % (f, success and "ok" or "failed"))
      # Print the captured output when the test failed.
      if not success or options.verbose:
        pos = output.rfind("exit code")
        output = output[0:pos]
        logger.Log(output)

      # Cleanup
      adb.SendShellCommand("rm %s" % full_path)

  def _CollectTestSources(self, test_list, dirname, files):
    """For each directory, find tests source file and add them to the list.

    Test files must match one of the following pattern:
      - test_*.[cc|cpp]
      - *_test.[cc|cpp]
      - *_unittest.[cc|cpp]

    This method is a callback for os.path.walk.

    Args:
      test_list: Where new tests should be inserted.
      dirname: Current directory.
      files: List of files in the current directory.
    """
    for f in files:
      (name, ext) = os.path.splitext(f)
      if ext == ".cc" or ext == ".cpp" or ext == ".c":
        if re.search("_test$|_test_$|_unittest$|_unittest_$|^test_", name):
          logger.SilentLog("Found %s" % f)
          test_list.append(str(os.path.join(dirname, f)))

  def _FilterOutMissing(self, path, sources):
    """Filter out from the sources list missing tests.

    Sometimes some test source are not built for the target, i.e there
    is no binary corresponding to the source file. We need to filter
    these out.

    Args:
      path: Where the binaries should be.
      sources: List of tests source path.
    Returns:
      A list of relative paths to the test binaries built from the sources.
    """
    binaries = []
    for f in sources:
      binary = os.path.basename(f)
      binary = os.path.splitext(binary)[0]
      found = self._FindFileRecursively(path, binary)
      if found:
        binary = os.path.relpath(os.path.abspath(found),
                                 os.path.abspath(path))
        binaries.append(binary)
    return binaries

  def _FindFileRecursively(self, path, match):
    """Finds the first executable binary in a given path that matches the name.

    Args:
      path: Where to search for binaries. Can be nested directories.
      binary: Which binary to search for.
    Returns:
      first matched file in the path or None if none is found.
    """
    for root, dirs, files in os.walk(path):
      for f in files:
        if f == match:
          return os.path.join(root, f)
      for d in dirs:
        found = self._FindFileRecursively(os.path.join(root, d), match)
        if found:
          return found
    return None

  def _RunHostCommand(self, binary, valgrind=False):
    """Run a command on the host (opt using valgrind).

    Runs the host binary and returns the exit code.
    If successfull, the output (stdout and stderr) are discarded,
    but printed in case of error.
    The command can be run under valgrind in which case all the
    output are always discarded.

    Args:
      binary: basename of the file to be run. It is expected to be under
            $ANDROID_HOST_OUT/bin.
      valgrind: If True the command will be run under valgrind.

    Returns:
      The command exit code (int)
    """
    full_path = os.path.join(android_build.GetHostBin(), binary)
    return run_command.RunHostCommand(full_path, valgrind=valgrind)
