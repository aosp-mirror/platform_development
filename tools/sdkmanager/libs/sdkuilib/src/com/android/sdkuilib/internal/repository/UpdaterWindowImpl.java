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
import com.android.sdklib.internal.repository.RepoSource;
import com.android.sdklib.internal.repository.RepoSources;
import com.android.sdklib.repository.SdkRepository;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.tasks.ProgressTaskFactory;
import com.android.sdkuilib.repository.UpdaterWindow.ISdkListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * This is the private implementation of the UpdateWindow.
 */
public class UpdaterWindowImpl {

    private final Shell mParentShell;
    /** Internal data shared between the window and its pages. */
    private final UpdaterData mUpdaterData;
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
    private AvdManagerPage mAvdManagerPage;
    private StackLayout mStackLayout;

    public UpdaterWindowImpl(Shell parentShell, ISdkLog sdkLog, String osSdkRoot,
            boolean userCanChangeSdkRoot) {
        mParentShell = parentShell;
        mUpdaterData = new UpdaterData(osSdkRoot, sdkLog);
        mUpdaterData.setUserCanChangeSdkRoot(userCanChangeSdkRoot);
    }

    /**
     * Open the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        if (mParentShell == null) {
            Display.setAppName("Android"); //$hide$ (hide from SWT designer)
        }

        createContents();
        mAndroidSdkUpdater.open();
        mAndroidSdkUpdater.layout();

        postCreate();    //$hide$ (hide from SWT designer)

        Display display = Display.getDefault();
        while (!mAndroidSdkUpdater.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        dispose();  //$hide$
    }

    /**
     * Create contents of the window.
     */
    protected void createContents() {
        mAndroidSdkUpdater = new Shell(mParentShell);
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
        mAndroidSdkUpdater.setText("Android SDK");

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

        mAvdManagerPage = new AvdManagerPage(mPagesRootComposite, mUpdaterData);
        mLocalPackagePage = new LocalPackagesPage(mPagesRootComposite, mUpdaterData);
        mRemotePackagesPage = new RemotePackagesPage(mPagesRootComposite, mUpdaterData);

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

    public void addListeners(ISdkListener listener) {
        mUpdaterData.addListeners(listener);
    }

    public void removeListener(ISdkListener listener) {
        mUpdaterData.removeListener(listener);
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
        if (mUpdaterData != null) {
            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                imgFactory.dispose();
            }
        }
    }

    /**
     * Creates the icon of the window shell.
     */
    private void setWindowImage(Shell androidSdkUpdater) {
        String imageName = "android_icon_16.png"; //$NON-NLS-1$
        if (SdkConstants.currentPlatform() == SdkConstants.PLATFORM_DARWIN) {
            imageName = "android_icon_128.png"; //$NON-NLS-1$
        }

        if (mUpdaterData != null) {
            ImageFactory imgFactory = mUpdaterData.getImageFactory();
            if (imgFactory != null) {
                mAndroidSdkUpdater.setImage(imgFactory.getImageByName(imageName));
            }
        }
    }

    /**
     * Once the UI has been created, initializes the content.
     * This creates the pages, selects the first one, setup sources and scan for local folders.
     */
    private void postCreate() {
        mUpdaterData.setWindowShell(getShell());
        mTaskFactory = new ProgressTaskFactory(getShell());
        mUpdaterData.setTaskFactory(mTaskFactory);
        mUpdaterData.setImageFactory(new ImageFactory(getShell().getDisplay()));

        setWindowImage(mAndroidSdkUpdater);

        addPage(mAvdManagerPage,     "Virtual Devices");
        addPage(mLocalPackagePage,   "Installed Packages");
        addPage(mRemotePackagesPage, "Available Packages");
        addExtraPages();

        displayPage(0);
        mPageList.setSelection(0);

        setupSources();
        initializeSettings();
        mUpdaterData.notifyListeners();
    }

    /**
     * Called by the main loop when the window has been disposed.
     */
    private void dispose() {
        mUpdaterData.getSources().saveUserSources(mUpdaterData.getSdkLog());
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
        if (mExtraPages == null) {
            return;
        }

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
        RepoSources sources = mUpdaterData.getSources();
        sources.add(new RepoSource(SdkRepository.URL_GOOGLE_SDK_REPO_SITE, false /*userSource*/));

        String str = System.getenv("TEMP_SDK_URL"); // TODO STOPSHIP temporary remove before shipping
        if (str != null) {
            String[] urls = str.split(";");
            for (String url : urls) {
                sources.add(new RepoSource(url, false /*userSource*/));
            }
        }

        // Load user sources
        sources.loadUserSources(mUpdaterData.getSdkLog());

        mRemotePackagesPage.onSdkChange();
    }

    /**
     * Initializes settings.
     * This must be called after addExtraPages(), which created a settings page.
     * Iterate through all the pages to find the first (and supposedly unique) setting page,
     * and use it to load and apply these settings.
     */
    private void initializeSettings() {
        SettingsController c = mUpdaterData.getSettingsController();
        c.loadSettings();
        c.applySettings();

        for (Object page : mPages) {
            if (page instanceof ISettingsPage) {
                ISettingsPage settingsPage = (ISettingsPage) page;

                c.setSettingsPage(settingsPage);
                break;
            }
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
