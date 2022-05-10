<!-- Copyright (C) 2019 The Android Open Source Project

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
  <video
    class="md-elevation-2 screen"
    :src="file.data"
    :style="style"
    ref="video"
  />
</template>
<script>
const EPSILON = 0.00001;

function uint8ToString(array) {
  const chunk = 0x8000;
  const out = [];
  for (let i = 0; i < array.length; i += chunk) {
    out.push(String.fromCharCode.apply(null, array.subarray(i, i + chunk)));
  }
  return out.join('');
}

export default {
  name: 'videoview',
  props: ['file', 'height'],
  data() {
    // Record analytics event
    this.recordOpenTraceEvent("Video");
    return {};
  },
  computed: {
    selectedIndex() {
      return this.file.selectedIndex;
    },
    style() {
      if (typeof this.height == 'number') {
        return `height: ${this.height}px`;
      } else {
        return `height: ${this.height}`;
      }
    },
  },
  methods: {
    arrowUp() {
      return true;
    },
    arrowDown() {
      return true;
    },
    selectFrameAtTime(timestamp) {
      const time = (timestamp - this.file.timeline[0]) / 1000000000 + EPSILON;
      this.$refs.video.currentTime = time;
    },
    selectFrame(idx) {
      this.selectFrameAtTime(this.file.timeline[idx]);
    },
    jumpToSelectedIndex() {
      this.selectFrame(this.file.selectedIndex);
    },
  },
  watch: {
    selectedIndex() {
      this.selectFrame(this.file.selectedIndex);
    },
  },
  mounted() {
    this.$el.addEventListener('canplay', (e) => {
      this.$emit('loaded');
    });
  },
};

</script>
<style>
</style>
