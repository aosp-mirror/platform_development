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
            <md-icon>{{file.type.icon}}</md-icon>
            {{file.filename}}
          </div>
        </md-card-header-text>
        <md-button
          :href="file.blobUrl"
          :download="file.filename"
          class="md-icon-button"
        >
          <md-icon>save_alt</md-icon>
        </md-button>
      </md-card-header>

      <WindowManagerTraceView
        v-if="showInWindowManagerTraceView(file)"
        :store="store"
        :file="file"
        ref="view"
      />
      <SurfaceFlingerTraceView
        v-else-if="showInSurfaceFlingerTraceView(file)"
        :store="store"
        :file="file"
        ref="view"
      />
      <transactionsview
        v-else-if="isTransactions(file)"
        :trace="file"
        ref="view"
      />
      <logview
        v-else-if="isLog(file)"
        :file="file"
        ref="view"
      />
      <traceview
        v-else-if="showInTraceView(file)"
        :store="store"
        :file="file"
        ref="view"
      />
      <div v-else>
        <h1 class="bad">Unrecognized DataType</h1>
      </div>

    </flat-card>
  </div>
</template>
<script>
import TraceView from '@/TraceView.vue';
import WindowManagerTraceView from '@/WindowManagerTraceView.vue';
import SurfaceFlingerTraceView from '@/SurfaceFlingerTraceView.vue';
import TransactionsView from '@/TransactionsView.vue';
import LogView from '@/LogView.vue';
import FileType from '@/mixins/FileType.js';
import FlatCard from '@/components/FlatCard.vue';

export default {
  name: 'dataview',
  data() {
    return {};
  },
  methods: {
    arrowUp() {
      return this.$refs.view.arrowUp();
    },
    arrowDown() {
      return this.$refs.view.arrowDown();
    },
    onClick(e) {
      // Pass click event to parent, so that click event handler can be attached
      // to component.
      this.$emit('click', e);
    },
  },
  props: ['store', 'file'],
  mixins: [FileType],
  components: {
    'traceview': TraceView,
    'transactionsview': TransactionsView,
    'logview': LogView,
    'flat-card': FlatCard,
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
</style>
