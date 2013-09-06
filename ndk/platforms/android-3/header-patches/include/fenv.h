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

/* 
 * arm/mips definition
 */
typedef __uint32_t fenv_t;
typedef __uint32_t fexcept_t;

/*
 * x86 definition
 */
/*
typedef struct {
        __uint16_t      __control;
        __uint16_t      __mxcsr_hi;
        __uint16_t      __status;
        __uint16_t      __mxcsr_lo;
        __uint32_t      __tag;
        char            __other[16];
} fenv_t;
*/


/* Exception flags. */
#define FE_INVALID    0x01
#define FE_DIVBYZERO  0x02
#define FE_OVERFLOW   0x04
#define FE_UNDERFLOW  0x08
#define FE_INEXACT    0x10
#define FE_ALL_EXCEPT (FE_DIVBYZERO | FE_INEXACT | FE_INVALID | FE_OVERFLOW | FE_UNDERFLOW)

/* Rounding modes. */
#define FE_TONEAREST  0x0
#define FE_UPWARD     0x1
#define FE_DOWNWARD   0x2
#define FE_TOWARDZERO 0x3

/* Default floating-point environment. */
extern const fenv_t __fe_dfl_env;
#define FE_DFL_ENV (&__fe_dfl_env)

/*
 * Need implementations in libportable to enable following functions
 *
 * int fegetenv(fenv_t* __envp);
 * int fesetenv(const fenv_t* __envp);
 * int feholdexcept(fenv_t* __envp);
 * int feupdateenv(const fenv_t* __envp);
 */

int feclearexcept(int __excepts);
int fegetexceptflag(fexcept_t* __flagp, int __excepts);
int fesetexceptflag(const fexcept_t* __flagp, int __excepts);
int feraiseexcept(int __excepts);
int fetestexcept(int __excepts);
int fegetround(void);
int fesetround(int __round);

__END_DECLS

#endif /* !_FENV_H_ */
