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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {TraceConfigurationMap, TRACES} from './trace_collection_utils';

export class TracingConfig {
  public requestedTraces: string[] = [];
  public requestedDumps: string[] = [];

  private storage: Storage | undefined;
  private tracingConfig: TraceConfigurationMap | undefined;
  private dumpConfig: TraceConfigurationMap | undefined;

  static getInstance(): TracingConfig {
    return setTracesInstance;
  }

  public initialize(storage: Storage) {
    this.storage = storage;
    this.tracingConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TracingSettings',
      TRACES['default'],
      this.storage
    );
    this.dumpConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'DumpSettings',
      {
        window_dump: {
          name: 'Window Manager',
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
        layers_dump: {
          name: 'Surface Flinger',
          isTraceCollection: undefined,
          run: true,
          config: undefined,
        },
      },
      this.storage
    );
  }

  public setTracingConfigForAvailableTraces(isWaylandAvailable = false) {
    const availableTracesConfig = TRACES['default'];
    if (isWaylandAvailable) {
      Object.assign(availableTracesConfig, TRACES['arc']);
    }
    this.setTracingConfig(availableTracesConfig);
  }

  public tracingConfigIsSet(): boolean {
    return this.tracingConfig !== undefined;
  }

  public getTracingConfig(): TraceConfigurationMap {
    if (this.tracingConfig === undefined) {
      throw Error('Tracing config not initialized yet');
    }
    return this.tracingConfig;
  }

  private setTracingConfig(traceConfig: TraceConfigurationMap) {
    if (this.storage === undefined) {
      throw Error('not initialized');
    }
    this.tracingConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TraceConfiguration',
      traceConfig,
      this.storage
    );
  }

  public getDumpConfig(): TraceConfigurationMap {
    if (this.dumpConfig === undefined) {
      throw Error('Dump config not initialized yet');
    }
    return this.dumpConfig;
  }
}

const setTracesInstance = new TracingConfig();
