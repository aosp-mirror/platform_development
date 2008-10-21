/*
** Copyright 2007, The Android Open Source Project
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

package com.android.development;

import static com.android.internal.util.CharSequences.forAsciiBytes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.net.Socket;

import android.app.Activity;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Typeface;
import android.view.Gravity;

/**
 * Views the device log.
 */
public class LogViewer extends Activity {

    static final String TAG = LogViewer.class.getSimpleName();

    FileOutputStream logger;

    volatile boolean active = true;
    Handler handler;
    LogTextBox text;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.log_viewer);
        this.handler = new Handler();

        text = (LogTextBox) findViewById(R.id.text);

        text.setTextSize(10);
        text.setHorizontallyScrolling(true);
        text.setTypeface(Typeface.MONOSPACE);
        text.setGravity(Gravity.BOTTOM | Gravity.LEFT);

        this.active = true;
        try {
            logger = new FileOutputStream("/tmp/logviewer.txt");
            new Thread(new LogReader()).start();
        } catch (IOException e) {
            appendThrowable(e);
        }
    }

    private void appendThrowable(Throwable t) {
        StringBuilder builder = new StringBuilder();
        builder.append("Error reading log: ");
        builder.append(Log.getStackTraceString(t));
        text.getText().append(builder);
    }

    private class LogReader implements Runnable {

        final Socket socket;
        final DataInputStream in;
        StringBuilder builder = new StringBuilder();
        long lastTime = System.currentTimeMillis();

        private static final int HEADER_SIZE = 24;

        public LogReader() throws IOException {
            this.socket = new Socket("127.0.0.1", 5040);
            this.in = new DataInputStream(this.socket.getInputStream());
            // Write two newlines to indicate "no reader args"
            this.socket.getOutputStream().write('\n');
            this.socket.getOutputStream().write('\n');
        }

        public void run() {
            while (active) {
                try {
                    while (in.available() > 0) {
                        logger.write("Reading message.\n".getBytes());

                        int length = in.readInt();
                        byte[] bytes = new byte[length];
                        in.readFully(bytes);

                        int tagEnd = next0(bytes, HEADER_SIZE);
                        int fileEnd = next0(bytes, tagEnd + 1);
                        int messageEnd = next0(bytes, fileEnd + 1);

                        CharSequence tag
                                = forAsciiBytes(bytes, HEADER_SIZE, tagEnd);
                        CharSequence message
                                = forAsciiBytes(bytes, fileEnd + 1, messageEnd);

                        builder.append(tag)
                                .append(": ")
                                .append(message)
                                .append("\n");
                    }

                    logger.write("Updating UI.\n".getBytes());
                    handler.post(new AppendCharacters(builder));
                    builder = new StringBuilder();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                } catch (final IOException e) {
                    handler.post(new AppendThrowable(e));
                }
            }
        }
    }

    static int next0(byte[] bytes, int start) {
        for (int current = start; current < bytes.length; current++) {
            if (bytes[current] == 0)
                return current;
        }
        return bytes.length;
    }

    private class AppendThrowable implements Runnable {

        private final Throwable t;

        public AppendThrowable(Throwable t) {
            this.t = t;
        }

        public void run() {
            appendThrowable(t);
        }
    }

    private class AppendCharacters implements Runnable {

        private final CharSequence message;

        public AppendCharacters(CharSequence message) {
            this.message = message;
        }

        public void run() {
            text.getText().append(message);
//            try {
//                logger.write(builder.toString().getBytes());
//            } catch (IOException e) {
//                appendThrowable(e);
//            }
        }
    }
}
