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
#ifndef GLES_CONTEX_H
#define GLES_CONTEX_H

#include "GLDispatch.h"
#include "GLESpointer.h"
#include "GLESbuffer.h"
#include <map>
#include <vector>
#include <utils/threads.h>

#define MAX_TEX_UNITS 8

typedef std::map<GLenum,GLESpointer*>  ArraysMap;
typedef std::map<GLuint,GLESbuffer*>   BuffersMap;
typedef std::map<GLfloat,std::vector<int> > PointSizeIndices;

struct GLESFloatArrays
{
    GLESFloatArrays(){};
    ~GLESFloatArrays();
    std::map<GLenum,GLfloat*> arrays;
};


struct GLsupport {
    GLsupport():maxLights(0),maxClipPlane(0),maxTexUnits(0),maxTexSize(0){};
    int  maxLights;
    int  maxClipPlane;
    int  maxTexUnits;
    int  maxTexSize;
};

class GLEScontext
{
public:
    void init();
    GLEScontext();
    GLenum getGLerror();

    bool  isArrEnabled(GLenum);
    void  enableArr(GLenum arr,bool enable);
    void  setGLerror(GLenum err);
    void  setActiveTexture(GLenum tex);
    const GLvoid* setPointer(GLenum arrType,GLint size,GLenum type,GLsizei stride,const GLvoid* data);
    const GLESpointer* getPointer(GLenum arrType);

    void convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct);
    void drawPointsArrs(GLESFloatArrays& arrs,GLint first,GLsizei count);
    void drawPointsElems(GLESFloatArrays& arrs,GLsizei count,GLenum type,const GLvoid* indices);

    void genBuffers(GLsizei n,GLuint* buffers);
    void deleteBuffers(GLsizei n,const GLuint* buffers);
    void bindBuffer(GLenum target,GLuint buffer);
    bool isBuffer(GLuint buffer);
    bool isBindedBuffer(GLenum target);
    GLvoid* getBindedBuffer(GLenum target);
    void getBufferSize(GLenum target,GLint* param);
    void getBufferUsage(GLenum target,GLint* param);
    bool setBufferData(GLenum target,GLsizeiptr size,const GLvoid* data,GLenum usage);
    bool setBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const GLvoid* data);

    static GLDispatch& dispatcher();
    static int getMaxLights(){return s_glSupport.maxLights;}
    static int getMaxClipPlanes(){return s_glSupport.maxClipPlane;}
    static int getMaxTexUnits(){return s_glSupport.maxTexUnits;}
    static int getMaxTexSize(){return s_glSupport.maxTexSize;}

    ~GLEScontext();
private:

    GLuint getBuffer(GLenum target);
    void sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int pointsIndex = -1);
    void drawPoints(PointSizeIndices* points);
    void drawPointsData(GLESFloatArrays& arrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices_in,bool isElemsDraw);

    void chooseConvertMethod(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct,GLESpointer* p,GLenum array_id,unsigned int& index);
    void convertDirect(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p,unsigned int& index);
    void convertDirectVBO(GLint first,GLsizei count,GLenum array_id,GLESpointer* p);
    void convertIndirect(GLESFloatArrays& fArrs,GLsizei count,GLenum type,const GLvoid* indices,GLenum array_id,GLESpointer* p,unsigned int& index);
    void convertIndirectVBO(GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p);

    static GLDispatch     s_glDispatch;
    static GLsupport      s_glSupport;
    static android::Mutex s_lock;

    ArraysMap             m_map;
    GLESpointer*          m_texCoords;
    GLenum                m_glError;
    unsigned int          m_activeTexture;
    unsigned int          m_arrayBuffer;
    unsigned int          m_elementBuffer;

    unsigned int          m_minAvailableBuffer;
    BuffersMap            m_vbos;

    int                   m_pointsIndex;
    bool                  m_initialized;
};

#endif

