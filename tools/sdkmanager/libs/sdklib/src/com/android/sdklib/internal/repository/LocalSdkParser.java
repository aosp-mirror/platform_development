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

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Scans a local SDK to find which packages are currently installed.
 */
public class LocalSdkParser {

    private static final String SOURCE_XML = "source.xml";  //$NON-NLS-1$ // TODO move to global constants
    private Package[] mPackages;

    public LocalSdkParser() {
        // pass
    }

    /**
     * Returns the packages found by the last call to {@link #parseSdk(String, SdkManager)}.
     */
    public Package[] getPackages() {
        return mPackages;
    }

    /**
     * Clear the internal packages list. After this call, {@link #getPackages()} will return
     * null till {@link #parseSdk(String, SdkManager)} is called.
     */
    public void clearPackages() {
        mPackages = null;
    }

    /**
     * Scan the give SDK to find all the packages already installed at this location.
     * <p/>
     * Store the packages internally. You can use {@link #getPackages()} to retrieve them
     * at any time later.
     *
     * @param osSdkRoot The path to the SDK folder.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return The packages found. Can be retrieved later using {@link #getPackages()}.
     */
    public Package[] parseSdk(String osSdkRoot, SdkManager sdkManager) {
        ArrayList<Package> packages = new ArrayList<Package>();

        Package pkg = scanDoc(new File(osSdkRoot, SdkConstants.FD_DOCS));
        if (pkg != null) {
            packages.add(pkg);
        }

        pkg = scanTools(new File(osSdkRoot, SdkConstants.FD_TOOLS));
        if (pkg != null) {
            packages.add(pkg);
        }

        // for platforms and add-ons, rely on the SdkManager parser
        for(IAndroidTarget target : sdkManager.getTargets()) {
            pkg = null;

            if (target.isPlatform()) {
                pkg = parseXml(new File(target.getLocation(), SOURCE_XML),
                               SdkRepository.NODE_PLATFORM);
                if (pkg == null) {
                    pkg = new PlatformPackage(target);
                }

            } else {
                pkg = parseXml(new File(target.getLocation(), SOURCE_XML),
                                        SdkRepository.NODE_ADD_ON);

                if (pkg == null) {
                    pkg = new AddonPackage(target);
                }
            }

            if (pkg != null) {
                packages.add(pkg);
            }
        }

        mPackages = packages.toArray(new Package[packages.size()]);
        return mPackages;
    }

    /**
     * Try to find a tools package at the given location.
     * Returns null if not found.
     */
    private Package scanTools(File toolFolder) {
        // Can we find a source.xml?
        Package pkg = parseXml(new File(toolFolder, SOURCE_XML), SdkRepository.NODE_TOOL);

        // We're not going to check that all tools are present. At the very least
        // we should expect to find adb, android and an emulator adapted to the current OS.
        Set<String> names = new HashSet<String>();
        for (File file : toolFolder.listFiles()) {
            names.add(file.getName());
        }
        if (!names.contains(SdkConstants.FN_ADB) ||
                !names.contains(SdkConstants.androidCmdName()) ||
                !names.contains(SdkConstants.FN_EMULATOR)) {
            return null;
        }

        // if we don't have the package info, make one up
        if (pkg == null) {
            pkg = new ToolPackage(
                    null,                       //source
                    0,                          //revision
                    null,                       //license
                    "Tools",                    //description
                    null,                       //descUrl
                    Os.getCurrentOs(),          //archiveOs
                    Arch.getCurrentArch(),      //archiveArch
                    toolFolder.getPath()        //archiveOsPath
                    );
        }

        return pkg;
    }

