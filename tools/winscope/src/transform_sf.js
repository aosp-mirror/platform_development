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

const FLAG_HIDDEN = 0x1;
var RELATIVE_Z_CHIP = {short: 'RelZ',
    long: "Is relative Z-ordered to another surface",
    class: 'warn'};
var RELATIVE_Z_PARENT_CHIP = {short: 'RelZParent',
    long: "Something is relative Z-ordered to this surface",
    class: 'warn'};
var MISSING_LAYER = {short: 'MissingLayer',
    long: "This layer was referenced from the parent, but not present in the trace",
    class: 'error'};

function transform_layer(layer, {parentHidden}) {
  function transform_rect(layer) {
    var pos = layer.position || {};
    var size = layer.size || {};

    return {
        left: pos.x || 0,
        right: pos.x + size.w || 0,
        top: pos.y || 0,
        bottom: pos.y + size.h || 0,
        label: layer.name,
    }
  }

  var chips = [];
  var rect = transform_rect(layer);
  var hidden = (layer.flags & FLAG_HIDDEN) != 0 || parentHidden;
  var visible = (layer.activeBuffer || layer.type === 'ColorLayer')
      && !hidden && layer.color.a > 0;
  if (visible) {
    chips.push(get_visible_chip());
  } else {
    rect = undefined;
  }
  var bounds = undefined;
  if (layer.name.startsWith("Display Root#0")) {
    bounds = {width: layer.size.w, height: layer.size.h};
  }
  if (layer.zOrderRelativeOf !== -1) {
    chips.push(RELATIVE_Z_CHIP);
  }
  if (layer.zOrderRelativeParentOf !== undefined) {
    chips.push(RELATIVE_Z_PARENT_CHIP);
  }
  if (layer.missing) {
    chips.push(MISSING_LAYER);
  }

  var transform_layer_with_parent_hidden =
      (layer) => transform_layer(layer, {parentHidden: hidden});

  return transform({
    obj: layer,
    kind: 'layer',
    name: layer.name,
    children: [
      [layer.resolvedChildren, transform_layer_with_parent_hidden],
    ],
    rect,
    bounds,
    highlight: rect,
    chips,
    visible,
  });
}

function missingLayer(childId) {
  return {
    name: "layer #" + childId,
    missing: true,
    zOrderRelativeOf: -1,
  }
}

function transform_layers(layers) {
  var idToItem = {};
  var isChild = {}
  layers.layers.forEach((e) => {
    idToItem[e.id] = e;
  });
  layers.layers.forEach((e) => {
    e.resolvedChildren = [];
    if (Array.isArray(e.children)) {
      e.resolvedChildren = e.children.map(
          (childId) => idToItem[childId] || missingLayer(childId));
      e.children.forEach((childId) => {
        isChild[childId] = true;
      });
    }
    if (e.zOrderRelativeOf !== -1) {
      idToItem[e.zOrderRelativeOf].zOrderRelativeParentOf = e.id;
    }
  });
  var roots = layers.layers.filter((e) => !isChild[e.id]);

  function foreachTree(nodes, fun) {
    nodes.forEach((n) => {
      fun(n);
      foreachTree(n.children, fun);
    });
  }

  var idToTransformed = {};
  var transformed_roots = roots.map(transform_layer);
  foreachTree(transformed_roots, (n) => {
    idToTransformed[n.obj.id] = n;
  });
  var flattened = [];
  layers.layers.forEach((e) => {
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
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.layers], transform_layers],
    ],
    timestamp: entry.elapsedRealtimeNanos,
  });
}

function transform_layers_trace(entries) {
  return transform({
    obj: entries,
    kind: 'layerstrace',
    name: 'layerstrace',
    children: [
      [entries.entry, transform_layers_entry],
    ],
  });
}

export {transform_layers, transform_layers_trace};
