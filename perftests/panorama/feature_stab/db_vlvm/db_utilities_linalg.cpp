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

/* $Id: db_utilities_linalg.cpp,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#include "db_utilities_linalg.h"
#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

/*Cholesky-factorize symmetric positive definite 6 x 6 matrix A. Upper
part of A is used from the input. The Cholesky factor is output as
subdiagonal part in A and diagonal in d, which is 6-dimensional*/
void db_CholeskyDecomp6x6(double A[36],double d[6])
{
    double s,temp;

    /*[50 mult 35 add 6sqrt=85flops 6func]*/
    /*i=0*/
    s=A[0];
    d[0]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[0]);
    A[6]=A[1]*temp;
    A[12]=A[2]*temp;
    A[18]=A[3]*temp;
    A[24]=A[4]*temp;
    A[30]=A[5]*temp;
    /*i=1*/
    s=A[7]-A[6]*A[6];
    d[1]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[1]);
    A[13]=(A[8]-A[6]*A[12])*temp;
    A[19]=(A[9]-A[6]*A[18])*temp;
    A[25]=(A[10]-A[6]*A[24])*temp;
    A[31]=(A[11]-A[6]*A[30])*temp;
    /*i=2*/
    s=A[14]-A[12]*A[12]-A[13]*A[13];
    d[2]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[2]);
    A[20]=(A[15]-A[12]*A[18]-A[13]*A[19])*temp;
    A[26]=(A[16]-A[12]*A[24]-A[13]*A[25])*temp;
    A[32]=(A[17]-A[12]*A[30]-A[13]*A[31])*temp;
    /*i=3*/
    s=A[21]-A[18]*A[18]-A[19]*A[19]-A[20]*A[20];
    d[3]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[3]);
    A[27]=(A[22]-A[18]*A[24]-A[19]*A[25]-A[20]*A[26])*temp;
    A[33]=(A[23]-A[18]*A[30]-A[19]*A[31]-A[20]*A[32])*temp;
    /*i=4*/
    s=A[28]-A[24]*A[24]-A[25]*A[25]-A[26]*A[26]-A[27]*A[27];
    d[4]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[4]);
    A[34]=(A[29]-A[24]*A[30]-A[25]*A[31]-A[26]*A[32]-A[27]*A[33])*temp;
    /*i=5*/
    s=A[35]-A[30]*A[30]-A[31]*A[31]-A[32]*A[32]-A[33]*A[33]-A[34]*A[34];
    d[5]=((s>0.0)?sqrt(s):1.0);
}

/*Cholesky-factorize symmetric positive definite n x n matrix A.Part
above diagonal of A is used from the input, diagonal of A is assumed to
be stored in d. The Cholesky factor is output as
subdiagonal part in A and diagonal in d, which is n-dimensional*/
void db_CholeskyDecompSeparateDiagonal(double **A,double *d,int n)
{
    int i,j,k;
    double s;
    double temp = 0.0;

    for(i=0;i<n;i++) for(j=i;j<n;j++)
    {
        if(i==j) s=d[i];
        else s=A[i][j];
        for(k=i-1;k>=0;k--) s-=A[i][k]*A[j][k];
        if(i==j)
        {
            d[i]=((s>0.0)?sqrt(s):1.0);
            temp=db_SafeReciprocal(d[i]);
        }
        else A[j][i]=s*temp;
    }
}

/*Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of an n x n matrix and the right hand side b. The vector b is unchanged*/
void db_CholeskyBacksub(double *x,const double * const *A,const double *d,int n,const double *b)
{
    int i,k;
    double s;

    for(i=0;i<n;i++)
    {
        for(s=b[i],k=i-1;k>=0;k--) s-=A[i][k]*x[k];
        x[i]=db_SafeDivision(s,d[i]);
    }
    for(i=n-1;i>=0;i--)
    {
        for(s=x[i],k=i+1;k<n;k++) s-=A[k][i]*x[k];
        x[i]=db_SafeDivision(s,d[i]);
    }
}

/*Cholesky-factorize symmetric positive definite 3 x 3 matrix A. Part
above diagonal of A is used from the input, diagonal of A is assumed to
be stored in d. The Cholesky factor is output as subdiagonal part in A
and diagonal in d, which is 3-dimensional*/
void db_CholeskyDecomp3x3SeparateDiagonal(double A[9],double d[3])
{
    double s,temp;

    /*i=0*/
    s=d[0];
    d[0]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[0]);
    A[3]=A[1]*temp;
    A[6]=A[2]*temp;
    /*i=1*/
    s=d[1]-A[3]*A[3];
    d[1]=((s>0.0)?sqrt(s):1.0);
    temp=db_SafeReciprocal(d[1]);
    A[7]=(A[5]-A[3]*A[6])*temp;
    /*i=2*/
    s=d[2]-A[6]*A[6]-A[7]*A[7];
    d[2]=((s>0.0)?sqrt(s):1.0);
}

