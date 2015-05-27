/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.autobackupsample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.autobackupsample.MainActivityFragment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * The purpose of AddFileActivity activity is to create a data file based on the
 * file name and size parameters specified as an Intent external parameters or with the
 * activity UI.
 * <p/>
 * The optional intent parameters are
 * {@link com.example.android.autobackupsample.AddFileActivity#FILE_NAME} and
 * {@link com.example.android.autobackupsample.AddFileActivity#FILE_SIZE_IN_BYTES}.
 * {@link com.example.android.autobackupsample.AddFileActivity#FILE_STORAGE}.
 * <p/>
 * The activity will return an
 * {@link com.example.android.autobackupsample.MainActivityFragment#ADD_FILE_RESULT_ERROR}
 * if intent parameters are specified incorrectly or it will display Toast messages to the user
 * if those parameters are specified via the activity UI.
 */
public class AddFileActivity extends Activity {

    private static final String TAG = "AutoBackupSample";

    /**
     * The intent parameter that specifies a file name. The file name must be unique for the
     * application internal directory.
     */
    public static final String FILE_NAME = "file_name";

    /**
     * The intent parameter that specifies a file size in bytes. The size must be a number
     * larger or equal to 0.
     */
    public static final String FILE_SIZE_IN_BYTES = "file_size_in_bytes";

    /**
     * The file storage is an optional parameter. It should be one of these:
     * "INTERNAL", "EXTERNAL", "DONOTBACKUP". The default option is "INTERNAL".
     */
    public static final String FILE_STORAGE = "file_storage";

    /**
     * A file size multiplier. It is used to calculate the total number of bytes to be added
     * to the file.
     */
    private int mSizeMultiplier = 1;

    /**
     * Defines File Storage options.
     */
    private static enum FileStorage {
        INTERNAL,
        EXTERNAL,
        DONOTBACKUP;
    }

    /**
     * Contains a selected by a user file storage option.
     */
    private FileStorage mFileStorage = FileStorage.INTERNAL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_file);
        initFileSizeSpinner();
        initFileStorageSpinner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If an intent has extra parameters, create the file and finish the activity.
        if (getIntent().hasExtra(FILE_NAME) && getIntent().hasExtra(FILE_SIZE_IN_BYTES)) {
            String fileName = getIntent().getStringExtra(FILE_NAME);
            String sizeInBytesParamValue = getIntent().getStringExtra(FILE_SIZE_IN_BYTES);
            String fileStorageParamValue = FileStorage.INTERNAL.toString();

            if (getIntent().hasExtra(FILE_STORAGE)) {
                fileStorageParamValue = getIntent().getStringExtra(FILE_STORAGE);
            }

            if (TextUtils.isEmpty(fileName) ||
                    isFileExists(fileName) ||
                    !isSizeValid(sizeInBytesParamValue) ||
                    !isFileStorageParamValid(fileStorageParamValue)) {
                setResult(MainActivityFragment.ADD_FILE_RESULT_ERROR);
                finish();
                return;
            }

            mFileStorage = FileStorage.valueOf(fileStorageParamValue);

            if (mFileStorage == FileStorage.EXTERNAL && !Utils.isExternalStorageAvailable()) {
                setResult(MainActivityFragment.ADD_FILE_RESULT_ERROR);
                finish();
                return;
            }

            createFileWithRandomDataAndFinishActivity(fileName, mFileStorage,
                    sizeInBytesParamValue);
        }
    }

    /**
     * A handler function for a Create File button click event.
     *
     * @param view a reference to the Create File button view.
     */
    public void onCreateFileButtonClick(View view) {
        EditText fileNameEditText = (EditText) findViewById(R.id.file_name);
        EditText fileSizeEditText = (EditText) findViewById(R.id.file_size);
        String fileName = fileNameEditText.getText().toString();
        String fileSizeEditTextValue = fileSizeEditText.getText().toString();

        if (TextUtils.isEmpty(fileName) || isFileExists(fileName)) {
            Toast toast = Toast.makeText(this, getText(R.string.file_exists), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }

        if (!isSizeValid(fileSizeEditTextValue)) {
            Toast toast = Toast.makeText(this, getText(R.string.file_size_is_invalid),
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }

        long fileSize = Integer.valueOf(fileSizeEditTextValue) * mSizeMultiplier;

        if (mFileStorage == FileStorage.EXTERNAL && !Utils.isExternalStorageAvailable()) {
            Toast toast = Toast.makeText(this,
                    getText(R.string.external_storage_unavailable), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            return;
        }

        createFileWithRandomDataAndFinishActivity(fileName, mFileStorage, String.valueOf(fileSize));
    }

    private void initFileSizeSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.file_size_spinner);
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this, R.array.file_size_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sizeMeasure = adapter.getItem(position).toString();
                mSizeMultiplier = (int) Math.pow(1024, position);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, String.format("Selected: %s, %d", sizeMeasure,
                            mSizeMultiplier));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initFileStorageSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.storage_spinner);
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this, R.array.file_storage_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFileStorage = FileStorage.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void createFileWithRandomDataAndFinishActivity(String fileName, FileStorage storage,
                                                           String sizeInBytes) {
        long size = Long.valueOf(sizeInBytes);
        File file = null;
        FileOutputStream out = null;
        BufferedOutputStream bufOut = null;
        try {
            switch (storage) {
                case INTERNAL:
                    file = getInternalFile(fileName);
                    out = openFileOutput(file.getName(), Context.MODE_PRIVATE);
                    break;
                case EXTERNAL:
                    assert Utils.isExternalStorageAvailable() :
                            "The external storage is not available";
                    File externalAppDir = getExternalFilesDir(null);
                    file = new File(externalAppDir, fileName);
                    out = new FileOutputStream(file);
                    break;
                case DONOTBACKUP:
                    file = new File(getNoBackupFilesDir(), fileName);
                    out = new FileOutputStream(file);
                    break;
            }

            if (file == null || out == null) {
                Log.d(TAG, "Unable to create file output stream");
                // Returning back to the caller activity.
                setResult(MainActivityFragment.ADD_FILE_RESULT_ERROR);
                finish();
                return;
            }

            bufOut = new BufferedOutputStream(out);
            for (int i = 0; i < size; i++) {
                byte b = (byte) (255 * Math.random());
                bufOut.write(b);
            }

            String message = String.format("File created: %s, size: %s bytes",
                    file.getAbsolutePath(), sizeInBytes);

            Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.show();
            Log.d(TAG, message);

            // Returning back to the caller activity.
            setResult(MainActivityFragment.ADD_FILE_RESULT_SUCCESS);
            finish();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            // Returning back to the caller activity.
            setResult(MainActivityFragment.ADD_FILE_RESULT_ERROR);
            finish();
        } finally {
            if (bufOut != null) {
                try {
                    bufOut.close();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }
    }

    private boolean isFileExists(String fileName) {
        File file = getInternalFile(fileName);
        if (file.exists()) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "This file exists: " + file.getName());
            }
            return true;
        }
        return false;
    }

    private boolean isSizeValid(String sizeInBytesParamValue) {
        long sizeInBytes = 0;
        try {
            sizeInBytes = Long.valueOf(sizeInBytesParamValue);
        } catch (NumberFormatException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Invalid file size: " + sizeInBytesParamValue);
            }
            return false;
        }

        // Validate file size value. It should be 0 or a positive number.
        if (sizeInBytes < 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Invalid file size: " + sizeInBytes);
            }
            return false;
        }
        return true;
    }

    private boolean isFileStorageParamValid(String fileStorage) {
        try {
            mFileStorage = FileStorage.valueOf(fileStorage);
        } catch (IllegalArgumentException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Invalid file storage: " + fileStorage);
            }
            return false;
        }
        return true;
    }

    private File getInternalFile(String fileName) {
        File internalAppDir = getFilesDir();
        return new File(internalAppDir, fileName);
    }

}