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

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

public class MessageData {
    public final Message msg;
    private Image image = null; // texture
    public String shader = null; // shader source
    public String text;
    public String[] columns = new String[3];

    float[][] attribs = null;
    short[] indices;

    public MessageData(final Device device, final Message msg, final Context context) {
        this.msg = msg;
        StringBuilder builder = new StringBuilder();
        final Function function = msg.getFunction();
        if (function != Message.Function.ACK && msg.getType() != Type.BeforeCall)
            assert msg.hasTime();
        builder.append(columns[0] = function.name());
        while (builder.length() < 30)
            builder.append(' ');
        columns[1] = String.format("%.3f", msg.getTime());
        if (msg.hasClock())
            columns[1] += String.format(":%.3f", msg.getClock());
        builder.append(columns[1]);

        builder.append("  ");
        builder.append(String.format("0x%08X", msg.getContextId()));
        builder.append("  ");
        columns[2] = "";
        if (msg.getType() == Type.BeforeCall) // incomplete call, client SKIPPED
            columns[2] = "[BeforeCall(AfterCall missing)] ";
        else if (msg.getType() == Type.AfterGeneratedCall)
            columns[2] = "[AfterGeneratedCall] ";
        else
            assert msg.getType() == Type.CompleteCall;
        columns[2] += MessageFormatter.format(msg, false);
        builder.append(columns[2]);
        switch (function) {
            case glDrawArrays:
                if (!msg.hasArg7())
                    break;
                context.serverVertex.glDrawArrays(this);
                break;
            case glDrawElements:
                if (!msg.hasArg7())
                    break;
                context.serverVertex.glDrawElements(this);
                break;
            case glShaderSource:
                shader = msg.getData().toStringUtf8();
                break;

        }
        text = builder.toString();
    }

    public Image getImage() {
        if (image != null)
            return image;
        ImageData imageData = null;
        switch (msg.getFunction()) {
            case glTexImage2D:
                if (!msg.hasData())
                    return null;
                imageData = MessageProcessor.receiveImage(msg.getArg3(), msg
                        .getArg4(), msg.getArg6(), msg.getArg7(), msg.getData());
                return image = new Image(Display.getCurrent(), imageData);
            case glTexSubImage2D:
                assert msg.hasData();
                imageData = MessageProcessor.receiveImage(msg.getArg4(), msg
                        .getArg5(), msg.getArg6(), msg.getArg7(), msg.getData());
                return image = new Image(Display.getCurrent(), imageData);
            case glCopyTexImage2D:
                imageData = MessageProcessor.receiveImage(msg.getArg5(), msg.getArg6(),
                        msg.getPixelFormat(), msg.getPixelType(), msg.getData());
                imageData = imageData.scaledTo(imageData.width, -imageData.height);
                return image = new Image(Display.getCurrent(), imageData);
            case glCopyTexSubImage2D:
                imageData = MessageProcessor.receiveImage(msg.getArg6(), msg.getArg7(),
                        msg.getPixelFormat(), msg.getPixelType(), msg.getData());
                imageData = imageData.scaledTo(imageData.width, -imageData.height);
                return image = new Image(Display.getCurrent(), imageData);
            case glReadPixels:
                if (!msg.hasData())
                    return null;
                imageData = MessageProcessor.receiveImage(msg.getArg2(), msg.getArg3(),
                        msg.getArg4(), msg.getArg5(), msg.getData());
                imageData = imageData.scaledTo(imageData.width, -imageData.height);
                return image = new Image(Display.getCurrent(), imageData);
            case eglSwapBuffers:
                if (!msg.hasData())
                    return null;
                imageData = MessageProcessor.receiveImage(msg.getImageWidth(),
                        msg.getImageHeight(), msg.getPixelFormat(), msg.getPixelType(),
                        msg.getData());
                imageData = imageData.scaledTo(imageData.width, -imageData.height);
                return image = new Image(Display.getCurrent(), imageData);
            default:
                return null;
        }
    }
}
