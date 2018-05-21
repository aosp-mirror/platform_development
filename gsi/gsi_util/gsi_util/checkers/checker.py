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

"""class Checker implementation.

Two major functionalities:
  1. Gets full or partial check list.
  2. Runs a given check list and returns the results.
"""

from collections import namedtuple

from gsi_util.checkers import sepolicy_checker
from gsi_util.checkers import vintf_checker

CheckListItem = namedtuple('CheckListItem', 'check_item checker_class')

# Uses a tuple to prevent functions in this module from returning a mutable
# list to the caller.
_CHECK_LIST = (
    CheckListItem('vintf', vintf_checker.VintfChecker),
    CheckListItem('sepolicy', sepolicy_checker.SepolicyChecker),
)


class Checker(object):
  """The main checker to maintain and run a full or partial check list."""

  def __init__(self, file_accessor):
    """Inits a checker with a given file_accessor.

    Args:
      file_accessor: Provides file access for the checker to retrieve
        required files for a given check item. e.g., getting compatibility
        matrix files for VINTF compatibility check, getting SEPolicy files
        for SEPolicy merge test.
    """
    self._file_accessor = file_accessor

  def check(self, check_list):
    """Runs all check items specified in the check_list.

    Args:
      check_list: A list of CheckListItem() containing multiple checkers.

    Returns:
      A list of CheckResultItem(), by concatenating the check results of
      each item in the check_list. Note that one CheckListItem() might
      generate more than one CheckResultItem()s. e.g., Three check list
      items in the check_list might return five check result items.
    """
    check_result_items = []

    for check_item in check_list:
      the_checker = check_item.checker_class(self._file_accessor)
      # A checker might return multiple CheckResultItem()s.
      check_result_items += the_checker.check()

    return check_result_items

  @staticmethod
  def make_check_list(check_items):
    """Returns a list of CheckListItem() by the given item names.

    Args:
      check_items: A list of CheckListItem().

    Raises:
      RuntimeError: When any input check_item is unknown or duplicated.
    """
    check_list = []

    for check_item in check_items:
      matched_items = [x for x in _CHECK_LIST if x.check_item == check_item]
      if not matched_items:
        raise RuntimeError('Unknown check item: {}'.format(check_item))
      # Checks there is exactly one match.
      if len(matched_items) != 1:
        raise RuntimeError(
            'Duplicated check items: {} in the check list'.format(check_item))
      check_list.append(matched_items[0])  # Appends the only matched item.

    return check_list

  @staticmethod
  def get_all_check_list():
    """Returns the default check list, which contains full check items."""
    return _CHECK_LIST
