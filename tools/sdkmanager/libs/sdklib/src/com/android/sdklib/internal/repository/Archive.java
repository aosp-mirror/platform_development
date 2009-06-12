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

package com.android.sdklib.internal.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * A {@link Archive} is the base class for "something" that can be downloaded from
 * the SDK repository -- subclasses include {@link PlatformPackage}, {@link AddonPackage},
 * {@link DocPackage} and {@link ToolPackage}.
 * <p/>
 * A package has some attributes (revision, description) and a list of archives
 * which represent the downloadable bits.
 * <p/>
 * Packages are contained in offered by a {@link RepoSource} (a download site).
 */
public class Archive implements IDescription {

    public static final int NUM_MONITOR_INC = 100;

    /** The checksum type. */
    public enum ChecksumType {
        /** A SHA1 checksum, represented as a 40-hex string. */
        SHA1("SHA-1");  //$NON-NLS-1$

        private final String mAlgorithmName;

        /**
         * Constructs a {@link ChecksumType} with the algorigth name
         * suitable for {@link MessageDigest#getInstance(String)}.
         * <p/>
         * These names are officially documented at
         * http://java.sun.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
         */
        private ChecksumType(String algorithmName) {
            mAlgorithmName = algorithmName;
        }

        /**
         * Returns a new {@link MessageDigest} instance for this checksum type.
         * @throws NoSuchAlgorithmException if this algorithm is not available.
         */
        public MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(mAlgorithmName);
        }
    }

    /** The OS that this archive can be downloaded on. */
    public enum Os {
        ANY("Any"),
        LINUX("Linux"),
        MACOSX("MacOS X"),
        WINDOWS("Windows");

        private final String mUiName;

        private Os(String uiName) {
            mUiName = uiName;
        }

        /** Returns the UI name of the OS. */
        public String getUiName() {
            return mUiName;
        }

        /**
         * Returns the current OS as one of the {@link Os} enum values or null.
         */
        public static Os getCurrentOs() {
            String os = System.getProperty("os.name");          //$NON-NLS-1$
            if (os.startsWith("Mac")) {                         //$NON-NLS-1$
                return Os.MACOSX;

            } else if (os.startsWith("Windows")) {              //$NON-NLS-1$
                return Os.WINDOWS;

            } else if (os.startsWith("Linux")) {                //$NON-NLS-1$
                return Os.LINUX;
            }

            return null;
        }
    }

    /** The Architecture that this archive can be downloaded on. */
    public enum Arch {
        ANY("Any"),
        PPC("PowerPC"),
        X86("x86"),
        X86_64("x86_64");

        private final String mUiName;

        private Arch(String uiName) {
            mUiName = uiName;
        }

        /** Returns the UI name of the architecture. */
        public String getUiName() {
            return mUiName;
        }

        /**
         * Returns the current architecture as one of the {@link Arch} enum values or null.
         */
        public static Arch getCurrentArch() {
            // Values listed from http://lopica.sourceforge.net/os.html
            String arch = System.getProperty("os.arch");

            if (arch.equalsIgnoreCase("x86_64") || arch.equalsIgnoreCase("amd64")) {
                return Arch.X86_64;

            } else if (arch.equalsIgnoreCase("x86")
                    || arch.equalsIgnoreCase("i386")
                    || arch.equalsIgnoreCase("i686")) {
                return Arch.X86;

            } else if (arch.equalsIgnoreCase("ppc") || arch.equalsIgnoreCase("PowerPC")) {
                return Arch.PPC;
            }

            return null;
        }
    }

    private final Os     mOs;
    private final Arch   mArch;
    private final String mUrl;
    private final long   mSize;
    private final String mChecksum;
    private final ChecksumType mChecksumType = ChecksumType.SHA1;
    private final Package mPackage;
    private final String mLocalOsPath;
    private final boolean mIsLocal;

    /**
     * Creates a new remote archive.
     */
    Archive(Package pkg, Os os, Arch arch, String url, long size, String checksum) {
        mPackage = pkg;
        mOs = os;
        mArch = arch;
        mUrl = url;
        mLocalOsPath = null;
        mSize = size;
        mChecksum = checksum;
        mIsLocal = false;
    }

    /**
     * Creates a new local archive.
     */
    Archive(Package pkg, Os os, Arch arch, String localOsPath) {
        mPackage = pkg;
        mOs = os;
        mArch = arch;
        mUrl = null;
        mLocalOsPath = localOsPath;
        mSize = 0;
        mChecksum = "";
        mIsLocal = true;
    }

    /**
     * Returns true if this is a locally installed archive.
     * Returns false if this is a remote archive that needs to be downloaded.
     */
    public boolean isLocal() {
        return mIsLocal;
    }

    /**
     * Returns the package that created and owns this archive.
     * It should generally not be null.
     */
    public Package getParentPackage() {
        return mPackage;
    }

    /**
     * Returns the archive size, an int > 0.
     * Size will be 0 if this a local installed folder of unknown size.
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Returns the SHA1 archive checksum, as a 40-char hex.
     * Can be empty but not null for local installed folders.
     */
    public String getChecksum() {
        return mChecksum;
    }

    /**
     * Returns the checksum type, always {@link ChecksumType#SHA1} right now.
     */
    public ChecksumType getChecksumType() {
        return mChecksumType;
    }

    /**
     * Returns the download archive URL, either absolute or relative to the repository xml.
     * Always return null for a local installed folder.
     * @see #getLocalOsPath()
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Returns the local OS folder where a local archive is installed.
     * Always return null for remote archives.
     * @see #getUrl()
     */
    public String getLocalOsPath() {
        return mLocalOsPath;
    }

    /**
     * Returns the archive {@link Os} enum.
     * Can be null for a local installed folder on an unknown OS.
     */
    public Os getOs() {
        return mOs;
    }

    /**
     * Returns the archive {@link Arch} enum.
     * Can be null for a local installed folder on an unknown architecture.
     */
    public Arch getArch() {
        return mArch;
    }

    /**
     * Generates a description for this archive of the OS/Arch supported by this archive.
     */
    public String getOsDescription() {
        String os;
        if (mOs == null) {
            os = "unknown OS";
        } else if (mOs == Os.ANY) {
            os = "any OS";
        } else {
            os = mOs.getUiName();
        }

        String arch = "";                               //$NON-NLS-1$
        if (mArch != null && mArch != Arch.ANY) {
            arch = mArch.getUiName();
        }

        return String.format("%1$s%2$s%3$s",
                os,
                arch.length() > 0 ? " " : "",           //$NON-NLS-2$
                arch);
    }

    /**
     * Generates a short description for this archive.
     */
    public String getShortDescription() {
        return String.format("Archive for %1$s", getOsDescription());
    }

    /**
     * Generates a longer description for this archive.
     */
    public String getLongDescription() {
        return String.format("%1$s\nSize: %2$d MiB\nSHA1: %3$s",
                getShortDescription(),
                Math.round(getSize() / (1024*1024)),
                getChecksum());
    }

    /**
     * Returns true if this archive can be installed on the current platform.
     */
    public boolean isCompatible() {
        // Check OS
        Os os = getOs();

        if (os != Os.ANY) {
            Os os2 = Os.getCurrentOs();
            if (os2 != os) {
                return false;
            }
        }

        // Check Arch
        Arch arch = getArch();

        if (arch != Arch.ANY) {
            Arch arch2 = Arch.getCurrentArch();
            if (arch2 != arch) {
                return false;
            }
        }

        return true;
    }

    /**
     * Delete the archive folder if this is a local archive.
     */
    public void deleteLocal() {
        if (isLocal()) {
            deleteFileOrFolder(new File(getLocalOsPath()));
        }
    }

    /**
     * Install this {@link Archive}s.
     * The archive will be skipped if it is incompatible.
     *
     * @return True if the archive was installed, false otherwise.
     */
    public boolean install(String osSdkRoot, boolean forceHttp, ITaskMonitor monitor) {

        File archiveFile = null;
        try {
            String name = getParentPackage().getShortDescription();

            if (isLocal()) {
                // This should never happen.
                monitor.setResult("Skipping already installed archive: %1$s for %2$s",
                        name,
                        getOsDescription());
                return false;
            }

            if (!isCompatible()) {
                monitor.setResult("Skipping incompatible archive: %1$s for %2$s",
                        name,
                        getOsDescription());
                return false;
            }

            archiveFile = downloadFile(monitor, forceHttp);
            if (archiveFile != null) {
                if (unarchive(osSdkRoot, archiveFile, monitor)) {
                    monitor.setResult("Installed: %1$s", name);
                    return true;
                }
            }

        } finally {
            // Delete the temp archive if it exists
            deleteFileOrFolder(archiveFile);
        }

        return false;
    }

    /**
     * Downloads an archive and returns the temp file with it.
     * Caller is responsible with deleting the temp file when done.
     */
    private File downloadFile(ITaskMonitor monitor, boolean forceHttp) {

        File tmpFileToDelete = null;
        try {
            File tmpFile = File.createTempFile("sdkupload", ".bin"); //$NON-NLS-1$ //$NON-NLS-2$
            tmpFileToDelete = tmpFile;

            String name = getParentPackage().getShortDescription();
            String desc = String.format("Downloading %1$s", name);
            monitor.setDescription(desc);

            String link = getUrl();
            if (!link.startsWith("http://")                          //$NON-NLS-1$
                    && !link.startsWith("https://")                  //$NON-NLS-1$
                    && !link.startsWith("ftp://")) {                 //$NON-NLS-1$
                // Make the URL absolute by prepending the source
                Package pkg = getParentPackage();
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

            if (forceHttp) {
                link = link.replaceAll("https://", "http://");  //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (fetchUrl(tmpFile, link, desc, monitor)) {
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

            MessageDigest digester = getChecksumType().getMessageDigest();

            byte[] buf = new byte[65536];
            int n;

            long total = 0;
            long size = getSize();
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

            String expected = getChecksum();
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
    private boolean unarchive(String osSdkRoot, File archiveFile, ITaskMonitor monitor) {
        String name = getParentPackage().getShortDescription();
        String desc = String.format("Installing %1$s", name);
        monitor.setDescription(desc);

        File destFolder = getParentPackage().getInstallFolder(osSdkRoot);

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
            }

            if (!unzipDestFolder.mkdirs()) {
                monitor.setResult("Failed to create directory %1$s",
                        unzipDestFolder.getPath());
                return false;
            }

            if (!unzipFolder(archiveFile, getSize(), unzipDestFolder, desc, monitor)) {
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

                // Zip entries are always packages in a top-level directory
                // (e.g. docs/index.html). However we want to use our top-level
                // directory so we drop the first segment of the path name.
                int pos = name.indexOf('/');
                if (pos < 0 || pos == name.length() - 1) {
                    continue;
                } else {
                    name = name.substring(pos + 1);
                }

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
}

