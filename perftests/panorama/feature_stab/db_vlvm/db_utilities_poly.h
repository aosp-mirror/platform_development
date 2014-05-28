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

/* $Id: db_utilities_poly.h,v 1.2 2010/09/03 12:00:11 bsouthall Exp $ */

#ifndef DB_UTILITIES_POLY
#define DB_UTILITIES_POLY

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMPolynomial (LM) Polynomial utilities (solvers, arithmetic, evaluation, etc.)
 */
/*\{*/

/*!
In debug mode closed form quadratic solving takes on the order of 15 microseconds
while eig of the companion matrix takes about 1.1 milliseconds
Speed-optimized code in release mode solves a quadratic in 0.3 microseconds on 450MHz
*/
inline void db_SolveQuadratic(double *roots,int *nr_roots,double a,double b,double c)
{
    double rs,srs,q;

    /*For non-degenerate quadratics
    [5 mult 2 add 1 sqrt=7flops 1func]*/
    if(a==0.0)
    {
        if(b==0.0) *nr_roots=0;
        else
        {
            roots[0]= -c/b;
            *nr_roots=1;
        }
    }
    else
    {
        rs=b*b-4.0*a*c;
        if(rs>=0.0)
        {
            *nr_roots=2;
            srs=sqrt(rs);
            q= -0.5*(b+db_sign(b)*srs);
            roots[0]=q/a;
            /*If b is zero db_sign(b) returns 1,
            so q is only zero when b=0 and c=0*/
            if(q==0.0) *nr_roots=1;
            else roots[1]=c/q;
        }
        else *nr_roots=0;
    }
}

/*!
In debug mode closed form cubic solving takes on the order of 45 microseconds
while eig of the companion matrix takes about 1.3 milliseconds
Speed-optimized code in release mode solves a cubic in 1.5 microseconds on 450MHz
For a non-degenerate cubic with two roots, the first root is the single root and
the second root is the double root
*/
DB_API void db_SolveCubic(double *roots,int *nr_roots,double a,double b,double c,double d);
/*!
In debug mode closed form quartic solving takes on the order of 0.1 milliseconds
while eig of the companion matrix takes about 1.5 milliseconds
Speed-optimized code in release mode solves a quartic in 2.6 microseconds on 450MHz*/
DB_API void db_SolveQuartic(double *roots,int *nr_roots,double a,double b,double c,double d,double e);
/*!
Quartic solving where a solution is forced when splitting into quadratics, which
can be good if the quartic is sometimes in fact a quadratic, such as in absolute orientation
when the data is planar*/
DB_API void db_SolveQuarticForced(double *roots,int *nr_roots,double a,double b,double c,double d,double e);

inline double db_PolyEval1(const double p[2],double x)
{
    return(p[0]+x*p[1]);
}

inline void db_MultiplyPoly1_1(double *d,const double *a,const double *b)
{
    double a0,a1;
    double b0,b1;
    a0=a[0];a1=a[1];
    b0=b[0];b1=b[1];

    d[0]=a0*b0;
    d[1]=a0*b1+a1*b0;
    d[2]=      a1*b1;
}

inline void db_MultiplyPoly0_2(double *d,const double *a,const double *b)
{
    double a0;
    double b0,b1,b2;
    a0=a[0];
    b0=b[0];b1=b[1];b2=b[2];

    d[0]=a0*b0;
    d[1]=a0*b1;
    d[2]=a0*b2;
}

inline void db_MultiplyPoly1_2(double *d,const double *a,const double *b)
{
    double a0,a1;
    double b0,b1,b2;
    a0=a[0];a1=a[1];
    b0=b[0];b1=b[1];b2=b[2];

    d[0]=a0*b0;
    d[1]=a0*b1+a1*b0;
    d[2]=a0*b2+a1*b1;
    d[3]=      a1*b2;
}


inline void db_MultiplyPoly1_3(double *d,const double *a,const double *b)
{
    double a0,a1;
    double b0,b1,b2,b3;
    a0=a[0];a1=a[1];
    b0=b[0];b1=b[1];b2=b[2];b3=b[3];

    d[0]=a0*b0;
    d[1]=a0*b1+a1*b0;
    d[2]=a0*b2+a1*b1;
    d[3]=a0*b3+a1*b2;
    d[4]=      a1*b3;
}
/*!
Multiply d=a*b where a is one degree and b is two degree*/
inline void db_AddPolyProduct0_1(double *d,const double *a,const double *b)
{
    double a0;
    double b0,b1;
    a0=a[0];
    b0=b[0];b1=b[1];

    d[0]+=a0*b0;
    d[1]+=a0*b1;
}
inline void db_AddPolyProduct0_2(double *d,const double *a,const double *b)
{
    double a0;
    double b0,b1,b2;
    a0=a[0];
    b0=b[0];b1=b[1];b2=b[2];

    d[0]+=a0*b0;
    d[1]+=a0*b1;
    d[2]+=a0*b2;
}
/*!
Multiply d=a*b where a is one degree and b is two degree*/
inline void db_SubtractPolyProduct0_0(double *d,const double *a,const double *b)
{
    double a0;
    double b0;
    a0=a[0];
    b0=b[0];

    d[0]-=a0*b0;
}

