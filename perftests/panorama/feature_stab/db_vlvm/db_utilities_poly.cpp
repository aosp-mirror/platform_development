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

/* $Id: db_utilities_poly.cpp,v 1.2 2010/09/03 12:00:10 bsouthall Exp $ */

#include "db_utilities_poly.h"
#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

void db_SolveCubic(double *roots,int *nr_roots,double a,double b,double c,double d)
{
    double bp,bp2,cp,dp,q,r,srq;
    double r2_min_q3,theta,bp_through3,theta_through3;
    double cos_theta_through3,sin_theta_through3,min2_cos_theta_plu,min2_cos_theta_min;
    double si_r_srq,A;

    /*For nondegenerate cubics with three roots
    [24 mult 9 add 2sqrt 1acos 1cos=33flops 4func]
    For nondegenerate cubics with one root
    [16 mult 6 add 1sqrt 1qbrt=24flops 3func]*/

    if(a==0.0) db_SolveQuadratic(roots,nr_roots,b,c,d);
    else
    {
        bp=b/a;
        bp2=bp*bp;
        cp=c/a;
        dp=d/a;

        q=(bp2-3.0*cp)/9.0;
        r=(2.0*bp2*bp-9.0*bp*cp+27.0*dp)/54.0;
        r2_min_q3=r*r-q*q*q;
        if(r2_min_q3<0.0)
        {
            *nr_roots=3;
            /*q has to be > 0*/
            srq=sqrt(q);
            theta=acos(db_maxd(-1.0,db_mind(1.0,r/(q*srq))));
            bp_through3=bp/3.0;
            theta_through3=theta/3.0;
            cos_theta_through3=cos(theta_through3);
            sin_theta_through3=sqrt(db_maxd(0.0,1.0-cos_theta_through3*cos_theta_through3));

            /*cos(theta_through3+2*pi/3)=cos_theta_through3*cos(2*pi/3)-sin_theta_through3*sin(2*pi/3)
            = -0.5*cos_theta_through3-sqrt(3)/2.0*sin_theta_through3
            = -0.5*(cos_theta_through3+sqrt(3)*sin_theta_through3)*/
            min2_cos_theta_plu=cos_theta_through3+DB_SQRT3*sin_theta_through3;
            min2_cos_theta_min=cos_theta_through3-DB_SQRT3*sin_theta_through3;

            roots[0]= -2.0*srq*cos_theta_through3-bp_through3;
            roots[1]=srq*min2_cos_theta_plu-bp_through3;
            roots[2]=srq*min2_cos_theta_min-bp_through3;
        }
        else if(r2_min_q3>0.0)
        {
            *nr_roots=1;
            A= -db_sign(r)*db_CubRoot(db_absd(r)+sqrt(r2_min_q3));
            bp_through3=bp/3.0;
            if(A!=0.0) roots[0]=A+q/A-bp_through3;
            else roots[0]= -bp_through3;
        }
        else
        {
            *nr_roots=2;
            bp_through3=bp/3.0;
            /*q has to be >= 0*/
            si_r_srq=db_sign(r)*sqrt(q);
            /*Single root*/
            roots[0]= -2.0*si_r_srq-bp_through3;
            /*Double root*/
            roots[1]=si_r_srq-bp_through3;
        }
    }
}

