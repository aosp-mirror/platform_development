#include <GLcommon/GLEScontext.h>
#include <GLcommon/GLconversion_macros.h>
#include <GLcommon/GLESmacros.h>
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <GLcommon/GLESvalidate.h>
#include <GLcommon/TextureUtils.h>
#include <GLcommon/FramebufferData.h>
#include <strings.h>

//decleration
static void convertFixedDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize);
static void convertFixedIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize);
static void convertByteDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize);
static void convertByteIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize);

GLESConversionArrays::~GLESConversionArrays() {
    for(std::map<GLenum,ArrayData>::iterator it = m_arrays.begin(); it != m_arrays.end();it++) {
        if((*it).second.allocated){
            if((*it).second.type == GL_FLOAT){
                GLfloat* p = (GLfloat *)((*it).second.data);
                if(p) delete[] p;
            } else if((*it).second.type == GL_SHORT){
                GLshort* p = (GLshort *)((*it).second.data);
                if(p) delete[] p;
            }
        }
    }
}

void GLESConversionArrays::allocArr(unsigned int size,GLenum type){
    if(type == GL_FIXED){
        m_arrays[m_current].data = new GLfloat[size];
        m_arrays[m_current].type = GL_FLOAT;
    } else if(type == GL_BYTE){
        m_arrays[m_current].data = new GLshort[size];
        m_arrays[m_current].type = GL_SHORT;
    }
    m_arrays[m_current].stride = 0;
    m_arrays[m_current].allocated = true;
}

void GLESConversionArrays::setArr(void* data,unsigned int stride,GLenum type){
   m_arrays[m_current].type = type;
   m_arrays[m_current].data = data;
   m_arrays[m_current].stride = stride;
   m_arrays[m_current].allocated = false;
}

void* GLESConversionArrays::getCurrentData(){
    return m_arrays[m_current].data;
}

ArrayData& GLESConversionArrays::getCurrentArray(){
    return m_arrays[m_current];
}

unsigned int GLESConversionArrays::getCurrentIndex(){
    return m_current;
}

ArrayData& GLESConversionArrays::operator[](int i){
    return m_arrays[i];
}

void GLESConversionArrays::operator++(){
    m_current++;
}

GLDispatch     GLEScontext::s_glDispatch;
android::Mutex GLEScontext::s_lock;
std::string*   GLEScontext::s_glExtensions= NULL;
std::string    GLEScontext::s_glRenderer;
GLSupport      GLEScontext::s_glSupport;

Version::Version():m_major(0),
                   m_minor(0),
                   m_release(0){};

Version::Version(int major,int minor,int release):m_major(major),
                                                  m_minor(minor),
                                                  m_release(release){};

Version::Version(const Version& ver):m_major(ver.m_major),
                                     m_minor(ver.m_minor),
                                     m_release(ver.m_release){}

Version::Version(const char* versionString){
    m_release = 0;
    if((!versionString) ||
      ((!(sscanf(versionString,"%d.%d"   ,&m_major,&m_minor) == 2)) &&
       (!(sscanf(versionString,"%d.%d.%d",&m_major,&m_minor,&m_release) == 3)))){
        m_major = m_minor = 0; // the version is not in the right format
    }
}

Version& Version::operator=(const Version& ver){
    m_major   = ver.m_major;
    m_minor   = ver.m_minor;
    m_release = ver.m_release;
    return *this;
}

bool Version::operator<(const Version& ver) const{
    if(m_major < ver.m_major) return true;
    if(m_major == ver.m_major){
        if(m_minor < ver.m_minor) return true;
        if(m_minor == ver.m_minor){
           return m_release < ver.m_release;
        }
    }
    return false;
}

void GLEScontext::init() {

    if (!s_glExtensions) {
        initCapsLocked(s_glDispatch.glGetString(GL_EXTENSIONS));
        s_glExtensions = new std::string("");
    }

    if (!m_initialized) {
        initExtensionString();

        int maxTexUnits = getMaxTexUnits();
        m_texState = new textureUnitState[maxTexUnits];
        for (int i=0;i<maxTexUnits;++i) {
            for (int j=0;j<NUM_TEXTURE_TARGETS;++j)
            {
                m_texState[i][j].texture = 0;
                m_texState[i][j].enabled = GL_FALSE;
            }
        }
    }
}

