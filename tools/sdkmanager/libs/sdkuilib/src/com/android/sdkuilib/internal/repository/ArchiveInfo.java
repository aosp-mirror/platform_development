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

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.internal.repository.Archive;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents an archive that we want to install.
 * Note that the installer deals with archives whereas the user mostly sees packages
 * but as far as we are concerned for installation there's a 1-to-1 mapping.
 * <p/>
 * A new archive is always a remote archive that needs to be downloaded and then
 * installed. It can replace an existing local one. It can also depends on another
 * (new or local) archive, which means the dependent archive needs to be successfully
 * installed first. Finally this archive can also be a dependency for another one.
 *
 * @see ArchiveInfo#ArchiveInfo(Archive, Archive, ArchiveInfo)
 */
class ArchiveInfo {

    private final Archive mNewArchive;
    private final Archive mReplaced;
    private final ArchiveInfo mDependsOn;
    private final ArrayList<ArchiveInfo> mDependencyFor = new ArrayList<ArchiveInfo>();
    private boolean mAccepted;
    private boolean mRejected;

    /**
     *
     * @param newArchive A "new archive" to be installed. This is always an archive
     *          that comes from a remote site. This can not be null.
     * @param replaced An optional local archive that the new one will replace.
     *          Can be null if this archive does not replace anything.
     * @param dependsOn An optional new or local dependency, that is an archive that
     *          <em>this</em> archive depends upon. In other words, we can only install
     *          this archive if the dependency has been successfully installed. It also
     *          means we need to install the dependency first.
     */
    public ArchiveInfo(Archive newArchive, Archive replaced, ArchiveInfo dependsOn) {
        mNewArchive = newArchive;
        mReplaced = replaced;
        mDependsOn = dependsOn;
    }

    /**
     * Returns the "new archive" to be installed.
     * This is always an archive that comes from a remote site.
     */
    public Archive getNewArchive() {
        return mNewArchive;
    }

    /**
     * Returns an optional local archive that the new one will replace.
     * Can be null if this archive does not replace anything.
     */
    public Archive getReplaced() {
        return mReplaced;
    }

    /**
     * Returns an optional new or local dependency, that is an archive that <em>this</em>
     * archive depends upon. In other words, we can only install this archive if the
     * dependency has been successfully installed. It also means we need to install the
     * dependency first.
     */
    public ArchiveInfo getDependsOn() {
        return mDependsOn;
    }

    /**
     * Returns true if this new archive is a dependency for <em>another</em> one that we
     * want to install.
     */
    public boolean isDependencyFor() {
        return mDependencyFor.size() > 0;
    }

    /**
     * Adds an {@link ArchiveInfo} for which <em>this</em> package is a dependency.
     * This means the package added here depends on this package.
     */
    public void addDependencyFor(ArchiveInfo dependencyFor) {
        if (!mDependencyFor.contains(dependencyFor)) {
            mDependencyFor.add(dependencyFor);
        }
    }

    public Collection<ArchiveInfo> getDependenciesFor() {
        return mDependencyFor;
    }

    /**
     * Sets whether this archive was accepted (either manually by the user or
     * automatically if it doesn't have a license) for installation.
     */
    public void setAccepted(boolean accepted) {
        mAccepted = accepted;
    }

    /**
     * Returns whether this archive was accepted (either manually by the user or
     * automatically if it doesn't have a license) for installation.
     */
    public boolean isAccepted() {
        return mAccepted;
    }

    /**
     * Sets whether this archive was rejected manually by the user.
     * An archive can neither accepted nor rejected.
     */
    public void setRejected(boolean rejected) {
        mRejected = rejected;
    }

    /**
     * Returns whether this archive was rejected manually by the user.
     * An archive can neither accepted nor rejected.
     */
    public boolean isRejected() {
        return mRejected;
    }
}
