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

    private static final AtomicInteger nextDisplayIndex = new AtomicInteger(1);

    // Simple list of all active displays.
    private final List<RemoteDisplay> displayRepository =
            Collections.synchronizedList(new ArrayList<>());

    private final RemoteIo remoteIo;
    private final ClientView recyclerView;
    private final InputManager inputManager;

    DisplayAdapter(ClientView recyclerView, RemoteIo remoteIo, InputManager inputManager) {
        this.recyclerView = recyclerView;
        this.remoteIo = remoteIo;
        this.inputManager = inputManager;
        setHasStableIds(true);
    }

    void addDisplay(boolean homeSupported) {
        Log.i(TAG, "Adding display " + nextDisplayIndex);
        displayRepository.add(new RemoteDisplay(nextDisplayIndex.getAndIncrement(), homeSupported));
        notifyItemInserted(displayRepository.size() - 1);
    }

    void removeDisplay(int displayId) {
        Log.i(TAG, "Removing display " + displayId);
        for (int i = 0; i < displayRepository.size(); ++i) {
            if (displayId == displayRepository.get(i).getDisplayId()) {
                displayRepository.remove(i);
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
        int size = displayRepository.size();
        displayRepository.clear();
        notifyItemRangeRemoved(0, size);
    }

    private DisplayHolder getDisplayHolder(int displayId) {
        for (int i = 0; i < displayRepository.size(); ++i) {
            if (displayId == displayRepository.get(i).getDisplayId()) {
                return (DisplayHolder) recyclerView.findViewHolderForAdapterPosition(i);
            }
        }
        return null;
    }

    @Override
    public DisplayHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Disable recycling so layout changes are not present in new displays.
        recyclerView.getRecycledViewPool().setMaxRecycledViews(viewType, 0);
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
        return displayRepository.get(position).getDisplayId();
    }

    @Override
    public int getItemCount() {
        return displayRepository.size();
    }

    public class DisplayHolder extends ViewHolder {
        private DisplayController displayController = null;
        private InputManager.FocusListener focusListener = null;
        private Surface surface = null;
        private TextureView textureView = null;
        private FrameLayout textureFrame = null;
        private TextView displayTitle = null;
        private View rotateButton = null;
        private int displayId = 0;

        DisplayHolder(View view) {
            super(view);
        }

        void rotateDisplay(int rotationDegrees, boolean resize) {
            Log.i(TAG, "Rotating display " + displayId + " to " + rotationDegrees);
            rotateButton.setEnabled(rotationDegrees == 0 || resize);

            // Make sure the rotation is visible.
            ViewGroup.LayoutParams frameLayoutParams = textureFrame.getLayoutParams();
            frameLayoutParams.width = Math.max(textureView.getWidth(), textureView.getHeight());
            frameLayoutParams.height = frameLayoutParams.width;
            textureFrame.setLayoutParams(frameLayoutParams);

            textureView
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
                                                    textureView.getHeight(),
                                                    textureView.getWidth()));
                                } else {
                                    frameLayoutParams.width =
                                            (rotationDegrees % 180 == 0)
                                                    ? textureView.getWidth()
                                                    : textureView.getHeight();
                                    frameLayoutParams.height =
                                            (rotationDegrees % 180 == 0)
                                                    ? textureView.getHeight()
                                                    : textureView.getWidth();
                                    textureFrame.setLayoutParams(frameLayoutParams);
                                }
                            })
                    .start();
        }

        private void resizeDisplay(Rect newBounds) {
            Log.i(TAG, "Resizing display " + displayId + " to " + newBounds);
            displayController.setSurface(surface, newBounds.width(), newBounds.height());

            ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
            layoutParams.width = newBounds.width();
            layoutParams.height = newBounds.height();
            textureView.setLayoutParams(layoutParams);

            ViewGroup.LayoutParams frameLayoutParams = textureFrame.getLayoutParams();
            frameLayoutParams.width = newBounds.width();
            frameLayoutParams.height = newBounds.height();
            textureFrame.setLayoutParams(frameLayoutParams);
        }

        private void setDisplayTitle(String title) {
            displayTitle.setText(
                    itemView.getContext().getString(R.string.display_title, displayId, title));
        }

        void close() {
            if (displayController != null) {
                Log.i(TAG, "Closing DisplayHolder for display " + displayId);
                inputManager.removeFocusListener(focusListener);
                inputManager.removeFocusableDisplay(displayId);
                displayController.close();
                displayController = null;
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        void onBind(int position) {
            RemoteDisplay remoteDisplay = displayRepository.get(position);
            displayId = remoteDisplay.getDisplayId();
            Log.v(
                    TAG,
                    "Binding DisplayHolder for display " + displayId + " to position " + position);

            displayTitle = itemView.findViewById(R.id.display_title);
            textureView = itemView.findViewById(R.id.remote_display_view);
            textureFrame = itemView.findViewById(R.id.frame);

            focusListener =
                    focusedDisplayId -> {
                        View displayFocusIndicator = itemView.findViewById(R.id.display_focus);
                        if (focusedDisplayId == displayId) {
                            displayFocusIndicator.setBackgroundResource(R.drawable.focus_frame);
                        } else {
                            displayFocusIndicator.setBackground(null);
                        }
                    };
            inputManager.addFocusListener(focusListener);

            displayController = new DisplayController(displayId, remoteIo);
            Log.v(TAG, "Creating new DisplayController for display " + displayId);

            setDisplayTitle("");

            View closeButton = itemView.findViewById(R.id.display_close);
            closeButton.setOnClickListener(
                    v -> ((DisplayAdapter) getBindingAdapter()).removeDisplay(displayId));

            View backButton = itemView.findViewById(R.id.display_back);
            backButton.setOnClickListener(v -> inputManager.sendBack(displayId));

            View homeButton = itemView.findViewById(R.id.display_home);
            if (remoteDisplay.isHomeSupported()) {
                homeButton.setVisibility(View.VISIBLE);
                homeButton.setOnClickListener(v -> inputManager.sendHome(displayId));
            } else {
                homeButton.setVisibility(View.GONE);
            }

            rotateButton = itemView.findViewById(R.id.display_rotate);
            rotateButton.setOnClickListener(
                    v -> {
                        // This rotation is simply resizing the display with width with height
                        // swapped.
                        displayController.setSurface(
                                surface,
                                /* width= */ textureView.getHeight(),
                                /* height= */ textureView.getWidth());
                        rotateDisplay(
                                textureView.getWidth() > textureView.getHeight() ? 90 : -90, true);
                    });

            View resizeButton = itemView.findViewById(R.id.display_resize);
            resizeButton.setOnTouchListener(
                    (v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            recyclerView.startResizing(
                                    textureView, event, DisplayHolder.this::resizeDisplay);
                            return true;
                        }
                        return false;
                    });

            textureView.setOnTouchListener(
                    (v, event) -> {
                        if (event.getDevice().supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                            textureView.getParent().requestDisallowInterceptTouchEvent(true);
                            inputManager.sendInputEvent(
                                    InputDeviceType.DEVICE_TYPE_TOUCHSCREEN, event, displayId);
                        }
                        return true;
                    });
            textureView.setSurfaceTextureListener(
                    new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}

                        @Override
                        public void onSurfaceTextureAvailable(
                                SurfaceTexture texture, int width, int height) {
                            Log.v(TAG, "Setting surface for display " + displayId);
                            inputManager.addFocusableDisplay(displayId);
                            surface = new Surface(texture);
                            displayController.setSurface(surface, width, height);
                        }

                        @Override
                        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                            Log.v(TAG, "onSurfaceTextureDestroyed for display " + displayId);
                            if (displayController != null) {
                                displayController.pause();
                            }
                            return true;
                        }

                        @Override
                        public void onSurfaceTextureSizeChanged(
                                SurfaceTexture texture, int width, int height) {
                            Log.v(TAG, "onSurfaceTextureSizeChanged for display " + displayId);
                            textureView.setRotation(0);
                            rotateButton.setEnabled(true);
                        }
                    });
            textureView.setOnGenericMotionListener(
                    (v, event) -> {
                        if (event.getDevice() == null
                                || !event.getDevice().supportsSource(InputDevice.SOURCE_MOUSE)) {
                            return false;
                        }
                        inputManager.sendInputEventToFocusedDisplay(
                                InputDeviceType.DEVICE_TYPE_MOUSE, event);
                        return true;
                    });
        }
    }

    private static class RemoteDisplay {
        // Local ID, not corresponding to the displayId of the relevant Display on the host device.
        private final int displayId;
        private final boolean homeSupported;

        RemoteDisplay(int displayId, boolean homeSupported) {
            this.displayId = displayId;
            this.homeSupported = homeSupported;
        }

        int getDisplayId() {
            return displayId;
        }

        boolean isHomeSupported() {
            return homeSupported;
        }
    }
}