/*Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of a 3 x 3 matrix and the right hand side b. The vector b is unchanged*/
void db_CholeskyBacksub3x3(double x[3],const double A[9],const double d[3],const double b[3])
{
    /*[42 mult 30 add=72flops]*/
    x[0]=db_SafeDivision(b[0],d[0]);
    x[1]=db_SafeDivision((b[1]-A[3]*x[0]),d[1]);
    x[2]=db_SafeDivision((b[2]-A[6]*x[0]-A[7]*x[1]),d[2]);
    x[2]=db_SafeDivision(x[2],d[2]);
    x[1]=db_SafeDivision((x[1]-A[7]*x[2]),d[1]);
    x[0]=db_SafeDivision((x[0]-A[6]*x[2]-A[3]*x[1]),d[0]);
}

/*Backsubstitute L%transpose(L)*x=b for x given the Cholesky decomposition
of a 6 x 6 matrix and the right hand side b. The vector b is unchanged*/
void db_CholeskyBacksub6x6(double x[6],const double A[36],const double d[6],const double b[6])
{
    /*[42 mult 30 add=72flops]*/
    x[0]=db_SafeDivision(b[0],d[0]);
    x[1]=db_SafeDivision((b[1]-A[6]*x[0]),d[1]);
    x[2]=db_SafeDivision((b[2]-A[12]*x[0]-A[13]*x[1]),d[2]);
    x[3]=db_SafeDivision((b[3]-A[18]*x[0]-A[19]*x[1]-A[20]*x[2]),d[3]);
    x[4]=db_SafeDivision((b[4]-A[24]*x[0]-A[25]*x[1]-A[26]*x[2]-A[27]*x[3]),d[4]);
    x[5]=db_SafeDivision((b[5]-A[30]*x[0]-A[31]*x[1]-A[32]*x[2]-A[33]*x[3]-A[34]*x[4]),d[5]);
    x[5]=db_SafeDivision(x[5],d[5]);
    x[4]=db_SafeDivision((x[4]-A[34]*x[5]),d[4]);
    x[3]=db_SafeDivision((x[3]-A[33]*x[5]-A[27]*x[4]),d[3]);
    x[2]=db_SafeDivision((x[2]-A[32]*x[5]-A[26]*x[4]-A[20]*x[3]),d[2]);
    x[1]=db_SafeDivision((x[1]-A[31]*x[5]-A[25]*x[4]-A[19]*x[3]-A[13]*x[2]),d[1]);
    x[0]=db_SafeDivision((x[0]-A[30]*x[5]-A[24]*x[4]-A[18]*x[3]-A[12]*x[2]-A[6]*x[1]),d[0]);
}


