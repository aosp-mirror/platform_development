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
import {
  proxyRequest,
  proxyClient,
  ProxyState,
  ProxyEndpoint
} from "trace_collection/proxy_client";
import { setTraces } from "./set_traces";
import { Connection, DeviceProperties } from "./connection";
import { configMap } from "./trace_collection_utils";

export class ProxyConnection implements Connection {
  proxy = proxyClient;
  keep_alive_worker: any = null;
  notConnected = [
    ProxyState.NO_PROXY,
    ProxyState.UNAUTH,
    ProxyState.INVALID_VERSION,
  ];
  loadProgress = 0;

  constructor() {
    this.proxy.setState(ProxyState.CONNECTING);
    this.proxy.onProxyChange(this.onConnectChange);
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("token")) {
      this.proxy.proxyKey = urlParams.get("token")!;
    }
    this.proxy.getDevices();
  }

  public devices() {
    return this.proxy.devices;
  }

  public adbData() {
    return this.proxy.adbData;
  }

  public state() {
    return this.proxy.state;
  }

  public isDevicesState() {
    return this.state() === ProxyState.DEVICES;
  }

  public isStartTraceState() {
    return this.state() === ProxyState.START_TRACE;
  }

  public isErrorState() {
    return this.state() === ProxyState.ERROR;
  }

  public isEndTraceState() {
    return this.state() === ProxyState.END_TRACE;
  }

  public isLoadDataState() {
    return this.state() === ProxyState.LOAD_DATA;
  }

  public isConnectingState() {
    return this.state() === ProxyState.CONNECTING;
  }

  public throwNoTargetsError() {
    this.proxy.setState(ProxyState.ERROR, "No targets selected");
  }

  public setProxyKey(key: string) {
    this.proxy.proxyKey = key;
    this.restart();
  }

  public adbSuccess() {
    return !this.notConnected.includes(this.proxy.state);
  }

  public selectedDevice(): DeviceProperties {
    return this.proxy.devices[this.proxy.selectedDevice];
  }

  public selectedDeviceId(): string {
    return this.proxy.selectedDevice;
  }

  public restart() {
    this.proxy.setState(ProxyState.CONNECTING);
  }

  public resetLastDevice() {
    this.proxy.store.addToStore("adb.lastDevice", "");
    this.restart();
  }

  public selectDevice(id: string) {
    this.proxy.selectDevice(id);
  }

  public keepAliveTrace(view:ProxyConnection) {
    if (!view.isEndTraceState()) {
      clearInterval(view.keep_alive_worker);
      view.keep_alive_worker = null;
      return;
    }
    proxyRequest.keepTraceAlive(view);
  }

  public startTrace(
    reqEnableConfig?: Array<string>,
    reqSelectedSfConfig?: configMap,
    reqSelectedWmConfig?: configMap
  ) {
    if (reqEnableConfig) {
      proxyRequest.setEnabledConfig(this, reqEnableConfig);
    }
    if (reqSelectedSfConfig) {
      proxyRequest.setSelectedConfig(ProxyEndpoint.SELECTED_SF_CONFIG_TRACE, this, reqSelectedSfConfig);
    }
    if (reqSelectedWmConfig) {
      proxyRequest.setSelectedConfig(ProxyEndpoint.SELECTED_WM_CONFIG_TRACE, this, reqSelectedWmConfig);
    }
    proxyClient.setState(ProxyState.END_TRACE);
    proxyRequest.startTrace(this);
  }

  public async endTrace() {
    this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.endTrace(this);
  }

  public async dumpState() {
    if (setTraces.reqDumps.length < 1) {
      this.proxy.setState(ProxyState.ERROR, "No targets selected");
      setTraces.dumpError = true;
      return;
    }
    this.proxy.setState(ProxyState.LOAD_DATA);
    await proxyRequest.dumpState(this);
  }

  public onConnectChange(newState: ProxyState) {
    if (newState === ProxyState.CONNECTING) {
      proxyClient.getDevices();
    }
    if (newState == ProxyState.START_TRACE) {
      setTraces.setAvailableTraces();
    }
  }
}
