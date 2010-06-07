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

import com.example.android.notepad.NotePad;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * This Activity handles "editing" a note, where editing is responding to
 * {@link Intent#ACTION_VIEW} (request to view data), edit a note
 * {@link Intent#ACTION_EDIT}, or create a note {@link Intent#ACTION_INSERT}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler}
 * or {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "Notes";

    /*
     * Creates a projection that returns the note ID and the note contents.
     */
    private static final String[] PROJECTION
        = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_NOTE
    };

    /** The index of the note column */
    private static final int COLUMN_INDEX_NOTE = 1;

    // This is our state data that is stored when freezing.
    private static final String ORIGINAL_CONTENT = "origContent";

    // Identifiers for our menu items.
    private static final int REVERT_ID = Menu.FIRST;
    private static final int DISCARD_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;

    // The different distinct states the activity can be run in.
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);

                // Draws a line in the background from the left of the rectangle to the right,
                // at a vertical position one dip below the baseline, using the "paint" object
                // for details.
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.
         */
        final Intent intent = getIntent();

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         */

        // Gets the action that triggered the intent filter for this Activity
        final String action = intent.getAction();

        // For an edit action:
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            mState = STATE_EDIT;
            mUri = intent.getData();

        // For an insert action:
        } else if (Intent.ACTION_INSERT.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             */
            if (mUri == null) {
                // Writes the log identifier, a message, and the URI that failed.
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        // If the action was other than EDIT or INSERT:
        } else {
            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Sets the layout for this Activity. See res/layout/note_editor.xml
        setContentView(R.layout.note_editor);

        // Gets a handle to the EditText in the the layout.
        mText = (EditText) findViewById(R.id.note);

        /*
         * Using the URI passed in with the triggering Intent, gets the note or notes in
         * the provider.
         * Note: This is being done on the UI thread. It will block the thread until the query
         * completes. In a sample app, going against a simple provider based on a local database,
         * the block will be momentary, but in a real app you should use
         * android.content.AsyncQueryHandler or android.os.AsyncTask.
         */
        mCursor = managedQuery(
            mUri,         // The URI that gets multiple notes from the provider.
            PROJECTION,   // A projection that returns the note ID and note content for each note.
            null,         // No "where" clause selection criteria.
            null,         // No "where" clause selection values.
            null          // Use the default sort order (modification date, descending)
        );

        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     *
     * Moves to the first note in the list, sets an appropriate title for the action chosen by
     * the user, puts the note contents into the TextView, and saves the original text as a
     * backup.
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         */
        if (mCursor != null) {

            /* Moves to the first record. Always call moveToFirst() before accessing data in
             * a Cursor for the first time. The semantics of using a Cursor are that when it is
             * created, its internal index is pointing to a "place" immediately before the first
             * record.
             */
            mCursor.moveToFirst();

            // Modifies the window title for the Activity according to the current Activity state.
            if (mState == STATE_EDIT) {

                // Sets the title to "edit"
                setTitle(getText(R.string.title_edit));
            } else if (mState == STATE_INSERT) {

                // Sets the title to "create"
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             */

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            // the text cursor's position.
            String note = mCursor.getString(COLUMN_INDEX_NOTE);
            mText.setTextKeepState(note);

            // Stores the original note text, to allow the user to revert changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        /*
         * Something is wrong. The Cursor should always contain data. Report an error in the
         * note.
         */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    /**
     * This method is called when an Activity loses focus during its normal operation, and is then
     * later on killed. The Activity has a chance to save its state so that the system can restore
     * it.
     *
     * Notice that this method isn't a normal part of the Activity lifecycle. It won't be called
     * if the user simply navigates away from the Activity.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Saves away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    /**
     * This method is called when the Activity loses focus.
     *
     * For Activity objects that edit information, onPause() may be the one place where changes are
     * saved. The Android application model is predicated on the idea that "save" and "exit" aren't
     * required actions. When users navigate away from an Activity, they shouldn't have to go back
     * to it to complete their work. The act of going away should save everything and leave the
     * Activity in a state where Android can destroy it if necessary.
     *
     * If the user hasn't done anything, then this deletes or clears out the note, otherwise it
     * writes the user's work to the provider.
     */
    @Override
    protected void onPause() {
        super.onPause();

        /*
         * Tests to see that the query operation didn't fail (see onCreate()). The Cursor object
         * will exist, even if no records were returned, unless the query failed because of some
         * exception or error.
         *
         */
        if (mCursor != null) {

            // Get the current note text.
            String text = mText.getText().toString();
            int note_length = text.length();

            /*
             * If the Activity is in the midst of finishing and there is no text in the current
             * note, returns a result of CANCELED to the caller, and deletes the note. This is done
             * even if the note was being edited, the assumption being that the user wanted to
             * "clear out" (delete) the note.
             */
            if (isFinishing() && (note_length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

            /*
             * Writes the edits to the provider. The note has been edited if an existing note was
             * retrieved into the editor *or* if a new note was inserted. In the latter case,
             * onCreate() inserted a new empty note into the provider, and it is this new note
             * that is being edited.
             */
            } else {
                // Creates a map to contain the new values for the columns
                ContentValues values = new ContentValues();

                // In the values map, sets the modification date column to the current time.
                values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

                // Creates a title for a newly-inserted note.
                if (mState == STATE_INSERT) {

                    // Gets the first 30 characters of the note, or the entire note if it's
                    // less than 30 characters long.
                    String title = text.substring(0, Math.min(30, note_length));

                    // If the note's entire length is greater than 30, then the title is 30
                    // characters long. Finds the last occurrence of blank in the title, and
                    // removes all characters to the right of it from the title string.
                    if (note_length > 30) {
                        int lastSpace = title.lastIndexOf(' ');
                        if (lastSpace > 0) {
                            title = title.substring(0, lastSpace);
                        }
                    }
                    // In the values map, set the title column to the new title.
                    values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
                }

                // In the values map, sets the note text column to the text in the View.
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

                /*
                 * Updates the provider with the new values in the map. The ListView is updated
                 * automatically. The provider sets this up by setting the notification URI for
                 * query Cursor objects to the incoming URI. The content resolver is thus
                 * automatically notified when the Cursor for the URI changes, and the UI is
                 * updated.
                 * Note: This is being done on the UI thread. It will block the thread until the
                 * update completes. In a sample app, going against a simple provider based on a
                 * local database, the block will be momentary, but in a real app you should use
                 * android.content.AsyncQueryHandler or android.os.AsyncTask.
                 */
                getContentResolver().update(
                    mUri,    // The URI for the record to update.
                    values,  // The map of column names and new values to apply to them.
                    null,    // No selection criteria are used, so no where columns are necessary.
                    null     // No where columns are used, so no where arguments are necessary.
                );
            }
        }
    }

    /**
     * This method is called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Builds the menus for editing and inserting, and adds in alternative actions that
     * registered themselves to handle the MIME types for this application.
     *
     * @param menu A Menu object to which items should be added.
     * @return True to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Builds the menus that are shown when editing. These are 'revert' to undo changes, and
        // 'delete' to delete the note.
        if (mState == STATE_EDIT) {

            // Adds the 'revert' menu item, and sets its shortcut to numeric 0, letter 'r' and its
            // icon to the Android standard revert icon.
            menu.add(0, REVERT_ID, 0, R.string.menu_revert)
                .setShortcut('0', 'r')
                .setIcon(android.R.drawable.ic_menu_revert);

            // Adds the 'delete' menu item, and sets its shortcut to numeric 1, letter 'd' and its
            // icon to the Android standard delete icon
            menu.add(0, DELETE_ID, 0, R.string.menu_delete)
                .setShortcut('1', 'd')
                .setIcon(android.R.drawable.ic_menu_delete);

        // Builds the menus that are shown when inserting. The only option is 'Discard' to throw
        // away the new note.
        } else {

            // Adds the 'discard' menu item, using the 'delete' shortcuts and icon.
            menu.add(0, DISCARD_ID, 0, R.string.menu_discard)
                    .setShortcut('0', 'd')
                    .setIcon(android.R.drawable.ic_menu_delete);
        }

        /*
         * Appends menu items for any Activity declarations that implement an alternative action
         * for this Activity's MIME type, one menu item for each Activity.
         */
        // Makes a new Intent with the URI data passed to this Activity
        Intent intent = new Intent(null, getIntent().getData());

        // Adds the ALTERNATIVE category to the Intent.
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

        /*
         * Constructs a new ComponentName object that represents the current Activity.
         */
        ComponentName component = new ComponentName(
            this,
            NoteEditor.class);

        /*
         * In the ALTERNATIVE menu group, adds an option for each Activity that is registered to
         * handle this Activity's MIME type. The Intent describes what type of items should be
         * added to the menu; in this case, Activity declarations with category ALTERNATIVE.
         */
        menu.addIntentOptions(
            Menu.CATEGORY_ALTERNATIVE,  // The menu group to add the items to.
            Menu.NONE,                  // No unique ID is needed.
            Menu.NONE,                  // No ordering is needed.
            component,                  // The current Activity object's component name
            null,                       // No specific items need to be placed first.
            intent,                     // The intent containing the type of items to add.
            Menu.NONE,                  // No flags are necessary.
            null                        // No need to generate an array of menu items.
        );

        // The method returns TRUE, so that further menu processing is not done.
        return true;
    }

    /**
     * This method is called when a menu item is selected. Android passes in the selected item.
     * The switch statement in this method calls the appropriate method to perform the action the
     * user chose.
     *
     * @param item The selected MenuItem
     * @return True to indicate that the item was processed, and no further work is necessary. False
     * to proceed to further processing as indicated in the MenuItem object.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Chooses the action to perform
        switch (item.getItemId()) {

        // Deletes the note and close the Activity.
        case DELETE_ID:
            deleteNote();
            finish();
            break;

        // Discards the new note.
        case DISCARD_ID:
            cancelNote();
            break;

        // Discards any changes to an edited note.
        case REVERT_ID:
            cancelNote();
            break;
        }

        // Continues with processing the menu item. In effect, if the item was an alternative
        // action, this invokes the Activity for that action.
        return super.onOptionsItemSelected(item);
    }

    /**
     * Takes care of canceling work on a note.  Deletes the note if we
     * had created it, otherwise reverts to the original text.
     */
    private final void cancelNote() {

        /*
         * Tests to see that the original query operation didn't fail (see onCreate()). The Cursor
         * object will exist, even if no records were returned, unless the query failed because of
         * some exception or error.
         */
        if (mCursor != null) {

            /*
             * If the user is editing a note, and asked to discard or revert, this puts the
             * previous note contents back into the note.
             */
            if (mState == STATE_EDIT) {

                // Closes the previous cursor prior to updating the provider
                mCursor.close();
                mCursor = null;

                // Creates a new values map
                ContentValues values = new ContentValues();

                // Puts the original notes content into the values map. The variable was set in
                // onResume().
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);

                /*
                 * Update the provider with the reverted note content.
                 *
                 * Note: This is being done on the UI thread. It will block the thread until the
                 * update completes. In a sample app, going against a simple provider based on a
                 * local database, the block will be momentary, but in a real app you should use
                 * android.content.AsyncQueryHandler or android.os.AsyncTask.
                 */
                getContentResolver().update(
                    mUri,    // The URI of the note or notes.
                    values,  // The reverted values to put into the provider.
                    null,    // No selection criteria, so no where columns are needed.
                    null     // No where columns are used, so no where values are needed.
                );

            /*
             * If the user was inserting a note and decides to discard it, this deletes the note.
             */
            } else if (mState == STATE_INSERT) {
                // Deletes the note.
                deleteNote();
            }
        }

        // Returns a result of CANCELED to the calling Activity.
        setResult(RESULT_CANCELED);

        // Finishes the Activity. Once the user deletes or discards, nothing more can be done, so
        // return to the calling Activity, either NotesList or some other Activity.
        finish();
    }

    /**
     * This method deletes a note from the provider.
     */
    private final void deleteNote() {
        /*
         * Tests to see that the original query operation didn't fail (see onCreate()). The Cursor
         * object will exist, even if no records were returned, unless the query failed because of
         * some exception or error.
         */
        if (mCursor != null) {

            // Gets rid of all the Cursor's resources, and deactivates it.
            mCursor.close();
            mCursor = null;

            /*
             * Deletes the note based on the ID in the URI.
             *
             * Note: This is being done on the UI thread. It will block the thread until the
             * delete completes. In a sample app, going against a simple provider based on a
             * local database, the block will be momentary, but in a real app you should use
             * android.content.AsyncQueryHandler android.os.AsyncTask.
             */

            getContentResolver().delete(
                mUri,  // The URI of the note to delete.
                null,  // No selection criteria are specified, so no where columns are needed.
                null   // No where columns are specified, so no where values are needed.
            );

            // Throws away any text currently showing in the View.
            mText.setText("");
        }
    }
}
