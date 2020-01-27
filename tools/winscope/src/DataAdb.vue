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
      <div class="md-title">ADB Connect</div>
    </md-card-header>
    <md-card-content v-if="status === STATES.CONNECTING">
      <md-spinner md-indeterminate></md-spinner>
    </md-card-content>
    <md-card-content v-if="status === STATES.NO_PROXY">
      <md-icon class="md-accent">error</md-icon>
      <span class="md-subheading">Unable to connect to Winscope ADB proxy</span>
      <div class="md-body-2">
        <p>Launch the Winscope ADB Connect proxy to capture traces directly from your browser.</p>
        <p>Python 3.5+ and ADB is required.</p>
        <p>Run:</p>
        <pre>python3 $ANDROID_BUILD_TOP/development/tools/winscope/adb_proxy/winscope_proxy.py</pre>
        <p>Or get it from the AOSP repository.</p>
      </div>
      <div class="md-layout md-gutter">
        <md-button class="md-accent md-raised" :href="downloadProxyUrl">Download from AOSP</md-button>
        <md-button class="md-raised md-accent" @click="restart">Retry</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.INVALID_VERSION">
      <md-icon class="md-accent">update</md-icon>
      <span class="md-subheading">The version of Winscope ADB Connect proxy running on your machine is incopatibile with Winscope.</span>
      <div class="md-body-2">
        <p>Please update the proxy to version {{ WINSCOPE_PROXY_VERSION }}</p>
        <p>Run:</p>
        <pre>python3 $ANDROID_BUILD_TOP/development/tools/winscope/adb_proxy/winscope_proxy.py</pre>
        <p>Or get it from the AOSP repository.</p>
      </div>
      <div class="md-layout md-gutter">
        <md-button class="md-accent md-raised" :href="downloadProxyUrl">Download from AOSP</md-button>
        <md-button class="md-raised md-accent" @click="restart">Retry</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.UNAUTH">
      <md-icon class="md-accent">lock</md-icon>
      <span class="md-subheading">Proxy authorisation required</span>
      <md-input-container>
        <label>Enter Winscope proxy token</label>
        <md-input v-model="adbStore.proxyKey"></md-input>
      </md-input-container>
      <div class="md-body-2">The proxy token is printed to console on proxy launch, copy and paste it above.</div>
      <div class="md-layout md-gutter">
        <md-button class="md-accent md-raised" @click="restart">Connect</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.DEVICES">
      <div class="md-subheading">{{ Object.keys(devices).length > 0 ? "Connected devices:" : "No devices detected" }}</div>
      <md-list>
        <md-list-item v-for="(device, id) in devices" :key="id" @click="selectDevice(id)" :disabled="!device.authorised">
          <md-icon>{{ device.authorised ? "smartphone" : "screen_lock_portrait" }}</md-icon><span>{{ device.authorised ? device.model : "unauthorised" }} ({{ id }})</span>
        </md-list-item>
      </md-list>
      <md-spinner :md-size="30" md-indeterminate></md-spinner>
    </md-card-content>
    <md-card-content v-if="status === STATES.START_TRACE">
      <md-list>
        <md-list-item>
          <md-icon>smartphone</md-icon><span>{{ devices[selectedDevice].model }} ({{ selectedDevice }})</span>
        </md-list-item>
      </md-list>
      <div>
        <p>Trace targets:</p>
        <md-checkbox v-for="file in TRACE_FILES" :key="file" v-model="adbStore[file]">{{FILE_TYPES[file].name}}</md-checkbox>
      </div>
      <div>
        <p>Dump targets:</p>
        <md-checkbox v-for="file in DUMP_FILES" :key="file" v-model="adbStore[file]">{{FILE_TYPES[file].name}}</md-checkbox>
      </div>
      <div class="md-layout md-gutter">
        <md-button class="md-accent md-raised" @click="startTrace">Start trace</md-button>
        <md-button class="md-accent md-raised" @click="dumpState">Dump state</md-button>
        <md-button class="md-raised" @click="resetLastDevice">Device list</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.ERROR">
      <md-icon class="md-accent">error</md-icon>
      <span class="md-subheading">Error:</span>
      <pre>
        {{ errorText }}
      </pre>
      <md-button class="md-raised md-accent" @click="restart">Retry</md-button>
    </md-card-content>
    <md-card-content v-if="status === STATES.END_TRACE">
      <span class="md-subheading">Tracing...</span>
      <md-progress md-indeterminate></md-progress>
      <div class="md-layout md-gutter">
        <md-button class="md-accent md-raised" @click="endTrace">End trace</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.LOAD_DATA">
      <span class="md-subheading">Loading data...</span>
      <md-progress :md-progress="loadProgress"></md-progress>
    </md-card-content>
  </md-card>
