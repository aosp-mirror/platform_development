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
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser.Activity;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.ide.eclipse.adt.internal.wizards.newproject.NewTestProjectCreationPage.TestInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
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
public class NewProjectCreationPage extends WizardPage {

    // constants
    private static final String MAIN_PAGE_NAME = "newAndroidProjectPage"; //$NON-NLS-1$

    /** Initial value for all name fields (project, activity, application, package). Used
     * whenever a value is requested before controls are created. */
    private static final String INITIAL_NAME = "";  //$NON-NLS-1$
    /** Initial value for the Create New Project radio; False means Create From Existing would be
     * the default.*/
    private static final boolean INITIAL_CREATE_NEW_PROJECT = true;
    /** Initial value for the Use Default Location check box. */
    private static final boolean INITIAL_USE_DEFAULT_LOCATION = true;
    /** Initial value for the Create Activity check box. */
    private static final boolean INITIAL_CREATE_ACTIVITY = true;


    /** Pattern for characters accepted in a project name. Since this will be used as a
     * directory name, we're being a bit conservative on purpose. It cannot start with a space. */
    private static final Pattern sProjectNamePattern = Pattern.compile("^[\\w][\\w. -]*$");  //$NON-NLS-1$
    /** Last user-browsed location, static so that it be remembered for the whole session */
    private static String sCustomLocationOsPath = "";  //$NON-NLS-1$
    private static boolean sAutoComputeCustomLocation = true;

    private final int MSG_NONE = 0;
    private final int MSG_WARNING = 1;
    private final int MSG_ERROR = 2;

    /** Structure with the externally visible information from this Main Project page. */
    private final MainInfo mInfo = new MainInfo();
    /** Structure with the externally visible information from the Test Project page.
     *  This is null if there's no such page, meaning the main project page is standalone. */
    private TestInfo mTestInfo;

    private String mUserPackageName = "";       //$NON-NLS-1$
    private String mUserActivityName = "";      //$NON-NLS-1$
    private boolean mUserCreateActivityCheck = INITIAL_CREATE_ACTIVITY;
    private String mSourceFolder = "";          //$NON-NLS-1$

    // widgets
    private Text mProjectNameField;
    private Text mPackageNameField;
    private Text mActivityNameField;
    private Text mApplicationNameField;
    private Button mCreateNewProjectRadio;
    private Button mUseDefaultLocation;
    private Label mLocationLabel;
    private Text mLocationPathField;
    private Button mBrowseButton;
    private Button mCreateActivityCheck;
    private Text mMinSdkVersionField;
    private SdkTargetSelector mSdkTargetSelector;
    private ITargetChangeListener mSdkTargetChangeListener;

    private boolean mInternalLocationPathUpdate;
    private boolean mInternalProjectNameUpdate;
    private boolean mInternalApplicationNameUpdate;
    private boolean mInternalCreateActivityUpdate;
    private boolean mInternalActivityNameUpdate;
    private boolean mProjectNameModifiedByUser;
    private boolean mApplicationNameModifiedByUser;
    private boolean mInternalMinSdkVersionUpdate;


    /**
     * Creates a new project creation wizard page.
     */
    public NewProjectCreationPage() {
        super(MAIN_PAGE_NAME);
        setPageComplete(false);
        setTitle("New Android Project");
        setDescription("Creates a new Android Project resource.");
    }

    // --- Getters used by NewProjectWizard ---


    /**
     * Structure that collects all externally visible information from this page.
     * This is used by the calling wizard to actually do the work or by other pages.
     * <p/>
     * This interface is provided so that the adt-test counterpart can override the returned
     * information.
     */
    public interface IMainInfo {
        public IPath getLocationPath();
        /**
         * Returns the current project location path as entered by the user, or its
         * anticipated initial value. Note that if the default has been returned the
         * path in a project description used to create a project should not be set.
         *
         * @return the project location path or its anticipated initial value.
         */
        /** Returns the value of the project name field with leading and trailing spaces removed. */
        public String getProjectName();
        /** Returns the value of the package name field with spaces trimmed. */
        public String getPackageName();
        /** Returns the value of the activity name field with spaces trimmed. */
        public String getActivityName();
        /** Returns the value of the min sdk version field with spaces trimmed. */
        public String getMinSdkVersion();
        /** Returns the value of the application name field with spaces trimmed. */
        public String getApplicationName();
        /** Returns the value of the "Create New Project" radio. */
        public boolean isNewProject();
        /** Returns the value of the "Create Activity" checkbox. */
        public boolean isCreateActivity();
        /** Returns the value of the Use Default Location field. */
        public boolean useDefaultLocation();
        /** Returns the internal source folder (for the "existing project" mode) or the default
         * "src" constant. */
        public String getSourceFolder();
        /** Returns the current sdk target or null if none has been selected yet. */
        public IAndroidTarget getSdkTarget();
    }


