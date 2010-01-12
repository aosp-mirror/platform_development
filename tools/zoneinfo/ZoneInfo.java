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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * Copied from ZoneInfo and ZoneInfoDB in dalvik.
 * {@hide}
 */
public class ZoneInfo extends TimeZone {

    private static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
    private static final long MILLISECONDS_PER_400_YEARS =
        MILLISECONDS_PER_DAY * (400 * 365 + 100 - 3);

    private static final long UNIX_OFFSET = 62167219200000L;

    private static final int[] NORMAL = new int[] {
        0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,
    };

    private static final int[] LEAP = new int[] {
        0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335,
    };

    private static String nullName(byte[] data, int where, int off) {
        if (off < 0)
            return null;

        int end = where + off;
        while (end < data.length && data[end] != '\0')
            end++;

        return new String(data, where + off, end - (where + off));
    }

    public static ZoneInfo make(String name, byte[] data) {
        int ntransition = read4(data, 32);
        int ngmtoff = read4(data, 36);
        int base = 44;

        int[] transitions = new int[ntransition];
        for (int i = 0; i < ntransition; i++)
            transitions[i] = read4(data, base + 4 * i);
        base += 4 * ntransition;

        byte[] type = new byte[ntransition];
        for (int i = 0; i < ntransition; i++)
            type[i] = data[base + i];
        base += ntransition;

        int[] gmtoff = new int[ngmtoff];
        byte[] isdst = new byte[ngmtoff];
        byte[] abbrev = new byte[ngmtoff];
        for (int i = 0; i < ngmtoff; i++) {
            gmtoff[i] = read4(data, base + 6 * i);
            isdst[i] = data[base + 6 * i + 4];
            abbrev[i] = data[base + 6 * i + 5];
        }

        base += 6 * ngmtoff;

        return new ZoneInfo(name, transitions, type, gmtoff, isdst, abbrev, data, base);
    }

    private static int read4(byte[] data, int off) {
        return ((data[off    ] & 0xFF) << 24) |
               ((data[off + 1] & 0xFF) << 16) |
               ((data[off + 2] & 0xFF) <<  8) |
               ((data[off + 3] & 0xFF) <<  0);
    }

    /*package*/ ZoneInfo(String name, int[] transitions, byte[] type,
                     int[] gmtoff, byte[] isdst, byte[] abbrev,
                     byte[] data, int abbrevoff) {
        mTransitions = transitions;
        mTypes = type;
        mGmtOffs = gmtoff;
        mIsDsts = isdst;
        mUseDst = false;
        setID(name);

        // Find the latest GMT and non-GMT offsets for their abbreviations

        int lastdst;
        for (lastdst = mTransitions.length - 1; lastdst >= 0; lastdst--) {
            if (mIsDsts[mTypes[lastdst] & 0xFF] != 0)
                break;
        }

        int laststd;
        for (laststd = mTransitions.length - 1; laststd >= 0; laststd--) {
            if (mIsDsts[mTypes[laststd] & 0xFF] == 0)
                break;
        }

        if (lastdst >= 0) {
            mDaylightName = nullName(data, abbrevoff,
                                     abbrev[mTypes[lastdst] & 0xFF]);
        }
        if (laststd >= 0) {
            mStandardName = nullName(data, abbrevoff,
                                     abbrev[mTypes[laststd] & 0xFF]);
        }

        // Use the latest non-DST offset if any as the raw offset

        if (laststd < 0) {
            laststd = 0;
        }

        if (laststd >= mTypes.length) {
            mRawOffset = mGmtOffs[0];
        } else {
            mRawOffset = mGmtOffs[mTypes[laststd] & 0xFF];
        }

        // Subtract the raw offset from all offsets so it can be changed
        // and affect them too.
        // Find whether there exist any observances of DST.

        for (int i = 0; i < mGmtOffs.length; i++) {
            mGmtOffs[i] -= mRawOffset;

            if (mIsDsts[i] != 0) {
                mUseDst = true;
            }
        }

        mRawOffset *= 1000;
    }

