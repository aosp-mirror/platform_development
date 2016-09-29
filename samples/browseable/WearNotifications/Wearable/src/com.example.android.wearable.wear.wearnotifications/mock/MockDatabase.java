package com.example.android.wearable.wear.wearnotifications.mock;

import android.support.v4.app.NotificationCompat.MessagingStyle;

import com.example.android.wearable.wear.wearnotifications.R;

import java.util.ArrayList;

/**
 * Mock data for each of the Notification Style Demos.
 */
public final class MockDatabase {

    public static BigTextStyleReminderAppData getBigTextStyleData() {
        return BigTextStyleReminderAppData.getInstance();
    }

    public static BigPictureStyleSocialAppData getBigPictureStyleData() {
        return BigPictureStyleSocialAppData.getInstance();
    }

    public static InboxStyleEmailAppData getInboxStyleData() {
        return InboxStyleEmailAppData.getInstance();
    }

    public static MessagingStyleCommsAppData getMessagingStyleData() {
        return MessagingStyleCommsAppData.getInstance();
    }

    /**
     * Represents data needed for BigTextStyle Notification.
     */
    public static class BigTextStyleReminderAppData {

        private static BigTextStyleReminderAppData sInstance = null;

        // Standard notification values
        private String mContentTitle;
        private String mContentText;

        // Style notification values
        private String mBigContentTitle;
        private String mBigText;
        private String mSummaryText;


        public static BigTextStyleReminderAppData getInstance() {
            if (sInstance == null) {
                sInstance = getSync();
            }

            return sInstance;
        }

        private static synchronized BigTextStyleReminderAppData getSync() {
            if (sInstance == null) {
                sInstance = new BigTextStyleReminderAppData();
            }

            return sInstance;
        }

        private BigTextStyleReminderAppData() {
            // Standard Notification values
            // Title for API <16 (4.0 and below) devices
            mContentTitle = "Don't forget to...";
            // Content for API <24 (4.0 and below) devices
            mContentText = "Feed Dogs and check garage!";

            // BigText Style Notification values
            mBigContentTitle = "Don't forget to...";
            mBigText = "... feed the dogs before you leave for work, and check the garage to "
                            + "make sure the door is closed.";
            mSummaryText = "Dogs and Garage";
        }
        public String getContentTitle() {
            return mContentTitle;
        }

        public String getContentText() {
            return mContentText;
        }

        public String getBigContentTitle() {
            return mBigContentTitle;
        }

        public String getBigText() {
            return mBigText;
        }

        public String getSummaryText() {
            return mSummaryText;
        }

        @Override
        public String toString() {
            return getBigContentTitle() + getBigText();
        }
    }

    /**
     * Represents data needed for BigPictureStyle Notification.
     */
    public static class BigPictureStyleSocialAppData {
        private static BigPictureStyleSocialAppData sInstance = null;

        // Standard notification values
        private String mContentTitle;
        private String mContentText;

        // Style notification values
        private int mBigImage;
        private String mBigContentTitle;
        private String mSummaryText;

        private CharSequence[] mPossiblePostResponses;

        private ArrayList<String> mParticipants;


        public static BigPictureStyleSocialAppData getInstance() {
            if (sInstance == null) {
                sInstance = getSync();
            }
            return sInstance;
        }

        private static synchronized BigPictureStyleSocialAppData getSync() {
            if (sInstance == null) {
                sInstance = new BigPictureStyleSocialAppData();
            }

            return sInstance;
        }

        private BigPictureStyleSocialAppData() {
            // Standard Notification values
            // Title/Content for API <16 (4.0 and below) devices
            mContentTitle = "Bob's Post";
            mContentText = "[Picture] Like my shot of Earth?";

            // Style notification values
            mBigImage = R.drawable.earth;
            mBigContentTitle = "Bob's Post";
            mSummaryText = "Like my shot of Earth?";

            // This would be possible responses based on the contents of the post
            mPossiblePostResponses = new CharSequence[]{"Yes", "No", "Maybe?"};

            mParticipants = new ArrayList<>();
            mParticipants.add("Bob Smith");
        }

        public String getContentTitle() {
            return mContentTitle;
        }

        public String getContentText() {
            return mContentText;
        }

        public int getBigImage() {
            return mBigImage;
        }

        public String getBigContentTitle() {
            return mBigContentTitle;
        }

        public String getSummaryText() {
            return mSummaryText;
        }

        public CharSequence[] getPossiblePostResponses() {
            return mPossiblePostResponses;
        }

        public ArrayList<String> getParticipants() {
            return mParticipants;
        }

        @Override
        public String toString() {
            return getContentTitle() + " - " + getContentText();
        }
    }

    /**
     * Represents data needed for InboxStyle Notification.
     */
    public static class InboxStyleEmailAppData {
        private static InboxStyleEmailAppData sInstance = null;

        // Standard notification values
        private String mContentTitle;
        private String mContentText;
        private int mNumberOfNewEmails;

        // Style notification values
        private String mBigContentTitle;
        private String mSummaryText;
        private ArrayList<String> mIndividualEmailSummary;

        private ArrayList<String> mParticipants;

        public static InboxStyleEmailAppData getInstance() {
            if (sInstance == null) {
                sInstance = getSync();
            }
            return sInstance;
        }

