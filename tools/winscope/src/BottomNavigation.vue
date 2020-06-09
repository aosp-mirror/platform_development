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
  <div class="overlay">
    <div class="overlay-content" ref="overlayContent">
      <draggable-div
        ref="videoOverlay"
        class="video-overlay"
        v-show="minimized && showVideoOverlay"
        position="bottomLeft"
        v-if="video"
      >
        <template slot="header">
          <div class="close-video-overlay" @click="closeVideoOverlay">
            <md-icon>
              close
              <md-tooltip md-direction="right">Close video overlay</md-tooltip>
            </md-icon>
          </div>
        </template>
        <template slot="main">
          <div ref="overlayVideoContainer">
            <videoview :file="video" ref="video" :height="videoHeight" />
          </div>
        </template>
      </draggable-div>
    </div>
    <md-bottom-bar class="bottom-nav" ref="bottomNav">
      <div class="nav-content">
        <div class="">
          <md-toolbar
            md-elevation="0"
            class="md-transparent">

            <div class="toolbar" :class="{ expanded: expanded }">
              <div class="resize-bar" v-show="expanded" @mousedown="onMouseDown">
                <md-icon class="drag-handle">
                  drag_handle
                  <md-tooltip md-direction="bottom">resize</md-tooltip>
                </md-icon>
              </div>

              <md-button
                class="md-icon-button show-video-overlay-btn"
                @click="openVideoOverlay"
                v-show="minimized && !showVideoOverlay"
              >
                <md-icon>
                  featured_video
                  <md-tooltip md-direction="right">Show video overlay</md-tooltip>
                </md-icon>
              </md-button>

              <div class="minimized-timeline-content" v-show="minimized">
                <div class="seek-time" v-if="seekTime">
                  <b>Seek time</b>: {{ seekTime }}
                </div>
                <timeline
                  :items="mergedTimeline.timeline"
                  :selected-index="mergedTimeline.selectedIndex"
                  :scale="scale"
                  @item-selected="onMergedTimelineItemSelected($event)"
                  class="minimized-timeline"
                />
              </div>

              <md-button class="md-icon-button toggle-btn" @click="toggle">
                <md-icon v-if="minimized">
                  expand_less
                  <md-tooltip md-direction="right">Expand timeline</md-tooltip>
                </md-icon>
                <md-icon v-else>
                  expand_more
                  <md-tooltip md-direction="right">Collapse timeline</md-tooltip>
                </md-icon>
              </md-button>
            </div>
          </md-toolbar>

          <div class="expanded-content" v-show="expanded">
            <div :v-if="video">
              <div class="expanded-content-video" ref="expandedContentVideoContainer">
                <!-- Video moved here on expansion -->
              </div>
            </div>
            <div class="flex-fill">
              <div ref="expandedTimeline" :style="`padding-top: ${resizeOffset}px;`">
                <div class="seek-time" v-if="seekTime">
                  <b>Seek time</b>: {{ seekTime }}
                </div>

                <md-list>
                  <md-list-item
                    v-for="indexedFile in indexedTimelineFiles"
                    :key="indexedFile.file.filename"
                  >
                    <md-icon>
                      {{indexedFile.file.type.icon}}
                      <md-tooltip md-direction="right">{{indexedFile.file.type.name}}</md-tooltip>
                    </md-icon>
                    <timeline
                      :items="indexedFile.file.timeline"
                      :selected-index="indexedFile.file.selectedIndex"
                      :scale="scale"
                      @item-selected="onTimelineItemSelected($event, indexedFile.index)"
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
      </div>
    </md-bottom-bar>
  </div>
</template>
<script>
import Timeline from './Timeline.vue'
import DataFilter from './DataFilter.vue'
import DraggableDiv from './DraggableDiv.vue'
import VideoView from './VideoView.vue'

