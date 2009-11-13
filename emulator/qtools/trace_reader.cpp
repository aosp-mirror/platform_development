// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <inttypes.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <elf.h>
#include "trace_reader.h"
#include "decoder.h"

// A struct for creating temporary linked-lists of DexSym structs
struct DexSymList {
    DexSymList  *next;
    DexSym      sym;
};

// Declare static functions used in this file
static char *ExtractDexPathFromMmap(const char *mmap_path);
static void CopyDexSymbolsToArray(DexFileList *dexfile,
                                  DexSymList *head, int num_symbols);

// This function creates the pathname to the a specific trace file.  The
// string space is allocated in this routine and must be freed by the
// caller.
static char *CreateTracePath(const char *filename, const char *ext)
{
    char *fname;
    const char *base_start, *base_end;
    int ii, len, base_len, dir_len, path_len, qtrace_len;

    // Handle error cases
    if (filename == NULL || *filename == 0 || strcmp(filename, "/") == 0)
        return NULL;

    // Ignore a trailing slash, if any
    len = strlen(filename);
    if (filename[len - 1] == '/')
        len -= 1;

    // Find the basename.  We don't use basename(3) because there are
    // different behaviors for GNU and Posix in the case where the
    // last character is a slash.
    base_start = base_end = &filename[len];
    for (ii = 0; ii < len; ++ii) {
        base_start -= 1;
        if (*base_start == '/') {
            base_start += 1;
            break;
        }
    }
    base_len = base_end - base_start;
    dir_len = len - base_len;
    qtrace_len = strlen("/qtrace");

    // Create space for the pathname: "/dir/basename/qtrace.ext"
    // The "ext" string already contains the dot, so just add a byte
    // for the terminating zero.
    path_len = dir_len + base_len + qtrace_len + strlen(ext) + 1;
    fname = new char[path_len];
    if (dir_len > 0)
        strncpy(fname, filename, dir_len);
    fname[dir_len] = 0;
    strncat(fname, base_start, base_len);
    strcat(fname, "/qtrace");
    strcat(fname, ext);
    return fname;
}

inline BBReader::Future *BBReader::AllocFuture()
{
    Future *future = free_;
    free_ = free_->next;
    return future;
}

inline void BBReader::FreeFuture(Future *future)
{
    future->next = free_;
    free_ = future;
}

inline void BBReader::InsertFuture(Future *future)
{
    uint64_t future_time = future->bb.next_time;
    Future *prev = NULL;
    Future *ptr;
    for (ptr = head_; ptr; prev = ptr, ptr = ptr->next) {
        if (future_time <= ptr->bb.next_time)
            break;
    }
    if (prev == NULL) {
        // link it at the front
        future->next = head_;
        head_ = future;
    } else {
        // link it after "prev"
        future->next = prev->next;
        prev->next = future;
    }
}

// Decodes the next basic block record from the file.  Returns 1
// at end-of-file, otherwise returns 0.
inline int BBReader::DecodeNextRec()
{
    int64_t bb_diff = decoder_->Decode(true);
    uint64_t time_diff = decoder_->Decode(false);
    nextrec_.bb_rec.repeat = decoder_->Decode(false);
    if (time_diff == 0)
        return 1;
    if (nextrec_.bb_rec.repeat)
        nextrec_.bb_rec.time_diff = decoder_->Decode(false);
    nextrec_.bb_rec.bb_num += bb_diff;
    nextrec_.bb_rec.start_time += time_diff;
    return 0;
}

BBReader::BBReader(TraceReaderBase *trace)
{
    trace_ = trace;
    decoder_ = new Decoder;
}

BBReader::~BBReader()
{
    delete decoder_;
}

void BBReader::Open(const char *filename)
{
    // Initialize the class variables
    memset(&nextrec_, 0, sizeof(TimeRec));
    memset(futures_, 0, sizeof(Future) * kMaxNumBasicBlocks);
    head_ = NULL;

    // Link all of the futures_[] array elements on the free list.
    for (int ii = 0; ii < kMaxNumBasicBlocks - 1; ++ii) {
        futures_[ii].next = &futures_[ii + 1];
    }
    futures_[kMaxNumBasicBlocks - 1].next = 0;
    free_ = &futures_[0];

    // Open the trace.bb file
    char *fname = CreateTracePath(filename, ".bb");
    decoder_->Open(fname);
    is_eof_ = DecodeNextRec();
    delete[] fname;
}

void BBReader::Close()
{
    decoder_->Close();
}

