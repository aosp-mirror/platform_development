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

package com.android.sdkuilib.repository;

import com.android.sdklib.ISdkLog;
import com.android.sdkuilib.internal.repository.UpdaterWindowImpl;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Opens an SDK Updater Window.
 *
 * This is the public interface for using the window.
 */
public class UpdaterWindow {

    private UpdaterWindowImpl mWindow;

    /**
     * Interface for listeners on SDK modifications (ie new installed compoments, or deleted
     * components)
     */
    public interface ISdkListener {
        /**
         * Sent when the content of the SDK changed
         */
        void onSdkChange();
    }

    /**
     * Creates a new window. Caller must call open(), which will block.
     * @param sdkLog
     * @param osSdkRoot The OS path to the SDK root.
     * @param userCanChangeSdkRoot If true, the window lets the user change the SDK path
     *                             being browsed.
     */
    public UpdaterWindow(Shell parentShell, ISdkLog sdkLog, String osSdkRoot,
            boolean userCanChangeSdkRoot) {
        mWindow = new UpdaterWindowImpl(parentShell, sdkLog, osSdkRoot, userCanChangeSdkRoot);
    }

    /**
     * Registers an extra page for the updater window.
     * <p/>
     * Pages must derive from {@link Composite} and implement a constructor that takes
     * a single parent {@link Composite} argument.
     * <p/>
     * All pages must be registered before the call to {@link #open()}.
     *
     * @param title The title of the page.
     * @param pageClass The {@link Composite}-derived class that will implement the page.
     */
    public void registerPage(String title, Class<? extends Composite> pageClass) {
        mWindow.registerExtraPage(title, pageClass);
    }

    /**
     * Adds a new listener to be notified when a change is made to the content of the SDK.
     */
    public void addListeners(ISdkListener listener) {
        mWindow.addListeners(listener);
    }

    /**
     * Removes a new listener to be notified anymore when a change is made to the content of
     * the SDK.
     */
    public void removeListener(ISdkListener listener) {
        mWindow.removeListener(listener);
    }

    /**
     * Opens the window.
     */
    public void open() {
        mWindow.open();
    }
}
