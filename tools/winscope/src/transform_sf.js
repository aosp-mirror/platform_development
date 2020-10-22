/*
 * Copyright 2017, The Android Open Source Project
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

// eslint-disable-next-line camelcase
import {transform, nanos_to_string, get_visible_chip} from './transform.js';
// eslint-disable-next-line camelcase
import {fill_occlusion_state, fill_inherited_state} from './sf_visibility.js';
import {getSimplifiedLayerName} from './utils/names';

const RELATIVE_Z_CHIP = {
  short: 'RelZ',
  long: 'Is relative Z-ordered to another surface',
  class: 'warn',
};
const RELATIVE_Z_PARENT_CHIP = {
  short: 'RelZParent',
  long: 'Something is relative Z-ordered to this surface',
  class: 'warn',
};
const MISSING_LAYER = {
  short: 'MissingLayer',
  long:
    'This layer was referenced from the parent, but not present in the trace',
  class: 'error',
};
const GPU_CHIP = {
  short: 'GPU',
  long: 'This layer was composed on the GPU',
  class: 'gpu',
};
const HWC_CHIP = {
  short: 'HWC',
  long: 'This layer was composed by Hardware Composer',
  class: 'hwc',
};

function transformLayer(layer) {
  function offsetTo(bounds, x, y) {
    return {
      right: bounds.right - (bounds.left - x),
      bottom: bounds.bottom - (bounds.top - y),
      left: x,
      top: y,
    };
  }

  function getRect(layer) {
    let result = layer.bounds;
    const tx = layer.position ? layer.position.x || 0 : 0;
    const ty = layer.position ? layer.position.y || 0 : 0;
    result = offsetTo(result, 0, 0);
    result.label = layer.name;
    result.transform = layer.transform;
    result.transform.tx = tx;
    result.transform.ty = ty;
    return result;
  }

  function addHwcCompositionTypeChip(layer) {
    if (layer.hwcCompositionType === 'CLIENT') {
      chips.push(GPU_CHIP);
    } else if (layer.hwcCompositionType === 'DEVICE' ||
        layer.hwcCompositionType === 'SOLID_COLOR') {
      chips.push(HWC_CHIP);
    }
  }

  const chips = [];
  if (layer.visible) {
    chips.push(get_visible_chip());
  }
  if ((layer.zOrderRelativeOf || -1) !== -1) {
    chips.push(RELATIVE_Z_CHIP);
  }
  if (layer.zOrderRelativeParentOf !== undefined) {
    chips.push(RELATIVE_Z_PARENT_CHIP);
  }
  if (layer.missing) {
    chips.push(MISSING_LAYER);
  }
  addHwcCompositionTypeChip(layer);

  const rect = layer.visible && layer.bounds !== null ?
      getRect(layer) : undefined;

  const simplifiedLayerName = getSimplifiedLayerName(layer.name);
  const shortName = simplifiedLayerName ?
      layer.id + ': ' + simplifiedLayerName : undefined;

  const transformedLayer = transform({
    obj: layer,
    kind: '',
    name: layer.id + ': ' + layer.name,
    shortName,
    children: [[layer.resolvedChildren, transformLayer]],
    rect,
    undefined /* bounds */,
    highlight: rect,
    chips,
    visible: layer.visible,
    freeze: false,
  });

  // NOTE: Temporary until refactored to use flickerlib
  transformedLayer.invisibleDueTo = layer.invisibleDueTo;
  transformedLayer.occludedBy = layer.occludedBy;
  transformedLayer.partiallyOccludedBy = layer.partiallyOccludedBy;
  transformedLayer.coveredBy = layer.coveredBy;

  return Object.freeze(transformedLayer);
}

function missingLayer(childId) {
  return {
    name: 'layer #' + childId,
    missing: true,
    zOrderRelativeOf: -1,
    transform: {dsdx: 1, dtdx: 0, dsdy: 0, dtdy: 1},
  };
}

function transformLayers(includesCompositionState, layers) {
  const idToItem = {};
  const isChild = {};

  const layersList = layers.layers || [];

  layersList.forEach((e) => {
    idToItem[e.id] = e;
  });
  layersList.forEach((e) => {
    e.resolvedChildren = [];
    if (Array.isArray(e.children)) {
      e.resolvedChildren = e.children.map(
          (childId) => idToItem[childId] || missingLayer(childId));
      e.children.forEach((childId) => {
        isChild[childId] = true;
      });
    }
    // We don't clean up relatives when the relative parent is removed, so it
    // may be inconsistent
    if ((e.zOrderRelativeOf || -1) !== -1 && (idToItem[e.zOrderRelativeOf])) {
      idToItem[e.zOrderRelativeOf].zOrderRelativeParentOf = e.id;
    }
  });

  const roots = layersList.filter((e) => !isChild[e.id]);
  fill_inherited_state(idToItem, roots);

  // Backwards compatibility check
  const occlusionDetectionCompatible = roots[0].bounds !== null;
  if (occlusionDetectionCompatible) {
    fill_occlusion_state(idToItem, roots, includesCompositionState);
  }
  function foreachTree(nodes, fun) {
    nodes.forEach((n) => {
      fun(n);
      foreachTree(n.children, fun);
    });
  }

  const idToTransformed = {};
  const transformedRoots = roots.map((r) =>
    transformLayer(r, {
      parentBounds: {left: 0, right: 0, top: 0, bottom: 0},
      parentHidden: null,
    }));

  foreachTree(transformedRoots, (n) => {
    idToTransformed[n.obj.id] = n;
  });
  const flattened = [];
  layersList.forEach((e) => {
    flattened.push(idToTransformed[e.id]);
  });

  return transform({
    obj: {},
    kind: 'layers',
    name: 'layers',
    children: [
      [transformedRoots, (c) => c],
    ],
    rectsTransform(r) {
      const res = [];
      flattened.forEach((l) => {
        if (l.rect) {
          res.push(l.rect);
        }
      });
      return res.reverse();
    },
    flattened,
  });
}

function transformLayersEntry(entry) {
  const includesCompositionState = !entry.excludesCompositionState;
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + ' - ' + entry.where,
    children: [
      [
        [entry.layers],
        (layer) => transformLayers(includesCompositionState, layer),
      ],
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
  });
}

function transformLayersTrace(entries) {
  const r = transform({
    obj: entries,
    kind: 'layerstrace',
    name: 'layerstrace',
    children: [
      [entries.entry, transformLayersEntry],
    ],
  });

  return r;
}

export {transformLayers, transformLayersTrace};
