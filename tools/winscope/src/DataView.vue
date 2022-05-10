<!-- Copyright (C) 2019 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<template>
  <div @click="onClick($event)">
    <flat-card v-if="hasDataView(file)">
      <md-card-header>
        <md-card-header-text>
          <div class="md-title">
            <button class="toggle-view-button" @click="toggleView">
              <i aria-hidden="true" class="md-icon md-theme-default material-icons">
                {{ isShowFileType(file.type) ? "expand_more" : "chevron_right" }}
              </i>
            </button>
            <md-icon>{{ TRACE_ICONS[file.type] }}</md-icon>
            {{ file.type }}
          </div>
        </md-card-header-text>
        <md-button
          :href="file.blobUrl"
          :download="file.type"
          class="md-icon-button"
        >
          <md-icon>save_alt</md-icon>
        </md-button>
      </md-card-header>
      <AccessibilityTraceView
        v-if="showInAccessibilityTraceView(file) && isShowFileType(file.type)"
        :store="store"
        :file="file"
        ref="view"
      />
      <WindowManagerTraceView
        v-if="showInWindowManagerTraceView(file) && isShowFileType(file.type)"
        :store="store"
        :file="file"
        :presentTags="presentTags"
        :presentErrors="presentErrors"
        ref="view"
      />
      <SurfaceFlingerTraceView
        v-else-if="showInSurfaceFlingerTraceView(file) && isShowFileType(file.type)"
        :store="store"
        :file="file"
        :presentTags="presentTags"
        :presentErrors="presentErrors"
        ref="view"
      />
      <transactionsviewlegacy
        v-else-if="isTransactionsLegacy(file) && isShowFileType(file.type)"
        :trace="file"
        ref="view"
      />
      <logview
        v-else-if="isLog(file) && isShowFileType(file.type)"
        :file="file"
        ref="view"
      />
      <traceview
        v-else-if="showInTraceView(file) && isShowFileType(file.type)"
        :store="store"
        :file="file"
        :presentTags="[]"
        :presentErrors="[]"
        ref="view"
      />
      <div v-else>
        <h1 v-if="isShowFileType(file.type)" class="bad">Unrecognized DataType</h1>
      </div>

    </flat-card>
  </div>
</template>
<script>
import TraceView from '@/TraceView.vue';
import AccessibilityTraceView from '@/AccessibilityTraceView.vue';
import WindowManagerTraceView from '@/WindowManagerTraceView.vue';
import SurfaceFlingerTraceView from '@/SurfaceFlingerTraceView.vue';
import TransactionsViewLegacy from '@/TransactionsViewLegacy.vue';
import LogView from '@/LogView.vue';
import FileType from '@/mixins/FileType.js';
import FlatCard from '@/components/FlatCard.vue';

import {TRACE_ICONS} from '@/decode.js';

export default {
  name: 'dataview',
  data() {
    return {
      TRACE_ICONS,
    };
  },
  methods: {
    // Recursively search for an arrowUp method in the children
    // This is necessary because the VueComponent hierarchy has
    // different depths depending on the source
    depthFirstSearchArrowUp(component) {
      if (component.arrowUp) {
        component.arrowUp();
        return true;
      } else {
        for (let i = 0; i < component.$children.length; i++) {
          var child = component.$children[i];
          if (this.depthFirstSearchArrowUp(child)) {
            return true;
          }
        }
        return false;
      }
    },
    // Recursively search for an arrowUp method in the children
    // This is necessary because the VueComponent hierarchy has
    // different depths depending on the source
    depthFirstSearchArrowDown(component) {
      if (component.arrowDown) {
        component.arrowDown();
        return true
      } else {
        for (let i = 0; i < component.$children.length; i++) {
          var child = component.$children[i];
          if (this.depthFirstSearchArrowDown(child)) {
            return true;
          }
        }
        return false;
      }
    },
    arrowUp() {
      for (let i = 0; i < this.$children.length; i++) {
        var child = this.$children[i];
        let done = this.depthFirstSearchArrowUp(child);
        if (done) {
          return true;
        }
      }
      return false;
    },
    arrowDown() {
      for (let i = 0; i < this.$children.length; i++) {
        var child = this.$children[i];
        let done = this.depthFirstSearchArrowDown(child);
        if (done) {
          return true;
        }
      }
      return false;
    },
    onClick(e) {
      // Pass click event to parent, so that click event handler can be attached
      // to component.
      this.$emit('click', e);
    },
    /** Filter data view files by current show settings */
    updateShowFileTypes() {
      this.store.showFileTypes = this.dataViewFiles
        .filter((file) => file.show)
        .map(file => file.type);
    },
    /** Expand or collapse data view */
    toggleView() {
      this.file.show = !this.file.show;
      this.updateShowFileTypes();
    },
    /** Check if data view file should be shown */
    isShowFileType(type) {
      return this.store.showFileTypes.find(fileType => fileType===type);
    },
  },
  props: ['store', 'file', 'presentTags', 'presentErrors', 'dataViewFiles'],
  mixins: [FileType],
  components: {
    'traceview': TraceView,
    'transactionsviewlegacy': TransactionsViewLegacy,
    'logview': LogView,
    'flat-card': FlatCard,
    AccessibilityTraceView,
    WindowManagerTraceView,
    SurfaceFlingerTraceView,
  },
};
</script>
<style>
.bad {
  margin: 1em 1em 1em 1em;
  font-size: 4em;
  color: red;
}

.toggle-view-button {
  background: none;
  color: inherit;
  border: none;
  font: inherit;
  cursor: pointer;
  padding-right: 10px;
  display: inline-block;
}

.md-title {
  display: inline-block;
}
</style>
