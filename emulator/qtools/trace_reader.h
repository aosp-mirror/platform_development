// Copyright 2006 The Android Open Source Project

#ifndef TRACE_READER_H
#define TRACE_READER_H

#include <string.h>
#include <inttypes.h>
#include <elf.h>
#include <assert.h>
#include <cxxabi.h>
#include "read_elf.h"
#include "trace_reader_base.h"
#include "hash_table.h"

struct TraceReaderEmptyStruct {
};

template <class T = TraceReaderEmptyStruct>
class TraceReader : public TraceReaderBase {
  public:

    struct region_entry;
    typedef struct symbol_entry : public T {
        typedef region_entry region_type;

        // Define flag values
        static const uint32_t kIsPlt = 0x01;
        static const uint32_t kIsVectorStart = 0x02;
        static const uint32_t kIsVectorTable = (kIsPlt | kIsVectorStart);
        static const uint32_t kIsInterpreter = 0x04;
        static const uint32_t kIsMethod = 0x08;

        uint32_t        addr;

        // This may hold the name of the interpreted method instead of
        // the name of the native function if the native function is a
        // virtual machine interpreter.
        const char      *name;

        // The symbol for the virtual machine interpreter, or NULL
        symbol_entry    *vm_sym;
        region_type     *region;
        uint32_t        flags;
    } symbol_type;

    typedef struct region_entry {
        // Define flag values
        static const uint32_t kIsKernelRegion           = 0x01;
        static const uint32_t kSharedSymbols            = 0x02;
        static const uint32_t kIsLibraryRegion          = 0x04;
        static const uint32_t kIsUserMappedRegion       = 0x08;

        region_entry() : refs(0), path(NULL), vstart(0), vend(0), base_addr(0),
                         file_offset(0), flags(0), nsymbols(0), symbols(NULL) {}

        symbol_type    *LookupFunctionByName(char *name) {
            // Just do a linear search
            for (int ii = 0; ii < nsymbols; ++ii) {
                if (strcmp(symbols[ii].name, name) == 0)
                    return &symbols[ii];
            }
            return NULL;
        }

        region_entry   *MakePrivateCopy(region_entry *dest) {
            dest->refs = 0;
            dest->path = Strdup(path);
            dest->vstart = vstart;
            dest->vend = vend;
            dest->base_addr = base_addr;
            dest->file_offset = file_offset;
            dest->flags = flags;
            dest->nsymbols = nsymbols;
            dest->symbols = symbols;
            return dest;
        }

        int             refs;        // reference count
        char            *path;
        uint32_t        vstart;
        uint32_t        vend;
        uint32_t        base_addr;
        uint32_t        file_offset;
        uint32_t        flags;
        int             nsymbols;
        symbol_type     *symbols;
    } region_type;

    typedef typename HashTable<region_type*>::entry_type hash_entry_type;

    class ProcessState {
      public:

        // The "regions" array below is a pointer to array of pointers to
        // regions.  The size of the pointer array is kInitialNumRegions,
        // but grows if needed.  There is a separate region for each mmap
        // call which includes shared libraries as well as .dex and .jar
        // files.  In addition, there is a region for the main executable
        // for this process, as well as a few regions for the kernel.
        //
        // If a child process is a clone of a parent process, the
        // regions array is unused.  Instead, the "addr_manager" pointer is
        // used to find the process that is the address space manager for
        // both the parent and child processes.
        static const int kInitialNumRegions = 10;

        static const int kMaxMethodStackSize = 1000;

        // Define values for the ProcessState flag bits
        static const int kCalledExec            = 0x01;
        static const int kCalledExit            = 0x02;
        static const int kIsClone               = 0x04;
        static const int kHasKernelRegion       = 0x08;
        static const int kHasFirstMmap          = 0x10;

        struct methodFrame {
            uint32_t    addr;
            bool        isNative;
        };

        ProcessState() {
            cpu_time = 0;
            tgid = 0;
            pid = 0;
            parent_pid = 0;
            exit_val = 0;
            flags = 0;
            argc = 0;
            argv = NULL;
            name = NULL;
            nregions = 0;
            max_regions = 0;
            // Don't allocate space yet until we know if we are a clone.
            regions = NULL;
            parent = NULL;
            addr_manager = this;
            next = NULL;
            current_method_sym = NULL;
            method_stack_top = 0;
        }

        ~ProcessState() {
            delete[] name;
            if ((flags & kIsClone) != 0) {
                return;
            }

            // Free the regions.  We must be careful not to free the symbols
            // within each region because the symbols are sometimes shared
            // between multiple regions.  The TraceReader class has a hash
            // table containing all the unique regions and it will free the
            // region symbols in its destructor.  We need to free only the
            // regions and the array of region pointers.
            //
            // Each region is also reference-counted.  The count is zero
            // if no other processes are sharing this region.
            for (int ii = 0; ii < nregions; ii++) {
                if (regions[ii]->refs > 0) {
                    regions[ii]->refs -= 1;
                    continue;
                }

                delete regions[ii];
            }

            delete[] regions;

            for (int ii = 0; ii < argc; ++ii)
                delete[] argv[ii];
            delete[] argv;
        }

        // Dumps the stack contents to standard output.  For debugging.
        void            DumpStack(FILE *stream);

        uint64_t        cpu_time;
        uint64_t        start_time;
        uint64_t        end_time;
        int             tgid;
        int             pid;
        int             parent_pid;
        int             exit_val;
        uint32_t        flags;
        int             argc;
        char            **argv;
        const char      *name;
        int             nregions;        // num regions in use
        int             max_regions;     // max regions allocated
        region_type     **regions;
        ProcessState    *parent;
        ProcessState    *addr_manager;   // the address space manager process
        ProcessState    *next;
        int             method_stack_top;
        methodFrame     method_stack[kMaxMethodStackSize];
        symbol_type     *current_method_sym;
    };

    TraceReader();
    ~TraceReader();

    void                ReadKernelSymbols(const char *kernel_file);
    void                CopyKernelRegion(ProcessState *pstate);
    void                ClearRegions(ProcessState *pstate);
    void                CopyRegions(ProcessState *parent, ProcessState *child);
    void                DumpRegions(FILE *stream, ProcessState *pstate);
    symbol_type         *LookupFunction(int pid, uint32_t addr, uint64_t time);
    symbol_type         *GetSymbols(int *num_syms);
    ProcessState        *GetCurrentProcess()            { return current_; }
    ProcessState        *GetProcesses(int *num_procs);
    ProcessState        *GetNextProcess();
    const char          *GetProcessName(int pid);
    void                SetRoot(const char *root)       { root_ = root; }
    void                SetDemangle(bool demangle)      { demangle_ = demangle; }
    bool                ReadMethodSymbol(MethodRec *method_record,
                                         symbol_type **psym,
                                         ProcessState **pproc);

