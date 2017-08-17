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

#include "asm_multiarch.h"

	.section .preinit_array, "aw"
	ASM_ALIGN_TO_PTR_SIZE
	ASM_PTR_SIZE(0)

	.section .init_array, "aw"
	ASM_ALIGN_TO_PTR_SIZE
	ASM_PTR_SIZE(0)

	.section .fini_array, "aw"
	ASM_ALIGN_TO_PTR_SIZE
	ASM_PTR_SIZE(0)

	.section .ctors, "aw"
	.type __CTOR_END__, @object
__CTOR_END__:
	ASM_PTR_SIZE(0)

	.section .dtors, "aw"
	.type __DTOR_END__, @object
__DTOR_END__:
	ASM_PTR_SIZE(0)

	.section	.eh_frame,"a",@progbits
	.align 4
	.type	__FRAME_END__, @object
	.size	__FRAME_END__, 4
__FRAME_END__:
	.zero	4
