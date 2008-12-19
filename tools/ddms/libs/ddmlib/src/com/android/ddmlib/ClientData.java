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

import com.android.ddmlib.HeapSegment.HeapSegmentElement;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Contains the data of a {@link Client}.
 */
public class ClientData {
    /* This is a place to stash data associated with a Client, such as thread
    * states or heap data.  ClientData maps 1:1 to Client, but it's a little
    * cleaner if we separate the data out.
    *
    * Message handlers are welcome to stash arbitrary data here.
    *
    * IMPORTANT: The data here is written by HandleFoo methods and read by
    * FooPanel methods, which run in different threads.  All non-trivial
    * access should be synchronized against the ClientData object.
    */

    
    /** Temporary name of VM to be ignored. */
    private final static String PRE_INITIALIZED = "<pre-initialized>"; //$NON-NLS-1$

    /** Debugger connection status: not waiting on one, not connected to one, but accepting
     * new connections. This is the default value. */
    public static final int DEBUGGER_DEFAULT = 1;
    /**
     * Debugger connection status: the application's VM is paused, waiting for a debugger to
     * connect to it before resuming. */
    public static final int DEBUGGER_WAITING = 2;
    /** Debugger connection status : Debugger is connected */
    public static final int DEBUGGER_ATTACHED = 3;
    /** Debugger connection status: The listening port for debugger connection failed to listen.
     * No debugger will be able to connect. */
    public static final int DEBUGGER_ERROR = 4;
    
    /**
     * Allocation tracking status: unknown.
     * <p/>This happens right after a {@link Client} is discovered
     * by the {@link AndroidDebugBridge}, and before the {@link Client} answered the query regarding
     * its allocation tracking status.
     * @see Client#requestAllocationStatus()
     */
    public static final int ALLOCATION_TRACKING_UNKNOWN = -1;
    /**
     * Allocation tracking status: the {@link Client} is not tracking allocations. */
    public static final int ALLOCATION_TRACKING_OFF = 0;
    /**
     * Allocation tracking status: the {@link Client} is tracking allocations. */
    public static final int ALLOCATION_TRACKING_ON = 1;

    /**
     * Name of the value representing the max size of the heap, in the {@link Map} returned by
     * {@link #getVmHeapInfo(int)}
     */
    public final static String HEAP_MAX_SIZE_BYTES = "maxSizeInBytes"; // $NON-NLS-1$
    /**
     * Name of the value representing the size of the heap, in the {@link Map} returned by
     * {@link #getVmHeapInfo(int)}
     */
    public final static String HEAP_SIZE_BYTES = "sizeInBytes"; // $NON-NLS-1$
    /**
     * Name of the value representing the number of allocated bytes of the heap, in the
     * {@link Map} returned by {@link #getVmHeapInfo(int)}
     */
    public final static String HEAP_BYTES_ALLOCATED = "bytesAllocated"; // $NON-NLS-1$
    /**
     * Name of the value representing the number of objects in the heap, in the {@link Map}
     * returned by {@link #getVmHeapInfo(int)}
     */
    public final static String HEAP_OBJECTS_ALLOCATED = "objectsAllocated"; // $NON-NLS-1$

    // is this a DDM-aware client?
    private boolean mIsDdmAware;

    // the client's process ID
    private final int mPid;

    // Java VM identification string
    private String mVmIdentifier;

    // client's self-description
    private String mClientDescription;

    // how interested are we in a debugger?
    private int mDebuggerInterest;

    // Thread tracking (THCR, THDE).
    private TreeMap<Integer,ThreadInfo> mThreadMap;

    /** VM Heap data */
    private final HeapData mHeapData = new HeapData();
    /** Native Heap data */
    private final HeapData mNativeHeapData = new HeapData();

    private HashMap<Integer, HashMap<String, Long>> mHeapInfoMap =
            new HashMap<Integer, HashMap<String, Long>>();


    /** library map info. Stored here since the backtrace data
     * is computed on a need to display basis.
     */
    private ArrayList<NativeLibraryMapInfo> mNativeLibMapInfo =
        new ArrayList<NativeLibraryMapInfo>();

    /** Native Alloc info list */
    private ArrayList<NativeAllocationInfo> mNativeAllocationList =
        new ArrayList<NativeAllocationInfo>();
    private int mNativeTotalMemory;

    private AllocationInfo[] mAllocations;
    private int mAllocationStatus = ALLOCATION_TRACKING_UNKNOWN;

    /**
     * Heap Information.
     * <p/>The heap is composed of several {@link HeapSegment} objects.
     * <p/>A call to {@link #isHeapDataComplete()} will indicate if the segments (available through
     * {@link #getHeapSegments()}) represent the full heap.
     */
    public static class HeapData {
        private TreeSet<HeapSegment> mHeapSegments = new TreeSet<HeapSegment>();
        private boolean mHeapDataComplete = false;
        private byte[] mProcessedHeapData;
        private Map<Integer, ArrayList<HeapSegmentElement>> mProcessedHeapMap;

