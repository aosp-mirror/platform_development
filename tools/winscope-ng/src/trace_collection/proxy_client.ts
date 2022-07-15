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
import { PersistentStore } from "common/persistent_store";

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
}

export enum ProxyEndpoint {
  DEVICES = "/devices/",
  START_TRACE = "/start/",
  END_TRACE = "/end/",
  CONFIG_TRACE = "/configtrace/",
  SELECTED_WM_CONFIG_TRACE = "/selectedwmconfigtrace/",
  SELECTED_SF_CONFIG_TRACE = "/selectedsfconfigtrace/",
  DUMP = "/dump/",
  FETCH = "/fetch/",
  STATUS = "/status/",
  CHECK_WAYLAND = "/checkwayland/",
}

export class ProxyClient {
  readonly WINSCOPE_PROXY_URL = "http://localhost:5544";
  readonly VERSION = "0.8";

  state: ProxyState = ProxyState.CONNECTING;
  stateChangeListeners: {(param:ProxyState, errorText:string): void;}[] = [];
  refresh_worker: NodeJS.Timeout | undefined = undefined;
  devices: any = {};
  selectedDevice = "";
  errorText = "";

  proxyKey = "";
  lastDevice = "";

  store = new PersistentStore();

  call(method: string, path: string, view: any, onSuccess: any, type = null, jsonRequest = null) {
    const request = new XMLHttpRequest();
    const client: ProxyClient = this;
    request.onreadystatechange = function() {
      if (this.readyState !== 4) {
        return;
      }
      if (this.status === 0) {
        client.setState(ProxyState.NO_PROXY);
      } else if (this.status === 200) {
        if (this.getResponseHeader("Winscope-Proxy-Version") !== client.VERSION) {
          client.setState(ProxyState.INVALID_VERSION);
        } else if (onSuccess) {
          onSuccess(this, view);
        }
      } else if (this.status === 403) {
        client.setState(ProxyState.UNAUTH);
      } else {
        if (this.responseType === "text" || !this.responseType) {
          client.errorText = this.responseText;
        } else if (this.responseType === "arraybuffer") {
          client.errorText = String.fromCharCode.apply(null, new Array(this.response));
        }
        client.setState(ProxyState.ERROR);
      }
    };
    request.responseType = type || "";
    request.open(method, client.WINSCOPE_PROXY_URL + path);
    const lastKey = client.store.getFromStore("adb.proxyKey");
    if (lastKey !== null) {
      client.proxyKey = lastKey;
    }

    request.setRequestHeader("Winscope-Token", client.proxyKey);
    if (jsonRequest) {
      const json = JSON.stringify(jsonRequest);
      request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
      request.send(json);
    } else {
      request.send();
    }
  }

  setState(state:ProxyState, errorText = "") {
    this.state = state;
    this.errorText = errorText;
    for (const listener of this.stateChangeListeners) {
      listener(state, errorText);
    }
  }

  onProxyChange(fn: (state:ProxyState, errorText:string) => void) {
    this.removeOnProxyChange(fn);
    this.stateChangeListeners.push(fn);
  }

  removeOnProxyChange(removeFn: (state:ProxyState, errorText:string) => void) {
    this.stateChangeListeners = this.stateChangeListeners.filter(fn => fn !== removeFn);
  }

  getDevices() {
    if (this.state !== ProxyState.DEVICES && this.state !== ProxyState.CONNECTING) {
      clearInterval(this.refresh_worker);
      this.refresh_worker = undefined;
      return;
    }
    const client = this;
    this.call("GET", ProxyEndpoint.DEVICES, this, function(request: any, view: any) {
      try {
        client.devices = JSON.parse(request.responseText);
        const last = client.store.getFromStore("adb.lastDevice");
        if (last && client.devices[last] &&
                client.devices[last].authorised) {
          client.selectDevice(last);
        } else {
          if (client.refresh_worker === undefined) {
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

  selectDevice(device_id: string) {
    this.selectedDevice = device_id;
    this.store.addToStore("adb.lastDevice", device_id);
    this.setState(ProxyState.START_TRACE);
  }

  deviceId() {
    return this.selectedDevice;
  }

  resetLastDevice() {
    this.lastDevice = "";
    this.store.addToStore("adb.lastDevice", "");
  }
}

export const proxyClient = new ProxyClient();