// Returns true at end of file.
bool BBReader::ReadBB(BBEvent *event)
{
    if (is_eof_ && head_ == NULL) {
        return true;
    }

#if 0
    if (nextrec_) {
        printf("nextrec: buffer[%d], bb_num: %lld start: %d diff %d repeat %d next %u\n",
               nextrec_ - &buffer_[0],
               nextrec_->bb_rec.bb_num, nextrec_->bb_rec.start_time,
               nextrec_->bb_rec.time_diff, nextrec_->bb_rec.repeat,
               nextrec_->next_time);
    }
    if (head_) {
        printf("head: 0x%x, bb_num: %lld start: %d diff %d repeat %d next %u\n",
               head_,
               head_->bb->bb_rec.bb_num, head_->bb->bb_rec.start_time,
               head_->bb->bb_rec.time_diff, head_->bb->bb_rec.repeat,
               head_->bb->next_time);
    }
#endif
    if (!is_eof_) {
        if (head_) {
            TimeRec *bb = &head_->bb;
            if (bb->next_time < nextrec_.bb_rec.start_time) {
                // The head is earlier.
                event->time = bb->next_time;
                event->bb_num = bb->bb_rec.bb_num;
                event->bb_addr = trace_->GetBBAddr(event->bb_num);
                event->insns = trace_->GetInsns(event->bb_num);
                event->num_insns = trace_->FindNumInsns(event->bb_num, event->time);
                event->pid = trace_->FindCurrentPid(event->time);
                event->is_thumb = trace_->GetIsThumb(event->bb_num);

                // Remove the head element from the list
                Future *future = head_;
                head_ = head_->next;
                if (bb->bb_rec.repeat > 0) {
                    // there are more repetitions of this bb
                    bb->bb_rec.repeat -= 1;
                    bb->next_time += bb->bb_rec.time_diff;

                    // Insert this future into the sorted list
                    InsertFuture(future);
                } else {
                    // Add this future to the free list
                    FreeFuture(future);
                }
                return false;
            }
        }
        // The nextrec is earlier (or there was no head)
        event->time = nextrec_.bb_rec.start_time;
        event->bb_num = nextrec_.bb_rec.bb_num;
        event->bb_addr = trace_->GetBBAddr(event->bb_num);
        event->insns = trace_->GetInsns(event->bb_num);
        event->num_insns = trace_->FindNumInsns(event->bb_num, event->time);
        event->pid = trace_->FindCurrentPid(event->time);
        event->is_thumb = trace_->GetIsThumb(event->bb_num);
        if (nextrec_.bb_rec.repeat > 0) {
            Future *future = AllocFuture();
            future->bb.bb_rec = nextrec_.bb_rec;
            future->bb.bb_rec.repeat -= 1;
            future->bb.next_time = nextrec_.bb_rec.start_time + nextrec_.bb_rec.time_diff;
            InsertFuture(future);
        }

        is_eof_ = DecodeNextRec();
        return false;
    }

    //printf("using head_ 0x%x\n", head_);
    assert(head_);
    TimeRec *bb = &head_->bb;
    event->time = bb->next_time;
    event->bb_num = bb->bb_rec.bb_num;
    event->bb_addr = trace_->GetBBAddr(event->bb_num);
    event->insns = trace_->GetInsns(event->bb_num);
    event->num_insns = trace_->FindNumInsns(event->bb_num, event->time);
    event->pid = trace_->FindCurrentPid(event->time);
    event->is_thumb = trace_->GetIsThumb(event->bb_num);

    // Remove the head element from the list
    Future *future = head_;
    head_ = head_->next;
    if (bb->bb_rec.repeat > 0) {
        // there are more repetitions of this bb
        bb->bb_rec.repeat -= 1;
        bb->next_time += bb->bb_rec.time_diff;

        // Insert this future into the sorted list
        InsertFuture(future);
    } else {
        // Add this future to the free list
        FreeFuture(future);
    }
    return false;
}

InsnReader::InsnReader()
{
    decoder_ = new Decoder;
}

InsnReader::~InsnReader()
{
    delete decoder_;
}

void InsnReader::Open(const char *filename)
{
    prev_time_ = 0;
    time_diff_ = 0;
    repeat_ = -1;

    // Open the trace.insn file
    char *fname = CreateTracePath(filename, ".insn");
    decoder_->Open(fname);
    delete[] fname;
}

void InsnReader::Close()
{
    decoder_->Close();
}

