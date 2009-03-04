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

import com.android.ddmlib.HeapSegment;
import com.android.ddmlib.ClientData.HeapData;
import com.android.ddmlib.HeapSegment.HeapSegmentElement;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * Base Panel for heap panels.
 */
public abstract class BaseHeapPanel extends TablePanel {

    /** store the processed heap segment, so that we don't recompute Image for nothing */
    protected byte[] mProcessedHeapData;
    private Map<Integer, ArrayList<HeapSegmentElement>> mHeapMap;

    /**
     * Serialize the heap data into an array. The resulting array is available through
     * <code>getSerializedData()</code>.
     * @param heapData The heap data to serialize
     * @return true if the data changed.
     */
    protected boolean serializeHeapData(HeapData heapData) {
        Collection<HeapSegment> heapSegments;

        // Atomically get and clear the heap data.
        synchronized (heapData) {
            // get the segments
            heapSegments = heapData.getHeapSegments();
            
            
            if (heapSegments != null) {
                // if they are not null, we never processed them.
                // Before we process then, we drop them from the HeapData
                heapData.clearHeapData();

                // process them into a linear byte[]
                doSerializeHeapData(heapSegments);
                heapData.setProcessedHeapData(mProcessedHeapData);
                heapData.setProcessedHeapMap(mHeapMap);
                
            } else {
                // the heap segments are null. Let see if the heapData contains a 
                // list that is already processed.
                
                byte[] pixData = heapData.getProcessedHeapData();
                
                // and compare it to the one we currently have in the panel.
                if (pixData == mProcessedHeapData) {
                    // looks like its the same
                    return false;
                } else {
                    mProcessedHeapData = pixData;
                }
                
                Map<Integer, ArrayList<HeapSegmentElement>> heapMap =
                    heapData.getProcessedHeapMap();
                mHeapMap = heapMap;
            }
        }

        return true;
    }

    /**
     * Returns the serialized heap data
     */
    protected byte[] getSerializedData() {
        return mProcessedHeapData;
    }

    /**
     * Processes and serialize the heapData.
     * <p/>
     * The resulting serialized array is {@link #mProcessedHeapData}.
     * <p/>
     * the resulting map is {@link #mHeapMap}.
     * @param heapData the collection of {@link HeapSegment} that forms the heap data.
     */
    private void doSerializeHeapData(Collection<HeapSegment> heapData) {
        mHeapMap = new TreeMap<Integer, ArrayList<HeapSegmentElement>>();

        Iterator<HeapSegment> iterator;
        ByteArrayOutputStream out;

        out = new ByteArrayOutputStream(4 * 1024);

        iterator = heapData.iterator();
        while (iterator.hasNext()) {
            HeapSegment hs = iterator.next();

            HeapSegmentElement e = null;
            while (true) {
                int v;

                e = hs.getNextElement(null);
                if (e == null) {
                    break;
                }
                
                if (e.getSolidity() == HeapSegmentElement.SOLIDITY_FREE) {
                    v = 1;
                } else {
                    v = e.getKind() + 2;
                }
                
                // put the element in the map
                ArrayList<HeapSegmentElement> elementList = mHeapMap.get(v);
                if (elementList == null) {
                    elementList = new ArrayList<HeapSegmentElement>();
                    mHeapMap.put(v, elementList);
                }
                elementList.add(e);


                int len = e.getLength() / 8;
                while (len > 0) {
                    out.write(v);
                    --len;
                }
            }
        }
        mProcessedHeapData = out.toByteArray();
        
        // sort the segment element in the heap info.
        Collection<ArrayList<HeapSegmentElement>> elementLists = mHeapMap.values();
        for (ArrayList<HeapSegmentElement> elementList : elementLists) {
            Collections.sort(elementList);
        }
    }
    
    /**
     * Creates a linear image of the heap data.
     * @param pixData
     * @param h
     * @param palette
     * @return
     */
    protected ImageData createLinearHeapImage(byte[] pixData, int h, PaletteData palette) {
        int w = pixData.length / h;
        if (pixData.length % h != 0) {
            w++;
        }

        // Create the heap image.
        ImageData id = new ImageData(w, h, 8, palette);

        int x = 0;
        int y = 0;
        for (byte b : pixData) {
            if (b >= 0) {
                id.setPixel(x, y, b);
            }

            y++;
            if (y >= h) {
                y = 0;
                x++;
            }
        }

        return id;
    }


}
