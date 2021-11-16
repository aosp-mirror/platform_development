/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
    KeyguardControllerState,
    RootWindowContainer,
    WindowManagerPolicy,
    WindowManagerState
} from "./common"

import WindowContainer from "./windows/WindowContainer"

WindowManagerState.fromProto = function (proto: any, timestamp: number = 0, where: string = ""): WindowManagerState {
    var inputMethodWIndowAppToken = "";
    if (proto.inputMethodWindow != null) {
        proto.inputMethodWindow.hashCode.toString(16)
    };

    const rootWindowContainer = createRootWindowContainer(proto.rootWindowContainer);
    const keyguardControllerState = createKeyguardControllerState(
        proto.rootWindowContainer.keyguardController);
    const policy = createWindowManagerPolicy(proto.policy);

    const entry = new WindowManagerState(
        where,
        policy,
        proto.focusedApp,
        proto.focusedDisplayId,
        proto.focusedWindow?.title ?? "",
        inputMethodWIndowAppToken,
        proto.rootWindowContainer.isHomeRecentsComponent,
        proto.displayFrozen,
        proto.rootWindowContainer.pendingActivities.map(it => it.title),
        rootWindowContainer,
        keyguardControllerState,
        /*timestamp */ `${timestamp}`
    );

    addAttributes(entry, proto);
    return entry
}

function addAttributes(entry: WindowManagerState, proto: any) {
    entry.kind = entry.constructor.name;
    // There no JVM/JS translation for Longs yet
    entry.timestampMs = entry.timestamp.toString();
    entry.rects = entry.windowStates.reverse().map(it => it.rect);
    if (!entry.isComplete()) {
        entry.isIncompleteReason = entry.getIsIncompleteReason();
    }
    entry.proto = proto;
    entry.shortName = entry.name;
    entry.chips = [];
    entry.isVisible = true;
}

function createWindowManagerPolicy(proto: any): WindowManagerPolicy {
    return new WindowManagerPolicy(
        proto.focusedAppToken ?? "",
        proto.forceStatusBar,
        proto.forceStatusBarFromKeyguard,
        proto.keyguardDrawComplete,
        proto.keyguardOccluded,
        proto.keyguardOccludedChanged,
        proto.keyguardOccludedPending,
        proto.lastSystemUiFlags,
        proto.orientation,
        proto.rotation,
        proto.rotationMode,
        proto.screenOnFully,
        proto.windowManagerDrawComplete
    );
}

function createRootWindowContainer(proto: any): RootWindowContainer {
    const windowContainer = WindowContainer.fromProto(
        /* proto */ proto.windowContainer,
        /* childrenProto */ proto.windowContainer?.children?.reverse() ?? [],
        /* isActivityInTree */ false
    );

    if (windowContainer == null) {
        throw new Error(`Window container should not be null.\n${JSON.stringify(proto)}`);
    }
    const entry = new RootWindowContainer(windowContainer);
    return entry;
}

function createKeyguardControllerState(proto: any): KeyguardControllerState {
    const keyguardOccludedStates = {};

    if (proto) {
        proto.keyguardOccludedStates.forEach(it =>
            keyguardOccludedStates[it.displayId] = it.keyguardOccluded);
    }

    return new KeyguardControllerState(
        proto?.isAodShowing ?? false,
        proto?.isKeyguardShowing ?? false,
        keyguardOccludedStates
    );
}

export default WindowManagerState;
