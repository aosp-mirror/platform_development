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

"""Command-related utilities."""

from collections import namedtuple
import logging
import os
import shlex
import subprocess
import sys


CommandResult = namedtuple('CommandResult', 'returncode stdoutdata, stderrdata')
PIPE = subprocess.PIPE

_LOCAL_BIN_PATH = os.path.join(os.path.abspath(os.path.dirname(sys.argv[0])),
                               'bin')


def _update_command_for_local(command, kwargs):
  if kwargs.get('shell', False):
    # do nothing for shell commands
    return

  prog = command[0]
  local_prog = os.path.join(_LOCAL_BIN_PATH, prog)
  if os.path.isfile(local_prog) and os.access(local_prog, os.X_OK):
    logging.debug('Using local executable: %s', local_prog)
    command[0] = local_prog


def run_command(command, read_stdout=False, read_stderr=False,
                log_stdout=False, log_stderr=False,
                raise_on_error=True, sudo=False, **kwargs):
  """Runs a command and returns the results.

    The method tries to use the executable in bin/ firstly.

  Args:
    command: A sequence of command arguments or else a single string.
    read_stdout: If True, includes stdout data in the returned tuple.
      Otherwise includes None in the returned tuple.
    read_stderr: If True, includes stderr data in the returned tuple.
      Otherwise includes None in the returned tuple.
    log_stdout: If True, logs stdout data.
    log_stderr: If True, logs stderro data.
    raise_on_error: If True, raise exception if return code is nonzero.
    sudo: Prepends 'sudo' to command if user is not root.
    **kwargs: the keyword arguments passed to subprocess.Popen().

  Returns:
    A namedtuple CommandResult(returncode, stdoutdata, stderrdata).
    The latter two fields will be set only when read_stdout/read_stderr
    is True, respectively. Otherwise, they will be None.

  Raises:
    OSError: Not such a command to execute, raised by subprocess.Popen().
    subprocess.CalledProcessError: The return code of the command is nonzero.
  """
  _update_command_for_local(command, kwargs)

  if sudo and os.getuid() != 0:
    if kwargs.pop('shell', False):
      command = ['sudo', 'sh', '-c', command]
    else:
      command = ['sudo'] + command

  if read_stdout or log_stdout:
    assert kwargs.get('stdout') in [None, PIPE]
    kwargs['stdout'] = PIPE
  if read_stderr or log_stderr:
    assert kwargs.get('stderr') in [None, PIPE]
    kwargs['stderr'] = PIPE

  need_communicate = (read_stdout or read_stderr or
                      log_stdout or log_stderr)
  proc = subprocess.Popen(command, **kwargs)
  if need_communicate:
    stdout, stderr = proc.communicate()
  else:
    proc.wait()  # no need to communicate; just wait.

  if kwargs.get('shell'):
    command_in_log = command
  else:
    command_in_log = ' '.join(arg for arg in command)
  logging.log(logging.INFO, 'Executed command: %r (ret: %d)',
              command_in_log, proc.returncode)

  log_level = logging.ERROR if proc.returncode != 0 else logging.INFO
  if log_stdout:
    logging.log(log_level, '  stdout: %r', stdout)
  if log_stderr:
    logging.log(log_level, '  stderr: %r', stderr)

  if proc.returncode != 0 and raise_on_error:
    raise subprocess.CalledProcessError(proc.returncode, command)

  return CommandResult(proc.returncode,
                       stdout if read_stdout else None,
                       stderr if read_stderr else None)
