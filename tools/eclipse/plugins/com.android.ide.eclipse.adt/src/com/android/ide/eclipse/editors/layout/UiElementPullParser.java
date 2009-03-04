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

import java.util.ArrayList;
import java.util.List;

/**
 * {@link IXmlPullParser} implementation on top of {@link UiElementNode}.
 * <p/>It's designed to work on layout files, and will most likely not work on other resource
 * files.
 */
public final class UiElementPullParser extends BasePullParser {
    
    private final ArrayList<UiElementNode> mNodeStack = new ArrayList<UiElementNode>();
    private UiElementNode mRoot;
    
    public UiElementPullParser(UiElementNode top) {
        super();
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

    public String getPositionDescription() {
        return "XML DOM element depth:" + mNodeStack.size();
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

    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (mParsingState == START_TAG) {
            return getCurrentNode().getUiChildren().size() == 0;
        }
        
        throw new XmlPullParserException("Call to isEmptyElementTag while not in START_TAG",
                this, null);
    }
    
    @Override
    public void onNextFromStartDocument() {
        onNextFromStartTag();
    }
    
    @Override
    public void onNextFromStartTag() {
        // get the current node, and look for text or children (children first)
        UiElementNode node = getCurrentNode();
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
    }
    
    @Override
    public void onNextFromEndTag() {
        // look for a sibling. if no sibling, go back to the parent
        UiElementNode node = getCurrentNode();
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
    }
}
