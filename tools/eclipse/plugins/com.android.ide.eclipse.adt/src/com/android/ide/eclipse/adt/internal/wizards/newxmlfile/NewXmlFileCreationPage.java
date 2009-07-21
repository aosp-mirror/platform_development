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


package com.android.ide.eclipse.adt.internal.wizards.newxmlfile;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DocumentDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.adt.internal.editors.menu.descriptors.MenuDescriptors;
import com.android.ide.eclipse.adt.internal.editors.resources.descriptors.ResourcesDescriptors;
import com.android.ide.eclipse.adt.internal.project.ProjectChooserHelper;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.ConfigurationState;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * This is the single page of the {@link NewXmlFileWizard} which provides the ability to create
 * skeleton XML resources files for Android projects.
 * <p/>
 * This page is used to select the project, the resource folder, resource type and file name.
 */
class NewXmlFileCreationPage extends WizardPage {

    /**
     * Information on one type of resource that can be created (e.g. menu, pref, layout, etc.)
     */
    static class TypeInfo {
        private final String mUiName;
        private final ResourceFolderType mResFolderType;
        private final String mTooltip;
        private final Object mRootSeed;
        private Button mWidget;
        private ArrayList<String> mRoots = new ArrayList<String>();
        private final String mXmlns;
        private final String mDefaultAttrs;
        private final String mDefaultRoot;
        private final int mTargetApiLevel;

        public TypeInfo(String uiName,
                        String tooltip,
                        ResourceFolderType resFolderType,
                        Object rootSeed,
                        String defaultRoot,
                        String xmlns,
                        String defaultAttrs,
                        int targetApiLevel) {
            mUiName = uiName;
            mResFolderType = resFolderType;
            mTooltip = tooltip;
            mRootSeed = rootSeed;
            mDefaultRoot = defaultRoot;
            mXmlns = xmlns;
            mDefaultAttrs = defaultAttrs;
            mTargetApiLevel = targetApiLevel;
        }

        /** Returns the UI name for the resource type. Unique. Never null. */
        String getUiName() {
            return mUiName;
        }

        /** Returns the tooltip for the resource type. Can be null. */
        String getTooltip() {
            return mTooltip;
        }

        /**
         * Returns the name of the {@link ResourceFolderType}.
         * Never null but not necessarily unique,
         * e.g. two types use  {@link ResourceFolderType#XML}.
         */
        String getResFolderName() {
            return mResFolderType.getName();
        }

        /**
         * Returns the matching {@link ResourceFolderType}.
         * Never null but not necessarily unique,
         * e.g. two types use  {@link ResourceFolderType#XML}.
         */
        ResourceFolderType getResFolderType() {
            return mResFolderType;
        }

        /** Sets the radio button associate with the resource type. Can be null. */
        void setWidget(Button widget) {
            mWidget = widget;
        }

        /** Returns the radio button associate with the resource type. Can be null. */
        Button getWidget() {
            return mWidget;
        }

        /**
         * Returns the seed used to fill the root element values.
         * The seed might be either a String, a String array, an {@link ElementDescriptor},
         * a {@link DocumentDescriptor} or null.
         */
        Object getRootSeed() {
            return mRootSeed;
        }

        /** Returns the default root element that should be selected by default. Can be null. */
        String getDefaultRoot() {
            return mDefaultRoot;
        }

        /**
         * Returns the list of all possible root elements for the resource type.
         * This can be an empty ArrayList but not null.
         * <p/>
         * TODO: the root list SHOULD depend on the currently selected project, to include
         * custom classes.
         */
        ArrayList<String> getRoots() {
            return mRoots;
        }

        /**
         * If the generated resource XML file requires an "android" XMLNS, this should be set
         * to {@link SdkConstants#NS_RESOURCES}. When it is null, no XMLNS is generated.
         */
        String getXmlns() {
            return mXmlns;
        }

        /**
         * When not null, this represent extra attributes that must be specified in the
         * root element of the generated XML file. When null, no extra attributes are inserted.
         */
        String getDefaultAttrs() {
            return mDefaultAttrs;
        }

        /**
         * The minimum API level required by the current SDK target to support this feature.
         */
        public int getTargetApiLevel() {
            return mTargetApiLevel;
        }
    }

