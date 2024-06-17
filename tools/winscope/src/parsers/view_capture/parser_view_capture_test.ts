/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('ParserViewCapture', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    )) as Parser<HierarchyTreeNode>;
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER)
      .setParser(parser)
      .build();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(
      TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER,
    );
  });

  it('provides elapsed timestamps', () => {
    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(181114412436130n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(181114421012750n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(181114429047540n),
    ];
    expect(
      assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3),
    ).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1691692936292808460n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1691692936301385080n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1691692936309419870n),
    ];
    expect(
      assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3),
    ).toEqual(expected);
  });

  it('applies timezone info to real timestamps only', async () => {
    const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
      true,
    )) as Parser<HierarchyTreeNode>;
    expect(parserWithTimezoneInfo.getTraceType()).toEqual(
      TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER,
    );

    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
      )[0],
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(181114412436130n),
    );

    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
      )[0],
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1691626336292808460n),
    );
  });

  it('retrieves trace entry', async () => {
    const entry = await parser.getEntry(1, TimestampType.REAL);
    expect(entry.id).toEqual(
      'ViewNode com.android.launcher3.taskbar.TaskbarDragLayer@265160962',
    );
  });

  it('supports VIEW_CAPTURE_PACKAGE_NAME custom query', async () => {
    const packageName = await trace.customQuery(
      CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME,
    );
    expect(packageName).toEqual('com.google.android.apps.nexuslauncher');
  });
});
