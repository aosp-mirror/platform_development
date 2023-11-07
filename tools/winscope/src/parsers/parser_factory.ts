/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {ProgressListener} from 'interfaces/progress_listener';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParserAccessibility} from './parser_accessibility';
import {ParserEventLog} from './parser_eventlog';
import {ParserInputMethodClients} from './parser_input_method_clients';
import {ParserInputMethodManagerService} from './parser_input_method_manager_service';
import {ParserInputMethodService} from './parser_input_method_service';
import {ParserProtoLog} from './parser_protolog';
import {ParserScreenRecording} from './parser_screen_recording';
import {ParserScreenRecordingLegacy} from './parser_screen_recording_legacy';
import {ParserSurfaceFlinger} from './parser_surface_flinger';
import {ParserTransactions} from './parser_transactions';
import {ParserTransitionsShell} from './parser_transitions_shell';
import {ParserTransitionsWm} from './parser_transitions_wm';
import {ParserViewCapture} from './parser_view_capture';
import {ParserWindowManager} from './parser_window_manager';
import {ParserWindowManagerDump} from './parser_window_manager_dump';

export class ParserFactory {
  static readonly PARSERS = [
    ParserAccessibility,
    ParserInputMethodClients,
    ParserInputMethodManagerService,
    ParserInputMethodService,
    ParserProtoLog,
    ParserScreenRecording,
    ParserScreenRecordingLegacy,
    ParserSurfaceFlinger,
    ParserTransactions,
    ParserWindowManager,
    ParserWindowManagerDump,
    ParserEventLog,
    ParserTransitionsWm,
    ParserTransitionsShell,
    ParserViewCapture,
  ];

  async createParsers(
    traceFiles: TraceFile[],
    progressListener?: ProgressListener
  ): Promise<[Array<{file: TraceFile; parser: Parser<object>}>, ParserError[]]> {
    const errors: ParserError[] = [];
    const parsers = new Array<{file: TraceFile; parser: Parser<object>}>();

    for (const [index, traceFile] of traceFiles.entries()) {
      progressListener?.onProgressUpdate('Parsing proto files', (index / traceFiles.length) * 100);

      let hasFoundParser = false;

      for (const ParserType of ParserFactory.PARSERS) {
        try {
          const p = new ParserType(traceFile);
          await p.parse();
          hasFoundParser = true;
          if (p instanceof ParserViewCapture) {
            p.getWindowParsers().forEach((it) => parsers.push({file: traceFile, parser: it}));
          } else {
            parsers.push({file: traceFile, parser: p});
          }
          break;
        } catch (error) {
          // skip current parser
        }
      }

      if (!hasFoundParser) {
        console.error(`Failed to find parser for trace ${traceFile.file.name}`);
        errors.push(new ParserError(ParserErrorType.UNSUPPORTED_FORMAT, traceFile.getDescriptor()));
      }
    }
    return [parsers, errors];
  }
}

export enum ParserErrorType {
  CORRUPTED_ARCHIVE,
  NO_INPUT_FILES,
  UNSUPPORTED_FORMAT,
  OVERRIDE,
}

export class ParserError {
  constructor(
    public type: ParserErrorType,
    public trace: string | undefined = undefined,
    public traceType: TraceType | undefined = undefined
  ) {}
}
