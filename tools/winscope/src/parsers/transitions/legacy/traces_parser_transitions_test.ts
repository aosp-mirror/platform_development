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
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('TracesParserTransitions', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
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
      TimestampConverterUtils.makeRealTimestamp(1683188477606574664n),
      TimestampConverterUtils.makeRealTimestamp(1683188477784695636n),
      TimestampConverterUtils.makeRealTimestamp(1683188479255739215n),
      TimestampConverterUtils.makeRealTimestamp(1683188481345218790n),
    ];
    expect(timestamps).toEqual(expected);
  });

  it('provides entries', async () => {
    const entryIds = [
      (await parser.getEntry(0)).getChildByName('id')?.getValue(),
      (await parser.getEntry(1)).getChildByName('id')?.getValue(),
      (await parser.getEntry(2)).getChildByName('id')?.getValue(),
      (await parser.getEntry(3)).getChildByName('id')?.getValue(),
    ];
    expect(entryIds).toEqual([6, 7, 8, 9]);
  });

  it('sets zero timestamp if both dispatch and send time unavailable', async () => {
    const parser = (await UnitTestUtils.getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ])) as Parser<PropertyTreeNode>;
    const entry = await parser.getEntry(1);
    entry
      .getChildByName('shellData')
      ?.removeChild(entry.id + '.shellData.dispatchTimeNs');
    entry
      .getChildByName('wmData')
      ?.removeChild(entry.id + '.wmData.sendTimeNs');

    await parser.createTimestamps();
    expect(parser.getTimestamps()?.at(1)).toEqual(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
  });

  it('fails to parse without both wm and shell transition traces', async () => {
    await expectAsync(
      UnitTestUtils.getTracesParser([
        'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      ]),
    ).toBeRejected();
    await expectAsync(
      UnitTestUtils.getTracesParser([
        'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
      ]),
    ).toBeRejected();
  });
});
