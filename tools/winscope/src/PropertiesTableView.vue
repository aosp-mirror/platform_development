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
  <div class="properties-table-wrapper">
    <md-table class="table">
      <md-table-row
          v-for="[propertyName, propertyValue] in propertyEntries"
          :key="propertyName">
        <md-table-cell class="table-key-cell">
          {{ propertyName }}
        </md-table-cell>
        <md-table-cell class="table-key-value">
          {{ propertyValue != null ? propertyValue : 'undefined' }}
        </md-table-cell>
      </md-table-row>
    </md-table>
  </div>
</template>

<script>
export default {
  name: 'PropertiesTableView.vue',
  props: ['tableEntries'],
  computed: {
    propertyEntries() {
      return Object.entries(this.tableEntries);
    },
  },
};
</script>

<style>
.properties-table-wrapper {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  margin-bottom: 10px;
}

.properties-table-wrapper .table-heading {
  padding: 10px 16px;
}

.properties-table-wrapper .table-key-cell {
  background-color: rgba(158, 192, 200, 0.281);
}

.properties-table-wrapper .table-key-value {
  overflow-wrap: anywhere;
  border-left: 1px solid rgba(0, 0, 0, 0.12);
}

.properties-table-wrapper .md-table-cell {
  height: auto;
}

.properties-table-wrapper .md-table-cell-container {
  padding: 8px 16px;
}

.properties-table-wrapper .md-table-cell:last-child .md-table-cell-container {
  padding-right: 16px;
}
</style>
