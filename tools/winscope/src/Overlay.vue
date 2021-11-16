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
          <searchbar
            class="search-bar"
            v-if="search"
            :searchTypes="searchTypes"
            :store="store"
            :presentTags="Object.freeze(presentTags)"
            :presentErrors="Object.freeze(presentErrors)"
            :timeline="mergedTimeline.timeline"
          />
          <md-toolbar
            md-elevation="0"
            class="md-transparent">

            <md-button
              @click="toggleSearch()"
              class="drop-search"
            >
              Toggle search bar
            </md-button>

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
                  @click="$refs.navigationTypeSelection.$el
                          .querySelector('input').click()"
                >
                  <md-icon class="collapsed-timeline-icon">
                    {{ collapsedTimelineIcon }}
                    <md-tooltip>
                      {{ collapsedTimelineIconTooltip }}
                    </md-tooltip>
                  </md-icon>
                </div>

                <md-field
                  v-if="multipleTraces"
                  ref="navigationTypeSelection"
                  class="navigation-style-selection-field"
                >

                  <label>Navigation</label>
                  <md-select
                    v-model="navigationStyle"
                    name="navigationStyle"
                    md-dense
                  >
                    <md-icon-option
                      :value="NAVIGATION_STYLE.GLOBAL"
                      icon="public"
                      desc="Consider all timelines for navigation"
                    />
                    <md-icon-option
                      :value="NAVIGATION_STYLE.FOCUSED"
                      :icon="TRACE_ICONS[focusedFile.type]"
                      :desc="`Automatically switch what timeline is considered
                        for navigation based on what is visible on screen.
                        Currently ${focusedFile.type}.`"
                    />
                    <!-- TODO: Add edit button for custom settings that opens
                               popup dialog menu -->
                    <md-icon-option
                      :value="NAVIGATION_STYLE.CUSTOM"
                      icon="dashboard_customize"
                      desc="Considers only the enabled timelines for
                            navigation. Expand the bottom bar to toggle
                            timelines."
                    />
                    <md-optgroup label="Targeted">
                      <md-icon-option
                        v-for="file in timelineFiles"
                        v-bind:key="file.type"
                        :value="`${NAVIGATION_STYLE.TARGETED}-` +
                                `${file.type}`"
                        :displayValue="file.type"
                        :shortValue="NAVIGATION_STYLE.TARGETED"
                        :icon="TRACE_ICONS[file.type]"
                        :desc="`Only consider ${file.type} ` +
                               'for timeline navigation.'"
                      />
                    </md-optgroup>
                  </md-select>
                </md-field>
              </div>

              <div
                class="minimized-timeline-content"
                v-show="minimized"
                v-if="hasTimeline"
              >
                <input
                  class="timestamp-search-input"
                  v-model="searchInput"
                  spellcheck="false"
                  :placeholder="seekTime"
                  @focus="updateInputMode(true)"
                  @blur="updateInputMode(false)"
                  @keyup.enter="updateSearchForTimestamp"
                />
                <timeline
                  :store="store"
                  :flickerMode="flickerMode"
                  :tags="Object.freeze(presentTags)"
                  :errors="Object.freeze(presentErrors)"
                  :timeline="Object.freeze(minimizedTimeline.timeline)"
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
                style="margin-bottom: 10px;"
              >
                <i class="md-icon md-icon-font">
                  featured_video
                </i>
                <md-tooltip md-direction="top">
                  <span v-if="showVideoOverlay">Hide video overlay</span>
                  <span v-else>Show video overlay</span>
                </md-tooltip>
              </md-button>

              <md-button
                class="md-icon-button toggle-btn"
                @click="toggle"
                style="margin-bottom: 10px;"
              >
                <md-icon v-if="minimized">
                  expand_less
                  <md-tooltip md-direction="top" @click="recordButtonClickedEvent(`Expand Timeline`)">Expand timeline</md-tooltip>
                </md-icon>
                <md-icon v-else>
                  expand_more
                  <md-tooltip md-direction="top" @click="recordButtonClickedEvent(`Collapse Timeline`)">Collapse timeline</md-tooltip>
                </md-icon>
              </md-button>
            </div>
          </md-toolbar>

          <div class="expanded-content" v-show="expanded">
            <div :v-if="video">
              <div
                class="expanded-content-video"
                ref="expandedContentVideoContainer"
              >
                <!-- Video moved here on expansion -->
              </div>
            </div>
            <div class="flex-fill">
              <div
                ref="expandedTimeline"
                :style="`padding-top: ${resizeOffset}px;`"
              >
                <div class="seek-time" v-if="seekTime">
                  <b>Seek time: </b>
                  <input
                    class="timestamp-search-input"
                    :class="{ expanded: expanded }"
                    v-model="searchInput"
                    spellcheck="false"
                    :placeholder="seekTime"
                    @focus="updateInputMode(true)"
                    @blur="updateInputMode(false)"
                    @keyup.enter="updateSearchForTimestamp"
                  />
                </div>

                <timelines
                  :timelineFiles="timelineFiles"
                  :scale="scale"
                  :crop="crop"
                  :cropIntent="cropIntent"
                  v-on:crop="onTimelineCrop"
                />

                <div class="timeline-selection">
                  <div class="timeline-selection-header">
                    <label>Timeline Area Selection</label>
                    <span class="material-icons help-icon">
                      help_outline
                      <md-tooltip md-direction="right">
                        Select the area of the timeline to focus on.
                        Click and drag to select.
                      </md-tooltip>
                    </span>
                    <md-button
                      class="md-primary"
                      v-if="isCropped"
                      @click.native="clearSelection"
                    >
                      Clear selection
                    </md-button>
                  </div>
                  <timeline-selection
                    :timeline="mergedTimeline.timeline"
                    :start-timestamp="0"
                    :end-timestamp="0"
                    :scale="scale"
                    :cropArea="crop"
                    v-on:crop="onTimelineCrop"
                    v-on:cropIntent="onTimelineCropIntent"
                    v-on:showVideoAt="changeVideoTimestamp"
                    v-on:resetVideoTimestamp="resetVideoTimestamp"
                  />
                </div>

                <div class="help" v-if="!minimized">
                  <div class="help-icon-wrapper">
                    <span class="material-icons help-icon">
                      help_outline
                      <md-tooltip md-direction="left">
                        Click on icons to disable timelines
                      </md-tooltip>
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
import Timelines from './Timelines.vue';
import TimelineSelection from './TimelineSelection.vue';
import DraggableDiv from './DraggableDiv.vue';
import VideoView from './VideoView.vue';
import MdIconOption from './components/IconSelection/IconSelectOption.vue';
import Searchbar from './Searchbar.vue';
import FileType from './mixins/FileType.js';
import {NAVIGATION_STYLE} from './utils/consts';
import {TRACE_ICONS} from '@/decode.js';

// eslint-disable-next-line camelcase
import {nanos_to_string, getClosestTimestamp} from './transform.js';

export default {
  name: 'overlay',
  props: ['store', 'presentTags', 'presentErrors', 'searchTypes'],
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
      cropIntent: null,
      TRACE_ICONS,
      search: false,
      searchInput: "",
      isSeekTimeInputMode: false,
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
    this.updateInputMode(false);
  },
  watch: {
    navigationStyle(style) {
      // Only store navigation type in local store if it's a type that will
      // work regardless of what data is loaded.
      if (style === NAVIGATION_STYLE.GLOBAL ||
        style === NAVIGATION_STYLE.FOCUSED) {
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
      const mx = Math.max(...(this.timelineFiles.map((f) =>
        Math.max(...f.timeline))));
      const mi = Math.min(...(this.timelineFiles.map((f) =>
        Math.min(...f.timeline))));
      return [mi, mx];
    },
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    hasTimeline() {
      // Returns true if a meaningful timeline exists (i.e. not only dumps)
      for (const file of this.timelineFiles) {
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
          return `Focused: ${this.focusedFile.type}`;

        case NAVIGATION_STYLE.CUSTOM:
          return 'Enabled timelines';

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            console.warn('Unexpected navigation type; fallback to global');
            return 'All timelines';
          }

          const fileType = split[1];

          return fileType;
      }
    },
    collapsedTimelineIcon() {
      switch (this.navigationStyle) {
        case NAVIGATION_STYLE.GLOBAL:
          return 'public';

        case NAVIGATION_STYLE.FOCUSED:
          return TRACE_ICONS[this.focusedFile.type];

        case NAVIGATION_STYLE.CUSTOM:
          return 'dashboard_customize';

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            console.warn('Unexpected navigation type; fallback to global');
            return 'public';
          }

          const fileType = split[1];

          return TRACE_ICONS[fileType];
      }
    },
    minimizedTimeline() {
      if (this.navigationStyle === NAVIGATION_STYLE.GLOBAL) {
        return this.mergedTimeline;
      }

      if (this.navigationStyle === NAVIGATION_STYLE.FOCUSED) {
        //dumps do not have a timeline, so if scrolling over a dump, show merged timeline
        if (this.focusedFile.timeline) {
          return this.focusedFile;
        }
        return this.mergedTimeline;
      }

      if (this.navigationStyle === NAVIGATION_STYLE.CUSTOM) {
        // TODO: Return custom timeline
        return this.mergedTimeline;
      }

      if (
        this.navigationStyle.split('-').length >= 2
        && this.navigationStyle.split('-')[0] === NAVIGATION_STYLE.TARGETED
      ) {
        return this.$store.state
            .traces[this.navigationStyle.split('-')[1]];
      }

      console.warn('Unexpected navigation type; fallback to global');
      return this.mergedTimeline;
    },
    isCropped() {
      return this.crop != null &&
        (this.crop.left !== 0 || this.crop.right !== 1);
    },
    multipleTraces() {
      return this.timelineFiles.length > 1;
    },
    flickerMode() {
      return this.presentTags.length>0 || this.presentErrors.length>0;
    },
  },
  updated() {
    this.$nextTick(() => {
      if (this.$refs.expandedTimeline && this.expanded) {
        this.videoHeight = this.$refs.expandedTimeline.clientHeight;
      } else {
        this.videoHeight = 'auto';
      }
    });
  },
  methods: {
    toggleSearch() {
      this.search = !(this.search);
      this.recordButtonClickedEvent("Toggle Search Bar");
    },
    /**
     * determines whether left/right arrow keys should move cursor in input field
     * and upon click of input field, fills with current timestamp
     */
    updateInputMode(isInputMode) {
      this.isSeekTimeInputMode = isInputMode;
      this.store.isInputMode = isInputMode;
      if (!isInputMode) {
        this.searchInput = "";
      } else {
        this.searchInput = this.seekTime;
      }
    },
    /** Navigates to closest timestamp in timeline to search input*/
    updateSearchForTimestamp() {
      const closestTimestamp = getClosestTimestamp(this.searchInput, this.mergedTimeline.timeline);
      this.$store.dispatch("updateTimelineTime", closestTimestamp);
      this.updateInputMode(false);
      this.recordNewEvent("Searching for timestamp")
    },

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

      var timelineToAdvance = 0;
      while (timelineToAdvance !== undefined) {
        timelineToAdvance = undefined;
        let minTime = Infinity;

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

      // Object is frozen for performance reasons
      // It will prevent Vue from making it a reactive object which will be very
      // slow as the timeline gets larger.
      Object.freeze(mergedTimeline.timeline);

      return mergedTimeline;
    },
    toggle() {
      this.minimized ? this.expand() : this.minimize();

      this.minimized = !this.minimized;
    },
    expand() {
      if (this.video) {
        this.$refs.expandedContentVideoContainer
            .appendChild(this.$refs.video.$el);
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
      this.recordButtonClickedEvent("Close Video Overlay")
    },
    openVideoOverlay() {
      this.showVideoOverlay = true;
      this.recordButtonClickedEvent("Open Video Overlay")
    },
    toggleVideoOverlay() {
      this.showVideoOverlay = !this.showVideoOverlay;
      this.recordButtonClickedEvent("Toggle Video Overlay")
    },
    videoLoaded() {
      this.$refs.videoOverlay.contentLoaded();
    },
    updateNavigationFileFilter() {
      if (!this.minimized) {
        // Always use custom mode navigation when timeline is expanded
        this.$store.commit('setNavigationFilesFilter',
            (f) => !f.timelineDisabled);
        return;
      }

      let navigationStyleFilter;
      switch (this.navigationStyle) {
        case NAVIGATION_STYLE.GLOBAL:
          navigationStyleFilter = (f) => true;
          break;

        case NAVIGATION_STYLE.FOCUSED:
          navigationStyleFilter =
            (f) => f.type === this.focusedFile.type;
          break;

        case NAVIGATION_STYLE.CUSTOM:
          navigationStyleFilter = (f) => !f.timelineDisabled;
          break;

        default:
          const split = this.navigationStyle.split('-');
          if (split[0] !== NAVIGATION_STYLE.TARGETED) {
            console.warn('Unexpected navigation type; fallback to global');
            navigationStyleFilter = (f) => true;
            break;
          }

          const fileType = split[1];
          navigationStyleFilter =
            (f) => f.type === fileType;
      }
      this.recordChangedNavigationStyleEvent(this.navigationStyle);
      this.$store.commit('setNavigationFilesFilter', navigationStyleFilter);
    },
    updateVideoOverlayWidth(width) {
      this.videoOverlayExtraWidth = width;
    },
    onTimelineCrop(cropDetails) {
      this.crop = cropDetails;
    },
    onTimelineCropIntent(cropIntent) {
      this.cropIntent = cropIntent;
    },
    changeVideoTimestamp(ts) {
      if (!this.$refs.video) {
        return;
      }
      this.$refs.video.selectFrameAtTime(ts);
    },
    resetVideoTimestamp() {
      if (!this.$refs.video) {
        return;
      }
      this.$refs.video.jumpToSelectedIndex();
    },
    clearSelection() {
      this.crop = null;
    },
  },
  components: {
    'timeline': Timeline,
    'timelines': Timelines,
    'timeline-selection': TimelineSelection,
    'videoview': VideoView,
    'draggable-div': DraggableDiv,
    'md-icon-option': MdIconOption,
    'searchbar': Searchbar,
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
  z-index: 10;
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
  margin-bottom: 18px;
}

.minimized-timeline-content {
  align-self: flex-start;
  padding-top: 1px;
}

.minimized-timeline-content label {
  color: rgba(0,0,0,0.54);
  font-size: 12px;
  font-family: inherit;
  cursor: text;
}

.minimized-timeline-content .minimized-timeline {
  margin-top: 4px;
}

.navigation-style-selection-field {
  width: 90px;
  margin-right: 10px;
  margin-bottom: 0;
}

.timeline-selection-header {
  display: flex;
  align-items: center;
  padding-left: 15px;
  height: 48px;
}

.help-icon {
  font-size: 15px;
  margin-bottom: 15px;
  cursor: help;
}

.timestamp-search-input {
  outline: none;
  border-width: 0 0 1px;
  border-color: gray;
  font-family: inherit;
  color: #448aff;
  font-size: 12px;
  padding: 0;
  letter-spacing: inherit;
  width: 125px;
}

.timestamp-search-input:focus {
  border-color: #448aff;
}

.timestamp-search-input.expanded {
  font-size: 14px;
  width: 150px;
}

.drop-search:hover {
  background-color: #9af39f;
}
</style>
