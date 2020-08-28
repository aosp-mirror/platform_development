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
          <md-icon>{{FILE_ICONS[file.type]}}</md-icon>
          <span class="md-list-item-text">{{file.filename}} ({{file.type}})</span>
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
            <md-option :value="k" v-for="(v,k) in FILE_DECODERS" v-bind:key="v.name">{{v.name}}</md-option>
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

    <md-snackbar
      md-position="center"
      :md-duration="Infinity"
      :md-active.sync="showFetchingSnackbar"
      md-persistent
    >
      <span>{{ fetchingSnackbarText }}</span>
    </md-snackbar>

    <md-snackbar
      md-position="center"
      :md-duration="snackbarDuration"
      :md-active.sync="showSnackbar"
      md-persistent
    >
      <span style="white-space: pre-line;">{{ snackbarText }}</span>
      <div @click="hideSnackbarMessage()">
        <md-button class="md-icon-button">
          <md-icon style="color: white">close</md-icon>
        </md-button>
      </div>
    </md-snackbar>
  </flat-card>
</template>
<script>
import FlatCard from './components/FlatCard.vue';
import JSZip from 'jszip';
import { detectAndDecode, FILE_TYPES, FILE_DECODERS, FILE_ICONS, UndetectableFileType } from './decode.js';
import { WebContentScriptMessageType } from './utils/consts';

export default {
  name: 'datainput',
  data() {
    return {
      FILE_TYPES,
      FILE_DECODERS,
      FILE_ICONS,
      fileType: "auto",
      dataFiles: {},
      loadingFiles: false,
      showFetchingSnackbar: false,
      showSnackbar: false,
      snackbarDuration: 3500,
      snackbarText: '',
      fetchingSnackbarText: "Fetching files...",
    }
  },
  props: ['store'],
  created() {
    // Attempt to load files from extension if present
    this.loadFilesFromExtension();
  },
  methods: {
    showSnackbarMessage(message, duration) {
      this.snackbarText = message;
      this.snackbarDuration = duration;
      this.showSnackbar = true;
    },
    hideSnackbarMessage() {
      this.showSnackbar = false;
    },
    getFetchFilesLoadingAnimation() {
      let frame = 0;
      const fetchingStatusAnimation = () => {
        frame++;
        this.fetchingSnackbarText = `Fetching files${'.'.repeat(frame % 4)}`;
      };
      let interval = undefined;

      return Object.freeze({
        start: () => {
          this.showFetchingSnackbar = true;
          interval = setInterval(fetchingStatusAnimation, 500);
        },
        stop: () => {
          this.showFetchingSnackbar = false;
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

        const loading = this.getFetchFilesLoadingAnimation();
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
                const failureMessages = "Got no attachements from extension...";
                console.warn(failureMessages);
                this.showSnackbarMessage(failureMessages, 3500);
              }
              break;

            default:
              loading.stop();
              const failureMessages = "Received unhandled response code from extension.";
              console.warn(failureMessages);
              this.showSnackbarMessage(failureMessages, 3500);
          }
        });
      }
    },
    onLoadFile(e) {
      const files = event.target.files || event.dataTransfer.files;
      this.processFiles(files);
    },
    async processFiles(files) {
      let error;
      const decodedFiles = [];
      for (const file of files) {
        try {
          this.loadingFiles = true;
          this.showSnackbarMessage(`Loading ${file.name}`, Infinity);
          const result = await this.addFile(file);
          decodedFiles.push(...result);
          this.hideSnackbarMessage();
        } catch(e) {
          this.showSnackbarMessage(`Failed to load '${file.name}'...\n${e}`, 5000);
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

      // TODO: Handle the fact that we can now have multiple files of type FILE_TYPES.TRANSACTION_EVENTS_TRACE

      const decodedFileTypes = new Set(Object.keys(this.dataFiles));
      // A file is overridden if a file of the same type is upload twice, as
      // Winscope currently only support at most one file to each type
      const overriddenFileTypes = new Set();
      const overriddenFiles = {}; // filetype => array of file names
      let overriddenCount = 0;
      for (const decodedFile of decodedFiles) {
        const dataType = decodedFile.filetype;

        if (decodedFileTypes.has(dataType)) {
          overriddenFileTypes.add(dataType);
          (overriddenFiles[dataType] = overriddenFiles[dataType] || [])
            .push(this.dataFiles[dataType].filename);
          overriddenCount++;
        }
        decodedFileTypes.add(dataType);

        this.$set(this.dataFiles,
          dataType, decodedFile.data);
      }

      if (overriddenFileTypes.size > 0) {
        if (overriddenFileTypes.size === 1 && overriddenCount === 1) {
          const type = overriddenFileTypes.values().next().value;
          const overriddenFile = overriddenFiles[type][0];
          const keptFile = this.dataFiles[type].filename;
          const message = `'${overriddenFile}' is conflicting with '${keptFile}'. Only '${keptFile}' will be kept. If you wish to display '${overriddenFile}', please upload it again with no other file of the same type.`;

          this.showSnackbarMessage(`WARNING: ${message}`, Infinity);
          console.warn(message);
        } else {
          const message = `Mutiple conflicting files have been uploaded. ${overriddenCount} files have been discarded. Please check the developer console for more information.`;
          this.showSnackbarMessage(`WARNING: ${message}`, Infinity);

          const messageBuilder = [];
          for (const type of overriddenFileTypes.values()) {
            const keptFile = this.dataFiles[type].filename;
            const overriddenFilesCount = overriddenFiles[type].length;

            messageBuilder.push(`${overriddenFilesCount} file${overriddenFilesCount > 1 ? 's' : ''} of type ${type} ${overriddenFilesCount > 1 ? 'have' : 'has'} been overridden. Only '${keptFile}' has been kept.`);
          }

          messageBuilder.push("");
          messageBuilder.push("Please reupload the specific files you want to read (one of each type).");
          messageBuilder.push("");

          messageBuilder.push("================DISCARDED FILES================");

          for (const type of overriddenFileTypes.values()) {
            const discardedFiles = overriddenFiles[type];
            const keptFile = this.dataFiles[type].filename;

            messageBuilder.push(`The following files of type ${type} have been discarded:`);
            for (const discardedFile of discardedFiles) {
              messageBuilder.push(`  - ${discardedFile}`);
            }
            messageBuilder.push("");
          }

          console.warn(messageBuilder.join("\n"));
        }
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
        const results = await this.decodeArchive(file);
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
      const buffer = await this.readFile(file);

      let filetype = this.filetype;
      let data;
      if (filetype) {
        const fileDecoder = FILE_DECODERS[filetype];
        data = fileDecoder.decoder(buffer, fileDecoder.decoderParams, file.name, this.store);
      } else {
        // Defaulting to auto — will attempt to detect file type
        [filetype, data] = detectAndDecode(buffer, file.name, this.store);
      }

      return {filetype, data};
    },
    async decodeArchive(archive) {
      const buffer = await this.readFile(archive);

      const zip = new JSZip();
      const content = await zip.loadAsync(buffer);

      const decodedFiles = [];

      for (const filename in content.files) {
        const file = content.files[filename];

        const fileBlob = await file.async("blob");
        fileBlob.name = filename;

        try {
          const decodedFile = await this.decodeFile(fileBlob);

          decodedFiles.push(decodedFile);
        } catch(e) {
          if (!(e instanceof UndetectableFileType)) {
            throw e;
          }
        }
      }

      if (decodedFiles.length == 0) {
        throw new Error("No matching files found in archive", archive);
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
