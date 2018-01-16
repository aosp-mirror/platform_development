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

"""Provide class Checker and maintain the checking list."""

from collections import namedtuple

from gsi_util.checkers.vintf_checker import VintfChecker

CheckListItem = namedtuple('CheckListItem', 'id checker_class')

_CHECK_LIST = [
    CheckListItem('checkvintf', VintfChecker),
]


class Checker(object):
  """Implement methods and utils to checking compatibility a FileAccessor."""

  def __init__(self, file_accessor):
    self._file_accessor = file_accessor

  def check(self, check_list):
    check_result_items = []

    for x in check_list:
      checker = x.checker_class(self._file_accessor)
      check_result_items += checker.check()

    return check_result_items

  @staticmethod
  def make_check_list_with_ids(ids):
    check_list = []
    for check_id in ids:
      # Find the first item matched check_id
      matched_check_item = next((x for x in _CHECK_LIST if x.id == check_id),
                                None)
      if not matched_check_item:
        raise RuntimeError('Unknown check ID: "{}"'.format(check_id))
      check_list.append(matched_check_item)
    return check_list

  @staticmethod
  def get_all_check_list():
    return _CHECK_LIST
