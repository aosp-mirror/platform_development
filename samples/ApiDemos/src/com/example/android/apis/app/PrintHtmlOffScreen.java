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
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.android.apis.R;

/**
 * This class demonstrates how to implement HTML content printing
 * from a {@link WebView} which is not shown on the screen.
 * <p>
 * This activity shows a text prompt and when the user chooses the
 * print option from the overflow menu an HTML page with content that
 * is not on the screen is printed via an off-screen {@link WebView}.
 * </p>
 *
 * @see PrintManager
 * @see WebView
 */
public class PrintHtmlOffScreen extends Activity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_html_off_screen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.print_custom_content, menu);
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
        // Create a WebView and hold on to it as the printing will start when
        // load completes and we do not want the WbeView to be garbage collected.
        mWebView = new WebView(this);

        // Important: Only after the page is loaded we will do the print.
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
              doPrint();
            }
        });

        // Load an HTML page.
        mWebView.loadUrl("file:///android_res/raw/motogp_stats.html");
    }

    private void doPrint() {
        // Get the print manager.
        PrintManager printManager = (PrintManager) getSystemService(
                Context.PRINT_SERVICE);

        // Create a wrapper PrintDocumentAdapter to clean up when done.
        PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
            private final PrintDocumentAdapter mWrappedInstance =
                    mWebView.createPrintDocumentAdapter();

            @Override
            public void onStart() {
                mWrappedInstance.onStart();
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal, LayoutResultCallback callback,
                    Bundle extras) {
                mWrappedInstance.onLayout(oldAttributes, newAttributes, cancellationSignal,
                        callback, extras);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                    CancellationSignal cancellationSignal, WriteResultCallback callback) {
                mWrappedInstance.onWrite(pages, destination, cancellationSignal, callback);
            }

            @Override
            public void onFinish() {
                mWrappedInstance.onFinish();
                // Intercept the finish call to know when printing is done
                // and destroy the WebView as it is expensive to keep around.
                mWebView.destroy();
                mWebView = null;
            }
        };

        // Pass in the ViewView's document adapter.
        printManager.print("MotoGP stats", adapter, null);
    }
}
