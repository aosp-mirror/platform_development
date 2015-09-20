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

// Delaunay.h
// $Id: Delaunay.h,v 1.9 2011/06/17 13:35:48 mbansal Exp $

#ifndef DELAUNAY_H
#define DELAUNAY_H
#include <stdio.h>
#include <math.h>
#include "CSite.h"
#include "EdgePointerUtil.h"

#ifndef TRUE
#define TRUE 1==1
#define FALSE 0==1
#endif

//******************************************************************************
// Reference for Quad-edge data structure:
//
// Leonidas Guibas and Jorge Stolfi, "Primitives for the manipulation of general
//     subdivisions and the computations of Voronoi diagrams",
//     ACM Transactions on Graphics 4, 74-123 (1985).
//
//******************************************************************************

//
// Common data structures
//

typedef short SitePointer;
typedef short TrianglePointer;

class CDelaunay
{
private:
  CSite *sa;
  EdgePointer oneBndryEdge;
  EdgePointer *next;
  SitePointer *org;
  struct EDGE_INFO *ei;
  SitePointer *sp;
  SEdgeVector *ev;

  SitePointer sp1;
  EdgePointer nextEdge;
  EdgePointer availEdge;

private:
  void build(int lo, int hi, EdgePointer *le, EdgePointer *re, int rows);
  void buildTriangulation(int size);

  EdgePointer allocEdge();
  void freeEdge(EdgePointer e);

  EdgePointer makeEdge(SitePointer origin, SitePointer destination);
  void deleteEdge(EdgePointer e);

  void splice(EdgePointer, EdgePointer);
  EdgePointer consolidateEdges();
  void deleteAllEdges();

  void spsortx(SitePointer *, int, int);
  void spsorty(SitePointer *, int, int);

  int cmpev(int i, int j);
  int xcmpsp(int i, int j);
  int ycmpsp(int i, int j);

  void swapsp(int i, int j);
  void swapev(int i, int j);

  void copysp(int i, int j);
  void copyev(int i, int j);

  void rcssort(int lowelt, int highelt, int temp,
                 int (CDelaunay::*comparison)(int,int),
                 void (CDelaunay::*swap)(int,int),
                 void (CDelaunay::*copy)(int,int));

  void doMerge(EdgePointer *ldo, EdgePointer ldi, EdgePointer rdi, EdgePointer *rdo);
  EdgePointer connectLeft(EdgePointer a, EdgePointer b);
  EdgePointer connectRight(EdgePointer a, EdgePointer b);
  int ccw(SitePointer a, SitePointer b, SitePointer c);
  int incircle(SitePointer a, SitePointer b, SitePointer c, SitePointer d);
  int constructList(EdgePointer e, int width, int height);

public:
  CDelaunay();
  ~CDelaunay();

  CSite *allocMemory(int nsite);
  void freeMemory();
  int triangulate(SEdgeVector **edge, int nsite, int width, int height);
  void linkNeighbors(SEdgeVector *edge, int nedge, int nsite);
};

#define onext(a) next[a]
#define oprev(a) rot(onext(rot(a)))
#define lnext(a) rot(onext(rotinv(a)))
#define lprev(a) sym(onext(a))
#define rnext(a) rotinv(onext(rot(a)))
#define rprev(a) onext(sym(a))
#define dnext(a) sym(onext(sym(a)))
#define dprev(a) rotinv(onext(rotinv(a)))

#define orig(a) org[a]
#define dest(a) orig(sym(a))
#define left(a) orig(rotinv(a))
#define right(a) orig(rot(a))

#endif
