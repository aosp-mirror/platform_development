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

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public class CodeGen {
    private FileWriter codeFile, makeFile, namesHeaderFile, namesSourceFile;
    private PrintWriter code, make, namesHeader, namesSource;
    private FileOutputStream dataOut;
    private SparseIntArray bufferNames,
            framebufferNames, programNames, textureNames, shaderNames, renderbufferNames;

    /** return true if msg was a texture upload */
    private boolean CodeGenTextureUpload(final Message msg, final boolean replaceCopy) {
        String s = null;
        switch (msg.getFunction()) {
            case glCompressedTexImage2D:
                s = MessageFormatter.Format(msg, true).replace("arg7", "texData");
                break;
            case glCompressedTexSubImage2D:
            case glTexImage2D:
            case glTexSubImage2D:
                s = MessageFormatter.Format(msg, true).replace("arg8", "texData");
                break;
            case glCopyTexImage2D:
                if (!replaceCopy) {
                    code.write(MessageFormatter.Format(msg, true));
                    code.write(";CHKERR;\n");
                    return true;
                }
                assert msg.getArg2() == msg.getPixelFormat(); // TODO
                s = "//" + MessageFormatter.Format(msg, true) + "\n";
                s += String.format("glTexImage2D(%s, %d, %s, %d, %d, %d, %s, %s, texData);CHKERR;",
                        GLEnum.valueOf(msg.getArg0()), msg.getArg1(),
                        GLEnum.valueOf(msg.getArg2()), msg.getArg5(), msg.getArg6(),
                        msg.getArg7(), GLEnum.valueOf(msg.getPixelFormat()),
                        GLEnum.valueOf(msg.getPixelType()));
                break;
            case glCopyTexSubImage2D:
                if (!replaceCopy) {
                    code.write(MessageFormatter.Format(msg, true));
                    code.write(";CHKERR;\n");
                    return true;
                }
                assert msg.getArg2() == msg.getPixelFormat(); // TODO
                s = "//" + MessageFormatter.Format(msg, true) + "\n";
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
            final byte[] data = MessageProcessor.LZFDecompressChunks(msg.getData());
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

    private void CodeGenServerState(final GLServerState serverState) {
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
            final String s = MessageFormatter.Format(msg, true);
            code.write(s);
            code.write(";\n");
        }
        // TODO: stencil and integers
    }

    private void CodeGenServerShader(final GLServerShader serverShader) {
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

    private void CodeGenServerTexture(final GLServerTexture serverTexture, final boolean replaceCopy) {
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
                if (CodeGenTextureUpload(msg, replaceCopy))
                    continue;
                switch (msg.getFunction()) {
                    case glGenerateMipmap:
                        s = MessageFormatter.Format(msg, true);
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
        for (int i = 0; i < serverTexture.tmu2D.length && i < 16; i++) {
            code.format("glActiveTexture(%s);CHKERR;\n",
                    GLEnum.valueOf(GLEnum.GL_TEXTURE0.value + i));
            code.format("glBindTexture(GL_TEXTURE_2D, texture_%d);CHKERR;\n",
                    serverTexture.tmu2D[i]);
        }
        for (int i = 0; i < serverTexture.tmuCube.length && i < 16; i++) {
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

    private void CodeGenBufferData(final ByteBuffer buffer, final String call) {
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

    private void CodeGenServerVertex(final GLServerVertex v) {
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
                    CodeGenBufferData(buffer.data, s);
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

    private void CodeGenGenNames(final Message msg) {
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

    private void CodeGenDeleteNames(final Message msg) {
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

    private void CodeGenBindNames(final Message msg) {
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
        }
        code.format("%s = %d;\n", id, name);
        namesArray.put(name, name);
        code.write(MessageFormatter.Format(msg, true));
        code.write(";CHKERR;\n");
    }

    private void CodeGenDrawArrays(final GLServerVertex v, final ByteBuffer[] attribs,
            final int mode, final int count) throws IOException {
        code.write("{\n");
        code.format("    FILE * attribFile = fopen(\"/sdcard/frame_data.bin\", \"rb\");CHKERR;\n");
        code.format("    assert(attribFile);CHKERR;\n");
        code.format("    fseek(attribFile, %d, SEEK_SET);CHKERR;\n", dataOut.getChannel()
                .position());
        code.format("    glBindBuffer(GL_ARRAY_BUFFER, 0);CHKERR;\n");
        for (int i = 0; i < attribs.length; i++) {
            final GLAttribPointer att = v.attribPointers[i];
            if (!att.enabled)
                continue;
            final byte[] data = attribs[i].array();
            final String typeName = "GL" + att.type.name().substring(3).toLowerCase();
            code.format("    %s * attrib%d = (%s *)malloc(%d);CHKERR;\n", typeName, i, typeName,
                    data.length);
            dataOut.write(data);
            code.format("    fread(attrib%d, %d, 1, attribFile);CHKERR;\n", i, data.length);
            // code.format("    for (unsigned int i = 0; i < %d; i++)\n", count
            // * att.size);
            // code.format("        printf(\"%%f \\n\", attrib%d[i]);CHKERR;\n",
            // i);
            code.format("    glVertexAttribPointer(%d, %d, %s, %b, %d, attrib%d);CHKERR;\n",
                    i, att.size, att.type, att.normalized,
                    att.size * GLServerVertex.TypeSize(att.type), i);
        }
        code.format("    fclose(attribFile);CHKERR;\n");
        code.format("    glDrawArrays(%s, 0, %d);CHKERR;\n", GLEnum.valueOf(mode), count);
        for (int i = 0; i < attribs.length; i++)
            if (v.attribPointers[i].enabled)
                code.format("    free(attrib%d);CHKERR;\n", i);

        if (v.attribBuffer != null)
            code.format("    glBindBuffer(GL_ARRAY_BUFFER, %d);CHKERR;\n",
                    v.attribBuffer.name);
        code.write("};\n");
    }

    private void CodeGenFunction(final Context ctx, final MessageData msgData) throws IOException {
        final Message msg = msgData.msg;
        final Message oriMsg = msgData.oriMsg;
        String call = MessageFormatter.Format(msg, true);
        switch (msg.getFunction()) {
            case glActiveTexture:
            case glAttachShader:
            case glBindAttribLocation:
                break;
            case glBindBuffer:
            case glBindFramebuffer:
            case glBindRenderbuffer:
            case glBindTexture:
                CodeGenBindNames(msg);
                return;
            case glBlendColor:
            case glBlendEquation:
            case glBlendEquationSeparate:
            case glBlendFunc:
            case glBlendFuncSeparate:
                break;
            case glBufferData:
                call = MessageFormatter.Format(msg, true).replace("arg2", "bufferData");
                CodeGenBufferData(msg.getData().asReadOnlyByteBuffer(), call);
                return;
            case glBufferSubData:
                call = MessageFormatter.Format(msg, true).replace("arg3", "bufferData");
                CodeGenBufferData(msg.getData().asReadOnlyByteBuffer(), call);
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
                CodeGenTextureUpload(msg, false);
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
                CodeGenDeleteNames(msg);
                return;
            case glDeleteShader:
                shaderNames.put(msg.getArg0(), 0);
                return;
            case glDeleteTextures:
                CodeGenDeleteNames(msg);
                return;
            case glDepthFunc:
            case glDepthMask:
            case glDepthRangef:
            case glDetachShader:
            case glDisable:
            case glDisableVertexAttribArray:
                break;
            case glDrawArrays:
                CodeGenDrawArrays(ctx.serverVertex, msgData.attribs, msg.getArg0(), msg.getArg2());
                return;
            case glDrawElements:
                CodeGenDrawArrays(ctx.serverVertex, msgData.attribs, msg.getArg0(), msg.getArg1());
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
                CodeGenGenNames(msg);
                return;
            case glGenerateMipmap:
                break;
            case glGenFramebuffers:
            case glGenRenderbuffers:
            case glGenTextures:
                CodeGenGenNames(msg);
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
                CodeGenTextureUpload(msg, false);
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
                CodeGenTextureUpload(msg, false);
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
                // pointer set during glDrawArrays/Elements from captured data
                call = call.replace("arg5", "NULL");
                break;
            case glViewport:
                break;
            case eglSwapBuffers:
                return;
            default:
                assert false;
                return;
        }
        code.write(call + ";CHKERR;\n");
    }

    private void CodeGenSetup(final Context ctx) {
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

        namesSource.write("#include \"frame_names.h\"\n");
        namesSource.write("const GLuint program_0 = 0;\n");

        code.write("#include \"frame_names.h\"\n");
        code.write("void FrameSetup(){\n");

        CodeGenServerState(ctx.serverState);
        CodeGenServerShader(ctx.serverShader);
        CodeGenServerTexture(ctx.serverTexture, true);
        CodeGenServerVertex(ctx.serverVertex);

        code.write("}\n");

        try {
            codeFile.close();
            makeFile = new FileWriter("frame_src.mk", false);
            make = new PrintWriter(makeFile);
            make.write("LOCAL_SRC_FILES := \\\n");
        } catch (IOException e) {
            e.printStackTrace();
            assert false;
        }
    }

    private void CodeGenCleanup() {
        make.write("    frame_setup.cpp \\\n");
        make.write("    frame_names.cpp");
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

    void CodeGenFrames(final DebugContext dbgCtx, int count) {
        Context ctx = dbgCtx.frames.get(0).startContext.clone();
        CodeGenSetup(ctx);
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
            final Frame frame = dbgCtx.frames.get(i);
            for (int j = 0; j < frame.calls.size(); j++) {
                final MessageData msgData = frame.calls.get(j);
                code.format("/* frame function %d: %s %s*/\n", j, msgData.msg.getFunction(),
                        MessageFormatter.Format(msgData.msg, false));
                ctx.ProcessMessage(msgData.oriMsg);
                try {
                    CodeGenFunction(ctx, msgData);
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
        CodeGenCleanup();
    }

    void CodeGenFrame(final Frame frame) {
        Context ctx = frame.startContext.clone();
        CodeGenSetup(ctx);
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
        for (int i = 0; i < frame.calls.size(); i++) {
            final MessageData msgData = frame.calls.get(i);
            code.format("/* frame function %d: %s %s*/\n", i, msgData.msg.getFunction(),
                    MessageFormatter.Format(msgData.msg, false));
            ctx.ProcessMessage(msgData.oriMsg);
            try {
                CodeGenFunction(ctx, msgData);
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
        CodeGenCleanup();
    }
}
