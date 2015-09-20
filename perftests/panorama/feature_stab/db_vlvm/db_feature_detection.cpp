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

/*$Id: db_feature_detection.cpp,v 1.4 2011/06/17 14:03:30 mbansal Exp $*/

/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

#include "db_utilities.h"
#include "db_feature_detection.h"
#ifdef _VERBOSE_
#include <iostream>
#endif
#include <float.h>

#define DB_SUB_PIXEL

#define BORDER 10 // 5

float** db_AllocStrengthImage_f(float **im,int w,int h)
{
    int i,n,aw;
    long c,size;
    float **img,*aim,*p;

    /*Determine number of 124 element chunks needed*/
    n=(db_maxi(1,w-6)+123)/124;
    /*Determine the total allocation width aw*/
    aw=n*124+8;
    /*Allocate*/
    size=aw*h+16;
    *im=new float [size];
    /*Clean up*/
    p=(*im);
    for(c=0;c<size;c++) p[c]=0.0;
    /*Get a 16 byte aligned pointer*/
    aim=db_AlignPointer_f(*im,16);
    /*Allocate pointer table*/
    img=new float* [h];
    /*Initialize the pointer table*/
    for(i=0;i<h;i++)
    {
        img[i]=aim+aw*i+1;
    }

    return(img);
}

void db_FreeStrengthImage_f(float *im,float **img,int h)
{
    delete [] im;
    delete [] img;
}

/*Compute derivatives Ix,Iy for a subrow of img with upper left (i,j) and width chunk_width
Memory references occur one pixel outside the subrow*/
inline void db_IxIyRow_f(float *Ix,float *Iy,const float * const *img,int i,int j,int chunk_width)
{
    int c;

    for(c=0;c<chunk_width;c++)
    {
        Ix[c]=img[i][j+c-1]-img[i][j+c+1];
        Iy[c]=img[i-1][j+c]-img[i+1][j+c];
    }
}

/*Compute derivatives Ix,Iy for a subrow of img with upper left (i,j) and width 128
Memory references occur one pixel outside the subrow*/
inline void db_IxIyRow_u(int *dxx,const unsigned char * const *img,int i,int j,int nc)
{
#ifdef DB_USE_MMX
    const unsigned char *r1,*r2,*r3;

    r1=img[i-1]+j; r2=img[i]+j; r3=img[i+1]+j;

    _asm
    {
        mov esi,16
        mov eax,r1
        mov ebx,r2
        mov ecx,r3
        mov edx,dxx

        /*Get bitmask into mm7*/
        mov       edi,7F7F7F7Fh
        movd      mm7,edi
        punpckldq mm7,mm7

loopstart:
        /***************dx part 1-12*********************************/
        movq       mm0,[eax]       /*1 Get upper*/
         pxor      mm6,mm6         /*2 Set to zero*/
        movq       mm1,[ecx]       /*3 Get lower*/
         psrlq     mm0,1           /*4 Shift*/
        psrlq      mm1,1           /*5 Shift*/
         pand      mm0,mm7         /*6 And*/
        movq       mm2,[ebx-1]     /*13 Get left*/
         pand      mm1,mm7         /*7 And*/
        psubb      mm0,mm1         /*8 Subtract*/
         pxor      mm5,mm5         /*14 Set to zero*/
        movq       mm1,mm0         /*9 Copy*/
         pcmpgtb   mm6,mm0         /*10 Create unpack mask*/
        movq       mm3,[ebx+1]     /*15 Get right*/
         punpcklbw mm0,mm6         /*11 Unpack low*/
        punpckhbw  mm1,mm6         /*12 Unpack high*/
        /***************dy part 13-24*********************************/
         movq      mm4,mm0         /*25 Copy dx*/
        psrlq      mm2,1           /*16 Shift*/
         pmullw    mm0,mm0         /*26 Multiply dx*dx*/
        psrlq      mm3,1           /*17 Shift*/
         pand      mm2,mm7         /*18 And*/
        pand       mm3,mm7         /*19 And*/
         /*Stall*/
        psubb      mm2,mm3         /*20 Subtract*/
         /*Stall*/
        movq       mm3,mm2         /*21 Copy*/
         pcmpgtb   mm5,mm2         /*22 Create unpack mask*/
        punpcklbw  mm2,mm5         /*23 Unpack low*/
         /*Stall*/
        punpckhbw  mm3,mm5         /*24 Unpack high*/
        /***************dxx dxy dyy low part 25-49*********************************/
         pmullw    mm4,mm2         /*27 Multiply dx*dy*/
        pmullw     mm2,mm2         /*28 Multiply dy*dy*/
         pxor      mm6,mm6         /*29 Set to zero*/
        movq       mm5,mm0         /*30 Copy dx*dx*/
         pcmpgtw   mm6,mm0         /*31 Create unpack mask for dx*dx*/
        punpcklwd  mm0,mm6         /*32 Unpack dx*dx lows*/
         /*Stall*/
        punpckhwd  mm5,mm6         /*33 Unpack dx*dx highs*/
         pxor      mm6,mm6         /*36 Set to zero*/
        movq       [edx],mm0       /*34 Store dx*dx lows*/
         movq      mm0,mm4         /*37 Copy dx*dy*/
        movq       [edx+8],mm5     /*35 Store dx*dx highs*/
         pcmpgtw   mm6,mm4         /*38 Create unpack mask for dx*dy*/
        punpcklwd  mm4,mm6         /*39 Unpack dx*dy lows*/
         /*Stall*/
        punpckhwd  mm0,mm6         /*40 Unpack dx*dy highs*/
         pxor      mm6,mm6         /*43 Set to zero*/
        movq       [edx+512],mm4   /*41 Store dx*dy lows*/
         movq      mm5,mm2         /*44 Copy dy*dy*/
        movq       [edx+520],mm0   /*42 Store dx*dy highs*/
         pcmpgtw   mm6,mm2         /*45 Create unpack mask for dy*dy*/
        punpcklwd  mm2,mm6         /*46 Unpack dy*dy lows*/
         movq      mm4,mm1         /*50 Copy dx*/
        punpckhwd  mm5,mm6         /*47 Unpack dy*dy highs*/
         pmullw    mm1,mm1         /*51 Multiply dx*dx*/
        movq       [edx+1024],mm2  /*48 Store dy*dy lows*/
         pmullw    mm4,mm3         /*52 Multiply dx*dy*/
        movq       [edx+1032],mm5  /*49 Store dy*dy highs*/
        /***************dxx dxy dyy high part 50-79*********************************/
         pmullw    mm3,mm3         /*53 Multiply dy*dy*/
        pxor       mm6,mm6         /*54 Set to zero*/
         movq      mm5,mm1         /*55 Copy dx*dx*/
        pcmpgtw    mm6,mm1         /*56 Create unpack mask for dx*dx*/
         pxor      mm2,mm2         /*61 Set to zero*/
        punpcklwd  mm1,mm6         /*57 Unpack dx*dx lows*/
         movq      mm0,mm4         /*62 Copy dx*dy*/
        punpckhwd  mm5,mm6         /*58 Unpack dx*dx highs*/
         pcmpgtw   mm2,mm4         /*63 Create unpack mask for dx*dy*/
        movq       [edx+16],mm1    /*59 Store dx*dx lows*/
         punpcklwd mm4,mm2         /*64 Unpack dx*dy lows*/
        movq       [edx+24],mm5    /*60 Store dx*dx highs*/
         punpckhwd mm0,mm2         /*65 Unpack dx*dy highs*/
        movq       [edx+528],mm4   /*66 Store dx*dy lows*/
         pxor      mm6,mm6         /*68 Set to zero*/
        movq       [edx+536],mm0   /*67 Store dx*dy highs*/
         movq      mm5,mm3         /*69 Copy dy*dy*/
        pcmpgtw    mm6,mm3         /*70 Create unpack mask for dy*dy*/
         add       eax,8           /*75*/
        punpcklwd  mm3,mm6         /*71 Unpack dy*dy lows*/
         add       ebx,8           /*76*/
        punpckhwd  mm5,mm6         /*72 Unpack dy*dy highs*/
         add       ecx,8           /*77*/
        movq       [edx+1040],mm3  /*73 Store dy*dy lows*/
         /*Stall*/
        movq       [edx+1048],mm5  /*74 Store dy*dy highs*/
         /*Stall*/
        add        edx,32          /*78*/
         dec esi                   /*79*/
        jnz loopstart

        emms
    }

#else
    int c;
    int Ix,Iy;

    for(c=0;c<nc;c++)
    {
        Ix=(img[i][j+c-1]-img[i][j+c+1])>>1;
        Iy=(img[i-1][j+c]-img[i+1][j+c])>>1;
        dxx[c]=Ix*Ix;
        dxx[c+128]=Ix*Iy;
        dxx[c+256]=Iy*Iy;
    }
#endif /*DB_USE_MMX*/
}

