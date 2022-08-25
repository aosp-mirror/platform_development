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
  <div id="app">
    <vue-title :appName="appName" :traceName="traceNameForTitle" />

    <md-dialog-prompt
          class="edit-trace-name-dialog"
          :md-active.sync="editingTraceName"
          v-model="traceName"
          md-title="Edit trace name"
          md-input-placeholder="Enter a new trace name"
          md-confirm-text="Save" />

    <md-app>
      <md-app-toolbar md-tag="md-toolbar" class="top-toolbar">
        <h1 class="md-title">{{appName}}</h1>

        <div class="trace-name" v-if="dataLoaded">
          <div>
            <span>{{ traceName }}</span>
            <!-- <input type="text" class="trace-name-editable" v-model="traceName" /> -->
            <md-icon class="edit-trace-name-btn" @click.native="editTraceName()">edit</md-icon>
          </div>
        </div>

        <md-button
          class="md-primary md-theme-default download-all-btn"
          @click="generateTags()"
          v-if="dataLoaded && canGenerateTags"
        >Generate Tags</md-button>
        <md-button
          class="md-primary md-theme-default"
          @click="downloadAsZip(files, traceName)"
          v-if="dataLoaded"
        >Download All</md-button>
        <md-button
          class="md-accent md-raised md-theme-default clear-btn"
          style="box-shadow: none;"
          @click="clear()"
          v-if="dataLoaded"
        >Clear</md-button>
      </md-app-toolbar>

      <md-app-content class="main-content" :style="mainContentStyle">
        <section class="data-inputs" v-if="!dataLoaded">
          <div class="input">
            <dataadb class="adbinput" ref="adb" :store="store"
              @dataReady="onDataReady" />
          </div>
          <div class="input" @dragover.prevent @drop.prevent>
            <datainput class="fileinput" ref="input" :store="store"
              @dataReady="onDataReady" />
          </div>
        </section>

        <section class="data-view">
          <div
            class="data-view-container"
            v-for="file in dataViewFiles"
            :key="file.type"
          >
            <dataview
              :ref="file.type"
              :store="store"
              :file="file"
              :presentTags="Object.freeze(presentTags)"
              :presentErrors="Object.freeze(presentErrors)"
              :dataViewFiles="dataViewFiles"
              @click="onDataViewFocus(file)"
            />
          </div>

          <overlay
            :presentTags="Object.freeze(presentTags)"
            :presentErrors="Object.freeze(presentErrors)"
            :store="store"
            :ref="overlayRef"
            :searchTypes="searchTypes"
            v-if="dataLoaded"
            v-on:bottom-nav-height-change="handleBottomNavHeightChange"
          />
        </section>
      </md-app-content>
    </md-app>
  </div>
</template>
<script>
import Overlay from './Overlay.vue';
import DataView from './DataView.vue';
import DataInput from './DataInput.vue';
import LocalStore from './localstore.js';
import DataAdb from './DataAdb.vue';
import FileType from './mixins/FileType.js';
import SaveAsZip from './mixins/SaveAsZip';
import FocusedDataViewFinder from './mixins/FocusedDataViewFinder';
import {DIRECTION} from './utils/utils';
import Searchbar from './Searchbar.vue';
import {NAVIGATION_STYLE, SEARCH_TYPE} from './utils/consts';
import {TRACE_TYPES, FILE_TYPES, dataFile} from './decode.js';
import { TaggingEngine } from './flickerlib/common';
import titleComponent from './Title.vue';

const APP_NAME = 'Winscope';

const CONTENT_BOTTOM_PADDING = 25;