    /**
     * Structure that collects all externally visible information from this page.
     * This is used by the calling wizard to actually do the work or by other pages.
     */
    public class MainInfo implements IMainInfo {
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

        /** Returns the value of the activity name field with spaces trimmed. */
        public String getActivityName() {
            return mActivityNameField == null ? INITIAL_NAME : mActivityNameField.getText().trim();
        }

        /** Returns the value of the min sdk version field with spaces trimmed. */
        public String getMinSdkVersion() {
            return mMinSdkVersionField == null ? "" : mMinSdkVersionField.getText().trim();  //$NON-NLS-1$
        }

        /** Returns the value of the application name field with spaces trimmed. */
        public String getApplicationName() {
            // Return the name of the activity as default application name.
            return mApplicationNameField == null ? getActivityName()
                                                 : mApplicationNameField.getText().trim();

        }

        /** Returns the value of the "Create New Project" radio. */
        public boolean isNewProject() {
            return mCreateNewProjectRadio == null ? INITIAL_CREATE_NEW_PROJECT
                                                  : mCreateNewProjectRadio.getSelection();
        }

        /** Returns the value of the "Create Activity" checkbox. */
        public boolean isCreateActivity() {
            return mCreateActivityCheck == null ? INITIAL_CREATE_ACTIVITY
                                                  : mCreateActivityCheck.getSelection();
        }

        /** Returns the value of the Use Default Location field. */
        public boolean useDefaultLocation() {
            return mUseDefaultLocation == null ? INITIAL_USE_DEFAULT_LOCATION
                                               : mUseDefaultLocation.getSelection();
        }

        /** Returns the internal source folder (for the "existing project" mode) or the default
         * "src" constant. */
        public String getSourceFolder() {
            if (isNewProject() || mSourceFolder == null || mSourceFolder.length() == 0) {
                return SdkConstants.FD_SOURCES;
            } else {
                return mSourceFolder;
            }
        }

        /** Returns the current sdk target or null if none has been selected yet. */
        public IAndroidTarget getSdkTarget() {
            return mSdkTargetSelector == null ? null : mSdkTargetSelector.getSelected();
        }
    }

    /**
     * Returns a {@link MainInfo} structure that collects all externally visible information
     * from this page, to be used by the calling wizard or by other pages.
     */
    public IMainInfo getMainInfo() {
        return mInfo;
    }

    /**
     * Grabs the {@link TestInfo} structure that collects externally visible fields from the
     * test project page. This may be null.
     */
    public void setTestInfo(TestInfo testInfo) {
        mTestInfo = testInfo;
    }

    /**
     * Overrides @DialogPage.setVisible(boolean) to put the focus in the project name when
     * the dialog is made visible.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            mProjectNameField.setFocus();
            validatePageComplete();
        }
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

        createProjectNameGroup(composite);
        createLocationGroup(composite);
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

    @Override
    public void dispose() {

        if (mSdkTargetChangeListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mSdkTargetChangeListener);
            mSdkTargetChangeListener = null;
        }

        super.dispose();
    }

    /**
     * Creates the group for the project name:
     * [label: "Project Name"] [text field]
     *
     * @param parent the parent composite
     */
    private final void createProjectNameGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // new project label
        Label label = new Label(group, SWT.NONE);
        label.setText("Project name:");
        label.setFont(parent.getFont());
        label.setToolTipText("Name of the Eclipse project to create. It cannot be empty.");

