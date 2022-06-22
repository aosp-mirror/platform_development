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
import {WINDOW_MANAGER_KIND} from '@/flickerlib/common';

function combineWmSfWithImeDataIfExisting(dataFiles) {
  // TODO(b/237744706): Add tests for this function
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
      console.log('combining WM file to', imeTraceFile.type, 'file');
      combineWmSfPropertiesIntoImeData(imeTraceFile,
          filesAsDict[TRACE_TYPES.WINDOW_MANAGER]);
    }
    if (filesAsDict[TRACE_TYPES.SURFACE_FLINGER] &&
      imeTraceFile.type !== TRACE_TYPES.IME_MANAGERSERVICE) {
      console.log('combining SF file to', imeTraceFile.type, 'file');
      // don't need SF properties for ime manager service
      combineWmSfPropertiesIntoImeData(imeTraceFile,
          filesAsDict[TRACE_TYPES.SURFACE_FLINGER]);
    }
  }
  console.log('after combining', dataFiles);
}

function combineWmSfPropertiesIntoImeData(imeTraceFile, wmOrSfTraceFile) {
  const imeTimestamps = imeTraceFile.timeline;
  const wmOrSfTimestamps = wmOrSfTraceFile.timeline;
  const intersectWmOrSfIndices =
    matchCorrespondingTimestamps(imeTimestamps, wmOrSfTimestamps);

  const wmOrSfData = wmOrSfTraceFile.data;

  console.log('number of entries:', imeTimestamps.length);
  for (let i = 0; i < imeTimestamps.length; i++) {
    const wmOrSfIntersectIndex = intersectWmOrSfIndices[i];
    let wmStateOrSfLayer = wmOrSfData[wmOrSfIntersectIndex];
    if (wmStateOrSfLayer) {
      // filter to only relevant nodes & fields
      if (wmStateOrSfLayer.kind === WINDOW_MANAGER_KIND) {
        wmStateOrSfLayer = filterWmStateForIme(wmStateOrSfLayer);
        imeTraceFile.data[i].wmProperties = wmStateOrSfLayer;
      } else {
        const sfImeContainerProperties =
          extractImeContainerFields(wmStateOrSfLayer);
        imeTraceFile.data[i].sfImeContainerProperties =
          sfImeContainerProperties;

        wmStateOrSfLayer = filterSfLayerForIme(wmStateOrSfLayer);
        // put SF entry in hierarchy view
        imeTraceFile.data[i].children.push(wmStateOrSfLayer);
      }
      if (wmStateOrSfLayer) {
        imeTraceFile.data[0].hasWmSfProperties = true;
        // Note: hasWmSfProperties is added into data because the
        // imeTraceFile object is inextensible if it's from file input
      }
    }
  }
}

function matchCorrespondingTimestamps(imeTimestamps, wmOrSfTimestamps) {
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
  console.log('done matching corresponding timestamps');
  return intersectWmOrSfIndices;
}

function filterWmStateForIme(wmState) {
  // create and return a custom entry that just contains relevant properties
  const displayContent = wmState.children[0];
  return {
    'kind': 'WM State Properties',
    'name': wmState.name,
    'shortName': wmState.shortName, // not sure what this would be yet
    'timestamp': wmState.timestamp, // not sure what this would be yet
    'stableId': wmState.stableId,
    'focusedApp': wmState.focusedApp,
    'focusedWindow': wmState.focusedWindow,
    'focusedActivity': wmState.focusedActivity,
    'inputMethodControlTarget': displayContent.proto.inputMethodControlTarget,
    'inputMethodInputTarget': displayContent.proto.inputMethodInputTarget,
    'inputMethodTarget': displayContent.proto.inputMethodTarget,
    'imeInsetsSourceProvider': displayContent.proto.imeInsetsSourceProvider,
  };
}

function filterSfLayerForIme(sfLayer) {
  const parentTaskName = findParentTaskNameOfImeContainer(sfLayer);
  let resultLayer;
  if (parentTaskName === '') {
    // there is no ImeContainer; check for ime-snapshot
    console.log('there is no ImeContainer; checking for IME-snapshot');
    const snapshotFilter = getFilter('IME-snapshot');
    resultLayer = pruneChildrenByFilter(sfLayer, snapshotFilter);
  } else {
    console.log('found parent task of ImeContainer:', parentTaskName);
    const imeParentTaskFilter = getFilter(parentTaskName);
    // prune all children that are not part of the "parent task" of ImeContainer
    resultLayer = pruneChildrenByFilter(sfLayer, imeParentTaskFilter);
  }
  resultLayer.kind = 'SurfaceFlinger Properties';
  return resultLayer;
}

function findParentTaskNameOfImeContainer(curr) {
  const isImeContainer = getFilter('ImeContainer');
  if (isImeContainer(curr)) {
    console.log('found ImeContainer; searching for parent');
    let parent = curr.parent;
    const isTask = getFilter('Task, ImePlaceholder');
    while (parent.parent && !isTask(parent)) {
      // if parent.parent is null, 'parent' is already the root node -- use it
      if (parent.parent != null) {
        parent = parent.parent;
      }
    }
    return parent.name;
  }
  // search for ImeContainer in children
  for (const child of curr.children) {
    const result = findParentTaskNameOfImeContainer(child);
    if (result !== '') {
      return result;
    }
  }
  return '';
}

function extractImeContainerFields(curr) {
  const isImeContainer = getFilter('ImeContainer');
  if (isImeContainer(curr)) {
    return {
      'bounds': curr.bounds,
      'rect': curr.rect,
      'zOrderRelativeOfId': curr.zOrderRelativeOfId,
      'z': curr.z,
    };
  }
  // search for ImeContainer in children
  for (const child of curr.children) {
    const result = extractImeContainerFields(child);
    if (result) {
      return result;
    }
  }
  return null;
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
