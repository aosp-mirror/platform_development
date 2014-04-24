/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunPro, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */

/*
 * from: @(#)fdlibm.h 5.1 93/09/24
 * $FreeBSD: src/lib/msun/src/math.h,v 1.61 2005/04/16 21:12:47 das Exp $
 */

#ifndef _MATH_H_
#define	_MATH_H_

#include <sys/cdefs.h>
#include <sys/types.h>
#include <limits.h>

#define __pure2

/*
 * ANSI/POSIX
 */
extern const union __infinity_un {
	unsigned char	__uc[8];
	double		__ud;
} __infinity;

extern const union __nan_un {
	unsigned char	__uc[sizeof(float)];
	float		__uf;
} __nan;

/* #if __GNUC_PREREQ__(3, 3) || (defined(__INTEL_COMPILER) && __INTEL_COMPILER >= 800) */
#if 1
#define	__MATH_BUILTIN_CONSTANTS
#endif

/* #if __GNUC_PREREQ__(3, 0) && !defined(__INTEL_COMPILER) */
#if 1
#define	__MATH_BUILTIN_RELOPS
#endif

/* #ifdef __MATH_BUILTIN_CONSTANTS */
#if 1
#define	HUGE_VAL	__builtin_huge_val()
#else
#define	HUGE_VAL	(__infinity.__ud)
#endif

/* #if __ISO_C_VISIBLE >= 1999 */
#if 0
#define	FP_ILOGB0	(-__INT_MAX)
#define	FP_ILOGBNAN	__INT_MAX
#else
#define	FP_ILOGB0	(-INT_MAX)
#define	FP_ILOGBNAN	INT_MAX
#endif

#ifdef __MATH_BUILTIN_CONSTANTS
#define	HUGE_VALF	__builtin_huge_valf()
#define	HUGE_VALL	__builtin_huge_vall()
#define	INFINITY	__builtin_inf()
#define	NAN		__builtin_nan("")
#else
#define	HUGE_VALF	(float)HUGE_VAL
#define	HUGE_VALL	(long double)HUGE_VAL
#define	INFINITY	HUGE_VALF
#define	NAN		(__nan.__uf)
#endif /* __MATH_BUILTIN_CONSTANTS */

#define	MATH_ERRNO	1
#define	MATH_ERREXCEPT	2
#define	math_errhandling	MATH_ERREXCEPT

/* XXX We need a <machine/math.h>. */
#if defined(__ia64__) || defined(__sparc64__)
#define	FP_FAST_FMA
#endif
#ifdef __ia64__
#define	FP_FAST_FMAL
#endif
#define	FP_FAST_FMAF

/* Symbolic constants to classify floating point numbers. */
#define	FP_INFINITE	0x01
#define	FP_NAN		0x02
#define	FP_NORMAL	0x04
#define	FP_SUBNORMAL	0x08
#define	FP_ZERO		0x10
#define	fpclassify(x) \
    ((sizeof (x) == sizeof (float)) ? __fpclassifyf(x) \
    : (sizeof (x) == sizeof (double)) ? __fpclassifyd(x) \
    : __fpclassifyl(x))

#define	isfinite(x)					\
    ((sizeof (x) == sizeof (float)) ? __isfinitef(x)	\
    : (sizeof (x) == sizeof (double)) ? __isfinite(x)	\
    : __isfinitel(x))
#define	isinf(x)					\
    ((sizeof (x) == sizeof (float)) ? __isinff(x)	\
    : (sizeof (x) == sizeof (double)) ? __isinf(x)	\
    : __isinfl(x))
#define	isnan(x)					\
    ((sizeof (x) == sizeof (float)) ? isnanf(x)		\
    : (sizeof (x) == sizeof (double)) ? isnan(x)	\
    : __isnanl(x))
#define	isnormal(x)					\
    ((sizeof (x) == sizeof (float)) ? __isnormalf(x)	\
    : (sizeof (x) == sizeof (double)) ? __isnormal(x)	\
    : __isnormall(x))

#ifdef __MATH_BUILTIN_RELOPS
#define	isgreater(x, y)		__builtin_isgreater((x), (y))
#define	isgreaterequal(x, y)	__builtin_isgreaterequal((x), (y))
#define	isless(x, y)		__builtin_isless((x), (y))
#define	islessequal(x, y)	__builtin_islessequal((x), (y))
#define	islessgreater(x, y)	__builtin_islessgreater((x), (y))
#define	isunordered(x, y)	__builtin_isunordered((x), (y))
#else
#define	isgreater(x, y)		(!isunordered((x), (y)) && (x) > (y))
#define	isgreaterequal(x, y)	(!isunordered((x), (y)) && (x) >= (y))
#define	isless(x, y)		(!isunordered((x), (y)) && (x) < (y))
#define	islessequal(x, y)	(!isunordered((x), (y)) && (x) <= (y))
#define	islessgreater(x, y)	(!isunordered((x), (y)) && \
					((x) > (y) || (y) > (x)))
