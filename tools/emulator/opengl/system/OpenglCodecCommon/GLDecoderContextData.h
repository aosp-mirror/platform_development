#ifndef _GL_DECODER_CONTEXT_DATA_H_
#define _GL_DECODER_CONTEXT_DATA_H_

#include <assert.h>
#include <string.h>
#include "FixedBuffer.h"

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
        LAST_LOCATION = 12
    } PointerDataLocation;

    void storePointerData(PointerDataLocation loc, void *data, size_t len) {
        assert(loc < LAST_LOCATION);

        m_pointerData[loc].alloc(len);
        memcpy(m_pointerData[loc].ptr(), data, len);
    }
    void *pointerData(PointerDataLocation loc) {
        assert(loc < LAST_LOCATION);
        return m_pointerData[loc].ptr();
    }
private:
    FixedBuffer m_pointerData[LAST_LOCATION];
};

#endif
