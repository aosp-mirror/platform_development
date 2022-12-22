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
import {TraceFile} from "common/trace/trace";
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
  ];

  private parsers = new Map<TraceType, Parser>();

  async createParsers(
    traceFiles: TraceFile[],
    onProgressUpdate: OnProgressUpdateType = FunctionUtils.DO_NOTHING):
    Promise<[Parser[], ParserError[]]> {
    const errors: ParserError[] = [];

    if (traceFiles.length === 0) {
      errors.push(new ParserError(ParserErrorType.NO_INPUT_FILES));
    }

    for (const [index, traceFile] of traceFiles.entries()) {
      let hasFoundParser = false;

      for (const ParserType of ParserFactory.PARSERS) {
        try {
          const parser = new ParserType(traceFile);
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
        console.log(`Failed to load trace ${traceFile.file.name}`);
        errors.push(new ParserError(ParserErrorType.UNSUPPORTED_FORMAT, traceFile.file));
      }

      onProgressUpdate(100 * (index + 1) / traceFiles.length);
    }

    return [Array.from(this.parsers.values()), errors];
  }

  private shouldUseParser(newParser: Parser, errors: ParserError[]): boolean {
    const oldParser = this.parsers.get(newParser.getTraceType());
    if (!oldParser) {
      console.log(`Loaded trace ${newParser.getTrace().traceFile.file.name} (trace type: ${newParser.getTraceType()})`);
      return true;
    }

    if (newParser.getEntriesLength() > oldParser.getEntriesLength()) {
      console.log(
        `Loaded trace ${newParser.getTrace().traceFile.file.name} (trace type: ${newParser.getTraceType()}).` +
        ` Replace trace ${oldParser.getTrace().traceFile.file.name}`
      );
      errors.push(
        new ParserError(
          ParserErrorType.OVERRIDE, oldParser.getTrace().traceFile.file, oldParser.getTraceType()
        )
      );
      return true;
    }

    console.log(
      `Skipping trace ${newParser.getTrace().traceFile.file.name} (trace type: ${newParser.getTraceType()}).` +
      ` Keep trace ${oldParser.getTrace().traceFile.file.name}`
    );
    errors.push(
      new ParserError(
        ParserErrorType.OVERRIDE, newParser.getTrace().traceFile.file, newParser.getTraceType()
      )
    );
    return false;
  }
}

export enum ParserErrorType {
  NO_INPUT_FILES,
  UNSUPPORTED_FORMAT,
  OVERRIDE
}

export class ParserError {
  constructor(
    public type: ParserErrorType,
    public trace: File|undefined = undefined,
    public traceType: TraceType|undefined = undefined) {
  }
}
