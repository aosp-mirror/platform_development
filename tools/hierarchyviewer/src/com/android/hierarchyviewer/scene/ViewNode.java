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

package com.android.hierarchyviewer.scene;

import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ViewNode {
    public String id;
    public String name;

    public List<Property> properties = new ArrayList<Property>();
    public Map<String, Property> namedProperties = new HashMap<String, Property>();

    public ViewNode parent;
    public List<ViewNode> children = new ArrayList<ViewNode>();

    public Image image;
    
    public int left;
    public int top;
    public int width;
    public int height;
    public int scrollX;
    public int scrollY;
    public int paddingLeft;
    public int paddingRight;
    public int paddingTop;
    public int paddingBottom;
    public int marginLeft;
    public int marginRight;
    public int marginTop;
    public int marginBottom;
    public int baseline;
    public boolean willNotDraw;
    public boolean hasMargins;
    
    boolean hasFocus;
    int index;

    public boolean decoded;
    public boolean filtered;

    private String shortName;
    private StateListener listener;

    void decode() {
        id = namedProperties.get("mID").value;

        left = getInt("mLeft", 0);
        top = getInt("mTop", 0);
        width = getInt("getWidth()", 0);
        height = getInt("getHeight()", 0);
        scrollX = getInt("mScrollX", 0);
        scrollY = getInt("mScrollY", 0);
        paddingLeft = getInt("mPaddingLeft", 0);
        paddingRight = getInt("mPaddingRight", 0);
        paddingTop = getInt("mPaddingTop", 0);
        paddingBottom = getInt("mPaddingBottom", 0);
        marginLeft = getInt("layout_leftMargin", Integer.MIN_VALUE);
        marginRight = getInt("layout_rightMargin", Integer.MIN_VALUE);
        marginTop = getInt("layout_topMargin", Integer.MIN_VALUE);
        marginBottom = getInt("layout_bottomMargin", Integer.MIN_VALUE);
        baseline = getInt("getBaseline()", 0);
        willNotDraw = getBoolean("willNotDraw()", false);
        hasFocus = getBoolean("hasFocus()", false);

        hasMargins = marginLeft != Integer.MIN_VALUE &&
                marginRight != Integer.MIN_VALUE &&
                marginTop != Integer.MIN_VALUE &&
                marginBottom != Integer.MIN_VALUE;

        decoded = true;
    }

    private boolean getBoolean(String name, boolean defaultValue) {
        Property p = namedProperties.get(name);
        if (p != null) {
            try {
                return Boolean.parseBoolean(p.value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }   
        }
        return defaultValue;
    }

    private int getInt(String name, int defaultValue) {
        Property p = namedProperties.get(name);
        if (p != null) {
            try {
                return Integer.parseInt(p.value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public void filter(Pattern pattern) {
        if (pattern == null || pattern.pattern().length() == 0) {
            filtered = false;
        } else {
            filtered = pattern.matcher(shortName).find() || pattern.matcher(id).find();
        }
        listener.nodeStateChanged(this);
    }

    void computeIndex() {
        index = parent == null ? 0 : parent.children.indexOf(this);
        listener.nodeIndexChanged(this);
    }

    void setShortName(String shortName) {
        this.shortName = shortName;
    }

    void setStateListener(StateListener listener) {
        this.listener = listener;
    }

    @SuppressWarnings({"StringEquality"})
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ViewNode other = (ViewNode) obj;
        return !(this.name != other.name && (this.name == null || !this.name.equals(other.name)));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    public static class Property {
        public String name;
        public String value;

        @Override
        public String toString() {
            return name + '=' + value;
        }
        
        @SuppressWarnings({"StringEquality"})
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Property other = (Property) obj;
            if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
                return false;
            }
            return !(this.value != other.value && (this.value == null || !this.value.equals(other.value)));
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 61 * hash + (this.value != null ? this.value.hashCode() : 0);
            return hash;
        }
    }

    interface StateListener {
        void nodeStateChanged(ViewNode node);
        void nodeIndexChanged(ViewNode node);
    }
}
