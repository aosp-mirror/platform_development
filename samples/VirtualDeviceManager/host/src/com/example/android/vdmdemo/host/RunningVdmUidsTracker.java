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

package com.example.android.vdmdemo.host;

import android.companion.virtual.VirtualDeviceManager.ActivityListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class RunningVdmUidsTracker implements ActivityListener {
    private static final String TAG = RunningVdmUidsTracker.class.getSimpleName();

    private final PackageManager mPackageManager;
    private final AudioStreamer mAudioStreamer;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final HashMap<Integer, HashSet<Integer>> mDisplayIdToRunningUids = new HashMap<>();

    @GuardedBy("mLock")
    private ImmutableSet<Integer> mRunningVdmUids = ImmutableSet.of();

    public RunningVdmUidsTracker(@NonNull Context context, @NonNull AudioStreamer audioStreamer) {
        mPackageManager = Objects.requireNonNull(context).getPackageManager();
        mAudioStreamer = Objects.requireNonNull(audioStreamer);
    }

    @Override
    public void onTopActivityChanged(int displayId, @NonNull ComponentName componentName) {

        Optional<Integer> topActivityUid = getUidForComponent(componentName);
        if (topActivityUid.isEmpty()) {
            Log.w(TAG, "Cannot determine UID for top activity component " + componentName);
            return;
        }

        ImmutableSet<Integer> updatedUids;
        synchronized (mLock) {
            HashSet<Integer> displayUidSet =
                    mDisplayIdToRunningUids.computeIfAbsent(displayId, k -> new HashSet<>());
            displayUidSet.add(topActivityUid.get());
            mRunningVdmUids =
                    mDisplayIdToRunningUids.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableSet());
            updatedUids = mRunningVdmUids;
        }

        mAudioStreamer.updateVdmUids(updatedUids);
    }

    @Override
    public void onDisplayEmpty(int displayId) {
        ImmutableSet<Integer> uidsBefore;
        ImmutableSet<Integer> uidsAfter;
        synchronized (mLock) {
            uidsBefore = mRunningVdmUids;
            mDisplayIdToRunningUids.remove(displayId);
            mRunningVdmUids =
                    mDisplayIdToRunningUids.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableSet());
            uidsAfter = mRunningVdmUids;
        }

        if (!uidsAfter.equals(uidsBefore)) {
            mAudioStreamer.updateVdmUids(uidsAfter);
        }
    }

    private Optional<Integer> getUidForComponent(@NonNull ComponentName topActivity) {
        try {
            return Optional.of(
                    mPackageManager.getPackageUid(topActivity.getPackageName(), /* flags= */ 0));
        } catch (NameNotFoundException e) {
            return Optional.empty();
        }
    }

    private static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
        return Collectors.collectingAndThen(Collectors.toList(), ImmutableSet::copyOf);
    }
}
