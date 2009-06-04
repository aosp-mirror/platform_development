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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This is the private implementation of the UpdateWindow.
 */
public class UpdaterWindowImpl {

    private static final int NUM_MONITOR_INC = 100;

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
        InputStream stream = getClass().getResourceAsStream("android_icon_16.png");  //$NON-NLS-1$
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
    private void scanLocalSdkFolders() {
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

                final int progressPerArchive = 2 * NUM_MONITOR_INC + 10;
                monitor.setProgressMax(archives.size() * progressPerArchive);
                monitor.setDescription("Preparing to install archives");

                int numInstalled = 0;
                for (Archive archive : archives) {

                    int nextProgress = monitor.getProgress() + progressPerArchive;
                    File archiveFile = null;
                    try {
                        if (monitor.isCancelRequested()) {
                            break;
                        }

                        String name = archive.getParentPackage().getShortDescription();

                        // TODO: we should not see this test fail if we had the filter UI above.
                        if (!archive.isCompatible()) {
                            monitor.setResult("Skipping incompatible archive: %1$s", name);
                            continue;
                        }

                        archiveFile = downloadArchive(archive, monitor);
                        if (archiveFile != null) {
                            if (installArchive(archive, archiveFile, monitor)) {
                                monitor.setResult("Installed: %1$s", name);
                                numInstalled++;
                            }
                        }
                    } catch (Throwable t) {
                        // Display anything unexpected in the monitor.
                        monitor.setResult("Unexpected Error: %1$s", t.getMessage());

                    } finally {
                        // Delete the temp archive if it exists
                        deleteFileOrFolder(archiveFile);

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
     * Downloads an archive and returns the temp file with it.
     * Caller is responsible with deleting the temp file when done.
     */
    private File downloadArchive(Archive archive, ITaskMonitor monitor) {

        File tmpFileToDelete = null;
        try {
            File tmpFile = File.createTempFile("sdkupload", ".bin"); //$NON-NLS-1$ //$NON-NLS-2$
            tmpFileToDelete = tmpFile;

            String name = archive.getParentPackage().getShortDescription();
            String desc = String.format("Downloading %1$s", name);
            monitor.setDescription(desc);

            String link = archive.getUrl();
            if (!link.startsWith("http://")                          //$NON-NLS-1$
                    && !link.startsWith("https://")                  //$NON-NLS-1$
                    && !link.startsWith("ftp://")) {                 //$NON-NLS-1$
                // Make the URL absolute by prepending the source
                Package pkg = archive.getParentPackage();
                RepoSource src = pkg.getParentSource();
                if (src == null) {
                    monitor.setResult("Internal error: no source for archive %1$s", name);
                    return null;
                }

                // take the URL to the repository.xml and remove the last component
                // to get the base
                String repoXml = src.getUrl();
                int pos = repoXml.lastIndexOf('/');
                String base = repoXml.substring(0, pos + 1);

                link = base + link;
            }

            if (fetchUrl(tmpFile, archive, link, desc, monitor)) {
                // Fetching was successful, don't delete the temp file here!
                tmpFileToDelete = null;
                return tmpFile;
            }

        } catch (IOException e) {
            monitor.setResult(e.getMessage());

        } finally {
            deleteFileOrFolder(tmpFileToDelete);
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
     * Increments the monitor by {@link #NUM_MONITOR_INC}.
     */
    private boolean fetchUrl(File tmpFile,
            Archive archive,
            String urlString,
            String description,
            ITaskMonitor monitor) {
        URL url;

        description += " (%1$d%%, %2$.0f KiB/s, %3$d %4$s left)";

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
            long inc = size / NUM_MONITOR_INC;
            long next_inc = inc;

            long startMs = System.currentTimeMillis();
            long nextMs = startMs + 2000;  // start update after 2 seconds

            while ((n = is.read(buf)) >= 0) {
                if (n > 0) {
                    os.write(buf, 0, n);
                    digester.update(buf, 0, n);
                }

                long timeMs = System.currentTimeMillis();

                total += n;
                if (total >= next_inc) {
                    monitor.incProgress(1);
                    next_inc += inc;
                }

                if (timeMs > nextMs) {
                    long delta = timeMs - startMs;
                    if (total > 0 && delta > 0) {
                        // percent left to download
                        int percent = (int) (100 * total / size);
                        // speed in KiB/s
                        float speed = (float)total / (float)delta * (1000.f / 1024.f);
                        // time left to download the rest at the current KiB/s rate
                        int timeLeft = (speed > 1e-3) ?
                                               (int)(((size - total) / 1024.0f) / speed) :
                                               0;
                        String timeUnit = "seconds";
                        if (timeLeft > 120) {
                            timeUnit = "minutes";
                            timeLeft /= 60;
                        }

                        monitor.setDescription(description, percent, speed, timeLeft, timeUnit);
                    }
                    nextMs = timeMs + 1000;  // update every second
                }

                if (monitor.isCancelRequested()) {
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
                int b = digest[i] & 0x0FF;
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

    /**
     * Install the given archive in the given folder.
     */
    private boolean installArchive(Archive archive, File archiveFile, ITaskMonitor monitor) {
        String name = archive.getParentPackage().getShortDescription();
        String desc = String.format("Installing %1$s", name);
        monitor.setDescription(desc);

        File destFolder = archive.getParentPackage().getInstallFolder(mUpdaterData.getOsSdkRoot());

        File unzipDestFolder = destFolder;
        File renamedDestFolder = null;

        try {
            // If this folder already exists, unzip in a temporary folder and then move/unlink.
            if (destFolder.exists()) {
                // Find a new temp folder that doesn't exist yet
                unzipDestFolder = findTempFolder(destFolder, "new");  //$NON-NLS-1$

                if (unzipDestFolder == null) {
                    // this should not seriously happen.
                    monitor.setResult("Failed to find a suitable temp directory similar to %1$s.",
                            destFolder.getPath());
                    return false;
                }

                if (!unzipDestFolder.mkdirs()) {
                    monitor.setResult("Failed to create temp directory %1$s",
                            unzipDestFolder.getPath());
                    return false;
                }
            }

            if (!unzipFolder(archiveFile, archive.getSize(), unzipDestFolder, desc, monitor)) {
                return false;
            }

            if (unzipDestFolder != destFolder) {
                // Swap the old folder by the new one.
                // Both original folders will be deleted in the finally clause below.
                renamedDestFolder = findTempFolder(destFolder, "old");  //$NON-NLS-1$
                if (renamedDestFolder == null) {
                    // this should not seriously happen.
                    monitor.setResult("Failed to find a suitable temp directory similar to %1$s.",
                            destFolder.getPath());
                    return false;
                }

                if (!destFolder.renameTo(renamedDestFolder)) {
                    monitor.setResult("Failed to rename directory %1$s to %2$s",
                            destFolder.getPath(), renamedDestFolder.getPath());
                    return false;

                }
                if (!unzipDestFolder.renameTo(destFolder)) {
                    monitor.setResult("Failed to rename directory %1$s to %2$s",
                            unzipDestFolder.getPath(), destFolder.getPath());
                    return false;
                }
            }

            return true;

        } finally {
            // Cleanup if the unzip folder is still set.
            deleteFileOrFolder(renamedDestFolder);
            if (unzipDestFolder != destFolder) {
                deleteFileOrFolder(unzipDestFolder);
            }
        }
    }

    private boolean unzipFolder(File archiveFile,
            long compressedSize,
            File unzipDestFolder,
            String description,
            ITaskMonitor monitor) {

        description += " (%1$d%%)";

        FileInputStream fis = null;
        ZipInputStream  zis = null;
        try {
            fis = new FileInputStream(archiveFile);
            zis = new ZipInputStream(fis);

            // To advance the percent and the progress bar, we don't know the number of
            // items left to unzip. However we know the size of the archive and the size of
            // each uncompressed item. The zip file format overhead is negligible so that's
            // a good approximation.
            long incStep = compressedSize / NUM_MONITOR_INC;
            long incTotal = 0;
            long incCurr = 0;
            int lastPercent = 0;

            byte[] buf = new byte[65536];

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                String name = entry.getName();

                // ZipFile entries should have forward slashes, but not all Zip
                // implementations can be expected to do that.
                name = name.replace('\\', '/');

                File destFile = new File(unzipDestFolder, name);

                if (name.endsWith("/")) {  //$NON-NLS-1$
                    // Create directory if it doesn't exist yet. This allows us to create
                    // empty directories.
                    if (!destFile.isDirectory() && !destFile.mkdirs()) {
                        monitor.setResult("Failed to create temp directory %1$s",
                                destFile.getPath());
                        return false;
                    }
                    continue;
                } else if (name.indexOf('/') != -1) {
                    // Otherwise it's a file in a sub-directory.
                    // Make sure the parent directory has been created.
                    File parentDir = destFile.getParentFile();
                    if (!parentDir.isDirectory()) {
                        if (!parentDir.mkdirs()) {
                            monitor.setResult("Failed to create temp directory %1$s",
                                    parentDir.getPath());
                            return false;
                        }
                    }
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(destFile);
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        if (n > 0) {
                            fos.write(buf, 0, n);
                        }
                    }
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }

                // Increment progress bar to match. We update only between files.
                for(incTotal += entry.getCompressedSize(); incCurr < incTotal; incCurr += incStep) {
                    monitor.incProgress(1);
                }

                int percent = (int) (100 * incTotal / compressedSize);
                if (percent != lastPercent) {
                    monitor.setDescription(description, percent);
                    lastPercent = percent;
                }

                if (monitor.isCancelRequested()) {
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            monitor.setResult("Unzip failed: %1$s", e.getMessage());

        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    // pass
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // pass
                }
            }
        }

        return false;
    }

    /**
     * Finds a temp folder which name is similar to the one of the ideal folder
     * and with a ".tmpN" appended.
     * <p/>
     * This operation is not atomic so there's no guarantee the folder can't get
     * created in between. This is however unlikely and the caller can assume the
     * returned folder does not exist yet.
     * <p/>
     * Returns null if no such folder can be found (e.g. if all candidates exist),
     * which is rather unlikely.
     */
    private File findTempFolder(File idealFolder, String suffix) {
        String basePath = idealFolder.getPath();

        for (int i = 1; i < 100; i++) {
            File folder = new File(String.format("%1$s.%2$s%3$02d", basePath, suffix, i));  //$NON-NLS-1$
            if (!folder.exists()) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Deletes a file or a directory.
     * Directories are deleted recursively.
     * The argument can be null.
     */
    private void deleteFileOrFolder(File fileOrFolder) {
        if (fileOrFolder != null) {
            if (fileOrFolder.isDirectory()) {
                // Must delete content recursively first
                for (File item : fileOrFolder.listFiles()) {
                    deleteFileOrFolder(item);
                }
            }
            if (!fileOrFolder.delete()) {
                fileOrFolder.deleteOnExit();
            }
        }
    }

    // End of hiding from SWT Designer
    //$hide<<$
}
