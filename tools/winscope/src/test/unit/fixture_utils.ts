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

import {assertDefined, assertTrue} from 'common/assert_utils';
import {getTimestampConverter} from 'common/time/test_utils';
import {TimestampConverter} from 'common/time/timestamp_converter';
import {getRootUrl} from 'common/url_utils';
import {FileAndParser} from 'parsers/file_and_parser';
import {ParserFactory as LegacyParserFactory} from 'parsers/legacy/parser_factory';
import {LegacyToPerfettoConverter} from 'parsers/legacy_to_perfetto_converter';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {TracesParserFactory} from 'parsers/traces/traces_parser_factory';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceMetadata} from 'trace/trace_metadata';
import {TraceEntryTypeMap, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {TraceBuilder} from './trace_builder';

export async function getFixtureFile(
  srcFilename: string,
  dstFilename: string = srcFilename,
): Promise<File> {
  const url = getRootUrl() + 'base/src/test/fixtures/' + srcFilename;
  const response = await fetch(url);
  expect(response.ok).toBeTrue();
  const blob = await response.blob();
  const file = new File([blob], dstFilename);
  return file;
}

export class LegacyParserProvider {
  private filenames: string[] = [];
  private converter = getTimestampConverter();
  private initializeRealToElapsedTimeOffsetNs = true;
  private metadata: TraceMetadata = {};
  private convertToPerfetto = false;
  private latestRealToElapsedTimeOffsetNs = 0n;

  addFilename(value: string) {
    this.filenames.push(value);
    return this;
  }

  setConverter(value: TimestampConverter) {
    this.converter = value;
    return this;
  }

  setInitializeRealToElapsedTimeOffsetNs(value: boolean) {
    this.initializeRealToElapsedTimeOffsetNs = value;
    return this;
  }

  setMetadata(value: TraceMetadata) {
    this.metadata = value;
    return this;
  }

  setConvertToPerfetto(value: boolean) {
    this.convertToPerfetto = value;
    return this;
  }

  setLatestRealToElapsedTimeOffsetNs(value: bigint) {
    this.latestRealToElapsedTimeOffsetNs = value;
    return this;
  }

  async getParser<T>(): Promise<Parser<T>> {
    const parsers = await this.getParsers();

    expect(parsers.length)
      .withContext(
        `Should have been able to create a parser for ${this.filenames.join(
          ', ',
        )}`,
      )
      .toBeGreaterThanOrEqual(1);

    return parsers[0] as Parser<T>;
  }

  async getParsers(): Promise<Array<Parser<object>>> {
    const files = [];
    for (const filename of this.filenames) {
      const file = new TraceFile(
        await getFixtureFile(assertDefined(filename)),
        undefined,
      );
      files.push(file);
    }
    const processedFiles = await new LegacyParserFactory().processFiles(
      files,
      this.converter,
      this.metadata,
    );
    const fileAndParsers = this.convertToPerfetto
      ? await this.convertToPerfettoTrace(processedFiles.parsers)
      : processedFiles.parsers;

    createTimestamps(
      fileAndParsers,
      this.initializeRealToElapsedTimeOffsetNs,
      this.converter,
    );

    return fileAndParsers.map((fileAndParser) => {
      return fileAndParser.parser;
    });
  }

  private async convertToPerfettoTrace(
    fileAndParsers: FileAndParser[],
  ): Promise<FileAndParser[]> {
    const perfettoTrace =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        fileAndParsers,
        this.latestRealToElapsedTimeOffsetNs,
      );
    if (perfettoTrace) {
      const processed = await new PerfettoParserFactory().processFile(
        perfettoTrace,
        this.converter,
      );
      fileAndParsers = processed.parsers.map((parser) => {
        return new FileAndParser(perfettoTrace, parser);
      });
    }
    return fileAndParsers;
  }
}

export async function getTrace<T extends TraceType>(
  type: T,
  filename: string,
): Promise<Trace<T>> {
  const converter = getTimestampConverter(false);
  const legacyParsers = await new LegacyParserProvider()
    .addFilename(filename)
    .setConverter(converter)
    .getParsers();
  expect(legacyParsers.length).toBeLessThanOrEqual(1);
  if (legacyParsers.length === 1) {
    expect(legacyParsers[0].getTraceType()).toEqual(type);
    return new TraceBuilder<T>()
      .setType(type)
      .setParser(legacyParsers[0] as unknown as Parser<T>)
      .build();
  }

  const perfettoParsers = await getPerfettoParsers(filename);
  expect(perfettoParsers.length).toEqual(1);
  expect(perfettoParsers[0].getTraceType()).toEqual(type);
  return new TraceBuilder<T>()
    .setType(type)
    .setParser(perfettoParsers[0] as unknown as Parser<T>)
    .build();
}

function createTimestamps(
  fileAndParsers: FileAndParser[],
  initializeRealToElapsedTimeOffsetNs: boolean,
  converter: TimestampConverter,
) {
  if (initializeRealToElapsedTimeOffsetNs) {
    const monotonicOffset = fileAndParsers
      .find(
        (fileAndParser) =>
          fileAndParser.parser.getRealToMonotonicTimeOffsetNs() !== undefined,
      )
      ?.parser.getRealToMonotonicTimeOffsetNs();
    if (monotonicOffset !== undefined) {
      converter.setRealToMonotonicTimeOffsetNs(monotonicOffset);
    }
    const bootTimeOffset = fileAndParsers
      .find(
        (fileAndParser) =>
          fileAndParser.parser.getRealToBootTimeOffsetNs() !== undefined,
      )
      ?.parser.getRealToBootTimeOffsetNs();
    if (bootTimeOffset !== undefined) {
      converter.setRealToBootTimeOffsetNs(bootTimeOffset);
    }
  }
  fileAndParsers.forEach((fileAndParser) => {
    fileAndParser.parser.createTimestamps();
  });
}

