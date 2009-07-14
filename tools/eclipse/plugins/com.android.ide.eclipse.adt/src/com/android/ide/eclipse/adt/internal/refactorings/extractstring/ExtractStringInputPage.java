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

package com.android.ide.eclipse.adt.internal.refactorings.extractstring;


import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see ExtractStringRefactoring
 */
class ExtractStringInputPage extends UserInputWizardPage implements IWizardPage {

    /** Last res file path used, shared across the session instances but specific to the
     *  current project. The default for unknown projects is {@link #DEFAULT_RES_FILE_PATH}. */
    private static HashMap<String, String> sLastResFilePath = new HashMap<String, String>();

    /** The project where the user selection happened. */
    private final IProject mProject;

    /** Text field where the user enters the new ID to be generated or replaced with. */
    private Combo mStringIdCombo;
    /** Text field where the user enters the new string value. */
    private Text mStringValueField;
    /** The configuration selector, to select the resource path of the XML file. */
    private ConfigurationSelector mConfigSelector;
    /** The combo to display the existing XML files or enter a new one. */
    private Combo mResFileCombo;

    /** Regex pattern to read a valid res XML file path. It checks that the are 2 folders and
     *  a leaf file name ending with .xml */
    private static final Pattern RES_XML_FILE_REGEX = Pattern.compile(
                                     "/res/[a-z][a-zA-Z0-9_-]+/[^.]+\\.xml");  //$NON-NLS-1$
    /** Absolute destination folder root, e.g. "/res/" */
    private static final String RES_FOLDER_ABS =
        AndroidConstants.WS_RESOURCES + AndroidConstants.WS_SEP;
    /** Relative destination folder root, e.g. "res/" */
    private static final String RES_FOLDER_REL =
        SdkConstants.FD_RESOURCES + AndroidConstants.WS_SEP;

    private static final String DEFAULT_RES_FILE_PATH = "/res/values/strings.xml";  //$NON-NLS-1$

    private XmlStringFileHelper mXmlHelper = new XmlStringFileHelper();


    public ExtractStringInputPage(IProject project) {
        super("ExtractStringInputPage");  //$NON-NLS-1$
        mProject = project;
    }

    /**
     * Create the UI for the refactoring wizard.
     * <p/>
     * Note that at that point the initial conditions have been checked in
     * {@link ExtractStringRefactoring}.
     */
    public void createControl(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        content.setLayout(layout);

        createStringGroup(content);
        createResFileGroup(content);

        validatePage();
        setControl(content);
    }

