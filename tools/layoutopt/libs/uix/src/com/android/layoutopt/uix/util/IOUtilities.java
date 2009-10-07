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

package com.android.layoutopt.uix.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Various utilities related to I/O operations.
 */
public class IOUtilities {
    private IOUtilities() {
    }

    /**
     * Safely close a Closeable object, like an InputStream.
     *
     * @param stream The object to close.
     *
     * @return True if the object is null or was closed properly,
     *         false otherwise.
     */
    public static boolean close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
