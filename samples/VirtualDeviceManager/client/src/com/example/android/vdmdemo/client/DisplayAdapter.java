/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.client;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.android.vdmdemo.client.DisplayAdapter.DisplayHolder;
import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class DisplayAdapter extends RecyclerView.Adapter<DisplayHolder> {
    private static final String TAG = "VdmClient";

    private static final AtomicInteger sNextDisplayIndex = new AtomicInteger(1);

    // Simple list of all active displays.
    private final List<RemoteDisplay> mDisplayRepository =
            Collections.synchronizedList(new ArrayList<>());

    private final RemoteIo mRemoteIo;
    private final ClientView mRecyclerView;
    private final InputManager mInputManager;

    DisplayAdapter(ClientView recyclerView, RemoteIo remoteIo, InputManager inputManager) {
        mRecyclerView = recyclerView;
        mRemoteIo = remoteIo;
        mInputManager = inputManager;
        setHasStableIds(true);
    }

    void addDisplay(boolean homeSupported) {
        Log.i(TAG, "Adding display " + sNextDisplayIndex);
        mDisplayRepository.add(
                new RemoteDisplay(sNextDisplayIndex.getAndIncrement(), homeSupported));
        notifyItemInserted(mDisplayRepository.size() - 1);
    }

    void removeDisplay(int displayId) {
        Log.i(TAG, "Removing display " + displayId);
        for (int i = 0; i < mDisplayRepository.size(); ++i) {
            if (displayId == mDisplayRepository.get(i).getDisplayId()) {
                mDisplayRepository.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    void rotateDisplay(RemoteEvent event) {
        DisplayHolder holder = getDisplayHolder(event.getDisplayId());
        if (holder != null) {
            holder.rotateDisplay(
                    event.getDisplayRotation().getRotationDegrees(), /* resize= */ false);
        }
    }

    void processDisplayChange(RemoteEvent event) {
        DisplayHolder holder = getDisplayHolder(event.getDisplayId());
        if (holder != null) {
            holder.setDisplayTitle(event.getDisplayChangeEvent().getTitle());
        }
    }

    void clearDisplays() {
        Log.i(TAG, "Clearing all displays");
        int size = mDisplayRepository.size();
        mDisplayRepository.clear();
        notifyItemRangeRemoved(0, size);
    }

    private DisplayHolder getDisplayHolder(int displayId) {
        for (int i = 0; i < mDisplayRepository.size(); ++i) {
            if (displayId == mDisplayRepository.get(i).getDisplayId()) {
                return (DisplayHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            }
        }
        return null;
    }

    @NonNull
    @Override
    public DisplayHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Disable recycling so layout changes are not present in new displays.
        mRecyclerView.getRecycledViewPool().setMaxRecycledViews(viewType, 0);
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.display_fragment, parent, false);
        return new DisplayHolder(view);
    }

    @Override
    public void onBindViewHolder(DisplayHolder holder, int position) {
        holder.onBind(position);
    }

    @Override
    public void onViewRecycled(DisplayHolder holder) {
        holder.close();
    }

    @Override
    public long getItemId(int position) {
        return mDisplayRepository.get(position).getDisplayId();
    }

    @Override
    public int getItemCount() {
        return mDisplayRepository.size();
    }

    public class DisplayHolder extends ViewHolder {
        private DisplayController mDisplayController = null;
        private InputManager.FocusListener mFocusListener = null;
        private Surface mSurface = null;
        private TextureView mTextureView = null;
        private FrameLayout mTextureFrame = null;
        private TextView mDisplayTitle = null;
        private View mRotateButton = null;
        private int mDisplayId = 0;

        DisplayHolder(View view) {
            super(view);
        }

        void rotateDisplay(int rotationDegrees, boolean resize) {
            Log.i(TAG, "Rotating display " + mDisplayId + " to " + rotationDegrees);
            mRotateButton.setEnabled(rotationDegrees == 0 || resize);

            // Make sure the rotation is visible.
            ViewGroup.LayoutParams frameLayoutParams = mTextureFrame.getLayoutParams();
            frameLayoutParams.width = Math.max(mTextureView.getWidth(), mTextureView.getHeight());
            frameLayoutParams.height = frameLayoutParams.width;
            mTextureFrame.setLayoutParams(frameLayoutParams);

            mTextureView
                    .animate()
                    .rotation(rotationDegrees)
                    .setDuration(420)
                    .withEndAction(
                            () -> {
                                if (resize) {
                                    resizeDisplay(
                                            new Rect(
                                                    0,
                                                    0,
                                                    mTextureView.getHeight(),
                                                    mTextureView.getWidth()));
                                } else {
                                    frameLayoutParams.width =
                                            (rotationDegrees % 180 == 0)
                                                    ? mTextureView.getWidth()
                                                    : mTextureView.getHeight();
                                    frameLayoutParams.height =
                                            (rotationDegrees % 180 == 0)
                                                    ? mTextureView.getHeight()
                                                    : mTextureView.getWidth();
                                    mTextureFrame.setLayoutParams(frameLayoutParams);
                                }
                            })
                    .start();
        }

        private void resizeDisplay(Rect newBounds) {
            Log.i(TAG, "Resizing display " + mDisplayId + " to " + newBounds);
            mDisplayController.setSurface(mSurface, newBounds.width(), newBounds.height());

            ViewGroup.LayoutParams layoutParams = mTextureView.getLayoutParams();
            layoutParams.width = newBounds.width();
            layoutParams.height = newBounds.height();
            mTextureView.setLayoutParams(layoutParams);

            ViewGroup.LayoutParams frameLayoutParams = mTextureFrame.getLayoutParams();
            frameLayoutParams.width = newBounds.width();
            frameLayoutParams.height = newBounds.height();
            mTextureFrame.setLayoutParams(frameLayoutParams);
        }

        private void setDisplayTitle(String title) {
            mDisplayTitle.setText(
                    itemView.getContext().getString(R.string.display_title, mDisplayId, title));
        }

        void close() {
            if (mDisplayController != null) {
                Log.i(TAG, "Closing DisplayHolder for display " + mDisplayId);
                mInputManager.removeFocusListener(mFocusListener);
                mInputManager.removeFocusableDisplay(mDisplayId);
                mDisplayController.close();
                mDisplayController = null;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        void onBind(int position) {
            RemoteDisplay remoteDisplay = mDisplayRepository.get(position);
            mDisplayId = remoteDisplay.getDisplayId();
            Log.v(TAG, "Binding DisplayHolder for display " + mDisplayId + " to position "
                    + position);

            mDisplayTitle = itemView.findViewById(R.id.display_title);
            mTextureView = itemView.findViewById(R.id.remote_display_view);
            mTextureFrame = itemView.findViewById(R.id.frame);

            mFocusListener =
                    focusedDisplayId -> {
                        View displayFocusIndicator = itemView.findViewById(R.id.display_focus);
                        if (focusedDisplayId == mDisplayId) {
                            displayFocusIndicator.setBackgroundResource(R.drawable.focus_frame);
                        } else {
                            displayFocusIndicator.setBackground(null);
                        }
                    };
            mInputManager.addFocusListener(mFocusListener);

            mDisplayController = new DisplayController(mDisplayId, mRemoteIo);
            Log.v(TAG, "Creating new DisplayController for display " + mDisplayId);

            setDisplayTitle("");

            View closeButton = itemView.findViewById(R.id.display_close);
            closeButton.setOnClickListener(
                    v -> ((DisplayAdapter) getBindingAdapter()).removeDisplay(mDisplayId));

            View backButton = itemView.findViewById(R.id.display_back);
            backButton.setOnClickListener(v -> mInputManager.sendBack(mDisplayId));

            View homeButton = itemView.findViewById(R.id.display_home);
            if (remoteDisplay.isHomeSupported()) {
                homeButton.setVisibility(View.VISIBLE);
                homeButton.setOnClickListener(v -> mInputManager.sendHome(mDisplayId));
            } else {
                homeButton.setVisibility(View.GONE);
            }

            mRotateButton = itemView.findViewById(R.id.display_rotate);
            mRotateButton.setOnClickListener(
                    v -> {
                        // This rotation is simply resizing the display with width with height
                        // swapped.
                        mDisplayController.setSurface(
                                mSurface,
                                /* width= */ mTextureView.getHeight(),
                                /* height= */ mTextureView.getWidth());
                        rotateDisplay(
                                mTextureView.getWidth() > mTextureView.getHeight() ? 90 : -90,
                                true);
                    });

            View resizeButton = itemView.findViewById(R.id.display_resize);
            resizeButton.setOnTouchListener(
                    (v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            mRecyclerView.startResizing(
                                    mTextureView, event, DisplayHolder.this::resizeDisplay);
                            return true;
                        }
                        return false;
                    });

            mTextureView.setOnTouchListener(
                    (v, event) -> {
                        if (event.getDevice().supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                            mTextureView.getParent().requestDisallowInterceptTouchEvent(true);
                            mInputManager.sendInputEvent(
                                    InputDeviceType.DEVICE_TYPE_TOUCHSCREEN, event, mDisplayId);
                        }
                        return true;
                    });
            mTextureView.setSurfaceTextureListener(
                    new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {}

                        @Override
                        public void onSurfaceTextureAvailable(
                                @NonNull SurfaceTexture texture, int width, int height) {
                            Log.v(TAG, "Setting surface for display " + mDisplayId);
                            mInputManager.addFocusableDisplay(mDisplayId);
                            mSurface = new Surface(texture);
                            mDisplayController.setSurface(mSurface, width, height);
                        }

                        @Override
                        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
                            Log.v(TAG, "onSurfaceTextureDestroyed for display " + mDisplayId);
                            if (mDisplayController != null) {
                                mDisplayController.pause();
                            }
                            return true;
                        }

                        @Override
                        public void onSurfaceTextureSizeChanged(
                                @NonNull SurfaceTexture texture, int width, int height) {
                            Log.v(TAG, "onSurfaceTextureSizeChanged for display " + mDisplayId);
                            mTextureView.setRotation(0);
                            mRotateButton.setEnabled(true);
                        }
                    });
            mTextureView.setOnGenericMotionListener(
                    (v, event) -> {
                        if (event.getDevice() == null
                                || !event.getDevice().supportsSource(InputDevice.SOURCE_MOUSE)) {
                            return false;
                        }
                        mInputManager.sendInputEvent(
                                InputDeviceType.DEVICE_TYPE_MOUSE, event, mDisplayId);
                        return true;
                    });
        }
    }

    private static class RemoteDisplay {
        // Local ID, not corresponding to the displayId of the relevant Display on the host device.
        private final int mDisplayId;
        private final boolean mHomeSupported;

        RemoteDisplay(int displayId, boolean homeSupported) {
            mDisplayId = displayId;
            mHomeSupported = homeSupported;
        }

        int getDisplayId() {
            return mDisplayId;
        }

        boolean isHomeSupported() {
            return mHomeSupported;
        }
    }
}
