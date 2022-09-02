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


import { Layer, Rect, toActiveBuffer, toColor, toRect, toRectF, toRegion } from "../common"
import { shortenName } from '../mixin'
import { RELATIVE_Z_CHIP, GPU_CHIP, HWC_CHIP } from '../treeview/Chips'
import Transform from './Transform'

Layer.fromProto = function (proto: any): Layer {
    const visibleRegion = toRegion(proto.visibleRegion)
    const activeBuffer = toActiveBuffer(proto.activeBuffer)
    const bounds = toRectF(proto.bounds)
    const color = toColor(proto.color)
    const screenBounds = toRectF(proto.screenBounds)
    const sourceBounds = toRectF(proto.sourceBounds)
    const transform = Transform.fromProto(proto.transform, proto.position)
    const bufferTransform = Transform.fromProto(proto.bufferTransform, /* position */ null)
    const hwcCrop = toRectF(proto.hwcCrop)
    const hwcFrame = toRect(proto.hwcFrame)
    let crop: Rect
    if (proto.crop) {
        crop = toRect(proto.crop)
    };

    const entry = new Layer(
        proto.name ?? ``,
        proto.id,
        proto.parent,
        proto.z,
        visibleRegion,
        activeBuffer,
        proto.flags,
        bounds,
        color,
        proto.isOpaque,
        proto.shadowRadius,
        proto.cornerRadius,
        proto.type ?? ``,
        screenBounds,
        transform,
        sourceBounds,
        proto.currFrame,
        proto.effectiveScalingMode,
        bufferTransform,
        proto.hwcCompositionType,
        hwcCrop,
        hwcFrame,
        proto.backgroundBlurRadius,
        crop,
        proto.isRelativeOf,
        proto.zOrderRelativeOf,
        proto.layerStack
    );

    addAttributes(entry, proto);
    return entry
}

function addAttributes(entry: Layer, proto: any) {
    entry.kind = `${entry.id}`;
    entry.shortName = shortenName(entry.name);
    entry.proto = proto;
    entry.rect = entry.bounds;
    entry.rect.transform = entry.transform;
    entry.rect.ref = entry;
    entry.rect.label = entry.name;
    entry.chips = [];
    updateChips(entry);
}

function updateChips(entry) {
    if ((entry.zOrderRelativeOf || -1) !== -1) {
        entry.chips.push(RELATIVE_Z_CHIP);
    }
    if (entry.hwcCompositionType === 'CLIENT') {
        entry.chips.push(GPU_CHIP);
    } else if (entry.hwcCompositionType === 'DEVICE' || entry.hwcCompositionType === 'SOLID_COLOR') {
        entry.chips.push(HWC_CHIP);
    }
}

export default Layer;
