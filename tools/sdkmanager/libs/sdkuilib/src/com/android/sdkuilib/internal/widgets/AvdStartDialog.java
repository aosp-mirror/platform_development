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

import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdkuilib.internal.repository.SettingsController;
import com.android.sdkuilib.ui.GridDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dialog dealing with emulator launch options. The following options are supported:
 * <ul>
 * <li>-wipe-data</li>
 * <li>-scale</li>
 * </ul>
 *
 * Values are stored (in the class as static field) to be reused while the app is still running.
 * The Monitor dpi is stored in the settings if availabe.
 */
final class AvdStartDialog extends GridDialog {
    // static field to reuse values during the same session.
    private static boolean sWipeData = false;
    private static int sMonitorDpi = 72; // used if there's no setting controller.
    private static final Map<String, String> sSkinScaling = new HashMap<String, String>();

    private static final Pattern sScreenSizePattern = Pattern.compile("\\d*(\\.\\d?)?");

    private final AvdInfo mAvd;
    private final String mSdkLocation;
    private final SettingsController mSettingsController;

    private Text mScreenSize;
    private Text mMonitorDpi;
    private Button mScaleButton;

    private float mScale = 0.f;
    private boolean mWipeData = false;
    private int mDensity = 160; // medium density
    private int mSize1 = -1;
    private int mSize2 = -1;
    private String mSkinDisplay;
    private boolean mEnableScaling = true;
    private Label mScaleField;

    AvdStartDialog(Shell parentShell, AvdInfo avd, String sdkLocation,
            SettingsController settingsController) {
        super(parentShell, 2, false);
        mAvd = avd;
        mSdkLocation = sdkLocation;
        mSettingsController = settingsController;
        if (mAvd == null) {
            throw new IllegalArgumentException("avd cannot be null");
        }
        if (mSdkLocation == null) {
            throw new IllegalArgumentException("sdkLocation cannot be null");
        }

        computeSkinData();
    }

    public boolean getWipeData() {
        return mWipeData;
    }

    /**
     * Returns the scaling factor, or 0.f if none are set.
     */
    public float getScale() {
        return mScale;
    }

