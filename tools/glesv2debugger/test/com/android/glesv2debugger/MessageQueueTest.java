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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.android.glesv2debugger.DebuggerMessage.Message;
import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.glesv2debugger.DebuggerMessage.Message.Type;

import org.junit.Before;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class MessageQueueTest {
    private MessageQueue queue;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        queue = new MessageQueue(null, new ProcessMessage[0]);
    }

    /**
     * Test method for
     * {@link com.android.glesv2debugger.MessageQueue#defaultProcessMessage(com.android.glesv2debugger.DebuggerMessage.Message, boolean, boolean)}
     * .
     * 
     * @throws IOException
     */
    @Test
    public void testDefaultProcessMessage() throws IOException {
        final int contextId = 8784;
        assertNull(queue.getPartialMessage(contextId));
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setExpectResponse(false);
        builder.setFunction(Function.glFinish);
        builder.setType(Type.BeforeCall);
        Message msg = builder.build();
        queue.defaultProcessMessage(msg, false, false);
        assertNotNull(queue.getPartialMessage(contextId));

        builder = msg.toBuilder();
        builder.setType(Type.AfterCall);
        builder.setTime(5);
        msg = builder.build();
        queue.defaultProcessMessage(msg, false, false);
        assertNull(queue.getPartialMessage(contextId));
        Message complete = queue.removeCompleteMessage(contextId);
        assertNotNull(complete);
        assertEquals(contextId, complete.getContextId());
        assertEquals(msg.getFunction(), complete.getFunction());
        assertEquals(msg.getTime(), complete.getTime(), 0);
        assertEquals(Type.CompleteCall, complete.getType());

        // an already complete message should just be added to complete queue
        queue.defaultProcessMessage(complete, false, false);
        assertNull(queue.getPartialMessage(contextId));
        complete = queue.removeCompleteMessage(contextId);
        assertNotNull(complete);
        assertEquals(contextId, complete.getContextId());
        assertEquals(msg.getFunction(), complete.getFunction());
        assertEquals(msg.getTime(), complete.getTime(), 0);
        assertEquals(Type.CompleteCall, complete.getType());
    }

    @Test
    public void testCompletePartialMessage() throws IOException {
        final int contextId = 8784;
        assertNull(queue.getPartialMessage(contextId));
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(contextId);
        builder.setExpectResponse(false);
        builder.setFunction(Function.glFinish);
        builder.setType(Type.BeforeCall);
        Message msg = builder.build();
        queue.defaultProcessMessage(msg, false, false);
        assertNotNull(queue.getPartialMessage(contextId));
        queue.completePartialMessage(contextId);

        final Message complete = queue.removeCompleteMessage(contextId);
        assertNotNull(complete);
        assertEquals(contextId, complete.getContextId());
        assertEquals(msg.getFunction(), complete.getFunction());
        assertEquals(msg.getTime(), complete.getTime(), 0);
        assertEquals(Type.BeforeCall, complete.getType());
    }

    /** Write two messages from two contexts to file and test handling them */
    @Test
    public void testRunWithFile() throws FileNotFoundException, IOException, InterruptedException {
        final File filePath = File.createTempFile("test", ".gles2dbg");
        DataOutputStream file = new DataOutputStream(new FileOutputStream(filePath));
        Message.Builder builder = Message.newBuilder();
        final int contextId0 = 521643, contextId1 = 87634;
        assertNull(queue.removeCompleteMessage(contextId0));
        assertNull(queue.removeCompleteMessage(contextId1));

        builder.setContextId(contextId0).setExpectResponse(false).setType(Type.BeforeCall);
        builder.setFunction(Function.glClear).setArg0(contextId0);
        Message msg0 = builder.build();
        byte[] data = msg0.toByteArray();
        file.writeInt(data.length);
        file.write(data);

        builder = Message.newBuilder();
        builder.setContextId(contextId1).setExpectResponse(false).setType(Type.BeforeCall);
        builder.setFunction(Function.glDisable).setArg0(contextId1);
        Message msg1 = builder.build();
        data = msg1.toByteArray();
        file.writeInt(data.length);
        file.write(data);

        builder = Message.newBuilder();
        msg0 = builder.setContextId(msg0.getContextId()).setExpectResponse(false)
                .setType(Type.AfterCall).setFunction(msg0.getFunction()).setTime(2).build();
        data = msg0.toByteArray();
        file.writeInt(data.length);
        file.write(data);

        builder = Message.newBuilder();
        msg1 = builder.setContextId(msg1.getContextId()).setExpectResponse(false)
                .setType(Type.AfterCall).setFunction(msg1.getFunction()).setTime(465).build();
        data = msg1.toByteArray();
        file.writeInt(data.length);
        file.write(data);

        file.close();

        FileInputStream fis = new FileInputStream(filePath);
        // Java VM uses big endian, so the file was written in big endian
        queue.start(ByteOrder.BIG_ENDIAN, fis);
        queue.thread.join();

        Message complete0 = queue.removeCompleteMessage(msg0.getContextId());
        assertNotNull(complete0);
        assertNull(queue.removeCompleteMessage(contextId0));
        assertEquals(contextId0, complete0.getContextId());
        assertEquals(false, complete0.getExpectResponse());
        assertEquals(Type.CompleteCall, complete0.getType());
        assertEquals(msg0.getFunction(), complete0.getFunction());
        assertEquals(contextId0, complete0.getArg0());
        assertEquals(msg0.getTime(), complete0.getTime(), 0);

        Message complete1 = queue.removeCompleteMessage(msg1.getContextId());
        assertNotNull(complete1);
        assertNull(queue.removeCompleteMessage(contextId1));
        assertEquals(contextId1, complete1.getContextId());
        assertEquals(false, complete1.getExpectResponse());
        assertEquals(Type.CompleteCall, complete1.getType());
        assertEquals(msg1.getFunction(), complete1.getFunction());
        assertEquals(contextId1, complete1.getArg0());
        assertEquals(msg1.getTime(), complete1.getTime(), 0);

        filePath.delete();
    }
}
