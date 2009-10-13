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

package com.android.sdklib.internal.repository;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

/**
 * A mock {@link AddonPackage} for testing.
 *
 * By design, this package contains one and only one archive.
 */
public class MockAddonPackage extends AddonPackage {

    /**
     * Creates a {@link MockAddonTarget} with the requested base platform and addon revision
     * and then a {@link MockAddonPackage} wrapping it.
     *
     * By design, this package contains one and only one archive.
     */
    public MockAddonPackage(MockPlatformPackage basePlatform, int revision) {
        super(new MockAddonTarget(basePlatform.getTarget(), revision), null /*props*/);
    }

    /**
     * A mock AddonTarget.
     * This reimplements the minimum needed from the interface for our limited testing needs.
     */
    static class MockAddonTarget implements IAndroidTarget {

        private final IAndroidTarget mParentTarget;
        private final int mRevision;

        public MockAddonTarget(IAndroidTarget parentTarget, int revision) {
            mParentTarget = parentTarget;
            mRevision = revision;
        }

        public String getClasspathName() {
            return null;
        }

        public String getDefaultSkin() {
            return null;
        }

        public String getDescription() {
            return "mock addon target";
        }

        public String getFullName() {
            return "mock addon target";
        }

        public String getLocation() {
            return "";
        }

        public String getName() {
            return "mock addon target";
        }

        public IOptionalLibrary[] getOptionalLibraries() {
            return null;
        }

        public IAndroidTarget getParent() {
            return mParentTarget;
        }

        public String getPath(int pathId) {
            return null;
        }

        public String[] getPlatformLibraries() {
            return null;
        }

        public int getRevision() {
            return mRevision;
        }

        public String[] getSkins() {
            return null;
        }

        public int getUsbVendorId() {
            return 0;
        }

        public String getVendor() {
            return null;
        }

        public AndroidVersion getVersion() {
            return mParentTarget.getVersion();
        }

        public String getVersionName() {
            return String.format("mock-addon-%1$d", getVersion().getApiLevel());
        }

        public String hashString() {
            return getVersionName();
        }

        /** Returns false for an addon. */
        public boolean isPlatform() {
            return false;
        }

        public boolean isCompatibleBaseFor(IAndroidTarget target) {
            throw new UnsupportedOperationException("Implement this as needed for tests");
        }

        public int compareTo(IAndroidTarget o) {
            throw new UnsupportedOperationException("Implement this as needed for tests");
        }

    }

}
