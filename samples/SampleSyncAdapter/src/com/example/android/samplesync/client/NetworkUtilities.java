/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.samplesync.client;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.android.samplesync.authenticator.AuthenticatorActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Provides utility methods for communicating with the server.
 */
public class NetworkUtilities {
    private static final String TAG = "NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String BASE_URL =
        "https://samplesyncadapter.appspot.com";
    public static final String AUTH_URI = BASE_URL + "/auth";
    public static final String FETCH_FRIEND_UPDATES_URI =
        BASE_URL + "/fetch_friend_updates";
    public static final String FETCH_STATUS_URI = BASE_URL + "/fetch_status";
    private static HttpClient mHttpClient;

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static void maybeCreateHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params,
                REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password,
        Handler handler, final Context context) {
        final HttpResponse resp;

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new AssertionError(e);
        }
        final HttpPost post = new HttpPost(AUTH_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Successful authentication");
                }
                sendResult(true, handler, context);
                return true;
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                }
                sendResult(false, handler, context);
                return false;
            }
        } catch (final IOException e) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "IOException when getting authtoken", e);
            }
            sendResult(false, handler, context);
            return false;
        } finally {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "getAuthtoken completing");
            }
        }
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final Boolean result, final Handler handler,
        final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username,
        final String password, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }

    /**
     * Fetches the list of friend data updates from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in AccountManager for this account
     * @param lastUpdated The last time that sync was performed
     * @return list The list of updates received from the server.
     */
    public static List<User> fetchFriendUpdates(Account account,
        String authtoken, Date lastUpdated) throws JSONException,
        ParseException, IOException, AuthenticationException {
        final ArrayList<User> friendList = new ArrayList<User>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        if (lastUpdated != null) {
            final SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy/MM/dd HH:mm");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
                .format(lastUpdated)));
        }
        Log.i(TAG, params.toString());

        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_FRIEND_UPDATES_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            final JSONArray friends = new JSONArray(response);
            Log.d(TAG, response);
            for (int i = 0; i < friends.length(); i++) {
                friendList.add(User.valueOf(friends.getJSONObject(i)));
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG,
                    "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching remote contacts: "
                    + resp.getStatusLine());
                throw new IOException();
            }
        }
        return friendList;
    }

    /**
     * Fetches status messages for the user's friends from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in the AccountManager for the
     *        account
     * @return list The list of status messages received from the server.
     */
    public static List<User.Status> fetchFriendStatuses(Account account,
        String authtoken) throws JSONException, ParseException, IOException,
        AuthenticationException {
        final ArrayList<User.Status> statusList = new ArrayList<User.Status>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));

        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_STATUS_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            final JSONArray statuses = new JSONArray(response);
            for (int i = 0; i < statuses.length(); i++) {
                statusList.add(User.Status.valueOf(statuses.getJSONObject(i)));
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG,
                    "Authentication exception in fetching friend status list");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching friend status list");
                throw new IOException();
            }
        }
        return statusList;
    }

}
