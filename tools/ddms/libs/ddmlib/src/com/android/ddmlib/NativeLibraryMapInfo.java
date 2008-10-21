/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmlib;

/**
 * Memory address to library mapping for native libraries.
 * <p/>
 * Each instance represents a single native library and its start and end memory addresses. 
 */
public final class NativeLibraryMapInfo {
    private long mStartAddr;
    private long mEndAddr;

    private String mLibrary;

    /**
     * Constructs a new native library map info.
     * @param startAddr The start address of the library.
     * @param endAddr The end address of the library.
     * @param library The name of the library.
     */
    NativeLibraryMapInfo(long startAddr, long endAddr, String library) {
        this.mStartAddr = startAddr;
        this.mEndAddr = endAddr;
        this.mLibrary = library;
    }
    
    /**
     * Returns the name of the library.
     */
    public String getLibraryName() {
        return mLibrary;
    }
    
    /**
     * Returns the start address of the library.
     */
    public long getStartAddress() {
        return mStartAddr;
    }
    
    /**
     * Returns the end address of the library.
     */
    public long getEndAddress() {
        return mEndAddr;
    }

    /**
     * Returns whether the specified address is inside the library.
     * @param address The address to test.
     * @return <code>true</code> if the address is between the start and end address of the library.
     * @see #getStartAddress()
     * @see #getEndAddress()
     */
    public boolean isWithinLibrary(long address) {
        return address >= mStartAddr && address <= mEndAddr;
    }
}
