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


import com.android.sdklib.internal.repository.Archive;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.RepoSource;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This is the private implementation of the UpdateWindow.
 */
public class UpdaterWindowImpl {

    private static final int NUM_FETCH_URL_MONITOR_INC = 10;

    private final UpdaterData mUpdaterData = new UpdaterData();
    private ArrayList<Composite> mPages = new ArrayList<Composite>();
    private boolean mInternalPageChange;

    // --- UI members ---

    protected Shell mAndroidSdkUpdater;
    private SashForm mSashForm;
    private List mPageList;
    private Composite mPagesRootComposite;
    private LocalPackagesPage mLocalPackagePage;
    private RemotePackagesPage mRemotePackagesPage;
    private StackLayout mStackLayout;
    private Image mIconImage;
    private ProgressTaskFactory mTaskFactory;

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

        mLocalPackagePage = new LocalPackagesPage(mPagesRootComposite, mUpdaterData);
        mRemotePackagesPage = new RemotePackagesPage(mPagesRootComposite, mUpdaterData, this);
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
        mTaskFactory = new ProgressTaskFactory(getShell());

        addPage(mLocalPackagePage, "Installed Packages");
        addPage(mRemotePackagesPage, "Available Packages");
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
        mUpdaterData.getSources().setTaskFactory(mTaskFactory);

        mUpdaterData.getSources().add(
                new RepoSource(SdkRepository.URL_GOOGLE_SDK_REPO_SITE, false /* addonOnly */));

        String url = System.getenv("TEMP_SDK_URL"); // TODO STOPSHIP temporary remove before shipping
        if (url != null) {
            mUpdaterData.getSources().add(new RepoSource(url, false /* addonOnly */));
        }

        mRemotePackagesPage.setInput(mUpdaterData.getSourcesAdapter());
    }

    private void scanLocalSdkFolders() {
        mUpdaterData.getLocalSdkAdapter().setSdkRoot(mUpdaterData.getOsSdkRoot());

        mLocalPackagePage.setInput(mUpdaterData.getLocalSdkAdapter());
    }

    public void installArchives(final Collection<Archive> archives) {
        // TODO move most parts to SdkLib, maybe as part of Archive, making archives self-installing.
        mTaskFactory.start("Installing Archives", new ITask() {
            public void run(ITaskMonitor monitor) {

                monitor.setProgressMax(archives.size() * (NUM_FETCH_URL_MONITOR_INC + 3));
                monitor.setDescription("Preparing to install archives");

                int num_installed = 0;
                for (Archive archive : archives) {

                    if (!archive.isCompatible()) {
                        monitor.setResult("Skipping incompatible archive: %1$s",
                                archive.getShortDescription());
                        monitor.incProgress(3);
                        continue;
                    }

                    File archiveFile = null;
                    try {
                        archiveFile = downloadArchive(archive, monitor);
                        monitor.incProgress(1);
                        if (archiveFile != null) {
                            if (installArchive(archive, archiveFile, monitor)) {
                                num_installed++;
                            }
                        }
                        monitor.incProgress(1);
                    } finally {
                        if (archiveFile != null) {
                            if (!archiveFile.delete()) {
                                archiveFile.deleteOnExit();
                            }
                        }
                    }
                }

                if (num_installed == 0) {
                    monitor.setResult("Nothing was installed.");
                }
            }
        });
    }

    /**
     * Downloads an archive and returns the temp file with it.
     * Caller is responsible with deleting the temp file when done.
     */
    private File downloadArchive(Archive archive, ITaskMonitor monitor) {

        try {
            File tmpFile = File.createTempFile("sdkupload", "bin"); //$NON-NLS-1$ //$NON-NLS-2$

            monitor.setDescription("Downloading %1$s", archive.getShortDescription());

            String link = archive.getUrl();
            if (!link.startsWith("http://")                          //$NON-NLS-1$
                    && !link.startsWith("https://")                  //$NON-NLS-1$
                    && !link.startsWith("ftp://")) {                 //$NON-NLS-1$
                // Make the URL absolute by prepending the source
                Package pkg = archive.getParentPackage();
                RepoSource src = pkg.getParentSource();
                if (src == null) {
                    monitor.setResult("Internal error: no source for archive %1$s",
                            archive.getShortDescription());
                    return null;
                }

                String base = src.getUrl();
                if (!base.endsWith("/") && !link.startsWith("/")) {  //$NON-NLS-1$ //$NON-NLS-2$
                    base += "/";                                     //$NON-NLS-1$
                }

                link = base + link;
            }

            fetchUrl(tmpFile, archive, link, monitor);

        } catch (IOException e) {
            monitor.setResult(e.getMessage());
        }
        return null;
    }

    /**
     * Actually performs the download.
     * Also computes the SHA1 of the file on the fly.
     * <p/>
     * Success is defined as downloading as many bytes as was expected and having the same
     * SHA1 as expected. Returns true on success or false if any of those checks fail.
     * <p/>
     * Increments the monitor by {@link #NUM_FETCH_URL_MONITOR_INC} (which is 10).
     */
    private boolean fetchUrl(File tmpFile, Archive archive, String urlString, ITaskMonitor monitor) {
        URL url;

        FileOutputStream os = null;
        InputStream is = null;
        try {
            url = new URL(urlString);
            is = url.openStream();
            os = new FileOutputStream(tmpFile);

            MessageDigest digester = archive.getChecksumType().getMessageDigest();

            byte[] buf = new byte[65536];
            int n;

            long total = 0;
            long size = archive.getSize();
            long inc = size / NUM_FETCH_URL_MONITOR_INC;
            long next_inc = inc;

            while ((n = is.read(buf)) >= 0) {
                if (n > 0) {
                    os.write(buf, 0, n);
                    digester.update(buf, 0, n);
                }

                total += n;
                if (total >= next_inc) {
                    monitor.incProgress(1);
                    next_inc += inc;
                }

                if (monitor.cancelRequested()) {
                    monitor.setResult("Download aborted by user at %1$d bytes.", total);
                    return false;
                }

            }

            if (total != size) {
                monitor.setResult("Download finished with wrong size. Expected %1$d bytes, got %2$d bytes.",
                        size, total);
                return false;
            }

            // Create an hex string from the digest
            byte[] digest = digester.digest();
            n = digest.length;
            String hex = "0123456789abcdef";                     //$NON-NLS-1$
            char[] hexDigest = new char[n * 2];
            for (int i = 0; i < n; i++) {
                byte b = digest[i];
                hexDigest[i*2 + 0] = hex.charAt(b >>> 4);
                hexDigest[i*2 + 1] = hex.charAt(b & 0x0f);
            }

            String expected = archive.getChecksum();
            String actual   = new String(hexDigest);
            if (!actual.equalsIgnoreCase(expected)) {
                monitor.setResult("Download finished with wrong checksum. Expected %1$s, got %2$s.",
                        expected, actual);
                return false;
            }

            return true;

        } catch (Exception e) {
            monitor.setResult(e.getMessage());

        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // pass
                }
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // pass
                }
            }
        }

        return false;
    }

    private boolean installArchive(Archive archive, File archiveFile, ITaskMonitor monitor) {
        monitor.setDescription("Installing %1$s", archive.getShortDescription());

        File destFolder = archive.getParentPackage().getInstallFolder(mUpdaterData.getOsSdkRoot());

        return false;
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
