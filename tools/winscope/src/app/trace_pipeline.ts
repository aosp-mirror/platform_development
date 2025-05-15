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
} from 'common/time/timestamp_converter';
import {UserNotifier} from 'common/user_notifier';
import {Analytics} from 'logging/analytics';
import {ProgressListener} from 'messaging/progress_listener';
import {UserWarning} from 'messaging/user_warning';
import {
  CorruptedArchive,
  InvalidPerfettoTrace,
  NoValidFiles,
  UnsupportedFileFormat,
} from 'messaging/user_warnings';
import {WinscopeEvent} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {ParserFactory as LegacyParserFactory} from 'parsers/legacy/parser_factory';
import {LegacyToPerfettoConverter} from 'parsers/legacy_to_perfetto_converter';
import {
  getParserWithLatestRealToBootTimeOffset,
  getParserWithLatestRealToMonotonicTimeOffset,
} from 'parsers/parser_time_utils';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {ParserSearch} from 'parsers/search/parser_search';
import {TracesParserFactory} from 'parsers/traces/traces_parser_factory';
import {FrameMapper} from 'trace/frame_mapper';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceEntryTypeMap, TraceType, TraceTypeUtils} from 'trace/trace_type';
import {QueryResult, Row} from 'trace_processor/query_result';
import {TraceProcessorFactory} from 'trace_processor/trace_processor_factory';
import {FilesSource} from './files_source';
import {LoadedParsers} from './loaded_parsers';
import {FilterResult, TraceFileFilter} from './trace_file_filter';

type UnzippedArchive = TraceFile[];

