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

import {ComponentFixture} from '@angular/core/testing';
import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'common/time';
import {TimestampConverter} from 'common/timestamp_converter';
import {UrlUtils} from 'common/url_utils';
import {ParserFactory as LegacyParserFactory} from 'parsers/legacy/parser_factory';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {TracesParserFactory} from 'parsers/traces/traces_parser_factory';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceFile} from 'trace/trace_file';
import {TraceEntryTypeMap, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {TimestampConverterUtils} from './timestamp_converter_utils';
import {TraceBuilder} from './trace_builder';

class UnitTestUtils {
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
    const converter = UnitTestUtils.getTimestampConverter(false);
    const legacyParsers = await UnitTestUtils.getParsers(filename, converter);
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
    converter = UnitTestUtils.getTimestampConverter(),
    initializeRealToElapsedTimeOffsetNs = true,
  ): Promise<Parser<object>> {
    const parsers = await UnitTestUtils.getParsers(
      filename,
      converter,
      initializeRealToElapsedTimeOffsetNs,
    );

    expect(parsers.length)
      .withContext(`Should have been able to create a parser for ${filename}`)
      .toBeGreaterThanOrEqual(1);

    return parsers[0];
  }

  static async getParsers(
    filename: string,
    converter = UnitTestUtils.getTimestampConverter(),
    initializeRealToElapsedTimeOffsetNs = true,
  ): Promise<Array<Parser<object>>> {
    const file = new TraceFile(
      await UnitTestUtils.getFixtureFile(filename),
      undefined,
    );
    const fileAndParsers = await new LegacyParserFactory().createParsers(
      [file],
      converter,
      undefined,
    );

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

    return fileAndParsers.map((fileAndParser) => {
      fileAndParser.parser.createTimestamps();
      return fileAndParser.parser;
    });
  }

  static async getPerfettoParser<T extends TraceType>(
    traceType: T,
    fixturePath: string,
    withUTCOffset = false,
  ): Promise<Parser<TraceEntryTypeMap[T]>> {
    const parsers = await UnitTestUtils.getPerfettoParsers(
      fixturePath,
      withUTCOffset,
    );
    const parser = assertDefined(
      parsers.find((parser) => parser.getTraceType() === traceType),
    );
    return parser as Parser<TraceEntryTypeMap[T]>;
  }

  static async getPerfettoParsers(
    fixturePath: string,
    withUTCOffset = false,
  ): Promise<Array<Parser<object>>> {
    const file = await UnitTestUtils.getFixtureFile(fixturePath);
    const traceFile = new TraceFile(file);
    const converter = UnitTestUtils.getTimestampConverter(withUTCOffset);
    const parsers = await new PerfettoParserFactory().createParsers(
      traceFile,
      converter,
      undefined,
    );
    parsers.forEach((parser) => {
      converter.setRealToBootTimeOffsetNs(
        assertDefined(parser.getRealToBootTimeOffsetNs()),
      );
      parser.createTimestamps();
    });
    return parsers;
  }

  static async getTracesParser(
    filenames: string[],
    withUTCOffset = false,
  ): Promise<Parser<object>> {
    const converter = UnitTestUtils.getTimestampConverter(withUTCOffset);
    const legacyParsers = (
      await Promise.all(
        filenames.map(async (filename) =>
          UnitTestUtils.getParsers(filename, converter, true),
        ),
      )
    ).reduce((acc, cur) => acc.concat(cur), []);

    const perfettoParsers = (
      await Promise.all(
        filenames.map(async (filename) =>
          UnitTestUtils.getPerfettoParsers(filename),
        ),
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
    expect(tracesParsers.length)
      .withContext(
        `Should have been able to create a traces parser for [${filenames.join()}]`,
      )
      .toEqual(1);
    return tracesParsers[0];
  }

  static getTimestampConverter(withUTCOffset = false): TimestampConverter {
    return withUTCOffset
      ? new TimestampConverter(TimestampConverterUtils.ASIA_TIMEZONE_INFO)
      : new TimestampConverter(TimestampConverterUtils.UTC_TIMEZONE_INFO);
  }

  static async getWindowManagerState(index = 0): Promise<HierarchyTreeNode> {
    return UnitTestUtils.getTraceEntry(
      'traces/elapsed_and_real_timestamp/WindowManager.pb',
      index,
    );
  }

  static async getLayerTraceEntry(index = 0): Promise<HierarchyTreeNode> {
    return await UnitTestUtils.getTraceEntry<HierarchyTreeNode>(
      'traces/elapsed_timestamp/SurfaceFlinger.pb',
      index,
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
    [Map<TraceType, HierarchyTreeNode>, Map<TraceType, HierarchyTreeNode>]
  > {
    let surfaceFlingerEntry: HierarchyTreeNode | undefined;
    {
      const parser = (await UnitTestUtils.getParser(
        'traces/ime/SurfaceFlinger_with_IME.pb',
      )) as Parser<HierarchyTreeNode>;
      surfaceFlingerEntry = await parser.getEntry(5);
    }

    let windowManagerEntry: HierarchyTreeNode | undefined;
    {
      const parser = (await UnitTestUtils.getParser(
        'traces/ime/WindowManager_with_IME.pb',
      )) as Parser<HierarchyTreeNode>;
      windowManagerEntry = await parser.getEntry(2);
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

    const secondEntries = new Map<TraceType, HierarchyTreeNode>();
    secondEntries.set(
      TraceType.INPUT_METHOD_CLIENTS,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodClients.pb', 1),
    );
    secondEntries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
    secondEntries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

    return [entries, secondEntries];
  }

  static async getTraceEntry<T>(filename: string, index = 0) {
    const parser = (await UnitTestUtils.getParser(filename)) as Parser<T>;
    return parser.getEntry(index);
  }

  static timestampEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof Timestamp && second instanceof Timestamp) {
      return UnitTestUtils.testTimestamps(first, second);
    }
    return undefined;
  }

  static checkSectionCollapseAndExpand<T>(
    htmlElement: HTMLElement,
    fixture: ComponentFixture<T>,
    selector: string,
    sectionTitle: string,
  ) {
    const section = assertDefined(htmlElement.querySelector(selector));
    const collapseButton = assertDefined(
      section.querySelector('collapsible-section-title button'),
    ) as HTMLElement;
    collapseButton.click();
    fixture.detectChanges();
    expect(section.classList).toContain('collapsed');
    const collapsedSections = assertDefined(
      htmlElement.querySelector('collapsed-sections'),
    );
    const collapsedSection = assertDefined(
      collapsedSections.querySelector('.collapsed-section'),
    ) as HTMLElement;
    expect(collapsedSection.textContent).toContain(sectionTitle);
    collapsedSection.click();
    fixture.detectChanges();
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  }

  static checkNoCollapsedSectionButtons(htmlElement: HTMLElement) {
    const collapsedSections = assertDefined(
      htmlElement.querySelector('collapsed-sections'),
    );
    expect(
      collapsedSections.querySelectorAll('.collapsed-section').length,
    ).toEqual(0);
  }

  static makeEmptyTrace<T extends TraceType>(
    traceType: T,
  ): Trace<TraceEntryTypeMap[T]> {
    return new TraceBuilder<TraceEntryTypeMap[T]>()
      .setEntries([])
      .setTimestamps([])
      .setType(traceType)
      .build();
  }

  private static testTimestamps(
    timestamp: Timestamp,
    expectedTimestamp: Timestamp,
  ): boolean {
    if (timestamp.format() !== expectedTimestamp.format()) return false;
    if (timestamp.getValueNs() !== expectedTimestamp.getValueNs()) {
      return false;
    }
    return true;
  }
}

export {UnitTestUtils};
