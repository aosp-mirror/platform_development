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

import com.android.sdklib.internal.repository.IDescription;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

/**
 * Table adapters to use the local SDK list.
 */
class LocalSdkAdapter  {

    private final UpdaterData mUpdaterData;

    public LocalSdkAdapter(UpdaterData updaterData) {
        mUpdaterData = updaterData;
    }

    public ILabelProvider getLabelProvider() {
        return new ViewerLabelProvider();
    }


    public IContentProvider getContentProvider() {
        return new TableContentProvider();
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

    private class TableContentProvider implements IStructuredContentProvider {

        // Called when the viewer is disposed
        public void dispose() {
            // pass
        }

        // Called when the input is set or changed on the provider
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            assert newInput == LocalSdkAdapter.this;
        }

        /**
         * Called to collect the root elements for the given input.
         * The input here is a {@link LocalSdkAdapter} object, this returns an array
         * of {@link RepoSource}.
         */
        public Object[] getElements(Object inputElement) {
            if (inputElement == LocalSdkAdapter.this) {
                Package[] packages = mUpdaterData.getInstalledPackage();

                if (packages != null) {
                    return packages;
                }
            }

            return new Object[0];
        }
    }

}
