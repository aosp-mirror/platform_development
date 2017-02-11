/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.wear.wearnotifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat.BigPictureStyle;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.app.NotificationCompat.MessagingStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.NotificationCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.BigTextIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.BigTextMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.InboxMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingMainActivity;
import com.example.android.wearable.wear.wearnotifications.mock.MockDatabase;

/**
 * Demonstrates best practice for {@link NotificationCompat} Notifications created by local
 * standalone Android Wear apps. All {@link NotificationCompat} examples use
 * {@link NotificationCompat.Style}.
 */
public class StandaloneMainActivity extends WearableActivity {

    private static final String TAG = "StandaloneMainActivity";

    public static final int NOTIFICATION_ID = 888;

    /*
     * Used to represent each major {@link NotificationCompat.Style} in the
     * {@link WearableRecyclerView}. These constants are also used in a switch statement when one
     * of the items is selected to create the appropriate {@link Notification}.
     */
    private static final String BIG_TEXT_STYLE = "BIG_TEXT_STYLE";
    private static final String BIG_PICTURE_STYLE = "BIG_PICTURE_STYLE";
    private static final String INBOX_STYLE = "INBOX_STYLE";
    private static final String MESSAGING_STYLE = "MESSAGING_STYLE";

    /*
    Collection of major {@link NotificationCompat.Style} to create {@link CustomRecyclerAdapter}
    for {@link WearableRecyclerView}.
    */
    private static final String[] NOTIFICATION_STYLES =
            {BIG_TEXT_STYLE, BIG_PICTURE_STYLE, INBOX_STYLE, MESSAGING_STYLE};

    private NotificationManagerCompat mNotificationManagerCompat;

