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

function transform_window(entry) {
  var chips = [];
  var renderIdentifier = (id) => shortenComponentName(id.title) + "@" + id.hashCode;
  var visible = entry.windowContainer.visible;
  function transform_rect(rect, label) {
    var r = rect || {};
    return {
        left: r.left || 0,
        right: r.right || 0,
        top: r.top || 0,
        bottom: r.bottom || 0,
        label,
    }
  }
  var name = renderIdentifier(entry.identifier)
  var rect = transform_rect((entry.windowFrames || entry).frame, name);

  if (visible) {
    chips.push(get_visible_chip());
  } else {
    rect = undefined;
  }

  return transform({
    obj: entry,
    kind: 'window',
    name,
    children: [
      [entry.childWindows, transform_window]
    ],
    rect,
    highlight: rect,
    chips: chips,
    visible: visible,
  });
}

function transform_app_window_token(entry) {
  return transform({
    obj: entry,
    kind: 'appWinToken',
    name: entry.name,
    children: [
      [entry.windowToken.windows, transform_window],
    ],
  });
}

function transform_task(entry) {
  return transform({
    obj: entry,
    kind: 'task',
    name: entry.id || 0,
    children: [
      [entry.appWindowTokens, transform_app_window_token],
    ],
  });
}

function transform_stack(entry) {
  return transform({
    obj: entry,
    kind: 'stack',
    name: entry.id || 0,
    children: [
      [entry.tasks, transform_task],
    ],
  });
}

function transform_window_token(entry) {
  return transform({
    obj: entry,
    kind: 'winToken',
    name: '',
    children: [
      [entry.windows, transform_window],
    ],
  });
}

function transform_below(entry) {
  return transform({
    obj: entry,
    kind: 'belowAppWindow',
    name: '',
    children: [
      [entry.windows, transform_window],
    ],
  });
}

function transform_above(entry) {
  return transform({
    obj: entry,
    kind: 'aboveAppWindow',
    name: '',
    children: [
      [entry.windows, transform_window],
    ],
  });
}

function transform_ime(entry) {
  return transform({
    obj: entry,
    kind: 'imeWindow',
    name: '',
    children: [
      [entry.windows, transform_window],
    ],
  });
}

function transform_display(entry) {
  var bounds = {
    width: entry.displayInfo.logicalWidth || 0,
    height: entry.displayInfo.logicalHeight || 0,
  };

  return transform({
    obj: entry,
    kind: 'display',
    name: entry.id || 0,
    children: [
      [entry.aboveAppWindows, transform_above],
      [entry.imeWindows, transform_ime],
      [entry.stacks, transform_stack],
      [entry.belowAppWindows, transform_below],
    ],
    bounds,
  });
}

function transform_policy(entry) {
  return transform({
    obj: entry,
    kind: 'policy',
    name: 'policy',
    children: [],
  });
}

function transform_window_service(entry) {
  return transform({
    obj: entry,
    kind: 'service',
    name: '',
    children: [
      [entry.rootWindowContainer.displays, transform_display],
      [[entry.policy], transform_policy],
    ],
    timestamp: entry.elapsedRealtimeNanos,
  });
}

function transform_entry(entry) {
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos),
    children: [
      [entry.windowManagerService.rootWindowContainer.displays, transform_display],
      [[entry.windowManagerService.policy], transform_policy],
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
  });
}

function transform_window_trace(entries) {
  return transform({
    obj: entries,
    kind: 'entries',
    name: 'entries',
    children: [
      [entries.entry, transform_entry],
    ],
  });
}

function shortenComponentName(name) {
  if (!name.includes('/')) {
    return name
  }
  var split = name.split('/');
  var pkg = split[0];
  var clazz = split.slice(1).join('/');
  if (clazz.startsWith(pkg + '.')) {
    clazz = clazz.slice(pkg.length + 1);
    return [pkg, clazz].join('/');
  }
  return name;
}

export {transform_window_service, transform_window_trace};
