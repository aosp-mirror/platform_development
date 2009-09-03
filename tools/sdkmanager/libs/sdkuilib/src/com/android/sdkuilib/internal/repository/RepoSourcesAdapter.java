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
import com.android.sdklib.internal.repository.IDescription;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.Package.UpdateInfo;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;

/**
 * A list of sdk-repository sources.
 *
 * This implementation is UI dependent.
 */
public class RepoSourcesAdapter {

    private final UpdaterData mUpdaterData;

    /**
     * A dummy RepoSource entry returned for sources which had load errors.
     * It displays a summary of the error as its short description or
     * it displays the source's long description.
     */
    public static class RepoSourceError implements IDescription {

        private final RepoSource mSource;

        public RepoSourceError(RepoSource source) {
            mSource = source;
        }

        public String getLongDescription() {
            return mSource.getLongDescription();
        }

        public String getShortDescription() {
            return mSource.getFetchError();
        }
    }

    /**
     * A dummy RepoSource entry returned for sources with no packages.
     * We need that to force the SWT tree to display an open/close triangle
     * even for empty sources.
     */
    public static class RepoSourceEmpty implements IDescription {

        private final RepoSource mSource;
        private final boolean mEmptyBecauseOfUpdateOnly;

        public RepoSourceEmpty(RepoSource source, boolean emptyBecauseOfUpdateOnly) {
            mSource = source;
            mEmptyBecauseOfUpdateOnly = emptyBecauseOfUpdateOnly;
        }

        public String getLongDescription() {
            return mSource.getLongDescription();
        }

        public String getShortDescription() {
            if (mEmptyBecauseOfUpdateOnly) {
                return "Some packages were found but are not compatible updates.";
            } else {
                return "No packages found";
            }
        }
    }

    public RepoSourcesAdapter(UpdaterData updaterData) {
        mUpdaterData = updaterData;
    }

    public ILabelProvider getLabelProvider() {
        return new ViewerLabelProvider();
    }


    public IContentProvider getContentProvider() {
        return new TreeContentProvider();
    }

    // ------------

    private class ViewerLabelProvider extends LabelProvider {

        /** Returns an image appropriate for this element. */
        @Override
        public Image getImage(Object element) {

            ImageFactory imgFactory = mUpdaterData.getImageFactory();

            if (imgFactory != null) {
                return imgFactory.getImageForObject(element);
            }

            return super.getImage(element);
        }

        /** Returns the toString of the element. */
        @Override
        public String getText(Object element) {
            if (element instanceof IDescription) {
                return ((IDescription) element).getShortDescription();
            }
            return super.getText(element);
        }
    }

    // ------------

    private class TreeContentProvider implements ITreeContentProvider {

        // Called when the viewer is disposed
        public void dispose() {
            // pass
        }

        // Called when the input is set or changed on the provider
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            assert newInput == RepoSourcesAdapter.this;
        }

        /**
         * Called to collect the root elements for the given input.
         * The input here is a {@link RepoSourcesAdapter} object, this returns an array
         * of {@link RepoSource}.
         */
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        /**
         * Get the children of the given parent. This is requested on-demand as
         * nodes are expanded.
         *
         * For a {@link RepoSourcesAdapter} object, returns an array of {@link RepoSource}s.
         * For a {@link RepoSource}, returns an array of {@link Package}s.
         * For a {@link Package}, returns an array of {@link Archive}s.
         */
        public Object[] getChildren(Object parentElement) {
            if (parentElement == RepoSourcesAdapter.this) {
                return mUpdaterData.getSources().getSources();

            } else if (parentElement instanceof RepoSource) {
                return getRepoSourceChildren((RepoSource) parentElement);

            } else if (parentElement instanceof Package) {
                return getPackageChildren((Package) parentElement);
            }

            return new Object[0];
        }

