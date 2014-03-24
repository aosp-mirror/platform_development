/*-
 * Copyright (c) 2004-2005 David Schultz <das@FreeBSD.ORG>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * $FreeBSD: src/lib/msun/arm/fenv.h,v 1.5 2005/03/16 19:03:45 das Exp $
 */

/*
 * Rewritten for Android.
 *
 * The ARM FPSCR (Floating-point Status and Control Register) described here:
 * http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.ddi0344b/Chdfafia.html
 * has been split into the FPCR (Floating-point Control Register) and FPSR
 * (Floating-point Status Register) on the ARMv8. These are described briefly in
 * "Procedure Call Standard for the ARM 64-bit Architecture"
 * http://infocenter.arm.com/help/topic/com.arm.doc.ihi0055a/IHI0055A_aapcs64.pdf
 * section 5.1.2 SIMD and Floating-Point Registers
 */

#ifndef _ARM64_FENV_H_
#define _ARM64_FENV_H_

#include <sys/types.h>

__BEGIN_DECLS

typedef __uint32_t fenv_t;
typedef __uint32_t fexcept_t;

/* Exception flags. */
#define FE_INVALID    0x01
#define FE_DIVBYZERO  0x02
#define FE_OVERFLOW   0x04
#define FE_UNDERFLOW  0x08
#define FE_INEXACT    0x10
#define FE_ALL_EXCEPT (FE_DIVBYZERO | FE_INEXACT | FE_INVALID | \
                       FE_OVERFLOW | FE_UNDERFLOW)

#define _FPSCR_ENABLE_SHIFT 8
#define _FPSCR_ENABLE_MASK  (FE_ALL_EXCEPT << _FPSCR_ENABLE_SHIFT)

/* Rounding modes. */
#define FE_TONEAREST  0x0
#define FE_UPWARD     0x1
#define FE_DOWNWARD   0x2
#define FE_TOWARDZERO 0x3

#define _FPSCR_RMODE_SHIFT 22

#define FPCR_IOE    (1 << 8)
#define FPCR_DZE    (1 << 9)
#define FPCR_OFE    (1 << 10)
#define FPCR_UFE    (1 << 11)
#define FPCR_IXE    (1 << 12)
#define FPCR_IDE    (1 << 15)
#define FPCR_LEN    (7 << 16)
#define FPCR_STRIDE (3 << 20)
#define FPCR_RMODE  (3 << 22)
#define FPCR_FZ     (1 << 24)
#define FPCR_DN     (1 << 25)
#define FPCR_AHP    (1 << 26)
#define FPCR_MASK   (FPCR_IOE | \
                     FPCR_DZE | \
                     FPCR_OFE | \
                     FPCR_UFE | \
                     FPCR_IXE | \
                     FPCR_IDE | \
                     FPCR_LEN | \
                     FPCR_STRIDE | \
                     FPCR_RMODE | \
                     FPCR_FZ | \
                     FPCR_DN | \
                     FPCR_AHP )

#define FPSR_IOC    (1 << 0)
#define FPSR_DZC    (1 << 1)
#define FPSR_OFC    (1 << 2)
#define FPSR_UFC    (1 << 3)
#define FPSR_IXC    (1 << 4)
#define FPSR_IDC    (1 << 7)
#define FPSR_QC     (1 << 27)
#define FPSR_V      (1 << 28)
#define FPSR_C      (1 << 29)
#define FPSR_Z      (1 << 30)
#define FPSR_N      (1 << 31)
#define FPSR_MASK   (FPSR_IOC | \
                     FPSR_DZC | \
                     FPSR_OFC | \
                     FPSR_UFC | \
                     FPSR_IXC | \
                     FPSR_IDC | \
                     FPSR_QC | \
                     FPSR_V | \
                     FPSR_C | \
                     FPSR_Z | \
                     FPSR_N )

__END_DECLS

#endif /* !_ARM64_FENV_H_ */
