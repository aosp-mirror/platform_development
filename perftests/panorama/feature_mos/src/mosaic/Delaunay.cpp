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

// Delaunay.cpp
// $Id: Delaunay.cpp,v 1.10 2011/06/17 13:35:48 mbansal Exp $

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include "Delaunay.h"

#define QQ 9   // Optimal value as determined by testing
#define DM 38  // 2^(1+DM/2) element sort capability. DM=38 for >10^6 elements
#define NYL -1
#define valid(l) ccw(orig(basel), dest(l), dest(basel))


CDelaunay::CDelaunay()
{
}

CDelaunay::~CDelaunay()
{
}

// Allocate storage, construct triangulation, compute voronoi corners
int CDelaunay::triangulate(SEdgeVector **edges, int n_sites, int width, int height)
{
  EdgePointer cep;

  deleteAllEdges();
  buildTriangulation(n_sites);
  cep = consolidateEdges();
  *edges = ev;

  // Note: construction_list will change ev
  return constructList(cep, width, height);
}

// builds delaunay triangulation
void CDelaunay::buildTriangulation(int size)
{
  int i, rows;
  EdgePointer lefte, righte;

  rows = (int)( 0.5 + sqrt( (double) size / log( (double) size )));

  // Sort the pointers by  x-coordinate of site
  for ( i=0 ; i < size ; i++ ) {
    sp[i] = (SitePointer) i;
  }

  spsortx( sp, 0, size-1 );
  build( 0, size-1, &lefte, &righte, rows );
  oneBndryEdge = lefte;
}

// Recursive Delaunay Triangulation Procedure
// Contains modifications for axis-switching division.
void CDelaunay::build(int lo, int hi, EdgePointer *le, EdgePointer *re, int rows)
{
  EdgePointer a, b, c, ldo, rdi, ldi, rdo, maxx, minx;
  int split, lowrows;
  int low, high;
  SitePointer s1, s2, s3;
  low = lo;
  high = hi;

  if ( low < (high-2) ) {
    // more than three elements; do recursion
    minx = sp[low];
    maxx = sp[high];
    if (rows == 1) {    // time to switch axis of division
      spsorty( sp, low, high);
      rows = 65536;
    }
    lowrows = rows/2;
    split = low - 1 + (int)
      (0.5 + ((double)(high-low+1) * ((double)lowrows / (double)rows)));
    build( low, split, &ldo, &ldi, lowrows );
    build( split+1, high, &rdi, &rdo, (rows-lowrows) );
    doMerge(&ldo, ldi, rdi, &rdo);
    while (orig(ldo) != minx) {
      ldo = rprev(ldo);
    }
    while (orig(rdo) != maxx) {
      rdo = (SitePointer) lprev(rdo);
    }
    *le = ldo;
    *re = rdo;
  }
  else if (low >= (high - 1)) { // two or one points
    a = makeEdge(sp[low], sp[high]);
    *le = a;
    *re = (EdgePointer) sym(a);
  } else { // three points
    // 3 cases: triangles of 2 orientations, and 3 points on a line
    a = makeEdge((s1 = sp[low]), (s2 = sp[low+1]));
    b = makeEdge(s2, (s3 = sp[high]));
    splice((EdgePointer) sym(a), b);
    if (ccw(s1, s3, s2)) {
      c = connectLeft(b, a);
      *le = (EdgePointer) sym(c);
      *re = c;
    } else {
      *le = a;
      *re = (EdgePointer) sym(b);
      if (ccw(s1, s2, s3)) {
        // not colinear
        c = connectLeft(b, a);
      }
    }
  }
}

// Quad-edge manipulation primitives
EdgePointer CDelaunay::makeEdge(SitePointer origin, SitePointer destination)
{
  EdgePointer temp, ans;
  temp = allocEdge();
  ans = temp;

  onext(temp) = ans;
  orig(temp) = origin;
  onext(++temp) = (EdgePointer) (ans + 3);
  onext(++temp) = (EdgePointer) (ans + 2);
  orig(temp) = destination;
  onext(++temp) = (EdgePointer) (ans + 1);

  return(ans);
}

void CDelaunay::splice(EdgePointer a, EdgePointer b)
{
  EdgePointer alpha, beta, temp;
  alpha = (EdgePointer) rot(onext(a));
  beta = (EdgePointer) rot(onext(b));
  temp = onext(alpha);
  onext(alpha) = onext(beta);
  onext(beta) = temp;
  temp = onext(a);
  onext(a) = onext(b);
  onext(b) = temp;
}

