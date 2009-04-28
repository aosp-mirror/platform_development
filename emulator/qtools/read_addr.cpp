#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "trace_reader.h"

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "Usage: %s trace_file\n", argv[0]);
    exit(1);
  }

  char *trace_filename = argv[1];
  TraceReaderBase *trace = new TraceReaderBase;
  trace->Open(trace_filename);

  while (1) {
    uint64_t time;
    uint32_t addr;
    int flags;

    if (trace->ReadAddr(&time, &addr, &flags))
      break;
    const char *op = "ld";
    if (flags == 1)
        op = "st";
    printf("%lld 0x%08x %s\n", time, addr, op);
  }
  return 0;
}
