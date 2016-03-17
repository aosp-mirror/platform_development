#!/usr/bin/python

#
# Copyright 2015, The Android Open Source Project
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
#

"""Script that is used by developers to run style checks on Java files."""

import argparse
import errno
import os
import shutil
import subprocess
import sys
import tempfile
import xml.dom.minidom
import gitlint.git as git


MAIN_DIRECTORY = os.path.normpath(os.path.dirname(__file__))
CHECKSTYLE_JAR = os.path.join(MAIN_DIRECTORY, 'checkstyle.jar')
CHECKSTYLE_STYLE = os.path.join(MAIN_DIRECTORY, 'android-style.xml')
FORCED_RULES = ['com.puppycrawl.tools.checkstyle.checks.imports.ImportOrderCheck',
                'com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck']
SKIPPED_RULES_FOR_TEST_FILES = ['com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocTypeCheck']
SUBPATH_FOR_TEST_FILES = ['/tests/java/', '/tests/src/']
ERROR_UNCOMMITTED = 'You need to commit all modified files before running Checkstyle\n'
ERROR_UNTRACKED = 'You have untracked java files that are not being checked:\n'


def RunCheckstyleOnFiles(java_files, config_xml=CHECKSTYLE_STYLE):
  """Runs Checkstyle checks on a given set of java_files.

  Args:
    java_files: A list of files to check.
    config_xml: Path of the checkstyle XML configuration file.

  Returns:
    A tuple of errors and warnings.
  """
  print 'Running Checkstyle on inputted files'
  java_files = map(os.path.abspath, java_files)
  stdout = _ExecuteCheckstyle(java_files, config_xml)
  (errors, warnings) = _ParseAndFilterOutput(stdout)
  _PrintErrorsAndWarnings(errors, warnings)
  return errors, warnings


def RunCheckstyleOnACommit(commit, config_xml=CHECKSTYLE_STYLE):
  """Runs Checkstyle checks on a given commit.

  It will run Checkstyle on the changed Java files in a specified commit SHA-1
  and if that is None it will fallback to check the latest commit of the
  currently checked out branch.

  Args:
    commit: A full 40 character SHA-1 of a commit to check.
    config_xml: Path of the checkstyle XML configuration file.

  Returns:
    A tuple of errors and warnings.
  """
  if not commit:
    _WarnIfUntrackedFiles()
    commit = git.last_commit()
  print 'Running Checkstyle on %s commit' % commit
  commit_modified_files = _GetModifiedFiles(commit)
  if not commit_modified_files.keys():
    print 'No Java files to check'
    return [], []

  (tmp_dir, tmp_file_map) = _GetTempFilesForCommit(
      commit_modified_files.keys(), commit)

  java_files = tmp_file_map.keys()
  stdout = _ExecuteCheckstyle(java_files, config_xml)

  # Remove all the temporary files.
  shutil.rmtree(tmp_dir)

  (errors, warnings) = _ParseAndFilterOutput(stdout,
                                             commit,
                                             commit_modified_files,
                                             tmp_file_map)
  _PrintErrorsAndWarnings(errors, warnings)
  return errors, warnings


def _WarnIfUntrackedFiles(out=sys.stdout):
  """Prints a warning and a list of untracked files if needed."""
  root = git.repository_root()
  untracked_files = git.modified_files(root, False)
  untracked_files = {f for f in untracked_files if f.endswith('.java')}
  if untracked_files:
    out.write(ERROR_UNTRACKED)
    for untracked_file in untracked_files:
      out.write(untracked_file + '\n')
    out.write('\n')


def _PrintErrorsAndWarnings(errors, warnings):
  """Prints given errors and warnings."""
  if errors:
    print 'ERRORS:'
    print '\n'.join(errors)
  if warnings:
    print 'WARNINGS:'
    print '\n'.join(warnings)


def _ExecuteCheckstyle(java_files, config_xml):
  """Runs Checkstyle to check give Java files for style errors.

  Args:
    java_files: A list of Java files that needs to be checked.
    config_xml: Path of the checkstyle XML configuration file.

  Returns:
    Checkstyle output in XML format.
  """
  # Run checkstyle
  checkstyle_env = os.environ.copy()
  checkstyle_env['JAVA_CMD'] = 'java'
  try:
    check = subprocess.Popen(['java', '-cp',
                              CHECKSTYLE_JAR,
                              'com.puppycrawl.tools.checkstyle.Main', '-c',
                              config_xml, '-f', 'xml'] + java_files,
                             stdout=subprocess.PIPE, env=checkstyle_env)
    stdout, _ = check.communicate()
  except OSError as e:
    if e.errno == errno.ENOENT:
      print 'Error running Checkstyle!'
      sys.exit(1)

  # A work-around for Checkstyle printing error count to stdio.
  if 'Checkstyle ends with' in stdout.splitlines()[-1]:
    stdout = '\n'.join(stdout.splitlines()[:-1])
  return stdout


