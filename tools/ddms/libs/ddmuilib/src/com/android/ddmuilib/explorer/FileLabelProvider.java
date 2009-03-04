/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib.explorer;

import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the FileEntry.
 */
class FileLabelProvider implements ILabelProvider, ITableLabelProvider {

    private Image mFileImage;
    private Image mFolderImage;
    private Image mPackageImage;
    private Image mOtherImage;

    /**
     * Creates Label provider with custom images.
     * @param fileImage the Image to represent a file
     * @param folderImage the Image to represent a folder
     * @param packageImage the Image to represent a .apk file. If null,
     *      fileImage is used instead.
     * @param otherImage the Image to represent all other entry type.
     */
    public FileLabelProvider(Image fileImage, Image folderImage,
            Image packageImage, Image otherImage) {
        mFileImage = fileImage;
        mFolderImage = folderImage;
        mOtherImage = otherImage;
        if (packageImage != null) {
            mPackageImage = packageImage;
        } else {
            mPackageImage = fileImage;
        }
    }

    /**
     * Creates a label provider with default images.
     *
     */
    public FileLabelProvider() {

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        return null;
    }

    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == 0) {
            if (element instanceof FileEntry) {
                FileEntry entry = (FileEntry)element;
                switch (entry.getType()) {
                    case FileListingService.TYPE_FILE:
                    case FileListingService.TYPE_LINK:
                        // get the name and extension
                        if (entry.isApplicationPackage()) {
                            return mPackageImage;
                        }
                        return mFileImage;
                    case FileListingService.TYPE_DIRECTORY:
                    case FileListingService.TYPE_DIRECTORY_LINK:
                        return mFolderImage;
                }
            }

            // default case return a different image.
            return mOtherImage;
        }
        return null;
    }

    public String getColumnText(Object element, int columnIndex) {
        if (element instanceof FileEntry) {
            FileEntry entry = (FileEntry)element;

            switch (columnIndex) {
                case 0:
                    return entry.getName();
                case 1:
                    return entry.getSize();
                case 2:
                    return entry.getDate();
                case 3:
                    return entry.getTime();
                case 4:
                    return entry.getPermissions();
                case 5:
                    return entry.getInfo();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
        // we don't need listeners.
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {
        // we don't need listeners
    }

}
