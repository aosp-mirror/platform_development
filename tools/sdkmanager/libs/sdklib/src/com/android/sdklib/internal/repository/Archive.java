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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


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

        @Override
        public String toString() {
            return mUiName;
        }

        /**
         * Returns the current OS as one of the {@link Os} enum values or null.
         */
        public static Os getCurrentOs() {
            String os = System.getProperty("os.name");          //$NON-NLS-1$
            if (os.startsWith("Mac OS")) {                      //$NON-NLS-1$
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

        @Override
        public String toString() {
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

    /**
     * Creates a new archive.
     */
    Archive(Package pkg, Os os, Arch arch, String url, long size, String checksum) {
        mPackage = pkg;
        mOs = os;
        mArch = arch;
        mUrl = url;
        mSize = size;
        mChecksum = checksum;
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
     * For a local installed folder, an URL is frabricated from the folder path.
     */
    public String getUrl() {
        return mUrl;
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
     * Generates a short description for this archive.
     */
    public String getShortDescription() {
        String os;
        if (mOs == null) {
            os = "unknown OS";
        } else if (mOs == Os.ANY) {
            os = "any OS";
        } else {
            os = mOs.toString();
        }

        String arch = "";
        if (mArch != null && mArch != Arch.ANY) {
            arch = mArch.toString();
        }

        return String.format("Archive for %1$s %2$s", os, arch);
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
}

