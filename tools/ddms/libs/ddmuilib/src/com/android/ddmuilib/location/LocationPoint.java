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

package com.android.ddmuilib.location;

/**
 * Base class for Location aware points.
 */
class LocationPoint {
    private double mLongitude;
    private double mLatitude;
    private boolean mHasElevation = false;
    private double mElevation;

    final void setLocation(double longitude, double latitude) {
        mLongitude = longitude;
        mLatitude = latitude;
    }
    
    public final double getLongitude() {
        return mLongitude;
    }
    
    public final double getLatitude() {
        return mLatitude;
    }

    final void setElevation(double elevation) {
        mElevation = elevation;
        mHasElevation = true;
    }
    
    public final boolean hasElevation() {
        return mHasElevation;
    }
    
    public final double getElevation() {
        return mElevation;
    }
}