        /**
         * Abandon the current list of heap segments.
         */
        public synchronized void clearHeapData() {
            /* Abandon the old segments instead of just calling .clear().
             * This lets the user hold onto the old set if it wants to.
             */
            mHeapSegments = new TreeSet<HeapSegment>();
            mHeapDataComplete = false;
        }

        /**
         * Add raw HPSG chunk data to the list of heap segments.
         *
         * @param data The raw data from an HPSG chunk.
         */
        synchronized void addHeapData(ByteBuffer data) {
            HeapSegment hs;

            if (mHeapDataComplete) {
                clearHeapData();
            }

            try {
                hs = new HeapSegment(data);
            } catch (BufferUnderflowException e) {
                System.err.println("Discarding short HPSG data (length " + data.limit() + ")");
                return;
            }

            mHeapSegments.add(hs);
        }

        /**
         * Called when all heap data has arrived.
         */
        synchronized void sealHeapData() {
            mHeapDataComplete = true;
        }

        /**
         * Returns whether the heap data has been sealed.
         */
        public boolean isHeapDataComplete() {
            return mHeapDataComplete;
        }

        /**
         * Get the collected heap data, if sealed.
         *
         * @return The list of heap segments if the heap data has been sealed, or null if it hasn't.
         */
        public Collection<HeapSegment> getHeapSegments() {
            if (isHeapDataComplete()) {
                return mHeapSegments;
            }
            return null;
        }

        /**
         * Sets the processed heap data.
         *
         * @param heapData The new heap data (can be null)
         */
        public void setProcessedHeapData(byte[] heapData) {
            mProcessedHeapData = heapData;
        }

        /**
         * Get the processed heap data, if present.
         *
         * @return the processed heap data, or null.
         */
        public byte[] getProcessedHeapData() {
            return mProcessedHeapData;
        }

        public void setProcessedHeapMap(Map<Integer, ArrayList<HeapSegmentElement>> heapMap) {
            mProcessedHeapMap = heapMap;
        }
        
        public Map<Integer, ArrayList<HeapSegmentElement>> getProcessedHeapMap() {
            return mProcessedHeapMap;
        }
        

    }


    /**
     * Generic constructor.
     */
    ClientData(int pid) {
        mPid = pid;

        mDebuggerInterest = DEBUGGER_DEFAULT;
        mThreadMap = new TreeMap<Integer,ThreadInfo>();
    }
    
    /**
     * Returns whether the process is DDM-aware.
     */
    public boolean isDdmAware() {
        return mIsDdmAware;
    }

    /**
     * Sets DDM-aware status.
     */
    void isDdmAware(boolean aware) {
        mIsDdmAware = aware;
    }

    /**
     * Returns the process ID.
     */
    public int getPid() {
        return mPid;
    }

    /**
     * Returns the Client's VM identifier.
     */
    public String getVmIdentifier() {
        return mVmIdentifier;
    }

    /**
     * Sets VM identifier.
     */
    void setVmIdentifier(String ident) {
        mVmIdentifier = ident;
    }

    /**
     * Returns the client description.
     * <p/>This is generally the name of the package defined in the
     * <code>AndroidManifest.xml</code>.
     * 
     * @return the client description or <code>null</code> if not the description was not yet
     * sent by the client.
     */
    public String getClientDescription() {
        return mClientDescription;
    }

    /**
     * Sets client description.
     *
     * There may be a race between HELO and APNM.  Rather than try
     * to enforce ordering on the device, we just don't allow an empty
     * name to replace a specified one.
     */
    void setClientDescription(String description) {
        if (mClientDescription == null && description.length() > 0) {
            /*
             * The application VM is first named <pre-initialized> before being assigned
             * its real name.
             * Depending on the timing, we can get an APNM chunk setting this name before
             * another one setting the final actual name. So if we get a SetClientDescription
             * with this value we ignore it.
             */
            if (PRE_INITIALIZED.equals(description) == false) {
                mClientDescription = description;
            }
        }
    }
    
    /**
     * Returns the debugger connection status. Possible values are {@link #DEBUGGER_DEFAULT},
     * {@link #DEBUGGER_WAITING}, {@link #DEBUGGER_ATTACHED}, and {@link #DEBUGGER_ERROR}.
     */
    public int getDebuggerConnectionStatus() {
        return mDebuggerInterest;
    }

    /**
     * Sets debugger connection status.
     */
    void setDebuggerConnectionStatus(int val) {
        mDebuggerInterest = val;
    }

