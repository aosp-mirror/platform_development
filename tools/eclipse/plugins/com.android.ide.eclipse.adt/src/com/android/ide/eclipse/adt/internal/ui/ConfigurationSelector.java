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

package com.android.ide.eclipse.adt.internal.ui;

import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.LanguageQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NetworkCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.RegionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenRatioQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenSizeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.VersionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier.KeyboardState;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier.NavigationMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier.Density;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier.ScreenOrientation;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenRatioQualifier.ScreenRatio;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenSizeQualifier.ScreenSize;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier.TextInputMethod;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier.TouchScreenType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import java.util.HashMap;

/**
 * Custom UI widget to let user build a Folder configuration.
 * <p/>
 * To use this, instantiate somewhere in the UI and then:
 * <ul>
 * <li>Use {@link #setConfiguration(String)} or {@link #setConfiguration(FolderConfiguration)}.
 * <li>Retrieve the configuration using {@link #getConfiguration(FolderConfiguration)}.
 * </ul>
 */
public class ConfigurationSelector extends Composite {

    public static final int WIDTH_HINT = 600;
    public static final int HEIGHT_HINT = 250;

    private Runnable mOnChangeListener;

    private TableViewer mFullTableViewer;
    private TableViewer mSelectionTableViewer;
    private Button mAddButton;
    private Button mRemoveButton;
    private StackLayout mStackLayout;

    private boolean mOnRefresh = false;

    private final FolderConfiguration mBaseConfiguration = new FolderConfiguration();
    private final FolderConfiguration mSelectedConfiguration = new FolderConfiguration();

    private final HashMap<Class<? extends ResourceQualifier>, QualifierEditBase> mUiMap =
        new HashMap<Class<? extends ResourceQualifier>, QualifierEditBase>();
    private Composite mQualifierEditParent;

