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
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertySource} from 'trace/tree_node/property_tree_node';

describe('ParserViewCapture', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    )) as Parser<HierarchyTreeNode>;
    trace = Trace.fromParser(parser);
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.VIEW_CAPTURE);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual([
      '.Taskbar',
      'com.google.android.apps.nexuslauncher_0.vc',
    ]);
  });

  it('provides timestamps', () => {
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1691692936292808460n),
      TimestampConverterUtils.makeRealTimestamp(1691692936301385080n),
      TimestampConverterUtils.makeRealTimestamp(1691692936309419870n),
    ];
    expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    const entry = await parser.getEntry(1);
    expect(entry.id).toEqual(
      'ViewNode com.android.launcher3.taskbar.TaskbarDragLayer@265160962',
    );
    // check calculated properties not overridden by lazily fetched properties
    expect(
      (await entry.getAllProperties()).getChildByName('translationX')?.source,
    ).toEqual(PropertySource.CALCULATED);
  });

  it('supports VIEW_CAPTURE_METADATA custom query', async () => {
    const metadata = await trace.customQuery(
      CustomQueryType.VIEW_CAPTURE_METADATA,
    );
    expect(metadata.packageName).toEqual(
      'com.google.android.apps.nexuslauncher',
    );
    expect(metadata.windowName).toEqual('.Taskbar');
  });
});
