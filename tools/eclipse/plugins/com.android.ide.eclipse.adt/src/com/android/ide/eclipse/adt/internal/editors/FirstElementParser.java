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

package com.android.ide.eclipse.adt.internal.editors;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Quickly parses a (potential) XML file to extract its first element (i.e. the root element)
 * and namespace, if any.
 * <p/>
 * This is used to determine if a file is an XML document that the XmlEditor can process.
 * <p/>
 * TODO use this to remove the hardcoded "android" namespace prefix limitation.
 */
public final class FirstElementParser {
    
    private static SAXParserFactory sSaxfactory;
    
    /**
     * Result from the XML parsing. <br/>
     * Contains the name of the root XML element. <br/>
     * If an XMLNS URI was specified and found, the XMLNS prefix is recorded. Otherwise it is null.
     */
    public static final class Result {
        private String mElement;
        private String mXmlnsPrefix;
        private String mXmlnsUri;
        
        public String getElement() {
            return mElement;
        }
        
        public String getXmlnsPrefix() {
            return mXmlnsPrefix;
        }
        
        public String getXmlnsUri() {
            return mXmlnsUri;
        }
        
        void setElement(String element) {
            mElement = element;
        }
        
        void setXmlnsPrefix(String xmlnsPrefix) {
            mXmlnsPrefix = xmlnsPrefix;
        }
        
        void setXmlnsUri(String xmlnsUri) {
            mXmlnsUri = xmlnsUri;
        }
    }
    
    private static class ResultFoundException extends SAXException { }
    
    /**
     * Parses the given filename.
     * 
     * @param osFilename The file to parse.
     * @param xmlnsUri An optional URL of which we want to know the prefix. 
     * @return The element details found or null if not found.
     */
    public static Result parse(String osFilename, String xmlnsUri) {
        if (sSaxfactory == null) {
            // TODO just create a single factory in CommonPlugin and reuse it
            sSaxfactory = SAXParserFactory.newInstance();
            sSaxfactory.setNamespaceAware(true);
        }

        Result result = new Result();
        if (xmlnsUri != null && xmlnsUri.length() > 0) {
            result.setXmlnsUri(xmlnsUri);
        }

        try {
            SAXParser parser = sSaxfactory.newSAXParser();
            XmlHandler handler = new XmlHandler(result);
            parser.parse(new InputSource(new FileReader(osFilename)), handler);

        } catch(ResultFoundException e) {
            // XML handling was aborted because the required element was found.
            // Simply return the result.
            return result;
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        return null;
    }

    /**
     * Private constructor. Use the static parse() method instead.
     */
    private FirstElementParser() {
        // pass
    }
    
    /**
     * A specialized SAX handler that captures the arguments of the very first element
     * (i.e. the root element)
     */
    private static class XmlHandler extends DefaultHandler {
        private final Result mResult;

        public XmlHandler(Result result) {
            mResult = result;
        }
        
        /**
         * Processes a namespace prefix mapping.
         * I.e. for xmlns:android="some-uri", this received prefix="android" and uri="some-uri".
         * <p/>
         * The prefix is recorded in the result structure if the URI is the one searched for.
         * <p/>
         * This event happens <em>before</em> the corresponding startElement event.
         */
        @Override
        public void startPrefixMapping(String prefix, String uri) {
            if (uri.equals(mResult.getXmlnsUri())) {
                mResult.setXmlnsPrefix(prefix);
            }
        }

        /**
         * Processes a new element start.
         * <p/>
         * This simply records the element name and abort processing by throwing an exception.
         */
        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes)
            throws SAXException {
            mResult.setElement(localName);
            throw new ResultFoundException();
        }
    }

}
