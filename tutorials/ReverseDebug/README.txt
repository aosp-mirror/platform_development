This is a tutorial/unittest for gdb's reverse debugging feature. It is a new
feature that allows users to take a snapshot of the machine state, continue
until a later stage of the program, then return to the previously recorded
state and execute again. An ideal usage case is to help track down the reason
why a memory location is clobbered. 

In the sample below, the "clobber" function trashes a neighboring variable "p"
on the stack next to the "values" variable, and the program will crash at
line 42 when "p" is being dereferenced.

 18 #include <stdio.h>
 19 #include <stdlib.h>
 20 
 21 #define ARRAY_LENGTH 10
 22 
 23 int flag;
 24 
 25 void clobber(int *array, int size) {
 26     /* Make sure it clobbers something. */
 27     array[-1] = 0x123;
 28     array[size] = 0x123;
 29 }
 30 
 31 int main(void) {
 32     int values[ARRAY_LENGTH];
 33     int *p = (int *) malloc(sizeof(int));
 34     *p = 10;
 35 
 36     while (!flag) {
 37         sleep(1);
 38     }
 39 
 40     /* Set a breakpint here: "b main.c:41" */
 41     clobber(values, ARRAY_LENGTH);
 42     printf("*p = %d\n", *p);
 43     free(p);
 44 
 45     return 0;
 46 }

The test program can be built/installed on the device by doing:

> mmm development/tutorials/ReverseDebug
> adb sync
> adb shell reverse_debug

In another window the following command can be used to attach to the running
program:

> gdbclient reverse_debug :5039 reverse_debug
[1] 12802
Attached; pid = 1842
Listening on port 5039
GNU gdb (GDB) 7.6
Copyright (C) 2013 Free Software Foundation, Inc.
License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.  Type "show copying"
and "show warranty" for details.
This GDB was configured as "--host=x86_64-linux-gnu --target=arm-linux-android".
For bug reporting instructions, please see:
<http://source.android.com/source/report-bugs.html>...
Reading symbols from /usr/local/google/work/master/out/target/product/manta/symbols/system/bin/reverse_debug...done.
Remote debugging from host 127.0.0.1
nanosleep () at bionic/libc/arch-arm/syscalls/nanosleep.S:10
10      mov     r7, ip

====

Now set a breakpoint on line 41 and set flag to 1 so that the program can
 continue. 

(gdb) b main.c:41
Breakpoint 1 at 0xb6f174a8: file development/tutorials/ReverseDebug/main.c, line 41.
(gdb) p flag=1
$1 = 1
(gdb) c
Continuing.

====

Now try the new "record" command to take a snapshot of the machine state.

Breakpoint 1, main () at development/tutorials/ReverseDebug/main.c:41
41      clobber(values, ARRAY_LENGTH);
(gdb) record
(gdb) c
Continuing.

====

Now the program crashes as expected as "p" has been clobbered. The
"reverse-continue" command will bring the program back to line 41 and let you
replay each instruction from there.

Program received signal SIGSEGV, Segmentation fault.
0xb6f174bc in main () at development/tutorials/ReverseDebug/main.c:42
42      printf("*p = %d\n", *p);
(gdb) reverse-continue
Continuing.

No more reverse-execution history.
main () at development/tutorials/ReverseDebug/main.c:41
41      clobber(values, ARRAY_LENGTH);


====

Now let's add a watch point at "&p" to hopefully catch the clobber on the spot:
 
(gdb) watch *(&p)
Hardware watchpoint 2: *(&p)
(gdb) c
Continuing.
Hardware watchpoint 2: *(&p)

====

And here it is:

Old value = (int *) 0xb728c020
New value = (int *) 0x123
0xb6f17440 in clobber (array=0xbebcaab0, size=10)
    at development/tutorials/ReverseDebug/main.c:28
28      array[size] = 0x123;


===============================

That said, reverse debugging on ARM is still in the infant stage. Currently
(as of gdb 7.6) it only recognizes ARM instructions and will punt on all
Thumb(2) instructions. To give it a try you will need to recompile your
program in ARM mode. To do that you have to add the ".arm" suffix to the
desired file in Android.mk:

LOCAL_SRC_FILES:= main.c.arm

