/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.tests.functests.sampleProjects;

import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.xml.AndroidManifest;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Helper class for modifying an AndroidManifest.
 * <p/>
 * TODO: consider merging this with AndroidManifestParser.
 */
class AndroidManifestWriter {

    private static final Logger sLogger = Logger.getLogger(AndroidManifestWriter.class.getName());

    private final Document mDoc;
    private final String mOsManifestFilePath;

    private AndroidManifestWriter(Document doc, String osManifestFilePath) {
        mDoc = doc;
        mOsManifestFilePath = osManifestFilePath;
    }

    /**
     * Sets the minimum SDK version for this manifest
     * @param minSdkVersion - the minimim sdk version to use
     * @returns <code>true</code> on success, false otherwise
     */
    public boolean setMinSdkVersion(String minSdkVersion) {
        Element usesSdkElement = null;
        NodeList nodeList = mDoc.getElementsByTagName(AndroidManifest.NODE_USES_SDK);
        if (nodeList.getLength() > 0) {
            usesSdkElement = (Element) nodeList.item(0);
        } else {
            usesSdkElement = mDoc.createElement(AndroidManifest.NODE_USES_SDK);
            mDoc.getDocumentElement().appendChild(usesSdkElement);
        }
        Attr minSdkAttr = mDoc.createAttributeNS(SdkConstants.NS_RESOURCES,
                AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION);
        String prefix = mDoc.lookupPrefix(SdkConstants.NS_RESOURCES);
        minSdkAttr.setPrefix(prefix);
        minSdkAttr.setValue(minSdkVersion);
        usesSdkElement.setAttributeNodeNS(minSdkAttr);
        return saveXmlToFile();
    }

    private boolean saveXmlToFile() {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(mDoc);

            // Prepare the output file
            File file = new File(mOsManifestFilePath);
            Result result = new StreamResult(file);

            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            sLogger.log(Level.SEVERE, "Failed to write xml file", e);
            return false;
        } catch (TransformerException e) {
            sLogger.log(Level.SEVERE, "Failed to write xml file", e);
            return false;
        }
        return true;
    }

    /**
     * Parses the manifest file, and collects data.
     *
     * @param osManifestFilePath The OS path of the manifest file to parse.
     * @return an {@link AndroidManifestParser} or null if parsing failed
     */
    public static AndroidManifestWriter parse(String osManifestFilePath) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(osManifestFilePath);
            return new AndroidManifestWriter(doc, osManifestFilePath);
        } catch (ParserConfigurationException e) {
            sLogger.log(Level.SEVERE, "Error parsing file", e);
            return null;
        } catch (SAXException e) {
            sLogger.log(Level.SEVERE, "Error parsing file", e);
            return null;
        } catch (IOException e) {
            sLogger.log(Level.SEVERE, "Error parsing file", e);
            return null;
        }
    }
}
