// Copyright 2009 The Android Open Source Project

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

typedef CallStack<StackFrame<symbol_type> > CallStackType;

void compareStacks(uint64_t time, int pid);
void dumpStacks(int pid);

static uint64_t debugTime;
static const int kNumStackFrames = 500;
static const int kMaxThreads = (32 * 1024);
CallStackType *eStacks[kMaxThreads];

int numErrors;
static const int kMaxErrors = 3;

struct frame {
    uint64_t    time;
    uint32_t    addr;
    const char  *name;
    bool        isNative;

    frame(uint64_t time, uint32_t addr, const char *name, bool isNative) {
        this->time = time;
        this->addr = addr;
        this->name = name;
        this->isNative = isNative;
    }
};

class Stack {
public:
    static const int kMaxFrames = 1000;
    int top;
    frame *frames[kMaxFrames];

    Stack() {
        top = 0;
    }

    void        push(frame *pframe);
    frame*      pop();
    void        dump();
};

void Stack::push(frame *pframe) {
    if (top == kMaxFrames) {
        fprintf(stderr, "Error: stack overflow\n");
        exit(1);
    }
    frames[top] = pframe;
    top += 1;
}

frame *Stack::pop() {
    if (top <= 0)
        return NULL;
    top -= 1;
    return frames[top];
}

Stack *mStacks[kMaxThreads];

void Usage(const char *program)
{
    fprintf(stderr, "Usage: %s [options] trace_name elf_file\n",
            program);
    OptionsUsage();
}

int main(int argc, char **argv)
{
    ParseOptions(argc, argv);
    if (argc - optind != 2) {
        Usage(argv[0]);
        exit(1);
    }

    char *qemu_trace_file = argv[optind++];
    char *elf_file = argv[optind++];

    TraceReaderType *etrace = new TraceReaderType;
    etrace->Open(qemu_trace_file);
    etrace->ReadKernelSymbols(elf_file);
    etrace->SetRoot(root);

    TraceReaderType *mtrace = new TraceReaderType;
    mtrace->Open(qemu_trace_file);
    mtrace->ReadKernelSymbols(elf_file);
    mtrace->SetRoot(root);

    BBEvent event;
    while (1) {
        BBEvent ignored;
        symbol_type *function;
        MethodRec method_record;
        symbol_type *sym;
        TraceReaderType::ProcessState *proc;
        frame *pframe;

        if (mtrace->ReadMethodSymbol(&method_record, &sym, &proc))
            break;

        if (!IsValidPid(proc->pid))
            continue;

        // Get the stack for the current thread
        Stack *mStack = mStacks[proc->pid];

        // If the stack does not exist, then allocate a new one.
        if (mStack == NULL) {
            mStack = new Stack();
            mStacks[proc->pid] = mStack;
        }

        int flags = method_record.flags;
        if (flags == kMethodEnter || flags == kNativeEnter) {
            pframe = new frame(method_record.time, method_record.addr,
                               sym == NULL ? NULL: sym->name,
                               method_record.flags == kNativeEnter);
            mStack->push(pframe);
        } else {
            pframe = mStack->pop();
            delete pframe;
        }

        do {
            if (GetNextValidEvent(etrace, &event, &ignored, &function))
                break;
            if (event.bb_num == 0)
                break;

            // Get the stack for the current thread
            CallStackType *eStack = eStacks[event.pid];

            // If the stack does not exist, then allocate a new one.
            if (eStack == NULL) {
                eStack = new CallStackType(event.pid, kNumStackFrames, etrace);
                eStacks[event.pid] = eStack;
            }
            if (debugTime != 0 && event.time >= debugTime)
                printf("time: %llu debug time: %lld\n", event.time, debugTime);

            // Update the stack
            eStack->updateStack(&event, function);
        } while (event.time < method_record.time);

        compareStacks(event.time, event.pid);
    }

    for (int ii = 0; ii < kMaxThreads; ++ii) {
        if (eStacks[ii])
            eStacks[ii]->popAll(event.time);
    }

    delete etrace;
    delete mtrace;
    return 0;
}

void compareStacks(uint64_t time, int pid) {
    CallStackType *eStack = eStacks[pid];
    Stack *mStack = mStacks[pid];
    frame **mFrames = mStack->frames;
    frame *mframe;

    int mTop = mStack->top;
    int eTop = eStack->mTop;
    CallStackType::frame_type *eFrames = eStack->mFrames;

    // Count the number of non-native methods (ie, Java methods) on the
    // Java method stack
    int numNonNativeMethods = 0;
    for (int ii = 0; ii < mTop; ++ii) {
        if (!mFrames[ii]->isNative) {
            numNonNativeMethods += 1;
        }
    }

    // Count the number of Java methods on the native stack
    int numMethods = 0;
    for (int ii = 0; ii < eTop; ++ii) {
        if (eFrames[ii].flags & CallStackType::frame_type::kInterpreted) {
            numMethods += 1;
        }
    }

    // Verify that the number of Java methods on both stacks are the same.
    // Allow the native stack to have one less Java method because the
    // native stack might be pushing a native function first.
    if (numNonNativeMethods != numMethods && numNonNativeMethods != numMethods + 1) {
        printf("\nDiff at time %llu pid %d: non-native %d numMethods %d\n",
               time, pid, numNonNativeMethods, numMethods);
        dumpStacks(pid);
        numErrors += 1;
        if (numErrors >= kMaxErrors)
            exit(1);
    }

    // Verify that the Java methods on the method stack are the same
    // as the Java methods on the native stack.
    int mIndex = 0;
    for (int ii = 0; ii < eTop; ++ii) {
        // Ignore native functions on the native stack.
        if ((eFrames[ii].flags & CallStackType::frame_type::kInterpreted) == 0)
            continue;
        uint32_t addr = eFrames[ii].function->addr;
        addr += eFrames[ii].function->region->vstart;
        while (mIndex < mTop && mFrames[mIndex]->isNative) {
            mIndex += 1;
        }
        if (mIndex >= mTop)
            break;
        if (addr != mFrames[mIndex]->addr) {
            printf("\nDiff at time %llu pid %d: frame %d\n", time, pid, ii);
            dumpStacks(pid);
            exit(1);
        }
        mIndex += 1;
    }
}

void dumpStacks(int pid) {
    CallStackType *eStack = eStacks[pid];
    Stack *mStack = mStacks[pid];
    frame *mframe;

    int mTop = mStack->top;
    printf("\nJava method stack\n");
    for (int ii = 0; ii < mTop; ii++) {
        mframe = mStack->frames[ii];
        const char *native = mframe->isNative ? "n" : " ";
        printf("  %s %d: %llu 0x%x %s\n",
               native, ii, mframe->time, mframe->addr,
               mframe->name == NULL ? "" : mframe->name);
    }

    int eTop = eStack->mTop;
    CallStackType::frame_type *eFrames = eStack->mFrames;
    int mIndex = 0;
    printf("\nNative stack\n");
    for (int ii = 0; ii < eTop; ++ii) {
        uint32_t addr = eFrames[ii].function->addr;
        addr += eFrames[ii].function->region->vstart;
        const char *marker = " ";
        if (eFrames[ii].flags & CallStackType::frame_type::kInterpreted) {
            if (mIndex >= mTop || addr != mStack->frames[mIndex]->addr) {
                marker = "*";
            }
            mIndex += 1;
        }
        printf(" %s %d: %d f %d 0x%08x %s\n",
               marker, ii, eFrames[ii].time, eFrames[ii].flags, addr,
               eFrames[ii].function->name);
    }
}
