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
  <svg width="100%" height="20" class="timeline-svg" :class="{disabled: disabled}">
    <rect
      :x="position(item)"
      y="0"
      :width="pointWidth"
      :height="pointHeight"
      :rx="corner"
      v-for="(item, idx) in timeline"
      :key="item"
      @click="onItemClick(idx)"
      class="point"
    />
    <rect
      :x="position(selected)"
      y="0"
      :width="pointWidth"
      :height="pointHeight"
      :rx="corner"
      class="point selected"
    />
  </svg>
</template>
<script>
export default {
  name: "timeline",
  // TODO: Add indication of trim, at least for collasped timeline
  props: ["timeline", "selectedIndex", "scale", "crop", "disabled"],
  data() {
    return {
      pointWidth: "1%",
      pointHeight: 15,
      corner: 2
    };
  },
  methods: {
    position(item) {
      let pos = this.translate(item);

      if (this.crop) {
        pos = (pos - this.crop.left) / (this.crop.right - this.crop.left);
      }

      return pos * 100 - (1 /*pointWidth*/) + "%";
    },
    translate(cx) {
      const scale = [...this.scale];
      if (scale[0] >= scale[1]) {
        return cx;
      }

      return (cx - scale[0]) / (scale[1] - scale[0]);
    },
    onItemClick(index) {
      if (this.disabled) {
        return;
      }
      const timestamp = parseInt(this.timeline[index]);
      this.$store.dispatch('updateTimelineTime', timestamp);
    }
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
    }
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
