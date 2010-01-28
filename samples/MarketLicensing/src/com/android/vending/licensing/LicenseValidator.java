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

import android.util.Log;

import com.android.vending.licensing.LicenseCheckerCallback.ApplicationErrorCode;
import com.android.vending.licensing.Policy.LicenseResponse;

/**
 * Contains data related to a licensing request and methods to verify
 * and process the response.
 */
class LicenseValidator {
    private static final String TAG = "LicenseValidator";

    // Server response codes.
    private static final int LICENSED = 0x0;
    private static final int NOT_LICENSED = 0x1;
    private static final int LICENSED_OLD_KEY = 0x2;
    private static final int ERROR_NOT_MARKET_MANAGED = 0x3;
    private static final int ERROR_INVALID_KEYS = 0x4;
    private static final int ERROR_OVER_QUOTA = 0x5;

    private static final int ERROR_CONTACTING_SERVER = 0x101;
    private static final int ERROR_INVALID_PACKAGE_NAME = 0x102;
    private static final int ERROR_NON_MATCHING_UID = 0x103;

    private final Policy mPolicy;
    private final LicenseCheckerCallback mCallback;
    private final int mNonce;
    private final String mPackageName;
    private final String mVersionCode;

    LicenseValidator(Policy policy, LicenseCheckerCallback callback, int nonce, String packageName,
        String versionCode) {
        mPolicy = policy;
        mCallback = callback;
        mNonce = nonce;
        mPackageName = packageName;
        mVersionCode = versionCode;
    }

    public LicenseCheckerCallback getCallback() {
        return mCallback;
    }

    public int getNonce() {
        return mNonce;
    }

    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Verifies the response from server and calls appropriate callback method.
     *
     * @param responseCode server response code
     * @param signedData signed data from server
     * @param signature server signature
     */
    public void verify(int responseCode, String signedData, String signature) {
        // Parse and validate response.
        // TODO(jyum): decode data with signature.
        // TODO(jyum): verify timestamp is within reason. However, relying
        // on device clock may lead to problems?
        ResponseData data;
        try {
            data = ResponseData.parse(signedData);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Could not parse response.");
            handleInvalidResponse();
            return;
        }

        if (data.responseCode != responseCode) {
            Log.e(TAG, "Response codes don't match.");
            handleInvalidResponse();
            return;
        }

        if (data.nonce != mNonce) {
            Log.e(TAG, "Nonce doesn't match.");
            handleInvalidResponse();
            return;
        }

        if (!data.packageName.equals(mPackageName)) {
            Log.e(TAG, "Package name doesn't match.");
            handleInvalidResponse();
            return;
        }

        if (!data.versionCode.equals(mVersionCode)) {
            Log.e(TAG, "Version codes don't match.");
            handleInvalidResponse();
            return;
        }

        switch (responseCode) {
            case LICENSED:
            case LICENSED_OLD_KEY:
                handleResponse(LicenseResponse.LICENSED);
                break;
            case NOT_LICENSED:
                handleResponse(LicenseResponse.NOT_LICENSED);
                break;
            case ERROR_CONTACTING_SERVER:
                handleResponse(LicenseResponse.CLIENT_RETRY);
                break;
            case ERROR_INVALID_KEYS:
            case ERROR_OVER_QUOTA:
                handleResponse(LicenseResponse.SERVER_RETRY);
                break;
            case ERROR_INVALID_PACKAGE_NAME:
                handleApplicationError(ApplicationErrorCode.INVALID_PACKAGE_NAME);
                break;
            case ERROR_NON_MATCHING_UID:
                handleApplicationError(ApplicationErrorCode.NON_MATCHING_UID);
                break;
            case ERROR_NOT_MARKET_MANAGED:
                handleApplicationError(ApplicationErrorCode.NOT_MARKET_MANAGED);
                break;
            default:
                Log.e(TAG, "Unknown response code for license check.");
                handleInvalidResponse();
        }
    }

    /**
     * Confers with policy and calls appropriate callback method.
     *
     * @param response
     */
    private void handleResponse(LicenseResponse response) {
        if (mPolicy.allowAccess(response)) {
            mCallback.allow();
        } else {
            mCallback.dontAllow();
        }
    }

    private void handleApplicationError(ApplicationErrorCode code) {
        mCallback.applicationError(code);
    }

    private void handleInvalidResponse() {
        mCallback.dontAllow();
    }
}
