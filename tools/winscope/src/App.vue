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
      <md-app-toolbar md-tag="md-toolbar">
        <h1 class="md-title" style="flex: 1">{{title}}</h1>
        <md-button
          class="md-accent md-raised md-theme-default"
          @click="clear()"
          v-if="dataLoaded"
        >Clear</md-button>
      </md-app-toolbar>

      <md-app-content class="main-content">
        <div class="md-layout m-2" v-if="!dataLoaded">
          <dataadb ref="adb" :store="store" @dataReady="onDataReady" @statusChange="setStatus" />
          <datainput ref="input" :store="store" @dataReady="onDataReady" @statusChange="setStatus" />
        </div>
        <dataview
          v-for="file in files"
          :key="file.filename"
          :ref="file.filename"
          :store="store"
          :file="file"
          @click="onDataViewFocus(file)"
        />

        <overlay
          :store="store"
          :ref="overlayRef"
          v-if="dataLoaded"
        />
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

const APP_NAME = "Winscope";

export default {
  name: 'app',
  mixins: [FileType],
  data() {
    return {
      title: APP_NAME,
      activeDataView: null,
      store: LocalStore('app', {
        flattened: false,
        onlyVisible: false,
        displayDefaults: true,
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
      this.activeDataView = null;
      this.activeFile = null;
      this.video = null;
    },
    onDataViewFocus(file) {
      this.$store.commit('setActiveFile', file);
      this.activeDataView = file.filename;
    },
    onKeyDown(event) {
      event = event || window.event;
      if (event.keyCode == 37 /* left */ ) {
        this.$store.dispatch('advanceTimeline', -1, this.$store.state.excludeFromTimeline);
      } else if (event.keyCode == 39 /* right */ ) {
        this.$store.dispatch('advanceTimeline', 1, this.$store.state.excludeFromTimeline);
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
      return this.$store.state.files;
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
#app .md-app-container {
  transform: none!important; /* Get rid of tranforms which prevent fixed position from being used */
}

.main-content {
  padding-bottom: 75px;
}

.main-content .md-layout>* {
  margin: 1em;
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
</style>
