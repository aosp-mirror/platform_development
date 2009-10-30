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

package com.android.sdkuilib.internal.repository;

import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.repository.AddonPackage;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskFactory;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.LocalSdkParser;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdklib.internal.repository.ToolPackage;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.repository.UpdaterWindow.ISdkListener;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Data shared between {@link UpdaterWindowImpl} and its pages.
 */
class UpdaterData {
    private String mOsSdkRoot;

    private final ISdkLog mSdkLog;
    private ITaskFactory mTaskFactory;
    private boolean mUserCanChangeSdkRoot;

    private SdkManager mSdkManager;
    private AvdManager mAvdManager;

    private final LocalSdkParser mLocalSdkParser = new LocalSdkParser();
    private final RepoSources mSources = new RepoSources();

    private final LocalSdkAdapter mLocalSdkAdapter = new LocalSdkAdapter(this);
    private final RepoSourcesAdapter mSourcesAdapter = new RepoSourcesAdapter(this);

    private ImageFactory mImageFactory;

    private final SettingsController mSettingsController;

    private final ArrayList<ISdkListener> mListeners = new ArrayList<ISdkListener>();

    private Shell mWindowShell;

    public UpdaterData(String osSdkRoot, ISdkLog sdkLog) {
        mOsSdkRoot = osSdkRoot;
        mSdkLog = sdkLog;

        mSettingsController = new SettingsController(this);

        initSdk();
    }

    // ----- getters, setters ----

    public void setOsSdkRoot(String osSdkRoot) {
        if (mOsSdkRoot == null || mOsSdkRoot.equals(osSdkRoot) == false) {
            mOsSdkRoot = osSdkRoot;
            initSdk();
        }
    }

    public String getOsSdkRoot() {
        return mOsSdkRoot;
    }

    public void setTaskFactory(ITaskFactory taskFactory) {
        mTaskFactory = taskFactory;
    }

    public ITaskFactory getTaskFactory() {
        return mTaskFactory;
    }

    public void setUserCanChangeSdkRoot(boolean userCanChangeSdkRoot) {
        mUserCanChangeSdkRoot = userCanChangeSdkRoot;
    }

    public boolean canUserChangeSdkRoot() {
        return mUserCanChangeSdkRoot;
    }

    public RepoSources getSources() {
        return mSources;
    }

    public RepoSourcesAdapter getSourcesAdapter() {
        return mSourcesAdapter;
    }

    public LocalSdkParser getLocalSdkParser() {
        return mLocalSdkParser;
    }

    public LocalSdkAdapter getLocalSdkAdapter() {
        return mLocalSdkAdapter;
    }

    public ISdkLog getSdkLog() {
        return mSdkLog;
    }

    public void setImageFactory(ImageFactory imageFactory) {
        mImageFactory = imageFactory;
    }

    public ImageFactory getImageFactory() {
        return mImageFactory;
    }

    public SdkManager getSdkManager() {
        return mSdkManager;
    }

    public AvdManager getAvdManager() {
        return mAvdManager;
    }

    public SettingsController getSettingsController() {
        return mSettingsController;
    }

    /** Adds a listener ({@link ISdkListener}) that is notified when the SDK is reloaded. */
    public void addListeners(ISdkListener listener) {
        if (mListeners.contains(listener) == false) {
            mListeners.add(listener);
        }
    }

    /** Removes a listener ({@link ISdkListener}) that is notified when the SDK is reloaded. */
    public void removeListener(ISdkListener listener) {
        mListeners.remove(listener);
    }

    public void setWindowShell(Shell windowShell) {
        mWindowShell = windowShell;
    }

    public Shell getWindowShell() {
        return mWindowShell;
    }

    // -----

    /**
     * Initializes the {@link SdkManager} and the {@link AvdManager}.
     */
    private void initSdk() {
        mSdkManager = SdkManager.createManager(mOsSdkRoot, mSdkLog);
        try {
            mAvdManager = null; // remove the old one if needed.
            mAvdManager = new AvdManager(mSdkManager, mSdkLog);
        } catch (AndroidLocationException e) {
            mSdkLog.error(e, "Unable to read AVDs");
        }

        // notify adapters/parsers
        // TODO

        // notify listeners.
        notifyListeners();
    }

    /**
     * Reloads the SDK content (targets).
     * <p/> This also reloads the AVDs in case their status changed.
     * <p/>This does not notify the listeners ({@link ISdkListener}).
     */
    public void reloadSdk() {
        // reload SDK
        mSdkManager.reloadSdk(mSdkLog);

        // reload AVDs
        if (mAvdManager != null) {
            try {
                mAvdManager.reloadAvds(mSdkLog);
            } catch (AndroidLocationException e) {
                // FIXME
            }
        }

        // notify adapters?
        mLocalSdkParser.clearPackages();
        // TODO

        // notify listeners
        notifyListeners();
    }