#define	isunordered(x, y)	(isnan(x) || isnan(y))
#endif /* __MATH_BUILTIN_RELOPS */

#define	signbit(x)					\
    ((sizeof (x) == sizeof (float)) ? __signbitf(x)	\
    : (sizeof (x) == sizeof (double)) ? __signbit(x)	\
    : __signbitl(x))

#if 0
typedef	__double_t	double_t;
typedef	__float_t	float_t;
#endif 
/* #endif */ /* __ISO_C_VISIBLE >= 1999 */

/*
 * XOPEN/SVID
 */
/* #if __BSD_VISIBLE || __XSI_VISIBLE */
#define	M_E		2.7182818284590452354	/* e */
#define	M_LOG2E		1.4426950408889634074	/* log 2e */
#define	M_LOG10E	0.43429448190325182765	/* log 10e */
#define	M_LN2		0.69314718055994530942	/* log e2 */
#define	M_LN10		2.30258509299404568402	/* log e10 */
#define	M_PI		3.14159265358979323846	/* pi */
#define	M_PI_2		1.57079632679489661923	/* pi/2 */
#define	M_PI_4		0.78539816339744830962	/* pi/4 */
#define	M_1_PI		0.31830988618379067154	/* 1/pi */
#define	M_2_PI		0.63661977236758134308	/* 2/pi */
#define	M_2_SQRTPI	1.12837916709551257390	/* 2/sqrt(pi) */
#define	M_SQRT2		1.41421356237309504880	/* sqrt(2) */
#define	M_SQRT1_2	0.70710678118654752440	/* 1/sqrt(2) */

#define	MAXFLOAT	((float)3.40282346638528860e+38)
extern int signgam;
/* #endif */ /* __BSD_VISIBLE || __XSI_VISIBLE */

#if __BSD_VISIBLE
#if 0
/* Old value from 4.4BSD-Lite math.h; this is probably better. */
#define	HUGE		HUGE_VAL
#else
#define	HUGE		MAXFLOAT
#endif
#endif /* __BSD_VISIBLE */

/*
 * Most of these functions depend on the rounding mode and have the side
 * effect of raising floating-point exceptions, so they are not declared
 * as __pure2.  In C99, FENV_ACCESS affects the purity of these functions.
 */
__BEGIN_DECLS
/*
 * ANSI/POSIX
 */
int	__fpclassifyd(double) __NDK_FPABI_MATH__ __pure2;
int	__fpclassifyf(float) __NDK_FPABI_MATH__ __pure2;
int	__fpclassifyl(long double) __NDK_FPABI_MATH__ __pure2;
int	__isfinitef(float) __NDK_FPABI_MATH__ __pure2;
int	__isfinite(double) __NDK_FPABI_MATH__ __pure2;
int	__isfinitel(long double) __NDK_FPABI_MATH__ __pure2;
int	__isinff(float) __NDK_FPABI_MATH__ __pure2;
int     __isinf(double) __NDK_FPABI_MATH__ __pure2;
int	__isinfl(long double) __NDK_FPABI_MATH__ __pure2;
int	__isnanl(long double) __NDK_FPABI_MATH__ __pure2;
int	__isnormalf(float) __NDK_FPABI_MATH__ __pure2;
int	__isnormal(double) __NDK_FPABI_MATH__ __pure2;
int	__isnormall(long double) __NDK_FPABI_MATH__ __pure2;
int	__signbit(double) __NDK_FPABI_MATH__ __pure2;
int	__signbitf(float) __NDK_FPABI_MATH__ __pure2;
int	__signbitl(long double) __NDK_FPABI_MATH__ __pure2;

double	acos(double) __NDK_FPABI_MATH__;
double	asin(double) __NDK_FPABI_MATH__;
double	atan(double) __NDK_FPABI_MATH__;
double	atan2(double, double) __NDK_FPABI_MATH__;
double	cos(double) __NDK_FPABI_MATH__;
double	sin(double) __NDK_FPABI_MATH__;
double	tan(double) __NDK_FPABI_MATH__;

double	cosh(double) __NDK_FPABI_MATH__;
double	sinh(double) __NDK_FPABI_MATH__;
double	tanh(double) __NDK_FPABI_MATH__;