    @Override
    public void createDialogContent(final Composite parent) {
        GridData gd;

        Label l = new Label(parent, SWT.NONE);
        l.setText("Skin:");

        l = new Label(parent, SWT.NONE);
        l.setText(mSkinDisplay == null ? "None" : mSkinDisplay);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        l = new Label(parent, SWT.NONE);
        l.setText("Density:");

        l = new Label(parent, SWT.NONE);
        l.setText(getDensityText());
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mScaleButton = new Button(parent, SWT.CHECK);
        mScaleButton.setText("Scale display to real size");
        mScaleButton.setEnabled(mEnableScaling);
        boolean defaultState = mEnableScaling && sSkinScaling.get(mAvd.getName()) != null;
        mScaleButton.setSelection(defaultState);
        mScaleButton.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;
        final Group scaleGroup = new Group(parent, SWT.NONE);
        scaleGroup.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalIndent = 30;
        gd.horizontalSpan = 2;
        scaleGroup.setLayout(new GridLayout(3, false));

        l = new Label(scaleGroup, SWT.NONE);
        l.setText("Screen Size (in):");
        mScreenSize = new Text(scaleGroup, SWT.BORDER);
        mScreenSize.setText(getScreenSize());
        mScreenSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mScreenSize.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent event) {
                // combine the current content and the new text
                String text = mScreenSize.getText();
                text = text.substring(0, event.start) + event.text + text.substring(event.end);

                // now make sure it's a match for the regex
                event.doit = sScreenSizePattern.matcher(text).matches();
            }
        });
        mScreenSize.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                onScaleChange();
            }
        });

        // empty composite, only 2 widgets on this line.
        new Composite(scaleGroup, SWT.NONE).setLayoutData(gd = new GridData());
        gd.widthHint = gd.heightHint = 0;

        l = new Label(scaleGroup, SWT.NONE);
        l.setText("Monitor dpi:");
        mMonitorDpi = new Text(scaleGroup, SWT.BORDER);
        mMonitorDpi.setText(Integer.toString(getMonitorDpi()));
        mMonitorDpi.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.widthHint = 50;
        mMonitorDpi.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent event) {
                // check for digit only.
                for (int i = 0 ; i < event.text.length(); i++) {
                    char letter = event.text.charAt(i);
                    if (letter < '0' || letter > '9') {
                        event.doit = false;
                        return;
                    }
                }
            }
        });
        mMonitorDpi.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                onScaleChange();
            }
        });

        Button button = new Button(scaleGroup, SWT.PUSH | SWT.FLAT);
        button.setText("?");
        button.setToolTipText("Click to figure out your monitor's pixel density");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                ResolutionChooserDialog dialog = new ResolutionChooserDialog(parent.getShell());
                if (dialog.open() == Window.OK) {
                    mMonitorDpi.setText(Integer.toString(dialog.getDensity()));
                }
            }
        });

        l = new Label(scaleGroup, SWT.NONE);
        l.setText("Scale:");
        mScaleField = new Label(scaleGroup, SWT.NONE);
        mScaleField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER,
                true /*grabExcessHorizontalSpace*/,
                true /*grabExcessVerticalSpace*/,
                2 /*horizontalSpan*/,
                1 /*verticalSpan*/));
        setScale(mScale); // set initial text value

        enableGroup(scaleGroup, defaultState);

        mScaleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean enabled = mScaleButton.getSelection();
                enableGroup(scaleGroup, enabled);
                if (enabled) {
                    onScaleChange();
                } else {
                    setScale(0);
                }
            }
        });

        final Button wipeButton = new Button(parent, SWT.CHECK);
        wipeButton.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;
        wipeButton.setText("Wipe user data");
        wipeButton.setSelection(mWipeData = sWipeData);
        wipeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mWipeData = wipeButton.getSelection();
            }
        });

        l = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = 2;

        // if the scaling is enabled by default, we must initialize the value of mScale
        if (defaultState) {
            onScaleChange();
        }
    }

    /** On Windows we need to manually enable/disable the children of a group */
    private void enableGroup(final Group group, boolean enabled) {
        group.setEnabled(enabled);
        for (Control c : group.getChildren()) {
            c.setEnabled(enabled);
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Launch Options");
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == IDialogConstants.OK_ID) {
            label = "Launch";
        }

        return super.createButton(parent, id, label, defaultButton);
    }

    @Override
    protected void okPressed() {
        // override ok to store some info
        // first the monitor dpi
        String dpi = mMonitorDpi.getText();
        if (dpi.length() > 0) {
            sMonitorDpi = Integer.parseInt(dpi);

            // if there is a setting controller, save it
            if (mSettingsController != null) {
                mSettingsController.setMonitorDensity(sMonitorDpi);
                mSettingsController.saveSettings();
            }
        }

        // now the scale factor
        String key = mAvd.getName();
        sSkinScaling.remove(key);
        if (mScaleButton.getSelection()) {
            String size = mScreenSize.getText();
            if (size.length() > 0) {
                sSkinScaling.put(key, size);
            }
        }

        // and then the wipe-data checkbox
        sWipeData = mWipeData;

        // finally continue with the ok action
        super.okPressed();
    }

    private void computeSkinData() {
        Map<String, String> prop = mAvd.getProperties();
        String dpi = prop.get("hw.lcd.density");
        if (dpi != null && dpi.length() > 0) {
            mDensity  = Integer.parseInt(dpi);
        }

        findSkinResolution();
    }

    private void onScaleChange() {
        String sizeStr = mScreenSize.getText();
        if (sizeStr.length() == 0) {
            setScale(0);
            return;
        }

        String dpiStr = mMonitorDpi.getText();
        if (dpiStr.length() == 0) {
            setScale(0);
            return;
        }

        int dpi = Integer.parseInt(dpiStr);
        float size = Float.parseFloat(sizeStr);
        /*
         * We are trying to emulate the following device:
         * resolution: 'mSize1'x'mSize2'
         * density: 'mDensity'
         * screen diagonal: 'size'
         * ontop a monitor running at 'dpi'
         */
        // We start by computing the screen diagonal in pixels, if the density was really mDensity
        float diagonalPx = (float)Math.sqrt(mSize1*mSize1+mSize2*mSize2);
        // Now we would convert this in actual inches:
        //    diagonalIn = diagonal / mDensity
        // the scale factor is a mix of adapting to the new density and to the new size.
        //    (size/diagonalIn) * (dpi/mDensity)
        // this can be simplified to:
        setScale((size * dpi) / diagonalPx);
    }

    private void setScale(float scale) {
        mScale = scale;

        // Do the rounding exactly like AvdSelector will do.
        scale = Math.round(scale * 100);
        scale /=  100.f;

        if (scale == 0.f) {
            mScaleField.setText("default");  //$NON-NLS-1$
        } else {
            mScaleField.setText(String.format("%.2f", scale));  //$NON-NLS-1$
        }
    }

    /**
     * Returns the monitor dpi to start with.
     * This can be coming from the settings, the session-based storage, or the from whatever Java
     * can tell us.
     */
    private int getMonitorDpi() {
        if (mSettingsController != null) {
            sMonitorDpi = mSettingsController.getMonitorDensity();
        }

        if (sMonitorDpi == -1) { // first time? try to get a value
            sMonitorDpi = Toolkit.getDefaultToolkit().getScreenResolution();
        }

        return sMonitorDpi;
    }

    /**
     * Returns the screen size to start with.
     * <p/>If an emulator with the same skin was already launched, scaled, the size used is reused.
     * <p/>Otherwise the default is returned (3)
     */
    private String getScreenSize() {
        String size = sSkinScaling.get(mAvd.getName());
        if (size != null) {
            return size;
        }

        return "3";
    }

    /**
     * Returns a display string for the density.
     */
    private String getDensityText() {
        switch (mDensity) {
            case 120:
                return "Low (120)";
            case 160:
                return "Medium (160)";
            case 240:
                return "High (240)";
        }

        return Integer.toString(mDensity);
    }

    /**
     * Finds the skin resolution and sets it in {@link #mSize1} and {@link #mSize2}.
     */
    private void findSkinResolution() {
        Map<String, String> prop = mAvd.getProperties();
        String skinName = prop.get(AvdManager.AVD_INI_SKIN_NAME);

        if (skinName != null) {
            Matcher m = AvdManager.NUMERIC_SKIN_SIZE.matcher(skinName);
            if (m != null && m.matches()) {
                mSize1 = Integer.parseInt(m.group(1));
                mSize2 = Integer.parseInt(m.group(2));
                mSkinDisplay = skinName;
                mEnableScaling = true;
                return;
            }
        }

        // The resolution is inside the layout file of the skin.
        mEnableScaling = false; // default to false for now.

        // path to the skin layout file.
        String skinPath = prop.get(AvdManager.AVD_INI_SKIN_PATH);
        if (skinPath != null) {
            File skinFolder = new File(mSdkLocation, skinPath);
            if (skinFolder.isDirectory()) {
                File layoutFile = new File(skinFolder, "layout");
                if (layoutFile.isFile()) {
                    if (parseLayoutFile(layoutFile)) {
                        mSkinDisplay = String.format("%1$s (%2$dx%3$d)", skinName, mSize1, mSize2);
                        mEnableScaling = true;
                    } else {
                        mSkinDisplay = skinName;
                    }
                }
            }
        }
    }

    /**
     * Parses a layout file.
     * <p/>
     * the format is relatively easy. It's a collection of items defined as
     * &lg;name&gt; {
     *     &lg;content&gt;
     * }
     *
     * content is either 1+ items or 1+ properties
     * properties are defined as
     * &lg;name&gt;&lg;whitespace&gt;&lg;value&gt;
     *
     * We're going to look for an item called display, with 2 properties height and width.
     * This is very basic parser.
     *
     * @param layoutFile the file to parse
     * @return true if both sizes where found.
     */
    private boolean parseLayoutFile(File layoutFile) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(layoutFile));
            String line;

            while ((line = input.readLine()) != null) {
                // trim to remove whitespace
                line = line.trim();
                int len = line.length();
                if (len == 0) continue;

                // check if this is a new item
                if (line.charAt(len-1) == '{') {
                    // this is the start of a node
                    String[] tokens = line.split(" ");
                    if ("display".equals(tokens[0])) {
                        // this is the one we're looking for!
                        while ((mSize1 == -1 || mSize2 == -1) &&
                                (line = input.readLine()) != null) {
                            // trim to remove whitespace
                            line = line.trim();
                            len = line.length();
                            if (len == 0) continue;

                            if ("}".equals(line)) { // looks like we're done with the item.
                                break;
                            }

                            tokens = line.split(" ");
                            if (tokens.length >= 2) {
                                // there can be multiple space between the name and value
                                // in which case we'll get an extra empty token in the middle.
                                if ("width".equals(tokens[0])) {
                                    mSize1 = Integer.parseInt(tokens[tokens.length-1]);
                                } else if ("height".equals(tokens[0])) {
                                    mSize2 = Integer.parseInt(tokens[tokens.length-1]);
                                }
                            }
                        }

                        return mSize1 != -1 && mSize2 != -1;
                    }
                }

            }
            // if it reaches here, display was not found.
            // false is returned below.
        } catch (IOException e) {
            // ignore.
        }

        return false;
    }
}
