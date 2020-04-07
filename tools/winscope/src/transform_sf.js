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

import {transform, nanos_to_string, get_visible_chip} from './transform.js'
import { fill_occlusion_state, fill_inherited_state } from './sf_visibility.js';

var RELATIVE_Z_CHIP = {short: 'RelZ',
    long: "Is relative Z-ordered to another surface",
    class: 'warn'};
var RELATIVE_Z_PARENT_CHIP = {short: 'RelZParent',
    long: "Something is relative Z-ordered to this surface",
    class: 'warn'};
var MISSING_LAYER = {short: 'MissingLayer',
    long: "This layer was referenced from the parent, but not present in the trace",
    class: 'error'};
var GPU_CHIP = {short: 'GPU',
    long: "This layer was composed on the GPU",
    class: 'gpu'};
var HWC_CHIP = {short: 'HWC',
    long: "This layer was composed by Hardware Composer",
    class: 'hwc'};

function transform_layer(layer) {
  function offset_to(bounds, x, y) {
    return {
      right: bounds.right - (bounds.left - x),
      bottom: bounds.bottom - (bounds.top - y),
      left: x,
      top: y,
    };
  }

  function get_rect(layer) {
    var result = layer.bounds;
    var tx = layer.position ? layer.position.x || 0 : 0;
    var ty = layer.position ? layer.position.y || 0 : 0;
    result = offset_to(result, 0, 0);
    result.label = layer.name;
    result.transform = layer.transform;
    result.transform.tx = tx;
    result.transform.ty = ty;
    return result;
  }

  function add_hwc_composition_type_chip(layer) {
      if (layer.hwcCompositionType === "CLIENT") {
          chips.push(GPU_CHIP);
      } else if (layer.hwcCompositionType === "DEVICE" || layer.hwcCompositionType === "SOLID_COLOR") {
          chips.push(HWC_CHIP);
      }
  }

  var chips = [];
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
  add_hwc_composition_type_chip(layer);
  const rect = layer.visible ? get_rect(layer) : undefined;

  return transform({
    obj: layer,
    kind: '',
    name: layer.id + ": " + layer.name,
    children: [[layer.resolvedChildren, transform_layer]],
    rect,
    undefined /* bounds */,
    highlight: rect,
    chips,
    visible: layer.visible,
  });
}
 
function missingLayer(childId) {
  return {
    name: "layer #" + childId,
    missing: true,
    zOrderRelativeOf: -1,
    transform: {dsdx:1, dtdx:0, dsdy:0, dtdy:1},
  }
}

function transform_layers(includesCompositionState, layers) {
  var idToItem = {};
  var isChild = {}

  var layersList = layers.layers || [];

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
    if ((e.zOrderRelativeOf || -1) !== -1) {
      idToItem[e.zOrderRelativeOf].zOrderRelativeParentOf = e.id;
    }
  });

  var roots = layersList.filter((e) => !isChild[e.id]);
  fill_inherited_state(idToItem, roots);
  fill_occlusion_state(idToItem, roots, includesCompositionState);
  function foreachTree(nodes, fun) {
    nodes.forEach((n) => {
      fun(n);
      foreachTree(n.children, fun);
    });
  }

  var idToTransformed = {};
  var transformed_roots = roots.map((r) =>
    transform_layer(r, {parentBounds: {left: 0, right: 0, top: 0, bottom: 0},
      parentHidden: null}));

  foreachTree(transformed_roots, (n) => {
    idToTransformed[n.obj.id] = n;
  });
  var flattened = [];
  layersList.forEach((e) => {
    flattened.push(idToTransformed[e.id]);
  });

  return transform({
    obj: {},
    kind: 'layers',
    name: 'layers',
    children: [
      [transformed_roots, (c) => c],
    ],
    rects_transform (r) {
      var res = [];
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

function transform_layers_entry(entry) {
  const includesCompositionState = !entry.excludesCompositionState;
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.layers], (layer) => transform_layers(includesCompositionState, layer)],
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
  });
}

function transform_layers_trace(entries) {
  var r = transform({
    obj: entries,
    kind: 'layerstrace',
    name: 'layerstrace',
    children: [
      [entries.entry, transform_layers_entry],
    ],
  });

  return r;
}

export {transform_layers, transform_layers_trace};
