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
  <md-content class="searchbar">

    <div class="tabs">
      <div class="search-timestamp" v-if="isTimestampSearch()">
        <md-field md-inline class="search-input">
          <label>Enter timestamp</label>
          <md-input
            v-model="searchInput"
            v-on:focus="updateInputMode(true)"
            v-on:blur="updateInputMode(false)"
            @keyup.enter.native="updateSearchForTimestamp"
          />
        </md-field>
        <md-button
          class="md-dense md-primary search-timestamp-button"
          @click="updateSearchForTimestamp"
        >
          Go to timestamp
        </md-button>
      </div>

      <div class="dropdown-content" v-if="isTransitionSearch()">
        <table>
          <tr class="header">
            <th style="width: 10%">Global Start</th>
            <th style="width: 10%">Global End</th>
            <th style="width: 80%">Transition</th>
          </tr>

          <tr v-for="item in filteredTransitionsAndErrors" :key="item.id">
            <td
              v-if="isTransition(item)"
              class="inline-time"
              @click="
                setCurrentTimestamp(transitionStart(transitionTags(item.id)))
              "
            >
              <span>{{ transitionTags(item.id)[0].desc }}</span>
            </td>
            <td
              v-if="isTransition(item)"
              class="inline-time"
              @click="setCurrentTimestamp(transitionEnd(transitionTags(item.id)))"
            >
              <span>{{ transitionTags(item.id)[1].desc }}</span>
            </td>
            <td
              v-if="isTransition(item)"
              class="inline-transition"
              :style="{color: transitionTextColor(item.transition)}"
              @click="setCurrentTimestamp(transitionStart(transitionTags(item.id)))"
            >
              {{ transitionDesc(item.transition) }}
            </td>
          </tr>
        </table>
        <md-field md-inline class="search-input">
          <label>
            Filter by transition name. Click to navigate to closest
            timestamp in active timeline.
          </label>
          <md-input
            v-model="searchInput"
            v-on:focus="updateInputMode(true)"
            v-on:blur="updateInputMode(false)"
          />
        </md-field>
      </div>

      <div class="dropdown-content" v-if="isErrorSearch()">
        <table>
          <tr class="header">
            <th style="width: 10%">Timestamp</th>
            <th style="width: 90%">Error Message</th>
          </tr>

          <tr v-for="item in filteredTransitionsAndErrors" :key="item.id">
            <td
              v-if="!isTransition(item)"
              class="inline-time"
              @click="setCurrentTimestamp(item.timestamp)"
            >
              {{ errorDesc(item.timestamp) }}
            </td>
            <td
              v-if="!isTransition(item)"
              class="inline-error"
              @click="setCurrentTimestamp(item.timestamp)"
            >
              {{ `${item.assertionName} ${item.message}` }}
            </td>
          </tr>
        </table>
        <md-field md-inline class="search-input">
          <label>
            Filter by error message. Click to navigate to closest
            timestamp in active timeline.
          </label>
          <md-input
            v-model="searchInput"
            v-on:focus="updateInputMode(true)"
            v-on:blur="updateInputMode(false)"
          />
        </md-field>
      </div>
    </div>

    <div class="tab-container" v-if="searchTypes.length > 0">
      Search mode:
      <md-button
        v-for="searchType in searchTypes"
        :key="searchType"
        @click="setSearchType(searchType)"
        :class="tabClass(searchType)"
      >
        {{ searchType }}
      </md-button>
    </div>
  </md-content>
</template>
<script>
import { transitionMap, SEARCH_TYPE } from "./utils/consts";
import { nanos_to_string, getClosestTimestamp } from "./transform";

