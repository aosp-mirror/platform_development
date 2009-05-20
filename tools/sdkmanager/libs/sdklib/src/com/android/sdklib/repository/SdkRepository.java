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
 * Constants for the sdk-repository XML Schema
 */
public class SdkRepository {

    public static final String NS_SDK_REPOSITORY =
        "http://schemas.android.com/sdk/android/repository/1";                  //$NON-NLS-1$

    public static final String NODE_VERSION = "version";                        //$NON-NLS-1$
    public static final String NODE_REVISION = "revision";                      //$NON-NLS-1$
    public static final String NODE_API_LEVEL = "api-level";                    //$NON-NLS-1$
    public static final String NODE_VENDOR = "vendor";                          //$NON-NLS-1$
    public static final String NODE_NAME = "name";                              //$NON-NLS-1$
    public static final String NODE_TOOL = "tool";                              //$NON-NLS-1$
    public static final String NODE_DOC = "doc";                                //$NON-NLS-1$
    public static final String NODE_PLATFORM = "platform";                      //$NON-NLS-1$
    public static final String NODE_ADD_ON = "add-on";                          //$NON-NLS-1$
    public static final String NODE_SDK_REPOSITORY = "sdk-repository";          //$NON-NLS-1$

    public static InputStream getXsdStream() {
        return SdkRepository.class.getResourceAsStream("sdk-repository.xsd");   //$NON-NLS-1$
    }

}
