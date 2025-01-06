/*
 * Copyright 2024 The Android Open Source Project
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
import {TraceBuilder} from 'test/unit/trace_builder';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('TracesParserInput', () => {
  let parser: Parser<PropertyTreeNode>;
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(() => {
    userNotifierChecker = new UserNotifierChecker();
  });

  beforeEach(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = (await UnitTestUtils.getTracesParser([
      'traces/perfetto/input-events.perfetto-trace',
    ])) as Parser<PropertyTreeNode>;
    userNotifierChecker.reset();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.INPUT_EVENT_MERGED);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('has expected descriptors', () => {
    expect(parser.getDescriptors()).toEqual(['input-events.perfetto-trace']);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1718386903800330430n),
      TimestampConverterUtils.makeRealTimestamp(1718386903800330430n),
      TimestampConverterUtils.makeRealTimestamp(1718386903821511338n),
      TimestampConverterUtils.makeRealTimestamp(1718386903827304592n),
      TimestampConverterUtils.makeRealTimestamp(1718386903836681382n),
      TimestampConverterUtils.makeRealTimestamp(1718386903841727281n),
      TimestampConverterUtils.makeRealTimestamp(1718386905115026232n),
      TimestampConverterUtils.makeRealTimestamp(1718386905123057319n),
    ];
    expect(timestamps).toEqual(expected);
  });

  it('provides correct entries from individual event traces', async () => {
    const keyEntry = await parser.getEntry(6);
    const keyEvent = assertDefined(keyEntry.getChildByName('keyEvent'));
    expect(keyEvent?.getChildByName('eventId')?.getValue()).toEqual(759309047);

    const motionEntry = await parser.getEntry(0);
    const motionEvent = assertDefined(
      motionEntry.getChildByName('motionEvent'),
    );
    expect(motionEvent?.getChildByName('eventId')?.getValue()).toEqual(
      330184796,
    );
  });

  it('supports VSYNCID custom query', async () => {
    const trace = new TraceBuilder()
      .setType(TraceType.INPUT_EVENT_MERGED)
      .setParser(parser)
      .build();
    const entries = await trace
      .sliceEntries(4, 7)
      .customQuery(CustomQueryType.VSYNCID);
    const values = entries.map((entry) => entry.getValue());
    expect(values).toEqual([89113n, 89113n, 89114n]);
    userNotifierChecker.expectNone();
  });

  it('supports VSYNCID custom query with missing vsync_ids', async () => {
    const missingVsyncIdsParser = (await UnitTestUtils.getTracesParser([
      'traces/perfetto/input-missing-vsync-ids.perfetto-trace',
    ])) as Parser<PropertyTreeNode>;
    const trace = new TraceBuilder()
      .setType(TraceType.INPUT_EVENT_MERGED)
      .setParser(missingVsyncIdsParser)
      .build();
    const entries = await trace.customQuery(CustomQueryType.VSYNCID);
    expect(entries).toHaveSize(missingVsyncIdsParser.getLengthEntries());
  });
});
