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

package com.android.ide.eclipse.adt;

import com.android.ddmuilib.StackTracePanel;
import com.android.ddmuilib.StackTracePanel.ISourceRevealer;
import com.android.ddmuilib.console.DdmConsole;
import com.android.ddmuilib.console.IDdmConsole;
import com.android.ide.eclipse.adt.internal.VersionCheck;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor;
import com.android.ide.eclipse.adt.internal.editors.menu.MenuEditor;
import com.android.ide.eclipse.adt.internal.editors.resources.ResourcesEditor;
import com.android.ide.eclipse.adt.internal.editors.xml.XmlEditor;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.preferences.BuildPreferencePage;
import com.android.ide.eclipse.adt.internal.project.AndroidClasspathContainerInitializer;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ExportHelper;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.project.ExportHelper.IExportCallback;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolder;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetParser;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.ide.eclipse.adt.internal.ui.EclipseUiHelper;
import com.android.ide.eclipse.adt.internal.wizards.export.ExportWizard;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.ImageLoader;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdkstats.SdkStatsService;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * The activator class controls the plug-in life cycle
 */
public class AdtPlugin extends AbstractUIPlugin {
    /** The plug-in ID */
    public static final String PLUGIN_ID = "com.android.ide.eclipse.adt"; //$NON-NLS-1$

    public final static String PREFS_SDK_DIR = PLUGIN_ID + ".sdk"; //$NON-NLS-1$

    public final static String PREFS_RES_AUTO_REFRESH = PLUGIN_ID + ".resAutoRefresh"; //$NON-NLS-1$

    public final static String PREFS_BUILD_VERBOSITY = PLUGIN_ID + ".buildVerbosity"; //$NON-NLS-1$

    public final static String PREFS_DEFAULT_DEBUG_KEYSTORE = PLUGIN_ID + ".defaultDebugKeyStore"; //$NON-NLS-1$

    public final static String PREFS_CUSTOM_DEBUG_KEYSTORE = PLUGIN_ID + ".customDebugKeyStore"; //$NON-NLS-1$

    public final static String PREFS_HOME_PACKAGE = PLUGIN_ID + ".homePackage"; //$NON-NLS-1$

    public final static String PREFS_EMU_OPTIONS = PLUGIN_ID + ".emuOptions"; //$NON-NLS-1$

    /** singleton instance */
    private static AdtPlugin sPlugin;

    private static Image sAndroidLogo;
    private static ImageDescriptor sAndroidLogoDesc;

    /** default store, provided by eclipse */
    private IPreferenceStore mStore;

    /** cached location for the sdk folder */
    private String mOsSdkLocation;

    /** The global android console */
    private MessageConsole mAndroidConsole;

    /** Stream to write in the android console */
    private MessageConsoleStream mAndroidConsoleStream;

    /** Stream to write error messages to the android console */
    private MessageConsoleStream mAndroidConsoleErrorStream;

    /** Image loader object */
    private ImageLoader mLoader;

    /** Verbosity of the build */
    private int mBuildVerbosity = AdtConstants.BUILD_NORMAL;

    /** Color used in the error console */
    private Color mRed;

    /** Load status of the SDK. Any access MUST be in a synchronized(mPostLoadProjects) block */
    private LoadStatus mSdkIsLoaded = LoadStatus.LOADING;
    /** Project to update once the SDK is loaded.
     * Any access MUST be in a synchronized(mPostLoadProjectsToResolve) block */
    private final ArrayList<IJavaProject> mPostLoadProjectsToResolve =
            new ArrayList<IJavaProject>();
    /** Project to check validity of cache vs actual once the SDK is loaded.
     * Any access MUST be in a synchronized(mPostLoadProjectsToResolve) block */
    private final ArrayList<IJavaProject> mPostLoadProjectsToCheck = new ArrayList<IJavaProject>();

    private ResourceMonitor mResourceMonitor;
    private ArrayList<ITargetChangeListener> mTargetChangeListeners =
            new ArrayList<ITargetChangeListener>();

    protected boolean mSdkIsLoading;

    /**
     * Custom PrintStream for Dx output. This class overrides the method
     * <code>println()</code> and adds the standard output tag with the
     * date and the project name in front of every messages.
     */
    private static final class AndroidPrintStream extends PrintStream {
        private IProject mProject;
        private String mPrefix;

        /**
         * Default constructor with project and output stream.
         * The project is used to get the project name for the output tag.
         *
         * @param project The Project
         * @param prefix A prefix to be printed before the actual message. Can be null
         * @param stream The Stream
         */
        public AndroidPrintStream(IProject project, String prefix, OutputStream stream) {
            super(stream);
            mProject = project;
        }

        @Override
        public void println(String message) {
            // write the date/project tag first.
            String tag = getMessageTag(mProject != null ? mProject.getName() : null);

            print(tag);
            if (mPrefix != null) {
                print(mPrefix);
            }

            // then write the regular message
            super.println(message);
        }
    }

    /**
     * An error handler for checkSdkLocationAndId() that will handle the generated error
     * or warning message. Each method must return a boolean that will in turn be returned by
     * checkSdkLocationAndId.
     */
    public static abstract class CheckSdkErrorHandler {
        /** Handle an error message during sdk location check. Returns whatever
         * checkSdkLocationAndId() should returns.
         */
        public abstract boolean handleError(String message);

