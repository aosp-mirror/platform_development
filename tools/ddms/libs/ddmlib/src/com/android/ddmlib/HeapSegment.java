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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;

/**
 * Describes the types and locations of objects in a segment of a heap.
 */
public final class HeapSegment implements Comparable<HeapSegment> {

    /**
     * Describes an object/region encoded in the HPSG data.
     */
    public static class HeapSegmentElement implements Comparable<HeapSegmentElement> {

        /*
         * Solidity values, which must match the values in
         * the HPSG data.
         */

        /** The element describes a free block. */
        public static int SOLIDITY_FREE = 0;

        /** The element is strongly-reachable. */
        public static int SOLIDITY_HARD = 1;

        /** The element is softly-reachable. */
        public static int SOLIDITY_SOFT = 2;

        /** The element is weakly-reachable. */
        public static int SOLIDITY_WEAK = 3;

        /** The element is phantom-reachable. */
        public static int SOLIDITY_PHANTOM = 4;

        /** The element is pending finalization. */
        public static int SOLIDITY_FINALIZABLE = 5;

        /** The element is not reachable, and is about to be swept/freed. */
        public static int SOLIDITY_SWEEP = 6;

        /** The reachability of the object is unknown. */
        public static int SOLIDITY_INVALID = -1;


        /*
         * Kind values, which must match the values in
         * the HPSG data.
         */

        /** The element describes a data object. */
        public static int KIND_OBJECT = 0;

        /** The element describes a class object. */
        public static int KIND_CLASS_OBJECT = 1;

        /** The element describes an array of 1-byte elements. */
        public static int KIND_ARRAY_1 = 2;

        /** The element describes an array of 2-byte elements. */
        public static int KIND_ARRAY_2 = 3;

        /** The element describes an array of 4-byte elements. */
        public static int KIND_ARRAY_4 = 4;

        /** The element describes an array of 8-byte elements. */
        public static int KIND_ARRAY_8 = 5;

        /** The element describes an unknown type of object. */
        public static int KIND_UNKNOWN = 6;

        /** The element describes a native object. */
        public static int KIND_NATIVE = 7;

        /** The object kind is unknown or unspecified. */
        public static int KIND_INVALID = -1;


        /**
         * A bit in the HPSG data that indicates that an element should
         * be combined with the element that follows, typically because
         * an element is too large to be described by a single element.
         */
        private static int PARTIAL_MASK = 1 << 7;


        /**
         * Describes the reachability/solidity of the element.  Must
         * be set to one of the SOLIDITY_* values.
         */
        private int mSolidity;

        /**
         * Describes the type/kind of the element.  Must be set to one
         * of the KIND_* values.
         */
        private int mKind;

        /**
         * Describes the length of the element, in bytes.
         */
        private int mLength;


        /**
         * Creates an uninitialized element.
         */
        public HeapSegmentElement() {
            setSolidity(SOLIDITY_INVALID);
            setKind(KIND_INVALID);
            setLength(-1);
        }

        /**
         * Create an element describing the entry at the current
         * position of hpsgData.
         *
         * @param hs The heap segment to pull the entry from.
         * @throws BufferUnderflowException if there is not a whole entry
         *                                  following the current position
         *                                  of hpsgData.
         * @throws ParseException           if the provided data is malformed.
         */
        public HeapSegmentElement(HeapSegment hs)
                throws BufferUnderflowException, ParseException {
            set(hs);
        }

