/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.backuprestore;

import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;

/**
 * This agent backs up the application's data using the BackupAgentHelper
 * infrastructure.  In this application's case, the backup data is merely
 * a duplicate of the stored data file; that makes it a perfect candidate
 * for backing up using the {@link android.app.backup.FileBackupHelper} class
 * provided by the Android operating system.
 *
 * <p>"Backup helpers" are a general mechanism that an agent implementation
 * uses by extending {@link BackupAgentHelper} rather than the basic
 * {@link BackupAgent} class.
 *
 * <p>By itself, the FileBackupHelper is properly handling the backup and
 * restore of the datafile that we've configured it with, but it does
 * not know about the potential need to use locking around its access
 * to it.  However, it is straightforward to override
 * {@link #onBackup()} and {@link #onRestore()} to supply the necessary locking
 * around the helper's operation.
 */
public class FileHelperExampleAgent extends BackupAgentHelper {
    /**
     * The "key" string passed when adding a helper is a token used to
     * disambiguate between entities supplied by multiple different helper
     * objects.  They only need to be unique among the helpers within this
     * one agent class, not globally unique.
     */
    static final String FILE_HELPER_KEY = "the_file";

    /**
     * The {@link android.app.backup.FileBackupHelper FileBackupHelper} class
     * does nearly all of the work for our use case:  backup and restore of a
     * file stored within our application's getFilesDir() location.  It will
     * also handle files stored at any subpath within that location.  All we
     * need to do is a bit of one-time configuration: installing the helper
     * when this agent object is created.
     */
    @Override
    public void onCreate() {
        // All we need to do when working within the BackupAgentHelper mechanism
        // is to install the helper that will process and back up the files we
        // care about.  In this case, it's just one file.
        FileBackupHelper helper = new FileBackupHelper(this, BackupRestoreActivity.DATA_FILE_NAME);
        addHelper(FILE_HELPER_KEY, helper);
    }

    /**
     * We want to ensure that the UI is not trying to rewrite the data file
     * while we're reading it for backup, so we override this method to
     * supply the necessary locking.
     */
    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
             ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper performs the backup operation
        synchronized (BackupRestoreActivity.sDataLock) {
            super.onBackup(oldState, data, newState);
        }
    }

    /**
     * Adding locking around the file rewrite that happens during restore is
     * similarly straightforward.
     */
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper restores the file from
        // the data provided here.
        synchronized (BackupRestoreActivity.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }
}
