/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: db_utilities_linalg.h,v 1.5 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_UTILITIES_LINALG
#define DB_UTILITIES_LINALG

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMLinAlg (LM) Linear Algebra Utilities (QR factorization, orthogonal basis, etc.)
 */

/*!
 \ingroup LMBasicUtilities
 */
inline void db_MultiplyScalar6(double A[6],double mult)
{
    (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult;
    (*A++) *= mult;
}
/*!
 \ingroup LMBasicUtilities
 */
inline void db_MultiplyScalar7(double A[7],double mult)
{
    (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult;
    (*A++) *= mult; (*A++) *= mult;
}
/*!
 \ingroup LMBasicUtilities
 */
inline void db_MultiplyScalar9(double A[9],double mult)
{
    (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult;
    (*A++) *= mult; (*A++) *= mult; (*A++) *= mult; (*A++) *= mult;
}

/*!
 \ingroup LMBasicUtilities
 */
inline double db_SquareSum6Stride7(const double *x)
{
    return(db_sqr(x[0])+db_sqr(x[7])+db_sqr(x[14])+
        db_sqr(x[21])+db_sqr(x[28])+db_sqr(x[35]));
}

/*!
 \ingroup LMBasicUtilities
 */
inline double db_SquareSum8Stride9(const double *x)
{
    return(db_sqr(x[0])+db_sqr(x[9])+db_sqr(x[18])+
        db_sqr(x[27])+db_sqr(x[36])+db_sqr(x[45])+
        db_sqr(x[54])+db_sqr(x[63]));
}

/*!
 \ingroup LMLinAlg
 Cholesky-factorize symmetric positive definite 6 x 6 matrix A. Upper
part of A is used from the input. The Cholesky factor is output as
subdiagonal part in A and diagonal in d, which is 6-dimensional
1.9 microseconds on 450MHz*/
DB_API void db_CholeskyDecomp6x6(double A[36],double d[6]);

/*!
 \ingroup LMLinAlg
 Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of a 6 x 6 matrix and the right hand side b. The vector b is unchanged
1.3 microseconds on 450MHz*/
DB_API void db_CholeskyBacksub6x6(double x[6],const double A[36],const double d[6],const double b[6]);

/*!
 \ingroup LMLinAlg
 Cholesky-factorize symmetric positive definite n x n matrix A.Part
above diagonal of A is used from the input, diagonal of A is assumed to
be stored in d. The Cholesky factor is output as
subdiagonal part in A and diagonal in d, which is n-dimensional*/
DB_API void db_CholeskyDecompSeparateDiagonal(double **A,double *d,int n);

/*!
 \ingroup LMLinAlg
 Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of an n x n matrix and the right hand side b. The vector b is unchanged*/
DB_API void db_CholeskyBacksub(double *x,const double * const *A,const double *d,int n,const double *b);

/*!
 \ingroup LMLinAlg
 Cholesky-factorize symmetric positive definite 3 x 3 matrix A. Part
above diagonal of A is used from the input, diagonal of A is assumed to
be stored in d. The Cholesky factor is output as subdiagonal part in A
and diagonal in d, which is 3-dimensional*/
DB_API void db_CholeskyDecomp3x3SeparateDiagonal(double A[9],double d[3]);

/*!
 \ingroup LMLinAlg
 Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of a 3 x 3 matrix and the right hand side b. The vector b is unchanged*/
DB_API void db_CholeskyBacksub3x3(double x[3],const double A[9],const double d[3],const double b[3]);

/*!
 \ingroup LMLinAlg
 perform A-=B*mult*/
inline void db_RowOperation3(double A[3],const double B[3],double mult)
{
    *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++);
}

/*!
 \ingroup LMLinAlg
 */
inline void db_RowOperation7(double A[7],const double B[7],double mult)
{
    *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++);
    *A++ -= mult*(*B++); *A++ -= mult*(*B++);
}

/*!
 \ingroup LMLinAlg
 */
inline void db_RowOperation9(double A[9],const double B[9],double mult)
{
    *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++);
    *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++); *A++ -= mult*(*B++);
}

/*!
 \ingroup LMBasicUtilities
 Swap values of A[7] and B[7]
 */
