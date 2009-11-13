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

package com.android.sdkuilib.internal.widgets;

import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdklib.internal.avd.HardwareProperties.HardwareProperty;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.widgets.AvdSelector.SdkLog;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * AVD creator dialog.
 *
 * TODO:
 * - use SdkTargetSelector instead of Combo
 * - tooltips on widgets.
 *
 */
final class AvdCreationDialog extends GridDialog {

    private final AvdManager mAvdManager;
    private final TreeMap<String, IAndroidTarget> mCurrentTargets =
        new TreeMap<String, IAndroidTarget>();

    private final Map<String, HardwareProperty> mHardwareMap;
    private final Map<String, String> mProperties = new HashMap<String, String>();
    // a list of user-edited properties.
    private final ArrayList<String> mEditedProperties = new ArrayList<String>();

    private Text mAvdName;
    private Combo mTargetCombo;

    private Button mSdCardSizeRadio;
    private Text mSdCardSize;
    private Combo mSdCardSizeCombo;

    private Text mSdCardFile;
    private Button mBrowseSdCard;
    private Button mSdCardFileRadio;

    private Button mSkinListRadio;
    private Combo mSkinCombo;

    private Button mSkinSizeRadio;
    private Text mSkinSizeWidth;
    private Text mSkinSizeHeight;

    private TableViewer mHardwareViewer;
    private Button mDeleteHardwareProp;

    private Button mForceCreation;
    private Button mOkButton;
    private Label mStatusIcon;
    private Label mStatusLabel;
    private Composite mStatusComposite;
    private final ImageFactory mImageFactory;

    /**
     * {@link VerifyListener} for {@link Text} widgets that should only contains numbers.
     */
    private final VerifyListener mDigitVerifier = new VerifyListener() {
        public void verifyText(VerifyEvent event) {
            int count = event.text.length();
            for (int i = 0 ; i < count ; i++) {
                char c = event.text.charAt(i);
                if (c < '0' || c > '9') {
                    event.doit = false;
                    return;
                }
            }
        }
    };

    /**
     * Callback when the AVD name is changed.
     * Enables the force checkbox if the name is a duplicate.
     */
    private class CreateNameModifyListener implements ModifyListener {
        public void modifyText(ModifyEvent e) {
            String name = mAvdName.getText().trim();
            AvdInfo avdMatch = mAvdManager.getAvd(name, false /*validAvdOnly*/);
            if (avdMatch != null) {
                mForceCreation.setEnabled(true);
            } else {
                mForceCreation.setEnabled(false);
                mForceCreation.setSelection(false);
            }

            validatePage();
        }
    }

    /**
     * {@link ModifyListener} used for live-validation of the fields content.
     */
    private class ValidateListener extends SelectionAdapter implements ModifyListener {
        public void modifyText(ModifyEvent e) {
            validatePage();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            validatePage();
        }
    }

    protected AvdCreationDialog(Shell parentShell, AvdManager avdManager,
            ImageFactory imageFactory) {
        super(parentShell, 2, false);
        mAvdManager = avdManager;
        mImageFactory = imageFactory;

        File hardwareDefs = new File (avdManager.getSdkManager().getLocation() + File.separator +
                SdkConstants.OS_SDK_TOOLS_LIB_FOLDER, SdkConstants.FN_HARDWARE_INI);
        mHardwareMap = HardwareProperties.parseHardwareDefinitions(
                hardwareDefs, null /*sdkLog*/);
    }

    @Override
    public void create() {
        super.create();

        Point p = getShell().getSize();
        if (p.x < 400) {
            p.x = 400;
        }
        getShell().setSize(p);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getShell().setText("Create new AVD");

        mOkButton = getButton(IDialogConstants.OK_ID);
        validatePage();

        return control;
    }