/*Filter vertically five rows of derivatives of length chunk_width into gxx,gxy,gyy*/
inline void db_gxx_gxy_gyy_row_f(float *gxx,float *gxy,float *gyy,int chunk_width,
                                 float *Ix0,float *Ix1,float *Ix2,float *Ix3,float *Ix4,
                                 float *Iy0,float *Iy1,float *Iy2,float *Iy3,float *Iy4)
{
    int c;
    float dx,dy;
    float Ixx0,Ixy0,Iyy0,Ixx1,Ixy1,Iyy1,Ixx2,Ixy2,Iyy2,Ixx3,Ixy3,Iyy3,Ixx4,Ixy4,Iyy4;

    for(c=0;c<chunk_width;c++)
    {
        dx=Ix0[c];
        dy=Iy0[c];
        Ixx0=dx*dx;
        Ixy0=dx*dy;
        Iyy0=dy*dy;

        dx=Ix1[c];
        dy=Iy1[c];
        Ixx1=dx*dx;
        Ixy1=dx*dy;
        Iyy1=dy*dy;

        dx=Ix2[c];
        dy=Iy2[c];
        Ixx2=dx*dx;
        Ixy2=dx*dy;
        Iyy2=dy*dy;

        dx=Ix3[c];
        dy=Iy3[c];
        Ixx3=dx*dx;
        Ixy3=dx*dy;
        Iyy3=dy*dy;

        dx=Ix4[c];
        dy=Iy4[c];
        Ixx4=dx*dx;
        Ixy4=dx*dy;
        Iyy4=dy*dy;

        /*Filter vertically*/
        gxx[c]=Ixx0+Ixx1*4.0f+Ixx2*6.0f+Ixx3*4.0f+Ixx4;
        gxy[c]=Ixy0+Ixy1*4.0f+Ixy2*6.0f+Ixy3*4.0f+Ixy4;
        gyy[c]=Iyy0+Iyy1*4.0f+Iyy2*6.0f+Iyy3*4.0f+Iyy4;
    }
}

/*Filter vertically five rows of derivatives of length 128 into gxx,gxy,gyy*/
inline void db_gxx_gxy_gyy_row_s(int *g,int *d0,int *d1,int *d2,int *d3,int *d4,int nc)
{
#ifdef DB_USE_MMX
    int c;

    _asm
    {
        mov c,64
        mov eax,d0
        mov ebx,d1
        mov ecx,d2
        mov edx,d3
        mov edi,d4
        mov esi,g

loopstart:
        /***************dxx part 1-14*********************************/
        movq        mm0,[eax]      /*1 Get dxx0*/
         /*Stall*/
        movq        mm1,[ebx]      /*2 Get dxx1*/
         /*Stall*/
        movq        mm2,[ecx]      /*5 Get dxx2*/
         pslld      mm1,2          /*3 Shift dxx1*/
        movq        mm3,[edx]      /*10 Get dxx3*/
         paddd      mm0,mm1        /*4 Accumulate dxx1*/
        movq        mm4,[eax+512]  /*15 Get dxy0*/
         pslld      mm2,1          /*6 Shift dxx2 1*/
        paddd       mm0,mm2        /*7 Accumulate dxx2 1*/
         pslld      mm2,1          /*8 Shift dxx2 2*/
        movq        mm5,[ebx+512]  /*16 Get dxy1*/
         paddd      mm0,mm2        /*9 Accumulate dxx2 2*/
        pslld       mm3,2          /*11 Shift dxx3*/
         /*Stall*/
        paddd       mm0,mm3        /*12 Accumulate dxx3*/
         pslld      mm5,2          /*17 Shift dxy1*/
        paddd       mm0,[edi]      /*13 Accumulate dxx4*/
         paddd      mm4,mm5        /*18 Accumulate dxy1*/
        movq        mm6,[ecx+512]  /*19 Get dxy2*/
         /*Stall*/
        movq        [esi],mm0      /*14 Store dxx sums*/
        /***************dxy part 15-28*********************************/
         pslld      mm6,1          /*20 Shift dxy2 1*/
        paddd       mm4,mm6        /*21 Accumulate dxy2 1*/
         pslld      mm6,1          /*22 Shift dxy2 2*/
        movq        mm0,[eax+1024] /*29 Get dyy0*/
         paddd      mm4,mm6        /*23 Accumulate dxy2 2*/
        movq        mm7,[edx+512]  /*24 Get dxy3*/
         pslld      mm7,2          /*25 Shift dxy3*/
        movq        mm1,[ebx+1024] /*30 Get dyy1*/
         paddd      mm4,mm7        /*26 Accumulate dxy3*/
        paddd       mm4,[edi+512]  /*27 Accumulate dxy4*/
         pslld      mm1,2          /*31 Shift dyy1*/
        movq        mm2,[ecx+1024] /*33 Get dyy2*/
         paddd      mm0,mm1        /*32 Accumulate dyy1*/
        movq        [esi+512],mm4  /*28 Store dxy sums*/
         pslld      mm2,1          /*34 Shift dyy2 1*/
        /***************dyy part 29-49*********************************/


        movq        mm3,[edx+1024] /*38 Get dyy3*/
         paddd      mm0,mm2        /*35 Accumulate dyy2 1*/
        paddd       mm0,[edi+1024] /*41 Accumulate dyy4*/
         pslld      mm2,1          /*36 Shift dyy2 2*/
        paddd       mm0,mm2        /*37 Accumulate dyy2 2*/
         pslld      mm3,2          /*39 Shift dyy3*/
        paddd       mm0,mm3        /*40 Accumulate dyy3*/
         add        eax,8           /*43*/
        add         ebx,8           /*44*/
         add        ecx,8           /*45*/
        movq        [esi+1024],mm0 /*42 Store dyy sums*/
         /*Stall*/
        add         edx,8           /*46*/
         add        edi,8           /*47*/
        add         esi,8           /*48*/
         dec        c               /*49*/
        jnz         loopstart

        emms
    }

#else
    int c,dd;

    for(c=0;c<nc;c++)
    {
        /*Filter vertically*/
        dd=d2[c];
        g[c]=d0[c]+(d1[c]<<2)+(dd<<2)+(dd<<1)+(d3[c]<<2)+d4[c];

        dd=d2[c+128];
        g[c+128]=d0[c+128]+(d1[c+128]<<2)+(dd<<2)+(dd<<1)+(d3[c+128]<<2)+d4[c+128];

        dd=d2[c+256];
        g[c+256]=d0[c+256]+(d1[c+256]<<2)+(dd<<2)+(dd<<1)+(d3[c+256]<<2)+d4[c+256];
    }
#endif /*DB_USE_MMX*/
}

/*Filter horizontally the three rows gxx,gxy,gyy into the strength subrow starting at i,j
and with width chunk_width. gxx,gxy and gyy are assumed to be four pixels wider than chunk_width
and starting at (i,j-2)*/
inline void db_HarrisStrength_row_f(float **s,float *gxx,float *gxy,float *gyy,int i,int j,int chunk_width)
{
    float Gxx,Gxy,Gyy,det,trc;
    int c;

    for(c=0;c<chunk_width;c++)
    {
        Gxx=gxx[c]+gxx[c+1]*4.0f+gxx[c+2]*6.0f+gxx[c+3]*4.0f+gxx[c+4];
        Gxy=gxy[c]+gxy[c+1]*4.0f+gxy[c+2]*6.0f+gxy[c+3]*4.0f+gxy[c+4];
        Gyy=gyy[c]+gyy[c+1]*4.0f+gyy[c+2]*6.0f+gyy[c+3]*4.0f+gyy[c+4];

        det=Gxx*Gyy-Gxy*Gxy;
        trc=Gxx+Gyy;
        s[i][j+c]=det-0.06f*trc*trc;
    }
}

