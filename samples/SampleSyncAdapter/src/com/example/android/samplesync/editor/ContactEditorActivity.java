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
package com.example.android.samplesync.editor;

import com.example.android.samplesync.R;
import com.example.android.samplesync.client.RawContact;
import com.example.android.samplesync.platform.BatchOperation;
import com.example.android.samplesync.platform.ContactManager;
import com.example.android.samplesync.platform.ContactManager.EditorQuery;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Implements a sample editor for a contact that belongs to a remote contact service.
 * The editor can be invoked for an existing SampleSyncAdapter contact, or it can
 * be used to create a brand new SampleSyncAdapter contact. We look at the Intent
 * object to figure out whether this is a "new" or "edit" operation.
 */
public class ContactEditorActivity extends Activity {
    private static final String TAG = "SampleSyncAdapter";

    // Keep track of whether we're inserting a new contact or editing an
    // existing contact.
    private boolean mIsInsert;

    // The name of the external account we're syncing this contact to.
    private String mAccountName;

    // For existing contacts, this is the URI to the contact data.
    private Uri mRawContactUri;

    // The raw clientId for this contact
    private long mRawContactId;

    // Make sure we only attempt to save the contact once if the
    // user presses the "done" button multiple times...
    private boolean mSaveInProgress = false;

    // Keep track of the controls used to edit contact values, so we can get/set
    // those values easily.
    private EditText mNameEditText;
    private EditText mHomePhoneEditText;
    private EditText mMobilePhoneEditText;
    private EditText mWorkPhoneEditText;
    private EditText mEmailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.editor);

        mNameEditText = (EditText)findViewById(R.id.editor_name);
        mHomePhoneEditText = (EditText)findViewById(R.id.editor_phone_home);
        mMobilePhoneEditText = (EditText)findViewById(R.id.editor_phone_mobile);
        mWorkPhoneEditText = (EditText)findViewById(R.id.editor_phone_work);
        mEmailEditText = (EditText)findViewById(R.id.editor_email);

        // Figure out whether we're creating a new contact (ACTION_INSERT) or editing
        // an existing contact.
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_INSERT.equals(action)) {
            // We're inserting a new contact, so save off the external account name
            // which should have been added to the intent we were passed.
            mIsInsert = true;
            String accountName = intent.getStringExtra(RawContacts.ACCOUNT_NAME);
            if (accountName == null) {
                Log.e(TAG, "Account name is required");
                finish();
            }
            setAccountName(accountName);
        } else {
            // We're editing an existing contact. Load in the data from the contact
            // so that the user can edit it.
            mIsInsert = false;
            mRawContactUri = intent.getData();
            if (mRawContactUri == null) {
                Log.e(TAG, "Raw contact URI is required");
                finish();
            }
            startLoadRawContactEntity();
        }
    }

    @Override
    public void onBackPressed() {
        // This method will have been called if the user presses the "Back" button
        // in the ActionBar.  We treat that the same way as the "Done" button in
        // the ActionBar.
        save();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This method gets called so that we can place items in the main Options menu -
        // for example, the ActionBar items.  We add our menus from the res/menu/edit.xml
        // file.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_done:
                // The user pressed the "Home" button or our "Done" button - both
                // in the ActionBar.  In both cases, we want to save the contact
                // and exit.
                save();
                return true;
            case R.id.menu_cancel:
                // The user pressed the Cancel menu item in the ActionBar.
                // Close the editor without saving any changes.
                finish();
                return true;
        }
        return false;
    }

    /**
     * Create an AsyncTask to load the contact from the Contacts data provider
     */
    private void startLoadRawContactEntity() {
        Uri uri = Uri.withAppendedPath(mRawContactUri, RawContacts.Entity.CONTENT_DIRECTORY);
        new LoadRawContactTask().execute(uri);
    }

    /**
     * Called by the LoadRawContactTask when the contact information has been
     * successfully loaded from the Contacts data provider.
     */
    public void onRawContactEntityLoaded(Cursor cursor) {
        while (cursor.moveToNext()) {
            String mimetype = cursor.getString(EditorQuery.COLUMN_MIMETYPE);
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimetype)) {
                setAccountName(cursor.getString(EditorQuery.COLUMN_ACCOUNT_NAME));
                mRawContactId = cursor.getLong(EditorQuery.COLUMN_RAW_CONTACT_ID);
                mNameEditText.setText(cursor.getString(EditorQuery.COLUMN_FULL_NAME));
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mimetype)) {
                final int type = cursor.getInt(EditorQuery.COLUMN_PHONE_TYPE);
                if (type == Phone.TYPE_HOME) {
                    mHomePhoneEditText.setText(cursor.getString(EditorQuery.COLUMN_PHONE_NUMBER));
                } else if (type == Phone.TYPE_MOBILE) {
                    mMobilePhoneEditText.setText(cursor.getString(EditorQuery.COLUMN_PHONE_NUMBER));
                } else if (type == Phone.TYPE_WORK) {
                    mWorkPhoneEditText.setText(cursor.getString(EditorQuery.COLUMN_PHONE_NUMBER));
                }
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimetype)) {
                mEmailEditText.setText(cursor.getString(EditorQuery.COLUMN_DATA1));
            }
        }
    }

    /**
     * Save the updated contact data. We actually take two different actions
     * depending on whether we are creating a new contact or editing an
     * existing contact.
     */
    public void save() {
        // If we're already saving this contact, don't kick-off yet
        // another save - the user probably just pressed the "Done"
        // button multiple times...
        if (mSaveInProgress) {
            return;
        }

        mSaveInProgress = true;
        if (mIsInsert) {
            saveNewContact();
        } else {
            saveChanges();
        }
    }

    /**
     * Save off the external contacts provider account name. We show the account name
     * in the header section of the edit panel, and we also need it later when we
     * save off a brand new contact.
     */
    private void setAccountName(String accountName) {
        mAccountName = accountName;
        if (accountName != null) {
            TextView accountNameLabel = (TextView)findViewById(R.id.header_account_name);
            if (accountNameLabel != null) {
                accountNameLabel.setText(accountName);
            }
        }
    }

    /**
     * Save a new contact using the Contacts content provider. The actual insertion
     * is performed in an AsyncTask.
     */
    @SuppressWarnings("unchecked")
    private void saveNewContact() {
        new InsertContactTask().execute(buildRawContact());
    }

    /**
     * Save changes to an existing contact.  The actual update is performed in
     * an AsyncTask.
     */
    @SuppressWarnings("unchecked")
    private void saveChanges() {
        new UpdateContactTask().execute(buildRawContact());
    }

    /**
     * Build a RawContact object from the data in the user-editable form
     * @return a new RawContact object representing the edited user
     */
    private RawContact buildRawContact() {
        return RawContact.create(mNameEditText.getText().toString(),
                null,
                null,
                mMobilePhoneEditText.getText().toString(),
                mWorkPhoneEditText.getText().toString(),
                mHomePhoneEditText.getText().toString(),
                mEmailEditText.getText().toString(),
                null,
                false,
                mRawContactId,
                -1);
    }

    /**
     * Called after a contact is saved - both for edited contacts and new contacts.
     * We set the final result of the activity to be "ok", and then close the activity
     * by calling finish().
     */
    public void onContactSaved(Uri result) {
        if (result != null) {
            Intent intent = new Intent();
            intent.setData(result);
            setResult(RESULT_OK, intent);
            finish();
        }
        mSaveInProgress = false;
    }

    /**
     * Represents an asynchronous task used to load a contact from
     * the Contacts content provider.
     *
     */
    public class LoadRawContactTask extends AsyncTask<Uri, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Uri... params) {
            // Our background task is to load the contact from the Contacts provider
            return getContentResolver().query(params[0], EditorQuery.PROJECTION, null, null, null);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            // After we've successfully loaded the contact, call back into
            // the ContactEditorActivity so we can update the UI
            try {
                if (cursor != null) {
                    onRawContactEntityLoaded(cursor);
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Represents an asynchronous task used to save a new contact
     * into the contacts database.
     */
    public class InsertContactTask extends AsyncTask<RawContact, Void, Uri> {

        @Override
        protected Uri doInBackground(RawContact... params) {
            try {
                final RawContact rawContact = params[0];
                final Context context = getApplicationContext();
                final ContentResolver resolver = getContentResolver();
                final BatchOperation batchOperation = new BatchOperation(context, resolver);
                ContactManager.addContact(context, mAccountName, rawContact, false, batchOperation);
                Uri rawContactUri = batchOperation.execute();

                // Convert the raw contact URI to a contact URI
                if (rawContactUri != null) {
                    return RawContacts.getContactLookupUri(resolver, rawContactUri);
                } else {
                    Log.e(TAG, "Could not save new contact");
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "An error occurred while saving new contact", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri result) {
            // Tell the UI that the contact has been successfully saved
            onContactSaved(result);
        }
    }


    /**
     * Represents an asynchronous task used to save an updated contact
     * into the contacts database.
     */
    public class UpdateContactTask extends AsyncTask<RawContact, Void, Uri> {

        @Override
        protected Uri doInBackground(RawContact... params) {
            try {
                final RawContact rawContact = params[0];
                final Context context = getApplicationContext();
                final ContentResolver resolver = getContentResolver();
                final BatchOperation batchOperation = new BatchOperation(context, resolver);
                ContactManager.updateContact(context, resolver, rawContact, false, false, false,
                        false, rawContact.getRawContactId(), batchOperation);
                batchOperation.execute();

                // Convert the raw contact URI to a contact URI
                return RawContacts.getContactLookupUri(resolver, mRawContactUri);
            } catch (Exception e) {
                Log.e(TAG, "Could not save changes", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri result) {
            // Tell the UI that the contact has been successfully saved
            onContactSaved(result);
        }
    }
}
