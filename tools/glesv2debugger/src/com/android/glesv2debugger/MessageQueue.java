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

public class MessageQueue implements Runnable {

    private boolean running = false;
    private ByteOrder byteOrder;
    private FileInputStream file; // if null, create and use socket
    private Thread thread = null;
    private ArrayList<Message> complete = new ArrayList<Message>(); // synchronized
    private ArrayList<Message> commands = new ArrayList<Message>(); // synchronized
    private SampleView sampleView;

    public MessageQueue(SampleView sampleView) {
        this.sampleView = sampleView;
    }

    public void Start(final ByteOrder byteOrder, final FileInputStream file) {
        if (running)
            return;
        running = true;
        this.byteOrder = byteOrder;
        this.file = file;
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

    private void SendCommands(final int contextId) throws IOException {
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
                    msg = ReceiveMessage(dis);
                ProcessMessage(dos, msg);
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

    // should only be used by DefaultProcessMessage
    private SparseArray<Message> partials = new SparseArray<Message>();

    Message GetPartialMessage(final int contextId) {
        return partials.get(contextId);
    }

    // used to add BeforeCall to complete if it was skipped
    void CompletePartialMessage(final int contextId) {
        final Message msg = partials.get(contextId);
        partials.remove(contextId);
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
        if (msg.getType() == Type.BeforeCall) {
            if (sendResponse) {
                final Message.Builder builder = Message.newBuilder();
                builder.setContextId(contextId);
                builder.setType(Type.Response);
                builder.setExpectResponse(expectResponse);
                builder.setFunction(Function.CONTINUE);
                SendMessage(dos, builder.build());
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
                SendMessage(dos, builder.build());
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
        SendCommands(msg.getContextId());
        return msg;
    }

    private void SendMessage(final DataOutputStream dos, final Message message)
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

    private void ProcessMessage(final DataOutputStream dos, final Message msg) throws IOException {
        if (msg.getExpectResponse()) {
            assert file == null; // file cannot be interactive mode
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
