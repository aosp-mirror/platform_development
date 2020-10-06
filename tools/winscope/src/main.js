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

import Vue from 'vue'
import Vuex from 'vuex'
import VueMaterial from 'vue-material'

import App from './App.vue'
import { TRACE_TYPES, DUMP_TYPES, TRACE_INFO, DUMP_INFO } from './decode.js'
import { DIRECTION, findLastMatchingSorted, stableIdCompatibilityFixup } from './utils/utils.js'

import 'style-loader!css-loader!vue-material/dist/vue-material.css'
import 'style-loader!css-loader!vue-material/dist/theme/default.css'

Vue.use(Vuex)
Vue.use(VueMaterial)

// Used to determine the order in which files or displayed
const fileOrder = {
  [TRACE_TYPES.WINDOW_MANAGER]: 1,
  [TRACE_TYPES.SURFACE_FLINGER]: 2,
  [TRACE_TYPES.TRANSACTION]: 3,
  [TRACE_TYPES.PROTO_LOG]: 4,
  [TRACE_TYPES.IME]: 5,
};

function sortFiles(files) {
  return files.sort(
    (a, b) => (fileOrder[a.type] ?? Infinity) - (fileOrder[b.type] ?? Infinity));
};

/**
 * Find the smallest timeline timestamp in a list of files
 * @return undefined if not timestamp exists in the timelines of the files
 */
function findSmallestTimestamp(files) {
  let timestamp = Infinity;
  for (const file of files) {
    if (file.timeline[0] && file.timeline[0] < timestamp) {
      timestamp = file.timeline[0];
    }
  }

  return timestamp === Infinity ? undefined : timestamp;
}