def _ParseAndFilterOutput(stdout,
                          sha=None,
                          commit_modified_files=None,
                          tmp_file_map=None):
  result_errors = []
  result_warnings = []
  root = xml.dom.minidom.parseString(stdout)
  for file_element in root.getElementsByTagName('file'):
    file_name = file_element.attributes['name'].value
    if tmp_file_map:
      file_name = tmp_file_map[file_name]
    modified_lines = None
    if commit_modified_files:
      modified_lines = git.modified_lines(file_name,
                                          commit_modified_files[file_name],
                                          sha)
    test_class = any(substring in file_name for substring
                     in SUBPATH_FOR_TEST_FILES)
    file_name = os.path.relpath(file_name)
    errors = file_element.getElementsByTagName('error')
    for error in errors:
      line = int(error.attributes['line'].value)
      rule = error.attributes['source'].value
      if _ShouldSkip(commit_modified_files, modified_lines, line, rule,
                     test_class):
        continue

      column = ''
      if error.hasAttribute('column'):
        column = '%s:' % error.attributes['column'].value
      message = error.attributes['message'].value
      result = '  %s:%s:%s %s' % (file_name, line, column, message)

      severity = error.attributes['severity'].value
      if severity == 'error':
        result_errors.append(result)
      elif severity == 'warning':
        result_warnings.append(result)
  return result_errors, result_warnings


def _ShouldSkip(commit_check, modified_lines, line, rule, test_class=False):
  """Returns whether an error on a given line should be skipped.

  Args:
    commit_check: Whether Checkstyle is being run on a specific commit.
    modified_lines: A list of lines that has been modified.
    line: The line that has a rule violation.
    rule: The type of rule that a given line is violating.
    test_class: Whether the file being checked is a test class.

  Returns:
    A boolean whether a given line should be skipped in the reporting.
  """
  # None modified_lines means checked file is new and nothing should be skipped.
  if test_class and rule in SKIPPED_RULES_FOR_TEST_FILES:
    return True
  if not commit_check:
    return False
  if modified_lines is None:
    return False
  return line not in modified_lines and rule not in FORCED_RULES


def _GetModifiedFiles(commit, out=sys.stdout):
  root = git.repository_root()
  pending_files = git.modified_files(root, True)
  if pending_files:
    out.write(ERROR_UNCOMMITTED)
    sys.exit(1)

  modified_files = git.modified_files(root, True, commit)
  modified_files = {f: modified_files[f] for f
                    in modified_files if f.endswith('.java')}
  return modified_files


def _GetTempFilesForCommit(file_names, commit):
  """Creates a temporary snapshot of the files in at a commit.

  Retrieves the state of every file in file_names at a given commit and writes
  them all out to a temporary directory.

  Args:
    file_names: A list of files that need to be retrieved.
    commit: A full 40 character SHA-1 of a commit.

  Returns:
    A tuple of temprorary directory name and a directionary of
    temp_file_name: filename. For example:

    ('/tmp/random/', {'/tmp/random/blarg.java': 'real/path/to/file.java' }
  """
  tmp_dir_name = tempfile.mkdtemp()
  tmp_file_names = {}
  for file_name in file_names:
    rel_path = os.path.relpath(file_name)
    content = subprocess.check_output(
        ['git', 'show', commit + ':' + rel_path])

    tmp_file_name = os.path.join(tmp_dir_name, rel_path)
    # create directory for the file if it doesn't exist
    if not os.path.exists(os.path.dirname(tmp_file_name)):
      os.makedirs(os.path.dirname(tmp_file_name))

    tmp_file = open(tmp_file_name, 'w')
    tmp_file.write(content)
    tmp_file.close()
    tmp_file_names[tmp_file_name] = file_name
  return tmp_dir_name, tmp_file_names


def main(args=None):
  """Runs Checkstyle checks on a given set of java files or a commit.

  It will run Checkstyle on the list of java files first, if unspecified,
  then the check will be run on a specified commit SHA-1 and if that
  is None it will fallback to check the latest commit of the currently checked
  out branch.
  """
  parser = argparse.ArgumentParser()
  parser.add_argument('--file', '-f', nargs='+')
  parser.add_argument('--sha', '-s')
  parser.add_argument('--config_xml', '-c')
  args = parser.parse_args()

  config_xml = args.config_xml or CHECKSTYLE_STYLE
  if not os.path.exists(config_xml):
    print 'Java checkstyle configuration file is missing'
    sys.exit(1)

  if args.file:
    # Files to check were specified via command line.
    (errors, warnings) = RunCheckstyleOnFiles(args.file, config_xml)
  else:
    (errors, warnings) = RunCheckstyleOnACommit(args.sha, config_xml)

  if errors or warnings:
    sys.exit(1)

  print 'SUCCESS! NO ISSUES FOUND'
  sys.exit(0)


if __name__ == '__main__':
  main()