    /**
     * TypeInfo, information for each "type" of file that can be created.
     */
    private static final TypeInfo[] sTypes = {
        new TypeInfo(
                "Layout",                                           // UI name
                "An XML file that describes a screen layout.",      // tooltip
                ResourceFolderType.LAYOUT,                          // folder type
                AndroidTargetData.DESCRIPTOR_LAYOUT,                // root seed
                "LinearLayout",                                     // default root
                SdkConstants.NS_RESOURCES,                          // xmlns
                "android:layout_width=\"wrap_content\"\n" +         // default attributes
                "android:layout_height=\"wrap_content\"",
                1                                                   // target API level
                ),
        new TypeInfo("Values",                                      // UI name
                "An XML file with simple values: colors, strings, dimensions, etc.", // tooltip
                ResourceFolderType.VALUES,                          // folder type
                ResourcesDescriptors.ROOT_ELEMENT,                  // root seed
                null,                                               // default root
                null,                                               // xmlns
                null,                                               // default attributes
                1                                                   // target API level
                ),
        new TypeInfo("Menu",                                        // UI name
                "An XML file that describes an menu.",              // tooltip
                ResourceFolderType.MENU,                            // folder type
                MenuDescriptors.MENU_ROOT_ELEMENT,                  // root seed
                null,                                               // default root
                SdkConstants.NS_RESOURCES,                          // xmlns
                null,                                               // default attributes
                1                                                   // target API level
                ),
        new TypeInfo("AppWidget Provider",                          // UI name
                "An XML file that describes a widget provider.",    // tooltip
                ResourceFolderType.XML,                             // folder type
                AndroidTargetData.DESCRIPTOR_APPWIDGET_PROVIDER,    // root seed
                null,                                               // default root
                SdkConstants.NS_RESOURCES,                          // xmlns
                null,                                               // default attributes
                3                                                   // target API level
                ),
        new TypeInfo("Preference",                                  // UI name
                "An XML file that describes preferences.",          // tooltip
                ResourceFolderType.XML,                             // folder type
                AndroidTargetData.DESCRIPTOR_PREFERENCES,           // root seed
                AndroidConstants.CLASS_NAME_PREFERENCE_SCREEN,      // default root
                SdkConstants.NS_RESOURCES,                          // xmlns
                null,                                               // default attributes
                1                                                   // target API level
                ),
        new TypeInfo("Searchable",                                  // UI name
                "An XML file that describes a searchable.",         // tooltip
                ResourceFolderType.XML,                             // folder type
                AndroidTargetData.DESCRIPTOR_SEARCHABLE,            // root seed
                null,                                               // default root
                SdkConstants.NS_RESOURCES,                          // xmlns
                null,                                               // default attributes
                1                                                   // target API level
                ),
        new TypeInfo("Animation",                                   // UI name
                "An XML file that describes an animation.",         // tooltip
                ResourceFolderType.ANIM,                            // folder type
                // TODO reuse constants if we ever make an editor with descriptors for animations
                new String[] {                                      // root seed
                    "set",          //$NON-NLS-1$
                    "alpha",        //$NON-NLS-1$
                    "scale",        //$NON-NLS-1$
                    "translate",    //$NON-NLS-1$
                    "rotate"        //$NON-NLS-1$
                    },
                "set",              //$NON-NLS-1$                   // default root
                null,                                               // xmlns
                null,                                               // default attributes
                1                                                   // target API level
                ),
    };

    /** Number of columns in the grid layout */
    final static int NUM_COL = 4;

    /** Absolute destination folder root, e.g. "/res/" */
    private static final String RES_FOLDER_ABS = AndroidConstants.WS_RESOURCES + AndroidConstants.WS_SEP;
    /** Relative destination folder root, e.g. "res/" */
    private static final String RES_FOLDER_REL = SdkConstants.FD_RESOURCES + AndroidConstants.WS_SEP;

    private IProject mProject;
    private Text mProjectTextField;
    private Button mProjectBrowseButton;
    private Text mFileNameTextField;
    private Text mWsFolderPathTextField;
    private Combo mRootElementCombo;
    private IStructuredSelection mInitialSelection;
    private ConfigurationSelector mConfigSelector;
    private FolderConfiguration mTempConfig = new FolderConfiguration();
    private boolean mInternalWsFolderPathUpdate;
    private boolean mInternalTypeUpdate;
    private boolean mInternalConfigSelectorUpdate;
    private ProjectChooserHelper mProjectChooserHelper;
    private ITargetChangeListener mSdkTargetChangeListener;

    private TypeInfo mCurrentTypeInfo;

    // --- UI creation ---

    /**
     * Constructs a new {@link NewXmlFileCreationPage}.
     * <p/>
     * Called by {@link NewXmlFileWizard#createMainPage()}.
     */
    protected NewXmlFileCreationPage(String pageName) {
        super(pageName);
        setPageComplete(false);
    }

    public void setInitialSelection(IStructuredSelection initialSelection) {
        mInitialSelection = initialSelection;
    }

