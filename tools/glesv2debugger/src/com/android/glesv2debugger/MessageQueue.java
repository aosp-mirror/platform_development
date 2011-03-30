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
import com.android.glesv2debugger.DebuggerMessage.Message.Type;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageQueue implements Runnable {

    boolean running = false;
    Thread thread = null;
    ArrayList<Message> complete = new ArrayList<Message>();
    ArrayList<Message> commands = new ArrayList<Message>();
    SampleView sampleView;

    HashMap<Integer, GLServerVertex> serversVertex = new HashMap<Integer, GLServerVertex>();

    public MessageQueue(SampleView sampleView) {
        this.sampleView = sampleView;
    }

    public void Start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void Stop() {
        if (!running)
            return;
        running = false;
    }

    public boolean IsRunning() {
        return running;
    }

    boolean SendCommands(final DataOutputStream dos, final int contextId) throws IOException {
        boolean sent = false;
        synchronized (commands) {
            for (int i = 0; i < commands.size(); i++) {
                Message command = commands.get(i);
                // FIXME: proper context id
                if (command.getContextId() == contextId || contextId == 0) {
                    SendMessage(dos, command);
                    commands.remove(i);
                    i--;
                    sent = true;
                }
            }
        }
        return sent;
    }

    public void AddCommand(Message command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        DataInputStream dis = null;
        DataOutputStream dos = null;
        HashMap<Integer, ArrayList<Message>> incoming = new HashMap<Integer, ArrayList<Message>>();
        try {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", Integer
                    .parseInt(sampleView.actionPort.getText())));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            running = false;
            Error(e);
        }

        // try {
        while (running) {
            Message msg = null;
            if (incoming.size() > 0) { // find queued incoming
                for (ArrayList<Message> messages : incoming
                            .values())
                    if (messages.size() > 0) {
                        msg = messages.get(0);
                        messages.remove(0);
                        break;
                    }
            }
            if (null == msg) { // get incoming from network
                try {
                    msg = ReadMessage(dis);
                    SendResponse(dos, msg);
                } catch (IOException e) {
                    Error(e);
                    running = false;
                    break;
                }
            }

            int contextId = msg.getContextId();
            if (!incoming.containsKey(contextId))
                incoming.put(contextId,
                            new ArrayList<Message>());

            // FIXME: the expected sequence will change for interactive mode
            while (msg.getType() == Type.BeforeCall) {
                Message next = null;
                // get existing message part for this context
                ArrayList<Message> messages = incoming
                            .get(contextId);
                if (messages.size() > 0) {
                    next = messages.get(0);
                    messages.remove(0);
                }
                if (null == next) { // read new part for message
                    try {
                        next = ReadMessage(dis);
                        SendResponse(dos, next);
                    } catch (IOException e) {
                        Error(e);
                        running = false;
                        break;
                    }

                    if (next.getContextId() != contextId) {
                        // message part not for this context
                        if (!incoming.containsKey(next.getContextId()))
                            incoming.put(
                                        next.getContextId(),
                                        new ArrayList<Message>());
                        incoming.get(next.getContextId()).add(next);
                        continue;
                    }
                }

                Message.Builder builder = msg.toBuilder();
                // builder.mergeFrom(next); seems to merge incorrectly
                if (next.hasRet())
                    builder.setRet(next.getRet());
                if (next.hasTime())
                    builder.setTime(next.getTime());
                if (next.hasData())
                    builder.setData(next.getData());
                builder.setType(next.getType());
                msg = builder.build();
            }

            GLServerVertex serverVertex = serversVertex.get(msg.getContextId());
            if (null == serverVertex) {
                serverVertex = new GLServerVertex();
                serversVertex.put(msg.getContextId(), serverVertex);
            }

            // forward message to synchronize state
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

            synchronized (complete) {
                complete.add(msg);
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            Error(e);
            running = false;
        }
        // } catch (Exception e) {
        // Error(e);
        // running = false;
        // }
    }

    public Message RemoveMessage(int contextId) {
        synchronized (complete) {
            if (complete.size() == 0)
                return null;
            if (0 == contextId) // get a message for any context
            {
                Message msg = complete.get(0);
                complete.remove(0);
                return msg;
            }
            for (int i = 0; i < complete.size(); i++) {
                Message msg = complete.get(i);
                if (msg.getContextId() == contextId) {
                    complete.remove(i);
                    return msg;
                }
            }
        }
        return null;
    }

    Message ReadMessage(final DataInputStream dis)
            throws IOException {
        int len = 0;
        try {
            len = dis.readInt();
        } catch (EOFException e) {
            Error(new Exception("EOF"));
        }
        byte[] buffer = new byte[len];
        int readLen = 0;
        while (readLen < len) {
            int read = -1;
            try {
                read = dis.read(buffer, readLen, len - readLen);
            } catch (EOFException e) {
                Error(new Exception("EOF"));
            }
            if (read < 0) {
                Error(new Exception("read length = " + read));
                return null;
            } else
                readLen += read;
        }
        Message msg = Message.parseFrom(buffer);
        return msg;
    }

    void SendMessage(final DataOutputStream dos, final Message message)
            throws IOException {
        final byte[] data = message.toByteArray();
        dos.writeInt(data.length);
        dos.write(data);
    }

    void SendResponse(final DataOutputStream dos, final Message msg) throws IOException {
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(msg.getContextId());
        if (msg.getType() == Type.BeforeCall)
            builder.setFunction(Function.CONTINUE);
        else if (msg.getType() == Type.AfterCall)
            builder.setFunction(Function.SKIP);
        builder.setType(Type.Response);
        builder.setExpectResponse(false);
        // FIXME: consider using proper context id
        if (SendCommands(dos, 0) || msg.getExpectResponse())
            if (builder.hasFunction())
                SendMessage(dos, builder.build());
    }

    void Error(Exception e) {
        sampleView.showError(e);
    }
}
