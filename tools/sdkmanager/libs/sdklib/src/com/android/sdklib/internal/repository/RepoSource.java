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
 * An sdk-repository source. It may be a full repository or an add-on only repository.
 */
public class RepoSource implements IDescription {

    private final String mUrl;
    private final boolean mAddonOnly;

    private ArrayList<String> mPackages;
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
     * Returns the list of known packages. This is null when the source hasn't been loaded yet.
     */
    public ArrayList<String> getPackages() {
        return mPackages;
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

                monitor.setDescription(String.format("Fetching %1$s", mUrl));
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
                if (mPackages.size() == 0) {
                    mDescription += "\nNo packages found.";
                } else if (mPackages.size() == 1) {
                    mDescription += "\nOne package found.";
                } else {
                    mDescription += String.format("\n%1$d packages found.", mPackages.size());
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

    /*
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

    /** Helper method that returns a validator for our XSD */
    private Validator getValidator() throws SAXException {
        InputStream xsdStream = SdkRepository.getXsdStream();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // This may throw a SAX Exception if the schema itself is not a valid XSD
        Schema schema = factory.newSchema(new StreamSource(xsdStream));

        Validator validator = schema.newValidator();

        return validator;
    }


    private boolean parsePackages(String xml, ITaskMonitor monitor) {

        try {
            Document doc = getDocument(xml);

            Node root = getFirstChild(doc, SdkRepository.NODE_SDK_REPOSITORY);
            if (root != null) {

                mPackages = new ArrayList<String>();

                for (Node child = root.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE &&
                            child.getNamespaceURI().equals(SdkRepository.NS_SDK_REPOSITORY)) {
                        String name = child.getLocalName();
                        if (SdkRepository.NODE_ADD_ON.equals(name)) {
                            parseAddon(child, mPackages, monitor);

                        } else if (!mAddonOnly) {
                            if (SdkRepository.NODE_PLATFORM.equals(name)) {
                                parsePlatform(child, mPackages, monitor);

                            } else if (SdkRepository.NODE_DOC.equals(name)) {
                                parseDoc(child, mPackages, monitor);

                            } else if (SdkRepository.NODE_TOOL.equals(name)) {
                                parseTool(child, mPackages, monitor);

                            }
                        }
                    }
                }

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

    private Node getFirstChild(Node node, String xmlLocalName) {

        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNamespaceURI().equals(SdkRepository.NS_SDK_REPOSITORY)) {
                if (xmlLocalName == null || child.getLocalName().equals(xmlLocalName)) {
                    return child;
                }
            }
        }

        return null;
    }

    private Document getDocument(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        return doc;
    }

    private void parseAddon(Node addon, ArrayList<String> packages, ITaskMonitor monitor) {
        // TODO Auto-generated method stub
        String s = String.format("addon %1$s by %2$s, api %3$s, rev %4$s",
                getFirstChild(addon, SdkRepository.NODE_NAME).getTextContent(),
                getFirstChild(addon, SdkRepository.NODE_VENDOR).getTextContent(),
                getFirstChild(addon, SdkRepository.NODE_API_LEVEL).getTextContent(),
                getFirstChild(addon, SdkRepository.NODE_REVISION).getTextContent()
                );
        packages.add(s);
    }

    private void parsePlatform(Node platform, ArrayList<String> packages, ITaskMonitor monitor) {
        // TODO Auto-generated method stub
        String s = String.format("platform %1$s, api %2$s, rev %3$s",
                getFirstChild(platform, SdkRepository.NODE_VERSION).getTextContent(),
                getFirstChild(platform, SdkRepository.NODE_API_LEVEL).getTextContent(),
                getFirstChild(platform, SdkRepository.NODE_REVISION).getTextContent()
                );
        packages.add(s);
    }

    private void parseDoc(Node doc, ArrayList<String> packages, ITaskMonitor monitor) {
        // TODO Auto-generated method stub
        String s = String.format("doc for api %1$s, rev %2$s",
                getFirstChild(doc, SdkRepository.NODE_API_LEVEL).getTextContent(),
                getFirstChild(doc, SdkRepository.NODE_REVISION).getTextContent()
                );
        packages.add(s);
    }

    private void parseTool(Node tool, ArrayList<String> packages, ITaskMonitor monitor) {
        // TODO Auto-generated method stub
        String s = String.format("tool, rev %1$s",
                getFirstChild(tool, SdkRepository.NODE_REVISION).getTextContent()
                );
        packages.add(s);
    }
}
