#!/usr/bin/python3
#
# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
"""Convert the compare result csv file to a spreadsheet.
Prerequisite:
  - Install the `gspread` python package.
  - Create credentials to allow access to spreadsheets via Google Sheets API.

Usage example:
python3 generate_spread_sheet.py \
  --compared_result compare_result/diff.csv \
  --sheet_name "CTS Compare Result" \
  --credentials_dir ~/.config/gspread/
"""

import argparse
import csv
import gspread
import os

from typing import List, Tuple


_COLOR_GREY = {'red': 0.37, 'green': 0.42, 'blue': 0.42}
_COLOR_WHITE = {'red': 0.95, 'green': 0.95, 'blue': 0.95}
_COLOR_YELLOW = {'red': 0.9, 'green': 0.8, 'blue': 0.07}
_COLOR_DARK_BLUE = {'red': 0.15, 'green': 0.15, 'blue': 0.46}


_SHEET_HEADER_FORMAT = {
    'backgroundColor': _COLOR_GREY,
    'horizontalAlignment': 'CENTER',
    'textFormat': {
        'foregroundColor': _COLOR_WHITE,
        'fontSize': 11,
        'bold': True,
    },
}


_MODULE_HEADER_FORMAT = {
    'backgroundColor': _COLOR_YELLOW,
    'horizontalAlignment': 'LEFT',
    'textFormat': {
        'foregroundColor': _COLOR_DARK_BLUE,
        'fontSize': 10,
        'bold': True,
    },
}


# The first four columns in compare_results are for test info.
_NUM_OF_INFO_COLUMNS = 4


def _parse_args() -> argparse.Namespace:
  """Parse the script arguments.

  Returns:
    An object of argparse.Namespace.
  """
  parser = argparse.ArgumentParser()
  parser.add_argument('--compared_result', required=True,
                      help='Path to the compared csv file.')
  parser.add_argument('--sheet_name', required=True,
                      help='Name for the output spreadsheet.')
  parser.add_argument('--credentials_dir', required=True,
                      help='Path to the directory that contains gspread '
                           'credentials files.')
  return parser.parse_args()


def _read_csv(csv_path: str) -> Tuple[List[str], List[List[str]]]:
  """Read a csv comparison report and return as lists.

  Args:
    csv_path: The path to the csv comparison report.

  Returns:
    A List of report names, A List of parsed results.
  """
  parsed_result = []
  with open(csv_path, 'r') as csvfile:
    result_reader = csv.reader(csvfile, delimiter=',')
    header = next(result_reader)
    report_names = header[_NUM_OF_INFO_COLUMNS:]
    for row in result_reader:
      parsed_result.append(row)
  return report_names, parsed_result


def _create_spread_sheet(
    new_sheet_name: str, credentials_dir: str
) -> gspread.Spreadsheet:
  """Create a spread sheet at the user's Drive directory.

  Args:
    new_sheet_name: The name of this spread sheet.
    credentials_dir: The path to the directory that contains gspread
                     credentials files.

  Returns:
    An object of gspread.Spreadsheet.
  """
  credentials = os.path.join(credentials_dir, 'credentials.json')
  authorized_user = os.path.join(credentials_dir, 'authorized_user.json')
  gc = gspread.oauth(credentials_filename=credentials,
                     authorized_user_filename=authorized_user)
  sh = gc.create(new_sheet_name)
  return sh


def _get_range_cell(
    begin_row: int, begin_column: str, num_rows: int, num_columns: int
) -> str:
  """Get the sheet cell range in the string format.

  Args:
    begin_row: The begin row, in integer format.
    begin_column: The begin column, in string format.
    num_rows: Number of rows.
    num_columns: Number of columns.

  Return:
    The range cell in the string format.
  """
  end_row = begin_row + num_rows - 1
  end_column = chr(ord(begin_column) + num_columns - 1)
  return f'{begin_column}{begin_row}:{end_column}{end_row}'


def _write_compare_info(
    sheet: gspread.Worksheet, report_names: List[str]
) -> None:
  """Write the compare information to a worksheet.

  Args:
    sheet: The object to worksheet for writing.
    report_names: A list of cts report names.
  """
  sheet.update_title('Test Info')
  build_info = []
  for i, name in enumerate(report_names):
    build_info.append([f'Build {i}', name])
  sheet.update(build_info)


def _write_compare_details(
    sheet: gspread.Worksheet, compare_results: List[List[str]], start_row: int
) -> None:
  """Write the detailed comparison result to a worksheet.

  Args:
    sheet: The object to worksheet for writing.
    compare_results: A list of comparison results.
    start_row: The starting row for writing comparison results.
  """
  curr_module = 'None'
  curr_row = start_row
  module_header_row = start_row
  rows_content = []
  module_header_formats = []

  num_reports = len(compare_results[0]) - _NUM_OF_INFO_COLUMNS
  module_failures = [0] * num_reports
  for row_index, row_values in enumerate(compare_results):
    module_name, abi, test_class, test_item, *test_statuses = row_values
    module_end = ((row_index == len(compare_results) - 1) or
                  (module_name != compare_results[row_index + 1][0]))

    # Module changes, need a new header row.
    if module_name != curr_module:
      rows_content.append([f'{module_name} [{abi}]', ''] + [''] * num_reports)
      module_header_row = len(rows_content)
      header_cell = _get_range_cell(
          begin_row=curr_row, begin_column='A',
          num_rows=1, num_columns=len(rows_content[0]))
      module_header_formats.append({
          'range': header_cell,
          'format': _MODULE_HEADER_FORMAT,
      })
      curr_row += 1

    curr_module = module_name
    for i, status in enumerate(test_statuses):
      test_statuses[i] = '-' if status == 'pass' else status.upper()
      if test_statuses[i] not in ['-', 'ASSUMPTION_FAILURE', 'NULL']:
        module_failures[i] += 1
    rows_content.append([test_class, test_item] + test_statuses)
    curr_row += 1

    # Module ends, update number of failed items in the header.
    if module_end:
      for index, count in enumerate(module_failures):
        # The first two columns are for module info.
        rows_content[module_header_row - 1][index + 2] = f'Failed: {count}'
      module_failures = [0] * num_reports

  if rows_content and module_header_formats:
    content_cell = _get_range_cell(
        begin_row=start_row, begin_column='A',
        num_rows=len(rows_content), num_columns=len(rows_content[0]))
    sheet.update(content_cell, rows_content)
    sheet.batch_format(module_header_formats)


def main():
  args = _parse_args()

  # Get the comparison result
  report_names, compare_results = _read_csv(args.compared_result)

  # Create a google spread sheet
  sheets = _create_spread_sheet(args.sheet_name, args.credentials_dir)

  # Write test info to the fist worksheet
  _write_compare_info(sheets.sheet1, report_names)

  # Write comapre details to the second worksheet
  # Limit the rows to len(compare_results) * 2 because we need module headers
  detail_sheet = sheets.add_worksheet(
      title='Detailed Comparison',
      rows=len(compare_results) * 2,
      cols=len(compare_results[0]))

  # Format the first row
  row_header = ['Test Class', 'Test Item'] + report_names
  cell = _get_range_cell(
      begin_row=1, begin_column='A', num_rows=1, num_columns=len(row_header))
  detail_sheet.update(cell, [row_header])
  detail_sheet.format(cell, _SHEET_HEADER_FORMAT)

  # write details to the worksheet, starting at the second row
  _write_compare_details(detail_sheet, compare_results, 2)


if __name__ == '__main__':
  main()
