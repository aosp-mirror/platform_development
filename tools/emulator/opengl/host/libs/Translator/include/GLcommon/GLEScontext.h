#ifndef GLES_CONTEXT_H
#define GLES_CONTEXT_H

#include "GLDispatch.h"
#include "GLESpointer.h"
#include "objectNameManager.h"
#include <utils/threads.h>
#include <string>

typedef std::map<GLenum,GLESpointer*>  ArraysMap;

enum TextureTarget {
TEXTURE_2D,
TEXTURE_CUBE_MAP,
NUM_TEXTURE_TARGETS
};

typedef struct _textureTargetState {
    GLuint texture;
    GLboolean enabled;
} textureTargetState;

typedef textureTargetState textureUnitState[NUM_TEXTURE_TARGETS];

class Version{
public:
    Version();
    Version(int major,int minor,int release);
    Version(const char* versionString);
    Version(const Version& ver);
    bool operator<(const Version& ver) const;
    Version& operator=(const Version& ver);
private:
    int m_major;
    int m_minor;
    int m_release;
};

struct GLSupport {
    GLSupport():maxLights(0),maxVertexAttribs(0),maxClipPlane(0),maxTexUnits(0), \
                maxTexImageUnits(0),maxTexSize(0) , \
                GL_EXT_TEXTURE_FORMAT_BGRA8888(false), GL_EXT_FRAMEBUFFER_OBJECT(false), \
                GL_ARB_VERTEX_BLEND(false), GL_ARB_MATRIX_PALETTE(false), \
                GL_EXT_PACKED_DEPTH_STENCIL(false) , GL_OES_READ_FORMAT(false), \
                GL_ARB_HALF_FLOAT_PIXEL(false), GL_NV_HALF_FLOAT(false), \
                GL_ARB_HALF_FLOAT_VERTEX(false),GL_SGIS_GENERATE_MIPMAP(false),
                GL_ARB_ES2_COMPATIBILITY(false),GL_OES_STANDARD_DERIVATIVES(false) {} ;
    int  maxLights;
    int  maxVertexAttribs;
    int  maxClipPlane;
    int  maxTexUnits;
    int  maxTexImageUnits;
    int  maxTexSize;
    Version glslVersion;
    bool GL_EXT_TEXTURE_FORMAT_BGRA8888;
    bool GL_EXT_FRAMEBUFFER_OBJECT;
    bool GL_ARB_VERTEX_BLEND;
    bool GL_ARB_MATRIX_PALETTE;
    bool GL_EXT_PACKED_DEPTH_STENCIL;
    bool GL_OES_READ_FORMAT;
    bool GL_ARB_HALF_FLOAT_PIXEL;
    bool GL_NV_HALF_FLOAT;
    bool GL_ARB_HALF_FLOAT_VERTEX;
    bool GL_SGIS_GENERATE_MIPMAP;
    bool GL_ARB_ES2_COMPATIBILITY;
    bool GL_OES_STANDARD_DERIVATIVES;

};

struct ArrayData{
    ArrayData():data(NULL),
                type(0),
                stride(0),
                allocated(false){};

    void*        data;
    GLenum       type;
    unsigned int stride;
    bool         allocated;
};

class GLESConversionArrays
{
public:
    GLESConversionArrays():m_current(0){};
    void setArr(void* data,unsigned int stride,GLenum type);
    void allocArr(unsigned int size,GLenum type);
    ArrayData& operator[](int i);
    void* getCurrentData();
    ArrayData& getCurrentArray();
    unsigned int getCurrentIndex();
    void operator++();

    ~GLESConversionArrays();
private:
    std::map<GLenum,ArrayData> m_arrays;
    unsigned int m_current;
};

class GLEScontext{
public:
    virtual void init();
    GLEScontext();
    GLenum getGLerror();
    void setGLerror(GLenum err);
    void setShareGroup(ShareGroupPtr grp){m_shareGroup = grp;};
    ShareGroupPtr shareGroup() const { return m_shareGroup; }
    virtual void setActiveTexture(GLenum tex);
    unsigned int getBindedTexture(GLenum target);
    unsigned int getBindedTexture(GLenum unit,GLenum target);
    void setBindedTexture(GLenum target,unsigned int tex);
    bool isTextureUnitEnabled(GLenum unit);
    void setTextureEnabled(GLenum target, GLenum enable);
    ObjectLocalName getDefaultTextureName(GLenum target);
    bool isInitialized() { return m_initialized; };
    void setUnpackAlignment(GLint param){ m_unpackAlignment = param; };
    GLint getUnpackAlignment(){ return m_unpackAlignment; };