uint64_t InsnReader::ReadInsnTime(uint64_t min_time)
{
    do {
        if (repeat_ == -1) {
            time_diff_ = decoder_->Decode(false);
            repeat_ = decoder_->Decode(false);
        }
        prev_time_ += time_diff_;
        repeat_ -= 1;
    } while (prev_time_ < min_time);
    return prev_time_;
}

AddrReader::AddrReader()
{
    decoder_ = new Decoder;
    opened_ = false;
}

AddrReader::~AddrReader()
{
    delete decoder_;
}

// Returns true if there is an error opening the file
bool AddrReader::Open(const char *filename, const char *suffix)
{
    struct stat stat_buf;

    prev_addr_ = 0;
    prev_time_ = 0;

    // Open the trace.addr file
    char *fname = CreateTracePath(filename, suffix);
    int rval = stat(fname, &stat_buf);
    if (rval == -1) {
        // The file does not exist
        delete[] fname;
        return true;
    }
    decoder_->Open(fname);
    opened_ = true;
    delete[] fname;
    return false;
}

void AddrReader::Close()
{
    decoder_->Close();
}

// Returns true at end of file.
bool AddrReader::ReadAddr(uint64_t *time, uint32_t *addr)
{
    if (!opened_) {
        fprintf(stderr, "Cannot read address trace\n");
        exit(1);
    }
    uint32_t addr_diff = decoder_->Decode(true);
    uint64_t time_diff = decoder_->Decode(false);
    if (time_diff == 0 && addr_diff == 0) {
        *addr = 0;
        *time = 0;
        return true;
    }
    prev_addr_ += addr_diff;
    prev_time_ += time_diff;
    *addr = prev_addr_;
    *time = prev_time_;
    return false;
}

ExcReader::ExcReader()
{
    decoder_ = new Decoder;
}

ExcReader::~ExcReader()
{
    delete decoder_;
}

void ExcReader::Open(const char *filename)
{
    prev_time_ = 0;
    prev_recnum_ = 0;

    // Open the trace.exc file
    char *fname = CreateTracePath(filename, ".exc");
    decoder_->Open(fname);
    delete[] fname;
}

void ExcReader::Close()
{
    decoder_->Close();
}

// Returns true at end of file.
bool ExcReader::ReadExc(uint64_t *time, uint32_t *current_pc, uint64_t *recnum,
                        uint32_t *target_pc, uint64_t *bb_num,
                        uint64_t *bb_start_time, int *num_insns)
{
    uint64_t time_diff = decoder_->Decode(false);
    uint32_t pc = decoder_->Decode(false);
    if ((time_diff | pc) == 0) {
        decoder_->Decode(false);
        decoder_->Decode(false);
        decoder_->Decode(false);
        decoder_->Decode(false);
        decoder_->Decode(false);
        return true;
    }
    uint64_t recnum_diff = decoder_->Decode(false);
    prev_time_ += time_diff;
    prev_recnum_ += recnum_diff;
    *time = prev_time_;
    *current_pc = pc;
    *recnum = prev_recnum_;
    *target_pc = decoder_->Decode(false);
    *bb_num = decoder_->Decode(false);
    *bb_start_time = decoder_->Decode(false);
    *num_insns = decoder_->Decode(false);
    return false;
}

PidReader::PidReader()
{
    decoder_ = new Decoder;
}

PidReader::~PidReader()
{
    delete decoder_;
}

void PidReader::Open(const char *filename)
{
    prev_time_ = 0;

    // Open the trace.pid file
    char *fname = CreateTracePath(filename, ".pid");
    decoder_->Open(fname);
    delete[] fname;
}

void PidReader::Close()
{
    decoder_->Close();
}

