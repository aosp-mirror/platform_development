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
    <md-card style="min-width: 50em">
      <md-card-header>
        <div class="md-title">Open files</div>
      </md-card-header>
      <md-card-content>
        <md-list>
          <md-list-item v-for="file in dataFiles" v-bind:key="file.filename">
            <md-icon>{{file.type.icon}}</md-icon>
            <span class="md-list-item-text">{{file.filename}} ({{file.type.name}})</span>
            <md-button class="md-icon-button md-accent" @click="onRemoveFile(file.type.name)">
              <md-icon>close</md-icon>
            </md-button>
          </md-list-item>
        </md-list>
        <div>
          <md-checkbox v-model="store.displayDefaults">Show default properties
            <md-tooltip md-direction="bottom">
              If checked, shows the value of all properties.
              Otherwise, hides all properties whose value is the default for its data type.
            </md-tooltip>
          </md-checkbox>
        </div>
        <div class="md-layout">
          <div class="md-layout-item md-small-size-100">
            <md-select v-model="fileType" id="file-type" placeholder="File type">
              <md-option value="auto">Detect type</md-option>
              <md-option :value="k" v-for="(v,k) in FILE_TYPES" v-bind:key="v.name">{{v.name}}</md-option>
            </md-select>
          </div>
        </div>
        <div class="md-layout md-gutter">
          <input type="file" @change="onLoadFile" id="upload-file" v-show="false" />
          <label class="md-button md-accent md-raised md-theme-default" for="upload-file">Add File</label>
          <md-button v-if="dataReady" @click="onSubmit" class="md-button md-primary md-raised md-theme-default">Submit</md-button>
        </div>
      </md-card-content>
    </md-card>
</template>
<script>
import { detectAndDecode, FILE_TYPES, DATA_TYPES } from './decode.js'

export default {
  name: 'datainput',
  data() {
    return {
      FILE_TYPES,
      fileType: "auto",
      dataFiles: {},
    }
  },
  props: ['store'],
  methods: {
    onLoadFile(e) {
      var type = this.fileType;
      var files = event.target.files || event.dataTransfer.files;
      var file = files[0];
      if (!file) {
        // No file selected.
        return;
      }
      this.$emit('statusChange', file.name + " (loading)");

      var reader = new FileReader();
      reader.onload = (e) => {
        var buffer = new Uint8Array(e.target.result);
        try {
          if (FILE_TYPES[type]) {
            var filetype = FILE_TYPES[type];
            var data = filetype.decoder(buffer, filetype, file.name, this.store);
          } else {
            var [filetype, data] = detectAndDecode(buffer, file.name, this.store);
          }
        } catch (ex) {
          this.$emit('statusChange', this.filename + ': ' + ex);
          return;
        } finally {
          event.target.value = ''
        }

        this.$set(this.dataFiles, filetype.dataType.name, data);
        this.$emit('statusChange', null);
      }
      reader.readAsArrayBuffer(files[0]);
    },
    onRemoveFile(typeName) {
      this.$delete(this.dataFiles, typeName);
    },
    onSubmit() {
      this.$emit('dataReady', Object.keys(this.dataFiles).map(key => this.dataFiles[key]));
    }
  },
  computed: {
    dataReady: function() { return Object.keys(this.dataFiles).length > 0 }
  }
}

</script>
