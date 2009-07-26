// Copyright 2006 The Android Open Source Project

#ifndef TRACE_READER_BASE_H
#define TRACE_READER_BASE_H

#include <inttypes.h>
#include "trace_common.h"
#include "hash_table.h"

class BBReader;
class InsnReader;
class AddrReader;
class ExcReader;
class PidReader;
class MethodReader;

struct StaticRec {
    uint64_t    bb_num;
    uint32_t    bb_addr;
    uint32_t    num_insns;
};

struct StaticBlock {
    StaticRec   rec;
    uint32_t    *insns;
};

struct BBEvent {
    uint64_t    time;
    uint64_t    bb_num;
    uint32_t    bb_addr;
    uint32_t    *insns;
    int         num_insns;
    int         pid;
    int         is_thumb;
};

struct PidEvent {
    uint64_t    time;
    int         rec_type;       // record type: fork, context switch, exit ...
    int         tgid;           // thread group id
    int         pid;            // for fork: child pid; for switch: next pid;
                                //   for exit: exit value
    uint32_t    vstart;         // virtual start address (only used with mmap)
    uint32_t    vend;           // virtual end address (only used with mmap)
    uint32_t    offset;         // virtual file offset (only used with mmap)

    // Dynamically allocated path to executable (or lib). In the case of
    // an mmapped dex file, the path is modified to be more useful for
    // comparing against the output of dexlist.  For example, instead of this:
    //   /data/dalvik-cache/system@app@TestHarness.apk@classes.dex
    // We convert to this:
    //   /system/app/TestHarness.apk
    char        *path;
    char        *mmap_path;     // unmodified mmap path
    int         argc;           // number of args
    char        **argv;         // dynamically allocated array of args
};

struct MethodRec {
    uint64_t    time;
    uint32_t    addr;
    int         pid;
    int         flags;
};

struct DexSym {
    uint32_t    addr;
    int         len;
    char        *name;
};

struct DexFileList {
    char        *path;
    int         nsymbols;
    DexSym      *symbols;
};

class TraceReaderBase {
  public:
    TraceReaderBase();
    virtual ~TraceReaderBase();

    friend class BBReader;

    void                Open(const char *filename);
    void                Close();
    void                WriteHeader(TraceHeader *header);
    inline bool         ReadBB(BBEvent *event);
    int                 ReadStatic(StaticRec *rec);
    int                 ReadStaticInsns(int num, uint32_t *insns);
    TraceHeader         *GetHeader()                { return header_; }
    inline uint64_t     ReadInsnTime(uint64_t min_time);
    void                TruncateLastBlock(uint32_t num_insns);
    inline bool         ReadAddr(uint64_t *time, uint32_t *addr, int *flags);
    inline bool         ReadExc(uint64_t *time, uint32_t *current_pc,
                                uint64_t *recnum, uint32_t *target_pc,
                                uint64_t *bb_num, uint64_t *bb_start_time,
                                int *num_insns);
    inline bool         ReadPidEvent(PidEvent *event);
    inline bool         ReadMethod(MethodRec *method_record);
    StaticBlock         *GetStaticBlock(uint64_t bb_num) { return &blocks_[bb_num]; }
    uint32_t            *GetInsns(uint64_t bb_num) { return blocks_[bb_num].insns; }
    uint32_t            GetBBAddr(uint64_t bb_num) {
        return blocks_[bb_num].rec.bb_addr & ~1;
    }
    int                 GetIsThumb(uint64_t bb_num) {
        return blocks_[bb_num].rec.bb_addr & 1;
    }
    void                SetPostProcessing(bool val) { post_processing_ = val; }

  protected:
    virtual int         FindCurrentPid(uint64_t time);
    int                 current_pid_;
    int                 next_pid_;
    uint64_t            next_pid_switch_time_;
    PidReader           *internal_pid_reader_;
    MethodReader        *internal_method_reader_;
    HashTable<DexFileList*> *dex_hash_;

  private:
    int          FindNumInsns(uint64_t bb_num, uint64_t bb_start_time);
    void         ReadTraceHeader(FILE *fstream, const char *filename,
                                const char *tracename, TraceHeader *header);
    PidEvent     *FindMmapDexFileEvent();
    void         ParseDexList(const char *filename);

    char         *static_filename_;
    FILE         *static_fstream_;
    TraceHeader  *header_;
    BBReader     *bb_reader_;
    InsnReader   *insn_reader_;
    AddrReader   *load_addr_reader_;
    AddrReader   *store_addr_reader_;
    ExcReader    *exc_reader_;
    PidReader    *pid_reader_;
    MethodReader *method_reader_;
    ExcReader    *internal_exc_reader_;
    StaticBlock  *blocks_;
    bool         exc_end_;
    uint64_t     bb_recnum_;
    uint64_t     exc_recnum_;
    uint64_t     exc_bb_num_;
    uint64_t     exc_time_;
    int          exc_num_insns_;
    bool         post_processing_;

