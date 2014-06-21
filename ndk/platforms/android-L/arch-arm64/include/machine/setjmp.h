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

/*
 * machine/setjmp.h: machine dependent setjmp-related information.
 */

/* _JBLEN is the size of a jmp_buf in longs(64bit on AArch64) */
#define _JBLEN 32

/* According to AARCH64 PCS document we need to save the following
 * registers:
 *
 * Core     x19 - x30, sp (see section 5.1.1)
 * VFP      d8 - d15 (see section 5.1.2)
 *
 * NOTE: All the registers saved here will have 64bit vales (except FPSR).
 *       AAPCS mandates that the higher part of q registers does not need to
 *       be saveved by the callee.
 */

/* The structure of jmp_buf for AArch64:
 *
 * NOTE: _JBLEN is the size of jmp_buf in longs(64bit on AArch64)! The table
 *      below computes the offsets in words(32bit).
 *
 *  word        name            description
 *  0       magic           magic number
 *  1       sigmask         signal mask (not used with _setjmp / _longjmp)
 *  2       core_base       base of core registers (x19-x30, sp)
 *  28      float_base      base of float registers (d8-d15)
 *  44      reserved        reserved entries (room to grow)
 *  64
 *
 *
 *  NOTE: The instructions that load/store core/vfp registers expect 8-byte
 *        alignment. Contrary to the previous setjmp header for ARM we do not
 *        need to save status/control registers for VFP (it is not a
 *        requirement for setjmp).
 */

#define _JB_MAGIC       0
#define _JB_SIGMASK     (_JB_MAGIC+1)
#define _JB_CORE_BASE   (_JB_SIGMASK+1)
#define _JB_FLOAT_BASE  (_JB_CORE_BASE + (31-19+1)*2)

#define _JB_MAGIC__SETJMP   0x53657200
#define _JB_MAGIC_SETJMP    0x53657201