  protected:
    virtual int FindCurrentPid(uint64_t time);

  private:

    static const int kNumPids = 32768;
    static const uint32_t kIncludeLocalSymbols = 0x1;

    void                AddPredefinedRegion(region_type *region, const char *path,
                                            uint32_t vstart, uint32_t vend,
                                            uint32_t base);
    void                InitRegionSymbols(region_type *region, int nsymbols);
    void                AddRegionSymbol(region_type *region, int idx,
                                        uint32_t addr, const char *name,
                                        uint32_t flags);
    void                AddPredefinedRegions(ProcessState *pstate);
    void                demangle_names(int nfuncs, symbol_type *functions);
    bool                ReadElfSymbols(region_type *region, uint32_t flags);
    void                AddRegion(ProcessState *pstate, region_type *region);
    region_type         *FindRegion(uint32_t addr, int nregions,
                                    region_type **regions);
    int                 FindRegionIndex(uint32_t addr, int nregions,
                                         region_type **regions);
    void                FindAndRemoveRegion(ProcessState *pstate,
                                            uint32_t vstart, uint32_t vend);
    symbol_type         *FindFunction(uint32_t addr, int nsyms,
                                      symbol_type *symbols, bool exact_match);
    symbol_type         *FindCurrentMethod(int pid, uint64_t time);
    void                PopulateSymbolsFromDexFile(const DexFileList *dexfile,
                                                   region_type *region);
    void                HandlePidEvent(PidEvent *event);
    void                HandleMethodRecord(ProcessState *pstate,
                                           MethodRec *method_rec);

    int                 cached_pid_;
    symbol_type         *cached_func_;
    symbol_type         unknown_;
    int                 next_pid_;

    PidEvent            next_pid_event_;
    ProcessState        *processes_[kNumPids];
    ProcessState        *current_;
    MethodRec           next_method_;
    uint64_t            function_start_time_;
    const char          *root_;
    HashTable<region_type*> *hash_;
    bool                demangle_;
};

template<class T>
TraceReader<T>::TraceReader()
{
    static PidEvent event_no_action;

    cached_pid_ = -1;
    cached_func_ = NULL;

    memset(&unknown_, 0, sizeof(symbol_type));
    unknown_.name = "(unknown)";
    next_pid_ = 0;

    memset(&event_no_action, 0, sizeof(PidEvent));
    event_no_action.rec_type = kPidNoAction;
    next_pid_event_ = event_no_action;
    for (int ii = 1; ii < kNumPids; ++ii)
        processes_[ii] = NULL;
    current_ = new ProcessState;
    processes_[0] = current_;
    next_method_.time = 0;
    next_method_.addr = 0;
    next_method_.flags = 0;
    function_start_time_ = 0;
    root_ = "";
    hash_ = new HashTable<region_type*>(512);
    AddPredefinedRegions(current_);
    demangle_ = true;
}

template<class T>
TraceReader<T>::~TraceReader()
{
    hash_entry_type *ptr;
    for (ptr = hash_->GetFirst(); ptr; ptr = hash_->GetNext()) {
        region_type *region = ptr->value;
        // If the symbols are not shared with another region, then delete them.
        if ((region->flags & region_type::kSharedSymbols) == 0) {
            int nsymbols = region->nsymbols;
            for (int ii = 0; ii < nsymbols; ii++) {
                delete[] region->symbols[ii].name;
            }
            delete[] region->symbols;
        }
        delete[] region->path;

        // Do not delete the region itself here.  Each region
        // is reference-counted and deleted by the ProcessState
        // object that owns it.
    }
    delete hash_;

    // Delete the ProcessState objects after the region symbols in
    // the hash table above so that we still have valid region pointers
    // when deleting the region symbols.
    for (int ii = 0; ii < kNumPids; ++ii) {
        delete processes_[ii];
    }
}

// This function is used by the qsort() routine to sort symbols
// into increasing address order.
template<class T>
int cmp_symbol_addr(const void *a, const void *b) {
    typedef typename TraceReader<T>::symbol_type stype;

    const stype *syma = static_cast<stype const *>(a);
    const stype *symb = static_cast<stype const *>(b);
    uint32_t addr1 = syma->addr;
    uint32_t addr2 = symb->addr;
    if (addr1 < addr2)
        return -1;
    if (addr1 > addr2)
        return 1;

    // The addresses are the same, sort the symbols into
    // increasing alphabetical order.  But put symbols that
    // that start with "_" last.
    if (syma->name[0] == '_' || symb->name[0] == '_') {
        // Count the number of leading underscores and sort the
        // symbol with the most underscores last.
        int aCount = 0;
        while (syma->name[aCount] == '_')
            aCount += 1;
        int bCount = 0;
        while (symb->name[bCount] == '_')
            bCount += 1;
        if (aCount < bCount) {
            return -1;
        }
        if (aCount > bCount) {
            return 1;
        }
        // If the symbols have the same number of underscores, then
        // fall through and sort by the whole name.
    }
    return strcmp(syma->name, symb->name);
}

// This function is used by the qsort() routine to sort region entries
// into increasing address order.
template<class T>
int cmp_region_addr(const void *a, const void *b) {
    typedef typename TraceReader<T>::region_type rtype;

    const rtype *ma = *static_cast<rtype* const *>(a);
    const rtype *mb = *static_cast<rtype* const *>(b);
    uint32_t addr1 = ma->vstart;
    uint32_t addr2 = mb->vstart;
    if (addr1 < addr2)
        return -1;
    if (addr1 == addr2)
        return 0;
    return 1;
}

// This routine returns a new array containing all the symbols.
template<class T>
typename TraceReader<T>::symbol_type*
TraceReader<T>::GetSymbols(int *num_syms)
{
    // Count the symbols
    int nsyms = 0;
    for (hash_entry_type *ptr = hash_->GetFirst(); ptr; ptr = hash_->GetNext()) {
        region_type *region = ptr->value;
        nsyms += region->nsymbols;
    }
    *num_syms = nsyms;

    // Allocate space
    symbol_type *syms = new symbol_type[nsyms];
    symbol_type *next_sym = syms;

    // Copy the symbols
    for (hash_entry_type *ptr = hash_->GetFirst(); ptr; ptr = hash_->GetNext()) {
        region_type *region = ptr->value;
        memcpy(next_sym, region->symbols, region->nsymbols * sizeof(symbol_type));
        next_sym += region->nsymbols;
    }

    return syms;
}