        // new project name entry field
        mProjectNameField = new Text(group, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        mProjectNameField.setToolTipText("Name of the Eclipse project to create. It cannot be empty.");
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


    /**
     * Creates the group for the Project options:
     * [radio] Create new project
     * [radio] Create project from existing sources
     * [check] Use default location
     * Location [text field] [browse button]
     *
     * @param parent the parent composite
     */
    private final void createLocationGroup(Composite parent) {
        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        // Layout has 4 columns of non-equal size
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(parent.getFont());
        group.setText("Contents");

        mCreateNewProjectRadio = new Button(group, SWT.RADIO);
        mCreateNewProjectRadio.setText("Create new project in workspace");
        mCreateNewProjectRadio.setSelection(INITIAL_CREATE_NEW_PROJECT);
        Button existing_project_radio = new Button(group, SWT.RADIO);
        existing_project_radio.setText("Create project from existing source");
        existing_project_radio.setSelection(!INITIAL_CREATE_NEW_PROJECT);

        mUseDefaultLocation = new Button(group, SWT.CHECK);
        mUseDefaultLocation.setText("Use default location");
        mUseDefaultLocation.setSelection(INITIAL_USE_DEFAULT_LOCATION);

        SelectionListener location_listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                enableLocationWidgets();
                extractNamesFromAndroidManifest();
                validatePageComplete();
            }
        };

        mCreateNewProjectRadio.addSelectionListener(location_listener);
        existing_project_radio.addSelectionListener(location_listener);
        mUseDefaultLocation.addSelectionListener(location_listener);

        Composite location_group = new Composite(group, SWT.NONE);
        location_group.setLayout(new GridLayout(3, /* num columns */
                false /* columns of not equal size */));
        location_group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        location_group.setFont(parent.getFont());

        mLocationLabel = new Label(location_group, SWT.NONE);
        mLocationLabel.setText("Location:");

        mLocationPathField = new Text(location_group, SWT.BORDER);
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

