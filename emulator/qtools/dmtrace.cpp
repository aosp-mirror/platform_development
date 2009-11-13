// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>
#include <string.h>
#include <unistd.h>
#include "dmtrace.h"

static const short kVersion = 2;

const DmTrace::Header DmTrace::header = {
    0x574f4c53, kVersion, sizeof(DmTrace::Header), 0LL
};

static char *keyHeader = "*version\n" "2\n" "clock=thread-cpu\n";
static char *keyThreadHeader = "*threads\n";
static char *keyFunctionHeader = "*methods\n";
static char *keyEnd = "*end\n";

DmTrace::DmTrace() {
    fData = NULL;
    fTrace = NULL;
    threads = new std::vector<ThreadRecord*>;
    functions = new std::vector<FunctionRecord*>;
}

DmTrace::~DmTrace() {
    delete threads;
    delete functions;
}

void DmTrace::open(const char *dmtrace_file, uint64_t start_time)
{
    fTrace = fopen(dmtrace_file, "w");
    if (fTrace == NULL) {
        perror(dmtrace_file);
        exit(1);
    }

    // Make a temporary file to write the data into.
    char tmpData[32];
    strcpy(tmpData, "/tmp/dmtrace-data-XXXXXX");
    int data_fd = mkstemp(tmpData);
    if (data_fd < 0) {
        perror("Cannot create temporary file");
        exit(1);
    }

    // Ensure it goes away on exit.
    unlink(tmpData);
    fData = fdopen(data_fd, "w+");
    if (fData == NULL) {
        perror("Can't make temp data file");
        exit(1);
    }

    writeHeader(fData, start_time);
}

void DmTrace::close()
{
    if (fTrace == NULL)
        return;
    writeKeyFile(fTrace);

    // Take down how much data we wrote to the temp data file.
    long size = ftell(fData);
    // Rewind the data file and append its contents to the trace file.
    rewind(fData);
    char *data = (char *)malloc(size);
    fread(data, size, 1, fData);
    fwrite(data, size, 1, fTrace);
    free(data);
    fclose(fData);
    fclose(fTrace);
}

/*
 * Write values to the binary data file.
 */
void DmTrace::write2LE(FILE* fstream, unsigned short val)
{
    putc(val & 0xff, fstream);
    putc(val >> 8, fstream);
}

void DmTrace::write4LE(FILE* fstream, unsigned int val)
{
    putc(val & 0xff, fstream);
    putc((val >> 8) & 0xff, fstream);
    putc((val >> 16) & 0xff, fstream);
    putc((val >> 24) & 0xff, fstream);
}

void DmTrace::write8LE(FILE* fstream, unsigned long long val)
{
    putc(val & 0xff, fstream);
    putc((val >> 8) & 0xff, fstream);
    putc((val >> 16) & 0xff, fstream);
    putc((val >> 24) & 0xff, fstream);
    putc((val >> 32) & 0xff, fstream);
    putc((val >> 40) & 0xff, fstream);
    putc((val >> 48) & 0xff, fstream);
    putc((val >> 56) & 0xff, fstream);
}

void DmTrace::writeHeader(FILE *fstream, uint64_t startTime)
{
    write4LE(fstream, header.magic);
    write2LE(fstream, header.version);
    write2LE(fstream, header.offset);
    write8LE(fstream, startTime);
}

void DmTrace::writeDataRecord(FILE *fstream, int threadId,
                             unsigned int methodVal,
                             unsigned int elapsedTime)
{
    write2LE(fstream, threadId);
    write4LE(fstream, methodVal);
    write4LE(fstream, elapsedTime);
}

void DmTrace::addFunctionEntry(int functionId, uint32_t cycle, uint32_t pid)
{
    writeDataRecord(fData, pid, functionId, cycle);
}

void DmTrace::addFunctionExit(int functionId, uint32_t cycle, uint32_t pid)
{
    writeDataRecord(fData, pid, functionId | 1, cycle);
}

void DmTrace::addFunction(int functionId, const char *name)
{
    FunctionRecord *rec = new FunctionRecord;
    rec->id = functionId;
    rec->name = name;
    functions->push_back(rec);
}

void DmTrace::addFunction(int functionId, const char *clazz,
                          const char *method, const char *sig)
{
    // Allocate space for all the strings, plus 2 tab separators plus null byte.
    // We currently don't reclaim this space.
    int len = strlen(clazz) + strlen(method) + strlen(sig) + 3;
    char *name = new char[len];
    sprintf(name, "%s\t%s\t%s", clazz, method, sig);

    addFunction(functionId, name);
}

void DmTrace::parseAndAddFunction(int functionId, const char *name)
{
    // Parse the "name" string into "class", "method" and "signature".
    // The "name" string should look something like this:
    //   name = "java.util.LinkedList.size()I"
    // and it will be parsed into this:
    //   clazz = "java.util.LinkedList"
    //   method = "size"
    //   sig = "()I"

    // Find the first parenthesis, the start of the signature.
    char *paren = (char*)strchr(name, '(');

    // If not found, then add the original name.
    if (paren == NULL) {
        addFunction(functionId, name);
        return;
    }

    // Copy the signature
    int len = strlen(paren) + 1;
    char *sig = new char[len];
    strcpy(sig, paren);

    // Zero the parenthesis so that we can search backwards from the signature
    *paren = 0;

    // Search for the last period, the start of the method name
    char *dot = (char*)strrchr(name, '.');

    // If not found, then add the original name.
    if (dot == NULL || dot == name) {
        delete[] sig;
        *paren = '(';
        addFunction(functionId, name);
        return;
    }

    // Copy the method, not including the dot
    len = strlen(dot + 1) + 1;
    char *method = new char[len];
    strcpy(method, dot + 1);

    // Zero the dot to delimit the class name
    *dot = 0;

    addFunction(functionId, name, method, sig);

    // Free the space we allocated.
    delete[] sig;
    delete[] method;
}

void DmTrace::addThread(int threadId, const char *name)
{
    ThreadRecord *rec = new ThreadRecord;
    rec->id = threadId;
    rec->name = name;
    threads->push_back(rec);
}

void DmTrace::updateName(int threadId, const char *name)
{
    std::vector<ThreadRecord*>::iterator iter;

    for (iter = threads->begin(); iter != threads->end(); ++iter) {
        if ((*iter)->id == threadId) {
            (*iter)->name = name;
            return;
        }
    }
}

void DmTrace::writeKeyFile(FILE *fstream)
{
    fwrite(keyHeader, strlen(keyHeader), 1, fstream);
    writeThreads(fstream);
    writeFunctions(fstream);
    fwrite(keyEnd, strlen(keyEnd), 1, fstream);
}

void DmTrace::writeThreads(FILE *fstream)
{
    std::vector<ThreadRecord*>::iterator iter;

    fwrite(keyThreadHeader, strlen(keyThreadHeader), 1, fstream);
    for (iter = threads->begin(); iter != threads->end(); ++iter) {
        fprintf(fstream, "%d\t%s\n", (*iter)->id, (*iter)->name);
    }
}

void DmTrace::writeFunctions(FILE *fstream)
{
    std::vector<FunctionRecord*>::iterator iter;

    fwrite(keyFunctionHeader, strlen(keyFunctionHeader), 1, fstream);
    for (iter = functions->begin(); iter != functions->end(); ++iter) {
        fprintf(fstream, "0x%x\t%s\n", (*iter)->id, (*iter)->name);
    }
}