// Returns true at end of file.
bool PidReader::ReadPidEvent(PidEvent *event)
{
    uint64_t time_diff = decoder_->Decode(false);
    int rec_type = decoder_->Decode(false);
    prev_time_ += time_diff;
    event->time = prev_time_;
    event->rec_type = rec_type;
    switch(rec_type) {
        case kPidEndOfFile:
            return true;
        case kPidSwitch:
        case kPidExit:
            event->pid = decoder_->Decode(false);
            break;
        case kPidFork:
        case kPidClone:
            event->tgid = decoder_->Decode(false);
            event->pid = decoder_->Decode(false);
            break;
        case kPidMmap:
            {
                event->vstart = decoder_->Decode(false);
                event->vend = decoder_->Decode(false);
                event->offset = decoder_->Decode(false);
                int len = decoder_->Decode(false);
                char *path = new char[len + 1];
                decoder_->Read(path, len);
                path[len] = 0;
                event->path = path;
                event->mmap_path = path;
                char *dexfile = ExtractDexPathFromMmap(path);
                if (dexfile != NULL) {
                    delete[] event->path;
                    event->path = dexfile;
                }
            }
            break;
        case kPidMunmap:
            {
                event->vstart = decoder_->Decode(false);
                event->vend = decoder_->Decode(false);
            }
            break;
        case kPidSymbolAdd:
            {
                event->vstart = decoder_->Decode(false);
                int len = decoder_->Decode(false);
                char *path = new char[len + 1];
                decoder_->Read(path, len);
                path[len] = 0;
                event->path = path;
            }
            break;
        case kPidSymbolRemove:
            event->vstart = decoder_->Decode(false);
            break;
        case kPidExec:
            {
                int argc = decoder_->Decode(false);
                event->argc = argc;
                char **argv = new char*[argc];
                event->argv = argv;
                for (int ii = 0; ii < argc; ++ii) {
                    int alen = decoder_->Decode(false);
                    argv[ii] = new char[alen + 1];
                    decoder_->Read(argv[ii], alen);
                    argv[ii][alen] = 0;
                }
            }
            break;
        case kPidName:
        case kPidKthreadName:
            {
                if (rec_type == kPidKthreadName) {
                    event->tgid = decoder_->Decode(false);
                }
                event->pid = decoder_->Decode(false);
                int len = decoder_->Decode(false);
                char *path = new char[len + 1];
                decoder_->Read(path, len);
                path[len] = 0;
                event->path = path;
            }
            break;
    }
    return false;
}

// Frees the memory that might have been allocated for the given event.
void PidReader::Dispose(PidEvent *event)
{
    switch(event->rec_type) {
        case kPidMmap:
        case kPidSymbolAdd:
        case kPidName:
        case kPidKthreadName:
            delete[] event->path;
            event->path = NULL;
            event->mmap_path = NULL;
            break;

        case kPidExec:
            for (int ii = 0; ii < event->argc; ++ii) {
                delete[] event->argv[ii];
            }
            delete[] event->argv;
            event->argv = NULL;
            event->argc = 0;
            break;
    }
}


MethodReader::MethodReader()
{
    decoder_ = new Decoder;
    opened_ = false;
}

MethodReader::~MethodReader()
{
    delete decoder_;
}

bool MethodReader::Open(const char *filename)
{
    struct stat stat_buf;

    prev_time_ = 0;
    prev_addr_ = 0;
    prev_pid_ = 0;

    // Open the trace.method file
    char *fname = CreateTracePath(filename, ".method");
    int rval = stat(fname, &stat_buf);
    if (rval == -1) {
        // The file does not exist
        delete[] fname;
        return true;
    }
    decoder_->Open(fname);
    delete[] fname;
    opened_ = true;
    return false;
}

void MethodReader::Close()
{
    decoder_->Close();
}

// Returns true at end of file.
bool MethodReader::ReadMethod(MethodRec *method_record)
{
    if (!opened_)
        return true;
    uint64_t time_diff = decoder_->Decode(false);
    int32_t addr_diff = decoder_->Decode(true);
    if (time_diff == 0) {
        method_record->time = 0;
        method_record->addr = 0;
        method_record->flags = 0;
        return true;
    }
    int32_t pid_diff = decoder_->Decode(true);
    prev_time_ += time_diff;
    prev_addr_ += addr_diff;
    prev_pid_ += pid_diff;
    method_record->time = prev_time_;
    method_record->addr = prev_addr_;
    method_record->pid = prev_pid_;
    method_record->flags = decoder_->Decode(false);
    return false;
}

TraceReaderBase::TraceReaderBase()
{
    static_filename_ = NULL;
    static_fstream_ = NULL;
    header_ = new TraceHeader;
    bb_reader_ = new BBReader(this);
    insn_reader_ = new InsnReader;
    load_addr_reader_ = new AddrReader;
    store_addr_reader_ = new AddrReader;
    exc_reader_ = new ExcReader;
    pid_reader_ = new PidReader;
    method_reader_ = new MethodReader;
    internal_exc_reader_ = new ExcReader;
    internal_pid_reader_ = new PidReader;
    internal_method_reader_ = new MethodReader;
    blocks_ = NULL;
    bb_recnum_ = 0;
    exc_recnum_ = 0;
    exc_end_ = false;
    exc_bb_num_ = 0;
    exc_time_ = 0;
    exc_num_insns_ = 0;
    current_pid_ = 0;
    next_pid_ = 0;
    next_pid_switch_time_ = 0;
    post_processing_ = false;
    dex_hash_ = NULL;
    load_eof_ = false;
    load_time_ = 0;
    load_addr_ = 0;
    store_eof_ = false;
    store_time_ = 0;
    store_addr_ = 0;
}

