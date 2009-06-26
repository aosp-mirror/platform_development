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
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

/**
 * A list of sdk-repository sources.
 *
 * This implementation is UI dependent.
 */
class RepoSourcesAdapter {

    private final UpdaterData mUpdaterData;

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

    public class ViewerLabelProvider extends LabelProvider {

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
                final RepoSource source = (RepoSource) parentElement;
                Package[] packages = source.getPackages();

                if (packages == null) {

                    final boolean forceHttp = mUpdaterData.getSettingsController().getForceHttp();

                    mUpdaterData.getTaskFactory().start("Loading Source", new ITask() {
                        public void run(ITaskMonitor monitor) {
                            source.load(monitor, forceHttp);
                        }
                    });

                    packages = source.getPackages();
                }
                if (packages != null) {
                    return packages;
                }

            } else if (parentElement instanceof Package) {
                return ((Package) parentElement).getArchives();
            }

            return new Object[0];
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

}
