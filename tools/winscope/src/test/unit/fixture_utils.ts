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

export async function getTrace<T extends TraceType>(
  type: T,
  filename: string,
): Promise<Trace<T>> {
  const converter = getTimestampConverter(false);
  const legacyParsers = await getParsers(filename, converter);
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

export async function getParser(
  filename: string,
  converter = getTimestampConverter(),
  initializeRealToElapsedTimeOffsetNs = true,
  metadata: TraceMetadata = {},
): Promise<Parser<object>> {
  const parsers = await getParsers(
    filename,
    converter,
    initializeRealToElapsedTimeOffsetNs,
    metadata,
  );

  expect(parsers.length)
    .withContext(`Should have been able to create a parser for ${filename}`)
    .toBeGreaterThanOrEqual(1);

  return parsers[0];
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

export async function getParsers(
  filename: string,
  converter = getTimestampConverter(),
  initializeRealToElapsedTimeOffsetNs = true,
  metadata: TraceMetadata = {},
): Promise<Array<Parser<object>>> {
  const file = new TraceFile(await getFixtureFile(filename), undefined);
  const processedFiles = await new LegacyParserFactory().processFiles(
    [file],
    converter,
    metadata,
  );
  const fileAndParsers = processedFiles.parsers;

  createTimestamps(
    fileAndParsers,
    initializeRealToElapsedTimeOffsetNs,
    converter,
  );

  return fileAndParsers.map((fileAndParser) => {
    return fileAndParser.parser;
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
    await new PerfettoParserFactory().processFiles(
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
      filenames.map(async (filename) => getParsers(filename, converter, true)),
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
  let surfaceFlingerEntry: HierarchyTreeNode | undefined;
  {
    const parser = (await getParser(
      'traces/ime/SurfaceFlinger_with_IME.pb',
    )) as Parser<HierarchyTreeNode>;
    surfaceFlingerEntry = await parser.getEntry(5);
  }

  let windowManagerEntry: HierarchyTreeNode | undefined;
  {
    const parser = (await getParser(
      'traces/ime/WindowManager_with_IME.pb',
    )) as Parser<HierarchyTreeNode>;
    windowManagerEntry = await parser.getEntry(2);
  }

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
  const parser = (await getParser(filename)) as Parser<T>;
  return parser.getEntry(index);
}
