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
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Node;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a doc XML node in an SDK repository.
 */
public class DocPackage extends Package {

    private final AndroidVersion mVersion;

    /**
     * Creates a new doc package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    DocPackage(RepoSource source, Node packageNode, Map<String,String> licenses) {
        super(source, packageNode, licenses);

        int apiLevel = XmlParserUtils.getXmlInt   (packageNode, SdkRepository.NODE_API_LEVEL, 0);
        String codeName = XmlParserUtils.getXmlString(packageNode, SdkRepository.NODE_CODENAME);
        if (codeName.length() == 0) {
            codeName = null;
        }
        mVersion = new AndroidVersion(apiLevel, codeName);
    }

    /**
     * Manually create a new package with one archive and the given attributes.
     * This is used to create packages from local directories in which case there must be
     * one archive which URL is the actual target location.
     */
    DocPackage(RepoSource source,
            Properties props,
            int apiLevel,
            String codename,
            int revision,
            String license,
            String description,
            String descUrl,
            Os archiveOs,
            Arch archiveArch,
            String archiveOsPath) {
        super(source,
                props,
                revision,
                license,
                description,
                descUrl,
                archiveOs,
                archiveArch,
                archiveOsPath);
        mVersion = new AndroidVersion(props, apiLevel, codename);
    }

    /**
     * Save the properties of the current packages in the given {@link Properties} object.
     * These properties will later be give the constructor that takes a {@link Properties} object.
     */
    @Override
    void saveProperties(Properties props) {
        super.saveProperties(props);

        mVersion.saveProperties(props);
    }

    /** Returns the version, for platform, add-on and doc packages.
     *  Can be 0 if this is a local package of unknown api-level. */
    public AndroidVersion getVersion() {
        return mVersion;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        if (mVersion.isPreview()) {
            return String.format("Documentation for Android '%1$s' Preview SDK",
                    mVersion.getCodename());
        } else if (mVersion.getApiLevel() != 0) {
            return String.format("Documentation for Android SDK, API %1$d", mVersion.getApiLevel());
        } else {
            return String.format("Documentation for Android SDK");
        }
    }

    /** Returns a long description for an {@link IDescription}. */
    @Override
    public String getLongDescription() {
        return String.format("%1$s.\n%2$s",
                getShortDescription(),
                super.getLongDescription());
    }

    /**
     * Computes a potential installation folder if an archive of this package were
     * to be installed right away in the given SDK root.
     * <p/>
     * A "doc" package should always be located in SDK/docs.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param suggestedDir A suggestion for the installation folder name, based on the root
     *                     folder used in the zip archive.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    @Override
    public File getInstallFolder(String osSdkRoot, String suggestedDir, SdkManager sdkManager) {
        return new File(osSdkRoot, SdkConstants.FD_DOCS);
    }

    @Override
    public boolean sameItemAs(Package pkg) {
        // only one doc package so any doc package is the same item.
        return pkg instanceof DocPackage;
    }

    /**
     * {@inheritDoc}
     *
     * The comparison between doc packages is a bit more complex so we override the default
     * implementation.
     * <p/>
     * Docs are upgrade if they have a higher api, or a similar api but a higher revision.
     * <p/>
     * What makes this more complex is handling codename.
     */
    @Override
    public UpdateInfo canBeUpdatedBy(Package replacementPackage) {
        if (replacementPackage == null) {
            return UpdateInfo.INCOMPATIBLE;
        }

        // check they are the same item.
        if (sameItemAs(replacementPackage) == false) {
            return UpdateInfo.INCOMPATIBLE;
        }

        DocPackage replacementDoc = (DocPackage)replacementPackage;

        AndroidVersion replacementVersion = replacementDoc.getVersion();

        // the new doc is an update if the api level is higher
        if (replacementVersion.getApiLevel() > mVersion.getApiLevel()) {
            return UpdateInfo.UPDATE;
        }

        // if it's the exactly same (including codename), we check the revision
        if (replacementVersion.equals(mVersion) &&
                replacementPackage.getRevision() > this.getRevision()) {
            return UpdateInfo.UPDATE;
        }

        // else we check if they have the same api level and the new one is a preview, in which
        // case it's also an update (since preview have the api level of the _previous_ version.
        if (replacementVersion.getApiLevel() == mVersion.getApiLevel() &&
                replacementVersion.isPreview()) {
            return UpdateInfo.UPDATE;
        }

        // not an upgrade but not incompatible either.
        return UpdateInfo.NOT_UPDATE;
    }
}
