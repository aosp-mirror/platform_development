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
        <md-table-row>
          <md-table-head class="time-column-header">Time</md-table-head>
          <md-table-head class="tag-column-header">Tag</md-table-head>
          <md-table-head class="at-column-header">At</md-table-head>
          <md-table-head>Message</md-table-head>
        </md-table-row>
      </md-table-header>

      <div class="scrollBody" ref="tableBody">
        <md-table-row v-for="(line, i) in data" :key="line.timestamp">
          <div :class="{inactive: i > idx}">
            <md-table-cell class="time-column">
              <a v-on:click="setTimelineTime(line.timestamp)">{{line.time}}</a>
            </md-table-cell>
            <md-table-cell class="tag-column">{{line.tag}}</md-table-cell>
            <md-table-cell class="at-column">{{line.at}}</md-table-cell>
            <md-table-cell>{{line.text}}</md-table-cell>
          </div>
        </md-table-row>
      </div>

    </md-table>
  </md-card-content>
</template>
<script>
export default {
  name: 'logview',
  data() {
    return {
      data: this.file.data,
      isSelected: false,
      idx: 0,
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
    },
    getRowEl(idx) {
      return this.$refs.tableBody.querySelectorAll('tr')[idx];
    },
    scrollToRow(idx) {
      const body = this.$refs.tableBody;
      const row = this.getRowEl(idx);

      const bodyRect = body.getBoundingClientRect();
      const rowRect = row.getBoundingClientRect();

      // Is the row viewable?
      const isViewable = (rowRect.top >= bodyRect.top) &&
        (rowRect.top <= bodyRect.top + body.clientHeight);

      if (!isViewable) {
        body.scrollTop = (rowRect.top + body.scrollTop + rowRect.height) -
          (body.clientHeight + bodyRect.top);
      }
    },
    setTimelineTime(timestamp) {
      this.$store.dispatch('updateTimelineTime', timestamp);
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
        this.idx = idx;
        this.scrollToRow(idx);
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
}

.log-table tr {
  width: 100%;
  display: block;
}

.log-table td:last-child {
  width: 100%;
}

.scrollBody {
  height: 75vh;
  width: 100%;
  overflow: auto;
}

.scrollBody a {
  cursor: pointer;
}

.scrollBody .inactive {
  color: gray;
}

.scrollBody .inactive a {
  color: gray;
}

</style>
