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

import {TimeUtils} from 'common/time_utils';
import {ElapsedTimestamp, RealTimestamp} from 'trace/timestamp';
import {
  Display,
  LayerTraceEntry,
  LayerTraceEntryBuilder,
  toRect,
  toSize,
  toTransform,
} from '../common';
import {getPropertiesForDisplay} from '../mixin';
import {Layer} from './Layer';

LayerTraceEntry.fromProto = (
  protos: object[],
  displayProtos: object[],
  elapsedTimestamp: bigint,
  vSyncId: number,
  hwcBlob: string,
  where = '',
  realToElapsedTimeOffsetNs: bigint | undefined = undefined,
  useElapsedTime = false,
  excludesCompositionState = false
): LayerTraceEntry => {
  const layers = protos.map((it) => Layer.fromProto(it, excludesCompositionState));
  const displays = (displayProtos || []).map((it) => newDisplay(it));
  const builder = new LayerTraceEntryBuilder()
    .setElapsedTimestamp(`${elapsedTimestamp}`)
    .setLayers(layers)
    .setDisplays(displays)
    .setVSyncId(`${vSyncId}`)
    .setHwcBlob(hwcBlob)
    .setWhere(where)
    .setRealToElapsedTimeOffsetNs(`${realToElapsedTimeOffsetNs ?? 0}`);
  const entry: LayerTraceEntry = builder.build();

  addAttributes(entry, protos, realToElapsedTimeOffsetNs === undefined || useElapsedTime);
  return entry;
};

function addAttributes(entry: LayerTraceEntry, protos: object[], useElapsedTime = false) {
  entry.kind = 'entry';
  // Avoid parsing the entry root because it is an array of layers
  // containing all trace information, this slows down the property tree.
  // Instead parse only key properties for debugging
  const newObj = getPropertiesForDisplay(entry);
  if (newObj.rects) delete newObj.rects;
  if (newObj.flattenedLayers) delete newObj.flattenedLayers;
  if (newObj.physicalDisplays) delete newObj.physicalDisplays;
  if (newObj.physicalDisplayBounds) delete newObj.physicalDisplayBounds;
  if (newObj.isVisible) delete newObj.isVisible;
  entry.proto = newObj;
  if (useElapsedTime || entry.clockTimestamp === undefined) {
    entry.name = TimeUtils.format(new ElapsedTimestamp(BigInt(entry.elapsedTimestamp)));
    entry.shortName = entry.name;
  } else {
    entry.name = TimeUtils.format(new RealTimestamp(entry.clockTimestamp));
    entry.shortName = entry.name;
  }
}

function newDisplay(proto: any): Display {
  return new Display(
    `${proto.id}`,
    proto.name,
    proto.layerStack,
    toSize(proto.size),
    toRect(proto.layerStackSpaceRect),
    toTransform(proto.transform),
    proto.isVirtual
  );
}

export {LayerTraceEntry};
