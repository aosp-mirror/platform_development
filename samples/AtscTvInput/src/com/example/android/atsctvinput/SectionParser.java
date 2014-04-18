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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PSIP section parser which conforms to ATSC A/65.
 */
public class SectionParser {
    private static final String TAG = "SectionParser";

    private static final boolean DEBUG = false;

    private static final byte TABLE_ID_MGT = (byte) 0xc7;
    private static final byte TABLE_ID_TVCT = (byte) 0xc8;
    private static final byte TABLE_ID_CVCT = (byte) 0xc9;
    private static final byte TABLE_ID_EIT = (byte) 0xcb;

    private static final byte COMPRESSION_TYPE_NO_COMPRESSION = (byte) 0x00;
    private static final byte MODE_SELECTED_UNICODE_RANGE_1 = (byte) 0x00;  // 0x0000 - 0x00ff
    private static final byte MODE_UTF16 = (byte) 0x3f;
    private static final int MAX_SHORT_NAME_BYTES = 14;

    /*
     * The following CRC table is from the generated code by the following command.
     * $ python pycrc.py --model crc-32-mpeg --algorithm table-driven --generate c
     * To see the details of pycrc, please visit http://www.tty1.net/pycrc/index_en.html
     */
    private static final int [] CRC_TABLE = {
        0x00000000, 0x04c11db7, 0x09823b6e, 0x0d4326d9,
        0x130476dc, 0x17c56b6b, 0x1a864db2, 0x1e475005,
        0x2608edb8, 0x22c9f00f, 0x2f8ad6d6, 0x2b4bcb61,
        0x350c9b64, 0x31cd86d3, 0x3c8ea00a, 0x384fbdbd,
        0x4c11db70, 0x48d0c6c7, 0x4593e01e, 0x4152fda9,
        0x5f15adac, 0x5bd4b01b, 0x569796c2, 0x52568b75,
        0x6a1936c8, 0x6ed82b7f, 0x639b0da6, 0x675a1011,
        0x791d4014, 0x7ddc5da3, 0x709f7b7a, 0x745e66cd,
        0x9823b6e0, 0x9ce2ab57, 0x91a18d8e, 0x95609039,
        0x8b27c03c, 0x8fe6dd8b, 0x82a5fb52, 0x8664e6e5,
        0xbe2b5b58, 0xbaea46ef, 0xb7a96036, 0xb3687d81,
        0xad2f2d84, 0xa9ee3033, 0xa4ad16ea, 0xa06c0b5d,
        0xd4326d90, 0xd0f37027, 0xddb056fe, 0xd9714b49,
        0xc7361b4c, 0xc3f706fb, 0xceb42022, 0xca753d95,
        0xf23a8028, 0xf6fb9d9f, 0xfbb8bb46, 0xff79a6f1,
        0xe13ef6f4, 0xe5ffeb43, 0xe8bccd9a, 0xec7dd02d,
        0x34867077, 0x30476dc0, 0x3d044b19, 0x39c556ae,
        0x278206ab, 0x23431b1c, 0x2e003dc5, 0x2ac12072,
        0x128e9dcf, 0x164f8078, 0x1b0ca6a1, 0x1fcdbb16,
        0x018aeb13, 0x054bf6a4, 0x0808d07d, 0x0cc9cdca,
        0x7897ab07, 0x7c56b6b0, 0x71159069, 0x75d48dde,
        0x6b93dddb, 0x6f52c06c, 0x6211e6b5, 0x66d0fb02,
        0x5e9f46bf, 0x5a5e5b08, 0x571d7dd1, 0x53dc6066,
        0x4d9b3063, 0x495a2dd4, 0x44190b0d, 0x40d816ba,
        0xaca5c697, 0xa864db20, 0xa527fdf9, 0xa1e6e04e,
        0xbfa1b04b, 0xbb60adfc, 0xb6238b25, 0xb2e29692,
        0x8aad2b2f, 0x8e6c3698, 0x832f1041, 0x87ee0df6,
        0x99a95df3, 0x9d684044, 0x902b669d, 0x94ea7b2a,
        0xe0b41de7, 0xe4750050, 0xe9362689, 0xedf73b3e,
        0xf3b06b3b, 0xf771768c, 0xfa325055, 0xfef34de2,
        0xc6bcf05f, 0xc27dede8, 0xcf3ecb31, 0xcbffd686,
        0xd5b88683, 0xd1799b34, 0xdc3abded, 0xd8fba05a,
        0x690ce0ee, 0x6dcdfd59, 0x608edb80, 0x644fc637,
        0x7a089632, 0x7ec98b85, 0x738aad5c, 0x774bb0eb,
        0x4f040d56, 0x4bc510e1, 0x46863638, 0x42472b8f,
        0x5c007b8a, 0x58c1663d, 0x558240e4, 0x51435d53,
        0x251d3b9e, 0x21dc2629, 0x2c9f00f0, 0x285e1d47,
        0x36194d42, 0x32d850f5, 0x3f9b762c, 0x3b5a6b9b,
        0x0315d626, 0x07d4cb91, 0x0a97ed48, 0x0e56f0ff,
        0x1011a0fa, 0x14d0bd4d, 0x19939b94, 0x1d528623,
        0xf12f560e, 0xf5ee4bb9, 0xf8ad6d60, 0xfc6c70d7,
        0xe22b20d2, 0xe6ea3d65, 0xeba91bbc, 0xef68060b,
        0xd727bbb6, 0xd3e6a601, 0xdea580d8, 0xda649d6f,
        0xc423cd6a, 0xc0e2d0dd, 0xcda1f604, 0xc960ebb3,
        0xbd3e8d7e, 0xb9ff90c9, 0xb4bcb610, 0xb07daba7,
        0xae3afba2, 0xaafbe615, 0xa7b8c0cc, 0xa379dd7b,
        0x9b3660c6, 0x9ff77d71, 0x92b45ba8, 0x9675461f,
        0x8832161a, 0x8cf30bad, 0x81b02d74, 0x857130c3,
        0x5d8a9099, 0x594b8d2e, 0x5408abf7, 0x50c9b640,
        0x4e8ee645, 0x4a4ffbf2, 0x470cdd2b, 0x43cdc09c,
        0x7b827d21, 0x7f436096, 0x7200464f, 0x76c15bf8,
        0x68860bfd, 0x6c47164a, 0x61043093, 0x65c52d24,
        0x119b4be9, 0x155a565e, 0x18197087, 0x1cd86d30,
        0x029f3d35, 0x065e2082, 0x0b1d065b, 0x0fdc1bec,
        0x3793a651, 0x3352bbe6, 0x3e119d3f, 0x3ad08088,
        0x2497d08d, 0x2056cd3a, 0x2d15ebe3, 0x29d4f654,
        0xc5a92679, 0xc1683bce, 0xcc2b1d17, 0xc8ea00a0,
        0xd6ad50a5, 0xd26c4d12, 0xdf2f6bcb, 0xdbee767c,
        0xe3a1cbc1, 0xe760d676, 0xea23f0af, 0xeee2ed18,
        0xf0a5bd1d, 0xf464a0aa, 0xf9278673, 0xfde69bc4,
        0x89b8fd09, 0x8d79e0be, 0x803ac667, 0x84fbdbd0,
        0x9abc8bd5, 0x9e7d9662, 0x933eb0bb, 0x97ffad0c,
        0xafb010b1, 0xab710d06, 0xa6322bdf, 0xa2f33668,
        0xbcb4666d, 0xb8757bda, 0xb5365d03, 0xb1f740b4
    };

