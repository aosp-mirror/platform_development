#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_file\n", program);
    OptionsUsage();
}

int main(int argc, char **argv) {
    // Parse the options
    ParseOptions(argc, argv);
    if (argc - optind != 1) {
        Usage(argv[0]);
        exit(1);
    }

    char *trace_filename = argv[optind];
    TraceReader<> *trace = new TraceReader<>;
    trace->Open(trace_filename);
    trace->SetRoot(root);

    while (1) {
        BBEvent event, ignored;
        symbol_type *dummy_sym;

        if (GetNextValidEvent(trace, &event, &ignored, &dummy_sym))
            break;
    }

    int num_procs;
    ProcessState *processes = trace->GetProcesses(&num_procs);

    ProcessState *pstate = &processes[0];
    for (int ii = 0; ii < num_procs; ++ii, ++pstate) {
        if (pstate->name == NULL)
            pstate->name = "";
        ProcessState *manager = pstate->addr_manager;
        printf("pid %d regions: %d %s",
               pstate->pid, manager->nregions, pstate->name);
        for (int jj = 1; jj < pstate->argc; ++jj) {
            printf(" %s", pstate->argv[jj]);
        }
        printf("\n");
        trace->DumpRegions(stdout, pstate);
    }

    delete trace;
    return 0;
}
