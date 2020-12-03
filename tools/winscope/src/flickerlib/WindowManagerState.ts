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
import { nanosToString, TimeUnits } from "../utils/utils.js"
import { getWMPropertiesForDisplay } from './mixin'

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

    entry.obj = getWMPropertiesForDisplay(proto)
    entry.obj["isComplete"] = entry.isComplete()
    if (!entry.obj.isComplete) {
        entry.obj["isIncompleteReason"] = entry.getIsIncompleteReason()
    }
    entry.name = nanosToString(entry.timestamp, TimeUnits.MILLI_SECONDS)
    entry.shortName = entry.name
    entry.children = entry.root.childrenWindows.reverse()
    entry.chips = []
    entry.visible = true
    entry.rawTreeViewObject = asRawTreeViewObject(entry)
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
    const windowContainer = WindowContainer.fromProto({proto: proto.windowContainer})
    if (windowContainer == null) {
        throw "Window container should not be null: " + JSON.stringify(proto)
    }
    const entry = new RootWindowContainer(windowContainer)
    proto.windowContainer.children.reverse()
        .map(it => WindowContainer.childrenFromProto(entry, it, /* isActivityInTree */ false))
        .filter(it => it != null)
        .forEach(it => windowContainer.childContainers.push(it))
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