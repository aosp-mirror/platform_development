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

package com.android.ide.eclipse.adt.internal.editors.layout.configuration;

import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.LanguageQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NetworkCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.RegionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier.KeyboardState;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier.NavigationMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier.Density;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier.ScreenOrientation;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier.TextInputMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier.TouchScreenType;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.DimensionVerifier;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.LanguageRegionVerifier;
import com.android.ide.eclipse.adt.internal.ui.ConfigurationSelector.MobileCodeVerifier;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.api.IStyleResourceValue;

import org.eclipse.draw2d.geometry.Rectangle;
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
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigurationComposite extends Composite {

    private final static String THEME_SEPARATOR = "----------"; //$NON-NLS-1$

    private Text mCountry;
    private Text mNetwork;
    private Combo mLanguage;
    private Combo mRegion;
    private Combo mOrientation;
    private Combo mDensity;
    private Combo mTouch;
    private Combo mKeyboard;
    private Combo mTextInput;
    private Combo mNavigation;
    private Text mSize1;
    private Text mSize2;
    private Combo mThemeCombo;
    private Button mCreateButton;

    private Label mCountryIcon;
    private Label mNetworkIcon;
    private Label mLanguageIcon;
    private Label mRegionIcon;
    private Label mOrientationIcon;
    private Label mDensityIcon;
    private Label mTouchIcon;
    private Label mKeyboardIcon;
    private Label mTextInputIcon;
    private Label mNavigationIcon;
    private Label mSizeIcon;

    private Label mCurrentLayoutLabel;

    private Image mWarningImage;
    private Image mMatchImage;
    private Image mErrorImage;

    private int mPlatformThemeCount = 0;
    private boolean mDisableUpdates = false;

    /** The {@link FolderConfiguration} representing the state of the UI controls */
    private final FolderConfiguration mCurrentConfig = new FolderConfiguration();
    private final IConfigListener mListener;

    public interface IConfigListener {
        void onConfigurationChange();
        void onThemeChange();
        void onCreate();

        ProjectResources getProjectResources();
        ProjectResources getFrameworkResources();
        Map<String, Map<String, IResourceValue>> getConfiguredProjectResources();
        Map<String, Map<String, IResourceValue>> getConfiguredFrameworkResources();
    }

    public ConfigurationComposite(IConfigListener listener, Composite parent, int style) {
        super(parent, style);
        mListener = listener;

        IconFactory factory = IconFactory.getInstance();
        mWarningImage = factory.getIcon("warning"); //$NON-NLS-1$
        mMatchImage = factory.getIcon("match"); //$NON-NLS-1$
        mErrorImage = factory.getIcon("error"); //$NON-NLS-1$

        GridLayout gl;
        GridData gd;
        int cols = 10;

        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        setLayout(gl = new GridLayout(cols, false));

        new Label(this, SWT.NONE).setText("MCC");
        mCountryIcon = createControlComposite(this, true /* grab_horizontal */);
        mCountry = new Text(mCountryIcon.getParent(), SWT.BORDER);
        mCountry.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mCountry.addVerifyListener(new MobileCodeVerifier());
        mCountry.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onCountryCodeChange();
            }
        });
        mCountry.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onCountryCodeChange();
            }
        });

        new Label(this, SWT.NONE).setText("MNC");
        mNetworkIcon = createControlComposite(this, true /* grab_horizontal */);
        mNetwork = new Text(mNetworkIcon.getParent(), SWT.BORDER);
        mNetwork.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mNetwork.addVerifyListener(new MobileCodeVerifier());
        mNetwork.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                onNetworkCodeChange();
            }
        });
        mNetwork.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onNetworkCodeChange();
            }
        });

        new Label(this, SWT.NONE).setText("Lang");
        mLanguageIcon = createControlComposite(this, true /* grab_horizontal */);
        mLanguage = new Combo(mLanguageIcon.getParent(), SWT.DROP_DOWN);
        mLanguage.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mLanguage.addVerifyListener(new LanguageRegionVerifier());
        mLanguage.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onLanguageChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onLanguageChange();
            }
        });
        mLanguage.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onLanguageChange();
            }
        });

        new Label(this, SWT.NONE).setText("Region");
        mRegionIcon = createControlComposite(this, true /* grab_horizontal */);
        mRegion = new Combo(mRegionIcon.getParent(), SWT.DROP_DOWN);
        mRegion.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mRegion.addVerifyListener(new LanguageRegionVerifier());
        mRegion.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onRegionChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onRegionChange();
            }
        });
        mRegion.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onRegionChange();
            }
        });

        new Label(this, SWT.NONE).setText("Orient");
        mOrientationIcon = createControlComposite(this, true /* grab_horizontal */);
        mOrientation = new Combo(mOrientationIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        ScreenOrientation[] soValues = ScreenOrientation.values();
        mOrientation.add("(Default)");
        for (ScreenOrientation value : soValues) {
            mOrientation.add(value.getDisplayValue());
        }
        mOrientation.select(0);
        mOrientation.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mOrientation.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               onOrientationChange();
            }
        });

        new Label(this, SWT.NONE).setText("Density");
        mDensityIcon = createControlComposite(this, true /* grab_horizontal */);
        mDensity = new Combo(mDensityIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        Density[] dValues = Density.values();
        mDensity.add("(Default)");
        for (Density value : dValues) {
            mDensity.add(value.getDisplayValue());
        }
        mDensity.select(0);
        mDensity.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mDensity.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
               onDensityChange();
            }
        });

        new Label(this, SWT.NONE).setText("Touch");
        mTouchIcon = createControlComposite(this, true /* grab_horizontal */);
        mTouch = new Combo(mTouchIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        TouchScreenType[] tstValues = TouchScreenType.values();
        mTouch.add("(Default)");
        for (TouchScreenType value : tstValues) {
            mTouch.add(value.getDisplayValue());
        }
        mTouch.select(0);
        mTouch.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mTouch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTouchChange();
            }
        });

        new Label(this, SWT.NONE).setText("Keybrd");
        mKeyboardIcon = createControlComposite(this, true /* grab_horizontal */);
        mKeyboard = new Combo(mKeyboardIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        KeyboardState[] ksValues = KeyboardState.values();
        mKeyboard.add("(Default)");
        for (KeyboardState value : ksValues) {
            mKeyboard.add(value.getDisplayValue());
        }
        mKeyboard.select(0);
        mKeyboard.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mKeyboard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onKeyboardChange();
            }
        });

        new Label(this, SWT.NONE).setText("Input");
        mTextInputIcon = createControlComposite(this, true /* grab_horizontal */);
        mTextInput = new Combo(mTextInputIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        TextInputMethod[] timValues = TextInputMethod.values();
        mTextInput.add("(Default)");
        for (TextInputMethod value : timValues) {
            mTextInput.add(value.getDisplayValue());
        }
        mTextInput.select(0);
        mTextInput.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mTextInput.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTextInputChange();
            }
        });

        new Label(this, SWT.NONE).setText("Nav");
        mNavigationIcon = createControlComposite(this, true /* grab_horizontal */);
        mNavigation = new Combo(mNavigationIcon.getParent(), SWT.DROP_DOWN | SWT.READ_ONLY);
        NavigationMethod[] nValues = NavigationMethod.values();
        mNavigation.add("(Default)");
        for (NavigationMethod value : nValues) {
            mNavigation.add(value.getDisplayValue());
        }
        mNavigation.select(0);
        mNavigation.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mNavigation.addSelectionListener(new SelectionAdapter() {
            @Override
             public void widgetSelected(SelectionEvent e) {
                onNavigationChange();
            }
        });

        Composite labelParent = new Composite(this, SWT.NONE);
        labelParent.setLayout(gl = new GridLayout(8, false));
        gl.marginWidth = gl.marginHeight = 0;
        labelParent.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.horizontalSpan = cols;

        new Label(labelParent, SWT.NONE).setText("Editing config:");
        mCurrentLayoutLabel = new Label(labelParent, SWT.NONE);
        mCurrentLayoutLabel.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
        gd.widthHint = 50;

        new Label(labelParent, SWT.NONE).setText("Size");
        mSizeIcon = createControlComposite(labelParent, false);
        Composite sizeParent = new Composite(mSizeIcon.getParent(), SWT.NONE);
        sizeParent.setLayout(gl = new GridLayout(3, false));
        gl.marginWidth = gl.marginHeight = 0;
        gl.horizontalSpacing = 0;

        mSize1 = new Text(sizeParent, SWT.BORDER);
        mSize1.setLayoutData(gd = new GridData());
        gd.widthHint = 30;
        new Label(sizeParent, SWT.NONE).setText("x");
        mSize2 = new Text(sizeParent, SWT.BORDER);
        mSize2.setLayoutData(gd = new GridData());
        gd.widthHint = 30;

        DimensionVerifier verifier = new DimensionVerifier();
        mSize1.addVerifyListener(verifier);
        mSize2.addVerifyListener(verifier);

        SelectionListener sl = new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                onSizeChange();
            }
            public void widgetSelected(SelectionEvent e) {
                onSizeChange();
            }
        };

        mSize1.addSelectionListener(sl);
        mSize2.addSelectionListener(sl);

        ModifyListener sizeModifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                onSizeChange();
            }
        };

        mSize1.addModifyListener(sizeModifyListener);
        mSize2.addModifyListener(sizeModifyListener);

        // first separator
        Label separator = new Label(labelParent, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;

        mThemeCombo = new Combo(labelParent, SWT.READ_ONLY | SWT.DROP_DOWN);
        mThemeCombo.setEnabled(false);
        updateUIFromResources();
        mThemeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onThemeChange();
            }
        });

        // second separator
        separator = new Label(labelParent, SWT.SEPARATOR | SWT.VERTICAL);
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;

        mCreateButton = new Button(labelParent, SWT.PUSH | SWT.FLAT);
        mCreateButton.setText("Create...");
        mCreateButton.setEnabled(false);
        mCreateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mListener != null) {
                    mListener.onCreate();
                }
            }
        });

    }

    public void setConfig(FolderConfiguration config) {
        mCurrentConfig.set(config);
    }

    public FolderConfiguration getCurrentConfig() {
        return mCurrentConfig;
    }

    public void getCurrentConfig(FolderConfiguration config) {
        config.set(mCurrentConfig);
    }

    public Rectangle getScreenBounds() {
        ScreenOrientation orientation = null;
        if (mOrientation.getSelectionIndex() == 0) {
            orientation = ScreenOrientation.PORTRAIT;
        } else {
            orientation = ScreenOrientation.getByIndex(
                    mOrientation.getSelectionIndex() - 1);
        }

        int s1, s2;

        // get the size from the UI controls. If it fails, revert to default values.
        try {
            s1 = Integer.parseInt(mSize1.getText().trim());
        } catch (NumberFormatException e) {
            s1 = 480;
        }

        try {
            s2 = Integer.parseInt(mSize2.getText().trim());
        } catch (NumberFormatException e) {
            s2 = 320;
        }

        // make sure s1 is bigger than s2
        if (s1 < s2) {
            int tmp = s1;
            s1 = s2;
            s2 = tmp;
        }

        switch (orientation) {
            default:
            case PORTRAIT:
                return new Rectangle(0, 0, s2, s1);
            case LANDSCAPE:
                return new Rectangle(0, 0, s1, s2);
            case SQUARE:
                return new Rectangle(0, 0, s1, s1);
        }
    }

    /**
     * Updates the UI from values in the resources, such as languages, regions, themes, etc...
     * This must be called from the UI thread.
     */
    public void updateUIFromResources() {
        if (mListener == null) {
            return; // can't do anything w/o it.
        }

        ProjectResources frameworkProject = mListener.getFrameworkResources();

        mDisableUpdates = true;

        // Reset stuff
        int selection = mThemeCombo.getSelectionIndex();
        mThemeCombo.removeAll();
        mPlatformThemeCount = 0;
        mLanguage.removeAll();

        Set<String> languages = new HashSet<String>();
        ArrayList<String> themes = new ArrayList<String>();

        // get the themes, and languages from the Framework.
        if (frameworkProject != null) {
            // get the configured resources for the framework
            Map<String, Map<String, IResourceValue>> frameworResources =
                mListener.getConfiguredFrameworkResources();

            if (frameworResources != null) {
                // get the styles.
                Map<String, IResourceValue> styles = frameworResources.get(
                        ResourceType.STYLE.getName());


                // collect the themes out of all the styles.
                for (IResourceValue value : styles.values()) {
                    String name = value.getName();
                    if (name.startsWith("Theme.") || name.equals("Theme")) {
                        themes.add(value.getName());
                        mPlatformThemeCount++;
                    }
                }

                // sort them and add them to the combo
                Collections.sort(themes);

                for (String theme : themes) {
                    mThemeCombo.add(theme);
                }

                mPlatformThemeCount = themes.size();
                themes.clear();
            }
            // now get the languages from the framework.
            Set<String> frameworkLanguages = frameworkProject.getLanguages();
            if (frameworkLanguages != null) {
                languages.addAll(frameworkLanguages);
            }
        }

        // now get the themes and languages from the project.
        ProjectResources project = mListener.getProjectResources();
        // in cases where the opened file is not linked to a project, this could be null.
        if (project != null) {
            // get the configured resources for the project
            Map<String, Map<String, IResourceValue>> configuredProjectRes =
                mListener.getConfiguredProjectResources();

            if (configuredProjectRes != null) {
                // get the styles.
                Map<String, IResourceValue> styleMap = configuredProjectRes.get(
                        ResourceType.STYLE.getName());

                if (styleMap != null) {
                    // collect the themes out of all the styles, ie styles that extend,
                    // directly or indirectly a platform theme.
                    for (IResourceValue value : styleMap.values()) {
                        if (isTheme(value, styleMap)) {
                            themes.add(value.getName());
                        }
                    }

                    // sort them and add them the to the combo.
                    if (mPlatformThemeCount > 0 && themes.size() > 0) {
                        mThemeCombo.add(THEME_SEPARATOR);
                    }

                    Collections.sort(themes);

                    for (String theme : themes) {
                        mThemeCombo.add(theme);
                    }
                }
            }

            // now get the languages from the project.
            Set<String> projectLanguages = project.getLanguages();
            if (projectLanguages != null) {
                languages.addAll(projectLanguages);
            }
        }

        // add the languages to the Combo
        for (String language : languages) {
            mLanguage.add(language);
        }

        mDisableUpdates = false;

        // and update the Region UI based on the current language
        updateRegionUi();

        // handle default selection of themes
        if (mThemeCombo.getItemCount() > 0) {
            mThemeCombo.setEnabled(true);
            if (selection == -1) {
                selection = 0;
            }

            if (mThemeCombo.getItemCount() <= selection) {
                mThemeCombo.select(0);
            } else {
                mThemeCombo.select(selection);
            }
        } else {
            mThemeCombo.setEnabled(false);
        }

        mThemeCombo.getParent().layout();
    }

    /**
     * Returns the current theme, or null if the combo has no selection.
     */
    public String getTheme() {
        int themeIndex = mThemeCombo.getSelectionIndex();
        if (themeIndex != -1) {
            return mThemeCombo.getItem(themeIndex);
        }

        return null;
    }

    /**
     * Returns whether the current theme selection is a project theme.
     * <p/>The returned value is meaningless if {@link #getTheme()} returns <code>null</code>.
     * @return true for project theme, false for framework theme
     */
    public boolean isProjectTheme() {
        return mThemeCombo.getSelectionIndex() >= mPlatformThemeCount;
    }


    public void setEnabledCreate(boolean enabled) {
        mCreateButton.setEnabled(enabled);
    }

    /**
     * Update the UI controls state with a given {@link FolderConfiguration}.
     * <p/>If <var>force</var> is set to <code>true</code> the UI will be changed to exactly reflect
     * <var>config</var>, otherwise, if a qualifier is not present in <var>config</var>,
     * the UI control is not modified. However if the value in the control is not the default value,
     * a warning icon is shown.
     * @param config The {@link FolderConfiguration} to set.
     * @param force Whether the UI should be changed to exactly match the received configuration.
     */
    public void setConfiguration(FolderConfiguration config, boolean force) {
        mDisableUpdates = true; // we do not want to trigger onXXXChange when setting new values in the widgets.

        mCountryIcon.setImage(mMatchImage);
        CountryCodeQualifier countryQualifier = config.getCountryCodeQualifier();
        if (countryQualifier != null) {
            mCountry.setText(String.format("%1$d", countryQualifier.getCode()));
            mCurrentConfig.setCountryCodeQualifier(countryQualifier);
        } else if (force) {
            mCountry.setText(""); //$NON-NLS-1$
            mCurrentConfig.setCountryCodeQualifier(null);
        } else if (mCountry.getText().length() > 0) {
            mCountryIcon.setImage(mWarningImage);
        }

        mNetworkIcon.setImage(mMatchImage);
        NetworkCodeQualifier networkQualifier = config.getNetworkCodeQualifier();
        if (networkQualifier != null) {
            mNetwork.setText(String.format("%1$d", networkQualifier.getCode()));
            mCurrentConfig.setNetworkCodeQualifier(networkQualifier);
        } else if (force) {
            mNetwork.setText(""); //$NON-NLS-1$
            mCurrentConfig.setNetworkCodeQualifier(null);
        } else if (mNetwork.getText().length() > 0) {
            mNetworkIcon.setImage(mWarningImage);
        }

        mLanguageIcon.setImage(mMatchImage);
        LanguageQualifier languageQualifier = config.getLanguageQualifier();
        if (languageQualifier != null) {
            mLanguage.setText(languageQualifier.getValue());
            mCurrentConfig.setLanguageQualifier(languageQualifier);
        } else if (force) {
            mLanguage.setText(""); //$NON-NLS-1$
            mCurrentConfig.setLanguageQualifier(null);
        } else if (mLanguage.getText().length() > 0) {
            mLanguageIcon.setImage(mWarningImage);
        }

        mRegionIcon.setImage(mMatchImage);
        RegionQualifier regionQualifier = config.getRegionQualifier();
        if (regionQualifier != null) {
            mRegion.setText(regionQualifier.getValue());
            mCurrentConfig.setRegionQualifier(regionQualifier);
        } else if (force) {
            mRegion.setText(""); //$NON-NLS-1$
            mCurrentConfig.setRegionQualifier(null);
        } else if (mRegion.getText().length() > 0) {
            mRegionIcon.setImage(mWarningImage);
        }

        mOrientationIcon.setImage(mMatchImage);
        ScreenOrientationQualifier orientationQualifier = config.getScreenOrientationQualifier();
        if (orientationQualifier != null) {
            mOrientation.select(
                    ScreenOrientation.getIndex(orientationQualifier.getValue()) + 1);
            mCurrentConfig.setScreenOrientationQualifier(orientationQualifier);
        } else if (force) {
            mOrientation.select(0);
            mCurrentConfig.setScreenOrientationQualifier(null);
        } else if (mOrientation.getSelectionIndex() != 0) {
            mOrientationIcon.setImage(mWarningImage);
        }

        mDensityIcon.setImage(mMatchImage);
        PixelDensityQualifier densityQualifier = config.getPixelDensityQualifier();
        if (densityQualifier != null) {
            mDensity.select(
                    Density.getIndex(densityQualifier.getValue()) + 1);
            mCurrentConfig.setPixelDensityQualifier(densityQualifier);
        } else if (force) {
            mDensity.select(0);
            mCurrentConfig.setPixelDensityQualifier(null);
        } else if (mDensity.getSelectionIndex() != 0) {
            mDensityIcon.setImage(mWarningImage);
        }

        mTouchIcon.setImage(mMatchImage);
        TouchScreenQualifier touchQualifier = config.getTouchTypeQualifier();
        if (touchQualifier != null) {
            mTouch.select(TouchScreenType.getIndex(touchQualifier.getValue()) + 1);
            mCurrentConfig.setTouchTypeQualifier(touchQualifier);
        } else if (force) {
            mTouch.select(0);
            mCurrentConfig.setTouchTypeQualifier(null);
        } else if (mTouch.getSelectionIndex() != 0) {
            mTouchIcon.setImage(mWarningImage);
        }

        mKeyboardIcon.setImage(mMatchImage);
        KeyboardStateQualifier keyboardQualifier = config.getKeyboardStateQualifier();
        if (keyboardQualifier != null) {
            mKeyboard.select(KeyboardState.getIndex(keyboardQualifier.getValue()) + 1);
            mCurrentConfig.setKeyboardStateQualifier(keyboardQualifier);
        } else if (force) {
            mKeyboard.select(0);
            mCurrentConfig.setKeyboardStateQualifier(null);
        } else if (mKeyboard.getSelectionIndex() != 0) {
            mKeyboardIcon.setImage(mWarningImage);
        }

        mTextInputIcon.setImage(mMatchImage);
        TextInputMethodQualifier inputQualifier = config.getTextInputMethodQualifier();
        if (inputQualifier != null) {
            mTextInput.select(TextInputMethod.getIndex(inputQualifier.getValue()) + 1);
            mCurrentConfig.setTextInputMethodQualifier(inputQualifier);
        } else if (force) {
            mTextInput.select(0);
            mCurrentConfig.setTextInputMethodQualifier(null);
        } else if (mTextInput.getSelectionIndex() != 0) {
            mTextInputIcon.setImage(mWarningImage);
        }

        mNavigationIcon.setImage(mMatchImage);
        NavigationMethodQualifier navigationQualifiter = config.getNavigationMethodQualifier();
        if (navigationQualifiter != null) {
            mNavigation.select(
                    NavigationMethod.getIndex(navigationQualifiter.getValue()) + 1);
            mCurrentConfig.setNavigationMethodQualifier(navigationQualifiter);
        } else if (force) {
            mNavigation.select(0);
            mCurrentConfig.setNavigationMethodQualifier(null);
        } else if (mNavigation.getSelectionIndex() != 0) {
            mNavigationIcon.setImage(mWarningImage);
        }

        mSizeIcon.setImage(mMatchImage);
        ScreenDimensionQualifier sizeQualifier = config.getScreenDimensionQualifier();
        if (sizeQualifier != null) {
            mSize1.setText(String.format("%1$d", sizeQualifier.getValue1()));
            mSize2.setText(String.format("%1$d", sizeQualifier.getValue2()));
            mCurrentConfig.setScreenDimensionQualifier(sizeQualifier);
        } else if (force) {
            mSize1.setText(""); //$NON-NLS-1$
            mSize2.setText(""); //$NON-NLS-1$
            mCurrentConfig.setScreenDimensionQualifier(null);
        } else if (mSize1.getText().length() > 0 && mSize2.getText().length() > 0) {
            mSizeIcon.setImage(mWarningImage);
        }

        // update the string showing the folder name
        String current = config.toDisplayString();
        mCurrentLayoutLabel.setText(current != null ? current : "(Default)");

        mDisableUpdates = false;
    }

    /**
     * Displays an error icon in front of all the non-null qualifiers.
     */
    public void displayConfigError() {
        mCountryIcon.setImage(mMatchImage);
        CountryCodeQualifier countryQualifier = mCurrentConfig.getCountryCodeQualifier();
        if (countryQualifier != null) {
            mCountryIcon.setImage(mErrorImage);
        }

        mNetworkIcon.setImage(mMatchImage);
        NetworkCodeQualifier networkQualifier = mCurrentConfig.getNetworkCodeQualifier();
        if (networkQualifier != null) {
            mNetworkIcon.setImage(mErrorImage);
        }

        mLanguageIcon.setImage(mMatchImage);
        LanguageQualifier languageQualifier = mCurrentConfig.getLanguageQualifier();
        if (languageQualifier != null) {
            mLanguageIcon.setImage(mErrorImage);
        }

        mRegionIcon.setImage(mMatchImage);
        RegionQualifier regionQualifier = mCurrentConfig.getRegionQualifier();
        if (regionQualifier != null) {
            mRegionIcon.setImage(mErrorImage);
        }

        mOrientationIcon.setImage(mMatchImage);
        ScreenOrientationQualifier orientationQualifier =
            mCurrentConfig.getScreenOrientationQualifier();
        if (orientationQualifier != null) {
            mOrientationIcon.setImage(mErrorImage);
        }

        mDensityIcon.setImage(mMatchImage);
        PixelDensityQualifier densityQualifier = mCurrentConfig.getPixelDensityQualifier();
        if (densityQualifier != null) {
            mDensityIcon.setImage(mErrorImage);
        }

        mTouchIcon.setImage(mMatchImage);
        TouchScreenQualifier touchQualifier = mCurrentConfig.getTouchTypeQualifier();
        if (touchQualifier != null) {
            mTouchIcon.setImage(mErrorImage);
        }

        mKeyboardIcon.setImage(mMatchImage);
        KeyboardStateQualifier keyboardQualifier = mCurrentConfig.getKeyboardStateQualifier();
        if (keyboardQualifier != null) {
            mKeyboardIcon.setImage(mErrorImage);
        }

        mTextInputIcon.setImage(mMatchImage);
        TextInputMethodQualifier inputQualifier = mCurrentConfig.getTextInputMethodQualifier();
        if (inputQualifier != null) {
            mTextInputIcon.setImage(mErrorImage);
        }

        mNavigationIcon.setImage(mMatchImage);
        NavigationMethodQualifier navigationQualifiter =
            mCurrentConfig.getNavigationMethodQualifier();
        if (navigationQualifiter != null) {
            mNavigationIcon.setImage(mErrorImage);
        }

        mSizeIcon.setImage(mMatchImage);
        ScreenDimensionQualifier sizeQualifier = mCurrentConfig.getScreenDimensionQualifier();
        if (sizeQualifier != null) {
            mSizeIcon.setImage(mErrorImage);
        }

        // update the string showing the folder name
        String current = mCurrentConfig.toDisplayString();
        mCurrentLayoutLabel.setText(current != null ? current : "(Default)");
    }



    private void onCountryCodeChange() {
        // because mCountry triggers onCountryCodeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mCountry.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setCountryCodeQualifier(null);
        } else {
            try {
                CountryCodeQualifier qualifier = CountryCodeQualifier.getQualifier(
                        CountryCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                if (qualifier != null) {
                    mCurrentConfig.setCountryCodeQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    // We do nothing in this case.
                    mCountryIcon.setImage(mErrorImage);
                    return;
                }
            } catch (NumberFormatException e) {
                // Looks like the code is not a number. This should not happen since the text
                // field has a VerifyListener that prevents it.
                mCurrentConfig.setCountryCodeQualifier(null);
                mCountryIcon.setImage(mErrorImage);
            }
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onNetworkCodeChange() {
        // because mNetwork triggers onNetworkCodeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mNetwork.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setNetworkCodeQualifier(null);
        } else {
            try {
                NetworkCodeQualifier qualifier = NetworkCodeQualifier.getQualifier(
                        NetworkCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                if (qualifier != null) {
                    mCurrentConfig.setNetworkCodeQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    // We do nothing in this case.
                    mNetworkIcon.setImage(mErrorImage);
                    return;
                }
            } catch (NumberFormatException e) {
                // Looks like the code is not a number. This should not happen since the text
                // field has a VerifyListener that prevents it.
                mCurrentConfig.setNetworkCodeQualifier(null);
                mNetworkIcon.setImage(mErrorImage);
            }
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    /**
     * Call back for language combo selection
     */
    private void onLanguageChange() {
        // because mLanguage triggers onLanguageChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mLanguage.getText();

        updateRegionUi();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setLanguageQualifier(null);
        } else {
            LanguageQualifier qualifier = null;
            String segment = LanguageQualifier.getFolderSegment(value);
            if (segment != null) {
                qualifier = LanguageQualifier.getQualifier(segment);
            }

            if (qualifier != null) {
                mCurrentConfig.setLanguageQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong (for instance a one letter string).
                mCurrentConfig.setLanguageQualifier(null);
                mLanguageIcon.setImage(mErrorImage);
            }
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onRegionChange() {
        // because mRegion triggers onRegionChange at each modification, the filling
        // of the combo with data will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String value = mRegion.getText();

        // empty string, means no qualifier.
        if (value.length() == 0) {
            mCurrentConfig.setRegionQualifier(null);
        } else {
            RegionQualifier qualifier = null;
            String segment = RegionQualifier.getFolderSegment(value);
            if (segment != null) {
                qualifier = RegionQualifier.getQualifier(segment);
            }

            if (qualifier != null) {
                mCurrentConfig.setRegionQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong (for instance a one letter string).
                mCurrentConfig.setRegionQualifier(null);
                mRegionIcon.setImage(mErrorImage);
            }
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onOrientationChange() {
        // update the current config
        int index = mOrientation.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setScreenOrientationQualifier(new ScreenOrientationQualifier(
                ScreenOrientation.getByIndex(index-1)));
        } else {
            mCurrentConfig.setScreenOrientationQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onDensityChange() {
        int index = mDensity.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setPixelDensityQualifier((new PixelDensityQualifier(
                Density.getByIndex(index-1))));
        } else {
            mCurrentConfig.setPixelDensityQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onTouchChange() {
        // update the current config
        int index = mTouch.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setTouchTypeQualifier(new TouchScreenQualifier(
                TouchScreenType.getByIndex(index-1)));
        } else {
            mCurrentConfig.setTouchTypeQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onKeyboardChange() {
        // update the current config
        int index = mKeyboard.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setKeyboardStateQualifier(new KeyboardStateQualifier(
                KeyboardState.getByIndex(index-1)));
        } else {
            mCurrentConfig.setKeyboardStateQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onTextInputChange() {
        // update the current config
        int index = mTextInput.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setTextInputMethodQualifier(new TextInputMethodQualifier(
                TextInputMethod.getByIndex(index-1)));
        } else {
            mCurrentConfig.setTextInputMethodQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onNavigationChange() {
        // update the current config
        int index = mNavigation.getSelectionIndex();
        if (index != 0) {
            mCurrentConfig.setNavigationMethodQualifier(new NavigationMethodQualifier(
                NavigationMethod.getByIndex(index-1)));
        } else {
            mCurrentConfig.setNavigationMethodQualifier(null);
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onSizeChange() {
        // because mSize1 and mSize2 trigger onSizeChange at each modification, calling setText()
        // will trigger notifications, and we don't want that.
        if (mDisableUpdates == true) {
            return;
        }

        // update the current config
        String size1 = mSize1.getText();
        String size2 = mSize2.getText();

        // if only one of the strings is empty, do nothing
        if ((size1.length() == 0) ^ (size2.length() == 0)) {
            mSizeIcon.setImage(mErrorImage);
            return;
        } else if (size1.length() == 0 && size2.length() == 0) {
            // both sizes are empty: remove the qualifier.
            mCurrentConfig.setScreenDimensionQualifier(null);
        } else {
            ScreenDimensionQualifier qualifier = ScreenDimensionQualifier.getQualifier(size1,
                    size2);

            if (qualifier != null) {
                mCurrentConfig.setScreenDimensionQualifier(qualifier);
            } else {
                // Failure! Looks like the value is wrong.
                // we do nothing in this case.
                return;
            }
        }

        if (mListener != null) {
            mListener.onConfigurationChange();
        }
    }

    private void onThemeChange() {
        int themeIndex = mThemeCombo.getSelectionIndex();
        if (themeIndex != -1) {
            String theme = mThemeCombo.getItem(themeIndex);

            if (theme.equals(THEME_SEPARATOR)) {
                mThemeCombo.select(0);
            }

            if (mListener != null) {
                mListener.onThemeChange();
            }
        }
    }

    /**
     * Creates a composite with no margin/spacing, and puts a {@link Label} in it with the matching
     * icon.
     * @param parent the parent to receive the composite
     * @return the created {@link Label} object.
     */
    private Label createControlComposite(Composite parent, boolean grab) {
        GridLayout gl;

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;
        gl.horizontalSpacing = 0;
        if (grab) {
            composite.setLayoutData(
                    new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        }

        // create the label
        Label icon = new Label(composite, SWT.NONE);
        icon.setImage(mMatchImage);

        return icon;
    }

    /**
     * Update the Region UI widget based on the current language selection
     * @param projectResources the project resources or {@code null}.
     * @param frameworkResources the framework resource or {@code null}
     */
    private void updateRegionUi() {
        if (mListener == null) {
            return;
        }

        ProjectResources projectResources = mListener.getProjectResources();
        ProjectResources frameworkResources = mListener.getFrameworkResources();

        String currentLanguage = mLanguage.getText();

        Set<String> set = null;

        if (projectResources != null) {
            set = projectResources.getRegions(currentLanguage);
        }

        if (frameworkResources != null) {
            if (set != null) {
                Set<String> set2 = frameworkResources.getRegions(currentLanguage);
                set.addAll(set2);
            } else {
                set = frameworkResources.getRegions(currentLanguage);
            }
        }

        if (set != null) {
            mDisableUpdates = true;

            mRegion.removeAll();
            for (String region : set) {
                mRegion.add(region);
            }

            mDisableUpdates = false;
        }
    }


    /**
     * Returns whether the given <var>style</var> is a theme.
     * This is done by making sure the parent is a theme.
     * @param value the style to check
     * @param styleMap the map of styles for the current project. Key is the style name.
     * @return True if the given <var>style</var> is a theme.
     */
    private boolean isTheme(IResourceValue value, Map<String, IResourceValue> styleMap) {
        if (value instanceof IStyleResourceValue) {
            IStyleResourceValue style = (IStyleResourceValue)value;

            boolean frameworkStyle = false;
            String parentStyle = style.getParentStyle();
            if (parentStyle == null) {
                // if there is no specified parent style we look an implied one.
                // For instance 'Theme.light' is implied child style of 'Theme',
                // and 'Theme.light.fullscreen' is implied child style of 'Theme.light'
                String name = style.getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    parentStyle = name.substring(0, index);
                }
            } else {
                // remove the useless @ if it's there
                if (parentStyle.startsWith("@")) {
                    parentStyle = parentStyle.substring(1);
                }

                // check for framework identifier.
                if (parentStyle.startsWith("android:")) {
                    frameworkStyle = true;
                    parentStyle = parentStyle.substring("android:".length());
                }

                // at this point we could have the format style/<name>. we want only the name
                if (parentStyle.startsWith("style/")) {
                    parentStyle = parentStyle.substring("style/".length());
                }
            }

            if (frameworkStyle) {
                // if the parent is a framework style, it has to be 'Theme' or 'Theme.*'
                return parentStyle.equals("Theme") || parentStyle.startsWith("Theme.");
            } else {
                // if it's a project style, we check this is a theme.
                value = styleMap.get(parentStyle);
                if (value != null) {
                    return isTheme(value, styleMap);
                }
            }
        }

        return false;
    }

}
