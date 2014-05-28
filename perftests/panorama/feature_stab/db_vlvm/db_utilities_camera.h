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

/* $Id: db_utilities_camera.h,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_UTILITIES_CAMERA
#define DB_UTILITIES_CAMERA

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMCamera (LM) Camera Utilities
 */
/*\{*/

#include "db_utilities.h"

#define DB_RADDISTMODE_BOUGEUT  4
#define DB_RADDISTMODE_2NDORDER 5
#define DB_RADDISTMODE_IDENTITY 6

/*!
Give reasonable guess of the calibration matrix for normalization purposes.
Use real K matrix when doing real geometry.
focal length = (w+h)/2.0*f_correction.
\param K            calibration matrix (out)
\param Kinv         inverse of K (out)
\param im_width     image width
\param im_height    image height
\param f_correction focal length correction factor
\param field        set to 1 if this is a field image (fy = fx/2)
\return K(3x3) intrinsic calibration matrix
*/
DB_API void db_Approx3DCalMat(double K[9],double Kinv[9],int im_width,int im_height,double f_correction=1.0,int field=0);

/*!
 Make a 2x2 identity matrix
 */
void inline db_Identity2x2(double A[4])
{
    A[0]=1;A[1]=0;
    A[2]=0;A[3]=1;
}
/*!
 Make a 3x3 identity matrix
 */
void inline db_Identity3x3(double A[9])
{
    A[0]=1;A[1]=0;A[2]=0;
    A[3]=0;A[4]=1;A[5]=0;
    A[6]=0;A[7]=0;A[8]=1;
}
/*!
 Invert intrinsic calibration matrix K(3x3)
 If fx or fy is 0, I is returned.
 */
void inline db_InvertCalibrationMatrix(double Kinv[9],const double K[9])
{
    double a,b,c,d,e,f,ainv,dinv,adinv;

    a=K[0];b=K[1];c=K[2];d=K[4];e=K[5];f=K[8];
    if((a==0.0)||(d==0.0)) db_Identity3x3(Kinv);
    else
    {
        Kinv[3]=0.0;
        Kinv[6]=0.0;
        Kinv[7]=0.0;
        Kinv[8]=1.0;

        ainv=1.0/a;
        dinv=1.0/d;
        adinv=ainv*dinv;
        Kinv[0]=f*ainv;
        Kinv[1]= -b*f*adinv;
        Kinv[2]=(b*e-c*d)*adinv;
        Kinv[4]=f*dinv;
        Kinv[5]= -e*dinv;
    }
}
/*!
 De-homogenize image point: xd(1:2) = xs(1:2)/xs(3).
 If xs(3) is 0, xd will become 0
 \param xd  destination point
 \param xs  source point
 */
void inline db_DeHomogenizeImagePoint(double xd[2],const double xs[3])
{
    double temp,div;

    temp=xs[2];
    if(temp!=0)
    {
        div=1.0/temp;
        xd[0]=xs[0]*div;xd[1]=xs[1]*div;
    }
    else
    {
        xd[0]=0.0;xd[1]=0.0;
    }
}


/*!
 Orthonormalize 3D rotation R
 */
inline void db_OrthonormalizeRotation(double R[9])
{
    double s,mult;
    /*Normalize first vector*/
    s=db_sqr(R[0])+db_sqr(R[1])+db_sqr(R[2]);
    mult=sqrt(1.0/(s?s:1));
    R[0]*=mult; R[1]*=mult; R[2]*=mult;
    /*Subtract scalar product from second vector*/
    s=R[0]*R[3]+R[1]*R[4]+R[2]*R[5];
    R[3]-=s*R[0]; R[4]-=s*R[1]; R[5]-=s*R[2];
    /*Normalize second vector*/
    s=db_sqr(R[3])+db_sqr(R[4])+db_sqr(R[5]);
    mult=sqrt(1.0/(s?s:1));
    R[3]*=mult; R[4]*=mult; R[5]*=mult;
    /*Get third vector by vector product*/
    R[6]=R[1]*R[5]-R[4]*R[2];
    R[7]=R[2]*R[3]-R[5]*R[0];
    R[8]=R[0]*R[4]-R[3]*R[1];
}
/*!
Update a rotation with the update dx=[sin(phi) sin(ohm) sin(kap)]
*/
inline void db_UpdateRotation(double R_p_dx[9],double R[9],const double dx[3])
{
    double R_temp[9];
    /*Update rotation*/
    db_IncrementalRotationMatrix(R_temp,dx);
    db_Multiply3x3_3x3(R_p_dx,R_temp,R);
}
/*!
 Compute xp = Hx for inhomogenous image points.
 */
inline void db_ImageHomographyInhomogenous(double xp[2],const double H[9],const double x[2])
{
    double x3,m;

    x3=H[6]*x[0]+H[7]*x[1]+H[8];
    if(x3!=0.0)
    {
        m=1.0/x3;
        xp[0]=m*(H[0]*x[0]+H[1]*x[1]+H[2]);
        xp[1]=m*(H[3]*x[0]+H[4]*x[1]+H[5]);
    }
    else
    {
        xp[0]=xp[1]=0.0;
    }
}
inline double db_FocalFromCamRotFocalHomography(const double H[9])
{
    double k1,k2;

    k1=db_sqr(H[2])+db_sqr(H[5]);
    k2=db_sqr(H[6])+db_sqr(H[7]);
    if(k1>=k2)
    {
        return(db_SafeSqrt(db_SafeDivision(k1,1.0-db_sqr(H[8]))));
    }
    else
    {
        return(db_SafeSqrt(db_SafeDivision(1.0-db_sqr(H[8]),k2)));
    }
}