export default {
  name: "searchbar",
  props: ["store", "presentTags", "timeline", "presentErrors", "searchTypes"],
  data() {
    return {
      searchType: SEARCH_TYPE.TIMESTAMP,
      searchInput: "",
    };
  },
  methods: {
    /** Set search type depending on tab selected */
    setSearchType(searchType) {
      this.searchType = searchType;
    },
    /** Set tab class to determine color highlight for active tab */
    tabClass(searchType) {
      var isActive = (this.searchType === searchType) ? 'active' : 'inactive';
      return ['tab', isActive];
    },

    /** Filter all the tags present in the trace by the searchbar input */
    filteredTags() {
      var tags = [];
      var filter = this.searchInput.toUpperCase();
      this.presentTags.forEach((tag) => {
        const tagTransition = tag.transition.toUpperCase();
        if (tagTransition.includes(filter)) tags.push(tag);
      });
      return tags;
    },
    /** Add filtered errors to filtered tags to integrate both into table*/
    filteredTagsAndErrors() {
      var tagsAndErrors = [...this.filteredTags()];
      var filter = this.searchInput.toUpperCase();
      this.presentErrors.forEach((error) => {
        const errorMessage = error.message.toUpperCase();
        if (errorMessage.includes(filter)) tagsAndErrors.push(error);
      });
      // sort into chronological order
      tagsAndErrors.sort((a, b) => (a.timestamp > b.timestamp ? 1 : -1));

      return tagsAndErrors;
    },
    /** Each transition has two tags present
     * Isolate the tags for the desire transition
     * Add a desc to display the timestamps as strings
     */
    transitionTags(id) {
      var tags = this.filteredTags().filter((tag) => tag.id === id);
      tags.forEach((tag) => {
        tag.desc = nanos_to_string(tag.timestamp);
      });
      return tags;
    },

    /** Find the start as minimum timestamp in transition tags */
    transitionStart(tags) {
      var times = tags.map((tag) => tag.timestamp);
      return times[0];
    },
    /** Find the end as maximum timestamp in transition tags */
    transitionEnd(tags) {
      var times = tags.map((tag) => tag.timestamp);
      return times[times.length - 1];
    },
    /**
     * Upon selecting a start/end tag in the dropdown;
     * navigates to that timestamp in the timeline
     */
    setCurrentTimestamp(timestamp) {
      this.$store.dispatch("updateTimelineTime", timestamp);
    },

    /** Colour codes text of transition in dropdown */
    transitionTextColor(transition) {
      return transitionMap.get(transition).color;
    },
    /** Displays transition description rather than variable name */
    transitionDesc(transition) {
      return transitionMap.get(transition).desc;
    },
    /** Add a desc to display the error timestamps as strings */
    errorDesc(timestamp) {
      return nanos_to_string(timestamp);
    },

    /** Navigates to closest timestamp in timeline to search input*/
    updateSearchForTimestamp() {
      const closestTimestamp = getClosestTimestamp(this.searchInput, this.timeline);
      this.setCurrentTimestamp(closestTimestamp);
    },

    isTransitionSearch() {
      return this.searchType === SEARCH_TYPE.TRANSITIONS;
    },
    isErrorSearch() {
      return this.searchType === SEARCH_TYPE.ERRORS;
    },
    isTimestampSearch() {
      return this.searchType === SEARCH_TYPE.TIMESTAMP;
    },
    isTransition(item) {
      return item.stacktrace === undefined;
    },

    /** determines whether left/right arrow keys should move cursor in input field */
    updateInputMode(isInputMode) {
      this.store.isInputMode = isInputMode;
    },
  },
  computed: {
    filteredTransitionsAndErrors() {
      var ids = [];
      return this.filteredTagsAndErrors().filter((item) => {
        if (this.isTransition(item) && !ids.includes(item.id)) {
          item.transitionStart = true;
          ids.push(item.id);
        }
        return !this.isTransition(item) || this.isTransition(item) && item.transitionStart;
      });
    },
  },
  destroyed() {
    this.updateInputMode(false);
  },
};
</script>
<style scoped>
.searchbar {
  background-color: rgb(250, 243, 233) !important;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  bottom: 1px;
}

.tabs {
  padding-top: 1rem;
}

.tab-container {
  padding-left: 20px;
  display: flex;
  align-items: center;
}

.tab.active {
  background-color: rgb(236, 222, 202);
}

.tab.inactive {
  background-color: rgb(250, 243, 233);
}

.search-timestamp {
  padding: 5px 20px 0px 20px;
  display: inline-flex;
  width: 100%;
}

.search-timestamp > .search-input {
  margin-top: -5px;
  max-width: 200px;
}

.search-timestamp-button {
  left: 0;
  padding: 0 15px;
}

.dropdown-content {
  padding: 5px 20px 0px 20px;
  display: block;
}

.dropdown-content table {
  overflow-y: scroll;
  max-height: 150px;
  display: block;
}

.dropdown-content table td {
  padding: 5px;
}

.dropdown-content table th {
  text-align: left;
  padding: 5px;
}

.inline-time:hover {
  background: rgb(216, 250, 218);
  cursor: pointer;
}

.inline-transition {
  font-weight: bold;
}

.inline-transition:hover {
  background: rgb(216, 250, 218);
  cursor: pointer;
}

.inline-error {
  font-weight: bold;
  color: red;
}

.inline-error:hover {
  background: rgb(216, 250, 218);
  cursor: pointer;
}
</style>