void db_Orthogonalize6x7(double A[42],int orthonormalize)
{
    int i;
    double ss[6];

    /*Compute square sums of rows*/
    ss[0]=db_SquareSum7(A);
    ss[1]=db_SquareSum7(A+7);
    ss[2]=db_SquareSum7(A+14);
    ss[3]=db_SquareSum7(A+21);
    ss[4]=db_SquareSum7(A+28);
    ss[5]=db_SquareSum7(A+35);

    ss[1]-=db_OrthogonalizePair7(A+7 ,A,ss[0]);
    ss[2]-=db_OrthogonalizePair7(A+14,A,ss[0]);
    ss[3]-=db_OrthogonalizePair7(A+21,A,ss[0]);
    ss[4]-=db_OrthogonalizePair7(A+28,A,ss[0]);
    ss[5]-=db_OrthogonalizePair7(A+35,A,ss[0]);

    /*Pivot on largest ss (could also be done on ss/(original_ss))*/
    i=db_MaxIndex5(ss+1);
    db_OrthogonalizationSwap7(A+7,i,ss+1);

    ss[2]-=db_OrthogonalizePair7(A+14,A+7,ss[1]);
    ss[3]-=db_OrthogonalizePair7(A+21,A+7,ss[1]);
    ss[4]-=db_OrthogonalizePair7(A+28,A+7,ss[1]);
    ss[5]-=db_OrthogonalizePair7(A+35,A+7,ss[1]);

    i=db_MaxIndex4(ss+2);
    db_OrthogonalizationSwap7(A+14,i,ss+2);

    ss[3]-=db_OrthogonalizePair7(A+21,A+14,ss[2]);
    ss[4]-=db_OrthogonalizePair7(A+28,A+14,ss[2]);
    ss[5]-=db_OrthogonalizePair7(A+35,A+14,ss[2]);

    i=db_MaxIndex3(ss+3);
    db_OrthogonalizationSwap7(A+21,i,ss+3);

    ss[4]-=db_OrthogonalizePair7(A+28,A+21,ss[3]);
    ss[5]-=db_OrthogonalizePair7(A+35,A+21,ss[3]);

    i=db_MaxIndex2(ss+4);
    db_OrthogonalizationSwap7(A+28,i,ss+4);

    ss[5]-=db_OrthogonalizePair7(A+35,A+28,ss[4]);

    if(orthonormalize)
    {
        db_MultiplyScalar7(A   ,db_SafeSqrtReciprocal(ss[0]));
        db_MultiplyScalar7(A+7 ,db_SafeSqrtReciprocal(ss[1]));
        db_MultiplyScalar7(A+14,db_SafeSqrtReciprocal(ss[2]));
        db_MultiplyScalar7(A+21,db_SafeSqrtReciprocal(ss[3]));
        db_MultiplyScalar7(A+28,db_SafeSqrtReciprocal(ss[4]));
        db_MultiplyScalar7(A+35,db_SafeSqrtReciprocal(ss[5]));
    }
}

void db_Orthogonalize8x9(double A[72],int orthonormalize)
{
    int i;
    double ss[8];

    /*Compute square sums of rows*/
    ss[0]=db_SquareSum9(A);
    ss[1]=db_SquareSum9(A+9);
    ss[2]=db_SquareSum9(A+18);
    ss[3]=db_SquareSum9(A+27);
    ss[4]=db_SquareSum9(A+36);
    ss[5]=db_SquareSum9(A+45);
    ss[6]=db_SquareSum9(A+54);
    ss[7]=db_SquareSum9(A+63);

    ss[1]-=db_OrthogonalizePair9(A+9 ,A,ss[0]);
    ss[2]-=db_OrthogonalizePair9(A+18,A,ss[0]);
    ss[3]-=db_OrthogonalizePair9(A+27,A,ss[0]);
    ss[4]-=db_OrthogonalizePair9(A+36,A,ss[0]);
    ss[5]-=db_OrthogonalizePair9(A+45,A,ss[0]);
    ss[6]-=db_OrthogonalizePair9(A+54,A,ss[0]);
    ss[7]-=db_OrthogonalizePair9(A+63,A,ss[0]);

    /*Pivot on largest ss (could also be done on ss/(original_ss))*/
    i=db_MaxIndex7(ss+1);
    db_OrthogonalizationSwap9(A+9,i,ss+1);

    ss[2]-=db_OrthogonalizePair9(A+18,A+9,ss[1]);
    ss[3]-=db_OrthogonalizePair9(A+27,A+9,ss[1]);
    ss[4]-=db_OrthogonalizePair9(A+36,A+9,ss[1]);
    ss[5]-=db_OrthogonalizePair9(A+45,A+9,ss[1]);
    ss[6]-=db_OrthogonalizePair9(A+54,A+9,ss[1]);
    ss[7]-=db_OrthogonalizePair9(A+63,A+9,ss[1]);

    i=db_MaxIndex6(ss+2);
    db_OrthogonalizationSwap9(A+18,i,ss+2);

    ss[3]-=db_OrthogonalizePair9(A+27,A+18,ss[2]);
    ss[4]-=db_OrthogonalizePair9(A+36,A+18,ss[2]);
    ss[5]-=db_OrthogonalizePair9(A+45,A+18,ss[2]);
    ss[6]-=db_OrthogonalizePair9(A+54,A+18,ss[2]);
    ss[7]-=db_OrthogonalizePair9(A+63,A+18,ss[2]);

    i=db_MaxIndex5(ss+3);
    db_OrthogonalizationSwap9(A+27,i,ss+3);

    ss[4]-=db_OrthogonalizePair9(A+36,A+27,ss[3]);
    ss[5]-=db_OrthogonalizePair9(A+45,A+27,ss[3]);
    ss[6]-=db_OrthogonalizePair9(A+54,A+27,ss[3]);
    ss[7]-=db_OrthogonalizePair9(A+63,A+27,ss[3]);

    i=db_MaxIndex4(ss+4);
    db_OrthogonalizationSwap9(A+36,i,ss+4);

    ss[5]-=db_OrthogonalizePair9(A+45,A+36,ss[4]);
    ss[6]-=db_OrthogonalizePair9(A+54,A+36,ss[4]);
    ss[7]-=db_OrthogonalizePair9(A+63,A+36,ss[4]);

    i=db_MaxIndex3(ss+5);
    db_OrthogonalizationSwap9(A+45,i,ss+5);

    ss[6]-=db_OrthogonalizePair9(A+54,A+45,ss[5]);
    ss[7]-=db_OrthogonalizePair9(A+63,A+45,ss[5]);

    i=db_MaxIndex2(ss+6);
    db_OrthogonalizationSwap9(A+54,i,ss+6);

    ss[7]-=db_OrthogonalizePair9(A+63,A+54,ss[6]);

    if(orthonormalize)
    {
        db_MultiplyScalar9(A   ,db_SafeSqrtReciprocal(ss[0]));
        db_MultiplyScalar9(A+9 ,db_SafeSqrtReciprocal(ss[1]));
        db_MultiplyScalar9(A+18,db_SafeSqrtReciprocal(ss[2]));
        db_MultiplyScalar9(A+27,db_SafeSqrtReciprocal(ss[3]));
        db_MultiplyScalar9(A+36,db_SafeSqrtReciprocal(ss[4]));
        db_MultiplyScalar9(A+45,db_SafeSqrtReciprocal(ss[5]));
        db_MultiplyScalar9(A+54,db_SafeSqrtReciprocal(ss[6]));
        db_MultiplyScalar9(A+63,db_SafeSqrtReciprocal(ss[7]));
    }
}

