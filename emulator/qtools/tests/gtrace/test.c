#include "../macros.h"

int foo1();
int foo2();
void bar();
int child1();
int child2();
int child3();
int child4();
int child5();

int global;

void start()
{
  // Set the stack pointer
  asm("  mov r13,#0x200000");
  PRINT_STR("hello\n");
  TRACE_INIT_NAME(701, "proc_foo");
  TRACE_INIT_NAME(702, "proc_bar");
  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();
  TRACE_SWITCH(702);
  if (global++ > 0)
      global++;
  bar();
  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo2();
  TRACE_SWITCH(703);
  if (global++ > 0)
      global++;
  foo1();
  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(704);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(705);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(706);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(707);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(708);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(709);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(701);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_SWITCH(710);
  if (global++ > 0)
      global++;
  foo1();

  TRACE_STOP_EMU();
}

int foo1()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 3; ++ii) {
    a += child1();
    a += child2();
    a += child3();
  }
  return a;
}

int foo2()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 2; ++ii) {
    a += child3();
    a += child4();
    a += child5();
  }
  return a;
}

#define kStride 64
void bar()
{
  int a = 0;

  static char mem[1000 * kStride];

  int ii, jj;

  for (ii = 0; ii < 4; ++ii) {
    for (jj = 0; jj < 10; ++jj)
      a += mem[jj * kStride];
    foo1();
    foo2();
  }
}

int child1()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 2; ++ii)
    a += ii;
  return a;
}

int child2()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 4; ++ii)
    a += ii;
  return a;
}

int child3()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 6; ++ii)
    a += ii;
  return a;
}

int child4()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 8; ++ii)
    a += ii;
  return a;
}

int child5()
{
  int a = 0;

  int ii;
  for (ii = 0; ii < 10; ++ii)
    a += ii;
  return a;
}
