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

import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
import java.util.regex.Pattern;

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
        Document validatedDoc = null;
        String validatedUri = null;
        if (xml != null) {
            monitor.setDescription("Validate XML");
            String uri = validateXml(xml, url, monitor);
            if (uri != null) {
                validatedDoc = getDocument(xml, monitor);
                validatedUri = uri;
            } else {
                validatedDoc = findAlternateToolsXml(xml);
                validatedUri = SdkRepository.NS_SDK_REPOSITORY;
            }
        }

        // If we failed the first time and the URL doesn't explicitly end with
        // our filename, make another tentative after changing the URL.
        if (validatedDoc == null && !url.endsWith(SdkRepository.URL_DEFAULT_XML_FILE)) {
            if (!url.endsWith("/")) {       //$NON-NLS-1$
                url += "/";                 //$NON-NLS-1$
            }
            url += SdkRepository.URL_DEFAULT_XML_FILE;

            xml = fetchUrl(url, exception);
            if (xml != null) {
                String uri = validateXml(xml, url, monitor);
                if (uri != null) {
                    validatedDoc = getDocument(xml, monitor);
                    validatedUri = uri;
                } else {
                    validatedDoc = findAlternateToolsXml(xml);
                    validatedUri = SdkRepository.NS_SDK_REPOSITORY;
                }
            }

            if (validatedDoc != null) {
                // If the second tentative succeeded, indicate it in the console
                // with the URL that worked.
                monitor.setResult("Repository found at %1$s", url);

                // Keep the modified URL
                mUrl = url;
            }
        }

        // If any exception was handled during the URL fetch, display it now.
        if (exception[0] != null) {
            mFetchError = "Failed to fetch URL";

            String reason = null;
            if (exception[0] instanceof FileNotFoundException) {
                // FNF has no useful getMessage, so we need to special handle it.
                reason = "File not found";
                mFetchError += ": " + reason;
            } else if (exception[0] instanceof SSLKeyException) {
                // That's a common error and we have a pref for it.
                reason = "HTTPS SSL error. You might want to force download through HTTP in the settings.";
                mFetchError += ": HTTPS SSL error";
            } else if (exception[0].getMessage() != null) {
                reason = exception[0].getMessage();
            } else {
                // We don't know what's wrong. Let's give the exception class at least.
                reason = String.format("Unknown (%1$s)", exception[0].getClass().getName());
            }

            monitor.setResult("Failed to fetch URL %1$s, reason: %2$s", url, reason);
        }

        // Stop here if we failed to validate the XML. We don't want to load it.
        if (validatedDoc == null) {
            return;
        }

        monitor.incProgress(1);

        if (xml != null) {
            monitor.setDescription("Parse XML");
            monitor.incProgress(1);
            parsePackages(validatedDoc, validatedUri, monitor);
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

        } catch (Exception e) {
            outException[0] = e;
        }

        return null;
    }

    /**
     * Validates this XML against one of the possible SDK Repository schema, starting
     * by the most recent one.
     * If the XML was correctly validated, returns the schema that worked.
     * If no schema validated the XML, returns null.
     */
    private String validateXml(ByteArrayInputStream xml, String url, ITaskMonitor monitor) {

        String lastError = null;
        String extraError = null;
        for (int version = SdkRepository.XSD_LATEST_VERSION; version >= 1; version--) {
            try {
                Validator validator = getValidator(version);

                if (validator == null) {
                    lastError = "XML verification failed for %1$s.\nNo suitable XML Schema Validator could be found in your Java environment. Please consider updating your version of Java.";
                    continue;
                }

                xml.reset();
                // Validation throws a bunch of possible Exceptions on failure.
                validator.validate(new StreamSource(xml));
                return SdkRepository.getSchemaUri(version);

            } catch (Exception e) {
                lastError = "XML verification failed for %1$s.\nError: %2$s";
                extraError = e.getMessage();
                if (extraError == null) {
                    extraError = e.getClass().getName();
                }
            }
        }

        if (lastError != null) {
            monitor.setResult(lastError, url, extraError);
        }
        return null;
    }

    /**
     * The purpose of this method is to support forward evolution of our schema.
     * <p/>
     * At this point, we know that xml does not point to any schema that this version of
     * the tool know how to process, so it's not one of the possible 1..N versions of our
     * XSD schema.
     * <p/>
     * We thus try to interpret the byte stream as a possible XML stream. It may not be
     * one at all in the first place. If it looks anything line an XML schema, we try to
     * find its &lt;tool&gt; elements. If we find any, we recreate a suitable document
     * that conforms to what we expect from our XSD schema with only those elements.
     * To be valid, the &lt;tool&gt; element must have at least one &lt;archive&gt;
     * compatible with this platform.
     *
     * If we don't find anything suitable, we drop the whole thing.
     *
     * @param xml The input XML stream. Can be null.
     * @return Either a new XML document conforming to our schema with at least one &lt;tool&gt;
     *         element or null.
     */
    protected Document findAlternateToolsXml(InputStream xml) {
        // Note: protected for unit-test access

        if (xml == null) {
            return null;
        }

        // Reset the stream if it supports that operation.
        // At runtime we use a ByteArrayInputStream which can be reset; however for unit tests
        // we use a FileInputStream that doesn't support resetting and is read-once.
        try {
            xml.reset();
        } catch (IOException e1) {
            // ignore if not supported
        }

        // Get an XML document

        Document oldDoc = null;
        Document newDoc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            factory.setValidating(false);

            // Parse the old document using a non namespace aware builder
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            oldDoc = builder.parse(xml);

            // Prepare a new document using a namespace aware builder
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            newDoc = builder.newDocument();

        } catch (Exception e) {
            // Failed to get builder factor
            // Failed to create XML document builder
            // Failed to parse XML document
            // Failed to read XML document
        }

        if (oldDoc == null || newDoc == null) {
            return null;
        }


        // Check the root element is an xsd-schema with at least the following properties:
        // <sdk:sdk-repository
        //    xmlns:sdk="http://schemas.android.com/sdk/android/repository/$N">
        //
        // Note that we don't have namespace support enabled, we just do it manually.

        Pattern nsPattern = Pattern.compile(SdkRepository.NS_SDK_REPOSITORY_PATTERN);

        Node oldRoot = null;
        String prefix = null;
        for (Node child = oldDoc.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                prefix = null;
                String name = child.getNodeName();
                int pos = name.indexOf(':');
                if (pos > 0 && pos < name.length() - 1) {
                    prefix = name.substring(0, pos);
                    name = name.substring(pos + 1);
                }
                if (SdkRepository.NODE_SDK_REPOSITORY.equals(name)) {
                    NamedNodeMap attrs = child.getAttributes();
                    String xmlns = "xmlns";                                         //$NON-NLS-1$
                    if (prefix != null) {
                        xmlns += ":" + prefix;                                      //$NON-NLS-1$
                    }
                    Node attr = attrs.getNamedItem(xmlns);
                    if (attr != null) {
                        String uri = attr.getNodeValue();
                        if (uri != null && nsPattern.matcher(uri).matches()) {
                            oldRoot = child;
                            break;
                        }
                    }
                }
            }
        }

        // we must have found the root node, and it must have an XML namespace prefix.
        if (oldRoot == null || prefix == null || prefix.length() == 0) {
            return null;
        }

        final String ns = SdkRepository.NS_SDK_REPOSITORY;
        Element newRoot = newDoc.createElementNS(ns, SdkRepository.NODE_SDK_REPOSITORY);
        newRoot.setPrefix(prefix);
        newDoc.appendChild(newRoot);
        int numTool = 0;

        // Find an inner <tool> node and extract its required parameters

        Node tool = null;
        while ((tool = findChild(oldRoot, tool, prefix, SdkRepository.NODE_TOOL)) != null) {
            // To be valid, the tool element must have:
            // - a <revision> element with a number
            // - an optional <uses-license> node, which we'll skip right now.
            //   (if we add it later, we must find the license declaration element too)
            // - an <archives> element with one or more <archive> elements inside
            // - one of the <archive> elements must have an "os" and "arch" attributes
            //   compatible with the current platform. Only keep the first such element found.
            // - the <archive> element must contain a <size>, a <checksum> and a <url>.

            try {
                Node revision = findChild(tool, null, prefix, SdkRepository.NODE_REVISION);
                Node archives = findChild(tool, null, prefix, SdkRepository.NODE_ARCHIVES);

                if (revision == null || archives == null) {
                    continue;
                }

                int rev = 0;
                try {
                    String content = revision.getTextContent();
                    content = content.trim();
                    rev = Integer.parseInt(content);
                    if (rev < 1) {
                        continue;
                    }
                } catch (NumberFormatException ignore) {
                    continue;
                }

                Element newTool = newDoc.createElementNS(ns, SdkRepository.NODE_TOOL);
                newTool.setPrefix(prefix);
                appendChild(newTool, ns, prefix,
                        SdkRepository.NODE_REVISION, Integer.toString(rev));
                Element newArchives = appendChild(newTool, ns, prefix,
                                                  SdkRepository.NODE_ARCHIVES, null);
                int numArchives = 0;

                Node archive = null;
                while ((archive = findChild(archives,
                                            archive,
                                            prefix,
                                            SdkRepository.NODE_ARCHIVE)) != null) {
                    try {
                        Os os = (Os) XmlParserUtils.getEnumAttribute(archive,
                                SdkRepository.ATTR_OS,
                                Os.values(),
                                null /*default*/);
                        Arch arch = (Arch) XmlParserUtils.getEnumAttribute(archive,
                                SdkRepository.ATTR_ARCH,
                                Arch.values(),
                                Arch.ANY);
                        if (os == null || !os.isCompatible() ||
                                arch == null || !arch.isCompatible()) {
                            continue;
                        }

                        Node node = findChild(archive, null, prefix, SdkRepository.NODE_URL);
                        String url = node == null ? null : node.getTextContent().trim();
                        if (url == null || url.length() == 0) {
                            continue;
                        }

                        node = findChild(archive, null, prefix, SdkRepository.NODE_SIZE);
                        long size = 0;
                        try {
                            size = Long.parseLong(node.getTextContent());
                        } catch (Exception e) {
                            // pass
                        }
                        if (size < 1) {
                            continue;
                        }

                        node = findChild(archive, null, prefix, SdkRepository.NODE_CHECKSUM);
                        // double check that the checksum element contains a type=sha1 attribute
                        if (node == null) {
                            continue;
                        }
                        NamedNodeMap attrs = node.getAttributes();
                        Node typeNode = attrs.getNamedItem(SdkRepository.ATTR_TYPE);
                        if (typeNode == null ||
                                !SdkRepository.ATTR_TYPE.equals(typeNode.getNodeName()) ||
                                !SdkRepository.SHA1_TYPE.equals(typeNode.getNodeValue())) {
                            continue;
                        }
                        String sha1 = node == null ? null : node.getTextContent().trim();
                        if (sha1 == null || sha1.length() != SdkRepository.SHA1_CHECKSUM_LEN) {
                            continue;
                        }

                        // Use that archive for the new tool element
                        Element ar = appendChild(newArchives, ns, prefix,
                                                 SdkRepository.NODE_ARCHIVE, null);
                        ar.setAttributeNS(ns, SdkRepository.ATTR_OS, os.getXmlName());
                        ar.setAttributeNS(ns, SdkRepository.ATTR_ARCH, arch.getXmlName());

                        appendChild(ar, ns, prefix, SdkRepository.NODE_URL, url);
                        appendChild(ar, ns, prefix, SdkRepository.NODE_SIZE, Long.toString(size));
                        Element cs = appendChild(ar, ns, prefix, SdkRepository.NODE_CHECKSUM, sha1);
                        cs.setAttributeNS(ns, SdkRepository.ATTR_TYPE, SdkRepository.SHA1_TYPE);

                        numArchives++;

                    } catch (Exception ignore1) {
                        // pass
                    }
                } // while <archive>

                if (numArchives > 0) {
                    newRoot.appendChild(newTool);
                    numTool++;
                }
            } catch (Exception ignore2) {
                // pass
            }
        } // while <tool>

        return numTool > 0 ? newDoc : null;
    }

    /**
     * Helper method used by {@link #findAlternateToolsXml(InputStream)} to find a given
     * element child in a root XML node.
     */
    private Node findChild(Node rootNode, Node after, String prefix, String nodeName) {
        nodeName = prefix + ":" + nodeName;
        Node child = after == null ? rootNode.getFirstChild() : after.getNextSibling();
        for(; child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && nodeName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Helper method used by {@link #findAlternateToolsXml(InputStream)} to create a new
     * XML element into a parent element.
     */
    private Element appendChild(Element rootNode, String namespaceUri,
            String prefix, String nodeName,
            String nodeValue) {
        Element node = rootNode.getOwnerDocument().createElementNS(namespaceUri, nodeName);
        node.setPrefix(prefix);
        if (nodeValue != null) {
            node.setTextContent(nodeValue);
        }
        rootNode.appendChild(node);
        return node;
    }


    /**
     * Helper method that returns a validator for our XSD, or null if the current Java
     * implementation can't process XSD schemas.
     *
     * @param version The version of the XML Schema.
     *        See {@link SdkRepository#getXsdStream(int)}
     */
    private Validator getValidator(int version) throws SAXException {
        InputStream xsdStream = SdkRepository.getXsdStream(version);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        if (factory == null) {
            return null;
        }

        // This may throw a SAX Exception if the schema itself is not a valid XSD
        Schema schema = factory.newSchema(new StreamSource(xsdStream));

        Validator validator = schema == null ? null : schema.newValidator();

        return validator;
    }


    /**
     * Parse all packages defined in the SDK Repository XML and creates
     * a new mPackages array with them.
     */
    protected boolean parsePackages(Document doc, String nsUri, ITaskMonitor monitor) {
        // protected for unit-test acces

        assert doc != null;

        Node root = getFirstChild(doc, nsUri, SdkRepository.NODE_SDK_REPOSITORY);
        if (root != null) {

            ArrayList<Package> packages = new ArrayList<Package>();

            // Parse license definitions
            HashMap<String, String> licenses = new HashMap<String, String>();
            for (Node child = root.getFirstChild();
                 child != null;
                 child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE &&
                        nsUri.equals(child.getNamespaceURI()) &&
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
                        nsUri.equals(child.getNamespaceURI())) {
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

        return false;
    }

    /**
     * Returns the first child element with the given XML local name.
     * If xmlLocalName is null, returns the very first child element.
     */
    private Node getFirstChild(Node node, String nsUri, String xmlLocalName) {

        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    nsUri.equals(child.getNamespaceURI())) {
                if (xmlLocalName == null || child.getLocalName().equals(xmlLocalName)) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Takes an XML document as a string as parameter and returns a DOM for it.
     *
     * On error, returns null and prints a (hopefully) useful message on the monitor.
     */
    private Document getDocument(ByteArrayInputStream xml, ITaskMonitor monitor) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            xml.reset();
            Document doc = builder.parse(new InputSource(xml));

            return doc;
        } catch (ParserConfigurationException e) {
            monitor.setResult("Failed to create XML document builder");

        } catch (SAXException e) {
            monitor.setResult("Failed to parse XML document");

        } catch (IOException e) {
            monitor.setResult("Failed to read XML document");
        }

        return null;
    }
}
