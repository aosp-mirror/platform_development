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
    <circle :cx="position(item)" cy="10" r="5" v-for="(item, idx) in items" @click="onItemClick(idx)" />
    <circle v-if="items.length" :cx="position(selected)" cy="10" r="5" class="selected" />
  </svg>
</template>
<script>
export default {
  name: 'timeline',
  props: ['items', 'selectedIndex', 'scale'],
  data() {
    return {};
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
      return (cx - scale[0]) / (scale[1] - scale[0]) * 2000;
    },
    onItemClick(index) {
      this.$emit('item-selected', index);
    },
  },
  computed: {
    timestamps() {
      if (this.items.length == 1) {
        return [0];
      }
      return this.items;
    },
    selected() {
      return this.items[this.selectedIndex];
    }
  },
}

</script>
<style scoped>
.selected {
  fill: red;
}

</style>
