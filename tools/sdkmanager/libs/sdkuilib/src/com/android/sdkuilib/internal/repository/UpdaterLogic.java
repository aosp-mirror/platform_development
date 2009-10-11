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

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.repository.AddonPackage;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.MinToolsPackage;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.PlatformPackage;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdklib.internal.repository.ToolPackage;
import com.android.sdklib.internal.repository.Package.UpdateInfo;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The logic to compute which packages to install, based on the choices
 * made by the user. This adds dependent packages as needed.
 * <p/>
 * When the user doesn't provide a selection, looks at local package to find
 * those that can be updated and compute dependencies too.
 */
class UpdaterLogic {

    private RepoSources mSources;

    /**
     * Compute which packages to install by taking the user selection
     * and adding dependent packages as needed.
     *
     * When the user doesn't provide a selection, looks at local package to find
     * those that can be updated and compute dependencies too.
     */
    public ArrayList<ArchiveInfo> computeUpdates(
            Collection<Archive> selectedArchives,
            RepoSources sources,
            Package[] localPkgs) {

        mSources = sources;
        ArrayList<ArchiveInfo> archives = new ArrayList<ArchiveInfo>();
        ArrayList<Package> remotePkgs = new ArrayList<Package>();

        if (selectedArchives == null) {
            selectedArchives = findUpdates(localPkgs, remotePkgs);
        }

        for (Archive a : selectedArchives) {
            insertArchive(a, archives, selectedArchives, remotePkgs, localPkgs, false);
        }

        return archives;
    }


    /**
     * Find suitable updates to all current local packages.
     */
    private Collection<Archive> findUpdates(Package[] localPkgs, ArrayList<Package> remotePkgs) {
        ArrayList<Archive> updates = new ArrayList<Archive>();

        fetchRemotePackages(remotePkgs);

        for (Package localPkg : localPkgs) {
            for (Package remotePkg : remotePkgs) {
                if (localPkg.canBeUpdatedBy(remotePkg) == UpdateInfo.UPDATE) {
                    // Found a suitable update. Only accept the remote package
                    // if it provides at least one compatible archive.

                    for (Archive a : remotePkg.getArchives()) {
                        if (a.isCompatible()) {
                            updates.add(a);
                            break;
                        }
                    }
                }
            }
        }

        return updates;
    }

    private ArchiveInfo insertArchive(Archive archive,
            ArrayList<ArchiveInfo> outArchives,
            Collection<Archive> selectedArchives,
            ArrayList<Package> remotePkgs,
            Package[] localPkgs,
            boolean automated) {
        Package p = archive.getParentPackage();

        // Is this an update?
        Archive updatedArchive = null;
        for (Package lp : localPkgs) {
            assert lp.getArchives().length == 1;
            if (lp.getArchives().length > 0 && lp.canBeUpdatedBy(p) == UpdateInfo.UPDATE) {
                updatedArchive = lp.getArchives()[0];
            }
        }

        // find dependencies
        ArchiveInfo dep = findDependency(p, outArchives, selectedArchives, remotePkgs, localPkgs);

        ArchiveInfo ai = new ArchiveInfo(
                archive, //newArchive
                updatedArchive, //replaced
                dep //dependsOn
                );

        outArchives.add(ai);
        if (dep != null) {
            dep.addDependencyFor(ai);
        }

        return ai;
    }

    private ArchiveInfo findDependency(Package pkg,
            ArrayList<ArchiveInfo> outArchives,
            Collection<Archive> selectedArchives,
            ArrayList<Package> remotePkgs,
            Package[] localPkgs) {

        // Current dependencies can be:
        // - addon: *always* depends on platform of same API level
        // - platform: *might* depends on tools of rev >= min-tools-rev

        if (pkg instanceof AddonPackage) {
            AddonPackage addon = (AddonPackage) pkg;

            return findPlatformDependency(
                    addon, outArchives, selectedArchives, remotePkgs, localPkgs);

        } else if (pkg instanceof MinToolsPackage) {
            MinToolsPackage platformOrExtra = (MinToolsPackage) pkg;

            return findToolsDependency(
                    platformOrExtra, outArchives, selectedArchives, remotePkgs, localPkgs);
        }

        return null;
    }

    /**
     * Resolves dependencies on tools.
     *
     * A platform or an extra package can both have a min-tools-rev, in which case it
     * depends on having a tools package of the requested revision.
     * Finds the tools dependency. If found, add it to the list of things to install.
     * Returns the archive info dependency, if any.
     */
    protected ArchiveInfo findToolsDependency(MinToolsPackage platformOrExtra,
            ArrayList<ArchiveInfo> outArchives,
            Collection<Archive> selectedArchives,
            ArrayList<Package> remotePkgs,
            Package[] localPkgs) {
        // This is the requirement to match.
        int rev = platformOrExtra.getMinToolsRevision();

        if (rev == MinToolsPackage.MIN_TOOLS_REV_NOT_SPECIFIED) {
            // Well actually there's no requirement.
            return null;
        }

        // First look in local packages.
        for (Package p : localPkgs) {
            if (p instanceof ToolPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // We found one already installed. We don't report this dependency
                    // as the UI only cares about resolving "newly added dependencies".
                    return null;
                }
            }
        }

