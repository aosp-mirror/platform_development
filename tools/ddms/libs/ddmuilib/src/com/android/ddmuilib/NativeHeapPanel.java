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

package com.android.ddmuilib;

import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.Log;
import com.android.ddmlib.NativeAllocationInfo;
import com.android.ddmlib.NativeLibraryMapInfo;
import com.android.ddmlib.NativeStackCallInfo;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.HeapSegment.HeapSegmentElement;
import com.android.ddmuilib.annotation.WorkerThread;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Panel with native heap information.
 */
public final class NativeHeapPanel extends BaseHeapPanel {

    /** color palette and map legend. NATIVE is the last enum is a 0 based enum list, so we need
     * Native+1 at least. We also need 2 more entries for free area and expansion area.  */
    private static final int NUM_PALETTE_ENTRIES = HeapSegmentElement.KIND_NATIVE+2 +1;
    private static final String[] mMapLegend = new String[NUM_PALETTE_ENTRIES];
    private static final PaletteData mMapPalette = createPalette();
    
    private static final int ALLOC_DISPLAY_ALL = 0;
    private static final int ALLOC_DISPLAY_PRE_ZYGOTE = 1;
    private static final int ALLOC_DISPLAY_POST_ZYGOTE = 2;

    private Display mDisplay;

    private Composite mBase;

    private Label mUpdateStatus;

    /** combo giving choice of what to display: all, pre-zygote, post-zygote */
    private Combo mAllocDisplayCombo;

    private Button mFullUpdateButton;

    // see CreateControl()
    //private Button mDiffUpdateButton;

    private Combo mDisplayModeCombo;

    /** stack composite for mode (1-2) & 3 */
    private Composite mTopStackComposite;

    private StackLayout mTopStackLayout;

    /** stack composite for mode 1 & 2 */
    private Composite mAllocationStackComposite;

    private StackLayout mAllocationStackLayout;

    /** top level container for mode 1 & 2 */
    private Composite mTableModeControl;

    /** top level object for the allocation mode */
    private Control mAllocationModeTop;

    /** top level for the library mode */
    private Control mLibraryModeTopControl;

    /** composite for page UI and total memory display */
    private Composite mPageUIComposite;

    private Label mTotalMemoryLabel;

    private Label mPageLabel;

    private Button mPageNextButton;

    private Button mPagePreviousButton;

    private Table mAllocationTable;

    private Table mLibraryTable;

    private Table mLibraryAllocationTable;

    private Table mDetailTable;

    private Label mImage;
    
    private int mAllocDisplayMode = ALLOC_DISPLAY_ALL;

    /**
     * pointer to current stackcall thread computation in order to quit it if
     * required (new update requested)
     */
    private StackCallThread mStackCallThread;

    /** Current Library Allocation table fill thread. killed if selection changes */
    private FillTableThread mFillTableThread;

    /**
     * current client data. Used to access the malloc info when switching pages
     * or selecting allocation to show stack call
     */
    private ClientData mClientData;

    /**
     * client data from a previous display. used when asking for an "update & diff"
     */
    private ClientData mBackUpClientData;

    /** list of NativeAllocationInfo objects filled with the list from ClientData */
    private final ArrayList<NativeAllocationInfo> mAllocations =
        new ArrayList<NativeAllocationInfo>();
    
    /** list of the {@link NativeAllocationInfo} being displayed based on the selection
     * of {@link #mAllocDisplayCombo}.
     */
    private final ArrayList<NativeAllocationInfo> mDisplayedAllocations =
        new ArrayList<NativeAllocationInfo>();

    /** list of NativeAllocationInfo object kept as backup when doing an "update & diff" */
    private final ArrayList<NativeAllocationInfo> mBackUpAllocations =
        new ArrayList<NativeAllocationInfo>();

    /** back up of the total memory, used when doing an "update & diff" */
    private int mBackUpTotalMemory;

    private int mCurrentPage = 0;

    private int mPageCount = 0;

    /**
     * list of allocation per Library. This is created from the list of
     * NativeAllocationInfo objects that is stored in the ClientData object. Since we
     * don't keep this list around, it is recomputed everytime the client
     * changes.
     */
    private final ArrayList<LibraryAllocations> mLibraryAllocations =
        new ArrayList<LibraryAllocations>();

    /* args to setUpdateStatus() */
    private static final int NOT_SELECTED = 0;

    private static final int NOT_ENABLED = 1;

    private static final int ENABLED = 2;

    private static final int DISPLAY_PER_PAGE = 20;

    private static final String PREFS_ALLOCATION_SASH = "NHallocSash"; //$NON-NLS-1$
    private static final String PREFS_LIBRARY_SASH = "NHlibrarySash"; //$NON-NLS-1$
    private static final String PREFS_DETAIL_ADDRESS = "NHdetailAddress"; //$NON-NLS-1$
    private static final String PREFS_DETAIL_LIBRARY = "NHdetailLibrary"; //$NON-NLS-1$
    private static final String PREFS_DETAIL_METHOD = "NHdetailMethod"; //$NON-NLS-1$
    private static final String PREFS_DETAIL_FILE = "NHdetailFile"; //$NON-NLS-1$
    private static final String PREFS_DETAIL_LINE = "NHdetailLine"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_TOTAL = "NHallocTotal"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_COUNT = "NHallocCount"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_SIZE = "NHallocSize"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_LIBRARY = "NHallocLib"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_METHOD = "NHallocMethod"; //$NON-NLS-1$
    private static final String PREFS_ALLOC_FILE = "NHallocFile"; //$NON-NLS-1$
    private static final String PREFS_LIB_LIBRARY = "NHlibLibrary"; //$NON-NLS-1$
    private static final String PREFS_LIB_SIZE = "NHlibSize"; //$NON-NLS-1$
    private static final String PREFS_LIB_COUNT = "NHlibCount"; //$NON-NLS-1$
    private static final String PREFS_LIBALLOC_TOTAL = "NHlibAllocTotal"; //$NON-NLS-1$
    private static final String PREFS_LIBALLOC_COUNT = "NHlibAllocCount"; //$NON-NLS-1$
    private static final String PREFS_LIBALLOC_SIZE = "NHlibAllocSize"; //$NON-NLS-1$
    private static final String PREFS_LIBALLOC_METHOD = "NHlibAllocMethod"; //$NON-NLS-1$