    /**
     * Sets the current heap info values for the specified heap.
     *
     * @param heapId The heap whose info to update
     * @param sizeInBytes The size of the heap, in bytes
     * @param bytesAllocated The number of bytes currently allocated in the heap
     * @param objectsAllocated The number of objects currently allocated in
     *                         the heap
     */
    // TODO: keep track of timestamp, reason
    synchronized void setHeapInfo(int heapId, long maxSizeInBytes,
            long sizeInBytes, long bytesAllocated, long objectsAllocated) {
        HashMap<String, Long> heapInfo = new HashMap<String, Long>();
        heapInfo.put(HEAP_MAX_SIZE_BYTES, maxSizeInBytes);
        heapInfo.put(HEAP_SIZE_BYTES, sizeInBytes);
        heapInfo.put(HEAP_BYTES_ALLOCATED, bytesAllocated);
        heapInfo.put(HEAP_OBJECTS_ALLOCATED, objectsAllocated);
        mHeapInfoMap.put(heapId, heapInfo);
    }

    /**
     * Returns the {@link HeapData} object for the VM.
     */
    public HeapData getVmHeapData() {
        return mHeapData;
    }

    /**
     * Returns the {@link HeapData} object for the native code.
     */
    HeapData getNativeHeapData() {
        return mNativeHeapData;
    }

    /**
     * Returns an iterator over the list of known VM heap ids.
     * <p/>
     * The caller must synchronize on the {@link ClientData} object while iterating.
     *
     * @return an iterator over the list of heap ids
     */
    public synchronized Iterator<Integer> getVmHeapIds() {
        return mHeapInfoMap.keySet().iterator();
    }

    /**
     * Returns the most-recent info values for the specified VM heap.
     *
     * @param heapId The heap whose info should be returned
     * @return a map containing the info values for the specified heap.
     *         Returns <code>null</code> if the heap ID is unknown.
     */
    public synchronized Map<String, Long> getVmHeapInfo(int heapId) {
        return mHeapInfoMap.get(heapId);
    }

    /**
     * Adds a new thread to the list.
     */
    synchronized void addThread(int threadId, String threadName) {
        ThreadInfo attr = new ThreadInfo(threadId, threadName);
        mThreadMap.put(threadId, attr);
    }

    /**
     * Removes a thread from the list.
     */
    synchronized void removeThread(int threadId) {
        mThreadMap.remove(threadId);
    }

    /**
     * Returns the list of threads as {@link ThreadInfo} objects.
     * <p/>The list is empty until a thread update was requested with
     * {@link Client#requestThreadUpdate()}.
     */
    public synchronized ThreadInfo[] getThreads() {
        Collection<ThreadInfo> threads = mThreadMap.values();
        return threads.toArray(new ThreadInfo[threads.size()]);
    }

    /**
     * Returns the {@link ThreadInfo} by thread id.
     */
    synchronized ThreadInfo getThread(int threadId) {
        return mThreadMap.get(threadId);
    }
    
    synchronized void clearThreads() {
        mThreadMap.clear();
    }

    /**
     * Returns the list of {@link NativeAllocationInfo}.
     * @see Client#requestNativeHeapInformation()
     */
    public synchronized List<NativeAllocationInfo> getNativeAllocationList() {
        return Collections.unmodifiableList(mNativeAllocationList);
    }

    /**
     * adds a new {@link NativeAllocationInfo} to the {@link Client}
     * @param allocInfo The {@link NativeAllocationInfo} to add.
     */
    synchronized void addNativeAllocation(NativeAllocationInfo allocInfo) {
        mNativeAllocationList.add(allocInfo);
    }

    /**
     * Clear the current malloc info.
     */
    synchronized void clearNativeAllocationInfo() {
        mNativeAllocationList.clear();
    }

    /**
     * Returns the total native memory.
     * @see Client#requestNativeHeapInformation()
     */
    public synchronized int getTotalNativeMemory() {
        return mNativeTotalMemory;
    }

    synchronized void setTotalNativeMemory(int totalMemory) {
        mNativeTotalMemory = totalMemory;
    }

    synchronized void addNativeLibraryMapInfo(long startAddr, long endAddr, String library) {
        mNativeLibMapInfo.add(new NativeLibraryMapInfo(startAddr, endAddr, library));
    }

    /**
     * Returns an {@link Iterator} on {@link NativeLibraryMapInfo} objects.
     * <p/>
     * The caller must synchronize on the {@link ClientData} object while iterating.
     */
    public synchronized Iterator<NativeLibraryMapInfo> getNativeLibraryMapInfo() {
        return mNativeLibMapInfo.iterator();
    }
    
    synchronized void setAllocationStatus(boolean enabled) {
        mAllocationStatus = enabled ? ALLOCATION_TRACKING_ON : ALLOCATION_TRACKING_OFF;
    }

    /**
     * Returns the allocation tracking status.
     * @see Client#requestAllocationStatus()
     */
    public synchronized int getAllocationStatus() {
        return mAllocationStatus;
    }
    
    synchronized void setAllocations(AllocationInfo[] allocs) {
        mAllocations = allocs;
    }
    
    /**
     * Returns the list of tracked allocations.
     * @see Client#requestAllocationDetails()
     */
    public synchronized AllocationInfo[] getAllocations() {
        return mAllocations;
    }
}