    /**
     * Try to find a docs package at the given location.
     * Returns null if not found.
     */
    private Package scanDoc(File docFolder) {
        // Can we find a source.xml?
        Package pkg = parseXml(new File(docFolder, SOURCE_XML), SdkRepository.NODE_DOC);

        // To start with, a doc folder should have an "index.html" to be acceptable.
        String html = readFile(new File(docFolder, "index.html"));
        if (html != null) {
            // Try to find something that looks like this line:
            //   <a href="./sdk/1.5_r1/index.html">
            // We should find one or more of these and we want the highest version
            // and release numbers. Note that unfortunately that doesn't give us
            // the api-level we care about for the doc package.

            String found = null;
            Pattern re = Pattern.compile(
                    "<a\\s+href=\"./sdk/(\\d\\.\\d_r\\d)/index.html\">",
                    Pattern.DOTALL);
            Matcher m = re.matcher(html);
            while(m.find()) {
                String v = m.group(1);
                if (found == null || v.compareTo(found) == 1) {
                    found = v;
                }
            }

            if (found == null) {
                // That doesn't look like a doc folder.
                return null;
            }

            // We found the line, so it seems like an SDK doc.
            // Create a pkg if we don't have one yet.

            if (pkg == null) {
                pkg = new DocPackage(
                        null,                       //source
                        0,                          //apiLevel
                        0,                          //revision
                        null,                       //license
                        String.format("Documentation for %1$s", found),     //description
                        null,                       //descUrl
                        Os.getCurrentOs(),          //archiveOs
                        Arch.getCurrentArch(),      //archiveArch
                        docFolder.getPath()         //archiveOsPath
                        );
            }
        }

        return pkg;
    }

    /**
     * Parses the given XML file for the specific element filter.
     * The element must one of the package type local names: doc, tool, platform or addon.
     * Returns null if no such package was found.
     */
    private Package parseXml(File sourceXmlFile, String elementFilter) {

        String xml = readFile(sourceXmlFile);
        if (xml != null) {
            if (validateXml(xml)) {
                return parsePackages(xml, elementFilter);
            }
        }

        return null;
    }

    /**
     * Parses the given XML to find the specific element filter.
     * The element must one of the package type local names: doc, tool, platform or addon.
     * Returns null if no such package was found.
     */
    private Package parsePackages(String xml, String elementFilter) {

        try {
            Document doc = getDocument(xml);

            Node root = getFirstChild(doc, SdkRepository.NODE_SDK_REPOSITORY);
            if (root != null) {

                for (Node child = root.getFirstChild();
                     child != null;
                     child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE &&
                            SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI()) &&
                            elementFilter.equals(child.getLocalName())) {
                        String name = child.getLocalName();

                        try {
                            if (SdkRepository.NODE_ADD_ON.equals(name)) {
                                return new AddonPackage(null /*source*/, child);

                            } else if (SdkRepository.NODE_PLATFORM.equals(name)) {
                                return new PlatformPackage(null /*source*/, child);

                            } else if (SdkRepository.NODE_DOC.equals(name)) {
                                return new DocPackage(null /*source*/, child);

                            } else if (SdkRepository.NODE_TOOL.equals(name)) {
                                return new ToolPackage(null /*source*/, child);
                            }
                        } catch (Exception e) {
                            // Ignore invalid packages
                        }
                    }
                }
            }

        } catch (Exception e) {
            // ignore
        }

        return null;
    }

    /**
     * Reads a file as a string.
     * Returns null if the file could not be read.
     */
    private String readFile(File sourceXmlFile) {
        FileReader fr = null;
        try {
            fr = new FileReader(sourceXmlFile);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder dest = new StringBuilder();
            char[] buf = new char[65536];
            int n;
            while ((n = br.read(buf)) > 0) {
                if (n > 0) {
                    dest.append(buf, 0, n);
                }
            }
            return dest.toString();

        } catch (IOException e) {
            // ignore

        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    /**
     * Validates this XML against the SDK Repository schema.
     * Returns true if the XML was correctly validated.
     */
    private boolean validateXml(String xml) {

        try {
            Validator validator = getValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return true;

        } catch (SAXException e) {
            // ignore

        } catch (IOException e) {
            // ignore
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
