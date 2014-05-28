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

/* $Id: db_metrics.h,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_METRICS
#define DB_METRICS



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

#include "db_utilities.h"
/*!
 * \defgroup LMMetrics (LM) Metrics
 */
/*\{*/




/*!
Compute function value fp and Jacobian J of robustifier given input value f*/
inline void db_CauchyDerivative(double J[4],double fp[2],const double f[2],double one_over_scale2)
{
    double x2,y2,r,r2,r2s,one_over_r2,fu,r_fu,one_over_r_fu;
    double one_plus_r2s,half_dfu_dx,half_dfu_dy,coeff,coeff2,coeff3;
    int at_zero;

    /*The robustifier takes the input (x,y) and makes a new
    vector (xp,yp) where
    xp=sqrt(log(1+(x^2+y^2)*one_over_scale2))*x/sqrt(x^2+y^2)
    yp=sqrt(log(1+(x^2+y^2)*one_over_scale2))*y/sqrt(x^2+y^2)
    The new vector has the property
    xp^2+yp^2=log(1+(x^2+y^2)*one_over_scale2)
    i.e. when it is square-summed it gives the robust
    reprojection error
    Define
    r2=(x^2+y^2) and
    r2s=r2*one_over_scale2
    fu=log(1+r2s)/r2
    then
    xp=sqrt(fu)*x
    yp=sqrt(fu)*y
    and
    d(r2)/dx=2x
    d(r2)/dy=2y
    and
    dfu/dx=d(r2)/dx*(r2s/(1+r2s)-log(1+r2s))/(r2*r2)
    dfu/dy=d(r2)/dy*(r2s/(1+r2s)-log(1+r2s))/(r2*r2)
    and
    d(xp)/dx=1/(2sqrt(fu))*(dfu/dx)*x+sqrt(fu)
    d(xp)/dy=1/(2sqrt(fu))*(dfu/dy)*x
    d(yp)/dx=1/(2sqrt(fu))*(dfu/dx)*y
    d(yp)/dy=1/(2sqrt(fu))*(dfu/dy)*y+sqrt(fu)
    */

    x2=db_sqr(f[0]);
    y2=db_sqr(f[1]);
    r2=x2+y2;
    r=sqrt(r2);

    if(r2<=0.0) at_zero=1;
    else
    {
        one_over_r2=1.0/r2;
        r2s=r2*one_over_scale2;
        one_plus_r2s=1.0+r2s;
        fu=log(one_plus_r2s)*one_over_r2;
        r_fu=sqrt(fu);
        if(r_fu<=0.0) at_zero=1;
        else
        {
            one_over_r_fu=1.0/r_fu;
            fp[0]=r_fu*f[0];
            fp[1]=r_fu*f[1];
            /*r2s is always >= 0*/
            coeff=(r2s/one_plus_r2s*one_over_r2-fu)*one_over_r2;
            half_dfu_dx=f[0]*coeff;
            half_dfu_dy=f[1]*coeff;
            coeff2=one_over_r_fu*half_dfu_dx;
            coeff3=one_over_r_fu*half_dfu_dy;

            J[0]=coeff2*f[0]+r_fu;
            J[1]=coeff3*f[0];
            J[2]=coeff2*f[1];
            J[3]=coeff3*f[1]+r_fu;
            at_zero=0;
        }
    }
    if(at_zero)
    {
        /*Close to zero the robustifying mapping
        becomes identity*sqrt(one_over_scale2)*/
        fp[0]=0.0;
        fp[1]=0.0;
        J[0]=sqrt(one_over_scale2);
        J[1]=0.0;
        J[2]=0.0;
        J[3]=J[0];
    }
}

inline double db_SquaredReprojectionErrorHomography(const double y[2],const double H[9],const double x[3])
{
    double x0,x1,x2,mult;
    double sd;

    x0=H[0]*x[0]+H[1]*x[1]+H[2]*x[2];
    x1=H[3]*x[0]+H[4]*x[1]+H[5]*x[2];
    x2=H[6]*x[0]+H[7]*x[1]+H[8]*x[2];
    mult=1.0/((x2!=0.0)?x2:1.0);
    sd=db_sqr((y[0]-x0*mult))+db_sqr((y[1]-x1*mult));

    return(sd);
}

