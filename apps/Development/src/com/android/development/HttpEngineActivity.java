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

package com.android.development;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.http.HttpEngine;
import android.net.http.HttpException;
import android.net.http.UploadDataProvider;
import android.net.http.UploadDataSink;
import android.net.http.UrlRequest;
import android.net.http.UrlResponseInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Activity for managing HttpEngine interactions.
 */
public class HttpEngineActivity extends Activity {
    private static final String TAG = HttpEngineActivity.class.getSimpleName();

    private HttpEngine mHttpEngine;

    private String mUrl;
    private TextView mResultText;
    private TextView mReceiveDataText;

    class SimpleUrlRequestCallback implements UrlRequest.Callback {
        private ByteArrayOutputStream mBytesReceived = new ByteArrayOutputStream();
        private WritableByteChannel mReceiveChannel = Channels.newChannel(mBytesReceived);

        @Override
        public void onRedirectReceived(
                UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            Log.i(TAG, "****** onRedirectReceived ******");
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "****** Response Started ******");
            Log.i(TAG, "*** Headers Are *** " + info.getHeaders());

            request.read(ByteBuffer.allocateDirect(32 * 1024));
        }

        @Override
        public void onReadCompleted(
                UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            byteBuffer.flip();
            Log.i(TAG, "****** onReadCompleted ******" + byteBuffer);

            try {
                mReceiveChannel.write(byteBuffer);
            } catch (IOException e) {
                Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            }
            byteBuffer.clear();
            request.read(byteBuffer);
        }

        @Override
        public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "****** Request Completed, status code is " + info.getHttpStatusCode()
                            + ", total received bytes is " + info.getReceivedByteCount());

            final String receivedData = mBytesReceived.toString();
            final String url = info.getUrl();
            final String text = "Completed " + url + " (" + info.getHttpStatusCode() + ")";
            HttpEngineActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResultText.setText(text);
                    mReceiveDataText.setText(receivedData);
                    promptForURL(url);
                }
            });
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, HttpException error) {
            Log.i(TAG, "****** onFailed, error is: " + error.getMessage());

            final String url = mUrl;
            final String text = "Failed " + mUrl + " (" + error.getMessage() + ")";
            HttpEngineActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResultText.setText(text);
                    promptForURL(url);
                }
            });
        }

        @Override
        public void onCanceled(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "****** onCanceled ******");
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.http_engine_activity);
        mResultText = (TextView) findViewById(R.id.resultView);
        mReceiveDataText = (TextView) findViewById(R.id.dataView);
        mReceiveDataText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptForURL(mUrl);
            }
        });

        HttpEngine.Builder myBuilder = new HttpEngine.Builder(this);
        myBuilder.setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_IN_MEMORY, 100 * 1024)
                .setEnableHttp2(true)
                .setEnableQuic(true);

        mHttpEngine = myBuilder.build();

        String appUrl = (getIntent() != null ? getIntent().getDataString() : null);
        if (appUrl == null) {
            promptForURL("https://");
        } else {
            startWithURL(appUrl);
        }
    }

    private void promptForURL(String url) {
        Log.i(TAG, "No URL provided via intent, prompting user...");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Enter a URL");
        LayoutInflater inflater = getLayoutInflater();
        View alertView = inflater.inflate(R.layout.http_engine_dialog, null);
        final EditText urlInput = (EditText) alertView.findViewById(R.id.urlText);
        urlInput.setText(url);
        final EditText postInput = (EditText) alertView.findViewById(R.id.postText);
        alert.setView(alertView);

        alert.setPositiveButton("Load", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int button) {
                String url = urlInput.getText().toString();
                String postData = postInput.getText().toString();
                startWithURL(url, postData);
            }
        });
        alert.show();
    }

    private void applyPostDataToUrlRequestBuilder(
            UrlRequest.Builder builder, Executor executor, String postData) {
        if (postData != null && postData.length() > 0) {
            builder.setHttpMethod("POST");
            builder.addHeader("Content-Type", "application/x-www-form-urlencoded");
            // TODO: make android.net.http.apihelpers.UploadDataProviders accessible.
            builder.setUploadDataProvider(new UploadDataProvider() {
                @Override
                public long getLength() {
                    return postData.length();
                }

                @Override
                public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
                    byteBuffer.put(postData.getBytes());
                    uploadDataSink.onReadSucceeded(/*finalChunk*/ false);
                }

                @Override
                public void rewind(UploadDataSink uploadDataSink) {
                    // noop
                    uploadDataSink.onRewindSucceeded();
                }
            }, executor);
        }
    }

    private void startWithURL(String url) {
        startWithURL(url, null);
    }

    private void startWithURL(String url, String postData) {
        Log.i(TAG, "UrlRequest started: " + url);
        mUrl = url;

        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Callback callback = new SimpleUrlRequestCallback();
        UrlRequest.Builder builder = mHttpEngine.newUrlRequestBuilder(url, executor, callback);
        applyPostDataToUrlRequestBuilder(builder, executor, postData);
        builder.build().start();
    }
}
