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
import {UserNotifier} from 'common/user_notifier';
import {TraceHasOldData, TraceOverridden} from 'messaging/user_warnings';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceEntryTypeMap, TraceType} from 'trace/trace_type';

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

  private legacyParsers = new Array<FileAndParser>();
  private perfettoParsers = new Array<FileAndParser>();

  addParsers(
    legacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
  ) {
    if (perfettoParsers) {
      this.addPerfettoParsers(perfettoParsers);
    }
    // Traces were simultaneously upgraded to contain real-to-boottime or real-to-monotonic offsets.
    // If we have a mix of parsers with and without offsets, the ones without must be dangling
    // trace files with old data, and should be filtered out.
    legacyParsers = this.filterOutParsersWithoutOffsetsIfRequired(
      legacyParsers,
      perfettoParsers,
    );
    legacyParsers = this.filterOutLegacyParsersWithOldData(legacyParsers);
    legacyParsers = this.filterScreenshotParsersIfRequired(legacyParsers);

    this.addLegacyParsers(legacyParsers);

    this.enforceLimitOfSingleScreenshotOrScreenRecordingParser();
  }

  getParsers(): Array<Parser<object>> {
    const fileAndParsers = [
      ...this.legacyParsers.values(),
      ...this.perfettoParsers.values(),
    ];
    return fileAndParsers.map((fileAndParser) => fileAndParser.parser);
  }

  remove<T extends TraceType>(parser: Parser<TraceEntryTypeMap[T]>) {
    this.legacyParsers = this.legacyParsers.filter(
      (fileAndParser) => fileAndParser.parser !== parser,
    );
    this.perfettoParsers = this.perfettoParsers.filter(
      (fileAndParser) => fileAndParser.parser !== parser,
    );
  }

  clear() {
    this.legacyParsers = [];
    this.perfettoParsers = [];
  }

  async makeZipArchive(): Promise<Blob> {
    const outputFilesSoFar = new Set<File>();
    const outputFilenameToFiles = new Map<string, File[]>();

    const tryPushOutputFile = (file: File, filename: string) => {
      // Remove duplicates because some parsers (e.g. view capture) could share the same file
      if (outputFilesSoFar.has(file)) {
        return;
      }
      outputFilesSoFar.add(file);

      if (outputFilenameToFiles.get(filename) === undefined) {
        outputFilenameToFiles.set(filename, []);
      }
      assertDefined(outputFilenameToFiles.get(filename)).push(file);
    };

    const makeArchiveFile = (
      filename: string,
      file: File,
      clashCount: number,
    ): File => {
      if (clashCount === 0) {
        return new File([file], filename);
      }

      const filenameWithoutExt =
        FileUtils.removeExtensionFromFilename(filename);
      const extension = FileUtils.getFileExtension(filename);

      if (extension === undefined) {
        return new File([file], `${filename} (${clashCount})`);
      }

      return new File(
        [file],
        `${filenameWithoutExt} (${clashCount}).${extension}`,
      );
    };

    if (this.perfettoParsers.length > 0) {
      const file: TraceFile = this.perfettoParsers.values().next().value.file;
      let outputFilename = FileUtils.removeDirFromFileName(file.file.name);
      if (FileUtils.getFileExtension(file.file.name) === undefined) {
        outputFilename += '.perfetto-trace';
      }
      tryPushOutputFile(file.file, outputFilename);
    }

    this.legacyParsers.forEach(({file, parser}) => {
      const traceType = parser.getTraceType();
      const archiveDir =
        TRACE_INFO[traceType].downloadArchiveDir.length > 0
          ? TRACE_INFO[traceType].downloadArchiveDir + '/'
          : '';
      let outputFilename =
        archiveDir + FileUtils.removeDirFromFileName(file.file.name);
      if (FileUtils.getFileExtension(file.file.name) === undefined) {
        outputFilename += TRACE_INFO[traceType].legacyExt;
      }
      tryPushOutputFile(file.file, outputFilename);
    });

    const archiveFiles = [...outputFilenameToFiles.entries()]
      .map(([filename, files]) => {
        return files.map((file, clashCount) =>
          makeArchiveFile(filename, file, clashCount),
        );
      })
      .flat();

    return await FileUtils.createZipArchive(archiveFiles);
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

  private addLegacyParsers(parsers: FileAndParser[]) {
    const legacyParsersBeingLoaded = new Map<TraceType, Parser<object>>();

    parsers.forEach((fileAndParser) => {
      const {parser} = fileAndParser;
      if (this.shouldUseLegacyParser(parser)) {
        legacyParsersBeingLoaded.set(parser.getTraceType(), parser);
        this.legacyParsers.push(fileAndParser);
      }
    });
  }

  private addPerfettoParsers({file, parsers}: FileAndParsers) {
    // We currently run only one Perfetto TP WebWorker at a time, so Perfetto parsers previously
    // loaded are now invalid and must be removed (previous WebWorker is not running anymore).
    this.perfettoParsers = [];

    parsers.forEach((parser) => {
      this.perfettoParsers.push(new FileAndParser(file, parser));

      // While transitioning to the Perfetto format, devices might still have old legacy trace files
      // dangling in the disk that get automatically included into bugreports. Hence, Perfetto
      // parsers must always override legacy ones so that dangling legacy files are ignored.
      this.legacyParsers = this.legacyParsers.filter((fileAndParser) => {
        const isOverriddenByPerfettoParser =
          fileAndParser.parser.getTraceType() === parser.getTraceType();
        if (isOverriddenByPerfettoParser) {
          UserNotifier.add(
            new TraceOverridden(fileAndParser.parser.getDescriptors().join()),
          );
        }
        return !isOverriddenByPerfettoParser;
      });
    });
  }

  private shouldUseLegacyParser(newParser: Parser<object>): boolean {
    // While transitioning to the Perfetto format, devices might still have old legacy trace files
    // dangling in the disk that get automatically included into bugreports. Hence, Perfetto parsers
    // must always override legacy ones so that dangling legacy files are ignored.
    const isOverriddenByPerfettoParser = this.perfettoParsers.some(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() === newParser.getTraceType(),
    );
    if (isOverriddenByPerfettoParser) {
      UserNotifier.add(new TraceOverridden(newParser.getDescriptors().join()));
      return false;
    }

    return true;
  }

  private filterOutLegacyParsersWithOldData(
    newLegacyParsers: FileAndParser[],
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
          UserNotifier.add(new TraceHasOldData(file.getDescriptor()));
          return false;
        }
      }

      const bootTimeOffset = parser.getRealToBootTimeOffsetNs();
      if (bootTimeOffset && latestBootTimeOffset) {
        const isOldData =
          Math.abs(Number(bootTimeOffset - latestBootTimeOffset)) >
          LoadedParsers.MAX_ALLOWED_TIME_GAP_BETWEEN_RTE_OFFSET;
        if (isOldData) {
          UserNotifier.add(new TraceHasOldData(file.getDescriptor()));
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
        UserNotifier.add(new TraceHasOldData(file.getDescriptor(), timeGap));
        return false;
      }

      return true;
    });
  }

  private filterScreenshotParsersIfRequired(
    newLegacyParsers: FileAndParser[],
  ): FileAndParser[] {
    const hasOldScreenRecordingParsers = this.legacyParsers.some(
      (entry) => entry.parser.getTraceType() === TraceType.SCREEN_RECORDING,
    );
    const hasNewScreenRecordingParsers = newLegacyParsers.some(
      (entry) => entry.parser.getTraceType() === TraceType.SCREEN_RECORDING,
    );
    const hasScreenRecordingParsers =
      hasOldScreenRecordingParsers || hasNewScreenRecordingParsers;

    if (!hasScreenRecordingParsers) {
      return newLegacyParsers;
    }

    const oldScreenshotParsers = this.legacyParsers.filter(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() === TraceType.SCREENSHOT,
    );
    const newScreenshotParsers = newLegacyParsers.filter(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() === TraceType.SCREENSHOT,
    );

    oldScreenshotParsers.forEach((fileAndParser) => {
      UserNotifier.add(
        new TraceOverridden(
          fileAndParser.parser.getDescriptors().join(),
          TraceType.SCREEN_RECORDING,
        ),
      );
      this.remove(fileAndParser.parser);
    });

    newScreenshotParsers.forEach((newScreenshotParser) => {
      UserNotifier.add(
        new TraceOverridden(
          newScreenshotParser.parser.getDescriptors().join(),
          TraceType.SCREEN_RECORDING,
        ),
      );
    });

    return newLegacyParsers.filter(
      (fileAndParser) =>
        fileAndParser.parser.getTraceType() !== TraceType.SCREENSHOT,
    );
  }

  private filterOutParsersWithoutOffsetsIfRequired(
    newLegacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
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
          UserNotifier.add(new TraceHasOldData(parser.getDescriptors().join()));
        }
        return hasOffset;
      });
    }

    return newLegacyParsers;
  }

  private enforceLimitOfSingleScreenshotOrScreenRecordingParser() {
    let firstScreenshotOrScreenrecordingParser: Parser<object> | undefined;

    this.legacyParsers = this.legacyParsers.filter((fileAndParser) => {
      const parser = fileAndParser.parser;
      if (
        parser.getTraceType() !== TraceType.SCREENSHOT &&
        parser.getTraceType() !== TraceType.SCREEN_RECORDING
      ) {
        return true;
      }

      if (firstScreenshotOrScreenrecordingParser) {
        UserNotifier.add(
          new TraceOverridden(
            parser.getDescriptors().join(),
            firstScreenshotOrScreenrecordingParser.getTraceType(),
          ),
        );
        return false;
      }

      firstScreenshotOrScreenrecordingParser = parser;
      return true;
    });
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
