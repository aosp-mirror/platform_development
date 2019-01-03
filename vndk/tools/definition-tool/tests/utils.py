#!/usr/bin/env python3

import os

from vndk_definition_tool import (ELF, ELFLinker, PT_SYSTEM, PT_VENDOR)


class GraphBuilder(object):
    _PARTITION_NAMES = {
        PT_SYSTEM: 'system',
        PT_VENDOR: 'vendor',
    }


    _LIB_DIRS = {
        ELF.ELFCLASS32: 'lib',
        ELF.ELFCLASS64: 'lib64',
    }


    def __init__(self):
        self.graph = ELFLinker()


    def add_lib(self, partition, klass, name, dt_needed=tuple(),
                exported_symbols=tuple(), imported_symbols=tuple(),
                extra_dir=None):
        """Create and add a shared library to ELFLinker."""

        lib_dir = os.path.join('/', self._PARTITION_NAMES[partition],
                               self._LIB_DIRS[klass])
        if extra_dir:
            lib_dir = os.path.join(lib_dir, extra_dir)

        path = os.path.join(lib_dir, name + '.so')

        elf = ELF(klass, ELF.ELFDATA2LSB, dt_needed=dt_needed,
                  exported_symbols=set(exported_symbols),
                  imported_symbols=set(imported_symbols))

        lib = self.graph.add_lib(partition, path, elf)
        setattr(self, name + '_' + elf.elf_class_name, lib)
        return lib


    def add_lib32(self, partition, name, dt_needed=tuple(),
                  exported_symbols=tuple(), imported_symbols=tuple(),
                  extra_dir=None):
        return self.add_lib(partition, ELF.ELFCLASS32, name, dt_needed,
                            exported_symbols, imported_symbols, extra_dir)


    def add_lib64(self, partition, name, dt_needed=tuple(),
                  exported_symbols=tuple(), imported_symbols=tuple(),
                  extra_dir=None):
        return self.add_lib(partition, ELF.ELFCLASS64, name, dt_needed,
                            exported_symbols, imported_symbols, extra_dir)


    def add_multilib(self, partition, name, dt_needed=tuple(),
                     exported_symbols=tuple(), imported_symbols=tuple(),
                     extra_dir=None):
        """Add 32-bit / 64-bit shared libraries to ELFLinker."""
        return (
            self.add_lib(partition, ELF.ELFCLASS32, name, dt_needed,
                         exported_symbols, imported_symbols, extra_dir),
            self.add_lib(partition, ELF.ELFCLASS64, name, dt_needed,
                         exported_symbols, imported_symbols, extra_dir)
        )


    def resolve(self, vndk_lib_dirs=None, ro_vndk_version=None):
        if vndk_lib_dirs is not None:
            self.graph.vndk_lib_dirs = vndk_lib_dirs
        if ro_vndk_version is not None:
            self.graph.ro_vndk_version = ro_vndk_version
        self.graph.resolve_deps()
