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

import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {UserNotifier} from 'common/user_notifier';
import {ProgressListener} from 'messaging/progress_listener';
import {
  InvalidPerfettoTrace,
  PerfettoPacketLoss,
} from 'messaging/user_warnings';
import {ParserKeyEvent} from 'parsers/input/perfetto/parser_key_event';
import {ParserMotionEvent} from 'parsers/input/perfetto/parser_motion_event';
import {ParserInputMethodClients} from 'parsers/input_method/perfetto/parser_input_method_clients';
import {ParserInputMethodManagerService} from 'parsers/input_method/perfetto/parser_input_method_manager_service';
import {ParserInputMethodService} from 'parsers/input_method/perfetto/parser_input_method_service';
import {ParserProtolog} from 'parsers/protolog/perfetto/parser_protolog';
import {ParserSurfaceFlinger} from 'parsers/surface_flinger/perfetto/parser_surface_flinger';
import {ParserTransactions} from 'parsers/transactions/perfetto/parser_transactions';
import {ParserTransitions} from 'parsers/transitions/perfetto/parser_transitions';
import {ParserViewCapture} from 'parsers/view_capture/perfetto/parser_view_capture';
import {ParserWindowManager} from 'parsers/window_manager/perfetto/parser_window_manager';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {Row} from 'trace_processor/query_result';
import {TraceProcessor} from 'trace_processor/trace_processor';
import {TraceProcessorFactory} from 'trace_processor/trace_processor_factory';

export class ParserFactory {
  private static readonly PARSERS = [
    ParserInputMethodClients,
    ParserInputMethodManagerService,
    ParserInputMethodService,
    ParserProtolog,
    ParserSurfaceFlinger,
    ParserTransactions,
    ParserTransitions,
    ParserViewCapture,
    ParserWindowManager,
    ParserMotionEvent,
    ParserKeyEvent,
  ];
  private static readonly CHUNK_SIZE_BYTES = 50 * 1024 * 1024;
  private static readonly NO_ENTRIES_ERROR_REGEX =
    /Perfetto trace has no \w+(\w|\s)* entries/;

  async createParsers(
    traceFile: TraceFile,
    timestampConverter: ParserTimestampConverter,
    progressListener?: ProgressListener,
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
          timestampConverter,
        );
        await parser.parse();
        if (parser instanceof ParserViewCapture) {
          parsers.push(...parser.getWindowParsers());
        } else {
          parsers.push(parser);
        }
        hasFoundParser = true;
      } catch (error) {
        // skip current parser
        const msg = (error as Error).message;
        if (!ParserFactory.NO_ENTRIES_ERROR_REGEX.test(msg)) {
          // If TP contains no entries for a particular trace type, the resulting
          // error message matches ParserFactory.NO_ENTRIES_ERROR_REGEX. These
          // messages are discarded, and if no parser is found, one representative
          // message is reported to the user below.
          errors.push(msg);
        }
      }
    }

    if (!hasFoundParser) {
      if (errors.length === 0) {
        errors.push('Perfetto trace has no Winscope trace entries');
      }
      UserNotifier.add(
        new InvalidPerfettoTrace(traceFile.getDescriptor(), errors),
      );
    }
    const result = await traceProcessor.queryAllRows(
      "select name, value from stats where name = 'traced_buf_trace_writer_packet_loss'",
    );
    if (result.numRows() > 0) {
      const value = result.firstRow<Row>({})['value'];
      if (typeof value === 'bigint' && value > 0n) {
        UserNotifier.add(
          new PerfettoPacketLoss(traceFile.getDescriptor(), Number(value)),
        );
      }
    }

    return parsers;
  }

  private async initializeTraceProcessor(): Promise<TraceProcessor> {
    const traceProcessor = await TraceProcessorFactory.getSingleInstance();

    await traceProcessor.resetTraceProcessor({
      cropTrackEvents: false,
      ingestFtraceInRawTable: false,
      analyzeTraceProtoContent: false,
    });

    return traceProcessor;
  }
}
