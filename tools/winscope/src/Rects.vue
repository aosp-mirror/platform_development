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
    <div class="rect" v-for="r in rects" :style="rectToStyle(r)"
        @click="onClick(r)">
      <span class="label">{{r.label}}</span>
    </div>
    <div class="highlight" v-if="highlight" :style="rectToStyle(highlight)" />
  </div>
</template>

<script>

import { multiply_rect } from './matrix_utils.js'

export default {
  name: 'rects',
  props: ['bounds', 'rects', 'highlight'],
  data () {
    return {
      desiredWidth: 400,
    };
  },
  computed: {
    boundsC() {
      if (this.bounds) {
        return this.bounds;
      }
      var width = Math.max(...this.rects.map((r) => multiply_rect(r.transform, r).right));
      var height = Math.max(...this.rects.map((r) => multiply_rect(r.transform, r).bottom));
      return {width, height};
    },
    boundsStyle() {
      return this.rectToStyle({top: 0, left: 0, right: this.boundsC.width,
          bottom: this.boundsC.height});
    },
  },
  methods: {
    s(sourceCoordinate) {  // translate source into target coordinates
      return sourceCoordinate / this.boundsC.width * this.desiredWidth;
    },
    rectToStyle(r) {
      var x = this.s(r.left);
      var y = this.s(r.top);
      var w = this.s(r.right) - this.s(r.left);
      var h = this.s(r.bottom) - this.s(r.top);
      var t = r.transform;
      var tr = t ? `matrix(${t.dsdx}, ${t.dtdx}, ${t.dsdy}, ${t.dtdy}, ${this.s(t.tx)}, ${this.s(t.ty)})` : '';
      return `top: ${y}px; left: ${x}px; height: ${h}px; width: ${w}px;` +
             `transform: ${tr}; transform-origin: 0 0;`
    },
    onClick(r) {
      this.$emit('rect-click', r.ref);
    },
  }
}
</script>

<style scoped>
.bounds {
  position: relative;
  overflow: hidden;
}
.highlight, .rect {
  position: absolute;
  box-sizing: border-box;
  display: flex;
  justify-content: flex-end;
}
.rect {
  border: 1px solid black;
  background-color: rgba(100, 100, 100, 0.8);
}
.highlight {
  border: 2px solid red;
  pointer-events: none;
}
.label {
  align-self: center;
}
</style>
