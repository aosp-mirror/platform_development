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
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.HeapSegment.HeapSegmentElement;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.experimental.swt.SWTUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Base class for our information panels.
 */
public final class HeapPanel extends BaseHeapPanel {
    private static final String PREFS_STATS_COL_TYPE = "heapPanel.col0"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_COUNT = "heapPanel.col1"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_SIZE = "heapPanel.col2"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_SMALLEST = "heapPanel.col3"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_LARGEST = "heapPanel.col4"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_MEDIAN = "heapPanel.col5"; //$NON-NLS-1$
    private static final String PREFS_STATS_COL_AVERAGE = "heapPanel.col6"; //$NON-NLS-1$

    /* args to setUpdateStatus() */
    private static final int NOT_SELECTED   = 0;
    private static final int NOT_ENABLED    = 1;
    private static final int ENABLED        = 2;

    /** color palette and map legend. NATIVE is the last enum is a 0 based enum list, so we need
     * Native+1 at least. We also need 2 more entries for free area and expansion area.  */
    private static final int NUM_PALETTE_ENTRIES = HeapSegmentElement.KIND_NATIVE+2 +1;
    private static final String[] mMapLegend = new String[NUM_PALETTE_ENTRIES];
    private static final PaletteData mMapPalette = createPalette();

    private static final boolean DISPLAY_HEAP_BITMAP = false;
    private static final boolean DISPLAY_HILBERT_BITMAP = false;

    private static final int PLACEHOLDER_HILBERT_SIZE = 200;
    private static final int PLACEHOLDER_LINEAR_V_SIZE = 100;
    private static final int PLACEHOLDER_LINEAR_H_SIZE = 300;

    private static final int[] ZOOMS = {100, 50, 25};
    
    private static final NumberFormat sByteFormatter = NumberFormat.getInstance();
    private static final NumberFormat sLargeByteFormatter = NumberFormat.getInstance();
    private static final NumberFormat sCountFormatter = NumberFormat.getInstance();
    
    static {
        sByteFormatter.setMinimumFractionDigits(0);
        sByteFormatter.setMaximumFractionDigits(1);
        sLargeByteFormatter.setMinimumFractionDigits(3);
        sLargeByteFormatter.setMaximumFractionDigits(3);

        sCountFormatter.setGroupingUsed(true);
    }

    private Display mDisplay;

    private Composite mTop; // real top
    private Label mUpdateStatus;
    private Table mHeapSummary;
    private Combo mDisplayMode;

    //private ScrolledComposite mScrolledComposite;

    private Composite mDisplayBase; // base of the displays.
    private StackLayout mDisplayStack;

    private Composite mStatisticsBase;
    private Table mStatisticsTable;
    private JFreeChart mChart;
    private ChartComposite mChartComposite;
    private Button mGcButton;
    private DefaultCategoryDataset mAllocCountDataSet;

    private Composite mLinearBase;
    private Label mLinearHeapImage;

    private Composite mHilbertBase;
    private Label mHilbertHeapImage;
    private Group mLegend;
    private Combo mZoom;

    /** Image used for the hilbert display. Since we recreate a new image every time, we
     * keep this one around to dispose it. */
    private Image mHilbertImage;
    private Image mLinearImage;
    private Composite[] mLayout;

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