inline double db_SquaredInhomogenousHomographyError(const double y[2],const double H[9],const double x[2])
{
    double x0,x1,x2,mult;
    double sd;

    x0=H[0]*x[0]+H[1]*x[1]+H[2];
    x1=H[3]*x[0]+H[4]*x[1]+H[5];
    x2=H[6]*x[0]+H[7]*x[1]+H[8];
    mult=1.0/((x2!=0.0)?x2:1.0);
    sd=db_sqr((y[0]-x0*mult))+db_sqr((y[1]-x1*mult));

    return(sd);
}

/*!
Return a constant divided by likelihood of a Cauchy distributed
reprojection error given the image point y, homography H, image point
point x and the squared scale coefficient one_over_scale2=1.0/(scale*scale)
where scale is the half width at half maximum (hWahM) of the
Cauchy distribution*/
inline double db_ExpCauchyInhomogenousHomographyError(const double y[2],const double H[9],const double x[2],
                                                      double one_over_scale2)
{
    double sd;
    sd=db_SquaredInhomogenousHomographyError(y,H,x);
    return(1.0+sd*one_over_scale2);
}

/*!
Compute residual vector f between image point y and homography Hx of
image point x. Also compute Jacobian of f with respect
to an update dx of H*/
inline void db_DerivativeInhomHomographyError(double Jf_dx[18],double f[2],const double y[2],const double H[9],
                                              const double x[2])
{
    double xh,yh,zh,mult,mult2,xh_mult2,yh_mult2;
    /*The Jacobian of the inhomogenous coordinates with respect to
    the homogenous is
    [1/zh  0  -xh/(zh*zh)]
    [ 0  1/zh -yh/(zh*zh)]
    The Jacobian of the homogenous coordinates with respect to dH is
    [x0 x1 1  0  0 0  0  0 0]
    [ 0  0 0 x0 x1 1  0  0 0]
    [ 0  0 0  0  0 0 x0 x1 1]
    The output Jacobian is minus their product, i.e.
    [-x0/zh -x1/zh -1/zh    0      0     0    x0*xh/(zh*zh) x1*xh/(zh*zh) xh/(zh*zh)]
    [   0      0     0   -x0/zh -x1/zh -1/zh  x0*yh/(zh*zh) x1*yh/(zh*zh) yh/(zh*zh)]*/

    /*Compute warped point, which is the same as
    homogenous coordinates of reprojection*/
    xh=H[0]*x[0]+H[1]*x[1]+H[2];
    yh=H[3]*x[0]+H[4]*x[1]+H[5];
    zh=H[6]*x[0]+H[7]*x[1]+H[8];
    mult=1.0/((zh!=0.0)?zh:1.0);
    /*Compute inhomogenous residual*/
    f[0]=y[0]-xh*mult;
    f[1]=y[1]-yh*mult;
    /*Compute Jacobian*/
    mult2=mult*mult;
    xh_mult2=xh*mult2;
    yh_mult2=yh*mult2;
    Jf_dx[0]= -x[0]*mult;
    Jf_dx[1]= -x[1]*mult;
    Jf_dx[2]= -mult;
    Jf_dx[3]=0;
    Jf_dx[4]=0;
    Jf_dx[5]=0;
    Jf_dx[6]=x[0]*xh_mult2;
    Jf_dx[7]=x[1]*xh_mult2;
    Jf_dx[8]=xh_mult2;
    Jf_dx[9]=0;
    Jf_dx[10]=0;
    Jf_dx[11]=0;
    Jf_dx[12]=Jf_dx[0];
    Jf_dx[13]=Jf_dx[1];
    Jf_dx[14]=Jf_dx[2];
    Jf_dx[15]=x[0]*yh_mult2;
    Jf_dx[16]=x[1]*yh_mult2;
    Jf_dx[17]=yh_mult2;
}

