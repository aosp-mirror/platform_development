#include "GLSharedGroup.h"

/**** BufferData ****/

BufferData::BufferData() : m_size(0) {};
BufferData::BufferData(GLsizeiptr size, void * data) : m_size(size) 
{
    void * buffer = NULL;
    if (size>0) buffer = m_fixedBuffer.alloc(size);
    if (data) memcpy(buffer, data, size);
}

/***** GLSharedGroup ****/

GLSharedGroup::GLSharedGroup() :
    m_buffers(android::DefaultKeyedVector<GLuint, BufferData*>(NULL))
{
}

GLSharedGroup::~GLSharedGroup()
{
    m_buffers.clear();
}

BufferData * GLSharedGroup::getBufferData(GLuint bufferId)
{
    android::AutoMutex _lock(m_lock);
    return m_buffers.valueFor(bufferId);    
}

void GLSharedGroup::addBufferData(GLuint bufferId, GLsizeiptr size, void * data)
{
    android::AutoMutex _lock(m_lock);
    m_buffers.add(bufferId, new BufferData(size, data));
}

void GLSharedGroup::updateBufferData(GLuint bufferId, GLsizeiptr size, void * data)
{
    android::AutoMutex _lock(m_lock);
    m_buffers.replaceValueFor(bufferId, new BufferData(size, data));
}

GLenum GLSharedGroup::subUpdateBufferData(GLuint bufferId, GLintptr offset, GLsizeiptr size, void * data)
{
    android::AutoMutex _lock(m_lock);
    BufferData * buf = m_buffers.valueFor(bufferId);
    if ((!buf) || (buf->m_size < offset+size) || (offset < 0) || (size<0)) return GL_INVALID_VALUE; 

    //it's safe to update now
    memcpy((char*)buf->m_fixedBuffer.ptr() + offset, data, size);
    return GL_NO_ERROR; 
}

void GLSharedGroup::deleteBufferData(GLuint bufferId)
{
    android::AutoMutex _lock(m_lock);
    m_buffers.removeItem(bufferId);
}
