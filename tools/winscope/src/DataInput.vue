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
  <flat-card style="min-width: 50em">
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
      <md-progress-spinner :md-diameter="30" :md-stroke="3" md-mode="indeterminate" v-show="loadingFiles"/>
      <div>
        <md-checkbox v-model="store.displayDefaults" class="md-primary">
          Show default properties
          <md-tooltip md-direction="bottom">
            If checked, shows the value of all properties.
            Otherwise, hides all properties whose value is the default for its data type.
          </md-tooltip>
        </md-checkbox>
      </div>
      <div class="md-layout">
        <div class="md-layout-item md-small-size-100">
          <md-field>
          <md-select v-model="fileType" id="file-type" placeholder="File type">
            <md-option value="auto">Detect type</md-option>
            <md-option value="bugreport">Bug Report (.zip)</md-option>
            <md-option :value="k" v-for="(v,k) in FILE_TYPES" v-bind:key="v.name">{{v.name}}</md-option>
          </md-select>
          </md-field>
        </div>
      </div>
      <div class="md-layout">
        <input type="file" @change="onLoadFile" ref="fileUpload" v-show="false" :multiple="fileType === 'auto'" />
        <md-button class="md-primary md-theme-default" @click="$refs.fileUpload.click()">Add File</md-button>
        <md-button v-if="dataReady" @click="onSubmit" class="md-button md-primary md-raised md-theme-default">Submit</md-button>
      </div>
    </md-card-content>
  </flat-card>
</template>
<script>
import FlatCard from './components/FlatCard.vue';
import JSZip from 'jszip';
import { detectAndDecode, FILE_TYPES, DATA_TYPES } from './decode.js';
import { WebContentScriptMessageType } from './utils/consts';

// Add any file that should be considered when extracting a bug report here
// NOTE: If two files have the same type, the last one will be selected
const BUG_REPORT_FILES = [
  "proto/SurfaceFlinger_CRITICAL.proto",
  "proto/window_CRITICAL.proto",
  "FS/data/misc/wmtrace/layers_trace.pb",
  "FS/data/misc/wmtrace/wm_log.pb",
  "FS/data/misc/wmtrace/wm_trace.pb",
];