inline void db_Swap7(double A[7],double B[7])
{
    double temp;
    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;
    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;
    temp= *A; *A++ = *B; *B++ =temp;
}

/*!
 \ingroup LMBasicUtilities
 Swap values of A[9] and B[9]
 */
inline void db_Swap9(double A[9],double B[9])
{
    double temp;
    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;
    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;
    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;    temp= *A; *A++ = *B; *B++ =temp;
}


/*!
 \ingroup LMLinAlg
 */
DB_API void db_Orthogonalize6x7(double A[42],int orthonormalize=0);

/*!
 \ingroup LMLinAlg
 */
DB_API void db_Orthogonalize8x9(double A[72],int orthonormalize=0);

/*!
 \ingroup LMLinAlg
 */
inline double db_OrthogonalizePair7(double *x,const double *v,double ssv)
{
    double m,sp,sp_m;

    m=db_SafeReciprocal(ssv);
    sp=db_ScalarProduct7(x,v);
    sp_m=sp*m;
    db_RowOperation7(x,v,sp_m);
    return(sp*sp_m);
}

/*!
 \ingroup LMLinAlg
 */
inline double db_OrthogonalizePair9(double *x,const double *v,double ssv)
{
    double m,sp,sp_m;

    m=db_SafeReciprocal(ssv);
    sp=db_ScalarProduct9(x,v);
    sp_m=sp*m;
    db_RowOperation9(x,v,sp_m);
    return(sp*sp_m);
}

/*!
 \ingroup LMLinAlg
 */
inline void db_OrthogonalizationSwap7(double *A,int i,double *ss)
{
    double temp;

    db_Swap7(A,A+7*i);
    temp=ss[0]; ss[0]=ss[i]; ss[i]=temp;
}
/*!
 \ingroup LMLinAlg
 */
inline void db_OrthogonalizationSwap9(double *A,int i,double *ss)
{
    double temp;

    db_Swap9(A,A+9*i);
    temp=ss[0]; ss[0]=ss[i]; ss[i]=temp;
}

/*!
 \ingroup LMLinAlg
 */
DB_API void db_NullVectorOrthonormal6x7(double x[7],const double A[42]);
/*!
 \ingroup LMLinAlg
 */
DB_API void db_NullVectorOrthonormal8x9(double x[9],const double A[72]);

/*!
 \ingroup LMLinAlg
 */
inline void db_NullVector6x7Destructive(double x[7],double A[42])
{
    db_Orthogonalize6x7(A,1);
    db_NullVectorOrthonormal6x7(x,A);
}

/*!
 \ingroup LMLinAlg
 */
inline void db_NullVector8x9Destructive(double x[9],double A[72])
{
    db_Orthogonalize8x9(A,1);
    db_NullVectorOrthonormal8x9(x,A);
}

inline int db_ScalarProduct512_s(const short *f,const short *g)
{
#ifndef DB_USE_MMX
    int back;
    back=0;
    for(int i=1; i<=512; i++)
        back+=(*f++)*(*g++);

    return(back);
#endif
}


inline int db_ScalarProduct32_s(const short *f,const short *g)
{
#ifndef DB_USE_MMX
    int back;
    back=0;
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    return(back);
#endif
}

/*!
 \ingroup LMLinAlg
 Scalar product of 128-vectors (short)
  Compile-time control: MMX, SSE2 or standard C
 */
