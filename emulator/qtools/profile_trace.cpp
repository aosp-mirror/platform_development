#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "trace_reader.h"
#include "parse_options.h"

const int kMillion = 1000000;
const int kMHz = 200 * kMillion;

struct symbol {
    int         count;      // number of times a function is executed
    uint64_t    elapsed;    // elapsed time for this function
};

typedef TraceReader<symbol> TraceReaderType;

#include "parse_options-inl.h"

static const uint32_t kOffsetThreshold = 0x100000;

// This comparison function is called from qsort() to sort
// symbols into decreasing elapsed time.
int cmp_sym_elapsed(const void *a, const void *b) {
    const symbol_type *syma, *symb;
    uint64_t elapsed1, elapsed2;

    syma = static_cast<symbol_type const *>(a);
    symb = static_cast<symbol_type const *>(b);
    elapsed1 = syma->elapsed;
    elapsed2 = symb->elapsed;
    if (elapsed1 < elapsed2)
        return 1;
    if (elapsed1 == elapsed2)
        return strcmp(syma->name, symb->name);
    return -1;
}

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_file elf_file\n", program);
    OptionsUsage();
}

int main(int argc, char **argv)
{
    ParseOptions(argc, argv);
    if (argc - optind != 2) {
        Usage(argv[0]);
        exit(1);
    }

    char *trace_filename = argv[optind++];
    char *elf_file = argv[optind++];
    TraceReader<symbol> *trace = new TraceReader<symbol>;
    trace->Open(trace_filename);
    trace->SetDemangle(demangle);
    trace->ReadKernelSymbols(elf_file);
    trace->SetRoot(root);

    symbol_type dummy;
    dummy.count = 0;
    dummy.elapsed = 0;
    symbol_type *prev_sym = &dummy;
    uint64_t prev_bb_time = 0;
    while (1) {
        symbol_type *sym;
        BBEvent event;
        BBEvent first_ignored_event;

        bool eof = GetNextValidEvent(trace, &event, &first_ignored_event, &sym);

        // Assign the elapsed time to the previous function symbol
        uint64_t elapsed = 0;
        if (first_ignored_event.time != 0)
            elapsed = first_ignored_event.time - prev_bb_time;
        else if (!eof)
            elapsed = event.time - prev_bb_time;
        prev_sym->elapsed += elapsed;

        if (eof)
            break;

        prev_bb_time = event.time;
        sym->count += 1;
        prev_sym = sym;
#if 0
        printf("t%lld bb_num: %d, bb_addr: 0x%x func: %s, addr: 0x%x, count: %d\n",
               bb_time, bb_num, bb_addr, sym->name, sym->addr, sym->count);
#endif
    }

    int nsyms;
    symbol_type *syms = trace->GetSymbols(&nsyms);

    // Sort the symbols into decreasing order of elapsed time
    qsort(syms, nsyms, sizeof(symbol_type), cmp_sym_elapsed);

    // Add up all the cycles
    uint64_t total = 0;
    symbol_type *sym = syms;
    for (int ii = 0; ii < nsyms; ++ii, ++sym) {
        total += sym->elapsed;
    }

    double secs = 1.0 * total / kMHz;
    printf("Total seconds: %.2f, total cycles: %lld, MHz: %d\n\n",
           secs, total, kMHz / kMillion);

    uint64_t sum = 0;
    printf("Elapsed secs Elapsed cyc      %%      %%    Function\n");
    sym = syms;
    for (int ii = 0; ii < nsyms; ++ii, ++sym) {
        if (sym->elapsed == 0)
            break;
        sum += sym->elapsed;
        double per = 100.0 * sym->elapsed / total;
        double sum_per = 100.0 * sum / total;
        double secs = 1.0 * sym->elapsed / kMHz;
        const char *ksym = " ";
        if (sym->region->flags & region_type::kIsKernelRegion)
            ksym = "k";
        printf("%12.2f %11lld %6.2f %6.2f  %s %s\n",
               secs, sym->elapsed, per, sum_per, ksym, sym->name);
    }
    delete[] syms;
    delete trace;

    return 0;
}
