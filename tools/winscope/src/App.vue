<!-- Copyright (C) 2017 The Android Open Source Project

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
  <div id="app">
    <md-whiteframe md-tag="md-toolbar">
      <h1 class="md-title" style="flex: 1">{{title}}</h1>

      <input type="file" @change="onLoadFile" id="upload-file" v-show="false"/>
      <label class="md-button md-accent md-raised md-theme-default" for="upload-file">Open File</label>

      <div>
        <md-select v-model="fileType" id="file-type" placeholder="File type">
          <md-option value="auto">Detect type</md-option>
          <md-option :value="k" v-for="(v,k) in FILE_TYPES">{{v.name}}</md-option>
        </md-select>
      </div>

    </md-whiteframe>

    <div class="main-content" v-if="timeline.length">
      <md-card class="timeline-card">
        <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense"><h2 class="md-title">Timeline</h2></md-whiteframe>
        <timeline :items="timeline" :selected="tree" @item-selected="onTimelineItemSelected" class="timeline" />
      </md-card>

      <div class="container">
        <md-card class="rects">
          <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense"><h2 class="md-title">Screen</h2></md-whiteframe>
          <md-whiteframe md-elevation="8">
            <rects :bounds="bounds" :rects="rects" :highlight="highlight" @rect-click="onRectClick" />
          </md-whiteframe>
        </md-card>

        <md-card class="hierarchy">
          <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
            <h2 class="md-title" style="flex: 1;">Hierarchy</h2>
            <md-checkbox v-model="store.onlyVisible">Only visible</md-checkbox>
            <md-checkbox v-model="store.flattened">Flat</md-checkbox>
          </md-whiteframe>
          <tree-view :item="tree" @item-selected="itemSelected" :selected="hierarchySelected" :filter="hierarchyFilter" :flattened="store.flattened" ref="hierarchy" />
        </md-card>

        <md-card class="properties">
          <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
            <h2 class="md-title" style="flex: 1">Properties</h2>
            <div class="filter">
              <input id="filter" type="search" placeholder="Filter..." v-model="propertyFilterString" />
            </div>
          </md-whiteframe>
          <tree-view :item="selectedTree" :filter="propertyFilter" />
        </md-card>
      </div>
    </div>
  </div>
</template>

<script>

import jsonProtoDefs from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto'
import jsonProtoDefsSF from 'frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto'
import protobuf from 'protobufjs'

import TreeView from './TreeView.vue'
import Timeline from './Timeline.vue'
import Rects from './Rects.vue'

import detectFile from './detectfile.js'
import LocalStore from './localstore.js'

import {transform_json} from './transform.js'
import {transform_layers, transform_layers_trace} from './transform_sf.js'
import {transform_window_service, transform_window_trace} from './transform_wm.js'


var protoDefs = protobuf.Root.fromJSON(jsonProtoDefs)
    .addJSON(jsonProtoDefsSF.nested);

var TraceMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerTraceFileProto");
var ServiceMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerServiceDumpProto");
var LayersMessage = protoDefs.lookupType("android.surfaceflinger.LayersProto");
var LayersTraceMessage = protoDefs.lookupType("android.surfaceflinger.LayersTraceFileProto");

function formatProto(obj) {
  if (!obj || !obj.$type) {
    return;
  }
  if (obj.$type.fullName === '.android.surfaceflinger.RectProto') {
    return `(${obj.left},${obj.top})-(${obj.right},${obj.bottom})`;
  } else if (obj.$type.fullName === '.android.surfaceflinger.PositionProto') {
    return `(${obj.x},${obj.y})`;
  } else if (obj.$type.fullName === '.android.surfaceflinger.SizeProto') {
    return `${obj.w}x${obj.h}`;
  } else if (obj.$type.fullName === '.android.surfaceflinger.ColorProto') {
    return `r:${obj.r} g:${obj.g} b:${obj.b} a:${obj.a}`;
  }
}

const FILE_TYPES = {
  'window_dump': {
    protoType: ServiceMessage,
    transform: transform_window_service,
    name: "WindowManager dump",
    timeline: false,
  },
  'window_trace': {
    protoType: TraceMessage,
    transform: transform_window_trace,
    name: "WindowManager trace",
    timeline: true,
  },
  'layers_dump': {
    protoType: LayersMessage,
    transform: transform_layers,
    name: "SurfaceFlinger dump",
    timeline: false,
  },
  'layers_trace': {
    protoType: LayersTraceMessage,
    transform: transform_layers_trace,
    name: "SurfaceFlinger trace",
    timeline: true,
  },
};

