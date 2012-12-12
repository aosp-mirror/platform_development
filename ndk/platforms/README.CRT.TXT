This directory contains the sources of the C runtime object files
required by the Android NDK toolchains. This document explains
what they are, as well as a few important details about them.

The files are located under the following directories:

  android-3/arch-arm/src/
  android-9/arch-x86/src/
  android-9/arch-mips/src/

They are either C files, or assembly files with an .S extension, which means
that they'll be sent to the C-preprocessor before being assembled into
object files. They have the following names and usage:

  crtbegin_static.[cS]
    This file contains a tiny ELF startup entry point (named '_start')
    that is linked into every Android _static_ executable. These binaries can
    run on any Linux system, but cannot perform dynamic linking at all.

    Note that the kernel calls the '_start' entry point directly when it
    launches such an executable. The _start stub is used to call the
    C library's runtime initialization, passing it the address of the
    'main' function.

  crtbegin_dynamic.[cS]
    This is equivalent to crtbegin_static.[cS] but for _dynamic_ executables.
    These executables always link to the system C library dynamically.

    When the kernel launches such an executable, it actually starts the
    dynamic linker (/system/bin/linker), which loads and relocates the
    executable (possibly loading any dependent system libraries as well),
    then call the _start stub.

  crtbegin_so.[cS]
    This is equivalent to crtbegin_dynamic.[cS], but shall be used for
    shared libraries. One major difference is that there is no _start
    entry point.

  crtend_android.S
    This source file shall be used when generating an executable, i.e. used
    in association with either crtbegin_static.[cS] or crtbegin_dynamic.[cS]

  crtend.S
    This source file is _strictly_ equivalent to crtend_android.S.
    Actually, it *must* be compiled into an object named 'crtend_android.o'
    because that's the hard-coded name that the toolchain binaries expect.

    (the naming difference for this source file is purely historical, it
    could probably be removed in the future).

  crtend_so.S
    This source's object file shall be used when generating a shared library,
    i.e. used in association with crtbegin_so.[cS] only.

Content of these files:

ELF section (lists);

  crtbegin_static.[cS] and crtbegin_dynamic.[cS] contain a '_start' entry point
  for the corresponding executable. crtbegin_so.[cS] doesn't need any.

  all crtbegin_XXX.[cS] files contain the head of various ELF sections, which are
  used to list of ELF constructors and destructors. The sections are:

    .init_array:
        Contains a list of function addresses that are run at load time.
        This means they are run *before* 'main', in the case of executables,
        or during 'dlopen()' for shared libraries (either implicit or explicit).

        The functions are called in list order (from first to last).

    .fini_array:
        Contains a list of destructor addresses that are run at unload time.
        This means they are run *after* 'exit', in the case of executables,
        or during 'dlclose()' for shared libraries (either implicit or explicit).

        The functions are called in _reverse_ list order (from last to first).

    .preinit_array:
        This section can *only* appear in executables. It contains a list of
        constructors that are run _before_ the ones in .init_array, or those
        of any dependent shared library (if any).

    .ctors
        This section shall *not* be used on Android. Used on some GLibc-based
        Linux systems to hold list of constructors. The toolchains should
        place all constructors in .init_array instead.

    .dtors
        This section shall *not* be used on Android. Used on some GLibc-based
        Linux systems to hold a list of destructors. The toolchains should
        place all destructors in .fini_array instead.


__dso_handle symbol:

  To properly support the C++ ABI, a unique *local* *hidden* symbol named
  '__dso_handle' must be defined in each shared library.

  This is used to implement static C++ object initialization in a shared
  library, as in:

      static Foo  foo(10);

  The statement above creates a hidden function, which address will be added
  to the .init_array section described above. Its compiler-generated code
  will perform the object construction, and also register static destructor
  using a call that looks like:

      __cxa_atexit( Foo::~Foo, &foo, &__dso_handle );

  Where '__cxa_atexit' is a special C++ support function provided by the
  C library. Doing this ensures that the destructor for 'foo' will be
  automatically called when the shared library containing this code is
  unloaded (i.e. either through 'dlclose' or at program exit).

  The value of __dso_handle is normally never taken directly.

  See http://sourcery.mentor.com/public/cxx-abi/abi.html#dso-dtor

  WARNING: There is a big caveat regarding this symbol. Read the section
           named 'IMPORTANT BACKWARDS COMPATIBILITY ISSUES' below.


atexit() implementation:

  The Posix standard doesn't mandate the program behaviour's when a shared
  library which registered a function with 'atexit' is unloaded explicitely
  (e.g. with 'dlclose()').

  On most BSD systems (including OS X), unloading the library succeeds, but
  the program will crash when it calls exit() or returns from main().

  On Linux, GLibc provides an implementation that automatically unregisters
  such atexit() handlers when the corresponding shared library is unloaded.

  However, this requires that the atexit() implementation be part of the
  shared library itself, rather than the C library.

  The crtbegin_dynamic.[cS] and crtbegin_so.[cS] files contain an tiny
  implementation of atexit() in assembler that essentially does:

      void atexit(void(*myfunc)(void))
      {
         __cxa_atexit(myfunc, NULL, &__dso_handle);
      }

  Because it references the shared library's hidden __dso_handle symbol,
  this code cannot be in the C library itself.

  Note that crtbegin_static.[cS] should *not* provide an atexit() function
  (the latter should be provided by libc.a instead).

  See 'BACKWARDS COMPATIBILITY ISSUES' section below.