/*!
Compute robust residual vector f between image point y and homography Hx of
image point x. Also compute Jacobian of f with respect
to an update dH of H*/
inline void db_DerivativeCauchyInhomHomographyReprojection(double Jf_dx[18],double f[2],const double y[2],const double H[9],
                                                           const double x[2],double one_over_scale2)
{
    double Jf_dx_loc[18],f_loc[2];
    double J[4],J0,J1,J2,J3;

    /*Compute reprojection Jacobian*/
    db_DerivativeInhomHomographyError(Jf_dx_loc,f_loc,y,H,x);
    /*Compute robustifier Jacobian*/
    db_CauchyDerivative(J,f,f_loc,one_over_scale2);

    /*Multiply the robustifier Jacobian with
    the reprojection Jacobian*/
    J0=J[0];J1=J[1];J2=J[2];J3=J[3];
    Jf_dx[0]=J0*Jf_dx_loc[0];
    Jf_dx[1]=J0*Jf_dx_loc[1];
    Jf_dx[2]=J0*Jf_dx_loc[2];
    Jf_dx[3]=                J1*Jf_dx_loc[12];
    Jf_dx[4]=                J1*Jf_dx_loc[13];
    Jf_dx[5]=                J1*Jf_dx_loc[14];
    Jf_dx[6]=J0*Jf_dx_loc[6]+J1*Jf_dx_loc[15];
    Jf_dx[7]=J0*Jf_dx_loc[7]+J1*Jf_dx_loc[16];
    Jf_dx[8]=J0*Jf_dx_loc[8]+J1*Jf_dx_loc[17];
    Jf_dx[9]= J2*Jf_dx_loc[0];
    Jf_dx[10]=J2*Jf_dx_loc[1];
    Jf_dx[11]=J2*Jf_dx_loc[2];
    Jf_dx[12]=                J3*Jf_dx_loc[12];
    Jf_dx[13]=                J3*Jf_dx_loc[13];
    Jf_dx[14]=                J3*Jf_dx_loc[14];
    Jf_dx[15]=J2*Jf_dx_loc[6]+J3*Jf_dx_loc[15];
    Jf_dx[16]=J2*Jf_dx_loc[7]+J3*Jf_dx_loc[16];
    Jf_dx[17]=J2*Jf_dx_loc[8]+J3*Jf_dx_loc[17];
}
/*!
Compute residual vector f between image point y and rotation of
image point x by R. Also compute Jacobian of f with respect
to an update dx of R*/
inline void db_DerivativeInhomRotationReprojection(double Jf_dx[6],double f[2],const double y[2],const double R[9],
                                                   const double x[2])
{
    double xh,yh,zh,mult,mult2,xh_mult2,yh_mult2;
    /*The Jacobian of the inhomogenous coordinates with respect to
    the homogenous is
    [1/zh  0  -xh/(zh*zh)]
    [ 0  1/zh -yh/(zh*zh)]
    The Jacobian at zero of the homogenous coordinates with respect to
    [sin(phi) sin(ohm) sin(kap)] is
    [-rx2   0   rx1 ]
    [  0   rx2 -rx0 ]
    [ rx0 -rx1   0  ]
    The output Jacobian is minus their product, i.e.
    [1+xh*xh/(zh*zh) -xh*yh/(zh*zh)   -yh/zh]
    [xh*yh/(zh*zh)   -1-yh*yh/(zh*zh)  xh/zh]*/

    /*Compute rotated point, which is the same as
    homogenous coordinates of reprojection*/
    xh=R[0]*x[0]+R[1]*x[1]+R[2];
    yh=R[3]*x[0]+R[4]*x[1]+R[5];
    zh=R[6]*x[0]+R[7]*x[1]+R[8];
    mult=1.0/((zh!=0.0)?zh:1.0);
    /*Compute inhomogenous residual*/
    f[0]=y[0]-xh*mult;
    f[1]=y[1]-yh*mult;
    /*Compute Jacobian*/
    mult2=mult*mult;
    xh_mult2=xh*mult2;
    yh_mult2=yh*mult2;
    Jf_dx[0]= 1.0+xh*xh_mult2;
    Jf_dx[1]= -yh*xh_mult2;
    Jf_dx[2]= -yh*mult;
    Jf_dx[3]= -Jf_dx[1];
    Jf_dx[4]= -1-yh*yh_mult2;
    Jf_dx[5]= xh*mult;
}

