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
import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.sdklib.util.SparseIntArray;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public class CodeGen implements IRunnableWithProgress {
    private FileWriter codeFile, makeFile, namesHeaderFile, namesSourceFile;
    private PrintWriter code, make, namesHeader, namesSource;
    private FileOutputStream dataOut;
    private SparseIntArray bufferNames,
            framebufferNames, programNames, textureNames, shaderNames, renderbufferNames;

    /** return true if msg was a texture upload */
    private boolean codeGenTextureUpload(final Message msg, final boolean replaceCopy) {
        String s = null;
        switch (msg.getFunction()) {
            case glCompressedTexImage2D:
                s = MessageFormatter.format(msg, true).replace("arg7", "texData");
                break;
            case glCompressedTexSubImage2D:
            case glTexImage2D:
            case glTexSubImage2D:
                s = MessageFormatter.format(msg, true).replace("arg8", "texData");
                break;
            case glCopyTexImage2D:
                if (!replaceCopy) {
                    code.write(MessageFormatter.format(msg, true));
                    code.write(";CHKERR;\n");
                    return true;
                }
                assert msg.getArg2() == msg.getPixelFormat(); // TODO
                s = "//" + MessageFormatter.format(msg, true) + "\n";
                s += String.format("glTexImage2D(%s, %d, %s, %d, %d, %d, %s, %s, texData);CHKERR;",
                        GLEnum.valueOf(msg.getArg0()), msg.getArg1(),
                        GLEnum.valueOf(msg.getArg2()), msg.getArg5(), msg.getArg6(),
                        msg.getArg7(), GLEnum.valueOf(msg.getPixelFormat()),
                        GLEnum.valueOf(msg.getPixelType()));
                break;
            case glCopyTexSubImage2D:
                if (!replaceCopy) {
                    code.write(MessageFormatter.format(msg, true));
                    code.write(";CHKERR;\n");
                    return true;
                }
                // FIXME: check the texture format & type, and convert
                s = "//" + MessageFormatter.format(msg, true) + "\n";
                s += String.format(
                        "glTexSubImage2D(%s, %d, %d, %d, %d, %d, %s, %s, texData);CHKERR;",
                        GLEnum.valueOf(msg.getArg0()), msg.getArg1(), msg.getArg2(),
                        msg.getArg3(), msg.getArg6(), msg.getArg7(),
                        GLEnum.valueOf(msg.getPixelFormat()), GLEnum.valueOf(msg.getPixelType()));
                break;
            default:
                return false;
        }

        if (msg.hasData()) {
            final byte[] data = MessageProcessor.lzfDecompressChunks(msg.getData());
            try {
                code.write("{\n");
                code.format("    void * texData = malloc(%d);CHKERR;\n", data.length);
                code.format("    FILE * texFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");CHKERR;\n");
                code.format("    assert(texFile);CHKERR;\n");
                code.format("    fseek(texFile, %d, SEEK_SET);CHKERR;\n", dataOut.getChannel()
                        .position());
                dataOut.write(data);
                code.format("    fread(texData, %d, 1, texFile);CHKERR;\n", data.length);
                code.format("    fclose(texFile);CHKERR;\n");
                code.format("    " + s + ";\n");
                code.format("    free(texData);CHKERR;\n");
                code.format("}\n");
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }
        } else
            code.write(s.replace("texData", "NULL") + ";\n");
        return true;
    }

    private void codeGenServerState(final GLServerState serverState) {
        code.write("// CodeGenServerState\n");
        for (int i = 0; i < serverState.enableDisables.size(); i++) {
            final GLEnum key = GLEnum.valueOf(serverState.enableDisables.keyAt(i));
            if (serverState.enableDisables.valueAt(i) == 0)
                code.format("glDisable(%s);CHKERR;\n", key);
            else
                code.format("glEnable(%s);CHKERR;\n", key);
        }
        for (int i = 0; i < serverState.lastSetter.size(); i++) {
            final Function key = Function.valueOf(serverState.lastSetter.keyAt(i));
            final Message msg = serverState.lastSetter.valueAt(i);
            if (msg == null) {
                code.format("// %s is default\n", key);
                continue;
            }
            final String s = MessageFormatter.format(msg, true);
            code.write(s);
            code.write(";\n");
        }
        // TODO: stencil and integers
    }

    private void codeGenServerShader(final GLServerShader serverShader) {
        code.write("// CodeGenServerShader\n");
        for (int i = 0; i < serverShader.shaders.size(); i++) {
            final int name = serverShader.shaders.keyAt(i);
            final GLShader shader = serverShader.shaders.valueAt(i);
            final String id = "shader_" + name;
            if (shaderNames.indexOfKey(name) < 0) {
                namesSource.format("GLuint %s = 0;\n", id);
                namesHeader.format("extern GLuint %s;\n", id);
            }
            code.format("%s = glCreateShader(%s);CHKERR;\n", id, shader.type);
            shaderNames.put(name, name);

            if (shader.source != null) {
                final String src = shader.source.replace("\r", "").replace("\n", "\\n\\\n")
                        .replace("\"", "\\\"");
                code.format("glShaderSource(%s, 1, (const GLchar *[]){\"%s\"}, NULL);CHKERR;\n",
                                id, src);
                code.format("glCompileShader(%s);CHKERR;\n", id);
            }
        }

        for (int i = 0; i < serverShader.programs.size(); i++) {
            final int name = serverShader.programs.keyAt(i);
            final GLProgram program = serverShader.programs.valueAt(i);
            final String id = "program_" + name;
            if (programNames.indexOfKey(name) < 0) {
                namesSource.format("GLuint %s = 0;\n", id);
                namesHeader.format("extern GLuint %s;\n", id);
            }
            code.format("%s = glCreateProgram();CHKERR;\n", id);
            programNames.put(name, name);
            code.format("glAttachShader(%s, shader_%d);CHKERR;\n", id,
                    program.vert);
            code.format("glAttachShader(%s, shader_%d);CHKERR;\n", id,
                    program.frag);
            code.format("glLinkProgram(%s);CHKERR;\n", id);
            if (serverShader.current == program)
                code.format("glUseProgram(%s);CHKERR;\n", id);
        }
    }

    private void codeGenServerTexture(final GLServerTexture serverTexture, final boolean replaceCopy) {
        code.write("// CodeGenServerTexture\n");
        for (int i = 0; i < serverTexture.textures.size(); i++) {
            final int name = serverTexture.textures.keyAt(i);
            final GLTexture tex = serverTexture.textures.valueAt(i);
            final String id = "texture_" + name;
            if (textureNames.indexOfKey(name) < 0) {
                namesHeader.format("extern GLuint %s;\n", id);
                namesSource.format("GLuint %s = 0;\n", id);
            }
            code.format("%s = 0;\n", id);
            textureNames.put(name, name);

            if (name == 0)
                continue;
            code.format("glGenTextures(1, &%s);CHKERR;\n", id);
            String s = String.format("glBindTexture(%s, texture_%d);CHKERR;\n", tex.target,
                    tex.name);
            code.write(s);
            for (final Message msg : tex.contentChanges) {
                if (codeGenTextureUpload(msg, replaceCopy))
                    continue;
                switch (msg.getFunction()) {
                    case glGenerateMipmap:
                        s = MessageFormatter.format(msg, true);
                        break;
                    default:
                        assert false;
                }
                code.write(s + ";\n");
            }
            code.format("glTexParameteriv(%s, GL_TEXTURE_WRAP_S, (GLint[]){%s});CHKERR;\n",
                    tex.target, tex.wrapS);
            code.format("glTexParameteriv(%s, GL_TEXTURE_WRAP_T, (GLint[]){%s});CHKERR;\n",
                    tex.target, tex.wrapT);
            code.format("glTexParameteriv(%s, GL_TEXTURE_MIN_FILTER, (GLint[]){%s});CHKERR;\n",
                    tex.target, tex.min);
            code.format("glTexParameteriv(%s, GL_TEXTURE_MAG_FILTER, (GLint[]){%s});CHKERR;\n",
                    tex.target, tex.mag);
        }
        for (int i = 0; i < serverTexture.tmu2D.length; i++) {
            code.format("glActiveTexture(%s);CHKERR;\n",
                    GLEnum.valueOf(GLEnum.GL_TEXTURE0.value + i));
            code.format("glBindTexture(GL_TEXTURE_2D, texture_%d);CHKERR;\n",
                    serverTexture.tmu2D[i]);
        }
        for (int i = 0; i < serverTexture.tmuCube.length; i++) {
            code.format("glActiveTexture(%s);CHKERR;\n",
                    GLEnum.valueOf(GLEnum.GL_TEXTURE0.value + i));
            code.format("glBindTexture(GL_TEXTURE_CUBE_MAP, texture_%d);CHKERR;\n",
                    serverTexture.tmuCube[i]);
        }
        code.format("glActiveTexture(%s);CHKERR;\n", serverTexture.activeTexture);
        if (serverTexture.tex2D == null)
            code.format("glBindTexture(GL_TEXTURE_2D, 0);CHKERR;\n");
        else
            code.format("glBindTexture(GL_TEXTURE_2D, texture_%d);CHKERR;\n",
                    serverTexture.tex2D.name);
        if (serverTexture.texCube == null)
            code.format("glBindTexture(GL_TEXTURE_CUBE_MAP, 0);CHKERR;\n");
        else
            code.format("glBindTexture(GL_TEXTURE_CUBE_MAP, texture_%d);CHKERR;\n",
                    serverTexture.texCube.name);
    }

    private void codeGenBufferData(final ByteBuffer buffer, final String call) {
        ByteBuffer bfr = buffer;
        if (buffer.isReadOnly()) {
            bfr = ByteBuffer.allocate(buffer.capacity());
            bfr.put(buffer);
        }
        final byte[] data = bfr.array();
        try {
            code.write("{\n");
            code.format("    void * bufferData = malloc(%d);\n", data.length);
            code.format("    FILE * bufferFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");\n");
            code.format("    assert(bufferFile);\n");
            code.format("    fseek(bufferFile, %d, SEEK_SET);\n", dataOut.getChannel()
                    .position());
            dataOut.write(data);
            code.format("    fread(bufferData, %d, 1, bufferFile);\n", data.length);
            code.format("    fclose(bufferFile);\n");
            code.format("    " + call + ";CHKERR;\n");
            code.format("    free(bufferData);\n");
            code.format("}\n");
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private void codeGenServerVertex(final GLServerVertex v) {
        code.write("// CodeGenServerVertex\n");
        for (int i = 0; i < v.buffers.size(); i++) {
            final int name = v.buffers.keyAt(i);
            final String id = "buffer_" + name;
            final GLBuffer buffer = v.buffers.valueAt(i);
            if (bufferNames.indexOfKey(name) < 0) {
                namesHeader.format("extern GLuint %s;\n", id);
                namesSource.format("GLuint %s = 0;\n", id);
            }
            code.format("%s = 0;\n", id);
            bufferNames.put(name, name);
            if (name == 0)
                continue;
            code.format("glGenBuffers(1, &%s);CHKERR;\n", id);
            if (buffer.target != null) {
                code.format("glBindBuffer(%s, %s);CHKERR;\n", buffer.target, id);
                if (buffer.data != null) {
                    String s = String.format("glBufferData(%s, %d, bufferData, %s)", buffer.target,
                            buffer.data.capacity(), buffer.usage);
                    codeGenBufferData(buffer.data, s);
                }
            }
        }
        // TODO: use MAX_VERTEX_ATTRIBS
        for (int i = 0; i < v.defaultAttribs.length; i++)
            code.format("glVertexAttrib4f(%d, %f, %f, %f, %f);CHKERR;\n", i,
                    v.defaultAttribs[i][0],
                    v.defaultAttribs[i][1], v.defaultAttribs[i][2], v.defaultAttribs[i][3]);
        for (int i = 0; i < v.attribPointers.length; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            if (att.type == null)
                continue;
            if (att.buffer != null)
                code.format("glBindBuffer(GL_ARRAY_BUFFER, buffer_%d);CHKERR;\n", att.buffer.name);
            else
                code.format("glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
            code.format("glVertexAttribPointer(%d, %d, %s, %b, %d, (const GLvoid *)%d);CHKERR;\n",
                    i, att.size, att.type, att.normalized, att.stride, att.ptr);
        }
        if (v.attribBuffer != null)
            code.format("glBindBuffer(GL_ARRAY_BUFFER, buffer_%d);CHKERR;\n", v.attribBuffer.name);
        else
            code.write("glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
        if (v.indexBuffer != null)
            code.format("glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer_%d);CHKERR;\n",
                    v.indexBuffer.name);
        else
            code.write("glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);CHKERR;\n");
    }

    private void codeGenGenNames(final Message msg) {
        final ByteBuffer names = msg.getData().asReadOnlyByteBuffer();
        names.order(SampleView.targetByteOrder);
        SparseIntArray namesArray = null;
        for (int i = 0; i < msg.getArg0(); i++) {
            String id = "";
            final int name = names.getInt();
            switch (msg.getFunction()) {
                case glGenBuffers:
                    id = "buffer";
                    namesArray = bufferNames;
                    break;
                case glGenFramebuffers:
                    id = "framebuffer";
                    namesArray = framebufferNames;
                    break;
                case glGenRenderbuffers:
                    id = "renderbuffer";
                    namesArray = renderbufferNames;
                    break;
                case glGenTextures:
                    id = "texture";
                    namesArray = textureNames;
                    break;
                default:
                    assert false;
            }
            id += "_" + name;
            if (namesArray.indexOfKey(name) < 0) {
                namesHeader.format("extern GLuint %s;\n", id);
                namesSource.format("GLuint %s = 0;\n", id);
            }
            code.format("%s = 0;\n", id);
            namesArray.put(name, name);
            code.format("%s(1, &%s);CHKERR;\n", msg.getFunction(), id);
        }
    }

    private void codeGenDeleteNames(final Message msg) {
        final ByteBuffer names = msg.getData().asReadOnlyByteBuffer();
        names.order(SampleView.targetByteOrder);
        SparseIntArray namesArray = null;
        for (int i = 0; i < msg.getArg0(); i++) {
            String id = null;
            final int name = names.getInt();
            switch (msg.getFunction()) {
                case glDeleteBuffers:
                    id = "buffer";
                    namesArray = bufferNames;
                    break;
                case glDeleteFramebuffers:
                    id = "framebuffer";
                    namesArray = framebufferNames;
                    break;
                case glDeleteRenderbuffers:
                    id = "renderbuffer";
                    namesArray = renderbufferNames;
                    break;
                case glDeleteTextures:
                    id = "texture";
                    namesArray = textureNames;
                    break;
                default:
                    assert false;
            }
            id += "_" + name;
            code.format("%s = 0;\n", id);
            namesArray.put(name, 0);
            code.format("%s(1, &%s);CHKERR;\n", msg.getFunction(), id);
        }
    }

    private void codeGenBindNames(final Message msg) {
        String id = null;
        SparseIntArray namesArray = null;
        final int name = msg.getArg1();
        switch (msg.getFunction()) {
            case glBindBuffer:
                id = "buffer";
                namesArray = bufferNames;
                break;
            case glBindFramebuffer:
                id = "framebuffer";
                namesArray = framebufferNames;
                break;
            case glBindRenderbuffer:
                id = "renderbuffer";
                namesArray = renderbufferNames;
                break;
            case glBindTexture:
                id = "texture";
                namesArray = textureNames;
                break;
            default:
                assert false;
        }
        id += "_" + name;
        if (namesArray.indexOfKey(name) < 0) {
            namesHeader.format("extern GLuint %s;\n", id);
            namesSource.format("GLuint %s = 0;\n", id);
        } else if (namesArray.get(name) != name)
            code.format("%s = %d;\n", id, name); // name was deleted
        namesArray.put(name, name);
        code.write(MessageFormatter.format(msg, true));
        code.write(";CHKERR;\n");
    }

    private void codeGenDrawArrays(final GLServerVertex v, final MessageData msgData)
            throws IOException {
        final int maxAttrib = msgData.msg.getArg7();
        if (maxAttrib < 1) {
            code.write("// no vertex data\n");
            return;
        }
        final byte[] data = msgData.msg.getData().toByteArray();
        final GLEnum mode = GLEnum.valueOf(msgData.msg.getArg0());
        final int first = msgData.msg.getArg1(), count = msgData.msg.getArg2();
        int attribDataStride = 0;
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            if (!att.enabled)
                continue;
            if (att.buffer != null)
                continue;
            attribDataStride += att.elemSize;
        }
        assert attribDataStride * count == data.length;
        code.write("{\n");
        if (attribDataStride > 0) {
            code.format("    FILE * attribFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");CHKERR;\n");
            code.format("    assert(attribFile);CHKERR;\n");
            code.format("    fseek(attribFile, %d, SEEK_SET);CHKERR;\n", dataOut.getChannel()
                    .position());
            dataOut.write(data);
            code.format("    char * const attribData = (char *)malloc(%d);\n", first
                    * attribDataStride + data.length);
            code.format("    assert(attribData);\n");
            code.format("    fread(attribData + %d, %d, 1, attribFile);\n",
                    first * attribDataStride, data.length);
            code.format("    fclose(attribFile);\n");
            code.format("    glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
            int attribDataOffset = 0;
            for (int i = 0; i < maxAttrib; i++) {
                final GLAttribPointer att = v.attribPointers[i];
                if (!att.enabled)
                    continue;
                if (att.buffer != null)
                    continue;
                code.format(
                        "    glVertexAttribPointer(%d, %d, %s, %b, %d, attribData + %d);CHKERR;\n",
                        i, att.size, att.type, att.normalized,
                        attribDataStride, attribDataOffset);
                attribDataOffset += att.elemSize;
            }
            if (v.attribBuffer != null)
                code.format("    glBindBuffer(GL_ARRAY_BUFFER, %d);CHKERR;\n",
                        v.attribBuffer.name);
        }
        code.format("    glDrawArrays(%s, %d, %d);CHKERR;\n", mode, first, count);
        if (attribDataStride > 0)
            code.format("    free(attribData);CHKERR;\n");
        code.write("};\n");
    }

    private void codeGenDrawElements(final GLServerVertex v, final MessageData msgData)
            throws IOException {
        final int maxAttrib = msgData.msg.getArg7();
        if (maxAttrib < 1) {
            code.write("// no vertex data\n");
            return;
        }
        final GLEnum mode = GLEnum.valueOf(msgData.msg.getArg0());
        final int count = msgData.msg.getArg1();
        final GLEnum type = GLEnum.valueOf(msgData.msg.getArg2());
        String typeName = "GLubyte";
        if (type == GLEnum.GL_UNSIGNED_SHORT)
            typeName = "GLushort";
        int attribDataStride = 0;
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            if (!att.enabled)
                continue;
            if (att.buffer != null)
                continue;
            attribDataStride += att.elemSize;
        }
        code.write("{\n");
        if (v.indexBuffer == null || attribDataStride > 0) {
            // need to load user pointer indices and/or attributes
            final byte[] element = new byte[attribDataStride];
            final ByteBuffer data = msgData.msg.getData().asReadOnlyByteBuffer();
            data.order(SampleView.targetByteOrder);
            final ByteBuffer indexData = ByteBuffer.allocate(count * GLServerVertex.typeSize(type));
            indexData.order(SampleView.targetByteOrder);
            final ByteBuffer attribData = ByteBuffer.allocate(count * attribDataStride);
            attribData.order(SampleView.targetByteOrder);
            int maxIndex = -1;
            ByteBuffer indexSrc = data;
            if (v.indexBuffer != null) {
                indexSrc = v.indexBuffer.data;
                indexSrc.position(msgData.msg.getArg3());
            }
            indexSrc.order(SampleView.targetByteOrder);
            for (int i = 0; i < count; i++) {
                int index = -1;
                if (type == GLEnum.GL_UNSIGNED_BYTE) {
                    byte idx = indexSrc.get();
                    index = idx & 0xff;
                    indexData.put(idx);
                } else if (type == GLEnum.GL_UNSIGNED_SHORT) {
                    short idx = indexSrc.getShort();
                    index = idx & 0xffff;
                    indexData.putShort(idx);
                } else
                    assert false;
                data.get(element);
                attribData.put(element);
                if (index > maxIndex)
                    maxIndex = index;
            }
            code.format("    FILE * attribFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");CHKERR;\n");
            code.format("    assert(attribFile);CHKERR;\n");
            code.format("    fseek(attribFile, 0x%X, SEEK_SET);CHKERR;\n",
                    dataOut.getChannel().position());
            dataOut.write(indexData.array());
            code.format("    %s * const indexData = (%s *)malloc(%d);\n", typeName, typeName,
                    indexData.capacity());
            code.format("    assert(indexData);\n");
            code.format("    fread(indexData, %d, 1, attribFile);\n", indexData.capacity());
            if (attribDataStride > 0) {
                code.format("    glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
                for (int i = 0; i < maxAttrib; i++) {
                    final GLAttribPointer att = v.attribPointers[i];
                    if (!att.enabled)
                        continue;
                    if (att.buffer != null)
                        continue;
                    code.format("    char * const attrib%d = (char *)malloc(%d);\n",
                            i, att.elemSize * (maxIndex + 1));
                    code.format("    assert(attrib%d);\n", i);
                    code.format(
                            "    glVertexAttribPointer(%d, %d, %s, %b, %d, attrib%d);CHKERR;\n",
                            i, att.size, att.type, att.normalized, att.elemSize, i);
                }
                dataOut.write(attribData.array());
                code.format("    for (%s i = 0; i < %d; i++) {\n", typeName, count);
                for (int i = 0; i < maxAttrib; i++) {
                    final GLAttribPointer att = v.attribPointers[i];
                    if (!att.enabled)
                        continue;
                    if (att.buffer != null)
                        continue;
                    code.format(
                            "        fread(attrib%d + indexData[i] * %d, %d, 1, attribFile);\n",
                            i, att.elemSize, att.elemSize);
                }
                code.format("    }\n");
                if (v.attribBuffer != null)
                    code.format("    glBindBuffer(GL_ARRAY_BUFFER, %d);CHKERR;\n",
                            v.attribBuffer.name);
            }
            code.format("    fclose(attribFile);\n");
        }
        if (v.indexBuffer != null)
            code.format("    glDrawElements(%s, %d, %s, (const void *)%d);CHKERR;\n",
                    mode, count, type, msgData.msg.getArg3());
        else {
            code.format("    glDrawElements(%s, %d, %s, indexData);CHKERR;\n",
                    mode, count, type);
            code.format("    free(indexData);\n");
        }
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            if (!att.enabled)
                continue;
            if (att.buffer != null)
                continue;
            code.format("    free(attrib%d);\n", i);
        }
        code.write("};\n");
    }

    private void codeGenDraw(final GLServerVertex v, final MessageData msgData)
            throws IOException {
        final int maxAttrib = msgData.msg.getArg7();
        if (maxAttrib < 1) {
            code.write("// no vertex data\n");
            return;
        }
        final int count = msgData.attribs[0].length / 4;
        final GLEnum mode = GLEnum.valueOf(msgData.msg.getArg0());
        final ByteBuffer attribData = ByteBuffer.allocate(maxAttrib * count * 16);
        attribData.order(SampleView.targetByteOrder);
        for (int i = 0; i < count; i++)
            for (int j = 0; j < maxAttrib; j++)
                for (int k = 0; k < 4; k++)
                    attribData.putFloat(msgData.attribs[j][i * 4 + k]);
        assert attribData.remaining() == 0;
        code.write("{\n");
        code.format("    FILE * attribFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");CHKERR;\n");
        code.format("    assert(attribFile);CHKERR;\n");
        code.format("    fseek(attribFile, 0x%X, SEEK_SET);CHKERR;\n",
                dataOut.getChannel().position());
        dataOut.write(attribData.array());
        code.format("    char * const attribData = (char *)malloc(%d);\n", attribData.capacity());
        code.format("    assert(attribData);\n");
        code.format("    fread(attribData, %d, 1, attribFile);\n", attribData.capacity());
        code.format("    fclose(attribFile);\n");
        code.format("    glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
        for (int i = 0; i < maxAttrib; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            assert msgData.attribs[i].length == count * 4;
            code.format(
                    "    glVertexAttribPointer(%d, %d, GL_FLOAT, GL_FALSE, %d, attribData + %d);CHKERR;\n",
                        i, att.size, maxAttrib * 16, i * 16);
        }
        code.format("    glDrawArrays(%s, 0, %d);CHKERR;\n", mode, count);
        code.format("    free(attribData);\n");
        if (v.attribBuffer != null)
            code.format("    glBindBuffer(GL_ARRAY_BUFFER, %d);CHKERR;\n",
                        v.attribBuffer.name);
        code.write("};\n");
    }

    private void codeGenFunction(final Context ctx, final MessageData msgData)
            throws IOException {
        final Message msg = msgData.msg;
        String call = MessageFormatter.format(msg, true);
        switch (msg.getFunction()) {
            case glActiveTexture:
            case glAttachShader:
            case glBindAttribLocation:
                break;
            case glBindBuffer:
            case glBindFramebuffer:
            case glBindRenderbuffer:
            case glBindTexture:
                codeGenBindNames(msg);
                return;
            case glBlendColor:
            case glBlendEquation:
            case glBlendEquationSeparate:
            case glBlendFunc:
            case glBlendFuncSeparate:
                break;
            case glBufferData:
                call = MessageFormatter.format(msg, true).replace("arg2", "bufferData");
                codeGenBufferData(msg.getData().asReadOnlyByteBuffer(), call);
                return;
            case glBufferSubData:
                call = MessageFormatter.format(msg, true).replace("arg3", "bufferData");
                codeGenBufferData(msg.getData().asReadOnlyByteBuffer(), call);
                return;
            case glCheckFramebufferStatus:
            case glClear:
            case glClearColor:
            case glClearDepthf:
            case glClearStencil:
            case glColorMask:
            case glCompileShader:
                break;
            case glCompressedTexImage2D:
            case glCompressedTexSubImage2D:
            case glCopyTexImage2D:
            case glCopyTexSubImage2D:
                codeGenTextureUpload(msg, false);
                return;
            case glCreateProgram:
                namesHeader.format("extern GLuint program_%d;\n", msg.getRet());
                namesSource.format("GLuint program_%d = 0;\n", msg.getRet());
                code.format("program_%d = glCreateProgram();CHKERR;\n", msg.getRet());
                return;
            case glCreateShader:
                namesHeader.format("extern GLuint shader_%d;\n", msg.getRet());
                namesSource.format("GLuint shader_%d = 0;\n", msg.getRet());
                code.format("shader_%d = %s;\n", msg.getRet(), call);
                return;
            case glCullFace:
                break;
            case glDeleteBuffers:
            case glDeleteFramebuffers:
            case glDeleteProgram:
                programNames.put(msg.getArg0(), 0);
                break;
            case glDeleteRenderbuffers:
                codeGenDeleteNames(msg);
                return;
            case glDeleteShader:
                shaderNames.put(msg.getArg0(), 0);
                return;
            case glDeleteTextures:
                codeGenDeleteNames(msg);
                return;
            case glDepthFunc:
            case glDepthMask:
            case glDepthRangef:
            case glDetachShader:
            case glDisable:
            case glDisableVertexAttribArray:
                break;
            case glDrawArrays:
                // CodeGenDraw(ctx.serverVertex, msgData);
                codeGenDrawArrays(ctx.serverVertex, msgData);
                return;
            case glDrawElements:
                // CodeGenDraw(ctx.serverVertex, msgData);
                codeGenDrawElements(ctx.serverVertex, msgData);
                return;
            case glEnable:
            case glEnableVertexAttribArray:
            case glFinish:
            case glFlush:
            case glFramebufferRenderbuffer:
            case glFramebufferTexture2D:
            case glFrontFace:
                break;
            case glGenBuffers:
                codeGenGenNames(msg);
                return;
            case glGenerateMipmap:
                break;
            case glGenFramebuffers:
            case glGenRenderbuffers:
            case glGenTextures:
                codeGenGenNames(msg);
                return;
            case glGetActiveAttrib:
            case glGetActiveUniform:
            case glGetAttachedShaders:
                break;
            case glGetAttribLocation:
                call = String.format("assert(%d == %s)", msg.getRet(), call);
                break;
            case glGetBooleanv:
            case glGetBufferParameteriv:
                return; // TODO
            case glGetError:
                code.write("CHKERR;\n");
                return;
            case glGetFloatv:
            case glGetFramebufferAttachmentParameteriv:
            case glGetIntegerv:
            case glGetProgramiv:
            case glGetProgramInfoLog:
            case glGetRenderbufferParameteriv:
            case glGetShaderiv:
            case glGetShaderInfoLog:
            case glGetShaderPrecisionFormat:
            case glGetShaderSource:
            case glGetString:
            case glGetTexParameterfv:
            case glGetTexParameteriv:
            case glGetUniformfv:
            case glGetUniformiv:
                return;
            case glGetUniformLocation:
                call = String.format("assert(%d == %s)", msg.getRet(), call);
                break;
            case glGetVertexAttribfv:
            case glGetVertexAttribiv:
            case glGetVertexAttribPointerv:
                return; // TODO
            case glHint:
            case glIsBuffer:
            case glIsEnabled:
            case glIsFramebuffer:
            case glIsProgram:
            case glIsRenderbuffer:
            case glIsShader:
            case glIsTexture:
            case glLineWidth:
            case glLinkProgram:
            case glPixelStorei:
            case glPolygonOffset:
                break;
            case glReadPixels:
                return; // TODO
            case glReleaseShaderCompiler:
            case glRenderbufferStorage:
            case glSampleCoverage:
            case glScissor:
                break;
            case glShaderBinary:
                return; // TODO
            case glShaderSource:
                call = String.format(
                        "glShaderSource(shader_%d, 1, (const char * []){\"%s\"}, NULL)",
                        msg.getArg0(),
                        msg.getData().toStringUtf8().replace("\r", "").replace("\n", "\\n\\\n")
                                .replace("\"", "\\\"")
                                );
                break;
            case glStencilFunc:
            case glStencilFuncSeparate:
            case glStencilMask:
            case glStencilMaskSeparate:
            case glStencilOp:
            case glStencilOpSeparate:
                break;
            case glTexImage2D:
                codeGenTextureUpload(msg, false);
                return;
            case glTexParameterf:
                break;
            case glTexParameterfv:
                return; // TODO
            case glTexParameteri:
                break;
            case glTexParameteriv:
                return; // TODO
            case glTexSubImage2D:
                codeGenTextureUpload(msg, false);
                return;
            case glUniform1f:
            case glUniform1fv:
            case glUniform1i:
            case glUniform1iv:
            case glUniform2f:
            case glUniform2fv:
            case glUniform2i:
            case glUniform2iv:
            case glUniform3f:
            case glUniform3fv:
            case glUniform3i:
            case glUniform3iv:
            case glUniform4f:
            case glUniform4fv:
            case glUniform4i:
            case glUniform4iv:
            case glUniformMatrix2fv:
            case glUniformMatrix3fv:
            case glUniformMatrix4fv:
            case glUseProgram:
            case glValidateProgram:
            case glVertexAttrib1f:
            case glVertexAttrib1fv:
            case glVertexAttrib2f:
            case glVertexAttrib2fv:
            case glVertexAttrib3f:
            case glVertexAttrib3fv:
            case glVertexAttrib4f:
            case glVertexAttrib4fv:
                break;
            case glVertexAttribPointer:
                // if it's user pointer, then CodeGenDrawArrays/Elements will
                // replace it with loaded data just before the draw
                call = call.replace("arg5", "(const void *)0x" +
                        Integer.toHexString(msg.getArg5()));
                break;
            case glViewport:
                break;
            case eglSwapBuffers:
                return;
            default:
                assert false;
                return;
        }
        if (call.indexOf("glEnable(/*cap*/ GL_TEXTURE_2D)") >= 0)
            return;
        else if (call.indexOf("glDisable(/*cap*/ GL_TEXTURE_2D)") >= 0)
            return;
        else if (call.indexOf("glActiveTexture(/*texture*/ GL_TEXTURE_2D)") >= 0)
            return;
        code.write(call + ";CHKERR;\n");
    }

    private void codeGenSetup(final Context ctx) {
        try {
            codeFile = new FileWriter("frame_setup.cpp", false);
            code = new PrintWriter(codeFile);
            dataOut = new FileOutputStream("frame_data.bin", false);
            namesHeaderFile = new FileWriter("frame_names.h", false);
            namesHeader = new PrintWriter(namesHeaderFile);
            namesSourceFile = new FileWriter("frame_names.cpp", false);
            namesSource = new PrintWriter(namesSourceFile);
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
        bufferNames = new SparseIntArray();
        framebufferNames = new SparseIntArray();
        programNames = new SparseIntArray();
        textureNames = new SparseIntArray();
        shaderNames = new SparseIntArray();
        renderbufferNames = new SparseIntArray();

        namesHeader.write("#include <stdlib.h>\n");
        namesHeader.write("#include <stdio.h>\n");
        namesHeader.write("#include <assert.h>\n");
        namesHeader.write("#include <GLES2/gl2.h>\n");
        namesHeader.write("#include <GLES2/gl2ext.h>\n");
        namesHeader.write("#define CHKERR assert(GL_NO_ERROR == glGetError());/**/\n");
        namesHeader.write("void FrameSetup();\n");
        namesHeader.write("extern const unsigned int FrameCount;\n");
        namesHeader.write("extern const GLuint program_0;\n");

        namesSource.write("/*\n" + 
        " * Copyright (C) 2011 The Android Open Source Project\n" + 
        " *\n" + 
        " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" + 
        " * you may not use this file except in compliance with the License.\n" + 
        " * You may obtain a copy of the License at\n" + 
        " *\n" + 
        " *      http://www.apache.org/licenses/LICENSE-2.0\n" + 
        " *\n" + 
        " * Unless required by applicable law or agreed to in writing, software\n" + 
        " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" + 
        " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" + 
        " * See the License for the specific language governing permissions and\n" + 
        " * limitations under the License.\n" + 
        " */\n" + 
        "\n" + 
        "#include <stdlib.h>\n" + 
        "#include <stdio.h>\n" + 
        "\n" + 
        "#include <EGL/egl.h>\n" + 
        "#include <GLES2/gl2.h>\n" + 
        "#include <GLES2/gl2ext.h>\n" + 
        "\n" + 
        "#include <ui/FramebufferNativeWindow.h>\n" + 
        "#include <ui/EGLUtils.h>\n" + 
        "\n" + 
        "#include <private/ui/android_natives_priv.h>\n" + 
        "\n" + 
        "#include <surfaceflinger/Surface.h>\n" + 
        "#include <surfaceflinger/ISurface.h>\n" + 
        "#include <surfaceflinger/SurfaceComposerClient.h>\n" + 
        "\n" + 
        "using namespace android;\n" + 
        "\n" + 
        "static void checkEglError(const char* op, EGLBoolean returnVal = EGL_TRUE)\n" + 
        "{\n" + 
        "    if (returnVal != EGL_TRUE) {\n" + 
        "        fprintf(stderr, \"%s() returned %d\\n\", op, returnVal);\n" + 
        "    }\n" + 
        "\n" + 
        "    for (EGLint error = eglGetError(); error != EGL_SUCCESS; error\n" + 
        "            = eglGetError()) {\n" + 
        "        fprintf(stderr, \"after %s() eglError %s (0x%x)\\n\", op, EGLUtils::strerror(error),\n" + 
        "                error);\n" + 
        "    }\n" + 
        "}\n" + 
        "\n" + 
        "static EGLDisplay dpy;\n" + 
        "static EGLSurface surface;\n" + 
        "\n" + 
        "#include \"frame_names.h\"\n" + 
        "const GLuint program_0 = 0;\n" + 
        "int main(int argc, char** argv)\n" + 
        "{\n" + 
        "    EGLBoolean returnValue;\n" + 
        "    EGLConfig myConfig = {0};\n" + 
        "\n" + 
        "    EGLint context_attribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };\n" + 
        "    EGLint majorVersion;\n" + 
        "    EGLint minorVersion;\n" + 
        "    EGLContext context;\n" + 
        "    EGLint w, h;\n" + 
        "\n" + 
        "\n" + 
        "    checkEglError(\"<init>\");\n" + 
        "    dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);\n" + 
        "    checkEglError(\"eglGetDisplay\");\n" + 
        "    if (dpy == EGL_NO_DISPLAY) {\n" + 
        "        printf(\"eglGetDisplay returned EGL_NO_DISPLAY.\\n\");\n" + 
        "        return 0;\n" + 
        "    }\n" + 
        "\n" + 
        "    returnValue = eglInitialize(dpy, &majorVersion, &minorVersion);\n" + 
        "    checkEglError(\"eglInitialize\", returnValue);\n" + 
        "    if (returnValue != EGL_TRUE) {\n" + 
        "        printf(\"eglInitialize failed\\n\");\n" + 
        "        return 0;\n" + 
        "    }\n" + 
        "\n" + 
        "    sp<SurfaceComposerClient> spClient;\n" + 
        "    sp<SurfaceControl> spControl;\n" + 
        "    sp<Surface> spSurface;\n" + 
        "\n" + 
        "    // create a client to surfaceflinger\n" + 
        "    spClient = new SurfaceComposerClient();\n" + 
        "\n" + 
        "    spControl = spClient->createSurface(getpid(), 0, 1280, 752, PIXEL_FORMAT_RGBX_8888);\n" + 
        "    spClient->openTransaction();\n" + 
        "    spControl->setLayer(350000);\n" + 
        "    spControl->show();\n" + 
        "    spClient->closeTransaction();\n" + 
        "\n" + 
        "    spSurface = spControl->getSurface();\n" + 
        "    EGLNativeWindowType window = spSurface.get();\n" + 
        "\n" + 
        "    printf(\"window=%p\\n\", window);\n" + 
        "    EGLint attrib_list[] = {\n" + 
        "        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,\n" + 
        "        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,\n" + 
        "        EGL_BUFFER_SIZE, 32,\n" + 
        "        EGL_RED_SIZE, 8,\n" + 
        "        EGL_GREEN_SIZE, 8,\n" + 
        "        EGL_BLUE_SIZE, 8,\n" + 
        "        EGL_NONE\n" + 
        "    };\n" + 
        "\n" + 
        "    EGLConfig configs[12] = {0};\n" + 
        "    int num_config = -1;\n" + 
        "    eglChooseConfig(dpy, attrib_list, configs, sizeof(configs) / sizeof(*configs), &num_config);\n" + 
        "    printf(\"eglChooseConfig %d \\n\", num_config);\n" + 
        "\n" + 
        "    surface = eglCreateWindowSurface(dpy, configs[0], window, NULL);\n" + 
        "    checkEglError(\"eglCreateWindowSurface\");\n" + 
        "    if (surface == EGL_NO_SURFACE) {\n" + 
        "        printf(\"gelCreateWindowSurface failed.\\n\");\n" + 
        "        return 0;\n" + 
        "    }\n" + 
        "\n" + 
        "    context = eglCreateContext(dpy, configs[0], EGL_NO_CONTEXT, context_attribs);\n" + 
        "    checkEglError(\"eglCreateContext\");\n" + 
        "    if (context == EGL_NO_CONTEXT) {\n" + 
        "        printf(\"eglCreateContext failed\\n\");\n" + 
        "        return 0;\n" + 
        "    }\n" + 
        "    printf(\"context=%p \\n\", context);\n" + 
        "\n" + 
        "    returnValue = eglMakeCurrent(dpy, surface, surface, context);\n" + 
        "    checkEglError(\"eglMakeCurrent\", returnValue);\n" + 
        "    if (returnValue != EGL_TRUE) {\n" + 
        "        return 0;\n" + 
        "    }\n" + 
        "\n" + 
        "    glClearColor(1,1,1,1);\n" + 
        "    glClear(GL_COLOR_BUFFER_BIT);\n" + 
        "\n" + 
        "    FrameSetup();\n" + 
        "    while (true)\n" + 
        "        for (unsigned int i = 0; i < FrameCount; i++) {\n" + 
        "            Frames[i]();\n" + 
        "            eglSwapBuffers(dpy, surface);\n" + 
        "            printf(\"press ENTER after Frame%d \\n\", i);\n" + 
        "            getchar();\n" + 
        "        }\n" + 
        "\n" + 
        "    return 0;\n" + 
        "}");

        code.write("#include \"frame_names.h\"\n");
        code.write("void FrameSetup(){\n");

        codeGenServerState(ctx.serverState);
        codeGenServerShader(ctx.serverShader);
        codeGenServerTexture(ctx.serverTexture, true);
        codeGenServerVertex(ctx.serverVertex);

        code.write("}\n");

        try {
            codeFile.close();
            makeFile = new FileWriter("Android.mk", false);
            make = new PrintWriter(makeFile);
            make.write("LOCAL_PATH:= $(call my-dir)\n" +
                    "include $(CLEAR_VARS)\n" +
                    "LOCAL_SRC_FILES := \\\n");
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private void codeGenCleanup() {
        make.write("    frame_setup.cpp \\\n");
        make.write("    frame_names.cpp \\\n");
        make.write("#\n");
        make.write(
                "LOCAL_SHARED_LIBRARIES := \\\n" + 
                "    libcutils \\\n" + 
                "    libutils \\\n" + 
                "    libEGL \\\n" + 
                "    libGLESv2 \\\n" + 
                "    libui \\\n" + 
                "    libhardware \\\n" + 
                "    libgui\n" + 
                "\n" + 
                "LOCAL_MODULE:= gles2dbg\n" + 
                "\n" + 
                "LOCAL_MODULE_TAGS := optional\n" + 
                "\n" + 
                "LOCAL_CFLAGS := -DGL_GLEXT_PROTOTYPES -O0 -g -DDEBUG -UNDEBUG\n" + 
                "\n" + 
                "include $(BUILD_EXECUTABLE)");
        try {
            dataOut.flush();
            dataOut.close();
            codeFile.close();
            makeFile.close();
            namesHeaderFile.close();
            namesSourceFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
        dataOut = null;
        code = null;
        codeFile = null;
        make = null;
        makeFile = null;

        bufferNames = null;
        framebufferNames = null;
        programNames = null;
        textureNames = null;
        shaderNames = null;
        renderbufferNames = null;
    }

    private DebugContext dbgCtx;
    private int count;
    private IProgressMonitor progress;

    @Override
    public void run(IProgressMonitor monitor) {
        progress.beginTask("CodeGenFrames", count + 2);
        Context ctx = dbgCtx.getFrame(0).startContext.clone();
        codeGenSetup(ctx);
        progress.worked(1);
        for (int i = 0; i < count; i++) {
            try {
                codeFile = new FileWriter("frame" + i + ".cpp", false);
                code = new PrintWriter(codeFile);
            } catch (IOException e1) {
                e1.printStackTrace();
                assert false;
            }
            make.format("    frame%d.cpp \\\n", i);

            code.write("#include \"frame_names.h\"\n");
            code.format("void Frame%d(){\n", i);
            final Frame frame = dbgCtx.getFrame(i);
            for (int j = 0; j < frame.size(); j++) {
                final MessageData msgData = frame.get(j);
                code.format("/* frame function %d: %s %s*/\n", j, msgData.msg.getFunction(),
                        MessageFormatter.format(msgData.msg, false));
                ctx.processMessage(msgData.msg);
                try {
                    codeGenFunction(ctx, msgData);
                } catch (IOException e) {
                    e.printStackTrace();
                    assert false;
                }
            }
            code.write("}\n");
            try {
                codeFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }
            progress.worked(1);
        }
        for (int i = 0; i < count; i++)
            namesHeader.format("void Frame%d();\n", i);
        namesHeader.format("extern void (* Frames[%d])();\n", count);
        namesSource.format("void (* Frames[%d])() = {\n", count);
        for (int i = 0; i < count; i++) {
            namesSource.format("    Frame%d,\n", i);
        }
        namesSource.write("};\n");
        namesSource.format("const unsigned int FrameCount = %d;\n", count);
        codeGenCleanup();
        progress.worked(1);
    }

    void codeGenFrames(final DebugContext dbgCtx, int count, final Shell shell) {
        this.dbgCtx = dbgCtx;
        this.count = count;
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
        this.progress = dialog.getProgressMonitor();
        try {
            dialog.run(false, true, this);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assert false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.dbgCtx = null;
        this.count = 0;
        progress = null;
    }

    void codeGenFrame(final Frame frame) {
        Context ctx = frame.startContext.clone();
        codeGenSetup(ctx);
        try {
            codeFile = new FileWriter("frame0.cpp", false);
            code = new PrintWriter(codeFile);
        } catch (IOException e1) {
            e1.printStackTrace();
            assert false;
        }
        make.format("    frame0.cpp \\\n");
        code.write("#include \"frame_names.h\"\n");
        code.format("void Frame0(){\n");
        for (int i = 0; i < frame.size(); i++) {
            final MessageData msgData = frame.get(i);
            code.format("/* frame function %d: %s %s*/\n", i, msgData.msg.getFunction(),
                    MessageFormatter.format(msgData.msg, false));
            ctx.processMessage(msgData.msg);
            try {
                codeGenFunction(ctx, msgData);
            } catch (IOException e) {
                e.printStackTrace();
                assert false;
            }
        }
        code.write("}\n");
        namesHeader.write("void Frame0();\n");
        namesHeader.write("extern void (* Frames[1])();\n");
        namesSource.write("void (* Frames[1])() = {Frame0};\n");
        namesSource.write("const unsigned int FrameCount = 1;\n");
        codeGenCleanup();
    }
}