inline int db_ScalarProduct128_s(const short *f,const short *g)
{
#ifndef DB_USE_MMX
    int back;
    back=0;
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    return(back);
#else
#ifdef DB_USE_SSE2
    int back;

    _asm
    {
        mov eax,f
        mov ecx,g
        /*First iteration************************************/
        movdqa     xmm0,[eax]
         pxor    xmm7,xmm7      /*Set xmm7 to zero*/
        pmaddwd  xmm0,[ecx]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm2,[eax+16]
         paddd   xmm7,xmm0
        pmaddwd  xmm2,[ecx+16]
         /*Stall*/
        movdqa     xmm1,[eax+32]
         paddd   xmm7,xmm2
        pmaddwd  xmm1,[ecx+32]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm0,[eax+48]
         paddd   xmm7,xmm1
        pmaddwd  xmm0,[ecx+48]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm2,[eax+64]
         paddd   xmm7,xmm0
        pmaddwd  xmm2,[ecx+64]
         /*Stall*/
        movdqa     xmm1,[eax+80]
         paddd   xmm7,xmm2
        pmaddwd  xmm1,[ecx+80]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm0,[eax+96]
         paddd   xmm7,xmm1
        pmaddwd  xmm0,[ecx+96]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm2,[eax+112]
         paddd   xmm7,xmm0
        pmaddwd  xmm2,[ecx+112]
         /*Stall*/
        movdqa     xmm1,[eax+128]
         paddd   xmm7,xmm2
        pmaddwd  xmm1,[ecx+128]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm0,[eax+144]
         paddd   xmm7,xmm1
        pmaddwd  xmm0,[ecx+144]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm2,[eax+160]
         paddd   xmm7,xmm0
        pmaddwd  xmm2,[ecx+160]
         /*Stall*/
        movdqa     xmm1,[eax+176]
         paddd   xmm7,xmm2
        pmaddwd  xmm1,[ecx+176]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm0,[eax+192]
         paddd   xmm7,xmm1
        pmaddwd  xmm0,[ecx+192]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm2,[eax+208]
         paddd   xmm7,xmm0
        pmaddwd  xmm2,[ecx+208]
         /*Stall*/
        movdqa     xmm1,[eax+224]
         paddd   xmm7,xmm2
        pmaddwd  xmm1,[ecx+224]
         /*Stall*/
        /*Standard iteration************************************/
        movdqa     xmm0,[eax+240]
         paddd   xmm7,xmm1
        pmaddwd  xmm0,[ecx+240]
         /*Stall*/
        /*Rest iteration************************************/
        paddd    xmm7,xmm0

        /* add up the sum squares */
        movhlps     xmm0,xmm7   /* high half to low half */
        paddd       xmm7,xmm0   /* add high to low */
        pshuflw     xmm0,xmm7, 0xE /* reshuffle */
        paddd       xmm7,xmm0   /* add remaining */
        movd        back,xmm7

        emms
    }

    return(back);
#else
    int back;

    _asm
    {
        mov eax,f
        mov ecx,g
        /*First iteration************************************/
        movq     mm0,[eax]
         pxor    mm7,mm7      /*Set mm7 to zero*/
        pmaddwd  mm0,[ecx]
         /*Stall*/
        movq     mm1,[eax+8]
         /*Stall*/
        pmaddwd  mm1,[ecx+8]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+16]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+16]
         /*Stall*/
        movq     mm0,[eax+24]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+24]
         /*Stall*/
        movq     mm1,[eax+32]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+32]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+40]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+40]
         /*Stall*/
        movq     mm0,[eax+48]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+48]
         /*Stall*/
        movq     mm1,[eax+56]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+56]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+64]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+64]
         /*Stall*/
        movq     mm0,[eax+72]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+72]
         /*Stall*/
        movq     mm1,[eax+80]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+80]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+88]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+88]
         /*Stall*/
        movq     mm0,[eax+96]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+96]
         /*Stall*/
        movq     mm1,[eax+104]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+104]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+112]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+112]
         /*Stall*/
        movq     mm0,[eax+120]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+120]
         /*Stall*/
        movq     mm1,[eax+128]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+128]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+136]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+136]
         /*Stall*/
        movq     mm0,[eax+144]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+144]
         /*Stall*/
        movq     mm1,[eax+152]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+152]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+160]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+160]
         /*Stall*/
        movq     mm0,[eax+168]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+168]
         /*Stall*/
        movq     mm1,[eax+176]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+176]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+184]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+184]
         /*Stall*/
        movq     mm0,[eax+192]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+192]
         /*Stall*/
        movq     mm1,[eax+200]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+200]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+208]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+208]
         /*Stall*/
        movq     mm0,[eax+216]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+216]
         /*Stall*/
        movq     mm1,[eax+224]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+224]
         /*Stall*/
        /*Standard iteration************************************/
        movq     mm2,[eax+232]
         paddd   mm7,mm0
        pmaddwd  mm2,[ecx+232]
         /*Stall*/
        movq     mm0,[eax+240]
         paddd   mm7,mm1
        pmaddwd  mm0,[ecx+240]
         /*Stall*/
        movq     mm1,[eax+248]
         paddd   mm7,mm2
        pmaddwd  mm1,[ecx+248]
         /*Stall*/
        /*Rest iteration************************************/
        paddd    mm7,mm0
         /*Stall*/
        /*Stall*/
         /*Stall*/
        paddd    mm7,mm1
         /*Stall*/
        movq     mm0,mm7
         psrlq   mm7,32
        paddd    mm0,mm7
         /*Stall*/
        /*Stall*/
         /*Stall*/
        movd     back,mm0
        emms
    }

    return(back);
