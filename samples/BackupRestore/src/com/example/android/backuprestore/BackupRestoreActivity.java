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

import android.app.Activity;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This example is intended to demonstrate a few approaches that an Android
 * application developer can take when implementing a
 * {@link android.app.backup.BackupAgent BackupAgent}.  This feature, added
 * to the Android platform with API version 8, allows the application to
 * back up its data to a device-provided storage location, transparently to
 * the user.  If the application is uninstalled and then reinstalled, or if
 * the user starts using a new Android device, the backed-up information
 * can be provided automatically when the application is reinstalled.
 *
 * <p>Participating in the backup/restore mechanism is simple.  The application
 * provides a class that extends {@link android.app.backup.BackupAgent}, and
 * overrides the two core callback methods
 * {@link android.app.backup.BackupAgent#onBackup(android.os.ParcelFileDescriptor, android.app.backup.BackupDataOutput, android.os.ParcelFileDescriptor) onBackup()}
 * and
 * {@link android.app.backup.BackupAgent#onRestore(android.app.backup.BackupDataInput, int, android.os.ParcelFileDescriptor) onRestore()}.
 * It also publishes the agent class to the operating system by naming the class
 * with the <code>android:backupAgent</code> attribute of the
 * <code>&lt;application&gt;</code> tag in the application's manifest.
 * When a backup or restore operation is performed, the application's agent class
 * is instantiated within the application's execution context and the corresponding
 * method invoked.  Please see the documentation on the
 * {@link android.app.backup.BackupAgent BackupAgent} class for details about the
 * data interchange between the agent and the backup mechanism.
 *
 * <p>This example application maintains a few pieces of simple data, and provides
 * three different sample agent implementations, each illustrating an alternative
 * approach.  The three sample agent classes are:
 *
 * <p><ol type="1">
 * <li>{@link ExampleAgent} - this agent backs up the application's data in a single
 *     record.  It illustrates the direct "by hand" processes of saving backup state for
 *     future reference, sending data to the backup transport, and reading it from a restore
 *     dataset.</li>
 * <li>{@link FileHelperExampleAgent} - this agent takes advantage of the suite of
 *     helper classes provided along with the core BackupAgent API.  By extending
 *     {@link android.app.backup.BackupHelperAgent} and using the targeted
 *     {link android.app.backup.FileBackupHelper FileBackupHelper} class, it achieves
 *     the same result as {@link ExampleAgent} - backing up the application's saved
 *     data file in a single chunk, and restoring it upon request -- in only a few lines
 *     of code.</li>
 * <li>{@link MultiRecordExampleAgent} - this agent stores each separate bit of data
 *     managed by the UI in separate records within the backup dataset.  It illustrates
 *     how an application's backup agent can do selective updates of only what information
 *     has changed since the last backup.</li></ol>
 *
 * <p>You can build the application to use any of these agent implementations simply by
 * changing the class name supplied in the <code>android:backupAgent</code> manifest
 * attribute to indicate the agent you wish to use.  <strong>Note:</strong> the backed-up
 * data and backup-state tracking of these agents are not compatible!  If you change which
 * agent the application uses, you should also wipe the backup state associated with
 * the application on your handset.  The 'bmgr' shell application on the device can
 * do this; simply run the following command from your desktop computer while attached
 * to the device via adb:
 *
 * <p><code>adb shell bmgr wipe com.example.android.backuprestore</code>
 *
 * <p>You can then install the new version of the application, and its next backup pass
 * will start over from scratch with the new agent.
 */
public class BackupRestoreActivity extends Activity {
    static final String TAG = "BRActivity";

    /**
     * We serialize access to our persistent data through a global static
     * object.  This ensures that in the unlikely event of the our backup/restore
     * agent running to perform a backup while our UI is updating the file, the
     * agent will not accidentally read partially-written data.
     *
     * <p>Curious but true: a zero-length array is slightly lighter-weight than
     * merely allocating an Object, and can still be synchronized on.
     */
    static final Object[] sDataLock = new Object[0];

    /** Also supply a global standard file name for everyone to use */
    static final String DATA_FILE_NAME = "saved_data";

    /** The various bits of UI that the user can manipulate */
    RadioGroup mFillingGroup;
    CheckBox mAddMayoCheckbox;
    CheckBox mAddTomatoCheckbox;

    /** Cache a reference to our persistent data file */
    File mDataFile;

    /** Also cache a reference to the Backup Manager */
    BackupManager mBackupManager;

    /** Set up the activity and populate its UI from the persistent data. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /** Establish the activity's UI */
        setContentView(R.layout.backup_restore);

        /** Once the UI has been inflated, cache the controls for later */
        mFillingGroup = (RadioGroup) findViewById(R.id.filling_group);
        mAddMayoCheckbox = (CheckBox) findViewById(R.id.mayo);
        mAddTomatoCheckbox = (CheckBox) findViewById(R.id.tomato);

        /** Set up our file bookkeeping */
        mDataFile = new File(getFilesDir(), BackupRestoreActivity.DATA_FILE_NAME);

        /** It is handy to keep a BackupManager cached */
        mBackupManager = new BackupManager(this);

        /**
         * Finally, build the UI from the persistent store
         */
        populateUI();
    }

    /**
     * Configure the UI based on our persistent data, creating the
     * data file and establishing defaults if necessary.
     */
    void populateUI() {
        RandomAccessFile file;

        // Default values in case there's no data file yet
        int whichFilling = R.id.pastrami;
        boolean addMayo = false;
        boolean addTomato = false;

        /** Hold the data-access lock around access to the file */
        synchronized (BackupRestoreActivity.sDataLock) {
            boolean exists = mDataFile.exists();
            try {
                file = new RandomAccessFile(mDataFile, "rw");
                if (exists) {
                    Log.v(TAG, "datafile exists");
                    whichFilling = file.readInt();
                    addMayo = file.readBoolean();
                    addTomato = file.readBoolean();
                    Log.v(TAG, "  mayo=" + addMayo
                            + " tomato=" + addTomato
                            + " filling=" + whichFilling);
                } else {
                    // The default values were configured above: write them
                    // to the newly-created file.
                    Log.v(TAG, "creating default datafile");
                    writeDataToFileLocked(file,
                            addMayo, addTomato, whichFilling);

                    // We also need to perform an initial backup; ask for one
                    mBackupManager.dataChanged();
                }
            } catch (IOException ioe) {
                
            }
        }

        /** Now that we've processed the file, build the UI outside the lock */
        mFillingGroup.check(whichFilling);
        mAddMayoCheckbox.setChecked(addMayo);
        mAddTomatoCheckbox.setChecked(addTomato);

        /**
         * We also want to record the new state when the user makes changes,
         * so install simple observers that do this
         */
        mFillingGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group,
                            int checkedId) {
                        // As with the checkbox listeners, rewrite the
                        // entire state file
                        Log.v(TAG, "New radio item selected: " + checkedId);
                        recordNewUIState();
                    }
                });

        CompoundButton.OnCheckedChangeListener checkListener
                = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                // Whichever one is altered, we rewrite the entire UI state
                Log.v(TAG, "Checkbox toggled: " + buttonView);
                recordNewUIState();
            }
        };
        mAddMayoCheckbox.setOnCheckedChangeListener(checkListener);
        mAddTomatoCheckbox.setOnCheckedChangeListener(checkListener);
    }

    /**
     * Handy helper routine to write the UI data to a file.
     */
    void writeDataToFileLocked(RandomAccessFile file,
            boolean addMayo, boolean addTomato, int whichFilling)
        throws IOException {
            file.setLength(0L);
            file.writeInt(whichFilling);
            file.writeBoolean(addMayo);
            file.writeBoolean(addTomato);
            Log.v(TAG, "NEW STATE: mayo=" + addMayo
                    + " tomato=" + addTomato
                    + " filling=" + whichFilling);
    }

    /**
     * Another helper; this one reads the current UI state and writes that
     * to the persistent store, then tells the backup manager that we need
     * a backup.
     */
    void recordNewUIState() {
        boolean addMayo = mAddMayoCheckbox.isChecked();
        boolean addTomato = mAddTomatoCheckbox.isChecked();
        int whichFilling = mFillingGroup.getCheckedRadioButtonId();
        try {
            synchronized (BackupRestoreActivity.sDataLock) {
                RandomAccessFile file = new RandomAccessFile(mDataFile, "rw");
                writeDataToFileLocked(file, addMayo, addTomato, whichFilling);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to record new UI state");
        }

        mBackupManager.dataChanged();
    }

    /**
     * Click handler, designated in the layout, that runs a restore of the app's
     * most recent data when the button is pressed.
     */
    public void onRestoreButtonClick(View v) {
        Log.v(TAG, "Requesting restore of our most recent data");
        mBackupManager.requestRestore(
                new RestoreObserver() {
                    public void restoreFinished(int error) {
                        /** Done with the restore!  Now draw the new state of our data */
                        Log.v(TAG, "Restore finished, error = " + error);
                        populateUI();
                    }
                }
        );
    }
}
