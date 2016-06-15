/*
* Copyright 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.scopeddirectoryaccess;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays the directory contents.
 */
public class ScopedDirectoryAccessFragment extends Fragment {

    private static final String DIRECTORY_ENTRIES_KEY = "directory_entries";
    private static final String SELECTED_DIRECTORY_KEY = "selected_directory";
    private static final int OPEN_DIRECTORY_REQUEST_CODE = 1;

    private static final String[] DIRECTORY_SELECTION = new String[]{
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    };

    private Activity mActivity;
    private StorageManager mStorageManager;
    private TextView mCurrentDirectoryTextView;
    private TextView mNothingInDirectoryTextView;
    private TextView mPrimaryVolumeNameTextView;
    private Spinner mDirectoriesSpinner;
    private DirectoryEntryAdapter mAdapter;
    private ArrayList<DirectoryEntry> mDirectoryEntries;

    public static ScopedDirectoryAccessFragment newInstance() {
        ScopedDirectoryAccessFragment fragment = new ScopedDirectoryAccessFragment();
        return fragment;
    }

    public ScopedDirectoryAccessFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
        mStorageManager = mActivity.getSystemService(StorageManager.class);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getActivity().getContentResolver().takePersistableUriPermission(data.getData(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updateDirectoryEntries(data.getData());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scoped_directory_access, container, false);
    }

    @Override
    public void onViewCreated(final View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        mCurrentDirectoryTextView = (TextView) rootView
                .findViewById(R.id.textview_current_directory);
        mNothingInDirectoryTextView = (TextView) rootView
                .findViewById(R.id.textview_nothing_in_directory);
        mPrimaryVolumeNameTextView = (TextView) rootView
                .findViewById(R.id.textview_primary_volume_name);

        // Set onClickListener for the primary volume
        Button openPictureButton = (Button) rootView
                .findViewById(R.id.button_open_directory_primary_volume);
        openPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selected = mDirectoriesSpinner.getSelectedItem().toString();
                String directoryName = getDirectoryName(selected);
                StorageVolume storageVolume = mStorageManager.getPrimaryStorageVolume();
                Intent intent = storageVolume.createAccessIntent(directoryName);
                startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
            }
        });

        // Set onClickListener for the external volumes if exists
        List<StorageVolume> storageVolumes = mStorageManager.getStorageVolumes();
        LinearLayout containerVolumes = (LinearLayout) mActivity
                .findViewById(R.id.container_volumes);
        for (final StorageVolume volume : storageVolumes) {
            String volumeDescription = volume.getDescription(mActivity);
            if (volume.isPrimary()) {
                // Primary volume area is already added...
                if (volumeDescription != null) {
                    // ...but with a default name: set it to the real name when available.
                    mPrimaryVolumeNameTextView.setText(volumeDescription);
                }
                continue;
            }
            LinearLayout volumeArea = (LinearLayout) mActivity.getLayoutInflater()
                    .inflate(R.layout.volume_entry, containerVolumes);
            TextView volumeName = (TextView) volumeArea.findViewById(R.id.textview_volume_name);
            volumeName.setText(volumeDescription);
            Button button = (Button) volumeArea.findViewById(R.id.button_open_directory);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String selected = mDirectoriesSpinner.getSelectedItem().toString();
                    String directoryName = getDirectoryName(selected);
                    Intent intent = volume.createAccessIntent(directoryName);
                    startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
                }
            });
        }
        RecyclerView recyclerView = (RecyclerView) rootView
                .findViewById(R.id.recyclerview_directory_entries);
        if (savedInstanceState != null) {
            mDirectoryEntries = savedInstanceState.getParcelableArrayList(DIRECTORY_ENTRIES_KEY);
            mCurrentDirectoryTextView.setText(savedInstanceState.getString(SELECTED_DIRECTORY_KEY));
            mAdapter = new DirectoryEntryAdapter(mDirectoryEntries);
            if (mAdapter.getItemCount() == 0) {
                mNothingInDirectoryTextView.setVisibility(View.VISIBLE);
            }
        } else {
            mDirectoryEntries = new ArrayList<>();
            mAdapter = new DirectoryEntryAdapter();
        }
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDirectoriesSpinner = (Spinner) rootView.findViewById(R.id.spinner_directories);
        ArrayAdapter<CharSequence> directoriesAdapter = ArrayAdapter
                .createFromResource(getActivity(),
                        R.array.directories, android.R.layout.simple_spinner_item);
        directoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDirectoriesSpinner.setAdapter(directoriesAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_DIRECTORY_KEY, mCurrentDirectoryTextView.getText().toString());
        outState.putParcelableArrayList(DIRECTORY_ENTRIES_KEY, mDirectoryEntries);
    }

    private void updateDirectoryEntries(Uri uri) {
        mDirectoryEntries.clear();
        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));

        try (Cursor docCursor = contentResolver
                .query(docUri, DIRECTORY_SELECTION, null, null, null)) {
            while (docCursor != null && docCursor.moveToNext()) {
                mCurrentDirectoryTextView.setText(docCursor.getString(docCursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME)));
            }
        }

        try (Cursor childCursor = contentResolver
                .query(childrenUri, DIRECTORY_SELECTION, null, null, null)) {
            while (childCursor != null && childCursor.moveToNext()) {
                DirectoryEntry entry = new DirectoryEntry();
                entry.fileName = childCursor.getString(childCursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                entry.mimeType = childCursor.getString(childCursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_MIME_TYPE));
                mDirectoryEntries.add(entry);
            }

            if (mDirectoryEntries.isEmpty()) {
                mNothingInDirectoryTextView.setVisibility(View.VISIBLE);
            } else {
                mNothingInDirectoryTextView.setVisibility(View.GONE);
            }
            mAdapter.setDirectoryEntries(mDirectoryEntries);
            mAdapter.notifyDataSetChanged();
        }
    }

    private String getDirectoryName(String name) {
        switch (name) {
            case "ALARMS":
                return Environment.DIRECTORY_ALARMS;
            case "DCIM":
                return Environment.DIRECTORY_DCIM;
            case "DOCUMENTS":
                return Environment.DIRECTORY_DOCUMENTS;
            case "DOWNLOADS":
                return Environment.DIRECTORY_DOWNLOADS;
            case "MOVIES":
                return Environment.DIRECTORY_MOVIES;
            case "MUSIC":
                return Environment.DIRECTORY_MUSIC;
            case "NOTIFICATIONS":
                return Environment.DIRECTORY_NOTIFICATIONS;
            case "PICTURES":
                return Environment.DIRECTORY_PICTURES;
            case "PODCASTS":
                return Environment.DIRECTORY_PODCASTS;
            case "RINGTONES":
                return Environment.DIRECTORY_RINGTONES;
            default:
                throw new IllegalArgumentException("Invalid directory representation: " + name);
        }
    }
}
