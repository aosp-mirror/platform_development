/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.vending.licensing;

import java.security.SecureRandom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.licensing.LicenseCheckerCallback.ApplicationErrorCode;
import com.android.vending.licensing.Policy.LicenseResponse;

/**
 * Client library for Android Market license verifications.
 *
 * The LicenseChecker is configured via a {@link Policy} which contains the
 * logic to determine whether a user should have access to the application.
 * For example, the Policy can define a threshold for allowable number of
 * server or client failures before the library reports the user as not having
 * access.
 *
 * This library is not thread-safe. Multiple, concurrent checks will result in
 * an error.
 */
public class LicenseChecker implements ServiceConnection {
    private static final String TAG = "LicenseChecker";

    private static final SecureRandom RANDOM = new SecureRandom();

    private ILicensingService mService;

    /** Validator for the request in progress. */
    private LicenseValidator mValidator;

    private final Context mContext;
    private final Policy mPolicy;
    /** Listener for service (IPC) calls. */
    private final ResultListener mListener;
    private final String mPackageName;
    private final String mVersionCode;

    public LicenseChecker(Context context, Policy policy) {
        mContext = context;
        mPolicy = policy;
        mListener = new ResultListener();
        mPackageName = mContext.getPackageName();
        mVersionCode = getVersionCode(context, mPackageName);
    }

    private boolean isInProgress() {
        return mValidator != null;
    }

    /**
     * Checks if the user should have access to the app.
     *
     * @param callback
     */
    public synchronized void checkAccess(LicenseCheckerCallback callback) {
        if (isInProgress()) {
            callback.applicationError(ApplicationErrorCode.CHECK_IN_PROGRESS);
        }

        mValidator = new LicenseValidator(mPolicy, callback, generateNonce(), mPackageName,
            mVersionCode);

        Log.i(TAG, "Binding to licensing service.");
        boolean bindResult = mContext.bindService(new Intent(ILicensingService.class.getName()),
            this,  // ServiceConnection.
            Context.BIND_AUTO_CREATE);

        if (!bindResult) {
            Log.e(TAG, "Could not bind to service.");
            callback.dontAllow();
            // No need to unbind at this point.
            return;
        }
    }

    private class ResultListener extends ILicenseResultListener.Stub {
        public void verifyLicense(int responseCode, String signedData, String signature) {
            mValidator.verify(responseCode, signedData, signature);
            cleanup();
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ILicensingService.Stub.asInterface(service);

        try {
            Log.i(TAG, "Calling checkLicense on service for " + mValidator.getPackageName());
            mService.checkLicense(mValidator.getNonce(), mValidator.getPackageName(), mListener);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException in checkLicense call.", e);
            handleServiceConnectionError();
            // cleanup unbinds service.
            cleanup();
        }
    }

    public void onServiceDisconnected(ComponentName name) {
        // Called when the connection with the service has been
        // unexpectedly disconnected. That is, Market crashed.
        Log.w(TAG, "Service unexpectedly disconnected.");
        handleServiceConnectionError();
        // cleanup unbinds service.
        cleanup();
    }

    private void handleServiceConnectionError() {
        if (mPolicy.allowAccess(LicenseResponse.CLIENT_RETRY)) {
            mValidator.getCallback().allow();
        } else {
            mValidator.getCallback().dontAllow();
        }
    }

    /** Resets request state. */
    private synchronized void cleanup() {
        mContext.unbindService(this);
        mValidator = null;
    }

    /** Generates a nonce (number used once). */
    private int generateNonce() {
        return RANDOM.nextInt();
    }

    /**
     * Get version code for the application package name.
     *
     * @param context
     * @param packageName application package name
     * @return the version code or empty string if package not found
     */
    private static String getVersionCode(Context context, String packageName) {
        try {
            return String.valueOf(context.getPackageManager().getPackageInfo(packageName, 0).
                versionCode);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package not found. could not get version code.");
            return "";
        }
    }
}