        /** Handle a warning message during sdk location check. Returns whatever
         * checkSdkLocationAndId() should returns.
         */
        public abstract boolean handleWarning(String message);
    }

    /**
     * The constructor
     */
    public AdtPlugin() {
        sPlugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        Display display = getDisplay();

        // set the default android console.
        mAndroidConsole = new MessageConsole("Android", null); //$NON-NLS-1$
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
                new IConsole[] { mAndroidConsole });

        // get the stream to write in the android console.
        mAndroidConsoleStream = mAndroidConsole.newMessageStream();
        mAndroidConsoleErrorStream = mAndroidConsole.newMessageStream();
        mRed = new Color(display, 0xFF, 0x00, 0x00);

        // because this can be run, in some cases, by a non ui thread, and beccause
        // changing the console properties update the ui, we need to make this change
        // in the ui thread.
        display.asyncExec(new Runnable() {
            public void run() {
                mAndroidConsoleErrorStream.setColor(mRed);
            }
        });

        // set up the ddms console to use this objects
        DdmConsole.setConsole(new IDdmConsole() {
            public void printErrorToConsole(String message) {
                AdtPlugin.printErrorToConsole((String)null, message);
            }
            public void printErrorToConsole(String[] messages) {
                AdtPlugin.printErrorToConsole((String)null, (Object[])messages);
            }
            public void printToConsole(String message) {
                AdtPlugin.printToConsole((String)null, message);
            }
            public void printToConsole(String[] messages) {
                AdtPlugin.printToConsole((String)null, (Object[])messages);
            }
        });

        // get the eclipse store
        mStore = getPreferenceStore();

        // set the listener for the preference change
        Preferences prefs = getPluginPreferences();
        prefs.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // get the name of the property that changed.
                String property = event.getProperty();

