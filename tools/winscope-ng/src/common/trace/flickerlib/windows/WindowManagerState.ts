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

import { TimeUtils } from "common/utils/time_utils";
import {
    KeyguardControllerState,
    RootWindowContainer,
    WindowManagerPolicy,
    WindowManagerState,
    WindowManagerTraceEntryBuilder
} from "../common"

import WindowContainer from "./WindowContainer"

WindowManagerState.fromProto = function (
    proto: any,
    elapsedTimestamp: bigint = 0n,
    where: string = "",
    realToElapsedTimeOffsetNs: bigint|undefined = undefined,
    useElapsedTime = false,
): WindowManagerState {
    var inputMethodWIndowAppToken = "";
    if (proto.inputMethodWindow != null) {
        proto.inputMethodWindow.hashCode.toString(16)
    };

    let parseOrder = 0;
    const nextSeq = () => ++parseOrder;
    const rootWindowContainer = createRootWindowContainer(proto.rootWindowContainer, nextSeq);
    const keyguardControllerState = createKeyguardControllerState(
        proto.rootWindowContainer.keyguardController);
    const policy = createWindowManagerPolicy(proto.policy);

    const entry = new WindowManagerTraceEntryBuilder(
        `${elapsedTimestamp}`,
        policy,
        proto.focusedApp,
        proto.focusedDisplayId,
        proto.focusedWindow?.title ?? "",
        inputMethodWIndowAppToken,
        proto.rootWindowContainer.isHomeRecentsComponent,
        proto.displayFrozen,
        proto.rootWindowContainer.pendingActivities.map((it: any) => it.title),
        rootWindowContainer,
        keyguardControllerState,
        where,
        `${realToElapsedTimeOffsetNs ?? 0}`,
    ).build();

    addAttributes(entry, proto, realToElapsedTimeOffsetNs === undefined || useElapsedTime);
    return entry
}

function addAttributes(entry: WindowManagerState, proto: any, useElapsedTime = false) {
    entry.kind = entry.constructor.name;
    if (!entry.isComplete()) {
        entry.isIncompleteReason = entry.getIsIncompleteReason();
    }
    entry.proto = proto;
    if (useElapsedTime || entry.clockTimestamp == undefined) {
        entry.name = TimeUtils.nanosecondsToHumanElapsed(BigInt(entry.elapsedTimestamp));
        entry.shortName = entry.name;
    } else {
        entry.name = TimeUtils.nanosecondsToHumanReal(BigInt(entry.clockTimestamp));
        entry.shortName = entry.name;
    }
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

function createRootWindowContainer(proto: any, nextSeq: () => number): RootWindowContainer {
    const windowContainer = WindowContainer.fromProto(
        /* proto */ proto.windowContainer,
        /* childrenProto */ proto.windowContainer?.children ?? [],
        /* isActivityInTree */ false,
        /* computedZ */ nextSeq
    );

    if (windowContainer == null) {
        throw new Error(`Window container should not be null.\n${JSON.stringify(proto)}`);
    }
    const entry = new RootWindowContainer(windowContainer);
    return entry;
}

function createKeyguardControllerState(proto: any): KeyguardControllerState {
    const keyguardOccludedStates: any = {};

    if (proto) {
        proto.keyguardOccludedStates.forEach((it: any) =>
            keyguardOccludedStates[<keyof typeof keyguardOccludedStates>it.displayId] = it.keyguardOccluded);
    }

    return new KeyguardControllerState(
        proto?.isAodShowing ?? false,
        proto?.isKeyguardShowing ?? false,
        keyguardOccludedStates
    );
}

export {WindowManagerState};