    /**
     * Reloads the AVDs.
     * <p/>This does not notify the listeners.
     */
    public void reloadAvds() {
        // reload AVDs
        if (mAvdManager != null) {
            try {
                mAvdManager.reloadAvds(mSdkLog);
            } catch (AndroidLocationException e) {
                mSdkLog.error(e, null);
            }
        }
    }

    /**
     * Returns the list of installed packages, parsing them if this has not yet been done.
     */
    public Package[] getInstalledPackage() {
        LocalSdkParser parser = getLocalSdkParser();

        Package[] packages = parser.getPackages();

        if (packages == null) {
            // load on demand the first time
            packages = parser.parseSdk(getOsSdkRoot(), getSdkManager(), getSdkLog());
        }

        return packages;
    }

    /**
     * Notify the listeners ({@link ISdkListener}) that the SDK was reloaded.
     * <p/>This can be called from any thread.
     */
    public void notifyListeners() {
        if (mWindowShell != null && mListeners.size() > 0) {
            mWindowShell.getDisplay().syncExec(new Runnable() {
                public void run() {
                    for (ISdkListener listener : mListeners) {
                        try {
                            listener.onSdkChange();
                        } catch (Throwable t) {
                            mSdkLog.error(t, null);
                        }
                    }
                }
            });
        }
    }

    /**
     * Install the list of given {@link Archive}s. This is invoked by the user selecting some
     * packages in the remote page and then clicking "install selected".
     *
     * @param result The archives to install. Incompatible ones will be skipped.
     */
    public void installArchives(final ArrayList<ArchiveInfo> result) {
        if (mTaskFactory == null) {
            throw new IllegalArgumentException("Task Factory is null");
        }

        final boolean forceHttp = getSettingsController().getForceHttp();

        mTaskFactory.start("Installing Archives", new ITask() {
            public void run(ITaskMonitor monitor) {

                final int progressPerArchive = 2 * Archive.NUM_MONITOR_INC;
                monitor.setProgressMax(result.size() * progressPerArchive);
                monitor.setDescription("Preparing to install archives");

                boolean installedAddon = false;
                boolean installedTools = false;

                // Mark all current local archives as already installed.
                HashSet<Archive> installedArchives = new HashSet<Archive>();
                for (Package p : getInstalledPackage()) {
                    for (Archive a : p.getArchives()) {
                        installedArchives.add(a);
                    }
                }

                int numInstalled = 0;
                for (ArchiveInfo ai : result) {
                    Archive archive = ai.getNewArchive();

                    int nextProgress = monitor.getProgress() + progressPerArchive;
                    try {
                        if (monitor.isCancelRequested()) {
                            break;
                        }

                        ArchiveInfo adep = ai.getDependsOn();
                        if (adep != null && !installedArchives.contains(adep.getNewArchive())) {
                            // This archive depends on another one that was not installed.
                            // Skip it.
                            monitor.setResult("Skipping '%1$s'; it depends on '%2$s' which was not installed.",
                                    archive.getParentPackage().getShortDescription(),
                                    adep.getNewArchive().getParentPackage().getShortDescription());
                        }

                        if (archive.install(mOsSdkRoot, forceHttp, mSdkManager, monitor)) {
                            // We installed this archive.
                            installedArchives.add(archive);
                            numInstalled++;

                            // If this package was replacing an existing one, the old one
                            // is no longer installed.
                            installedArchives.remove(ai.getReplaced());

                            // Check if we successfully installed a tool or add-on package.
                            if (archive.getParentPackage() instanceof AddonPackage) {
                                installedAddon = true;
                            } else if (archive.getParentPackage() instanceof ToolPackage) {
                                installedTools = true;
                            }
                        }

                    } catch (Throwable t) {
                        // Display anything unexpected in the monitor.
                        String msg = t.getMessage();
                        if (msg != null) {
                            monitor.setResult("Unexpected Error installing '%1$s': %2$s",
                                    archive.getParentPackage().getShortDescription(), msg);
                        } else {
                            // no error info? get the stack call to display it
                            // At least that'll give us a better bug report.
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            t.printStackTrace(new PrintStream(baos));

                            // and display it
                            monitor.setResult("Unexpected Error installing '%1$s'\n%2$s",
                                    archive.getParentPackage().getShortDescription(),
                                    baos.toString());
                        }
                    } finally {

                        // Always move the progress bar to the desired position.
                        // This allows internal methods to not have to care in case
                        // they abort early
                        monitor.incProgress(nextProgress - monitor.getProgress());
                    }
                }

                if (installedAddon) {
                    // Update the USB vendor ids for adb
                    try {
                        mSdkManager.updateAdb();
                        monitor.setResult("Updated ADB to support the USB devices declared in the SDK add-ons.");
                    } catch (Exception e) {
                        mSdkLog.error(e, "Update ADB failed");
                        monitor.setResult("failed to update adb to support the USB devices declared in the SDK add-ons.");
                    }
                }

                if (installedAddon || installedTools) {
                    // We need to restart ADB. Actually since we don't know if it's even
                    // running, maybe we should just kill it and not start it.
                    // Note: it turns out even under Windows we don't need to kill adb
                    // before updating the tools folder, as adb.exe is (surprisingly) not
                    // locked.

                    askForAdbRestart(monitor);
                }

                if (installedTools) {
                    notifyToolsNeedsToBeRestarted();
                }

                if (numInstalled == 0) {
                    monitor.setDescription("Done. Nothing was installed.");
                } else {
                    monitor.setDescription("Done. %1$d %2$s installed.",
                            numInstalled,
                            numInstalled == 1 ? "package" : "packages");

                    //notify listeners something was installed, so that they can refresh
                    reloadSdk();
                }
            }
        });
    }

