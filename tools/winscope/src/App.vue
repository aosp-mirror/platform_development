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
      <a class="md-button md-accent md-raised md-theme-default" @click="clear()" v-if="dataLoaded">Clear</a>
    </md-whiteframe>
    <div class="main-content">
      <md-layout v-if="!dataLoaded" class="m-2">
        <dataadb ref="adb" :store="store" @dataReady="onDataReady" @statusChange="setStatus"/>
        <datainput ref="input" :store="store" @dataReady="onDataReady" @statusChange="setStatus"/>
      </md-layout>
      <md-card v-if="dataLoaded">
        <md-whiteframe md-tag="md-toolbar" md-elevation="0" class="card-toolbar md-transparent md-dense">
          <h2 class="md-title">Timeline</h2>
          <datafilter v-for="file in files" :key="file.filename" :store="store" :file="file" />
        </md-whiteframe>
        <md-list>
          <md-list-item v-for="(file, idx) in files" :key="file.filename">
            <md-icon>{{file.type.icon}}</md-icon>
            <timeline :items="file.timeline" :selected-index="file.selectedIndex" :scale="scale" @item-selected="onTimelineItemSelected($event, idx)" class="timeline" />
          </md-list-item>
        </md-list>
      </md-card>
      <dataview v-for="file in files" :key="file.filename" :ref="file.filename" :store="store" :file="file" @focus="onDataViewFocus(file.filename)" />
    </div>
  </div>
</template>
<script>
import TreeView from './TreeView.vue'
import Timeline from './Timeline.vue'
import Rects from './Rects.vue'
import DataView from './DataView.vue'
import DataInput from './DataInput.vue'
import LocalStore from './localstore.js'
import DataAdb from './DataAdb.vue'
import DataFilter from './DataFilter.vue'

const APP_NAME = "Winscope"

// Find the index of the last element matching the predicate in a sorted array
function findLastMatchingSorted(array, predicate) {
  var a = 0;
  var b = array.length - 1;
  while (b - a > 1) {
    var m = Math.floor((a + b) / 2);
    if (predicate(array, m)) {
      a = m;
    } else {
      b = m - 1;
    }
  }
  return predicate(array, b) ? b : a;
}

export default {
  name: 'app',
  data() {
    return {
      files: [],
      title: APP_NAME,
      currentTimestamp: 0,
      activeDataView: null,
      store: LocalStore('app', {
        flattened: false,
        onlyVisible: false,
        displayDefaults: true,
      }),
    }
  },
  created() {
    window.addEventListener('keydown', this.onKeyDown);
    document.title = this.title;
  },
  methods: {
    clear() {
      this.files.forEach(function(item) { item.destroy(); })
      this.files = [];
      this.activeDataView = null;
    },
    onTimelineItemSelected(index, timelineIndex) {
      this.files[timelineIndex].selectedIndex = index;
      var t = parseInt(this.files[timelineIndex].timeline[index]);
      for (var i = 0; i < this.files.length; i++) {
        if (i != timelineIndex) {
          this.files[i].selectedIndex = findLastMatchingSorted(this.files[i].timeline, function(array, idx) {
            return parseInt(array[idx]) <= t;
          });
        }
      }
      this.currentTimestamp = t;
    },
    advanceTimeline(direction) {
      var closestTimeline = -1;
      var timeDiff = Infinity;
      for (var idx = 0; idx < this.files.length; idx++) {
        var file = this.files[idx];
        var cur = file.selectedIndex;
        if (cur + direction < 0 || cur + direction >= this.files[idx].timeline.length) {
          continue;
        }
        var d = Math.abs(parseInt(file.timeline[cur + direction]) - this.currentTimestamp);
        if (timeDiff > d) {
          timeDiff = d;
          closestTimeline = idx;
        }
      }
      if (closestTimeline >= 0) {
        this.files[closestTimeline].selectedIndex += direction;
        this.currentTimestamp = parseInt(this.files[closestTimeline].timeline[this.files[closestTimeline].selectedIndex]);
      }
    },
    onDataViewFocus(view) {
      this.activeDataView = view;
    },
    onKeyDown(event) {
      event = event || window.event;
      if (event.keyCode == 37 /* left */ ) {
        this.advanceTimeline(-1);
      } else if (event.keyCode == 39 /* right */ ) {
        this.advanceTimeline(1);
      } else if (event.keyCode == 38 /* up */ ) {
        this.$refs[this.activeView][0].arrowUp();
      } else if (event.keyCode == 40 /* down */ ) {
        this.$refs[this.activeView][0].arrowDown();
      } else {
        return false;
      }
      event.preventDefault();
      return true;
    },
    onDataReady(files) {
      this.files = files;
    },
    setStatus(status) {
      if (status) {
        this.title = status;
      } else {
        this.title = APP_NAME;
      }
    }
  },
  computed: {
    prettyDump: function() { return JSON.stringify(this.dump, null, 2); },
    dataLoaded: function() { return this.files.length > 0 },
    scale() {
      var mx = Math.max(...(this.files.map(f => Math.max(...f.timeline))));
      var mi = Math.min(...(this.files.map(f => Math.min(...f.timeline))));
      return [mi, mx];
    },
    activeView: function() {
      if (!this.activeDataView && this.files.length > 0) {
        this.activeDataView = this.files[0].filename;
      }
      return this.activeDataView;
    }
  },
  watch: {
    title() {
      document.title = this.title;
    }
  },
  components: {
    'timeline': Timeline,
    'dataview': DataView,
    'datainput': DataInput,
    'dataadb': DataAdb,
    'datafilter': DataFilter,
  },
}

</script>
<style>
.main-content>* {
  margin: 1em;
}

.card-toolbar {
  border-bottom: 1px solid rgba(0, 0, 0, .12);
}

.timeline {
  margin: 16px;
}

.container {
  display: flex;
  flex-wrap: wrap;
}

.md-layout > .md-card {
  margin: 0.5em;
}

.md-button {
  margin-top: 1em
}

h1,
h2 {
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
