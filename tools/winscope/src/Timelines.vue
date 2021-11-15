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
  <div class="timelines-container">

    <div class="timeline-icons" @mousedown="mousedownHandler">
      <div
        v-for="file in timelineFiles"
        :key="file.filename"
        class="trace-icon"
        :class="{disabled: file.timelineDisabled}"
        @click="toggleTimeline(file)"
        style="cursor: pointer;"
      >
        <i class="material-icons">
          {{ TRACE_ICONS[file.type] }}
          <md-tooltip md-direction="bottom">{{ file.type }}</md-tooltip>
        </i>
      </div>
    </div>

    <div class="timelines-wrapper" ref="timelinesWrapper">
      <md-list class="timelines" @mousedown="mousedownHandler" ref="timelines">
        <md-list-item
        v-for="file in timelineFiles"
        :key="file.filename"
        >
          <timeline
            :timeline="Object.freeze(file.timeline)"
            :selected-index="file.selectedIndex"
            :scale="scale"
            :crop="crop"
            :disabled="file.timelineDisabled"
            class="timeline"
          />
        </md-list-item>
      </md-list>

      <div
        class="selection"
        :style="selectionStyle"
      />

      <div
        v-show="this.cropIntent"
        class="selection-intent"
        :style="selectionIntentStyle"
      />
    </div>
  </div>
</template>
<script>
import Timeline from './Timeline.vue';
import {TRACE_ICONS} from '@/decode.js';

