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

public class Context {
    public final int contextId;
    ArrayList<Context> shares = new ArrayList<Context>(); // includes self
    public GLServerVertex serverVertex = new GLServerVertex();
    public GLServerShader serverShader = new GLServerShader(this);
    public byte[] readPixelRef = new byte[0];

    public Context(int contextId) {
        this.contextId = contextId;
        shares.add(this);
    }

    public Message ProcessMessage(Message msg) {
        switch (msg.getFunction()) {
            case glBindBuffer:
                serverVertex.glBindBuffer(msg);
                break;
            case glBufferData:
                serverVertex.glBufferData(msg);
                break;
            case glBufferSubData:
                serverVertex.glBufferSubData(msg);
                break;
            case glDeleteBuffers:
                serverVertex.glDeleteBuffers(msg);
                break;
            case glDrawArrays:
                if (msg.hasArg7())
                    msg = serverVertex.glDrawArrays(msg);
                break;
            case glDrawElements:
                if (msg.hasArg7())
                    msg = serverVertex.glDrawElements(msg);
                break;
            case glDisableVertexAttribArray:
                serverVertex.glDisableVertexAttribArray(msg);
                break;
            case glEnableVertexAttribArray:
                serverVertex.glEnableVertexAttribArray(msg);
                break;
            case glGenBuffers:
                serverVertex.glGenBuffers(msg);
                break;
            case glVertexAttribPointer:
                serverVertex.glVertexAttribPointer(msg);
                break;
            case glVertexAttrib1f:
                serverVertex.glVertexAttrib1f(msg);
                break;
            case glVertexAttrib1fv:
                serverVertex.glVertexAttrib1fv(msg);
                break;
            case glVertexAttrib2f:
                serverVertex.glVertexAttrib2f(msg);
                break;
            case glVertexAttrib2fv:
                serverVertex.glVertexAttrib2fv(msg);
                break;
            case glVertexAttrib3f:
                serverVertex.glVertexAttrib3f(msg);
                break;
            case glVertexAttrib3fv:
                serverVertex.glVertexAttrib3fv(msg);
                break;
            case glVertexAttrib4f:
                serverVertex.glVertexAttrib4f(msg);
                break;
            case glVertexAttrib4fv:
                serverVertex.glVertexAttrib4fv(msg);
                break;
        }
        serverShader.ProcessMessage(msg);
        return msg;
    }
}
