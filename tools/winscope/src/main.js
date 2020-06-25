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
import { findLastMatchingSorted, stableIdCompatibilityFixup } from './utils/utils.js'

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

const store = new Vuex.Store({
  state: {
    currentTimestamp: 0,
    filesByType: {},
    excludeFromTimeline: [
      DATA_TYPES.PROTO_LOG,
    ],
    activeFile: null,
    mergedTimeline: null,
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
    sortedFiles(state) {
      return Object.values(state.filesByType).sort((a, b) => {
        return (fileOrder[a.type.name] ?? Infinity) - (fileOrder[b.type.name] ?? Infinity);
      });
    },
    timelineFiles(state) {
      return Object.values(state.filesByType)
        .filter(file => !state.excludeFromTimeline.includes(file.type));
    },
    video(state) {
      return state.filesByType[DATA_TYPES.SCREEN_RECORDING.name];
    },
  },
  mutations: {
    setCurrentTimestamp(state, { timestamp }) {
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
      for (const filename in state.filesByType) {
        if (state.filesByType.hasOwnProperty(filename)) {
          delete state.filesByType[filename];
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
    }
  },
  actions: {
    setFiles(context, files) {
      context.commit('clearFiles');
      context.commit('addFiles', files);
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

      context.commit('setCurrentTimestamp', { timestamp });
    },
    advanceTimeline(context, direction) {
      // NOTE: MergedTimeline is never considered to find the next closest index
      // MergedTimeline only represented the timelines overlapped together and
      // isn't considered an actual timeline.

      let closestTimeline = -1;
      let timeDiff = Infinity;
      const consideredFiles = context.getters.timelineFiles;

      for (let idx = 0; idx < consideredFiles.length; idx++) {
        const file = consideredFiles[idx];
        const cur = file.selectedIndex;
        if (cur + direction < 0 || cur + direction >= consideredFiles[idx].timeline.length) {
          continue;
        }
        var d = Math.abs(parseInt(file.timeline[cur + direction]) - context.state.currentTimestamp);
        if (timeDiff > d) {
          timeDiff = d;
          closestTimeline = idx;
        }
      }

      if (closestTimeline >= 0) {
        consideredFiles[closestTimeline].selectedIndex += direction;
        const timestamp = parseInt(
          consideredFiles[closestTimeline]
            .timeline[consideredFiles[closestTimeline].selectedIndex]);
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
