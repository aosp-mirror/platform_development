#!/usr/bin/python
"""Diff a repo (downstream) and its upstream.

This script:
  1. Downloads a repo source tree with specified manifest URL, branch
     and release tag.
  2. Retrieves the BUILD_ID from $downstream/build/core/build_id.mk.
  3. Downloads the upstream using the BUILD_ID.
  4. Diffs each project in these two repos.
"""

import argparse
import datetime
import os
import subprocess
import repo_diff_trees

HELP_MSG = "Diff a repo (downstream) and its upstream"

DOWNSTREAM_WORKSPACE = "downstream"
UPSTREAM_WORKSPACE = "upstream"

DEFAULT_MANIFEST_URL = "https://android.googlesource.com/platform/manifest"
DEFAULT_MANIFEST_BRANCH = "android-8.0.0_r10"
DEFAULT_UPSTREAM_MANIFEST_URL = "https://android.googlesource.com/platform/manifest"
DEFAULT_UPSTREAM_MANIFEST_BRANCH = "android-8.0.0_r1"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_EXCLUSIONS_FILE = os.path.join(SCRIPT_DIR, "android_exclusions.txt")


def parse_args():
  """Parse args."""

  parser = argparse.ArgumentParser(description=HELP_MSG)

  parser.add_argument("-u", "--manifest-url",
                      help="manifest url",
                      default=DEFAULT_MANIFEST_URL)
  parser.add_argument("-b", "--manifest-branch",
                      help="manifest branch",
                      default=DEFAULT_MANIFEST_BRANCH)
  parser.add_argument("-r", "--upstream-manifest-url",
                      help="upstream manifest url",
                      default=DEFAULT_UPSTREAM_MANIFEST_URL)
  parser.add_argument("-a", "--upstream-manifest-branch",
                      help="upstream manifest branch",
                      default=DEFAULT_UPSTREAM_MANIFEST_BRANCH)
  parser.add_argument("-e", "--exclusions-file",
                      help="exclusions file",
                      default=DEFAULT_EXCLUSIONS_FILE)
  parser.add_argument("-t", "--tag",
                      help="release tag (optional). If not set then will "
                      "sync the latest in the branch.")
  parser.add_argument("-i", "--ignore_error_during_sync",
                      action="store_true",
                      help="repo sync might fail due to varios reasons. "
                      "Ignore these errors and move on. Use with caution.")

  return parser.parse_args()


def repo_init(url, rev, workspace):
  """Repo init with specific url and rev.

  Args:
    url: manifest url
    rev: manifest branch, or rev
    workspace: the folder to init and sync code
  """

  print("repo init:\n  url: %s\n  rev: %s\n  workspace: %s" %
        (url, rev, workspace))

  subprocess.check_output("repo init --manifest-url=%s --manifest-branch=%s" %
                          (url, rev), cwd=workspace, shell=True)


def repo_sync(workspace, ignore_error, retry=5):
  """Repo sync."""

  count = 0
  while count < retry:
    count += 1
    print("repo sync (retry=%d/%d):\n  workspace: %s" %
          (count, retry, workspace))

    try:
      command = "repo sync --jobs=24 --current-branch --quiet"
      command += " --no-tags --no-clone-bundle"
      if ignore_error:
        command += " --force-broken"
      subprocess.check_output(command, cwd=workspace, shell=True)
    except subprocess.CalledProcessError as e:
      print "Error: %s" % e.output
      if count == retry and not ignore_error:
        raise e
    # Stop retrying if the repo sync was successful
    else:
      break


def get_commit_with_keyword(project_path, keyword):
  """Get the latest commit in $project_path with the specific keyword."""

  return subprocess.check_output(("git -C %s "
                                  "rev-list --max-count=1 --grep=\"%s\" "
                                  "HEAD") %
                                 (project_path, keyword), shell=True).rstrip()


def get_build_id(workspace):
  """Get BUILD_ID defined in $workspace/build/core/build_id.mk."""

  path = os.path.join(workspace, "build", "core", "build_id.mk")
  return subprocess.check_output("source %s && echo $BUILD_ID" % path,
                                 shell=True).rstrip()


def repo_sync_specific_release(url, branch, tag, workspace, ignore_error):
  """Repo sync source with the specific release tag."""

  if not os.path.exists(workspace):
    os.makedirs(workspace)

  manifest_path = os.path.join(workspace, ".repo", "manifests")

  repo_init(url, branch, workspace)

  if tag:
    rev = get_commit_with_keyword(manifest_path, tag)
    if not rev:
      raise(ValueError("could not find a manifest revision for tag " + tag))
    repo_init(url, rev, workspace)

  repo_sync(workspace, ignore_error)


def diff(manifest_url, manifest_branch, tag, 
         upstream_manifest_url, upstream_manifest_branch,
         exclusions_file, ignore_error_during_sync):
  """Syncs and diffs an Android workspace against an upstream workspace."""

  workspace = os.path.abspath(DOWNSTREAM_WORKSPACE)
  upstream_workspace = os.path.abspath(UPSTREAM_WORKSPACE)
  # repo sync downstream source tree
  repo_sync_specific_release(
      manifest_url,
      manifest_branch,
      tag,
      workspace,
      ignore_error_during_sync)

  build_id = None

  if tag:
    # get the build_id so that we know which rev of upstream we need
    build_id = get_build_id(workspace)
    if not build_id:
      raise(ValueError("Error: could not find the Build ID of " + workspace))

  # repo sync upstream source tree
  repo_sync_specific_release(
      upstream_manifest_url,
      upstream_manifest_branch,
      build_id,
      upstream_workspace,
      ignore_error_during_sync)


  # make output folder
  if tag:
    output_folder = os.path.abspath(tag.replace(" ", "_"))
  else:
    current_time = datetime.datetime.today().strftime('%Y%m%d_%H%M%S')
    output_folder = os.path.abspath(current_time)

  if not os.path.exists(output_folder):
      os.makedirs(output_folder)

  # do the comparison
  repo_diff_trees.diff(
      upstream_workspace,
      workspace,
      os.path.join(output_folder, "project.csv"),
      os.path.join(output_folder, "commit.csv"),
      os.path.abspath(exclusions_file),
  )


def main():
  args = parse_args()

  diff(args.manifest_url,
       args.manifest_branch,
       args.tag,
       args.upstream_manifest_url,
       args.upstream_manifest_branch,
       args.exclusions_file,
       args.ignore_error_during_sync)

if __name__ == "__main__":
  main()
