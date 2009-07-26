#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "trace_reader.h"
#include "parse_options.h"
#include "opcode.h"

const int kMillion = 1000000;
const int kMHz = 200 * kMillion;

struct symbol {
    int    numCalls;    // number of times this function is called
};

typedef TraceReader<symbol> TraceReaderType;

#include "parse_options-inl.h"
#include "callstack.h"

class MyFrame : public StackFrame<symbol_type> {
  public:
    void    push(int stackLevel, uint64_t time, CallStackBase *base) {
        function->numCalls += 1;
    }
    void    pop(int stackLevel, uint64_t time, CallStackBase *base) {
    }
};

typedef CallStack<MyFrame> CallStackType;

static const int kNumStackFrames = 500;
static const int kMaxThreads = (32 * 1024);
CallStackType *stacks[kMaxThreads];

// This comparison function is called from qsort() to sort symbols
// into decreasing number of calls.
int cmp_sym_calls(const void *a, const void *b) {
    const symbol_type *syma, *symb;
    uint64_t calls1, calls2;

    syma = static_cast<symbol_type const *>(a);
    symb = static_cast<symbol_type const *>(b);
    calls1 = syma->numCalls;
    calls2 = symb->numCalls;
    if (calls1 < calls2)
        return 1;
    if (calls1 == calls2) {
        int cmp = strcmp(syma->name, symb->name);
        if (cmp == 0)
            cmp = strcmp(syma->region->path, symb->region->path);
        return cmp;
    }
    return -1;
}

// This comparison function is called from qsort() to sort symbols
// into alphabetical order.
int cmp_sym_names(const void *a, const void *b) {
    const symbol_type *syma, *symb;

    syma = static_cast<symbol_type const *>(a);
    symb = static_cast<symbol_type const *>(b);
    int cmp = strcmp(syma->region->path, symb->region->path);
    if (cmp == 0)
        cmp = strcmp(syma->name, symb->name);
    return cmp;
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

    BBEvent event;
    while (1) {
        BBEvent ignored;
        symbol_type *function;

        if (GetNextValidEvent(trace, &event, &ignored, &function))
            break;
        if (event.bb_num == 0)
            break;

        // Get the stack for the current thread
        CallStackType *pStack = stacks[event.pid];

        // If the stack does not exist, then allocate a new one.
        if (pStack == NULL) {
            pStack = new CallStackType(event.pid, kNumStackFrames, trace);
            stacks[event.pid] = pStack;
        }

        // Update the stack
        pStack->updateStack(&event, function);
    }

    for (int ii = 0; ii < kMaxThreads; ++ii) {
        if (stacks[ii])
            stacks[ii]->popAll(event.time);
    }

    int nsyms;
    symbol_type *syms = trace->GetSymbols(&nsyms);

    // Sort the symbols into decreasing number of calls
    qsort(syms, nsyms, sizeof(symbol_type), cmp_sym_names);

    symbol_type *psym = syms;
    for (int ii = 0; ii < nsyms; ++ii, ++psym) {
        // Ignore functions with non-zero calls
        if (psym->numCalls)
            continue;

        // Ignore some symbols
        if (strcmp(psym->name, "(end)") == 0)
            continue;
        if (strcmp(psym->name, "(unknown)") == 0)
            continue;
        if (strcmp(psym->name, ".plt") == 0)
            continue;
        const char *ksym = " ";
        if (psym->region->flags & region_type::kIsKernelRegion)
            ksym = "k";
        printf("%s %s %s\n", ksym, psym->name, psym->region->path);
#if 0
        printf("#%d %5d %s %s %s\n", ii + 1, psym->numCalls, ksym, psym->name,
               psym->region->path);
#endif
    }
    delete[] syms;
    delete trace;

    return 0;
}
