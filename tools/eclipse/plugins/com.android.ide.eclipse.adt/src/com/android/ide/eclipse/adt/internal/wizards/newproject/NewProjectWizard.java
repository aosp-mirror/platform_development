/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.wizards.newproject;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidNature;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreationPage.IMainInfo;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewTestProjectCreationPage.TestInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.actions.OpenJavaPerspectiveAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A "New Android Project" Wizard.
 * <p/>
 * Note: this class is public so that it can be accessed from unit tests.
 * It is however an internal class. Its API may change without notice.
 * It should semantically be considered as a private final class.
 * Do not derive from this class.

 */
public class NewProjectWizard extends Wizard implements INewWizard {

    /**
     * Indicates which pages should be available in the New Project Wizard.
     */
    protected enum AvailablePages {
        /**
         * Both the usual "Android Project" and the "Android Test Project" pages will
         * be available. The first page displayed will be the former one and it can depend
         * on the soon-to-be created normal project.
         */
        ANDROID_AND_TEST_PROJECT,
        /**
         * Only the "Android Test Project" page will be available. User will have to
         * select an existing Android Project. If the selection matches such a project,
         * it will be used as a default.
         */
        TEST_PROJECT_ONLY
    }

    private static final String PARAM_SDK_TOOLS_DIR = "ANDROID_SDK_TOOLS";          //$NON-NLS-1$
    private static final String PARAM_ACTIVITY = "ACTIVITY_NAME";                   //$NON-NLS-1$
    private static final String PARAM_APPLICATION = "APPLICATION_NAME";             //$NON-NLS-1$
    private static final String PARAM_PACKAGE = "PACKAGE";                          //$NON-NLS-1$
    private static final String PARAM_PROJECT = "PROJECT_NAME";                     //$NON-NLS-1$
    private static final String PARAM_STRING_NAME = "STRING_NAME";                  //$NON-NLS-1$
    private static final String PARAM_STRING_CONTENT = "STRING_CONTENT";            //$NON-NLS-1$
    private static final String PARAM_IS_NEW_PROJECT = "IS_NEW_PROJECT";            //$NON-NLS-1$
    private static final String PARAM_SRC_FOLDER = "SRC_FOLDER";                    //$NON-NLS-1$
    private static final String PARAM_SDK_TARGET = "SDK_TARGET";                    //$NON-NLS-1$
    private static final String PARAM_MIN_SDK_VERSION = "MIN_SDK_VERSION";          //$NON-NLS-1$
    // Warning: The expanded string PARAM_TEST_TARGET_PACKAGE must not contain the
    // string "PACKAGE" since it collides with the replacement of PARAM_PACKAGE.
    private static final String PARAM_TEST_TARGET_PACKAGE = "TEST_TARGET_PCKG";     //$NON-NLS-1$
    private static final String PARAM_TARGET_SELF = "TARGET_SELF";                  //$NON-NLS-1$
    private static final String PARAM_TARGET_MAIN = "TARGET_MAIN";                  //$NON-NLS-1$
    private static final String PARAM_TARGET_EXISTING = "TARGET_EXISTING";          //$NON-NLS-1$
    private static final String PARAM_REFERENCE_PROJECT = "REFERENCE_PROJECT";      //$NON-NLS-1$

    private static final String PH_ACTIVITIES = "ACTIVITIES";                       //$NON-NLS-1$
    private static final String PH_USES_SDK = "USES-SDK";                           //$NON-NLS-1$
    private static final String PH_INTENT_FILTERS = "INTENT_FILTERS";               //$NON-NLS-1$
    private static final String PH_STRINGS = "STRINGS";                             //$NON-NLS-1$
    private static final String PH_TEST_USES_LIBRARY = "TEST-USES-LIBRARY";         //$NON-NLS-1$
    private static final String PH_TEST_INSTRUMENTATION = "TEST-INSTRUMENTATION";   //$NON-NLS-1$

    private static final String BIN_DIRECTORY =
        SdkConstants.FD_OUTPUT + AndroidConstants.WS_SEP;
    private static final String RES_DIRECTORY =
        SdkConstants.FD_RESOURCES + AndroidConstants.WS_SEP;
    private static final String ASSETS_DIRECTORY =
        SdkConstants.FD_ASSETS + AndroidConstants.WS_SEP;
    private static final String DRAWABLE_DIRECTORY =
        SdkConstants.FD_DRAWABLE + AndroidConstants.WS_SEP;
    private static final String LAYOUT_DIRECTORY =
        SdkConstants.FD_LAYOUT + AndroidConstants.WS_SEP;
    private static final String VALUES_DIRECTORY =
        SdkConstants.FD_VALUES + AndroidConstants.WS_SEP;
    private static final String GEN_SRC_DIRECTORY =
        SdkConstants.FD_GEN_SOURCES + AndroidConstants.WS_SEP;

