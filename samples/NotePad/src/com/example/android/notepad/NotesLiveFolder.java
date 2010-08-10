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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

public class NotesLiveFolder extends Activity {
    /**
     * The URI for the Notes Live Folder content provider.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + NotePad.AUTHORITY + "/live_folders/notes");

    public static final Uri NOTE_URI = Uri.parse("content://"
            + NotePad.AUTHORITY + "/notes/#");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
            // Build the live folder intent.
            final Intent liveFolderIntent = new Intent();

            liveFolderIntent.setData(CONTENT_URI);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME,
                    getString(R.string.live_folder_name));
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
                    Intent.ShortcutIconResource.fromContext(this,
                            R.drawable.live_folder_notes));
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                    LiveFolders.DISPLAY_MODE_LIST);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT,
                    new Intent(Intent.ACTION_EDIT, NOTE_URI));

            // The result of this activity should be a live folder intent.
            setResult(RESULT_OK, liveFolderIntent);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }
}
