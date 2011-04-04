/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.samplesync.client;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import java.lang.StringBuilder;

/**
 * Represents a low-level contacts RawContact - or at least
 * the fields of the RawContact that we care about.
 */
final public class RawContact {

    /** The tag used to log to adb console. **/
    private static final String TAG = "RawContact";

    private final String mUserName;

    private final String mFullName;

    private final String mFirstName;

    private final String mLastName;

    private final String mCellPhone;

    private final String mOfficePhone;

    private final String mHomePhone;

    private final String mEmail;

    private final String mStatus;

    private final String mAvatarUrl;

    private final boolean mDeleted;

    private final boolean mDirty;

    private final long mServerContactId;

    private final long mRawContactId;

    private final long mSyncState;

    public long getServerContactId() {
        return mServerContactId;
    }

    public long getRawContactId() {
        return mRawContactId;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getCellPhone() {
        return mCellPhone;
    }

    public String getOfficePhone() {
        return mOfficePhone;
    }

    public String getHomePhone() {
        return mHomePhone;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public boolean isDirty() {
        return mDirty;
    }

    public long getSyncState() {
        return mSyncState;
    }

    public String getBestName() {
        if (!TextUtils.isEmpty(mFullName)) {
            return mFullName;
        } else if (TextUtils.isEmpty(mFirstName)) {
            return mLastName;
        } else {
            return mFirstName;
        }
    }

    /**
     * Convert the RawContact object into a JSON string.  From the
     * JSONString interface.
     * @return a JSON string representation of the object
     */
    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            if (!TextUtils.isEmpty(mFirstName)) {
                json.put("f", mFirstName);
            }
            if (!TextUtils.isEmpty(mLastName)) {
                json.put("l", mLastName);
            }
            if (!TextUtils.isEmpty(mCellPhone)) {
                json.put("m", mCellPhone);
            }
            if (!TextUtils.isEmpty(mOfficePhone)) {
                json.put("o", mOfficePhone);
            }
            if (!TextUtils.isEmpty(mHomePhone)) {
                json.put("h", mHomePhone);
            }
            if (!TextUtils.isEmpty(mEmail)) {
                json.put("e", mEmail);
            }
            if (mServerContactId > 0) {
                json.put("i", mServerContactId);
            }
            if (mRawContactId > 0) {
                json.put("c", mRawContactId);
            }
            if (mDeleted) {
                json.put("d", mDeleted);
            }
        } catch (final Exception ex) {
            Log.i(TAG, "Error converting RawContact to JSONObject" + ex.toString());
        }

        return json;
    }

    public RawContact(String name, String fullName, String firstName, String lastName,
            String cellPhone, String officePhone, String homePhone, String email,
            String status, String avatarUrl, boolean deleted, long serverContactId,
            long rawContactId, long syncState, boolean dirty) {
        mUserName = name;
        mFullName = fullName;
        mFirstName = firstName;
        mLastName = lastName;
        mCellPhone = cellPhone;
        mOfficePhone = officePhone;
        mHomePhone = homePhone;
        mEmail = email;
        mStatus = status;
        mAvatarUrl = avatarUrl;
        mDeleted = deleted;
        mServerContactId = serverContactId;
        mRawContactId = rawContactId;
        mSyncState = syncState;
        mDirty = dirty;
    }

    /**
     * Creates and returns an instance of the RawContact from the provided JSON data.
     *
     * @param user The JSONObject containing user data
     * @return user The new instance of Sample RawContact created from the JSON data.
     */
    public static RawContact valueOf(JSONObject contact) {

        try {
            final String userName = !contact.isNull("u") ? contact.getString("u") : null;
            final int serverContactId = !contact.isNull("i") ? contact.getInt("i") : -1;
            // If we didn't get either a username or serverId for the contact, then
            // we can't do anything with it locally...
            if ((userName == null) && (serverContactId <= 0)) {
                throw new JSONException("JSON contact missing required 'u' or 'i' fields");
            }

            final int rawContactId = !contact.isNull("c") ? contact.getInt("c") : -1;
            final String firstName = !contact.isNull("f")  ? contact.getString("f") : null;
            final String lastName = !contact.isNull("l") ? contact.getString("l") : null;
            final String cellPhone = !contact.isNull("m") ? contact.getString("m") : null;
            final String officePhone = !contact.isNull("o") ? contact.getString("o") : null;
            final String homePhone = !contact.isNull("h") ? contact.getString("h") : null;
            final String email = !contact.isNull("e") ? contact.getString("e") : null;
            final String status = !contact.isNull("s") ? contact.getString("s") : null;
            final String avatarUrl = !contact.isNull("a") ? contact.getString("a") : null;
            final boolean deleted = !contact.isNull("d") ? contact.getBoolean("d") : false;
            final long syncState = !contact.isNull("x") ? contact.getLong("x") : 0;
            return new RawContact(userName, null, firstName, lastName, cellPhone,
                    officePhone, homePhone, email, status, avatarUrl, deleted,
                    serverContactId, rawContactId, syncState, false);
        } catch (final Exception ex) {
            Log.i(TAG, "Error parsing JSON contact object" + ex.toString());
        }
        return null;
    }

    /**
     * Creates and returns RawContact instance from all the supplied parameters.
     */
    public static RawContact create(String fullName, String firstName, String lastName,
            String cellPhone, String officePhone, String homePhone,
            String email, String status, boolean deleted, long rawContactId,
            long serverContactId) {
        return new RawContact(null, fullName, firstName, lastName, cellPhone, officePhone,
                homePhone, email, status, null, deleted, serverContactId, rawContactId,
                -1, true);
    }

    /**
     * Creates and returns a User instance that represents a deleted user.
     * Since the user is deleted, all we need are the client/server IDs.
     * @param clientUserId The client-side ID for the contact
     * @param serverUserId The server-side ID for the contact
     * @return a minimal User object representing the deleted contact.
     */
    public static RawContact createDeletedContact(long rawContactId, long serverContactId)
    {
        return new RawContact(null, null, null, null, null, null, null,
                null, null, null, true, serverContactId, rawContactId, -1, true);
    }
}