// This routine returns all the valid processes.
template<class T>
typename TraceReader<T>::ProcessState*
TraceReader<T>::GetProcesses(int *num_procs)
{
    // Count the valid processes
    int nprocs = 0;
    for (int ii = 0; ii < kNumPids; ++ii) {
        if (processes_[ii])
            nprocs += 1;
    }

    // Allocate a new array to hold the valid processes.
    ProcessState *procs = new ProcessState[nprocs];

    // Copy the processes to the new array.
    ProcessState *pstate = procs;
    for (int ii = 0; ii < kNumPids; ++ii) {
        if (processes_[ii])
            memcpy(pstate++, processes_[ii], sizeof(ProcessState));
    }

    *num_procs = nprocs;
    return procs;
}

// This routine returns the next valid process, or NULL if there are no
// more valid processes.
template<class T>
typename TraceReader<T>::ProcessState*
TraceReader<T>::GetNextProcess()
{
    while (next_pid_ < kNumPids) {
        if (processes_[next_pid_])
            return processes_[next_pid_++];
        next_pid_ += 1;
    }
    next_pid_ = 0;
    return NULL;
}

template<class T>
const char* TraceReader<T>::GetProcessName(int pid)
{
    if (pid < 0 || pid >= kNumPids || processes_[pid] == NULL)
        return "(unknown)";
    return processes_[pid]->name;
}

template<class T>
void TraceReader<T>::AddPredefinedRegion(region_type *region, const char *path,
                                         uint32_t vstart, uint32_t vend,
                                         uint32_t base)
{
    // Copy the path to make it easy to delete later.
    int len = strlen(path);
    region->path = new char[len + 1];
    strcpy(region->path, path);
    region->vstart = vstart;
    region->vend = vend;
    region->base_addr = base;
    region->flags = region_type::kIsKernelRegion;
}

template<class T>
void TraceReader<T>::InitRegionSymbols(region_type *region, int nsymbols)
{
    region->nsymbols = nsymbols;
    region->symbols = new symbol_type[nsymbols];
    memset(region->symbols, 0, nsymbols * sizeof(symbol_type));
}

template<class T>
void TraceReader<T>::AddRegionSymbol(region_type *region, int idx,
                                     uint32_t addr, const char *name,
                                     uint32_t flags)
{
    region->symbols[idx].addr = addr;
    region->symbols[idx].name = Strdup(name);
    region->symbols[idx].vm_sym = NULL;
    region->symbols[idx].region = region;
    region->symbols[idx].flags = flags;
}

template<class T>
void TraceReader<T>::AddPredefinedRegions(ProcessState *pstate)
{
    region_type *region = new region_type;
    AddPredefinedRegion(region, "(bootloader)", 0, 0x14, 0);
    InitRegionSymbols(region, 2);
    AddRegionSymbol(region, 0, 0, "(bootloader_start)", 0);
    AddRegionSymbol(region, 1, 0x14, "(bootloader_end)", 0);
    AddRegion(pstate, region);
    hash_->Update(region->path, region);

    region = new region_type;
    AddPredefinedRegion(region, "(exception vectors)", 0xffff0000, 0xffff0500,
                        0xffff0000);
    InitRegionSymbols(region, 2);
    AddRegionSymbol(region, 0, 0x0, "(vector_start)",
                    symbol_type::kIsVectorStart);
    AddRegionSymbol(region, 1, 0x500, "(vector_end)", 0);
    AddRegion(pstate, region);
    hash_->Update(region->path, region);

    region = new region_type;
    AddPredefinedRegion(region, "(atomic ops)", 0xffff0f80, 0xffff1000,
                        0xffff0f80);
    // Mark this region as also being mapped in user-space.
    // This isn't used anywhere in this code but client code can test for
    // this flag and decide whether to treat this as kernel or user code.
    region->flags |= region_type::kIsUserMappedRegion;

    InitRegionSymbols(region, 4);
    AddRegionSymbol(region, 0, 0x0, "(kuser_atomic_inc)", 0);
    AddRegionSymbol(region, 1, 0x20, "(kuser_atomic_dec)", 0);
    AddRegionSymbol(region, 2, 0x40, "(kuser_cmpxchg)", 0);
    AddRegionSymbol(region, 3, 0x80, "(kuser_end)", 0);
    AddRegion(pstate, region);
    hash_->Update(region->path, region);
}

template<class T>
void TraceReader<T>::ReadKernelSymbols(const char *kernel_file)
{
    region_type *region = new region_type;
    // Copy the path to make it easy to delete later.
    int len = strlen(kernel_file);
    region->path = new char[len + 1];
    strcpy(region->path, kernel_file);
    region->flags = region_type::kIsKernelRegion;
    ReadElfSymbols(region, kIncludeLocalSymbols);
    region->vend = 0xffff0000;
    AddRegion(processes_[0], region);
    processes_[0]->flags |= ProcessState::kHasKernelRegion;
    hash_->Update(region->path, region);
}

template<class T>
void TraceReader<T>::demangle_names(int nfuncs, symbol_type *functions)
{
    char *demangled;
    int status;

    for (int ii = 0; ii < nfuncs; ++ii) {
        demangled = NULL;
        int len = strlen(functions[ii].name);

        // If we don't check for "len > 1" then the demangler will incorrectly
        // expand 1-letter function names.  For example, "b" becomes "bool",
        // "c" becomes "char" and "d" becomes "double".  Also check that the
        // first character is an underscore.  Otherwise, on some strings
        // the demangler will try to read past the end of the string (because
        // the string is not really a C++ mangled name) and valgrind will
        // complain.
        if (demangle_ && len > 1 && functions[ii].name[0] == '_') {
            demangled = abi::__cxa_demangle(functions[ii].name, 0, NULL,
                                            &status);
        }

        if (demangled != NULL) {
            delete[] functions[ii].name;
            functions[ii].name = Strdup(demangled);
            free(demangled);
        }
    }
}