#endif
#endif /*DB_USE_MMX*/
}

/*!
 \ingroup LMLinAlg
 Scalar product of 16 byte aligned 128-vectors (float).
  Compile-time control: SIMD (SSE) or standard C.
 */
inline float db_ScalarProduct128Aligned16_f(const float *f,const float *g)
{
#ifndef DB_USE_SIMD
    float back;
    back=0.0;
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);
    back+=(*f++)*(*g++); back+=(*f++)*(*g++); back+=(*f++)*(*g++);

    return(back);
#else
    float back;

    _asm
    {
        mov eax,f
        mov ecx,g
        /*First iteration************************************/
        movaps     xmm0,[eax]
         xorps      xmm7,xmm7       /*Set mm7 to zero*/
        mulps      xmm0,[ecx]
         /*Stall*/
        movaps     xmm1,[eax+16]
         /*Stall*/
        mulps      xmm1,[ecx+16]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+32]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+32]
         /*Stall*/
        movaps     xmm0,[eax+48]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+48]
         /*Stall*/
        movaps     xmm1,[eax+64]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+64]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+80]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+80]
         /*Stall*/
        movaps     xmm0,[eax+96]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+96]
         /*Stall*/
        movaps     xmm1,[eax+112]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+112]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+128]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+128]
         /*Stall*/
        movaps     xmm0,[eax+144]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+144]
         /*Stall*/
        movaps     xmm1,[eax+160]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+160]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+176]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+176]
         /*Stall*/
        movaps     xmm0,[eax+192]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+192]
         /*Stall*/
        movaps     xmm1,[eax+208]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+208]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+224]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+224]
         /*Stall*/
        movaps     xmm0,[eax+240]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+240]
         /*Stall*/
        movaps     xmm1,[eax+256]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+256]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+272]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+272]
         /*Stall*/
        movaps     xmm0,[eax+288]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+288]
         /*Stall*/
        movaps     xmm1,[eax+304]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+304]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+320]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+320]
         /*Stall*/
        movaps     xmm0,[eax+336]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+336]
         /*Stall*/
        movaps     xmm1,[eax+352]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+352]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+368]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+368]
         /*Stall*/
        movaps     xmm0,[eax+384]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+384]
         /*Stall*/
        movaps     xmm1,[eax+400]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+400]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+416]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+416]
         /*Stall*/
        movaps     xmm0,[eax+432]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+432]
         /*Stall*/
        movaps     xmm1,[eax+448]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+448]
         /*Stall*/
        /*Standard iteration************************************/
        movaps     xmm2,[eax+464]
         addps      xmm7,xmm0
        mulps      xmm2,[ecx+464]
         /*Stall*/
        movaps     xmm0,[eax+480]
         addps      xmm7,xmm1
        mulps      xmm0,[ecx+480]
         /*Stall*/
        movaps     xmm1,[eax+496]
         addps      xmm7,xmm2
        mulps      xmm1,[ecx+496]
         /*Stall*/
        /*Rest iteration************************************/
        addps      xmm7,xmm0
         /*Stall*/
        addps      xmm7,xmm1
         /*Stall*/
        movaps xmm6,xmm7
         /*Stall*/
        shufps xmm6,xmm6,4Eh
         /*Stall*/
        addps  xmm7,xmm6
         /*Stall*/
        movaps xmm6,xmm7
         /*Stall*/
        shufps xmm6,xmm6,11h
         /*Stall*/
        addps  xmm7,xmm6
         /*Stall*/
        movss  back,xmm7
    }

    return(back);
#endif /*DB_USE_SIMD*/
}

#endif /* DB_UTILITIES_LINALG */
