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

import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.adt.wizards.newproject.StubSampleProjectWizard;
import com.android.ide.eclipse.tests.FuncTestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test case that verifies all SDK sample projects can be imported, built in
 * Eclipse
 * 
 * TODO: add support for deploying apps onto emulator and verifying successful
 * execution there
 * 
 */
public class SampleProjectTest extends FuncTestCase {
    
    private static final Logger sLogger = Logger.getLogger(SampleProjectTest.class.getName());

    /**
     * Tests the sample project with the given name
     * 
     * @param name - name of sample project to test
     */
    protected void doTestSampleProject(String name) {
        try {

            StubSampleProjectWizard newProjCreator = new StubSampleProjectWizard(
                    name, getOsSdkLocation());
            newProjCreator.init(null, null);
            newProjCreator.performFinish();

            IProject iproject = validateProjectExists(name);

            validateNoProblems(iproject);

        } 
        catch (CoreException e) {
            fail("Unexpected exception when creating sample project: " + e.toString());
        }
    }

    public void testApiDemos() {
        doTestSampleProject("ApiDemos");
    }

    public void testHelloActivity() {
        doTestSampleProject("HelloActivity");
    }

    public void testLunarLander() {
        doTestSampleProject("LunarLander");
    }

    public void testNotePad() {
        doTestSampleProject("NotePad");
    }

    public void testSkeletonApp() {
        doTestSampleProject("SkeletonApp");
    }

    public void testSnake() {
        doTestSampleProject("Snake");
    }

    private IProject validateProjectExists(String name) {
        IProject iproject = getIProject(name);
        assertTrue(iproject.exists());
        assertTrue(iproject.isOpen());
        return iproject;
    }

    private IProject getIProject(String name) {
        IProject iproject = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(name);
        return iproject;
    }

    private void validateNoProblems(IProject iproject) throws CoreException {
        waitForBuild(iproject);
        assertFalse(ProjectHelper.hasError(iproject, true));
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
                
            }
           if (Display.getCurrent() != null) {
               Display.getCurrent().readAndDispatch();
           }
        }
        
        sLogger.log(Level.SEVERE, "expected build event never happened?");
        fail("expected build event never happened for " + iproject.getName());

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
