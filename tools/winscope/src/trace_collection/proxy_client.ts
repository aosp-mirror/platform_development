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

import {OnProgressUpdateType} from 'common/function_utils';
import {PersistentStore} from 'common/persistent_store';
import {OnRequestSuccessCallback} from './on_request_success_callback';
import {ConfigMap} from './trace_collection_utils';

export interface Devices {
  [key: string]: DeviceProperties;
}

export interface DeviceProperties {
  authorised: boolean;
  model: string;
}

export enum ProxyState {
  ERROR,
  CONNECTING,
  NO_PROXY,
  INVALID_VERSION,
  UNAUTH,
  DEVICES,
  CONFIGURE_TRACE,
  STARTING_TRACE,
  TRACING,
  LOADING_DATA,
}

export enum ProxyEndpoint {
  DEVICES = '/devices/',
  START_TRACE = '/start/',
  END_TRACE = '/end/',
  ENABLE_CONFIG_TRACE = '/configtrace/',
  SELECTED_WM_CONFIG_TRACE = '/selectedwmconfigtrace/',
  SELECTED_SF_CONFIG_TRACE = '/selectedsfconfigtrace/',
  DUMP = '/dump/',
  FETCH = '/fetch/',
  STATUS = '/status/',
  CHECK_WAYLAND = '/checkwayland/',
}

// from here, all requests to the proxy are made
class ProxyRequest {
  // List of trace we are actively tracing
  private tracingTraces: string[] | undefined;

  async call(
    method: string,
    path: string,
    onSuccess: OnRequestSuccessCallback | undefined,
    type?: XMLHttpRequest['responseType'],
    jsonRequest?: object,
  ): Promise<void> {
    return new Promise((resolve) => {
      const request = new XMLHttpRequest();
      const client = proxyClient;
      request.onreadystatechange = async function () {
        if (this.readyState !== XMLHttpRequest.DONE) {
          return;
        }
        if (this.status === XMLHttpRequest.UNSENT) {
          client.setState(ProxyState.NO_PROXY);
          resolve();
        } else if (this.status === 200) {
          if (
            !client.areVersionsCompatible(
              this.getResponseHeader('Winscope-Proxy-Version'),
            )
          ) {
            client.setState(ProxyState.INVALID_VERSION);
            resolve();
          } else if (onSuccess) {
            try {
              await onSuccess(this);
            } catch (err) {
              console.error(err);
              proxyClient.setState(
                ProxyState.ERROR,
                `Error handling request response:\n${err}\n\n` +
                  `Request:\n ${request.responseText}`,
              );
              resolve();
            }
          }
          resolve();
        } else if (this.status === 403) {
          client.setState(ProxyState.UNAUTH);
          resolve();
        } else {
          if (this.responseType === 'text' || !this.responseType) {
            client.errorText = this.responseText;
          } else if (this.responseType === 'arraybuffer') {
            client.errorText = String.fromCharCode.apply(
              null,
              new Array(this.response),
            );
          }
          client.setState(ProxyState.ERROR, client.errorText);
          resolve();
        }
      };
      request.responseType = type || '';
      request.open(method, client.WINSCOPE_PROXY_URL + path);
      const lastKey = client.store.get('adb.proxyKey');
      if (lastKey !== undefined) {
        client.proxyKey = lastKey;
      }
      request.setRequestHeader('Winscope-Token', client.proxyKey);
      if (jsonRequest) {
        const json = JSON.stringify(jsonRequest);
        request.setRequestHeader(
          'Content-Type',
          'application/json;charset=UTF-8',
        );
        request.send(json);
      } else {
        request.send();
      }
    });
  }

  async getDevices() {
    await proxyRequest.call(
      'GET',
      ProxyEndpoint.DEVICES,
      proxyRequest.onSuccessGetDevices,
    );
  }