// Adds the symbols from the given ELF file to the given process.
// Returns false if the file was not an ELF file or if there was an
// error trying to read the sections of the ELF file.
template<class T>
bool TraceReader<T>::ReadElfSymbols(region_type *region, uint32_t flags)
{
    static char full_path[4096];
    Elf32_Shdr  *symtab, *symstr;
    Elf32_Ehdr  *hdr;
    Elf32_Shdr  *shdr;

    full_path[0] = 0;
    if (root_ && strcmp(root_, "/")) {
        strcpy(full_path, root_);
    }
    strcat(full_path, region->path);
    FILE *fobj = fopen(full_path, "r");
    if(fobj == NULL) {
    EmptyRegion:
        // we need to create an (unknown) symbol with address 0, otherwise some
        // other parts of the trace reader will simply crash when dealing with
        // an empty region
        region->vstart = 0;
        region->nsymbols = 1;
        region->symbols  = new symbol_type[1];
        memset(region->symbols, 0, sizeof(symbol_type));

        region->symbols[0].addr   = 0;
        region->symbols[0].name   = Strdup("(unknown)");
        region->symbols[0].vm_sym = NULL;
        region->symbols[0].region = region;
        region->symbols[0].flags  = 0;

        if (fobj != NULL)
            fclose(fobj);
        return false;
    }

    hdr = ReadElfHeader(fobj);
    if (hdr == NULL) {
        fprintf(stderr, "Cannot read ELF header from '%s'\n", full_path);
        goto EmptyRegion;
    }

    shdr = ReadSectionHeaders(hdr, fobj);
    if(shdr == NULL) {
        fprintf(stderr, "Can't read section headers from executable\n");
        goto EmptyRegion;
    }
    char *section_names = ReadStringTable(hdr, shdr, fobj);

    // Get the symbol table section
    symtab = FindSymbolTableSection(hdr, shdr, section_names);
    if (symtab == NULL || symtab->sh_size == 0) {
        fprintf(stderr, "Can't read symbol table from '%s'\n", full_path);
        goto EmptyRegion;
    }

    // Get the symbol string table section
    symstr = FindSymbolStringTableSection(hdr, shdr, section_names);
    if (symstr == NULL || symstr->sh_size == 0) {
        fprintf(stderr, "Can't read symbol string table from '%s'\n", full_path);
        goto EmptyRegion;
    }

    // Load the symbol string table data
    char *symbol_names = new char[symstr->sh_size];
    ReadSection(symstr, symbol_names, fobj);

    int num_entries = symtab->sh_size / symtab->sh_entsize;
    Elf32_Sym *elf_symbols = new Elf32_Sym[num_entries];
    ReadSection(symtab, elf_symbols, fobj);
    AdjustElfSymbols(hdr, elf_symbols, num_entries);
#if 0
    printf("size: %d, ent_size: %d, num_entries: %d\n",
           symtab->sh_size, symtab->sh_entsize, num_entries);
#endif
    int nfuncs = 0;

    // Allocate space for all of the symbols for now.  We will
    // reallocate space for just the function symbols after we
    // know how many there are.  Also, make sure there is room
    // for some extra symbols, including the text section names.
    int num_alloc = num_entries + hdr->e_shnum + 1;
    symbol_type *func_symbols = new symbol_type[num_alloc];
    memset(func_symbols, 0, num_alloc * sizeof(symbol_type));

    // If this is the shared library for a virtual machine, then
    // set the IsInterpreter flag for all symbols in that shared library.
    // This will allow us to replace the symbol names with the name of
    // the currently executing method on the virtual machine.
    int symbol_flags = 0;
    char *cp = strrchr(region->path, '/');
    if (cp != NULL) {
        // Move past the '/'
        cp += 1;
    } else {
        // There was no '/', so use the whole path
        cp = region->path;
    }
    if (strcmp(cp, "libdvm.so") == 0) {
        symbol_flags = symbol_type::kIsInterpreter;
    }

    bool zero_found = false;
    for (int ii = 1; ii < num_entries; ++ii) {
        int idx = elf_symbols[ii].st_name;

        // If the symbol does not have a name, or if the name starts with a
        // dollar sign ($), then skip it.
        if (idx == 0 || symbol_names[idx] == 0 || symbol_names[idx] == '$')
            continue;

        // If the section index is not executable, then skip it.
        uint32_t section = elf_symbols[ii].st_shndx;
        if (section == 0 || section >= hdr->e_shnum)
            continue;
        if ((shdr[section].sh_flags & SHF_EXECINSTR) == 0)
            continue;

        uint8_t sym_type = ELF32_ST_TYPE(elf_symbols[ii].st_info);
        uint8_t sym_bind = ELF32_ST_BIND(elf_symbols[ii].st_info);

        // Allow the caller to decide if we want local non-function
        // symbols to be included.  We currently include these symbols
        // only for the kernel, where it is useful because the kernel
        // has lots of assembly language labels that have meaningful names.
        if ((flags & kIncludeLocalSymbols) == 0 && sym_bind == STB_LOCAL
            && sym_type != STT_FUNC) {
            continue;
        }
#if 0
        printf("%08x %x %x %s\n",
               elf_symbols[ii].st_value,
               sym_bind,
               sym_type,
               &symbol_names[idx]);
#endif
        if (sym_type != STT_FUNC && sym_type != STT_NOTYPE)
            continue;

        if (elf_symbols[ii].st_value == 0)
            zero_found = true;

        // The address of thumb functions seem to have the low bit set,
        // even though the instructions are really at an even address.
        uint32_t addr = elf_symbols[ii].st_value & ~0x1;
        func_symbols[nfuncs].addr = addr;
        func_symbols[nfuncs].name = Strdup(&symbol_names[idx]);
        func_symbols[nfuncs].flags = symbol_flags;

        nfuncs += 1;
    }

    // Add a [0, "(unknown)"] symbol pair if there is not already a
    // symbol with the address zero.  We don't need to reallocate space
    // because we already have more than we need.
    if (!zero_found) {
        func_symbols[nfuncs].addr = 0;
        func_symbols[nfuncs].name = Strdup("(0 unknown)");
        nfuncs += 1;
    }

    // Add another entry at the end
    func_symbols[nfuncs].addr = 0xffffffff;
    func_symbols[nfuncs].name = Strdup("(end)");
    nfuncs += 1;

    // Add in the names of the text sections, but only if there
    // are no symbols with that address already.
    for (int section = 0; section < hdr->e_shnum; ++section) {
        if ((shdr[section].sh_flags & SHF_EXECINSTR) == 0)
            continue;

        uint32_t addr = shdr[section].sh_addr;
        // Search for a symbol with a matching address.  The symbols aren't
        // sorted yet so we just search the whole list.
        int ii;
        for (ii = 0; ii < nfuncs; ++ii) {
            if (addr == func_symbols[ii].addr)
                break;
        }
        if (ii == nfuncs) {
            // Symbol at address "addr" does not exist, so add the text
            // section name.  This will usually add the ".plt" section
            // (procedure linkage table).
            int idx = shdr[section].sh_name;
            func_symbols[nfuncs].addr = addr;
            func_symbols[nfuncs].name = Strdup(&section_names[idx]);
            if (strcmp(func_symbols[nfuncs].name, ".plt") == 0) {
                func_symbols[nfuncs].flags |= symbol_type::kIsPlt;
                // Change the name of the symbol to include the
                // name of the library.  Otherwise we will have lots
                // of ".plt" symbols.
                int len = strlen(region->path);
                len += strlen(":.plt");
                char *name = new char[len + 1];
                strcpy(name, region->path);
                strcat(name, ":.plt");
                delete[] func_symbols[nfuncs].name;
                func_symbols[nfuncs].name = name;

                // Check if this is part of the virtual machine interpreter
                char *cp = strrchr(region->path, '/');
                if (cp != NULL) {
                    // Move past the '/'
                    cp += 1;
                } else {
                    // There was no '/', so use the whole path
                    cp = region->path;
                }
                if (strcmp(cp, "libdvm.so") == 0) {
                    func_symbols[nfuncs].flags |= symbol_type::kIsInterpreter;
                }
            }
            nfuncs += 1;
        }
    }

    // Allocate just the space we need now that we know exactly
    // how many symbols we have.
    symbol_type *functions = new symbol_type[nfuncs];

    // Copy the symbols to the functions array
    memcpy(functions, func_symbols, nfuncs * sizeof(symbol_type));
    delete[] func_symbols;

    // Assign the region pointers
    for (int ii = 0; ii < nfuncs; ++ii) {
        functions[ii].region = region;
    }

    // Sort the symbols into increasing address order
    qsort(functions, nfuncs, sizeof(symbol_type), cmp_symbol_addr<T>);

    // If there are multiple symbols with the same address, then remove
    // the duplicates.  First, count the number of duplicates.
    uint32_t prev_addr = ~0;
    int num_duplicates = 0;
    for (int ii = 0; ii < nfuncs; ++ii) {
        if (prev_addr == functions[ii].addr)
            num_duplicates += 1;
        prev_addr = functions[ii].addr;
    }
 
    if (num_duplicates > 0) {
        int num_uniq = nfuncs - num_duplicates;

        // Allocate space for the unique functions
        symbol_type *uniq_functions = new symbol_type[num_uniq];

        // Copy the unique functions
        prev_addr = ~0;
        int next_uniq = 0;
        for (int ii = 0; ii < nfuncs; ++ii) {
            if (prev_addr == functions[ii].addr) {
                delete[] functions[ii].name;
                continue;
            }
            memcpy(&uniq_functions[next_uniq++], &functions[ii],
                   sizeof(symbol_type));
            prev_addr = functions[ii].addr;
        }
        assert(next_uniq == num_uniq);

        delete[] functions;
        functions = uniq_functions;
        nfuncs = num_uniq;
    }

    // Finally, demangle all of the symbol names
    demangle_names(nfuncs, functions);

    uint32_t min_addr = 0;
    if (!zero_found)
        min_addr = functions[1].addr;
    if (region->vstart == 0)
        region->vstart = min_addr;
    region->nsymbols = nfuncs;
    region->symbols = functions;

#if 0
    printf("%s num symbols: %d min_addr: 0x%x\n", region->path, nfuncs, min_addr);
    for (int ii = 0; ii < nfuncs; ++ii) {
        printf("0x%08x %s\n", functions[ii].addr, functions[ii].name);
    }
#endif
    delete[] elf_symbols;
    delete[] symbol_names;
    delete[] section_names;
    delete[] shdr;
    delete hdr;
    fclose(fobj);
    
    return true;
}

