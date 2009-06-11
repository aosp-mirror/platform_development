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
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskFactory;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.LocalSdkParser;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Collection;

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
    private final RepoSourcesAdapter mSourcesAdapter = new RepoSourcesAdapter(mSources);

    private ImageFactory mImageFactory;

    private final ArrayList<ISdkListener> mListeners = new ArrayList<ISdkListener>();

    public interface ISdkListener {
        void onSdkChange();
    }

    public UpdaterData(String osSdkRoot, ISdkLog sdkLog) {
        mOsSdkRoot = osSdkRoot;
        mSdkLog = sdkLog;

        initSdk();
    }

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
        mSourcesAdapter.setImageFactory(imageFactory);
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

    public void addListeners(ISdkListener listener) {
        if (mListeners.contains(listener) == false) {
            mListeners.add(listener);
        }
    }

    public void removeListener(ISdkListener listener) {
        mListeners.remove(listener);
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
                mAvdManager.reloadAvds();
            } catch (AndroidLocationException e) {
                // FIXME
            }
        }

        // notify adapters?
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
                mAvdManager.reloadAvds();
            } catch (AndroidLocationException e) {
                // FIXME
            }
        }
    }

    /**
     * Notify the listeners ({@link ISdkListener}) that the SDK was reloaded.
     * <p/>This can be called from any thread.
     */
    public void notifyListeners() {
        Display display = Display.getCurrent();
        if (display != null && mListeners.size() > 0) {
            display.syncExec(new Runnable() {
                public void run() {
                    for (ISdkListener listener : mListeners) {
                        try {
                            listener.onSdkChange();
                        } catch (Throwable t) {
                            // TODO: log error
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
     * @param archives The archives to install. Incompatible ones will be skipped.
     */
    public void installArchives(final Collection<Archive> archives) {
        if (mTaskFactory == null) {
            throw new IllegalArgumentException("Task Factory is null");
        }

        // TODO filter the archive list to: a/ display a list of what is going to be installed,
        // b/ display licenses and c/ check that the selected packages are actually upgrades
        // or ask user to confirm downgrades. All this should be done in a separate class+window
        // which will then call this method with the final list.

        mTaskFactory.start("Installing Archives", new ITask() {
            public void run(ITaskMonitor monitor) {

                final int progressPerArchive = 2 * Archive.NUM_MONITOR_INC;
                monitor.setProgressMax(archives.size() * progressPerArchive);
                monitor.setDescription("Preparing to install archives");

                int numInstalled = 0;
                for (Archive archive : archives) {

                    int nextProgress = monitor.getProgress() + progressPerArchive;
                    try {
                        if (monitor.isCancelRequested()) {
                            break;
                        }

                        if (archive.install(mOsSdkRoot, monitor)) {
                            numInstalled++;
                        }

                    } catch (Throwable t) {
                        // Display anything unexpected in the monitor.
                        monitor.setResult("Unexpected Error: %1$s", t.getMessage());

                    } finally {

                        // Always move the progress bar to the desired position.
                        // This allows internal methods to not have to care in case
                        // they abort early
                        monitor.incProgress(nextProgress - monitor.getProgress());
                    }
                }

                if (numInstalled == 0) {
                    monitor.setDescription("Done. Nothing was installed.");
                } else {
                    monitor.setDescription("Done. %1$d %2$s installed.",
                            numInstalled,
                            numInstalled == 1 ? "package" : "packages");
                }
            }
        });
    }

    /**
     * Tries to update all the *existing* local packages.
     * This first refreshes all sources, then compares the available remote packages when
     * the current local ones and suggest updates to be done to the user. Finally all
     * selected updates are installed.
     */
    public void updateAll() {
        assert mTaskFactory != null;

        mTaskFactory.start("Update Archives", new ITask() {
            public void run(ITaskMonitor monitor) {
                monitor.setProgressMax(3);

                monitor.setDescription("Refresh sources");
                refreshSources(true, monitor.createSubMonitor(1));

                // TODO compare available vs local
                // TODO suggest update packages to user (also validate license click-through)
                // TODO install selected packages
            }
        });
    }

    /**
     * Refresh all sources. This is invoked either internally (reusing an existing monitor)
     * or as a UI callback on the remote page "Refresh" button (in which case the monitor is
     * null and a new task should be created.)
     *
     * @param forceFetching When true, load sources that haven't been loaded yet. When
     * false, only refresh sources that have been loaded yet.
     */
    public void refreshSources(final boolean forceFetching, ITaskMonitor monitor) {
        assert mTaskFactory != null;

        ITask task = new ITask() {
            public void run(ITaskMonitor monitor) {
                ArrayList<RepoSource> sources = mSources.getSources();
                monitor.setProgressMax(sources.size());
                for (RepoSource source : sources) {
                    if (forceFetching || source.getPackages() != null) {
                        source.load(monitor.createSubMonitor(1));
                    }
                    monitor.incProgress(1);

                }
            }
        };

        if (monitor != null) {
            task.run(monitor);
        } else {
            mTaskFactory.start("Refresh Sources", task);
        }
    }

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
}
