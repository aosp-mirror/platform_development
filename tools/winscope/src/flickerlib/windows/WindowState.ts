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

import { getWMPropertiesForDisplay, shortenName } from '../mixin'
import { asRawTreeViewObject } from '../../utils/diff.js'
import { toRect, WindowState } from "../common"
import { VISIBLE_CHIP } from '../treeview/Chips'
import WindowContainer from "./WindowContainer"

 WindowState.fromProto = function (proto, isActivityInTree: Boolean): WindowState {
    if (proto == null) {
        return null
    } else {
        const identifierName = proto.windowContainer.identifier?.title ?? proto.identifier?.title ?? ""
        var windowType = 0
        if (identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX)) {
            windowType = WindowState.WINDOW_TYPE_STARTING
        } else if (proto.animatingExit) {
            windowType = WindowState.WINDOW_TYPE_EXITING
        } else if (identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX)) {
            windowType = WindowState.WINDOW_TYPE_STARTING
        }

        var nameOverride = identifierName

        if (identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX)) {
            nameOverride = identifierName.substring(WindowState.STARTING_WINDOW_PREFIX.length)
        } else if (identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX)) {
            nameOverride = identifierName.substring(WindowState.DEBUGGER_WINDOW_PREFIX.length)
        }

        const children = proto.windowContainer.children.reverse()
            .mapNotNull(it => WindowContainer.childrenFromProto(it, isActivityInTree))

        const windowContainer = WindowContainer.fromProto({
            proto: proto.windowContainer,
            children: children,
            nameOverride: nameOverride,
            identifierOverride: proto.identifier})
        if (windowContainer == null) {
            throw "Window container should not be null: " + JSON.stringify(proto)
        }

        const entry = new WindowState(
            proto.attributes?.type ?? 0,
            proto.displayId,
            proto.stackId,
            proto.animator?.surface?.layer ?? 0,
            proto.animator?.surface?.shown ?? false,
            windowType,
            toRect(proto.windowFrames?.frame ?? null),
            toRect(proto.windowFrames?.containingFrame ?? null),
            toRect(proto.windowFrames?.parentFrame ?? null),
            toRect(proto.windowFrames?.contentFrame ?? null),
            toRect(proto.windowFrames?.contentInsets ?? null),
            toRect(proto.surfaceInsets),
            toRect(proto.givenContentInsets),
            toRect(proto.animator?.lastClipRect ?? null),
            windowContainer,
            /* isAppWindow */ isActivityInTree
        )

        entry.rects.map((rect) => rect.ref = entry)
        entry.obj = getWMPropertiesForDisplay(proto)
        entry.shortName = shortenName(entry.name)
        entry.visible = entry.isSurfaceShown ?? false
        entry.chips = entry.isSurfaceShown ? [VISIBLE_CHIP] : []
        entry.children = entry.childrenWindows
        if (entry.isSurfaceShown) {
            entry.rect = entry.rects[0]
        }
        entry.rawTreeViewObject = asRawTreeViewObject(entry)
        return entry
    }
}

export default WindowState
