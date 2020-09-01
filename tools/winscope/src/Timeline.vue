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
  <svg
    width="100%"
    height="20"
    class="timeline-svg"
    :class="{disabled: disabled}"
    ref="timeline"
  >
    <rect
      :x="`${block.startPos}%`"
      y="0"
      :width="`${block.width}%`"
      :height="pointHeight"
      :rx="corner"
      v-for="(block, idx) in timelineBlocks"
      :key="idx"
      @click="onBlockClick"
      class="point"
    />
    <rect
      :x="`${position(selected)}%`"
      y="0"
      :width="`${pointWidth}%`"
      :height="pointHeight"
      :rx="corner"
      class="point selected"
    />
  </svg>
</template>
<script>
import TimelineMixin from "./mixins/Timeline.js";

export default {
  name: "timeline",
  // TODO: Add indication of trim, at least for collasped timeline
  props: ["selectedIndex", "crop", "disabled"],
  data() {
    return {
      pointHeight: 15,
      corner: 2
    };
  },
  mixins: [TimelineMixin],
  methods: {},
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
  }
};
</script>
<style scoped>
.timeline-svg .point {
  cursor: pointer;
}
.timeline-svg.disabled .point {
  fill: #BDBDBD;
  cursor: not-allowed;
}
.timeline-svg:not(.disabled) .point.selected {
  fill: rgb(240, 59, 59);
}
.timeline-svg.disabled .point.selected {
  fill: rgba(240, 59, 59, 0.596);
}
</style>
