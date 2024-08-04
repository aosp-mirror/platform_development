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
import {
  DeviceProperties,
  proxyClient,
  ProxyEndpoint,
  proxyRequest,
  ProxyState,
} from 'trace_collection/proxy_client';
import {Connection} from './connection';
import {
  ConfigMap,
  TraceConfigurationMap,
  TRACES,
} from './trace_collection_utils';

export class ProxyConnection implements Connection {
  proxy = proxyClient;
  keep_alive_worker: number | undefined;
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

  devices() {
    return this.proxy.devices;
  }

  adbData() {
    return this.proxy.adbData;
  }

  state() {
    return this.proxy.state;
  }

  isDevicesState() {
    return this.state() === ProxyState.DEVICES;
  }

  isStartTraceState() {
    return this.state() === ProxyState.START_TRACE;
  }

  isErrorState() {
    return this.state() === ProxyState.ERROR;
  }

  isEndTraceState() {
    return this.state() === ProxyState.END_TRACE;
  }

  isLoadDataState() {
    return this.state() === ProxyState.LOAD_DATA;
  }

  isConnectingState() {
    return this.state() === ProxyState.CONNECTING;
  }

  throwNoTargetsError() {
    this.proxy.setState(ProxyState.ERROR, 'No targets selected');
  }

  setProxyKey(key: string) {
    this.proxy.proxyKey = key;
    this.proxy.store.add('adb.proxyKey', key);
  }

  adbSuccess() {
    return !this.notConnected.includes(this.proxy.state);
  }

  selectedDevice(): DeviceProperties {
    return this.proxy.devices[this.proxy.selectedDevice];
  }

  selectedDeviceId(): string {
    return this.proxy.selectedDevice;
  }

  restart() {
    this.proxy.setState(ProxyState.CONNECTING);
  }

  resetLastDevice() {
    this.proxy.store.add('adb.lastDevice', '');
    this.restart();
  }

  selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  keepAliveTrace(view: ProxyConnection) {
    if (!view.isEndTraceState()) {
      window.clearInterval(view.keep_alive_worker);
      view.keep_alive_worker = undefined;
      return;
    }
    proxyRequest.keepTraceAlive(view);
  }

  async startTrace(
    requestedTraces: string[],
    reqEnableConfig?: string[],
    reqSelectedSfConfig?: ConfigMap,
    reqSelectedWmConfig?: ConfigMap,
  ) {
    if (reqEnableConfig) {
      proxyRequest.setEnabledConfig(this, reqEnableConfig);
    }
    if (reqSelectedSfConfig) {
      proxyRequest.setSelectedConfig(
        ProxyEndpoint.SELECTED_SF_CONFIG_TRACE,
        this,
        reqSelectedSfConfig,
      );
    }
    if (reqSelectedWmConfig) {
      proxyRequest.setSelectedConfig(
        ProxyEndpoint.SELECTED_WM_CONFIG_TRACE,
        this,
        reqSelectedWmConfig,
      );
    }
    await proxyClient.setState(ProxyState.END_TRACE);
    proxyRequest.startTrace(this, requestedTraces);
  }

  async endTrace() {
    this.progressCallback(0);
    await this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.endTrace(this, this.progressCallback);
  }

  async dumpState(requestedDumps: string[]): Promise<boolean> {
    this.progressCallback(0);
    if (requestedDumps.length < 1) {
      console.error('No targets selected');
      await this.proxy.setState(ProxyState.ERROR, 'No targets selected');
      return false;
    }
    await this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.dumpState(this, requestedDumps, this.progressCallback);
    return true;
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

  async onConnectChange(newState: ProxyState) {
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