GLEScontext::GLEScontext():
                           m_initialized(false)    ,
                           m_activeTexture(0)      ,
                           m_unpackAlignment(4)    ,
                           m_glError(GL_NO_ERROR)  ,
                           m_texState(0)          ,
                           m_arrayBuffer(0)        ,
                           m_elementBuffer(0),
                           m_renderbuffer(0),
                           m_framebuffer(0)
{
};

GLenum GLEScontext::getGLerror() {
    return m_glError;
}

void GLEScontext::setGLerror(GLenum err) {
    m_glError = err;
}

void GLEScontext::setActiveTexture(GLenum tex) {
   m_activeTexture = tex - GL_TEXTURE0;
}

GLEScontext::~GLEScontext() {
    for(ArraysMap::iterator it = m_map.begin(); it != m_map.end();it++) {
        GLESpointer* p = (*it).second;
        if(p) {
            delete p;
        }
    }
    delete[] m_texState;
    m_texState = NULL;
}

const GLvoid* GLEScontext::setPointer(GLenum arrType,GLint size,GLenum type,GLsizei stride,const GLvoid* data,bool normalize) {
    GLuint bufferName = m_arrayBuffer;
    if(bufferName) {
        unsigned int offset = reinterpret_cast<unsigned int>(data);
        GLESbuffer* vbo = static_cast<GLESbuffer*>(m_shareGroup->getObjectData(VERTEXBUFFER,bufferName).Ptr());
        m_map[arrType]->setBuffer(size,type,stride,vbo,bufferName,offset,normalize);
        return  static_cast<const unsigned char*>(vbo->getData()) +  offset;
    }
    m_map[arrType]->setArray(size,type,stride,data,normalize);
    return data;
}

void GLEScontext::enableArr(GLenum arr,bool enable) {
    m_map[arr]->enable(enable);
}

bool GLEScontext::isArrEnabled(GLenum arr) {
    return m_map[arr]->isEnable();
}

const GLESpointer* GLEScontext::getPointer(GLenum arrType) {
    if (m_map.find(arrType) != m_map.end()) return m_map[arrType];
    return NULL;
}

static void convertFixedDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize) {

    for(unsigned int i = 0; i < nBytes;i+=strideOut) {
        const GLfixed* fixed_data = (const GLfixed *)dataIn;
        //filling attrib
        for(int j=0;j<attribSize;j++) {
            reinterpret_cast<GLfloat*>(&static_cast<unsigned char*>(dataOut)[i])[j] = X2F(fixed_data[j]);
        }
        dataIn += strideIn;
    }
}

static void convertFixedIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize) {
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

static void convertByteDirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,unsigned int nBytes,unsigned int strideOut,int attribSize) {

    for(unsigned int i = 0; i < nBytes;i+=strideOut) {
        const GLbyte* byte_data = (const GLbyte *)dataIn;
        //filling attrib
        for(int j=0;j<attribSize;j++) {
            reinterpret_cast<GLshort*>(&static_cast<unsigned char*>(dataOut)[i])[j] = B2S(byte_data[j]);
        }
        dataIn += strideIn;
    }
}

