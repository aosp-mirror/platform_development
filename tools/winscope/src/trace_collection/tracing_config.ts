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
import {globalConfig} from 'common/global_config';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceConfigurationMap, TRACES} from './trace_collection_utils';

export class TracingConfig {
  requestedTraces: string[] = [];
  requestedDumps: string[] = [];

  private storage: Storage;
  private traceConfig: TraceConfigurationMap;
  private dumpConfig: TraceConfigurationMap;

  private static instance: TracingConfig | undefined;

  static getInstance(): TracingConfig {
    if (!TracingConfig.instance) {
      TracingConfig.instance = new TracingConfig();
    }
    return TracingConfig.instance;
  }

  setTraceConfigForAvailableTraces(isWaylandAvailable = false) {
    const availableTracesConfig = TRACES['default'];
    if (isWaylandAvailable) {
      Object.assign(availableTracesConfig, TRACES['arc']);
    }
    this.setTraceConfig(availableTracesConfig);
  }

  setTraceConfig(traceConfig: TraceConfigurationMap) {
    this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
      'TraceConfiguration',
      traceConfig,
      this.storage
    );
  }

  getTraceConfig(): TraceConfigurationMap {
    return this.traceConfig;
  }

  getDumpConfig(): TraceConfigurationMap {
    if (this.dumpConfig === undefined) {
      throw Error('Dump config not initialized yet');
    }
    return this.dumpConfig;
  }

  private constructor() {
    this.storage = globalConfig.MODE === 'PROD' ? localStorage : new MockStorage();

    this.traceConfig = PersistentStoreProxy.new<TraceConfigurationMap>(
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
}
