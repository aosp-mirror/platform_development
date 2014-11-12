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

package com.example.android.support.wearable.notifications;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Main activity which posts a notification when resumed, and allows customization
 * of that notification via controls.
 */
public class MainActivity extends Activity implements Handler.Callback {
    private static final int MSG_POST_NOTIFICATIONS = 0;
    private static final long POST_NOTIFICATIONS_DELAY_MS = 200;

    private Handler mHandler;
    private Spinner mPresetSpinner;
    private EditText mTitleEditText;
    private EditText mTextEditText;
    private TextWatcher mTextChangedListener;
    private Spinner mPrioritySpinner;
    private Spinner mActionsSpinner;
    private CheckBox mIncludeLargeIconCheckbox;
    private CheckBox mLocalOnlyCheckbox;
    private CheckBox mIncludeContentIntentCheckbox;
    private CheckBox mVibrateCheckbox;
    private BackgroundPickers mBackgroundPickers;
    private int postedNotificationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHandler = new Handler(this);
        mTextChangedListener = new UpdateNotificationsOnTextChangeListener();

        initPresetSpinner();
        initTitleEditText();
        initTextEditText();
        initPrioritySpinner();
        initActionsSpinner();
        initIncludeLargeIconCheckbox();
        initLocalOnlyCheckbox();
        initIncludeContentIntentCheckbox();
        initVibrateCheckbox();
        initBackgroundPickers();

