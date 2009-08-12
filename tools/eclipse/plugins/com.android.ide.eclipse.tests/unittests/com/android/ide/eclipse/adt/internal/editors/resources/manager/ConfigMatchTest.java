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

package com.android.ide.eclipse.adt.internal.editors.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier.KeyboardState;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier.NavigationMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier.ScreenOrientation;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier.TextInputMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier.TouchScreenType;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolder;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.resources.manager.SingleResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IAbstractFolder;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IFileWrapper;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IFolderWrapper;
import com.android.ide.eclipse.mock.FileMock;
import com.android.ide.eclipse.mock.FolderMock;
import com.android.sdklib.IAndroidTarget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.TestCase;

public class ConfigMatchTest extends TestCase {
    private static final String SEARCHED_FILENAME = "main.xml"; //$NON-NLS-1$
    private static final String MISC1_FILENAME = "foo.xml"; //$NON-NLS-1$
    private static final String MISC2_FILENAME = "bar.xml"; //$NON-NLS-1$

    private ProjectResources mResources;
    private ResourceQualifier[] mQualifierList;
    private FolderConfiguration config4;
    private FolderConfiguration config3;
    private FolderConfiguration config2;
    private FolderConfiguration config1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create a Resource Manager to get a list of qualifier as instantiated by the real code.
        // Thanks for QualifierListTest we know this contains all the qualifiers.
        ResourceManager manager = ResourceManager.getInstance();
        Field qualifierListField = ResourceManager.class.getDeclaredField("mQualifiers");
        assertNotNull(qualifierListField);
        qualifierListField.setAccessible(true);

        // get the actual list.
        mQualifierList = (ResourceQualifier[])qualifierListField.get(manager);

        // create the project resources.
        mResources = new ProjectResources(false /* isFrameworkRepository */);

        // create 2 arrays of IResource. one with the filename being looked up, and one without.
        // Since the required API uses IResource, we can use MockFolder for them.
        FileMock[] validMemberList = new FileMock[] {
                new FileMock(MISC1_FILENAME),
                new FileMock(SEARCHED_FILENAME),
                new FileMock(MISC2_FILENAME),
        };
        FileMock[] invalidMemberList = new FileMock[] {
                new FileMock(MISC1_FILENAME),
                new FileMock(MISC2_FILENAME),
        };

        // add multiple ResourceFolder to the project resource.
        FolderConfiguration defaultConfig = getConfiguration(
                null, // country code
                null, // network code
                null, // language
                null, // region
                null, // screen orientation
                null, // dpi
                null, // touch mode
                null, // keyboard state
                null, // text input
                null, // navigation
                null); // screen size

        addFolder(mResources, defaultConfig, validMemberList);

        config1 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                null, // screen orientation
                null, // dpi
                null, // touch mode
                KeyboardState.EXPOSED.getValue(), // keyboard state
                null, // text input
                null, // navigation
                null); // screen size

        addFolder(mResources, config1, validMemberList);

        config2 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                null, // screen orientation
                null, // dpi
                null, // touch mode
                KeyboardState.HIDDEN.getValue(), // keyboard state
                null, // text input
                null, // navigation
                null); // screen size

        addFolder(mResources, config2, validMemberList);

        config3 = getConfiguration(
                null, // country code
                null, // network code
                "en", // language
                null, // region
                ScreenOrientation.LANDSCAPE.getValue(), // screen orientation
                null, // dpi
                null, // touch mode
                null, // keyboard state
                null, // text input
                null, // navigation
                null); // screen size

        addFolder(mResources, config3, validMemberList);

        config4 = getConfiguration(
                "mcc310", // country code
                "mnc435", // network code
                "en", // language
                "rUS", // region
                ScreenOrientation.LANDSCAPE.getValue(), // screen orientation
                "160dpi", // dpi
                TouchScreenType.FINGER.getValue(), // touch mode
                KeyboardState.EXPOSED.getValue(), // keyboard state
                TextInputMethod.QWERTY.getValue(), // text input
                NavigationMethod.DPAD.getValue(), // navigation
                "480x320"); // screen size

        addFolder(mResources, config4, invalidMemberList);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mResources = null;
    }

    public void test1() {
        FolderConfiguration testConfig = getConfiguration(
                "mcc310", // country code
                "mnc435", // network code
                "en", // language
                "rUS", // region
                ScreenOrientation.LANDSCAPE.getValue(), // screen orientation
                "160dpi", // dpi
                TouchScreenType.FINGER.getValue(), // touch mode
                KeyboardState.EXPOSED.getValue(), // keyboard state
                TextInputMethod.QWERTY.getValue(), // text input
                NavigationMethod.DPAD.getValue(), // navigation
                "480x320"); // screen size

        ResourceFile result = mResources.getMatchingFile(SEARCHED_FILENAME,
                ResourceFolderType.LAYOUT, testConfig);

        boolean bresult = result.getFolder().getConfiguration().equals(config3);
        assertEquals(bresult, true);
    }

    /**
     * Creates a {@link FolderConfiguration}.
     * @param qualifierValues The list of qualifier values. The length must equals the total number
     * of Qualifiers. <code>null</code> is permitted and will make the FolderConfiguration not use
     * this particular qualifier.
     */
    private FolderConfiguration getConfiguration(String... qualifierValues) {
        FolderConfiguration config = new FolderConfiguration();

        // those must be of the same length
        assertEquals(qualifierValues.length, mQualifierList.length);

        int index = 0;

        for (ResourceQualifier qualifier : mQualifierList) {
            String value = qualifierValues[index++];
            if (value != null) {
                assertTrue(qualifier.checkAndSet(value, config));
            }
        }

        return config;
    }

    /**
     * Adds a folder to the given {@link ProjectResources} with the given
     * {@link FolderConfiguration}. The folder is filled with files from the provided list.
     * @param resources the {@link ProjectResources} in which to add the folder.
     * @param config the {@link FolderConfiguration} for the created folder.
     * @param memberList the list of files for the folder.
     */
    private void addFolder(ProjectResources resources, FolderConfiguration config,
            FileMock[] memberList) throws Exception {

        // figure out the folder name based on the configuration
        String folderName = config.getFolderName(ResourceFolderType.LAYOUT, (IAndroidTarget)null);

        // create the folder mock
        FolderMock folder = new FolderMock(folderName, memberList);

        // add it to the resource, and get back a ResourceFolder object.
        ResourceFolder resFolder = _addProjectResourceFolder(resources, config, folder);

        // and fill it with files from the list.
        for (FileMock file : memberList) {
            resFolder.addFile(new SingleResourceFile(new IFileWrapper(file), resFolder));
        }
    }

    /** Calls ProjectResource.add method via reflection to circumvent access
     * restrictions that are enforced when running in the plug-in environment
     * ie cannot access package or protected members in a different plug-in, even
     * if they are in the same declared package as the accessor
     */
    private ResourceFolder _addProjectResourceFolder(ProjectResources resources,
            FolderConfiguration config, FolderMock folder) throws Exception {

        Method addMethod = ProjectResources.class.getDeclaredMethod("add",
                ResourceFolderType.class, FolderConfiguration.class,
                IAbstractFolder.class);
        addMethod.setAccessible(true);
        ResourceFolder resFolder = (ResourceFolder)addMethod.invoke(resources,
                ResourceFolderType.LAYOUT, config, new IFolderWrapper(folder));
        return resFolder;
    }
}