void db_NullVectorOrthonormal6x7(double x[7],const double A[42])
{
    int i;
    double omss[7];
    const double *B;

    /*Pivot by choosing row of the identity matrix
    (the one corresponding to column of A with smallest square sum)*/
    omss[0]=db_SquareSum6Stride7(A);
    omss[1]=db_SquareSum6Stride7(A+1);
    omss[2]=db_SquareSum6Stride7(A+2);
    omss[3]=db_SquareSum6Stride7(A+3);
    omss[4]=db_SquareSum6Stride7(A+4);
    omss[5]=db_SquareSum6Stride7(A+5);
    omss[6]=db_SquareSum6Stride7(A+6);
    i=db_MinIndex7(omss);
    /*orthogonalize that row against all previous rows
    and normalize it*/
    B=A+i;
    db_MultiplyScalarCopy7(x,A,-B[0]);
    db_RowOperation7(x,A+7 ,B[7]);
    db_RowOperation7(x,A+14,B[14]);
    db_RowOperation7(x,A+21,B[21]);
    db_RowOperation7(x,A+28,B[28]);
    db_RowOperation7(x,A+35,B[35]);
    x[i]+=1.0;
    db_MultiplyScalar7(x,db_SafeSqrtReciprocal(1.0-omss[i]));
}

void db_NullVectorOrthonormal8x9(double x[9],const double A[72])
{
    int i;
    double omss[9];
    const double *B;

    /*Pivot by choosing row of the identity matrix
    (the one corresponding to column of A with smallest square sum)*/
    omss[0]=db_SquareSum8Stride9(A);
    omss[1]=db_SquareSum8Stride9(A+1);
    omss[2]=db_SquareSum8Stride9(A+2);
    omss[3]=db_SquareSum8Stride9(A+3);
    omss[4]=db_SquareSum8Stride9(A+4);
    omss[5]=db_SquareSum8Stride9(A+5);
    omss[6]=db_SquareSum8Stride9(A+6);
    omss[7]=db_SquareSum8Stride9(A+7);
    omss[8]=db_SquareSum8Stride9(A+8);
    i=db_MinIndex9(omss);
    /*orthogonalize that row against all previous rows
    and normalize it*/
    B=A+i;
    db_MultiplyScalarCopy9(x,A,-B[0]);
    db_RowOperation9(x,A+9 ,B[9]);
    db_RowOperation9(x,A+18,B[18]);
    db_RowOperation9(x,A+27,B[27]);
    db_RowOperation9(x,A+36,B[36]);
    db_RowOperation9(x,A+45,B[45]);
    db_RowOperation9(x,A+54,B[54]);
    db_RowOperation9(x,A+63,B[63]);
    x[i]+=1.0;
    db_MultiplyScalar9(x,db_SafeSqrtReciprocal(1.0-omss[i]));
}

