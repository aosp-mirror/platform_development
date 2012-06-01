/*
 * Copyright (C) 2008 The Android Open Source Project
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

.section .init_array, "aw"
.align 4
.type __INIT_ARRAY__, @object
.globl __INIT_ARRAY__
__INIT_ARRAY__:
    .long -1
    .long frame_dummy

.section .fini_array, "aw"
.align 4
.type __FINI_ARRAY__, @object
.globl __FINI_ARRAY__
__FINI_ARRAY__:
    .long -1
    .long __do_global_dtors_aux

	.section	.eh_frame,"a",@progbits
	.align 4
	.type	__EH_FRAME_BEGIN__, @object
__EH_FRAME_BEGIN__:
	.text
	.p2align 4,,15
	.type	__do_global_dtors_aux, @function
__do_global_dtors_aux:
	pushl	%ebp
	movl	%esp, %ebp
	pushl	%ebx
	call	__x86.get_pc_thunk.bx
	addl	$_GLOBAL_OFFSET_TABLE_, %ebx
	subl	$20, %esp
	cmpb	$0, completed.4454@GOTOFF(%ebx)
	jne	.L5
	movl	__dso_handle@GOTOFF(%ebx), %eax
	movl	%eax, (%esp)
	call	__cxa_finalize@PLT
	movl	__deregister_frame_info_bases@GOT(%ebx), %eax
	testl	%eax, %eax
	je	.L4
	leal	__EH_FRAME_BEGIN__@GOTOFF(%ebx), %eax
	movl	%eax, (%esp)
	call	__deregister_frame_info_bases@PLT
.L4:
	movb	$1, completed.4454@GOTOFF(%ebx)
.L5:
	addl	$20, %esp
	popl	%ebx
	popl	%ebp
	ret
	.text
	.p2align 4,,15
	.type	frame_dummy, @function
frame_dummy:
	pushl	%ebp
	movl	%esp, %ebp
	pushl	%ebx
	call	__x86.get_pc_thunk.bx
	addl	$_GLOBAL_OFFSET_TABLE_, %ebx
	subl	$20, %esp
	movl	__register_frame_info_bases@GOT(%ebx), %eax
	testl	%eax, %eax
	je	.L8
	leal	object.4469@GOTOFF(%ebx), %eax
	movl	%eax, 4(%esp)
	leal	__EH_FRAME_BEGIN__@GOTOFF(%ebx), %eax
	movl	%ebx, 12(%esp)
	movl	$0, 8(%esp)
	movl	%eax, (%esp)
	call	__register_frame_info_bases@PLT
.L8:
	addl	$20, %esp
	popl	%ebx
	popl	%ebp
	ret
	.local	completed.4454
	.comm	completed.4454,1,1
	.local	object.4469
	.comm	object.4469,24,4
	.weak	__register_frame_info_bases
	.weak	__deregister_frame_info_bases

#include "__dso_handle_so.S"
#include "atexit.S"
#include "__stack_chk_fail_local.S"
