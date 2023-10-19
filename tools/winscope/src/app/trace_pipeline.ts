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

import {FileUtils, OnFile} from 'common/file_utils';
import {TimestampType} from 'common/time';
import {ProgressListener} from 'interfaces/progress_listener';
import {UserNotificationListener} from 'interfaces/user_notification_listener';
import {ParserError, ParserErrorType, ParserFactory} from 'parsers/parser_factory';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {TracesParserFactory} from 'parsers/traces_parser_factory';
import {FrameMapper} from 'trace/frame_mapper';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TraceFileFilter} from './trace_file_filter';
import {TRACE_INFO} from './trace_info';

export class TracePipeline {
  private userNotificationListener?: UserNotificationListener;
  private traceFileFilter = new TraceFileFilter();
  private parserFactory = new ParserFactory();
  private tracesParserFactory = new TracesParserFactory();
  private parsers: Array<Parser<object>> = [];
  private loadedPerfettoTraceFile?: TraceFile;
  private loadedTraceFiles = new Map<TraceType, TraceFile>();
  private traces = new Traces();
  private commonTimestampType?: TimestampType;

  constructor(userNotificationListener?: UserNotificationListener) {
    this.userNotificationListener = userNotificationListener;
  }

  async loadFiles(files: File[], progressListener?: ProgressListener) {
    const [traceFiles, errors] = await this.unzipFiles(files, progressListener);

    const filterResult = await this.traceFileFilter.filter(traceFiles);
    if (!filterResult.perfetto && filterResult.legacy.length === 0) {
      progressListener?.onOperationFinished();
      errors.push(new ParserError(ParserErrorType.NO_INPUT_FILES));
      this.userNotificationListener?.onParserErrors(errors);
      return;
    }

    errors.push(...filterResult.errors);

    const [fileAndParsers, legacyErrors] = await this.parserFactory.createParsers(
      filterResult.legacy,
      progressListener
    );
    errors.push(...legacyErrors);
    for (const fileAndParser of fileAndParsers) {
      this.loadedTraceFiles.set(fileAndParser.parser.getTraceType(), fileAndParser.file);
    }

    const newParsers = fileAndParsers.map((it) => it.parser);
    this.parsers = this.parsers.concat(newParsers);

    if (filterResult.perfetto) {
      const perfettoParsers = await new PerfettoParserFactory().createParsers(
        filterResult.perfetto,
        progressListener
      );
      this.loadedPerfettoTraceFile = perfettoParsers.length > 0 ? filterResult.perfetto : undefined;
      this.parsers = this.parsers.concat(perfettoParsers);
    }

    const tracesParsers = await this.tracesParserFactory.createParsers(this.parsers);

    const allParsers = this.parsers.concat(tracesParsers);

    this.traces = new Traces();
    allParsers.forEach((parser) => {
      const trace = Trace.newUninitializedTrace(parser);
      this.traces?.setTrace(parser.getTraceType(), trace);
    });

    const hasTransitionTrace = this.traces
      .mapTrace((trace) => trace.type)
      .some((type) => type === TraceType.TRANSITION);
    if (hasTransitionTrace) {
      this.traces.deleteTrace(TraceType.WM_TRANSITION);
      this.traces.deleteTrace(TraceType.SHELL_TRANSITION);
    }

    progressListener?.onOperationFinished();

    if (errors.length > 0) {
      this.userNotificationListener?.onParserErrors(errors);
    }
  }

  removeTrace(trace: Trace<object>) {
    this.parsers = this.parsers.filter((parser) => parser.getTraceType() !== trace.type);
    this.traces.deleteTrace(trace.type);
    this.loadedTraceFiles.delete(trace.type);
    this.parserFactory.removeParser(trace.type);
  }

  async makeZipArchiveWithLoadedTraceFiles(): Promise<Blob> {
    const archiveFiles: File[] = [];

    if (this.loadedPerfettoTraceFile) {
      const archiveFilename = FileUtils.removeDirFromFileName(
        this.loadedPerfettoTraceFile.file.name
      );
      const archiveFile = new File([this.loadedPerfettoTraceFile.file], archiveFilename);
      archiveFiles.push(archiveFile);
    }

    this.loadedTraceFiles.forEach((traceFile, traceType) => {
      const archiveDir =
        TRACE_INFO[traceType].downloadArchiveDir.length > 0
          ? TRACE_INFO[traceType].downloadArchiveDir + '/'
          : '';
      const archiveFilename = archiveDir + FileUtils.removeDirFromFileName(traceFile.file.name);
      const archiveFile = new File([traceFile.file], archiveFilename);
      archiveFiles.push(archiveFile);
    });

    // Remove duplicates because some traces (e.g. view capture) could share the same file
    const uniqueArchiveFiles = archiveFiles.filter(
      (file, index, fileList) => fileList.indexOf(file) === index
    );

    return await FileUtils.createZipArchive(uniqueArchiveFiles);
  }

  async buildTraces() {
    const commonTimestampType = this.getCommonTimestampType();
    this.traces.forEachTrace((trace) => trace.init(commonTimestampType));
    await new FrameMapper(this.traces).computeMapping();
  }

  getTraces(): Traces {
    return this.traces;
  }

  async getScreenRecordingVideo(): Promise<undefined | Blob> {
    const screenRecording = this.getTraces().getTrace(TraceType.SCREEN_RECORDING);
    if (!screenRecording || screenRecording.lengthEntries === 0) {
      return undefined;
    }
    return (await screenRecording.getEntry(0).getValue()).videoData;
  }

  clear() {
    this.parserFactory = new ParserFactory();
    this.parsers = [];
    this.traces = new Traces();
    this.commonTimestampType = undefined;
    this.loadedPerfettoTraceFile = undefined;
    this.loadedTraceFiles = new Map<TraceType, TraceFile>();
  }

  private async unzipFiles(
    files: File[],
    progressListener?: ProgressListener
  ): Promise<[TraceFile[], ParserError[]]> {
    const traceFiles: TraceFile[] = [];
    const errors: ParserError[] = [];
    const progressMessage = 'Unzipping files...';

    const onProgressUpdate = (progressPercentage: number) => {
      progressListener?.onProgressUpdate(progressMessage, progressPercentage);
    };

    const onFile: OnFile = (file: File, parentArchive?: File) => {
      traceFiles.push(new TraceFile(file, parentArchive));
    };

    progressListener?.onProgressUpdate(progressMessage, 0);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      const onSubProgressUpdate = (subPercentage: number) => {
        const totalPercentage = (100 * i) / files.length + subPercentage / files.length;
        progressListener?.onProgressUpdate(progressMessage, totalPercentage);
      };

      if (FileUtils.isZipFile(file)) {
        try {
          const subFiles = await FileUtils.unzipFile(file, onSubProgressUpdate);
          const subTraceFiles = subFiles.map((subFile) => {
            return new TraceFile(subFile, file);
          });
          traceFiles.push(...subTraceFiles);
          onSubProgressUpdate(100);
        } catch (e) {
          errors.push(new ParserError(ParserErrorType.CORRUPTED_ARCHIVE, file.name));
        }
      } else {
        traceFiles.push(new TraceFile(file, undefined));
        onSubProgressUpdate(100);
      }
    }

    progressListener?.onProgressUpdate(progressMessage, 100);

    return [traceFiles, errors];
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
}
