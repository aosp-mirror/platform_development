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
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;

/**
 * Collection of notification builder presets.
 */
public class NotificationPresets {
    private static final String EXAMPLE_GROUP_KEY = "example";

    public static final NotificationPreset BASIC = new BasicNotificationPreset();
    public static final NotificationPreset STYLIZED_TEXT = new StylizedTextNotificationPreset();
    public static final NotificationPreset INBOX = new InboxNotificationPreset();
    public static final NotificationPreset BIG_PICTURE = new BigPictureNotificationPreset();
    public static final NotificationPreset BIG_TEXT = new BigTextNotificationPreset();
    public static final NotificationPreset BOTTOM_ALIGNED = new BottomAlignedNotificationPreset();
    public static final NotificationPreset GRAVITY = new GravityNotificationPreset();
    public static final NotificationPreset CONTENT_ACTION = new ContentActionNotificationPreset();
    public static final NotificationPreset CONTENT_ICON = new ContentIconNotificationPreset();
    public static final NotificationPreset MULTIPLE_PAGE = new MultiplePageNotificationPreset();
    public static final NotificationPreset BUNDLE = new NotificationBundlePreset();
    public static final NotificationPreset BARCODE = new NotificationBarcodePreset();

    public static final NotificationPreset[] PRESETS = new NotificationPreset[] {
            BASIC,
            STYLIZED_TEXT,
            INBOX,
            BIG_PICTURE,
            BIG_TEXT,
            BOTTOM_ALIGNED,
            GRAVITY,
            CONTENT_ACTION,
            CONTENT_ICON,
            MULTIPLE_PAGE,
            BUNDLE,
            BARCODE
    };

