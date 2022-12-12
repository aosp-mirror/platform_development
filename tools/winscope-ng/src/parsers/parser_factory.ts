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

import {TraceType} from "common/trace/trace_type";
import {FunctionUtils, OnProgressUpdateType} from "common/utils/function_utils";
import {Parser} from "./parser";
import {ParserAccessibility} from "./parser_accessibility";
import {ParserInputMethodClients} from "./parser_input_method_clients";
import {ParserInputMethodManagerService} from "./parser_input_method_manager_service";
import {ParserInputMethodService} from "./parser_input_method_service";
import {ParserProtoLog} from "./parser_protolog";
import {ParserScreenRecording} from "./parser_screen_recording";
import {ParserScreenRecordingLegacy} from "./parser_screen_recording_legacy";
import {ParserSurfaceFlinger} from "./parser_surface_flinger";
import {ParserTransactions} from "./parser_transactions";
import {ParserWindowManager} from "./parser_window_manager";
import {ParserWindowManagerDump} from "./parser_window_manager_dump";

class ParserFactory {
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
  ];

  private parsers = new Map<TraceType, Parser>();

  async createParsers(
    traces: File[],
    onProgressUpdate: OnProgressUpdateType = FunctionUtils.DO_NOTHING):
    Promise<[Parser[], ParserError[]]> {
    const errors: ParserError[] = [];

    for (const [index, trace] of traces.entries()) {
      let hasFoundParser = false;

      for (const ParserType of ParserFactory.PARSERS) {
        try {
          const parser = new ParserType(trace);
          await parser.parse();
          hasFoundParser = true;
          if (this.shouldUseParser(parser, errors)) {
            this.parsers.set(parser.getTraceType(), parser);
          }
          break;
        }
        catch(error) {
          // skip current parser
        }
      }

      if (!hasFoundParser) {
        console.log(`Failed to load trace ${trace.name}`);
        errors.push(new ParserError(trace, ParserErrorType.UNSUPPORTED_FORMAT));
      }

      onProgressUpdate(100 * (index + 1) / traces.length);
    }

    return [Array.from(this.parsers.values()), errors];
  }

  private shouldUseParser(newParser: Parser, errors: ParserError[]): boolean {
    const oldParser = this.parsers.get(newParser.getTraceType());
    if (!oldParser) {
      console.log(`Loaded trace ${newParser.getTrace().file.name} (trace type: ${newParser.getTraceType()})`);
      return true;
    }

    if (newParser.getEntriesLength() > oldParser.getEntriesLength()) {
      console.log(
        `Loaded trace ${newParser.getTrace().file.name} (trace type: ${newParser.getTraceType()}).` +
        ` Replace trace ${oldParser.getTrace().file.name}`
      );
      errors.push(
        new ParserError(
          oldParser.getTrace().file, ParserErrorType.OVERRIDE, oldParser.getTraceType()
        )
      );
      return true;
    }

    console.log(
      `Skipping trace ${newParser.getTrace().file.name} (trace type: ${newParser.getTraceType()}).` +
      ` Keep trace ${oldParser.getTrace().file.name}`
    );
    errors.push(
      new ParserError(
        newParser.getTrace().file, ParserErrorType.OVERRIDE, newParser.getTraceType()
      )
    );
    return false;
  }
}

export enum ParserErrorType {
  UNSUPPORTED_FORMAT,
  OVERRIDE
}

export class ParserError {
  constructor(
    public trace: File,
    public type: ParserErrorType,
    public traceType: TraceType|undefined = undefined) {
  }
}

export {ParserFactory};