/*Filter g of length 128 in place with 14641. Output is shifted two steps
and of length 124*/
inline void db_Filter14641_128_i(int *g,int nc)
{
#ifdef DB_USE_MMX
    int mask;

    mask=0xFFFFFFFF;
    _asm
    {
        mov esi,31
        mov eax,g

        /*Get bitmask 00000000FFFFFFFF into mm7*/
        movd mm7,mask

        /*Warming iteration one 1-16********************/
        movq       mm6,[eax]      /*1 Load new data*/
        paddd      mm0,mm6        /*2 Add 1* behind two steps*/
        movq       mm2,mm6        /*3 Start with 1* in front two steps*/
        pslld      mm6,1          /*4*/
        paddd      mm1,mm6        /*5 Add 2* same place*/
        pslld      mm6,1          /*6*/
        paddd      mm1,mm6        /*7 Add 4* same place*/
        pshufw     mm6,mm6,4Eh    /*8 Swap the two double-words using bitmask 01001110=4Eh*/
        paddd      mm1,mm6        /*9 Add 4* swapped*/
        movq       mm5,mm6        /*10 Copy*/
        pand       mm6,mm7        /*11 Get low double-word only*/
        paddd      mm2,mm6        /*12 Add 4* in front one step*/
        pxor       mm6,mm5        /*13 Get high double-word only*/
        paddd      mm0,mm6        /*14 Add 4* behind one step*/
        movq       mm0,mm1        /*15 Shift along*/
        movq       mm1,mm2        /*16 Shift along*/
        /*Warming iteration two 17-32********************/
        movq       mm4,[eax+8]    /*17 Load new data*/
        paddd      mm0,mm4        /*18 Add 1* behind two steps*/
        movq       mm2,mm4        /*19 Start with 1* in front two steps*/
        pslld      mm4,1          /*20*/
        paddd      mm1,mm4        /*21 Add 2* same place*/
        pslld      mm4,1          /*22*/
        paddd      mm1,mm4        /*23 Add 4* same place*/
        pshufw     mm4,mm4,4Eh    /*24 Swap the two double-words using bitmask 01001110=4Eh*/
        paddd      mm1,mm4        /*25 Add 4* swapped*/
        movq       mm3,mm4        /*26 Copy*/
        pand       mm4,mm7        /*27 Get low double-word only*/
        paddd      mm2,mm4        /*28 Add 4* in front one step*/
        pxor       mm4,mm3        /*29 Get high double-word only*/
        paddd      mm0,mm4        /*30 Add 4* behind one step*/
        movq       mm0,mm1        /*31 Shift along*/
        movq       mm1,mm2        /*32 Shift along*/

        /*Loop********************/
loopstart:
        /*First part of loop 33-47********/
        movq        mm6,[eax+16]   /*33 Load new data*/
         /*Stall*/
        paddd       mm0,mm6        /*34 Add 1* behind two steps*/
         movq       mm2,mm6        /*35 Start with 1* in front two steps*/
        movq        mm4,[eax+24]   /*48 Load new data*/
         pslld      mm6,1          /*36*/
        paddd       mm1,mm6        /*37 Add 2* same place*/
         pslld      mm6,1          /*38*/
        paddd       mm1,mm6        /*39 Add 4* same place*/
         pshufw     mm6,mm6,4Eh    /*40 Swap the two double-words using bitmask 01001110=4Eh*/
        paddd       mm1,mm4        /*49 Add 1* behind two steps*/
         movq       mm5,mm6        /*41 Copy*/
        paddd       mm1,mm6        /*42 Add 4* swapped*/
         pand       mm6,mm7        /*43 Get low double-word only*/
        paddd       mm2,mm6        /*44 Add 4* in front one step*/
         pxor       mm6,mm5        /*45 Get high double-word only*/
        paddd       mm0,mm6        /*46 Add 4* behind one step*/
         movq       mm6,mm4        /*50a Copy*/
        pslld       mm4,1          /*51*/
         /*Stall*/
        movq        [eax],mm0      /*47 Store result two steps behind*/
        /*Second part of loop 48-66********/
         movq       mm0,mm6        /*50b Start with 1* in front two steps*/
        paddd       mm2,mm4        /*52 Add 2* same place*/
         pslld      mm4,1          /*53*/
        paddd       mm2,mm4        /*54 Add 4* same place*/
         pshufw     mm4,mm4,4Eh    /*55 Swap the two double-words using bitmask 01001110=4Eh*/
        paddd       mm2,mm4        /*56 Add 4* swapped*/
         movq       mm3,mm4        /*57 Copy*/
        pand        mm4,mm7        /*58 Get low double-word only*/
         /*Stall*/
        paddd       mm0,mm4        /*59 Add 4* in front one step*/
         pxor       mm4,mm3        /*60 Get high double-word only*/
        paddd       mm1,mm4        /*61 Add 4* behind one step*/
         add        eax,16         /*65*/
        dec         esi            /*66*/
         /*Stall*/
        movq        [eax-8],mm1    /*62 Store result two steps behind*/
         movq       mm1,mm0        /*63 Shift along*/
        movq        mm0,mm2        /*64 Shift along*/
        jnz loopstart

        emms
    }

#else
    int c;

    for(c=0;c<nc-4;c++)
    {
        g[c]=g[c]+(g[c+1]<<2)+(g[c+2]<<2)+(g[c+2]<<1)+(g[c+3]<<2)+g[c+4];
    }
#endif /*DB_USE_MMX*/
}

/*Filter horizontally the three rows gxx,gxy,gyy of length 128 into the strength subrow s
of length 124. gxx,gxy and gyy are assumed to be starting at (i,j-2) if s[i][j] is sought.
s should be 16 byte aligned*/
inline void db_HarrisStrength_row_s(float *s,int *gxx,int *gxy,int *gyy,int nc)
{
    float k;

    k=0.06f;

    db_Filter14641_128_i(gxx,nc);
    db_Filter14641_128_i(gxy,nc);
    db_Filter14641_128_i(gyy,nc);

#ifdef DB_USE_SIMD


    _asm
    {
        mov esi,15
        mov eax,gxx
        mov ebx,gxy
        mov ecx,gyy
        mov edx,s

        /*broadcast k to all positions of xmm7*/
        movss   xmm7,k
        shufps  xmm7,xmm7,0

        /*****Warm up 1-10**************************************/
        cvtpi2ps  xmm0,[eax+8] /*1 Convert two integers into floating point of low double-word*/
         /*Stall*/
        cvtpi2ps  xmm1,[ebx+8] /*4 Convert two integers into floating point of low double-word*/
         movlhps  xmm0,xmm0    /*2 Move them to the high double-word*/
        cvtpi2ps  xmm2,[ecx+8] /*7 Convert two integers into floating point of low double-word*/
         movlhps  xmm1,xmm1    /*5 Move them to the high double-word*/
        cvtpi2ps  xmm0,[eax]   /*3 Convert two integers into floating point of low double-word*/
         movlhps  xmm2,xmm2    /*8 Move them to the high double-word*/
        cvtpi2ps  xmm1,[ebx]   /*6 Convert two integers into floating point of low double-word*/
         movaps   xmm3,xmm0    /*10 Copy Cxx*/
        cvtpi2ps  xmm2,[ecx]   /*9 Convert two integers into floating point of low double-word*/
         /*Stall*/
loopstart:
        /*****First part of loop 11-18***********************/
        mulps     xmm0,xmm2     /*11 Multiply to get Gxx*Gyy*/
         addps    xmm2,xmm3     /*12 Add to get Gxx+Gyy*/
        cvtpi2ps  xmm4,[eax+24] /*19 Convert two integers into floating point of low double-word*/
         mulps    xmm1,xmm1     /*13 Multiply to get Gxy*Gxy*/
        mulps     xmm2,xmm2     /*14 Multiply to get (Gxx+Gyy)*(Gxx+Gyy)*/
         movlhps  xmm4,xmm4     /*20 Move them to the high double-word*/
        cvtpi2ps  xmm4,[eax+16] /*21 Convert two integers into floating point of low double-word*/
         /*Stall*/
        subps     xmm0,xmm1     /*15 Subtract to get Gxx*Gyy-Gxy*Gxy*/
         mulps    xmm2,xmm7     /*16 Multiply to get k*(Gxx+Gyy)*(Gxx+Gyy)*/
        cvtpi2ps  xmm5,[ebx+24] /*22 Convert two integers into floating point of low double-word*/
         /*Stall*/
        movlhps   xmm5,xmm5     /*23 Move them to the high double-word*/
         /*Stall*/
        cvtpi2ps  xmm5,[ebx+16] /*24 Convert two integers into floating point of low double-word*/
         subps    xmm0,xmm2     /*17 Subtract to get Gxx*Gyy-Gxy*Gxy-k*(Gxx+Gyy)*(Gxx+Gyy)*/
        cvtpi2ps  xmm6,[ecx+24] /*25 Convert two integers into floating point of low double-word*/
         /*Stall*/
        movaps    [edx],xmm0    /*18 Store*/
        /*****Second part of loop 26-40***********************/
         movlhps  xmm6,xmm6     /*26 Move them to the high double-word*/
        cvtpi2ps  xmm6,[ecx+16] /*27 Convert two integers into floating point of low double-word*/
         movaps   xmm3,xmm4     /*28 Copy Cxx*/
        mulps     xmm4,xmm6     /*29 Multiply to get Gxx*Gyy*/
         addps    xmm6,xmm3     /*30 Add to get Gxx+Gyy*/
        cvtpi2ps  xmm0,[eax+40] /*(1 Next) Convert two integers into floating point of low double-word*/
         mulps    xmm5,xmm5     /*31 Multiply to get Gxy*Gxy*/
        cvtpi2ps  xmm1,[ebx+40] /*(4 Next) Convert two integers into floating point of low double-word*/
         mulps    xmm6,xmm6     /*32 Multiply to get (Gxx+Gyy)*(Gxx+Gyy)*/
        cvtpi2ps  xmm2,[ecx+40] /*(7 Next) Convert two integers into floating point of low double-word*/
         movlhps  xmm0,xmm0     /*(2 Next) Move them to the high double-word*/
        subps     xmm4,xmm5     /*33 Subtract to get Gxx*Gyy-Gxy*Gxy*/
         movlhps  xmm1,xmm1     /*(5 Next) Move them to the high double-word*/
        cvtpi2ps  xmm0,[eax+32] /*(3 Next)Convert two integers into floating point of low double-word*/
         mulps    xmm6,xmm7     /*34 Multiply to get k*(Gxx+Gyy)*(Gxx+Gyy)*/
        cvtpi2ps  xmm1,[ebx+32] /*(6 Next) Convert two integers into floating point of low double-word*/
         movlhps  xmm2,xmm2     /*(8 Next) Move them to the high double-word*/
        movaps    xmm3,xmm0     /*(10 Next) Copy Cxx*/
         add      eax,32        /*37*/
        subps     xmm4,xmm6     /*35 Subtract to get Gxx*Gyy-Gxy*Gxy-k*(Gxx+Gyy)*(Gxx+Gyy)*/
         add      ebx,32        /*38*/
        cvtpi2ps  xmm2,[ecx+32] /*(9 Next) Convert two integers into floating point of low double-word*/
         /*Stall*/
        movaps    [edx+16],xmm4 /*36 Store*/
         /*Stall*/
        add       ecx,32        /*39*/
         add      edx,32        /*40*/
        dec       esi           /*41*/
        jnz loopstart

        /****Cool down***************/
        mulps    xmm0,xmm2    /*Multiply to get Gxx*Gyy*/
        addps    xmm2,xmm3    /*Add to get Gxx+Gyy*/
        mulps    xmm1,xmm1    /*Multiply to get Gxy*Gxy*/
        mulps    xmm2,xmm2    /*Multiply to get (Gxx+Gyy)*(Gxx+Gyy)*/
        subps    xmm0,xmm1    /*Subtract to get Gxx*Gyy-Gxy*Gxy*/
        mulps    xmm2,xmm7    /*Multiply to get k*(Gxx+Gyy)*(Gxx+Gyy)*/
        subps    xmm0,xmm2    /*Subtract to get Gxx*Gyy-Gxy*Gxy-k*(Gxx+Gyy)*(Gxx+Gyy)*/
        movaps   [edx],xmm0   /*Store*/
    }

#else
    float Gxx,Gxy,Gyy,det,trc;
    int c;

    //for(c=0;c<124;c++)
    for(c=0;c<nc-4;c++)
    {
        Gxx=(float)gxx[c];
        Gxy=(float)gxy[c];
        Gyy=(float)gyy[c];

        det=Gxx*Gyy-Gxy*Gxy;
        trc=Gxx+Gyy;
        s[c]=det-k*trc*trc;
    }
#endif /*DB_USE_SIMD*/
}

