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

package com.android.ide.eclipse.editors.layout;

/**
 * A bunch of constants that map to either:
 * <ul>
 * <li>Android Layouts XML element names (Linear, Relative, Absolute, etc.)
 * <li>Attributes for layout XML elements. 
 * <li>Values for attributes.
 * </ul>
 */
public class LayoutConstants {

    public static final String RELATIVE_LAYOUT = "RelativeLayout";      //$NON-NLS-1$
    public static final String LINEAR_LAYOUT   = "LinearLayout";        //$NON-NLS-1$
    public static final String ABSOLUTE_LAYOUT = "AbsoluteLayout";      //$NON-NLS-1$

    public static final String ATTR_TEXT = "text";                      //$NON-NLS-1$
    public static final String ATTR_ID = "id";                          //$NON-NLS-1$

    public static final String ATTR_LAYOUT_HEIGHT = "layout_height";    //$NON-NLS-1$
    public static final String ATTR_LAYOUT_WIDTH = "layout_width";      //$NON-NLS-1$

    public static final String ATTR_LAYOUT_ALIGN_PARENT_TOP = "layout_alignParentTop"; //$NON-NLS-1$
    public static final String ATTR_LAYOUT_ALIGN_PARENT_BOTTOM = "layout_alignParentBottom"; //$NON-NLS-1$
    public static final String ATTR_LAYOUT_ALIGN_PARENT_LEFT = "layout_alignParentLeft";//$NON-NLS-1$
    public static final String ATTR_LAYOUT_ALIGN_PARENT_RIGHT = "layout_alignParentRight";   //$NON-NLS-1$
    
    public static final String ATTR_LAYOUT_ALIGN_BASELINE = "layout_alignBaseline"; //$NON-NLS-1$

    public static final String ATTR_LAYOUT_CENTER_VERTICAL = "layout_centerVertical"; //$NON-NLS-1$
    public static final String ATTR_LAYOUT_CENTER_HORIZONTAL = "layout_centerHorizontal"; //$NON-NLS-1$
    
    public static final String ATTR_LAYOUT_TO_RIGHT_OF = "layout_toRightOf";    //$NON-NLS-1$
    public static final String ATTR_LAYOUT_TO_LEFT_OF = "layout_toLeftOf";      //$NON-NLS-1$
    
    public static final String ATTR_LAYOUT_BELOW = "layout_below";              //$NON-NLS-1$
    public static final String ATTR_LAYOUT_ABOVE = "layout_above";              //$NON-NLS-1$
    
    public static final String ATTR_LAYOUT_Y = "layout_y";                      //$NON-NLS-1$
    public static final String ATTR_LAYOUT_X = "layout_x";                      //$NON-NLS-1$

    public static final String VALUE_WRAP_CONTENT = "wrap_content";             //$NON-NLS-1$
    public static final String VALUE_FILL_PARENT = "fill_parent";               //$NON-NLS-1$
    public static final String VALUE_TRUE = "true";                             //$NON-NLS-1$
    public static final String VALUE_N_DIP = "%ddip";                           //$NON-NLS-1$

    private LayoutConstants() {
    }
}