    public interface OutputListener {
        void onMGTParsed(List<MGTItem> items);
        void onVCTParsed(List<VCTItem> items);
        void onEITParsed(int sourceId, List<EITItem> items);
    }

    public static class MGTItem {
        public static final int TABLE_TYPE_EIT_0 = 0x0100;
        private int mTableType;
        private int mTableTypePid;

        public MGTItem(int tableType, int tableTypePid) {
            mTableType = tableType;
            mTableTypePid = tableTypePid;
        }

        public int getTableType() {
            return mTableType;
        }

        public int getTableTypePid() {
            return mTableTypePid;
        }
    }

    public static class VCTItem {
        private String mShortName;
        private int mProgramNumber;
        private int mMajorChannelNumber;
        private int mMinorChannelNumber;
        private int mSourceId;

        public VCTItem(String shortName, int programNumber, int majorChannelNumber,
                int minorChannelNumber, int sourceId) {
            mShortName = shortName;
            mProgramNumber = programNumber;
            mMajorChannelNumber = majorChannelNumber;
            mMinorChannelNumber = minorChannelNumber;
            mSourceId = sourceId;
        }

        public String getShortName() {
            return mShortName;
        }

        public int getProgramNumber() {
            return mProgramNumber;
        }

        public int getMajorChannelNumber() {
            return mMajorChannelNumber;
        }

