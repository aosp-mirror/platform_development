// Copyright 2006 The Android Open Source Project

#ifndef PARSE_OPTIONS_H
#define PARSE_OPTIONS_H

#include "bitvector.h"
#include "hash_table.h"

extern const char *root;
extern bool lump_kernel;
extern bool lump_libraries;
extern Bitvector pid_include_vector;
extern Bitvector pid_exclude_vector;
extern bool include_some_pids;
extern bool exclude_some_pids;

extern HashTable<int> excluded_procedures;
extern HashTable<int> included_procedures;
extern bool exclude_some_procedures;
extern bool include_some_procedures;

extern bool exclude_kernel_syms;
extern bool exclude_library_syms;
extern bool include_kernel_syms;
extern bool include_library_syms;
extern bool demangle;

extern void Usage(const char *program);
extern void ParseOptions(int argc, char **argv);
extern void OptionsUsage();

#endif  // PARSE_OPTIONS_H