        /**
         * Replace the element with the entry at the current position of
         * hpsgData.
         *
         * @param hs The heap segment to pull the entry from.
         * @return this object.
         * @throws BufferUnderflowException if there is not a whole entry
         *                                  following the current position of
         *                                  hpsgData.
         * @throws ParseException           if the provided data is malformed.
         */
        public HeapSegmentElement set(HeapSegment hs)
                throws BufferUnderflowException, ParseException {

            /* TODO: Maybe keep track of the virtual address of each element
             *       so that they can be examined independently.
             */
            ByteBuffer data = hs.mUsageData;
            int eState = (int)data.get() & 0x000000ff;
            int eLen = ((int)data.get() & 0x000000ff) + 1;

            while ((eState & PARTIAL_MASK) != 0) {

                /* If the partial bit was set, the next byte should describe
                 * the same object as the current one.
                 */
                int nextState = (int)data.get() & 0x000000ff;
                if ((nextState & ~PARTIAL_MASK) != (eState & ~PARTIAL_MASK)) {
                    throw new ParseException("State mismatch", data.position());
                }
                eState = nextState;
                eLen += ((int)data.get() & 0x000000ff) + 1;
            }

            setSolidity(eState & 0x7);
            setKind((eState >> 3) & 0x7);
            setLength(eLen * hs.mAllocationUnitSize);

            return this;
        }

        public int getSolidity() {
            return mSolidity;
        }

        public void setSolidity(int solidity) {
            this.mSolidity = solidity;
        }

        public int getKind() {
            return mKind;
        }

        public void setKind(int kind) {
            this.mKind = kind;
        }

        public int getLength() {
            return mLength;
        }

        public void setLength(int length) {
            this.mLength = length;
        }

        public int compareTo(HeapSegmentElement other) {
            if (mLength != other.mLength) {
                return mLength < other.mLength ? -1 : 1;
            }
            return 0;
        }
    }

    //* The ID of the heap that this segment belongs to.
    protected int mHeapId;

    //* The size of an allocation unit, in bytes. (e.g., 8 bytes)
    protected int mAllocationUnitSize;

    //* The virtual address of the start of this segment.
    protected long mStartAddress;

    //* The offset of this pices from mStartAddress, in bytes.
    protected int mOffset;

    //* The number of allocation units described in this segment.
    protected int mAllocationUnitCount;

    //* The raw data that describes the contents of this segment.
    protected ByteBuffer mUsageData;

    //* mStartAddress is set to this value when the segment becomes invalid.
    private final static long INVALID_START_ADDRESS = -1;

    /**
     * Create a new HeapSegment based on the raw contents
     * of an HPSG chunk.
     *
     * @param hpsgData The raw data from an HPSG chunk.
     * @throws BufferUnderflowException if hpsgData is too small
     *                                  to hold the HPSG chunk header data.
     */
    public HeapSegment(ByteBuffer hpsgData) throws BufferUnderflowException {
        /* Read the HPSG chunk header.
         * These get*() calls may throw a BufferUnderflowException
         * if the underlying data isn't big enough.
         */
        hpsgData.order(ByteOrder.BIG_ENDIAN);
        mHeapId = hpsgData.getInt();
        mAllocationUnitSize = (int) hpsgData.get();
        mStartAddress = (long) hpsgData.getInt() & 0x00000000ffffffffL;
        mOffset = hpsgData.getInt();
        mAllocationUnitCount = hpsgData.getInt();

        // Hold onto the remainder of the data.
        mUsageData = hpsgData.slice();
        mUsageData.order(ByteOrder.BIG_ENDIAN);   // doesn't actually matter

        // Validate the data.
//xxx do it
//xxx make sure the number of elements matches mAllocationUnitCount.
//xxx make sure the last element doesn't have P set
    }

    /**
     * See if this segment still contains data, and has not been
     * appended to another segment.
     *
     * @return true if this segment has not been appended to
     *         another segment.
     */
    public boolean isValid() {
        return mStartAddress != INVALID_START_ADDRESS;
    }

    /**
     * See if <code>other</code> comes immediately after this segment.
     *
     * @param other The HeapSegment to check.
     * @return true if <code>other</code> comes immediately after this
     *         segment.
     */
    public boolean canAppend(HeapSegment other) {
        return isValid() && other.isValid() && mHeapId == other.mHeapId &&
                mAllocationUnitSize == other.mAllocationUnitSize &&
                getEndAddress() == other.getStartAddress();
    }

