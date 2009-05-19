#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

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
    static const int kMaxFrames = 1000;
    int top;
    frame *frames[kMaxFrames];

public:
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

void Stack::dump() {
    frame *pframe;

    for (int ii = 0; ii < top; ii++) {
        pframe = frames[ii];
        const char *native = pframe->isNative ? "n" : " ";
        printf(" %s %d: %llu 0x%x %s\n",
               native, ii, pframe->time, pframe->addr,
               pframe->name == NULL ? "" : pframe->name);
    }
}

static const int kMaxThreads = (32 * 1024);
Stack *stacks[kMaxThreads];

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
        frame *pframe;

        if (trace->ReadMethodSymbol(&method_record, &sym, &proc))
            break;

        if (!IsValidPid(proc->pid))
            continue;

        if (sym != NULL) {
            printf("%lld p %d 0x%x %d %s\n",
                   method_record.time, proc->pid, method_record.addr,
                   method_record.flags, sym->name);
        } else {
            printf("%lld p %d 0x%x %d\n",
                   method_record.time, proc->pid, method_record.addr,
                   method_record.flags);
        }

        // Get the stack for the current thread
        Stack *pStack = stacks[proc->pid];

        // If the stack does not exist, then allocate a new one.
        if (pStack == NULL) {
            pStack = new Stack();
            stacks[proc->pid] = pStack;
        }

        int flags = method_record.flags;
        if (flags == kMethodEnter || flags == kNativeEnter) {
            pframe = new frame(method_record.time, method_record.addr,
                               sym == NULL ? NULL: sym->name,
                               method_record.flags == kNativeEnter);
            pStack->push(pframe);
        } else {
            pframe = pStack->pop();
            delete pframe;
        }
        pStack->dump();
    }
    return 0;
}
