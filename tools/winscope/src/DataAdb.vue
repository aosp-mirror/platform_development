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
      <div class="md-title">ADB Connect</div>
    </md-card-header>
    <md-card-content v-if="status === STATES.CONNECTING">
      <md-progress-spinner md-indeterminate></md-progress-spinner>
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
      <div class="md-layout">
        <md-button class="md-accent" :href="downloadProxyUrl" @click="buttonClicked(`Download from AOSP`)">Download from AOSP</md-button>
        <md-button class="md-accent" @click="restart">Retry</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.INVALID_VERSION">
      <md-icon class="md-accent">update</md-icon>
      <span class="md-subheading">The version of Winscope ADB Connect proxy running on your machine is incopatibile with Winscope.</span>
      <div class="md-body-2">
        <p>Please update the proxy to version {{ proxyClient.VERSION }}</p>
        <p>Run:</p>
        <pre>python3 $ANDROID_BUILD_TOP/development/tools/winscope/adb_proxy/winscope_proxy.py</pre>
        <p>Or get it from the AOSP repository.</p>
      </div>
      <div class="md-layout">
        <md-button class="md-accent" :href="downloadProxyUrl">Download from AOSP</md-button>
        <md-button class="md-accent" @click="restart">Retry</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.UNAUTH">
      <md-icon class="md-accent">lock</md-icon>
      <span class="md-subheading">Proxy authorisation required</span>
      <md-field>
        <label>Enter Winscope proxy token</label>
        <md-input v-model="proxyClient.store.proxyKey"></md-input>
      </md-field>
      <div class="md-body-2">The proxy token is printed to console on proxy launch, copy and paste it above.</div>
      <div class="md-layout">
        <md-button class="md-primary" @click="restart">Connect</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.DEVICES">
      <div class="md-subheading">{{ Object.keys(proxyClient.devices).length > 0 ? "Connected devices:" : "No devices detected" }}</div>
      <md-list>
        <md-list-item v-for="(device, id) in proxyClient.devices" :key="id" @click="proxyClient.selectDevice(id)" :disabled="!device.authorised">
          <md-icon>{{ device.authorised ? "smartphone" : "screen_lock_portrait" }}</md-icon>
          <span class="md-list-item-text">{{ device.authorised ? device.model : "unauthorised" }} ({{ id }})</span>
        </md-list-item>
      </md-list>
      <md-progress-spinner :md-size="30" md-indeterminate></md-progress-spinner>
    </md-card-content>
    <md-card-content v-if="status === STATES.START_TRACE">
      <div class="device-choice">
        <md-list>
          <md-list-item>
            <md-icon>smartphone</md-icon>
            <span class="md-list-item-text">{{ proxyClient.devices[proxyClient.selectedDevice].model }} ({{ proxyClient.selectedDevice }})</span>
          </md-list-item>
        </md-list>
        <md-button class="md-primary" @click="resetLastDevice">Change device</md-button>
      </div>
      <div class="trace-section">
        <h3>Trace targets:</h3>
        <div class="selection">
          <md-checkbox class="md-primary" v-for="traceKey in Object.keys(DYNAMIC_TRACES)" :key="traceKey" v-model="traceStore[traceKey]">{{ DYNAMIC_TRACES[traceKey].name }}</md-checkbox>
        </div>
        <div class="trace-config">
            <h4>Surface Flinger config</h4>
            <div class="selection">
              <md-checkbox class="md-primary" v-for="config in TRACE_CONFIG['layers_trace']" :key="config" v-model="traceStore[config]">{{config}}</md-checkbox>
              <div class="selection">
                <md-field class="config-selection" v-for="selectConfig in Object.keys(SF_SELECTED_CONFIG)" :key="selectConfig">
                  <md-select v-model="SF_SELECTED_CONFIG_VALUES[selectConfig]" :placeholder="selectConfig">
                    <md-option value="">{{selectConfig}}</md-option>
                    <md-option v-for="option in SF_SELECTED_CONFIG[selectConfig]" :key="option" :value="option">{{ option }}</md-option>
                  </md-select>
                </md-field>
              </div>
            </div>
        </div>
        <div class="trace-config">
            <h4>Window Manager config</h4>
            <div class="selection">
              <md-field class="config-selection" v-for="selectConfig in Object.keys(WM_SELECTED_CONFIG)" :key="selectConfig">
                <md-select v-model="WM_SELECTED_CONFIG_VALUES[selectConfig]" :placeholder="selectConfig">
                  <md-option value="">{{selectConfig}}</md-option>
                  <md-option v-for="option in WM_SELECTED_CONFIG[selectConfig]" :key="option" :value="option">{{ option }}</md-option>
                </md-select>
              </md-field>
            </div>
        </div>
        <md-button class="md-primary trace-btn" @click="startTrace">Start trace</md-button>
      </div>
      <div class="dump-section">
        <h3>Dump targets:</h3>
        <div class="selection">
          <md-checkbox class="md-primary" v-for="dumpKey in Object.keys(DUMPS)" :key="dumpKey" v-model="traceStore[dumpKey]">{{DUMPS[dumpKey].name}}</md-checkbox>
        </div>
        <div class="md-layout">
          <md-button class="md-primary dump-btn" @click="dumpState">Dump state</md-button>
        </div>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.ERROR">
      <md-icon class="md-accent">error</md-icon>
      <span class="md-subheading">Error:</span>
      <pre>
        {{ errorText }}
      </pre>
      <md-button class="md-primary" @click="restart">Retry</md-button>
    </md-card-content>
    <md-card-content v-if="status === STATES.END_TRACE">
      <span class="md-subheading">Tracing...</span>
      <md-progress-bar md-mode="indeterminate"></md-progress-bar>
      <div class="md-layout">
        <md-button class="md-primary" @click="endTrace">End trace</md-button>
      </div>
    </md-card-content>
    <md-card-content v-if="status === STATES.LOAD_DATA">
      <span class="md-subheading">Loading data...</span>
      <md-progress-bar md-mode="determinate" :md-value="loadProgress"></md-progress-bar>
    </md-card-content>
  </flat-card>
