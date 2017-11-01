#!/usr/bin/python

import disassemble_tombstone
import disassemble_test_input

for test in disassemble_test_input.tests:
  print test
  for line in disassemble_tombstone.Disassemble(iter(disassemble_test_input.tests[test].splitlines(True))):
    print line,
