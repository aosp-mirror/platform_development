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
#include "GLEScontext.h"
#include "GLESutils.h"
#include "GLfixed_ops.h"
#include "RangeManip.h"
#include <GLcommon/GLutils.h>
#include <string.h>
#include <GLES/gl.h>

//declerations
static void convertDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize);
static void convertIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize);
static int findMaxIndex(GLsizei count,GLenum type,const GLvoid* indices);


GLDispatch     GLEScontext::s_glDispatch;
GLsupport      GLEScontext::s_glSupport;
android::Mutex GLEScontext::s_lock;

GLESFloatArrays::~GLESFloatArrays() {
    for(std::map<GLenum,GLfloat*>::iterator it = arrays.begin(); it != arrays.end();it++) {
        GLfloat* p = (*it).second;
        if(p) {
            delete[] p;
        }
    }
}

GLEScontext::~GLEScontext() {
    for(ArraysMap::iterator it = m_map.begin(); it != m_map.end();it++) {
        GLESpointer* p = (*it).second;
        if(p) {
            delete[] p;
        }
    }
}

void GLEScontext::init() {
    android::Mutex::Autolock mutex(s_lock);
    if(!m_initialized) {
        int maxTexUnits;
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

GLEScontext::GLEScontext():m_glError(GL_NO_ERROR),m_activeTexture(0),m_arrayBuffer(0),m_elementBuffer(0),m_pointsIndex(-1),m_initialized(false) {

    s_glDispatch.dispatchFuncs();

    m_map[GL_COLOR_ARRAY]          = new GLESpointer();
    m_map[GL_NORMAL_ARRAY]         = new GLESpointer();
    m_map[GL_VERTEX_ARRAY]         = new GLESpointer();
    m_map[GL_POINT_SIZE_ARRAY_OES] = new GLESpointer();
}

GLDispatch& GLEScontext::dispatcher() {
    return s_glDispatch;
}

GLenum GLEScontext::getGLerror() {
    return m_glError;
}

void GLEScontext::setGLerror(GLenum err) {
    m_glError = err;
}

void GLEScontext::setActiveTexture(GLenum tex) {
   m_activeTexture = tex - GL_TEXTURE0;
   m_map[GL_TEXTURE_COORD_ARRAY] = &m_texCoords[m_activeTexture];
}

const GLvoid* GLEScontext::setPointer(GLenum arrType,GLint size,GLenum type,GLsizei stride,const GLvoid* data) {
    GLuint bufferName = m_arrayBuffer;
    if(bufferName) {
        unsigned int offset = reinterpret_cast<unsigned int>(data);
        GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
        m_map[arrType]->setBuffer(size,type,stride,vbo,offset);
        return  static_cast<const unsigned char*>(vbo->getData()) +  offset;
    }
    m_map[arrType]->setArray(size,type,stride,data);
    return data;
}


void GLEScontext::enableArr(GLenum arr,bool enable) {
    m_map[arr]->enable(enable);
}

bool GLEScontext::isArrEnabled(GLenum arr) {
    return m_map[arr]->isEnable();
}

const GLESpointer* GLEScontext::getPointer(GLenum arrType) {
    if(m_map.find(arrType) != m_map.end()) return m_map[arrType];
    return NULL;
}

//sending data to server side
void GLEScontext::sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int index) {
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

static void convertDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize) {

    for(unsigned int i = 0; i < nBytes;i+=strideOut) {
        const GLfixed* fixed_data = (const GLfixed *)dataIn;
        //filling attrib
        for(int j=0;j<attribSize;j++) {
            reinterpret_cast<GLfloat*>(&static_cast<unsigned char*>(dataOut)[i])[j] = X2F(fixed_data[j]);
        }
        dataIn += strideIn;
    }
}

static void convertIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize) {
    for(int i = 0 ;i < count ;i++) {
        unsigned short index = indices_type == GL_UNSIGNED_BYTE? ((GLubyte *)indices)[i]:
                                                             ((GLushort *)indices)[i];
        const GLfixed* fixed_data = (GLfixed *)(dataIn  + index*strideIn);
        GLfloat* float_data = reinterpret_cast<GLfloat*>(static_cast<unsigned char*>(dataOut) + index*strideOut);

        for(int j=0;j<attribSize;j++) {
            float_data[j] = X2F(fixed_data[j]);
         }
    }
}

static void directToBytesRanges(GLint first,GLsizei count,GLESpointer* p,RangeList& list) {

    int attribSize = p->getSize()*4; //4 is the sizeof GLfixed or GLfloat in bytes
    int stride = p->getStride()?p->getStride():attribSize;
    int start  = p->getBufferOffset()+first*attribSize;
    if(!p->getStride()) {
        list.addRange(Range(start,count*attribSize));
    } else {
        for(int i = 0 ;i < count; i++,start+=stride) {
            list.addRange(Range(start,attribSize));
        }
    }
}

