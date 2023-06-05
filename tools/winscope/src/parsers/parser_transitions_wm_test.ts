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
import {Parser} from 'trace/parser';
import {ElapsedTimestamp, RealTimestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('WmFileParserTransitions', () => {
  let parser: Parser<object>;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb'
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.WM_TRANSITION);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length).toEqual(8);

    const expected = [
      new ElapsedTimestamp(57649586217344n),
      new ElapsedTimestamp(57649691956439n),
      new ElapsedTimestamp(57650183020323n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      new RealTimestamp(1683188477542869667n),
      new RealTimestamp(1683188477648608762n),
      new RealTimestamp(1683188478139672646n),
    ];

    const timestamps = parser.getTimestamps(TimestampType.REAL)!;

    expect(timestamps.length).toEqual(8);

    expect(timestamps.slice(0, 3)).toEqual(expected);
  });
});