    // Needed for {@link SnackBar} to alert users when {@link Notification} are disabled for app.
    private FrameLayout mMainFrameLayout;
    private WearableRecyclerView mWearableRecyclerView;
    private CustomRecyclerAdapter mCustomRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());

        mMainFrameLayout = (FrameLayout) findViewById(R.id.mainFrameLayout);
        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.setCenterEdgeItems(true);

        // Customizes scrolling (zoom) and offsets of WearableRecyclerView's items
        ScalingOffsettingHelper scalingOffsettingHelper = new ScalingOffsettingHelper();
        mWearableRecyclerView.setOffsettingHelper(scalingOffsettingHelper);

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true);

        // Specifies an adapter (see also next example).
        mCustomRecyclerAdapter = new CustomRecyclerAdapter(
                NOTIFICATION_STYLES,
                // Controller passes selected data from the Adapter out to this Activity to trigger
                // updates in the UI/Notifications.
                new Controller(this));

        mWearableRecyclerView.setAdapter(mCustomRecyclerAdapter);
    }

    // Called by WearableRecyclerView when an item is selected (check onCreate() for initialization)
    public void itemSelected(String data) {

        Log.d(TAG, "itemSelected()");

        boolean areNotificationsEnabled = mNotificationManagerCompat.areNotificationsEnabled();

        // If notifications are disabled, allow user to enable.
        if (!areNotificationsEnabled) {
            // Because the user took an action to create a notification, we create a prompt to let
            // the user re-enable notifications for this application again.
            Snackbar snackbar = Snackbar
                    .make(
                            mMainFrameLayout,
                            "", // Not enough space for both text and action text
                            Snackbar.LENGTH_LONG)
                    .setAction("Enable Notifications", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Links to this app's notification settings
                            openNotificationSettingsForApp();
                        }
                    });
            snackbar.show();
            return;
        }

        String notificationStyle = data;

        switch (notificationStyle) {
            case BIG_TEXT_STYLE:
                generateBigTextStyleNotification();
                break;

            case BIG_PICTURE_STYLE:
                generateBigPictureStyleNotification();
                break;

            case INBOX_STYLE:
                generateInboxStyleNotification();
                break;

            case MESSAGING_STYLE:
                generateMessagingStyleNotification();
                break;

            default:
                // continue below
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     *
     * IMPORTANT NOTE:
     * This method includes extra code to replicate Notification Styles behavior from Wear 1.+ and
     * phones on Wear 2.0, i.e., the notification expands on click. To see the specific code in the
     * method, search for "REPLICATE_NOTIFICATION_STYLE_CODE".
     *
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to see the new behavior, please review the generateBigPictureStyleNotification()
     * and generateMessagingStyleNotification() methods.
     */
    private void generateBigTextStyleNotification() {

        Log.d(TAG, "generateBigTextStyleNotification()");

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get your data
        //      1. Build the BIG_TEXT_STYLE
        //      2. Set up main Intent for notification
        //      3. Create additional Actions for the Notification
        //      4. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        MockDatabase.BigTextStyleReminderAppData bigTextStyleReminderAppData =
                MockDatabase.getBigTextStyleData();

        // 1. Build the BIG_TEXT_STYLE
        BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                // Overrides ContentText in the big form of the template
                .bigText(bigTextStyleReminderAppData.getBigText())
                // Overrides ContentTitle in the big form of the template
                .setBigContentTitle(bigTextStyleReminderAppData.getBigContentTitle())
                // Summary line after the detail section in the big form of the template
                // Note: To improve readability, don't overload the user with info. If Summary Text
                // doesn't add critical information, you should skip it.
                .setSummaryText(bigTextStyleReminderAppData.getSummaryText());


        // 2. Set up main Intent for notification
        Intent mainIntent = new Intent(this, BigTextMainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 3. Create additional Actions (Intents) for the Notification

        // In our case, we create two additional actions: a Snooze action and a Dismiss action.

        // Snooze Action
        Intent snoozeIntent = new Intent(this, BigTextIntentService.class);
        snoozeIntent.setAction(BigTextIntentService.ACTION_SNOOZE);

        PendingIntent snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0);
        NotificationCompat.Action snoozeAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_alarm_white_48dp,
                        "Snooze",
                        snoozePendingIntent)
                        .build();

        // Dismiss Action
        Intent dismissIntent = new Intent(this, BigTextIntentService.class);
        dismissIntent.setAction(BigTextIntentService.ACTION_DISMISS);

        PendingIntent dismissPendingIntent = PendingIntent.getService(this, 0, dismissIntent, 0);
        NotificationCompat.Action dismissAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_cancel_white_48dp,
                        "Dismiss",
                        dismissPendingIntent)
                        .build();


        // 4. Build and issue the notification

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext());

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content
                .setStyle(bigTextStyle)
                .setContentTitle(bigTextStyleReminderAppData.getContentTitle())
                .setContentText(bigTextStyleReminderAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_alarm_white_48dp))
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)

                // Shows content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PUBLIC)

                // Adds additional actions specified above
                .addAction(snoozeAction)
                .addAction(dismissAction);

        /* REPLICATE_NOTIFICATION_STYLE_CODE:
         * You can replicate Notification Style functionality on Wear 2.0 (24+) by not setting the
         * main content intent, that is, skipping the call setContentIntent(). However, you need to
         * still allow the user to open the native Wear app from the Notification itself, so you
         * add an action to launch the app.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            // Enables launching app in Wear 2.0 while keeping the old Notification Style behavior.
            NotificationCompat.Action mainAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher,
                    "Open",
                    mainPendingIntent)
                    .build();

            notificationCompatBuilder.addAction(mainAction);

        } else {
            // Wear 1.+ still functions the same, so we set the main content intent.
            notificationCompatBuilder.setContentIntent(mainPendingIntent);
        }


        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);

        // Close app to demonstrate notification in steam.
        finish();
    }

    /*
     * Generates a BIG_PICTURE_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     *
     * This example Notification is a social post. It allows updating the notification with
     * comments/responses via RemoteInput and the BigPictureSocialIntentService on 24+ (N+) and
     * Android Wear devices.
     *
     * IMPORTANT NOTE:
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to enable an action on your Notification without launching the app, you can do so
     * with the setHintDisplayActionInline() feature (shown below), but this only allows one action.
     *
     * If you wish to replicate the original experience of a bridged notification, please review the
     * generateBigTextStyleNotification() method above to see how.
     */
    private void generateBigPictureStyleNotification() {

        Log.d(TAG, "generateBigPictureStyleNotification()");

        // Main steps for building a BIG_PICTURE_STYLE notification:
        //      0. Get your data
        //      1. Build the BIG_PICTURE_STYLE
        //      2. Set up main Intent for notification
        //      3. Set up RemoteInput, so users can input (keyboard and voice) from notification
        //      4. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        MockDatabase.BigPictureStyleSocialAppData bigPictureStyleSocialAppData =
                MockDatabase.getBigPictureStyleData();

        // 1. Build the BIG_PICTURE_STYLE
        BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle()
                // Provides the bitmap for the BigPicture notification
                .bigPicture(
                        BitmapFactory.decodeResource(
                                getResources(),
                                bigPictureStyleSocialAppData.getBigImage()))
                // Overrides ContentTitle in the big form of the template
                .setBigContentTitle(bigPictureStyleSocialAppData.getBigContentTitle())
                // Summary line after the detail section in the big form of the template
                .setSummaryText(bigPictureStyleSocialAppData.getSummaryText());

        // 2. Set up main Intent for notification
        Intent mainIntent = new Intent(this, BigPictureSocialMainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 3. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.

        // Create the RemoteInput.
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput =
                new RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
                        .setLabel(replyLabel)
                        // List of quick response choices for any wearables paired with the phone
                        .setChoices(bigPictureStyleSocialAppData.getPossiblePostResponses())
                        .build();

        // Create PendingIntent for service that handles input.
        Intent replyIntent = new Intent(this, BigPictureSocialIntentService.class);
        replyIntent.setAction(BigPictureSocialIntentService.ACTION_COMMENT);
        PendingIntent replyActionPendingIntent = PendingIntent.getService(this, 0, replyIntent, 0);

        // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
        // lower portion of the Notification for easy action (only possible for one action).
        final NotificationCompat.Action.WearableExtender inlineActionForWear2 =
                new NotificationCompat.Action.WearableExtender()
                        .setHintDisplayActionInline(true)
                        .setHintLaunchesActivity(false);

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        // Add WearableExtender to enable inline actions
                        .extend(inlineActionForWear2)
                        .build();

        // 4. Build and issue the notification

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for a comment on the post.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext());

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        // Build notification
        notificationCompatBuilder
                // BIG_PICTURE_STYLE sets title and content
                .setStyle(bigPictureStyle)
                .setContentTitle(bigPictureStyleSocialAppData.getContentTitle())
                .setContentText(bigPictureStyleSocialAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                .setSubText(Integer.toString(1))
                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setPriority(Notification.PRIORITY_HIGH)

                // Hides content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                // Notifies system that the main launch intent is an Activity.
                .extend(new NotificationCompat.WearableExtender()
                        .setHintContentIntentLaunchesActivity(true));

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : bigPictureStyleSocialAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();
        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);

        // Close app to demonstrate notification in steam.
        finish();
    }

    /*
     * Generates a INBOX_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     */
    private void generateInboxStyleNotification() {

        Log.d(TAG, "generateInboxStyleNotification()");


        // Main steps for building a INBOX_STYLE notification:
        //      0. Get your data
        //      1. Build the INBOX_STYLE
        //      2. Set up main Intent for notification
        //      3. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        MockDatabase.InboxStyleEmailAppData inboxStyleEmailAppData =
                MockDatabase.getInboxStyleData();

        // 1. Build the INBOX_STYLE
        InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                // This title is slightly different than regular title, since I know INBOX_STYLE is
                // available.
                .setBigContentTitle(inboxStyleEmailAppData.getBigContentTitle())
                .setSummaryText(inboxStyleEmailAppData.getSummaryText());

        // Add each summary line of the new emails, you can add up to 5
        for (String summary : inboxStyleEmailAppData.getIndividualEmailSummary()) {
            inboxStyle.addLine(summary);
        }

        // 2. Set up main Intent for notification
        Intent mainIntent = new Intent(this, InboxMainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 3. Build and issue the notification

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. However, we don't need to update this notification later, so we
        // will not need to set a global builder for access to the notification later.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext());

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        // 4. Build and issue the notification
        notificationCompatBuilder
                // INBOX_STYLE sets title and content
                .setStyle(inboxStyle)
                .setContentTitle(inboxStyleEmailAppData.getContentTitle())
                .setContentText(inboxStyleEmailAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // Sets large number at the right-hand side of the notification for Wear 1.+.
                .setSubText(Integer.toString(inboxStyleEmailAppData.getNumberOfNewEmails()))

                .setCategory(Notification.CATEGORY_EMAIL)
                .setPriority(Notification.PRIORITY_HIGH)

                // Hides content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                // Notifies system that the main launch intent is an Activity.
                .extend(new NotificationCompat.WearableExtender()
                        .setHintContentIntentLaunchesActivity(true));

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : inboxStyleEmailAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();
        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);

        // Close app to demonstrate notification in steam.
        finish();
    }

    /*
     * Generates a MESSAGING_STYLE Notification that supports both Wear 1.+ and Wear 2.0. For
     * devices on API level 24 (Wear 2.0) and after, displays MESSAGING_STYLE. Otherwise, displays
     * a basic BIG_TEXT_STYLE.
     *
     * IMPORTANT NOTE:
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to enable an action on your Notification without launching the app, you can do so
     * with the setHintDisplayActionInline() feature (shown below), but this only allows one action.
     *
     * If you wish to replicate the original experience of a bridged notification, please review the
     * generateBigTextStyleNotification() method above to see how.
     */
    private void generateMessagingStyleNotification() {

        Log.d(TAG, "generateMessagingStyleNotification()");

        // Main steps for building a MESSAGING_STYLE notification:
        //      0. Get your data
        //      1. Build the MESSAGING_STYLE
        //      2. Add support for Wear 1.+
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput (users can input directly from notification)
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        MockDatabase.MessagingStyleCommsAppData messagingStyleCommsAppData =
                MockDatabase.getMessagingStyleData();

        // 1. Build the Notification.Style (MESSAGING_STYLE)
        String contentTitle = messagingStyleCommsAppData.getContentTitle();

        MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(messagingStyleCommsAppData.getReplayName())
                        // You could set a different title to appear when the messaging style
                        // is supported on device (24+) if you wish. In our case, we use the same
                        // title.
                        .setConversationTitle(contentTitle);

        // Adds all Messages
        // Note: Messages include the text, timestamp, and sender
        for (MessagingStyle.Message message : messagingStyleCommsAppData.getMessages()) {
            messagingStyle.addMessage(message);
        }


        // 2. Add support for Wear 1.+

        // Since Wear 1.0 doesn't support the MESSAGING_STYLE, we use the BIG_TEXT_STYLE, so all the
        // text is visible.

        // This is basically a toString() of all the Messages above.
        String fullMessageForWearVersion1 = messagingStyleCommsAppData.getFullConversation();

        Notification chatHistoryForWearV1 = new NotificationCompat.Builder(getApplicationContext())
                .setStyle(new BigTextStyle().bigText(fullMessageForWearVersion1))
                .setContentTitle(contentTitle)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText(fullMessageForWearVersion1)
                .build();

        // Adds page with all text to support Wear 1.+.
        NotificationCompat.WearableExtender wearableExtenderForWearVersion1 =
                new NotificationCompat.WearableExtender()
                        .setHintContentIntentLaunchesActivity(true)
                        .addPage(chatHistoryForWearV1);

        // 3. Set up main Intent for notification
        Intent notifyIntent = new Intent(this, MessagingMainActivity.class);

        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.

        // Create the RemoteInput specifying this key.
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput = new RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
                .setLabel(replyLabel)
                .build();

        // Create PendingIntent for service that handles input.
        Intent replyIntent = new Intent(this, MessagingIntentService.class);
        replyIntent.setAction(MessagingIntentService.ACTION_REPLY);
        PendingIntent replyActionPendingIntent = PendingIntent.getService(this, 0, replyIntent, 0);

        // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
        // lower portion of the Notification for easy action (only possible for one action).
        final NotificationCompat.Action.WearableExtender inlineActionForWear2 =
                new NotificationCompat.Action.WearableExtender()
                        .setHintDisplayActionInline(true)
                        .setHintLaunchesActivity(false);

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        // Allows system to generate replies by context of conversation
                        .setAllowGeneratedReplies(true)
                        // Add WearableExtender to enable inline actions
                        .extend(inlineActionForWear2)
                        .build();


        // 5. Build and issue the notification

        // Because we want this to be a new notification (not updating current notification), we
        // create a new Builder. Later, we update this same notification, so we need to save this
        // Builder globally (as outlined earlier).

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext());

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        // Builds and issues notification
        notificationCompatBuilder
                // MESSAGING_STYLE sets title and content for API 24+ (Wear 2.0) devices
                .setStyle(messagingStyle)
                // Title for API <24 (Wear 1.+) devices
                .setContentTitle(contentTitle)
                // Content for API <24 (Wear 1.+) devices
                .setContentText(messagingStyleCommsAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // Number of new notifications for API <24 (Wear 1.+) devices
                .setSubText(Integer.toString(messagingStyleCommsAppData.getNumberOfNewMessages()))

                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)

                // Hides content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PRIVATE)

                // Adds multiple pages for easy consumption on a wear device.
                .extend(wearableExtenderForWearVersion1);

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : messagingStyleCommsAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();
        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);

        // Close app to demonstrate notification in steam.
        finish();
    }

    /**
     * Helper method for the SnackBar action, i.e., if the user has this application's notifications
     * disabled, this opens up the dialog to turn them back on after the user requests a
     * Notification launch.
     *
     * IMPORTANT NOTE: You should not do this action unless the user takes an action to see your
     * Notifications like this sample demonstrates. Spamming users to re-enable your notifications
     * is a bad idea.
     */
    private void openNotificationSettingsForApp() {
        // Links to this app's notification settings
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", getPackageName());
        intent.putExtra("app_uid", getApplicationInfo().uid);
        startActivity(intent);
    }
}