    private static final String TEMPLATES_DIRECTORY = "templates/"; //$NON-NLS-1$
    private static final String TEMPLATE_MANIFEST = TEMPLATES_DIRECTORY
            + "AndroidManifest.template"; //$NON-NLS-1$
    private static final String TEMPLATE_ACTIVITIES = TEMPLATES_DIRECTORY
            + "activity.template"; //$NON-NLS-1$
    private static final String TEMPLATE_USES_SDK = TEMPLATES_DIRECTORY
            + "uses-sdk.template"; //$NON-NLS-1$
    private static final String TEMPLATE_INTENT_LAUNCHER = TEMPLATES_DIRECTORY
            + "launcher_intent_filter.template"; //$NON-NLS-1$
    private static final String TEMPLATE_TEST_USES_LIBRARY = TEMPLATES_DIRECTORY
            + "test_uses-library.template"; //$NON-NLS-1$
    private static final String TEMPLATE_TEST_INSTRUMENTATION = TEMPLATES_DIRECTORY
            + "test_instrumentation.template"; //$NON-NLS-1$



    private static final String TEMPLATE_STRINGS = TEMPLATES_DIRECTORY
            + "strings.template"; //$NON-NLS-1$
    private static final String TEMPLATE_STRING = TEMPLATES_DIRECTORY
            + "string.template"; //$NON-NLS-1$
    private static final String ICON = "icon.png"; //$NON-NLS-1$

    private static final String STRINGS_FILE = "strings.xml";       //$NON-NLS-1$

    private static final String STRING_RSRC_PREFIX = "@string/";    //$NON-NLS-1$
    private static final String STRING_APP_NAME = "app_name";       //$NON-NLS-1$
    private static final String STRING_HELLO_WORLD = "hello";       //$NON-NLS-1$

    private static final String[] DEFAULT_DIRECTORIES = new String[] {
            BIN_DIRECTORY, RES_DIRECTORY, ASSETS_DIRECTORY };
    private static final String[] RES_DIRECTORIES = new String[] {
            DRAWABLE_DIRECTORY, LAYOUT_DIRECTORY, VALUES_DIRECTORY};

    private static final String PROJECT_LOGO_LARGE = "icons/android_large.png"; //$NON-NLS-1$
    private static final String JAVA_ACTIVITY_TEMPLATE = "java_file.template";  //$NON-NLS-1$
    private static final String LAYOUT_TEMPLATE = "layout.template";            //$NON-NLS-1$
    private static final String MAIN_LAYOUT_XML = "main.xml";                   //$NON-NLS-1$

    private NewProjectCreationPage mMainPage;
    private NewTestProjectCreationPage mTestPage;
    /** Package name available when the wizard completes. */
    private String mPackageName;
    private final AvailablePages mAvailablePages;

    public NewProjectWizard() {
        this(AvailablePages.ANDROID_AND_TEST_PROJECT);
    }

    protected NewProjectWizard(AvailablePages availablePages) {
        mAvailablePages = availablePages;
    }

    /**
     * Initializes this creation wizard using the passed workbench and object
     * selection. Inherited from org.eclipse.ui.IWorkbenchWizard
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setHelpAvailable(false); // TODO have help
        setWindowTitle("New Android Project");
        setImageDescriptor();

        if (mAvailablePages == AvailablePages.ANDROID_AND_TEST_PROJECT) {
            mMainPage = createMainPage();
        }
        mTestPage = createTestPage();
    }

    /**
     * Creates the main wizard page.
     * <p/>
     * Please do NOT override this method.
     * <p/>
     * This is protected so that it can be overridden by unit tests.
     * However the contract of this class is private and NO ATTEMPT will be made
     * to maintain compatibility between different versions of the plugin.
     */
    protected NewProjectCreationPage createMainPage() {
        return new NewProjectCreationPage();
    }

    /**
     * Creates the test wizard page.
     * <p/>
     * Please do NOT override this method.
     * <p/>
     * This is protected so that it can be overridden by unit tests.
     * However the contract of this class is private and NO ATTEMPT will be made
     * to maintain compatibility between different versions of the plugin.
     */
    protected NewTestProjectCreationPage createTestPage() {
        return new NewTestProjectCreationPage();
    }

