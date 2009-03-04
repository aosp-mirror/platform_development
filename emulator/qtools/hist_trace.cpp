#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "trace_reader.h"

static const int kMaxHistEntries = 256;
static const int kMaxHistEntries2 = kMaxHistEntries /  2;
int hist[kMaxHistEntries];
int underflow, overflow;

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "Usage: %s trace_file\n", argv[0]);
    exit(1);
  }

  char *trace_filename = argv[1];
  TraceReaderBase *trace = new TraceReaderBase;
  trace->Open(trace_filename);

  uint64_t prev_bb_num = 0;
  uint64_t prev_time = 0;
  int total = 0;
  
  while (1) {
    BBEvent event;

    if (trace->ReadBB(&event))
      break;
    int bb_diff = event.bb_num - prev_bb_num;
    //int time_diff = event.time - prev_time;
    //printf("bb_num: %llu prev: %llu, diff: %d\n",
    // event.bb_num, prev_bb_num, bb_diff);
    prev_bb_num = event.bb_num;
    prev_time = event.time;

    bb_diff += kMaxHistEntries2;
    if (bb_diff < 0)
      underflow += 1;
    else if (bb_diff >= kMaxHistEntries)
      overflow += 1;
    else
      hist[bb_diff] += 1;
    total += 1;
  }

  int sum = 0;
  double sum_per = 0;
  double per = 0;
  for (int ii = 0; ii < kMaxHistEntries; ++ii) {
    if (hist[ii] == 0)
      continue;
    per = 100.0 * hist[ii] / total;
    sum += hist[ii];
    sum_per = 100.0 * sum / total;
    printf(" %4d: %6d %6.2f %6.2f\n", ii - kMaxHistEntries2, hist[ii], per, sum_per);
  }
  per = 100.0 * underflow / total;
  printf("under: %6d %6.2f\n", underflow, per);
  per = 100.0 * overflow / total;
  printf("over:  %6d %6.2f\n", overflow, per);
  printf("total: %6d\n", total);
  return 0;
}
