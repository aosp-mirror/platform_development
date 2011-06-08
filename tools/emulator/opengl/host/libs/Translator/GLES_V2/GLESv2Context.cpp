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

#include "GLESv2Context.h"



void GLESv2Context::init() {
    android::Mutex::Autolock mutex(s_lock);
    if(!m_initialized) {
        s_glDispatch.dispatchFuncs(GLES_2_0);
        initCapsLocked(s_glDispatch.glGetString(GL_EXTENSIONS));
        initExtensionString();

        for(int i=0; i < s_glSupport.maxVertexAttribs;i++){
            m_map[i] = new GLESpointer();
        }
    }
    m_initialized = true;
}

GLESv2Context::GLESv2Context():GLEScontext(){};

void GLESv2Context::convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct) {
    ArraysMap::iterator it;
    unsigned int index = 0;

    //going over all clients arrays Pointers
    for ( it=m_map.begin() ; it != m_map.end(); it++ ) {
        GLenum array_id   = (*it).first;
        GLESpointer* p = (*it).second;

        chooseConvertMethod(fArrs,first,count,type,indices,direct,p,array_id,index);
    }
}

//sending data to server side
void GLESv2Context::sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int index) {
     s_glDispatch.glVertexAttribPointer(arrayType,size,GL_FLOAT,GL_FALSE,stride,arr);
}

void GLESv2Context::initExtensionString() {
    *s_glExtensions = "GL_OES_EGL_image GL_OES_depth24 GL_OES_depth32 GL_OES_element_index_uint "
                      "GL_OES_standard_derivatives GL_OES_texture_float GL_OES_texture_float_linear ";
    if (s_glSupport.GL_ARB_HALF_FLOAT_PIXEL || s_glSupport.GL_NV_HALF_FLOAT)       
        *s_glExtensions+="GL_OES_texture_half_float GL_OES_texture_half_float_linear ";
    if (s_glSupport.GL_NV_PACKED_DEPTH_STENCIL)
        *s_glExtensions+="GL_OES_packed_depth_stencil ";
    if (s_glSupport.GL_ARB_HALF_FLOAT_VERTEX)
        *s_glExtensions+="GL_OES_vertex_half_float ";
}
