/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apkcheck;

import java.io.StringReader;

/**
 * Class containing "built-in" API description entries.
 *
 * There are some bugs in the API description file that we can't work around
 * (notably some ambiguity with generic types).  The easiest way to cope
 * is to supply the correct definitions in an add-on file.  Rather than
 * cart around an extra file, we bake them in here.
 */
public class Builtin {
    private Builtin() {}

    private static final String BUILTIN =
        "<api>\n" +
        " <package name=\"java.util\">\n" +
        "  <class name=\"EnumSet\"\n" +
        "   extends=\"java.util.AbstractSet\">\n" +
        "   <method name=\"of\" return=\"java.util.EnumSet\">\n" +
        "    <parameter name=\"e\" type=\"java.lang.Enum\"/>\n" +
        "   </method>\n" +
        "   <method name=\"of\" return=\"java.util.EnumSet\">\n" +
        "    <parameter name=\"e1\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e2\" type=\"java.lang.Enum\"/>\n" +
        "   </method>\n" +
        "   <method name=\"of\" return=\"java.util.EnumSet\">\n" +
        "    <parameter name=\"e1\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e2\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e3\" type=\"java.lang.Enum\"/>\n" +
        "   </method>\n" +
        "   <method name=\"of\" return=\"java.util.EnumSet\">\n" +
        "    <parameter name=\"e1\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e2\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e3\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e4\" type=\"java.lang.Enum\"/>\n" +
        "   </method>\n" +
        "   <method name=\"of\" return=\"java.util.EnumSet\">\n" +
        "    <parameter name=\"e1\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e2\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e3\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e4\" type=\"java.lang.Enum\"/>\n" +
        "    <parameter name=\"e5\" type=\"java.lang.Enum\"/>\n" +
        "   </method>\n" +
        "  </class>\n" +

        " </package>\n" +
        " <package name=\"android.os\">\n" +

        "  <class name=\"RemoteCallbackList\"\n" +
        "   extends=\"java.lang.Object\">\n" +
        "   <method name=\"register\" return=\"boolean\">\n" +
        "    <parameter name=\"callback\" type=\"android.os.IInterface\"/>\n" +
        "   </method>\n" +
        "   <method name=\"unregister\" return=\"boolean\">\n" +
        "    <parameter name=\"callback\" type=\"android.os.IInterface\"/>\n" +
        "   </method>\n" +
        "  </class>\n" +

        "  <class name=\"AsyncTask\"\n" +
        "   extends=\"java.lang.Object\">\n" +
        "   <method name=\"onPostExecute\" return=\"void\">\n" +
        "    <parameter name=\"result\" type=\"java.lang.Object\"/>\n" +
        "   </method>\n" +
        "   <method name=\"onProgressUpdate\" return=\"void\">\n" +
        "    <parameter name=\"values\" type=\"java.lang.Object[]\"/>\n" +
        "   </method>\n" +
        "   <method name=\"execute\" return=\"android.os.AsyncTask\">\n" +
        "    <parameter name=\"params\" type=\"java.lang.Object[]\"/>\n" +
        "   </method>\n" +
        "  </class>\n" +

        " </package>\n" +
        " <package name=\"android.widget\">\n" +

        "  <class name=\"AutoCompleteTextView\"\n" +
        "   extends=\"android.widget.EditText\">\n" +
        "   <method name=\"setAdapter\" return=\"void\">\n" +
        "    <parameter name=\"adapter\" type=\"android.widget.ListAdapter\"/>\n" +
        "   </method>\n" +
        "  </class>\n" +

        " </package>\n" +
        "</api>\n"
        ;

    /**
     * Returns the built-in definition "file".
     */
    public static StringReader getReader() {
        return new StringReader(BUILTIN);
    }
}

