/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.vdmdemo.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Simple message exchange framework between the client and the host. */
@Singleton
public class RemoteIo {
    public static final String TAG = "VdmRemoteIo";

    interface StreamClosedCallback {
        void onStreamClosed();
    }

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private OutputStream mOutputStream = null;

    private StreamClosedCallback mOutputStreamClosedCallback = null;
    private final Handler mSendMessageHandler;

    @GuardedBy("mMessageConsumers")
    private final Map<Object, MessageConsumer> mMessageConsumers = new ArrayMap<>();

    @Inject
    RemoteIo() {
        final HandlerThread sendMessageThread = new HandlerThread("SendMessageThread");
        sendMessageThread.start();
        mSendMessageHandler = new Handler(sendMessageThread.getLooper());
    }

    @SuppressWarnings("ThreadPriorityCheck")
    void initialize(InputStream inputStream, StreamClosedCallback inputStreamClosedCallback) {
        Thread t = new Thread(new ReceiverRunnable(inputStream, inputStreamClosedCallback));
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    void initialize(
            OutputStream outputStream, StreamClosedCallback outputStreamClosedCallback) {
        synchronized (mLock) {
            mOutputStream = outputStream;
            mOutputStreamClosedCallback = outputStreamClosedCallback;
        }
    }

    /** Registers a consumer for processing events coming from the remote device. */
    public void addMessageConsumer(Consumer<RemoteEvent> consumer) {
        synchronized (mMessageConsumers) {
            mMessageConsumers.put(consumer, new MessageConsumer(consumer));
        }
    }

    /** Unregisters a previously registered message consumer. */
    public void removeMessageConsumer(Consumer<RemoteEvent> consumer) {
        synchronized (mMessageConsumers) {
            if (mMessageConsumers.remove(consumer) == null) {
                Log.w(TAG, "Failed to remove message consumer.");
            }
        }
    }

    /** Sends an event to the remote device. */
    public void sendMessage(RemoteEvent event) {
        synchronized (mLock) {
            if (mOutputStream == null) {
                Log.e(TAG, "Failed to send event, RemoteIO not initialized.");
                return;
            }
        }
        mSendMessageHandler.post(() -> {
            synchronized (mLock) {
                try {
                    event.writeDelimitedTo(mOutputStream);
                    mOutputStream.flush();
                } catch (IOException e) {
                    mOutputStream = null;
                    mOutputStreamClosedCallback.onStreamClosed();
                }
            }
        });
    }

    private class ReceiverRunnable implements Runnable {

        private final InputStream mInputStream;
        private final StreamClosedCallback mInputStreamClosedCallback;

        ReceiverRunnable(InputStream inputStream, StreamClosedCallback inputStreamClosedCallback) {
            mInputStream = inputStream;
            mInputStreamClosedCallback = inputStreamClosedCallback;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    RemoteEvent event = RemoteEvent.parseDelimitedFrom(mInputStream);
                    if (event == null) {
                        break;
                    }
                    synchronized (mMessageConsumers) {
                        mMessageConsumers.values().forEach(consumer -> consumer.accept(event));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain event: " + e);
            }
            mInputStreamClosedCallback.onStreamClosed();
        }
    }

    private static class MessageConsumer {
        private final Executor mExecutor;
        private final Consumer<RemoteEvent> mConsumer;

        MessageConsumer(Consumer<RemoteEvent> consumer) {
            mExecutor = Executors.newSingleThreadExecutor();
            mConsumer = consumer;
        }

        public void accept(RemoteEvent event) {
            mExecutor.execute(() -> mConsumer.accept(event));
        }
    }
}
