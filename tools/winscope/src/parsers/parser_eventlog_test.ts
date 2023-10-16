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
import {UnitTestUtils} from 'test/unit/utils';
import {CujEvent} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {RealTimestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserEventLog', () => {
  let parser: Parser<Event>;

  beforeAll(async () => {
    parser = assertDefined(
      await UnitTestUtils.getParser('traces/eventlog.winscope')
    ) as Parser<Event>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.EVENT_LOG);
  });

  it('has expected timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    expect(timestamps.length).toEqual(184);

    const expected = [
      new RealTimestamp(1681207047981157120n),
      new RealTimestamp(1681207047991161088n),
      new RealTimestamp(1681207047991310592n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it("doesn't provide elapsed timestamps", () => {
    expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(undefined);
  });

  it('contains parsed jank CUJ events', () => {
    const entry = parser.getEntry(18, TimestampType.REAL);
    expect(entry instanceof CujEvent).toBeTrue();
  });
});
