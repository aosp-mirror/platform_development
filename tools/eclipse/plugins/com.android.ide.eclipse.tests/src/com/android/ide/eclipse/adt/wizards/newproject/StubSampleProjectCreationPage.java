/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 *      http://www.eclipse.org/org/documents/epl-v10.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.adt.wizards.newproject;

import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreationPage;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * Stub class for project creation page Returns canned responses for creating a
 * sample project
 */
public class StubSampleProjectCreationPage extends NewProjectCreationPage {

    private String mSampleProjectName;
    private String mOsSdkLocation;

    public StubSampleProjectCreationPage(String sampleProjectName, String osSdkLocation) {
        super();
        this.mSampleProjectName = sampleProjectName;
        this.mOsSdkLocation = osSdkLocation;
    }

    @Override
    public IMainInfo getMainInfo() {
        return new IMainInfo() {
            public String getProjectName() {
                return mSampleProjectName;
            }

            public String getPackageName() {
                return "com.android.samples";
            }

            public String getActivityName() {
                return mSampleProjectName;
            }

            public String getApplicationName() {
                return mSampleProjectName;
            }

            public boolean isNewProject() {
                return false;
            }

            public String getSourceFolder() {
                return "src";
            }

            public IPath getLocationPath() {
                return new Path(mOsSdkLocation + File.separator +
                        "samples" + File.separator +
                        mSampleProjectName);
            }

            public String getMinSdkVersion() {
                return null;
            }

            public IAndroidTarget getSdkTarget() {
                return null;
            }

            public boolean isCreateActivity() {
                return false;
            }

            public boolean useDefaultLocation() {
                return false;
            }
        };
    }
}
