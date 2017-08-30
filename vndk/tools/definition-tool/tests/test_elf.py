#!/usr/bin/env python3

from __future__ import print_function

import os
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import tempfile
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
    def test_get_ei_class_from_name(self):
        self.assertEqual(ELF.ELFCLASS32, ELF.get_ei_class_from_name('32'))
        self.assertEqual(ELF.ELFCLASS64, ELF.get_ei_class_from_name('64'))

    def test_get_ei_data_from_name(self):
        self.assertEqual(ELF.ELFDATA2LSB,
                         ELF.get_ei_data_from_name('Little-Endian'))
        self.assertEqual(ELF.ELFDATA2MSB,
                         ELF.get_ei_data_from_name('Big-Endian'))

    def test_get_e_machine_from_name(self):
        self.assertEqual(0, ELF.get_e_machine_from_name('EM_NONE'))
        self.assertEqual(3, ELF.get_e_machine_from_name('EM_386'))
        self.assertEqual(8, ELF.get_e_machine_from_name('EM_MIPS'))
        self.assertEqual(40, ELF.get_e_machine_from_name('EM_ARM'))
        self.assertEqual(62, ELF.get_e_machine_from_name('EM_X86_64'))
        self.assertEqual(183, ELF.get_e_machine_from_name('EM_AARCH64'))

    def test_repr(self):
        elf = ELF()
        self.assertEqual(elf, eval(repr(elf)))

        elf = ELF(ei_class=ELF.ELFCLASS32, ei_data=ELF.ELFDATA2LSB,
                  e_machine=183, dt_rpath=['a'], dt_runpath=['b'],
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
        self.assertEqual([], elf.dt_rpath)
        self.assertEqual([], elf.dt_runpath)

        elf = ELF(None, None, 0, ['a'], ['b'])
        self.assertEqual(['a'], elf.dt_rpath)
        self.assertEqual(['b'], elf.dt_runpath)

    def test_dump(self):
        elf = ELF(ELF.ELFCLASS32, ELF.ELFDATA2LSB, 183, ['a'], ['b'],
                  ['libc.so', 'libm.so'], {'hello', 'world'}, {'d', 'e'})

        f = StringIO()
        elf.dump(f)
        actual_output = f.getvalue()

        self.assertEqual('EI_CLASS\t32\n'
                         'EI_DATA\t\tLittle-Endian\n'
                         'E_MACHINE\tEM_AARCH64\n'
                         'FILE_SIZE\t0\n'
                         'RO_SEG_FILE_SIZE\t0\n'
                         'RO_SEG_MEM_SIZE\t0\n'
                         'RW_SEG_FILE_SIZE\t0\n'
                         'RW_SEG_MEM_SIZE\t0\n'
                         'DT_RPATH\ta\n'
                         'DT_RUNPATH\tb\n'
                         'DT_NEEDED\tlibc.so\n'
                         'DT_NEEDED\tlibm.so\n'
                         'EXP_SYMBOL\thello\n'
                         'EXP_SYMBOL\tworld\n'
                         'IMP_SYMBOL\td\n'
                         'IMP_SYMBOL\te\n',
                         actual_output)

    def test_parse_dump_file(self):
        data = ('EI_CLASS\t64\n'
                'EI_DATA\t\tLittle-Endian\n'
                'E_MACHINE\tEM_AARCH64\n'
                'FILE_SIZE\t90\n'
                'RO_SEG_FILE_SIZE\t18\n'
                'RO_SEG_MEM_SIZE\t24\n'
                'RW_SEG_FILE_SIZE\t42\n'
                'RW_SEG_MEM_SIZE\t81\n'
                'DT_RPATH\trpath_1\n'
                'DT_RPATH\trpath_2\n'
                'DT_RUNPATH\trunpath_1\n'
                'DT_RUNPATH\trunpath_2\n'
                'DT_NEEDED\tlibc.so\n'
                'DT_NEEDED\tlibm.so\n'
                'EXP_SYMBOL\texported_1\n'
                'EXP_SYMBOL\texported_2\n'
                'IMP_SYMBOL\timported_1\n'
                'IMP_SYMBOL\timported_2\n')

        def check_parse_dump_file_result(res):
            self.assertEqual(ELF.ELFCLASS64, res.ei_class)
            self.assertEqual(ELF.ELFDATA2LSB, res.ei_data)
            self.assertEqual(183, res.e_machine)
            self.assertEqual(90, res.file_size)
            self.assertEqual(18, res.ro_seg_file_size)
            self.assertEqual(24, res.ro_seg_mem_size)
            self.assertEqual(42, res.rw_seg_file_size)
            self.assertEqual(81, res.rw_seg_mem_size)
            self.assertEqual(['rpath_1', 'rpath_2'], res.dt_rpath)
            self.assertEqual(['runpath_1', 'runpath_2'], res.dt_runpath)
            self.assertEqual(['libc.so', 'libm.so'], res.dt_needed)
            self.assertSetEqual({'exported_1', 'exported_2'},
                                res.exported_symbols)
            self.assertSetEqual({'imported_1', 'imported_2'},
                                res.imported_symbols)

        # Parse ELF dump from the string buffer.
        check_parse_dump_file_result(ELF.load_dumps(data))

        # Parse ELF dump from the given file path.
        with tempfile.NamedTemporaryFile('w+') as f:
            f.write(data)
            f.flush()
            f.seek(0)

            check_parse_dump_file_result(ELF.load_dump(f.name))


if __name__ == '__main__':
    unittest.main()
