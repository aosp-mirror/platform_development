/*-
 * Copyright (c) 2001-2011 The FreeBSD Project.
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
 * $FreeBSD$
 */

#ifndef _COMPLEX_H
#define	_COMPLEX_H

#include <sys/cdefs.h>

#ifdef __GNUC__
#if __STDC_VERSION__ < 199901
#define	_Complex	__complex__
#endif
#define	_Complex_I	((float _Complex)1.0i)
#endif

#ifdef __generic
_Static_assert(__generic(_Complex_I, float _Complex, 1, 0),
    "_Complex_I must be of type float _Complex");
#endif

#define	complex		_Complex
#define	I		_Complex_I

__BEGIN_DECLS

double		cabs(double complex) __NDK_FPABI__;
float		cabsf(float complex) __NDK_FPABI__;
long double	cabsl(long double complex) __NDK_FPABI__;
double		carg(double complex) __NDK_FPABI__;
float		cargf(float complex) __NDK_FPABI__;
long double	cargl(long double complex) __NDK_FPABI__;
double complex	ccos(double complex) __NDK_FPABI__;
float complex	ccosf(float complex) __NDK_FPABI__;
double complex	ccosh(double complex) __NDK_FPABI__;
float complex	ccoshf(float complex) __NDK_FPABI__;
double complex	cexp(double complex) __NDK_FPABI__;
float complex	cexpf(float complex) __NDK_FPABI__;
double		cimag(double complex) __pure2 __NDK_FPABI__;
float		cimagf(float complex) __pure2 __NDK_FPABI__;
long double	cimagl(long double complex) __pure2 __NDK_FPABI__;
double complex	conj(double complex) __pure2 __NDK_FPABI__;
float complex	conjf(float complex) __pure2 __NDK_FPABI__;
long double complex
		conjl(long double complex) __pure2 __NDK_FPABI__;
float complex	cprojf(float complex) __pure2 __NDK_FPABI__;
double complex	cproj(double complex) __pure2 __NDK_FPABI__;
long double complex
		cprojl(long double complex) __pure2 __NDK_FPABI__;
double		creal(double complex) __pure2 __NDK_FPABI__;
float		crealf(float complex) __pure2 __NDK_FPABI__;
long double	creall(long double complex) __pure2 __NDK_FPABI__;
double complex	csin(double complex) __NDK_FPABI__;
float complex	csinf(float complex) __NDK_FPABI__;
double complex	csinh(double complex) __NDK_FPABI__;
float complex	csinhf(float complex) __NDK_FPABI__;
double complex	csqrt(double complex) __NDK_FPABI__;
float complex	csqrtf(float complex) __NDK_FPABI__;
long double complex
		csqrtl(long double complex) __NDK_FPABI__;
double complex	ctan(double complex) __NDK_FPABI__;
float complex	ctanf(float complex) __NDK_FPABI__;
double complex	ctanh(double complex) __NDK_FPABI__;
float complex	ctanhf(float complex) __NDK_FPABI__;

__END_DECLS

#endif /* _COMPLEX_H */
