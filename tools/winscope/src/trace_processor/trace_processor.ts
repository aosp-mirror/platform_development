/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {Analytics} from 'logging/analytics';
import {TraceProcessorConfig} from './engine';
import {QueryResult} from './query_result';
import {WasmEngineProxy} from './wasm_engine_proxy';

export class TraceProcessor {
  private wasmEngine: WasmEngineProxy;

  constructor(engineId: string, enginePort: MessagePort) {
    this.wasmEngine = new WasmEngineProxy(engineId, enginePort);
  }

  async query(sqlQuery: string): Promise<QueryResult> {
    const startTimeMs = Date.now();
    const result = await this.wasmEngine.query(sqlQuery);
    Analytics.TraceProcessor.logQueryExecutionTime(
      Date.now() - startTimeMs,
      false
    );
    return result;
  }

  async queryAllRows(sqlQuery: string): Promise<QueryResult> {
    const startTimeMs = Date.now();
    const result = await this.wasmEngine.query(sqlQuery).waitAllRows();
    Analytics.TraceProcessor.logQueryExecutionTime(
      Date.now() - startTimeMs,
      true
    );
    return result;
  }

  async resetTraceProcessor(config: TraceProcessorConfig) {
    await this.wasmEngine.resetTraceProcessor(config);
  }

  async parse(data: Uint8Array) {
    await this.wasmEngine.parse(data);
  }

  async notifyEof() {
    await this.wasmEngine.notifyEof();
  }
}
