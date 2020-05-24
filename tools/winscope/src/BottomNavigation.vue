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
                <timeline
                  :items="mergedTimeline.timeline"
                  :selected-index="mergedTimeline.selectedIndex"
                  :scale="scale"
                  @item-selected="onMergedTimelineItemSelected($event)"
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
          <div class="md-layout-item video" :v-if="video">
            <videoview :file="video" :ref="video.filename" />
          </div>
          <div class="flex-fill">
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
    </div>
  </md-bottom-bar>
</template>
<script>
import Timeline from './Timeline.vue'
import DataFilter from './DataFilter.vue'
import VideoView from './VideoView.vue'
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
  props: [ 'files', 'video', 'store', 'dataViewPositions', 'activeFile' ],
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
    seekTime() {
      return nanos_to_string(this.currentTimestamp);
    },
    scale() {
      var mx = Math.max(...(this.files.map(f => Math.max(...f.timeline))));
      var mi = Math.min(...(this.files.map(f => Math.min(...f.timeline))));
      return [mi, mx];
    },
    mergedTimeline() {
      const mergedTimeline = {
        timeline: [], // Array of integers timestamps
        selectedIndex: 0,
      };

      const timelineIndexes = [];
      const timelines = [];
      for (const file of this.files) {
        timelineIndexes.push(0);
        timelines.push(file.timeline);
      }

      while(true) {
        let minTime = Infinity;
        let timelineToAdvance;

        for (let i = 0; i < timelines.length; i++) {
          const timeline = timelines[i];
          const index = timelineIndexes[i];

          if (index >= timeline.length) {
            continue;
          }

          const time = timeline[index];

          if (time < minTime) {
            minTime = time;
            timelineToAdvance = i;
          }
        }

        if (timelineToAdvance === undefined) {
          // No more elements left
          break;
        }

        timelineIndexes[timelineToAdvance]++;
        mergedTimeline.timeline.push(minTime);
      }

      return mergedTimeline;
    }
  },
  methods: {
    toggle() {
      this.minimized = !this.minimized;
    },
    fileIsVisible(f) {
      return this.visibleDataViews.includes(f.filename);
    },
    updateSelectedIndex(file, timestamp) {
      file.selectedIndex = findLastMatchingSorted(
        file.timeline,
        function(array, idx) {
          return parseInt(array[idx]) <= timestamp;
        }
      );
    },
    onMergedTimelineItemSelected(index) {
      this.mergedTimeline.selectedIndex = index;
      const timestamp = this.mergedTimeline.timeline[index];
      this.files.forEach(file => this.updateSelectedIndex(file, timestamp));
      this.currentTimestamp = timestamp;
    },
    onTimelineItemSelected(index, timelineIndex) {
      this.files[timelineIndex].selectedIndex = index;
      const timestamp = parseInt(this.files[timelineIndex].timeline[index]);
      for (let i = 0; i < this.files.length; i++) {
        if (i != timelineIndex) {
          this.updateSelectedIndex(this.files[i], timestamp);
        }
      }

      this.updateSelectedIndex(this.mergedTimeline, timestamp);

      this.currentTimestamp = timestamp;
    },
    advanceTimeline(direction) {
      if (0 < this.mergedTimeline.selectedIndex + direction &&
            this.mergedTimeline.selectedIndex + direction <
              this.mergedTimeline.timeline.length) {
        this.mergedTimeline.selectedIndex += direction;
      }

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
    videoview: VideoView,
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

.expanded-content {
  display: flex;
}

.flex-fill {
  flex-grow: 1;
}

.video {
  flex-grow: 0;
}

</style>