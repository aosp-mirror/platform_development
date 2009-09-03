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

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * Represents an add-on XML node in an SDK repository.
 */
public class AddonPackage extends Package {

    private static final String PROP_NAME      = "Addon.Name";      //$NON-NLS-1$
    private static final String PROP_VENDOR    = "Addon.Vendor";    //$NON-NLS-1$

    private final String mVendor;
    private final String mName;
    private final AndroidVersion mVersion;

    /** An add-on library. */
    public static class Lib {
        private final String mName;
        private final String mDescription;

        public Lib(String name, String description) {
            mName = name;
            mDescription = description;
        }

        public String getName() {
            return mName;
        }

        public String getDescription() {
            return mDescription;
        }
    }

    private final Lib[] mLibs;

    /**
     * Creates a new add-on package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    AddonPackage(RepoSource source, Node packageNode, Map<String,String> licenses) {
        super(source, packageNode, licenses);
        mVendor   = XmlParserUtils.getXmlString(packageNode, SdkRepository.NODE_VENDOR);
        mName     = XmlParserUtils.getXmlString(packageNode, SdkRepository.NODE_NAME);
        int apiLevel = XmlParserUtils.getXmlInt   (packageNode, SdkRepository.NODE_API_LEVEL, 0);
        String codeName = XmlParserUtils.getXmlString(packageNode, SdkRepository.NODE_CODENAME);
        if (codeName.length() == 0) {
            codeName = null;
        }
        mVersion = new AndroidVersion(apiLevel, codeName);

        mLibs = parseLibs(XmlParserUtils.getFirstChild(packageNode, SdkRepository.NODE_LIBS));
    }

    /**
     * Creates a new platform package based on an actual {@link IAndroidTarget} (which
     * {@link IAndroidTarget#isPlatform()} false) from the {@link SdkManager}.
     * This is used to list local SDK folders in which case there is one archive which
     * URL is the actual target location.
     */
    AddonPackage(IAndroidTarget target, Properties props) {
        super(  null,                       //source
                props,                      //properties
                target.getRevision(),       //revision
                null,                       //license
                target.getDescription(),    //description
                null,                       //descUrl
                Os.getCurrentOs(),          //archiveOs
                Arch.getCurrentArch(),      //archiveArch
                target.getLocation()        //archiveOsPath
                );

        mVersion = target.getVersion();
        mName     = target.getName();
        mVendor   = target.getVendor();

        IOptionalLibrary[] optLibs = target.getOptionalLibraries();
        if (optLibs == null || optLibs.length == 0) {
            mLibs = new Lib[0];
        } else {
            mLibs = new Lib[optLibs.length];
            for (int i = 0; i < optLibs.length; i++) {
                mLibs[i] = new Lib(optLibs[i].getName(), optLibs[i].getDescription());
            }
        }
    }

    /**
     * Save the properties of the current packages in the given {@link Properties} object.
     * These properties will later be given to a constructor that takes a {@link Properties} object.
     */
    @Override
    void saveProperties(Properties props) {
        super.saveProperties(props);

        mVersion.saveProperties(props);
        if (mName != null) {
            props.setProperty(PROP_NAME, mName);
        }
        if (mVendor != null) {
            props.setProperty(PROP_VENDOR, mVendor);
        }
    }

    /**
     * Parses a <libs> element.
     */
    private Lib[] parseLibs(Node libsNode) {
        ArrayList<Lib> libs = new ArrayList<Lib>();

        if (libsNode != null) {
            for(Node child = libsNode.getFirstChild();
                child != null;
                child = child.getNextSibling()) {

                if (child.getNodeType() == Node.ELEMENT_NODE &&
                        SdkRepository.NS_SDK_REPOSITORY.equals(child.getNamespaceURI()) &&
                        SdkRepository.NODE_LIB.equals(child.getLocalName())) {
                    libs.add(parseLib(child));
                }
            }
        }

        return libs.toArray(new Lib[libs.size()]);
    }

    /**
     * Parses a <lib> element from a <libs> container.
     */
    private Lib parseLib(Node libNode) {
        return new Lib(XmlParserUtils.getXmlString(libNode, SdkRepository.NODE_NAME),
                       XmlParserUtils.getXmlString(libNode, SdkRepository.NODE_DESCRIPTION));
    }

    /** Returns the vendor, a string, for add-on packages. */
    public String getVendor() {
        return mVendor;
    }

    /** Returns the name, a string, for add-on packages or for libraries. */
    public String getName() {
        return mName;
    }

    /** Returns the version, for platform, add-on and doc packages. */
    public AndroidVersion getVersion() {
        return mVersion;
    }

    /** Returns the libs defined in this add-on. Can be an empty array but not null. */
    public Lib[] getLibs() {
        return mLibs;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        return String.format("%1$s by %2$s for Android API %3$s",
                getName(),
                getVendor(),
                mVersion.getApiString());
    }

    /** Returns a long description for an {@link IDescription}. */
    @Override
    public String getLongDescription() {
        return String.format("%1$s,\nRevision %2$d.",
                getShortDescription(),
                getRevision());
    }

    /**
     * Computes a potential installation folder if an archive of this package were
     * to be installed right away in the given SDK root.
     * <p/>
     * An add-on package is typically installed in SDK/add-ons/"addon-name"-"api-level".
     * The name needs to be sanitized to be acceptable as a directory name.
     * However if we can find a different directory under SDK/add-ons that already
     * has this add-ons installed, we'll use that one.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param suggestedDir A suggestion for the installation folder name, based on the root
     *                     folder used in the zip archive.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    @Override
    public File getInstallFolder(String osSdkRoot, String suggestedDir, SdkManager sdkManager) {
        File addons = new File(osSdkRoot, SdkConstants.FD_ADDONS);

        // First find if this add-on is already installed. If so, reuse the same directory.
        for (IAndroidTarget target : sdkManager.getTargets()) {
            if (!target.isPlatform() &&
                    target.getVersion().equals(mVersion) &&
                    target.getName().equals(getName()) &&
                    target.getVendor().equals(getVendor())) {
                return new File(target.getLocation());
            }
        }

        // Otherwise, see about reusing the suggestedDir. It must not be already used or
        // add some index to it, or we try to make up one.
        String name = suggestedDir;

        if (suggestedDir == null || suggestedDir.length() == 0) {
            name = String.format("addon-%s-%s-%s", getName(), getVendor(), //$NON-NLS-1$
                    mVersion.getApiString());
            name = name.toLowerCase();
            name = name.replaceAll("[^a-z0-9_-]+", "_");      //$NON-NLS-1$ //$NON-NLS-2$
            name = name.replaceAll("_+", "_");                //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (int i = 0; i < 100; i++) {
            String name2 = i == 0 ? name : String.format("%s-%d", name, i); //$NON-NLS-1$
            File folder = new File(addons, name2);
            if (!folder.exists()) {
                return folder;
            }
        }

        // We shouldn't really get here. I mean, seriously, we tried hard enough.
        return null;
    }

    @Override
    public boolean sameItemAs(Package pkg) {
        if (pkg instanceof AddonPackage) {
            AddonPackage newPkg = (AddonPackage)pkg;

            // check they are the same add-on.
            return getName().equals(newPkg.getName()) &&
                    getVendor().equals(newPkg.getVendor()) &&
                    getVersion().equals(newPkg.getVersion());
        }

        return false;
    }
}