</template>
<script>
import { FILE_TYPES, DATA_TYPES } from './decode.js'
import LocalStore from './localstore.js'

const STATES = {
  ERROR: 0,
  CONNECTING: 1,
  NO_PROXY: 2,
  INVALID_VERSION: 3,
  UNAUTH: 4,
  DEVICES: 5,
  START_TRACE: 6,
  END_TRACE: 7,
  LOAD_DATA: 8,
}

const WINSCOPE_PROXY_VERSION = "0.5"
const WINSCOPE_PROXY_URL = "http://localhost:5544"
const PROXY_ENDPOINTS = {
  DEVICES: "/devices/",
  START_TRACE: "/start/",
  END_TRACE: "/end/",
  DUMP: "/dump/",
  FETCH: "/fetch/",
  STATUS: "/status/",
}
const TRACE_FILES = [
  "window_trace",
  "layers_trace",
  "screen_recording",
  "transaction",
  "proto_log"
]
const DUMP_FILES = [
  "window_dump",
  "layers_dump"
]
const CAPTURE_FILES = TRACE_FILES.concat(DUMP_FILES)

export default {
  name: 'dataadb',
  data() {
    return {
      STATES,
      TRACE_FILES,
      DUMP_FILES,
      CAPTURE_FILES,
      FILE_TYPES,
      WINSCOPE_PROXY_VERSION,
      status: STATES.CONNECTING,
      dataFiles: [],
      devices: {},
      selectedDevice: '',
      refresh_worker: null,
      keep_alive_worker: null,
      errorText: '',
      loadProgress: 0,
      adbStore: LocalStore('adb', Object.assign({
        proxyKey: '',
        lastDevice: '',
      }, CAPTURE_FILES.reduce(function(obj, key) { obj[key] = true; return obj }, {}))),
      downloadProxyUrl: 'https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py',
    }
  },
  props: ["store"],
  methods: {
    getDevices() {
      if (this.status !== STATES.DEVICES && this.status !== STATES.CONNECTING) {
        clearInterval(this.refresh_worker);
        this.refresh_worker = null;
        return;
      }
      this.callProxy("GET", PROXY_ENDPOINTS.DEVICES, this, function(request, view) {
        try {
          view.devices = JSON.parse(request.responseText);
          if (view.adbStore.lastDevice && view.devices[view.adbStore.lastDevice] && view.devices[view.adbStore.lastDevice].authorised) {
            view.selectDevice(view.adbStore.lastDevice)
          } else {
            if (view.refresh_worker === null) {
              view.refresh_worker = setInterval(view.getDevices, 1000)
            }
            view.status = STATES.DEVICES;
          }
        } catch (err) {
          view.errorText = request.responseText;
          view.status = STATES.ERROR;
        }
      })
    },
    keepAliveTrace() {
      if (this.status !== STATES.END_TRACE) {
        clearInterval(this.keep_alive_worker);
        this.keep_alive_worker = null;
        return;
      }
      this.callProxy("GET", PROXY_ENDPOINTS.STATUS + this.deviceId() + "/", this, function(request, view) {
        if (request.responseText !== "True") {
          view.endTrace();
        } else if (view.keep_alive_worker === null) {
          view.keep_alive_worker = setInterval(view.keepAliveTrace, 1000)
        }
      })
    },
    startTrace() {
      const requested = this.toTrace()
      if (requested.length < 1) {
        this.errorText = "No targets selected";
        this.status = STATES.ERROR;
        return
      }
      this.status = STATES.END_TRACE;
      this.callProxy("POST", PROXY_ENDPOINTS.START_TRACE + this.deviceId() + "/", this, function(request, view) {
        view.keepAliveTrace();
      }, null, requested)
    },
    dumpState() {
      const requested = this.toDump()
      if (requested.length < 1) {
        this.errorText = "No targets selected";
        this.status = STATES.ERROR;
        return
      }
      this.status = STATES.LOAD_DATA;
      this.callProxy("POST", PROXY_ENDPOINTS.DUMP + this.deviceId() + "/", this, function(request, view) {
        view.loadFile(requested, 0);
      }, null, requested)
    },
    endTrace() {
      this.status = STATES.LOAD_DATA;
      this.callProxy("POST", PROXY_ENDPOINTS.END_TRACE + this.deviceId() + "/", this, function(request, view) {
        view.loadFile(view.toTrace(), 0);
      })
    },
    loadFile(files, idx) {
      this.callProxy("GET", PROXY_ENDPOINTS.FETCH + this.deviceId() + "/" + files[idx] + "/", this, function(request, view) {
        try {
          var buffer = new Uint8Array(request.response);
          var filetype = FILE_TYPES[files[idx]];
          var data = filetype.decoder(buffer, filetype, filetype.name, view.store);
          view.dataFiles.push(data)
          view.loadProgress = 100 * (idx + 1) / files.length;
          if (idx < files.length - 1) {
            view.loadFile(files, idx + 1)
          } else {
            view.$emit('dataReady', view.dataFiles);
          }
        } catch (err) {
          view.errorText = err;
          view.status = STATES.ERROR;
        }
      }, "arraybuffer")
    },
    toTrace() {
      return TRACE_FILES.filter(file => this.adbStore[file]);
    },
    toDump() {
      return DUMP_FILES.filter(file => this.adbStore[file]);
    },
    selectDevice(device_id) {
      this.selectedDevice = device_id;
      this.adbStore.lastDevice = device_id;
      this.status = STATES.START_TRACE;
    },
    deviceId() {
      return this.selectedDevice;
    },
    restart() {
      this.status = STATES.CONNECTING;
    },
    resetLastDevice() {
      this.adbStore.lastDevice = '';
      this.restart()
    },
    callProxy(method, path, view, onSuccess, type, jsonRequest) {
      var request = new XMLHttpRequest();
      var view = this;
      request.onreadystatechange = function() {
        if (this.readyState !== 4) {
          return;
        }
        if (this.status === 0) {
          view.status = STATES.NO_PROXY;
        } else if (this.status === 200) {
          if (this.getResponseHeader("Winscope-Proxy-Version") !== WINSCOPE_PROXY_VERSION) {
            view.status = STATES.INVALID_VERSION;
          } else {
            onSuccess(this, view)
          }
        } else if (this.status === 403) {
          view.status = STATES.UNAUTH;
        } else {
          if (this.responseType === "text" || !this.responseType) {
            view.errorText = this.responseText;
          } else if (this.responseType === "arraybuffer") {
            view.errorText = String.fromCharCode.apply(null, new Uint8Array(this.response));
          }
          view.status = STATES.ERROR;
        }
      }
      request.responseType = type || "";
      request.open(method, WINSCOPE_PROXY_URL + path);
      request.setRequestHeader("Winscope-Token", this.adbStore.proxyKey);
      if (jsonRequest) {
        const json = JSON.stringify(jsonRequest)
        request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        request.send(json)
      } else {
        request.send();
      }
    }
  },
  created() {
    var urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("token")) {
      this.adbStore.proxyKey = urlParams.get("token")
    }
    this.getDevices();
  },
  watch: {
    status: {
      handler(st) {
        if (st == STATES.CONNECTING) {
          this.getDevices();
        }
      }
    }
  },
}

</script>
