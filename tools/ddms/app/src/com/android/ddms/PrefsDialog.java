/* //device/tools/ddms/src/com/android/ddms/PrefsDialog.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.ddms;

import com.android.ddmlib.DdmConstants;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.PortFieldEditor;
import com.android.sdkstats.SdkStatsService;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.IOException;

/**
 * Preferences dialog.
 */
public final class PrefsDialog {

    // Preference store.
    private static PreferenceStore mPrefStore;

    // public const values for storage
    public final static String SHELL_X = "shellX"; //$NON-NLS-1$
    public final static String SHELL_Y = "shellY"; //$NON-NLS-1$
    public final static String SHELL_WIDTH = "shellWidth"; //$NON-NLS-1$
    public final static String SHELL_HEIGHT = "shellHeight"; //$NON-NLS-1$
    public final static String EXPLORER_SHELL_X = "explorerShellX"; //$NON-NLS-1$
    public final static String EXPLORER_SHELL_Y = "explorerShellY"; //$NON-NLS-1$
    public final static String EXPLORER_SHELL_WIDTH = "explorerShellWidth"; //$NON-NLS-1$
    public final static String EXPLORER_SHELL_HEIGHT = "explorerShellHeight"; //$NON-NLS-1$
    public final static String SHOW_NATIVE_HEAP = "native"; //$NON-NLS-1$

    public final static String LOGCAT_COLUMN_MODE = "ddmsLogColumnMode"; //$NON-NLS-1$
    public final static String LOGCAT_FONT = "ddmsLogFont"; //$NON-NLS-1$

    public final static String LOGCAT_COLUMN_MODE_AUTO = "auto"; //$NON-NLS-1$
    public final static String LOGCAT_COLUMN_MODE_MANUAL = "manual"; //$NON-NLS-1$

    private final static String PREFS_DEBUG_PORT_BASE = "adbDebugBasePort"; //$NON-NLS-1$
    private final static String PREFS_SELECTED_DEBUG_PORT = "debugSelectedPort"; //$NON-NLS-1$
    private final static String PREFS_DEFAULT_THREAD_UPDATE = "defaultThreadUpdateEnabled"; //$NON-NLS-1$
    private final static String PREFS_DEFAULT_HEAP_UPDATE = "defaultHeapUpdateEnabled"; //$NON-NLS-1$
    private final static String PREFS_THREAD_REFRESH_INTERVAL = "threadStatusInterval"; //$NON-NLS-1$
    private final static String PREFS_LOG_LEVEL = "ddmsLogLevel"; //$NON-NLS-1$
    private final static String PREFS_TIMEOUT = "timeOut"; //$NON-NLS-1$


    /**
     * Private constructor -- do not instantiate.
     */
    private PrefsDialog() {}

    /**
     * Return the PreferenceStore that holds our values.
     */
    public static PreferenceStore getStore() {
        return mPrefStore;
    }

    /**
     * Save the prefs to the config file.
     */
    public static void save() {
        try {
            mPrefStore.save();
        }
        catch (IOException ioe) {
            Log.w("ddms", "Failed saving prefs file: " + ioe.getMessage());
        }
    }

