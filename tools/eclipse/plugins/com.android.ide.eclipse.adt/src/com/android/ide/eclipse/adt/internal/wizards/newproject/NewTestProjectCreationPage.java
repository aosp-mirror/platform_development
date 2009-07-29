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

/*
 * References:
 * org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard
 * org.eclipse.jdt.internal.ui.wizards.JavaProjectWizardFirstPage
 */

package com.android.ide.eclipse.adt.internal.wizards.newproject;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.ProjectChooserHelper;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreationPage.IMainInfo;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewProjectCreationPage.MainInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * NewAndroidProjectCreationPage is a project creation page that provides the
 * following fields:
 * <ul>
 * <li> Project name
 * <li> SDK Target
 * <li> Application name
 * <li> Package name
 * <li> Activity name
 * </ul>
 * Note: this class is public so that it can be accessed from unit tests.
 * It is however an internal class. Its API may change without notice.
 * It should semantically be considered as a private final class.
 * Do not derive from this class.
 */
public class NewTestProjectCreationPage extends WizardPage {

    // constants
    static final String TEST_PAGE_NAME = "newAndroidTestProjectPage"; //$NON-NLS-1$

    /** Initial value for all name fields (project, activity, application, package). Used
     * whenever a value is requested before controls are created. */
    private static final String INITIAL_NAME = "";  //$NON-NLS-1$
    /** Initial value for the Use Default Location check box. */
    private static final boolean INITIAL_USE_DEFAULT_LOCATION = true;
    /** Initial value for the Create Test Project check box. */
    private static final boolean INITIAL_CREATE_TEST_PROJECT = false;


    /** Pattern for characters accepted in a project name. Since this will be used as a
     * directory name, we're being a bit conservative on purpose. It cannot start with a space. */
    private static final Pattern sProjectNamePattern = Pattern.compile("^[\\w][\\w. -]*$");  //$NON-NLS-1$
    /** Last user-browsed location, static so that it be remembered for the whole session */
    private static String sCustomLocationOsPath = "";  //$NON-NLS-1$

    private final int MSG_NONE = 0;
    private final int MSG_WARNING = 1;
    private final int MSG_ERROR = 2;

    /** Structure with the externally visible information from this Test Project page. */
    private final TestInfo mInfo = new TestInfo();
    /** Structure with the externally visible information from the Main Project page.
     *  This is null if there's no such page, meaning the test project page is standalone. */
    private IMainInfo mMainInfo;

    // widgets
    private Text mProjectNameField;
    private Text mPackageNameField;
    private Text mApplicationNameField;
    private Button mUseDefaultLocation;
    private Label mLocationLabel;
    private Text mLocationPathField;
    private Button mBrowseButton;
    private Text mMinSdkVersionField;
    private SdkTargetSelector mSdkTargetSelector;
    private ITargetChangeListener mSdkTargetChangeListener;
    private Button mCreateTestProjectField;
    private Text mTestedProjectNameField;
    private Button mProjectBrowseButton;
    private ProjectChooserHelper mProjectChooserHelper;
    private Button mTestSelfProjectRadio;
    private Button mTestExistingProjectRadio;

    /** A list of composites that are disabled when the "Create Test Project" toggle is off. */
    private ArrayList<Composite> mToggleComposites = new ArrayList<Composite>();

    private boolean mInternalProjectNameUpdate;
    private boolean mInternalLocationPathUpdate;
    private boolean mInternalPackageNameUpdate;
    private boolean mInternalApplicationNameUpdate;
    private boolean mInternalMinSdkVersionUpdate;
    private boolean mInternalSdkTargetUpdate;
    private IProject mExistingTestedProject;
    private boolean mProjectNameModifiedByUser;
    private boolean mApplicationNameModifiedByUser;
    private boolean mPackageNameModifiedByUser;
    private boolean mMinSdkVersionModifiedByUser;
    private boolean mSdkTargetModifiedByUser;

    private Label mTestTargetPackageLabel;

    private String mLastExistingPackageName;


    /**
     * Creates a new project creation wizard page.
     */
    public NewTestProjectCreationPage() {
        super(TEST_PAGE_NAME);
        setPageComplete(false);
        setTitle("New Android Test Project");
        setDescription("Creates a new Android Test Project resource.");
    }

    // --- Getters used by NewProjectWizard ---

    /**
     * Structure that collects all externally visible information from this page.
     * This is used by the calling wizard to actually do the work or by other pages.
     */
    public class TestInfo {

        /** Returns true if a new Test Project should be created. */
        public boolean getCreateTestProject() {
            return mCreateTestProjectField == null ? true : mCreateTestProjectField.getSelection();
        }

        /**
         * Returns the current project location path as entered by the user, or its
         * anticipated initial value. Note that if the default has been returned the
         * path in a project description used to create a project should not be set.
         *
         * @return the project location path or its anticipated initial value.
         */
        public IPath getLocationPath() {
            return new Path(getProjectLocation());
        }

