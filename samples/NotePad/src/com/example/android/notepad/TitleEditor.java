/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.notepad;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This Activity allows the user to edit a note's title. It displays a floating window
 * containing an EditText.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler}
 * or {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class TitleEditor extends Activity {

    /**
     * This is a special intent action that means "edit the title of a note".
     */
    public static final String EDIT_TITLE_ACTION = "com.android.notepad.action.EDIT_TITLE";

    // Creates a projection that returns the note ID and the note contents.
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
    };

    // The position of the title column in a Cursor returned by the provider.
    private static final int COLUMN_INDEX_TITLE = 1;

    // An EditText object for preserving the edited title.
    private EditText mText;

    // A URI object for the note whose title is being edited.
    private Uri mUri;

    // The title that was last saved.
    private String mSavedTitle;

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the View for this Activity object's UI.
        setContentView(R.layout.title_editor);

        // Gets the View ID for the EditText box
        mText = (EditText) this.findViewById(R.id.title);

        // Get the Intent that activated this Activity, and from it get the URI of the note whose
        // title we need to edit.
        mUri = getIntent().getData();

        /*
         * Using the URI passed in with the triggering Intent, gets the note.
         *
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         */

        Cursor cursor = getContentResolver().query(
            mUri,        // The URI for the note that is to be retrieved.
            PROJECTION,  // The columns to retrieve
            null,        // No selection criteria are used, so no where columns are needed.
            null,        // No where columns are used, so no where values are needed.
            null         // No sort order is needed.
        );

        if (cursor != null) {

            // The Cursor was just retrieved, so its index is set to one record *before* the first
            // record retrieved. This moves it to the first record.
            cursor.moveToFirst();

            // Displays the current title text in the EditText object.
            mText.setText(cursor.getString(COLUMN_INDEX_TITLE));
        }
    }

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     *
     * Displays the current title for the selected note.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * This method is called when the Activity loses focus.
     *
     * While there is no need to override this method in this app, it is shown here to highlight
     * that we are not saving any state in onPause, but have moved app state saving to onStop
     * callback.
     * In earlier versions of this app and popular literature it had been shown that onPause is good
     * place to persist any unsaved work, however, this is not really a good practice because of how
     * application and process lifecycle behave.
     * As a general guideline apps should have a way of saving their business logic that does not
     * solely rely on Activity (or other component) lifecyle state transitions.
     * As a backstop you should save any app state, not saved during lifetime of the Activity, in
     * onStop().
     * For a more detailed explanation of this recommendation please read
     * <a href = "https://developer.android.com/guide/topics/processes/process-lifecycle.html">
     * Processes and Application Life Cycle </a>.
     * <a href="https://developer.android.com/training/basics/activity-lifecycle/pausing.html">
     * Pausing and Resuming an Activity </a>.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * This method is called when the Activity becomes invisible.
     *
     * For Activity objects that edit information, onStop() may be the one place where changes are
     * saved.
     * Updates the note with the text currently in the text box.
     */
    @Override
    protected void onStop() {
        super.onStop();
        saveTitle();
    }

    public void onClickOk(View v) {
        saveTitle();
        finish();
    }

    // Saves the title if required
    private void saveTitle() {

        if (!TextUtils.isEmpty(mText.getText())) {

            String newTitle = mText.getText().toString();

            if (!newTitle.equals(mSavedTitle)) {
                // Creates a values map for updating the provider.
                ContentValues values = new ContentValues();

                // In the values map, sets the title to the current contents of the edit box.
                values.put(NotePad.Notes.COLUMN_NAME_TITLE, newTitle);

                /*
                 * Updates the provider with the note's new title.
                 *
                 * Note: This is being done on the UI thread. It will block the thread until the
                 * update completes. In a sample app, going against a simple provider based on a
                 * local database, the block will be momentary, but in a real app you should use
                 * android.content.AsyncQueryHandler or android.os.AsyncTask.
                 */
                getContentResolver().update(
                    mUri,    // The URI for the note to update.
                    values,
                    // The values map containing the columns to update and the values to use.
                    null,    // No selection criteria is used, so no "where" columns are needed.
                    null     // No "where" columns are used, so no "where" values are needed.
                );
                mSavedTitle = newTitle;
            }
        } else {
            Toast.makeText(this, R.string.title_blank, Toast.LENGTH_SHORT).show();
        }
    }
}
