/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {AdbDeviceState} from 'trace_collection/adb/adb_device_connection';
import {AdbHostConnection} from 'trace_collection/adb/adb_host_connection';
import {AdbConnectionType} from 'trace_collection/adb_connection_type';
import {ConnectionState} from 'trace_collection/connection_state';
import {MockAdbDeviceConnection} from 'trace_collection/mock/mock_adb_device_connection';

export class MockAdbHostConnection extends AdbHostConnection<MockAdbDeviceConnection> {
  readonly connectionType = AdbConnectionType.MOCK;

  override devices: MockAdbDeviceConnection[] = [
    new MockAdbDeviceConnection(
      '35562',
      'Pixel 6',
      AdbDeviceState.AVAILABLE,
      this.listener,
    ),
  ];

  override initializeExtraParameters() {}

  override destroyHost() {}

  override setSecurityToken(token: string) {
    // do nothing
  }

  override cancelDeviceRequests() {
    // do nothing
  }

  override async requestDevices(): Promise<void> {
    this.listener.onDevicesChange(this.devices);
    await this.setState(ConnectionState.IDLE);
  }
}