EdgePointer CDelaunay::connectLeft(EdgePointer a, EdgePointer b)
{
  EdgePointer ans;
  ans = makeEdge(dest(a), orig(b));
  splice(ans, (EdgePointer) lnext(a));
  splice((EdgePointer) sym(ans), b);
  return(ans);
}

EdgePointer CDelaunay::connectRight(EdgePointer a, EdgePointer b)
{
  EdgePointer ans;
  ans = makeEdge(dest(a), orig(b));
  splice(ans, (EdgePointer) sym(a));
  splice((EdgePointer) sym(ans), (EdgePointer) oprev(b));
  return(ans);
}

// disconnects e from the rest of the structure and destroys it
void CDelaunay::deleteEdge(EdgePointer e)
{
  splice(e, (EdgePointer) oprev(e));
  splice((EdgePointer) sym(e), (EdgePointer) oprev(sym(e)));
  freeEdge(e);
}

//
// Overall storage allocation
//

// Quad-edge storage allocation
CSite *CDelaunay::allocMemory(int n)
{
  unsigned int size;

  size = ((sizeof(CSite) + sizeof(SitePointer)) * n +
          (sizeof(SitePointer) + sizeof(EdgePointer)) * 12
          ) * n;
  if (!(sa = (CSite*) malloc(size))) {
    return NULL;
  }
  sp = (SitePointer *) (sa + n);
  ev = (SEdgeVector *) (org = sp + n);
  next = (EdgePointer *) (org + 12 * n);
  ei = (struct EDGE_INFO *) (next + 12 * n);
  return sa;
}

void CDelaunay::freeMemory()
{
  if (sa) {
    free(sa);
    sa = (CSite*)NULL;
  }
}

//
// Edge storage management
//

void CDelaunay::deleteAllEdges()
{
    nextEdge = 0;
    availEdge = NYL;
}

EdgePointer CDelaunay::allocEdge()
{
  EdgePointer ans;

  if (availEdge == NYL) {
    ans = nextEdge, nextEdge += 4;
  } else {
    ans = availEdge, availEdge = onext(availEdge);
  }
  return(ans);
}

void CDelaunay::freeEdge(EdgePointer e)
{
  e ^= e & 3;
  onext(e) = availEdge;
  availEdge = e;
}

EdgePointer CDelaunay::consolidateEdges()
{
  EdgePointer e;
  int i,j;

  while (availEdge != NYL) {
    nextEdge -= 4; e = availEdge; availEdge = onext(availEdge);

    if (e==nextEdge) {
      continue; // the one deleted was the last one anyway
    }
    if ((oneBndryEdge&~3) == nextEdge) {
      oneBndryEdge = (EdgePointer) (e | (oneBndryEdge&3));
    }
    for (i=0,j=3; i<4; i++,j=rot(j)) {
      onext(e+i) = onext(nextEdge+i);
      onext(rot(onext(e+i))) = (EdgePointer) (e+j);
    }
  }
  return nextEdge;
}

//
// Sorting Routines
//

int CDelaunay::xcmpsp(int i, int j)
{
  double d = sa[(i>=0)?sp[i]:sp1].X() - sa[(j>=0)?sp[j]:sp1].X();
  if ( d > 0. ) {
    return 1;
  }
  if ( d < 0. ) {
    return -1;
  }
  d = sa[(i>=0)?sp[i]:sp1].Y() - sa[(j>=0)?sp[j]:sp1].Y();
  if ( d > 0. ) {
    return 1;
  }
  if ( d < 0. ) {
    return -1;
  }
  return 0;
}

int CDelaunay::ycmpsp(int i, int j)
{
  double d = sa[(i>=0)?sp[i]:sp1].Y() - sa[(j>=0)?sp[j]:sp1].Y();
  if ( d > 0. ) {
    return 1;
  }
  if ( d < 0. ) {
    return -1;
  }
  d = sa[(i>=0)?sp[i]:sp1].X() - sa[(j>=0)?sp[j]:sp1].X();
  if ( d > 0. ) {
    return 1;
  }
  if ( d < 0. ) {
    return -1;
  }
  return 0;
}

int CDelaunay::cmpev(int i, int j)
{
  return (ev[i].first - ev[j].first);
}

void CDelaunay::swapsp(int i, int j)
{
  int t;
  t = (i>=0) ? sp[i] : sp1;

  if (i>=0) {
    sp[i] = (j>=0)?sp[j]:sp1;
  } else {
    sp1 = (j>=0)?sp[j]:sp1;
  }

  if (j>=0) {
    sp[j] = (SitePointer) t;
  } else {
    sp1 = (SitePointer) t;
  }
}