    /**
     * Sent when an existing client information changed.
     * <p/>
     * This is sent from a non UI thread.
     * @param client the updated client.
     * @param changeMask the bit mask describing the changed properties. It can contain
     * any of the following values: {@link Client#CHANGE_INFO}, {@link Client#CHANGE_NAME}
     * {@link Client#CHANGE_DEBUGGER_INTEREST}, {@link Client#CHANGE_THREAD_MODE},
     * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
     * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
     *
     * @see IClientChangeListener#clientChanged(Client, int)
     */
    public void clientChanged(final Client client, int changeMask) {
        if (client == getCurrentClient()) {
            if ((changeMask & Client.CHANGE_HEAP_MODE) == Client.CHANGE_HEAP_MODE ||
                    (changeMask & Client.CHANGE_HEAP_DATA) == Client.CHANGE_HEAP_DATA) {
                try {
                    mTop.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            clientSelected();
                        }
                    });
                } catch (SWTException e) {
                    // display is disposed (app is quitting most likely), we do nothing.
                }
            }
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}
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
        if (mTop.isDisposed())
            return;

        Client client = getCurrentClient();

        Log.d("ddms", "HeapPanel: changed " + client);

        if (client != null) {
            ClientData cd = client.getClientData();

            if (client.isHeapUpdateEnabled()) {
                mGcButton.setEnabled(true);
                mDisplayMode.setEnabled(true);
                setUpdateStatus(ENABLED);
            } else {
                setUpdateStatus(NOT_ENABLED);
                mGcButton.setEnabled(false);
                mDisplayMode.setEnabled(false);
            }

            fillSummaryTable(cd);
            
            int mode = mDisplayMode.getSelectionIndex();
            if (mode == 0) {
                fillDetailedTable(client, false /* forceRedraw */);
            } else {
                if (DISPLAY_HEAP_BITMAP) {
                    renderHeapData(cd, mode - 1, false /* forceRedraw */);
                }
            }
        } else {
            mGcButton.setEnabled(false);
            mDisplayMode.setEnabled(false);
            fillSummaryTable(null);
            fillDetailedTable(null, true);
            setUpdateStatus(NOT_SELECTED);
        }

        // sizes of things change frequently, so redo layout
        //mScrolledComposite.setMinSize(mDisplayStack.topControl.computeSize(SWT.DEFAULT,
        //        SWT.DEFAULT));
        mDisplayBase.layout();
        //mScrolledComposite.redraw();
    }

    /**
     * Create our control(s).
     */
    @Override
    protected Control createControl(Composite parent) {
        mDisplay = parent.getDisplay();

        GridLayout gl;

        mTop = new Composite(parent, SWT.NONE);
        mTop.setLayout(new GridLayout(1, false));
        mTop.setLayoutData(new GridData(GridData.FILL_BOTH));

        mUpdateStatus = new Label(mTop, SWT.NONE);
        setUpdateStatus(NOT_SELECTED);

        Composite summarySection = new Composite(mTop, SWT.NONE);
        summarySection.setLayout(gl = new GridLayout(2, false));
        gl.marginHeight = gl.marginWidth = 0;

        mHeapSummary = createSummaryTable(summarySection);
        mGcButton = new Button(summarySection, SWT.PUSH);
        mGcButton.setText("Cause GC");
        mGcButton.setEnabled(false);
        mGcButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Client client = getCurrentClient();
                if (client != null) {
                    client.executeGarbageCollector();
                }
            }
        });

        Composite comboSection = new Composite(mTop, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        comboSection.setLayout(gl);

        Label displayLabel = new Label(comboSection, SWT.NONE);
        displayLabel.setText("Display: ");

        mDisplayMode = new Combo(comboSection, SWT.READ_ONLY);
        mDisplayMode.setEnabled(false);
        mDisplayMode.add("Stats");
        if (DISPLAY_HEAP_BITMAP) {
            mDisplayMode.add("Linear");
            if (DISPLAY_HILBERT_BITMAP) {
                mDisplayMode.add("Hilbert");
            }
        }

        // the base of the displays.
        mDisplayBase = new Composite(mTop, SWT.NONE);
        mDisplayBase.setLayoutData(new GridData(GridData.FILL_BOTH));
        mDisplayStack = new StackLayout();
        mDisplayBase.setLayout(mDisplayStack);

        // create the statistics display
        mStatisticsBase = new Composite(mDisplayBase, SWT.NONE);
        //mStatisticsBase.setLayoutData(new GridData(GridData.FILL_BOTH));
        mStatisticsBase.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;
        mDisplayStack.topControl = mStatisticsBase;
        
        mStatisticsTable = createDetailedTable(mStatisticsBase);
        mStatisticsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        createChart();

        //create the linear composite
        mLinearBase = new Composite(mDisplayBase, SWT.NONE);
        //mLinearBase.setLayoutData(new GridData());
        gl = new GridLayout(1, false);
        gl.marginHeight = gl.marginWidth = 0;
        mLinearBase.setLayout(gl);

        {
            mLinearHeapImage = new Label(mLinearBase, SWT.NONE);
            mLinearHeapImage.setLayoutData(new GridData());
            mLinearHeapImage.setImage(ImageHelper.createPlaceHolderArt(mDisplay,
                    PLACEHOLDER_LINEAR_H_SIZE, PLACEHOLDER_LINEAR_V_SIZE,
                    mDisplay.getSystemColor(SWT.COLOR_BLUE)));

            // create a composite to contain the bottom part (legend)
            Composite bottomSection = new Composite(mLinearBase, SWT.NONE);
            gl = new GridLayout(1, false);
            gl.marginHeight = gl.marginWidth = 0;
            bottomSection.setLayout(gl);

            createLegend(bottomSection);
        }

/*        
        mScrolledComposite = new ScrolledComposite(mTop, SWT.H_SCROLL | SWT.V_SCROLL);
        mScrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        mScrolledComposite.setExpandHorizontal(true);
        mScrolledComposite.setExpandVertical(true);
        mScrolledComposite.setContent(mDisplayBase);
*/


        // create the hilbert display.
        mHilbertBase = new Composite(mDisplayBase, SWT.NONE);
        //mHilbertBase.setLayoutData(new GridData());
        gl = new GridLayout(2, false);
        gl.marginHeight = gl.marginWidth = 0;
        mHilbertBase.setLayout(gl);

        if (DISPLAY_HILBERT_BITMAP) {
            mHilbertHeapImage = new Label(mHilbertBase, SWT.NONE);
            mHilbertHeapImage.setLayoutData(new GridData());
            mHilbertHeapImage.setImage(ImageHelper.createPlaceHolderArt(mDisplay,
                    PLACEHOLDER_HILBERT_SIZE, PLACEHOLDER_HILBERT_SIZE,
                    mDisplay.getSystemColor(SWT.COLOR_BLUE)));

            // create a composite to contain the right part (legend + zoom)
            Composite rightSection = new Composite(mHilbertBase, SWT.NONE);
            gl = new GridLayout(1, false);
            gl.marginHeight = gl.marginWidth = 0;
            rightSection.setLayout(gl);

            Composite zoomComposite = new Composite(rightSection, SWT.NONE);
            gl = new GridLayout(2, false);
            zoomComposite.setLayout(gl);

            Label l = new Label(zoomComposite, SWT.NONE);
            l.setText("Zoom:");
            mZoom = new Combo(zoomComposite, SWT.READ_ONLY);
            for (int z : ZOOMS) {
                mZoom.add(String.format("%1$d%%", z)); // $NON-NLS-1$
            }

            mZoom.select(0);
            mZoom.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setLegendText(mZoom.getSelectionIndex());
                    Client client = getCurrentClient();
                    if (client != null) {
                        renderHeapData(client.getClientData(), 1, true);
                        mTop.pack();
                    }
                }
            });

            createLegend(rightSection);
        }
        mHilbertBase.pack();

        mLayout = new Composite[] { mStatisticsBase, mLinearBase, mHilbertBase };
        mDisplayMode.select(0);
        mDisplayMode.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = mDisplayMode.getSelectionIndex();
                Client client = getCurrentClient();

                if (client != null) {
                    if (index == 0) {
                        fillDetailedTable(client, true /* forceRedraw */);
                    } else {
                        renderHeapData(client.getClientData(), index-1, true /* forceRedraw */);
                    }
                }

                mDisplayStack.topControl = mLayout[index];
                //mScrolledComposite.setMinSize(mDisplayStack.topControl.computeSize(SWT.DEFAULT,
                //        SWT.DEFAULT));
                mDisplayBase.layout();
                //mScrolledComposite.redraw();
            }
        });

        //mScrolledComposite.setMinSize(mDisplayStack.topControl.computeSize(SWT.DEFAULT,
        //        SWT.DEFAULT));
        mDisplayBase.layout();
        //mScrolledComposite.redraw();

        return mTop;
    }

    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mHeapSummary.setFocus();
    }
    

    private Table createSummaryTable(Composite base) {
        Table tab = new Table(base, SWT.SINGLE | SWT.FULL_SELECTION);
        tab.setHeaderVisible(true);
        tab.setLinesVisible(true);

        TableColumn col;

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("ID");
        col.pack();

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("000.000WW"); // $NON-NLS-1$
        col.pack();
        col.setText("Heap Size");

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("000.000WW"); // $NON-NLS-1$
        col.pack();
        col.setText("Allocated");

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("000.000WW"); // $NON-NLS-1$
        col.pack();
        col.setText("Free");

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("000.00%"); // $NON-NLS-1$
        col.pack();
        col.setText("% Used");

        col = new TableColumn(tab, SWT.RIGHT);
        col.setText("000,000,000"); // $NON-NLS-1$
        col.pack();
        col.setText("# Objects");

        return tab;
    }
    
    private Table createDetailedTable(Composite base) {
        IPreferenceStore store = DdmUiPreferences.getStore();
        
        Table tab = new Table(base, SWT.SINGLE | SWT.FULL_SELECTION);
        tab.setHeaderVisible(true);
        tab.setLinesVisible(true);

        TableHelper.createTableColumn(tab, "Type", SWT.LEFT,
                "4-byte array (object[], int[], float[])", //$NON-NLS-1$
                PREFS_STATS_COL_TYPE, store);

        TableHelper.createTableColumn(tab, "Count", SWT.RIGHT,
                "00,000", //$NON-NLS-1$
                PREFS_STATS_COL_COUNT, store);

        TableHelper.createTableColumn(tab, "Total Size", SWT.RIGHT,
                "000.000 WW", //$NON-NLS-1$
                PREFS_STATS_COL_SIZE, store);

        TableHelper.createTableColumn(tab, "Smallest", SWT.RIGHT,
                "000.000 WW", //$NON-NLS-1$
                PREFS_STATS_COL_SMALLEST, store);

        TableHelper.createTableColumn(tab, "Largest", SWT.RIGHT,
                "000.000 WW", //$NON-NLS-1$
                PREFS_STATS_COL_LARGEST, store);

        TableHelper.createTableColumn(tab, "Median", SWT.RIGHT,
                "000.000 WW", //$NON-NLS-1$
                PREFS_STATS_COL_MEDIAN, store);

        TableHelper.createTableColumn(tab, "Average", SWT.RIGHT,
                "000.000 WW", //$NON-NLS-1$
                PREFS_STATS_COL_AVERAGE, store);

        tab.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                
                Client client = getCurrentClient();
                if (client != null) {
                    int index = mStatisticsTable.getSelectionIndex();
                    TableItem item = mStatisticsTable.getItem(index);
                    
                    if (item != null) {
                        Map<Integer, ArrayList<HeapSegmentElement>> heapMap =
                            client.getClientData().getVmHeapData().getProcessedHeapMap();
                        
                        ArrayList<HeapSegmentElement> list = heapMap.get(item.getData());
                        if (list != null) {
                            showChart(list);
                        }
                    }
                }

            }
        });
        
        return tab;
    }
    
    /**
     * Creates the chart below the statistics table
     */
    private void createChart() {
        mAllocCountDataSet = new DefaultCategoryDataset();
        mChart = ChartFactory.createBarChart(null, "Size", "Count", mAllocCountDataSet,
                PlotOrientation.VERTICAL, false, true, false);
        
        // get the font to make a proper title. We need to convert the swt font,
        // into an awt font.
        Font f = mStatisticsBase.getFont();
        FontData[] fData = f.getFontData();
        
        // event though on Mac OS there could be more than one fontData, we'll only use
        // the first one.
        FontData firstFontData = fData[0];
        
        java.awt.Font awtFont = SWTUtils.toAwtFont(mStatisticsBase.getDisplay(),
                firstFontData, true /* ensureSameSize */);

        mChart.setTitle(new TextTitle("Allocation count per size", awtFont));
        
        Plot plot = mChart.getPlot();
        if (plot instanceof CategoryPlot) {
            // get the plot
            CategoryPlot categoryPlot = (CategoryPlot)plot;
            
            // set the domain axis to draw labels that are displayed even with many values.
            CategoryAxis domainAxis = categoryPlot.getDomainAxis();
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
            
            CategoryItemRenderer renderer = categoryPlot.getRenderer();
            renderer.setBaseToolTipGenerator(new CategoryToolTipGenerator() {
                public String generateToolTip(CategoryDataset dataset, int row, int column) {
                    // get the key for the size of the allocation
                    ByteLong columnKey = (ByteLong)dataset.getColumnKey(column);
                    String rowKey = (String)dataset.getRowKey(row);
                    Number value = dataset.getValue(rowKey, columnKey);
                    
                    return String.format("%1$d %2$s of %3$d bytes", value.intValue(), rowKey, 
                            columnKey.getValue());
                }
            });
        }
        mChartComposite = new ChartComposite(mStatisticsBase, SWT.BORDER, mChart,
                ChartComposite.DEFAULT_WIDTH,
                ChartComposite.DEFAULT_HEIGHT,
                ChartComposite.DEFAULT_MINIMUM_DRAW_WIDTH,
                ChartComposite.DEFAULT_MINIMUM_DRAW_HEIGHT,
                3000, // max draw width. We don't want it to zoom, so we put a big number
                3000, // max draw height. We don't want it to zoom, so we put a big number
                true,  // off-screen buffer
                true,  // properties
                true,  // save
                true,  // print
                false,  // zoom
                true);   // tooltips

        mChartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    private static String prettyByteCount(long bytes) {
        double fracBytes = bytes;
        String units = " B";
        if (fracBytes < 1024) {
            return sByteFormatter.format(fracBytes) + units;
        } else {
            fracBytes /= 1024;
            units = " KB";
        }
        if (fracBytes >= 1024) {
            fracBytes /= 1024;
            units = " MB";
        }
        if (fracBytes >= 1024) {
            fracBytes /= 1024;
            units = " GB";
        }

        return sLargeByteFormatter.format(fracBytes) + units;
    }

    private static String approximateByteCount(long bytes) {
        double fracBytes = bytes;
        String units = "";
        if (fracBytes >= 1024) {
            fracBytes /= 1024;
            units = "K";
        }
        if (fracBytes >= 1024) {
            fracBytes /= 1024;
            units = "M";
        }
        if (fracBytes >= 1024) {
            fracBytes /= 1024;
            units = "G";
        }

        return sByteFormatter.format(fracBytes) + units;
    }

    private static String addCommasToNumber(long num) {
        return sCountFormatter.format(num);
    }

    private static String fractionalPercent(long num, long denom) {
        double val = (double)num / (double)denom;
        val *= 100;

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(val) + "%";
    }

    private void fillSummaryTable(ClientData cd) {
        if (mHeapSummary.isDisposed()) {
            return;
        }

        mHeapSummary.setRedraw(false);
        mHeapSummary.removeAll();

        if (cd != null) {
            synchronized (cd) {
                Iterator<Integer> iter = cd.getVmHeapIds();
    
                while (iter.hasNext()) {
                    Integer id = iter.next();
                    Map<String, Long> heapInfo = cd.getVmHeapInfo(id);
                    if (heapInfo == null) {
                        continue;
                    }
                    long sizeInBytes = heapInfo.get(ClientData.HEAP_SIZE_BYTES);
                    long bytesAllocated = heapInfo.get(ClientData.HEAP_BYTES_ALLOCATED);
                    long objectsAllocated = heapInfo.get(ClientData.HEAP_OBJECTS_ALLOCATED);
    
                    TableItem item = new TableItem(mHeapSummary, SWT.NONE);
                    item.setText(0, id.toString());
    
                    item.setText(1, prettyByteCount(sizeInBytes));
                    item.setText(2, prettyByteCount(bytesAllocated));
                    item.setText(3, prettyByteCount(sizeInBytes - bytesAllocated));
                    item.setText(4, fractionalPercent(bytesAllocated, sizeInBytes));
                    item.setText(5, addCommasToNumber(objectsAllocated));
                }
            }
        }

        mHeapSummary.pack();
        mHeapSummary.setRedraw(true);
    }
    
    private void fillDetailedTable(Client client, boolean forceRedraw) {
        // first check if the client is invalid or heap updates are not enabled.
        if (client == null || client.isHeapUpdateEnabled() == false) {
            mStatisticsTable.removeAll();
            showChart(null);
            return;
        }
        
        ClientData cd = client.getClientData();

        Map<Integer, ArrayList<HeapSegmentElement>> heapMap;

        // Atomically get and clear the heap data.
        synchronized (cd) {
            if (serializeHeapData(cd.getVmHeapData()) == false && forceRedraw == false) {
                // no change, we return.
                return;
            }
            
            heapMap = cd.getVmHeapData().getProcessedHeapMap();
        }
        
        // we have new data, lets display it.
        
        // First, get the current selection, and its key.
        int index = mStatisticsTable.getSelectionIndex();
        Integer selectedKey = null;
        if (index != -1) {
            selectedKey = (Integer)mStatisticsTable.getItem(index).getData();
        }

        // disable redraws and remove all from the table.
        mStatisticsTable.setRedraw(false);
        mStatisticsTable.removeAll();
        
        if (heapMap != null) {
            int selectedIndex = -1;
            ArrayList<HeapSegmentElement> selectedList = null;
            
            // get the keys
            Set<Integer> keys = heapMap.keySet();
            int iter = 0; // use a manual iter int because Set<?> doesn't have an index
            // based accessor.
            for (Integer key : keys) {
                ArrayList<HeapSegmentElement> list = heapMap.get(key);

                // check if this is the key that is supposed to be selected
                if (key.equals(selectedKey)) {
                    selectedIndex = iter;
                    selectedList = list;
                }
                iter++;

                TableItem item = new TableItem(mStatisticsTable, SWT.NONE);
                item.setData(key);

                // get the type
                item.setText(0, mMapLegend[key]);
                
                // set the count, smallest, largest
                int count = list.size();
                item.setText(1, addCommasToNumber(count));
                
                if (count > 0) {
                    item.setText(3, prettyByteCount(list.get(0).getLength()));
                    item.setText(4, prettyByteCount(list.get(count-1).getLength()));

                    int median = count / 2;
                    HeapSegmentElement element = list.get(median);
                    long size = element.getLength();
                    item.setText(5, prettyByteCount(size));

                    long totalSize = 0;
                    for (int i = 0 ; i < count; i++) {
                        element = list.get(i);
                        
                        size = element.getLength();
                        totalSize += size;
                    }
                    
                    // set the average and total
                    item.setText(2, prettyByteCount(totalSize));
                    item.setText(6, prettyByteCount(totalSize / count));
                }
            }

            mStatisticsTable.setRedraw(true);
            
            if (selectedIndex != -1) {
                mStatisticsTable.setSelection(selectedIndex);
                showChart(selectedList);
            } else {
                showChart(null);
            }
        } else {
            mStatisticsTable.setRedraw(true);
        }
    }
    
    private static class ByteLong implements Comparable<ByteLong> {
        private long mValue;
        
        private ByteLong(long value) {
            mValue = value;
        }
        
        public long getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return approximateByteCount(mValue);
        }

        public int compareTo(ByteLong other) {
            if (mValue != other.mValue) {
                return mValue < other.mValue ? -1 : 1;
            }
            return 0;
        }
        
    }
    
    /**
     * Fills the chart with the content of the list of {@link HeapSegmentElement}.
     */
    private void showChart(ArrayList<HeapSegmentElement> list) {
        mAllocCountDataSet.clear();

        if (list != null) {
            String rowKey = "Alloc Count";
    
            long currentSize = -1;
            int currentCount = 0;
            for (HeapSegmentElement element : list) {
                if (element.getLength() != currentSize) {
                    if (currentSize != -1) {
                        ByteLong columnKey = new ByteLong(currentSize);
                        mAllocCountDataSet.addValue(currentCount, rowKey, columnKey);
                    }
                    
                    currentSize = element.getLength();
                    currentCount = 1;
                } else {
                    currentCount++;
                }
            }
            
            // add the last item
            if (currentSize != -1) {
                ByteLong columnKey = new ByteLong(currentSize);
                mAllocCountDataSet.addValue(currentCount, rowKey, columnKey);
            }
        }
    }

    /*
     * Add a color legend to the specified table.
     */
    private void createLegend(Composite parent) {
        mLegend = new Group(parent, SWT.NONE);
        mLegend.setText(getLegendText(0));

        mLegend.setLayout(new GridLayout(2, false));

        RGB[] colors = mMapPalette.colors;

        for (int i = 0; i < NUM_PALETTE_ENTRIES; i++) {
            Image tmpImage = createColorRect(parent.getDisplay(), colors[i]);

            Label l = new Label(mLegend, SWT.NONE);
            l.setImage(tmpImage);

            l = new Label(mLegend, SWT.NONE);
            l.setText(mMapLegend[i]);
        }
    }

    private String getLegendText(int level) {
        int bytes = 8 * (100 / ZOOMS[level]);

        return String.format("Key (1 pixel = %1$d bytes)", bytes);
    }

    private void setLegendText(int level) {
        mLegend.setText(getLegendText(level));

    }

    /*
     * Create a nice rectangle in the specified color.
     */
    private Image createColorRect(Display display, RGB color) {
        int width = 32;
        int height = 16;

        Image img = new Image(display, width, height);
        GC gc = new GC(img);
        gc.setBackground(new Color(display, color));
        gc.fillRectangle(0, 0, width, height);
        gc.dispose();
        return img;
    }


    /*
     * Are updates enabled?
     */
    private void setUpdateStatus(int status) {
        switch (status) {
            case NOT_SELECTED:
                mUpdateStatus.setText("Select a client to see heap updates");
                break;
            case NOT_ENABLED:
                mUpdateStatus.setText("Heap updates are " +
                                      "NOT ENABLED for this client");
                break;
            case ENABLED:
                mUpdateStatus.setText("Heap updates will happen after " +
                                      "every GC for this client");
                break;
            default:
                throw new RuntimeException();
        }

        mUpdateStatus.pack();
    }


    /**
     * Return the closest power of two greater than or equal to value.
     *
     * @param value the return value will be >= value
     * @return a power of two >= value.  If value > 2^31, 2^31 is returned.
     */