    bool  isArrEnabled(GLenum);
    void  enableArr(GLenum arr,bool enable);
    const GLvoid* setPointer(GLenum arrType,GLint size,GLenum type,GLsizei stride,const GLvoid* data,bool normalize = false);
    virtual const GLESpointer* getPointer(GLenum arrType);
    virtual void setupArraysPointers(GLESConversionArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct) = 0;
    void bindBuffer(GLenum target,GLuint buffer);
    void unbindBuffer(GLuint buffer);
    bool isBuffer(GLuint buffer);
    bool isBindedBuffer(GLenum target);
    GLvoid* getBindedBuffer(GLenum target);
    void getBufferSize(GLenum target,GLint* param);
    void getBufferUsage(GLenum target,GLint* param);
    bool setBufferData(GLenum target,GLsizeiptr size,const GLvoid* data,GLenum usage);
    bool setBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const GLvoid* data);
    const char * getExtensionString();
    const char * getRendererString() const;
    void getGlobalLock();
    void releaseGlobalLock();
    virtual GLSupport*  getCaps(){return &s_glSupport;};
    virtual ~GLEScontext();
    virtual int getMaxTexUnits() = 0;
    virtual void drawValidate(void);

    void setRenderbufferBinding(GLuint rb) { m_renderbuffer = rb; }
    GLuint getRenderbufferBinding() const { return m_renderbuffer; }
    void setFramebufferBinding(GLuint fb) { m_framebuffer = fb; }
    GLuint getFramebufferBinding() const { return m_framebuffer; }

    static GLDispatch& dispatcher(){return s_glDispatch;};

    static int getMaxLights(){return s_glSupport.maxLights;}
    static int getMaxClipPlanes(){return s_glSupport.maxClipPlane;}
    static int getMaxTexSize(){return s_glSupport.maxTexSize;}
    static Version glslVersion(){return s_glSupport.glslVersion;}
    static bool isAutoMipmapSupported(){return s_glSupport.GL_SGIS_GENERATE_MIPMAP;}
    static TextureTarget GLTextureTargetToLocal(GLenum target);
    static int findMaxIndex(GLsizei count,GLenum type,const GLvoid* indices);

    virtual bool glGetIntegerv(GLenum pname, GLint *params);
    virtual bool glGetBooleanv(GLenum pname, GLboolean *params);
    virtual bool glGetFloatv(GLenum pname, GLfloat *params);
    virtual bool glGetFixedv(GLenum pname, GLfixed *params);

protected:
    virtual bool needConvert(GLESConversionArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct,GLESpointer* p,GLenum array_id) = 0;
    void convertDirect(GLESConversionArrays& fArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p);
    void convertDirectVBO(GLESConversionArrays& fArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p);
    void convertIndirect(GLESConversionArrays& fArrs,GLsizei count,GLenum type,const GLvoid* indices,GLenum array_id,GLESpointer* p);
    void convertIndirectVBO(GLESConversionArrays& fArrs,GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p);
    void initCapsLocked(const GLubyte * extensionString);
    virtual void initExtensionString() =0;
    static android::Mutex s_lock;
    static GLDispatch     s_glDispatch;
    bool                  m_initialized;
    unsigned int          m_activeTexture;
    GLint                 m_unpackAlignment;
    ArraysMap             m_map;
    static std::string*   s_glExtensions;
    static std::string    s_glRenderer;
    static GLSupport      s_glSupport;

private:

    virtual void setupArr(const GLvoid* arr,GLenum arrayType,GLenum dataType,GLint size,GLsizei stride, GLboolean normalized, int pointsIndex = -1) = 0 ;
    GLuint getBuffer(GLenum target);

    ShareGroupPtr         m_shareGroup;
    GLenum                m_glError;
    textureUnitState*     m_texState;
    unsigned int          m_arrayBuffer;
    unsigned int          m_elementBuffer;
    GLuint                m_renderbuffer;
    GLuint                m_framebuffer;
};

#endif

