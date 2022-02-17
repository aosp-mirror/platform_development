<!-- Copyright (C) 2022 The Android Open Source Project

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
  <div class="matrix">
    <md-tooltip>{{ transform.getTypeAsString() }}</md-tooltip>
    <div class="cell">{{ formatFloat(transform.matrix.dsdx) }}</div>
    <div class="cell">{{ formatFloat(transform.matrix.dsdy) }}</div>
    <div class="cell">
      {{ formatFloat(transform.matrix.tx) }}<md-tooltip>Translate x</md-tooltip>
    </div>

    <div class="cell">{{ formatFloat(transform.matrix.dtdx) }}</div>
    <div class="cell">{{ formatFloat(transform.matrix.dtdy) }}</div>
    <div class="cell">
      {{ formatFloat(transform.matrix.ty) }}<md-tooltip>Translate y</md-tooltip>
    </div>

    <div class="cell">0</div>
    <div class="cell">0</div>
    <div class="cell">1</div>
  </div>
</template>

<script>
export default {
  name: "TransformMatrix",
  props: ["transform"],
  methods: {
    formatFloat(num) {
      return Math.round(num * 100) / 100;
    },
  },
};
</script>

<style scoped>
.matrix {
  display: grid;
  grid-gap: 1px;
  grid-template-columns: repeat(3, 1fr);
}

.cell {
  padding-left: 10px;
  background-color: rgba(158, 192, 200, 0.281);
}
</style>
