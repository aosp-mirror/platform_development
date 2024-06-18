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

import {FileUtils} from 'common/file_utils';
import {
  NO_TIMEZONE_OFFSET_FACTORY,
  TimestampFactory,
} from 'common/timestamp_factory';
import {ProgressListener} from 'messaging/progress_listener';
import {
  CorruptedArchive,
  NoCommonTimestampType,
  NoInputFiles,
} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {ParserFactory} from 'parsers/parser_factory';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {TracesParserFactory} from 'parsers/traces_parser_factory';
import {FrameMapper} from 'trace/frame_mapper';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {FilesSource} from './files_source';
import {LoadedParsers} from './loaded_parsers';
import {TraceFileFilter} from './trace_file_filter';

type UnzippedArchive = TraceFile[];

export class TracePipeline {
  private loadedParsers = new LoadedParsers();
  private traceFileFilter = new TraceFileFilter();
  private tracesParserFactory = new TracesParserFactory();
  private traces = new Traces();
  private downloadArchiveFilename?: string;
  private timestampFactory = NO_TIMEZONE_OFFSET_FACTORY;

  async loadFiles(
    files: File[],
    source: FilesSource,
    errorListener: WinscopeErrorListener,
    progressListener: ProgressListener | undefined,
  ) {
    this.downloadArchiveFilename = this.makeDownloadArchiveFilename(
      files,
      source,
    );

    try {
      const unzippedArchives = await this.unzipFiles(
        files,
        progressListener,
        errorListener,
      );

      if (unzippedArchives.length === 0) {
        errorListener.onError(new NoInputFiles());
        return;
      }

      for (const unzippedArchive of unzippedArchives) {
        await this.loadUnzippedArchive(
          unzippedArchive,
          errorListener,
          progressListener,
        );
      }

      this.traces = new Traces();

      const commonTimestampType = this.loadedParsers.findCommonTimestampType();
      if (commonTimestampType === undefined) {
        errorListener.onError(new NoCommonTimestampType());
        return;
      }

      this.loadedParsers.getParsers().forEach((parser) => {
        const trace = Trace.fromParser(parser, commonTimestampType);
        this.traces.setTrace(parser.getTraceType(), trace);
      });

      const tracesParsers = await this.tracesParserFactory.createParsers(
        this.traces,
      );

      tracesParsers.forEach((tracesParser) => {
        const trace = Trace.fromParser(tracesParser, commonTimestampType);
        this.traces.setTrace(trace.type, trace);
      });

      const hasTransitionTrace = this.traces
        .mapTrace((trace) => trace.type)
        .some((type) => type === TraceType.TRANSITION);
      if (hasTransitionTrace) {
        this.traces.deleteTrace(TraceType.WM_TRANSITION);
        this.traces.deleteTrace(TraceType.SHELL_TRANSITION);
      }
    } finally {
      progressListener?.onOperationFinished();
    }
  }

  removeTrace(trace: Trace<object>) {
    this.loadedParsers.remove(trace.type);
    this.traces.deleteTrace(trace.type);
  }

  async makeZipArchiveWithLoadedTraceFiles(): Promise<Blob> {
    return this.loadedParsers.makeZipArchive();
  }

  async buildTraces() {
    await new FrameMapper(this.traces).computeMapping();
  }

  getTraces(): Traces {
    return this.traces;
  }

  getDownloadArchiveFilename(): string {
    return this.downloadArchiveFilename ?? 'winscope';
  }

  getTimestampFactory(): TimestampFactory {
    return this.timestampFactory;
  }

  async getScreenRecordingVideo(): Promise<undefined | Blob> {
    const traces = this.getTraces();
    const screenRecording =
      traces.getTrace(TraceType.SCREEN_RECORDING) ??
      traces.getTrace(TraceType.SCREENSHOT);
    if (!screenRecording || screenRecording.lengthEntries === 0) {
      return undefined;
    }
    return (await screenRecording.getEntry(0).getValue()).videoData;
  }

  clear() {
    this.loadedParsers.clear();
    this.traces = new Traces();
    this.timestampFactory = NO_TIMEZONE_OFFSET_FACTORY;
    this.downloadArchiveFilename = undefined;
  }

  private async loadUnzippedArchive(
    unzippedArchive: UnzippedArchive,
    errorListener: WinscopeErrorListener,
    progressListener: ProgressListener | undefined,
  ) {
    const filterResult = await this.traceFileFilter.filter(
      unzippedArchive,
      errorListener,
    );
    if (filterResult.timezoneInfo) {
      this.timestampFactory = new TimestampFactory(filterResult.timezoneInfo);
    }

    if (!filterResult.perfetto && filterResult.legacy.length === 0) {
      errorListener.onError(new NoInputFiles());
      return;
    }

    const legacyParsers = await new ParserFactory().createParsers(
      filterResult.legacy,
      this.timestampFactory,
      progressListener,
      errorListener,
    );

    let perfettoParsers: FileAndParsers | undefined;

    if (filterResult.perfetto) {
      const parsers = await new PerfettoParserFactory().createParsers(
        filterResult.perfetto,
        this.timestampFactory,
        progressListener,
        errorListener,
      );
      perfettoParsers = new FileAndParsers(filterResult.perfetto, parsers);
    }

    this.loadedParsers.addParsers(
      legacyParsers,
      perfettoParsers,
      errorListener,
    );
  }

  private makeDownloadArchiveFilename(
    files: File[],
    source: FilesSource,
  ): string {
    // set download archive file name, used to download all traces
    let filenameWithCurrTime: string;
    const currTime = new Date().toISOString().slice(0, -5).replace('T', '_');
    if (!this.downloadArchiveFilename && files.length === 1) {
      const filenameNoDir = FileUtils.removeDirFromFileName(files[0].name);
      const filenameNoDirOrExt =
        FileUtils.removeExtensionFromFilename(filenameNoDir);
      filenameWithCurrTime = `${filenameNoDirOrExt}_${currTime}`;
    } else {
      filenameWithCurrTime = `${source}_${currTime}`;
    }

    const archiveFilenameNoIllegalChars = filenameWithCurrTime.replace(
      FileUtils.ILLEGAL_FILENAME_CHARACTERS_REGEX,
      '_',
    );
    if (FileUtils.DOWNLOAD_FILENAME_REGEX.test(archiveFilenameNoIllegalChars)) {
      return archiveFilenameNoIllegalChars;
    } else {
      console.error(
        "Cannot convert uploaded archive filename to acceptable format for download. Defaulting download filename to 'winscope.zip'.",
      );
      return 'winscope';
    }
  }

  private async unzipFiles(
    files: File[],
    progressListener: ProgressListener | undefined,
    errorListener: WinscopeErrorListener,
  ): Promise<UnzippedArchive[]> {
    const unzippedArchives: UnzippedArchive[] = [];
    const progressMessage = 'Unzipping files...';

    progressListener?.onProgressUpdate(progressMessage, 0);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      const onSubProgressUpdate = (subPercentage: number) => {
        const totalPercentage =
          (100 * i) / files.length + subPercentage / files.length;
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
          errorListener.onError(new CorruptedArchive(file));
        }
      } else {
        unzippedArchives.push([new TraceFile(file, undefined)]);
        onSubProgressUpdate(100);
      }
    }

    progressListener?.onProgressUpdate(progressMessage, 100);

    return unzippedArchives;
  }
}
