"""List downstream commits that are not upstream and are visible in the diff.

Only include changes that are visible when you diff
the downstream and usptream branches.

This will naturally exclude changes that already landed upstream
in some form but were not merged or cherry picked.

This will also exclude changes that were added then reverted downstream.

"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
import argparse
import os
import subprocess


def git(args):
  """Git command.

  Args:
    args: A list of arguments to be sent to the git command.

  Returns:
    The output of the git command.
  """

  command = ['git']
  command.extend(args)
  with open(os.devnull, 'w') as devull:
    return subprocess.check_output(command, stderr=devull)


class CommitFinder(object):

  def __init__(self, working_dir, upstream, downstream):
    self.working_dir = working_dir
    self.upstream = upstream
    self.downstream = downstream

  def __call__(self, filename):
    insertion_commits = set()

    if os.path.isfile(os.path.join(self.working_dir, filename)):
      blame_output = git(['-C', self.working_dir, 'blame', '-l',
                          '%s..%s' % (self.upstream, self.downstream),
                          '--', filename])
      for line in blame_output.splitlines():
        # The commit is the first field of a line
        blame_fields = line.split(' ', 1)
        # Some lines can be empty
        if blame_fields:
          insertion_commits.add(blame_fields[0])

    return insertion_commits


def find_insertion_commits(upstream, downstream, working_dir):
  """Finds all commits that insert lines on top of the upstream baseline.

  Args:
    upstream: Upstream branch to be used as a baseline.
    downstream: Downstream branch to search for commits missing upstream.
    working_dir: Run as if git was started in this directory.

  Returns:
    A set of commits that insert lines on top of the upstream baseline.
  """

  insertion_commits = set()

  diff_files = git(['-C', working_dir, 'diff',
                    '--name-only',
                    '--diff-filter=d',
                    upstream,
                    downstream])
  diff_files = diff_files.splitlines()

  finder = CommitFinder(working_dir, upstream, downstream)
  commits_per_file = [finder(filename) for filename in diff_files]

  for commits in commits_per_file:
    insertion_commits.update(commits)

  return insertion_commits


def find(upstream, downstream, working_dir):
  """Finds downstream commits that are not upstream and are visible in the diff.

  Args:
    upstream: Upstream branch to be used as a baseline.
    downstream: Downstream branch to search for commits missing upstream.
    working_dir: Run as if git was started in thid directory.

  Returns:
    A set of downstream commits missing upstream.
  """

  commits_not_upstreamed = set()
  revlist_output = git(['-C', working_dir, 'rev-list', '--no-merges',
                        '%s..%s' % (upstream, downstream)])
  downstream_only_commits = set(revlist_output.splitlines())
  insertion_commits = set()

  # If there are no downstream-only commits there's no point in
  # futher filtering
  if downstream_only_commits:
    insertion_commits = find_insertion_commits(upstream, downstream,
                                               working_dir)

  # The commits that are only downstream and are visible in 'git blame' are the
  # ones that insert lines in the diff between upstream and downstream.
  commits_not_upstreamed.update(
      downstream_only_commits.intersection(insertion_commits))

  # TODO(diegowilson) add commits that deleted lines

  return commits_not_upstreamed


def main():
  parser = argparse.ArgumentParser(
      description='Finds commits yet to be applied upstream.')
  parser.add_argument(
      'upstream',
      help='Upstream branch to be used as a baseline.',
  )
  parser.add_argument(
      'downstream',
      help='Downstream branch to search for commits missing upstream.',
  )
  parser.add_argument(
      '-C',
      '--working_directory',
      help='Run as if git was started in thid directory',
      default='.',)
  args = parser.parse_args()
  upstream = args.upstream
  downstream = args.downstream
  working_dir = os.path.abspath(args.working_directory)

  print('\n'.join(find(upstream, downstream, working_dir)))


if __name__ == '__main__':
  main()
