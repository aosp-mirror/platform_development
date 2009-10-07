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

package com.android.layoutopt.uix;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.android.layoutopt.uix.xml.XmlDocumentBuilder;

/**
 * Wrapper class for W3C Node objects. Provides extra utilities specific
 * to Android XML layouts.
 */
public class LayoutNode {
    private static final String ANDROID_LAYOUT_WIDTH = "android:layout_width";
    private static final String ANDROID_LAYOUT_HEIGHT = "android:layout_height";
    private static final String VALUE_FILL_PARENT = "fill_parent";
    private static final String VALUE_WRAP_CONTENT = "wrap_content";

    private Map<String, String> mAttributes;
    private final Element mNode;
    private LayoutNode[] mChildren;

    LayoutNode(Node node) {
        if (node == null) throw new IllegalArgumentException("The node cannot be null");
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new IllegalArgumentException("The node must be an element type");
        }
        mNode = (Element) node;
    }

    /**
     * Returns the start line of this node.
     *
     * @return The start line or -1 if the line is unknown.
     */
    public int getStartLine() {
        final Object data = mNode.getUserData(XmlDocumentBuilder.NODE_START_LINE);
        return data == null ? -1 : (Integer) data;
    }

    /**
     * Returns the end line of this node.
     *
     * @return The end line or -1 if the line is unknown.
     */
    public int getEndLine() {
        final Object data = mNode.getUserData(XmlDocumentBuilder.NODE_END_LINE);
        return data == null ? -1 : (Integer) data;
    }

    /**
     * Returns the wrapped W3C XML node object.
     *
     * @return An XML node.
     */
    public Node getNode() {
        return mNode;
    }

    /**
     * Indicates whether the node is of the specified type.
     *
     * @param name The name of the node.
     *
     * @return True if this node has the same name as tagName, false otherwise.
     */
    public boolean is(String name) {
        return mNode.getNodeName().equals(name);
    }

    /**
     * Indicates whether the node has declared the specified attribute.
     *
     * @param attribute The name of the attribute to check.
     *
     * @return True if the attribute is specified, false otherwise.
     */
    public boolean has(String attribute) {
        return mNode.hasAttribute(attribute);
    }

    /**
     * Returns whether this node is the document root.
     *
     * @return True if the wrapped node is the root of the document,
     *         false otherwise.
     */
    public boolean isRoot() {
        return mNode == mNode.getOwnerDocument().getDocumentElement();
    }

    /**
     * Returns whether this node's width is fill_parent.
     */
    public boolean isWidthFillParent() {
        return mNode.getAttribute(ANDROID_LAYOUT_WIDTH).equals(VALUE_FILL_PARENT);
    }

    /**
     * Returns whether this node's width is wrap_content.
     */
    public boolean isWidthWrapContent() {
        return mNode.getAttribute(ANDROID_LAYOUT_WIDTH).equals(VALUE_WRAP_CONTENT);
    }

    /**
     * Returns whether this node's height is fill_parent.
     */
    public boolean isHeightFillParent() {
        return mNode.getAttribute(ANDROID_LAYOUT_HEIGHT).equals(VALUE_FILL_PARENT);
    }

    /**
     * Returns whether this node's height is wrap_content.
     */
    public boolean isHeightWrapContent() {
        return mNode.getAttribute(ANDROID_LAYOUT_HEIGHT).equals(VALUE_WRAP_CONTENT);
    }

    /**
     * Returns a map of all the attributes declared for this node.
     *
     * The name of the attributes contains the namespace.
     *
     * @return A map of [name, value] describing the attributes of this node.
     */
    public Map<String, String> getAttributes() {
        if (mAttributes == null) {
            NamedNodeMap attributes = mNode.getAttributes();
            int count = attributes.getLength();
            mAttributes = new HashMap<String, String>(count);

            for (int i = 0; i < count; i++) {
                Node node = attributes.item(i);
                Attr attribute = (Attr) node;
                mAttributes.put(attribute.getName(), attribute.getValue());
            }
        }

        return mAttributes;
    }

    /**
     * Returns all the children of this node.
     */
    public LayoutNode[] getChildren() {
        if (mChildren == null) {
            NodeList list = mNode.getChildNodes();
            int count = list.getLength();
            List<LayoutNode> children = new ArrayList<LayoutNode>(count);
            for (int i = 0; i < count; i++) {
                Node child = list.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    children.add(new LayoutNode(child));
                }
            }
            mChildren = children.toArray(new LayoutNode[children.size()]);
        }
        return mChildren;
    }
}
