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
    <md-app>
      <md-app-toolbar md-tag="md-toolbar" class="top-toolbar">
        <h1 class="md-title" style="flex: 1">{{title}}</h1>
        <md-button
          class="md-primary md-theme-default download-all-btn"
          @click="downloadAsZip(files)"
          v-if="dataLoaded"
        >Download All</md-button>
        <md-button
          class="md-accent md-raised md-theme-default clear-btn"
          style="box-shadow: none;"
          @click="clear()"
          v-if="dataLoaded"
        >Clear</md-button>
      </md-app-toolbar>

      <md-app-content class="main-content">
        <section class="data-inputs" v-if="!dataLoaded">
          <div class="input">
            <dataadb class="adbinput" ref="adb" :store="store" @dataReady="onDataReady" @statusChange="setStatus" />
          </div>
          <div class="input">
            <datainput class="fileinput" ref="input" :store="store" @dataReady="onDataReady" @statusChange="setStatus" />
          </div>
        </section>

        <section class="data-view">
          <div
            class="data-view-container"
            v-for="file in dataViewFiles"
            :key="file.filename"
          >
            <dataview
              :ref="file.filename"
              :store="store"
              :file="file"
              @click="onDataViewFocus(file)"
            />
          </div>

          <overlay
            :store="store"
            :ref="overlayRef"
            v-if="dataLoaded"
          />
        </section>
      </md-app-content>
    </md-app>
  </div>
</template>
<script>
import TreeView from './TreeView.vue'
import Overlay from './Overlay.vue'
import Rects from './Rects.vue'
import DataView from './DataView.vue'
import DataInput from './DataInput.vue'
import LocalStore from './localstore.js'
import DataAdb from './DataAdb.vue'
import FileType from './mixins/FileType.js'
import SaveAsZip from './mixins/SaveAsZip'
import FocusedDataViewFinder from './mixins/FocusedDataViewFinder'
import {DIRECTION} from './utils/utils'
import {NAVIGATION_STYLE} from './utils/consts';

const APP_NAME = "Winscope";

export default {
  name: 'app',
  mixins: [FileType, SaveAsZip, FocusedDataViewFinder],
  data() {
    return {
      title: APP_NAME,
      activeDataView: null,
      store: LocalStore('app', {
        flattened: false,
        onlyVisible: false,
        displayDefaults: true,
        navigationStyle: NAVIGATION_STYLE.GLOBAL,
      }),
      overlayRef: "overlay",
    }
  },
  created() {
    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('scroll', this.onScroll);
    document.title = this.title;
  },
  destroyed() {
    window.removeEventListener('keydown', this.onKeyDown);
    window.removeEventListener('scroll', this.onScroll);
  },
  methods: {
    clear() {
      this.$store.commit('clearFiles');
    },
    onDataViewFocus(file) {
      this.$store.commit('setActiveFile', file);
      this.activeDataView = file.filename;
    },
    onKeyDown(event) {
      event = event || window.event;
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
    onDataReady(files) {
      this.$store.dispatch('setFiles', files);
      this.updateFocusedView();
    },
    setStatus(status) {
      if (status) {
        this.title = status;
      } else {
        this.title = APP_NAME;
      }
    },
  },
  computed: {
    files() {
      return this.$store.getters.sortedFiles;
    },
    prettyDump() {
      return JSON.stringify(this.dump, null, 2);
    },
    dataLoaded() {
      return this.files.length > 0;
    },
    activeView() {
      if (!this.activeDataView && this.files.length > 0) {
        this.activeDataView = this.files[0].filename;
      }
      return this.activeDataView;
    },
    dataViewFiles() {
      return this.files.filter(f => this.hasDataView(f));
    }
  },
  watch: {
    title() {
      document.title = this.title;
    }
  },
  components: {
    overlay: Overlay,
    dataview: DataView,
    datainput: DataInput,
    dataadb: DataAdb,
  }
};
</script>
<style>
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@600&display=swap');

#app .md-app-container {
  transform: none!important; /* Get rid of tranforms which prevent fixed position from being used */
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
  padding-bottom: 75px; /* TODO: Disable if no bottom bar */
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

h1,
h2 {
  font-weight: normal;
}

ul {
  list-style-type: none;
  padding: 0;
}

a {
  color: #42b983;
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
</style>