export default {
  name: 'app',
  data() {
    return {
      selectedTree: {},
      hierarchySelected: null,
      tree: {},
      timeline: [],
      bounds: {},
      rects: [],
      highlight: null,
      timelineIndex: 0,
      title: "The Tool",
      filename: "",
      lastSelectedStableId: null,
      propertyFilterString: "",
      store: LocalStore('app', {
        flattened: false,
        onlyVisible: false,
      }),
      FILE_TYPES,
      fileType: "auto",
    }
  },
  created() {
    window.addEventListener('keydown', this.onKeyDown);
    document.title = this.title;
  },
  methods: {
    onLoadFile(e) {
      return this.onLoadProtoFile(e, this.fileType);
    },
    onLoadProtoFile(event, type) {
      var files = event.target.files || event.dataTransfer.files;
      var file = files[0];
      if (!file) {
        // No file selected.
        return;
      }
      this.filename = file.name;
      this.title = this.filename + " (loading)";

      var reader = new FileReader();
      reader.onload = (e) => {
        var buffer = new Uint8Array(e.target.result);
        var filetype = FILE_TYPES[type] || FILE_TYPES[detectFile(buffer)];
        if (!filetype) {
          this.title = this.filename + ": Could not detect file type."
          event.target.value = '';
          return;
        }
        this.title = this.filename + " (loading " + filetype.name + ")";

        try {
          var decoded = filetype.protoType.decode(buffer);
          decoded = filetype.protoType.toObject(decoded, {enums: String, defaults: true});
          var transformed = filetype.transform(decoded);
        } catch (ex) {
          this.title = this.filename + " (loading " + filetype.name + "):" + ex;
          return;
        } finally {
          event.target.value = '';
        }

        if (filetype.timeline) {
          this.timeline = transformed.children;
        } else {
          this.timeline = [transformed];
        }

        this.title = this.filename + " (" + filetype.name + ")";

        this.lastSelectedStableId = null;
        this.onTimelineItemSelected(this.timeline[0], 0);
      }
      reader.readAsArrayBuffer(files[0]);
    },
    itemSelected(item) {
      this.hierarchySelected = item;
      this.selectedTree = transform_json(item.obj, item.name, {
          skip: item.skip,
          formatter: formatProto});
      this.highlight = item.highlight;
      this.lastSelectedStableId = item.stableId;
    },
    onRectClick(item) {
      if (item) {
        this.itemSelected(item);
      }
    },
    onTimelineItemSelected(item, index) {
      this.timelineIndex = index;
      this.tree = item;
      this.rects = [...item.rects].reverse();
      this.bounds = item.bounds;

      this.hierarchySelected = null;
      this.selectedTree = {};
      this.highlight = null;

      function find_item(item, stableId) {
        if (item.stableId === stableId) {
          return item;
        }
        if (Array.isArray(item.children)) {
          for (var child of item.children) {
            var found = find_item(child, stableId);
            if (found) {
              return found;
            }
          }
        }
        return null;
      }

      if (this.lastSelectedStableId) {
        var found = find_item(item, this.lastSelectedStableId);
        if (found) {
          this.itemSelected(found);
        }
      }
    },
    onKeyDown(event) {
      event = event || window.event;
      if (event.keyCode == 37 /* left */) {
        this.advanceTimeline(-1);
      } else if (event.keyCode == 39 /* right */) {
        this.advanceTimeline(1);
      } else if (event.keyCode == 38 /* up */) {
        this.$refs.hierarchy.selectPrev();
      } else if (event.keyCode == 40 /* down */) {
        this.$refs.hierarchy.selectNext();
      } else {
        return false;
      }
      event.preventDefault();
      return true;
    },
    advanceTimeline(frames) {
      if (!Array.isArray(this.timeline) || this.timeline.length == 0) {
        return false;
      }
      var nextIndex = this.timelineIndex + frames;
      if (nextIndex < 0) {
        nextIndex = 0;
      }
      if (nextIndex >= this.timeline.length) {
        nextIndex = this.timeline.length - 1;
      }
      this.onTimelineItemSelected(this.timeline[nextIndex], nextIndex);
      return true;
    },
  },
  computed: {
    prettyDump: function() { return JSON.stringify(this.dump, null, 2); },
    hierarchyFilter() {
      return this.store.onlyVisible ? (c, flattened) => {
        return c.visible || c.childrenVisible && !flattened;
      } : null;
    },
    propertyFilter() {
      var filterStrings = this.propertyFilterString.split(",");
      var positive = [];
      var negative = [];
      filterStrings.forEach((f) => {
        if (f.startsWith("!")) {
          var str = f.substring(1);
          negative.push((s) => s.indexOf(str) === -1);
        } else {
          var str = f;
          positive.push((s) => s.indexOf(str) !== -1);
        }
      });
      var filter = (item) => {
        var apply = (f) => f(item.name);
        return (positive.length === 0 || positive.some(apply))
            && (negative.length === 0 || negative.every(apply));
      };
      filter.includeChildren = true;
      return filter;
    },
  },
  watch: {
    title() {
      document.title = this.title;
    }
  },
  components: {
    'tree-view': TreeView,
    'timeline': Timeline,
    'rects': Rects,
  }
}
</script>

<style>
#app {
}

.main-content {
  padding: 8px;
}

.card-toolbar {
  border-bottom: 1px solid rgba(0, 0, 0, .12);
}

.timeline-card {
  margin: 8px;
}

.timeline {
  margin: 16px;
}

.screen {
  border: 1px solid black;
}

.container {
  display: flex;
  flex-wrap: wrap;
}

.rects {
  flex: none;
  margin: 8px;
}

.hierarchy, .properties {
  flex: 1;
  margin: 8px;
  min-width: 400px;
}

.hierarchy > .tree-view, .properties > .tree-view {
  margin: 16px;
}

h1, h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}
</style>
