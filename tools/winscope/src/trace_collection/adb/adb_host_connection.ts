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

import {HttpResponse} from 'common/http_request';
import {PersistentStore} from 'common/store/persistent_store';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionState} from 'trace_collection/connection_state';
import {ConnectionStateListener} from 'trace_collection/connection_state_listener';
import {AdbDeviceConnection} from './adb_device_connection';

export abstract class AdbHostConnection<
  D extends AdbDeviceConnection = AdbDeviceConnection,
> {
  protected devices: D[] = [];
  protected readonly store = new PersistentStore();
  abstract readonly connectionType: AdbConnectionType;

  constructor(protected listener: ConnectionStateListener) {
    this.initializeExtraParameters();
  }

  async restart(): Promise<void> {
    this.onDestroy();
    await this.setState(ConnectionState.CONNECTING);
  }

  getDevices(): D[] {
    return this.devices;
  }

  protected async setState(newState: ConnectionState, errorText = '') {
    if (newState === ConnectionState.ERROR) {
      await this.listener.onError(errorText);
    } else {
      await this.listener.onConnectionStateChange(newState);
    }
  }

  onDestroy() {
    this.destroyHost();
    this.devices.forEach((device) => device.onDestroy());
  }

  protected abstract initializeExtraParameters(): void;
  protected abstract destroyHost(): void;
  abstract setSecurityToken(token: string): void;
  abstract requestDevices(): Promise<void>;
  abstract cancelDeviceRequests(): void;
}

export interface AdbResponse {
  errorState: ConnectionState;
  errorMsg: string | undefined;
}

/**
 * Type for the callback function that is called when a request is successful.
 */
export type OnRequestSuccessCallback = (
  resp: HttpResponse,
) => void | Promise<void> | Promise<Uint8Array>;
