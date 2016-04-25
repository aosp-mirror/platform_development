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
 * The ARM FPSCR is described here:
 * http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.ddi0344b/Chdfafia.html
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

/* Default floating-point environment. */
extern const fenv_t __fe_dfl_env;
#define FE_DFL_ENV (&__fe_dfl_env)

static __inline int fegetenv(fenv_t* __envp) {
  fenv_t _fpscr;
#if !defined(__SOFTFP__)
  #if !defined(__thumb__) || defined(__thumb2__)
  __asm__ __volatile__("vmrs %0,fpscr" : "=r" (_fpscr));
  #else
   /* Switching from thumb1 to arm, do vmrs, then switch back */
  __asm__ __volatile__(
    ".balign 4           \n\t"
    "mov     ip, pc      \n\t"
    "bx      ip          \n\t"
    ".arm                \n\t"
    "vmrs    %0, fpscr   \n\t"
    "add     ip, pc, #1  \n\t"
    "bx      ip          \n\t"
    ".thumb              \n\t"
    : "=r" (_fpscr) : : "ip");
  #endif
#else
  _fpscr = 0;
#endif
  *__envp = _fpscr;
  return 0;
}

static __inline int fesetenv(const fenv_t* __envp) {
  fenv_t _fpscr = *__envp;
#if !defined(__SOFTFP__)
  #if !defined(__thumb__) || defined(__thumb2__)
  __asm__ __volatile__("vmsr fpscr,%0" : :"ri" (_fpscr));
  #else
   /* Switching from thumb1 to arm, do vmsr, then switch back */
  __asm__ __volatile__(
    ".balign 4           \n\t"
    "mov     ip, pc      \n\t"
    "bx      ip          \n\t"
    ".arm                \n\t"
    "vmsr    fpscr, %0   \n\t"
    "add     ip, pc, #1  \n\t"
    "bx      ip          \n\t"
    ".thumb              \n\t"
    : : "ri" (_fpscr) : "ip");
  #endif
#else
  _fpscr = _fpscr;
#endif
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
