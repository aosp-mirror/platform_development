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

    <md-list class="timeline-icons" @mousedown="mousedownHandler">
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
          {{ file.type.icon }}
          <md-tooltip md-direction="bottom">{{ file.type.name }}</md-tooltip>
          </i>
        </div>
      </md-list-item>
    </md-list>

    <div class="timelines-wrapper">
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
    </div>
  </div>
</template>
<script>
import Timeline from "./Timeline.vue";

export default {
  name: "Timelines",
  props: ["timelineFiles", "scale", "crop"],
  data() {
    return {
      // Distances of sides from top left corner of wrapping div in pixels
      selectionPosition: {
        top: 0,
        left: 0,
        bottom: 0,
        right: 0,
      }
    }
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
        height: `${this.selectionPosition.bottom - this.selectionPosition.top}px`,
        width: `${this.selectionPosition.right - this.selectionPosition.left}px`,
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
        position: "fixed",
        top: 0,
        left: 0,
        height: "100vh",
        width: "100vw",
        "z-index": 100,
        cursor: "crosshair",
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
     */
    selectionPositionsUpdater() {
      let startClientX, startClientY, x, y;

      return {
        init: e => {
          startClientX = e.clientX;
          startClientY = e.clientY;
          x = startClientX - this.$refs.timelines.$el.getBoundingClientRect().left;
          y = startClientY - this.$refs.timelines.$el.getBoundingClientRect().top;
        },
        update: e => {
          let left, right, top, bottom;

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

          if (bottom > this.$refs.timelines.$el.getBoundingClientRect().height) {
            bottom = this.$refs.timelines.$el.getBoundingClientRect().height;
          }

          this.$set(this.selectionPosition, "left", left);
          this.$set(this.selectionPosition, "right", right);
          this.$set(this.selectionPosition, "top", top);
          this.$set(this.selectionPosition, "bottom", bottom);
        },
        reset: e => {
          this.$set(this.selectionPosition, "left", 0);
          this.$set(this.selectionPosition, "right", 0);
          this.$set(this.selectionPosition, "top", 0);
          this.$set(this.selectionPosition, "bottom", 0);
        },
      }
    },

    /**
     * Handles the mousedown event indicating the start of a selection.
     * Adds listeners to handles mousemove and mouseup event to detect the
     * selection and update the selection box's coordinates.
     */
    mousedownHandler(e) {
      const selectionPositionsUpdater = this.selectionPositionsUpdater();
      selectionPositionsUpdater.init(e);

      let dragged = false;

      const mousemoveHandler = e => {
        if (!dragged) {
          dragged = true;
          this.addOverlay();
        }

        selectionPositionsUpdater.update(e);
      };
      document.addEventListener('mousemove', mousemoveHandler);

      const mouseupHandler = e => {
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
        this.$refs.timelines.$el.getBoundingClientRect().width) * (right - left);

      if (this.crop) {
        this.$set(this.crop, "left", newLeft);
        this.$set(this.crop, "right", newLeft + newCropWidth);
      } else {
        this.$emit('crop', {
          left: newLeft,
          right: newLeft + newCropWidth,
        })
      }
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

.selection {
  position: absolute;
  z-index: 100;
  background: rgba(255, 36, 36, 0.5);
  pointer-events: none;
}
</style>
