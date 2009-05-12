// Copyright 2006 The Android Open Source Project

#ifndef PARSE_OPTIONS_INL_H
#define PARSE_OPTIONS_INL_H

// Define a typedef for TraceReaderType and include "parse_options.h"
// before including this header file in a C++ source file.
//
// For example:
// 
// struct symbol {
//   int  elapsed;
// };
// 
// typedef TraceReader<symbol> TraceReaderType;


typedef TraceReaderType::symbol_type symbol_type;
typedef TraceReaderType::region_type region_type;
typedef TraceReaderType::ProcessState ProcessState;

symbol_type *kernel_sym;
symbol_type *library_sym;

// Returns true if the given event is included (or not excluded)
// from the list of valid events specified by the user on the
// command line.
inline bool IsValidEvent(BBEvent *event, symbol_type *sym)
{
  if (include_some_pids && pid_include_vector.GetBit(event->pid) == 0)
      return false;
  if (exclude_some_pids && pid_exclude_vector.GetBit(event->pid))
      return false;
  if (include_some_procedures) {
    if (sym == NULL || included_procedures.Find(sym->name) == 0)
      return false;
  }
  if (exclude_some_procedures) {
    if (sym == NULL || excluded_procedures.Find(sym->name))
      return false;
  }
  return true;
}

inline bool IsValidPid(int pid) {
  if (include_some_pids && pid_include_vector.GetBit(pid) == 0)
    return false;
  if (exclude_some_pids && pid_exclude_vector.GetBit(pid))
    return false;
  return true;
}

inline symbol_type *GetSymbol(TraceReaderType *trace, int pid, uint32_t addr,
                              uint64_t time)
{
  symbol_type *sym = trace->LookupFunction(pid, addr, time);

  if (lump_kernel && (sym->region->flags & region_type::kIsKernelRegion)) {
    if (kernel_sym == NULL) {
      kernel_sym = sym;
      sym->name = ":kernel";
    } else {
      sym = kernel_sym;
    }
  }

  if (lump_libraries && (sym->region->flags & region_type::kIsLibraryRegion)) {
    if (library_sym == NULL) {
      library_sym = sym;
      sym->name = ":libs";
    } else {
      sym = library_sym;
    }
  }

  return sym;
}

inline bool IsIncludedProcedure(symbol_type *sym)
{
  if (include_kernel_syms && (sym->region->flags & region_type::kIsKernelRegion))
    return true;
  if (include_library_syms && (sym->region->flags & region_type::kIsLibraryRegion))
    return true;
  return included_procedures.Find(sym->name);
}

inline bool IsExcludedProcedure(symbol_type *sym)
{
  if (exclude_kernel_syms && (sym->region->flags & region_type::kIsKernelRegion))
    return true;
  if (exclude_library_syms && (sym->region->flags & region_type::kIsLibraryRegion))
    return true;
  return excluded_procedures.Find(sym->name);
}

// Returns true on end-of-file.
inline bool GetNextValidEvent(TraceReaderType *trace,
                              BBEvent *event,
                              BBEvent *first_ignored_event,
                              symbol_type **sym_ptr)
{
  symbol_type *sym = NULL;
  first_ignored_event->time = 0;
  if (trace->ReadBB(event))
    return true;
  bool recheck = true;
  while (recheck) {
    recheck = false;
    if (include_some_pids) {
      while (pid_include_vector.GetBit(event->pid) == 0) {
        if (first_ignored_event->time == 0)
          *first_ignored_event = *event;
        if (trace->ReadBB(event))
          return true;
      }
    } else if (exclude_some_pids) {
      while (pid_exclude_vector.GetBit(event->pid)) {
        if (first_ignored_event->time == 0)
          *first_ignored_event = *event;
        if (trace->ReadBB(event))
          return true;
      }
    }

    if (include_some_procedures) {
      sym = GetSymbol(trace, event->pid, event->bb_addr, event->time);
      while (!IsIncludedProcedure(sym)) {
        if (first_ignored_event->time == 0)
          *first_ignored_event = *event;
        if (trace->ReadBB(event))
          return true;
        recheck = true;
        sym = GetSymbol(trace, event->pid, event->bb_addr, event->time);
      }
    } else if (exclude_some_procedures) {
      sym = GetSymbol(trace, event->pid, event->bb_addr, event->time);
      while (IsExcludedProcedure(sym)) {
        if (first_ignored_event->time == 0)
          *first_ignored_event = *event;
        if (trace->ReadBB(event))
          return true;
        recheck = true;
        sym = GetSymbol(trace, event->pid, event->bb_addr, event->time);
      }
    }
  }
  if (sym == NULL)
    sym = GetSymbol(trace, event->pid, event->bb_addr, event->time);

  *sym_ptr = sym;
  return false;
}

#endif  // PARSE_OPTIONS_INL_H
