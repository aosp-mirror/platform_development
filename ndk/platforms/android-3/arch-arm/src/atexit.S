/*
 * Copyright (C) 2011 The Android Open Source Project
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

/* CRT_LEGACY_WORKAROUND should only be defined when building
 * this file as part of the platform's C library.
 *
 * The C library already defines a function named 'atexit()'
 * for backwards compatibility with older NDK-generated binaries.
 *
 * For newer ones, 'atexit' is actually embedded in the C
 * runtime objects that are linked into the final ELF
 * binary (shared library or executable), and will call
 * __cxa_atexit() in order to un-register any atexit()
 * handler when a library is unloaded.
 *
 * This function must be global *and* hidden. Only the
 * code inside the same ELF binary should be able to access it.
 */
#ifndef CRT_LEGACY_WORKAROUND
	.arch armv5te
	.fpu softvfp
	.eabi_attribute 20, 1
	.eabi_attribute 21, 1
	.eabi_attribute 23, 3
	.eabi_attribute 24, 1
	.eabi_attribute 25, 1
	.eabi_attribute 26, 2
	.eabi_attribute 30, 4
	.eabi_attribute 18, 4
	.hidden	atexit
	.code	16
	.thumb_func
        .text
        .align 0
        .global atexit
        .type atexit, #function
atexit: .fnstart
.LFB0:
	.save	{r4, lr}
	push	{r4, lr}
.LCFI0:
	ldr	r3, .L3
	mov	r1, #0
	@ sp needed for prologue
.LPIC0:
	add	r3, pc
	ldr	r2, [r3]
        ldr     r3, =__cxa_atexit
	blx     r3
	pop	{r4, pc}
.L4:
	.align	2
.L3:
	.word	__dso_handle-(.LPIC0+4)
.LFE0:
        .fnend
        .size atexit, . - atexit
#endif