        private static synchronized InboxStyleEmailAppData getSync() {
            if (sInstance == null) {
                sInstance = new InboxStyleEmailAppData();
            }

            return sInstance;
        }

        private InboxStyleEmailAppData() {
            // Standard Notification values
            // Title/Content for API <16 (4.0 and below) devices
            mContentTitle = "5 new emails";
            mContentText = "from Jane, Jay, Alex +2 more";
            mNumberOfNewEmails = 5;

            // Style notification values
            mBigContentTitle = "5 new emails from Jane, Jay, Alex +2";
            mSummaryText = "New emails";

            // Add each summary line of the new emails, you can add up to 5
            mIndividualEmailSummary = new ArrayList<>();
            mIndividualEmailSummary.add("Jane Faab  -   Launch Party is here...");
            mIndividualEmailSummary.add("Jay Walker -   There's a turtle on the server!");
            mIndividualEmailSummary.add("Alex Chang -   Check this out...");
            mIndividualEmailSummary.add("Jane Johns -   Check in code?");
            mIndividualEmailSummary.add("John Smith -   Movies later....");

            // If the phone is in "Do not disturb mode, the user will still be notified if
            // the user(s) is starred as a favorite.
            mParticipants = new ArrayList<>();
            mParticipants.add("Jane Faab");
            mParticipants.add("Jay Walker");
            mParticipants.add("Alex Chang");
            mParticipants.add("Jane Johns");
            mParticipants.add("John Smith");
        }

        public String getContentTitle() {
            return mContentTitle;
        }

        public String getContentText() {
            return mContentText;
        }

        public int getNumberOfNewEmails() {
            return mNumberOfNewEmails;
        }

        public String getBigContentTitle() {
            return mBigContentTitle;
        }

        public String getSummaryText() {
            return mSummaryText;
        }

        public ArrayList<String> getIndividualEmailSummary() {
            return mIndividualEmailSummary;
        }

        public ArrayList<String> getParticipants() {
            return mParticipants;
        }

        @Override
        public String toString() {
            return getContentTitle() + " " + getContentText();
        }
    }

    /**
     * Represents data needed for MessagingStyle Notification.
     */
    public static class MessagingStyleCommsAppData {

        private static MessagingStyleCommsAppData sInstance = null;

        // Standard notification values
        private String mContentTitle;
        private String mContentText;

        // Style notification values
        private ArrayList<MessagingStyle.Message> mMessages;
        // Basically, String of all mMessages
        private String mFullConversation;
        // Name preferred when replying to chat
        private String mReplayName;
        private int mNumberOfNewMessages;
        private ArrayList<String> mParticipants;

        public static MessagingStyleCommsAppData getInstance() {
            if (sInstance == null) {
                sInstance = getSync();
            }
            return sInstance;
        }

        private static synchronized MessagingStyleCommsAppData getSync() {
            if (sInstance == null) {
                sInstance = new MessagingStyleCommsAppData();
            }

            return sInstance;
        }

        private MessagingStyleCommsAppData() {
            // Standard notification values
            // Content for API <24 (M and below) devices
            mContentTitle = "2 Messages w/ Famous McFamously";
            mContentText = "Dude! ... You know I am a Pesce-pescetarian. :P";

            // Style notification values

            // For each message, you need the timestamp, in this case, we are using arbitrary ones.
            long currentTime = System.currentTimeMillis();

            mMessages = new ArrayList<>();
            mMessages.add(new MessagingStyle.Message(
                    "What are you doing tonight?", currentTime - 4000, "Famous"));
            mMessages.add(new MessagingStyle.Message(
                    "I don't know, dinner maybe?", currentTime - 3000, null));
            mMessages.add(new MessagingStyle.Message(
                    "Sounds good.", currentTime - 2000, "Famous"));
            mMessages.add(new MessagingStyle.Message(
                    "How about BBQ?", currentTime - 1000, null));
            // Last two are the newest message (2) from friend
            mMessages.add(new MessagingStyle.Message(
                    "Dude!", currentTime, "Famous"));
            mMessages.add(new MessagingStyle.Message(
                    "You know I am a Pesce-pescetarian. :P", currentTime, "Famous"));


            // String version of the mMessages above
            mFullConversation = "Famous: What are you doing tonight?\n\n"
                    + "Me: I don't know, dinner maybe?\n\n"
                    + "Famous: Sounds good.\n\n"
                    + "Me: How about BBQ?\n\n"
                    + "Famous: Dude!\n\n"
                    + "Famous: You know I am a Pesce-pescetarian. :P\n\n";

            mNumberOfNewMessages = 2;

            // Name preferred when replying to chat
            mReplayName = "Me";

            // If the phone is in "Do not disturb mode, the user will still be notified if
            // the user(s) is starred as a favorite.
            mParticipants = new ArrayList<>();
            mParticipants.add("Famous McFamously");

        }

        public String getContentTitle() {
            return mContentTitle;
        }

        public String getContentText() {
            return mContentText;
        }

        public ArrayList<MessagingStyle.Message> getMessages() {
            return mMessages;
        }

        public String getFullConversation() {
            return mFullConversation;
        }

        public String getReplayName() {
            return mReplayName;
        }

        public int getNumberOfNewMessages() {
            return mNumberOfNewMessages;
        }

        public ArrayList<String> getParticipants() {
            return mParticipants;
        }

        @Override
        public String toString() {
            return getFullConversation();
        }
    }
}