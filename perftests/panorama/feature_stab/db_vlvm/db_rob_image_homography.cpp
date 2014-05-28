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

/* $Id: db_rob_image_homography.cpp,v 1.2 2011/06/17 14:03:31 mbansal Exp $ */

#include "db_utilities.h"
#include "db_rob_image_homography.h"
#include "db_bundle.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

#include "db_image_homography.h"

#ifdef _VERBOSE_
#include <iostream>
using namespace std;
#endif /*VERBOSE*/

inline double db_RobImageHomography_Cost(double H[9],int point_count,double *x_i,double *xp_i,double one_over_scale2)
{
    int c;
    double back,acc,*x_i_temp,*xp_i_temp;

    for(back=0.0,c=0;c<point_count;)
    {
        /*Take log of product of ten reprojection
        errors to reduce nr of expensive log operations*/
        if(c+9<point_count)
        {
            x_i_temp=x_i+(c<<1);
            xp_i_temp=xp_i+(c<<1);

            acc=db_ExpCauchyInhomogenousHomographyError(xp_i_temp,H,x_i_temp,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+2,H,x_i_temp+2,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+4,H,x_i_temp+4,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+6,H,x_i_temp+6,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+8,H,x_i_temp+8,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+10,H,x_i_temp+10,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+12,H,x_i_temp+12,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+14,H,x_i_temp+14,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+16,H,x_i_temp+16,one_over_scale2);
            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+18,H,x_i_temp+18,one_over_scale2);
            c+=10;
        }
        else
        {
            for(acc=1.0;c<point_count;c++)
            {
                acc*=db_ExpCauchyInhomogenousHomographyError(xp_i+(c<<1),H,x_i+(c<<1),one_over_scale2);
            }
        }
        back+=log(acc);
    }
    return(back);
}

inline double db_RobImageHomography_Statistics(double H[9],int point_count,double *x_i,double *xp_i,double one_over_scale2,db_Statistics *stat,double thresh=DB_OUTLIER_THRESHOLD)
{
    int c,i;
    double t2,frac;

    t2=thresh*thresh;
    for(i=0,c=0;c<point_count;c++)
    {
        i+=(db_SquaredInhomogenousHomographyError(xp_i+(c<<1),H,x_i+(c<<1))*one_over_scale2<=t2)?1:0;
    }
    frac=((double)i)/((double)(db_maxi(point_count,1)));

#ifdef _VERBOSE_
    std::cout << "Inlier Percentage RobImageHomography: " << frac*100.0 << "% out of " << point_count << " constraints" << std::endl;
#endif /*_VERBOSE_*/

    if(stat)
    {
        stat->nr_points=point_count;
        stat->one_over_scale2=one_over_scale2;
        stat->nr_inliers=i;
        stat->inlier_fraction=frac;

        stat->cost=db_RobImageHomography_Cost(H,point_count,x_i,xp_i,one_over_scale2);
        stat->model_dimension=0;
        /*stat->nr_parameters=;*/

        stat->lambda1=log(4.0);
        stat->lambda2=log(4.0*((double)db_maxi(1,stat->nr_points)));
        stat->lambda3=10.0;
        stat->gric=stat->cost+stat->lambda1*stat->model_dimension*((double)stat->nr_points)+stat->lambda2*((double)stat->nr_parameters);
        stat->inlier_evidence=((double)stat->nr_inliers)-stat->lambda3*((double)stat->nr_parameters);
    }

    return(frac);
}

/*Compute min_Jtf and upper right of JtJ. Return cost.*/
inline double db_RobImageHomography_Jacobians(double JtJ[81],double min_Jtf[9],double H[9],int point_count,double *x_i,double *xp_i,double one_over_scale2)
{
    double back,Jf_dx[18],f[2],temp,temp2;
    int i;

    db_Zero(JtJ,81);
    db_Zero(min_Jtf,9);
    for(back=0.0,i=0;i<point_count;i++)
    {
        /*Compute reprojection error vector and its Jacobian
        for this point*/
        db_DerivativeCauchyInhomHomographyReprojection(Jf_dx,f,xp_i+(i<<1),H,x_i+(i<<1),one_over_scale2);
        /*Perform
        min_Jtf-=Jf_dx*f[0] and
        min_Jtf-=(Jf_dx+9)*f[1] to accumulate -Jt%f*/
        db_RowOperation9(min_Jtf,Jf_dx,f[0]);
        db_RowOperation9(min_Jtf,Jf_dx+9,f[1]);
        /*Accumulate upper right of JtJ with outer product*/
        temp=Jf_dx[0]; temp2=Jf_dx[9];
        JtJ[0]+=temp*Jf_dx[0]+temp2*Jf_dx[9];
        JtJ[1]+=temp*Jf_dx[1]+temp2*Jf_dx[10];
        JtJ[2]+=temp*Jf_dx[2]+temp2*Jf_dx[11];
        JtJ[3]+=temp*Jf_dx[3]+temp2*Jf_dx[12];
        JtJ[4]+=temp*Jf_dx[4]+temp2*Jf_dx[13];
        JtJ[5]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[6]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[7]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[8]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[1]; temp2=Jf_dx[10];
        JtJ[10]+=temp*Jf_dx[1]+temp2*Jf_dx[10];
        JtJ[11]+=temp*Jf_dx[2]+temp2*Jf_dx[11];
        JtJ[12]+=temp*Jf_dx[3]+temp2*Jf_dx[12];
        JtJ[13]+=temp*Jf_dx[4]+temp2*Jf_dx[13];
        JtJ[14]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[15]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[16]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[17]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[2]; temp2=Jf_dx[11];
        JtJ[20]+=temp*Jf_dx[2]+temp2*Jf_dx[11];
        JtJ[21]+=temp*Jf_dx[3]+temp2*Jf_dx[12];
        JtJ[22]+=temp*Jf_dx[4]+temp2*Jf_dx[13];
        JtJ[23]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[24]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[25]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[26]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[3]; temp2=Jf_dx[12];
        JtJ[30]+=temp*Jf_dx[3]+temp2*Jf_dx[12];
        JtJ[31]+=temp*Jf_dx[4]+temp2*Jf_dx[13];
        JtJ[32]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[33]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[34]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[35]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[4]; temp2=Jf_dx[13];
        JtJ[40]+=temp*Jf_dx[4]+temp2*Jf_dx[13];
        JtJ[41]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[42]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[43]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[44]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[5]; temp2=Jf_dx[14];
        JtJ[50]+=temp*Jf_dx[5]+temp2*Jf_dx[14];
        JtJ[51]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[52]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[53]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[6]; temp2=Jf_dx[15];
        JtJ[60]+=temp*Jf_dx[6]+temp2*Jf_dx[15];
        JtJ[61]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[62]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[7]; temp2=Jf_dx[16];
        JtJ[70]+=temp*Jf_dx[7]+temp2*Jf_dx[16];
        JtJ[71]+=temp*Jf_dx[8]+temp2*Jf_dx[17];
        temp=Jf_dx[8]; temp2=Jf_dx[17];
        JtJ[80]+=temp*Jf_dx[8]+temp2*Jf_dx[17];

        /*Add square-sum to cost*/
        back+=db_sqr(f[0])+db_sqr(f[1]);
    }

    return(back);
}