inline void db_SubtractPolyProduct0_1(double *d,const double *a,const double *b)
{
    double a0;
    double b0,b1;
    a0=a[0];
    b0=b[0];b1=b[1];

    d[0]-=a0*b0;
    d[1]-=a0*b1;
}

inline void db_SubtractPolyProduct0_2(double *d,const double *a,const double *b)
{
    double a0;
    double b0,b1,b2;
    a0=a[0];
    b0=b[0];b1=b[1];b2=b[2];

    d[0]-=a0*b0;
    d[1]-=a0*b1;
    d[2]-=a0*b2;
}

inline void db_SubtractPolyProduct1_3(double *d,const double *a,const double *b)
{
    double a0,a1;
    double b0,b1,b2,b3;
    a0=a[0];a1=a[1];
    b0=b[0];b1=b[1];b2=b[2];b3=b[3];

    d[0]-=a0*b0;
    d[1]-=a0*b1+a1*b0;
    d[2]-=a0*b2+a1*b1;
    d[3]-=a0*b3+a1*b2;
    d[4]-=      a1*b3;
}

inline void    db_CharacteristicPolynomial4x4(double p[5],const double A[16])
{
    /*All two by two determinants of the first two rows*/
    double two01[3],two02[3],two03[3],two12[3],two13[3],two23[3];
    /*Polynomials representing third and fourth row of A*/
    double P0[2],P1[2],P2[2],P3[2];
    double P4[2],P5[2],P6[2],P7[2];
    /*All three by three determinants of the first three rows*/
    double neg_three0[4],neg_three1[4],three2[4],three3[4];

    /*Compute 2x2 determinants*/
    two01[0]=A[0]*A[5]-A[1]*A[4];
    two01[1]= -(A[0]+A[5]);
    two01[2]=1.0;

    two02[0]=A[0]*A[6]-A[2]*A[4];
    two02[1]= -A[6];

    two03[0]=A[0]*A[7]-A[3]*A[4];
    two03[1]= -A[7];

    two12[0]=A[1]*A[6]-A[2]*A[5];
    two12[1]=A[2];

    two13[0]=A[1]*A[7]-A[3]*A[5];
    two13[1]=A[3];

    two23[0]=A[2]*A[7]-A[3]*A[6];

    P0[0]=A[8];
    P1[0]=A[9];
    P2[0]=A[10];P2[1]= -1.0;
    P3[0]=A[11];

    P4[0]=A[12];
    P5[0]=A[13];
    P6[0]=A[14];
    P7[0]=A[15];P7[1]= -1.0;

    /*Compute 3x3 determinants.Note that the highest
    degree polynomial goes first and the smaller ones
    are added or subtracted from it*/
    db_MultiplyPoly1_1(       neg_three0,P2,two13);
    db_SubtractPolyProduct0_0(neg_three0,P1,two23);
    db_SubtractPolyProduct0_1(neg_three0,P3,two12);

    db_MultiplyPoly1_1(       neg_three1,P2,two03);
    db_SubtractPolyProduct0_1(neg_three1,P3,two02);
    db_SubtractPolyProduct0_0(neg_three1,P0,two23);

    db_MultiplyPoly0_2(       three2,P3,two01);
    db_AddPolyProduct0_1(     three2,P0,two13);
    db_SubtractPolyProduct0_1(three2,P1,two03);

    db_MultiplyPoly1_2(       three3,P2,two01);
    db_AddPolyProduct0_1(     three3,P0,two12);
    db_SubtractPolyProduct0_1(three3,P1,two02);

    /*Compute 4x4 determinants*/
    db_MultiplyPoly1_3(       p,P7,three3);
    db_AddPolyProduct0_2(     p,P4,neg_three0);
    db_SubtractPolyProduct0_2(p,P5,neg_three1);
    db_SubtractPolyProduct0_2(p,P6,three2);
}

