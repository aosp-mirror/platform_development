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

#include "GLEScmContext.h"
#include "GLEScmUtils.h"
#include <GLcommon/GLutils.h>
#include <string.h>
#include <GLES/gl.h>

GLcmSupport      GLEScmContext::s_glSupport;

void GLEScmContext::init() {
    android::Mutex::Autolock mutex(s_lock);
    if(!m_initialized) {
        int maxTexUnits;
        s_glDispatch.dispatchFuncs(GLES_1_1);
        s_glDispatch.glGetIntegerv(GL_MAX_CLIP_PLANES,&s_glSupport.maxClipPlane);
        s_glDispatch.glGetIntegerv(GL_MAX_LIGHTS,&s_glSupport.maxLights);
        s_glDispatch.glGetIntegerv(GL_MAX_TEXTURE_SIZE,&s_glSupport.maxTexSize);
        s_glDispatch.glGetIntegerv(GL_MAX_TEXTURE_UNITS,&maxTexUnits);
        s_glSupport.maxTexUnits = maxTexUnits < MAX_TEX_UNITS ? maxTexUnits:MAX_TEX_UNITS;
    }

    m_texCoords = new GLESpointer[s_glSupport.maxTexUnits];
    m_map[GL_TEXTURE_COORD_ARRAY]  = &m_texCoords[m_activeTexture];
    m_initialized = true;
}

GLEScmContext::GLEScmContext():GLEScontext(),m_pointsIndex(-1){

    m_map[GL_COLOR_ARRAY]          = new GLESpointer();
    m_map[GL_NORMAL_ARRAY]         = new GLESpointer();
    m_map[GL_VERTEX_ARRAY]         = new GLESpointer();
    m_map[GL_POINT_SIZE_ARRAY_OES] = new GLESpointer();
}


void GLEScmContext::setActiveTexture(GLenum tex) {
   m_activeTexture = tex - GL_TEXTURE0;
   m_map[GL_TEXTURE_COORD_ARRAY] = &m_texCoords[m_activeTexture];
}

GLEScmContext::~GLEScmContext(){
    if(m_texCoords){
        delete[] m_texCoords;
        m_texCoords = NULL;
    }
}


//sending data to server side
void GLEScmContext::sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int index) {
    switch(arrayType) {
        case GL_VERTEX_ARRAY:
            s_glDispatch.glVertexPointer(size,GL_FLOAT,stride,arr);
            break;
        case GL_NORMAL_ARRAY:
            s_glDispatch.glNormalPointer(GL_FLOAT,stride,arr);
            break;
        case GL_TEXTURE_COORD_ARRAY:
            s_glDispatch.glTexCoordPointer(size,GL_FLOAT,stride,arr);
            break;
        case GL_COLOR_ARRAY:
            s_glDispatch.glColorPointer(size,GL_FLOAT,stride,arr);
            break;
        case GL_POINT_SIZE_ARRAY_OES:
            m_pointsIndex = index;
            break;
    }
}

void GLEScmContext::convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct) {
    ArraysMap::iterator it;
    unsigned int index = 0;
    m_pointsIndex = -1;

    //going over all clients arrays Pointers
    for ( it=m_map.begin() ; it != m_map.end(); it++ ) {
        GLenum array_id   = (*it).first;
        GLESpointer* p = (*it).second;

        if(array_id == GL_TEXTURE_COORD_ARRAY) continue; //handling textures later
        chooseConvertMethod(fArrs,first,count,type,indices,direct,p,array_id,index);
    }

    unsigned int activeTexture = m_activeTexture + GL_TEXTURE0;

    s_lock.lock();
    int maxTexUnits = s_glSupport.maxTexUnits;
    s_lock.unlock();

    //converting all texture coords arrays
    for(int i=0; i< maxTexUnits;i++) {

        unsigned int tex = GL_TEXTURE0+i;
        setActiveTexture(tex);
        s_glDispatch.glClientActiveTexture(tex);

        GLenum array_id   = GL_TEXTURE_COORD_ARRAY;
        GLESpointer* p = m_map[array_id];
        chooseConvertMethod(fArrs,first,count,type,indices,direct,p,array_id,index);
    }

    setActiveTexture(activeTexture);
    s_glDispatch.glClientActiveTexture(activeTexture);
}

void GLEScmContext::drawPoints(PointSizeIndices* points) {

    GLushort* indices = NULL;
    int last_size = 0;

    //drawing each group of vertices by the points size
    for(PointSizeIndices::iterator it = points->begin();it != points->end(); it++) {
            int count = (*it).second.size();
            int pointSize = (*it).first;
            std::vector<int>& arr = (*it).second;

            if(count > last_size) {
             if(indices) delete [] indices;
             indices = new GLushort[count];
            }
            int i = 0 ;
            for(std::vector<int>::iterator it2 = arr.begin();it2 != arr.end();it2++) {
                indices[i++] = (*it2);
            }
            s_glDispatch.glPointSize(pointSize);
            s_glDispatch.glDrawElements(GL_POINTS,count,GL_UNSIGNED_SHORT,indices);
    }
    if(indices) delete [] indices;
}

void  GLEScmContext::drawPointsData(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices_in,bool isElemsDraw) {
    const GLfloat  *pointsArr =  NULL;
    int stride = 0; //steps in GLfloats

    //choosing the right points sizes array source
    if(m_pointsIndex >= 0) { //point size array was converted
        pointsArr=fArrs.arrays[m_pointsIndex];
        stride = 1;
    } else {
        GLESpointer* p = m_map[GL_POINT_SIZE_ARRAY_OES];
        pointsArr = static_cast<const GLfloat*>(isBindedBuffer(GL_ARRAY_BUFFER)?p->getBufferData():p->getArrayData());
        stride = p->getStride()?p->getStride()/sizeof(GLfloat):1;
    }

    //filling  arrays before sorting them
    PointSizeIndices  points;
    if(isElemsDraw) {
        for(int i=0; i< count; i++) {
            GLushort index = (type == GL_UNSIGNED_SHORT?
                    static_cast<const GLushort*>(indices_in)[i]:
                    static_cast<const GLubyte*>(indices_in)[i]);
            points[pointsArr[index*stride]].push_back(index);
        }
    } else {
        for(int i=0; i< count; i++) {
            points[pointsArr[first+i*stride]].push_back(i+first);
        }
    }
    drawPoints(&points);
}

void  GLEScmContext::drawPointsArrs(GLESFloatArrays& arrs,GLint first,GLsizei count) {
    drawPointsData(arrs,first,count,0,NULL,false);
}

void GLEScmContext::drawPointsElems(GLESFloatArrays& arrs,GLsizei count,GLenum type,const GLvoid* indices_in) {
    drawPointsData(arrs,0,count,type,indices_in,true);
}

