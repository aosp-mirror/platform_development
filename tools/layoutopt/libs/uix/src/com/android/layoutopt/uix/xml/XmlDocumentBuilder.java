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

package com.android.layoutopt.uix.xml;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

/**
 * Parses XML documents. This class tries to add meta-data in the resulting DOM
 * trees to indicate the start and end line numbers of each node.
 */
public class XmlDocumentBuilder {
    /**
     * Name of the node user data containing the start line number of the node.
     * 
     * @see Node#getUserData(String)
     */
    public static final String NODE_START_LINE = "startLine";

    /**
     * Name of the node user data containing the end line number of the node.
     *
     * @see Node#getUserData(String)
     */
    public static final String NODE_END_LINE = "endLine";

    private final DocumentBuilder mBuilder;
    private boolean mHasLineNumbersSupport;

    /**
     * Creates a new XML document builder.
     */
    public XmlDocumentBuilder() {
        try {
            Class.forName("com.sun.org.apache.xerces.internal.parsers.DOMParser");
            mHasLineNumbersSupport = true;
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        if (!mHasLineNumbersSupport) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                mBuilder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Could not initialize the XML parser");
            }
        } else {
            mBuilder = null;
        }
    }

    /**
     * Indicates whether the XML documents created by this class are annotated
     * with line numbers.
     *
     * @return True if the parsed documents contain line numbers meta-data,
     *         false otherwise.
     *
     * @see #NODE_START_LINE
     * @see #NODE_END_LINE
     */
    public boolean isHasLineNumbersSupport() {
        return mHasLineNumbersSupport;
    }

    public Document parse(InputStream inputStream) throws SAXException, IOException {
        if (!mHasLineNumbersSupport) {
            return mBuilder.parse(inputStream);
        } else {
            DOMParser parser = new LineNumberDOMParser();
            parser.parse(new InputSource(inputStream));
            return parser.getDocument();
        }
    }

    public Document parse(String content) throws SAXException, IOException {
        if (!mHasLineNumbersSupport) {
            return mBuilder.parse(content);
        } else {
            DOMParser parser = new LineNumberDOMParser();
            parser.parse(content);
            return parser.getDocument();
        }
    }

    public Document parse(File file) throws SAXException, IOException {
        return parse(new FileInputStream(file));
    }

    private static class LineNumberDOMParser extends DOMParser {
        private static final String FEATURE_NODE_EXPANSION =
                "http://apache.org/xml/features/dom/defer-node-expansion";
        private static final String CURRENT_NODE =
                "http://apache.org/xml/properties/dom/current-element-node";

        private XMLLocator mLocator;
        private LinkedList<Node> mStack = new LinkedList<Node>();

        private LineNumberDOMParser() {
            try {
                setFeature(FEATURE_NODE_EXPANSION, false);
            } catch (SAXNotRecognizedException e) {
                e.printStackTrace();
            } catch (SAXNotSupportedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void startDocument(XMLLocator xmlLocator, String s,
                NamespaceContext namespaceContext, Augmentations augmentations)
                throws XNIException {
            super.startDocument(xmlLocator, s, namespaceContext, augmentations);

            mLocator = xmlLocator;
            mStack.add(setNodeLineNumber(NODE_START_LINE));
        }

        private Node setNodeLineNumber(String tag) {
            Node node = null;
            try {
                node = (Node) getProperty(CURRENT_NODE);
            } catch (SAXNotRecognizedException e) {
                e.printStackTrace();
            } catch (SAXNotSupportedException e) {
                e.printStackTrace();
            }

            if (node != null) {
                node.setUserData(tag, mLocator.getLineNumber(), null);
            }

            return node;
        }

        @Override
        public void startElement(QName qName, XMLAttributes xmlAttributes,
                Augmentations augmentations) throws XNIException {
            super.startElement(qName, xmlAttributes, augmentations);
            mStack.add(setNodeLineNumber(NODE_START_LINE));            
        }

        @Override
        public void endElement(QName qName, Augmentations augmentations) throws XNIException {
            super.endElement(qName, augmentations);
            Node node = mStack.removeLast();
            if (node != null) {
                node.setUserData(NODE_END_LINE, mLocator.getLineNumber(), null);
            }
        }
    }
}