void CDelaunay::swapev(int i, int j)
{
  SEdgeVector temp;

  temp = ev[i];
  ev[i] = ev[j];
  ev[j] = temp;
}

void CDelaunay::copysp(int i, int j)
{
  if (j>=0) {
    sp[j] = (i>=0)?sp[i]:sp1;
  } else {
    sp1 = (i>=0)?sp[i]:sp1;
  }
}

void CDelaunay::copyev(int i, int j)
{
  ev[j] = ev[i];
}

void CDelaunay::spsortx(SitePointer *sp_in, int low, int high)
{
  sp = sp_in;
  rcssort(low,high,-1,&CDelaunay::xcmpsp,&CDelaunay::swapsp,&CDelaunay::copysp);
}

void CDelaunay::spsorty(SitePointer *sp_in, int low, int high )
{
  sp = sp_in;
  rcssort(low,high,-1,&CDelaunay::ycmpsp,&CDelaunay::swapsp,&CDelaunay::copysp);
}

void CDelaunay::rcssort(int lowelt, int highelt, int temp,
                    int (CDelaunay::*comparison)(int,int),
                    void (CDelaunay::*swap)(int,int),
                    void (CDelaunay::*copy)(int,int))
{
  int m,sij,si,sj,sL,sk;
  int stack[DM];

  if (highelt-lowelt<=1) {
    return;
  }
  if (highelt-lowelt>QQ) {
    m = 0;
    si = lowelt; sj = highelt;
    for (;;) { // partition [si,sj] about median-of-3.
      sij = (sj+si) >> 1;

      // Now to sort elements si,sij,sj into order & set temp=their median
      if ( (this->*comparison)( si,sij ) > 0 ) {
        (this->*swap)( si,sij );
      }
      if ( (this->*comparison)( sij,sj ) > 0 ) {
        (this->*swap)( sj,sij );
        if ( (this->*comparison)( si,sij ) > 0 ) {
          (this->*swap)( si,sij );
        }
      }
      (this->*copy)( sij,temp );

      // Now to partition into elements <=temp, >=temp, and ==temp.
      sk = si; sL = sj;
      do {
        do {
          sL--;
        } while( (this->*comparison)( sL,temp ) > 0 );
        do {
          sk++;
        } while( (this->*comparison)( temp,sk ) > 0 );
        if ( sk < sL ) {
          (this->*swap)( sL,sk );
        }
      } while(sk <= sL);

      // Now to recurse on shorter partition, store longer partition on stack
      if ( sL-si > sj-sk ) {
        if ( sL-si < QQ ) {
          if( m==0 ) {
            break;  // empty stack && both partitions < QQ so break
          } else {
            sj = stack[--m];
            si = stack[--m];
          }
        }
        else {
          if ( sj-sk < QQ ) {
            sj = sL;
          } else {
            stack[m++] = si;
            stack[m++] = sL;
            si = sk;
          }
        }
      }
      else {
        if ( sj-sk < QQ ) {
          if ( m==0 ) {
            break; // empty stack && both partitions < QQ so break
          } else {
            sj = stack[--m];
            si = stack[--m];
          }
        }
        else {
          if ( sL-si < QQ ) {
            si = sk;
          } else {
            stack[m++] = sk;
            stack[m++] = sj;
            sj = sL;
          }
        }
      }
    }
  }

  // Now for 0 or Data bounded  "straight insertion" sort of [0,nels-1]; if it is
  // known that el[-1] = -INF, then can omit the "sk>=0" test and save time.
  for (si=lowelt; si<highelt; si++) {
    if ( (this->*comparison)( si,si+1 ) > 0 ) {
      (this->*copy)( si+1,temp );
      sj = sk = si;
      sj++;
      do {
        (this->*copy)( sk,sj );
        sj = sk;
        sk--;
      } while ( (this->*comparison)( sk,temp ) > 0 && sk>=lowelt );
      (this->*copy)( temp,sj );
    }
  }
}

//
// Geometric primitives
//

// incircle, as in the Guibas-Stolfi paper.
int CDelaunay::incircle(SitePointer a, SitePointer b, SitePointer c, SitePointer d)
{
  double adx, ady, bdx, bdy, cdx, cdy, dx, dy, nad, nbd, ncd;
  dx = sa[d].X();
  dy = sa[d].Y();
  adx = sa[a].X() - dx;
  ady = sa[a].Y() - dy;
  bdx = sa[b].X() - dx;
  bdy = sa[b].Y() - dy;
  cdx = sa[c].X() - dx;
  cdy = sa[c].Y() - dy;
  nad = adx*adx+ady*ady;
  nbd = bdx*bdx+bdy*bdy;
  ncd = cdx*cdx+cdy*cdy;
  return( (0.0 < (nad * (bdx * cdy - bdy * cdx)
                  + nbd * (cdx * ady - cdy * adx)
                  + ncd * (adx * bdy - ady * bdx))) ? TRUE : FALSE );
}

