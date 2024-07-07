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
import {DeviceProperties, Devices} from 'trace_collection/proxy_client';
import {ConfigMap} from './trace_collection_utils';

export interface AdbConnection {
  adbSuccess: () => boolean;
  setSecurityKey(key: string): void;
  getDevices(): Devices;
  getSelectedDevice(): [string, DeviceProperties];
  restart(): Promise<void>;
  selectDevice(id: string): Promise<void>;
  clearLastDevice(): Promise<void>;
  isDevicesState(): boolean;
  isConfigureTraceState(): boolean;
  isErrorState(): boolean;
  isStartingTraceState(): boolean;
  isTracingState(): boolean;
  isLoadingDataState(): boolean;
  isConnectingState(): boolean;
  setErrorState(message: string): Promise<void>;
  setLoadingDataState(): void;
  startTrace(
    requestedTraces: string[],
    reqEnableConfig?: string[],
    reqSelectedSfConfig?: ConfigMap,
    reqSelectedWmConfig?: ConfigMap,
  ): Promise<void>;
  endTrace(): Promise<void>;
  fetchExistingTraces(): Promise<void>;
  getAdbData(): File[];
  dumpState(requestedDumps: string[]): Promise<boolean>;
  getErrorText(): string;
  onDestroy(): void;
}
