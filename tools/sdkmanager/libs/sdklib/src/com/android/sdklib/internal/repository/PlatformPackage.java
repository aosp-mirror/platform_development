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

import org.w3c.dom.Node;

import java.io.File;

/**
 * Represents a platform XML node in an SDK repository.
 */
public class PlatformPackage extends Package {

    private final String mVersion;
    private final int mApiLevel;

    /**
     * Creates a new platform package from the attributes and elements of the given XML node.
     * <p/>
     * This constructor should throw an exception if the package cannot be created.
     */
    PlatformPackage(RepoSource source, Node packageNode) {
        super(source, packageNode);
        mVersion  = getXmlString(packageNode, SdkRepository.NODE_VERSION);
        mApiLevel = getXmlInt   (packageNode, SdkRepository.NODE_API_LEVEL, 0);
    }

    /**
     * Creates a new platform package based on an actual {@link IAndroidTarget} (which
     * must have {@link IAndroidTarget#isPlatform()} true) from the {@link SdkManager}.
     * This is used to list local SDK folders in which case there is one archive which
     * URL is the actual target location.
     */
    PlatformPackage(IAndroidTarget target) {
        super(  null,                       //source
                0,                          //revision
                null,                       //license
                target.getDescription(),    //description
                null,                       //descUrl
                Os.getCurrentOs(),          //archiveOs
                Arch.getCurrentArch(),      //archiveArch
                target.getLocation()        //archiveOsPath
                );

        mApiLevel = target.getApiVersionNumber();
        mVersion  = target.getApiVersionName();
    }

    /** Returns the version, a string, for platform packages. */
    public String getVersion() {
        return mVersion;
    }

    /** Returns the api-level, an int > 0, for platform, add-on and doc packages. */
    public int getApiLevel() {
        return mApiLevel;
    }

    /** Returns a short description for an {@link IDescription}. */
    @Override
    public String getShortDescription() {
        return String.format("SDK Platform Android %1$s, API %2$d",
                getVersion(),
                getApiLevel());
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
     * A platform package is typically installed in SDK/platforms/android-"version".
     * However if we can find a different directory under SDK/platform that already
     * has this platform version installed, we'll use that one.
     *
     * @param osSdkRoot The OS path of the SDK root folder.
     * @param suggestedDir A suggestion for the installation folder name, based on the root
     *                     folder used in the zip archive.
     * @param sdkManager An existing SDK manager to list current platforms and addons.
     * @return A new {@link File} corresponding to the directory to use to install this package.
     */
    @Override
    public File getInstallFolder(String osSdkRoot, String suggestedDir, SdkManager sdkManager) {

        // First find if this add-on is already installed. If so, reuse the same directory.
        for (IAndroidTarget target : sdkManager.getTargets()) {
            if (target.isPlatform() &&
                    target.getApiVersionNumber() == getApiLevel() &&
                    target.getApiVersionName().equals(getVersion())) {
                return new File(target.getLocation());
            }
        }

        File platforms = new File(osSdkRoot, SdkConstants.FD_PLATFORMS);
        File folder = new File(platforms, String.format("android-%s", getVersion())); //$NON-NLS-1$
        // TODO find similar existing platform in platforms folder
        return folder;
    }

    /**
     * Computes whether the given platform package is a suitable update for the current package.
     * The base method checks the class type.
     * The platform package also tests that the version and API level are the same and
     * the revision number is greater
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

        PlatformPackage newPkg = (PlatformPackage) replacementPackage;
        return newPkg.getVersion().equalsIgnoreCase(this.getVersion()) &&
            newPkg.getApiLevel() == this.getApiLevel() &&
            newPkg.getRevision() > this.getRevision();
    }
}