    /** static formatter object to format all numbers as #,### */
    private static DecimalFormat sFormatter;
    static {
        sFormatter = (DecimalFormat)NumberFormat.getInstance();
        if (sFormatter != null)
            sFormatter = new DecimalFormat("#,###");
        else
            sFormatter.applyPattern("#,###");
    }


    /**
     * caching mechanism to avoid recomputing the backtrace for a particular
     * address several times.
     */
    private HashMap<Long, NativeStackCallInfo> mSourceCache =
        new HashMap<Long,NativeStackCallInfo>();
    private long mTotalSize;
    private Button mSaveButton;
    private Button mSymbolsButton;

    /**
     * thread class to convert the address call into method, file and line
     * number in the background.
     */
    private class StackCallThread extends BackgroundThread {
        private ClientData mClientData;

        public StackCallThread(ClientData cd) {
            mClientData = cd;
        }

        public ClientData getClientData() {
            return mClientData;
        }

        @Override
        public void run() {
            // loop through all the NativeAllocationInfo and init them
            Iterator<NativeAllocationInfo> iter = mAllocations.iterator();
            int total = mAllocations.size();
            int count = 0;
            while (iter.hasNext()) {

                if (isQuitting())
                    return;

                NativeAllocationInfo info = iter.next();
                if (info.isStackCallResolved() == false) {
                    final Long[] list = info.getStackCallAddresses();
                    final int size = list.length;
                    
                    ArrayList<NativeStackCallInfo> resolvedStackCall =
                        new ArrayList<NativeStackCallInfo>(); 

                    for (int i = 0; i < size; i++) {
                        long addr = list[i];

                        // first check if the addr has already been converted.
                        NativeStackCallInfo source = mSourceCache.get(addr);

                        // if not we convert it
                        if (source == null) {
                            source = sourceForAddr(addr);
                            mSourceCache.put(addr, source);
                        }

                        resolvedStackCall.add(source);
                    }
                    
                    info.setResolvedStackCall(resolvedStackCall);
                }
                // after every DISPLAY_PER_PAGE we ask for a ui refresh, unless
                // we reach total, since we also do it after the loop
                // (only an issue in case we have a perfect number of page)
                count++;
                if ((count % DISPLAY_PER_PAGE) == 0 && count != total) {
                    if (updateNHAllocationStackCalls(mClientData, count) == false) {
                        // looks like the app is quitting, so we just
                        // stopped the thread
                        return;
                    }
                }
            }

            updateNHAllocationStackCalls(mClientData, count);
        }

        private NativeStackCallInfo sourceForAddr(long addr) {
            NativeLibraryMapInfo library = getLibraryFor(addr);

            if (library != null) {

                Addr2Line process = Addr2Line.getProcess(library.getLibraryName());
                if (process != null) {
                    // remove the base of the library address
                    long value = addr - library.getStartAddress();
                    NativeStackCallInfo info = process.getAddress(value);
                    if (info != null) {
                        return info;
                    }
                }
            }

            return new NativeStackCallInfo(library != null ? library.getLibraryName() : null,
                    Long.toHexString(addr), "");
        }

        private NativeLibraryMapInfo getLibraryFor(long addr) {
            Iterator<NativeLibraryMapInfo> it = mClientData.getNativeLibraryMapInfo();

            while (it.hasNext()) {
                NativeLibraryMapInfo info = it.next();

                if (info.isWithinLibrary(addr)) {
                    return info;
                }
            }

            Log.d("ddm-nativeheap", "Failed finding Library for " + Long.toHexString(addr));
            return null;
        }

        /**
         * update the Native Heap panel with the amount of allocation for which the
         * stack call has been computed. This is called from a non UI thread, but
         * will be executed in the UI thread.
         *
         * @param count the amount of allocation
         * @return false if the display was disposed and the update couldn't happen
         */
        private boolean updateNHAllocationStackCalls(final ClientData clientData, final int count) {
            if (mDisplay.isDisposed() == false) {
                mDisplay.asyncExec(new Runnable() {
                    public void run() {
                        updateAllocationStackCalls(clientData, count);
                    }
                });
                return true;
            }
            return false;
        }
    }

    private class FillTableThread extends BackgroundThread {
        private LibraryAllocations mLibAlloc;

        private int mMax;

        public FillTableThread(LibraryAllocations liballoc, int m) {
            mLibAlloc = liballoc;
            mMax = m;
        }

        @Override
        public void run() {
            for (int i = mMax; i > 0 && isQuitting() == false; i -= 10) {
                updateNHLibraryAllocationTable(mLibAlloc, mMax - i, mMax - i + 10);
            }
        }

        /**
         * updates the library allocation table in the Native Heap panel. This is
         * called from a non UI thread, but will be executed in the UI thread.
         *
         * @param liballoc the current library allocation object being displayed
         * @param start start index of items that need to be displayed
         * @param end end index of the items that need to be displayed
         */
        private void updateNHLibraryAllocationTable(final LibraryAllocations libAlloc,
                final int start, final int end) {
            if (mDisplay.isDisposed() == false) {
                mDisplay.asyncExec(new Runnable() {
                    public void run() {
                        updateLibraryAllocationTable(libAlloc, start, end);
                    }
                });
            }

        }
    }

    /** class to aggregate allocations per library */
    public static class LibraryAllocations {
        private String mLibrary;

        private final ArrayList<NativeAllocationInfo> mLibAllocations =
            new ArrayList<NativeAllocationInfo>();

        private int mSize;

        private int mCount;

        /** construct the aggregate object for a library */
        public LibraryAllocations(final String lib) {
            mLibrary = lib;
        }

        /** get the library name */
        public String getLibrary() {
            return mLibrary;
        }

        /** add a NativeAllocationInfo object to this aggregate object */
        public void addAllocation(NativeAllocationInfo info) {
            mLibAllocations.add(info);
        }

        /** get an iterator on the NativeAllocationInfo objects */
        public Iterator<NativeAllocationInfo> getAllocations() {
            return mLibAllocations.iterator();
        }

        /** get a NativeAllocationInfo object by index */
        public NativeAllocationInfo getAllocation(int index) {
            return mLibAllocations.get(index);
        }

        /** returns the NativeAllocationInfo object count */
        public int getAllocationSize() {
            return mLibAllocations.size();
        }

        /** returns the total allocation size */
        public int getSize() {
            return mSize;
        }