    /**
     * Basic of {@link VerifyListener} to only accept digits.
     */
    private static class DigitVerifier implements VerifyListener {
        public void verifyText(VerifyEvent e) {
            // check for digit only.
            for (int i = 0 ; i < e.text.length(); i++) {
                char letter = e.text.charAt(i);
                if (letter < '0' || letter > '9') {
                    e.doit = false;
                    return;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for Country Code qualifiers.
     */
    public static class MobileCodeVerifier extends DigitVerifier {
        @Override
        public void verifyText(VerifyEvent e) {
            super.verifyText(e);

            // basic tests passed?
            if (e.doit) {
                // check the max 3 digits.
                if (e.text.length() - e.end + e.start +
                        ((Text)e.getSource()).getText().length() > 3) {
                    e.doit = false;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for the Language and Region qualifiers.
     */
    public static class LanguageRegionVerifier implements VerifyListener {
        public void verifyText(VerifyEvent e) {
            // check for length
            if (e.text.length() - e.end + e.start + ((Combo)e.getSource()).getText().length() > 2) {
                e.doit = false;
                return;
            }

            // check for lower case only.
            for (int i = 0 ; i < e.text.length(); i++) {
                char letter = e.text.charAt(i);
                if ((letter < 'a' || letter > 'z') && (letter < 'A' || letter > 'Z')) {
                    e.doit = false;
                    return;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for the Pixel Density qualifier.
     */
    public static class DensityVerifier extends DigitVerifier { }

    /**
     * Implementation of {@link VerifyListener} for the Screen Dimension qualifier.
     */
    public static class DimensionVerifier extends DigitVerifier { }

    /**
     * Enum for the state of the configuration being created.
     */
    public enum ConfigurationState {
        OK, INVALID_CONFIG, REGION_WITHOUT_LANGUAGE;
    }

    public ConfigurationSelector(Composite parent) {
        super(parent, SWT.NONE);

        mBaseConfiguration.createDefault();

        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = gl.marginHeight = 0;
        setLayout(gl);

        // first column is the first table
        final Table fullTable = new Table(this, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        fullTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        fullTable.setHeaderVisible(true);
        fullTable.setLinesVisible(true);

        // create the column
        final TableColumn fullTableColumn = new TableColumn(fullTable, SWT.LEFT);
        // set the header
        fullTableColumn.setText("Available Qualifiers");

        fullTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = fullTable.getClientArea();
                fullTableColumn.setWidth(r.width);
            }
        });

        mFullTableViewer = new TableViewer(fullTable);
        mFullTableViewer.setContentProvider(new QualifierContentProvider());
        mFullTableViewer.setLabelProvider(new QualifierLabelProvider(
                false /* showQualifierValue */));
        mFullTableViewer.setInput(mBaseConfiguration);
        mFullTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structSelection = (IStructuredSelection)selection;
                    Object first = structSelection.getFirstElement();

                    if (first instanceof ResourceQualifier) {
                        mAddButton.setEnabled(true);
                        return;
                    }
                }

                mAddButton.setEnabled(false);
            }
        });

        // 2nd column is the left/right arrow button
        Composite buttonComposite = new Composite(this, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.marginWidth = gl.marginHeight = 0;
        buttonComposite.setLayout(gl);
        buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        new Composite(buttonComposite, SWT.NONE);
        mAddButton = new Button(buttonComposite, SWT.BORDER | SWT.PUSH);
        mAddButton.setText("->");
        mAddButton.setEnabled(false);
        mAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection =
                    (IStructuredSelection)mFullTableViewer.getSelection();

                Object first = selection.getFirstElement();
                if (first instanceof ResourceQualifier) {
                    ResourceQualifier qualifier = (ResourceQualifier)first;

                    mBaseConfiguration.removeQualifier(qualifier);
                    mSelectedConfiguration.addQualifier(qualifier);

                    mFullTableViewer.refresh();
                    mSelectionTableViewer.refresh();
                    mSelectionTableViewer.setSelection(new StructuredSelection(qualifier), true);

                    onChange(false /* keepSelection */);
                }
            }
        });

        mRemoveButton = new Button(buttonComposite, SWT.BORDER | SWT.PUSH);
        mRemoveButton.setText("<-");
        mRemoveButton.setEnabled(false);
        mRemoveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection =
                    (IStructuredSelection)mSelectionTableViewer.getSelection();

                Object first = selection.getFirstElement();
                if (first instanceof ResourceQualifier) {
                    ResourceQualifier qualifier = (ResourceQualifier)first;

                    mSelectedConfiguration.removeQualifier(qualifier);
                    mBaseConfiguration.addQualifier(qualifier);

                    mFullTableViewer.refresh();
                    mSelectionTableViewer.refresh();

                    onChange(false /* keepSelection */);
                }
            }
        });

        // 3rd column is the selected config table
        final Table selectionTable = new Table(this, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        selectionTable.setHeaderVisible(true);
        selectionTable.setLinesVisible(true);

        // create the column
        final TableColumn selectionTableColumn = new TableColumn(selectionTable, SWT.LEFT);
        // set the header
        selectionTableColumn.setText("Chosen Qualifiers");

        selectionTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = selectionTable.getClientArea();
                selectionTableColumn.setWidth(r.width);
            }
        });
        mSelectionTableViewer = new TableViewer(selectionTable);
        mSelectionTableViewer.setContentProvider(new QualifierContentProvider());
        mSelectionTableViewer.setLabelProvider(new QualifierLabelProvider(
                true /* showQualifierValue */));
        mSelectionTableViewer.setInput(mSelectedConfiguration);
        mSelectionTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                // ignore selection changes during resfreshes in some cases.
                if (mOnRefresh) {
                    return;
                }

                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structSelection = (IStructuredSelection)selection;