</template>
<script>
import LocalStore from './localstore.js';
import FlatCard from './components/FlatCard.vue';
import {proxyClient, ProxyState, ProxyEndpoint} from './proxyclient/ProxyClient.ts';

// trace options should be added in a nested category
const TRACES = {
  'default': {
    'window_trace': {
      name: 'Window Manager',
    },
    'accessibility_trace': {
      name: 'Accessibility',
    },
    'layers_trace': {
      name: 'Surface Flinger',
    },
    'transactions': {
      name: 'Transaction',
    },
    'proto_log': {
      name: 'ProtoLog',
    },
    'screen_recording': {
      name: 'Screen Recording',
    },
    'ime_trace_clients': {
      name: 'Input Method Clients',
    },
    'ime_trace_service': {
      name: 'Input Method Service',
    },
    'ime_trace_managerservice': {
      name: 'Input Method Manager Service',
    },
  },
  'arc': {
    'wayland_trace': {
      name: 'Wayland',
    },
  },
};

const TRACE_CONFIG = {
  'layers_trace': [
    'composition',
    'metadata',
    'hwc',
    'tracebuffers',
  ],
};

const SF_SELECTED_CONFIG = {
  'sfbuffersize': [
    '4000',
    '8000',
    '16000',
    '32000',
  ],
};

const WM_SELECTED_CONFIG = {
  'wmbuffersize': [
    '4000',
    '8000',
    '16000',
    '32000',
  ],
  'tracingtype': [
    'frame',
    'transaction',
  ],
  'tracinglevel': [
    'verbose',
    'debug',
    'critical',
  ],
};

