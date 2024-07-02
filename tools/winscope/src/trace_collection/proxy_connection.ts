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

import {assertDefined} from 'common/assert_utils';
import {FunctionUtils, OnProgressUpdateType} from 'common/function_utils';
import {TimeUtils} from 'common/time_utils';
import {
  DeviceProperties,
  proxyClient,
  ProxyEndpoint,
  proxyRequest,
  ProxyState,
} from 'trace_collection/proxy_client';
import {AdbConnection} from './adb_connection';
import {
  ConfigMap,
  TraceConfigurationMap,
  TRACES,
} from './trace_collection_utils';

export class ProxyConnection implements AdbConnection {
  proxy = proxyClient;
  keep_alive_worker: NodeJS.Timeout | undefined;
  notConnected = [
    ProxyState.NO_PROXY,
    ProxyState.UNAUTH,
    ProxyState.INVALID_VERSION,
  ];

  constructor(
    private proxyStateChangeCallback: (state: ProxyState) => void,
    private progressCallback: OnProgressUpdateType = FunctionUtils.DO_NOTHING,
    private traceConfigChangeCallback: (
      availableTracesConfig: TraceConfigurationMap,
    ) => void,
  ) {
    this.proxy.setState(ProxyState.CONNECTING);
    this.proxy.onProxyChange(
      async (newState) => await this.onConnectChange(newState),
    );
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('token')) {
      this.proxy.proxyKey = assertDefined(urlParams.get('token'));
    } else if (this.proxy.store.get('adb.proxyKey')) {
      this.proxy.proxyKey = assertDefined(this.proxy.store.get('adb.proxyKey'));
    }
    this.proxy.getDevices();
  }

  getDevices() {
    return this.proxy.devices;
  }

  getAdbData() {
    return this.proxy.adbData;
  }

  isDevicesState() {
    return this.proxy.getState() === ProxyState.DEVICES;
  }

  isStartTraceState() {
    return this.proxy.getState() === ProxyState.START_TRACE;
  }

  isErrorState() {
    return this.proxy.getState() === ProxyState.ERROR;
  }

  isStartingTraceState() {
    return this.proxy.getState() === ProxyState.STARTING_TRACE;
  }

  isEndTraceState() {
    return this.proxy.getState() === ProxyState.END_TRACE;
  }

  isLoadDataState() {
    return this.proxy.getState() === ProxyState.LOAD_DATA;
  }

  isConnectingState() {
    return this.proxy.getState() === ProxyState.CONNECTING;
  }

  async setErrorState(message: string) {
    this.proxy.setState(ProxyState.ERROR, message);
  }

  async setLoadDataState() {
    this.proxy.setState(ProxyState.LOAD_DATA);
  }

  setSecurityKey(key: string) {
    this.proxy.proxyKey = key;
    this.proxy.store.add('adb.proxyKey', key);
  }

  adbSuccess() {
    return !this.notConnected.includes(this.proxy.getState());
  }

  getSelectedDevice(): [string, DeviceProperties] {
    return [
      this.proxy.selectedDevice,
      this.proxy.devices[this.proxy.selectedDevice],
    ];
  }

  async restart() {
    this.proxy.setState(ProxyState.CONNECTING);
  }

  async clearLastDevice() {
    this.proxy.store.add('adb.lastDevice', '');
    this.restart();
  }

  async selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  keepAliveTrace(view: ProxyConnection) {
    if (!view.isStartingTraceState() && !view.isEndTraceState()) {
      clearInterval(view.keep_alive_worker);
      view.keep_alive_worker = undefined;
      return;
    }
    proxyRequest.keepTraceAlive(view.proxy, (request: XMLHttpRequest) => {
      if (request.responseText !== 'True') {
        view.endTrace();
      } else if (view.keep_alive_worker === undefined) {
        view.keep_alive_worker = setInterval(view.keepAliveTrace, 1000, view);
      }
    });
  }

  async startTrace(
    requestedTraces: string[],
    reqEnableConfig?: string[],
    reqSelectedSfConfig?: ConfigMap,
    reqSelectedWmConfig?: ConfigMap,
  ) {
    if (reqEnableConfig) {
      proxyRequest.setEnabledConfig(this.proxy, reqEnableConfig);
    }
    if (reqSelectedSfConfig) {
      proxyRequest.setSelectedConfig(
        ProxyEndpoint.SELECTED_SF_CONFIG_TRACE,
        this.proxy,
        reqSelectedSfConfig,
      );
    }
    if (reqSelectedWmConfig) {
      proxyRequest.setSelectedConfig(
        ProxyEndpoint.SELECTED_WM_CONFIG_TRACE,
        this.proxy,
        reqSelectedWmConfig,
      );
    }
    await proxyClient.setState(ProxyState.STARTING_TRACE);
    await proxyRequest.startTrace(
      this.proxy,
      requestedTraces,
      (request: XMLHttpRequest) => this.keepAliveTrace(this),
    );
    // TODO(b/330118129): identify source of additional start latency that affects some traces
    await TimeUtils.sleepMs(1000); // 1s timeout ensures SR fully started
    if (proxyClient.getState() === ProxyState.STARTING_TRACE) {
      proxyClient.setState(ProxyState.END_TRACE);
    }
  }

  async endTrace() {
    this.progressCallback(0);
    await this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.endTrace(this.proxy, this.progressCallback);
  }

  async dumpState(requestedDumps: string[]): Promise<boolean> {
    this.progressCallback(0);
    if (requestedDumps.length < 1) {
      console.error('No targets selected');
      await this.proxy.setState(ProxyState.ERROR, 'No targets selected');
      return false;
    }
    await this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.dumpState(
      this.proxy,
      requestedDumps,
      this.progressCallback,
    );
    return true;
  }

  async fetchExistingTraces() {
    await this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.fetchExistingFiles(this.proxy.selectedDevice);
  }

  isWaylandAvailable(): Promise<boolean> {
    return new Promise((resolve, reject) => {
      proxyRequest.call(
        'GET',
        ProxyEndpoint.CHECK_WAYLAND,
        (request: XMLHttpRequest) => {
          resolve(request.responseText === 'true');
        },
      );
    });
  }

  getErrorText(): string {
    return this.proxy.errorText;
  }

  onDestroy() {
    this.proxy.clearStateChangeListeners();
  }

  private async onConnectChange(newState: ProxyState) {
    if (newState === ProxyState.CONNECTING) {
      proxyClient.getDevices();
    }
    if (newState === ProxyState.START_TRACE) {
      const isWaylandAvailable = await this.isWaylandAvailable();
      if (isWaylandAvailable) {
        const availableTracesConfig = TRACES['default'];
        if (isWaylandAvailable) {
          Object.assign(availableTracesConfig, TRACES['arc']);
        }
        this.traceConfigChangeCallback(availableTracesConfig);
      }
    }
    this.proxyStateChangeCallback(newState);
  }
}
