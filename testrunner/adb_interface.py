#!/usr/bin/python2.4
#
#
# Copyright 2008, The Android Open Source Project
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

"""Provides an interface to communicate with the device via the adb command.

Assumes adb binary is currently on system path.
"""
# Python imports
import os
import string
import time

# local imports
import am_instrument_parser
import errors
import logger
import run_command


class AdbInterface:
  """Helper class for communicating with Android device via adb."""

  # argument to pass to adb, to direct command to specific device
  _target_arg = ""

  DEVICE_TRACE_DIR = "/data/test_results/"

  def SetEmulatorTarget(self):
    """Direct all future commands to the only running emulator."""
    self._target_arg = "-e"

  def SetDeviceTarget(self):
    """Direct all future commands to the only connected USB device."""
    self._target_arg = "-d"

  def SetTargetSerial(self, serial):
    """Direct all future commands to Android target with the given serial."""
    self._target_arg = "-s %s" % serial

  def SendCommand(self, command_string, timeout_time=20, retry_count=3):
    """Send a command via adb.

    Args:
      command_string: adb command to run
      timeout_time: number of seconds to wait for command to respond before
        retrying
      retry_count: number of times to retry command before raising
        WaitForResponseTimedOutError
    Returns:
      string output of command

    Raises:
      WaitForResponseTimedOutError if device does not respond to command within time
    """
    adb_cmd = "adb %s %s" % (self._target_arg, command_string)
    logger.SilentLog("about to run %s" % adb_cmd)
    return run_command.RunCommand(adb_cmd, timeout_time=timeout_time,
                                  retry_count=retry_count)

  def SendShellCommand(self, cmd, timeout_time=20, retry_count=3):
    """Send a adb shell command.

    Args:
      cmd: adb shell command to run
      timeout_time: number of seconds to wait for command to respond before
        retrying
      retry_count: number of times to retry command before raising
        WaitForResponseTimedOutError

    Returns:
      string output of command

    Raises:
      WaitForResponseTimedOutError: if device does not respond to command
    """
    return self.SendCommand("shell %s" % cmd, timeout_time=timeout_time,
                            retry_count=retry_count)

  def BugReport(self, path):
    """Dumps adb bugreport to the file specified by the path.

    Args:
      path: Path of the file where adb bugreport is dumped to.
    """
    bug_output = self.SendShellCommand("bugreport", timeout_time=60)
    bugreport_file = open(path, "w")
    bugreport_file.write(bug_output)
    bugreport_file.close()

  def Push(self, src, dest):
    """Pushes the file src onto the device at dest.

    Args:
      src: file path of host file to push
      dest: destination absolute file path on device
    """
    self.SendCommand("push %s %s" % (src, dest), timeout_time=60)

  def Pull(self, src, dest):
    """Pulls the file src on the device onto dest on the host.

    Args:
      src: absolute file path of file on device to pull
      dest: destination file path on host

    Returns:
      True if success and False otherwise.
    """
    # Create the base dir if it doesn't exist already
    if not os.path.exists(os.path.dirname(dest)):
      os.makedirs(os.path.dirname(dest))

    if self.DoesFileExist(src):
      self.SendCommand("pull %s %s" % (src, dest), timeout_time=60)
      return True
    else:
      logger.Log("ADB Pull Failed: Source file %s does not exist." % src)
      return False

  def DoesFileExist(self, src):
    """Checks if the given path exists on device target.

    Args:
      src: file path to be checked.

    Returns:
      True if file exists
    """

    output = self.SendShellCommand("ls %s" % src)
    error = "No such file or directory"

    if error in output:
      return False
    return True

  def StartInstrumentationForPackage(
      self, package_name, runner_name, timeout_time=60*10,
      no_window_animation=False, instrumentation_args={}):
    """Run instrumentation test for given package and runner.

    Equivalent to StartInstrumentation, except instrumentation path is
    separated into its package and runner components.
    """
    instrumentation_path = "%s/%s" % (package_name, runner_name)
    return self.StartInstrumentation(instrumentation_path, timeout_time=timeout_time,
                                     no_window_animation=no_window_animation,
                                     instrumentation_args=instrumentation_args)

  def StartInstrumentation(
      self, instrumentation_path, timeout_time=60*10, no_window_animation=False,
      profile=False, instrumentation_args={}):

    """Runs an instrumentation class on the target.

    Returns a dictionary containing the key value pairs from the
    instrumentations result bundle and a list of TestResults. Also handles the
    interpreting of error output from the device and raises the necessary
    exceptions.

    Args:
      instrumentation_path: string. It should be the fully classified package
      name, and instrumentation test runner, separated by "/"
        e.g. com.android.globaltimelaunch/.GlobalTimeLaunch
      timeout_time: Timeout value for the am command.
      no_window_animation: boolean, Whether you want window animations enabled
        or disabled
      profile: If True, profiling will be turned on for the instrumentation.
      instrumentation_args: Dictionary of key value bundle arguments to pass to
      instrumentation.

    Returns:
      (test_results, inst_finished_bundle)

      test_results: a list of TestResults
      inst_finished_bundle (dict): Key/value pairs contained in the bundle that
        is passed into ActivityManager.finishInstrumentation(). Included in this
        bundle is the return code of the Instrumentation process, any error
        codes reported by the activity manager, and any results explicitly added
        by the instrumentation code.

     Raises:
       WaitForResponseTimedOutError: if timeout occurred while waiting for
         response to adb instrument command
       DeviceUnresponsiveError: if device system process is not responding
       InstrumentationError: if instrumentation failed to run
    """

    command_string = self._BuildInstrumentationCommandPath(
        instrumentation_path, no_window_animation=no_window_animation,
        profile=profile, raw_mode=True,
        instrumentation_args=instrumentation_args)
    logger.Log(command_string)
    (test_results, inst_finished_bundle) = (
        am_instrument_parser.ParseAmInstrumentOutput(
            self.SendShellCommand(command_string, timeout_time=timeout_time,
                                  retry_count=2)))

    if "code" not in inst_finished_bundle:
      raise errors.InstrumentationError("no test results... device setup "
                                        "correctly?")

    if inst_finished_bundle["code"] == "0":
      short_msg_result = "no error message"
      if "shortMsg" in inst_finished_bundle:
        short_msg_result = inst_finished_bundle["shortMsg"]
        logger.Log("Error! Test run failed: %s" % short_msg_result)
      raise errors.InstrumentationError(short_msg_result)

    if "INSTRUMENTATION_ABORTED" in inst_finished_bundle:
      logger.Log("INSTRUMENTATION ABORTED!")
      raise errors.DeviceUnresponsiveError

    return (test_results, inst_finished_bundle)

  def StartInstrumentationNoResults(
      self, package_name, runner_name, no_window_animation=False,
      raw_mode=False, instrumentation_args={}):
    """Runs instrumentation and dumps output to stdout.

    Equivalent to StartInstrumentation, but will dump instrumentation
    'normal' output to stdout, instead of parsing return results. Command will
    never timeout.
    """
    adb_command_string = self.PreviewInstrumentationCommand(
        package_name, runner_name, no_window_animation=no_window_animation,
        raw_mode=raw_mode, instrumentation_args=instrumentation_args)
    logger.Log(adb_command_string)
    run_command.RunCommand(adb_command_string, return_output=False)

  def PreviewInstrumentationCommand(
      self, package_name, runner_name, no_window_animation=False,
      raw_mode=False, instrumentation_args={}):
    """Returns a string of adb command that will be executed."""
    inst_command_string = self._BuildInstrumentationCommand(
        package_name, runner_name, no_window_animation=no_window_animation,
        raw_mode=raw_mode, instrumentation_args=instrumentation_args)
    command_string = "adb %s shell %s" % (self._target_arg, inst_command_string)
    return command_string

  def _BuildInstrumentationCommand(
      self, package, runner_name, no_window_animation=False, profile=False,
      raw_mode=True, instrumentation_args={}):
    instrumentation_path = "%s/%s" % (package, runner_name)

    return self._BuildInstrumentationCommandPath(
        instrumentation_path, no_window_animation=no_window_animation,
        profile=profile, raw_mode=raw_mode,
        instrumentation_args=instrumentation_args)

  def _BuildInstrumentationCommandPath(
      self, instrumentation_path, no_window_animation=False, profile=False,
      raw_mode=True, instrumentation_args={}):
    command_string = "am instrument"
    if no_window_animation:
      command_string += " --no_window_animation"
    if profile:
      self._CreateTraceDir()
      command_string += (
          " -p %s/%s.dmtrace" %
          (self.DEVICE_TRACE_DIR, instrumentation_path.split(".")[-1]))

    for key, value in instrumentation_args.items():
      command_string += " -e %s '%s'" % (key, value)
    if raw_mode:
      command_string += " -r"
    command_string += " -w %s" % instrumentation_path
    return command_string

  def _CreateTraceDir(self):
    ls_response = self.SendShellCommand("ls /data/trace")
    if ls_response.strip("#").strip(string.whitespace) != "":
      self.SendShellCommand("create /data/trace", "mkdir /data/trace")
      self.SendShellCommand("make /data/trace world writeable",
                            "chmod 777 /data/trace")

  def WaitForDevicePm(self, wait_time=120):
    """Waits for targeted device's package manager to be up.

    Args:
      wait_time: time in seconds to wait

    Raises:
      WaitForResponseTimedOutError if wait_time elapses and pm still does not
      respond.
    """
    logger.Log("Waiting for device package manager...")
    self.SendCommand("wait-for-device")
    # Now the device is there, but may not be running.
    # Query the package manager with a basic command
    pm_found = False
    attempts = 0
    wait_period = 5
    while not pm_found and (attempts*wait_period) < wait_time:
      # assume the 'adb shell pm path android' command will always
      # return 'package: something' in the success case
      output = self.SendShellCommand("pm path android", retry_count=1)
      if "package:" in output:
        pm_found = True
      else:
        time.sleep(wait_period)
        attempts += 1
    if not pm_found:
      raise errors.WaitForResponseTimedOutError(
          "Package manager did not respond after %s seconds" % wait_time)

  def WaitForInstrumentation(self, package_name, runner_name, wait_time=120):
    """Waits for given instrumentation to be present on device

    Args:
      wait_time: time in seconds to wait

    Raises:
      WaitForResponseTimedOutError if wait_time elapses and instrumentation
      still not present.
    """
    instrumentation_path = "%s/%s" % (package_name, runner_name)
    logger.Log("Waiting for instrumentation to be present")
    # Query the package manager
    inst_found = False
    attempts = 0
    wait_period = 5
    while not inst_found and (attempts*wait_period) < wait_time:
      # assume the 'adb shell pm list instrumentation'
      # return 'instrumentation: something' in the success case
      try:
        output = self.SendShellCommand("pm list instrumentation | grep %s"
                                       % instrumentation_path, retry_count=1)
        if "instrumentation:" in output:
          inst_found = True
      except errors.AbortError, e:
        # ignore
        pass
      if not inst_found:
        time.sleep(wait_period)
        attempts += 1
    if not inst_found:
      logger.Log(
          "Could not find instrumentation %s on device. Does the "
          "instrumentation in test's AndroidManifest.xml match definition"
          "in test_defs.xml?" % instrumentation_path)
      raise errors.WaitForResponseTimedOutError()

  def Sync(self, retry_count=3):
    """Perform a adb sync.

    Blocks until device package manager is responding.

    Args:
      retry_count: number of times to retry sync before failing

    Raises:
      WaitForResponseTimedOutError if package manager does not respond
      AbortError if unrecoverable error occurred
    """
    output = ""
    error = None
    try:
      output = self.SendCommand("sync", retry_count=retry_count)
    except errors.AbortError, e:
      error = e
      output = e.msg
    if "Read-only file system" in output:
      logger.SilentLog(output)
      logger.Log("Remounting read-only filesystem")
      self.SendCommand("remount")
      output = self.SendCommand("sync", retry_count=retry_count)
    elif "No space left on device" in output:
      logger.SilentLog(output)
      logger.Log("Restarting device runtime")
      self.SendShellCommand("stop", retry_count=retry_count)
      output = self.SendCommand("sync", retry_count=retry_count)
      self.SendShellCommand("start", retry_count=retry_count)
    elif error is not None:
      # exception occurred that cannot be recovered from
      raise error
    logger.SilentLog(output)
    self.WaitForDevicePm()
    return output

  def GetSerialNumber(self):
    """Returns the serial number of the targeted device."""
    return self.SendCommand("get-serialno").strip()

