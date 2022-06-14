/*
 * Copyright 2022, The Android Open Source Project
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

/**
 * @fileoverview This file contains functions used after the decoding of proto
 * trace files and before the display of trace entries in DataView panels
 * to combine WM & SF trace properties into IME trace entries.
 */

import {getFilter} from '@/utils/utils';
import {TRACE_TYPES} from '@/decode';

function combineWmSfWithImeDataIfExisting(dataFiles) {
  console.log('before combining', dataFiles);
  let filesAsDict;
  if (Array.isArray(dataFiles)) {
    // transform dataFiles to a dictionary / object with filetype as the key
    // this happens for the adb direct capture case
    filesAsDict = {};
    for (const dataFile of dataFiles) {
      const dataType = dataFile.type;
      filesAsDict[dataType] = dataFile;
    }
  } else {
    filesAsDict = dataFiles;
  }

  const imeTraceFiles = Object.entries(filesAsDict).filter(
      ([filetype]) => (
      // mapping it to an array of files; removes the key which is filetype
        filetype.includes('ImeTrace'))).map(([k, v]) => v);
  for (const imeTraceFile of imeTraceFiles) {
    if (filesAsDict[TRACE_TYPES.WINDOW_MANAGER]) {
      combineWmSfPropertiesIntoImeData(imeTraceFile,
          filesAsDict[TRACE_TYPES.WINDOW_MANAGER]);
    }
    if (filesAsDict[TRACE_TYPES.SURFACE_FLINGER]) {
      combineWmSfPropertiesIntoImeData(imeTraceFile,
          filesAsDict[TRACE_TYPES.SURFACE_FLINGER]);
    }
  }
  console.log('after combining', dataFiles);
}

function combineWmSfPropertiesIntoImeData(imeTraceFile, wmOrSfTraceFile) {
  const imeTimestamps = imeTraceFile.timeline;
  const wmOrSfData = wmOrSfTraceFile.data;
  const wmOrSfTimestamps = wmOrSfTraceFile.timeline;

  // find the latest sf / wm timestamp that comes before current ime timestamp
  let wmOrSfIndex = 0;
  const intersectWmOrSfIndices = [];
  for (let imeIndex = 0; imeIndex < imeTimestamps.length; imeIndex++) {
    const currImeTimestamp = imeTimestamps[imeIndex];

    let currWmOrSfTimestamp = wmOrSfTimestamps[wmOrSfIndex];
    while (currWmOrSfTimestamp < currImeTimestamp) {
      wmOrSfIndex++;
      currWmOrSfTimestamp = wmOrSfTimestamps[wmOrSfIndex];
    }
    intersectWmOrSfIndices.push(wmOrSfIndex - 1);
  }

  for (let i = 0; i < imeTimestamps.length; i++) {
    const wmOrSfIntersectIndex = intersectWmOrSfIndices[i];
    let wmStateOrSfLayer = wmOrSfData[wmOrSfIntersectIndex];
    if (wmStateOrSfLayer) {
      // filter to only relevant nodes & fields
      // console.log('before pruning:', wmStateOrSfLayer);
      if (wmStateOrSfLayer.kind !== 'WindowManagerState') {
        wmStateOrSfLayer = filterSfLayerForIme(wmStateOrSfLayer);
      }
      // console.log('after pruning:', wmStateOrSfLayer);
      if (wmStateOrSfLayer) {
        imeTraceFile.data[i].children.push(wmStateOrSfLayer);
      }
    }
  }
}

function filterSfLayerForIme(sfLayer) {
  const filter = getFilter('ImeContainer');
  // prune all children that don't match filter
  return pruneChildrenByFilter(sfLayer, filter);
}

function pruneChildrenByFilter(curr, filter) {
  const prunedChildren = [];
  if (filter(curr)) { // curr node passes filter; will keep all children
    return curr;
  }
  // else, filter curr's children
  for (const child of curr.children) {
    const prunedChild = pruneChildrenByFilter(child, filter);
    if (prunedChild) {
      prunedChildren.push(prunedChild);
    }
    // else undefined - child does not match the filter; discard it
  }
  if (prunedChildren.length > 0) {
    // make a copy because we can't set property 'children' of original object
    const copy = Object.assign({}, curr);
    copy.children = prunedChildren;
    return copy;
  }
  return undefined; // no children match the filter; discard curr node
}

export {combineWmSfWithImeDataIfExisting};