TraceReaderBase::~TraceReaderBase()
{
    Close();
    delete bb_reader_;
    delete insn_reader_;
    delete load_addr_reader_;
    delete store_addr_reader_;
    delete exc_reader_;
    delete pid_reader_;
    delete method_reader_;
    delete internal_exc_reader_;
    delete internal_pid_reader_;
    delete internal_method_reader_;
    if (blocks_) {
        int num_static_bb = header_->num_static_bb;
        for (int ii = 0; ii < num_static_bb; ++ii) {
            delete[] blocks_[ii].insns;
        }
        delete[] blocks_;
    }
    delete header_;
    if (dex_hash_ != NULL) {
        HashTable<DexFileList*>::entry_type *ptr;
        for (ptr = dex_hash_->GetFirst(); ptr; ptr = dex_hash_->GetNext()) {
            DexFileList *dexfile = ptr->value;
            delete[] dexfile->path;
            int nsymbols = dexfile->nsymbols;
            DexSym *symbols = dexfile->symbols;
            for (int ii = 0; ii < nsymbols; ii++) {
                delete[] symbols[ii].name;
            }
            delete[] dexfile->symbols;
            delete dexfile;
        }
    }
    delete dex_hash_;
    delete[] static_filename_;
}

void TraceReaderBase::ReadTraceHeader(FILE *fstream, const char *filename,
                                      const char *tracename, TraceHeader *header)
{
    int rval = fread(header, sizeof(TraceHeader), 1, fstream);
    if (rval != 1) {
        perror(filename);
        exit(1);
    }

    if (!post_processing_ && strcmp(header->ident, TRACE_IDENT) != 0) {
        fprintf(stderr, "%s: missing trace header; run 'post_trace %s' first\n",
                filename, tracename);
        exit(1);
    }

    if (header->version != TRACE_VERSION) {
        fprintf(stderr,
                "%s: trace header version (%d) does not match compiled tools version (%d)\n",
                tracename, header->version, TRACE_VERSION);
        exit(1);
    }

    convert32(header->version);
    convert32(header->start_sec);
    convert32(header->start_usec);
    convert32(header->pdate);
    convert32(header->ptime);
    convert64(header->num_static_bb);
    convert64(header->num_static_insn);
    convert64(header->num_dynamic_bb);
    convert64(header->num_dynamic_insn);
    convert64(header->elapsed_usecs);
}


void TraceReaderBase::Open(const char *filename)
{
    char *fname;
    FILE *fstream;

    // Open the qtrace.bb file
    bb_reader_->Open(filename);

    // Open the qtrace.insn file
    insn_reader_->Open(filename);

    // Open the qtrace.load file and read the first line
    load_eof_ = load_addr_reader_->Open(filename, ".load");
    if (!load_eof_)
        load_eof_ = load_addr_reader_->ReadAddr(&load_time_, &load_addr_);

    // Open the qtrace.store file and read the first line
    store_eof_ = store_addr_reader_->Open(filename, ".store");
    if (!store_eof_)
        store_eof_ = store_addr_reader_->ReadAddr(&store_time_, &store_addr_);

    // Open the qtrace.exc file
    exc_reader_->Open(filename);

    // Open another file stream to the qtrace.exc file for internal reads.
    // This allows the caller to also read from the qtrace.exc file.
    internal_exc_reader_->Open(filename);

    // Open the qtrace.pid file
    pid_reader_->Open(filename);
    internal_pid_reader_->Open(filename);

    // Open the qtrace.method file
    method_reader_->Open(filename);
    internal_method_reader_->Open(filename);

    // Open the qtrace.static file
    fname = CreateTracePath(filename, ".static");
    static_filename_ = fname;

    fstream = fopen(fname, "r");
    if (fstream == NULL) {
        perror(fname);
        exit(1);
    }
    static_fstream_ = fstream;

    // Read the header
    ReadTraceHeader(fstream, fname, filename, header_);

    // Allocate space for all of the static blocks
    int num_static_bb = header_->num_static_bb;
    if (num_static_bb) {
        blocks_ = new StaticBlock[num_static_bb];

        // Read in all the static blocks
        for (int ii = 0; ii < num_static_bb; ++ii) {
            ReadStatic(&blocks_[ii].rec);
            int num_insns = blocks_[ii].rec.num_insns;
            if (num_insns > 0) {
                blocks_[ii].insns = new uint32_t[num_insns];
                ReadStaticInsns(num_insns, blocks_[ii].insns);
            } else {
                blocks_[ii].insns = NULL;
            }
        }
        fseek(static_fstream_, sizeof(TraceHeader), SEEK_SET);
    }

    ParseDexList(filename);

    // If the dex_hash_ is NULL, then assign it a small hash table
    // so that we can simply do a Find() operation without having
    // to check for NULL first.
    if (dex_hash_ == NULL) {
        dex_hash_ = new HashTable<DexFileList*>(1, NULL);
    }
}