export async function getPerfettoParser<T extends TraceType>(
  traceType: T,
  fixturePath: string,
  withUTCOffset = false,
): Promise<Parser<TraceEntryTypeMap[T]>> {
  const parsers = await getPerfettoParsers(fixturePath, withUTCOffset);
  const parser = assertDefined(
    parsers.find((parser) => parser.getTraceType() === traceType),
  );
  return parser as Parser<TraceEntryTypeMap[T]>;
}

export async function getPerfettoParsers(
  fixturePath: string,
  withUTCOffset = false,
  isPerfetto?: boolean,
): Promise<Array<Parser<object>>> {
  const file = await getFixtureFile(fixturePath);
  const traceFile = new TraceFile(file);
  const converter = getTimestampConverter(withUTCOffset);
  const {parsers, isPerfettoTrace} =
    await new PerfettoParserFactory().processFile(
      traceFile,
      converter,
      undefined,
    );
  if (isPerfetto !== undefined) {
    expect(isPerfettoTrace).toEqual(isPerfetto);
  }
  createTimestamps(
    parsers.map((parser) => {
      return new FileAndParser(traceFile, parser);
    }),
    true,
    converter,
  );
  return parsers;
}

export async function getTracesParser(
  filenames: string[],
  withUTCOffset = false,
): Promise<Parser<object>> {
  const converter = getTimestampConverter(withUTCOffset);
  const legacyParsers = (
    await Promise.all(
      filenames.map(async (filename) => {
        return new LegacyParserProvider()
          .addFilename(filename)
          .setConverter(converter)
          .setInitializeRealToElapsedTimeOffsetNs(true)
          .getParsers();
      }),
    )
  ).reduce((acc, cur) => acc.concat(cur), []);

  const perfettoParsers = (
    await Promise.all(
      filenames.map(async (filename) => getPerfettoParsers(filename)),
    )
  ).reduce((acc, cur) => acc.concat(cur), []);

  const parsersArray = legacyParsers.concat(perfettoParsers);

  const offset = parsersArray
    .filter((parser) => parser.getRealToBootTimeOffsetNs() !== undefined)
    .sort((a, b) =>
      Number(
        (a.getRealToBootTimeOffsetNs() ?? 0n) -
          (b.getRealToBootTimeOffsetNs() ?? 0n),
      ),
    )
    .at(-1)
    ?.getRealToBootTimeOffsetNs();

  if (offset !== undefined) {
    converter.setRealToBootTimeOffsetNs(offset);
  }

  const traces = new Traces();
  parsersArray.forEach((parser) => {
    const trace = Trace.fromParser(parser);
    traces.addTrace(trace);
  });

  const tracesParsers = await new TracesParserFactory().createParsers(
    traces,
    converter,
  );
  assertTrue(
    tracesParsers.length === 1,
    () =>
      `Should have been able to create a traces parser for [${filenames.join()}]`,
  );
  return tracesParsers[0];
}

export async function getWindowManagerState(
  index = 0,
): Promise<HierarchyTreeNode> {
  return getTraceEntry(
    'traces/elapsed_and_real_timestamp/WindowManager.pb',
    index,
  );
}

export async function getLayerTraceEntry(
  index = 0,
): Promise<HierarchyTreeNode> {
  return await getTraceEntry<HierarchyTreeNode>(
    'traces/elapsed_timestamp/SurfaceFlinger.pb',
    index,
  );
}

export async function getViewCaptureEntry(): Promise<HierarchyTreeNode> {
  return await getTraceEntry<HierarchyTreeNode>(
    'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
  );
}

export async function getMultiDisplayLayerTraceEntry(): Promise<HierarchyTreeNode> {
  return await getTraceEntry<HierarchyTreeNode>(
    'traces/elapsed_and_real_timestamp/SurfaceFlinger_multidisplay.pb',
  );
}

export async function getImeTraceEntries(): Promise<
  [Map<TraceType, HierarchyTreeNode>, Map<TraceType, HierarchyTreeNode>]
> {
  const surfaceFlingerEntry = await getTraceEntry<HierarchyTreeNode>(
    'traces/ime/SurfaceFlinger_with_IME.pb',
    5,
  );
  const windowManagerEntry = await getTraceEntry<HierarchyTreeNode>(
    'traces/ime/WindowManager_with_IME.pb',
    2,
  );

  const entries = new Map<TraceType, HierarchyTreeNode>();
  entries.set(
    TraceType.INPUT_METHOD_CLIENTS,
    await getTraceEntry('traces/ime/InputMethodClients.pb'),
  );
  entries.set(
    TraceType.INPUT_METHOD_MANAGER_SERVICE,
    await getTraceEntry('traces/ime/InputMethodManagerService.pb'),
  );
  entries.set(
    TraceType.INPUT_METHOD_SERVICE,
    await getTraceEntry('traces/ime/InputMethodService.pb'),
  );
  entries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
  entries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

  const secondEntries = new Map<TraceType, HierarchyTreeNode>();
  secondEntries.set(
    TraceType.INPUT_METHOD_CLIENTS,
    await getTraceEntry('traces/ime/InputMethodClients.pb', 1),
  );
  secondEntries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
  secondEntries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

  return [entries, secondEntries];
}

export async function getTraceEntry<T>(filename: string, index = 0) {
  const parser = await new LegacyParserProvider()
    .addFilename(filename)
    .setConvertToPerfetto(true)
    .getParser<T>();
  return parser.getEntry(index);
}
