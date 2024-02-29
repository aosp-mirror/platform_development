/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {TimestampFactory} from 'common/timestamp_factory';
import {UrlUtils} from 'common/url_utils';
import {ProgressListener} from 'messaging/progress_listener';
import {InvalidPerfettoTrace} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {
  initWasm,
  resetEngineWorker,
  WasmEngineProxy,
} from 'trace_processor/wasm_engine_proxy';
import {ParserProtolog} from './parser_protolog';
import {ParserSurfaceFlinger} from './parser_surface_flinger';
import {ParserTransactions} from './parser_transactions';
import {ParserTransitions} from './parser_transitions';

export class ParserFactory {
  private static readonly PARSERS = [
    ParserSurfaceFlinger,
    ParserTransactions,
    ParserTransitions,
    ParserProtolog,
  ];
  private static readonly CHUNK_SIZE_BYTES = 50 * 1024 * 1024;
  private static traceProcessor?: WasmEngineProxy;

  async createParsers(
    traceFile: TraceFile,
    timestampFactory: TimestampFactory,
    progressListener?: ProgressListener,
    errorListener?: WinscopeErrorListener,
  ): Promise<Array<Parser<object>>> {
    const traceProcessor = await this.initializeTraceProcessor();
    for (
      let chunkStart = 0;
      chunkStart < traceFile.file.size;
      chunkStart += ParserFactory.CHUNK_SIZE_BYTES
    ) {
      progressListener?.onProgressUpdate(
        'Loading perfetto trace...',
        (chunkStart / traceFile.file.size) * 100,
      );
      const chunkEnd = chunkStart + ParserFactory.CHUNK_SIZE_BYTES;
      const data = await traceFile.file
        .slice(chunkStart, chunkEnd)
        .arrayBuffer();
      try {
        await traceProcessor.parse(new Uint8Array(data));
      } catch (e) {
        console.error('Trace processor failed to parse data:', e);
        return [];
      }
    }
    await traceProcessor.notifyEof();

    progressListener?.onProgressUpdate(
      'Reading from trace processor...',
      undefined,
    );
    const parsers: Array<Parser<object>> = [];

    let hasFoundParser = false;

    const errors: string[] = [];
    for (const ParserType of ParserFactory.PARSERS) {
      try {
        const parser = new ParserType(
          traceFile,
          traceProcessor,
          timestampFactory,
        );
        await parser.parse();
        parsers.push(parser);
        hasFoundParser = true;
      } catch (error) {
        // skip current parser
        errors.push((error as Error).message);
      }
    }

    if (!hasFoundParser) {
      errorListener?.onError(
        new InvalidPerfettoTrace(traceFile.getDescriptor(), errors),
      );
    }

    return parsers;
  }

  private async initializeTraceProcessor(): Promise<WasmEngineProxy> {
    if (!ParserFactory.traceProcessor) {
      const traceProcessorRootUrl =
        globalConfig.MODE === 'KARMA_TEST'
          ? UrlUtils.getRootUrl() +
            'base/deps_build/trace_processor/to_be_served/'
          : UrlUtils.getRootUrl();
      initWasm(traceProcessorRootUrl);
      const engineId = 'random-id';
      const enginePort = resetEngineWorker();
      ParserFactory.traceProcessor = new WasmEngineProxy(engineId, enginePort);
    }

    await ParserFactory.traceProcessor.resetTraceProcessor({
      cropTrackEvents: false,
      ingestFtraceInRawTable: false,
      analyzeTraceProtoContent: false,
    });

    return ParserFactory.traceProcessor;
  }
}