template<class T>
void TraceReader<T>::CopyKernelRegion(ProcessState *pstate)
{
    ProcessState *manager = pstate->addr_manager;
    if (manager->flags & ProcessState::kHasKernelRegion)
        return;

    int nregions = processes_[0]->nregions;
    region_type **regions = processes_[0]->regions;
    for (int ii = 0; ii < nregions; ii++) {
        if (regions[ii]->flags & region_type::kIsKernelRegion) {
            AddRegion(manager, regions[ii]);
            regions[ii]->refs += 1;
        }
    }
    manager->flags |= ProcessState::kHasKernelRegion;
}

template<class T>
void TraceReader<T>::ClearRegions(ProcessState *pstate)
{
    assert(pstate->pid != 0);
    int nregions = pstate->nregions;
    region_type **regions = pstate->regions;

    // Decrement the reference count on all the regions
    for (int ii = 0; ii < nregions; ii++) {
        if (regions[ii]->refs > 0) {
            regions[ii]->refs -= 1;
            continue;
        }

        delete regions[ii];
    }
    delete[] pstate->regions;
    pstate->regions = NULL;
    pstate->nregions = 0;
    pstate->max_regions = 0;
    pstate->addr_manager = pstate;
    pstate->flags &= ~ProcessState::kIsClone;
    pstate->flags &= ~ProcessState::kHasKernelRegion;
    CopyKernelRegion(pstate);
}

template<class T>
void TraceReader<T>::AddRegion(ProcessState *pstate, region_type *region)
{
    ProcessState *manager = pstate->addr_manager;
    if (manager->regions == NULL) {
        manager->max_regions = ProcessState::kInitialNumRegions;
        manager->regions = new region_type*[manager->max_regions];
        manager->nregions = 0;
    }

    // Check if we need to grow the array
    int nregions = manager->nregions;
    int max_regions = manager->max_regions;
    if (nregions >= max_regions) {
        max_regions <<= 1;
        manager->max_regions = max_regions;
        region_type **regions = new region_type*[max_regions];
        for (int ii = 0; ii < nregions; ii++) {
            regions[ii] = manager->regions[ii];
        }
        delete[] manager->regions;
        manager->regions = regions;
    }

    // Add the new region to the end of the array and resort
    manager->regions[nregions] = region;
    nregions += 1;
    manager->nregions = nregions;

    // Resort the regions into increasing start address
    qsort(manager->regions, nregions, sizeof(region_type*), cmp_region_addr<T>);
}

