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
import com.android.glesv2debugger.DebuggerMessage.Message.DataType;
import com.android.glesv2debugger.DebuggerMessage.Message.Function;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class MessageData {
    public final Message msg;
    public Image image; // texture
    public String shader; // shader source
    public String text;
    public float[] data;
    public int maxAttrib; // used for formatting data
    public GLEnum dataType; // could be float, int; mainly for formatting use

    public MessageData(final Device device, final Message msg, final Context context) {
        this.msg = msg;
        image = null;
        shader = null;
        data = null;
        StringBuilder builder = new StringBuilder();
        final Function function = msg.getFunction();
        ImageData imageData = null;
        if (function != Message.Function.ACK)
            assert msg.hasTime();
        builder.append(function);
        while (builder.length() < 30)
            builder.append(' ');
        builder.append(String.format("%.3f", msg.getTime()));
        if (msg.hasClock())
            builder.append(String.format(":%.3f", msg.getClock()));
        builder.append(String.format("  0x%08X", msg.getContextId()));
        builder.append("  ");
        builder.append(MessageFormatter.Format(msg));
        switch (function) {
            case glDrawArrays: // msg was modified by GLServerVertex
            case glDrawElements:
                if (!msg.hasArg8() || !msg.hasData())
                    break;
                dataType = GLEnum.valueOf(msg.getArg8());
                maxAttrib = msg.getArg7();
                data = MessageProcessor.ReceiveData(dataType, msg.getData());
                break;
            case glShaderSource:
                shader = msg.getData().toStringUtf8();
                break;
            case glTexImage2D:
                if (!msg.hasData())
                    break;
                imageData = MessageProcessor.ReceiveImage(msg.getArg3(), msg
                        .getArg4(), msg.getArg6(), msg.getArg7(), msg.getData()
                        .toByteArray());
                if (null == imageData)
                    break;
                image = new Image(device, imageData);
                break;
            case glTexSubImage2D:
                assert msg.hasData();
                imageData = MessageProcessor.ReceiveImage(msg.getArg4(), msg
                        .getArg5(), msg.getArg6(), msg.getArg7(), msg.getData()
                        .toByteArray());
                if (null == imageData)
                    break;
                image = new Image(device, imageData);
                break;
            case glCopyTexImage2D:
                imageData = MessageProcessor.ReceiveImage(msg.getArg5(), msg.getArg6(),
                        GLEnum.GL_RGBA.value, GLEnum.GL_UNSIGNED_BYTE.value, msg.getData()
                                .toByteArray());
                image = new Image(device, imageData);
                break;
            case glCopyTexSubImage2D:
                imageData = MessageProcessor.ReceiveImage(msg.getArg6(), msg.getArg7(),
                        GLEnum.GL_RGBA.value, GLEnum.GL_UNSIGNED_BYTE.value, msg.getData()
                                .toByteArray());
                image = new Image(device, imageData);
                break;
            case glReadPixels:
                if (!msg.hasData())
                    break;
                if (msg.getDataType() == DataType.ReferencedImage)
                    MessageProcessor.ref = context.readPixelRef;
                imageData = MessageProcessor.ReceiveImage(msg.getArg2(), msg.getArg3(),
                        msg.getArg4(), msg.getArg5(), msg.getData().toByteArray());
                context.readPixelRef = MessageProcessor.ref;
                MessageProcessor.ref = null;
                imageData = imageData.scaledTo(imageData.width, -imageData.height);
                image = new Image(device, imageData);
                break;
        }
        text = builder.toString();
    }
}