export default {
  name: 'app',
  mixins: [FileType, SaveAsZip, FocusedDataViewFinder],
  data() {
    return {
      appName: APP_NAME,
      activeDataView: null,
      // eslint-disable-next-line new-cap
      store: LocalStore('app', {
        flattened: false,
        onlyVisible: false,
        simplifyNames: true,
        displayDefaults: true,
        navigationStyle: NAVIGATION_STYLE.GLOBAL,
        flickerTraceView: false,
        showFileTypes: [],
        isInputMode: false,
      }),
      overlayRef: 'overlay',
      mainContentStyle: {
        'padding-bottom': `${CONTENT_BOTTOM_PADDING}px`,
      },
      tagFile: null,
      presentTags: [],
      presentErrors: [],
      searchTypes: [SEARCH_TYPE.TIMESTAMP],
      hasTagOrErrorTraces: false,
      traceName: "unnamed_winscope_trace",
      editingTraceName: false
    };
  },
  created() {
    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('scroll', this.onScroll);
    // document.title = this.traceName;
  },
  destroyed() {
    window.removeEventListener('keydown', this.onKeyDown);
    window.removeEventListener('scroll', this.onScroll);
  },

  methods: {
    /** Get states from either tag files or error files */
    getUpdatedStates(files) {
      var states = [];
      for (const file of files) {
        states.push(...file.data);
      }
      return states;
    },
    /** Get tags from all uploaded tag files*/
    getUpdatedTags() {
      if (this.tagFile === null) return [];
      const tagStates = this.getUpdatedStates([this.tagFile]);
      var tags = [];
      tagStates.forEach(tagState => {
        tagState.tags.forEach(tag => {
          tag.timestamp = Number(tagState.timestamp);
          // tags generated on frontend have transition.name due to kotlin enum
          tag.transition = tag.transition.name ?? tag.transition;
          tags.push(tag);
        });
      });
      return tags;
    },
    /** Get tags from all uploaded error files*/
    getUpdatedErrors() {
      var errorStates = this.getUpdatedStates(this.errorFiles);
      var errors = [];
      //TODO (b/196201487) add check if errors empty
      errorStates.forEach(errorState => {
        errorState.errors.forEach(error => {
          error.timestamp = Number(errorState.timestamp);
          errors.push(error);
        });
      });
      return errors;
    },
    /** Set flicker mode check for if there are tag/error traces uploaded*/
    updateHasTagOrErrorTraces() {
      return this.hasTagTrace() || this.hasErrorTrace();
    },
    hasTagTrace() {
      return this.tagFile !== null;
    },
    hasErrorTrace() {
      return this.errorFiles.length > 0;
    },
    /** Activate flicker search tab if tags/errors uploaded*/
    updateSearchTypes() {
      this.searchTypes = [SEARCH_TYPE.TIMESTAMP];
      if (this.hasTagTrace()) {
        this.searchTypes.push(SEARCH_TYPE.TRANSITIONS);
      }
      if (this.hasErrorTrace()) {
        this.searchTypes.push(SEARCH_TYPE.ERRORS);
      }
    },
    /** Filter data view files by current show settings*/
    updateShowFileTypes() {
      this.store.showFileTypes = this.dataViewFiles
        .filter((file) => file.show)
        .map(file => file.type);
    },
    clear() {
      this.store.showFileTypes = [];
      this.tagFile = null;
      this.$store.commit('clearFiles');
      this.recordButtonClickedEvent("Clear")
    },
    onDataViewFocus(file) {
      this.$store.commit('setActiveFile', file);
      this.activeDataView = file.type;
    },
    onKeyDown(event) {
      event = event || window.event;
      if (this.store.isInputMode) return false;
      if (event.keyCode == 37 /* left */ ) {
        this.$store.dispatch('advanceTimeline', DIRECTION.BACKWARD);
      } else if (event.keyCode == 39 /* right */ ) {
        this.$store.dispatch('advanceTimeline', DIRECTION.FORWARD);
      } else if (event.keyCode == 38 /* up */ ) {
        this.$refs[this.activeView][0].arrowUp();
      } else if (event.keyCode == 40 /* down */ ) {
        this.$refs[this.activeView][0].arrowDown();
      } else {
        return false;
      }
      event.preventDefault();
      return true;
    },
    onDataReady(traceName, files) {
      this.traceName = traceName;
      this.$store.dispatch('setFiles', files);

      this.tagFile = this.tagFiles[0] ?? null;
      this.hasTagOrErrorTraces = this.updateHasTagOrErrorTraces();
      this.presentTags = this.getUpdatedTags();
      this.presentErrors = this.getUpdatedErrors();
      this.updateSearchTypes();
      this.updateFocusedView();
      this.updateShowFileTypes();
    },
    setStatus(status) {
      if (status) {
        this.title = status;
      } else {
        this.title = APP_NAME;
      }
    },
    handleBottomNavHeightChange(newHeight) {
      this.$set(
          this.mainContentStyle,
          'padding-bottom',
          `${ CONTENT_BOTTOM_PADDING + newHeight }px`,
      );
    },
    generateTags() {
      // generate tag file
      this.recordButtonClickedEvent("Generate Tags");
      const engine = new TaggingEngine(
        this.$store.getters.tagGenerationWmTrace,
        this.$store.getters.tagGenerationSfTrace,
        (text) => { console.log(text) }
      );
      const tagTrace = engine.run();
      const tagFile = this.generateTagFile(tagTrace);

      // update tag trace in set files, update flicker mode
      this.tagFile = tagFile;
      this.hasTagOrErrorTraces = this.updateHasTagOrErrorTraces();
      this.presentTags = this.getUpdatedTags();
      this.presentErrors = this.getUpdatedErrors();
      this.updateSearchTypes();
    },

    generateTagFile(tagTrace) {
      const data = tagTrace.entries;
      const blobUrl = URL.createObjectURL(new Blob([], {type: undefined}));
      return dataFile(
        "GeneratedTagTrace.winscope",
        data.map((x) => x.timestamp),
        data,
        blobUrl,
        FILE_TYPES.TAG_TRACE
      );
    },

    editTraceName() {
      this.editingTraceName = true;
    }
  },
  computed: {
    files() {
      return this.$store.getters.sortedFiles.map(file => {
        if (this.hasDataView(file)) {
          file.show = true;
        }
        return file;
      });
    },
    prettyDump() {
      return JSON.stringify(this.dump, null, 2);
    },
    dataLoaded() {
      return this.files.length > 0;
    },
    activeView() {
      if (!this.activeDataView && this.files.length > 0) {
        // eslint-disable-next-line vue/no-side-effects-in-computed-properties
        this.activeDataView = this.files[0].type;
      }
      return this.activeDataView;
    },
    dataViewFiles() {
      return this.files.filter((file) => this.hasDataView(file));
    },
    tagFiles() {
      return this.$store.getters.tagFiles;
    },
    errorFiles() {
      return this.$store.getters.errorFiles;
    },
    timelineFiles() {
      return this.$store.getters.timelineFiles;
    },
    canGenerateTags() {
      const fileTypes = this.dataViewFiles.map((file) => file.type);
      return fileTypes.includes(TRACE_TYPES.WINDOW_MANAGER)
        && fileTypes.includes(TRACE_TYPES.SURFACE_FLINGER);
    },
    traceNameForTitle() {
      if (!this.dataLoaded) {
        return undefined;
      } else {
        return this.traceName;
      }
    }
  },
  watch: {
    // title() {
    //   document.title = this.title;
    // },
  },
  components: {
    overlay: Overlay,
    dataview: DataView,
    datainput: DataInput,
    dataadb: DataAdb,
    searchbar: Searchbar,
    ["vue-title"]: titleComponent,
  },
};
</script>
<style>
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@600&display=swap');

