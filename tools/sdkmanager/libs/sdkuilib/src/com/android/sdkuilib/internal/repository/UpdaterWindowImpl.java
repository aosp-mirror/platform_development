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


import com.android.sdklib.internal.repository.RepoSource;

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
import java.util.ArrayList;

/**
 * This is the private implementation of the UpdateWindow.
 */
public class UpdaterWindowImpl {

    private final UpdaterData mUpdaterData = new UpdaterData();
    private ArrayList<Composite> mPages = new ArrayList<Composite>();
    private boolean mInternalPageChange;

    // --- UI members ---

    protected Shell mAndroidSdkUpdater;
    private SashForm mSashForm;
    private List mPageList;
    private Composite mPagesRootComposite;
    private InstalledPackagesPage mInstalledPackagePage;
    private AvailablePackagesPage mAvailablePackagesPage;
    private StackLayout mStackLayout;
    private Image mIconImage;

    public UpdaterWindowImpl(String osSdkRoot, boolean userCanChangeSdkRoot) {
        mUpdaterData.setOsSdkRoot(osSdkRoot);
        mUpdaterData.setUserCanChangeSdkRoot(userCanChangeSdkRoot);
    }

    /**
     * Open the window.
     * @wbp.parser.entryPoint
     */
    public void open() {
        Display display = Display.getDefault();
        createContents();
        mAndroidSdkUpdater.open();
        mAndroidSdkUpdater.layout();

        firstInit();    //$hide$ (hide from SWT designer)

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

        mAndroidSdkUpdater.setLayout(new FillLayout(SWT.HORIZONTAL));
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

        mInstalledPackagePage = new InstalledPackagesPage(mPagesRootComposite, mUpdaterData);
        mAvailablePackagesPage = new AvailablePackagesPage(mPagesRootComposite, mUpdaterData);
        mSashForm.setWeights(new int[] {150, 576});
    }


    // -- Start of internal part ----------
    // Hide everything down-below from SWT designer
    //$hide>>$

    // --- UI Callbacks -----------

    private void onAndroidSdkUpdaterDispose() {
        if (mIconImage != null) {
            mIconImage.dispose();
            mIconImage = null;
        }
    }

    private void setWindowImage(Shell androidSdkUpdater) {
        InputStream stream = getClass().getResourceAsStream("android_icon_16.png");  //$NON-NLS-1$
        if (stream != null) {
            try {
                ImageData imgData = new ImageData(stream);
                mIconImage = new Image(mAndroidSdkUpdater.getDisplay(),
                        imgData,
                        imgData.getTransparencyMask());
                mAndroidSdkUpdater.setImage(mIconImage);
            } catch (SWTException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }

    private Shell getShell() {
        return mAndroidSdkUpdater;
    }

    /**
     * Once the UI has been created, initialize the content
     */
    private void firstInit() {
        addPage(mInstalledPackagePage, "Installed Packages");
        addPage(mAvailablePackagesPage, "Available Packages");
        displayPage(0);
        mPageList.setSelection(0);

        setupSources();
        scanLocalSdkFolders();
    }

    // --- page switching ---

    private void addPage(Composite page, String title) {
        page.setData(title);
        mPages.add(page);
        mPageList.add(title);
    }

    private void onPageListSelected() {
        if (mInternalPageChange == false) {
            int index = mPageList.getSelectionIndex();
            if (index >= 0) {
                displayPage(index);
            }
        }
    }

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

    private void setupSources() {
        mUpdaterData.getSources().setTaskFactory(new ProgressTaskFactory(getShell()));

        mUpdaterData.getSources().add(new RepoSource(
                "https://dl.google.com/android/eclipse/repository/index.xml",          //$NON-NLS-1$
                false /* addonOnly */));

        String url = System.getenv("TEMP_SDK_URL"); // TODO STOPSHIP temporary remove before shipping
        if (url != null) {
            mUpdaterData.getSources().add(new RepoSource(url, false /* addonOnly */));
        }

        mAvailablePackagesPage.setInput(mUpdaterData.getSources());
    }

    private void scanLocalSdkFolders() {
        // TODO Auto-generated method stub

    }

    // End of hiding from SWT Designer
    //$hide<<$
}
