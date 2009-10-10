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

import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.MockAddonPackage;
import com.android.sdklib.internal.repository.MockPlatformPackage;
import com.android.sdklib.internal.repository.MockToolPackage;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

public class UpdaterLogicTest extends TestCase {

    private static class MockUpdaterLogic extends UpdaterLogic {
        private final Package[] mRemotePackages;

        public MockUpdaterLogic(Package[] remotePackages) {
            mRemotePackages = remotePackages;
        }

        @Override
        protected void fetchRemotePackages(ArrayList<Package> remotePkgs,
                RepoSource[] remoteSources) {
            // Ignore remoteSources and instead uses the remotePackages list given to the
            // constructor.
            if (mRemotePackages != null) {
                remotePkgs.addAll(Arrays.asList(mRemotePackages));
            }
        }
    }

    public void testFindAddonDependency() throws Exception {
        MockUpdaterLogic mul = new MockUpdaterLogic(null);

        MockPlatformPackage p1 = new MockPlatformPackage(1, 1);
        MockPlatformPackage p2 = new MockPlatformPackage(2, 1);

        MockAddonPackage a1 = new MockAddonPackage(p1, 1);
        MockAddonPackage a2 = new MockAddonPackage(p2, 2);

        ArrayList<ArchiveInfo> out = new ArrayList<ArchiveInfo>();
        ArrayList<Archive> selected = new ArrayList<Archive>();
        ArrayList<Package> remote = new ArrayList<Package>();

        // a2 depends on p2, which is not in the locals
        Package[] locals = { p1, a1 };
        RepoSource[] sources = null;
        assertNull(mul.findPlatformDependency(a2, out, selected, remote, sources, locals));
        assertEquals(0, out.size());

        // p2 is now selected, and should be scheduled for install in out
        Archive p2_archive = p2.getArchives()[0];
        selected.add(p2_archive);
        ArchiveInfo ai2 = mul.findPlatformDependency(a2, out, selected, remote, sources, locals);
        assertNotNull(ai2);
        assertSame(p2_archive, ai2.getNewArchive());
        assertEquals(1, out.size());
        assertSame(p2_archive, out.get(0).getNewArchive());
    }

    public void testFindPlatformDependency() throws Exception {
        MockUpdaterLogic mul = new MockUpdaterLogic(null);

        MockToolPackage t1 = new MockToolPackage(1);
        MockToolPackage t2 = new MockToolPackage(2);

        MockPlatformPackage p2 = new MockPlatformPackage(2, 1, 2);

        ArrayList<ArchiveInfo> out = new ArrayList<ArchiveInfo>();
        ArrayList<Archive> selected = new ArrayList<Archive>();
        ArrayList<Package> remote = new ArrayList<Package>();

        // p2 depends on t2, which is not locally installed
        Package[] locals = { t1 };
        RepoSource[] sources = null;
        assertNull(mul.findToolsDependency(p2, out, selected, remote, sources, locals));
        assertEquals(0, out.size());

        // t2 is now selected and can be used as a dependency
        Archive t2_archive = t2.getArchives()[0];
        selected.add(t2_archive);
        ArchiveInfo ai2 = mul.findToolsDependency(p2, out, selected, remote, sources, locals);
        assertNotNull(ai2);
        assertSame(t2_archive, ai2.getNewArchive());
        assertEquals(1, out.size());
        assertSame(t2_archive, out.get(0).getNewArchive());
    }
}