export class TracePipeline
  implements WinscopeEventListener, WinscopeEventEmitter
{
  private loadedParsers = new LoadedParsers();
  private traceFileFilter = new TraceFileFilter();
  private traces = new Traces();
  private downloadArchiveFilename?: string;
  private lostPerfettoPackets = 0;
  private timestampConverter = new TimestampConverter(UTC_TIMEZONE_INFO);

  setEmitEvent(callback: EmitEvent) {
    this.traceFileFilter.setEmitEvent(callback);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.traceFileFilter.onWinscopeEvent(event);
  }

  async loadFiles(
    files: File[],
    source: FilesSource,
    progressListener: ProgressListener | undefined,
  ): Promise<UserWarning[]> {
    this.downloadArchiveFilename = this.makeDownloadArchiveFilename(
      files,
      source,
    );

    const warnings: UserWarning[] = [];

    try {
      const unzippedArchives = await this.unzipFiles(files, progressListener);

      if (unzippedArchives.length === 0) {
        UserNotifier.add(new NoValidFiles());
        return warnings;
      }

      for (const unzippedArchive of unzippedArchives) {
        const newWarnings = await this.loadUnzippedArchive(
          unzippedArchive,
          source,
          progressListener,
        );
        warnings.push(...newWarnings);
      }

      await this.convertLoadedParsersToTraces();
      return warnings;
    } finally {
      progressListener?.onOperationFinished(true);
    }
  }

  async convertLegacyTracesToPerfetto() {
    const singlePerfettoTrace = await this.convertLegacyParsersToPerfettoFile();
    if (!singlePerfettoTrace) {
      return;
    }
    const perfettoParsers = await this.processPerfettoFile(
      singlePerfettoTrace,
      FilesSource.APP,
      undefined,
      new InvalidPerfettoTrace(singlePerfettoTrace.getDescriptor(), [
        'failed to convert legacy parsers into perfetto trace',
      ]),
    );
    if (!perfettoParsers || perfettoParsers.parsers.length === 0) {
      return;
    }

    this.timestampConverter.clear();
    this.updateTimestamps([], perfettoParsers);
    this.loadedParsers.addParsers([], perfettoParsers);
    await this.convertLoadedParsersToTraces();
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

  lostPackets(): number {
    return this.lostPerfettoPackets;
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
    this.lostPerfettoPackets = 0;
  }

  private async loadUnzippedArchive(
    unzippedArchive: UnzippedArchive,
    source: FilesSource,
    progressListener: ProgressListener | undefined,
  ): Promise<UserWarning[]> {
    const warnings: UserWarning[] = [];

    let startTimeMs = Date.now();
    const filterResult = await this.traceFileFilter.filter(unzippedArchive);
    warnings.push(...(filterResult.criticalWarnings ?? []));
    const size =
      filterResult.legacy.reduce(
        (totalSize, f) => (totalSize += f.file.size),
        0,
      ) + (filterResult.perfetto?.file.size ?? 0);
    Analytics.Loading.logFileExtractionTime(
      'bugreport',
      Date.now() - startTimeMs,
      size,
    );

    if (filterResult.timezoneInfo) {
      this.timestampConverter = new TimestampConverter(
        filterResult.timezoneInfo,
      );
    }

    if (!filterResult.perfetto && filterResult.legacy.length === 0) {
      UserNotifier.add(new NoValidFiles());
      return warnings;
    }

    startTimeMs = Date.now();

    const {parsers: legacyParsers, unsupportedFiles} =
      await this.processLegacyFiles(filterResult, source, progressListener);

    let perfettoParsers: FileAndParsers | undefined;
    if (filterResult.perfetto) {
      perfettoParsers = await this.processPerfettoFile(
        filterResult.perfetto,
        source,
        progressListener,
        new UnsupportedFileFormat(filterResult.perfetto.getDescriptor()),
      );
    } else {
      for (const file of unsupportedFiles) {
        if (perfettoParsers) {
          UserNotifier.add(new UnsupportedFileFormat(file.getDescriptor()));
          continue;
        }
        perfettoParsers = await this.processPerfettoFile(
          file,
          source,
          progressListener,
          new UnsupportedFileFormat(file.getDescriptor()),
        );
      }
    }

    if (perfettoParsers) {
      await this.checkForLostPerfettoPackets();
    }

    this.updateTimestamps(legacyParsers, perfettoParsers);
    this.loadedParsers.addParsers(legacyParsers, perfettoParsers);

    return warnings;
  }

  private async processLegacyFiles(
    filterResult: FilterResult,
    source: FilesSource,
    progressListener: ProgressListener | undefined,
  ) {
    const startTimeMs = Date.now();
    const processedLegacyFiles = await new LegacyParserFactory().processFiles(
      filterResult.legacy,
      this.timestampConverter,
      filterResult.metadata,
      progressListener,
    );
    Analytics.Loading.logFileParsingTime(
      'legacy',
      source,
      Date.now() - startTimeMs,
    );
    Analytics.Memory.logUsage('legacy_files_parsed');

    return processedLegacyFiles;
  }

  private async processPerfettoFile(
    file: TraceFile,
    source: FilesSource,
    progressListener: ProgressListener | undefined,
    onFailureWarning: UserWarning,
  ): Promise<FileAndParsers | undefined> {
    const startTimeMs = Date.now();
    const {parsers, isPerfettoTrace} =
      await new PerfettoParserFactory().processFile(
        file,
        this.timestampConverter,
        progressListener,
      );
    Analytics.Loading.logFileParsingTime(
      'perfetto',
      source,
      Date.now() - startTimeMs,
    );
    Analytics.Memory.logUsage('perfetto_files_parsed');
    if (parsers.length > 0) {
      return new FileAndParsers(file, parsers);
    }
    if (!isPerfettoTrace) {
      UserNotifier.add(onFailureWarning);
    }
    return undefined;
  }

  private async checkForLostPerfettoPackets() {
    const tp = TraceProcessorFactory.getSingleInstance();
    const packetLossQuery =
      'SELECT name, value FROM stats ' +
      "WHERE name = 'traced_buf_trace_writer_packet_loss'";
    const res = await tp.query(packetLossQuery);
    const value =
      res.numRows() > 0 ? res.firstRow<Row>({})['value'] : undefined;
    if (typeof value === 'bigint' && value > 0n) {
      this.lostPerfettoPackets = Number(value);
    } else {
      this.lostPerfettoPackets = 0;
    }
  }

  private updateTimestamps(
    nonPerfettoParsers: FileAndParser[],
    perfettoParsers?: FileAndParsers,
  ) {
    const allParsers = nonPerfettoParsers
      .map((fileAndParser) => fileAndParser.parser)
      .concat(perfettoParsers?.parsers ?? []);

    const monotonicTimeOffset =
      getParserWithLatestRealToMonotonicTimeOffset(
        allParsers,
      )?.getRealToMonotonicTimeOffsetNs();

    const realToBootTimeOffset =
      getParserWithLatestRealToBootTimeOffset(
        allParsers,
      )?.getRealToBootTimeOffsetNs();

    if (monotonicTimeOffset !== undefined) {
      this.timestampConverter.setRealToMonotonicTimeOffsetNs(
        monotonicTimeOffset,
      );
    }
    if (realToBootTimeOffset !== undefined) {
      this.timestampConverter.setRealToBootTimeOffsetNs(realToBootTimeOffset);
    }

    perfettoParsers?.parsers.forEach((p) => p.createTimestamps());
    nonPerfettoParsers.forEach((fileAndParser) =>
      fileAndParser.parser.createTimestamps(),
    );
  }

  private async convertLoadedParsersToTraces() {
    this.traces = new Traces();

    this.loadedParsers.getParsers().forEach((parser) => {
      const trace = Trace.fromParser(parser);
      this.traces.addTrace(trace);
      Analytics.Tracing.logTraceLoaded(parser);
    });

    const tracesParsers = await new TracesParserFactory().createParsers(
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
  }

  private async convertLegacyParsersToPerfettoFile(): Promise<
    TraceFile | undefined
  > {
    const legacyParsers = this.traces
      .mapTrace((trace) => {
        return trace.isPerfetto() ? undefined : trace.getParser();
      })
      .filter((parser) => parser !== undefined) as Array<Parser<object>>;

    if (legacyParsers.length === 0) {
      return undefined;
    }

    const allParsers = this.traces.mapTrace((trace) => {
      return trace.getParser();
    });

    return await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
      legacyParsers,
      allParsers,
      this.loadedParsers.getPerfettoFile(),
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
        'Cannot convert uploaded archive filename to acceptable format for download. ' +
          "Defaulting download filename to 'winscope.zip'.",
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

    const currArchive: UnzippedArchive = [];
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
        currArchive.push(new TraceFile(file, undefined));
      }
    }
    if (currArchive.length > 0) {
      unzippedArchives.push(currArchive);
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
