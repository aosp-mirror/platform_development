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
import com.android.ddmlib.FileListingService.IListingReceiver;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

/**
 * Content provider class for device Explorer.
 */
class DeviceContentProvider implements ITreeContentProvider {

    private TreeViewer mViewer;
    private FileListingService mFileListingService;
    private FileEntry mRootEntry;

    private IListingReceiver sListingReceiver = new IListingReceiver() {
        public void setChildren(final FileEntry entry, FileEntry[] children) {
            final Tree t = mViewer.getTree();
            if (t != null && t.isDisposed() == false) {
                Display display = t.getDisplay();
                if (display.isDisposed() == false) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            if (t.isDisposed() == false) {
                                // refresh the entry.
                                mViewer.refresh(entry);

                                // force it open, since on linux and windows
                                // when getChildren() returns null, the node is
                                // not considered expanded.
                                mViewer.setExpandedState(entry, true);
                            }
                        }
                    });
                }
            }
        }

        public void refreshEntry(final FileEntry entry) {
            final Tree t = mViewer.getTree();
            if (t != null && t.isDisposed() == false) {
                Display display = t.getDisplay();
                if (display.isDisposed() == false) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            if (t.isDisposed() == false) {
                                // refresh the entry.
                                mViewer.refresh(entry);
                            }
                        }
                    });
                }
            }
        }
    };

    /**
     *
     */
    public DeviceContentProvider() {
    }

    public void setListingService(FileListingService fls) {
        mFileListingService = fls;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof FileEntry) {
            FileEntry parentEntry = (FileEntry)parentElement;

            Object[] oldEntries = parentEntry.getCachedChildren();
            Object[] newEntries = mFileListingService.getChildren(parentEntry,
                    true, sListingReceiver);

            if (newEntries != null) {
                return newEntries;
            } else {
                // if null was returned, this means the cache was not valid,
                // and a thread was launched for ls. sListingReceiver will be
                // notified with the new entries.
                return oldEntries;
            }
        }
        return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof FileEntry) {
            FileEntry entry = (FileEntry)element;

            return entry.getParent();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        if (element instanceof FileEntry) {
            FileEntry entry = (FileEntry)element;

            return entry.getType() == FileListingService.TYPE_DIRECTORY;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof FileEntry) {
            FileEntry entry = (FileEntry)inputElement;
            if (entry.isRoot()) {
                return getChildren(mRootEntry);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (viewer instanceof TreeViewer) {
            mViewer = (TreeViewer)viewer;
        }
        if (newInput instanceof FileEntry) {
            mRootEntry = (FileEntry)newInput;
        }
    }
}
