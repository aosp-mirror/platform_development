/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef _GL_DECODER_CONTEXT_DATA_H_
#define _GL_DECODER_CONTEXT_DATA_H_

#include <assert.h>
#include <string.h>
#include "FixedBuffer.h"
#include "codec_defs.h"

class  GLDecoderContextData {
public:
    typedef enum  {
        VERTEX_LOCATION = 0,
        NORMAL_LOCATION = 1,
        COLOR_LOCATION = 2,
        POINTSIZE_LOCATION = 3,
        TEXCOORD0_LOCATION = 4,
        TEXCOORD1_LOCATION = 5,
        TEXCOORD2_LOCATION = 6,
        TEXCOORD3_LOCATION = 7,
        TEXCOORD4_LOCATION = 8,
        TEXCOORD5_LOCATION = 9,
        TEXCOORD6_LOCATION = 10,
        TEXCOORD7_LOCATION = 11,
        MATRIXINDEX_LOCATION = 12,
        WEIGHT_LOCATION = 13,
        LAST_LOCATION = 14
    } PointerDataLocation;

    GLDecoderContextData(int nLocations = CODEC_MAX_VERTEX_ATTRIBUTES) :
        m_nLocations(nLocations)
    {
        m_pointerData = new FixedBuffer[m_nLocations];
    }

    ~GLDecoderContextData() {
        delete [] m_pointerData;
    }

    void storePointerData(unsigned int loc, void *data, size_t len) {

        assert(loc < m_nLocations);
        m_pointerData[loc].alloc(len);
        memcpy(m_pointerData[loc].ptr(), data, len);
    }
    void *pointerData(unsigned int loc) {
        assert(loc < m_nLocations);
        return m_pointerData[loc].ptr();
    }
private:
    FixedBuffer *m_pointerData;
    int m_nLocations;
};

#endif
