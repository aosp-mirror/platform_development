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

import {FileUtils} from 'common/file_utils';
import {INVALID_TIME_NS, TimeRange, TimestampType} from 'common/time';
import {TraceHasOldData, TraceOverridden} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TRACE_INFO} from './trace_info';

export class LoadedParsers {
  static readonly MAX_ALLOWED_TIME_GAP_BETWEEN_TRACES_NS =
    5n * 60n * 1000000000n; // 5m

  private legacyParsers = new Map<TraceType, FileAndParser>();
  private perfettoParsers = new Map<TraceType, FileAndParser>();

  addParsers(
    legacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
    errorListener: WinscopeErrorListener,
  ) {
    if (perfettoParsers) {
      this.addPerfettoParsers(perfettoParsers, errorListener);
    }

    legacyParsers = this.filterOutLegacyParsersWithOldData(
      legacyParsers,
      errorListener,
    );
    legacyParsers = this.filterScreenshotParsersIfRequired(
      legacyParsers,
      errorListener,
    );

    this.addLegacyParsers(legacyParsers, errorListener);
  }

  getParsers(): Array<Parser<object>> {
    const fileAndParsers = [
      ...this.legacyParsers.values(),
      ...this.perfettoParsers.values(),
    ];
    return fileAndParsers.map((fileAndParser) => fileAndParser.parser);
  }

  remove(type: TraceType) {
    this.legacyParsers.delete(type);
    this.perfettoParsers.delete(type);
  }

  clear() {
    this.legacyParsers.clear();
    this.perfettoParsers.clear();
  }

  async makeZipArchive(): Promise<Blob> {
    const archiveFiles: File[] = [];

    if (this.perfettoParsers.size > 0) {
      const file: TraceFile = this.perfettoParsers.values().next().value.file;
      const filenameInArchive = FileUtils.removeDirFromFileName(file.file.name);
      const archiveFile = new File([file.file], filenameInArchive);
      archiveFiles.push(archiveFile);
    }

    this.legacyParsers.forEach(({file, parser}, traceType) => {
      const archiveDir =
        TRACE_INFO[traceType].downloadArchiveDir.length > 0
          ? TRACE_INFO[traceType].downloadArchiveDir + '/'
          : '';
      const filenameInArchive =
        archiveDir + FileUtils.removeDirFromFileName(file.file.name);
      const archiveFile = new File([file.file], filenameInArchive);
      archiveFiles.push(archiveFile);
    });

    // Remove duplicates because some traces (e.g. view capture) could share the same file
    const uniqueArchiveFiles = archiveFiles.filter(
      (file, index, fileList) => fileList.indexOf(file) === index,
    );

    return await FileUtils.createZipArchive(uniqueArchiveFiles);
  }

  findCommonTimestampType(): TimestampType | undefined {
    return this.findCommonTimestampTypeInternal(this.getParsers());
  }
  private findCommonTimestampTypeInternal(
    parsers: Array<Parser<object>>,
  ): TimestampType | undefined {
    const priorityOrder = [TimestampType.REAL, TimestampType.ELAPSED];
    for (const type of priorityOrder) {
      if (parsers.every((parser) => parser.getTimestamps(type) !== undefined)) {
        return type;
      }
    }
    return undefined;
  }

  private addLegacyParsers(
    parsers: FileAndParser[],
    errorListener: WinscopeErrorListener,
  ) {
    const legacyParsersBeingLoaded = new Map<TraceType, Parser<object>>();

    parsers.forEach((fileAndParser) => {
      const {parser} = fileAndParser;
      if (
        this.shouldUseLegacyParser(
          parser,
          legacyParsersBeingLoaded,
          errorListener,
        )
      ) {
        legacyParsersBeingLoaded.set(parser.getTraceType(), parser);
        this.legacyParsers.set(parser.getTraceType(), fileAndParser);
      }
    });
  }

  private addPerfettoParsers(
    {file, parsers}: FileAndParsers,
    errorListener: WinscopeErrorListener,
  ) {
    // We currently run only one Perfetto TP WebWorker at a time, so Perfetto parsers previously
    // loaded are now invalid and must be removed (previous WebWorker is not running anymore).
    this.perfettoParsers.clear();

    parsers.forEach((parser) => {
      this.perfettoParsers.set(
        parser.getTraceType(),
        new FileAndParser(file, parser),
      );

      // While transitioning to the Perfetto format, devices might still have old legacy trace files
      // dangling in the disk that get automatically included into bugreports. Hence, Perfetto
      // parsers must always override legacy ones so that dangling legacy files are ignored.
      const legacyParser = this.legacyParsers.get(parser.getTraceType());
      if (legacyParser) {
        errorListener.onError(
          new TraceOverridden(legacyParser.parser.getDescriptors().join()),
        );
        this.legacyParsers.delete(parser.getTraceType());
      }
    });
  }

