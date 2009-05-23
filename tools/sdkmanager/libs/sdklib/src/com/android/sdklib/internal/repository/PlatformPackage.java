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
 * Represents a platform XML node in an SDK repository.
 */
public class PlatformPackage extends Package {

    private final String mVersion;
    private final int mApiLevel;

    /**
     * Creates a new platform package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    PlatformPackage(Node packageNode) {
        super(packageNode);
        mVersion  = getXmlString(packageNode, SdkRepository.NODE_VERSION);
        mApiLevel = getXmlInt   (packageNode, SdkRepository.NODE_API_LEVEL, 0);
    }

    /** Returns the version, a string, for platform packages. */
    public String getVersion() {
        return mVersion;
    }

    /** Returns the api-level, an int > 0, for platform, add-on and doc packages. */
    public int getApiLevel() {
        return mApiLevel;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        return String.format("SDK Platform Android %1$s, API %2$d",
                getVersion(),
                getApiLevel());
    }

    /** Returns a long description for an {@link IDescription}. */
    @Override
    public String getLongDescription() {
        return String.format("%1$s.\n%2$s",
                getShortDescription(),
                super.getLongDescription());
    }
}
