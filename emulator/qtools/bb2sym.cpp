#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

struct MyStaticRec {
    StaticRec   bb;
    symbol_type *sym;
    MyStaticRec *inner;    // pointer to an inner basic block
    int         is_thumb;
};

MyStaticRec **assign_inner_blocks(int num_blocks, MyStaticRec *blocks);

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_file elf_file\n", program);
    OptionsUsage();
}

// This function is called from quicksort to compare addresses of basic
// blocks.
int cmp_inc_addr(const void *a, const void *b) {
    MyStaticRec *bb1, *bb2;

    bb1 = *(MyStaticRec**)a;
    bb2 = *(MyStaticRec**)b;
    if (bb1->bb.bb_addr < bb2->bb.bb_addr)
        return -1;
    if (bb1->bb.bb_addr > bb2->bb.bb_addr)
        return 1;
    return bb1->bb.bb_num - bb2->bb.bb_num;
}

int main(int argc, char **argv) {
    uint32_t insns[kMaxInsnPerBB];

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

    TraceHeader *header = trace->GetHeader();
    uint32_t num_static_bb = header->num_static_bb;

    // Allocate space for all of the static blocks
    MyStaticRec *blocks = new MyStaticRec[num_static_bb];

    // Read in all the static blocks
    for (uint32_t ii = 0; ii < num_static_bb; ++ii) {
        trace->ReadStatic(&blocks[ii].bb);
        blocks[ii].is_thumb = blocks[ii].bb.bb_addr & 1;
        blocks[ii].bb.bb_addr &= ~1;
        blocks[ii].sym = NULL;
        blocks[ii].inner = NULL;
        trace->ReadStaticInsns(blocks[ii].bb.num_insns, insns);
    }

    MyStaticRec **sorted = assign_inner_blocks(num_static_bb, blocks);

    while (1) {
        symbol_type *sym;
        BBEvent event;
        BBEvent ignored;

        if (GetNextValidEvent(trace, &event, &ignored, &sym))
            break;

        uint64_t bb_num = event.bb_num;
        blocks[bb_num].sym = sym;
    }
        
    printf("#     bb num_insns     bb_addr file  symbol\n");
    for (uint32_t ii = 0; ii < num_static_bb; ++ii) {
        if (sorted[ii]->bb.bb_addr == 0 || sorted[ii]->bb.num_insns == 0
            || sorted[ii]->sym == NULL)
            continue;

        printf("%8lld       %3d  0x%08x %s %s\n",
               sorted[ii]->bb.bb_num, sorted[ii]->bb.num_insns,
               sorted[ii]->bb.bb_addr, sorted[ii]->sym->region->path,
               sorted[ii]->sym->name);
    }
    return 0;
}

// Find the basic blocks that are subsets of other basic blocks.
MyStaticRec **assign_inner_blocks(int num_blocks, MyStaticRec *blocks)
{
    int ii;
    uint32_t addr_end, addr_diff;

    // Create a list of pointers to the basic blocks that we can sort.
    MyStaticRec **sorted = new MyStaticRec*[num_blocks];
    for (ii = 0; ii < num_blocks; ++ii) {
        sorted[ii] = &blocks[ii];
    }

    // Sort the basic blocks into increasing address order
    qsort(sorted, num_blocks, sizeof(MyStaticRec*), cmp_inc_addr);

    // Create pointers to inner blocks and break up the enclosing block
    // so that there is no overlap.
    for (ii = 0; ii < num_blocks - 1; ++ii) {
        int num_bytes;
        if (sorted[ii]->is_thumb)
            num_bytes = sorted[ii]->bb.num_insns << 1;
        else
            num_bytes = sorted[ii]->bb.num_insns << 2;
        addr_end = sorted[ii]->bb.bb_addr + num_bytes;
        if (addr_end > sorted[ii + 1]->bb.bb_addr) {
            sorted[ii]->inner = sorted[ii + 1];
            addr_diff = sorted[ii + 1]->bb.bb_addr - sorted[ii]->bb.bb_addr;
            uint32_t num_insns;
            if (sorted[ii]->is_thumb)
                num_insns = addr_diff >> 1;
            else
                num_insns = addr_diff >> 2;
            sorted[ii]->bb.num_insns = num_insns;
        }
    }

    return sorted;
}
