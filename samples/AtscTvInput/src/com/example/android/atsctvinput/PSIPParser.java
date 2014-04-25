/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.atsctvinput;

import android.util.Log;

import com.example.android.atsctvinput.SectionParser.EITItem;
import com.example.android.atsctvinput.SectionParser.MGTItem;
import com.example.android.atsctvinput.SectionParser.OutputListener;
import com.example.android.atsctvinput.SectionParser.VCTItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ATSC Program and System Information Protocol (PSIP) parser.
 */
public class PSIPParser {
    private static final String TAG = "PSIPParser";

    public static final int ATSC_SI_BASE_PID = 0x1ffb;
    private static final int TS_PACKET_START_CODE = 0x47;
    private static final int TS_PACKET_SIZE = 188;

    private Map<Integer, Stream> mStreamMap = new HashMap<Integer, Stream>();
    private Map<Integer, VCTItem> mSourceIdToVCTItemMap = new HashMap<Integer, VCTItem>();
    private SectionParser mSectionParser;
    private PSIPOutputListener mListener;

    private int mPartialTSPacketSize;
    private byte[] mPartialTSPacketBuf = new byte[TS_PACKET_SIZE];

    public interface PSIPOutputListener {
        void onEITPidDetected(int pid);
        void onEITItemParsed(VCTItem channel, List<EITItem> items);
    }

    private class Stream {
        private static final int INVALID_CONTINUITY_COUNTER = -1;
        private static final int NUM_CONTINUITY_COUNTER = 16;

        int mContinuityCounter = INVALID_CONTINUITY_COUNTER;
        byte[] mData = new byte[0];

        public void feedData(byte[] data, int continuityCounter, boolean startIndicator) {
            if ((mContinuityCounter + 1) % NUM_CONTINUITY_COUNTER
                    != continuityCounter) {
                mData = new byte[0];
            }
            mContinuityCounter = continuityCounter;
            int startPos = 0;
            if (mData.length == 0) {
                if (startIndicator) {
                    startPos = (data[0] & 0xff) + 1;
                } else {
                    // Don't know where the section starts yet. Wait until start indicator is on.
                    return;
                }
            } else {
                if (startIndicator) {
                    startPos = 1;
                }
            }
            int prevSize = mData.length;
            mData = Arrays.copyOf(mData, mData.length + data.length - startPos);
            System.arraycopy(data, startPos, mData, prevSize, data.length - startPos);
            parseSectionIfAny();
        }

        private void parseSectionIfAny() {
            while (mData.length >= 3) {
                if ((mData[0] & 0xff) == 0xff) {
                    // Clear stuffing bytes according to H222.0 section 2.4.4.
                    mData = new byte[0];
                    break;
                }
                int sectionLength = (((mData[1] & 0x0f) << 8) | (mData[2] & 0xff) + 3);
                if (mData.length < sectionLength) {
                    break;
                }
                Log.d(TAG, "parseSection 0x" + Integer.toHexString(mData[0] & 0xff));
                parseSection(Arrays.copyOfRange(mData, 0, sectionLength));
                mData = Arrays.copyOfRange(mData, sectionLength, mData.length);
            }
        }
    }

    private OutputListener mSectionListener = new OutputListener() {
        @Override
        public void onMGTParsed(List<MGTItem> items) {
            for (MGTItem i : items) {
                if (i.getTableType() == MGTItem.TABLE_TYPE_EIT_0
                        && mStreamMap.get(i.getTableTypePid()) == null) {
                    mStreamMap.put(i.getTableTypePid(), new Stream());
                    if (mListener != null) {
                        mListener.onEITPidDetected(i.getTableTypePid());
                    }
                }
            }
        }

        @Override
        public void onVCTParsed(List<VCTItem> items) {
            for (VCTItem i : items) {
                mSourceIdToVCTItemMap.put(i.getSourceId(), i);
            }
        }

        @Override
        public void onEITParsed(int sourceId, List<EITItem> items) {
            Log.d(TAG, "onEITParsed " + sourceId);
            VCTItem channel = mSourceIdToVCTItemMap.get(sourceId);
            if (channel != null && mListener != null) {
                mListener.onEITItemParsed(channel, items);
            }
        }
    };

    public PSIPParser(PSIPOutputListener listener) {
        mSectionParser = new SectionParser(mSectionListener);
        mStreamMap.put(ATSC_SI_BASE_PID, new Stream());
        mListener = listener;
    }

    private boolean feedTSPacket(byte[] tsData, int pos) {
        if (tsData.length < pos + TS_PACKET_SIZE) {
            Log.d(TAG, "Data should include a single TS packet.");
            return false;
        }
        if (tsData[pos + 0] != TS_PACKET_START_CODE) {
            Log.d(TAG, "Invalid ts packet.");
            return false;
        }
        if ((tsData[pos + 1] & 0x80) != 0) {
            Log.d(TAG, "Erroneous ts packet.");
            return false;
        }
        // For details for the structire of TS packet, please see H.222.0 Table 2-2.
        int pid = ((tsData[pos + 1] & 0x1f) << 8) | (tsData[pos + 2] & 0xff);
        boolean hasAdaptation = (tsData[pos + 3] & 0x20) != 0;
        boolean hasPayload = (tsData[pos + 3] & 0x10) != 0;
        boolean payloadStartIndicator = (tsData[pos + 1] & 0x40) != 0;
        int continuityCounter = tsData[pos + 3] & 0x0f;
        Stream stream = mStreamMap.get(pid);
        int payloadPos = pos;
        payloadPos += hasAdaptation ? 5 + (tsData[pos + 4] & 0xff) : 4;
        if (!hasPayload || stream == null) {
            // We are not interested in this packet.
            return false;
        }
        stream.feedData(Arrays.copyOfRange(tsData, payloadPos, pos + TS_PACKET_SIZE),
                continuityCounter, payloadStartIndicator);
        return true;
    }

    public void feedTSData(byte[] tsData, int pos, int length) {
        int origPos = pos;
        if (mPartialTSPacketSize != 0
                && (mPartialTSPacketSize + length) > TS_PACKET_SIZE) {
            System.arraycopy(tsData, pos, mPartialTSPacketBuf, mPartialTSPacketSize,
                    TS_PACKET_SIZE - mPartialTSPacketSize);
            feedTSPacket(mPartialTSPacketBuf, 0);
            pos += TS_PACKET_SIZE - mPartialTSPacketSize;
            mPartialTSPacketSize = 0;
        }
        for (;pos <= length - TS_PACKET_SIZE; pos += TS_PACKET_SIZE) {
            feedTSPacket(tsData, pos);
        }
        int remaining = origPos + length - pos;
        if (remaining > 0) {
            System.arraycopy(tsData, pos, mPartialTSPacketBuf, mPartialTSPacketSize, remaining);
        }
    }

    private void parseSection(byte[] data) {
        mSectionParser.parseSection(data);
    }
}
