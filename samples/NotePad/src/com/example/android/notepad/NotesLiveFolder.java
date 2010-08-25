/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.provider.LiveFolders;

/**
 * This Activity creates a live folder Intent and
 * sends it back to HOME. From the data in the Intent, HOME creates a live folder and displays
 * its icon in the Home view.
 * When the user clicks the icon, Home uses the data it got from the Intent to retrieve information
 * from a content provider and display it in a View.
 *
 * The intent filter for this Activity is set to ACTION_CREATE_LIVE_FOLDER, which
 * HOME sends in response to a long press and selection of Live Folder.
 */
public class NotesLiveFolder extends Activity {

    /**
     * All of the work is done in onCreate(). The Activity doesn't actually display a UI.
     * Instead, it sets up an Intent and returns it to its caller (the HOME activity).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Gets the incoming Intent and its action. If the incoming Intent was
         * ACTION_CREATE_LIVE_FOLDER, then create an outgoing Intent with the
         * necessary data and send back OK. Otherwise, send back CANCEL.
         */
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {

            // Creates a new Intent.
            final Intent liveFolderIntent = new Intent();

            /*
             * The following statements put data into the outgoing Intent. Please see
             * {@link android.provider.LiveFolders for a detailed description of these
             * data values. From this data, HOME sets up a live folder.
             */
            // Sets the URI pattern for the content provider backing the folder.
            liveFolderIntent.setData(NotePad.Notes.LIVE_FOLDER_URI);

            // Adds the display name of the live folder as an Extra string.
            String foldername = getString(R.string.live_folder_name);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, foldername);

            // Adds the display icon of the live folder as an Extra resource.
            ShortcutIconResource foldericon =
                Intent.ShortcutIconResource.fromContext(this, R.drawable.live_folder_notes);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, foldericon);

            // Add the display mode of the live folder as an integer. The specified
            // mode causes the live folder to display as a list.
            liveFolderIntent.putExtra(
                    LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                    LiveFolders.DISPLAY_MODE_LIST);

            /*
             * Adds a base action for items in the live folder list, as an Intent. When the
             * user clicks an individual note in the list, the live folder fires this Intent.
             *
             * Its action is ACTION_EDIT, so it triggers the Note Editor activity. Its
             * data is the URI pattern for a single note identified by its ID. The live folder
             * automatically adds the ID value of the selected item to the URI pattern.
             *
             * As a result, Note Editor is triggered and gets a single note to retrieve by ID.
             */
            Intent returnIntent
                    = new Intent(Intent.ACTION_EDIT, NotePad.Notes.CONTENT_ID_URI_PATTERN);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, returnIntent);

            /* Creates an ActivityResult object to propagate back to HOME. Set its result indicator
             * to OK, and sets the returned Intent to the live folder Intent that was just
             * constructed.
             */
            setResult(RESULT_OK, liveFolderIntent);

        } else {

            // If the original action was not ACTION_CREATE_LIVE_FOLDER, creates an
            // ActivityResult with the indicator set to CANCELED, but do not return an Intent
            setResult(RESULT_CANCELED);
        }

        // Closes the Activity. The ActivityObject is propagated back to the caller.
        finish();
    }
}