/*Compute min_Jtf and upper right of JtJ. Return cost*/
inline double db_RobCamRotation_Jacobians(double JtJ[9],double min_Jtf[3],double H[9],int point_count,double *x_i,double *xp_i,double one_over_scale2)
{
    double back,Jf_dx[6],f[2];
    int i,j;

    db_Zero(JtJ,9);
    db_Zero(min_Jtf,3);
    for(back=0.0,i=0;i<point_count;i++)
    {
        /*Compute reprojection error vector and its Jacobian
        for this point*/
        j=(i<<1);
        db_DerivativeCauchyInhomRotationReprojection(Jf_dx,f,xp_i+j,H,x_i+j,one_over_scale2);
        /*Perform
        min_Jtf-=Jf_dx*f[0] and
        min_Jtf-=(Jf_dx+3)*f[1] to accumulate -Jt%f*/
        db_RowOperation3(min_Jtf,Jf_dx,f[0]);
        db_RowOperation3(min_Jtf,Jf_dx+3,f[1]);
        /*Accumulate upper right of JtJ with outer product*/
        JtJ[0]+=Jf_dx[0]*Jf_dx[0]+Jf_dx[3]*Jf_dx[3];
        JtJ[1]+=Jf_dx[0]*Jf_dx[1]+Jf_dx[3]*Jf_dx[4];
        JtJ[2]+=Jf_dx[0]*Jf_dx[2]+Jf_dx[3]*Jf_dx[5];
        JtJ[4]+=Jf_dx[1]*Jf_dx[1]+Jf_dx[4]*Jf_dx[4];
        JtJ[5]+=Jf_dx[1]*Jf_dx[2]+Jf_dx[4]*Jf_dx[5];
        JtJ[8]+=Jf_dx[2]*Jf_dx[2]+Jf_dx[5]*Jf_dx[5];

        /*Add square-sum to cost*/
        back+=db_sqr(f[0])+db_sqr(f[1]);
    }

    return(back);
}

void db_RobCamRotation_Polish(double H[9],int point_count,double *x_i,double *xp_i,double one_over_scale2,
                               int max_iterations,double improvement_requirement)
{
    int i,update,stop;
    double lambda,cost,current_cost;
    double JtJ[9],min_Jtf[3],dx[3],H_p_dx[9];

    lambda=0.001;
    for(update=1,stop=0,i=0;(stop<2) && (i<max_iterations);i++)
    {
        /*if first time since improvement, compute Jacobian and residual*/
        if(update)
        {
            current_cost=db_RobCamRotation_Jacobians(JtJ,min_Jtf,H,point_count,x_i,xp_i,one_over_scale2);
            update=0;
        }

#ifdef _VERBOSE_
        /*std::cout << "Cost:" << current_cost << " ";*/
#endif /*_VERBOSE_*/

        /*Come up with a hypothesis dx
        based on the current lambda*/
        db_Compute_dx_3x3(dx,JtJ,min_Jtf,lambda);

        /*Compute Cost(x+dx)*/
        db_UpdateRotation(H_p_dx,H,dx);
        cost=db_RobImageHomography_Cost(H_p_dx,point_count,x_i,xp_i,one_over_scale2);

        /*Is there an improvement?*/
        if(cost<current_cost)
        {
            /*improvement*/
            if(current_cost-cost<current_cost*improvement_requirement) stop++;
            else stop=0;
            lambda*=0.1;
            /*Move to the hypothesised position x+dx*/
            current_cost=cost;
            db_Copy9(H,H_p_dx);
            db_OrthonormalizeRotation(H);
            update=1;

#ifdef _VERBOSE_
        std::cout << "Step" << i << "Imp,Lambda=" << lambda << "Cost:" << current_cost << std::endl;
#endif /*_VERBOSE_*/
        }
        else
        {
            /*no improvement*/
            lambda*=10.0;
            stop=0;
        }
    }
}

