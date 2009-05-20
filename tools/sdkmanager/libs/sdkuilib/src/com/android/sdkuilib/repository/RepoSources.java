/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.repository;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;

/**
 *
 */
class RepoSources {

    private Shell mShell;
    private ArrayList<RepoSource> mSources = new ArrayList<RepoSource>();

    public RepoSources() {
    }

    public void setShell(Shell shell) {
        mShell = shell;
    }

    public void add(RepoSource source) {
        mSources.add(source);
    }

    public ILabelProvider getLabelProvider() {
        return new ViewerLabelProvider();
    }


    public IContentProvider getContentProvider() {
        return new TreeContentProvider();
    }

    // ------------

    public class ViewerLabelProvider extends LabelProvider {
        /** Returns null by default */
        @Override
        public Image getImage(Object element) {
            return super.getImage(element);
        }

        /** Returns the toString of the element. */
        @Override
        public String getText(Object element) {
            return super.getText(element);
        }
    }

    // ------------

    private class TreeContentProvider implements ITreeContentProvider {

        private Object mInput;

        // Called when the viewer is disposed
        public void dispose() {
            // pass
        }

        // Called when the input is set or changed on the provider
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            mInput = newInput;
            // pass
        }

        /**
         * Called to collect the root elements for the given input.
         * The input here is a {@link RepoSources} object, this returns an array
         * of {@link RepoSource}.
         */
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        /**
         * Get the children of the given parent. This is requested on-demand as
         * nodes are expanded.
         *
         * For a {@link RepoSources} object, returns an array of {@link RepoSource}.
         * For a {@link RepoSource}, returns an array of packages.
         */
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof RepoSources) {
                return ((RepoSources) parentElement).mSources.toArray();

            } else if (parentElement instanceof RepoSource) {
                RepoSource source = (RepoSource) parentElement;
                ArrayList<String> pkgs = source.getPackages();

                if (pkgs == null) {
                    source.load(mShell);
                    pkgs = source.getPackages();
                }
                if (pkgs != null) {
                    return pkgs.toArray();
                }
            }

            return new Object[0];
        }

        /**
         * Returns the parent of a given element.
         * The input {@link RepoSources} is the parent of all {@link RepoSource} elements.
         */
        public Object getParent(Object element) {
            if (element instanceof RepoSource) {
                return mInput;
            }
            return null;
        }

        /**
         * Returns true if a given element has children, which is used to display a
         * "+/expand" box next to the tree node.
         * All {@link RepoSource} are expandable, whether they actually have any childre or not.
         */
        public boolean hasChildren(Object element) {
            return element instanceof RepoSource;
        }
    }

}
