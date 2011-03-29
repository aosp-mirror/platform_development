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

import java.util.ArrayList;
import java.util.HashMap;

class GLShader {
    final int name;
    final GLServerShader context; // the context this was created in
    final GLEnum type;
    boolean delete;
    ArrayList<GLProgram> programs = new ArrayList<GLProgram>();
    String source, originalSource;

    GLShader(final int name, final GLServerShader context, final GLEnum type) {
        this.name = name;
        this.context = context;
        this.type = type;
    }
}

class GLProgram {
    final int name;
    final GLServerShader context; // the context this was created in
    boolean delete;
    GLShader vert, frag;

    GLProgram(final int name, final GLServerShader context) {
        this.name = name;
        this.context = context;
    }
}

public class GLServerShader {
    final Context context;
    HashMap<Integer, GLShader> privateShaders = new HashMap<Integer, GLShader>();
    HashMap<Integer, GLProgram> privatePrograms = new HashMap<Integer, GLProgram>();
    GLProgram current = null;
    public boolean uiUpdate = false;

    GLServerShader(final Context context) {
        this.context = context;
    }

    public void ProcessMessage(final Message msg) {
        boolean oldUiUpdate = uiUpdate;
        uiUpdate = true;
        switch (msg.getFunction()) {
            case glAttachShader:
                glAttachShader(msg);
                break;
            case glCreateProgram:
                glCreateProgram(msg);
                break;
            case glCreateShader:
                glCreateShader(msg);
                break;
            case glDeleteProgram:
                glDeleteProgram(msg);
                break;
            case glDeleteShader:
                glDeleteShader(msg);
                break;
            case glDetachShader:
                glDetachShader(msg);
                break;
            case glShaderSource:
                glShaderSource(msg);
                break;
            case glUseProgram:
                glUseProgram(msg);
                break;
            default:
                uiUpdate = oldUiUpdate;
                break;
        }
    }

    GLShader GetShader(int name) {
        if (name == 0)
            return null;
        for (Context ctx : context.shares) {
            GLShader shader = ctx.serverShader.privateShaders.get(name);
            if (shader != null)
                return shader;
        }
        assert false;
        return null;
    }

    GLProgram GetProgram(int name) {
        if (name == 0)
            return null;
        for (Context ctx : context.shares) {
            GLProgram program = ctx.serverShader.privatePrograms.get(name);
            if (program != null)
                return program;
        }
        assert false;
        return null;
    }

    // void API_ENTRY(glAttachShader)(GLuint program, GLuint shader)
    void glAttachShader(final Message msg) {
        GLProgram program = GetProgram(msg.getArg0());
        GLShader shader = GetShader(msg.getArg1());
        if (GLEnum.GL_VERTEX_SHADER == shader.type)
            program.vert = shader;
        else
            program.frag = shader;
        shader.programs.add(program);
    }

    // GLuint API_ENTRY(glCreateProgram)(void)
    void glCreateProgram(final Message msg) {
        privatePrograms.put(msg.getRet(), new GLProgram(msg.getRet(), this));
    }

    // GLuint API_ENTRY(glCreateShader)(GLenum type)
    void glCreateShader(final Message msg) {
        privateShaders.put(msg.getRet(),
                new GLShader(msg.getRet(), this, GLEnum.valueOf(msg.getArg0())));
    }

    // void API_ENTRY(glDeleteProgram)
    void glDeleteProgram(final Message msg) {
        if (msg.getArg0() == 0)
            return;
        GLProgram program = GetProgram(msg.getArg0());
        program.delete = true;
        for (Context ctx : context.shares)
            if (ctx.serverShader.current == program)
                return;
        glDetachShader(program, program.vert);
        glDetachShader(program, program.frag);
        privatePrograms.remove(program.name);
    }

    // void API_ENTRY(glDeleteShader)(GLuint shader)
    void glDeleteShader(final Message msg) {
        if (msg.getArg0() == 0)
            return;
        GLShader shader = GetShader(msg.getArg0());
        shader.delete = true;
        if (shader.programs.size() == 0)
            privateShaders.remove(shader.name);
    }

    // void API_ENTRY(glDetachShader)(GLuint program, GLuint shader)
    void glDetachShader(final Message msg) {
        glDetachShader(GetProgram(msg.getArg0()), GetShader(msg.getArg1()));
    }

    void glDetachShader(final GLProgram program, final GLShader shader) {
        if (program == null)
            return;
        if (program.vert == shader)
            program.vert = null;
        else if (program.frag == shader)
            program.frag = null;
        else
            return;
        shader.programs.remove(program);
        if (shader.delete && shader.programs.size() == 0)
            shader.context.privateShaders.remove(shader.name);
    }

    // void API_ENTRY(glShaderSource)(GLuint shader, GLsizei count, const
    // GLchar** string, const GLint* length)
    void glShaderSource(final Message msg) {
        if (!msg.hasData())
            return; // TODO: distinguish between generated calls
        GLShader shader = GetShader(msg.getArg0());
        shader.source = shader.originalSource = msg.getData().toStringUtf8();
    }

    // void API_ENTRY(glUseProgram)(GLuint program)
    void glUseProgram(final Message msg) {
        GLProgram oldCurrent = current;
        current = GetProgram(msg.getArg0());
        if (null != oldCurrent && oldCurrent.delete && oldCurrent != current)
        {
            for (Context ctx : context.shares)
                if (ctx.serverShader.current == oldCurrent)
                    return;
            oldCurrent.context.privatePrograms.remove(oldCurrent.name);
        }
    }
}
