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

package com.android.sdklib.internal.repository;

import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * An sdk-repository source, i.e. a download site.
 * It may be a full repository or an add-on only repository.
 * A repository describes one or {@link Package}s available for download.
 */
public class RepoSource implements IDescription {

    private final String mUrl;
    private final boolean mAddonOnly;

    private Package[] mPackages;
    private String mDescription;

    /**
     * Constructs a new source for the given repository URL.
     */
    public RepoSource(String url, boolean addonOnly) {
        mUrl = url;
        mAddonOnly = addonOnly;
        setDefaultDescription();
    }

    /** Returns the URL of the source repository. */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Returns the list of known packages found by the last call to {@link #load(ITaskFactory)}.
     * This is null when the source hasn't been loaded yet.
     */
    public Package[] getPackages() {
        return mPackages;
    }

    /**
     * Clear the internal packages list. After this call, {@link #getPackages()} will return
     * null till {@link #load(ITaskFactory)} is called.
     */
    public void clearPackages() {
        mPackages = null;
    }

    public String getShortDescription() {
        return mUrl;
    }

    public String getLongDescription() {
        return mDescription == null ? "" : mDescription;  //$NON-NLS-1$
    }

    /**
     * Tries to fetch the repository index for the given URL.
     */
    public void load(ITaskFactory taskFactory) {

        taskFactory.start("Init SDK Updater", new ITask() {
            public void run(ITaskMonitor monitor) {
                monitor.setProgressMax(4);

                setDefaultDescription();

                monitor.setDescription("Fetching %1$s", mUrl);
                monitor.incProgress(1);

                String xml = fetchUrl(mUrl, monitor);

                if (xml == null) {
                    mDescription += String.format("\nFailed to fetch URL %1$s", mUrl);
                    return;
                }

                monitor.setDescription("Validate XML");
                monitor.incProgress(1);

                if (!validateXml(xml, monitor)) {
                    mDescription += String.format("\nFailed to validate XML at %1$s", mUrl);
                    return;
                }

                monitor.setDescription("Parse XML");
                monitor.incProgress(1);
                parsePackages(xml, monitor);
                if (mPackages.length == 0) {
                    mDescription += "\nNo packages found.";
                } else if (mPackages.length == 1) {
                    mDescription += "\nOne package found.";
                } else {
                    mDescription += String.format("\n%1$d packages found.", mPackages.length);
                }

                // done
                monitor.incProgress(1);
            }
        });
    }

    private void setDefaultDescription() {
        if (mAddonOnly) {
            mDescription = String.format("Add-on Source: %1$s", mUrl);
        } else {
            mDescription = String.format("SDK Source: %1$s", mUrl);
        }
    }

    /**
     * Fetches the document at the given URL and returns it as a string.
     * Returns null if anything wrong happens and write errors to the monitor.
     *
     * References:
     * Java URL Connection: http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html
     * Java URL Reader: http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html
     * Java set Proxy: http://java.sun.com/docs/books/tutorial/networking/urls/_setProxy.html
     */
    private String fetchUrl(String urlString, ITaskMonitor monitor) {
        URL url;
        try {
            url = new URL(urlString);

            StringBuilder xml = new StringBuilder();
            InputStream is = null;
            BufferedReader br = null;
            try {
                is = url.openStream();
                br = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = br.readLine()) != null) {
                    xml.append(line);
                }

                return xml.toString();

            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // pass
                    }
                }

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // pass
                    }
                }
            }

        } catch (IOException e) {
            monitor.setResult(e.getMessage());
        }

        return null;
    }

    /**
     * Validates this XML against the SDK Repository schema.
     * Returns true if the XML was correctly validated.
     */
    private boolean validateXml(String xml, ITaskMonitor monitor) {

        try {
            Validator validator = getValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return true;

        } catch (SAXException e) {
            monitor.setResult(e.getMessage());

        } catch (IOException e) {
            monitor.setResult(e.getMessage());
        }

        return false;
    }

    /**
     * Helper method that returns a validator for our XSD
     */
    private Validator getValidator() throws SAXException {
        InputStream xsdStream = SdkRepository.getXsdStream();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // This may throw a SAX Exception if the schema itself is not a valid XSD
        Schema schema = factory.newSchema(new StreamSource(xsdStream));

        Validator validator = schema.newValidator();

        return validator;
    }


    /**
     * Parse all packages defined in the SDK Repository XML and creates
     * a new mPackages array with them.
     */
    private boolean parsePackages(String xml, ITaskMonitor monitor) {

        try {
            Document doc = getDocument(xml);

            Node root = getFirstChild(doc, SdkRepository.NODE_SDK_REPOSITORY);
            if (root != null) {

                ArrayList<Package> packages = new ArrayList<Package>();

                for (Node child = root.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE &&
                            SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI())) {
                        String name = child.getLocalName();
                        Package p = null;

                        try {
                            if (SdkRepository.NODE_ADD_ON.equals(name)) {
                                p = new AddonPackage(this, child);

                            } else if (!mAddonOnly) {
                                if (SdkRepository.NODE_PLATFORM.equals(name)) {
                                    p = new PlatformPackage(this, child);
                                } else if (SdkRepository.NODE_DOC.equals(name)) {
                                    p = new DocPackage(this, child);
                                } else if (SdkRepository.NODE_TOOL.equals(name)) {
                                    p = new ToolPackage(this, child);
                                }
                            }

                            if (p != null) {
                                packages.add(p);
                                monitor.setDescription("Found %1$s", p.getShortDescription());
                            }
                        } catch (Exception e) {
                            // Ignore invalid packages
                        }
                    }
                }

                mPackages = packages.toArray(new Package[packages.size()]);

                return true;
            }

        } catch (ParserConfigurationException e) {
            monitor.setResult("Failed to create XML document builder for %1$s");

        } catch (SAXException e) {
            monitor.setResult("Failed to parse XML document %1$s");

        } catch (IOException e) {
            monitor.setResult("Failed to read XML document");
        }

        return false;
    }

    /**
     * Returns the first child element with the given XML local name.
     * If xmlLocalName is null, returns the very first child element.
     */
    private Node getFirstChild(Node node, String xmlLocalName) {

        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI())) {
                if (xmlLocalName == null || child.getLocalName().equals(xmlLocalName)) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Takes an XML document as a string as parameter and returns a DOM for it.
     */
    private Document getDocument(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        return doc;
    }
}
