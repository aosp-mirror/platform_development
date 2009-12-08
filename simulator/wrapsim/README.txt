This shared library is used with LD_PRELOAD to wrap the Android runtime
when used with the desktop simulator.

Because LD_PRELOAD is part of the environment when gdb and valgrind are
invoked, the wrapper can "see" activity from both of these (more so gdb
than valgrind).  For this reason it needs to be very careful about which
"open" calls are intercepted.

It's also important that none of the intercepted system or library calls
are invoked directly, or infinite recursion could result.

Avoid creating dependencies on other libraries.


To debug wrapsim, set WRAPSIM_LOG to a log file before launching, e.g.

% WRAPSIM_LOG=/tmp/wraplog.txt simulator

For more verbose logging, you can enable the verbose forms of CALLTRACE
and CALLTRACEV in Intercept.c.

To build, you will need to have the 32-bit libaudio2 development package
installed. On Ubuntu Hardy, do something like:
% sudo apt-get install lib32asound2-dev
