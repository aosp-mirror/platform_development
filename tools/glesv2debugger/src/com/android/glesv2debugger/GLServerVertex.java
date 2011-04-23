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
import com.android.sdklib.util.SparseArray;

import java.nio.ByteBuffer;

class GLBuffer implements Cloneable {
    public final int name;
    public GLEnum usage;
    public GLEnum target;
    /** in SampleView.targetByteOrder */
    public ByteBuffer data;

    public GLBuffer(final int name) {
        this.name = name;
    }

    /** deep copy */
    @Override
    public GLBuffer clone() {
        try {
            GLBuffer copy = (GLBuffer) super.clone();
            if (data != null) {
                copy.data = ByteBuffer.allocate(data.capacity());
                copy.data.order(SampleView.targetByteOrder);
                data.position(0);
                copy.data.put(data);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }
}

class GLAttribPointer implements Cloneable {
    public int size; // number of values per vertex
    public GLEnum type; // data type
    public int stride; // bytes
    /**
     * element stride in bytes, used when fetching from buffer; not for fetching
     * from user pointer since server already packed elements
     */
    int elemStride; // in bytes
    /** element size in bytes */
    int elemSize;
    public int ptr; // pointer in debugger server or byte offset into buffer
    public GLBuffer buffer;
    public boolean normalized;
    public boolean enabled;

    /** deep copy, re-maps buffer into copyBuffers */
    public GLAttribPointer clone(SparseArray<GLBuffer> copyBuffers) {
        try {
            GLAttribPointer copy = (GLAttribPointer) super.clone();
            if (buffer != null)
                copy.buffer = copyBuffers.get(buffer.name);
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }
}

public class GLServerVertex implements Cloneable {
    public SparseArray<GLBuffer> buffers = new SparseArray<GLBuffer>();
    public GLBuffer attribBuffer, indexBuffer; // current binding
    public GLAttribPointer attribPointers[];
    public float defaultAttribs[][];

    public GLServerVertex(final int MAX_VERTEX_ATTRIBS) {
        buffers.append(0, null);
        attribPointers = new GLAttribPointer[MAX_VERTEX_ATTRIBS];
        for (int i = 0; i < attribPointers.length; i++)
            attribPointers[i] = new GLAttribPointer();
        defaultAttribs = new float[MAX_VERTEX_ATTRIBS][4];
        for (int i = 0; i < defaultAttribs.length; i++) {
            defaultAttribs[i][0] = 0;
            defaultAttribs[i][1] = 0;
            defaultAttribs[i][2] = 0;
            defaultAttribs[i][3] = 1;
        }
    }

    /** deep copy */
    @Override
    public GLServerVertex clone() {
        try {
            GLServerVertex copy = (GLServerVertex) super.clone();

            copy.buffers = new SparseArray<GLBuffer>(buffers.size());
            for (int i = 0; i < buffers.size(); i++)
                if (buffers.valueAt(i) != null)
                    copy.buffers.append(buffers.keyAt(i), buffers.valueAt(i).clone());
                else
                    copy.buffers.append(buffers.keyAt(i), null);

            if (attribBuffer != null)
                copy.attribBuffer = copy.buffers.get(attribBuffer.name);
            if (indexBuffer != null)
                copy.indexBuffer = copy.buffers.get(indexBuffer.name);

            copy.attribPointers = new GLAttribPointer[attribPointers.length];
            for (int i = 0; i < attribPointers.length; i++)
                copy.attribPointers[i] = attribPointers[i].clone(copy.buffers);

            copy.defaultAttribs = defaultAttribs.clone();

            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    /** returns true if processed */
    public boolean process(final Message msg) {
        switch (msg.getFunction()) {
            case glBindBuffer:
                glBindBuffer(msg);
                return true;
            case glBufferData:
                glBufferData(msg);
                return true;
            case glBufferSubData:
                glBufferSubData(msg);
                return true;
            case glDeleteBuffers:
                glDeleteBuffers(msg);
                return true;
            case glDrawArrays:
            case glDrawElements:
                return true;
            case glDisableVertexAttribArray:
                glDisableVertexAttribArray(msg);
                return true;
            case glEnableVertexAttribArray:
                glEnableVertexAttribArray(msg);
                return true;
            case glGenBuffers:
                glGenBuffers(msg);
                return true;
            case glVertexAttribPointer:
                glVertexAttribPointer(msg);
                return true;
            case glVertexAttrib1f:
                glVertexAttrib1f(msg);
                return true;
            case glVertexAttrib1fv:
                glVertexAttrib1fv(msg);
                return true;
            case glVertexAttrib2f:
                glVertexAttrib2f(msg);
                return true;
            case glVertexAttrib2fv:
                glVertexAttrib2fv(msg);
                return true;
            case glVertexAttrib3f:
                glVertexAttrib3f(msg);
                return true;
            case glVertexAttrib3fv:
                glVertexAttrib3fv(msg);
                return true;
            case glVertexAttrib4f:
                glVertexAttrib4f(msg);
                return true;
            case glVertexAttrib4fv:
                glVertexAttrib4fv(msg);
                return true;
            default:
                return false;
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
            attribBuffer.data.order(SampleView.targetByteOrder);
        } else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ELEMENT_ARRAY_BUFFER) {
            indexBuffer.usage = GLEnum.valueOf(msg.getArg3());
            indexBuffer.data = msg.getData().asReadOnlyByteBuffer();
            indexBuffer.data.order(SampleView.targetByteOrder);
        } else
            assert false;
    }

    // void API_ENTRY(glBufferSubData)(GLenum target, GLintptr offset,
    // GLsizeiptr size, const GLvoid:size:in data)
    public void glBufferSubData(Message msg) {
        if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ARRAY_BUFFER) {
            if (attribBuffer.data.isReadOnly()) {
                ByteBuffer buffer = ByteBuffer.allocate(attribBuffer.data.capacity());
                buffer.order(SampleView.targetByteOrder);
                buffer.put(attribBuffer.data);
                attribBuffer.data = buffer;
            }
            attribBuffer.data.position(msg.getArg1());
            attribBuffer.data.put(msg.getData().asReadOnlyByteBuffer());
        } else if (GLEnum.valueOf(msg.getArg0()) == GLEnum.GL_ELEMENT_ARRAY_BUFFER) {
            if (indexBuffer.data.isReadOnly()) {
                ByteBuffer buffer = ByteBuffer.allocate(indexBuffer.data.capacity());
                buffer.order(SampleView.targetByteOrder);
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
        names.order(SampleView.targetByteOrder);
        for (int i = 0; i < n; i++) {
            final int name = names.getInt();
            final GLBuffer buffer = buffers.get(name);
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
        if (msg.getArg0() >= 0 && msg.getArg0() < attribPointers.length)
            attribPointers[msg.getArg0()].enabled = false;
    }

    float fetchConvert(final ByteBuffer src, final GLEnum type, final boolean normalized) {
        if (GLEnum.GL_FLOAT == type)
            return Float.intBitsToFloat(src.getInt());
        else if (GLEnum.GL_UNSIGNED_INT == type)
            if (normalized)
                return (src.getInt() & 0xffffffffL) / (2e32f - 1);
            else
                return src.getInt() & 0xffffffffL;
        else if (GLEnum.GL_INT == type)
            if (normalized)
                return (src.getInt() * 2 + 1) / (2e32f - 1);
            else
                return src.getInt();
        else if (GLEnum.GL_UNSIGNED_SHORT == type)
            if (normalized)
                return (src.getShort() & 0xffff) / (2e16f - 1);
            else
                return src.getShort() & 0xffff;
        else if (GLEnum.GL_SHORT == type)
            if (normalized)
                return (src.getShort() * 2 + 1) / (2e16f - 1);
            else
                return src.getShort();
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
                return (src.getInt() * 2 + 1) / (2e32f - 1);
            else
                return src.getInt() / (2e16f);
        else
            assert false;
        return 0;
    }

    static int typeSize(final GLEnum type) {
        switch (type) {
            case GL_FLOAT:
            case GL_UNSIGNED_INT:
            case GL_INT:
            case GL_FIXED:
                return 4;
            case GL_UNSIGNED_SHORT:
            case GL_SHORT:
                return 2;
            case GL_UNSIGNED_BYTE:
            case GL_BYTE:
                return 1;
            default:
                assert false;
                return 0;
        }
    }

    void fetch(final int maxAttrib, final int index, final int dstIdx, final ByteBuffer nonVBO,
            final float[][] fetchedAttribs) {
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer attrib = attribPointers[i];
            int size = 0;
            if (attrib.enabled) {
                size = attrib.size;
                if (null != attrib.buffer) {
                    final ByteBuffer src = attrib.buffer.data;
                    src.position(attrib.ptr + index * attrib.elemStride);
                    for (int j = 0; j < size; j++)
                        fetchedAttribs[i][dstIdx * 4 + j] = fetchConvert(src, attrib.type,
                                attrib.normalized);
                } else
                    for (int j = 0; j < size; j++)
                        fetchedAttribs[i][dstIdx * 4 + j] = fetchConvert(nonVBO, attrib.type,
                                attrib.normalized);
            }
            if (size < 1)
                fetchedAttribs[i][dstIdx * 4 + 0] = defaultAttribs[i][0];
            if (size < 2)
                fetchedAttribs[i][dstIdx * 4 + 1] = defaultAttribs[i][1];
            if (size < 3)
                fetchedAttribs[i][dstIdx * 4 + 2] = defaultAttribs[i][2];
            if (size < 4)
                fetchedAttribs[i][dstIdx * 4 + 3] = defaultAttribs[i][3];
        }
    }

    /**
     * fetches and converts vertex data from buffers, defaults and user pointers
     * into MessageData; mainly for display use
     */
    public void glDrawArrays(MessageData msgData) {
        final Message msg = msgData.msg;
        if (!msg.hasArg7())
            return;
        final int maxAttrib = msg.getArg7();
        final int first = msg.getArg1(), count = msg.getArg2();
        msgData.attribs = new float[maxAttrib][count * 4];
        ByteBuffer arrays = null;
        if (msg.hasData()) // server sends user pointer attribs
        {
            arrays = msg.getData().asReadOnlyByteBuffer();
            arrays.order(SampleView.targetByteOrder);
        }
        for (int i = 0; i < count; i++)
            fetch(maxAttrib, first + i, i, arrays, msgData.attribs);
        assert null == arrays || arrays.remaining() == 0;
    }

    // void glDrawElements(GLenum mode, GLsizei count, GLenum type, const
    // GLvoid* indices)
    /**
     * fetches and converts vertex data from buffers, defaults and user pointers
     * and indices from buffer/pointer into MessageData; mainly for display use
     */
    public void glDrawElements(MessageData msgData) {
        final Message msg = msgData.msg;
        if (!msg.hasArg7())
            return;
        final int maxAttrib = msg.getArg7();
        final int count = msg.getArg1();
        final GLEnum type = GLEnum.valueOf(msg.getArg2());
        msgData.attribs = new float[maxAttrib][count * 4];
        msgData.indices = new short[count];
        ByteBuffer arrays = null, index = null;
        if (msg.hasData()) // server sends user pointer attribs
        {
            arrays = msg.getData().asReadOnlyByteBuffer();
            arrays.order(SampleView.targetByteOrder);
        }
        if (null == indexBuffer)
            index = arrays; // server also interleaves user pointer indices
        else {
            index = indexBuffer.data;
            index.position(msg.getArg3());
        }
        if (GLEnum.GL_UNSIGNED_SHORT == type) {
            for (int i = 0; i < count; i++) {
                msgData.indices[i] = index.getShort();
                fetch(maxAttrib, msgData.indices[i] & 0xffff, i, arrays, msgData.attribs);
            }
        } else if (GLEnum.GL_UNSIGNED_BYTE == type) {
            for (int i = 0; i < count; i++) {
                msgData.indices[i] = (short) (index.get() & 0xff);
                fetch(maxAttrib, msgData.indices[i], i, arrays, msgData.attribs);
            }
        } else
            assert false;
        assert null == arrays || arrays.remaining() == 0;
    }

    // void glEnableVertexAttribArray(GLuint index)
    public void glEnableVertexAttribArray(Message msg) {
        if (msg.getArg0() >= 0 && msg.getArg0() < attribPointers.length)
            attribPointers[msg.getArg0()].enabled = true;
    }

    // void API_ENTRY(glGenBuffers)(GLsizei n, GLuint:n:out buffers)
    public void glGenBuffers(Message msg) {
        final int n = msg.getArg0();
        final ByteBuffer buffer = msg.getData().asReadOnlyByteBuffer();
        buffer.order(SampleView.targetByteOrder);
        for (int i = 0; i < n; i++) {
            final int name = buffer.getInt();
            final int index = buffers.indexOfKey(name);
            if (index < 0)
                buffers.append(name, new GLBuffer(name));
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
        attrib.elemSize = attrib.size * typeSize(attrib.type);
        if (attrib.stride == 0)
            attrib.elemStride = attrib.elemSize;
        else
            attrib.elemStride = attrib.stride;
        attrib.ptr = msg.getArg5();
        attrib.buffer = attribBuffer;
    }

    // void glVertexAttrib1f(GLuint indx, GLfloat x)
    public void glVertexAttrib1f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(msg.getArg1()),
                0, 0, 1);
    }

    // void glVertexAttrib1fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib1fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        values.order(SampleView.targetByteOrder);
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(values.getInt()),
                0, 0, 1);
    }

    // void glVertexAttrib2f(GLuint indx, GLfloat x, GLfloat y)
    public void glVertexAttrib2f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(msg.getArg1()),
                Float.intBitsToFloat(msg.getArg2()), 0, 1);
    }

