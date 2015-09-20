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

/* $Id: db_framestitching.cpp,v 1.2 2011/06/17 14:03:30 mbansal Exp $ */

#include "db_utilities.h"
#include "db_framestitching.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

inline void db_RotationFromMOuterProductSum(double R[9],double *score,double M[9])
{
    double N[16],q[4],lambda[4],lambda_max;
    double y[4];
    int nr_roots;

    N[0]=   M[0]+M[4]+M[8];
    N[5]=   M[0]-M[4]-M[8];
    N[10]= -M[0]+M[4]-M[8];
    N[15]= -M[0]-M[4]+M[8];
    N[1] =N[4] =M[5]-M[7];
    N[2] =N[8] =M[6]-M[2];
    N[3] =N[12]=M[1]-M[3];
    N[6] =N[9] =M[1]+M[3];
    N[7] =N[13]=M[6]+M[2];
    N[11]=N[14]=M[5]+M[7];

    /*get the quaternion representing the rotation
    by finding the eigenvector corresponding to the most
    positive eigenvalue. Force eigenvalue solutions, since the matrix
    is symmetric and solutions might otherwise be lost
    when the data is planar*/
    db_RealEigenvalues4x4(lambda,&nr_roots,N,1);
    if(nr_roots)
    {
        lambda_max=lambda[0];
        if(nr_roots>=2)
        {
            if(lambda[1]>lambda_max) lambda_max=lambda[1];
            if(nr_roots>=3)
            {
                if(lambda[2]>lambda_max) lambda_max=lambda[2];
                {
                    if(nr_roots>=4) if(lambda[3]>lambda_max) lambda_max=lambda[3];
                }
            }
        }
    }
    else lambda_max=1.0;
    db_EigenVector4x4(q,lambda_max,N);

    /*Compute the rotation matrix*/
    db_QuaternionToRotation(R,q);

    if(score)
    {
        /*Compute score=transpose(q)*N*q */
        db_Multiply4x4_4x1(y,N,q);
        *score=db_ScalarProduct4(q,y);
    }
}

void db_StitchSimilarity3DRaw(double *scale,double R[9],double t[3],
                            double **Xp,double **X,int nr_points,int orientation_preserving,
                            int allow_scaling,int allow_rotation,int allow_translation)
{
    int i;
    double c[3],cp[3],r[3],rp[3],M[9],s,sp,sc;
    double Rr[9],score_p,score_r;
    double *temp,*temp_p;

    if(allow_translation)
    {
        db_PointCentroid3D(c,X,nr_points);
        db_PointCentroid3D(cp,Xp,nr_points);
    }
    else
    {
        db_Zero3(c);
        db_Zero3(cp);
    }

    db_Zero9(M);
    s=sp=0;
    for(i=0;i<nr_points;i++)
    {
        temp=   *X++;
        temp_p= *Xp++;
        r[0]=(*temp++)-c[0];
        r[1]=(*temp++)-c[1];
        r[2]=(*temp++)-c[2];
        rp[0]=(*temp_p++)-cp[0];
        rp[1]=(*temp_p++)-cp[1];
        rp[2]=(*temp_p++)-cp[2];

        M[0]+=r[0]*rp[0];
        M[1]+=r[0]*rp[1];
        M[2]+=r[0]*rp[2];
        M[3]+=r[1]*rp[0];
        M[4]+=r[1]*rp[1];
        M[5]+=r[1]*rp[2];
        M[6]+=r[2]*rp[0];
        M[7]+=r[2]*rp[1];
        M[8]+=r[2]*rp[2];

        s+=db_sqr(r[0])+db_sqr(r[1])+db_sqr(r[2]);
        sp+=db_sqr(rp[0])+db_sqr(rp[1])+db_sqr(rp[2]);
    }

    /*Compute scale*/
    if(allow_scaling) sc=sqrt(db_SafeDivision(sp,s));
    else sc=1.0;
    *scale=sc;

    /*Compute rotation*/
    if(allow_rotation)
    {
        if(orientation_preserving)
        {
            db_RotationFromMOuterProductSum(R,0,M);
        }
        else
        {
            /*Try preserving*/
            db_RotationFromMOuterProductSum(R,&score_p,M);
            /*Try reversing*/
            M[6]= -M[6];
            M[7]= -M[7];
            M[8]= -M[8];
            db_RotationFromMOuterProductSum(Rr,&score_r,M);
            if(score_r>score_p)
            {
                /*Reverse is better*/
                R[0]=Rr[0]; R[1]=Rr[1]; R[2]= -Rr[2];
                R[3]=Rr[3]; R[4]=Rr[4]; R[5]= -Rr[5];
                R[6]=Rr[6]; R[7]=Rr[7]; R[8]= -Rr[8];
            }
        }
    }
    else db_Identity3x3(R);

    /*Compute translation*/
    if(allow_translation)
    {
        t[0]=cp[0]-sc*(R[0]*c[0]+R[1]*c[1]+R[2]*c[2]);
        t[1]=cp[1]-sc*(R[3]*c[0]+R[4]*c[1]+R[5]*c[2]);
        t[2]=cp[2]-sc*(R[6]*c[0]+R[7]*c[1]+R[8]*c[2]);
    }
    else db_Zero3(t);
}


