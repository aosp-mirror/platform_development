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


import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdklib.repository.SdkRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This is the private implementation of the UpdateWindow.
 */
public class UpdaterWindowImpl {

    /** Internal data shared between the window and its pages. */
    private final UpdaterData mUpdaterData = new UpdaterData();
    /** The array of pages instances. Only one is visible at a time. */
    private ArrayList<Composite> mPages = new ArrayList<Composite>();
    /** Indicates a page change is due to an internal request. Prevents callbacks from looping. */
    private boolean mInternalPageChange;
    /** A list of extra pages to instantiate. Each entry is an object array with 2 elements:
     *  the string title and the Composite class to instantiate to create the page. */
    private ArrayList<Object[]> mExtraPages;
    /** A factory to create progress task dialogs. */
    private ProgressTaskFactory mTaskFactory;


    // --- UI members ---

    protected Shell mAndroidSdkUpdater;
    private SashForm mSashForm;
    private List mPageList;
    private Composite mPagesRootComposite;
    private LocalPackagesPage mLocalPackagePage;
    private RemotePackagesPage mRemotePackagesPage;
    private StackLayout mStackLayout;
    private Image mIconImage;

    public UpdaterWindowImpl(ISdkLog sdkLog, String osSdkRoot, boolean userCanChangeSdkRoot) {
        mUpdaterData.setSdkLog(sdkLog);
        mUpdaterData.setOsSdkRoot(osSdkRoot);
        mUpdaterData.setUserCanChangeSdkRoot(userCanChangeSdkRoot);
    }

    /**
     * Open the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        Display.setAppName("Android"); //$hide$ (hide from SWT designer)

        createContents();
        mAndroidSdkUpdater.open();
        mAndroidSdkUpdater.layout();

        firstInit();    //$hide$ (hide from SWT designer)

        Display display = Display.getDefault();
        while (!mAndroidSdkUpdater.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        mAndroidSdkUpdater = new Shell();
        setWindowImage(mAndroidSdkUpdater);
        mAndroidSdkUpdater.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                onAndroidSdkUpdaterDispose();    //$hide$ (hide from SWT designer)
            }
        });

        FillLayout fl;
        mAndroidSdkUpdater.setLayout(fl = new FillLayout(SWT.HORIZONTAL));
        fl.marginHeight = fl.marginWidth = 5;
        mAndroidSdkUpdater.setMinimumSize(new Point(200, 50));
        mAndroidSdkUpdater.setSize(745, 433);
        mAndroidSdkUpdater.setText("Android SDK Updater");

        mSashForm = new SashForm(mAndroidSdkUpdater, SWT.NONE);

        mPageList = new List(mSashForm, SWT.BORDER);
        mPageList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onPageListSelected();    //$hide$ (hide from SWT designer)
            }
        });

        mPagesRootComposite = new Composite(mSashForm, SWT.NONE);
        mStackLayout = new StackLayout();
        mPagesRootComposite.setLayout(mStackLayout);

        mLocalPackagePage = new LocalPackagesPage(mPagesRootComposite, mUpdaterData, this);
        mRemotePackagesPage = new RemotePackagesPage(mPagesRootComposite, mUpdaterData, this);
        mSashForm.setWeights(new int[] {150, 576});
    }

    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- UI Callbacks -----------


    /**
     * Registers an extra page for the updater window.
     * <p/>
     * Pages must derive from {@link Composite} and implement a constructor that takes
     * a single parent {@link Composite} argument.
     * <p/>
     * All pages must be registered before the call to {@link #open()}.
     *
     * @param title The title of the page.
     * @param pageClass The {@link Composite}-derived class that will implement the page.
     */
    public void registerExtraPage(String title, Class<? extends Composite> pageClass) {
        if (mExtraPages == null) {
            mExtraPages = new ArrayList<Object[]>();
        }
        mExtraPages.add(new Object[]{ title, pageClass });
    }

    /**
     * Helper to return the SWT shell.
     */
    private Shell getShell() {
        return mAndroidSdkUpdater;
    }

    /**
     * Callback called when the window shell is disposed.
     */
    private void onAndroidSdkUpdaterDispose() {
        if (mIconImage != null) {
            mIconImage.dispose();
            mIconImage = null;
        }
    }

