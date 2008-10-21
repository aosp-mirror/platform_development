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

import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.layoutlib.api.IXmlPullParser;

import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * XmlPullParser implementation on top of {@link UiElementNode}.
 * <p/>It's designed to work on layout files, and will most likely not work on other resource
 * files.
 */
public final class UiElementPullParser implements IXmlPullParser {
    
    private final ArrayList<UiElementNode> mNodeStack = new ArrayList<UiElementNode>();
    private int mParsingState = START_DOCUMENT;
    private UiElementNode mRoot;
    
    public UiElementPullParser(UiElementNode top) {
        mRoot = top;
        push(mRoot);
    }
    
    private UiElementNode getCurrentNode() {
        if (mNodeStack.size() > 0) {
            return mNodeStack.get(mNodeStack.size()-1);
        }
        
        return null;
    }
    
    private Node getAttribute(int i) {
        if (mParsingState != START_TAG) {
            throw new IndexOutOfBoundsException();
        }

        // get the current uiNode
        UiElementNode uiNode = getCurrentNode();
        
        // get its xml node
        Node xmlNode = uiNode.getXmlNode();

        if (xmlNode != null) {
            return xmlNode.getAttributes().item(i);
        }

        return null;
    }
    
    private void push(UiElementNode node) {
        mNodeStack.add(node);
    }
    
    private UiElementNode pop() {
        return mNodeStack.remove(mNodeStack.size()-1);
    }

    // ------------- IXmlPullParser --------

    /**
     * {@inheritDoc}
     * 
     * This implementation returns the underlying DOM node.
     */
    public Object getViewKey() {
        return getCurrentNode();
    }

    // ------------- XmlPullParser --------