    // void glVertexAttrib2fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib2fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        values.order(SampleView.targetByteOrder);
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()), 0, 1);
    }

    // void glVertexAttrib3f(GLuint indx, GLfloat x, GLfloat y, GLfloat z)
    public void glVertexAttrib3f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(msg.getArg1()),
                Float.intBitsToFloat(msg.getArg2()),
                Float.intBitsToFloat(msg.getArg3()), 1);
    }

    // void glVertexAttrib3fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib3fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        values.order(SampleView.targetByteOrder);
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()), 1);
    }

    public void glVertexAttrib4f(Message msg) {
        glVertexAttrib4f(msg.getArg0(), Float.intBitsToFloat(msg.getArg1()),
                Float.intBitsToFloat(msg.getArg2()),
                Float.intBitsToFloat(msg.getArg3()),
                Float.intBitsToFloat(msg.getArg4()));
    }

    void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        if (indx < 0 || indx >= defaultAttribs.length)
            return;
        defaultAttribs[indx][0] = x;
        defaultAttribs[indx][1] = y;
        defaultAttribs[indx][2] = z;
        defaultAttribs[indx][3] = w;
    }

    // void glVertexAttrib4fv(GLuint indx, const GLfloat* values)
    public void glVertexAttrib4fv(Message msg) {
        final ByteBuffer values = msg.getData().asReadOnlyByteBuffer();
        values.order(SampleView.targetByteOrder);
        glVertexAttrib4f(msg.getArg0(),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()),
                Float.intBitsToFloat(values.getInt()));
    }
}
