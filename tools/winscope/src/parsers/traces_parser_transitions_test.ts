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

import {UnitTestUtils} from 'test/unit/utils';
import {Transition} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserTransitions', () => {
  let parser: Parser<Transition>;

  beforeAll(async () => {
    parser = await UnitTestUtils.getTracesParser([
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    ]);
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length).toEqual(4);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 57649586217344n),
      new Timestamp(TimestampType.ELAPSED, 57649691956439n),
      new Timestamp(TimestampType.ELAPSED, 57651263812071n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.REAL, 1683188477542869667n),
      new Timestamp(TimestampType.REAL, 1683188477648608762n),
      new Timestamp(TimestampType.REAL, 1683188479220464394n),
    ];

    const timestamps = parser.getTimestamps(TimestampType.REAL)!;

    expect(timestamps.length).toEqual(4);

    expect(timestamps.slice(0, 3)).toEqual(expected);
  });
});
