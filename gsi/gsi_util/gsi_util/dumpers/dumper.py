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

"""Implement dump methods and utils to dump info from a mounter."""

from gsi_util.dumpers.dump_info_list import DUMP_LIST


class Dumper(object):

  def __init__(self, file_accessor):
    self._file_accessor = file_accessor

  @staticmethod
  def _dump_by_dumper(dumper_instance, dump_list):
    """Dump info by the given dumper instance according the given dump_list.

    Used for dump(), see the comment of dump() for type details.

    Args:
      dumper_instance: a dumper instance to process dump.
      dump_list: a list of dump info to be dump. The items in the list must
        relative to dumper_instance.

    Returns:
      The dump result by dictionary maps info_name to the value of dump result.
    """
    dump_result = {}

    for dump_info in dump_list:
      value = dumper_instance.dump(dump_info.lookup_key)
      dump_result[dump_info.info_name] = value

    return dump_result

  def dump(self, dump_list):
    """Dump info according the given dump_list.

    Args:
      dump_list: a list of dump info to be dump. See dump_info_list.py for
                 the detail types.
    Returns:
      The dump result by dictionary maps info_name to the value of dump result.
    """
    dump_result = {}

    # query how many different dumpers to dump
    dumper_set = set([x.dumper_create_args for x in dump_list])
    for dumper_create_args in dumper_set:
      # The type of a dumper_create_args is (Class, instantiation args...)
      dumper_class = dumper_create_args[0]
      dumper_args = dumper_create_args[1:]
      # Create the dumper
      with dumper_class(self._file_accessor, dumper_args) as dumper_instance:
        dump_list_for_the_dumper = (
            x for x in dump_list if x.dumper_create_args == dumper_create_args)
        dumper_result = self._dump_by_dumper(dumper_instance,
                                             dump_list_for_the_dumper)
        dump_result.update(dumper_result)

    return dump_result

  @staticmethod
  def make_dump_list_by_name_list(name_list):
    info_list = []
    for info_name in name_list:
      info = next((x for x in DUMP_LIST if x.info_name == info_name), None)
      if not info:
        raise RuntimeError('Unknown info name: "{}"'.format(info_name))
      info_list.append(info)
    return info_list

  @staticmethod
  def get_all_dump_list():
    return DUMP_LIST
