#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "armdis.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

static const uint32_t kOffsetThreshold = 0x100000;

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_file elf_file\n", program);
    OptionsUsage();
}

int main(int argc, char **argv) {
    // Parse the options
    ParseOptions(argc, argv);
    if (argc - optind != 2) {
        Usage(argv[0]);
        exit(1);
    }

    char *trace_filename = argv[optind++];
    char *elf_file = argv[optind++];
    TraceReader<> *trace = new TraceReader<>;
    trace->Open(trace_filename);
    trace->ReadKernelSymbols(elf_file);
    trace->SetRoot(root);

    while (1) {
        symbol_type *sym;
        BBEvent event;
        BBEvent ignored;

        if (GetNextValidEvent(trace, &event, &ignored, &sym))
            break;
        if (event.bb_num == 0)
            break;
        //printf("t%llu bb %lld %d\n", event.time, event.bb_num, event.num_insns);
        uint64_t insn_time = trace->ReadInsnTime(event.time);
        if (insn_time != event.time) {
            printf("time: %llu insn time: %llu bb: %llu addr: 0x%x num_insns: %d, pid: %d\n",
                   event.time, insn_time, event.bb_num, event.bb_addr,
                   event.num_insns, event.pid);
            exit(1);
        }
        for (int ii = 1; ii < event.num_insns; ++ii) {
            trace->ReadInsnTime(event.time);
        }
    }

    delete trace;
    return 0;
}
