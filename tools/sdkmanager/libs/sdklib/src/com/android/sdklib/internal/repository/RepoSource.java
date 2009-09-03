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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLKeyException;
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

    private String mUrl;
    private final boolean mUserSource;

    private Package[] mPackages;
    private String mDescription;
    private String mFetchError;

    /**
     * Constructs a new source for the given repository URL.
     * @param url The source URL. Cannot be null. If the URL ends with a /, the default
     *            repository.xml filename will be appended automatically.
     * @param userSource True if this a user source (add-ons & packages only.)
     */
    public RepoSource(String url, boolean userSource) {

        // if the URL ends with a /, it must be "directory" resource,
        // in which case we automatically add the default file that will
        // looked for. This way it will be obvious to the user which
        // resource we are actually trying to fetch.
        if (url.endsWith("/")) {  //$NON-NLS-1$
            url += SdkRepository.URL_DEFAULT_XML_FILE;
        }

        mUrl = url;
        mUserSource = userSource;
        setDefaultDescription();
    }

    /**
     * Two repo source are equal if they have the same userSource flag and the same URL.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RepoSource) {
            RepoSource rs = (RepoSource) obj;
            return  rs.isUserSource() == this.isUserSource() && rs.getUrl().equals(this.getUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mUrl.hashCode() ^ Boolean.valueOf(mUserSource).hashCode();
    }

    /** Returns true if this is a user source. We only load addon and extra packages
     * from a user source and ignore the rest. */
    public boolean isUserSource() {
        return mUserSource;
    }

    /** Returns the URL of the repository.xml file for this source. */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Returns the list of known packages found by the last call to load().
     * This is null when the source hasn't been loaded yet.
     */
    public Package[] getPackages() {
        return mPackages;
    }

    /**
     * Clear the internal packages list. After this call, {@link #getPackages()} will return
     * null till load() is called.
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
     * Returns the last fetch error description.
     * If there was no error, returns null.
     */
    public String getFetchError() {
        return mFetchError;
    }

    /**
     * Tries to fetch the repository index for the given URL.
     */
    public void load(ITaskMonitor monitor, boolean forceHttp) {

        monitor.setProgressMax(4);

        setDefaultDescription();

        String url = mUrl;
        if (forceHttp) {
            url = url.replaceAll("https://", "http://");  //$NON-NLS-1$ //$NON-NLS-2$
        }

        monitor.setDescription("Fetching %1$s", url);
        monitor.incProgress(1);

        mFetchError = null;
        Exception[] exception = new Exception[] { null };
        ByteArrayInputStream xml = fetchUrl(url, exception);
        boolean validated = false;
        if (xml != null) {
            monitor.setDescription("Validate XML");
            validated = validateXml(xml, url, monitor);
        }

        // If we failed the first time and the URL doesn't explicitly end with
        // our filename, make another tentative after changing the URL.
        if (!validated && !url.endsWith(SdkRepository.URL_DEFAULT_XML_FILE)) {
            if (!url.endsWith("/")) {       //$NON-NLS-1$
                url += "/";                 //$NON-NLS-1$
            }
            url += SdkRepository.URL_DEFAULT_XML_FILE;

            xml = fetchUrl(url, exception);
            if (xml != null) {
                validated = validateXml(xml, url, monitor);
            }

            if (validated) {
                // If the second tentative succeeded, indicate it in the console
                // with the URL that worked.
                monitor.setResult("Repository found at %1$s", url);

                // Keep the modified URL
                mUrl = url;
            }
        }

        if (!validated) {
            mFetchError = "Failed to fetch URL";

            String reason = "Unknown";
            if (exception[0] != null) {
                if (exception[0] instanceof FileNotFoundException) {
                    reason = "File not found";
                } else if (exception[0] instanceof SSLKeyException) {
                    reason = "SSL error. You might want to force download through http in the settings.";
                } else if (exception[0].getMessage() != null) {
                    reason = exception[0].getMessage();
                }
            }

            monitor.setResult("Failed to fetch URL %1$s, reason: %2$s", url, reason);
        }

        monitor.incProgress(1);

        if (xml != null) {
            monitor.setDescription("Parse XML");
            monitor.incProgress(1);
            parsePackages(xml, monitor);
            if (mPackages == null || mPackages.length == 0) {
                mDescription += "\nNo packages found.";
            } else if (mPackages.length == 1) {
                mDescription += "\nOne package found.";
            } else {
                mDescription += String.format("\n%1$d packages found.", mPackages.length);
            }
        }

        // done
        monitor.incProgress(1);
    }

    private void setDefaultDescription() {
        if (mUserSource) {
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
    private ByteArrayInputStream fetchUrl(String urlString, Exception[] outException) {
        URL url;
        try {
            url = new URL(urlString);

            InputStream is = null;

            int inc = 65536;
            int curr = 0;
            byte[] result = new byte[inc];

            try {
                is = url.openStream();

                int n;
                while ((n = is.read(result, curr, result.length - curr)) != -1) {
                    curr += n;
                    if (curr == result.length) {
                        byte[] temp = new byte[curr + inc];
                        System.arraycopy(result, 0, temp, 0, curr);
                        result = temp;
                    }
                }

                return new ByteArrayInputStream(result, 0, curr);

            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // pass
                    }
                }
            }

        } catch (IOException e) {
            outException[0] = e;
        }

        return null;
    }

    /**
     * Validates this XML against the SDK Repository schema.
     * Returns true if the XML was correctly validated.
     */
    private boolean validateXml(ByteArrayInputStream xml, String url, ITaskMonitor monitor) {

        try {
            Validator validator = getValidator();
            xml.reset();
            validator.validate(new StreamSource(xml));
            return true;

        } catch (Exception e) {
            monitor.setResult("XML verification failed for %1$s.\nError: %2$s",
                    url,
                    e.getMessage());
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
    private boolean parsePackages(ByteArrayInputStream xml, ITaskMonitor monitor) {

        try {
            Document doc = getDocument(xml);

            Node root = getFirstChild(doc, SdkRepository.NODE_SDK_REPOSITORY);
            if (root != null) {

                ArrayList<Package> packages = new ArrayList<Package>();

                // Parse license definitions
                HashMap<String, String> licenses = new HashMap<String, String>();
                for (Node child = root.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE &&
                            SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI()) &&
                            child.getLocalName().equals(SdkRepository.NODE_LICENSE)) {
                        Node id = child.getAttributes().getNamedItem(SdkRepository.ATTR_ID);
                        if (id != null) {
                            licenses.put(id.getNodeValue(), child.getTextContent());
                        }
                    }
                }

                // Parse packages
                for (Node child = root.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE &&
                            SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI())) {
                        String name = child.getLocalName();
                        Package p = null;

                        try {
                            // We can load addon and extra packages from all sources, either
                            // internal or user sources.
                            if (SdkRepository.NODE_ADD_ON.equals(name)) {
                                p = new AddonPackage(this, child, licenses);

                            } else if (SdkRepository.NODE_EXTRA.equals(name)) {
                                p = new ExtraPackage(this, child, licenses);

                            } else if (!mUserSource) {
                                // We only load platform, doc and tool packages from internal
                                // sources, never from user sources.
                                if (SdkRepository.NODE_PLATFORM.equals(name)) {
                                    p = new PlatformPackage(this, child, licenses);
                                } else if (SdkRepository.NODE_DOC.equals(name)) {
                                    p = new DocPackage(this, child, licenses);
                                } else if (SdkRepository.NODE_TOOL.equals(name)) {
                                    p = new ToolPackage(this, child, licenses);
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
            monitor.setResult("Failed to create XML document builder");

        } catch (SAXException e) {
            monitor.setResult("Failed to parse XML document");

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
    private Document getDocument(ByteArrayInputStream xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        xml.reset();
        Document doc = builder.parse(new InputSource(xml));

        return doc;
    }
}