inline void db_RealEigenvalues4x4(double lambda[4],int *nr_roots,const double A[16],int forced=0)
{
    double p[5];

    db_CharacteristicPolynomial4x4(p,A);
    if(forced) db_SolveQuarticForced(lambda,nr_roots,p[4],p[3],p[2],p[1],p[0]);
     else db_SolveQuartic(lambda,nr_roots,p[4],p[3],p[2],p[1],p[0]);
}

/*!
Compute the unit norm eigenvector v of the matrix A corresponding
to the eigenvalue lambda
[96mult 60add 1sqrt=156flops 1sqrt]*/
inline void db_EigenVector4x4(double v[4],double lambda,const double A[16])
{
    double a0,a5,a10,a15;
    double d01,d02,d03,d12,d13,d23;
    double e01,e02,e03,e12,e13,e23;
    double C[16],n0,n1,n2,n3,m;

    /*Compute diagonal
    [4add=4flops]*/
    a0=A[0]-lambda;
    a5=A[5]-lambda;
    a10=A[10]-lambda;
    a15=A[15]-lambda;

    /*Compute 2x2 determinants of rows 1,2 and 3,4
    [24mult 12add=36flops]*/
    d01=a0*a5    -A[1]*A[4];
    d02=a0*A[6]  -A[2]*A[4];
    d03=a0*A[7]  -A[3]*A[4];
    d12=A[1]*A[6]-A[2]*a5;
    d13=A[1]*A[7]-A[3]*a5;
    d23=A[2]*A[7]-A[3]*A[6];

    e01=A[8]*A[13]-A[9] *A[12];
    e02=A[8]*A[14]-a10  *A[12];
    e03=A[8]*a15  -A[11]*A[12];
    e12=A[9]*A[14]-a10  *A[13];
    e13=A[9]*a15  -A[11]*A[13];
    e23=a10 *a15  -A[11]*A[14];

    /*Compute matrix of cofactors
    [48mult 32 add=80flops*/
    C[0]=  (a5  *e23-A[6]*e13+A[7]*e12);
    C[1]= -(A[4]*e23-A[6]*e03+A[7]*e02);
    C[2]=  (A[4]*e13-a5  *e03+A[7]*e01);
    C[3]= -(A[4]*e12-a5  *e02+A[6]*e01);

    C[4]= -(A[1]*e23-A[2]*e13+A[3]*e12);
    C[5]=  (a0  *e23-A[2]*e03+A[3]*e02);
    C[6]= -(a0  *e13-A[1]*e03+A[3]*e01);
    C[7]=  (a0  *e12-A[1]*e02+A[2]*e01);

    C[8]=   (A[13]*d23-A[14]*d13+a15  *d12);
    C[9]=  -(A[12]*d23-A[14]*d03+a15  *d02);
    C[10]=  (A[12]*d13-A[13]*d03+a15  *d01);
    C[11]= -(A[12]*d12-A[13]*d02+A[14]*d01);

    C[12]= -(A[9]*d23-a10 *d13+A[11]*d12);
    C[13]=  (A[8]*d23-a10 *d03+A[11]*d02);
    C[14]= -(A[8]*d13-A[9]*d03+A[11]*d01);
    C[15]=  (A[8]*d12-A[9]*d02+a10  *d01);

    /*Compute square sums of rows
    [16mult 12add=28flops*/
    n0=db_sqr(C[0]) +db_sqr(C[1]) +db_sqr(C[2]) +db_sqr(C[3]);
    n1=db_sqr(C[4]) +db_sqr(C[5]) +db_sqr(C[6]) +db_sqr(C[7]);
    n2=db_sqr(C[8]) +db_sqr(C[9]) +db_sqr(C[10])+db_sqr(C[11]);
    n3=db_sqr(C[12])+db_sqr(C[13])+db_sqr(C[14])+db_sqr(C[15]);

    /*Take the largest norm row and normalize
    [4mult 1 sqrt=4flops 1sqrt]*/
    if(n0>=n1 && n0>=n2 && n0>=n3)
    {
        m=db_SafeReciprocal(sqrt(n0));
        db_MultiplyScalarCopy4(v,C,m);
    }
    else if(n1>=n2 && n1>=n3)
    {
        m=db_SafeReciprocal(sqrt(n1));
        db_MultiplyScalarCopy4(v,&(C[4]),m);
    }
    else if(n2>=n3)
    {
        m=db_SafeReciprocal(sqrt(n2));
        db_MultiplyScalarCopy4(v,&(C[8]),m);
    }
    else
    {
        m=db_SafeReciprocal(sqrt(n3));
        db_MultiplyScalarCopy4(v,&(C[12]),m);
    }
}



/*\}*/
#endif /* DB_UTILITIES_POLY */
