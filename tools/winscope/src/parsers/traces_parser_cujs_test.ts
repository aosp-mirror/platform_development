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
import {Cuj} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {TracesParserCujs} from './traces_parser_cujs';

describe('ParserCujs', () => {
  let parser: Parser<Cuj>;

  beforeAll(async () => {
    const eventLogParser = assertDefined(
      await UnitTestUtils.getParser('traces/eventlog.winscope')
    ) as Parser<Event>;

    parser = new TracesParserCujs([eventLogParser]);
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.CUJS);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length).toEqual(16);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 2661012770462n),
      new Timestamp(TimestampType.ELAPSED, 2661012874914n),
      new Timestamp(TimestampType.ELAPSED, 2661012903966n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.REAL, 1681207048025446000n),
      new Timestamp(TimestampType.REAL, 1681207048025551000n),
      new Timestamp(TimestampType.REAL, 1681207048025580000n),
    ];

    const timestamps = parser.getTimestamps(TimestampType.REAL)!;

    expect(timestamps.length).toEqual(16);

    expect(timestamps.slice(0, 3)).toEqual(expected);
  });
});
