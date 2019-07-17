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

// Layer flags
const FLAG_HIDDEN = 0x01;
const FLAG_OPAQUE = 0x02;
const FLAG_SECURE = 0x80;

var RELATIVE_Z_CHIP = {short: 'RelZ',
    long: "Is relative Z-ordered to another surface",
    class: 'warn'};
var RELATIVE_Z_PARENT_CHIP = {short: 'RelZParent',
    long: "Something is relative Z-ordered to this surface",
    class: 'warn'};
var MISSING_LAYER = {short: 'MissingLayer',
    long: "This layer was referenced from the parent, but not present in the trace",
    class: 'error'};

function transform_layer(layer, {parentBounds, parentHidden}) {

  function get_size(layer) {
    var size = layer.size || {w: 0, h: 0};
    return {
      left: 0,
      right: size.w,
      top: 0,
      bottom: size.h
    };
  }

  function get_crop(layer) {
    var crop = layer.crop || {left: 0, top: 0, right: 0 , bottom:0};
    return {
      left: crop.left || 0,
      right: crop.right  || 0,
      top: crop.top || 0,
      bottom: crop.bottom || 0
    };
  }

  function intersect(bounds, crop) {
    return {
      left: Math.max(crop.left, bounds.left),
      right: Math.min(crop.right, bounds.right),
      top: Math.max(crop.top, bounds.top),
      bottom: Math.min(crop.bottom, bounds.bottom),
    };
  }

  function is_empty_rect(rect) {
    var right = rect.right || 0;
    var left = rect.left || 0;
    var top = rect.top || 0;
    var bottom = rect.bottom || 0;

    return (right - left) <= 0 || (bottom - top) <= 0;
  }

  function get_cropped_bounds(layer, parentBounds) {
    var size = get_size(layer);
    var crop = get_crop(layer);
    if (!is_empty_rect(size) && !is_empty_rect(crop)) {
      return intersect(size, crop);
    }
    if (!is_empty_rect(size)) {
      return size;
    }
    if (!is_empty_rect(crop)) {
      return crop;
    }
    return parentBounds || { left: 0, right: 0, top: 0, bottom: 0 };
  }

  function offset_to(bounds, x, y) {
    return {
      right: bounds.right - (bounds.left - x),
      bottom: bounds.bottom - (bounds.top - y),
      left: x,
      top: y,
    };
  }

  function transform_bounds(layer, parentBounds) {
    var result = layer.bounds || get_cropped_bounds(layer, parentBounds);
    var tx = (layer.position) ? layer.position.x || 0 : 0;
    var ty = (layer.position) ? layer.position.y || 0 : 0;
    result = offset_to(result, 0, 0);
    result.label = layer.name;
    result.transform = layer.transform;
    result.transform.tx = tx;
    result.transform.ty = ty;
    return result;
  }

  function is_opaque(layer) {
    return layer.color == undefined || (layer.color.a || 0) > 0;
  }

  function is_empty(region) {
    return region == undefined ||
        region.rect == undefined ||
        region.rect.length == 0 ||
        region.rect.every(function(r) { return is_empty_rect(r) } );
  }

  /**
   * Checks if the layer is visible on screen according to its type,
   * active buffer content, alpha and visible regions.
   *
   * @param {layer} layer
   * @returns if the layer is visible on screen or not
   */
  function is_visible(layer) {
    var visible = (layer.activeBuffer || layer.type === 'ColorLayer')
                  && !hidden && is_opaque(layer);
    visible &= !is_empty(layer.visibleRegion);
    return visible;
  }

  function postprocess_flags(layer) {
    if (!layer.flags) return;
    var verboseFlags = [];
    if (layer.flags & FLAG_HIDDEN) {
      verboseFlags.push("HIDDEN");
    }
    if (layer.flags & FLAG_OPAQUE) {
      verboseFlags.push("OPAQUE");
    }
    if (layer.flags & FLAG_SECURE) {
      verboseFlags.push("SECURE");
    }

    layer.flags = verboseFlags.join('|') + " (" + layer.flags + ")";
  }

  var chips = [];
  var rect = transform_bounds(layer, parentBounds);
  var hidden = (layer.flags & FLAG_HIDDEN) != 0 || parentHidden;
  var visible = is_visible(layer);
  if (visible) {
    chips.push(get_visible_chip());
  } else {
    rect = undefined;
  }

  var bounds = undefined;
  if (layer.name.startsWith("Display Root#0") && layer.sourceBounds) {
    bounds = {width: layer.sourceBounds.right, height: layer.sourceBounds.bottom};
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

  var transform_layer_with_parent_hidden =
      (layer) => transform_layer(layer, {parentBounds: rect, parentHidden: hidden});

  postprocess_flags(layer);

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
    transform: {dsdx:1, dtdx:0, dsdy:0, dtdy:1},
  }
}

function transform_layers(layers) {
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

  function foreachTree(nodes, fun) {
    nodes.forEach((n) => {
      fun(n);
      foreachTree(n.children, fun);
    });
  }

  var idToTransformed = {};
  var transformed_roots = roots.map((r) =>
    transform_layer(r, {parentBounds: {left: 0, right: 0, top: 0, bottom: 0},
      parentHidden: false}));

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
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.layers], transform_layers],
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
