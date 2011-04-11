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
#ifndef _GL_CLIENT_STATE_H_
#define _GL_CLIENT_STATE_H_

#define GL_API
#ifndef ANDROID
#define GL_APIENTRY
#define GL_APIENTRYP
#endif

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <stdio.h>
#include <stdlib.h>

class GLClientState {
public:
    typedef enum {
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
    } StateLocation;

    typedef struct {
        GLint enabled;
        GLint size;
        GLenum type;
        GLsizei stride;
        void *data;
        GLuint bufferObject;
        GLenum glConst;
        unsigned int elementSize;
        bool enableDirty;  // true if any enable state has changed since last draw
    } VertexAttribState;

    typedef struct {
        int unpack_alignment;
        int pack_alignment;
    } PixelStoreState;

public:
    GLClientState(int nLocations = 32);
    ~GLClientState();
    const PixelStoreState *pixelStoreState() { return &m_pixelStore; }
    int setPixelStore(GLenum param, GLint value);
    GLuint currentArrayVbo() { return m_currentArrayVbo; }
    GLuint currentIndexVbo() { return m_currentIndexVbo; }
    void enable(int location, int state);
    void setState(int  location, int size, GLenum type, GLsizei stride, void *data);
    void setBufferObject(int location, GLuint id);
    const VertexAttribState  *getState(int location);
    const VertexAttribState  *getStateAndEnableDirty(int location, bool *enableChanged);
    int getLocation(GLenum loc);
    void setActiveTexture(int texUnit) {m_activeTexture = texUnit; };
    int getActiveTexture() const { return m_activeTexture; }
    int bindBuffer(GLenum target, GLuint id)
    {
        int err = 0;
        switch(target) {
        case GL_ARRAY_BUFFER:
            m_currentArrayVbo = id;
            break;
        case GL_ELEMENT_ARRAY_BUFFER:
            m_currentIndexVbo = id;
            break;
        default:
            err = -1;
        }
        return err;
    }
    size_t pixelDataSize(GLsizei width, GLsizei height, GLenum format, GLenum type, int pack);

private:
    PixelStoreState m_pixelStore;
    VertexAttribState *m_states;
    int m_nLocations;
    GLuint m_currentArrayVbo;
    GLuint m_currentIndexVbo;
    int m_activeTexture;


    bool validLocation(int location) { return (location >= 0 && location < m_nLocations); }

};
#endif
