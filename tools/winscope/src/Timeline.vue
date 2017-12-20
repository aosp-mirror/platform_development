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
  <svg width="2000" height="20" viewBox="-5,0,2010,20">
    <circle :cx="translate(c.timestamp)" cy="10" r="5" v-for="(c,i) in items"
      @click="onItemClick(c, i)" :class="itemClass(c)" />
  </svg>
</template>

<script>

export default {
  name: 'timeline',
  props: ['items', 'selected'],
  data () {
    return {
    };
  },
  methods: {
    translate(cx) {
      var scale = [...this.scale];
      if (scale[0] >= scale[1]) {
        return cx;
      }
      return (cx - scale[0]) / (scale[1] - scale[0]) * 2000;
    },
    onItemClick(item, index) {
      this.$emit('item-selected', item, index);
    },
    itemClass(item) {
      return (this.selected == item) ? 'selected' : 'not-selected'
    }
  },
  computed: {
    scale() {
      return [Math.min(...this.timestamps), Math.max(...this.timestamps)];
    },
    timestamps() {
      if (this.items.length == 1) {
        return [0];
      }
      return this.items.map((e) => parseInt(e.timestamp));
    }
  },
}
</script>

<style scoped>
.selected {
  fill: red;
}
</style>
