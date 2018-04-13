import os
import json
import xml.etree.ElementTree as ET
import sys


STATIC_FILENAME = "tmp.xml"
KNOWN_FAILING_FILENAME = "known_failing_names.txt"


class FileDownloader(object):

  def __init__(self, file_url):
    self.file_url = file_url

  def __enter__(self):
    os.system("wget -O %s %s" % (STATIC_FILENAME, self.file_url))
    os.system("touch %s" % KNOWN_FAILING_FILENAME)

  def __exit__(self, *args):
    os.system("rm %s" % STATIC_FILENAME)


class TestCase(object):

  def __init__(self, xml_test):
    self._xml_test = xml_test

  @property
  def name(self):
    return self._xml_test.get("name")

  @property
  def passed(self):
    return self._xml_test.get("result") == "pass"

  def __repr__(self):
    return "%s: %s" % (self.name, self.passed)


def generate_tests_from_file(fname):
  tree = ET.parse(fname)
  result = tree.getroot()
  for module in result:
    for test_case in module:
      for test in test_case:
        yield TestCase(test)


def get_failing_tests(fname):
  return filter(
    lambda test: not test.passed,
    generate_tests_from_file(fname),
  )


def get_failing_test_names(fname):
  return map(
    lambda test: test.name,
    get_failing_tests(fname),
  )


def get_known_failing_names():
  try:
    with open(KNOWN_FAILING_FILENAME, "rb") as f:
      return [line for line in f.read().splitlines() if line]
  except IOError:
    return []


def new_test_failures(failing_test_names, known_failing_test_names):
  return sorted(
    list(
      set(failing_test_names) - set(known_failing_test_names),
    ),
  )


if __name__ == "__main__":
  print "Any known test failures can be added to %s as a new line" % KNOWN_FAILING_FILENAME
  try:
    input_test_url = sys.argv[1]
  except IndexError:
    print "Supply *result_*.xml URL from a build from https://partner.android.com/"
    sys.exit(1)

  with FileDownloader(input_test_url):
    print "Current failing tests"
    print json.dumps(
      sorted(
        new_test_failures(
          get_failing_test_names(STATIC_FILENAME),
          get_known_failing_names(),
        ),
      ),
      indent=4,
    )
