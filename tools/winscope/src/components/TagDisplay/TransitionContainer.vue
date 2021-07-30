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
  <div class="transition-container" :style="transitionStyle">
    <md-tooltip md-direction="left"> {{tooltip}} </md-tooltip>
    <arrow class="arrow-start" :style="transitionComponentColor"/>
    <div class="connector" :style="transitionComponentColor"/>
    <arrow class="arrow-end" :style="transitionComponentColor"/>
  </div>
</template>
<script>
import Arrow from './Arrow.vue';
export default {
  name: 'transition-container',
  components: {
    'arrow': Arrow,
  },
  props: {
    'width': {
      type: Number,
    },
    'startPos': {
      type: Number,
    },
    'color': {
      type: String,
    },
    'overlap': {
      type: Number,
    },
    'tooltip': {
      type: String,
    },
  },
  computed: {
    transitionStyle() {
      return {
        width: this.width + '%',
        left: this.startPos + '%',
        bottom: this.overlap * 100 + '%',
      }
    },
    transitionComponentColor() {
      return {
        borderTopColor: this.color,
      }
    },
  },
};
</script>
<style scoped>
.transition-container {
  position: absolute;
  height: 15px;
  display: inline-flex;
}

.arrow-start {
  left: 0%;
}

.arrow-end {
  right: 0%;
}

.connector {
  position: absolute;
  display: inline-block;
  width: auto;
  height: 9px;
  left: 5px;
  right: 5px;
  border-top: 1px solid;
}
</style>