export default {
  name: 'datainput',
  data() {
    return {
      FILE_TYPES,
      fileType: "auto",
      dataFiles: {},
      loadingFiles: false,
    }
  },
  props: ['store'],
  created() {
    // Attempt to load files from extension if present
    this.loadFilesFromExtension();
  },
  methods: {
    getLoadingStatusAnimation(message) {
      let frame = 0;
      const fetchingStatusAnimation = () => {
        frame++;
        this.$emit('statusChange', `${message}${'.'.repeat(frame % 4)}`);
      };
      let interval = undefined;

      return Object.freeze({
        start: () => {
          interval = setInterval(fetchingStatusAnimation, 500);
        },
        stop: () => {
          clearInterval(interval);
        },
      });
    },
    /**
     * Attempt to load files from the extension if present.
     *
     * If the source URL parameter is set to the extension it make a request
     * to the extension to fetch the files from the extension.
     */
    loadFilesFromExtension() {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('source') === 'openFromExtension' && chrome) {
        // Fetch files from extension
        const androidBugToolExtensionId = "mbbaofdfoekifkfpgehgffcpagbbjkmj";

        const loading = this.getLoadingStatusAnimation('Fetching files');
        loading.start();

        // Request to convert the blob object url "blob:chrome-extension://xxx"
        // the chrome extension has to a web downloadable url "blob:http://xxx".
        chrome.runtime.sendMessage(androidBugToolExtensionId, {
          action: WebContentScriptMessageType.CONVERT_OBJECT_URL
        }, async (response) => {
          switch (response.action) {
            case WebContentScriptMessageType.CONVERT_OBJECT_URL_RESPONSE:
              if (response.attachments?.length > 0) {
                const filesBlobPromises = response.attachments.map(async attachment => {
                  const fileQueryResponse = await fetch(attachment.objectUrl);
                  const blob = await fileQueryResponse.blob();

                  /**
                   * Note: The blob's media type is not correct. It is always set to "image/png".
                   * Context: http://google3/javascript/closure/html/safeurl.js?g=0&l=256&rcl=273756987
                   */

                  // Clone blob to clear media type.
                  const file = new Blob([blob]);
                  file.name = attachment.name;

                  return file;
                });

                const files = await Promise.all(filesBlobPromises);

                loading.stop();
                this.processFiles(files);
              } else {
                console.warn("Got no attachements from extension...");
              }
              break;

            default:
              loading.stop();
              console.warn("Received unhandled response code from extension.");
          }
        });
      }
    },
    onLoadFile(e) {
      // Clear status to avoid keeping status of previous failed uploads
      this.$emit('statusChange', null);

      const files = event.target.files || event.dataTransfer.files;
      this.processFiles(files);
    },
    async processFiles(files) {
      let error;
      const decodedFiles = [];
      for (const file of files) {
        try {
          this.loadingFiles = true;
          this.$emit('statusChange', file.name + " (loading)");
          const result = await this.addFile(file);
          decodedFiles.push(...result);
          this.$emit('statusChange', null);
        } catch(e) {
          this.$emit('statusChange', `${file.name}: ${e}`);
          console.error(e);
          error = e;
          break;
        } finally {
          this.loadingFiles = false;
        }
      }

      event.target.value = '';

      if (error) {
        return;
      }

      for (const decodedFile of decodedFiles) {
        this.$set(this.dataFiles,
          decodedFile.filetype.dataType.name, decodedFile.data);
      }
    },
    getFileExtensions(file) {
      const split = file.name.split('.');
      if (split.length > 1) {
        return split.pop();
      }

      return undefined;
    },
    async addFile(file) {
      const decodedFiles = [];
      const type = this.fileType;

      const extension = this.getFileExtensions(file);

      // extension === 'zip' is required on top of file.type === 'application/zip' because when
      // loaded from the extension the type is incorrect. See comment in loadFilesFromExtension()
      // for more information.
      if (type === 'bugreport' ||
          (type === 'auto' && (extension === 'zip' || file.type === 'application/zip'))) {
        const results = await this.decodeCompressedBugReport(file);
        decodedFiles.push(...results);
      } else {
        const decodedFile = await this.decodeFile(file);
        decodedFiles.push(decodedFile);
      }

      return decodedFiles;
    },
    readFile(file) {
      return new Promise((resolve, _) => {
        const reader = new FileReader();
        reader.onload = async (e) => {
          const buffer = new Uint8Array(e.target.result);
          resolve(buffer);
        };
        reader.readAsArrayBuffer(file);
      });
    },
    async decodeFile(file) {
      const type = this.fileType;
      const buffer = await this.readFile(file);

      let filetype, data;
      if (FILE_TYPES[type]) {
        filetype = FILE_TYPES[type];
        data = filetype.decoder(buffer, filetype, file.name, this.store);
      } else {
        [filetype, data] = detectAndDecode(buffer, file.name, this.store);
      }

      return {filetype, data};
    },
    async decodeCompressedBugReport(file) {
      const buffer = await this.readFile(file);

      const zip = new JSZip();
      const content = await zip.loadAsync(buffer);

      console.log("ZIP CONTENT", content);

      const decodedFiles = [];
      for (const filename of BUG_REPORT_FILES) {
        const file = content.files[filename];
        if (file) {
          const fileBlob = await file.async("blob");
          fileBlob.name = filename;

          const decodedFile = await this.decodeFile(fileBlob);
          decodedFiles.push(decodedFile);
        }
      }

      return decodedFiles;
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
  },
  components: {
    'flat-card': FlatCard,
  },
}

</script>
