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

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;

/**
 * A {@link Package} is the base class for "something" that can be downloaded from
 * the SDK repository -- subclasses include {@link PlatformPackage}, {@link AddonPackage},
 * {@link DocPackage} and {@link ToolPackage}.
 * <p/>
 * A package has some attributes (revision, description) and a list of archives
 * which represent the downloadable bits.
 * <p/>
 * Packages are contained by a {@link RepoSource} (a download site).
 * <p/>
 * Derived classes must implement the {@link IDescription} methods.
 */
public abstract class Package implements IDescription {

    private final int mRevision;
    private final String mLicense;
    private final String mDescription;
    private final String mDescUrl;
    private final Archive[] mArchives;
    private final RepoSource mSource;

    /**
     * Creates a new package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    Package(RepoSource source, Node packageNode) {
        mSource = source;
        mRevision    = getXmlInt   (packageNode, SdkRepository.NODE_REVISION, 0);
        mDescription = getXmlString(packageNode, SdkRepository.NODE_DESCRIPTION);
        mDescUrl     = getXmlString(packageNode, SdkRepository.NODE_DESC_URL);
        mLicense     = getXmlString(packageNode, SdkRepository.NODE_LICENSE);
        mArchives = parseArchives(getFirstChild(packageNode, SdkRepository.NODE_ARCHIVES));
    }

    /**
     * Manually create a new package with one archive and the given attributes.
     * This is used to create packages from local directories in which case there must be
     * one archive which URL is the actual target location.
     */
    public Package(RepoSource source,
            int revision,
            String license,
            String description,
            String descUrl,
            Os archiveOs,
            Arch archiveArch,
            String archiveOsPath) {
        mSource = source;
        mRevision = revision;
        mLicense = license;
        mDescription = description;
        mDescUrl = descUrl;
        mArchives = new Archive[1];
        mArchives[0] = new Archive(this,
                archiveOs,
                archiveArch,
                archiveOsPath);
    }

    /**
     * Parses an XML node to process the <archives> element.
     */
    private Archive[] parseArchives(Node archivesNode) {
        ArrayList<Archive> archives = new ArrayList<Archive>();

        if (archivesNode != null) {
            for(Node child = archivesNode.getFirstChild();
                child != null;
                child = child.getNextSibling()) {

                if (child.getNodeType() == Node.ELEMENT_NODE &&
                        SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI()) &&
                        SdkRepository.NODE_ARCHIVE.equals(child.getLocalName())) {
                    archives.add(parseArchive(child));
                }
            }
        }

        return archives.toArray(new Archive[archives.size()]);
    }

    /**
     * Parses one <archive> element from an <archives> container.
     */
    private Archive parseArchive(Node archiveNode) {
        Archive a = new Archive(
                    this,
                    (Os)   getEnumAttribute(archiveNode, SdkRepository.ATTR_OS,
                            Os.values(), null),
                    (Arch) getEnumAttribute(archiveNode, SdkRepository.ATTR_ARCH,
                            Arch.values(), Arch.ANY),
                    getXmlString(archiveNode, SdkRepository.NODE_URL),
                    getXmlLong(archiveNode, SdkRepository.NODE_SIZE, 0),
                    getXmlString(archiveNode, SdkRepository.NODE_CHECKSUM)
                );

        return a;
    }

    /**
     * Returns the source that created (and owns) this package. Can be null.
     */
    public RepoSource getParentSource() {
        return mSource;
    }

    /**
     * Returns the revision, an int > 0, for all packages (platform, add-on, tool, doc).
     * Can be 0 if this is a local package of unknown revision.
     */
    public int getRevision() {
        return mRevision;
    }

    /**
     * Returns the optional description for all packages (platform, add-on, tool, doc) or
     * for a lib. It is null if the element has not been specified in the repository XML.
     */
    public String getLicense() {
        return mLicense;
    }

    /**
     * Returns the optional description for all packages (platform, add-on, tool, doc) or
     * for a lib. Can be empty but not null.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the optional description URL for all packages (platform, add-on, tool, doc).
     * Can be empty but not null.
     */
    public String getDescUrl() {
        return mDescUrl;
    }

    /**
     * Returns the archives defined in this package.
     * Can be an empty array but not null.
     */
    public Archive[] getArchives() {
        return mArchives;
    }

    /**
     * Returns a short description for an {@link IDescription}.
     * Can be empty but not null.
     */
    public abstract String getShortDescription();

    /**
     * Returns a long description for an {@link IDescription}.
     * Can be empty but not null.
     */
    public String getLongDescription() {
        return String.format("%1$s\nRevision %2$d", getDescription(), getRevision());
    }

    /**
     * Computes a potential installation folder if an archive of this package were
     * to be installed right away in the given SDK root.
     * <p/>
     * Some types of packages install in a fix location, for example docs and tools.
     * In this case the returned folder may already exist with a different archive installed
     * at the desired location.
     * For other packages types, such as add-on or platform, the folder name is only partially
     * relevant to determine the content and thus a real check will be done to provide an
     * existing or new folder depending on the current content of the SDK.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    public abstract File getInstallFolder(String osSdkRoot);

    //---

    /**
     * Returns the first child element with the given XML local name.
     * If xmlLocalName is null, returns the very first child element.
     */
    protected static Node getFirstChild(Node node, String xmlLocalName) {

        for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI())) {
                if (xmlLocalName == null || xmlLocalName.equals(child.getLocalName())) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves the value of that XML element as a string.
     * Returns an empty string when the element is missing.
     */
    protected static String getXmlString(Node node, String xmlLocalName) {
        Node child = getFirstChild(node, xmlLocalName);

        return child == null ? "" : child.getTextContent();  //$NON-NLS-1$
    }

    /**
     * Retrieves the value of that XML element as an integer.
     * Returns the default value when the element is missing or is not an integer.
     */
    protected static int getXmlInt(Node node, String xmlLocalName, int defaultValue) {
        String s = getXmlString(node, xmlLocalName);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Retrieves the value of that XML element as a long.
     * Returns the default value when the element is missing or is not an integer.
     */
    protected static long getXmlLong(Node node, String xmlLocalName, long defaultValue) {
        String s = getXmlString(node, xmlLocalName);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Retrieve an attribute which value must match one of the given enums using a
     * case-insensitive name match.
     *
     * Returns defaultValue if the attribute does not exist or its value does not match
     * the given enum values.
     */
    private Object getEnumAttribute(
            Node archiveNode,
            String attrName,
            Object[] values,
            Object defaultValue) {

        Node attr = archiveNode.getAttributes().getNamedItem(attrName);
        if (attr != null) {
            String found = attr.getNodeValue();
            for (Object value : values) {
                if (value.toString().equalsIgnoreCase(found)) {
                    return value;
                }
            }
        }

        return defaultValue;
    }
}
