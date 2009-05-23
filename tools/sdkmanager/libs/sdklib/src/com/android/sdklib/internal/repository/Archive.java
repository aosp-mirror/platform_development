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
        SHA1
    }

    /** The OS that this archive can be downloaded on. */
    public enum Os {
        ANY,
        LINUX,
        MACOSX,
        WINDOWS
    }

    /** The Architecture that this archvie can be downloaded on. */
    public enum Arch {
        ANY,
        PPC,
        X86,
        X86_64
    }

    private final Os     mOs;
    private final Arch   mArch;
    private final String mUrl;
    private final long   mSize;
    private final String mChecksum;
    private final ChecksumType mChecksumType = ChecksumType.SHA1;

    /**
     * Creates a new archive.
     */
    Archive(Os os, Arch arch, String url, long size, String checksum) {
        mOs = os;
        mArch = arch;
        mUrl = url;
        mSize = size;
        mChecksum = checksum;
    }

    /** Returns the archive size, an int > 0. */
    public long getSize() {
        return mSize;
    }

    /** Returns the SHA1 archive checksum, as a 40-char hex. */
    public String getChecksum() {
        return mChecksum;
    }

    /** Returns the checksum type, always {@link ChecksumType#SHA1} right now. */
    public ChecksumType getChecksumType() {
        return mChecksumType;
    }

    /** Returns the optional description URL for all packages (platform, add-on, tool, doc).
     * Can be empty but not null. */
    public String getDescUrl() {
        return mUrl;
    }

    /** Returns the archive {@link Os} enum. */
    public Os getOs() {
        return mOs;
    }

    /** Returns the archive {@link Arch} enum. */
    public Arch getArch() {
        return mArch;
    }

    public String getShortDescription() {
        String os = "any OS";
        if (mOs != Os.ANY) {
            os = capitalize(mOs.toString());
        }

        String arch = "";
        if (mArch != Arch.ANY) {
            arch = mArch.toString().toLowerCase();
        }

        return String.format("Archive for %1$s %2$s", os, arch);
    }

    private String capitalize(String string) {
        if (string.length() > 1) {
            return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
        } else {
            return string.toUpperCase();
        }
    }

    public String getLongDescription() {
        return String.format("%1$s\nSize: %2$d MiB\nSHA1: %3$s",
                getShortDescription(),
                Math.round(getSize() / (1024*1024)),
                getChecksum());
    }
}
