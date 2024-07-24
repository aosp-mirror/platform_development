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
import {Timestamp, TimestampType} from 'common/time';
import {
  NO_TIMEZONE_OFFSET_FACTORY,
  TimestampFactory,
} from 'common/timestamp_factory';
import {UrlUtils} from 'common/url_utils';
import {ParserFactory} from 'parsers/parser_factory';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {TracesParserFactory} from 'parsers/traces_parser_factory';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceEntryTypeMap, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {TraceBuilder} from './trace_builder';

class UnitTestUtils {
  static readonly TIMESTAMP_FACTORY_WITH_TIMEZONE = new TimestampFactory({
    timezone: 'Asia/Kolkata',
    locale: 'en-US',
  });

  static async getFixtureFile(
    srcFilename: string,
    dstFilename: string = srcFilename,
  ): Promise<File> {
    const url = UrlUtils.getRootUrl() + 'base/src/test/fixtures/' + srcFilename;
    const response = await fetch(url);
    expect(response.ok).toBeTrue();
    const blob = await response.blob();
    const file = new File([blob], dstFilename);
    return file;
  }

  static async getTrace<T extends TraceType>(
    type: T,
    filename: string,
  ): Promise<Trace<T>> {
    const legacyParsers = await UnitTestUtils.getParsers(filename);
    expect(legacyParsers.length).toBeLessThanOrEqual(1);
    if (legacyParsers.length === 1) {
      expect(legacyParsers[0].getTraceType()).toEqual(type);
      return new TraceBuilder<T>()
        .setType(type)
        .setParser(legacyParsers[0] as unknown as Parser<T>)
        .build();
    }

    const perfettoParsers = await UnitTestUtils.getPerfettoParsers(filename);
    expect(perfettoParsers.length).toEqual(1);
    expect(perfettoParsers[0].getTraceType()).toEqual(type);
    return new TraceBuilder<T>()
      .setType(type)
      .setParser(perfettoParsers[0] as unknown as Parser<T>)
      .build();
  }

  static async getParser(
    filename: string,
    withTimezoneInfo = false,
  ): Promise<Parser<object>> {
    const parsers = await UnitTestUtils.getParsers(filename, withTimezoneInfo);
    expect(parsers.length)
      .withContext(`Should have been able to create a parser for ${filename}`)
      .toBeGreaterThanOrEqual(1);
    return parsers[0];
  }

  static async getParsers(
    filename: string,
    withTimezoneInfo = false,
  ): Promise<Array<Parser<object>>> {
    const file = new TraceFile(
      await UnitTestUtils.getFixtureFile(filename),
      undefined,
    );
    const fileAndParsers = await new ParserFactory().createParsers(
      [file],
      withTimezoneInfo
        ? UnitTestUtils.TIMESTAMP_FACTORY_WITH_TIMEZONE
        : NO_TIMEZONE_OFFSET_FACTORY,
      undefined,
      undefined,
    );
    return fileAndParsers.map((fileAndParser) => {
      return fileAndParser.parser;
    });
  }

  static async getPerfettoParser<T extends TraceType>(
    traceType: T,
    fixturePath: string,
    withTimezoneInfo = false,
  ): Promise<Parser<TraceEntryTypeMap[T]>> {
    const parsers = await UnitTestUtils.getPerfettoParsers(
      fixturePath,
      withTimezoneInfo,
    );
    const parser = assertDefined(
      parsers.find((parser) => parser.getTraceType() === traceType),
    );
    return parser as Parser<TraceEntryTypeMap[T]>;
  }

  static async getPerfettoParsers(
    fixturePath: string,
    withTimezoneInfo = false,
  ): Promise<Array<Parser<object>>> {
    const file = await UnitTestUtils.getFixtureFile(fixturePath);
    const traceFile = new TraceFile(file);
    return await new PerfettoParserFactory().createParsers(
      traceFile,
      withTimezoneInfo
        ? UnitTestUtils.TIMESTAMP_FACTORY_WITH_TIMEZONE
        : NO_TIMEZONE_OFFSET_FACTORY,
      undefined,
    );
  }

  static async getTracesParser(
    filenames: string[],
    withTimezoneInfo = false,
  ): Promise<Parser<object>> {
    const parsersArray = await Promise.all(
      filenames.map((filename) =>
        UnitTestUtils.getParser(filename, withTimezoneInfo),
      ),
    );

    const traces = new Traces();
    parsersArray.forEach((parser) => {
      const trace = Trace.fromParser(parser, TimestampType.REAL);
      traces.setTrace(parser.getTraceType(), trace);
    });

    const tracesParsers = await new TracesParserFactory().createParsers(traces);
    expect(tracesParsers.length)
      .withContext(
        `Should have been able to create a traces parser for [${filenames.join()}]`,
      )
      .toEqual(1);
    return tracesParsers[0];
  }

  static async getWindowManagerState(): Promise<HierarchyTreeNode> {
    return UnitTestUtils.getTraceEntry(
      'traces/elapsed_timestamp/WindowManager.pb',
    );
  }

  static async getLayerTraceEntry(): Promise<HierarchyTreeNode> {
    return await UnitTestUtils.getTraceEntry<HierarchyTreeNode>(
      'traces/elapsed_timestamp/SurfaceFlinger.pb',
    );
  }

  static async getViewCaptureEntry(): Promise<HierarchyTreeNode> {
    return await UnitTestUtils.getTraceEntry<HierarchyTreeNode>(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    );
  }

  static async getMultiDisplayLayerTraceEntry(): Promise<HierarchyTreeNode> {
    return await UnitTestUtils.getTraceEntry<HierarchyTreeNode>(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger_multidisplay.pb',
    );
  }

  static async getImeTraceEntries(): Promise<
    Map<TraceType, HierarchyTreeNode>
  > {
    let surfaceFlingerEntry: HierarchyTreeNode | undefined;
    {
      const parser = (await UnitTestUtils.getParser(
        'traces/ime/SurfaceFlinger_with_IME.pb',
      )) as Parser<HierarchyTreeNode>;
      surfaceFlingerEntry = await parser.getEntry(5, TimestampType.ELAPSED);
    }

    let windowManagerEntry: HierarchyTreeNode | undefined;
    {
      const parser = (await UnitTestUtils.getParser(
        'traces/ime/WindowManager_with_IME.pb',
      )) as Parser<HierarchyTreeNode>;
      windowManagerEntry = await parser.getEntry(2, TimestampType.ELAPSED);
    }

    const entries = new Map<TraceType, HierarchyTreeNode>();
    entries.set(
      TraceType.INPUT_METHOD_CLIENTS,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodClients.pb'),
    );
    entries.set(
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
      await UnitTestUtils.getTraceEntry(
        'traces/ime/InputMethodManagerService.pb',
      ),
    );
    entries.set(
      TraceType.INPUT_METHOD_SERVICE,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodService.pb'),
    );
    entries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
    entries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

    return entries;
  }

  static timestampEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof Timestamp && second instanceof Timestamp) {
      return UnitTestUtils.testTimestamps(first, second);
    }
    return undefined;
  }

  private static testTimestamps(
    node: Timestamp,
    expectedNode: Timestamp,
  ): boolean {
    if (node.getType() !== expectedNode.getType()) return false;
    if (node.getValueNs() !== expectedNode.getValueNs()) return false;
    return true;
  }

  private static async getTraceEntry<T>(filename: string) {
    const parser = (await UnitTestUtils.getParser(filename)) as Parser<T>;
    return parser.getEntry(0, TimestampType.ELAPSED);
  }
}

export {UnitTestUtils};
