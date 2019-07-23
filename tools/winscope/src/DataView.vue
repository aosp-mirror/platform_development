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
  <md-card v-if="file">
    <md-card-header>
      <div class="md-title">
        <md-icon>{{file.type.icon}}</md-icon> {{file.filename}}
      </div>
    </md-card-header>
    <traceview v-if="isTrace" :store="store" :file="file" ref="view" />
    <videoview v-if="isVideo" :file="file" ref="view" />
  </md-card>
</template>
<script>
import TraceView from './TraceView.vue'
import VideoView from './VideoView.vue'
import { DATA_TYPES } from './decode.js'

export default {
  name: 'dataview',
  data() {
    return {}
  },
  methods: {
    arrowUp() {
      return this.$refs.view.arrowUp();
    },
    arrowDown() {
      return this.$refs.view.arrowDown();
    },
  },
  props: ['store', 'file'],
  computed: {
    isTrace() {
      return this.file.type == DATA_TYPES.WINDOW_MANAGER ||
          this.file.type == DATA_TYPES.SURFACE_FLINGER || this.file.type == DATA_TYPES.TRANSACTION;
    },
    isVideo() {
      return this.file.type == DATA_TYPES.SCREEN_RECORDING;
    }
  },
  components: {
    'traceview': TraceView,
    'videoview': VideoView,
  }
}

</script>