void db_SolveQuartic(double *roots,int *nr_roots,double a,double b,double c,double d,double e)
{
    /*Normalized coefficients*/
    double c0,c1,c2,c3;
    /*Temporary coefficients*/
    double c3through2,c3through4,c3c3through4_min_c2,min4_c0;
    double lz,ms,ns,mn,m,n,lz_through2;
    /*Cubic polynomial roots, nr of roots and coefficients*/
    double c_roots[3];
    int nr_c_roots;
    double k0,k1;
    /*nr additional roots from second quadratic*/
    int addroots;

    /*For nondegenerate quartics
    [16mult 11add 2sqrt 1cubic 2quadratic=74flops 8funcs]*/

    if(a==0.0) db_SolveCubic(roots,nr_roots,b,c,d,e);
    else if(e==0.0)
    {
        db_SolveCubic(roots,nr_roots,a,b,c,d);
        roots[*nr_roots]=0.0;
        *nr_roots+=1;
    }
    else
    {
        /*Compute normalized coefficients*/
        c3=b/a;
        c2=c/a;
        c1=d/a;
        c0=e/a;
        /*Compute temporary coefficients*/
        c3through2=c3/2.0;
        c3through4=c3/4.0;
        c3c3through4_min_c2=c3*c3through4-c2;
        min4_c0= -4.0*c0;
        /*Compute coefficients of cubic*/
        k0=min4_c0*c3c3through4_min_c2-c1*c1;
        k1=c1*c3+min4_c0;
        /*k2= -c2*/
        /*k3=1.0*/

        /*Solve it for roots*/
        db_SolveCubic(c_roots,&nr_c_roots,1.0,-c2,k1,k0);

        if(nr_c_roots>0)
        {
            lz=c_roots[0];
            lz_through2=lz/2.0;
            ms=lz+c3c3through4_min_c2;
            ns=lz_through2*lz_through2-c0;
            mn=lz*c3through4-c1/2.0;

            if((ms>=0.0)&&(ns>=0.0))
            {
                m=sqrt(ms);
                n=sqrt(ns)*db_sign(mn);

                db_SolveQuadratic(roots,nr_roots,
                    1.0,c3through2+m,lz_through2+n);

                db_SolveQuadratic(&roots[*nr_roots],&addroots,
                    1.0,c3through2-m,lz_through2-n);

                *nr_roots+=addroots;
            }
            else *nr_roots=0;
        }
        else *nr_roots=0;
    }
}

void db_SolveQuarticForced(double *roots,int *nr_roots,double a,double b,double c,double d,double e)
{
    /*Normalized coefficients*/
    double c0,c1,c2,c3;
    /*Temporary coefficients*/
    double c3through2,c3through4,c3c3through4_min_c2,min4_c0;
    double lz,ms,ns,mn,m,n,lz_through2;
    /*Cubic polynomial roots, nr of roots and coefficients*/
    double c_roots[3];
    int nr_c_roots;
    double k0,k1;
    /*nr additional roots from second quadratic*/
    int addroots;

    /*For nondegenerate quartics
    [16mult 11add 2sqrt 1cubic 2quadratic=74flops 8funcs]*/

    if(a==0.0) db_SolveCubic(roots,nr_roots,b,c,d,e);
    else if(e==0.0)
    {
        db_SolveCubic(roots,nr_roots,a,b,c,d);
        roots[*nr_roots]=0.0;
        *nr_roots+=1;
    }
    else
    {
        /*Compute normalized coefficients*/
        c3=b/a;
        c2=c/a;
        c1=d/a;
        c0=e/a;
        /*Compute temporary coefficients*/
        c3through2=c3/2.0;
        c3through4=c3/4.0;
        c3c3through4_min_c2=c3*c3through4-c2;
        min4_c0= -4.0*c0;
        /*Compute coefficients of cubic*/
        k0=min4_c0*c3c3through4_min_c2-c1*c1;
        k1=c1*c3+min4_c0;
        /*k2= -c2*/
        /*k3=1.0*/

        /*Solve it for roots*/
        db_SolveCubic(c_roots,&nr_c_roots,1.0,-c2,k1,k0);

        if(nr_c_roots>0)
        {
            lz=c_roots[0];
            lz_through2=lz/2.0;
            ms=lz+c3c3through4_min_c2;
            ns=lz_through2*lz_through2-c0;
            mn=lz*c3through4-c1/2.0;

            if(ms<0.0) ms=0.0;
            if(ns<0.0) ns=0.0;

            m=sqrt(ms);
            n=sqrt(ns)*db_sign(mn);

            db_SolveQuadratic(roots,nr_roots,
                1.0,c3through2+m,lz_through2+n);

            db_SolveQuadratic(&roots[*nr_roots],&addroots,
                1.0,c3through2-m,lz_through2-n);

            *nr_roots+=addroots;
        }
        else *nr_roots=0;
    }
}
