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

import { asRawTreeViewObject } from '../utils/diff.js'
import { getPropertiesForDisplay } from './mixin'

import {
    KeyguardControllerState,
    RootWindowContainer,
    WindowManagerPolicy,
    WindowManagerState
} from "./common"

import WindowContainer from "./windows/WindowContainer"

WindowManagerState.fromProto = function ({proto, timestamp = 0, where = ""}): WindowManagerState {
    var inputMethodWIndowAppToken = ""
    if (proto.inputMethodWindow != null) {
        proto.inputMethodWindow.hashCode.toString(16)
    }
    const rootWindowContainer = newRootWindowContainer(proto.rootWindowContainer)
    const keyguardControllerState = newKeyguardControllerState(
        proto.rootWindowContainer.keyguardController)
    const entry = new WindowManagerState(
        where,
        newWindowManagerPolicy(proto.policy),
        proto.focusedApp,
        proto.focusedDisplayId,
        proto.focusedWindow?.title ?? "",
        inputMethodWIndowAppToken,
        proto.rootWindowContainer.isHomeRecentsComponent,
        proto.displayFrozen,
        proto.rootWindowContainer.pendingActivities.map(it => it.title),
        rootWindowContainer,
        keyguardControllerState,
        timestamp = timestamp
    )

    entry.kind = entry.constructor.name
    entry.rects = entry.windowStates.reverse().map(it => it.rect)
    if (!entry.isComplete()) {
        entry.isIncompleteReason = entry.getIsIncompleteReason()
    }
    entry.obj = getPropertiesForDisplay(proto, entry)
    entry.shortName = entry.name
    entry.chips = []
    entry.visible = true
    entry.rawTreeViewObject = asRawTreeViewObject(entry)

    console.warn("Created ", entry.kind, " stableId=", entry.stableId)
    return entry
}

function newWindowManagerPolicy(proto): WindowManagerPolicy {
    return new WindowManagerPolicy(
        proto.focusedAppToken || "",
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
    )
}

function newRootWindowContainer(proto): RootWindowContainer {
    const children = proto.windowContainer.children.reverse()
        .filter(it => it != null)
        .map(it => WindowContainer.childrenFromProto(it, /* isActivityInTree */ false))
    const windowContainer = WindowContainer.fromProto(
        {proto: proto.windowContainer, children: children})
    if (windowContainer == null) {
        throw "Window container should not be null: " + JSON.stringify(proto)
    }
    const entry = new RootWindowContainer(windowContainer)

    return entry
}

function newKeyguardControllerState(proto): KeyguardControllerState {
    const keyguardOccludedStates = {}

    if (proto) {
        proto.keyguardOccludedStates.forEach(it =>
            keyguardOccludedStates[it.displayId] = it.keyguardOccluded)
    }

    return new KeyguardControllerState(
        proto?.isAodShowing ?? false,
        proto?.isKeyguardShowing ?? false,
        keyguardOccludedStates
    )
}

export default WindowManagerState;