    /**
     * Attemps to restart ADB.
     *
     * If the "ask before restart" setting is set (the default), prompt the user whether
     * now is a good time to restart ADB.
     * @param monitor
     */
    private void askForAdbRestart(ITaskMonitor monitor) {
        final boolean[] canRestart = new boolean[] { true };

        if (getSettingsController().getAskBeforeAdbRestart()) {
            // need to ask for permission first
            Display display = mWindowShell.getDisplay();

            display.syncExec(new Runnable() {
                public void run() {
                    canRestart[0] = MessageDialog.openQuestion(mWindowShell,
                            "ADB Restart",
                            "A package that depends on ADB has been updated. It is recommended " +
                            "to restart ADB. Is it OK to do it now? If not, you can restart it " +
                            "manually later.");
                }
            });
        }

        if (canRestart[0]) {
            AdbWrapper adb = new AdbWrapper(getOsSdkRoot(), monitor);
            adb.stopAdb();
            adb.startAdb();
        }
    }

    private void notifyToolsNeedsToBeRestarted() {
        Display display = mWindowShell.getDisplay();

        display.syncExec(new Runnable() {
            public void run() {
                MessageDialog.openInformation(mWindowShell,
                        "Android Tools Updated",
                        "The Android SDK and AVD Manager that you are currently using has been updated. " +
                        "It is recommended that you now close the manager window and re-open it. " +
                        "If you started this window from Eclipse, please check if the Android " +
                        "plug-in needs to be updated.");
            }
        });
    }


    /**
     * Tries to update all the *existing* local packages.
     * <p/>
     * There are two modes of operation:
     * <ul>
     * <li>If selectedArchives is null, refreshes all sources, compares the available remote
     * packages with the current local ones and suggest updates to be done to the user (including
     * new platforms that the users doesn't have yet).
     * <li>If selectedArchives is not null, this represents a list of archives/packages that
     * the user wants to install or update, so just process these.
     * </ul>
     *
     * @param selectedArchives The list of remote archive to consider for the update.
     *  This can be null, in which case a list of remote archive is fetched from all
     *  available sources.
     */
    public void updateOrInstallAll(Collection<Archive> selectedArchives) {
        if (selectedArchives == null) {
            refreshSources(true);
        }

        UpdaterLogic ul = new UpdaterLogic();
        ArrayList<ArchiveInfo> archives = ul.computeUpdates(
                selectedArchives,
                getSources(),
                getLocalSdkParser().getPackages());

        if (selectedArchives == null) {
            ul.addNewPlatforms(archives, getSources(), getLocalSdkParser().getPackages());
        }

        // TODO if selectedArchives is null and archives.len==0, find if there's
        // any new platform we can suggest to install instead.

        UpdateChooserDialog dialog = new UpdateChooserDialog(getWindowShell(), this, archives);
        dialog.open();

        ArrayList<ArchiveInfo> result = dialog.getResult();
        if (result != null && result.size() > 0) {
            installArchives(result);
        }
    }
    /**
     * Refresh all sources. This is invoked either internally (reusing an existing monitor)
     * or as a UI callback on the remote page "Refresh" button (in which case the monitor is
     * null and a new task should be created.)
     *
     * @param forceFetching When true, load sources that haven't been loaded yet.
     *                      When false, only refresh sources that have been loaded yet.
     */
    public void refreshSources(final boolean forceFetching) {
        assert mTaskFactory != null;

        final boolean forceHttp = getSettingsController().getForceHttp();

        mTaskFactory.start("Refresh Sources",new ITask() {
            public void run(ITaskMonitor monitor) {
                RepoSource[] sources = mSources.getSources();
                monitor.setProgressMax(sources.length);
                for (RepoSource source : sources) {
                    if (forceFetching ||
                            source.getPackages() != null ||
                            source.getFetchError() != null) {
                        source.load(monitor.createSubMonitor(1), forceHttp);
                    }
                    monitor.incProgress(1);
                }
            }
        });
    }
}
