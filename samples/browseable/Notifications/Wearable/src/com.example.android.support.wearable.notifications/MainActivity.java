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
import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements WearableListView.ClickListener {
    private static final int SAMPLE_NOTIFICATION_ID = 0;
    public static final String KEY_REPLY = "reply";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WearableListView listView = (WearableListView) findViewById(R.id.list);
        listView.setAdapter(new Adapter(this));
        listView.setClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null) {
            Bundle inputResults = RemoteInput.getResultsFromIntent(getIntent());
            if (inputResults != null) {
                CharSequence replyText = inputResults.getCharSequence(KEY_REPLY);
                if (replyText != null) {
                    Toast.makeText(this, TextUtils.concat(getString(R.string.reply_was), replyText),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /** Post a new or updated notification using the selected notification options. */
    private void updateNotification(int presetIndex) {
        NotificationPreset preset = NotificationPresets.PRESETS[presetIndex];
        Notification notif = preset.buildNotification(this);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(SAMPLE_NOTIFICATION_ID, notif);
        finish();
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        updateNotification((Integer) v.itemView.getTag());
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    private static final class Adapter extends WearableListView.Adapter {
        private final Context mContext;
        private final LayoutInflater mInflater;

        private Adapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(
                    mInflater.inflate(R.layout.notif_preset_list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView view = (TextView) holder.itemView.findViewById(R.id.name);
            view.setText(mContext.getString(NotificationPresets.PRESETS[position].nameResId));
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return NotificationPresets.PRESETS.length;
        }
    }
}
