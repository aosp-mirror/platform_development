/*
 * Copyright (C) 2013 The Android Open Source Project
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


__LIBC_HIDDEN__  void do_mips_start(void *raw_args) {
  structors_array_t array;
  array.preinit_array = &__PREINIT_ARRAY__;
  array.init_array = &__INIT_ARRAY__;
  array.fini_array = &__FINI_ARRAY__;

  __libc_init(raw_args, NULL, &main, &array);
}

/*
 * This function prepares the return address with a branch-and-link
 * instruction (bal) and then uses a .cpsetup to compute the Global
 * Offset Table (GOT) pointer ($gp). The $gp is then used to load
 * the address of _do_mips_start() into $t9 just before calling it.
 * Terminating the stack with a NULL return address.
 */
__asm__ (
"       .set push                   \n"
"                                   \n"
"       .text                       \n"
"       .align  4                   \n"
"       .type __start,@function     \n"
"       .globl __start              \n"
"       .globl  _start              \n"
"                                   \n"
"       .ent    __start             \n"
"__start:                           \n"
" _start:                           \n"
"       .frame   $sp,32,$0          \n"
"       .mask   0x80000000,-8       \n"
"                                   \n"
"       move    $a0, $sp            \n"
"       daddiu  $sp, $sp, -32       \n"
"                                   \n"
"       .set noreorder              \n"
"       bal     1f                  \n"
"       nop                         \n"
"1:                                 \n"
"       .cpsetup $ra,16,1b          \n"
"       .set reorder                \n"
"                                   \n"
"       sd      $0, 24($sp)         \n"
"       jal     do_mips_start       \n"
"                                   \n"
"2:     b       2b                  \n"
"       .end    __start             \n"
"                                   \n"
"       .set pop                    \n"
);

#include "../../arch-common/bionic/__dso_handle.h"
#include "atexit.h"
