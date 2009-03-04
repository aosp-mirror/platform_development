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
static uint64_t startTime = 0;

void Usage(const char *program)
{
    fprintf(stderr,
            "Usage: %s [options] [-- -s start_time] trace_file elf_file\n",
            program);
    OptionsUsage();
}


bool localParseOptions(int argc, char **argv)
{
    bool err = false;
    while (!err) {
        int opt = getopt(argc, argv, "+s:");
        if (opt == -1)
            break;
        switch (opt) {
        case 's':
            startTime = strtoull(optarg, NULL, 0);
            break;
        default:
            err = true;
            break;
        }
    }
    return err;
}

int main(int argc, char **argv) {
    // Parse the options
    ParseOptions(argc, argv);
    localParseOptions(argc, argv);
    if (argc - optind != 2) {
        Usage(argv[0]);
        exit(1);
    }

    char *trace_filename = argv[optind++];
    char *elf_file = argv[optind++];
    TraceReader<> *trace = new TraceReader<>;
    trace->Open(trace_filename);
    trace->SetDemangle(demangle);
    trace->ReadKernelSymbols(elf_file);
    trace->SetRoot(root);

    while (1) {
        symbol_type *sym;
        char buf[1024];
        BBEvent event;
        BBEvent ignored;

        if (GetNextValidEvent(trace, &event, &ignored, &sym))
            break;
#if 0
        fprintf(stderr, "t%llu bb %lld %d\n",
                event.time, event.bb_num, event.num_insns);
#endif

        uint32_t *insns = event.insns;
        uint32_t addr = event.bb_addr;
        uint32_t offset = addr - sym->addr - sym->region->base_addr;
        symbol_type *vm_sym = sym->vm_sym;
        const char *vm_name = NULL;
        if (vm_sym != NULL) {
            vm_name = vm_sym->name;
            offset = addr - vm_sym->addr - vm_sym->region->base_addr;
        }
#if 0
        if (strcmp(sym->name, "(unknown)") == 0 || offset > kOffsetThreshold) {
            ProcessState *process = trace->GetCurrentProcess();
            ProcessState *manager = process->addr_manager;
            for (int ii = 0; ii < manager->nregions; ++ii) {
                printf("  %2d: %08x - %08x base: %08x offset: %u nsyms: %4d flags: 0x%x %s\n",
                       ii,
                       manager->regions[ii]->vstart,
                       manager->regions[ii]->vend,
                       manager->regions[ii]->base_addr,
                       manager->regions[ii]->file_offset,
                       manager->regions[ii]->nsymbols,
                       manager->regions[ii]->flags,
                       manager->regions[ii]->path);
                int nsymbols = manager->regions[ii]->nsymbols;
                for (int jj = 0; jj < 10 && jj < nsymbols; ++jj) {
                    printf("    %08x %s\n",
                           manager->regions[ii]->symbols[jj].addr,
                           manager->regions[ii]->symbols[jj].name);
                }
            }
        }
#endif
#if 1
        for (int ii = 0; ii < event.num_insns; ++ii) {
            uint64_t sim_time = trace->ReadInsnTime(event.time);
            if (sim_time < startTime)
                continue;

            uint32_t insn = insns[ii];
            char *disasm;
            int bytes;
            if (vm_name != NULL) {
                sprintf(buf, "%s+%02x: %s", vm_name, offset, sym->name);
            } else {
                sprintf(buf, "%s+%02x", sym->name, offset);
            }

            if (insn_is_thumb(insn)) {
                bytes = 2;
                insn = insn_unwrap_thumb(insn);

                // thumb_pair is true if this is the first of a pair of
                // thumb instructions (BL or BLX).
                bool thumb_pair = ((insn & 0xf800) == 0xf000);

                // Get the next thumb instruction (if any) because we may need
                // it for the case where insn is BL or BLX.
                uint32_t insn2 = 0;
                if (thumb_pair && (ii + 1 < event.num_insns)) {
                    insn2 = insns[ii + 1];
                    insn2 = insn_unwrap_thumb(insn2);
                    bytes = 4;
                    ii += 1;
                }
                disasm = disasm_insn_thumb(addr, insn, insn2, NULL);
                if (thumb_pair) {
                    printf("%llu p%-4d %08x %04x %04x %-30s %s\n",
                           sim_time, event.pid, addr, insn, insn2, buf, disasm);
                } else {
                    printf("%llu p%-4d %08x     %04x %-30s %s\n",
                           sim_time, event.pid, addr, insn, buf, disasm);
                }
            } else {
                bytes = 4;
                disasm = Arm::disasm(addr, insn, NULL);
                printf("%llu p%-4d %08x %08x %-30s %s\n",
                       sim_time, event.pid, addr, insn, buf, disasm);
            }
            //printf("t%llu \t%08x\n", sim_time, addr);
            addr += bytes;
            offset += bytes;
        }
#endif
#if 0
        assert(offset < kOffsetThreshold);
#endif
    }

    delete trace;
    return 0;
}