template<class T>
void TraceReader<T>::FindAndRemoveRegion(ProcessState *pstate, uint32_t vstart,
                                         uint32_t vend)
{
    ProcessState *manager = pstate->addr_manager;
    int nregions = manager->nregions;
    int index = FindRegionIndex(vstart, nregions, manager->regions);
    region_type *region = manager->regions[index];

    // If the region does not contain [vstart,vend], then return.
    if (vstart < region->vstart || vend > region->vend)
        return;

    // If the existing region exactly matches the address range [vstart,vend]
    // then remove the whole region.
    if (vstart == region->vstart && vend == region->vend) {
        // The regions are reference-counted.
        if (region->refs == 0) {
            // Free the region
            hash_->Remove(region->path);
            delete region;
        } else {
            region->refs -= 1;
        }

        if (nregions > 1) {
            // Assign the region at the end of the array to this empty slot
            manager->regions[index] = manager->regions[nregions - 1];

            // Resort the regions into increasing start address
            qsort(manager->regions, nregions - 1, sizeof(region_type*),
                  cmp_region_addr<T>);
        }
        manager->nregions = nregions - 1;
        return;
    }

    // If the existing region contains the given range and ends at the
    // end of the given range (a common case for some reason), then
    // truncate the existing region so that it ends at vstart (because
    // we are deleting the range [vstart,vend]).
    if (vstart > region->vstart && vend == region->vend) {
        region_type *truncated;

        if (region->refs == 0) {
            // This region is not shared, so truncate it directly
            truncated = region;
        } else {
            // This region is shared, so make a copy that we can truncate
            region->refs -= 1;
            truncated = region->MakePrivateCopy(new region_type);
        }
        truncated->vend = vstart;
        manager->regions[index] = truncated;
    }
}

template<class T>
void TraceReader<T>::CopyRegions(ProcessState *parent, ProcessState *child)
{
    // Copy the parent's address space
    ProcessState *manager = parent->addr_manager;
    int nregions = manager->nregions;
    child->nregions = nregions;
    child->max_regions = manager->max_regions;
    region_type **regions = new region_type*[manager->max_regions];
    child->regions = regions;
    memcpy(regions, manager->regions, nregions * sizeof(region_type*));

    // Increment the reference count on all the regions
    for (int ii = 0; ii < nregions; ii++) {
        regions[ii]->refs += 1;
    }
}

template<class T>
void TraceReader<T>::DumpRegions(FILE *stream, ProcessState *pstate) {
    ProcessState *manager = pstate->addr_manager;
    for (int ii = 0; ii < manager->nregions; ++ii) {
        fprintf(stream, "  %08x - %08x offset: %5x  nsyms: %4d refs: %d %s\n",
                manager->regions[ii]->vstart,
                manager->regions[ii]->vend,
                manager->regions[ii]->file_offset,
                manager->regions[ii]->nsymbols,
                manager->regions[ii]->refs,
                manager->regions[ii]->path);
    }
}

template<class T>
typename TraceReader<T>::region_type *
TraceReader<T>::FindRegion(uint32_t addr, int nregions, region_type **regions)
{
    int high = nregions;
    int low = -1;
    while (low + 1 < high) {
        int middle = (high + low) / 2;
        uint32_t middle_addr = regions[middle]->vstart;
        if (middle_addr == addr)
            return regions[middle];
        if (middle_addr > addr)
            high = middle;
        else
            low = middle;
    }

    // If we get here then we did not find an exact address match.  So use
    // the closest region address that is less than the given address.
    if (low < 0)
        low = 0;
    return regions[low];
}

template<class T>
int TraceReader<T>::FindRegionIndex(uint32_t addr, int nregions,
                                    region_type **regions)
{
    int high = nregions;
    int low = -1;
    while (low + 1 < high) {
        int middle = (high + low) / 2;
        uint32_t middle_addr = regions[middle]->vstart;
        if (middle_addr == addr)
            return middle;
        if (middle_addr > addr)
            high = middle;
        else
            low = middle;
    }

    // If we get here then we did not find an exact address match.  So use
    // the closest region address that is less than the given address.
    if (low < 0)
        low = 0;
    return low;
}

template<class T>
typename TraceReader<T>::symbol_type *
TraceReader<T>::FindFunction(uint32_t addr, int nsyms, symbol_type *symbols,
                             bool exact_match)
{
    int high = nsyms;
    int low = -1;
    while (low + 1 < high) {
        int middle = (high + low) / 2;
        uint32_t middle_addr = symbols[middle].addr;
        if (middle_addr == addr)
            return &symbols[middle];
        if (middle_addr > addr)
            high = middle;
        else
            low = middle;
    }

    // If we get here then we did not find an exact address match.  So use
    // the closest function address that is less than the given address.
    // We added a symbol with address zero so if there is no known
    // function containing the given address, then we will return the
    // "(unknown)" symbol.
    if (low >= 0 && !exact_match)
        return &symbols[low];
    return NULL;
}

template<class T>
typename TraceReader<T>::symbol_type *
TraceReader<T>::LookupFunction(int pid, uint32_t addr, uint64_t time)
{
    // Check if the previous match is still a good match.
    if (cached_pid_ == pid) {
        uint32_t vstart = cached_func_->region->vstart;
        uint32_t vend = cached_func_->region->vend;
        if (addr >= vstart && addr < vend) {
            uint32_t sym_addr = addr - cached_func_->region->base_addr;
            if (sym_addr >= cached_func_->addr
                && sym_addr < (cached_func_ + 1)->addr) {

                // Check if there is a Java method on the method trace.
                symbol_type *sym = FindCurrentMethod(pid, time);
                if (sym != NULL) {
                    sym->vm_sym = cached_func_;
                    return sym;
                }
                return cached_func_;
            }
        }
    }

    ProcessState *pstate = processes_[pid];
    if (pstate == NULL) {
        // There is no process state for the specified pid.
        // This should never happen.
        cached_pid_ = -1;
        cached_func_ = NULL;
        return NULL;
    }
    ProcessState *manager = pstate->addr_manager;
    cached_pid_ = pid;
    region_type *region = FindRegion(addr, manager->nregions, manager->regions);
    uint32_t sym_addr = addr - region->base_addr;

    cached_func_ = FindFunction(sym_addr, region->nsymbols, region->symbols,
                                false /* no exact match */);
    if (cached_func_ != NULL) {
        cached_func_->region = region;

        // Check if there is a Java method on the method trace.
        symbol_type *sym = FindCurrentMethod(pid, time);
        if (sym != NULL) {
            sym->vm_sym = cached_func_;
            return sym;
        }
    }

    return cached_func_;
}

