/*
 * Copyright 2022, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import LocalStore from '../localstore.js';
import {FILE_DECODERS, FILE_TYPES} from '../decode.js';

export enum ProxyState {
  ERROR = 0,
  CONNECTING = 1,
  NO_PROXY = 2,
  INVALID_VERSION = 3,
  UNAUTH = 4,
  DEVICES = 5,
  START_TRACE = 6,
  END_TRACE = 7,
  LOAD_DATA = 8,
};

export enum ProxyEndpoint {
  DEVICES = '/devices/',
  START_TRACE = '/start/',
  END_TRACE = '/end/',
  CONFIG_TRACE = '/configtrace/',
  SELECTED_WM_CONFIG_TRACE = '/selectedwmconfigtrace/',
  SELECTED_SF_CONFIG_TRACE = '/selectedsfconfigtrace/',
  DUMP = '/dump/',
  FETCH = '/fetch/',
  STATUS = '/status/',
  CHECK_WAYLAND = '/checkwayland/',
};

const proxyFileTypeAdapter = {
  'window_trace': FILE_TYPES.WINDOW_MANAGER_TRACE,
  'accessibility_trace': FILE_TYPES.ACCESSIBILITY_TRACE,
  'layers_trace': FILE_TYPES.SURFACE_FLINGER_TRACE,
  'wl_trace': FILE_TYPES.WAYLAND_TRACE,
  'layers_dump': FILE_TYPES.SURFACE_FLINGER_DUMP,
  'window_dump': FILE_TYPES.WINDOW_MANAGER_DUMP,
  'wl_dump': FILE_TYPES.WAYLAND_DUMP,
  'screen_recording': FILE_TYPES.SCREEN_RECORDING,
  'transactions': FILE_TYPES.TRANSACTIONS_TRACE,
  'transactions_legacy': FILE_TYPES.TRANSACTIONS_TRACE_LEGACY,
  'proto_log': FILE_TYPES.PROTO_LOG,
  'system_ui_trace': FILE_TYPES.SYSTEM_UI,
  'launcher_trace': FILE_TYPES.LAUNCHER,
  'ime_trace_clients': FILE_TYPES.IME_TRACE_CLIENTS,
  'ime_trace_service': FILE_TYPES.IME_TRACE_SERVICE,
  'ime_trace_managerservice': FILE_TYPES.IME_TRACE_MANAGERSERVICE,
};

class ProxyClient {
  readonly WINSCOPE_PROXY_URL = 'http://localhost:5544';
  readonly VERSION = '0.8';

  store:LocalStore = LocalStore('adb', {
    proxyKey: '',
    lastDevice: ''});

  state:ProxyState = ProxyState.CONNECTING;
  stateChangeListeners:{(param:ProxyState, errorText:String): void;}[] = [];
  refresh_worker: NodeJS.Timer = null;
  devices = {};
  selectedDevice = ""
  errorText:String = ""

  call(method, path, view, onSuccess, type = null, jsonRequest = null) {
    const request = new XMLHttpRequest();
    let client = this;
    request.onreadystatechange = function() {
      if (this.readyState !== 4) {
        return;
      }
      if (this.status === 0) {
        client.setState(ProxyState.NO_PROXY);
      } else if (this.status === 200) {
        if (this.getResponseHeader('Winscope-Proxy-Version') !== client.VERSION) {
          client.setState(ProxyState.INVALID_VERSION);
        } else if (onSuccess) {
          onSuccess(this, view);
        }
      } else if (this.status === 403) {
        client.setState(ProxyState.UNAUTH);
      } else {
        if (this.responseType === 'text' || !this.responseType) {
          client.errorText = this.responseText;
        } else if (this.responseType === 'arraybuffer') {
          client.errorText = String.fromCharCode.apply(null, new Uint8Array(this.response));
        }
        client.setState(ProxyState.ERROR);
      }
    };
    request.responseType = type || "";
    request.open(method, client.WINSCOPE_PROXY_URL + path);
    request.setRequestHeader('Winscope-Token', client.store.proxyKey);
    if (jsonRequest) {
      const json = JSON.stringify(jsonRequest);
      request.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
      request.send(json);
    } else {
      request.send();
    }
  }

  setState(state:ProxyState, errorText:String = "") {
    this.state = state;
    this.errorText = errorText;
    for (let listener of this.stateChangeListeners) {
      listener(state, errorText);
    }
  }

  onStateChange(fn: (state:ProxyState, errorText:String) => void) {
    this.removeOnStateChange(fn);
    this.stateChangeListeners.push(fn);
  }

  removeOnStateChange(removeFn: (state:ProxyState, errorText:String) => void) {
    this.stateChangeListeners = this.stateChangeListeners.filter(fn => fn !== removeFn);
  }

  getDevices() {
    if (this.state !== ProxyState.DEVICES && this.state !== ProxyState.CONNECTING) {
      clearInterval(this.refresh_worker);
      this.refresh_worker = null;
      return;
    }
    let client = this;
    this.call('GET', ProxyEndpoint.DEVICES, this, function(request, view) {
      try {
        client.devices = JSON.parse(request.responseText);
        if (client.store.lastDevice && client.devices[client.store.lastDevice] &&
              client.devices[client.store.lastDevice].authorised) {
          client.selectDevice(client.store.lastDevice);
        } else {
          if (client.refresh_worker === null) {
            client.refresh_worker = setInterval(client.getDevices, 1000);
          }
          client.setState(ProxyState.DEVICES);
        }
      } catch (err) {
        console.error(err);
        client.errorText = request.responseText;
        client.setState(ProxyState.ERROR);
      }
    });
  }

  selectDevice(device_id) {
    this.selectedDevice = device_id;
    this.store.lastDevice = device_id;
    this.setState(ProxyState.START_TRACE);
  }

  deviceId() {
    return this.selectedDevice;
  }

  resetLastDevice() {
    this.store.lastDevice = '';
  }

  loadFile(files, idx, traceType, view) {
    let client = this;
    this.call('GET', `${ProxyEndpoint.FETCH}${proxyClient.deviceId()}/${files[idx]}/`, view,
        (request, view) => {
      try {
        const enc = new TextDecoder('utf-8');
        const resp = enc.decode(request.response);
        const filesByType = JSON.parse(resp);

        for (const filetype in filesByType) {
          if (filesByType.hasOwnProperty(filetype)) {
            const files = filesByType[filetype];
            const fileDecoder = FILE_DECODERS[proxyFileTypeAdapter[filetype]];

            for (const encodedFileBuffer of files) {
              const buffer = Uint8Array.from(atob(encodedFileBuffer), (c) => c.charCodeAt(0));
              const data = fileDecoder.decoder(buffer, fileDecoder.decoderParams,
                  fileDecoder.name, view.store);
              view.dataFiles.push(data);
              view.loadProgress = 100 * (idx + 1) / files.length; // TODO: Update this
            }
          }
        }

        if (idx < files.length - 1) {
          client.loadFile(files, idx + 1, traceType, view);
        } else {
          const currentDate = new Date().toISOString();
          view.$emit('dataReady',
              `winscope-${traceType}-${currentDate}`,
              view.dataFiles);
        }
      } catch (err) {
        console.error(err);
        client.setState(ProxyState.ERROR, err);
      }
    }, 'arraybuffer');
  }

}

export const proxyClient = new ProxyClient();