        public int getMinorChannelNumber() {
            return mMinorChannelNumber;
        }

        public int getSourceId() {
            return mSourceId;
        }
    }

    public static class EITItem {
        private String mTitleText;
        private long mStartTime;
        private int mLengthInSecond;

        public EITItem(String titleText, long startTime, int lengthInSecond) {
            mTitleText = titleText;
            mStartTime = startTime;
            mLengthInSecond = lengthInSecond;
        }

        public String getTitleText() {
            return mTitleText;
        }

        public long getStartTime() {
            return mStartTime;
        }

        public int getLengthInSecond() {
            return mLengthInSecond;
        }
    }

    private OutputListener mListener;

    public SectionParser(OutputListener listener) {
        mListener = listener;
    }

    public void parseSection(byte[] data) {
        if (!checkSanity(data)) {
            Log.d(TAG, "Bad CRC!");
            return;
        }
        switch (data[0]) {
            case TABLE_ID_MGT:
                parseMGT(data);
                break;
            case TABLE_ID_TVCT:
            case TABLE_ID_CVCT:
                parseVCT(data);
                break;
            case TABLE_ID_EIT:
                parseEIT(data);
                break;
            default:
                break;
        }
    }

    private void parseMGT(byte[] data) {
        // For details of the structure for MGT, please see ATSC A/65 Table 6.2.
        if (DEBUG) {
            Log.d(TAG, "MGT is discovered.");
        }
        int tablesDefined = ((data[9] & 0xff) << 8) | (data[10] & 0xff);
        int pos = 11;
        List<MGTItem> results = new ArrayList<MGTItem>();
        for (int i = 0; i < tablesDefined; ++i) {
            int tableType = ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
            int tableTypePid = ((data[pos + 2] & 0x1f) << 8) | (data[pos + 3] & 0xff);
            int descriptorsLength = ((data[pos + 9] & 0x0f) << 8) | (data[pos + 10] & 0xff);
            pos += 11 + descriptorsLength;
            if (pos >= data.length) {
                Log.d(TAG, "Broken MGT.");
                return;
            }
            results.add(new MGTItem(tableType, tableTypePid));
        }
        if ((data[pos] & 0xf0) != 0xf0) {
            Log.d(TAG, "Broken MGT.");
            return;
        }
        if (mListener != null) {
            mListener.onMGTParsed(results);
        }
    }