// Reads the list of pid events looking for an mmap of a dex file.
PidEvent * TraceReaderBase::FindMmapDexFileEvent()
{
    static PidEvent event;

    while (!pid_reader_->ReadPidEvent(&event)) {
        if (event.rec_type == kPidMmap && event.path != event.mmap_path) {
            return &event;
        }
        pid_reader_->Dispose(&event);
    }
    return NULL;
}

static void CopyDexSymbolsToArray(DexFileList *dexfile,
                                  DexSymList *head, int num_symbols)
{
    if (dexfile == NULL)
        return;

    DexSym *symbols = NULL;
    if (num_symbols > 0) {
        symbols = new DexSym[num_symbols];
    }
    dexfile->nsymbols = num_symbols;
    dexfile->symbols = symbols;
    
    // Copy the linked-list to the array.
    DexSymList *next_sym = NULL;
    int next_index = 0;
    for (DexSymList *sym = head; sym; sym = next_sym) {
        next_sym = sym->next;
        symbols[next_index].addr = sym->sym.addr;
        symbols[next_index].len = sym->sym.len;
        symbols[next_index].name = sym->sym.name;
        next_index += 1;
        delete sym;
    }
}

void TraceReaderBase::ParseDexList(const char *filename)
{
    struct stat stat_buf;
    static const int kBufSize = 4096;
    char buf[kBufSize];
    char current_file[kBufSize];

    // Find an example dex file in the list of mmaps
    PidEvent *event = FindMmapDexFileEvent();

    // Reset the pid_reader to the beginning of the file.
    pid_reader_->Close();
    pid_reader_->Open(filename);

    // If there were no mmapped dex files, then there is no need to parse
    // the dexlist.
    if (event == NULL)
        return;
    char *mmap_dexfile = event->path;

    // Check if the dexlist file exists.  It should have the name
    // "qtrace.dexlist"
    char *fname = CreateTracePath(filename, ".dexlist");
    int rval = stat(fname, &stat_buf);
    if (rval == -1) {
        // The file does not exist
        delete[] fname;
        return;
    }

    // Open the qtrace.dexlist file
    FILE *fstream = fopen(fname, "r");
    if (fstream == NULL) {
        perror(fname);
        exit(1);
    }

    // First pass: read all the filenames, looking for a match for the
    // example mmap dex filename.  Also count the files so that we
    // know how big to make the hash table.
    char *match = NULL;
    int num_files = 0;
    while (fgets(buf, kBufSize, fstream)) {
        if (buf[0] != '#')
            continue;
        num_files += 1;
        match = strstr(buf + 1, mmap_dexfile);

        // Check that the dexlist file ends with the string mmap_dexfile.
        // We add one to the length of the mmap_dexfile because buf[]
        // ends with a newline.  The strlen(mmap_dexfile) computation
        // could be moved above the loop but it should only ever be
        // executed once.
        if (match != NULL && strlen(match) == strlen(mmap_dexfile) + 1)
            break;
    }

    // Count the rest of the files
    while (fgets(buf, kBufSize, fstream)) {
        if (buf[0] == '#')
            num_files += 1;
    }

    if (match == NULL) {
        fprintf(stderr,
                "Cannot find the mmapped dex file '%s' in the dexlist\n",
                mmap_dexfile);
        exit(1);
    }
    delete[] mmap_dexfile;

    // The prefix length includes the leading '#'.
    int prefix_len = match - buf;

    // Allocate a hash table
    dex_hash_ = new HashTable<DexFileList*>(4 * num_files, NULL);

    // Reset the file stream to the beginning
    rewind(fstream);

    // Second pass: read the filenames, stripping off the common prefix.
    // And read all the (address, method) mappings.  When we read a new
    // filename, create a new DexFileList and add it to the hash table.
    // Add new symbol mappings to a linked list until we have the whole
    // list and then create an array for them so that we can use binary
    // search on the address to find the symbol name quickly.

    // Use a linked list for storing the symbols
    DexSymList *head = NULL;
    DexSymList *prev = NULL;
    int num_symbols = 0;

    DexFileList *dexfile = NULL;
    int linenum = 0;
    while (fgets(buf, kBufSize, fstream)) {
        linenum += 1;
        if (buf[0] == '#') {
            // Everything after the '#' is a filename.
            // Ignore the common prefix.

            // First, save all the symbols from the previous file (if any).
            CopyDexSymbolsToArray(dexfile, head, num_symbols);

            dexfile = new DexFileList;
            // Subtract one because buf[] contains a trailing newline
            int pathlen = strlen(buf) - prefix_len - 1;
            char *path = new char[pathlen + 1];
            strncpy(path, buf + prefix_len, pathlen);
            path[pathlen] = 0;
            dexfile->path = path;
            dexfile->nsymbols = 0;
            dexfile->symbols = NULL;
            dex_hash_->Update(path, dexfile);
            num_symbols = 0;
            head = NULL;
            prev = NULL;
            continue;
        }

        uint32_t addr;
        int len, line;
        char clazz[kBufSize], method[kBufSize], sig[kBufSize], file[kBufSize];
        if (sscanf(buf, "0x%x %d %s %s %s %s %d",
                   &addr, &len, clazz, method, sig, file, &line) != 7) {
            fprintf(stderr, "Cannot parse line %d of file %s:\n%s",
                    linenum, fname, buf);
            exit(1);
        }

        // Concatenate the class name, method name, and signature
        // plus one for the period separating the class and method.
        int nchars = strlen(clazz) + strlen(method) + strlen(sig) + 1;
        char *name = new char[nchars + 1];
        strcpy(name, clazz);
        strcat(name, ".");
        strcat(name, method);
        strcat(name, sig);

        DexSymList *symbol = new DexSymList;
        symbol->sym.addr = addr;
        symbol->sym.len = len;
        symbol->sym.name = name;
        symbol->next = NULL;

        // Keep the list in the same order as the file
        if (head == NULL)
            head = symbol;
        if (prev != NULL)
            prev->next = symbol;
        prev = symbol;
        num_symbols += 1;
    }
    fclose(fstream);

    // Copy the symbols from the last file.
    CopyDexSymbolsToArray(dexfile, head, num_symbols);
    delete[] fname;
}

