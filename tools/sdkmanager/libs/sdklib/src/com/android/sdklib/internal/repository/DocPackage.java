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

import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Node;

import java.io.File;

/**
 * Represents a doc XML node in an SDK repository.
 */
public class DocPackage extends Package {

    private final int mApiLevel;

    /**
     * Creates a new doc package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    DocPackage(RepoSource source, Node packageNode) {
        super(source, packageNode);
        mApiLevel = getXmlInt(packageNode, SdkRepository.NODE_API_LEVEL, 0);
    }

    /**
     * Manually create a new package with one archive and the given attributes.
     * This is used to create packages from local directories in which case there must be
     * one archive which URL is the actual target location.
     */
    DocPackage(RepoSource source,
            int apiLevel,
            int revision,
            String license,
            String description,
            String descUrl,
            Os archiveOs,
            Arch archiveArch,
            String archiveOsPath) {
        super(source,
                revision,
                license,
                description,
                descUrl,
                archiveOs,
                archiveArch,
                archiveOsPath);
        mApiLevel = apiLevel;
    }

    /** Returns the api-level, an int > 0, for platform, add-on and doc packages.
     *  Can be 0 if this is a local package of unknown api-level. */
    public int getApiLevel() {
        return mApiLevel;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        if (mApiLevel != 0) {
            return String.format("Documentation for Android SDK, API %1$d", mApiLevel);
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
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    @Override
    public File getInstallFolder(String osSdkRoot) {
        return new File(osSdkRoot, SdkConstants.FD_DOCS);
    }

    /**
     * Computes whether the given doc package is a suitable update for the current package.
     * The base method checks the class type.
     * The doc package also tests the API level and revision number: the revision number must
     * always be bumped. The API level can be the same or greater.
     * <p/>
     * An update is just that: a new package that supersedes the current one. If the new
     * package has the same revision as the current one, it's not an update.
     *
     * @param replacementPackage The potential replacement package.
     * @return True if the replacement package is a suitable update for this one.
     */
    @Override
    public boolean canBeUpdatedBy(Package replacementPackage) {
        if (!super.canBeUpdatedBy(replacementPackage)) {
            return false;
        }

        DocPackage newPkg = (DocPackage) replacementPackage;
        return newPkg.getRevision() > this.getRevision() &&
            newPkg.getApiLevel() >= this.getApiLevel();
    }
}