    /**
     * Do some one-time prep.
     *
     * The original plan was to let the individual classes define their
     * own defaults, which we would get and then override with the config
     * file.  However, PreferencesStore.load() doesn't trigger the "changed"
     * events, which means we have to pull the loaded config values out by
     * hand.
     *
     * So, we set the defaults, load the values from the config file, and
     * then run through and manually export the values.  Then we duplicate
     * the second part later on for the "changed" events.
     */
    public static void init() {
        assert mPrefStore == null;

        mPrefStore = SdkStatsService.getPreferenceStore();

        if (mPrefStore == null) {
            // we have a serious issue here...
            Log.e("ddms",
                    "failed to access both the user HOME directory and the system wide temp folder. Quitting.");
            System.exit(1);
        }

        // configure default values
        setDefaults(System.getProperty("user.home")); //$NON-NLS-1$

        // listen for changes
        mPrefStore.addPropertyChangeListener(new ChangeListener());

        // Now we initialize the value of the preference, from the values in the store.

        // First the ddm lib.
        DdmPreferences.setDebugPortBase(mPrefStore.getInt(PREFS_DEBUG_PORT_BASE));
        DdmPreferences.setSelectedDebugPort(mPrefStore.getInt(PREFS_SELECTED_DEBUG_PORT));
        DdmPreferences.setLogLevel(mPrefStore.getString(PREFS_LOG_LEVEL));
        DdmPreferences.setInitialThreadUpdate(mPrefStore.getBoolean(PREFS_DEFAULT_THREAD_UPDATE));
        DdmPreferences.setInitialHeapUpdate(mPrefStore.getBoolean(PREFS_DEFAULT_HEAP_UPDATE));
        DdmPreferences.setTimeOut(mPrefStore.getInt(PREFS_TIMEOUT));

        // some static values
        String out = System.getenv("ANDROID_PRODUCT_OUT"); //$NON-NLS-1$
        DdmUiPreferences.setSymbolsLocation(out + File.separator + "symbols"); //$NON-NLS-1$
        DdmUiPreferences.setAddr2LineLocation("arm-eabi-addr2line"); //$NON-NLS-1$

        String traceview = System.getProperty("com.android.ddms.bindir");  //$NON-NLS-1$
        if (traceview != null && traceview.length() != 0) {
            traceview += File.separator + DdmConstants.FN_TRACEVIEW;
        } else {
            traceview = DdmConstants.FN_TRACEVIEW;
        }
        DdmUiPreferences.setTraceviewLocation(traceview);

        // Now the ddmui lib
        DdmUiPreferences.setStore(mPrefStore);
        DdmUiPreferences.setThreadRefreshInterval(mPrefStore.getInt(PREFS_THREAD_REFRESH_INTERVAL));
    }

    /*
     * Set default values for all preferences.  These are either defined
     * statically or are based on the values set by the class initializers
     * in other classes.
     *
     * The other threads (e.g. VMWatcherThread) haven't been created yet,
     * so we want to use static values rather than reading fields from
     * class.getInstance().
     */
    private static void setDefaults(String homeDir) {
        mPrefStore.setDefault(PREFS_DEBUG_PORT_BASE, DdmPreferences.DEFAULT_DEBUG_PORT_BASE);

        mPrefStore.setDefault(PREFS_SELECTED_DEBUG_PORT,
                DdmPreferences.DEFAULT_SELECTED_DEBUG_PORT);

        mPrefStore.setDefault(PREFS_DEFAULT_THREAD_UPDATE, true);
        mPrefStore.setDefault(PREFS_DEFAULT_HEAP_UPDATE, false);
        mPrefStore.setDefault(PREFS_THREAD_REFRESH_INTERVAL,
            DdmUiPreferences.DEFAULT_THREAD_REFRESH_INTERVAL);

        mPrefStore.setDefault("textSaveDir", homeDir); //$NON-NLS-1$
        mPrefStore.setDefault("imageSaveDir", homeDir); //$NON-NLS-1$

        mPrefStore.setDefault(PREFS_LOG_LEVEL, "info"); //$NON-NLS-1$

        mPrefStore.setDefault(PREFS_TIMEOUT, DdmPreferences.DEFAULT_TIMEOUT);

        // choose a default font for the text output
        FontData fdat = new FontData("Courier", 10, SWT.NORMAL); //$NON-NLS-1$
        mPrefStore.setDefault("textOutputFont", fdat.toString()); //$NON-NLS-1$

        // layout information.
        mPrefStore.setDefault(SHELL_X, 100);
        mPrefStore.setDefault(SHELL_Y, 100);
        mPrefStore.setDefault(SHELL_WIDTH, 800);
        mPrefStore.setDefault(SHELL_HEIGHT, 600);

        mPrefStore.setDefault(EXPLORER_SHELL_X, 50);
        mPrefStore.setDefault(EXPLORER_SHELL_Y, 50);

        mPrefStore.setDefault(SHOW_NATIVE_HEAP, false);
    }


    /*
     * Create a "listener" to take action when preferences change.  These are
     * required for ongoing activities that don't check prefs on each use.
     *
     * This is only invoked when something explicitly changes the value of
     * a preference (e.g. not when the prefs file is loaded).
     */
    private static class ChangeListener implements IPropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            String changed = event.getProperty();

