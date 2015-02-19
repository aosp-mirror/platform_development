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

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

/**
 * Collection of notification actions presets.
 */
public class ActionsPresets {
    public static final ActionsPreset NO_ACTIONS_PRESET = new NoActionsPreset();
    public static final ActionsPreset SINGLE_ACTION_PRESET = new SingleActionPreset();

    public static final ActionsPreset[] PRESETS = new ActionsPreset[] {
            NO_ACTIONS_PRESET,
            SINGLE_ACTION_PRESET,
            new ReplyActionPreset(),
            new ReplyWithChoicesActionPreset(),
            new DifferentActionsOnPhoneAndWearable(),
            new LongTitleActionPreset()
    };

    private static class NoActionsPreset extends ActionsPreset {
        public NoActionsPreset() {
            super(R.string.no_actions);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
        }
    }

    private static class SingleActionPreset extends ActionsPreset {
        public SingleActionPreset() {
            super(R.string.single_action);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            builder.addAction(R.drawable.ic_full_action,
                    context.getString(R.string.example_action),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.example_action_clicked))
                    .build();
        }
    }

    private static class LongTitleActionPreset extends ActionsPreset {
        public LongTitleActionPreset() {
            super(R.string.long_title_action);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            builder.addAction(R.drawable.ic_full_action,
                    context.getString(R.string.example_action_long_title),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.example_action_clicked))
                    .build();
        }
    }

    private static class ReplyActionPreset extends ActionsPreset {
        public ReplyActionPreset() {
            super(R.string.reply_action);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            RemoteInput remoteInput = new RemoteInput.Builder(NotificationUtil.EXTRA_REPLY)
                    .setLabel(context.getString(R.string.example_reply_label))
                    .build();
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_full_reply,
                    context.getString(R.string.example_reply_action),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.example_reply_action_clicked))
                    .addRemoteInput(remoteInput)
                    .build();
            builder.addAction(action);
        }
    }

    private static class ReplyWithChoicesActionPreset extends ActionsPreset {
        public ReplyWithChoicesActionPreset() {
            super(R.string.reply_action_with_choices);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            RemoteInput remoteInput = new RemoteInput.Builder(NotificationUtil.EXTRA_REPLY)
                    .setLabel(context.getString(R.string.example_reply_answer_label))
                    .setChoices(new String[] { context.getString(R.string.yes),
                            context.getString(R.string.no), context.getString(R.string.maybe) })
                    .build();
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_full_reply,
                    context.getString(R.string.example_reply_action),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.example_reply_action_clicked))
                    .addRemoteInput(remoteInput)
                    .build();
            wearableOptions.addAction(action);
        }
    }

    private static class DifferentActionsOnPhoneAndWearable extends ActionsPreset {
        public DifferentActionsOnPhoneAndWearable() {
            super(R.string.different_actions_on_phone_and_wearable);
        }

        @Override
        public void apply(Context context, NotificationCompat.Builder builder,
                NotificationCompat.WearableExtender wearableOptions) {
            NotificationCompat.Action phoneAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_full_action,
                    context.getString(R.string.phone_action),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.phone_action_clicked))
                    .build();
            builder.addAction(phoneAction);

            RemoteInput remoteInput = new RemoteInput.Builder(NotificationUtil.EXTRA_REPLY)
                    .setLabel(context.getString(R.string.example_reply_label))
                    .build();

            NotificationCompat.Action wearableAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_full_reply,
                    context.getString(R.string.wearable_action),
                    NotificationUtil.getExamplePendingIntent(context,
                            R.string.wearable_action_clicked))
                    .addRemoteInput(remoteInput)
                    .build();
            wearableOptions.addAction(wearableAction);
        }
    }
}