  async setEnabledConfig(view: ProxyClient, req: string[]) {
    await proxyRequest.call(
      'POST',
      `${ProxyEndpoint.ENABLE_CONFIG_TRACE}${view.selectedDevice}/`,
      undefined,
      undefined,
      req,
    );
  }

  async setSelectedConfig(
    endpoint: ProxyEndpoint,
    view: ProxyClient,
    req: ConfigMap,
  ) {
    await proxyRequest.call(
      'POST',
      `${endpoint}${view.selectedDevice}/`,
      undefined,
      undefined,
      req,
    );
  }

  async startTrace(
    view: ProxyClient,
    requestedTraces: string[],
    onSuccessStartTrace: OnRequestSuccessCallback,
  ) {
    this.tracingTraces = requestedTraces;
    await proxyRequest.call(
      'POST',
      `${ProxyEndpoint.START_TRACE}${view.selectedDevice}/`,
      onSuccessStartTrace,
      undefined,
      requestedTraces,
    );
  }

  async endTrace(
    view: ProxyClient,
    progressCallback: OnProgressUpdateType,
  ): Promise<void> {
    const requestedTraces = this.tracingTraces;
    this.tracingTraces = undefined;
    if (requestedTraces === undefined) {
      throw new Error('Trace not started before stopping');
    }
    await proxyRequest.call(
      'POST',
      `${ProxyEndpoint.END_TRACE}${view.selectedDevice}/`,
      async (request: XMLHttpRequest) => {
        await proxyClient.updateAdbData(
          requestedTraces,
          'trace',
          progressCallback,
        );
      },
    );
  }

  async keepTraceAlive(
    view: ProxyClient,
    onSuccessKeepTraceAlive: OnRequestSuccessCallback,
  ) {
    await proxyRequest.call(
      'GET',
      `${ProxyEndpoint.STATUS}${view.selectedDevice}/`,
      onSuccessKeepTraceAlive,
    );
  }

  async dumpState(
    view: ProxyClient,
    requestedDumps: string[],
    progressCallback: OnProgressUpdateType,
  ) {
    await proxyRequest.call(
      'POST',
      `${ProxyEndpoint.DUMP}${view.selectedDevice}/`,
      async (request: XMLHttpRequest) => {
        await proxyClient.updateAdbData(
          requestedDumps,
          'dump',
          progressCallback,
        );
      },
      undefined,
      requestedDumps,
    );
  }

  async fetchExistingFiles(deviceId: string) {
    await proxyRequest.call(
      'GET',
      `${ProxyEndpoint.FETCH}${deviceId}/`,
      this.onSuccessFetchFiles,
      'arraybuffer',
    );
  }

  async fetchFiles(deviceId: string, adbParams: AdbParams): Promise<void> {
    const files = adbParams.files;
    const idx = adbParams.idx;

    await proxyRequest.call(
      'GET',
      `${ProxyEndpoint.FETCH}${deviceId}/${files[idx]}/`,
      this.onSuccessFetchFiles,
      'arraybuffer',
    );
  }

  private onSuccessFetchFiles: OnRequestSuccessCallback = async (
    request: XMLHttpRequest,
  ) => {
    try {
      const enc = new TextDecoder('utf-8');
      const resp = enc.decode(request.response);
      const filesByType = JSON.parse(resp);

      for (const filetype of Object.keys(filesByType)) {
        const files = filesByType[filetype];
        for (const encodedFileBuffer of files) {
          const buffer = Uint8Array.from(atob(encodedFileBuffer), (c) =>
            c.charCodeAt(0),
          );
          const blob = new Blob([buffer]);
          const newFile = new File([blob], filetype);
          proxyClient.adbData.push(newFile);
        }
      }
    } catch (error) {
      proxyClient.setState(ProxyState.ERROR, request.responseText);
      throw error;
    }
  };