/*Compute the Harris corner strength of the chunk [left,top,right,bottom] of img and
store it into the corresponding region of s. left and top have to be at least 3 and
right and bottom have to be at most width-4,height-4*/
inline void db_HarrisStrengthChunk_f(float **s,const float * const *img,int left,int top,int right,int bottom,
                                      /*temp should point to at least
                                      13*(right-left+5) of allocated memory*/
                                      float *temp)
{
    float *Ix[5],*Iy[5];
    float *gxx,*gxy,*gyy;
    int i,chunk_width,chunk_width_p4;

    chunk_width=right-left+1;
    chunk_width_p4=chunk_width+4;
    gxx=temp;
    gxy=gxx+chunk_width_p4;
    gyy=gxy+chunk_width_p4;
    for(i=0;i<5;i++)
    {
        Ix[i]=gyy+chunk_width_p4+(2*i*chunk_width_p4);
        Iy[i]=Ix[i]+chunk_width_p4;
    }

    /*Fill four rows of the wrap-around derivative buffers*/
    for(i=top-2;i<top+2;i++) db_IxIyRow_f(Ix[i%5],Iy[i%5],img,i,left-2,chunk_width_p4);

    /*For each output row*/
    for(i=top;i<=bottom;i++)
    {
        /*Step the derivative buffers*/
        db_IxIyRow_f(Ix[(i+2)%5],Iy[(i+2)%5],img,(i+2),left-2,chunk_width_p4);

        /*Filter Ix2,IxIy,Iy2 vertically into gxx,gxy,gyy*/
        db_gxx_gxy_gyy_row_f(gxx,gxy,gyy,chunk_width_p4,
                                 Ix[(i-2)%5],Ix[(i-1)%5],Ix[i%5],Ix[(i+1)%5],Ix[(i+2)%5],
                                 Iy[(i-2)%5],Iy[(i-1)%5],Iy[i%5],Iy[(i+1)%5],Iy[(i+2)%5]);

        /*Filter gxx,gxy,gyy horizontally and compute corner response s*/
        db_HarrisStrength_row_f(s,gxx,gxy,gyy,i,left,chunk_width);
    }
}

/*Compute the Harris corner strength of the chunk [left,top,left+123,bottom] of img and
store it into the corresponding region of s. left and top have to be at least 3 and
right and bottom have to be at most width-4,height-4. The left of the region in s should
be 16 byte aligned*/
inline void db_HarrisStrengthChunk_u(float **s,const unsigned char * const *img,int left,int top,int bottom,
                                      /*temp should point to at least
                                      18*128 of allocated memory*/
                                      int *temp, int nc)
{
    int *Ixx[5],*Ixy[5],*Iyy[5];
    int *gxx,*gxy,*gyy;
    int i;

    gxx=temp;
    gxy=gxx+128;
    gyy=gxy+128;
    for(i=0;i<5;i++)
    {
        Ixx[i]=gyy+(3*i+1)*128;
        Ixy[i]=gyy+(3*i+2)*128;
        Iyy[i]=gyy+(3*i+3)*128;
    }

    /*Fill four rows of the wrap-around derivative buffers*/
    for(i=top-2;i<top+2;i++) db_IxIyRow_u(Ixx[i%5],img,i,left-2,nc);

    /*For each output row*/
    for(i=top;i<=bottom;i++)
    {
        /*Step the derivative buffers*/
        db_IxIyRow_u(Ixx[(i+2)%5],img,(i+2),left-2,nc);

        /*Filter Ix2,IxIy,Iy2 vertically into gxx,gxy,gyy*/
        db_gxx_gxy_gyy_row_s(gxx,Ixx[(i-2)%5],Ixx[(i-1)%5],Ixx[i%5],Ixx[(i+1)%5],Ixx[(i+2)%5],nc);

        /*Filter gxx,gxy,gyy horizontally and compute corner response s*/
        db_HarrisStrength_row_s(s[i]+left,gxx,gxy,gyy,nc);
    }

}

/*Compute Harris corner strength of img. Strength is returned for the region
with (3,3) as upper left and (w-4,h-4) as lower right, positioned in the
same place in s. In other words,image should be at least 7 pixels wide and 7 pixels high
for a meaningful result*/
void db_HarrisStrength_f(float **s,const float * const *img,int w,int h,
                                    /*temp should point to at least
                                    13*(chunk_width+4) of allocated memory*/
                                    float *temp,
                                    int chunk_width)
{
    int x,next_x,last,right;

    last=w-4;
    for(x=3;x<=last;x=next_x)
    {
        next_x=x+chunk_width;
        right=next_x-1;
        if(right>last) right=last;
        /*Compute the Harris strength of a chunk*/
        db_HarrisStrengthChunk_f(s,img,x,3,right,h-4,temp);
    }
}

/*Compute Harris corner strength of img. Strength is returned for the region
with (3,3) as upper left and (w-4,h-4) as lower right, positioned in the
same place in s. In other words,image should be at least 7 pixels wide and 7 pixels high
for a meaningful result.Moreover, the image should be overallocated by 256 bytes.
s[i][3] should by 16 byte aligned for any i*/
void db_HarrisStrength_u(float **s, const unsigned char * const *img,int w,int h,
                                    /*temp should point to at least
                                    18*128 of allocated memory*/
                                    int *temp)
{
    int x,next_x,last;
    int nc;

    last=w-4;
    for(x=3;x<=last;x=next_x)
    {
        next_x=x+124;

        // mayban: to revert to the original full chunks state, change the line below to: nc = 128;
        nc = db_mini(128,last-x+1);
        //nc = 128;

        /*Compute the Harris strength of a chunk*/
        db_HarrisStrengthChunk_u(s,img,x,3,h-4,temp,nc);
    }
}

