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

import {TRACE_TYPES} from '@/decode';
import {WINDOW_MANAGER_KIND} from '@/flickerlib/common';
import {getFilter} from '@/utils/utils';

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

  const imeTraceFiles = Object.entries(filesAsDict)
      .filter(
          ([filetype]) => (
            // mapping it to an array of files; removes
            // the key which is filetype
            filetype.includes('ImeTrace')))
      .map(([k, v]) => v);
  for (const imeTraceFile of imeTraceFiles) {
    if (filesAsDict[TRACE_TYPES.WINDOW_MANAGER]) {
      console.log('combining WM file to', imeTraceFile.type, 'file');
      combineWmSfPropertiesIntoImeData(
          imeTraceFile, filesAsDict[TRACE_TYPES.WINDOW_MANAGER]);
    }
    if (filesAsDict[TRACE_TYPES.SURFACE_FLINGER] &&
        imeTraceFile.type !== TRACE_TYPES.IME_MANAGERSERVICE) {
      console.log('combining SF file to', imeTraceFile.type, 'file');
      // don't need SF properties for ime manager service
      combineWmSfPropertiesIntoImeData(
          imeTraceFile, filesAsDict[TRACE_TYPES.SURFACE_FLINGER]);
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
    const wmStateOrSfLayer = wmOrSfData[wmOrSfIntersectIndex];
    // filter wmStateOrSfLayer to only relevant nodes & fields
    if (wmStateOrSfLayer && wmStateOrSfLayer.kind === WINDOW_MANAGER_KIND) {
      const wmProperties = filterWmStateForIme(wmStateOrSfLayer);
      imeTraceFile.data[i].wmProperties = wmProperties;
      imeTraceFile.data[0].hasWmSfProperties = true;
      // hasWmSfProperties is added into data because the
      // imeTraceFile object itself is inextensible if it's from file input
    } else if (wmStateOrSfLayer) {
      const sfImeContainerProperties =
          extractImeContainerFields(wmStateOrSfLayer);
      imeTraceFile.data[i].sfImeContainerProperties = Object.assign(
          {'name': wmStateOrSfLayer.name}, sfImeContainerProperties);
      // 'name' is the ..d..h..m..s..ms timestamp

      const sfSubtrees = filterSfLayerForIme(
          wmStateOrSfLayer); // for display in Hierarchy sub-panel
      imeTraceFile.data[i].children.push(...sfSubtrees);

      if (sfImeContainerProperties || sfSubtrees.length > 0) {
        imeTraceFile.data[0].hasWmSfProperties = true;
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
    'name': wmState.name, // this is the ..d..h..m..s..ms timestamp
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
  const parentTaskOfImeContainer =
    findParentTaskOfNode(sfLayer, 'ImeContainer');
  const parentTaskOfImeSnapshot = findParentTaskOfNode(sfLayer, 'IME-snapshot');
  // we want to see both ImeContainer and IME-snapshot if there are
  // cases where both exist
  const resultSubtree = [parentTaskOfImeContainer, parentTaskOfImeSnapshot]
      .filter((node) => node) // filter away null values
      .map((node) => {
        node.kind = 'SF subtree - ' + node.id;
        return node;
      });
  return resultSubtree;
}

function findParentTaskOfNode(curr, nodeName) {
  const isImeContainer = getFilter(nodeName);
  if (isImeContainer(curr)) {
    console.log('found ' + nodeName + '; searching for parent');
    let parent = curr.parent;
    const isTask = getFilter('Task, ImePlaceholder');
    while (parent.parent && !isTask(parent)) {
      // if parent.parent is null, 'parent' is already the root node -- use it
      if (parent.parent != null) {
        parent = parent.parent;
      }
    }
    return parent;
  }
  // search for ImeContainer in children
  for (const child of curr.children) {
    const result = findParentTaskOfNode(child, nodeName);
    if (result != null) {
      return result;
    }
  }
  return null;
}

function extractImeContainerFields(curr) {
  const isImeContainer = getFilter('ImeContainer');
  if (isImeContainer(curr)) {
    return {
      'screenBounds': curr.screenBounds,
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

export {combineWmSfWithImeDataIfExisting};
