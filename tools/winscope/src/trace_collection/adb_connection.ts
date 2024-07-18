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

import {OnProgressUpdateType} from 'common/function_utils';
import {AdbDevice} from './adb_device';
import {ConnectionState} from './connection_state';
import {TraceConfigurationMap} from './trace_collection_utils';
import {TraceRequest} from './trace_request';

export interface AdbConnection {
  initialize(
    detectStateChangeInUi: () => Promise<void>,
    progressCallback: OnProgressUpdateType,
    availableTracesChangeCallback: (
      availableTracesConfig: TraceConfigurationMap,
    ) => void,
  ): Promise<void>;
  restartConnection(): Promise<void>;
  setSecurityToken(token: string): void;
  getDevices(): AdbDevice[];
  getState(): ConnectionState;
  getErrorText(): string;
  startTrace(device: AdbDevice, requestedTraces: TraceRequest[]): Promise<void>;
  endTrace(device: AdbDevice): Promise<void>;
  dumpState(device: AdbDevice, requestedDumps: TraceRequest[]): Promise<void>;
  fetchLastTracingSessionData(device: AdbDevice): Promise<File[]>;
  onDestroy(): void;
}
