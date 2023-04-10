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

import {DisplayContent, DisplayCutout, Rect, Rotation, toInsets, toRect} from '../common';
import {shortenName} from '../mixin';
import {WindowContainer} from './WindowContainer';

DisplayContent.fromProto = (
  proto: any,
  isActivityInTree: boolean,
  nextSeq: () => number
): DisplayContent => {
  if (proto == null) {
    return null;
  } else {
    const windowContainer = WindowContainer.fromProto(
      /* proto */ proto.rootDisplayArea.windowContainer,
      /* protoChildren */ proto.rootDisplayArea.windowContainer?.children ?? [],
      /* isActivityInTree */ isActivityInTree,
      /* computedZ */ nextSeq,
      /* nameOverride */ proto.displayInfo?.name ?? null
    );
    const displayRectWidth = proto.displayInfo?.logicalWidth ?? 0;
    const displayRectHeight = proto.displayInfo?.logicalHeight ?? 0;
    const appRectWidth = proto.displayInfo?.appWidth ?? 0;
    const appRectHeight = proto.displayInfo?.appHeight ?? 0;
    const defaultBounds = proto.pinnedStackController?.defaultBounds ?? null;
    const movementBounds = proto.pinnedStackController?.movementBounds ?? null;

    const entry = new DisplayContent(
      proto.id,
      proto.focusedRootTaskId,
      proto.resumedActivity?.title ?? '',
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
      proto.appTransition?.lastUsedAppTransition ?? '',
      proto.appTransition?.appTransitionState ?? '',
      Rotation.Companion.getByValue(proto.displayRotation?.rotation ?? 0),
      proto.displayRotation?.lastOrientation ?? 0,
      createDisplayCutout(proto.displayInfo?.cutout),
      windowContainer
    );

    addAttributes(entry, proto);
    return entry;
  }
};

function createDisplayCutout(proto: any | null): DisplayCutout | null {
  if (proto == null) {
    return null;
  } else {
    return new DisplayCutout(
      toInsets(proto?.insets),
      toRect(proto?.boundLeft),
      toRect(proto?.boundTop),
      toRect(proto?.boundRight),
      toRect(proto?.boundBottom),
      toInsets(proto?.waterfallInsets)
    );
  }
}

function addAttributes(entry: DisplayContent, proto: any) {
  entry.proto = proto;
  entry.kind = entry.constructor.name;
  entry.shortName = shortenName(entry.name);
}

export {DisplayContent};