    @Override
    public void createDialogContent(final Composite parent) {
        GridData gd;
        GridLayout gl;

        Label label = new Label(parent, SWT.NONE);
        label.setText("Name:");

        mAvdName = new Text(parent, SWT.BORDER);
        mAvdName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mAvdName.addModifyListener(new CreateNameModifyListener());

        label = new Label(parent, SWT.NONE);
        label.setText("Target:");

        mTargetCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mTargetCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTargetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                reloadSkinCombo();
                validatePage();
            }
        });

        // --- sd card group
        label = new Label(parent, SWT.NONE);
        label.setText("SD Card:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group sdCardGroup = new Group(parent, SWT.NONE);
        sdCardGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdCardGroup.setLayout(new GridLayout(3, false));

        mSdCardSizeRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardSizeRadio.setText("Size:");
        mSdCardSizeRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean sizeMode = mSdCardSizeRadio.getSelection();
                enableSdCardWidgets(sizeMode);
                validatePage();
            }
        });

        ValidateListener validateListener = new ValidateListener();

        mSdCardSize = new Text(sdCardGroup, SWT.BORDER);
        mSdCardSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardSize.addVerifyListener(mDigitVerifier);

        mSdCardSizeCombo = new Combo(sdCardGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        mSdCardSizeCombo.add("KiB");
        mSdCardSizeCombo.add("MiB");
        mSdCardSizeCombo.select(1);

        mSdCardFileRadio = new Button(sdCardGroup, SWT.RADIO);
        mSdCardFileRadio.setText("File:");

        mSdCardFile = new Text(sdCardGroup, SWT.BORDER);
        mSdCardFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSdCardFile.addModifyListener(validateListener);

        mBrowseSdCard = new Button(sdCardGroup, SWT.PUSH);
        mBrowseSdCard.setText("Browse...");
        mBrowseSdCard.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent arg0) {
               onBrowseSdCard();
               validatePage();
            }
        });

        mSdCardSizeRadio.setSelection(true);
        enableSdCardWidgets(true);

        // --- skin group
        label = new Label(parent, SWT.NONE);
        label.setText("Skin:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group skinGroup = new Group(parent, SWT.NONE);
        skinGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        skinGroup.setLayout(new GridLayout(4, false));

        mSkinListRadio = new Button(skinGroup, SWT.RADIO);
        mSkinListRadio.setText("Built-in:");
        mSkinListRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean listMode = mSkinListRadio.getSelection();
                enableSkinWidgets(listMode);
                validatePage();
            }
        });

        mSkinCombo = new Combo(skinGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
        mSkinCombo.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 3;
        mSkinCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                // get the skin info
                loadSkin();
            }
        });

        mSkinSizeRadio = new Button(skinGroup, SWT.RADIO);
        mSkinSizeRadio.setText("Resolution:");

        mSkinSizeWidth = new Text(skinGroup, SWT.BORDER);
        mSkinSizeWidth.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSkinSizeWidth.addVerifyListener(mDigitVerifier);
        mSkinSizeWidth.addModifyListener(validateListener);

        new Label(skinGroup, SWT.NONE).setText("x");

        mSkinSizeHeight = new Text(skinGroup, SWT.BORDER);
        mSkinSizeHeight.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mSkinSizeHeight.addVerifyListener(mDigitVerifier);
        mSkinSizeHeight.addModifyListener(validateListener);

        mSkinListRadio.setSelection(true);
        enableSkinWidgets(true);

        // --- hardware group
        label = new Label(parent, SWT.NONE);
        label.setText("Hardware:");
        label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));

        final Group hwGroup = new Group(parent, SWT.NONE);
        hwGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hwGroup.setLayout(new GridLayout(2, false));

        createHardwareTable(hwGroup);

        // composite for the side buttons
        Composite hwButtons = new Composite(hwGroup, SWT.NONE);
        hwButtons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        hwButtons.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;

        Button b = new Button(hwButtons, SWT.PUSH | SWT.FLAT);
        b.setText("New...");
        b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        b.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                HardwarePropertyChooser dialog = new HardwarePropertyChooser(parent.getShell(),
                        mHardwareMap, mProperties.keySet());
                if (dialog.open() == Window.OK) {
                    HardwareProperty choice = dialog.getProperty();
                    if (choice != null) {
                        mProperties.put(choice.getName(), choice.getDefault());
                        mHardwareViewer.refresh();
                    }
                }
            }
        });
        mDeleteHardwareProp = new Button(hwButtons, SWT.PUSH | SWT.FLAT);
        mDeleteHardwareProp.setText("Delete");
        mDeleteHardwareProp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDeleteHardwareProp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                ISelection selection = mHardwareViewer.getSelection();
                if (selection instanceof IStructuredSelection) {
                    String hwName = (String)((IStructuredSelection)selection).getFirstElement();
                    mProperties.remove(hwName);
                    mHardwareViewer.refresh();
                }
            }
        });
        mDeleteHardwareProp.setEnabled(false);

        // --- end hardware group

        mForceCreation = new Button(parent, SWT.CHECK);
        mForceCreation.setText("Force create");
        mForceCreation.setToolTipText("Select this to override any existing AVD");
        mForceCreation.setLayoutData(new GridData(GridData.END, GridData.CENTER,
                true, false, 2, 1));
        mForceCreation.setEnabled(false);
        mForceCreation.addSelectionListener(validateListener);

        // add a separator to separate from the ok/cancel button
        label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 3, 1));

        // add stuff for the error display
        mStatusComposite = new Composite(parent, SWT.NONE);
        mStatusComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true, false, 3, 1));
        mStatusComposite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mStatusIcon = new Label(mStatusComposite, SWT.NONE);
        mStatusIcon.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING,
                false, false));
        mStatusLabel = new Label(mStatusComposite, SWT.NONE);
        mStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStatusLabel.setText(" \n "); //$NON-NLS-1$

        reloadTargetCombo();
    }

    /**
     * Creates the UI for the hardware properties table.
     * This creates the {@link Table}, and several viewers ({@link TableViewer},
     * {@link TableViewerColumn}) and adds edit support for the 2nd column
     */
    private void createHardwareTable(Composite parent) {
        final Table hardwareTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        gd.widthHint = 200;
        gd.heightHint = 100;
        hardwareTable.setLayoutData(gd);
        hardwareTable.setHeaderVisible(true);
        hardwareTable.setLinesVisible(true);

        // -- Table viewer
        mHardwareViewer = new TableViewer(hardwareTable);
        mHardwareViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                // it's a single selection mode, we can just access the selection index
                // from the table directly.
                mDeleteHardwareProp.setEnabled(hardwareTable.getSelectionIndex() != -1);
            }
        });

        // only a content provider. Use viewers per column below (for editing support)
        mHardwareViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                // we can just ignore this. we just use mProperties directly.
            }

            public Object[] getElements(Object arg0) {
                return mProperties.keySet().toArray();
            }

            public void dispose() {
                // pass
            }
        });

        // -- column 1: prop abstract name
        TableColumn col1 = new TableColumn(hardwareTable, SWT.LEFT);
        col1.setText("Property");
        col1.setWidth(150);
        TableViewerColumn tvc1 = new TableViewerColumn(mHardwareViewer, col1);
        tvc1.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                HardwareProperty prop = mHardwareMap.get(cell.getElement());
                cell.setText(prop != null ? prop.getAbstract() : "");
            }
        });

        // -- column 2: prop value
        TableColumn col2 = new TableColumn(hardwareTable, SWT.LEFT);
        col2.setText("Value");
        col2.setWidth(50);
        TableViewerColumn tvc2 = new TableViewerColumn(mHardwareViewer, col2);
        tvc2.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                String value = mProperties.get(cell.getElement());
                cell.setText(value != null ? value : "");
            }
        });

        // add editing support to the 2nd column
        tvc2.setEditingSupport(new EditingSupport(mHardwareViewer) {
            @Override
            protected void setValue(Object element, Object value) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                switch (property.getType()) {
                    case INTEGER:
                        mProperties.put((String)element, (String)value);
                        break;
                    case DISKSIZE:
                        if (HardwareProperties.DISKSIZE_PATTERN.matcher((String)value).matches()) {
                            mProperties.put((String)element, (String)value);
                        }
                        break;
                    case BOOLEAN:
                        int index = (Integer)value;
                        mProperties.put((String)element, HardwareProperties.BOOLEAN_VALUES[index]);
                        break;
                }
                mHardwareViewer.refresh(element);
            }

            @Override
            protected Object getValue(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                String value = mProperties.get(hardwareName);
                switch (property.getType()) {
                    case INTEGER:
                        // intended fall-through.
                    case DISKSIZE:
                        return value;
                    case BOOLEAN:
                        return HardwareProperties.getBooleanValueIndex(value);
                }

                return null;
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                switch (property.getType()) {
                    // TODO: custom TextCellEditor that restrict input.
                    case INTEGER:
                        // intended fall-through.
                    case DISKSIZE:
                        return new TextCellEditor(hardwareTable);
                    case BOOLEAN:
                        return new ComboBoxCellEditor(hardwareTable,
                                HardwareProperties.BOOLEAN_VALUES,
                                SWT.READ_ONLY | SWT.DROP_DOWN);
                }
                return null;
            }

            @Override
            protected boolean canEdit(Object element) {
                String hardwareName = (String)element;
                HardwareProperty property = mHardwareMap.get(hardwareName);
                return property != null;
            }
        });


        mHardwareViewer.setInput(mProperties);
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == IDialogConstants.OK_ID) {
            label = "Create AVD";
        }

        return super.createButton(parent, id, label, defaultButton);
    }

    @Override
    protected void okPressed() {
        if (createAvd()) {
            super.okPressed();
        }
    }

    /**
     * Enable or disable the sd card widgets.
     * @param sizeMode if true the size-based widgets are to be enabled, and the file-based ones
     * disabled.
     */
    private void enableSdCardWidgets(boolean sizeMode) {
        mSdCardSize.setEnabled(sizeMode);
        mSdCardSizeCombo.setEnabled(sizeMode);

        mSdCardFile.setEnabled(!sizeMode);
        mBrowseSdCard.setEnabled(!sizeMode);
    }

    /**
     * Enable or disable the skin widgets.
     * @param listMode if true the list-based widgets are to be enabled, and the size-based ones
     * disabled.
     */
    private void enableSkinWidgets(boolean listMode) {
        mSkinCombo.setEnabled(listMode);

        mSkinSizeWidth.setEnabled(!listMode);
        mSkinSizeHeight.setEnabled(!listMode);
    }


    private void onBrowseSdCard() {
        FileDialog dlg = new FileDialog(getContents().getShell(), SWT.OPEN);
        dlg.setText("Choose SD Card image file.");

        String fileName = dlg.open();
        if (fileName != null) {
            mSdCardFile.setText(fileName);
        }
    }

    private void reloadTargetCombo() {
        String selected = null;
        int index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mTargetCombo.getItem(index);
        }

        mCurrentTargets.clear();
        mTargetCombo.removeAll();

        boolean found = false;
        index = -1;

        SdkManager sdkManager = mAvdManager.getSdkManager();
        if (sdkManager != null) {
            for (IAndroidTarget target : sdkManager.getTargets()) {
                String name;
                if (target.isPlatform()) {
                    name = String.format("%s - API Level %s",
                            target.getName(),
                            target.getVersion().getApiString());
                } else {
                    name = String.format("%s (%s) - API Level %s",
                            target.getName(),
                            target.getVendor(),
                            target.getVersion().getApiString());
                }
                mCurrentTargets.put(name, target);
                mTargetCombo.add(name);
                if (!found) {
                    index++;
                    found = name.equals(selected);
                }
            }
        }

        mTargetCombo.setEnabled(mCurrentTargets.size() > 0);

        if (found) {
            mTargetCombo.select(index);
        }

        reloadSkinCombo();
    }

    private void reloadSkinCombo() {
        String selected = null;
        int index = mSkinCombo.getSelectionIndex();
        if (index >= 0) {
            selected = mSkinCombo.getItem(index);
        }

        mSkinCombo.removeAll();
        mSkinCombo.setEnabled(false);

        index = mTargetCombo.getSelectionIndex();
        if (index >= 0) {

            String targetName = mTargetCombo.getItem(index);

            boolean found = false;
            IAndroidTarget target = mCurrentTargets.get(targetName);
            if (target != null) {
                mSkinCombo.add(String.format("Default (%s)", target.getDefaultSkin()));

                index = -1;
                for (String skin : target.getSkins()) {
                    mSkinCombo.add(skin);
                    if (!found) {
                        index++;
                        found = skin.equals(selected);
                    }
                }

                mSkinCombo.setEnabled(true);

                if (found) {
                    mSkinCombo.select(index);
                } else {
                    mSkinCombo.select(0);  // default
                    loadSkin();
                }
            }
        }
    }

    /**
     * Validates the fields, displays errors and warnings.
     * Enables the finish button if there are no errors.
     */
    private void validatePage() {
        String error = null;

        // Validate AVD name
        String avdName = mAvdName.getText().trim();
        boolean hasAvdName = avdName.length() > 0;
        if (hasAvdName && !AvdManager.RE_AVD_NAME.matcher(avdName).matches()) {
            error = String.format(
                "AVD name '%1$s' contains invalid characters.\nAllowed characters are: %2$s",
                avdName, AvdManager.CHARS_AVD_NAME);
        }

        // Validate target
        if (hasAvdName && error == null && mTargetCombo.getSelectionIndex() < 0) {
            error = "A target must be selected in order to create an AVD.";
        }

        // Validate SDCard path or value
        if (error == null) {
            // get the mode. We only need to check the file since the
            // verifier on the size Text will prevent invalid input
            boolean sdcardFileMode = mSdCardFileRadio.getSelection();
            if (sdcardFileMode) {
                String sdName = mSdCardFile.getText().trim();
                if (sdName.length() > 0 && !new File(sdName).isFile()) {
                    error = "SD Card path isn't valid.";
                }
            }
        }

        // validate the skin
        if (error == null) {
            // get the mode, we should only check if it's in size mode since
            // the built-in list mode is always valid.
            if (mSkinSizeRadio.getSelection()) {
                // need both with and heigh to be non null
                String width = mSkinSizeWidth.getText();   // no need for trim, since the verifier
                String height = mSkinSizeHeight.getText(); // rejects non digit.

                if (width.length() == 0 || height.length() == 0) {
                    error = "Skin size is incorrect.\nBoth dimensions must be > 0";
                }
            }
        }

        // Check for duplicate AVD name
        if (hasAvdName && error == null) {
            AvdInfo avdMatch = mAvdManager.getAvd(avdName, false /*validAvdOnly*/);
            if (avdMatch != null && !mForceCreation.getSelection()) {
                error = String.format(
                        "The AVD name '%s' is already used.\n" +
                        "Check \"Force create\" to override existing AVD.",
                        avdName);
            }
        }

        // Validate the create button
        boolean can_create = hasAvdName && error == null;
        if (can_create) {
            can_create &= mTargetCombo.getSelectionIndex() >= 0;
        }
        mOkButton.setEnabled(can_create);

        // -- update UI
        if (error != null) {
            mStatusIcon.setImage(mImageFactory.getImageByName("reject_icon16.png"));
            mStatusLabel.setText(error);
        } else {
            mStatusIcon.setImage(null);
            mStatusLabel.setText(" \n "); //$NON-NLS-1$
        }

        mStatusComposite.pack(true);
    }

    private void loadSkin() {
        int targetIndex = mTargetCombo.getSelectionIndex();
        if (targetIndex < 0) {
            return;
        }

        // resolve the target.
        String targetName = mTargetCombo.getItem(targetIndex);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return;
        }

        // get the skin name
        String skinName = null;
        int skinIndex = mSkinCombo.getSelectionIndex();
        if (skinIndex < 0) {
            return;
        } else if (skinIndex == 0) { // default skin for the target
            skinName = target.getDefaultSkin();
        } else {
            skinName = mSkinCombo.getItem(skinIndex);
        }

        // load the skin properties
        String path = target.getPath(IAndroidTarget.SKINS);
        File skin = new File(path, skinName);
        if (skin.isDirectory() == false && target.isPlatform() == false) {
            // it's possible the skin is in the parent target
            path = target.getParent().getPath(IAndroidTarget.SKINS);
            skin = new File(path, skinName);
        }

        if (skin.isDirectory() == false) {
            return;
        }

        // now get the hardware.ini from the add-on (if applicable) and from the skin
        // (if applicable)
        HashMap<String, String> hardwareValues = new HashMap<String, String>();
        if (target.isPlatform() == false) {
            File targetHardwareFile = new File(target.getLocation(), AvdManager.HARDWARE_INI);
            if (targetHardwareFile.isFile()) {
                Map<String, String> targetHardwareConfig = SdkManager.parsePropertyFile(
                        targetHardwareFile, null /*log*/);
                if (targetHardwareConfig != null) {
                    hardwareValues.putAll(targetHardwareConfig);
                }
            }
        }

        // from the skin
        File skinHardwareFile = new File(skin, AvdManager.HARDWARE_INI);
        if (skinHardwareFile.isFile()) {
            Map<String, String> skinHardwareConfig = SdkManager.parsePropertyFile(
                    skinHardwareFile, null /*log*/);
            if (skinHardwareConfig != null) {
                hardwareValues.putAll(skinHardwareConfig);
            }
        }

        // now set those values in the list of properties for the AVD.
        // We just check that none of those properties have been edited by the user yet.
        for (Entry<String, String> entry : hardwareValues.entrySet()) {
            if (mEditedProperties.contains(entry.getKey()) == false) {
                mProperties.put(entry.getKey(), entry.getValue());
            }
        }

        mHardwareViewer.refresh();
    }

    /**
     * Creates an AVD from the values in the UI. Called when the user presses the OK button.
     */
    private boolean createAvd() {
        String avdName = mAvdName.getText().trim();
        int targetIndex = mTargetCombo.getSelectionIndex();

        // quick check on the name and the target selection
        if (avdName.length() == 0 || targetIndex < 0) {
            return false;
        }

        // resolve the target.
        String targetName = mTargetCombo.getItem(targetIndex);
        IAndroidTarget target = mCurrentTargets.get(targetName);
        if (target == null) {
            return false;
        }

        // get the SD card data from the UI.
        String sdName = null;
        if (mSdCardSizeRadio.getSelection()) {
            // size mode
            String value = mSdCardSize.getText().trim();
            if (value.length() > 0) {
                sdName = value;
                // add the unit
                switch (mSdCardSizeCombo.getSelectionIndex()) {
                    case 0:
                        sdName += "K";
                        break;
                    case 1:
                        sdName += "M";
                        break;
                    default:
                        // shouldn't be here
                        assert false;
                }
            }
        } else {
            // file mode.
            sdName = mSdCardFile.getText().trim();
        }

        // get the Skin data from the UI
        String skinName = null;
        if (mSkinListRadio.getSelection()) {
            // built-in list provides the skin
            int skinIndex = mSkinCombo.getSelectionIndex();
            if (skinIndex > 0) {
                // index 0 is the default, we don't use it
                skinName = mSkinCombo.getItem(skinIndex);
            }
        } else {
            // size mode. get both size and writes it as a skin name
            // thanks to validatePage() we know the content of the fields is correct
            skinName = mSkinSizeWidth.getText() + "x" + mSkinSizeHeight.getText(); //$NON-NLS-1$
        }

        SdkLog log = new SdkLog(String.format("Result of creating AVD '%s':", avdName),
                getContents().getDisplay());

        File avdFolder;
        try {
            avdFolder = new File(
                    AndroidLocation.getFolder() + AndroidLocation.FOLDER_AVD,
                    avdName + AvdManager.AVD_FOLDER_EXTENSION);
        } catch (AndroidLocationException e) {
            return false;
        }

        boolean force = mForceCreation.getSelection();

        boolean success = false;
        AvdInfo avdInfo = mAvdManager.createAvd(
                avdFolder,
                avdName,
                target,
                skinName,
                sdName,
                mProperties,
                force,
                log);

        success = avdInfo != null;

        log.displayResult(success);
        return success;
    }
}
