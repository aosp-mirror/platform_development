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

import java.util.ArrayList;

class GLShader implements Cloneable {
    public final int name;
    GLServerShader context; // the context this was created in
    public final GLEnum type;
    public boolean delete;
    public ArrayList<Integer> programs = new ArrayList<Integer>();
    public String source, originalSource;

    GLShader(final int name, final GLServerShader context, final GLEnum type) {
        this.name = name;
        this.context = context;
        this.type = type;
    }

    /** deep copy */
    public GLShader clone(final GLServerShader copyContext) {
        try {
            GLShader shader = (GLShader) super.clone();
            shader.programs = (ArrayList<Integer>) programs.clone();
            shader.context = copyContext;
            return shader;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }
}

class GLProgram implements Cloneable {
    public final int name;
    GLServerShader context; // the context this was created in
    public boolean delete;
    public int vert, frag;

    GLProgram(final int name, final GLServerShader context) {
        this.name = name;
        this.context = context;
    }

    /** deep copy */
    public GLProgram clone(final GLServerShader copyContext) {
        try {
            GLProgram copy = (GLProgram) super.clone();
            copy.context = copyContext;
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }
}

public class GLServerShader implements Cloneable {
    Context context;
    public SparseArray<GLShader> shaders = new SparseArray<GLShader>();
    public SparseArray<GLProgram> programs = new SparseArray<GLProgram>();
    public GLProgram current = null;
    boolean uiUpdate = false;

    GLServerShader(Context context) {
        this.context = context;
    }

    /** deep copy */
    public GLServerShader clone(final Context copyContext) {
        try {
            GLServerShader copy = (GLServerShader) super.clone();
            copy.context = copyContext;

            copy.shaders = new SparseArray<GLShader>(shaders.size());
            for (int i = 0; i < shaders.size(); i++)
                copy.shaders.append(shaders.keyAt(i), shaders.valueAt(i).clone(copy));

            copy.programs = new SparseArray<GLProgram>(programs.size());
            for (int i = 0; i < programs.size(); i++)
                copy.programs.append(programs.keyAt(i), programs.valueAt(i).clone(copy));

            if (current != null)
                copy.current = copy.programs.get(current.name);
            return copy;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            assert false;
            return null;
        }
    }

    /** returns true if processed */
    public boolean processMessage(final Message msg) {
        boolean oldUiUpdate = uiUpdate;
        uiUpdate = true;
        switch (msg.getFunction()) {
            case glAttachShader:
                glAttachShader(msg);
                return true;
            case glCreateProgram:
                glCreateProgram(msg);
                return true;
            case glCreateShader:
                glCreateShader(msg);
                return true;
            case glDeleteProgram:
                glDeleteProgram(msg);
                return true;
            case glDeleteShader:
                glDeleteShader(msg);
                return true;
            case glDetachShader:
                glDetachShader(msg);
                return true;
            case glShaderSource:
                glShaderSource(msg);
                return true;
            case glUseProgram:
                glUseProgram(msg);
                return true;
            default:
                uiUpdate = oldUiUpdate;
                return false;
        }
    }

    GLShader getShader(int name) {
        if (name == 0)
            return null;
        for (Context ctx : context.shares) {
            GLShader shader = ctx.serverShader.shaders.get(name);
            if (shader != null)
                return shader;
        }
        assert false;
        return null;
    }

    GLProgram getProgram(int name) {
        if (name == 0)
            return null;
        for (Context ctx : context.shares) {
            GLProgram program = ctx.serverShader.programs.get(name);
            if (program != null)
                return program;
        }
        assert false;
        return null;
    }

    // void API_ENTRY(glAttachShader)(GLuint program, GLuint shader)
    void glAttachShader(final Message msg) {
        GLProgram program = getProgram(msg.getArg0());
        assert program != null;
        GLShader shader = getShader(msg.getArg1());
        assert program != null;
        if (GLEnum.GL_VERTEX_SHADER == shader.type)
            program.vert = shader.name;
        else
            program.frag = shader.name;
        shader.programs.add(program.name);
    }

    // GLuint API_ENTRY(glCreateProgram)(void)
    void glCreateProgram(final Message msg) {
        programs.put(msg.getRet(), new GLProgram(msg.getRet(), this));
    }

    // GLuint API_ENTRY(glCreateShader)(GLenum type)
    void glCreateShader(final Message msg) {
        shaders.put(msg.getRet(),
                new GLShader(msg.getRet(), this, GLEnum.valueOf(msg.getArg0())));
    }

    // void API_ENTRY(glDeleteProgram)
    void glDeleteProgram(final Message msg) {
        if (msg.getArg0() == 0)
            return;
        GLProgram program = getProgram(msg.getArg0());
        program.delete = true;
        for (Context ctx : context.shares)
            if (ctx.serverShader.current == program)
                return;
        glDetachShader(program, getShader(program.vert));
        glDetachShader(program, getShader(program.frag));
        programs.remove(program.name);
    }

    // void API_ENTRY(glDeleteShader)(GLuint shader)
    void glDeleteShader(final Message msg) {
        if (msg.getArg0() == 0)
            return;
        GLShader shader = getShader(msg.getArg0());
        shader.delete = true;
        if (shader.programs.size() == 0)
            shaders.remove(shader.name);
    }

    // void API_ENTRY(glDetachShader)(GLuint program, GLuint shader)
    void glDetachShader(final Message msg) {
        glDetachShader(getProgram(msg.getArg0()), getShader(msg.getArg1()));
    }

    void glDetachShader(final GLProgram program, final GLShader shader) {
        if (program == null)
            return;
        if (program.vert == shader.name)
            program.vert = 0;
        else if (program.frag == shader.name)
            program.frag = 0;
        else
            return;
        shader.programs.remove(new Integer(program.name));
        if (shader.delete && shader.programs.size() == 0)
            shaders.remove(shader.name);
    }

    // void API_ENTRY(glShaderSource)(GLuint shader, GLsizei count, const
    // GLchar** string, const GLint* length)
    void glShaderSource(final Message msg) {
        if (!msg.hasData())
            return; // TODO: distinguish between generated calls
        GLShader shader = getShader(msg.getArg0());
        shader.source = shader.originalSource = msg.getData().toStringUtf8();
    }

    // void API_ENTRY(glUseProgram)(GLuint program)
    void glUseProgram(final Message msg) {
        GLProgram oldCurrent = current;
        current = getProgram(msg.getArg0());
        if (null != oldCurrent && oldCurrent.delete && oldCurrent != current) {
            for (Context ctx : context.shares)
                if (ctx.serverShader.current == oldCurrent)
                    return;
            oldCurrent.context.programs.remove(new Integer(oldCurrent.name));
        }
    }
}
