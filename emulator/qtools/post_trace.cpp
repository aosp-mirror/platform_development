#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "trace_reader.h"

typedef struct MyStaticRec {
  StaticRec bb;
  uint32_t  *insns;
} MyStaticRec;

const int kNumPids = 32768;
char usedPids[kNumPids];

int main(int argc, char **argv) {
  uint32_t insns[kMaxInsnPerBB];

  if (argc != 2) {
    fprintf(stderr, "Usage: %s trace_file\n", argv[0]);
    exit(1);
  }

  char *trace_filename = argv[1];
  TraceReaderBase *trace = new TraceReaderBase;
  trace->SetPostProcessing(true);
  trace->Open(trace_filename);

  // Count the number of static basic blocks and instructions.
  uint64_t num_static_bb = 0;
  uint64_t num_static_insn = 0;
  while (1) {
    StaticRec static_rec;

    if (trace->ReadStatic(&static_rec))
      break;
    if (static_rec.bb_num != num_static_bb) {
      fprintf(stderr,
              "Error: basic block numbers out of order; expected %lld, got %lld\n",
              num_static_bb, static_rec.bb_num);
      exit(1);
    }
    num_static_bb += 1;
    num_static_insn += static_rec.num_insns;
    trace->ReadStaticInsns(static_rec.num_insns, insns);
  }
  trace->Close();

  // Allocate space for all of the static blocks
  MyStaticRec *blocks = new MyStaticRec[num_static_bb];

  // Read the static blocks again and save pointers to them
  trace->Open(trace_filename);
  for (uint32_t ii = 0; ii < num_static_bb; ++ii) {
    trace->ReadStatic(&blocks[ii].bb);
    uint32_t num_insns = blocks[ii].bb.num_insns;
    if (num_insns > 0) {
      blocks[ii].insns = new uint32_t[num_insns];
      trace->ReadStaticInsns(num_insns, blocks[ii].insns);
    }
  }

  // Check the last basic block.  If it contains a special undefined
  // instruction, then truncate the basic block at that point.
  uint32_t num_insns = blocks[num_static_bb - 1].bb.num_insns;
  uint32_t *insn_ptr = blocks[num_static_bb - 1].insns;
  for (uint32_t ii = 0; ii < num_insns; ++ii, ++insn_ptr) {
    if (*insn_ptr == 0xe6c00110) {
      uint32_t actual_num_insns = ii + 1;
      blocks[num_static_bb - 1].bb.num_insns = actual_num_insns;
      num_static_insn -= (num_insns - actual_num_insns);

      // Write the changes back to the trace file
      trace->TruncateLastBlock(actual_num_insns);
      break;
    }
  }
  TraceHeader *header = trace->GetHeader();
  strcpy(header->ident, TRACE_IDENT);
  header->num_static_bb = num_static_bb;
  header->num_dynamic_bb = 0;
  header->num_static_insn = num_static_insn;
  header->num_dynamic_insn = 0;
  trace->WriteHeader(header);

  // Reopen the trace file in order to force the trace manager to reread
  // the static blocks now that we have written that information to the
  // header.
  trace->Close();
  trace->Open(trace_filename);

  // Count the number of dynamic executions of basic blocks and instructions.
  // Also keep track of which process ids are used.
  uint64_t num_dynamic_bb = 0;
  uint64_t num_dynamic_insn = 0;
  while (1) {
    BBEvent event;

    if (trace->ReadBB(&event))
      break;
    if (event.bb_num >= num_static_bb) {
      fprintf(stderr,
              "Error: basic block number (%lld) too large (num blocks: %lld)\n",
              event.bb_num, num_static_bb);
      exit(1);
    }
    usedPids[event.pid] = 1;
    num_dynamic_bb += 1;
    num_dynamic_insn += event.num_insns;
  }

  // Count the number of process ids that are used and remember the first
  // unused pid.
  int numUsedPids = 0;
  int unusedPid = -1;
  for (int pid = 0; pid < kNumPids; pid++) {
      if (usedPids[pid] == 1) {
          numUsedPids += 1;
      } else if (unusedPid == -1) {
          unusedPid = pid;
      }
  }

  // Rewrite the header with the dynamic counts
  header->num_dynamic_bb = num_dynamic_bb;
  header->num_dynamic_insn = num_dynamic_insn;
  header->num_used_pids = numUsedPids;
  header->first_unused_pid = unusedPid;
  trace->WriteHeader(header);
  trace->Close();

  printf("Static basic blocks: %llu, Dynamic basic blocks: %llu\n",
         num_static_bb, num_dynamic_bb);
  printf("Static instructions: %llu, Dynamic instructions: %llu\n",
         num_static_insn, num_dynamic_insn);

  double elapsed_secs = header->elapsed_usecs / 1000000.0;
  double insn_per_sec = 0;
  if (elapsed_secs != 0)
    insn_per_sec = num_dynamic_insn / elapsed_secs;
  const char *suffix = "";
  if (insn_per_sec >= 1000000) {
    insn_per_sec /= 1000000.0;
    suffix = "M";
  } else if (insn_per_sec > 1000) {
    insn_per_sec /= 1000.0;
    suffix = "K";
  }
  printf("Elapsed seconds: %.2f, simulated instructions/sec: %.1f%s\n",
         elapsed_secs, insn_per_sec, suffix);
  return 0;
}
