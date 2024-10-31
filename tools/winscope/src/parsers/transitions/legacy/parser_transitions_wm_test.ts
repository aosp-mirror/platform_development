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
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('ParserTransitionsWm', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
    )) as Parser<PropertyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.WM_TRANSITION);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    expect(timestamps.length).toEqual(8);
    const expected = TimestampConverterUtils.makeZeroTimestamp();
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('translates flags', async () => {
    const entry = await parser.getEntry(4);
    expect(
      entry.getChildByName('wmData')?.getChildByName('flags')?.formattedValue(),
    ).toEqual('TRANSIT_FLAG_IS_RECENTS');

    const targets = entry.getChildByName('wmData')?.getChildByName('targets');
    expect(
      targets?.getChildByName('0')?.getChildByName('flags')?.formattedValue(),
    ).toEqual('FLAG_MOVED_TO_TOP | FLAG_SHOW_WALLPAPER');
    expect(
      targets?.getChildByName('1')?.getChildByName('flags')?.formattedValue(),
    ).toEqual('FLAG_NONE');
  });
});