  private onSuccessGetDevices: OnRequestSuccessCallback = async (
    request: XMLHttpRequest,
  ) => {
    const client = proxyClient;
    try {
      client.devices = JSON.parse(request.responseText);
      const last = client.store.get('adb.lastDevice');
      if (last && client.devices[last] && client.devices[last].authorised) {
        await client.selectDevice(last);
      } else {
        client.setState(ProxyState.DEVICES);
      }
      if (client.refresh_worker === undefined) {
        client.refresh_worker = setInterval(client.getDevices, 1000);
      }
    } catch (err) {
      console.error(err);
      client.errorText = request.responseText;
      client.setState(ProxyState.ERROR, client.errorText);
    }
  };
}
export const proxyRequest = new ProxyRequest();

interface AdbParams {
  files: string[];
  idx: number;
  traceType: string;
}

// stores all the changing variables from proxy and sets up calls from ProxyRequest
export class ProxyClient {
  readonly WINSCOPE_PROXY_URL = 'http://localhost:5544';
  readonly VERSION = '2.2.0';
  stateChangeListeners: Array<{
    (param: ProxyState, errorText: string): Promise<void>;
  }> = [];
  refresh_worker: NodeJS.Timeout | undefined;
  devices: Devices = {};
  selectedDevice = '';
  errorText = '';
  adbData: File[] = [];
  proxyKey = '';
  lastDevice = '';
  store = new PersistentStore();
  private state: ProxyState = ProxyState.CONNECTING;

  async setState(state: ProxyState, errorText = '') {
    this.state = state;
    this.errorText = errorText;
    for (const listener of this.stateChangeListeners) {
      await listener(state, errorText);
    }
  }

  getState() {
    return this.state;
  }

  onProxyChange(fn: (state: ProxyState, errorText: string) => Promise<void>) {
    this.removeOnProxyChange(fn);
    this.stateChangeListeners.push(fn);
  }

  removeOnProxyChange(
    removeFn: (state: ProxyState, errorText: string) => Promise<void>,
  ) {
    this.stateChangeListeners = this.stateChangeListeners.filter(
      (fn) => fn !== removeFn,
    );
  }

  clearStateChangeListeners() {
    this.stateChangeListeners = [];
  }

  async getDevices() {
    if (
      proxyClient.state !== ProxyState.DEVICES &&
      proxyClient.state !== ProxyState.CONNECTING &&
      proxyClient.state !== ProxyState.CONFIGURE_TRACE
    ) {
      if (proxyClient.refresh_worker !== undefined) {
        clearInterval(proxyClient.refresh_worker);
        proxyClient.refresh_worker = undefined;
      }
      return;
    }
    proxyRequest.getDevices();
  }

  async selectDevice(device_id: string) {
    this.selectedDevice = device_id;
    this.store.add('adb.lastDevice', device_id);
    this.setState(ProxyState.CONFIGURE_TRACE);
  }

  async updateAdbData(
    files: string[],
    traceType: string,
    progressCallback: OnProgressUpdateType,
  ) {
    for (let idx = 0; idx < files.length; idx++) {
      const adbParams = {
        files,
        idx,
        traceType,
      };
      await proxyRequest.fetchFiles(this.selectedDevice, adbParams);
      progressCallback((100 * (idx + 1)) / files.length);
    }
  }

  areVersionsCompatible(proxyVersion: string | null): boolean {
    if (!proxyVersion) return false;
    const [proxyMajor, proxyMinor, proxyPatch] = proxyVersion
      .split('.')
      .map((s) => Number(s));
    const [clientMajor, clientMinor, clientPatch] = this.VERSION.split('.').map(
      (s) => Number(s),
    );

    if (proxyMajor !== clientMajor) {
      return false;
    }

    if (proxyMinor === clientMinor) {
      // Check patch number to ensure user has deployed latest bug fixes
      return proxyPatch >= clientPatch;
    }

    return proxyMinor > clientMinor;
  }
}

export const proxyClient = new ProxyClient();