        NotificationPreset preset = NotificationPresets.PRESETS[
                mPresetSpinner.getSelectedItemPosition()];
        updateTextEditors(preset);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotifications(false /* cancelExisting */);
    }

    private void initPresetSpinner() {
        mPresetSpinner = (Spinner) findViewById(R.id.preset_spinner);
        mPresetSpinner.setAdapter(new NamedPresetSpinnerArrayAdapter(this,
                NotificationPresets.PRESETS));
        mPresetSpinner.post(new Runnable() {
            @Override
            public void run() {
                mPresetSpinner.setOnItemSelectedListener(new PresetSpinnerListener());
            }
        });
    }

    private void initTitleEditText() {
        mTitleEditText = (EditText) findViewById(R.id.title_editor);
    }

    private void initTextEditText() {
        mTextEditText = (EditText) findViewById(R.id.text_editor);
    }

    private void initPrioritySpinner() {
        mPrioritySpinner = (Spinner) findViewById(R.id.priority_spinner);
        mPrioritySpinner.setAdapter(new NamedPresetSpinnerArrayAdapter(this,
                PriorityPresets.PRESETS));
        mPrioritySpinner.setSelection(Arrays.asList(PriorityPresets.PRESETS)
                .indexOf(PriorityPresets.DEFAULT));
        mPrioritySpinner.post(new Runnable() {
            @Override
            public void run() {
                mPrioritySpinner.setOnItemSelectedListener(
                        new UpdateNotificationsOnItemSelectedListener(true /* cancelExisting */));
            }
        });
    }

    private void initActionsSpinner() {
        mActionsSpinner = (Spinner) findViewById(R.id.actions_spinner);
        mActionsSpinner.setAdapter(new NamedPresetSpinnerArrayAdapter(this,
                ActionsPresets.PRESETS));
        mActionsSpinner.post(new Runnable() {
            @Override
            public void run() {
                mActionsSpinner.setOnItemSelectedListener(
                        new UpdateNotificationsOnItemSelectedListener(false /* cancelExisting */));
            }
        });
    }

    private void initIncludeLargeIconCheckbox() {
        mIncludeLargeIconCheckbox = (CheckBox) findViewById(R.id.include_large_icon_checkbox);
        mIncludeLargeIconCheckbox.setOnCheckedChangeListener(
                new UpdateNotificationsOnCheckedChangeListener(false /* cancelExisting */));
    }

    private void initLocalOnlyCheckbox() {
        mLocalOnlyCheckbox = (CheckBox) findViewById(R.id.local_only_checkbox);
        mLocalOnlyCheckbox.setOnCheckedChangeListener(
                new UpdateNotificationsOnCheckedChangeListener(false /* cancelExisting */));
    }

    private void initIncludeContentIntentCheckbox() {
        mIncludeContentIntentCheckbox = (CheckBox) findViewById(
                R.id.include_content_intent_checkbox);
        mIncludeContentIntentCheckbox.setOnCheckedChangeListener(
                new UpdateNotificationsOnCheckedChangeListener(false /* cancelExisting */));
    }

    private void initVibrateCheckbox() {
        mVibrateCheckbox = (CheckBox) findViewById(R.id.vibrate_checkbox);
        mVibrateCheckbox.setOnCheckedChangeListener(
                new UpdateNotificationsOnCheckedChangeListener(false /* cancelExisting */));
    }

    private void initBackgroundPickers() {
        mBackgroundPickers = new BackgroundPickers(
                (ViewGroup) findViewById(R.id.background_pickers),
                new BackgroundPickerListener());
    }

    private void updateTextEditors(NotificationPreset preset) {
        if (preset == NotificationPresets.BASIC) {
            findViewById(R.id.title_edit_field).setVisibility(View.VISIBLE);
            mTitleEditText.setText(getString(preset.titleResId));
            mTitleEditText.addTextChangedListener(mTextChangedListener);
            findViewById(R.id.text_edit_field).setVisibility(View.VISIBLE);
            mTextEditText.setText(getString(preset.textResId));
            mTextEditText.addTextChangedListener(mTextChangedListener);
        } else {
            findViewById(R.id.title_edit_field).setVisibility(View.GONE);
            mTitleEditText.removeTextChangedListener(mTextChangedListener);
            findViewById(R.id.text_edit_field).setVisibility(View.GONE);
            mTextEditText.removeTextChangedListener(mTextChangedListener);
        }
    }

    /**
     * Begin to re-post the sample notification(s).
     */
    private void updateNotifications(boolean cancelExisting) {
        // Disable messages to skip notification deleted messages during cancel.
        sendBroadcast(new Intent(NotificationIntentReceiver.ACTION_DISABLE_MESSAGES)
                .setClass(this, NotificationIntentReceiver.class));

        if (cancelExisting) {
            // Cancel all existing notifications to trigger fresh-posting behavior: For example,
            // switching from HIGH to LOW priority does not cause a reordering in Notification Shade.
            NotificationManagerCompat.from(this).cancelAll();
            postedNotificationCount = 0;

            // Post the updated notifications on a delay to avoid a cancel+post race condition
            // with notification manager.
            mHandler.removeMessages(MSG_POST_NOTIFICATIONS);
            mHandler.sendEmptyMessageDelayed(MSG_POST_NOTIFICATIONS, POST_NOTIFICATIONS_DELAY_MS);
        } else {
            postNotifications();
        }
    }

    /**
     * Post the sample notification(s) using current options.
     */
    private void postNotifications() {
        sendBroadcast(new Intent(NotificationIntentReceiver.ACTION_ENABLE_MESSAGES)
                .setClass(this, NotificationIntentReceiver.class));

        NotificationPreset preset = NotificationPresets.PRESETS[
                mPresetSpinner.getSelectedItemPosition()];
        CharSequence titlePreset = mTitleEditText.getText();
        CharSequence textPreset = mTextEditText.getText();
        PriorityPreset priorityPreset = PriorityPresets.PRESETS[
                mPrioritySpinner.getSelectedItemPosition()];
        ActionsPreset actionsPreset = ActionsPresets.PRESETS[
                mActionsSpinner.getSelectedItemPosition()];
        if (preset.actionsRequired() && actionsPreset == ActionsPresets.NO_ACTIONS_PRESET) {
            // If actions are required, but the no-actions preset was selected, change presets.
            actionsPreset = ActionsPresets.SINGLE_ACTION_PRESET;
            mActionsSpinner.setSelection(Arrays.asList(ActionsPresets.PRESETS).indexOf(
                    actionsPreset), true);
        }
        NotificationPreset.BuildOptions options = new NotificationPreset.BuildOptions(
                titlePreset,
                textPreset,
                priorityPreset,
                actionsPreset,
                mIncludeLargeIconCheckbox.isChecked(),
                mLocalOnlyCheckbox.isChecked(),
                mIncludeContentIntentCheckbox.isChecked(),
                mVibrateCheckbox.isChecked(),
                mBackgroundPickers.getRes());
        Notification[] notifications = preset.buildNotifications(this, options);

        // Post new notifications
        for (int i = 0; i < notifications.length; i++) {
            NotificationManagerCompat.from(this).notify(i, notifications[i]);
        }
        // Cancel any that are beyond the current count.
        for (int i = notifications.length; i < postedNotificationCount; i++) {
            NotificationManagerCompat.from(this).cancel(i);
        }
        postedNotificationCount = notifications.length;
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_POST_NOTIFICATIONS:
                postNotifications();
                return true;
        }
        return false;
    }

    private class PresetSpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            NotificationPreset preset = NotificationPresets.PRESETS[position];
            mBackgroundPickers.generatePickers(preset.countBackgroundPickersRequired());
            updateTextEditors(preset);
            updateNotifications(false /* cancelExisting */);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private class UpdateNotificationsOnTextChangeListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateNotifications(false /* cancelExisting */);
        }
    }

    private class UpdateNotificationsOnItemSelectedListener
            implements AdapterView.OnItemSelectedListener {
        private final boolean mCancelExisting;

        public UpdateNotificationsOnItemSelectedListener(boolean cancelExisting) {
            mCancelExisting = cancelExisting;
        }
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateNotifications(mCancelExisting);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private class UpdateNotificationsOnCheckedChangeListener
            implements CompoundButton.OnCheckedChangeListener {
        private final boolean mCancelExisting;

        public UpdateNotificationsOnCheckedChangeListener(boolean cancelExisting) {
            mCancelExisting = cancelExisting;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            updateNotifications(mCancelExisting);
        }
    }

    private class BackgroundPickerListener
            implements BackgroundPickers.OnBackgroundPickersChangedListener {
        @Override
        public void onBackgroundPickersChanged(BackgroundPickers pickers) {
            updateNotifications(false /* cancelExisting */);
        }
    }

    private class NamedPresetSpinnerArrayAdapter extends ArrayAdapter<NamedPreset> {
        public NamedPresetSpinnerArrayAdapter(Context context, NamedPreset[] presets) {
            super(context, R.layout.simple_spinner_item, presets);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setText(getString(getItem(position).nameResId));
            return view;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) getLayoutInflater().inflate(
                    android.R.layout.simple_spinner_item, parent, false);
            view.setText(getString(getItem(position).nameResId));
            return view;
        }
    }
}
