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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores native allocation information.
 * <p/>Contains number of allocations, their size and the stack trace.
 * <p/>Note: the ddmlib does not resolve the stack trace automatically. While this class provides
 * storage for resolved stack trace, this is merely for convenience.
 */
public final class NativeAllocationInfo {
    /* constants for flag bits */
    private static final int FLAG_ZYGOTE_CHILD  = (1<<31);
    private static final int FLAG_MASK          = (FLAG_ZYGOTE_CHILD);

    /**
     * list of alloc functions that are filtered out when attempting to display
     * a relevant method responsible for an allocation
     */
    private static ArrayList<String> sAllocFunctionFilter;
    static {
        sAllocFunctionFilter = new ArrayList<String>();
        sAllocFunctionFilter.add("malloc"); //$NON-NLS-1$
        sAllocFunctionFilter.add("calloc"); //$NON-NLS-1$
        sAllocFunctionFilter.add("realloc"); //$NON-NLS-1$
        sAllocFunctionFilter.add("get_backtrace"); //$NON-NLS-1$
        sAllocFunctionFilter.add("get_hash"); //$NON-NLS-1$
        sAllocFunctionFilter.add("??"); //$NON-NLS-1$
        sAllocFunctionFilter.add("internal_free"); //$NON-NLS-1$
        sAllocFunctionFilter.add("operator new"); //$NON-NLS-1$
        sAllocFunctionFilter.add("leak_free"); //$NON-NLS-1$
        sAllocFunctionFilter.add("chk_free"); //$NON-NLS-1$
        sAllocFunctionFilter.add("chk_memalign"); //$NON-NLS-1$
        sAllocFunctionFilter.add("Malloc"); //$NON-NLS-1$
    }

    private final int mSize;

    private final boolean mIsZygoteChild;

    private final int mAllocations;

    private final ArrayList<Long> mStackCallAddresses = new ArrayList<Long>();

    private ArrayList<NativeStackCallInfo> mResolvedStackCall = null;

    private boolean mIsStackCallResolved = false;

    /**
     * Constructs a new {@link NativeAllocationInfo}.
     * @param size The size of the allocations.
     * @param allocations the allocation count
     */
    NativeAllocationInfo(int size, int allocations) {
        this.mSize = size & ~FLAG_MASK;
        this.mIsZygoteChild = ((size & FLAG_ZYGOTE_CHILD) != 0);
        this.mAllocations = allocations;
    }
    
    /**
     * Adds a stack call address for this allocation.
     * @param address The address to add.
     */
    void addStackCallAddress(long address) {
        mStackCallAddresses.add(address);
    }
    
    /**
     * Returns the total size of this allocation.
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Returns whether the allocation happened in a child of the zygote
     * process.
     */
    public boolean isZygoteChild() {
        return mIsZygoteChild;
    }

    /**
     * Returns the allocation count.
     */
    public int getAllocationCount() {
        return mAllocations;
    }
    
    /**
     * Returns whether the stack call addresses have been resolved into
     * {@link NativeStackCallInfo} objects.
     */
    public boolean isStackCallResolved() {
        return mIsStackCallResolved;
    }

    /**
     * Returns the stack call of this allocation as raw addresses.
     * @return the list of addresses where the allocation happened.
     */
    public Long[] getStackCallAddresses() {
        return mStackCallAddresses.toArray(new Long[mStackCallAddresses.size()]);
    }
    
    /**
     * Sets the resolved stack call for this allocation.
     * <p/>
     * If <code>resolvedStackCall</code> is non <code>null</code> then
     * {@link #isStackCallResolved()} will return <code>true</code> after this call.
     * @param resolvedStackCall The list of {@link NativeStackCallInfo}.
     */
    public synchronized void setResolvedStackCall(List<NativeStackCallInfo> resolvedStackCall) {
        if (mResolvedStackCall == null) {
            mResolvedStackCall = new ArrayList<NativeStackCallInfo>();
        } else {
            mResolvedStackCall.clear();
        }
        mResolvedStackCall.addAll(resolvedStackCall);
        mIsStackCallResolved = mResolvedStackCall.size() != 0;
    }

