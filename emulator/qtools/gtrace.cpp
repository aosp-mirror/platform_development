#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "gtrace.h"

// A buffer of zeros
static char zeros[Gtrace::kGtraceEntriesPerBlock * sizeof(Gtrace::trace_entry)];

Gtrace::Gtrace() {
  gtrace_file_ = NULL;
  ftrace_ = NULL;
  fnames_ = NULL;
  start_sec_ = 0;
  pdate_ = 0;
  ptime_ = 0;
  num_entries_ = 0;
  blockno_ = 1;
  current_pid_ = 0;
}

Gtrace::~Gtrace() {
  if (ftrace_) {
    // Extend the trace file to a multiple of 8k. Otherwise gtracepost64
    // complains.
    long pos = ftell(ftrace_);
    long pos_end = (pos + 0x1fff) & ~0x1fff;
    if (pos_end > pos) {
      char ch = 0;
      fseek(ftrace_, pos_end - 1, SEEK_SET);
      fwrite(&ch, 1, 1, ftrace_);
    }
    fclose(ftrace_);
  }
  if (fnames_)
    fclose(fnames_);
}

void Gtrace::Open(const char *gtrace_file, uint32_t pdate, uint32_t ptime)
{
  ftrace_ = fopen(gtrace_file, "w");
  if (ftrace_ == NULL) {
    perror(gtrace_file);
    exit(1);
  }
  gtrace_file_ = gtrace_file;

  pdate_ = pdate;
  ptime_ = ptime;
  sprintf(gname_file_, "gname_%x_%06x.txt", pdate, ptime);
  fnames_ = fopen(gname_file_, "w");
  if (fnames_ == NULL) {
    perror(gname_file_);
    exit(1);
  }
  fprintf(fnames_, "# File# Proc# Line# Name\n");
}

void Gtrace::WriteFirstHeader(uint32_t start_sec, uint32_t pid)
{
  first_header fh;
  current_pid_ = pid;
  start_sec_ = start_sec;
  FillFirstHeader(start_sec, pid, &fh);
  fwrite(&fh, sizeof(fh), 1, ftrace_);
  num_entries_ = 8;
}

void Gtrace::FillFirstHeader(uint32_t start_sec, uint32_t pid,
                             first_header *fh) {
  int cpu = 0;
  int max_files = 16;
  int max_procedures = 12;

  fh->common.blockno = 0;
  fh->common.entry_width = 8;
  fh->common.block_tic = kBaseTic;
  fh->common.block_time = start_sec;
  //fh->common.usec_cpu = (start_usec << 8) | (cpu & 0xff);
  fh->common.usec_cpu = cpu & 0xff;
  fh->common.pid = pid;
  fh->common.bug_count = 0;
  fh->common.zero_count = 0;

  fh->tic = kBaseTic + 1;
  fh->one = 1;
  fh->tics_per_second = kTicsPerSecond;
  fh->trace_time = start_sec;
  fh->version = 5;
  fh->file_proc = (max_files << 8) | max_procedures;
  fh->pdate = pdate_;
  fh->ptime = ptime_;
}

void Gtrace::WriteBlockHeader(uint32_t cycle, uint32_t pid)
{
  int cpu = 0;
  block_header bh;

  bh.blockno = blockno_++;
  bh.entry_width = 8;
  bh.block_tic = cycle + kBaseTic;
  bh.block_time = start_sec_ + cycle / kTicsPerSecond;
  //bh.usec_cpu = (start_usec << 8) | (cpu & 0xff);
  bh.usec_cpu = cpu & 0xff;
  bh.pid = pid;
  bh.bug_count = 0;
  bh.zero_count = 0;
  fwrite(&bh, sizeof(bh), 1, ftrace_);
}

void Gtrace::AddGtraceRecord(int filenum, int procnum, uint32_t cycle, uint32_t pid,
                             int is_exit)
{
  trace_entry	entry;

  if (current_pid_ != pid) {
    current_pid_ = pid;

    // We are switching to a new process id, so pad the current block
    // with zeros.
    int num_zeros = (kGtraceEntriesPerBlock - num_entries_) * sizeof(entry);
    fwrite(zeros, num_zeros, 1, ftrace_);
    WriteBlockHeader(cycle, pid);
    num_entries_ = 4;
  }

  // If the current block is full, write out a new block header
  if (num_entries_ == kGtraceEntriesPerBlock) {
    WriteBlockHeader(cycle, pid);
    num_entries_ = 4;
  }

  entry.cycle = cycle + kBaseTic;
  entry.event = (filenum << 13) | (procnum << 1) | is_exit;
  fwrite(&entry, sizeof(entry), 1, ftrace_);
  num_entries_ += 1;
}

void Gtrace::AddProcEntry(int filenum, int procnum, uint32_t cycle, uint32_t pid)
{
  AddGtraceRecord(filenum, procnum, cycle, pid, 0);
}

void Gtrace::AddProcExit(int filenum, int procnum, uint32_t cycle, uint32_t pid)
{
  AddGtraceRecord(filenum, procnum, cycle, pid, 1);
}

void Gtrace::AddProcedure(int filenum, int procnum, const char *proc_name)
{
  fprintf(fnames_, "%d %d %d %s\n", filenum, procnum, procnum, proc_name);
}