inline float db_Max_128Aligned16_f(float *v)
{
#ifdef DB_USE_SIMD
    float back;

    _asm
    {
        mov eax,v

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]
        movaps xmm2,[eax+32]
        movaps xmm3,[eax+48]
        movaps xmm4,[eax+64]
        movaps xmm5,[eax+80]
        movaps xmm6,[eax+96]
        movaps xmm7,[eax+112]

        /*Chunk2*/
        maxps xmm0,[eax+128]
        maxps xmm1,[eax+144]
        maxps xmm2,[eax+160]
        maxps xmm3,[eax+176]
        maxps xmm4,[eax+192]
        maxps xmm5,[eax+208]
        maxps xmm6,[eax+224]
        maxps xmm7,[eax+240]

        /*Chunk3*/
        maxps xmm0,[eax+256]
        maxps xmm1,[eax+272]
        maxps xmm2,[eax+288]
        maxps xmm3,[eax+304]
        maxps xmm4,[eax+320]
        maxps xmm5,[eax+336]
        maxps xmm6,[eax+352]
        maxps xmm7,[eax+368]

        /*Chunk4*/
        maxps xmm0,[eax+384]
        maxps xmm1,[eax+400]
        maxps xmm2,[eax+416]
        maxps xmm3,[eax+432]
        maxps xmm4,[eax+448]
        maxps xmm5,[eax+464]
        maxps xmm6,[eax+480]
        maxps xmm7,[eax+496]

        /*Collect*/
        maxps   xmm0,xmm1
        maxps   xmm2,xmm3
        maxps   xmm4,xmm5
        maxps   xmm6,xmm7
        maxps   xmm0,xmm2
        maxps   xmm4,xmm6
        maxps   xmm0,xmm4
        movhlps xmm1,xmm0
        maxps   xmm0,xmm1
        shufps  xmm1,xmm0,1
        maxps   xmm0,xmm1
        movss   back,xmm0
    }

    return(back);
#else
    float val,max_val;
    float *p,*stop_p;
    max_val=v[0];
    for(p=v+1,stop_p=v+128;p!=stop_p;)
    {
        val= *p++;
        if(val>max_val) max_val=val;
    }
    return(max_val);
#endif /*DB_USE_SIMD*/
}

inline float db_Max_64Aligned16_f(float *v)
{
#ifdef DB_USE_SIMD
    float back;

    _asm
    {
        mov eax,v

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]
        movaps xmm2,[eax+32]
        movaps xmm3,[eax+48]
        movaps xmm4,[eax+64]
        movaps xmm5,[eax+80]
        movaps xmm6,[eax+96]
        movaps xmm7,[eax+112]

        /*Chunk2*/
        maxps xmm0,[eax+128]
        maxps xmm1,[eax+144]
        maxps xmm2,[eax+160]
        maxps xmm3,[eax+176]
        maxps xmm4,[eax+192]
        maxps xmm5,[eax+208]
        maxps xmm6,[eax+224]
        maxps xmm7,[eax+240]

        /*Collect*/
        maxps   xmm0,xmm1
        maxps   xmm2,xmm3
        maxps   xmm4,xmm5
        maxps   xmm6,xmm7
        maxps   xmm0,xmm2
        maxps   xmm4,xmm6
        maxps   xmm0,xmm4
        movhlps xmm1,xmm0
        maxps   xmm0,xmm1
        shufps  xmm1,xmm0,1
        maxps   xmm0,xmm1
        movss   back,xmm0
    }

    return(back);
#else
    float val,max_val;
    float *p,*stop_p;
    max_val=v[0];
    for(p=v+1,stop_p=v+64;p!=stop_p;)
    {
        val= *p++;
        if(val>max_val) max_val=val;
    }
    return(max_val);
#endif /*DB_USE_SIMD*/
}

inline float db_Max_32Aligned16_f(float *v)
{
#ifdef DB_USE_SIMD
    float back;

    _asm
    {
        mov eax,v

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]
        movaps xmm2,[eax+32]
        movaps xmm3,[eax+48]
        movaps xmm4,[eax+64]
        movaps xmm5,[eax+80]
        movaps xmm6,[eax+96]
        movaps xmm7,[eax+112]

        /*Collect*/
        maxps   xmm0,xmm1
        maxps   xmm2,xmm3
        maxps   xmm4,xmm5
        maxps   xmm6,xmm7
        maxps   xmm0,xmm2
        maxps   xmm4,xmm6
        maxps   xmm0,xmm4
        movhlps xmm1,xmm0
        maxps   xmm0,xmm1
        shufps  xmm1,xmm0,1
        maxps   xmm0,xmm1
        movss   back,xmm0
    }

    return(back);
#else
    float val,max_val;
    float *p,*stop_p;
    max_val=v[0];
    for(p=v+1,stop_p=v+32;p!=stop_p;)
    {
        val= *p++;
        if(val>max_val) max_val=val;
    }
    return(max_val);
#endif /*DB_USE_SIMD*/
}

inline float db_Max_16Aligned16_f(float *v)
{
#ifdef DB_USE_SIMD
    float back;

    _asm
    {
        mov eax,v

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]
        movaps xmm2,[eax+32]
        movaps xmm3,[eax+48]

        /*Collect*/
        maxps   xmm0,xmm1
        maxps   xmm2,xmm3
        maxps   xmm0,xmm2
        movhlps xmm1,xmm0
        maxps   xmm0,xmm1
        shufps  xmm1,xmm0,1
        maxps   xmm0,xmm1
        movss   back,xmm0
    }

    return(back);
#else
    float val,max_val;
    float *p,*stop_p;
    max_val=v[0];
    for(p=v+1,stop_p=v+16;p!=stop_p;)
    {
        val= *p++;
        if(val>max_val) max_val=val;
    }
    return(max_val);
#endif /*DB_USE_SIMD*/
}

inline float db_Max_8Aligned16_f(float *v)
{
#ifdef DB_USE_SIMD
    float back;

    _asm
    {
        mov eax,v

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]

        /*Collect*/
        maxps   xmm0,xmm1
        movhlps xmm1,xmm0
        maxps   xmm0,xmm1
        shufps  xmm1,xmm0,1
        maxps   xmm0,xmm1
        movss   back,xmm0
    }

    return(back);
#else
    float val,max_val;
    float *p,*stop_p;
    max_val=v[0];
    for(p=v+1,stop_p=v+8;p!=stop_p;)
    {
        val= *p++;
        if(val>max_val) max_val=val;
    }
    return(max_val);
#endif /*DB_USE_SIMD*/
}

inline float db_Max_Aligned16_f(float *v,int size)
{
    float val,max_val;
    float *stop_v;

    max_val=v[0];
    for(;size>=128;size-=128)
    {
        val=db_Max_128Aligned16_f(v);
        v+=128;
        if(val>max_val) max_val=val;
    }
    if(size&64)
    {
        val=db_Max_64Aligned16_f(v);
        v+=64;
        if(val>max_val) max_val=val;
    }
    if(size&32)
    {
        val=db_Max_32Aligned16_f(v);
        v+=32;
        if(val>max_val) max_val=val;
    }
    if(size&16)
    {
        val=db_Max_16Aligned16_f(v);
        v+=16;
        if(val>max_val) max_val=val;
    }
    if(size&8)
    {
        val=db_Max_8Aligned16_f(v);
        v+=8;
        if(val>max_val) max_val=val;
    }
    if(size&7)
    {
        for(stop_v=v+(size&7);v!=stop_v;)
        {
            val= *v++;
            if(val>max_val) max_val=val;
        }
    }

    return(max_val);
}

/*Find maximum value of img in the region starting at (left,top)
and with width w and height h. img[left] should be 16 byte aligned*/
float db_MaxImage_Aligned16_f(float **img,int left,int top,int w,int h)
{
    float val,max_val;
    int i,stop_i;

    if(w && h)
    {
        stop_i=top+h;
        max_val=img[top][left];

        for(i=top;i<stop_i;i++)
        {
            val=db_Max_Aligned16_f(img[i]+left,w);
            if(val>max_val) max_val=val;
        }
        return(max_val);
    }
    return(0.0);
}

