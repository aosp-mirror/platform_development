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
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceType} from 'trace/trace_type';

describe('ParserScreenRecording', () => {
  let parser: Parser<ScreenRecordingTraceEntry>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    )) as Parser<ScreenRecordingTraceEntry>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = assertDefined(
      parser.getTimestamps(TimestampType.ELAPSED),
    );

    expect(timestamps.length).toEqual(123);

    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211827840430n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211842401430n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211862172430n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    expect(timestamps.length).toEqual(123);

    const expected = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666361048792787045n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666361048807348045n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666361048827119045n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('applies timezone info to real timestamps only', async () => {
    const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
      true,
    )) as Parser<ScreenRecordingTraceEntry>;
    expect(parserWithTimezoneInfo.getTraceType()).toEqual(
      TraceType.SCREEN_RECORDING,
    );

    const expectedElapsed = [
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211827840430n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211842401430n),
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(211862172430n),
    ];
    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
      ).slice(0, 3),
    ).toEqual(expectedElapsed);

    const expectedReal = [
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666380848792787045n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666380848807348045n),
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1666380848827119045n),
    ];
    expect(
      assertDefined(
        parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
      ).slice(0, 3),
    ).toEqual(expectedReal);
  });

  it('retrieves trace entry', async () => {
    {
      const entry = await parser.getEntry(0, TimestampType.REAL);
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }
    {
      const entry = await parser.getEntry(
        parser.getLengthEntries() - 1,
        TimestampType.REAL,
      );
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(1.371077, 0.001);
    }
  });
});
