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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('Perfetto ParserProtolog', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = await UnitTestUtils.getPerfettoParser(
      TraceType.PROTO_LOG,
      'traces/perfetto/protolog.perfetto-trace',
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = assertDefined(
      parser.getTimestamps(TimestampType.ELAPSED),
    );

    expect(timestamps.length).toEqual(75);

    // TODO: They shouldn't all have the same timestamp...
    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(5939002349294n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(5939002349294n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(5939002349294n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    expect(timestamps.length).toEqual(75);

    // TODO: They shouldn't all have the same timestamp...
    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1706547264827624563n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1706547264827624563n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1706547264827624563n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('reconstructs human-readable log message (ELAPSED time)', async () => {
    const message = await parser.getEntry(0, TimestampType.ELAPSED);

    expect(
      assertDefined(message.getChildByName('text')).formattedValue(),
    ).toEqual('Sent Transition (#11) createdAt=01-29 17:54:23.793');
    expect(
      assertDefined(message.getChildByName('timestamp')).formattedValue(),
    ).toEqual('1h38m59s2ms349294ns');
    expect(
      assertDefined(message.getChildByName('tag')).formattedValue(),
    ).toEqual('WindowManager');
    expect(
      assertDefined(message.getChildByName('level')).formattedValue(),
    ).toEqual('VERBOSE');
    expect(
      assertDefined(message.getChildByName('at')).formattedValue(),
    ).toEqual('<NO_LOC>');
  });

  it('reconstructs human-readable log message (REAL time)', async () => {
    const message = await parser.getEntry(0, TimestampType.REAL);

    expect(
      assertDefined(message.getChildByName('text')).formattedValue(),
    ).toEqual('Sent Transition (#11) createdAt=01-29 17:54:23.793');
    expect(
      assertDefined(message.getChildByName('timestamp')).formattedValue(),
    ).toEqual('2024-01-29T16:54:24.827624563');
    expect(
      assertDefined(message.getChildByName('tag')).formattedValue(),
    ).toEqual('WindowManager');
    expect(
      assertDefined(message.getChildByName('level')).formattedValue(),
    ).toEqual('VERBOSE');
    expect(
      assertDefined(message.getChildByName('at')).formattedValue(),
    ).toEqual('<NO_LOC>');
  });

  it('applies timezone info to real timestamps only', async () => {
    const parserWithTimezoneInfo = await UnitTestUtils.getPerfettoParser(
      TraceType.PROTO_LOG,
      'traces/perfetto/protolog.perfetto-trace',
      true,
    );
    expect(parserWithTimezoneInfo.getTraceType()).toEqual(TraceType.PROTO_LOG);

    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
      )[0],
    ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(5939002349294n));
    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
      )[0],
    ).toEqual(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1706567064827624563n),
    );
  });
});
