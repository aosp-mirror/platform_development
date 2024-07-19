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
import {PersistentStore} from 'common/persistent_store';
import {TimeUtils} from 'common/time_utils';
import {Analytics} from 'logging/analytics';
import {AdbConnection} from './adb_connection';
import {AdbDevice} from './adb_device';
import {ConnectionState} from './connection_state';
import {HttpRequest} from './http_request';
import {OnRequestSuccessCallback} from './on_request_success_callback';
import {ProxyEndpoint} from './proxy_endpoint';
import {RequestHeaderType} from './request_header_type';
import {TraceConfigurationMap, TRACES} from './trace_collection_utils';
import {TraceRequest} from './trace_request';

export class ProxyConnection implements AdbConnection {
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
        await this.requestDevices(this);
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
          () => this.keepTraceAlive(this),
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

  private async keepTraceAlive(view: ProxyConnection) {
    const state = view.getState();
    if (
      state !== ConnectionState.STARTING_TRACE &&
      state !== ConnectionState.TRACING
    ) {
      clearInterval(view.keep_trace_alive_worker);
      view.keep_trace_alive_worker = undefined;
      return;
    }

    await view.getFromProxy(
      `${ProxyEndpoint.STATUS}${assertDefined(view.selectedDevice).id}/`,
      async (request: XMLHttpRequest) => {
        if (request.responseText !== 'True') {
          view.endTrace();
        } else if (view.keep_trace_alive_worker === undefined) {
          view.keep_trace_alive_worker = setInterval(
            view.keepTraceAlive,
            1000,
            view,
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

  private async requestDevices(view: ProxyConnection) {
    if (
      view.state !== ConnectionState.IDLE &&
      view.state !== ConnectionState.CONNECTING
    ) {
      if (view.refresh_devices_worker !== undefined) {
        clearInterval(view.refresh_devices_worker);
        view.refresh_devices_worker = undefined;
      }
      return;
    }

    const onSuccessGetDevices: OnRequestSuccessCallback = (
      req: XMLHttpRequest,
    ) => view.onSuccessGetDevices(view, req);

    await view.getFromProxy(ProxyEndpoint.DEVICES, onSuccessGetDevices);
  }

  private async onReadyStateChange(
    view: ProxyConnection,
    req: XMLHttpRequest,
    resolve: (value: void | PromiseLike<void>) => void,
    onSuccess: OnRequestSuccessCallback,
  ) {
    if (req.readyState !== XMLHttpRequest.DONE) {
      return;
    }
    if (req.status === XMLHttpRequest.UNSENT) {
      view.setState(ConnectionState.NOT_FOUND);
      resolve();
    } else if (req.status === 200) {
      if (
        !view.areVersionsCompatible(
          req.getResponseHeader('Winscope-Proxy-Version'),
        )
      ) {
        view.setState(ConnectionState.INVALID_VERSION);
        resolve();
      } else if (onSuccess) {
        try {
          await onSuccess(req);
        } catch (err) {
          console.error(err);
          view.setState(
            ConnectionState.ERROR,
            `Error handling request response:\n${err}\n\n` +
              `Request:\n ${req.responseText}`,
          );
          resolve();
        }
      }
      resolve();
    } else if (req.status === 403) {
      view.setState(ConnectionState.UNAUTH);
      resolve();
    } else {
      let errorMsg = '';
      if (req.responseType === 'text' || !req.responseType) {
        errorMsg = req.responseText;
      } else if (req.responseType === 'arraybuffer') {
        errorMsg = String.fromCharCode.apply(null, new Array(req.response));
      }
      view.setState(ConnectionState.ERROR, errorMsg);
      resolve();
    }
  }

  private onSuccessGetDevices = async (
    view: ProxyConnection,
    request: XMLHttpRequest,
  ) => {
    try {
      const devices = JSON.parse(request.responseText);
      view.devices = Object.keys(devices).map((deviceId) => {
        return {
          id: deviceId,
          authorized: devices[deviceId].authorized,
          model: devices[deviceId].model,
        };
      });
      if (view.refresh_devices_worker === undefined) {
        view.refresh_devices_worker = setInterval(
          () => view.requestDevices(view),
          1000,
        );
      }
      view.setState(ConnectionState.IDLE);
    } catch (err) {
      view.setState(ConnectionState.ERROR, request.responseText);
    }
  };

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
          this.adbData.push(newFile);
        }
      }
    } catch (error) {
      this.setState(ConnectionState.ERROR, request.responseText);
      throw error;
    }
  };

  private areVersionsCompatible(proxyVersion: string | null): boolean {
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

  private isWaylandAvailable(): Promise<boolean> {
    return new Promise((resolve) => {
      this.getFromProxy(
        ProxyEndpoint.CHECK_WAYLAND,
        (request: XMLHttpRequest) => {
          resolve(request.responseText === 'true');
        },
      );
    });
  }

  private async getFromProxy(
    path: string,
    onSuccess: OnRequestSuccessCallback = FunctionUtils.DO_NOTHING,
    type?: XMLHttpRequest['responseType'],
  ) {
    await HttpRequest.get(
      this.makeRequestPath(path),
      this.getSecurityTokenHeader(),
      (req: XMLHttpRequest, resolve: ResolvePromiseType) =>
        this.onReadyStateChange(this, req, resolve, onSuccess),
      type,
    );
  }

  private async postToProxy(
    path: string,
    onSuccess: OnRequestSuccessCallback = FunctionUtils.DO_NOTHING,
    jsonRequest?: object,
  ) {
    await HttpRequest.post(
      this.makeRequestPath(path),
      this.getSecurityTokenHeader(),
      (req: XMLHttpRequest, resolve: ResolvePromiseType) =>
        this.onReadyStateChange(this, req, resolve, onSuccess),
      jsonRequest,
    );
  }

  private getSecurityTokenHeader(): RequestHeaderType {
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

type ResolvePromiseType = (value: void | PromiseLike<void>) => void;
