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
  <div class="overlay" v-if="hasTimeline || video">
    <div class="overlay-content" ref="overlayContent">
      <draggable-div
        ref="videoOverlay"
        class="video-overlay"
        v-show="minimized && showVideoOverlay"
        position="bottomLeft"
        :asyncLoad="true"
        :resizeable="true"
        v-on:requestExtraWidth="updateVideoOverlayWidth"
        :style="videoOverlayStyle"
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
            <videoview
              ref="video"
              :file="video"
              :height="videoHeight"
              @loaded="videoLoaded" />
          </div>
        </template>
      </draggable-div>
    </div>
    <md-bottom-bar
      class="bottom-nav"
      v-if="hasTimeline || (video && !showVideoOverlay)"
      ref="bottomNav"
    >
      <div class="nav-content">
        <div class="">
          <md-toolbar
            md-elevation="0"
            class="md-transparent">

            <div class="toolbar" :class="{ expanded: expanded }">
              <div class="resize-bar" v-show="expanded">
                <div v-if="video" @mousedown="resizeBottomNav">
                  <md-icon class="drag-handle">
                    drag_handle
                    <md-tooltip md-direction="top">resize</md-tooltip>
                  </md-icon>
                </div>
              </div>

              <div class="active-timeline" v-show="minimized">
                <div
                  class="active-timeline-icon"
                  @click="$refs.navigationTypeSelection.$el.querySelector('input').click()"
                >
                  <md-icon class="collapsed-timeline-icon">
                    {{ collapsedTimelineIcon }}
                    <md-tooltip>
                      {{ collapsedTimelineIconTooltip }}
                    </md-tooltip>
                  </md-icon>
                </div>

                <md-field ref="navigationTypeSelection" class="nagivation-style-selection-field">
                  <label>Navigation</label>
                  <md-select v-model="navigationStyle" name="navigationStyle" md-dense>
                    <md-icon-option :value="NAVIGATION_STYLE.GLOBAL"
                      icon="public"
                      desc="Consider all timelines for navigation"
                    />
                    <md-icon-option
                      :value="NAVIGATION_STYLE.FOCUSED"
                      :icon="focusedFile.type.icon"
                      :desc="`Automatically switch what timeline is considered for navigation based
                        on what is visible on screen. Currently ${focusedFile.type.name}.`"
                    />
                    <!-- TODO: Add edit button for custom settings that opens popup dialog menu -->
                    <md-icon-option
                      :value="NAVIGATION_STYLE.CUSTOM"
                      icon="dashboard_customize"
                      desc="Considers only the enabled timelines for navigation. Expand the bottom bar to toggle timelines."
                    />
                    <md-optgroup label="Targeted">
                      <md-icon-option
                        v-for="file in timelineFiles"
                        v-bind:key="file.type.name"
                        :value="`${NAVIGATION_STYLE.TARGETED}-${file.type.name}`"
                        :displayValue="file.type.name"
                        :shortValue="NAVIGATION_STYLE.TARGETED"
                        :icon="file.type.icon"
                        :desc="`Only consider ${file.type.name} for timeline navigation.`"
                      />
                    </md-optgroup>
                  </md-select>
                </md-field>
              </div>

              <div class="minimized-timeline-content" v-show="minimized" v-if="hasTimeline">
                <label>
                  {{ seekTime }}
                </label>
                <timeline
                  :timeline="minimizedTimeline.timeline"
                  :selected-index="minimizedTimeline.selectedIndex"
                  :scale="scale"
                  :crop="crop"
                  class="minimized-timeline"
                />
              </div>

              <md-button
                class="md-icon-button show-video-overlay-btn"
                :class="{active: minimized && showVideoOverlay}"
                @click="toggleVideoOverlay"
                v-show="minimized"
              >
                <i class="md-icon md-icon-font">
                  featured_video
                </i>
                <md-tooltip md-direction="top">
                  <span v-if="showVideoOverlay">Hide video overlay</span>
                  <span v-else>Show video overlay</span>
                </md-tooltip>
              </md-button>

              <md-button class="md-icon-button toggle-btn" @click="toggle">
                <md-icon v-if="minimized">
                  expand_less
                  <md-tooltip md-direction="top">Expand timeline</md-tooltip>
                </md-icon>
                <md-icon v-else>
                  expand_more
                  <md-tooltip md-direction="top">Collapse timeline</md-tooltip>
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
                    v-for="file in timelineFiles"
                    :key="file.filename"
                  >
                    <div
                      class="trace-icon"
                      :class="{disabled: file.timelineDisabled}"
                      @click="toggleTimeline(file)"
                    >
                      <i class="material-icons">
                        {{file.type.icon}}
                        <md-tooltip md-direction="bottom">{{file.type.name}}</md-tooltip>
                      </i>
                    </div>
                    <timeline
                      :timeline="file.timeline"
                      :selected-index="file.selectedIndex"
                      :scale="scale"
                      :crop="crop"
                      :disabled="file.timelineDisabled"
                      class="timeline"
                    />
                  </md-list-item>
                </md-list>
                <div class="timeline-selection">
                  <label>Timeline Area Selection</label>
                  <span class="material-icons help-icon">
                    help_outline
                    <md-tooltip md-direction="right">Select the area of the timeline to focus on. Click and drag to select.</md-tooltip>
                  </span>
                  <br />
                  <timeline-selection
                    :timeline="mergedTimeline.timeline"
                    :start-timestamp="0"
                    :end-timestamp="0"
                    :scale="scale"
                    v-on:crop="onTimelineCrop"
                    v-on:showVideoAt="changeVideoTimestamp"
                    v-on:resetVideoTimestamp="resetVideoTimestamp"
                  />
                </div>

                <div class="help" v-if="!minimized">
                  <div class="help-icon-wrapper">
                    <span class="material-icons help-icon">
                      help_outline
                      <md-tooltip md-direction="left">Click on icons to disable timelines</md-tooltip>
                    </span>
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
import Timeline from './Timeline.vue';
import TimelineSelection from './TimelineSelection.vue';
import DraggableDiv from './DraggableDiv.vue';
import VideoView from './VideoView.vue';
import MdIconOption from './components/IconSelection/IconSelectOption.vue';
import FileType from './mixins/FileType.js';
import {NAVIGATION_STYLE} from './utils/consts';

import {nanos_to_string} from './transform.js';

export default {
  name: 'overlay',
  props: ['store'],
  mixins: [FileType],
  data() {
    return {
      minimized: true,
      // height of video in expanded timeline,
      // made to match expandedTimeline dynamically
      videoHeight: 'auto',
      dragState: {
        clientY: null,
        lastDragEndPosition: null,
      },
      resizeOffset: 0,
      showVideoOverlay: true,
      mergedTimeline: null,
      NAVIGATION_STYLE,
      navigationStyle: this.store.navigationStyle,
      videoOverlayExtraWidth: 0,
      crop: null,
    };
  },
  created() {
    this.mergedTimeline = this.computeMergedTimeline();
    this.$store.commit('setMergedTimeline', this.mergedTimeline);
    this.updateNavigationFileFilter();
  },
  mounted() {
    this.emitBottomHeightUpdate();
  },
  destroyed() {
    this.$store.commit('removeMergedTimeline', this.mergedTimeline);
  },
  watch: {
    navigationStyle(style) {
      // Only store navigation type in local store if it's a type that will
      // work regardless of what data is loaded.
      if (style === NAVIGATION_STYLE.GLOBAL || style === NAVIGATION_STYLE.FOCUSED) {
        this.store.navigationStyle = style;
      }
      this.updateNavigationFileFilter();
    },
    minimized() {
      // Minimized toggled
      this.updateNavigationFileFilter();

      this.$nextTick(this.emitBottomHeightUpdate);
    },
  },
  computed: {
    video() {
      return this.$store.getters.video;
    },
    videoOverlayStyle() {
      return {
        width: 150 + this.videoOverlayExtraWidth + 'px',
      };
    },
    timelineFiles() {
      return this.$store.getters.timelineFiles;
    },
    focusedFile() {
      return this.$store.state.focusedFile;
    },
    expanded() {
      return !this.minimized;
    },
    seekTime() {
      return nanos_to_string(this.currentTimestamp);
    },
    scale() {
      const mx = Math.max(...(this.timelineFiles.map((f) => Math.max(...f.timeline))));
      const mi = Math.min(...(this.timelineFiles.map((f) => Math.min(...f.timeline))));
      return [mi, mx];
    },
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    hasTimeline() {
      // Returns true if a meaningful timeline exists (i.e. not only dumps)
      for (const file of this.timelineFiles) {
        const timeline = file.timeline;
        if (file.timeline.length > 0 &&
            (file.timeline[0] !== undefined || file.timeline.length > 1)) {
          return true;
        }
      }

      return false;
    },
    collapsedTimelineIconTooltip() {
      switch (this.navigationStyle) {
        case NAVIGATION_STYLE.GLOBAL:
          return 'All timelines';

        case NAVIGATION_STYLE.FOCUSED:
          return `Focused: ${this.focusedFile.type.name}`;

        case NAVIGATION_STYLE.CUSTOM:
          return 'Enabled timelines';

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            throw new Error('Unexpected nagivation type');
          }

          const fileType = split[1];
          return this.getDataTypeByName(fileType).name;
      }
    },
    collapsedTimelineIcon() {
      switch (this.navigationStyle) {
        case NAVIGATION_STYLE.GLOBAL:
          return 'public';

        case NAVIGATION_STYLE.FOCUSED:
          return this.focusedFile.type.icon;

        case NAVIGATION_STYLE.CUSTOM:
          return 'dashboard_customize';

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            throw new Error('Unexpected nagivation type');
          }

          const fileType = split[1];
          return this.getDataTypeByName(fileType).icon;
      }
    },
    minimizedTimeline() {
      if (this.navigationStyle === NAVIGATION_STYLE.GLOBAL) {
        return this.mergedTimeline;
      }

      if (this.navigationStyle === NAVIGATION_STYLE.FOCUSED) {
        return this.focusedFile;
      }

      if (this.navigationStyle === NAVIGATION_STYLE.CUSTOM) {
        // TODO: Return custom timeline
        return this.mergedTimeline;
      }

      if (this.navigationStyle.split('-')[0] === NAVIGATION_STYLE.TARGETED) {
        return this.$store.state
            .filesByType[this.navigationStyle.split('-')[1]];
      }

      throw new Error('Unexpected Nagivation Style');
    },
  },
  updated() {
    this.$nextTick(function() {
      if (this.$refs.expandedTimeline && this.expanded) {
        this.videoHeight = this.$refs.expandedTimeline.clientHeight;
      } else {
        this.videoHeight = 'auto';
      }
    });
  },
  methods: {
    emitBottomHeightUpdate() {
      if (this.$refs.bottomNav) {
        const newHeight = this.$refs.bottomNav.$el.clientHeight;
        this.$emit('bottom-nav-height-change', newHeight);
      }
    },
    computeMergedTimeline() {
      const mergedTimeline = {
        timeline: [], // Array of integers timestamps
        selectedIndex: 0,
      };

      const timelineIndexes = [];
      const timelines = [];
      for (const file of this.timelineFiles) {
        timelineIndexes.push(0);
        timelines.push(file.timeline);
      }

      while (true) {
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
    },
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
    resizeBottomNav(e) {
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
    },
    toggleVideoOverlay() {
      this.showVideoOverlay = !this.showVideoOverlay;
    },
    videoLoaded() {
      this.$refs.videoOverlay.contentLoaded();
    },
    toggleTimeline(file) {
      this.$set(file, 'timelineDisabled', !file.timelineDisabled);
    },
    updateNavigationFileFilter() {
      if (!this.minimized) {
        // Always use custom mode navigation when timeline is expanded
        this.$store.commit('setNavigationFilesFilter', (f) => !f.timelineDisabled);
        return;
      }

      let navigationStyleFilter;
      switch (this.navigationStyle) {
        case NAVIGATION_STYLE.GLOBAL:
          navigationStyleFilter = (f) => true;
          break;

        case NAVIGATION_STYLE.FOCUSED:
          navigationStyleFilter = (f) => f.type.name === this.focusedFile.type.name;
          break;

        case NAVIGATION_STYLE.CUSTOM:
          navigationStyleFilter = (f) => !f.timelineDisabled;
          break;

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            throw new Error('Unexpected nagivation type');
          }

          const fileType = split[1];
          navigationStyleFilter = (f) => f.type.name === this.getDataTypeByName(fileType).name;
      }

      this.$store.commit('setNavigationFilesFilter', navigationStyleFilter);
    },
    updateVideoOverlayWidth(width) {
      this.videoOverlayExtraWidth = width;
    },
    onTimelineCrop(cropDetails) {
      this.crop = cropDetails;
    },
    changeVideoTimestamp(ts) {
      this.$refs.video.selectFrameAtTime(ts);
    },
    resetVideoTimestamp() {
      this.$refs.video.jumpToSelectedIndex();
    }
  },
  components: {
    'timeline': Timeline,
    'timeline-selection': TimelineSelection,
    'videoview': VideoView,
    'draggable-div': DraggableDiv,
    'md-icon-option': MdIconOption,
  },
};
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
  align-items: center;
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
  align-self: flex-end;
}

.video-overlay {
  display: inline-block;
  margin-bottom: 15px;
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
  margin-left: 12px;
  margin-right: -8px;
  align-self: flex-end;
}

.show-video-overlay-btn .md-icon {
  color: #9E9E9E!important;
}

.collapsed-timeline-icon {
  cursor: pointer;
}

.show-video-overlay-btn.active .md-icon {
  color: #212121!important;
}

.help {
  display: flex;
  align-content: flex-end;
  align-items: flex-end;
  flex-direction: column;
}

.help-icon-wrapper {
  margin-right: 20px;
  margin-bottom: 10px;
}

.help-icon-wrapper .help-icon {
  cursor: help;
}

.trace-icon {
  cursor: pointer;
  user-select: none;
}

.trace-icon.disabled {
  color: gray;
}

.active-timeline {
  flex: 0 0 auto;
}

.active-timeline .icon {
  margin-right: 20px;
}

.active-timeline .active-timeline-icon {
  margin-right: 10px;
  align-self: flex-end;
  margin-bottom: 3px;
}

.minimized-timeline-content {
  align-self: flex-start;
  padding-top: 1px;
}

.minimized-timeline-content label {
  color: rgba(0,0,0,0.54);
  font-size: 12px;
  font-family: inherit;
}

.minimized-timeline-content .minimized-timeline {
  margin-top: 4px;
}

.nagivation-style-selection-field {
  width: 90px;
  margin-right: 10px;
  margin-bottom: 0;
}

</style>