                    if (structSelection.isEmpty() == false) {
                        Object first = structSelection.getFirstElement();

                        if (first instanceof ResourceQualifier) {
                            mRemoveButton.setEnabled(true);

                            QualifierEditBase composite = mUiMap.get(first.getClass());

                            if (composite != null) {
                                composite.setQualifier((ResourceQualifier)first);
                            }

                            mStackLayout.topControl = composite;
                            mQualifierEditParent.layout();

                            return;
                        }
                    } else {
                        mStackLayout.topControl = null;
                        mQualifierEditParent.layout();
                    }
                }

                mRemoveButton.setEnabled(false);
            }
        });

        // 4th column is the detail of the selected qualifier
        mQualifierEditParent = new Composite(this, SWT.NONE);
        mQualifierEditParent.setLayout(mStackLayout = new StackLayout());
        mQualifierEditParent.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        // create the UI for all the qualifiers, and associate them to the ResourceQualifer class.
        mUiMap.put(CountryCodeQualifier.class, new MCCEdit(mQualifierEditParent));
        mUiMap.put(NetworkCodeQualifier.class, new MNCEdit(mQualifierEditParent));
        mUiMap.put(LanguageQualifier.class, new LanguageEdit(mQualifierEditParent));
        mUiMap.put(RegionQualifier.class, new RegionEdit(mQualifierEditParent));
        mUiMap.put(ScreenSizeQualifier.class, new ScreenSizeEdit(mQualifierEditParent));
        mUiMap.put(ScreenRatioQualifier.class, new ScreenRatioEdit(mQualifierEditParent));
        mUiMap.put(ScreenOrientationQualifier.class, new OrientationEdit(mQualifierEditParent));
        mUiMap.put(PixelDensityQualifier.class, new PixelDensityEdit(mQualifierEditParent));
        mUiMap.put(TouchScreenQualifier.class, new TouchEdit(mQualifierEditParent));
        mUiMap.put(KeyboardStateQualifier.class, new KeyboardEdit(mQualifierEditParent));
        mUiMap.put(TextInputMethodQualifier.class, new TextInputEdit(mQualifierEditParent));
        mUiMap.put(NavigationMethodQualifier.class, new NavigationEdit(mQualifierEditParent));
        mUiMap.put(ScreenDimensionQualifier.class, new ScreenDimensionEdit(mQualifierEditParent));
        mUiMap.put(VersionQualifier.class, new VersionEdit(mQualifierEditParent));
    }

    /**
     * Sets a listener to be notified when the configuration changes.
     * @param listener A {@link Runnable} whose <code>run()</code> method is called when the
     * configuration is changed. The method is called from the UI thread.
     */
    public void setOnChangeListener(Runnable listener) {
        mOnChangeListener = listener;
    }

    /**
     * Initialize the UI with a given {@link FolderConfiguration}. This must
     * be called from the UI thread.
     * @param config The configuration.
     */
    public void setConfiguration(FolderConfiguration config) {
        mSelectedConfiguration.set(config);
        mSelectionTableViewer.refresh();

        // create the base config, which is the default config minus the qualifiers
        // in SelectedConfiguration
        mBaseConfiguration.substract(mSelectedConfiguration);
        mFullTableViewer.refresh();
    }

    /**
     * Initialize the UI with the configuration represented by a resource folder name.
     * This must be called from the UI thread.
     *
     * @param folderSegments the segments of the folder name,
     *                       split using {@link FolderConfiguration#QUALIFIER_SEP}.
     * @return true if success, or false if the folder name is not a valid name.
     */
    public boolean setConfiguration(String[] folderSegments) {
        FolderConfiguration config = ResourceManager.getInstance().getConfig(folderSegments);

        if (config == null) {
            return false;
        }

        setConfiguration(config);

        return true;
    }

    /**
     * Initialize the UI with the configuration represented by a resource folder name.
     * This must be called from the UI thread.
     * @param folderName the name of the folder.
     * @return true if success, or false if the folder name is not a valid name.
     */
    public boolean setConfiguration(String folderName) {
        // split the name of the folder in segments.
        String[] folderSegments = folderName.split(FolderConfiguration.QUALIFIER_SEP);

        return setConfiguration(folderSegments);
    }

    /**
     * Gets the configuration as setup by the widget.
     * @param config the {@link FolderConfiguration} object to be filled with the information
     * from the UI.
     */
    public void getConfiguration(FolderConfiguration config) {
        config.set(mSelectedConfiguration);
    }

    /**
     * Returns the state of the configuration being edited/created.
     */
    public ConfigurationState getState() {
        if (mSelectedConfiguration.getInvalidQualifier() != null) {
            return ConfigurationState.INVALID_CONFIG;
        }

        if (mSelectedConfiguration.checkRegion() == false) {
            return ConfigurationState.REGION_WITHOUT_LANGUAGE;
        }

        return ConfigurationState.OK;
    }

    /**
     * Returns the first invalid qualifier of the configuration being edited/created,
     * or <code>null<code> if they are all valid (or if none exists).
     * <p/>If {@link #getState()} return {@link ConfigurationState#INVALID_CONFIG} then this will
     * not return <code>null</code>.
     */
    public ResourceQualifier getInvalidQualifier() {
        return mSelectedConfiguration.getInvalidQualifier();
    }

    /**
     * Handle changes in the configuration.
     * @param keepSelection if <code>true</code> attemps to avoid triggering selection change in
     * {@link #mSelectedConfiguration}.
     */
    private void onChange(boolean keepSelection) {
        ISelection selection = null;
        if (keepSelection) {
            mOnRefresh = true;
            selection = mSelectionTableViewer.getSelection();
        }

        mSelectionTableViewer.refresh(true);

        if (keepSelection) {
            mSelectionTableViewer.setSelection(selection);
            mOnRefresh = false;
        }

        if (mOnChangeListener != null) {
            mOnChangeListener.run();
        }
    }

    /**
     * Content provider around a {@link FolderConfiguration}.
     */
    private static class QualifierContentProvider implements IStructuredContentProvider {

        private FolderConfiguration mInput;

        public QualifierContentProvider() {
        }

        public void dispose() {
            // pass
        }

        public Object[] getElements(Object inputElement) {
            return mInput.getQualifiers();
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            mInput = null;
            if (newInput instanceof FolderConfiguration) {
                mInput = (FolderConfiguration)newInput;
            }
        }
    }

    /**
     * Label provider for {@link ResourceQualifier} objects.
     */
    private static class QualifierLabelProvider implements ITableLabelProvider {

        private final boolean mShowQualifierValue;

        public QualifierLabelProvider(boolean showQualifierValue) {
            mShowQualifierValue = showQualifierValue;
        }

        public String getColumnText(Object element, int columnIndex) {
            // only one column, so we can ignore columnIndex
            if (element instanceof ResourceQualifier) {
                if (mShowQualifierValue) {
                    String value = ((ResourceQualifier)element).getStringValue();
                    if (value.length() == 0) {
                        return String.format("%1$s (?)",
                                ((ResourceQualifier)element).getShortName());
                    } else {
                        return value;
                    }

                } else {
                    return ((ResourceQualifier)element).getShortName();
                }
            }

            return null;
        }

        public Image getColumnImage(Object element, int columnIndex) {
            // only one column, so we can ignore columnIndex
            if (element instanceof ResourceQualifier) {
                return ((ResourceQualifier)element).getIcon();
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }

    /**
     * Base class for Edit widget for {@link ResourceQualifier}.
     */
    private abstract static class QualifierEditBase extends Composite {

        public QualifierEditBase(Composite parent, String title) {
            super(parent, SWT.NONE);
            setLayout(new GridLayout(1, false));

            new Label(this, SWT.NONE).setText(title);
        }

        public abstract void setQualifier(ResourceQualifier qualifier);
    }

    /**
     * Edit widget for {@link CountryCodeQualifier}.
     */
    private class MCCEdit extends QualifierEditBase {

        private Text mText;

        public MCCEdit(Composite parent) {
            super(parent, CountryCodeQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onTextChange();
                }
            });

            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onTextChange();
                }
            });

            new Label(this, SWT.NONE).setText("(3 digit code)");
        }

        private void onTextChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
            } else {
                try {
                    CountryCodeQualifier qualifier = CountryCodeQualifier.getQualifier(
                            CountryCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setCountryCodeQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        // (for instance not exactly 3 digits).
                        mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            CountryCodeQualifier q = (CountryCodeQualifier)qualifier;

            mText.setText(Integer.toString(q.getCode()));
        }
    }

    /**
     * Edit widget for {@link NetworkCodeQualifier}.
     */
    private class MNCEdit extends QualifierEditBase {
        private Text mText;

        public MNCEdit(Composite parent) {
            super(parent, NetworkCodeQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onTextChange();
                }
            });
            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onTextChange();
                }
            });

            new Label(this, SWT.NONE).setText("(1-3 digit code)");
        }

        private void onTextChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
            } else {
                try {
                    NetworkCodeQualifier qualifier = NetworkCodeQualifier.getQualifier(
                            NetworkCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setNetworkCodeQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        // (for instance not exactly 3 digits).
                        mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NetworkCodeQualifier q = (NetworkCodeQualifier)qualifier;

            mText.setText(Integer.toString(q.getCode()));
        }
    }

    /**
     * Edit widget for {@link LanguageQualifier}.
     */
    private class LanguageEdit extends QualifierEditBase {
        private Combo mLanguage;

        public LanguageEdit(Composite parent) {
            super(parent, LanguageQualifier.NAME);

            mLanguage = new Combo(this, SWT.DROP_DOWN);
            mLanguage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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

            new Label(this, SWT.NONE).setText("(2 letter code)");
        }

        private void onLanguageChange() {
            // update the current config
            String value = mLanguage.getText();

            if (value.length() == 0) {
                // empty string, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setLanguageQualifier(new LanguageQualifier());
            } else {
                LanguageQualifier qualifier = null;
                String segment = LanguageQualifier.getFolderSegment(value);
                if (segment != null) {
                    qualifier = LanguageQualifier.getQualifier(segment);
                }

                if (qualifier != null) {
                    mSelectedConfiguration.setLanguageQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    mSelectedConfiguration.setLanguageQualifier(new LanguageQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            LanguageQualifier q = (LanguageQualifier)qualifier;

            String value = q.getValue();
            if (value != null) {
                mLanguage.setText(value);
            }
        }
    }

    /**
     * Edit widget for {@link RegionQualifier}.
     */
    private class RegionEdit extends QualifierEditBase {
        private Combo mRegion;

        public RegionEdit(Composite parent) {
            super(parent, RegionQualifier.NAME);

            mRegion = new Combo(this, SWT.DROP_DOWN);
            mRegion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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

            new Label(this, SWT.NONE).setText("(2 letter code)");
        }

        private void onRegionChange() {
            // update the current config
            String value = mRegion.getText();

            if (value.length() == 0) {
                // empty string, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setRegionQualifier(new RegionQualifier());
            } else {
                RegionQualifier qualifier = null;
                String segment = RegionQualifier.getFolderSegment(value);
                if (segment != null) {
                    qualifier = RegionQualifier.getQualifier(segment);
                }

                if (qualifier != null) {
                    mSelectedConfiguration.setRegionQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    mSelectedConfiguration.setRegionQualifier(new RegionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            RegionQualifier q = (RegionQualifier)qualifier;

            String value = q.getValue();
            if (value != null) {
                mRegion.setText(q.getValue());
            }
        }
    }

    /**
     * Edit widget for {@link ScreenSizeQualifier}.
     */
    private class ScreenSizeEdit extends QualifierEditBase {

        private Combo mSize;

        public ScreenSizeEdit(Composite parent) {
            super(parent, ScreenSizeQualifier.NAME);

            mSize = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            ScreenSize[] ssValues = ScreenSize.values();
            for (ScreenSize value : ssValues) {
                mSize.add(value.getDisplayValue());
            }

            mSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mSize.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onScreenSizeChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onScreenSizeChange();
                }
            });
        }

        protected void onScreenSizeChange() {
            // update the current config
            int index = mSize.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenSizeQualifier(new ScreenSizeQualifier(
                    ScreenSize.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenSizeQualifier(
                        new ScreenSizeQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenSizeQualifier q = (ScreenSizeQualifier)qualifier;

            ScreenSize value = q.getValue();
            if (value == null) {
                mSize.clearSelection();
            } else {
                mSize.select(ScreenSize.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenRatioQualifier}.
     */
    private class ScreenRatioEdit extends QualifierEditBase {

        private Combo mSize;

        public ScreenRatioEdit(Composite parent) {
            super(parent, ScreenRatioQualifier.NAME);

            mSize = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            ScreenRatio[] srValues = ScreenRatio.values();
            for (ScreenRatio value : srValues) {
                mSize.add(value.getDisplayValue());
            }

            mSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mSize.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onScreenRatioChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onScreenRatioChange();
                }
            });
        }

        protected void onScreenRatioChange() {
            // update the current config
            int index = mSize.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenRatioQualifier(new ScreenRatioQualifier(
                        ScreenRatio.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenRatioQualifier(
                        new ScreenRatioQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenRatioQualifier q = (ScreenRatioQualifier)qualifier;

            ScreenRatio value = q.getValue();
            if (value == null) {
                mSize.clearSelection();
            } else {
                mSize.select(ScreenRatio.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenOrientationQualifier}.
     */
    private class OrientationEdit extends QualifierEditBase {

        private Combo mOrientation;

        public OrientationEdit(Composite parent) {
            super(parent, ScreenOrientationQualifier.NAME);

            mOrientation = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            ScreenOrientation[] soValues = ScreenOrientation.values();
            for (ScreenOrientation value : soValues) {
                mOrientation.add(value.getDisplayValue());
            }

            mOrientation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mOrientation.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onOrientationChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onOrientationChange();
                }
            });
        }

        protected void onOrientationChange() {
            // update the current config
            int index = mOrientation.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenOrientationQualifier(new ScreenOrientationQualifier(
                    ScreenOrientation.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenOrientationQualifier(
                        new ScreenOrientationQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenOrientationQualifier q = (ScreenOrientationQualifier)qualifier;

            ScreenOrientation value = q.getValue();
            if (value == null) {
                mOrientation.clearSelection();
            } else {
                mOrientation.select(ScreenOrientation.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link PixelDensityQualifier}.
     */
    private class PixelDensityEdit extends QualifierEditBase {
        private Combo mDensity;

        public PixelDensityEdit(Composite parent) {
            super(parent, PixelDensityQualifier.NAME);

            mDensity = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            Density[] soValues = Density.values();
            for (Density value : soValues) {
                mDensity.add(value.getDisplayValue());
            }

            mDensity.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mDensity.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onDensityChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onDensityChange();
                }
            });

        }

        private void onDensityChange() {
            // update the current config
            int index = mDensity.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setPixelDensityQualifier(new PixelDensityQualifier(
                    Density.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setPixelDensityQualifier(
                        new PixelDensityQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            PixelDensityQualifier q = (PixelDensityQualifier)qualifier;

            Density value = q.getValue();
            if (value == null) {
                mDensity.clearSelection();
            } else {
                mDensity.select(Density.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link TouchScreenQualifier}.
     */
    private class TouchEdit extends QualifierEditBase {

        private Combo mTouchScreen;

        public TouchEdit(Composite parent) {
            super(parent, TouchScreenQualifier.NAME);

            mTouchScreen = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            TouchScreenType[] tstValues = TouchScreenType.values();
            for (TouchScreenType value : tstValues) {
                mTouchScreen.add(value.getDisplayValue());
            }

            mTouchScreen.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mTouchScreen.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onTouchChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onTouchChange();
                }
            });
        }

        protected void onTouchChange() {
            // update the current config
            int index = mTouchScreen.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setTouchTypeQualifier(new TouchScreenQualifier(
                        TouchScreenType.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setTouchTypeQualifier(new TouchScreenQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            TouchScreenQualifier q = (TouchScreenQualifier)qualifier;

            TouchScreenType value = q.getValue();
            if (value == null) {
                mTouchScreen.clearSelection();
            } else {
                mTouchScreen.select(TouchScreenType.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link KeyboardStateQualifier}.
     */
    private class KeyboardEdit extends QualifierEditBase {

        private Combo mKeyboard;

        public KeyboardEdit(Composite parent) {
            super(parent, KeyboardStateQualifier.NAME);

            mKeyboard = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            KeyboardState[] ksValues = KeyboardState.values();
            for (KeyboardState value : ksValues) {
                mKeyboard.add(value.getDisplayValue());
            }

            mKeyboard.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mKeyboard.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onKeyboardChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onKeyboardChange();
                }
            });
        }

        protected void onKeyboardChange() {
            // update the current config
            int index = mKeyboard.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setKeyboardStateQualifier(new KeyboardStateQualifier(
                        KeyboardState.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setKeyboardStateQualifier(
                        new KeyboardStateQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            KeyboardStateQualifier q = (KeyboardStateQualifier)qualifier;

            KeyboardState value = q.getValue();
            if (value == null) {
                mKeyboard.clearSelection();
            } else {
                mKeyboard.select(KeyboardState.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link TextInputMethodQualifier}.
     */
    private class TextInputEdit extends QualifierEditBase {

        private Combo mTextInput;

        public TextInputEdit(Composite parent) {
            super(parent, TextInputMethodQualifier.NAME);

            mTextInput = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            TextInputMethod[] timValues = TextInputMethod.values();
            for (TextInputMethod value : timValues) {
                mTextInput.add(value.getDisplayValue());
            }

            mTextInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mTextInput.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onTextInputChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onTextInputChange();
                }
            });
        }

        protected void onTextInputChange() {
            // update the current config
            int index = mTextInput.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setTextInputMethodQualifier(new TextInputMethodQualifier(
                        TextInputMethod.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setTextInputMethodQualifier(
                        new TextInputMethodQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            TextInputMethodQualifier q = (TextInputMethodQualifier)qualifier;

            TextInputMethod value = q.getValue();
            if (value == null) {
                mTextInput.clearSelection();
            } else {
                mTextInput.select(TextInputMethod.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link NavigationMethodQualifier}.
     */
    private class NavigationEdit extends QualifierEditBase {

        private Combo mNavigation;

        public NavigationEdit(Composite parent) {
            super(parent, NavigationMethodQualifier.NAME);

            mNavigation = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            NavigationMethod[] nmValues = NavigationMethod.values();
            for (NavigationMethod value : nmValues) {
                mNavigation.add(value.getDisplayValue());
            }

            mNavigation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mNavigation.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onNavigationChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onNavigationChange();
                }
            });
        }

        protected void onNavigationChange() {
            // update the current config
            int index = mNavigation.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setNavigationMethodQualifier(new NavigationMethodQualifier(
                        NavigationMethod.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNavigationMethodQualifier(
                        new NavigationMethodQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NavigationMethodQualifier q = (NavigationMethodQualifier)qualifier;

            NavigationMethod value = q.getValue();
            if (value == null) {
                mNavigation.clearSelection();
            } else {
                mNavigation.select(NavigationMethod.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenDimensionQualifier}.
     */
    private class ScreenDimensionEdit extends QualifierEditBase {

        private Text mSize1;
        private Text mSize2;

        public ScreenDimensionEdit(Composite parent) {
            super(parent, ScreenDimensionQualifier.NAME);

            ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onSizeChange();
                }
            };

            FocusAdapter focusListener = new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onSizeChange();
                }
            };

            mSize1 = new Text(this, SWT.BORDER);
            mSize1.addVerifyListener(new DimensionVerifier());
            mSize1.addModifyListener(modifyListener);
            mSize1.addFocusListener(focusListener);

            mSize2 = new Text(this, SWT.BORDER);
            mSize2.addVerifyListener(new DimensionVerifier());
            mSize2.addModifyListener(modifyListener);
            mSize2.addFocusListener(focusListener);
        }

        private void onSizeChange() {
            // update the current config
            String size1 = mSize1.getText();
            String size2 = mSize2.getText();

            if (size1.length() == 0 || size2.length() == 0) {
                // if one of the strings is empty, reset to no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenDimensionQualifier(new ScreenDimensionQualifier());
            } else {
                ScreenDimensionQualifier qualifier = ScreenDimensionQualifier.getQualifier(size1,
                        size2);

                if (qualifier != null) {
                    mSelectedConfiguration.setScreenDimensionQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong, reset the qualifier
                    // Since the qualifier classes are immutable, and we don't want to
                    // remove the qualifier from the configuration, we create a new default one.
                    mSelectedConfiguration.setScreenDimensionQualifier(
                            new ScreenDimensionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenDimensionQualifier q = (ScreenDimensionQualifier)qualifier;

            mSize1.setText(Integer.toString(q.getValue1()));
            mSize2.setText(Integer.toString(q.getValue2()));
        }
    }

    /**
     * Edit widget for {@link VersionQualifier}.
     */
    private class VersionEdit extends QualifierEditBase {
        private Text mText;

        public VersionEdit(Composite parent) {
            super(parent, VersionQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onVersionChange();
                }
            });
            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onVersionChange();
                }
            });

            new Label(this, SWT.NONE).setText("(Platform API level)");
        }

        private void onVersionChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
            } else {
                try {
                    VersionQualifier qualifier = VersionQualifier.getQualifier(
                            VersionQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setVersionQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            VersionQualifier q = (VersionQualifier)qualifier;

            mText.setText(Integer.toString(q.getVersion()));
        }
    }

}
