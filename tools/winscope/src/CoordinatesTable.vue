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
  <span class="coord-null-value" v-if="!hasCoordinates">null</span>
  <div class="coord-table-wrapper" v-else>
    <md-table class="table">
      <md-table-row class="header-row">
        <md-table-cell>Left</md-table-cell>
        <md-table-cell>Top</md-table-cell>
        <md-table-cell>Right</md-table-cell>
        <md-table-cell>Bottom</md-table-cell>
      </md-table-row>
      <md-table-row>
        <md-table-cell>{{ coordinates.left }}</md-table-cell>
        <md-table-cell>{{ coordinates.top }}</md-table-cell>
        <md-table-cell>{{ coordinates.right }}</md-table-cell>
        <md-table-cell>{{ coordinates.bottom }}</md-table-cell>
      </md-table-row>
    </md-table>
  </div>
</template>

<script>
export default {
  name: 'CoordinatesTable.vue',
  props: ['coordinates'],
  computed: {
    hasCoordinates() {
      return this.coordinates.left || this.coordinates.top ||
          this.coordinates.right || this.coordinates.bottom;
    },
  },
};
</script>

<style>
.coord-null-value {
  color: rgba(0, 0, 0, 0.75);
}

.coord-table-wrapper {
  margin-left: 10px;
  display: inline-flex;
  padding: 3px 0px;
}

.coord-table-wrapper .md-table-cell {
  height: auto;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.coord-table-wrapper .md-table-cell-container {
  padding: 3px 15px;
  text-align: center;
}

.coord-table-wrapper .header-row .md-table-cell {
  color: gray;
  font-weight: 600;
}
</style>
