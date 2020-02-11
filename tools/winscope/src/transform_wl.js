/*
 * Copyright 2019, The Android Open Source Project
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

function transform_wl_layer(layer) {
  function is_visible(layer) {
    return layer.parent == 0 || (layer.visibleInParent && layer.visible && (layer.hidden != 1));
  }

  var chips = [];
  var rect = layer.displayFrame;
  var visible = is_visible(layer);
  if (visible && layer.parent != 0) {
    chips.push(get_visible_chip());
  }
  if (!visible) {
    rect = undefined;
  }

  return transform({
    obj: layer,
    kind: 'layer',
    name: layer.name,
    children: [
      [layer.resolvedChildren, transform_wl_layer],
    ],
    rect,
    highlight: rect,
    chips,
    visible,
    stableId: layer.id,
  });
}

function transform_wl_container(cntnr) {
  var rect = cntnr.geometry;
  var layersList = cntnr.layers || [];

  return transform({
    obj: cntnr,
    kind: 'container',
    name: cntnr.name,
    children: [
      [layersList, transform_wl_layer],
    ],
    rect,
    highlight: rect,
    stableId: cntnr.id,
  });
}

function transform_wl_outputstate(layers) {
  var containerList = layers.containers || [];
  var fullBounds = layers.fullBounds;

  return transform({
    obj: {name: "Output State", fullBounds: fullBounds},
    kind: 'outputstate',
    name: 'Output State',
    rect: fullBounds,
    highlight: fullBounds,
    children: [
      [containerList, transform_wl_container],
    ],
  });
}

function transform_wl_entry(entry) {
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.state], transform_wl_outputstate],
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
  });
}

function transform_wayland_trace(entries) {
  var r = transform({
    obj: entries,
    kind: 'wltrace',
    name: 'wltrace',
    children: [
      [entries.entry, transform_wl_entry],
    ],
  });

  return r;
}

export {transform_wl_outputstate, transform_wayland_trace};
