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

import {assertUnreachable} from 'common/assert_utils';
import {HttpRequestStatus, HttpResponse} from 'common/http_request';
import {AdbDevice} from './adb_device';
import {ConnectionState} from './connection_state';
import {TraceRequest} from './trace_request';

export abstract class AdbConnection {
  abstract initialize(
    detectStateChangeInUi: () => Promise<void>,
    availableTracesChangeCallback: (traces: string[]) => void,
    devicesChangeCallback: (devices: AdbDevice[]) => void,
  ): Promise<void>;
  abstract restartConnection(): Promise<void>;
  abstract setSecurityToken(token: string): void;
  abstract getDevices(): AdbDevice[];
  abstract getState(): ConnectionState;
  abstract getErrorText(): string;
  abstract startTrace(
    device: AdbDevice,
    requestedTraces: TraceRequest[],
  ): Promise<void>;
  abstract endTrace(device: AdbDevice): Promise<void>;
  abstract dumpState(
    device: AdbDevice,
    requestedDumps: TraceRequest[],
  ): Promise<void>;
  abstract fetchLastTracingSessionData(device: AdbDevice): Promise<File[]>;
  abstract onDestroy(): void;

  protected async processHttpResponse(
    resp: HttpResponse,
    onSuccess: OnRequestSuccessCallback,
  ): Promise<AdbResponse | undefined> {
    let newState: ConnectionState | undefined;
    let errorMsg: string | undefined;

    switch (resp.status) {
      case HttpRequestStatus.UNSENT:
        newState = ConnectionState.NOT_FOUND;
        break;

      case HttpRequestStatus.UNAUTH:
        newState = ConnectionState.UNAUTH;
        break;

      case HttpRequestStatus.SUCCESS:
        try {
          await onSuccess(resp);
        } catch (err) {
          newState = ConnectionState.ERROR;
          errorMsg =
            `Error handling request response:\n${err}\n\n` +
            `Request:\n ${resp.text}`;
        }
        break;

      case HttpRequestStatus.ERROR:
        if (resp.type === 'text' || !resp.type) {
          errorMsg = resp.text;
        } else if (resp.type === 'arraybuffer') {
          errorMsg = String.fromCharCode.apply(null, new Array(resp.body));
          if (errorMsg === '\x00') {
            errorMsg = 'No data received.';
          }
        }
        newState = ConnectionState.ERROR;
        break;

      default:
        assertUnreachable(resp.status);
    }

    return newState !== undefined ? {newState, errorMsg} : undefined;
  }
}

interface AdbResponse {
  newState: ConnectionState;
  errorMsg: string | undefined;
}

export type OnRequestSuccessCallback = (
  resp: HttpResponse,
) => void | Promise<void>;