//xxx use Integer.highestOneBit() or numberOfLeadingZeros().
    private int nextPow2(int value) {
        for (int i = 31; i >= 0; --i) {
            if ((value & (1<<i)) != 0) {
                if (i < 31) {
                    return 1<<(i + 1);
                } else {
                    return 1<<31;
                }
            }
        }
        return 0;
    }

    private int zOrderData(ImageData id, byte pixData[]) {
        int maxX = 0;
        for (int i = 0; i < pixData.length; i++) {
            /* Tread the pixData index as a z-order curve index and
             * decompose into Cartesian coordinates.
             */
            int x = (i & 1) |
                    ((i >>> 2) & 1) << 1 |
                    ((i >>> 4) & 1) << 2 |
                    ((i >>> 6) & 1) << 3 |
                    ((i >>> 8) & 1) << 4 |
                    ((i >>> 10) & 1) << 5 |
                    ((i >>> 12) & 1) << 6 |
                    ((i >>> 14) & 1) << 7 |
                    ((i >>> 16) & 1) << 8 |
                    ((i >>> 18) & 1) << 9 |
                    ((i >>> 20) & 1) << 10 |
                    ((i >>> 22) & 1) << 11 |
                    ((i >>> 24) & 1) << 12 |
                    ((i >>> 26) & 1) << 13 |
                    ((i >>> 28) & 1) << 14 |
                    ((i >>> 30) & 1) << 15;
            int y = ((i >>> 1) & 1) << 0 |
                    ((i >>> 3) & 1) << 1 |
                    ((i >>> 5) & 1) << 2 |
                    ((i >>> 7) & 1) << 3 |
                    ((i >>> 9) & 1) << 4 |
                    ((i >>> 11) & 1) << 5 |
                    ((i >>> 13) & 1) << 6 |
                    ((i >>> 15) & 1) << 7 |
                    ((i >>> 17) & 1) << 8 |
                    ((i >>> 19) & 1) << 9 |
                    ((i >>> 21) & 1) << 10 |
                    ((i >>> 23) & 1) << 11 |
                    ((i >>> 25) & 1) << 12 |
                    ((i >>> 27) & 1) << 13 |
                    ((i >>> 29) & 1) << 14 |
                    ((i >>> 31) & 1) << 15;
            try {
                id.setPixel(x, y, pixData[i]);
                if (x > maxX) {
                    maxX = x;
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("bad pixels: i " + i +
                        ", w " + id.width +
                        ", h " + id.height +
                        ", x " + x +
                        ", y " + y);
                throw ex;
            }
        }
        return maxX;
    }

    private final static int HILBERT_DIR_N = 0;
    private final static int HILBERT_DIR_S = 1;
    private final static int HILBERT_DIR_E = 2;
    private final static int HILBERT_DIR_W = 3;

    private void hilbertWalk(ImageData id, InputStream pixData,
                             int order, int x, int y, int dir)
                             throws IOException {
        if (x >= id.width || y >= id.height) {
            return;
        } else if (order == 0) {
            try {
                int p = pixData.read();
                if (p >= 0) {
                    // flip along x=y axis;  assume width == height
                    id.setPixel(y, x, p);

                    /* Skanky; use an otherwise-unused ImageData field
                     * to keep track of the max x,y used. Note that x and y are inverted.
                     */
                    if (y > id.x) {
                        id.x = y;
                    }
                    if (x > id.y) {
                        id.y = x;
                    }
                }
//xxx just give up; don't bother walking the rest of the image
            } catch (IllegalArgumentException ex) {
                System.out.println("bad pixels: order " + order +
                        ", dir " + dir +
                        ", w " + id.width +
                        ", h " + id.height +
                        ", x " + x +
                        ", y " + y);
                throw ex;
            }
        } else {
            order--;
            int delta = 1 << order;
            int nextX = x + delta;
            int nextY = y + delta;

            switch (dir) {
            case HILBERT_DIR_E:
                hilbertWalk(id, pixData, order,     x,     y, HILBERT_DIR_N);
                hilbertWalk(id, pixData, order,     x, nextY, HILBERT_DIR_E);
                hilbertWalk(id, pixData, order, nextX, nextY, HILBERT_DIR_E);
                hilbertWalk(id, pixData, order, nextX,     y, HILBERT_DIR_S);
                break;
            case HILBERT_DIR_N:
                hilbertWalk(id, pixData, order,     x,     y, HILBERT_DIR_E);
                hilbertWalk(id, pixData, order, nextX,     y, HILBERT_DIR_N);
                hilbertWalk(id, pixData, order, nextX, nextY, HILBERT_DIR_N);
                hilbertWalk(id, pixData, order,     x, nextY, HILBERT_DIR_W);
                break;
            case HILBERT_DIR_S:
                hilbertWalk(id, pixData, order, nextX, nextY, HILBERT_DIR_W);
                hilbertWalk(id, pixData, order,     x, nextY, HILBERT_DIR_S);
                hilbertWalk(id, pixData, order,     x,     y, HILBERT_DIR_S);
                hilbertWalk(id, pixData, order, nextX,     y, HILBERT_DIR_E);
                break;
            case HILBERT_DIR_W:
                hilbertWalk(id, pixData, order, nextX, nextY, HILBERT_DIR_S);
                hilbertWalk(id, pixData, order, nextX,     y, HILBERT_DIR_W);
                hilbertWalk(id, pixData, order,     x,     y, HILBERT_DIR_W);
                hilbertWalk(id, pixData, order,     x, nextY, HILBERT_DIR_N);
                break;
            default:
                throw new RuntimeException("Unexpected Hilbert direction " +
                                           dir);
            }
        }
    }

    private Point hilbertOrderData(ImageData id, byte pixData[]) {

        int order = 0;
        for (int n = 1; n < id.width; n *= 2) {
            order++;
        }
        /* Skanky; use an otherwise-unused ImageData field
         * to keep track of maxX.
         */
        Point p = new Point(0,0);
        int oldIdX = id.x;
        int oldIdY = id.y;
        id.x = id.y = 0;
        try {
            hilbertWalk(id, new ByteArrayInputStream(pixData),
                        order, 0, 0, HILBERT_DIR_E);
            p.x = id.x;
            p.y = id.y;
        } catch (IOException ex) {
            System.err.println("Exception during hilbertWalk()");
            p.x = id.height;
            p.y = id.width;
        }
        id.x = oldIdX;
        id.y = oldIdY;
        return p;
    }

    private ImageData createHilbertHeapImage(byte pixData[]) {
        int w, h;

        // Pick an image size that the largest of heaps will fit into.
        w = (int)Math.sqrt((double)((16 * 1024 * 1024)/8));

        // Space-filling curves require a power-of-2 width.
        w = nextPow2(w);
        h = w;

        // Create the heap image.
        ImageData id = new ImageData(w, h, 8, mMapPalette);

        // Copy the data into the image
        //int maxX = zOrderData(id, pixData);
        Point maxP = hilbertOrderData(id, pixData);

        // update the max size to make it a round number once the zoom is applied
        int factor = 100 / ZOOMS[mZoom.getSelectionIndex()];
        if (factor != 1) {
            int tmp = maxP.x % factor;
            if (tmp != 0) {
                maxP.x += factor - tmp;
            }

            tmp = maxP.y % factor;
            if (tmp != 0) {
                maxP.y += factor - tmp;
            }
        }

        if (maxP.y < id.height) {
            // Crop the image down to the interesting part.
            id = new ImageData(id.width, maxP.y, id.depth, id.palette,
                               id.scanlinePad, id.data);
        }

        if (maxP.x < id.width) {
            // crop the image again. A bit trickier this time.
           ImageData croppedId = new ImageData(maxP.x, id.height, id.depth, id.palette);

           int[] buffer = new int[maxP.x];
           for (int l = 0 ; l < id.height; l++) {
               id.getPixels(0, l, maxP.x, buffer, 0);
               croppedId.setPixels(0, l, maxP.x, buffer, 0);
           }

           id = croppedId;
        }

        // apply the zoom
        if (factor != 1) {
            id = id.scaledTo(id.width / factor, id.height / factor);
        }

        return id;
    }

    /**
     * Convert the raw heap data to an image.  We know we're running in
     * the UI thread, so we can issue graphics commands directly.
     *
     * http://help.eclipse.org/help31/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/swt/graphics/GC.html
     *
     * @param cd The client data
     * @param mode The display mode. 0 = linear, 1 = hilbert.
     * @param forceRedraw
     */
    private void renderHeapData(ClientData cd, int mode, boolean forceRedraw) {
        Image image;

        byte[] pixData;

        // Atomically get and clear the heap data.
        synchronized (cd) {
            if (serializeHeapData(cd.getVmHeapData()) == false && forceRedraw == false) {
                // no change, we return.
                return;
            }

            pixData = getSerializedData();
        }

        if (pixData != null) {
            ImageData id;
            if (mode == 1) {
                id = createHilbertHeapImage(pixData);
            } else {
                id = createLinearHeapImage(pixData, 200, mMapPalette);
            }

            image = new Image(mDisplay, id);
        } else {
            // Render a placeholder image.
            int width, height;
            if (mode == 1) {
                width = height = PLACEHOLDER_HILBERT_SIZE;
            } else {
                width = PLACEHOLDER_LINEAR_H_SIZE;
                height = PLACEHOLDER_LINEAR_V_SIZE;
            }
            image = new Image(mDisplay, width, height);
            GC gc = new GC(image);
            gc.setForeground(mDisplay.getSystemColor(SWT.COLOR_RED));
            gc.drawLine(0, 0, width-1, height-1);
            gc.dispose();
            gc = null;
        }

        // set the new image

        if (mode == 1) {
            if (mHilbertImage != null) {
                mHilbertImage.dispose();
            }

            mHilbertImage = image;
            mHilbertHeapImage.setImage(mHilbertImage);
            mHilbertHeapImage.pack(true);
            mHilbertBase.layout();
            mHilbertBase.pack(true);
        } else {
            if (mLinearImage != null) {
                mLinearImage.dispose();
            }

            mLinearImage = image;
            mLinearHeapImage.setImage(mLinearImage);
            mLinearHeapImage.pack(true);
            mLinearBase.layout();
            mLinearBase.pack(true);
        }
    }

    @Override
    protected void setTableFocusListener() {
        addTableToFocusListener(mHeapSummary);
    }
}