inline void db_MaxVector_128_Aligned16_f(float *m,float *v1,float *v2)
{
#ifdef DB_USE_SIMD
    _asm
    {
        mov eax,v1
        mov ebx,v2
        mov ecx,m

        /*Chunk1*/
        movaps xmm0,[eax]
        movaps xmm1,[eax+16]
        movaps xmm2,[eax+32]
        movaps xmm3,[eax+48]
        movaps xmm4,[eax+64]
        movaps xmm5,[eax+80]
        movaps xmm6,[eax+96]
        movaps xmm7,[eax+112]
        maxps  xmm0,[ebx]
        maxps  xmm1,[ebx+16]
        maxps  xmm2,[ebx+32]
        maxps  xmm3,[ebx+48]
        maxps  xmm4,[ebx+64]
        maxps  xmm5,[ebx+80]
        maxps  xmm6,[ebx+96]
        maxps  xmm7,[ebx+112]
        movaps [ecx],xmm0
        movaps [ecx+16],xmm1
        movaps [ecx+32],xmm2
        movaps [ecx+48],xmm3
        movaps [ecx+64],xmm4
        movaps [ecx+80],xmm5
        movaps [ecx+96],xmm6
        movaps [ecx+112],xmm7

        /*Chunk2*/
        movaps xmm0,[eax+128]
        movaps xmm1,[eax+144]
        movaps xmm2,[eax+160]
        movaps xmm3,[eax+176]
        movaps xmm4,[eax+192]
        movaps xmm5,[eax+208]
        movaps xmm6,[eax+224]
        movaps xmm7,[eax+240]
        maxps  xmm0,[ebx+128]
        maxps  xmm1,[ebx+144]
        maxps  xmm2,[ebx+160]
        maxps  xmm3,[ebx+176]
        maxps  xmm4,[ebx+192]
        maxps  xmm5,[ebx+208]
        maxps  xmm6,[ebx+224]
        maxps  xmm7,[ebx+240]
        movaps [ecx+128],xmm0
        movaps [ecx+144],xmm1
        movaps [ecx+160],xmm2
        movaps [ecx+176],xmm3
        movaps [ecx+192],xmm4
        movaps [ecx+208],xmm5
        movaps [ecx+224],xmm6
        movaps [ecx+240],xmm7

        /*Chunk3*/
        movaps xmm0,[eax+256]
        movaps xmm1,[eax+272]
        movaps xmm2,[eax+288]
        movaps xmm3,[eax+304]
        movaps xmm4,[eax+320]
        movaps xmm5,[eax+336]
        movaps xmm6,[eax+352]
        movaps xmm7,[eax+368]
        maxps  xmm0,[ebx+256]
        maxps  xmm1,[ebx+272]
        maxps  xmm2,[ebx+288]
        maxps  xmm3,[ebx+304]
        maxps  xmm4,[ebx+320]
        maxps  xmm5,[ebx+336]
        maxps  xmm6,[ebx+352]
        maxps  xmm7,[ebx+368]
        movaps [ecx+256],xmm0
        movaps [ecx+272],xmm1
        movaps [ecx+288],xmm2
        movaps [ecx+304],xmm3
        movaps [ecx+320],xmm4
        movaps [ecx+336],xmm5
        movaps [ecx+352],xmm6
        movaps [ecx+368],xmm7

        /*Chunk4*/
        movaps xmm0,[eax+384]
        movaps xmm1,[eax+400]
        movaps xmm2,[eax+416]
        movaps xmm3,[eax+432]
        movaps xmm4,[eax+448]
        movaps xmm5,[eax+464]
        movaps xmm6,[eax+480]
        movaps xmm7,[eax+496]
        maxps  xmm0,[ebx+384]
        maxps  xmm1,[ebx+400]
        maxps  xmm2,[ebx+416]
        maxps  xmm3,[ebx+432]
        maxps  xmm4,[ebx+448]
        maxps  xmm5,[ebx+464]
        maxps  xmm6,[ebx+480]
        maxps  xmm7,[ebx+496]
        movaps [ecx+384],xmm0
        movaps [ecx+400],xmm1
        movaps [ecx+416],xmm2
        movaps [ecx+432],xmm3
        movaps [ecx+448],xmm4
        movaps [ecx+464],xmm5
        movaps [ecx+480],xmm6
        movaps [ecx+496],xmm7
    }
#else
    int i;
    float a,b;
    for(i=0;i<128;i++)
    {
        a=v1[i];
        b=v2[i];
        if(a>=b) m[i]=a;
        else m[i]=b;
    }
#endif /*DB_USE_SIMD*/
}

inline void db_MaxVector_128_SecondSourceDestAligned16_f(float *m,float *v1,float *v2)
{
#ifdef DB_USE_SIMD
    _asm
    {
        mov eax,v1
        mov ebx,v2
        mov ecx,m

        /*Chunk1*/
        movups xmm0,[eax]
        movups xmm1,[eax+16]
        movups xmm2,[eax+32]
        movups xmm3,[eax+48]
        movups xmm4,[eax+64]
        movups xmm5,[eax+80]
        movups xmm6,[eax+96]
        movups xmm7,[eax+112]
        maxps  xmm0,[ebx]
        maxps  xmm1,[ebx+16]
        maxps  xmm2,[ebx+32]
        maxps  xmm3,[ebx+48]
        maxps  xmm4,[ebx+64]
        maxps  xmm5,[ebx+80]
        maxps  xmm6,[ebx+96]
        maxps  xmm7,[ebx+112]
        movaps [ecx],xmm0
        movaps [ecx+16],xmm1
        movaps [ecx+32],xmm2
        movaps [ecx+48],xmm3
        movaps [ecx+64],xmm4
        movaps [ecx+80],xmm5
        movaps [ecx+96],xmm6
        movaps [ecx+112],xmm7

        /*Chunk2*/
        movups xmm0,[eax+128]
        movups xmm1,[eax+144]
        movups xmm2,[eax+160]
        movups xmm3,[eax+176]
        movups xmm4,[eax+192]
        movups xmm5,[eax+208]
        movups xmm6,[eax+224]
        movups xmm7,[eax+240]
        maxps  xmm0,[ebx+128]
        maxps  xmm1,[ebx+144]
        maxps  xmm2,[ebx+160]
        maxps  xmm3,[ebx+176]
        maxps  xmm4,[ebx+192]
        maxps  xmm5,[ebx+208]
        maxps  xmm6,[ebx+224]
        maxps  xmm7,[ebx+240]
        movaps [ecx+128],xmm0
        movaps [ecx+144],xmm1
        movaps [ecx+160],xmm2
        movaps [ecx+176],xmm3
        movaps [ecx+192],xmm4
        movaps [ecx+208],xmm5
        movaps [ecx+224],xmm6
        movaps [ecx+240],xmm7

        /*Chunk3*/
        movups xmm0,[eax+256]
        movups xmm1,[eax+272]
        movups xmm2,[eax+288]
        movups xmm3,[eax+304]
        movups xmm4,[eax+320]
        movups xmm5,[eax+336]
        movups xmm6,[eax+352]
        movups xmm7,[eax+368]
        maxps  xmm0,[ebx+256]
        maxps  xmm1,[ebx+272]
        maxps  xmm2,[ebx+288]
        maxps  xmm3,[ebx+304]
        maxps  xmm4,[ebx+320]
        maxps  xmm5,[ebx+336]
        maxps  xmm6,[ebx+352]
        maxps  xmm7,[ebx+368]
        movaps [ecx+256],xmm0
        movaps [ecx+272],xmm1
        movaps [ecx+288],xmm2
        movaps [ecx+304],xmm3
        movaps [ecx+320],xmm4
        movaps [ecx+336],xmm5
        movaps [ecx+352],xmm6
        movaps [ecx+368],xmm7

        /*Chunk4*/
        movups xmm0,[eax+384]
        movups xmm1,[eax+400]
        movups xmm2,[eax+416]
        movups xmm3,[eax+432]
        movups xmm4,[eax+448]
        movups xmm5,[eax+464]
        movups xmm6,[eax+480]
        movups xmm7,[eax+496]
        maxps  xmm0,[ebx+384]
        maxps  xmm1,[ebx+400]
        maxps  xmm2,[ebx+416]
        maxps  xmm3,[ebx+432]
        maxps  xmm4,[ebx+448]
        maxps  xmm5,[ebx+464]
        maxps  xmm6,[ebx+480]
        maxps  xmm7,[ebx+496]
        movaps [ecx+384],xmm0
        movaps [ecx+400],xmm1
        movaps [ecx+416],xmm2
        movaps [ecx+432],xmm3
        movaps [ecx+448],xmm4
        movaps [ecx+464],xmm5
        movaps [ecx+480],xmm6
        movaps [ecx+496],xmm7
    }
#else
    int i;
    float a,b;
    for(i=0;i<128;i++)
    {
        a=v1[i];
        b=v2[i];
        if(a>=b) m[i]=a;
        else m[i]=b;
    }
#endif /*DB_USE_SIMD*/
}

