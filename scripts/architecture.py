"""Abstraction layer for different ABIs."""

import re
import symbol

def UnpackLittleEndian(word):
  """Split a hexadecimal string in little endian order."""
  return [word[x:x+2] for x in range(len(word) - 2, -2, -2)]


ASSEMBLE = 'as'
DISASSEMBLE = 'objdump'
LINK = 'ld'
UNPACK = 'unpack'

OPTIONS = {
    'x86': {
        ASSEMBLE: ['--32'],
        LINK: ['-melf_i386']
    }
}


class Architecture(object):
  """Creates an architecture abstraction for a given ABI.

  Args:
    name: The abi name, as represented in a tombstone.
  """

  def __init__(self, name):
    symbol.ARCH = name
    self.toolchain = symbol.FindToolchain()
    self.options = OPTIONS.get(name, {})

  def Assemble(self, args):
    """Generates an assembler command, appending the given args."""
    return [symbol.ToolPath(ASSEMBLE)] + self.options.get(ASSEMBLE, []) + args

  def Link(self, args):
    """Generates a link command, appending the given args."""
    return [symbol.ToolPath(LINK)] + self.options.get(LINK, []) + args

  def Disassemble(self, args):
    """Generates a disassemble command, appending the given args."""
    return ([symbol.ToolPath(DISASSEMBLE)] + self.options.get(DISASSEMBLE, []) +
            args)

  def WordToBytes(self, word):
    """Unpacks a hexadecimal string in the architecture's byte order.

    Args:
      word: A string representing a hexadecimal value.

    Returns:
      An array of hexadecimal byte values.
    """
    return self.options.get(UNPACK, UnpackLittleEndian)(word)
