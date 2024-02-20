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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserWindowManagerDump', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    parser = (await UnitTestUtils.getParser(
      'traces/dump_WindowManager.pb',
    )) as Parser<HierarchyTreeNode>;
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.WINDOW_MANAGER)
      .setParser(parser)
      .build();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
  });

  it('provides elapsed timestamp (always zero)', () => {
    const expected = [NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n)];
    expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
  });

  it('provides real timestamp (always zero)', () => {
    const expected = [NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n)];
    expect(parser.getTimestamps(TimestampType.REAL)).toEqual(expected);
  });

  it('does not apply timezone info', async () => {
    const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
      'traces/dump_WindowManager.pb',
      true,
    )) as Parser<HierarchyTreeNode>;
    expect(parserWithTimezoneInfo.getTraceType()).toEqual(
      TraceType.WINDOW_MANAGER,
    );

    expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual([
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n),
    ]);
    expect(parser.getTimestamps(TimestampType.REAL)).toEqual([
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
    ]);
  });

  it('retrieves trace entry', async () => {
    const entry = await parser.getEntry(0, TimestampType.ELAPSED);
    expect(entry).toBeInstanceOf(HierarchyTreeNode);
    expect(entry.getEagerPropertyByName('focusedApp')?.getValue()).toEqual(
      'com.google.android.apps.nexuslauncher/.NexusLauncherActivity',
    );
  });

  it('supports WM_WINDOWS_TOKEN_AND_TITLE custom query', async () => {
    const tokenAndTitles = await trace.customQuery(
      CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE,
    );
    expect(tokenAndTitles.length).toEqual(73);
    expect(tokenAndTitles).toContain({token: 'cab97a6', title: 'Leaf:36:36'});
  });
});