static void indirectToBytesRanges(const GLvoid* indices,GLenum indices_type,GLsizei count,GLESpointer* p,RangeList& list) {

    int attribSize = p->getSize() * 4; //4 is the sizeof GLfixed or GLfloat in bytes
    int stride = p->getStride()?p->getStride():attribSize;
    int start  = p->getBufferOffset();
    for(int i=0 ; i < count; i++) {
        GLushort index = (indices_type == GL_UNSIGNED_SHORT?
                         static_cast<const GLushort*>(indices)[i]:
                         static_cast<const GLubyte*>(indices)[i]);
        list.addRange(Range(start+index*stride,attribSize));

    }
}

int bytesRangesToIndices(RangeList& ranges,GLESpointer* p,GLushort* indices) {

    int attribSize = p->getSize() * 4; //4 is the sizeof GLfixed or GLfloat in bytes
    int stride = p->getStride()?p->getStride():attribSize;
    int offset = p->getBufferOffset();

    int n = 0;
    for(int i=0;i<ranges.size();i++) {
        int startIndex = (ranges[i].getStart() - offset) / stride;
        int nElements = ranges[i].getSize()/attribSize;
        for(int j=0;j<nElements;j++) {
            indices[n++] = startIndex+j;
        }
    }
    return n;
}

void GLEScontext::convertDirect(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p,unsigned int& index) {
    GLenum type    = p->getType();
    if(isArrEnabled(array_id) && type == GL_FIXED) {
        int attribSize = p->getSize();
        unsigned int size = attribSize*count + first;
        fArrs.arrays[index] = new GLfloat[size];
        int stride = p->getStride()?p->getStride():sizeof(GLfixed)*attribSize;
        const char* data = (const char*)p->getArrayData() + (first*stride);

        convertDirectLoop(data,stride,fArrs.arrays[index],size*sizeof(GLfloat),attribSize*sizeof(GLfloat),attribSize);
        sendArr(fArrs.arrays[index],array_id,attribSize,0,index);
        index++;
    }
}

void GLEScontext::convertDirectVBO(GLint first,GLsizei count,GLenum array_id,GLESpointer* p) {
    GLenum type    = p->getType();
    if(isArrEnabled(array_id) && type == GL_FIXED) {
        RangeList ranges;
        RangeList conversions;
        GLushort* indices = NULL;
        int attribSize = p->getSize();
        int stride = p->getStride()?p->getStride():sizeof(GLfixed)*attribSize;
        unsigned int size = p->getStride()?p->getStride()*count:attribSize*count*sizeof(GLfixed);
        char* data = (char*)p->getBufferData() + (first*stride);

        if(p->bufferNeedConversion()) {
            directToBytesRanges(first,count,p,ranges); //converting indices range to buffer bytes ranges by offset
            p->getBufferConversions(ranges,conversions); // getting from the buffer the relevant ranges that still needs to be converted

            if(conversions.size()) { // there are some elements to convert
               indices = new GLushort[count];
               int nIndices = bytesRangesToIndices(conversions,p,indices); //converting bytes ranges by offset to indices in this array
               convertIndirectLoop(data,stride,data,nIndices,GL_UNSIGNED_SHORT,indices,stride,attribSize);
            }
        }

        sendArr(data,array_id,attribSize,p->getStride());
        if(indices) delete[] indices;
    }
}

void GLEScontext::convertIndirect(GLESFloatArrays& fArrs,GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p,unsigned int& index_out) {
    GLenum type    = p->getType();
    int maxElements = findMaxIndex(count,type,indices) + 1;

    if(isArrEnabled(array_id) && type == GL_FIXED) {
        int attribSize = p->getSize();
        int size = attribSize * maxElements;
        fArrs.arrays[index_out] = new GLfloat[size];
        int stride = p->getStride()?p->getStride():sizeof(GLfixed)*attribSize;

        const char* data = (const char*)p->getArrayData();
        convertIndirectLoop(data,stride,fArrs.arrays[index_out],count,indices_type,indices,attribSize*sizeof(GLfloat),attribSize);
        sendArr(fArrs.arrays[index_out],array_id,attribSize,0,index_out);
        index_out++;
    }
}