export default {
  name: 'Timelines',
  props: ['timelineFiles', 'scale', 'crop', 'cropIntent'],
  data() {
    return {
      // Distances of sides from top left corner of wrapping div in pixels
      selectionPosition: {
        top: 0,
        left: 0,
        bottom: 0,
        right: 0,
      },
      TRACE_ICONS,
    };
  },
  computed: {
    /**
     * Used to check whether or not a selection box should be displayed.
     * @return {bool} true if any of the positions are non nullish values
     */
    isEmptySelection() {
      return this.selectionPosition.top ||
        this.selectionPosition.left ||
        this.selectionPosition.bottom ||
        this.selectionPosition.right;
    },
    /**
     * Generates the style of the selection box.
     * @return {object} an object containing the style of the selection box.
     */
    selectionStyle() {
      return {
        top: `${this.selectionPosition.top}px`,
        left: `${this.selectionPosition.left}px`,
        height:
          `${this.selectionPosition.bottom - this.selectionPosition.top}px`,
        width:
          `${this.selectionPosition.right - this.selectionPosition.left}px`,
      };
    },
    /**
     * Generates the dynamic style of the selection intent box.
     * @return {object} an object containing the style of the selection intent
     *                  box.
     */
    selectionIntentStyle() {
      if (!(this.cropIntent && this.$refs.timelinesWrapper)) {
        return {
          left: 0,
          width: 0,
        };
      }

      const activeCropLeft = this.crop?.left ?? 0;
      const activeCropRight = this.crop?.right ?? 1;
      const timelineWidth =
        this.$refs.timelinesWrapper.getBoundingClientRect().width;

      const r = timelineWidth / (activeCropRight - activeCropLeft);

      let left = 0;
      let boderLeft = 'none';
      if (this.cropIntent.left > activeCropLeft) {
        left = (this.cropIntent.left - activeCropLeft) * r;
        boderLeft = null;
      }

      let right = timelineWidth;
      let borderRight = 'none';
      if (this.cropIntent.right < activeCropRight) {
        right = timelineWidth - (activeCropRight - this.cropIntent.right) * r;
        borderRight = null;
      }

      return {
        'left': `${left}px`,
        'width': `${right - left}px`,
        'border-left': boderLeft,
        'border-right': borderRight,
      };
    },
  },
  methods: {
    /**
     * Adds an overlay to make sure element selection can't happen and the
     * crosshair cursor style is maintained wherever the curso is on the screen
     * while a selection is taking place.
     */
    addOverlay() {
      if (this.overlay) {
        return;
      }

      this.overlay = document.createElement('div');
      Object.assign(this.overlay.style, {
        'position': 'fixed',
        'top': 0,
        'left': 0,
        'height': '100vh',
        'width': '100vw',
        'z-index': 10,
        'cursor': 'crosshair',
      });

      document.body.appendChild(this.overlay);
    },

    /**
     * Removes the overlay that is added by a call to addOverlay.
     */
    removeOverlay() {
      if (!this.overlay) {
        return;
      }

      document.body.removeChild(this.overlay);
      delete this.overlay;
    },

    /**
     * Generates an object that can is used to update the position and style of
     * the selection box when a selection is being made. The object contains
     * three functions which all take a DOM event as a parameter.
     *
     * - init: setup the initial drag position of the selection base on the
     *         mousedown event
     * - update: updates the selection box's coordinates based on the mousemouve
     *           event
     * - reset: clears the selection box, shold be called when the mouseup event
     *          occurs or when we want to no longer display the selection box.
     * @return {null}
     */
    selectionPositionsUpdater() {
      let startClientX; let startClientY; let x; let y;

      return {
        init: (e) => {
          startClientX = e.clientX;
          startClientY = e.clientY;
          x = startClientX -
            this.$refs.timelines.$el.getBoundingClientRect().left;
          y = startClientY -
            this.$refs.timelines.$el.getBoundingClientRect().top;
        },
        update: (e) => {
          let left; let right; let top; let bottom;

          const xDiff = e.clientX - startClientX;
          if (xDiff > 0) {
            left = x;
            right = x + xDiff;
          } else {
            left = x + xDiff;
            right = x;
          }

          const yDiff = e.clientY - startClientY;
          if (yDiff > 0) {
            top = y;
            bottom = y + yDiff;
          } else {
            top = y + yDiff;
            bottom = y;
          }

          if (left < 0) {
            left = 0;
          }
          if (top < 0) {
            top = 0;
          }
          if (right > this.$refs.timelines.$el.getBoundingClientRect().width) {
            right = this.$refs.timelines.$el.getBoundingClientRect().width;
          }

          if (bottom >
            this.$refs.timelines.$el.getBoundingClientRect().height) {
            bottom = this.$refs.timelines.$el.getBoundingClientRect().height;
          }

          this.$set(this.selectionPosition, 'left', left);
          this.$set(this.selectionPosition, 'right', right);
          this.$set(this.selectionPosition, 'top', top);
          this.$set(this.selectionPosition, 'bottom', bottom);
        },
        reset: (e) => {
          this.$set(this.selectionPosition, 'left', 0);
          this.$set(this.selectionPosition, 'right', 0);
          this.$set(this.selectionPosition, 'top', 0);
          this.$set(this.selectionPosition, 'bottom', 0);
        },
      };
    },

    /**
     * Handles the mousedown event indicating the start of a selection.
     * Adds listeners to handles mousemove and mouseup event to detect the
     * selection and update the selection box's coordinates.
     * @param {event} e
     */
    mousedownHandler(e) {
      const selectionPositionsUpdater = this.selectionPositionsUpdater();
      selectionPositionsUpdater.init(e);

      let dragged = false;

      const mousemoveHandler = (e) => {
        if (!dragged) {
          dragged = true;
          this.addOverlay();
        }

        selectionPositionsUpdater.update(e);
      };
      document.addEventListener('mousemove', mousemoveHandler);

      const mouseupHandler = (e) => {
        document.removeEventListener('mousemove', mousemoveHandler);
        document.removeEventListener('mouseup', mouseupHandler);

        if (dragged) {
          this.removeOverlay();
          selectionPositionsUpdater.update(e);
          this.zoomToSelection();
        }
        selectionPositionsUpdater.reset();
      };
      document.addEventListener('mouseup', mouseupHandler);
    },

    /**
     * Update the crop values to zoom into the timeline based on the currently
     * set selection box coordinates.
     */
    zoomToSelection() {
      const left = this.crop?.left ?? 0;
      const right = this.crop?.right ?? 1;

      const ratio =
        (this.selectionPosition.right - this.selectionPosition.left) /
        this.$refs.timelines.$el.getBoundingClientRect().width;

      const newCropWidth = ratio * (right - left);
      const newLeft = left + (this.selectionPosition.left /
        this.$refs.timelines.$el.getBoundingClientRect().width) *
          (right - left);

      if (this.crop) {
        this.$set(this.crop, 'left', newLeft);
        this.$set(this.crop, 'right', newLeft + newCropWidth);
      } else {
        this.$emit('crop', {
          left: newLeft,
          right: newLeft + newCropWidth,
        });
      }
    },

    toggleTimeline(file) {
      this.$set(file, 'timelineDisabled', !file.timelineDisabled);
    },
  },
  components: {
    Timeline,
  },
};
</script>
<style scoped>
.timelines-container {
  display: flex;
}

.timelines-container .timelines-wrapper {
  flex-grow: 1;
  cursor: crosshair;
  position: relative;
}

.timelines-wrapper {
  overflow: hidden;
}

.selection, .selection-intent {
  position: absolute;
  z-index: 10;
  background: rgba(255, 36, 36, 0.5);
  pointer-events: none;
}

.selection-intent {
  top: 0;
  height: 100%;
  margin-left: -3px;
  border-left: 3px #1261A0 solid;
  border-right: 3px #1261A0 solid;
}

.timeline-icons {
  display: flex;
  flex-direction: column;
  justify-content: space-evenly;
  margin-left: 15px;
}

.trace-icon.disabled {
  color: gray;
}
</style>
