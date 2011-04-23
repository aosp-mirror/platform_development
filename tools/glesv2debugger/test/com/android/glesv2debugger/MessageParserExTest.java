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

import com.android.glesv2debugger.DebuggerMessage.Message;
import com.android.glesv2debugger.DebuggerMessage.Message.Function;
import com.android.glesv2debugger.DebuggerMessage.Message.Type;
import com.google.protobuf.ByteString;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

public class MessageParserExTest {
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testParseFloats() {
        final MessageParserEx parser = new MessageParserEx();
        final String args = "{0, 1    ,2,3  }";
        parser.args = args;
        final ByteBuffer data = parser.parseFloats(4).asReadOnlyByteBuffer();
        data.order(SampleView.targetByteOrder);
        for (int i = 0; i < 4; i++)
            assertEquals(i, data.getFloat(), 0);
    }

    @Test
    public void testParseArgument() {
        final MessageParserEx parser = new MessageParserEx();
        final String args = "sdfa   =  GL_VERTEX_SHADER , -5421 ,0x443=0x54f";
        parser.args = args;
        assertEquals(GLEnum.GL_VERTEX_SHADER.value, parser.parseArgument());
        assertEquals(-5421, parser.parseArgument());
        assertEquals(0x54f, parser.parseArgument());
    }

    /**
     * Test method for
     * {@link com.android.glesv2debugger.MessageParserEx#parse_glShaderSource(com.android.glesv2debugger.DebuggerMessage.Message.Builder)}
     * .
     */
    @Test
    public void testParse_glShaderSource() {
        final Message.Builder builder = Message.newBuilder();
        final MessageParserEx messageParserEx = new MessageParserEx();
        final String source = "dks \n jafhskjaho { urehg ; } hskjg";
        messageParserEx.parse(builder, "void glShaderSource ( shader=4, count= 1, "
                                + "string =\"" + source + "\"  , 0x0)");
        assertEquals(Function.glShaderSource, builder.getFunction());
        assertEquals(4, builder.getArg0());
        assertEquals(1, builder.getArg1());
        assertEquals(source, builder.getData().toStringUtf8());
        assertEquals(0, builder.getArg3());
    }

    @Test
    public void testParse_glBlendEquation() {
        assertNotNull(MessageParserEx.instance);
        final Message.Builder builder = Message.newBuilder();
        MessageParserEx.instance.parse(builder, "void glBlendEquation ( mode= GL_ADD ) ; ");
        assertEquals(Function.glBlendEquation, builder.getFunction());
        assertEquals(GLEnum.GL_ADD.value, builder.getArg0());
    }

    /** loopback testing of typical generated MessageFormatter and MessageParser */
    @Test
    public void testParseFormatterMessage() {
        final ByteBuffer srcData = ByteBuffer.allocate(4 * 2 * 4);
        srcData.order(SampleView.targetByteOrder);
        for (int i = 0; i < 4 * 2; i++)
            srcData.putFloat(i);
        srcData.rewind();
        Message.Builder builder = Message.newBuilder();
        builder.setContextId(3752).setExpectResponse(false).setType(Type.CompleteCall);
        builder.setFunction(Function.glUniformMatrix2fv);
        builder.setArg0(54).setArg1(2).setArg2(0).setData(ByteString.copyFrom(srcData));
        Message msg = builder.build();
        builder = msg.toBuilder();
        String formatted = MessageFormatter.format(msg, false);
        formatted = formatted.substring(0, formatted.indexOf('(')) + ' ' + builder.getFunction() +
                formatted.substring(formatted.indexOf('('));
        Message.Builder parsed = Message.newBuilder();
        MessageParserEx.instance.parse(parsed, formatted);
        assertEquals(builder.getFunction(), parsed.getFunction());
        assertEquals(builder.getArg0(), parsed.getArg0());
        assertEquals(builder.getArg1(), parsed.getArg1());
        assertEquals(builder.getArg2(), parsed.getArg2());
        assertEquals(builder.getData().toStringUtf8(), parsed.getData().toStringUtf8());
    }

}
