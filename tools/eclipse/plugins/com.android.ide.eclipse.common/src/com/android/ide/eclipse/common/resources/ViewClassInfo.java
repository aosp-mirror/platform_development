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

import com.android.ide.eclipse.common.resources.DeclareStyleableInfo.AttributeInfo;

/**
 * Information needed to represent a View or ViewGroup (aka Layout) item
 * in the layout hierarchy, as extracted from the main android.jar and the
 * associated attrs.xml.
 */
public class ViewClassInfo {
    /** Is this a layout class (i.e. ViewGroup) or just a view? */
    private boolean mIsLayout;
    /** FQCN e.g. android.view.View, never null. */
    private String mCanonicalClassName;
    /** Short class name, e.g. View, never null. */
    private String mShortClassName;
    /** Super class. Can be null. */
    private ViewClassInfo mSuperClass;
    /** Short javadoc. Can be null. */
    private String mJavaDoc;    
    /** Attributes for this view or view group. Can be empty but never null. */
    private AttributeInfo[] mAttributes;
    
    public static class LayoutParamsInfo {
        /** Short class name, e.g. LayoutData, never null. */
        private String mShortClassName;
        /** ViewLayout class info owning this layout data */
        private ViewClassInfo mViewLayoutClass;
        /** Super class. Can be null. */
        private LayoutParamsInfo mSuperClass; 
        /** Layout Data Attributes for layout classes. Can be empty but not null. */
        private AttributeInfo[] mAttributes;

        public LayoutParamsInfo(ViewClassInfo enclosingViewClassInfo,
                String shortClassName, LayoutParamsInfo superClassInfo) {
            mShortClassName = shortClassName;
            mViewLayoutClass = enclosingViewClassInfo;
            mSuperClass = superClassInfo;
            mAttributes = new AttributeInfo[0];
        }
        
        /** Returns short class name, e.g. "LayoutData" */
        public String getShortClassName() {
            return mShortClassName;
        }
        /** Returns the ViewLayout class info enclosing this layout data. Cannot null. */
        public ViewClassInfo getViewLayoutClass() {
            return mViewLayoutClass;
        }
        /** Returns the super class info. Can be null. */
        public LayoutParamsInfo getSuperClass() {
            return mSuperClass;
        }
        /** Returns the LayoutData attributes. Can be empty but not null. */
        public AttributeInfo[] getAttributes() {
            return mAttributes;
        }
        /** Sets the LayoutData attributes. Can be empty but not null. */
        public void setAttributes(AttributeInfo[] attributes) {
            mAttributes = attributes;
        }
    }

    /** Layout data info for a layout class. Null for all non-layout classes and always
     *  non-null for a layout class. */
    public LayoutParamsInfo mLayoutData;

    // --------
    
    public ViewClassInfo(boolean isLayout, String canonicalClassName, String shortClassName) {
        mIsLayout = isLayout;
        mCanonicalClassName = canonicalClassName;
        mShortClassName = shortClassName;
        mAttributes = new AttributeInfo[0];
    }
    
    /** Returns whether this is a layout class (i.e. ViewGroup) or just a View */
    public boolean isLayout() {
        return mIsLayout;
    }

    /** Returns FQCN e.g. "android.view.View" */
    public String getCanonicalClassName() {
        return mCanonicalClassName;
    }

    /** Returns short class name, e.g. "View" */
    public String getShortClassName() {
        return mShortClassName;
    }

    /** Returns the super class. Can be null. */
    public ViewClassInfo getSuperClass() {
        return mSuperClass;
    }

    /** Returns a short javadoc */
    public String getJavaDoc() {
        return mJavaDoc;
    }

    /** Returns the attributes for this view or view group. Maybe empty but not null. */
    public AttributeInfo[] getAttributes() {
        return mAttributes;
    }

    /** Returns the LayoutData info for layout classes. Null for non-layout view classes. */
    public LayoutParamsInfo getLayoutData() {
        return mLayoutData;
    }

    /**
     * Sets a link on the info of the super class of this View or ViewGroup.
     * <p/>
     * The super class info must be of the same kind (i.e. group to group or view to view)
     * except for the top ViewGroup which links to the View info.
     * <p/>
     * The super class cannot be null except for the top View info.
     */
    public void setSuperClass(ViewClassInfo superClass) {
        mSuperClass = superClass;
    }

    /** Sets the javadoc for this View or ViewGroup. */
    public void setJavaDoc(String javaDoc) {
        mJavaDoc = javaDoc;
    }

    /** Sets the list of attributes for this View or ViewGroup. */
    public void setAttributes(AttributeInfo[] attributes) {
        mAttributes = attributes;
    }

    /**
     * Sets the {@link LayoutParamsInfo} for layout classes.
     * Does nothing for non-layout view classes.
     */
    public void setLayoutParams(LayoutParamsInfo layoutData) {
        if (mIsLayout) {
            mLayoutData = layoutData;
        }
    }
}