// Extracts the pathname to a jar file (or .apk file) from the mmap pathname.
// An example mmap pathname looks something like this:
//   /data/dalvik-cache/system@app@TestHarness.apk@classes.dex
// We want to convert that to this:
//   /system/app/TestHarness.apk
// If the pathname is not of the expected form, then NULL is returned.
// The space for the extracted path is allocated in this routine and should
// be freed by the caller after it is no longer needed.
static char *ExtractDexPathFromMmap(const char *mmap_path)
{
    const char *end = rindex(mmap_path, '@');
    if (end == NULL)
        return NULL;
    const char *start = rindex(mmap_path, '/');
    if (start == NULL)
        return NULL;
    int len = end - start;
    char *path = new char[len + 1];
    strncpy(path, start, len);
    path[len] = 0;

    // Replace all the occurrences of '@' with '/'
    for (int ii = 0; ii < len; ii++) {
        if (path[ii] == '@')
            path[ii] = '/';
    }
    return path;
}

void TraceReaderBase::Close()
{
    bb_reader_->Close();
    insn_reader_->Close();
    load_addr_reader_->Close();
    store_addr_reader_->Close();
    exc_reader_->Close();
    pid_reader_->Close();
    method_reader_->Close();
    internal_exc_reader_->Close();
    internal_pid_reader_->Close();
    internal_method_reader_->Close();
    fclose(static_fstream_);
    static_fstream_ = NULL;
}

void TraceReaderBase::WriteHeader(TraceHeader *header)
{
    TraceHeader swappedHeader;

    freopen(static_filename_, "r+", static_fstream_);
    fseek(static_fstream_, 0, SEEK_SET);

    memcpy(&swappedHeader, header, sizeof(TraceHeader));

    convert32(swappedHeader.version);
    convert32(swappedHeader.start_sec);
    convert32(swappedHeader.start_usec);
    convert32(swappedHeader.pdate);
    convert32(swappedHeader.ptime);
    convert64(swappedHeader.num_static_bb);
    convert64(swappedHeader.num_static_insn);
    convert64(swappedHeader.num_dynamic_bb);
    convert64(swappedHeader.num_dynamic_insn);
    convert64(swappedHeader.elapsed_usecs);

    fwrite(&swappedHeader, sizeof(TraceHeader), 1, static_fstream_);
}

