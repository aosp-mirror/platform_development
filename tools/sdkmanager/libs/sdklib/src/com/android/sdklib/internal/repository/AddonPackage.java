/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Node;

/**
 *
 */
public class AddonPackage extends Package {

    private final String mVendor;
    private final String mName;
    private final String mApiLevel;

    public AddonPackage(Node packageNode) {
        super(packageNode);
        mVendor   = getXmlString(packageNode, SdkRepository.NODE_VENDOR);
        mName     = getXmlString(packageNode, SdkRepository.NODE_NAME);
        mApiLevel = getXmlString(packageNode, SdkRepository.NODE_API_LEVEL);

        // TODO libs
    }

    public String getVendor() {
        return mVendor;
    }

    public String getName() {
        return mName;
    }

    public String getApiLevel() {
        return mApiLevel;
    }


}
