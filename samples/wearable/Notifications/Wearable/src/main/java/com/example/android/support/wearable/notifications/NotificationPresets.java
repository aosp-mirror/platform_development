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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;

/**
 * Collection of notification builder presets.
 */
public class NotificationPresets {
    public static final NotificationPreset[] PRESETS = new NotificationPreset[] {
            new BasicPreset(),
            new StylizedTextPreset(),
            new DisplayIntentPreset(),
            new MultiSizeDisplayIntentPreset(),
            new AnimatedDisplayIntentPreset(),
            new ContentIconPreset()
    };

    private static Notification.Builder buildBasicNotification(Context context) {
        return new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.example_content_title))
                .setContentText(context.getString(R.string.example_content_text))
                // Set a content intent to return to this sample
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class), 0))
                .setSmallIcon(R.mipmap.ic_launcher);
    }

    private static class BasicPreset extends NotificationPreset {
        public BasicPreset() {
            super(R.string.basic_example);
        }

        @Override
        public Notification buildNotification(Context context) {
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class), 0);

            Notification page2 = buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setHintShowBackgroundOnly(true)
                            .setBackground(BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.example_big_picture)))
                    .build();

            Notification page3 = buildBasicNotification(context)
                    .setContentTitle(context.getString(R.string.third_page))
                    .setContentText(null)
                    .extend(new Notification.WearableExtender()
                            .setContentAction(0 /* action A */))
                    .build();

            SpannableStringBuilder choice2 = new SpannableStringBuilder(
                    "This choice is best");
            choice2.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 5, 11, 0);

            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .addAction(new Notification.Action(R.mipmap.ic_launcher,
                                    context.getString(R.string.action_a), pendingIntent))
                            .addAction(new Notification.Action.Builder(R.mipmap.ic_launcher,
                                    context.getString(R.string.reply), pendingIntent)
                                    .addRemoteInput(new RemoteInput.Builder(MainActivity.KEY_REPLY)
                                            .setChoices(new CharSequence[] {
                                                    context.getString(R.string.choice_1),
                                                    choice2 })
                                            .build())
                                    .build())
                            .addPage(page2)
                            .addPage(page3))
                    .build();
        }
    }

    private static class StylizedTextPreset extends NotificationPreset {
        public StylizedTextPreset() {
            super(R.string.stylized_text_example);
        }

        @Override
        public Notification buildNotification(Context context) {
            Notification.Builder builder = buildBasicNotification(context);

            Notification.BigTextStyle style = new Notification.BigTextStyle();

            SpannableStringBuilder title = new SpannableStringBuilder();
            appendStyled(title, "Stylized", new StyleSpan(Typeface.BOLD_ITALIC));
            title.append(" title");
            SpannableStringBuilder text = new SpannableStringBuilder("Stylized text: ");
            appendStyled(text, "C", new ForegroundColorSpan(Color.RED));
            appendStyled(text, "O", new ForegroundColorSpan(Color.GREEN));
            appendStyled(text, "L", new ForegroundColorSpan(Color.BLUE));
            appendStyled(text, "O", new ForegroundColorSpan(Color.YELLOW));
            appendStyled(text, "R", new ForegroundColorSpan(Color.MAGENTA));
            appendStyled(text, "S", new ForegroundColorSpan(Color.CYAN));
            text.append("; ");
            appendStyled(text, "1.25x size", new RelativeSizeSpan(1.25f));
            text.append("; ");
            appendStyled(text, "0.75x size", new RelativeSizeSpan(0.75f));
            text.append("; ");
            appendStyled(text, "underline", new UnderlineSpan());
            text.append("; ");
            appendStyled(text, "strikethrough", new StrikethroughSpan());
            text.append("; ");
            appendStyled(text, "bold", new StyleSpan(Typeface.BOLD));
            text.append("; ");
            appendStyled(text, "italic", new StyleSpan(Typeface.ITALIC));
            text.append("; ");
            appendStyled(text, "sans-serif-thin", new TypefaceSpan("sans-serif-thin"));
            text.append("; ");
            appendStyled(text, "monospace", new TypefaceSpan("monospace"));
            text.append("; ");
            appendStyled(text, "sub", new SubscriptSpan());
            text.append("script");
            appendStyled(text, "super", new SuperscriptSpan());

            style.setBigContentTitle(title);
            style.bigText(text);

            builder.setStyle(style);
            return builder.build();
        }

        private void appendStyled(SpannableStringBuilder builder, String str, Object... spans) {
            builder.append(str);
            for (Object span : spans) {
                builder.setSpan(span, builder.length() - str.length(), builder.length(), 0);
            }
        }
    }

    private static class DisplayIntentPreset extends NotificationPreset {
        public DisplayIntentPreset() {
            super(R.string.display_intent_example);
        }

        @Override
        public Notification buildNotification(Context context) {
            Intent displayIntent = new Intent(context, BasicNotificationDisplayActivity.class);
            displayIntent.putExtra(BasicNotificationDisplayActivity.EXTRA_TITLE,
                    context.getString(nameResId));
            PendingIntent displayPendingIntent = PendingIntent.getActivity(context,
                    0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(displayPendingIntent))
                    .build();
        }
    }

    private static class MultiSizeDisplayIntentPreset extends NotificationPreset {
        public MultiSizeDisplayIntentPreset() {
            super(R.string.multisize_display_intent_example);
        }

        @Override
        public Notification buildNotification(Context context) {
           PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                                   new Intent(context, MainActivity.class), 0);
             Intent displayIntent = new Intent(context, BasicNotificationDisplayActivity.class)
                    .putExtra(BasicNotificationDisplayActivity.EXTRA_TITLE,
                            context.getString(R.string.xsmall_sized_display));
            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(PendingIntent.getActivity(context, 0, displayIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT))
                            .addPage(createPageForSizePreset(context,
                                    Notification.WearableExtender.SIZE_SMALL,
                                    R.string.small_sized_display, 0))
                            .addPage(createPageForSizePreset(context,
                                    Notification.WearableExtender.SIZE_MEDIUM,
                                    R.string.medium_sized_display, 1))
                            .addPage(createPageForSizePreset(context,
                                    Notification.WearableExtender.SIZE_LARGE,
                                    R.string.large_sized_display, 2))
                            .addPage(createPageForSizePreset(context,
                                    Notification.WearableExtender.SIZE_FULL_SCREEN,
                                    R.string.full_screen_display, 3))
                             .addPage(createPageForCustomHeight(context, 256,
                                    R.string.dp256_height_display))
                            .addPage(createPageForCustomHeight(context, 512,
                                    R.string.dp512_height_display))
                            .addAction(new Notification.Action(R.mipmap.ic_launcher,
                                    context.getString(R.string.action_a), pendingIntent))
                            .addAction(new Notification.Action(R.mipmap.ic_launcher,
                                    context.getString(R.string.action_b), pendingIntent))
                            .addAction(new Notification.Action(R.mipmap.ic_launcher,
                                    context.getString(R.string.action_c), pendingIntent))
                            .addAction(new Notification.Action(R.mipmap.ic_launcher,
                                    context.getString(R.string.action_d), pendingIntent))
                            .setCustomSizePreset(Notification.WearableExtender.SIZE_XSMALL))
                    .build();
        }

        private Notification createPageForCustomHeight(Context context, int heightDisplayDp,
                int pageNameResId) {
            int contentHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    heightDisplayDp, context.getResources().getDisplayMetrics());
            Intent displayIntent = new Intent(context, BasicNotificationDisplayActivity.class)
                    .setData(Uri.fromParts("example", "height/" + heightDisplayDp, null))
                    .putExtra(BasicNotificationDisplayActivity.EXTRA_TITLE,
                            context.getString(pageNameResId));
            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(PendingIntent.getActivity(context, 0, displayIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT))
                            .setCustomContentHeight(contentHeight))
                    .build();
        }

        private Notification createPageForSizePreset(Context context, int sizePreset,
                int pageNameResId, int contentAction) {
           Intent displayIntent = new Intent(context, BasicNotificationDisplayActivity.class)
                    .setData(Uri.fromParts("example", "size/" + sizePreset, null))
                    .putExtra(BasicNotificationDisplayActivity.EXTRA_TITLE,
                            context.getString(pageNameResId));
            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(PendingIntent.getActivity(context, 0, displayIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT))
                            .setCustomSizePreset(sizePreset)
                            .setContentAction(contentAction))
                    .build();
        }
    }

    private static class AnimatedDisplayIntentPreset extends NotificationPreset {
        public AnimatedDisplayIntentPreset() {
            super(R.string.animated_display_intent_example);
        }

        @Override
        public Notification buildNotification(Context context) {
            Intent displayIntent = new Intent(context, AnimatedNotificationDisplayActivity.class);
            displayIntent.putExtra(BasicNotificationDisplayActivity.EXTRA_TITLE,
                    context.getString(nameResId));
            PendingIntent displayPendingIntent = PendingIntent.getActivity(context,
                    0, displayIntent, 0);
            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setDisplayIntent(displayPendingIntent))
                    .build();
        }
    }

    private static class ContentIconPreset extends NotificationPreset {
        public ContentIconPreset() {
            super(R.string.content_icon_example);
        }

        @Override
        public Notification buildNotification(Context context) {
            Notification page2 = buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_small)
                            .setContentIconGravity(Gravity.START))
                    .build();

            Notification page3 = buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_large))
                    .build();

            Notification page4 = buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_large)
                            .setContentIconGravity(Gravity.START))
                    .build();

            return buildBasicNotification(context)
                    .extend(new Notification.WearableExtender()
                            .setHintHideIcon(true)
                            .setContentIcon(R.drawable.content_icon_small)
                            .addPage(page2)
                            .addPage(page3)
                            .addPage(page4))
                    .build();
        }
    }
}
