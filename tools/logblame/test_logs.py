#!/usr/bin/env python2.7 -B

import logs
import ps

import datetime
import StringIO

def test_empty():
  """Test parsing no tag and no text, not well formed."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "",
      "") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/ ]

"""
  check_parsing(expected, text)


def test_none():
  """Test parsing no tag and no text."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "",
      "") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/ ]
"""
  check_parsing(expected, text)



def test_trailing_blank():
  """Test parsing text containing an extra intended newline at the end."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "Newline after\n") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]
Newline after


"""
  check_parsing(expected, text)


def test_blank_between():
  """Test parsing text containing a newline in the middle."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "Message\n\nNewline between") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]
Message

Newline between

"""
  check_parsing(expected, text)


def test_preceeding_blank():
  """Test parsing text containing a newline then text."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "\nNewline before") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]

Newline before

"""
  check_parsing(expected, text)


def test_one_blank():
  """Test parsing text one blank line."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "\n") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]


"""
  check_parsing(expected, text)


def test_two_blanks():
  """Test parsing text two blank lines."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "\n\n") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]



"""
  check_parsing(expected, text)


def test_two_lines_noblanks():
  """Test parsing two lines of text with no blank lines."""
  expected = [ logs.LogLine(None, "03-29 00:46:58.872", "1000", "1815", "1816", "I", "abcd",
      "One\nTwo") ]
  text = """[ 03-29 00:46:58.872  1000: 1815: 1816 I/abcd ]
One
Two

"""
  check_parsing(expected, text)


def test_chatty():
  """Test a log with chatty identical messages."""

  expected = [
      logs.LogLine("system", "03-29 00:46:58.857", "1000", "1815", "1816", "I", "Noisy", "Message"),
      logs.LogLine("system", "03-29 00:46:58.858", "1000", "1815", "1816", "I", "Noisy", "Message"),
      logs.LogLine("system", "03-29 00:46:58.858", "1000", "1815", "1816", "I", "Noisy", "Message"),
      logs.LogLine("system", "03-29 00:46:58.858", "1000", "1815", "1816", "I", "Noisy", "Message"),
      logs.LogLine("system", "03-29 00:46:58.859", "1000", "1815", "1816", "I", "Noisy", "Message"),
      ]
  text = """--------- beginning of system
[ 03-29 00:46:58.857  1000: 1815: 1816 I/Noisy ]
Message

[ 03-29 00:46:58.858  1000: 1815: 1816 I/chatty ]
uid=1000(system) Thread-6 identical 3 lines

[ 03-29 00:46:58.859  1000: 1815: 1816 I/Noisy ]
Message

"""
  check_parsing(expected, text)



def test_normal():
  """Test a realistic (albeit short) log."""
  expected = [
      logs.LogLine("system", "03-29 00:46:58.857", "1000", "1815", "1816", "I", "Package: ]Manager",
        "/system/app/KeyChain changed; collecting certs"),
      logs.LogLine("system", "03-29 00:46:58.872", "1000", "1815", "1816", "I", "PackageManager",
        "/system/app/HiddenMenu changed; collecting certs"),
      logs.LogLine("main", "03-29 00:46:58.872", "1000", "1815", "1816", "I", "PackageManager",
        "/system/app/HiddenMenu changed; collecting certs"),
  ]
  
  text = """--------- beginning of system
[ 03-29 00:46:58.857  1000: 1815: 1816 I/Package: ]Manager ]
/system/app/KeyChain changed; collecting certs

[ 03-29 00:46:58.872  1000: 1815: 1816 I/PackageManager ]
/system/app/HiddenMenu changed; collecting certs

--------- switch to main
[ 03-29 00:46:58.872  1000: 1815: 1816 I/PackageManager ]
/system/app/HiddenMenu changed; collecting certs

"""
  check_parsing(expected, text)



def check_parsing(expected, text):
  """Parse the text and see if it parsed as expected."""
  processes = ps.ProcessSet()
  result = [x for x in logs.ParseLogcat(StringIO.StringIO(text), processes)]
  if result != expected:
    raise Exception("test failed.\nexpected:\n[%s]\nactual\n[%s]" % (
        ", ".join([str(r) for r in expected]),
        ", ".join([str(r) for r in result])))


def main():
  test_empty()
  test_none()
  test_trailing_blank()
  test_blank_between()
  test_preceeding_blank()
  test_one_blank()
  test_two_blanks()
  test_chatty()
  test_normal()


if __name__ == "__main__":
    main()


# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab:
