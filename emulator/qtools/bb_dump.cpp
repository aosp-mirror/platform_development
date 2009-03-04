#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

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

    printf("#  time   bb   pid num_insns  bb_addr\n");
    while (1) {
        symbol_type *sym;
        BBEvent event;
        BBEvent ignored;

        if (GetNextValidEvent(trace, &event, &ignored, &sym))
            break;
        printf("%7lld %4lld %5d       %3d  0x%08x %s\n",
               event.time, event.bb_num, event.pid, event.num_insns,
               event.bb_addr, sym->name);
    }
    return 0;
}
