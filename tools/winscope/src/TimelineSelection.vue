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
  <div class="wrapper">
    <svg
      width="100%"
      height="20"
      class="timeline-svg"
      :class="{disabled: disabled}"
      ref="timelineSvg"
    >
      <rect
        :x="position(item)"
        y="0"
        :width="pointWidth"
        :height="pointHeight"
        :rx="corner"
        v-for="item in timeline"
        :key="item"
        class="point"
      />
      <rect
        v-if="selectedWidth >= 0"
        :x="selectionStartPosition"
        y="0"
        :width="selectedWidth"
        :height="pointHeight"
        :rx="corner"
        class="point selection"
        ref="selectedSection"
      />
      <rect
        v-else
        :x="selectionStartPosition + selectedWidth"
        y="0"
        :width="-selectedWidth"
        :height="pointHeight"
        :rx="corner"
        class="point selection"
        ref="selectedSection"
      />

      <rect
        :x="selectionStartPosition - 2"
        y="0"
        :width="4"
        :height="pointHeight"
        :rx="corner"
        class="point selection-edge"
        ref="leftResizeDragger"
      />

      <rect
        :x="selectionStartPosition + selectedWidth - 2"
        y="0"
        :width="4"
        :height="pointHeight"
        :rx="corner"
        class="point selection-edge"
        ref="rightResizeDragger"
      />
    </svg>
  </div>
