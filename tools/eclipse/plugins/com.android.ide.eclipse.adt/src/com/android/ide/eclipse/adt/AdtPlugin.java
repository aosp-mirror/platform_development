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
import com.android.ide.eclipse.adt.build.DexWrapper;
import com.android.ide.eclipse.adt.debug.launching.AndroidLaunchController;
import com.android.ide.eclipse.adt.debug.ui.SkinRepository;
import com.android.ide.eclipse.adt.preferences.BuildPreferencePage;
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.adt.project.export.ExportWizard;
import com.android.ide.eclipse.adt.project.internal.AndroidClasspathContainerInitializer;
import com.android.ide.eclipse.adt.resources.FrameworkResourceParser;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.CommonPlugin;
import com.android.ide.eclipse.common.EclipseUiHelper;
import com.android.ide.eclipse.common.SdkStatsHelper;
import com.android.ide.eclipse.common.StreamHelper;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.common.project.ExportHelper;
import com.android.ide.eclipse.common.project.ExportHelper.IExportCallback;
import com.android.ide.eclipse.common.resources.FrameworkResourceManager;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.ImageLoader;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
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

    /** default store, provided by eclipse */
    private IPreferenceStore mStore;

    /** cached location for the sdk folder */
    private String mOsSdkLocation;

    /** SDK Api Version */
    String mSdkApiVersion;

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
    
    private final ArrayList<IJavaProject> mPostDexProjects = new ArrayList<IJavaProject>();
    
    /** Boolean wrapper to run dialog in the UI thread, and still get the
     * return code.
     */
    private static final class BooleanWrapper {
        public boolean b;
    }

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
            String tag = StreamHelper.getMessageTag(mProject != null ? mProject.getName() : null);

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

        // get the stream to write in the android console.
        MessageConsole androidConsole = CommonPlugin.getDefault().getAndroidConsole();
        mAndroidConsoleStream = androidConsole.newMessageStream();
        mAndroidConsoleErrorStream = androidConsole.newMessageStream();
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
                        // if sdk if valid, reparse the skin folder
                        SkinRepository.getInstance().parseFolder(getOsSkinFolder());
                    }

                    // parse the SDK resources at the new location
                    parseSdkContent();
                    
                    // get the project to refresh.
                    IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects();
                    
                    // Setup the new container for each project. By providing new instances of
                    // AndroidClasspathContainer, this will force JDT to call
                    // IClasspathContainer#getClasspathEntries() again and receive the new
                    // path to the framework jar.
                    AndroidClasspathContainerInitializer.updateProjects(androidProjects);
                    
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
        if (checkSdkLocationAndId())  {
            // if sdk if valid, parse the skin folder
            SkinRepository.getInstance().parseFolder(getOsSkinFolder());

            // parse the SDK resources.
            parseSdkContent();
        }
        
        mBuildVerbosity = BuildPreferencePage.getBuildLevel(
                mStore.getString(PREFS_BUILD_VERBOSITY));

        // Ping the usage start server.
        pingUsageServer();

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

        /* The Editors plugin must be started as soon as Android projects are opened or created,
         * in order to properly set default editors on the layout/values XML files.
         * 
         * This ensures that the default editors is really only set when a new XML file
         * is added to the workspace (IResourceDelta.ADDED event), through project creation or
         * manual add.
         * Other methods would force to go through existing projects when the Editors plugin is
         * started, and set the default editors for their XML files, possibly erasing user set
         * default editors.
         */
        startEditorsPlugin();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        
        DexWrapper.unloadDex();

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
        return AndroidConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_ADB;
    }

    /** Returns the aapt path relative to the sdk folder */
    public static String getOsRelativeAapt() {
        return AndroidConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_AAPT;
    }

    /** Returns the emulator path relative to the sdk folder */
    public static String getOsRelativeEmulator() {
        return AndroidConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_EMULATOR;
    }

    /** Returns the aidl path relative to the sdk folder */
    public static String getOsRelativeAidl() {
        return AndroidConstants.OS_SDK_TOOLS_FOLDER + AndroidConstants.FN_AIDL;
    }

    /** Returns the framework jar path relative to the sdk folder */
    public static String getOsRelativeFramework() {
        return AndroidConstants.FN_FRAMEWORK_LIBRARY;
    }

    /** Returns the android sources path relative to the sdk folder */
    public static String getOsRelativeAndroidSources() {
        return AndroidConstants.FD_ANDROID_SOURCES;
    }

    /** Returns the framework jar path relative to the sdk folder */
    public static String getOsRelativeAttrsXml() {
        return AndroidConstants.OS_SDK_LIBS_FOLDER + AndroidConstants.FN_ATTRS_XML;
    }

    /** Returns the absolute adb path */
    public static String getOsAbsoluteAdb() {
        return getOsSdkFolder() + getOsRelativeAdb();
    }

    /** Returns the absolute traceview path */
    public static String getOsAbsoluteTraceview() {
        return getOsSdkFolder() + AndroidConstants.OS_SDK_TOOLS_FOLDER +
                AndroidConstants.FN_TRACEVIEW;
    }

    /** Returns the absolute aapt path */
    public static String getOsAbsoluteAapt() {
        return getOsSdkFolder() + getOsRelativeAapt();
    }

    /** Returns the absolute sdk framework path */
    public static String getOsAbsoluteFramework() {
        return getOsSdkFolder() + getOsRelativeFramework();
    }

    /** Returns the absolute android sources path in the sdk */
    public static String getOsAbsoluteAndroidSources() {
        return getOsSdkFolder() + getOsRelativeAndroidSources();
    }

    /** Returns the absolute attrs.xml path */
    public static String getOsAbsoluteAttrsXml() {
        return getOsSdkFolder() + getOsRelativeAttrsXml();
    }

    /** Returns the absolute emulator path */
    public static String getOsAbsoluteEmulator() {
        return getOsSdkFolder() + getOsRelativeEmulator();
    }

    /** Returns the absolute aidl path */
    public static String getOsAbsoluteAidl() {
        return getOsSdkFolder() + getOsRelativeAidl();
    }

    /** Returns the absolute path to the aidl framework import file. */
    public static String getOsAbsoluteFrameworkAidl() {
        return getOsSdkFolder() + AndroidConstants.OS_SDK_LIBS_FOLDER +
                AndroidConstants.FN_FRAMEWORK_AIDL;
    }

    public static String getOsSdkSamplesFolder() {
        return getOsSdkFolder() + AndroidConstants.OS_SDK_SAMPLES_FOLDER;
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
        return getOsSdkFolder() + AndroidConstants.OS_SDK_TOOLS_FOLDER;
    }

    public static String getOsSkinFolder() {
        return getOsSdkFolder() + AndroidConstants.OS_SDK_SKINS_FOLDER;
    }

    public static synchronized boolean getAutoResRefresh() {
        if (sPlugin == null) {
            return false;
        }
        return sPlugin.mStore.getBoolean(PREFS_RES_AUTO_REFRESH);
    }

    /**
     * Returns the SDK build id.
     * @return a string containing the SDK build id, or null it it is unknownn.
     */
    public static synchronized String getSdkApiVersion() {
        if (sPlugin != null) {
            return sPlugin.mSdkApiVersion;
        }
        
        return null;
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
        final BooleanWrapper wrapper = new BooleanWrapper();
        display.syncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                wrapper.b = MessageDialog.openQuestion(shell, title, message);
            }
        });
        return wrapper.b;
    }
    
    /**
     * Logs a message to the default Eclipse log.
     * 
     * @param severity The severity code. Valid values are: {@link IStatus#OK}, {@link IStatus#ERROR},
     * {@link IStatus#INFO}, {@link IStatus#WARNING} or {@link IStatus#CANCEL}.
     * @param format The format string, like for {@link String#format(String, Object...)}.
     * @param args The arguments for the format string, like for {@link String#format(String, Object...)}.
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
            StreamHelper.printToStream(sPlugin.mAndroidConsoleErrorStream, tag, message);
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
            StreamHelper.printToStream(sPlugin.mAndroidConsoleErrorStream, tag, objects);
    
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
                StreamHelper.printToStream(sPlugin.mAndroidConsoleStream, tag, objects);
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
            StreamHelper.printToStream(sPlugin.mAndroidConsoleStream, tag, objects);
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
                CommonPlugin.getDefault().getAndroidConsole());
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
     * Adds a {@link IJavaProject} to a list of projects to be recompiled once dx.jar is loaded.
     * @param javaProject
     */
    public void addPostDexProject(IJavaProject javaProject) {
        synchronized (mPostDexProjects) {
            if (DexWrapper.getStatus() == DexWrapper.LoadStatus.LOADED) {
                // Setup the new container for each project. By providing new instances of
                // AndroidClasspathContainer, this will force JDT to call
                // IClasspathContainer#getClasspathEntries() again and receive the new
                // path to the framework jar, and the project will be recompiled.
                AndroidClasspathContainerInitializer.updateProjects(new IJavaProject [] {
                        javaProject });
            } else {
                mPostDexProjects.add(javaProject);
            }
        }
    }

    /**
     * Checks the location of the SDK is valid and if it is, grab the SDK API version
     * from the SDK.
     * @return false if the location is not correct.
     */
    private boolean checkSdkLocationAndId() {
        // Reset the sdk build first in case the SDK is invalid and we abort.
        mSdkApiVersion = null;

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

        String osTools = osSdkLocation + AndroidConstants.OS_SDK_TOOLS_FOLDER;
        File toolsFolder = new File(osTools);
        if (toolsFolder.isDirectory() == false) {
            return errorHandler.handleError(
                    String.format(Messages.Could_Not_Find_Folder_In_SDK,
                            AndroidConstants.FD_TOOLS, osSdkLocation));
        }

        // check the path to various tools we use
        String[] filesToCheck = new String[] {
                osSdkLocation + getOsRelativeFramework(),
                osSdkLocation + getOsRelativeAdb(),
                osSdkLocation + getOsRelativeAapt(),
                osSdkLocation + getOsRelativeAidl(),
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
     * Pings the usage start server.
     */
    private void pingUsageServer() {
        // In order to not block the plugin loading, so we spawn another thread.
        new Thread("Ping!") { //$NON-NLS-1$
            @Override
            public void run() {
                // get the version of the plugin
                String versionString = (String) getBundle().getHeaders().get(
                        Constants.BUNDLE_VERSION);
                Version version = new Version(versionString);
                
                SdkStatsHelper.pingUsageServer("adt", version); //$NON-NLS-1$
            }
        }.start();
    }
    
    /**
     * Starts the Editors plugin.
     * <p/> 
     * Since we do not want any dependencies between the plugins (Editors is an optional
     * plugin not needed for Android development), we attempt to start the plugin through
     * OSGi directly.
     * <p/> 
     * This is done in another thread to not delay the start of this plugin.
     */
    private void startEditorsPlugin() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // look for the bundle of the Editors plugin
                    Bundle editorsBundle = Platform.getBundle(AndroidConstants.EDITORS_PLUGIN_ID);
                    if (editorsBundle != null) {
                        // we only start if the bundle is installed and not started.
                        // STARTING means that its start is pending a triggering.
                        int bundleState = editorsBundle.getState();
                        if ((bundleState & (Bundle.RESOLVED | Bundle.INSTALLED
                                | Bundle.STARTING)) != 0) {
                            // Attempt to start it.
                            // START_TRANSIENT is used because we don't want
                            // to change the auto start value.
                            editorsBundle.start(Bundle.START_TRANSIENT);
                        }
                    }
                } catch (Exception e) {
                    log(e, Messages.AdtPlugin_Failed_To_Start_s, AndroidConstants.EDITORS_PLUGIN_ID);
                }
            }
        }.start();
    }
    
    /**
     * Parses the SDK resources and set them in the {@link FrameworkResourceManager}.
     */
    private void parseSdkContent() {
        // Perform the update in a thread (here an Eclipse runtime job)
        // since this should never block the caller (especially the start method)
        new Job(Messages.AdtPlugin_Android_SDK_Content_Loader) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    SubMonitor progress = null;
                    try {
                        progress = SubMonitor.convert(monitor, Messages.AdtPlugin_Parsing_Resources, 100);
                        
                        // load the values.
                        FrameworkResourceParser parser = new FrameworkResourceParser();
                        parser.parse(mOsSdkLocation, FrameworkResourceManager.getInstance(),
                                progress);
    
                        // set the location of the layout lib jar file.
                        FrameworkResourceManager.getInstance().setLayoutLibLocation(
                                mOsSdkLocation + AndroidConstants.OS_SDK_LIBS_LAYOUTLIB_JAR);
                        FrameworkResourceManager.getInstance().setFrameworkResourcesLocation(
                                mOsSdkLocation + AndroidConstants.OS_SDK_RESOURCES_FOLDER);
                        FrameworkResourceManager.getInstance().setFrameworkFontLocation(
                                mOsSdkLocation + AndroidConstants.OS_SDK_FONTS_FOLDER);
                    } catch (Throwable e) {
                        AdtPlugin.log(e, "Android SDK Resource Parser failed"); //$NON-NLS-1$
                        AdtPlugin.printErrorToConsole(Messages.AdtPlugin_Android_SDK_Resource_Parser,
                                Messages.AdtPlugin_Failed_To_Parse_s + e.getMessage());
                        
                        return new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
                    } finally {
                        if (progress != null) {
                            progress.worked(100);
                        }
                    }
                    
                    try {
                        progress = SubMonitor.convert(monitor, Messages.AdtPlugin_Parsing_Resources, 20);
                        DexWrapper.unloadDex();

                        IStatus res = DexWrapper.loadDex(
                                mOsSdkLocation + AndroidConstants.OS_SDK_LIBS_DX_JAR);
                        if (res != Status.OK_STATUS) {
                            return res;
                        } else {
                            // update the project that needs recompiling.
                            synchronized (mPostDexProjects) {
                                if (mPostDexProjects.size() > 0) {
                                    IJavaProject[] array = mPostDexProjects.toArray(
                                            new IJavaProject[mPostDexProjects.size()]);
                                    AndroidClasspathContainerInitializer.updateProjects(array);
                                    mPostDexProjects.clear();
                                }
                            }
                        }
                    } finally {
                        if (progress != null) {
                            progress.worked(20);
                        }
                    }
                } finally {
                    if (monitor != null) {
                        monitor.done();
                    }
                }

                return Status.OK_STATUS;
            }
        }.schedule();
    }
}
