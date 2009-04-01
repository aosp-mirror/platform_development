/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdklib;

import java.util.Formatter;

/**
 * Interface used to display warnings/errors while parsing the SDK content.
 */
public interface ISdkLog {
    
    /**
     * Prints a warning message on stdout.
     * <p/>
     * The message will be tagged with "Warning" on the output so the caller does not
     * need to put such a prefix in the format string.
     * <p/>
     * Implementations should only display warnings in verbose mode.
     * 
     * @param warningFormat is an optional error format. If non-null, it will be printed
     *          using a {@link Formatter} with the provided arguments.
     * @param args provides the arguments for warningFormat.
     */
    void warning(String warningFormat, Object... args);
    
    /**
     * Prints an error message on stderr.
     * <p/>
     * The message will be tagged with "Error" on the output so the caller does not
     * need to put such a prefix in the format string.
     * <p/>
     * Implementation should always display errors, independent of verbose mode.
     * 
     * @param t is an optional {@link Throwable} or {@link Exception}. If non-null, it's
     *          message will be printed out.
     * @param errorFormat is an optional error format. If non-null, it will be printed
     *          using a {@link Formatter} with the provided arguments.
     * @param args provides the arguments for errorFormat.
     */
    void error(Throwable t, String errorFormat, Object... args);
    
    /**
     * Prints a message as-is on stdout.
     * <p/>
     * Implementation should always display errors, independent of verbose mode.
     * No prefix is used, the message is printed as-is after formatting.
     * 
     * @param msgFormat is an optional error format. If non-null, it will be printed
     *          using a {@link Formatter} with the provided arguments.
     * @param args provides the arguments for msgFormat.
     */
    void printf(String msgFormat, Object... args);
}
