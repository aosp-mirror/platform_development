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
      <md-content
        md-tag="md-toolbar"
        md-elevation="0"
        class="card-toolbar md-transparent md-dense"
      >
        <h2 class="md-title" style="flex: 1">Log View</h2>
        <md-button
          class="md-dense md-primary"
          @click.native="scrollToRow(lastOccuredVisibleIndex)"
        >
          Jump to latest entry
        </md-button>
        <md-button
          class="md-icon-button" :class="{'md-primary': pinnedToLatest}"
          @click.native="togglePin"
        >
          <md-icon>push_pin</md-icon>
          <md-tooltip md-direction="top" v-if="pinnedToLatest">
            Unpin to latest message
          </md-tooltip>
          <md-tooltip md-direction="top" v-else>
            Pin to latest message
          </md-tooltip>
        </md-button>
      </md-content>
    </div>

    <div class="filters">
      <md-field>
        <label>Log Levels</label>
        <md-select v-model="selectedLogLevels" multiple>
          <md-option v-for="level in logLevels" :value="level" v-bind:key="level">{{ level }}</md-option>
        </md-select>
      </md-field>

      <md-field>
        <label>Tags</label>
        <md-select v-model="selectedTags" multiple>
          <md-option v-for="tag in tags" :value="tag" v-bind:key="tag">{{ tag }}</md-option>
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

    <div v-if="processedData.length > 0" style="overflow-y: auto;">
      <virtual-list style="height: 600px; overflow-y: auto;"
        :data-key="'uid'"
        :data-sources="processedData"
        :data-component="logEntryComponent"
        ref="loglist"
      />
    </div>
    <div class="no-logs-message" v-else>
      <md-icon>error_outline</md-icon>
      <span class="message">No logs founds...</span>
    </div>
  </md-card-content>
</template>
<script>
import { findLastMatchingSorted } from './utils/utils.js';
import { logLevel } from './utils/consts';
import LogEntryComponent from './LogEntry.vue';
import VirtualList from '../libs/virtualList/VirtualList';

export default {
  name: 'logview',
  data() {
    const data = this.file.data;
    // Record analytics event
    this.recordOpenTraceEvent("ProtoLog");

    const tags = new Set();
    const sourceFiles = new Set();
    for (const line of data) {
      tags.add(line.tag);
      sourceFiles.add(line.at);
    }

    data.forEach((entry, index) => entry.index = index);

    const logLevels = Object.values(logLevel);

    return {
      data,
      isSelected: false,
      prevLastOccuredIndex: -1,
      lastOccuredIndex: 0,
      selectedTags: [],
      selectedSourceFile: null,
      searchInput: null,
      sourceFiles: Object.freeze(Array.from(sourceFiles)),
      tags: Object.freeze(Array.from(tags)),
      pinnedToLatest: true,
      logEntryComponent: LogEntryComponent,
      logLevels,
      selectedLogLevels: [],
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
    scrollToRow(index) {
      if (!this.$refs.loglist) {
        return;
      }

      const itemOffset = this.$refs.loglist.virtual.getOffset(index);
      const itemSize = 35;
      const loglistSize = this.$refs.loglist.getClientSize();

      this.$refs.loglist.scrollToOffset(itemOffset - loglistSize + itemSize);
    },
    getLastOccuredIndex(data, timestamp) {
      if (this.data.length === 0) {
          return 0;
      }
      return findLastMatchingSorted(data,
        (array, idx) => array[idx].timestamp <= timestamp);
    },
  },
  watch: {
    pinnedToLatest(isPinned) {
      if (isPinned) {
        this.scrollToRow(this.lastOccuredVisibleIndex);
      }
    },
    currentTimestamp: {
      immediate: true,
      handler(newTimestamp) {
        this.prevLastOccuredIndex = this.lastOccuredIndex;
        this.lastOccuredIndex = this.getLastOccuredIndex(this.data, newTimestamp);

        if (this.pinnedToLatest) {
          this.scrollToRow(this.lastOccuredVisibleIndex);
        }
      },
    }
  },
  props: ['file'],
  computed: {
    lastOccuredVisibleIndex() {
      return this.getLastOccuredIndex(this.processedData, this.currentTimestamp);
    },
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    processedData() {
      const filteredData = this.data.filter(line => {
        if (this.selectedLogLevels.length > 0 &&
            !this.selectedLogLevels.includes(line.level.toLowerCase())) {
          return false;
        }

        if (this.sourceFiles.includes(this.selectedSourceFile)) {
          // Only filter once source file is fully inputed
          if (line.at != this.selectedSourceFile) {
            return false;
          }
        }

        if (this.selectedTags.length > 0 && !this.selectedTags.includes(line.tag)) {
          return false;
        }

        if (this.searchInput && !line.text.includes(this.searchInput)) {
          return false;
        }

        return true;
      });

      for (const entry of filteredData) {
        entry.new = this.prevLastOccuredIndex < entry.index &&
          entry.index <= this.lastOccuredIndex;
        entry.occured = entry.index <= this.lastOccuredIndex;
        entry.justInactivated = this.lastOccuredIndex < entry.index &&
          entry.index <= this.prevLastOccuredIndex;

        // Force refresh if any of these changes
        entry.uid = `${entry.index}${entry.new ? '-new' : ''}${entry.index}${entry.justInactivated ? '-just-inactivated' : ''}${entry.occured ? '-occured' : ''}`
      }

      return filteredData;
    }
  },
  components: {
    'virtual-list': VirtualList,
    'logentry': LogEntryComponent,
  }
}

</script>
<style>
.container {
  display: flex;
  flex-wrap: wrap;
}

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

.log-header {
  display: inline-flex;
  color: var(--md-theme-default-text-accent-on-background, rgba(0,0,0,0.54));
  font-weight: bold;
}

.log-header > div {
  padding: 6px 10px;
  border-bottom: 1px solid #f1f1f1;
}

.log-header .time-column {
  width: 13em;
}

.log-header .tag-column {
  width: 10em;
}

.log-header .at-column {
  width: 30em;
}

.column-title {
  font-size: 12px;
}

.no-logs-message {
  margin: 15px;
  display: flex;
  align-content: center;
  align-items: center;
}

.no-logs-message .message {
  margin-left: 10px;
  font-size: 15px;
}
</style>
