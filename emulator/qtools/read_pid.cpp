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
    PidEvent event;
    if (trace->ReadPidEvent(&event))
      break;
    switch (event.rec_type) {
      case kPidFork:
        printf("t%lld fork tgid %d pid %d\n", event.time, event.tgid, event.pid);
        break;
      case kPidClone:
        printf("t%lld clone tgid %d pid %d\n", event.time, event.tgid, event.pid);
        break;
      case kPidSwitch:
        printf("t%lld switch %d\n", event.time, event.pid);
        break;
      case kPidExit:
        printf("t%lld exit %d\n", event.time, event.pid);
        break;
      case kPidMmap:
        printf("t%lld mmap %08x - %08x, offset %08x '%s'\n",
               event.time, event.vstart, event.vend, event.offset, event.path);
        delete[] event.path;
        break;
      case kPidMunmap:
        printf("t%lld munmap %08x - %08x\n",
               event.time, event.vstart, event.vend);
        break;
      case kPidSymbolAdd:
        printf("t%lld add sym %08x '%s'\n",
               event.time, event.vstart, event.path);
        delete[] event.path;
        break;
      case kPidSymbolRemove:
        printf("t%lld remove %08x\n", event.time, event.vstart);
        break;
      case kPidExec:
        printf("t%lld argc: %d\n", event.time, event.argc);
        for (int ii = 0; ii < event.argc; ++ii) {
          printf("  argv[%d]: %s\n", ii, event.argv[ii]);
          delete[] event.argv[ii];
        }
        delete[] event.argv;
        break;
      case kPidKthreadName:
        printf("t%lld kthread tgid %d pid %d %s\n",
               event.time, event.tgid, event.pid, event.path);
        delete[] event.path;
        break;
      case kPidName:
        printf("t%lld name %d %s\n",
               event.time, event.pid, event.path);
        delete[] event.path;
        break;
    }
  }
  return 0;
}
