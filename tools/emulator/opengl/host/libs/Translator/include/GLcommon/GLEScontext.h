#ifndef GLES_CONTEXT_H
#define GLES_CONTEXT_H

#include "GLDispatch.h"
#include "GLESpointer.h"
#include "objectNameManager.h"
#include <utils/threads.h>

#define MAX_TEX_UNITS 8

typedef std::map<GLenum,GLESpointer*>  ArraysMap;

struct GLESFloatArrays
{
    GLESFloatArrays(){};
    ~GLESFloatArrays();
    std::map<GLenum,GLfloat*> arrays;
};

class GLEScontext{
public:
    virtual void init() = 0;
    GLEScontext();
    GLenum getGLerror();
    void setGLerror(GLenum err);
    void setShareGroup(ShareGroupPtr grp){m_shareGroup = grp;};
    virtual void setActiveTexture(GLenum tex);
    unsigned int getBindedTexture(){return m_tex2DBind[m_activeTexture];};
    void setBindedTexture(unsigned int tex){ m_tex2DBind[m_activeTexture] = tex;};

    bool  isArrEnabled(GLenum);
    void  enableArr(GLenum arr,bool enable);
    const GLvoid* setPointer(GLenum arrType,GLint size,GLenum type,GLsizei stride,const GLvoid* data,bool normalize = false);
    const GLESpointer* getPointer(GLenum arrType);
    virtual void convertArrs(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct) = 0;
    void bindBuffer(GLenum target,GLuint buffer);
    void unbindBuffer(GLuint buffer);
    bool isBuffer(GLuint buffer);
    bool isBindedBuffer(GLenum target);
    GLvoid* getBindedBuffer(GLenum target);
    void getBufferSize(GLenum target,GLint* param);
    void getBufferUsage(GLenum target,GLint* param);
    bool setBufferData(GLenum target,GLsizeiptr size,const GLvoid* data,GLenum usage);
    bool setBufferSubData(GLenum target,GLintptr offset,GLsizeiptr size,const GLvoid* data);

    virtual ~GLEScontext();

    static GLDispatch& dispatcher(){return s_glDispatch;};

protected:
    void chooseConvertMethod(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum type,const GLvoid* indices,bool direct,GLESpointer* p,GLenum array_id,unsigned int& index);

    static android::Mutex s_lock;
    static GLDispatch     s_glDispatch;
    bool                  m_initialized;
    unsigned int          m_activeTexture;
    ArraysMap             m_map;

private:

    virtual void sendArr(GLvoid* arr,GLenum arrayType,GLint size,GLsizei stride,int pointsIndex = -1) = 0 ;
    GLuint getBuffer(GLenum target);
    void convertDirect(GLESFloatArrays& fArrs,GLint first,GLsizei count,GLenum array_id,GLESpointer* p,unsigned int& index);
    void convertDirectVBO(GLint first,GLsizei count,GLenum array_id,GLESpointer* p);
    void convertIndirect(GLESFloatArrays& fArrs,GLsizei count,GLenum type,const GLvoid* indices,GLenum array_id,GLESpointer* p,unsigned int& index);
    void convertIndirectVBO(GLsizei count,GLenum indices_type,const GLvoid* indices,GLenum array_id,GLESpointer* p);

    ShareGroupPtr         m_shareGroup;
    GLenum                m_glError;
    unsigned int          m_tex2DBind[MAX_TEX_UNITS];
    unsigned int          m_arrayBuffer;
    unsigned int          m_elementBuffer;
};

#endif

