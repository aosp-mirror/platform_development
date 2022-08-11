#!/usr/bin/env python3
#
# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Add files to a Rust package for third party review."""

import collections
import datetime
import enum
import glob
import json
import os
import pathlib
import re

# patterns to match keys in Cargo.toml
NAME_PATTERN = r"^name *= *\"(.+)\""
NAME_MATCHER = re.compile(NAME_PATTERN)
VERSION_PATTERN = r"^version *= *\"(.+)\""
VERSION_MATCHER = re.compile(VERSION_PATTERN)
DESCRIPTION_PATTERN = r"^description *= *(\".+\")"
DESCRIPTION_MATCHER = re.compile(DESCRIPTION_PATTERN)
# NOTE: This description one-liner pattern fails to match
# multi-line descriptions in some Rust crates, e.g. shlex.
LICENSE_PATTERN = r"^license *= *\"(.+)\""
LICENSE_MATCHER = re.compile(LICENSE_PATTERN)

# patterns to match year/month/day in METADATA
YMD_PATTERN = r"^ +(year|month|day): (.+)$"
YMD_MATCHER = re.compile(YMD_PATTERN)
YMD_LINE_PATTERN = r"^.* year: *([^ ]+) +month: *([^ ]+) +day: *([^ ]+).*$"
YMD_LINE_MATCHER = re.compile(YMD_LINE_PATTERN)

# patterns to match Apache/MIT licence in LICENSE*
APACHE_PATTERN = r"^.*Apache License.*$"
APACHE_MATCHER = re.compile(APACHE_PATTERN)
MIT_PATTERN = r"^.*MIT License.*$"
MIT_MATCHER = re.compile(MIT_PATTERN)
BSD_PATTERN = r"^.*BSD .*License.*$"
BSD_MATCHER = re.compile(BSD_PATTERN)
MULTI_LICENSE_COMMENT = ("# Dual-licensed, using the least restrictive "
        "per go/thirdpartylicenses#same.\n  ")

# default owners added to OWNERS
DEFAULT_OWNERS = "include platform/prebuilts/rust:master:/OWNERS\n"

# See b/159487435 Official policy for rust imports METADATA URLs.
# "license_type: NOTICE" might be optional,
# but it is already used in most rust crate METADATA.
# This line format should match the output of external_updater.
METADATA_CONTENT = """name: "{name}"
description: {description}
third_party {{
  url {{
    type: HOMEPAGE
    value: "https://crates.io/crates/{name}"
  }}
  url {{
    type: ARCHIVE
    value: "https://static.crates.io/crates/{name}/{name}-{version}.crate"
  }}
  version: "{version}"
  {license_comment}license_type: NOTICE
  last_upgrade_date {{
    year: {year}
    month: {month}
    day: {day}
  }}
}}
"""


def get_metadata_date():
  """Return last_upgrade_date in METADATA or today."""
  # When applied to existing directories to normalize METADATA,
  # we don't want to change the last_upgrade_date.
  year, month, day = "", "", ""
  if os.path.exists("METADATA"):
    with open("METADATA", "r") as inf:
      for line in inf:
        match = YMD_MATCHER.match(line)
        if match:
          if match.group(1) == "year":
            year = match.group(2)
          elif match.group(1) == "month":
            month = match.group(2)
          elif match.group(1) == "day":
            day = match.group(2)
        else:
          match = YMD_LINE_MATCHER.match(line)
          if match:
            year, month, day = match.group(1), match.group(2), match.group(3)
  if year and month and day:
    print("### Reuse date in METADATA:", year, month, day)
    return int(year), int(month), int(day)
  today = datetime.date.today()
  return today.year, today.month, today.day


def add_metadata(name, version, description, multi_license):
  """Update or add METADATA file."""
  if os.path.exists("METADATA"):
    print("### Updating METADATA")
  else:
    print("### Adding METADATA")
  year, month, day = get_metadata_date()
  license_comment = ""
  if multi_license:
    license_comment = MULTI_LICENSE_COMMENT
  with open("METADATA", "w") as outf:
    outf.write(METADATA_CONTENT.format(
        name=name, description=description, version=version,
        license_comment=license_comment, year=year, month=month, day=day))


def grep_license_keyword(license_file):
  """Find familiar patterns in a file and return the type."""
  with open(license_file, "r") as input_file:
    for line in input_file:
      if APACHE_MATCHER.match(line):
        return License(LicenseType.APACHE2, license_file)
      if MIT_MATCHER.match(line):
        return License(LicenseType.MIT, license_file)
      if BSD_MATCHER.match(line):
        return License(LicenseType.BSD_LIKE, license_file)
  print("ERROR: cannot decide license type in", license_file,
        "assume BSD_LIKE")
  return License(LicenseType.BSD_LIKE, license_file)


class LicenseType(enum.IntEnum):
  """A type of license.

  An IntEnum is used to be able to sort by preference. This is mainly the case
  for dual-licensed Apache/MIT code, for which we prefer the Apache license.
  The enum name is used to generate the corresponding MODULE_LICENSE_* file.
  """
  APACHE2 = 1
  MIT = 2
  BSD_LIKE = 3
  ISC = 4


License = collections.namedtuple('License', ['type', 'filename'])


