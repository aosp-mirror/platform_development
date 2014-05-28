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

/* $Id: db_utilities_indexing.cpp,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#include "db_utilities_indexing.h"
#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

void db_Zero(double *d,long nr)
{
    long i;
    for(i=0;i<nr;i++) d[i]=0.0;
}

/*This routine breaks number in source into values smaller and larger than
a pivot element. Values equal to the pivot are ignored*/
void db_LeanPartitionOnPivot(double pivot,double *dest,const double *source,long first,long last,long *first_equal,long *last_equal)
{
    double temp;
    const double *s_point;
    const double *s_top;
    double *d_bottom;
    double *d_top;

    s_point=source+first;
    s_top=source+last;
    d_bottom=dest+first;
    d_top=dest+last;

    for(;s_point<=s_top;)
    {
        temp= *(s_point++);
        if(temp<pivot) *(d_bottom++)=temp;
        else if(temp>pivot) *(d_top--)=temp;
    }
    *first_equal=d_bottom-dest;
    *last_equal=d_top-dest;
}

double db_LeanQuickSelect(const double *s,long nr_elements,long pos,double *temp)
{
  long first=0;
  long last=nr_elements-1;
  double pivot;
  long first_equal,last_equal;
  double *tempA;
  double *tempB;
  double *tempC;
  const double *source;
  double *dest;

  tempA=temp;
  tempB=temp+nr_elements;
  source=s;
  dest=tempA;

  for(;last-first>2;)
  {
      pivot=db_TripleMedian(source[first],source[last],source[(first+last)/2]);
      db_LeanPartitionOnPivot(pivot,dest,source,first,last,&first_equal,&last_equal);

      if(first_equal>pos) last=first_equal-1;
      else if(last_equal<pos) first=last_equal+1;
      else
      {
        return(pivot);
      }

      /*Swap pointers*/
      tempC=tempA;
      tempA=tempB;
      tempB=tempC;
      source=tempB;
      dest=tempA;
  }
  pivot=db_TripleMedian(source[first],source[last],source[(first+last)/2]);

  return(pivot);
}

float* db_AlignPointer_f(float *p,unsigned long nr_bytes)
{
    float *ap;
    unsigned long m;

    m=((unsigned long)p)%nr_bytes;
    if(m) ap=(float*) (((unsigned long)p)-m+nr_bytes);
    else ap=p;
    return(ap);
}

short* db_AlignPointer_s(short *p,unsigned long nr_bytes)
{
    short *ap;
    unsigned long m;

    m=((unsigned long)p)%nr_bytes;
    if(m) ap=(short*) (((unsigned long)p)-m+nr_bytes);
    else ap=p;
    return(ap);
}