        mBrowseButton = new Button(location_group, SWT.PUSH);
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
                onPackageNameFieldModified();
            }
        });

        // new activity label
        mCreateActivityCheck = new Button(group, SWT.CHECK);
        mCreateActivityCheck.setText("Create Activity:");
        mCreateActivityCheck.setToolTipText("Specifies if you want to create a default Activity.");
        mCreateActivityCheck.setFont(parent.getFont());
        mCreateActivityCheck.setSelection(INITIAL_CREATE_ACTIVITY);
        mCreateActivityCheck.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                onCreateActivityCheckModified();
                enableLocationWidgets();
            }
        });

        // new activity name entry field
        mActivityNameField = new Text(group, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        mActivityNameField.setToolTipText("Name of the Activity class to create. Must be a valid Java identifier.");
        mActivityNameField.setLayoutData(data);
        mActivityNameField.setFont(parent.getFont());
        mActivityNameField.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                onActivityNameFieldModified();
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
        if (mInfo.isNewProject() && mInfo.useDefaultLocation()) {
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
            extractNamesFromAndroidManifest();
            validatePageComplete();
        }
    }

    /**
     * Enables or disable the location widgets depending on the user selection:
     * the location path is enabled when using the "existing source" mode (i.e. not new project)
     * or in new project mode with the "use default location" turned off.
     */
    private void enableLocationWidgets() {
        boolean is_new_project = mInfo.isNewProject();
        boolean use_default = mInfo.useDefaultLocation();
        boolean location_enabled = !is_new_project || !use_default;
        boolean create_activity = mInfo.isCreateActivity();

        mUseDefaultLocation.setEnabled(is_new_project);

        mLocationLabel.setEnabled(location_enabled);
        mLocationPathField.setEnabled(location_enabled);
        mBrowseButton.setEnabled(location_enabled);

        mPackageNameField.setEnabled(is_new_project);
        mCreateActivityCheck.setEnabled(is_new_project);
        mActivityNameField.setEnabled(is_new_project & create_activity);

        updateLocationPathField(null);
        updatePackageAndActivityFields();
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
        boolean is_new_project = mInfo.isNewProject();
        boolean use_default = mInfo.useDefaultLocation();
        boolean custom_location = !is_new_project || !use_default;

        if (!mInternalLocationPathUpdate) {
            mInternalLocationPathUpdate = true;
            if (custom_location) {
                if (abs_dir != null) {
                    // We get here if the user selected a directory with the "Browse" button.
                    // Disable auto-compute of the custom location unless the user selected
                    // the exact same path.
                    sAutoComputeCustomLocation = sAutoComputeCustomLocation &&
                                                 abs_dir.equals(sCustomLocationOsPath);
                    sCustomLocationOsPath = TextProcessor.process(abs_dir);
                } else  if (sAutoComputeCustomLocation ||
                            (!is_new_project && !new File(sCustomLocationOsPath).isDirectory())) {
                    // By default select the samples directory of the current target
                    IAndroidTarget target = mInfo.getSdkTarget();
                    if (target != null) {
                        sCustomLocationOsPath = target.getPath(IAndroidTarget.SAMPLES);
                    }

                    // If we don't have a target, select the base directory of the
                    // "universal sdk". If we don't even have that, use a root drive.
                    if (sCustomLocationOsPath == null || sCustomLocationOsPath.length() == 0) {
                        if (Sdk.getCurrent() != null) {
                            sCustomLocationOsPath = Sdk.getCurrent().getSdkLocation();
                        } else {
                            sCustomLocationOsPath = File.listRoots()[0].getAbsolutePath();
                        }
                    }
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
            // and we disable auto-compute of the custom location (to avoid overriding the user
            // value)
            String newPath = getLocationPathFieldValue();
            sAutoComputeCustomLocation = sAutoComputeCustomLocation &&
                                         newPath.equals(sCustomLocationOsPath);
            sCustomLocationOsPath = newPath;
            extractNamesFromAndroidManifest();
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
        if (mInfo.isNewProject()) {
            mUserPackageName = mInfo.getPackageName();
            validatePageComplete();
        }
    }

    /**
     * The create activity checkbox is either modified internally (from
     * extractNamesFromAndroidManifest)  or manually by the user.
     *
     * Ignore the internal modification. When modified by the user, memorize the choice and
     * validate the page.
     */
    private void onCreateActivityCheckModified() {
        if (mInfo.isNewProject() && !mInternalCreateActivityUpdate) {
            mUserCreateActivityCheck = mInfo.isCreateActivity();
        }
        validatePageComplete();
    }

    /**
     * The activity name field is either modified internally (from extractNamesFromAndroidManifest)
     * or manually by the user when the custom_location mode is not set.
     *
     * Ignore the internal modification. When modified by the user, memorize the choice and
     * validate the page.
     */
    private void onActivityNameFieldModified() {
        if (mInfo.isNewProject() && !mInternalActivityNameUpdate) {
            mUserActivityName = mInfo.getActivityName();
            validatePageComplete();
        }
    }

    /**
     * Called when the min sdk version field has been modified.
     *
     * Ignore the internal modifications. When modified by the user, try to match
     * a target with the same API level.
     */
    private void onMinSdkVersionFieldModified() {
        if (mInternalMinSdkVersionUpdate) {
            return;
        }

        String minSdkVersion = mInfo.getMinSdkVersion();

        // Before changing, compare with the currently selected one, if any.
        // There can be multiple targets with the same sdk api version, so don't change
        // it if it's already at the right version.
        IAndroidTarget curr_target = mInfo.getSdkTarget();
        if (curr_target != null && curr_target.getVersion().equals(minSdkVersion)) {
            return;
        }

        for (IAndroidTarget target : mSdkTargetSelector.getTargets()) {
            if (target.getVersion().equals(minSdkVersion)) {
                mSdkTargetSelector.setSelection(target);
                break;
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
        IAndroidTarget target = mInfo.getSdkTarget();

        if (target != null) {
            mInternalMinSdkVersionUpdate = true;
            mMinSdkVersionField.setText(target.getVersion().getApiString());
            mInternalMinSdkVersionUpdate = false;
        }
    }

    /**
     * Called when the radio buttons are changed between the "create new project" and the
     * "use existing source" mode. This reverts the fields to whatever the user manually
     * entered before.
     */
    private void updatePackageAndActivityFields() {
        if (mInfo.isNewProject()) {
            if (mUserPackageName.length() > 0 &&
                    !mPackageNameField.getText().equals(mUserPackageName)) {
                mPackageNameField.setText(mUserPackageName);
            }

            if (mUserActivityName.length() > 0 &&
                    !mActivityNameField.getText().equals(mUserActivityName)) {
                mInternalActivityNameUpdate = true;
                mActivityNameField.setText(mUserActivityName);
                mInternalActivityNameUpdate = false;
            }

            if (mUserCreateActivityCheck != mCreateActivityCheck.getSelection()) {
                mInternalCreateActivityUpdate = true;
                mCreateActivityCheck.setSelection(mUserCreateActivityCheck);
                mInternalCreateActivityUpdate = false;
            }
        }
    }

    /**
     * Extract names from an android manifest.
     * This is done only if the user selected the "use existing source" and a manifest xml file
     * can actually be found in the custom user directory.
     */
    private void extractNamesFromAndroidManifest() {
        if (mInfo.isNewProject()) {
            return;
        }

        String projectLocation = getProjectLocation();
        File f = new File(projectLocation);
        if (!f.isDirectory()) {
            return;
        }

        Path path = new Path(f.getPath());
        String osPath = path.append(AndroidConstants.FN_ANDROID_MANIFEST).toOSString();

        AndroidManifestParser manifestData = AndroidManifestParser.parseForData(osPath);
        if (manifestData == null) {
            return;
        }

        String packageName = null;
        Activity activity = null;
        String activityName = null;
        String minSdkVersion = null;
        try {
            packageName = manifestData.getPackage();
            minSdkVersion = manifestData.getApiLevelRequirement();

            // try to get the first launcher activity. If none, just take the first activity.
            activity = manifestData.getLauncherActivity();
            if (activity == null) {
                Activity[] activities = manifestData.getActivities();
                if (activities != null && activities.length > 0) {
                    activity = activities[0];
                }
            }
        } catch (Exception e) {
            // ignore exceptions
        }

        if (packageName != null && packageName.length() > 0) {
            mPackageNameField.setText(packageName);
        }

        if (activity != null) {
            activityName = AndroidManifestParser.extractActivityName(activity.getName(),
                    packageName);
        }

        if (activityName != null && activityName.length() > 0) {
            mInternalActivityNameUpdate = true;
            mInternalCreateActivityUpdate = true;
            mActivityNameField.setText(activityName);
            mCreateActivityCheck.setSelection(true);
            mInternalCreateActivityUpdate = false;
            mInternalActivityNameUpdate = false;

            // If project name and application names are empty, use the activity
            // name as a default. If the activity name has dots, it's a part of a
            // package specification and only the last identifier must be used.
            if (activityName.indexOf('.') != -1) {
                String[] ids = activityName.split(AndroidConstants.RE_DOT);
                activityName = ids[ids.length - 1];
            }
            if (mProjectNameField.getText().length() == 0 ||
                    !mProjectNameModifiedByUser) {
                mInternalProjectNameUpdate = true;
                mProjectNameField.setText(activityName);
                mInternalProjectNameUpdate = false;
            }
            if (mApplicationNameField.getText().length() == 0 ||
                    !mApplicationNameModifiedByUser) {
                mInternalApplicationNameUpdate = true;
                mApplicationNameField.setText(activityName);
                mInternalApplicationNameUpdate = false;
            }
        } else {
            mInternalActivityNameUpdate = true;
            mInternalCreateActivityUpdate = true;
            mActivityNameField.setText("");  //$NON-NLS-1$
            mCreateActivityCheck.setSelection(false);
            mInternalCreateActivityUpdate = false;
            mInternalActivityNameUpdate = false;

            // There is no activity name to use to fill in the project and application
            // name. However if there's a package name, we can use this as a base.
            if (packageName != null && packageName.length() > 0) {
                // Package name is a java identifier, so it's most suitable for
                // an application name.

                if (mApplicationNameField.getText().length() == 0 ||
                        !mApplicationNameModifiedByUser) {
                    mInternalApplicationNameUpdate = true;
                    mApplicationNameField.setText(packageName);
                    mInternalApplicationNameUpdate = false;
                }

                // For the project name, remove any dots
                packageName = packageName.replace('.', '_');
                if (mProjectNameField.getText().length() == 0 ||
                        !mProjectNameModifiedByUser) {
                    mInternalProjectNameUpdate = true;
                    mProjectNameField.setText(packageName);
                    mInternalProjectNameUpdate = false;
                }

            }
        }

        // Select the target matching the manifest's sdk or build properties, if any
        boolean foundTarget = false;

        ProjectProperties p = ProjectProperties.create(projectLocation, null);
        if (p != null) {
            // Check the {build|default}.properties files if present
            p.merge(PropertyType.BUILD).merge(PropertyType.DEFAULT);
            String v = p.getProperty(ProjectProperties.PROPERTY_TARGET);
            IAndroidTarget target = Sdk.getCurrent().getTargetFromHashString(v);
            if (target != null) {
                mSdkTargetSelector.setSelection(target);
                foundTarget = true;
            }
        }

        if (!foundTarget && minSdkVersion != null) {
            for (IAndroidTarget target : mSdkTargetSelector.getTargets()) {
                if (target.getVersion().equals(minSdkVersion)) {
                    mSdkTargetSelector.setSelection(target);
                    foundTarget = true;
                    break;
                }
            }
        }

        if (!foundTarget) {
            for (IAndroidTarget target : mSdkTargetSelector.getTargets()) {
                if (projectLocation.startsWith(target.getLocation())) {
                    mSdkTargetSelector.setSelection(target);
                    foundTarget = true;
                    break;
                }
            }
        }

        if (!foundTarget) {
            mInternalMinSdkVersionUpdate = true;
            if (minSdkVersion != null) {
                mMinSdkVersionField.setText(minSdkVersion);
            }
            mInternalMinSdkVersionUpdate = false;
        }
    }

    /**
     * Returns whether this page's controls currently all contain valid values.
     *
     * @return <code>true</code> if all controls are valid, and
     *         <code>false</code> if at least one is invalid
     */
    private boolean validatePage() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        int status = validateProjectField(workspace);
        if ((status & MSG_ERROR) == 0) {
            status |= validateLocationPath(workspace);
        }
        if ((status & MSG_ERROR) == 0) {
            status |= validateSdkTarget();
        }
        if ((status & MSG_ERROR) == 0) {
            status |= validatePackageField();
        }
        if ((status & MSG_ERROR) == 0) {
            status |= validateActivityField();
        }
        if ((status & MSG_ERROR) == 0) {
            status |= validateMinSdkVersionField();
        }
        if ((status & MSG_ERROR) == 0) {
            status |= validateSourceFolder();
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

        if (getProjectHandle().exists()) {
            return setStatus("A project with that name already exists in the workspace",
                    MSG_ERROR);
        }

        if (mTestInfo != null &&
                mTestInfo.getCreateTestProject() &&
                projectName.equals(mTestInfo.getProjectName())) {
            return setStatus("The main project name and the test project name must be different.",
                    MSG_WARNING);
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
        if (mInfo.isNewProject()) {
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
        } else {
            // Must be an existing directory
            File f = path.toFile();
            if (!f.isDirectory()) {
                return setStatus("An existing directory name must be specified.", MSG_ERROR);
            }

            // Check there's an android manifest in the directory
            String osPath = path.append(AndroidConstants.FN_ANDROID_MANIFEST).toOSString();
            File manifestFile = new File(osPath);
            if (!manifestFile.isFile()) {
                return setStatus(
                        String.format("File %1$s not found in %2$s.",
                                AndroidConstants.FN_ANDROID_MANIFEST, f.getName()),
                                MSG_ERROR);
            }

            // Parse it and check the important fields.
            AndroidManifestParser manifestData = AndroidManifestParser.parseForData(osPath);
            if (manifestData == null) {
                return setStatus(
                        String.format("File %1$s could not be parsed.", osPath),
                        MSG_ERROR);
            }

            String packageName = manifestData.getPackage();
            if (packageName == null || packageName.length() == 0) {
                return setStatus(
                        String.format("No package name defined in %1$s.", osPath),
                        MSG_ERROR);
            }

            Activity[] activities = manifestData.getActivities();
            if (activities == null || activities.length == 0) {
                // This is acceptable now as long as no activity needs to be created
                if (mInfo.isCreateActivity()) {
                    return setStatus(
                            String.format("No activity name defined in %1$s.", osPath),
                            MSG_ERROR);
                }
            }

            // If there's already a .project, tell the user to use import instead.
            if (path.append(".project").toFile().exists()) {  //$NON-NLS-1$
                return setStatus("An Eclipse project already exists in this directory. Consider using File > Import > Existing Project instead.",
                        MSG_WARNING);
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
     * Validates the activity name field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateActivityField() {
        // Disregard if not creating an activity
        if (!mInfo.isCreateActivity()) {
            return MSG_NONE;
        }

        // Validate activity field
        String activityFieldContents = mInfo.getActivityName();
        if (activityFieldContents.length() == 0) {
            return setStatus("Activity name must be specified.", MSG_ERROR);
        }

        // The activity field can actually contain part of a sub-package name
        // or it can start with a dot "." to indicates it comes from the parent package name.
        String packageName = "";  //$NON-NLS-1$
        int pos = activityFieldContents.lastIndexOf('.');
        if (pos >= 0) {
            packageName = activityFieldContents.substring(0, pos);
            if (packageName.startsWith(".")) { //$NON-NLS-1$
                packageName = packageName.substring(1);
            }

            activityFieldContents = activityFieldContents.substring(pos + 1);
        }

        // the activity field can contain a simple java identifier, or a
        // package name or one that starts with a dot. So if it starts with a dot,
        // ignore this dot -- the rest must look like a package name.
        if (activityFieldContents.charAt(0) == '.') {
            activityFieldContents = activityFieldContents.substring(1);
        }

        // Check it's a valid activity string
        int result = MSG_NONE;
        IStatus status = JavaConventions.validateTypeVariableName(activityFieldContents,
                                                            "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
        if (!status.isOK()) {
            result = setStatus(status.getMessage(),
                        status.getSeverity() == IStatus.ERROR ? MSG_ERROR : MSG_WARNING);
        }

        // Check it's a valid package string
        if (result != MSG_ERROR && packageName.length() > 0) {
            status = JavaConventions.validatePackageName(packageName,
                                                            "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
            if (!status.isOK()) {
                result = setStatus(status.getMessage() + " (in the activity name)",
                            status.getSeverity() == IStatus.ERROR ? MSG_ERROR : MSG_WARNING);
            }
        }


        return result;
    }

    /**
     * Validates the package name field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validatePackageField() {
        // Validate package field
        String packageFieldContents = mInfo.getPackageName();
        if (packageFieldContents.length() == 0) {
            return setStatus("Package name must be specified.", MSG_ERROR);
        }

        // Check it's a valid package string
        int result = MSG_NONE;
        IStatus status = JavaConventions.validatePackageName(packageFieldContents, "1.5", "1.5"); //$NON-NLS-1$ $NON-NLS-2$
        if (!status.isOK()) {
            result = setStatus(status.getMessage(),
                        status.getSeverity() == IStatus.ERROR ? MSG_ERROR : MSG_WARNING);
        }

        // The Android Activity Manager does not accept packages names with only one
        // identifier. Check the package name has at least one dot in them (the previous rule
        // validated that if such a dot exist, it's not the first nor last characters of the
        // string.)
        if (result != MSG_ERROR && packageFieldContents.indexOf('.') == -1) {
            return setStatus("Package name must have at least two identifiers.", MSG_ERROR);
        }

        return result;
    }

    /**
     * Validates that an existing project actually has a source folder.
     *
     * For project in "use existing source" mode, this tries to find the source folder.
     * A source folder should be just under the project directory and it should have all
     * the directories composing the package+activity name.
     *
     * As a side effect, it memorizes the source folder in mSourceFolder.
     *
     * TODO: support multiple source folders for multiple activities.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    private int validateSourceFolder() {
        // This check does nothing when creating a new project.
        // This check is also useless when no activity is present or created.
        if (mInfo.isNewProject() || !mInfo.isCreateActivity()) {
            return MSG_NONE;
        }

        String osTarget = mInfo.getActivityName();

        if (osTarget.indexOf('.') == -1) {
            osTarget = mInfo.getPackageName() + File.separator + osTarget;
        } else if (osTarget.indexOf('.') == 0) {
            osTarget = mInfo.getPackageName() + osTarget;
        }
        osTarget = osTarget.replace('.', File.separatorChar) + AndroidConstants.DOT_JAVA;

        String projectPath = getProjectLocation();
        File projectDir = new File(projectPath);
        File[] all_dirs = projectDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File f : all_dirs) {
            Path path = new Path(f.getAbsolutePath());
            File java_activity = path.append(osTarget).toFile();
            if (java_activity.isFile()) {
                mSourceFolder = f.getName();
                return MSG_NONE;
            }
        }

        if (all_dirs.length > 0) {
            return setStatus(
                    String.format("%1$s can not be found under %2$s.", osTarget, projectPath),
                    MSG_ERROR);
        } else {
            return setStatus(
                    String.format("No source folders can be found in %1$s.", projectPath),
                    MSG_ERROR);
        }
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
