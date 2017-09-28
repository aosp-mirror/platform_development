"""Diffs one repo source tree an upstream repo source tree.

Matches the projects from a Gerrit repo workspace to the projects
of an upstream workspace. After identifying exist both in the
downstream and the upstream workspace it then diffs the each project.

Finally, the results of the project matching and diffing are reported.

"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
import argparse
import csv
import datetime
import multiprocessing
import os
import re
import subprocess
import xml.etree.ElementTree as et
import git_commits_not_upstreamed


def get_projects(source_tree):
  """Retrieve the dict of projects names and paths.

  Args:
    source_tree: A path to the source tree.

  Returns:
    A dict of project paths keyed by project names.
  """

  projects = {}

  manifest = source_tree + '/.repo/manifest.xml'
  tree = et.parse(manifest)
  root = tree.getroot()

  for project in root.findall('project'):
    # Ignore projects that are not synced by default
    if 'notdefault' in project.get('groups', ''):
      continue
    path = project.get('path', project.get('name'))
    path = os.path.abspath(os.path.join(source_tree, path))
    name = project.get('name')

    # check if project files actually exist
    if not os.path.exists(path):
      continue

    projects[name] = path

  return projects


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


def get_revision_diff_stats(directory, rev_a, rev_b):
  """Retrieves stats of diff between two git revisions.

  Args:
    directory: A path to the git directory to diff.
    rev_a: A git revision to diff.
    rev_b: A git revision to diff.

  Returns:
    A dict with the count of files modified, lines added
    and lines removed.
  """
  stats = {
      'file': 0,
      'insertion': 0,
      'deletion': 0,
  }

  git_diffstat = git(
      ['-C', directory, 'diff', '--shortstat', rev_a, rev_b])
  for element in git_diffstat.split(','):
    for key in stats:
      if key in element:
        stats[key] = int(element.split()[0])

  return stats


def get_project_stats(upstream_dir, downstream_dir):
  """Retrieves stats of diff between two git projects.

  Diffs a downstream directory against an upstream directory.
  Lines that exist only in the downstream directory are considered insertions.
  Lines that exist only in the upstream directory are considered deletions.

  Args:
    upstream_dir: A path to the upstream directory to compare.
    downstream_dir: A path to the downstream directory to compare.

  Returns:
    A dict with the count of files modified, lines added
    and lines removed.
  """
  stats = {
      'file': 0,
      'insertion': 0,
      'deletion': 0,
  }

  if upstream_dir and downstream_dir:
    print('Diffing %s vs %s' % (downstream_dir, upstream_dir))
    git(['-C', downstream_dir, 'fetch', '--update-shallow', upstream_dir])
    stats = get_revision_diff_stats(downstream_dir, 'FETCH_HEAD', 'HEAD')

  return stats


def match_project_by_root_commits(
    downstream_project_name, downstream_project_path, upstream_root_commits):
  """Match a downstream project to an upstream project using their root commits.

  Find all root commits in a downstream project and find a matching
  upstream project that have a root commit in common.

  Args:
    downstream_project_name: A string with the downstream project name.
    downstream_project_path: A string with the downstream project path.
    upstream_root_commits: A dict of root commits and their upstream project.

  Returns:
    A string with the matched upstream project name.
  """
  upstream_match = None
  downstream_root_commits = find_root_commits_in_path(downstream_project_path)
  for root in downstream_root_commits:
    if root in upstream_root_commits:
      upstream_project_list = upstream_root_commits[root]
      if len(upstream_project_list) > 1:
        print('Warning: ' + downstream_project_name +
              ' matches multiple projects')
        print(upstream_project_list)
      else:
        upstream_match = upstream_project_list[0]['name']
      # Once there's a root commit match, stop looking for a project match
      break

  return upstream_match


def match_projects(upstream_projects, downstream_projects):
  """Match downstream projects to upstream projects.

  Args:
    upstream_projects: A dict of upstream projects.
    downstream_projects: A dict of downstream projects.

  Returns:
    A list of upstream and downstream project pairs.
  """

  project_matches = []

  # keep a list of upstream projects that have not been matched
  unmatched_upstream_projects = set(upstream_projects.keys())

  upstream_root_commits = find_root_commits_in_projects(upstream_projects)
  # Match all downstream projects to an upstream project
  for downstream_name, downstream_path in downstream_projects.iteritems():
    # First try to match projects by name
    if downstream_name in upstream_projects:
      upstream_match = downstream_name
    # If there is no project name match then try matching by commit
    else:
      upstream_match = match_project_by_root_commits(
          downstream_name, downstream_path, upstream_root_commits)

    project_matches.append({
        'upstream': upstream_match,
        'downstream': downstream_name,
    })
    unmatched_upstream_projects.discard(upstream_match)

  # Add all upstream projects that have not been matched
  for project in unmatched_upstream_projects:
    project_matches.append({
        'upstream': project,
        'downstream': None,
    })

  return project_matches


def filter_exclusion_list(projects, exclusion_file):
  """Removes all projects that match the exclusion patterns."""

  filtered = {}

  exclusion_list = []
  if exclusion_file:
    with open(exclusion_file) as f:
      exclusion_list = f.readlines()
  exclusion_list = [line.strip() for line in exclusion_list]
  exclusion_pattern = '|'.join(exclusion_list)

  if exclusion_pattern:
    for name, path in projects.iteritems():
      if re.match(exclusion_pattern, name):
        print('Excluding ' + name)
      else:
        filtered[name] = path
  else:
    filtered = projects

  return filtered


def get_all_projects_stats(upstream_source_tree, downstream_source_tree,
                           exclusion_file):
  """Finds the stats of all project in a source tree.

  Args:
    upstream_source_tree: A string with the path to the upstream gerrit
      source tree.
    downstream_source_tree: A string with the path to the downstream gerrit
      source tree.
    exclusion_file: A string with the path to the exclusion file.

  Returns:
    A dict of matching upstream and downstream projects
    including stats for projects that matches.
  """
  all_project_stats = []

  upstream_projects = get_projects(upstream_source_tree)
  downstream_projects = get_projects(downstream_source_tree)

  upstream_projects = filter_exclusion_list(upstream_projects, exclusion_file)
  downstream_projects = filter_exclusion_list(downstream_projects,
                                              exclusion_file)

  project_matches = match_projects(upstream_projects, downstream_projects)

  for match in project_matches:
    upstream_project_name = match['upstream']
    downstream_project_name = match['downstream']
    project_stats = get_project_stats(
        upstream_projects.get(upstream_project_name, None),
        downstream_projects.get(downstream_project_name, None))
    status = ''
    if not upstream_project_name:
      status = 'Downstream Only Projects'
    elif not downstream_project_name:
      status = 'Upstream Only Projects'
    elif project_stats['file'] == 0:
      status = 'Intact Projects'
    elif upstream_project_name == downstream_project_name:
      status = 'Modified Projects'
    else:
      status = 'Forked Projects'

    project_stats['status'] = status
    project_stats['upstream'] = upstream_project_name
    project_stats['downstream'] = downstream_project_name
    project_stats['downstream_path'] = downstream_projects.get(
        downstream_project_name)

    all_project_stats.append(project_stats)

  return all_project_stats


def find_root_commits_in_path(path):
  """Returns a list of root commits in a git project path."""
  print('Analyzing history of ' + path)
  rev_list = git(['-C', path, 'rev-list', '--max-parents=0', 'HEAD'])
  return rev_list.splitlines()


def find_root_commits_in_projects(projects):
  """Returns a dict of root commits with all projects with that root commit."""
  root_commits = {}
  for name, path in projects.iteritems():
    for root in find_root_commits_in_path(path):
      root_list = root_commits.get(root, [])
      root_list.append({
          'name': name,
          'path': path,
      })
      root_commits[root] = root_list
  return root_commits


def get_commit_stats_in_project(project):
  """Extract commits that have not been upstreamed in a specific project.

  Args:
    project: A dict of a project name and path.

  Returns:
    A dict of commits not upstreamed.
  """
  name = project['name']
  path = project['downstream_path']
  print('Finding commits not upstreamed in ' + name)
  commits = git_commits_not_upstreamed.find('FETCH_HEAD', 'HEAD', path)
  print('Found commits not upstreamed in ' + name)
  stats = []
  for commit in commits:
    author = git(['-C', path, 'show', '--no-patch', '--format=%ae', commit])
    author = author.strip()
    subject = git(['-C', path, 'show', '--no-patch', '--format=%s', commit])
    subject = subject.strip()
    stats.append({
        'commit': commit,
        'author': author,
        'subject': subject,
    })

  return {
      'name': name,
      'stats': stats,
  }


def get_all_commits_stats(project_stats):
  """Extract commits that have not been upstreamed in all projects.

  Args:
    project_stats: A dict of matching upstream and downstream projects
      including stats for projects that matches.

  Returns:
    A dict of commits not upstreamed.
  """
  commit_stats = {}
  downstream_stats = {match['downstream']: match for match in project_stats}

  # Only analyze modified projects
  modified_projects = []
  for name, stats in downstream_stats.iteritems():
    if stats['status'].startswith('Modified'):
      stats['name'] = name
      modified_projects.append(stats)

  pool = multiprocessing.Pool()

  commit_stats = pool.map(get_commit_stats_in_project, modified_projects)

  commit_stats = {stats['name']: stats['stats'] for stats in commit_stats}

  return commit_stats


def write_commit_csv(commit_stats, commit_output_file):
  """Write project comparison data to a CSV file.

  Args:
    commit_stats: The dict of the stats for all commits.
    commit_output_file: Path to the output file.
  """
  with open(commit_output_file, 'w') as f:
    fieldnames = [
        'Date',
        'Commit',
        'Downstream Project',
        'Author',
        'Subject',
    ]
    today = datetime.datetime.today().strftime('%Y/%m/%d')
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    for project, stats in commit_stats.iteritems():
      for stat in stats:
        writer.writerow({
            'Date': today,
            'Commit': stat['commit'],
            'Downstream Project': project,
            'Author': stat['author'],
            'Subject': stat['subject'],
        })
  print('Wrote commit stats to ' + commit_output_file)


def write_project_csv(project_stats, commit_stats, project_output_file):
  """Write project comparison data to a CSV file.

  Args:
    project_stats: The dict of the stats for all projects.
    commit_stats: The dict of the stats for all commits.
    project_output_file: Path to the output file.
  """
  with open(project_output_file, 'w') as f:
    fieldnames = [
        'Date',
        'Downstream Project',
        'Upstream Project',
        'Diff Status',
        'Files Changed',
        'Line Insertions',
        'Line Deletions',
        'Line Changes',
        'Commits Not Upstreamed',
    ]
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    today = datetime.datetime.today().strftime('%Y/%m/%d')
    for stat in project_stats:
      commits_not_upstreamed = 0
      downstream_project = stat['downstream']
      if downstream_project in commit_stats:
        commits_not_upstreamed = len(commit_stats[downstream_project])
      writer.writerow({
          'Date': today,
          'Downstream Project': downstream_project,
          'Upstream Project': stat['upstream'],
          'Diff Status': stat['status'],
          'Files Changed': stat['file'],
          'Line Insertions': stat['insertion'],
          'Line Deletions': stat['deletion'],
          'Line Changes': stat['insertion'] + stat['deletion'],
          'Commits Not Upstreamed': commits_not_upstreamed,
      })
  print('Wrote project stats to ' + project_output_file)


def diff(upstream_source_tree, downstream_source_tree, project_output_file,
         commit_output_file, exclusions_file):
  """Diff one repo source tree against another.

  Args:
    upstream_source_tree: A string with the path to a gerrit source tree.
    downstream_source_tree: A string with the path to a gerrit source tree.
    project_output_file: Path to the project output file.
    commit_output_file: Path to the commit output file.
    exclusions_file: Path to exclusions file.
  """
  project_stats = get_all_projects_stats(upstream_source_tree,
                                         downstream_source_tree,
                                         exclusions_file)
  commit_stats = get_all_commits_stats(project_stats)
  write_commit_csv(commit_stats, commit_output_file)
  write_project_csv(project_stats, commit_stats, project_output_file)


def main():
  parser = argparse.ArgumentParser(
      description='Diff a repo source tree against an upstream source tree.')
  parser.add_argument('upstream_path', help='Path to an upstream source tree.')
  parser.add_argument(
      'downstream_path', help='Path to a downstream source tree.')
  parser.add_argument(
      '-p',
      '--project_output_file',
      help='Path to write the project output file',
      default='project.csv',)
  parser.add_argument(
      '-c',
      '--commit_output_file',
      help='Path to write the commit output file',
      default='commit.csv',)
  parser.add_argument(
      '-e',
      '--exclusions_file',
      help='Path to file with a list of project names to be excluded from'
      'the diff. You may use a regular expression to match project names as'
      'described in https://docs.python.org/2/howto/regex.html',
      default='',
  )
  args = parser.parse_args()
  upstream_source_tree = os.path.abspath(args.upstream_path)
  downstream_source_tree = os.path.abspath(args.downstream_path)
  project_output_file = os.path.abspath(args.project_output_file)
  commit_output_file = os.path.abspath(args.commit_output_file)
  exclusions_file = ''
  if args.exclusions_file:
    exclusions_file = os.path.abspath(args.exclusions_file)

  diff(upstream_source_tree, downstream_source_tree, project_output_file,
       commit_output_file, exclusions_file)


if __name__ == '__main__':
  main()
