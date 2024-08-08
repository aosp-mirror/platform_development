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
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {ProxyTracingErrors} from 'messaging/user_warnings';
import {AdbConnection, OnRequestSuccessCallback} from './adb_connection';
import {AdbDevice} from './adb_device';
import {ConnectionState} from './connection_state';
import {ProxyEndpoint} from './proxy_endpoint';
import {TraceRequest} from './trace_request';

export class ProxyConnection extends AdbConnection {
  static readonly VERSION = '2.6.0';
  static readonly WINSCOPE_PROXY_URL = 'http://localhost:5544';

  private readonly store = new PersistentStore();
  private readonly storeKeySecurityToken = 'adb.proxyKey';

  private state: ConnectionState = ConnectionState.CONNECTING;
  private errorText = '';
  private securityToken = '';
  private devices: AdbDevice[] = [];
  selectedDevice: AdbDevice | undefined;
  private requestedTraces: TraceRequest[] = [];
  private adbData: File[] = [];
  private keepTraceAliveWorker: number | undefined;
  private refreshDevicesWorker: number | undefined;
  private detectStateChangeInUi: () => Promise<void> =
    FunctionUtils.DO_NOTHING_ASYNC;
  private progressCallback: OnProgressUpdateType = FunctionUtils.DO_NOTHING;
  private availableTracesChangeCallback: (traces: string[]) => void =
    FunctionUtils.DO_NOTHING;

  async initialize(
    detectStateChangeInUi: () => Promise<void>,
    progressCallback: OnProgressUpdateType,
    availableTracesChangeCallback: (traces: string[]) => void,
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
    if (token.length > 0) {
      this.securityToken = token;
      this.store?.add(this.storeKeySecurityToken, token);
    }
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
    window.clearInterval(this.refreshDevicesWorker);
    this.refreshDevicesWorker = undefined;
    window.clearInterval(this.keepTraceAliveWorker);
    this.keepTraceAliveWorker = undefined;
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
    this.requestedTraces = [];
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
    await this.getFromProxy(
      `${ProxyEndpoint.FETCH}${device.id}/`,
      this.onSuccessFetchFiles,
      'arraybuffer',
    );
    if (this.adbData.length === 0) {
      Analytics.Proxy.logNoFilesFound();
    }
  }

  private async onConnectionStateChange(newState: ConnectionState) {
    await this.detectStateChangeInUi();

    switch (newState) {
      case ConnectionState.ERROR:
        Analytics.Error.logProxyError(this.errorText);
        return;

      case ConnectionState.CONNECTING:
        await this.requestDevices();
        return;

      case ConnectionState.IDLE:
        {
          const isWaylandAvailable = await this.isWaylandAvailable();
          if (isWaylandAvailable) {
            this.availableTracesChangeCallback(['wayland_trace']);
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
          (response: HttpResponse) => {
            const errors = JSON.parse(response.body);
            if (Array.isArray(errors) && errors.length > 0) {
              const processedErrors: string[] = errors.map((error: string) => {
                const processed = error
                  .replace("b'", "'")
                  .replace('\\n', '')
                  .replace(
                    'please check your display state',
                    'please check your display state (must be on at start of trace)',
                  );
                return processed;
              });
              UserNotifier.add(
                new ProxyTracingErrors(processedErrors),
              ).notify();
            }
          },
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
          throw new Error('No device selected');
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
      window.clearInterval(this.keepTraceAliveWorker);
      this.keepTraceAliveWorker = undefined;
      return;
    }

    await this.getFromProxy(
      `${ProxyEndpoint.STATUS}${assertDefined(this.selectedDevice).id}/`,
      async (request: HttpResponse) => {
        if (request.text !== 'True') {
          window.clearInterval(this.keepTraceAliveWorker);
          this.keepTraceAliveWorker = undefined;
          await this.endTrace();
          if (this.state === ConnectionState.ENDING_TRACE) {
            await this.setState(ConnectionState.TRACE_TIMEOUT);
          }
        } else if (this.keepTraceAliveWorker === undefined) {
          this.keepTraceAliveWorker = window.setInterval(
            () => this.keepTraceAlive(),
            1000,
          );
        }
      },
    );
  }

  private async setState(state: ConnectionState, errorText = '') {
    const connectedStates = [
      ConnectionState.IDLE,
      ConnectionState.STARTING_TRACE,
      ConnectionState.TRACING,
      ConnectionState.ENDING_TRACE,
      ConnectionState.DUMPING_STATE,
      ConnectionState.LOADING_DATA,
    ];
    if (
      state === ConnectionState.NOT_FOUND &&
      connectedStates.includes(this.state)
    ) {
      Analytics.Proxy.logServerNotFound();
    }
    this.state = state;
    this.errorText = errorText;
    await this.onConnectionStateChange(state);
  }

  private async requestDevices() {
    if (
      this.state !== ConnectionState.IDLE &&
      this.state !== ConnectionState.CONNECTING
    ) {
      if (this.refreshDevicesWorker !== undefined) {
        window.clearInterval(this.refreshDevicesWorker);
        this.refreshDevicesWorker = undefined;
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
      if (this.refreshDevicesWorker === undefined) {
        this.refreshDevicesWorker = window.setInterval(
          () => this.requestDevices(),
          1000,
        );
      }
      if (this.state === ConnectionState.CONNECTING) {
        this.setState(ConnectionState.IDLE);
      } else if (this.state === ConnectionState.IDLE) {
        this.detectStateChangeInUi();
      }
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
    const lastKey = this.store?.get(this.storeKeySecurityToken);
    if (lastKey !== undefined) {
      this.securityToken = lastKey;
    }
    return [['Winscope-Token', this.securityToken]];
  }

  private makeRequestPath(path: string): string {
    return ProxyConnection.WINSCOPE_PROXY_URL + path;
  }
}