        /** returns the number of allocations */
        public int getCount() {
            return mCount;
        }

        /**
         * compute the allocation count and size for allocation objects added
         * through <code>addAllocation()</code>, and sort the objects by
         * total allocation size.
         */
        public void computeAllocationSizeAndCount() {
            mSize = 0;
            mCount = 0;
            for (NativeAllocationInfo info : mLibAllocations) {
                mCount += info.getAllocationCount();
                mSize += info.getAllocationCount() * info.getSize();
            }
            Collections.sort(mLibAllocations, new Comparator<NativeAllocationInfo>() {
                public int compare(NativeAllocationInfo o1, NativeAllocationInfo o2) {
                    return o2.getAllocationCount() * o2.getSize() -
                        o1.getAllocationCount() * o1.getSize();
                }
            });
        }
    }

    /**
     * Create our control(s).
     */
    @Override
    protected Control createControl(Composite parent) {

        mDisplay = parent.getDisplay();

        mBase = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        mBase.setLayout(gl);
        mBase.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // composite for <update btn> <status>
        Composite tmp = new Composite(mBase, SWT.NONE);
        tmp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tmp.setLayout(gl = new GridLayout(2, false));
        gl.marginWidth = gl.marginHeight = 0;

        mFullUpdateButton = new Button(tmp, SWT.NONE);
        mFullUpdateButton.setText("Full Update");
        mFullUpdateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mBackUpClientData = null;
                mDisplayModeCombo.setEnabled(false);
                mSaveButton.setEnabled(false);
                emptyTables();
                // if we already have a stack call computation for this
                // client
                // we stop it
                if (mStackCallThread != null &&
                        mStackCallThread.getClientData() == mClientData) {
                    mStackCallThread.quit();
                    mStackCallThread = null;
                }
                mLibraryAllocations.clear();
                Client client = getCurrentClient();
                if (client != null) {
                    client.requestNativeHeapInformation();
                }
            }
        });

        mUpdateStatus = new Label(tmp, SWT.NONE);
        mUpdateStatus.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // top layout for the combos and oter controls on the right.
        Composite top_layout = new Composite(mBase, SWT.NONE);
        top_layout.setLayout(gl = new GridLayout(4, false));
        gl.marginWidth = gl.marginHeight = 0;
        
        new Label(top_layout, SWT.NONE).setText("Show:");
        
        mAllocDisplayCombo = new Combo(top_layout, SWT.DROP_DOWN | SWT.READ_ONLY);
        mAllocDisplayCombo.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mAllocDisplayCombo.add("All Allocations");
        mAllocDisplayCombo.add("Pre-Zygote Allocations");
        mAllocDisplayCombo.add("Zygote Child Allocations (Z)");
        mAllocDisplayCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onAllocDisplayChange();
            }
        });
        mAllocDisplayCombo.select(0);
        
        // separator
        Label separator = new Label(top_layout, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd;
        separator.setLayoutData(gd = new GridData(
                GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
        gd.heightHint = 0;
        gd.verticalSpan = 2;

        mSaveButton = new Button(top_layout, SWT.PUSH);
        mSaveButton.setText("Save...");
        mSaveButton.setEnabled(false);
        mSaveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(mBase.getShell(), SWT.SAVE);

                fileDialog.setText("Save Allocations");
                fileDialog.setFileName("allocations.txt");

                String fileName = fileDialog.open();
                if (fileName != null) {
                    saveAllocations(fileName);
                }
            }
        });
        
        /*
         * TODO: either fix the diff mechanism or remove it altogether.
        mDiffUpdateButton = new Button(top_layout, SWT.NONE);
        mDiffUpdateButton.setText("Update && Diff");
        mDiffUpdateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // since this is an update and diff, we need to store the
                // current list
                // of mallocs
                mBackUpAllocations.clear();
                mBackUpAllocations.addAll(mAllocations);
                mBackUpClientData = mClientData;
                mBackUpTotalMemory = mClientData.getTotalNativeMemory();

                mDisplayModeCombo.setEnabled(false);
                emptyTables();
                // if we already have a stack call computation for this
                // client
                // we stop it
                if (mStackCallThread != null &&
                        mStackCallThread.getClientData() == mClientData) {
                    mStackCallThread.quit();
                    mStackCallThread = null;
                }
                mLibraryAllocations.clear();
                Client client = getCurrentClient();
                if (client != null) {
                    client.requestNativeHeapInformation();
                }
            }
        });
        */

        Label l = new Label(top_layout, SWT.NONE);
        l.setText("Display:");

        mDisplayModeCombo = new Combo(top_layout, SWT.DROP_DOWN | SWT.READ_ONLY);
        mDisplayModeCombo.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
        mDisplayModeCombo.setItems(new String[] { "Allocation List", "By Libraries" });
        mDisplayModeCombo.select(0);
        mDisplayModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switchDisplayMode();
            }
        });
        mDisplayModeCombo.setEnabled(false);
        
        mSymbolsButton = new Button(top_layout, SWT.PUSH);
        mSymbolsButton.setText("Load Symbols");
        mSymbolsButton.setEnabled(false);


        // create a composite that will contains the actual content composites,
        // in stack mode layout.
        // This top level composite contains 2 other composites.
        // * one for both Allocations and Libraries mode
        // * one for flat mode (which is gone for now)

        mTopStackComposite = new Composite(mBase, SWT.NONE);
        mTopStackComposite.setLayout(mTopStackLayout = new StackLayout());
        mTopStackComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // create 1st and 2nd modes
        createTableDisplay(mTopStackComposite);

        mTopStackLayout.topControl = mTableModeControl;
        mTopStackComposite.layout();

        setUpdateStatus(NOT_SELECTED);

        // Work in progress
        // TODO add image display of native heap.
        //mImage = new Label(mBase, SWT.NONE);

        mBase.pack();

        return mBase;
    }
    
    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        // TODO
    }


    /**
     * Sent when an existing client information changed.
     * <p/>
     * This is sent from a non UI thread.
     * @param client the updated client.
     * @param changeMask the bit mask describing the changed properties. It can contain
     * any of the following values: {@link Client#CHANGE_INFO}, {@link Client#CHANGE_NAME}
     * {@link Client#CHANGE_DEBUGGER_STATUS}, {@link Client#CHANGE_THREAD_MODE},
     * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
     * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
     *
     * @see IClientChangeListener#clientChanged(Client, int)
     */
    public void clientChanged(final Client client, int changeMask) {
        if (client == getCurrentClient()) {
            if ((changeMask & Client.CHANGE_NATIVE_HEAP_DATA) == Client.CHANGE_NATIVE_HEAP_DATA) {
                if (mBase.isDisposed())
                    return;

                mBase.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        clientSelected();
                    }
                });
            }
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}.
     */
    @Override
    public void deviceSelected() {
        // pass
    }

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}.
     */
    @Override
    public void clientSelected() {
        if (mBase.isDisposed())
            return;

        Client client = getCurrentClient();

        mDisplayModeCombo.setEnabled(false);
        emptyTables();

        Log.d("ddms", "NativeHeapPanel: changed " + client);

        if (client != null) {
            ClientData cd = client.getClientData();
            mClientData = cd;

            // if (cd.getShowHeapUpdates())
            setUpdateStatus(ENABLED);
            // else
            // setUpdateStatus(NOT_ENABLED);

            initAllocationDisplay();

            //renderBitmap(cd);
        } else {
            mClientData = null;
            setUpdateStatus(NOT_SELECTED);
        }

        mBase.pack();
    }

    /**
     * Update the UI with the newly compute stack calls, unless the UI switched
     * to a different client.
     *
     * @param cd the ClientData for which the stack call are being computed.
     * @param count the current count of allocations for which the stack calls
     *            have been computed.
     */
    @WorkerThread
    public void updateAllocationStackCalls(ClientData cd, int count) {
        // we have to check that the panel still shows the same clientdata than
        // the thread is computing for.
        if (cd == mClientData) {

            int total = mAllocations.size();

            if (count == total) {
                // we're done: do something
                mDisplayModeCombo.setEnabled(true);
                mSaveButton.setEnabled(true);
                
                mStackCallThread = null;
            } else {
                // work in progress, update the progress bar.
//                mUiThread.setStatusLine("Computing stack call: " + count
//                        + "/" + total);
            }

            // FIXME: attempt to only update when needed.
            // Because the number of pages is not related to mAllocations.size() anymore
            // due to pre-zygote/post-zygote display, update all the time.
            // At some point we should remove the pages anyway, since it's getting computed
            // really fast now.
//            if ((mCurrentPage + 1) * DISPLAY_PER_PAGE == count
//                    || (count == total && mCurrentPage == mPageCount - 1)) {
            try {
                // get the current selection of the allocation
                int index = mAllocationTable.getSelectionIndex();
                NativeAllocationInfo info = null;
                
                if (index != -1) {
                    info = (NativeAllocationInfo)mAllocationTable.getItem(index).getData();
                }

                // empty the table
                emptyTables();

                // fill it again
                fillAllocationTable();

                // reselect
                mAllocationTable.setSelection(index);

                // display detail table if needed
                if (info != null) {
                    fillDetailTable(info);
                }
            } catch (SWTException e) {
                if (mAllocationTable.isDisposed()) {
                    // looks like the table is disposed. Let's ignore it.
                } else {
                    throw e;
                }
            }

        } else {
            // old client still running. doesn't really matter.
        }
    }

    @Override
    protected void setTableFocusListener() {
        addTableToFocusListener(mAllocationTable);
        addTableToFocusListener(mLibraryTable);
        addTableToFocusListener(mLibraryAllocationTable);
        addTableToFocusListener(mDetailTable);
    }
    
    protected void onAllocDisplayChange() {
        mAllocDisplayMode = mAllocDisplayCombo.getSelectionIndex();
        
        // create the new list
        updateAllocDisplayList();
        
        updateTotalMemoryDisplay();

        // reset the ui.
        mCurrentPage = 0;
        updatePageUI();
        switchDisplayMode();
    }
    
    private void updateAllocDisplayList() {
        mTotalSize = 0;
        mDisplayedAllocations.clear();
        for (NativeAllocationInfo info : mAllocations) {
            if (mAllocDisplayMode == ALLOC_DISPLAY_ALL ||
                    (mAllocDisplayMode == ALLOC_DISPLAY_PRE_ZYGOTE ^ info.isZygoteChild())) {
                mDisplayedAllocations.add(info);
                mTotalSize += info.getSize() * info.getAllocationCount();
            } else {
                // skip this item
                continue;
            }
        }
        
        int count = mDisplayedAllocations.size();
        
        mPageCount = count / DISPLAY_PER_PAGE;

        // need to add a page for the rest of the div
        if ((count % DISPLAY_PER_PAGE) > 0) {
            mPageCount++;
        }
    }
    
    private void updateTotalMemoryDisplay() {
        switch (mAllocDisplayMode) {
            case ALLOC_DISPLAY_ALL:
                mTotalMemoryLabel.setText(String.format("Total Memory: %1$s Bytes",
                        sFormatter.format(mTotalSize)));
                break;
            case ALLOC_DISPLAY_PRE_ZYGOTE:
                mTotalMemoryLabel.setText(String.format("Zygote Memory: %1$s Bytes",
                        sFormatter.format(mTotalSize)));
                break;
            case ALLOC_DISPLAY_POST_ZYGOTE:
                mTotalMemoryLabel.setText(String.format("Post-zygote Memory: %1$s Bytes",
                        sFormatter.format(mTotalSize)));
                break;
        }
    }


    private void switchDisplayMode() {
        switch (mDisplayModeCombo.getSelectionIndex()) {
            case 0: {// allocations
                mTopStackLayout.topControl = mTableModeControl;
                mAllocationStackLayout.topControl = mAllocationModeTop;
                mAllocationStackComposite.layout();
                mTopStackComposite.layout();
                emptyTables();
                fillAllocationTable();
            }
                break;
            case 1: {// libraries
                mTopStackLayout.topControl = mTableModeControl;
                mAllocationStackLayout.topControl = mLibraryModeTopControl;
                mAllocationStackComposite.layout();
                mTopStackComposite.layout();
                emptyTables();
                fillLibraryTable();
            }
                break;
        }
    }

    private void initAllocationDisplay() {
        mAllocations.clear();
        mAllocations.addAll(mClientData.getNativeAllocationList());
        
        updateAllocDisplayList();

        // if we have a previous clientdata and it matches the current one. we
        // do a diff between the new list and the old one.
        if (mBackUpClientData != null && mBackUpClientData == mClientData) {

            ArrayList<NativeAllocationInfo> add = new ArrayList<NativeAllocationInfo>();

            // we go through the list of NativeAllocationInfo in the new list and check if
            // there's one with the same exact data (size, allocation, count and
            // stackcall addresses) in the old list.
            // if we don't find any, we add it to the "add" list
            for (NativeAllocationInfo mi : mAllocations) {
                boolean found = false;
                for (NativeAllocationInfo old_mi : mBackUpAllocations) {
                    if (mi.equals(old_mi)) {
                        found = true;
                        break;
                    }
                }
                if (found == false) {
                    add.add(mi);
                }
            }

            // put the result in mAllocations
            mAllocations.clear();
            mAllocations.addAll(add);

            // display the difference in memory usage. This is computed
            // calculating the memory usage of the objects in mAllocations.
            int count = 0;
            for (NativeAllocationInfo allocInfo : mAllocations) {
                count += allocInfo.getSize() * allocInfo.getAllocationCount();
            }

            mTotalMemoryLabel.setText(String.format("Memory Difference: %1$s Bytes",
                    sFormatter.format(count)));
        }
        else {
            // display the full memory usage
            updateTotalMemoryDisplay();
            //mDiffUpdateButton.setEnabled(mClientData.getTotalNativeMemory() > 0);
        }
        mTotalMemoryLabel.pack();

        // update the page ui
        mDisplayModeCombo.select(0);

        mLibraryAllocations.clear();

        // reset to first page
        mCurrentPage = 0;

        // update the label
        updatePageUI();

        // now fill the allocation Table with the current page
        switchDisplayMode();

        // start the thread to compute the stack calls
        if (mAllocations.size() > 0) {
            mStackCallThread = new StackCallThread(mClientData);
            mStackCallThread.start();
        }
    }

    private void updatePageUI() {

        // set the label and pack to update the layout, otherwise
        // the label will be cut off if the new size is bigger
        if (mPageCount == 0) {
            mPageLabel.setText("0 of 0 allocations.");
        } else {
            StringBuffer buffer = new StringBuffer();
            // get our starting index
            int start = (mCurrentPage * DISPLAY_PER_PAGE) + 1;
            // end index, taking into account the last page can be half full
            int count = mDisplayedAllocations.size();
            int end = Math.min(start + DISPLAY_PER_PAGE - 1, count);
            buffer.append(sFormatter.format(start));
            buffer.append(" - ");
            buffer.append(sFormatter.format(end));
            buffer.append(" of ");
            buffer.append(sFormatter.format(count));
            buffer.append(" allocations.");
            mPageLabel.setText(buffer.toString());
        }

        // handle the button enabled state.
        mPagePreviousButton.setEnabled(mCurrentPage > 0);
        // reminder: mCurrentPage starts at 0.
        mPageNextButton.setEnabled(mCurrentPage < mPageCount - 1);

        mPageLabel.pack();
        mPageUIComposite.pack();

    }

    private void fillAllocationTable() {
        // get the count
        int count = mDisplayedAllocations.size();

        // get our starting index
        int start = mCurrentPage * DISPLAY_PER_PAGE;

        // loop for DISPLAY_PER_PAGE or till we reach count
        int end = start + DISPLAY_PER_PAGE;

        for (int i = start; i < end && i < count; i++) {
            NativeAllocationInfo info = mDisplayedAllocations.get(i);

            TableItem item = null;

            if (mAllocDisplayMode == ALLOC_DISPLAY_ALL)  {
                item = new TableItem(mAllocationTable, SWT.NONE);
                item.setText(0, (info.isZygoteChild() ? "Z " : "") +
                        sFormatter.format(info.getSize() * info.getAllocationCount()));
                item.setText(1, sFormatter.format(info.getAllocationCount()));
                item.setText(2, sFormatter.format(info.getSize()));
            } else if (mAllocDisplayMode == ALLOC_DISPLAY_PRE_ZYGOTE ^ info.isZygoteChild()) {
                item = new TableItem(mAllocationTable, SWT.NONE);
                item.setText(0, sFormatter.format(info.getSize() * info.getAllocationCount()));
                item.setText(1, sFormatter.format(info.getAllocationCount()));
                item.setText(2, sFormatter.format(info.getSize()));
            } else {
                // skip this item
                continue;
            }

            item.setData(info);

            NativeStackCallInfo bti = info.getRelevantStackCallInfo();
            if (bti != null) {
                String lib = bti.getLibraryName();
                String method = bti.getMethodName();
                String source = bti.getSourceFile();
                if (lib != null)
                    item.setText(3, lib);
                if (method != null)
                    item.setText(4, method);
                if (source != null)
                    item.setText(5, source);
            }
        }
    }

    private void fillLibraryTable() {
        // fill the library table
        sortAllocationsPerLibrary();

        for (LibraryAllocations liballoc : mLibraryAllocations) {
            if (liballoc != null) {
                TableItem item = new TableItem(mLibraryTable, SWT.NONE);
                String lib = liballoc.getLibrary();
                item.setText(0, lib != null ? lib : "");
                item.setText(1, sFormatter.format(liballoc.getSize()));
                item.setText(2, sFormatter.format(liballoc.getCount()));
            }
        }
    }

    private void fillLibraryAllocationTable() {
        mLibraryAllocationTable.removeAll();
        mDetailTable.removeAll();
        int index = mLibraryTable.getSelectionIndex();
        if (index != -1) {
            LibraryAllocations liballoc = mLibraryAllocations.get(index);
            // start a thread that will fill table 10 at a time to keep the ui
            // responsive, but first we kill the previous one if there was one
            if (mFillTableThread != null) {
                mFillTableThread.quit();
            }
            mFillTableThread = new FillTableThread(liballoc,
                    liballoc.getAllocationSize());
            mFillTableThread.start();
        }
    }

    public void updateLibraryAllocationTable(LibraryAllocations liballoc,
            int start, int end) {
        try {
            if (mLibraryTable.isDisposed() == false) {
                int index = mLibraryTable.getSelectionIndex();
                if (index != -1) {
                    LibraryAllocations newliballoc = mLibraryAllocations.get(
                            index);
                    if (newliballoc == liballoc) {
                        int count = liballoc.getAllocationSize();
                        for (int i = start; i < end && i < count; i++) {
                            NativeAllocationInfo info = liballoc.getAllocation(i);

                            TableItem item = new TableItem(
                                    mLibraryAllocationTable, SWT.NONE);
                            item.setText(0, sFormatter.format(
                                    info.getSize() * info.getAllocationCount()));
                            item.setText(1, sFormatter.format(info.getAllocationCount()));
                            item.setText(2, sFormatter.format(info.getSize()));

                            NativeStackCallInfo stackCallInfo = info.getRelevantStackCallInfo();
                            if (stackCallInfo != null) {
                                item.setText(3, stackCallInfo.getMethodName());
                            }
                        }
                    } else {
                        // we should quit the thread
                        if (mFillTableThread != null) {
                            mFillTableThread.quit();
                            mFillTableThread = null;
                        }
                    }
                }
            }
        } catch (SWTException e) {
            Log.e("ddms", "error when updating the library allocation table");
        }
    }

    private void fillDetailTable(final NativeAllocationInfo mi) {
        mDetailTable.removeAll();
        mDetailTable.setRedraw(false);
        
        try {
            // populate the detail Table with the back trace
            Long[] addresses = mi.getStackCallAddresses();
            NativeStackCallInfo[] resolvedStackCall = mi.getResolvedStackCall();
            
            if (resolvedStackCall == null) {
                return;
            }

            for (int i = 0 ; i < resolvedStackCall.length ; i++) {
                if (addresses[i] == null || addresses[i].longValue() == 0) {
                    continue;
                }
                
                long addr = addresses[i].longValue();
                NativeStackCallInfo source = resolvedStackCall[i];
                
                TableItem item = new TableItem(mDetailTable, SWT.NONE);
                item.setText(0, String.format("%08x", addr)); //$NON-NLS-1$
    
                String libraryName = source.getLibraryName();
                String methodName = source.getMethodName();
                String sourceFile = source.getSourceFile();
                int lineNumber = source.getLineNumber();
                
                if (libraryName != null)
                    item.setText(1, libraryName);
                if (methodName != null)
                    item.setText(2, methodName);
                if (sourceFile != null)
                    item.setText(3, sourceFile);
                if (lineNumber != -1)
                    item.setText(4, Integer.toString(lineNumber));
            }
        } finally {
            mDetailTable.setRedraw(true);
        }
    }

    /*
     * Are updates enabled?
     */
    private void setUpdateStatus(int status) {
        switch (status) {
            case NOT_SELECTED:
                mUpdateStatus.setText("Select a client to see heap info");
                mAllocDisplayCombo.setEnabled(false);
                mFullUpdateButton.setEnabled(false);
                //mDiffUpdateButton.setEnabled(false);
                break;
            case NOT_ENABLED:
                mUpdateStatus.setText("Heap updates are " + "NOT ENABLED for this client");
                mAllocDisplayCombo.setEnabled(false);
                mFullUpdateButton.setEnabled(false);
                //mDiffUpdateButton.setEnabled(false);
                break;
            case ENABLED:
                mUpdateStatus.setText("Press 'Full Update' to retrieve " + "latest data");
                mAllocDisplayCombo.setEnabled(true);
                mFullUpdateButton.setEnabled(true);
                //mDiffUpdateButton.setEnabled(true);
                break;
            default:
                throw new RuntimeException();
        }

        mUpdateStatus.pack();
    }

    /**
     * Create the Table display. This includes a "detail" Table in the bottom
     * half and 2 modes in the top half: allocation Table and
     * library+allocations Tables.
     *
     * @param base the top parent to create the display into
     */
    private void createTableDisplay(Composite base) {
        final int minPanelWidth = 60;

        final IPreferenceStore prefs = DdmUiPreferences.getStore();

        // top level composite for mode 1 & 2
        mTableModeControl = new Composite(base, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
        mTableModeControl.setLayout(gl);
        mTableModeControl.setLayoutData(new GridData(GridData.FILL_BOTH));

        mTotalMemoryLabel = new Label(mTableModeControl, SWT.NONE);
        mTotalMemoryLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mTotalMemoryLabel.setText("Total Memory: 0 Bytes");

        // the top half of these modes is dynamic

        final Composite sash_composite = new Composite(mTableModeControl,
                SWT.NONE);
        sash_composite.setLayout(new FormLayout());
        sash_composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // create the stacked composite
        mAllocationStackComposite = new Composite(sash_composite, SWT.NONE);
        mAllocationStackLayout = new StackLayout();
        mAllocationStackComposite.setLayout(mAllocationStackLayout);
        mAllocationStackComposite.setLayoutData(new GridData(
                GridData.FILL_BOTH));

        // create the top half for mode 1
        createAllocationTopHalf(mAllocationStackComposite);

        // create the top half for mode 2
        createLibraryTopHalf(mAllocationStackComposite);

        final Sash sash = new Sash(sash_composite, SWT.HORIZONTAL);

        // bottom half of these modes is the same: detail table
        createDetailTable(sash_composite);

        // init value for stack
        mAllocationStackLayout.topControl = mAllocationModeTop;

        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(mTotalMemoryLabel, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mAllocationStackComposite.setLayoutData(data);

        final FormData sashData = new FormData();
        if (prefs != null && prefs.contains(PREFS_ALLOCATION_SASH)) {
            sashData.top = new FormAttachment(0,
                    prefs.getInt(PREFS_ALLOCATION_SASH));
        } else {
            sashData.top = new FormAttachment(50, 0); // 50% across
        }
        sashData.left = new FormAttachment(0, 0);
        sashData.right = new FormAttachment(100, 0);
        sash.setLayoutData(sashData);

        data = new FormData();
        data.top = new FormAttachment(sash, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mDetailTable.setLayoutData(data);

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = sash_composite.getClientArea();
                int bottom = panelRect.height - sashRect.height - minPanelWidth;
                e.y = Math.max(Math.min(e.y, bottom), minPanelWidth);
                if (e.y != sashRect.y) {
                    sashData.top = new FormAttachment(0, e.y);
                    prefs.setValue(PREFS_ALLOCATION_SASH, e.y);
                    sash_composite.layout();
                }
            }
        });
    }

    private void createDetailTable(Composite base) {

        final IPreferenceStore prefs = DdmUiPreferences.getStore();

        mDetailTable = new Table(base, SWT.MULTI | SWT.FULL_SELECTION);
        mDetailTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mDetailTable.setHeaderVisible(true);
        mDetailTable.setLinesVisible(true);

        TableHelper.createTableColumn(mDetailTable, "Address", SWT.RIGHT,
                "00000000", PREFS_DETAIL_ADDRESS, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mDetailTable, "Library", SWT.LEFT,
                "abcdefghijklmnopqrst", PREFS_DETAIL_LIBRARY, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mDetailTable, "Method", SWT.LEFT,
                "abcdefghijklmnopqrst", PREFS_DETAIL_METHOD, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mDetailTable, "File", SWT.LEFT,
                "abcdefghijklmnopqrstuvwxyz", PREFS_DETAIL_FILE, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mDetailTable, "Line", SWT.RIGHT,
                "9,999", PREFS_DETAIL_LINE, prefs); //$NON-NLS-1$
    }

    private void createAllocationTopHalf(Composite b) {
        final IPreferenceStore prefs = DdmUiPreferences.getStore();

        Composite base = new Composite(b, SWT.NONE);
        mAllocationModeTop = base;
        GridLayout gl = new GridLayout(1, false);
        gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
        gl.verticalSpacing = 0;
        base.setLayout(gl);
        base.setLayoutData(new GridData(GridData.FILL_BOTH));

        // horizontal layout for memory total and pages UI
        mPageUIComposite = new Composite(base, SWT.NONE);
        mPageUIComposite.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_BEGINNING));
        gl = new GridLayout(3, false);
        gl.marginLeft = gl.marginRight = gl.marginTop = gl.marginBottom = 0;
        gl.horizontalSpacing = 0;
        mPageUIComposite.setLayout(gl);

        // Page UI
        mPagePreviousButton = new Button(mPageUIComposite, SWT.NONE);
        mPagePreviousButton.setText("<");
        mPagePreviousButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mCurrentPage--;
                updatePageUI();
                emptyTables();
                fillAllocationTable();
            }
        });

        mPageNextButton = new Button(mPageUIComposite, SWT.NONE);
        mPageNextButton.setText(">");
        mPageNextButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mCurrentPage++;
                updatePageUI();
                emptyTables();
                fillAllocationTable();
            }
        });

        mPageLabel = new Label(mPageUIComposite, SWT.NONE);
        mPageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updatePageUI();

        mAllocationTable = new Table(base, SWT.MULTI | SWT.FULL_SELECTION);
        mAllocationTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mAllocationTable.setHeaderVisible(true);
        mAllocationTable.setLinesVisible(true);

        TableHelper.createTableColumn(mAllocationTable, "Total", SWT.RIGHT,
                "9,999,999", PREFS_ALLOC_TOTAL, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mAllocationTable, "Count", SWT.RIGHT,
                "9,999", PREFS_ALLOC_COUNT, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mAllocationTable, "Size", SWT.RIGHT,
                "999,999", PREFS_ALLOC_SIZE, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mAllocationTable, "Library", SWT.LEFT,
                "abcdefghijklmnopqrst", PREFS_ALLOC_LIBRARY, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mAllocationTable, "Method", SWT.LEFT,
                "abcdefghijklmnopqrst", PREFS_ALLOC_METHOD, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mAllocationTable, "File", SWT.LEFT,
                "abcdefghijklmnopqrstuvwxyz", PREFS_ALLOC_FILE, prefs); //$NON-NLS-1$

        mAllocationTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the selection index
                int index = mAllocationTable.getSelectionIndex();
                TableItem item = mAllocationTable.getItem(index);
                if (item != null && item.getData() instanceof NativeAllocationInfo) {
                    fillDetailTable((NativeAllocationInfo)item.getData());
                }
            }
        });
    }

    private void createLibraryTopHalf(Composite base) {
        final int minPanelWidth = 60;

        final IPreferenceStore prefs = DdmUiPreferences.getStore();

        // create a composite that'll contain 2 tables horizontally
        final Composite top = new Composite(base, SWT.NONE);
        mLibraryModeTopControl = top;
        top.setLayout(new FormLayout());
        top.setLayoutData(new GridData(GridData.FILL_BOTH));

        // first table: library
        mLibraryTable = new Table(top, SWT.MULTI | SWT.FULL_SELECTION);
        mLibraryTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mLibraryTable.setHeaderVisible(true);
        mLibraryTable.setLinesVisible(true);

        TableHelper.createTableColumn(mLibraryTable, "Library", SWT.LEFT,
                "abcdefghijklmnopqrstuvwxyz", PREFS_LIB_LIBRARY, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mLibraryTable, "Size", SWT.RIGHT,
                "9,999,999", PREFS_LIB_SIZE, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mLibraryTable, "Count", SWT.RIGHT,
                "9,999", PREFS_LIB_COUNT, prefs); //$NON-NLS-1$

        mLibraryTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillLibraryAllocationTable();
            }
        });

        final Sash sash = new Sash(top, SWT.VERTICAL);

        // 2nd table: allocation per library
        mLibraryAllocationTable = new Table(top, SWT.MULTI | SWT.FULL_SELECTION);
        mLibraryAllocationTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mLibraryAllocationTable.setHeaderVisible(true);
        mLibraryAllocationTable.setLinesVisible(true);

        TableHelper.createTableColumn(mLibraryAllocationTable, "Total",
                SWT.RIGHT, "9,999,999", PREFS_LIBALLOC_TOTAL, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mLibraryAllocationTable, "Count",
                SWT.RIGHT, "9,999", PREFS_LIBALLOC_COUNT, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mLibraryAllocationTable, "Size",
                SWT.RIGHT, "999,999", PREFS_LIBALLOC_SIZE, prefs); //$NON-NLS-1$
        TableHelper.createTableColumn(mLibraryAllocationTable, "Method",
                SWT.LEFT, "abcdefghijklmnopqrst", PREFS_LIBALLOC_METHOD, prefs); //$NON-NLS-1$

        mLibraryAllocationTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the index of the selection in the library table
                int index1 = mLibraryTable.getSelectionIndex();
                // get the index in the library allocation table
                int index2 = mLibraryAllocationTable.getSelectionIndex();
                // get the MallocInfo object
                LibraryAllocations liballoc = mLibraryAllocations.get(index1);
                NativeAllocationInfo info = liballoc.getAllocation(index2);
                fillDetailTable(info);
            }
        });

        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(sash, 0);
        mLibraryTable.setLayoutData(data);

        final FormData sashData = new FormData();
        if (prefs != null && prefs.contains(PREFS_LIBRARY_SASH)) {
            sashData.left = new FormAttachment(0,
                    prefs.getInt(PREFS_LIBRARY_SASH));
        } else {
            sashData.left = new FormAttachment(50, 0);
        }
        sashData.bottom = new FormAttachment(100, 0);
        sashData.top = new FormAttachment(0, 0); // 50% across
        sash.setLayoutData(sashData);

        data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(sash, 0);
        data.right = new FormAttachment(100, 0);
        mLibraryAllocationTable.setLayoutData(data);

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = top.getClientArea();
                int right = panelRect.width - sashRect.width - minPanelWidth;
                e.x = Math.max(Math.min(e.x, right), minPanelWidth);
                if (e.x != sashRect.x) {
                    sashData.left = new FormAttachment(0, e.x);
                    prefs.setValue(PREFS_LIBRARY_SASH, e.y);
                    top.layout();
                }
            }
        });
    }

    private void emptyTables() {
        mAllocationTable.removeAll();
        mLibraryTable.removeAll();
        mLibraryAllocationTable.removeAll();
        mDetailTable.removeAll();
    }

    private void sortAllocationsPerLibrary() {
        if (mClientData != null) {
            mLibraryAllocations.clear();
            
            // create a hash map of LibraryAllocations to access aggregate
            // objects already created
            HashMap<String, LibraryAllocations> libcache =
                new HashMap<String, LibraryAllocations>();

            // get the allocation count
            int count = mDisplayedAllocations.size();
            for (int i = 0; i < count; i++) {
                NativeAllocationInfo allocInfo = mDisplayedAllocations.get(i);

                NativeStackCallInfo stackCallInfo = allocInfo.getRelevantStackCallInfo();
                if (stackCallInfo != null) {
                    String libraryName = stackCallInfo.getLibraryName();
                    LibraryAllocations liballoc = libcache.get(libraryName);
                    if (liballoc == null) {
                        // didn't find a library allocation object already
                        // created so we create one
                        liballoc = new LibraryAllocations(libraryName);
                        // add it to the cache
                        libcache.put(libraryName, liballoc);
                        // add it to the list
                        mLibraryAllocations.add(liballoc);
                    }
                    // add the MallocInfo object to it.
                    liballoc.addAllocation(allocInfo);
                }
            }
            // now that the list is created, we need to compute the size and
            // sort it by size. This will also sort the MallocInfo objects
            // inside each LibraryAllocation objects.
            for (LibraryAllocations liballoc : mLibraryAllocations) {
                liballoc.computeAllocationSizeAndCount();
            }

            // now we sort it
            Collections.sort(mLibraryAllocations,
                    new Comparator<LibraryAllocations>() {
                public int compare(LibraryAllocations o1,
                        LibraryAllocations o2) {
                    return o2.getSize() - o1.getSize();
                }
            });
        }
    }

    private void renderBitmap(ClientData cd) {
        byte[] pixData;

        // Atomically get and clear the heap data.
        synchronized (cd) {
            if (serializeHeapData(cd.getVmHeapData()) == false) {
                // no change, we return.
                return;
            }

            pixData = getSerializedData();

            ImageData id = createLinearHeapImage(pixData, 200, mMapPalette);
            Image image = new Image(mBase.getDisplay(), id);
            mImage.setImage(image);
            mImage.pack(true);
        }
    }

    /*
     * Create color palette for map.  Set up titles for legend.
     */
    private static PaletteData createPalette() {
        RGB colors[] = new RGB[NUM_PALETTE_ENTRIES];
        colors[0]
                = new RGB(192, 192, 192); // non-heap pixels are gray
        mMapLegend[0]
                = "(heap expansion area)";

        colors[1]
                = new RGB(0, 0, 0);       // free chunks are black
        mMapLegend[1]
                = "free";

        colors[HeapSegmentElement.KIND_OBJECT + 2]
                = new RGB(0, 0, 255);     // objects are blue
        mMapLegend[HeapSegmentElement.KIND_OBJECT + 2]
                = "data object";

        colors[HeapSegmentElement.KIND_CLASS_OBJECT + 2]
                = new RGB(0, 255, 0);     // class objects are green
        mMapLegend[HeapSegmentElement.KIND_CLASS_OBJECT + 2]
                = "class object";

        colors[HeapSegmentElement.KIND_ARRAY_1 + 2]
                = new RGB(255, 0, 0);     // byte/bool arrays are red
        mMapLegend[HeapSegmentElement.KIND_ARRAY_1 + 2]
                = "1-byte array (byte[], boolean[])";

        colors[HeapSegmentElement.KIND_ARRAY_2 + 2]
                = new RGB(255, 128, 0);   // short/char arrays are orange
        mMapLegend[HeapSegmentElement.KIND_ARRAY_2 + 2]
                = "2-byte array (short[], char[])";

        colors[HeapSegmentElement.KIND_ARRAY_4 + 2]
                = new RGB(255, 255, 0);   // obj/int/float arrays are yellow
        mMapLegend[HeapSegmentElement.KIND_ARRAY_4 + 2]
                = "4-byte array (object[], int[], float[])";

        colors[HeapSegmentElement.KIND_ARRAY_8 + 2]
                = new RGB(255, 128, 128); // long/double arrays are pink
        mMapLegend[HeapSegmentElement.KIND_ARRAY_8 + 2]
                = "8-byte array (long[], double[])";

        colors[HeapSegmentElement.KIND_UNKNOWN + 2]
                = new RGB(255, 0, 255);   // unknown objects are cyan
        mMapLegend[HeapSegmentElement.KIND_UNKNOWN + 2]
                = "unknown object";

        colors[HeapSegmentElement.KIND_NATIVE + 2]
                = new RGB(64, 64, 64);    // native objects are dark gray
        mMapLegend[HeapSegmentElement.KIND_NATIVE + 2]
                = "non-Java object";

        return new PaletteData(colors);
    }
    
    private void saveAllocations(String fileName) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
    
            for (NativeAllocationInfo alloc : mAllocations) {
                out.println(alloc.toString());
            }
            out.close();
        } catch (IOException e) {
            Log.e("Native", e);
        }
    }
}
