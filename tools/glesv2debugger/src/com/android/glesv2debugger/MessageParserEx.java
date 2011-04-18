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
    void Parse_glBufferData(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLsizeiptr size
        // TODO // GLvoid data
        builder.setArg3(ParseArgument()); // GLenum usage
    }

    @Override
    void Parse_glBufferSubData(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLintptr offset
        builder.setArg2(ParseArgument()); // GLsizeiptr size
        // TODO // GLvoid data
    }

    @Override
    void Parse_glCompressedTexImage2D(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLint level
        builder.setArg2(ParseArgument()); // GLenum internalformat
        builder.setArg3(ParseArgument()); // GLsizei width
        builder.setArg4(ParseArgument()); // GLsizei height
        builder.setArg5(ParseArgument()); // GLint border
        builder.setArg6(ParseArgument()); // GLsizei imageSize
        // TODO: GLvoid* data
    }

    @Override
    void Parse_glCompressedTexSubImage2D(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLint level
        builder.setArg2(ParseArgument()); // GLint xoffset
        builder.setArg3(ParseArgument()); // GLint yoffset
        builder.setArg4(ParseArgument()); // GLsizei width
        builder.setArg5(ParseArgument()); // GLsizei height
        builder.setArg6(ParseArgument()); // GLenum format
        builder.setArg7(ParseArgument()); // GLsizei imageSize
        // TODO: GLvoid* data
    }

    @Override
    void Parse_glDrawElements(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum mode
        builder.setArg1(ParseArgument()); // GLsizei count
        builder.setArg2(ParseArgument()); // GLenum type
        // TODO: GLvoid* indices
    }

    @Override
    void Parse_glGetActiveAttrib(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLuint index
        builder.setArg2(ParseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        // TODO: GLint* size
        // TODO: GLenum* type
        builder.setData(ParseString()); // GLchar name
    }

    @Override
    void Parse_glGetActiveUniform(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLuint index
        builder.setArg2(ParseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        // TODO: GLint* size
        // TODO: GLenum* type
        builder.setData(ParseString()); // GLchar name
    }

    @Override
    void Parse_glGetAttachedShaders(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLsizei maxcount
        // TODO: GLsizei* count
        // TODO: GLuint* shaders
    }

    @Override
    void Parse_glGetBooleanv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum pname
        // TODO: GLboolean* params
    }

    @Override
    void Parse_glGetBufferParameteriv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetFloatv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void Parse_glGetFramebufferAttachmentParameteriv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum attachment
        builder.setArg2(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetIntegerv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetProgramInfoLog(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(ParseString()); // GLchar infolog
    }

    @Override
    void Parse_glGetRenderbufferParameteriv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetShaderInfoLog(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint shader
        builder.setArg1(ParseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(ParseString()); // GLchar infolog
    }

    @Override
    void Parse_glGetShaderPrecisionFormat(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum shadertype
        builder.setArg1(ParseArgument()); // GLenum precisiontype
        // TODO: GLint* range
        // TODO: GLint* precision
    }

    @Override
    void Parse_glGetShaderSource(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint shader
        builder.setArg1(ParseArgument()); // GLsizei bufsize
        // TODO: GLsizei* length
        builder.setData(ParseString()); // GLchar source
    }

    @Override
    void Parse_glGetTexParameterfv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void Parse_glGetTexParameteriv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetUniformfv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLint location
        // TODO: GLfloat* params
    }

    @Override
    void Parse_glGetUniformiv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint program
        builder.setArg1(ParseArgument()); // GLint location
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetVertexAttribfv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint index
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void Parse_glGetVertexAttribiv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint index
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glGetVertexAttribPointerv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint index
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLvoid** pointer
    }

    @Override
    void Parse_glReadPixels(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLint x
        builder.setArg1(ParseArgument()); // GLint y
        builder.setArg2(ParseArgument()); // GLsizei width
        builder.setArg3(ParseArgument()); // GLsizei height
        builder.setArg4(ParseArgument()); // GLenum format
        builder.setArg5(ParseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void Parse_glShaderBinary(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLsizei n
        // TODO: GLuint* shaders
        builder.setArg2(ParseArgument()); // GLenum binaryformat
        // TODO: GLvoid* binary
        builder.setArg4(ParseArgument()); // GLsizei length
    }

    @Override
    void Parse_glShaderSource(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint shader
        builder.setArg1(ParseArgument()); // GLsizei count
        assert 1 == builder.getArg1();
        builder.setData(ParseString()); // GLchar** string
        builder.setArg3(ParseArgument());// not used, always 1 null terminated string; GLint* length
    }

    @Override
    void Parse_glTexImage2D(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLint level
        builder.setArg2(ParseArgument()); // GLint internalformat
        builder.setArg3(ParseArgument()); // GLsizei width
        builder.setArg4(ParseArgument()); // GLsizei height
        builder.setArg5(ParseArgument()); // GLint border
        builder.setArg6(ParseArgument()); // GLenum format
        builder.setArg7(ParseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void Parse_glTexParameterfv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLfloat* params
    }

    @Override
    void Parse_glTexParameteriv(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLenum pname
        // TODO: GLint* params
    }

    @Override
    void Parse_glTexSubImage2D(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLenum target
        builder.setArg1(ParseArgument()); // GLint level
        builder.setArg2(ParseArgument()); // GLint xoffset
        builder.setArg3(ParseArgument()); // GLint yoffset
        builder.setArg4(ParseArgument()); // GLsizei width
        builder.setArg5(ParseArgument()); // GLsizei height
        builder.setArg6(ParseArgument()); // GLenum format
        builder.setArg7(ParseArgument()); // GLenum type
        // TODO: GLvoid* pixels
    }

    @Override
    void Parse_glVertexAttribPointer(Message.Builder builder) {
        builder.setArg0(ParseArgument()); // GLuint indx
        builder.setArg1(ParseArgument()); // GLint size
        builder.setArg2(ParseArgument()); // GLenum type
        builder.setArg3(ParseArgument()); // GLboolean normalized
        builder.setArg4(ParseArgument()); // GLsizei stride
        // TODO: GLvoid* ptr
    }

}