        /** Returns the value of the project name field with leading and trailing spaces removed. */
        public String getProjectName() {
            return mProjectNameField == null ? INITIAL_NAME : mProjectNameField.getText().trim();
        }

        /** Returns the value of the package name field with spaces trimmed. */
        public String getPackageName() {
            return mPackageNameField == null ? INITIAL_NAME : mPackageNameField.getText().trim();
        }

        /** Returns the value of the test target package name field with spaces trimmed. */
        public String getTargetPackageName() {
            return mTestTargetPackageLabel == null ? INITIAL_NAME
                                                   : mTestTargetPackageLabel.getText().trim();
        }

        /** Returns the value of the min sdk version field with spaces trimmed. */
        public String getMinSdkVersion() {
            return mMinSdkVersionField == null ? "" : mMinSdkVersionField.getText().trim();  //$NON-NLS-1$
        }

        /** Returns the value of the application name field with spaces trimmed. */
        public String getApplicationName() {
            // Return the name of the activity as default application name.
            return mApplicationNameField == null ? "" : mApplicationNameField.getText().trim();  //$NON-NLS-1$
        }

        /** Returns the value of the Use Default Location field. */
        public boolean useDefaultLocation() {
            return mUseDefaultLocation == null ? INITIAL_USE_DEFAULT_LOCATION
                                               : mUseDefaultLocation.getSelection();
        }

        /** Returns the the default "src" constant. */
        public String getSourceFolder() {
            return SdkConstants.FD_SOURCES;
        }

        /** Returns the current sdk target or null if none has been selected yet. */
        public IAndroidTarget getSdkTarget() {
            return mSdkTargetSelector == null ? null : mSdkTargetSelector.getSelected();
        }

        public boolean isTestingSelf() {
            return mMainInfo == null &&
                (mTestSelfProjectRadio == null ? false : mTestSelfProjectRadio.getSelection());
        }

        public boolean isTestingMain() {
            return mMainInfo != null;
        }

        public boolean isTestingExisting() {
            return mMainInfo == null &&
                (mTestExistingProjectRadio == null ? false
                                                   : mTestExistingProjectRadio.getSelection());
        }

        public IProject getExistingTestedProject() {
            return mExistingTestedProject;
        }
    }

    /**
     * Returns a {@link TestInfo} structure that collects all externally visible information
     * from this page. This is used by the calling wizard to actually do the work or by other pages.
     */
    public TestInfo getTestInfo() {
        return mInfo;
    }

    /**
     * Grabs the {@link MainInfo} structure with visible parameters from the main project page.
     * This may be null.
     */
    public void setMainInfo(IMainInfo mainInfo) {
        mMainInfo = mainInfo;
    }

    // --- UI creation ---

