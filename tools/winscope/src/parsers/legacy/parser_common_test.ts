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
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {ParserFactory} from './parser_factory';

describe('Parser', () => {
  beforeAll(() => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
  });

  it('is robust to empty trace file', async () => {
    const trace = new TraceFile(
      await UnitTestUtils.getFixtureFile('traces/empty.pb'),
      undefined,
    );
    const parsers = await new ParserFactory().createParsers(
      [trace],
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
      {},
    );
    expect(parsers.length).toEqual(0);
  });

  it('is robust to trace with no entries', async () => {
    const trace = new TraceFile(
      await UnitTestUtils.getFixtureFile(
        'traces/no_entries_InputMethodClients.pb',
      ),
      undefined,
    );
    const parsers = await new ParserFactory().createParsers(
      [trace],
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
      {},
    );
    expect(parsers.length).toEqual(0);
  });

  it('is robust to view capture trace with no entries', async () => {
    const trace = new TraceFile(
      await UnitTestUtils.getFixtureFile('traces/no_entries_view_capture.vc'),
      undefined,
    );
    const parsers = await new ParserFactory().createParsers(
      [trace],
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
      {},
    );
    expect(parsers.length).toEqual(0);
  });

  describe('real timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected descriptors', () => {
      expect(parser.getDescriptors()).toEqual(['WindowManager.pb']);
    });

    it('provides timestamps', () => {
      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1659107089075566202n),
        TimestampConverterUtils.makeRealTimestamp(1659107089999048990n),
        TimestampConverterUtils.makeRealTimestamp(1659107090010194213n),
      ];
      expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(
        expected,
      );
    });

    it('retrieves trace entries', async () => {
      let entry = await parser.getEntry(0);
      expect(
        assertDefined(entry.getEagerPropertyByName('focusedApp')).getValue(),
      ).toEqual('com.google.android.apps.nexuslauncher/.NexusLauncherActivity');

      entry = await parser.getEntry(parser.getLengthEntries() - 1);
      expect(
        assertDefined(entry.getEagerPropertyByName('focusedApp')).getValue(),
      ).toEqual('com.google.android.apps.nexuslauncher/.NexusLauncherActivity');
    });
  });

  describe('elapsed timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/WindowManager.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('provides timestamps', () => {
      const expected = [
        TimestampConverterUtils.makeElapsedTimestamp(850254319343n),
        TimestampConverterUtils.makeElapsedTimestamp(850763506110n),
        TimestampConverterUtils.makeElapsedTimestamp(850782750048n),
      ];
      expect(parser.getTimestamps()).toEqual(expected);
    });

    it('retrieves trace entries', async () => {
      let entry = await parser.getEntry(0);
      expect(
        assertDefined(entry.getEagerPropertyByName('focusedApp')).getValue(),
      ).toEqual('com.google.android.apps.nexuslauncher/.NexusLauncherActivity');

      entry = await parser.getEntry(parser.getLengthEntries() - 1);
      expect(
        assertDefined(entry.getEagerPropertyByName('focusedApp')).getValue(),
      ).toEqual('com.google.android.apps.nexuslauncher/.NexusLauncherActivity');
    });
  });
});