    /**
     * Returns the resolved stack call.
     * @return An array of {@link NativeStackCallInfo} or <code>null</code> if the stack call
     * was not resolved.
     * @see #setResolvedStackCall(ArrayList)
     * @see #isStackCallResolved()
     */
    public synchronized NativeStackCallInfo[] getResolvedStackCall() {
        if (mIsStackCallResolved) {
            return mResolvedStackCall.toArray(new NativeStackCallInfo[mResolvedStackCall.size()]);
        }
        
        return null;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is equal to the obj argument;
     * <code>false</code> otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof NativeAllocationInfo) {
            NativeAllocationInfo mi = (NativeAllocationInfo)obj;
            // quick compare of size, alloc, and stackcall size
            if (mSize != mi.mSize || mAllocations != mi.mAllocations ||
                    mStackCallAddresses.size() != mi.mStackCallAddresses.size()) {
                return false;
            }
            // compare the stack addresses
            int count = mStackCallAddresses.size();
            for (int i = 0 ; i < count ; i++) {
                long a = mStackCallAddresses.get(i);
                long b = mi.mStackCallAddresses.get(i);
                if (a != b) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Returns a string representation of the object.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Allocations: ");
        buffer.append(mAllocations);
        buffer.append("\n"); //$NON-NLS-1$

        buffer.append("Size: ");
        buffer.append(mSize);
        buffer.append("\n"); //$NON-NLS-1$

        buffer.append("Total Size: ");
        buffer.append(mSize * mAllocations);
        buffer.append("\n"); //$NON-NLS-1$

        Iterator<Long> addrIterator = mStackCallAddresses.iterator();
        Iterator<NativeStackCallInfo> sourceIterator = mResolvedStackCall.iterator();

        while (sourceIterator.hasNext()) {
            long addr = addrIterator.next();
            NativeStackCallInfo source = sourceIterator.next();
            if (addr == 0)
                continue;

            if (source.getLineNumber() != -1) {
                buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s:%5$d\n", addr,
                        source.getLibraryName(), source.getMethodName(),
                        source.getSourceFile(), source.getLineNumber()));
            } else {
                buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s\n", addr,
                        source.getLibraryName(), source.getMethodName(), source.getSourceFile()));
            }
        }

        return buffer.toString();
    }

    /**
     * Returns the first {@link NativeStackCallInfo} that is relevant.
     * <p/>
     * A relevant <code>NativeStackCallInfo</code> is a stack call that is not deep in the
     * lower level of the libc, but the actual method that performed the allocation. 
     * @return a <code>NativeStackCallInfo</code> or <code>null</code> if the stack call has not
     * been processed from the raw addresses.
     * @see #setResolvedStackCall(ArrayList)
     * @see #isStackCallResolved()
     */
    public synchronized NativeStackCallInfo getRelevantStackCallInfo() {
        if (mIsStackCallResolved && mResolvedStackCall != null) {
            Iterator<NativeStackCallInfo> sourceIterator = mResolvedStackCall.iterator();
            Iterator<Long> addrIterator = mStackCallAddresses.iterator();

            while (sourceIterator.hasNext() && addrIterator.hasNext()) {
                long addr = addrIterator.next();
                NativeStackCallInfo info = sourceIterator.next();
                if (addr != 0 && info != null) {
                    if (isRelevant(info.getMethodName())) {
                        return info;
                    }
                }
            }

            // couldnt find a relevant one, so we'll return the first one if it
            // exists.
            if (mResolvedStackCall.size() > 0)
                return mResolvedStackCall.get(0);
        }

        return null;
    }
    
    /**
     * Returns true if the method name is relevant.
     * @param methodName the method name to test.
     */
    private boolean isRelevant(String methodName) {
        for (String filter : sAllocFunctionFilter) {
            if (methodName.contains(filter)) {
                return false;
            }
        }
        
        return true;
    }
}
