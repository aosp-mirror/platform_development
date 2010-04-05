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

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

/**
 * This agent implementation is similar to the {@link ExampleAgent} one, but
 * stores each distinct piece of application data in a separate record within
 * the backup data set.  These records are updated independently: if the user
 * changes the state of one of the UI's checkboxes, for example, only that
 * datum's backup record is updated, not the entire data file.
 */
public class MultiRecordExampleAgent extends BackupAgent {
    // Key strings for each record in the backup set
    static final String FILLING_KEY = "filling";
    static final String MAYO_KEY = "mayo";
    static final String TOMATO_KEY = "tomato";

    // Current live data, read from the application's data file
    int mFilling;
    boolean mAddMayo;
    boolean mAddTomato;

    /** The location of the application's persistent data file */
    File mDataFile;

    @Override
    public void onCreate() {
        // Cache a File for the app's data
        mDataFile = new File(getFilesDir(), BackupRestoreActivity.DATA_FILE_NAME);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {
        // First, get the current data from the application's file.  This
        // may throw an IOException, but in that case something has gone
        // badly wrong with the app's data on disk, and we do not want
        // to back up garbage data.  If we just let the exception go, the
        // Backup Manager will handle it and simply skip the current
        // backup operation.
        synchronized (BackupRestoreActivity.sDataLock) {
            RandomAccessFile file = new RandomAccessFile(mDataFile, "r");
            mFilling = file.readInt();
            mAddMayo = file.readBoolean();
            mAddTomato = file.readBoolean();
        }

        // If this is the first backup ever, we have to back up everything
        boolean forceBackup = (oldState == null);

        // Now read the state as of the previous backup pass, if any
        int lastFilling = 0;
        boolean lastMayo = false;
        boolean lastTomato = false;

        if (!forceBackup) {

            FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
            DataInputStream in = new DataInputStream(instream);

            try {
                // Read the state as of the last backup
                lastFilling = in.readInt();
                lastMayo = in.readBoolean();
                lastTomato = in.readBoolean();
            } catch (IOException e) {
                // If something went wrong reading the state file, be safe and
                // force a backup of all the data again.
                forceBackup = true;
            }
        }

        // Okay, now check each datum to see whether we need to back up a new value.  We'll
        // reuse the bytearray buffering stream for each datum.  We also use a little
        // helper routine to avoid some code duplication when writing the two boolean
        // records.
        ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bufStream);

        if (forceBackup || (mFilling != lastFilling)) {
            // bufStream.reset();   // not necessary the first time, but good to remember
            out.writeInt(mFilling);
            writeBackupEntity(data, bufStream, FILLING_KEY);
        }

        if (forceBackup || (mAddMayo != lastMayo)) {
            bufStream.reset();
            out.writeBoolean(mAddMayo);
            writeBackupEntity(data, bufStream, MAYO_KEY);
        }

        if (forceBackup || (mAddTomato != lastTomato)) {
            bufStream.reset();
            out.writeBoolean(mAddTomato);
            writeBackupEntity(data, bufStream, TOMATO_KEY);
        }

        // Finally, write the state file that describes our data as of this backup pass
        writeStateFile(newState);
    }

    /**
     * Write out the new state file:  the version number, followed by the
     * three bits of data as we sent them off to the backup transport.
     */
    void writeStateFile(ParcelFileDescriptor stateFile) throws IOException {
        FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outstream);

        out.writeInt(mFilling);
        out.writeBoolean(mAddMayo);
        out.writeBoolean(mAddTomato);
    }

    // Helper: write the boolean 'value' as a backup record under the given 'key',
    // reusing the given buffering stream & data writer objects to do so.
    void writeBackupEntity(BackupDataOutput data, ByteArrayOutputStream bufStream, String key)
            throws IOException {
        byte[] buf = bufStream.toByteArray();
        data.writeEntityHeader(key, buf.length);
        data.writeEntityData(buf, buf.length);
    }

    /**
     * On restore, we pull the various bits of data out of the restore stream,
     * then reconstruct the application's data file inside the shared lock.  A
     * restore data set will always be the full set of records supplied by the
     * application's backup operations.
     */
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {

        // Consume the restore data set, remembering each bit of application state
        // that we see along the way
        while (data.readNextHeader()) {
            String key = data.getKey();
            int dataSize = data.getDataSize();

            // In this implementation, we trust that we won't see any record keys
            // that we don't understand.  Since we expect to handle them all, we
            // go ahead and extract the data for each record before deciding how
            // it will be handled.
            byte[] dataBuf = new byte[dataSize];
            data.readEntityData(dataBuf, 0, dataSize);
            ByteArrayInputStream instream = new ByteArrayInputStream(dataBuf);
            DataInputStream in = new DataInputStream(instream);

            if (FILLING_KEY.equals(key)) {
                mFilling = in.readInt();
            } else if (MAYO_KEY.equals(key)) {
                mAddMayo = in.readBoolean();
            } else if (TOMATO_KEY.equals(key)) {
                mAddTomato = in.readBoolean();
            }
        }

        // Now we're ready to write out a full new dataset for the application.  Note that
        // the restore process is intended to *replace* any existing or default data, so
        // we can just go ahead and overwrite it all.
        synchronized (BackupRestoreActivity.sDataLock) {
            RandomAccessFile file = new RandomAccessFile(mDataFile, "rw");
            file.setLength(0L);
            file.writeInt(mFilling);
            file.writeBoolean(mAddMayo);
            file.writeBoolean(mAddTomato);
        }

        // Finally, write the state file that describes our data as of this restore pass.
        writeStateFile(newState);
    }
}
