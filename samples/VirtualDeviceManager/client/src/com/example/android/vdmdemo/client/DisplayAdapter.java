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
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.example.android.vdmdemo.client.DisplayAdapter.DisplayHolder;
import com.example.android.vdmdemo.common.RemoteEventProto.DisplayRotation;
import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class DisplayAdapter extends RecyclerView.Adapter<DisplayHolder> {
    private static final String TAG = "VdmClient";

    private static final AtomicInteger sNextDisplayIndex = new AtomicInteger(1);

    // Simple list of all active displays.
    private final List<RemoteDisplay> mDisplayRepository =
            Collections.synchronizedList(new ArrayList<>());

    private final RemoteIo mRemoteIo;
    private final ClientView mRecyclerView;
    private final InputManager mInputManager;
    private ActivityResultLauncher<Intent> mFullscreenLauncher;

    DisplayAdapter(ClientView recyclerView, RemoteIo remoteIo, InputManager inputManager) {
        mRecyclerView = recyclerView;
        mRemoteIo = remoteIo;
        mInputManager = inputManager;
        setHasStableIds(true);
    }

    void setFullscreenLauncher(ActivityResultLauncher<Intent> launcher) {
        mFullscreenLauncher = launcher;
    }

    void onFullscreenActivityResult(ActivityResult result) {
        Intent data = result.getData();
        if (data == null) {
            return;
        }
        int displayId =
                data.getIntExtra(ImmersiveActivity.EXTRA_DISPLAY_ID, Display.INVALID_DISPLAY);
        if (result.getResultCode() == ImmersiveActivity.RESULT_CLOSE) {
            removeDisplay(displayId);
        } else if (result.getResultCode() == ImmersiveActivity.RESULT_MINIMIZE) {
            int requestedRotation =
                    data.getIntExtra(ImmersiveActivity.EXTRA_REQUESTED_ROTATION, 0);
            rotateDisplay(displayId, requestedRotation);
        }
    }

    void addDisplay(boolean homeSupported, boolean rotationSupported) {
        Log.i(TAG, "Adding display " + sNextDisplayIndex);
        mDisplayRepository.add(
                new RemoteDisplay(sNextDisplayIndex.getAndIncrement(), homeSupported,
                        rotationSupported));
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

    void rotateDisplay(int displayId, int rotationDegrees) {
        DisplayHolder holder = getDisplayHolder(displayId);
        if (holder != null) {
            holder.rotateDisplay(rotationDegrees, /* resize= */ false);
        }
    }

    void processDisplayChange(RemoteEvent event) {
        DisplayHolder holder = getDisplayHolder(event.getDisplayId());
        if (holder != null) {
            holder.setDisplayTitle(event.getDisplayChangeEvent().getTitle());
        }
    }

    void clearDisplays() {
        int size = mDisplayRepository.size();
        if (size > 0) {
            Log.i(TAG, "Clearing all displays");
            mDisplayRepository.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    void pauseAllDisplays() {
        Log.i(TAG, "Pausing all displays");
        forAllDisplays(DisplayHolder::pause);
    }

    void resumeAllDisplays() {
        Log.i(TAG, "Resuming all displays");
        forAllDisplays(DisplayHolder::resume);
    }

    private void forAllDisplays(Consumer<DisplayHolder> consumer) {
        for (int i = 0; i < mDisplayRepository.size(); ++i) {
            DisplayHolder holder =
                    (DisplayHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                consumer.accept(holder);
            }
        }
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
        private TextView mDisplayTitle = null;
        private View mRotateButton = null;
        private int mDisplayId = 0;
        private RemoteDisplay mRemoteDisplay = null;

        DisplayHolder(View view) {
            super(view);
        }

        void rotateDisplay(int rotationDegrees, boolean resize) {
            if (mTextureView.getRotation() == rotationDegrees) {
                return;
            }
            Log.i(TAG, "Rotating display " + mDisplayId + " to " + rotationDegrees);
            mRotateButton.setEnabled(rotationDegrees == 0 || resize
                    || mRemoteDisplay.isRotationSupported());

            // Make sure the rotation is visible.
            View strut = itemView.requireViewById(R.id.strut);
            ViewGroup.LayoutParams layoutParams = strut.getLayoutParams();
            layoutParams.width = Math.max(mTextureView.getWidth(), mTextureView.getHeight());
            strut.setLayoutParams(layoutParams);
            final int postRotationWidth = (resize || rotationDegrees % 180 != 0)
                    ? mTextureView.getHeight() : mTextureView.getWidth();

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
                                }
                                layoutParams.width = postRotationWidth;
                                strut.setLayoutParams(layoutParams);
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

        void pause() {
            mDisplayController.pause();
        }

        void resume() {
            mDisplayController.setSurface(
                    mSurface, mTextureView.getWidth(), mTextureView.getHeight());
        }

        @SuppressLint("ClickableViewAccessibility")
        void onBind(int position) {
            mRemoteDisplay = mDisplayRepository.get(position);
            mDisplayId = mRemoteDisplay.getDisplayId();
            Log.v(TAG, "Binding DisplayHolder for display " + mDisplayId + " to position "
                    + position);

            mDisplayTitle = itemView.requireViewById(R.id.display_title);
            mTextureView = itemView.requireViewById(R.id.remote_display_view);
            final View displayHeader = itemView.requireViewById(R.id.display_header);

            mFocusListener =
                    focusedDisplayId -> {
                        if (focusedDisplayId == mDisplayId && mDisplayRepository.size() > 1) {
                            displayHeader.setBackgroundResource(R.drawable.focus_frame);
                        } else {
                            displayHeader.setBackground(null);
                        }
                    };
            mInputManager.addFocusListener(mFocusListener);

            mDisplayController = new DisplayController(mDisplayId, mRemoteIo);
            Log.v(TAG, "Creating new DisplayController for display " + mDisplayId);

            setDisplayTitle("");

            View closeButton = itemView.requireViewById(R.id.display_close);
            closeButton.setOnClickListener(
                    v -> ((DisplayAdapter) Objects.requireNonNull(getBindingAdapter()))
                            .removeDisplay(mDisplayId));

            View backButton = itemView.requireViewById(R.id.display_back);
            backButton.setOnClickListener(v -> mInputManager.sendBack(mDisplayId));

            View homeButton = itemView.requireViewById(R.id.display_home);
            if (mRemoteDisplay.isHomeSupported()) {
                homeButton.setVisibility(View.VISIBLE);
                homeButton.setOnClickListener(v -> mInputManager.sendHome(mDisplayId));
            } else {
                homeButton.setVisibility(View.GONE);
            }

            mRotateButton = itemView.requireViewById(R.id.display_rotate);
            mRotateButton.setOnClickListener(v -> {
                mInputManager.setFocusedDisplayId(mDisplayId);
                if (mRemoteDisplay.isRotationSupported()) {
                    mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                            .setDisplayId(mDisplayId)
                            .setDisplayRotation(DisplayRotation.newBuilder())
                            .build());
                } else {
                    // This rotation is simply resizing the display with width with height swapped.
                    mDisplayController.setSurface(
                            mSurface,
                            /* width= */ mTextureView.getHeight(),
                            /* height= */ mTextureView.getWidth());
                    rotateDisplay(mTextureView.getWidth() > mTextureView.getHeight() ? 90 : -90,
                            true);
                }
            });

            View resizeButton = itemView.requireViewById(R.id.display_resize);
            resizeButton.setOnTouchListener((v, event) -> {
                if (event.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                mInputManager.setFocusedDisplayId(mDisplayId);
                int maxSize = itemView.getHeight() - displayHeader.getHeight()
                        - itemView.getPaddingTop() - itemView.getPaddingBottom();
                mRecyclerView.startResizing(
                        mTextureView, event, maxSize, DisplayHolder.this::resizeDisplay);
                return true;
            });

            View fullscreenButton = itemView.requireViewById(R.id.display_fullscreen);
            fullscreenButton.setOnClickListener(v -> {
                mInputManager.setFocusedDisplayId(mDisplayId);
                Intent intent = new Intent(v.getContext(), ImmersiveActivity.class);
                intent.putExtra(ImmersiveActivity.EXTRA_DISPLAY_ID, mDisplayId);
                mFullscreenLauncher.launch(intent);
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
        private final boolean mRotationSupported;

        RemoteDisplay(int displayId, boolean homeSupported, boolean rotationSupported) {
            mDisplayId = displayId;
            mHomeSupported = homeSupported;
            mRotationSupported = rotationSupported;
        }

        int getDisplayId() {
            return mDisplayId;
        }

        boolean isHomeSupported() {
            return mHomeSupported;
        }

        boolean isRotationSupported() {
            return mRotationSupported;
        }
    }
}