    private void parseVCT(byte[] data) {
        // For details of the structure for VCT, please see ATSC A/65 Table 6.4 and 6.8.
        if (DEBUG) {
            Log.d(TAG, "VCT is discovered.");
        }
        int sectionNumber = (data[6] & 0xff);
        int lastSectionNumber = (data[7] & 0xff);
        int numChannelsInSection = (data[9] & 0xff);
        int pos = 10;
        List<VCTItem> results = new ArrayList<VCTItem>();
        for (int i = 0; i < numChannelsInSection; ++i) {
            String shortName = "";
            int shortNameSize = getShortNameSize(data, pos);
            try {
                shortName = new String(
                        Arrays.copyOfRange(data, pos, pos + shortNameSize), "UTF-16");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if ((data[pos + 14] & 0xf0) != 0xf0) {
                Log.d(TAG, "Broken VCT.");
                return;
            }
            int majorNumber = ((data[pos + 14] & 0x0f) << 6) | ((data[pos + 15] & 0xff) >> 2);
            int minorNumber = ((data[pos + 15] & 0x03) << 8) | (data[pos + 16] & 0xff);
            if ((majorNumber & 0x3f0) == 0x3f0) {
                // If the six MSBs are 111111, these indicate that there is only one-part channel
                // number. To see details, please refer A/65 Section 6.3.2.
                majorNumber = ((majorNumber & 0xf) << 10) + minorNumber;
                minorNumber = 0;
            }
            int programNumber = ((data[pos + 24] & 0xff) << 8) | (data[pos + 25] & 0xff);
            int sourceId = ((data[pos + 28] & 0xff) << 8) | (data[pos + 29] & 0xff);
            int descriptorsLength = ((data[pos + 30] & 0x03) << 8) | (data[pos + 31] & 0xff);
            pos += 32 + descriptorsLength;
            if (pos >= data.length) {
                Log.d(TAG, "Broken VCT.");
                return;
            }
            results.add(new VCTItem(shortName, programNumber, majorNumber, minorNumber, sourceId));
        }
        if ((data[pos] & 0xfc) != 0xfc) {
            Log.d(TAG, "Broken VCT.");
            return;
        }
        if (mListener != null) {
            mListener.onVCTParsed(results);
        }
    }

    private void parseEIT(byte[] data) {
        // For details of the structure for EIT, please see ATSC A/65 Table 6.11.
        if (DEBUG) {
            Log.d(TAG, "EIT is discovered.");
        }
        int sourceId = ((data[3] & 0xff) << 8) | (data[4] & 0xff);
        int sectionNumber = (data[6] & 0xff);
        int lastSectionNumber = (data[7] & 0xff);
        int numEventsInSection = (data[9] & 0xff);

        int pos = 10;
        List<EITItem> results = new ArrayList<EITItem>();
        for (int i = 0; i < numEventsInSection; ++i) {
            if ((data[pos] & 0xc0) != 0xc0) {
                Log.d(TAG, "Broken EIT.");
                return;
            }
            long startTime = ((data[pos + 2] & (long) 0xff) << 24) | ((data[pos + 3] & 0xff) << 16)
                    | ((data[pos + 4] & 0xff) << 8) | (data[pos + 5] & 0xff);
            int lengthInSecond = ((data[pos + 6] & 0x0f) << 16)
                    | ((data[pos + 7] & 0xff) << 8) | (data[pos + 8] & 0xff);
            int titleLength = (data[pos + 9] & 0xff);
            String titleText = "";
            if (titleLength > 0) {
                titleText = extractText(data, pos + 10);
            }
            if ((data[pos + 10 + titleLength] & 0xf0) != 0xf0) {
                Log.d(TAG, "Broken EIT.");
                return;
            }
            int descriptorsLength = ((data[pos + 10 + titleLength] & 0x0f) << 8)
                    | (data[pos + 10 + titleLength + 1] & 0xff);
            pos += 10 + titleLength + 2 + descriptorsLength;
            if (pos >= data.length) {
                Log.d(TAG, "Broken EIT.");
                return;
            }
            results.add(new EITItem(titleText, startTime, lengthInSecond));
        }
        if (mListener != null) {
            mListener.onEITParsed(sourceId, results);
        }
    }

    private int getShortNameSize(byte[] data, int offset) {
        for (int i = 0; i < MAX_SHORT_NAME_BYTES; i += 2) {
            if(data[offset + i] == 0 && data[offset + i + 1] == 0) {
                return i;
            }
        }
        return MAX_SHORT_NAME_BYTES;
    }

    private String extractText(byte[] data, int startOffset) {
        int pos = startOffset;
        int numStrings = data[pos] & 0xff;
        pos++;
        for (int i = 0; i < numStrings; ++i) {
            int numSegments = data[pos + 3] & 0xff;
            pos += 4;
            for (int j = 0; j < numSegments; ++j) {
                int compressionType = data[pos] & 0xff;
                int mode = data[pos + 1] & 0xff;
                int numBytes = data[pos + 2] & 0xff;
                if (compressionType == COMPRESSION_TYPE_NO_COMPRESSION) {
                    try {
                        if (mode == MODE_UTF16) {
                            return new String(Arrays.copyOfRange(
                                    data, pos + 3, pos + 3 + numBytes), "UTF-16");
                        } else if (mode == MODE_SELECTED_UNICODE_RANGE_1){
                            return new String(Arrays.copyOfRange(
                                    data, pos + 3, pos + 3 + numBytes), "UTF-8");
                        }
                    } catch (UnsupportedEncodingException e) {
                        System.out.println("Unsupported text format.");
                    }
                }
                pos += 3 + numBytes;
            }
        }
        return null;
    }

    private boolean checkSanity(byte[] data) {
        boolean hasCRC = (data[1] & 0x80) != 0; // section_syntax_indicator
        if (hasCRC) {
            int crc = 0xffffffff;
            for(byte b : data) {
                int index = ((crc >> 24) ^ (b & 0xff)) & 0xff;
                crc = CRC_TABLE[index] ^ (crc << 8);
            }
            if(crc != 0){
                return false;
            }
        }
        return true;
    }
}