void GLEScontext::convertIndirectVBO(GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p) {
    GLenum type    = p->getType();

    if(isArrEnabled(array_id) && type == GL_FIXED) {
        RangeList ranges;
        RangeList conversions;
        GLushort* conversionIndices = NULL;
        int attribSize = p->getSize();
        int stride = p->getStride()?p->getStride():sizeof(GLfixed)*attribSize;
        char* data = static_cast<char*>(p->getBufferData());
        if(p->bufferNeedConversion()) {
            indirectToBytesRanges(indices,indices_type,count,p,ranges); //converting indices range to buffer bytes ranges by offset
            p->getBufferConversions(ranges,conversions); // getting from the buffer the relevant ranges that still needs to be converted
            if(conversions.size()) { // there are some elements to convert
                conversionIndices = new GLushort[count];
                int nIndices = bytesRangesToIndices(conversions,p,conversionIndices); //converting bytes ranges by offset to indices in this array
                convertIndirectLoop(data,stride,data,nIndices,GL_UNSIGNED_SHORT,conversionIndices,stride,attribSize);
            }
        }
        sendArr(data,array_id,attribSize,p->getStride());
        if(conversionIndices) delete[] conversionIndices;
    }
}

void GLEScontext::chooseConvertMethod(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct,GLESpointer* p,GLenum array_id, unsigned int& index) {

    bool vertexVBO = m_arrayBuffer!= 0;
    if(direct) {
        if(vertexVBO) {
            convertDirectVBO(first,count,array_id,p);
        } else {
            convertDirect(fArrs,first,count,array_id,p,index);
        }
    } else {
        if(vertexVBO) {
            convertIndirectVBO(count,type,indices,array_id,p);
        } else {
            convertIndirect(fArrs,count,type,indices,array_id,p,index);
        }
    }
}

void GLEScontext::convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct) {
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


static int findMaxIndex(GLsizei count,GLenum type,const GLvoid* indices) {
    //finding max index
    int max = 0;
    if(type == GL_UNSIGNED_BYTE) {
        GLubyte*  b_indices  =(GLubyte *)indices;
        for(int i=0;i<count;i++) {
            if(b_indices[i] > max) max = b_indices[i];
        }
    } else {
        GLushort* us_indices =(GLushort *)indices;
        for(int i=0;i<count;i++) {
            if(us_indices[i] > max) max = us_indices[i];
        }
    }
    return max;
}

//TODO change it into merge sort to work in O(nlogn)
void sortPoints(GLfloat* sortedPoints,GLushort* sortedIndices,int size) {

    int flag = 1;    // set flag to 1 to start first pass
    for(int i = 1; (i <= size) && flag; i++) {
        flag = 0;
        for (int j=0; j < (size -1); j++) {
            if (sortedPoints[j+1] < sortedPoints[j]) {
                swap<GLfloat>(sortedPoints[j],sortedPoints[j+1]);
                swap<GLushort>(sortedIndices[j],sortedIndices[j+1]);
                flag = 1;               // indicates that a swap occurred.
            }
        }
    }
}


void GLEScontext::drawPoints(PointSizeIndices* points) {

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

void  GLEScontext::drawPointsData(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices_in,bool isElemsDraw) {
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

void  GLEScontext::drawPointsArrs(GLESFloatArrays& arrs,GLint first,GLsizei count) {
    drawPointsData(arrs,first,count,0,NULL,false);
}

void GLEScontext::drawPointsElems(GLESFloatArrays& arrs,GLsizei count,GLenum type,const GLvoid* indices_in) {
    drawPointsData(arrs,0,count,type,indices_in,true);
}

void GLEScontext::bindBuffer(GLenum target,GLuint buffer) {
    if(target == GL_ARRAY_BUFFER) {
        m_arrayBuffer = buffer;
    } else {
       m_elementBuffer = buffer;
    }
}

//checks if any buffer is binded to target
bool GLEScontext::isBindedBuffer(GLenum target) {
    if(target == GL_ARRAY_BUFFER) {
        return m_arrayBuffer != 0;
    } else {
        return m_elementBuffer != 0;
    }
}

GLuint GLEScontext::getBuffer(GLenum target) {
    return target == GL_ARRAY_BUFFER ? m_arrayBuffer:m_elementBuffer;
}

GLvoid* GLEScontext::getBindedBuffer(GLenum target) {
    GLuint bufferName = getBuffer(target);
    if(!bufferName) return NULL;
    
    GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
    return vbo->getData();
}

void GLEScontext::getBufferSize(GLenum target,GLint* param) {
    GLuint bufferName = getBuffer(target);
    GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
    *param = vbo->getSize();
}

void GLEScontext::getBufferUsage(GLenum target,GLint* param) {
    GLuint bufferName = getBuffer(target);
    GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
    *param = vbo->getUsage();
}

bool GLEScontext::setBufferData(GLenum target,GLsizeiptr size,const GLvoid* data,GLenum usage) {
    GLuint bufferName = getBuffer(target);
    if(!bufferName) return false;
    GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
    return vbo->setBuffer(size,usage,data);
}

bool GLEScontext::setBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const GLvoid* data) {

    GLuint bufferName = getBuffer(target);
    if(!bufferName) return false;
    GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
    return vbo->setSubBuffer(offset,size,data);
}
