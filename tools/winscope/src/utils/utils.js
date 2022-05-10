/*
 * Copyright 2020, The Android Open Source Project
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

// Find the index of the last element matching the predicate in a sorted array
function findLastMatchingSorted(array, predicate) {
  let a = 0;
  let b = array.length - 1;
  while (b - a > 1) {
    const m = Math.floor((a + b) / 2);
    if (predicate(array, m)) {
      a = m;
    } else {
      b = m - 1;
    }
  }

  return predicate(array, b) ? b : a;
}

// Make sure stableId is unique (makes old versions of proto compatible)
function stableIdCompatibilityFixup(item) {
  // For backwards compatibility
  // (the only item that doesn't have a unique stable ID in the tree)
  if (item.stableId === 'winToken|-|') {
    return item.stableId + item.children[0].stableId;
  }

  return item.stableId;
}

const DIRECTION = Object.freeze({
  BACKWARD: -1,
  FORWARD: 1,
});

const TimeUnits = Object.freeze({
  NANO_SECONDS: 'ns',
  MILLI_SECONDS: 'ms',
  SECONDS: 's',
  MINUTES: 'm',
  HOURS: 'h',
  DAYS: 'd',
});

function nanosToString(elapsedRealtimeNanos, precision) {
  const units = [
    [1000000, TimeUnits.NANO_SECONDS],
    [1000, TimeUnits.MILLI_SECONDS],
    [60, TimeUnits.SECONDS],
    [60, TimeUnits.MINUTES],
    [24, TimeUnits.HOURS],
    [Infinity, TimeUnits.DAYS],
  ];

  const parts = [];

  let precisionThresholdReached = false;
  units.some(([div, timeUnit], i) => {
    const part = (elapsedRealtimeNanos % div).toFixed()
    if (timeUnit === precision) {
      precisionThresholdReached = true;
    }
    if (precisionThresholdReached) {
      parts.push(part + timeUnit);
    }
    elapsedRealtimeNanos = Math.floor(elapsedRealtimeNanos / div);

    return elapsedRealtimeNanos == 0;
  });

  return parts.reverse().join('');
}

/** Checks for match in window manager properties taskId, layerId, or windowToken,
 * or surface flinger property id
 */
function isPropertyMatch(flickerItem, entryItem) {
  return flickerItem.taskId === entryItem.taskId ||
    (flickerItem.windowToken === entryItem.windowToken) ||
    ((flickerItem.layerId === entryItem.layerId) && flickerItem.layerId !== 0) ||
    flickerItem.layerId === entryItem.id;
}

export {
  DIRECTION,
  findLastMatchingSorted,
  isPropertyMatch,
  stableIdCompatibilityFixup,
  nanosToString,
  TimeUnits
}