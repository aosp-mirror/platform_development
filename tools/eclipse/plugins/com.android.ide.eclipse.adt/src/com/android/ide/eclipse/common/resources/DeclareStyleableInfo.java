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

package com.android.ide.eclipse.common.resources;


/**
 * Information needed to represent a View or ViewGroup (aka Layout) item
 * in the layout hierarchy, as extracted from the main android.jar and the
 * associated attrs.xml.
 */
public class DeclareStyleableInfo {
    /** The style name, never null. */
    private String mStyleName;
    /** Attributes for this view or view group. Can be empty but never null. */
    private AttributeInfo[] mAttributes;
    /** Short javadoc. Can be null. */
    private String mJavaDoc;
    /** Optional name of the parents stylable. Can be null. */
    private String[] mParents;    

    public static class AttributeInfo {
        /** XML Name of the attribute */
        private String mName;
        
        public enum Format {
            STRING,
            BOOLEAN,
            INTEGER,
            FLOAT,
            REFERENCE,
            COLOR,
            DIMENSION,
            FRACTION,
            ENUM,
            FLAG,
        }
        
        /** Formats of the attribute. Cannot be null. Should have at least one format. */
        private Format[] mFormats;
        /** Values for enum. null for other types. */
        private String[] mEnumValues;
        /** Values for flag. null for other types. */
        private String[] mFlagValues;
        /** Short javadoc (i.e. the first sentence). */
        private String mJavaDoc;
        /** Documentation for deprecated attributes. Null if not deprecated. */
        private String mDeprecatedDoc;

        /**
         * @param name The XML Name of the attribute
         * @param formats The formats of the attribute. Cannot be null.
         *                Should have at least one format.
         */
        public AttributeInfo(String name, Format[] formats) {
            mName = name;
            mFormats = formats;
        }

        /**
         * @param name The XML Name of the attribute
         * @param formats The formats of the attribute. Cannot be null.
         *                Should have at least one format.
         * @param javadoc Short javadoc (i.e. the first sentence).
         */
        public AttributeInfo(String name, Format[] formats, String javadoc) {
            mName = name;
            mFormats = formats;
            mJavaDoc = javadoc;
        }

        public AttributeInfo(AttributeInfo info) {
            mName = info.mName;
            mFormats = info.mFormats;
            mEnumValues = info.mEnumValues;
            mFlagValues = info.mFlagValues;
            mJavaDoc = info.mJavaDoc;
            mDeprecatedDoc = info.mDeprecatedDoc;
        }
        
        /** Returns the XML Name of the attribute */
        public String getName() {
            return mName;
        }
        /** Returns the formats of the attribute. Cannot be null.
         *  Should have at least one format. */
        public Format[] getFormats() {
            return mFormats;
        }
        /** Returns the values for enums. null for other types. */
        public String[] getEnumValues() {
            return mEnumValues;
        }
        /** Returns the values for flags. null for other types. */
        public String[] getFlagValues() {
            return mFlagValues;
        }
        /** Returns a short javadoc, .i.e. the first sentence. */
        public String getJavaDoc() {
            return mJavaDoc;
        }
        /** Returns the documentation for deprecated attributes. Null if not deprecated. */
        public String getDeprecatedDoc() {
            return mDeprecatedDoc;
        }

        /** Sets the values for enums. null for other types. */
        public void setEnumValues(String[] values) {
            mEnumValues = values;
        }
        /** Sets the values for flags. null for other types. */
        public void setFlagValues(String[] values) {
            mFlagValues = values;
        }
        /** Sets a short javadoc, .i.e. the first sentence. */
        public void setJavaDoc(String javaDoc) {
            mJavaDoc = javaDoc;
        }
        /** Sets the documentation for deprecated attributes. Null if not deprecated. */
        public void setDeprecatedDoc(String deprecatedDoc) {
            mDeprecatedDoc = deprecatedDoc;
        }

    }
    
    // --------
    
    /**
     * Creates a new {@link DeclareStyleableInfo}.
     * 
     * @param styleName The name of the style. Should not be empty nor null.
     * @param attributes The initial list of attributes. Can be null.
     */
    public DeclareStyleableInfo(String styleName, AttributeInfo[] attributes) {
        mStyleName = styleName;
        mAttributes = attributes == null ? new AttributeInfo[0] : attributes;
    }
    
    /** Returns style name */
    public String getStyleName() {
        return mStyleName;
    }

    /** Returns the attributes for this view or view group. Maybe empty but not null. */
    public AttributeInfo[] getAttributes() {
        return mAttributes;
    }

    /** Sets the list of attributes for this View or ViewGroup. */
    public void setAttributes(AttributeInfo[] attributes) {
        mAttributes = attributes;
    }
    
    /** Returns a short javadoc */
    public String getJavaDoc() {
        return mJavaDoc;
    }

    /** Sets the javadoc. */
    public void setJavaDoc(String javaDoc) {
        mJavaDoc = javaDoc;
    }

    /** Sets the name of the parents styleable. Can be null. */
    public void setParents(String[] parents) {
        mParents = parents;
    }

    /** Returns the name of the parents styleable. Can be null. */
    public String[] getParents() {
        return mParents;
    }
}
