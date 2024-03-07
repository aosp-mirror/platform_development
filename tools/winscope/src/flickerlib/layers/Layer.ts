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
  HwcCompositionType,
  Layer,
  LayerProperties,
  Rect,
  toActiveBuffer,
  toColor,
  toCropRect,
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
  const transform = Transform.fromProto(proto.transform, proto.position);
  const bufferTransform = Transform.fromProto(proto.bufferTransform, /* position */ null);
  const crop: Rect = toCropRect(proto.crop);

  const properties = new LayerProperties(
    visibleRegion,
    activeBuffer,
    /* flags */ proto.flags,
    bounds,
    color,
    /* isOpaque */ proto.isOpaque,
    /* shadowRadius */ proto.shadowRadius,
    /* cornerRadius */ proto.cornerRadius,
    screenBounds,
    transform,
    /* effectiveScalingMode */ proto.effectiveScalingMode,
    bufferTransform,
    /* hwcCompositionType */ new HwcCompositionType(proto.hwcCompositionType),
    /* backgroundBlurRadius */ proto.backgroundBlurRadius,
    crop,
    /* isRelativeOf */ proto.isRelativeOf,
    /* zOrderRelativeOfId */ proto.zOrderRelativeOf,
    /* stackId */ proto.layerStack,
    excludesCompositionState
  );

  const entry = new Layer(
    /* name */ proto.name ?? ``,
    /* id */ proto.id,
    /*parentId */ proto.parent,
    /* z */ proto.z,
    /* currFrameString */ `${proto.currFrame}`,
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
