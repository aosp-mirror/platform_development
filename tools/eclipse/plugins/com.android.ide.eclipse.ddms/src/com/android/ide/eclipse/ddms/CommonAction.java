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

import com.android.ddmuilib.actions.ICommonAction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Basic action extending the jFace Action class in order to implement
 * ICommonAction.
 */
public class CommonAction extends Action implements ICommonAction {
    
    private Runnable mRunnable;

    public CommonAction() {
        super();
    }

    public CommonAction(String text) {
        super(text);
    }

    /**
     * @param text
     * @param image
     */
    public CommonAction(String text, ImageDescriptor image) {
        super(text, image);
    }

    /**
     * @param text
     * @param style
     */
    public CommonAction(String text, int style) {
        super(text, style);
    }
    
    @Override
    public void run() {
        if (mRunnable != null) {
            mRunnable.run();
        }
    }
    
    /**
     * Sets the {@link Runnable}.
     * @see ICommonAction#setRunnable(Runnable)
     */
    public void setRunnable(Runnable runnable) {
        mRunnable = runnable;
    }
}
