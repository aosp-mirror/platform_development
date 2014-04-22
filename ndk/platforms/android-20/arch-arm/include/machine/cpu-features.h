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
#ifndef _ARM_MACHINE_CPU_FEATURES_H
#define _ARM_MACHINE_CPU_FEATURES_H

/* The purpose of this file is to define several macros corresponding
 * to CPU features that may or may not be available at build time on
 * on the target CPU.
 *
 * This is done to abstract us from the various ARM Architecture
 * quirks and alphabet soup.
 */

/* __ARM_ARCH__ is a number corresponding to the ARM revision
 * we're going to support. Our toolchain doesn't define __ARM_ARCH__
 * so try to guess it.
 */
#ifndef __ARM_ARCH__
#  if defined __ARM_ARCH_7__   || defined __ARM_ARCH_7A__ || \
        defined __ARM_ARCH_7R__  || defined __ARM_ARCH_7M__
#    define __ARM_ARCH__ 7
#  elif defined __ARM_ARCH_6__   || defined __ARM_ARCH_6J__ || \
        defined __ARM_ARCH_6K__  || defined __ARM_ARCH_6Z__ || \
        defined __ARM_ARCH_6KZ__ || defined __ARM_ARCH_6T2__
#    define __ARM_ARCH__ 6
#  else
#    error Unknown or unsupported ARM architecture
#  endif
#endif

/* define __ARM_HAVE_HALFWORD_MULTIPLY when half-word multiply instructions
 * this means variants of: smul, smulw, smla, smlaw, smlal
 */
#define  __ARM_HAVE_HALFWORD_MULTIPLY  1

/* define __ARM_HAVE_LDREXD for ARMv7 architecture
 * (also present in ARMv6K, but not implemented in ARMv7-M, neither of which
 * we care about)
 */
#if __ARM_ARCH__ >= 7
#  define __ARM_HAVE_LDREXD
#endif

/* define _ARM_HAVE_VFP if we have VFPv3
 */
#if __ARM_ARCH__ >= 7 && defined __VFP_FP__
#  define __ARM_HAVE_VFP
#endif

/* define _ARM_HAVE_NEON for ARMv7 architecture if we support the
 * Neon SIMD instruction set extensions. This also implies
 * that VFPv3-D32 is supported.
 */
#if __ARM_ARCH__ >= 7 && defined __ARM_NEON__
#  define __ARM_HAVE_NEON
#endif

#endif /* _ARM_MACHINE_CPU_FEATURES_H */
