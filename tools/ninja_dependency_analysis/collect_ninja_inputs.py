#!/usr/bin/env python3

# Copyright (C) 2022 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import json
import os
import pathlib
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import OrderedDict
from operator import itemgetter
from ninja_metrics_proto import ninja_metrics

def build_cmd(ninja_binary, ninja_file, target, exempted_file_list):
    cmd = [ninja_binary, '-f', ninja_file, '-t', 'inputs']
    if exempted_file_list and exempted_file_list.exists():
        with open(exempted_file_list) as fin:
            for l in map(str.strip, fin.readlines()):
                if l and not l.startswith('#'):
                    cmd.extend(['-e', l])
    cmd.append(target)

    return cmd


def count_project(projects, input_files):
    project_count = dict()
    for p in projects:
        file_count = sum(f.startswith(p + os.path.sep) for f in input_files)
        if file_count > 0:
            project_count[p] = file_count

    return dict(sorted(project_count.items(), key=itemgetter(1), reverse=True))


parser = argparse.ArgumentParser()

parser.add_argument('-n', '--ninja_binary', type=pathlib.Path, required=True)
parser.add_argument('-f', '--ninja_file', type=pathlib.Path, required=True)
parser.add_argument('-t', '--target', type=str, required=True)
parser.add_argument('-e', '--exempted_file_list', type=pathlib.Path)
parser.add_argument('-o', '--out', type=pathlib.Path)
group = parser.add_mutually_exclusive_group()
group.add_argument('-r', '--repo_project_list', type=pathlib.Path)
group.add_argument('-m', '--repo_manifest', type=pathlib.Path)
args = parser.parse_args()

input_files = sorted(
    subprocess.check_output(
        build_cmd(args.ninja_binary, args.ninja_file, args.target,
                  args.exempted_file_list), text=True).strip().split('\n'))

result = dict()
result['input_files'] = input_files

projects = None
if args.repo_project_list and args.repo_project_list.exists():
    with open(args.repo_project_list) as fin:
        projects = list(map(str.strip, fin.readlines()))
elif args.repo_manifest and args.repo_manifest.exists():
    projects = [
        p.attrib['path']
        for p in ET.parse(args.repo_manifest).getroot().findall('project')
    ]

if projects:
    project_to_count = count_project(projects, input_files)
    result['project_count'] = project_to_count
    result['total_project_count'] = len(project_to_count)

result['total_input_count'] = len(input_files)

if args.out:
    with open(os.path.join(args.out.parent, args.out.name + '.json'), 'w') as json_file:
        json.dump(result, json_file, indent=2)
    with open(os.path.join(args.out.parent, args.out.name + '.pb'), 'wb') as pb_file:
        pb_file.write(ninja_metrics.generate_proto(result).SerializeToString())
else:
    print(json.dumps(result, indent=2))
