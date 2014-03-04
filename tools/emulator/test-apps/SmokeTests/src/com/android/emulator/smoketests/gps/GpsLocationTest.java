/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.emulator.smoketests.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import junit.framework.Assert;

/**
 * GPS Location Test
 *
 * Test the GPS API by verifying the previously set location
 */
public class GpsLocationTest extends AndroidTestCase implements LocationListener {

    private LocationManager locationManager;
    private Location mLocation;
    /**
     * Prior to running this test the GPS location must be set to the following
     * longitude and latitude coordinates via the geo fix command
     */
    private static final double LONGITUDE = -122.08345770835876;
    private static final double LATITUDE = 37.41991859119417;
    private static final int TIMEOUT = 5000;

    @Override
    protected void setUp() throws Exception {
        locationManager = (LocationManager) getContext().
            getSystemService(Context.LOCATION_SERVICE);
        assertNotNull(locationManager);
    }

    /**
     * verify that the last location equals to the location set
     * via geo fix command
     */
    public void testCurrentLocationGivenLocation(){
        try{
            synchronized ( this ){
                HandlerThread handlerThread = new HandlerThread("testLocationUpdates");
                handlerThread.start();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this,
                        handlerThread.getLooper());
                this.wait(TIMEOUT);
            }
        }catch ( InterruptedException ie){
            ie.printStackTrace();
            Assert.fail();
        }
        assertNotNull(mLocation);
        assertEquals(new Float(LONGITUDE), new Float(mLocation.getLongitude()));
        assertEquals(new Float(LATITUDE), new Float(mLocation.getLatitude()));
        locationManager.removeUpdates(this);
    }

    public void onLocationChanged(Location location) {
        synchronized ( this ){
            mLocation=location;
            this.notify();
        }
    }

    public void onProviderDisabled(String arg0) {
    }

    public void onProviderEnabled(String arg0) {
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    }
}
