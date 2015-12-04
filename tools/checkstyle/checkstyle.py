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
import subprocess
import sys
import xml.dom.minidom
import gitlint.git as git


MAIN_DIRECTORY = os.path.normpath(os.path.dirname(__file__))
CHECKSTYLE_JAR = os.path.join(MAIN_DIRECTORY, 'checkstyle.jar')
CHECKSTYLE_STYLE = os.path.join(MAIN_DIRECTORY, 'android-style.xml')
FORCED_RULES = ['com.puppycrawl.tools.checkstyle.checks.imports.ImportOrderCheck',
                'com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck']

def RunCheckstyle(java_files):
  if not os.path.exists(CHECKSTYLE_STYLE):
    print 'Java checkstyle configuration file is missing'
    sys.exit(1)

  if java_files:
    # Files to check were specified via command line.
    print 'Running Checkstyle on inputted files'
    sha = ''
    last_commit_modified_files = {}
    java_files = map(os.path.abspath, java_files)
  else:
    # Files where not specified exclicitly. Let's use last commit files.
    sha, last_commit_modified_files = _GetModifiedFiles()
    print 'Running Checkstyle on %s commit' % sha
    java_files = last_commit_modified_files.keys()

    if not java_files:
      print 'No files to check'
      return

  stdout = _ExecuteCheckstyle(java_files)
  (errors, warnings) = _ParseAndFilterOutput(stdout,
                                             sha,
                                             last_commit_modified_files)

  if errors:
    print 'ERRORS:'
    print '\n'.join(errors)
  if warnings:
    print 'WARNINGS:'
    print '\n'.join(warnings)
  if errors or warnings:
    sys.exit(1)

  print 'SUCCESS! NO ISSUES FOUND'
  sys.exit(0)


def _ExecuteCheckstyle(java_files):
  # Run checkstyle
  checkstyle_env = os.environ.copy()
  checkstyle_env['JAVA_CMD'] = 'java'
  try:
    check = subprocess.Popen(['java', '-cp',
                              CHECKSTYLE_JAR,
                              'com.puppycrawl.tools.checkstyle.Main', '-c',
                              CHECKSTYLE_STYLE, '-f', 'xml'] + java_files,
                             stdout=subprocess.PIPE, env=checkstyle_env)
    stdout, _ = check.communicate()
  except OSError as e:
    if e.errno == errno.ENOENT:
      print 'Error running Checkstyle!'
      sys.exit(1)

  # Work-around for Checkstyle printing error count to stdio.
  if 'Checkstyle ends with' in stdout.splitlines()[-1]:
    stdout = '\n'.join(stdout.splitlines()[:-1])
  return stdout

def _ParseAndFilterOutput(stdout, sha, last_commit_modified_files):
  result_errors = []
  result_warnings = []
  root = xml.dom.minidom.parseString(stdout)
  for file_element in root.getElementsByTagName('file'):
    file_name = file_element.attributes['name'].value
    if last_commit_modified_files:
      modified_lines = git.modified_lines(file_name,
                                          last_commit_modified_files[file_name],
                                          sha)
    file_name = os.path.relpath(file_name)
    errors = file_element.getElementsByTagName('error')
    for error in errors:
      line = error.attributes['line'].value
      if last_commit_modified_files and int(line) not in modified_lines:
        if error.attributes['source'].value not in FORCED_RULES:
          continue

      column = ''
      if error.hasAttribute('column'):
        column = '%s:' % (error.attributes['column'].value)
      message = error.attributes['message'].value
      result = '  %s:%s:%s %s' % (file_name, line, column, message)

      severity = error.attributes['severity'].value
      if severity == 'error':
        result_errors.append(result)
      elif severity == 'warning':
        result_warnings.append(result)
  return (result_errors, result_warnings)


def _GetModifiedFiles():
  root = git.repository_root()
  sha = git.last_commit()
  working_files = git.modified_files(root)
  if working_files:
    print 'You need to commit all the files before running Checkstyle'
    sys.exit(1)
  modified_files = git.modified_files(root, True, sha)
  modified_files = {os.path.abspath(f): modified_files[f] for f
                    in modified_files if f.endswith('.java')}
  return (sha, modified_files)


def main(args=None):
  parser = argparse.ArgumentParser()
  parser.add_argument('--file', '-f', nargs='+')
  args = parser.parse_args()
  RunCheckstyle(args.file)

if __name__ == '__main__':
  main()
