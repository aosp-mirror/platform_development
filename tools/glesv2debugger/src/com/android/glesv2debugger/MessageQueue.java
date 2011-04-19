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
import com.android.sdklib.util.SparseArray;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteOrder;
import java.util.ArrayList;

abstract interface ProcessMessage {
    abstract boolean processMessage(final MessageQueue queue, final Message msg)
            throws IOException;
}

public class MessageQueue implements Runnable {

    private boolean running = false;
    private ByteOrder byteOrder;
    private FileInputStream file; // if null, create and use socket
    Thread thread = null;
    private final ProcessMessage[] processes;
    private ArrayList<Message> complete = new ArrayList<Message>(); // synchronized
    private ArrayList<Message> commands = new ArrayList<Message>(); // synchronized
    private SampleView sampleView;

    public MessageQueue(SampleView sampleView, final ProcessMessage[] processes) {
        this.sampleView = sampleView;
        this.processes = processes;
    }

    public void start(final ByteOrder byteOrder, final FileInputStream file) {
        if (running)
            return;
        running = true;
        this.byteOrder = byteOrder;
        this.file = file;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (!running)
            return;
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void sendCommands(final int contextId) throws IOException {
        synchronized (commands) {
            for (int i = 0; i < commands.size(); i++) {
                Message command = commands.get(i);
                if (command.getContextId() == contextId || command.getContextId() == 0) {
                    sendMessage(commands.remove(i));
                    i--;
                }
            }
        }
    }

    public void addCommand(Message command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    // these should only be accessed from the network thread;
    // access call chain starts with run()
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private SparseArray<ArrayList<Message>> incoming = new SparseArray<ArrayList<Message>>();

    @Override
    public void run() {
        Socket socket = null;
        if (file == null)
            try {
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress("127.0.0.1", Integer
                        .parseInt(sampleView.actionPort.getText())));
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                running = false;
                Error(e);
            }
        else
            dis = new DataInputStream(file);

        while (running) {
            try {
                if (file != null && file.available() == 0) {
                    running = false;
                    break;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                assert false;
            }

            Message msg = null;
            if (incoming.size() > 0) { // find queued incoming
                for (int i = 0; i < incoming.size(); i++) {
                    final ArrayList<Message> messages = incoming.valueAt(i);
                    if (messages.size() > 0) {
                        msg = messages.remove(0);
                        break;
                    }
                }
            }
            try {
                if (null == msg) // get incoming from network
                    msg = receiveMessage(dis);
                processMessage(dos, msg);
            } catch (IOException e) {
                Error(e);
                running = false;
                break;
            }
        }

        try {
            if (socket != null)
                socket.close();
            else
                file.close();
        } catch (IOException e) {
            Error(e);
            running = false;
        }

    }

    private void putMessage(final Message msg) {
        ArrayList<Message> existing = incoming.get(msg.getContextId());
        if (existing == null)
            incoming.put(msg.getContextId(), existing = new ArrayList<Message>());
        existing.add(msg);
    }

    Message receiveMessage(final int contextId) throws IOException {
        Message msg = receiveMessage(dis);
        while (msg.getContextId() != contextId) {
            putMessage(msg);
            msg = receiveMessage(dis);
        }
        return msg;
    }

    void sendMessage(final Message msg) throws IOException {
        sendMessage(dos, msg);
    }

    // should only be used by DefaultProcessMessage
    private SparseArray<Message> partials = new SparseArray<Message>();

    Message getPartialMessage(final int contextId) {
        return partials.get(contextId);
    }

    // used to add BeforeCall to complete if it was skipped
    void completePartialMessage(final int contextId) {
        final Message msg = partials.get(contextId);
        partials.remove(contextId);
        assert msg != null;
        assert msg.getType() == Type.BeforeCall;
        if (msg != null)
            synchronized (complete) {
                complete.add(msg);
            }
    }

    // can be used by other message processor as default processor
    void defaultProcessMessage(final Message msg, boolean expectResponse,
            boolean sendResponse) throws IOException {
        final int contextId = msg.getContextId();
        if (msg.getType() == Type.BeforeCall) {
            if (sendResponse) {
                final Message.Builder builder = Message.newBuilder();
                builder.setContextId(contextId);
                builder.setType(Type.Response);
                builder.setExpectResponse(expectResponse);
                builder.setFunction(Function.CONTINUE);
                sendMessage(dos, builder.build());
            }
            assert partials.indexOfKey(contextId) < 0;
            partials.put(contextId, msg);
        } else if (msg.getType() == Type.AfterCall) {
            if (sendResponse) {
                final Message.Builder builder = Message.newBuilder();
                builder.setContextId(contextId);
                builder.setType(Type.Response);
                builder.setExpectResponse(expectResponse);
                builder.setFunction(Function.SKIP);
                sendMessage(dos, builder.build());
            }
            assert partials.indexOfKey(contextId) >= 0;
            final Message before = partials.get(contextId);
            partials.remove(contextId);
            assert before.getFunction() == msg.getFunction();
            final Message completed = before.toBuilder().mergeFrom(msg)
                    .setType(Type.CompleteCall).build();
            synchronized (complete) {
                complete.add(completed);
            }
        } else if (msg.getType() == Type.CompleteCall) {
            // this type should only be encountered on client after processing
            assert file != null;
            assert !msg.getExpectResponse();
            assert !sendResponse;
            assert partials.indexOfKey(contextId) < 0;
            synchronized (complete) {
                complete.add(msg);
            }
        } else if (msg.getType() == Type.Response && msg.getFunction() == Function.SETPROP) {
            synchronized (complete) {
                complete.add(msg);
            }
        } else
            assert false;
    }

    public Message removeCompleteMessage(int contextId) {
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

    private Message receiveMessage(final DataInputStream dis)
            throws IOException {
        int len = 0;
        try {
            len = dis.readInt();
            if (byteOrder == ByteOrder.LITTLE_ENDIAN)
                len = Integer.reverseBytes(len); // readInt reads BIT_ENDIAN
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
        sendCommands(msg.getContextId());
        return msg;
    }

    private void sendMessage(final DataOutputStream dos, final Message message)
            throws IOException {
        if (dos == null)
            return;
        assert message.getFunction() != Function.NEG;
        final byte[] data = message.toByteArray();
        if (byteOrder == ByteOrder.BIG_ENDIAN)
            dos.writeInt(data.length);
        else
            dos.writeInt(Integer.reverseBytes(data.length));
        dos.write(data);
    }

    private void processMessage(final DataOutputStream dos, final Message msg) throws IOException {
        if (msg.getExpectResponse()) {
            assert dos != null; // readonly source cannot expectResponse
            for (ProcessMessage process : processes)
                if (process.processMessage(this, msg))
                    return;
            defaultProcessMessage(msg, msg.getExpectResponse(), msg.getExpectResponse());
        } else
            defaultProcessMessage(msg, msg.getExpectResponse(), msg.getExpectResponse());
    }

    void Error(Exception e) {
        sampleView.showError(e);
    }
}
