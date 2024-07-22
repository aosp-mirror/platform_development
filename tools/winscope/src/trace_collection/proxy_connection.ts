/*
 * Copyright 2024, The Android Open Source Project
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
  HttpRequest,
  HttpRequestHeaderType,
  HttpRequestStatus,
  HttpResponse,
} from 'common/http_request';
import {PersistentStore} from 'common/persistent_store';
import {TimeUtils} from 'common/time_utils';
import {Analytics} from 'logging/analytics';
import {AdbConnection, OnRequestSuccessCallback} from './adb_connection';
import {AdbDevice} from './adb_device';
import {ConnectionState} from './connection_state';
import {ProxyEndpoint} from './proxy_endpoint';
import {TraceConfigurationMap, TRACES} from './trace_collection_utils';
import {TraceRequest} from './trace_request';

export class ProxyConnection extends AdbConnection {
  static readonly VERSION = '2.3.0';
  static readonly WINSCOPE_PROXY_URL = 'http://localhost:5544';

  private readonly store = new PersistentStore();
  private readonly storeKeySecurityToken = 'adb.proxyKey';

  private state: ConnectionState = ConnectionState.CONNECTING;
  private errorText = '';
  private securityToken = '';
  private devices: AdbDevice[] = [];
  private selectedDevice: AdbDevice | undefined;
  private requestedTraces: TraceRequest[] = [];
  private adbData: File[] = [];
  private keep_trace_alive_worker: NodeJS.Timeout | undefined;
  private refresh_devices_worker: NodeJS.Timeout | undefined;
  private detectStateChangeInUi: () => Promise<void> =
    FunctionUtils.DO_NOTHING_ASYNC;
  private progressCallback: OnProgressUpdateType = FunctionUtils.DO_NOTHING;
  private availableTracesChangeCallback: (
    availableTracesConfig: TraceConfigurationMap,
  ) => void = FunctionUtils.DO_NOTHING;

  async initialize(
    detectStateChangeInUi: () => Promise<void>,
    progressCallback: OnProgressUpdateType,
    availableTracesChangeCallback: (
      availableTracesConfig: TraceConfigurationMap,
    ) => void,
  ): Promise<void> {
    this.detectStateChangeInUi = detectStateChangeInUi;
    this.progressCallback = progressCallback;
    this.availableTracesChangeCallback = availableTracesChangeCallback;

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('token')) {
      this.securityToken = assertDefined(urlParams.get('token'));
    } else {
      this.securityToken = this.store?.get(this.storeKeySecurityToken) ?? '';
    }
    await this.setState(ConnectionState.CONNECTING);
  }

  async restartConnection(): Promise<void> {
    await this.setState(ConnectionState.CONNECTING);
  }

  setSecurityToken(token: string) {
    this.securityToken = token;
    this.store?.add(this.storeKeySecurityToken, token);
  }

  getDevices(): AdbDevice[] {
    return this.devices;
  }

  getState(): ConnectionState {
    return this.state;
  }

  getErrorText(): string {
    return this.errorText;
  }

  onDestroy() {
    clearInterval(this.refresh_devices_worker);
    this.refresh_devices_worker = undefined;
    clearInterval(this.keep_trace_alive_worker);
    this.keep_trace_alive_worker = undefined;
  }

  async startTrace(
    device: AdbDevice,
    requestedTraces: TraceRequest[],
  ): Promise<void> {
    if (requestedTraces.length === 0) {
      throw new Error('No traces requested');
    }
    this.selectedDevice = device;
    this.requestedTraces = requestedTraces;
    await this.setState(ConnectionState.STARTING_TRACE);
  }

  async endTrace() {
    if (this.requestedTraces.length === 0) {
      throw new Error('Trace not started before stopping');
    }
    await this.setState(ConnectionState.ENDING_TRACE);
  }

  async dumpState(
    device: AdbDevice,
    requestedDumps: TraceRequest[],
  ): Promise<void> {
    if (requestedDumps.length === 0) {
      throw new Error('No dumps requested');
    }
    this.progressCallback(0);
    this.selectedDevice = device;
    this.requestedTraces = requestedDumps;
    await this.setState(ConnectionState.DUMPING_STATE);
  }

  async fetchLastTracingSessionData(device: AdbDevice): Promise<File[]> {
    this.adbData = [];
    this.selectedDevice = device;
    await this.setState(ConnectionState.LOADING_DATA);
    this.selectedDevice = undefined;
    return this.adbData;
  }

  private async updateAdbData(device: AdbDevice) {
    if (this.requestedTraces.length === 0) {
      await this.getFromProxy(
        `${ProxyEndpoint.FETCH}${device.id}/`,
        this.onSuccessFetchFiles,
        'arraybuffer',
      );
    } else {
      const fileNames = this.requestedTraces.map((t) => t.name);
      for (let idx = 0; idx < fileNames.length; idx++) {
        await this.getFromProxy(
          `${ProxyEndpoint.FETCH}${device.id}/${fileNames[idx]}/`,
          this.onSuccessFetchFiles,
          'arraybuffer',
        );
        this.progressCallback((100 * (idx + 1)) / fileNames.length);
      }
      this.requestedTraces = [];
    }
  }

  private async onConnectionStateChange() {
    await this.detectStateChangeInUi();

    switch (this.state) {
      case ConnectionState.ERROR:
        Analytics.Error.logProxyError(this.errorText);
        return;

      case ConnectionState.CONNECTING:
        await this.requestDevices();
        return;

      case ConnectionState.IDLE:
        if (this.selectedDevice) {
          const isWaylandAvailable = await this.isWaylandAvailable();
          if (isWaylandAvailable) {
            const availableTracesConfig = TRACES['default'];
            Object.assign(availableTracesConfig, TRACES['arc']);
            this.availableTracesChangeCallback(availableTracesConfig);
          }
        }
        return;

      case ConnectionState.STARTING_TRACE:
        await this.postToProxy(
          `${ProxyEndpoint.START_TRACE}${
            assertDefined(this.selectedDevice).id
          }/`,
          () => this.keepTraceAlive(),
          this.requestedTraces,
        );
        // TODO(b/330118129): identify source of additional start latency that affects some traces
        await TimeUtils.sleepMs(1000); // 1s timeout ensures SR fully started
        if (this.getState() === ConnectionState.STARTING_TRACE) {
          this.setState(ConnectionState.TRACING);
        }
        return;

      case ConnectionState.ENDING_TRACE:
        await this.postToProxy(
          `${ProxyEndpoint.END_TRACE}${assertDefined(this.selectedDevice).id}/`,
        );
        return;

      case ConnectionState.DUMPING_STATE:
        await this.postToProxy(
          `${ProxyEndpoint.DUMP}${assertDefined(this.selectedDevice).id}/`,
          FunctionUtils.DO_NOTHING,
          this.requestedTraces.map((t) => t.name),
        );
        return;

      case ConnectionState.LOADING_DATA:
        if (this.selectedDevice === undefined) {
          throw new Error('No device found');
        }
        await this.updateAdbData(assertDefined(this.selectedDevice));
        return;

      default:
      // do nothing
    }
  }

  private async keepTraceAlive() {
    const state = this.getState();
    if (
      state !== ConnectionState.STARTING_TRACE &&
      state !== ConnectionState.TRACING
    ) {
      clearInterval(this.keep_trace_alive_worker);
      this.keep_trace_alive_worker = undefined;
      return;
    }

    await this.getFromProxy(
      `${ProxyEndpoint.STATUS}${assertDefined(this.selectedDevice).id}/`,
      async (request: HttpResponse) => {
        if (request.text !== 'True') {
          this.endTrace();
        } else if (this.keep_trace_alive_worker === undefined) {
          this.keep_trace_alive_worker = setInterval(
            () => this.keepTraceAlive(),
            1000,
          );
        }
      },
    );
  }

  private async setState(state: ConnectionState, errorText = '') {
    this.state = state;
    this.errorText = errorText;
    await this.onConnectionStateChange();
  }

  private async requestDevices() {
    if (
      this.state !== ConnectionState.IDLE &&
      this.state !== ConnectionState.CONNECTING
    ) {
      if (this.refresh_devices_worker !== undefined) {
        clearInterval(this.refresh_devices_worker);
        this.refresh_devices_worker = undefined;
      }
      return;
    }

    await this.getFromProxy(ProxyEndpoint.DEVICES, this.onSuccessGetDevices);
  }

  private onSuccessGetDevices: OnRequestSuccessCallback = async (
    resp: HttpResponse,
  ) => {
    try {
      const devices = JSON.parse(resp.text);
      this.devices = Object.keys(devices).map((deviceId) => {
        return {
          id: deviceId,
          authorized: devices[deviceId].authorized,
          model: devices[deviceId].model,
        };
      });
      if (this.refresh_devices_worker === undefined) {
        this.refresh_devices_worker = setInterval(
          () => this.requestDevices(),
          1000,
        );
      }
      this.setState(ConnectionState.IDLE);
    } catch (err) {
      this.setState(
        ConnectionState.ERROR,
        `Could not find devices. Received:\n${resp.text}`,
      );
    }
  };

  private onSuccessFetchFiles: OnRequestSuccessCallback = async (
    httpResponse: HttpResponse,
  ) => {
    try {
      const enc = new TextDecoder('utf-8');
      const resp = enc.decode(httpResponse.body);
      const filesByType = JSON.parse(resp);

      for (const filetype of Object.keys(filesByType)) {
        const files = filesByType[filetype];
        for (const encodedFileBuffer of files) {
          const buffer = Uint8Array.from(window.atob(encodedFileBuffer), (c) =>
            c.charCodeAt(0),
          );
          const blob = new Blob([buffer]);
          const newFile = new File([blob], filetype);
          this.adbData.push(newFile);
        }
      }
    } catch (error) {
      this.setState(
        ConnectionState.ERROR,
        `Could not fetch files. Received:\n${httpResponse.text}`,
      );
    }
  };

  private isWaylandAvailable(): Promise<boolean> {
    return new Promise((resolve) => {
      this.getFromProxy(
        ProxyEndpoint.CHECK_WAYLAND,
        (request: HttpResponse) => {
          resolve(request.text === 'true');
        },
      );
    });
  }

  private async getFromProxy(
    path: string,
    onSuccess: OnRequestSuccessCallback = FunctionUtils.DO_NOTHING,
    type?: XMLHttpRequest['responseType'],
  ) {
    const response = await HttpRequest.get(
      this.makeRequestPath(path),
      this.getSecurityTokenHeader(),
      type,
    );
    await this.processProxyResponse(response, onSuccess);
  }

  private async postToProxy(
    path: string,
    onSuccess: OnRequestSuccessCallback = FunctionUtils.DO_NOTHING,
    jsonRequest?: object,
  ) {
    const response = await HttpRequest.post(
      this.makeRequestPath(path),
      this.getSecurityTokenHeader(),
      jsonRequest,
    );
    await this.processProxyResponse(response, onSuccess);
  }

  private async processProxyResponse(
    response: HttpResponse,
    onSuccess: OnRequestSuccessCallback = FunctionUtils.DO_NOTHING,
  ) {
    if (
      response.status === HttpRequestStatus.SUCCESS &&
      !this.isVersionCompatible(response)
    ) {
      await this.setState(ConnectionState.INVALID_VERSION);
      return;
    }
    const adbResponse = await this.processHttpResponse(response, onSuccess);
    if (adbResponse !== undefined) {
      await this.setState(adbResponse.newState, adbResponse.errorMsg);
    }
  }

  private isVersionCompatible(req: HttpResponse): boolean {
    const proxyVersion = req.getHeader('Winscope-Proxy-Version');
    if (!proxyVersion) return false;
    const [proxyMajor, proxyMinor, proxyPatch] = proxyVersion
      .split('.')
      .map((s) => Number(s));
    const [clientMajor, clientMinor, clientPatch] =
      ProxyConnection.VERSION.split('.').map((s) => Number(s));

    if (proxyMajor !== clientMajor) {
      return false;
    }

    if (proxyMinor === clientMinor) {
      // Check patch number to ensure user has deployed latest bug fixes
      return proxyPatch >= clientPatch;
    }

    return proxyMinor > clientMinor;
  }

  private getSecurityTokenHeader(): HttpRequestHeaderType {
    const lastKey = this.store?.get('adb.proxyKey');
    if (lastKey !== undefined) {
      this.securityToken = lastKey;
    }
    return [['Winscope-Token', this.securityToken]];
  }

  private makeRequestPath(path: string): string {
    return ProxyConnection.WINSCOPE_PROXY_URL + path;
  }
}