// TRUE iff A, B, C form a counterclockwise oriented triangle
int CDelaunay::ccw(SitePointer a, SitePointer b, SitePointer c)
{
  int result;

  double ax = sa[a].X();
  double bx = sa[b].X();
  double cx = sa[c].X();
  double ay = sa[a].Y();
  double by = sa[b].Y();
  double cy = sa[c].Y();

  double val = (ax - cx)*(by - cy) - (bx - cx)*(ay - cy);
  if ( val > 0.0) {
    return true;
  }

  return false;
}

//
// The Merge Procedure.
//

void CDelaunay::doMerge(EdgePointer *ldo, EdgePointer ldi, EdgePointer rdi, EdgePointer *rdo)
{
  int rvalid, lvalid;
  EdgePointer basel,lcand,rcand,t;

  for (;;) {
    while (ccw(orig(ldi), dest(ldi), orig(rdi))) {
        ldi = (EdgePointer) lnext(ldi);
    }
    if (ccw(dest(rdi), orig(rdi), orig(ldi))) {
        rdi = (EdgePointer)rprev(rdi);
    } else {
      break;
    }
  }

  basel = connectLeft((EdgePointer) sym(rdi), ldi);
  lcand = rprev(basel);
  rcand = (EdgePointer) oprev(basel);
  if (orig(basel) == orig(*rdo)) {
    *rdo = basel;
  }
  if (dest(basel) == orig(*ldo)) {
    *ldo = (EdgePointer) sym(basel);
  }

  for (;;) {
#if 1
    if (valid(t=onext(lcand))) {
#else
    t = (EdgePointer)onext(lcand);
    if (valid(basel, t)) {
#endif
      while (incircle(dest(lcand), dest(t), orig(lcand), orig(basel))) {
        deleteEdge(lcand);
        lcand = t;
        t = onext(lcand);
      }
    }
#if 1
    if (valid(t=(EdgePointer)oprev(rcand))) {
#else
    t = (EdgePointer)oprev(rcand);
    if (valid(basel, t)) {
#endif
      while (incircle(dest(t), dest(rcand), orig(rcand), dest(basel))) {
        deleteEdge(rcand);
        rcand = t;
        t = (EdgePointer)oprev(rcand);
      }
    }

#if 1
    lvalid = valid(lcand);
    rvalid = valid(rcand);
#else
    lvalid = valid(basel, lcand);
    rvalid = valid(basel, rcand);
#endif
    if ((! lvalid) && (! rvalid)) {
      return;
    }

    if (!lvalid ||
        (rvalid && incircle(dest(lcand), orig(lcand), orig(rcand), dest(rcand)))) {
      basel = connectLeft(rcand, (EdgePointer) sym(basel));
      rcand = (EdgePointer) lnext(sym(basel));
    } else {
      basel = (EdgePointer) sym(connectRight(lcand, basel));
      lcand = rprev(basel);
    }
  }
}

int CDelaunay::constructList(EdgePointer last, int width, int height)
{
  int c, i;
  EdgePointer curr, src, nex;
  SEdgeVector *currv, *prevv;

  c = (int) ((curr = (EdgePointer) ((last & ~3))) >> 1);

  for (last -= 4; last >= 0; last -= 4) {
    src = orig(last);
    nex = dest(last);
    orig(--curr) = src;
    orig(--curr) = nex;
    orig(--curr) = nex;
    orig(--curr) = src;
  }
  rcssort(0, c - 1, -1, &CDelaunay::cmpev, &CDelaunay::swapev, &CDelaunay::copyev);

  // Throw out any edges that are too far apart
  currv = prevv = ev;
  for (i = c; i--; currv++) {
      if ((int) fabs(sa[currv->first].getVCenter().x - sa[currv->second].getVCenter().x) <= width &&
          (int) fabs(sa[currv->first].getVCenter().y - sa[currv->second].getVCenter().y) <= height) {
          *(prevv++) = *currv;
      } else {
        c--;
      }
  }
  return c;
}

// Fill in site neighbor information
void CDelaunay::linkNeighbors(SEdgeVector *edge, int nedge, int nsite)
{
  int i;

  for (i = 0; i < nsite; i++) {
    sa[i].setNeighbor(edge);
    sa[i].setNumNeighbors(0);
    for (; edge->first == i && nedge; edge++, nedge--) {
      sa[i].incrNumNeighbors();
    }
  }
}
