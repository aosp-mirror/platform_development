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
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserProtolog', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await UnitTestUtils.getPerfettoParser(
      TraceType.PROTO_LOG,
      'traces/perfetto/protolog.perfetto-trace',
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    expect(timestamps.length).toEqual(3);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1713866817780323315n),
      TimestampConverterUtils.makeRealTimestamp(1713866817780323415n),
      TimestampConverterUtils.makeRealTimestamp(1713866817780323445n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('reconstructs human-readable log message (REAL time)', async () => {
    const message = await parser.getEntry(0);

    expect(
      assertDefined(message.getChildByName('text')).formattedValue(),
    ).toEqual(
      'Test message with different int formats: 888, 0o1570, 0x378, 888.000000, 8.880000e+02.',
    );
    expect(
      assertDefined(message.getChildByName('timestamp')).formattedValue(),
    ).toEqual('2024-04-23, 10:06:57.780');
    expect(
      assertDefined(message.getChildByName('tag')).formattedValue(),
    ).toEqual('MySecondGroup');
    expect(
      assertDefined(message.getChildByName('level')).formattedValue(),
    ).toEqual('WARN');
    expect(
      assertDefined(message.getChildByName('at')).formattedValue(),
    ).toEqual('<NO_LOC>');
  });

  it('messages are ordered by timestamp', async () => {
    let prevEntryTs = 0n;
    for (let i = 0; i < parser.getLengthEntries(); i++) {
      const ts = (await parser.getEntry(i))
        .getChildByName('timestamp')
        ?.getValue();
      expect(ts >= prevEntryTs).toBeTrue();
      prevEntryTs = ts;
    }
  });

  it('timestamps are ordered', () => {
    let prevEntryTs = 0n;
    for (const ts of assertDefined(parser.getTimestamps())) {
      expect(ts.getValueNs() >= prevEntryTs).toBeTrue();
      prevEntryTs = ts.getValueNs();
    }
  });
});
