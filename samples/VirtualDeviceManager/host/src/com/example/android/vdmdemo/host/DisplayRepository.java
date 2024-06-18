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

import android.view.Display;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class DisplayRepository {

    @GuardedBy("mDisplayRepository")
    private final List<RemoteDisplay> mDisplayRepository = new ArrayList<>();

    @Inject
    DisplayRepository() {}

    void addDisplay(RemoteDisplay display) {
        synchronized (mDisplayRepository) {
            mDisplayRepository.add(display);
        }
    }

    void removeDisplay(int displayId) {
        getDisplay(displayId).ifPresent(this::closeDisplay);
    }

    void removeDisplayByRemoteId(int remoteDisplayId) {
        getDisplayByRemoteId(remoteDisplayId).ifPresent(this::closeDisplay);
    }

    void onDisplayChanged(int displayId) {
        getDisplay(displayId).ifPresent(RemoteDisplay::onDisplayChanged);
    }

    boolean resetDisplay(RemoteEvent event) {
        Optional<RemoteDisplay> display = getDisplayByRemoteId(event.getDisplayId());
        display.ifPresent(d -> d.reset(event.getDisplayCapabilities()));
        return display.isPresent();
    }

    void clear() {
        synchronized (mDisplayRepository) {
            mDisplayRepository.forEach(RemoteDisplay::close);
            mDisplayRepository.clear();
        }
    }

    int[] getDisplayIds() {
        synchronized (mDisplayRepository) {
            return mDisplayRepository.stream()
                    .mapToInt(RemoteDisplay::getDisplayId)
                    .toArray();
        }
    }

    int[] getRemoteDisplayIds() {
        synchronized (mDisplayRepository) {
            return mDisplayRepository.stream()
                    .mapToInt(RemoteDisplay::getRemoteDisplayId)
                    .toArray();
        }
    }

    int getRemoteDisplayId(int displayId) {
        return getDisplay(displayId)
                .map(RemoteDisplay::getRemoteDisplayId)
                .orElse(Display.INVALID_DISPLAY);
    }

    Optional<RemoteDisplay> getDisplayByIndex(int index) {
        synchronized (mDisplayRepository) {
            if (index < 0 || index >= mDisplayRepository.size()) {
                return Optional.empty();
            }
            return Optional.of(mDisplayRepository.get(index));
        }
    }

    private Optional<RemoteDisplay> getDisplay(int displayId) {
        synchronized (mDisplayRepository) {
            return mDisplayRepository.stream()
                    .filter(display -> display.getDisplayId() == displayId)
                    .findFirst();
        }
    }

    Optional<RemoteDisplay> getDisplayByRemoteId(int remoteDisplayId) {
        synchronized (mDisplayRepository) {
            return mDisplayRepository.stream()
                    .filter(display -> display.getRemoteDisplayId() == remoteDisplayId)
                    .findFirst();
        }
    }

    private void closeDisplay(RemoteDisplay display) {
        synchronized (mDisplayRepository) {
            mDisplayRepository.remove(display);
        }
        display.close();
    }
}