// Reads the next StaticRec from the trace file (not including the list
// of instructions).  On end-of-file, this function returns true.
int TraceReaderBase::ReadStatic(StaticRec *rec)
{
    int rval = fread(rec, sizeof(StaticRec), 1, static_fstream_);
    if (rval != 1) {
        if (feof(static_fstream_)) {
            return true;
        }
        perror(static_filename_);
        exit(1);
    }
    convert64(rec->bb_num);
    convert32(rec->bb_addr);
    convert32(rec->num_insns);
    return false;
}

// Reads "num" instructions into the array "insns" which must be large
// enough to hold the "num" instructions.
// Returns the actual number of instructions read.  This will usually
// be "num" but may be less if end-of-file occurred.
int TraceReaderBase::ReadStaticInsns(int num, uint32_t *insns)
{
    if (num == 0)
        return 0;
    int rval = fread(insns, sizeof(uint32_t), num, static_fstream_);

    // Convert from little-endian, if necessary
    for (int ii = 0; ii < num; ++ii)
        convert32(insns[ii]);

    if (rval != num) {
        if (feof(static_fstream_)) {
            return rval;
        }
        perror(static_filename_);
        exit(1);
    }
    return rval;
}

void TraceReaderBase::TruncateLastBlock(uint32_t num_insns)
{
    uint32_t insns[kMaxInsnPerBB];
    StaticRec static_rec;
    long loc = 0, prev_loc = 0;

    freopen(static_filename_, "r+", static_fstream_);
    fseek(static_fstream_, sizeof(TraceHeader), SEEK_SET);

    // Find the last record
    while (1) {
        prev_loc = loc;
        loc = ftell(static_fstream_);

        // We don't need to byte-swap static_rec here because we are just
        // reading the records until we get to the last one.
        int rval = fread(&static_rec, sizeof(StaticRec), 1, static_fstream_);
        if (rval != 1)
            break;
        ReadStaticInsns(static_rec.num_insns, insns);
    }
    if (prev_loc != 0) {
        fseek(static_fstream_, prev_loc, SEEK_SET);
        static_rec.num_insns = num_insns;

        // Now we need to byte-swap, but just the field that we changed.
        convert32(static_rec.num_insns);
        fwrite(&static_rec, sizeof(StaticRec), 1, static_fstream_);
        int fd = fileno(static_fstream_);
        long len = ftell(static_fstream_);
        len += num_insns * sizeof(uint32_t);
        ftruncate(fd, len);
    }
}

int TraceReaderBase::FindNumInsns(uint64_t bb_num, uint64_t bb_start_time)
{
    int num_insns;

    // Read the exception trace file.  "bb_recnum_" is the number of
    // basic block records that have been read so far, and "exc_recnum_"
    // is the record number from the exception trace.
    while (!exc_end_ && exc_recnum_ < bb_recnum_) {
        uint32_t current_pc, target_pc;
        uint64_t time;

        exc_end_ = internal_exc_reader_->ReadExc(&time, &current_pc, &exc_recnum_,
                                                 &target_pc, &exc_bb_num_,
                                                 &exc_time_, &exc_num_insns_);
    }

    // If an exception occurred in this basic block, then use the
    // number of instructions specified in the exception record.
    if (!exc_end_ && exc_recnum_ == bb_recnum_) {
        num_insns = exc_num_insns_;
    } else {
        // Otherwise, use the number of instructions specified in the
        // static basic block.
        num_insns = blocks_[bb_num].rec.num_insns;
    }
    return num_insns;
}

// Finds the current pid for the given time.  This routine reads the pid
// trace file and assumes that the "time" parameter is monotonically
// increasing.
int TraceReaderBase::FindCurrentPid(uint64_t time)
{
    PidEvent event;

    if (time < next_pid_switch_time_)
        return current_pid_;

    current_pid_ = next_pid_;
    while (1) {
        if (internal_pid_reader_->ReadPidEvent(&event)) {
            next_pid_switch_time_ = ~0ull;
            break;
        }
        if (event.rec_type != kPidSwitch)
            continue;
        if (event.time > time) {
            next_pid_ = event.pid;
            next_pid_switch_time_ = event.time;
            break;
        }
        current_pid_ = event.pid;
    }
    return current_pid_;
}