inline void db_RobImageHomographyFetchJacobian(double **JtJ_ref,double *min_Jtf,double **JtJ_temp_ref,double *min_Jtf_temp,int n,int *fetch_vector)
{
    int i,j,t;
    double *t1,*t2;

    for(i=0;i<n;i++)
    {
        t=fetch_vector[i];
        min_Jtf[i]=min_Jtf_temp[t];
        t1=JtJ_ref[i];
        t2=JtJ_temp_ref[t];
        for(j=i;j<n;j++)
        {
            t1[j]=t2[fetch_vector[j]];
        }
    }
}

inline void db_RobImageHomographyMultiplyJacobian(double **JtJ_ref,double *min_Jtf,double **JtJ_temp_ref,double *min_Jtf_temp,double **JE_dx_ref,int n)
{
    double JtJ_JE[72],*JtJ_JE_ref[9];

    db_SetupMatrixRefs(JtJ_JE_ref,9,8,JtJ_JE);

    db_SymmetricExtendUpperToLower(JtJ_temp_ref,9,9);
    db_MultiplyMatricesAB(JtJ_JE_ref,JtJ_temp_ref,JE_dx_ref,9,9,n);
    db_UpperMultiplyMatricesAtB(JtJ_ref,JE_dx_ref,JtJ_JE_ref,n,9,n);
    db_MultiplyMatrixVectorAtb(min_Jtf,JE_dx_ref,min_Jtf_temp,n,9);
}

inline void db_RobImageHomographyJH_Js(double **JE_dx_ref,int j,double H[9])
{
    /*Update of upper 2x2 is multiplication by
    [s 0][ cos(theta) sin(theta)]
    [0 s][-sin(theta) cos(theta)]*/
    JE_dx_ref[0][j]=H[0];
    JE_dx_ref[1][j]=H[1];
    JE_dx_ref[2][j]=0;
    JE_dx_ref[3][j]=H[2];
    JE_dx_ref[4][j]=H[3];
    JE_dx_ref[5][j]=0;
    JE_dx_ref[6][j]=0;
    JE_dx_ref[7][j]=0;
    JE_dx_ref[8][j]=0;
}

inline void db_RobImageHomographyJH_JR(double **JE_dx_ref,int j,double H[9])
{
    /*Update of upper 2x2 is multiplication by
    [s 0][ cos(theta) sin(theta)]
    [0 s][-sin(theta) cos(theta)]*/
    JE_dx_ref[0][j]=  H[3];
    JE_dx_ref[1][j]=  H[4];
    JE_dx_ref[2][j]=0;
    JE_dx_ref[3][j]= -H[0];
    JE_dx_ref[4][j]= -H[1];
    JE_dx_ref[5][j]=0;
    JE_dx_ref[6][j]=0;
    JE_dx_ref[7][j]=0;
    JE_dx_ref[8][j]=0;
}

inline void db_RobImageHomographyJH_Jt(double **JE_dx_ref,int j,int k,double H[9])
{
    JE_dx_ref[0][j]=0;
    JE_dx_ref[1][j]=0;
    JE_dx_ref[2][j]=1.0;
    JE_dx_ref[3][j]=0;
    JE_dx_ref[4][j]=0;
    JE_dx_ref[5][j]=0;
    JE_dx_ref[6][j]=0;
    JE_dx_ref[7][j]=0;
    JE_dx_ref[8][j]=0;

    JE_dx_ref[0][k]=0;
    JE_dx_ref[1][k]=0;
    JE_dx_ref[2][k]=0;
    JE_dx_ref[3][k]=0;
    JE_dx_ref[4][k]=0;
    JE_dx_ref[5][k]=1.0;
    JE_dx_ref[6][k]=0;
    JE_dx_ref[7][k]=0;
    JE_dx_ref[8][k]=0;
}

inline void db_RobImageHomographyJH_dRotFocal(double **JE_dx_ref,int j,int k,int l,int m,double H[9])
{
    double f,fi,fi2;
    double R[9],J[9];

    /*Updated matrix is diag(f+df,f+df)*dR*R*diag(1/(f+df),1/(f+df),1)*/
    f=db_FocalAndRotFromCamRotFocalHomography(R,H);
    fi=db_SafeReciprocal(f);
    fi2=db_sqr(fi);
    db_JacobianOfRotatedPointStride(J,R,3);
    JE_dx_ref[0][j]=   J[0];
    JE_dx_ref[1][j]=   J[1];
    JE_dx_ref[2][j]=f* J[2];
    JE_dx_ref[3][j]=   J[3];
    JE_dx_ref[4][j]=   J[4];
    JE_dx_ref[5][j]=f* J[5];
    JE_dx_ref[6][j]=fi*J[6];
    JE_dx_ref[7][j]=fi*J[7];
    JE_dx_ref[8][j]=   J[8];
    db_JacobianOfRotatedPointStride(J,R+1,3);
    JE_dx_ref[0][k]=   J[0];
    JE_dx_ref[1][k]=   J[1];
    JE_dx_ref[2][k]=f* J[2];
    JE_dx_ref[3][k]=   J[3];
    JE_dx_ref[4][k]=   J[4];
    JE_dx_ref[5][k]=f* J[5];
    JE_dx_ref[6][k]=fi*J[6];
    JE_dx_ref[7][k]=fi*J[7];
    JE_dx_ref[8][k]=   J[8];
    db_JacobianOfRotatedPointStride(J,R+2,3);
    JE_dx_ref[0][l]=   J[0];
    JE_dx_ref[1][l]=   J[1];
    JE_dx_ref[2][l]=f* J[2];
    JE_dx_ref[3][l]=   J[3];
    JE_dx_ref[4][l]=   J[4];
    JE_dx_ref[5][l]=f* J[5];
    JE_dx_ref[6][l]=fi*J[6];
    JE_dx_ref[7][l]=fi*J[7];
    JE_dx_ref[8][l]=   J[8];

    JE_dx_ref[0][m]=0;
    JE_dx_ref[1][m]=0;
    JE_dx_ref[2][m]=H[2];
    JE_dx_ref[3][m]=0;
    JE_dx_ref[4][m]=0;
    JE_dx_ref[5][m]=H[5];
    JE_dx_ref[6][m]= -fi2*H[6];
    JE_dx_ref[7][m]= -fi2*H[7];
    JE_dx_ref[8][m]=0;
}

