/*
 * Copyright (C) 2024 The Android Open Source Project
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

describe('Perfetto ParserInputMethodManagerService', () => {
  let parser: Parser<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (await UnitTestUtils.getPerfettoParser(
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
      'traces/perfetto/ime.perfetto-trace',
    )) as Parser<HierarchyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
    );
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    expect(assertDefined(parser.getTimestamps()).length).toEqual(7);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1714659587704398638n),
      TimestampConverterUtils.makeRealTimestamp(1714659588929259723n),
      TimestampConverterUtils.makeRealTimestamp(1714659588964769244n),
    ];
    expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    const entry = await parser.getEntry(0);
    expect(entry).toBeInstanceOf(HierarchyTreeNode);
    expect(entry.id).toEqual('InputMethodManagerService entry');
  });

  //TODO: check decoded intdefs
});
