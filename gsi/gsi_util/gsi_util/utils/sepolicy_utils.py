# Copyright 2018 - The Android Open Source Project
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

"""SEPolicy-related commands."""

from gsi_util.utils import cmd_utils


def secilc(options, files):
  """Invokes SELinux Common Intermediate Language (CIL) Compiler.

  Args:
    options: A dict of the options passed to 'secilc'.
      e.g., dict(mls='true', multiple-decls=None, policyvers=30) ==>
        '--mls true --multiple-decls --policyvers 30'.
      e.g., dict(M='true', m=None, c=30) ==> '-M true -m -c 30'.
    files: CIL files passed to 'secilc'.

  Returns:
    A tuple of (result_ok, stderr).

  $ secilc --help
  Usage: secilc [OPTION]... FILE...

  Options:
  -o, --output=<file>            write binary policy to <file>
                                 (default: policy.<version>)
  -f, --filecontext=<file>       write file contexts to <file>
                                 (default: file_contexts)
  -t, --target=<type>            specify target architecture. may be selinux or
                                 xen. (default: selinux)
  -M, --mls true|false           build an mls policy. Must be true or false.
                                 This will override the (mls boolean) statement
                                 if present in the policy
  -c, --policyvers=<version>     build a binary policy with a given <version>
                                 (default: 31)
  -U, --handle-unknown=<action>  how to handle unknown classes or permissions.
                                 may be deny, allow, or reject. (default: deny)
                                 This will override the (handleunknown action)
                                 statement if present in the policy
  -D, --disable-dontaudit        do not add dontaudit rules to the binary policy
  -P, --preserve-tunables        treat tunables as booleans
  -m, --multiple-decls           allow some statements to be re-declared
  -N, --disable-neverallow       do not check neverallow rules
  -G, --expand-generated         Expand and remove auto-generated attributes
  -X, --expand-size <SIZE>       Expand type attributes with fewer than <SIZE>
                                 members.
  -v, --verbose                  increment verbosity level
  -h, --help                     display usage information
  """

  cmd = ['secilc']
  for option in options:
    # For short options. e.g., '-m', '-c 30'.
    if len(option) == 1:
      cmd.append('-' + option)
    else:  # For long options. e.g., '--multiple-decls', '--policyvers 30'.
      cmd.append('--' + option)
    # Some option doesn't need value. e.g., -m, -G.
    if options[option] is not None:
      cmd.append(options[option])

  # Adding CIL files.
  cmd.extend(files)

  # Uses 'log_stdout' and 'log_stderr' to disable output.
  returncode, _, stderrdata = cmd_utils.run_command(cmd,
                                                    raise_on_error=False,
                                                    log_stdout=True,
                                                    log_stderr=True,
                                                    read_stderr=True)
  return (returncode == 0, stderrdata)
