#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "trace_reader_base.h"

int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s trace_file\n", argv[0]);
        exit(1);
    }

    char *trace_filename = argv[1];
    TraceReaderBase *trace = new TraceReaderBase;
    trace->Open(trace_filename);

    while (1) {
        uint64_t time, recnum, bb_num, bb_start_time;
        uint32_t pc, target_pc;
        int num_insns;

        if (trace->ReadExc(&time, &pc, &recnum, &target_pc, &bb_num,
                           &bb_start_time, &num_insns))
            break;
        printf("time: %lld rec: %llu pc: %08x target: %08x bb: %llu bb_start: %llu insns: %d\n",
               time, recnum, pc, target_pc, bb_num, bb_start_time, num_insns);
    }
    return 0;
}
