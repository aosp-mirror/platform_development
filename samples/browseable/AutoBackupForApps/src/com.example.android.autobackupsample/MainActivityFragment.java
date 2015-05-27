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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "AutoBackupSample";
    public static final int ADD_FILE_REQUEST = 1;
    public static final int ADD_FILE_RESULT_SUCCESS = 101;
    public static final int ADD_FILE_RESULT_ERROR = 102;

    private ArrayAdapter<File> mFilesArrayAdapter;
    private ArrayList<File> mFiles;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_FILE_REQUEST && resultCode == ADD_FILE_RESULT_SUCCESS) {
            updateListOfFiles();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_add_file) {
            Intent addFileIntent = new Intent(getActivity(), AddFileActivity.class);
            startActivityForResult(addFileIntent, ADD_FILE_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFilesArrayAdapter == null) {
            mFiles = createListOfFiles();
            mFilesArrayAdapter = new ArrayAdapter<File>(getActivity(),
                    R.layout.file_list_item, mFiles) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View itemView = inflater.inflate(R.layout.file_list_item, parent, false);
                    TextView fileNameView = (TextView) itemView.findViewById(R.id.file_name);
                    String fileName = getItem(position).getAbsolutePath();
                    fileNameView.setText(fileName);
                    TextView fileSize = (TextView) itemView.findViewById(R.id.file_size);
                    String fileSizeInBytes = NumberFormat.getInstance()
                            .format(getItem(position).length());
                    fileSize.setText(fileSizeInBytes);
                    return itemView;
                }
            };
            updateListOfFiles();
            ListView filesListView = (ListView) getView().findViewById(R.id.file_list);
            filesListView.setAdapter(mFilesArrayAdapter);
        }
    }

    private ArrayList<File> createListOfFiles() {
        ArrayList<File> listOfFiles = new ArrayList<File>();
        addFilesToList(listOfFiles, getActivity().getFilesDir());
        if (Utils.isExternalStorageAvailable()) {
            addFilesToList(listOfFiles, getActivity().getExternalFilesDir(null));
        }
        addFilesToList(listOfFiles, getActivity().getNoBackupFilesDir());
        return listOfFiles;
    }

    private void addFilesToList(ArrayList<File> listOfFiles, File dir) {
        File[] files = dir.listFiles();
        for (File file: files) {
            listOfFiles.add(file);
        }
    }

    public void updateListOfFiles() {
        TextView emptyFileListMessage =
                (TextView) getView().findViewById(R.id.empty_file_list_message);
        mFiles = createListOfFiles();
        if (mFilesArrayAdapter.getCount() > 0) {
            mFilesArrayAdapter.clear();
        }
        for (File file: mFiles) {
            mFilesArrayAdapter.add(file);
        }
        // Display a message instructing to add files if no files found.
        if (mFiles.size() == 0) {
            emptyFileListMessage.setVisibility(View.VISIBLE);
        } else {
            emptyFileListMessage.setVisibility(View.GONE);
        }
    }
}