static void convertByteIndirectLoop(const char* dataIn,unsigned int strideIn,void* dataOut,GLsizei count,GLenum indices_type,const GLvoid* indices,unsigned int strideOut,int attribSize) {
    for(int i = 0 ;i < count ;i++) {
        unsigned short index = indices_type == GL_UNSIGNED_BYTE? ((GLubyte *)indices)[i]:
                                                             ((GLushort *)indices)[i];
        const GLbyte* bytes_data = (GLbyte *)(dataIn  + index*strideIn);
        GLshort* short_data = reinterpret_cast<GLshort*>(static_cast<unsigned char*>(dataOut) + index*strideOut);

        for(int j=0;j<attribSize;j++) {
            short_data[j] = B2S(bytes_data[j]);
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

void GLEScontext::convertDirect(GLESConversionArrays& cArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p) {

    GLenum type    = p->getType();
    int attribSize = p->getSize();
    unsigned int size = attribSize*count + first;
    unsigned int bytes = type == GL_FIXED ? sizeof(GLfixed):sizeof(GLbyte);
    cArrs.allocArr(size,type);
    int stride = p->getStride()?p->getStride():bytes*attribSize;
    const char* data = (const char*)p->getArrayData() + (first*stride);

    if(type == GL_FIXED) {
        convertFixedDirectLoop(data,stride,cArrs.getCurrentData(),size*sizeof(GLfloat),attribSize*sizeof(GLfloat),attribSize);
    } else if(type == GL_BYTE) {
        convertByteDirectLoop(data,stride,cArrs.getCurrentData(),size*sizeof(GLshort),attribSize*sizeof(GLshort),attribSize);
    }
}

void GLEScontext::convertDirectVBO(GLESConversionArrays& cArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p) {

    RangeList ranges;
    RangeList conversions;
    GLushort* indices = NULL;
    GLenum type    = p->getType();
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
           convertFixedIndirectLoop(data,stride,data,nIndices,GL_UNSIGNED_SHORT,indices,stride,attribSize);
        }
    }
    if(indices) delete[] indices;
    cArrs.setArr(data,p->getStride(),GL_FLOAT);
}

int GLEScontext::findMaxIndex(GLsizei count,GLenum type,const GLvoid* indices) {
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

void GLEScontext::convertIndirect(GLESConversionArrays& cArrs,GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p) {
    GLenum type    = p->getType();
    int maxElements = findMaxIndex(count,type,indices) + 1;

    int attribSize = p->getSize();
    int size = attribSize * maxElements;
    unsigned int bytes = type == GL_FIXED ? sizeof(GLfixed):sizeof(GLbyte);
    cArrs.allocArr(size,type);
    int stride = p->getStride()?p->getStride():bytes*attribSize;

    const char* data = (const char*)p->getArrayData();
    if(type == GL_FIXED) {
        convertFixedIndirectLoop(data,stride,cArrs.getCurrentData(),count,indices_type,indices,attribSize*sizeof(GLfloat),attribSize);
    } else if(type == GL_BYTE){
        convertByteIndirectLoop(data,stride,cArrs.getCurrentData(),count,indices_type,indices,attribSize*sizeof(GLshort),attribSize);
    }
}

void GLEScontext::convertIndirectVBO(GLESConversionArrays& cArrs,GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p) {
    RangeList ranges;
    RangeList conversions;
    GLushort* conversionIndices = NULL;
    GLenum type    = p->getType();
    int attribSize = p->getSize();
    int stride = p->getStride()?p->getStride():sizeof(GLfixed)*attribSize;
    char* data = static_cast<char*>(p->getBufferData());
    if(p->bufferNeedConversion()) {
        indirectToBytesRanges(indices,indices_type,count,p,ranges); //converting indices range to buffer bytes ranges by offset
        p->getBufferConversions(ranges,conversions); // getting from the buffer the relevant ranges that still needs to be converted
        if(conversions.size()) { // there are some elements to convert
            conversionIndices = new GLushort[count];
            int nIndices = bytesRangesToIndices(conversions,p,conversionIndices); //converting bytes ranges by offset to indices in this array
            convertFixedIndirectLoop(data,stride,data,nIndices,GL_UNSIGNED_SHORT,conversionIndices,stride,attribSize);
        }
    }
    if(conversionIndices) delete[] conversionIndices;
    cArrs.setArr(data,p->getStride(),GL_FLOAT);
}



void GLEScontext::bindBuffer(GLenum target,GLuint buffer) {
    if(target == GL_ARRAY_BUFFER) {
        m_arrayBuffer = buffer;
    } else {
       m_elementBuffer = buffer;
    }
}

void GLEScontext::unbindBuffer(GLuint buffer) {
    if(m_arrayBuffer == buffer)
    {
        m_arrayBuffer = 0;
    }
    if(m_elementBuffer == buffer)
    {
        m_elementBuffer = 0;
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

const char * GLEScontext::getExtensionString() {
    const char * ret;
    s_lock.lock();
    if (s_glExtensions)
        ret = s_glExtensions->c_str();
    else
        ret="";
    s_lock.unlock();
    return ret;
}

const char * GLEScontext::getRendererString() const {
    return s_glRenderer.c_str();
}

void GLEScontext::getGlobalLock() {
    s_lock.lock();
}

void GLEScontext::releaseGlobalLock() {
    s_lock.unlock();
}


void GLEScontext::initCapsLocked(const GLubyte * extensionString)
{
    const char* cstring = (const char*)extensionString;

    s_glDispatch.glGetIntegerv(GL_MAX_VERTEX_ATTRIBS,&s_glSupport.maxVertexAttribs);
    s_glDispatch.glGetIntegerv(GL_MAX_CLIP_PLANES,&s_glSupport.maxClipPlane);
    s_glDispatch.glGetIntegerv(GL_MAX_LIGHTS,&s_glSupport.maxLights);
    s_glDispatch.glGetIntegerv(GL_MAX_TEXTURE_SIZE,&s_glSupport.maxTexSize);
    s_glDispatch.glGetIntegerv(GL_MAX_TEXTURE_UNITS,&s_glSupport.maxTexUnits);
    s_glDispatch.glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS,&s_glSupport.maxTexImageUnits);
    const GLubyte* glslVersion = s_glDispatch.glGetString(GL_SHADING_LANGUAGE_VERSION);
    s_glSupport.glslVersion = Version((const  char*)(glslVersion));

    if (strstr(cstring,"GL_EXT_bgra ")!=NULL)
        s_glSupport.GL_EXT_TEXTURE_FORMAT_BGRA8888 = true;

    if (strstr(cstring,"GL_EXT_framebuffer_object ")!=NULL)
        s_glSupport.GL_EXT_FRAMEBUFFER_OBJECT = true;

    if (strstr(cstring,"GL_ARB_vertex_blend ")!=NULL)
        s_glSupport.GL_ARB_VERTEX_BLEND = true;

    if (strstr(cstring,"GL_ARB_matrix_palette ")!=NULL)
        s_glSupport.GL_ARB_MATRIX_PALETTE = true;

    if (strstr(cstring,"GL_EXT_packed_depth_stencil ")!=NULL )
        s_glSupport.GL_EXT_PACKED_DEPTH_STENCIL = true;

    if (strstr(cstring,"GL_OES_read_format ")!=NULL)
        s_glSupport.GL_OES_READ_FORMAT = true;

    if (strstr(cstring,"GL_ARB_half_float_pixel ")!=NULL)
        s_glSupport.GL_ARB_HALF_FLOAT_PIXEL = true;

    if (strstr(cstring,"GL_NV_half_float ")!=NULL)
        s_glSupport.GL_NV_HALF_FLOAT = true;

    if (strstr(cstring,"GL_ARB_half_float_vertex ")!=NULL)
        s_glSupport.GL_ARB_HALF_FLOAT_VERTEX = true;

    if (strstr(cstring,"GL_SGIS_generate_mipmap ")!=NULL)
        s_glSupport.GL_SGIS_GENERATE_MIPMAP = true;

    if (strstr(cstring,"GL_ARB_ES2_compatibility ")!=NULL)
        s_glSupport.GL_ARB_ES2_COMPATIBILITY = true;

    if (strstr(cstring,"GL_OES_standard_derivatives ")!=NULL)
        s_glSupport.GL_OES_STANDARD_DERIVATIVES = true;

}

bool GLEScontext::isTextureUnitEnabled(GLenum unit) {
    for (int i=0;i<NUM_TEXTURE_TARGETS;++i) {
        if (m_texState[unit-GL_TEXTURE0][i].enabled)
            return true;
    }
    return false;
}

bool GLEScontext::glGetBooleanv(GLenum pname, GLboolean *params)
{
    GLint iParam;

    if(glGetIntegerv(pname, &iParam))
    {
        *params = (iParam != 0);
        return true;
    }

    return false;
}

bool GLEScontext::glGetFixedv(GLenum pname, GLfixed *params)
{
    bool result = false;
    GLint numParams = 1;

    GLint* iParams = new GLint[numParams];
    if (numParams>0 && glGetIntegerv(pname,iParams)) {
        while(numParams >= 0)
        {
            params[numParams] = I2X(iParams[numParams]);
            numParams--;
        }
        result = true;
    }
    delete [] iParams;

    return result;
}

bool GLEScontext::glGetFloatv(GLenum pname, GLfloat *params)
{
    bool result = false;
    GLint numParams = 1;

    GLint* iParams = new GLint[numParams];
    if (numParams>0 && glGetIntegerv(pname,iParams)) {
        while(numParams >= 0)
        {
            params[numParams] = (GLfloat)iParams[numParams];
            numParams--;
        }
        result = true;
    }
    delete [] iParams;

    return result;
}        

bool GLEScontext::glGetIntegerv(GLenum pname, GLint *params)
{
    switch(pname)
    {
        case GL_ARRAY_BUFFER_BINDING:
            *params = m_arrayBuffer;
            break;

        case GL_ELEMENT_ARRAY_BUFFER_BINDING:
            *params = m_elementBuffer;
            break;

        case GL_TEXTURE_BINDING_CUBE_MAP:
            *params = m_texState[m_activeTexture][TEXTURE_CUBE_MAP].texture;
            break;

        case GL_TEXTURE_BINDING_2D:
            *params = m_texState[m_activeTexture][TEXTURE_2D].texture;
            break;

        case GL_ACTIVE_TEXTURE:
            *params = m_activeTexture+GL_TEXTURE0;
            break;

        case GL_IMPLEMENTATION_COLOR_READ_TYPE_OES:
            *params = GL_UNSIGNED_BYTE;
            break;

        case GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES:
            *params = GL_RGBA;
            break;
        default:
            return false;
    }

    return true;
}

TextureTarget GLEScontext::GLTextureTargetToLocal(GLenum target) {
    TextureTarget value=TEXTURE_2D;
    switch (target) {
    case GL_TEXTURE_CUBE_MAP:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_X:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
        value = TEXTURE_CUBE_MAP;
        break;
    case GL_TEXTURE_2D:
        value = TEXTURE_2D;
        break;
    }
    return value;
}

unsigned int GLEScontext::getBindedTexture(GLenum target) {
    TextureTarget pos = GLTextureTargetToLocal(target);
    return m_texState[m_activeTexture][pos].texture;
}
  
unsigned int GLEScontext::getBindedTexture(GLenum unit, GLenum target) {
    TextureTarget pos = GLTextureTargetToLocal(target);
    return m_texState[unit-GL_TEXTURE0][pos].texture;
}

void GLEScontext::setBindedTexture(GLenum target, unsigned int tex) {
    TextureTarget pos = GLTextureTargetToLocal(target);
    m_texState[m_activeTexture][pos].texture = tex;
}

void GLEScontext::setTextureEnabled(GLenum target, GLenum enable) {
    TextureTarget pos = GLTextureTargetToLocal(target);
    m_texState[m_activeTexture][pos].enabled = enable;
}

#define INTERNAL_NAME(x) (x +0x100000000ll);

ObjectLocalName GLEScontext::getDefaultTextureName(GLenum target) {
    ObjectLocalName name = 0;
    switch (GLTextureTargetToLocal(target)) {
    case TEXTURE_2D:
        name = INTERNAL_NAME(0);
        break;
    case TEXTURE_CUBE_MAP:
        name = INTERNAL_NAME(1);
        break;
    default:
        name = 0;
        break;
    }
    return name;
}

void GLEScontext::drawValidate(void)
{
    if(m_framebuffer == 0)
        return;

    ObjectDataPtr fbObj = m_shareGroup->getObjectData(FRAMEBUFFER,m_framebuffer);
    if (fbObj.Ptr() == NULL)
        return;

    FramebufferData *fbData = (FramebufferData *)fbObj.Ptr();

    fbData->validate(this);
}
