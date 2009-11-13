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
package com.android.ide.eclipse.tests.functests.sampleProjects;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.wizards.newproject.StubProjectWizard;
import com.android.ide.eclipse.tests.SdkTestCase;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test case that verifies all SDK sample projects can be imported, and built in
 * Eclipse.
 * <p/>
 * TODO: add support for deploying apps onto emulator and verifying successful
 * execution there
 *
 */
public class SampleProjectTest extends SdkTestCase {

    private static final Logger sLogger = Logger.getLogger(SampleProjectTest.class.getName());

    /**
     * Finds all samples projects in set SDK and verify they can be built in Eclipse.
     * <p/>
     * TODO: add install and run on emulator test
     * @throws CoreException
     */
    public void testSamples() throws CoreException {
        // TODO: For reporting purposes, it would be better if a separate test success or failure
        // could be reported for each sample
        IAndroidTarget[] targets = getSdk().getTargets();
        for (IAndroidTarget target : targets) {
            doTestSamplesForTarget(target);
        }
    }

    private void doTestSamplesForTarget(IAndroidTarget target) throws CoreException {
        String path = target.getPath(IAndroidTarget.SAMPLES);
        File samples = new File(path);
        if (samples.isDirectory()) {
            File[] files = samples.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    doTestSampleProject(file.getName(), file.getAbsolutePath(), target);
                }
            }
        }
    }

    /**
     * Tests the sample project with the given name
     *
     * @param target - SDK target of project
     * @param name - name of sample project to test
     * @param path - absolute file system path
     * @throws CoreException
     */
    private void doTestSampleProject(String name, String path, IAndroidTarget target)
             throws CoreException {
        IProject iproject = null;
        try {
            sLogger.log(Level.INFO, String.format("Testing sample %s for target %s", name,
                    target.getName()));

            prepareProject(path, target);

            final StubProjectWizard newProjCreator = new StubProjectWizard(
                    name, path, target);
            newProjCreator.init(null, null);
            // need to run finish on ui thread since it invokes a perspective switch
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    newProjCreator.performFinish();
                }
            });

            iproject = validateProjectExists(name);
            validateNoProblems(iproject);
        }
        catch (CoreException e) {
            sLogger.log(Level.SEVERE,
                    String.format("Unexpected exception when creating sample project %s " +
                            "for target %s", name, target.getName()));
            throw e;
        } finally {
            if (iproject != null) {
                iproject.delete(false, true, new NullProgressMonitor());
            }
        }
    }

    private void prepareProject(String path, IAndroidTarget target) {
        if (target.getVersion().isPreview()) {
            // need to explicitly set preview's version in manifest for project to compile
            final String manifestPath = path + File.separatorChar +
                    AndroidConstants.FN_ANDROID_MANIFEST;
            AndroidManifestWriter manifestWriter =
                AndroidManifestWriter.parse(manifestPath);
            assertNotNull(String.format("could not read manifest %s", manifestPath),
                    manifestWriter);
            assertTrue(manifestWriter.setMinSdkVersion(target.getVersion().getApiString()));
        }
    }

    private IProject validateProjectExists(String name) {
        IProject iproject = getIProject(name);
        assertTrue(String.format("%s project not created", name), iproject.exists());
        assertTrue(String.format("%s project not opened", name), iproject.isOpen());
        return iproject;
    }

    private IProject getIProject(String name) {
        IProject iproject = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(name);
        return iproject;
    }

    private void validateNoProblems(IProject iproject) throws CoreException {
        waitForBuild(iproject);

        boolean hasErrors = false;
        StringBuilder failureBuilder = new StringBuilder(String.format("%s project has errors:",
                iproject.getName()));
        IMarker[] markers = iproject.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        if (markers != null && markers.length > 0) {
            // the project has marker(s). even though they are "problem" we
            // don't know their severity. so we loop on them and figure if they
            // are warnings or errors
            for (IMarker m : markers) {
                int s = m.getAttribute(IMarker.SEVERITY, -1);
                if (s == IMarker.SEVERITY_ERROR) {
                    hasErrors = true;
                    failureBuilder.append("\n");
                    failureBuilder.append(m.getAttribute(IMarker.MESSAGE, ""));
                }
            }
        }
        assertFalse(failureBuilder.toString(), hasErrors);
    }

    /**
     * Waits for build to complete.
     *
     * @param iproject
     */
    private void waitForBuild(final IProject iproject) {

        final BuiltProjectDeltaVisitor deltaVisitor = new BuiltProjectDeltaVisitor(iproject);
        IResourceChangeListener newBuildListener = new IResourceChangeListener() {

            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    event.getDelta().accept(deltaVisitor);
                }
                catch (CoreException e) {
                    fail();
                }
            }

        };
        iproject.getWorkspace().addResourceChangeListener(newBuildListener,
          IResourceChangeEvent.POST_BUILD);

        // poll build listener to determine when build is done
        // loop max of 1200 times * 50 ms = 60 seconds
        final int maxWait = 1200;
        for (int i=0; i < maxWait; i++) {
            if (deltaVisitor.isProjectBuilt()) {
                return;
            }
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                // ignore
            }
           if (Display.getCurrent() != null) {
               Display.getCurrent().readAndDispatch();
           }
        }

        sLogger.log(Level.SEVERE, "expected build event never happened?");
        fail(String.format("Expected build event never happened for %s", iproject.getName()));
    }

    /**
     * Scans a given IResourceDelta looking for a "build event" change for given IProject
     *
     */
    private class BuiltProjectDeltaVisitor implements IResourceDeltaVisitor {

        private IProject mIProject;
        private boolean  mIsBuilt;

        public BuiltProjectDeltaVisitor(IProject iproject) {
            mIProject = iproject;
            mIsBuilt = false;
        }

        public boolean visit(IResourceDelta delta) {
            if (mIProject.equals(delta.getResource())) {
                setBuilt(true);
                return false;
            }
            return true;
        }

        private synchronized void setBuilt(boolean b) {
            mIsBuilt = b;
        }

        public synchronized boolean isProjectBuilt() {
            return mIsBuilt;
        }
    }
}