/*!
Compute robust residual vector f between image point y and rotation of
image point x by R. Also compute Jacobian of f with respect
to an update dx of R*/
inline void db_DerivativeCauchyInhomRotationReprojection(double Jf_dx[6],double f[2],const double y[2],const double R[9],
                                                         const double x[2],double one_over_scale2)
{
    double Jf_dx_loc[6],f_loc[2];
    double J[4],J0,J1,J2,J3;

    /*Compute reprojection Jacobian*/
    db_DerivativeInhomRotationReprojection(Jf_dx_loc,f_loc,y,R,x);
    /*Compute robustifier Jacobian*/
    db_CauchyDerivative(J,f,f_loc,one_over_scale2);

    /*Multiply the robustifier Jacobian with
    the reprojection Jacobian*/
    J0=J[0];J1=J[1];J2=J[2];J3=J[3];
    Jf_dx[0]=J0*Jf_dx_loc[0]+J1*Jf_dx_loc[3];
    Jf_dx[1]=J0*Jf_dx_loc[1]+J1*Jf_dx_loc[4];
    Jf_dx[2]=J0*Jf_dx_loc[2]+J1*Jf_dx_loc[5];
    Jf_dx[3]=J2*Jf_dx_loc[0]+J3*Jf_dx_loc[3];
    Jf_dx[4]=J2*Jf_dx_loc[1]+J3*Jf_dx_loc[4];
    Jf_dx[5]=J2*Jf_dx_loc[2]+J3*Jf_dx_loc[5];
}



/*!
// remove the outliers whose projection error is larger than pre-defined
*/
inline int db_RemoveOutliers_Homography(const double H[9], double *x_i,double *xp_i, double *wp,double *im, double *im_p, double *im_r, double *im_raw,double *im_raw_p,int point_count,double scale, double thresh=DB_OUTLIER_THRESHOLD)
{
    double temp_valueE, t2;
    int c;
    int k1=0;
    int k2=0;
    int k3=0;
    int numinliers=0;
    int ind1;
    int ind2;
    int ind3;
    int isinlier;

    // experimentally determined
    t2=1.0/(thresh*thresh*thresh*thresh);

    // count the inliers
    for(c=0;c<point_count;c++)
    {
        ind1=c<<1;
        ind2=c<<2;
        ind3=3*c;

        temp_valueE=db_SquaredInhomogenousHomographyError(im_p+ind3,H,im+ind3);

        isinlier=((temp_valueE<=t2)?1:0);

        // if it is inlier, then copy the 3d and 2d correspondences
        if (isinlier)
        {
            numinliers++;

            x_i[k1]=x_i[ind1];
            x_i[k1+1]=x_i[ind1+1];

            xp_i[k1]=xp_i[ind1];
            xp_i[k1+1]=xp_i[ind1+1];

            k1=k1+2;

            // original normalized pixel coordinates
            im[k3]=im[ind3];
            im[k3+1]=im[ind3+1];
            im[k3+2]=im[ind3+2];

            im_r[k3]=im_r[ind3];
            im_r[k3+1]=im_r[ind3+1];
            im_r[k3+2]=im_r[ind3+2];

            im_p[k3]=im_p[ind3];
            im_p[k3+1]=im_p[ind3+1];
            im_p[k3+2]=im_p[ind3+2];

            // left and right raw pixel coordinates
            im_raw[k3] = im_raw[ind3];
            im_raw[k3+1] = im_raw[ind3+1];
            im_raw[k3+2] = im_raw[ind3+2]; // the index

            im_raw_p[k3] = im_raw_p[ind3];
            im_raw_p[k3+1] = im_raw_p[ind3+1];
            im_raw_p[k3+2] = im_raw_p[ind3+2]; // the index

            k3=k3+3;

            // 3D coordinates
            wp[k2]=wp[ind2];
            wp[k2+1]=wp[ind2+1];
            wp[k2+2]=wp[ind2+2];
            wp[k2+3]=wp[ind2+3];

            k2=k2+4;

        }
    }

    return numinliers;
}





/*\}*/

#endif /* DB_METRICS */
