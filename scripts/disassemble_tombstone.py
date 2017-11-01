#!/usr/bin/python

"""Disassemble the code stored in a tombstone.

The classes in this module use an interface, ProcessLine, so that they can be
chained together to do arbitrary procerssing. The current classes support
disassembling the bytes embedded in tombstones and printing output to stdout.
"""


import re
import subprocess
import sys
import tempfile
import architecture


STANDARD_PROLOGUE = """
       .type   _start, %function
       .globl  _start
_start:
"""


THUMB_PROLOGUE = STANDARD_PROLOGUE + """
       .code   16
       .thumb_func
       .type   thumb_start, %function
thumb_start:
"""


def Disassemble(line_generator):
  abi_line = re.compile("(ABI: \'(.*)\')")
  abi = None
  tools = None
  # Process global headers
  for line in line_generator:
    yield line
    abi_header = abi_line.search(line)
    if abi_header:
      abi = abi_header.group(2)
      # Look up the tools here so we don't do a lookup for each code block.
      tools = architecture.Architecture(abi)
      break
  # The rest of the file consists of:
  #   o Lines that should pass through unchanged
  #   o Blocks of register values, which follow a 'pid: ...' line and end with
  #     'backtrace:' line
  #   o Blocks of code represented as words, which start with 'code around ...'
  #     and end with a line that doesn't look like a list of words.
  #
  # The only constraint on the ordering of these blocks is that the register
  # values must come before the first code block.
  #
  # It's easiest to nest register processing in the codeblock search loop.
  register_list_re = re.compile('^pid: ')
  codeblock_re = re.compile('^code around ([a-z0-9]+)')
  register_text = {}
  for line in line_generator:
    yield line
    if register_list_re.search(line):
      register_text = {}
      for output in ProcessRegisterList(line_generator, register_text):
        yield output
    code_match = codeblock_re.search(line)
    if code_match:
      for output in ProcessCodeBlock(
          abi, tools, code_match.group(1), register_text, line_generator):
        yield output


def ProcessRegisterList(line_generator, rval):
  for line in line_generator:
    yield line
    if line.startswith('backtrace:'):
      return
    # The register list is indented and consists of alternating name, value
    # pairs.
    if line.startswith(' '):
      words = line.split()
      assert len(words) % 2 == 0
      for index in range(0, len(words), 2):
        rval[words[index]] = words[index + 1]


def ProcessCodeBlock(abi, tools, register_name, register_text, line_generator):
  program_counter = register_text[register_name]
  program_counter_val = int(program_counter, 16)
  scratch_file = tempfile.NamedTemporaryFile(suffix='.s')
  # ARM code comes in two flavors: arm and thumb. Figure out the one
  # to use by peeking in the cpsr.
  if abi == 'arm' and int(register_text['cpsr'], 16) & 0x20:
    scratch_file.write(THUMB_PROLOGUE)
  else:
    scratch_file.write(STANDARD_PROLOGUE)
  # Retains the hexadecimal text for the start of the block
  start_address = None
  # Maintains a numeric counter for the address of the current byte
  current_address = None
  # Handle the 3 differnt file formats that we've observerd.
  if len(program_counter) == 8:
    block_line_len = [67]
    block_num_words = 4
  else:
    assert len(program_counter) == 16
    block_line_len = [57, 73]
    block_num_words = 2
  # Now generate assembly from the bytes in the code block.
  for line in line_generator:
    words = line.split()
    # Be conservative and stop interpreting if the line length is wrong
    # We can't count words because spaces can appear in the text representation
    # of the memory.
    if len(line) not in block_line_len:
      break
    # Double check the address at the start of each line
    if current_address is None:
      start_address = words[0]
      current_address = int(start_address, 16)
    else:
      assert current_address == int(words[0], 16)
    for word in words[1:block_num_words+1]:
      # Handle byte swapping
      for byte in tools.WordToBytes(word):
        # Emit a label at the desired program counter.
        # This will cause the disassembler to resynchronize at this point,
        # allowing us to position the arrow and also ensuring that we decode
        # the instruction properly.
        if current_address == program_counter_val:
          scratch_file.write('program_counter_was_here:\n')
        scratch_file.write('  .byte 0x%s\n' % byte)
        current_address += 1
  scratch_file.flush()
  # Assemble the scratch file and relocate it to the block address with the
  # linker.
  object_file = tempfile.NamedTemporaryFile(suffix='.o')
  subprocess.check_call(tools.Assemble([
      '-o', object_file.name, scratch_file.name]))
  scratch_file.close()
  linked_file = tempfile.NamedTemporaryFile(suffix='.o')
  cmd = tools.Link([
      '-Ttext', '0x' + start_address, '-o', linked_file.name, object_file.name])
  subprocess.check_call(cmd)
  object_file.close()
  disassembler = subprocess.Popen(tools.Disassemble([
      '-S', linked_file.name]), stdout=subprocess.PIPE)
  # Skip some of the annoying assembler headers.
  emit = False
  start_pattern = start_address + ' '
  # objdump padding varies between 32 bit and 64 bit architectures
  arrow_pattern = re.compile('^[ 0]*%8x:\t' % program_counter_val)
  for line in disassembler.stdout:
    emit = emit or line.startswith(start_pattern)
    if emit and len(line) > 1 and line.find('program_counter_was_here') == -1:
      if arrow_pattern.search(line):
        yield '--->' + line
      else:
        yield '    ' + line
  linked_file.close()
  yield '\n'


def main(argv):
  for fn in argv[1:]:
    for line in Disassemble(open(fn, 'r')):
      print line,


if __name__ == '__main__':
  main(sys.argv)
