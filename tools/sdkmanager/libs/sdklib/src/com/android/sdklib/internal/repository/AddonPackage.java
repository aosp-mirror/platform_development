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

import java.util.ArrayList;

/**
 * Represents an add-on XML node in an SDK repository.
 */
public class AddonPackage extends Package {

    private final String mVendor;
    private final String mName;
    private final int    mApiLevel;

    /** An add-on library. */
    public static class Lib {
        private final String mName;
        private final String mDescription;

        public Lib(String name, String description) {
            mName = name;
            mDescription = description;
        }

        public String getName() {
            return mName;
        }

        public String getDescription() {
            return mDescription;
        }
    }

    private final Lib[] mLibs;

    /**
     * Creates a new add-on package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    AddonPackage(Node packageNode) {
        super(packageNode);
        mVendor   = getXmlString(packageNode, SdkRepository.NODE_VENDOR);
        mName     = getXmlString(packageNode, SdkRepository.NODE_NAME);
        mApiLevel = getXmlInt   (packageNode, SdkRepository.NODE_API_LEVEL, 0);

        mLibs = parseLibs(getFirstChild(packageNode, SdkRepository.NODE_LIBS));
    }

    private Lib[] parseLibs(Node libsNode) {
        ArrayList<Lib> libs = new ArrayList<Lib>();

        if (libsNode != null) {
            for(Node child = libsNode.getFirstChild();
                child != null;
                child = child.getNextSibling()) {

                if (child.getNodeType() == Node.ELEMENT_NODE &&
                        SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI()) &&
                        SdkRepository.NODE_LIB.equals(child.getLocalName())) {
                    libs.add(parseLib(child));
                }
            }
        }

        return libs.toArray(new Lib[libs.size()]);
    }

    private Lib parseLib(Node libNode) {
        return new Lib(getXmlString(libNode, SdkRepository.NODE_NAME),
                       getXmlString(libNode, SdkRepository.NODE_DESCRIPTION));
    }

    /** Returns the vendor, a string, for add-on packages. */
    public String getVendor() {
        return mVendor;
    }

    /** Returns the name, a string, for add-on packages or for libraries. */
    public String getName() {
        return mName;
    }

    /** Returns the api-level, an int > 0, for platform, add-on and doc packages. */
    public int getApiLevel() {
        return mApiLevel;
    }

    /** Returns the libs defined in this add-on. Can be an empty array but not null. */
    public Lib[] getLibs() {
        return mLibs;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        return String.format("%1$s by %2$s for Android API %3$d",
                getName(),
                getVendor(),
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
