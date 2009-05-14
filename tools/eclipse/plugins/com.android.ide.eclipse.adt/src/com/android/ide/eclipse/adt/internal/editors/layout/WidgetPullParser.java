/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.layoutlib.api.IXmlPullParser;
import com.android.sdklib.SdkConstants;

import org.xmlpull.v1.XmlPullParserException;

/**
 * {@link IXmlPullParser} implementation to render android widget bitmap.
 * <p/>The parser emulates a layout that contains just one widget, described by the
 * {@link ViewElementDescriptor} passed in the constructor.
 */
public class WidgetPullParser extends BasePullParser {
    
    private final ViewElementDescriptor mDescriptor;
    private String[][] mAttributes = new String[][] {
            { "text", null },
            { "layout_width", "wrap_content" },
            { "layout_height", "wrap_content" },
    };

    public WidgetPullParser(ViewElementDescriptor descriptor) {
        mDescriptor = descriptor;
        
        String[] segments = mDescriptor.getCanonicalClassName().split(AndroidConstants.RE_DOT);
        mAttributes[0][1] = segments[segments.length-1];
    }

    public Object getViewKey() {
        // we need a viewKey or the ILayoutResult will not contain any ILayoutViewInfo
        return mDescriptor;
    }

    public int getAttributeCount() {
        return mAttributes.length; // text attribute
    }

    public String getAttributeName(int index) {
        if (index < mAttributes.length) {
            return mAttributes[index][0];
        }
        
        return null;
    }

    public String getAttributeNamespace(int index) {
        return SdkConstants.NS_RESOURCES;
    }

    public String getAttributePrefix(int index) {
        // pass
        return null;
    }

    public String getAttributeValue(int index) {
        if (index < mAttributes.length) {
            return mAttributes[index][1];
        }
        
        return null;
    }

    public String getAttributeValue(String ns, String name) {
        if (SdkConstants.NS_RESOURCES.equals(ns)) {
            for (String[] attribute : mAttributes) {
                if (name.equals(attribute[0])) {
                    return attribute[1];
                }
            }
        }
        
        return null;
    }

    public int getDepth() {
        // pass
        return 0;
    }

    public String getName() {
        return mDescriptor.getXmlLocalName();
    }

    public String getNamespace() {
        // pass
        return null;
    }

    public String getPositionDescription() {
        // pass
        return null;
    }

    public String getPrefix() {
        // pass
        return null;
    }

    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (mParsingState == START_TAG) {
            return true;
        }
        
        throw new XmlPullParserException("Call to isEmptyElementTag while not in START_TAG",
                this, null);
    }

    @Override
    public void onNextFromStartDocument() {
        // just go to start_tag
        mParsingState = START_TAG;
    }

    @Override
    public void onNextFromStartTag() {
        // since we have no children, just go to end_tag
        mParsingState = END_TAG;
    }

    @Override
    public void onNextFromEndTag() {
        // just one tag. we are done.
        mParsingState = END_DOCUMENT;
    }
}