    /**
     * Creates the icon of the window shell.
     * The icon is disposed by {@link #onAndroidSdkUpdaterDispose()}.
     */
    private void setWindowImage(Shell androidSdkUpdater) {
        String image = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            image = "android_icon_128.png"; //$NON-NLS-1$
        }
        InputStream stream = getClass().getResourceAsStream(image);
        if (stream != null) {
            try {
                ImageData imgData = new ImageData(stream);
                mIconImage = new Image(mAndroidSdkUpdater.getDisplay(),
                        imgData,
                        imgData.getTransparencyMask());
                mAndroidSdkUpdater.setImage(mIconImage);
            } catch (SWTException e) {
                mUpdaterData.getSdkLog().error(e, "Failed to set window icon");  //$NON-NLS-1$
            } catch (IllegalArgumentException e) {
                mUpdaterData.getSdkLog().error(e, "Failed to set window icon");  //$NON-NLS-1$
            }
        }
    }

    /**
     * Once the UI has been created, initializes the content.
     * This creates the pages, selects the first one, setup sources and scan for local folders.
     */
    private void firstInit() {
        mTaskFactory = new ProgressTaskFactory(getShell());

        addPage(mLocalPackagePage, "Installed Packages");
        addPage(mRemotePackagesPage, "Available Packages");
        addExtraPages();

        displayPage(0);
        mPageList.setSelection(0);

        // TODO read and apply settings
        // TODO read add-on sources from some file
        setupSources();
        scanLocalSdkFolders();
    }

    // --- page switching ---

    /**
     * Adds an instance of a page to the page list.
     * <p/>
     * Each page is a {@link Composite}. The title of the page is stored in the
     * {@link Composite#getData()} field.
     */
    private void addPage(Composite page, String title) {
        page.setData(title);
        mPages.add(page);
        mPageList.add(title);
    }

    /**
     * Adds all extra pages. For each page, instantiates an instance of the {@link Composite}
     * using the constructor that takes a single {@link Composite} argument and then adds it
     * to the page list.
     */
    @SuppressWarnings("unchecked")
    private void addExtraPages() {
        for (Object[] extraPage : mExtraPages) {
            String title = (String) extraPage[0];
            Class<? extends Composite> clazz = (Class<? extends Composite>) extraPage[1];

            // We want the constructor that takes a single Composite as parameter
            Constructor<? extends Composite> cons;
            try {
                cons = clazz.getConstructor(new Class<?>[] { Composite.class });
                Composite instance = cons.newInstance(new Object[] { mPagesRootComposite });
                addPage(instance, title);

            } catch (NoSuchMethodException e) {
                // There is no such constructor.
                mUpdaterData.getSdkLog().error(e,
                        "Failed to add extra page %1$s. Constructor args must be (Composite parent).",  //$NON-NLS-1$
                        clazz.getSimpleName());

            } catch (Exception e) {
                // Log this instead of crashing the whole app.
                mUpdaterData.getSdkLog().error(e,
                        "Failed to add extra page %1$s.",  //$NON-NLS-1$
                        clazz.getSimpleName());
            }
        }
    }

    /**
     * Callback invoked when an item is selected in the page list.
     * If this is not an internal page change, displays the given page.
     */
    private void onPageListSelected() {
        if (mInternalPageChange == false) {
            int index = mPageList.getSelectionIndex();
            if (index >= 0) {
                displayPage(index);
            }
        }
    }

    /**
     * Displays the page at the given index.
     *
     * @param index An index between 0 and {@link #mPages}'s length - 1.
     */
    private void displayPage(int index) {
        Composite page = mPages.get(index);
        if (page != null) {
            mStackLayout.topControl = page;
            mPagesRootComposite.layout(true);

            if (!mInternalPageChange) {
                mInternalPageChange = true;
                mPageList.setSelection(index);
                mInternalPageChange = false;
            }
        }
    }

    /**
     * Used to initialize the sources.
     */
    private void setupSources() {
        mUpdaterData.getSources().setTaskFactory(mTaskFactory);

        mUpdaterData.getSources().add(
                new RepoSource(SdkRepository.URL_GOOGLE_SDK_REPO_SITE, false /* addonOnly */));

        String url = System.getenv("TEMP_SDK_URL"); // TODO STOPSHIP temporary remove before shipping
        if (url != null) {
            mUpdaterData.getSources().add(new RepoSource(url, false /* addonOnly */));
        }

        mRemotePackagesPage.setInput(mUpdaterData.getSourcesAdapter());
    }

    /**
     * Used to scan the local SDK folders the first time.
     */
    public void scanLocalSdkFolders() {
        mUpdaterData.getLocalSdkAdapter().setSdkRoot(mUpdaterData.getOsSdkRoot());

        mLocalPackagePage.setInput(mUpdaterData.getLocalSdkAdapter());
    }

    /**
     * Install the list of given {@link Archive}s.
     * @param archives The archives to install. Incompatible ones will be skipped.
     */
    public void installArchives(final Collection<Archive> archives) {

        // TODO filter the archive list to: a/ display a list of what is going to be installed,
        // b/ display licenses and c/ check that the selected packages are actually upgrades
        // or ask user to confirm downgrades. All this should be done in a separate class+window
        // which will then call this method with the final list.

        // TODO move most parts to SdkLib, maybe as part of Archive, making archives self-installing.
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

                        if (archive.install(mUpdaterData.getOsSdkRoot(), monitor)) {
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

    public void updateAll() {
        refreshSources(true);

        // TODO have refreshSources reuse the current progress task
//        mTaskFactory.start("Update Archives", new ITask() {
//            public void run(ITaskMonitor monitor) {
//                monitor.setProgressMax(3);
//
//                monitor.setDescription("Refresh sources");
//                refreshSources(true);
//            }
//        });
    }

    /**
     * Refresh sources
     *
     * @param forceFetching When true, load sources that haven't been loaded yet. When
     * false, only refresh sources that have been loaded yet.
     */
    public void refreshSources(final boolean forceFetching) {
        ArrayList<RepoSource> sources = mUpdaterData.getSources().getSources();
        for (RepoSource source : sources) {
            if (forceFetching || source.getPackages() != null) {
                source.load(mTaskFactory);
            }

        }

        // TODO have source.load reuse the current progress task
//        mTaskFactory.start("Refresh sources", new ITask() {
//            public void run(ITaskMonitor monitor) {
//                ArrayList<RepoSource> sources = mUpdaterData.getSources().getSources();
//                monitor.setProgressMax(sources.size());
//
//                for (RepoSource source : sources) {
//                    if (forceFetching || source.getPackages() != null) {
//                        monitor.setDescription(String.format("Refresh %1$s",
//                                source.getShortDescription()));
//                        source.load(mTaskFactory);
//                    }
//                    monitor.incProgress(1);
//                }
//            }
//        });

    }

    // End of hiding from SWT Designer
    //$hide<<$
}
