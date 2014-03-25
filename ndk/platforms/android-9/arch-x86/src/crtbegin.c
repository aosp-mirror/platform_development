/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include "../../bionic/libc_init_common.h"
#include <stddef.h>
#include <stdint.h>

__attribute__ ((section (".preinit_array")))
void (*__PREINIT_ARRAY__)(void) = (void (*)(void)) -1;

__attribute__ ((section (".init_array")))
void (*__INIT_ARRAY__)(void) = (void (*)(void)) -1;

__attribute__ ((section (".fini_array")))
void (*__FINI_ARRAY__)(void) = (void (*)(void)) -1;

__LIBC_HIDDEN__
#ifdef __i386__
__attribute__((force_align_arg_pointer))
#endif
void _start() {
  structors_array_t array;
  array.preinit_array = &__PREINIT_ARRAY__;
  array.init_array = &__INIT_ARRAY__;
  array.fini_array = &__FINI_ARRAY__;

  void* raw_args = (void*) ((uintptr_t) __builtin_frame_address(0) + sizeof(void*));
#ifdef __x86_64__
  // 16-byte stack alignment is required by x86_64 ABI
  asm("andq  $~15, %rsp");
#endif
  __libc_init(raw_args, NULL, &main, &array);
}

#include "__dso_handle.h"
#include "atexit.h"
#ifdef __i386__
# include "../../arch-x86/bionic/__stack_chk_fail_local.h"
#endif
