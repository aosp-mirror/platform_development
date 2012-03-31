/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.keychain;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

public class SecureWebServer {

    // Log tag for this class
    private static final String TAG = "SecureWebServer";

    // File name of the image used in server response
    private static final String EMBEDDED_IMAGE_FILENAME = "training-prof.png";

    private SSLServerSocketFactory sssf;
    private SSLServerSocket sss;

    // A flag to control whether the web server should be kept running
    private boolean isRunning = true;

    // The base64 encoded image string used as an embedded image
    private final String base64Image;

    /**
     * WebServer constructor.
     */
    public SecureWebServer(Context ctx) {
        try {
            // Get an SSL context using the TLS protocol
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // Get a key manager factory using the default algorithm
            KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());

            // Load the PKCS12 key chain
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream fis = ctx.getAssets()
                    .openFd(KeyChainDemoActivity.PKCS12_FILENAME)
                    .createInputStream();
            ks.load(fis, KeyChainDemoActivity.PKCS12_PASSWORD.toCharArray());
            kmf.init(ks, KeyChainDemoActivity.PKCS12_PASSWORD.toCharArray());

            // Initialize the SSL context
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Create the SSL server socket factory
            sssf = sslContext.getServerSocketFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create the base64 image string used in the server response
        base64Image = createBase64Image(ctx);
    }

    /**
     * This method starts the web server listening to the port 8080
     */
    protected void start() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Secure Web Server is starting up on port 8080");
                try {
                    // Create the secure server socket
                    sss = (SSLServerSocket) sssf.createServerSocket(8080);
                } catch (Exception e) {
                    System.out.println("Error: " + e);
                    return;
                }

                Log.d(TAG, "Waiting for connection");
                while (isRunning) {
                    try {
                        // Wait for an SSL connection
                        Socket socket = sss.accept();

                        // Got a connection
                        Log.d(TAG, "Connected, sending data.");

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter out = new PrintWriter(socket
                                .getOutputStream());

                        // Read the data until a blank line is reached which
                        // signifies the end of the client HTTP headers
                        String str = ".";
                        while (!str.equals(""))
                            str = in.readLine();

                        // Send a HTTP response
                        out.println("HTTP/1.0 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("Server: Android KeyChainiDemo SSL Server");
                        // this blank line signals the end of the headers
                        out.println("");
                        // Send the HTML page
                        out.println("<H1>Welcome to Android!</H1>");
                        // Add an embedded Android image
                        out.println("<img src='data:image/png;base64," + base64Image + "'/>");
                        out.flush();
                        socket.close();
                    } catch (Exception e) {
                        Log.d(TAG, "Error: " + e);
                    }
                }
            }
        }).start();

    }

    /**
     * This method stops the SSL web server
     */
    protected void stop() {
        try {
            // Break out from the infinite while loop in start()
            isRunning = false;

            // Close the socket
            if (sss != null) {
                sss.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method reads a binary image from the assets folder and returns the
     * base64 encoded image string.
     *
     * @param ctx The service this web server is running in.
     * @return String The base64 encoded image string or "" if there is an
     *         exception
     */
    private String createBase64Image(Context ctx) {
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(ctx.getAssets().open(EMBEDDED_IMAGE_FILENAME));
            byte[] embeddedImage = new byte[bis.available()];
            bis.read(embeddedImage);
            return Base64.encodeToString(embeddedImage, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
