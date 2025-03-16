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

import {WindowUtils} from 'common/window_utils';
import {AdbHostConnection} from 'trace_collection/adb/adb_host_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionState} from 'trace_collection/connection_state';
import {StreamProvider} from './stream_provider';
import {WdpDeviceConnection} from './wdp_device_connection';

/**
 * A connection to the WebDeviceProxy websocket.
 */
export class WdpHostConnection extends AdbHostConnection<WdpDeviceConnection> {
  private static readonly WDP_TRACK_DEVICES_URL =
    'ws://localhost:9167/track-devices-json';
  readonly connectionType = AdbConnectionType.WDP;
  private streamProvider = new StreamProvider();

  protected override initializeExtraParameters() {
    // do nothing
  }

  protected override destroyHost() {
    this.streamProvider.closeAllStreams();
  }

  override setSecurityToken(token: string) {
    // do nothing
  }

  override cancelDeviceRequests() {
    // do nothing
  }

  override async requestDevices() {
    const sock = new WebSocket(WdpHostConnection.WDP_TRACK_DEVICES_URL);
    const devicesStream = this.streamProvider.createDevicesStream(
      sock,
      async (message: string) => {
        await this.handleRequestDevicesResponse(message);
      },
      () => {
        this.setState(ConnectionState.NOT_FOUND);
      },
    );
    await devicesStream.connect();
  }

  private async handleRequestDevicesResponse(data: string) {
    const resp: WdpRequestDevicesResponse = JSON.parse(data);
    if (
      resp.error?.type === 'ORIGIN_NOT_ALLOWLISTED' &&
      resp.error.approveUrl !== undefined
    ) {
      const popup = WindowUtils.showPopupWindow(resp.error.approveUrl);
      if (popup === false) {
        this.listener.onError(`Please enable popups and try again.`);
        return;
      }
      this.setState(ConnectionState.UNAUTH);
      return;
    } else if (resp.error !== undefined) {
      console.error(`Invalid WebDeviceProxy response ${data} : ${resp.error}`);
      this.listener.onError(resp.error.message ?? 'Unknown WDP Error');
      return;
    }
    await this.onRequestDevicesResponse(resp);
    this.setState(ConnectionState.IDLE);
    return;
  }

  private async onRequestDevicesResponse(resp: WdpRequestDevicesResponse) {
    const curDevs = new Map<string, WdpDeviceConnectionResponse>(
      (resp.device ?? []).map((d) => [d.serialNumber, d]),
    );
    this.devices = this.devices.filter((d) => curDevs.has(d.id));

    for (const [serial, devJson] of curDevs.entries()) {
      const existingDevice = this.devices.find((d) => d.id === serial);
      if (existingDevice !== undefined) {
        await existingDevice.updateProperties(devJson);
      } else {
        const newDevice = new WdpDeviceConnection(
          serial,
          this.listener,
          devJson.approveUrl,
        );
        await newDevice.updateProperties(devJson);
        this.devices.push(newDevice);
      }
    }
    this.listener.onDevicesChange(this.devices);
  }
}

export interface WdpRequestDevicesResponse {
  device?: WdpDeviceConnectionResponse[];
  version?: string;
  error?: {
    type?: string;
    message?: string;
    approveUrl?: string;
  };
}

export interface WdpDeviceConnectionResponse {
  serialNumber: string;
  proxyStatus: 'ADB' | 'PROXY_UNAUTHORIZED';
  adbStatus: string;
  adbProps?: {[key: string]: string};
  approveUrl?: string;
}
