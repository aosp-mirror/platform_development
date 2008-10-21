// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "gtrace.h"
#include "bitvector.h"
#include "parse_options.h"

struct symbol {
  int		filenum;	// the file number (for gtrace)
  int		procnum;	// the procedure number (for gtrace)
};

typedef TraceReader<symbol> TraceReaderType;

#include "parse_options-inl.h"

const int kMaxProcNum = 4095;
int next_filenum = 1;
int next_procnum = 1;

void Usage(const char *program)
{
  fprintf(stderr, "Usage: %s [options] trace_file elf_file gtrace_file\n",
          program);
  OptionsUsage();
}

int main(int argc, char **argv)
{
  ParseOptions(argc, argv);
  if (argc - optind != 3) {
    Usage(argv[0]);
    exit(1);
  }

  char *qemu_trace_file = argv[optind++];
  char *elf_file = argv[optind++];
  char *gtrace_file = argv[optind++];
  TraceReader<symbol> *trace = new TraceReader<symbol>;
  trace->Open(qemu_trace_file);
  trace->ReadKernelSymbols(elf_file);
  trace->SetRoot(root);
  TraceHeader *qheader = trace->GetHeader();

  // Get the first valid event to get the process id for the gtrace header.
  BBEvent event;
  BBEvent ignored;
  symbol_type *sym;
  if (GetNextValidEvent(trace, &event, &ignored, &sym))
    return 0;

  Gtrace *gtrace = new Gtrace;
  gtrace->Open(gtrace_file, qheader->pdate, qheader->ptime);
  gtrace->WriteFirstHeader(qheader->start_sec, event.pid);

  symbol_type *prev_sym = NULL;
  bool eof = false;
  while (!eof) {
    if (sym != prev_sym) {
      // This procedure is different from the previous procedure.

      // If we have never seen this symbol before, then add it to the
      // list of known procedures.
      if (sym->filenum == 0) {
        sym->filenum = next_filenum;
        sym->procnum = next_procnum;
        gtrace->AddProcedure(sym->filenum, sym->procnum, sym->name);
        next_procnum += 1;
        if (next_procnum > kMaxProcNum) {
          next_filenum += 1;
          next_procnum = 1;
        }
      }

      // If we haven't yet recorded the procedure exit for the previous
      // procedure, then do it now.
      if (prev_sym) {
        gtrace->AddProcExit(prev_sym->filenum, prev_sym->procnum, event.time,
                            event.pid);
      }

      // If this is not the terminating record, then record a procedure
      // entry.
      if (event.bb_num != 0) {
        gtrace->AddProcEntry(sym->filenum, sym->procnum, event.time, event.pid);
        prev_sym = sym;
      }
    }

    eof = GetNextValidEvent(trace, &event, &ignored, &sym);
    if (ignored.time != 0 && prev_sym) {
      // We read an event that we are ignoring.
      // If we haven't already recorded a procedure exit, then do so.
      gtrace->AddProcExit(prev_sym->filenum, prev_sym->procnum, ignored.time,
                          ignored.pid);
      prev_sym = NULL;
    }
  }

  delete gtrace;
  delete trace;
  return 0;
}
