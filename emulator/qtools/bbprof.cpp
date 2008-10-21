#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "trace_reader.h"
#include "armdis.h"

struct MyStaticRec {
    StaticRec   bb;
    uint32_t    *insns;
    uint32_t    *cycles;    // number of cycles for each insn
    uint32_t    elapsed;    // number of cycles for basic block
    int         freq;       // execution frequency
    MyStaticRec *inner;     // pointer to an inner basic block
    int         is_thumb;
};

MyStaticRec **assign_inner_blocks(int num_blocks, MyStaticRec *blocks);

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

// This function is called from quicksort to compare the elapsed time
// of basic blocks.
int cmp_dec_elapsed(const void *a, const void *b) {
    MyStaticRec *bb1, *bb2;

    bb1 = *(MyStaticRec**)a;
    bb2 = *(MyStaticRec**)b;
    if (bb1->elapsed < bb2->elapsed)
        return 1;
    if (bb1->elapsed > bb2->elapsed)
        return -1;
    return bb1->bb.bb_num - bb2->bb.bb_num;
}

// This function is called from quicksort to compare frequencies of
// basic blocks.
int cmp_dec_freq(const void *a, const void *b) {
    MyStaticRec *bb1, *bb2;

    bb1 = *(MyStaticRec**)a;
    bb2 = *(MyStaticRec**)b;
    if (bb1->freq < bb2->freq)
        return 1;
    if (bb1->freq > bb2->freq)
        return -1;
    return bb1->bb.bb_num - bb2->bb.bb_num;
}

int main(int argc, char **argv)
{
    if (argc != 2) {
        fprintf(stderr, "Usage: %s trace_file\n", argv[0]);
        exit(1);
    }

    char *trace_filename = argv[1];
    TraceReaderBase *trace = new TraceReaderBase;
    trace->Open(trace_filename);
    TraceHeader *header = trace->GetHeader();
    uint32_t num_static_bb = header->num_static_bb;

    // Allocate space for all of the static blocks
    MyStaticRec *blocks = new MyStaticRec[num_static_bb];

    // Read in all the static blocks
    for (uint32_t ii = 0; ii < num_static_bb; ++ii) {
        trace->ReadStatic(&blocks[ii].bb);
        blocks[ii].is_thumb = blocks[ii].bb.bb_addr & 1;
        blocks[ii].bb.bb_addr &= ~1;
        uint32_t num_insns = blocks[ii].bb.num_insns;
        blocks[ii].insns = new uint32_t[num_insns];
        blocks[ii].cycles = new uint32_t[num_insns];
        memset(blocks[ii].cycles, 0, num_insns * sizeof(uint32_t));
        trace->ReadStaticInsns(num_insns, blocks[ii].insns);
        blocks[ii].elapsed = 0;
        blocks[ii].freq = 0;
        blocks[ii].inner = NULL;
    }

    MyStaticRec **sorted = assign_inner_blocks(num_static_bb, blocks);

    uint32_t prev_time = 0;
    uint32_t elapsed = 0;
    uint32_t dummy;
    uint32_t *cycle_ptr = &dummy;
    uint32_t *bb_elapsed_ptr = &dummy;
    while (1) {
        BBEvent event;

        if (trace->ReadBB(&event))
            break;
        // Assign frequencies to each basic block
        uint64_t bb_num = event.bb_num;
        int num_insns = event.num_insns;
        blocks[bb_num].freq += 1;
        for (MyStaticRec *bptr = blocks[bb_num].inner; bptr; bptr = bptr->inner)
            bptr->freq += 1;

        // Assign simulation time to each instruction
        for (MyStaticRec *bptr = &blocks[bb_num]; bptr; bptr = bptr->inner) {
            uint32_t bb_num_insns = bptr->bb.num_insns;
            for (uint32_t ii = 0; num_insns && ii < bb_num_insns; ++ii, --num_insns) {
                uint32_t sim_time = trace->ReadInsnTime(event.time);
                elapsed = sim_time - prev_time;
                prev_time = sim_time;

                // Attribute the elapsed time to the previous instruction and
                // basic block.
                *cycle_ptr += elapsed;
                *bb_elapsed_ptr += elapsed;
                cycle_ptr = &bptr->cycles[ii];
                bb_elapsed_ptr = &bptr->elapsed;
            }
        }
    }
    *cycle_ptr += 1;
    *bb_elapsed_ptr += 1;

    // Sort the basic blocks into decreasing elapsed time
    qsort(sorted, num_static_bb, sizeof(MyStaticRec*), cmp_dec_elapsed);

    char spaces[80];
    memset(spaces, ' ', 79);
    spaces[79] = 0;
    for (uint32_t ii = 0; ii < num_static_bb; ++ii) {
        printf("bb %lld addr: 0x%x, insns: %d freq: %u elapsed: %u\n",
               sorted[ii]->bb.bb_num, sorted[ii]->bb.bb_addr,
               sorted[ii]->bb.num_insns, sorted[ii]->freq,
               sorted[ii]->elapsed);
        int num_insns = sorted[ii]->bb.num_insns;
        uint32_t addr = sorted[ii]->bb.bb_addr;
        for (int jj = 0; jj < num_insns; ++jj) {
            uint32_t elapsed = sorted[ii]->cycles[jj];
            uint32_t insn = sorted[ii]->insns[jj];
            if (insn_is_thumb(insn)) {
                insn = insn_unwrap_thumb(insn);

                // thumb_pair is true if this is the first of a pair of
                // thumb instructions (BL or BLX).
                bool thumb_pair = ((insn & 0xf800) == 0xf000);

                // Get the next thumb instruction (if any) because we may need
                // it for the case where insn is BL or BLX.
                uint32_t insn2 = 0;
                if (thumb_pair && (jj + 1 < num_insns)) {
                    insn2 = sorted[ii]->insns[jj + 1];
                    insn2 = insn_unwrap_thumb(insn2);
                    jj += 1;
                }
                char *disasm = disasm_insn_thumb(addr, insn, insn2, NULL);
                if (thumb_pair) {
                    printf("  %4u %08x %04x %04x %s\n", elapsed, addr, insn,
                           insn2, disasm);
                    addr += 2;
                } else {
                    printf("  %4u %08x     %04x %s\n", elapsed, addr, insn,
                           disasm);
                }
                addr += 2;
            } else {
                char *disasm = Arm::disasm(addr, insn, NULL);
                printf("  %4u %08x %08x %s\n", elapsed, addr, insn, disasm);
                addr += 4;
            }
        }
    }

    delete[] sorted;
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
