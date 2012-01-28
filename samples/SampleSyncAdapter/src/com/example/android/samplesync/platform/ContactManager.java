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
package com.example.android.samplesync.platform;

import com.example.android.samplesync.Constants;
import com.example.android.samplesync.R;
import com.example.android.samplesync.client.RawContact;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for managing contacts sync related mOperations
 */
public class ContactManager {

    /**
     * Custom IM protocol used when storing status messages.
     */
    public static final String CUSTOM_IM_PROTOCOL = "SampleSyncAdapter";

    private static final String TAG = "ContactManager";

    public static final String SAMPLE_GROUP_NAME = "Sample Group";

    public static long ensureSampleGroupExists(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();

        // Lookup the sample group
        long groupId = 0;
        final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] { Groups._ID },
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " +
                Groups.TITLE + "=?",
                new String[] { account.name, account.type, SAMPLE_GROUP_NAME }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    groupId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        if (groupId == 0) {
            // Sample group doesn't exist yet, so create it
            final ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, account.name);
            contentValues.put(Groups.ACCOUNT_TYPE, account.type);
            contentValues.put(Groups.TITLE, SAMPLE_GROUP_NAME);
            contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

            final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
            groupId = ContentUris.parseId(newGroupUri);
        }
        return groupId;
    }

    /**
     * Take a list of updated contacts and apply those changes to the
     * contacts database. Typically this list of contacts would have been
     * returned from the server, and we want to apply those changes locally.
     *
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param rawContacts The list of contacts to update
     * @param lastSyncMarker The previous server sync-state
     * @return the server syncState that should be used in our next
     * sync request.
     */
    public static synchronized long updateContacts(Context context, String account,
            List<RawContact> rawContacts, long groupId, long lastSyncMarker) {

        long currentSyncMarker = lastSyncMarker;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final List<RawContact> newUsers = new ArrayList<RawContact>();

        Log.d(TAG, "In SyncContacts");
        for (final RawContact rawContact : rawContacts) {
            // The server returns a syncState (x) value with each contact record.
            // The syncState is sequential, so higher values represent more recent
            // changes than lower values. We keep track of the highest value we
            // see, and consider that a "high water mark" for the changes we've
            // received from the server.  That way, on our next sync, we can just
            // ask for changes that have occurred since that most-recent change.
            if (rawContact.getSyncState() > currentSyncMarker) {
                currentSyncMarker = rawContact.getSyncState();
            }

            // If the server returned a clientId for this user, then it's likely
            // that the user was added here, and was just pushed to the server
            // for the first time. In that case, we need to update the main
            // row for this contact so that the RawContacts.SOURCE_ID value
            // contains the correct serverId.
            final long rawContactId;
            final boolean updateServerId;
            if (rawContact.getRawContactId() > 0) {
                rawContactId = rawContact.getRawContactId();
                updateServerId = true;
            } else {
                long serverContactId = rawContact.getServerContactId();
                rawContactId = lookupRawContact(resolver, serverContactId);
                updateServerId = false;
            }
            if (rawContactId != 0) {
                if (!rawContact.isDeleted()) {
                    updateContact(context, resolver, rawContact, updateServerId,
                            true, true, true, rawContactId, batchOperation);
                } else {
                    deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                Log.d(TAG, "In addContact");
                if (!rawContact.isDeleted()) {
                    newUsers.add(rawContact);
                    addContact(context, account, rawContact, groupId, true, batchOperation);
                }
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            // (UI updates, etc)
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();

        return currentSyncMarker;
    }

    /**
     * Return a list of the local contacts that have been marked as
     * "dirty", and need syncing to the SampleSync server.
     *
     * @param context The context of Authenticator Activity
     * @param account The account that we're interested in syncing
     * @return a list of Users that are considered "dirty"
     */
    public static List<RawContact> getDirtyContacts(Context context, Account account) {
        Log.i(TAG, "*** Looking for local dirty contacts");
        List<RawContact> dirtyContacts = new ArrayList<RawContact>();

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c = resolver.query(DirtyQuery.CONTENT_URI,
                DirtyQuery.PROJECTION,
                DirtyQuery.SELECTION,
                new String[] {account.name},
                null);
        try {
            while (c.moveToNext()) {
                final long rawContactId = c.getLong(DirtyQuery.COLUMN_RAW_CONTACT_ID);
                final long serverContactId = c.getLong(DirtyQuery.COLUMN_SERVER_ID);
                final boolean isDirty = "1".equals(c.getString(DirtyQuery.COLUMN_DIRTY));
                final boolean isDeleted = "1".equals(c.getString(DirtyQuery.COLUMN_DELETED));

                // The system actually keeps track of a change version number for
                // each contact. It may be something you're interested in for your
                // client-server sync protocol. We're not using it in this example,
                // other than to log it.
                final long version = c.getLong(DirtyQuery.COLUMN_VERSION);

                Log.i(TAG, "Dirty Contact: " + Long.toString(rawContactId));
                Log.i(TAG, "Contact Version: " + Long.toString(version));

                if (isDeleted) {
                    Log.i(TAG, "Contact is marked for deletion");
                    RawContact rawContact = RawContact.createDeletedContact(rawContactId,
                            serverContactId);
                    dirtyContacts.add(rawContact);
                } else if (isDirty) {
                    RawContact rawContact = getRawContact(context, rawContactId);
                    Log.i(TAG, "Contact Name: " + rawContact.getBestName());
                    dirtyContacts.add(rawContact);
                }
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }
        return dirtyContacts;
    }

    /**
     * Update the status messages for a list of users.  This is typically called
     * for contacts we've just added to the system, since we can't monkey with
     * the contact's status until they have a profileId.
     *
     * @param context The context of Authenticator Activity
     * @param rawContacts The list of users we want to update
     */
    public static void updateStatusMessages(Context context, List<RawContact> rawContacts) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        for (RawContact rawContact : rawContacts) {
            updateContactStatus(context, rawContact, batchOperation);
        }
        batchOperation.execute();
    }

    /**
     * Demonstrate how to add stream items and stream item photos to a raw
     * contact. This just adds items for all of the contacts for this sync
     * adapter with some locally created text and an image. You should check
     * for stream items on the server that you are syncing with and use the
     * text and photo data from there instead.
     *
     * @param context The context of Authenticator Activity
     * @param rawContacts The list of users we want to update
     */
    public static void addStreamItems(Context context, List<RawContact> rawContacts,
             String accountName, String accountType) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        String text = "This is a test stream item!";
        String message = "via SampleSyncAdapter";
        for (RawContact rawContact : rawContacts) {
           addContactStreamItem(context, lookupRawContact(resolver,
                   rawContact.getServerContactId()), accountName, accountType,
                   text, message, batchOperation );
        }
        List<Uri> streamItemUris = batchOperation.execute();

        // Stream item photos are added after the stream items that they are
        // associated with, using the stream item's ID as a reference.

        for (Uri uri : streamItemUris){
          // All you need is the ID of the stream item, which is the last index
          // path segment returned by getPathSegments().
          long streamItemId = Long.parseLong(uri.getPathSegments().get(
                  uri.getPathSegments().size()-1));
          addStreamItemPhoto(context, resolver, streamItemId, accountName,
                  accountType, batchOperation);
        }
        batchOperation.execute();
    }

    /**
     * After we've finished up a sync operation, we want to clean up the sync-state
     * so that we're ready for the next time.  This involves clearing out the 'dirty'
     * flag on the synced contacts - but we also have to finish the DELETE operation
     * on deleted contacts.  When the user initially deletes them on the client, they're
     * marked for deletion - but they're not actually deleted until we delete them
     * again, and include the ContactsContract.CALLER_IS_SYNCADAPTER parameter to
     * tell the contacts provider that we're really ready to let go of this contact.
     *
     * @param context The context of Authenticator Activity
     * @param dirtyContacts The list of contacts that we're cleaning up
     */
    public static void clearSyncFlags(Context context, List<RawContact> dirtyContacts) {
        Log.i(TAG, "*** Clearing Sync-related Flags");
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        for (RawContact rawContact : dirtyContacts) {
            if (rawContact.isDeleted()) {
                Log.i(TAG, "Deleting contact: " + Long.toString(rawContact.getRawContactId()));
                deleteContact(context, rawContact.getRawContactId(), batchOperation);
            } else if (rawContact.isDirty()) {
                Log.i(TAG, "Clearing dirty flag for: " + rawContact.getBestName());
                clearDirtyFlag(context, rawContact.getRawContactId(), batchOperation);
            }
        }
        batchOperation.execute();
    }

    /**
     * Adds a single contact to the platform contacts provider.
     * This can be used to respond to a new contact found as part
     * of sync information returned from the server, or because a
     * user added a new contact.
     *
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param rawContact the sample SyncAdapter User object
     * @param groupId the id of the sample group
     * @param inSync is the add part of a client-server sync?
     * @param batchOperation allow us to batch together multiple operations
     *        into a single provider call
     */
    public static void addContact(Context context, String accountName, RawContact rawContact,
            long groupId, boolean inSync, BatchOperation batchOperation) {

        // Put the data in the contacts provider
        final ContactOperations contactOp = ContactOperations.createNewContact(
                context, rawContact.getServerContactId(), accountName, inSync, batchOperation);

        contactOp.addName(rawContact.getFullName(), rawContact.getFirstName(),
                rawContact.getLastName())
                .addEmail(rawContact.getEmail())
                .addPhone(rawContact.getCellPhone(), Phone.TYPE_MOBILE)
                .addPhone(rawContact.getHomePhone(), Phone.TYPE_HOME)
                .addPhone(rawContact.getOfficePhone(), Phone.TYPE_WORK)
                .addGroupMembership(groupId)
                .addAvatar(rawContact.getAvatarUrl());

        // If we have a serverId, then go ahead and create our status profile.
        // Otherwise skip it - and we'll create it after we sync-up to the
        // server later on.
        if (rawContact.getServerContactId() > 0) {
            contactOp.addProfileAction(rawContact.getServerContactId());
        }
    }

    /**
     * Updates a single contact to the platform contacts provider.
     * This method can be used to update a contact from a sync
     * operation or as a result of a user editing a contact
     * record.
     *
     * This operation is actually relatively complex.  We query
     * the database to find all the rows of info that already
     * exist for this Contact. For rows that exist (and thus we're
     * modifying existing fields), we create an update operation
     * to change that field.  But for fields we're adding, we create
     * "add" operations to create new rows for those fields.
     *
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param rawContact the sample SyncAdapter contact object
     * @param updateStatus should we update this user's status
     * @param updateAvatar should we update this user's avatar image
     * @param inSync is the update part of a client-server sync?
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     * @param batchOperation allow us to batch together multiple operations
     *        into a single provider call
     */
    public static void updateContact(Context context, ContentResolver resolver,
        RawContact rawContact, boolean updateServerId, boolean updateStatus, boolean updateAvatar,
        boolean inSync, long rawContactId, BatchOperation batchOperation) {

        boolean existingCellPhone = false;
        boolean existingHomePhone = false;
        boolean existingWorkPhone = false;
        boolean existingEmail = false;
        boolean existingAvatar = false;

        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                new String[] {String.valueOf(rawContactId)}, null);
        final ContactOperations contactOp =
                ContactOperations.updateExistingContact(context, rawContactId,
                inSync, batchOperation);
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    contactOp.updateName(uri,
                            c.getString(DataQuery.COLUMN_GIVEN_NAME),
                            c.getString(DataQuery.COLUMN_FAMILY_NAME),
                            c.getString(DataQuery.COLUMN_FULL_NAME),
                            rawContact.getFirstName(),
                            rawContact.getLastName(),
                            rawContact.getFullName());
                } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);
                    if (type == Phone.TYPE_MOBILE) {
                        existingCellPhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getCellPhone(), uri);
                    } else if (type == Phone.TYPE_HOME) {
                        existingHomePhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getHomePhone(), uri);
                    } else if (type == Phone.TYPE_WORK) {
                        existingWorkPhone = true;
                        contactOp.updatePhone(c.getString(DataQuery.COLUMN_PHONE_NUMBER),
                                rawContact.getOfficePhone(), uri);
                    }
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    existingEmail = true;
                    contactOp.updateEmail(rawContact.getEmail(),
                            c.getString(DataQuery.COLUMN_EMAIL_ADDRESS), uri);
                } else if (mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                    existingAvatar = true;
                    contactOp.updateAvatar(rawContact.getAvatarUrl(), uri);
                }
            } // while
        } finally {
            c.close();
        }

        // Add the cell phone, if present and not updated above
        if (!existingCellPhone) {
            contactOp.addPhone(rawContact.getCellPhone(), Phone.TYPE_MOBILE);
        }
        // Add the home phone, if present and not updated above
        if (!existingHomePhone) {
            contactOp.addPhone(rawContact.getHomePhone(), Phone.TYPE_HOME);
        }

        // Add the work phone, if present and not updated above
        if (!existingWorkPhone) {
            contactOp.addPhone(rawContact.getOfficePhone(), Phone.TYPE_WORK);
        }
        // Add the email address, if present and not updated above
        if (!existingEmail) {
            contactOp.addEmail(rawContact.getEmail());
        }
        // Add the avatar if we didn't update the existing avatar
        if (!existingAvatar) {
            contactOp.addAvatar(rawContact.getAvatarUrl());
        }

        // If we need to update the serverId of the contact record, take
        // care of that.  This will happen if the contact is created on the
        // client, and then synced to the server. When we get the updated
        // record back from the server, we can set the SOURCE_ID property
        // on the contact, so we can (in the future) lookup contacts by
        // the serverId.
        if (updateServerId) {
            Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
            contactOp.updateServerId(rawContact.getServerContactId(), uri);
        }

        // If we don't have a status profile, then create one.  This could
        // happen for contacts that were created on the client - we don't
        // create the status profile until after the first sync...
        final long serverId = rawContact.getServerContactId();
        final long profileId = lookupProfile(resolver, serverId);
        if (profileId <= 0) {
            contactOp.addProfileAction(serverId);
        }
    }

    /**
     * When we first add a sync adapter to the system, the contacts from that
     * sync adapter will be hidden unless they're merged/grouped with an existing
     * contact.  But typically we want to actually show those contacts, so we
     * need to mess with the Settings table to get them to show up.
     *
     * @param context the Authenticator Activity context
     * @param account the Account who's visibility we're changing
     * @param visible true if we want the contacts visible, false for hidden
     */
    public static void setAccountContactsVisibility(Context context, Account account,
            boolean visible) {
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, account.name);
        values.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

        context.getContentResolver().insert(Settings.CONTENT_URI, values);
    }

    /**
     * Return a User object with data extracted from a contact stored
     * in the local contacts database.
     *
     * Because a contact is actually stored over several rows in the
     * database, our query will return those multiple rows of information.
     * We then iterate over the rows and build the User structure from
     * what we find.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the unique ID for the local contact
     * @return a User object containing info on that contact
     */
    private static RawContact getRawContact(Context context, long rawContactId) {
        String firstName = null;
        String lastName = null;
        String fullName = null;
        String cellPhone = null;
        String homePhone = null;
        String workPhone = null;
        String email = null;
        long serverId = -1;

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c =
            resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                new String[] {String.valueOf(rawContactId)}, null);
        try {
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final long tempServerId = c.getLong(DataQuery.COLUMN_SERVER_ID);
                if (tempServerId > 0) {
                    serverId = tempServerId;
                }
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    lastName = c.getString(DataQuery.COLUMN_FAMILY_NAME);
                    firstName = c.getString(DataQuery.COLUMN_GIVEN_NAME);
                    fullName = c.getString(DataQuery.COLUMN_FULL_NAME);
                } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);
                    if (type == Phone.TYPE_MOBILE) {
                        cellPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    } else if (type == Phone.TYPE_HOME) {
                        homePhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    } else if (type == Phone.TYPE_WORK) {
                        workPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                    }
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                }
            } // while
        } finally {
            c.close();
        }

        // Now that we've extracted all the information we care about,
        // create the actual User object.
        RawContact rawContact = RawContact.create(fullName, firstName, lastName, cellPhone,
                workPhone, homePhone, email, null, false, rawContactId, serverId);

        return rawContact;
    }

    /**
     * Update the status message associated with the specified user.  The status
     * message would be something that is likely to be used by IM or social
     * networking sync providers, and less by a straightforward contact provider.
     * But it's a useful demo to see how it's done.
     *
     * @param context the Authenticator Activity context
     * @param rawContact the contact whose status we should update
     * @param batchOperation allow us to batch together multiple operations
     */
    private static void updateContactStatus(Context context, RawContact rawContact,
            BatchOperation batchOperation) {
        final ContentValues values = new ContentValues();
        final ContentResolver resolver = context.getContentResolver();

        final long userId = rawContact.getServerContactId();
        final String username = rawContact.getUserName();
        final String status = rawContact.getStatus();

        // Look up the user's sample SyncAdapter data row
        final long profileId = lookupProfile(resolver, userId);

        // Insert the activity into the stream
        if (profileId > 0) {
            values.put(StatusUpdates.DATA_ID, profileId);
            values.put(StatusUpdates.STATUS, status);
            values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM);
            values.put(StatusUpdates.CUSTOM_PROTOCOL, CUSTOM_IM_PROTOCOL);
            values.put(StatusUpdates.IM_ACCOUNT, username);
            values.put(StatusUpdates.IM_HANDLE, userId);
            values.put(StatusUpdates.STATUS_RES_PACKAGE, context.getPackageName());
            values.put(StatusUpdates.STATUS_ICON, R.drawable.icon);
            values.put(StatusUpdates.STATUS_LABEL, R.string.label);
            batchOperation.add(ContactOperations.newInsertCpo(StatusUpdates.CONTENT_URI,
                    false, true).withValues(values).build());
        }
    }

    /**
     * Adds a stream item to a raw contact. The stream item is usually obtained
     * from the server you are syncing with, but we create it here locally as an
     * example.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the raw contact ID that the stream item is associated with
     * @param accountName the account name of the sync adapter
     * @param accountType the account type of the sync adapter
     * @param text the text of the stream item
     * @param comments the comments for the stream item, such as where the stream item came from
     * @param batchOperation allow us to batch together multiple operations
     */
    private static void addContactStreamItem(Context context, long rawContactId,
        String accountName, String accountType, String text, String comments,
        BatchOperation batchOperation) {

        final ContentValues values = new ContentValues();
        final ContentResolver resolver = context.getContentResolver();
        if (rawContactId > 0){
            values.put(StreamItems.RAW_CONTACT_ID, rawContactId);
            values.put(StreamItems.TEXT, text);
            values.put(StreamItems.TIMESTAMP, System.currentTimeMillis());
            values.put(StreamItems.COMMENTS, comments);
            values.put(StreamItems.ACCOUNT_NAME, accountName);
            values.put(StreamItems.ACCOUNT_TYPE, accountType);

            batchOperation.add(ContactOperations.newInsertCpo(
                    StreamItems.CONTENT_URI, false, true).withValues(values).build());
        }
    }

    private static void addStreamItemPhoto(Context context, ContentResolver
        resolver, long streamItemId, String accountName, String accountType,
        BatchOperation batchOperation){

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                R.raw.img1);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        byte[] photoData = stream.toByteArray();

        final ContentValues values = new ContentValues();
        values.put(StreamItemPhotos.STREAM_ITEM_ID, streamItemId);
        values.put(StreamItemPhotos.SORT_INDEX, 1);
        values.put(StreamItemPhotos.PHOTO, photoData);
        values.put(StreamItems.ACCOUNT_NAME, accountName);
        values.put(StreamItems.ACCOUNT_TYPE, accountType);

        batchOperation.add(ContactOperations.newInsertCpo(
                StreamItems.CONTENT_PHOTO_URI, false, true).withValues(values).build());
    }

    /**
     * Clear the local system 'dirty' flag for a contact.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the id of the contact update
     * @param batchOperation allow us to batch together multiple operations
     */
    private static void clearDirtyFlag(Context context, long rawContactId,
        BatchOperation batchOperation) {
        final ContactOperations contactOp =
                ContactOperations.updateExistingContact(context, rawContactId, true,
                batchOperation);

        final Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        contactOp.updateDirtyFlag(false, uri);
    }

     /**
     * Deletes a contact from the platform contacts provider. This method is used
     * both for contacts that were deleted locally and then that deletion was synced
     * to the server, and for contacts that were deleted on the server and the
     * deletion was synced to the client.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void deleteContact(Context context, long rawContactId,
        BatchOperation batchOperation) {

        batchOperation.add(ContactOperations.newDeleteCpo(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                true, true).build());
    }

    /**
     * Returns the RawContact id for a sample SyncAdapter contact, or 0 if the
     * sample SyncAdapter user isn't found.
     *
     * @param resolver the content resolver to use
     * @param serverContactId the sample SyncAdapter user ID to lookup
     * @return the RawContact id, or 0 if not found
     */
    private static long lookupRawContact(ContentResolver resolver, long serverContactId) {

        long rawContactId = 0;
        final Cursor c = resolver.query(
                UserIdQuery.CONTENT_URI,
                UserIdQuery.PROJECTION,
                UserIdQuery.SELECTION,
                new String[] {String.valueOf(serverContactId)},
                null);
        try {
            if ((c != null) && c.moveToFirst()) {
                rawContactId = c.getLong(UserIdQuery.COLUMN_RAW_CONTACT_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rawContactId;
    }

    /**
     * Returns the Data id for a sample SyncAdapter contact's profile row, or 0
     * if the sample SyncAdapter user isn't found.
     *
     * @param resolver a content resolver
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the profile Data row id, or 0 if not found
     */
    private static long lookupProfile(ContentResolver resolver, long userId) {

        long profileId = 0;
        final Cursor c =
            resolver.query(Data.CONTENT_URI, ProfileQuery.PROJECTION, ProfileQuery.SELECTION,
                new String[] {String.valueOf(userId)}, null);
        try {
            if ((c != null) && c.moveToFirst()) {
                profileId = c.getLong(ProfileQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return profileId;
    }

    final public static class EditorQuery {

        private EditorQuery() {
        }

        public static final String[] PROJECTION = new String[] {
            RawContacts.ACCOUNT_NAME,
            Data._ID,
            RawContacts.Entity.DATA_ID,
            Data.MIMETYPE,
            Data.DATA1,
            Data.DATA2,
            Data.DATA3,
            Data.DATA15,
            Data.SYNC1
            };

        public static final int COLUMN_ACCOUNT_NAME = 0;
        public static final int COLUMN_RAW_CONTACT_ID = 1;
        public static final int COLUMN_DATA_ID = 2;
        public static final int COLUMN_MIMETYPE = 3;
        public static final int COLUMN_DATA1 = 4;
        public static final int COLUMN_DATA2 = 5;
        public static final int COLUMN_DATA3 = 6;
        public static final int COLUMN_DATA15 = 7;
        public static final int COLUMN_SYNC1 = 8;

        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
        public static final int COLUMN_AVATAR_IMAGE = COLUMN_DATA15;
        public static final int COLUMN_SYNC_DIRTY = COLUMN_SYNC1;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    final private static class ProfileQuery {

        private ProfileQuery() {
        }

        public final static String[] PROJECTION = new String[] {Data._ID};

        public final static int COLUMN_ID = 0;

        public static final String SELECTION =
            Data.MIMETYPE + "='" + SampleSyncAdapterColumns.MIME_PROFILE + "' AND "
                + SampleSyncAdapterColumns.DATA_PID + "=?";
    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    final private static class UserIdQuery {

        private UserIdQuery() {
        }

        public final static String[] PROJECTION = new String[] {
            RawContacts._ID,
            RawContacts.CONTACT_ID
            };

        public final static int COLUMN_RAW_CONTACT_ID = 0;
        public final static int COLUMN_LINKED_CONTACT_ID = 1;

        public final static Uri CONTENT_URI = RawContacts.CONTENT_URI;

        public static final String SELECTION =
            RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND "
                + RawContacts.SOURCE_ID + "=?";
    }

    /**
     * Constants for a query to find SampleSyncAdapter contacts that are
     * in need of syncing to the server. This should cover new, edited,
     * and deleted contacts.
     */
    final private static class DirtyQuery {

        private DirtyQuery() {
        }

        public final static String[] PROJECTION = new String[] {
            RawContacts._ID,
            RawContacts.SOURCE_ID,
            RawContacts.DIRTY,
            RawContacts.DELETED,
            RawContacts.VERSION
            };

        public final static int COLUMN_RAW_CONTACT_ID = 0;
        public final static int COLUMN_SERVER_ID = 1;
        public final static int COLUMN_DIRTY = 2;
        public final static int COLUMN_DELETED = 3;
        public final static int COLUMN_VERSION = 4;

        public static final Uri CONTENT_URI = RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build();

        public static final String SELECTION =
            RawContacts.DIRTY + "=1 AND "
                + RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND "
                + RawContacts.ACCOUNT_NAME + "=?";
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
            new String[] {Data._ID, RawContacts.SOURCE_ID, Data.MIMETYPE, Data.DATA1,
            Data.DATA2, Data.DATA3, Data.DATA15, Data.SYNC1};

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_SERVER_ID = 1;
        public static final int COLUMN_MIMETYPE = 2;
        public static final int COLUMN_DATA1 = 3;
        public static final int COLUMN_DATA2 = 4;
        public static final int COLUMN_DATA3 = 5;
        public static final int COLUMN_DATA15 = 6;
        public static final int COLUMN_SYNC1 = 7;

        public static final Uri CONTENT_URI = Data.CONTENT_URI;

        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
        public static final int COLUMN_AVATAR_IMAGE = COLUMN_DATA15;
        public static final int COLUMN_SYNC_DIRTY = COLUMN_SYNC1;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }

    /**
     * Constants for a query to read basic contact columns
     */
    final public static class ContactQuery {
        private ContactQuery() {
        }

        public static final String[] PROJECTION =
            new String[] {Contacts._ID, Contacts.DISPLAY_NAME};

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_DISPLAY_NAME = 1;
    }
}