  private shouldUseLegacyParser(
    newParser: Parser<object>,
    parsersBeingLoaded: Map<TraceType, Parser<object>>,
    errorListener: WinscopeErrorListener,
  ): boolean {
    // While transitioning to the Perfetto format, devices might still have old legacy trace files
    // dangling in the disk that get automatically included into bugreports. Hence, Perfetto parsers
    // must always override legacy ones so that dangling legacy files are ignored.
    if (this.perfettoParsers.get(newParser.getTraceType())) {
      errorListener.onError(
        new TraceOverridden(newParser.getDescriptors().join()),
      );
      return false;
    }

    const oldParser = this.legacyParsers.get(newParser.getTraceType())?.parser;
    const currParser = parsersBeingLoaded.get(newParser.getTraceType());
    if (!oldParser && !currParser) {
      return true;
    }

    if (oldParser && !currParser) {
      errorListener.onError(
        new TraceOverridden(oldParser.getDescriptors().join()),
      );
      return true;
    }

    if (
      currParser &&
      newParser.getLengthEntries() > currParser.getLengthEntries()
    ) {
      errorListener.onError(
        new TraceOverridden(currParser.getDescriptors().join()),
      );
      return true;
    }

    errorListener.onError(
      new TraceOverridden(newParser.getDescriptors().join()),
    );
    return false;
  }

  private filterOutLegacyParsersWithOldData(
    newLegacyParsers: FileAndParser[],
    errorListener: WinscopeErrorListener,
  ): FileAndParser[] {
    const allParsers = [
      ...newLegacyParsers,
      ...this.legacyParsers.values(),
      ...this.perfettoParsers.values(),
    ];

    const commonTimestampType = this.findCommonTimestampTypeInternal(
      allParsers.map(({parser}) => parser),
    );
    if (commonTimestampType === undefined) {
      return newLegacyParsers;
    }

    const timeRanges = allParsers
      .map(({parser}) => {
        const timestamps = parser.getTimestamps(commonTimestampType);
        if (!timestamps || timestamps.length === 0) {
          return undefined;
        }
        return new TimeRange(timestamps[0], timestamps[timestamps.length - 1]);
      })
      .filter((range) => range !== undefined) as TimeRange[];

    const timeGap = this.findLastTimeGapAboveThreshold(timeRanges);
    if (!timeGap) {
      return newLegacyParsers;
    }

    return newLegacyParsers.filter(({parser, file}) => {
      const timestamps = parser.getTimestamps(commonTimestampType);
      if (!timestamps || timestamps.length === 0) {
        return true;
      }

      const isDump =
        timestamps.length === 1 &&
        timestamps[0].getValueNs() === INVALID_TIME_NS;
      if (isDump) {
        return true;
      }

      const endTimestamp = timestamps[timestamps.length - 1];
      const isOldData = endTimestamp.getValueNs() <= timeGap.from.getValueNs();
      if (isOldData) {
        errorListener.onError(
          new TraceHasOldData(file.getDescriptor(), timeGap),
        );
        return false;
      }

      return true;
    });
  }

  private filterScreenshotParsersIfRequired(
    newLegacyParsers: FileAndParser[],
    errorListener: WinscopeErrorListener,
  ): FileAndParser[] {
    const oldScreenRecordingParser = this.legacyParsers.get(
      TraceType.SCREEN_RECORDING,
    )?.parser;
    const oldScreenshotParser = this.legacyParsers.get(
      TraceType.SCREENSHOT,
    )?.parser;

    const newScreenRecordingParsers = newLegacyParsers.filter(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() === TraceType.SCREEN_RECORDING,
    );
    const newScreenshotParsers = newLegacyParsers.filter(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() === TraceType.SCREENSHOT,
    );

    if (oldScreenRecordingParser || newScreenRecordingParsers.length > 0) {
      newScreenshotParsers.forEach((newScreenshotParser) => {
        errorListener.onError(
          new TraceOverridden(
            newScreenshotParser.parser.getDescriptors().join(),
            TraceType.SCREEN_RECORDING,
          ),
        );
      });

      if (oldScreenshotParser) {
        errorListener.onError(
          new TraceOverridden(
            oldScreenshotParser.getDescriptors().join(),
            TraceType.SCREEN_RECORDING,
          ),
        );
        this.remove(TraceType.SCREENSHOT);
      }

      return newLegacyParsers.filter(
        (fileAndParser) =>
          fileAndParser.parser.getTraceType() !== TraceType.SCREENSHOT,
      );
    }

    return newLegacyParsers;
  }

  private findLastTimeGapAboveThreshold(
    ranges: readonly TimeRange[],
  ): TimeRange | undefined {
    const rangesSortedByEnd = ranges
      .slice()
      .sort((a, b) => (a.to.getValueNs() < b.to.getValueNs() ? -1 : +1));

    for (let i = rangesSortedByEnd.length - 2; i >= 0; --i) {
      const curr = rangesSortedByEnd[i];
      const next = rangesSortedByEnd[i + 1];
      const gap = next.from.getValueNs() - curr.to.getValueNs();
      if (gap > LoadedParsers.MAX_ALLOWED_TIME_GAP_BETWEEN_TRACES_NS) {
        return new TimeRange(curr.to, next.from);
      }
    }

    return undefined;
  }
}