    /**
     * Append the contents of <code>other</code> to this segment
     * if it describes the segment immediately after this one.
     *
     * @param other The segment to append to this segment, if possible.
     *              If appended, <code>other</code> will be invalid
     *              when this method returns.
     * @return true if <code>other</code> was successfully appended to
     *         this segment.
     */
    public boolean append(HeapSegment other) {
        if (canAppend(other)) {
            /* Preserve the position.  The mark is not preserved,
             * but we don't use it anyway.
             */
            int pos = mUsageData.position();

            // Guarantee that we have enough room for the new data.
            if (mUsageData.capacity() - mUsageData.limit() <
                    other.mUsageData.limit()) {
                /* Grow more than necessary in case another append()
                 * is about to happen.
                 */
                int newSize = mUsageData.limit() + other.mUsageData.limit();
                ByteBuffer newData = ByteBuffer.allocate(newSize * 2);

                mUsageData.rewind();
                newData.put(mUsageData);
                mUsageData = newData;
            }

            // Copy the data from the other segment and restore the position.
            other.mUsageData.rewind();
            mUsageData.put(other.mUsageData);
            mUsageData.position(pos);

            // Fix this segment's header to cover the new data.
            mAllocationUnitCount += other.mAllocationUnitCount;

            // Mark the other segment as invalid.
            other.mStartAddress = INVALID_START_ADDRESS;
            other.mUsageData = null;

            return true;
        } else {
            return false;
        }
    }

    public long getStartAddress() {
        return mStartAddress + mOffset;
    }

    public int getLength() {
        return mAllocationUnitSize * mAllocationUnitCount;
    }

    public long getEndAddress() {
        return getStartAddress() + getLength();
    }

    public void rewindElements() {
        if (mUsageData != null) {
            mUsageData.rewind();
        }
    }

    public HeapSegmentElement getNextElement(HeapSegmentElement reuse) {
        try {
            if (reuse != null) {
                return reuse.set(this);
            } else {
                return new HeapSegmentElement(this);
            }
        } catch (BufferUnderflowException ex) {
            /* Normal "end of buffer" situation.
             */
        } catch (ParseException ex) {
            /* Malformed data.
             */
//TODO: we should catch this in the constructor
        }
        return null;
    }

    /*
     * Method overrides for Comparable
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof HeapSegment) {
            return compareTo((HeapSegment) o) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mHeapId * 31 +
                mAllocationUnitSize * 31 +
                (int) mStartAddress * 31 +
                mOffset * 31 +
                mAllocationUnitCount * 31 +
                mUsageData.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("HeapSegment { heap ").append(mHeapId)
                .append(", start 0x")
                .append(Integer.toHexString((int) getStartAddress()))
                .append(", length ").append(getLength())
                .append(" }");

        return str.toString();
    }

    public int compareTo(HeapSegment other) {
        if (mHeapId != other.mHeapId) {
            return mHeapId < other.mHeapId ? -1 : 1;
        }
        if (getStartAddress() != other.getStartAddress()) {
            return getStartAddress() < other.getStartAddress() ? -1 : 1;
        }

        /* If two segments have the same start address, the rest of
         * the fields should be equal.  Go through the motions, though.
         * Note that we re-check the components of getStartAddress()
         * (mStartAddress and mOffset) to make sure that all fields in
         * an equal segment are equal.
         */

        if (mAllocationUnitSize != other.mAllocationUnitSize) {
            return mAllocationUnitSize < other.mAllocationUnitSize ? -1 : 1;
        }
        if (mStartAddress != other.mStartAddress) {
            return mStartAddress < other.mStartAddress ? -1 : 1;
        }
        if (mOffset != other.mOffset) {
            return mOffset < other.mOffset ? -1 : 1;
        }
        if (mAllocationUnitCount != other.mAllocationUnitCount) {
            return mAllocationUnitCount < other.mAllocationUnitCount ? -1 : 1;
        }
        if (mUsageData != other.mUsageData) {
            return mUsageData.compareTo(other.mUsageData);
        }
        return 0;
    }
}