template <class T>
void TraceReader<T>::HandlePidEvent(PidEvent *event)
{
    switch (event->rec_type) {
    case kPidFork:
    case kPidClone:
        // event->pid is the process id of the child
        if (event->pid >= kNumPids) {
            fprintf(stderr, "Error: pid (%d) too large\n", event->pid);
            exit(1);
        }
        // Create a new ProcessState struct for the child
        // and link it in at the front of the list for that
        // pid.
        {
            ProcessState *child = new ProcessState;
            processes_[event->pid] = child;
            child->pid = event->pid;
            child->tgid = event->tgid;

            // Link the new child at the front of the list (only needed if
            // pids wrap around, which will probably never happen when
            // tracing because it would take so long).
            child->next = processes_[event->pid];
            child->parent_pid = current_->pid;
            child->parent = current_;
            child->start_time = event->time;
            child->name = Strdup(current_->name);
            if (event->rec_type == kPidFork) {
                CopyRegions(current_, child);
            } else {
                // Share the parent's address space
                child->flags |= ProcessState::kIsClone;

                // The address space manager for the clone is the same
                // as the address space manager for the parent.  This works
                // even if the child later clones itself.
                child->addr_manager = current_->addr_manager;
            }
        }
        break;
    case kPidSwitch:
        // event->pid is the process id of the process we are
        // switching to.
        {
            uint64_t elapsed = event->time - function_start_time_;
            function_start_time_ = event->time;
            current_->cpu_time += elapsed;
        }
        if (current_->flags & ProcessState::kCalledExit)
            current_->end_time = event->time;

        if (event->pid >= kNumPids) {
            fprintf(stderr, "Error: pid (%d) too large\n", event->pid);
            exit(1);
        }

        // If the process we are switching to does not exist, then
        // create one.  This can happen because the tracing code does
        // not start tracing from the very beginning of the kernel.
        current_ = processes_[event->pid];
        if (current_ == NULL) {
            current_ = new ProcessState;
            processes_[event->pid] = current_;
            current_->pid = event->pid;
            current_->start_time = event->time;
            CopyKernelRegion(current_);
        }
#if 0
        {
            printf("switching to p%d\n", current_->pid);
            ProcessState *manager = current_->addr_manager;
            for (int ii = 0; ii < manager->nregions; ++ii) {
                printf("  %08x - %08x offset: %d nsyms: %4d %s\n",
                       manager->regions[ii]->vstart,
                       manager->regions[ii]->vend,
                       manager->regions[ii]->file_offset,
                       manager->regions[ii]->nsymbols,
                       manager->regions[ii]->path);
            }
        }
#endif
        break;
    case kPidExit:
        current_->exit_val = event->pid;
        current_->flags |= ProcessState::kCalledExit;
        break;
    case kPidMunmap:
        FindAndRemoveRegion(current_, event->vstart, event->vend);
        break;
    case kPidMmap:
        {
            region_type *region;
            region_type *existing_region = hash_->Find(event->path);
            if (existing_region == NULL
                || existing_region->vstart != event->vstart
                || existing_region->vend != event->vend
                || existing_region->file_offset != event->offset) {
                // Create a new region and add it to the current process'
                // address space.
                region = new region_type;

                // The event->path is allocated by ReadPidEvent() and owned
                // by us.
                region->path = event->path;
                region->vstart = event->vstart;
                region->vend = event->vend;
                region->file_offset = event->offset;
                if (existing_region == NULL) {
                    DexFileList *dexfile = dex_hash_->Find(event->path);
                    if (dexfile != NULL) {
                        PopulateSymbolsFromDexFile(dexfile, region);
                    } else {
                        ReadElfSymbols(region, 0);
                    }
                    hash_->Update(region->path, region);
                } else {
                    region->nsymbols = existing_region->nsymbols;
                    region->symbols = existing_region->symbols;
                    region->flags |= region_type::kSharedSymbols;
                }

                // The base_addr is subtracted from an address before the
                // symbol name lookup and is either zero or event->vstart.
                // HACK: Determine if base_addr is non-zero by looking at the
                // second symbol address (skip the first symbol because that is
                // the special symbol "(unknown)" with an address of zero).
                if (region->nsymbols > 2 && region->symbols[1].addr < event->vstart)
                    region->base_addr = event->vstart;

                // Treat all mmapped regions after the first as "libraries".
                // Profiling tools can test for this property.
                if (current_->flags & ProcessState::kHasFirstMmap)
                    region->flags |= region_type::kIsLibraryRegion;
                else
                    current_->flags |= ProcessState::kHasFirstMmap;
#if 0
                printf("%s vstart: 0x%x vend: 0x%x offset: 0x%x\n",
                       region->path, region->vstart, region->vend, region->file_offset);
#endif
            } else {
                region = existing_region;
                region->refs += 1;
                delete[] event->path;
            }
            AddRegion(current_, region);
        }
        break;
    case kPidExec:
        if (current_->argc > 0) {
            for (int ii = 0; ii < current_->argc; ii++) {
                delete[] current_->argv[ii];
            }
            delete[] current_->argv;
        }
        delete[] current_->name;

        current_->argc = event->argc;
        current_->argv = event->argv;
        current_->name = Strdup(current_->argv[0]);
        current_->flags |= ProcessState::kCalledExec;
        ClearRegions(current_);
        break;
    case kPidName:
    case kPidKthreadName:
        {
            ProcessState *pstate = processes_[event->pid];
            if (pstate == NULL) {
                pstate = new ProcessState;
                if (event->rec_type == kPidKthreadName) {
                    pstate->tgid = event->tgid;
                }
                pstate->pid = event->pid;
                pstate->start_time = event->time;
                processes_[event->pid] = pstate;
                CopyKernelRegion(pstate);
            } else {
                delete[] pstate->name;
            }
            pstate->name = event->path;
        }
        break;
    case kPidNoAction:
        break;
    case kPidSymbolAdd:
        delete[] event->path;
        break;
    case kPidSymbolRemove:
        break;
    }
}