                // if the SDK changed, we update the cached version
                if (PREFS_SDK_DIR.equals(property)) {

                    // get the new one from the preferences
                    mOsSdkLocation = (String)event.getNewValue();

                    // make sure it ends with a separator
                    if (mOsSdkLocation.endsWith(File.separator) == false) {
                        mOsSdkLocation = mOsSdkLocation + File.separator;
                    }

                    // finally restart adb, in case it's a different version
                    DdmsPlugin.setAdb(getOsAbsoluteAdb(), true /* startAdb */);

                    // get the SDK location and build id.
                    if (checkSdkLocationAndId()) {
                        // if sdk if valid, reparse it

                        reparseSdk();
                    }
                } else if (PREFS_BUILD_VERBOSITY.equals(property)) {
                    mBuildVerbosity = BuildPreferencePage.getBuildLevel(
                            mStore.getString(PREFS_BUILD_VERBOSITY));
                }
            }
        });

        mOsSdkLocation = mStore.getString(PREFS_SDK_DIR);

        // make sure it ends with a separator. Normally this is done when the preference
        // is set. But to make sure older version still work, we fix it here as well.
        if (mOsSdkLocation.length() > 0 && mOsSdkLocation.endsWith(File.separator) == false) {
            mOsSdkLocation = mOsSdkLocation + File.separator;
        }

        // check the location of SDK
        final boolean isSdkLocationValid = checkSdkLocationAndId();

        mBuildVerbosity = BuildPreferencePage.getBuildLevel(
                mStore.getString(PREFS_BUILD_VERBOSITY));

        // create the loader that's able to load the images
        mLoader = new ImageLoader(this);

        // start the DdmsPlugin by setting the adb location, only if it is set already.
        if (mOsSdkLocation.length() > 0) {
            DdmsPlugin.setAdb(getOsAbsoluteAdb(), true);
        }

        // and give it the debug launcher for android projects
        DdmsPlugin.setRunningAppDebugLauncher(new DdmsPlugin.IDebugLauncher() {
            public boolean debug(String appName, int port) {
                // search for an android project matching the process name
                IProject project = ProjectHelper.findAndroidProjectByAppName(appName);
                if (project != null) {
                    AndroidLaunchController.debugRunningApp(project, port);
                    return true;
                } else {
                    return false;
                }
            }
        });

        StackTracePanel.setSourceRevealer(new ISourceRevealer() {
            public void reveal(String applicationName, String className, int line) {
                IProject project = ProjectHelper.findAndroidProjectByAppName(applicationName);
                if (project != null) {
                    BaseProjectHelper.revealSource(project, className, line);
                }
            }
        });

        // setup export callback for editors
        ExportHelper.setCallback(new IExportCallback() {
            public void startExportWizard(IProject project) {
                StructuredSelection selection = new StructuredSelection(project);

                ExportWizard wizard = new ExportWizard();
                wizard.init(PlatformUI.getWorkbench(), selection);
                WizardDialog dialog = new WizardDialog(getDisplay().getActiveShell(),
                        wizard);
                dialog.open();
            }
        });

        // initialize editors
        startEditors();

        // Ping the usage server and parse the SDK content.
        // This is deferred in separate jobs to avoid blocking the bundle start.
        // We also serialize them to avoid too many parallel jobs when Eclipse starts.
        Job pingJob = createPingUsageServerJob();
        pingJob.addJobChangeListener(new JobChangeAdapter() {
           @Override
            public void done(IJobChangeEvent event) {
                super.done(event);

                // Once the ping job is finished, start the SDK parser
                if (isSdkLocationValid) {
                    // parse the SDK resources.
                    parseSdkContent();
                }
            }
        });
        // build jobs are run after other interactive jobs
        pingJob.setPriority(Job.BUILD);
        // Wait 2 seconds before starting the ping job. This leaves some time to the
        // other bundles to initialize.
        pingJob.schedule(2000 /*milliseconds*/);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);

        stopEditors();

        mRed.dispose();
        synchronized (AdtPlugin.class) {
            sPlugin = null;
        }
    }

    /** Return the image loader for the plugin */
    public static synchronized ImageLoader getImageLoader() {
        if (sPlugin != null) {
            return sPlugin.mLoader;
        }
        return null;
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static synchronized AdtPlugin getDefault() {
        return sPlugin;
    }

    public static Display getDisplay() {
        IWorkbench bench = null;
        synchronized (AdtPlugin.class) {
            bench = sPlugin.getWorkbench();
        }

        if (bench != null) {
            return bench.getDisplay();
        }
        return null;
    }

    /** Returns the adb path relative to the sdk folder */
    public static String getOsRelativeAdb() {
        return SdkConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_ADB;
    }

    /** Returns the emulator path relative to the sdk folder */
    public static String getOsRelativeEmulator() {
        return SdkConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_EMULATOR;
    }

    /** Returns the absolute adb path */
    public static String getOsAbsoluteAdb() {
        return getOsSdkFolder() + getOsRelativeAdb();
    }

    /** Returns the absolute traceview path */
    public static String getOsAbsoluteTraceview() {
        return getOsSdkFolder() + SdkConstants.OS_SDK_TOOLS_FOLDER +
                AndroidConstants.FN_TRACEVIEW;
    }

    /** Returns the absolute emulator path */
    public static String getOsAbsoluteEmulator() {
        return getOsSdkFolder() + getOsRelativeEmulator();
    }

    /**
     * Returns a Url file path to the javaDoc folder.
     */
    public static String getUrlDoc() {
        return ProjectHelper.getJavaDocPath(
                getOsSdkFolder() + AndroidConstants.WS_JAVADOC_FOLDER_LEAF);
    }

    /**
     * Returns the SDK folder.
     * Guaranteed to be terminated by a platform-specific path separator.
     */
    public static synchronized String getOsSdkFolder() {
        if (sPlugin == null) {
            return null;
        }

        if (sPlugin.mOsSdkLocation == null) {
            sPlugin.mOsSdkLocation = sPlugin.mStore.getString(PREFS_SDK_DIR);
        }
        return sPlugin.mOsSdkLocation;
    }

    public static String getOsSdkToolsFolder() {
        return getOsSdkFolder() + SdkConstants.OS_SDK_TOOLS_FOLDER;
    }

    public static synchronized boolean getAutoResRefresh() {
        if (sPlugin == null) {
            return false;
        }
        return sPlugin.mStore.getBoolean(PREFS_RES_AUTO_REFRESH);
    }

    public static synchronized int getBuildVerbosity() {
        if (sPlugin != null) {
            return sPlugin.mBuildVerbosity;
        }

        return 0;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Reads and returns the content of a text file embedded in the plugin jar
     * file.
     * @param filepath the file path to the text file
     * @return null if the file could not be read
     */
    public static String readEmbeddedTextFile(String filepath) {
        Bundle bundle = null;
        synchronized (AdtPlugin.class) {
            if (sPlugin != null) {
                bundle = sPlugin.getBundle();
            } else {
                return null;
            }
        }

        // attempt to get a file to one of the template.
        try {
            URL url = bundle.getEntry(AndroidConstants.WS_SEP + filepath);
            if (url != null) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream()));

                String line;
                StringBuilder total = new StringBuilder(reader.readLine());
                while ((line = reader.readLine()) != null) {
                    total.append('\n');
                    total.append(line);
                }

                return total.toString();
            }
        } catch (MalformedURLException e) {
            // we'll just return null.
        } catch (IOException e) {
            // we'll just return null.
        }

        return null;
    }

    /**
     * Reads and returns the content of a binary file embedded in the plugin jar
     * file.
     * @param filepath the file path to the text file
     * @return null if the file could not be read
     */
    public static byte[] readEmbeddedFile(String filepath) {
        Bundle bundle = null;
        synchronized (AdtPlugin.class) {
            if (sPlugin != null) {
                bundle = sPlugin.getBundle();
            } else {
                return null;
            }
        }

        // attempt to get a file to one of the template.
        try {
            URL url = bundle.getEntry(AndroidConstants.WS_SEP + filepath);
            if (url != null) {
                // create a buffered reader to facilitate reading.
                BufferedInputStream stream = new BufferedInputStream(
                        url.openStream());

                // get the size to read.
                int avail = stream.available();

                // create the buffer and reads it.
                byte[] buffer = new byte[avail];
                stream.read(buffer);

                // and return.
                return buffer;
            }
        } catch (MalformedURLException e) {
            // we'll just return null.
        } catch (IOException e) {
            // we'll just return null;.
        }

        return null;
    }

    /**
     * Displays an error dialog box. This dialog box is ran asynchronously in the ui thread,
     * therefore this method can be called from any thread.
     * @param title The title of the dialog box
     * @param message The error message
     */
    public final static void displayError(final String title, final String message) {
        // get the current Display
        final Display display = getDisplay();

        // dialog box only run in ui thread..
        display.asyncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                MessageDialog.openError(shell, title, message);
            }
        });
    }

    /**
     * Displays a warning dialog box. This dialog box is ran asynchronously in the ui thread,
     * therefore this method can be called from any thread.
     * @param title The title of the dialog box
     * @param message The warning message
     */
    public final static void displayWarning(final String title, final String message) {
        // get the current Display
        final Display display = getDisplay();

        // dialog box only run in ui thread..
        display.asyncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                MessageDialog.openWarning(shell, title, message);
            }
        });
    }

    /**
     * Display a yes/no question dialog box. This dialog is opened synchronously in the ui thread,
     * therefore this message can be called from any thread.
     * @param title The title of the dialog box
     * @param message The error message
     * @return true if OK was clicked.
     */
    public final static boolean displayPrompt(final String title, final String message) {
        // get the current Display and Shell
        final Display display = getDisplay();

        // we need to ask the user what he wants to do.
        final boolean[] result = new boolean[1];
        display.syncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                result[0] = MessageDialog.openQuestion(shell, title, message);
            }
        });
        return result[0];
    }

    /**
     * Logs a message to the default Eclipse log.
     *
     * @param severity The severity code. Valid values are: {@link IStatus#OK},
     * {@link IStatus#ERROR}, {@link IStatus#INFO}, {@link IStatus#WARNING} or
     * {@link IStatus#CANCEL}.
     * @param format The format string, like for {@link String#format(String, Object...)}.
     * @param args The arguments for the format string, like for
     * {@link String#format(String, Object...)}.
     */
    public static void log(int severity, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(severity, PLUGIN_ID, message);
        getDefault().getLog().log(status);
    }

    /**
     * Logs an exception to the default Eclipse log.
     * <p/>
     * The status severity is always set to ERROR.
     *
     * @param exception the exception to log.
     * @param format The format string, like for {@link String#format(String, Object...)}.
     * @param args The arguments for the format string, like for
     * {@link String#format(String, Object...)}.
     */
    public static void log(Throwable exception, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
        getDefault().getLog().log(status);
    }

    /**
     * This is a mix between log(Throwable) and printErrorToConsole.
     * <p/>
     * This logs the exception with an ERROR severity and the given printf-like format message.
     * The same message is then printed on the Android error console with the associated tag.
     *
     * @param exception the exception to log.
     * @param format The format string, like for {@link String#format(String, Object...)}.
     * @param args The arguments for the format string, like for
     * {@link String#format(String, Object...)}.
     */
    public static synchronized void logAndPrintError(Throwable exception, String tag,
            String format, Object ... args) {
        if (sPlugin != null) {
            String message = String.format(format, args);
            Status status = new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
            getDefault().getLog().log(status);
            printToStream(sPlugin.mAndroidConsoleErrorStream, tag, message);
            showAndroidConsole();
        }
    }

    /**
     * Prints one or more error message to the android console.
     * @param tag A tag to be associated with the message. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static synchronized void printErrorToConsole(String tag, Object... objects) {
        if (sPlugin != null) {
            printToStream(sPlugin.mAndroidConsoleErrorStream, tag, objects);

            showAndroidConsole();
        }
    }

    /**
     * Prints one or more error message to the android console.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static void printErrorToConsole(Object... objects) {
        printErrorToConsole((String)null, objects);
    }

    /**
     * Prints one or more error message to the android console.
     * @param project The project to which the message is associated. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static void printErrorToConsole(IProject project, Object... objects) {
        String tag = project != null ? project.getName() : null;
        printErrorToConsole(tag, objects);
    }

    /**
     * Prints one or more build messages to the android console, filtered by Build output verbosity.
     * @param level Verbosity level of the message.
     * @param project The project to which the message is associated. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     * @see AdtConstants#BUILD_ALWAYS
     * @see AdtConstants#BUILD_NORMAL
     * @see AdtConstants#BUILD_VERBOSE
     */
    public static synchronized void printBuildToConsole(int level, IProject project,
            Object... objects) {
        if (sPlugin != null) {
            if (level <= sPlugin.mBuildVerbosity) {
                String tag = project != null ? project.getName() : null;
                printToStream(sPlugin.mAndroidConsoleStream, tag, objects);
            }
        }
    }

    /**
     * Prints one or more message to the android console.
     * @param tag The tag to be associated with the message. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static synchronized void printToConsole(String tag, Object... objects) {
        if (sPlugin != null) {
            printToStream(sPlugin.mAndroidConsoleStream, tag, objects);
        }
    }

    /**
     * Prints one or more message to the android console.
     * @param project The project to which the message is associated. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static void printToConsole(IProject project, Object... objects) {
        String tag = project != null ? project.getName() : null;
        printToConsole(tag, objects);
    }

    /** Force the display of the android console */
    public static void showAndroidConsole() {
        // first make sure the console is in the workbench
        EclipseUiHelper.showView(IConsoleConstants.ID_CONSOLE_VIEW, true);

        // now make sure it's not docked.
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(
                AdtPlugin.getDefault().getAndroidConsole());
    }

    /**
     * Returns an standard PrintStream object for a specific project.<br>
     * This PrintStream will add a date/project at the beginning of every
     * <code>println()</code> output.
     *
     * @param project The project object
     * @param prefix The prefix to be added to the message. Can be null.
     * @return a new PrintStream
     */
    public static synchronized PrintStream getOutPrintStream(IProject project, String prefix) {
        if (sPlugin != null) {
            return new AndroidPrintStream(project, prefix, sPlugin.mAndroidConsoleStream);
        }

        return null;
    }

    /**
     * Returns an error PrintStream object for a specific project.<br>
     * This PrintStream will add a date/project at the beginning of every
     * <code>println()</code> output.
     *
     * @param project The project object
     * @param prefix The prefix to be added to the message. Can be null.
     * @return a new PrintStream
     */
    public static synchronized PrintStream getErrPrintStream(IProject project, String prefix) {
        if (sPlugin != null) {
            return new AndroidPrintStream(project, prefix, sPlugin.mAndroidConsoleErrorStream);
        }

        return null;
    }

    /**
     * Returns whether the Sdk has been loaded.
     */
    public final LoadStatus getSdkLoadStatus() {
        synchronized (getSdkLockObject()) {
            return mSdkIsLoaded;
        }
    }

    /**
     * Returns the lock object for SDK loading. If you wish to do things while the SDK is loading,
     * you must synchronize on this object.
     */
    public final Object getSdkLockObject() {
        return mPostLoadProjectsToResolve;
    }

    /**
     * Sets the given {@link IJavaProject} to have its target resolved again once the SDK finishes
     * to load.
     */
    public final void setProjectToResolve(IJavaProject javaProject) {
        synchronized (getSdkLockObject()) {
            mPostLoadProjectsToResolve.add(javaProject);
        }
    }

    /**
     * Sets the given {@link IJavaProject} to have its target checked for consistency
     * once the SDK finishes to load. This is used if the target is resolved using cached
     * information while the SDK is loading.
     */
    public final void setProjectToCheck(IJavaProject javaProject) {
        // only lock on
        synchronized (getSdkLockObject()) {
            mPostLoadProjectsToCheck.add(javaProject);
        }
    }

    /**
     * Checks the location of the SDK is valid and if it is, grab the SDK API version
     * from the SDK.
     * @return false if the location is not correct.
     */
    private boolean checkSdkLocationAndId() {
        if (mOsSdkLocation == null || mOsSdkLocation.length() == 0) {
            displayError(Messages.Dialog_Title_SDK_Location, Messages.SDK_Not_Setup);
            return false;
        }

        return checkSdkLocationAndId(mOsSdkLocation, new CheckSdkErrorHandler() {
            @Override
            public boolean handleError(String message) {
                AdtPlugin.displayError(Messages.Dialog_Title_SDK_Location,
                        String.format(Messages.Error_Check_Prefs, message));
                return false;
            }

            @Override
            public boolean handleWarning(String message) {
                AdtPlugin.displayWarning(Messages.Dialog_Title_SDK_Location, message);
                return true;
            }
        });
    }

    /**
     * Internal helper to perform the actual sdk location and id check.
     *
     * @param osSdkLocation The sdk directory, an OS path.
     * @param errorHandler An checkSdkErrorHandler that can display a warning or an error.
     * @return False if there was an error or the result from the errorHandler invocation.
     */
    public boolean checkSdkLocationAndId(String osSdkLocation, CheckSdkErrorHandler errorHandler) {
        if (osSdkLocation.endsWith(File.separator) == false) {
            osSdkLocation = osSdkLocation + File.separator;
        }

        File osSdkFolder = new File(osSdkLocation);
        if (osSdkFolder.isDirectory() == false) {
            return errorHandler.handleError(
                    String.format(Messages.Could_Not_Find_Folder, osSdkLocation));
        }

        String osTools = osSdkLocation + SdkConstants.OS_SDK_TOOLS_FOLDER;
        File toolsFolder = new File(osTools);
        if (toolsFolder.isDirectory() == false) {
            return errorHandler.handleError(
                    String.format(Messages.Could_Not_Find_Folder_In_SDK,
                            SdkConstants.FD_TOOLS, osSdkLocation));
        }

        // check the path to various tools we use
        String[] filesToCheck = new String[] {
                osSdkLocation + getOsRelativeAdb(),
                osSdkLocation + getOsRelativeEmulator()
        };
        for (String file : filesToCheck) {
            if (checkFile(file) == false) {
                return errorHandler.handleError(String.format(Messages.Could_Not_Find, file));
            }
        }

        // check the SDK build id/version and the plugin version.
        return VersionCheck.checkVersion(osSdkLocation, errorHandler);
    }

    /**
     * Checks if a path reference a valid existing file.
     * @param osPath the os path to check.
     * @return true if the file exists and is, in fact, a file.
     */
    private boolean checkFile(String osPath) {
        File file = new File(osPath);
        if (file.isFile() == false) {
            return false;
        }

        return true;
    }

    /**
     * Creates a job than can ping the usage server.
     */
    private Job createPingUsageServerJob() {
        // In order to not block the plugin loading, so we spawn another thread.
        Job job = new Job("Android SDK Ping") {  // Job name, visible in progress view
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    pingUsageServer(); //$NON-NLS-1$

                    return Status.OK_STATUS;
                } catch (Throwable t) {
                    log(t, "pingUsageServer failed");       //$NON-NLS-1$
                    return new Status(IStatus.ERROR, PLUGIN_ID,
                            "pingUsageServer failed", t);    //$NON-NLS-1$
                }
            }
        };
        return job;
    }

    /**
     * Parses the SDK resources.
     */
    private void parseSdkContent() {
        // Perform the update in a thread (here an Eclipse runtime job)
        // since this should never block the caller (especially the start method)
        Job job = new Job(Messages.AdtPlugin_Android_SDK_Content_Loader) {
            @SuppressWarnings("unchecked")
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {

                    if (mSdkIsLoading) {
                        return new Status(IStatus.WARNING, PLUGIN_ID,
                                "An Android SDK is already being loaded. Please try again later.");
                    }

                    mSdkIsLoading = true;

                    SubMonitor progress = SubMonitor.convert(monitor,
                            "Initialize SDK Manager", 100);

                    Sdk sdk = Sdk.loadSdk(mOsSdkLocation);

                    if (sdk != null) {

                        progress.setTaskName(Messages.AdtPlugin_Parsing_Resources);

                        int n = sdk.getTargets().length;
                        if (n > 0) {
                            int w = 60 / n;
                            for (IAndroidTarget target : sdk.getTargets()) {
                                SubMonitor p2 = progress.newChild(w);
                                IStatus status = new AndroidTargetParser(target).run(p2);
                                if (status.getCode() != IStatus.OK) {
                                    synchronized (getSdkLockObject()) {
                                        mSdkIsLoaded = LoadStatus.FAILED;
                                        mPostLoadProjectsToResolve.clear();
                                    }
                                    return status;
                                }
                            }
                        }

                        synchronized (getSdkLockObject()) {
                            mSdkIsLoaded = LoadStatus.LOADED;

                            progress.setTaskName("Check Projects");

                            ArrayList<IJavaProject> list = new ArrayList<IJavaProject>();
                            for (IJavaProject javaProject : mPostLoadProjectsToResolve) {
                                if (javaProject.getProject().isOpen()) {
                                    list.add(javaProject);
                                }
                            }

                            // done with this list.
                            mPostLoadProjectsToResolve.clear();

                            // check the projects that need checking.
                            // The method modifies the list (it removes the project that
                            // do not need to be resolved again).
                            AndroidClasspathContainerInitializer.checkProjectsCache(
                                    mPostLoadProjectsToCheck);

                            list.addAll(mPostLoadProjectsToCheck);

                            // update the project that needs recompiling.
                            if (list.size() > 0) {
                                IJavaProject[] array = list.toArray(
                                        new IJavaProject[list.size()]);
                                AndroidClasspathContainerInitializer.updateProjects(array);
                            }

                            progress.worked(10);
                        }
                    }

                    // Notify resource changed listeners
                    progress.setTaskName("Refresh UI");
                    progress.setWorkRemaining(mTargetChangeListeners.size());

                    // Clone the list before iterating, to avoid Concurrent Modification
                    // exceptions
                    final List<ITargetChangeListener> listeners =
                            (List<ITargetChangeListener>)mTargetChangeListeners.clone();
                    final SubMonitor progress2 = progress;
                    AdtPlugin.getDisplay().syncExec(new Runnable() {
                        public void run() {
                            for (ITargetChangeListener listener : listeners) {
                                try {
                                    listener.onTargetsLoaded();
                                } catch (Exception e) {
                                    AdtPlugin.log(e, "Failed to update a TargetChangeListener.");  //$NON-NLS-1$
                                } finally {
                                    progress2.worked(1);
                                }
                            }
                        }
                    });
                } catch (Throwable t) {
                    log(t, "Unknown exception in parseSdkContent.");    //$NON-NLS-1$
                    return new Status(IStatus.ERROR, PLUGIN_ID,
                            "parseSdkContent failed", t);               //$NON-NLS-1$

                } finally {
                    mSdkIsLoading = false;
                    if (monitor != null) {
                        monitor.done();
                    }
                }

                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.BUILD); // build jobs are run after other interactive jobs
        job.schedule();
    }

    /** Returns the global android console */
    public MessageConsole getAndroidConsole() {
        return mAndroidConsole;
    }

    // ----- Methods for Editors -------

    public void startEditors() {
        sAndroidLogoDesc = imageDescriptorFromPlugin(AdtPlugin.PLUGIN_ID,
                "/icons/android.png"); //$NON-NLS-1$
        sAndroidLogo = sAndroidLogoDesc.createImage();

        // Add a resource listener to handle compiled resources.
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        mResourceMonitor = ResourceMonitor.startMonitoring(ws);

        if (mResourceMonitor != null) {
            try {
                setupDefaultEditor(mResourceMonitor);
                ResourceManager.setup(mResourceMonitor);
            } catch (Throwable t) {
                log(t, "ResourceManager.setup failed"); //$NON-NLS-1$
            }
        }
    }

    /**
     * The <code>AbstractUIPlugin</code> implementation of this <code>Plugin</code>
     * method saves this plug-in's preference and dialog stores and shuts down
     * its image registry (if they are in use). Subclasses may extend this
     * method, but must send super <b>last</b>. A try-finally statement should
     * be used where necessary to ensure that <code>super.shutdown()</code> is
     * always done.
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stopEditors() {
        sAndroidLogo.dispose();

        IconFactory.getInstance().Dispose();

        // Remove the resource listener that handles compiled resources.
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        ResourceMonitor.stopMonitoring(ws);

        mRed.dispose();
    }

    /**
     * Returns an Image for the small Android logo.
     *
     * Callers should not dispose it.
     */
    public static Image getAndroidLogo() {
        return sAndroidLogo;
    }

    /**
     * Returns an {@link ImageDescriptor} for the small Android logo.
     *
     * Callers should not dispose it.
     */
    public static ImageDescriptor getAndroidLogoDesc() {
        return sAndroidLogoDesc;
    }

    /**
     * Returns the ResourceMonitor object.
     */
    public ResourceMonitor getResourceMonitor() {
        return mResourceMonitor;
    }

    /**
     * Sets up the editor to register default editors for resource files when needed.
     *
     * This is called by the {@link AdtPlugin} during initialization.
     *
     * @param monitor The main Resource Monitor object.
     */
    public void setupDefaultEditor(ResourceMonitor monitor) {
        monitor.addFileListener(new IFileListener() {

            private static final String UNKNOWN_EDITOR = "unknown-editor"; //$NON-NLS-1$

            /* (non-Javadoc)
             * Sent when a file changed.
             * @param file The file that changed.
             * @param markerDeltas The marker deltas for the file.
             * @param kind The change kind. This is equivalent to
             * {@link IResourceDelta#accept(IResourceDeltaVisitor)}
             *
             * @see IFileListener#fileChanged
             */
            public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind) {
                if (AndroidConstants.EXT_XML.equals(file.getFileExtension())) {
                    // The resources files must have a file path similar to
                    //    project/res/.../*.xml
                    // There is no support for sub folders, so the segment count must be 4
                    if (file.getFullPath().segmentCount() == 4) {
                        // check if we are inside the res folder.
                        String segment = file.getFullPath().segment(1);
                        if (segment.equalsIgnoreCase(SdkConstants.FD_RESOURCES)) {
                            // we are inside a res/ folder, get the actual ResourceFolder
                            ProjectResources resources = ResourceManager.getInstance().
                                getProjectResources(file.getProject());

                            // This happens when importing old Android projects in Eclipse
                            // that lack the container (probably because resources fail to build
                            // properly.)
                            if (resources == null) {
                                log(IStatus.INFO,
                                        "getProjectResources failed for path %1$s in project %2$s", //$NON-NLS-1$
                                        file.getFullPath().toOSString(),
                                        file.getProject().getName());
                                return;
                            }

                            ResourceFolder resFolder = resources.getResourceFolder(
                                (IFolder)file.getParent());

                            if (resFolder != null) {
                                if (kind == IResourceDelta.ADDED) {
                                    resourceAdded(file, resFolder.getType());
                                } else if (kind == IResourceDelta.CHANGED) {
                                    resourceChanged(file, resFolder.getType());
                                }
                            } else {
                                // if the res folder is null, this means the name is invalid,
                                // in this case we remove whatever android editors that was set
                                // as the default editor.
                                IEditorDescriptor desc = IDE.getDefaultEditor(file);
                                String editorId = desc.getId();
                                if (editorId.startsWith(AndroidConstants.EDITORS_NAMESPACE)) {
                                    // reset the default editor.
                                    IDE.setDefaultEditor(file, null);
                                }
                            }
                        }
                    }
                }
            }

            private void resourceAdded(IFile file, ResourceFolderType type) {
                // set the default editor based on the type.
                if (type == ResourceFolderType.LAYOUT) {
                    IDE.setDefaultEditor(file, LayoutEditor.ID);
                } else if (type == ResourceFolderType.DRAWABLE
                        || type == ResourceFolderType.VALUES) {
                    IDE.setDefaultEditor(file, ResourcesEditor.ID);
                } else if (type == ResourceFolderType.MENU) {
                    IDE.setDefaultEditor(file, MenuEditor.ID);
                } else if (type == ResourceFolderType.XML) {
                    if (XmlEditor.canHandleFile(file)) {
                        IDE.setDefaultEditor(file, XmlEditor.ID);
                    } else {
                        // set a property to determine later if the XML can be handled
                        QualifiedName qname = new QualifiedName(
                                AdtPlugin.PLUGIN_ID,
                                UNKNOWN_EDITOR);
                        try {
                            file.setPersistentProperty(qname, "1"); //$NON-NLS-1$
                        } catch (CoreException e) {
                            // pass
                        }
                    }
                }
            }

            private void resourceChanged(IFile file, ResourceFolderType type) {
                if (type == ResourceFolderType.XML) {
                    IEditorDescriptor ed = IDE.getDefaultEditor(file);
                    if (ed == null || ed.getId() != XmlEditor.ID) {
                        QualifiedName qname = new QualifiedName(
                                AdtPlugin.PLUGIN_ID,
                                UNKNOWN_EDITOR);
                        String prop = null;
                        try {
                            prop = file.getPersistentProperty(qname);
                        } catch (CoreException e) {
                            // pass
                        }
                        if (prop != null && XmlEditor.canHandleFile(file)) {
                            try {
                                // remove the property & set editor
                                file.setPersistentProperty(qname, null);
                                IWorkbenchPage page = PlatformUI.getWorkbench().
                                                        getActiveWorkbenchWindow().getActivePage();

                                IEditorPart oldEditor = page.findEditor(new FileEditorInput(file));
                                if (oldEditor != null &&
                                        AdtPlugin.displayPrompt("Android XML Editor",
                                            String.format("The file you just saved as been recognized as a file that could be better handled using the Android XML Editor. Do you want to edit '%1$s' using the Android XML editor instead?",
                                                    file.getFullPath()))) {
                                    IDE.setDefaultEditor(file, XmlEditor.ID);
                                    IEditorPart newEditor = page.openEditor(
                                            new FileEditorInput(file),
                                            XmlEditor.ID,
                                            true, /* activate */
                                            IWorkbenchPage.MATCH_NONE);

                                    if (newEditor != null) {
                                        page.closeEditor(oldEditor, true /* save */);
                                    }
                                }
                            } catch (CoreException e) {
                                // setPersistentProperty or page.openEditor may have failed
                            }
                        }
                    }
                }
            }

        }, IResourceDelta.ADDED | IResourceDelta.CHANGED);
    }

    /**
     * Adds a new {@link ITargetChangeListener} to be notified when a new SDK is loaded, or when
     * a project has its target changed.
     */
    public void addTargetListener(ITargetChangeListener listener) {
        mTargetChangeListeners.add(listener);
    }

    /**
     * Removes an existing {@link ITargetChangeListener}.
     * @see #addTargetListener(ITargetChangeListener)
     */
    public void removeTargetListener(ITargetChangeListener listener) {
        mTargetChangeListeners.remove(listener);
    }

    /**
     * Updates all the {@link ITargetChangeListener} that a target has changed for a given project.
     * <p/>Only editors related to that project should reload.
     */
    @SuppressWarnings("unchecked")
    public void updateTargetListener(final IProject project) {
        final List<ITargetChangeListener> listeners =
            (List<ITargetChangeListener>)mTargetChangeListeners.clone();

        AdtPlugin.getDisplay().asyncExec(new Runnable() {
            public void run() {
                for (ITargetChangeListener listener : listeners) {
                    try {
                        listener.onProjectTargetChange(project);
                    } catch (Exception e) {
                        AdtPlugin.log(e, "Failed to update a TargetChangeListener.");  //$NON-NLS-1$
                    }
                }
            }
        });
    }

    public static synchronized OutputStream getErrorStream() {
        return sPlugin.mAndroidConsoleErrorStream;
    }

    /**
     * Pings the usage start server.
     */
    private void pingUsageServer() {
        // get the version of the plugin
        String versionString = (String) getBundle().getHeaders().get(
                Constants.BUNDLE_VERSION);
        Version version = new Version(versionString);

        versionString = String.format("%1$d.%2$d.%3$d", version.getMajor(), //$NON-NLS-1$
                version.getMinor(), version.getMicro());

        SdkStatsService.ping("adt", versionString, getDisplay()); //$NON-NLS-1$
    }

    /**
     * Reparses the content of the SDK and updates opened projects.
     */
    public void reparseSdk() {
        // add all the opened Android projects to the list of projects to be updated
        // after the SDK is reloaded
        synchronized (getSdkLockObject()) {
            // get the project to refresh.
            IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects();
            mPostLoadProjectsToResolve.addAll(Arrays.asList(androidProjects));
        }

        // parse the SDK resources at the new location
        parseSdkContent();
    }

    /**
     * Prints messages, associated with a project to the specified stream
     * @param stream The stream to write to
     * @param tag The tag associated to the message. Can be null
     * @param objects The objects to print through their toString() method (or directly for
     * {@link String} objects.
     */
    public static synchronized void printToStream(MessageConsoleStream stream, String tag,
            Object... objects) {
        String dateTag = getMessageTag(tag);

        for (Object obj : objects) {
            stream.print(dateTag);
            if (obj instanceof String) {
                stream.println((String)obj);
            } else {
                stream.println(obj.toString());
            }
        }
    }

    /**
     * Creates a string containing the current date/time, and the tag
     * @param tag The tag associated to the message. Can be null
     * @return The dateTag
     */
    public static String getMessageTag(String tag) {
        Calendar c = Calendar.getInstance();

        if (tag == null) {
            return String.format(Messages.Console_Date_Tag, c);
        }

        return String.format(Messages.Console_Data_Project_Tag, c, tag);
    }

}
