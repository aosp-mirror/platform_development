/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.apis.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.android.apis.R;

/**
 * This class demonstrates how to implement HTML content printing
 * from a {@link WebView} which is shown on the screen.
 * <p>
 * This activity shows a simple HTML content in a {@link WebView}
 * and allows the user to print that content via an action in the
 * action bar. The shown {@link WebView} is doing the printing.
 * </p>
 *
 * @see PrintManager
 * @see WebView
 */
public class PrintHtmlFromScreen extends Activity {

    private WebView mWebView;

    private boolean mDataLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_html_from_screen);
        mWebView = (WebView) findViewById(R.id.web_view);

        // Important: Only enable the print option after the page is loaded.
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Data loaded, so now we want to show the print option.
                mDataLoaded = true;
                invalidateOptionsMenu();
            }
        });

        // Load an HTML page.
        mWebView.loadUrl("file:///android_res/raw/motogp_stats.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mDataLoaded) {
            getMenuInflater().inflate(R.menu.print_custom_content, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_print) {
            print();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void print() {
        // Get the print manager.
        PrintManager printManager = (PrintManager) getSystemService(
                Context.PRINT_SERVICE);
        // Pass in the ViewView's document adapter.
        printManager.print("MotoGP stats", mWebView.createPrintDocumentAdapter(), null);
    }
}