double	exp(double) __NDK_FPABI_MATH__;
double	frexp(double, int *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
double	ldexp(double, int) __NDK_FPABI_MATH__;
double	log(double) __NDK_FPABI_MATH__;
double	log10(double) __NDK_FPABI_MATH__;
double	modf(double, double *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */

double	pow(double, double) __NDK_FPABI_MATH__;
double	sqrt(double) __NDK_FPABI_MATH__;

double	ceil(double) __NDK_FPABI_MATH__;
double	fabs(double) __NDK_FPABI_MATH__ __pure2;
double	floor(double) __NDK_FPABI_MATH__;
double	fmod(double, double) __NDK_FPABI_MATH__;

/*
 * These functions are not in C90.
 */
/* #if __BSD_VISIBLE || __ISO_C_VISIBLE >= 1999 || __XSI_VISIBLE */
double	acosh(double) __NDK_FPABI_MATH__;
double	asinh(double) __NDK_FPABI_MATH__;
double	atanh(double) __NDK_FPABI_MATH__;
double	cbrt(double) __NDK_FPABI_MATH__;
double	erf(double) __NDK_FPABI_MATH__;
double	erfc(double) __NDK_FPABI_MATH__;
double	exp2(double) __NDK_FPABI_MATH__;
double	expm1(double) __NDK_FPABI_MATH__;
double	fma(double, double, double) __NDK_FPABI_MATH__;
double	hypot(double, double) __NDK_FPABI_MATH__;
int	ilogb(double) __NDK_FPABI_MATH__ __pure2;
/* int	(isinf)(double) __NDK_FPABI_MATH__ __pure2; */
int	(isnan)(double) __NDK_FPABI_MATH__ __pure2;
double	lgamma(double) __NDK_FPABI_MATH__;
long long llrint(double) __NDK_FPABI_MATH__;
long long llround(double) __NDK_FPABI_MATH__;
double	log1p(double) __NDK_FPABI_MATH__;
double	log2(double) __NDK_FPABI_MATH__;
double	logb(double) __NDK_FPABI_MATH__;
long	lrint(double) __NDK_FPABI_MATH__;
long	lround(double) __NDK_FPABI_MATH__;
double	nan(const char *) __NDK_FPABI_MATH__ __pure2;
double	nextafter(double, double) __NDK_FPABI_MATH__;
double	remainder(double, double) __NDK_FPABI_MATH__;
double	remquo(double, double, int *) __NDK_FPABI_MATH__;
double	rint(double) __NDK_FPABI_MATH__;
/* #endif */ /* __BSD_VISIBLE || __ISO_C_VISIBLE >= 1999 || __XSI_VISIBLE */

/* #if __BSD_VISIBLE || __XSI_VISIBLE */
double	j0(double) __NDK_FPABI_MATH__;
double	j1(double) __NDK_FPABI_MATH__;
double	jn(int, double) __NDK_FPABI_MATH__;
double	scalb(double, double) __NDK_FPABI_MATH__;
double	y0(double) __NDK_FPABI_MATH__;
double	y1(double) __NDK_FPABI_MATH__;
double	yn(int, double) __NDK_FPABI_MATH__;

/* #if __XSI_VISIBLE <= 500 || __BSD_VISIBLE */
double	gamma(double) __NDK_FPABI_MATH__;
/* #endif */
/* #endif */ /* __BSD_VISIBLE || __XSI_VISIBLE */

/* #if __BSD_VISIBLE || __ISO_C_VISIBLE >= 1999 */
double	copysign(double, double) __NDK_FPABI_MATH__ __pure2;
double	fdim(double, double) __NDK_FPABI_MATH__;
double	fmax(double, double) __NDK_FPABI_MATH__ __pure2;
double	fmin(double, double) __NDK_FPABI_MATH__ __pure2;
double	nearbyint(double) __NDK_FPABI_MATH__;
double	round(double) __NDK_FPABI_MATH__;
double	scalbln(double, long) __NDK_FPABI_MATH__;
double	scalbn(double, int) __NDK_FPABI_MATH__;
double	tgamma(double) __NDK_FPABI_MATH__;
double	trunc(double) __NDK_FPABI_MATH__;
/* #endif */

/*
 * BSD math library entry points
 */
/* #if __BSD_VISIBLE */
double	drem(double, double) __NDK_FPABI_MATH__;
int	finite(double) __NDK_FPABI_MATH__ __pure2;
int	isnanf(float) __NDK_FPABI_MATH__ __pure2;

/*
 * Reentrant version of gamma & lgamma; passes signgam back by reference
 * as the second argument; user must allocate space for signgam.
 */
double	gamma_r(double, int *) __NDK_FPABI_MATH__;
double	lgamma_r(double, int *) __NDK_FPABI_MATH__;

/*
 * IEEE Test Vector
 */
double	significand(double) __NDK_FPABI_MATH__;
/* #endif */ /* __BSD_VISIBLE */

/* float versions of ANSI/POSIX functions */
/*#if __ISO_C_VISIBLE >= 1999 */
float	acosf(float) __NDK_FPABI_MATH__;
float	asinf(float) __NDK_FPABI_MATH__;
float	atanf(float) __NDK_FPABI_MATH__;
float	atan2f(float, float) __NDK_FPABI_MATH__;
float	cosf(float) __NDK_FPABI_MATH__;
float	sinf(float) __NDK_FPABI_MATH__;
float	tanf(float) __NDK_FPABI_MATH__;

float	coshf(float) __NDK_FPABI_MATH__;
float	sinhf(float) __NDK_FPABI_MATH__;
float	tanhf(float) __NDK_FPABI_MATH__;

float	exp2f(float) __NDK_FPABI_MATH__;
float	expf(float) __NDK_FPABI_MATH__;
float	expm1f(float) __NDK_FPABI_MATH__;
float	frexpf(float, int *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
int	ilogbf(float) __NDK_FPABI_MATH__ __pure2;
float	ldexpf(float, int) __NDK_FPABI_MATH__;
float	log10f(float) __NDK_FPABI_MATH__;
float	log1pf(float) __NDK_FPABI_MATH__;
float	log2f(float) __NDK_FPABI_MATH__;
float	logf(float) __NDK_FPABI_MATH__;
float	modff(float, float *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */

float	powf(float, float) __NDK_FPABI_MATH__;
float	sqrtf(float) __NDK_FPABI_MATH__;

float	ceilf(float) __NDK_FPABI_MATH__;
float	fabsf(float) __NDK_FPABI_MATH__ __pure2;
float	floorf(float) __NDK_FPABI_MATH__;
float	fmodf(float, float) __NDK_FPABI_MATH__;
float	roundf(float) __NDK_FPABI_MATH__;

float	erff(float) __NDK_FPABI_MATH__;
float	erfcf(float) __NDK_FPABI_MATH__;
float	hypotf(float, float) __NDK_FPABI_MATH__;
float	lgammaf(float) __NDK_FPABI_MATH__;
float	tgammaf(float) __NDK_FPABI_MATH__;

float	acoshf(float) __NDK_FPABI_MATH__;
float	asinhf(float) __NDK_FPABI_MATH__;
float	atanhf(float) __NDK_FPABI_MATH__;
float	cbrtf(float) __NDK_FPABI_MATH__;
float	logbf(float) __NDK_FPABI_MATH__;
float	copysignf(float, float) __NDK_FPABI_MATH__ __pure2;
long long llrintf(float) __NDK_FPABI_MATH__;
long long llroundf(float) __NDK_FPABI_MATH__;
long	lrintf(float) __NDK_FPABI_MATH__;
long	lroundf(float) __NDK_FPABI_MATH__;
float	nanf(const char *) __NDK_FPABI_MATH__ __pure2;
float	nearbyintf(float) __NDK_FPABI_MATH__;
float	nextafterf(float, float) __NDK_FPABI_MATH__;
float	remainderf(float, float) __NDK_FPABI_MATH__;
float	remquof(float, float, int *) __NDK_FPABI_MATH__;
float	rintf(float) __NDK_FPABI_MATH__;
float	scalblnf(float, long) __NDK_FPABI_MATH__;
float	scalbnf(float, int) __NDK_FPABI_MATH__;
float	truncf(float) __NDK_FPABI_MATH__;

float	fdimf(float, float) __NDK_FPABI_MATH__;
float	fmaf(float, float, float) __NDK_FPABI_MATH__;
float	fmaxf(float, float) __NDK_FPABI_MATH__ __pure2;
float	fminf(float, float) __NDK_FPABI_MATH__ __pure2;
/* #endif */

/*
 * float versions of BSD math library entry points
 */
/* #if __BSD_VISIBLE */
float	dremf(float, float) __NDK_FPABI_MATH__;
int	finitef(float) __NDK_FPABI_MATH__ __pure2;
float	gammaf(float) __NDK_FPABI_MATH__;
float	j0f(float) __NDK_FPABI_MATH__;
float	j1f(float) __NDK_FPABI_MATH__;
float	jnf(int, float) __NDK_FPABI_MATH__;
float	scalbf(float, float) __NDK_FPABI_MATH__;
float	y0f(float) __NDK_FPABI_MATH__;
float	y1f(float) __NDK_FPABI_MATH__;
float	ynf(int, float) __NDK_FPABI_MATH__;

/*
 * Float versions of reentrant version of gamma & lgamma; passes
 * signgam back by reference as the second argument; user must
 * allocate space for signgam.
 */
float	gammaf_r(float, int *) __NDK_FPABI_MATH__;
float	lgammaf_r(float, int *) __NDK_FPABI_MATH__;

/*
 * float version of IEEE Test Vector
 */
float	significandf(float) __NDK_FPABI_MATH__;
/* #endif */	/* __BSD_VISIBLE */ 

/*
 * long double versions of ISO/POSIX math functions
 */
/* #if __ISO_C_VISIBLE >= 1999 */
#if 0
long double	acoshl(long double) __NDK_FPABI_MATH__;
long double	acosl(long double) __NDK_FPABI_MATH__;
long double	asinhl(long double) __NDK_FPABI_MATH__;
long double	asinl(long double) __NDK_FPABI_MATH__;
long double	atan2l(long double, long double) __NDK_FPABI_MATH__;
long double	atanhl(long double) __NDK_FPABI_MATH__;
long double	atanl(long double) __NDK_FPABI_MATH__;
long double	cbrtl(long double) __NDK_FPABI_MATH__;
#endif
long double	ceill(long double) __NDK_FPABI_MATH__;
long double	copysignl(long double, long double) __NDK_FPABI_MATH__ __pure2;
#if 0
long double	coshl(long double) __NDK_FPABI_MATH__;
long double	cosl(long double) __NDK_FPABI_MATH__;
long double	erfcl(long double) __NDK_FPABI_MATH__;
long double	erfl(long double) __NDK_FPABI_MATH__;
long double	exp2l(long double) __NDK_FPABI_MATH__;
long double	expl(long double) __NDK_FPABI_MATH__;
long double	expm1l(long double) __NDK_FPABI_MATH__;
#endif
long double	fabsl(long double) __NDK_FPABI_MATH__ __pure2;
long double	fdiml(long double, long double) __NDK_FPABI_MATH__;
long double	floorl(long double) __NDK_FPABI_MATH__;
long double	fmal(long double, long double, long double) __NDK_FPABI_MATH__;
long double	fmaxl(long double, long double) __NDK_FPABI_MATH__ __pure2;
long double	fminl(long double, long double) __NDK_FPABI_MATH__ __pure2;
#if 0
long double	fmodl(long double, long double) __NDK_FPABI_MATH__;
#endif
long double	frexpl(long double value, int *) __NDK_FPABI_MATH__; /* fundamentally !__pure2 */
#if 0
long double	hypotl(long double, long double) __NDK_FPABI_MATH__;
#endif
int		ilogbl(long double) __NDK_FPABI_MATH__ __pure2;
long double	ldexpl(long double, int) __NDK_FPABI_MATH__;
#if 0
long double	lgammal(long double) __NDK_FPABI_MATH__;
long long	llrintl(long double) __NDK_FPABI_MATH__;
#endif
long long	llroundl(long double) __NDK_FPABI_MATH__;
#if 0
long double	log10l(long double) __NDK_FPABI_MATH__;
long double	log1pl(long double) __NDK_FPABI_MATH__;
long double	log2l(long double) __NDK_FPABI_MATH__;
#endif
long double	logbl(long double) __NDK_FPABI_MATH__;
#if 0
long		lrintl(long double) __NDK_FPABI_MATH__;
#endif
long		lroundl(long double) __NDK_FPABI_MATH__;
#if 0
long double	modfl(long double, long double *) __NDK_FPABI_MATH__; /* fundamentally !__pure2 */
#endif
long double	nanl(const char *) __NDK_FPABI_MATH__ __pure2;
#if 0
long double	nearbyintl(long double) __NDK_FPABI_MATH__;
#endif
long double	nextafterl(long double, long double) __NDK_FPABI_MATH__;
double		nexttoward(double, long double) __NDK_FPABI_MATH__;
float		nexttowardf(float, long double) __NDK_FPABI_MATH__;
long double	nexttowardl(long double, long double) __NDK_FPABI_MATH__;
#if 0
long double	powl(long double, long double) __NDK_FPABI_MATH__;
long double	remainderl(long double, long double) __NDK_FPABI_MATH__;
long double	remquol(long double, long double, int *) __NDK_FPABI_MATH__;
long double	rintl(long double) __NDK_FPABI_MATH__;
#endif
long double	roundl(long double) __NDK_FPABI_MATH__;
long double	scalblnl(long double, long) __NDK_FPABI_MATH__;
long double	scalbnl(long double, int) __NDK_FPABI_MATH__;
#if 0
long double	sinhl(long double) __NDK_FPABI_MATH__;
long double	sinl(long double) __NDK_FPABI_MATH__;
long double	sqrtl(long double) __NDK_FPABI_MATH__;
long double	tanhl(long double) __NDK_FPABI_MATH__;
long double	tanl(long double) __NDK_FPABI_MATH__;
long double	tgammal(long double) __NDK_FPABI_MATH__;
#endif
long double	truncl(long double) __NDK_FPABI_MATH__;

/* BIONIC: GLibc compatibility - required by the ARM toolchain */
#ifdef _GNU_SOURCE
void  sincos(double x, double *sin, double *cos) __NDK_FPABI_MATH__;
void  sincosf(float x, float *sin, float *cos) __NDK_FPABI_MATH__;
void  sincosl(long double x, long double *sin, long double *cos) __NDK_FPABI_MATH__;
#endif

long double	log2l(long double) __NDK_FPABI_MATH__;

/* builtin version of all the above math functions are annotated too */

double	__builtin_acos(double) __NDK_FPABI_MATH__;
double	__builtin_asin(double) __NDK_FPABI_MATH__;
double	__builtin_atan(double) __NDK_FPABI_MATH__;
double	__builtin_atan2(double, double) __NDK_FPABI_MATH__;
double	__builtin_cos(double) __NDK_FPABI_MATH__;
double	__builtin_sin(double) __NDK_FPABI_MATH__;
double	__builtin_tan(double) __NDK_FPABI_MATH__;
double	__builtin_cosh(double) __NDK_FPABI_MATH__;
double	__builtin_sinh(double) __NDK_FPABI_MATH__;
double	__builtin_tanh(double) __NDK_FPABI_MATH__;
double	__builtin_exp(double) __NDK_FPABI_MATH__;
double	__builtin_frexp(double, int *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
double	__builtin_ldexp(double, int) __NDK_FPABI_MATH__;
double	__builtin_log(double) __NDK_FPABI_MATH__;
double	__builtin_log10(double) __NDK_FPABI_MATH__;
double	__builtin_modf(double, double *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
double	__builtin_pow(double, double) __NDK_FPABI_MATH__;
double	__builtin_sqrt(double) __NDK_FPABI_MATH__;
double	__builtin_ceil(double) __NDK_FPABI_MATH__;
double	__builtin_fabs(double) __NDK_FPABI_MATH__ __pure2;
double	__builtin_floor(double) __NDK_FPABI_MATH__;
double	__builtin_fmod(double, double) __NDK_FPABI_MATH__;
double	__builtin_acosh(double) __NDK_FPABI_MATH__;
double	__builtin_asinh(double) __NDK_FPABI_MATH__;
double	__builtin_atanh(double) __NDK_FPABI_MATH__;
double	__builtin_cbrt(double) __NDK_FPABI_MATH__;
double	__builtin_erf(double) __NDK_FPABI_MATH__;
double	__builtin_erfc(double) __NDK_FPABI_MATH__;
double	__builtin_exp2(double) __NDK_FPABI_MATH__;
double	__builtin_expm1(double) __NDK_FPABI_MATH__;
double	__builtin_fma(double, double, double) __NDK_FPABI_MATH__;
double	__builtin_hypot(double, double) __NDK_FPABI_MATH__;
int	__builtin_ilogb(double) __NDK_FPABI_MATH__ __pure2;
/* int	__builtin_isinf(double) __NDK_FPABI_MATH__ __pure2; */
#if !defined(__clang__) || __clang_major__ > 3 || (__clang_major__ == 3 && __clang_minor__ >= 6)
int	__builtin_isnan(double) __NDK_FPABI_MATH__ __pure2;
#else
/* clang < 3.5 has faulty prototype for __builtin_isnan */
#endif
double	__builtin_lgamma(double) __NDK_FPABI_MATH__;
long long __builtin_llrint(double) __NDK_FPABI_MATH__;
long long __builtin_llround(double) __NDK_FPABI_MATH__;
double	__builtin_log1p(double) __NDK_FPABI_MATH__;
double	__builtin_log2(double) __NDK_FPABI_MATH__;
double	__builtin_logb(double) __NDK_FPABI_MATH__;
long	__builtin_lrint(double) __NDK_FPABI_MATH__;
long	__builtin_lround(double) __NDK_FPABI_MATH__;
double	__builtin_nan(const char *) __NDK_FPABI_MATH__ __pure2;
double	__builtin_nextafter(double, double) __NDK_FPABI_MATH__;
double	__builtin_remainder(double, double) __NDK_FPABI_MATH__;
double	__builtin_remquo(double, double, int *) __NDK_FPABI_MATH__;
double	__builtin_rint(double) __NDK_FPABI_MATH__;
double	__builtin_j0(double) __NDK_FPABI_MATH__;
double	__builtin_j1(double) __NDK_FPABI_MATH__;
double	__builtin_jn(int, double) __NDK_FPABI_MATH__;
double	__builtin_scalb(double, double) __NDK_FPABI_MATH__;
double	__builtin_y0(double) __NDK_FPABI_MATH__;
double	__builtin_y1(double) __NDK_FPABI_MATH__;
double	__builtin_yn(int, double) __NDK_FPABI_MATH__;
double	__builtin_gamma(double) __NDK_FPABI_MATH__;
double	__builtin_copysign(double, double) __NDK_FPABI_MATH__ __pure2;
double	__builtin_fdim(double, double) __NDK_FPABI_MATH__;
double	__builtin_fmax(double, double) __NDK_FPABI_MATH__ __pure2;
double	__builtin_fmin(double, double) __NDK_FPABI_MATH__ __pure2;
double	__builtin_nearbyint(double) __NDK_FPABI_MATH__;
double	__builtin_round(double) __NDK_FPABI_MATH__;
double	__builtin_scalbln(double, long) __NDK_FPABI_MATH__;
double	__builtin_scalbn(double, int) __NDK_FPABI_MATH__;
double	__builtin_tgamma(double) __NDK_FPABI_MATH__;
double	__builtin_trunc(double) __NDK_FPABI_MATH__;
double	__builtin_drem(double, double) __NDK_FPABI_MATH__;
int	__builtin_finite(double) __NDK_FPABI_MATH__ __pure2;
int	__builtin_isnanf(float) __NDK_FPABI_MATH__ __pure2;
double	__builtin_gamma_r(double, int *) __NDK_FPABI_MATH__;
double	__builtin_lgamma_r(double, int *) __NDK_FPABI_MATH__;
double	__builtin_significand(double) __NDK_FPABI_MATH__;
float	__builtin_acosf(float) __NDK_FPABI_MATH__;
float	__builtin_asinf(float) __NDK_FPABI_MATH__;
float	__builtin_atanf(float) __NDK_FPABI_MATH__;
float	__builtin_atan2f(float, float) __NDK_FPABI_MATH__;
float	__builtin_cosf(float) __NDK_FPABI_MATH__;
float	__builtin_sinf(float) __NDK_FPABI_MATH__;
float	__builtin_tanf(float) __NDK_FPABI_MATH__;
float	__builtin_coshf(float) __NDK_FPABI_MATH__;
float	__builtin_sinhf(float) __NDK_FPABI_MATH__;
float	__builtin_tanhf(float) __NDK_FPABI_MATH__;
float	__builtin_exp2f(float) __NDK_FPABI_MATH__;
float	__builtin_expf(float) __NDK_FPABI_MATH__;
float	__builtin_expm1f(float) __NDK_FPABI_MATH__;
float	__builtin_frexpf(float, int *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
int	__builtin_ilogbf(float) __NDK_FPABI_MATH__ __pure2;
float	__builtin_ldexpf(float, int) __NDK_FPABI_MATH__;
float	__builtin_log10f(float) __NDK_FPABI_MATH__;
float	__builtin_log1pf(float) __NDK_FPABI_MATH__;
float	__builtin_log2f(float) __NDK_FPABI_MATH__;
float	__builtin_logf(float) __NDK_FPABI_MATH__;
float	__builtin_modff(float, float *) __NDK_FPABI_MATH__;	/* fundamentally !__pure2 */
float	__builtin_powf(float, float) __NDK_FPABI_MATH__;
float	__builtin_sqrtf(float) __NDK_FPABI_MATH__;
float	__builtin_ceilf(float) __NDK_FPABI_MATH__;
float	__builtin_fabsf(float) __NDK_FPABI_MATH__ __pure2;
float	__builtin_floorf(float) __NDK_FPABI_MATH__;
float	__builtin_fmodf(float, float) __NDK_FPABI_MATH__;
float	__builtin_roundf(float) __NDK_FPABI_MATH__;
float	__builtin_erff(float) __NDK_FPABI_MATH__;
float	__builtin_erfcf(float) __NDK_FPABI_MATH__;
float	__builtin_hypotf(float, float) __NDK_FPABI_MATH__;
float	__builtin_lgammaf(float) __NDK_FPABI_MATH__;
float	__builtin_tgammaf(float) __NDK_FPABI_MATH__;
float	__builtin_acoshf(float) __NDK_FPABI_MATH__;
float	__builtin_asinhf(float) __NDK_FPABI_MATH__;
float	__builtin_atanhf(float) __NDK_FPABI_MATH__;
float	__builtin_cbrtf(float) __NDK_FPABI_MATH__;
float	__builtin_logbf(float) __NDK_FPABI_MATH__;
float	__builtin_copysignf(float, float) __NDK_FPABI_MATH__ __pure2;
long long __builtin_llrintf(float) __NDK_FPABI_MATH__;
long long __builtin_llroundf(float) __NDK_FPABI_MATH__;
long	__builtin_lrintf(float) __NDK_FPABI_MATH__;
long	__builtin_lroundf(float) __NDK_FPABI_MATH__;
float	__builtin_nanf(const char *) __NDK_FPABI_MATH__ __pure2;
float	__builtin_nearbyintf(float) __NDK_FPABI_MATH__;
float	__builtin_nextafterf(float, float) __NDK_FPABI_MATH__;
float	__builtin_remainderf(float, float) __NDK_FPABI_MATH__;
float	__builtin_remquof(float, float, int *) __NDK_FPABI_MATH__;
float	__builtin_rintf(float) __NDK_FPABI_MATH__;
float	__builtin_scalblnf(float, long) __NDK_FPABI_MATH__;
float	__builtin_scalbnf(float, int) __NDK_FPABI_MATH__;
float	__builtin_truncf(float) __NDK_FPABI_MATH__;
float	__builtin_fdimf(float, float) __NDK_FPABI_MATH__;
float	__builtin_fmaf(float, float, float) __NDK_FPABI_MATH__;
float	__builtin_fmaxf(float, float) __NDK_FPABI_MATH__ __pure2;
float	__builtin_fminf(float, float) __NDK_FPABI_MATH__ __pure2;
float	__builtin_dremf(float, float) __NDK_FPABI_MATH__;
int	__builtin_finitef(float) __NDK_FPABI_MATH__ __pure2;
float	__builtin_gammaf(float) __NDK_FPABI_MATH__;
float	__builtin_j0f(float) __NDK_FPABI_MATH__;
float	__builtin_j1f(float) __NDK_FPABI_MATH__;
float	__builtin_jnf(int, float) __NDK_FPABI_MATH__;
float	__builtin_scalbf(float, float) __NDK_FPABI_MATH__;
float	__builtin_y0f(float) __NDK_FPABI_MATH__;
float	__builtin_y1f(float) __NDK_FPABI_MATH__;
float	__builtin_ynf(int, float) __NDK_FPABI_MATH__;
float	__builtin_gammaf_r(float, int *) __NDK_FPABI_MATH__;
float	__builtin_lgammaf_r(float, int *) __NDK_FPABI_MATH__;
float	__builtin_significandf(float) __NDK_FPABI_MATH__;
long double	__builtin_acoshl(long double) __NDK_FPABI_MATH__;
long double	__builtin_acosl(long double) __NDK_FPABI_MATH__;
long double	__builtin_asinhl(long double) __NDK_FPABI_MATH__;
long double	__builtin_asinl(long double) __NDK_FPABI_MATH__;
long double	__builtin_atan2l(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_atanhl(long double) __NDK_FPABI_MATH__;
long double	__builtin_atanl(long double) __NDK_FPABI_MATH__;
long double	__builtin_cbrtl(long double) __NDK_FPABI_MATH__;
long double	__builtin_ceill(long double) __NDK_FPABI_MATH__;
long double	__builtin_copysignl(long double, long double) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_coshl(long double) __NDK_FPABI_MATH__;
long double	__builtin_cosl(long double) __NDK_FPABI_MATH__;
long double	__builtin_erfcl(long double) __NDK_FPABI_MATH__;
long double	__builtin_erfl(long double) __NDK_FPABI_MATH__;
long double	__builtin_exp2l(long double) __NDK_FPABI_MATH__;
long double	__builtin_expl(long double) __NDK_FPABI_MATH__;
long double	__builtin_expm1l(long double) __NDK_FPABI_MATH__;
long double	__builtin_fabsl(long double) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_fdiml(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_floorl(long double) __NDK_FPABI_MATH__;
long double	__builtin_fmal(long double, long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_fmaxl(long double, long double) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_fminl(long double, long double) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_fmodl(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_frexpl(long double value, int *) __NDK_FPABI_MATH__; /* fundamentally !__pure2 */
long double	__builtin_hypotl(long double, long double) __NDK_FPABI_MATH__;
int		__builtin_ilogbl(long double) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_ldexpl(long double, int) __NDK_FPABI_MATH__;
long double	__builtin_lgammal(long double) __NDK_FPABI_MATH__;
long long	__builtin_llrintl(long double) __NDK_FPABI_MATH__;
long long	__builtin_llroundl(long double) __NDK_FPABI_MATH__;
long double	__builtin_log10l(long double) __NDK_FPABI_MATH__;
long double	__builtin_log1pl(long double) __NDK_FPABI_MATH__;
long double	__builtin_log2l(long double) __NDK_FPABI_MATH__;
long double	__builtin_logbl(long double) __NDK_FPABI_MATH__;
long double	__builtin_logl(long double) __NDK_FPABI_MATH__;
long		__builtin_lrintl(long double) __NDK_FPABI_MATH__;
long		__builtin_lroundl(long double) __NDK_FPABI_MATH__;
long double	__builtin_modfl(long double, long double *) __NDK_FPABI_MATH__; /* fundamentally !__pure2 */
long double	__builtin_nanl(const char *) __NDK_FPABI_MATH__ __pure2;
long double	__builtin_nearbyintl(long double) __NDK_FPABI_MATH__;
long double	__builtin_nextafterl(long double, long double) __NDK_FPABI_MATH__;
#if !defined(__clang__) || __clang_major__ > 3 || (__clang_major__ == 3 && __clang_minor__ >= 4)
double		__builtin_nexttoward(double, long double) __NDK_FPABI_MATH__;
float		__builtin_nexttowardf(float, long double) __NDK_FPABI_MATH__;
#else
/* clang < 3.4 */
double		__builtin_nexttoward(double, double) __NDK_FPABI_MATH__;
float		__builtin_nexttowardf(float, float) __NDK_FPABI_MATH__;
#endif
long double	__builtin_nexttowardl(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_powl(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_remainderl(long double, long double) __NDK_FPABI_MATH__;
long double	__builtin_remquol(long double, long double, int *) __NDK_FPABI_MATH__;
long double	__builtin_rintl(long double) __NDK_FPABI_MATH__;
long double	__builtin_roundl(long double) __NDK_FPABI_MATH__;
long double	__builtin_scalblnl(long double, long) __NDK_FPABI_MATH__;
long double	__builtin_scalbnl(long double, int) __NDK_FPABI_MATH__;
long double	__builtin_sinhl(long double) __NDK_FPABI_MATH__;
long double	__builtin_sinl(long double) __NDK_FPABI_MATH__;
long double	__builtin_sqrtl(long double) __NDK_FPABI_MATH__;
long double	__builtin_tanhl(long double) __NDK_FPABI_MATH__;
long double	__builtin_tanl(long double) __NDK_FPABI_MATH__;
long double	__builtin_tgammal(long double) __NDK_FPABI_MATH__;
long double	__builtin_truncl(long double) __NDK_FPABI_MATH__;
void  __builtin_sincos(double x, double *sin, double *cos) __NDK_FPABI_MATH__;
void  __builtin_sincosf(float x, float *sin, float *cos) __NDK_FPABI_MATH__;
void  __builtin_sincosl(long double x, long double *sin, long double *cos) __NDK_FPABI_MATH__;
long double	__builtin_log2l(long double) __NDK_FPABI_MATH__;

/* #endif */ /* __ISO_C_VISIBLE >= 1999 */
__END_DECLS

#endif /* !_MATH_H_ */
