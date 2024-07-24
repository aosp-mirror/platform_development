/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.vdmdemo.virtualcamera;

import android.companion.virtual.camera.VirtualCamera;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;


/** Array adapter for viewing list of currently registered cameras. */
public class VirtualCameraArrayAdapter extends ArrayAdapter<VirtualCamera> {
    private final LayoutInflater mLayoutInflater;

    private VirtualCameraDemoService mVirtualCameraDemoService;

    public VirtualCameraArrayAdapter(@NonNull Context context) {
        super(context, 0);

        mLayoutInflater = Objects.requireNonNull(context.getSystemService(LayoutInflater.class));
    }

    static class CameraViewHolder {
        TextView mCameraDisplayName;
        TextView mCameraId;

        Button mRemoveButton;
    }

    public void setVirtualCameraDemoService(@Nullable VirtualCameraDemoService service) {
        mVirtualCameraDemoService = service;
    }

    @Override
    public int getCount() {
        return mVirtualCameraDemoService != null ? mVirtualCameraDemoService.getCameras().size()
                : 0;
    }

    @Override
    public VirtualCamera getItem(int index) {
        return mVirtualCameraDemoService != null ? mVirtualCameraDemoService.getCameras().get(index)
                : null;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        CameraViewHolder viewHolder;
        if (row == null) {
            row = mLayoutInflater.inflate(R.layout.camera_item, parent, false);
            viewHolder = new CameraViewHolder();
            viewHolder.mCameraDisplayName = row.findViewById(R.id.camera_display_name);
            viewHolder.mCameraId = row.findViewById(R.id.camera_id);
            viewHolder.mRemoveButton = row.findViewById(R.id.remove_button);
            row.setTag(viewHolder);
        } else {
            viewHolder = (CameraViewHolder) row.getTag();
        }
        VirtualCamera camera = Objects.requireNonNull(getItem(position));
        viewHolder.mCameraDisplayName.setText(camera.getConfig().getName());
        viewHolder.mCameraId.setText(camera.getId());
        viewHolder.mRemoveButton.setOnClickListener(v -> {
            if (mVirtualCameraDemoService != null) {
                mVirtualCameraDemoService.removeCamera(camera);
            }
            notifyDataSetChanged();
        });
        return row;
    }
}