        /**
         * Returns the list of packages for this repo source, eventually filtered to display
         * only update packages. If the list is empty, returns a specific empty node. If there's
         * an error, returns a specific error node.
         */
        private Object[] getRepoSourceChildren(final RepoSource source) {
            Package[] packages = source.getPackages();

            if (packages == null && source.getFetchError() == null) {
                final boolean forceHttp = mUpdaterData.getSettingsController().getForceHttp();

                mUpdaterData.getTaskFactory().start("Loading Source", new ITask() {
                    public void run(ITaskMonitor monitor) {
                        source.load(monitor, forceHttp);
                    }
                });

                packages = source.getPackages();
            }

            boolean wasEmptyBeforeFilter = (packages == null || packages.length == 0);

            // filter out only the packages that are new/upgrade.
            if (packages != null && mUpdaterData.getSettingsController().getShowUpdateOnly()) {
                packages = filteredPackages(packages);
            }
            if (packages != null && packages.length == 0) {
                packages = null;
            }

            if (packages != null && source.getFetchError() != null) {
                // Return a dummy entry to display the fetch error
                return new Object[] { new RepoSourceError(source) };
            }

            // Either return a non-null package list or create a new empty node
            if (packages != null) {
                return packages;
            } else {
                return new Object[] { new RepoSourceEmpty(source, !wasEmptyBeforeFilter) } ;
            }
        }

        /**
         * Returns the list of archives for the given package, eventually filtering it
         * to only show the compatible archives.
         */
        private Object[] getPackageChildren(Package pkg) {
            Archive[] archives = pkg.getArchives();
            if (mUpdaterData.getSettingsController().getShowUpdateOnly()) {
                for (Archive archive : archives) {
                    // if we only want the compatible archives, then we just take the first
                    // one. it's unlikely there are 2 compatible archives for the same
                    // package
                    if (archive.isCompatible()) {
                        return new Object[] { archive };
                    }
                }
            }

            return archives;
        }

        /**
         * Returns the parent of a given element.
         * The input {@link RepoSourcesAdapter} is the parent of all {@link RepoSource} elements.
         */
        public Object getParent(Object element) {

            if (element instanceof RepoSource) {
                return RepoSourcesAdapter.this;

            } else if (element instanceof Package) {
                return ((Package) element).getParentSource();
            }
            return null;
        }

        /**
         * Returns true if a given element has children, which is used to display a
         * "+/expand" box next to the tree node.
         * All {@link RepoSource} and {@link Package} are expandable, whether they actually
         * have any children or not.
         */
        public boolean hasChildren(Object element) {
            return element instanceof RepoSource || element instanceof Package;
        }
    }

    /**
     * Filters out a list of remote packages to only keep the ones that are either new or
     * updates of existing package.
     * @param remotePackages the list of packages to filter.
     * @return a non null (but maybe empty) list of new or update packages.
     */
    private Package[] filteredPackages(Package[] remotePackages) {
        // get the installed packages
        Package[] installedPackages = mUpdaterData.getInstalledPackage();

        ArrayList<Package> filteredList = new ArrayList<Package>();

        // for each remote packages, we look for an existing version.
        // If no existing version -> add to the list
        // if existing version but with older revision -> add it to the list
        for (Package remotePkg : remotePackages) {
            boolean newPkg = true;

            // For all potential packages, we also make sure that there's an archive for the current
            // platform, or we simply skip them.
            if (remotePkg.hasCompatibleArchive()) {
                for (Package installedPkg : installedPackages) {
                    UpdateInfo info = installedPkg.canBeUpdatedBy(remotePkg);
                    if (info == UpdateInfo.UPDATE) {
                        filteredList.add(remotePkg);
                        newPkg = false;
                        break; // there shouldn't be 2 revisions of the same package
                    } else if (info != UpdateInfo.INCOMPATIBLE) {
                        newPkg = false;
                        break; // there shouldn't be 2 revisions of the same package
                    }
                }

                // if we have not found the same package, then we add it (it's a new package)
                if (newPkg) {
                    filteredList.add(remotePkg);
                }
            }
        }

        return filteredList.toArray(new Package[filteredList.size()]);
    }
}
