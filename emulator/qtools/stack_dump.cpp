// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "bitvector.h"
#include "parse_options.h"
#include "armdis.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"
#include "callstack.h"

static uint64_t debugTime;
static uint64_t dumpTime = 0;

class MyFrame : public StackFrame<symbol_type> {
  public:
    void    push(int stackLevel, uint64_t time, CallStackBase *base);
    void    pop(int stackLevel, uint64_t time, CallStackBase *base);
};

typedef CallStack<MyFrame> CallStackType;

void MyFrame::push(int stackLevel, uint64_t time, CallStackBase *base)
{
    if (dumpTime > 0)
        return;
    printf("%llu en thr %d %3d", time, base->getId(), stackLevel);
    for (int ii = 0; ii < stackLevel; ++ii)
        printf(".");
    printf(" 0x%08x %s\n", addr, function->name);
}

void MyFrame::pop(int stackLevel, uint64_t time, CallStackBase *base)
{
    if (dumpTime > 0)
        return;
    printf("%llu x  thr %d %3d", time, base->getId(), stackLevel);
    for (int ii = 0; ii < stackLevel; ++ii)
        printf(".");
    printf(" 0x%08x %s\n", addr, function->name);
}

static const int kNumStackFrames = 500;
static const int kMaxThreads = (32 * 1024);
CallStackType *stacks[kMaxThreads];

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_name elf_file\n",
            program);
    OptionsUsage();
}

bool localParseOptions(int argc, char **argv)
{
    bool err = false;
    while (!err) {
        int opt = getopt(argc, argv, "+d:");
        if (opt == -1)
            break;
        switch (opt) {
        case 'd':
            dumpTime = strtoull(optarg, NULL, 0);
            break;
        default:
            err = true;
            break;
        }
    }
    return err;
}

int main(int argc, char **argv)
{
    ParseOptions(argc, argv);
    localParseOptions(argc, argv);
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
        if (debugTime != 0 && event.time >= debugTime)
            printf("debug time: %lld\n", debugTime);

        // Update the stack
        pStack->updateStack(&event, function);

        // If the user requested a stack dump at a certain time,
        // and we are at that time, then dump the stack and exit.
        if (dumpTime > 0 && event.time >= dumpTime) {
            pStack->showStack(stdout);
            break;
        }
    }

    for (int ii = 0; ii < kMaxThreads; ++ii) {
        if (stacks[ii])
            stacks[ii]->popAll(event.time);
    }

    delete trace;
    return 0;
}
