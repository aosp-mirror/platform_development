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

import {assertDefined} from 'common/assert_utils';
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

describe('ParserProtoLog', () => {
  let parser: Parser<PropertyTreeNode>;

  beforeAll(async () => {
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/ProtoLog.pb'
    )) as Parser<PropertyTreeNode>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
  });

  it('has expected length', () => {
    expect(parser.getLengthEntries()).toEqual(50);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;
    expect(timestamps.length).toEqual(50);

    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746266486n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746336718n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746350430n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.REAL)!;
    expect(timestamps.length).toEqual(50);

    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377266486n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377336718n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655727125377350430n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('applies timezone info to real timestamps only', async () => {
    const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/ProtoLog.pb',
      true
    )) as Parser<PropertyTreeNode>;
    expect(parserWithTimezoneInfo.getTraceType()).toEqual(TraceType.PROTO_LOG);

    const expectedElapsed = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746266486n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746336718n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850746350430n),
    ];
    expect(
      assertDefined(parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED)).slice(0, 3)
    ).toEqual(expectedElapsed);

    const expectedReal = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377266486n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377336718n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1655746925377350430n),
    ];
    expect(
      assertDefined(parserWithTimezoneInfo.getTimestamps(TimestampType.REAL)).slice(0, 3)
    ).toEqual(expectedReal);
  });

  it('reconstructs human-readable log message (ELAPSED time)', async () => {
    const message = await parser.getEntry(0, TimestampType.ELAPSED);

    expect(assertDefined(message.getChildByName('text')).formattedValue()).toEqual(
      'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false'
    );
    expect(assertDefined(message.getChildByName('timestamp')).formattedValue()).toEqual(
      '14m10s746ms266486ns'
    );
    expect(assertDefined(message.getChildByName('tag')).formattedValue()).toEqual('WindowManager');
    expect(assertDefined(message.getChildByName('level')).formattedValue()).toEqual('DEBUG');
    expect(assertDefined(message.getChildByName('at')).formattedValue()).toEqual(
      'com/android/server/wm/InsetsSourceProvider.java'
    );
  });

  it('reconstructs human-readable log message (REAL time)', async () => {
    const message = await parser.getEntry(0, TimestampType.REAL);

    expect(assertDefined(message.getChildByName('text')).formattedValue()).toEqual(
      'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false'
    );
    expect(assertDefined(message.getChildByName('timestamp')).formattedValue()).toEqual(
      '2022-06-20T12:12:05.377266486'
    );
    expect(assertDefined(message.getChildByName('tag')).formattedValue()).toEqual('WindowManager');
    expect(assertDefined(message.getChildByName('level')).formattedValue()).toEqual('DEBUG');
    expect(assertDefined(message.getChildByName('at')).formattedValue()).toEqual(
      'com/android/server/wm/InsetsSourceProvider.java'
    );
  });
});
