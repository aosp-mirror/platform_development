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

import {FunctionUtils, OnProgressUpdateType} from 'common/function_utils';
import {ParserError, ParserFactory} from 'parsers/parser_factory';
import {TracesParserCujs} from 'parsers/traces_parser_cujs';
import {TracesParserTransitions} from 'parsers/traces_parser_transitions';
import {FrameMapper} from 'trace/frame_mapper';
import {LoadedTrace} from 'trace/loaded_trace';
import {Parser} from 'trace/parser';
import {TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';

class TracePipeline {
  private parserFactory = new ParserFactory();
  private parsers: Array<Parser<object>> = [];
  private files = new Map<TraceType, TraceFile>();
  private traces?: Traces;
  private commonTimestampType?: TimestampType;

  async loadTraceFiles(
    traceFiles: TraceFile[],
    onLoadProgressUpdate: OnProgressUpdateType = FunctionUtils.DO_NOTHING
  ): Promise<ParserError[]> {
    traceFiles = await this.filterBugreportFilesIfNeeded(traceFiles);
    const [parsers, parserErrors] = await this.parserFactory.createParsers(
      traceFiles,
      onLoadProgressUpdate
    );
    this.parsers = parsers.map((it) => it.parser);

    const tracesParsers = [
      new TracesParserTransitions(this.parsers),
      new TracesParserCujs(this.parsers),
    ];
    for (const tracesParser of tracesParsers) {
      if (tracesParser.canProvideEntries()) {
        this.parsers.push(tracesParser);
      }
    }

    for (const parser of parsers) {
      this.files.set(parser.parser.getTraceType(), parser.file);
    }

    if (this.parsers.some((it) => it.getTraceType() === TraceType.TRANSITION)) {
      this.parsers = this.parsers.filter(
        (it) =>
          it.getTraceType() !== TraceType.WM_TRANSITION &&
          it.getTraceType() !== TraceType.SHELL_TRANSITION
      );
    }

    return parserErrors;
  }

  removeTraceFile(type: TraceType) {
    this.parsers = this.parsers.filter((parser) => parser.getTraceType() !== type);
  }

  getLoadedFiles(): Map<TraceType, TraceFile> {
    return this.files;
  }

  getLoadedTraces(): LoadedTrace[] {
    return this.parsers.map(
      (parser: Parser<object>) => new LoadedTrace(parser.getDescriptors(), parser.getTraceType())
    );
  }

  buildTraces() {
    const commonTimestampType = this.getCommonTimestampType();

    this.traces = new Traces();
    this.parsers.forEach((parser) => {
      const trace = Trace.newUninitializedTrace(parser);
      trace.init(commonTimestampType);
      this.traces?.setTrace(parser.getTraceType(), trace);
    });

    new FrameMapper(this.traces).computeMapping();
  }

  getTraces(): Traces {
    this.checkTracesWereBuilt();
    return this.traces!;
  }

  getScreenRecordingVideo(): undefined | Blob {
    const screenRecording = this.getTraces().getTrace(TraceType.SCREEN_RECORDING);
    if (!screenRecording || screenRecording.lengthEntries === 0) {
      return undefined;
    }
    return screenRecording.getEntry(0).getValue().videoData;
  }

  clear() {
    this.parserFactory = new ParserFactory();
    this.parsers = [];
    this.traces = undefined;
    this.commonTimestampType = undefined;
    this.files = new Map<TraceType, TraceFile>();
  }

  private async filterBugreportFilesIfNeeded(files: TraceFile[]): Promise<TraceFile[]> {
    const bugreportMainEntry = files.find((file) => file.file.name === 'main_entry.txt');
    if (!bugreportMainEntry) {
      return files;
    }

    const bugreportName = (await bugreportMainEntry.file.text()).trim();
    const isBugreport = files.find((file) => file.file.name === bugreportName) !== undefined;
    if (!isBugreport) {
      return files;
    }

    const BUGREPORT_TRACE_DIRS = ['FS/data/misc/wmtrace/', 'FS/data/misc/perfetto-traces/'];
    const isFileWithinBugreportTraceDir = (file: TraceFile) => {
      for (const traceDir of BUGREPORT_TRACE_DIRS) {
        if (file.file.name.startsWith(traceDir)) {
          return true;
        }
      }
      return false;
    };

    const fileBelongsToBugreport = (file: TraceFile) =>
      file.parentArchive === bugreportMainEntry.parentArchive;

    return files.filter((file) => {
      return isFileWithinBugreportTraceDir(file) || !fileBelongsToBugreport(file);
    });
  }

  private getCommonTimestampType(): TimestampType {
    if (this.commonTimestampType !== undefined) {
      return this.commonTimestampType;
    }

    const priorityOrder = [TimestampType.REAL, TimestampType.ELAPSED];
    for (const type of priorityOrder) {
      if (this.parsers.every((it) => it.getTimestamps(type) !== undefined)) {
        this.commonTimestampType = type;
        return this.commonTimestampType;
      }
    }

    throw Error('Failed to find common timestamp type across all traces');
  }

  private checkTracesWereBuilt() {
    if (!this.traces) {
      throw new Error(
        `Can't access traces before building them. Did you forget to call '${this.buildTraces.name}'?`
      );
    }
  }
}

export {TracePipeline};
