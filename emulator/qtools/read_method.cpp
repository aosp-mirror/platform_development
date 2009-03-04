#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_name elf_file\n",
            program);
    OptionsUsage();
}

int main(int argc, char **argv) {
    ParseOptions(argc, argv);
    if (argc - optind != 2) {
        Usage(argv[0]);
        exit(1);
    }

    char *qemu_trace_file = argv[optind++];
    char *elf_file = argv[optind++];
    TraceReaderType *trace = new TraceReaderType;
    trace->Open(qemu_trace_file);
    trace->ReadKernelSymbols(elf_file);
    trace->SetRoot(root);

    while (1) {
        MethodRec method_record;
        symbol_type *sym;
        TraceReaderType::ProcessState *proc;

        if (trace->ReadMethodSymbol(&method_record, &sym, &proc))
            break;
        if (sym != NULL) {
            printf("%lld p %d 0x%x %d %s\n",
                   method_record.time, proc->pid, method_record.addr,
                   method_record.flags, sym->name);
        } else {
            printf("%lld p %d 0x%x %d\n",
                   method_record.time, proc->pid, method_record.addr,
                   method_record.flags);
        }
        proc->DumpStack();
    }
    return 0;
}
