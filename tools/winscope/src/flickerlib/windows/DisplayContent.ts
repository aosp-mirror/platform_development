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

import { getWMPropertiesForDisplay,  shortenName } from '../mixin'
import { asRawTreeViewObject } from '../../utils/diff.js'
import { toRect, DisplayContent, Rect } from "../common"
import WindowContainer from "./WindowContainer"

DisplayContent.fromProto = function (proto, isActivityInTree: Boolean): DisplayContent {
    if (proto == null) {
        return null
    } else {
        const windowContainer = WindowContainer.fromProto({proto: proto.rootDisplayArea.windowContainer,
            nameOverride: proto.displayInfo?.name ?? null})
        if (windowContainer == null) {
            throw "Window container should not be null: " + JSON.stringify(proto)
        }

        const displayRectWidth = proto.displayInfo?.logicalWidth ?? 0
        const displayRectHeight = proto.displayInfo?.logicalHeight ?? 0
        const appRectWidth = proto.displayInfo?.appWidth ?? 0
        const appRectHeight = proto.displayInfo?.appHeight ?? 0

        const defaultBounds = proto.pinnedStackController?.defaultBounds ?? null
        const movementBounds = proto.pinnedStackController?.movementBounds ?? null

        const entry = new DisplayContent(
            proto.id,
            proto.focusedRootTaskId,
            proto.resumedActivity?.title ?? "",
            proto.singleTaskInstance,
            toRect(defaultBounds),
            toRect(movementBounds),
            new Rect(0, 0, displayRectWidth, displayRectHeight),
            new Rect(0, 0, appRectWidth, appRectHeight),
            proto.dpi,
            proto.displayInfo?.flags ?? 0,
            toRect(proto.displayFrames?.stableBounds),
            proto.surfaceSize,
            proto.focusedApp,
            proto.appTransition?.lastUsedAppTransition ?? "",
            proto.appTransition?.appTransitionState ?? "",
            proto.displayRotation?.rotation ?? 0,
            proto.displayRotation?.lastOrientation ?? 0,
            windowContainer
        )

        proto.rootDisplayArea.windowContainer.children.reverse()
            .map(it => WindowContainer.childrenFromProto(entry, it, isActivityInTree))
            .filter(it => it != null)
            .forEach(it => windowContainer.childContainers.push(it))

        entry.obj = getWMPropertiesForDisplay(proto)
        entry.shortName = shortenName(entry.name)
        entry.children = entry.childrenWindows
        entry.rawTreeViewObject = asRawTreeViewObject(entry)
        return entry
    }
}

export default DisplayContent
