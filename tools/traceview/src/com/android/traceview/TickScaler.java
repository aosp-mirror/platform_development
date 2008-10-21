/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

class TickScaler {

    private double mMinVal; // required input
    private double mMaxVal; // required input
    private double mRangeVal;
    private int mNumPixels; // required input
    private int mPixelsPerTick; // required input
    private double mPixelsPerRange;
    private double mTickIncrement;
    private double mMinMajorTick;

    TickScaler(double minVal, double maxVal, int numPixels, int pixelsPerTick) {
        mMinVal = minVal;
        mMaxVal = maxVal;
        mNumPixels = numPixels;
        mPixelsPerTick = pixelsPerTick;
    }

    public void setMinVal(double minVal) {
        mMinVal = minVal;
    }

    public double getMinVal() {
        return mMinVal;
    }

    public void setMaxVal(double maxVal) {
        mMaxVal = maxVal;
    }

    public double getMaxVal() {
        return mMaxVal;
    }

    public void setNumPixels(int numPixels) {
        mNumPixels = numPixels;
    }

    public int getNumPixels() {
        return mNumPixels;
    }

    public void setPixelsPerTick(int pixelsPerTick) {
        mPixelsPerTick = pixelsPerTick;
    }

    public int getPixelsPerTick() {
        return mPixelsPerTick;
    }

    public void setPixelsPerRange(double pixelsPerRange) {
        mPixelsPerRange = pixelsPerRange;
    }

    public double getPixelsPerRange() {
        return mPixelsPerRange;
    }

    public void setTickIncrement(double tickIncrement) {
        mTickIncrement = tickIncrement;
    }

    public double getTickIncrement() {
        return mTickIncrement;
    }

    public void setMinMajorTick(double minMajorTick) {
        mMinMajorTick = minMajorTick;
    }

    public double getMinMajorTick() {
        return mMinMajorTick;
    }

    // Convert a time value to a 0-based pixel value
    public int valueToPixel(double value) {
        return (int) Math.ceil(mPixelsPerRange * (value - mMinVal) - 0.5);
    }

    // Convert a time value to a 0-based fractional pixel
    public double valueToPixelFraction(double value) {
        return mPixelsPerRange * (value - mMinVal);
    }

    // Convert a 0-based pixel value to a time value
    public double pixelToValue(int pixel) {
        return mMinVal + (pixel / mPixelsPerRange);
    }

    public void computeTicks(boolean useGivenEndPoints) {
        int numTicks = mNumPixels / mPixelsPerTick;
        mRangeVal = mMaxVal - mMinVal;
        mTickIncrement = mRangeVal / numTicks;
        double dlogTickIncrement = Math.log10(mTickIncrement);
        int logTickIncrement = (int) Math.floor(dlogTickIncrement);
        double scale = Math.pow(10, logTickIncrement);
        double scaledTickIncr = mTickIncrement / scale;
        if (scaledTickIncr > 5.0)
            scaledTickIncr = 10;
        else if (scaledTickIncr > 2)
            scaledTickIncr = 5;
        else if (scaledTickIncr > 1)
            scaledTickIncr = 2;
        else
            scaledTickIncr = 1;
        mTickIncrement = scaledTickIncr * scale;

        if (!useGivenEndPoints) {
            // Round up the max val to the next minor tick
            double minorTickIncrement = mTickIncrement / 5;
            double dval = mMaxVal / minorTickIncrement;
            int ival = (int) dval;
            if (ival != dval)
                mMaxVal = (ival + 1) * minorTickIncrement;

            // Round down the min val to a multiple of tickIncrement
            ival = (int) (mMinVal / mTickIncrement);
            mMinVal = ival * mTickIncrement;
            mMinMajorTick = mMinVal;
        } else {
            int ival = (int) (mMinVal / mTickIncrement);
            mMinMajorTick = ival * mTickIncrement;
            if (mMinMajorTick < mMinVal)
                mMinMajorTick = mMinMajorTick + mTickIncrement;
        }

        mRangeVal = mMaxVal - mMinVal;
        mPixelsPerRange = (double) mNumPixels / mRangeVal;
    }
}
