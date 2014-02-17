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

#ifndef _FENV_H_
#define _FENV_H_

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
#define FE_ALL_EXCEPT (FE_DIVBYZERO | FE_INEXACT | FE_INVALID | FE_OVERFLOW | FE_UNDERFLOW)
#define _FPSCR_ENABLE_SHIFT 8
#define _FPSCR_ENABLE_MASK (FE_ALL_EXCEPT << _FPSCR_ENABLE_SHIFT)

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

/* Default floating-point environment. */
extern const fenv_t __fe_dfl_env;
#define FE_DFL_ENV (&__fe_dfl_env)

static __inline int fegetenv(fenv_t* __envp) {
    fenv_t _fpcr, _fpsr;
    __asm__ __volatile__("mrs %0,fpcr" : "=r" (_fpcr));
    __asm__ __volatile__("mrs %0,fpsr" : "=r" (_fpsr));
  *__envp = (_fpcr | _fpsr);
  return 0;
}

static __inline int fesetenv(const fenv_t* __envp) {
    fenv_t _fpcr = (*__envp & FPCR_MASK);
    fenv_t _fpsr = (*__envp & FPSR_MASK);
    __asm__ __volatile__("msr fpcr,%0" : :"ri" (_fpcr));
    __asm__ __volatile__("msr fpsr,%0" : :"ri" (_fpsr));
  return 0;
}

static __inline int feclearexcept(int __excepts) {
  fexcept_t __fpscr;
  fegetenv(&__fpscr);
  __fpscr &= ~__excepts;
  fesetenv(&__fpscr);
  return 0;
}

static __inline int fegetexceptflag(fexcept_t* __flagp, int __excepts) {
  fexcept_t __fpscr;
  fegetenv(&__fpscr);
  *__flagp = __fpscr & __excepts;
  return 0;
}

static __inline int fesetexceptflag(const fexcept_t* __flagp, int __excepts) {
  fexcept_t __fpscr;
  fegetenv(&__fpscr);
  __fpscr &= ~__excepts;
  __fpscr |= *__flagp & __excepts;
  fesetenv(&__fpscr);
  return 0;
}

static __inline int feraiseexcept(int __excepts) {
  fexcept_t __ex = __excepts;
  fesetexceptflag(&__ex, __excepts);
  return 0;
}

static __inline int fetestexcept(int __excepts) {
  fexcept_t __fpscr;
  fegetenv(&__fpscr);
  return (__fpscr & __excepts);
}

static __inline int fegetround(void) {
  fenv_t _fpscr;
  fegetenv(&_fpscr);
  return ((_fpscr >> _FPSCR_RMODE_SHIFT) & 0x3);
}

static __inline int fesetround(int __round) {
  fenv_t _fpscr;
  fegetenv(&_fpscr);
  _fpscr &= ~(0x3 << _FPSCR_RMODE_SHIFT);
  _fpscr |= (__round << _FPSCR_RMODE_SHIFT);
  fesetenv(&_fpscr);
  return 0;
}

static __inline int feholdexcept(fenv_t* __envp) {
  fenv_t __env;
  fegetenv(&__env);
  *__envp = __env;
  __env &= ~(FE_ALL_EXCEPT | _FPSCR_ENABLE_MASK);
  fesetenv(&__env);
  return 0;
}

static __inline int feupdateenv(const fenv_t* __envp) {
  fexcept_t __fpscr;
  fegetenv(&__fpscr);
  fesetenv(__envp);
  feraiseexcept(__fpscr & FE_ALL_EXCEPT);
  return 0;
}

#if __BSD_VISIBLE

static __inline int feenableexcept(int __mask) {
  fenv_t __old_fpscr, __new_fpscr;
  fegetenv(&__old_fpscr);
  __new_fpscr = __old_fpscr | (__mask & FE_ALL_EXCEPT) << _FPSCR_ENABLE_SHIFT;
  fesetenv(&__new_fpscr);
  return ((__old_fpscr >> _FPSCR_ENABLE_SHIFT) & FE_ALL_EXCEPT);
}

static __inline int fedisableexcept(int __mask) {
  fenv_t __old_fpscr, __new_fpscr;
  fegetenv(&__old_fpscr);
  __new_fpscr = __old_fpscr & ~((__mask & FE_ALL_EXCEPT) << _FPSCR_ENABLE_SHIFT);
  fesetenv(&__new_fpscr);
  return ((__old_fpscr >> _FPSCR_ENABLE_SHIFT) & FE_ALL_EXCEPT);
}

static __inline int fegetexcept(void) {
  fenv_t __fpscr;
  fegetenv(&__fpscr);
  return ((__fpscr & _FPSCR_ENABLE_MASK) >> _FPSCR_ENABLE_SHIFT);
}

#endif /* __BSD_VISIBLE */

__END_DECLS

#endif /* !_FENV_H_ */
