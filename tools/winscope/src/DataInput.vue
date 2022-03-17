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
<div @dragleave="fileDragOut" @dragover="fileDragIn" @drop="handleFileDrop">
  <flat-card style="min-width: 50em">
    <md-card-header>
      <div class="md-title">Open files</div>
    </md-card-header>
    <md-card-content>
      <div class="dropbox" @click="$refs.fileUpload.click()" ref="dropbox">
        <md-list
          class="uploaded-files"
          v-show="Object.keys(dataFiles).length > 0"
        >
          <md-list-item v-for="file in dataFiles" v-bind:key="file.filename">
            <md-icon>{{FILE_ICONS[file.type]}}</md-icon>
            <span class="md-list-item-text">{{file.filename}} ({{file.type}})
            </span>
            <md-button
              class="md-icon-button md-accent"
              @click="e => {
                e.stopPropagation()
                onRemoveFile(file.type)
              }"
            >
              <md-icon>close</md-icon>
            </md-button>
          </md-list-item>
        </md-list>
        <div class="progress-spinner-wrapper" v-show="loadingFiles">
          <md-progress-spinner
            :md-diameter="30"
            :md-stroke="3"
            md-mode="indeterminate"
            class="progress-spinner"
          />
        </div>
        <input
          type="file"
          @change="onLoadFile"
          v-on:drop="handleFileDrop"
          ref="fileUpload"
          id="dropzone"
          v-show="false"
          multiple
        />
          <p v-if="!dataReady && !loadingFiles">
            Drag your <b>.winscope</b> or <b>.zip</b> file(s) or click here to begin
          </p>
        </div>

      <div class="md-layout">
        <div class="md-layout-item md-small-size-100">
          <md-field>
          <md-select v-model="fileType" id="file-type" placeholder="File type">
            <md-option value="auto">Detect type</md-option>
            <md-option value="bugreport">Bug Report (.zip)</md-option>
            <md-option
              :value="k" v-for="(v,k) in FILE_DECODERS"
              v-bind:key="v.name">{{v.name}}
            ></md-option>
          </md-select>
          </md-field>
        </div>
      </div>
      <div class="md-layout">
        <md-button
          class="md-primary md-theme-default"
          @click="$refs.fileUpload.click()"
        >
          Add File
        </md-button>
        <md-button
          v-if="dataReady"
          @click="onSubmit"
          class="md-button md-primary md-raised md-theme-default"
        >
          Submit
        </md-button>
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
      <p class="snackbar-break-words">{{ snackbarText }}</p>
      <div @click="hideSnackbarMessage()">
        <md-button class="md-icon-button">
          <md-icon style="color: white">close</md-icon>
        </md-button>
      </div>
    </md-snackbar>
  </flat-card>
</div>
</template>
<script>
import FlatCard from './components/FlatCard.vue';
import JSZip from 'jszip';
import {
  detectAndDecode,
  FILE_TYPES,
  FILE_DECODERS,
  FILE_ICONS,
  UndetectableFileType,
} from './decode.js';
import {WebContentScriptMessageType} from './utils/consts';

