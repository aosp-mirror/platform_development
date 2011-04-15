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

// skeleton from stdout of generate_MessageParser_java.py

package com.android.glesv2debugger;

import com.android.glesv2debugger.DebuggerMessage.Message;

public class MessageParserEx extends MessageParser {

    @Override
    void parse_glBufferData(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLsizeiptr size
        // TODO // GLvoid data
        builder.setArg3(parseArgument()); // GLenum usage
    }

    @Override
    void parse_glBufferSubData(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLintptr offset
        builder.setArg2(parseArgument()); // GLsizeiptr size
        // TODO // GLvoid data
    }

    @Override
    void parse_glCompressedTexImage2D(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLint level
        builder.setArg2(parseArgument()); // GLenum internalformat
        builder.setArg3(parseArgument()); // GLsizei width
        builder.setArg4(parseArgument()); // GLsizei height
        builder.setArg5(parseArgument()); // GLint border
        builder.setArg6(parseArgument()); // GLsizei imageSize
        // TODO: GLvoid* data
    }

    @Override
    void parse_glCompressedTexSubImage2D(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLint level
        builder.setArg2(parseArgument()); // GLint xoffset
        builder.setArg3(parseArgument()); // GLint yoffset
        builder.setArg4(parseArgument()); // GLsizei width
        builder.setArg5(parseArgument()); // GLsizei height
        builder.setArg6(parseArgument()); // GLenum format
        builder.setArg7(parseArgument()); // GLsizei imageSize
        // TODO: GLvoid* data
    }

    @Override
    void parse_glDrawElements(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum mode
        builder.setArg1(parseArgument()); // GLsizei count
        builder.setArg2(parseArgument()); // GLenum type
        // TODO: GLvoid* indices
    }

    @Override
    void parse_glGetActiveAttrib(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLuint index
        builder.setArg2(parseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        // TODO: GLint* size
        // TODO: GLenum* type
        builder.setData(parseString()); // GLchar name
    }

    @Override
    void parse_glGetActiveUniform(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLuint index
        builder.setArg2(parseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        // TODO: GLint* size
        // TODO: GLenum* type
        builder.setData(parseString()); // GLchar name
    }

    @Override
    void parse_glGetAttachedShaders(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLsizei maxcount
        // TODO: GLsizei* count
        // TODO: GLuint* shaders
    }

    @Override
    void parse_glGetBooleanv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum pname
        // TODO: GLboolean* params
    }

    @Override
    void parse_glGetBufferParameteriv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetFloatv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void parse_glGetFramebufferAttachmentParameteriv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum attachment
        builder.setArg2(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetIntegerv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetProgramInfoLog(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(parseString()); // GLchar infolog
    }

    @Override
    void parse_glGetRenderbufferParameteriv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetShaderInfoLog(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint shader
        builder.setArg1(parseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(parseString()); // GLchar infolog
    }

    @Override
    void parse_glGetShaderPrecisionFormat(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum shadertype
        builder.setArg1(parseArgument()); // GLenum precisiontype
        // TODO: GLint* range
        // TODO: GLint* precision
    }

    @Override
    void parse_glGetShaderSource(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint shader
        builder.setArg1(parseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(parseString()); // GLchar source
    }

    @Override
    void parse_glGetTexParameterfv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void parse_glGetTexParameteriv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetUniformfv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLint location
        // TODO: GLfloat* params
    }

    @Override
    void parse_glGetUniformiv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint program
        builder.setArg1(parseArgument()); // GLint location
        // TODO: GLint* params
    }

    @Override
    void parse_glGetVertexAttribfv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint index
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void parse_glGetVertexAttribiv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint index
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glGetVertexAttribPointerv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint index
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLvoid** pointer
    }

    @Override
    void parse_glReadPixels(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLint x
        builder.setArg1(parseArgument()); // GLint y
        builder.setArg2(parseArgument()); // GLsizei width
        builder.setArg3(parseArgument()); // GLsizei height
        builder.setArg4(parseArgument()); // GLenum format
        builder.setArg5(parseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void parse_glShaderBinary(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLsizei n
        // TODO: GLuint* shaders
        builder.setArg2(parseArgument()); // GLenum binaryformat
        // TODO: GLvoid* binary
        builder.setArg4(parseArgument()); // GLsizei length
    }

    @Override
    void parse_glShaderSource(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint shader
        builder.setArg1(parseArgument()); // GLsizei count
        assert 1 == builder.getArg1();
        builder.setData(parseString()); // GLchar** string
        builder.setArg3(parseArgument());// not used, always 1 null terminated
                                         // string; GLint* length
    }

    @Override
    void parse_glTexImage2D(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLint level
        builder.setArg2(parseArgument()); // GLint internalformat
        builder.setArg3(parseArgument()); // GLsizei width
        builder.setArg4(parseArgument()); // GLsizei height
        builder.setArg5(parseArgument()); // GLint border
        builder.setArg6(parseArgument()); // GLenum format
        builder.setArg7(parseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void parse_glTexParameterfv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void parse_glTexParameteriv(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void parse_glTexSubImage2D(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLenum target
        builder.setArg1(parseArgument()); // GLint level
        builder.setArg2(parseArgument()); // GLint xoffset
        builder.setArg3(parseArgument()); // GLint yoffset
        builder.setArg4(parseArgument()); // GLsizei width
        builder.setArg5(parseArgument()); // GLsizei height
        builder.setArg6(parseArgument()); // GLenum format
        builder.setArg7(parseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void parse_glVertexAttribPointer(Message.Builder builder) {
        builder.setArg0(parseArgument()); // GLuint indx
        builder.setArg1(parseArgument()); // GLint size
        builder.setArg2(parseArgument()); // GLenum type
        builder.setArg3(parseArgument()); // GLboolean normalized
        builder.setArg4(parseArgument()); // GLsizei stride
        // TODO: GLvoid* ptr
    }

    public final static MessageParserEx instance = new MessageParserEx();
}
