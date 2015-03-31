/*
* Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.directoryselection;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Provide views to RecyclerView with the directory entries.
 */
public class DirectoryEntryAdapter extends RecyclerView.Adapter<DirectoryEntryAdapter.ViewHolder> {

    static final String DIRECTORY_MIME_TYPE = "vnd.android.document/directory";
    private List<DirectoryEntry> mDirectoryEntries;

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mFileName;
        private final TextView mMimeType;
        private final ImageView mImageView;

        public ViewHolder(View v) {
            super(v);
            mFileName = (TextView) v.findViewById(R.id.textview_filename);
            mMimeType = (TextView) v.findViewById(R.id.textview_mimetype);
            mImageView = (ImageView) v.findViewById(R.id.entry_image);
        }

        public TextView getFileName() {
            return mFileName;
        }

        public TextView getMimeType() {
            return mMimeType;
        }

        public ImageView getImageView() {
            return mImageView;
        }
    }

    /**
     * Initialize the directory entries of the Adapter.
     *
     * @param directoryEntries an array of {@link DirectoryEntry}.
     */
    public DirectoryEntryAdapter(List<DirectoryEntry> directoryEntries) {
        mDirectoryEntries = directoryEntries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.directory_item, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.getFileName().setText(mDirectoryEntries.get(position).fileName);
        viewHolder.getMimeType().setText(mDirectoryEntries.get(position).mimeType);

        if (DIRECTORY_MIME_TYPE.equals(mDirectoryEntries.get(position).mimeType)) {
            viewHolder.getImageView().setImageResource(R.drawable.ic_folder_grey600_36dp);
        } else {
            viewHolder.getImageView().setImageResource(R.drawable.ic_description_grey600_36dp);
        }
    }

    @Override
    public int getItemCount() {
        return mDirectoryEntries.size();
    }

    public void setDirectoryEntries(List<DirectoryEntry> directoryEntries) {
        mDirectoryEntries = directoryEntries;
    }
}
