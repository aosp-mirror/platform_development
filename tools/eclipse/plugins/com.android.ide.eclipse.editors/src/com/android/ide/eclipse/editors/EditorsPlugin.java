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

package com.android.ide.eclipse.editors;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.CommonPlugin;
import com.android.ide.eclipse.common.SdkStatsHelper;
import com.android.ide.eclipse.common.StreamHelper;
import com.android.ide.eclipse.common.resources.FrameworkResourceManager;
import com.android.ide.eclipse.editors.EditorsPlugin.LayoutBridge.LoadStatus;
import com.android.ide.eclipse.editors.layout.LayoutEditor;
import com.android.ide.eclipse.editors.layout.descriptors.LayoutDescriptors;
import com.android.ide.eclipse.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.editors.menu.MenuEditor;
import com.android.ide.eclipse.editors.menu.descriptors.MenuDescriptors;
import com.android.ide.eclipse.editors.resources.ResourcesEditor;
import com.android.ide.eclipse.editors.resources.manager.ProjectResources;
import com.android.ide.eclipse.editors.resources.manager.ResourceFolder;
import com.android.ide.eclipse.editors.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.editors.resources.manager.ResourceManager;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.editors.xml.XmlEditor;
import com.android.ide.eclipse.editors.xml.descriptors.XmlDescriptors;
import com.android.layoutlib.api.ILayoutBridge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * The activator class controls the plug-in life cycle
 */
public class EditorsPlugin extends AbstractUIPlugin {
    // The shared instance
    private static EditorsPlugin sPlugin;
    
    private static Image sAndroidLogo;
    private static ImageDescriptor sAndroidLogoDesc;
    
    private ResourceMonitor mResourceMonitor;
    private SdkPathChangedListener mSdkPathChangedListener;
    private ArrayList<Runnable> mResourceRefreshListener = new ArrayList<Runnable>();

    private MessageConsoleStream mAndroidConsoleStream;
    /** Stream to write error messages to the android console */
    private MessageConsoleStream mAndroidConsoleErrorStream;
    
    public final static class LayoutBridge {
        public enum LoadStatus { LOADING, LOADED, FAILED }

        /** Link to the layout bridge */
        public ILayoutBridge bridge;

        public LoadStatus status = LoadStatus.LOADING;
    }

    private final LayoutBridge mLayoutBridge = new LayoutBridge();

    private boolean mLayoutBridgeInit;

    private ClassLoader mBridgeClassLoader;

    private Color mRed;


    /**
     * The constructor
     */
    public EditorsPlugin() {
    }

    /**
     * The <code>AbstractUIPlugin</code> implementation of this <code>Plugin</code>
     * method refreshes the plug-in actions.  Subclasses may extend this method,
     * but must send super <b>first</b>.
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        sPlugin = this;
        sAndroidLogoDesc = imageDescriptorFromPlugin(AndroidConstants.EDITORS_PLUGIN_ID,
                "/icons/android.png"); //$NON-NLS-1$
        sAndroidLogo = sAndroidLogoDesc.createImage();
        
        // get the stream to write in the android console.
        MessageConsole androidConsole = CommonPlugin.getDefault().getAndroidConsole();
        mAndroidConsoleStream = androidConsole.newMessageStream();

        mAndroidConsoleErrorStream = androidConsole.newMessageStream();
        mRed = new Color(getDisplay(), 0xFF, 0x00, 0x00);

        // because this can be run, in some cases, by a non ui thread, and beccause
        // changing the console properties update the ui, we need to make this change
        // in the ui thread.
        getDisplay().asyncExec(new Runnable() {
            public void run() {
                mAndroidConsoleErrorStream.setColor(mRed);
            }
        });

        // Add a resource listener to handle compiled resources.
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        mResourceMonitor = ResourceMonitor.startMonitoring(ws);

        if (mResourceMonitor != null) {
            setupDefaultEditor(mResourceMonitor);
            ResourceManager.setup(mResourceMonitor);
        }
        
        // Setup the sdk location changed listener and invoke it the first time
        mSdkPathChangedListener = new SdkPathChangedListener();
        FrameworkResourceManager.getInstance().addFrameworkResourcesChangeListener(
                mSdkPathChangedListener);
        
        // ping the usage server
        pingUsageServer();
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
    @Override
    public void stop(BundleContext context) throws Exception {
        sPlugin = null;
        sAndroidLogo.dispose();
        
        IconFactory.getInstance().Dispose();
        
        // Remove the resource listener that handles compiled resources.
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        ResourceMonitor.stopMonitoring(ws);

        FrameworkResourceManager.getInstance().removeFrameworkResourcesChangeListener(
                mSdkPathChangedListener);
        mSdkPathChangedListener = null;
        
        mRed.dispose();

        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static EditorsPlugin getDefault() {
        return sPlugin;
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
     * Logs a message to the default Eclipse log.
     * 
     * @param severity One of IStatus' severity codes: OK, ERROR, INFO, WARNING or CANCEL.
     * @param format The format string, like for String.format().
     * @param args The arguments for the format string, like for String.format().
     */
    public static void log(int severity, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(severity, AndroidConstants.EDITORS_PLUGIN_ID, message);
        getDefault().getLog().log(status);
    }

