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
import { DATA_TYPES } from './decode.js'
import { DIRECTION, findLastMatchingSorted, stableIdCompatibilityFixup } from './utils/utils.js'

import 'style-loader!css-loader!vue-material/dist/vue-material.css'
import 'style-loader!css-loader!vue-material/dist/theme/default.css'

Vue.use(Vuex)
Vue.use(VueMaterial)

// Used to determine the order in which files or displayed
const fileOrder = {
  [DATA_TYPES.WINDOW_MANAGER.name]: 1,
  [DATA_TYPES.SURFACE_FLINGER.name]: 2,
  [DATA_TYPES.TRANSACTION.name]: 3,
  [DATA_TYPES.PROTO_LOG.name]: 4,
};

function sortFiles(files) {
  return files.sort(
    (a, b) => (fileOrder[a.type.name] ?? Infinity) - (fileOrder[b.type.name] ?? Infinity));
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
    filesByType: {},
    excludeFromTimeline: [
      DATA_TYPES.PROTO_LOG,
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
      return Object.values(state.filesByType);
    },
    sortedFiles(state, getters) {
      return sortFiles(getters.files);
    },
    timelineFiles(state, getters) {
      return getters.files
        .filter(file => !state.excludeFromTimeline.includes(file.type));
    },
    sortedTimelineFiles(state, getters) {
      return sortFiles(getters.timelineFiles);
    },
    video(state) {
      return state.filesByType[DATA_TYPES.SCREEN_RECORDING.name];
    },
  },
  mutations: {
    setCurrentTimestamp(state, timestamp) {
      state.currentTimestamp = timestamp;
    },
    setFileEntryIndex(state, { fileTypeName, entryIndex }) {
      state.filesByType[fileTypeName].selectedIndex = entryIndex;
    },
    addFiles(state, files) {
      if (!state.activeFile && files.length > 0) {
        state.activeFile = files[0];
      }

      for (const file of files) {
        Vue.set(state.filesByType, file.type.name, file);
      }
    },
    clearFiles(state) {
      for (const fileType in state.filesByType) {
        if (state.filesByType.hasOwnProperty(fileType)) {
          Vue.delete(state.filesByType, fileType);
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
      context.commit('addFiles', files);

      const timestamp = findSmallestTimestamp(files);
      if (timestamp !== undefined) {
        context.commit('setCurrentTimestamp', timestamp);
      }
    },
    updateTimelineTime(context, timestamp) {
      for (const file of context.getters.files) {
        const fileTypeName = file.type.name;
        const entryIndex = findLastMatchingSorted(
          file.timeline,
          (array, idx) => parseInt(array[idx]) <= timestamp,
        );

        context.commit('setFileEntryIndex', { fileTypeName, entryIndex });
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