    /**
     * Creates the top group with the field to replace which string and by what
     * and by which options.
     *
     * @param content A composite with a 1-column grid layout
     */
    public void createStringGroup(Composite content) {

        final ExtractStringRefactoring ref = getOurRefactoring();

        Group group = new Group(content, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (ref.getMode() == ExtractStringRefactoring.Mode.EDIT_SOURCE) {
            group.setText("String Replacement");
        } else {
            group.setText("New String");
        }

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);

        // line: Textfield for string value (based on selection, if any)

        Label label = new Label(group, SWT.NONE);
        label.setText("&String");

        String selectedString = ref.getTokenString();

        mStringValueField = new Text(group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        mStringValueField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStringValueField.setText(selectedString != null ? selectedString : "");  //$NON-NLS-1$

        ref.setNewStringValue(mStringValueField.getText());

        mStringValueField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validatePage();
            }
        });


        // TODO provide an option to replace all occurences of this string instead of
        // just the one.

        // line : Textfield for new ID

        label = new Label(group, SWT.NONE);
        if (ref.getMode() == ExtractStringRefactoring.Mode.EDIT_SOURCE) {
            label.setText("&Replace by R.string.");
        } else if (ref.getMode() == ExtractStringRefactoring.Mode.SELECT_NEW_ID) {
            label.setText("New &R.string.");
        } else {
            label.setText("ID &R.string.");
        }

        mStringIdCombo = new Combo(group, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.DROP_DOWN);
        mStringIdCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStringIdCombo.setText(guessId(selectedString));
        mStringIdCombo.forceFocus();

        ref.setNewStringId(mStringIdCombo.getText().trim());

        mStringIdCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validatePage();
            }
        });
        mStringIdCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validatePage();
            }
        });
    }

    /**
     * Creates the lower group with the fields to choose the resource confirmation and
     * the target XML file.
     *
     * @param content A composite with a 1-column grid layout
     */
    private void createResFileGroup(Composite content) {

        Group group = new Group(content, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("XML resource to edit");

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);

        // line: selection of the res config

        Label label;
        label = new Label(group, SWT.NONE);
        label.setText("&Configuration:");

        mConfigSelector = new ConfigurationSelector(group);
        GridData gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        gd.horizontalSpan = 2;
        gd.widthHint = ConfigurationSelector.WIDTH_HINT;
        gd.heightHint = ConfigurationSelector.HEIGHT_HINT;
        mConfigSelector.setLayoutData(gd);
        OnConfigSelectorUpdated onConfigSelectorUpdated = new OnConfigSelectorUpdated();
        mConfigSelector.setOnChangeListener(onConfigSelectorUpdated);

        // line: selection of the output file

        label = new Label(group, SWT.NONE);
        label.setText("Resource &file:");

        mResFileCombo = new Combo(group, SWT.DROP_DOWN);
        mResFileCombo.select(0);
        mResFileCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mResFileCombo.addModifyListener(onConfigSelectorUpdated);

        // set output file name to the last one used

        String projPath = mProject.getFullPath().toPortableString();
        String filePath = sLastResFilePath.get(projPath);

        mResFileCombo.setText(filePath != null ? filePath : DEFAULT_RES_FILE_PATH);
        onConfigSelectorUpdated.run();
    }

    /**
     * Utility method to guess a suitable new XML ID based on the selected string.
     */
    private String guessId(String text) {
        if (text == null) {
            return "";  //$NON-NLS-1$
        }

        // make lower case
        text = text.toLowerCase();

        // everything not alphanumeric becomes an underscore
        text = text.replaceAll("[^a-zA-Z0-9]+", "_");  //$NON-NLS-1$ //$NON-NLS-2$

        // the id must be a proper Java identifier, so it can't start with a number
        if (text.length() > 0 && !Character.isJavaIdentifierStart(text.charAt(0))) {
            text = "_" + text;  //$NON-NLS-1$
        }
        return text;
    }

    /**
     * Returns the {@link ExtractStringRefactoring} instance used by this wizard page.
     */
    private ExtractStringRefactoring getOurRefactoring() {
        return (ExtractStringRefactoring) getRefactoring();
    }

    /**
     * Validates fields of the wizard input page. Displays errors as appropriate and
     * enable the "Next" button (or not) by calling {@link #setPageComplete(boolean)}.
     *
     * If validation succeeds, this udpates the text id & value in the refactoring object.
     *
     * @return True if the page has been positively validated. It may still have warnings.
     */
    private boolean validatePage() {
        boolean success = true;

        ExtractStringRefactoring ref = getOurRefactoring();

        // Analyze fatal errors.

        String text = mStringIdCombo.getText().trim();
        if (text == null || text.length() < 1) {
            setErrorMessage("Please provide a resource ID.");
            success = false;
        } else {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean ok = i == 0 ?
                        Character.isJavaIdentifierStart(c) :
                        Character.isJavaIdentifierPart(c);
                if (!ok) {
                    setErrorMessage(String.format(
                            "The resource ID must be a valid Java identifier. The character %1$c at position %2$d is not acceptable.",
                            c, i+1));
                    success = false;
                    break;
                }
            }

            // update the field in the refactoring object in case of success
            if (success) {
                ref.setNewStringId(text);
            }
        }

        String resFile = mResFileCombo.getText();
        if (success) {
            if (resFile == null || resFile.length() == 0) {
                setErrorMessage("A resource file name is required.");
                success = false;
            } else if (!RES_XML_FILE_REGEX.matcher(resFile).matches()) {
                setErrorMessage("The XML file name is not valid.");
                success = false;
            }
        }

        // Analyze info & warnings.

        if (success) {
            setErrorMessage(null);

            ref.setTargetFile(resFile);
            sLastResFilePath.put(mProject.getFullPath().toPortableString(), resFile);

            String idValue = mXmlHelper.valueOfStringId(mProject, resFile, text);
            if (idValue != null) {
                String msg = String.format("%1$s already contains a string ID '%2$s' with value '%3$s'.",
                        resFile,
                        text,
                        idValue);
                if (ref.getMode() == ExtractStringRefactoring.Mode.SELECT_NEW_ID) {
                    setErrorMessage(msg);
                    success = false;
                } else {
                    setMessage(msg, WizardPage.WARNING);
                }
            } else if (mProject.findMember(resFile) == null) {
                setMessage(
                        String.format("File %2$s does not exist and will be created.",
                                text, resFile),
                        WizardPage.INFORMATION);
            } else {
                setMessage(null);
            }
        }

        if (success) {
            // Also update the text value in case of success.
            ref.setNewStringValue(mStringValueField.getText());
        }

        setPageComplete(success);
        return success;
    }

    private void updateStringValueCombo() {
        String resFile = mResFileCombo.getText();
        Map<String, String> ids = mXmlHelper.getResIdsForFile(mProject, resFile);

        // get the current text from the combo, to make sure we don't change it
        String currText = mStringIdCombo.getText();

        // erase the choices and fill with the given ids
        mStringIdCombo.removeAll();
        mStringIdCombo.setItems(ids.keySet().toArray(new String[ids.size()]));

        // set the current text to preserve it in case it changed
        if (!currText.equals(mStringIdCombo.getText())) {
            mStringIdCombo.setText(currText);
        }
    }

    public class OnConfigSelectorUpdated implements Runnable, ModifyListener {

        /** Regex pattern to parse a valid res path: it reads (/res/folder-name/)+(filename). */
        private final Pattern mPathRegex = Pattern.compile(
            "(/res/[a-z][a-zA-Z0-9_-]+/)(.+)");  //$NON-NLS-1$

        /** Temporary config object used to retrieve the Config Selector value. */
        private FolderConfiguration mTempConfig = new FolderConfiguration();

        private HashMap<String, TreeSet<String>> mFolderCache =
            new HashMap<String, TreeSet<String>>();
        private String mLastFolderUsedInCombo = null;
        private boolean mInternalConfigChange;
        private boolean mInternalFileComboChange;

        /**
         * Callback invoked when the {@link ConfigurationSelector} has been changed.
         * <p/>
         * The callback does the following:
         * <ul>
         * <li> Examine the current file name to retrieve the XML filename, if any.
         * <li> Recompute the path based on the configuration selector (e.g. /res/values-fr/).
         * <li> Examine the path to retrieve all the files in it. Keep those in a local cache.
         * <li> If the XML filename from step 1 is not in the file list, it's a custom file name.
         *      Insert it and sort it.
         * <li> Re-populate the file combo with all the choices.
         * <li> Select the original XML file.
         */
        public void run() {
            if (mInternalConfigChange) {
                return;
            }

            // get current leafname, if any
            String leafName = "";  //$NON-NLS-1$
            String currPath = mResFileCombo.getText();
            Matcher m = mPathRegex.matcher(currPath);
            if (m.matches()) {
                // Note: groups 1 and 2 cannot be null.
                leafName = m.group(2);
                currPath = m.group(1);
            } else {
                // There was a path but it was invalid. Ignore it.
                currPath = "";  //$NON-NLS-1$
            }

            // recreate the res path from the current configuration
            mConfigSelector.getConfiguration(mTempConfig);
            StringBuffer sb = new StringBuffer(RES_FOLDER_ABS);
            sb.append(mTempConfig.getFolderName(ResourceFolderType.VALUES));
            sb.append('/');

            String newPath = sb.toString();
            if (newPath.equals(currPath) && newPath.equals(mLastFolderUsedInCombo)) {
                // Path has not changed. No need to reload.
                return;
            }

            // Get all the files at the new path

            TreeSet<String> filePaths = mFolderCache.get(newPath);

            if (filePaths == null) {
                filePaths = new TreeSet<String>();

                IFolder folder = mProject.getFolder(newPath);
                if (folder != null && folder.exists()) {
                    try {
                        for (IResource res : folder.members()) {
                            String name = res.getName();
                            if (res.getType() == IResource.FILE && name.endsWith(".xml")) {
                                filePaths.add(newPath + name);
                            }
                        }
                    } catch (CoreException e) {
                        // Ignore.
                    }
                }

                mFolderCache.put(newPath, filePaths);
            }

            currPath = newPath + leafName;
            if (leafName.length() > 0 && !filePaths.contains(currPath)) {
                filePaths.add(currPath);
            }

            // Fill the combo
            try {
                mInternalFileComboChange = true;

                mResFileCombo.removeAll();

                for (String filePath : filePaths) {
                    mResFileCombo.add(filePath);
                }

                int index = -1;
                if (leafName.length() > 0) {
                    index = mResFileCombo.indexOf(currPath);
                    if (index >= 0) {
                        mResFileCombo.select(index);
                    }
                }

                if (index == -1) {
                    mResFileCombo.setText(currPath);
                }

                mLastFolderUsedInCombo = newPath;

            } finally {
                mInternalFileComboChange = false;
            }

            // finally validate the whole page
            updateStringValueCombo();
            validatePage();
        }

        /**
         * Callback invoked when {@link ExtractStringInputPage#mResFileCombo} has been
         * modified.
         */
        public void modifyText(ModifyEvent e) {
            if (mInternalFileComboChange) {
                return;
            }

            String wsFolderPath = mResFileCombo.getText();

            // This is a custom path, we need to sanitize it.
            // First it should start with "/res/". Then we need to make sure there are no
            // relative paths, things like "../" or "./" or even "//".
            wsFolderPath = wsFolderPath.replaceAll("/+\\.\\./+|/+\\./+|//+|\\\\+|^/+", "/");  //$NON-NLS-1$ //$NON-NLS-2$
            wsFolderPath = wsFolderPath.replaceAll("^\\.\\./+|^\\./+", "");                   //$NON-NLS-1$ //$NON-NLS-2$
            wsFolderPath = wsFolderPath.replaceAll("/+\\.\\.$|/+\\.$|/+$", "");               //$NON-NLS-1$ //$NON-NLS-2$

            // We get "res/foo" from selections relative to the project when we want a "/res/foo" path.
            if (wsFolderPath.startsWith(RES_FOLDER_REL)) {
                wsFolderPath = RES_FOLDER_ABS + wsFolderPath.substring(RES_FOLDER_REL.length());

                mInternalFileComboChange = true;
                mResFileCombo.setText(wsFolderPath);
                mInternalFileComboChange = false;
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

                    if (folderName != null && !folderName.equals(wsFolderPath)) {
                        // update config selector
                        mInternalConfigChange = true;
                        mConfigSelector.setConfiguration(folderSegments);
                        mInternalConfigChange = false;
                    }
                }
            }

            updateStringValueCombo();
            validatePage();
        }
    }

}