    /**
     * Called by the parent Wizard to create the UI for this Wizard Page.
     *
     * {@inheritDoc}
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        initializeDialogUnits(parent);

        composite.setLayout(new GridLayout(NUM_COL, false /*makeColumnsEqualWidth*/));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createProjectGroup(composite);
        createTypeGroup(composite);
        createRootGroup(composite);

        // Show description the first time
        setErrorMessage(null);
        setMessage(null);
        setControl(composite);

        // Update state the first time
        initializeFromSelection(mInitialSelection);
        initializeRootValues();
        enableTypesBasedOnApi();
        if (mCurrentTypeInfo != null) {
            updateRootCombo(mCurrentTypeInfo);
        }
        installTargetChangeListener();
        validatePage();
    }

    private void installTargetChangeListener() {
        mSdkTargetChangeListener = new ITargetChangeListener() {
            public void onProjectTargetChange(IProject changedProject) {
                // If this is the current project, force it to reload its data
                if (changedProject != null && changedProject == mProject) {
                    changeProject(mProject);
                }
            }

            public void onTargetsLoaded() {
                // Reload the current project, if any, in case its target has changed.
                if (mProject != null) {
                    changeProject(mProject);
                }
            }
        };

        AdtPlugin.getDefault().addTargetListener(mSdkTargetChangeListener);
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
     * Returns the target project or null.
     */
    public IProject getProject() {
        return mProject;
    }

    /**
     * Returns the destination filename or an empty string.
     */
    public String getFileName() {
        return mFileNameTextField == null ? "" : mFileNameTextField.getText();         //$NON-NLS-1$
    }

    /**
     * Returns the destination folder path relative to the project or an empty string.
     */
    public String getWsFolderPath() {
        return mWsFolderPathTextField == null ? "" : mWsFolderPathTextField.getText(); //$NON-NLS-1$
    }


    /**
     * Returns an {@link IFile} on the destination file.
     * <p/>
     * Uses {@link #getProject()}, {@link #getWsFolderPath()} and {@link #getFileName()}.
     * <p/>
     * Returns null if the project, filename or folder are invalid and the destination file
     * cannot be determined.
     * <p/>
     * The {@link IFile} is a resource. There might or might not be an actual real file.
     */
    public IFile getDestinationFile() {
        IProject project = getProject();
        String wsFolderPath = getWsFolderPath();
        String fileName = getFileName();
        if (project != null && wsFolderPath.length() > 0 && fileName.length() > 0) {
            IPath dest = new Path(wsFolderPath).append(fileName);
            IFile file = project.getFile(dest);
            return file;
        }
        return null;
    }

    /**
     * Returns the {@link TypeInfo} for the currently selected type radio button.
     * Returns null if no radio button is selected.
     *
     * @return A {@link TypeInfo} or null.
     */
    public TypeInfo getSelectedType() {
        TypeInfo type = null;
        for (TypeInfo ti : sTypes) {
            if (ti.getWidget().getSelection()) {
                type = ti;
                break;
            }
        }
        return type;
    }

    /**
     * Returns the selected root element string, if any.
     *
     * @return The selected root element string or null.
     */
    public String getRootElement() {
        int index = mRootElementCombo.getSelectionIndex();
        if (index >= 0) {
            return mRootElementCombo.getItem(index);
        }
        return null;
    }

    // --- UI creation ---

    /**
     * Helper method to create a new GridData with an horizontal span.
     *
     * @param horizSpan The number of cells for the horizontal span.
     * @return A new GridData with the horizontal span.
     */
    private GridData newGridData(int horizSpan) {
        GridData gd = new GridData();
        gd.horizontalSpan = horizSpan;
        return gd;
    }

    /**
     * Helper method to create a new GridData with an horizontal span and a style.
     *
     * @param horizSpan The number of cells for the horizontal span.
     * @param style The style, e.g. {@link GridData#FILL_HORIZONTAL}
     * @return A new GridData with the horizontal span and the style.
     */
    private GridData newGridData(int horizSpan, int style) {
        GridData gd = new GridData(style);
        gd.horizontalSpan = horizSpan;
        return gd;
    }

    /**
     * Helper method that creates an empty cell in the parent composite.
     *
     * @param parent The parent composite.
     */
    private void emptyCell(Composite parent) {
        new Label(parent, SWT.NONE);
    }

    /**
     * Pads the parent with empty cells to match the number of columns of the parent grid.
     *
     * @param parent A grid layout with NUM_COL columns
     * @param col The current number of columns used.
     * @return 0, the new number of columns used, for convenience.
     */
    private int padWithEmptyCells(Composite parent, int col) {
        for (; col < NUM_COL; ++col) {
            emptyCell(parent);
        }
        col = 0;
        return col;
    }

    /**
     * Creates the project & filename fields.
     * <p/>
     * The parent must be a GridLayout with NUM_COL colums.
     */
    private void createProjectGroup(Composite parent) {
        int col = 0;

        // project name
        String tooltip = "The Android Project where the new resource file will be created.";
        Label label = new Label(parent, SWT.NONE);
        label.setText("Project");
        label.setToolTipText(tooltip);
        ++col;

        mProjectTextField = new Text(parent, SWT.BORDER);
        mProjectTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mProjectTextField.setToolTipText(tooltip);
        mProjectTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onProjectFieldUpdated();
            }
        });
        ++col;

        mProjectBrowseButton = new Button(parent, SWT.NONE);
        mProjectBrowseButton.setText("Browse...");
        mProjectBrowseButton.setToolTipText("Allows you to select the Android project to modify.");
        mProjectBrowseButton.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               onProjectBrowse();
            }
        });
        mProjectChooserHelper = new ProjectChooserHelper(parent.getShell());
        ++col;

        col = padWithEmptyCells(parent, col);

        // file name
        tooltip = "The name of the resource file to create.";
        label = new Label(parent, SWT.NONE);
        label.setText("File");
        label.setToolTipText(tooltip);
        ++col;

        mFileNameTextField = new Text(parent, SWT.BORDER);
        mFileNameTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFileNameTextField.setToolTipText(tooltip);
        mFileNameTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validatePage();
            }
        });
        ++col;

        padWithEmptyCells(parent, col);
    }

    /**
     * Creates the type field, {@link ConfigurationSelector} and the folder field.
     * <p/>
     * The parent must be a GridLayout with NUM_COL colums.
     */
    private void createTypeGroup(Composite parent) {
        // separator
        Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(newGridData(NUM_COL, GridData.GRAB_HORIZONTAL));

        // label before type radios
        label = new Label(parent, SWT.NONE);
        label.setText("What type of resource would you like to create?");
        label.setLayoutData(newGridData(NUM_COL));

        // display the types on three columns of radio buttons.
        emptyCell(parent);
        Composite grid = new Composite(parent, SWT.NONE);
        padWithEmptyCells(parent, 2);

        grid.setLayout(new GridLayout(NUM_COL, true /*makeColumnsEqualWidth*/));

        SelectionListener radioListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // single-click. Only do something if activated.
                if (e.getSource() instanceof Button) {
                    onRadioTypeUpdated((Button) e.getSource());
                }
            }
        };

        int n = sTypes.length;
        int num_lines = (n + NUM_COL/2) / NUM_COL;
        for (int line = 0, k = 0; line < num_lines; line++) {
            for (int i = 0; i < NUM_COL; i++, k++) {
                if (k < n) {
                    TypeInfo type = sTypes[k];
                    Button radio = new Button(grid, SWT.RADIO);
                    type.setWidget(radio);
                    radio.setSelection(false);
                    radio.setText(type.getUiName());
                    radio.setToolTipText(type.getTooltip());
                    radio.addSelectionListener(radioListener);
                } else {
                    emptyCell(grid);
                }
            }
        }

        // label before configuration selector
        label = new Label(parent, SWT.NONE);
        label.setText("What type of resource configuration would you like?");
        label.setLayoutData(newGridData(NUM_COL));

        // configuration selector
        emptyCell(parent);
        mConfigSelector = new ConfigurationSelector(parent);
        GridData gd = newGridData(2, GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gd.widthHint = ConfigurationSelector.WIDTH_HINT;
        gd.heightHint = ConfigurationSelector.HEIGHT_HINT;
        mConfigSelector.setLayoutData(gd);
        mConfigSelector.setOnChangeListener(new onConfigSelectorUpdated());
        emptyCell(parent);

        // folder name
        String tooltip = "The folder where the file will be generated, relative to the project.";
        label = new Label(parent, SWT.NONE);
        label.setText("Folder");
        label.setToolTipText(tooltip);

        mWsFolderPathTextField = new Text(parent, SWT.BORDER);
        mWsFolderPathTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mWsFolderPathTextField.setToolTipText(tooltip);
        mWsFolderPathTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onWsFolderPathUpdated();
            }
        });
    }

    /**
     * Creates the root element combo.
     * <p/>
     * The parent must be a GridLayout with NUM_COL colums.
     */
    private void createRootGroup(Composite parent) {
        // separator
        Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(newGridData(NUM_COL, GridData.GRAB_HORIZONTAL));

        // label before the root combo
        String tooltip = "The root element to create in the XML file.";
        label = new Label(parent, SWT.NONE);
        label.setText("Select the root element for the XML file:");
        label.setLayoutData(newGridData(NUM_COL));
        label.setToolTipText(tooltip);

        // root combo
        emptyCell(parent);

        mRootElementCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        mRootElementCombo.setEnabled(false);
        mRootElementCombo.select(0);
        mRootElementCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mRootElementCombo.setToolTipText(tooltip);

        padWithEmptyCells(parent, 2);
    }

    /**
     * Called by {@link NewXmlFileWizard} to initialize the page with the selection
     * received by the wizard -- typically the current user workbench selection.
     * <p/>
     * Things we expect to find out from the selection:
     * <ul>
     * <li>The project name, valid if it's an android nature.</li>
     * <li>The current folder, valid if it's a folder under /res</li>
     * <li>An existing filename, in which case the user will be asked whether to override it.</li>
     * <ul>
     *
     * @param selection The selection when the wizard was initiated.
     */
    private void initializeFromSelection(IStructuredSelection selection) {
        if (selection == null) {
            return;
        }

        // Find the best match in the element list. In case there are multiple selected elements
        // select the one that provides the most information and assign them a score,
        // e.g. project=1 + folder=2 + file=4.
        IProject targetProject = null;
        String targetWsFolderPath = null;
        String targetFileName = null;
        int targetScore = 0;
        for (Object element : selection.toList()) {
            if (element instanceof IAdaptable) {
                IResource res = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
                IProject project = res != null ? res.getProject() : null;

                // Is this an Android project?
                try {
                    if (project == null || !project.hasNature(AndroidConstants.NATURE)) {
                        continue;
                    }
                } catch (CoreException e) {
                    // checking the nature failed, ignore this resource
                    continue;
                }

                int score = 1; // we have a valid project at least

                IPath wsFolderPath = null;
                String fileName = null;
                if (res.getType() == IResource.FOLDER) {
                    wsFolderPath = res.getProjectRelativePath();
                } else if (res.getType() == IResource.FILE) {
                    fileName = res.getName();
                    wsFolderPath = res.getParent().getProjectRelativePath();
                }

                // Disregard this folder selection if it doesn't point to /res/something
                if (wsFolderPath != null &&
                        wsFolderPath.segmentCount() > 1 &&
                        SdkConstants.FD_RESOURCES.equals(wsFolderPath.segment(0))) {
                    score += 2;
                } else {
                    wsFolderPath = null;
                    fileName = null;
                }

                score += fileName != null ? 4 : 0;

                if (score > targetScore) {
                    targetScore = score;
                    targetProject = project;
                    targetWsFolderPath = wsFolderPath != null ? wsFolderPath.toString() : null;
                    targetFileName = fileName;
                }
            }
        }

        // Now set the UI accordingly
        if (targetScore > 0) {
            mProject = targetProject;
            mProjectTextField.setText(targetProject != null ? targetProject.getName() : ""); //$NON-NLS-1$
            mFileNameTextField.setText(targetFileName != null ? targetFileName : ""); //$NON-NLS-1$
            mWsFolderPathTextField.setText(targetWsFolderPath != null ? targetWsFolderPath : ""); //$NON-NLS-1$
        }
    }

    /**
     * Initialize the root values of the type infos based on the current framework values.
     */
    private void initializeRootValues() {
        for (TypeInfo type : sTypes) {
            // Clear all the roots for this type
            ArrayList<String> roots = type.getRoots();
            if (roots.size() > 0) {
                roots.clear();
            }

            // depending of the type of the seed, initialize the root in different ways
            Object rootSeed = type.getRootSeed();

            if (rootSeed instanceof String) {
                // The seed is a single string, Add it as-is.
                roots.add((String) rootSeed);
            } else if (rootSeed instanceof String[]) {
                // The seed is an array of strings. Add them as-is.
                for (String value : (String[]) rootSeed) {
                    roots.add(value);
                }
            } else if (rootSeed instanceof Integer && mProject != null) {
                // The seed is a descriptor reference defined in AndroidTargetData.DESCRIPTOR_*
                // In this case add all the children element descriptors defined, recursively,
                // and avoid infinite recursion by keeping track of what has already been added.

                // Note: if project is null, the root list will be empty since it has been
                // cleared above.

                // get the AndroidTargetData from the project
                IAndroidTarget target = null;
                AndroidTargetData data = null;

                target = Sdk.getCurrent().getTarget(mProject);
                if (target == null) {
                    // A project should have a target. The target can be missing if the project
                    // is an old project for which a target hasn't been affected or if the
                    // target no longer exists in this SDK. Simply log the error and dismiss.

                    AdtPlugin.log(IStatus.INFO,
                            "NewXmlFile wizard: no platform target for project %s",  //$NON-NLS-1$
                            mProject.getName());
                    continue;
                } else {
                    data = Sdk.getCurrent().getTargetData(target);

                    if (data == null) {
                        // We should have both a target and its data.
                        // However if the wizard is invoked whilst the platform is still being
                        // loaded we can end up in a weird case where we have a target but it
                        // doesn't have any data yet.
                        // Lets log a warning and silently ignore this root.

                        AdtPlugin.log(IStatus.INFO,
                              "NewXmlFile wizard: no data for target %s, project %s",  //$NON-NLS-1$
                              target.getName(), mProject.getName());
                        continue;
                    }
                }

                IDescriptorProvider provider = data.getDescriptorProvider((Integer)rootSeed);
                ElementDescriptor descriptor = provider.getDescriptor();
                if (descriptor != null) {
                    HashSet<ElementDescriptor> visited = new HashSet<ElementDescriptor>();
                    initRootElementDescriptor(roots, descriptor, visited);
                }

                // Sort alphabetically.
                Collections.sort(roots);
            }
        }
    }

    /**
     * Helper method to recursively insert all XML names for the given {@link ElementDescriptor}
     * into the roots array list. Keeps track of visited nodes to avoid infinite recursion.
     * Also avoids inserting the top {@link DocumentDescriptor} which is generally synthetic
     * and not a valid root element.
     */
    private void initRootElementDescriptor(ArrayList<String> roots,
            ElementDescriptor desc, HashSet<ElementDescriptor> visited) {
        if (!(desc instanceof DocumentDescriptor)) {
            String xmlName = desc.getXmlName();
            if (xmlName != null && xmlName.length() > 0) {
                roots.add(xmlName);
            }
        }

        visited.add(desc);

        for (ElementDescriptor child : desc.getChildren()) {
            if (!visited.contains(child)) {
                initRootElementDescriptor(roots, child, visited);
            }
        }
    }

    /**
     * Callback called when the user edits the project text field.
     */
    private void onProjectFieldUpdated() {
        String project = mProjectTextField.getText();

        // Is this a valid project?
        IJavaProject[] projects = mProjectChooserHelper.getAndroidProjects(null /*javaModel*/);
        IProject found = null;
        for (IJavaProject p : projects) {
            if (p.getProject().getName().equals(project)) {
                found = p.getProject();
                break;
            }
        }

        if (found != mProject) {
            changeProject(found);
        }
    }

    /**
     * Callback called when the user uses the "Browse Projects" button.
     */
    private void onProjectBrowse() {
        IJavaProject p = mProjectChooserHelper.chooseJavaProject(mProjectTextField.getText());
        if (p != null) {
            changeProject(p.getProject());
            mProjectTextField.setText(mProject.getName());
        }
    }

    /**
     * Changes mProject to the given new project and update the UI accordingly.
     * <p/>
     * Note that this does not check if the new project is the same as the current one
     * on purpose, which allows a project to be updated when its target has changed or
     * when targets are loaded in the background.
     */
    private void changeProject(IProject newProject) {
        mProject = newProject;

        // enable types based on new API level
        enableTypesBasedOnApi();

        // update the Type with the new descriptors.
        initializeRootValues();

        // update the combo
        updateRootCombo(getSelectedType());

        validatePage();
    }

    /**
     * Callback called when the Folder text field is changed, either programmatically
     * or by the user.
     */
    private void onWsFolderPathUpdated() {
        if (mInternalWsFolderPathUpdate) {
            return;
        }

        String wsFolderPath = mWsFolderPathTextField.getText();

        // This is a custom path, we need to sanitize it.
        // First it should start with "/res/". Then we need to make sure there are no
        // relative paths, things like "../" or "./" or even "//".
        wsFolderPath = wsFolderPath.replaceAll("/+\\.\\./+|/+\\./+|//+|\\\\+|^/+", "/");  //$NON-NLS-1$ //$NON-NLS-2$
        wsFolderPath = wsFolderPath.replaceAll("^\\.\\./+|^\\./+", "");                   //$NON-NLS-1$ //$NON-NLS-2$
        wsFolderPath = wsFolderPath.replaceAll("/+\\.\\.$|/+\\.$|/+$", "");               //$NON-NLS-1$ //$NON-NLS-2$

        ArrayList<TypeInfo> matches = new ArrayList<TypeInfo>();

        // We get "res/foo" from selections relative to the project when we want a "/res/foo" path.
        if (wsFolderPath.startsWith(RES_FOLDER_REL)) {
            wsFolderPath = RES_FOLDER_ABS + wsFolderPath.substring(RES_FOLDER_REL.length());

            mInternalWsFolderPathUpdate = true;
            mWsFolderPathTextField.setText(wsFolderPath);
            mInternalWsFolderPathUpdate = false;
        }

        if (wsFolderPath.startsWith(RES_FOLDER_ABS)) {
            wsFolderPath = wsFolderPath.substring(RES_FOLDER_ABS.length());

            int pos = wsFolderPath.indexOf(AndroidConstants.WS_SEP_CHAR);
            if (pos >= 0) {
                wsFolderPath = wsFolderPath.substring(0, pos);
            }

            String[] folderSegments = wsFolderPath.split(FolderConfiguration.QUALIFIER_SEP);

            if (folderSegments.length > 0) {
                String folderName = folderSegments[0];

                // update config selector
                mInternalConfigSelectorUpdate = true;
                mConfigSelector.setConfiguration(folderSegments);
                mInternalConfigSelectorUpdate = false;

                boolean selected = false;
                for (TypeInfo type : sTypes) {
                    if (type.getResFolderName().equals(folderName)) {
                        matches.add(type);
                        selected |= type.getWidget().getSelection();
                    }
                }

                if (matches.size() == 1) {
                    // If there's only one match, select it if it's not already selected
                    if (!selected) {
                        selectType(matches.get(0));
                    }
                } else if (matches.size() > 1) {
                    // There are multiple type candidates for this folder. This can happen
                    // for /res/xml for example. Check to see if one of them is currently
                    // selected. If yes, leave the selection unchanged. If not, deselect all type.
                    if (!selected) {
                        selectType(null);
                    }
                } else {
                    // Nothing valid was selected.
                    selectType(null);
                }
            }
        }

        validatePage();
    }

    /**
     * Callback called when one of the type radio button is changed.
     *
     * @param typeWidget The type radio button that changed.
     */
    private void onRadioTypeUpdated(Button typeWidget) {
        // Do nothing if this is an internal modification or if the widget has been
        // de-selected.
        if (mInternalTypeUpdate || !typeWidget.getSelection()) {
            return;
        }

        // Find type info that has just been enabled.
        TypeInfo type = null;
        for (TypeInfo ti : sTypes) {
            if (ti.getWidget() == typeWidget) {
                type = ti;
                break;
            }
        }

        if (type == null) {
            return;
        }

        // update the combo

        updateRootCombo(type);

        // update the folder path

        String wsFolderPath = mWsFolderPathTextField.getText();
        String newPath = null;

        mConfigSelector.getConfiguration(mTempConfig);
        ResourceQualifier qual = mTempConfig.getInvalidQualifier();
        if (qual == null) {
            // The configuration is valid. Reformat the folder path using the canonical
            // value from the configuration.

            newPath = RES_FOLDER_ABS + mTempConfig.getFolderName(type.getResFolderType());
        } else {
            // The configuration is invalid. We still update the path but this time
            // do it manually on the string.
            if (wsFolderPath.startsWith(RES_FOLDER_ABS)) {
                wsFolderPath.replaceFirst(
                        "^(" + RES_FOLDER_ABS +")[^-]*(.*)",         //$NON-NLS-1$ //$NON-NLS-2$
                        "\\1" + type.getResFolderName() + "\\2");   //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                newPath = RES_FOLDER_ABS + mTempConfig.getFolderName(type.getResFolderType());
            }
        }

        if (newPath != null && !newPath.equals(wsFolderPath)) {
            mInternalWsFolderPathUpdate = true;
            mWsFolderPathTextField.setText(newPath);
            mInternalWsFolderPathUpdate = false;
        }

        validatePage();
    }

    /**
     * Helper method that fills the values of the "root element" combo box based
     * on the currently selected type radio button. Also disables the combo is there's
     * only one choice. Always select the first root element for the given type.
     *
     * @param type The currently selected {@link TypeInfo}. Cannot be null.
     */
    private void updateRootCombo(TypeInfo type) {
        // reset all the values in the combo
        mRootElementCombo.removeAll();

        if (type != null) {
            // get the list of roots. The list can be empty but not null.
            ArrayList<String> roots = type.getRoots();

            // enable the combo if there's more than one choice
            mRootElementCombo.setEnabled(roots != null && roots.size() > 1);

            for (String root : roots) {
                mRootElementCombo.add(root);
            }

            int index = 0; // default is to select the first one
            String defaultRoot = type.getDefaultRoot();
            if (defaultRoot != null) {
                index = roots.indexOf(defaultRoot);
            }
            mRootElementCombo.select(index < 0 ? 0 : index);
        }
    }

    /**
     * Callback called when the configuration has changed in the {@link ConfigurationSelector}.
     */
    private class onConfigSelectorUpdated implements Runnable {
        public void run() {
            if (mInternalConfigSelectorUpdate) {
                return;
            }

            TypeInfo type = getSelectedType();

            if (type != null) {
                mConfigSelector.getConfiguration(mTempConfig);
                StringBuffer sb = new StringBuffer(RES_FOLDER_ABS);
                sb.append(mTempConfig.getFolderName(type.getResFolderType()));

                mInternalWsFolderPathUpdate = true;
                mWsFolderPathTextField.setText(sb.toString());
                mInternalWsFolderPathUpdate = false;

                validatePage();
            }
        }
    }

    /**
     * Helper method to select on of the type radio buttons.
     *
     * @param type The TypeInfo matching the radio button to selected or null to deselect them all.
     */
    private void selectType(TypeInfo type) {
        if (type == null || !type.getWidget().getSelection()) {
            mInternalTypeUpdate = true;
            mCurrentTypeInfo = type;
            for (TypeInfo type2 : sTypes) {
                type2.getWidget().setSelection(type2 == type);
            }
            updateRootCombo(type);
            mInternalTypeUpdate = false;
        }
    }

    /**
     * Helper method to enable the type radio buttons depending on the current API level.
     * <p/>
     * A type radio button is enabled either if:
     * - if mProject is null, API level 1 is considered valid
     * - if mProject is !null, the project->target->API must be >= to the type's API level.
     */
    private void enableTypesBasedOnApi() {

        IAndroidTarget target = mProject != null ? Sdk.getCurrent().getTarget(mProject) : null;
        int currentApiLevel = 1;
        if (target != null) {
            currentApiLevel = target.getVersion().getApiLevel();
        }

        for (TypeInfo type : sTypes) {
            type.getWidget().setEnabled(type.getTargetApiLevel() <= currentApiLevel);
        }
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;
        String warning = null;

        // -- validate project
        if (getProject() == null) {
            error = "Please select an Android project.";
        }

        // -- validate filename
        if (error == null) {
            String fileName = getFileName();
            if (fileName == null || fileName.length() == 0) {
                error = "A destination file name is required.";
            } else if (!fileName.endsWith(AndroidConstants.DOT_XML)) {
                error = String.format("The filename must end with %1$s.", AndroidConstants.DOT_XML);
            }
        }

        // -- validate type
        if (error == null) {
            TypeInfo type = getSelectedType();

            if (type == null) {
                error = "One of the types must be selected (e.g. layout, values, etc.)";
            }
        }

        // -- validate type API level
        if (error == null) {
            IAndroidTarget target = Sdk.getCurrent().getTarget(mProject);
            int currentApiLevel = 1;
            if (target != null) {
                currentApiLevel = target.getVersion().getApiLevel();
            }

            TypeInfo type = getSelectedType();

            if (type.getTargetApiLevel() > currentApiLevel) {
                error = "The API level of the selected type (e.g. AppWidget, etc.) is not " +
                        "compatible with the API level of the project.";
            }
        }

        // -- validate folder configuration
        if (error == null) {
            ConfigurationState state = mConfigSelector.getState();
            if (state == ConfigurationState.INVALID_CONFIG) {
                ResourceQualifier qual = mConfigSelector.getInvalidQualifier();
                if (qual != null) {
                    error = String.format("The qualifier '%1$s' is invalid in the folder configuration.",
                            qual.getName());
                }
            } else if (state == ConfigurationState.REGION_WITHOUT_LANGUAGE) {
                error = "The Region qualifier requires the Language qualifier.";
            }
        }

        // -- validate generated path
        if (error == null) {
            String wsFolderPath = getWsFolderPath();
            if (!wsFolderPath.startsWith(RES_FOLDER_ABS)) {
                error = String.format("Target folder must start with %1$s.", RES_FOLDER_ABS);
            }
        }

        // -- validate destination file doesn't exist
        if (error == null) {
            IFile file = getDestinationFile();
            if (file != null && file.exists()) {
                warning = "The destination file already exists";
            }
        }

        // -- update UI & enable finish if there's no error
        setPageComplete(error == null);
        if (error != null) {
            setMessage(error, WizardPage.ERROR);
        } else if (warning != null) {
            setMessage(warning, WizardPage.WARNING);
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

}