    @Override
    public int getOffset(@SuppressWarnings("unused") int era,
        int year, int month, int day,
        @SuppressWarnings("unused") int dayOfWeek,
        int millis) {
        // XXX This assumes Gregorian always; Calendar switches from
        // Julian to Gregorian in 1582.  What calendar system are the
        // arguments supposed to come from?

        long calc = (year / 400) * MILLISECONDS_PER_400_YEARS;
        year %= 400;

        calc += year * (365 * MILLISECONDS_PER_DAY);
        calc += ((year + 3) / 4) * MILLISECONDS_PER_DAY;

        if (year > 0)
            calc -= ((year - 1) / 100) * MILLISECONDS_PER_DAY;

        boolean isLeap = (year == 0 || (year % 4 == 0 && year % 100 != 0));
        int[] mlen = isLeap ? LEAP : NORMAL;

        calc += mlen[month] * MILLISECONDS_PER_DAY;
        calc += (day - 1) * MILLISECONDS_PER_DAY;
        calc += millis;

        calc -= mRawOffset;
        calc -= UNIX_OFFSET;

        return getOffset(calc);
    }

    @Override
    public int getOffset(long when) {
        int unix = (int) (when / 1000);
        int trans = Arrays.binarySearch(mTransitions, unix);

        if (trans == ~0) {
            return mGmtOffs[0] * 1000 + mRawOffset;
        }
        if (trans < 0) {
            trans = ~trans - 1;
        }

        return mGmtOffs[mTypes[trans] & 0xFF] * 1000 + mRawOffset;
    }

    @Override
    public int getRawOffset() {
        return mRawOffset;
    }

    @Override
    public void setRawOffset(int off) {
        mRawOffset = off;
    }

    @Override
    public boolean inDaylightTime(Date when) {
        int unix = (int) (when.getTime() / 1000);
        int trans = Arrays.binarySearch(mTransitions, unix);

        if (trans == ~0) {
            return mIsDsts[0] != 0;
        }
        if (trans < 0) {
            trans = ~trans - 1;
        }

        return mIsDsts[mTypes[trans] & 0xFF] != 0;
    }

    @Override
    public boolean useDaylightTime() {
        return mUseDst;
    }

    private int mRawOffset;
    private int[] mTransitions;
    private int[] mGmtOffs;
    private byte[] mTypes;
    private byte[] mIsDsts;
    private boolean mUseDst;
    private String mDaylightName;
    private String mStandardName;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneInfo)) {
           return false;
        }
        ZoneInfo other = (ZoneInfo) obj;
        return mUseDst == other.mUseDst
                && (mDaylightName == null ? other.mDaylightName == null :
                        mDaylightName.equals(other.mDaylightName))
                && (mStandardName == null ? other.mStandardName == null :
                        mStandardName.equals(other.mStandardName))
                && mRawOffset == other.mRawOffset
                // Arrays.equals returns true if both arrays are null
                && Arrays.equals(mGmtOffs, other.mGmtOffs)
                && Arrays.equals(mIsDsts, other.mIsDsts)
                && Arrays.equals(mTypes, other.mTypes)
                && Arrays.equals(mTransitions, other.mTransitions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDaylightName == null) ? 0 :
                mDaylightName.hashCode());
        result = prime * result + Arrays.hashCode(mGmtOffs);
        result = prime * result + Arrays.hashCode(mIsDsts);
        result = prime * result + mRawOffset;
        result = prime * result + ((mStandardName == null) ? 0 :
                mStandardName.hashCode());
        result = prime * result + Arrays.hashCode(mTransitions);
        result = prime * result + Arrays.hashCode(mTypes);
        result = prime * result + (mUseDst ? 1231 : 1237);
        return result;
    }
}
