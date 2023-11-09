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

import {assertDefined} from 'common/assert_utils';
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
import {FilesSource} from './files_source';
import {TraceFileFilter} from './trace_file_filter';
import {TRACE_INFO} from './trace_info';

type TraceFilesFromArchive = TraceFile[];

export class TracePipeline {
  private userNotificationListener?: UserNotificationListener;
  private traceFileFilter = new TraceFileFilter();
  private parserFactory = new ParserFactory();
  private tracesParserFactory = new TracesParserFactory();
  private parsers = new Map<TraceType, Parser<object>>();
  private loadedPerfettoTraceFile?: TraceFile;
  private loadedTraceFiles = new Map<TraceType, TraceFile>();
  private traces = new Traces();
  private commonTimestampType?: TimestampType;
  private downloadArchiveFilename?: string;

  constructor(userNotificationListener?: UserNotificationListener) {
    this.userNotificationListener = userNotificationListener;
  }

  async loadFiles(
    files: File[],
    progressListener?: ProgressListener,
    source: FilesSource = FilesSource.UNKNOWN
  ) {
    this.downloadArchiveFilename = this.makeDownloadArchiveFilename(files, source);

    const [unzippedArchives, errors] = await this.unzipFiles(files, progressListener);

    const currParsers = new Map<TraceType, Parser<object>>();

    const maybeAddFileAndParser = (p: Parser<object>, f: TraceFile) => {
      if (this.shouldUseParser(p, errors, currParsers)) {
        const traceType = p.getTraceType();
        currParsers.set(traceType, p);
        this.parsers.set(traceType, p);
        this.loadedTraceFiles.set(traceType, f);
      }
    };

    if (unzippedArchives.length === 0) {
      progressListener?.onOperationFinished();
      errors.push(new ParserError(ParserErrorType.NO_INPUT_FILES));
      this.userNotificationListener?.onParserErrors(errors);
      return;
    }

    for (const traceFilesFromArchive of unzippedArchives) {
      const filterResult = await this.traceFileFilter.filter(traceFilesFromArchive);
      if (!filterResult.perfetto && filterResult.legacy.length === 0) {
        errors.push(new ParserError(ParserErrorType.NO_INPUT_FILES));
      } else {
        errors.push(...filterResult.errors);

        const [fileAndParsers, legacyErrors] = await this.parserFactory.createParsers(
          filterResult.legacy,
          progressListener
        );
        errors.push(...legacyErrors);

        for (const fileAndParser of fileAndParsers) {
          maybeAddFileAndParser(fileAndParser.parser, fileAndParser.file);
        }

        if (filterResult.perfetto) {
          const perfettoParsers = await new PerfettoParserFactory().createParsers(
            filterResult.perfetto,
            progressListener
          );
          this.loadedPerfettoTraceFile =
            perfettoParsers.length > 0 ? filterResult.perfetto : undefined;
          perfettoParsers.forEach((parser) => {
            this.parsers.set(parser.getTraceType(), parser);
          });
        }
      }
    }

    if (currParsers.size === 0) {
      this.userNotificationListener?.onParserErrors(errors);
      return;
    }

    const tracesParsers = await this.tracesParserFactory.createParsers(this.parsers);

    this.traces = new Traces();

    this.parsers.forEach((parser, traceType) => {
      const trace = Trace.newUninitializedTrace(parser);
      this.traces.setTrace(traceType, trace);
    });

    tracesParsers.forEach((tracesParser, traceType) => {
      const trace = Trace.newUninitializedTrace(tracesParser);
      this.traces.setTrace(traceType, trace);
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
    this.parsers.delete(trace.type);
    this.traces.deleteTrace(trace.type);
    this.loadedTraceFiles.delete(trace.type);
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

  getDownloadArchiveFilename(): string {
    return this.downloadArchiveFilename ?? 'winscope';
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
    this.parsers = new Map<TraceType, Parser<object>>();
    this.traces = new Traces();
    this.commonTimestampType = undefined;
    this.loadedPerfettoTraceFile = undefined;
    this.loadedTraceFiles = new Map<TraceType, TraceFile>();
    this.downloadArchiveFilename = undefined;
  }

  private makeDownloadArchiveFilename(files: File[], source: FilesSource): string {
    // set download archive file name, used to download all traces
    let filenameWithCurrTime: string;
    const currTime = new Date().toISOString().slice(0, -5).replace('T', '_');
    if (!this.downloadArchiveFilename && files.length === 1) {
      const filenameNoDir = FileUtils.removeDirFromFileName(files[0].name);
      const filenameNoDirOrExt = FileUtils.removeExtensionFromFilename(filenameNoDir);
      filenameWithCurrTime = `${filenameNoDirOrExt}_${currTime}`;
    } else {
      filenameWithCurrTime = `${source}_${currTime}`;
    }

    const archiveFilenameNoIllegalChars = filenameWithCurrTime.replace(
      FileUtils.ILLEGAL_FILENAME_CHARACTERS_REGEX,
      '_'
    );
    if (FileUtils.DOWNLOAD_FILENAME_REGEX.test(archiveFilenameNoIllegalChars)) {
      return archiveFilenameNoIllegalChars;
    } else {
      console.error(
        "Cannot convert uploaded archive filename to acceptable format for download. Defaulting download filename to 'winscope.zip'."
      );
      return 'winscope';
    }
  }

  private async unzipFiles(
    files: File[],
    progressListener?: ProgressListener
  ): Promise<[TraceFilesFromArchive[], ParserError[]]> {
    const unzippedArchives: TraceFilesFromArchive[] = [];
    const errors: ParserError[] = [];
    const progressMessage = 'Unzipping files...';

    const onProgressUpdate = (progressPercentage: number) => {
      progressListener?.onProgressUpdate(progressMessage, progressPercentage);
    };

    const onFile: OnFile = (file: File, parentArchive?: File) => {
      unzippedArchives.push([new TraceFile(file, parentArchive)]);
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
          unzippedArchives.push([...subTraceFiles]);
          onSubProgressUpdate(100);
        } catch (e) {
          errors.push(new ParserError(ParserErrorType.CORRUPTED_ARCHIVE, file.name));
        }
      } else {
        unzippedArchives.push([new TraceFile(file, undefined)]);
        onSubProgressUpdate(100);
      }
    }

    progressListener?.onProgressUpdate(progressMessage, 100);

    return [unzippedArchives, errors];
  }

  private getCommonTimestampType(): TimestampType {
    if (this.commonTimestampType !== undefined) {
      return this.commonTimestampType;
    }

    const priorityOrder = [TimestampType.REAL, TimestampType.ELAPSED];
    for (const type of priorityOrder) {
      const parsers = Array.from(this.parsers.values());
      if (parsers.every((it) => it.getTimestamps(type) !== undefined)) {
        this.commonTimestampType = type;
        return this.commonTimestampType;
      }
    }

    throw Error('Failed to find common timestamp type across all traces');
  }

  private shouldUseParser(
    newParser: Parser<object>,
    errors: ParserError[],
    currParsers: Map<TraceType, Parser<object>>
  ): boolean {
    const oldParser = this.parsers.get(newParser.getTraceType());
    const currParser = currParsers.get(newParser.getTraceType());
    if (!oldParser && !currParser) {
      console.log(
        `Loaded trace ${newParser
          .getDescriptors()
          .join()} (trace type: ${newParser.getTraceType()})`
      );
      return true;
    }

    if (oldParser && !currParser) {
      console.log(
        `Loaded trace ${newParser
          .getDescriptors()
          .join()} (trace type: ${newParser.getTraceType()}).` +
          ` Replace trace ${oldParser.getDescriptors().join()}`
      );
      errors.push(
        new ParserError(
          ParserErrorType.OVERRIDE,
          oldParser.getDescriptors().join(),
          oldParser.getTraceType()
        )
      );
      return true;
    }

    if (currParser && newParser.getLengthEntries() > currParser.getLengthEntries()) {
      console.log(
        `Loaded trace ${newParser
          .getDescriptors()
          .join()} (trace type: ${newParser.getTraceType()}).` +
          ` Replace trace ${currParser.getDescriptors().join()}`
      );
      errors.push(
        new ParserError(
          ParserErrorType.OVERRIDE,
          currParser.getDescriptors().join(),
          currParser.getTraceType()
        )
      );
      return true;
    }

    console.log(
      `Skipping trace ${newParser
        .getDescriptors()
        .join()} (trace type: ${newParser.getTraceType()}).` +
        ` Keep trace ${assertDefined(currParser).getDescriptors().join()}`
    );
    errors.push(
      new ParserError(
        ParserErrorType.OVERRIDE,
        newParser.getDescriptors().join(),
        newParser.getTraceType()
      )
    );
    return false;
  }
}