def decide_license_type(cargo_license):
  """Check LICENSE* files to determine the license type.

  Returns: A list of Licenses. The first element is the license we prefer.
  """
  # Most crates.io packages have both APACHE and MIT.
  # Some crate like time-macros-impl uses lower case names like LICENSE-Apache.
  licenses = []
  license_file = None
  for license_file in glob.glob("LICENSE*") + glob.glob("COPYING*"):
    lowered_name = license_file.lower()
    if lowered_name == "license-apache":
      licenses.append(License(LicenseType.APACHE2, license_file))
    elif lowered_name == "license-mit":
      licenses.append(License(LicenseType.MIT, license_file))
  if licenses:
    licenses.sort(key=lambda l: l.type)
    return licenses
  if not license_file:
    raise FileNotFoundError("No license file has been found.")
  # There is a LICENSE* or COPYING* file, use cargo_license found in
  # Cargo.toml.
  if "Apache" in cargo_license:
    return [License(LicenseType.APACHE2, license_file)]
  if "MIT" in cargo_license:
    return [License(LicenseType.MIT, license_file)]
  if "BSD" in cargo_license:
    return [License(LicenseType.BSD_LIKE, license_file)]
  if "ISC" in cargo_license:
    return [License(LicenseType.ISC, license_file)]
  return [grep_license_keyword(license_file)]


def add_notice():
  if not os.path.exists("NOTICE"):
    if os.path.exists("LICENSE"):
      os.symlink("LICENSE", "NOTICE")
      print("Created link from NOTICE to LICENSE")
    else:
      print("ERROR: missing NOTICE and LICENSE")


def check_license_link(target):
  """Check the LICENSE link, must bet the given target."""
  if not os.path.islink("LICENSE"):
    print("ERROR: LICENSE file is not a link")
    return
  found_target = os.readlink("LICENSE")
  if target != found_target and found_target != "LICENSE.txt":
    print("ERROR: found LICENSE link to", found_target,
          "but expected", target)


def add_license(target):
  """Add LICENSE link to give target."""
  if os.path.exists("LICENSE"):
    if os.path.islink("LICENSE"):
      check_license_link(target)
    else:
      print("NOTE: found LICENSE and it is not a link.")
    return
  print("### Creating LICENSE link to", target)
  os.symlink(target, "LICENSE")


def add_module_license(license_type):
  """Touch MODULE_LICENSE_type file."""
  # Do not change existing MODULE_* files.
  for suffix in ["MIT", "APACHE", "APACHE2", "BSD_LIKE"]:
    module_file = "MODULE_LICENSE_" + suffix
    if os.path.exists(module_file):
      if license_type.name != suffix:
        raise Exception("Found unexpected license " + module_file)
      return
  module_file = "MODULE_LICENSE_" + license_type.name.upper()
  pathlib.Path(module_file).touch()
  print("### Touched", module_file)


def found_line(file_name, line):
  """Returns true if the given line is found in a file."""
  with open(file_name, "r") as input_file:
    return line in input_file


def add_owners():
  """Create or append OWNERS with the default owner line."""
  # Existing OWNERS file might contain more than the default owners.
  # Only append missing default owners to existing OWNERS.
  if os.path.isfile("OWNERS"):
    if found_line("OWNERS", DEFAULT_OWNERS):
      print("### No change to OWNERS, which has already default owners.")
      return
    else:
      print("### Append default owners to OWNERS")
      mode = "a"
  else:
    print("### Creating OWNERS with default owners")
    mode = "w"
  with open("OWNERS", mode) as outf:
    outf.write(DEFAULT_OWNERS)


def toml2json(line):
  """Convert a quoted toml string to a json quoted string for METADATA."""
  if line.startswith("\"\"\""):
    return "\"()\""  # cannot handle broken multi-line description
  # TOML string escapes: \b \t \n \f \r \" \\ (no unicode escape)
  line = line[1:-1].replace("\\\\", "\n").replace("\\b", "")
  line = line.replace("\\t", " ").replace("\\n", " ").replace("\\f", " ")
  line = line.replace("\\r", "").replace("\\\"", "\"").replace("\n", "\\")
  # replace a unicode quotation mark, used in the libloading crate
  line = line.replace("’", "'")
  # strip and escape single quotes
  return json.dumps(line.strip()).replace("'", "\\'")


def parse_cargo_toml(cargo):
  """get name, version, description, license string from Cargo.toml."""
  name = ""
  version = ""
  description = ""
  cargo_license = ""
  with open(cargo, "r") as toml:
    for line in toml:
      if not name and NAME_MATCHER.match(line):
        name = NAME_MATCHER.match(line).group(1)
      elif not version and VERSION_MATCHER.match(line):
        version = VERSION_MATCHER.match(line).group(1)
      elif not description and DESCRIPTION_MATCHER.match(line):
        description = toml2json(DESCRIPTION_MATCHER.match(line).group(1))
      elif not cargo_license and LICENSE_MATCHER.match(line):
        cargo_license = LICENSE_MATCHER.match(line).group(1)
      if name and version and description and cargo_license:
        break
  return name, version, description, cargo_license


def main():
  """Add 3rd party review files."""
  cargo = "Cargo.toml"
  if not os.path.isfile(cargo):
    print("ERROR: ", cargo, "is not found")
    return
  if not os.access(cargo, os.R_OK):
    print("ERROR: ", cargo, "is not readable")
    return
  name, version, description, cargo_license = parse_cargo_toml(cargo)
  if not name or not version or not description:
    print("ERROR: Cannot find name, version, or description in", cargo)
    return
  print("### Cargo.toml license:", cargo_license)
  licenses = decide_license_type(cargo_license)
  preferred_license = licenses[0]
  add_metadata(name, version, description, len(licenses) > 1)
  add_owners()
  add_license(preferred_license.filename)
  add_module_license(preferred_license.type)
  # It is unclear yet if a NOTICE file is required.
  # add_notice()


if __name__ == "__main__":
  main()
