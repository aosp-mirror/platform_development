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
  <div class="timeline-container">
    <div class="tag-timeline" v-if="flickerMode" :style="maxOverlap">
      <transition-container
        class="container"
        v-for="transition in timelineTransitions"
        :key="transition.type"
        :startPos="transition.startPos"
        :startTime="transition.startTime"
        :endTime="transition.endTime"
        :width="transition.width"
        :color="transition.color"
        :overlap="transition.overlap"
        :tooltip="transition.tooltip"
        :store="store"
      />
    </div>
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
      <line
        v-for="error in errorPositions"
        :key="error.pos"
        :x1="`${error.pos}%`"
        :x2="`${error.pos}%`"
        y1="0"
        y2="18px"
        class="error"
        @click="onErrorClick(error.ts)"
      />
    </svg>
  </div>
</template>
<script>
import TimelineMixin from "./mixins/Timeline.js";
import TransitionContainer from './components/TagDisplay/TransitionContainer.vue';

export default {
  name: "timeline",
  // TODO: Add indication of trim, at least for collasped timeline
  components: {
    'transition-container': TransitionContainer,
  },
  props: ["selectedIndex", "crop", "disabled", "store"],
  data() {
    return {
      pointHeight: 15,
      corner: 2
    };
  },
  mixins: [TimelineMixin],
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
    maxOverlap() {
      if (!this.timelineTransitions) {
        return {
          marginTop: '0px',
        }
      }
      var overlaps = [];
      for (const transition in this.timelineTransitions) {
        overlaps.push(this.timelineTransitions[transition].overlap);
      }
      return {
        marginTop: (Math.max(...overlaps)+1)*10 + 'px',
      }
    },
  }
};
</script>
<style scoped>
.timeline-container {
  width: 100%;
}
.container:hover {
  cursor: pointer;
}
.tag-timeline {
  width: 100%;
  position: relative;
  height: 10px;
}
.timeline-svg .point {
  cursor: pointer;
}
.timeline-svg.disabled .point {
  fill: #BDBDBD;
  cursor: not-allowed;
}
.timeline-svg:not(.disabled) .point.selected {
  fill: #b2f6faff;
}
.timeline-svg.disabled .point.selected {
  fill: rgba(240, 59, 59, 0.596);
}
.error {
  stroke: rgb(255, 0, 0);
  stroke-width: 8px;
  cursor: pointer;
}
</style>