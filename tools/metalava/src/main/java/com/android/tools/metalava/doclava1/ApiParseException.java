/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.metalava.doclava1;

//
// Copied from doclava1, but adapted to metalava's code model
//
public final class ApiParseException extends Exception {
    public String file;
    public int line;

    public ApiParseException(String message) {
        super(message);
    }

    public ApiParseException(String message, Exception cause) {
        super(message, cause);
        if (cause instanceof ApiParseException) {
            this.line = ((ApiParseException) cause).line;
        }
    }

    public ApiParseException(String message, int line) {
        super(message);
        this.line = line;
    }

    public String getMessage() {
        if (line > 0) {
            return super.getMessage() + " line " + line;
        } else {
            return super.getMessage();
        }
    }
}
