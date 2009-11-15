/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.layoutopt.uix.groovy;

import com.android.layoutopt.uix.LayoutAnalysis;
import com.android.layoutopt.uix.xml.XmlDocumentBuilder;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import groovy.lang.GString;
import groovy.xml.dom.DOMCategory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

/**
 * Support class for Groovy rules. This class adds new Groovy capabilities
 * to {@link com.android.layoutopt.uix.LayoutAnalysis} and {@link org.w3c.dom.Node}.
 */
public class LayoutAnalysisCategory {
    private static final String ANDROID_PADDING = "android:padding";
    private static final String ANDROID_PADDING_LEFT = "android:paddingLeft";
    private static final String ANDROID_PADDING_TOP = "android:paddingTop";
    private static final String ANDROID_PADDING_RIGHT = "android:paddingRight";
    private static final String ANDROID_PADDING_BOTTOM = "android:paddingBottom";
    private static final String ANDROID_LAYOUT_WIDTH = "android:layout_width";
    private static final String ANDROID_LAYOUT_HEIGHT = "android:layout_height";
    private static final String VALUE_FILL_PARENT = "fill_parent";
    private static final String VALUE_WRAP_CONTENT = "wrap_content";
    
    private static final String[] sContainers = new String[] {
            "FrameLayout", "LinearLayout", "RelativeLayout", "SlidingDrawer",
            "AbsoluteLayout", "TableLayout", "Gallery", "GridView", "ListView",
            "RadioGroup", "ScrollView", "HorizontalScrollView", "Spinner",
            "ViewSwitcher", "ViewFlipper", "ViewAnimator", "ImageSwitcher",
            "TextSwitcher", "android.gesture.GestureOverlayView", "TabHost"
    };
    static {
        Arrays.sort(sContainers);
    }

    /**
     * xmlNode.isContainer()
     * 
     * @return True if the specified node corresponds to a container widget.
     */
    public static boolean isContainer(Element element) {
        return Arrays.binarySearch(sContainers, element.getNodeName()) >= 0;
    }

    /**
     * xmlNode.all()
     * 
     * Same as xmlNode.'**' but excludes xmlNode from the results.
     * 
     * @return All descendants, this node excluded.
     */
    public static List<Node> all(Element element) {
        NodeList list = DOMCategory.depthFirst(element);
        int count = list.getLength();
        List<Node> nodes = new ArrayList<Node>(count - 1);
        for (int i = 1; i < count; i++) {
            nodes.add(list.item(i));
        }
        return nodes;
    }

    /**
     * Returns the start line of this node.
     *
     * @return The start line or -1 if the line is unknown.
     */
    public static int getStartLine(Node node) {
        final Object data = node == null ? null :
                node.getUserData(XmlDocumentBuilder.NODE_START_LINE);
        return data == null ? -1 : (Integer) data;
    }

    /**
     * Returns the end line of this node.
     *
     * @return The end line or -1 if the line is unknown.
     */
    public static int getEndLine(Node node) {
        final Object data = node == null ? null :
                node.getUserData(XmlDocumentBuilder.NODE_END_LINE);
        return data == null ? -1 : (Integer) data;
    }

    /**
     * xmlNode.hasPadding()
     * 
     * @return True if the node has one ore more padding attributes.
     */
    public static boolean hasPadding(Element element) {
        return element.getAttribute(ANDROID_PADDING).length() > 0 ||
                element.getAttribute(ANDROID_PADDING_LEFT).length() > 0 ||
                element.getAttribute(ANDROID_PADDING_TOP).length() > 0 ||
                element.getAttribute(ANDROID_PADDING_BOTTOM).length() > 0 ||
                element.getAttribute(ANDROID_PADDING_RIGHT).length() > 0;
    }

    /**
     * Returns whether this node's width is fill_parent.
     */
    public static boolean isWidthFillParent(Element element) {
        return element.getAttribute(ANDROID_LAYOUT_WIDTH).equals(VALUE_FILL_PARENT);
    }

    /**
     * Returns whether this node's width is wrap_content.
     */
    public static boolean isWidthWrapContent(Element element) {
        return element.getAttribute(ANDROID_LAYOUT_WIDTH).equals(VALUE_WRAP_CONTENT);
    }

    /**
     * Returns whether this node's height is fill_parent.
     */
    public static boolean isHeightFillParent(Element element) {
        return element.getAttribute(ANDROID_LAYOUT_HEIGHT).equals(VALUE_FILL_PARENT);
    }

    /**
     * Returns whether this node's height is wrap_content.
     */
    public static boolean isHeightWrapContent(Element element) {
        return element.getAttribute(ANDROID_LAYOUT_HEIGHT).equals(VALUE_WRAP_CONTENT);
    }

    /**
     * xmlNode.isRoot()
     * 
     * @return True if xmlNode is the root of the document, false otherwise
     */
    public static boolean isRoot(Node node) {
        return node.getOwnerDocument().getDocumentElement() == node;
    }

    /**
     * xmlNode.is("tagName")
     * 
     * @return True if xmlNode.getNodeName().equals(name), false otherwise.
     */
    public static boolean is(Node node, String name) {
        return node.getNodeName().equals(name);
    }

    /**
     * xmlNode.depth()
     * 
     * @return The maximum depth of the node.
     */
    public static int depth(Node node) {
        int maxDepth = 0;
        NodeList list = node.getChildNodes();
        int count = list.getLength();

        for (int i = 0; i < count; i++) {
            maxDepth = Math.max(maxDepth, depth(list.item(i)));
        }

        return maxDepth + 1;
    }

    /**
     * analysis << "The issue"
     * 
     * @return The analysis itself to chain calls.
     */
    public static LayoutAnalysis leftShift(LayoutAnalysis analysis, GString description) {
        analysis.addIssue(description.toString());
        return analysis;
    }

    /**
     * analysis << "The issue"
     * 
     * @return The analysis itself to chain calls.
     */
    public static LayoutAnalysis leftShift(LayoutAnalysis analysis, String description) {
        analysis.addIssue(description);
        return analysis;
    }

    /**
     * analysis << [node: node, description: "The issue"]
     * 
     * @return The analysis itself to chain calls.
     */
    public static LayoutAnalysis leftShift(LayoutAnalysis analysis, Map issue) {
        analysis.addIssue((Node) issue.get("node"), issue.get("description").toString());
        return analysis;
    }
}
