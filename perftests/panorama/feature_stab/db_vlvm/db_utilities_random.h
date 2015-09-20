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

/* $Id: db_utilities_random.h,v 1.1 2010/08/19 18:09:20 bsouthall Exp $ */

#ifndef DB_UTILITIES_RANDOM
#define DB_UTILITIES_RANDOM

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMRandom (LM) Random numbers, random sampling
 */
/*\{*/
/*!
 Random Number generator. Initialize with non-zero
integer value r. A double between zero and one is
returned.
\param r    seed
\return random double
*/
inline double db_QuickRandomDouble(int &r)
{
    int c;
    c=r/127773;
    r=16807*(r-c*127773)-2836*c;
    if(r<0) r+=2147483647;
    return((1.0/((double)2147483647))*r);
    //return (((double)rand())/(double)RAND_MAX);
}

/*!
Random Number generator. Initialize with non-zero
integer value r. An int between and including 0 and max
 \param r    seed
 \param max    upped limit
 \return random int
*/
inline int db_RandomInt(int &r,int max)
{
    double dtemp;
    int itemp;
    dtemp=db_QuickRandomDouble(r)*(max+1);
    itemp=(int) dtemp;
    if(itemp<=0) return(0);
    if(itemp>=max) return(max);
    return(itemp);
}

/*!
 Generate a random sample indexing into [0..pool_size-1].
 \param s            sample (out) pre-allocated array of size sample_size
 \param sample_size    size of sample
 \param pool_size    upper limit on item index
 \param r_seed        random number generator seed
 */
inline void db_RandomSample(int *s,int sample_size,int pool_size,int &r_seed)
{
    int temp,temp2,i,j;

    for(i=0;i<sample_size;i++)
    {
        temp=db_RandomInt(r_seed,pool_size-1-i);

        for(j=0;j<i;j++)
        {
            if(s[j]<=temp) temp++;
            else
            {
                /*swap*/
                temp2=temp;
                temp=s[j];
                s[j]=temp2;
            }
        }
        s[i]=temp;
    }
}
/*\}*/
#endif /* DB_UTILITIES_RANDOM */
