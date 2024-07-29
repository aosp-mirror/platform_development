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
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('Perfetto ParserWindowManager', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getPerfettoParser(
      TraceType.WINDOW_MANAGER,
      'traces/perfetto/windowmanager.perfetto-trace',
    )) as Parser<HierarchyTreeNode>;
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.WINDOW_MANAGER)
      .setParser(parser)
      .build();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1719409456335086006n),
      TimestampConverterUtils.makeRealTimestamp(1719409456922787137n),
      TimestampConverterUtils.makeRealTimestamp(1719409456929933622n),
    ];
    expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    const entry = await parser.getEntry(1);
    expect(entry).toBeInstanceOf(HierarchyTreeNode);
    expect(entry.id).toEqual('WindowManagerState root');
  });

  it('supports WM_WINDOWS_TOKEN_AND_TITLE custom query', async () => {
    const tokenAndTitles = await trace
      .sliceEntries(0, 1)
      .customQuery(CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE);
    expect(tokenAndTitles.length).toEqual(69);
    expect(tokenAndTitles).toContain({token: '86f4c23', title: 'Leaf:36:36'});
  });
});
