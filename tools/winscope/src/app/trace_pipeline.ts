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
import {OnProgressUpdateType} from 'common/function_utils';
import {
  TimestampConverter,
  UTC_TIMEZONE_INFO,
} from 'common/timestamp_converter';
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {ProgressListener} from 'messaging/progress_listener';
import {CorruptedArchive, NoValidFiles} from 'messaging/user_warnings';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {ParserFactory as LegacyParserFactory} from 'parsers/legacy/parser_factory';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {ParserSearch} from 'parsers/search/parser_search';
import {TracesParserFactory} from 'parsers/traces/traces_parser_factory';
import {FrameMapper} from 'trace/frame_mapper';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceEntryTypeMap, TraceType, TraceTypeUtils} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
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
  private timestampConverter = new TimestampConverter(UTC_TIMEZONE_INFO);

  async loadFiles(
    files: File[],
    source: FilesSource,
    progressListener: ProgressListener | undefined,
  ) {
    this.downloadArchiveFilename = this.makeDownloadArchiveFilename(
      files,
      source,
    );

    try {
      const unzippedArchives = await this.unzipFiles(files, progressListener);

      if (unzippedArchives.length === 0) {
        UserNotifier.add(new NoValidFiles());
        return;
      }

      for (const unzippedArchive of unzippedArchives) {
        await this.loadUnzippedArchive(unzippedArchive, progressListener);
      }

      this.traces = new Traces();

      this.loadedParsers.getParsers().forEach((parser) => {
        const trace = Trace.fromParser(parser);
        this.traces.addTrace(trace);
        Analytics.Tracing.logTraceLoaded(parser);
      });

      const tracesParsers = await this.tracesParserFactory.createParsers(
        this.traces,
        this.timestampConverter,
      );

      tracesParsers.forEach((tracesParser) => {
        const trace = Trace.fromParser(tracesParser);
        this.traces.addTrace(trace);
      });

      const hasTransitionTrace =
        this.traces.getTrace(TraceType.TRANSITION) !== undefined;
      if (hasTransitionTrace) {
        this.removeTracesAndParsersByType(TraceType.WM_TRANSITION);
        this.removeTracesAndParsersByType(TraceType.SHELL_TRANSITION);
      }

      const hasCujTrace = this.traces.getTrace(TraceType.CUJS) !== undefined;
      if (hasCujTrace) {
        this.removeTracesAndParsersByType(TraceType.EVENT_LOG);
      }

      const hasMergedInputTrace =
        this.traces.getTrace(TraceType.INPUT_EVENT_MERGED) !== undefined;
      if (hasMergedInputTrace) {
        this.removeTracesAndParsersByType(TraceType.INPUT_KEY_EVENT);
        this.removeTracesAndParsersByType(TraceType.INPUT_MOTION_EVENT);
      }
    } finally {
      progressListener?.onOperationFinished(true);
    }
  }

  removeTrace<T extends TraceType>(
    trace: Trace<TraceEntryTypeMap[T]>,
    keepFileForDownload = false,
  ) {
    this.loadedParsers.remove(trace.getParser(), keepFileForDownload);
    this.traces.deleteTrace(trace);
  }

  async makeZipArchiveWithLoadedTraceFiles(
    onProgressUpdate?: OnProgressUpdateType,
  ): Promise<Blob> {
    return this.loadedParsers.makeZipArchive(onProgressUpdate);
  }

  filterTracesWithoutVisualization() {
    const tracesWithoutVisualization = this.traces
      .mapTrace((trace) => {
        if (!TraceTypeUtils.isTraceTypeWithViewer(trace.type)) {
          return trace;
        }
        return undefined;
      })
      .filter((trace) => trace !== undefined) as Array<Trace<object>>;
    tracesWithoutVisualization.forEach((trace) =>
      this.traces.deleteTrace(trace),
    );
  }

  async buildTraces() {
    for (const trace of this.traces) {
      if (trace.lengthEntries === 0 || trace.isDumpWithoutTimestamp()) {
        continue;
      } else {
        const timestamp = trace.getEntry(0).getTimestamp();
        this.timestampConverter.initializeUTCOffset(timestamp);
        break;
      }
    }
    await new FrameMapper(this.traces).computeMapping();
  }

  getTraces(): Traces {
    return this.traces;
  }

  getDownloadArchiveFilename(): string {
    return this.downloadArchiveFilename ?? 'winscope';
  }

  getTimestampConverter(): TimestampConverter {
    return this.timestampConverter;
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

  async tryCreateSearchTrace(
    query: string,
  ): Promise<Trace<QueryResult> | undefined> {
    try {
      const parser = new ParserSearch(query, this.timestampConverter);
      await parser.parse();
      const trace = Trace.fromParser(parser);
      this.traces.addTrace(trace);
      return trace;
    } catch (e) {
      return undefined;
    }
  }

  clear() {
    this.loadedParsers.clear();
    this.traces = new Traces();
    this.timestampConverter.clear();
    this.downloadArchiveFilename = undefined;
  }

  private async loadUnzippedArchive(
    unzippedArchive: UnzippedArchive,
    progressListener: ProgressListener | undefined,
  ) {
    const filterResult = await this.traceFileFilter.filter(unzippedArchive);
    if (filterResult.timezoneInfo) {
      this.timestampConverter = new TimestampConverter(
        filterResult.timezoneInfo,
      );
    }

    if (!filterResult.perfetto && filterResult.legacy.length === 0) {
      UserNotifier.add(new NoValidFiles());
      return;
    }

    const legacyParsers = await new LegacyParserFactory().createParsers(
      filterResult.legacy,
      this.timestampConverter,
      progressListener,
    );

    let perfettoParsers: FileAndParsers | undefined;

    if (filterResult.perfetto) {
      const parsers = await new PerfettoParserFactory().createParsers(
        filterResult.perfetto,
        this.timestampConverter,
        progressListener,
      );
      perfettoParsers = new FileAndParsers(filterResult.perfetto, parsers);
    }

    const monotonicTimeOffset =
      this.loadedParsers.getLatestRealToMonotonicOffset(
        legacyParsers
          .map((fileAndParser) => fileAndParser.parser)
          .concat(perfettoParsers?.parsers ?? []),
      );

    const realToBootTimeOffset =
      this.loadedParsers.getLatestRealToBootTimeOffset(
        legacyParsers
          .map((fileAndParser) => fileAndParser.parser)
          .concat(perfettoParsers?.parsers ?? []),
      );

    if (monotonicTimeOffset !== undefined) {
      this.timestampConverter.setRealToMonotonicTimeOffsetNs(
        monotonicTimeOffset,
      );
    }
    if (realToBootTimeOffset !== undefined) {
      this.timestampConverter.setRealToBootTimeOffsetNs(realToBootTimeOffset);
    }

    perfettoParsers?.parsers.forEach((p) => p.createTimestamps());
    legacyParsers.forEach((fileAndParser) =>
      fileAndParser.parser.createTimestamps(),
    );

    this.loadedParsers.addParsers(legacyParsers, perfettoParsers);
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
  ): Promise<UnzippedArchive[]> {
    const unzippedArchives: UnzippedArchive[] = [];
    const progressMessage = 'Unzipping files...';

    progressListener?.onProgressUpdate(progressMessage, 0);

    for (let i = 0; i < files.length; i++) {
      let file = files[i];

      const onSubProgressUpdate = (subPercentage: number) => {
        const totalPercentage =
          (100 * i) / files.length + subPercentage / files.length;
        progressListener?.onProgressUpdate(progressMessage, totalPercentage);
      };

      if (await FileUtils.isGZipFile(file)) {
        file = await FileUtils.decompressGZipFile(file);
      }

      if (await FileUtils.isZipFile(file)) {
        try {
          const subFiles = await FileUtils.unzipFile(file, onSubProgressUpdate);
          const subTraceFiles = subFiles.map((subFile) => {
            return new TraceFile(subFile, file);
          });
          unzippedArchives.push([...subTraceFiles]);
          onSubProgressUpdate(100);
        } catch (e) {
          UserNotifier.add(new CorruptedArchive(file));
        }
      } else {
        unzippedArchives.push([new TraceFile(file, undefined)]);
        onSubProgressUpdate(100);
      }
    }

    progressListener?.onProgressUpdate(progressMessage, 100);

    return unzippedArchives;
  }

  private removeTracesAndParsersByType(type: TraceType) {
    const traces = this.traces.getTraces(type);
    traces.forEach((trace) => {
      this.removeTrace(trace, true);
    });
  }
}
