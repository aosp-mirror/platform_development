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

/**
 * Callback for the license checker library.
 *
 * Upon checking with the Market server and conferring with the policy, the
 * library calls a appropriate callback method to communicate the result.
 */
public interface LicenseCheckerCallback {

    /**
     * Allow use. App should proceed as normal.
     */
    public void allow();

    /**
     * Don't allow use. App should inform user and take appropriate action.
     */
    public void dontAllow();

    /** Application error codes. */
    public enum ApplicationErrorCode {
        /** Package is not installed. */
        INVALID_PACKAGE_NAME,
        /** Requested for a package that is not the current app. */
        NON_MATCHING_UID,
        /** Market does not know about the package. */
        NOT_MARKET_MANAGED,
        /** A previous check request is already in progress.
         * Only one check is allowed at a time. */
        CHECK_IN_PROGRESS
    }

    /**
     * Error in application code. Caller did not call or set up license
     * checker correctly. Should be considered fatal.
     */
    public void applicationError(ApplicationErrorCode errorCode);
}