BACKWARDS COMPATIBILITY ISSUES:
-------------------------------

To maintain binary compatibility to all existing NDK-generated machine code,
the system's C library (i.e. /system/lib/libc.so) needs to exports symbols
that shall *not* be exported by the NDK-provided link-time libraries (i.e.
$NDK/platforms/android-$LEVEL/arch-$ARCH/usr/lib/libc.so).

Starting from NDK r7, the NDK libc.so is itself generated by a script
(gen-platforms.sh) from a list of symbol files (see libc.so.functions.txt
and libc.so.variables.txt) and does not contain any implementation code.

The NDK libc.a, on the other hand, is a copy of a given version of the system
C static library, and shall only be used to generate static executables (it
is also required to build gdbserver).

1. libgcc compatibility symbols:

  None of the link-time NDK shared libraries should export any libgcc symbol.

  However, on ARM, the system C library needs to export some of them to
  maintain binary compatibility with 'legacy' NDK machine code. Details are
  under bionic/libc/arch-arm/bionic/libgcc_compat.c.

  Note that gen-platforms.sh takes care of this by explicitely removing any
  libgcc symbol from the link-time shared libraries it generates. This is done
  by using the lists under:

     $NDK/build/tools/unwanted-symbols/$ARCH/libgcc.a.functions.txt

  You will need to update these files when the toolchain changes.

  Note that all libgcc releases should be backwards-compatible, i.e. newer
  releases always contain all the symbols from previous ones).


2. __dso_handle compatibility symbol:

  Earlier versions of the C library exported a __dso_handle symbol
  *incorrectly*. As such:

   - the system's libc.so shall always export its __dso_handle, as *global*
     and *public* (in ELF visibility terms). A weak symbol definition is ok
     but not necessary. This is only to ensure binary compatibility with
    'legacy' NDK machine code.

   - the NDK link-time libc.so shall *never* export or contain any
     __dso_handle symbol.

   - The NDK's crtbegin_dynamic.[cS] and crtbegin_so.[cS] shall provide a *local*
     and *hidden* __dso_handle symbol.

   - The NDK's libc.a will containg a *global* and *public* __dso_handle, since
     it is a copy of a release-specific system libc.so.

   - crtbegin_static.[cS] shall not provide any __dso_handle symbol, since static
     executables will use the one in libc.a instead.

Note that existing NDK machine code that links against the system libc's
__dso_handle will not have their C++ destructors run correctly when the
library is unloaded. However, this bug can be solved by simply recompiling
/relinking against a newer NDK release, without touching the original
sources.



3. atexit compatibility symbol:

  Earlier versions of the C library implemented and exported an atexit()
  function. While this is compliant with Posix, this doesn't allow a useful
  GLibc extension which automatically un-registers atexit() handlers when
  a shared library is unloaded with dlclose().

  To support this, while providing binary compatibility, the following
  must apply:

  - The platform's /system/lib/libc.so should *always* export a working
    atexit() implementation (used by 'legacy' NDK machine code).

  - The NDK link-time libc.so should *never* export atexit()

  - crtbegin_dynamic.[cS] and crtbegin_so.[cS] shall define a *local* *hidden*
    symbol for atexit(), with a tiny implementation that amounts to the
    following code:

         void atexit( void(*handler)(void) )
         {
            __cxa_atexit( handler, NULL, &__dso_handle );
         }

  - The NDK libc.a shall provide an atexit() implementation, and
    crtbegin_static.[cS] shall *not* provide one to avoid conflicts.

Note that existing NDK machine code that links against the system libc's
atexit symbol will not have their atexit-handler automatically unregistered
when the library is unloaded. However, this bug can be solved by simply
recompiling/relinking against a newer NDK release, without touching the
original sources.

4. __atomic_xxx sompatibility symbols:

This issues is detailed in ndk/docs/ANDROID-ATOMICS.html and
bionic/libc/arch-arm/bionic/atomics_arm.c. In a nutshell:

   - The system C library *shall* always export on *ARM* the __atomic_cmpxchg,
     __atomic_inc and __atomic_dec functions to support legacy NDK machine code.
     Their implementation should have full (i.e. acquire+release) memory ordering
     semantics.

   - The system C library for other CPU architectures (e.g. x86 or mips) *shall*
     *not* export any of these symbols.

   - The NDK libc.so *shall* *not* export these symbols at all.

   - The NDK <sys/atomics.h> header shall provide inlined-static versions of
     these functions that use the built-in GCC atomic functions instead.
    
