/*
* Copyright 2014 The Android Open Source Project
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

package com.example.android.directoryselection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that demonstrates how to use Directory Selection API.
 */
public class DirectorySelectionFragment extends Fragment {

    private static final String TAG = DirectorySelectionFragment.class.getSimpleName();

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 1;

    Uri mCurrentDirectoryUri;
    TextView mCurrentDirectoryTextView;
    Button mCreateDirectoryButton;
    RecyclerView mRecyclerView;
    DirectoryEntryAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment {@link DirectorySelectionFragment}.
     */
    public static DirectorySelectionFragment newInstance() {
        DirectorySelectionFragment fragment = new DirectorySelectionFragment();
        return fragment;
    }

    public DirectorySelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_directory_selection, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        rootView.findViewById(R.id.button_open_directory)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                    }
                });

        mCurrentDirectoryTextView = (TextView) rootView
                .findViewById(R.id.textview_current_directory);
        mCreateDirectoryButton = (Button) rootView.findViewById(R.id.button_create_directory);
        mCreateDirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editView = new EditText(getActivity());
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.create_directory)
                        .setView(editView)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        createDirectory(mCurrentDirectoryUri,
                                                editView.getText().toString());
                                        updateDirectoryEntries(mCurrentDirectoryUri);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                        .show();
            }
        });
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_directory_entries);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(0);
        mAdapter = new DirectoryEntryAdapter(new ArrayList<DirectoryEntry>());
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, String.format("Open Directory result Uri : %s", data.getData()));
            updateDirectoryEntries(data.getData());
            mAdapter.notifyDataSetChanged();
        }
    }


    /**
     * Updates the current directory of the uri passed as an argument and its children directories.
     * And updates the {@link #mRecyclerView} depending on the contents of the children.
     *
     * @param uri The uri of the current directory.
     */
    //VisibileForTesting
    void updateDirectoryEntries(Uri uri) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));

        Cursor docCursor = contentResolver.query(docUri, new String[]{
                Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE}, null, null, null);
        try {
            while (docCursor.moveToNext()) {
                Log.d(TAG, "found doc =" + docCursor.getString(0) + ", mime=" + docCursor
                        .getString(1));
                mCurrentDirectoryUri = uri;
                mCurrentDirectoryTextView.setText(docCursor.getString(0));
                mCreateDirectoryButton.setEnabled(true);
            }
        } finally {
            closeQuietly(docCursor);
        }

        Cursor childCursor = contentResolver.query(childrenUri, new String[]{
                Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE}, null, null, null);
        try {
            List<DirectoryEntry> directoryEntries = new ArrayList<>();
            while (childCursor.moveToNext()) {
                Log.d(TAG, "found child=" + childCursor.getString(0) + ", mime=" + childCursor
                        .getString(1));
                DirectoryEntry entry = new DirectoryEntry();
                entry.fileName = childCursor.getString(0);
                entry.mimeType = childCursor.getString(1);
                directoryEntries.add(entry);
            }
            mAdapter.setDirectoryEntries(directoryEntries);
            mAdapter.notifyDataSetChanged();
        } finally {
            closeQuietly(childCursor);
        }
    }

    /**
     * Creates a directory under the directory represented as the uri in the argument.
     *
     * @param uri The uri of the directory under which a new directory is created.
     * @param directoryName The directory name of a new directory.
     */
    //VisibileForTesting
    void createDirectory(Uri uri, String directoryName) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri));
        Uri directoryUri = DocumentsContract
                .createDocument(contentResolver, docUri, Document.MIME_TYPE_DIR, directoryName);
        if (directoryUri != null) {
            Log.i(TAG, String.format(
                    "Created directory : %s, Document Uri : %s, Created directory Uri : %s",
                    directoryName, docUri, directoryUri));
            Toast.makeText(getActivity(), String.format("Created a directory [%s]",
                    directoryName), Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, String.format("Failed to create a directory : %s, Uri %s", directoryName,
                    docUri));
            Toast.makeText(getActivity(), String.format("Failed to created a directory [%s] : ",
                    directoryName), Toast.LENGTH_SHORT).show();
        }

    }

    public void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}

