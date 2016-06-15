/*
* Copyright 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.directboot;

import com.example.android.directboot.alarms.Alarm;
import com.example.android.directboot.alarms.AlarmStorage;
import com.example.android.directboot.alarms.AlarmUtil;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

/**
 * DialogFragment for showing a TimePicker.
 */
public class TimePickerFragment extends DialogFragment {

    private TimePicker mTimePicker;
    private AlarmStorage mAlarmStorage;
    private AlarmAddListener mAlarmAddListener;
    private AlarmUtil mAlarmUtil;

    public TimePickerFragment() {}

    public static TimePickerFragment newInstance() {
        return new TimePickerFragment();
    }

    public void setAlarmAddListener(AlarmAddListener listener) {
        mAlarmAddListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAlarmStorage = new AlarmStorage(getActivity());
        mAlarmUtil = new AlarmUtil(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_picker, container, false);
        mTimePicker = (TimePicker) view.findViewById(R.id.time_picker_alarm);
        Button buttonOk = (Button) view.findViewById(R.id.button_ok_time_picker);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar alarmTime = mAlarmUtil
                        .getNextAlarmTime(mTimePicker.getHour(), mTimePicker.getMinute());
                Alarm alarm = mAlarmStorage
                        .saveAlarm(alarmTime.get(Calendar.MONTH), alarmTime.get(Calendar.DATE),
                                alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE));
                String alarmSavedString = getActivity()
                        .getString(R.string.alarm_saved, alarm.hour, alarm.minute);
                Toast.makeText(getActivity(), alarmSavedString, Toast.LENGTH_SHORT).show();
                if (mAlarmAddListener != null) {
                    mAlarmAddListener.onAlarmAdded(alarm);
                }
                dismiss();
            }
        });
        Button buttonCancel = (Button) view.findViewById(R.id.button_cancel_time_picker);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }

    public interface AlarmAddListener {
        void onAlarmAdded(Alarm alarm);
    }
}
