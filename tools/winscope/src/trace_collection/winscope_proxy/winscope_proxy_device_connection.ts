/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {byteArrayToString} from 'common/buffer_utils';
import {FunctionUtils} from 'common/function_utils';
import {HttpRequestHeaderType, HttpResponse} from 'common/http_request';
import {UserNotifier} from 'common/user_notifier';
import {ProxyTracingErrors} from 'messaging/user_warnings';
import {
  AdbDeviceConnection,
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {ConnectionState} from 'trace_collection/connection_state';
import {TraceTarget} from 'trace_collection/trace_target';
import {Endpoint} from './endpoint';
import {getFromProxy, postToProxy} from './utils';

export class WinscopeProxyDeviceConnection extends AdbDeviceConnection {
  private isTracing = true;
  private keepTraceAliveWorkers: Array<{name: string; worker: number}> = [];

  constructor(
    id: string,
    listener: AdbDeviceConnectionListener,
    private securityHeader: HttpRequestHeaderType,
  ) {
    super(id, listener);
  }

  override onDestroy() {
    this.isTracing = false;
    this.keepTraceAliveWorkers.forEach(({name, worker}) => {
      window.clearInterval(worker);
    });
    this.keepTraceAliveWorkers = [];
  }

  override async tryAuthorize() {
    throw new Error('not implemented');
  }

  override async runShellCommand(cmd: string): Promise<string> {
    return await postToProxy(
      `${Endpoint.RUN_ADB_CMD}${this.id}/`,
      this.securityHeader,
      FunctionUtils.DO_NOTHING,
      (newState, errorText) => this.setState(newState, errorText),
      {cmd: 'shell ' + cmd},
    );
  }

  override async pullFile(filepath: string): Promise<Uint8Array> {
    return await new Promise<Uint8Array>((resolve) => {
      getFromProxy(
        `${Endpoint.FETCH}${this.id}/${filepath}`,
        this.securityHeader,
        (response) => {
          resolve(this.onSuccessFetchFile(response, filepath));
        },
        async (newState, errorText) => {
          this.setState(newState, errorText);
          resolve(Uint8Array.from([]));
        },
        'arraybuffer',
      );
    });
  }

  private async setState(newState: ConnectionState, errorText = '') {
    if (newState === ConnectionState.ERROR) {
      await this.listener.onError(errorText);
    } else {
      await this.listener.onConnectionStateChange(newState);
    }
  }

  private onSuccessFetchFile = async (
    httpResponse: HttpResponse,
    filepath: string,
  ) => {
    try {
      const resp = byteArrayToString(httpResponse.body);
      const fileToPath = JSON.parse(resp);
      const encodedFileBuffer = fileToPath[filepath];
      return Uint8Array.from(window.atob(encodedFileBuffer), (c) =>
        c.charCodeAt(0),
      );
    } catch (error) {
      await this.listener.onError(
        `Could not fetch file. Received: ${httpResponse.text}`,
      );
      return Uint8Array.from([]);
    }
  };

  override async startTrace(target: TraceTarget) {
    this.isTracing = true;
    console.debug(`Starting trace for ${target.traceName} on ${this.id}`);
    await postToProxy(
      `${Endpoint.START_TRACE}${this.id}/`,
      this.securityHeader,
      (response: HttpResponse) => {
        this.keepTraceAlive(target.traceName);
      },
      (newState, errorText) => this.setState(newState, errorText),
      {
        targetId: target.traceName,
        startCmd: target.startCmd,
        stopCmd: target.stopCmd,
      },
    );
    console.debug(`Started trace for ${target.traceName} on ${this.id}`);
  }

  override async endTrace(target: TraceTarget) {
    this.isTracing = false;
    console.debug(`Ending trace for ${target.traceName} on ${this.id}`);
    await postToProxy(
      `${Endpoint.END_TRACE}${this.id}/`,
      this.securityHeader,
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
          UserNotifier.add(new ProxyTracingErrors(processedErrors));
        }
      },
      (newState, errorText) => this.setState(newState, errorText),
      {targetId: target.traceName},
    );
    console.debug(`Ended trace for ${target.traceName}.`);
  }

  protected override async updatePropertiesFromResponse(
    resp: WinscopeProxyDeviceConnectionResponse,
  ): Promise<void> {
    this.state = resp.authorized
      ? AdbDeviceState.AVAILABLE
      : AdbDeviceState.UNAUTHORIZED;
    this.model = resp.model;
  }

  private clearTraceAliveWorker(target: string) {
    this.keepTraceAliveWorkers = this.keepTraceAliveWorkers.filter(
      ({name, worker}) => {
        if (target === name) {
          window.clearInterval(worker);
          return false;
        }
        return true;
      },
    );
  }

  private async keepTraceAlive(targetName: string) {
    if (!this.isTracing) {
      this.clearTraceAliveWorker(targetName);
      return;
    }

    await getFromProxy(
      `${Endpoint.STATUS}${this.id}/${targetName}`,
      this.securityHeader,
      async (request: HttpResponse) => {
        if (request.text !== 'True') {
          this.clearTraceAliveWorker(targetName);
          console.warn(targetName + ' timed out');
          await this.listener.onConnectionStateChange(
            ConnectionState.TRACE_TIMEOUT,
          );
        } else {
          const workerExists = this.keepTraceAliveWorkers.some(
            ({name, worker}) => name === targetName,
          );
          if (!workerExists && this.isTracing) {
            const worker = window.setInterval(
              () => this.keepTraceAlive(targetName),
              1000,
            );
            this.keepTraceAliveWorkers.push({
              name: targetName,
              worker,
            });
          }
        }
      },
      (newState, errorText) => this.setState(newState, errorText),
    );
  }
}

export interface WinscopeProxyDeviceConnectionResponse {
  id: string;
  authorized: boolean;
  model: string;
}
