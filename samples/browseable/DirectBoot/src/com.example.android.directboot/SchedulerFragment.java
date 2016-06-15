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
import com.example.android.directboot.alarms.AlarmAdapter;
import com.example.android.directboot.alarms.AlarmIntentService;
import com.example.android.directboot.alarms.AlarmStorage;
import com.example.android.directboot.alarms.AlarmUtil;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment that registers scheduled alarms.
 */
public class SchedulerFragment extends Fragment {

    private static final String FRAGMENT_TIME_PICKER_TAG = "fragment_time_picker";

    private AlarmAdapter mAlarmAdapter;
    private AlarmUtil mAlarmUtil;
    private TextView mTextViewIntroMessage;
    private BroadcastReceiver mAlarmWentOffBroadcastReceiver;

    public static SchedulerFragment newInstance() {
        SchedulerFragment fragment = new SchedulerFragment();
        return fragment;
    }

    public SchedulerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAlarmWentOffBroadcastReceiver = new AlarmWentOffReceiver();
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mAlarmWentOffBroadcastReceiver,
                        new IntentFilter(AlarmIntentService.ALARM_WENT_OFF_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarm_scheduler, container, false);
    }

    @Override
    public void onViewCreated(final View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab_add_alarm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerFragment fragment = TimePickerFragment.newInstance();
                fragment.setAlarmAddListener(new AlarmAddListenerImpl());
                fragment.show(getFragmentManager(), FRAGMENT_TIME_PICKER_TAG);
            }
        });
        mTextViewIntroMessage = (TextView) rootView.findViewById(R.id.text_intro_message);
        Activity activity = getActivity();
        AlarmStorage alarmStorage = new AlarmStorage(activity);
        mAlarmAdapter = new AlarmAdapter(activity, alarmStorage.getAlarms());
        if (mAlarmAdapter.getItemCount() == 0) {
            mTextViewIntroMessage.setVisibility(View.VISIBLE);
        }
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(mAlarmAdapter);
        recyclerView.addItemDecoration(new AlarmAdapter.DividerItemDecoration(activity));
        mAlarmUtil = new AlarmUtil(activity);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mAlarmWentOffBroadcastReceiver);
        super.onDestroy();
    }

    /**
     * {@link TimePickerFragment.AlarmAddListener} to do actions after an alarm is added.
     */
    private class AlarmAddListenerImpl implements TimePickerFragment.AlarmAddListener {

        @Override
        public void onAlarmAdded(Alarm alarm) {
            mAlarmAdapter.addAlarm(alarm);
            mAlarmUtil.scheduleAlarm(alarm);
            mTextViewIntroMessage.setVisibility(View.GONE);
        }
    }

    /**
     * A {@link BroadcastReceiver} that receives an intent when an alarm goes off.
     * This receiver removes the corresponding alarm from the RecyclerView.
     */
    private class AlarmWentOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Alarm alarm = intent.getParcelableExtra(AlarmIntentService.ALARM_KEY);
            mAlarmAdapter.deleteAlarm(alarm);
        }
    }
}
