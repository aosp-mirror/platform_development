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

import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.glesv2debugger.DebuggerMessage.Message.Type;

import java.io.EOFException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageQueue implements Runnable {

    boolean running = false;
    Thread thread = null;
    ArrayList<DebuggerMessage.Message> complete = new ArrayList<DebuggerMessage.Message>();
    ArrayList<DebuggerMessage.Message> commands = new ArrayList<DebuggerMessage.Message>();
    SampleView sampleView;

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

    void SendCommands(final DataOutputStream dos, final int contextId) throws IOException {
        synchronized (commands) {
            for (int i = 0; i < commands.size(); i++) {
                DebuggerMessage.Message command = commands.get(i);
                if (command.getContextId() == contextId || contextId == 0) { // FIXME:
                                                                             // proper
                                                                             // context
                                                                             // id
                    SendMessage(dos, command);
                    commands.remove(i);
                    i--;
                }
            }
        }
        SendResponse(dos, contextId, DebuggerMessage.Message.Function.SKIP);
    }

    public void AddCommand(DebuggerMessage.Message command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        DataInputStream dis = null;
        DataOutputStream dos = null;
        HashMap<Integer, ArrayList<DebuggerMessage.Message>> incoming = new HashMap<Integer, ArrayList<DebuggerMessage.Message>>();
        try {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 5039));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            running = false;
            Error(e);
        }

        try {
            while (running) {
                DebuggerMessage.Message msg = null;
                if (incoming.size() > 0) { // find queued incoming
                    for (ArrayList<DebuggerMessage.Message> messages : incoming
                            .values())
                        if (messages.size() > 0) {
                            msg = messages.get(0);
                            messages.remove(0);
                            break;
                        }
                }
                if (null == msg) { // get incoming from network
                    msg = ReadMessage(dis);
                    if (msg.getExpectResponse()) {
                        if (msg.getType() == Type.BeforeCall)
                            SendResponse(dos, msg.getContextId(),
                                    DebuggerMessage.Message.Function.CONTINUE);
                        else if (msg.getType() == Type.AfterCall)
                            // after GL function call
                            SendCommands(dos, 0); // FIXME: proper context id
                        // SendResponse(dos, msg.getContextId(),
                        // DebuggerMessage.Message.Function.SKIP);
                        else if (msg.getType() == Type.Response)
                            ;
                        else
                            assert false;
                    }
                }

                int contextId = msg.getContextId();
                if (!incoming.containsKey(contextId))
                    incoming.put(contextId,
                            new ArrayList<DebuggerMessage.Message>());

                // FIXME: the expected sequence will change for interactive mode
                while (msg.getType() == Type.BeforeCall) {
                    DebuggerMessage.Message next = null;
                    // get existing message part for this context
                    ArrayList<DebuggerMessage.Message> messages = incoming
                            .get(contextId);
                    if (messages.size() > 0) {
                        next = messages.get(0);
                        messages.remove(0);
                    }
                    if (null == next) { // read new part for message
                        next = ReadMessage(dis);

                        if (next.getExpectResponse()) {
                            if (next.getType() == Type.BeforeCall)
                                SendResponse(
                                        dos,
                                        next.getContextId(),
                                        DebuggerMessage.Message.Function.CONTINUE);
                            else if (next.getType() == Type.AfterCall)
                                SendCommands(dos, 0); // FIXME: proper context id
                            else if (msg.getType() == Type.Response)
                                ;
                            else
                                assert false;
                        }

                        if (next.getContextId() != contextId) {
                            // message part not for this context
                            if (!incoming.containsKey(next.getContextId()))
                                incoming.put(
                                        next.getContextId(),
                                        new ArrayList<DebuggerMessage.Message>());
                            incoming.get(next.getContextId()).add(next);
                            continue;
                        }
                    }

                    DebuggerMessage.Message.Builder builder = msg.toBuilder();
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

                synchronized (complete) {
                    complete.add(msg);
                }
            }
            socket.close();
        } catch (Exception e) {
            Error(e);
            running = false;
        }
    }

    public DebuggerMessage.Message RemoveMessage(int contextId) {
        synchronized (complete) {
            if (complete.size() == 0)
                return null;
            if (0 == contextId) // get a message of any
            {
                DebuggerMessage.Message msg = complete.get(0);
                complete.remove(0);
                return msg;
            }
            for (int i = 0; i < complete.size(); i++) {
                DebuggerMessage.Message msg = complete.get(i);
                if (msg.getContextId() == contextId) {
                    complete.remove(i);
                    return msg;
                }
            }
        }
        return null;
    }

    DebuggerMessage.Message ReadMessage(final DataInputStream dis)
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
        DebuggerMessage.Message msg = DebuggerMessage.Message.parseFrom(buffer);
        return msg;
    }

    void SendMessage(final DataOutputStream dos, final DebuggerMessage.Message message)
            throws IOException {
        final byte[] data = message.toByteArray();
        dos.writeInt(data.length);
        dos.write(data);
    }

    void SendResponse(final DataOutputStream dos, final int contextId,
            final DebuggerMessage.Message.Function function) throws IOException {
        DebuggerMessage.Message.Builder builder = DebuggerMessage.Message
                .newBuilder();
        builder.setContextId(contextId);
        builder.setFunction(function);
        builder.setType(Type.Response);
        builder.setExpectResponse(false);
        SendMessage(dos, builder.build());
    }

    void Error(Exception e) {
        sampleView.showError(e);
    }
}
