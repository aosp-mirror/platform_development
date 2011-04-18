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
    ArrayList<Message> complete = new ArrayList<Message>(); // need synchronized
    ArrayList<Message> commands = new ArrayList<Message>(); // need synchronized
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

    void SendCommands(final int contextId) throws IOException {
        synchronized (commands) {
            for (int i = 0; i < commands.size(); i++) {
                Message command = commands.get(i);
                if (command.getContextId() == contextId || command.getContextId() == 0) {
                    SendMessage(commands.remove(i));
                    i--;
                }
            }
        }
    }

    public void AddCommand(Message command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    // these should only be accessed from the network thread;
    // access call chain starts with run()
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private HashMap<Integer, ArrayList<Message>> incoming = new HashMap<Integer, ArrayList<Message>>();

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", Integer
                    .parseInt(sampleView.actionPort.getText())));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            running = false;
            Error(e);
        }

        while (running) {
            Message msg = null;
            if (incoming.size() > 0) { // find queued incoming
                for (ArrayList<Message> messages : incoming.values())
                    if (messages.size() > 0) {
                        msg = messages.remove(0);
                        break;
                    }
            }
            try {
                if (null == msg) // get incoming from network
                    msg = ReceiveMessage(dis);
                ProcessMessage(dos, msg);
            } catch (IOException e) {
                Error(e);
                running = false;
                break;
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            Error(e);
            running = false;
        }
    }

    private void PutMessage(final Message msg) {
        ArrayList<Message> existing = incoming.get(msg.getContextId());
        if (existing == null)
            incoming.put(msg.getContextId(), existing = new ArrayList<Message>());
        existing.add(msg);
    }

    Message ReceiveMessage(final int contextId) throws IOException {
        Message msg = ReceiveMessage(dis);
        while (msg.getContextId() != contextId) {
            PutMessage(msg);
            msg = ReceiveMessage(dis);
        }
        return msg;
    }

    void SendMessage(final Message msg) throws IOException {
        SendMessage(dos, msg);
    }

    // should only used by DefaultProcessMessage
    private HashMap<Integer, Message> partials = new HashMap<Integer, Message>();

    Message GetPartialMessage(final int contextId) {
        return partials.get(contextId);
    }

    // used to add BeforeCall to complete if it was skipped
    void CompletePartialMessage(final int contextId) {
        final Message msg = partials.remove(contextId);
        assert msg != null;
        assert msg.getType() == Type.BeforeCall;
        synchronized (complete) {
            complete.add(msg);
        }
    }

    // can be used by other message processor as default processor
    void DefaultProcessMessage(final Message msg, boolean expectResponse,
            boolean sendResponse)
            throws IOException {
        final int contextId = msg.getContextId();
        final Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setType(Type.Response);
        builder.setExpectResponse(expectResponse);
        if (msg.getType() == Type.BeforeCall) {
            if (sendResponse) {
                builder.setFunction(Function.CONTINUE);
                SendMessage(dos, builder.build());
            }
            assert !partials.containsKey(contextId);
            partials.put(contextId, msg);
        } else if (msg.getType() == Type.AfterCall) {
            if (sendResponse) {
                builder.setFunction(Function.CONTINUE);
                SendMessage(dos, builder.build());
            }
            assert partials.containsKey(contextId);
            final Message before = partials.remove(contextId);
            assert before.getFunction() == msg.getFunction();
            final Message completed = before.toBuilder().mergeFrom(msg).build();
            synchronized (complete) {
                complete.add(completed);
            }
        } else
            assert false;
    }

    public Message RemoveCompleteMessage(int contextId) {
        synchronized (complete) {
            if (complete.size() == 0)
                return null;
            if (0 == contextId) // get a message for any context
                return complete.remove(0);
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

    private Message ReceiveMessage(final DataInputStream dis)
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
        SendCommands(msg.getContextId());
        return msg;
    }

    private void SendMessage(final DataOutputStream dos, final Message message)
            throws IOException {
        assert message.getFunction() != Function.NEG;
        final byte[] data = message.toByteArray();
        dos.writeInt(data.length);
        dos.write(data);
    }

    private void ProcessMessage(final DataOutputStream dos, final Message msg) throws IOException {
        if (msg.getExpectResponse()) {
            if (sampleView.shaderEditor.ProcessMessage(this, msg))
                return;
            else if (sampleView.breakpointOption.ProcessMessage(this, msg))
                return;
            else
                DefaultProcessMessage(msg, msg.getExpectResponse(), msg.getExpectResponse());
        } else
            DefaultProcessMessage(msg, msg.getExpectResponse(), msg.getExpectResponse());
    }

    void Error(Exception e) {
        sampleView.showError(e);
    }
}
