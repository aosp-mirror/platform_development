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

import {assertDefined} from 'common/assert_utils';
import {FileUtils} from 'common/file_utils';
import {INVALID_TIME_NS, TimeRange, Timestamp} from 'common/time';
import {TIME_UNIT_TO_NANO} from 'common/time_units';
import {UserNotificationsListener} from 'messaging/user_notifications_listener';
import {TraceHasOldData, TraceOverridden} from 'messaging/user_warnings';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TRACE_INFO} from './trace_info';

export class LoadedParsers {
  static readonly MAX_ALLOWED_TIME_GAP_BETWEEN_TRACES_NS = BigInt(
    5 * TIME_UNIT_TO_NANO.m,
  ); // 5m
  static readonly MAX_ALLOWED_TIME_GAP_BETWEEN_RTE_OFFSET = BigInt(
    5 * TIME_UNIT_TO_NANO.s,
  ); // 5s
  static readonly REAL_TIME_TRACES_WITHOUT_RTE_OFFSET = [
    TraceType.CUJS,
    TraceType.EVENT_LOG,
  ];

  private legacyParsers = new Map<TraceType, FileAndParser>();
  private perfettoParsers = new Map<TraceType, FileAndParser>();

  addParsers(
    legacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
    userNotificationsListener: UserNotificationsListener,
  ) {
    if (perfettoParsers) {
      this.addPerfettoParsers(perfettoParsers, userNotificationsListener);
    }
    // Traces were simultaneously upgraded to contain real-to-boottime or real-to-monotonic offsets.
    // If we have a mix of parsers with and without offsets, the ones without must be dangling
    // trace files with old data, and should be filtered out.
    legacyParsers = this.filterOutParsersWithoutOffsetsIfRequired(
      legacyParsers,
      perfettoParsers,
      userNotificationsListener,
    );
    legacyParsers = this.filterOutLegacyParsersWithOldData(
      legacyParsers,
      userNotificationsListener,
    );
    legacyParsers = this.filterScreenshotParsersIfRequired(
      legacyParsers,
      userNotificationsListener,
    );

    this.addLegacyParsers(legacyParsers, userNotificationsListener);
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

  getLatestRealToMonotonicOffset(
    parsers: Array<Parser<object>>,
  ): bigint | undefined {
    const p = parsers
      .filter((offset) => offset.getRealToMonotonicTimeOffsetNs() !== undefined)
      .sort((a, b) => {
        return Number(
          (a.getRealToMonotonicTimeOffsetNs() ?? 0n) -
            (b.getRealToMonotonicTimeOffsetNs() ?? 0n),
        );
      })
      .at(-1);
    return p?.getRealToMonotonicTimeOffsetNs();
  }

  getLatestRealToBootTimeOffset(
    parsers: Array<Parser<object>>,
  ): bigint | undefined {
    const p = parsers
      .filter((offset) => offset.getRealToBootTimeOffsetNs() !== undefined)
      .sort((a, b) => {
        return Number(
          (a.getRealToBootTimeOffsetNs() ?? 0n) -
            (b.getRealToBootTimeOffsetNs() ?? 0n),
        );
      })
      .at(-1);
    return p?.getRealToBootTimeOffsetNs();
  }

  private addLegacyParsers(
    parsers: FileAndParser[],
    userNotificationsListener: UserNotificationsListener,
  ) {
    const legacyParsersBeingLoaded = new Map<TraceType, Parser<object>>();

    parsers.forEach((fileAndParser) => {
      const {parser} = fileAndParser;
      if (
        this.shouldUseLegacyParser(
          parser,
          legacyParsersBeingLoaded,
          userNotificationsListener,
        )
      ) {
        legacyParsersBeingLoaded.set(parser.getTraceType(), parser);
        this.legacyParsers.set(parser.getTraceType(), fileAndParser);
      }
    });
  }

  private addPerfettoParsers(
    {file, parsers}: FileAndParsers,
    userNotificationsListener: UserNotificationsListener,
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
        userNotificationsListener.onNotifications([
          new TraceOverridden(legacyParser.parser.getDescriptors().join()),
        ]);
        this.legacyParsers.delete(parser.getTraceType());
      }
    });
  }

  private shouldUseLegacyParser(
    newParser: Parser<object>,
    parsersBeingLoaded: Map<TraceType, Parser<object>>,
    userNotificationsListener: UserNotificationsListener,
  ): boolean {
    // While transitioning to the Perfetto format, devices might still have old legacy trace files
    // dangling in the disk that get automatically included into bugreports. Hence, Perfetto parsers
    // must always override legacy ones so that dangling legacy files are ignored.
    if (this.perfettoParsers.get(newParser.getTraceType())) {
      userNotificationsListener.onNotifications([
        new TraceOverridden(newParser.getDescriptors().join()),
      ]);
      return false;
    }

    const oldParser = this.legacyParsers.get(newParser.getTraceType())?.parser;
    const currParser = parsersBeingLoaded.get(newParser.getTraceType());
    if (!oldParser && !currParser) {
      return true;
    }

    if (oldParser && !currParser) {
      userNotificationsListener.onNotifications([
        new TraceOverridden(oldParser.getDescriptors().join()),
      ]);
      return true;
    }

    if (
      currParser &&
      newParser.getLengthEntries() > currParser.getLengthEntries()
    ) {
      userNotificationsListener.onNotifications([
        new TraceOverridden(currParser.getDescriptors().join()),
      ]);
      return true;
    }

    userNotificationsListener.onNotifications([
      new TraceOverridden(newParser.getDescriptors().join()),
    ]);
    return false;
  }

  private filterOutLegacyParsersWithOldData(
    newLegacyParsers: FileAndParser[],
    userNotificationsListener: UserNotificationsListener,
  ): FileAndParser[] {
    let allParsers = [
      ...newLegacyParsers,
      ...this.legacyParsers.values(),
      ...this.perfettoParsers.values(),
    ];

    const latestMonotonicOffset = this.getLatestRealToMonotonicOffset(
      allParsers.map(({parser, file}) => parser),
    );
    const latestBootTimeOffset = this.getLatestRealToBootTimeOffset(
      allParsers.map(({parser, file}) => parser),
    );

    newLegacyParsers = newLegacyParsers.filter(({parser, file}) => {
      const monotonicOffset = parser.getRealToMonotonicTimeOffsetNs();
      if (monotonicOffset && latestMonotonicOffset) {
        const isOldData =
          Math.abs(Number(monotonicOffset - latestMonotonicOffset)) >
          LoadedParsers.MAX_ALLOWED_TIME_GAP_BETWEEN_RTE_OFFSET;
        if (isOldData) {
          userNotificationsListener.onNotifications([
            new TraceHasOldData(file.getDescriptor()),
          ]);
          return false;
        }
      }

      const bootTimeOffset = parser.getRealToBootTimeOffsetNs();
      if (bootTimeOffset && latestBootTimeOffset) {
        const isOldData =
          Math.abs(Number(bootTimeOffset - latestBootTimeOffset)) >
          LoadedParsers.MAX_ALLOWED_TIME_GAP_BETWEEN_RTE_OFFSET;
        if (isOldData) {
          userNotificationsListener.onNotifications([
            new TraceHasOldData(file.getDescriptor()),
          ]);
          return false;
        }
      }

      return true;
    });

    allParsers = [
      ...newLegacyParsers,
      ...this.legacyParsers.values(),
      ...this.perfettoParsers.values(),
    ];

    const timeRanges = allParsers
      .map(({parser}) => {
        const timestamps = parser.getTimestamps();
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
      // Only Shell Transition data used to set timestamps of merged Transition trace,
      // so WM Transition data should not be considered by "old data" policy
      if (parser.getTraceType() === TraceType.WM_TRANSITION) {
        return true;
      }

      let timestamps = parser.getTimestamps();
      if (!this.hasValidTimestamps(timestamps)) {
        return true;
      }
      timestamps = assertDefined(timestamps);

      const endTimestamp = timestamps[timestamps.length - 1];
      const isOldData = endTimestamp.getValueNs() <= timeGap.from.getValueNs();
      if (isOldData) {
        userNotificationsListener.onNotifications([
          new TraceHasOldData(file.getDescriptor(), timeGap),
        ]);
        return false;
      }

      return true;
    });
  }

  private filterScreenshotParsersIfRequired(
    newLegacyParsers: FileAndParser[],
    userNotificationsListener: UserNotificationsListener,
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
        userNotificationsListener.onNotifications([
          new TraceOverridden(
            newScreenshotParser.parser.getDescriptors().join(),
            TraceType.SCREEN_RECORDING,
          ),
        ]);
      });

      if (oldScreenshotParser) {
        userNotificationsListener.onNotifications([
          new TraceOverridden(
            oldScreenshotParser.getDescriptors().join(),
            TraceType.SCREEN_RECORDING,
          ),
        ]);
        this.remove(TraceType.SCREENSHOT);
      }

      return newLegacyParsers.filter(
        (fileAndParser) =>
          fileAndParser.parser.getTraceType() !== TraceType.SCREENSHOT,
      );
    }

    return newLegacyParsers;
  }

  private filterOutParsersWithoutOffsetsIfRequired(
    newLegacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
    userNotificationsListener: UserNotificationsListener,
  ): FileAndParser[] {
    const hasParserWithOffset =
      perfettoParsers ||
      newLegacyParsers.find(({parser, file}) => {
        return (
          parser.getRealToBootTimeOffsetNs() !== undefined ||
          parser.getRealToMonotonicTimeOffsetNs() !== undefined
        );
      });
    const hasParserWithoutOffset = newLegacyParsers.find(({parser, file}) => {
      const timestamps = parser.getTimestamps();
      return (
        this.hasValidTimestamps(timestamps) &&
        parser.getRealToBootTimeOffsetNs() === undefined &&
        parser.getRealToMonotonicTimeOffsetNs() === undefined
      );
    });

    if (hasParserWithOffset && hasParserWithoutOffset) {
      return newLegacyParsers.filter(({parser, file}) => {
        if (
          LoadedParsers.REAL_TIME_TRACES_WITHOUT_RTE_OFFSET.some(
            (traceType) => parser.getTraceType() === traceType,
          )
        ) {
          return true;
        }
        const hasOffset =
          parser.getRealToMonotonicTimeOffsetNs() !== undefined ||
          parser.getRealToBootTimeOffsetNs() !== undefined;
        if (!hasOffset) {
          userNotificationsListener.onNotifications([
            new TraceHasOldData(parser.getDescriptors().join()),
          ]);
        }
        return hasOffset;
      });
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

  private hasValidTimestamps(timestamps: Timestamp[] | undefined): boolean {
    if (!timestamps || timestamps.length === 0) {
      return false;
    }

    const isDump =
      timestamps.length === 1 && timestamps[0].getValueNs() === INVALID_TIME_NS;
    if (isDump) {
      return false;
    }
    return true;
  }
}
