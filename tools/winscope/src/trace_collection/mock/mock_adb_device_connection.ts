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

import {
  AdbDeviceConnection,
  AdbDeviceConnectionListener,
  AdbDeviceState,
} from 'trace_collection/adb/adb_device_connection';
import {TraceTarget} from 'trace_collection/trace_target';

/**
 * Represents an ADB device.
 */
export class MockAdbDeviceConnection extends AdbDeviceConnection {
  startTraceSuccess = true;

  constructor(
    id: string,
    model: string,
    state: AdbDeviceState,
    listener: AdbDeviceConnectionListener,
    displays: string[] = [],
    multiDisplayScreenRecording = false,
  ) {
    super(id, listener);
    this.state = state;
    this.model = model;
    this.displays = displays;
    this.multiDisplayScreenRecording = multiDisplayScreenRecording;
  }

  override async startTrace(target: TraceTarget) {}

  override async endTrace(target: TraceTarget): Promise<void> {}

  override async tryAuthorize(): Promise<void> {}
  override async runShellCommand(cmd: string): Promise<string> {
    return '';
  }

  override async pullFile(filepath: string): Promise<Uint8Array> {
    return Uint8Array.from([]);
  }

  override onDestroy() {
    // do nothing
  }

  protected override updatePropertiesFromResponse(resp: object) {}
}
