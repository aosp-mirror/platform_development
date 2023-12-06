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

    @GuardedBy("displayRepository")
    private final List<RemoteDisplay> displayRepository = new ArrayList<>();

    @Inject
    DisplayRepository() {}

    void addDisplay(RemoteDisplay display) {
        synchronized (displayRepository) {
            displayRepository.add(display);
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
        synchronized (displayRepository) {
            displayRepository.forEach(RemoteDisplay::close);
            displayRepository.clear();
        }
    }

    int[] getRemoteDisplayIds() {
        synchronized (displayRepository) {
            return displayRepository.stream().mapToInt(RemoteDisplay::getRemoteDisplayId).toArray();
        }
    }

    int getRemoteDisplayId(int displayId) {
        return getDisplay(displayId)
                .map(RemoteDisplay::getRemoteDisplayId)
                .orElse(Display.INVALID_DISPLAY);
    }

    Optional<RemoteDisplay> getDisplayByIndex(int index) {
        synchronized (displayRepository) {
            if (index < 0 || index >= displayRepository.size()) {
                return Optional.empty();
            }
            return Optional.of(displayRepository.get(index));
        }
    }

    private Optional<RemoteDisplay> getDisplay(int displayId) {
        synchronized (displayRepository) {
            return displayRepository.stream()
                    .filter(display -> display.getDisplayId() == displayId)
                    .findFirst();
        }
    }

    private Optional<RemoteDisplay> getDisplayByRemoteId(int remoteDisplayId) {
        synchronized (displayRepository) {
            return displayRepository.stream()
                    .filter(display -> display.getRemoteDisplayId() == remoteDisplayId)
                    .findFirst();
        }
    }

    private void closeDisplay(RemoteDisplay display) {
        synchronized (displayRepository) {
            displayRepository.remove(display);
        }
        display.close();
    }
}