    /**
     * Creates the top level control for this dialog page under the given parent
     * composite.
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createToggleTestProject(composite);
        createTestProjectGroup(composite);
        createLocationGroup(composite);
        createTestTargetGroup(composite);
        createTargetGroup(composite);
        createPropertiesGroup(composite);

        // Update state the first time
        enableLocationWidgets();

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);

        // Validate. This will complain about the first empty field.
        validatePageComplete();
    }

    /**
     * Overrides @DialogPage.setVisible(boolean) to put the focus in the project name when
     * the dialog is made visible and to also update the enabled/disabled state of some
     * controls (doing so in createControl doesn't always change their state somehow.)
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            mProjectNameField.setFocus();
            validatePageComplete();
            onCreateTestProjectToggle();
            onExistingProjectChanged();
        }
    }

    @Override
    public void dispose() {

        if (mSdkTargetChangeListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mSdkTargetChangeListener);
            mSdkTargetChangeListener = null;
        }

        super.dispose();
    }

    /**
     * Creates the "create test project" checkbox but only if there's a main page in the wizard.
     *
     * @param parent the parent composite
     */
    private final void createToggleTestProject(Composite parent) {

        if (mMainInfo != null) {
            mCreateTestProjectField = new Button(parent, SWT.CHECK);
            mCreateTestProjectField.setText("Create a Test Project");
            mCreateTestProjectField.setToolTipText("Select this if you also want to create a Test Project.");
            mCreateTestProjectField.setSelection(INITIAL_CREATE_TEST_PROJECT);
            mCreateTestProjectField.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onCreateTestProjectToggle();
                }
            });
        }
    }

    /**
     * Creates the group for the project name:
     * [label: "Project Name"] [text field]
     *
     * @param parent the parent composite
     */
    private final void createTestProjectGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mToggleComposites.add(group);

        // --- test project name ---

        // new project label
        String tooltip = "Name of the Eclipse test project to create. It cannot be empty.";
        Label label = new Label(group, SWT.NONE);
        label.setText("Test Project Name:");
        label.setFont(parent.getFont());
        label.setToolTipText(tooltip);

        // new project name entry field
        mProjectNameField = new Text(group, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        mProjectNameField.setToolTipText(tooltip);
        mProjectNameField.setLayoutData(data);
        mProjectNameField.setFont(parent.getFont());
        mProjectNameField.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                if (!mInternalProjectNameUpdate) {
                    mProjectNameModifiedByUser = true;
                }
                updateLocationPathField(null);
            }
        });

    }

    private final void createLocationGroup(Composite parent) {

        // --- project location ---

        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        group.setLayout(new GridLayout(3, /* num columns */
                false /* columns of not equal size */));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(parent.getFont());
        group.setText("Content");

        mToggleComposites.add(group);

        mUseDefaultLocation = new Button(group, SWT.CHECK);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        mUseDefaultLocation.setLayoutData(gd);
        mUseDefaultLocation.setText("Use default location");
        mUseDefaultLocation.setSelection(INITIAL_USE_DEFAULT_LOCATION);

        mUseDefaultLocation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                enableLocationWidgets();
                validatePageComplete();
            }
        });


        mLocationLabel = new Label(group, SWT.NONE);
        mLocationLabel.setText("Location:");

        mLocationPathField = new Text(group, SWT.BORDER);
        GridData data = new GridData(GridData.FILL, /* horizontal alignment */
                GridData.BEGINNING, /* vertical alignment */
                true,  /* grabExcessHorizontalSpace */
                false, /* grabExcessVerticalSpace */
                1,     /* horizontalSpan */
                1);    /* verticalSpan */
        mLocationPathField.setLayoutData(data);
        mLocationPathField.setFont(parent.getFont());
        mLocationPathField.addListener(SWT.Modify, new Listener() {
           public void handleEvent(Event event) {
               onLocationPathFieldModified();
            }
        });

        mBrowseButton = new Button(group, SWT.PUSH);
        mBrowseButton.setText("Browse...");
        setButtonLayoutData(mBrowseButton);
        mBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onOpenDirectoryBrowser();
            }
        });
    }

    /**
     * Creates the group for Test Target options.
     *
     * There are two different modes here:
     * <ul>
     * <li>When mMainInfo exists, this is part of a new Android Project. In which case
     * the new test is tied to the soon-to-be main project and there is actually no choice.
     * <li>When mMainInfo does not exist, this is a standalone new test project. In this case
     * we offer 2 options for the test target: self test or against an existing Android project.
     * </ul>
     *
     * @param parent the parent composite
     */
    private final void createTestTargetGroup(Composite parent) {

        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());
        group.setText("Test Target");

        mToggleComposites.add(group);

        if (mMainInfo == null) {
            // Standalone mode: choose between self-test and existing-project test

            Label label = new Label(group, SWT.NONE);
            label.setText("Select the project to test:");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            label.setLayoutData(gd);

            mTestSelfProjectRadio = new Button(group, SWT.RADIO);
            mTestSelfProjectRadio.setText("This project");
            mTestSelfProjectRadio.setSelection(false);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            mTestSelfProjectRadio.setLayoutData(gd);

            mTestExistingProjectRadio = new Button(group, SWT.RADIO);
            mTestExistingProjectRadio.setText("An existing Android project");
            mTestExistingProjectRadio.setSelection(mMainInfo == null);
            mTestExistingProjectRadio.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onExistingProjectChanged();
                }
            });

            String tooltip = "The existing Android Project that is being tested.";

            mTestedProjectNameField = new Text(group, SWT.BORDER);
            mTestedProjectNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mTestedProjectNameField.setToolTipText(tooltip);
            mTestedProjectNameField.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onProjectFieldUpdated();
                }
            });

            mProjectBrowseButton = new Button(group, SWT.NONE);
            mProjectBrowseButton.setText("Browse...");
            mProjectBrowseButton.setToolTipText("Allows you to select the Android project to test.");
            mProjectBrowseButton.addSelectionListener(new SelectionAdapter() {
               @Override
                public void widgetSelected(SelectionEvent e) {
                   onProjectBrowse();
                }
            });

            mProjectChooserHelper = new ProjectChooserHelper(parent.getShell());
        } else {
            // Part of NPW mode: no selection.

        }

        // package label line

        Label label = new Label(group, SWT.NONE);
        label.setText("Test Target Package:");
        mTestTargetPackageLabel = new Label(group, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        mTestTargetPackageLabel.setLayoutData(gd);
    }

    /**
     * Creates the target group.
     * It only contains an SdkTargetSelector.
     */
    private void createTargetGroup(Composite parent) {
        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        // Layout has 1 column
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());
        group.setText("Build Target");

        mToggleComposites.add(group);

        // The selector is created without targets. They are added below in the change listener.
        mSdkTargetSelector = new SdkTargetSelector(group, null);

        mSdkTargetChangeListener = new ITargetChangeListener() {
            public void onProjectTargetChange(IProject changedProject) {
                // Ignore
            }

            public void onTargetsLoaded() {
                // Update the sdk target selector with the new targets

                // get the targets from the sdk
                IAndroidTarget[] targets = null;
                if (Sdk.getCurrent() != null) {
                    targets = Sdk.getCurrent().getTargets();
                }
                mSdkTargetSelector.setTargets(targets);

                // If there's only one target, select it
                if (targets != null && targets.length == 1) {
                    mSdkTargetSelector.setSelection(targets[0]);
                }
            }
        };

        AdtPlugin.getDefault().addTargetListener(mSdkTargetChangeListener);

        // Invoke it once to initialize the targets
        mSdkTargetChangeListener.onTargetsLoaded();

        mSdkTargetSelector.setSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onSdkTargetModified();
                updateLocationPathField(null);
                validatePageComplete();
            }
        });
    }

    /**
     * Creates the group for the project properties:
     * - Package name [text field]
     * - Activity name [text field]
     * - Application name [text field]
     *
     * @param parent the parent composite
     */
    private final void createPropertiesGroup(Composite parent) {
        // package specification group
        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(parent.getFont());
        group.setText("Properties");

        mToggleComposites.add(group);

        // new application label
        Label label = new Label(group, SWT.NONE);
        label.setText("Application name:");
        label.setFont(parent.getFont());
        label.setToolTipText("Name of the Application. This is a free string. It can be empty.");

        // new application name entry field
        mApplicationNameField = new Text(group, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        mApplicationNameField.setToolTipText("Name of the Application. This is a free string. It can be empty.");
        mApplicationNameField.setLayoutData(data);
        mApplicationNameField.setFont(parent.getFont());
        mApplicationNameField.addListener(SWT.Modify, new Listener() {
           public void handleEvent(Event event) {
               if (!mInternalApplicationNameUpdate) {
                   mApplicationNameModifiedByUser = true;
               }
           }
        });

        // new package label
        label = new Label(group, SWT.NONE);
        label.setText("Package name:");
        label.setFont(parent.getFont());
        label.setToolTipText("Namespace of the Package to create. This must be a Java namespace with at least two components.");

        // new package name entry field
        mPackageNameField = new Text(group, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        mPackageNameField.setToolTipText("Namespace of the Package to create. This must be a Java namespace with at least two components.");
        mPackageNameField.setLayoutData(data);
        mPackageNameField.setFont(parent.getFont());
        mPackageNameField.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                if (!mInternalPackageNameUpdate) {
                    mPackageNameModifiedByUser = true;
                }
                onPackageNameFieldModified();
            }
        });

        // min sdk version label
        label = new Label(group, SWT.NONE);
        label.setText("Min SDK Version:");
        label.setFont(parent.getFont());
        label.setToolTipText("The minimum SDK version number that the application requires. Must be an integer > 0. It can be empty.");

        // min sdk version entry field
        mMinSdkVersionField = new Text(group, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        label.setToolTipText("The minimum SDK version number that the application requires. Must be an integer > 0. It can be empty.");
        mMinSdkVersionField.setLayoutData(data);
        mMinSdkVersionField.setFont(parent.getFont());
        mMinSdkVersionField.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                onMinSdkVersionFieldModified();
                validatePageComplete();
            }
        });
    }


    //--- Internal getters & setters ------------------

    /** Returns the location path field value with spaces trimmed. */
    private String getLocationPathFieldValue() {
        return mLocationPathField == null ? "" : mLocationPathField.getText().trim();  //$NON-NLS-1$
    }

    /** Returns the current project location, depending on the Use Default Location check box. */
    private String getProjectLocation() {
        if (mInfo.useDefaultLocation()) {
            return Platform.getLocation().toString();
        } else {
            return getLocationPathFieldValue();
        }
    }

    /**
     * Creates a project resource handle for the current project name field
     * value.
     * <p>
     * This method does not create the project resource; this is the
     * responsibility of <code>IProject::create</code> invoked by the new
     * project resource wizard.
     * </p>
     *
     * @return the new project resource handle
     */
    private IProject getProjectHandle() {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(mInfo.getProjectName());
    }

    // --- UI Callbacks ----

    /**
     * Callback invoked when the user toggles the "Test target: Existing Android Project"
     * checkbox. It enables or disable the UI to select an existing project.
     */
    private void onExistingProjectChanged() {
        if (mInfo.isTestingExisting()) {
            boolean enabled = mTestExistingProjectRadio.getSelection();
            mTestedProjectNameField.setEnabled(enabled);
            mProjectBrowseButton.setEnabled(enabled);
            setExistingProject(mInfo.getExistingTestedProject());
            validatePageComplete();
        }
    }

    /**
     * Tries to load the defaults from the main page if possible.
     */
    private void useMainProjectInformation() {
        if (mInfo.isTestingMain() && mMainInfo != null) {

            String projName = String.format("%1$sTest", mMainInfo.getProjectName());
            String appName = String.format("%1$sTest", mMainInfo.getApplicationName());

            String packageName = mMainInfo.getPackageName();
            if (packageName == null) {
                packageName = "";  //$NON-NLS-1$
            }

            updateTestTargetPackageField(packageName);

            if (!mProjectNameModifiedByUser) {
                mInternalProjectNameUpdate = true;
                mProjectNameField.setText(projName);  //$NON-NLS-1$
                mInternalProjectNameUpdate = false;
            }

            if (!mApplicationNameModifiedByUser) {
                mInternalApplicationNameUpdate = true;
                mApplicationNameField.setText(appName);
                mInternalApplicationNameUpdate = false;
            }

            if (!mPackageNameModifiedByUser) {
                mInternalPackageNameUpdate = true;
                packageName += ".test";  //$NON-NLS-1$
                mPackageNameField.setText(packageName);
                mInternalPackageNameUpdate = false;
            }

            if (!mSdkTargetModifiedByUser) {
                mInternalSdkTargetUpdate = true;
                mSdkTargetSelector.setSelection(mMainInfo.getSdkTarget());
                mInternalSdkTargetUpdate = false;
            }

            if (!mMinSdkVersionModifiedByUser) {
                mInternalMinSdkVersionUpdate = true;
                mMinSdkVersionField.setText(mMainInfo.getMinSdkVersion());
                mInternalMinSdkVersionUpdate = false;
            }
        }
    }

    /**
     * Callback invoked when the user edits the project text field.
     */
    private void onProjectFieldUpdated() {
        String project = mTestedProjectNameField.getText();

        // Is this a valid project?
        IJavaProject[] projects = mProjectChooserHelper.getAndroidProjects(null /*javaModel*/);
        for (IJavaProject p : projects) {
            if (p.getProject().getName().equals(project)) {
                setExistingProject(p.getProject());
                return;
            }
        }
    }

    /**
     * Callback called when the user uses the "Browse Projects" button.
     */
    private void onProjectBrowse() {
        IJavaProject p = mProjectChooserHelper.chooseJavaProject(mTestedProjectNameField.getText());
        if (p != null) {
            setExistingProject(p.getProject());
            mTestedProjectNameField.setText(mExistingTestedProject.getName());
        }
    }

    private void setExistingProject(IProject project) {
        mExistingTestedProject = project;

        // Try to update the application, package, sdk target and minSdkVersion accordingly
        if (project != null &&
                (!mApplicationNameModifiedByUser ||
                 !mPackageNameModifiedByUser     ||
                 !mSdkTargetModifiedByUser       ||
                 !mMinSdkVersionModifiedByUser)) {

            IFile file = AndroidManifestParser.getManifest(project);
            AndroidManifestParser manifestData = null;
            if (file != null) {
                try {
                    manifestData = AndroidManifestParser.parseForData(file);
                } catch (CoreException e) {
                    // pass
                }
            }

            if (manifestData != null) {
                String appName = String.format("%1$sTest", project.getName());
                String packageName = manifestData.getPackage();
                String minSdkVersion = manifestData.getApiLevelRequirement();
                IAndroidTarget sdkTarget = null;
                if (Sdk.getCurrent() != null) {
                    sdkTarget = Sdk.getCurrent().getTarget(project);
                }

                if (packageName == null) {
                    packageName = "";  //$NON-NLS-1$
                }
                mLastExistingPackageName = packageName;

                if (!mProjectNameModifiedByUser) {
                    mInternalProjectNameUpdate = true;
                    mProjectNameField.setText(appName);
                    mInternalProjectNameUpdate = false;
                }

                if (!mApplicationNameModifiedByUser) {
                    mInternalApplicationNameUpdate = true;
                    mApplicationNameField.setText(appName);
                    mInternalApplicationNameUpdate = false;
                }

                if (!mPackageNameModifiedByUser) {
                    mInternalPackageNameUpdate = true;
                    packageName += ".test";  //$NON-NLS-1$
                    mPackageNameField.setText(packageName);  //$NON-NLS-1$
                    mInternalPackageNameUpdate = false;
                }

                if (!mSdkTargetModifiedByUser && sdkTarget != null) {
                    mInternalSdkTargetUpdate = true;
                    mSdkTargetSelector.setSelection(sdkTarget);
                    mInternalSdkTargetUpdate = false;
                }

                if (!mMinSdkVersionModifiedByUser) {
                    mInternalMinSdkVersionUpdate = true;
                    if (minSdkVersion != null) {
                        mMinSdkVersionField.setText(minSdkVersion);
                    }
                    if (sdkTarget == null) {
                        updateSdkSelectorToMatchMinSdkVersion();
                    }
                    mInternalMinSdkVersionUpdate = false;
                }
            }
        }

        updateTestTargetPackageField(mLastExistingPackageName);
        validatePageComplete();
    }

    /**
     * Display a directory browser and update the location path field with the selected path
     */
    private void onOpenDirectoryBrowser() {

        String existing_dir = getLocationPathFieldValue();

        // Disable the path if it doesn't exist
        if (existing_dir.length() == 0) {
            existing_dir = null;
        } else {
            File f = new File(existing_dir);
            if (!f.exists()) {
                existing_dir = null;
            }
        }

        DirectoryDialog dd = new DirectoryDialog(mLocationPathField.getShell());
        dd.setMessage("Browse for folder");
        dd.setFilterPath(existing_dir);
        String abs_dir = dd.open();

        if (abs_dir != null) {
            updateLocationPathField(abs_dir);
            validatePageComplete();
        }
    }

    /**
     * Callback when the "create test project" checkbox is changed.
     * It enables or disables all UI groups accordingly.
     */
    private void onCreateTestProjectToggle() {
        boolean enabled = mInfo.getCreateTestProject();
        for (Composite c : mToggleComposites) {
            enableControl(c, enabled);
        }
        mSdkTargetSelector.setEnabled(enabled);

        if (enabled) {
            useMainProjectInformation();
        }
        validatePageComplete();
    }

    /** Enables or disables controls; recursive for composite controls. */
    private void enableControl(Control c, boolean enabled) {
        c.setEnabled(enabled);
        if (c instanceof Composite)
        for (Control c2 : ((Composite) c).getChildren()) {
            enableControl(c2, enabled);
        }
    }

    /**
     * Enables or disable the location widgets depending on the user selection:
     * the location path is enabled when using the "existing source" mode (i.e. not new project)
     * or in new project mode with the "use default location" turned off.
     */
    private void enableLocationWidgets() {
        boolean use_default = mInfo.useDefaultLocation();
        boolean location_enabled = !use_default;

        mLocationLabel.setEnabled(location_enabled);
        mLocationPathField.setEnabled(location_enabled);
        mBrowseButton.setEnabled(location_enabled);

        updateLocationPathField(null);
    }

    /**
     * Updates the location directory path field.
     * <br/>
     * When custom user selection is enabled, use the abs_dir argument if not null and also
     * save it internally. If abs_dir is null, restore the last saved abs_dir. This allows the
     * user selection to be remembered when the user switches from default to custom.
     * <br/>
     * When custom user selection is disabled, use the workspace default location with the
     * current project name. This does not change the internally cached abs_dir.
     *
     * @param abs_dir A new absolute directory path or null to use the default.
     */
    private void updateLocationPathField(String abs_dir) {
        boolean use_default = mInfo.useDefaultLocation();
        boolean custom_location = !use_default;

        if (!mInternalLocationPathUpdate) {
            mInternalLocationPathUpdate = true;
            if (custom_location) {
                if (abs_dir != null) {
                    // We get here if the user selected a directory with the "Browse" button.
                    sCustomLocationOsPath = TextProcessor.process(abs_dir);
                }
                if (!mLocationPathField.getText().equals(sCustomLocationOsPath)) {
                    mLocationPathField.setText(sCustomLocationOsPath);
                }
            } else {
                String value = Platform.getLocation().append(mInfo.getProjectName()).toString();
                value = TextProcessor.process(value);
                if (!mLocationPathField.getText().equals(value)) {
                    mLocationPathField.setText(value);
                }
            }
            validatePageComplete();
            mInternalLocationPathUpdate = false;
        }
    }

    /**
     * The location path field is either modified internally (from updateLocationPathField)
     * or manually by the user when the custom_location mode is not set.
     *
     * Ignore the internal modification. When modified by the user, memorize the choice and
     * validate the page.
     */
    private void onLocationPathFieldModified() {
        if (!mInternalLocationPathUpdate) {
            // When the updates doesn't come from updateLocationPathField, it must be the user
            // editing the field manually, in which case we want to save the value internally
            String newPath = getLocationPathFieldValue();
            sCustomLocationOsPath = newPath;
            validatePageComplete();
        }
    }

    /**
     * The package name field is either modified internally (from extractNamesFromAndroidManifest)
     * or manually by the user when the custom_location mode is not set.
     *
     * Ignore the internal modification. When modified by the user, memorize the choice and
     * validate the page.
     */
    private void onPackageNameFieldModified() {
        updateTestTargetPackageField(null);
        validatePageComplete();
    }

    /**
     * Changes the {@link #mTestTargetPackageLabel} field.
     *
     * When using the "self-test" option, the packageName argument is ignored and the
     * current value from the project package is used.
     *
     * Otherwise the packageName is used if it is not null.
     */
    private void updateTestTargetPackageField(String packageName) {
        if (mInfo.isTestingSelf()) {
            mTestTargetPackageLabel.setText(mInfo.getPackageName());

        } else if (packageName != null) {
            mTestTargetPackageLabel.setText(packageName);
        }
    }

    /**
     * Called when the min sdk version field has been modified.
     *
     * Ignore the internal modifications. When modified by the user, try to match
     * a target with the same API level.
     */
    private void onMinSdkVersionFieldModified() {
        if (mInternalMinSdkVersionUpdate || mInternalSdkTargetUpdate) {
            return;
        }

        updateSdkSelectorToMatchMinSdkVersion();

        mMinSdkVersionModifiedByUser = true;
    }

    /**
     * Try to find an SDK Target that matches the current MinSdkVersion.
     *
     * There can be multiple targets with the same sdk api version, so don't change
     * it if it's already at the right version. Otherwise pick the first target
     * that matches.
     */
    private void updateSdkSelectorToMatchMinSdkVersion() {
        String minSdkVersion = mInfo.getMinSdkVersion();

        IAndroidTarget curr_target = mInfo.getSdkTarget();
        if (curr_target != null && curr_target.getVersion().equals(minSdkVersion)) {
            return;
        }

        for (IAndroidTarget target : mSdkTargetSelector.getTargets()) {
            if (target.getVersion().equals(minSdkVersion)) {
                mSdkTargetSelector.setSelection(target);
                return;
            }
        }
    }

    /**
     * Called when an SDK target is modified.
     *
     * Also changes the minSdkVersion field to reflect the sdk api level that has
     * just been selected.
     */
    private void onSdkTargetModified() {
        if (mInternalMinSdkVersionUpdate || mInternalSdkTargetUpdate) {
            return;
        }

        IAndroidTarget target = mInfo.getSdkTarget();

        if (target != null) {
            mInternalMinSdkVersionUpdate = true;
            mMinSdkVersionField.setText(target.getVersion().getApiString());
            mInternalMinSdkVersionUpdate = false;
        }

        mSdkTargetModifiedByUser = true;
    }

    /**
     * Returns whether this page's controls currently all contain valid values.
     *
     * @return <code>true</code> if all controls are valid, and
     *         <code>false</code> if at least one is invalid
     */
    private boolean validatePage() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        int status = MSG_NONE;

        // there is nothing to validate if we're not going to create a test project
        if (mInfo.getCreateTestProject()) {
            status = validateProjectField(workspace);
            if ((status & MSG_ERROR) == 0) {
                status |= validateLocationPath(workspace);
            }
            if ((status & MSG_ERROR) == 0) {
                status |= validateTestTarget();
            }
            if ((status & MSG_ERROR) == 0) {
                status |= validateSdkTarget();
            }
            if ((status & MSG_ERROR) == 0) {
                status |= validatePackageField();
            }
            if ((status & MSG_ERROR) == 0) {
                status |= validateMinSdkVersionField();
            }
        }
        if (status == MSG_NONE)  {
            setStatus(null, MSG_NONE);
        }

        // Return false if there's an error so that the finish button be disabled.
        return (status & MSG_ERROR) == 0;
    }

    /**
     * Validates the page and updates the Next/Finish buttons
     */
    private void validatePageComplete() {
        setPageComplete(validatePage());
    }

    /**
     * Validates the test target (self, main project or existing project)
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateTestTarget() {
        if (mInfo.isTestingExisting() && mInfo.getExistingTestedProject() == null) {
            return setStatus("Please select an existing Android project as a test target.",
                    MSG_ERROR);
        }

        return MSG_NONE;
    }

    /**
     * Validates the project name field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateProjectField(IWorkspace workspace) {
        // Validate project field
        String projectName = mInfo.getProjectName();
        if (projectName.length() == 0) {
            return setStatus("Project name must be specified", MSG_ERROR);
        }

        // Limit the project name to shell-agnostic characters since it will be used to
        // generate the final package
        if (!sProjectNamePattern.matcher(projectName).matches()) {
            return setStatus("The project name must start with an alphanumeric characters, followed by one or more alphanumerics, digits, dots, dashes, underscores or spaces.",
                    MSG_ERROR);
        }

        IStatus nameStatus = workspace.validateName(projectName, IResource.PROJECT);
        if (!nameStatus.isOK()) {
            return setStatus(nameStatus.getMessage(), MSG_ERROR);
        }

        if (mMainInfo != null && projectName.equals(mMainInfo.getProjectName())) {
            return setStatus("The main project name and the test project name must be different.",
                    MSG_ERROR);
        }

        if (getProjectHandle().exists()) {
            return setStatus("A project with that name already exists in the workspace",
                    MSG_ERROR);
        }

        return MSG_NONE;
    }

    /**
     * Validates the location path field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateLocationPath(IWorkspace workspace) {
        Path path = new Path(getProjectLocation());
        if (!mInfo.useDefaultLocation()) {
            // If not using the default value validate the location.
            URI uri = URIUtil.toURI(path.toOSString());
            IStatus locationStatus = workspace.validateProjectLocationURI(getProjectHandle(),
                    uri);
            if (!locationStatus.isOK()) {
                return setStatus(locationStatus.getMessage(), MSG_ERROR);
            } else {
                // The location is valid as far as Eclipse is concerned (i.e. mostly not
                // an existing workspace project.) Check it either doesn't exist or is
                // a directory that is empty.
                File f = path.toFile();
                if (f.exists() && !f.isDirectory()) {
                    return setStatus("A directory name must be specified.", MSG_ERROR);
                } else if (f.isDirectory()) {
                    // However if the directory exists, we should put a warning if it is not
                    // empty. We don't put an error (we'll ask the user again for confirmation
                    // before using the directory.)
                    String[] l = f.list();
                    if (l.length != 0) {
                        return setStatus("The selected output directory is not empty.",
                                MSG_WARNING);
                    }
                }
            }
        } else {
            // Otherwise validate the path string is not empty
            if (getProjectLocation().length() == 0) {
                return setStatus("A directory name must be specified.", MSG_ERROR);
            }

            File dest = path.append(mInfo.getProjectName()).toFile();
            if (dest.exists()) {
                return setStatus(String.format("There is already a file or directory named \"%1$s\" in the selected location.",
                        mInfo.getProjectName()), MSG_ERROR);
            }
        }

        return MSG_NONE;
    }

    /**
     * Validates the sdk target choice.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateSdkTarget() {
        if (mInfo.getSdkTarget() == null) {
            return setStatus("An SDK Target must be specified.", MSG_ERROR);
        }
        return MSG_NONE;
    }

    /**
     * Validates the sdk target choice.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateMinSdkVersionField() {

        // If the min sdk version is empty, it is always accepted.
        if (mInfo.getMinSdkVersion().length() == 0) {
            return MSG_NONE;
        }

        if (mInfo.getSdkTarget() != null &&
                mInfo.getSdkTarget().getVersion().equals(mInfo.getMinSdkVersion()) == false) {
            return setStatus("The API level for the selected SDK target does not match the Min SDK version.",
                    mInfo.getSdkTarget().getVersion().isPreview() ? MSG_ERROR : MSG_WARNING);
        }

        return MSG_NONE;
    }

    /**
     * Validates the package name field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validatePackageField() {
        // Validate package field
        String packageName = mInfo.getPackageName();
        if (packageName.length() == 0) {
            return setStatus("Project package name must be specified.", MSG_ERROR);
        }

        // Check it's a valid package string
        int result = MSG_NONE;
        IStatus status = JavaConventions.validatePackageName(packageName, "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
        if (!status.isOK()) {
            result = setStatus(String.format("Project package: %s", status.getMessage()),
                        status.getSeverity() == IStatus.ERROR ? MSG_ERROR : MSG_WARNING);
        }

        // The Android Activity Manager does not accept packages names with only one
        // identifier. Check the package name has at least one dot in them (the previous rule
        // validated that if such a dot exist, it's not the first nor last characters of the
        // string.)
        if (result != MSG_ERROR && packageName.indexOf('.') == -1) {
            return setStatus("Project package name must have at least two identifiers.", MSG_ERROR);
        }

        // Check that the target package name is valid too
        packageName = mInfo.getTargetPackageName();
        if (packageName.length() == 0) {
            return setStatus("Target package name must be specified.", MSG_ERROR);
        }

        // Check it's a valid package string
        status = JavaConventions.validatePackageName(packageName, "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
        if (!status.isOK()) {
            result = setStatus(String.format("Target package: %s", status.getMessage()),
                        status.getSeverity() == IStatus.ERROR ? MSG_ERROR : MSG_WARNING);
        }

        if (result != MSG_ERROR && packageName.indexOf('.') == -1) {
            return setStatus("Target name must have at least two identifiers.", MSG_ERROR);
        }

        return result;
    }

    /**
     * Sets the error message for the wizard with the given message icon.
     *
     * @param message The wizard message type, one of MSG_ERROR or MSG_WARNING.
     * @return As a convenience, always returns messageType so that the caller can return
     *         immediately.
     */
    private int setStatus(String message, int messageType) {
        if (message == null) {
            setErrorMessage(null);
            setMessage(null);
        } else if (!message.equals(getMessage())) {
            setMessage(message, messageType == MSG_WARNING ? WizardPage.WARNING : WizardPage.ERROR);
        }
        return messageType;
    }

}
