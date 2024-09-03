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

describe('ParserTransitions', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ])) as Parser<PropertyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual([
      'wm_transition_trace.pb',
      'shell_transition_trace.pb',
    ]);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
      TimestampConverterUtils.makeRealTimestamp(1683188477606574664n),
      TimestampConverterUtils.makeRealTimestamp(1683188479255739215n),
    ];
    expect(timestamps).toEqual(expected);
  });
});
