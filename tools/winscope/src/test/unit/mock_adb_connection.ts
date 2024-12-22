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

import {FunctionUtils} from 'common/function_utils';
import {AdbConnection} from 'trace_collection/adb_connection';
import {AdbDevice} from 'trace_collection/adb_device';
import {ConnectionState} from 'trace_collection/connection_state';
import {TraceRequest} from 'trace_collection/trace_request';

export class MockAdbConnection extends AdbConnection {
  state: ConnectionState = ConnectionState.CONNECTING;
  errorText = '';
  files = [new File([], 'test_file')];
  devices: AdbDevice[] = [];
  availableTracesChangeCallback: (traces: string[]) => void =
    FunctionUtils.DO_NOTHING;
  devicesChangeCallback: (devices: AdbDevice[]) => void =
    FunctionUtils.DO_NOTHING;
  private detectStateChangeInUi: () => Promise<void> =
    FunctionUtils.DO_NOTHING_ASYNC;

  async initialize(
    detectStateChangeInUi: () => Promise<void>,
    availableTracesChangeCallback: (traces: string[]) => void,
    devicesChangeCallback: (devices: AdbDevice[]) => void,
  ) {
    this.detectStateChangeInUi = detectStateChangeInUi;
    this.availableTracesChangeCallback = availableTracesChangeCallback;
    this.devicesChangeCallback = devicesChangeCallback;
    this.setState(ConnectionState.CONNECTING);
  }

  setState(state: ConnectionState) {
    this.state = state;
    this.detectStateChangeInUi();
  }

  async restartConnection(): Promise<void> {
    this.setState(ConnectionState.CONNECTING);
  }

  setSecurityToken(token: string) {
    // do nothing
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
    // do nothing
  }

  async startTrace(
    device: AdbDevice,
    requestedTraces: TraceRequest[],
  ): Promise<void> {
    this.setState(ConnectionState.STARTING_TRACE);
  }

  async endTrace() {
    this.setState(ConnectionState.ENDING_TRACE);
  }

  async dumpState(
    device: AdbDevice,
    requestedDumps: TraceRequest[],
  ): Promise<void> {
    this.setState(ConnectionState.DUMPING_STATE);
  }

  async fetchLastTracingSessionData(device: AdbDevice): Promise<File[]> {
    this.setState(ConnectionState.LOADING_DATA);
    return this.files;
  }
}
