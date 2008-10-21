/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ddmlib.log;

import com.android.ddmlib.log.EventValueDescription.ValueType;
import com.android.ddmlib.log.LogReceiver.LogEntry;

/**
 * Custom Event Container for the Gc event since this event doesn't simply output data in
 * int or long format, but encodes several values on 4 longs.
 * <p/>
 * The array of {@link EventValueDescription}s parsed from the "event-log-tags" file must
 * be ignored, and instead, the array returned from {@link #getValueDescriptions()} must be used. 
 */
final class GcEventContainer extends EventContainer {
    
    public final static int GC_EVENT_TAG = 20001;

    private String processId;
    private long gcTime;
    private long bytesFreed;
    private long objectsFreed;
    private long actualSize;
    private long allowedSize;
    private long softLimit;
    private long objectsAllocated;
    private long bytesAllocated;
    private long zActualSize;
    private long zAllowedSize;
    private long zObjectsAllocated;
    private long zBytesAllocated;
    private long dlmallocFootprint;
    private long mallinfoTotalAllocatedSpace;
    private long externalLimit;
    private long externalBytesAllocated;

    GcEventContainer(LogEntry entry, int tag, Object data) {
        super(entry, tag, data);
        init(data);
    }

    GcEventContainer(int tag, int pid, int tid, int sec, int nsec, Object data) {
        super(tag, pid, tid, sec, nsec, data);
        init(data);
    }