    // -- Methods inherited from org.eclipse.jface.wizard.Wizard --
    // The Wizard class implements most defaults and boilerplate code needed by
    // IWizard

    /**
     * Adds pages to this wizard.
     */
    @Override
    public void addPages() {
        if (mAvailablePages == AvailablePages.ANDROID_AND_TEST_PROJECT) {
            addPage(mMainPage);
        }
        addPage(mTestPage);

        if (mMainPage != null && mTestPage != null) {
            mTestPage.setMainInfo(mMainPage.getMainInfo());
            mMainPage.setTestInfo(mTestPage.getTestInfo());
        }
    }

    /**
     * Performs any actions appropriate in response to the user having pressed
     * the Finish button, or refuse if finishing now is not permitted: here, it
     * actually creates the workspace project and then switch to the Java
     * perspective.
     *
     * @return True
     */
    @Override
    public boolean performFinish() {
        if (!createAndroidProjects()) {
            return false;
        }

        // Open the default Java Perspective
        OpenJavaPerspectiveAction action = new OpenJavaPerspectiveAction();
        action.run();
        return true;
    }

    // -- Public Fields --

    /** Returns the main project package name. Only valid once the wizard finishes. */
    public String getPackageName() {
        return mPackageName;
    }

    // -- Custom Methods --

    /**
     * Before actually creating the project for a new project (as opposed to using an
     * existing project), we check if the target location is a directory that either does
     * not exist or is empty.
     *
     * If it's not empty, ask the user for confirmation.
     *
     * @param destination The destination folder where the new project is to be created.
     * @return True if the destination doesn't exist yet or is an empty directory or is
     *         accepted by the user.
     */
    private boolean validateNewProjectLocationIsEmpty(IPath destination) {
        File f = new File(destination.toOSString());
        if (f.isDirectory() && f.list().length > 0) {
            return AdtPlugin.displayPrompt("New Android Project",
                    "You are going to create a new Android Project in an existing, non-empty, directory. Are you sure you want to proceed?");
        }
        return true;
    }

    /**
     * Structure that describes all the information needed to create a project.
     * This is collected from the pages by {@link NewProjectWizard#createAndroidProjects()}
     * and then used by
     * {@link NewProjectWizard#createProjectAsync(IProgressMonitor, ProjectInfo, ProjectInfo)}.
     */
    private static class ProjectInfo {
        private final IProject mProject;
        private final IProjectDescription mDescription;
        private final Map<String, Object> mParameters;
        private final HashMap<String, String> mDictionary;

        public ProjectInfo(IProject project,
                IProjectDescription description,
                Map<String, Object> parameters,
                HashMap<String, String> dictionary) {
                    mProject = project;
                    mDescription = description;
                    mParameters = parameters;
                    mDictionary = dictionary;
        }

        public IProject getProject() {
            return mProject;
        }

        public IProjectDescription getDescription() {
            return mDescription;
        }

        public Map<String, Object> getParameters() {
            return mParameters;
        }

        public HashMap<String, String> getDictionary() {
            return mDictionary;
        }
    }

