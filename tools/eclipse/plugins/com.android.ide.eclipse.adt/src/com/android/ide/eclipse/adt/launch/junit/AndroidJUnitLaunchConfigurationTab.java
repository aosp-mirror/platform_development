/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.ide.eclipse.adt.launch.junit;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.launch.MainLaunchConfigTab;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.common.project.ProjectChooserHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitMigrationDelegate;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.launcher.TestSelectionDialog;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import java.lang.reflect.InvocationTargetException;

/**
 * The launch config UI tab for Android JUnit
 * <p/>
 * Based on org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab
 */
@SuppressWarnings("restriction")
public class AndroidJUnitLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

    // Project UI widgets
    private Label mProjLabel;
    private Text mProjText;
    private Button mProjButton;
    
    // Test class UI widgets
    private Text mTestText;
    private Button mSearchButton;
    private String mOriginalTestMethodName;
    private Label mTestMethodLabel;
    private Text mContainerText;
    private IJavaElement mContainerElement;
    private final ILabelProvider mJavaElementLabelProvider = new JavaElementLabelProvider();

    private Button mContainerSearchButton;
    private Button mTestContainerRadioButton;
    private Button mTestRadioButton;
    private Label mTestLabel; 

    // Android specific members
    private Image mTabIcon = null;
    private Combo mInstrumentationCombo;
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final String TAG = "AndroidJUnitLaunchConfigurationTab"; //$NON-NLS-1$
    private String[] mInstrumentations = null;
    private InstrumentationRunnerValidator mInstrValidator = null;
    private ProjectChooserHelper mProjectChooserHelper;

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        mProjectChooserHelper = new ProjectChooserHelper(parent.getShell());

        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);

        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 3;
        comp.setLayout(topLayout);      
        
        createSingleTestSection(comp);
        createTestContainerSelectionGroup(comp);
        
        createSpacer(comp);
        
        createInstrumentationGroup(comp);

        createSpacer(comp);
        
        Dialog.applyDialogFont(comp);
        // TODO: add help link here when available
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), 
        //      IJUnitHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_JUNIT_MAIN_TAB);
        validatePage();
    }


    private void createSpacer(Composite comp) {
        Label label = new Label(comp, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        label.setLayoutData(gd);
    }
    
    private void createSingleTestSection(Composite comp) {
        mTestRadioButton = new Button(comp, SWT.RADIO);
        mTestRadioButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_oneTest); 
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        mTestRadioButton.setLayoutData(gd); 
        mTestRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mTestRadioButton.getSelection()) {
                    testModeChanged();
                }    
            }
        });
        
        mProjLabel = new Label(comp, SWT.NONE);
        mProjLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_project); 
        gd = new GridData();
        gd.horizontalIndent = 25;
        mProjLabel.setLayoutData(gd);
        
        mProjText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        mProjText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mProjText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                validatePage();
                updateLaunchConfigurationDialog();              
                mSearchButton.setEnabled(mTestRadioButton.getSelection() && 
                        mProjText.getText().length() > 0);
            }
        });
            
        mProjButton = new Button(comp, SWT.PUSH);
        mProjButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_browse); 
        mProjButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleProjectButtonSelected();
            }
        });
        setButtonGridData(mProjButton);
        
        mTestLabel = new Label(comp, SWT.NONE);
        gd = new GridData();
        gd.horizontalIndent = 25;
        mTestLabel.setLayoutData(gd);
        mTestLabel.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_test); 
        
    
        mTestText = new Text(comp, SWT.SINGLE | SWT.BORDER);
        mTestText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTestText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                validatePage();
                updateLaunchConfigurationDialog();
            }
        });
        
        mSearchButton = new Button(comp, SWT.PUSH);
        mSearchButton.setEnabled(mProjText.getText().length() > 0);
        mSearchButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_search); 
        mSearchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleSearchButtonSelected();
            }
        });
        setButtonGridData(mSearchButton);
        
        new Label(comp, SWT.NONE);
        
        mTestMethodLabel = new Label(comp, SWT.NONE);
        mTestMethodLabel.setText("");  //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 2;
        mTestMethodLabel.setLayoutData(gd);
    }

    private void createTestContainerSelectionGroup(Composite comp) {
        mTestContainerRadioButton = new Button(comp, SWT.RADIO);
        mTestContainerRadioButton.setText(
                "Run all tests in the selected project, or package"); 
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        mTestContainerRadioButton.setLayoutData(gd);
        mTestContainerRadioButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                if (mTestContainerRadioButton.getSelection()) {
                    testModeChanged();
                }
            }
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        mContainerText = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 25;
        gd.horizontalSpan = 2;
        mContainerText.setLayoutData(gd);
        mContainerText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

        mContainerSearchButton = new Button(comp, SWT.PUSH);
        mContainerSearchButton.setText(JUnitMessages.JUnitLaunchConfigurationTab_label_search); 
        mContainerSearchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent evt) {
                handleContainerSearchButtonSelected();
            }
        });
        setButtonGridData(mContainerSearchButton);  
    }
    
    private void createInstrumentationGroup(Composite comp) {
        Label loaderLabel = new Label(comp, SWT.NONE);
        loaderLabel.setText("Instrumentation runner:");
        GridData gd = new GridData();
        gd.horizontalIndent = 0;
        loaderLabel.setLayoutData(gd);
        
        mInstrumentationCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        mInstrumentationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mInstrumentationCombo.clearSelection();
        mInstrumentationCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validatePage();
                updateLaunchConfigurationDialog();
            }
        });
    }

    private void handleContainerSearchButtonSelected() {
        IJavaElement javaElement = chooseContainer(mContainerElement);
        if (javaElement != null) {
            setContainerElement(javaElement);
        }    
    }

    private void setContainerElement(IJavaElement javaElement) {
        mContainerElement = javaElement;
        mContainerText.setText(getPresentationName(javaElement));
        validatePage();
        updateLaunchConfigurationDialog();
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration config) {
        String projectName = updateProjectFromConfig(config);
        String containerHandle = EMPTY_STRING;
        try {
            containerHandle = config.getAttribute(
                    JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, EMPTY_STRING);
        } catch (CoreException ce) {
            // ignore
        }
        
        if (containerHandle.length() > 0) {
            updateTestContainerFromConfig(config);
        } else {
            updateTestTypeFromConfig(config);
        }    

        IProject proj = mProjectChooserHelper.getAndroidProject(projectName);
        loadInstrumentations(proj);
        updateInstrumentationFromConfig(config);
        
        validatePage();
    }

    private void updateInstrumentationFromConfig(ILaunchConfiguration config) {
        boolean found = false;
        try {
            String currentInstrumentation = config.getAttribute(
                    AndroidJUnitLaunchConfigDelegate.ATTR_INSTR_NAME, EMPTY_STRING);
            if (mInstrumentations != null) {
                // look for the name of the instrumentation in the combo.
                for (int i = 0; i < mInstrumentations.length; i++) {
                   if (currentInstrumentation.equals(mInstrumentations[i])) {
                       found = true;
                       mInstrumentationCombo.select(i);
                       break;
                    }
                }
            }
        } catch (CoreException ce) {
            // ignore
        }
        if (!found) {
            mInstrumentationCombo.clearSelection();
        }    
    }

    private String updateProjectFromConfig(ILaunchConfiguration config) {
        String projectName = EMPTY_STRING;
        try {
            projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    EMPTY_STRING);
        } catch (CoreException ce) {
            // ignore
        }
        mProjText.setText(projectName);
        return projectName;
    }

    private void updateTestTypeFromConfig(ILaunchConfiguration config) {
        String testTypeName = EMPTY_STRING;
        mOriginalTestMethodName = EMPTY_STRING;
        try {
            testTypeName = config.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
            mOriginalTestMethodName = config.getAttribute(
                    JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, ""); //$NON-NLS-1$
        } catch (CoreException ce) {
            // ignore
        }
        mTestRadioButton.setSelection(true);
        setEnableSingleTestGroup(true);
        setEnableContainerTestGroup(false);     
        mTestContainerRadioButton.setSelection(false);
        mTestText.setText(testTypeName);
        mContainerText.setText(EMPTY_STRING); 
        setTestMethodLabel(mOriginalTestMethodName);
    }

    private void setTestMethodLabel(String testMethodName) {
        if (!EMPTY_STRING.equals(testMethodName)) {
            mTestMethodLabel.setText(
                    JUnitMessages.JUnitLaunchConfigurationTab_label_method + 
                    mOriginalTestMethodName); 
        } else {
            mTestMethodLabel.setText(EMPTY_STRING);
        }
    }

    private void updateTestContainerFromConfig(ILaunchConfiguration config) {
        String containerHandle = EMPTY_STRING;
        IJavaElement containerElement = null;
        try {
            containerHandle = config.getAttribute(
                    JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, EMPTY_STRING);
            if (containerHandle.length() > 0) {
                containerElement = JavaCore.create(containerHandle);
            }
        } catch (CoreException ce) {
            // ignore
        }
        if (containerElement != null) {
            mContainerElement = containerElement;
        }    
        mTestContainerRadioButton.setSelection(true);
        setEnableSingleTestGroup(false);
        setEnableContainerTestGroup(true);              
        mTestRadioButton.setSelection(false);
        if (mContainerElement != null) {
            mContainerText.setText(getPresentationName(mContainerElement));
        }    
        mTestText.setText(EMPTY_STRING);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy config) {
        if (mTestContainerRadioButton.getSelection() && mContainerElement != null) {
            config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, 
                    mContainerElement.getJavaProject().getElementName());
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, 
                    mContainerElement.getHandleIdentifier());
            config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                    EMPTY_STRING);
             //workaround for Eclipse bug 65399
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
                    EMPTY_STRING);
        } else {
            config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    mProjText.getText());
            config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                    mTestText.getText());
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER,
                    EMPTY_STRING);
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
                    mOriginalTestMethodName);
        }
        try {
            mapResources(config);
        } catch (CoreException e) {
            // TODO: does the real error need to be extracted out of CoreException
            AdtPlugin.log(e, "Error occurred saving configuration"); //$NON-NLS-1$
        }
        AndroidJUnitLaunchConfigDelegate.setJUnitDefaults(config);
        
        config.setAttribute(AndroidJUnitLaunchConfigDelegate.ATTR_INSTR_NAME,
                getSelectedInstrumentation());
    }

    private void mapResources(ILaunchConfigurationWorkingCopy config)  throws CoreException {
        JUnitMigrationDelegate.mapResources(config);
    }   

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (mTabIcon != null) {
            mTabIcon.dispose();
            mTabIcon = null;
        }
        mJavaElementLabelProvider.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
     */
    @Override
    public Image getImage() {
        // reuse icon from the Android App Launch config tab
        if (mTabIcon == null) {
            mTabIcon = AdtPlugin.getImageLoader().loadImage(MainLaunchConfigTab.LAUNCH_TAB_IMAGE,
                    null);
        }
        return mTabIcon;
    }

    /**
     * Show a dialog that lists all main types
     */
    private void handleSearchButtonSelected() {
        Shell shell = getShell();

        IJavaProject javaProject = getJavaProject();

        IType[] types = new IType[0];
        boolean[] radioSetting = new boolean[2];
        try {
            // fix for Eclipse bug 66922 Wrong radio behaviour when switching
            // remember the selected radio button
            radioSetting[0] = mTestRadioButton.getSelection();
            radioSetting[1] = mTestContainerRadioButton.getSelection();
            
            types = TestSearchEngine.findTests(getLaunchConfigurationDialog(), javaProject,
                    getTestKind()); 
        } catch (InterruptedException e) {
            setErrorMessage(e.getMessage());
            return;
        } catch (InvocationTargetException e) {
            AdtPlugin.log(e.getTargetException(), "Error finding test types"); //$NON-NLS-1$
            return;
        } finally {
            mTestRadioButton.setSelection(radioSetting[0]);
            mTestContainerRadioButton.setSelection(radioSetting[1]);
        }

        SelectionDialog dialog = new TestSelectionDialog(shell, types);
        dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_testdialog_title); 
        dialog.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_testdialog_message); 
        if (dialog.open() == Window.CANCEL) {
            return;
        }

        Object[] results = dialog.getResult();
        if ((results == null) || (results.length < 1)) {
            return;
        }       
        IType type = (IType) results[0];

        if (type != null) {
            mTestText.setText(type.getFullyQualifiedName('.'));
            javaProject = type.getJavaProject();
            mProjText.setText(javaProject.getElementName());
        }
    }

    private ITestKind getTestKind() {
        // harddcode this to JUnit 3
        return TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT3_TEST_KIND_ID);
    }

    /**
     * Show a dialog that lets the user select a Android project.  This in turn provides
     * context for the main type, allowing the user to key a main type name, or
     * constraining the search for main types to the specified project.
     */
    private void handleProjectButtonSelected() {
        IJavaProject project = mProjectChooserHelper.chooseJavaProject(getProjectName());
        if (project == null) {
            return;
        }

        String projectName = project.getElementName();
        mProjText.setText(projectName);
        loadInstrumentations(project.getProject());   
    }

    /**
     * Return the IJavaProject corresponding to the project name in the project name
     * text field, or null if the text does not match a Android project name.
     */
    private IJavaProject getJavaProject() {
        String projectName = getProjectName();
        return getJavaModel().getJavaProject(projectName);      
    }

    /**
     * Returns the name of the currently specified project. Null if no project is selected.
     */
    private String getProjectName() {
        String projectName = mProjText.getText().trim();
        if (projectName.length() < 1) {
            return null;
        }
        return projectName;
    }

    /**
     * Convenience method to get the workspace root.
     */
    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    /**
     * Convenience method to get access to the java model.
     */
    private IJavaModel getJavaModel() {
        return JavaCore.create(getWorkspaceRoot());
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration config) {
        validatePage();
        return getErrorMessage() == null;
    }

    private void testModeChanged() {
        boolean isSingleTestMode = mTestRadioButton.getSelection();
        setEnableSingleTestGroup(isSingleTestMode);
        setEnableContainerTestGroup(!isSingleTestMode);
        if (!isSingleTestMode && mContainerText.getText().length() == 0) {
            String projText = mProjText.getText();
            if (Path.EMPTY.isValidSegment(projText)) {
                IJavaProject javaProject = getJavaModel().getJavaProject(projText);
                if (javaProject != null && javaProject.exists()) {
                    setContainerElement(javaProject);
                }    
            }
        }
        validatePage();
        updateLaunchConfigurationDialog();
    }

    private void validatePage() {       
        setErrorMessage(null);
        setMessage(null);

        if (mTestContainerRadioButton.getSelection()) {
            if (mContainerElement == null) {
                setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_noContainer);
                return;
            }
            validateJavaProject(mContainerElement.getJavaProject());
            return;
        }

        String projectName = mProjText.getText().trim();
        if (projectName.length() == 0) {
            setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_projectnotdefined);
            return;
        }

        IStatus status = ResourcesPlugin.getWorkspace().validatePath(IPath.SEPARATOR + projectName,
                IResource.PROJECT);
        if (!status.isOK() || !Path.ROOT.isValidSegment(projectName)) {
            setErrorMessage(Messages.format(
                    JUnitMessages.JUnitLaunchConfigurationTab_error_invalidProjectName, 
                    projectName));
            return;
        }

        IProject project = getWorkspaceRoot().getProject(projectName);
        if (!project.exists()) {
            setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_projectnotexists);
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        validateJavaProject(javaProject);

        try {
            if (!project.hasNature(AndroidConstants.NATURE)) {
                setErrorMessage("Specified project is not an Android project");
                return;
            }
            String className = mTestText.getText().trim();
            if (className.length() == 0) {
                setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_testnotdefined);
                return;
            }
            if (javaProject.findType(className) == null) {
                setErrorMessage(Messages.format(
                        JUnitMessages.JUnitLaunchConfigurationTab_error_test_class_not_found,
                        new String[] { className, projectName }));
                return;
            }          
        } catch (CoreException e) {
            AdtPlugin.log(e, "validatePage failed"); //$NON-NLS-1$
        }

        validateInstrumentation();
    }

    private void validateJavaProject(IJavaProject javaProject) {
        if (!TestSearchEngine.hasTestCaseType(javaProject)) {
            setErrorMessage(JUnitMessages.JUnitLaunchConfigurationTab_error_testcasenotonpath); 
            return;             
        }
    }

    private void validateInstrumentation() {
        String instrumentation = getSelectedInstrumentation();
        if (instrumentation == null) {
            setErrorMessage("Instrumentation runner not specified");
            return;
        }
        String result = mInstrValidator.validateInstrumentationRunner(instrumentation);
        if (result != InstrumentationRunnerValidator.INSTRUMENTATION_OK) {
            setErrorMessage(result);
            return;
        }
    }

    private String getSelectedInstrumentation() {
        int selectionIndex = mInstrumentationCombo.getSelectionIndex();
        if (mInstrumentations != null && selectionIndex >= 0 && 
                selectionIndex < mInstrumentations.length) {
            return mInstrumentations[selectionIndex];
        }
        return null;
    }

    private void setEnableContainerTestGroup(boolean enabled) {
        mContainerSearchButton.setEnabled(enabled);
        mContainerText.setEnabled(enabled);
    }

    private void setEnableSingleTestGroup(boolean enabled) {
        mProjLabel.setEnabled(enabled);
        mProjText.setEnabled(enabled);
        mProjButton.setEnabled(enabled);
        mTestLabel.setEnabled(enabled);
        mTestText.setEnabled(enabled);
        mSearchButton.setEnabled(enabled && mProjText.getText().length() > 0);
        mTestMethodLabel.setEnabled(enabled);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        IJavaElement javaElement = getContext();
        if (javaElement != null) {
            initializeJavaProject(javaElement, config);
        } else {
            // We set empty attributes for project & main type so that when one config is
            // compared to another, the existence of empty attributes doesn't cause an
            // incorrect result (the performApply() method can result in empty values
            // for these attributes being set on a config if there is nothing in the
            // corresponding text boxes)
            config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER,
                    EMPTY_STRING);
        }
        initializeTestAttributes(javaElement, config);
    }

    private void initializeTestAttributes(IJavaElement javaElement,
            ILaunchConfigurationWorkingCopy config) {
        if (javaElement != null && javaElement.getElementType() < IJavaElement.COMPILATION_UNIT) { 
            initializeTestContainer(javaElement, config);
        } else {
            initializeTestType(javaElement, config);
        }    
    }

    private void initializeTestContainer(IJavaElement javaElement,
            ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER,
                javaElement.getHandleIdentifier());
        initializeName(config, javaElement.getElementName());
    }

    private void initializeName(ILaunchConfigurationWorkingCopy config, String name) {
        if (name == null) {
            name = EMPTY_STRING;
        }
        if (name.length() > 0) {
            int index = name.lastIndexOf('.');
            if (index > 0) {
                name = name.substring(index + 1);
            }
            name = getLaunchConfigurationDialog().generateName(name);
            config.rename(name);
        }
    }

    /**
     * Sets the main type & name attributes on the working copy based on the IJavaElement
     */
    private void initializeTestType(IJavaElement javaElement,
            ILaunchConfigurationWorkingCopy config) {
        String name = EMPTY_STRING;
        String testKindId = null;
        try {
            // only do a search for compilation units or class files or source references
            if (javaElement instanceof ISourceReference) {
                ITestKind testKind = TestKindRegistry.getContainerTestKind(javaElement);
                testKindId = testKind.getId();

                IType[] types = TestSearchEngine.findTests(getLaunchConfigurationDialog(),
                        javaElement, testKind); 
                if ((types == null) || (types.length < 1)) {
                    return;
                }
                // Simply grab the first main type found in the searched element
                name = types[0].getFullyQualifiedName('.');
                
            }   
        } catch (InterruptedException ie) {
            // ignore
        } catch (InvocationTargetException ite) {
            // ignore
        }
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, name);
        if (testKindId != null) {
            config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND,
                    testKindId);
        }    
        initializeName(config, name);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return JUnitMessages.JUnitLaunchConfigurationTab_tab_label; 
    }

    @SuppressWarnings("unchecked")
    private IJavaElement chooseContainer(IJavaElement initElement) {
        Class[] acceptedClasses = new Class[] { IJavaProject.class,
                IPackageFragment.class };
        TypedElementSelectionValidator validator = new TypedElementSelectionValidator(
                acceptedClasses, false) {
            @Override
            public boolean isSelectedValid(Object element) {
                return true;
            }
        };

        acceptedClasses = new Class[] { IJavaModel.class, IPackageFragmentRoot.class,
                IJavaProject.class, IPackageFragment.class };
        ViewerFilter filter = new TypedViewerFilter(acceptedClasses) {
            @Override
            public boolean select(Viewer viewer, Object parent, Object element) {
                if (element instanceof IPackageFragmentRoot && 
                        ((IPackageFragmentRoot) element).isArchive()) {
                    return false;
                }
                try {
                    if (element instanceof IPackageFragment &&
                            !((IPackageFragment) element).hasChildren()) {
                        return false;
                    }
                } catch (JavaModelException e) {
                    return false;
                }
                return super.select(viewer, parent, element);
            }
        };      

        AndroidJavaElementContentProvider provider = new AndroidJavaElementContentProvider();
        ILabelProvider labelProvider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT); 
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), 
                labelProvider, provider);
        dialog.setValidator(validator);
        dialog.setComparator(new JavaElementComparator());
        dialog.setTitle(JUnitMessages.JUnitLaunchConfigurationTab_folderdialog_title);  
        dialog.setMessage(JUnitMessages.JUnitLaunchConfigurationTab_folderdialog_message);  
        dialog.addFilter(filter);
        dialog.setInput(JavaCore.create(getWorkspaceRoot()));
        dialog.setInitialSelection(initElement);
        dialog.setAllowMultiple(false);

        if (dialog.open() == Window.OK) {
            Object element = dialog.getFirstResult();
            return (IJavaElement) element;
        }
        return null;
    }

    private String getPresentationName(IJavaElement element) {
        return mJavaElementLabelProvider.getText(element);
    }

    /**
     * Returns the current Java element context from which to initialize
     * default settings, or <code>null</code> if none.
     * 
     * @return Java element context.
     */
    private IJavaElement getContext() {
        IWorkbenchWindow activeWorkbenchWindow =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return null;
        }
        IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
        if (page != null) {
            ISelection selection = page.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection ss = (IStructuredSelection) selection;
                if (!ss.isEmpty()) {
                    Object obj = ss.getFirstElement();
                    if (obj instanceof IJavaElement) {
                        return (IJavaElement) obj;
                    }
                    if (obj instanceof IResource) {
                        IJavaElement je = JavaCore.create((IResource) obj);
                        if (je == null) {
                            IProject pro = ((IResource) obj).getProject();
                            je = JavaCore.create(pro);
                        }
                        if (je != null) {
                            return je;
                        }
                    }
                }
            }
            IEditorPart part = page.getActiveEditor();
            if (part != null) {
                IEditorInput input = part.getEditorInput();
                return (IJavaElement) input.getAdapter(IJavaElement.class);
            }
        }
        return null;
    }

    private void initializeJavaProject(IJavaElement javaElement,
            ILaunchConfigurationWorkingCopy config) {
        IJavaProject javaProject = javaElement.getJavaProject();
        String name = null;
        if (javaProject != null && javaProject.exists()) {
            name = javaProject.getElementName();
        }
        config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
    }

    private void setButtonGridData(Button button) {
        GridData gridData = new GridData();
        button.setLayoutData(gridData);
        LayoutUtil.setButtonDimensionHint(button);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
     */
    @Override
    public String getId() {
        return "com.android.ide.eclipse.adt.launch.AndroidJUnitLaunchConfigurationTab"; //$NON-NLS-1$
    }

    /**
     * Loads the UI with the instrumentations of the specified project, and stores the
     * instrumentations in <code>mInstrumentations</code>.
     * 
     * @param project the {@link IProject} to load the instrumentations from.
     */
    private void loadInstrumentations(IProject project) {
        try {
        mInstrValidator = new InstrumentationRunnerValidator(project);
        mInstrumentations = (mInstrValidator == null ? null : 
            mInstrValidator.getInstrumentationNames());
        if (mInstrumentations != null) {
            mInstrumentationCombo.removeAll();
            for (String instrumentation : mInstrumentations) {
                mInstrumentationCombo.add(instrumentation);
            }
            // the selection will be set when we update the ui from the current
            // config object.
            return;
        }
        } catch (CoreException e) {
            AdtPlugin.logAndPrintError(e, TAG, "ERROR: Failed to get instrumentations for %1$s",
                    project.getName());
        }
        // if we reach this point, either project is null, or we got an exception during
        // the parsing. In either case, we empty the instrumentation list.
        mInstrValidator = null;
        mInstrumentations = null;
        mInstrumentationCombo.removeAll();
    }

    /**
     * Overrides the {@link StandardJavaElementContentProvider} to only display Android projects
     */
    private static class AndroidJavaElementContentProvider
            extends StandardJavaElementContentProvider {

        /**
         * Override parent to return only Android projects if at the root. Otherwise, use parent 
         * functionality.
         */
        @Override
        public Object[] getChildren(Object element) {
            if (element instanceof IJavaModel) {
                return BaseProjectHelper.getAndroidProjects((IJavaModel) element);
            }
            return super.getChildren(element);
        }
    }
}
