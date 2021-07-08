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

import { shortenName } from '../mixin'
import { asRawTreeViewObject } from '../../utils/diff.js'
import { toRect, Size, WindowState, WindowLayoutParams } from "../common"
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
            .filter(it => it != null)
            .map(it => WindowContainer.childrenFromProto(it, isActivityInTree))

        const windowContainer = WindowContainer.fromProto({
            proto: proto.windowContainer,
            children: children,
            nameOverride: nameOverride,
            identifierOverride: proto.identifier})
        if (windowContainer == null) {
            throw "Window container should not be null: " + JSON.stringify(proto)
        }

        const entry = new WindowState(
            newWindowLayoutParams(proto.attributes),
            proto.displayId,
            proto.stackId,
            proto.animator?.surface?.layer ?? 0,
            proto.animator?.surface?.shown ?? false,
            windowType,
            new Size(proto.requestedWidth, proto.requestedHeight),
            toRect(proto.surfacePosition),
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

        entry.kind = entry.constructor.name
        entry.rect = entry.frame
        entry.rect.ref = entry
        entry.rect.label = entry.name
        entry.proto = proto
        entry.shortName = shortenName(entry.name)
        entry.chips = entry.isVisible ? [VISIBLE_CHIP] : []
        entry.rawTreeViewObject = asRawTreeViewObject(entry)
        return entry
    }
}

function newWindowLayoutParams(proto): WindowLayoutParams {
    return new WindowLayoutParams(
        /* type */ proto?.type ?? 0,
        /* x */ proto?.x ?? 0,
        /* y */ proto?.y ?? 0,
        /* width */ proto?.width ?? 0,
        /* height */ proto?.height ?? 0,
        /* horizontalMargin */ proto?.horizontalMargin ?? 0,
        /* verticalMargin */ proto?.verticalMargin ?? 0,
        /* gravity */ proto?.gravity ?? 0,
        /* softInputMode */ proto?.softInputMode ?? 0,
        /* format */ proto?.format ?? 0,
        /* windowAnimations */ proto?.windowAnimations ?? 0,
        /* alpha */ proto?.alpha ?? 0,
        /* screenBrightness */ proto?.screenBrightness ?? 0,
        /* buttonBrightness */ proto?.buttonBrightness ?? 0,
        /* rotationAnimation */ proto?.rotationAnimation ?? 0,
        /* preferredRefreshRate */ proto?.preferredRefreshRate ?? 0,
        /* preferredDisplayModeId */ proto?.preferredDisplayModeId ?? 0,
        /* hasSystemUiListeners */ proto?.hasSystemUiListeners ?? false,
        /* inputFeatureFlags */ proto?.inputFeatureFlags ?? 0,
        /* userActivityTimeout */ proto?.userActivityTimeout ?? 0,
        /* colorMode */ proto?.colorMode ?? 0,
        /* flags */ proto?.flags ?? 0,
        /* privateFlags */ proto?.privateFlags ?? 0,
        /* systemUiVisibilityFlags */ proto?.systemUiVisibilityFlags ?? 0,
        /* subtreeSystemUiVisibilityFlags */ proto?.subtreeSystemUiVisibilityFlags ?? 0,
        /* appearance */ proto?.appearance ?? 0,
        /* behavior */ proto?.behavior ?? 0,
        /* fitInsetsTypes */ proto?.fitInsetsTypes ?? 0,
        /* fitInsetsSides */ proto?.fitInsetsSides ?? 0,
        /* fitIgnoreVisibility */ proto?.fitIgnoreVisibility ?? false
    )
}

export default WindowState
