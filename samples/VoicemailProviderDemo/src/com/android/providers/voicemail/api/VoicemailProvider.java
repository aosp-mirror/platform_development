/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.providers.voicemail.api;

import android.content.Intent;
import android.net.Uri;

/**
 * Defines the constants needed to access and interact with the voicemail content provider.
 */
public class VoicemailProvider {
    /** The authority used by the voicemail provider. */
    public static final String AUTHORITY =
            "com.android.providers.voicemail";

    /** The main URI exposed by the service. */
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/voicemail");
    /** The URI to fetch an individual voicemail. */
    public static final Uri CONTENT_URI_ID_QUERY =
            Uri.parse("content://" + AUTHORITY + "/voicemail/");
    /** The URI to fetch all voicemails from a given provider. */
    public static final Uri CONTENT_URI_PROVIDER_QUERY =
            Uri.parse("content://" + AUTHORITY + "/voicemail/provider/");
    /** The URI to fetch an individual voicemail from a given provider. */
    public static final Uri CONTENT_URI_PROVIDER_ID_QUERY =
            Uri.parse("content://" + AUTHORITY + "/voicemail/provider/");

    /** Broadcast intent when a new voicemail record is inserted. */
    public static final String ACTION_NEW_VOICEMAIL = "android.intent.action.NEW_VOICEMAIL";
    /**
     * Extra included in {@value Intent#ACTION_PROVIDER_CHANGED} and {@value #ACTION_NEW_VOICEMAIL}
     * broadcast intents to indicate the package that caused the change in content provider.
     * <p>
     * Receivers of the broadcast can use this field to determine if this is a self change.
     */
    public static final String EXTRA_CHANGED_BY =
          "com.android.providers.voicemail.changed_by";

    /** The different tables defined by the content provider. */
    public static final class Tables {
        /** The table containing voicemail information. */
        public static final class Voicemails {
            public static final String NAME = "voicemails";

            /** The mime type for a collection of voicemails. */
            public static final String DIR_TYPE =
                    "vnd.android.cursor.dir/voicemails";

            /** The different columns contained within the voicemail table. */
            public static final class Columns {
                public static final String _ID = "_id";
                public static final String _DATA = "_data";
                public static final String _DATA_FILE_EXISTS = "_data_file_exists";
                public static final String NUMBER = "number";
                public static final String DATE = "date";
                public static final String DURATION = "duration";
                public static final String PROVIDER = "provider";
                public static final String PROVIDER_DATA = "provider_data";
                public static final String DATA_MIME_TYPE = "data_mime_type";
                public static final String READ_STATUS = "read_status";
                /**
                 * Current mailbox state of the message.
                 * <p>
                 * Legal values: 0(Inbox)/1(Deleted)/2(Undeleted).
                 */
                public static final String STATE = "state";
            }
        }
    }
}