    bool         load_eof_;
    uint64_t     load_time_;
    uint32_t     load_addr_;
    bool         store_eof_;
    uint64_t     store_time_;
    uint32_t     store_addr_;
};

class Decoder;

class BBReader {
  public:
    explicit BBReader(TraceReaderBase *trace);
    ~BBReader();
    void     Open(const char *filename);
    void     Close();
    bool     ReadBB(BBEvent *event);

  private:
    struct TimeRec {
        BBRec       bb_rec;
        uint64_t    next_time;
    };

    struct Future {
        Future      *next;
        TimeRec     bb;
    };

    inline Future   *AllocFuture();
    inline void     FreeFuture(Future *future);
    inline void     InsertFuture(Future *future);
    inline int      DecodeNextRec();

    TimeRec         nextrec_;
    Future          futures_[kMaxNumBasicBlocks];
    Future          *head_;
    Future          *free_;
    Decoder         *decoder_;
    bool            is_eof_;
    TraceReaderBase *trace_;
};

class InsnReader {
  public:
    InsnReader();
    ~InsnReader();

    void        Open(const char *filename);
    void        Close();
    uint64_t    ReadInsnTime(uint64_t min_time);

  private:
    Decoder     *decoder_;
    uint64_t    prev_time_;
    uint64_t    time_diff_;
    int         repeat_;
};

class AddrReader {
  public:
    AddrReader();
    ~AddrReader();

    bool        Open(const char *filename, const char *suffix);
    void        Close();
    bool        ReadAddr(uint64_t *time, uint32_t *addr);

  private:
    Decoder     *decoder_;
    uint32_t    prev_addr_;
    uint64_t    prev_time_;
    bool        opened_;        // true after file is opened
};

class ExcReader {
  public:
    ExcReader();
    ~ExcReader();

    void        Open(const char *filename);
    void        Close();
    bool        ReadExc(uint64_t *time, uint32_t *current_pc,
                        uint64_t *recnum, uint32_t *target_pc,
                        uint64_t *bb_num, uint64_t *bb_start_time,
                        int *num_insns);

  private:
    Decoder     *decoder_;
    uint64_t    prev_time_;
    uint64_t    prev_recnum_;
};

class PidReader {
  public:
    PidReader();
    ~PidReader();

    void        Open(const char *filename);
    void        Close();
    bool        ReadPidEvent(struct PidEvent *event);
    void        Dispose(struct PidEvent *event);

  private:
    Decoder     *decoder_;
    uint64_t    prev_time_;
};

class MethodReader {
  public:
    MethodReader();
    ~MethodReader();

    bool        Open(const char *filename);
    void        Close();
    bool        ReadMethod(MethodRec *method_record);

  private:
    Decoder     *decoder_;
    uint64_t    prev_time_;
    uint32_t    prev_addr_;
    int32_t     prev_pid_;
    bool        opened_;        // true after file is opened
};

// Reads the next dynamic basic block from the trace.
// Returns true on end-of-file.
inline bool TraceReaderBase::ReadBB(BBEvent *event)
{
    bb_recnum_ += 1;
    return bb_reader_->ReadBB(event);
}

inline uint64_t TraceReaderBase::ReadInsnTime(uint64_t min_time)
{
    return insn_reader_->ReadInsnTime(min_time);
}

inline bool TraceReaderBase::ReadAddr(uint64_t *time, uint32_t *addr, int *flags)
{
    if (load_eof_ && store_eof_)
        return true;

    if (store_eof_ || (!load_eof_ && load_time_ <= store_time_)) {
        *time = load_time_;
        *addr = load_addr_;
        *flags = 0;
        load_eof_ = load_addr_reader_->ReadAddr(&load_time_, &load_addr_);
    } else {
        *time = store_time_;
        *addr = store_addr_;
        *flags = 1;
        store_eof_ = store_addr_reader_->ReadAddr(&store_time_, &store_addr_);
    }
    return false;
}

inline bool TraceReaderBase::ReadExc(uint64_t *time, uint32_t *current_pc,
                                     uint64_t *recnum, uint32_t *target_pc,
                                     uint64_t *bb_num, uint64_t *bb_start_time,
                                     int *num_insns)
{
    return exc_reader_->ReadExc(time, current_pc, recnum, target_pc, bb_num,
                                bb_start_time, num_insns);
}

inline bool TraceReaderBase::ReadPidEvent(PidEvent *event)
{
    return pid_reader_->ReadPidEvent(event);
}

inline bool TraceReaderBase::ReadMethod(MethodRec *method_record)
{
    return method_reader_->ReadMethod(method_record);
}

// Duplicates a string, allocating space using new[].
inline char * Strdup(const char *src) {
    int len = strlen(src);
    char *copy = new char[len + 1];
    strcpy(copy, src);
    return copy;
}

#endif /* TRACE_READER_BASE_H */