    /**
     * Logs an exception to the default Eclipse log.
     * <p/>
     * The status severity is always set to ERROR.
     * 
     * @param exception The exception to log. Its call trace will be recorded.
     * @param format The format string, like for String.format().
     * @param args The arguments for the format string, like for String.format().
     */
    public static void log(Throwable exception, String format, Object ... args) {
        String message = String.format(format, args);
        Status status = new Status(IStatus.ERROR, AndroidConstants.EDITORS_PLUGIN_ID,
                message, exception);
        getDefault().getLog().log(status);
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
     * This is called by the {@link EditorsPlugin} during initialization.
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
                        if (segment.equalsIgnoreCase(AndroidConstants.FD_RESOURCES)) {
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
                                if (editorId.startsWith(AndroidConstants.EDITORS_PLUGIN_ID)) {
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
                                AndroidConstants.EDITORS_PLUGIN_ID,
                                UNKNOWN_EDITOR);
                        try {
                            file.setPersistentProperty(qname, "1");
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
                                AndroidConstants.EDITORS_PLUGIN_ID,
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
                                        CommonPlugin.displayPrompt("Android XML Editor",
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
     * Respond to notifications from the resource manager than the SDK resources have been updated.
     * It gets the new resources from the {@link FrameworkResourceManager} and then try to update
     * the layout descriptors from the new layout data.
     */
    private class SdkPathChangedListener implements Runnable {
        public void run() {

            // Perform the update in a thread (here an Eclipse runtime job)
            // since this should never block the caller (especially the start method)
            new Job("Editors: Load Framework Resource Parser") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        SubMonitor progress = SubMonitor.convert(monitor, "Update Description",
                                60);
                        
                        FrameworkResourceManager resourceManager = FrameworkResourceManager.getInstance();
                        
                        AndroidManifestDescriptors.updateDescriptors(
                                resourceManager.getManifestDefinitions());
                        progress.worked(10);

                        if (progress.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }

                        LayoutDescriptors.getInstance().updateDescriptors(
                                resourceManager.getLayoutViewsInfo(),
                                resourceManager.getLayoutGroupsInfo());
                        progress.worked(10);

                        if (progress.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }

                        MenuDescriptors.getInstance().updateDescriptors(
                                resourceManager.getXmlMenuDefinitions());
                        progress.worked(10);

                        if (progress.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }

                        XmlDescriptors.getInstance().updateDescriptors(
                                resourceManager.getXmlSearchableDefinitions(),
                                resourceManager.getPreferencesInfo(),
                                resourceManager.getPreferenceGroupsInfo());
                        progress.worked(10);
                        
                        // load the layout lib bridge.
                        if (System.getenv("ANDROID_DISABLE_LAYOUT") == null) {
                            loadLayoutBridge();
                            FrameworkResourceManager frMgr = FrameworkResourceManager.getInstance();
                            ResourceManager rMgr = ResourceManager.getInstance(); 
                            rMgr.loadFrameworkResources(frMgr.getFrameworkResourcesLocation());
                        }
                        progress.worked(10);

                        // Notify resource changed listeners
                        progress.subTask("Refresh UI");
                        progress.setWorkRemaining(mResourceRefreshListener.size());
                        
                        // Clone the list before iterating, to avoid Concurrent Modification
                        // exceptions
                        @SuppressWarnings("unchecked")
                        ArrayList<Runnable> listeners = (ArrayList<Runnable>)
                                                            mResourceRefreshListener.clone();
                        for (Runnable listener : listeners) {
                            try {
                                getDisplay().syncExec(listener);
                            } catch (Exception e) {
                                log(e, "ResourceRefreshListener Failed");  //$NON-NLS-1$
                            } finally {
                                progress.worked(1);
                            }
                        }
                    } catch (Throwable e) {
                        EditorsPlugin.log(e, "Load Framework Resource Parser failed");  //$NON-NLS-1$
                        EditorsPlugin.printToConsole("Framework Resource Parser",
                                "Failed to parse");
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

    public void addResourceChangedListener(Runnable resourceRefreshListener) {
        mResourceRefreshListener.add(resourceRefreshListener);
    }

    public void removeResourceChangedListener(Runnable resourceRefreshListener) {
        mResourceRefreshListener.remove(resourceRefreshListener);
    }
    
    public static Display getDisplay() {
        IWorkbench bench = sPlugin.getWorkbench();
        if (bench != null) {
            return bench.getDisplay();
        }
        return null;
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
                
                SdkStatsHelper.pingUsageServer("editors", version); //$NON-NLS-1$
            }
        }.start();

    }

    /**
     * Prints one or more message to the android console.
     * @param tag The tag to be associated with the message. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static synchronized void printToConsole(String tag, Object... objects) {
        StreamHelper.printToStream(sPlugin.mAndroidConsoleStream, tag, objects);
    }

    /**
     * Prints one or more error messages to the android console.
     * @param tag The tag to be associated with the message. Can be null.
     * @param objects the objects to print through their <code>toString</code> method.
     */
    public static synchronized void printErrorToConsole(String tag, Object... objects) {
        StreamHelper.printToStream(sPlugin.mAndroidConsoleErrorStream, tag, objects);
    }
    
    public static synchronized OutputStream getErrorStream() {
        return sPlugin.mAndroidConsoleErrorStream;
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
        final boolean[] wrapper = new boolean[] { false };
        display.syncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                wrapper[0] = MessageDialog.openQuestion(shell, title, message);
            }
        });
        return wrapper[0];
    }

    /**
     * Returns a {@link LayoutBridge} object possibly containing a {@link ILayoutBridge} object.
     * <p/>If {@link LayoutBridge#bridge} is <code>null</code>, {@link LayoutBridge#status} will
     * contain the reason (either {@link LoadStatus#LOADING} or {@link LoadStatus#FAILED}).
     * <p/>Valid {@link ILayoutBridge} objects are always initialized before being returned.
     */
    public synchronized LayoutBridge getLayoutBridge() {
        if (mLayoutBridgeInit == false && mLayoutBridge.bridge != null) {
            FrameworkResourceManager manager = FrameworkResourceManager.getInstance(); 
            mLayoutBridge.bridge.init(
                        manager.getFrameworkFontLocation() + AndroidConstants.FD_DEFAULT_RES,
                        manager.getEnumValueMap());
            mLayoutBridgeInit = true;
        }
        return mLayoutBridge;
    }
    
    /**
     * Loads the layout bridge from the dynamically loaded layoutlib.jar
     */
    private void loadLayoutBridge() {
        try {
            // reset to be sure we won't be using an obsolete version if we
            // get an exception somewhere.
            mLayoutBridge.bridge = null;
            mLayoutBridge.status = LayoutBridge.LoadStatus.LOADING;
            
            // get the URL for the file.
            File f = new File(
                    FrameworkResourceManager.getInstance().getLayoutLibLocation());
            if (f.isFile() == false) {
                log(IStatus.ERROR, "layoutlib.jar is missing!"); //$NON-NLS-1$
            } else {
                URL url = f.toURL();
                
                // create a class loader. Because this jar reference interfaces
                // that are in the editors plugin, it's important to provide 
                // a parent class loader.
                mBridgeClassLoader = new URLClassLoader(new URL[] { url },
                        this.getClass().getClassLoader());
   
                // load the class
                Class<?> clazz = mBridgeClassLoader.loadClass(AndroidConstants.CLASS_BRIDGE);
                if (clazz != null) {
                    // instantiate an object of the class.
                    Constructor<?> constructor = clazz.getConstructor();
                    if (constructor != null) {
                        Object bridge = constructor.newInstance();
                        if (bridge instanceof ILayoutBridge) {
                            mLayoutBridge.bridge = (ILayoutBridge)bridge;
                        }
                    }
                }
                
                if (mLayoutBridge.bridge == null) {
                    mLayoutBridge.status = LayoutBridge.LoadStatus.FAILED;
                    log(IStatus.ERROR, "Failed to load " + AndroidConstants.CLASS_BRIDGE); //$NON-NLS-1$
                } else {
                    mLayoutBridge.status = LayoutBridge.LoadStatus.LOADED;
                }
            }
        } catch (Throwable t) {
            mLayoutBridge.status = LayoutBridge.LoadStatus.FAILED;
            // log the error.
            log(t, "Failed to load the LayoutLib");
        }
    }
    
    public ClassLoader getLayoutlibBridgeClassLoader() {
        return mBridgeClassLoader;
    }
}