/*Compute Max-suppression-filtered image for a chunk of sf starting at (left,top), of width 124 and
stopping at bottom. The output is shifted two steps left and overwrites 128 elements for each row.
The input s should be of width at least 128, and exist for 2 pixels outside the specified region.
s[i][left-2] and sf[i][left-2] should be 16 byte aligned. Top must be at least 3*/
inline void db_MaxSuppressFilterChunk_5x5_Aligned16_f(float **sf,float **s,int left,int top,int bottom,
                                      /*temp should point to at least
                                      6*132 floats of 16-byte-aligned allocated memory*/
                                      float *temp)
{
#ifdef DB_USE_SIMD
    int i,lm2;
    float *two[4];
    float *four,*five;

    lm2=left-2;

    /*Set pointers to pre-allocated memory*/
    four=temp;
    five=four+132;
    for(i=0;i<4;i++)
    {
        two[i]=five+(i+1)*132;
    }

    /*Set rests of four and five to zero to avoid
    floating point exceptions*/
    for(i=129;i<132;i++)
    {
        four[i]=0.0;
        five[i]=0.0;
    }

    /*Fill three rows of the wrap-around max buffers*/
    for(i=top-3;i<top;i++) db_MaxVector_128_Aligned16_f(two[i&3],s[i+1]+lm2,s[i+2]+lm2);

    /*For each output row*/
    for(;i<=bottom;i++)
    {
        /*Compute max of the lowest pair of rows in the five row window*/
        db_MaxVector_128_Aligned16_f(two[i&3],s[i+1]+lm2,s[i+2]+lm2);
        /*Compute max of the lowest and highest pair of rows in the five row window*/
        db_MaxVector_128_Aligned16_f(four,two[i&3],two[(i-3)&3]);
        /*Compute max of all rows*/
        db_MaxVector_128_Aligned16_f(five,four,two[(i-1)&3]);
        /*Compute max of 2x5 chunks*/
        db_MaxVector_128_SecondSourceDestAligned16_f(five,five+1,five);
        /*Compute max of pairs of 2x5 chunks*/
        db_MaxVector_128_SecondSourceDestAligned16_f(five,five+3,five);
        /*Compute max of pairs of 5x5 except middle*/
        db_MaxVector_128_SecondSourceDestAligned16_f(sf[i]+lm2,four+2,five);
    }

#else
    int i,j,right;
    float sv;

    right=left+128;
    for(i=top;i<=bottom;i++) for(j=left;j<right;j++)
    {
        sv=s[i][j];

        if( sv>s[i-2][j-2] && sv>s[i-2][j-1] && sv>s[i-2][j] && sv>s[i-2][j+1] && sv>s[i-2][j+2] &&
            sv>s[i-1][j-2] && sv>s[i-1][j-1] && sv>s[i-1][j] && sv>s[i-1][j+1] && sv>s[i-1][j+2] &&
            sv>s[  i][j-2] && sv>s[  i][j-1] &&                 sv>s[  i][j+1] && sv>s[  i][j+2] &&
            sv>s[i+1][j-2] && sv>s[i+1][j-1] && sv>s[i+1][j] && sv>s[i+1][j+1] && sv>s[i+1][j+2] &&
            sv>s[i+2][j-2] && sv>s[i+2][j-1] && sv>s[i+2][j] && sv>s[i+2][j+1] && sv>s[i+2][j+2])
        {
            sf[i][j-2]=0.0;
        }
        else sf[i][j-2]=sv;
    }
#endif /*DB_USE_SIMD*/
}

/*Compute Max-suppression-filtered image for a chunk of sf starting at (left,top) and
stopping at bottom. The output is shifted two steps left. The input s should exist for 2 pixels
outside the specified region. s[i][left-2] and sf[i][left-2] should be 16 byte aligned.
Top must be at least 3. Reading and writing from and to the input and output images is done
as if the region had a width equal to a multiple of 124. If this is not the case, the images
should be over-allocated and the input cleared for a sufficient region*/
void db_MaxSuppressFilter_5x5_Aligned16_f(float **sf,float **s,int left,int top,int right,int bottom,
                                          /*temp should point to at least
                                          6*132 floats of 16-byte-aligned allocated memory*/
                                          float *temp)
{
    int x,next_x;

    for(x=left;x<=right;x=next_x)
    {
        next_x=x+124;
        db_MaxSuppressFilterChunk_5x5_Aligned16_f(sf,s,x,top,bottom,temp);
    }
}

/*Extract corners from the chunk (left,top) to (right,bottom). Store in x_temp,y_temp and s_temp
which should point to space of at least as many positions as there are pixels in the chunk*/
inline int db_CornersFromChunk(float **strength,int left,int top,int right,int bottom,float threshold,double *x_temp,double *y_temp,double *s_temp)
{
    int i,j,nr;
    float s;

    nr=0;
    for(i=top;i<=bottom;i++) for(j=left;j<=right;j++)
    {
        s=strength[i][j];

        if(s>=threshold &&
            s>strength[i-2][j-2] && s>strength[i-2][j-1] && s>strength[i-2][j] && s>strength[i-2][j+1] && s>strength[i-2][j+2] &&
            s>strength[i-1][j-2] && s>strength[i-1][j-1] && s>strength[i-1][j] && s>strength[i-1][j+1] && s>strength[i-1][j+2] &&
            s>strength[  i][j-2] && s>strength[  i][j-1] &&                       s>strength[  i][j+1] && s>strength[  i][j+2] &&
            s>strength[i+1][j-2] && s>strength[i+1][j-1] && s>strength[i+1][j] && s>strength[i+1][j+1] && s>strength[i+1][j+2] &&
            s>strength[i+2][j-2] && s>strength[i+2][j-1] && s>strength[i+2][j] && s>strength[i+2][j+1] && s>strength[i+2][j+2])
        {
            x_temp[nr]=(double) j;
            y_temp[nr]=(double) i;
            s_temp[nr]=(double) s;
            nr++;
        }
    }
    return(nr);
}


//Sub-pixel accuracy using 2D quadratic interpolation.(YCJ)
inline void db_SubPixel(float **strength, const double xd, const double yd, double &xs, double &ys)
{
    int x = (int) xd;
    int y = (int) yd;

    float fxx = strength[y][x-1] - strength[y][x] - strength[y][x] + strength[y][x+1];
    float fyy = strength[y-1][x] - strength[y][x] - strength[y][x] + strength[y+1][x];
    float fxy = (strength[y-1][x-1] - strength[y-1][x+1] - strength[y+1][x-1] + strength[y+1][x+1])/(float)4.0;

    float denom = (fxx * fyy - fxy * fxy) * (float) 2.0;

    xs = xd;
    ys = yd;

    if ( db_absf(denom) <= FLT_EPSILON )
    {
        return;
    }
    else
    {
        float fx = strength[y][x+1] - strength[y][x-1];
        float fy = strength[y+1][x] - strength[y-1][x];

        float dx = (fyy * fx - fxy * fy) / denom;
        float dy = (fxx * fy - fxy * fx) / denom;

        if ( db_absf(dx) > 1.0 || db_absf(dy) > 1.0 )
        {
            return;
        }
        else
        {
            xs -= dx;
            ys -= dy;
        }
    }

    return;
}

