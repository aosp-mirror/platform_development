#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string.h>
#include "trace_reader.h"
#include "parse_options.h"

typedef TraceReader<> TraceReaderType;

#include "parse_options-inl.h"

// This function is called from quicksort to compare the cpu time
// of processes and sort into decreasing order.
int cmp_dec_cpu_time(const void *a, const void *b) {
  ProcessState *proc1, *proc2;

  proc1 = (ProcessState*)a;
  proc2 = (ProcessState*)b;
  if (proc1 == NULL) {
    if (proc2 == NULL)
      return 0;
    return 1;
  }
  if (proc2 == NULL)
    return -1;
  if (proc1->cpu_time < proc2->cpu_time)
    return 1;
  if (proc1->cpu_time > proc2->cpu_time)
    return -1;
  // If the cpu_time times are the same, then sort into increasing
  // order of pid.
  return proc1->pid - proc2->pid;
}

void Usage(const char *program)
{
  fprintf(stderr, "Usage: %s [options] trace_file\n", program);
  OptionsUsage();
}

int main(int argc, char **argv) {
  // Parse the options
  ParseOptions(argc, argv);
  if (argc - optind != 1) {
    Usage(argv[0]);
    exit(1);
  }

  char *trace_filename = argv[optind];
  TraceReader<> *trace = new TraceReader<>;
  trace->Open(trace_filename);
  trace->SetRoot(root);

  while (1) {
    BBEvent event, ignored;
    symbol_type *dummy_sym;

    if (GetNextValidEvent(trace, &event, &ignored, &dummy_sym))
      break;
  }

  int num_procs;
  ProcessState *processes = trace->GetProcesses(&num_procs);
  qsort(processes, num_procs, sizeof(ProcessState), cmp_dec_cpu_time);

  uint64_t total_time = 0;
  for (int ii = 0; ii < num_procs; ++ii) {
    total_time += processes[ii].cpu_time;
  }

  uint64_t sum_time = 0;
  printf("  pid parent   cpu_time      %%      %% flags argv\n"); 
  ProcessState *pstate = &processes[0];
  for (int ii = 0; ii < num_procs; ++ii, ++pstate) {
    sum_time += pstate->cpu_time;
    double per = 100.0 * pstate->cpu_time / total_time;
    double sum_per = 100.0 * sum_time / total_time;
    const char *print_flags = "";
    if ((pstate->flags & ProcessState::kCalledExec) == 0)
      print_flags = "T";
    if (pstate->name == NULL)
      pstate->name = "";
    printf("%5d  %5d %10llu %6.2f %6.2f %5s %s",
           pstate->pid, pstate->parent_pid, pstate->cpu_time,
           per, sum_per, print_flags, pstate->name);
    for (int jj = 1; jj < pstate->argc; ++jj) {
      printf(" %s", pstate->argv[jj]);
    }
    printf("\n");
  }
  delete trace;
  return 0;
}
