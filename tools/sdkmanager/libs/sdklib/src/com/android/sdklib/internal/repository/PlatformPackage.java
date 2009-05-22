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
public class PlatformPackage extends Package {

    private final String mVersion;
    private final String mApiLevel;

    public PlatformPackage(Node packageNode) {
        super(packageNode);
        mVersion  = getXmlString(packageNode, SdkRepository.NODE_VERSION);
        mApiLevel = getXmlString(packageNode, SdkRepository.NODE_API_LEVEL);
    }

    public String getVersion() {
        return mVersion;
    }

    public String getApiLevel() {
        return mApiLevel;
    }


}