    private static NotificationCompat.Builder applyBasicOptions(Context context,
            NotificationCompat.Builder builder, NotificationCompat.WearableExtender wearableOptions,
            NotificationPreset.BuildOptions options) {
        builder.setContentTitle(options.titlePreset)
                .setContentText(options.textPreset)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDeleteIntent(NotificationUtil.getExamplePendingIntent(
                        context, R.string.example_notification_deleted));
        options.actionsPreset.apply(context, builder, wearableOptions);
        options.priorityPreset.apply(builder, wearableOptions);
        if (options.includeLargeIcon) {
            builder.setLargeIcon(BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.example_large_icon));
        }
        if (options.isLocalOnly) {
            builder.setLocalOnly(true);
        }
        if (options.hasContentIntent) {
            builder.setContentIntent(NotificationUtil.getExamplePendingIntent(context,
                    R.string.content_intent_clicked));
        }
        if (options.vibrate) {
            builder.setVibrate(new long[] {0, 100, 50, 100} );
        }
        return builder;
    }

    private static class BasicNotificationPreset extends NotificationPreset {
        public BasicNotificationPreset() {
            super(R.string.basic_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class StylizedTextNotificationPreset extends NotificationPreset {
        public StylizedTextNotificationPreset() {
            super(R.string.stylized_text_example, R.string.example_content_title,
                    R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();

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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setStyle(style);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }

        private void appendStyled(SpannableStringBuilder builder, String str, Object... spans) {
            builder.append(str);
            for (Object span : spans) {
                builder.setSpan(span, builder.length() - str.length(), builder.length(), 0);
            }
        }
    }

    private static class InboxNotificationPreset extends NotificationPreset {
        public InboxNotificationPreset() {
            super(R.string.inbox_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            style.addLine(context.getString(R.string.inbox_style_example_line1));
            style.addLine(context.getString(R.string.inbox_style_example_line2));
            style.addLine(context.getString(R.string.inbox_style_example_line3));
            style.setBigContentTitle(context.getString(R.string.inbox_style_example_title));
            style.setSummaryText(context.getString(R.string.inbox_style_example_summary_text));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setStyle(style);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class BigPictureNotificationPreset extends NotificationPreset {
        public BigPictureNotificationPreset() {
            super(R.string.big_picture_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
            style.bigPicture(BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.example_big_picture));
            style.setBigContentTitle(context.getString(R.string.big_picture_style_example_title));
            style.setSummaryText(context.getString(
                    R.string.big_picture_style_example_summary_text));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setStyle(style);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class BigTextNotificationPreset extends NotificationPreset {
        public BigTextNotificationPreset() {
            super(R.string.big_text_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.bigText(context.getString(R.string.big_text_example_big_text));
            style.setBigContentTitle(context.getString(R.string.big_text_example_title));
            style.setSummaryText(context.getString(R.string.big_text_example_summary_text));

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setStyle(style);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class BottomAlignedNotificationPreset extends NotificationPreset {
        public BottomAlignedNotificationPreset() {
            super(R.string.bottom_aligned_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);

            NotificationCompat.Builder secondPageBuilder = new NotificationCompat.Builder(context);
            secondPageBuilder.setContentTitle(
                    context.getString(R.string.second_page_content_title));
            secondPageBuilder.setContentText(context.getString(R.string.big_text_example_big_text));
            secondPageBuilder.extend(new NotificationCompat.WearableExtender()
                            .setStartScrollBottom(true));

            wearableOptions.addPage(secondPageBuilder.build());
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class GravityNotificationPreset extends NotificationPreset {
        public GravityNotificationPreset() {
            super(R.string.gravity_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);

            NotificationCompat.Builder secondPageBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(options.titlePreset)
                    .setContentText(options.textPreset)
                    .extend(new NotificationCompat.WearableExtender()
                            .setGravity(Gravity.CENTER_VERTICAL));
            wearableOptions.addPage(secondPageBuilder.build());

            NotificationCompat.Builder thirdPageBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(options.titlePreset)
                    .setContentText(options.textPreset)
                    .extend(new NotificationCompat.WearableExtender()
                            .setGravity(Gravity.TOP));
            wearableOptions.addPage(thirdPageBuilder.build());

            wearableOptions.setGravity(Gravity.BOTTOM);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class ContentActionNotificationPreset extends NotificationPreset {
        public ContentActionNotificationPreset() {
            super(R.string.content_action_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            Notification secondPage = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.second_page_content_title))
                    .setContentText(context.getString(R.string.second_page_content_text))
                    .extend(new NotificationCompat.WearableExtender()
                            .setContentAction(1))
                    .build();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_result_open, null, NotificationUtil.getExamplePendingIntent(
                            context, R.string.example_content_action_clicked)).build();
            NotificationCompat.Action action2 = new NotificationCompat.Action.Builder(
                    R.drawable.ic_result_open, null, NotificationUtil.getExamplePendingIntent(
                            context, R.string.example_content_action2_clicked)).build();
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender()
                            .addAction(action)
                            .addAction(action2)
                            .addPage(secondPage)
                            .setContentAction(0)
                            .setHintHideIcon(true);
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }

        @Override
        public boolean actionsRequired() {
            return true;
        }
    }

    private static class ContentIconNotificationPreset extends NotificationPreset {
        public ContentIconNotificationPreset() {
            super(R.string.content_icon_example, R.string.example_content_title,
                    R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            Notification secondPage = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.second_page_content_title))
                    .setContentText(context.getString(R.string.second_page_content_text))
                    .extend(new NotificationCompat.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_small)
                            .setContentIconGravity(Gravity.START))
                    .build();

            Notification thirdPage = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.third_page_content_title))
                    .setContentText(context.getString(R.string.third_page_content_text))
                    .extend(new NotificationCompat.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_large))
                    .build();

            Notification fourthPage = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.fourth_page_content_title))
                    .setContentText(context.getString(R.string.fourth_page_content_text))
                    .extend(new NotificationCompat.WearableExtender()
                            .setContentIcon(R.drawable.content_icon_large)
                            .setContentIconGravity(Gravity.START))
                    .build();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender()
                            .setHintHideIcon(true)
                            .setContentIcon(R.drawable.content_icon_small)
                            .addPage(secondPage)
                            .addPage(thirdPage)
                            .addPage(fourthPage);
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }

    private static class MultiplePageNotificationPreset extends NotificationPreset {
        public MultiplePageNotificationPreset() {
            super(R.string.multiple_page_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder secondPageBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.second_page_content_title))
                    .setContentText(context.getString(R.string.second_page_content_text));

            NotificationCompat.Builder firstPageBuilder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender firstPageWearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, firstPageBuilder, firstPageWearableOptions, options);

            Integer firstBackground = options.backgroundIds == null
                    ? null : options.backgroundIds[0];
            if (firstBackground != null) {
                NotificationCompat.BigPictureStyle style =
                        new NotificationCompat.BigPictureStyle();
                style.bigPicture(BitmapFactory.decodeResource(context.getResources(),
                        firstBackground));
                firstPageBuilder.setStyle(style);
            }

            Integer secondBackground = options.backgroundIds == null
                    ? null : options.backgroundIds[1];
            if (secondBackground != null) {
                NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
                style.bigPicture(BitmapFactory.decodeResource(context.getResources(),
                        secondBackground));
                secondPageBuilder.setStyle(style);
            }

            firstPageBuilder.extend(
                    firstPageWearableOptions.addPage(secondPageBuilder.build()));

            return new Notification[]{ firstPageBuilder.build() };
        }

        @Override
        public int countBackgroundPickersRequired() {
            return 2; // This sample does 2 pages notifications.
        }
    }

    private static class NotificationBundlePreset extends NotificationPreset {
        public NotificationBundlePreset() {
            super(R.string.bundle_example, R.string.example_content_title,
                R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder childBuilder1 = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.first_child_content_title))
                    .setContentText(context.getString(R.string.first_child_content_text))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLocalOnly(options.isLocalOnly)
                    .setGroup(EXAMPLE_GROUP_KEY)
                    .setSortKey("0");

            NotificationCompat.Builder childBuilder2 = new NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.second_child_content_title))
                    .setContentText(context.getString(R.string.second_child_content_text))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .addAction(R.mipmap.ic_launcher,
                            context.getString(R.string.second_child_action),
                            NotificationUtil.getExamplePendingIntent(
                                    context, R.string.second_child_action_clicked))
                    .setLocalOnly(options.isLocalOnly)
                    .setGroup(EXAMPLE_GROUP_KEY)
                    .setSortKey("1");

            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context)
                    .setGroup(EXAMPLE_GROUP_KEY)
                    .setGroupSummary(true);

            NotificationCompat.WearableExtender summaryWearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, summaryBuilder, summaryWearableOptions, options);
            summaryBuilder.extend(summaryWearableOptions);

            return new Notification[] { summaryBuilder.build(), childBuilder1.build(),
                    childBuilder2.build() };
        }
    }

    private static class NotificationBarcodePreset extends NotificationPreset {
        public NotificationBarcodePreset() {
            super(R.string.barcode_example, R.string.barcode_content_title,
                    R.string.barcode_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder secondPageBuilder = new NotificationCompat.Builder(context)
                    .extend(new NotificationCompat.WearableExtender()
                            .setHintShowBackgroundOnly(true)
                            .setBackground(BitmapFactory.decodeResource(context.getResources(),
                                    R.drawable.qr_code))
                            .setHintAvoidBackgroundClipping(true)
                            .setHintScreenTimeout(
                                    NotificationCompat.WearableExtender.SCREEN_TIMEOUT_LONG));

            NotificationCompat.Builder firstPageBuilder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender firstPageWearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, firstPageBuilder, firstPageWearableOptions, options);

            firstPageBuilder.extend(
                    firstPageWearableOptions.addPage(secondPageBuilder.build()));

            return new Notification[]{ firstPageBuilder.build() };
        }
    }
}
