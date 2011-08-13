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
#ifndef _GL_SHARED_GROUP_H_
#define _GL_SHARED_GROUP_H_

#define GL_API
#ifndef ANDROID
#define GL_APIENTRY
#define GL_APIENTRYP
#endif

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <stdio.h>
#include <stdlib.h>
#include "ErrorLog.h"
#include <utils/KeyedVector.h>
#include <utils/threads.h>
#include <utils/List.h>
#include "FixedBuffer.h"
#include "SmartPtr.h"

struct BufferData {
    BufferData();
    BufferData(GLsizeiptr size, void * data);
    GLsizeiptr  m_size;
    FixedBuffer m_fixedBuffer;    
};

class ProgramData {
private:
    typedef struct _IndexInfo {
        GLint base;
        GLint size;
        GLenum type;
        GLint appBase;
        GLint hostLocsPerElement;
    } IndexInfo;

    GLuint m_numIndexes;
    IndexInfo* m_Indexes;
    bool m_initialized;
    bool m_locShiftWAR;
public:
    ProgramData();
    void initProgramData(GLuint numIndexes);
    bool isInitialized();
    virtual ~ProgramData();
    void setIndexInfo(GLuint index, GLint base, GLint size, GLenum type);
    GLuint getIndexForLocation(GLint location);
    GLenum getTypeForLocation(GLint location);

    bool needUniformLocationWAR() const { return m_locShiftWAR; }
    void setupLocationShiftWAR();
    GLint locationWARHostToApp(GLint hostLoc, GLint arrIndex);
    GLint locationWARAppToHost(GLint appLoc);
    
};

class GLSharedGroup {
private:
    android::DefaultKeyedVector<GLuint, BufferData*>    m_buffers;
    android::DefaultKeyedVector<GLuint, ProgramData*>    m_programs;
    android::List<GLuint> m_shaders;
    mutable android::Mutex       m_lock;
public:
    GLSharedGroup();
    ~GLSharedGroup();
    BufferData * getBufferData(GLuint bufferId);
    void    addBufferData(GLuint bufferId, GLsizeiptr size, void * data);
    void    updateBufferData(GLuint bufferId, GLsizeiptr size, void * data);
    GLenum  subUpdateBufferData(GLuint bufferId, GLintptr offset, GLsizeiptr size, void * data);
    void    deleteBufferData(GLuint);

    bool    isProgram(GLuint program);
    bool    isProgramInitialized(GLuint program);
    void    addProgramData(GLuint program); 
    void    initProgramData(GLuint program, GLuint numIndexes);
    void    deleteProgramData(GLuint program);
    void    setProgramIndexInfo(GLuint program, GLuint index, GLint base, GLint size, GLenum type);
    GLenum  getProgramUniformType(GLuint program, GLint location);
    void    setupLocationShiftWAR(GLuint program);
    GLint   locationWARHostToApp(GLuint program, GLint hostLoc, GLint arrIndex);
    GLint   locationWARAppToHost(GLuint program, GLint appLoc);
    bool    needUniformLocationWAR(GLuint program);

    void    addShaderData(GLuint shader);
    bool    isShader(GLuint shader);
    void    deleteShaderData(GLuint shader);

};

typedef SmartPtr<GLSharedGroup> GLSharedGroupPtr; 

#endif //_GL_SHARED_GROUP_H_
