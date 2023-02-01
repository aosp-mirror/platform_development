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

from ninja_metrics_proto import ninja_metrics_pb2

def generate_proto(ninja_data: dict):
    proto = ninja_metrics_pb2.NinjaMetrics()
    proto.num_input_files = ninja_data['total_input_count']
    proto.num_projects = ninja_data['total_project_count']
    for project in ninja_data['project_count']:
        project_info = proto.project_infos.add()
        project_info.name = project
        project_info.num_input_files = ninja_data['project_count'][project]
    return proto

def print_proto_file(proto_file):
    proto = ninja_metrics_pb2.NinjaMetrics()
    proto.ParseFromString(proto_file.read())
    print("num_input_files: ", proto.num_input_files)
    print("num_projects: ", proto.num_projects)
    print("project_infos: [")
    for project in proto.project_infos:
        print(f"  {project.name}: {project.num_input_files}")
    print("]")