    /**
     * Creates the android project.
     * @return True if the project could be created.
     */
    private boolean createAndroidProjects() {

        final ProjectInfo mainData = collectMainPageInfo();
        if (mMainPage != null && mainData == null) {
            return false;
        }

        final ProjectInfo testData = collectTestPageInfo();

        // Create a monitored operation to create the actual project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
                createProjectAsync(monitor, mainData, testData);
            }
        };

        // Run the operation in a different thread
        runAsyncOperation(op);
        return true;
    }

    /**
     * Collects all the parameters needed to create the main project.
     * @return A new {@link ProjectInfo} on success. Returns null if the project cannot be
     *    created because parameters are incorrect or should not be created because there
     *    is no main page.
     */
    private ProjectInfo collectMainPageInfo() {
        if (mMainPage == null) {
            return null;
        }

        IMainInfo info = mMainPage.getMainInfo();

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject project = workspace.getRoot().getProject(info.getProjectName());
        final IProjectDescription description = workspace.newProjectDescription(project.getName());

        // keep some variables to make them available once the wizard closes
        mPackageName = info.getPackageName();

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_PROJECT, info.getProjectName());
        parameters.put(PARAM_PACKAGE, mPackageName);
        parameters.put(PARAM_APPLICATION, STRING_RSRC_PREFIX + STRING_APP_NAME);
        parameters.put(PARAM_SDK_TOOLS_DIR, AdtPlugin.getOsSdkToolsFolder());
        parameters.put(PARAM_IS_NEW_PROJECT, info.isNewProject());
        parameters.put(PARAM_SRC_FOLDER, info.getSourceFolder());
        parameters.put(PARAM_SDK_TARGET, info.getSdkTarget());
        parameters.put(PARAM_MIN_SDK_VERSION, info.getMinSdkVersion());

        if (info.isCreateActivity()) {
            // An activity name can be of the form ".package.Class" or ".Class".
            // The initial dot is ignored, as it is always added later in the templates.
            String activityName = info.getActivityName();
            if (activityName.startsWith(".")) { //$NON-NLS-1$
                activityName = activityName.substring(1);
            }
            parameters.put(PARAM_ACTIVITY, activityName);
        }

        // create a dictionary of string that will contain name+content.
        // we'll put all the strings into values/strings.xml
        final HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put(STRING_APP_NAME, info.getApplicationName());

        IPath path = info.getLocationPath();
        IPath defaultLocation = Platform.getLocation();
        if (!path.equals(defaultLocation)) {
            description.setLocation(path);
        }

        if (info.isNewProject() && !info.useDefaultLocation() &&
                !validateNewProjectLocationIsEmpty(path)) {
            return null;
        }

        return new ProjectInfo(project, description, parameters, dictionary);
    }

    /**
     * Collects all the parameters needed to create the test project.
     *
     * @return A new {@link ProjectInfo} on success. Returns null if the project cannot be
     *    created because parameters are incorrect or should not be created because there
     *    is no test page.
     */
    private ProjectInfo collectTestPageInfo() {
        if (mTestPage == null) {
            return null;
        }
        TestInfo info = mTestPage.getTestInfo();

        if (!info.getCreateTestProject()) {
            return null;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProject project = workspace.getRoot().getProject(info.getProjectName());
        final IProjectDescription description = workspace.newProjectDescription(project.getName());

        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(PARAM_PROJECT, info.getProjectName());
        parameters.put(PARAM_PACKAGE, info.getPackageName());
        parameters.put(PARAM_APPLICATION, STRING_RSRC_PREFIX + STRING_APP_NAME);
        parameters.put(PARAM_SDK_TOOLS_DIR, AdtPlugin.getOsSdkToolsFolder());
        parameters.put(PARAM_IS_NEW_PROJECT, true);
        parameters.put(PARAM_SRC_FOLDER, info.getSourceFolder());
        parameters.put(PARAM_SDK_TARGET, info.getSdkTarget());
        parameters.put(PARAM_MIN_SDK_VERSION, info.getMinSdkVersion());

        // Test-specific parameters
        parameters.put(PARAM_TEST_TARGET_PACKAGE, info.getTargetPackageName());

        if (info.isTestingSelf()) {
            parameters.put(PARAM_TARGET_SELF, true);
        }
        if (info.isTestingMain()) {
            parameters.put(PARAM_TARGET_MAIN, true);
        }
        if (info.isTestingExisting()) {
            parameters.put(PARAM_TARGET_EXISTING, true);
            parameters.put(PARAM_REFERENCE_PROJECT, info.getExistingTestedProject());
        }

        // create a dictionary of string that will contain name+content.
        // we'll put all the strings into values/strings.xml
        final HashMap<String, String> dictionary = new HashMap<String, String>();
        dictionary.put(STRING_APP_NAME, info.getApplicationName());

        IPath path = info.getLocationPath();
        IPath defaultLocation = Platform.getLocation();
        if (!path.equals(defaultLocation)) {
            description.setLocation(path);
        }

        if (!info.useDefaultLocation() && !validateNewProjectLocationIsEmpty(path)) {
            return null;
        }

        return new ProjectInfo(project, description, parameters, dictionary);
    }

    /**
     * Runs the operation in a different thread and display generated
     * exceptions.
     *
     * @param op The asynchronous operation to run.
     */
    private void runAsyncOperation(WorkspaceModifyOperation op) {
        try {
            getContainer().run(true /* fork */, true /* cancelable */, op);
        } catch (InvocationTargetException e) {
            // The runnable threw an exception
            Throwable t = e.getTargetException();
            if (t instanceof CoreException) {
                CoreException core = (CoreException) t;
                if (core.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
                    // The error indicates the file system is not case sensitive
                    // and there's a resource with a similar name.
                    MessageDialog.openError(getShell(), "Error", "Error: Case Variant Exists");
                } else {
                    ErrorDialog.openError(getShell(), "Error", null, core.getStatus());
                }
            } else {
                // Some other kind of exception
                MessageDialog.openError(getShell(), "Error", t.getMessage());
            }
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the actual project(s). This is run asynchronously in a different thread.
     *
     * @param monitor An existing monitor.
     * @param mainData Data for main project. Can be null.
     * @throws InvocationTargetException to wrap any unmanaged exception and
     *         return it to the calling thread. The method can fail if it fails
     *         to create or modify the project or if it is canceled by the user.
     */
    private void createProjectAsync(IProgressMonitor monitor,
            ProjectInfo mainData,
            ProjectInfo testData)
                throws InvocationTargetException {
        monitor.beginTask("Create Android Project", 100);
        try {
            IProject mainProject = null;

            if (mainData != null) {
                mainProject = createEclipseProject(
                        new SubProgressMonitor(monitor, 50),
                        mainData.getProject(),
                        mainData.getDescription(),
                        mainData.getParameters(),
                        mainData.getDictionary());
            }

            if (testData != null) {

                Map<String, Object> parameters = testData.getParameters();
                if (parameters.containsKey(PARAM_TARGET_MAIN) && mainProject != null) {
                    parameters.put(PARAM_REFERENCE_PROJECT, mainProject);
                }

                createEclipseProject(
                        new SubProgressMonitor(monitor, 50),
                        testData.getProject(),
                        testData.getDescription(),
                        parameters,
                        testData.getDictionary());
            }

        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Creates the actual project, sets its nature and adds the required folders
     * and files to it. This is run asynchronously in a different thread.
     *
     * @param monitor An existing monitor.
     * @param project The project to create.
     * @param description A description of the project.
     * @param parameters Template parameters.
     * @param dictionary String definition.
     * @return The project newly created
     */
    private IProject createEclipseProject(IProgressMonitor monitor,
            IProject project,
            IProjectDescription description,
            Map<String, Object> parameters,
            Map<String, String> dictionary)
                throws CoreException, IOException {

        // Create project and open it
        project.create(description, new SubProgressMonitor(monitor, 10));
        if (monitor.isCanceled()) throw new OperationCanceledException();

        project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 10));

        // Add the Java and android nature to the project
        AndroidNature.setupProjectNatures(project, monitor);

        // Create folders in the project if they don't already exist
        addDefaultDirectories(project, AndroidConstants.WS_ROOT, DEFAULT_DIRECTORIES, monitor);
        String[] sourceFolders = new String[] {
                    (String) parameters.get(PARAM_SRC_FOLDER),
                    GEN_SRC_DIRECTORY
                };
        addDefaultDirectories(project, AndroidConstants.WS_ROOT, sourceFolders, monitor);

        // Create the resource folders in the project if they don't already exist.
        addDefaultDirectories(project, RES_DIRECTORY, RES_DIRECTORIES, monitor);

        // Setup class path: mark folders as source folders
        IJavaProject javaProject = JavaCore.create(project);
        for (String sourceFolder : sourceFolders) {
            setupSourceFolder(javaProject, sourceFolder, monitor);
        }

        // Mark the gen source folder as derived
        IFolder genSrcFolder = project.getFolder(AndroidConstants.WS_ROOT + GEN_SRC_DIRECTORY);
        if (genSrcFolder.exists()) {
            genSrcFolder.setDerived(true);
        }

        if (((Boolean) parameters.get(PARAM_IS_NEW_PROJECT)).booleanValue()) {
            // Create files in the project if they don't already exist
            addManifest(project, parameters, dictionary, monitor);

            // add the default app icon
            addIcon(project, monitor);

            // Create the default package components
            addSampleCode(project, sourceFolders[0], parameters, dictionary, monitor);

            // add the string definition file if needed
            if (dictionary.size() > 0) {
                addStringDictionaryFile(project, dictionary, monitor);
            }

            // Set output location
            javaProject.setOutputLocation(project.getFolder(BIN_DIRECTORY).getFullPath(),
                    monitor);
        }

        // Create the reference to the target project
        if (parameters.containsKey(PARAM_REFERENCE_PROJECT)) {
            IProject refProject = (IProject) parameters.get(PARAM_REFERENCE_PROJECT);
            if (refProject != null) {
                IProjectDescription desc = project.getDescription();

                // Add out reference to the existing project reference.
                // We just created a project with no references so we don't need to expand
                // the currently-empty current list.
                desc.setReferencedProjects(new IProject[] { refProject });

                project.setDescription(desc, IResource.KEEP_HISTORY, new SubProgressMonitor(monitor, 10));

                IClasspathEntry entry = JavaCore.newProjectEntry(
                        refProject.getFullPath(), //path
                        new IAccessRule[0], //accessRules
                        false, //combineAccessRules
                        new IClasspathAttribute[0], //extraAttributes
                        false //isExported

                );
                ProjectHelper.addEntryToClasspath(javaProject, entry);
            }
        }

        Sdk.getCurrent().setProject(project, (IAndroidTarget) parameters.get(PARAM_SDK_TARGET),
                null /* apkConfigMap*/);

        // Fix the project to make sure all properties are as expected.
        // Necessary for existing projects and good for new ones to.
        ProjectHelper.fixProject(project);

        return project;
    }

    /**
     * Adds default directories to the project.
     *
     * @param project The Java Project to update.
     * @param parentFolder The path of the parent folder. Must end with a
     *        separator.
     * @param folders Folders to be added.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to create the directories in
     *         the project.
     */
    private void addDefaultDirectories(IProject project, String parentFolder,
            String[] folders, IProgressMonitor monitor) throws CoreException {
        for (String name : folders) {
            if (name.length() > 0) {
                IFolder folder = project.getFolder(parentFolder + name);
                if (!folder.exists()) {
                    folder.create(true /* force */, true /* local */,
                            new SubProgressMonitor(monitor, 10));
                }
            }
        }
    }

    /**
     * Adds the manifest to the project.
     *
     * @param project The Java Project to update.
     * @param parameters Template Parameters.
     * @param dictionary String List to be added to a string definition
     *        file. This map will be filled by this method.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addManifest(IProject project, Map<String, Object> parameters,
            Map<String, String> dictionary, IProgressMonitor monitor)
            throws CoreException, IOException {

        // get IFile to the manifest and check if it's not already there.
        IFile file = project.getFile(AndroidConstants.FN_ANDROID_MANIFEST);
        if (!file.exists()) {

            // Read manifest template
            String manifestTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_MANIFEST);

            // Replace all keyword parameters
            manifestTemplate = replaceParameters(manifestTemplate, parameters);

            if (parameters.containsKey(PARAM_ACTIVITY)) {
                // now get the activity template
                String activityTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_ACTIVITIES);

                // Replace all keyword parameters to make main activity.
                String activities = replaceParameters(activityTemplate, parameters);

                // set the intent.
                String intent = AdtPlugin.readEmbeddedTextFile(TEMPLATE_INTENT_LAUNCHER);

                // set the intent to the main activity
                activities = activities.replaceAll(PH_INTENT_FILTERS, intent);

                // set the activity(ies) in the manifest
                manifestTemplate = manifestTemplate.replaceAll(PH_ACTIVITIES, activities);
            } else {
                // remove the activity(ies) from the manifest
                manifestTemplate = manifestTemplate.replaceAll(PH_ACTIVITIES, "");  //$NON-NLS-1$
            }

            // Handle the case of the test projects
            if (parameters.containsKey(PARAM_TEST_TARGET_PACKAGE)) {
                // Set the uses-library needed by the test project
                String usesLibrary = AdtPlugin.readEmbeddedTextFile(TEMPLATE_TEST_USES_LIBRARY);
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_USES_LIBRARY, usesLibrary);

                // Set the instrumentation element needed by the test project
                String instru = AdtPlugin.readEmbeddedTextFile(TEMPLATE_TEST_INSTRUMENTATION);
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_INSTRUMENTATION, instru);

                // Replace PARAM_TEST_TARGET_PACKAGE itself now
                manifestTemplate = replaceParameters(manifestTemplate, parameters);

            } else {
                // remove the unused entries
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_USES_LIBRARY, "");     //$NON-NLS-1$
                manifestTemplate = manifestTemplate.replaceAll(PH_TEST_INSTRUMENTATION, "");  //$NON-NLS-1$
            }

            String minSdkVersion = (String) parameters.get(PARAM_MIN_SDK_VERSION);
            if (minSdkVersion != null && minSdkVersion.length() > 0) {
                String usesSdkTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_USES_SDK);
                String usesSdk = replaceParameters(usesSdkTemplate, parameters);
                manifestTemplate = manifestTemplate.replaceAll(PH_USES_SDK, usesSdk);
            } else {
                manifestTemplate = manifestTemplate.replaceAll(PH_USES_SDK, "");
            }

            // Save in the project as UTF-8
            InputStream stream = new ByteArrayInputStream(
                    manifestTemplate.getBytes("UTF-8")); //$NON-NLS-1$
            file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }

    /**
     * Adds the string resource file.
     *
     * @param project The Java Project to update.
     * @param strings The list of strings to be added to the string file.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addStringDictionaryFile(IProject project,
            Map<String, String> strings, IProgressMonitor monitor)
            throws CoreException, IOException {

        // create the IFile object and check if the file doesn't already exist.
        IFile file = project.getFile(RES_DIRECTORY + AndroidConstants.WS_SEP
                                     + VALUES_DIRECTORY + AndroidConstants.WS_SEP + STRINGS_FILE);
        if (!file.exists()) {
            // get the Strings.xml template
            String stringDefinitionTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_STRINGS);

            // get the template for one string
            String stringTemplate = AdtPlugin.readEmbeddedTextFile(TEMPLATE_STRING);

            // get all the string names
            Set<String> stringNames = strings.keySet();

            // loop on it and create the string definitions
            StringBuilder stringNodes = new StringBuilder();
            for (String key : stringNames) {
                // get the value from the key
                String value = strings.get(key);

                // place them in the template
                String stringDef = stringTemplate.replace(PARAM_STRING_NAME, key);
                stringDef = stringDef.replace(PARAM_STRING_CONTENT, value);

                // append to the other string
                if (stringNodes.length() > 0) {
                    stringNodes.append("\n");
                }
                stringNodes.append(stringDef);
            }

            // put the string nodes in the Strings.xml template
            stringDefinitionTemplate = stringDefinitionTemplate.replace(PH_STRINGS,
                                                                        stringNodes.toString());

            // write the file as UTF-8
            InputStream stream = new ByteArrayInputStream(
                    stringDefinitionTemplate.getBytes("UTF-8")); //$NON-NLS-1$
            file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
        }
    }


    /**
     * Adds default application icon to the project.
     *
     * @param project The Java Project to update.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     */
    private void addIcon(IProject project, IProgressMonitor monitor)
            throws CoreException {
        IFile file = project.getFile(RES_DIRECTORY + AndroidConstants.WS_SEP
                                     + DRAWABLE_DIRECTORY + AndroidConstants.WS_SEP + ICON);
        if (!file.exists()) {
            // read the content from the template
            byte[] buffer = AdtPlugin.readEmbeddedFile(TEMPLATES_DIRECTORY + ICON);

            // if valid
            if (buffer != null) {
                // Save in the project
                InputStream stream = new ByteArrayInputStream(buffer);
                file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
            }
        }
    }

    /**
     * Creates the package folder and copies the sample code in the project.
     *
     * @param project The Java Project to update.
     * @param parameters Template Parameters.
     * @param dictionary String List to be added to a string definition
     *        file. This map will be filled by this method.
     * @param monitor An existing monitor.
     * @throws CoreException if the method fails to update the project.
     * @throws IOException if the method fails to create the files in the
     *         project.
     */
    private void addSampleCode(IProject project, String sourceFolder,
            Map<String, Object> parameters, Map<String, String> dictionary,
            IProgressMonitor monitor) throws CoreException, IOException {
        // create the java package directories.
        IFolder pkgFolder = project.getFolder(sourceFolder);
        String packageName = (String) parameters.get(PARAM_PACKAGE);

        // The PARAM_ACTIVITY key will be absent if no activity should be created,
        // in which case activityName will be null.
        String activityName = (String) parameters.get(PARAM_ACTIVITY);
        Map<String, Object> java_activity_parameters = parameters;
        if (activityName != null) {
            if (activityName.indexOf('.') >= 0) {
                // There are package names in the activity name. Transform packageName to add
                // those sub packages and remove them from activityName.
                packageName += "." + activityName; //$NON-NLS-1$
                int pos = packageName.lastIndexOf('.');
                activityName = packageName.substring(pos + 1);
                packageName = packageName.substring(0, pos);

                // Also update the values used in the JAVA_FILE_TEMPLATE below
                // (but not the ones from the manifest so don't change the caller's dictionary)
                java_activity_parameters = new HashMap<String, Object>(parameters);
                java_activity_parameters.put(PARAM_PACKAGE, packageName);
                java_activity_parameters.put(PARAM_ACTIVITY, activityName);
            }
        }

        String[] components = packageName.split(AndroidConstants.RE_DOT);
        for (String component : components) {
            pkgFolder = pkgFolder.getFolder(component);
            if (!pkgFolder.exists()) {
                pkgFolder.create(true /* force */, true /* local */,
                        new SubProgressMonitor(monitor, 10));
            }
        }

        if (activityName != null) {
            // create the main activity Java file
            String activityJava = activityName + AndroidConstants.DOT_JAVA;
            IFile file = pkgFolder.getFile(activityJava);
            if (!file.exists()) {
                copyFile(JAVA_ACTIVITY_TEMPLATE, file, java_activity_parameters, monitor);
            }
        }

        // create the layout file
        IFolder layoutfolder = project.getFolder(RES_DIRECTORY).getFolder(LAYOUT_DIRECTORY);
        IFile file = layoutfolder.getFile(MAIN_LAYOUT_XML);
        if (!file.exists()) {
            copyFile(LAYOUT_TEMPLATE, file, parameters, monitor);
            if (activityName != null) {
                dictionary.put(STRING_HELLO_WORLD, "Hello World, " + activityName + "!");
            } else {
                dictionary.put(STRING_HELLO_WORLD, "Hello World!");
            }
        }
    }

    /**
     * Adds the given folder to the project's class path.
     *
     * @param javaProject The Java Project to update.
     * @param sourceFolder Template Parameters.
     * @param monitor An existing monitor.
     * @throws JavaModelException if the classpath could not be set.
     */
    private void setupSourceFolder(IJavaProject javaProject, String sourceFolder,
            IProgressMonitor monitor) throws JavaModelException {
        IProject project = javaProject.getProject();

        // Add "src" to class path
        IFolder srcFolder = project.getFolder(sourceFolder);

        IClasspathEntry[] entries = javaProject.getRawClasspath();
        entries = removeSourceClasspath(entries, srcFolder);
        entries = removeSourceClasspath(entries, srcFolder.getParent());

        entries = ProjectHelper.addEntryToClasspath(entries,
                JavaCore.newSourceEntry(srcFolder.getFullPath()));

        javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
    }


    /**
     * Removes the corresponding source folder from the class path entries if
     * found.
     *
     * @param entries The class path entries to read. A copy will be returned.
     * @param folder The parent source folder to remove.
     * @return A new class path entries array.
     */
    private IClasspathEntry[] removeSourceClasspath(IClasspathEntry[] entries, IContainer folder) {
        if (folder == null) {
            return entries;
        }
        IClasspathEntry source = JavaCore.newSourceEntry(folder.getFullPath());
        int n = entries.length;
        for (int i = n - 1; i >= 0; i--) {
            if (entries[i].equals(source)) {
                IClasspathEntry[] newEntries = new IClasspathEntry[n - 1];
                if (i > 0) System.arraycopy(entries, 0, newEntries, 0, i);
                if (i < n - 1) System.arraycopy(entries, i + 1, newEntries, i, n - i - 1);
                n--;
                entries = newEntries;
            }
        }
        return entries;
    }


    /**
     * Copies the given file from our resource folder to the new project.
     * Expects the file to the US-ASCII or UTF-8 encoded.
     *
     * @throws CoreException from IFile if failing to create the new file.
     * @throws MalformedURLException from URL if failing to interpret the URL.
     * @throws FileNotFoundException from RandomAccessFile.
     * @throws IOException from RandomAccessFile.length() if can't determine the
     *         length.
     */
    private void copyFile(String resourceFilename, IFile destFile,
            Map<String, Object> parameters, IProgressMonitor monitor)
            throws CoreException, IOException {

        // Read existing file.
        String template = AdtPlugin.readEmbeddedTextFile(
                TEMPLATES_DIRECTORY + resourceFilename);

        // Replace all keyword parameters
        template = replaceParameters(template, parameters);

        // Save in the project as UTF-8
        InputStream stream = new ByteArrayInputStream(template.getBytes("UTF-8")); //$NON-NLS-1$
        destFile.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }

    /**
     * Returns an image descriptor for the wizard logo.
     */
    private void setImageDescriptor() {
        ImageDescriptor desc = AdtPlugin.getImageDescriptor(PROJECT_LOGO_LARGE);
        setDefaultPageImageDescriptor(desc);
    }

    /**
     * Replaces placeholders found in a string with values.
     *
     * @param str the string to search for placeholders.
     * @param parameters a map of <placeholder, Value> to search for in the string
     * @return A new String object with the placeholder replaced by the values.
     */
    private String replaceParameters(String str, Map<String, Object> parameters) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() instanceof String) {
                str = str.replaceAll(entry.getKey(), (String) entry.getValue());
            }
        }

        return str;
    }
}