    public void setFeature(String name, boolean state) throws XmlPullParserException {
        if (FEATURE_PROCESS_NAMESPACES.equals(name) && state) {
            return;
        }
        if (FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name) && state) {
            return;
        }
        throw new XmlPullParserException("Unsupported feature: " + name);
    }

    public boolean getFeature(String name) {
        if (FEATURE_PROCESS_NAMESPACES.equals(name)) {
            return true;
        }
        if (FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            return true;
        }
        return false;
    }

    public void setProperty(String name, Object value) throws XmlPullParserException {
        throw new XmlPullParserException("setProperty() not supported");
    }

    public Object getProperty(String name) {
        return null;
    }

    public void setInput(Reader in) throws XmlPullParserException {
        throw new XmlPullParserException("setInput() not supported");
    }

    public void setInput(InputStream inputStream, String inputEncoding)
            throws XmlPullParserException {
        throw new XmlPullParserException("setInput() not supported");
    }

    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        throw new XmlPullParserException("defineEntityReplacementText() not supported");
    }

    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        throw new XmlPullParserException("getNamespacePrefix() not supported");
    }

    public String getInputEncoding() {
        return null;
    }

    public String getNamespace(String prefix) {
        throw new RuntimeException("getNamespace() not supported");
    }

    public int getNamespaceCount(int depth) throws XmlPullParserException {
        throw new XmlPullParserException("getNamespaceCount() not supported");
    }

    public String getPositionDescription() {
        return "XML DOM element depth:" + mNodeStack.size();
    }

    public String getNamespaceUri(int pos) throws XmlPullParserException {
        throw new XmlPullParserException("getNamespaceUri() not supported");
    }

    public int getColumnNumber() {
        return -1;
    }

    public int getLineNumber() {
        return -1;
    }

    public int getAttributeCount() {
        UiElementNode node = getCurrentNode();
        if (node != null) {
            return node.getUiAttributes().size();
        }

        return 0;
    }

    public String getAttributeName(int i) {
        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getLocalName();
        }

        return null;
    }

    public String getAttributeNamespace(int i) {
        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getNamespaceURI();
        }
        return ""; //$NON-NLS-1$
    }

    public String getAttributePrefix(int i) {
        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getPrefix();
        }
        return null;
    }

    public String getAttributeType(int arg0) {
        return "CDATA";
    }

    public String getAttributeValue(int i) {
        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getNodeValue();
        }
        
        return null;
    }

    public String getAttributeValue(String namespace, String localName) {
        // get the current uiNode
        UiElementNode uiNode = getCurrentNode();
        
        // get its xml node
        Node xmlNode = uiNode.getXmlNode();
        
        if (xmlNode != null) {
            Node attribute = xmlNode.getAttributes().getNamedItemNS(namespace, localName);
            if (attribute != null) {
                return attribute.getNodeValue();
            }
        }

        return null;
    }

    public int getDepth() {
        return mNodeStack.size();
    }

    public int getEventType() throws XmlPullParserException {
        return mParsingState;
    }

    public String getName() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            return getCurrentNode().getDescriptor().getXmlLocalName();
        }

        return null;
    }

    public String getNamespace() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            return getCurrentNode().getDescriptor().getNamespace();
        }

        return null;
    }

    public String getPrefix() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            // FIXME will NEVER work
            if (getCurrentNode().getDescriptor().getXmlLocalName().startsWith("android:")) { //$NON-NLS-1$
                return "android"; //$NON-NLS-1$
            }
        }

        return null;
    }

    public String getText() {
        return null;
    }

    public char[] getTextCharacters(int[] arg0) {
        return null;
    }

    public boolean isAttributeDefault(int arg0) {
        return false;
    }

    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (mParsingState == START_TAG) {
            return getCurrentNode().getUiChildren().size() == 0;
        }
        
        throw new XmlPullParserException("Must be on START_TAG");
    }

    public boolean isWhitespace() throws XmlPullParserException {
        return false;
    }

    public int next() throws XmlPullParserException, IOException {
        UiElementNode node;
        switch (mParsingState) {
            case END_DOCUMENT:
                throw new XmlPullParserException("Nothing after the end");
            case START_DOCUMENT:
                /* intended fall-through */
            case START_TAG:
                // get the current node, and look for text or children (children first)
                node = getCurrentNode();
                List<UiElementNode> children = node.getUiChildren();
                if (children.size() > 0) {
                    // move to the new child, and don't change the state.
                    push(children.get(0));
                    
                    // in case the current state is CURRENT_DOC, we set the proper state.
                    mParsingState = START_TAG;
                } else {
                    if (mParsingState == START_DOCUMENT) {
                        // this handles the case where there's no node.
                        mParsingState = END_DOCUMENT;
                    } else {
                        mParsingState = END_TAG;
                    }
                }
                break;
            case END_TAG:
                // look for a sibling. if no sibling, go back to the parent
                node = getCurrentNode();
                node = node.getUiNextSibling();
                if (node != null) {
                    // to go to the sibling, we need to remove the current node,
                    pop();
                    // and add its sibling.
                    push(node);
                    mParsingState = START_TAG;
                } else {
                    // move back to the parent
                    pop();
                    
                    // we have only one element left (mRoot), then we're done with the document.
                    if (mNodeStack.size() == 1) {
                        mParsingState = END_DOCUMENT;
                    } else {
                        mParsingState = END_TAG;
                    }
                }
                break;
            case TEXT:
                // not used
                break;
            case CDSECT:
                // not used
                break;
            case ENTITY_REF:
                // not used
                break;
            case IGNORABLE_WHITESPACE:
                // not used
                break;
            case PROCESSING_INSTRUCTION:
                // not used
                break;
            case COMMENT:
                // not used
                break;
            case DOCDECL:
                // not used
                break;
        }
        
        return mParsingState;
    }

    public int nextTag() throws XmlPullParserException, IOException {
        int eventType = next();
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("expected start or end tag", this, null);
        }
        return eventType;
    }

    public String nextText() throws XmlPullParserException, IOException {
        if (getEventType() != START_TAG) {
            throw new XmlPullParserException("parser must be on START_TAG to read next text", this,
                    null);
        }
        int eventType = next();
        if (eventType == TEXT) {
            String result = getText();
            eventType = next();
            if (eventType != END_TAG) {
                throw new XmlPullParserException(
                        "event TEXT it must be immediately followed by END_TAG", this, null);
            }
            return result;
        } else if (eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException("parser must be on START_TAG or TEXT to read text",
                    this, null);
        }
    }

    public int nextToken() throws XmlPullParserException, IOException {
        return next();
    }

    public void require(int type, String namespace, String name) throws XmlPullParserException,
            IOException {
        if (type != getEventType() || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName())))
            throw new XmlPullParserException("expected " + TYPES[type] + getPositionDescription());
    }
}
