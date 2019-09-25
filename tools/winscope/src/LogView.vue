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
  <md-card-content class="container">
    <md-table class="log-table">
      <md-table-header>
        <md-table-head class="time-column-header">Time</md-table-head>
        <md-table-head class="tag-column-header">Tag</md-table-head>
        <md-table-head class="at-column-header">At</md-table-head>
        <md-table-head>Message</md-table-head>
      </md-table-header>
      <md-table-body>
        <md-table-row v-for="line in data" :key="line.timestamp">
          <md-table-cell class="time-column">{{line.time}}</md-table-cell>
          <md-table-cell class="tag-column">{{line.tag}}</md-table-cell>
          <md-table-cell class="at-column">{{line.at}}</md-table-cell>
          <md-table-cell>{{line.text}}</md-table-cell>
        </md-table-row>
      </md-table-body>
    </md-table>
  </md-card-content>
</template>
<script>
export default {
  name: 'logview',
  data() {
    return {
      data: [],
      isSelected: false,
    }
  },
  methods: {
    arrowUp() {
      this.isSelected = !this.isSelected;
      return !this.isSelected;
    },
    arrowDown() {
      this.isSelected = !this.isSelected;
      return !this.isSelected;
    }
  },
  updated() {
    let scrolltable = this.$el.getElementsByTagName("tbody")[0]
    scrolltable.scrollTop = scrolltable.scrollHeight - 100;
  },
  watch: {
    selectedIndex: {
      immediate: true,
      handler(idx) {
        if (this.file.data.length > 0) {
          while (this.data.length > idx + 1) {
            this.data.pop();
          }
          while (this.data.length <= idx) {
            this.data.push(this.file.data[this.data.length]);
          }
        }
      },
    }
  },
  props: ['file'],
  computed: {
    selectedIndex() {
      return this.file.selectedIndex;
    },
  },
}

</script>
<style>
.log-table .md-table-cell {
  height: auto;
}

.log-table {
  width: 100%;
}

.time-column {
  min-width: 15em;
}

.time-column-header {
  min-width: 15em;
  padding-right: 9em !important;
}

.tag-column {
  min-width: 10em;
}

.tag-column-header {
  min-width: 10em;
  padding-right: 7em !important;
}

.at-column {
  min-width: 35em;
}

.at-column-header {
  min-width: 35em;
  padding-right: 32em !important;
}

.log-table table {
  display: block;
}

.log-table tbody {
  display: block;
  overflow-y: scroll;
  width: 100%;
  height: 20em;
}

.log-table tr {
  width: 100%;
  display: block;
}

.log-table td:last-child {
  width: 100%;
}

</style>
