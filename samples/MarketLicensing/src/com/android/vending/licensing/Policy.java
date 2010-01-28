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
 * Policy used by {@link LicenseChecker} to determine whether a user should
 * have access to the application.
 */
public interface Policy {

    /**
     * Result of a license check.
     */
    public enum LicenseResponse {
        /**
         * User is licensed to use the app.
         */
        LICENSED,
        /**
         * User is not licensed to use the app.
         */
        NOT_LICENSED,
        /**
         * Retryable error on the client side e.g. no network.
         */
        CLIENT_RETRY,
        /**
         * Retryable error on the server side e.g. application is over request
         * quota.
         */
        SERVER_RETRY,
    }

    /**
     * Determines whether the user should be allowed access.
     *
     * @param response result of the license check request
     * @return true iff access should be allowed
     */
    boolean allowAccess(LicenseResponse response);
}