/*Extract corners from the image part from (left,top) to (right,bottom).
Store in x and y, extracting at most satnr corners in each block of size (bw,bh).
The pointer temp_d should point to at least 5*bw*bh positions.
area_factor holds how many corners max to extract per 10000 pixels*/
void db_ExtractCornersSaturated(float **strength,int left,int top,int right,int bottom,
                                int bw,int bh,unsigned long area_factor,
                                float threshold,double *temp_d,
                                double *x_coord,double *y_coord,int *nr_corners)
{
    double *x_temp,*y_temp,*s_temp,*select_temp;
    double loc_thresh;
    unsigned long bwbh,area,saturation;
    int x,next_x,last_x;
    int y,next_y,last_y;
    int nr,nr_points,i,stop;

    bwbh=bw*bh;
    x_temp=temp_d;
    y_temp=x_temp+bwbh;
    s_temp=y_temp+bwbh;
    select_temp=s_temp+bwbh;

#ifdef DB_SUB_PIXEL
    // subpixel processing may sometimes push the corner ourside the real border
    // increasing border size:
    left++;
    top++;
    bottom--;
    right--;
#endif /*DB_SUB_PIXEL*/

    nr_points=0;
    for(y=top;y<=bottom;y=next_y)
    {
        next_y=y+bh;
        last_y=next_y-1;
        if(last_y>bottom) last_y=bottom;
        for(x=left;x<=right;x=next_x)
        {
            next_x=x+bw;
            last_x=next_x-1;
            if(last_x>right) last_x=right;

            area=(last_x-x+1)*(last_y-y+1);
            saturation=(area*area_factor)/10000;
            nr=db_CornersFromChunk(strength,x,y,last_x,last_y,threshold,x_temp,y_temp,s_temp);
            if(nr)
            {
                if(((unsigned long)nr)>saturation) loc_thresh=db_LeanQuickSelect(s_temp,nr,nr-saturation,select_temp);
                else loc_thresh=threshold;

                stop=nr_points+saturation;
                for(i=0;(i<nr)&&(nr_points<stop);i++)
                {
                    if(s_temp[i]>=loc_thresh)
                    {
                        #ifdef DB_SUB_PIXEL
                               db_SubPixel(strength, x_temp[i], y_temp[i], x_coord[nr_points], y_coord[nr_points]);
                        #else
                               x_coord[nr_points]=x_temp[i];
                               y_coord[nr_points]=y_temp[i];
                        #endif

                        nr_points++;
                    }
                }
            }
        }
    }
    *nr_corners=nr_points;
}

db_CornerDetector_f::db_CornerDetector_f()
{
    m_w=0; m_h=0;
}

db_CornerDetector_f::~db_CornerDetector_f()
{
    Clean();
}

void db_CornerDetector_f::Clean()
{
    if(m_w!=0)
    {
        delete [] m_temp_f;
        delete [] m_temp_d;
        db_FreeStrengthImage_f(m_strength_mem,m_strength,m_h);
    }
    m_w=0; m_h=0;
}

unsigned long db_CornerDetector_f::Init(int im_width,int im_height,int target_nr_corners,
                            int nr_horizontal_blocks,int nr_vertical_blocks,
                            double absolute_threshold,double relative_threshold)
{
    int chunkwidth=208;
    int block_width,block_height;
    unsigned long area_factor;
    int active_width,active_height;

    active_width=db_maxi(1,im_width-10);
    active_height=db_maxi(1,im_height-10);
    block_width=db_maxi(1,active_width/nr_horizontal_blocks);
    block_height=db_maxi(1,active_height/nr_vertical_blocks);

    area_factor=db_minl(1000,db_maxl(1,(long)(10000.0*((double)target_nr_corners)/
        (((double)active_width)*((double)active_height)))));

    return(Start(im_width,im_height,block_width,block_height,area_factor,
        absolute_threshold,relative_threshold,chunkwidth));
}

unsigned long db_CornerDetector_f::Start(int im_width,int im_height,
                             int block_width,int block_height,unsigned long area_factor,
                             double absolute_threshold,double relative_threshold,int chunkwidth)
{
    Clean();

    m_w=im_width;
    m_h=im_height;
    m_cw=chunkwidth;
    m_bw=block_width;
    m_bh=block_height;
    m_area_factor=area_factor;
    m_r_thresh=relative_threshold;
    m_a_thresh=absolute_threshold;
    m_max_nr=db_maxl(1,1+(m_w*m_h*m_area_factor)/10000);

    m_temp_f=new float[13*(m_cw+4)];
    m_temp_d=new double[5*m_bw*m_bh];
    m_strength=db_AllocStrengthImage_f(&m_strength_mem,m_w,m_h);

    return(m_max_nr);
}

void db_CornerDetector_f::DetectCorners(const float * const *img,double *x_coord,double *y_coord,int *nr_corners) const
{
    float max_val,threshold;

    db_HarrisStrength_f(m_strength,img,m_w,m_h,m_temp_f,m_cw);

    if(m_r_thresh)
    {
        max_val=db_MaxImage_Aligned16_f(m_strength,3,3,m_w-6,m_h-6);
        threshold= (float) db_maxd(m_a_thresh,max_val*m_r_thresh);
    }
    else threshold= (float) m_a_thresh;

    db_ExtractCornersSaturated(m_strength,BORDER,BORDER,m_w-BORDER-1,m_h-BORDER-1,m_bw,m_bh,m_area_factor,threshold,
        m_temp_d,x_coord,y_coord,nr_corners);
}

db_CornerDetector_u::db_CornerDetector_u()
{
    m_w=0; m_h=0;
}

db_CornerDetector_u::~db_CornerDetector_u()
{
    Clean();
}

db_CornerDetector_u::db_CornerDetector_u(const db_CornerDetector_u& cd)
{
    Start(cd.m_w, cd.m_h, cd.m_bw, cd.m_bh, cd.m_area_factor,
        cd.m_a_thresh, cd.m_r_thresh);
}

db_CornerDetector_u& db_CornerDetector_u::operator=(const db_CornerDetector_u& cd)
{
    if ( this == &cd ) return *this;

    Clean();

    Start(cd.m_w, cd.m_h, cd.m_bw, cd.m_bh, cd.m_area_factor,
        cd.m_a_thresh, cd.m_r_thresh);

    return *this;
}

void db_CornerDetector_u::Clean()
{
    if(m_w!=0)
    {
        delete [] m_temp_i;
        delete [] m_temp_d;
        db_FreeStrengthImage_f(m_strength_mem,m_strength,m_h);
    }
    m_w=0; m_h=0;
}

unsigned long db_CornerDetector_u::Init(int im_width,int im_height,int target_nr_corners,
                            int nr_horizontal_blocks,int nr_vertical_blocks,
                            double absolute_threshold,double relative_threshold)
{
    int block_width,block_height;
    unsigned long area_factor;
    int active_width,active_height;

    active_width=db_maxi(1,im_width-10);
    active_height=db_maxi(1,im_height-10);
    block_width=db_maxi(1,active_width/nr_horizontal_blocks);
    block_height=db_maxi(1,active_height/nr_vertical_blocks);

    area_factor=db_minl(1000,db_maxl(1,(long)(10000.0*((double)target_nr_corners)/
        (((double)active_width)*((double)active_height)))));

    return(Start(im_width,im_height,block_width,block_height,area_factor,
        16.0*absolute_threshold,relative_threshold));
}

unsigned long db_CornerDetector_u::Start(int im_width,int im_height,
                             int block_width,int block_height,unsigned long area_factor,
                             double absolute_threshold,double relative_threshold)
{
    Clean();

    m_w=im_width;
    m_h=im_height;
    m_bw=block_width;
    m_bh=block_height;
    m_area_factor=area_factor;
    m_r_thresh=relative_threshold;
    m_a_thresh=absolute_threshold;
    m_max_nr=db_maxl(1,1+(m_w*m_h*m_area_factor)/10000);

    m_temp_i=new int[18*128];
    m_temp_d=new double[5*m_bw*m_bh];
    m_strength=db_AllocStrengthImage_f(&m_strength_mem,m_w,m_h);

    return(m_max_nr);
}

void db_CornerDetector_u::DetectCorners(const unsigned char * const *img,double *x_coord,double *y_coord,int *nr_corners,
                                        const unsigned char * const *msk, unsigned char fgnd) const
{
    float max_val,threshold;

    db_HarrisStrength_u(m_strength,img,m_w,m_h,m_temp_i);


    if(m_r_thresh)
    {
        max_val=db_MaxImage_Aligned16_f(m_strength,3,3,m_w-6,m_h-6);
        threshold= (float) db_maxd(m_a_thresh,max_val*m_r_thresh);
    }
    else threshold= (float) m_a_thresh;

    db_ExtractCornersSaturated(m_strength,BORDER,BORDER,m_w-BORDER-1,m_h-BORDER-1,m_bw,m_bh,m_area_factor,threshold,
        m_temp_d,x_coord,y_coord,nr_corners);


    if ( msk )
    {
        int nr_corners_mask=0;

        for ( int i = 0; i < *nr_corners; ++i)
        {
            int cor_x = db_roundi(*(x_coord+i));
            int cor_y = db_roundi(*(y_coord+i));
            if ( msk[cor_y][cor_x] == fgnd )
            {
                x_coord[nr_corners_mask] = x_coord[i];
                y_coord[nr_corners_mask] = y_coord[i];
                nr_corners_mask++;
            }
        }
        *nr_corners = nr_corners_mask;
    }
}

void db_CornerDetector_u::ExtractCorners(float ** strength, double *x_coord, double *y_coord, int *nr_corners) {
    if ( m_w!=0 )
        db_ExtractCornersSaturated(strength,BORDER,BORDER,m_w-BORDER-1,m_h-BORDER-1,m_bw,m_bh,m_area_factor,float(m_a_thresh),
            m_temp_d,x_coord,y_coord,nr_corners);
}

