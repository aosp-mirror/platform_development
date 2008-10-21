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
package com.android.ide.eclipse.ddms;

import com.android.ide.eclipse.ddms.views.DeviceView;
import com.android.ide.eclipse.ddms.views.EmulatorControlView;
import com.android.ide.eclipse.ddms.views.FileExplorerView;
import com.android.ide.eclipse.ddms.views.HeapView;
import com.android.ide.eclipse.ddms.views.LogCatView;
import com.android.ide.eclipse.ddms.views.ThreadView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

    public void createInitialLayout(IPageLayout layout) {
        // create a default layout that looks like the stand alone DDMS.

        // no editor window
        layout.setEditorAreaVisible(false);

        String editorArea = layout.getEditorArea();
        IFolderLayout folder;

        folder = layout.createFolder("logcat", IPageLayout.BOTTOM, 0.8f, //$NON-NLS-1$
                editorArea);
        folder.addPlaceholder(LogCatView.ID + ":*"); //$NON-NLS-1$
        folder.addView(LogCatView.ID);

        folder = layout.createFolder("devices", IPageLayout.LEFT, 0.3f, //$NON-NLS-1$
                editorArea);
        folder.addPlaceholder(DeviceView.ID + ":*"); //$NON-NLS-1$
        folder.addView(DeviceView.ID);

        folder = layout.createFolder("emulator", IPageLayout.BOTTOM, 0.5f, //$NON-NLS-1$
                "devices");
        folder.addPlaceholder(EmulatorControlView.ID + ":*"); //$NON-NLS-1$
        folder.addView(EmulatorControlView.ID);

        folder = layout.createFolder("ddms-detail", IPageLayout.RIGHT, 0.5f, //$NON-NLS-1$
                editorArea);
        folder.addPlaceholder(ThreadView.ID + ":*"); //$NON-NLS-1$
        folder.addView(ThreadView.ID);
        folder.addView(HeapView.ID);
        folder.addView(FileExplorerView.ID);

        layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
        layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective"); //$NON-NLS-1$
        layout.addPerspectiveShortcut("org.eclipse.jdt.ui.JavaPerspective"); //$NON-NLS-1$

        layout.addShowViewShortcut(DeviceView.ID);
        layout.addShowViewShortcut(FileExplorerView.ID);
        layout.addShowViewShortcut(HeapView.ID);
        layout.addShowViewShortcut(LogCatView.ID);
        layout.addShowViewShortcut(ThreadView.ID);

        layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
        layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
    }
}
