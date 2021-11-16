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
  <div class="bounds" :style="boundsStyle">
    <div
      class="rect" v-for="rect in filteredRects"
      :style="rectToStyle(rect)"
      @click="onClick(rect)"
      v-bind:key="`${rect.left}-${rect.right}-${rect.top}-${rect.bottom}-${rect.ref.name}`"
    >
      <span class="label">{{rect.label}}</span>
    </div>
    <div
      class="highlight"
      v-if="highlight"
      :style="rectToStyle(highlight)"
    />
    <div
      class="displayRect" v-for="rect in displayRects"
      :style="rectToStyle(rect)"
      v-bind:key="`${rect.left}-${rect.right}-${rect.top}-${rect.bottom}-${rect.id}`"
    />
  </div>
</template>

<script>

// eslint-disable-next-line camelcase
import {multiplyRect} from './matrix_utils.js';

export default {
  name: 'rects',
  props: ['bounds', 'rects', 'highlight','displays'],
  data() {
    return {
      desiredHeight: 800,
      desiredWidth: 400,
    };
  },
  computed: {
    boundsC() {
      if (this.bounds) {
        return this.bounds;
      }
      var width = Math.max(
          ...this.rects.map((rect) => multiplyRect(rect.transform, rect).right));
      var height = Math.max(
          ...this.rects.map((rect) => multiplyRect(rect.transform, rect).bottom));

      // constrain max bounds to prevent boundless layers from shrinking visible displays
      if (this.hasDisplays) {
        width = Math.min(width, this.maxWidth);
        height = Math.min(height, this.maxHeight);
      }
      return {width, height};
    },
    maxWidth() {
      return Math.max(...this.displayRects.map(rect => rect.width)) * 1.3;
    },
    maxHeight() {
      return Math.max(...this.displayRects.map(rect => rect.height)) * 1.3;
    },
    hasDisplays() {
      return this.displays.length > 0;
    },
    boundsStyle() {
      return this.rectToStyle({top: 0, left: 0, right: this.boundsC.width,
        bottom: this.boundsC.height});
    },
    filteredRects() {
      return this.rects.filter((rect) => {
        const isVisible = rect.ref.isVisible;
        return isVisible;
      });
    },
    displayRects() {
      return this.displays.map(display => {
        var rect = display.layerStackSpace;
        rect.id = display.id;
        return rect;
      });
    },
  },
  methods: {
    s(sourceCoordinate) { // translate source into target coordinates
      let scale;
      if (this.boundsC.width < this.boundsC.height) {
        scale = this.desiredHeight / this.boundsC.height;
      } else {
        scale = this.desiredWidth / this.boundsC.width;
      }
      return sourceCoordinate * scale;
    },
    rectToStyle(rect) {
      const x = this.s(rect.left);
      const y = this.s(rect.top);
      const w = this.s(rect.right) - this.s(rect.left);
      const h = this.s(rect.bottom) - this.s(rect.top);

      let t;
      if (rect.transform && rect.transform.matrix) {
        t = rect.transform.matrix;
      } else {
        t = rect.transform;
      }

      const tr = t ? `matrix(${t.dsdx}, ${t.dtdx}, ${t.dsdy}, ${t.dtdy}, ` +
          `${this.s(t.tx)}, ${this.s(t.ty)})` : '';
      const rectStyle = `top: ${y}px; left: ` +
            `${x}px; height: ${h}px; width: ${w}px; ` +
            `transform: ${tr}; transform-origin: 0 0;`;
      return rectStyle;
    },
    onClick(rect) {
      this.$emit('rect-click', rect.ref);
    },
  },
};
</script>

<style scoped>
.bounds {
  position: relative;
  overflow: hidden;
}
.highlight, .rect, .displayRect {
  position: absolute;
  box-sizing: border-box;
  display: flex;
  justify-content: flex-end;
}
.rect {
  border: 1px solid black;
  background-color: rgba(146, 149, 150, 0.8);
}
.highlight {
  border: 2px solid rgb(235, 52, 52);
  background-color: rgba(243, 212, 212, 0.25);
  pointer-events: none;
}
.displayRect {
  border: 4px dashed #195aca;
  pointer-events: none;
}
.label {
  align-self: center;
}
</style>
