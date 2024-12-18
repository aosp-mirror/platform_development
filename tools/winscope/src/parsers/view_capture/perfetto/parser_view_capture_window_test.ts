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
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

describe('Perfetto ParserViewCaptureWindow', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (await UnitTestUtils.getPerfettoParser(
      TraceType.VIEW_CAPTURE,
      'traces/perfetto/viewcapture.perfetto-trace',
    )) as Parser<HierarchyTreeNode>;
    trace = Trace.fromParser(parser);
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.VIEW_CAPTURE);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual([
      'com.android.internal.policy.PhoneWindow@4f9be60',
      'viewcapture.perfetto-trace',
    ]);
  });

  it('provides timestamps', () => {
    expect(assertDefined(parser.getTimestamps()).length).toEqual(36);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1716828479973482553n),
      TimestampConverterUtils.makeRealTimestamp(1716828479982373666n),
      TimestampConverterUtils.makeRealTimestamp(1716828479986084197n),
    ];
    expect(assertDefined(parser.getTimestamps()).slice(0, 3)).toEqual(expected);
  });

  it('builds trace entry', async () => {
    const root = await parser.getEntry(1);
    expect(root).toBeInstanceOf(HierarchyTreeNode);
    expect(root.name).toEqual(
      'com.android.internal.policy.DecorView@203589466',
    );
    expect(root.getRects()?.length).toEqual(1);

    const children = root.getAllChildren();
    expect(children.length).toEqual(1);
    expect(children[0].name).toEqual('android.widget.LinearLayout@160251275');
    expect(children[0].getRects()?.length).toEqual(1);
  });

  it('sets property default values + formatters', async () => {
    const root = await parser.getEntry(1);
    const properties = await root.getAllProperties();
    const defaultProperty = assertDefined(properties.getChildByName('left'));
    expect(defaultProperty.getValue()).toEqual(0);
    expect(defaultProperty.formattedValue()).toEqual('0');
  });

  it('supports VIEW_CAPTURE_METADATA custom query', async () => {
    const metadata = await trace.customQuery(
      CustomQueryType.VIEW_CAPTURE_METADATA,
    );
    expect(metadata.packageName).toEqual(
      'com.google.android.apps.nexuslauncher',
    );
    expect(metadata.windowName).toEqual(
      'com.android.internal.policy.PhoneWindow@4f9be60',
    );
  });
});
