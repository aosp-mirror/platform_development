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

///////////////////////////////////////////////////
// CSite.h
// $Id: CSite.h,v 1.3 2011/06/17 13:35:47 mbansal Exp $

#ifndef TRIDEL_H
#define TRIDEL_H

#include "MosaicTypes.h"

typedef struct
{
    short first;
    short second;
} SEdgeVector;

typedef struct
{
    double x;
    double y;
} SVec2d;

class CSite
{
private:
    MosaicFrame *mosaicFrame;
    SEdgeVector *neighbor;
    int numNeighbors;
    SVec2d voronoiCenter;

public:
    CSite();
    ~CSite();

    inline MosaicFrame* getMb() { return mosaicFrame; }
    inline SEdgeVector* getNeighbor() { return neighbor; }
    inline int getNumNeighbors() { return numNeighbors; }
    inline SVec2d& getVCenter() { return voronoiCenter; }
    inline double X() { return voronoiCenter.x; }
    inline double Y() { return voronoiCenter.y; }

    inline void incrNumNeighbors() { numNeighbors++; }
    inline void setNumNeighbors(int num) { numNeighbors = num; }
    inline void setNeighbor(SEdgeVector *nb) { neighbor = nb; }
    inline void setMb(MosaicFrame *mb) { mosaicFrame = mb; }
};

#endif
