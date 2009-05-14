/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.ui;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Helpers for Eclipse UI related stuff.
 */
public final class EclipseUiHelper {

    /** View Id for the default Eclipse Content Outline view. */
    public static final String CONTENT_OUTLINE_VIEW_ID = "org.eclipse.ui.views.ContentOutline";
    /** View Id for the default Eclipse Property Sheet view. */
    public static final String PROPERTY_SHEET_VIEW_ID  = "org.eclipse.ui.views.PropertySheet";
    
    /** This class never gets instantiated. */
    private EclipseUiHelper() {
    }
    
    /**
     * Shows the corresponding view.
     * <p/>
     * Silently fails in case of error.
     * 
     * @param viewId One of {@link #CONTENT_OUTLINE_VIEW_ID}, {@link #PROPERTY_SHEET_VIEW_ID}.
     * @param activate True to force activate (i.e. takes focus), false to just make visible (i.e.
     *                 does not steal focus.)
     */
    public static void showView(String viewId, boolean activate) {
        IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (win != null) {
            IWorkbenchPage page = win.getActivePage();
            if (page != null) {
                try {
                    IViewPart part = page.showView(viewId,
                            null /* secondaryId */,
                            activate ? IWorkbenchPage.VIEW_ACTIVATE : IWorkbenchPage.VIEW_VISIBLE);
                } catch (PartInitException e) {
                    // ignore
                }
            }
        }
        
    }
}
