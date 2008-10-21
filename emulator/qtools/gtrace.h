// Copyright 2006 The Android Open Source Project

#ifndef GTRACE_H
#define GTRACE_H

class Gtrace {
 public:
  static const int kGtraceEntriesPerBlock = 1024;
  static const uint32_t kMillion = 1000000;
  static const uint32_t kTicsPerSecond = 200 * kMillion;
  static const int kBaseTic = 0x1000;

  struct trace_entry {
    uint32_t	cycle;
    uint32_t	event;
  };

  struct block_header {
    uint32_t	blockno;
    uint32_t	entry_width;
    uint32_t	block_tic;
    uint32_t	block_time;
    uint32_t	usec_cpu;
    uint32_t	pid;
    uint32_t	bug_count;
    uint32_t	zero_count;
  };

  struct first_header {
    block_header	common;
    uint32_t		tic;
    uint32_t		one;
    uint32_t		tics_per_second;
    uint32_t		trace_time;
    uint32_t		version;
    uint32_t		file_proc;
    uint32_t		pdate;
    uint32_t		ptime;
  };

  Gtrace();
  ~Gtrace();

  void		Open(const char *gtrace_file, uint32_t pdate, uint32_t ptime);
  void		WriteFirstHeader(uint32_t start_sec, uint32_t pid);
  void		AddProcedure(int filenum, int procnum, const char *proc_name);
  void		AddProcEntry(int filenum, int procnum, uint32_t cycle, uint32_t pid);
  void		AddProcExit(int filenum, int procnum, uint32_t cycle, uint32_t pid);

 private:
  void		AddGtraceRecord(int filenum, int procnum, uint32_t cycle, uint32_t pid,
                                int is_exit);
  void		FillFirstHeader(uint32_t start_sec, uint32_t pid,
                                first_header *fh);
  void		WriteBlockHeader(uint32_t cycle, uint32_t pid);

  const char	*gtrace_file_;
  char		gname_file_[100];
  FILE		*ftrace_;
  FILE		*fnames_;
  uint32_t	start_sec_;
  uint32_t	pdate_;
  uint32_t	ptime_;
  int		num_entries_;
  int		blockno_;
  uint32_t	current_pid_;
};

#endif  // GTRACE_H
