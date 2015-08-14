/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.bluetoothadvertisements;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

/**
 * Allows user to start & stop Bluetooth LE Advertising of their device.
 */
public class AdvertiserFragment extends Fragment {

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private AdvertiseCallback mAdvertiseCallback;

    private Switch mSwitch;

    /**
     * Must be called after object creation by MainActivity.
     *
     * @param btAdapter the local BluetoothAdapter
     */
    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advertiser, container, false);

        mSwitch = (Switch) view.findViewById(R.id.advertise_switch);
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwitchClicked(v);
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mAdvertiseCallback != null){
            stopAdvertising();
        }
    }

    /**
     * Called when switch is toggled - starts or stops advertising.
     *
     * @param view is the Switch View object
     */
    public void onSwitchClicked(View view) {

        // Is the toggle on?
        boolean on = ((Switch) view).isChecked();

        if (on) {
            startAdvertising();
        } else {
            stopAdvertising();
        }
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {

        mAdvertiseCallback = new SampleAdvertiseCallback();

        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.startAdvertising(buildAdvertiseSettings(), buildAdvertiseData(),
                    mAdvertiseCallback);
        } else {
            mSwitch.setChecked(false);
            Toast.makeText(getActivity(), getString(R.string.bt_null), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {

        if (mBluetoothLeAdvertiser != null) {

            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;

        } else {
            mSwitch.setChecked(false);
            Toast.makeText(getActivity(), getString(R.string.bt_null), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        // Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
        // This includes everything put into AdvertiseData including UUIDs, device info, &
        // arbitrary service or manufacturer data.
        // Attempting to send packets over this limit will result in a failure with error code
        // AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
        // onStartFailure() method of an AdvertiseCallback implementation.

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life).
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);

        return settingsBuilder.build();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            mSwitch.setChecked(false);

            String errorMessage = getString(R.string.start_error_prefix);
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMessage += " " + getString(R.string.start_error_already_started);
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMessage += " " + getString(R.string.start_error_too_large);
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMessage += " " + getString(R.string.start_error_unsupported);
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMessage += " " + getString(R.string.start_error_internal);
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMessage += " " + getString(R.string.start_error_too_many);
                    break;
            }

            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            // Don't need to do anything here, advertising successfully started.
        }
    }

}
