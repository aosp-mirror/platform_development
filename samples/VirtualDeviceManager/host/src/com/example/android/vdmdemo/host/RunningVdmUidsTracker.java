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

    private final PackageManager packageManager;
    private final AudioStreamer audioStreamer;

    private final Object lock = new Object();

    @GuardedBy("lock")
    private HashMap<Integer, HashSet<Integer>> displayIdsToRunningUids = new HashMap<>();

    @GuardedBy("lock")
    private ImmutableSet<Integer> runningVdmUids = ImmutableSet.of();

    public RunningVdmUidsTracker(@NonNull Context context, @NonNull AudioStreamer audioStreamer) {
        packageManager = Objects.requireNonNull(context).getPackageManager();
        this.audioStreamer = Objects.requireNonNull(audioStreamer);
    }

    @Override
    public void onTopActivityChanged(int displayId, @NonNull ComponentName componentName) {

        Optional<Integer> topActivityUid = getUidForComponent(componentName);
        if (topActivityUid.isEmpty()) {
            Log.w(TAG, "Cannot determine UID for top activity component " + componentName);
            return;
        }

        ImmutableSet<Integer> updatedUids;
        synchronized (lock) {
            HashSet<Integer> displayUidSet = displayIdsToRunningUids.get(displayId);
            if (displayUidSet == null) {
                displayUidSet = new HashSet<>();
                displayIdsToRunningUids.put(displayId, displayUidSet);
            }
            displayUidSet.add(topActivityUid.get());
            runningVdmUids =
                    displayIdsToRunningUids.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableSet());
            updatedUids = runningVdmUids;
        }

        audioStreamer.updateVdmUids(updatedUids);
    }

    @Override
    public void onDisplayEmpty(int displayId) {
        ImmutableSet<Integer> uidsBefore;
        ImmutableSet<Integer> uidsAfter;
        synchronized (lock) {
            uidsBefore = runningVdmUids;
            displayIdsToRunningUids.remove(displayId);
            runningVdmUids =
                    displayIdsToRunningUids.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableSet());
            uidsAfter = runningVdmUids;
        }

        if (!uidsAfter.equals(uidsBefore)) {
            audioStreamer.updateVdmUids(uidsAfter);
        }
    }

    private Optional<Integer> getUidForComponent(@NonNull ComponentName topActivity) {
        try {
            return Optional.of(
                    packageManager.getPackageUid(topActivity.getPackageName(), /* flags= */ 0));
        } catch (NameNotFoundException e) {
            return Optional.empty();
        }
    }

    private static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
        return Collectors.collectingAndThen(Collectors.toList(), ImmutableSet::copyOf);
    }
}
