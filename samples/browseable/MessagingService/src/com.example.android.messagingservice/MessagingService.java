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

package com.example.android.messagingservice;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat.CarExtender;
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class MessagingService extends Service {
    private static final String TAG = MessagingService.class.getSimpleName();
    private static final String EOL = "\n";
    private static final String READ_ACTION =
            "com.example.android.messagingservice.ACTION_MESSAGE_READ";

    public static final String REPLY_ACTION =
            "com.example.android.messagingservice.ACTION_MESSAGE_REPLY";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    public static final int MSG_SEND_NOTIFICATION = 1;

    private NotificationManagerCompat mNotificationManager;

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mNotificationManager = NotificationManagerCompat.from(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mMessenger.getBinder();
    }

    // Creates an intent that will be triggered when a message is marked as read.
    private Intent getMessageReadIntent(int id) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(READ_ACTION)
                .putExtra(CONVERSATION_ID, id);
    }

    // Creates an Intent that will be triggered when a voice reply is received.
    private Intent getMessageReplyIntent(int conversationId) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(REPLY_ACTION)
                .putExtra(CONVERSATION_ID, conversationId);
    }

    private void sendNotification(int howManyConversations, int messagesPerConversation) {
        Conversations.Conversation[] conversations = Conversations.getUnreadConversations(
                howManyConversations, messagesPerConversation);
        for (Conversations.Conversation conv : conversations) {
            sendNotificationForConversation(conv);
        }
    }

    private void sendNotificationForConversation(Conversations.Conversation conversation) {
        // A pending Intent for reads
        PendingIntent readPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                conversation.getConversationId(),
                getMessageReadIntent(conversation.getConversationId()),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Build a RemoteInput for receiving voice input in a Car Notification
        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                .setLabel(getApplicationContext().getString(R.string.notification_reply))
                .build();

        // Building a Pending Intent for the reply action to trigger
        PendingIntent replyIntent = PendingIntent.getBroadcast(getApplicationContext(),
                conversation.getConversationId(),
                getMessageReplyIntent(conversation.getConversationId()),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the UnreadConversation and populate it with the participant name,
        // read and reply intents.
        UnreadConversation.Builder unreadConvBuilder =
                new UnreadConversation.Builder(conversation.getParticipantName())
                .setLatestTimestamp(conversation.getTimestamp())
                .setReadPendingIntent(readPendingIntent)
                .setReplyAction(replyIntent, remoteInput);

        // Note: Add messages from oldest to newest to the UnreadConversation.Builder
        StringBuilder messageForNotification = new StringBuilder();
        for (Iterator<String> messages = conversation.getMessages().iterator();
             messages.hasNext(); ) {
            String message = messages.next();
            unreadConvBuilder.addMessage(message);
            messageForNotification.append(message);
            if (messages.hasNext()) {
                messageForNotification.append(EOL);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(
                        getApplicationContext().getResources(), R.drawable.android_contact))
                .setContentText(messageForNotification.toString())
                .setWhen(conversation.getTimestamp())
                .setContentTitle(conversation.getParticipantName())
                .setContentIntent(readPendingIntent)
                .extend(new CarExtender()
                        .setUnreadConversation(unreadConvBuilder.build())
                        .setColor(getApplicationContext()
                                .getResources().getColor(R.color.default_color_light)));

        MessageLogger.logMessage(getApplicationContext(), "Sending notification "
                + conversation.getConversationId() + " conversation: " + conversation);

        mNotificationManager.notify(conversation.getConversationId(), builder.build());
    }

    /**
     * Handler for incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<MessagingService> mReference;

        IncomingHandler(MessagingService service) {
            mReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MessagingService service = mReference.get();
            switch (msg.what) {
                case MSG_SEND_NOTIFICATION:
                    int howManyConversations = msg.arg1 <= 0 ? 1 : msg.arg1;
                    int messagesPerConversation = msg.arg2 <= 0 ? 1 : msg.arg2;
                    if (service != null) {
                        service.sendNotification(howManyConversations, messagesPerConversation);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
