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
import {
  getParserWithLatestRealToBootTimeOffset,
  getParserWithLatestRealToMonotonicTimeOffset,
} from 'parsers/parser_time_utils';
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
  private timestampConverter = getTimestampConverter();
  private initializeRealToElapsedTimeOffsetNs = true;
  private metadata: TraceMetadata = {};
  private convertToPerfetto = false;
  private existingPerfettoFile: TraceFile | undefined;

  addFilename(value: string) {
    this.filenames.push(value);
    return this;
  }

  setTimestampConverter(value: TimestampConverter) {
    this.timestampConverter = value;
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

  setExistingPerfettoFile(value: TraceFile) {
    this.existingPerfettoFile = value;
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
      this.timestampConverter,
      this.metadata,
    );

    createTimestamps(
      processedFiles.parsers,
      this.initializeRealToElapsedTimeOffsetNs,
      this.timestampConverter,
    );

    const fileAndParsers = this.convertToPerfetto
      ? await convertToPerfettoTrace(
          processedFiles.parsers,
          this.timestampConverter,
          this.existingPerfettoFile,
        )
      : processedFiles.parsers;

    if (this.convertToPerfetto) {
      this.timestampConverter.clear();
      createTimestamps(
        fileAndParsers,
        this.initializeRealToElapsedTimeOffsetNs,
        this.timestampConverter,
      );
    }

    return fileAndParsers.map((fileAndParser) => {
      return fileAndParser.parser;
    });
  }
}

export async function convertToPerfettoTrace(
  fileAndParsers: FileAndParser[],
  timestampConverter: TimestampConverter,
  existingPerfettoFile?: TraceFile,
): Promise<FileAndParser[]> {
  const parsers = fileAndParsers.map((p) => p.parser);
  const perfettoTrace =
    await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
      parsers,
      parsers,
      existingPerfettoFile,
    );
  if (perfettoTrace) {
    const processed = await new PerfettoParserFactory().processFile(
      perfettoTrace,
      timestampConverter,
    );
    fileAndParsers = processed.parsers.map((parser) => {
      return new FileAndParser(perfettoTrace, parser);
    });
  }
  return fileAndParsers;
}

export async function getTrace<T extends TraceType>(
  type: T,
  filename: string,
): Promise<Trace<T>> {
  const converter = getTimestampConverter(false);
  const legacyParsers = await new LegacyParserProvider()
    .addFilename(filename)
    .setTimestampConverter(converter)
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
    const monotonicOffset = getParserWithLatestRealToMonotonicTimeOffset(
      fileAndParsers.map((fileAndParser) => fileAndParser.parser),
    )?.getRealToMonotonicTimeOffsetNs();
    if (monotonicOffset !== undefined) {
      converter.setRealToMonotonicTimeOffsetNs(monotonicOffset);
    }
    const boottimeOffset = getParserWithLatestRealToBootTimeOffset(
      fileAndParsers.map((fileAndParser) => fileAndParser.parser),
    )?.getRealToBootTimeOffsetNs();
    if (boottimeOffset !== undefined) {
      converter.setRealToBootTimeOffsetNs(boottimeOffset);
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
): Promise<{
  tracesParser: Parser<object>;
  constituentParsers: Array<Parser<object>>;
}> {
  const converter = getTimestampConverter(withUTCOffset);
  const provider = new LegacyParserProvider();
  filenames.forEach((filename) => provider.addFilename(filename));
  const legacyParsers = await provider
    .setTimestampConverter(converter)
    .setInitializeRealToElapsedTimeOffsetNs(true)
    .getParsers();

  const perfettoParsers = (
    await Promise.all(
      filenames.map(async (filename) => getPerfettoParsers(filename)),
    )
  ).reduce((acc, cur) => acc.concat(cur), []);

  const parsersArray = legacyParsers.concat(perfettoParsers);

  const offset =
    getParserWithLatestRealToBootTimeOffset(
      parsersArray,
    )?.getRealToBootTimeOffsetNs();
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
  return {tracesParser: tracesParsers[0], constituentParsers: parsersArray};
}

export async function getWindowManagerState(
  index = 0,
): Promise<HierarchyTreeNode> {
  return getTraceEntry(
    'traces/elapsed_and_real_timestamp/WindowManager.pb',
    index,
  );
}

export async function getImeTraceEntries(): Promise<
  [Map<TraceType, HierarchyTreeNode>, Map<TraceType, HierarchyTreeNode>]
> {
  const [clientsParser, managerServiceParser, serviceParser, sfParser] =
    (await new LegacyParserProvider()
      .addFilename('traces/ime/SurfaceFlinger_with_IME.pb')
      .addFilename('traces/ime/InputMethodService.pb')
      .addFilename('traces/ime/InputMethodManagerService.pb')
      .addFilename('traces/ime/InputMethodClients.pb')
      .setConvertToPerfetto(true)
      .getParsers()) as Array<Parser<HierarchyTreeNode>>;

  const surfaceFlingerEntry = await sfParser.getEntry(5);
  const imServiceEntry = await serviceParser.getEntry(0);
  const imManagerServiceEntry = await managerServiceParser.getEntry(0);
  const clientsEntry0 = await clientsParser.getEntry(0);
  const clientsEntry1 = await clientsParser.getEntry(1);

  const windowManagerEntry = await getTraceEntry<HierarchyTreeNode>(
    'traces/ime/WindowManager_with_IME.pb',
    2,
  );

  const entries = new Map<TraceType, HierarchyTreeNode>();
  entries.set(TraceType.INPUT_METHOD_CLIENTS, clientsEntry0);
  entries.set(TraceType.INPUT_METHOD_MANAGER_SERVICE, imManagerServiceEntry);
  entries.set(TraceType.INPUT_METHOD_SERVICE, imServiceEntry);
  entries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
  entries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

  const secondEntries = new Map<TraceType, HierarchyTreeNode>();
  secondEntries.set(TraceType.INPUT_METHOD_CLIENTS, clientsEntry1);
  secondEntries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
  secondEntries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

  return [entries, secondEntries];
}

async function getTraceEntry<T>(filename: string, index = 0) {
  const parser = await new LegacyParserProvider()
    .addFilename(filename)
    .getParser<T>();
  return parser.getEntry(index);
}
