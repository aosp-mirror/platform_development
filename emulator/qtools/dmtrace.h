// Copyright 2006 The Android Open Source Project

#ifndef DMTRACE_H
#define DMTRACE_H

#include <vector>

class DmTrace {
  public:
    struct Header {
        uint32_t        magic;
        uint16_t        version;
        uint16_t        offset;
        uint64_t        date_time;
    };

    DmTrace();
    ~DmTrace();

    void        open(const char *dmtrace_file, uint64_t startTime);
    void        close();
    void        addFunctionEntry(int methodId, uint32_t cycle, uint32_t pid);
    void        addFunctionExit(int methodId, uint32_t cycle, uint32_t pid);
    void        addFunction(int functionId, const char *name);
    void        addFunction(int functionId, const char *clazz, const char *method,
                            const char *sig);
    void        parseAndAddFunction(int functionId, const char *name);
    void        addThread(int threadId, const char *name);
    void        updateName(int threadId, const char *name);

  private:
    static const Header header;

    struct ThreadRecord {
        int             id;
        const char      *name;
    };

    struct FunctionRecord {
        int             id;
        const char      *name;
    };

    void        write2LE(FILE* fstream, unsigned short val);
    void        write4LE(FILE* fstream, unsigned int val);
    void        write8LE(FILE* fstream, unsigned long long val);
    void        writeHeader(FILE *fstream, uint64_t startTime);
    void        writeDataRecord(FILE *fstream, int threadId,
                                unsigned int methodVal,
                                unsigned int elapsedTime);
    void        writeKeyFile(FILE *fstream);
    void        writeThreads(FILE *fstream);
    void        writeFunctions(FILE *fstream);

    FILE        *fData;
    FILE        *fTrace;
    std::vector<ThreadRecord*> *threads;
    std::vector<FunctionRecord*> *functions;
};

#endif  // DMTRACE_H
