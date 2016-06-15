package com.example.android.scopeddirectoryaccess;

import android.provider.DocumentsContract;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide views to RecyclerView with the directory entries.
 */
public class DirectoryEntryAdapter extends RecyclerView.Adapter<DirectoryEntryAdapter.ViewHolder> {

    private List<DirectoryEntry> mDirectoryEntries;

    public DirectoryEntryAdapter() {
        this(new ArrayList<DirectoryEntry>());
    }

    public DirectoryEntryAdapter(List<DirectoryEntry> directoryEntries) {
        mDirectoryEntries = directoryEntries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.directory_entry, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.fileName.setText(mDirectoryEntries.get(position).fileName);
        viewHolder.mimeType.setText(mDirectoryEntries.get(position).mimeType);

        if (DocumentsContract.Document.MIME_TYPE_DIR
                .equals(mDirectoryEntries.get(position).mimeType)) {
            viewHolder.imageView.setImageResource(R.drawable.ic_directory_grey600_36dp);
        } else {
            viewHolder.imageView.setImageResource(R.drawable.ic_description_grey600_36dp);
        }
    }

    @Override
    public int getItemCount() {
        return mDirectoryEntries.size();
    }

    public void setDirectoryEntries(List<DirectoryEntry> directoryEntries) {
        mDirectoryEntries = directoryEntries;
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView fileName;
        public TextView mimeType;
        public ImageView imageView;

        public ViewHolder(View v) {
            super(v);
            fileName = (TextView) v.findViewById(R.id.textview_filename);
            mimeType = (TextView) v.findViewById(R.id.textview_mimetype);
            imageView = (ImageView) v.findViewById(R.id.imageview_entry);
        }
    }
}