inline double db_RobImageHomography_Jacobians_Generic(double *JtJ_ref[8],double min_Jtf[8],int *num_param,int *frozen_coord,double H[9],int point_count,double *x_i,double *xp_i,int homography_type,double one_over_scale2)
{
    double back;
    int i,j,fetch_vector[8],n;
    double JtJ_temp[81],min_Jtf_temp[9],JE_dx[72];
    double *JE_dx_ref[9],*JtJ_temp_ref[9];

    /*Compute cost and JtJ,min_Jtf with respect to H*/
    back=db_RobImageHomography_Jacobians(JtJ_temp,min_Jtf_temp,H,point_count,x_i,xp_i,one_over_scale2);

    /*Compute JtJ,min_Jtf with respect to the right parameters
    The formulas are
    JtJ=transpose(JE_dx)*JtJ*JE_dx and
    min_Jtf=transpose(JE_dx)*min_Jtf,
    where the 9xN matrix JE_dx is the Jacobian of H with respect
    to the update*/
    db_SetupMatrixRefs(JtJ_temp_ref,9,9,JtJ_temp);
    db_SetupMatrixRefs(JE_dx_ref,9,8,JE_dx);
    switch(homography_type)
    {
        case DB_HOMOGRAPHY_TYPE_SIMILARITY:
        case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
            n=4;
            db_RobImageHomographyJH_Js(JE_dx_ref,0,H);
            db_RobImageHomographyJH_JR(JE_dx_ref,1,H);
            db_RobImageHomographyJH_Jt(JE_dx_ref,2,3,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
        case DB_HOMOGRAPHY_TYPE_ROTATION:
        case DB_HOMOGRAPHY_TYPE_ROTATION_U:
            n=1;
            db_RobImageHomographyJH_JR(JE_dx_ref,0,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
        case DB_HOMOGRAPHY_TYPE_SCALING:
            n=1;
            db_RobImageHomographyJH_Js(JE_dx_ref,0,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
        case DB_HOMOGRAPHY_TYPE_S_T:
            n=3;
            db_RobImageHomographyJH_Js(JE_dx_ref,0,H);
            db_RobImageHomographyJH_Jt(JE_dx_ref,1,2,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
        case DB_HOMOGRAPHY_TYPE_R_T:
            n=3;
            db_RobImageHomographyJH_JR(JE_dx_ref,0,H);
            db_RobImageHomographyJH_Jt(JE_dx_ref,1,2,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
        case DB_HOMOGRAPHY_TYPE_R_S:
            n=2;
            db_RobImageHomographyJH_Js(JE_dx_ref,0,H);
            db_RobImageHomographyJH_JR(JE_dx_ref,1,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;

        case DB_HOMOGRAPHY_TYPE_TRANSLATION:
            n=2;
            fetch_vector[0]=2;
            fetch_vector[1]=5;
            db_RobImageHomographyFetchJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,n,fetch_vector);
            break;
        case DB_HOMOGRAPHY_TYPE_AFFINE:
            n=6;
            fetch_vector[0]=0;
            fetch_vector[1]=1;
            fetch_vector[2]=2;
            fetch_vector[3]=3;
            fetch_vector[4]=4;
            fetch_vector[5]=5;
            db_RobImageHomographyFetchJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,n,fetch_vector);
            break;
        case DB_HOMOGRAPHY_TYPE_PROJECTIVE:
            n=8;
            *frozen_coord=db_MaxAbsIndex9(H);
            for(j=0,i=0;i<9;i++) if(i!=(*frozen_coord))
            {
                fetch_vector[j]=i;
                j++;
            }
            db_RobImageHomographyFetchJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,n,fetch_vector);
            break;
        case DB_HOMOGRAPHY_TYPE_CAMROTATION_F:
        case DB_HOMOGRAPHY_TYPE_CAMROTATION_F_UD:
            n=4;
            db_RobImageHomographyJH_dRotFocal(JE_dx_ref,0,1,2,3,H);
            db_RobImageHomographyMultiplyJacobian(JtJ_ref,min_Jtf,JtJ_temp_ref,min_Jtf_temp,JE_dx_ref,n);
            break;
    }
    *num_param=n;

    return(back);
}

inline void db_ImageHomographyUpdateGeneric(double H_p_dx[9],double H[9],double *dx,int homography_type,int frozen_coord)
{
    switch(homography_type)
    {
        case DB_HOMOGRAPHY_TYPE_SIMILARITY:
        case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
            db_Copy9(H_p_dx,H);
            db_MultiplyScaleOntoImageHomography(H,1.0+dx[0]);
            db_MultiplyRotationOntoImageHomography(H,dx[1]);
            H_p_dx[2]+=dx[2];
            H_p_dx[5]+=dx[3];
            break;
        case DB_HOMOGRAPHY_TYPE_ROTATION:
        case DB_HOMOGRAPHY_TYPE_ROTATION_U:
            db_MultiplyRotationOntoImageHomography(H,dx[0]);
            break;
        case DB_HOMOGRAPHY_TYPE_SCALING:
            db_MultiplyScaleOntoImageHomography(H,1.0+dx[0]);
            break;
        case DB_HOMOGRAPHY_TYPE_S_T:
            db_Copy9(H_p_dx,H);
            db_MultiplyScaleOntoImageHomography(H,1.0+dx[0]);
            H_p_dx[2]+=dx[1];
            H_p_dx[5]+=dx[2];
            break;
        case DB_HOMOGRAPHY_TYPE_R_T:
            db_Copy9(H_p_dx,H);
            db_MultiplyRotationOntoImageHomography(H,dx[0]);
            H_p_dx[2]+=dx[1];
            H_p_dx[5]+=dx[2];
            break;
        case DB_HOMOGRAPHY_TYPE_R_S:
            db_Copy9(H_p_dx,H);
            db_MultiplyScaleOntoImageHomography(H,1.0+dx[0]);
            db_MultiplyRotationOntoImageHomography(H,dx[1]);
            break;
        case DB_HOMOGRAPHY_TYPE_TRANSLATION:
            db_Copy9(H_p_dx,H);
            H_p_dx[2]+=dx[0];
            H_p_dx[5]+=dx[1];
            break;
        case DB_HOMOGRAPHY_TYPE_AFFINE:
            db_UpdateImageHomographyAffine(H_p_dx,H,dx);
            break;
        case DB_HOMOGRAPHY_TYPE_PROJECTIVE:
            db_UpdateImageHomographyProjective(H_p_dx,H,dx,frozen_coord);
            break;
        case DB_HOMOGRAPHY_TYPE_CAMROTATION_F:
        case DB_HOMOGRAPHY_TYPE_CAMROTATION_F_UD:
            db_UpdateRotFocalHomography(H_p_dx,H,dx);
            break;
    }
}

void db_RobCamRotation_Polish_Generic(double H[9],int point_count,int homography_type,double *x_i,double *xp_i,double one_over_scale2,
                               int max_iterations,double improvement_requirement)
{
    int i,update,stop,n;
    int frozen_coord = 0;
    double lambda,cost,current_cost;
    double JtJ[72],min_Jtf[9],dx[8],H_p_dx[9];
    double *JtJ_ref[9],d[8];

    lambda=0.001;
    for(update=1,stop=0,i=0;(stop<2) && (i<max_iterations);i++)
    {
        /*if first time since improvement, compute Jacobian and residual*/
        if(update)
        {
            db_SetupMatrixRefs(JtJ_ref,9,8,JtJ);
            current_cost=db_RobImageHomography_Jacobians_Generic(JtJ_ref,min_Jtf,&n,&frozen_coord,H,point_count,x_i,xp_i,homography_type,one_over_scale2);
            update=0;
        }

#ifdef _VERBOSE_
        /*std::cout << "Cost:" << current_cost << " ";*/
#endif /*_VERBOSE_*/

        /*Come up with a hypothesis dx
        based on the current lambda*/
        db_Compute_dx(dx,JtJ_ref,min_Jtf,lambda,d,n);

        /*Compute Cost(x+dx)*/
        db_ImageHomographyUpdateGeneric(H_p_dx,H,dx,homography_type,frozen_coord);
        cost=db_RobImageHomography_Cost(H_p_dx,point_count,x_i,xp_i,one_over_scale2);

        /*Is there an improvement?*/
        if(cost<current_cost)
        {
            /*improvement*/
            if(current_cost-cost<current_cost*improvement_requirement) stop++;
            else stop=0;
            lambda*=0.1;
            /*Move to the hypothesised position x+dx*/
            current_cost=cost;
            db_Copy9(H,H_p_dx);
            update=1;

#ifdef _VERBOSE_
        std::cout << "Step" << i << "Imp,Lambda=" << lambda << "Cost:" << current_cost << std::endl;
#endif /*_VERBOSE_*/
        }
        else
        {
            /*no improvement*/
            lambda*=10.0;
            stop=0;
        }
    }
}
void db_RobImageHomography(
                              /*Best homography*/
                              double H[9],
                              /*2DPoint to 2DPoint constraints
                              Points are assumed to be given in
                              homogenous coordinates*/
                              double *im, double *im_p,
                              /*Nr of points in total*/
                              int nr_points,
                              /*Calibration matrices
                              used to normalize the points*/
                              double K[9],
                              double Kp[9],
                              /*Pre-allocated space temp_d
                              should point to at least
                              12*nr_samples+10*nr_points
                              allocated positions*/
                              double *temp_d,
                              /*Pre-allocated space temp_i
                              should point to at least
                              max(nr_samples,nr_points)
                              allocated positions*/
                              int *temp_i,
                              int homography_type,
                              db_Statistics *stat,
                              int max_iterations,
                              int max_points,
                              double scale,
                              int nr_samples,
                              int chunk_size,
                              /////////////////////////////////////////////
                              // regular use: set outlierremoveflagE =0;
                              // flag for the outlier removal
                              int outlierremoveflagE,
                              // if flag is 1, then the following variables
                              // need the input
                              //////////////////////////////////////
                              // 3D coordinates
                              double *wp,
                              // its corresponding stereo pair's points
                              double *im_r,
                              // raw image coordinates
                              double *im_raw, double *im_raw_p,
                              // final matches
                              int *finalNumE)
{
    /*Random seed*/
    int r_seed;

    int point_count_new;
    /*Counters*/
    int i,j,c,point_count,hyp_count;
    int last_hyp,new_last_hyp,last_corr;
    int pos,point_pos,last_point;
    /*Accumulator*/
    double acc;
    /*Hypothesis pointer*/
    double *hyp_point;
    /*Random sample*/
    int s[4];
    /*Pivot for hypothesis pruning*/
    double pivot;
    /*Best hypothesis position*/
    int best_pos;
    /*Best score*/
    double lowest_cost;
    /*One over the squared scale of
    Cauchy distribution*/
    double one_over_scale2;
    /*temporary pointers*/
    double *x_i_temp,*xp_i_temp;
    /*Temporary space for inverse calibration matrices*/
    double K_inv[9];
    double Kp_inv[9];
    /*Temporary space for homography*/
    double H_temp[9],H_temp2[9];
    /*Pointers to homogenous coordinates*/
    double *x_h_point,*xp_h_point;
    /*Array of pointers to inhomogenous coordinates*/
    double *X[3],*Xp[3];
    /*Similarity parameters*/
    int orientation_preserving,allow_scaling,allow_rotation,allow_translation,sample_size;

    /*Homogenous coordinates of image points in first image*/
    double *x_h;
    /*Homogenous coordinates of image points in second image*/
    double *xp_h;
    /*Inhomogenous coordinates of image points in first image*/
    double *x_i;
    /*Inhomogenous coordinates of image points in second image*/
    double *xp_i;
    /*Homography hypotheses*/
    double *hyp_H_array;
    /*Cost array*/
    double *hyp_cost_array;
    /*Permutation of the hypotheses*/
    int *hyp_perm;
    /*Sample of the points*/
    int *point_perm;
    /*Temporary space for quick-select
    2*nr_samples*/
    double *temp_select;

    /*Get inverse calibration matrices*/
    db_InvertCalibrationMatrix(K_inv,K);
    db_InvertCalibrationMatrix(Kp_inv,Kp);
    /*Compute scale coefficient*/
    one_over_scale2=1.0/(scale*scale);
    /*Initialize random seed*/
    r_seed=12345;
    /*Set pointers to pre-allocated space*/
    hyp_cost_array=temp_d;
    hyp_H_array=temp_d+nr_samples;
    temp_select=temp_d+10*nr_samples;
    x_h=temp_d+12*nr_samples;
    xp_h=temp_d+12*nr_samples+3*nr_points;
    x_i=temp_d+12*nr_samples+6*nr_points;
    xp_i=temp_d+12*nr_samples+8*nr_points;
    hyp_perm=temp_i;
    point_perm=temp_i;

    /*Prepare a randomly permuted subset of size
    point_count from the input points*/

    point_count=db_mini(nr_points,(int)(chunk_size*log((double)nr_samples)/DB_LN2));

    point_count_new = point_count;

    for(i=0;i<nr_points;i++) point_perm[i]=i;

    for(last_point=nr_points-1,i=0;i<point_count;i++,last_point--)
    {
        pos=db_RandomInt(r_seed,last_point);
        point_pos=point_perm[pos];
        point_perm[pos]=point_perm[last_point];

        /*Normalize image points with calibration
        matrices and move them to x_h and xp_h*/
        c=3*point_pos;
        j=3*i;
        x_h_point=x_h+j;
        xp_h_point=xp_h+j;
        db_Multiply3x3_3x1(x_h_point,K_inv,im+c);
        db_Multiply3x3_3x1(xp_h_point,Kp_inv,im_p+c);

        db_HomogenousNormalize3(x_h_point);
        db_HomogenousNormalize3(xp_h_point);

        /*Dehomogenize image points and move them
        to x_i and xp_i*/
        c=(i<<1);
        db_DeHomogenizeImagePoint(x_i+c,x_h_point); // 2-dimension
        db_DeHomogenizeImagePoint(xp_i+c,xp_h_point); //2-dimension
    }


    /*Generate Hypotheses*/
    hyp_count=0;
    switch(homography_type)
    {
    case DB_HOMOGRAPHY_TYPE_SIMILARITY:
    case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
    case DB_HOMOGRAPHY_TYPE_TRANSLATION:
    case DB_HOMOGRAPHY_TYPE_ROTATION:
    case DB_HOMOGRAPHY_TYPE_ROTATION_U:
    case DB_HOMOGRAPHY_TYPE_SCALING:
    case DB_HOMOGRAPHY_TYPE_S_T:
    case DB_HOMOGRAPHY_TYPE_R_T:
    case DB_HOMOGRAPHY_TYPE_R_S:

        switch(homography_type)
        {
        case DB_HOMOGRAPHY_TYPE_SIMILARITY:
            orientation_preserving=1;
            allow_scaling=1;
            allow_rotation=1;
            allow_translation=1;
            sample_size=2;
            break;
        case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
            orientation_preserving=0;
            allow_scaling=1;
            allow_rotation=1;
            allow_translation=1;
            sample_size=3;
            break;
        case DB_HOMOGRAPHY_TYPE_TRANSLATION:
            orientation_preserving=1;
            allow_scaling=0;
            allow_rotation=0;
            allow_translation=1;
            sample_size=1;
            break;
        case DB_HOMOGRAPHY_TYPE_ROTATION:
            orientation_preserving=1;
            allow_scaling=0;
            allow_rotation=1;
            allow_translation=0;
            sample_size=1;
            break;
        case DB_HOMOGRAPHY_TYPE_ROTATION_U:
            orientation_preserving=0;
            allow_scaling=0;
            allow_rotation=1;
            allow_translation=0;
            sample_size=2;
            break;
        case DB_HOMOGRAPHY_TYPE_SCALING:
            orientation_preserving=1;
            allow_scaling=1;
            allow_rotation=0;
            allow_translation=0;
            sample_size=1;
            break;
        case DB_HOMOGRAPHY_TYPE_S_T:
            orientation_preserving=1;
            allow_scaling=1;
            allow_rotation=0;
            allow_translation=1;
            sample_size=2;
            break;
        case DB_HOMOGRAPHY_TYPE_R_T:
            orientation_preserving=1;
            allow_scaling=0;
            allow_rotation=1;
            allow_translation=1;
            sample_size=2;
            break;
        case DB_HOMOGRAPHY_TYPE_R_S:
            orientation_preserving=1;
            allow_scaling=1;
            allow_rotation=0;
            allow_translation=0;
            sample_size=1;
            break;
        }

        if(point_count>=sample_size) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,3,point_count,r_seed);
            X[0]= &x_i[s[0]<<1];
            X[1]= &x_i[s[1]<<1];
            X[2]= &x_i[s[2]<<1];
            Xp[0]= &xp_i[s[0]<<1];
            Xp[1]= &xp_i[s[1]<<1];
            Xp[2]= &xp_i[s[2]<<1];
            db_StitchSimilarity2D(&hyp_H_array[9*hyp_count],Xp,X,sample_size,orientation_preserving,
                                  allow_scaling,allow_rotation,allow_translation);
            hyp_count++;
        }
        break;

    case DB_HOMOGRAPHY_TYPE_CAMROTATION:
        if(point_count>=2) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,2,point_count,r_seed);
            db_StitchCameraRotation_2Points(&hyp_H_array[9*hyp_count],
                                      &x_h[3*s[0]],&x_h[3*s[1]],
                                      &xp_h[3*s[0]],&xp_h[3*s[1]]);
            hyp_count++;
        }
        break;

    case DB_HOMOGRAPHY_TYPE_CAMROTATION_F:
        if(point_count>=3) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,3,point_count,r_seed);
            hyp_count+=db_StitchRotationCommonFocalLength_3Points(&hyp_H_array[9*hyp_count],
                                      &x_h[3*s[0]],&x_h[3*s[1]],&x_h[3*s[2]],
                                      &xp_h[3*s[0]],&xp_h[3*s[1]],&xp_h[3*s[2]]);
        }
        break;

    case DB_HOMOGRAPHY_TYPE_CAMROTATION_F_UD:
        if(point_count>=3) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,3,point_count,r_seed);
            hyp_count+=db_StitchRotationCommonFocalLength_3Points(&hyp_H_array[9*hyp_count],
                                      &x_h[3*s[0]],&x_h[3*s[1]],&x_h[3*s[2]],
                                      &xp_h[3*s[0]],&xp_h[3*s[1]],&xp_h[3*s[2]],NULL,0);
        }
        break;

    case DB_HOMOGRAPHY_TYPE_AFFINE:
        if(point_count>=3) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,3,point_count,r_seed);
            db_StitchAffine2D_3Points(&hyp_H_array[9*hyp_count],
                                      &x_h[3*s[0]],&x_h[3*s[1]],&x_h[3*s[2]],
                                      &xp_h[3*s[0]],&xp_h[3*s[1]],&xp_h[3*s[2]]);
            hyp_count++;
        }
        break;

    case DB_HOMOGRAPHY_TYPE_PROJECTIVE:
    default:
        if(point_count>=4) for(i=0;i<nr_samples;i++)
        {
            db_RandomSample(s,4,point_count,r_seed);
            db_StitchProjective2D_4Points(&hyp_H_array[9*hyp_count],
                                      &x_h[3*s[0]],&x_h[3*s[1]],&x_h[3*s[2]],&x_h[3*s[3]],
                                      &xp_h[3*s[0]],&xp_h[3*s[1]],&xp_h[3*s[2]],&xp_h[3*s[3]]);
            hyp_count++;
        }
    }

    if(hyp_count)
    {
        /*Count cost in chunks and decimate hypotheses
        until only one remains or the correspondences are
        exhausted*/
        for(i=0;i<hyp_count;i++)
        {
            hyp_perm[i]=i;
            hyp_cost_array[i]=0.0;
        }
        for(i=0,last_hyp=hyp_count-1;(last_hyp>0) && (i<point_count);i+=chunk_size)
        {
            /*Update cost with the next chunk*/
            last_corr=db_mini(i+chunk_size-1,point_count-1);
            for(j=0;j<=last_hyp;j++)
            {
                hyp_point=hyp_H_array+9*hyp_perm[j];
                for(c=i;c<=last_corr;)
                {
                    /*Take log of product of ten reprojection
                    errors to reduce nr of expensive log operations*/
                    if(c+9<=last_corr)
                    {
                        x_i_temp=x_i+(c<<1);
                        xp_i_temp=xp_i+(c<<1);

                        acc=db_ExpCauchyInhomogenousHomographyError(xp_i_temp,hyp_point,x_i_temp,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+2,hyp_point,x_i_temp+2,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+4,hyp_point,x_i_temp+4,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+6,hyp_point,x_i_temp+6,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+8,hyp_point,x_i_temp+8,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+10,hyp_point,x_i_temp+10,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+12,hyp_point,x_i_temp+12,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+14,hyp_point,x_i_temp+14,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+16,hyp_point,x_i_temp+16,one_over_scale2);
                        acc*=db_ExpCauchyInhomogenousHomographyError(xp_i_temp+18,hyp_point,x_i_temp+18,one_over_scale2);
                        c+=10;
                    }
                    else
                    {
                        for(acc=1.0;c<=last_corr;c++)
                        {
                            acc*=db_ExpCauchyInhomogenousHomographyError(xp_i+(c<<1),hyp_point,x_i+(c<<1),one_over_scale2);
                        }
                    }
                    hyp_cost_array[j]+=log(acc);
                }
            }
            if (chunk_size<point_count){
                /*Prune out half of the hypotheses*/
                new_last_hyp=(last_hyp+1)/2-1;
                pivot=db_LeanQuickSelect(hyp_cost_array,last_hyp+1,new_last_hyp,temp_select);
                for(j=0,c=0;(j<=last_hyp) && (c<=new_last_hyp);j++)
                {
                    if(hyp_cost_array[j]<=pivot)
                    {
                        hyp_cost_array[c]=hyp_cost_array[j];
                        hyp_perm[c]=hyp_perm[j];
                        c++;
                    }
                }
                last_hyp=new_last_hyp;
            }
        }
        /*Find the best hypothesis*/
        lowest_cost=hyp_cost_array[0];
        best_pos=0;
        for(j=1;j<=last_hyp;j++)
        {
            if(hyp_cost_array[j]<lowest_cost)
            {
                lowest_cost=hyp_cost_array[j];
                best_pos=j;
            }
        }

        /*Move the best hypothesis*/
        db_Copy9(H_temp,hyp_H_array+9*hyp_perm[best_pos]);

        // outlier removal
        if (outlierremoveflagE) // no polishment needed
        {
            point_count_new = db_RemoveOutliers_Homography(H_temp,x_i,xp_i,wp,im,im_p,im_r,im_raw,im_raw_p,point_count,one_over_scale2);
        }
        else
        {
            /*Polish*/
            switch(homography_type)
            {
            case DB_HOMOGRAPHY_TYPE_SIMILARITY:
            case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
            case DB_HOMOGRAPHY_TYPE_TRANSLATION:
            case DB_HOMOGRAPHY_TYPE_ROTATION:
            case DB_HOMOGRAPHY_TYPE_ROTATION_U:
            case DB_HOMOGRAPHY_TYPE_SCALING:
            case DB_HOMOGRAPHY_TYPE_S_T:
            case DB_HOMOGRAPHY_TYPE_R_T:
            case DB_HOMOGRAPHY_TYPE_R_S:
            case DB_HOMOGRAPHY_TYPE_AFFINE:
            case DB_HOMOGRAPHY_TYPE_PROJECTIVE:
            case DB_HOMOGRAPHY_TYPE_CAMROTATION_F:
            case DB_HOMOGRAPHY_TYPE_CAMROTATION_F_UD:
                db_RobCamRotation_Polish_Generic(H_temp,db_mini(point_count,max_points),homography_type,x_i,xp_i,one_over_scale2,max_iterations);
                break;
            case DB_HOMOGRAPHY_TYPE_CAMROTATION:
                db_RobCamRotation_Polish(H_temp,db_mini(point_count,max_points),x_i,xp_i,one_over_scale2,max_iterations);
                break;
            }

        }

    }
    else db_Identity3x3(H_temp);

    switch(homography_type)
    {
    case DB_HOMOGRAPHY_TYPE_PROJECTIVE:
        if(stat) stat->nr_parameters=8;
        break;
    case DB_HOMOGRAPHY_TYPE_AFFINE:
        if(stat) stat->nr_parameters=6;
        break;
    case DB_HOMOGRAPHY_TYPE_SIMILARITY:
    case DB_HOMOGRAPHY_TYPE_SIMILARITY_U:
    case DB_HOMOGRAPHY_TYPE_CAMROTATION_F:
    case DB_HOMOGRAPHY_TYPE_CAMROTATION_F_UD:
        if(stat) stat->nr_parameters=4;
        break;
    case DB_HOMOGRAPHY_TYPE_CAMROTATION:
        if(stat) stat->nr_parameters=3;
        break;
    case DB_HOMOGRAPHY_TYPE_TRANSLATION:
    case DB_HOMOGRAPHY_TYPE_S_T:
    case DB_HOMOGRAPHY_TYPE_R_T:
    case DB_HOMOGRAPHY_TYPE_R_S:
        if(stat) stat->nr_parameters=2;
        break;
    case DB_HOMOGRAPHY_TYPE_ROTATION:
    case DB_HOMOGRAPHY_TYPE_ROTATION_U:
    case DB_HOMOGRAPHY_TYPE_SCALING:
        if(stat) stat->nr_parameters=1;
        break;
    }

    db_RobImageHomography_Statistics(H_temp,db_mini(point_count,max_points),x_i,xp_i,one_over_scale2,stat);

    /*Put on the calibration matrices*/
    db_Multiply3x3_3x3(H_temp2,H_temp,K_inv);
    db_Multiply3x3_3x3(H,Kp,H_temp2);

    if (finalNumE)
        *finalNumE = point_count_new;

}
