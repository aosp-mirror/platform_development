/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.repository;


import java.io.InputStream;

/**
 * Public constants for the sdk-repository XML Schema.
 */
public class SdkRepository {

    /** The URL of the official Google sdk-repository site. */
    public static final String URL_GOOGLE_SDK_REPO_SITE =
        "https://dl-ssl.google.com/android/repository/";      //$NON-NLS-1$

    public static final String URL_DEFAULT_XML_FILE = "repository.xml";         //$NON-NLS-1$

    /** The XML namespace of the sdk-repository XML. */
    public static final String NS_SDK_REPOSITORY =
        "http://schemas.android.com/sdk/android/repository/1";                  //$NON-NLS-1$

    /** The root sdk-repository element */
    public static final String NODE_SDK_REPOSITORY = "sdk-repository";          //$NON-NLS-1$

    /** A platform package. */
    public static final String NODE_PLATFORM = "platform";                      //$NON-NLS-1$
    /** An add-on package. */
    public static final String NODE_ADD_ON   = "add-on";                        //$NON-NLS-1$
    /** A tool package. */
    public static final String NODE_TOOL     = "tool";                          //$NON-NLS-1$
    /** A doc package. */
    public static final String NODE_DOC      = "doc";                           //$NON-NLS-1$
    /** An extra package. */
    public static final String NODE_EXTRA    = "extra";                         //$NON-NLS-1$

    /** The license definition. */
    public static final String NODE_LICENSE = "license";                        //$NON-NLS-1$
    /** The optional uses-license for all packages (platform, add-on, tool, doc) or for a lib. */
    public static final String NODE_USES_LICENSE = "uses-license";              //$NON-NLS-1$
    /** The revision, an int > 0, for all packages (platform, add-on, tool, doc). */
    public static final String NODE_REVISION    = "revision";                   //$NON-NLS-1$
    /** The optional description for all packages (platform, add-on, tool, doc) or for a lib. */
    public static final String NODE_DESCRIPTION = "description";                //$NON-NLS-1$
    /** The optional description URL for all packages (platform, add-on, tool, doc). */
    public static final String NODE_DESC_URL    = "desc-url";                   //$NON-NLS-1$

    /** The version, a string, for platform packages. */
    public static final String NODE_VERSION   = "version";                      //$NON-NLS-1$
    /** The api-level, an int > 0, for platform, add-on and doc packages. */
    public static final String NODE_API_LEVEL = "api-level";                    //$NON-NLS-1$
    /** The codename, a string, for platform packages. */
    public static final String NODE_CODENAME = "codename";                      //$NON-NLS-1$
    /** The vendor, a string, for add-on packages. */
    public static final String NODE_VENDOR    = "vendor";                       //$NON-NLS-1$
    /** The name, a string, for add-on packages or for libraries. */
    public static final String NODE_NAME      = "name";                         //$NON-NLS-1$

    /** The libs container, optional for an add-on. */
    public static final String NODE_LIBS      = "libs";                         //$NON-NLS-1$
    /** A lib element in a libs container. */
    public static final String NODE_LIB       = "lib";                          //$NON-NLS-1$

    /** The path, a string, for extra packages. */
    public static final String NODE_PATH = "path";                              //$NON-NLS-1$

    /** The archives container, for all packages. */
    public static final String NODE_ARCHIVES = "archives";                      //$NON-NLS-1$
    /** An archive element, for the archives container. */
    public static final String NODE_ARCHIVE  = "archive";                       //$NON-NLS-1$

    /** An archive size, an int > 0. */
    public static final String NODE_SIZE     = "size";                          //$NON-NLS-1$
    /** A sha1 archive checksum, as a 40-char hex. */
    public static final String NODE_CHECKSUM = "checksum";                      //$NON-NLS-1$
    /** A download archive URL, either absolute or relative to the repository xml. */
    public static final String NODE_URL      = "url";                           //$NON-NLS-1$

    /** An archive checksum type, mandatory. */
    public static final String ATTR_TYPE = "type";                              //$NON-NLS-1$
    /** An archive OS attribute, mandatory. */
    public static final String ATTR_OS   = "os";                                //$NON-NLS-1$
    /** An optional archive Architecture attribute. */
    public static final String ATTR_ARCH = "arch";                              //$NON-NLS-1$

    /** A license definition ID. */
    public static final String ATTR_ID = "id";                                  //$NON-NLS-1$
    /** A license reference. */
    public static final String ATTR_REF = "ref";                                //$NON-NLS-1$


    public static InputStream getXsdStream() {
        return SdkRepository.class.getResourceAsStream("sdk-repository.xsd");   //$NON-NLS-1$
    }

}