</template>
<script>
export default {
  name: "timelineSelection",
  props: ["timeline", "startTimestamp",  "endTimestamp", "scale", "disabled"],
  data() {
    return {
      pointWidth: "1%",
      pointHeight: 15,
      corner: 2,
      selectionStartPosition: 0,
      selectionEndPosition: 0,
    };
  },
  watch: {
    selectionStartPosition() {
      this.emitCropDetails();
    },
    selectionEndPosition() {
      this.emitCropDetails();
    }
  },
  methods: {
    position(item) {
      return this.translate(item);
    },
    translate(cx) {
      var scale = [...this.scale];
      if (scale[0] >= scale[1]) {
        return cx;
      }

      return (((cx - scale[0]) / (scale[1] - scale[0])) * 100 - (1 /*pointWidth*/)) + "%";
    },
    emitCropDetails() {
      const width = this.$refs.timelineSvg.clientWidth;
      this.$emit('crop', {
        left: this.selectionStartPosition / width,
        right: this.selectionEndPosition / width
      });
    },

    setupCreateSelectionListeners() {
      this.timelineSvgMouseDownEventListener = e => {
        e.stopPropagation();
        this.selecting = true;
        this.dragged = false;
        this.mouseDownX = e.offsetX;
        this.mouseDownClientX = e.clientX;

        document.body.style.cursor = "crosshair";
      };

      this.createSelectionMouseMoveEventListener = e => {
        if (this.selecting) {
          if (!this.dragged) {
            this.selectionStartX = this.mouseDownX;
          }

          this.dragged = true;
          const draggedAmount =  e.clientX - this.mouseDownClientX;

          if (draggedAmount >= 0) {
            this.selectionStartPosition = this.selectionStartX;

            const endX = this.selectionStartX + draggedAmount;
            if (endX <= this.$refs.timelineSvg.clientWidth) {
              this.selectionEndPosition = endX;
            } else {
              this.selectionEndPosition = this.$refs.timelineSvg.clientWidth;
            }
          } else {
            this.selectionEndPosition = this.selectionStartX;

            const startX = this.selectionStartX + draggedAmount;
            if (startX >= 0) {
              this.selectionStartPosition = startX;
            } else {
              this.selectionStartPosition = 0;
            }
          }
        }
      }

      this.createSelectionMouseUpEventListener = e => {
        this.selecting = false;
        document.body.style.cursor = null;
      };

      this.$refs.timelineSvg
        .addEventListener('mousedown', this.timelineSvgMouseDownEventListener);
      document
        .addEventListener('mousemove', this.createSelectionMouseMoveEventListener);
      document
        .addEventListener('mouseup', this.createSelectionMouseUpEventListener);
    },

    teardownCreateSelectionListeners() {
      this.$refs.timelineSvg
        .removeEventListener('mousedown', this.timelineSvgMouseDownEventListener);
      document
        .removeEventListener('mousemove', this.createSelectionMouseMoveEventListener);
      document
        .removeEventListener('mouseup', this.createSelectionMouseUpEventListener);
    },

    setupDragSelectionListeners() {
      this.selectedSectionMouseDownListener = e => {
        e.stopPropagation();
        this.draggingSelection = true;
        this.draggingSelectionStartX = e.clientX;
        this.draggingSelectionStartPos = this.selectionStartPosition;
        this.draggingSelectionEndPos = this.selectionEndPosition;

        document.body.style.cursor = "move";
      };

      this.dragSelectionMouseMoveEventListener = e => {
        if (this.draggingSelection) {
          const dragAmount = e.clientX - this.draggingSelectionStartX;

          const newStartPos = this.draggingSelectionStartPos + dragAmount;
          const newEndPos = this.draggingSelectionEndPos + dragAmount;
          if (newStartPos >= 0 && newEndPos <= this.$refs.timelineSvg.clientWidth) {
            this.selectionStartPosition = newStartPos;
            this.selectionEndPosition = newEndPos;
          } else {
            if (newStartPos < 0) {
              this.selectionStartPosition = 0;
              this.selectionEndPosition = newEndPos - (newStartPos /*negative overflown amount*/);
            } else {
              const overflownAmount = newEndPos - this.$refs.timelineSvg.clientWidth;
              this.selectionEndPosition = this.$refs.timelineSvg.clientWidth;
              this.selectionStartPosition = newStartPos - overflownAmount;
            }
          }
        }
      }

      this.dragSelectionMouseUpEventListener = e => {
        this.draggingSelection = false;
        document.body.style.cursor = null;
      };

      this.$refs.selectedSection
        .addEventListener('mousedown', this.selectedSectionMouseDownListener);
      document
        .addEventListener('mousemove', this.dragSelectionMouseMoveEventListener);
      document
        .addEventListener('mouseup', this.dragSelectionMouseUpEventListener);
    },

    teardownDragSelectionListeners() {
      this.$refs.selectedSection
        .removeEventListener('mousedown', this.selectedSectionMouseDownListener);
       document
        .removeEventListener('mousemove', this.dragSelectionMouseMoveEventListener);
      document
        .removeEventListener('mouseup', this.dragSelectionMouseUpEventListener);
    },

    setupResizeSelectionListeners() {
       this.leftResizeDraggerMouseDownEventListener = e => {
        e.stopPropagation();
        this.resizeingLeft = true;
        this.resizeStartX = e.clientX;
        this.resizeStartPos = this.selectionStartPosition;

        document.body.style.cursor = "ew-resize";
      };

      this.rightResizeDraggerMouseDownEventListener = e => {
        e.stopPropagation();
        this.resizeingRight = true;
        this.resizeStartX = e.clientX;
        this.resizeEndPos = this.selectionEndPosition;

        document.body.style.cursor = "ew-resize";
      };

      this.resizeMouseMoveEventListener = e => {
        if (this.resizeingLeft) {
          const moveAmount = e.clientX - this.resizeStartX;
          let newStartPos = this.resizeStartPos + moveAmount;
          if (newStartPos >= this.selectionEndPosition) {
            newStartPos = this.selectionEndPosition;
          }
          if (newStartPos < 0) {
            newStartPos = 0;
          }

          this.selectionStartPosition = newStartPos;
        }

        if (this.resizeingRight) {
          const moveAmount = e.clientX - this.resizeStartX;
          let newEndPos = this.resizeEndPos + moveAmount;
          if (newEndPos <= this.selectionStartPosition) {
            newEndPos = this.selectionStartPosition;
          }
          if (newEndPos > this.$refs.timelineSvg.clientWidth) {
            newEndPos = this.$refs.timelineSvg.clientWidth;
          }

          this.selectionEndPosition = newEndPos;
        }
      };

      this.resizeSelectionMouseUpEventListener = e => {
        this.resizeingLeft = false;
        this.resizeingRight = false;
        document.body.style.cursor = null;
      }

      document.body.style.cursor = null;

      this.$refs.leftResizeDragger
        .addEventListener('mousedown', this.leftResizeDraggerMouseDownEventListener);
      this.$refs.rightResizeDragger
        .addEventListener('mousedown', this.rightResizeDraggerMouseDownEventListener);
      document
        .addEventListener('mousemove', this.resizeMouseMoveEventListener);
      document
        .addEventListener('mouseup', this.resizeSelectionMouseUpEventListener);
    },

    teardownResizeSelectionListeners() {
      this.$refs.leftResizeDragger
        .removeEventListener('mousedown', this.leftResizeDraggerMouseDownEventListener);
      this.$refs.rightResizeDragger
        .removeEventListener('mousedown', this.rightResizeDraggerMouseDownEventListener);
      document
        .removeEventListener('mousemove', this.resizeMouseMoveEventListener);
      document
        .removeEventListener('mouseup', this.resizeSelectionMouseUpEventListener);
    },
  },
  computed: {
    timestamps() {
      if (this.timeline.length == 1) {
        return [0];
      }
      return this.timeline;
    },
    selected() {
      return this.timeline[this.selectedIndex];
    },
    selectedWidth() {
      return this.selectionEndPosition - this.selectionStartPosition;
    }
  },
  mounted() {
    this.setupCreateSelectionListeners();
    this.setupDragSelectionListeners();
    this.setupResizeSelectionListeners();
  },
  beforeDestroy() {
    this.teardownCreateSelectionListeners();
    this.teardownDragSelectionListeners();
    this.teardownResizeSelectionListeners();
  },
};
</script>
<style scoped>
.wrapper {
  padding: 0 15px;
}

.timeline-svg {
  cursor: crosshair;
}
.timeline-svg .point {
  fill: #BDBDBD;
}
.timeline-svg .point.selection {
  fill: rgba(240, 59, 59, 0.596);
  cursor: move;
}

.timeline-svg .point.selection-edge {
  fill: rgba(27, 123, 212, 0.596);
  cursor: ew-resize;
}
</style>
