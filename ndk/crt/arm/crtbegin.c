/*
 * Copyright (C) 2016 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

typedef struct
{
    void (**preinit_array)(void);
    void (**init_array)(void);
    void (**fini_array)(void);
    void (**ctor_list)(void);
} structors_array_t;

extern int main(int argc, char **argv, char **env);

extern void __libc_init(
  unsigned int *elfdata,
  void (*onexit)(void),
  int (*slingshot)(int, char**, char**),
  structors_array_t const * const structors
);

__attribute__ ((section (".preinit_array")))
void (*__PREINIT_ARRAY__)(void) = (void (*)(void)) -1;

__attribute__ ((section (".init_array")))
void (*__INIT_ARRAY__)(void) = (void (*)(void)) -1;

__attribute__ ((section (".fini_array")))
void (*__FINI_ARRAY__)(void) = (void (*)(void)) -1;

__attribute__ ((section (".ctors")))
void (*__CTOR_LIST__)(void) = (void (*)(void)) -1;

/*
 * This is the small startup code that is first run when any executable runs.
 *
 * Its purpose is to call __libc_init with appropriate arguments, which are:
 *
 * - The address of the raw data block setup by the Linux kernel ELF loader.
 *
 * - The address of an "onexit" function, not used on any platform supported by
 *   Bionic.
 *
 * - The address of the "main" function of the program.
 *
 * - The address of the constructor list.
 */
__attribute__((visibility("hidden")))
void _start() {
  structors_array_t array;
  void *elfdata;

  array.preinit_array = &__PREINIT_ARRAY__;
  array.init_array =    &__INIT_ARRAY__;
  array.fini_array =    &__FINI_ARRAY__;
  array.ctor_list =    &__CTOR_LIST__;

  elfdata = __builtin_frame_address(0) + sizeof(void *);
  __libc_init(elfdata, (void *) 0, &main, &array);
}

#include "__dso_handle.h"
// Old releases included atexit() details in libc.a, not in crtbegin_static.o.
// It was, however, in crtbegin_dynamic.o rather than libc.so.
#if PLATFORM_SDK_VERSION >= 21 || defined(BUILDING_DYNAMIC)
#include "atexit.h"
#endif
