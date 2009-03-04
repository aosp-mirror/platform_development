/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.common.project;

import com.android.sdklib.SdkConstants;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * XPath factory with automatic support for the android namespace.
 */
public class AndroidXPathFactory {
    public final static String DEFAULT_NS_PREFIX = "android"; //$NON-NLS-1$

    private final static XPathFactory sFactory = XPathFactory.newInstance();
    
    /** Namespace context for Android resource XML files. */
    private static class AndroidNamespaceContext implements NamespaceContext {
        private String mAndroidPrefix;

        /**
         * Construct the context with the prefix associated with the android namespace.
         * @param androidPrefix the Prefix
         */
        public AndroidNamespaceContext(String androidPrefix) {
            mAndroidPrefix = androidPrefix;
        }

        public String getNamespaceURI(String prefix) {
            if (prefix != null) {
                if (prefix.equals(mAndroidPrefix)) {
                    return SdkConstants.NS_RESOURCES;
                }
            }
            
            return XMLConstants.NULL_NS_URI;
        }

        public String getPrefix(String namespaceURI) {
            // This isn't necessary for our use.
            assert false;
            return null;
        }

        public Iterator<?> getPrefixes(String namespaceURI) {
            // This isn't necessary for our use.
            assert false;
            return null;
        }
    }
    
    /**
     * Creates a new XPath object, specifying which prefix in the query is used for the
     * android namespace.
     * @param androidPrefix The namespace prefix.
     */
    public static XPath newXPath(String androidPrefix) {
        XPath xpath = sFactory.newXPath();
        xpath.setNamespaceContext(new AndroidNamespaceContext(androidPrefix));
        return xpath;
    }

    /**
     * Creates a new XPath object using the default prefix for the android namespace.
     * @see #DEFAULT_NS_PREFIX
     */
    public static XPath newXPath() {
        return newXPath(DEFAULT_NS_PREFIX);
    }
}
