#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import unittest

from compat import StringIO
from vndk_definition_tool import Elf_Sym, ELF

class ElfSymTest(unittest.TestCase):
    def setUp(self):
        self.sym_local = Elf_Sym(0, 0, 4, 0, 0, 1)
        self.sym_global = Elf_Sym(0, 0, 4, 17, 0, 1)
        self.sym_weak = Elf_Sym(0, 0, 4, 33, 0, 1)
        self.sym_undef = Elf_Sym(0, 0, 4, 16, 0, 0)

    def test_is_local(self):
        self.assertTrue(self.sym_local.is_local)
        self.assertFalse(self.sym_global.is_local)
        self.assertFalse(self.sym_weak.is_local)

    def test_is_global(self):
        self.assertFalse(self.sym_local.is_global)
        self.assertTrue(self.sym_global.is_global)
        self.assertFalse(self.sym_weak.is_global)

    def test_is_weak(self):
        self.assertFalse(self.sym_local.is_weak)
        self.assertFalse(self.sym_global.is_weak)
        self.assertTrue(self.sym_weak.is_weak)

    def test_is_undef(self):
        self.assertFalse(self.sym_global.is_undef)
        self.assertTrue(self.sym_undef.is_undef)


class ELFTest(unittest.TestCase):
    def test_repr(self):
        elf = ELF()
        self.assertEqual(elf, eval(repr(elf)))

        elf = ELF(ei_class=ELF.ELFCLASS32, ei_data=ELF.ELFDATA2LSB,
                  e_machine=183, dt_rpath='a', dt_runpath='b',
                  dt_needed=['c', 'd'], exported_symbols={'e', 'f', 'g'})
        self.assertEqual(elf, eval(repr(elf)))

    def test_class_name(self):
        self.assertEqual('None', ELF().elf_class_name)

        elf = ELF(ELF.ELFCLASS32)
        self.assertEqual('32', elf.elf_class_name)
        self.assertTrue(elf.is_32bit)
        self.assertFalse(elf.is_64bit)

        elf = ELF(ELF.ELFCLASS64)
        self.assertEqual('64', elf.elf_class_name)
        self.assertFalse(elf.is_32bit)
        self.assertTrue(elf.is_64bit)

    def test_endianness(self):
        self.assertEqual('None', ELF().elf_data_name)
        self.assertEqual('Little-Endian',
                         ELF(None, ELF.ELFDATA2LSB).elf_data_name)
        self.assertEqual('Big-Endian',
                         ELF(None, ELF.ELFDATA2MSB).elf_data_name)

    def test_machine_name(self):
        self.assertEqual('EM_NONE', ELF(e_machine=0).elf_machine_name)
        self.assertEqual('EM_386', ELF(e_machine=3).elf_machine_name)
        self.assertEqual('EM_MIPS', ELF(e_machine=8).elf_machine_name)
        self.assertEqual('EM_ARM', ELF(e_machine=40).elf_machine_name)
        self.assertEqual('EM_X86_64', ELF(e_machine=62).elf_machine_name)
        self.assertEqual('EM_AARCH64', ELF(e_machine=183).elf_machine_name)

    def test_dt_rpath_runpath(self):
        elf = ELF()
        self.assertEqual(None, elf.dt_rpath)
        self.assertEqual(None, elf.dt_runpath)

        elf = ELF(None, None, 0, 'a', 'b')
        self.assertEqual('a', elf.dt_rpath)
        self.assertEqual('b', elf.dt_runpath)

    def test_dump(self):
        elf = ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB, 183, 'a', 'b',
                  ['libc.so', 'libm.so'], {'hello', 'world'}, {'d', 'e'})

        with StringIO() as f:
            elf.dump(f)
            actual_output = f.getvalue()

        self.assertEqual('EI_CLASS\t32\n'
                         'EI_DATA\t\tLittle-Endian\n'
                         'E_MACHINE\tEM_AARCH64\n'
                         'DT_RPATH\ta\n'
                         'DT_RUNPATH\tb\n'
                         'DT_NEEDED\tlibc.so\n'
                         'DT_NEEDED\tlibm.so\n'
                         'EXP_SYMBOL\thello\n'
                         'EXP_SYMBOL\tworld\n'
                         'IMP_SYMBOL\td\n'
                         'IMP_SYMBOL\te\n',
                         actual_output)

    def test_dump_exported_symbols(self):
        elf = ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB, 183, 'a', 'b',
                  ['libc.so', 'libm.so'], {'hello', 'world'})

        with StringIO() as f:
            elf.dump_exported_symbols(f)
            actual_output = f.getvalue()

        self.assertEqual('hello\nworld\n', actual_output)

if __name__ == '__main__':
    unittest.main()
