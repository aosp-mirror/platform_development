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
import com.android.sdklib.internal.repository.DocPackage;
import com.android.sdklib.internal.repository.ExtraPackage;
import com.android.sdklib.internal.repository.IPackageVersion;
import com.android.sdklib.internal.repository.MinToolsPackage;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.PlatformPackage;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdklib.internal.repository.ToolPackage;
import com.android.sdklib.internal.repository.Package.UpdateInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * The logic to compute which packages to install, based on the choices
 * made by the user. This adds dependent packages as needed.
 * <p/>
 * When the user doesn't provide a selection, looks at local package to find
 * those that can be updated and compute dependencies too.
 */
class UpdaterLogic {

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

        ArrayList<ArchiveInfo> archives = new ArrayList<ArchiveInfo>();
        ArrayList<Package> remotePkgs = new ArrayList<Package>();
        RepoSource[] remoteSources = sources.getSources();

        if (selectedArchives == null) {
            selectedArchives = findUpdates(localPkgs, remotePkgs, remoteSources);
        }

        for (Archive a : selectedArchives) {
            insertArchive(a,
                    archives,
                    selectedArchives,
                    remotePkgs,
                    remoteSources,
                    localPkgs,
                    false /*automated*/);
        }

        return archives;
    }

    /**
     * Finds new platforms that the user does not have in his/her local SDK
     * and adds them to the list of archives to install.
     */
    public void addNewPlatforms(ArrayList<ArchiveInfo> archives,
            RepoSources sources,
            Package[] localPkgs) {

        // Find the highest platform installed
        float currentPlatformScore = 0;
        float currentAddonScore = 0;
        float currentDocScore = 0;
        HashMap<String, Float> currentExtraScore = new HashMap<String, Float>();
        for (Package p : localPkgs) {
            int rev = p.getRevision();
            int api = 0;
            boolean isPreview = false;
            if (p instanceof  IPackageVersion) {
                AndroidVersion vers = ((IPackageVersion) p).getVersion();
                api = vers.getApiLevel();
                isPreview = vers.isPreview();
            }

            // The score is 10*api + (1 if preview) + rev/100
            // This allows previews to rank above a non-preview and
            // allows revisions to rank appropriately.
            float score = api * 10 + (isPreview ? 1 : 0) + rev/100.f;

            if (p instanceof PlatformPackage) {
                currentPlatformScore = Math.max(currentPlatformScore, score);
            } else if (p instanceof AddonPackage) {
                currentAddonScore = Math.max(currentAddonScore, score);
            } else if (p instanceof ExtraPackage) {
                currentExtraScore.put(((ExtraPackage) p).getPath(), score);
            } else if (p instanceof DocPackage) {
                currentDocScore = Math.max(currentDocScore, score);
            }
        }

        RepoSource[] remoteSources = sources.getSources();
        ArrayList<Package> remotePkgs = new ArrayList<Package>();
        fetchRemotePackages(remotePkgs, remoteSources);

        Package suggestedDoc = null;

        for (Package p : remotePkgs) {
            int rev = p.getRevision();
            int api = 0;
            boolean isPreview = false;
            if (p instanceof  IPackageVersion) {
                AndroidVersion vers = ((IPackageVersion) p).getVersion();
                api = vers.getApiLevel();
                isPreview = vers.isPreview();
            }

            float score = api * 10 + (isPreview ? 1 : 0) + rev/100.f;

            boolean shouldAdd = false;
            if (p instanceof PlatformPackage) {
                shouldAdd = score > currentPlatformScore;
            } else if (p instanceof AddonPackage) {
                shouldAdd = score > currentAddonScore;
            } else if (p instanceof ExtraPackage) {
                String key = ((ExtraPackage) p).getPath();
                shouldAdd = !currentExtraScore.containsKey(key) ||
                    score > currentExtraScore.get(key).floatValue();
            } else if (p instanceof DocPackage) {
                // We don't want all the doc, only the most recent one
                if (score > currentDocScore) {
                    suggestedDoc = p;
                    currentDocScore = score;
                }
            }

            if (shouldAdd) {
                // We should suggest this package for installation.
                for (Archive a : p.getArchives()) {
                    if (a.isCompatible()) {
                        insertArchive(a,
                                archives,
                                null /*selectedArchives*/,
                                remotePkgs,
                                remoteSources,
                                localPkgs,
                                true /*automated*/);
                    }
                }
            }
        }

        if (suggestedDoc != null) {
            // We should suggest this package for installation.
            for (Archive a : suggestedDoc.getArchives()) {
                if (a.isCompatible()) {
                    insertArchive(a,
                            archives,
                            null /*selectedArchives*/,
                            remotePkgs,
                            remoteSources,
                            localPkgs,
                            true /*automated*/);
                }
            }
        }

    }

    /**
     * Find suitable updates to all current local packages.
     */
    private Collection<Archive> findUpdates(Package[] localPkgs,
            ArrayList<Package> remotePkgs,
            RepoSource[] remoteSources) {
        ArrayList<Archive> updates = new ArrayList<Archive>();

        fetchRemotePackages(remotePkgs, remoteSources);

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
            RepoSource[] remoteSources,
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
        ArchiveInfo dep = findDependency(p,
                outArchives,
                selectedArchives,
                remotePkgs,
                remoteSources,
                localPkgs);

        // Make sure it's not a dup
        ArchiveInfo ai = null;

        for (ArchiveInfo ai2 : outArchives) {
            if (ai2.getNewArchive().getParentPackage().sameItemAs(archive.getParentPackage())) {
                ai = ai2;
                break;
            }
        }

        if (ai == null) {
            ai = new ArchiveInfo(
                archive, //newArchive
                updatedArchive, //replaced
                dep //dependsOn
                );
            outArchives.add(ai);
        }

        if (dep != null) {
            dep.addDependencyFor(ai);
        }

        return ai;
    }

    private ArchiveInfo findDependency(Package pkg,
            ArrayList<ArchiveInfo> outArchives,
            Collection<Archive> selectedArchives,
            ArrayList<Package> remotePkgs,
            RepoSource[] remoteSources,
            Package[] localPkgs) {

        // Current dependencies can be:
        // - addon: *always* depends on platform of same API level
        // - platform: *might* depends on tools of rev >= min-tools-rev

        if (pkg instanceof AddonPackage) {
            AddonPackage addon = (AddonPackage) pkg;

            return findPlatformDependency(
                    addon,
                    outArchives,
                    selectedArchives,
                    remotePkgs,
                    remoteSources,
                    localPkgs);

        } else if (pkg instanceof MinToolsPackage) {
            MinToolsPackage platformOrExtra = (MinToolsPackage) pkg;

            return findToolsDependency(
                    platformOrExtra,
                    outArchives,
                    selectedArchives,
                    remotePkgs,
                    remoteSources,
                    localPkgs);
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
            RepoSource[] remoteSources,
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
            if (p instanceof ToolPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // The dependency is already scheduled for install, nothing else to do.
                    return ai;
                }
            }
        }

        // Otherwise look in the selected archives.
        if (selectedArchives != null) {
            for (Archive a : selectedArchives) {
                Package p = a.getParentPackage();
                if (p instanceof ToolPackage) {
                    if (((ToolPackage) p).getRevision() >= rev) {
                        // It's not already in the list of things to install, so add it now
                        return insertArchive(a, outArchives,
                                selectedArchives, remotePkgs, remoteSources, localPkgs,
                                true /*automated*/);
                    }
                }
            }
        }

        // Finally nothing matched, so let's look at all available remote packages
        fetchRemotePackages(remotePkgs, remoteSources);
        for (Package p : remotePkgs) {
            if (p instanceof ToolPackage) {
                if (((ToolPackage) p).getRevision() >= rev) {
                    // It's not already in the list of things to install, so add the
                    // first compatible archive we can find.
                    for (Archive a : p.getArchives()) {
                        if (a.isCompatible()) {
                            return insertArchive(a, outArchives,
                                    selectedArchives, remotePkgs, remoteSources, localPkgs,
                                    true /*automated*/);
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
            RepoSource[] remoteSources,
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
        if (selectedArchives != null) {
            for (Archive a : selectedArchives) {
                Package p = a.getParentPackage();
                if (p instanceof PlatformPackage) {
                    if (v.equals(((PlatformPackage) p).getVersion())) {
                        // It's not already in the list of things to install, so add it now
                        return insertArchive(a, outArchives,
                                selectedArchives, remotePkgs, remoteSources, localPkgs,
                                true /*automated*/);
                    }
                }
            }
        }

        // Finally nothing matched, so let's look at all available remote packages
        fetchRemotePackages(remotePkgs, remoteSources);
        for (Package p : remotePkgs) {
            if (p instanceof PlatformPackage) {
                if (v.equals(((PlatformPackage) p).getVersion())) {
                    // It's not already in the list of things to install, so add the
                    // first compatible archive we can find.
                    for (Archive a : p.getArchives()) {
                        if (a.isCompatible()) {
                            return insertArchive(a, outArchives,
                                    selectedArchives, remotePkgs, remoteSources, localPkgs,
                                    true /*automated*/);
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
    protected void fetchRemotePackages(ArrayList<Package> remotePkgs, RepoSource[] remoteSources) {
        if (remotePkgs.size() > 0) {
            return;
        }

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
