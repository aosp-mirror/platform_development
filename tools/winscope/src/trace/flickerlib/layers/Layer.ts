/*
 * Copyright 2021, The Android Open Source Project
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
  Layer,
  LayerProperties,
  Rect,
  toActiveBuffer,
  toColor,
  toRect,
  toRectF,
  toRegion,
} from '../common';
import {shortenName} from '../mixin';
import {Transform} from './Transform';

Layer.fromProto = (proto: any, excludesCompositionState = false): Layer => {
  const visibleRegion = toRegion(proto.visibleRegion);
  const activeBuffer = toActiveBuffer(proto.activeBuffer);
  const bounds = toRectF(proto.bounds);
  const color = toColor(proto.color);
  const screenBounds = toRectF(proto.screenBounds);
  const sourceBounds = toRectF(proto.sourceBounds);
  const transform = Transform.fromProto(proto.transform, proto.position);
  const bufferTransform = Transform.fromProto(proto.bufferTransform, /* position */ null);
  const hwcCrop = toRectF(proto.hwcCrop);
  const hwcFrame = toRect(proto.hwcFrame);
  const requestedColor = toColor(proto.requestedColor);
  const requestedTransform = Transform.fromProto(proto.requestedTransform, proto.requestedPosition);
  const cornerRadiusCrop = toRectF(proto.cornerRadiusCrop);
  const inputTransform = Transform.fromProto(
    proto.inputWindowInfo ? proto.inputWindowInfo.transform : null
  );
  const inputRegion = toRegion(
    proto.inputWindowInfo ? proto.inputWindowInfo.touchableRegion : null
  );
  let crop: Rect;
  if (proto.crop) {
    crop = toRect(proto.crop);
  }

  const properties = new LayerProperties(
    visibleRegion,
    activeBuffer,
    /* flags */ proto.flags,
    bounds,
    color,
    /* isOpaque */ proto.isOpaque,
    /* shadowRadius */ proto.shadowRadius,
    /* cornerRadius */ proto.cornerRadius,
    /* type */ proto.type ?? ``,
    screenBounds,
    transform,
    sourceBounds,
    /* effectiveScalingMode */ proto.effectiveScalingMode,
    bufferTransform,
    /* hwcCompositionType */ proto.hwcCompositionType,
    hwcCrop,
    hwcFrame,
    /* backgroundBlurRadius */ proto.backgroundBlurRadius,
    crop,
    /* isRelativeOf */ proto.isRelativeOf,
    /* zOrderRelativeOfId */ proto.zOrderRelativeOf,
    /* stackId */ proto.layerStack,
    requestedTransform,
    requestedColor,
    cornerRadiusCrop,
    inputTransform,
    inputRegion,
    excludesCompositionState
  );

  const entry = new Layer(
    /* name */ proto.name ?? ``,
    /* id */ proto.id,
    /*parentId */ proto.parent,
    /* z */ proto.z,
    /* currFrame */ proto.currFrame,
    properties
  );

  addAttributes(entry, proto);
  return entry;
};

function addAttributes(entry: Layer, proto: any) {
  entry.kind = `${entry.id}`;
  entry.shortName = shortenName(entry.name);
  entry.proto = proto;
  entry.rect = entry.bounds;
  entry.rect.transform = entry.transform;
  entry.rect.ref = entry;
  entry.rect.label = entry.name;
}

export {Layer};
