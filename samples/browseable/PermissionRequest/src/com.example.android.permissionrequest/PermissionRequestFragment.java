/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.permissionrequest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.example.android.common.logger.Log;

/**
 * This fragment shows a {@link WebView} and loads a web app from the {@link SimpleWebServer}.
 */
public class PermissionRequestFragment extends Fragment
        implements ConfirmationDialogFragment.Listener {

    private static final String TAG = PermissionRequestFragment.class.getSimpleName();

    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * We use this web server to serve HTML files in the assets folder. This is because we cannot
     * use the JavaScript method "getUserMedia" from "file:///android_assets/..." URLs.
     */
    private SimpleWebServer mWebServer;

    /**
     * A reference to the {@link WebView}.
     */
    private WebView mWebView;

    /**
     * This field stores the {@link PermissionRequest} from the web application until it is allowed
     * or denied by user.
     */
    private PermissionRequest mPermissionRequest;

    /**
     * For testing.
     */
    private ConsoleMonitor mConsoleMonitor;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permission_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mWebView = (WebView) view.findViewById(R.id.web_view);
        // Here, we use #mWebChromeClient with implementation for handling PermissionRequests.
        mWebView.setWebChromeClient(mWebChromeClient);
        configureWebSettings(mWebView.getSettings());
    }

    @Override
    public void onResume() {
        super.onResume();
        final int port = 8080;
        mWebServer = new SimpleWebServer(port, getResources().getAssets());
        mWebServer.start();
        mWebView.loadUrl("http://localhost:" + port + "/sample.html");
    }

    @Override
    public void onPause() {
        mWebServer.stop();
        super.onPause();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
    }

    /**
     * This {@link WebChromeClient} has implementation for handling {@link PermissionRequest}.
     */
    private WebChromeClient mWebChromeClient = new WebChromeClient() {

        // This method is called when the web content is requesting permission to access some
        // resources.
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            Log.i(TAG, "onPermissionRequest");
            mPermissionRequest = request;
            ConfirmationDialogFragment.newInstance(request.getResources())
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }

        // This method is called when the permission request is canceled by the web content.
        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            Log.i(TAG, "onPermissionRequestCanceled");
            // We dismiss the prompt UI here as the request is no longer valid.
            mPermissionRequest = null;
            DialogFragment fragment = (DialogFragment) getChildFragmentManager()
                    .findFragmentByTag(FRAGMENT_DIALOG);
            if (null != fragment) {
                fragment.dismiss();
            }
        }

        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage message) {
            switch (message.messageLevel()) {
                case TIP:
                    Log.v(TAG, message.message());
                    break;
                case LOG:
                    Log.i(TAG, message.message());
                    break;
                case WARNING:
                    Log.w(TAG, message.message());
                    break;
                case ERROR:
                    Log.e(TAG, message.message());
                    break;
                case DEBUG:
                    Log.d(TAG, message.message());
                    break;
            }
            if (null != mConsoleMonitor) {
                mConsoleMonitor.onConsoleMessage(message);
            }
            return true;
        }

    };

    @Override
    public void onConfirmation(boolean allowed) {
        if (allowed) {
            mPermissionRequest.grant(mPermissionRequest.getResources());
            Log.d(TAG, "Permission granted.");
        } else {
            mPermissionRequest.deny();
            Log.d(TAG, "Permission request denied.");
        }
        mPermissionRequest = null;
    }

    public void setConsoleMonitor(ConsoleMonitor monitor) {
        mConsoleMonitor = monitor;
    }

    /**
     * For testing.
     */
    public interface ConsoleMonitor {
        public void onConsoleMessage(ConsoleMessage message);
    }

}
