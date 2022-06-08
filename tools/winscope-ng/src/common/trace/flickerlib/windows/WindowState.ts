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
import { toRect, Size, WindowState, WindowLayoutParams } from "../common"
import { VISIBLE_CHIP } from '../treeview/Chips'
import WindowContainer from "./WindowContainer"

 WindowState.fromProto = function (proto: any, isActivityInTree: Boolean): WindowState {
    if (proto == null) {
        return null;
    } else {
        const windowParams = createWindowLayoutParams(proto.attributes);
        const identifierName = getIdentifier(proto);
        const windowType = getWindowType(proto, identifierName);
        const name = getName(identifierName);
        const windowContainer = WindowContainer.fromProto(
            /* proto */ proto.windowContainer,
            /* protoChildren */ proto.windowContainer?.children.reverse() ?? [],
            /* isActivityInTree */ isActivityInTree,
            /* nameOverride */ name,
            /* identifierOverride */ proto.identifier
        );

        const entry = new WindowState(
            windowParams,
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
        );

        addAttributes(entry, proto);
        return entry;
    }
}

function createWindowLayoutParams(proto: any): WindowLayoutParams {
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

function getWindowType(proto: any, identifierName: string): number {
    if (identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX)) {
        return WindowState.WINDOW_TYPE_STARTING;
    } else if (proto.animatingExit) {
        return WindowState.WINDOW_TYPE_EXITING;
    } else if (identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX)) {
        return WindowState.WINDOW_TYPE_STARTING;
    }

    return 0;
}

function getName(identifierName: string): string {
    var name = identifierName;

    if (identifierName.startsWith(WindowState.STARTING_WINDOW_PREFIX)) {
        name = identifierName.substring(WindowState.STARTING_WINDOW_PREFIX.length);
    } else if (identifierName.startsWith(WindowState.DEBUGGER_WINDOW_PREFIX)) {
        name = identifierName.substring(WindowState.DEBUGGER_WINDOW_PREFIX.length);
    }

    return name;
}

function getIdentifier(proto: any): string {
    return proto.windowContainer.identifier?.title ?? proto.identifier?.title ?? "";
}

function addAttributes(entry: WindowState, proto: any) {
    entry.kind = entry.constructor.name;
    entry.rect = entry.frame;
    entry.rect.ref = entry;
    entry.rect.label = entry.name;
    entry.proto = proto;
    entry.shortName = shortenName(entry.name);
    entry.chips = entry.isVisible ? [VISIBLE_CHIP] : [];
}

export default WindowState
