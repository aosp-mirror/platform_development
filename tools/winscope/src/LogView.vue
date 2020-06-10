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
    <div class="navigation">
      <md-button class="md-dense md-primary" @click.native="scrollToRow(lastOccuredIndex)">
        Jump to latest entry
      </md-button>
      <md-button
        class="md-icon-button" :class="{'md-primary': pinnedToLatest}"
        @click.native="togglePin"
      >
        <md-icon>push_pin</md-icon>
      </md-button>
      <!-- <md-checkbox v-model="pinnedToTimeline" class="md-primary">Pin to timeline</md-checkbox> -->
    </div>

    <div class="filters">
      <md-field>
        <label>Tags</label>
        <md-select v-model="selectedTags" multiple>
          <md-option v-for="tag in tags" :value="tag">{{ tag }}</md-option>
        </md-select>
      </md-field>

      <md-autocomplete v-model="selectedSourceFile" :md-options="sourceFiles">
        <label>Source file</label>

        <template slot="md-autocomplete-item" slot-scope="{ item, term }">
          <md-highlight-text :md-term="term">{{ item }}</md-highlight-text>
        </template>

        <template slot="md-autocomplete-empty" slot-scope="{ term }">
          No source file matching "{{ term }}" was found.
        </template>
      </md-autocomplete>

      <md-field class="search-message-field" md-clearable>
        <md-input placeholder="Search messages..." v-model="searchInput"></md-input>
      </md-field>
    </div>

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
        <md-table-row v-for="(line, i) in processedData" :key="line.timestamp">
          <div :class="{inactive: !line.occured}">
            <md-table-cell class="time-column">
              <a v-on:click="setTimelineTime(line.timestamp)">{{line.time}}</a>
              <div class="new-badge" v-show="prevLastOccuredIndex < i && i <= lastOccuredIndex">New</div>
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
import { findLastMatchingSorted } from './utils/utils.js'

export default {
  name: 'logview',
  data() {
    const data = this.file.data;

    const tags = new Set();
    const sourceFiles = new Set();
    for (const line of data) {
      tags.add(line.tag);
      sourceFiles.add(line.at);
    }

    return {
      data,
      isSelected: false,
      prevLastOccuredIndex: 0,
      lastOccuredIndex: 0,
      selectedTags: Array.from(tags),
      selectedSourceFile: null,
      searchInput: null,
      sourceFiles: Array.from(sourceFiles),
      tags: Array.from(tags),
      pinnedToLatest: true,
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
    togglePin() {
      this.pinnedToLatest = !this.pinnedToLatest;
    },
    scrollToRow(idx) {
      if (!this.$refs.tableBody) {
        return;
      }

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
    pinnedToLatest(isPinned) {
      if (isPinned) {
        this.scrollToRow(this.lastOccuredIndex);
      }
    },
    currentTimestamp: {
      immediate: true,
      handler(ts) {
        this.prevLastOccuredIndex = this.lastOccuredIndex;
        this.lastOccuredIndex = findLastMatchingSorted(this.processedData,
          (array, idx) => array[idx].timestamp <= ts);

        if (this.pinnedToLatest) {
          this.scrollToRow(this.lastOccuredIndex);
        }
      },
    }
  },
  props: ['file'],
  computed: {
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    processedData() {
      const filteredData = this.data.filter(line => {
        if (this.sourceFiles.includes(this.selectedSourceFile)) {
          // Only filter once source file is fully inputed
          if (line.at != this.selectedSourceFile) {
            return false;
          }
        }

        if (!this.selectedTags.includes(line.tag)) {
          return false;
        }

        if (this.searchInput && !line.text.includes(this.searchInput)) {
          return false;
        }

        return true;
      });

      for (const line of filteredData) {
        line.occured = line.timestamp <= this.$store.state.currentTimestamp;
      }

      return filteredData;
    }
  },
}

</script>
<style>
.filters, .navigation {
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.navigation {
  justify-content: flex-end;
}

.navigation > button {
  margin: 0;
}

.filters > div {
  margin: 10px;
}

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

.new-badge {
  display: inline-block;
  background: rgb(84, 139, 247);
  border-radius: 3px;
  color: white;
  padding: 0 5px;
  position: absolute;
  margin-left: 5px;
  font-size: 10px;
}
</style>
