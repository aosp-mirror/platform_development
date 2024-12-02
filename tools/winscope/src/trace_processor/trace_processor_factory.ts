/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {globalConfig} from 'common/global_config';
import {UrlUtils} from 'common/url_utils';
import {
  initWasm,
  resetEngineWorker,
  WasmEngineProxy,
} from 'trace_processor/wasm_engine_proxy';

export class TraceProcessorFactory {
  private static wasmEngine?: WasmEngineProxy;

  static async getSingleInstance(): Promise<WasmEngineProxy> {
    if (!TraceProcessorFactory.wasmEngine) {
      const traceProcessorRootUrl =
        globalConfig.MODE === 'KARMA_TEST'
          ? UrlUtils.getRootUrl() +
            'base/deps_build/trace_processor/to_be_served/'
          : UrlUtils.getRootUrl();
      initWasm(traceProcessorRootUrl);
      const engineId = 'random-id';
      const enginePort = resetEngineWorker();
      TraceProcessorFactory.wasmEngine = new WasmEngineProxy(engineId, enginePort);
    }

    return TraceProcessorFactory.wasmEngine;
  }
}
