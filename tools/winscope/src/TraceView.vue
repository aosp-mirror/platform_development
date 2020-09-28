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
  <md-card-content class="container">
    <md-card class="rects" v-if="hasScreenView">
      <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
        <h2 class="md-title">Screen</h2>
      </md-whiteframe>
      <md-whiteframe md-elevation="8">
        <rects :bounds="bounds" :rects="rects" :highlight="highlight" @rect-click="onRectClick" />
      </md-whiteframe>
    </md-card>
    <md-card class="hierarchy">
      <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
        <h2 class="md-title" style="flex: 1;">Hierarchy</h2>
        <md-checkbox v-model="store.onlyVisible">Only visible</md-checkbox>
        <md-checkbox v-model="store.flattened">Flat</md-checkbox>
        <input id="filter" type="search" placeholder="Filter..." v-model="hierarchyPropertyFilterString" />
      </md-whiteframe>
      <tree-view class="data-card" :item="tree" @item-selected="itemSelected" :selected="hierarchySelected" :filter="hierarchyFilter" :flattened="store.flattened" ref="hierarchy" />
    </md-card>
    <md-card class="properties">
      <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
        <h2 class="md-title" style="flex: 1">Properties</h2>
        <div class="filter">
          <input id="filter" type="search" placeholder="Filter..." v-model="propertyFilterString" />
        </div>
      </md-whiteframe>
      <tree-view class="pre-line-data-card" :item="selectedTree" :filter="propertyFilter" />
    </md-card>
  </md-card-content>
</template>
<script>
import TreeView from './TreeView.vue'
import Timeline from './Timeline.vue'
import Rects from './Rects.vue'

import { transform_json } from './transform.js'
import { format_transform_type, is_simple_transform } from './matrix_utils.js'
import { DATA_TYPES } from './decode.js'

function formatColorTransform(vals) {
    const fixedVals = vals.map(v => v.toFixed(1));
    var formatted = ``;
    for (var i = 0; i < fixedVals.length; i += 4) {
      formatted += `[`;
      formatted += fixedVals.slice(i, i + 4).join(", ");
      formatted += `] `;
    }
    return formatted;
}


function formatProto(obj) {
  if (!obj || !obj.$type) {
    return;
  }
  if (obj.$type.name === 'RectProto') {
    return `(${obj.left}, ${obj.top})  -  (${obj.right}, ${obj.bottom})`;
  } else if (obj.$type.name === 'FloatRectProto') {
    return `(${obj.left.toFixed(3)}, ${obj.top.toFixed(3)})  -  (${obj.right.toFixed(3)}, ${obj.bottom.toFixed(3)})`;
  } else if (obj.$type.name === 'PositionProto') {
    return `(${obj.x.toFixed(3)}, ${obj.y.toFixed(3)})`;
  } else if (obj.$type.name === 'SizeProto') {
    return `${obj.w} x ${obj.h}`;
  } else if (obj.$type.name === 'ColorProto') {
    return `r:${obj.r} g:${obj.g} \n b:${obj.b} a:${obj.a}`;
  } else if (obj.$type.name === 'TransformProto') {
    var transform_type = format_transform_type(obj);
    if (is_simple_transform(obj)) {
      return `${transform_type}`;
    }
    return `${transform_type}  dsdx:${obj.dsdx.toFixed(3)}   dtdx:${obj.dtdx.toFixed(3)}   dsdy:${obj.dsdy.toFixed(3)}   dtdy:${obj.dtdy.toFixed(3)}`;
  } else if (obj.$type.name === 'ColorTransformProto') {
    var formated = formatColorTransform(obj.val);
    return `${formated}`;
  }
}

export default {
  name: 'traceview',
  data() {
    return {
      propertyFilterString: "",
      hierarchyPropertyFilterString:"",
      selectedTree: {},
      hierarchySelected: null,
      lastSelectedStableId: null,
      bounds: {},
      rects: [],
      tree: null,
      highlight: null,
    }
  },
  methods: {
    itemSelected(item) {
      this.hierarchySelected = item;
      this.selectedTree = transform_json(item.obj, item.name, {
        skip: item.skip,
        formatter: formatProto
      });
      this.highlight = item.highlight;
      this.lastSelectedStableId = item.stableId;
      this.$emit('focus');
    },
    onRectClick(item) {
      if (item) {
        this.itemSelected(item);
      }
    },
    setData(item) {
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
    arrowUp() {
      return this.$refs.hierarchy.selectPrev();
    },
    arrowDown() {
      return this.$refs.hierarchy.selectNext();
    },
  },
  created() {
    this.setData(this.file.data[this.file.selectedIndex]);
  },
  watch: {
    selectedIndex() {
      this.setData(this.file.data[this.file.selectedIndex]);
    }
  },
  props: ['store', 'file'],
  computed: {
    selectedIndex() {
      return this.file.selectedIndex;
    },
    hierarchyFilter() {
      var hierarchyPropertyFilter = getFilter(this.hierarchyPropertyFilterString);
      return this.store.onlyVisible ? (c) => {
        return c.visible && hierarchyPropertyFilter(c);} : hierarchyPropertyFilter;
    },
    propertyFilter() {
      return getFilter(this.propertyFilterString);
    },
    hasScreenView() {
      return this.file.type !== DATA_TYPES.TRANSACTION;
    },
  },
  components: {
    'tree-view': TreeView,
    'rects': Rects,
  }
}

function getFilter(filterString) {
  var filterStrings = filterString.split(",");
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
    var apply = (f) => f(String(item.name));
    return (positive.length === 0 || positive.some(apply)) &&
          (negative.length === 0 || negative.every(apply));
  };
  return filter;
}

</script>
<style>
.rects {
  flex: none;
  margin: 8px;
}

.hierarchy,
.properties {
  flex: 1;
  margin: 8px;
  min-width: 400px;
}

.hierarchy>.tree-view,
.properties>.tree-view {
  margin: 16px;
}

.data-card {
  overflow: auto;
  max-height: 48em;
}

.pre-line-data-card {
  overflow: auto;
  max-height: 48em;
  white-space: pre-line;
}

</style>