    /**
     * @param data
     */
    private void init(Object data) {
        if (data instanceof Object[]) {
            Object[] values = (Object[])data;
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Long) {
                    parseDvmHeapInfo((Long)values[i], i);
                }
            }
        }
    }
    
    @Override
    public EventValueType getType() {
        return EventValueType.LIST;
    }

    @Override
    public boolean testValue(int index, Object value, CompareMethod compareMethod)
            throws InvalidTypeException {
        // do a quick easy check on the type.
        if (index == 0) {
            if ((value instanceof String) == false) {
                throw new InvalidTypeException();
            }
        } else if ((value instanceof Long) == false) {
            throw new InvalidTypeException();
        }
        
        switch (compareMethod) {
            case EQUAL_TO:
                if (index == 0) {
                    return processId.equals(value);
                } else {
                    return getValueAsLong(index) == ((Long)value).longValue();
                }
            case LESSER_THAN:
                return getValueAsLong(index) <= ((Long)value).longValue();
            case LESSER_THAN_STRICT:
                return getValueAsLong(index) < ((Long)value).longValue();
            case GREATER_THAN:
                return getValueAsLong(index) >= ((Long)value).longValue();
            case GREATER_THAN_STRICT:
                return getValueAsLong(index) > ((Long)value).longValue();
            case BIT_CHECK:
                return (getValueAsLong(index) & ((Long)value).longValue()) != 0;
        }

        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public Object getValue(int valueIndex) {
        if (valueIndex == 0) {
            return processId;
        }
        
        try {
            return new Long(getValueAsLong(valueIndex));
        } catch (InvalidTypeException e) {
            // this would only happened if valueIndex was 0, which we test above.
        }
        
        return null;
    }

    @Override
    public double getValueAsDouble(int valueIndex) throws InvalidTypeException {
        return (double)getValueAsLong(valueIndex);
    }

    @Override
    public String getValueAsString(int valueIndex) {
        switch (valueIndex) {
            case 0:
                return processId;
            default:
                try {
                    return Long.toString(getValueAsLong(valueIndex));
                } catch (InvalidTypeException e) {
                    // we shouldn't stop there since we test, in this method first.
                }
        }

        throw new ArrayIndexOutOfBoundsException();
    }
    
    /**
     * Returns a custom array of {@link EventValueDescription} since the actual content of this
     * event (list of (long, long) does not match the values encoded into those longs.
     */
    static EventValueDescription[] getValueDescriptions() {
        try {
            return new EventValueDescription[] {
                    new EventValueDescription("Process Name", EventValueType.STRING),
                    new EventValueDescription("GC Time", EventValueType.LONG,
                            ValueType.MILLISECONDS),
                    new EventValueDescription("Freed Objects", EventValueType.LONG,
                            ValueType.OBJECTS),
                    new EventValueDescription("Freed Bytes", EventValueType.LONG, ValueType.BYTES),
                    new EventValueDescription("Soft Limit", EventValueType.LONG, ValueType.BYTES),
                    new EventValueDescription("Actual Size (aggregate)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Allowed Size (aggregate)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Allocated Objects (aggregate)",
                            EventValueType.LONG, ValueType.OBJECTS),
                    new EventValueDescription("Allocated Bytes (aggregate)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Actual Size", EventValueType.LONG, ValueType.BYTES),
                    new EventValueDescription("Allowed Size", EventValueType.LONG, ValueType.BYTES),
                    new EventValueDescription("Allocated Objects", EventValueType.LONG,
                            ValueType.OBJECTS),
                    new EventValueDescription("Allocated Bytes", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Actual Size (zygote)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Allowed Size (zygote)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Allocated Objects (zygote)", EventValueType.LONG,
                            ValueType.OBJECTS),
                    new EventValueDescription("Allocated Bytes (zygote)", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("External Allocation Limit", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("External Bytes Allocated", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("dlmalloc Footprint", EventValueType.LONG,
                            ValueType.BYTES),
                    new EventValueDescription("Malloc Info: Total Allocated Space",
                            EventValueType.LONG, ValueType.BYTES),
                  };
        } catch (InvalidValueTypeException e) {
            // this shouldn't happen since we control manual the EventValueType and the ValueType
            // values. For development purpose, we assert if this happens.
            assert false;
        }

        // this shouldn't happen, but the compiler complains otherwise.
        return null;
    }

    private void parseDvmHeapInfo(long data, int index) {
        switch (index) {
            case 0:
                //    [63   ] Must be zero
                //    [62-24] ASCII process identifier
                //    [23-12] GC time in ms
                //    [11- 0] Bytes freed
                
                gcTime = float12ToInt((int)((data >> 12) & 0xFFFL));
                bytesFreed = float12ToInt((int)(data & 0xFFFL));
                
                // convert the long into an array, in the proper order so that we can convert the
                // first 5 char into a string.
                byte[] dataArray = new byte[8];
                put64bitsToArray(data, dataArray, 0);
                
                // get the name from the string
                processId = new String(dataArray, 0, 5);
                break;
            case 1:
                //    [63-62] 10
                //    [61-60] Reserved; must be zero
                //    [59-48] Objects freed
                //    [47-36] Actual size (current footprint)
                //    [35-24] Allowed size (current hard max)
                //    [23-12] Objects allocated
                //    [11- 0] Bytes allocated
                objectsFreed = float12ToInt((int)((data >> 48) & 0xFFFL));
                actualSize = float12ToInt((int)((data >> 36) & 0xFFFL));
                allowedSize = float12ToInt((int)((data >> 24) & 0xFFFL));
                objectsAllocated = float12ToInt((int)((data >> 12) & 0xFFFL));
                bytesAllocated = float12ToInt((int)(data & 0xFFFL));
                break;
            case 2:
                //    [63-62] 11
                //    [61-60] Reserved; must be zero
                //    [59-48] Soft limit (current soft max)
                //    [47-36] Actual size (current footprint)
                //    [35-24] Allowed size (current hard max)
                //    [23-12] Objects allocated
                //    [11- 0] Bytes allocated
                softLimit = float12ToInt((int)((data >> 48) & 0xFFFL));
                zActualSize = float12ToInt((int)((data >> 36) & 0xFFFL));
                zAllowedSize = float12ToInt((int)((data >> 24) & 0xFFFL));
                zObjectsAllocated = float12ToInt((int)((data >> 12) & 0xFFFL));
                zBytesAllocated = float12ToInt((int)(data & 0xFFFL));
                break;
            case 3:
                //    [63-48] Reserved; must be zero
                //    [47-36] dlmallocFootprint
                //    [35-24] mallinfo: total allocated space
                //    [23-12] External byte limit
                //    [11- 0] External bytes allocated
                dlmallocFootprint = float12ToInt((int)((data >> 36) & 0xFFFL));
                mallinfoTotalAllocatedSpace = float12ToInt((int)((data >> 24) & 0xFFFL));
                externalLimit = float12ToInt((int)((data >> 12) & 0xFFFL));
                externalBytesAllocated = float12ToInt((int)(data & 0xFFFL));
                break;
            default:
                break;
        }
    }
    
    /**
     * Converts a 12 bit float representation into an unsigned int (returned as a long)
     * @param f12
     */
    private static long float12ToInt(int f12) {
        return (f12 & 0x1FF) << ((f12 >>> 9) * 4);
    }
    
    /**
     * puts an unsigned value in an array.
     * @param value The value to put.
     * @param dest the destination array
     * @param offset the offset in the array where to put the value.
     *      Array length must be at least offset + 8
     */
    private static void put64bitsToArray(long value, byte[] dest, int offset) {
        dest[offset + 7] = (byte)(value & 0x00000000000000FFL);
        dest[offset + 6] = (byte)((value & 0x000000000000FF00L) >> 8);
        dest[offset + 5] = (byte)((value & 0x0000000000FF0000L) >> 16);
        dest[offset + 4] = (byte)((value & 0x00000000FF000000L) >> 24);
        dest[offset + 3] = (byte)((value & 0x000000FF00000000L) >> 32);
        dest[offset + 2] = (byte)((value & 0x0000FF0000000000L) >> 40);
        dest[offset + 1] = (byte)((value & 0x00FF000000000000L) >> 48);
        dest[offset + 0] = (byte)((value & 0xFF00000000000000L) >> 56);
    }
    
    /**
     * Returns the long value of the <code>valueIndex</code>-th value.
     * @param valueIndex the index of the value.
     * @throws InvalidTypeException if index is 0 as it is a string value.
     */
    private final long getValueAsLong(int valueIndex) throws InvalidTypeException {
        switch (valueIndex) {
            case 0:
                throw new InvalidTypeException();
            case 1:
                return gcTime;
            case 2:
                return objectsFreed;
            case 3:
                return bytesFreed;
            case 4:
                return softLimit;
            case 5:
                return actualSize;
            case 6:
                return allowedSize;
            case 7:
                return objectsAllocated;
            case 8:
                return bytesAllocated;
            case 9:
                return actualSize - zActualSize;
            case 10:
                return allowedSize - zAllowedSize;
            case 11:
                return objectsAllocated - zObjectsAllocated;
            case 12:
                return bytesAllocated - zBytesAllocated;
            case 13:
               return zActualSize;
            case 14:
                return zAllowedSize;
            case 15:
                return zObjectsAllocated;
            case 16:
                return zBytesAllocated;
            case 17:
                return externalLimit;
            case 18:
                return externalBytesAllocated;
            case 19:
                return dlmallocFootprint;
            case 20:
                return mallinfoTotalAllocatedSpace;
        }

        throw new ArrayIndexOutOfBoundsException();
    }
}
