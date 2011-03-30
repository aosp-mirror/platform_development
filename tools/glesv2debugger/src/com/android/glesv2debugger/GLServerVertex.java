/*
 ** Copyright 2011, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.android.glesv2debugger;

import com.android.glesv2debugger.DebuggerMessage.Message;

import java.nio.ByteBuffer;
import java.util.HashMap;

class GLBuffer {
    public GLEnum usage;
    public GLEnum target;
    public ByteBuffer data;
}

class GLAttribPointer {
    public int size; // number of values per vertex
    public GLEnum type; // data type
    public int stride; // bytes
    public int ptr; // pointer in debugger server or byte offset into buffer
    public GLBuffer buffer;
    public boolean normalized;
    public boolean enabled;
}

public class GLServerVertex {

    public  HashMap<Integer, GLBuffer> buffers;
    public GLBuffer attribBuffer, indexBuffer; // current binding
    public GLAttribPointer attribPointers[];
    public float defaultAttribs[][];
    int maxAttrib;

    public GLServerVertex() {
        buffers = new HashMap<Integer, GLBuffer>();
        buffers.put(0, null);
        attribPointers = new GLAttribPointer[16];
        for (int i = 0; i < attribPointers.length; i++)
            attribPointers[i] = new GLAttribPointer();
        defaultAttribs = new float[16][4];
        for (int i = 0; i < defaultAttribs.length; i++) {
            defaultAttribs[i][0] = 0;
            defaultAttribs[i][1] = 0;
            defaultAttribs[i][2] = 0;
            defaultAttribs[i][3] = 1;
        }
    }

    Message processed = null; // return; glDrawArrays/Elements with fetched data

    // returns instance if processed TODO: return new instance if changed
    public GLServerVertex Process(final Message msg) {
        processed = null;
        switch (msg.getFunction()) {
            case glBindBuffer:
                glBindBuffer(msg);
                return this;
            case glBufferData:
                glBufferData(msg);
                return this;
            case glBufferSubData:
                glBufferSubData(msg);
                return this;
            case glDeleteBuffers:
                glDeleteBuffers(msg);
                return this;
            case glDrawArrays:
                if (msg.hasArg7())
                    processed = glDrawArrays(msg);
                return this;
            case glDrawElements:
                if (msg.hasArg7())
                    processed = glDrawElements(msg);
                return this;
            case glDisableVertexAttribArray:
                glDisableVertexAttribArray(msg);
                return this;
            case glEnableVertexAttribArray:
                glEnableVertexAttribArray(msg);
                return this;
            case glGenBuffers:
                glGenBuffers(msg);
                return this;
            case glVertexAttribPointer:
                glVertexAttribPointer(msg);
                return this;
            case glVertexAttrib1f:
                glVertexAttrib1f(msg);
                return this;
            case glVertexAttrib1fv:
                glVertexAttrib1fv(msg);
                return this;
            case glVertexAttrib2f:
                glVertexAttrib2f(msg);
                return this;
            case glVertexAttrib2fv:
                glVertexAttrib2fv(msg);
                return this;
            case glVertexAttrib3f:
                glVertexAttrib3f(msg);
                return this;
            case glVertexAttrib3fv:
                glVertexAttrib3fv(msg);
                return this;
            case glVertexAttrib4f:
                glVertexAttrib4f(msg);
                return this;
            case glVertexAttrib4fv:
                glVertexAttrib4fv(msg);
                return this;
            default:
                return null;
        }
    }

    // void API_ENTRY(glBindBuffer)(GLenum target, GLuint buffer)
    public void glBindBuffer(Message msg) {
        if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ARRAY_BUFFER) {
            attribBuffer = buffers.get(msg.getArg1());
            if (null != attribBuffer)
                attribBuffer.target = GLEnum.GL_ARRAY_BUFFER;
        } else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ELEMENT_ARRAY_BUFFER) {
            indexBuffer = buffers.get(msg.getArg1());
            if (null != indexBuffer)
                indexBuffer.target = GLEnum.GL_ELEMENT_ARRAY_BUFFER;
        } else
            assert false;
    }

    // void API_ENTRY(glBufferData)(GLenum target, GLsizeiptr size, const
    // GLvoid:size:in data, GLenum usage)
    public void glBufferData(Message msg) {
        if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ARRAY_BUFFER) {
            attribBuffer.usage = GLEnum.valueOf(msg.getArg3());
            attribBuffer.data = msg.getData().asReadOnlyByteBuffer();
        } else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ELEMENT_ARRAY_BUFFER) {
            indexBuffer.usage = GLEnum.valueOf(msg.getArg3());
            indexBuffer.data = msg.getData().asReadOnlyByteBuffer();
        } else
            assert false;
    }

    // void API_ENTRY(glBufferSubData)(GLenum target, GLintptr offset,
    // GLsizeiptr size, const GLvoid:size:in data)
    public void glBufferSubData(Message msg) {
        if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ARRAY_BUFFER) {
            if (attribBuffer.data.isReadOnly()) {
                ByteBuffer buffer = ByteBuffer.allocate(attribBuffer.data.capacity());
                buffer.put(attribBuffer.data);
                attribBuffer.data = buffer;
            }
            attribBuffer.data.position(msg.getArg1());
            attribBuffer.data.put(msg.getData().asReadOnlyByteBuffer());
        } else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ELEMENT_ARRAY_BUFFER) {
            if (indexBuffer.data.isReadOnly()) {
                ByteBuffer buffer = ByteBuffer.allocate(indexBuffer.data.capacity());
                buffer.put(indexBuffer.data);
                indexBuffer.data = buffer;
            }
            indexBuffer.data.position(msg.getArg1());
            indexBuffer.data.put(msg.getData().asReadOnlyByteBuffer());
        } else
            assert false;
    }

    // void glDeleteBuffers(GLsizei n, const GLuint* buffers)
    public void glDeleteBuffers(Message msg) {
        final int n = msg.getArg0();
        final ByteBuffer names = msg.getData().asReadOnlyByteBuffer();
        for (int i = 0; i < n; i++) {
            int name = Integer.reverseBytes(names.getInt());
            GLBuffer buffer = buffers.get(name);
            for (int j = 0; j < attribPointers.length; j++)
                if (attribPointers[j].buffer == buffer) {
                    attribPointers[j].buffer = null;
                    attribPointers[j].enabled = false;
                }
            if (attribBuffer == buffer)
                attribBuffer = null;
            if (indexBuffer == buffer)
                indexBuffer = null;
            buffers.remove(name);
        }
    }

    // void glDisableVertexAttribArray(GLuint index)
    public void glDisableVertexAttribArray(Message msg) {
        attribPointers[msg.getArg0()].enabled = false;
    }

    float FetchConvert(final ByteBuffer src, final GLEnum type, final boolean normalized) {
        if (GLEnum.GL_FLOAT == type)
            return Float.intBitsToFloat(Integer.reverseBytes(src.getInt()));
        else if (GLEnum.GL_UNSIGNED_INT == type)
            if (normalized)
                return (Integer.reverseBytes(src.getInt()) & 0xffffffffL) / (2e32f - 1);
            else
                return Integer.reverseBytes(src.getInt()) & 0xffffffffL;
        else if (GLEnum.GL_INT == type)
            if (normalized)
                return (Integer.reverseBytes(src.getInt()) * 2 + 1) / (2e32f - 1);
            else
                return Integer.reverseBytes(src.getInt());
        else if (GLEnum.GL_UNSIGNED_SHORT == type)
            if (normalized)
                return (Short.reverseBytes(src.getShort()) & 0xffff) / (2e16f - 1);
            else
                return Short.reverseBytes(src.getShort()) & 0xffff;
        else if (GLEnum.GL_SHORT == type)
            if (normalized)
                return (Short.reverseBytes(src.getShort()) * 2 + 1) / (2e16f - 1);
            else
                return Short.reverseBytes(src.getShort());
        else if (GLEnum.GL_UNSIGNED_BYTE == type)
            if (normalized)
                return (src.get() & 0xff) / (2e8f - 1);
            else
                return src.get() & 0xff;
        else if (GLEnum.GL_BYTE == type)
            if (normalized)
                return (src.get() * 2 + 1) / (2e8f - 1);
            else
                return src.get();
        else if (GLEnum.GL_FIXED == type)
            if (normalized)
                return (Integer.reverseBytes(src.getInt()) * 2 + 1) / (2e32f - 1);
            else
                return Integer.reverseBytes(src.getInt()) / (2e16f);
        else
            assert false;
        return 0;
    }

    void Fetch(int index, final ByteBuffer nonVBO, final ByteBuffer dst) {
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer attrib = attribPointers[i];
            int size = 0;
            if (attrib.enabled)
                size = attrib.size;
            if (null != attrib.buffer) {
                final ByteBuffer src = attrib.buffer.data;
                src.position(attrib.ptr + i * attrib.stride);
                dst.putFloat(FetchConvert(src, attrib.type, attrib.normalized));
            } else
                for (int j = 0; j < size; j++)
                    dst.putFloat(FetchConvert(nonVBO, attrib.type, attrib.normalized));
            if (size < 1)
                dst.putFloat(defaultAttribs[i][0]);
            if (size < 2)
                dst.putFloat(defaultAttribs[i][1]);
            if (size < 3)
                dst.putFloat(defaultAttribs[i][2]);
            if (size < 4)
                dst.putFloat(defaultAttribs[i][3]);
        }
    }

    // void glDrawArrays(GLenum mode, GLint first, GLsizei count)
    public Message glDrawArrays(Message msg) {
        maxAttrib = msg.getArg7();
        final int first = msg.getArg1(), count = msg.getArg2();
        final ByteBuffer buffer = ByteBuffer.allocate(4 * 4 * maxAttrib * count);
        ByteBuffer arrays = null;
        if (msg.hasData()) // server sends user pointer attribs
            arrays = msg.getData().asReadOnlyByteBuffer();
        for (int i = first; i < first + count; i++)
            Fetch(i, arrays, buffer);
        assert null == arrays || arrays.remaining() == 0;
        buffer.rewind();
        return msg.toBuilder().setData(com.google.protobuf.ByteString.copyFrom(buffer))
                .setArg8(GLEnum.GL_FLOAT.value).build();
    }

    // void glDrawElements(GLenum mode, GLsizei count, GLenum type, const
    // GLvoid* indices)
    public Message glDrawElements(Message msg) {
        maxAttrib = msg.getArg7();
        final int count = msg.getArg1();
        final GLEnum type = GLEnum.valueOf(msg.getArg2());
        final ByteBuffer buffer = ByteBuffer.allocate(4 * 4 * maxAttrib * count);
        ByteBuffer arrays = null, index = null;
        if (msg.hasData()) // server sends user pointer attribs
            arrays = msg.getData().asReadOnlyByteBuffer();
        if (null == indexBuffer)
            index = arrays; // server also interleaves user pointer indices
        else {
            index = indexBuffer.data;
            index.position(msg.getArg3());
        }
        if (GLEnum.GL_UNSIGNED_SHORT == type)
            for (int i = 0; i < count; i++)
                Fetch(Short.reverseBytes(index.getShort()) & 0xffff, arrays, buffer);
        else if (GLEnum.GL_UNSIGNED_BYTE == type)
            for (int i = 0; i < count; i++)
                Fetch(index.get() & 0xff, arrays, buffer);
        else
            assert false;
        assert null == arrays || arrays.remaining() == 0;
        buffer.rewind();
        return msg.toBuilder().setData(com.google.protobuf.ByteString.copyFrom(buffer))
                .setArg8(GLEnum.GL_FLOAT.value).build();
    }

    // void glEnableVertexAttribArray(GLuint index)
    public void glEnableVertexAttribArray(Message msg) {
        attribPointers[msg.getArg0()].enabled = true;
    }

    // void API_ENTRY(glGenBuffers)(GLsizei n, GLuint:n:out buffers)
    public void glGenBuffers(Message msg) {
        final int n = msg.getArg0();
        final ByteBuffer buffer = msg.getData().asReadOnlyByteBuffer();
        for (int i = 0; i < n; i++) {
            int name = Integer.reverseBytes(buffer.getInt());
            if (!buffers.containsKey(name))
                buffers.put(name, new GLBuffer());
        }
    }

    // void glVertexAttribPointer(GLuint index, GLint size, GLenum type,
    // GLboolean normalized, GLsizei stride, const GLvoid* ptr)
    public void glVertexAttribPointer(Message msg) {
        GLAttribPointer attrib = attribPointers[msg.getArg0()];
        attrib.size = msg.getArg1();
        attrib.type = GLEnum.valueOf(msg.getArg2());
        attrib.normalized = msg.getArg3() != 0;
        attrib.stride = msg.getArg4();
        if (0 == attrib.stride)
            attrib.stride = attrib.size * 4;
        attrib.ptr = msg.getArg5();
        attrib.buffer = attribBuffer;
    }

    // void glVertexAttrib1f(GLuint indx, GLfloat x)
    public void glVertexAttrib1f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(Integer.reverseBytes(msg.getArg1())),
                0, 0, 1);
    }

    // void glVertexAttrib1fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib1fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                0, 0, 1);
    }

    // void glVertexAttrib2f(GLuint indx, GLfloat x, GLfloat y)
    public void glVertexAttrib2f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(Integer.reverseBytes(msg.getArg1())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg2())), 0, 1);
    }

    // void glVertexAttrib2fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib2fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())), 0, 1);
    }

    // void glVertexAttrib3f(GLuint indx, GLfloat x, GLfloat y, GLfloat z)
    public void glVertexAttrib3f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(Integer.reverseBytes(msg.getArg1())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg2())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg3())), 1);
    }

    // void glVertexAttrib3fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib3fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())), 1);
    }

    public void glVertexAttrib4f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(Integer.reverseBytes(msg.getArg1())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg2())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg3())),
                Float.intBitsToFloat(Integer.reverseBytes(msg.getArg4())));
    }

    void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        defaultAttribs[indx][0] = x;
        defaultAttribs[indx][1] = y;
        defaultAttribs[indx][2] = z;
        defaultAttribs[indx][3] = w;
    }

    // void glVertexAttrib4fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib4fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())),
                Float.intBitsToFloat(Integer.reverseBytes(values.getInt())));
    }
}
