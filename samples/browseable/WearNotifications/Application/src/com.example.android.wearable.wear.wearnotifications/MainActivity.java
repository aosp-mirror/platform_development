/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;



import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.BigTextIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.BigTextMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.InboxMainActivity;
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingIntentService;
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingMainActivity;
import com.example.android.wearable.wear.wearnotifications.mock.MockDatabase;

/**
 * The Activity demonstrates several popular Notification.Style examples along with their best
 * practices (include proper Android Wear support when you don't have a dedicated Android Wear
 * app).
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static final String TAG = "MainActivity";

    public static final int NOTIFICATION_ID = 888;

    // Used for Notification Style array and switch statement for Spinner selection
    private static final String BIG_TEXT_STYLE = "BIG_TEXT_STYLE";
    private static final String BIG_PICTURE_STYLE = "BIG_PICTURE_STYLE";
    private static final String INBOX_STYLE = "INBOX_STYLE";
    private static final String MESSAGING_STYLE = "MESSAGING_STYLE";

    // Collection of notification styles to back ArrayAdapter for Spinner
    private static final String[] NOTIFICATION_STYLES =
            {BIG_TEXT_STYLE, BIG_PICTURE_STYLE, INBOX_STYLE, MESSAGING_STYLE};

    private static final String[] NOTIFICATION_STYLES_DESCRIPTION =
            {
                    "Demos reminder type app using BIG_TEXT_STYLE",
                    "Demos social type app using BIG_PICTURE_STYLE + inline notification response",
                    "Demos email type app using INBOX_STYLE",
                    "Demos messaging app using MESSAGING_STYLE + inline notification responses"
            };

    private NotificationManagerCompat mNotificationManagerCompat;

    private int mSelectedNotification = 0;

    // RelativeLayout is needed for SnackBars to alert users when Notifications are disabled for app
    private RelativeLayout mMainRelativeLayout;
    private Spinner mSpinner;
    private TextView mNotificationDetailsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainRelativeLayout = (RelativeLayout) findViewById(R.id.mainRelativeLayout);
        mNotificationDetailsTextView = (TextView) findViewById(R.id.notificationDetails);
        mSpinner = (Spinner) findViewById(R.id.spinner);

        mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        NOTIFICATION_STYLES);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected(): position: " + position + " id: " + id);

        mSelectedNotification = position;

        mNotificationDetailsTextView.setText(
                NOTIFICATION_STYLES_DESCRIPTION[mSelectedNotification]);
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Required
    }

    public void onClick(View view) {

        Log.d(TAG, "onClick()");

        boolean areNotificationsEnabled = mNotificationManagerCompat.areNotificationsEnabled();

        if (!areNotificationsEnabled) {
            // Because the user took an action to create a notification, we create a prompt to let
            // the user re-enable notifications for this application again.
            Snackbar snackbar = Snackbar
                    .make(
                            mMainRelativeLayout,
                            "You need to enable notifications for this app",
                            Snackbar.LENGTH_LONG)
                    .setAction("ENABLE", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Links to this app's notification settings
                            openNotificationSettingsForApp();
                        }
                    });
            snackbar.show();
            return;
        }

        String notificationStyle = NOTIFICATION_STYLES[mSelectedNotification];

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
     * Generates a BIG_TEXT_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_TEXT_STYLE. Otherwise, displays
     * a basic notification.
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
        Intent notifyIntent = new Intent(this, BigTextMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // For the BIG_TEXT_STYLE notification, we will consider the activity launched by the main
        // Intent as a special activity, so we will follow option 2.

        // For an example of option 1, check either the MESSAGING_STYLE or BIG_PICTURE_STYLE
        // examples.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 3. Create additional Actions (Intents) for the Notification

        // In our case, we create two additional actions: a Snooze action and a Dismiss action
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

        Notification notification = notificationCompatBuilder
                // BIG_TEXT_STYLE sets title and content for API 16 (4.1 and after)
                .setStyle(bigTextStyle)
                // Title for API <16 (4.0 and below) devices
                .setContentTitle(bigTextStyleReminderAppData.getContentTitle())
                // Content for API <24 (7.0 and below) devices
                .setContentText(bigTextStyleReminderAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_alarm_white_48dp))
                .setContentIntent(notifyPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Android Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)

                // Shows content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PUBLIC)

                // Adds additional actions specified above
                .addAction(snoozeAction)
                .addAction(dismissAction)

                .build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a BIG_PICTURE_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 16 (4.1.x - Jelly Bean) and after, displays BIG_PICTURE_STYLE.
     * Otherwise, displays a basic notification.
     *
     * This example Notification is a social post. It allows updating the notification with
     * comments/responses via RemoteInput and the BigPictureSocialIntentService on 24+ (N+) and
     * Android Wear devices.
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

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a social app generally
        // always links to individual posts as part of the app flow, so we will follow option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(BigPictureSocialMainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(mainIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        // 3. Set up RemoteInput, so users can input (keyboard and voice) from notification

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen presents
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput =
                new RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
                        .setLabel(replyLabel)
                        // List of quick response choices for any wearables paired with the phone
                        .setChoices(bigPictureStyleSocialAppData.getPossiblePostResponses())
                        .build();

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver
        PendingIntent replyActionPendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, BigPictureSocialIntentService.class);
            intent.setAction(BigPictureSocialIntentService.ACTION_COMMENT);
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        } else {
            replyActionPendingIntent = mainPendingIntent;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        // 4. Build and issue the notification

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for a comment on the post.

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(getApplicationContext());

        GlobalNotificationBuilder.setNotificationCompatBuilderInstance(notificationCompatBuilder);

        // 4. Build and issue the notification
        notificationCompatBuilder
                // BIG_PICTURE_STYLE sets title and content for API 16 (4.1 and after)
                .setStyle(bigPictureStyle)
                // Title for API <16 (4.0 and below) devices
                .setContentTitle(bigPictureStyleSocialAppData.getContentTitle())
                // Content for API <24 (7.0 and below) devices
                .setContentText(bigPictureStyleSocialAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Android Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                .setSubText(Integer.toString(1))
                .addAction(replyAction)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setPriority(Notification.PRIORITY_HIGH)

                // Hides content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PRIVATE);

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : bigPictureStyleSocialAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a INBOX_STYLE Notification that supports both phone/tablet and wear. For devices
     * on API level 16 (4.1.x - Jelly Bean) and after, displays INBOX_STYLE. Otherwise, displays a
     * basic notification.
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

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a eamil app generally
        // always links to individual emails as part of the app flow, so we will follow option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(InboxMainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(mainIntent);
        // Gets a PendingIntent containing the entire back stack
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

                // INBOX_STYLE sets title and content for API 16+ (4.1 and after) when the
                // notification is expanded
                .setStyle(inboxStyle)

                // Title for API <16 (4.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed
                .setContentTitle(inboxStyleEmailAppData.getContentTitle())

                // Content for API <24 (7.0 and below) devices and API 16+ (4.1 and after) when the
                // notification is collapsed
                .setContentText(inboxStyleEmailAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Android Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Sets large number at the right-hand side of the notification for API <24 devices
                .setSubText(Integer.toString(inboxStyleEmailAppData.getNumberOfNewEmails()))

                .setCategory(Notification.CATEGORY_EMAIL)
                .setPriority(Notification.PRIORITY_HIGH)

                // Hides content on the lock-screen
                .setVisibility(Notification.VISIBILITY_PRIVATE);

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (String name : inboxStyleEmailAppData.getParticipants()) {
            notificationCompatBuilder.addPerson(name);
        }

        Notification notification = notificationCompatBuilder.build();

        mNotificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    /*
     * Generates a MESSAGING_STYLE Notification that supports both phone/tablet and wear. For
     * devices on API level 24 (7.0 - Nougat) and after, displays MESSAGING_STYLE. Otherwise,
     * displays a basic BIG_TEXT_STYLE.
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
                        .addPage(chatHistoryForWearV1);



        // 3. Set up main Intent for notification
        Intent notifyIntent = new Intent(this, MessagingMainActivity.class);

        // When creating your Intent, you need to take into account the back state, i.e., what
        // happens after your Activity launches and the user presses the back button.

        // There are two options:
        //      1. Regular activity - You're starting an Activity that's part of the application's
        //      normal workflow.

        //      2. Special activity - The user only sees this Activity if it's started from a
        //      notification. In a sense, the Activity extends the notification by providing
        //      information that would be hard to display in the notification itself.

        // Even though this sample's MainActivity doesn't link to the Activity this Notification
        // launches directly, i.e., it isn't part of the normal workflow, a chat app generally
        // always links to individual conversations as part of the app flow, so we will follow
        // option 1.

        // For an example of option 2, check out the BIG_TEXT_STYLE example.

        // For more information, check out our dev article:
        // https://developer.android.com/training/notify-user/navigation.html

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(MessagingMainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        // 4. Set up RemoteInput, so users can input (keyboard and voice) from notification

        // Note: For API <24 (M and below) we need to use an Activity, so the lock-screen present
        // the auth challenge. For API 24+ (N and above), we use a Service (could be a
        // BroadcastReceiver), so the user can input from Notification or lock-screen (they have
        // choice to allow) without leaving the notification.

        // Create the RemoteInput specifying this key
        String replyLabel = getString(R.string.reply_label);
        RemoteInput remoteInput = new RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
                .setLabel(replyLabel)
                .build();

        // Pending intent =
        //      API <24 (M and below): activity so the lock-screen presents the auth challenge
        //      API 24+ (N and above): this should be a Service or BroadcastReceiver
        PendingIntent replyActionPendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, MessagingIntentService.class);
            intent.setAction(MessagingIntentService.ACTION_REPLY);
            replyActionPendingIntent = PendingIntent.getService(this, 0, intent, 0);

        } else {
            replyActionPendingIntent = mainPendingIntent;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_reply_white_18dp,
                        replyLabel,
                        replyActionPendingIntent)
                        .addRemoteInput(remoteInput)
                        // Allows system to generate replies by context of conversation
                        .setAllowGeneratedReplies(true)
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
                // MESSAGING_STYLE sets title and content for API 24+ (N and above) devices
                .setStyle(messagingStyle)
                // Title for API <24 (M and below) devices
                .setContentTitle(contentTitle)
                // Content for API <24 (M and below) devices
                .setContentText(messagingStyleCommsAppData.getContentText())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.ic_person_black_48dp))
                .setContentIntent(mainPendingIntent)
                // Set primary color (important for Wear 2.0 Notifications)
                .setColor(getResources().getColor(R.color.colorPrimary))

                // SIDE NOTE: Auto-bundling is enabled for 4 or more notifications on API 24+ (N+)
                // devices and all Android Wear devices. If you have more than one notification and
                // you prefer a different summary notification, set a group key and create a
                // summary notification via
                // .setGroupSummary(true)
                // .setGroup(GROUP_KEY_YOUR_NAME_HERE)

                // Number of new notifications for API <24 (M and below) devices
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