export default {
  name: 'datainput',
  data() {
    return {
      FILE_TYPES,
      FILE_DECODERS,
      FILE_ICONS,
      fileType: 'auto',
      dataFiles: {},
      loadingFiles: false,
      showFetchingSnackbar: false,
      showSnackbar: false,
      snackbarDuration: 3500,
      snackbarText: '',
      fetchingSnackbarText: 'Fetching files...',
      traceName: undefined,
    };
  },
  props: ['store'],
  created() {
    // Attempt to load files from extension if present
    this.loadFilesFromExtension();
  },
  mounted() {
    this.handleDropboxDragEvents();
  },
  beforeUnmount() {

  },
  methods: {
    showSnackbarMessage(message, duration) {
      this.snackbarText = '\n' + message + '\n';
      this.snackbarDuration = duration;
      this.showSnackbar = true;
    },
    hideSnackbarMessage() {
      this.showSnackbar = false;
      this.recordButtonClickedEvent("Hide Snackbar Message")
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
    handleDropboxDragEvents() {
      // Counter used to keep track of when we actually exit the dropbox area
      // When we drag over a child of the dropbox area the dragenter event will
      // be called again and subsequently the dragleave so we don't want to just
      // remove the class on the dragleave event.
      let dropboxDragCounter = 0;

      console.log(this.$refs["dropbox"])

      this.$refs["dropbox"].addEventListener('dragenter', e => {
        dropboxDragCounter++;
        this.$refs["dropbox"].classList.add('dragover');
      });

      this.$refs["dropbox"].addEventListener('dragleave', e => {
        dropboxDragCounter--;
        if (dropboxDragCounter == 0) {
          this.$refs["dropbox"].classList.remove('dragover');
        }
      });

      this.$refs["dropbox"].addEventListener('drop', e => {
        dropboxDragCounter = 0;
        this.$refs["dropbox"].classList.remove('dragover');
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
        const androidBugToolExtensionId = 'mbbaofdfoekifkfpgehgffcpagbbjkmj';

        const loading = this.getFetchFilesLoadingAnimation();
        loading.start();

        // Request to convert the blob object url "blob:chrome-extension://xxx"
        // the chrome extension has to a web downloadable url "blob:http://xxx".
        chrome.runtime.sendMessage(androidBugToolExtensionId, {
          action: WebContentScriptMessageType.CONVERT_OBJECT_URL,
        }, async (response) => {
          switch (response.action) {
            case WebContentScriptMessageType.CONVERT_OBJECT_URL_RESPONSE:
              if (response.attachments?.length > 0) {
                const filesBlobPromises = response.attachments
                    .map(async (attachment) => {
                      const fileQueryResponse =
                        await fetch(attachment.objectUrl);
                      const blob = await fileQueryResponse.blob();

                      /**
                       * Note: The blob's media type is not correct.
                       * It is always set to "image/png".
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
                const failureMessages = 'Got no attachements from extension...';
                console.warn(failureMessages);
                this.showSnackbarMessage(failureMessages, 3500);
              }
              break;

            default:
              loading.stop();
              const failureMessages =
                'Received unhandled response code from extension.';
              console.warn(failureMessages);
              this.showSnackbarMessage(failureMessages, 3500);
          }
        });
      }
    },
    fileDragIn(e) {
      e.preventDefault();
    },
    fileDragOut(e) {
      e.preventDefault();
    },
    handleFileDrop(e) {
      e.preventDefault();
      let droppedFiles = e.dataTransfer.files;
      if(!droppedFiles) return;
      // Record analytics event
      this.recordDragAndDropFileEvent(droppedFiles);

      this.processFiles(droppedFiles);
    },
    onLoadFile(e) {
      const files = event.target.files || event.dataTransfer.files;
      this.recordFileUploadEvent(files);
      this.processFiles(files);
    },
    async processFiles(files) {
      console.log("Object.keys(this.dataFiles).length", Object.keys(this.dataFiles).length)
      // The trace name to use if we manage to load the archive without errors.
      let tmpTraceName;

      if (Object.keys(this.dataFiles).length > 0) {
        // We have already loaded some files so only want to use the name of
        // this archive as the name of the trace if we override all loaded files
      } else {
        // No files have been uploaded yet so if we are uploading only 1 archive
        // we want to use it's name as the trace name
        if (files.length == 1 && this.isArchive(files[0])) {
          tmpTraceName = this.getFileNameWithoutZipExtension(files[0])
        }
      }

      let error;
      const decodedFiles = [];
      for (const file of files) {
        try {
          this.loadingFiles = true;
          this.showSnackbarMessage(`Loading ${file.name}`, Infinity);
          const result = await this.addFile(file);
          decodedFiles.push(...result);
          this.hideSnackbarMessage();
        } catch (e) {
          this.showSnackbarMessage(
              `Failed to load '${file.name}'...\n${e}`, 5000);
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

      // TODO: Handle the fact that we can now have multiple files of type
      // FILE_TYPES.TRANSACTION_EVENTS_TRACE

      const decodedFileTypes = new Set(Object.keys(this.dataFiles));
      // A file is overridden if a file of the same type is upload twice, as
      // Winscope currently only support at most one file to each type
      const overriddenFileTypes = new Set();
      const overriddenFiles = {}; // filetype => array of files
      for (const decodedFile of decodedFiles) {
        const dataType = decodedFile.filetype;

        if (decodedFileTypes.has(dataType)) {
          overriddenFileTypes.add(dataType);
          (overriddenFiles[dataType] = overriddenFiles[dataType] || [])
              .push(this.dataFiles[dataType]);
        }
        decodedFileTypes.add(dataType);

        const frozenData = Object.freeze(decodedFile.data.data);
        delete decodedFile.data.data;
        decodedFile.data.data = frozenData;

        this.$set(this.dataFiles,
            dataType, Object.freeze(decodedFile.data));
      }

      // TODO(b/169305853): Remove this once we have magic numbers or another
      // way to detect the file type more reliably.
      for (const dataType in overriddenFiles) {
        if (overriddenFiles.hasOwnProperty(dataType)) {
          const files = overriddenFiles[dataType];
          files.push(this.dataFiles[dataType]);

          const selectedFile =
              this.getMostLikelyCandidateFile(dataType, files);
          if (selectedFile.data) {
            selectedFile.data = Object.freeze(selectedFile.data);
          }

          this.$set(this.dataFiles, dataType, Object.freeze(selectedFile));

          // Remove selected file from overriden list
          const index = files.indexOf(selectedFile);
          files.splice(index, 1);
        }
      }

      if (overriddenFileTypes.size > 0) {
        this.displayFilesOverridenWarning(overriddenFiles);
      }

      if (tmpTraceName !== undefined) {
        this.traceName = tmpTraceName;
      }
    },

    getFileNameWithoutZipExtension(file) {
      const fileNameSplitOnDot = file.name.split('.')
      if (fileNameSplitOnDot.slice(-1)[0] == 'zip') {
        return fileNameSplitOnDot.slice(0,-1).join('.');
      } else {
        return file.name;
      }
    },

    /**
     * Gets the file that is most likely to be the actual file of that type out
     * of all the candidateFiles. This is required because there are some file
     * types that have no magic number and may lead to false positives when
     * decoding in decode.js. (b/169305853)
     * @param {string} dataType - The type of the candidate files.
     * @param {files[]} candidateFiles - The list all the files detected to be
     *                                   of type dataType, passed in the order
     *                                   they are detected/uploaded in.
     * @return {file} - the most likely candidate.
     */
    getMostLikelyCandidateFile(dataType, candidateFiles) {
      const keyWordsByDataType = {
        [FILE_TYPES.WINDOW_MANAGER_DUMP]: 'window',
        [FILE_TYPES.SURFACE_FLINGER_DUMP]: 'surface',
      };

      if (
        !candidateFiles ||
        !candidateFiles.length ||
        candidateFiles.length == 0
      ) {
        throw new Error('No candidate files provided');
      }

      if (!keyWordsByDataType.hasOwnProperty(dataType)) {
        console.warn(`setMostLikelyCandidateFile doesn't know how to handle ` +
            `candidates of dataType ${dataType} – setting last candidate as ` +
            `target file.`);

        // We want to return the last candidate file so that, we always override
        // old uploaded files with once of the latest uploaded files.
        return candidateFiles.slice(-1)[0];
      }

      for (const file of candidateFiles) {
        if (file.filename
            .toLowerCase().includes(keyWordsByDataType[dataType])) {
          return file;
        }
      }

      // We want to return the last candidate file so that, we always override
      // old uploaded files with once of the latest uploaded files.
      return candidateFiles.slice(-1)[0];
    },

    /**
     * Display a snackbar warning that files have been overriden and any
     * relavant additional information in the logs.
     * @param {{string: file[]}} overriddenFiles - a mapping from data types to
     * the files of the of that datatype tha have been overriden.
     */
    displayFilesOverridenWarning(overriddenFiles) {
      const overriddenFileTypes = Object.keys(overriddenFiles);
      const overriddenCount = Object.values(overriddenFiles)
          .map((files) => files.length).reduce((length, next) => length + next);

      if (overriddenFileTypes.length === 1 && overriddenCount === 1) {
        const type = overriddenFileTypes.values().next().value;
        const overriddenFile = overriddenFiles[type][0].filename;
        const keptFile = this.dataFiles[type].filename;
        const message =
          `'${overriddenFile}' is conflicting with '${keptFile}'. ` +
          `Only '${keptFile}' will be kept. If you wish to display ` +
          `'${overriddenFile}', please upload it again with no other file ` +
          `of the same type.`;

        this.showSnackbarMessage(`WARNING: ${message}`, Infinity);
        console.warn(message);
      } else {
        const message = `Mutiple conflicting files have been uploaded. ` +
          `${overriddenCount} files have been discarded. Please check the ` +
          `developer console for more information.`;
        this.showSnackbarMessage(`WARNING: ${message}`, Infinity);

        const messageBuilder = [];
        for (const type of overriddenFileTypes.values()) {
          const keptFile = this.dataFiles[type].filename;
          const overriddenFilesCount = overriddenFiles[type].length;

          messageBuilder.push(`${overriddenFilesCount} file` +
              `${overriddenFilesCount > 1 ? 's' : ''} of type ${type} ` +
              `${overriddenFilesCount > 1 ? 'have' : 'has'} been ` +
              `overridden. Only '${keptFile}' has been kept.`);
        }

        messageBuilder.push('');
        messageBuilder.push('Please reupload the specific files you want ' +
          'to read (one of each type).');
        messageBuilder.push('');

        messageBuilder.push('===============DISCARDED FILES===============');

        for (const type of overriddenFileTypes.values()) {
          const discardedFiles = overriddenFiles[type];

          messageBuilder.push(`The following files of type ${type} ` +
            `have been discarded:`);
          for (const discardedFile of discardedFiles) {
            messageBuilder.push(`  - ${discardedFile.filename}`);
          }
          messageBuilder.push('');
        }

        console.warn(messageBuilder.join('\n'));
      }
    },

    getFileExtensions(file) {
      const split = file.name.split('.');
      if (split.length > 1) {
        return split.pop();
      }

      return undefined;
    },

    isArchive(file) {
      const type = this.fileType;

      const extension = this.getFileExtensions(file);

      // extension === 'zip' is required on top of file.type ===
      // 'application/zip' because when loaded from the extension the type is
      // incorrect. See comment in loadFilesFromExtension() for more
      // information.
      return type === 'bugreport' ||
          (type === 'auto' && (extension === 'zip' ||
            file.type === 'application/zip'))
    },

    async addFile(file) {
      const decodedFiles = [];

      if (this.isArchive(file)) {
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
        data = fileDecoder.decoder(
            buffer, fileDecoder.decoderParams, file.name, this.store);
      } else {
        // Defaulting to auto — will attempt to detect file type
        [filetype, data] = detectAndDecode(buffer, file.name, this.store);
      }

      return {filetype, data};
    },
    /**
     * Decode a zip file
     *
     * Load all files that can be decoded, even if some failures occur.
     * For example, a zip file with an mp4 recorded via MediaProjection
     * doesn't include the winscope metadata (b/140855415), but the trace
     * files within the zip should be nevertheless readable
     */
    async decodeArchive(archive) {
      const buffer = await this.readFile(archive);

      const zip = new JSZip();
      const content = await zip.loadAsync(buffer);

      const decodedFiles = [];

      let lastError;
      for (const filename in content.files) {
        const file = content.files[filename];
        if (file.dir) {
          // Ignore directories
          continue;
        }

        const fileBlob = await file.async('blob');
        // Get only filename and remove rest of path
        fileBlob.name = filename.split('/').slice(-1).pop();

        try {
          const decodedFile = await this.decodeFile(fileBlob);

          decodedFiles.push(decodedFile);
        } catch (e) {
          if (!(e instanceof UndetectableFileType)) {
            lastError = e;
          }

          console.error(e);
        }
      }

      if (decodedFiles.length == 0) {
        if (lastError) {
          throw lastError;
        }
        throw new Error('No matching files found in archive', archive);
      } else {
        if (lastError) {
          this.showSnackbarMessage(
            'Unable to parse all files, check log for more details', 3500);
        }
      }

      return decodedFiles;
    },
    onRemoveFile(typeName) {
      this.$delete(this.dataFiles, typeName);
    },
    onSubmit() {
      this.$emit('dataReady', this.formattedTraceName,
          Object.keys(this.dataFiles).map((key) => this.dataFiles[key]));
    },
  },
  computed: {
    dataReady: function() {
      return Object.keys(this.dataFiles).length > 0;
    },

    formattedTraceName() {
      if (this.traceName === undefined) {
        return 'winscope-trace';
      } else {
        return this.traceName;
      }
    }
  },
  components: {
    'flat-card': FlatCard,
  },
};

</script>
<style>
  .dropbox:hover, .dropbox.dragover {
      background: rgb(224, 224, 224);
    }

  .dropbox {
    outline: 2px dashed #448aff; /* the dash box */
    outline-offset: -10px;
    background: white;
    color: #448aff;
    padding: 10px 10px 10px 10px;
    min-height: 200px; /* minimum height */
    position: relative;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-items: center;
  }

  .dropbox p, .dropbox .progress-spinner-wrapper {
    font-size: 1.2em;
    margin: auto;
  }

  .progress-spinner-wrapper, .progress-spinner {
    width: fit-content;
    height: fit-content;
    display: block;
  }

  .progress-spinner-wrapper {
    padding: 1.5rem 0 1.5rem 0;
  }

  .dropbox .uploaded-files {
    background: none!important;
    width: 100%;
  }
</style>