inline double db_FocalAndRotFromCamRotFocalHomography(double R[9],const double H[9])
{
    double back,fi;

    back=db_FocalFromCamRotFocalHomography(H);
    fi=db_SafeReciprocal(back);
    R[0]=H[0];      R[1]=H[1];      R[2]=fi*H[2];
    R[3]=H[3];      R[4]=H[4];      R[5]=fi*H[5];
    R[6]=back*H[6]; R[7]=back*H[7]; R[8]=H[8];
    return(back);
}
/*!
Compute Jacobian at zero of three coordinates dR*x with
respect to the update dR([sin(phi) sin(ohm) sin(kap)]) given x.

The Jacobian at zero of the homogenous coordinates with respect to
    [sin(phi) sin(ohm) sin(kap)] is
\code
    [-rx2   0   rx1 ]
    [  0   rx2 -rx0 ]
    [ rx0 -rx1   0  ].
\endcode

*/
inline void db_JacobianOfRotatedPointStride(double J[9],const double x[3],int stride)
{
    /*The Jacobian at zero of the homogenous coordinates with respect to
    [sin(phi) sin(ohm) sin(kap)] is
    [-rx2   0   rx1 ]
    [  0   rx2 -rx0 ]
    [ rx0 -rx1   0  ]*/

    J[0]= -x[stride<<1];
    J[1]=0;
    J[2]=  x[stride];
    J[3]=0;
    J[4]=  x[stride<<1];
    J[5]= -x[0];
    J[6]=  x[0];
    J[7]= -x[stride];
    J[8]=0;
}
/*!
 Invert an affine (if possible)
 \param Hinv    inverted matrix
 \param H       input matrix
 \return true if success and false if matrix is ill-conditioned (det < 1e-7)
 */
inline bool db_InvertAffineTransform(double Hinv[9],const double H[9])
{
    double det=H[0]*H[4]-H[3]*H[1];
    if (det<1e-7)
    {
        db_Copy9(Hinv,H);
        return false;
    }
    else
    {
        Hinv[0]=H[4]/det;
        Hinv[1]=-H[1]/det;
        Hinv[3]=-H[3]/det;
        Hinv[4]=H[0]/det;
        Hinv[2]= -Hinv[0]*H[2]-Hinv[1]*H[5];
        Hinv[5]= -Hinv[3]*H[2]-Hinv[4]*H[5];
    }
    return true;
}

/*!
Update of upper 2x2 is multiplication by
\code
[s 0][ cos(theta) sin(theta)]
[0 s][-sin(theta) cos(theta)]
\endcode
*/
inline void db_MultiplyScaleOntoImageHomography(double H[9],double s)
{

    H[0]*=s;
    H[1]*=s;
    H[3]*=s;
    H[4]*=s;
}
/*!
Update of upper 2x2 is multiplication by
\code
[s 0][ cos(theta) sin(theta)]
[0 s][-sin(theta) cos(theta)]
\endcode
*/
inline void db_MultiplyRotationOntoImageHomography(double H[9],double theta)
{
    double c,s,H0,H1;


    c=cos(theta);
    s=db_SafeSqrt(1.0-db_sqr(c));
    H0=  c*H[0]+s*H[3];
    H[3]= -s*H[0]+c*H[3];
    H[0]=H0;
    H1=c*H[1]+s*H[4];
    H[4]= -s*H[1]+c*H[4];
    H[1]=H1;
}

inline void db_UpdateImageHomographyAffine(double H_p_dx[9],const double H[9],const double dx[6])
{
    db_AddVectors6(H_p_dx,H,dx);
    db_Copy3(H_p_dx+6,H+6);
}

inline void db_UpdateImageHomographyProjective(double H_p_dx[9],const double H[9],const double dx[8],int frozen_coord)
{
    int i,j;

    for(j=0,i=0;i<9;i++)
    {
        if(i!=frozen_coord)
        {
            H_p_dx[i]=H[i]+dx[j];
            j++;
        }
        else H_p_dx[i]=H[i];
    }
}

inline void db_UpdateRotFocalHomography(double H_p_dx[9],const double H[9],const double dx[4])
{
    double f,fp,fpi;
    double R[9],dR[9];

    /*Updated matrix is diag(f+df,f+df)*dR*R*diag(1/(f+df),1/(f+df),1)*/
    f=db_FocalAndRotFromCamRotFocalHomography(R,H);
    db_IncrementalRotationMatrix(dR,dx);
    db_Multiply3x3_3x3(H_p_dx,dR,R);
    fp=f+dx[3];
    fpi=db_SafeReciprocal(fp);
    H_p_dx[2]*=fp;
    H_p_dx[5]*=fp;
    H_p_dx[6]*=fpi;
    H_p_dx[7]*=fpi;
}

/*\}*/
#endif /* DB_UTILITIES_CAMERA */