        // Look in archives already scheduled for install
        for (ArchiveInfo ai : outArchives) {
            Package p = ai.getNewArchive().getParentPackage();
            if (p instanceof PlatformPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // The dependency is already scheduled for install, nothing else to do.
                    return ai;
                }
            }
        }

        // Otherwise look in the selected archives.
        for (Archive a : selectedArchives) {
            Package p = a.getParentPackage();
            if (p instanceof ToolPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // It's not already in the list of things to install, so add it now
                    return insertArchive(a, outArchives,
                            selectedArchives, remotePkgs, localPkgs,
                            true);
                }
            }
        }

        // Finally nothing matched, so let's look at all available remote packages
        fetchRemotePackages(remotePkgs);
        for (Package p : remotePkgs) {
            if (p instanceof ToolPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // It's not already in the list of things to install, so add the
                    // first compatible archive we can find.
                    for (Archive a : p.getArchives()) {
                        if (a.isCompatible()) {
                            return insertArchive(a, outArchives,
                                    selectedArchives, remotePkgs, localPkgs,
                                    true);
                        }
                    }
                }
            }
        }

        // We end up here if nothing matches. We don't have a good tools to match.
        // Seriously, that can't happens unless we totally screwed our repo manifest.
        // We'll let this one go through anyway.
        return null;
    }

    /**
     * Resolves dependencies on platform.
     *
     * An addon depends on having a platform with the same API version.
     * Finds the platform dependency. If found, add it to the list of things to install.
     * Returns the archive info dependency, if any.
     */
    protected ArchiveInfo findPlatformDependency(AddonPackage addon,
            ArrayList<ArchiveInfo> outArchives,
            Collection<Archive> selectedArchives,
            ArrayList<Package> remotePkgs,
            Package[] localPkgs) {
        // This is the requirement to match.
        AndroidVersion v = addon.getVersion();

        // Find a platform that would satisfy the requirement.

        // First look in local packages.
        for (Package p : localPkgs) {
            if (p instanceof PlatformPackage) {
                if (v.equals(((PlatformPackage) p).getVersion())) {
                    // We found one already installed. We don't report this dependency
                    // as the UI only cares about resolving "newly added dependencies".
                    return null;
                }
            }
        }

        // Look in archives already scheduled for install
        for (ArchiveInfo ai : outArchives) {
            Package p = ai.getNewArchive().getParentPackage();
            if (p instanceof PlatformPackage) {
                if (v.equals(((PlatformPackage) p).getVersion())) {
                    // The dependency is already scheduled for install, nothing else to do.
                    return ai;
                }
            }
        }

        // Otherwise look in the selected archives.
        for (Archive a : selectedArchives) {
            Package p = a.getParentPackage();
            if (p instanceof PlatformPackage) {
                if (v.equals(((PlatformPackage) p).getVersion())) {
                    // It's not already in the list of things to install, so add it now
                    return insertArchive(a, outArchives,
                            selectedArchives, remotePkgs, localPkgs,
                            true);
                }
            }
        }

        // Finally nothing matched, so let's look at all available remote packages
        fetchRemotePackages(remotePkgs);
        for (Package p : remotePkgs) {
            if (p instanceof PlatformPackage) {
                if (v.equals(((PlatformPackage) p).getVersion())) {
                    // It's not already in the list of things to install, so add the
                    // first compatible archive we can find.
                    for (Archive a : p.getArchives()) {
                        if (a.isCompatible()) {
                            return insertArchive(a, outArchives,
                                    selectedArchives, remotePkgs, localPkgs,
                                    true);
                        }
                    }
                }
            }
        }

        // We end up here if nothing matches. We don't have a good platform to match.
        // Seriously, that can't happens unless the repository contains a bogus addon
        // entry that does not match any existing platform API level.
        // It's conceivable that a 3rd part addon repo might have error, in which case
        // we'll let this one go through anyway.
        return null;
    }

    /** Fetch all remote packages only if really needed. */
    protected void fetchRemotePackages(ArrayList<Package> remotePkgs) {
        if (remotePkgs.size() > 0) {
            return;
        }

        // Get all the available packages from all loaded sources
        RepoSource[] remoteSources = mSources.getSources();

        for (RepoSource remoteSrc : remoteSources) {
            Package[] pkgs = remoteSrc.getPackages();
            if (pkgs != null) {
                nextPackage: for (Package pkg : pkgs) {
                    for (Archive a : pkg.getArchives()) {
                        // Only add a package if it contains at least one compatible archive
                        if (a.isCompatible()) {
                            remotePkgs.add(pkg);
                            continue nextPackage;
                        }
                    }
                }
            }
        }
    }

}
