// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>
#include "trace_reader.h"
#include "bitvector.h"
#include "parse_options.h"
#include "dmtrace.h"
#include "armdis.h"

struct symbol {
    uint32_t id;
};

typedef TraceReader<symbol> TraceReaderType;

#include "parse_options-inl.h"
#include "callstack.h"

DmTrace *dmtrace;

class MyFrame : public StackFrame<symbol_type> {
  public:
    void push(int stackLevel, uint64_t time, CallStackBase *base);
    void pop(int stackLevel, uint64_t time, CallStackBase *base);
};

typedef CallStack<MyFrame> CallStackType;

static const int kNumStackFrames = 500;
static const int kMaxThreads = (32 * 1024);
uint64_t thread_time[kMaxThreads];

class FunctionStack {
  public:
    FunctionStack() {
        top = 0;
    }
    void push(symbol_type *sym) {
        if (top >= kNumStackFrames)
            return;
        frames[top] = sym;
        top += 1;
    }

    symbol_type* pop() {
        if (top <= 0) {
            return NULL;
        }
        top -= 1;
        return frames[top];
    }

    void showStack() {
        fprintf(stderr, "top %d\n", top);
        for (int ii = 0; ii < top; ii++) {
            fprintf(stderr, "  %d: %s\n", ii, frames[ii]->name);
        }
    }

  private:
    int top;
    symbol_type *frames[kNumStackFrames];
};

FunctionStack *dmtrace_stack[kMaxThreads];

void MyFrame::push(int stackLevel, uint64_t time, CallStackBase *base)
{
    int pid = base->getId();
    CallStackType *stack = (CallStackType *) base;

#if 0
    fprintf(stderr, "native push t %llu p %d s %d fid %d 0x%x %s\n",
            stack->getGlobalTime(time), pid, stackLevel,
            function->id, function->addr, function->name);
#endif

    FunctionStack *fstack = dmtrace_stack[pid];
    if (fstack == NULL) {
        fstack = new FunctionStack();
        dmtrace_stack[pid] = fstack;
    }

    fstack->push(function);
    thread_time[pid] = time;
    dmtrace->addFunctionEntry(function->id, time, pid);
}

void MyFrame::pop(int stackLevel, uint64_t time, CallStackBase *base)
{
    int pid = base->getId();
    CallStackType *stack = (CallStackType *) base;

#if 0
    fprintf(stderr, "native pop  t %llu p %d s %d fid %d 0x%x %s\n",
            stack->getGlobalTime(time), pid, stackLevel,
            function->id, function->addr, function->name);
#endif

    FunctionStack *fstack = dmtrace_stack[pid];
    if (fstack == NULL) {
        fstack = new FunctionStack();
        dmtrace_stack[pid] = fstack;
    }

    symbol_type *sym = fstack->pop();
    if (sym != NULL && sym != function) {
        fprintf(stderr, "Error: q2dm function mismatch at time %llu pid %d sym %s\n",
                stack->getGlobalTime(time), pid, sym->name);
        fstack->showStack();
        exit(1);
    }

    thread_time[pid] = time;
    dmtrace->addFunctionExit(function->id, time, pid);
}

uint32_t nextFunctionId = 4;
CallStackType *stacks[kMaxThreads];

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_name elf_file dmtrace_name\n",
            program);
    OptionsUsage();
}

int main(int argc, char **argv)
{
    bool useKernelStack = true;

    ParseOptions(argc, argv);
    if (argc - optind != 3) {
        Usage(argv[0]);
        exit(1);
    }

    char *qemu_trace_file = argv[optind++];
    char *elf_file = argv[optind++];
    char *dmtrace_file = argv[optind++];
    TraceReaderType *trace = new TraceReaderType;
    trace->Open(qemu_trace_file);
    trace->SetDemangle(demangle);
    trace->ReadKernelSymbols(elf_file);
    trace->SetRoot(root);
    TraceHeader *qheader = trace->GetHeader();
    uint64_t startTime = qheader->start_sec;
    startTime = (startTime << 32) | qheader->start_usec;
    int kernelPid = qheader->first_unused_pid;

    dmtrace = new DmTrace;
    dmtrace->open(dmtrace_file, startTime);

    bool inKernel = false;
    CallStackType *kernelStack = NULL;
    if (useKernelStack) {
        // Create a fake kernel thread stack where we will put all the kernel
        // code.
        kernelStack = new CallStackType(kernelPid, kNumStackFrames, trace);
        dmtrace->addThread(kernelPid, "(kernel)");
    }

    CallStackType *prevStack = NULL;
    BBEvent event;
    while (1) {
        BBEvent ignored;
        symbol_type *function;

        if (GetNextValidEvent(trace, &event, &ignored, &function))
            break;
        if (event.bb_num == 0)
            break;
#if 0
        fprintf(stderr, "event t %llu p %d %s\n",
                event.time, event.pid, function->name);
#endif

        CallStackType *pStack;
        if (useKernelStack) {
            uint32_t flags = function->region->flags;
            uint32_t region_mask = region_type::kIsKernelRegion
                    | region_type::kIsUserMappedRegion;
            if ((flags & region_mask) == region_type::kIsKernelRegion) {
                // Use the kernel stack
                pStack = kernelStack;
                inKernel = true;
            } else {
                // If we were just in the kernel then pop off all of the
                // stack frames for the kernel thread.
                if (inKernel == true) {
                    inKernel = false;
                    kernelStack->popAll(event.time);
                }
                
                // Get the stack for the current thread
                pStack = stacks[event.pid];
            }
        } else {
            // Get the stack for the current thread
            pStack = stacks[event.pid];
        }

        // If the stack does not exist, then allocate a new one.
        if (pStack == NULL) {
            pStack = new CallStackType(event.pid, kNumStackFrames, trace);
            stacks[event.pid] = pStack;
            const char *name = trace->GetProcessName(event.pid);
            dmtrace->addThread(event.pid, name);
        }

        if (prevStack != pStack) {
            pStack->threadStart(event.time);
            if (prevStack)
                prevStack->threadStop(event.time);
        }
        prevStack = pStack;

        // If we have never seen this function before, then add it to the
        // list of known functions.
        if (function->id == 0) {
            function->id = nextFunctionId;
            nextFunctionId += 4;
            uint32_t flags = function->region->flags;
            const char *name = function->name;
            if (flags & region_type::kIsKernelRegion) {
                // To distinguish kernel function names from user library
                // names, add a marker to the name.
                int len = strlen(name) + strlen(" [kernel]") + 1;
                char *kernelName = new char[len];
                strcpy(kernelName, name);
                strcat(kernelName, " [kernel]");
                name = kernelName;
            }
            dmtrace->parseAndAddFunction(function->id, name);
        }

        // Update the stack
        pStack->updateStack(&event, function);
    }

    if (prevStack == NULL) {
        fprintf(stderr, "Error: no events in trace.\n");
        exit(1);
    }
    prevStack->threadStop(event.time);
    for (int ii = 0; ii < kMaxThreads; ++ii) {
        if (stacks[ii]) {
            stacks[ii]->threadStart(event.time);
            stacks[ii]->popAll(event.time);
        }
    }
    if (useKernelStack) {
        kernelStack->popAll(event.time);
    }

    // Read the pid events to find the names of the processes
    while (1) {
        PidEvent pid_event;
        if (trace->ReadPidEvent(&pid_event))
            break;
        if (pid_event.rec_type == kPidName) {
            dmtrace->updateName(pid_event.pid, pid_event.path);
        }
    }

    dmtrace->close();
    delete dmtrace;
    delete trace;
    return 0;
}
