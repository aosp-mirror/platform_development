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

package com.android.ddmlib.log;

import java.io.Serializable;

/**
 * Exception thrown when accessing an {@link EventContainer} value with the wrong type.
 */
public final class InvalidTypeException extends Exception {

    /**
     * Needed by {@link Serializable}.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the default detail message.
     * @see java.lang.Exception
     */
    public InvalidTypeException() {
        super("Invalid Type");
    }

    /**
     * Constructs a new exception with the specified detail message.
     * @param message the detail message. The detail message is saved for later retrieval
     * by the {@link Throwable#getMessage()} method.
     * @see java.lang.Exception
     */
    public InvalidTypeException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code> (which typically contains
     * the class and detail message of cause).
     * @param cause the cause (which is saved for later retrieval by the
     * {@link Throwable#getCause()} method). (A <code>null</code> value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @see java.lang.Exception
     */
    public InvalidTypeException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * @param message the detail message. The detail message is saved for later retrieval
     * by the {@link Throwable#getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the
     * {@link Throwable#getCause()} method). (A <code>null</code> value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @see java.lang.Exception
     */
    public InvalidTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