            if (changed.equals(PREFS_DEBUG_PORT_BASE)) {
                DdmPreferences.setDebugPortBase(mPrefStore.getInt(PREFS_DEBUG_PORT_BASE));
            } else if (changed.equals(PREFS_SELECTED_DEBUG_PORT)) {
                DdmPreferences.setSelectedDebugPort(mPrefStore.getInt(PREFS_SELECTED_DEBUG_PORT));
            } else if (changed.equals(PREFS_LOG_LEVEL)) {
                DdmPreferences.setLogLevel((String)event.getNewValue());
            } else if (changed.equals("textSaveDir")) {
                mPrefStore.setValue("lastTextSaveDir",
                    (String) event.getNewValue());
            } else if (changed.equals("imageSaveDir")) {
                mPrefStore.setValue("lastImageSaveDir",
                    (String) event.getNewValue());
            } else if (changed.equals(PREFS_TIMEOUT)) {
                DdmPreferences.setTimeOut(mPrefStore.getInt(PREFS_TIMEOUT));
            } else {
                Log.v("ddms", "Preference change: " + event.getProperty()
                    + ": '" + event.getOldValue()
                    + "' --> '" + event.getNewValue() + "'");
            }
        }
    }


    /**
     * Create and display the dialog.
     */
    public static void run(Shell shell) {
        assert mPrefStore != null;

        PreferenceManager prefMgr = new PreferenceManager();

        PreferenceNode node, subNode;

        // this didn't work -- got NPE, possibly from class lookup:
        //PreferenceNode app = new PreferenceNode("app", "Application", null,
        //    AppPrefs.class.getName());

        node = new PreferenceNode("debugger", new DebuggerPrefs());
        prefMgr.addToRoot(node);

        subNode = new PreferenceNode("panel", new PanelPrefs());
        //prefMgr.addTo(node.getId(), subNode);
        prefMgr.addToRoot(subNode);

        node = new PreferenceNode("LogCat", new LogCatPrefs());
        prefMgr.addToRoot(node);

        node = new PreferenceNode("misc", new MiscPrefs());
        prefMgr.addToRoot(node);

        node = new PreferenceNode("stats", new UsageStatsPrefs());
        prefMgr.addToRoot(node);

        PreferenceDialog dlg = new PreferenceDialog(shell, prefMgr);
        dlg.setPreferenceStore(mPrefStore);

        // run it
        dlg.open();

        // save prefs
        try {
            mPrefStore.save();
        }
        catch (IOException ioe) {
        }

        // discard the stuff we created
        //prefMgr.dispose();
        //dlg.dispose();
    }

    /**
     * "Debugger" prefs page.
     */
    private static class DebuggerPrefs extends FieldEditorPreferencePage {

        /**
         * Basic constructor.
         */
        public DebuggerPrefs() {
            super(GRID);        // use "grid" layout so edit boxes line up
            setTitle("Debugger");
        }

         /**
         * Create field editors.
         */
        @Override
        protected void createFieldEditors() {
            IntegerFieldEditor ife;

            ife = new PortFieldEditor(PREFS_DEBUG_PORT_BASE,
                "Starting value for local port:", getFieldEditorParent());
            addField(ife);

            ife = new PortFieldEditor(PREFS_SELECTED_DEBUG_PORT,
                "Port of Selected VM:", getFieldEditorParent());
            addField(ife);
        }
    }

    /**
     * "Panel" prefs page.
     */
    private static class PanelPrefs extends FieldEditorPreferencePage {

        /**
         * Basic constructor.
         */
        public PanelPrefs() {
            super(FLAT);        // use "flat" layout
            setTitle("Info Panels");
        }

        /**
         * Create field editors.
         */
        @Override
        protected void createFieldEditors() {
            BooleanFieldEditor bfe;
            IntegerFieldEditor ife;

            bfe = new BooleanFieldEditor(PREFS_DEFAULT_THREAD_UPDATE,
                "Thread updates enabled by default", getFieldEditorParent());
            addField(bfe);

            bfe = new BooleanFieldEditor(PREFS_DEFAULT_HEAP_UPDATE,
                "Heap updates enabled by default", getFieldEditorParent());
            addField(bfe);

            ife = new IntegerFieldEditor(PREFS_THREAD_REFRESH_INTERVAL,
                "Thread status interval (seconds):", getFieldEditorParent());
            ife.setValidRange(1, 60);
            addField(ife);
        }
    }

    /**
     * "logcat" prefs page.
     */
    private static class LogCatPrefs extends FieldEditorPreferencePage {

        /**
         * Basic constructor.
         */
        public LogCatPrefs() {
            super(FLAT);        // use "flat" layout
            setTitle("Logcat");
        }

        /**
         * Create field editors.
         */
        @Override
        protected void createFieldEditors() {
            RadioGroupFieldEditor rgfe;

            rgfe = new RadioGroupFieldEditor(PrefsDialog.LOGCAT_COLUMN_MODE,
                "Message Column Resizing Mode", 1, new String[][] {
                    { "Manual", PrefsDialog.LOGCAT_COLUMN_MODE_MANUAL },
                    { "Automatic", PrefsDialog.LOGCAT_COLUMN_MODE_AUTO },
                    },
                getFieldEditorParent(), true);
            addField(rgfe);

            FontFieldEditor ffe = new FontFieldEditor(PrefsDialog.LOGCAT_FONT, "Text output font:",
                    getFieldEditorParent());
            addField(ffe);
        }
    }

    /**
     * "misc" prefs page.
     */
    private static class MiscPrefs extends FieldEditorPreferencePage {

        /**
         * Basic constructor.
         */
        public MiscPrefs() {
            super(FLAT);        // use "flat" layout
            setTitle("Misc");
        }

        /**
         * Create field editors.
         */
        @Override
        protected void createFieldEditors() {
            DirectoryFieldEditor dfe;
            FontFieldEditor ffe;

            IntegerFieldEditor ife = new IntegerFieldEditor(PREFS_TIMEOUT,
                    "ADB connection time out (ms):", getFieldEditorParent());
            addField(ife);

            dfe = new DirectoryFieldEditor("textSaveDir",
                "Default text save dir:", getFieldEditorParent());
            addField(dfe);

            dfe = new DirectoryFieldEditor("imageSaveDir",
                "Default image save dir:", getFieldEditorParent());
            addField(dfe);

            ffe = new FontFieldEditor("textOutputFont", "Text output font:",
                getFieldEditorParent());
            addField(ffe);

            RadioGroupFieldEditor rgfe;

            rgfe = new RadioGroupFieldEditor(PREFS_LOG_LEVEL,
                "Logging Level", 1, new String[][] {
                    { "Verbose", LogLevel.VERBOSE.getStringValue() },
                    { "Debug", LogLevel.DEBUG.getStringValue() },
                    { "Info", LogLevel.INFO.getStringValue() },
                    { "Warning", LogLevel.WARN.getStringValue() },
                    { "Error", LogLevel.ERROR.getStringValue() },
                    { "Assert", LogLevel.ASSERT.getStringValue() },
                    },
                getFieldEditorParent(), true);
            addField(rgfe);
        }
    }

    /**
     * "Device" prefs page.
     */
    private static class UsageStatsPrefs extends PreferencePage {

        private BooleanFieldEditor mOptInCheckbox;
        private Composite mTop;

        /**
         * Basic constructor.
         */
        public UsageStatsPrefs() {
            setTitle("Usage Stats");
        }

        @Override
        protected Control createContents(Composite parent) {
            mTop = new Composite(parent, SWT.NONE);
            mTop.setLayout(new GridLayout(1, false));
            mTop.setLayoutData(new GridData(GridData.FILL_BOTH));

            Link text = new Link(mTop, SWT.WRAP);
            text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            text.setText(SdkStatsService.BODY_TEXT);

            text.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    SdkStatsService.openUrl(event.text);
                }
            });

            mOptInCheckbox = new BooleanFieldEditor(SdkStatsService.PING_OPT_IN,
                    SdkStatsService.CHECKBOX_TEXT, mTop);
            mOptInCheckbox.setPage(this);
            mOptInCheckbox.setPreferenceStore(getPreferenceStore());
            mOptInCheckbox.load();

            return null;
        }

        @Override
        protected Point doComputeSize() {
            if (mTop != null) {
                return mTop.computeSize(450, SWT.DEFAULT, true);
            }

            return super.doComputeSize();
        }

        @Override
        protected void performDefaults() {
            if (mOptInCheckbox != null) {
                mOptInCheckbox.loadDefault();
            }
            super.performDefaults();
        }

        @Override
        public void performApply() {
            if (mOptInCheckbox != null) {
                mOptInCheckbox.store();
            }
            super.performApply();
        }

        @Override
        public boolean performOk() {
            if (mOptInCheckbox != null) {
                mOptInCheckbox.store();
            }
            return super.performOk();
        }
    }

}