#app .md-app-container {
  /* Get rid of transforms which prevent fixed position from being used */
  transform: none!important;
  min-height: 100vh;
}

#app .top-toolbar {
  box-shadow: none;
  background-color: #fff;
  background-color: var(--md-theme-default-background, #fff);
  border-bottom: thin solid rgba(0,0,0,.12);
  padding:  0 40px;
}

#app .top-toolbar .md-title {
  font-family: 'Open Sans', sans-serif;
  white-space: nowrap;
  color: #5f6368;
  margin: 0;
  padding: 0;
  font-size: 22px;
  letter-spacing: 0;
  font-weight: 600;
}

.data-view {
  display: flex;
  flex-direction: column;
}

.card-toolbar {
  border-bottom: 1px solid rgba(0, 0, 0, .12);
}

.timeline {
  margin: 16px;
}

.container {
  display: flex;
  flex-wrap: wrap;
}

.md-button {
  margin-top: 1em
}

h1 {
  font-weight: normal;
}

.data-inputs {
  display: flex;
  flex-wrap: wrap;
  height: 100%;
  width: 100%;
  align-self: center;
  /* align-items: center; */
  align-content: center;
  justify-content: center;
}

.data-inputs .input {
  padding: 15px;
  flex: 1 1 0;
  max-width: 840px;
  /* align-self: center; */
}

.data-inputs .input > div {
  height: 100%;
}

.data-view-container {
  padding: 25px 20px 0 20px;
}

.snackbar-break-words {
  /* These are technically the same, but use both */
  overflow-wrap: break-word;
  word-wrap: break-word;
  -ms-word-break: break-all;
  word-break: break-word;
  /* Adds a hyphen where the word breaks, if supported (No Blink) */
  -ms-hyphens: auto;
  -moz-hyphens: auto;
  -webkit-hyphens: auto;
  hyphens: auto;
  padding: 10px 10px 10px 10px;
}

.trace-name {
  flex: 1;
  display: flex;
  align-items: center;
  align-content: center;
  justify-content: center;
  font-family: 'Open Sans', sans-serif;
  font-size: 1rem;
}

.md-icon.edit-trace-name-btn {
  color: rgba(0, 0, 0, 0.6)!important;
  font-size: 1rem!important;
  margin-bottom: 0.1rem;
}

.md-icon.edit-trace-name-btn:hover {
  cursor: pointer;
}

.trace-name-editable {
  all: unset;
  cursor: default;
}

.edit-trace-name-dialog .md-dialog-container {
  min-width: 350px;
}

.md-overlay.md-dialog-overlay {
  z-index: 10;
}
</style>