import { nanos_to_string } from './transform.js'
import { DATA_TYPES } from './decode.js'

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
  props: [ 'files', 'video', 'store', 'activeFile' ],
  data() {
    // Files that should be excluded from being shown in timeline
    const timelineFileFilter = new Set([DATA_TYPES.PROTO_LOG]);

    const timelineFiles = this.files
      .filter(file => !timelineFileFilter.has(file.type));

    const indexedTimelineFiles = [];
    for (let i = 0; i < this.files.length; i++) {
      const file = this.files[i];

      if (!timelineFileFilter.has(file.type)) {
        indexedTimelineFiles.push({
          index: i,
          file: file,
        })
      }
    }

    return {
      minimized: true,
      currentTimestamp: 0,
      // height of video in expanded timeline,
      // made to match expandedTimeline dynamically
      videoHeight: 'auto',
      dragState: {
        clientY: null,
        lastDragEndPosition: null,
      },
      resizeOffset: 0,
      showVideoOverlay: true,
      videoOverlayTop: 0,
      timelineFiles,
      indexedTimelineFiles,
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
  updated () {
    this.$nextTick(function () {
      if (this.$refs.expandedTimeline && this.expanded) {
        this.videoHeight = this.$refs.expandedTimeline.clientHeight;
      } else {
        this.videoHeight = 'auto';
      }
    })
  },
  methods: {
    toggle() {
      this.minimized ? this.expand() : this.minimize();

      this.minimized = !this.minimized;
    },
    expand() {
      if (this.video) {
        this.$refs.expandedContentVideoContainer.appendChild(this.$refs.video.$el);
      }
    },
    minimize() {
      if (this.video) {
        this.$refs.overlayVideoContainer.appendChild(this.$refs.video.$el);
      }
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
    onMouseDown(e) {
      this.initResizeAction(e);
    },
    initResizeAction(e) {
      document.onmousemove = this.startResize;
      document.onmouseup = this.endResize;
    },
    startResize(e) {
      if (this.dragState.clientY === null) {
        this.dragState.clientY = e.clientY;
      }

      const movement = this.dragState.clientY - e.clientY;

      const resizeOffset = this.resizeOffset + movement;
      if (resizeOffset < 0) {
        this.resizeOffset = 0;
        this.dragState.clientY = null;
      } else if (movement > this.getBottomNavDistanceToTop()) {
        this.dragState.clientY += this.getBottomNavDistanceToTop();
        this.resizeOffset += this.getBottomNavDistanceToTop();
      } else {
        this.resizeOffset = resizeOffset;
        this.dragState.clientY = e.clientY;
      }
    },
    endResize() {
      this.dragState.lastDragEndPosition = this.dragState.clientY;
      this.dragState.clientY = null;
      document.onmouseup = null;
      document.onmousemove = null;
    },
    getBottomNavDistanceToTop() {
      return this.$refs.bottomNav.$el.getBoundingClientRect().top;
    },
    closeVideoOverlay() {
      this.showVideoOverlay = false;
    },
    openVideoOverlay() {
      this.showVideoOverlay = true;
    }
  },
  components: {
    'timeline': Timeline,
    'datafilter': DataFilter,
    'videoview': VideoView,
    'draggable-div': DraggableDiv,
  },
  mounted() {
    this.videoOverlayTop = this.$refs.overlayContent.clientHeight - 150 - 15 + "px";
  }
}
</script>
<style scoped>
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  width: 100vw;
  height: 100vh;
  z-index: 10;
  margin: 0;
  display: flex;
  flex-direction: column;
  pointer-events: none;
}

.overlay-content {
  flex-grow: 1;
}

.bottom-nav {
  background: white;
  margin: 0;
  max-height: 100vh;
  bottom: 0;
  left: 0;
  pointer-events: all;
}

.nav-content {
  width: 100%;
}

.toolbar, .active-timeline, .options {
  display: flex;
  flex-direction: row;
  flex: 1;
  align-items: flex-end;
}

.toolbar.expanded {
  align-items: baseline;
}

.minimized-timeline-content {
 flex-grow: 1;
}

.minimized-timeline-content .seek-time {
  padding: 3px 0;
}

.options, .expanded-content .seek-time {
  padding: 0 20px 15px 20px;
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

.resize-bar {
  flex-grow: 1;
}

.drag-handle {
  cursor: grab;
}

.md-icon-button {
  margin: 0;
}

.toggle-btn {
  margin-left: 8px;
}

.video-overlay {
  display: inline-block;
  margin-bottom: 15px;
  width: 150px;
  min-width: 50px;
  max-width: 50vw;
  height: auto;
  resize: horizontal;
  pointer-events: all;
}

.close-video-overlay {
  float: right;
  cursor: pointer;
}

.show-video-overlay-btn {
  align-self: flex-end;
  margin-right: 12px;
}

</style>