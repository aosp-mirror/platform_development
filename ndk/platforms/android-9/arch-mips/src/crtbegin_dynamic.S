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
	.text
	.align 4
	.type __start,@function
	.globl __start
	.globl _start

# this is the small startup code that is first run when
# any executable that is dynamically-linked with Bionic
# runs.
#
# it's purpose is to call __libc_init with appropriate
# arguments, which are:
#
#    - the address of the raw data block setup by the Linux
#      kernel ELF loader
#
#    - address of an "onexit" function, not used on any
#      platform supported by Bionic
#
#    - address of the "main" function of the program.
#
#    - address of the constructor list
#

	.ent	__start
__start:	
_start:
	bal	1f
1:
	.set	noreorder
	.cpload	$ra
	.set	reorder

	move	$a0, $sp
	move	$a1, $0
	la	$a2, main
	la	$a3, 1f
	subu	$sp, 32
	la	$t9, __libc_init
	j	$t9
	.end	__start

1:	.long	__PREINIT_ARRAY__
	.long	__INIT_ARRAY__
	.long	__FINI_ARRAY__
	.long	__CTOR_LIST__
	.long	__DTOR_LIST__

	.section .preinit_array, "aw"
	.type __PREINIT_ARRAY__, @object
	.globl __PREINIT_ARRAY__
__PREINIT_ARRAY__:
	.long -1

	.section .init_array, "aw"
	.type __INIT_ARRAY__, @object
	.globl __INIT_ARRAY__
__INIT_ARRAY__:
	.long -1

	.section .fini_array, "aw"
	.type __FINI_ARRAY__, @object
	.globl __FINI_ARRAY__
__FINI_ARRAY__:
	.long -1
	.long __do_global_dtors_aux

	.section .ctors, "aw"
	.type __CTOR_LIST__, @object
	.globl __CTOR_LIST__
__CTOR_LIST__:
	.long -1

	.section .dtors, "aw"
	.type __DTOR_LIST__, @object
	.globl __DTOR_LIST__
__DTOR_LIST__:
	.long -1

	.abicalls
	.section	.eh_frame,"a",@progbits
	.align	2
	.type	__EH_FRAME_BEGIN__, @object
	.size	__EH_FRAME_BEGIN__, 0
__EH_FRAME_BEGIN__:
	.text
	.align	2
	.set	nomips16
	.ent	__do_global_dtors_aux
	.type	__do_global_dtors_aux, @function
__do_global_dtors_aux:
	.frame	$sp,32,$31		# vars= 0, regs= 1/0, args= 16, gp= 8
	.mask	0x80000000,-4
	.fmask	0x00000000,0
	.set	noreorder
	.cpload	$25
	.set	nomacro
	addiu	$sp,$sp,-32
	sw	$31,28($sp)
	.cprestore	16
	lw	$2,%got(completed.1269)($28)
	lbu	$2,%lo(completed.1269)($2)
	bne	$2,$0,$L8
	nop

$L4:
	lw	$2,%got(__cxa_finalize)($28)
	beq	$2,$0,$L6
	nop

	lw	$2,%got(__dso_handle)($28)
	lw	$4,0($2)
	lw	$25,%call16(__cxa_finalize)($28)
	.reloc	1f,R_MIPS_JALR,__cxa_finalize
1:	jalr	$25
	nop

	lw	$28,16($sp)
$L6:
	lw	$2,%got(completed.1269)($28)
	li	$3,1			# 0x1
	sb	$3,%lo(completed.1269)($2)
$L8:
	lw	$31,28($sp)
	addiu	$sp,$sp,32
	j	$31
	nop

	.set	macro
	.set	reorder
	.end	__do_global_dtors_aux
	.size	__do_global_dtors_aux, .-__do_global_dtors_aux
	.local	completed.1269
	.comm	completed.1269,1,1
	.local	object.1265
	.comm	object.1265,24,4
	.weak	__cxa_finalize

.include "__dso_handle.S"
.include "atexit.S"
	
