/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.metalava.apilevels;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents an API element, e.g. class, method or field.
 */
public class ApiElement implements Comparable<ApiElement> {
    private final String mName;
    private int mSince;
    private int mDeprecatedIn;
    private int mLastPresentIn;

    /**
     * @param name       the name of the API element
     * @param version    an API version for which the API element existed
     * @param deprecated whether the API element was deprecated in the API version in question
     */
    public ApiElement(String name, int version, boolean deprecated) {
        assert name != null;
        assert version > 0;
        mName = name;
        mSince = version;
        mLastPresentIn = version;
        if (deprecated) {
            mDeprecatedIn = version;
        }
    }

    /**
     * @param name    the name of the API element
     * @param version an API version for which the API element existed
     */
    public ApiElement(String name, int version) {
        this(name, version, false);
    }

    protected ApiElement(String name) {
        assert name != null;
        mName = name;
    }

    /**
     * Returns the name of the API element.
     */
    public final String getName() {
        return mName;
    }

    /**
     * Checks if this API element was introduced not later than another API element.
     *
     * @param other the API element to compare to
     * @return true if this API element was introduced not later than {@code other}
     */
    public final boolean introducedNotLaterThan(ApiElement other) {
        return mSince <= other.mSince;
    }

    /**
     * Updates the API element with information for a specific API version.
     *
     * @param version    an API version for which the API element existed
     * @param deprecated whether the API element was deprecated in the API version in question
     */
    public void update(int version, boolean deprecated) {
        assert version > 0;
        if (mSince > version) {
            mSince = version;
        }
        if (mLastPresentIn < version) {
            mLastPresentIn = version;
        }
        if (deprecated) {
            if (mDeprecatedIn == 0 || mDeprecatedIn > version) {
                mDeprecatedIn = version;
            }
        }
    }

    /**
     * Updates the API element with information for a specific API version.
     *
     * @param version an API version for which the API element existed
     */
    public void update(int version) {
        update(version, false);
    }

    /**
     * Checks whether the API element is deprecated or not.
     */
    public final boolean isDeprecated() {
        return mDeprecatedIn != 0;
    }

    /**
     * Prints an XML representation of the element to a stream terminated by a line break.
     * Attributes with values matching the parent API element are omitted.
     *
     * @param tag           the tag of the XML element
     * @param parentElement the parent API element
     * @param indent        the whitespace prefix to insert before the XML element
     * @param stream        the stream to print the XML element to
     */
    public void print(String tag, ApiElement parentElement, String indent, PrintStream stream) {
        print(tag, true, parentElement, indent, stream);
    }

    /**
     * Prints an XML representation of the element to a stream terminated by a line break.
     * Attributes with values matching the parent API element are omitted.
     *
     * @param tag           the tag of the XML element
     * @param closeTag      if true the XML element is terminated by "/>", otherwise the closing
     *                      tag of the element is not printed
     * @param parentElement the parent API element
     * @param indent        the whitespace prefix to insert before the XML element
     * @param stream        the stream to print the XML element to
     * @see #printClosingTag(String, String, PrintStream)
     */
    protected void print(String tag, boolean closeTag, ApiElement parentElement, String indent,
                         PrintStream stream) {
        stream.print(indent);
        stream.print('<');
        stream.print(tag);
        stream.print(" name=\"");
        stream.print(encodeAttribute(mName));
        if (mSince > parentElement.mSince) {
            stream.print("\" since=\"");
            stream.print(mSince);
        }
        if (mDeprecatedIn != 0) {
            stream.print("\" deprecated=\"");
            stream.print(mDeprecatedIn);
        }
        if (mLastPresentIn < parentElement.mLastPresentIn) {
            stream.print("\" removed=\"");
            stream.print(mLastPresentIn + 1);
        }
        stream.print('"');
        if (closeTag) {
            stream.print('/');
        }
        stream.println('>');
    }

    /**
     * Prints homogeneous XML elements to a stream. Each element is printed on a separate line.
     * Attributes with values matching the parent API element are omitted.
     *
     * @param elements the elements to print
     * @param tag      the tag of the XML elements
     * @param indent   the whitespace prefix to insert before each XML element
     * @param stream   the stream to print the XML elements to
     */
    protected void print(Collection<? extends ApiElement> elements, String tag, String indent, PrintStream stream) {
        for (ApiElement element : sortedList(elements)) {
            element.print(tag, this, indent, stream);
        }
    }

    private <T extends ApiElement> List<T> sortedList(Collection<T> elements) {
        List<T> list = new ArrayList<T>(elements);
        Collections.sort(list);
        return list;
    }

    /**
     * Prints a closing tag of an XML element terminated by a line break.
     *
     * @param tag    the tag of the element
     * @param indent the whitespace prefix to insert before the closing tag
     * @param stream the stream to print the XML element to
     */
    protected static void printClosingTag(String tag, String indent, PrintStream stream) {
        stream.print(indent);
        stream.print("</");
        stream.print(tag);
        stream.println('>');
    }

    protected static String encodeAttribute(String attribute) {
        StringBuilder sb = new StringBuilder();
        int n = attribute.length();
        // &, ", ' and < are illegal in attributes; see http://www.w3.org/TR/REC-xml/#NT-AttValue
        // (' legal in a " string and " is legal in a ' string but here we'll stay on the safe side).
        for (int i = 0; i < n; i++) {
            char c = attribute.charAt(i);
            if (c == '"') {
                sb.append("&quot;"); //$NON-NLS-1$
            } else if (c == '<') {
                sb.append("&lt;"); //$NON-NLS-1$
            } else if (c == '\'') {
                sb.append("&apos;"); //$NON-NLS-1$
            } else if (c == '&') {
                sb.append("&amp;"); //$NON-NLS-1$
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    @Override
    public int compareTo(ApiElement other) {
        return mName.compareTo(other.mName);
    }
}