// Finds the current pid for the given time.  This routine reads the pid
// trace file and assumes that the "time" parameter is monotonically
// increasing.
template <class T>
int TraceReader<T>::FindCurrentPid(uint64_t time)
{
    if (time < next_pid_event_.time)
        return current_->pid;

    while (1) {
        HandlePidEvent(&next_pid_event_);

        if (internal_pid_reader_->ReadPidEvent(&next_pid_event_)) {
            next_pid_event_.time = ~0ull;
            break;
        }
        if (next_pid_event_.time > time)
            break;
    }
    return current_->pid;
}

template <class T>
void TraceReader<T>::ProcessState::DumpStack(FILE *stream)
{
    const char *native;
    for (int ii = 0; ii < method_stack_top; ii++) {
        native = method_stack[ii].isNative ? "n" : " ";
        fprintf(stream, "%2d: %s 0x%08x\n", ii, native, method_stack[ii].addr);
    }
}

template <class T>
void TraceReader<T>::HandleMethodRecord(ProcessState *pstate,
                                        MethodRec *method_rec)
{
    uint32_t addr;
    int top = pstate->method_stack_top;
    int flags = method_rec->flags;
    bool isNative;
    if (flags == kMethodEnter || flags == kNativeEnter) {
        // Push this method on the stack
        if (top >= pstate->kMaxMethodStackSize) {
            fprintf(stderr, "Stack overflow at time %llu\n", method_rec->time);
            exit(1);
        }
        pstate->method_stack[top].addr = method_rec->addr;
        isNative = (flags == kNativeEnter);
        pstate->method_stack[top].isNative = isNative;
        pstate->method_stack_top = top + 1;
        addr = method_rec->addr;
    } else {
        if (top <= 0) {
            // If the stack underflows, then set the current method to NULL.
            pstate->current_method_sym = NULL;
            return;
        }
        top -= 1;
        addr = pstate->method_stack[top].addr;

        // If this is a non-native method then the address we are popping should
        // match the top-of-stack address.  Native pops don't always match the
        // address of the native push for some reason.
        if (addr != method_rec->addr && !pstate->method_stack[top].isNative) {
            fprintf(stderr,
                    "Stack method (0x%x) at index %d does not match trace record (0x%x) at time %llu\n",
                    addr, top, method_rec->addr, method_rec->time);
            pstate->DumpStack(stderr);
            exit(1);
        }

        // If we are popping a native method, then the top-of-stack should also
        // be a native method.
        bool poppingNative = (flags == kNativeExit) || (flags == kNativeException);
        if (poppingNative != pstate->method_stack[top].isNative) {
            fprintf(stderr,
                    "Popping native vs. non-native mismatch at index %d time %llu\n",
                    top, method_rec->time);
            pstate->DumpStack(stderr);
            exit(1);
        }

        pstate->method_stack_top = top;
        if (top == 0) {
            // When we empty the stack, set the current method to NULL
            pstate->current_method_sym = NULL;
            return;
        }
        addr = pstate->method_stack[top - 1].addr;
        isNative = pstate->method_stack[top - 1].isNative;
    }

    // If the top-of-stack is a native method, then set the current method
    // to NULL.
    if (isNative) {
        pstate->current_method_sym = NULL;
        return;
    }

    ProcessState *manager = pstate->addr_manager;
    region_type *region = FindRegion(addr, manager->nregions, manager->regions);
    uint32_t sym_addr = addr - region->base_addr;
    symbol_type *sym = FindFunction(sym_addr, region->nsymbols,
                                    region->symbols, true /* exact match */);

    pstate->current_method_sym = sym;
    if (sym != NULL) {
        sym->region = region;
    }
}

// Returns the current top-of-stack Java method, if any, for the given pid
// at the given time. The "time" parameter must be monotonically increasing
// across successive calls to this method.
// If the Java method stack is empty or if a native JNI method is on the
// top of the stack, then this method returns NULL.
template <class T>
typename TraceReader<T>::symbol_type*
TraceReader<T>::FindCurrentMethod(int pid, uint64_t time)
{
    ProcessState *procState = processes_[pid];

    if (time < next_method_.time) {
        return procState->current_method_sym;
    }

    while (1) {
        if (next_method_.time != 0) {
            // We may have to process methods from a different pid so use
            // a local variable here so that we don't overwrite procState.
            ProcessState *pState = processes_[next_method_.pid];
            HandleMethodRecord(pState, &next_method_);
        }

        if (internal_method_reader_->ReadMethod(&next_method_)) {
            next_method_.time = ~0ull;
            break;
        }
        if (next_method_.time > time)
            break;
    }
    return procState->current_method_sym;
}

template <class T>
void TraceReader<T>::PopulateSymbolsFromDexFile(const DexFileList *dexfile,
                                                region_type *region)
                                                
{
    int nsymbols = dexfile->nsymbols;
    DexSym *dexsyms = dexfile->symbols;
    region->nsymbols = nsymbols + 1;
    symbol_type *symbols = new symbol_type[nsymbols + 1];
    memset(symbols, 0, (nsymbols + 1) * sizeof(symbol_type));
    region->symbols = symbols;
    for (int ii = 0; ii < nsymbols; ii++) {
        symbols[ii].addr = dexsyms[ii].addr;
        symbols[ii].name = Strdup(dexsyms[ii].name);
        symbols[ii].vm_sym = NULL;
        symbols[ii].region = region;
        symbols[ii].flags = symbol_type::kIsMethod;
    }

    // Add an entry at the end with an address of 0xffffffff.  This
    // is required for LookupFunction() to work.
    symbol_type *symbol = &symbols[nsymbols];
    symbol->addr = 0xffffffff;
    symbol->name = Strdup("(end)");
    symbol->vm_sym = NULL;
    symbol->region = region;
    symbol->flags = symbol_type::kIsMethod;
}

template <class T>
bool TraceReader<T>::ReadMethodSymbol(MethodRec *method_record,
                                      symbol_type **psym,
                                      ProcessState **pproc)
{
    if (internal_method_reader_->ReadMethod(&next_method_)) {
        return true;
    }

    // Copy the whole MethodRec struct
    *method_record = next_method_;

    uint64_t time = next_method_.time;
    
    // Read the pid trace file up to this point to make sure the
    // process state is valid.
    FindCurrentPid(time);

    ProcessState *pstate = processes_[next_method_.pid];
    *pproc = pstate;
    HandleMethodRecord(pstate, &next_method_);
    *psym = pstate->current_method_sym;
    return false;
}

#endif /* TRACE_READER_H */