const store = new Vuex.Store({
  state: {
    currentTimestamp: 0,
    traces: {},
    dumps: {},
    excludeFromTimeline: [
      TRACE_TYPES.PROTO_LOG,
    ],
    activeFile: null,
    focusedFile: null,
    mergedTimeline: null,
    navigationFilesFilter: f => true,
    // obj -> bool, identifies whether or not an item is collapsed in a treeView
    collapsedStateStore: {},
  },
  getters: {
    collapsedStateStoreFor: (state) => (item) => {
      if (item.stableId === undefined || item.stableId === null) {
        console.error("Missing stable ID for item", item);
        throw new Error("Failed to get collapse state of item — missing a stableId");
      }

      return state.collapsedStateStore[stableIdCompatibilityFixup(item)];
    },
    files(state) {
      return Object.values(state.traces).concat(Object.values(state.dumps));
    },
    sortedFiles(state, getters) {
      return sortFiles(getters.files);
    },
    timelineFiles(state, getters) {
      return Object.values(state.traces)
        .filter(file => !state.excludeFromTimeline.includes(file.type));
    },
    sortedTimelineFiles(state, getters) {
      return sortFiles(getters.timelineFiles);
    },
    video(state) {
      return state.traces[TRACE_TYPES.SCREEN_RECORDING];
    },
  },
  mutations: {
    setCurrentTimestamp(state, timestamp) {
      state.currentTimestamp = timestamp;
    },
    setFileEntryIndex(state, { type, entryIndex }) {
      if (state.traces[type]) {
        state.traces[type].selectedIndex = entryIndex;
      } else {
        throw new Error("Unexpected type — not a trace...");
      }
    },
    setFiles(state, files) {
      const filesByType = {};
      for (const file of files) {
        if (!filesByType[file.type]) {
          filesByType[file.type] = [];
        }
        filesByType[file.type].push(file);
      }

      // TODO: Extract into smaller functions
      const traces = {};
      for (const traceType of Object.values(TRACE_TYPES)) {
        const traceFiles = {};
        const typeInfo = TRACE_INFO[traceType];

        for (const traceDataFile of typeInfo.files) {

          const files = filesByType[traceDataFile.type];

          if (!files) {
            continue;
          }

          if (traceDataFile.oneOf) {
            if (files.length > 1) {
              throw new Error(`More than one file of type ${traceDataFile.type} has been provided`);
            }

            traceFiles[traceDataFile.type] = files[0];
          } else if (traceDataFile.manyOf) {
            traceFiles[traceDataFile.type] = files;
          } else {
            throw new Error("Missing oneOf or manyOf property...");
          }
        }

        if (Object.keys(traceFiles).length > 0 && typeInfo.constructor) {
          traces[traceType] = new typeInfo.constructor(traceFiles);
        }
      }

      state.traces = traces;

      // TODO: Refactor common code out
      const dumps = {};
      for (const dumpType of Object.values(DUMP_TYPES)) {
        const dumpFiles = {};
        const typeInfo = DUMP_INFO[dumpType];

        for (const dumpDataFile of typeInfo.files) {
          const files = filesByType[dumpDataFile.type];

          if (!files) {
            continue;
          }

          if (dumpDataFile.oneOf) {
            if (files.length > 1) {
              throw new Error(`More than one file of type ${dumpDataFile.type} has been provided`);
            }

            dumpFiles[dumpDataFile.type] = files[0];
          } else if (dumpDataFile.manyOf) {

          } else {
            throw new Error("Missing oneOf or manyOf property...");
          }
        }

        if (Object.keys(dumpFiles).length > 0 && typeInfo.constructor) {
          dumps[dumpType] = new typeInfo.constructor(dumpFiles);
        }

      }

      state.dumps = dumps;

      if (!state.activeFile && Object.keys(traces).length > 0) {
        state.activeFile = sortFiles(Object.values(traces))[0];
      }

      // TODO: Add same for dumps
    },
    clearFiles(state) {
      for (const traceType in state.traces) {
        if (state.traces.hasOwnProperty(traceType)) {
          Vue.delete(state.traces, traceType);
        }
      }

      for (const dumpType in state.dumps) {
        if (state.dumps.hasOwnProperty(dumpType)) {
          Vue.delete(state.dumps, dumpType);
        }
      }

      state.activeFile = null;
      state.mergedTimeline = null;
    },
    setActiveFile(state, file) {
      state.activeFile = file;
    },
    setMergedTimeline(state, timeline) {
      state.mergedTimeline = timeline;
    },
    removeMergedTimeline(state, timeline) {
      state.mergedTimeline = null;
    },
    setMergedTimelineIndex(state, newIndex) {
      state.mergedTimeline.selectedIndex = newIndex;
    },
    setCollapsedState(state, { item, isCollapsed }) {
      if (item.stableId === undefined || item.stableId === null) {
        return;
      }

      Vue.set(
        state.collapsedStateStore,
        stableIdCompatibilityFixup(item),
        isCollapsed
      );
    },
    setFocusedFile(state, file) {
      state.focusedFile = file;
    },
    setNavigationFilesFilter(state, filter) {
      state.navigationFilesFilter = filter;
    },
  },
  actions: {
    setFiles(context, files) {
      context.commit('clearFiles');
      context.commit('setFiles', files);

      const timestamp = findSmallestTimestamp(files);
      if (timestamp !== undefined) {
        context.commit('setCurrentTimestamp', timestamp);
      }
    },
    updateTimelineTime(context, timestamp) {
      for (const file of context.getters.files) {
        const type = file.type;
        const entryIndex = findLastMatchingSorted(
          file.timeline,
          (array, idx) => parseInt(array[idx]) <= timestamp,
        );

        context.commit('setFileEntryIndex', { type, entryIndex });
      }

      if (context.state.mergedTimeline) {
        const newIndex = findLastMatchingSorted(
          context.state.mergedTimeline.timeline,
          (array, idx) => parseInt(array[idx]) <= timestamp,
        );

        context.commit('setMergedTimelineIndex', newIndex);
      }

      context.commit('setCurrentTimestamp', timestamp);
    },
    advanceTimeline(context, direction) {
      // NOTE: MergedTimeline is never considered to find the next closest index
      // MergedTimeline only represented the timelines overlapped together and
      // isn't considered an actual timeline.

      if (direction !== DIRECTION.FORWARD && direction !== DIRECTION.BACKWARD) {
        throw new Error("Unsupported direction provided.");
      }

      const consideredFiles = context.getters.timelineFiles
        .filter(context.state.navigationFilesFilter);

      let fileIndex = -1;
      let timelineIndex;
      let minTimeDiff = Infinity;

      for (let idx = 0; idx < consideredFiles.length; idx++) {
        const file = consideredFiles[idx];

        let candidateTimestampIndex = file.selectedIndex;
        let candidateTimestamp = file.timeline[candidateTimestampIndex];

        let candidateCondition;
        switch (direction) {
          case DIRECTION.BACKWARD:
            candidateCondition = () => candidateTimestamp < context.state.currentTimestamp;
            break;
          case DIRECTION.FORWARD:
            candidateCondition = () => candidateTimestamp > context.state.currentTimestamp;
            break;
        }

        if (!candidateCondition()) {
          // Not a candidate — find a valid candidate
          let noCandidate = false;
          while (!candidateCondition()) {
            candidateTimestampIndex += direction;
            if (candidateTimestampIndex < 0 || candidateTimestampIndex >= file.timeline.length) {
              noCandidate = true;
              break;
            }
            candidateTimestamp = file.timeline[candidateTimestampIndex];
          }

          if (noCandidate) {
            continue;
          }
        }

        const timeDiff = Math.abs(candidateTimestamp - context.state.currentTimestamp);
        if (minTimeDiff > timeDiff) {
          minTimeDiff = timeDiff;
          fileIndex = idx;
          timelineIndex = candidateTimestampIndex;
        }
      }

      if (fileIndex >= 0) {
        const closestFile = consideredFiles[fileIndex];
        const timestamp = parseInt(closestFile.timeline[timelineIndex]);

        context.dispatch('updateTimelineTime', timestamp);
      }
    }
  }
})

new Vue({
  el: '#app',
  store, // inject the Vuex store into all components
  render: h => h(App)
})
