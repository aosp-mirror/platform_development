<!-- Copyright (C) 2020 The Android Open Source Project

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
  <md-bottom-bar class="bottom-nav">
    <div class="md-layout md-alignment-left-center nav-content">
      <div class="md-layout-item">
        <md-toolbar
          md-elevation="0"
          class="md-transparent">

          <div class="toolbar">
            <div class="toolbar-content">
              <div class="seek-time" v-if="seekTime">
                <b>Seek time</b>: {{ seekTime }}
              </div>
              <div class="active-timeline" v-show="minimized">
                <md-icon>
                  {{timelineActiveFile.type.icon}}
                  <md-tooltip md-direction="right">{{timelineActiveFile.type.name}}</md-tooltip>
                </md-icon>
                <timeline
                  :items="timelineActiveFile.timeline"
                  :selected-index="timelineActiveFile.selectedIndex"
                  :scale="scale"
                  @item-selected="onTimelineItemSelected($event, timelineActiveFileIndex)"
                  class="timeline"
                />
              </div>
            </div>

            <md-button class="md-icon-button toggle-btn" @click="toggle">
              <md-icon v-if="minimized">expand_less</md-icon>
              <md-icon v-else>expand_more</md-icon>
            </md-button>
          </div>
        </md-toolbar>

        <div class="expanded-content" v-show="expanded">
          <md-list>
            <md-list-item v-for="(file, idx) in files" :key="file.filename">
              <md-icon>
                {{file.type.icon}}
                <md-tooltip md-direction="right">{{file.type.name}}</md-tooltip>
              </md-icon>
              <timeline
                :items="file.timeline"
                :selected-index="file.selectedIndex"
                :scale="scale"
                @item-selected="onTimelineItemSelected($event, idx)"
                class="timeline"
              />
            </md-list-item>
          </md-list>
          <div class="options">
            <div class="datafilter">
              <label>Datafilter</label>
              <datafilter v-for="file in files" :key="file.filename" :store="store" :file="file" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </md-bottom-bar>
</template>
<script>
import Timeline from './Timeline.vue'
import DataFilter from './DataFilter.vue'
import { nanos_to_string } from './transform.js'

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
  name: 'bottom-navigation',
  props: [ 'files', 'store', 'dataViewPositions', 'activeFile' ],
  data() {
    return {
      minimized: true,
      currentTimestamp: 0,
    }
  },
  computed: {
    expanded() {
      return !this.minimized;
    },
    timelineActiveFile() {
      if (this.activeFile) {
        return this.activeFile;
      }

      if (!this.dataViewPositions) {
        return this.files[0];
      }

      // If not active file is selected figure out which one takes up the most
      // of the screen and mark that one as the active file
      const visibleHeight =
        Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);
      let maxScreenSpace = 0;
      let selectedFile = this.files[0];
      for (const file of this.files) {
        const pos = this.dataViewPositions[file.filename];

        let screenSpace = 0;
        if (0 <= pos.top && pos.top <= visibleHeight) {
          screenSpace = Math.min(visibleHeight, pos.bottom) - pos.top;
        } else if (0 <= pos.bottom && pos.bottom <= visibleHeight) {
          screenSpace = pos.bottom - Math.max(0, pos.top);
        } else if (pos.top <=0 && pos.bottom >= visibleHeight) {
          screenSpace = visibleHeight;
        }

        if (screenSpace >= maxScreenSpace) {
          maxScreenSpace = screenSpace;
          selectedFile = file;
        }
      }

      return selectedFile;
    },
    timelineActiveFileIndex() {
      for (let i = 0; i < this.files.length; i++) {
        if (this.files[i].filename == this.timelineActiveFile.filename) {
          return i;
        }
      }
      throw "Active file index not found";
    },
    seekTime() {
      return nanos_to_string(this.currentTimestamp);
    },
    scale() {
      var mx = Math.max(...(this.files.map(f => Math.max(...f.timeline))));
      var mi = Math.min(...(this.files.map(f => Math.min(...f.timeline))));
      return [mi, mx];
    },
  },
  methods: {
    toggle() {
      this.minimized = !this.minimized;
    },
    fileIsVisible(f) {
      return this.visibleDataViews.includes(f.filename);
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
  },
  components: {
    timeline: Timeline,
    datafilter: DataFilter,
  }
}
</script>
<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1;
  background: white;
  margin: 0;
}

.nav-content {
  width: 100%;
}

.toggle-btn {
  margin-left: auto;
}

.toolbar, .active-timeline, .options {
  display: flex;
  flex-direction: row;
  flex: 1;
  align-items: center;
}

.toolbar-content {
 flex-grow: 1;
}

.toolbar-content .seek-time {
  margin: 8px 0 -8px 0;
}

.options {
  padding: 0 20px 15px 20px
}

.options label {
  font-weight: 600;
}

.options .datafilter {
  height: 50px;
  display: flex;
  align-items: center;
}

</style>