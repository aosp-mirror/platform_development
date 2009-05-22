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
public class Package {

    private final int mRevision;
    private final String mDescription;
    private final String mDescUrl;

    private Package(int revision, String description, String descUrl) {
        mRevision = revision;
        mDescription = description;
        mDescUrl = descUrl;
    }

    public Package(Node packageNode) {
        this(getXmlInt   (packageNode, SdkRepository.NODE_REVISION, 0),
             getXmlString(packageNode, SdkRepository.NODE_DESCRIPTION),
             getXmlString(packageNode, SdkRepository.NODE_DESC_URL));

        // TODO archives
    }

    /** The revision, an int > 0, for all packages (platform, add-on, tool, doc). */
    public int getRevision() {
        return mRevision;
    }

    /** The optional description for all packages (platform, add-on, tool, doc) or for a lib. */
    public String getDescription() {
        return mDescription;
    }

    /** The optional description URL for all packages (platform, add-on, tool, doc).
     * Can be empty but not null. */
    public String getDescUrl() {
        return mDescUrl;
    }

    /**
     * Retrieves the value of that XML element as a string.
     * Returns an empty string when the element is missing.
     */
    protected static String getXmlString(Node node, String xmlLocalName) {
        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNamespaceURI().equals(SdkRepository.NS_SDK_REPOSITORY)) {
                if (xmlLocalName == null || child.getLocalName().equals(xmlLocalName)) {
                    return child.getTextContent();
                }
            }
        }

        return "";
    }

    /**
     * Retrieves the value of that XML element as an integer.
     * Returns the default value when the element is missing or is not an integer.
     */
    protected static int getXmlInt(Node node, String xmlLocalName, int defaultValue) {
        String s = getXmlString(node, xmlLocalName);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