const DUMPS = {
  'window_dump': {
    name: 'Window Manager',
  },
  'layers_dump': {
    name: 'Surface Flinger',
  },
};

const CONFIGS = Object.keys(TRACE_CONFIG).flatMap((file) => TRACE_CONFIG[file]);

export default {
  name: 'dataadb',
  data() {
    return {
      proxyClient,
      ProxyState,
      STATES: ProxyState,
      TRACES,
      DYNAMIC_TRACES: TRACES['default'],
      TRACE_CONFIG,
      SF_SELECTED_CONFIG,
      WM_SELECTED_CONFIG,
      SF_SELECTED_CONFIG_VALUES: {},
      WM_SELECTED_CONFIG_VALUES: {},
      DUMPS,
      status: ProxyState.CONNECTING,
      dataFiles: [],
      keep_alive_worker: null,
      errorText: '',
      loadProgress: 0,
      traceStore: LocalStore(
          'trace',
          Object.assign(
              this.getAllTraceKeys(TRACES)
                  .concat(Object.keys(DUMPS))
                  .concat(CONFIGS)
                  .reduce(function(obj, key) {
                    obj[key] = true; return obj;
                  }, {}),
          ),
      ),
      downloadProxyUrl: 'https://android.googlesource.com/platform/development/+/master/tools/winscope/adb_proxy/winscope_proxy.py',
      onStateChangeFn: (newState, errorText) => {
        this.status = newState;
        this.errorText = errorText;
      },
    };
  },
  props: ['store'],
  components: {
    'flat-card': FlatCard,
  },
  methods: {
    getAllTraceKeys(traces) {
      let keys = [];
      for (let dict_key in traces) {
        for (let key in traces[dict_key]) {
          keys.push(key);
        }
      }
      return keys;
    },
    setAvailableTraces() {
      this.DYNAMIC_TRACES = this.TRACES['default'];
      proxyClient.call('GET', ProxyEndpoint.CHECK_WAYLAND, this, function(request, view) {
        try {
          if(request.responseText == 'true') {
            view.appendOptionalTraces('arc');
          }
        } catch(err) {
          console.error(err);
          proxyClient.setState(ProxyState.ERROR, request.responseText);
        }
      });
    },
    appendOptionalTraces(device_key) {
      for(let key in this.TRACES[device_key]) {
        this.$set(this.DYNAMIC_TRACES, key, this.TRACES[device_key][key]);
      }
    },
    keepAliveTrace() {
      if (this.status !== ProxyState.END_TRACE) {
        clearInterval(this.keep_alive_worker);
        this.keep_alive_worker = null;
        return;
      }
      proxyClient.call('GET', `${ProxyEndpoint.STATUS}${proxyClient.deviceId()}/`, this, function(request, view) {
        if (request.responseText !== 'True') {
          view.endTrace();
        } else if (view.keep_alive_worker === null) {
          view.keep_alive_worker = setInterval(view.keepAliveTrace, 1000);
        }
      });
    },
    startTrace() {
      const requested = this.toTrace();
      const requestedConfig = this.toTraceConfig();
      const requestedSelectedSfConfig = this.toSelectedSfTraceConfig();
      const requestedSelectedWmConfig = this.toSelectedWmTraceConfig();
      if (requested.length < 1) {
        proxyClient.setState(ProxyState.ERROR, 'No targets selected');
        this.recordNewEvent("No targets selected");
        return;
      }

      this.recordNewEvent("Start Trace");
      proxyClient.call('POST', `${ProxyEndpoint.CONFIG_TRACE}${proxyClient.deviceId()}/`, this, null, null, requestedConfig);
      proxyClient.call('POST', `${ProxyEndpoint.SELECTED_SF_CONFIG_TRACE}${proxyClient.deviceId()}/`, this, null, null, requestedSelectedSfConfig);
      proxyClient.call('POST',  `${ProxyEndpoint.SELECTED_WM_CONFIG_TRACE}${proxyClient.deviceId()}/`, this, null, null, requestedSelectedWmConfig);
      proxyClient.setState(ProxyState.END_TRACE);
      proxyClient.call('POST', `${ProxyEndpoint.START_TRACE}${proxyClient.deviceId()}/`, this, function(request, view) {
        view.keepAliveTrace();
      }, null, requested);
    },
    dumpState() {
      this.recordButtonClickedEvent("Dump State");
      const requested = this.toDump();
      if (requested.length < 1) {
        proxyClient.setState(ProxyState.ERROR, 'No targets selected');
        this.recordNewEvent("No targets selected");
        return;
      }
      proxyClient.setState(ProxyState.LOAD_DATA);
      proxyClient.call('POST', `${ProxyEndpoint.DUMP}${proxyClient.deviceId()}/`, this, function(request, view) {
        proxyClient.loadFile(requested, 0, "dump", view);
      }, null, requested);
    },
    endTrace() {
      proxyClient.setState(ProxyState.LOAD_DATA);
      proxyClient.call('POST', `${ProxyEndpoint.END_TRACE}${proxyClient.deviceId()}/`, this, function(request, view) {
        proxyClient.loadFile(view.toTrace(), 0, "trace", view);
      });
      this.recordNewEvent("Ended Trace");
    },
    toTrace() {
      return Object.keys(this.DYNAMIC_TRACES)
          .filter((traceKey) => this.traceStore[traceKey]);
    },
    toTraceConfig() {
      return Object.keys(TRACE_CONFIG)
          .filter((file) => this.traceStore[file])
          .flatMap((file) => TRACE_CONFIG[file])
          .filter((config) => this.traceStore[config]);
    },
    toSelectedSfTraceConfig() {
      const requestedSelectedConfig = {};
      for (const config in this.SF_SELECTED_CONFIG_VALUES) {
        if (this.SF_SELECTED_CONFIG_VALUES[config] !== "") {
          requestedSelectedConfig[config] = this.SF_SELECTED_CONFIG_VALUES[config];
        }
      }
      return requestedSelectedConfig;
    },
    toSelectedWmTraceConfig() {
      const requestedSelectedConfig = {};
      for (const config in this.WM_SELECTED_CONFIG_VALUES) {
        if (this.WM_SELECTED_CONFIG_VALUES[config] !== "") {
          requestedSelectedConfig[config] = this.WM_SELECTED_CONFIG_VALUES[config];
        }
      }
      return requestedSelectedConfig;
    },
    toDump() {
      return Object.keys(DUMPS)
          .filter((dumpKey) => this.traceStore[dumpKey]);
    },
    restart() {
      this.recordButtonClickedEvent("Connect / Retry");
      proxyClient.setState(ProxyState.CONNECTING);
    },
    resetLastDevice() {
      this.recordButtonClickedEvent("Change Device");
      this.proxyClient.resetLastDevice();
      this.restart();
    },
  },
  created() {
    proxyClient.setState(ProxyState.CONNECTING);
    this.proxyClient.onStateChange(this.onStateChangeFn);
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('token')) {
      this.proxyClient.proxyKey = urlParams.get('token');
    }
    this.proxyClient.getDevices();
  },
  beforeDestroy() {
    this.proxyClient.removeOnStateChange(this.onStateChangeFn);
  },
  watch: {
    status: {
      handler(st) {
        if (st == ProxyState.CONNECTING) {
          this.proxyClient.getDevices();
        }
        if (st == ProxyState.START_TRACE) {
          this.setAvailableTraces();
        }
      },
    },
  },
};

</script>
<style scoped>
.config-selection {
  width: 150px;
  display: inline-flex;
  margin-left: 5px;
  margin-right: 5px;
}
.device-choice {
  display: inline-flex;
}
h3 {
  margin-bottom: 0;
}
.trace-btn, .dump-btn {
  margin-top: 0;
}
pre {
  white-space: pre-wrap;
}
</style>
