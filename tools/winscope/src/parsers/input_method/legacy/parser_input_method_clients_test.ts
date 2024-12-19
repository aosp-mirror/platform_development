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
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserInputMethodClients', () => {
  describe('trace with real timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('has expected coarse version', () => {
      expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
    });

    it('provides timestamps', () => {
      const expected = [
        TimestampConverterUtils.makeRealTimestamp(1659107090215405395n),
        TimestampConverterUtils.makeRealTimestamp(1659107090249283325n),
        TimestampConverterUtils.makeRealTimestamp(1659107090279417928n),
      ];
      expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(
        expected,
      );
    });

    it('retrieves trace entry', async () => {
      const entry = await parser.getEntry(1);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('InputMethodClients entry');
    });
  });

  describe('trace with only elapsed timestamps', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/InputMethodClients.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    });

    it('provides timestamps', () => {
      expect(assertDefined(parser.getTimestamps())[0]).toEqual(
        TimestampConverterUtils.makeElapsedTimestamp(1149083651642n),
      );
    });

    it('retrieves trace entry from timestamp', async () => {
      const entry = await parser.getEntry(0);
      expect(entry).toBeInstanceOf(HierarchyTreeNode);
      expect(entry.id).toEqual('InputMethodClients entry');
    });

    it('translates intdefs', async () => {
      const entry = await parser.getEntry(8);
      const client = assertDefined(entry.getChildByName('client'));
      const properties = await client.getAllProperties();
      const intdefProperty = assertDefined(
        properties
          ?.getChildByName('viewRootImpl')
          ?.getChildByName('windowAttributes')
          ?.getChildByName('type'),
      );
      expect(intdefProperty.formattedValue()).toEqual('TYPE_BASE_APPLICATION');
    });
  });
});
